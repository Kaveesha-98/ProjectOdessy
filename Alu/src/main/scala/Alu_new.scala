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
  val ready    = UInt(1.W)
  val valid      = UInt(1.W)
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
  val funct3Reg = RegInit(0.U(3.W))

val aluResultReg = RegInit(0.U(64.W))
val nextInstPtrReg = RegInit(0.U(64.W))
val branchResultValidReg = RegInit(0.U(1.W))
val branchResultTargetReg = RegInit(0.U(64.W))


  
//Defining the 03 states of the ALU
val READ :: EXEC :: WRITE :: Nil = Enum(3)

//The state register
val stateReg = RegInit(READ)

//state change logic
switch (stateReg){
  is (READ){
    when (exec_ready){
      when (FIFO_empty){
        pcReg     := io.decodeIssuePort.PC
        insReg    := io.decodeIssuePort.instruction
        immReg    := io.decodeIssuePort.imm
        rs1Reg    := io.decodeIssuePort.rs1
        rs2Reg    := io.decodeIssuePort.rs2
        opCodeReg := io.decodeIssuePort.opCode
        funct3Reg := io.decodeIssuePort.instruction(14,12)
        funct7Reg := io.decodeIssuePort.instruction(31,25)
        stateReg:= EXEC
      } .otherwise{
        //FIFOIssue port connected to ALU registers

        stateReg:= EXEC
      }
    }.otherwise{
      when (FIFO_full){
        io.UnitStatus.ready := 0.U
        stateReg := EXEC
      }.otherwise{
        //DecodeIssuePort conneted to the input of the FIFO

        stateReg:= EXEC
      }
      
    }
  }

  is (EXEC){
    exec_ready := 0.U

    //Execution of instructions 
    switch (opCodeReg){

      is (lui.U)    {
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
      
      is (cjump.U){

        switch(funct3Reg){

          is (000) {      //BEQ
            when (rs1Reg === rs2Reg){
              nextInstPtrReg := pcReg + immReg
              branchResultTargetReg := pcReg + immReg 
            }.otherwise{
              nextInstPtrReg := pcReg + 4.U
              branchResultTargetReg := pcReg + 4.U
            }
            aluResultReg := 0.U
            branchResultValidReg := 1.U
          }

          is (001) {      //BNE
            when (rs1Reg != rs2Reg){
              nextInstPtrReg := pcReg + immReg
              branchResultTargetReg := pcReg + immReg 
            }.otherwise{
              nextInstPtrReg := pcReg + 4.U
              branchResultTargetReg := pcReg + 4.U
            }
            aluResultReg := 0.U
            branchResultValidReg := 1.U
          }            
          
          is (100) {      //BLT
            when (rs1Reg < rs2Reg){
              nextInstPtrReg := pcReg + immReg
              branchResultTargetReg := pcReg + immReg 
            }.otherwise{
              nextInstPtrReg := pcReg + 4.U
              branchResultTargetReg := pcReg + 4.U
            }
            aluResultReg := 0.U
            branchResultValidReg := 1.U          
          }

          is (101) {      //BGE
            when (rs1Reg >= rs2Reg){
              nextInstPtrReg := pcReg + immReg
              branchResultTargetReg := pcReg + immReg 
            }.otherwise{
              nextInstPtrReg := pcReg + 4.U
              branchResultTargetReg := pcReg + 4.U
            }
            aluResultReg := 0.U
            branchResultValidReg := 1.U                      
          }

          is (110) {      //BLTU
            when (rs1Reg.U < rs2Reg.U){
              nextInstPtrReg := pcReg + immReg
              branchResultTargetReg := pcReg + immReg 
            }.otherwise{
              nextInstPtrReg := pcReg + 4.U
              branchResultTargetReg := pcReg + 4.U
            }
            aluResultReg := 0.U
            branchResultValidReg := 1.U                      
          }

          is (111) {      //BGEU
            when (rs1Reg.U >= rs2Reg.U){
              nextInstPtrReg := pcReg + immReg
              branchResultTargetReg := pcReg + immReg 
            }.otherwise{
              nextInstPtrReg := pcReg + 4.U
              branchResultTargetReg := pcReg + 4.U
            }
            aluResultReg := 0.U
            branchResultValidReg := 1.U          
            
          }
        }
      }

      is (iops.U){

        branchResultValidReg := 0.U
        nextInstPtrReg :- pcReg + 4.U

        switch(funct3Reg){

          is (000) {      //ADDI
            aluResultReg := rs1Reg + immReg
          }

          is (010) {      //SLTI
            when (rs1Reg < immReg){
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }
          }            
          
          is (011) {      //SLTIU
          }

          is (100) {      //XORI
            aluResultReg := rs1Reg ^ immReg
          }

          is (110) {      //ORI
            aluResultReg := rs1Reg | immReg
          }

          is (111) {      //ANDI
            aluResultReg := rs1Reg & immReg
          }

          is (001) {      //SLLI
            aluResultReg := 
          }

          is (101) {      //SRLI
            when (funct7Reg === 0000000) {

            }.otherwise{

            }
          }


        }        
      }

     
      

    }

    stateReg := WRITE
  }

  is (WRITE){
    exec_ready := 1.U
    //Connecting ALU registers to the Output port
    io.aluIssuePort.aluResult := aluResultReg
    io.aluIssuePort.nextInstrPtr := nextInstPtrReg
    io.branchResult.valid := branchResultValidReg
    io.branchResult.target := branchResultTargetReg

    stateReg := READ

  }
}
  

  

}

