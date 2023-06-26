// package potential

// import chisel3._
// import chiseltest._
// import org.scalatest.freespec.AnyFreeSpec
// import chisel3.experimental.BundleLiterals._
// import potential.Arithmetic.FloatArithmetic._
// import chisel3.iotesters.{PeekPokeTester}

// /**
//   * sbt 'testOnly potential.LUTSpec'
//   * sbt clean test
//   */
// class ArithmeticSpec(f: FloatArithmetic) extends PeekPokeTester(f) {
//   // val num = Float(expWidth=8, sigWidth=24)
//   // // num.bits := 1065353216.U
//   // num.bits.poke(1065353216)
//   // val denom = Float(expWidth=8, sigWidth=24)
//   // // denom.bits := 1065353216.U
//   // denom.bits.poke(1065353216)

//   // val result = (num / denom).get
//   // assert(result.valid == true.B)
//   // assert(result.bits.bits == num.bits)

//   // "Testing basic division" in {
//   //   // 11 53 for doubles
//   //   // 8 24 for floats
//   //   // 32 point floating point representation
//   //   test(new Float(expWidth=8, sigWidth=24)) { dut =>
//   //     dut.bits := 1065353216.U

//   //     val denom = Float(expWidth=8, sigWidth=24)
//   //     denom.bits := 1065353216.U

//   //     val result = dut / denom
//   //     assert(result.valid)
//   //     assert(result.bits.bits == dut.bits)
//   //   }
//   // }
// }