
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

        val r2e         = in(RegFile2Execute(config))

        val rvfi        = out(RVFI(config))
    }

    io.e2d.stall := False

    val iformat = InstrFormat()
    val itype   = InstrType()
    val instr   = Bits(32 bits)
    val funct3  = Bits(3 bits)

    iformat := io.d2e.decoded_instr.iformat
    itype   := io.d2e.decoded_instr.itype
    instr   := io.d2e.instr
    funct3  := instr(14 downto 12)

    val rs1 = Bits(32 bits)
    val rs2 = Bits(32 bits)

    rs1 := io.r2e.rs1_data
    rs2 := io.r2e.rs2_data

    val i_imm_11_0 = instr(31 downto 20)
    val b_imm_12_1 = S(instr(31) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8))

    val rd_wr_addr  = instr(11 downto 7)

    val alu = new Area {
        val result = UInt(32 bits)

        result := U"32'd0"

        switch(itype){
            is(InstrType.ALU){
                switch(funct3){
                    is(B"000"){         // ADD/SUB
                        result := Mux(instr(30),
                                      U(rs1) - U(rs2),
                                      U(rs1) + U(rs2))
                    }
                    is(B"100"){         // XOR
                        result := U(rs1) ^ U(rs2)
                    }
                }
            }
            is(InstrType.ALU_I){
            }
        }
    }

    val jump = new Area {

        val pc      = UInt(32 bits)
        val pc_jump = UInt(32 bits)
        val branch_cond = Bool

        pc          := io.d2e.pc
        pc_jump     := pc + 4
        branch_cond := False

        io.e2d.pc_jump_valid := False
        io.e2d.pc_jump       := pc_jump

        switch(itype){
            is(InstrType.B){
                switch(funct3){
                    is(B"000"){         // BEQ
                        branch_cond := (rs1 === rs2)
                    }
                    is(B"001"){         // BNE
                        branch_cond := (rs1 =/= rs2)
                    }
                    is(B"100"){         // BLT
                        branch_cond := (S(rs1) < S(rs2))
                    }
                    is(B"101"){         // BGE
                        branch_cond := (S(rs1) >= S(rs2))
                    }
                    is(B"110"){         // BLTU
                        branch_cond := (U(rs1) < U(rs2))
                    }
                    is(B"111"){         // BGEU
                        branch_cond := (U(rs1) >= U(rs2))
                    }
                }

                when(branch_cond){
                    pc_jump := U(S(U("0") ## pc) + (b_imm_12_1 @@ S("0")))(31 downto 0)
                }

                io.e2d.pc_jump_valid := True
                io.e2d.pc_jump       := pc_jump
            }
            is(InstrType.JAL){
            }
            is(InstrType.JALR){
            }
        }
    }

    val formal = if (config.hasFormal) new Area {

        val rvfi = io.d2e.rvfi

        io.rvfi := rvfi

        when(True){
            io.rvfi.rs1_rdata := io.r2e.rs1_data
            io.rvfi.rs2_rdata := io.r2e.rs2_data

            io.rvfi.rd_wdata  := (rd_wr_addr =/= 0) ? B(alu.result) | B"32'd0"
        }
    }

}


