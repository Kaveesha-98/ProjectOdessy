package pipeline.decode

import chisel3._
import chisel3.util._

import Constants._
import pipeline.ports.{DecodeIssuePort, WriteBackResult, handshake}

/**
 * This module will output whether the input instruction is a branch or not
 *
 * Input:  instr     = Instruction to check
 * Output: is_branch = Branch or not
 */
class branch_detector extends Module {
  val io = IO(new Bundle {
    val instr     = Input(UInt(32.W))
    val is_branch = Output(Bool())
  })
  val op1       = io.instr(6,5)
  val op2       = io.instr(4,2)
  val flag1     = op1 === "b11".U
  val flag2     = op2 === "b001".U || op2 === "b011".U || op2 === "b000".U
  io.is_branch :=  flag1 && flag2
}

/**
 * Interface ports between fetch unit and decode unit
 *
 * Input:  issued         = Received data valid or not
 * Input:  PC             = Address of current input instruction
 * Input:  instruction    = Current input instruction
 * Output: ready          = Fetch unit is ready to take next instruction or not
 * Output: expected.valid = Expected PC is valid or not
 * Output: expected.PC    = Expected PC value from the fetch unit
 */
class fetchIssueIntfce extends Bundle {
  val ready       = Output(Bool())
  val issued      = Input(Bool())
  val PC          = Input(UInt(64.W))
  val instruction = Input(UInt(64.W))

  val expected = new Bundle {
    val valid = Output(Bool())
    val PC    = Output(UInt(64.W))
  }
}

/**
 * Interface ports for branch results
 *
 * Output: valid         = Signals from this interface is valid or not
 * Output: branchTaken   = Branch is taken or not
 * Output: PC            = Address of the branch instruction
 * Output: targetAddress = Address of the target instruction after the branch
 */
class BranchResult extends Bundle {
  val valid         = Bool()
  val branchTaken   = Bool()
  val PC            = UInt(64.W)
  val targetAddress = UInt(64.W)
}

/**
 * Body of the Decode Module
 *
 */
class DecodeUnit extends Module{
  // Initializing the input/output ports
  val fetchIssueIntfce  = IO(new fetchIssueIntfce)
  val decodeIssuePort = IO(Flipped(new handshake(new DecodeIssuePort)))
  val writeBackResult = IO(Input(new WriteBackResult))
  val branchResult    = IO(Output(new BranchResult))

  // Assigning some wires for inputs
  val validIn   = fetchIssueIntfce.issued
  val writeEn   = writeBackResult.toRegisterFile
  val writeData = writeBackResult.rdData
  val writeRd   = Wire(UInt(5.W))
  writeRd      := writeBackResult.rd
  val readyIn   = decodeIssuePort.ready

  // Initializing a buffer for storing the input values (PC, instruction)
  val fetchIssueBuffer = RegInit({
    val initialState = Wire(new Bundle() {
      val PC          = UInt(64.W)
      val instruction = UInt(32.W)
    })
    initialState.PC := "h80000000".U - 4.U   // Initial value is set for the use of expectedPC
    initialState.instruction := 0.U
    initialState
  })

  // Initializing some intermediate wires
  val validOut  = WireDefault(false.B)        // Valid signal from decode unit to execute unit
  val readyOut = WireDefault(false.B)        // Ready signal from decode unit to fetch unit

  val insType   = WireDefault(0.U(3.W))      // RISC-V type of the instruction
  val immediate = WireDefault(0.U(64.W))     // Immediate value of the instruction
  val rs1Data   = WireDefault(0.U(64.W))     // rs1 source register data
  val rs2Data   = WireDefault(0.U(64.W))     // rs2 source register data

  val rdValid  = WireDefault(true.B)         // Destination register valid signal
  val rs1Valid = WireDefault(true.B)         // rs1 register valid signal
  val rs2Valid = WireDefault(true.B)         // rs2 register valid signal
  val stalled  = WireDefault(false.B)       // Stall signal

  val ins = WireDefault(0.U(32.W))           // Current instruction
//  val pc  = WireDefault(0.U(64.W))           // Address of current instruction

  val isBranch   = WireDefault(false.B)      // Current instruction is a branch or not
  val expectedPC = WireDefault(0.U(64.W))    // Expected PC value from the fetch unit

  val branchValid   = WireDefault(false.B)   // Branch result port signals are valid or not
  val branchIsTaken = WireDefault(false.B)   // Branch is taken or not
  val branchPC      = WireDefault(0.U(64.W)) // Address of the branch instruction
  val branchTarget  = WireDefault(0.U(64.W)) // Next address of the instruction after the branch

  // Initializing states for the FSM
  val emptyState :: fullState :: Nil = Enum(2) // States of FSM
  val stateReg = RegInit(emptyState)

  // Storing instruction and PC in a buffer
  when(validIn && readyOut && fetchIssueIntfce.expected.PC === fetchIssueIntfce.PC) {
    fetchIssueBuffer.instruction := fetchIssueIntfce.instruction
    fetchIssueBuffer.PC          := fetchIssueIntfce.PC
  }

  ins := fetchIssueBuffer.instruction
  val pc  = fetchIssueBuffer.PC
  
