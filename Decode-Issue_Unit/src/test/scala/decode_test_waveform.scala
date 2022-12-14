import chisel3.iotesters.PeekPokeTester
import chisel3.iotesters.Driver
import decode_issue.DECODE_ISSUE_UNIT
import org.scalatest._

class TestDecodeWaveform(dut: DECODE_ISSUE_UNIT) extends PeekPokeTester(dut) {

  poke(dut.io.fetchIssuePort.valid, 1)
  poke(dut.io.fetchIssuePort.PC, 1)
  poke(dut.io.fetchIssuePort.instruction, Integer.parseInt("3e800093", 16))
  poke(dut.io.writeBackResult.rd, 25)
  poke(dut.io.writeBackResult.rdData, 1000)
  poke(dut.io.writeBackResult.toRegisterFile, 0)
  poke(dut.io.readyIn, 1)

  step(1)

  poke(dut.io.fetchIssuePort.valid, 1)
  poke(dut.io.fetchIssuePort.PC, 2)
  poke(dut.io.fetchIssuePort.instruction, Integer.parseInt("7d008113", 16))
  poke(dut.io.writeBackResult.rd, 1)
  poke(dut.io.writeBackResult.rdData, 1500)
  poke(dut.io.writeBackResult.toRegisterFile, 0)
  poke(dut.io.readyIn, 1)

  step(1)

  poke(dut.io.fetchIssuePort.valid, 0)
  poke(dut.io.fetchIssuePort.PC, 2)
  poke(dut.io.fetchIssuePort.instruction, Integer.parseInt("7d008113", 16))
  poke(dut.io.writeBackResult.rd, 1)
  poke(dut.io.writeBackResult.rdData, 1050)
  poke(dut.io.writeBackResult.toRegisterFile, 1)
  poke(dut.io.readyIn, 1)

  step(1)

  poke(dut.io.fetchIssuePort.valid, 0)
  poke(dut.io.fetchIssuePort.PC, 2)
  poke(dut.io.fetchIssuePort.instruction, Integer.parseInt("7d008113", 16))
  poke(dut.io.writeBackResult.rd, 5)
  poke(dut.io.writeBackResult.rdData, 1250)
  poke(dut.io.writeBackResult.toRegisterFile, 0)
  poke(dut.io.readyIn, 0)

  step(1)

  poke(dut.io.fetchIssuePort.valid, 0)
  poke(dut.io.fetchIssuePort.PC, 2)
  poke(dut.io.fetchIssuePort.instruction, Integer.parseInt("7d008113", 16))
  poke(dut.io.writeBackResult.rd, 5)
  poke(dut.io.writeBackResult.rdData, 1250)
  poke(dut.io.writeBackResult.toRegisterFile, 0)
  poke(dut.io.readyIn, 1)

  step(1)
//
//  poke(dut.io.fetchIssuePort.valid, 1)
//  poke(dut.io.fetchIssuePort.PC, 6)
//  poke(dut.io.fetchIssuePort.instruction, Integer.parseInt("7d008113", 16))
//  poke(dut.io.writeBackResult.rd, 1)
//  poke(dut.io.writeBackResult.rdData, 1000)
//  poke(dut.io.writeBackResult.toRegisterFile, 0)
//  poke(dut.io.pipeLineStalled, 1)
//
//  step(1)
//
//  poke(dut.io.fetchIssuePort.valid, 1)
//  poke(dut.io.fetchIssuePort.PC, 6)
//  poke(dut.io.fetchIssuePort.instruction, Integer.parseInt("7d008113", 16))
//  poke(dut.io.writeBackResult.rd, 1)
//  poke(dut.io.writeBackResult.rdData, 1000)
//  poke(dut.io.writeBackResult.toRegisterFile, 0)
//  poke(dut.io.pipeLineStalled, 0)
//
//  step(1)
//
//  poke(dut.io.fetchIssuePort.valid, 1)
//  poke(dut.io.fetchIssuePort.PC, 7)
//  poke(dut.io.fetchIssuePort.instruction, Integer.parseInt("7d008113", 16))
//  poke(dut.io.writeBackResult.rd, 1)
//  poke(dut.io.writeBackResult.rdData, 1000)
//  poke(dut.io.writeBackResult.toRegisterFile, 0)
//  poke(dut.io.pipeLineStalled, 0)

  step(1)
}

class decode_test_waveform extends FlatSpec with Matchers {
  "Waveform" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
        new DECODE_ISSUE_UNIT()) { c =>
      new TestDecodeWaveform(c)
    } should be (true)
  }
}