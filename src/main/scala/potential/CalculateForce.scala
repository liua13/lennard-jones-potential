
package potential

import chisel3._
import chisel3.util._
import hardfloat._
import potential.Arithmetic.FloatArithmetic._

case class MoleculeBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val id = UInt(log2Up(dim).W)
    val x = Float(expWidth, sigWidth)
    val y = Float(expWidth, sigWidth)
    val z = Float(expWidth, sigWidth)
}

case class InitializeInputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
    val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
}

case class InitializeOutputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
    val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
}

class Initialize(val dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new InitializeInputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new InitializeOutputBundle(dim, expWidth, sigWidth)))

    val outputReg = Reg(InitializeOutputBundle(dim, expWidth, sigWidth))
    val outputRegValid = Reg(Bool())

    input.ready := output.ready || !output.valid

    when(output.ready || !output.valid) {
        outputReg.error := input.valid && 
            input.bits.molecule1.x === input.bits.molecule2.x &&
            input.bits.molecule1.y === input.bits.molecule2.y &&
            input.bits.molecule1.z === input.bits.molecule2.z
        outputReg.molecule1 := input.bits.molecule1
        outputReg.molecule2 := input.bits.molecule2
        outputRegValid := input.valid 
    }.otherwise {
        outputReg := outputReg
        outputRegValid := outputRegValid
    }

    output.bits := outputReg
    output.valid := outputRegValid
}

case class CalcRsqInputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
    val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
}

case class CalcRsqOutputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
    val rsq = Float(expWidth, sigWidth)
}

class CalcRsq(val dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CalcRsqInputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CalcRsqOutputBundle(dim, expWidth, sigWidth)))

    val outputReg = Reg(CalcRsqOutputBundle(dim, expWidth, sigWidth))
    val outputRegValid = Reg(Bool())

    input.ready := output.ready || !output.valid

    when(output.ready || !output.valid) {
        val delx = input.bits.molecule2.x - input.bits.molecule1.x
        val dely = input.bits.molecule2.y - input.bits.molecule1.y
        val delz = input.bits.molecule2.z - input.bits.molecule1.z
        outputReg.error := input.bits.error
        outputReg.sigma6 := input.bits.sigma6
        outputReg.epsilon := input.bits.epsilon
        outputReg.rsq := delx * delx + dely * dely + delz * delz
        outputRegValid := input.valid
    }.otherwise {
        outputReg := outputReg
        outputRegValid := outputRegValid
    }

    output.bits := outputReg
    output.valid := outputRegValid
}

case class CalcSr2InputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
    val rsq = Float(expWidth, sigWidth)
}

case class CalcSr2OutputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
    val sr2 = Float(expWidth, sigWidth)
}

class CalcSr2(val dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CalcSr2InputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CalcSr2OutputBundle(dim, expWidth, sigWidth)))

    val one = Wire(Float(expWidth, sigWidth))
    if(expWidth == 8 && sigWidth == 24) one.bits := 1065353216.U 
    else if(expWidth == 11 && sigWidth == 53) one.bits := 4607182418800017408L.U
    else one.bits := Cat(0.U(2.W), ((1 << (expWidth - 1)) - 1).U((expWidth - 1).W), 0.U((sigWidth - 1).W))

    val outputReg = Reg(CalcSr2OutputBundle(dim, expWidth, sigWidth))
    val outputRegValid = Reg(Bool())
    val dividerReady = Reg(Bool())

    val dividerValidIn = input.valid && (output.ready || !output.valid) && !input.bits.error 
    val divider = (one./(input.bits.rsq, dividerValidIn)).get

    dividerReady := divider.ready
    input.ready := dividerReady && (output.ready || !output.valid)

    when(dividerReady && input.valid) { //  && (output.ready || !output.valid)
        outputReg.error := input.bits.error
        outputReg.sigma6 := input.bits.sigma6
        outputReg.epsilon := input.bits.epsilon
    }.otherwise {
        outputReg.error := outputReg.error
        outputReg.sigma6 := outputReg.sigma6
        outputReg.epsilon := outputReg.epsilon
    }

    when(output.ready || !output.valid) {
        outputReg.sr2 := divider.bits
        outputRegValid := (input.valid && input.bits.error) || divider.valid
    }.otherwise {
        outputReg := outputReg
        outputRegValid := outputRegValid
    }

    output.bits := outputReg
    output.valid := outputRegValid
}

case class CalcSr6InputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
    val sr2 = Float(expWidth, sigWidth)
}

case class CalcSr6OutputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val epsilon = Float(expWidth, sigWidth)
    val sr2 = Float(expWidth, sigWidth)
    val sr6 = Float(expWidth, sigWidth)
}

