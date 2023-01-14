/*
 Fetch Unit for in-order with speculation
 */

import Chisel.Cat
import chisel3._

class BHT extends Module {
  val io = IO(new Bundle {
    val execport = new Execport()
    val curr_pc = Input(UInt(64.W))
    val next_pc = Output(UInt(64.W))
  })

  when (!io.execport.predicted) {
    io.next_pc := io.execport.branch_address
  } .otherwise {
    io.next_pc := io.curr_pc + 4.U
  }
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
  val insport_pc = RegInit(0.U(128.W))

  // initialize BHT and fifo buffer
  val bht = Module(new BHT)
  bht.io.execport <> io.execport
  bht.io.curr_pc := PC
  val PC_fifo  = Module(new RegFifo(UInt(128.W), fifo_size))

  //connect PC_fifo
  PC_fifo.io.enq.bits := Cat(PC,bht.io.next_pc)
  PC_fifo.io.enq.valid := io.reqport.valid & io.reqport.ready
  PC_fifo.io.deq.ready := io.resport.valid & io.issueport.ready

  when (io.issueport.ready === 1.U) {
    insport_pc := PC_fifo.io.deq.bits
  }
  io.issueport.PC := insport_pc(127,64)
  io.issueport.prediction := insport_pc(63,0)

  //PC update logic
  when (internal_stall===0.U & io.reqport.valid & io.reqport.ready){
    PC := bht.io.next_pc
  }.elsewhen(internal_stall===0.U & (!io.execport.predicted) & (io.execport.valid)){  //special case where pc buffer is full and a mispred occurs
    PC := bht.io.next_pc
  }
  io.reqport.bits := PC

  //ready valid signal logic
  io.reqport.valid := internal_stall===0.U & PC_fifo.io.enq.ready
  io.resport.ready := io.issueport.ready & PC_fifo.io.deq.valid
  io.issueport.valid := IR_valid & (internal_stall===0.U) & (io.issueport.ready)


  //IR update logic
  when (io.resport.ready===1.U & io.resport.valid===1.U & internal_stall===0.U & io.issueport.ready===1.U){
    IR := io.resport.bits
  }
  io.issueport.ins := IR
  when(IR_valid === 0.U) {
    IR_valid := (io.resport.ready === 1.U & io.resport.valid === 1.U & internal_stall === 0.U & io.issueport.ready === 1.U).asUInt
  }.otherwise {
    IR_valid := io.issueport.ready | (io.resport.ready === 1.U & io.resport.valid === 1.U & internal_stall === 0.U & io.issueport.ready === 1.U).asUInt
  }

  //stall logic
  when (internal_stall === 0.U){
    internal_stall := io.execport.predicted
  } .elsewhen (PC_fifo.io.deq.valid === 0.U){
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
