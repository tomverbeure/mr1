
package mr1

import spinal.core._


class TopPicoRV32(config: MR1Config) extends Component {

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
            resetKind = SYNC,
            resetActiveLevel = LOW
        )
    )

    val core = new ClockingArea(coreClockDomain) {

        val picorv32 = new picorv32(
            enableCounters      = 0,
            enableCounter64     = 0,
            enableRegs1631      = 1,
            enableRegsDualPort  = 1,
            latchedMemRData     = 0,
            twoStageShift       = 0,
            barrelShifter       = 1,
            twoCycleCompare     = 0,
            twoCycleAlu         = 0,
            compressedIsa       = 0,
            catchMisalign       = 1,
            catchIllinsn        = 1,
            enablePcpi          = 0,
            enableMul           = if (config.hasMul) 1 else 0,
            enableFastMul       = if (config.hasMul) 1 else 0,
            enableDiv           = 0,
            enableIreq          = 0,
            enableIrqQregs      = 0,
            enableIrqTimer      = 0,
            enableTrace         = 0,
            enableInitZero      = 0,

            maskedIrq           = 0x00000000,
            latchedIrq          = 0xffffffff,
            progaddrReset       = 0x00000000,
            progaddrIrq         = 0x00000010,
            stackaddr           = 0x00001000
        )

        picorv32.io.pcpi_wr     := False
        picorv32.io.pcpi_rd     := 0
        picorv32.io.pcpi_wait   := False
        picorv32.io.pcpi_ready  := False

        picorv32.io.irq         := 0

        val cpu_ram = new cpu_ram()

        // Only use port B, since that one's read/write
        cpu_ram.io.address_a    := 0
        cpu_ram.io.wren_a       := False
        cpu_ram.io.data_a       := 0

        picorv32.io.mem_ready   := True

        cpu_ram.io.address_b    := (U(picorv32.io.mem_la_addr) >> 2).resized
        cpu_ram.io.wren_b       := picorv32.io.mem_la_write && !picorv32.io.mem_la_addr(19)
        cpu_ram.io.data_b       := picorv32.io.mem_la_wdata
        cpu_ram.io.byteena_b    := picorv32.io.mem_la_wstrb
        picorv32.io.mem_rdata   := cpu_ram.io.q_b

        val update_leds = picorv32.io.mem_la_write && picorv32.io.mem_la_addr(19)

        io.led1 := RegNextWhen(picorv32.io.mem_la_wdata(0), update_leds) init(False)
        io.led2 := RegNextWhen(picorv32.io.mem_la_wdata(1), update_leds) init(False)
        io.led3 := RegNextWhen(picorv32.io.mem_la_wdata(2), update_leds) init(False)
    }
}

