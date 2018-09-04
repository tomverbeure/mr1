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

