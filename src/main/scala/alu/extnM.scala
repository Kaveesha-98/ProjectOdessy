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
        case 1 => 0.U // mulh
        case 2 => 0.U // mulhsu
        case 3 => (input.bits.src1 * input.bits.src2)(127, 64) // mulhu
        case 4 => 0.U // div
        case 5 => 0.U // divu
        case 6 => 0.U // rem
        case 7 => 0.U // remu
      })(input.bits.instruction(14, 12))

      val result32 = VecInit.tabulate(8)(i => i match {
        case 0 => 0.U // mulw
        case 1 => 0.U // not defined
        case 2 => 0.U // not defined
        case 3 => 0.U // not defined
        case 4 => 0.U // divw
        case 5 => 0.U // divuw
        case 6 => 0.U // remw
        case 7 => 0.U // remuw
      })(input.bits.instruction(14, 12))

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
