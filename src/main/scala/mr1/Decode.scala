
package mr1

import spinal.core._

object InstrFormat extends SpinalEnum {
    val Undef   = newElement()
    val R       = newElement()
    val I       = newElement()
    val S       = newElement()
    val B       = newElement()
    val U       = newElement()
    val J       = newElement()
}

object InstrType extends SpinalEnum {
    val LUI     = newElement()
    val AUIPC   = newElement()
    val JAL     = newElement()
    val JALR    = newElement()
    val B       = newElement()
    val L       = newElement()
    val S       = newElement()
    val ALU_I   = newElement()
    val SHIFT_I = newElement()
    val ALU     = newElement()
    val SHIFT   = newElement()
    val FENCE   = newElement()
    val E       = newElement()
    val CSR     = newElement()
    val MUL     = newElement()
    val DIV     = newElement()
}

case class DecodedInstr(config: MR1Config) extends Bundle {

    val iformat = InstrFormat()
    val itype   = InstrType()
}


class Decode(config: MR1Config) extends Component {

    val hasMul = config.hasMul
    val hasDiv = config.hasMul
    val hasCsr = config.hasMul

    val io = new Bundle {
        val instr_valid = in Bool
        val instr       = in Bits(32 bits)

        val stall       = out Bool
    }

    val opcode      = io.instr(6 downto 0)
    val funct3      = io.instr(14 downto 12)
    val funct7      = io.instr(31 downto 25)
    val rd          = io.instr(11 downto 7)
    val rs1         = io.instr(19 downto 15)
    val rs2         = io.instr(24 downto 20)

    val i_imm_11_0  = io.instr(31 downto 20)
    val s_imm_11_0  = io.instr(31 downto 25) ## io.instr(11 downto 7)
    val b_imm_12_1  = io.instr(31) ## io.instr(7) ## io.instr(30 downto 25) ## io.instr(11 downto 8)
    val u_imm_31_12 = io.instr(31 downto 12)
    val j_imm_20_1  = io.instr(20) ## io.instr(19 downto 12) ## io.instr(20) ## io.instr(30 downto 21)

    val decoded_instr       = DecodedInstr(config)

    decoded_instr.iformat   := InstrFormat.Undef
    decoded_instr.itype     := InstrType.LUI

    switch(opcode){
        // LUI
        is(B"0110111"){
            decoded_instr.itype     := InstrType.LUI
            decoded_instr.iformat   := InstrFormat.U
        }
        // AUIPC
        is(B"0010111"){
            decoded_instr.itype     := InstrType.AUIPC
            decoded_instr.iformat   := InstrFormat.U
        }
        // JAL
        is(B"1101111"){
            decoded_instr.itype     := InstrType.JAL
            decoded_instr.iformat   := InstrFormat.J
        }
        // JALR
        is(B"1100111"){
            when(funct3 === B"000") {
                decoded_instr.itype     := InstrType.JALR
                decoded_instr.iformat   := InstrFormat.I
            }
        }
        // Bxx
        is(B"1100011"){
            when(funct3 =/= B"010" && funct3 =/= B"011") {
                decoded_instr.itype     := InstrType.B
                decoded_instr.iformat   := InstrFormat.B
            }
        }
        // Lxx
        is(B"0000011"){
            when(funct3 =/= B"010" && funct3 =/= B"011" && funct3 =/= B"110" && funct3 =/= B"111") {
                decoded_instr.itype     := InstrType.L
                decoded_instr.iformat   := InstrFormat.I
            }
        }
        // Sx
        is(B"0100011"){
            when(funct3 === B"000" || funct3 === B"001" || funct3 === B"010") {
                decoded_instr.itype     := InstrType.S
                decoded_instr.iformat   := InstrFormat.S
            }
        }
        is(B"0010011"){
            when(funct3 === B"000" || funct3 === B"010" || funct3 === B"011" || funct3 === B"100" || funct3 === B"110" || funct3 === B"111") {
                // ALU_I
                decoded_instr.itype     := InstrType.ALU_I
                decoded_instr.iformat   := InstrFormat.I
            }.elsewhen( (funct7 ## funct3) === B"0000000001" || (funct7 ## funct3) === B"0000000101" || (funct7 ## funct3) === B"0100000101") {
                // SHIFT_I
                decoded_instr.itype     := InstrType.SHIFT_I
                decoded_instr.iformat   := InstrFormat.R
            }
        }
        // ALU, SHIFT
        is(B"0110011"){
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
                            decoded_instr.itype     := InstrType.MUL
                            decoded_instr.iformat   := InstrFormat.R
                        }
                    }
                }
                is(B"0000001_100", B"0000001_101", B"0000001_110", B"0000001_111"){
                    // DIV
                    if (hasDiv){
                        when(funct7 === B"0000001"){
                            decoded_instr.itype     := InstrType.DIV
                            decoded_instr.iformat   := InstrFormat.R
                        }
                    }
                }
            }
        }
        // FENCE
        is(B"0001111"){
            when( funct3 === B"000" || funct3 === B"001"){
                decoded_instr.itype     := InstrType.FENCE
                decoded_instr.iformat   := InstrFormat.I
            }
        }
        // ECALL, EBREAK, CSR
        is(B"1110011"){
            when( io.instr(31 downto 7) === B"0000_0000_0000_0000_0000_0000_0" || io.instr(31 downto 7) === B"0000_0000_0001_0000_0000_0000_0")
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


