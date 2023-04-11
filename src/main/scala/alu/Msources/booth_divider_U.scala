import chisel3._
import chisel3.util._

class booth_divider_U(N:Int) extends Module{
    val io = IO(new Bundle{
        val signed   = Input(UInt(1.W))
        val dividend = Input(UInt(N.W))
        val divisor  = Input(UInt(N.W))  //M
        val quotient = Output(UInt(N.W))
        val remainder= Output(UInt(N.W))        
    })

    val dividend_in = Wire(UInt(N.W))
    val divisor_in  = Wire(UInt(N.W))

    //Quotient is negative if the signs are different
    val neg_quotient = Wire(UInt(1.W))
    neg_quotient := (io.dividend(N-1) ^ io.divisor(N-1)) & (io.signed === 1.U)

    //get the 2's complement of both dividend and divisor
    val dividend_comp = Wire(UInt(N.W))
    val divisor_comp  = Wire(UInt(N.W))

    dividend_comp := ~io.dividend + 1.U
    divisor_comp  := ~io.divisor + 1.U
    
    //If signed and negative, convert. 
    //Otherwise, forward
    when (io.signed === 1.U & io.dividend(N-1) === 1.U){
        dividend_in := dividend_comp
    }.otherwise{
        dividend_in := io.dividend
    }
    
    when (io.signed === 1.U & io.divisor(N-1) === 1.U){
        divisor_in := divisor_comp
    }.otherwise{
        divisor_in := io.divisor
    }      
    
    //Initiate : division algorithm
    val Q = Wire(Vec(N,UInt(N.W)))
    val acc = Wire(Vec(N,UInt(N.W)))
    
    val quotientTemp  = Wire(UInt(N.W))
    val remainderTemp = Wire(UInt(N.W))

    Q(0)    := dividend_in
    acc(0)  := 0.U

    val bds = Seq.fill(N)(Module(new booth_div_substep(N)))

    for (i <- 0 until (N-1)){
        bds(i).io.acc       := acc(i)
        bds(i).io.Q         := Q(i)
        bds(i).io.divisor   := divisor_in
        acc(i+1)            := bds(i).io.next_acc
        Q(i+1)              := bds(i).io.next_Q    
    }

    bds(N-1).io.acc          := acc(N-1)
    bds(N-1).io.Q           := Q(N-1)
    bds(N-1).io.divisor      := divisor_in
    quotientTemp            := bds(N-1).io.next_Q
    remainderTemp           := bds(N-1).io.next_acc

    //End : Division Algorithm

    io.quotient := Mux((neg_quotient===1.U) , ~quotientTemp + 1.U , quotientTemp)
    io.remainder:= Mux((io.signed === 1.U) & (io.dividend(N-1) === 1.U), ~remainderTemp + 1.U , remainderTemp)

}

object boothDividerU extends App {
  println("Generating the booth divider hardware")
  (new chisel3.stage.ChiselStage).emitVerilog(new booth_divider_U(64), Array("--target-dir","verilog/"))

}