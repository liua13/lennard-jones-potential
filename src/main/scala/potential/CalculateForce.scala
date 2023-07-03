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

class Initialize(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new Bundle {
        val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
        val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
    })))

    val output = IO(Decoupled(new Bundle {
        val error = Bool()
        val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
        val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
    }))

    input.ready := output.ready || !output.valid 
    output.valid := input.valid
    output.bits.error := input.valid && 
        input.bits.molecule1.x === input.bits.molecule2.x &&
        input.bits.molecule1.y === input.bits.molecule2.y &&
        input.bits.molecule1.z === input.bits.molecule2.z
    output.bits.molecule1 := input.bits.molecule1
    output.bits.molecule2 := input.bits.molecule2
}

class CalcRsq(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new Bundle {
        val error = Bool()
        val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
        val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
        val sigma6 = Float(expWidth, sigWidth)
        val epsilon = Float(expWidth, sigWidth)
    })))

    val output = IO(Decoupled(new Bundle {
        val error = Bool()
        val sigma6 = Float(expWidth, sigWidth)
        val epsilon = Float(expWidth, sigWidth)
        val rsq = Float(expWidth, sigWidth)
    }))

    input.ready := output.ready || !output.valid 
    val delx = input.bits.molecule2.x - input.bits.molecule1.x
    val dely = input.bits.molecule2.y - input.bits.molecule1.y
    val delz = input.bits.molecule2.z - input.bits.molecule1.z
    output.valid := input.valid
    output.bits.error := input.bits.error
    output.bits.sigma6 := input.bits.sigma6
    output.bits.epsilon := input.bits.epsilon
    output.bits.rsq := delx * delx + dely * dely + delz * delz
}

class CalcSr2(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new Bundle {
        val error = Bool()
        val sigma6 = Float(expWidth, sigWidth)
        val epsilon = Float(expWidth, sigWidth)
        val rsq = Float(expWidth, sigWidth)
    })))

    val output = IO(Decoupled(new Bundle {
        val error = Bool()
        val sigma6 = Float(expWidth, sigWidth)
        val epsilon = Float(expWidth, sigWidth)
        val sr2 = Float(expWidth, sigWidth)
    }))

    val one = Wire(Float(expWidth, sigWidth))
    if(expWidth == 8 && sigWidth == 24) one.bits := 1065353216.U 
    else if(expWidth == 11 && sigWidth == 53) one.bits := 4607182418800017408L.U
    else one.bits := Cat(0.U(2.W), ((1 << (expWidth - 1)) - 1).U((expWidth - 1).W), 0.U((sigWidth - 1).W))

    val dividerValidIn = input.valid && !input.bits.error && (output.ready || !output.valid)
    val divider = (one./(input.bits.rsq, dividerValidIn)).get
    output.valid := (input.valid && input.bits.error) || divider.valid
    output.bits.error := input.bits.error
    output.bits.sigma6 := input.bits.sigma6
    output.bits.epsilon := input.bits.epsilon
    output.bits.sr2 := divider.bits
    input.ready := divider.ready && (output.ready || !output.valid)
}

class CalcSr6(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new Bundle {
        val error = Bool()
        val sigma6 = Float(expWidth, sigWidth)
        val epsilon = Float(expWidth, sigWidth)
        val sr2 = Float(expWidth, sigWidth)
    })))

    val output = IO(Decoupled(new Bundle {
        val error = Bool()
        val epsilon = Float(expWidth, sigWidth)
        val sr2 = Float(expWidth, sigWidth)
        val sr6 = Float(expWidth, sigWidth)
    }))

    input.ready := output.ready || !output.valid
    output.valid := input.valid
    output.bits.error := input.bits.error
    output.bits.epsilon := input.bits.epsilon
    output.bits.sr2 := input.bits.sr2
    output.bits.sr6 := input.bits.sr2 * input.bits.sr2 * input.bits.sr2 * input.bits.sigma6
}

class CalcForce(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new Bundle {
        val error = Bool()
        val epsilon = Float(expWidth, sigWidth)
        val sr2 = Float(expWidth, sigWidth)
        val sr6 = Float(expWidth, sigWidth)
    })))

    val output = IO(Decoupled(new Bundle {
        val error = Bool()
        val force = Float(expWidth, sigWidth)
    }))

    val half = Wire(Float(expWidth, sigWidth))
    if(expWidth == 8 && sigWidth == 24) half.bits := 1056964608.U
    else if(expWidth == 11 && sigWidth == 53) 4602678819172646912L.U
    else half.bits := Cat(0.U(2.W), ((1 << (expWidth - 2)) - 1).U((expWidth - 2).W), 0.U(sigWidth.W))

    val fortyEight = Wire(Float(expWidth, sigWidth))
    if(expWidth == 8 && sigWidth == 24) fortyEight.bits := 1111490560.U
    else if(expWidth == 11 && sigWidth == 53) 4631952216750555136L.U
    else fortyEight.bits := Cat((1 << (expWidth - 4)).U((expWidth - 2).W), (1 << 1).U(2.W), (1 << (sigWidth - 2)).U(sigWidth.W))

    input.ready := output.ready || !output.valid 
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

    // both initialize and the LUT should complete in one clock cycle
    initialize.input.valid := input.valid
    initialize.input.bits.molecule1 := input.bits.molecule1
    initialize.input.bits.molecule2 := input.bits.molecule2
    initialize.output.ready := calcRsq.input.ready

    sigma6Table.readIO.addr := (input.bits.molecule1.id << log2Up(dim).U) + input.bits.molecule2.id
    epsilonTable.readIO.addr := (input.bits.molecule1.id << log2Up(dim).U) + input.bits.molecule2.id

    calcRsq.input.valid := initialize.output.valid
    calcRsq.input.bits.error := initialize.output.bits.error
    calcRsq.input.bits.molecule1 := initialize.output.bits.molecule1
    calcRsq.input.bits.molecule2 := initialize.output.bits.molecule2
    calcRsq.input.bits.sigma6 := sigma6Table.readIO.data
    calcRsq.input.bits.epsilon := epsilonTable.readIO.data
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
    
    input.ready := initialize.input.ready
    output.valid := calcForce.output.valid
    output.bits.error := calcForce.output.bits.error
    output.bits.data := calcForce.output.bits.force
}