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

        dut.io.aluIssuePort.bits.poke({
            (new AluIssuePort()).Lit(
            _.instruction -> 0.U, 
            _.nextInstPtr -> 1.U, 
            _.aluResult -> 2.U, 
            _.rs2 -> 3.U)
        })
        dut.clock.step(1)

      }
    }
}
