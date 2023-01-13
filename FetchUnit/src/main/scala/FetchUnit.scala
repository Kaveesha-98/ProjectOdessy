/*
 Fetch Unit for in-order with speculation
 */

import chisel3._

class branch_detector extends Module {
  val io = IO(new Bundle {
    val instr  = Input(UInt(32.W))
    val is_branch = Output(UInt(1.W))
  })

  val op1 = io.instr(6,5)
  val op2 = io.instr(4,2)

  val flag1 = op1 === "b11".U
  val flag2 = op2 === "b001".U || op2 === "b011".U || op2 === "b000".U

  io.is_branch :=  flag1 & flag2

}

class FetchUnit(val pc_reset_val: Int, val fifo_size: Int) extends Module {
  val io = IO(new Bundle {
    val reqport = new DecoupledIO(UInt(64.W))
    val resport =  Flipped(new DecoupledIO(UInt(32.W)))
    val issueport = new Issueport()
    val execport = new Execport()
  })

  //register defs
  val PC = RegInit(pc_reset_val.U(64.W))
  val IR = RegInit(0.U(32.W))
  val internal_stall = RegInit(0.U(1.W))
  val IR_valid = RegInit(0.U(1.W))
  val PC_valid = RegInit(1.U(1.W))
  val insport_pc = RegInit(0.U(64.W))

  // initialize branch detector and fifo buffer
  val branch_detector  = Module(new branch_detector)
  val PC_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))

  //connect PC_fifo
  PC_fifo.io.enq.bits := PC
  PC_fifo.io.enq.valid := io.reqport.valid & io.reqport.ready
  PC_fifo.io.deq.ready := io.resport.valid & io.issueport.ready

  when (io.issueport.ready === 1.U) {
    insport_pc := PC_fifo.io.deq.bits
  }
  io.issueport.PC := insport_pc

  //PC update logic
  when (internal_stall===1.U & io.execport.valid===1.U){
    PC := io.execport.branch_address
  } .elsewhen(io.reqport.ready === 1.U & io.reqport.valid === 1.U){
    PC := PC + 4.U
  }
  io.reqport.bits := PC

  //PC valid bit logic
  when (branch_detector.io.is_branch === 1.U & PC_valid === 1.U){
    PC_valid := 0.U
  } .elsewhen(io.execport.valid === 1.U){
    PC_valid := 1.U
  }


  //ready valid signal logic
  io.reqport.valid := ~internal_stall & PC_fifo.io.enq.ready
  io.resport.ready := io.issueport.ready & PC_fifo.io.deq.valid
  io.issueport.valid := IR_valid & (~internal_stall) & (io.issueport.ready)


  //IR update logic
  when (io.resport.ready===1.U & io.resport.valid===1.U & internal_stall===0.U & io.issueport.ready===1.U){
    IR := io.resport.bits
  }
  io.issueport.ins := IR
  branch_detector.io.instr := IR
  when(IR_valid === 0.U) {
    IR_valid := (io.resport.ready === 1.U & io.resport.valid === 1.U & internal_stall === 0.U & io.issueport.ready === 1.U).asUInt
  }.otherwise {
    IR_valid := io.issueport.ready | (io.resport.ready === 1.U & io.resport.valid === 1.U & internal_stall === 0.U & io.issueport.ready === 1.U).asUInt
  }

  //stall logic
  when (internal_stall === 0.U){
    internal_stall := branch_detector.io.is_branch & IR_valid
  } .elsewhen (PC_fifo.io.deq.valid === 0.U & PC_valid === 1.U){
    internal_stall := 0.U
  }
  printf(p"$io\n")
}

/**
 * An object extending App to generate the Verilog code.
 */
object Verilog extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new FetchUnit(0,256))
}
