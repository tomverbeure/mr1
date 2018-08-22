
package mr1

import spinal.core._


case class Fetch2Decode(config: MR1Config) extends Bundle {

    val valid               = Bool
    val instr               = Bits(32 bits)
}

case class Decode2Fetch(config: MR1Config) extends Bundle {
    val stall               = Bool
}

class Fetch(config: MR1Config) extends Component {

    val io = new Bundle {

        val instr_valid     = in(Bool)
        val instr_stall     = out(Bool)
        val instr           = in(Bits(32 bits))

        val f2d             =  out(Fetch2Decode(config))
        val d2f             =  in(Decode2Fetch(config))
    }

    io.f2d.valid   <> io.instr_valid
    io.d2f.stall   <> io.instr_stall
    io.f2d.instr   <> io.instr
}

