package potential

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import potential.Arithmetic.FloatArithmetic._

/**
  * sbt 'testOnly potential.LUTSpec'
  * sbt clean test
  */
class MiniDividerSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Testing MiniDivider" in {
    test(new MiniDivider(expWidth=8, sigWidth=24)) { dut =>
      dut.input.bits.numerator.bits.poke(1065353216)
      dut.input.bits.denominator.bits.poke(1077936128)
      dut.input.valid.poke(true)

      while(!dut.input.ready.peek().litToBoolean) {
        dut.clock.step(1) 
      }

      while(!dut.output.valid.peek().litToBoolean) {
        dut.clock.step(1) 
        dut.input.valid.poke(false)
      }

      dut.output.valid.expect(true)
      dut.output.bits.numerator.bits.expect(1065353216)
      dut.output.bits.denominator.bits.expect(1077936128)
      dut.output.bits.data.bits.expect(1051372203)

      // ----------
      dut.input.bits.numerator.bits.poke(1065353216)
      dut.input.bits.denominator.bits.poke(1065353216)
      dut.input.valid.poke(true)

      while(!dut.input.ready.peek().litToBoolean) {
        dut.clock.step(1) 
      }

      while(!dut.output.valid.peek().litToBoolean) {
        dut.clock.step(1) 
        dut.input.valid.poke(false)
      }

      dut.output.valid.expect(true)
      dut.output.bits.numerator.bits.expect(1065353216)
      dut.output.bits.denominator.bits.expect(1065353216)
      dut.output.bits.data.bits.expect(1065353216)
    }
  }

  "Testing MiniDivider2" in {
    test(new MiniDivider2(expWidth=8, sigWidth=24)) { dut =>
      dut.input.bits.numerator.bits.poke(1065353216)
      dut.input.bits.denominator.bits.poke(1077936128)
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
      dut.output.bits.numerator.bits.expect(1065353216)
      dut.output.bits.denominator.bits.expect(1077936128)
      dut.output.bits.data.bits.expect(1051372203)

      // ----------
      dut.input.bits.numerator.bits.poke(1065353216)
      dut.input.bits.denominator.bits.poke(1065353216)
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
      dut.output.bits.numerator.bits.expect(1065353216)
      dut.output.bits.denominator.bits.expect(1065353216)
      dut.output.bits.data.bits.expect(1065353216)
    }
  }
}