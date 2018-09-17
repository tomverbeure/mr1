
package mr1

import spinal.core._

import vexriscv.demo._
import vexriscv.plugin._

class TopVexRiscv(config: MR1Config) extends Component {

    val io = new Bundle {
        val osc_clk = in(Bool)

        val led1    = out(Bool)
        val led2    = out(Bool)
        val led3    = out(Bool)

        val switch_ = in(Bool)
    }

    noIoPrefix()

    val resetCtrlClockDomain = ClockDomain(
        clock = io.osc_clk,
        frequency = FixedFrequency(50 MHz),
        config = ClockDomainConfig(
                    resetKind = BOOT
        )
    )

    val resetCtrl = new ClockingArea(resetCtrlClockDomain) {
        val reset_unbuffered_ = True

        val reset_cntr = Reg(UInt(5 bits)) init(0)
        when(reset_cntr =/= U(reset_cntr.range -> true)){
            reset_cntr := reset_cntr + 1
            reset_unbuffered_ := False
        }

        val reset_ = RegNext(reset_unbuffered_)
    }

    val coreClockDomain = ClockDomain(
        clock = io.osc_clk,
        reset = resetCtrl.reset_,
        config = ClockDomainConfig(
            resetKind = spinal.core.SYNC,
            resetActiveLevel = LOW
        )
    )

    val core = new ClockingArea(coreClockDomain) {

        val vex = GenSmallestNoCsr.cpu()

        var iBus : IBusSimpleBus = null
        var dBus : DBusSimpleBus = null

        for(plugin <- vex.plugins) plugin match{
            case plugin : IBusSimplePlugin => iBus <> plugin.iBus
            case plugin : DBusSimplePlugin => dBus <> plugin.dBus
        }

        val cpu_ram = new cpu_ram()

        iBus.cmd.ready := True

        iBus.rsp.valid := RegNext(iBus.cmd.valid) init(False)

        cpu_ram.io.address_a     := (iBus.cmd.payload.pc >> 2).resized
        cpu_ram.io.wren_a        := False
        cpu_ram.io.data_a        := 0
        iBus.rsp.inst            := cpu_ram.io.q_a

        dBus.cmd.ready := True

        val wmask = dBus.cmd.size.mux(
                        B"00"   -> B"0001",
                        B"01"   -> B"0011",
                        default -> B"1111") |<< dBus.cmd.address(1 downto 0)

        dBus.rsp.ready := RegNext(dBus.cmd.valid && !dBus.cmd.wr) init(False)

        cpu_ram.io.address_b     := (dBus.cmd.address >> 2).resized
        cpu_ram.io.wren_b        := dBus.cmd.valid && dBus.cmd.payload.wr && !dBus.cmd.payload.address(19)
        cpu_ram.io.byteena_b     := wmask
        cpu_ram.io.data_b        := dBus.cmd.payload.data
        dBus.rsp.data            := cpu_ram.io.q_b

        val update_leds = dBus.cmd.valid && dBus.cmd.wr && dBus.cmd.address(19)

        io.led1 := RegNextWhen(dBus.cmd.data(0), update_leds) init(False)
        io.led2 := RegNextWhen(dBus.cmd.data(1), update_leds) init(False)
        io.led3 := RegNextWhen(dBus.cmd.data(2), update_leds) init(False)
    }
}
