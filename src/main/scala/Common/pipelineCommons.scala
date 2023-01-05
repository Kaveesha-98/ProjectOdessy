package pipeline.ports

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class handshake[T <: Data](gen: T) extends Bundle{
    val valid = Input(Bool())
    val ready = Output(Bool())
    val bits = Input(gen)
}

class MemoryIssuePort extends Bundle {
    val instruction = UInt(32.W)
    val nextInstPtr = UInt(64.W)
    val aluResult   = UInt(64.W)
}

class AluIssuePort extends Bundle {
    val instruction = UInt(32.W)
    val nextInstPtr = UInt(64.W)
    val aluResult   = UInt(64.W)
    val rs2         = UInt(64.W)
}

class DecodeIssuePort extends Bundle {
    val instruction = UInt(32.W)
    val PC = UInt(64.W)
    val opCode = UInt(7.W)
    val rs1 = UInt(64.W)
    val rs2 = UInt(64.W)
    val immediate = UInt(64.W)
}

class FetchIssuePort extends Bundle {
    val instruction = UInt(32.W)
    val PC = UInt(32.W)
}

class WriteBackResult extends Bundle {
    val toRegisterFile = UInt(1.W)
    val rd             = UInt(5.W)
    val rdData         = UInt(64.W)
}

class test extends Module {
    val inputs = IO(new handshake(new MemoryIssuePort()))
    val outputs = IO(Flipped(new handshake(new MemoryIssuePort())))

    inputs := outputs
}

object test extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new test())
}