class CalcSr6(val dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CalcSr6InputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CalcSr6OutputBundle(dim, expWidth, sigWidth)))

    val outputReg = Reg(CalcSr6OutputBundle(dim, expWidth, sigWidth))
    val outputRegValid = Reg(Bool())
    
    input.ready := output.ready || !output.valid 

    when(output.ready || !output.valid) {
        outputReg.error := input.bits.error
        outputReg.epsilon := input.bits.epsilon
        outputReg.sr2 := input.bits.sr2
        outputReg.sr6 := input.bits.sr2 * input.bits.sr2 * input.bits.sr2 * input.bits.sigma6
        outputRegValid := input.valid
    }.otherwise {
        outputReg := outputReg
        outputRegValid := outputRegValid
    }

    output.bits := outputReg
    output.valid := outputRegValid
}

case class CalcForceInputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val epsilon = Float(expWidth, sigWidth)
    val sr2 = Float(expWidth, sigWidth)
    val sr6 = Float(expWidth, sigWidth)
}

case class CalcForceOutputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val force = Float(expWidth, sigWidth)
}

class CalcForce(val dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CalcForceInputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CalcForceOutputBundle(dim, expWidth, sigWidth)))

    val half = Wire(Float(expWidth, sigWidth))
    if(expWidth == 8 && sigWidth == 24) half.bits := 1056964608.U
    else if(expWidth == 11 && sigWidth == 53) 4602678819172646912L.U
    else half.bits := Cat(0.U(2.W), ((1 << (expWidth - 2)) - 1).U((expWidth - 2).W), 0.U(sigWidth.W))

    val fortyEight = Wire(Float(expWidth, sigWidth))
    if(expWidth == 8 && sigWidth == 24) fortyEight.bits := 1111490560.U
    else if(expWidth == 11 && sigWidth == 53) 4631952216750555136L.U
    else fortyEight.bits := Cat((1 << (expWidth - 4)).U((expWidth - 2).W), (1 << 1).U(2.W), (1 << (sigWidth - 2)).U(sigWidth.W))

    input.ready := output.ready
    output.valid := input.valid 
    output.bits.error := input.bits.error
    output.bits.force := fortyEight * input.bits.sr6 * (input.bits.sr6 - half) * input.bits.sr2 * input.bits.epsilon
}

case class CFInputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
    val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
}

case class CFOutputBundle(dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val data = Float(expWidth, sigWidth)
}

