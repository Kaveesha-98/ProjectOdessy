package pipeline.decode

import Constants._
import chisel3._
import chisel3.util._
import pipeline.ports.{DecodeIssuePort, FetchIssuePort, WriteBackResult}

class handshake[T <: Data](gen: T) extends Bundle {
  val ready = Output(UInt(1.W))
  val valid = Input(UInt(1.W))
  val bits  = Input(gen)
}

class DecodeUnit extends Module{
  val io = IO(new Bundle() {
    val fetchIssuePort  = new handshake(new FetchIssuePort)
    val decodeIssuePort = Flipped(new handshake(new DecodeIssuePort))
    val writeBackResult = Input(new WriteBackResult)
    val branchMisspredict = Input(Bool())
  })

  val rdBuffer = RegInit(0.U(5.W))

  // Assigning some wires for inputs
  val validIn   = io.fetchIssuePort.valid
  val pc        = io.fetchIssuePort.bits.PC
  val writeEn   = io.writeBackResult.toRegisterFile
  val writeData = io.writeBackResult.rdData
  val writeRd   = Wire(UInt(5.W))
  writeRd      := io.writeBackResult.rd
  val readyIn   = io.decodeIssuePort.ready

  // Initializing some registers for outputs
  val decodeIssueBuffer = Reg(new DecodeIssuePort)

  // Initializing some intermediate wires
  val validOut  = WireDefault(0.U(1.W))
  val readyOut  = WireDefault(0.U(1.W))
  val insType   = WireDefault(0.U(3.W))
  val rdValid   = WireDefault(1.U(1.W))
  val rs1Valid  = WireDefault(1.U(1.W))
  val rs2Valid  = WireDefault(1.U(1.W))
  val stalled   = WireDefault(0.U(1.W))
  val ins       = WireDefault(0.U(32.W))

  
  // Storing instruction value in a register
  when(io.fetchIssuePort.valid === 1.U & io.fetchIssuePort.ready === 1.U) {
    decodeIssueBuffer.instruction := io.fetchIssuePort.bits.instruction
    decodeIssueBuffer.PC          := io.fetchIssuePort.bits.PC
  }

  ins := Mux(io.fetchIssuePort.valid === 1.U & io.fetchIssuePort.ready === 1.U, io.fetchIssuePort.bits.instruction, decodeIssueBuffer.instruction)

  rdBuffer := ins(11, 7)

  // Assigning outputs
  io.decodeIssuePort.valid := validOut
  io.fetchIssuePort.ready  := readyOut
  io.decodeIssuePort.bits  := decodeIssueBuffer

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
    is(itype.U) { decodeIssueBuffer.immediate := Cat(Fill(53, ins(31)), ins(30, 20)) }
    is(stype.U) { decodeIssueBuffer.immediate := Cat(Fill(53, ins(31)), ins(30,25), ins(11, 7)) }
    is(btype.U) { decodeIssueBuffer.immediate := Cat(Fill(53, ins(31)), ins(7), ins(30,25), ins(11,8), 0.U(1.W)) }
    is(utype.U) { decodeIssueBuffer.immediate := Cat(Fill(32, ins(31)), ins(31,12), 0.U(12.W)) }
    is(jtype.U) { decodeIssueBuffer.immediate := Cat(Fill(44, ins(31)), ins(19,12), ins(20), ins(30,25), ins(24,21), 0.U(1.W)) }
    is(ntype.U) { decodeIssueBuffer.immediate := Fill(64, 0.U) }
    is(rtype.U) { decodeIssueBuffer.immediate := Fill(64, 0.U) }
  }

  // Valid bits for each register
  val validBit = RegInit(VecInit(Seq.fill(32)(1.U(1.W))))
  validBit(0) := 1.U

  // Initializing the Register file
  val registerFile = Reg(Vec(32, UInt(64.W)))
  registerFile(0) := 0.U

  decodeIssueBuffer.rs1 := registerFile(ins(19, 15))
  decodeIssueBuffer.rs2 := registerFile(ins(24, 20))


  // Register writing
  when(writeEn === 1.U & writeRd =/= 0.U) {
    registerFile(writeRd) := writeData
    validBit(writeRd)     := 1.U
  }

  // Checking rs1 validity
  when(insType === rtype.U | insType === itype.U | insType === stype.U | insType === btype.U) {
    when(validBit(ins(19, 15)) === 0.U & ~(io.branchMisspredict & ins(19, 15) === rdBuffer)) { rs1Valid := 0.U }
  }
  // Checking rs2 validity
  when(insType === rtype.U | insType === stype.U | insType === btype.U) {
    when(validBit(ins(24, 20)) === 0.U & ~(io.branchMisspredict & ins(24, 20) === rdBuffer)) { rs2Valid := 0.U }
  }
  // Checking rd validity and changing the valid bit for rd
  when(insType === rtype.U | insType === utype.U | insType === itype.U | insType === jtype.U) {
    when(validBit(ins(11, 7) ) === 0.U & ~(io.branchMisspredict & ins(11, 7) === rdBuffer)) { rdValid := 0.U }
    .otherwise {
      when(rs1Valid === 1.U & rs2Valid === 1.U & readyIn === 1.U & validOut === 1.U & ins(11, 7) =/= 0.U) { validBit(ins(11, 7)) := 0.U }
    }
  }

  when(io.branchMisspredict & ins(11, 7) =/= rdBuffer) {
    validBit(rdBuffer) := 1.U
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
      stateReg := Mux(readyIn === 0.U, waitState, Mux(validIn === 1.U, Mux(stalled === 1.U, stallState, passthroughState), idleState))
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
  emitVerilog(new DecodeUnit())
}