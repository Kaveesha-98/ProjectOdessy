package pipeline.memory

import chisel3._
import chisel3.util._

import scala.math._

/* class aluIssuePort extends DecoupledIO {
    val instruction = Input(UInt(32.W))
} */

class handshake[T <: Data](gen: T) extends Bundle {
    val ready = Output(Bool())
    val valid = Input(Bool())
    val bits = Input(gen)
}

class AluIssuePort extends Bundle {
    val instruction = UInt(32.W)
    val nextInstPtr = UInt(64.W)
    val aluResult = UInt(64.W)
    val rs2 = UInt(64.W)
}

class MemoryIssuePort extends Bundle {
    val instruction = UInt(32.W)
    val nextInstPtr = UInt(64.W)
    val aluResult = UInt(64.W)
}

class channel_a(
    val z : Int = 2,
    val o : Int = 1,
    val a : Int = 64,
    val w : Int = 8,
) extends Bundle {
    val opcode      = Output(UInt(3.W))
    val param       = Output(UInt(3.W))
    val size        = Output(UInt(z.W))
    val source      = Output(UInt(o.W))
    val address     = Output(UInt(a.W))
    val mask        = Output(UInt(w.W))
    val data        = Output(UInt((8*w).W))
    val valid       = Output(UInt(1.W))
    val ready       = Input(UInt(1.W))
}

class channel_d(
    val z : Int = 2,
    val o : Int = 1,
    val i : Int = 1,
    val w : Int = 8,
) extends Bundle {
    val opcode  = Input(UInt(3.W))
    val param   = Input(UInt(2.W))
    val size    = Input(UInt(z.W))
    val source  = Input(UInt(o.W))
    val sink    = Input(UInt(i.W))
    val data    = Input(UInt((8*w).W))
    val error   = Input(UInt(1.W))
    val valid   = Input(UInt(1.W))
    val ready   = Output(UInt(1.W))
}

class MemoryUnit extends Module {
    val io = IO(new Bundle(){
        val aluIssuePort = new handshake(new AluIssuePort())
        val memoryIssuePort = Flipped(new handshake(new MemoryIssuePort()))
        val memPort = new Bundle{
            val a = new channel_a()
            val d = new channel_d()
        }
    })

    /* Might need to implement if we need to support misaligned accesses
    def getNextMemAccess(address: UInt, byteSize: UInt, op: UInt) = {
        val result = Wire(new Bundle(){
            val accessSize = UInt(2.W)
            val accessAddress = UInt(64.W)
            val mask = UInt(8.W)
        })
        val addressAlignment = Muxcase(0.U, Array.tabulate(3)(i => (!(address(2-i, 0)).orR) -> (3-i).U))
        val byteSizeAligment = Muxcase(3.U, Array.tabulate(3)(i => byteSize <= pow(2, i).U))
        when (op.asBool) {
            //for writes
            Wire(new Bundle(){})
        }
    } */

    val pass_through :: wait_mem_req :: wait_mem_resp :: wait_writeback :: Nil = Enum(4)

    val state_reg = RegInit(pass_through)

    //Output is driven by a register
    val resultsBuffer = Reg(new MemoryIssuePort())
    io.memoryIssuePort.bits := resultsBuffer

    val memoryIssuePort_valid = RegInit(false.B)
    io.memoryIssuePort.valid := memoryIssuePort_valid

    //initalizing buffer for mem requests
    val memReqBuffer = RegInit({
        val initial_state = Wire(new Bundle(){
            val opcode      = UInt(3.W)
            val param       = UInt(3.W)
            val size        = UInt(2.W)
            val source      = UInt(1.W)
            val address     = UInt(64.W)
            val mask        = UInt(8.W)
            val data        = UInt(64.W)
            val valid       = UInt(1.W)
        })

        initial_state.opcode := 0.U
        initial_state.param := 0.U
        initial_state.size := 0.U
        initial_state.source := 0.U
        initial_state.address := 0.U
        initial_state.mask := 0.U
        initial_state.data := 0.U
        initial_state.valid := 0.U

        initial_state
    })
    io.memPort.a.opcode := memReqBuffer.opcode
    io.memPort.a.param := memReqBuffer.param
    io.memPort.a.size := memReqBuffer.size
    io.memPort.a.source := memReqBuffer.source
    io.memPort.a.address := memReqBuffer.address
    io.memPort.a.mask := memReqBuffer.mask
    io.memPort.a.data := memReqBuffer.data
    io.memPort.a.valid := memReqBuffer.valid

