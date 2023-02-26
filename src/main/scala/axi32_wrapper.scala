package pipeline

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._
import pipeline._

class axi32_cpu_wrapper extends Module {
	val MEM = IO(new Bundle{
		// the cpu is acting as the master
		// write address channel signals
		val AWID 		= Output(UInt(1.W)) // random small number(hardwired to 0 for our implementation)
		val AWADDR 		= Output(UInt(32.W)) // 64-bit processor
		val AWLEN 		= Output(UInt(8.W)) // as spec
		val AWSIZE 		= Output(UInt(3.W)) // as spec
		val AWBURST 	= Output(UInt(2.W)) // as spec
		val AWLOCK 		= Output(UInt(1.W)) // as spec
		val AWCACHE 	= Output(UInt(4.W)) // as spec
		val AWPROT 		= Output(UInt(3.W)) // as spec
		val AWQOS 		= Output(UInt(4.W)) // as spec
		//val AWREGION 	= Output(UInt(4.W)) // as spec
		val AWVALID 	= Output(Bool()) // as spec
		val AWREADY 	= Input(Bool()) // as spec

		// write data channel signals
		// AXI 4 does not support WID signal
		val WDATA 	= Output(UInt(32.W)) // 64-bit processor
		val WSTRB 	= Output(UInt(4.W)) // one bit for each byte in WDATA
		val WLAST 	= Output(Bool()) // as spec
		val WVALID 	= Output(Bool()) // as spec
		val WREADY 	= Input(Bool()) // as spec

		// write response channel
		val BID 	= Input(UInt(1.W)) // same width as AWID
		val BRESP 	= Input(UInt(2.W)) // as spec
		val BVALID 	= Input(Bool()) // as spec
		val BREADY 	= Output(Bool()) // as spec

		// read address channel signals
		val ARID 		= Output(UInt(1.W)) // random width(hardwired to 0 for this implemntation)
		val ARADDR 		= Output(UInt(32.W)) // 64-bit machine
		val ARLEN 		= Output(UInt(8.W)) // as spec
		val ARSIZE 		= Output(UInt(3.W)) // as spec
		val ARBURST 	= Output(UInt(2.W)) // as spec
		val ARLOCK 		= Output(UInt(1.W)) // as spec AXI4
		val ARCACHE 	= Output(UInt(4.W)) // as spec
		val ARPROT 		= Output(UInt(3.W)) // as spec
		val ARQOS 		= Output(UInt(4.W)) // as spec
		//val ARREGION 	= Output(UInt(4.W)) // as spec
		val ARVALID 	= Output(Bool()) // as spec
		val ARREADY 	= Input(Bool()) // as spec

		// read data channel signals
		val RID 	= Input(UInt(1.W)) // width matched to ARID
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
  val instrAccess :: dataAccess :: Nil = Enum(2)
  val respBuffers = RegInit(VecInit(Seq.fill(2)(RegInit((new Bundle{
		val free 	= Bool()
    val isWrite = Bool()
		val internalWait = Bool()
		val data 	= UInt(64.W) // these have been properly aligned
    val size = UInt(3.W)
	}).Lit(
		_.free 	-> true.B,
    _.isWrite -> false.B,
		_.internalWait 	-> false.B,
		_.data 		-> 0.U,
    _.size  -> 0.U
	)))))

  val currReq = RegInit((new Bundle{
    val src = instrAccess.cloneType
		val size 	= UInt(3.W)
		val address = UInt(64.W)
		val mask 	= UInt(8.W) // used for strobe in axi
		val data 	= UInt(64.W) // these have been properly aligned
	}).Lit(
    _.src   -> instrAccess,
		_.size 		-> 0.U,
		_.address 	-> 0.U,
		_.mask 		-> 0.U,
		_.data 		-> 0.U
	))

  val reqStatus = RegInit((new Bundle{
    val awvalid   = Bool()
    val wvalid    = Bool()
    val bready    = Bool()
    val arvalid   = Bool()
    val rready    = Bool()
    val wlast     = Bool()
  }).Lit(
    _.awvalid   -> false.B,
    _.wvalid    -> false.B,
    _.bready    -> false.B,
    _.arvalid   -> false.B,
    _.rready    -> false.B,
    _.wlast     -> false.B
  ))

  val handlingReq = Cat(reqStatus.awvalid, reqStatus.wvalid, reqStatus.bready, reqStatus.arvalid, reqStatus.rready).orR

  riscvCore.instrPort.reqport_ready := ((!respBuffers(dataAccess).free || !riscvCore.dataPort.a.valid.asBool) && respBuffers(instrAccess).free && !handlingReq).asUInt
  riscvCore.dataPort.a.ready := (respBuffers(dataAccess).free && !handlingReq).asUInt

  when(!handlingReq) {
    when(respBuffers(dataAccess).free && riscvCore.dataPort.a.valid.asBool) {
      respBuffers(dataAccess).free := false.B
      respBuffers(dataAccess).size := riscvCore.dataPort.a.size

      reqStatus.awvalid := riscvCore.dataPort.a.opcode === 1.U
      reqStatus.wvalid := riscvCore.dataPort.a.opcode === 1.U
      reqStatus.bready := riscvCore.dataPort.a.opcode === 1.U
      reqStatus.arvalid := riscvCore.dataPort.a.opcode === 4.U
      reqStatus.rready := riscvCore.dataPort.a.opcode === 4.U
      reqStatus.wlast  := riscvCore.dataPort.a.size =/= 3.U

      currReq.src := dataAccess
      currReq.address := riscvCore.dataPort.a.address
      currReq.size := riscvCore.dataPort.a.size
      // double words are always aligned, other sizes has to be aligned
      currReq.mask := Mux(riscvCore.dataPort.a.address(2).asBool, Cat(0.U(4.W), riscvCore.dataPort.a.mask(7, 4)), riscvCore.dataPort.a.mask)
      currReq.data := Mux(riscvCore.dataPort.a.address(2).asBool, Cat(0.U(32.W), riscvCore.dataPort.a.data(63, 32)), riscvCore.dataPort.a.data)
    }.elsewhen(respBuffers(instrAccess).free && riscvCore.instrPort.reqport_valid.asBool) {
      respBuffers(instrAccess).free := false.B
      respBuffers(instrAccess).size := 2.U

      reqStatus.arvalid := true.B
      reqStatus.rready := true.B

      currReq.src := instrAccess
      reqStatus.arvalid := true.B
      reqStatus.rready := true.B
      currReq.address := riscvCore.instrPort.reqport_addr
      currReq.size := 2.U
    }
  }

  // accepting requests and responses
  when(reqStatus.awvalid && MEM.AWREADY) { reqStatus.awvalid := false.B }
  when(reqStatus.wvalid && MEM.WVALID && reqStatus.wlast) { reqStatus.wvalid := false.B }
  when(reqStatus.bready && MEM.BVALID) { reqStatus.bready := false.B }
  when(reqStatus.arvalid && MEM.ARREADY) { reqStatus.arvalid := false.B }
  when(reqStatus.rready && MEM.RVALID && MEM.RLAST) { reqStatus.rready := false.B }

  when(reqStatus.wvalid && MEM.WVALID && !reqStatus.wlast) { 
    reqStatus.wlast := true.B
    currReq.mask := Cat(0.U(4.W), currReq.mask(7, 4)) 
    currReq.data := Cat(0.U(32.W), currReq.data(63, 32))
  }

  when(reqStatus.rready && MEM.RVALID) {
    respBuffers(MEM.RID).data := Mux(MEM.RLAST && (currReq.size === 3.U || currReq.address(2).asBool) && MEM.RID =/= instrAccess,  
    Cat(MEM.RDATA, respBuffers(MEM.RID).data(31, 0)), Cat(0.U(32.W), MEM.RDATA))
    respBuffers(MEM.RID).internalWait := MEM.RLAST
    respBuffers(MEM.RID).isWrite := false.B
  }

  when(reqStatus.bready && MEM.BVALID) {
    respBuffers(MEM.BID).internalWait := true.B
    respBuffers(MEM.BID).isWrite := true.B
  }

  when(respBuffers(instrAccess).internalWait && riscvCore.instrPort.resport_ready.asBool) {
    respBuffers(instrAccess).free := true.B
    respBuffers(instrAccess).internalWait := false.B
  }

  when(respBuffers(dataAccess).internalWait && riscvCore.dataPort.d.ready.asBool) {
    respBuffers(dataAccess).free := true.B
    respBuffers(dataAccess).internalWait := false.B
  }

  riscvCore.instrPort.resport_instr := respBuffers(instrAccess).data(31, 0)
  riscvCore.instrPort.resport_valid := respBuffers(instrAccess).internalWait.asUInt

  riscvCore.dataPort.d.opcode  := !respBuffers(dataAccess).isWrite.asUInt
  riscvCore.dataPort.d.param   := 0.U
  riscvCore.dataPort.d.size    := respBuffers(dataAccess).size
  riscvCore.dataPort.d.source  := dataAccess
  riscvCore.dataPort.d.sink    := 0.U
  riscvCore.dataPort.d.data    := respBuffers(dataAccess).data
  riscvCore.dataPort.d.error   := 0.U
  riscvCore.dataPort.d.valid   := respBuffers(dataAccess).internalWait.asUInt

  val test_data = IO(Output(UInt(8.W)))
  val reg_test_data = RegInit(0.U(8.W))

  when (reg_test_data === 0.U && riscvCore.dataPort.a.valid.asBool &&
	riscvCore.dataPort.a.address === (~(0.U(64.W)))) {
		reg_test_data := riscvCore.dataPort.a.data(63, 56)
	}

  test_data := reg_test_data

  MEM.AWID 		:= currReq.src// random small number(hardwired to 0 for our implementation)
  MEM.AWADDR 		:= currReq.address // 64-bit processor
  MEM.AWLEN 		:= Mux(currReq.size === 3.U, 1.U, 0.U) // as spec
  MEM.AWSIZE 		:= currReq.size // as spec
  MEM.AWBURST 	:= 1.U // as spec
  MEM.AWLOCK 		:= 0.U // as spec
  MEM.AWCACHE 	:= 2.U// as spec
  MEM.AWPROT 		:= 0.U // as spec
  MEM.AWQOS 		:= 0.U // as spec
  //val AWREGION 	= Output(UInt(4.W)) // as spec
  MEM.AWVALID 	:= reqStatus.awvalid // as spec

  // write data channel signals
  // AXI 4 does not support WID signal
  MEM.WDATA 	:= currReq.data(31, 0) // 64-bit processor
  MEM.WSTRB 	:= currReq.mask(3, 0) // one bit for each byte in WDATA
  MEM.WLAST 	:= reqStatus.wlast // as spec
  MEM.WVALID 	:= reqStatus.wvalid // as spec

  // write response channel
  MEM.BREADY 	:= reqStatus.bready // as spec

  // read address channel signals
  MEM.ARID 		:= currReq.src // random width(hardwired to 0 for this implemntation)
  MEM.ARADDR 		:= currReq.address // 64-bit machine
  MEM.ARLEN 		:= Mux(currReq.size === 3.U, 1.U, 0.U) // as spec
  MEM.ARSIZE 		:= currReq.size // as spec
  MEM.ARBURST 	:= 1.U // as spec
  MEM.ARLOCK 		:= 0.U // as spec AXI4
  MEM.ARCACHE 	:= 2.U // as spec
  MEM.ARPROT 		:= 0.U // as spec
  MEM.ARQOS 		:= 0.U // as spec
  //val ARREGION 	= Output(UInt(4.W)) // as spec
  MEM.ARVALID 	:= reqStatus.arvalid // as spe

  // read data channel signals
  MEM.RREADY 	:= reqStatus.rready // as spec
}

object axi32_cpu_wrapper extends App {
	(new stage.ChiselStage).emitVerilog(new axi32_cpu_wrapper)
}
