
package mr1

import spinal.core._

case class MR1Config(
                supportMul      : Boolean = true,
                supportDiv      : Boolean = true,
                supportCsr      : Boolean = true) {

    def hasMul = supportMul
    def hasDiv = supportDiv
    def hasCsr = supportCsr
}


class MR1(config: MR1Config) extends Component {
    val io = new Bundle {
        val instr_valid = in Bool
        val instr       = in Bits(32 bits)
    }

    val decode = new Decode(config)

    io.instr_valid <> decode.io.instr_valid
    io.instr <> decode.io.instr

}

object MR1Verilog {
    def main(args: Array[String]) {
        SpinalVerilog(new MR1(config = MR1Config()))
    }
}

