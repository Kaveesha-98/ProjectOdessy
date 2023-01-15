import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class FetchUnitTester extends AnyFreeSpec with ChiselScalatestTester {
  "Testing FetchUnit" in {
    test(new FetchUnit(0,32)) { c =>
      c.io.reqport.ready.poke(1.U)
      c.io.resport.valid.poke(0.U)
      c.io.issueport.ready.poke(1.U)
      c.io.execport.valid.poke(0.U)
      c.io.execport.branch_taken.poke(0.U)
      c.io.execport.predicted.poke(1.U)
      c.io.execport.PC.poke(1.U)
      c.io.execport.branch_address.poke(1.U)
      c.io.execport.is_branch.poke(1.U)
      c.io.resport.bits.poke(0.U)
      c.clock.step(1)
      c.clock.step(1)

      for (i <- 0 until 10) {
        c.clock.step(1)
      }
      for (i <- 0 until 10){
        c.io.resport.valid.poke(1.U)
        c.io.resport.bits.poke(i.U)
        c.clock.step(1)
      }
      //pipeline stall
      for (i <- 10 until 20) {
        c.io.resport.valid.poke(1.U)
        c.io.resport.bits.poke(10.U)
        c.io.issueport.ready.poke(0.U)
        c.clock.step(1)
      }
      //resume
      for (i <- 10 until 20) {
        c.io.resport.valid.poke(1.U)
        c.io.resport.bits.poke(i.U)
        c.io.issueport.ready.poke(1.U)
        c.clock.step(1)
      }

      c.io.execport.valid.poke(1.U)
      c.io.execport.branch_taken.poke(0.U)
      c.io.execport.predicted.poke(0.U)
      c.io.execport.PC.poke(1.U)
      c.io.execport.branch_address.poke(200.U)
      c.io.execport.is_branch.poke(1.U)
      c.io.resport.bits.poke(0.U)
      c.clock.step(1)

      for (i <- 20 until 44) {
        c.io.execport.valid.poke(0.U)
        c.io.resport.valid.poke(1.U)
        c.io.resport.bits.poke(i.U)
        c.io.issueport.ready.poke(1.U)
        c.clock.step(1)
      }

      for (i <- 200 until 210) {
        c.io.resport.valid.poke(1.U)
        c.io.resport.bits.poke(i.U)
        c.io.issueport.ready.poke(1.U)
        c.clock.step(1)
      }

    }
  }
}
