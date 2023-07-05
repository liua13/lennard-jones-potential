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

// "Testing 4x4 table (pt 2)" in {
  //   test(new LUT(dim2=16, dataWidth=8)) { dut =>
  //     dut.input.initSource()
  //     dut.input.setSourceClock(dut.clock)
  //     dut.output.initSink()
  //     dut.output.setSinkClock(dut.clock)

  //     val testValues = for { i <- 0 until dut.dim2 } yield (i)
  //     val writeInputSeq = testValues.map{ i =>
  //       new LUTInputBundle(dim2=dut.dim2, dataWidth=dut.dataWidth).Lit(_.index -> i.U, _.write -> true.B, _.dataIn -> i.U) 
  //     }
  //     val writeResultSeq = testValues.map{ i =>
  //       new LUTOutputBundle(dim2=dut.dim2, dataWidth=dut.dataWidth).Lit(_.dataOut -> i.U) 
  //     }
  //     val readInputSeq = testValues.map{ i =>
  //       new LUTInputBundle(dim2=dut.dim2, dataWidth=dut.dataWidth).Lit(_.index -> i.U, _.write -> false.B, _.dataIn -> i.U) 
  //     }
  //     val readResultSeq = testValues.map{ i =>
  //       new LUTOutputBundle(dim2=dut.dim2, dataWidth=dut.dataWidth).Lit(_.dataOut -> i.U) 
  //     }

  //     // testing writes
  //     fork {
  //       val (seq1, seq2) = writeInputSeq.splitAt(writeInputSeq.length / 3)
  //       dut.input.enqueueSeq(seq1)
  //       dut.clock.step(2)
  //       dut.input.enqueueSeq(seq2)
  //     }.fork {
  //       val (seq1, seq2) = writeResultSeq.splitAt(writeResultSeq.length / 2)
  //       dut.output.expectDequeueSeq(seq1)
  //       dut.clock.step(2)
  //       dut.output.expectDequeueSeq(seq2)
  //     }.join()

  //     // testing reads
  //     fork {
  //       val (seq1, seq2) = readInputSeq.splitAt(readResultSeq.length / 4)
  //       dut.input.enqueueSeq(seq1)
  //       dut.clock.step(7)
  //       dut.input.enqueueSeq(seq2)
  //     }.fork {
  //       val (seq1, seq2) = readResultSeq.splitAt(readResultSeq.length / 2)
  //       dut.output.expectDequeueSeq(seq1)
  //       dut.clock.step(5)
  //       dut.output.expectDequeueSeq(seq2)
  //     }.join()
  //   }
  // }