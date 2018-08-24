
package mr1

import spinal.core._

case class MR1Config(
                supportMul      : Boolean = true,
                supportDiv      : Boolean = true,
                supportCsr      : Boolean = true,
                supportFormal   : Boolean = true,
                supportFence    : Boolean = false,
                supportAsyncReg  :Boolean = false
                ) {

    def hasMul      = supportMul
    def hasDiv      = supportDiv
    def hasCsr      = supportCsr
    def hasFence    = supportFence

    def hasAsyncReg = supportAsyncReg

    def hasFormal   = supportFormal
}

case class RVFI(config: MR1Config) extends Bundle {

    val valid       = Bool
    val order       = UInt(64 bits)
    val insn        = Bits(32 bits)
    val trap        = Bool
    val halt        = Bool
    val intr        = Bool
    val rs1_addr    = Bits(5 bits)
    val rs2_addr    = Bits(5 bits)
    val rs1_rdata   = Bits(32 bits)
    val rs2_rdata   = Bits(32 bits)
    val rd_addr     = Bits(5 bits)
    val rd_wdata    = Bits(32 bits)
    val pc_rdata    = Bits(32 bits)
    val pc_wdata    = Bits(32 bits)
    val mem_addr    = Bits(32 bits)
    val mem_rmask   = Bits(4 bits)
    val mem_wmask   = Bits(4 bits)
    val mem_rdata   = Bits(32 bits)
    val mem_wdata   = Bits(32 bits)

    def init() : RVFI = {
        valid init(False)
        this
    }
}

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
    val ALU     = newElement()
    val ALU_I   = newElement()
    val SHIFT   = newElement()
    val SHIFT_I = newElement()
    val FENCE   = newElement()
    val E       = newElement()
    val CSR     = newElement()
    val MUL     = newElement()
    val DIV     = newElement()
}

case class InstrReqIntfc(config: MR1Config) extends Bundle() {

        val valid       = out(Bool)
        val ready       = in(Bool)
        val addr        = out(Bits(32 bits))
}

case class InstrRspIntfc(config: MR1Config) extends Bundle() {

        val valid       = in(Bool)
        val data        = in(Bits(32 bits))
}

class MR1(config: MR1Config) extends Component {
    val io = new Bundle {
        val instr_req = InstrReqIntfc(config).setName("instr_req")
        val instr_rsp = InstrRspIntfc(config).setName("instr_rsp")

        val rvfi        = if (config.hasFormal) out(RVFI(config).setName("rvfi")) else null
    }

    val fetch = new Fetch(config)

    io.instr_req <> fetch.io.instr_req
    io.instr_rsp <> fetch.io.instr_rsp

    val decode = new Decode(config)
    fetch.io.f2d <> decode.io.f2d
    fetch.io.d2f <> decode.io.d2f

    val execute = new Execute(config)
    decode.io.d2e <> execute.io.d2e
    decode.io.e2d <> execute.io.e2d

    val reg_file = new RegFile(config)
    decode.io.d2r   <> reg_file.io.d2r
    reg_file.io.r2e <> execute.io.r2e

    reg_file.io.rd_wr      := False
    reg_file.io.rd_wr_addr := 0
    reg_file.io.rd_wr_data := 0

    if (config.hasFormal)
        io.rvfi <> execute.io.rvfi
}

object MR1Verilog {
    def main(args: Array[String]) {
        SpinalVerilog(new MR1(config = MR1Config(supportFormal = true)))
    }
}

