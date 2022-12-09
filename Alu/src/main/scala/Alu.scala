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
  rs1Reg    := io.decodeIssuePort.rs1
  rs2Reg    := io.decodeIssuePort.rs2
  opCodeReg := io.decodeIssuePort.opCode
  funct3Reg := io.decodeIssuePort.instruction(14,12)


val aluResultReg = RegInit(0.U(64.W))
val nextInstPtrReg = RegInit(0.U(64.W))
val branchResultValidReg = RegInit(0.U(1.W))
val branchResultTargetReg = RegInit(0.U(64.W))

io.aluIssuePort.aluResult := aluResultReg
io.aluIssuePort.nextInstrPtr := nextInstPtrReg
io.branchResult.valid := branchResultValidReg
io.branchResult.target := branchResultTargetReg
  


  switch (opCode) {
    is(lui.U)    { 
      aluResultReg            := immReg
      nextInstPtrReg          := pcReg + 4.U
      branchResultValidReg    := 0.U     
      }

    is(auipc.U)  { 
      aluResultReg            := pcReg + immReg
      nextInstPtrReg          := pcReg + 4.U
      branchResultValidReg    := 0.U     
     }
    is(jump.U)   { 
      aluResultReg            := pcReg + 4.U
      nextInstPtrReg          := pcReg + immReg
      branchResultValidReg    := 1.U
      branchResultTargetReg   := pcReg + immReg          
     }
    is(jumpr.U)  { 
      aluResultReg            := pcReg + 4.U
      nextInstPtrReg          := rs1Reg + immReg
      branchResultValidReg    := 1.U
      branchResultTargetReg   := rs1Reg + immReg               
     }
    is(cjump.U)  { 
      //impelment conditional jump
     }
    is(load.U)   { 
      //implement LB,LW,LH,LBU,LHU
     }
    is(store.U)  { 
      //implement SB,SH,SW
     }
    is(iops.U)   { 
      //implement ADDI,SLTI,SLTIU,XORI,ORI,ANDI,SLLI,SRLI,SRAI
     }

    // is(iops32.U) { 
    //   //implement RV32M Standard Extension
    //  }

     is(rops.U)   { 
      //implement ADD,SUB,SLL,SLT,SLTU,XOR,SRL,SRA,OR,AND
     }

    // is(rops32.U) { 
    //   //implement RV64M Standard Extension
    //  }

    // is(system.U) { 
    //   //Implement RV32/RV64 Zicsr Standard Extension
    //  }

    // is(fence.U)  { 
        //implement FENCE instruction
    //  }

    // is(amos.U)   { 
    //   //implement RV32A Standard Extension
    //  }

    is(store.U)  { 
      //implement SB,SH,SW
     }
  }

  

}

