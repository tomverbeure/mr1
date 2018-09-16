MR1 Project
============

Test project for SpinalHDL

v1.0:

Out of the box synthesis:

* Quartus 13.0 synthesis to EP2C5T144C8: 51.75 MHz
* Megawizard CPU RAM of 4KB
* RegFile infers 2 M4K RAMs
* 2111 logic elements
* 234 registers
* 0 multipliers

With MLAB attribute on RegFile:

* Identical result.
* Cyclone 2 doesn't seem to support MLABs: no such option in MegaWizard either.

* Critical path: from output of register file to input of register file. Not a surprise.

Enable synthesis for Speed (instead of Balanced) and Timing Driven Synthesis:

* 2146 logic elements
* 234 registers
* 48.46 MHz

Remove Formal IOs:

* 2105 logic elements
* 234 registers
* 51 MHZ
* IOW: everything RVFI has been optimized away

Simple FF at output of reg file RAMS (non-functional):

* Clock increases from 50MHz to 66MHz.
* Critical path moves from regfile RAM output to regfile RAM input to regfile output FF to regfile RAM input.
* Paths are all very close to eachother: adder, barrel shifter, ...
* Adds 64 FFs, for a total of 299. As usual, Quartus isn't smart enough to merge these FFs into the RAM itself. In this case, this might be
  a benefit, because a core logic to core logic path may be faster than RAM to core logic.


Initial comparison with equivalent TopMR1 and TopPicoRV32:

MR1 vs PicoRV32: RV32I
* Speed: 50MHz vs 99MHz
* Area:

    Logic elements: 2137 vs 1582

    Registers: 242 vs 404

    Memory bits: 34816 vs 34816

v1.1:

With multiplier but no divide:
* Speed: 43MHz vs 73MHz
* Area:

    Logic elements: 2318 vs 1806

    Registers: 242 vs 495

    Multipliers: 4x 18x18

v1.2:

No multiplier, but barrelshifter refactored:

* Speed: 52MHz vs 99MHz

* Area:

    Logic elements: 1688 (from 2137) vs 1582

    Registers: 242 vs 495

ALU and branch ops refactored:

* Speed: 56MHz vs 99MHz

* Area:

    Logic elements: 1570 (from 2137) vs 1582

    Registers: 242 vs 495


ADD/SUB refactored into a single ADD:

* Speed: 59MHz vs 99MHz

* Area:

    Logic elements: 1525 (from 2137) vs 1582

    Registers: 242 vs 495


General IMM moved to Decode stage:

* Speed: 51MHz vs 99MHz

* Area:

    Logic elements: 1505 (from 2137) vs 1582

    Registers: 263 vs 495

    Weird timing path with cascaded carry chains.


Convert priority mux of execute.rd\_wdata to flat and/or reduction

* Speed: 52MHz vs 99MHz

* Area:

    Logic elements: 1479 (from 2137) vs 1582

    Registers: 263 vs 495

Break cascaded carry chains:

* Speed: 60MHz vs 99MHz

* Area:

    Logic elements: 1485 (from 2137) vs 1582

    Registers: 263 vs 495

    Critical path is now in barrel shifter

Simple FF at output of reg file RAMS (non-functional):

* Speed: 72MHz vs 99MHz

* Area:

    Logic elements: 1496 (from 2137) vs 1582

    Registers: 327 vs 495

    Critical path is now in barrel shifter

Remove lsu.rd\_wdata from unnecessary conditional

* Speed: 54MHz vs 99MHz

* Area:

    Logic elements: 1462 (from 2137) vs 1582

    Registers: 263 vs 495

    Critical path is in branch compare

Major rework. Move register file issue to Fetch.

* Speed: 73MHz vs 99MHz

* Area:

    Logic elements: 1501 vs 1582

    Registers: 328 vs 495

    Critical path is in barrel shifter.

Move ALU imm to Decode

* Speed: 77MHz vs 99MHz

