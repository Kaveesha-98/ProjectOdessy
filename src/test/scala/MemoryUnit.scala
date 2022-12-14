import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._

import chiseltest.simulator.WriteVcdAnnotation

import pipeline.memory._

class GCDSpec extends AnyFreeSpec with ChiselScalatestTester {

  "testing memory unit in pipeline" in {
    //test(new MemoryUnit) { dut =>
    test(new MemoryUnit).withAnnotations(Seq(WriteVcdAnnotation))  { dut =>
        dut.reset
        dut.clock.step(1)

        dut.io.aluIssuePort.bits.poke(dut.assertInstr())
        dut.io.aluIssuePort.valid.poke(true.B)
        dut.io.memPort.a.valid.expect(false.B)
        dut.clock.step(4)
        dut.io.memoryIssuePort.ready.poke(true.B)
        dut.clock.step(4)
        dut.io.aluIssuePort.ready.expect(false.B)
        dut.io.memPort.a.valid.expect(true.B)

        dut.io.memPort.a.ready.poke(1.U)
        dut.clock.step(1)
        //once mem requested is accpted no more memory
        //requests should be issued until a mem accesses
        //is accepted from aluIssuePort
        dut.io.memPort.a.valid.expect(0.U)
        dut.io.memPort.d.ready.expect(1.U)
        //no instruction should be issued from memIssuePort
        dut.io.memoryIssuePort.valid.expect(false.B)

        

      }
    }
}
