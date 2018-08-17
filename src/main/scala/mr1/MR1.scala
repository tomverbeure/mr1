
package mr1

import spinal.core._

class MR1 extends Component {
    val io = new Bundle {
        val instr_valid = in Bool
        val instr       = in Bits(32 bits)
    }

    val decode = new Decode()

    io.instr_valid <> decode.io.instr_valid
    io.instr <> decode.io.instr

}

object MR1Verilog {
    def main(args: Array[String]) {
        SpinalVerilog(new MR1)
    }
}

