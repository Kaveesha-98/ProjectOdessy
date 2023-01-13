package pipeline.alu

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import pipeline._
import pipeline.ports._

abstract class aluTemplate extends Module {
    val decodeIssuePort = IO(new handshake(new DecodeIssuePort()))
    val aluIssuePort = IO(Flipped(new handshake(new AluIssuePort())))
    val branchResult = IO(Output(new BranchResult()))

    def getsWriteBack(recievedIns: DecodeIssuePort): AluIssuePort
    def resolveBranch(recievedIns: DecodeIssuePort): BranchResult

    val passThrough :: waitOnMem :: execBuffIns :: flush :: Nil = Enum(4)
    val stateReg = RegInit(passThrough)

    /**
      * If memory unit is not ready, then the instruction accepted in this
      * cycle must be buffered.
      */
    val bufferdInstruction = Reg(new DecodeIssuePort())
    when(stateReg === passThrough && !aluIssuePort.ready){
        bufferdInstruction := decodeIssuePort.bits
    }

    /**
      * Instruction is passed through either the buffered instruction(priority)
      * or the instruction on the docodeIssuePort
      */
    val issuePortBits = Reg(new AluIssuePort())
    when(stateReg =/= waitOnMem) {
        issuePortBits := getsWriteBack(Mux(stateReg === passThrough, decodeIssuePort.bits, bufferdInstruction))
    }

    /**
      * Branch results are broadcasted in the next
      * cycle to the decode and fetch units
      */
    val branchResultDriver = RegInit((new BranchResult).Lit(
        _.valid -> false.B,
        _.nextInstPtr -> 0.U,
        _.PC -> 0.U,
        _.predicted -> false.B,
        _.isBranch -> false.B,
        _.branchTaken -> false.B
        ))
    val branchResolvedResults = resolveBranch(decodeIssuePort.bits)
    when(stateReg === passThrough) {
        branchResultDriver := branchResolvedResults
    }.otherwise {
        branchResultDriver.valid := false.B
    }

    val aluIssueValid = RegInit(false.B)
    switch(stateReg){
        is(passThrough) {aluIssueValid := decodeIssuePort.valid}
        is(execBuffIns)  {aluIssueValid := true.B}
    }

    switch(stateReg) {
        is(passThrough) { 
            when(decodeIssuePort.valid && !branchResolvedResults.predicted) { stateReg := flush } 
            .elsewhen(decodeIssuePort.valid && !aluIssuePort.ready) { stateReg := waitOnMem }
        }
        is(waitOnMem)   { when(aluIssuePort.ready){ stateReg := execBuffIns }}
        is(execBuffIns) { stateReg := passThrough }
        is(flush) { stateReg := passThrough }
    }

    aluIssuePort.bits := issuePortBits
    aluIssuePort.valid := aluIssueValid
    branchResult := branchResultDriver
    decodeIssuePort.ready := stateReg === passThrough
}

class alu extends aluTemplate {
    def getsWriteBack(recievedIns: DecodeIssuePort): AluIssuePort = {
        val result = Wire(new AluIssuePort)
        def getResult(pattern: (chisel3.util.BitPat, chisel3.UInt), prev: UInt) = pattern match {
            case (bitpat, result) => Mux(recievedIns.instruction === bitpat, result, prev)
        }

        def result32bit(res: UInt) =
            Cat(Fill(32, res(31)), res(31, 0))

        val immediate = recievedIns.immediate
        val pc = recievedIns.PC
        val rs1 = recievedIns.rs1
        val rs2 = recievedIns.rs2

        result.aluResult := Seq(
            BitPat("b?????????????????????????0110111") -> immediate, // lui
            BitPat("b?????????????????????????0010111") -> (immediate + pc), // auipc
            BitPat("b?????????????????????????1101111") -> (pc + 4.U), // jal
            BitPat("b?????????????????????????1100111") -> (pc + 4.U), //jalr
            BitPat("b?????????????????????????0?00011") -> (rs1 + immediate), // loads and stores
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
        ).foldRight(0.U(64.W))(getResult)

        val branchTaken = (Seq(
            rs1 === rs2,
            rs1 =/= rs2,
            rs1.asSInt < rs2.asSInt,
            rs1.asSInt >= rs2.asSInt,
            rs1 < rs2,
            rs1 >= rs2
        ).zip(Seq(0, 1, 4, 5, 6, 7).map(i => i.U === recievedIns.instruction(14, 12))
        ).map(condAndMatch => condAndMatch._1 && condAndMatch._2).reduce(_ || _))

        val brachNextAddress = Mux(branchTaken, (pc + immediate), (pc + 4.U))

        result.nextInstPtr := Seq(
            BitPat("b?????????????????????????1101111") -> (pc + immediate), // jal
            BitPat("b?????????????????????????1100111") -> (rs1 + immediate), //jalr
            BitPat("b?????????????????????????1100011") -> brachNextAddress, // branches
        ).foldRight((pc + 4.U))(getResult)
        result.instruction := recievedIns.instruction
        result.rs2 := recievedIns.rs2
        result
    }
    def resolveBranch(recievedIns: DecodeIssuePort): BranchResult = {
        val result = Wire(new BranchResult)
        def getResult(pattern: (chisel3.util.BitPat, chisel3.UInt), prev: UInt) = pattern match {
            case (bitpat, result) => Mux(recievedIns.instruction === bitpat, result, prev)
        }

        val immediate = recievedIns.immediate
        val pc = recievedIns.PC
        val rs1 = recievedIns.rs1
        val rs2 = recievedIns.rs2
        val predNextAddr = recievedIns.predNextAddr

        val branchTaken = (Seq(
            rs1 === rs2,
            rs1 =/= rs2,
            rs1.asSInt < rs2.asSInt,
            rs1.asSInt >= rs2.asSInt,
            rs1 < rs2,
            rs1 >= rs2
        ).zip(Seq(0, 1, 4, 5, 6, 7).map(i => i.U === recievedIns.instruction(14, 12))
        ).map(condAndMatch => condAndMatch._1 && condAndMatch._2).reduce(_ || _))

        val brachNextAddress = Mux(branchTaken, (pc + immediate), (pc + 4.U))

        val nextInstPtr = Seq(
            BitPat("b?????????????????????????1101111") -> (pc + immediate), // jal
            BitPat("b?????????????????????????1100111") -> (rs1 + immediate), //jalr
            BitPat("b?????????????????????????1100011") -> brachNextAddress, // branches
        ).foldRight((pc + 4.U))(getResult)

        result.nextInstPtr := nextInstPtr
        result.PC := pc
        result.predicted := nextInstPtr === predNextAddr
        result.isBranch := recievedIns.instruction(6, 0) === BitPat("b110??11")
        result.branchTaken := branchTaken

        result.valid := (recievedIns.instruction(6, 0) === BitPat("b110??11") || 
            nextInstPtr =/= predNextAddr)
        result 
    }
}

object alu extends App {
    (new stage.ChiselStage).emitVerilog(new alu())
}