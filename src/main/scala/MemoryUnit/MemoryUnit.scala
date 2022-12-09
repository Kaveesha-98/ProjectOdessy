package pipeline.memory

import chisel3._
import chisel3.util._

/* class aluIssuePort extends DecoupledIO {
    val instruction = Input(UInt(32.W))
} */

class handshake[T <: Data](gen: T) extends Bundle {
    val ready = Output(Bool())
    val valid = Input(Bool())
    val bits = Input(gen)
}

class AluIssuePort extends Bundle {
    val instruction = UInt(32.W)
    val nextInstPtr = UInt(64.W)
    val aluResult = UInt(64.W)
    val rs2 = UInt(64.W)
}

class MemoryIssuePort extends Bundle {
    val instruction = UInt(32.W)
    val nextInstPtr = UInt(64.W)
    val aluResult = UInt(64.W)
}

class channel_a(
    val z : Int = 2,
    val o : Int = 1,
    val a : Int = 64,
    val w : Int = 8,
) extends Bundle {
    val opcode      = Output(UInt(3.W))
    val param       = Output(UInt(3.W))
    val size        = Output(UInt(z.W))
    val source      = Output(UInt(o.W))
    val address     = Output(UInt(a.W))
    val mask        = Output(UInt(w.W))
    val data        = Output(UInt((8*w).W))
    val valid       = Output(UInt(1.W))
    val ready       = Input(UInt(1.W))
}

class channel_d(
    val z : Int = 2,
    val o : Int = 1,
    val i : Int = 1,
    val w : Int = 8,
) extends Bundle {
    val opcode  = Input(UInt(3.W))
    val param   = Input(UInt(2.W))
    val size    = Input(UInt(z.W))
    val source  = Input(UInt(o.W))
    val sink    = Input(UInt(i.W))
    val data    = Input(UInt((8*w).W))
    val error   = Input(UInt(1.W))
    val valid   = Input(UInt(1.W))
    val ready   = Output(UInt(1.W))
}

class MemoryUnit extends Module {
    val io = IO(new Bundle(){
        val aluIssuePort = new handshake(new AluIssuePort())
        val memoryIssuePort = Flipped(new handshake(new MemoryIssuePort()))
        val memPort = new Bundle{
            val a = new channel_a()
            val d = new channel_d()
        }
    })

    val pass_through :: wait_mem_req :: wait_mem_resp :: wait_writeback :: Nil = Enum(4)



    io.aluIssuePort.ready := true.B

    io.memoryIssuePort.valid := false.B
    io.memoryIssuePort.bits.instruction := 0.U
    io.memoryIssuePort.bits.nextInstPtr := 0.U
    io.memoryIssuePort.bits.aluResult := 0.U

    io.memPort.a.opcode      := 0.U
    io.memPort.a.param       := 0.U
    io.memPort.a.size        := 0.U
    io.memPort.a.source      := 0.U
    io.memPort.a.address     := 0.U
    io.memPort.a.mask        := 0.U
    io.memPort.a.data        := 0.U
    io.memPort.a.valid       := false.B

    io.memPort.d.ready := false.B
}

object ALU extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new MemoryUnit())
}