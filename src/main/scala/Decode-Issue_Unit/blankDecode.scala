package pipeline.decode

import Constants._

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class blankDecode extends Module {
    /**
      * This is a blank module. Only interfaces are defined. The outputs are driven with Literal values
      */
    val fetchIssueIntfce = IO(new Bundle{
        val ready       = Output(Bool())
        val issued      = Input(Bool())
        val PC          = Input(UInt(64.W))
        val instruction = Input(UInt(64.W))
        val expected    = new Bundle{
            val valid       = Output(Bool())
            val PC          = Output(UInt(64.W))
        }
    })

    val decodeIssuePort = IO(Flipped(new handshake(new DecodeIssuePort)))
    val writeBackResult = IO(Input(new WriteBackResult))

    // These are place holder values for an actual implementation
    fetchIssueIntfce.ready              := false.B
    fetchIssueIntfce.expected.valid     := false.B
    fetchIssueIntfce.expected.PC        := 0.U

    decodeIssuePort.valid := 0.U
    decodeIssuePort.bits := (new DecodeIssuePort).Lit(
        _.instruction -> 0.U,
        _.PC          -> 0.U,
        _.rs1         -> 0.U,
        _.rs2         -> 0.U,
        _.immediate   -> 0.U
    )
}

object blankDecode extends  App {
    emitVerilog(new blankDecode)
}

class decodeHWTestbench extends Module {
    /**
      * This module is meant to test decode units. Since the 
      * decode unit contains the architectural state as well
      * some instructions are implmented to simulate cpu.
      * 
      * i.e. When an add instruction is passed through the decode
      * internal state of the decode changes(The destination register
      * will not be available for a proceeding instruction). Hence, the 
      * add instruction that is issued from the decode unit must be 
      * implemented in order for proper operation of decode unit for futher
      * testing.
      *  
      * Memory access operations are not implmented as they are too 
      * complicated to implement.
      */

    /**
      * A continous stream of instructions will be issued through
      * the below interface. Not exactly in the program order.
      */
    val fetchIssueIntfce = IO(new Bundle{
        val ready       = Input(Bool())
        val issued      = Output(Bool())
        val PC          = Input(UInt(64.W))
        val instruction = Input(UInt(64.W))
        val expected    = new Bundle{
            val valid       = Output(Bool())
            val PC          = Output(UInt(64.W))
        }
    })

    val dutDecode = Module(new blankDecode)
    /* connecting the decode to instruction stream */
    fetchIssueIntfce.PC             <> dutDecode.fetchIssueIntfce.PC
    fetchIssueIntfce.instruction    <> dutDecode.fetchIssueIntfce.instruction
    fetchIssueIntfce.expected       <> dutDecode.fetchIssueIntfce.expected

    /** applying condition for firing rule:
      * Two interfaces have to be ready and the PC presented by fetch must match the expected PC
      * from the decode(if it is valid).
      */
    val conditional_fetch_issue = Seq(fetchIssueIntfce, dutDecode.fetchIssueIntfce)
    conditional_fetch_issue.foreach(_.issued := (
        fetchIssueIntfce.ready && dutDecode.fetchIssueIntfce.ready &&
        (!dutDecode.fetchIssueIntfce.expected.valid || (dutDecode.fetchIssueIntfce.expected.PC === fetchIssueIntfce.expected.PC))
    ))

    // For testing purposes this interface must be visible for outside
    val decodeIssuePort = IO(Flipped(dutDecode.decodeIssuePort.cloneType))
    decodeIssuePort <> dutDecode.decodeIssuePort
    // assumes pipeline is never blocked
    dutDecode.decodeIssuePort.ready := 1.U

    // ----------------------- below code(implementing instructions issued from decode) needs to be completed
    val resultsWriteBack = RegInit((new WriteBackResult).Lit(
        _.toRegisterFile -> 0.U,
        _.rd             -> 0.U,
        _.rdData         -> 0.U
    ))
    dutDecode.writeBackResult <> resultsWriteBack
}

object decodeHWTestbench extends App {
    emitVerilog(new decodeHWTestbench)
}