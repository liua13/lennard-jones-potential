package potential

import chisel3._
import chisel3.util._

class LUTInputBundle(val dim2: Int, val dataWidth: Int) extends Bundle {
    val index = Input(UInt(log2Up(dim2).W))
    val dataIn = Input(UInt(dataWidth.W))
    val write = Input(Bool()) // read is false
}

class LUTOutputBundle(val dim2: Int, val dataWidth: Int) extends Bundle {
    val index = Input(UInt(log2Up(dim2).W))
    val dataIn = Input(UInt(dataWidth.W))
    val write = Input(Bool()) // read is false

    val dataOut = Output(UInt(dataWidth.W))
}

// class LUTDecoupled(val dim2: Int, val dataWidth: Int) extends Module {
//     val input = IO(Flipped(Decoupled(new LUTInputBundle(dim2, dataWidth))))
//     val output = IO(Decoupled(new LUTOutputBundle(dim2, dataWidth)))
//     val table = Mem(dim2, UInt(dataWidth.W))
    
//     val index = Reg(UInt())
//     val dataIn = Reg(UInt())
//     val write = Reg(Bool())
//     val dataOut = Reg(UInt(dataWidth.W))
//     val resultValid = RegInit(false.B)

//     input.ready := output.ready
//     output.valid := resultValid && output.ready

//     output.bits.index := index
//     output.bits.dataIn := dataIn
//     output.bits.write := write 
//     output.bits.dataOut := dataOut

//     when(input.valid) {
//         val inputBundle = input.deq()
//         index := inputBundle.index
//         dataIn := inputBundle.dataIn
//         write := inputBundle.write 

//         when(write) {
//             table(index) := dataIn
//             dataOut := dataIn
//         }.otherwise {
//             dataOut := table(index)
//         }
//         resultValid := true.B
//     }.otherwise {
//         output.bits.dataIn := 28.U
//     }
// }

class LUTDecoupled(val dim2: Int, val dataWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new LUTInputBundle(dim2, dataWidth))))
    val output = IO(Decoupled(new LUTOutputBundle(dim2, dataWidth)))
    val table = Mem(dim2, UInt(dataWidth.W))
    
    val index = Reg(UInt())
    val dataIn = Reg(UInt())
    val write = Reg(Bool())
    val busy = RegInit(false.B)

    input.ready := ! busy
    output.valid := false.B
    output.bits := DontCare

    when(busy) {
        when(write) {
            table(index) := dataIn
            output.bits.dataOut := dataIn
        }.otherwise {
            output.bits.dataOut := table(index)
        }
        output.bits.index := index
        output.bits.dataIn := dataIn
        output.bits.write := write 
        output.valid := true.B
        busy := ! output.ready
    }.elsewhen(input.valid) {
        val inputBundle = input.deq()
        index := inputBundle.index
        dataIn := inputBundle.dataIn
        write := inputBundle.write 
        busy := true.B
        output.valid := false.B
    }
}

// class LUTDecoupled(val dim2: Int, val dataWidth: Int) extends Module {
//     val input = IO(Flipped(Decoupled(new LUTInputBundle(dim2, dataWidth))))
//     val output = IO(Decoupled(new LUTOutputBundle(dim2, dataWidth))) 

//     val table = Mem(dim2, UInt(dataWidth.W))
    
//     input.ready := true.B
//     output.valid := input.valid
//     output.bits := DontCare

//     when(input.valid) {
//         val inputBundle = input.deq()
//         when(inputBundle.write) {
//             table(inputBundle.index) := inputBundle.dataIn
//         }.otherwise {
//             output.bits.dataOut := table(inputBundle.index)
//         }
//         output.bits.index := inputBundle.index
//         output.bits.dataIn := inputBundle.dataIn
//         output.bits.write := inputBundle.write 
//     }
// }

// can also queue