package alu

import chisel3._
import chisel3.util._

import Constants._

class DecodeIssuePort extends Bundle {
  val valid       = UInt(1.W)
  val PC          = UInt(64.W)
  val instruction = UInt(32.W)
  val opCode      = UInt(7.W)
  val rs1         = UInt(64.W)
  val rs2         = UInt(64.W)
  val immediate   = UInt(64.W)
}

class AluIssuePort extends Bundle {
  val valid       = UInt(1.W)
  val aluResult   = UInt(64.W)
  val instruction = UInt(32.W)
  val nextInstPtr = UInt(64.W)
  val rs2         = UInt(64.W)
}

class UnitStatus extends Bundle {
  val stalled    = UInt(1.W)
  val empty      = UInt(1.W)
}

class BranchResult extends Bundle {
  val valid     = UInt(1.W)
  val target    = UInt(64.W)
}

class Alu extends Module{
  val io = IO(new Bundle(){
    val decodeIssuePort = Input(new DecodeIssuePort)
    val aluIssuePort    = Output(new AluIssuePort)
    val unitStatus      = Output(new UnitStatus)
    val branchResult    = Output(new BranchResult)
    val pipelineStalled = Input(UInt(1.W))
    //val interrupted     = Input(UInt(1.W)) 
  })

  val pcReg    = RegInit(0.U(64.W))
  val insReg   = RegInit(0.U(32.W))
  val immReg   = RegInit(0.U(64.W))
  val rs1Reg   = RegInit(0.U(64.W))
  val rs2Reg   = RegInit(0.U(64.W))
  val opCodeReg= RegInit(0.U(7.W))

  pcReg     := io.decodeIssuePort.PC
  insReg    := io.decodeIssuePort.instruction
  immReg    := io.decodeIssuePort.imm
  rs1Reg    :- io.decodeIssuePort.rs1
  rs2Reg    :- io.decodeIssuePort.rs2
  opCodeReg :- io.decodeIssuePort.opCode
  funct3Reg :- io.decodeIssuePort.instruction(14,12)

  


  switch (opCode) {
    is(lui.U)    { insType := utype.U }
    is(auipc.U)  { insType := utype.U }
    is(jump.U)   { insType := jtype.U }
    is(jumpr.U)  { insType := itype.U }
    is(cjump.U)  { insType := btype.U }
    is(load.U)   { insType := itype.U }
    is(store.U)  { insType := stype.U }
    is(iops.U)   { insType := itype.U }
    is(iops32.U) { insType := itype.U }
    is(rops.U)   { insType := rtype.U }
    is(rops32.U) { insType := rtype.U }
    is(system.U) { insType := itype.U }
    is(fence.U)  { insType := ntype.U }
    is(amos.U)   { insType := rtype.U }
  }

  

}

