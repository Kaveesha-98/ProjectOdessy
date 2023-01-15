package pipeline.decode

import Constants._
import chisel3._
import chisel3.util._
import pipeline.ports.{BranchResult, DecodeIssuePort, FetchIssuePort, WriteBackResult}

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
    val branchResult    = Input(new BranchResult)
  })

  // Assigning some wires for inputs
  val validIn   = io.fetchIssuePort.valid
  val writeEn   = io.writeBackResult.toRegisterFile
  val writeData = io.writeBackResult.rdData
  val writeRd   = Wire(UInt(5.W))
  writeRd      := io.writeBackResult.rd
  val readyIn   = io.decodeIssuePort.ready

  // Initializing some registers for buffers
  val fetchIssueBuffer = Reg(new FetchIssuePort)

  // Initializing some intermediate wires
  val validOut      = WireDefault(0.U(1.W))
  val readyOut      = WireDefault(0.U(1.W))
  val insType       = WireDefault(0.U(3.W))
  val rdValid       = WireDefault(1.U(1.W))
  val rs1Valid      = WireDefault(1.U(1.W))
  val rs2Valid      = WireDefault(1.U(1.W))
  val stalled       = WireDefault(0.U(1.W))
  val immediate     = WireDefault(0.U(64.W))
  val ins           = WireDefault(0.U(32.W))
  val rs1Data       = WireDefault(0.U(64.W))
  val rs2Data       = WireDefault(0.U(64.W))
  val missPredicted = WireDefault(false.B)

  val prevRd        = RegInit(0.U(5.W))

  missPredicted := io.branchResult.valid & ~io.branchResult.predicted

  // Storing input value in a buffer
  when(validIn === 1.U & readyOut === 1.U) {
    fetchIssueBuffer.instruction  := io.fetchIssuePort.bits.instruction
    fetchIssueBuffer.PC           := io.fetchIssuePort.bits.PC
    fetchIssueBuffer.predNextAddr := io.fetchIssuePort.bits.predNextAddr
  }

  ins    := fetchIssueBuffer.instruction
  prevRd := ins(11, 7)

  // Assigning outputs
  io.decodeIssuePort.valid             := validOut
  io.fetchIssuePort.ready              := readyOut
  io.decodeIssuePort.bits.instruction  := fetchIssueBuffer.instruction
  io.decodeIssuePort.bits.PC           := fetchIssueBuffer.PC
  io.decodeIssuePort.bits.rs1          := rs1Data
  io.decodeIssuePort.bits.rs2          := rs2Data
  io.decodeIssuePort.bits.immediate    := immediate
  io.decodeIssuePort.bits.predNextAddr := fetchIssueBuffer.predNextAddr

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
    is(itype.U) { immediate := Cat(Fill(53, ins(31)), ins(30, 20)) }
    is(stype.U) { immediate := Cat(Fill(53, ins(31)), ins(30,25), ins(11, 7)) }
    is(btype.U) { immediate := Cat(Fill(53, ins(31)), ins(7), ins(30,25), ins(11,8), 0.U(1.W)) }
    is(utype.U) { immediate := Cat(Fill(32, ins(31)), ins(31,12), 0.U(12.W)) }
    is(jtype.U) { immediate := Cat(Fill(44, ins(31)), ins(19,12), ins(20), ins(30,25), ins(24,21), 0.U(1.W)) }
    is(ntype.U) { immediate := Fill(64, 0.U) }
    is(rtype.U) { immediate := Fill(64, 0.U) }
  }

  // FSM for ready valid interface
  // ------------------------------------------------------------------------------------------------------------------ //
  val emptyState :: fullState :: Nil = Enum(2) // States of FSM
  val stateReg = RegInit(emptyState)

  switch(stateReg) {
    is(emptyState) {
      validOut := 0.U
      readyOut := 1.U
      when(missPredicted) {
        stateReg := emptyState
      } otherwise {
        when(validIn === 1.U) {
          stateReg := fullState
        }
      }
    }
    is(fullState) {
      when(missPredicted) {
        validOut := 0.U
        readyOut := 0.U
        stateReg := emptyState
      } otherwise {
        when(stalled === 1.U) {
          validOut := 0.U
          readyOut := 0.U
        } otherwise {
          validOut := 1.U
          when(readyIn === 1.U) {
            readyOut := 1.U
            when(validIn === 0.U) {
              stateReg := emptyState
            }
          } otherwise {
            readyOut := 0.U
          }
        }
      }
    }
  }
  // ------------------------------------------------------------------------------------------------------------------ //

  // Valid bits for each register
  val validBit = RegInit(VecInit(Seq.fill(32)(1.U(1.W))))
  validBit(0) := 1.U

  // Initializing the Register file
  val registerFile = Reg(Vec(32, UInt(64.W)))
  registerFile(0) := 0.U

  rs1Data := registerFile(ins(19, 15))
  rs2Data := registerFile(ins(24, 20))

  // Register writing
  when(writeEn === 1.U & writeRd =/= 0.U) {
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
    when(validBit(ins(11, 7) ) === 0.U) { rdValid := 0.U }
    .otherwise {
      when(readyIn === 1.U & validOut === 1.U & ins(11, 7) =/= 0.U & ~missPredicted) { validBit(ins(11, 7)) := 0.U }
    }
  }

  when(missPredicted) { validBit(prevRd) := 1.U }

  stalled := ~(rdValid & rs1Valid & rs2Valid)     // stall signal for FSM

}

object DECODE_ISSUE_UNIT extends App{
  emitVerilog(new DecodeUnit())
}