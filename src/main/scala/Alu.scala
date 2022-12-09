import chisel3._
import chisel3.util._

class DecodeIssuePort extends Bundle {
  val valid       = UInt(1.W)
  val PC          = UInt(64.W)
  val instruction = UInt(32.W)
  val rs1         = UInt(64.W)
  val rs2         = UInt(64.W)
  val immediate   = UInt(64.W)
}

class AluIssuePort extends Bundle {
  val valid       = UInt(1.W)
  val aluResult   = UInt(64.W)
  val instruction = UInt(32.W)
  val nextInstPtr = UInt(64.W)
  val rs2         = UInt(64.W)
}

class UnitStatus extends Bundle {
  val stalled    = UInt(1.W)
  val empty      = UInt(1.W)
}

class BranchResult extends Bundle {
  val valid     = UInt(1.W)
  val target    = UInt(64.W)
}

class Alu extends Module{
  val io = IO(new Bundle(){
    val decodeIssuePort = Input(new DecodeIssuePort)
    val aluIssuePort    = Output(new AluIssuePort)
    val unitStatus      = Output(new UnitStatus)
    val branchResult    = Output(new BranchResult)
    val pipelineStalled = Input(UInt(1.W))
    //val interrupted     = Input(UInt(1.W)) 
  })

  val pcReg    = RegInit(0.U(64.W))
  val insReg   = RegInit(0.U(32.W))
  val immReg   = RegInit(0.U(64.W))
  val rs1Reg   = RegInit(0.U(64.W))
  val rs2Reg   = RegInit(0.U(64.W))

  val insType = WireDefault(0.U)

  pcReg  := io.decodeIssuePort.PC
  insReg := io.decodeIssuePort.instruction

}

