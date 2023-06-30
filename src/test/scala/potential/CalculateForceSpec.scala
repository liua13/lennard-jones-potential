
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

    if(fractionPart == 0) {
      return "0" * 27
    }

    var i = 0
    var found = false // if the first one has been found yet 
    var foundIndex = 0

    while(i < 27) { // 27
      fractionPart *= 2

      if(fractionPart > 1) {
        result += "1"
        fractionPart -= 1L

        found = true
      } else {
        result += "0"
      }

      if(found) {
        i += 1
      }
    }

    return result
  }

  // converts decimal float to the bits in the 
  // 32 bit floating point representation with expWidth=8, sigWidth=24
  def decimal_to_floating32(num: scala.Double): Long = {
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
    val indexInWhole = whole.indexOf('1')
    val indexInFrac = frac.indexOf('1')
    var mantissa = if(indexInWhole != -1) (whole + frac).substring(indexInWhole + 1)
                  else                    (whole + frac).substring(whole.length() + indexInFrac + 1)

    for(i <- 22 to 0 by -1) {
      if(mantissa(22 - i) == '1') {
        bits += scala.math.pow(2, i).toLong
      }
    }

    // exponent (bits 30 to 23)
    val unadjustedExp = if(indexInWhole != -1)  whole.length() - 1 - indexInWhole 
                        else                    -1 * (indexInFrac + 1)
    var exponent = unadjustedExp + scala.math.pow(2, expWidth - 1).toLong - 1L

    for(i <- 7 to 0 by -1) {
      if(exponent % 2 == 1) {
        bits += scala.math.pow(2, 30 - i).toLong
      } 
      exponent /= 2
      exponent = exponent.toInt
    }

    // 1 (true) means round up, 0 (false) means round down (do nothing)
    var roundUp = mantissa.substring(23, 26) match {
      case "100" => if(mantissa(22) == '1') 1 else 0 // 0
      case "101" => 1
      case "110" => 1
      case "111" => 1
      case _ => 0
    }
    bits += roundUp

    return bits
  }

  def floating32_to_decimal(num: scala.Long): Double = {
    // floating 32 bit to binary
    var b = ""
    var n = num

    for(i <- 0 until 32) {
      b += n % 2
      n /= 2
      n = n.toLong
    }

    b = b.reverse
    
    // binary to decimal

    // exponent
    val exponent_bits = b.substring(1, 9).reverse
    var exponent = 0
    for(i <- 0 until 8) {
      if(exponent_bits(i) == '1') {
        exponent += Math.pow(2, i).toInt
      }
    }
    exponent -= 127

    // mantissa
    val mantissa_bits = b.substring(9, 32)
    var mantissa = 0.0
    for(i <- 0 until 23) {
      if(mantissa_bits(i) == '1') {
        mantissa += Math.pow(2, -1 * (i + 1))
      }
    }

    var result = (1 + mantissa) * Math.pow(2, exponent)
    if(b(0) == '1') result *= -1 // signed 

    return result
  }

  // calculates the force
  def calc(m1x: scala.Double, m1y: scala.Double, m1z: scala.Double, m2x: scala.Double, m2y: scala.Double, m2z: scala.Double, sigma6: scala.Double, epsilon: scala.Double): scala.Float = {
    val delx: scala.Float = m2x.toFloat - m1x.toFloat
    val dely: scala.Float = m2y.toFloat - m1y.toFloat
    val delz: scala.Float = m2z.toFloat - m1z.toFloat
    val rsq: scala.Float = delx * delx + dely * dely + delz * delz
    val sr2: scala.Float = 1.0F / rsq
    val sr6: scala.Float = sr2 * sr2 * sr2 * sigma6.toFloat
    val force: scala.Float = 48.0F * sr6 * (sr6 - 0.5F) * sr2 * epsilon.toFloat
    return force
  }

  def calc2(m1x: scala.Double, m1y: scala.Double, m1z: scala.Double, m2x: scala.Double, m2y: scala.Double, m2z: scala.Double, sigma6: scala.Double, epsilon: scala.Double): scala.Float = {
    val delx = m2x - m1x
    val dely = m2y - m1y
    val delz = m2z - m1z
    val rsq = delx * delx + dely * dely + delz * delz
    val sr2 = 1.0 / rsq
    val sr6 = sr2 * sr2 * sr2 * sigma6
    val force = 48.0 * sr6 * (sr6 - 0.5) * sr2 * epsilon
    return force.toFloat
  }

  // doubles: exp=11 / sig=53
  // floats: exp=8 / sig=24
  val r = scala.util.Random
  val ERROR = 0.000001

  "Testing conversion from decimal to floating 32 bit" in {
    assert(Math.abs(1072902963 - decimal_to_floating32(1.9)) <= 5)
    assert(Math.abs(1065353216 - decimal_to_floating32(1.0)) <= 5)
    assert(Math.abs(1073741824 - decimal_to_floating32(2.0)) <= 5)
    assert(Math.abs(1077936128 - decimal_to_floating32(3.0)) <= 5)
    assert(Math.abs(1087981486 - decimal_to_floating32(6.79)) <= 5)
    assert(Math.abs(3253096939L - decimal_to_floating32(-28.79)) <= 5)
    assert(Math.abs(1063675494 - decimal_to_floating32(0.9)) <= 5)
    assert(Math.abs(0 - decimal_to_floating32(0.0)) <= 5)
  }

  "Testing conversion from floating 32 bit to decimal" in {
    assert(Math.abs(1.9 - floating32_to_decimal(1072902963)) <= ERROR)
    assert(Math.abs(1.0 - floating32_to_decimal(1065353216)) <= ERROR)
    assert(Math.abs(2.0 - floating32_to_decimal(1073741824)) <= ERROR)
    assert(Math.abs(3.0 - floating32_to_decimal(1077936128)) <= ERROR)
    assert(Math.abs(6.79 - floating32_to_decimal(1087981486)) <= ERROR)
    assert(Math.abs(-28.79 - floating32_to_decimal(3253096939L)) <= ERROR)
    assert(Math.abs(0.9 - floating32_to_decimal(1063675494)) <= ERROR)
    assert(Math.abs(0.0 - floating32_to_decimal(0)) <= ERROR)
  }

  "Testing decimal to and from floating 32 bit conversion" in {
    assert(Math.abs(1.9 - floating32_to_decimal(decimal_to_floating32(1.9))) <= ERROR)
    assert(Math.abs(1.0 - floating32_to_decimal(decimal_to_floating32(1.0))) <= ERROR)
    assert(Math.abs(2.0 - floating32_to_decimal(decimal_to_floating32(2.0))) <= ERROR)
    assert(Math.abs(3.0 - floating32_to_decimal(decimal_to_floating32(3.0))) <= ERROR)
    assert(Math.abs(6.79 - floating32_to_decimal(decimal_to_floating32(6.79))) <= ERROR)
    assert(Math.abs(-28.79 - floating32_to_decimal(decimal_to_floating32(-28.79))) <= ERROR)
    assert(Math.abs(0.9 - floating32_to_decimal(decimal_to_floating32(0.9))) <= ERROR)
    assert(Math.abs(0.0 - floating32_to_decimal(decimal_to_floating32(0))) <= ERROR)
  }

  "Testing Initialize module" in {
    test(new Initialize(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val m1x = (r.nextDouble() * r.nextInt(100)).toFloat
        val m1y = (r.nextDouble() * r.nextInt(100)).toFloat
        val m1z = (r.nextDouble() * r.nextInt(100)).toFloat

        val m2x = (r.nextDouble() * r.nextInt(100)).toFloat
        val m2y = (r.nextDouble() * r.nextInt(100)).toFloat
        val m2z = (r.nextDouble() * r.nextInt(100)).toFloat

        dut.input.bits.molecule1.id.poke(1)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

        dut.input.bits.molecule2.id.poke(2)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(false)

        dut.output.bits.molecule1.x.bits.expect(decimal_to_floating32(m1x))
        dut.input.bits.molecule1.y.bits.expect(decimal_to_floating32(m1y))
        dut.input.bits.molecule1.z.bits.expect(decimal_to_floating32(m1z))

        dut.input.bits.molecule2.x.bits.expect(decimal_to_floating32(m2x))
        dut.input.bits.molecule2.y.bits.expect(decimal_to_floating32(m2y))
        dut.input.bits.molecule2.z.bits.expect(decimal_to_floating32(m2z))
      
        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing Initialize module (invalid input)" in {
    test(new Initialize(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val m1x = (r.nextDouble() * r.nextInt(100)).toFloat
        val m1y = (r.nextDouble() * r.nextInt(100)).toFloat
        val m1z = (r.nextDouble() * r.nextInt(100)).toFloat
        
        dut.input.bits.molecule1.id.poke(1)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

        dut.input.bits.molecule2.id.poke(2)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m1z))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(true)

        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalcRsq module" in {
    test(new CalcRsq(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val m1x = (r.nextDouble() * r.nextInt(100)).toFloat
        val m1y = (r.nextDouble() * r.nextInt(100)).toFloat
        val m1z = (r.nextDouble() * r.nextInt(100)).toFloat

        val m2x = (r.nextDouble() * r.nextInt(100)).toFloat
        val m2y = (r.nextDouble() * r.nextInt(100)).toFloat
        val m2z = (r.nextDouble() * r.nextInt(100)).toFloat

        val delx = (m2x.toFloat - m1x.toFloat).toFloat
        val dely = (m2y.toFloat - m1y.toFloat).toFloat
        val delz = (m2z.toFloat - m1z.toFloat).toFloat
        val rsq = (delx * delx + dely * dely + delz * delz).toFloat

        val sigma6 = r.nextDouble() * r.nextInt(100)
        val epsilon = r.nextDouble() * r.nextInt(100)
        
        dut.input.bits.error.poke(false)
        dut.input.bits.molecule1.id.poke(1)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

        dut.input.bits.molecule2.id.poke(2)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))
        
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(epsilon))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(false)
        dut.input.bits.sigma6.bits.expect(decimal_to_floating32(sigma6))
        dut.input.bits.epsilon.bits.expect(decimal_to_floating32(epsilon))
        assert(Math.abs(rsq - floating32_to_decimal(dut.output.bits.rsq.bits.peek().litValue.toLong)) <= ERROR)

        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalcRsq module (invalid input)" in {
    test(new CalcRsq(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val m1x = (r.nextDouble() * r.nextInt(100)).toFloat
        val m1y = (r.nextDouble() * r.nextInt(100)).toFloat
        val m1z = (r.nextDouble() * r.nextInt(100)).toFloat

        val sigma6 = (r.nextDouble() * r.nextInt(100)).toFloat
        val epsilon = (r.nextDouble() * r.nextInt(100)).toFloat
        
        dut.input.bits.error.poke(true)
        dut.input.bits.molecule1.id.poke(1)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

        dut.input.bits.molecule2.id.poke(2)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m1z))
        
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(epsilon))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(true)

        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalcSr2 module" in {
    test(new CalcSr2(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val sigma6 = (r.nextDouble() * r.nextInt(100)).toFloat
        val epsilon = (r.nextDouble() * r.nextInt(100)).toFloat
        var rsq = 0.0
        while(rsq == 0) {
          rsq = (r.nextDouble() * r.nextInt(100)).toFloat
        }
        val sr2 = (1.0 / rsq).toFloat
        
        dut.input.bits.error.poke(false)
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(epsilon))
        dut.input.bits.rsq.bits.poke(decimal_to_floating32(rsq))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(false)
        dut.output.bits.sigma6.bits.expect(decimal_to_floating32(sigma6))
        dut.output.bits.epsilon.bits.expect(decimal_to_floating32(epsilon))
        assert(Math.abs(sr2 - floating32_to_decimal(dut.output.bits.sr2.bits.peek().litValue.toLong)) <= ERROR)
        
        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalcSr2 module (invalid input)" in {
    test(new CalcSr2(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val sigma6 = (r.nextDouble() * r.nextInt(100)).toFloat
        val epsilon = (r.nextDouble() * r.nextInt(100)).toFloat
        val rsq = (r.nextDouble() * r.nextInt(100)).toFloat
        
        dut.input.bits.error.poke(true)
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(epsilon))
        dut.input.bits.rsq.bits.poke(decimal_to_floating32(rsq))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(true)

        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalcSr6 module" in {
    test(new CalcSr6(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val sigma6 = (r.nextDouble() * r.nextInt(100)).toFloat
        val epsilon = (r.nextDouble() * r.nextInt(100)).toFloat
        val sr2 = (r.nextDouble() * r.nextInt(100)).toFloat
        val sr6 = (sr2 * sr2 * sr2 * sigma6.toFloat).toFloat
        
        dut.input.bits.error.poke(false)
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(epsilon))
        dut.input.bits.sr2.bits.poke(decimal_to_floating32(sr2))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(false)
        dut.output.bits.epsilon.bits.expect(decimal_to_floating32(epsilon))
        dut.output.bits.sr2.bits.expect(decimal_to_floating32(sr2))

        assert(Math.abs(sr6 - floating32_to_decimal(dut.output.bits.sr6.bits.peek().litValue.toLong)) <= ERROR)
        
        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalcSr6 module (invalid input)" in {
    test(new CalcSr6(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val sigma6 = (r.nextDouble() * r.nextInt(100)).toFloat
        val epsilon = (r.nextDouble() * r.nextInt(100)).toFloat
        val sr2 = (r.nextDouble() * r.nextInt(100)).toFloat
        
        dut.input.bits.error.poke(true)
        dut.input.bits.sigma6.bits.poke(decimal_to_floating32(sigma6))
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(epsilon))
        dut.input.bits.sr2.bits.poke(decimal_to_floating32(sr2))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(true)
        
        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalcForce module" in {
    test(new CalcForce(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val sigma6 = (r.nextDouble() * r.nextInt(100)).toFloat
        val epsilon = (r.nextDouble() * r.nextInt(100)).toFloat
        val sr2 = (r.nextDouble() * r.nextInt(100)).toFloat
        val sr6 = (sr2 * sr2 * sr2 * sigma6.toFloat).toFloat
        val force = (48.0F * sr6 * (sr6 - 0.5F) * sr2 * epsilon.toFloat).toFloat
        
        dut.input.bits.error.poke(false)
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(epsilon))
        dut.input.bits.sr2.bits.poke(decimal_to_floating32(sr2))
        dut.input.bits.sr6.bits.poke(decimal_to_floating32(sr6))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(false)

        assert(Math.abs(force - floating32_to_decimal(dut.output.bits.force.bits.peek().litValue.toLong)) <= ERROR)
        
        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalcForce module (invalid input)" in {
    test(new CalcForce(dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val sigma6 = (r.nextDouble() * r.nextInt(100)).toFloat
        val epsilon = (r.nextDouble() * r.nextInt(100)).toFloat
        val sr2 = (r.nextDouble() * r.nextInt(100)).toFloat
        val sr6 = (sr2 * sr2 * sr2 * sigma6.toFloat).toFloat
        
        dut.input.bits.error.poke(true)
        dut.input.bits.epsilon.bits.poke(decimal_to_floating32(epsilon))
        dut.input.bits.sr2.bits.poke(decimal_to_floating32(sr2))
        dut.input.bits.sr6.bits.poke(decimal_to_floating32(sr6))
        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(true)
        
        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalculateForce module" in {
    test(new CalculateForce(dim=4, dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val m1id = 1
        val m1x = r.nextDouble() * r.nextInt(100)
        val m1y = r.nextDouble() * r.nextInt(100)
        val m1z = r.nextDouble() * r.nextInt(100)

        val m2id = 2
        val m2x = r.nextDouble() * r.nextInt(100)
        val m2y = r.nextDouble() * r.nextInt(100)
        val m2z = r.nextDouble() * r.nextInt(100)

        val sigma6 = r.nextDouble() * r.nextInt(100)
        val epsilon = r.nextDouble() * r.nextInt(100)

        val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)

        dut.sigma6WriteIO.addr.poke(m1id * dut.dim + m2id)
        dut.sigma6WriteIO.data.bits.poke(decimal_to_floating32(sigma6.toFloat))
        dut.sigma6WriteIO.validIn.poke(true)

        dut.epsilonWriteIO.addr.poke(m1id * dut.dim + m2id)
        dut.epsilonWriteIO.data.bits.poke(decimal_to_floating32(epsilon.toFloat))
        dut.epsilonWriteIO.validIn.poke(true)

        dut.clock.step(1)
        
        dut.sigma6WriteIO.validIn.poke(false)
        dut.epsilonWriteIO.validIn.poke(false)

        // ------
      
        dut.input.bits.molecule1.id.poke(m1id)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

        dut.input.bits.molecule2.id.poke(m2id)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))

        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(false)

        assert(Math.abs(force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) <= ERROR)
        
        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }

  "Testing CalculateForce module (invalid input)" in {
    test(new CalculateForce(dim=4, dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
      for(i <- 0 until 3) {
        while(!dut.input.ready.peek().litToBoolean) {
          dut.clock.step(1) 
        }

        val m1id = 1
        val m2id = 2
        val m1x = r.nextDouble() * r.nextInt(100)
        val m1y = r.nextDouble() * r.nextInt(100)
        val m1z = r.nextDouble() * r.nextInt(100)

        val sigma6 = r.nextDouble() * r.nextInt(100)
        val epsilon = r.nextDouble() * r.nextInt(100)

        dut.sigma6WriteIO.addr.poke(m1id * dut.dim + m2id)
        dut.sigma6WriteIO.data.bits.poke(decimal_to_floating32(sigma6.toFloat))
        dut.sigma6WriteIO.validIn.poke(true)

        dut.epsilonWriteIO.addr.poke(m1id * dut.dim + m2id)
        dut.epsilonWriteIO.data.bits.poke(decimal_to_floating32(epsilon.toFloat))
        dut.epsilonWriteIO.validIn.poke(true)

        dut.clock.step(1)

        dut.sigma6WriteIO.validIn.poke(false)
        dut.epsilonWriteIO.validIn.poke(false)

        // ------
        
        dut.input.bits.molecule1.id.poke(m1id)
        dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

        dut.input.bits.molecule2.id.poke(m2id)
        dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m1x))
        dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m1y))
        dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m1z))

        dut.input.valid.poke(true)

        while(!dut.output.valid.peek().litToBoolean) {
          dut.output.bits.error.expect(false)
          dut.clock.step(1)
          dut.input.valid.poke(false)
        }

        dut.output.valid.expect(true)
        dut.output.bits.error.expect(true)

        dut.clock.step(1)
        dut.output.ready.poke(true)
      }
    }
  }
}