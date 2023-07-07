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

case class InitializeInputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
    val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
}

case class InitializeOutputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
    val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
}

class Initialize(val dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new InitializeInputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new InitializeOutputBundle(dim, expWidth, sigWidth)))

    input.ready := output.ready
    output.valid := input.valid
    output.bits.error := input.valid && 
        input.bits.molecule1.x === input.bits.molecule2.x &&
        input.bits.molecule1.y === input.bits.molecule2.y &&
        input.bits.molecule1.z === input.bits.molecule2.z
    output.bits.molecule1 := input.bits.molecule1
    output.bits.molecule2 := input.bits.molecule2
}

case class CalcRsqInputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val molecule1 = new MoleculeBundle(dim, expWidth, sigWidth)
    val molecule2 = new MoleculeBundle(dim, expWidth, sigWidth)
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
}

case class CalcRsqOutputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
    val rsq = Float(expWidth, sigWidth)
}

class CalcRsq(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CalcRsqInputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CalcRsqOutputBundle(dim, expWidth, sigWidth)))

    input.ready := output.ready
    val delx = input.bits.molecule2.x - input.bits.molecule1.x
    val dely = input.bits.molecule2.y - input.bits.molecule1.y
    val delz = input.bits.molecule2.z - input.bits.molecule1.z
    output.valid := input.valid
    output.bits.error := input.bits.error
    output.bits.sigma6 := input.bits.sigma6
    output.bits.epsilon := input.bits.epsilon
    output.bits.rsq := delx * delx + dely * dely + delz * delz
}

case class CalcSr2InputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
    val rsq = Float(expWidth, sigWidth)
}

case class CalcSr2OutputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
    val sr2 = Float(expWidth, sigWidth)
}

class CalcSr2(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CalcSr2InputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CalcSr2OutputBundle(dim, expWidth, sigWidth)))

    val one = Wire(Float(expWidth, sigWidth))
    if(expWidth == 8 && sigWidth == 24) one.bits := 1065353216.U 
    else if(expWidth == 11 && sigWidth == 53) one.bits := 4607182418800017408L.U
    else one.bits := Cat(0.U(2.W), ((1 << (expWidth - 1)) - 1).U((expWidth - 1).W), 0.U((sigWidth - 1).W))

    val sigma6Reg = Reg(Float(expWidth, sigWidth))
    val epsilonReg = Reg(Float(expWidth, sigWidth))
    when(input.ready && input.valid && !input.bits.error && (output.ready)) {
        sigma6Reg := input.bits.sigma6
        epsilonReg := input.bits.epsilon
    }.otherwise {
        sigma6Reg := sigma6Reg
        epsilonReg := epsilonReg
    }
    
    val dividerValidIn = input.ready && input.valid && !input.bits.error && (output.ready)
    val divider = (one./(input.bits.rsq, dividerValidIn)).get
    output.valid := (input.valid && input.bits.error) || divider.valid
    output.bits.error := input.bits.error
    output.bits.sigma6 := sigma6Reg
    output.bits.epsilon := epsilonReg
    output.bits.sr2 := divider.bits
    input.ready := divider.ready && (output.ready)
}

case class CalcSr6InputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val sigma6 = Float(expWidth, sigWidth)
    val epsilon = Float(expWidth, sigWidth)
    val sr2 = Float(expWidth, sigWidth)
}

case class CalcSr6OutputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val epsilon = Float(expWidth, sigWidth)
    val sr2 = Float(expWidth, sigWidth)
    val sr6 = Float(expWidth, sigWidth)
}

class CalcSr6(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
    val input = IO(Flipped(Decoupled(new CalcSr6InputBundle(dim, expWidth, sigWidth))))
    val output = IO(Decoupled(new CalcSr6OutputBundle(dim, expWidth, sigWidth)))

    input.ready := output.ready
    output.valid := input.valid
    output.bits.error := input.bits.error
    output.bits.epsilon := input.bits.epsilon
    output.bits.sr2 := input.bits.sr2
    output.bits.sr6 := input.bits.sr2 * input.bits.sr2 * input.bits.sr2 * input.bits.sigma6
}

case class CalcForceInputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val epsilon = Float(expWidth, sigWidth)
    val sr2 = Float(expWidth, sigWidth)
    val sr6 = Float(expWidth, sigWidth)
}

case class CalcForceOutputBundle(val dim: Int, expWidth: Int, sigWidth: Int) extends Bundle {
    val error = Bool()
    val force = Float(expWidth, sigWidth)
}

class CalcForce(dim: Int, expWidth: Int, sigWidth: Int) extends Module {
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

