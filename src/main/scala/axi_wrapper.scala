package pipeline.alu

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._
import pipeline._

class axi_cpu_wrapper extends Module {
	val MEM = IO(new Bundle{
		// the cpu is acting as the master
		// write address channel signals
		val AWID 		= Output(UInt(2.W)) // random small number(hardwired to 0 for our implementation)
		val AWADDR 		= Output(UInt(64.W)) // 64-bit processor
		val AWLEN 		= Output(UInt(8.W)) // as spec
		val AWSIZE 		= Output(UInt(3.W)) // as spec
		val AWBURST 	= Output(UInt(2.W)) // as spec
		val AWLOCK 		= Output(UInt(1.W)) // as spec
		val AWCACHE 	= Output(UInt(4.W)) // as spec
		val AWPROT 		= Output(UInt(3.W)) // as spec
		val AWQOS 		= Output(UInt(4.W)) // as spec
		val AWREGION 	= Output(UInt(4.W)) // as spec
		val AWVALID 	= Output(Bool()) // as spec
		val AWREADY 	= Input(Bool()) // as spec

		// write data channel signals
		// AXI 4 does not support WID signal
		val WDATA 	= Output(UInt(64.W)) // 64-bit processor
		val WSTRB 	= Output(UInt(8.W)) // one bit for each byte in WDATA
		val WLAST 	= Output(Bool()) // as spec
		val WVALID 	= Output(Bool()) // as spec
		val WREADY 	= Input(Bool()) // as spec

		// write response channel
		val BID 	= Input(UInt(2.W)) // same width as AWID
		val BRESP 	= Input(UInt(2.W)) // as spec
		val BVALID 	= Input(Bool()) // as spec
		val BREADY 	= Output(Bool()) // as spec

		// read address channel signals
		val ARID 		= Output(UInt(2.W)) // random width(hardwired to 0 for this implemntation)
		val ARADDR 		= Output(UInt(64.W)) // 64-bit machine
		val ARLEN 		= Output(UInt(8.W)) // as spec
		val ARSIZE 		= Output(UInt(3.W)) // as spec
		val ARBURST 	= Output(UInt(2.W)) // as spec
		val ARLOCK 		= Output(UInt(1.W)) // as spec AXI4
		val ARCACHE 	= Output(UInt(4.W)) // as spec
		val ARPROT 		= Output(UInt(3.W)) // as spec
		val ARQOS 		= Output(UInt(4.W)) // as spec
		val ARREGION 	= Output(UInt(4.W)) // as spec
		val ARVALID 	= Output(Bool()) // as spec
		val ARREADY 	= Input(Bool()) // as spec

		// read data channel signals
		val RID 	= Input(UInt(2.W)) // width matched to ARID
		val RDATA 	= Input(UInt(64.W)) // 64 - bit machine
		val RRESP 	= Input(UInt(2.W)) // as spec
		val RLAST 	= Input(Bool()) // as spec
		val RVALID 	= Input(Bool()) // as spec
		val RREADY 	= Output(Bool()) // as spec
	})

	val riscvCore = Module(new cpu)

	/**
	  * The cpu accesses memory through two ports(data and instructions). There are two buffers
	  * for each port. An given port is ready when the corresponding buffer is free. A request 
	  * from one of the buffers is selected for service. Data accesses are given priority.
	  * 
	  * If instruction access is given priority, then there is chance of deadlock.
	  */

	// buffer for data access port
	val dataReqBuffer = RegInit((new Bundle{
		val valid 	= Bool()
		val isWrite = Bool()
		val size 	= UInt(2.W)
		val address = UInt(64.W)
		val mask 	= UInt(8.W) // used for strobe in axi
		val data 	= UInt(64.W) // these have been properly aligned
	}).Lit(
		_.valid 	-> false.B,
		_.isWrite 	-> false.B,
		_.size 		-> 0.U,
		_.address 	-> 0.U,
		_.mask 		-> 0.U,
		_.data 		-> 0.U
	))


	// buffer for instruction access port
	val instrReqBuffer = RegInit(dataReqBuffer.cloneType.Lit(
		_.valid 	-> false.B,
		_.isWrite 	-> false.B, // instruction are always read
		_.size 		-> 2.U, // instructions are 4 bytes
		_.address 	-> 0.U,
		_.mask 		-> 0.U,
		_.data 		-> 0.U
	))

	riscvCore.instrPort.reqport_ready := ~instrReqBuffer.valid.asUInt
	riscvCore.dataPort.a.ready := ~dataReqBuffer.valid.asUInt

	when(!instrReqBuffer.valid) {
		// get a request from instruction port when buffer is empty
		instrReqBuffer.valid := riscvCore.instrPort.reqport_valid.asBool
		instrReqBuffer.address := riscvCore.instrPort.reqport_addr
	}
	when(!dataReqBuffer.valid) {
		// get a request from data port when buffer is empty
		// data accesses are assumed to be aligned
		dataReqBuffer.valid 	:= riscvCore.dataPort.a.valid.asBool
		dataReqBuffer.isWrite 	:= (riscvCore.dataPort.a.opcode === 1.U)
		dataReqBuffer.size 		:= riscvCore.dataPort.a.size
		dataReqBuffer.address 	:= riscvCore.dataPort.a.address
		dataReqBuffer.mask 		:= riscvCore.dataPort.a.mask
		dataReqBuffer.data 		:= riscvCore.dataPort.a.data
	}

