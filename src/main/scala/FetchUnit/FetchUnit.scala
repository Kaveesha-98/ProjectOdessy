/*
 Fetch Unit for in-order without speculation
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

    //reqport signals
    val reqport_valid = Output(UInt(1.W))
    val reqport_ready = Input(UInt(1.W))
    val reqport_addr = Output(UInt(64.W))

    //resport signals
    val resport_valid = Input(UInt(1.W))
    val resport_ready = Output(UInt(1.W))
    val resport_instr = Input(UInt(32.W))

    //Issueport signals
    val issueport_valid = Output(UInt(1.W))
    val issueport_instr = Output(UInt(32.W))
    val issueport_pc = Output(UInt(64.W))

    //BranchResult Port
    val target_input = Input(UInt(64.W))
    val target_valid = Input(UInt(1.W))

    //pipelinestall signal
    val pipelinestalled = Input(UInt(1.W))

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
  PC_fifo.io.enq.valid := io.reqport_valid & io.reqport_ready
  PC_fifo.io.deq.ready := io.resport_valid & ~io.pipelinestalled

  when (io.pipelinestalled === 0.U) {
    insport_pc := PC_fifo.io.deq.bits
  }
  io.issueport_pc := insport_pc

  //PC update logic
  when (internal_stall===1.U & io.target_valid===1.U){
    PC := io.target_input
  } .elsewhen(io.reqport_ready === 1.U & io.reqport_valid === 1.U){
    PC := PC + 4.U
  }
  io.reqport_addr := PC

  //PC valid bit logic
  when (branch_detector.io.is_branch === 1.U & PC_valid === 1.U){
    PC_valid := 0.U
  } .elsewhen(io.target_valid === 1.U){
    PC_valid := 1.U
  }


  //ready valid signal logic
  io.reqport_valid := ~internal_stall & PC_fifo.io.enq.ready
  io.resport_ready := ~(io.pipelinestalled) & PC_fifo.io.deq.valid
  io.issueport_valid := IR_valid & (~internal_stall) & (~io.pipelinestalled)


  //IR update logic
  when (io.resport_ready===1.U & io.resport_valid===1.U & internal_stall===0.U & io.pipelinestalled===0.U){
    IR := io.resport_instr
  }
  io.issueport_instr := IR
  branch_detector.io.instr := IR
  IR_valid := io.resport_valid & ~internal_stall

  //stall logic
  when (internal_stall === 0.U){
    internal_stall := branch_detector.io.is_branch
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
