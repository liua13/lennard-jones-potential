
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

  // gets the whole part of the number
  def whole_mantissa(num: scala.Double): String = {
    var result = ""
    var wholePart = num.toInt.abs

    while(wholePart > 0) {
      result += (wholePart % 2).toString
      wholePart /= 2
    }

    return result.reverse
  }

  // gets the fraction part of the number
  def frac_mantissa(num: scala.Double): String = {
    var result = ""
    var wholePart = num.toInt.abs
    var fractionPart = num.toDouble.abs - wholePart.toDouble

    for(i <- 0 to 27) {
      fractionPart *= 2

      if(fractionPart > 1) {
        result += "1"
        fractionPart -= 1L
      } else {
        result += "0"
      }
    }

    return result
  }

  // converts decimal float to the bits in the 
  // 32 bit floating point representation with expWidth=8, sigWidth=24
  def decimal_to_32_bit(num: scala.Double): Long = {
    if(num == 0) {
      return 0L
    }

    val expWidth = 8
    var bits = 0L

    // signed (bit 31)
    if(num < 0) {
      bits += scala.math.pow(2, 31).toLong
    } 

    // mantissa (bits 22 to 0)
    var whole = whole_mantissa(num)
    var frac = frac_mantissa(num)
    var mantissa = (whole + frac).substring(1)
    
    for(i <- 22 to 0 by -1) {
      if(mantissa(22 - i) == '1') {
        bits += scala.math.pow(2, i).toLong
      }
    }

    // exponent (bits 30 to 23)
    var unadjustedExp = 0 // index of the first 1 in the mantissa
    var i = 0
    var found = false

    // try to find first one in the whole part of mantissa
    while(i < whole.length() && !found) {
      if(whole(i) == '1') {
        unadjustedExp = whole.length() - i - 1
        found = true
      }
      i += 1
    }

    // try to find first one in the fraction part of mantissa
    if(!found) {
      i = 0
      while(i < frac.length() && !found) {
        if(frac(i) == '1') {
          unadjustedExp = -1 * (i + 1)
          found = true
        }
        i += 1
      }
    }

    var exponent = unadjustedExp + scala.math.pow(2, expWidth - 1).toLong - 1L
    for(i <- 7 to 0 by -1) {
      if(exponent % 2 == 1) {
        bits += scala.math.pow(2, 30 - i).toLong
      } 
      exponent /= 2
      exponent = exponent.toInt
    }

    // true means round up, false means round down (do nothing)
    var roundUp = mantissa.substring(23, 26) match {
      case "100" => false // if(mantissa(22) == '1') true else false
      case "101" => true
      case "110" => true
      case "111" => true
      case _ => false
    }

    if(roundUp) {
      bits += 1
    }

    return bits
  }

  def calc(m1x: scala.Double, m1y: scala.Double, m1z: scala.Double, m2x: scala.Double, m2y: scala.Double, m2z: scala.Double, sigma6: scala.Double, epsilon: scala.Double): scala.Float = {
    val delx = m2x.toFloat - m1x.toFloat
    val dely = m2y.toFloat - m1y.toFloat
    val delz = m2z.toFloat - m1z.toFloat
    val rsq = delx * delx + dely * dely + delz * delz
    val sr2 = 1.0 / rsq
    val sr6 = sr2 * sr2 * sr2 * sigma6.toFloat
    val force = 48.0 * sr6 * (sr6 - 0.5F) * sr2 * epsilon.toFloat
    return force.toFloat
  }

  "Testing basic calc" in {
    // 11 53 for doubles
    // 8 24 for floats
    // 32 point floating point representation
    
    // test conversion from decimal to 32 point floating point
    assert(decimal_to_32_bit(1.9) == 1072902963)
    assert(decimal_to_32_bit(1.0) == 1065353216)
    assert(decimal_to_32_bit(2.0) == 1073741824)
    assert(decimal_to_32_bit(3.0) == 1077936128)
    assert(decimal_to_32_bit(6.79) == 1087981486)
    assert(decimal_to_32_bit(-28.79) == 3253096939L)
    assert(decimal_to_32_bit(0.9) == 1063675494)
    assert(decimal_to_32_bit(0) == 0)

    test(new CalculateForce(log2dim=2, dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
        val m1x = 1.8
        val m1y = 1
        val m1z = 1

        val m2x = 0
        val m2y = 0.9
        val m2z = 0

        val sigma6 = 3
        val epsilon = 3.9
        val expectedForce = decimal_to_32_bit(calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon))

        dut.io.molecule1.id.poke(1)
        dut.io.molecule1.x.bits.poke(decimal_to_32_bit(m1x))
        dut.io.molecule1.y.bits.poke(decimal_to_32_bit(m1y))
        dut.io.molecule1.z.bits.poke(decimal_to_32_bit(m1z))

        dut.io.molecule2.id.poke(2)
        dut.io.molecule2.x.bits.poke(decimal_to_32_bit(m2x))
        dut.io.molecule2.y.bits.poke(decimal_to_32_bit(m2y))
        dut.io.molecule2.z.bits.poke(decimal_to_32_bit(m2z))

        dut.io.sigma6Table.validOut.poke(true)
        dut.io.sigma6Table.dataOut.bits.poke(decimal_to_32_bit(sigma6))
        dut.io.epsilonTable.validOut.poke(true)
        dut.io.epsilonTable.dataOut.bits.poke(decimal_to_32_bit(epsilon))
        dut.io.validIn.poke(true)
        
        while(!dut.io.validOut.peek().litToBoolean) {
            dut.io.errorOut.expect(false)
            dut.clock.step(1)
        }

        dut.io.validOut.expect(true)
        // assert(dut.io.dataOut.bits.peek() == expectedForce || dut.io.dataOut.bits.peek() + 1.U == expectedForce)
        dut.io.dataOut.bits.expect(expectedForce)
    }
  }
}
