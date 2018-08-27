
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
}

class Fetch(config: MR1Config) extends Component {

    val io = new Bundle {
        val instr_req       = InstrReqIntfc(config)
        val instr_rsp       = InstrRspIntfc(config)

        val f2d             =  out(Fetch2Decode(config))
        val d2f             =  in(Decode2Fetch(config))
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
        io.instr_req.addr  := B(real_pc)

        switch(cur_state){
            is(PcState.Idle){
                when (!fetch_halt && !io.d2f.stall){
                    io.instr_req.valid := True
                    io.instr_req.addr  := B(real_pc)

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
                io.instr_req.addr  := B(real_pc)

                when(io.instr_req.ready){
                    cur_state := PcState.WaitRsp
                }
            }
            is(PcState.WaitRsp){

                when(io.instr_rsp.valid){
                    send_instr       := !io.d2f.stall
                    send_instr_r_set :=  io.d2f.stall

                    when(instr_is_jump){
                        cur_state := PcState.WaitJumpDone
                    }
                    .elsewhen(fetch_halt || io.d2f.stall){
                        real_pc   := real_pc_incr
                        cur_state := PcState.Idle
                    }
                    .otherwise{
                        io.instr_req.valid := True
                        io.instr_req.addr  := B(real_pc_incr)
                        real_pc := real_pc_incr

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
                        io.instr_req.addr  := B(io.d2f.pc_jump)

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

    val instr_r = RegNextWhen(instr, io.instr_rsp.valid) init(0)

    val send_instr_r = Reg(Bool) init (False)

    send_instr_r := (pc.send_instr_r_set ? True   |
                    (!io.d2f.stall       ? False  |
                                           send_instr_r))

    val f2d = Reg(Fetch2Decode(config)) init()

    when(pc.send_instr){
        f2d.valid := True
        f2d.pc    := pc.real_pc
        f2d.instr := instr
    }
    .elsewhen(send_instr_r && !io.d2f.stall){
        f2d.valid := True
        f2d.pc    := pc.real_pc
        f2d.instr := B(instr_r)
    }
    .elsewhen(!io.d2f.stall){
        f2d.valid := False
        f2d.pc    := U("32'd0")         // FIXME: replace with pc.real_pc later
        f2d.instr := B("32'd0")         // FIXME: replace with instr later
    }

    io.f2d := f2d
}


