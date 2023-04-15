//NOT FINALZED

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
  val int_result = Reg(UInt(64.W))  //Intermittent result register

  when(input.valid && input.ready) {

    switch(input.bits.instruction(3).asBool){
      is(true.B){
        switch(input.bits.instruction(14,12)){
          is (0.U){                             //mulw
            val multiplierS32  = Module(new booth_multiplier_S(32))
            multiplierS32.io.multiplier    := input.bits.src1(31,0).asSInt
            multiplierS32.io.multiplicand  := input.bits.src2(31,0).asSInt
            int_result                    := Cat(Fill(32,multiplierS32.io.product(31)) ,multiplierS32.io.product(31,0))
          }
          is (4.U){                             //divw
            val dividerS32_1     = Module(new booth_divider_S(32))
            dividerS32_1.io.dividend    := input.bits.src1(31,0).asSInt
            dividerS32_1.io.divisor     := input.bits.src2(31,0).asSInt
            dividerS32_1.io.signed      := 1.U
            int_result                := Cat(Fill(32, dividerS32_1.io.quotient(31)) ,dividerS32_1.io.quotient(31,0))
          }
          is (5.U){                              //divuw
            val dividerU32_1     = Module(new booth_divider_U(32))
            dividerU32_1.io.dividend    := input.bits.src1(31,0)
            dividerU32_1.io.divisor     := input.bits.src2(31,0)
            dividerU32_1.io.signed      := 0.U
            int_result                := Cat(Fill(32, dividerU32_1.io.quotient(31)) ,dividerU32_1.io.quotient(31,0))
          }
          is (6.U){                             //remw
            val dividerS32_2     = Module(new booth_divider_S(32))
            dividerS32_2.io.dividend    := input.bits.src1(31,0).asSInt
            dividerS32_2.io.divisor     := input.bits.src2(31,0).asSInt
            dividerS32_2.io.signed      := 1.U
            int_result                := Cat(Fill(32, dividerS32_2.io.remainder(31)) ,dividerS32_2.io.remainder(31,0))
          }
          is (7.U){                             //remuw
            val dividerU32_2     = Module(new booth_divider_U(32))
            dividerU32_2.io.dividend    := input.bits.src1(31,0)
            dividerU32_2.io.divisor     := input.bits.src2(31,0)
            dividerU32_2.io.signed      := 0.U
            int_result                := Cat(Fill(32, dividerU32_2.io.remainder(31)) ,dividerU32_2.io.remainder(31,0))
          }
        }
      }

      is(false.B){
        switch(input.bits.instruction(14,12)){
          is (0.U){                             //mul
            val multiplierS64_1  = Module(new booth_multiplier_S(64))
            multiplierS64_1.io.multiplier    := input.bits.src1.asSInt
            multiplierS64_1.io.multiplicand  := input.bits.src2.asSInt
            int_result                    := multiplierS64_1.io.product(63,0).asUInt
          }
          is (1.U){                              //mulh
            val multiplierS64_2  = Module(new booth_multiplier_S(64))
            multiplierS64_2.io.multiplier    := input.bits.src1.asSInt
            multiplierS64_2.io.multiplicand  := input.bits.src2.asSInt
            int_result                    := multiplierS64_2.io.product(127,64).asUInt
          }
          is (2.U){                               //mulhsu
            
          }
          is (3.U){                               //mulhu
            val multiplierU64_1  = Module(new booth_multiplier_U(64))
            multiplierU64_1.io.multiplier    := input.bits.src1
            multiplierU64_1.io.multiplicand  := input.bits.src2
            int_result                    := multiplierU64_1.io.product(127,64)
          }
          is (4.U){                               //div
            val dividerS64_1     = Module(new booth_divider_S(64))
            dividerS64_1.io.dividend         := input.bits.src1.asSInt
            dividerS64_1.io.divisor          := input.bits.src2.asSInt
            dividerS64_1.io.signed           := 1.U
            int_result                     := dividerS64_1.io.quotient.asUInt 
          }
          is (5.U){                               //divu
            val dividerU64_1     = Module(new booth_divider_U(64))
            dividerU64_1.io.dividend         := input.bits.src1
            dividerU64_1.io.divisor          := input.bits.src2
            dividerU64_1.io.signed           := 0.U
            int_result                     := dividerU64_1.io.quotient 
          }
          is (6.U){                               //rem
            val dividerS64_2     = Module(new booth_divider_S(64))
            dividerS64_2.io.dividend         := input.bits.src1.asSInt
            dividerS64_2.io.divisor          := input.bits.src2.asSInt
            dividerS64_2.io.signed           := 1.U
            int_result                     := dividerS64_2.io.remainder.asUInt 
          }
          is (7.U){                               //remu
            val dividerU64_2     = Module(new booth_divider_U(64))
            dividerU64_2.io.dividend         := input.bits.src1
            dividerU64_2.io.divisor          := input.bits.src2
            dividerU64_2.io.signed           := 0.U
            int_result                     := dividerU64_2.io.remainder 
          }
      }
    }
    }

  //   result := {
  //     val result64 = VecInit.tabulate(8)(i => i match {
  //       case 0 => (input.bits.src1.asSInt * input.bits.src2.asSInt).asUInt // mul
  //       case 1 => 0.U // mulh
  //       case 2 => 0.U // mulhsu
  //       case 3 => (input.bits.src1 * input.bits.src2)(127, 64) // mulhu
  //       case 4 => 0.U // div
  //       case 5 => 0.U // divu
  //       case 6 => 0.U // rem
  //       case 7 => 0.U // remu
  //     })(input.bits.instruction(14, 12))

  //     val result32 = VecInit.tabulate(8)(i => i match {
  //       case 0 => 0.U // mulw
  //       case 1 => 0.U // not defined
  //       case 2 => 0.U // not defined
  //       case 3 => 0.U // not defined
  //       case 4 => 0.U // divw
  //       case 5 => 0.U // divuw
  //       case 6 => 0.U // remw
  //       case 7 => 0.U // remuw
  //     })(input.bits.instruction(14, 12))

  //     Mux(input.bits.instruction(3).asBool, result32, result64)
  //   }
  }

  result := int_result

  input.ready := status
  output.valid := !status
  output.bits := result
}

object mExten extends App {
  emitVerilog(new mExten)
}