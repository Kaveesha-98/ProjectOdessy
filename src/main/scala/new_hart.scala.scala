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
    io.resport_ready := 0.U

    dataPort.a := dataPort.a.init()

    dataPort.d.ready := 0.U

    val pc = RegInit("h80000000".U(64.W))
    io.reqport_addr := pc
    io.reqport_valid := 1.U

    val writesRegFile = Wire(Bool())
    val writeBackData = Wire(UInt(64.W))

    val REG_ARRAY = new Array[chisel3.UInt](32)
    val registerFile = VecInit.tabulate(32)(i => {
        if (i == 0) { REG_ARRAY(i) = 0.U(64.W) } //x0 is hardwired to ground
        else { 
            REG_ARRAY(i) = Reg(UInt(64.W))// when reset x2 is set to 32'h10000
            when (io.resport_instr(11, 7) === i.U && writesRegFile){
                REG_ARRAY(i) := writeBackData
            }
        }

        REG_ARRAY(i)
    })

    val rs1 = registerFile(io.resport_instr(19, 15))
    val rs2 = registerFile(io.resport_instr(24, 20))

    val extractImmBits = extractImm(io.resport_instr)_

    val immediate = MuxLookup(io.resport_instr(6, 0), 0.U(64.W), Array(
        "b0110111".U -> extractImmBits(Array((31, 12), (0, 12))), // lui
        "b0010111".U -> extractImmBits(Array((31, 12), (0, 12))), // auipc
        "b1101111".U -> extractImmBits(Array((1, 12), (19, 12), (20, 20), (30, 21), (0, 1))), // jal
        "b1100111".U -> extractImmBits(Array((1, 20), (31, 20))), // jal
        "b1100011".U -> extractImmBits(Array((1, 21), (7, 7), (30, 25), (11, 8), (0, 1))),
        "b0000011".U -> extractImmBits(Array((1, 20), (31, 20))), // loads
        "b0100011".U -> extractImmBits(Array((1, 20), (31, 25), (11, 7))), // stores
        "b0010011".U -> extractImmBits(Array((1, 20), (31, 20))), // arithmetic immediates
        "b0011011".U -> extractImmBits(Array((1, 20), (31, 20))) //addiw
    ))

    val address = rs1 + immediate

    // assume loads are always aligned
    val loadData = dataPort.d.data >> address(2, 0)
    val loadMask = VecInit.tabulate(4)(i => Cat(Fill((3-i)*8+1, loadData(8*i + 7) & ~io.resport_instr(14)), 0.U((8*i + 7).W)))

    def getResult(pattern: (chisel3.util.BitPat, chisel3.UInt), prev: UInt) = pattern match {
        case (bitpat, result) => Mux(io.resport_instr === bitpat, result, prev)
    }

    def result32bit(res: UInt) =
        Cat(Fill(32, res(31)), res(31, 0))

    writeBackData := Seq(
        BitPat("b?????????????????????????0110111") -> immediate, // lui
        BitPat("b?????????????????????????0010111") -> (immediate + pc), // auipc
        BitPat("b?????????????????????????1101111") -> (pc + 4.U), // jal
        BitPat("b?????????????????????????1100111") -> (pc + 4.U), //jalr
        BitPat("b?????????????????????????0000011") -> (loadData | loadMask(io.resport_instr(13, 12))), // loads
        BitPat("b?????????????????000?????0010011") -> (rs1 + immediate), // addi
        BitPat("b?????????????????010?????0010011") -> (rs1.asSInt < immediate.asSInt).asUInt, // slti
        BitPat("b?????????????????011?????0010011") -> (rs1 < immediate).asUInt, // sltiu
        BitPat("b?????????????????100?????0010011") -> (rs1 ^ immediate), // xori
        BitPat("b?????????????????110?????0010011") -> (rs1 | immediate), // ori
        BitPat("b?????????????????111?????0010011") -> (rs1 & immediate), // andi
        BitPat("b000000???????????001?????0010011") -> (rs1 << io.resport_instr(25, 20)), // slli
        BitPat("b000000???????????101?????0010011") -> (rs1 >> io.resport_instr(25, 20)), // srli
        BitPat("b010000???????????101?????0010011") -> (rs1.asSInt >> io.resport_instr(25, 20)).asUInt, // srai
        BitPat("b0000000??????????000?????0110011") -> (rs1 + rs2), // add
        BitPat("b0100000??????????000?????0110011") -> (rs1 - rs2), // sub
        BitPat("b0000000??????????001?????0110011") -> Mux(rs2(31, 5).orR, 0.U, rs1 << rs2(5, 0)), // sll
        BitPat("b0000000??????????010?????0110011") -> (rs1.asSInt < rs2.asSInt), // slt
        BitPat("b0000000??????????011?????0110011") -> (rs1 < rs2), // sltu
        BitPat("b0000000??????????100?????0110011") -> (rs1 ^ rs2), // xor
        BitPat("b0000000??????????101?????0110011") -> (rs1 >> rs2), // srl
        BitPat("b0100000??????????101?????0110011") -> (rs1.asSInt >> rs2).asUInt, // sra
        BitPat("b0000000??????????110?????0110011") -> (rs1 | rs2), // or
        BitPat("b0000000??????????111?????0110011") -> (rs1 & rs2), // and
        BitPat("b?????????????????000?????0011011") -> result32bit(rs1 + immediate), // addiw
        BitPat("b0000000??????????001?????0011011") -> result32bit(rs1 << io.resport_instr(24, 20)), // slliw
        BitPat("b0000000??????????101?????0011011") -> result32bit(rs1 >> io.resport_instr(24, 20)), // srliw
        BitPat("b0100000??????????101?????0011011") -> result32bit((rs1.asSInt >> io.resport_instr(24, 20)).asUInt), // sraiw
        BitPat("b0000000??????????000?????0111011") -> result32bit(rs1 + rs2), // addw
        BitPat("b0100000??????????000?????0111011") -> result32bit(rs1 - rs2), // subw
        BitPat("b0000000??????????001?????0111011") -> result32bit(Mux(rs2(31, 5).orR, 0.U, rs1(31, 0) << rs2(4, 0))), // sllw
        BitPat("b0000000??????????101?????0111011") -> result32bit(rs1(31, 0) >> rs2), // srlw
        BitPat("b0100000??????????101?????0111011") -> result32bit((rs1(31, 0).asSInt >> rs2).asUInt)// sraw
    ).foldRight(0.U(64.W))(getResult)
    
    writesRegFile := Seq(
        "b0000011".U, "b0010011".U, "b0110011".U, 
        "b0110111".U, "b0010111".U, "b1101111".U, "b1100111".U
    ).map(op => op === io.resport_instr(6, 0)).reduce(_ || _)

    val branchTaken = (Seq(
        rs1 === rs2,
        rs1 =/= rs2,
        rs1.asSInt < rs2.asSInt,
        rs1.asSInt >= rs2.asSInt,
        rs1 < rs2,
        rs1 >= rs2
    ).zip(Seq(0, 1, 4, 5, 6, 7).map(i => i.U === io.resport_instr(14, 12))
    ).map(condAndMatch => condAndMatch._1 && condAndMatch._2).reduce(_ || _))

    val brachNextAddress = Mux(branchTaken, (pc + immediate), (pc + 4.U))

    val nextpc = Seq(
        BitPat("b?????????????????????????1101111") -> (pc + immediate), // jal
        BitPat("b?????????????????????????1100111") -> (rs1 + immediate), //jalr
        BitPat("b?????????????????????????1100011") -> brachNextAddress, // branches
    ).foldRight((pc + 4.U))(getResult)

    pc := nextpc
}

object singleCycleHart extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new singleCycleHart())
}