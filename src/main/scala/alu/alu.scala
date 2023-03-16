package pipeline.alu

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._
import pipeline._

class DecodeIssuePort extends Bundle {
    val PC          = UInt(64.W)
    val instruction = UInt(32.W)
    val rs1         = UInt(64.W)
    val rs2         = UInt(64.W)
    val immediate   = UInt(64.W)
}

class BranchResult extends Bundle {
    val target = UInt(64.W)
    val valid = Bool()
}

/**
  * Functionality:
  * Assuming no stall conditions arise. The input is combinationally processed
  * in a single cycle. Then is presented to the memory unit. The outputs are
  * directly driven by registers("aluIssueBuffer" in this case).
  * 
  * If a instruction presented to the memory unit is not accepted in the same
  * cycle(stall) an instruction is accepted from decode unit then, the instruction is
  * buffered. The instruction is not processed until the old is accepted by 
  * memory unit. 
  * 
  * Instruction are processed and presented to memory unit in-order they're 
  * accepted from the decode unit.
  * 
  * Branch are results are broadcasted to fetch unit in the next cycle after it
  * is accepted from decode.
  * 
  * aluTemplate codifies the above information. Exact implmentation of instructions
  * are given by alu module.
  */

abstract class aluTemplate extends Module {
    val decodeIssuePort = IO(new handshake(new DecodeIssuePort()))
    val aluIssuePort = IO(Flipped(new handshake(new AluIssuePort())))
    val branchResult = IO(Output(new BranchResult()))

    def getsWriteBack(recievedIns: DecodeIssuePort): AluIssuePort
    def resolveBranch(recievedIns: DecodeIssuePort): BranchResult

    val mulReqValid = RegInit(false.B)
    def isExtnM(instruction: UInt) = (instruction(31, 25) === 1.U) && (instruction(6, 2) === BitPat("b011?0"))

    val mulDivUnit = Module(new mExten)

    when(!mulReqValid) {
        mulReqValid := decodeIssuePort.valid && decodeIssuePort.ready && isExtnM(decodeIssuePort.bits.instruction)}.otherwise {mulReqValid := !mulDivUnit.input.ready}

    mulDivUnit.input.valid := mulReqValid

    val decodeIssueBuffer = RegInit(new Bundle{
        val valid   = Bool()
        val bits    = (new DecodeIssuePort())
    }.Lit(
        _.valid -> false.B,
        _.bits.PC -> 0.U,
        _.bits.instruction -> 0.U,
        _.bits.rs1 -> 0.U,
        _.bits.rs2 -> 0.U,
        _.bits.immediate -> 0.U
    ))

    mulDivUnit.input.bits.instruction := decodeIssueBuffer.bits.instruction
    mulDivUnit.input.bits.src1 := decodeIssueBuffer.bits.rs1
    mulDivUnit.input.bits.src2 := decodeIssueBuffer.bits.rs2

    val aluIssueBuffer = RegInit(new Bundle{
        val valid   = Bool()
        val bits    = (new AluIssuePort())
    }.Lit(
        _.valid -> false.B,
        _.bits.nextInstPtr -> 0.U,
        _.bits.instruction -> 0.U,
        _.bits.aluResult -> 0.U,
        _.bits.rs2 -> 0.U
    ))

    // no instructions are accepted while there is a buffered instruction
    decodeIssuePort.ready := !decodeIssueBuffer.valid

    // an instruction is buffered if the older instruction is not yet accepted by the memory access unit
    when((decodeIssuePort.ready && decodeIssuePort.valid && !isExtnM(decodeIssuePort.bits.instruction)) && (aluIssuePort.valid && !aluIssuePort.ready)) {
        decodeIssueBuffer.bits  := decodeIssuePort.bits
        decodeIssueBuffer.valid := true.B
    }.elsewhen(decodeIssueBuffer.valid && aluIssuePort.valid && aluIssuePort.ready && !isExtnM(decodeIssueBuffer.bits.instruction)) {
        // older instruction was accepted by memory unit, processing the buffered instruction
        decodeIssueBuffer.valid := false.B
    }.elsewhen(!decodeIssueBuffer.valid && isExtnM(decodeIssuePort.bits.instruction) && decodeIssuePort.valid) {
        decodeIssueBuffer.bits  := decodeIssuePort.bits
        decodeIssueBuffer.valid := true.B
    }.elsewhen(decodeIssueBuffer.valid && mulDivUnit.output.valid && isExtnM(decodeIssueBuffer.bits.instruction) && (!aluIssueBuffer.valid || aluIssuePort.ready)) {
        decodeIssueBuffer.valid := false.B
    }
    // buffered instruction is given priority
    val processingEntry = Mux(decodeIssueBuffer.valid, decodeIssueBuffer.bits, decodeIssuePort.bits) 

    val entryValid = decodeIssueBuffer.valid || decodeIssuePort.valid

