// package potential

// import chisel3._
// import chiseltest._
// import org.scalatest.freespec.AnyFreeSpec
// import chisel3.experimental.BundleLiterals._

// class CalculateBufferSpec extends AnyFreeSpec with ChiselScalatestTester {

//     "Testing CalculateBuffer module" in {
//         test(new CalculateBuffer(dim=16, dataWidth=8, expWidth=8, sigWidth=24)) { dut =>
//             for(i <- 0 until 3) {
//                 while(!dut.input.ready.peek().litToBoolean) {
//                     dut.clock.step(1) 
//                 }


//                 dut.clock.step(1)
//                 dut.output.ready.poke(true)
//             }
//         }
//     }
// }