    val initializeOutputReg = Reg(new Bundle{
        val bits = InitializeOutputBundle(dim, expWidth, sigWidth)
        val sigma6 = Float(expWidth, sigWidth)
        val epsilon = Float(expWidth, sigWidth)
        val valid = Bool()
    })
    val calcRsqOutputReg = Reg(new Bundle{
        val bits = CalcRsqOutputBundle(dim, expWidth, sigWidth)
        val valid = Bool()
    })
    val calcSr2OutputReg = Reg(new Bundle{
        val bits = CalcSr2OutputBundle(dim, expWidth, sigWidth)
        val valid = Bool()
    })
    val calcSr6OutputReg = Reg(new Bundle{
        val bits = CalcSr6OutputBundle(dim, expWidth, sigWidth)
        val valid = Bool()
    })

    // both initialize and the LUT should complete in one clock cycle
    initialize.input.valid := input.valid && initialize.input.ready
    initialize.input.bits.molecule1 := input.bits.molecule1
    initialize.input.bits.molecule2 := input.bits.molecule2
    initialize.output.ready := !initializeOutputReg.valid // RegNext(calcRsq.input.ready)

    sigma6Table.readIO.addr := (input.bits.molecule1.id << log2Up(dim).U) + input.bits.molecule2.id
    epsilonTable.readIO.addr := (input.bits.molecule1.id << log2Up(dim).U) + input.bits.molecule2.id

    when(initializeOutputReg.valid && !calcRsq.input.ready) {
        initializeOutputReg := initializeOutputReg
    }.otherwise {
        initializeOutputReg.valid := initialize.output.valid
        initializeOutputReg.bits := initialize.output.bits
        initializeOutputReg.sigma6 := sigma6Table.readIO.data
        initializeOutputReg.epsilon := epsilonTable.readIO.data
    }

    calcRsq.input.valid := initializeOutputReg.valid && calcRsq.input.ready
    calcRsq.input.bits.error := initializeOutputReg.bits.error
    calcRsq.input.bits.molecule1 := initializeOutputReg.bits.molecule1
    calcRsq.input.bits.molecule2 := initializeOutputReg.bits.molecule2
    calcRsq.input.bits.sigma6 := initializeOutputReg.sigma6
    calcRsq.input.bits.epsilon := initializeOutputReg.epsilon
    calcRsq.output.ready := !calcRsqOutputReg.valid

    when(calcRsqOutputReg.valid && !calcSr2.input.ready) { 
        calcRsqOutputReg := calcRsqOutputReg
    }.otherwise {
        calcRsqOutputReg.valid := calcRsq.output.valid
        calcRsqOutputReg.bits := calcRsq.output.bits
    }

    calcSr2.input.valid := calcRsqOutputReg.valid && calcSr2.input.ready
    calcSr2.input.bits.error := calcRsqOutputReg.bits.error
    calcSr2.input.bits.sigma6 := calcRsqOutputReg.bits.sigma6
    calcSr2.input.bits.epsilon := calcRsqOutputReg.bits.epsilon
    calcSr2.input.bits.rsq := calcRsqOutputReg.bits.rsq
    calcSr2.output.ready := !calcSr2OutputReg.valid

    when(calcSr2OutputReg.valid && !calcSr6.input.ready) { 
        calcSr2OutputReg := calcSr2OutputReg
    }.otherwise {
        calcSr2OutputReg.valid := calcSr2.output.valid
        calcSr2OutputReg.bits := calcSr2.output.bits
    }

    calcSr6.input.valid := calcSr2OutputReg.valid && calcSr6.input.ready
    calcSr6.input.bits.error := calcSr2OutputReg.bits.error
    calcSr6.input.bits.sigma6 := calcSr2OutputReg.bits.sigma6
    calcSr6.input.bits.epsilon := calcSr2OutputReg.bits.epsilon
    calcSr6.input.bits.sr2 := calcSr2OutputReg.bits.sr2
    calcSr6.output.ready := !calcSr6OutputReg.valid

    when(calcSr6OutputReg.valid && !calcForce.input.ready) { 
        calcSr6OutputReg := calcSr6OutputReg
    }.otherwise {
        calcSr6OutputReg.valid := calcSr6.output.valid
        calcSr6OutputReg.bits := calcSr6.output.bits
    }

    calcForce.input.valid := calcSr6OutputReg.valid && calcForce.input.ready
    calcForce.input.bits.error := calcSr6OutputReg.bits.error
    calcForce.input.bits.epsilon := calcSr6OutputReg.bits.epsilon
    calcForce.input.bits.sr2 := calcSr6OutputReg.bits.sr2
    calcForce.input.bits.sr6 := calcSr6OutputReg.bits.sr6
    calcForce.output.ready := output.ready
    
    input.ready := initialize.input.ready
    output.valid := calcForce.output.valid
    output.bits.error := calcForce.output.bits.error
    output.bits.data := calcForce.output.bits.force
}