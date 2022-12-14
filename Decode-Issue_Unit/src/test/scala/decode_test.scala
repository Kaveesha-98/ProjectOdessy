//import chisel3._
//import chisel3.iotesters._
//import decode_issue.DECODE_ISSUE_UNIT
//
//class TestDecode(dut: DECODE_ISSUE_UNIT) extends PeekPokeTester(dut) {
//  var step = 0
//
//  println("Step " + step + ", valid = " + peek(dut.io.decodeIssuePort.valid))
//  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
//  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
//  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
//  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
//  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
//  println("Step " + step + ", stall = " + peek(dut.io.unitStatus.stalled))
//  println("Step " + step + ", empty = " + peek(dut.io.unitStatus.empty))
////  println("Step " + step + ", out = " + peek(dut.io.out))
//  println("-------------------------------------------------------------")
//
//  poke(dut.io.fetchIssuePort.valid, 1.U)
//  poke(dut.io.fetchIssuePort.PC, 150.U)
//  poke(dut.io.fetchIssuePort.instruction, "h7d008113".U)
//  poke(dut.io.writeBackResult.rd, 1.U)
//  poke(dut.io.writeBackResult.rdData, 1000.U)
//  poke(dut.io.writeBackResult.toRegisterFile, 0.U)
//  poke(dut.io.pipeLineStalled, 0.U)
//
//  step(1)
//  step = step + 1
//
//  println("Step " + step + ", valid = " + peek(dut.io.decodeIssuePort.valid))
//  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
//  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
//  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
//  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
//  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
//  println("Step " + step + ", stall = " + peek(dut.io.unitStatus.stalled))
//  println("Step " + step + ", empty = " + peek(dut.io.unitStatus.empty))
////  println("Step " + step + ", out = " + peek(dut.io.out))
//  println("-------------------------------------------------------------")
//
//  poke(dut.io.fetchIssuePort.valid, 1.U)
//  poke(dut.io.fetchIssuePort.PC, 151.U)
//  poke(dut.io.fetchIssuePort.instruction, "h3e820293".U)
//  poke(dut.io.writeBackResult.rd, 1.U)
//  poke(dut.io.writeBackResult.rdData, 1000.U)
//  poke(dut.io.writeBackResult.toRegisterFile, 0.U)
//  poke(dut.io.pipeLineStalled, 0.U)
//
//  step(1)
//  step = step + 1
//
//  println("Step " + step + ", valid = " + peek(dut.io.decodeIssuePort.valid))
//  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
//  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
//  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
//  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
//  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
//  println("Step " + step + ", stall = " + peek(dut.io.unitStatus.stalled))
//  println("Step " + step + ", empty = " + peek(dut.io.unitStatus.empty))
//  println("-------------------------------------------------------------")
//
//  step(1)
//  step = step + 1
//
//  println("Step " + step + ", valid = " + peek(dut.io.decodeIssuePort.valid))
//  println("Step " + step + ", ins = " + peek(dut.io.decodeIssuePort.instruction))
//  println("Step " + step + ", pc = " + peek(dut.io.decodeIssuePort.PC))
//  println("Step " + step + ", rs1 = " + peek(dut.io.decodeIssuePort.rs1))
//  println("Step " + step + ", rs2 = " + peek(dut.io.decodeIssuePort.rs2))
//  println("Step " + step + ", imm = " + peek(dut.io.decodeIssuePort.immediate))
//  println("Step " + step + ", stall = " + peek(dut.io.unitStatus.stalled))
//  println("Step " + step + ", empty = " + peek(dut.io.unitStatus.empty))
//  println("-------------------------------------------------------------")
//}
//
//object TestDecode extends App {
//  chisel3.iotesters.Driver(() => new DECODE_ISSUE_UNIT()) { c =>
//    new TestDecode(c)
//  }
//}