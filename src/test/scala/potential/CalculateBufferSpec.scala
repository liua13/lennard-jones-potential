// package potential

// import chisel3._
// import chiseltest._
// import org.scalatest.freespec.AnyFreeSpec
// import chisel3.experimental.BundleLiterals._

// class CalculateBufferSpec extends AnyFreeSpec with ChiselScalatestTester {
//     "Testing CalculateBuffer module (2 x 2)" in {
//         test(new CalculateBuffer(dim=2, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
//             dut.output.ready.poke(true)
//             var arr = new Array[MoleculeInfo](dut.dim / 2)

//             for(i <- 0 until dut.dim / 2) {
//                 while(!dut.input.ready.peek().litToBoolean) {
//                     dut.clock.step(1) 
//                 }

//                 val m1id = i * 2
//                 val m1x = get_float()
//                 val m1y = get_float()
//                 val m1z = get_float()

//                 val m2id = i * 2 + 1
//                 val m2x = get_float()
//                 val m2y = get_float()
//                 val m2z = get_float()

//                 val sigma6 = get_float()
//                 val epsilon = get_float()

//                 val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)

//                 dut.sigma6WriteIO.addr.poke(m1id * dut.dim + m2id)
//                 dut.sigma6WriteIO.data.bits.poke(decimal_to_floating32(sigma6))
//                 dut.sigma6WriteIO.validIn.poke(true)

//                 dut.epsilonWriteIO.addr.poke(m1id * dut.dim + m2id)
//                 dut.epsilonWriteIO.data.bits.poke(decimal_to_floating32(epsilon))
//                 dut.epsilonWriteIO.validIn.poke(true)

//                 dut.clock.step(1)
                
//                 dut.sigma6WriteIO.validIn.poke(false)
//                 dut.epsilonWriteIO.validIn.poke(false)

//                 // ------

//                 dut.input.bits.molecule1.id.poke(m1id)
//                 dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
//                 dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
//                 dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

//                 dut.input.bits.molecule2.id.poke(m2id)
//                 dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
//                 dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
//                 dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))
//                 dut.input.valid.poke(true)

//                 arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, sigma6, epsilon, force)

//                 dut.clock.step(1)
//                 dut.input.valid.poke(false)
//             }

//             for(i <- 0 until dut.dim / 2) {
//                 while(!dut.output.valid.peek().litToBoolean) {
//                     dut.output.bits.error.expect(false)
//                     dut.clock.step(1)
//                 }

//                 dut.output.valid.expect(true)
//                 dut.output.bits.error.expect(false)
//                 assert(Math.abs((arr(i).force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / arr(i).force) <= ERROR)

//                 dut.clock.step(1)
//             }
//         }
//     }

//     "Testing CalculateBuffer module (4 x 4)" in {
//         test(new CalculateBuffer(dim=4, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
//             dut.output.ready.poke(true)
//             var arr = new Array[MoleculeInfo](dut.dim / 2)

//             for(i <- 0 until dut.dim / 2) {
//                 while(!dut.input.ready.peek().litToBoolean) {
//                     dut.clock.step(1) 
//                 }

//                 val m1id = i * 2
//                 val m1x = get_float()
//                 val m1y = get_float()
//                 val m1z = get_float()

//                 val m2id = i * 2 + 1
//                 val m2x = get_float()
//                 val m2y = get_float()
//                 val m2z = get_float()

//                 val sigma6 = get_float()
//                 val epsilon = get_float()

//                 val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)

//                 dut.sigma6WriteIO.addr.poke(m1id * dut.dim + m2id)
//                 dut.sigma6WriteIO.data.bits.poke(decimal_to_floating32(sigma6))
//                 dut.sigma6WriteIO.validIn.poke(true)

//                 dut.epsilonWriteIO.addr.poke(m1id * dut.dim + m2id)
//                 dut.epsilonWriteIO.data.bits.poke(decimal_to_floating32(epsilon))
//                 dut.epsilonWriteIO.validIn.poke(true)

//                 dut.clock.step(1)
                
