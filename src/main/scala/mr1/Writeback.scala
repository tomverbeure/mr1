
package mr1

import spinal.core._


class Writeback(config: MR1Config) extends Component {

    val io = new Bundle {
        val e2w         = in(Execute2Writeback(config))
        val w2e         = out(Writeback2Execute(config))
        val w2all       = out(ActiveRd2All(config))

        val w2r         = out(Write2RegFile(config))

        val data_rsp    = DataRspIntfc(config)

        val rvfi        = if (config.hasFormal) out(Reg(RVFI(config)) init) else null
    }

    val w2e_stall_d = RegNext(io.w2e.stall, False)

    val wb_start = io.e2w.valid && !w2e_stall_d
    val wb_end   = io.e2w.valid && !io.w2e.stall

    val lsu = new Area {

        val rsp_data_shift_adj = Bits(32 bits)
        rsp_data_shift_adj := io.data_rsp.data >> (ld_addr_lsb(1 downto 0) * 8)

        val rd_wdata = ( (funct3 === B"000") ? B(S(rsp_data_shift_adj( 7 downto 0)).resize(32)) |
                       ( (funct3 === B"100") ? B(U(rsp_data_shift_adj( 7 downto 0)).resize(32)) |
                       ( (funct3 === B"001") ? B(S(rsp_data_shift_adj(15 downto 0)).resize(32)) |
                       ( (funct3 === B"101") ? B(U(rsp_data_shift_adj(15 downto 0)).resize(32)) |
                                                   rsp_data_shift_adj))))
    }

    val rd_wr    = io.e2w.valid && (io.e2w.rd_wr | lsu.rd_wr) && (io.e2w.rd_addr =/= 0)
    val rd_waddr = rd_wr ? io.e2w.rd_addr | U"5'd0"
    val rd_wdata = B((io.e2w.rd_wdata.range -> io.e2w.rd_wr))   & B(io.e2w.rd_wdata)   |
                   B((lsu.rd_wdata.range    -> lsu.rd_wr   ))   & B(lsu.rd_wdata)

    io.w2e.stall         := lsu.lsu_stall

    io.w2all.rd_addr_valid := io.e2w.valid && io.e2w.rd_valid
    io.w2all.rd_addr       := rd_addr

    // Write to RegFile
    io.w2r.rd_wr        := rd_wr
    io.w2r.rd_wr_addr   := rd_waddr
    io.w2r.rd_wr_data   := rd_wdata

    val formal = if (config.hasFormal) new Area {

        io.rvfi.valid := exe_end

        when(wb_start){
            io.rvfi.order     := io.e2w.rvfi.order
            io.rvfi.pc_rdata  := io.e2w.rvfi.pc_rdata
            io.rvfi.pc_wdata  := io.e2w.rvfi.pc_wdata
            io.rvfi.insn      := io.e2w.rvfi.insn
            io.rvfi.trap      := io.e2w.rvfi.trap
            io.rvfi.halt      := io.e2w.rvfi.halt
            io.rvfi.intr      := io.e2w.rvfi.intr

            io.rvfi.rs1_addr  := io.e2w.rvfi.rs1_addr
            io.rvfi.rs2_addr  := io.e2w.rvfi.rs2_addr
            io.rvfi.rd_addr   := io.e2w.rvfi.rd_addr

            io.rvfi.rs1_rdata := io.e2w.rvfi.rs1_rdata
            io.rvfi.rs2_rdata := io.e2w.rvfi.rs2_rdata
            io.rvfi.rd_wdata  := 0

            io.rvfi.mem_addr  := io.e2w.rvfi.mem_addr
            io.rvfi.mem_rmask := io.e2w.rvfi.mem_rmask
            io.rvfi.mem_rdata := 0
            io.rvfi.mem_wmask := io.e2w.rvfi.mem_wmask
            io.rvfi.mem_wdata := io.e2w.rvfi.mem_wdata
        }

        when(rd_wr){
            io.rvfi.rd_addr   := rd_waddr
            io.rvfi.rd_wdata  := rd_wdata
        }

        switch(itype){
            is(InstrType.L){
                when(io.data_rsp.valid){
                    io.rvfi.mem_rdata := io.data_rsp.data
                }
            }
        }

    } else null

}


