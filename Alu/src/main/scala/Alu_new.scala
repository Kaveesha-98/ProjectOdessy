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

class Alu (val fifo_size: Int) extends Module{
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
  val funct7Reg = RegInit(0.U(7.W))

val aluResultReg = RegInit(0.U(64.W))
val nextInstPtrReg = RegInit(0.U(64.W))
val branchResultValidReg = RegInit(0.U(1.W))
val branchResultTargetReg = RegInit(0.U(64.W))

//Defining registers to enable read/write FIFO
val write_to_fifo = RegInit(false.B)
val read_from_fifo = RegInit(false.B)


//Initialize FIFOs
val pc_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))
pc_fifo.io.enq.bits := io.decodeIssuePort.PC
pc_fifo.io.enq.valid := write_to_fifo
pc_fifo.io.deq.ready := read_from_fifo

val ins_fifo  = Module(new RegFifo(UInt(32.W), fifo_size))
ins_fifo.io.enq.bits := io.decodeIssuePort.PC
ins_fifo.io.enq.valid := write_to_fifo
ins_fifo.io.deq.ready := read_from_fifo

val imm_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))
imm_fifo.io.enq.bits := io.decodeIssuePort.PC
imm_fifo.io.enq.valid := write_to_fifo
imm_fifo.io.deq.ready := read_from_fifo

val rs1_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))
rs1_fifo.io.enq.bits := io.decodeIssuePort.PC
rs1_fifo.io.enq.valid := write_to_fifo
rs1_fifo.io.deq.ready := read_from_fifo

val rs2_fifo  = Module(new RegFifo(UInt(64.W), fifo_size))
rs2_fifo.io.enq.bits := io.decodeIssuePort.PC
rs2_fifo.io.enq.valid := write_to_fifo
rs2_fifo.io.deq.ready := read_from_fifo

val opcode_fifo  = Module(new RegFifo(UInt(7.W), fifo_size))
opcode_fifo.io.enq.bits := io.decodeIssuePort.PC
opcode_fifo.io.enq.valid := write_to_fifo
opcode_fifo.io.deq.ready := read_from_fifo

//Combining all deq.valid outputs from FIFOs through an OR gate
val fifos_empty = RegNext(pc_fifo.io.deq.valid || ins_fifo.io.deq.valid || imm_fifo.io.deq.valid || rs1_fifo.io.deq.valid || rs2_fifo.io.deq.valid || opcode_fifo.io.deq.valid)

//Combining al enq.ready outputs from FIFOs through an OR gate
val fifos_full = RegNext(pc_fifo.io.enq.ready || ins_fifo.io.enq.ready || imm_fifo.io.enq.ready || rs1_fifo.io.enq.ready || rs2_fifo.io.enq.ready || opcode_fifo.io.enq.ready)

//registers with control information
val exec_ready = RegInit(true.B)
  
//Defining the 03 states of the ALU
val load_values :: execute :: write_to_output :: Nil = Enum(3)

//The state register
val stateReg = RegInit(load_values)

