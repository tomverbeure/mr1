
package mr1

import spinal.core._

class MR1 extends Component {
  val io = new Bundle {
    val instr = in  UInt(32 bits)
  }

}

object MR1Verilog {
  def main(args: Array[String]) {
    SpinalVerilog(new MR1)
  }
}

