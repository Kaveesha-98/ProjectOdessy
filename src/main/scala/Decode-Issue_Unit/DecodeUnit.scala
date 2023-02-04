package pipeline.decode

import Constants._

import chisel3._
import chisel3.util._

class handshake[T <: Data](gen: T) extends Bundle {
  val ready = Output(UInt(1.W))
  val valid = Input(UInt(1.W))
  val bits  = Input(gen)
}

// Inputs from Fetch unit
class FetchIssuePort extends Bundle {
  val instruction = UInt(32.W)
  val PC          = UInt(64.W)
}

// Outputs for ALU
class DecodeIssuePort extends Bundle {
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

class DecodeUnit extends Module{
  val io = IO(new Bundle() {
    val fetchIssuePort  = new handshake(new FetchIssuePort)
    val decodeIssuePort = Flipped(new handshake(new DecodeIssuePort))
    val writeBackResult = Input(new WriteBackResult)
  })

  // Assigning some wires for inputs
  val validIn   = io.fetchIssuePort.valid
//  val pc        = io.fetchIssuePort.bits.PC
  val writeEn   = io.writeBackResult.toRegisterFile
  val writeData = io.writeBackResult.rdData
  val writeRd   = Wire(UInt(5.W))
  writeRd      := io.writeBackResult.rd
  val readyIn   = io.decodeIssuePort.ready

  // Initializing some registers for outputs
  val fetchIssueBuffer = RegInit({
    val initialState = Wire(new Bundle() {
      val pc = UInt(64.W)
      val ins = UInt(32.W)
    })
    initialState.pc := 0.U
    initialState.ins := 0.U
    initialState
  })

  // Initializing a buffer for storing the output values to execution unit
  val decodeIssueBuffer = RegInit({
    val initialState = Wire(new Bundle() {
      val instruction = UInt(32.W)
      val PC = UInt(64.W)
      val rs1 = UInt(64.W)
      val rs2 = UInt(64.W)
      val immediate = UInt(64.W)
    })
    initialState.instruction := 0.U
    initialState.PC := 0.U
    initialState.rs1 := 0.U
    initialState.rs2 := 0.U
    initialState.immediate := 0.U
    initialState
  })

  // Initializing some intermediate wires
  val validOutFetchBuf  = WireDefault(0.U(1.W))
  val readyOutFetchBuf  = WireDefault(0.U(1.W))
  val validOutDecodeBuf = WireDefault(0.U(1.W)) // Valid signal of decode buffer
  val readyOutDecodeBuf = WireDefault(0.U(1.W)) // Ready signal of decode buffer
  val insType   = WireDefault(0.U(3.W))
  val rdValid   = WireDefault(1.U(1.W))
  val rs1Valid  = WireDefault(1.U(1.W))
  val rs2Valid  = WireDefault(1.U(1.W))
  val stalled   = WireDefault(0.U(1.W))
  val ins       = WireDefault(0.U(32.W))
  val imm       = WireDefault(0.U(64.W))
  val rs1       = WireDefault(0.U(64.W))
  val rs2       = WireDefault(0.U(64.W ))

  
  // Storing instruction value in a register
  when(validIn === 1.U & readyOutFetchBuf === 1.U) {
    fetchIssueBuffer.ins := io.fetchIssuePort.bits.instruction
    fetchIssueBuffer.pc  := io.fetchIssuePort.bits.PC
  }

  ins := fetchIssueBuffer.ins
  val pc  = fetchIssueBuffer.pc

  // Storing values to the decode buffer
  when(validOutFetchBuf === 1.U & readyOutDecodeBuf === 1.U) {
    decodeIssueBuffer.instruction := ins
    decodeIssueBuffer.PC := pc
    decodeIssueBuffer.immediate := imm
    decodeIssueBuffer.rs1 := rs1
    decodeIssueBuffer.rs2 := Mux(ins(6, 2) === BitPat("b001?0"), imm, rs2)//rs2
  }

