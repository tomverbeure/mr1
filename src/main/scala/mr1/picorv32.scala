
package mr1

import spinal.core._


class picorv32(
        enableCounters      : Boolean = true,
        enableCounter64     : Boolean = true,
        enableRegs1631      : Boolean = true,
        enableRegsDualPort  : Boolean = true,
        latchedMemRData     : Boolean = false,
        twoStageShift       : Boolean = true,
        barrelShifter       : Boolean = false,
        twoCycleCompare     : Boolean = false,
        twoCycleAlu         : Boolean = false,
        compressedIsa       : Boolean = false,
        catchMisalign       : Boolean = true,
        catchIllinsn        : Boolean = true,
        enablePcpi          : Boolean = false,
        enableMul           : Boolean = false,
        enableFastMul       : Boolean = false,
        enableDiv           : Boolean = false,
        enableIreq          : Boolean = false,
        enableIrqQregs      : Boolean = true,
        enableIrqTimer      : Boolean = true,
        enableTrace         : Boolean = false,
        enableInitZero      : Boolean = false,
    
        maskedIrq           : Int   = 0x00000000,
        latchedIrq          : Int   = 0xffffffff,
        progaddrReset       : Int   = 0x00000000,
        progaddrIrq         : Int   = 0x00000010,
        stackaddr           : Int   = 0xffffffff
    
    ) extends BlackBox {

    addGeneric("ENABLE_COUNTERS", enableCounters)
    addGeneric("ENABLE_COUNTERS64", enableCounter64)
    addGeneric("ENABLE_REGS_16_31", enableRegs1631)
    addGeneric("ENABLE_REGS_DUALPORT", enableRegsDualPort)
    addGeneric("LATCHED_MEM_RDATA", latchedMemRData)
    addGeneric("TWO_STAGE_SHIFT",twoStageShift)
    addGeneric("BARREL_SHIFTER", barrelShifter)
    addGeneric("TWO_CYCLE_COMPARE", twoCycleCompare)
    addGeneric("TWO_CYCLE_ALU", twoCycleAlu)
    addGeneric("COMPRESSED_ISA", compressedIsa)
    addGeneric("CATCH_MISALIGN", catchMisalign)
    addGeneric("CATCH_ILLINSN", catchIllinsn)
    addGeneric("ENABLE_PCPI", enablePcpi)
    addGeneric("ENABLE_MUL", enableMul)
    addGeneric("ENABLE_FAST_MUL", enableFastMul)
    addGeneric("ENABLE_DIV", enableDiv)
    addGeneric("ENABLE_IRQ", enableIreq)
    addGeneric("ENABLE_IRQ_QREGS", enableIrqQregs)
    addGeneric("ENABLE_IRQ_TIMER", enableIrqTimer)
    addGeneric("ENABLE_TRACE", enableTrace)
    addGeneric("REGS_INIT_ZERO", enableInitZero)

    addGeneric("MASKED_IRQ", maskedIrq)
    addGeneric("LATCHED_IRQ", latchedIrq)
    addGeneric("PROGADDR_RESET", progaddrReset)
    addGeneric("PROGADDR_IRQ", progaddrIrq)
    addGeneric("STACKADDR", stackaddr)

    val io = new Bundle {
        val clk         = in(Bool)
        val resetn      = in(Bool)

        val trap        = out(Bool)

        val mem_valid   = out(Bool)
        val mem_instr   = out(Bool)
        val mem_ready   = in(Bool)

        val mem_addr    = out(Bits(32 bits))
        val mem_wdata   = out(Bits(32 bits))
        val mem_wstrb   = out(Bits(4 bits))
        val mem_rdata   = in(Bits(32 bits))

        // Look-Ahead Interface
        val mem_la_read     = out(Bool)
        val mem_la_write    = out(Bool)
        val mem_la_addr     = out(Bits(32 bits))
        val mem_la_wdata    = out(Bits(32 bits))
        val mem_la_wstrb    = out(Bits(4 bits))

        // Pico Co-Processor Interface (PCPI)
        val pcpi_valid      = out(Bool)
        val pcpi_insn       = out(Bits(32 bits))
        val pcpi_pcpi_rs1   = out(Bits(32 bits))
        val pcpi_pcpi_rs2   = out(Bits(32 bits))
        val pcpi_wr         = in(Bool)
        val pcpi_rd         = in(Bits(32 bits))
        val pcpi_wait       = in(Bool)
        val pcpi_ready      = in(Bool)

        // IRQ Interface
        val irq             = in(Bits(32 bits))
        val eoi             = out(Bits(32 bits))

        // Trace Interface
        val trace_valid     = out(Bool)
        val trace_data      = out(Bits(36 bits))
    }

    mapCurrentClockDomain(io.clk)
    noIoPrefix()
}

