package potential

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._

/**
  * sbt 'testOnly potential.LUTSpec'
  * sbt clean test
  */
class CalculateForceSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Testing conversion from decimal to floating 32 bit" in {
    assert(Math.abs(1072902963 - decimal_to_floating32(1.9)) <= ERROR)
    assert(Math.abs(1065353216 - decimal_to_floating32(1.0)) <= ERROR)
    assert(Math.abs(1073741824 - decimal_to_floating32(2.0)) <= ERROR)
    assert(Math.abs(1077936128 - decimal_to_floating32(3.0)) <= ERROR)
    assert(Math.abs(1087981486 - decimal_to_floating32(6.79)) <= ERROR)
    assert(Math.abs(3253096939L - decimal_to_floating32(-28.79)) <= ERROR)
    assert(Math.abs(1063675494 - decimal_to_floating32(0.9)) <= ERROR)
    assert(Math.abs(0 - decimal_to_floating32(0.0F)) <= ERROR)
  }

  "Testing conversion from floating 32 bit to decimal" in {
    assert(Math.abs(1.9F - floating32_to_decimal(1072902963)) <= ERROR)
    assert(Math.abs(1.0F - floating32_to_decimal(1065353216)) <= ERROR)
    assert(Math.abs(2.0F - floating32_to_decimal(1073741824)) <= ERROR)
    assert(Math.abs(3.0F - floating32_to_decimal(1077936128)) <= ERROR)
    assert(Math.abs(6.79F - floating32_to_decimal(1087981486)) <= ERROR)
    assert(Math.abs(-28.79F - floating32_to_decimal(3253096940L)) <= ERROR)
    assert(Math.abs(0.9F - floating32_to_decimal(1063675494)) <= ERROR)
    assert(Math.abs(0.0F - floating32_to_decimal(0)) <= ERROR)
  }

  "Testing decimal to and from floating 32 bit conversion" in {
    assert(Math.abs(1.9F - floating32_to_decimal(decimal_to_floating32(1.9F))) <= ERROR)
    assert(Math.abs(1.0F - floating32_to_decimal(decimal_to_floating32(1.0F))) <= ERROR)
    assert(Math.abs(2.0F - floating32_to_decimal(decimal_to_floating32(2.0F))) <= ERROR)
    assert(Math.abs(3.0F - floating32_to_decimal(decimal_to_floating32(3.0F))) <= ERROR)
    assert(Math.abs(6.79F - floating32_to_decimal(decimal_to_floating32(6.79F))) <= ERROR)
    assert(Math.abs(-28.79F - floating32_to_decimal(decimal_to_floating32(-28.79F))) <= ERROR)
    assert(Math.abs(0.9F - floating32_to_decimal(decimal_to_floating32(0.9F))) <= ERROR)
    assert(Math.abs(0.0F - floating32_to_decimal(decimal_to_floating32(0F))) <= ERROR)
  }

  "Testing Initialize module" in {
    test(new Initialize(dim=4, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.output.ready.poke(true)

      var arr = new Array[MoleculeInfo](dut.dim)

      for(i <- 0 until dut.dim) {
        val m1x = get_float()
        val m1y = get_float()
        val m1z = get_float()

        val m2x = get_float()
        val m2y = get_float()
        val m2z = get_float()

        if(i == dut.dim / 2) {
            arr(i) = MoleculeInfo(1, m1x, m1y, m1z, 2, m1x, m1y, m1z, i, 0F, 0F, 0F, 0F, 0F, 0F)
        } else {
            arr(i) = MoleculeInfo(1, m1x, m1y, m1z, 2, m2x, m2y, m2z, i, 0F, 0F, 0F, 0F, 0F, 0F)
        }
      }

      for(i <- 0 until dut.dim) {
        val entry = arr(i)

        dut.input.bits.molecule1.id.poke(1)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(entry.m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(entry.m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(entry.m1z))

        dut.input.bits.molecule2.id.poke(2)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(entry.m2x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(entry.m2y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(entry.m2z))
        dut.input.valid.poke(true)

        while(!dut.input.ready.peek().litToBoolean) {
            dut.clock.step(1) 
        }

        dut.clock.step(1)

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(i == dut.dim / 2)

        // dut.clock.step(1)
      }
    }
  }

  "Testing CalcRsq module" in {
    test(new CalcRsq(dim=4, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.output.ready.poke(true)

      var arr = new Array[MoleculeInfo](dut.dim)

      for(i <- 0 until dut.dim) {
        val m1x = get_float()
        val m1y = get_float()
        val m1z = get_float()

        val m2x = get_float()
        val m2y = get_float()
        val m2z = get_float()

        val delx = (m2x.toFloat - m1x.toFloat).toFloat
        val dely = (m2y.toFloat - m1y.toFloat).toFloat
        val delz = (m2z.toFloat - m1z.toFloat).toFloat
        val rsq = (delx * delx + dely * dely + delz * delz).toFloat

        val sigma6 = get_float()
        val epsilon = get_float()

        if(i == dut.dim / 2) {
            arr(i) = MoleculeInfo(1, m1x, m1y, m1z, 2, m1x, m1y, m1z, i, sigma6, epsilon, 0F, 0F, 0F, 0F)
        } else {
            arr(i) = MoleculeInfo(1, m1x, m1y, m1z, 2, m2x, m2y, m2z, i, sigma6, epsilon, rsq, 0F, 0F, 0F)
        }
      }

      for(i <- 0 until dut.dim) {
        val entry = arr(i)

        dut.input.bits.molecule1.id.poke(entry.m1id)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(entry.m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(entry.m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(entry.m1z))

        dut.input.bits.molecule2.id.poke(entry.m2id)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(entry.m2x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(entry.m2y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(entry.m2z))

        dut.input.bits.index.poke(entry.index)
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(entry.sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(entry.epsilon))
        dut.input.bits.error.poke(i == dut.dim / 2)
        dut.input.valid.poke(true)

        while(!dut.input.ready.peek().litToBoolean) {
            dut.clock.step(1) 
        }

        dut.clock.step(1)

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(i == dut.dim / 2)

        dut.output.bits.index.expect(entry.index)
        dut.output.bits.sigma6.bits.expect(decimal_to_floating32(entry.sigma6))
        dut.output.bits.epsilon.bits.expect(decimal_to_floating32(entry.epsilon))

        if(i != dut.dim / 2) {
            assert(Math.abs((entry.rsq - floating32_to_decimal(dut.output.bits.rsq.bits.peek().litValue.toLong)) / entry.rsq) <= ERROR)
        }
      }
    }
  }

  "Testing CalcSr2 module" in {
    test(new CalcSr2(dim=8, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.output.ready.poke(true)

      var arr = new Array[MoleculeInfo](dut.dim)

      for(i <- 0 until dut.dim) {
        val sigma6 = get_float()
        val epsilon = get_float()
        val rsq = get_float()
        val sr2 = (1.0 / rsq).toFloat

        if(i == dut.dim / 2) {
            arr(i) = MoleculeInfo(1, 0F, 0F, 0F, 2, 0F, 0F, 0F, i, sigma6, epsilon, 0F, 0F, 0F, 0F)
        } else {
            arr(i) = MoleculeInfo(1, 0F, 0F, 0F, 2, 0F, 0F, 0F, i, sigma6, epsilon, rsq, sr2, 0F, 0F)
        }
      }

      for(i <- 0 until dut.dim) {
        val entry = arr(i)

        dut.input.bits.index.poke(entry.index)
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(entry.sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(entry.epsilon))
        dut.input.bits.rsq.bits.poke(decimal_to_floating32(entry.rsq))

        dut.input.bits.error.poke(i == dut.dim / 2)
        dut.input.valid.poke(true)

        while(!dut.input.ready.peek().litToBoolean) {
            dut.clock.step(1)
        }

        dut.clock.step(1)
        dut.input.valid.poke(false)

        while(!dut.output.valid.peek().litToBoolean) {
            dut.clock.step(1) 
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(i == dut.dim / 2)

        dut.output.bits.index.expect(entry.index)
        dut.input.bits.sigma6.bits.expect(decimal_to_floating32(entry.sigma6))
        dut.input.bits.epsilon.bits.expect(decimal_to_floating32(entry.epsilon))

        if(i != dut.dim / 2) {
            assert(Math.abs((entry.sr2 - floating32_to_decimal(dut.output.bits.sr2.bits.peek().litValue.toLong)) / entry.sr2) <= ERROR)
        }
      }
    }
  }

"Testing CalcSr6 module" in {
    test(new CalcSr6(dim=4, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.output.ready.poke(true)

      var arr = new Array[MoleculeInfo](dut.dim)

      for(i <- 0 until dut.dim) {
        val sigma6 = get_float()
        val epsilon = get_float()
        val sr2 = get_float()
        val sr6 = (sr2 * sr2 * sr2 * sigma6).toFloat

        if(i == dut.dim / 2) {
            arr(i) = MoleculeInfo(1, 0F, 0F, 0F, 2, 0F, 0F, 0F, i, sigma6, epsilon, 0F, sr2, 0F, 0F)
        } else {
            arr(i) = MoleculeInfo(1, 0F, 0F, 0F, 2, 0F, 0F, 0F, i, sigma6, epsilon, 0F, sr2, sr6, 0F)
        }
      }

      for(i <- 0 until dut.dim) {
        val entry = arr(i)

        dut.input.bits.index.poke(entry.index)
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(entry.sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(entry.epsilon))
        dut.input.bits.sr2.bits.poke(decimal_to_floating32(entry.sr2))
        dut.input.bits.error.poke(i == dut.dim / 2)
        dut.input.valid.poke(true)

        while(!dut.input.ready.peek().litToBoolean) {
            dut.clock.step(1) 
        }

        dut.clock.step(1)

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(i == dut.dim / 2)

        dut.output.bits.index.expect(entry.index)
        dut.output.bits.epsilon.bits.expect(decimal_to_floating32(entry.epsilon))
        dut.output.bits.sr2.bits.expect(decimal_to_floating32(entry.sr2))

        if(i != dut.dim / 2) {
            assert(Math.abs((entry.sr6 - floating32_to_decimal(dut.output.bits.sr6.bits.peek().litValue.toLong)) / entry.sr6) <= ERROR)
        }
      }
    }
  }

  "Testing CalcForce module" in {
    test(new CalcForce(dim=4, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.output.ready.poke(true)

      var arr = new Array[MoleculeInfo](dut.dim)

      for(i <- 0 until dut.dim) {
        val sigma6 = get_float()
        val epsilon = get_float()
        val sr2 = get_float()
        val sr6 = (sr2 * sr2 * sr2 * sigma6).toFloat
        val force = (48.0F * sr6 * (sr6 - 0.5F) * sr2 * epsilon).toFloat

        if(i == dut.dim / 2) {
            arr(i) = MoleculeInfo(1, 0F, 0F, 0F, 2, 0F, 0F, 0F, i, sigma6, epsilon, 0F, sr2, sr6, 0F)
        } else {
            arr(i) = MoleculeInfo(1, 0F, 0F, 0F, 2, 0F, 0F, 0F, i, sigma6, epsilon, 0F, sr2, sr6, force)
        }
      }

      for(i <- 0 until dut.dim) {
        val entry = arr(i)

        dut.input.bits.index.poke(entry.index)
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(entry.epsilon))
        dut.input.bits.sr2.bits.poke(decimal_to_floating32(entry.sr2))
        dut.input.bits.sr6.bits.poke(decimal_to_floating32(entry.sr6))
        dut.input.bits.error.poke(i == dut.dim / 2)
        dut.input.valid.poke(true)

        while(!dut.input.ready.peek().litToBoolean) {
            dut.clock.step(1) 
        }

        dut.clock.step(1)

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(i == dut.dim / 2)
        dut.output.bits.index.expect(entry.index)

        if(i != dut.dim / 2) {
            assert(Math.abs((entry.force - floating32_to_decimal(dut.output.bits.force.bits.peek().litValue.toLong)) / entry.force) <= ERROR)
        }
      }
    }
  }

  "Testing CalculateForce module" in {
    test(new CalculateForce(dim=8, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.output.ready.poke(true)

      var sigma6Table = getTable(dut.dim)
      var epsilonTable = getTable(dut.dim)
      var arr = new Array[MoleculeInfo](dut.dim)

      // put values into LUTs
      for(i <- 0 until dut.dim) {
        for(j <- 0 until dut.dim) {
          val index = i * dut.dim + j
          dut.sigma6WriteIO.addr.poke(index)
          dut.sigma6WriteIO.data.bits.poke(decimal_to_floating32(sigma6Table(index)))
          dut.sigma6WriteIO.validIn.poke(true)

          dut.epsilonWriteIO.addr.poke(index)
          dut.epsilonWriteIO.data.bits.poke(decimal_to_floating32(epsilonTable(index)))
          dut.epsilonWriteIO.validIn.poke(true)

          dut.clock.step(1)
        }
      }

      dut.sigma6WriteIO.validIn.poke(false)
      dut.epsilonWriteIO.validIn.poke(false)
      dut.sigma6WriteIO.tableReady.poke(true)
      dut.epsilonWriteIO.tableReady.poke(true)
      dut.clock.step(1)

      // add data to array
      for(i <- 0 until dut.dim / 2) {
        val m1id = i * 2
        val m1x = get_float()
        val m1y = get_float()
        val m1z = get_float()

        val m2id = i * 2 + 1
        val m2x = get_float()
        val m2y = get_float()
        val m2z = get_float()

        val sigma6 = sigma6Table(m1id * dut.dim + m2id)
        val epsilon = epsilonTable(m1id * dut.dim + m2id)

        val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)

        if(i == dut.dim / 4) {
            arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m1x, m1y, m1z, m1id * dut.dim + m2id, sigma6, epsilon, 0F, 0F, 0F, 0F)
        } else {
            arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, m1id * dut.dim + m2id, sigma6, epsilon, 0F, 0F, 0F, force)
        }
      }

      // input + read output
      for(i <- 0 until dut.dim / 2) {
        val entry = arr(i)

        dut.input.bits.molecule1.id.poke(entry.m1id)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(entry.m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(entry.m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(entry.m1z))

        dut.input.bits.molecule2.id.poke(entry.m2id)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(entry.m2x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(entry.m2y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(entry.m2z))
        dut.input.valid.poke(true)

        while(!dut.input.ready.peek().litToBoolean) {
            dut.clock.step(1) 
        }

        dut.clock.step(1)
        dut.input.valid.poke(false)

        while(!dut.output.valid.peek().litToBoolean) {
            dut.clock.step(1) 
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(i == dut.dim / 4)
        dut.output.bits.index.expect(entry.index)

        if(i != dut.dim / 4) {
            assert(Math.abs((entry.force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / entry.force) <= ERROR)
        }
      }
    }
  }

  "Testing CalculateForce module (pipelined)" in {
    test(new CalculateForce(dim=8, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.output.ready.poke(true)

      var sigma6Table = getTable(dut.dim)
      var epsilonTable = getTable(dut.dim)
      var arr = new Array[MoleculeInfo](dut.dim)

      // put values into LUTs
      for(i <- 0 until dut.dim) {
        for(j <- 0 until dut.dim) {
          val index = i * dut.dim + j
          dut.sigma6WriteIO.addr.poke(index)
          dut.sigma6WriteIO.data.bits.poke(decimal_to_floating32(sigma6Table(index)))
          dut.sigma6WriteIO.validIn.poke(true)

          dut.epsilonWriteIO.addr.poke(index)
          dut.epsilonWriteIO.data.bits.poke(decimal_to_floating32(epsilonTable(index)))
          dut.epsilonWriteIO.validIn.poke(true)

          dut.clock.step(1)
        }
      }

      dut.sigma6WriteIO.validIn.poke(false)
      dut.epsilonWriteIO.validIn.poke(false)
      dut.sigma6WriteIO.tableReady.poke(true)
      dut.epsilonWriteIO.tableReady.poke(true)
      dut.clock.step(1)

      // send input
      for(i <- 0 until dut.dim / 2) {
        val m1id = i * 2
        val m1x = get_float()
        val m1y = get_float()
        val m1z = get_float()

        val m2id = i * 2 + 1
        val m2x = get_float()
        val m2y = get_float()
        val m2z = get_float()

        val sigma6 = sigma6Table(m1id * dut.dim + m2id)
        val epsilon = epsilonTable(m1id * dut.dim + m2id)

        val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)

        if(i == dut.dim / 4) {
            arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m1x, m1y, m1z, m1id * dut.dim + m2id, sigma6, epsilon, 0F, 0F, 0F, 0F)
        } else {
            arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, m1id * dut.dim + m2id, sigma6, epsilon, 0F, 0F, 0F, force)
        }

        val entry = arr(i)

        dut.input.bits.molecule1.id.poke(entry.m1id)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(entry.m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(entry.m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(entry.m1z))

        dut.input.bits.molecule2.id.poke(entry.m2id)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(entry.m2x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(entry.m2y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(entry.m2z))

        dut.input.valid.poke(true)

        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        dut.clock.step(1)
      }

      dut.input.valid.poke(false)

      // read output
      for(i <- 0 until dut.dim / 2) {
        var entry = arr(i)

        while(!dut.output.valid.peek().litToBoolean) {
            dut.clock.step(1) 
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(i == dut.dim / 4)
        dut.output.bits.index.expect(entry.index)

        if(i != dut.dim / 4) {
          assert(Math.abs((entry.force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / entry.force) <= ERROR)
        }

        dut.clock.step(1)
      }
    }
  }
}