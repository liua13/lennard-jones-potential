package potential

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._

class CalculateBufferSpec extends AnyFreeSpec with ChiselScalatestTester {
    "Testing CalculateBuffer module (2 x 2)" in {
        test(new CalculateBuffer(dim=2, expWidth=8, sigWidth=24, entries=1)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
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
                arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, m1id * dut.dim + m2id, sigma6, epsilon, 0F, 0F, 0F, force)

                dut.input.bits.molecule1.id.poke(m1id)
                dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
                dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
                dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

                dut.input.bits.molecule2.id.poke(m2id)
                dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
                dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
                dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))
                dut.input.valid.poke(true)

                while(!dut.input.ready.peek().litToBoolean) {
                    dut.clock.step(1) 
                }

                dut.clock.step(1)
            }

            dut.input.valid.poke(false)

            // read output
            for(i <- 0 until dut.dim / 2) {
                while(!dut.output.valid.peek().litToBoolean) {
                    dut.clock.step(1)
                }

                dut.output.valid.expect(true)
                dut.output.bits.error.expect(false)
                dut.output.bits.index.expect(arr(i).index)
                assert(Math.abs((arr(i).force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / arr(i).force) <= ERROR)

                dut.clock.step(1)
            }
        }
    }

    "Testing CalculateBuffer module (4 x 4)" in {
        test(new CalculateBuffer(dim=4, expWidth=8, sigWidth=24, entries=2)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
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

                val index = m1id * dut.dim + m2id
                val sigma6 = sigma6Table(index)
                val epsilon = epsilonTable(index)

                val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)
                arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, index, sigma6, epsilon, 0F, 0F, 0F, force)

                dut.input.bits.molecule1.id.poke(m1id)
                dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
                dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
                dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

                dut.input.bits.molecule2.id.poke(m2id)
                dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
                dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
                dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))
                dut.input.valid.poke(true)

                while(!dut.input.ready.peek().litToBoolean) {
                    dut.clock.step(1) 
                }

                dut.clock.step(1)
            }

            dut.input.valid.poke(false)

            // read output
            for(i <- 0 until dut.dim / 2) {
                while(!dut.output.valid.peek().litToBoolean) {
                    dut.clock.step(1)
                }

                dut.output.valid.expect(true)
                dut.output.bits.error.expect(false)
                
                dut.output.bits.index.expect(arr(i).index)
                assert(Math.abs((arr(i).force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / arr(i).force) <= ERROR)

                dut.clock.step(1)
            }
        }
    }

    "Testing CalculateBuffer module (8 x 8)" in {
        test(new CalculateBuffer(dim=8, expWidth=8, sigWidth=24, entries=4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
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
                while(!dut.input.ready.peek().litToBoolean) {
                    dut.clock.step(1) 
                }

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
                arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, m1id * dut.dim + m2id, sigma6, epsilon, 0F, 0F, 0F, force)

                dut.input.bits.molecule1.id.poke(m1id)
                dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
                dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
                dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

                dut.input.bits.molecule2.id.poke(m2id)
                dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
                dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
                dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))
                dut.input.valid.poke(true)

                dut.clock.step(1)
            }

            dut.input.valid.poke(false)

            // read output
            for(i <- 0 until dut.dim / 2) {
                while(!dut.output.valid.peek().litToBoolean) {
                    dut.output.bits.error.expect(false)
                    dut.clock.step(1)
                }

                dut.output.valid.expect(true)
                dut.output.bits.error.expect(false)
                dut.output.bits.index.expect(arr(i).index)
                assert(Math.abs((arr(i).force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / arr(i).force) <= ERROR)

                dut.clock.step(1)
            }
        }
    }
}
