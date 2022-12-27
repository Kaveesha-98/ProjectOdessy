package pipeline.decode

import Constants._

import chisel3._
import chisel3.util._

// Inputs from Fetch unit
class FetchIssuePort extends Bundle {
  val valid       = UInt(1.W)
  val instruction = UInt(32.W)
  val PC          = UInt(64.W)
}

// Outputs for ALU
class DecodeIssuePort extends Bundle {
  val valid       = UInt(1.W)
  val instruction = UInt(32.W)
  val PC          = UInt(64.W)
  val opCode      = UInt(7.W)
  val rs1         = UInt(64.W)
  val rs2         = UInt(64.W)
  val immediate   = UInt(64.W)
}

// Inputs for Register file
class WriteBackResult extends Bundle {
  val toRegisterFile = UInt(1.W)
  val rd             = UInt(5.W)
  val rdData         = UInt(64.W)
}

class DECODE_ISSUE_UNIT extends Module{
  val io = IO(new Bundle() {
    val fetchIssuePort  = Input(new FetchIssuePort)
    val decodeIssuePort = Output(new DecodeIssuePort)
    val writeBackResult = Input(new WriteBackResult)
    val readyOut        = Output(UInt(1.W))             // ready signal for Fetch unit
    val readyIn         = Input(UInt(1.W))              // ready signal from ALU
  })

  // Assigning some wires for inputs
  val validIn   = io.fetchIssuePort.valid
  val pc        = io.fetchIssuePort.PC
  val writeEn   = io.writeBackResult.toRegisterFile
  val writeData = io.writeBackResult.rdData
  val writeRd   = Wire(UInt(1.W))
  writeRd      := io.writeBackResult.rd
  val readyIn   = io.readyIn

  // Inintializing some registers for outputs
  val pcReg     = RegInit(0.U(64.W))
  val insReg    = RegInit(0.U(32.W))
  val opCodeReg = RegInit(0.U(7.W))
  val immReg    = RegInit(0.U(64.W))
  val rs1Reg    = RegInit(0.U(64.W))
  val rs2Reg    = RegInit(0.U(64.W))

  // Initializing some intermediate wires
  val validOut  = WireDefault(0.U(1.W))
  val readyOut  = WireDefault(0.U(1.W))
  val insType   = WireDefault(0.U(3.W))
  val rdValid   = WireDefault(1.U(1.W))
  val rs1Valid  = WireDefault(1.U(1.W))
  val rs2Valid  = WireDefault(1.U(1.W))
  val stalled   = WireDefault(0.U(1.W))
  val ins       = WireDefault(0.U(32.W))

  pcReg  := pc
  // Storing instruction value in a register
  when(io.fetchIssuePort.valid === 1.U & io.readyOut === 1.U) {
    insReg := io.fetchIssuePort.instruction
  }

  ins := Mux(io.fetchIssuePort.valid === 1.U & io.readyOut === 1.U, io.fetchIssuePort.instruction, insReg)

  // Assigning outputs
  io.decodeIssuePort.PC          := pcReg
  io.decodeIssuePort.instruction := insReg
  io.decodeIssuePort.opCode      := opCodeReg
  io.decodeIssuePort.immediate   := immReg
  io.decodeIssuePort.valid       := validOut
  io.decodeIssuePort.rs1         := rs1Reg
  io.decodeIssuePort.rs2         := rs2Reg
  io.readyOut                    := readyOut

  // Deciding the instruction type
  switch (ins(6,0)) {
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

  // Calculating the immediate value
  switch (insType) {
    is(itype.U) { immReg := Cat(Fill(53, ins(31)), ins(30, 20)) }
    is(stype.U) { immReg := Cat(Fill(53, ins(31)), ins(30,25), ins(11, 7)) }
    is(btype.U) { immReg := Cat(Fill(53, ins(31)), ins(7), ins(30,25), ins(11,8), 0.U(1.W)) }
    is(utype.U) { immReg := Cat(Fill(32, ins(31)), ins(31,12), 0.U(12.W)) }
    is(jtype.U) { immReg := Cat(Fill(44, ins(31)), ins(19,12), ins(20), ins(30,25), ins(24,21), 0.U(1.W)) }
    is(ntype.U) { immReg := Fill(64, 0.U) }
    is(rtype.U) { immReg := Fill(64, 0.U) }
  }

  // Valid bits for each register
  val validBit = RegInit(VecInit(Seq.fill(32)(1.U(1.W))))
  validBit(0) := 1.U

  // Initializing the Register file
  val registerFile = Reg(Vec(32, UInt(64.W)))
  registerFile(0) := 0.U

  rs1Reg := registerFile(ins(19, 15))
  rs2Reg := registerFile(ins(24, 20))

  opCodeReg := ins(6, 0)

  // Register writing
  when(writeEn === 1.U & validBit(writeRd) === 0.U & writeRd =/= 0.U) {
    registerFile(writeRd) := writeData
    validBit(writeRd)     := 1.U
  }

  // Checking rs1 validity
  when(insType === rtype.U | insType === itype.U | insType === stype.U | insType === btype.U) {
    when(validBit(ins(19, 15)) === 0.U) { rs1Valid := 0.U }
  }
  // Checking rs2 validity
  when(insType === rtype.U | insType === stype.U | insType === btype.U) {
    when(validBit(ins(24, 20)) === 0.U) { rs2Valid := 0.U }
  }
  // Checking rd validity and changing the valid bit for rd
  when(insType === rtype.U | insType === utype.U | insType === itype.U | insType === jtype.U) {
    when(validBit(ins(11, 7)) === 0.U) { rdValid := 0.U }
    .otherwise {
      when(rs1Valid === 1.U & rs2Valid === 1.U & readyIn === 1.U) { validBit(ins(11, 7)) := 0.U }
    }
  }

  stalled := ~(rdValid & rs1Valid & rs2Valid)     // stall signal for FSM

  // FSM for ready valid interface
  // ------------------------------------------------------------------------------------------------------------------ //
  val idleState :: passthroughState :: waitState :: stallState :: Nil = Enum(4)     // States of FSM
  val stateReg = RegInit(idleState)

  switch(stateReg) {
    is(idleState) {
      validOut := 0.U
      readyOut := 1.U
      when(validIn === 1.U) {
        stateReg := Mux(stalled === 1.U, stallState, Mux(readyIn === 1.U, passthroughState, waitState))
      }
    }
    is(passthroughState) {
      validOut := 1.U
      readyOut := 1.U
      stateReg := Mux(stalled === 1.U, stallState, Mux(readyIn === 0.U, waitState, Mux(validIn === 1.U, passthroughState, idleState)))
    }
    is(waitState) {
      validOut := 1.U
      readyOut := 0.U
      when(readyIn === 1.U) {
        stateReg := Mux(validIn === 1.U, passthroughState, idleState)
      }
    }
    is(stallState) {
      validOut := 0.U
      readyOut := 0.U
      when(stalled === 0.U) {
        stateReg := Mux(readyIn === 1.U, passthroughState, waitState)
      }
    }
  }
  // ------------------------------------------------------------------------------------------------------------------ //
}

object DECODE_ISSUE_UNIT extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new DECODE_ISSUE_UNIT())
}