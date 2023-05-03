package pipeline.alu

import chisel3._
import chisel3.util._
//import common._

class booth_mult_substep_U(N:Int) extends Module{
    val io = IO(new Bundle{
        val acc = Input(UInt(N.W))
        val Q = Input(UInt(N.W))
        val q0 = Input(UInt(1.W))
        val multiplicand = Input(UInt(N.W))
        val next_acc = Output(UInt(N.W))
        val next_Q = Output(UInt(N.W))
        val q0_next = Output(UInt(1.W))
    })



    val int_ip = Wire(UInt(N.W))   //Output to be fed into the 64 bit adder subtractor

    // //PRE
    // //initiating getOnesComplement module and making the connections
    // val g0 = Module(new(getOnesComplement))

    // g0.io.cin := io.Q(0)
    // g0.io.i1 := io.multiplicand 
    // int_ip := g0.io.onesComp

    //REV
    when (io.Q(0).asUInt === 1.U){
        int_ip  := ~io.multiplicand.asUInt
    } .otherwise{
        int_ip  := io.multiplicand.asUInt
    }

    val addsub_temp = Wire(UInt(N.W))  //Output to be used in the logic loop
    
    // //PRE
    // //initating the addsub_64 module and making the connections
    // val as0 = Module(new addsub(N))

    // as0.io.cin := io.Q(0)
    // as0.io.onesComp_ip := int_ip
    // as0.io.i0 := io.acc
    // addsub_temp := as0.io.sum

    //REV
    addsub_temp := int_ip + io.Q(0).asUInt + io.acc.asUInt

    //logic loop

    //temporary variables to assign to
    val next_Q_temp = Wire(UInt((N-1).W))
    val next_acc_temp = Wire(UInt((N-1).W))
    val next_Q_MSB = Wire(UInt(1.W))
    val next_acc_MSB = Wire(UInt(1.W))

    when (io.Q(0) === io.q0){
        io.q0_next := io.Q(0)
        next_Q_temp := io.Q >> 1
        next_Q_MSB := io.acc(0)
        next_acc_temp := io.acc >> 1
        when (io.acc(N-1) === 1.U){
            next_acc_MSB := 1.U
        }.otherwise{
            next_acc_MSB := 0.U
        }
    }.otherwise{
        io.q0_next := io.Q(0)
        next_Q_temp := io.Q >> 1
        next_Q_MSB := addsub_temp(0)
        next_acc_temp := addsub_temp >> 1
        when (addsub_temp(N-1) === 1.U){
            next_acc_MSB := 1.U
        }.otherwise{
            next_acc_MSB := 0.U
        }
    }

    io.next_Q := Cat(next_Q_MSB, next_Q_temp)
    io.next_acc := Cat(next_acc_MSB, next_acc_temp)

}

object boothMultSubstepU extends App {
  println("Generating the Booth Multiplier Substep hardware")
  (new chisel3.stage.ChiselStage).emitVerilog(new booth_mult_substep_U(64), Array("--target-dir","verilog/"))

}