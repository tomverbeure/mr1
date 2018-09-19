
package mr1

import spinal.core._

import vexriscv._
import vexriscv.demo._
import vexriscv.plugin._

object TopVerilog {
    def main(args: Array[String]) {

//        SpinalVerilog(new MR1(config = MR1Config(supportFormal = true,
//                                                supportMul = false,
//                                                supportDiv = false,
//                                                supportCsr = false)))

        val config = MR1Config(
            supportFormal = false,
            supportMul    = false,
            supportDiv    = false,
            supportCsr    = false
        )

        //SpinalVerilog(new TopMR1(config))
        //SpinalVerilog(new TopPicoRV32(config))

        val vexConfig = VexRiscvConfig(
            plugins = List(
                new IBusSimplePlugin(
                    resetVector = 0x80000000l,
                    relaxedPcCalculation = false,
//                    prediction = NONE,
                    prediction = DYNAMIC_TARGET,
                    historyRamSizeLog2 = 8,
                    catchAccessFault = false,
                    compressedGen = false
                ),
                new DBusSimplePlugin(
                    catchAddressMisaligned = false,
                    catchAccessFault = false,
                    earlyInjection = true
                ),
                new DecoderSimplePlugin(
                    catchIllegalInstruction = false
                ),
                new RegFilePlugin(
                    regFileReadyKind = plugin.SYNC,
                    zeroBoot = true,
                    writeRfInMemoryStage = false
                ),
                new IntAluPlugin,
                new MulPlugin,
                new DivPlugin,
                new SrcPlugin(
                    separatedAddSub = false,
                    executeInsertion = false
                ),
//                new LightShifterPlugin,
                new FullBarrelShifterPlugin(
                    earlyInjection = true
                ),
                new HazardSimplePlugin(
                    bypassExecute           = true,
                    bypassMemory            = true,
                    bypassWriteBack         = true,
                    bypassWriteBackBuffer   = true,
                    pessimisticUseSrc       = false,
                    pessimisticWriteRegFile = false,
                    pessimisticAddressMatch = false
                ),
                new BranchPlugin(
                    earlyBranch = true,
                    catchAddressMisaligned = false
                ),
                new YamlPlugin("cpu0.yaml")
            )
        )

        SpinalVerilog(new TopVexRiscv(vexConfig))
    }
}

