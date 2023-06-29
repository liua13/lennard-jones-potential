package potential

import chisel3._
import chisel3.util._

case class LUTInputBundle(val dim2: Int, val dataWidth: Int) extends Bundle {
    val index = UInt(log2Up(dim2).W)
    val write = Bool() // true is write, false is read
    val dataIn = UInt(dataWidth.W)
}

case class LUTOutputBundle(val dim2: Int, val dataWidth: Int) extends Bundle {
    val dataOut = UInt(dataWidth.W)
}

// LUT should be a linearized square table of size dim x dim, 
// where dim is a power of 2
// dim2 is dim^2
// use cases: sigma^6 and epsilon tables
class LUT(val dim2: Int, val dataWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new LUTInputBundle(dim2, dataWidth))))
    val output = IO(Decoupled(new LUTOutputBundle(dim2, dataWidth)))

    val table = Mem(dim2, UInt(dataWidth.W))
    
    input.ready := output.ready || !output.ready
    output.valid := input.valid
    
    when(input.bits.write){
        output.bits.dataOut := input.bits.dataIn
    }.otherwise {
        output.bits.dataOut := table(input.bits.index)
    }

    when(input.valid && input.bits.write) {
        table(input.bits.index) := input.bits.dataIn
    }
}

// class LUT(val dim2: Int, val dataWidth: Int) extends Module {
//     val io = IO(new Bundle {
//         val validIn = Input(Bool())
//         val index = Input(UInt(log2Up(dim2).W))
//         val dataIn = Input(UInt(dataWidth.W))
//         val write = Input(Bool()) // write true, read false

//         val validOut = Output(Bool())
//         val dataOut = Output(UInt(dataWidth.W))
//     })

//     val table = Mem(dim2, UInt(dataWidth.W))
    
//     val validOut = RegInit(false.B)
//     val dataOut = Reg(UInt())

//     // io.validOut := io.validIn
//     // io.dataOut := DontCare
//     io.validOut := validOut
//     io.dataOut := dataOut

//     when(io.validIn) {
//         when(io.write) {
//             table(io.index) := io.dataIn
//         }.otherwise {
//             // io.dataOut := table(io.index)
//             dataOut := table(io.index)
//             validOut := true.B
//         }
//     }
// }