  // Assigning outputs
  decodeIssuePort.valid             := validOut
  decodeIssuePort.bits.PC           := fetchIssueBuffer.PC
  decodeIssuePort.bits.instruction  := fetchIssueBuffer.instruction
  decodeIssuePort.bits.immediate    := immediate
  decodeIssuePort.bits.rs1          := rs1Data
  decodeIssuePort.bits.rs2          := rs2Data
  decodeIssuePort.bits.predNextAddr := 0.U

  fetchIssueIntfce.ready          := readyOut
  fetchIssueIntfce.expected.PC    := expectedPC
  fetchIssueIntfce.expected.valid := readyOut

  branchResult.valid         := branchValid
  branchResult.branchTaken   := branchIsTaken
  branchResult.PC            := branchPC
  branchResult.targetAddress := branchTarget

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
    is(jtype.U) { immediate:= Cat(Fill(44, ins(31)), ins(19,12), ins(20), ins(30,25), ins(24,21), 0.U(1.W)) }
    is(ntype.U) { immediate := Fill(64, 0.U) }
    is(rtype.U) { immediate := Fill(64, 0.U) }
  }

  // Valid bits for each register
  val validBit = RegInit(VecInit(Seq.fill(32)(1.U(1.W))))
  validBit(0) := 1.U

  // Initializing the Register file
  val registerFile = Reg(Vec(32, UInt(64.W)))
  registerFile(0) := 0.U

  rs1Data := registerFile(ins(19, 15))
  rs2Data := registerFile(ins(24, 20))

  // Register writing
  when(writeEn === 1.U && validBit(writeRd) === 0.U && writeRd =/= 0.U) {
    registerFile(writeRd) := writeData
    validBit(writeRd)     := 1.U
  }

  // Checking rs1 validity
  when(insType === rtype.U || insType === itype.U || insType === stype.U || insType === btype.U) {
    when(validBit(ins(19, 15)) === 0.U) { rs1Valid := false.B }
  }
  // Checking rs2 validity
  when(insType === rtype.U || insType === stype.U || insType === btype.U) {
    when(validBit(ins(24, 20)) === 0.U) { rs2Valid := false.B }
  }
  // Checking rd validity and changing the valid bit for rd
  when(insType === rtype.U || insType === utype.U || insType === itype.U || insType === jtype.U) {
    when(validBit(ins(11, 7)) === 0.U) { rdValid := false.B }
    .otherwise {
      when(validOut && readyIn  && ins(11, 7) =/= 0.U) { validBit(ins(11, 7)) := 0.U }
    }
  }

  when(stateReg === fullState) {
    stalled := !(rdValid && rs1Valid && rs2Valid) // stall signal for FSM
  }

  // FSM for ready valid interface
  // ------------------------------------------------------------------------------------------------------------------ //
  switch(stateReg) {
    is(emptyState) {
      validOut := false.B
      readyOut := true.B
      when(validIn && fetchIssueIntfce.PC === fetchIssueIntfce.expected.PC) {
        stateReg := fullState
      }
    }
    is(fullState) {
      when(stalled) {
        validOut := false.B
        readyOut := false.B
      } otherwise {
        validOut := true.B
        when(readyIn) {
          readyOut := true.B
          when(!validIn || fetchIssueIntfce.PC =/= fetchIssueIntfce.expected.PC) {
            stateReg := emptyState
          }
        } otherwise {
          readyOut := false.B
        }
      }
    }
  }
  // ------------------------------------------------------------------------------------------------------------------ //

  // Branch handling
  //  ------------------------------------------------------------------------------------------------------------------------------------------------------
  val branch_detector = Module(new branch_detector)
  branch_detector.io.instr := ins
  isBranch := branch_detector.io.is_branch

  def getResult(pattern: (chisel3.util.BitPat, chisel3.UInt), prev: UInt) = pattern match {
    case (bitpat, result) => Mux(ins === bitpat, result, prev)
  }

  when(isBranch && !stalled) {
    val branchTaken = (Seq(
      rs1Data === rs2Data,
      rs1Data =/= rs2Data,
      rs1Data.asSInt < rs2Data.asSInt,
      rs1Data.asSInt >= rs2Data.asSInt,
      rs1Data < rs2Data,
      rs1Data >= rs2Data
    ).zip(Seq(0, 1, 4, 5, 6, 7).map(i => i.U === ins(14, 12))
    ).map(condAndMatch => condAndMatch._1 && condAndMatch._2).reduce(_ || _))

    val brachNextAddress = Mux(branchTaken, (pc + immediate), (pc + 4.U))

    val target = Seq(
      BitPat("b?????????????????????????1101111") -> (pc + immediate), // jal
      BitPat("b?????????????????????????1100111") -> (rs1Data + immediate), //jalr
      BitPat("b?????????????????????????1100011") -> brachNextAddress, // branches
    ).foldRight((pc + 4.U))(getResult)

    expectedPC := target
    when(stateReg === fullState) {
      branchValid := true.B
    }
    branchIsTaken := branchTaken
    branchPC := pc
    branchTarget := target
  } otherwise {
    expectedPC := pc + 4.U
  }
  //  -----------------------------------------------------------------------------------------------------------------------------------------------------------

}

object DECODE_ISSUE_UNIT extends App{
  emitVerilog(new DecodeUnit())
}
