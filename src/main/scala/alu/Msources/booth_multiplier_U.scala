package pipeline.alu

import chisel3._
import chisel3.util._

class booth_multiplier_U(N:Int) extends Module{
    val io = IO(new Bundle{
        val multiplier = Input(UInt(N.W))
        val multiplicand = Input(UInt(N.W))
        val product = Output(UInt((2*N).W))
    })

    val Q = Wire(Vec(N,UInt(N.W)))
    val acc = Wire(Vec(N,UInt(N.W)))
    val q0 = Wire(Vec(N,UInt(1.W)))
    val qout = Wire(UInt(1.W))

    val product_temp_hi = Wire(UInt(N.W))
    val product_temp_lo = Wire(UInt(N.W))

    q0(0) := 0.U
    acc(0) := 0.U
    Q(0) := io.multiplier

    val bs = Seq.fill(N)(Module(new booth_mult_substep_U(N)))

    for (i <- 0 until (N-1)){
        bs(i).io.acc := acc(i)
        bs(i).io.Q := Q(i)
        bs(i).io.q0 := q0(i)
        bs(i).io.multiplicand := io.multiplicand
        acc(i+1) := bs(i).io.next_acc
        Q(i+1) := bs(i).io.next_Q 
        q0(i+1) := bs(i).io.q0_next
    }

    bs((N-1)).io.acc := acc((N-1))
    bs((N-1)).io.Q := Q((N-1))
    bs((N-1)).io.q0 := q0((N-1))
    bs((N-1)).io.multiplicand := io.multiplicand
    product_temp_hi := bs((N-1)).io.next_acc
    product_temp_lo := bs((N-1)).io.next_Q 
    qout := bs((N-1)).io.q0_next

    io.product := Cat(product_temp_hi,product_temp_lo)

}

object boothMultiplierU extends App {
  println("Generating the booth multiplier hardware")
  (new chisel3.stage.ChiselStage).emitVerilog(new booth_multiplier_U(64), Array("--target-dir","verilog/"))

}