
package mr1

import spinal.core._

class Top(config: MR1Config) extends Component {

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

object TopVerilog {
    def main(args: Array[String]) {
        SpinalVerilog(new Top(config = MR1Config(supportFormal = false)))
    }
}

