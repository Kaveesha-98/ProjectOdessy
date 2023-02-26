package common

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._


object common {
    val opcodes = Map(
        "lui"   -> "b????????????????????0110111",
        "auipc" -> "b????????????????????0010111",
        "jal"   -> "b????????????????????1101111",
        "jalr"  -> "b????????????000?????1100111",
        "beq"   -> "b????????????000?????1100011",
        "bne"   -> "b????????????001?????1100011",
        "blt"   -> "b????????????100?????1100011",
        "bge"   -> "b????????????101?????1100011",
        "bltu"  -> "b????????????110?????1100011",
        "bgeu"  -> "b????????????111?????1100011",
        "lb"    -> "b????????????000?????0000011",
        "lh"    -> "b????????????001?????0000011",
        "lw"    -> "b????????????010?????0000011",
        "lbu"   -> "b????????????100?????0000011",
        "lhu"   -> "b????????????000?????0000011",
        "sb"    -> "b????????????000?????0100011",
        "sh"    -> "b????????????001?????0100011",
        "sw"    -> "b????????????010?????0100011",
        "addi"  -> "b????????????000?????0010011",
        "addi"  -> "b????????????000?????0010011")

    def getreadInstr(instr: String = "ld x0, 8(x2)") = 
        "b????????????001?????0000011".replace('?', '0')
}

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

    class sourceReg extends Bundle {
        val opcode      = Output(UInt(3.W))
        val param       = Output(UInt(3.W))
        val size        = Output(UInt(z.W))
        val source      = Output(UInt(o.W))
        val address     = Output(UInt(a.W))
        val mask        = Output(UInt(w.W))
        val data        = Output(UInt((8*w).W))
        val valid       = Output(UInt(1.W))
    }

    def init() =
        (new sourceReg).Lit(
            _.opcode -> 0.U,
            _.param -> 0.U,
            _.size -> 0.U,
            _.source -> 0.U,
            _.address -> 0.U,
            _.mask -> 0.U,
            _.data -> 0.U,
            _.valid -> 0.U
        )

    def :=(sink: sourceReg):Unit = {
        this.opcode := sink.opcode
        this.param       := sink.param
        this.size        := sink.size
        this.source      := sink.source
        this.address     := sink.address
        this.mask        := sink.mask
        this.data        := sink.data
        this.valid       := sink.valid
    } 
}

class channel_d(
    val z : Int = 2,
    val o : Int = 1,
    val i : Int = 1,
    val w : Int = 8,
) extends Bundle {
    val opcode  = Input(UInt(3.W))
    val param   = Input(UInt(3.W))
    val size    = Input(UInt(z.W))
    val source  = Input(UInt(o.W))
    val sink    = Input(UInt(i.W))
    val data    = Input(UInt((8*w).W))
    val error   = Input(UInt(1.W))
    val valid   = Input(UInt(1.W))
    val ready   = Output(UInt(1.W))

    class sourceReg extends Bundle {
        val opcode      = Output(UInt(3.W))
        val param       = Output(UInt(3.W))
        val size        = Output(UInt(z.W))
        val source      = Output(UInt(o.W))
        val sink        = Input(UInt(i.W))
        val data        = Output(UInt((8*w).W))
        val error       = Input(UInt(1.W))
        val valid       = Output(UInt(1.W))
    }

    def init() =
        (new sourceReg).Lit(
            _.opcode -> 0.U,
            _.param -> 0.U,
            _.size -> 0.U,
            _.source -> 0.U,
            _.sink -> 0.U,
            _.data -> 0.U,
            _.error -> 0.U,
            _.valid -> 0.U
        )

    def :=(sink_source: sourceReg):Unit = {
        this.opcode      := sink_source.opcode
        this.param       := sink_source.param
        this.size        := sink_source.size
        this.source      := sink_source.source
        this.sink        := sink_source.sink
        this.data        := sink_source.data
        this.error       := sink_source.error
        this.valid       := sink_source.valid
    } 
}

