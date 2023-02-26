import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import chisel3.stage.PrintFullStackTraceAnnotation

import pipeline._

class BasicTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "MyModule"
  // test class body here
  it should "do something" in {
    test(new axi32_cpu_wrapper).withAnnotations(Seq(PrintFullStackTraceAnnotation)) { c =>
      c.reset
    }
  }
}
