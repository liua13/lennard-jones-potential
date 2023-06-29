// package potential

// import chisel3._
// import chisel3.util._
// import hardfloat._
// import potential.Arithmetic.FloatArithmetic._

// class MoleculeBundle(val w: Int, expWidth: Int, sigWidth: Int) extends Bundle {
//   val id = UInt(w.W)
//   val x = Float(expWidth, sigWidth)
//   val y = Float(expWidth, sigWidth)
//   val z = Float(expWidth, sigWidth)
// }

// class CalculateForce(log2dim: Int, dataWidth: Int, expWidth: Int, sigWidth: Int) extends Module { 
//   val ready :: initialize :: calc :: calc2 :: calc3 :: calc4 :: error :: done :: Nil = Enum(8)

//   val io = IO(new Bundle{
//     val validIn = Input(Bool())
//     val molecule1 = Input(new MoleculeBundle(dataWidth, expWidth, sigWidth))
//     val molecule2 = Input(new MoleculeBundle(dataWidth, expWidth, sigWidth))

//     val sigma6Table = new Bundle {
//       val validIn = Output(Bool())
//       val index = Output(UInt((2 * log2dim).W))
//       val write = Output(Bool()) // write true, read false

//       val validOut = Input(Bool())
//       val dataOut = Input(Float(expWidth, sigWidth))
//     }

//     val epsilonTable = new Bundle {
//       val validIn = Output(Bool())
//       val index = Output(UInt((2 * log2dim).W))
//       val write = Output(Bool()) // write true, read false

//       val validOut = Input(Bool())
//       val dataOut = Input(Float(expWidth, sigWidth))
//     }

//     val errorOut = Output(Bool())
//     val validOut = Output(Bool())
//     val dataOut = Output(Float(expWidth, sigWidth))
//   })

//   val state = RegInit(ready)
//   val molecule1 = Reg(new MoleculeBundle(dataWidth, expWidth, sigWidth))
//   val molecule2 = Reg(new MoleculeBundle(dataWidth, expWidth, sigWidth))
//   val sigma6 = Reg(Float(expWidth, sigWidth))
//   val epsilon = Reg(Float(expWidth, sigWidth))
//   // val sr2 = Reg(Float(expWidth, sigWidth))
//   // val sr6 = Reg(Float(expWidth, sigWidth))
//   val rsq = RegInit(1.U.asTypeOf(Float(expWidth, sigWidth)))
//   val sr2 = RegInit(0.U.asTypeOf(Float(expWidth, sigWidth)))
//   val sr6 = RegInit(0.U.asTypeOf(Float(expWidth, sigWidth)))
//   val delx = Reg(Float(expWidth, sigWidth))
//   val dely = Reg(Float(expWidth, sigWidth))
//   val delz = Reg(Float(expWidth, sigWidth))
//   val inValid = RegInit(false.B)

//   val one = Wire(Float(expWidth, sigWidth))
//   if(expWidth == 8 && sigWidth == 24) one.bits := 1065353216.U 
//   else if(expWidth == 11 && sigWidth == 53) one.bits := 4607182418800017408L.U
//   else one.bits := Cat(0.U(2.W), ((1 << (expWidth - 1)) - 1).U((expWidth - 1).W), 0.U((sigWidth - 1).W))
  
//   val half = Wire(Float(expWidth, sigWidth))
//   if(expWidth == 8 && sigWidth == 24) half.bits := 1056964608.U
//   else if(expWidth == 11 && sigWidth == 53) 4602678819172646912L.U
//   else half.bits := Cat(0.U(2.W), ((1 << (expWidth - 2)) - 1).U((expWidth - 2).W), 0.U(sigWidth.W))

//   val fortyEight = Wire(Float(expWidth, sigWidth))
//   if(expWidth == 8 && sigWidth == 24) fortyEight.bits := 1111490560.U
//   else if(expWidth == 11 && sigWidth == 53) 4631952216750555136L.U
//   else fortyEight.bits := Cat((1 << (expWidth - 4)).U((expWidth - 2).W), (1 << 1).U(2.W), (1 << (sigWidth - 2)).U(sigWidth.W))
  
//   val result = (one./(rsq, inValid)).get

//   io.errorOut := false.B
//   io.validOut := false.B
//   io.dataOut := DontCare

//   io.sigma6Table.write := false.B
//   io.sigma6Table.validIn := false.B
//   io.sigma6Table.index := 0.U
//   io.epsilonTable.write := false.B
//   io.epsilonTable.validIn := false.B
//   io.epsilonTable.index := 0.U

//   sr2 := sr2
//   sr6 := sr6

//   inValid := false.B

//   when(state === ready && io.validIn) {
//     molecule1 := io.molecule1
//     molecule2 := io.molecule2

//     io.sigma6Table.index := io.molecule1.id << log2dim.U + io.molecule2.id
//     io.epsilonTable.index := io.molecule1.id << log2dim.U + io.molecule2.id
//     io.sigma6Table.validIn := true.B
//     io.epsilonTable.validIn := true.B

//     state := initialize

//     when(io.molecule1.x === io.molecule2.x && 
//         io.molecule1.y === io.molecule2.y &&
//         io.molecule1.z === io.molecule2.z) {
//       state := error
//     }

//   }.elsewhen(state === initialize) {
//     when(io.sigma6Table.validOut) {
//       sigma6 := io.sigma6Table.dataOut
//     }

//     when(io.epsilonTable.validOut) {
//       epsilon := io.epsilonTable.dataOut
//       state := calc
//     }

//   }.elsewhen(state === calc) {
//     val delx = molecule2.x - molecule1.x
//     val dely = molecule2.y - molecule1.y
//     val delz = molecule2.z - molecule1.z
//     rsq := delx * delx + dely * dely + delz * delz
//     inValid := true.B
//   }.elsewhen() {

//     // when(result.valid) {
//     //   sr2 := result.bits
//     //   state := calc2
//     // }
//   }.elsewhen(state === calc2) {
//     sr6 := sr2 * sr2 * sr2 * sigma6
//     state := done
//   }.elsewhen(state === done) {
//     io.validOut := true.B
//     io.dataOut := fortyEight * sr6 * (sr6 - half) * sr2 * epsilon
//     state := ready
//   }.elsewhen(state === error) {
//     io.errorOut := true.B
//   }
// }