  // Assigning outputs
  io.decodeIssuePort.bits.PC          := decodeIssueBuffer.PC
  io.decodeIssuePort.bits.instruction := decodeIssueBuffer.instruction
  io.decodeIssuePort.bits.immediate   := decodeIssueBuffer.immediate
  io.decodeIssuePort.valid            := validOutDecodeBuf
  io.decodeIssuePort.bits.rs1         := decodeIssueBuffer.rs1
  io.decodeIssuePort.bits.rs2         := decodeIssueBuffer.rs2
  io.fetchIssuePort.ready             := readyOutFetchBuf
  io.decodeIssuePort.bits.opCode      := decodeIssueBuffer.instruction(6,0)

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
    is(itype.U) { imm := Cat(Fill(53, ins(31)), ins(30, 20)) }
    is(stype.U) { imm := Cat(Fill(53, ins(31)), ins(30,25), ins(11, 7)) }
    is(btype.U) { imm := Cat(Fill(53, ins(31)), ins(7), ins(30,25), ins(11,8), 0.U(1.W)) }
    is(utype.U) { imm := Cat(Fill(32, ins(31)), ins(31,12), 0.U(12.W)) }
    is(jtype.U) { imm := Cat(Fill(44, ins(31)), ins(19,12), ins(20), ins(30,25), ins(24,21), 0.U(1.W)) }
    is(ntype.U) { imm := Fill(64, 0.U) }
    is(rtype.U) { imm := Fill(64, 0.U) }
  }

  // FSM for ready valid interface
  // ------------------------------------------------------------------------------------------------------------------ //
  val emptyState :: fullState :: Nil = Enum(2) // States of FSM
  val stateReg = RegInit(emptyState)

  switch(stateReg) {
    is(emptyState) {
      validOutFetchBuf := 0.U
      readyOutFetchBuf := 1.U
      when(validIn === 1.U) {
        stateReg := fullState
      }
    }
    is(fullState) {
      when(stalled === 1.U) {
       validOutFetchBuf := 0.U
       readyOutFetchBuf := 0.U
      } otherwise {
        validOutFetchBuf := 1.U
        when(readyIn === 1.U) {
          readyOutFetchBuf := 1.U
          when(validIn === 0.U) {
            stateReg := emptyState
          }
        } otherwise {
          readyOutFetchBuf := 0.U
        }
      }
    }
  }
  // ------------------------------------------------------------------------------------------------------------------ //

  // Initializing states for the FSM of decode buffer
  val emptyState2 :: fullState2 :: Nil = Enum(2) // States of FSM
  val stateReg2 = RegInit(emptyState2)

  // FSM for ready valid interface of decode buffer
  // ------------------------------------------------------------------------------------------------------------------ //
  switch(stateReg2) {
    is(emptyState2) {
      validOutDecodeBuf := 0.U
      readyOutDecodeBuf := 1.U
      when(validOutFetchBuf === 1.U) {
        stateReg2 := fullState2
      }
    }
    is(fullState2) {
      validOutDecodeBuf := 1.U
      when(readyIn === 1.U) {
        readyOutDecodeBuf := 1.U
        when(!validOutFetchBuf) {
          stateReg2 := emptyState2
        }
      } otherwise {
        readyOutDecodeBuf := 0.U
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

  rs1 := registerFile(ins(19, 15))
  rs2 := registerFile(ins(24, 20))

  // Register writing
  when(writeEn === 1.U & writeRd =/= 0.U & validBit(writeRd) === 0.U) {
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
      when(validOutFetchBuf === 1.U & readyIn === 1.U & ins(11, 7) =/= 0.U) {
        validBit(ins(11, 7)) := 0.U
      }
    }
  }
  when(stateReg === fullState) {
    stalled := ~(rdValid & rs1Valid & rs2Valid)     // stall signal for FSM
  }
}

object DECODE_ISSUE_UNIT extends App{
  emitVerilog(new DecodeUnit())
}