    when(entryValid && (!aluIssueBuffer.valid || aluIssuePort.ready) && !isExtnM(processingEntry.instruction)) {
        // either the old instruction was accepted, or no old instruction
        aluIssueBuffer.bits := getsWriteBack(processingEntry)
        aluIssueBuffer.valid := true.B
    }.elsewhen(!entryValid && (aluIssuePort.valid && aluIssuePort.ready)) {
        aluIssueBuffer.valid := false.B
    }.elsewhen(entryValid && (!aluIssueBuffer.valid || aluIssuePort.ready) && isExtnM(decodeIssueBuffer.bits.instruction) && mulDivUnit.output.valid) {
        aluIssueBuffer.valid := true.B
        aluIssueBuffer.bits.nextInstPtr := decodeIssueBuffer.bits.PC + 4.U
        aluIssueBuffer.bits.instruction := decodeIssueBuffer.bits.instruction
        aluIssueBuffer.bits.rs2 := decodeIssueBuffer.bits.rs2
        aluIssueBuffer.bits.aluResult := mulDivUnit.output.bits
    }.elsewhen((!aluIssueBuffer.valid || aluIssuePort.ready) && (decodeIssuePort.valid && decodeIssuePort.ready && isExtnM(decodeIssuePort.bits.instruction))) {
        aluIssueBuffer.valid := false.B
    }

    mulDivUnit.output.ready := (!aluIssueBuffer.valid || aluIssuePort.ready) && isExtnM(decodeIssueBuffer.bits.instruction) && decodeIssueBuffer.valid

    aluIssuePort.valid := aluIssueBuffer.valid
    aluIssuePort.bits   := aluIssueBuffer.bits

    val branchResultDriver = RegInit((new BranchResult).Lit(_.target -> 0.U, _.valid -> false.B))
    branchResultDriver := resolveBranch(decodeIssuePort.bits)

    branchResult := branchResultDriver
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

        /**
          * For aithmatic operations that includes immedaite, rs2 will have that immediate.
          */

        result.aluResult := {
            /**
              * 64 bit operations, indexed with funct3, op-imm, op
              */
            val arithmetic64 = VecInit.tabulate(8)(i => i match {
                case 0 => Mux(Cat(recievedIns.instruction(30), recievedIns.instruction(5)) === "b11".U, rs1 - rs2, rs1 + rs2)
                case 1 => (rs1 << rs2(5, 0))
                case 2 => (rs1.asSInt < rs2.asSInt).asUInt
                case 3 => (rs1 < rs2).asUInt
                case 4 => (rs1 ^ rs2)
                case 5 => Mux(recievedIns.instruction(30).asBool, (rs1.asSInt >> rs2(5, 0)).asUInt, (rs1 >> rs2(5, 0)))
                case 6 => (rs1 | rs2)
                case 7 => (rs1 & rs2)
            })(recievedIns.instruction(14, 12))
            val result64 = Mux(isExtnM(recievedIns.instruction), mulDivUnit.output.bits, arithmetic64)

            /**
              * 32 bit operations, indexed with funct3, op-imm-32, op-32
              */
            val arithmetic32 = VecInit.tabulate(4)(i => i match {
                case 0 => Mux(Cat(recievedIns.instruction(30), recievedIns.instruction(5)) === "b11".U, result32bit(rs1 - rs2), result32bit(rs1 + rs2)) // add & sub
                case 1 => (result32bit(rs1 << rs2(4, 0))) // sll\iw
                case 2 => (result32bit(rs1 << rs2(4, 0))) // filler
                case 3 => Mux(recievedIns.instruction(30).asBool, result32bit((rs1(31, 0).asSInt >> rs2(4, 0)).asUInt), result32bit(rs1(31, 0) >> rs2(4, 0))) // sra\l\iw
            })(Cat(recievedIns.instruction(14), recievedIns.instruction(12)))
            val result32 = Mux(isExtnM(recievedIns.instruction), mulDivUnit.output.bits, arithmetic32)

            /**
              * Taken from register mapping in the instruction listing risc-spec
              */
            VecInit.tabulate(7)(i => i match {
                case 0 => (rs1 + immediate) // address calculation for memory access
                case 1 => (pc + 4.U) // jal link address
                case 2 => (pc + 4.U) //(63, 0) // filler
                case 3 => (pc + 4.U) //(63, 0) jalr link address
                case 4 => result64 // (63, 0) op-imm, op
                case 5 => (immediate + Mux(recievedIns.instruction(5).asBool, 0.U, pc)) // (63, 0) // lui and auipc
                case 6 => result32 // op-32, op-imm-32
            })(recievedIns.instruction(4, 2))
        }
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

        result.target := Seq(
            BitPat("b?????????????????????????1101111") -> (pc + immediate), // jal
            BitPat("b?????????????????????????1100111") -> (rs1 + immediate), //jalr
            BitPat("b?????????????????????????1100011") -> brachNextAddress, // branches
        ).foldRight((pc + 4.U))(getResult)

        result.valid := recievedIns.instruction(6, 0) === BitPat("b110??11") && ((decodeIssuePort.ready && decodeIssuePort.valid)) 
        result 
    }
}

object alu extends App {
    (new stage.ChiselStage).emitVerilog(new alu())
}