//                 dut.sigma6WriteIO.validIn.poke(false)
//                 dut.epsilonWriteIO.validIn.poke(false)

//                 // ------

//                 dut.input.bits.molecule1.id.poke(m1id)
//                 dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
//                 dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
//                 dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

//                 dut.input.bits.molecule2.id.poke(m2id)
//                 dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
//                 dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
//                 dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))
//                 dut.input.valid.poke(true)

//                 arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, sigma6, epsilon, force)

//                 dut.clock.step(1)
//                 dut.input.valid.poke(false)
//             }

//             for(i <- 0 until dut.dim / 2) {
//                 while(!dut.output.valid.peek().litToBoolean) {
//                     dut.output.bits.error.expect(false)
//                     dut.clock.step(1)
//                 }

//                 dut.output.valid.expect(true)
//                 dut.output.bits.error.expect(false)
//                 assert(Math.abs((arr(i).force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / arr(i).force) <= ERROR)

//                 dut.clock.step(1)
//             }
//         }
//     }

//     "Testing CalculateBuffer module (8 x 8)" in {
//         test(new CalculateBuffer(dim=8, expWidth=8, sigWidth=24)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
//             dut.output.ready.poke(true)
//             var arr = new Array[MoleculeInfo](dut.dim / 2)

//             for(i <- 0 until dut.dim / 2) {
//                 while(!dut.input.ready.peek().litToBoolean) {
//                     dut.clock.step(1) 
//                 }

//                 val m1id = i * 2
//                 val m1x = get_float()
//                 val m1y = get_float()
//                 val m1z = get_float()

//                 val m2id = i * 2 + 1
//                 val m2x = get_float()
//                 val m2y = get_float()
//                 val m2z = get_float()

//                 val sigma6 = get_float()
//                 val epsilon = get_float()

//                 val force = calc(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon)

//                 dut.sigma6WriteIO.addr.poke(m1id * dut.dim + m2id)
//                 dut.sigma6WriteIO.data.bits.poke(decimal_to_floating32(sigma6))
//                 dut.sigma6WriteIO.validIn.poke(true)

//                 dut.epsilonWriteIO.addr.poke(m1id * dut.dim + m2id)
//                 dut.epsilonWriteIO.data.bits.poke(decimal_to_floating32(epsilon))
//                 dut.epsilonWriteIO.validIn.poke(true)

//                 dut.clock.step(1)
                
//                 dut.sigma6WriteIO.validIn.poke(false)
//                 dut.epsilonWriteIO.validIn.poke(false)

//                 // ------

//                 dut.input.bits.molecule1.id.poke(m1id)
//                 dut.input.bits.molecule1.x.bits.poke(decimal_to_floating32(m1x))
//                 dut.input.bits.molecule1.y.bits.poke(decimal_to_floating32(m1y))
//                 dut.input.bits.molecule1.z.bits.poke(decimal_to_floating32(m1z))

//                 dut.input.bits.molecule2.id.poke(m2id)
//                 dut.input.bits.molecule2.x.bits.poke(decimal_to_floating32(m2x))
//                 dut.input.bits.molecule2.y.bits.poke(decimal_to_floating32(m2y))
//                 dut.input.bits.molecule2.z.bits.poke(decimal_to_floating32(m2z))
//                 dut.input.valid.poke(true)

//                 arr(i) = MoleculeInfo(m1id, m1x, m1y, m1z, m2id, m2x, m2y, m2z, sigma6, epsilon, force)

//                 dut.clock.step(1)
//                 dut.input.valid.poke(false)
//             }

//             for(i <- 0 until dut.dim / 2) {
//                 while(!dut.output.valid.peek().litToBoolean) {
//                     dut.output.bits.error.expect(false)
//                     dut.clock.step(1)
//                 }

//                 dut.output.valid.expect(true)
//                 dut.output.bits.error.expect(false)
//                 assert(Math.abs((arr(i).force - floating32_to_decimal(dut.output.bits.data.bits.peek().litValue.toLong)) / arr(i).force) <= ERROR)

//                 dut.clock.step(1)
//             }
//         }
//     }
// }