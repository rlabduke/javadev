# (jEdit options) :folding=explicit:collapseFolds=1:
#
#{{{ Notes on usage
# Standard generalized Makefile for building
# Java projects under GNU/Linux systems.
#
# Project 'quickies' begun on Tue Feb 18 17:59:48 EST 2003
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
DEVROOT  = ../..
CLASSES  = $(DEVROOT)/classes
JARS     = $(DEVROOT)/jars
OLD      = $(DEVROOT)/old

# Project details
PROJNAME = quickies
LONGVER := $(shell date +%y%m%d.%H%M)
PACKAGE  = quickies
PKGPATH  = quickies
# Variables }}}

############################################################
### TARGETS ################################################
############################################################
.PHONY: all back clean try

# Make a copy of all the files in this directory
back : clean
	mkdir -p $(OLD)/$(PROJNAME)
	tar cvzf $(OLD)/$(PROJNAME)/$(PROJNAME)-$(LONGVER).tgz .
	scp $(OLD)/$(PROJNAME)/$(PROJNAME)-$(LONGVER).tgz iwd@server:~/devel/old-versions

# Clean up - delete binaries, JAR files, etc.
clean :
	rm -rf *.class

