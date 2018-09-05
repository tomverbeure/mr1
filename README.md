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

Simply FF at output of reg file RAMS (non-functional):

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

    Logic elements: 1690 (from 2137) vs 1582

    Registers: 242 vs 495

