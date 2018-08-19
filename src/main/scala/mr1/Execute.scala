
package mr1

import spinal.core._


class Execute(config: MR1Config) extends Component {

    val hasMul   = config.hasMul
    val hasDiv   = config.hasDiv
    val hasCsr   = config.hasCsr
    val hasFence = config.hasFence

    val io = new Bundle {
        val d2e         = in(Decode2Execute(config))
        val e2d         = out(Execute2Decode(config))

        val rvfi        = out(RVFI(config))
    }

    io.e2d.stall := False

    val formal = if (config.hasFormal) new Area {

        val rvfi = io.d2e.rvfi

        io.rvfi := rvfi
    }

}


