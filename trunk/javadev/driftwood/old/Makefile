# (jEdit options) :folding=explicit:collapseFolds=1:
#
#{{{ Notes on usage
# Standard generalized Makefile for building
# Java projects under GNU/Linux systems.
#
# Project 'isosurface' begun on Sun Feb  9 17:15:00 EST 2003
# Copyright (C) 2003 Ian W. Davis
#
# COMMON TARGETS:
#   % make all          Builds Java components
#   % make back         Create .tgz file for backup
#   % make clean        Remove all generated files
#   % make try          Build all & run sample command line
#
# Notes on usage }}}

#{{{ Variables
############################################################
### VARIABLES ##############################################
############################################################

# Directory structures
DEVROOT  = ../../..
CLASSES  = $(DEVROOT)/classes
JARS     = $(DEVROOT)/jars
OLD      = $(DEVROOT)/old

# Project details
PROJNAME = util-isosurface
PROJVER := $(shell gawk 'match($$0,/VERSION *= *"([^"]+)"/,m){print m[1]}' Version.java)
LONGVER := $(PROJVER).$(shell date +%y%m%d.%H%M)
PACKAGE  = util.isosurface
PKGPATH  = util/isosurface

# Target names (JAR file, binary/library)
JTARGET   = $(PROJNAME).jar
# Source files
JFILES   := $(wildcard *.java)
# Object & class files (defaults to one per source file)
JCLASSES := $(patsubst %.java,%.class,$(JFILES))
# Flags for compilers, etc.
JAVACFLAGS = -classpath $(CLASSES) -d $(CLASSES)

# Additional resources
#JARCLASSES = util/
JARRC     = rc/
# Variables }}}

#{{{ Standard targets
############################################################
### TARGETS ################################################
############################################################
.PHONY: all back clean try

# The Java component
all : $(JTARGET)

# The (real) Java component
$(JTARGET) : $(JFILES)
	javac $(JAVACFLAGS) $(JFILES)
	jar cf $(JTARGET) -C $(CLASSES) $(PKGPATH) $(addprefix -C $(CLASSES) ,$(JARCLASSES)) $(JARRC)
	jar i $(JTARGET)
	cp $(JTARGET) $(JARS)

# Make a copy of all the files in this directory
back : clean
	mkdir -p $(OLD)/$(PROJNAME)
	tar cvzf $(OLD)/$(PROJNAME)/$(PROJNAME)-$(LONGVER).tgz . --exclude ed
	scp $(OLD)/$(PROJNAME)/$(PROJNAME)-$(LONGVER).tgz iwd@server:~/devel/old-versions

# Clean up - delete binaries, JAR files, etc.
clean :
	rm -rf $(CLASSES)/$(PKGPATH) $(JTARGET)

# Standard targets }}}

# Run the program to test it out
try : all
#	java -cp $(JTARGET) $(PACKAGE).Try -color blue	       < ed/1ubq.xmap > ed/1ubq-xmap.kin
#	java -cp $(JTARGET) $(PACKAGE).Try -color orange -omap < ed/1ubq.dsn6 > ed/1ubq-dsn6.kin
#	java -cp $(JTARGET) $(PACKAGE).Try < ed/1ubq.refit.gen.2fofc.map > ed/1ubq-map.kin
#
#	java -cp $(JTARGET) $(PACKAGE).Try -color blue          < ed/ref.BS.rd0.1sbp_2fofc.xmap > ed/1sbp-xmap.kin
#	java -cp $(JTARGET) $(PACKAGE).Try -color sky           < ed/ref.BS.rd0.1sbp_2fofc.backtrans.xplor.gz > ed/1sbp-btxplor.kin
#	java -cp $(JTARGET) $(PACKAGE).Try -color orange  -omap < ed/ref.BS.rd0.1sbp_2fofc.dsn6 > ed/1sbp-dsn6.kin
	java -cp $(JTARGET) $(PACKAGE).Try -color sky -omap < ed/ref.BS.rd0.1sbp_2fofc.brix.gz > ed/1sbp-brix.kin
#
#	java -cp $(JTARGET) $(PACKAGE).Try -color hotpink < ed/2aak.xmap.gz > ed/2aak-xmap.kin
