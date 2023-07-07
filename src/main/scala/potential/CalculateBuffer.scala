package potential

import chisel3._
import chisel3.util._
import hardfloat._
import potential.Arithmetic.FloatArithmetic._

class CalculateBuffer(val dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CFInputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CFOutputBundle(dim, expWidth, sigWidth)))
    
    val calculateForce = Module(new CalculateForce(dim, expWidth, sigWidth))
    calculateForce.output.ready := output.ready

    val sigma6WriteIO = IO(new LUTWriteIO(dim * dim, expWidth, sigWidth))
    calculateForce.sigma6WriteIO <> sigma6WriteIO

    val epsilonWriteIO = IO(new LUTWriteIO(dim * dim, expWidth, sigWidth))
    calculateForce.epsilonWriteIO <> epsilonWriteIO

    val m1 = Reg(new MoleculeBundle(dim, expWidth, sigWidth))
    val m2 = Reg(new MoleculeBundle(dim, expWidth, sigWidth))

    val q = Queue(input)
    q.nodeq() 

    calculateForce.input.valid := false.B

    when(q.valid && calculateForce.input.ready) {
        val qVal = q.deq()
        calculateForce.input.bits.molecule1 := qVal.molecule1
        calculateForce.input.bits.molecule2 := qVal.molecule2
        calculateForce.input.valid := true.B

        m1 := qVal.molecule1
        m2 := qVal.molecule2
    }.otherwise {
        calculateForce.input.bits.molecule1 := m1
        calculateForce.input.bits.molecule2 := m2
        calculateForce.input.valid := false.B
    }

    output.valid := calculateForce.output.valid
    output.bits.data := calculateForce.output.bits.data
    output.bits.error := calculateForce.output.bits.error
}