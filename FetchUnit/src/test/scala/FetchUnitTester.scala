import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class FetchUnitTester extends AnyFreeSpec with ChiselScalatestTester {
  "Testing FetchUnit" in {
    test(new FetchUnit) { c =>
      c.io.reqport_ready.poke(1.U)
      c.io.resport_valid.poke(0.U)
      c.io.pipelinestalled.poke(0.U)
      c.io.target_input.poke(0.U)
      c.io.target_valid.poke(0.U)
      c.io.resport_instr.poke(0.U)
      c.clock.step(1)
      //for loop
      for (i <- 1 until 30) {
        c.io.reqport_ready.poke(1.U)
        c.io.resport_valid.poke(1.U)
        c.io.pipelinestalled.poke(0.U)
        c.io.target_input.poke(0.U)
        c.io.target_valid.poke(0.U)
        c.io.resport_instr.poke(i.U)
        c.clock.step(1)
      }

      for (i <- 30 until 40) {
        c.io.reqport_ready.poke(1.U)
        c.io.resport_valid.poke(1.U)
        c.io.pipelinestalled.poke(1.U)
        c.io.target_input.poke(0.U)
        c.io.target_valid.poke(0.U)
        c.io.resport_instr.poke(i.U)
        c.clock.step(1)
      }

      for (i <- 40 until 50) {
        c.io.reqport_ready.poke(1.U)
        c.io.resport_valid.poke(1.U)
        c.io.pipelinestalled.poke(0.U)
        c.io.target_input.poke(0.U)
        c.io.target_valid.poke(0.U)
        c.io.resport_instr.poke(i.U)
        c.clock.step(1)
      }

    }
  }
}
