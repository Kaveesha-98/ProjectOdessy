
package pipeline

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._

class hart extends Module{
    val fetch = Module(new FetchUnit(16, 2))
    val instPort = IO(fetch.io.cloneType)
    fetch.io <> instPort
    /* val decode = Module(new DECODE_ISSUE_UNIT())
    val memory_access = Module(new MemoryUnit())

    val io = IO(new Bundle(){

        //reqport signals
        val reqport_valid = Output(UInt(1.W))
        val reqport_ready = Input(UInt(1.W))
        val reqport_addr = Output(UInt(64.W))

        //resport signals
        val resport_valid = Input(UInt(1.W))
        val resport_ready = Output(UInt(1.W))
        val resport_instr = Input(UInt(32.W))

    })

    val dataMemPort = IO(memory_access.io.memPort.cloneType())
    dataMemPort <> memory_access.io.memPort

    io.reqport_valid <> fetch.io.reqport_valid
    io.reqport_ready <> fetch.io.reqport_ready
    io.reqport_addr <> fetch.io.reqport_addr

    io.resport_valid <> fetch.io.resport_valid
    io.resport_ready <> fetch.io.resport_ready
    io.resport_instr <> fetch.io.resport_instr

     */

}

object hart extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new hart())
}