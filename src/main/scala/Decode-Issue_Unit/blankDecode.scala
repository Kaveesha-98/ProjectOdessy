package pipeline.decode

import Constants._
import pipeline.ports.{DecodeIssuePort, WriteBackResult, handshake}

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class blankDecode extends Module {
    /**
      * This is a blank module. Only interfaces are defined. The outputs are driven with Literal values
      */
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
    val branchResult = IO(Output(new Bundle{
    	val valid = Bool()
    	val branchTaken = Bool()
    	val PC = UInt(64.W)
    	val targetAddress = UInt(64.W)
    }))
    
    branchResult := branchResult.cloneType.Lit(
    	_.valid 		-> false.B,
    	_.branchTaken 	-> false.B,
    	_.PC 			-> 0.U,
    	_.targetAddress -> 0.U
    )

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

class decodeHWTestbench extends Module {
    /**
      * This module is meant to test decode units. Since the 
      * decode unit contains the architectural state as well
      * some instructions are implmented to simulate cpu.
      * 
      * i.e. When an add instruction is passed through the decode
      * internal state of the decode changes(The destination register
      * will not be available for a proceeding instruction). Hence, the 
      * add instruction that is issued from the decode unit must be 
      * implemented in order for proper operation of decode unit for futher
      * testing.
      *  
      * Memory access operations are not implmented as they are too 
      * complicated to implement.
      */

    /**
      * A continous stream of instructions will be issued through
      * the below interface. Not exactly in the program order.
      */
    val fetchIssueIntfce = IO(new Bundle{
        val ready       = Input(Bool())
        val issued      = Output(Bool())
        val PC          = Input(UInt(64.W))
        val instruction = Input(UInt(64.W))
        val expected    = new Bundle{
            val valid       = Output(Bool())
            val PC          = Output(UInt(64.W))
        }
    })

    val dutDecode = Module(new blankDecode)
    /* connecting the decode to instruction stream */
    fetchIssueIntfce.PC             <> dutDecode.fetchIssueIntfce.PC
    fetchIssueIntfce.instruction    <> dutDecode.fetchIssueIntfce.instruction
    fetchIssueIntfce.expected       <> dutDecode.fetchIssueIntfce.expected

    /** applying condition for firing rule:
      * Two interfaces have to be ready and the PC presented by fetch must match the expected PC
      * from the decode(if it is valid).
      */
    val conditional_fetch_issue = Seq(fetchIssueIntfce, dutDecode.fetchIssueIntfce)
    conditional_fetch_issue.foreach(_.issued := (
        fetchIssueIntfce.ready && dutDecode.fetchIssueIntfce.ready &&
        (!dutDecode.fetchIssueIntfce.expected.valid || (dutDecode.fetchIssueIntfce.expected.PC === fetchIssueIntfce.expected.PC))
    ))
    
    val branchResult = IO(dutDecode.branchResult.cloneType)
    branchResult <> dutDecode.branchResult

    // For testing purposes this interface must be visible for outside
    val decodeIssuePort = IO(Flipped(dutDecode.decodeIssuePort.cloneType))
    decodeIssuePort <> dutDecode.decodeIssuePort
    // assumes pipeline is never blocked
    dutDecode.decodeIssuePort.ready := 1.U
    val immediate   = dutDecode.decodeIssuePort.bits.immediate
    val pc          = dutDecode.decodeIssuePort.bits.PC
    val rs1         = dutDecode.decodeIssuePort.bits.rs1
    val rs2         = dutDecode.decodeIssuePort.bits.rs2

    val resultsWriteBack = RegInit((new WriteBackResult).Lit(
        _.toRegisterFile -> 0.U,
        _.rd             -> 0.U,
        _.rdData         -> 0.U
    ))

    // Results are guarateed to return in next cycle
    def result32bit(res: UInt) =
        Cat(Fill(32, res(31)), res(31, 0))

    resultsWriteBack.rdData := MuxCase(0.U(64.W),
    Seq(
        BitPat("b?????????????????????????0110111") -> immediate, // lui
        BitPat("b?????????????????????????0010111") -> (immediate + pc), // auipc
        BitPat("b?????????????????????????1101111") -> (pc + 4.U), // jal
        BitPat("b?????????????????????????1100111") -> (pc + 4.U), //jalr
        BitPat("b?????????????????000?????0010011") -> (rs1 + immediate), // addi
        BitPat("b?????????????????010?????0010011") -> (rs1.asSInt < immediate.asSInt).asUInt, // slti
        BitPat("b?????????????????011?????0010011") -> (rs1 < immediate).asUInt, // sltiu
        BitPat("b?????????????????100?????0010011") -> (rs1 ^ immediate), // xori
        BitPat("b?????????????????110?????0010011") -> (rs1 | immediate), // ori
        BitPat("b?????????????????111?????0010011") -> (rs1 & immediate), // andi
        BitPat("b000000???????????001?????0010011") -> (rs1 << immediate(5, 0)), // slli
        BitPat("b000000???????????101?????0010011") -> (rs1 >> immediate(5, 0)), // srli
        BitPat("b010000???????????101?????0010011") -> (rs1.asSInt >> immediate(5, 0)).asUInt, // srai
        BitPat("b0000000??????????000?????0110011") -> (rs1 + rs2), // add
        BitPat("b0100000??????????000?????0110011") -> (rs1 - rs2), // sub
        BitPat("b0000000??????????001?????0110011") -> (rs1 << rs2(5, 0)), // sll
        BitPat("b0000000??????????010?????0110011") -> (rs1.asSInt < rs2.asSInt), // slt
        BitPat("b0000000??????????011?????0110011") -> (rs1 < rs2), // sltu
        BitPat("b0000000??????????100?????0110011") -> (rs1 ^ rs2), // xor
        BitPat("b0000000??????????101?????0110011") -> (rs1 >> rs2), // srl
        BitPat("b0100000??????????101?????0110011") -> (rs1.asSInt >> rs2(5, 0)).asUInt, // sra
        BitPat("b0000000??????????110?????0110011") -> (rs1 | rs2), // or
        BitPat("b0000000??????????111?????0110011") -> (rs1 & rs2), // and
        BitPat("b?????????????????000?????0011011") -> result32bit(rs1 + immediate), // addiw
        BitPat("b0000000??????????001?????0011011") -> result32bit(rs1 << immediate(4, 0)), // slliw
        BitPat("b0000000??????????101?????0011011") -> result32bit(rs1(31, 0) >> immediate(4, 0)), // srliw
        BitPat("b0100000??????????101?????0011011") -> result32bit((rs1(31, 0).asSInt >> immediate(4, 0)).asUInt), // sraiw
        BitPat("b0000000??????????000?????0111011") -> result32bit(rs1 + rs2), // addw
        BitPat("b0100000??????????000?????0111011") -> result32bit(rs1 - rs2), // subw
        BitPat("b0000000??????????001?????0111011") -> result32bit(rs1(31, 0) << rs2(4, 0)), // sllw
        BitPat("b0000000??????????101?????0111011") -> result32bit(rs1(31, 0) >> rs2), // srlw
        BitPat("b0100000??????????101?????0111011") -> result32bit((rs1(31, 0).asSInt >> rs2(4, 0)).asUInt)// sraw
    ).map{case(bitpat, aluResult) => (bitpat === dutDecode.decodeIssuePort.bits.instruction) -> aluResult})

    resultsWriteBack.rd := dutDecode.decodeIssuePort.bits.instruction(11, 7)
    resultsWriteBack.toRegisterFile := dutDecode.decodeIssuePort.valid.asBool && (
        Seq("b0110111".U, "b0010111".U, "b1101111".U, "b1100111".U, "b0010011".U, "b0110011".U, "b0011011".U, "b0111011".U)
        .map(_ === dutDecode.decodeIssuePort.bits.instruction(6, 0)).reduce(_ || _)
    )

    dutDecode.writeBackResult <> resultsWriteBack
}

object decodeHWTestbench extends App {
    emitVerilog(new decodeHWTestbench)
}
