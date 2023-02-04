package pipeline.decode

import Constants._

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import pipeline.ports

abstract class decodeTemplate extends Module {
    val fetchIssuePort  = IO(new handshake(new FetchIssuePort))
    val decodeIssuePort = IO(Flipped(new handshake(new DecodeIssuePort)))
    val writeBackResult = IO(Input(new WriteBackResult))

    // update register file only using write back result 
    def updateRegisterFile: Unit

    val registerFile = VecInit.tabulate(32)(i => { i match { 
        case 0 => 0.U
        case _ => Reg(UInt(64.W))        
    }
    })

    val validBits = VecInit.tabulate(32)(i => {i match {
        case 0 => true.B
        case _ => RegInit(true.B)    }
    })

    val fetchBuffer = RegInit(new Bundle{
        val valid   = Bool()
        val bits    = (new FetchIssuePort)
    }.Lit(
        _.valid -> false.B,
        _.bits.PC -> 0.U,
        _.bits.instruction -> 0.U
    ))

    val decodeBuffer = RegInit(new Bundle{
        val valid   = Bool()
        val bits    = (new DecodeIssuePort)
    }.Lit(
        _.valid             -> false.B,
        _.bits.instruction  -> 0.U,
        _.bits.PC           -> 0.U,
        _.bits.opCode       -> 0.U,
        _.bits.rs1          -> 0.U,
        _.bits.rs2          -> 0.U,
        _.bits.immediate    -> 0.U
    ))

    fetchIssuePort.valid := ~fetchBuffer.valid.asUInt // until buffered instruction in processed no new instructions are taken

    // decodeIssuePort.ready means that the presented instruction is accepted and a new one can be issued to decodeBuffer
    when(!fetchBuffer.valid && fetchIssuePort.valid.asBool && decodeBuffer.valid && !decodeIssuePort.ready) {
        // decode is waiting for one entry to accepted by ALU. Hence need to buffer the new one
        fetchBuffer.bits := fetchIssuePort.bits
        fetchBuffer.valid := true.B
    }.elsewhen(fetchBuffer.valid && (!decodeBuffer.valid || decodeIssuePort.ready.asBool)) {
        // old instruction was accepted and the buffered one is processed
        fetchBuffer.valid := false.B
    }

    // buffered instruction is always processed first
    val processingEntry: FetchIssuePort

    // check for true(RAW) dependecy and false(WAW) dependency
    val hasDependency: Bool

    /**
      * entryValid is used to validate changes to validBits. Changes are made by the buffered instruction
      * or the new instruction being accepted in current cycle. In order to do changes decodeBuffer must be empty 
      * or the current entry is accepted by alu in the current cycle. and has no instruction dependency
      */
    val entryValid = (!decodeBuffer.valid || decodeIssuePort.ready.asBool) &&
        (fetchBuffer.valid || (fetchIssuePort.ready.asBool && fetchIssuePort.valid.asBool)) && 
        !hasDependency

    def instructionHasRd(instruction: UInt): Bool

    // updating valid bits
    for (i <- 1 to 31) {
        when (validBits(i)) {
            validBits(i) := (writeBackResult.rd === i.U && writeBackResult.toRegisterFile.asBool)
        }.otherwise {
            // invaliding register entry until value is calculated
            validBits(i) := !(instructionHasRd(processingEntry.instruction) && entryValid)
        }
    }

    def getdecodedResults(fetchInputs: FetchIssuePort): DecodeIssuePort

    when(entryValid) {
        decodeBuffer.bits := getdecodedResults(processingEntry)
        decodeBuffer.valid := true.B
    }.elsewhen(decodeIssuePort.ready.asBool) {
        // no entry available and presented instruction is accepted by alu
        decodeBuffer.valid := false.B
    }

    decodeIssuePort.bits := decodeBuffer.bits
    decodeIssuePort.valid := decodeBuffer.valid.asUInt
}
