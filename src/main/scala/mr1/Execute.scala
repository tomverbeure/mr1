
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

        val rvfi        = if (config.hasFormal) out(RVFI(config)) else null
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


    val alu = new Area {
        val rd_wr    = False
        val rd_wdata = U(0, 32 bits)

        switch(itype){
            is(InstrType.ALU){
                switch(funct3){
                    is(B"000"){         // ADD/SUB
                        rd_wr    := True
                        rd_wdata := Mux(instr(30),
                                        U(rs1) - U(rs2),
                                        U(rs1) + U(rs2))
                    }
                    is(B"010"){         // SLT
                        rd_wr    := True
                        rd_wdata := U(S(rs1) < S(rs2)).resize(32)
                    }
                    is(B"011"){         // SLTU
                        rd_wr    := True
                        rd_wdata := U(U(rs1) < U(rs2)).resize(32)
                    }
                    is(B"100"){         // XOR
                        rd_wr    := True
                        rd_wdata := U(rs1) ^ U(rs2)
                    }
                    is(B"110"){         // OR
                        rd_wr    := True
                        rd_wdata := U(rs1) | U(rs2)
                    }
                    is(B"111"){         // AND
                        rd_wr    := True
                        rd_wdata := U(rs1) & U(rs2)
                    }
                }
            }
            is(InstrType.ALU_I){
                switch(funct3){
                    is(B"000"){         // ADDI
                        rd_wr    := True
                        rd_wdata := U(S(rs1) + S(i_imm_11_0).resize(32))
                    }
                    is(B"010"){         // SLT
                        rd_wr    := True
                        rd_wdata := U(S(rs1) < S(i_imm_11_0).resize(32)).resize(32)
                    }
                    is(B"011"){         // SLTIU
                        rd_wr    := True
                        rd_wdata := U(U(rs1) < U(S(i_imm_11_0).resize(32))).resize(32)
                    }
                    is(B"100"){         // XORI
                        rd_wr    := True
                        rd_wdata := U(S(rs1) ^ S(i_imm_11_0).resize(32))
                    }
                    is(B"110"){         // ORI
                        rd_wr    := True
                        rd_wdata := U(S(rs1) | S(i_imm_11_0).resize(32))
                    }
                    is(B"111"){         // ANDI
                        rd_wr    := True
                        rd_wdata := U(S(rs1) & S(i_imm_11_0).resize(32))
                    }
                }
            }
        }
    }

    val shift = new Area {
        val rd_wr    = False
        val rd_wdata = U(0, 32 bits)
        val shamt    = U(instr(24 downto 20))

        switch(itype){
            is(InstrType.SHIFT){
                switch(funct3){
                    is(B"001"){             // SLL
                        rd_wr    := True
                        rd_wdata := U(rs1) |<< U(rs2(4 downto 0))
                    }
                    is(B"101"){
                        when(instr(30)){    // SRA
                            rd_wr    := True
                            rd_wdata := U(S(rs1) >> U(rs2(4 downto 0)))
                        }.otherwise{        // SRL
                            rd_wr    := True
                            rd_wdata := U(rs1) |>> U(rs2(4 downto 0))
                        }
                    }
                }
            }
            is(InstrType.SHIFT_I){
                switch(funct3){
                    is(B"001"){             // SLLI
                        rd_wr    := True
                        rd_wdata := U(rs1) |<< shamt
                    }
                    is(B"101"){
                        when(instr(30)){    // SRAI
                            rd_wr    := True
                            rd_wdata := U(S(rs1) >> shamt)
                        }.otherwise{        // SRLI
                            rd_wr    := True
                            rd_wdata := U(rs1) |>> shamt
                        }
                    }
                }
            }
        }
    }

    val jump = new Area {

        val rd_wr    = False
        val rd_wdata = U(0, 32 bits)

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

    val rd_wr     = (alu.rd_wr | jump.rd_wr | shift.rd_wr) && (instr(11 downto 7) =/= 0)
    val rd_waddr  = rd_wr ? instr(11 downto 7) | B"5'd0"

    val rd_wdata = B(0, 32 bits)
    when(rd_wr){
        when(alu.rd_wr){
            rd_wdata := B(alu.rd_wdata)
        }.elsewhen(jump.rd_wr){
            rd_wdata := B(jump.rd_wdata)
        }.elsewhen(shift.rd_wr){
            rd_wdata := B(shift.rd_wdata)
        }
    }

    val formal = if (config.hasFormal) new Area {

        val rvfi = io.d2e.rvfi

        io.rvfi := rvfi

        when(io.r2e.rs1_data =/= 0){
            io.rvfi.rs1_rdata := io.r2e.rs1_data
        }

        when(io.r2e.rs2_data =/= 0){
            io.rvfi.rs2_rdata := io.r2e.rs2_data
        }

        when(True){
            io.rvfi.rd_addr   := rd_waddr
            io.rvfi.rd_wdata  := rd_wdata
        }

        when(io.e2d.pc_jump_valid){
            io.rvfi.pc_wdata  := B(io.e2d.pc_jump)
        }

        when(io.e2d.pc_jump_valid && io.e2d.pc_jump(1 downto 0) =/= "00"){
            io.rvfi.trap := True
        }
    } else null

}