	val instruction :: data :: Nil = Enum(2)

	/**
	  * A buffered resquest from instruction or data buffers is selected
	  * and buffered here
	  */
	val axiReqBuffer = RegInit((new Bundle{
		val source = instruction.cloneType
		val request = dataReqBuffer.cloneType
	}).Lit(
		_.source -> instruction,
		_.request -> (dataReqBuffer.cloneType.Lit(
			_.valid 	-> false.B,
			_.isWrite 	-> false.B, // instruction are always read
			_.size 		-> 0.U, // instructions are 4 bytes
			_.address 	-> 0.U,
			_.mask 		-> 0.U,
			_.data 		-> 0.U
		))
	))

	/**
	  * When giveCtrlToCpu is deasserted to accesses pass through
	  * to memory
	  */
	val giveCtrlToCpu = IO(Input(Bool()))
	// buffers to collect response
	val dataRespBuffer = RegInit((new Bundle{
		val free = Bool()
		val responded = Bool()
		val RDATA = UInt(64.W)
		val isWrite = Bool()
	}).Lit(
		_.free -> true.B,
		_.responded -> false.B,
		_.RDATA -> 0.U,
		_.isWrite -> false.B
	))
	val instrRespBuffer = RegInit(dataRespBuffer.cloneType.Lit(
		_.free -> true.B,
		_.responded -> false.B,
		_.RDATA -> 0.U,
		_.isWrite -> false.B
	))
	when(!axiReqBuffer.request.valid && giveCtrlToCpu){
		// getting a request from one of the two buffers
		when(dataReqBuffer.valid && dataRespBuffer.free){
			// data is given prority
			axiReqBuffer.source := data
			axiReqBuffer.request := dataReqBuffer
			dataReqBuffer.valid := false.B
			dataRespBuffer.free := false.B
			dataRespBuffer.isWrite := dataReqBuffer.isWrite
		}.elsewhen(instrReqBuffer.valid && instrRespBuffer.free){
			axiReqBuffer.source := instruction
			axiReqBuffer.request := instrReqBuffer
			instrReqBuffer.valid := false.B
			instrRespBuffer.free := false.B
			instrRespBuffer.isWrite := false.B
		}
	} 

	/**
	  * all transaction IDs will be hardwired to ground. This simplifies
	  * the design as well forces data and instructions to be serviced in
	  * order.
	  */
	// connecting axiReqBuffer to AXI write address access port
	MEM.AWID := axiReqBuffer.source
	MEM.AWADDR := axiReqBuffer.request.address
	MEM.AWLEN := 0.U // all transactions have burst length of one
	MEM.AWSIZE := axiReqBuffer.request.size
	MEM.AWBURST := 1.U // this number shouldn't matter for burst length of one
	MEM.AWLOCK := 0.U // all are normal access
	MEM.AWCACHE := 0.U // shouldn't affect us
	MEM.AWPROT := "b010".U // no protections
	MEM.AWQOS := 0.U // not participating in any QoS scheme
	MEM.AWREGION := 0.U // default value

	/**
	  * For requests AXI has 3 channels
	  * 1. Write address
	  * 2. Write data
	  * 3. Read address
	  */

	/**
	  * Write requests require transactions on 2 channels. Each channels behaves independently.
	  */
	val writeAddressIssued = RegInit(false.B)
	MEM.AWVALID := (axiReqBuffer.request.valid && axiReqBuffer.request.isWrite && !writeAddressIssued)
	when(MEM.AWREADY && axiReqBuffer.request.valid && axiReqBuffer.request.isWrite && !writeAddressIssued){
		writeAddressIssued := true.B
	}

	// connecting axiReqBuffer to write data channel
	MEM.WDATA := axiReqBuffer.request.data
	MEM.WSTRB := axiReqBuffer.request.mask
	MEM.WLAST := true.B // burst length of one
	
	val writeDataIssued = RegInit(false.B)
	MEM.WVALID := (axiReqBuffer.request.valid && axiReqBuffer.request.isWrite && !writeDataIssued)
	when(MEM.WREADY && axiReqBuffer.request.valid && axiReqBuffer.request.isWrite && !writeDataIssued) {
		writeDataIssued := true.B
	}

	// connecting axiReqBuffer to read address channel
	MEM.ARID := axiReqBuffer.source // default
	MEM.ARADDR := axiReqBuffer.request.address
	MEM.ARLEN := 0.U // default burst length of one
	MEM.ARSIZE := axiReqBuffer.request.size
	MEM.ARBURST := 1.U // default burst type INCR
	MEM.ARLOCK := 0.U // all are normal access
	MEM.ARCACHE := 0.U // default option
	MEM.ARPROT := "b010".U // no protections
	MEM.ARQOS := 0.U // does not participate in QoS
	MEM.ARREGION := 0.U // default option

