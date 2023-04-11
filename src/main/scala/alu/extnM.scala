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

    switch(input.bits.instruction(3).asBool){
      is(true.B){
        switch(input.bits.instruction(14,12)){
          is (0.U){                             //mulw
            multiplierS32.io.multiplier    := input.bits.src1(31,0).asSInt
            multiplierS32.io.multiplicand  := input.bits.src2(31,0).asSInt
            int_result                    := Cat(Seq.fill(32)(multiplierS32.io.product(31)),multiplierS32.io.product(31,0))
          }
          is (4.U){                             //divw
            dividerS32.io.dividend    := input.bits.src1(31,0).asSInt
            dividerS32.io.divisor     := input.bits.src2(31,0).asSInt
            dividerS32.io.signed      := 1.U
            int_result                := Cat(Seq.fill(32)dividerS32.io.quotient(31),dividerS32.io.quotient(31,0))
          }
          is (5.U){                              //divuw
            dividerU32.io.dividend    := input.bits.src1(31,0)
            dividerU32.io.divisor     := input.bits.src2(31,0)
            dividerU32.io.signed      := 0.U
            int_result                := Cat(Seq.fill(32)dividerU32.io.quotient(31),dividerU32.io.quotient(31,0))
          }
          is (6.U){                             //remw
            dividerS32.io.dividend    := input.bits.src1(31,0).asSInt
            dividerS32.io.divisor     := input.bits.src2(31,0).asSInt
            dividerS32.io.signed      := 1.U
            int_result                := Cat(Seq.fill(32)dividerS32.io.remainder(31),dividerS32.io.remainder(31,0))
          }
          is (7.U){                             //remuw
            dividerU32.io.dividend    := input.bits.src1(31,0)
            dividerU32.io.divisor     := input.bits.src2(31,0)
            dividerU32.io.signed      := 0.U
            int_result                := Cat(Seq.fill(32)dividerU32.io.remainder(31),dividerU32.io.remainder(31,0))
          }
        }
      }

      is(false.B){
        switch(input.bits.instruction(14,12)){
          is (0.U){                             //mul
            multiplierS64.io.multiplier    := input.bits.src1.asSInt
            multiplierS64.io.multiplicand  := input.bits.src2.asSInt
            int_result                    := multiplierS32.io.product(63,0)
          }
          is (1.U){                              //mulh
            multiplierS64.io.multiplier    := input.bits.src1.asSInt
            multiplierS64.io.multiplicand  := input.bits.src2.asSInt
            int_result                    := multiplierS32.io.product(127,64)
          }
          is (2.U){                               //mulhsu
            
          }
          is (3.U){                               //mulhu
            multiplierU64.io.multiplier    := input.bits.src1
            multiplierU64.io.multiplicand  := input.bits.src2
            int_result                    := multiplierU32.io.product(127,64)
          }
          is (4.U){                               //div
            dividerS64.io.dividend         := input.bits.src1.asSInt
            dividerS64.io.divisor          := input.bits.src2.asSInt
            dividerS64.io.signed           := 1.UInt
            int_result                     := dividerS64.io.quotient 
          }
          is (5.U){                               //divu
            dividerU64.io.dividend         := input.bits.src1
            dividerU64.io.divisor          := input.bits.src2
            dividerU64.io.signed           := 0.UInt
            int_result                     := dividerS64.io.quotient 
          }
          is (6.U){                               //rem
            dividerS64.io.dividend         := input.bits.src1.asSInt
            dividerS64.io.divisor          := input.bits.src2.asSInt
            dividerS64.io.signed           := 1.UInt
            int_result                     := dividerS64.io.remainder 
          }
          is (7.U){                               //remu
            dividerU64.io.dividend         := input.bits.src1.asSInt
            dividerU64.io.divisor          := input.bits.src2.asSInt
            dividerU64.io.signed           := 0.UInt
            int_result                     := dividerU64.io.remainder 
          }
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