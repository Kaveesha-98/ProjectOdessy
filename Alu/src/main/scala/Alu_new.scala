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

//Defining registers to enable read/write FIFO
val write_to_fifo = RegInit(false.B)
val read_from_fifo = RegInit(false.B)

//Initialize FIFOs
val PC_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))
PC_fifo.io.enq.bits := io.decodeIssuePort.PC
PC_fifo.io.enq.valid := read_to_fifo
PC_fifo.io.deq.ready := read_from_fifo

val ins_fifo  = Module(new RegFifo(UInt(32.W), fifo_size))
ins_fifo.io.enq.bits := io.decodeIssuePort.PC
ins_fifo.io.enq.valid := read_to_fifo
ins_fifo.io.deq.ready := read_from_fifo

val imm_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))
imm_fifo.io.enq.bits := io.decodeIssuePort.PC
imm_fifo.io.enq.valid := read_to_fifo
imm_fifo.io.deq.ready := read_from_fifo

val rs1_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))
rs1_fifo.io.enq.bits := io.decodeIssuePort.PC
rs1_fifo.io.enq.valid := read_to_fifo
rs1_fifo.io.deq.ready := read_from_fifo

val rs2_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))
rs2_fifo.io.enq.bits := io.decodeIssuePort.PC
rs2_fifo.io.enq.valid := read_to_fifo
rs2_fifo.io.deq.ready := read_from_fifo

val opcode_fifo  = Module(new RegFifo(UInt(7.W), fifo_size))
opcode_fifo.io.enq.bits := io.decodeIssuePort.PC
opcode_fifo.io.enq.valid := read_to_fifo
opcode_fifo.io.deq.ready := read_from_fifo



  
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
        read_from_fifo := true.B

        stateReg:= EXEC
      }
    }.otherwise{
      when (FIFO_full){
        io.UnitStatus.ready := 0.U
        stateReg := EXEC
      }.otherwise{
        write_to_fifo := true.B

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
            when (rs1Reg.asSInt < rs2Reg.asSInt){ // this should be signed comparision
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
            when (rs1Reg.asSInt >= rs2Reg.asSInt){ // this should be a signed comparision
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
            when (rs1Reg.U < rs2Reg.U){ // this might throw an error rs*Reg are already UInt
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
            when (rs1Reg.U >= rs2Reg.U){ // this might throw an error rs*Reg are already UInt
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
            when (rs1Reg.asSInt < immReg.asSInt){ // this is a signed comparision
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }
          }            
          
          is (011) {      //SLTIU
            // same code as above without ".asSInt"
            when (rs1Reg < immReg){ // this is a signed comparision
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }            
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
            aluResultReg := rs1Reg << immReg(5,0) // this is the 64 bit version shamt field is 6-bits wide
          }

          is (101) {      //SRLI
            when (funct7Reg === 0000000) {
              aluResultReg := rs1Reg >> immReg(5, 0)           // this is the 64 bit version shamt field is 6-bits wide   
            }.otherwise{  //SRAI
              aluResultReg := (rs1Reg.asSInt >> immReg(5, 0)).asUInt // for this rs1 is considered to be a signed int
            }
          }


        }        
      }

      is (rops.U){
        branchResultValidReg := 0.U
        nextInstPtrReg :- pcReg + 4.U

        switch (funct3Reg){
          is (000){
            when (funct7Reg === 0000000){       //ADD
              aluResultReg := rs1Reg + rs2Reg
            }.otherwise {                       //SUB
              aluResultReg := rs1Reg - rs2Reg     
            }
          }

          is (001){
              aluResultReg := rs1Reg << rs2Reg  //SLL - might throw an error
              // if the above line throws an error try - 
              // Mux(rs2(31, 5).orR, 0.U, rs1 << rs2(5, 0)) 
          }

          is (010){     //SLT
            when (rs1Reg.asSInt < rs2Reg.asSInt){ // this is a signed compare
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }
          }

          is (011){         //SLTU   
            when (rs1Reg < rs2Reg){
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }
          }  

          is (100){         //XOR
            aluResultReg := rs1Reg ^ rs2Reg
          }   

          is (101){
            when (funct7Reg === 0000000){       //SRL
              aluResultReg := rs1Reg >> rs2Reg
            }.otherwise {                       //SRA  
              aluResultReg := rs1Reg.asSInt >> rs2Reg // rs1 is treated as a signed number   
            }
          }

          is (110){     //OR
            aluResultReg := rs1Reg | rs2Reg
          }

          is (111){
            aluResultReg := rs1Reg & rs2Reg
          }
        }        
      }

      is (load.U){
        branchResultValidReg := 0.U
        nextInstPtrReg :- pcReg + 4.U
        //<add load instruction implementation here>        
      }

      is (store.U){
        branchResultValidReg := 0.U
        nextInstPtrReg :- pcReg + 4.U
        //<add store instruction implementation here>        
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