* Area:

    Logic elements: 1528 vs 1582

    Registers: 328 vs 495

    Critical path is in barrel shifter.

Move Shamt to Decode

* Speed: 80MHz vs 99MHz

* Area:

    Logic elements: 1467 vs 1582

    Registers: 323 vs 495

    Critical path from branch to fetch

Move AUIPC and LUI to ALU.

* Speed: 80MHz vs 99MHz

* Area:

    Logic elements: 1427 vs 1582

    Registers: 325 vs 495

    Critical path in ALU_ADD

Make ALU\_ADD the default rd\_wdata result

* Speed: 81MHz vs 99MHz

* Area:

    Logic elements: 1392 vs 1582

    Registers: 325 vs 495

    Critical path from data RAM into reg file

Merge SLT and SLTU

* Speed: 81MHz vs 99MHz

* Area:

    Logic elements: 1374 vs 1582

    Registers: 325 vs 495

    Critical path through ALU_ADD

Reduce imm from 32 to 21 bits.

* Speed: 80MHz vs 99MHz

* Area:

    Logic elements: 1338 vs 1582

    Registers: 313 vs 495

    Critical path from data RAM to reg file

Merge BLT/BLTU with SLT/SLTU

* Speed: 79MHz vs 99MHz

* Area:

    Logic elements: 1320 vs 1582

    Registers: 313 vs 495

    Critical path from data RAM to reg file

jump.rd\_wdata is always pc\_plus4

* Speed: 82MHz vs 99MHz

* Area:

    Logic elements: 1324 vs 1582

    Registers: 313 vs 495

    Critical path from data RAM to reg file

Non-32-bit memory operations were completely broken. GASP.

* Speed: 82MHz vs 99MHz

* Area:

    Logic elements: 1358 vs 1582

    Registers: 313 vs 495

    Critical path from data RAM to reg file

v2.0

Remove iformat from Execute

* Speed: 82MHz vs 99MHz

* Area:

    Logic elements: 1336 vs 1582

    Registers: 311 vs 495

    Critical path execute.rs2 to instr RAM

Reduce PC to 12 bits

* Speed: 82MHz vs 99MHz

* Area:

    Logic elements: 1171 vs 1582

    Registers: 230 vs 495

    Critical path execute.rs2 to instr RAM

Use ALU\_ADD for LSU calc and part of jump

* Speed: 76MHz vs 99MHz

* Area:

    Logic elements: 1298 vs 1582

    Registers: 323 vs 495

    Critical path <less than> to fetch to RAM (BLT)

Jump doesn't use ALU\_ADD anymore

* Speed: 80MHz vs 99MHz

* Area:

    Logic elements: 1338 vs 1582

    Registers: 323 vs 495

    Critical path <less than> to fetch to RAM (BLT)

LT merged with ALU\_ADD. ALU\_ADD split into Decoder

* Speed: 84MHz vs 99MHz

* Area:

    Logic elements: 1314 vs 1582

    Registers: 333 vs 495

    Critical path from instr RAM to pc.

Simplify Decoder

* Speed: 78MHz vs 99MHz (???)

* Area:

    Logic elements: 1320 vs 1582

    Registers: 333 vs 495

    Critical path in data load path to reg_wr

IFormat -> one-hot

* Speed: 83MHz vs 99MHz (???)

* Area:

    Logic elements: 1403 vs 1582

    Registers: 333 vs 495

    Critical path in data load path to reg_wr

IType and Fetch.PcState -> one-hot + SpinalHDL one-hot fix.

* Speed: 83MHz vs 99MHz 

* Area:

    Logic elements: 1259 vs 1582

    Registers: 336 vs 495

    Critical path in data load path to reg_wr

Working Writeback stage

* Speed: 76MHz vs 99MHz (Uses non-one-hot SpinalHDL version)

* Area:

    Logic elements: 1478 vs 1582

    Registers: 386 vs 495

    Critical path in fetch.

