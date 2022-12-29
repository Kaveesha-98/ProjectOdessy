
package pipeline.alu

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._
import pipeline._

class cpu extends Module {
    val fetch = Module(new pipeline.fetch.FetchUnit(0, 2))
    val decode = Module(new pipeline.decode.DecodeUnit)
    val alu = Module(new pipeline.alu.alu)
    val memoryAccess = Module(new memory.MemoryUnit)

    val instrPort = IO(new Bundle(){
        //reqport signals
        val reqport_valid = Output(UInt(1.W))
        val reqport_ready = Input(UInt(1.W))
        val reqport_addr = Output(UInt(64.W))

        //resport signals
        val resport_valid = Input(UInt(1.W))
        val resport_ready = Output(UInt(1.W))
        val resport_instr = Input(UInt(32.W))
    })

    instrPort.reqport_valid := fetch.io.reqport_valid
    fetch.io.reqport_ready := instrPort.reqport_ready
    instrPort.reqport_addr := fetch.io.reqport_addr

    fetch.io.resport_valid := instrPort.resport_valid
    instrPort.resport_ready := fetch.io.resport_ready
    fetch.io.resport_instr := instrPort.resport_instr

    decode.io.fetchIssuePort.valid := fetch.io.issueport_valid
    decode.io.fetchIssuePort.bits.instruction := fetch.io.issueport_instr
    decode.io.fetchIssuePort.bits.PC := fetch.io.issueport_pc
    fetch.io.pipelinestalled := ~decode.io.fetchIssuePort.ready

    fetch.io.target_input := alu.branchResult.target
    fetch.io.target_valid := alu.branchResult.valid.asUInt

    decode.io.decodeIssuePort.ready := alu.decodeIssuePort.ready.asUInt
    alu.decodeIssuePort.valid := decode.io.decodeIssuePort.valid.asBool
    alu.decodeIssuePort.bits.instruction := decode.io.decodeIssuePort.bits.instruction
    alu.decodeIssuePort.bits.PC := decode.io.decodeIssuePort.bits.PC
    alu.decodeIssuePort.bits.rs1 := decode.io.decodeIssuePort.bits.rs1
    alu.decodeIssuePort.bits.rs2 := decode.io.decodeIssuePort.bits.rs2
    alu.decodeIssuePort.bits.immediate := decode.io.decodeIssuePort.bits.immediate

    decode.io.writeBackResult.rd := memoryAccess.memoryIssuePort.bits.instruction(11, 7)
    decode.io.writeBackResult.rdData := memoryAccess.memoryIssuePort.bits.aluResult
    decode.io.writeBackResult.toRegisterFile := memoryAccess.memoryIssuePort.valid && !(
        memoryAccess.memoryIssuePort.bits.instruction(6, 0) === BitPat("b?100011") // excluding stores and conditional branches
    )

    alu.aluIssuePort <> memoryAccess.aluIssuePort

    val dataPort = IO(memoryAccess.dataPort.cloneType)
    dataPort <> memoryAccess.dataPort

    memoryAccess.memoryIssuePort.ready := true.B
}

object cpu extends App {
    (new stage.ChiselStage).emitVerilog(new cpu)
}

class cpuTestbench extends Module {
    val giveCtrlToCpu = IO(Input(Bool()))
    val storeOut = IO(new Bundle(){
        val value = Output(UInt(8.W))
        val valid = Output(Bool())
    })
    val programmer = IO(new Bundle(){
        val byteValid = Input(Bool())
        val byte = Input(UInt(8.W))
    })

    val dutcpu = Module(new cpu)
    val mem = SyncReadMem(10240, UInt(8.W))

    val readReq :: readingMem :: writeResp :: Nil = Enum(3)
    val dataReq = Reg(new Bundle(){
        val address = UInt(64.W)
        val size = UInt(2.W)
        val opcode = UInt(3.W)
    })
    val dataAccessState = RegInit(readReq)
    switch(dataAccessState) {
        is(readReq) {
            dataReq.address := (dutcpu.dataPort.a.address - "h80000000".U)
            dataReq.size := dutcpu.dataPort.a.size
            dataReq.opcode := dutcpu.dataPort.a.opcode
            when (dutcpu.dataPort.a.valid.asBool && giveCtrlToCpu) { dataAccessState := readingMem }
        }
        is(writeResp) {
            when (dutcpu.dataPort.d.ready.asBool && giveCtrlToCpu) { dataAccessState := readReq }
        }
        is(readingMem) {
            dataAccessState := writeResp
        }
    }

    dutcpu.dataPort.a.ready := (dataAccessState === readReq && giveCtrlToCpu).asUInt
    dutcpu.dataPort.d.valid := (dataAccessState === writeResp && giveCtrlToCpu).asUInt
    dutcpu.dataPort.d.opcode := Mux(dataReq.opcode === 4.U, 1.U, 0.U)
    dutcpu.dataPort.d.param := 0.U
    dutcpu.dataPort.d.size := dataReq.size
    dutcpu.dataPort.d.source := 0.U
    dutcpu.dataPort.d.sink := 0.U
    dutcpu.dataPort.d.data := Cat(Seq.tabulate(8)(i => mem.read((dataReq.address&(~7.U)) + i.U)).reverse)
    dutcpu.dataPort.d.error := 0.U

    when (dutcpu.dataPort.a.valid.asBool &&
    dutcpu.dataPort.a.opcode === 1.U &&
    dataAccessState === readReq && giveCtrlToCpu) {

        Seq.tabulate(8)(i => i).foreach(i => {
            when(dutcpu.dataPort.a.mask(i).asBool) {
                mem.write((dutcpu.dataPort.a.address&(~7.U)) + i.U, dutcpu.dataPort.a.data(8*(i+1) - 1, 8*i))
            }
        })
    }

    storeOut.value := dutcpu.dataPort.a.data(7, 0)
    storeOut.valid := dutcpu.dataPort.a.valid.asBool

    val instrAccessState = RegInit(readReq)
    dutcpu.instrPort.reqport_ready := (instrAccessState === readReq && giveCtrlToCpu).asUInt
    dutcpu.instrPort.resport_valid := (instrAccessState === writeResp && giveCtrlToCpu).asUInt
    val instrReqAddress = Reg(UInt(64.W))
    when ( instrAccessState === readReq ) { instrReqAddress := dutcpu.instrPort.reqport_addr - "h8000_0000".U}
    dutcpu.instrPort.resport_instr := Cat(Seq.tabulate(4)(i => mem.read(instrReqAddress + i.U)).reverse)
    switch (instrAccessState) {
        is ( readReq ) { when (dutcpu.instrPort.reqport_valid.asBool && giveCtrlToCpu) { instrAccessState := readingMem} }
        is ( writeResp) { when (dutcpu.instrPort.resport_ready.asBool && giveCtrlToCpu) { instrAccessState := readReq} }
        is ( readingMem ) { instrAccessState := writeResp }
    }

    val programAddress = RegInit(0.U(64.W))
    when (!giveCtrlToCpu && programmer.byteValid) {
        mem.write(programAddress, programmer.byte)
        programAddress := programAddress + 1.U
    } 
}

object cpuTestbench extends App {
    (new stage.ChiselStage).emitVerilog(new cpuTestbench)
}