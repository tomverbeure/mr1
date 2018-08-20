
package mr1

import spinal.core._

case class RegFile2Execute(config: MR1Config) extends Bundle {
    
    val rs1_valid   = Bool
    val rs2_valid   = Bool

    val rs1_data    = Bits(32 bits)
    val rs2_data    = Bits(32 bits)
}

class RegFile(config: MR1Config) extends Component {

    val io = new Bundle {
        val d2r         = in(Decode2RegFile(config))
        val r2e         = out(RegFile2Execute(config))

        val rd_wr       = in(Bool)
        val rd_wr_addr  = in(UInt(5 bits))
        val rd_wr_data  = in(Bits(32 bits))
    }

    val mem = Mem(Bits(32 bits), 32)

    io.r2e.rs1_data := mem.readSync(io.d2r.rs1_rd_addr, io.d2r.rs1_rd)
    io.r2e.rs2_data := mem.readSync(io.d2r.rs2_rd_addr, io.d2r.rs2_rd)

    // FIXME: These are only for debug. 
    io.r2e.rs1_valid := RegNext(io.d2r.rs1_rd)
    io.r2e.rs2_valid := RegNext(io.d2r.rs2_rd)

    val initR0 = RegNext(False) init(True)

    val rd_wr      = Bool
    val rd_wr_addr = UInt(5 bits)
    val rd_wr_data = Bits(32 bits)

    // Write 0 to r0 after reset 
    rd_wr      := initR0 ? True     | io.rd_wr
    rd_wr_addr := initR0 ? U"5'd0"  | io.rd_wr_addr
    rd_wr_data := initR0 ? B"32'd0" | io.rd_wr_data

    mem.write(rd_wr_addr, rd_wr_data, rd_wr)
}


