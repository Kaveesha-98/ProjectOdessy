import chisel3._
import chisel3.util._

class booth_div_substep(N:Int) extends Module{
    val io = IO(new Bundle{
        val acc = Input(UInt(N.W))         //A
        val Q = Input(UInt(N.W))           
        val divisor = Input(UInt(N.W))     //M
        val next_acc = Output(UInt(N.W))
        val next_Q = Output(UInt(N.W))
    })

    val g1 = Module(new getOnesComplement(N))

    val int_ip = Wire(UInt(N.W))

    g1.io.cin := 1.U
    g1.io.i1 := io.divisor
    int_ip := g1.io.onesComp

    //left shift before sending to the adder
    val shiftedA = Wire(UInt((N+1).W))
    val shiftedQ = Wire(UInt((N+1).W))
    val shiftedA_LSB = Wire(UInt(1.W))
    val shiftedQ_LSB = Wire(UInt(1.W))
    val Aout = Wire(UInt((N+1).W))

    shiftedA := io.acc << 1
    shiftedA_LSB := io.Q(N-1)
    shiftedQ := io.Q << 1

    val as1 = Module(new addsub(N))

    val sub_temp = Wire(UInt(N.W))

    as1.io.cin := 1.U
    as1.io.onesComp_ip := int_ip
    as1.io.i0 := Cat(shiftedA(N-1,1),shiftedA_LSB)
    sub_temp := as1.io.sum          //sub_temp will hold the value of A-M

    //logic loop
    when (sub_temp(N-1) === 1.U){
        shiftedQ_LSB := 0.U 
        Aout         := Cat(shiftedA(N-1,1),shiftedA_LSB)
    }.otherwise{
        shiftedQ_LSB := 1.U
        Aout         := sub_temp
    }

    io.next_acc := Aout
    io.next_Q   := Cat(shiftedQ(N-1,1),shiftedQ_LSB)
}

object boothDivSubstep extends App {
  println("Generating the Booth Division Substep hardware")
  (new chisel3.stage.ChiselStage).emitVerilog(new booth_div_substep(32), Array("--target-dir","verilog/"))

} 