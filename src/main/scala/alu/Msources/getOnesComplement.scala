package extnM

import chisel3._
import chisel3.util._


class xor2 extends Module{
    val io = IO(new Bundle{
        val a = Input(UInt(1.W))
        val b = Input(UInt(1.W))
        val out = Output(UInt(1.W))

    })

    io.out := io.a ^ io.b
}


class getOnesComplement(N: Int=64) extends Module{
    val io = IO(new Bundle{
        val cin = Input(UInt(1.W))
        val i1  = Input(UInt(N.W))
        val onesComp = Output(UInt(N.W))
    })

    val xor = Seq.fill(N)(Module(new xor2))

    for (i<- 0 until N){
        xor(i).io.a := io.i1(i)
        xor(i).io.b := io.cin
        // io.onesComp(i) := xor(i).io.out
    }

    io.onesComp := Cat(Seq.tabulate(N)(i => xor(i).io.out).reverse)
    
}



object getOnesComp extends App {
  println("Generating the Ones Complement hardware")
  (new chisel3.stage.ChiselStage).emitVerilog(new getOnesComplement(), Array("--target-dir","verilog/"))

}

