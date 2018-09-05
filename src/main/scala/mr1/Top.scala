
package mr1

import spinal.core._

class TopMR1(config: MR1Config) extends Component {

    val io = new Bundle {
        val osc_clk = in(Bool)

        val led1    = out(Bool)
        val led2    = out(Bool)
        val led3    = out(Bool)

        val switch_ = in(Bool)
    }

    noIoPrefix()

    val coreClockDomain = ClockDomain.internal(
        name = "core",
        config = ClockDomainConfig(
                    clockEdge = RISING,
                    resetKind = SYNC,
                    resetActiveLevel = LOW),
        frequency = FixedFrequency(50 MHz)
    )

    val core = new ClockingArea(coreClockDomain) {

        coreClockDomain.clock := io.osc_clk
        coreClockDomain.reset := RegNext(True)

        val mr1 = new MR1(config)

        if (false){
            val cpu_ram = Mem(Bits(32 bits), 4096/4)

            mr1.io.instr_req.ready := True

            mr1.io.instr_rsp.valid := RegNext(mr1.io.instr_req.valid) init(False)
            mr1.io.instr_rsp.data := cpu_ram.readSync(
                    enable  = mr1.io.instr_req.valid,
                    address = (mr1.io.instr_req.addr >> 2).resized
                )

            mr1.io.data_req.ready := True

            val wmask = mr1.io.data_req.size.mux(
                            B"00"   -> B"0001",
                            B"01"   -> B"0011",
                            default -> B"1111") |<< mr1.io.data_req.addr(1 downto 0)

            mr1.io.data_rsp.valid := RegNext(mr1.io.data_req.valid && !mr1.io.data_req.wr) init(False)
            mr1.io.data_rsp.data := cpu_ram.readWriteSync(
                    enable  = mr1.io.data_req.valid && !mr1.io.data_req.addr(19),
                    address = (mr1.io.data_req.addr >> 2).resized,
                    write   = mr1.io.data_req.wr,
                    data    = mr1.io.data_req.data,
                    mask    = wmask
                )
        }
        else {
            val cpu_ram = new cpu_ram()

            mr1.io.instr_req.ready := True

            mr1.io.instr_rsp.valid := RegNext(mr1.io.instr_req.valid) init(False)

            cpu_ram.io.address_a     := (mr1.io.instr_req.addr >> 2).resized
            cpu_ram.io.wren_a        := False
            cpu_ram.io.data_a        := 0
            mr1.io.instr_rsp.data    := cpu_ram.io.q_a

            mr1.io.data_req.ready := True

            val wmask = mr1.io.data_req.size.mux(
                            B"00"   -> B"0001",
                            B"01"   -> B"0011",
                            default -> B"1111") |<< mr1.io.data_req.addr(1 downto 0)

            mr1.io.data_rsp.valid := RegNext(mr1.io.data_req.valid && !mr1.io.data_req.wr) init(False)

            cpu_ram.io.address_b     := (mr1.io.data_req.addr >> 2).resized
            cpu_ram.io.wren_b        := mr1.io.data_req.valid && mr1.io.data_req.wr && !mr1.io.data_req.addr(19)
            cpu_ram.io.byteena_b     := wmask
            cpu_ram.io.data_b        := mr1.io.data_req.data
            mr1.io.data_rsp.data     := cpu_ram.io.q_b
        }


        io.led1 := RegNextWhen(mr1.io.data_req.data(0), mr1.io.data_req.valid && mr1.io.data_req.wr && mr1.io.data_req.addr(19)) init(False)
        io.led2 := RegNextWhen(mr1.io.data_req.data(1), mr1.io.data_req.valid && mr1.io.data_req.wr && mr1.io.data_req.addr(19)) init(False)
        io.led3 := RegNextWhen(mr1.io.data_req.data(2), mr1.io.data_req.valid && mr1.io.data_req.wr && mr1.io.data_req.addr(19)) init(False)

    }
}

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
            enableMul           = 0,
            enableFastMul       = 0,
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

//        picorv32.io.mem_ready   := RegNext(picorv32.io.mem_la_write || picorv32.io.mem_la_read)
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

object TopVerilog {
    def main(args: Array[String]) {
        SpinalVerilog(new TopMR1(config = MR1Config(supportFormal = false)))
        SpinalVerilog(new TopPicoRV32(config = MR1Config(supportFormal = false)))
    }
}

