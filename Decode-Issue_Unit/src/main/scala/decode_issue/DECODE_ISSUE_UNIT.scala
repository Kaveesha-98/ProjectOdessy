package decode_issue

import Constants._

import chisel3._
import chisel3.util._
import chisel3.Driver

class FetchIssuePort extends Bundle {
  val valid       = UInt(1.W)
  val instruction = UInt(32.W)
  val PC          = UInt(64.W)
}

class DecodeIssuePort extends Bundle {
  val valid       = UInt(1.W)
  val instruction = UInt(32.W)
  val PC          = UInt(64.W)
  val rs1         = UInt(64.W)
  val rs2         = UInt(64.W)
  val immediate   = UInt(64.W)
}

class WriteBackResult extends Bundle {
  val toRegisterFile = UInt(1.W)
  val rd             = UInt(5.W)
  val rdData         = UInt(64.W)
}

class UnitStatus extends Bundle {
  val stalled = UInt(1.W)
  val empty   = UInt(1.W)
}

class DECODE_ISSUE_UNIT extends Module{
  val io = IO(new Bundle() {
    val fetchIssuePort  = Input(new FetchIssuePort)
    val decodeIssuePort = Output(new DecodeIssuePort)
    val writeBackResult = Input(new WriteBackResult)
    val unitStatus      = Output(new UnitStatus)
    val pipeLineStalled = Input(UInt(1.W))
    val out             = Output(UInt(64.W))
  })

  io.unitStatus.stalled    := WireDefault(0.U(1.W))
  io.unitStatus.empty      := WireDefault(0.U(1.W))

  val validReg = RegInit(0.U(1.W))
  val pcReg    = RegInit(0.U(64.W))
  val insReg   = RegInit(0.U(32.W))
  val immReg   = RegInit(0.U(64.W))
  val rs1Reg   = RegInit(0.U(64.W))
  val rs2Reg   = RegInit(0.U(64.W))

  val insType = WireDefault(0.U)

  pcReg  := io.fetchIssuePort.PC
  insReg := io.fetchIssuePort.instruction

  io.decodeIssuePort.PC          := pcReg
  io.decodeIssuePort.instruction := insReg
  io.decodeIssuePort.immediate   := immReg
  io.decodeIssuePort.valid       := validReg
  io.decodeIssuePort.rs1         := rs1Reg
  io.decodeIssuePort.rs2         := rs2Reg

  val ins = io.fetchIssuePort.instruction

  switch (ins(6,0)) {
    is (lui.U)    { insType := utype.U }
    is (auipc.U)  { insType := utype.U }
    is (jump.U)   { insType := jtype.U }
    is (jumpr.U)  { insType := itype.U }
    is (cjump.U)  { insType := btype.U }
    is (load.U)   { insType := itype.U }
    is (store.U)  { insType := stype.U }
    is (iops.U)   { insType := itype.U }
    is (iops32.U) { insType := itype.U }
    is (rops.U)   { insType := rtype.U }
    is (rops32.U) { insType := rtype.U }
    is (system.U) { insType := itype.U }
    is (fence.U)  { insType := ntype.U }
    is (amos.U)   { insType := rtype.U }
  }

  switch (insType) {
    is (itype.U) { immReg := Cat(Fill(53, ins(31)), ins(30, 20)) }
    is (stype.U) { immReg := Cat(Fill(53, ins(31)), ins(30,25), ins(11, 7)) }
    is (btype.U) { immReg := Cat(Fill(53, ins(31)), ins(7), ins(30,25), ins(11,8), 0.U(1.W)) }
    is (utype.U) { immReg := Cat(Fill(32, ins(31)), ins(31,12), 0.U(12.W)) }
    is (jtype.U) { immReg := Cat(Fill(44, ins(31)), ins(19,12), ins(20), ins(30,25), ins(24,21), 0.U(1.W)) }
    is (ntype.U) { immReg := Fill(64, 0.U) }
    is (rtype.U) { immReg := Fill(64, 0.U) }
  }

  val validBit = RegInit(VecInit(Seq.fill(32)(0.U(1.W))))
  validBit(0) := 1.U
  validBit(2) := 1.U
  validBit(5) := 1.U

  val registerFile = Reg(Vec(32, UInt(64.W)))
  registerFile(0) := 0.U
  registerFile(1) := 125.U

  when(io.writeBackResult.toRegisterFile === 1.U & validBit(io.writeBackResult.rd) === 0.U & io.writeBackResult.rd =/= 0.U) {
    registerFile(io.writeBackResult.rd) := io.writeBackResult.rdData
    validBit(io.writeBackResult.rd) := 1.U
  }

  rs1Reg := registerFile(ins(19, 15))
  rs2Reg := registerFile(ins(24, 20))

  when(io.fetchIssuePort.valid === 1.U & (insType === rtype.U | insType === utype.U | insType === itype.U | insType === jtype.U)) {
    when(validBit(ins(11, 7)) === 0.U) {
      io.unitStatus.stalled := 1.U
    } otherwise {
      validBit(ins(11, 7)) := 0.U
      io.unitStatus.stalled := 0.U
    }
  }

  when(io.fetchIssuePort.valid === 0.U) { io.unitStatus.empty := 1.U }
  .otherwise { io.unitStatus.empty := 0.U }

  when(io.fetchIssuePort.valid === 0.U) { validReg := 0.U}
  .otherwise {
    when(io.pipeLineStalled === 0.U & io.unitStatus.stalled === 0.U) { validReg := 1.U }
    when(io.pipeLineStalled === 0.U & io.unitStatus.stalled === 1.U) { validReg := 0.U }
    when(io.pipeLineStalled === 1.U & io.unitStatus.stalled === 0.U) { validReg := 1.U }
    when(io.pipeLineStalled === 1.U & io.unitStatus.stalled === 1.U) { validReg := 0.U }
  }

  io.out := registerFile(1)
}

object DECODE_ISSUE_UNIT extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new DECODE_ISSUE_UNIT())
}