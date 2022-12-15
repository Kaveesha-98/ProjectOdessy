import chisel3._
import chisel3.iotesters._
import decode_issue.DECODE_ISSUE_UNIT

class TestDecode(dut: DECODE_ISSUE_UNIT) extends PeekPokeTester(dut) {
  var step = 0

  println("Step " + step + ", validOut = " + peek(dut.io.decodeIssuePort.valid))
  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
  println("Step " + step + ", opcode = " + peek(dut.io.decodeIssuePort.opCode))
  println("Step " + step + ", readyOut = " + peek(dut.io.readyOut))
  println("-------------------------------------------------------------")

  poke(dut.io.fetchIssuePort.valid, 1.U)
  poke(dut.io.fetchIssuePort.PC, 1.U)
  poke(dut.io.fetchIssuePort.instruction, "b00111110100000000000000010010011".U)
  poke(dut.io.writeBackResult.rd, 0.U)
  poke(dut.io.writeBackResult.rdData, 0.U)
  poke(dut.io.writeBackResult.toRegisterFile, 0.U)
  poke(dut.io.readyIn, 1.U)

  step(1)
  step = step + 1

  println("Step " + step + ", validOut = " + peek(dut.io.decodeIssuePort.valid))
  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
  println("Step " + step + ", opcode = " + peek(dut.io.decodeIssuePort.opCode))
  println("Step " + step + ", readyOut = " + peek(dut.io.readyOut))
  println("-------------------------------------------------------------")

  poke(dut.io.fetchIssuePort.valid, 1.U)
  poke(dut.io.fetchIssuePort.PC, 2.U)
  poke(dut.io.fetchIssuePort.instruction, "b01111101000000001000000100010011".U)
  poke(dut.io.writeBackResult.rd, 0.U)
  poke(dut.io.writeBackResult.rdData, 0.U)
  poke(dut.io.writeBackResult.toRegisterFile, 0.U)
  poke(dut.io.readyIn, 1.U)

  step(1)
  step = step + 1

  println("Step " + step + ", validOut = " + peek(dut.io.decodeIssuePort.valid))
  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
  println("Step " + step + ", opcode = " + peek(dut.io.decodeIssuePort.opCode))
  println("Step " + step + ", readyOut = " + peek(dut.io.readyOut))
  println("-------------------------------------------------------------")

  poke(dut.io.fetchIssuePort.valid, 1.U)
  poke(dut.io.fetchIssuePort.PC, 3.U)
  poke(dut.io.fetchIssuePort.instruction,"b11000001100000010000000110010011".U)
  poke(dut.io.writeBackResult.rd, 1.U)
  poke(dut.io.writeBackResult.rdData, 225.U)
  poke(dut.io.writeBackResult.toRegisterFile, 1.U)
  poke(dut.io.readyIn, 1.U)

  step(1)
  step = step + 1

  println("Step " + step + ", validOut = " + peek(dut.io.decodeIssuePort.valid))
  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
  println("Step " + step + ", opcode = " + peek(dut.io.decodeIssuePort.opCode))
  println("Step " + step + ", readyOut = " + peek(dut.io.readyOut))
  println("-------------------------------------------------------------")

  poke(dut.io.fetchIssuePort.valid, 1.U)
  poke(dut.io.fetchIssuePort.PC, 3.U)
  poke(dut.io.fetchIssuePort.instruction, "b11000001100000010000000110010011".U)
  poke(dut.io.writeBackResult.rd, 0.U)
  poke(dut.io.writeBackResult.rdData, 0.U)
  poke(dut.io.writeBackResult.toRegisterFile, 0.U)
  poke(dut.io.readyIn, 1.U)

  step(1)
  step = step + 1

  println("Step " + step + ", validOut = " + peek(dut.io.decodeIssuePort.valid))
  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
  println("Step " + step + ", opcode = " + peek(dut.io.decodeIssuePort.opCode))
  println("Step " + step + ", readyOut = " + peek(dut.io.readyOut))
  println("-------------------------------------------------------------")
}

object TestDecode extends App {
  chisel3.iotesters.Driver(() => new DECODE_ISSUE_UNIT()) { c =>
    new TestDecode(c)
  }
}