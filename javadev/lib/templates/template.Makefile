# (jEdit options) :folding=explicit:collapseFolds=1:
#
#{{{ Notes on usage
# Standard generalized Makefile for building
# Java projects under GNU/Linux systems.
#
# Project '|PACKAGE|' begun on |DATE|
# Copyright (C) |YEAR| Ian W. Davis
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
DEVROOT  = |DEVROOT|
CLASSES  = $(DEVROOT)/classes
JARS     = $(DEVROOT)/jars
OLD      = $(DEVROOT)/old

# Project details
PROJNAME = |PKGDASH|
PROJVER := $(shell gawk 'match($$0,/VERSION *= *"([^"]+)"/,m){print m[1]}' Version.java)
LONGVER := $(PROJVER).$(shell date +%y%m%d.%H%M)
PACKAGE  = |PACKAGE|
PKGPATH  = |PKGPATH|

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
	tar cvzf $(OLD)/$(PROJNAME)/$(PROJNAME)-$(LONGVER).tgz .
	scp $(OLD)/$(PROJNAME)/$(PROJNAME)-$(LONGVER).tgz iwd@server:~/devel/old-versions

# Clean up - delete binaries, JAR files, etc.
clean :
	rm -rf $(CLASSES)/$(PKGPATH) $(JTARGET)

# Standard targets }}}

# Run the program to test it out
try : all
	echo "*** Set up your own sample command line for |PACKAGE| ***"
#	java -cp $(JTARGET) $(PACKAGE).Try