class CalculateForce(val dim: Int, expWidth: Int, sigWidth: Int) extends Module { 
    val input = IO(Flipped(Decoupled(new CFInputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CFOutputBundle(dim, expWidth, sigWidth)))

    val sigma6Table = Module(new LUT(dim * dim, expWidth, sigWidth))
    val sigma6WriteIO = IO(new LUTWriteIO(dim * dim, expWidth, sigWidth))
    sigma6Table.writeIO <> sigma6WriteIO

    val epsilonTable = Module(new LUT(dim * dim, expWidth, sigWidth))
    val epsilonWriteIO = IO(new LUTWriteIO(dim * dim, expWidth, sigWidth))
    epsilonTable.writeIO <> epsilonWriteIO

    val initialize = Module(new Initialize(dim, expWidth, sigWidth))
    val calcRsq = Module(new CalcRsq(dim, expWidth, sigWidth))
    val calcSr2 = Module(new CalcSr2(dim, expWidth, sigWidth))
    val calcSr6 = Module(new CalcSr6(dim, expWidth, sigWidth))
    val calcForce = Module(new CalcForce(dim, expWidth, sigWidth))

    val inputReg = Reg(new Bundle{
        val valid = Bool()
        val bits = InitializeOutputBundle(dim, expWidth, sigWidth)
    })

    input.ready := initialize.input.ready

    initialize.input.valid := input.valid
    initialize.input.bits.molecule1 := input.bits.molecule1
    initialize.input.bits.molecule2 := input.bits.molecule2
    initialize.output.ready := calcRsq.input.ready // RegNext(calcRsq.input.ready)

    // LUT should take one clock cycle; inputReg holds for one clock cycle
    inputReg.valid := initialize.output.valid
    inputReg.bits := initialize.output.bits

    sigma6Table.readIO.addr := (initialize.output.bits.molecule1.id << log2Up(dim).U) + initialize.output.bits.molecule2.id
    epsilonTable.readIO.addr := (initialize.output.bits.molecule1.id << log2Up(dim).U) + initialize.output.bits.molecule2.id

    calcRsq.input.valid := inputReg.valid 
    calcRsq.input.bits.error := inputReg.bits.error
    calcRsq.input.bits.molecule1 := inputReg.bits.molecule1
    calcRsq.input.bits.molecule2 := inputReg.bits.molecule2
    calcRsq.input.bits.sigma6 := sigma6Table.readIO.data // initialize.output.bits.sigma6
    calcRsq.input.bits.epsilon := epsilonTable.readIO.data // initialize.output.bits.sigma6
    calcRsq.output.ready := calcSr2.input.ready

    calcSr2.input.valid := calcRsq.output.valid
    calcSr2.input.bits.error := calcRsq.output.bits.error
    calcSr2.input.bits.sigma6 := calcRsq.output.bits.sigma6
    calcSr2.input.bits.epsilon := calcRsq.output.bits.epsilon
    calcSr2.input.bits.rsq := calcRsq.output.bits.rsq
    calcSr2.output.ready := calcSr6.input.ready

    calcSr6.input.valid := calcSr2.output.valid
    calcSr6.input.bits.error := calcSr2.output.bits.error
    calcSr6.input.bits.sigma6 := calcSr2.output.bits.sigma6
    calcSr6.input.bits.epsilon := calcSr2.output.bits.epsilon
    calcSr6.input.bits.sr2 := calcSr2.output.bits.sr2
    calcSr6.output.ready := calcForce.input.ready

    calcForce.input.valid := calcSr6.output.valid
    calcForce.input.bits.error := calcSr6.output.bits.error
    calcForce.input.bits.epsilon := calcSr6.output.bits.epsilon
    calcForce.input.bits.sr2 := calcSr6.output.bits.sr2
    calcForce.input.bits.sr6 := calcSr6.output.bits.sr6
    calcForce.output.ready := output.ready
    
    output.valid := calcForce.output.valid
    output.bits.error := calcForce.output.bits.error
    output.bits.data := calcForce.output.bits.force
}

// initialize.input.valid := input.valid && initialize.input.ready
// initialize.input.bits.molecule1 := input.bits.molecule1
// initialize.input.bits.molecule2 := input.bits.molecule2
// initialize.output.ready := !initializeOutputReg.valid // RegNext(calcRsq.input.ready)

// sigma6Table.readIO.addr := (input.bits.molecule1.id << log2Up(dim).U) + input.bits.molecule2.id
// epsilonTable.readIO.addr := (input.bits.molecule1.id << log2Up(dim).U) + input.bits.molecule2.id

// when(initializeOutputReg.valid && !calcRsq.input.ready) {
//     initializeOutputReg := initializeOutputReg
// }.otherwise {
//     initializeOutputReg.valid := initialize.output.valid
//     initializeOutputReg.bits := initialize.output.bits
//     initializeOutputReg.sigma6 := sigma6Table.readIO.data
//     initializeOutputReg.epsilon := epsilonTable.readIO.data
// }

// calcRsq.input.valid := initializeOutputReg.valid && calcRsq.input.ready
// calcRsq.input.bits.error := initializeOutputReg.bits.error
// calcRsq.input.bits.molecule1 := initializeOutputReg.bits.molecule1
// calcRsq.input.bits.molecule2 := initializeOutputReg.bits.molecule2
// calcRsq.input.bits.sigma6 := initializeOutputReg.sigma6
// calcRsq.input.bits.epsilon := initializeOutputReg.epsilon
// calcRsq.output.ready := !calcRsqOutputReg.valid

// when(calcRsqOutputReg.valid && !calcSr2.input.ready) { 
//     calcRsqOutputReg := calcRsqOutputReg
// }.otherwise {
//     calcRsqOutputReg.valid := calcRsq.output.valid
//     calcRsqOutputReg.bits := calcRsq.output.bits
// }

// calcSr2.input.valid := calcRsqOutputReg.valid && calcSr2.input.ready
// calcSr2.input.bits.error := calcRsqOutputReg.bits.error
// calcSr2.input.bits.sigma6 := calcRsqOutputReg.bits.sigma6
// calcSr2.input.bits.epsilon := calcRsqOutputReg.bits.epsilon
// calcSr2.input.bits.rsq := calcRsqOutputReg.bits.rsq
// calcSr2.output.ready := !calcSr2OutputReg.valid

// when(calcSr2OutputReg.valid && !calcSr6.input.ready) { 
//     calcSr2OutputReg := calcSr2OutputReg
// }.otherwise {
//     calcSr2OutputReg.valid := calcSr2.output.valid
//     calcSr2OutputReg.bits := calcSr2.output.bits
// }

// calcSr6.input.valid := calcSr2OutputReg.valid && calcSr6.input.ready
// calcSr6.input.bits.error := calcSr2OutputReg.bits.error
// calcSr6.input.bits.sigma6 := calcSr2OutputReg.bits.sigma6
// calcSr6.input.bits.epsilon := calcSr2OutputReg.bits.epsilon
// calcSr6.input.bits.sr2 := calcSr2OutputReg.bits.sr2
// calcSr6.output.ready := !calcSr6OutputReg.valid

// when(calcSr6OutputReg.valid && !calcForce.input.ready) { 
//     calcSr6OutputReg := calcSr6OutputReg
// }.otherwise {
//     calcSr6OutputReg.valid := calcSr6.output.valid
//     calcSr6OutputReg.bits := calcSr6.output.bits
// }

// calcForce.input.valid := calcSr6OutputReg.valid && calcForce.input.ready
// calcForce.input.bits.error := calcSr6OutputReg.bits.error
// calcForce.input.bits.epsilon := calcSr6OutputReg.bits.epsilon
// calcForce.input.bits.sr2 := calcSr6OutputReg.bits.sr2
// calcForce.input.bits.sr6 := calcSr6OutputReg.bits.sr6
// calcForce.output.ready := output.ready

// input.ready := initialize.input.ready
// output.valid := calcForce.output.valid
// output.bits.error := calcForce.output.bits.error
// output.bits.data := calcForce.output.bits.force