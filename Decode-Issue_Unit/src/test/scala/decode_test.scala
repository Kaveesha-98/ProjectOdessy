import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import decode_issue.DECODE_ISSUE_UNIT

class DecodeTest extends AnyFlatSpec with ChiselScalatestTester {
  "DUT" should "pass" in {
    test(new DECODE_ISSUE_UNIT).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      var step = 0

      println("Step " + step + ", validOut = " + dut.io.decodeIssuePort.valid.peek().toString())
      println("Step " + step + ", ins = " + dut.io.decodeIssuePort.bits.instruction.peek().toString())
      println("Step " + step + ", pc = " + dut.io.decodeIssuePort.bits.PC.peek().toString())
      println("Step " + step + ", rs1 = " + dut.io.decodeIssuePort.bits.rs1.peek().toString())
      println("Step " + step + ", rs2 = " + dut.io.decodeIssuePort.bits.rs2.peek().toString())
      println("Step " + step + ", imm = " + dut.io.decodeIssuePort.bits.immediate.peek().toString())
      println("Step " + step + ", opcode = " + dut.io.decodeIssuePort.bits.opCode.peek().toString())
      println("Step " + step + ", readyOut = " + dut.io.decodeIssuePort.ready.peek().toString())
      println("-------------------------------------------------------------")

      dut.io.fetchIssuePort.valid.poke(1.U)
      dut.io.fetchIssuePort.bits.PC.poke(1.U)
      dut.io.fetchIssuePort.bits.instruction.poke("b00111110100000000000000010010011".U)
      dut.io.writeBackResult.rd.poke(25.U)
      dut.io.writeBackResult.rdData.poke(1000.U)
      dut.io.writeBackResult.toRegisterFile.poke(0.U)
      dut.io.decodeIssuePort.ready.poke(1.U)

      dut.clock.step(1)
      step = step + 1

      println("Step " + step + ", validOut = " + dut.io.decodeIssuePort.valid.peek().toString())
      println("Step " + step + ", ins = " + dut.io.decodeIssuePort.bits.instruction.peek().toString())
      println("Step " + step + ", pc = " + dut.io.decodeIssuePort.bits.PC.peek().toString())
      println("Step " + step + ", rs1 = " + dut.io.decodeIssuePort.bits.rs1.peek().toString())
      println("Step " + step + ", rs2 = " + dut.io.decodeIssuePort.bits.rs2.peek().toString())
      println("Step " + step + ", imm = " + dut.io.decodeIssuePort.bits.immediate.peek().toString())
      println("Step " + step + ", opcode = " + dut.io.decodeIssuePort.bits.opCode.peek().toString())
      println("Step " + step + ", readyOut = " + dut.io.decodeIssuePort.ready.peek().toString())
      println("-------------------------------------------------------------")

      dut.io.fetchIssuePort.valid.poke(1.U)
      dut.io.fetchIssuePort.bits.PC.poke(2.U)
      dut.io.fetchIssuePort.bits.instruction.poke("b01111101000000001000000100010011".U)
      dut.io.writeBackResult.rd.poke(1.U)
      dut.io.writeBackResult.rdData.poke(1500.U)
      dut.io.writeBackResult.toRegisterFile.poke(0.U)
      dut.io.decodeIssuePort.ready.poke(1.U)

      dut.clock.step(1)
      step = step + 1

      println("Step " + step + ", validOut = " + dut.io.decodeIssuePort.valid.peek().toString())
      println("Step " + step + ", ins = " + dut.io.decodeIssuePort.bits.instruction.peek().toString())
      println("Step " + step + ", pc = " + dut.io.decodeIssuePort.bits.PC.peek().toString())
      println("Step " + step + ", rs1 = " + dut.io.decodeIssuePort.bits.rs1.peek().toString())
      println("Step " + step + ", rs2 = " + dut.io.decodeIssuePort.bits.rs2.peek().toString())
      println("Step " + step + ", imm = " + dut.io.decodeIssuePort.bits.immediate.peek().toString())
      println("Step " + step + ", opcode = " + dut.io.decodeIssuePort.bits.opCode.peek().toString())
      println("Step " + step + ", readyOut = " + dut.io.decodeIssuePort.ready.peek().toString())
      println("-------------------------------------------------------------")

      dut.io.fetchIssuePort.valid.poke(0.U)
      dut.io.fetchIssuePort.bits.PC.poke(2.U)
      dut.io.fetchIssuePort.bits.instruction.poke("b01111101000000001000000100010011".U)
      dut.io.writeBackResult.rd.poke(1.U)
      dut.io.writeBackResult.rdData.poke(1050.U)
      dut.io.writeBackResult.toRegisterFile.poke(1.U)
      dut.io.decodeIssuePort.ready.poke(1.U)

      dut.clock.step(1)
      step = step + 1

      println("Step " + step + ", validOut = " + dut.io.decodeIssuePort.valid.peek().toString())
      println("Step " + step + ", ins = " + dut.io.decodeIssuePort.bits.instruction.peek().toString())
      println("Step " + step + ", pc = " + dut.io.decodeIssuePort.bits.PC.peek().toString())
      println("Step " + step + ", rs1 = " + dut.io.decodeIssuePort.bits.rs1.peek().toString())
      println("Step " + step + ", rs2 = " + dut.io.decodeIssuePort.bits.rs2.peek().toString())
      println("Step " + step + ", imm = " + dut.io.decodeIssuePort.bits.immediate.peek().toString())
      println("Step " + step + ", opcode = " + dut.io.decodeIssuePort.bits.opCode.peek().toString())
      println("Step " + step + ", readyOut = " + dut.io.decodeIssuePort.ready.peek().toString())
      println("-------------------------------------------------------------")

      dut.io.fetchIssuePort.valid.poke(0.U)
      dut.io.fetchIssuePort.bits.PC.poke(2.U)
      dut.io.fetchIssuePort.bits.instruction.poke("b01111101000000001000000100010011".U)
      dut.io.writeBackResult.rd.poke(5.U)
      dut.io.writeBackResult.rdData.poke(1250.U)
      dut.io.writeBackResult.toRegisterFile.poke(0.U)
      dut.io.decodeIssuePort.ready.poke(0.U)

      dut.clock.step(1)
      step = step + 1

      println("Step " + step + ", validOut = " + dut.io.decodeIssuePort.valid.peek().toString())
      println("Step " + step + ", ins = " + dut.io.decodeIssuePort.bits.instruction.peek().toString())
      println("Step " + step + ", pc = " + dut.io.decodeIssuePort.bits.PC.peek().toString())
      println("Step " + step + ", rs1 = " + dut.io.decodeIssuePort.bits.rs1.peek().toString())
      println("Step " + step + ", rs2 = " + dut.io.decodeIssuePort.bits.rs2.peek().toString())
      println("Step " + step + ", imm = " + dut.io.decodeIssuePort.bits.immediate.peek().toString())
      println("Step " + step + ", opcode = " + dut.io.decodeIssuePort.bits.opCode.peek().toString())
      println("Step " + step + ", readyOut = " + dut.io.decodeIssuePort.ready.peek().toString())
      println("-------------------------------------------------------------")

      dut.io.fetchIssuePort.valid.poke(0.U)
      dut.io.fetchIssuePort.bits.PC.poke(2.U)
      dut.io.fetchIssuePort.bits.instruction.poke("b01111101000000001000000100010011".U)
      dut.io.writeBackResult.rd.poke(5.U)
      dut.io.writeBackResult.rdData.poke(1250.U)
      dut.io.writeBackResult.toRegisterFile.poke(0.U)
      dut.io.decodeIssuePort.ready.poke(1.U)

      dut.clock.step(1)
      step = step + 1

      println("Step " + step + ", validOut = " + dut.io.decodeIssuePort.valid.peek().toString())
      println("Step " + step + ", ins = " + dut.io.decodeIssuePort.bits.instruction.peek().toString())
      println("Step " + step + ", pc = " + dut.io.decodeIssuePort.bits.PC.peek().toString())
      println("Step " + step + ", rs1 = " + dut.io.decodeIssuePort.bits.rs1.peek().toString())
      println("Step " + step + ", rs2 = " + dut.io.decodeIssuePort.bits.rs2.peek().toString())
      println("Step " + step + ", imm = " + dut.io.decodeIssuePort.bits.immediate.peek().toString())
      println("Step " + step + ", opcode = " + dut.io.decodeIssuePort.bits.opCode.peek().toString())
      println("Step " + step + ", readyOut = " + dut.io.decodeIssuePort.ready.peek().toString())
      println("-------------------------------------------------------------")
    }
  }
}