	val readAddressIssued = RegInit(false.B)
	MEM.ARVALID := (axiReqBuffer.request.valid && !axiReqBuffer.request.isWrite && !readAddressIssued)
	when(MEM.ARREADY && axiReqBuffer.request.valid && !axiReqBuffer.request.isWrite && !readAddressIssued) {
		readAddressIssued := true.B
	}

	val respBuffers = Seq(dataRespBuffer, instrRespBuffer)
	// connecting response channels
	MEM.BREADY := respBuffers.map(i =>(!i.free && i.isWrite && !i.responded)).reduce(_ || _)
	MEM.RREADY := respBuffers.map(i =>(!i.free && !i.isWrite && !i.responded)).reduce(_ || _)

	when (MEM.RREADY && MEM.RVALID) {
		// recodring response from read data channel
		when(MEM.RID === data) {
			dataRespBuffer.responded := true.B
			dataRespBuffer.RDATA := MEM.RDATA
		}.elsewhen(MEM.RID === instruction) {
			instrRespBuffer.responded := true.B
			instrRespBuffer.RDATA := MEM.RDATA
		}
	}.elsewhen(MEM.BREADY && MEM.BVALID) {
		// recodring response from write response channel
		when(MEM.BID === data) {
			dataRespBuffer.responded := true.B
		}.elsewhen(MEM.BID === instruction) {
			// this shouldn't happen
			instrRespBuffer.responded := true.B
		}
	}

	val dataAXIWait = RegInit(dataReqBuffer.cloneType.Lit(
		_.valid 	-> false.B,
		_.isWrite 	-> false.B,
		_.size 		-> 0.U,
		_.address 	-> 0.U,
		_.mask 		-> 0.U,
		_.data 		-> 0.U
	))

	val instrAXIWait = RegInit(dataReqBuffer.cloneType.Lit(
		_.valid 	-> false.B,
		_.isWrite 	-> false.B,
		_.size 		-> 0.U,
		_.address 	-> 0.U,
		_.mask 		-> 0.U,
		_.data 		-> 0.U
	))

	when(axiReqBuffer.request.valid &&
	((axiReqBuffer.request.isWrite && writeAddressIssued && writeDataIssued) || 
	(!axiReqBuffer.request.isWrite && readAddressIssued)) &&
	((axiReqBuffer.source === instruction && !instrAXIWait.valid) ||
	(axiReqBuffer.source === data && !dataAXIWait.valid))) {
		axiReqBuffer.request.valid 	:= false.B
		writeAddressIssued 			:= false.B
		writeDataIssued 			:= false.B
		readAddressIssued 			:= false.B
		when(axiReqBuffer.source === instruction) {
			instrAXIWait := axiReqBuffer.request
		}.elsewhen(axiReqBuffer.source === data) {
			dataAXIWait := axiReqBuffer.request
		}
	}

	riscvCore.instrPort.resport_valid := (instrAXIWait.valid && instrRespBuffer.responded && !instrRespBuffer.free).asUInt
	riscvCore.instrPort.resport_instr := Mux(instrAXIWait.address(2).asBool, instrRespBuffer.RDATA(63, 32), instrRespBuffer.RDATA(31, 0))

	when(riscvCore.instrPort.resport_ready.asBool && riscvCore.instrPort.resport_valid.asBool) {
		instrAXIWait.valid := false.B
		instrRespBuffer.free := true.B
		instrRespBuffer.responded := false.B
	}

	// connecting response to data port
	riscvCore.dataPort.d.data := dataRespBuffer.RDATA
	riscvCore.dataPort.d.opcode := Mux(dataAXIWait.isWrite, 1.U, 4.U)
	riscvCore.dataPort.d.param := 0.U
	riscvCore.dataPort.d.size := dataAXIWait.size
	riscvCore.dataPort.d.source := 0.U
	riscvCore.dataPort.d.sink := 0.U
	riscvCore.dataPort.d.error := 0.U
	riscvCore.dataPort.d.valid := (dataAXIWait.valid && dataRespBuffer.responded && !dataRespBuffer.free).asUInt

	when(riscvCore.dataPort.d.ready.asBool && riscvCore.dataPort.d.valid.asBool) {
		dataAXIWait.valid := false.B
		dataRespBuffer.free := true.B
		dataRespBuffer.responded := false.B
		dataRespBuffer.isWrite := false.B
	}
	
	val testResult = RegInit(0.U)
	when (testResult === 0.U && riscvCore.dataPort.a.valid.asBool &&
	riscvCore.dataPort.a.address === (~(0.U(64.W)))) {
		testResult := riscvCore.dataPort.a.data(63, 56)
	}
	val testResultData = IO(Output(UInt(8.W)))
	testResultData := testResult
}

object axi_cpu_wrapper extends App {
	(new stage.ChiselStage).emitVerilog(new axi_cpu_wrapper)
}
