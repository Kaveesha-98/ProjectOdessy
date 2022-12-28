
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._

import chiseltest.simulator.WriteVcdAnnotation

import pipeline.fetch._

class fetchTest extends AnyFreeSpec with ChiselScalatestTester {

  "testing memory unit in pipeline" in {
    //test(new MemoryUnit) { dut =>
    test(new FetchUnit(0, 2)).withAnnotations(Seq(WriteVcdAnnotation))  { dut =>
        dut.reset
        dut.clock.step(1)
      }
    }
}