package pipeline

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._

class singleCycleHart extends Module {
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

    val dataPort = IO(new Bundle{
        val a = new memory.channel_a()
        val d = new memory.channel_d()
    })

    def extractImm(bigWire: UInt)(concats: Array[(Int, Int)]) = 
        Cat(concats.map(i => i match {
            case (1, x) => Fill(x, bigWire(31))
            case (0, x) => Fill(x, "b0".U)
            case (x, y) => bigWire(x, y)
        }))

    io.reqport_valid := 0.U
    io.reqport_addr := 0.U
    io.resport_ready := 0.U

    dataPort.a := dataPort.a.init()

    dataPort.d.ready := 0.U

    val pc = RegInit("h80000000".U(64.W))
    io.reqport_addr := pc
    io.reqport_valid := 1.U

    val registerFile = VecInit.tabulate(32)(i => {
        if (i == 0) {0.U(64.W)} else {Reg(UInt(64.W))}
    })

    val rs1 = io.resport_instr(11, 7)
    val rs2 = io.resport_instr(24, 20)

    val extractImmBits = extractImm(io.resport_instr)_

    val immediate = MuxLookup(io.resport_instr(6, 0), 0.U(64.W), Array(
        "b0110111".U -> extractImmBits(Array((31, 12), (0, 10))), // lui
        "b0010111".U -> extractImmBits(Array((31, 12), (0, 10))), // auipc
        "b1101111".U -> extractImmBits(Array((1, 12), (19, 12), (20, 20), (30, 21), (0, 1))), // jal
        "b1100111".U -> extractImmBits(Array((1, 20), (31, 20))), // jal
        "b1100011".U -> extractImmBits(Array((1, 21), (7, 7), (30, 25), (11, 8), (0, 1))),
        "b0000011".U -> extractImmBits(Array((1, 20), (31, 20))), // loads
        "b0100011".U -> extractImmBits(Array((1, 20), (31, 25), (11, 7))), // stores
        "b0010011".U -> extractImmBits(Array((1, 20), (31, 20))), // arithmetic immediates
        "b0011011".U -> extractImmBits(Array((1, 20), (31, 20))) //addiw
    ))

    val address = rs1 + immediate
    dataPort.a.data := ~0.U(64.W)

    // assume loads are always aligned
    val loadData = dataPort.d.data >> address(2, 0)

    def getResult(pattern: (chisel3.util.BitPat, chisel3.UInt), prev: UInt) = pattern match {
        case (bitpat, result) => Mux(io.resport_instr === bitpat, result, prev)
    }

    val writeBackData = Seq(
        BitPat("b?????????????????????????0110111") -> immediate, // lui
        BitPat("b?????????????????????????0010111") -> (immediate + pc), // auipc
        BitPat("b?????????????????????????1101111") -> (pc + 4.U), // jal
        BitPat("b?????????????????????????1100111") -> (pc + 4.U) //jalr
    ).foldRight(0.U(64.W))(getResult)
    
}

object singleCycleHart extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new singleCycleHart())
}