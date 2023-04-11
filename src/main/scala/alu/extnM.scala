//NOT FINALZED

package pipeline.alu

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._
import pipeline._
import extnM._
import m_constants._


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

  val dividerS32     = Module(new booth_divider_S(32))
  val dividerU32     = Module(new booth_divider_U(32))
  val dividerS64     = Module(new booth_divider_S(64))
  val dividerU64     = Module(new booth_divider_U(64))
  
  val multiplierU32  = Module(new booth_multiplier_U(32))
  val multiplierS64  = Module(new booth_multiplier_S(64))
  val multiplierU64  = Module(new booth_multiplier_U(64))

  val result = Reg(UInt(64.W))
  val int_result = Reg(UInt(64.W))  //Intermittent result register

  when(input.valid && input.ready) {

    switch(input.bits.instruction(6,0)){
      is(m32.U){
        switch(input.bits.instruction(14,12)){
          is (0.U){                             //mul
            val multiplierS32_1  = Module(new booth_multiplier_S(32))  //Signed 32 bit multiplier for mul
            multiplierS32_1.io.multiplier    := input.bits.src1.asSInt
            multiplierS32_1.io.multiplicand  := input.bits.src2.asSInt
            int_result                    := multiplierS32_1.io.product(31,0).asUInt
          }
          is (1.U){                             //mulh
            val multiplierS32_2  = Module(new booth_multiplier_S(32))  //Signed 32 bit multiplier for mulh
            multiplierS32_2.io.multiplier    := input.bits.src1.asSInt
            multiplierS32_2.io.multiplicand  := input.bits.src2.asSInt
            int_result                    := multiplierS32_2.io.product(63,32).asUInt
          }
          is (2.U){                             //mulhsu - Yet to be implemented
            multiplier64.io.multiplier    := input.bits.src1.asSInt
            multiplier64.io.multiplicand  := input.bits.src2.asSInt
            int_result                    := multiplier64.io.product(63,32).asUInt
          }
          is (3.U){                             //mulhu
            multiplierU32.io.multiplier    := input.bits.src1
            multiplierU32.io.multiplicand  := input.bits.src2
            int_result                    := multiplierU32.io.product(63,32)
          }
          is (4.U){                             //div
            val dividerS32     = Module(new booth_divider_S(32))
            dividerS32.io.dividend    := input.bits.src1
            dividerS32.io.divisor     := input.bits.src2
            dividerS32.io.signed      := 1.U
            int_result                := dividerS32.io.quotient
          }
        }

      }
      is(m64.U){

      }
    }

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
