package pipeline

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import common._
import java.nio.file.{Files, Paths}

class singleCycleTestBench extends Module {
    val byteArray = Files.readAllBytes(Paths.get("src/test/scala/entry.text"))
    val programMemory = VecInit(byteArray.map(i => (i&255).U(8.W)))

    val testHart = Module(new singleCycleHart)

    val io = IO(new Bundle(){
        val ecall = Output(Bool())
        val pc = Output(UInt(64.W))
    })

    io.ecall := Cat(Seq.tabulate(4)(i => programMemory(testHart.io.reqport_addr - "h80000000".U + 3.U - i.U))) === BitPat("b00000000000000000000000001110011")
    io.pc := testHart.io.reqport_addr

    testHart.io.reqport_ready := 1.U
    testHart.io.resport_valid := 1.U
    testHart.io.resport_instr := Cat(Seq.tabulate(4)(i => programMemory(testHart.io.reqport_addr - "h80000000".U + 3.U - i.U)))

    testHart.dataPort.d := testHart.dataPort.d.init()

    testHart.dataPort.a.ready := 1.U
}
object singleCycleTestBench extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new singleCycleTestBench())
}