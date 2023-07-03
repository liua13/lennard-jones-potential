// package potential

// import chisel3._
// import chisel3.util._
// import hardfloat._
// import potential.Arithmetic.FloatArithmetic._

// case class CBInputBundle(dim: Int, dataWidth: Int, expWidth: Int, sigWidth: Int) extends Bundle {
//     val molecule1 = new MoleculeBundle(dataWidth, expWidth, sigWidth)
//     val molecule2 = new MoleculeBundle(dataWidth, expWidth, sigWidth)
// }

// case class CBOutputBundle(dim: Int, dataWidth: Int, expWidth: Int, sigWidth: Int) extends Bundle {
//     val data = Float(expWidth, sigWidth)
//     val error = Bool()
// }

// class CalculateBuffer(dim: Int, dataWidth: Int, expWidth: Int, sigWidth: Int) extends Module {
//     val input = IO(Flipped(Decoupled(new CBInputBundle(dim, dataWidth, expWidth, sigWidth))))
//     val output = IO(Decoupled(new CBOutputBundle(dim, dataWidth, expWidth, sigWidth)))
    
//     val calculateForce = Module(new CalculateForce(dim, dataWidth, expWidth, sigWidth))

//     val q = Queue(input)
//     q.nodeq() 

//     when(q.valid && calculateForce.input.ready) {
//         val qVal = q.deq()
//         calculateForce.input.bits.molecule1 := qVal.molecule1
//         calculateForce.input.bits.molecule2 := qVal.molecule2
//         calculateForce.input.valid := true.B
//     }.otherwise {
//         calculateForce.input.bits.molecule1 := calculateForce.input.bits.molecule1
//         calculateForce.input.bits.molecule2 := calculateForce.input.bits.molecule2
//         calculateForce.input.valid := false.B
//     }
// }

// // todo: change tests in CalculateForce to compare bits instead of error range OR using relative range: (x - y) / max(x, y)
// // todo: write tests for this module 