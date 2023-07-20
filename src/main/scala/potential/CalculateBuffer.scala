package potential

import chisel3._
import chisel3.util._
import hardfloat._
import potential.Arithmetic.FloatArithmetic._

// NOTE: dim must be a power of 2
class CalculateBuffer(val dim: Int, expWidth: Int, sigWidth: Int, val entries: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CFInputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CFOutputBundle(dim, expWidth, sigWidth)))
    
    val calculateForce = Module(new CalculateForce(dim, expWidth, sigWidth))
    calculateForce.output.ready := output.ready

    val sigma6WriteIO = IO(new LUTWriteIO(dim * dim, expWidth, sigWidth))
    calculateForce.sigma6WriteIO <> sigma6WriteIO

    val epsilonWriteIO = IO(new LUTWriteIO(dim * dim, expWidth, sigWidth))
    calculateForce.epsilonWriteIO <> epsilonWriteIO

    val qin = Module(new Queue(new CFInputBundle(dim, expWidth, sigWidth), entries))
    qin.io.enq <> input
    qin.io.deq <> calculateForce.input

    val qout = Module(new Queue(new CFOutputBundle(dim, expWidth, sigWidth), entries))
    qout.io.enq <> calculateForce.output
    qout.io.deq <> output
}