
package mr1

import spinal.core._

case class DecodedInstr(config: MR1Config) extends Bundle {

    val iformat = InstrFormat()
    val itype   = InstrType()
}

case class Decode2Execute(config: MR1Config) extends Bundle {

    val valid           = Bool
    val pc              = UInt(32 bits)
    val instr           = Bits(32 bits)
    val decoded_instr   = DecodedInstr(config)
    val imm             = SInt(32 bits)
    val rs1_data        = Bits(32 bits)
    val rs2_data        = Bits(32 bits)

    val rvfi = if (config.hasFormal) RVFI(config) else null

    def init() : Decode2Execute = {
        valid init(False)
        if (config.hasFormal) rvfi init()
        this
    }

}

case class Execute2Decode(config: MR1Config) extends Bundle {

    val stall   = Bool

    val pc_jump_valid = Bool                // FIXME: This is probably redundant with stall, but let's leave it for now.
    val pc_jump       = UInt(32 bits)

    val rd_addr_valid = Bool
    val rd_addr       = UInt(5 bits)
}

class Decode(config: MR1Config) extends Component {

    val hasMul   = config.hasMul
    val hasDiv   = config.hasDiv
    val hasCsr   = config.hasCsr
    val hasFence = config.hasFence

    val io = new Bundle {
        val f2d         = in(Fetch2Decode(config))
        val d2f         = out(Decode2Fetch(config))

        val r2rr        = in(RegFile2ReadResult(config))

        val d2e         = out(Reg(Decode2Execute(config)) init)
        val e2d         = in(Execute2Decode(config))
    }

    val instr       = io.f2d.instr

    val d2f_stall_d = RegNext(io.d2f.stall, False)
    val f2d_valid_d = RegNext(io.f2d.valid, False)
    val decode_start    = io.f2d.valid && !d2f_stall_d
    val decode_end      = io.f2d.valid && !io.d2f.stall
    val decode_go_idle  = !io.f2d.valid && f2d_valid_d

