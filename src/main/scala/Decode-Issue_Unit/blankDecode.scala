package pipeline.decode

import Constants._

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class blankDecode extends Module {
    val fetchIssueIntfce = IO(new Bundle{
        val ready       = Output(Bool())
        val issued      = Input(Bool())
        val PC          = Input(UInt(64.W))
        val instruction = Input(UInt(64.W))
        val expected    = new Bundle{
            val valid       = Output(Bool())
            val PC          = Output(UInt(64.W))
        }
    })

    val decodeIssuePort = IO(Flipped(new handshake(new DecodeIssuePort)))
    val writeBackResult = IO(Input(new WriteBackResult))

    // These are place holder values for an actual implementation
    fetchIssueIntfce.ready              := false.B
    fetchIssueIntfce.expected.valid     := false.B
    fetchIssueIntfce.expected.PC        := 0.U

    decodeIssuePort.valid := 0.U
    decodeIssuePort.bits := (new DecodeIssuePort).Lit(
        _.instruction -> 0.U,
        _.PC          -> 0.U,
        _.rs1         -> 0.U,
        _.rs2         -> 0.U,
        _.immediate   -> 0.U
    )
}

object blankDecode extends  App {
    emitVerilog(new blankDecode)
}