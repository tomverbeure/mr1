
package mr1

import spinal.core._


case class Fetch2Decode(config: MR1Config) extends Bundle {

    val valid               = Bool
    val pc                  = UInt(32 bits)
    val instr               = Bits(32 bits)

    def init() : Fetch2Decode = {
        valid init(False)
        pc    init(0)
        instr init(0)
        this
    }
}

case class Decode2Fetch(config: MR1Config) extends Bundle {
    val stall               = Bool

    val pc_jump_valid       = Bool
    val pc_jump             = UInt(32 bits)

    val rd_addr_valid       = Bool
    val rd_addr             = UInt(5 bits)
}

case class Execute2Fetch(config: MR1Config) extends Bundle {
    val rd_addr_valid       = Bool
    val rd_addr             = UInt(5 bits)
}

class Fetch(config: MR1Config) extends Component {

    val io = new Bundle {
        val instr_req       = InstrReqIntfc(config)
        val instr_rsp       = InstrRspIntfc(config)

        val f2d             =  out(Reg(Fetch2Decode(config)) init)
        val d2f             =  in(Decode2Fetch(config))
        val e2f             =  in(Execute2Fetch(config))

        val rd2r            = out(Read2RegFile(config))
        val r2rd            = in(RegFile2Read(config))
    }

    val fetch_halt = False

    val instr = io.instr_rsp.data
    val opcode = instr(6 downto 0)

    val instr_is_jump = (opcode === Opcodes.JAL)  ||
                        (opcode === Opcodes.JALR) ||
                        (opcode === Opcodes.B)    ||
                        (opcode === Opcodes.SYS)


    val pc = new Area {
        // Keeps track of real, confirmed PC
        val real_pc = Reg(UInt(32 bits)) init(0)
        val real_pc_incr = real_pc + 4

        val send_instr       = False
        val send_instr_r_set = False

        object PcState extends SpinalEnum {
            val Idle           = newElement()
            val WaitReqReady   = newElement()
            val WaitRsp        = newElement()
            val WaitJumpDone   = newElement()
        }

        val cur_state = Reg(PcState()) init(PcState.Idle)

        io.instr_req.valid := False
        io.instr_req.addr  := real_pc

        switch(cur_state){
            is(PcState.Idle){
                when (!fetch_halt && !(io.d2f.stall || io.r2rd.stall)){
                    io.instr_req.valid := True
                    io.instr_req.addr  := real_pc

                    when(io.instr_req.ready){
                        cur_state := PcState.WaitRsp
                    }
                    .otherwise{
                        cur_state := PcState.WaitReqReady
                    }
                }
            }
            is(PcState.WaitReqReady){
                io.instr_req.valid := True
                io.instr_req.addr  := real_pc

                when(io.instr_req.ready){
                    cur_state := PcState.WaitRsp
                }
            }
            is(PcState.WaitRsp){

                when(io.instr_rsp.valid){
                    send_instr       := !(io.d2f.stall || io.r2rd.stall)
                    send_instr_r_set :=  (io.d2f.stall || io.r2rd.stall)

                    real_pc            := real_pc_incr
                    io.instr_req.addr  := real_pc_incr

                    when(instr_is_jump){
                        cur_state := PcState.WaitJumpDone
                    }
                    .elsewhen(fetch_halt || (io.d2f.stall || io.r2rd.stall)){
                        cur_state := PcState.Idle
                    }
                    .otherwise{
                        io.instr_req.valid := True

                        when(io.instr_req.ready){
                            cur_state := PcState.WaitRsp
                        }
                        .otherwise{
                            cur_state := PcState.WaitReqReady
                        }
                    }
                }
            }
            is(PcState.WaitJumpDone){
                when(io.d2f.pc_jump_valid){
                    real_pc := io.d2f.pc_jump

                    when(fetch_halt){
                        cur_state := PcState.Idle
                    }
                    .otherwise{
                        io.instr_req.valid := True
                        io.instr_req.addr  := io.d2f.pc_jump

                        when(io.instr_req.ready){
                            cur_state := PcState.WaitRsp
                        }
                        .otherwise{
                            cur_state := PcState.WaitReqReady
                        }
                    }
                }
            }
        }
    }

    val instr_r = RegNextWhen(instr,      io.instr_rsp.valid) init(0)
    val pc_r    = RegNextWhen(pc.real_pc, io.instr_rsp.valid) init(0)

    val send_instr_r = Reg(Bool) init (False)

    send_instr_r := (pc.send_instr_r_set              ? True   |
                    (!(io.d2f.stall || io.r2rd.stall) ? False  |
                                                        send_instr_r))


    val f2d_nxt = Fetch2Decode(config)

    f2d_nxt := io.f2d

    when(pc.send_instr){
        f2d_nxt.valid := True
        f2d_nxt.pc    := pc.real_pc
        f2d_nxt.instr := instr
    }
    .elsewhen(send_instr_r && !(io.d2f.stall || io.r2rd.stall)){
        f2d_nxt.valid := True
        f2d_nxt.pc    := pc_r
        f2d_nxt.instr := B(instr_r)
    }
    .elsewhen(!(io.d2f.stall || io.r2rd.stall)){
        f2d_nxt.valid := False
        f2d_nxt.pc    := U("32'd0")         // FIXME: replace with pc.real_pc later
        f2d_nxt.instr := B("32'd0")         // FIXME: replace with instr later
    }

    val fetch_active = f2d_nxt.valid && !(io.d2f.stall || io.r2rd.stall)

    io.f2d := f2d_nxt

    val rf = new Area {

        val rs1_valid = True
        val rs2_valid = True

        val rs1_addr    = U(instr(19 downto 15))
        val rs2_addr    = U(instr(24 downto 20))

        val raw_stall = (rs1_valid && ((io.d2f.rd_addr_valid && (rs1_addr === io.d2f.rd_addr && io.d2f.rd_addr =/= 0)) ||
                                       (io.e2f.rd_addr_valid && (rs1_addr === io.e2f.rd_addr && io.e2f.rd_addr =/= 0))    )) ||
                        (rs2_valid && ((io.d2f.rd_addr_valid && (rs2_addr === io.d2f.rd_addr && io.d2f.rd_addr =/= 0)) ||
                                       (io.e2f.rd_addr_valid && (rs2_addr === io.e2f.rd_addr && io.e2f.rd_addr =/= 0))    ))
    
        io.rd2r.rs1_rd := rs1_valid && !(io.d2f.stall || io.r2rd.stall)
        io.rd2r.rs2_rd := rs2_valid && !(io.d2f.stall || io.r2rd.stall)
        io.rd2r.rs1_rd_addr := rs1_valid ? rs1_addr | 0
        io.rd2r.rs2_rd_addr := rs2_valid ? rs2_addr | 0
    }

}


