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

  "Testing 4x4 table" in {
    test(new LUT(dim2=16, dataWidth=8)) { dut =>
      for(i <- 0 until dut.dim2) { 
        dut.io.validIn.poke(1)
        dut.io.index.poke(i)
        dut.io.dataIn.poke(i)
        dut.io.write.poke(true)
        dut.clock.step(1)
        // dut.io.validOut.expect(true)
      }
      
      for(i <- 0 until dut.dim2) { 
        dut.io.validIn.poke(1)
        dut.io.index.poke(i)
        // dut.io.dataIn.poke(i)
        dut.io.write.poke(false)
        dut.clock.step(1)
        dut.io.dataOut.expect(i)
        dut.io.validOut.expect(true)
      }
    }
  }

  "Testing 4x4 table (decoupled)" in {
    test(new LUTDecoupled(dim2=16, dataWidth=8)) { dut =>
      
      for(i <- 0 until dut.dim2) { 
        // pass in values, input valid and output signal not ready
        dut.input.bits.index.poke(i)
        dut.input.bits.dataIn.poke(i)
        dut.input.bits.write.poke(true)
        dut.input.valid.poke(true)
        dut.output.ready.poke(false)

        dut.clock.step(1)

        // // test that output is as expected
        // dut.output.valid.expect(true)
        // dut.output.bits.dataOut.expect(i)

        // change output signal to ready and test that output is no longer valid
        dut.output.ready.poke(true)
        dut.clock.step(1)
      }

      for(i <- 0 until dut.dim2) { 
        // pass in values, input valid and output not ready
        dut.input.bits.index.poke(i)
        dut.input.bits.dataIn.poke(i)
        dut.input.bits.write.poke(false)
        dut.input.valid.poke(true)
        dut.output.ready.poke(false)

        dut.clock.step(1)

        // test that output is as expected
        dut.output.valid.expect(true)
        dut.output.bits.dataOut.expect(i)

        // change output signal to ready and test that output is no longer valid
        dut.output.ready.poke(true)
        dut.clock.step(1)
        // dut.output.valid.expect(false)
      }
    }
  }

  "Testing 4x4 table (decoupled2)" in {
    test(new LUTDecoupled(dim2=16, dataWidth=8)) { dut =>
      dut.input.initSource()
      dut.input.setSourceClock(dut.clock)
      dut.output.initSink()
      dut.output.setSinkClock(dut.clock)

      val testValues = for { i <- 0 until dut.dim2 } yield (i)
      val writeInputSeq = testValues.map{ i =>
        new LUTInputBundle(dim2=dut.dim2, dataWidth=dut.dataWidth).Lit(_.index -> i.U, _.dataIn -> i.U, _.write -> true.B) 
      }
      val writeResultSeq = testValues.map{ i =>
        new LUTOutputBundle(dim2=dut.dim2, dataWidth=dut.dataWidth).Lit(_.index -> i.U, _.dataIn -> i.U, _.write -> true.B, _.dataOut -> i.U) 
      }
      val readInputSeq = testValues.map{ i =>
        new LUTInputBundle(dim2=dut.dim2, dataWidth=dut.dataWidth).Lit(_.index -> i.U, _.dataIn -> i.U, _.write -> false.B) 
      }
      val readResultSeq = testValues.map{ i =>
        new LUTOutputBundle(dim2=dut.dim2, dataWidth=dut.dataWidth).Lit(_.index -> i.U, _.dataIn -> i.U, _.write -> false.B, _.dataOut -> i.U) 
      }

      // testing writes
      fork {
        val (seq1, seq2) = writeInputSeq.splitAt(writeInputSeq.length / 3)
        dut.input.enqueueSeq(seq1)
        dut.clock.step(2)
        dut.input.enqueueSeq(seq2)
      }.fork {
        val (seq1, seq2) = writeResultSeq.splitAt(writeResultSeq.length / 2)
        dut.output.expectDequeueSeq(seq1)
        dut.clock.step(2)
        dut.output.expectDequeueSeq(seq2)
      }.join()

      // testing reads
      fork {
        val (seq1, seq2) = readInputSeq.splitAt(readResultSeq.length / 4)
        dut.input.enqueueSeq(seq1)
        dut.clock.step(7)
        dut.input.enqueueSeq(seq2)
      }.fork {
        val (seq1, seq2) = readResultSeq.splitAt(readResultSeq.length / 2)
        dut.output.expectDequeueSeq(seq1)
        dut.clock.step(5)
        dut.output.expectDequeueSeq(seq2)
      }.join()
    }
  }

}
