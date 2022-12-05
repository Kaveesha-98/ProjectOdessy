import chisel3._
import chisel3.iotesters._

class PracticeTest(dut:Practice) extends PeekPokeTester(dut){
  poke(dut.io.a,0.U)
  poke(dut.io.b,1.U)
  step(1)
  expect(dut.io.out,0)
  println("Result is : "+peek(dut.io.out).toString)
  poke(dut.io.a,3.U)
  poke(dut.io.b,2.U)
  step(1)
  expect(dut.io.out,2)
  println("Result is : "+peek(dut.io.out).toString)
}

object PracticeTest extends App{
  chisel3.iotesters.Driver(() => new Practice ()){c =>
  new PracticeTest(c)
  }
}