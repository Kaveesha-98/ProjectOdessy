package pipeline.fetch

import chisel3._

class DecoupledIO[T <: Data ]( gen: T) extends Bundle {
  val ready = Input(Bool ())
  val valid = Output(Bool ())
  val bits = Output(gen)
}

class FifoIO[T <: Data ](private val gen: T) extends Bundle {
  val enq = Flipped(new DecoupledIO(gen))
  val deq = new DecoupledIO (gen)
}
class Execport extends Bundle{
  val valid = Input(Bool())
  val is_branch = Input(Bool())
  val branch_taken = Input(Bool())
  val predicted = Input(Bool())
  val PC= Input(UInt(64.W))
  val branch_address= Input(UInt(64.W))
}
class Issueport extends Bundle{
  val valid = Output(Bool())
  val ins = Output(UInt(32.W))
  val prediction = Output(UInt(64.W))
  val PC= Output(UInt(64.W))
  val ready = Input(Bool())
}
