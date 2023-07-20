package potential

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._

class ThroughputSpec extends AnyFreeSpec with ChiselScalatestTester {

    val dim = 16
    val entries = dim / 2
    var sigma6Table = getTable(dim)
    var epsilonTable = getTable(dim)
    var arr = new Array[MoleculeInfo](entries)

    var moduleLatencies = new Array[Int](entries)
    var moduleThroughput = 0
    var dividerLatency = 0
    var dividerThroughput = 0

    for(i <- 0 until entries) {
        val m1id = i * 2
        val m1x = get_float()
        val m1y = get_float()
        val m1z = get_float()

        val m2id = i * 2 + 1
        val m2x = get_float()
        val m2y = get_float()
        val m2z = get_float()

        val sigma6 = sigma6Table(m1id * dim + m2id)
        val epsilon = epsilonTable(m1id * dim + m2id)

        val delx = m2x - m1x
        val dely = m2y - m1y
        val delz = m2z - m1z
        val rsq = delx * delx + dely * dely + delz * delz
        val sr2 = 1.0F / rsq

        val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)
        arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, m1id * dim + m2id, sigma6, epsilon, rsq, sr2, 0F, force)
    }

    "Testing throughput of CalculateBuffer module (20 x 20)" in {
        test(new CalculateBuffer(dim=dim, expWidth=8, sigWidth=24, entries=entries)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.output.ready.poke(true)

            // put values into LUTs
            for(i <- 0 until dim) {
                for(j <- 0 until dim) {
                    val index = i * dim + j
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
            for(i <- 0 until dut.entries) {
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
                    for(j <- 0 to i) {
                        moduleLatencies(j) += 1
                    }
                }

                dut.clock.step(1)
                for(j <- 0 to i) {
                    moduleLatencies(j) += 1
                }
            }

            dut.input.valid.poke(false)

            // read output
            for(i <- 0 until dut.entries) {
                while(!dut.output.valid.peek().litToBoolean) {
                    dut.clock.step(1)
                    moduleLatencies(i) += 1
                    moduleThroughput += 1
                }

                dut.output.valid.expect(true)
                dut.output.bits.error.expect(false)
                dut.output.bits.index.expect(arr(i).index)
                assert(Math.abs((arr(i).force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / arr(i).force) <= ERROR)

                println("i", i)
                println("Module latency", moduleLatencies(i))
                if(i != 0) {
                    println("Module throughput", moduleThroughput)
                }
                println("-----")

                dut.clock.step(1)
                moduleThroughput = 1
            }
        }
    }

    "Testing throughput of CalcSr2 module" in {
        test(new CalcSr2(dim=dim, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.output.ready.poke(true)

            for(i <- 0 until entries) {
                val entry = arr(i)

                val sigma6 = entry.sigma6
                val epsilon = entry.epsilon
                val rsq = entry.rsq
                val sr2 = (1.0 / rsq).toFloat
            }

            for(i <- 0 until entries) {
                val entry = arr(i)

                dut.input.bits.index.poke(entry.index)
                dut.input.bits.sigma6.bits.poke(decimal_to_floating32(entry.sigma6))
                dut.input.bits.epsilon.bits.poke(decimal_to_floating32(entry.epsilon))
                dut.input.bits.rsq.bits.poke(decimal_to_floating32(entry.rsq))

                dut.input.bits.error.poke(false)
                dut.input.valid.poke(true)

                dividerLatency = 0

                while(!dut.input.ready.peek().litToBoolean) {
                    dut.clock.step(1)
                    dividerLatency += 1
                    dividerThroughput += 1
                }

                dut.clock.step(1)
                dut.input.valid.poke(false)

                dividerLatency += 1
                dividerThroughput += 1

                while(!dut.output.valid.peek().litToBoolean) {
                    dut.clock.step(1) 
                    dividerLatency += 1
                    dividerThroughput += 1
                }

                dut.output.valid.expect(true)
                dut.output.bits.error.expect(false)

                dut.input.bits.sigma6.bits.expect(decimal_to_floating32(entry.sigma6))
                dut.input.bits.epsilon.bits.expect(decimal_to_floating32(entry.epsilon))
                dut.output.bits.index.expect(entry.index)

                assert(Math.abs((entry.sr2 - floating32_to_decimal(dut.output.bits.sr2.bits.peek().litValue.toLong)) / entry.sr2) <= ERROR)
                
                println("i", i)
                println("Divider latency", dividerLatency)
                println("Divider throughput", dividerThroughput)
                println("-----")
                dividerThroughput = 0
            }
        }
    }
}