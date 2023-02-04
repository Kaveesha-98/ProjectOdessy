package pipeline.memory

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import common._

import scala.math._
class MemoryUnit extends Module {

    def assertInstr(instr: String = "add x0, x0, x0", pc: Int = 0) = {
        (new AluIssuePort()).Lit(
        _.instruction -> common.getreadInstr().U, 
        _.nextInstPtr -> 1.U, 
        _.aluResult -> 2.U, 
        _.rs2 -> 3.U)
    }

    val aluIssuePort = IO(new handshake(new AluIssuePort))
    val dataPort = IO(new Bundle(){
        val a = new channel_a()
        val d = new channel_d()
    })
    val memoryIssuePort = IO(Flipped(new handshake(new MemoryIssuePort)))

    val passThrough :: waitOnMemReq :: waitOnMemResp :: waitWriteBack :: Nil = Enum(4)
    val stateReg = RegInit(passThrough)

    val recieveBuffer = Reg(new AluIssuePort)
    when (stateReg === passThrough && aluIssuePort.valid && (
        (!memoryIssuePort.ready || aluIssuePort.bits.instruction(6, 0) === BitPat("b0?00011"))
    )) {
        recieveBuffer := aluIssuePort.bits
    } 

    val a_bits = RegInit(dataPort.a.init())
    dataPort.a := a_bits

    when (stateReg === passThrough && aluIssuePort.valid && aluIssuePort.bits.instruction(6, 0) === BitPat("b0?00011")) {
        /**
          * Setting registers to send request
          * Unaligned accesses are not supported in hardware
          */
        a_bits.opcode := Mux(aluIssuePort.bits.instruction(5).asBool, 1.U, 4.U)
        a_bits.param := 0.U
        a_bits.size := aluIssuePort.bits.instruction(13, 12)
        a_bits.source := 0.U
        a_bits.address := aluIssuePort.bits.aluResult
        val unalignedMask = MuxLookup(aluIssuePort.bits.instruction(13, 12), 255.U(8.W), Seq.tabulate(3)(i => (i.U -> ((1 << (1 << i))-1).U)))
        a_bits.mask := unalignedMask << aluIssuePort.bits.aluResult(2, 0)
        a_bits.data := MuxLookup(aluIssuePort.bits.aluResult(2, 0), aluIssuePort.bits.rs2, Seq.tabulate(8)(
            i => (i.U -> (aluIssuePort.bits.rs2 << (8*i)))))
        a_bits.valid := 1.U
    }

    when (stateReg === waitOnMemReq && dataPort.a.ready.asBool) {
        /**
          * Request has been accepted
          */
        a_bits.valid := 0.U
    }

    val memIssueBuffer = Reg(new MemoryIssuePort())
    val memIssueValid = RegInit(false.B)
    memoryIssuePort.bits := memIssueBuffer
    memoryIssuePort.valid := memIssueValid

    val justifiedLoadData = MuxLookup(recieveBuffer.aluResult(2, 0), dataPort.d.data, Seq.tabulate(8)(
        i => (i.U -> (dataPort.d.data >> (8*i)))))
    val signExtendedData = MuxLookup(recieveBuffer.instruction(13,12), justifiedLoadData, Seq(
        0.U -> Cat(Fill(56, justifiedLoadData(7)), justifiedLoadData(7, 0)),
        1.U -> Cat(Fill(48, justifiedLoadData(15)), justifiedLoadData(15, 0)),
        2.U -> Cat(Fill(32, justifiedLoadData(31)), justifiedLoadData(31, 0))
    ))
    val unsignExtendedData = MuxLookup(recieveBuffer.instruction(13,12), justifiedLoadData, Seq(
        0.U -> Cat(0.U(56.W), justifiedLoadData(7, 0)),
        1.U -> Cat(0.U(48.W), justifiedLoadData(15, 0)),
        2.U -> Cat(0.U(32.W), justifiedLoadData(31, 0))
    ))
    val dataWrtieBack = Mux(recieveBuffer.instruction(14).asBool, unsignExtendedData, signExtendedData)

    when (stateReg === passThrough) {
        when (memoryIssuePort.ready && aluIssuePort.bits.instruction =/= BitPat("b0?00011")) {
            memIssueBuffer.instruction := aluIssuePort.bits.instruction
            memIssueBuffer.nextInstPtr := aluIssuePort.bits.nextInstPtr
            memIssueBuffer.aluResult := aluIssuePort.bits.aluResult
            
            memIssueValid := aluIssuePort.valid
        }.elsewhen(memoryIssuePort.ready) {
            memIssueValid := false.B
        }
    }. elsewhen (stateReg === waitWriteBack) {
        when (memoryIssuePort.ready) {
            memIssueBuffer.instruction := recieveBuffer.instruction
            memIssueBuffer.nextInstPtr := recieveBuffer.nextInstPtr
            memIssueBuffer.aluResult := recieveBuffer.aluResult
            
            memIssueValid := true.B
        }
    }.elsewhen (stateReg === waitOnMemResp && dataPort.d.valid.asBool) {
        when (memoryIssuePort.ready) {
            memIssueBuffer.instruction := recieveBuffer.instruction
            memIssueBuffer.nextInstPtr := recieveBuffer.nextInstPtr
            memIssueBuffer.aluResult := dataWrtieBack
            memIssueValid := true.B
        }.otherwise {
            recieveBuffer.aluResult := dataWrtieBack
        }
    }.elsewhen (memoryIssuePort.ready) {
        /**
         * when memory instructions are executing, an instruction  may be issuing
         */
        memIssueValid := false.B
    }

    switch(stateReg) {
        is(passThrough) {
            when(aluIssuePort.valid && aluIssuePort.bits.instruction(6, 0) === BitPat("b0?00011")) {
                stateReg := waitOnMemReq
            }.elsewhen(aluIssuePort.valid && !memoryIssuePort.ready) {
                stateReg := waitWriteBack
            }
        }
        is(waitOnMemReq) {
            when(dataPort.a.ready.asBool) { stateReg := waitOnMemResp }
        }
        is(waitOnMemResp) {
            when(dataPort.d.valid.asBool) {
                stateReg := Mux(memoryIssuePort.ready, passThrough, waitWriteBack)
            } 
        }
        is(waitWriteBack) {
            when(memoryIssuePort.ready) { stateReg := passThrough }
        }
    }
    aluIssuePort.ready := stateReg === passThrough
    dataPort.d.ready := stateReg === waitOnMemResp
}

object MemoryUnit extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new MemoryUnit())
}