
package mr1

import spinal.core._

object InstrType extends SpinalEnum {
    val Undef   = newElement()
    val R       = newElement()
    val I       = newElement()
    val S       = newElement()
    val B       = newElement()
    val U       = newElement()
    val J       = newElement()
}

case class DecodedInstr(hasMul: Boolean, hasDiv: Boolean, hasCsr: Boolean) extends Bundle {

    val itype   = InstrType()

    val lui     = Bool
    val auipc   = Bool
    val jal     = Bool
    val jalr    = Bool
    val b       = Bool
    val l       = Bool
    val s       = Bool
    val alu_i   = Bool
    val shift_i = Bool
    val alu     = Bool
    val shift   = Bool
    val fence   = Bool
    val e       = Bool
    val csr     = if (hasCsr) Bool else null
    val mul     = if (hasMul) Bool else null
    val div     = if (hasDiv) Bool else null
}


class Decode extends Component {

    val hasMul = true
    val hasDiv = true
    val hasCsr = false

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

    val decoded_instr       = DecodedInstr(hasMul, hasDiv, hasCsr)

    decoded_instr.itype     := InstrType.Undef
    decoded_instr.lui       := False
    decoded_instr.auipc     := False
    decoded_instr.jal       := False
    decoded_instr.jalr      := False
    decoded_instr.b         := False
    decoded_instr.l         := False
    decoded_instr.s         := False
    decoded_instr.alu_i     := False
    decoded_instr.shift_i   := False
    decoded_instr.alu       := False
    decoded_instr.shift     := False
    decoded_instr.fence     := False
    decoded_instr.e         := False
    if (hasCsr){
        decoded_instr.csr       := False
    }
    if (hasMul){
        decoded_instr.mul       := False
    }
    if (hasDiv){
        decoded_instr.div       := False
    }

    switch(opcode){
        // LUI
        is(B"0110111"){
            decoded_instr.lui       := True
            decoded_instr.itype     := InstrType.U
        }
        // AUIPC
        is(B"0010111"){
            decoded_instr.auipc     := True
            decoded_instr.itype     := InstrType.U
        }
        // JAL
        is(B"1101111"){
            decoded_instr.jal       := True
            decoded_instr.itype     := InstrType.J
        }
        // JALR
        is(B"1100111"){
            when(funct3 === B"000") {
                decoded_instr.jalr      := True
                decoded_instr.itype     := InstrType.I
            }
        }
        // Bxx
        is(B"1100011"){
            when(funct3 =/= B"010" && funct3 =/= B"011") {
                decoded_instr.b         := True
                decoded_instr.itype     := InstrType.B
            }
        }
        // Lxx
        is(B"0000011"){
            when(funct3 =/= B"010" && funct3 =/= B"011" && funct3 =/= B"110" && funct3 =/= B"111") {
                decoded_instr.l         := True
                decoded_instr.itype     := InstrType.I
            }
        }
        // Sx
        is(B"0100011"){
            when(funct3 === B"000" || funct3 === B"001" || funct3 === B"010") {
                decoded_instr.s         := True
                decoded_instr.itype     := InstrType.S
            }
        }
        is(B"0010011"){
            when(funct3 === B"000" || funct3 === B"010" || funct3 === B"011" || funct3 === B"100" || funct3 === B"110" || funct3 === B"111") {
                // ALU_I
                decoded_instr.alu_i     := True
                decoded_instr.itype     := InstrType.I
            }.elsewhen( (funct7 ## funct3) === B"0000000001" || (funct7 ## funct3) === B"0000000101" || (funct7 ## funct3) === B"0100000101") {
                // SHIFT_I
                decoded_instr.shift_i   := True
                decoded_instr.itype     := InstrType.R
            }
        }
        // ALU
        is(B"0110011"){
            when( (funct7 ## funct3) === B"0000000000" || (funct7 ## funct3) === B"0100000000"){
                // ADD, SUB
                decoded_instr.alu       := True
                decoded_instr.itype     := InstrType.R
            }
            .elsewhen( (funct7 ## funct3) === B"0000000001"){
                // SLL
                decoded_instr.shift       := True
                decoded_instr.itype     := InstrType.R
            }
            .elsewhen( (funct7 ## funct3) === B"0000000010" || (funct7 ## funct3) === B"0000000011") {
                // SLT, SLTU
                decoded_instr.alu       := True
                decoded_instr.itype     := InstrType.R
            }
        }
    }

}