//state change logic
switch (stateReg){
  is (load_values){
    when (exec_ready){
      when (fifos_empty){  //when FIFO is empty, io.deq.valid is false
        pcReg     := io.decodeIssuePort.PC
        insReg    := io.decodeIssuePort.instruction
        immReg    := io.decodeIssuePort.immediate
        rs1Reg    := io.decodeIssuePort.rs1
        rs2Reg    := io.decodeIssuePort.rs2
        opCodeReg := io.decodeIssuePort.opCode
        funct3Reg := io.decodeIssuePort.instruction(14,12)
        funct7Reg := io.decodeIssuePort.instruction(31,25)
        stateReg:= execute
      } .otherwise{
        read_from_fifo := true.B

        stateReg:= execute
      }
    }.otherwise{
      when (fifos_full){
        io.unitStatus.ready := 0.U
        stateReg := execute
      }.otherwise{
        write_to_fifo := true.B

        stateReg:= execute
      }
      
    }
  }

  is (execute){
    exec_ready := false.B

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

      is(jal.U)   { 
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

          is ("b000".U) {      //BEQ
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

          is ("b001".U) {      //BNE
            when (rs1Reg === rs2Reg){
              nextInstPtrReg := pcReg + 4.U
              branchResultTargetReg := pcReg + 4.U 
            }.otherwise{
              nextInstPtrReg := pcReg + immReg
              branchResultTargetReg := pcReg + immReg
            }
            aluResultReg := 0.U
            branchResultValidReg := 1.U
          }            
          
          is ("b100".U) {      //BLT
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

          is ("b101".U) {      //BGE
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

          is ("b110".U) {      //BLTU
            when (rs1Reg < rs2Reg){ // this might throw an error rs*Reg are already UInt
              nextInstPtrReg := pcReg + immReg
              branchResultTargetReg := pcReg + immReg 
            }.otherwise{
              nextInstPtrReg := pcReg + 4.U
              branchResultTargetReg := pcReg + 4.U
            }
            aluResultReg := 0.U
            branchResultValidReg := 1.U                      
          }

          is ("b111".U) {      //BGEU
            when (rs1Reg >= rs2Reg){ // this might throw an error rs*Reg are already UInt
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
        nextInstPtrReg := pcReg + 4.U

        switch(funct3Reg){

          is ("b000".U) {      //ADDI
            aluResultReg := rs1Reg + immReg
          }

          is ("b010".U) {      //SLTI
            when (rs1Reg.asSInt < immReg.asSInt){ // this is a signed comparision
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }
          }            
          
          is ("b011".U) {      //SLTIU
            // same code as above without ".asSInt"
            when (rs1Reg < immReg){ // this is a signed comparision
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }            
          }

          is ("b100".U) {      //XORI
            aluResultReg := rs1Reg ^ immReg
          }

          is ("b110".U) {      //ORI
            aluResultReg := rs1Reg | immReg
          }

          is ("b111".U) {      //ANDI
            aluResultReg := rs1Reg & immReg
          }

          is ("b001".U) {      //SLLI
            aluResultReg := rs1Reg << immReg(5,0) // this is the 64 bit version shamt field is 6-bits wide
          }

          is ("b101".U) {      //SRLI
            when (funct7Reg === "b0000000".U) {
              aluResultReg := rs1Reg >> immReg(5, 0)           // this is the 64 bit version shamt field is 6-bits wide   
            }.otherwise{  //SRAI
              aluResultReg := (rs1Reg.asSInt >> immReg(5, 0)).asUInt // for this rs1 is considered to be a signed int
            }
          }


        }        
      }

      is (rops.U){
        branchResultValidReg := 0.U
        nextInstPtrReg := pcReg + 4.U

        switch (funct3Reg){
          is ("b000".U){
            when (funct7Reg === "b0000000".U){       //ADD
              aluResultReg := rs1Reg + rs2Reg
            }.otherwise {                       //SUB
              aluResultReg := rs1Reg - rs2Reg     
            }
          }

          is ("b001".U){
              aluResultReg := rs1Reg << rs2Reg  //SLL - might throw an error
              // if the above line throws an error try - 
              // Mux(rs2(31, 5).orR, 0.U, rs1 << rs2(5, 0)) 
          }

          is ("b010".U){     //SLT
            when (rs1Reg.asSInt < rs2Reg.asSInt){ // this is a signed compare
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }
          }

          is ("b011".U){         //SLTU   
            when (rs1Reg < rs2Reg){
              aluResultReg := 1.U
            }.otherwise {
              aluResultReg := 0.U
            }
          }  

          is ("b100".U){         //XOR
            aluResultReg := rs1Reg ^ rs2Reg
          }   

          is ("b101".U){
            when (funct7Reg === "b0000000".U){       //SRL
              aluResultReg := rs1Reg >> rs2Reg
            }.otherwise {                       //SRA  
              aluResultReg := rs1Reg.asSInt >> rs2Reg // rs1 is treated as a signed number   
            }
          }

          is ("b110".U){     //OR
            aluResultReg := rs1Reg | rs2Reg
          }

          is ("b111".U){
            aluResultReg := rs1Reg & rs2Reg
          }
        }        
      }

      is (load.U){
        branchResultValidReg := 0.U
        nextInstPtrReg := pcReg + 4.U
        //<add load instruction implementation here>        
      }

      is (store.U){
        branchResultValidReg := 0.U
        nextInstPtrReg := pcReg + 4.U
        //<add store instruction implementation here>        
      }
     
      

    }

    stateReg := write_to_output
  }

  is (write_to_output){
    exec_ready := true.B
    //Connecting ALU registers to the Output port
    io.aluIssuePort.aluResult := aluResultReg
    io.aluIssuePort.nextInstPtr := nextInstPtrReg
    io.branchResult.valid := branchResultValidReg
    io.branchResult.target := branchResultTargetReg

    stateReg := load_values

  }
}
   

}

/**
 * An object extending App to generate the Verilog code.
 */
object Verilog extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Alu(32))
}


