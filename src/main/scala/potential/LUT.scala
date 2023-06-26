package potential

import chisel3._
import chisel3.util._

// LUT should be a linearized square table of size dim x dim, 
// where dim is a power of 2
// dim2 is dim^2
// use cases: sigma^6 and epsilon tables
class LUT(val dim2: Int, val dataWidth: Int) extends Module {
    val io = IO(new Bundle {
        val validIn = Input(Bool())
        val index = Input(UInt(log2Up(dim2).W))
        val dataIn = Input(UInt(dataWidth.W))
        val write = Input(Bool()) // write true, read false

        val validOut = Output(Bool())
        val dataOut = Output(UInt(dataWidth.W))
    })

    val table = Mem(dim2, UInt(dataWidth.W))
    
    val validOut = RegInit(false.B)
    val dataOut = Reg(UInt())

    // io.validOut := io.validIn
    // io.dataOut := DontCare
    io.validOut := validOut
    io.dataOut := dataOut

    when(io.validIn) {
        when(io.write) {
            table(io.index) := io.dataIn
        }.otherwise {
            // io.dataOut := table(io.index)
            dataOut := table(io.index)
            validOut := true.B
        }
    }
}