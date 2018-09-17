
package mr1

import spinal.core._
import vexriscv.demo._

object TopVerilog {
    def main(args: Array[String]) {

        SpinalVerilog(new MR1(config = MR1Config(supportFormal = true,
                                                 supportMul = false,
                                                 supportDiv = false,
                                                 supportCsr = false)))

        val config = MR1Config(
            supportFormal = false,
            supportMul    = false,
            supportDiv    = false,
            supportCsr    = false
        )

        //SpinalVerilog(new TopMR1(config))
        //SpinalVerilog(new TopPicoRV32(config))

        SpinalVerilog(new TopVexRiscv(config))
        //SpinalConfig(mergeAsyncProcess = false).generateVerilog(GenSmallestNoCsr.cpu())
    }
}

