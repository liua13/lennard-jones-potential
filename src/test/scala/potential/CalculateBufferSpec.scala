// package potential

// import chisel3._
// import chiseltest._
// import org.scalatest.freespec.AnyFreeSpec
// import chisel3.experimental.BundleLiterals._

// class CalculateBufferSpec extends AnyFreeSpec with ChiselScalatestTester {

//     "Testing CalculateBuffer module" in {
//         test(new CalculateBuffer(dim=8, expWidth=8, sigWidth=24)) { dut =>
//             var arr = new Array[MoleculeInfo](dut.dim / 2)

//             for(i <- 0 until dut.dim by 2) {
//                 while(!dut.input.ready.peek().litToBoolean) {
//                     dut.clock.step(1) 
//                 }

//                 val m1id = i
//                 val m1x = get_float()
//                 val m1y = get_float()
//                 val m1z = get_float()

//                 val m2id = i + 1
//                 val m2x = get_float()
//                 val m2y = get_float()
//                 val m2z = get_float()

//                 val sigma6 = get_float()
//                 val epsilon = get_float()

//                 val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)

//                 dut.calculateForce.sigma6WriteIO.addr.poke(m1id * dut.dim + m2id)
//                 dut.calculateForce.sigma6WriteIO.data.bits.poke(decimal_to_floating32(sigma6))
//                 dut.calculateForce.sigma6WriteIO.validIn.poke(true)

//                 dut.calculateForce.epsilonWriteIO.addr.poke(m1id * dut.dim + m2id)
//                 dut.calculateForce.epsilonWriteIO.data.bits.poke(decimal_to_floating32(epsilon))
//                 dut.calculateForce.epsilonWriteIO.validIn.poke(true)

//                 dut.clock.step(1)
                
//                 dut.calculateForce.sigma6WriteIO.validIn.poke(false)
//                 dut.calculateForce.epsilonWriteIO.validIn.poke(false)

//                 // ------

//                 dut.input.bits.molecule1.id.poke(m1id)
//                 dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
//                 dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
//                 dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

//                 dut.input.bits.molecule2.id.poke(m2id)
//                 dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
//                 dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
//                 dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))

//                 arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, sigma6, epsilon, force)

//                 dut.clock.step(1)
//                 // dut.output.ready.poke(true)
//             }

//             while(!dut.output.valid.peek().litToBoolean) {
//                 dut.output.bits.error.expect(false)
//                 dut.clock.step(1)
//                 dut.input.valid.poke(false)
//             }

//             for(i <- 0 until 4) {
//                 dut.output.valid.expect(true)
//                 dut.output.bits.error.expect(false)
//                 assert(Math.abs((arr(i).force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / arr(i).force) <= ERROR)

//                 dut.clock.step(1)
//                 dut.output.ready.poke(true)
//             }
//         }
//     }
// }