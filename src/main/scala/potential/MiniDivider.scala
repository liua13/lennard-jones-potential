
package potential

import chisel3._
import chisel3.util._
import potential.Arithmetic.FloatArithmetic._

class FractionInputBundle(expWidth: Int, sigWidth: Int) extends Bundle {
    val numerator = Float(expWidth, sigWidth)
    val denominator = Float(expWidth, sigWidth)
}

class FractionOutputBundle(expWidth: Int, sigWidth: Int) extends Bundle {
    val numerator = Float(expWidth, sigWidth)
    val denominator = Float(expWidth, sigWidth)
    val data = Float(expWidth, sigWidth)
}

class MiniDivider(expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new FractionInputBundle(expWidth, sigWidth))))
    val output = IO(Decoupled(new FractionOutputBundle(expWidth, sigWidth)))

    val dividerValidIn = input.valid && (output.ready || !output.valid)
    val divider = (input.bits.numerator./(input.bits.denominator, dividerValidIn)).get

    output.valid := divider.valid 
    output.bits.numerator := input.bits.numerator
    output.bits.denominator := input.bits.denominator
    output.bits.data := divider.bits
    input.ready := divider.ready && (output.ready || !output.valid)
}