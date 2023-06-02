package pipeline.alu

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._
import pipeline._


class mExten extends Module {
  val input = IO(Flipped(DecoupledIO(new Bundle{
    val src1 = UInt(64.W)
    val src2 = UInt(64.W)
    val instruction = UInt(64.W)
  })))

  val output = IO(DecoupledIO(UInt(64.W)))

  val status = RegInit(true.B)
  when(status) {status := !input.valid}
  .otherwise {status := !output.ready}

  val result = Reg(UInt(64.W))
  when(input.valid && input.ready) {
    result := {
      val result64 = VecInit.tabulate(8)(i => i match {
        case 0 => (input.bits.src1.asSInt * input.bits.src2.asSInt).asUInt // mul
        case 1 => (input.bits.src1.asSInt * input.bits.src2.asSInt)(127, 64).asUInt // mulh
        case 2 => (Cat(input.bits.src1(63), input.bits.src1).asSInt * Cat(0.U(1.W) ,input.bits.src2).asSInt)(127, 64).asUInt // mulhsu
        case 3 => (input.bits.src1 * input.bits.src2)(127, 64) // mulhu
        case 4 => Mux(input.bits.src2 === 0.U, ~(0.U(64.W)), (input.bits.src1.asSInt / input.bits.src2.asSInt).asUInt) // div
        case 5 => Mux(input.bits.src2 === 0.U, ~(0.U(64.W)), (input.bits.src1 / input.bits.src2))// divu
        case 6 => (input.bits.src1.asSInt - Mux(input.bits.src2 === 0.U, (-1).S(64.W), (input.bits.src1.asSInt / input.bits.src2.asSInt))*(input.bits.src2.asSInt) ).asUInt  // rem
        case 7 => (input.bits.src1 - Mux(input.bits.src2 === 0.U, ~(0.U(64.W)), (input.bits.src1 / input.bits.src2))*(input.bits.src2) ) // remu
      })(input.bits.instruction(14, 12))

      val src1W = input.bits.src1(31,0)
      val src2W = input.bits.src2(31,0)
      val result32W = VecInit.tabulate(8)(i => i match {
        case 0 => (src1W.asSInt * src2W.asSInt)(31,0).asUInt // mulw
        case 1 => 0.U // not defined
        case 2 => 0.U // not defined
        case 3 => 0.U // not defined
        case 4 => Mux(src2W === 0.U, ~(0.U(32.W)), (src1W.asSInt / src2W.asSInt).asUInt)(31,0)  // divw
        case 5 => Mux(src2W === 0.U, ~(0.U(64.W)), (src1W / src2W))(31,0) // divuw
        case 6 => ((src1W.asSInt - Mux(src2W === 0.U, (-1).S(32.W), (src1W.asSInt / src2W.asSInt))*(src2W.asSInt) ).asUInt)(31,0) // remw
        case 7 => (src1W - Mux(src2W === 0.U, ~(0.U(32.W)), (src1W / src2W))*(src2W) )(31,0)  // remuw
      })(input.bits.instruction(14, 12))
      val result32 = Cat(Fill(32, result32W(31)), result32W)

      Mux(input.bits.instruction(3).asBool, result32, result64)
    }
  }

  input.ready := status
  output.valid := !status
  output.bits := result
}

object mExten extends App {
  emitVerilog(new mExten)
}