    //for unaligned accesses there needs to be 2 accesses, hence needs to remember instruction
    val memReqInstrBuffer = Reg(new Bundle(){
        val opcode = UInt(1.W)
        val data = UInt(64.W)
        val mask = UInt(8.W)
        val size = UInt(3.W)
    })

    val accessSize = io.aluIssuePort.bits.instruction(13, 12)
    val mask = (MuxLookup(accessSize, 63.U, Seq(0.U -> 1.U, 1.U -> 3.U, 2.U -> 15.U)))

    val bufferedAluResult = Reg(new AluIssuePort())
    io.memPort.d.ready := (state_reg === wait_mem_resp).asUInt

    val fetchedTempWire = WireDefault({
        val result = Wire(new MemoryIssuePort())

        result.instruction := bufferedAluResult.instruction
        result.nextInstPtr := bufferedAluResult.nextInstPtr
        result.aluResult := {
            // right justifing read data
            val dData = io.memPort.d.data >> bufferedAluResult.aluResult(2, 0)
            val size = bufferedAluResult.instruction(13, 12)
            val sign = MuxLookup(size, dData(63), Seq.tabulate(3)(i => (i.U , dData(8*pow(2, i).toInt - 1))))
            val extn = Mux(sign === 1.U && bufferedAluResult.instruction(14) === 0.U, "hff".U(8.W), "h00".U(8.W))
            // sign extending when necessary
            MuxLookup(size, dData, Seq.tabulate(3)(i => (i.U , Cat(Fill(6 - pow(2, i).toInt, extn), dData(8*pow(2, i).toInt - 1, 0)))))
        }

        result
    })

    switch(state_reg){
        is(pass_through){
            when(io.memoryIssuePort.ready){
                resultsBuffer.instruction := io.aluIssuePort.bits.instruction
                resultsBuffer.nextInstPtr := io.aluIssuePort.bits.nextInstPtr
                resultsBuffer.aluResult := io.aluIssuePort.bits.aluResult
                memoryIssuePort_valid := io.aluIssuePort.valid
                when(io.aluIssuePort.valid && BitPat("b0?00011") === io.aluIssuePort.bits.instruction(6,0)){
                    //execute mem access
                    // Does not support misaligned accesses
                    state_reg := wait_mem_req
                    memoryIssuePort_valid := false.B
                    memReqBuffer.valid := 1.U
                    memReqBuffer.opcode := Mux(io.aluIssuePort.bits.instruction(5).asBool, 1.U, 4.U)
                    memReqBuffer.size := accessSize
                    memReqBuffer.address := io.aluIssuePort.bits.aluResult
                    memReqBuffer.mask := mask << io.aluIssuePort.bits.aluResult(2, 0)
                    memReqBuffer.data := io.aluIssuePort.bits.rs2
                    bufferedAluResult := io.aluIssuePort.bits // to shape realign data once arrived
                }
            }.otherwise {
                bufferedAluResult := io.aluIssuePort.bits
                state_reg := wait_writeback
            }
        }
        is (wait_mem_req) {
            when(io.memPort.a.ready.asBool) {
                memReqBuffer.valid := 0.U
                state_reg := wait_mem_resp
            }
        }
        is (wait_mem_resp) {
            when(io.memPort.d.valid.asBool) {
                
                when(io.memoryIssuePort.ready) {
                    resultsBuffer:= fetchedTempWire
                    state_reg := pass_through
                }.otherwise {
                    bufferedAluResult.aluResult := fetchedTempWire.aluResult
                    state_reg := wait_writeback
                }
            }
        }
        is(wait_writeback) {
            when(io.memoryIssuePort.ready) {
                resultsBuffer.instruction := bufferedAluResult.instruction
                resultsBuffer.nextInstPtr := bufferedAluResult.nextInstPtr
                resultsBuffer.aluResult := bufferedAluResult.aluResult
                memoryIssuePort_valid := true.B
                state_reg := pass_through
            }
        }
    }

    io.aluIssuePort.ready := state_reg === pass_through

    /* io.memoryIssuePort.valid := false.B
    io.memoryIssuePort.bits.instruction := 0.U
    io.memoryIssuePort.bits.nextInstPtr := 0.U
    io.memoryIssuePort.bits.aluResult := 0.U */

    /* io.memPort.a.opcode      := 0.U
    io.memPort.a.param       := 0.U
    io.memPort.a.size        := 0.U
    io.memPort.a.source      := 0.U
    io.memPort.a.address     := 0.U
    io.memPort.a.mask        := 0.U
    io.memPort.a.data        := 0.U
    io.memPort.a.valid       := false.B */

    io.memPort.d.ready := state_reg === wait_mem_resp
}

object ALU extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new MemoryUnit())
}