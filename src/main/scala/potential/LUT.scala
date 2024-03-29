package potential

import chisel3._
import chisel3.util._

case class LUTWriteIO(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val validIn = Input(Bool())
    val addr = Input(UInt(dim.W))
    val data = Input(Float(expWidth, sigWidth))
    val tableReady = Input(Bool())
}

case class LUTReadIO(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val addr = Input(UInt(dim.W))
    val data = Output(Float(expWidth, sigWidth))
    val tableReady = Output(Bool())
}

// LUT should be a linearized square table of size dim x dim, 
// NOTE: dim must be a power of 2
// use cases: sigma^6 and epsilon tables
class LUT(val dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val writeIO = IO(new LUTWriteIO(dim, expWidth, sigWidth))
    val readIO = IO(new LUTReadIO(dim, expWidth, sigWidth))

    val table = Mem(dim * dim, Float(expWidth, sigWidth))
    val tableReady = Reg(Bool())

    when(writeIO.tableReady) {
        tableReady := writeIO.tableReady
    }.otherwise {
        tableReady := tableReady
    }

    when(writeIO.validIn) {
        table(writeIO.addr) := writeIO.data
    }

    readIO.data := table(readIO.addr)
    readIO.tableReady := tableReady
}