package pipeline.decode

import Constants._

import chisel3._
import chisel3.util._

class BranchResult extends Bundle {
	val valid 			= Input(Bool())
	val is_branch 		= Input(Bool())
	val branch_taken 	= Input(Bool())
	val predicted 		= Input(Bool())
	val PC				= Input(UInt(64.W))
	val branch_address	= Input(UInt(64.W))
}

class decodeWrapper extends Module {
	val decode = Module(new DecodeUnit)
	val io = IO(decode.io.cloneType)
	io <> decode.io
	
	val expectedPC = IO(Output(UInt(64.W)))
	val expectedPC_internal = RegInit("h80000000".U(64.W))
	
	val branchResult = IO(new BranchResult)
	
	
}

object decodeWrapper extends App {
	(new stage.ChiselStage).emitVerilog(new decodeWrapper)
}
