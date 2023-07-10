
package object potential {
  // doubles: exp=11 / sig=53
  // floats: exp=8 / sig=24
  val r = scala.util.Random
  val ERROR = 0.000001

  case class MoleculeInfo(
    m1id: scala.Int,
    m1x: scala.Float,
    m1y: scala.Float,
    m1z: scala.Float,
    m2id: scala.Int,
    m2x: scala.Float,
    m2y: scala.Float,
    m2z: scala.Float,
    sigma6: scala.Float,
    epsilon: scala.Float,
    rsq: scala.Float,
    sr2: scala.Float,
    sr6: scala.Float,
    force: scala.Float
  )

  def get_double(): scala.Double = {
    var v = 0.0
    while(v == 0) {
      v = r.nextDouble() * r.nextInt(100)
    }
    return v
  }

  def get_float(): scala.Float = {
    var v = 0.0F
    while(v == 0) {
      v = (r.nextFloat() * r.nextInt(100)).toFloat
    }
    return v
  }

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
      case "100" => 0 // if(mantissa(22) == '1') 1 else 0 // 0
      case "101" => 1
      case "110" => 1
      case "111" => 1
      case _ => 0
    }
    bits += roundUp

    return bits
  }

  def floating32_to_decimal(num: scala.Long): Double = {
    if(num == 0) {
      return 0
    }
    
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
  def calc(m1x: scala.Float, m1y: scala.Float, m1z: scala.Float, m2x: scala.Float, m2y: scala.Float, m2z: scala.Float, sigma6: scala.Float, epsilon: scala.Float): scala.Float = {
    val delx: scala.Float = m2x - m1x
    val dely: scala.Float = m2y - m1y
    val delz: scala.Float = m2z - m1z
    val rsq: scala.Float = delx * delx + dely * dely + delz * delz
    val sr2: scala.Float = 1.0F / rsq
    val sr6: scala.Float = sr2 * sr2 * sr2 * sigma6
    val force: scala.Float = 48.0F * sr6 * (sr6 - 0.5F) * sr2 * epsilon
    return force
  }

  // def calc(m1x: scala.Double, m1y: scala.Double, m1z: scala.Double, m2x: scala.Double, m2y: scala.Double, m2z: scala.Double, sigma6: scala.Double, epsilon: scala.Double): scala.Float = {
  //   val delx: scala.Float = m2x.toFloat - m1x.toFloat
  //   val dely: scala.Float = m2y.toFloat - m1y.toFloat
  //   val delz: scala.Float = m2z.toFloat - m1z.toFloat
  //   val rsq: scala.Float = delx * delx + dely * dely + delz * delz
  //   val sr2: scala.Float = 1.0F / rsq
  //   val sr6: scala.Float = sr2 * sr2 * sr2 * sigma6.toFloat
  //   val force: scala.Float = 48.0F * sr6 * (sr6 - 0.5F) * sr2 * epsilon.toFloat
  //   return force
  // }

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
}