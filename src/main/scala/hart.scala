
package pipeline

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._

class hart extends Module{
    val fetchUnit = Module(new fetch.FetchUnit(16, 2))
    val decodeUnit = Module(new decode.DECODE_ISSUE_UNIT())
    val memoryAccessUnit = Module(new MemoryUnit())

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

    decodeUnit.io.decodeIssuePort
}

object hart extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new hart())
}