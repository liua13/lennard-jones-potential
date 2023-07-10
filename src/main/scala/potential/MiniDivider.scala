
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

// class MiniDivider2(expWidth: Int, sigWidth: Int) extends Module {
//     val input = IO(Flipped(Decoupled(new FractionInputBundle(expWidth, sigWidth))))
//     val output = IO(Decoupled(new FractionOutputBundle(expWidth, sigWidth)))

//     val dividerValidIn = input.valid && (output.ready || !output.valid)
//     val divider = (input.bits.numerator./(input.bits.denominator, dividerValidIn)).get
    
//     val outputReg = Reg(new FractionOutputBundle(expWidth, sigWidth))
//     val outputRegValid = Reg(Bool())

//     outputRegValid := divider.valid 
//     outputReg.numerator := input.bits.numerator
//     outputReg.denominator := input.bits.denominator
//     outputReg.data := divider.bits
//     input.ready := divider.ready && (output.ready || !output.valid)

//     output.bits := outputReg
//     output.valid := outputRegValid
// }

class MiniDivider2(expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new FractionInputBundle(expWidth, sigWidth))))
    val output = IO(Decoupled(new FractionOutputBundle(expWidth, sigWidth)))

    val one = Wire(Float(expWidth, sigWidth))
    if(expWidth == 8 && sigWidth == 24) one.bits := 1065353216.U 
    else if(expWidth == 11 && sigWidth == 53) one.bits := 4607182418800017408L.U
    else one.bits := Cat(0.U(2.W), ((1 << (expWidth - 1)) - 1).U((expWidth - 1).W), 0.U((sigWidth - 1).W))

    val dividerValidIn = input.valid && (output.ready || !output.valid)
    val divider = (one./(input.bits.denominator, dividerValidIn)).get
    
    val outputReg = Reg(new FractionOutputBundle(expWidth, sigWidth))
    val outputRegValid = Reg(Bool())

    outputRegValid := divider.valid 
    outputReg.numerator := input.bits.numerator
    outputReg.denominator := input.bits.denominator
    outputReg.data := divider.bits
    input.ready := divider.ready && (output.ready || !output.valid)

    output.bits := outputReg
    output.valid := outputRegValid
}