    val decode = new Area {

        val opcode      = instr(6 downto 0)
        val funct3      = instr(14 downto 12)
        val funct7      = instr(31 downto 25)
        val rd_addr     = U(instr(11 downto 7))
        val rs1_addr    = U(instr(19 downto 15))
        val rs2_addr    = U(instr(24 downto 20))

        val decoded_instr       = DecodedInstr(config)

        decoded_instr.iformat   := InstrFormat.Undef
        decoded_instr.itype     := InstrType.Undef

        switch(opcode){
            is(Opcodes.LUI){
                decoded_instr.itype     := InstrType.LUI
                decoded_instr.iformat   := InstrFormat.U
            }
            is(Opcodes.AUIPC){
                decoded_instr.itype     := InstrType.AUIPC
                decoded_instr.iformat   := InstrFormat.U
            }
            is(Opcodes.JAL){
                decoded_instr.itype     := InstrType.JAL
                decoded_instr.iformat   := InstrFormat.J
            }
            is(Opcodes.JALR){
                when(funct3 === B"000") {
                    decoded_instr.itype     := InstrType.JALR
                    decoded_instr.iformat   := InstrFormat.I
                }
            }
            is(Opcodes.B){
                when(funct3 =/= B"010" && funct3 =/= B"011") {
                    decoded_instr.itype     := InstrType.B
                    decoded_instr.iformat   := InstrFormat.B
                }
            }
            is(Opcodes.L){
                when(funct3 =/= B"011" && funct3 =/= B"110" && funct3 =/= B"111") {
                    decoded_instr.itype     := InstrType.L
                    decoded_instr.iformat   := InstrFormat.I
                }
            }
            is(Opcodes.S){
                when(funct3 === B"000" || funct3 === B"001" || funct3 === B"010") {
                    decoded_instr.itype     := InstrType.S
                    decoded_instr.iformat   := InstrFormat.S
                }
            }
            is(Opcodes.ALUI){
                when(funct3 === B"000" || funct3 === B"010" || funct3 === B"011" || funct3 === B"100" || funct3 === B"110" || funct3 === B"111") {
                    // ALU_I: ADDI, SLTI, SLTIU, XORI, ORI, ANDI
                    decoded_instr.itype     := InstrType.ALU_I
                    decoded_instr.iformat   := InstrFormat.I
                }.elsewhen( (funct7 ## funct3) === B"0000000001" || (funct7 ## funct3) === B"0000000101" || (funct7 ## funct3) === B"0100000101") {
                    // SHIFT_I: SLLI, SRLI, SRAI
                    decoded_instr.itype     := InstrType.SHIFT_I
                    decoded_instr.iformat   := InstrFormat.I
                }
            }
            is(Opcodes.ALU){
                switch(funct7 ## funct3){
                    is(B"0000000_000", B"0100000_000", B"0000000_100", B"0000000_110", B"0000000_111"){
                        // ADD, SUB, XOR, OR, AND
                        decoded_instr.itype     := InstrType.ALU
                        decoded_instr.iformat   := InstrFormat.R
                    }
                    is(B"0000000_001", B"0000000_101", B"0100000_101"){
                        // SLL, SRL, SRA
                        decoded_instr.itype     := InstrType.SHIFT
                        decoded_instr.iformat   := InstrFormat.R
                    }
                    is( B"0000000_010", B"0000000_011") {
                        // SLT, SLTU
                        decoded_instr.itype     := InstrType.ALU
                        decoded_instr.iformat   := InstrFormat.R
                    }
                    is(B"0000001_000", B"0000001_001", B"0000001_010", B"0000001_011"){
                        // MUL
                        if (hasMul){
                            when(funct7 === B"0000001"){
                                decoded_instr.itype     := InstrType.MULDIV
                                decoded_instr.iformat   := InstrFormat.R
                            }
                        }
                    }
                    is(B"0000001_100", B"0000001_101", B"0000001_110", B"0000001_111"){
                        // DIV
                        if (hasDiv){
                            when(funct7 === B"0000001"){
                                decoded_instr.itype     := InstrType.MULDIV
                                decoded_instr.iformat   := InstrFormat.R
                            }
                        }
                    }
                }
            }
            is(Opcodes.F){
                if (hasFence){
                    when( funct3 === B"000" || funct3 === B"001"){
                        decoded_instr.itype     := InstrType.FENCE
                        decoded_instr.iformat   := InstrFormat.I
                    }
                }
            }
            // ECALL, EBREAK, CSR
            is(Opcodes.SYS){
                when( instr(31 downto 7) === B"0000_0000_0000_0000_0000_0000_0" || instr(31 downto 7) === B"0000_0000_0001_0000_0000_0000_0")
                {
                    decoded_instr.itype     := InstrType.E
                    decoded_instr.iformat   := InstrFormat.I
                }.elsewhen(funct3 === B"001" || funct3 === B"010" || funct3 === B"011" || funct3 === B"101" || funct3 === B"110" || funct3 === B"111") {
                    if (hasCsr){
                        decoded_instr.itype     := InstrType.CSR
                        decoded_instr.iformat   := InstrFormat.I
                    }
                }
            }
        }
    }

    val i_imm = S(B((19 downto 0) -> instr(31)) ## instr(31 downto 20))
    val s_imm = S(B((19 downto 0) -> instr(31)) ## instr(31 downto 25) ## instr(11 downto 7))
    val b_imm = S(B((19 downto 0) -> instr(31)) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8) ## "0")
    val j_imm = S(B((10 downto 0) -> instr(31)) ## instr(31) ## instr(19 downto 12) ## instr(20) ## instr(30 downto 21) ## "0")
    val u_imm = S(instr(31 downto 12) ## B((11 downto 0) -> false))

    val imm = decode.decoded_instr.iformat.mux(
                InstrFormat.I -> i_imm,
                InstrFormat.S -> s_imm,
                InstrFormat.B -> b_imm,
                InstrFormat.J -> j_imm,
                InstrFormat.U -> u_imm,
                default       -> i_imm
                )

    io.d2f.pc_jump_valid <> io.e2d.pc_jump_valid
    io.d2f.pc_jump       <> io.e2d.pc_jump

    val rs1_valid =  (decode.decoded_instr.iformat === InstrFormat.R) ||
                     (decode.decoded_instr.iformat === InstrFormat.I) ||
                     (decode.decoded_instr.iformat === InstrFormat.S) ||
                     (decode.decoded_instr.iformat === InstrFormat.B)

    val rs2_valid =  (decode.decoded_instr.iformat === InstrFormat.R) ||
                     (decode.decoded_instr.iformat === InstrFormat.S) ||
                     (decode.decoded_instr.iformat === InstrFormat.B)

    val rd_valid =   (decode.decoded_instr.iformat === InstrFormat.R) ||
                     (decode.decoded_instr.iformat === InstrFormat.I) ||
                     (decode.decoded_instr.iformat === InstrFormat.U) ||
                     (decode.decoded_instr.iformat === InstrFormat.J)

    val rs1 = io.r2rr.rs1_data

    val rs2 = Bits(32 bits)
    rs2 := decode.decoded_instr.iformat.mux(
            InstrFormat.R   -> io.r2rr.rs2_data,
            InstrFormat.I   -> B(i_imm),
            default         -> io.r2rr.rs2_data
            )

    io.d2f.stall := io.e2d.stall
    io.d2f.rd_addr_valid := rd_valid
    io.d2f.rd_addr       := decode.rd_addr

    val formal = if (config.hasFormal) new Area {

        val rvfi = RVFI(config)

        val order = Reg(UInt(64 bits)) init(0)
        when(decode_end){
            order := order + 1
        }

        rvfi.valid      := decode_end
        rvfi.order      := order
        rvfi.insn       := io.f2d.instr
        rvfi.trap       := (decode.decoded_instr.iformat === InstrFormat.Undef)
        rvfi.halt       := False
        rvfi.intr       := False
        rvfi.rs1_addr   := rs1_valid ? decode.rs1_addr | 0
        rvfi.rs2_addr   := rs2_valid ? decode.rs2_addr | 0
        rvfi.rs1_rdata  := rs1_valid ? io.r2rr.rs1_data | 0
        rvfi.rs2_rdata  := rs2_valid ? io.r2rr.rs2_data | 0
        rvfi.rd_addr    := rd_valid ?  decode.rd_addr | 0
        rvfi.rd_wdata   := 0
        rvfi.pc_rdata   := io.f2d.pc
        rvfi.pc_wdata   := 0
        rvfi.mem_addr   := 0
        rvfi.mem_rmask  := 0
        rvfi.mem_wmask  := 0
        rvfi.mem_rdata  := 0
        rvfi.mem_wdata  := 0
    } else null

    val d2e = new Area {
        val d2e_nxt     = Decode2Execute(config).setName("d2e_nxt")

        d2e_nxt.valid           := io.f2d.valid 
        d2e_nxt.pc              := io.f2d.pc
        d2e_nxt.decoded_instr   := decode.decoded_instr
        d2e_nxt.instr           := instr
        d2e_nxt.imm             := imm
        d2e_nxt.rs1_data        := rs1
        d2e_nxt.rs2_data        := rs2

        if (config.hasFormal)
            d2e_nxt.rvfi            := formal.rvfi

        when(io.f2d.valid && !io.e2d.stall){
            io.d2e          := d2e_nxt
        }
        .elsewhen(!io.e2d.stall && io.d2e.valid){
            io.d2e.valid    := False
        }

    }
}


