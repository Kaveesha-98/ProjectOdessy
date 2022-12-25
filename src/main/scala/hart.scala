
package pipeline

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._

class hart extends Module{
    val fetchUnit = Module(new fetch.FetchUnit(16, 2))
    val decodeUnit = Module(new decode.DECODE_ISSUE_UNIT())
    val memoryAccessUnit = Module(new memory.MemoryUnit())

    val io = IO(new Bundle(){

        //reqport signals
        val reqport_valid = Output(UInt(1.W))
        val reqport_ready = Input(UInt(1.W))
        val reqport_addr = Output(UInt(64.W))

        //resport signals
        val resport_valid = Input(UInt(1.W))
        val resport_ready = Output(UInt(1.W))
        val resport_instr = Input(UInt(32.W))

    })

    val dataMemPort = IO(memoryAccessUnit.io.memPort.cloneType)
    dataMemPort <> memoryAccessUnit.io.memPort

    io.reqport_valid <> fetchUnit.io.reqport_valid
    io.reqport_ready <> fetchUnit.io.reqport_ready
    io.reqport_addr <> fetchUnit.io.reqport_addr

    io.resport_valid <> fetchUnit.io.resport_valid
    io.resport_ready <> fetchUnit.io.resport_ready
    io.resport_instr <> fetchUnit.io.resport_instr

    /* val issueport_valid = Output(UInt(1.W))
    val issueport_instr = Output(UInt(32.W))
    val issueport_pc = Output(UInt(64.W)) */

    decodeUnit.io.fetchIssuePort.valid <> fetchUnit.io.issueport_valid
    decodeUnit.io.fetchIssuePort.instruction <> fetchUnit.io.issueport_instr
    decodeUnit.io.fetchIssuePort.PC <> fetchUnit.io.issueport_pc
    decodeUnit.io.readyOut <> fetchUnit.io.pipelinestalled

    decodeUnit.io.decodeIssuePort.valid <> memoryAccessUnit.io.aluIssuePort.valid
    decodeUnit.io.decodeIssuePort.instruction <> memoryAccessUnit.io.aluIssuePort.bits.instruction
    decodeUnit.io.decodeIssuePort.PC <> memoryAccessUnit.io.aluIssuePort.bits.nextInstPtr
    decodeUnit.io.decodeIssuePort.rs1 <> memoryAccessUnit.io.aluIssuePort.bits.aluResult
    decodeUnit.io.decodeIssuePort.rs2 <> memoryAccessUnit.io.aluIssuePort.bits.rs2
    decodeUnit.io.readyIn := memoryAccessUnit.io.aluIssuePort.ready.asUInt

    val memIssueOp = memoryAccessUnit.io.memoryIssuePort.bits.instruction(6, 0)

    decodeUnit.io.writeBackResult.toRegisterFile := Seq(
        "b0000011".U, "b0010011".U, "b0110011".U
    ).map(op => op === memIssueOp).reduce(_ || _)

    decodeUnit.io.writeBackResult.rd := memoryAccessUnit.io.memoryIssuePort.bits.instruction(11, 7)
    decodeUnit.io.writeBackResult.rdData := memoryAccessUnit.io.memoryIssuePort.bits.aluResult

    val decodeIssuedBranch = Seq(
        "b1101111".U, "b1100111".U, "b1100011".U
    ).map(op => op === decodeUnit.io.decodeIssuePort.instruction(6, 0)).reduce(_ || _)
    
    fetchUnit.io.target_valid := (decodeUnit.io.decodeIssuePort.valid.asBool && decodeIssuedBranch && memoryAccessUnit.io.aluIssuePort.ready).asUInt
    fetchUnit.io.target_input := decodeUnit.io.decodeIssuePort.PC + 4.U

    memoryAccessUnit.io.memoryIssuePort.ready := true.B
}

object hart extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new hart())
}