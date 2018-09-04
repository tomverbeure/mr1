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

