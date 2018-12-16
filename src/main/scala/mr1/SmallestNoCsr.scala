
package mr1

import spinal.core._
import vexriscv.demo._

object SmallestNoCsr {
    def main(args: Array[String]) {

        //SpinalVerilog(new TopVexRiscv(config))
        SpinalConfig(mergeAsyncProcess = false).generateVerilog(GenSmallestNoCsr.cpu())
    }
}

