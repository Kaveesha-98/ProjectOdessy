import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._

import chiseltest.simulator.WriteVcdAnnotation

import java.nio.file.{Files, Paths}

import pipeline._

class singleCycleTest extends AnyFreeSpec with ChiselScalatestTester {

  "testing memory unit in pipeline" in {
    //test(new MemoryUnit) { dut =>
    test(new singleCycleTestBench).withAnnotations(Seq(WriteVcdAnnotation))  { dut =>



        dut.reset
        while(dut.io.ecall.peek().litValue == 0) {
            dut.clock.step(1)
            println(dut.io.pc.peek().litValue.toString)
        }
      }
    }
}

