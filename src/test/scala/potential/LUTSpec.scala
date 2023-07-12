package potential

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._

/**
  * sbt 'testOnly potential.LUTSpec'
  * sbt clean test
  */
class LUTSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Testing 4x4 table (standard loop)" in {
    test(new LUT(dim=4, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until dut.dim * dut.dim) { 
        dut.writeIO.validIn.poke(true)
        dut.writeIO.addr.poke(i)
        dut.writeIO.data.bits.poke(i)
        dut.clock.step(1)
      }
      
      for(i <- 0 until dut.dim * dut.dim) { 
        dut.readIO.addr.poke(i)
        dut.clock.step(1)
        dut.readIO.data.bits.expect(i)
      }
    }
  }
}