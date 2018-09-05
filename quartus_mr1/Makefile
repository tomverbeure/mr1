
QUARTUS_ROOTDIR ?= `realpath ~/altera/13.0sp1/quartus`
PROJECT_NAME    = mr1

synth: progmem
	cp ../sw/progmem.mif cpu_ram.mif
	$(QUARTUS_ROOTDIR)/bin/quartus_map --read_settings_files=on  --write_settings_files=off $(PROJECT_NAME) -c $(PROJECT_NAME)
	$(QUARTUS_ROOTDIR)/bin/quartus_fit --read_settings_files=off --write_settings_files=off $(PROJECT_NAME) -c $(PROJECT_NAME)
	$(QUARTUS_ROOTDIR)/bin/quartus_asm --read_settings_files=off --write_settings_files=off $(PROJECT_NAME) -c $(PROJECT_NAME)

update_ram: progmem
	cp ../sw/progmem.mif cpu_ram.mif
	$(QUARTUS_ROOTDIR)/bin/quartus_cdb $(PROJECT_NAME) -c $(PROJECT_NAME) --update_mif
	$(QUARTUS_ROOTDIR)/bin/quartus_asm --read_settings_files=off --write_settings_files=off $(PROJECT_NAME) -c $(PROJECT_NAME)

.PHONY: progmem
progmem:
	cd ../sw && make

check:
	$(QUARTUS_ROOTDIR)/bin/quartus_pgm -a

pgm:
	$(QUARTUS_ROOTDIR)/bin/quartus_pgm $(PROJECT_NAME).cdf

clean:
	\rm -fr db incremental_db simulation Chain1.cdf progmem*.mif progmem*.hex

jtag:
	sudo LD_LIBRARY_PATH=$(QUARTUS_ROOTDIR)/linux64 $(QUARTUS_ROOTDIR)/linux64/jtagconfig

