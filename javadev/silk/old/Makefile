# (jEdit options) :folding=explicit:collapseFolds=1:
#{{{ Notes
#
# Standard generalized Makefile for building projects with
# Java and C++ components under GNU/Linux systems.
#
# By default, C++ is disabled, but the mechanisms are here.
#
# Project 'boundrotamers' begun on Fri May 17 10:22:49 EDT 2002
# Copyright (C) 2002 Ian W. Davis
#
# COMMON TARGETS:
#   % make all          Builds C++ and Java components
#   % make java         Java component only
#   % make cpp          C++ component only
#   % make back         Create .tgz file for backup
#   % make clean        Remove all generated files
#   % make try          Build all & run sample command line
#
#}}}

#{{{ Variables
############################################################
### VARIABLES ##############################################
############################################################

# Directory structures
DEVROOT  = ../..
BIN      = $(DEVROOT)/bin
CLASSES  = $(DEVROOT)/classes
JARS     = $(DEVROOT)/jars
OLD      = $(DEVROOT)/old

# Project details
PROJNAME = boundrotamers
PROJVER := $(shell $(BIN)/getversion Version.java)
LONGVER := $(PROJVER).$(shell date +%y%m%d.%H%M)
PACKAGE  = boundrotamers
PKGPATH  = boundrotamers

# Target names (JAR file, binary/library)
JTARGET   = $(PROJNAME).jar
CXXTARGET = $(PROJNAME)

# Source files
JFILES   := $(wildcard *.java)
CXXFILES := $(wildcard *.cpp)
HEADERS  := $(wildcard *.h)

# Object & class files (defaults to one per source file)
JCLASSES := $(patsubst %.java,%.class,$(JFILES))
JAREXTRAS = util/
#JARRC     = rc/
OBJS     := $(patsubst %.cpp,%.o,$(CXXFILES))

# Tools
CP    = cp
CXX   = g++
JAR   = jar
JAVA  = java
JAVAC = javac
MKDIR = mkdir
RM    = rm
SCP   = scp
TAR   = tar

# Flags for compilers, etc.
JAVACFLAGS = -classpath $(CLASSES):util-isosurface.jar -d $(CLASSES)
CXXFLAGS   =
LINKFLAGS  =

# C++ libraries, headers, etc. in other locations
#     Libraries to link in: libfoobar.(so|a) is -lfoobar
CXXLIBS =
#     Directories to search for included files: -Isomedir
CXXINCDIR =
#     Directories to search for libraries in $(CXXLIBS): -Lsomedir
CXXLIBDIR =
#}}}

#{{{ Patterns
############################################################
### PATTERN RULES ##########################################
############################################################
%.o : %.cpp $(HEADERS)
	$(CXX) -c $(CXXFLAGS) $< $(CXXINCDIR) -o $@
#}}}

#{{{ Targets
############################################################
### TARGETS ################################################
############################################################
.PHONY: all java cpp back clean try

# Both the Java component and the C++ component
#all : java cpp
all : java

# The Java component
java : $(JTARGET)

# The (real) Java component
$(JTARGET) : $(JFILES)
	$(JAVAC) $(JAVACFLAGS) $(JFILES)
	$(JAR) cmf Manifest.mf $(JTARGET) -C $(CLASSES) $(PKGPATH) $(addprefix -C $(CLASSES) ,$(JAREXTRAS)) $(JARRC)
	$(CP) $(JTARGET) $(JARS)

# The C++ component
cpp : $(CXXTARGET)

# The (real) C++ component
$(CXXTARGET) : $(OBJS)
	$(CXX) $(LINKFLAGS) $(OBJS) $(CXXLIBDIR) $(CXXLIBS) -o $(CXXTARGET)

# Make a copy of all the files in this directory
back : clean
	$(MKDIR) -p $(OLD)/$(PROJNAME)
	$(TAR) cvzf $(OLD)/$(PROJNAME)/$(PROJNAME)-$(LONGVER).tgz . --exclude="rota.0*"
	$(SCP) $(OLD)/$(PROJNAME)/$(PROJNAME)-$(LONGVER).tgz iwd@server:~/devel/old-versions

# Clean up - delete binaries, JAR files, etc.
clean :
	$(RM) -f $(wildcard $(CLASSES)/$(PKGPATH)/*.class) $(OBJS) $(JTARGET) $(CXXTARGET)

#}}}

.PHONY : r1 r2 r3 r4 bbdep rota rama bbang

# Run the program to test it out
ROTAOPTS1 = -fields=6,10 -blt=40 -interval=1 -mask=10,14 -contour=:98,95,90,80 -vscale=360
ROTAOPTS2 = -fields=6,7,10 -blt=40 -interval=5 -mask=10,14 -contour=:98,95,90,80
ROTAOPTS3 = -fields=6,7,8,10 -blt=40 -interval=8 -mask=10,14 -contour=:95,90,80
ROTAOPTS4 = -fields=6,7,8,9,10 -blt=40 -interval=10 -mask=10,14 -contour=:95
ROTAOPTS123 = -fields=6,7,8,10 -blt=40 -interval=10 -mask=10,14 -contour=:95,90,80
ROTAOPTS234 = -fields=7,8,9,10 -blt=40 -interval=10 -mask=10,14 -contour=:95,90,80

rota : all r1 r2 r3 r4

#{{{ 1-D rotamers
r1 : all
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota1D -output=ser -name="Ser" $(ROTAOPTS1) rotatabs/Ser-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota1D -output=pro -name="Pro" $(ROTAOPTS1) rotatabs/Pro-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota1D -output=thr -name="Thr" $(ROTAOPTS1) rotatabs/Thr-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota1D -output=val -name="Val" $(ROTAOPTS1) rotatabs/Val-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota1D -output=cys -name="Cys" $(ROTAOPTS1) rotatabs/Cys-clash.tab
#}}}

#{{{ 2-D rotamers
r2 : all
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=phetyr -name="Tyr/Phe" $(ROTAOPTS2) -limit2 rotatabs/Tyr-clash.tab rotatabs/Phe-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=ile -name="Ile" $(ROTAOPTS2) rotatabs/Ile-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=leu-chop -name="Leu" $(ROTAOPTS2) rotatabs/Leu-clash-chopped.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=leu-nochop -name="Leu" $(ROTAOPTS2) rotatabs/Leu-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=his -name="His" $(ROTAOPTS2) rotatabs/His-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=trp -name="Trp" $(ROTAOPTS2) rotatabs/Trp-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=asp -name="Asp" $(ROTAOPTS2) -limit2 rotatabs/Asp-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=asn -name="Asn" $(ROTAOPTS2) rotatabs/Asn-clash.tab
#}}}

#{{{ 3-D rotamers
r3 : all
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=met -name="Met" $(ROTAOPTS3) rotatabs/Met-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=glu -name="Glu" $(ROTAOPTS3) -limit3 rotatabs/Glu-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=gln -name="Gln" $(ROTAOPTS3) rotatabs/Gln-clash.tab
#}}}

#{{{ 4-D rotamers
r4 : all
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota4D -output=lys -name="Lys" $(ROTAOPTS4) rotatabs/Lys-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=lys123 -name="Lys-123" $(ROTAOPTS123) rotatabs/Lys-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=lysp -name="Lys-plus" $(ROTAOPTS234) rotatabs/Lys-cplus.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=lyst -name="Lys-trans" $(ROTAOPTS234) rotatabs/Lys-ctrans.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=lysm -name="Lys-minus" $(ROTAOPTS234) rotatabs/Lys-cminus.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota4D -output=arg -name="Arg" $(ROTAOPTS4) rotatabs/Arg-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=arg123 -name="Arg-123" $(ROTAOPTS123) rotatabs/Arg-clash.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=argp -name="Arg-plus" $(ROTAOPTS234) rotatabs/Arg-cplus.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=argt -name="Arg-trans" $(ROTAOPTS234) rotatabs/Arg-ctrans.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=argm -name="Arg-minus" $(ROTAOPTS234) rotatabs/Arg-cminus.tab
	rm lys?*.ndft arg?*.ndft lys?*.data arg?*.data
#}}}

ROTAOPTSBB = -fields=6,7,4,14 -blt=40 -interval=5 -mask=10,13 -contour=:99.9,99,95,75
#{{{ backbone dependent: Asp/Asn
bbdep : all
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=aspBBdep -name="Asp-BBdep" $(ROTAOPTSBB) tabs/top500Asp.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=asnBBdep -name="Asn-BBdep" $(ROTAOPTSBB) tabs/top500Asn.tab
#}}}

rama : all
#{{{ Rama with one-param density-dependent smoothing
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=general -name="General-case Ramachandran plot" -center0 -fields=8,9,15 -blt=30 -interval=2 -mask=10,13 -contour=:99.95,98 tabs/top500-rama-noprepro.old
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=gen-pp -name="General+PrePro" -center0 -fields=8,9,15 -blt=30 -interval=2 -mask=10,13 -contour=:99.95,99.9,99.8,99.7,99.5,99,98,95,90,80,70,60,40,20 tabs/top500-rama-noprepro.old tabs/top500-rama-prepro-noGP.old
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=genAllB -name="General w/ no B filtering" -center0 -fields=8,9,15 -blt=1000 -interval=2 -mask=10,13 -contour=:99.95,99.9,99.8,99.5,99,98,95,90,80,70,60 tabs/top500-rama-noprepro.old
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=genNoSS -name="General, non(pro,prepro,repet)" -center0 -fields=5,6,4 -blt=100000 -interval=2 -mask=10,13 -contour=:99.9,95 tabs/noGPSansSecPreProB30.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=glycine -name="Glycine Ramachandran plot" -center0 -fields=4,5,9 -sym -blt=30 -interval=2 -mask=10,16 -contour=:99.8,98 tabs/top500-rama-Gly-blt30.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=proline -name="Proline Ramachandran plot" -center0 -fields=4,5,9 -blt=30 -interval=2 -mask=10,16 -contour=:99.8,98 tabs/top500-rama-Pro-blt30.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=prepro  -name="Pre-proline Ramachandran plot" -center0 -fields=8,9,15 -blt=30 -interval=2 -mask=10,16 -contour=:99.8,98 tabs/top500-rama-prepro-noGP.old
#
# For Jan Hermans (already B<30, B field set to res #):
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=AlaNoSS      -name="Ala, non(pro,prepro,repet)" -center0 -fields=5,6,4 -blt=100000 -mask=10,15 -contour=:99.9,99.8,99.5,99,98,97,96,95,90,80,70,60 tabs/AlaSansSecPreProB30.tab
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=GlySymNoSS   -name="Gly (sym), non(pro,prepro,repet)" -center0 -fields=5,6,4 -sym -blt=100000 -mask=10,16 -contour=:99.9,99.8,99.5,99,98,97,96,95,90,80,70,60 tabs/GlySansSecPreProB30.tab
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=GlyNoSymNoSS -name="Gly (not sym), non(pro,prepro,repet)" -center0 -fields=5,6,4 -blt=100000 -mask=10,16 -contour=:99.9,99.8,99.5,99,98,97,96,95,90,80,70,60 tabs/GlySansSecPreProB30.tab
#}}}
#
#{{{ Old (first submission) data:
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).SmoothHist2D -name="General phi,psi plot w/ density-dependent smoothing" -center0 -fields=8,9,15 -blt=30 -mask=10,99.95,35,98,15 -contour=:99.95,98 tabs/top500-rama-noprepro.old
#	-kin2Dcont -samp -wrap -179 181 -179 181 -g2 -nosmooth -group -l 0.0382282 sea -l 1.1061987 cyan output.kincont >> output.kin
#
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).SmoothHist2D -name="Glycine phi,psi plot w/ density-dependent smoothing" -center0 -fields=4,5,9 -sym -blt=30 -mask=10,99.9,46,85,14 -contour=:99.8,98 tabs/top500-rama-Gly-blt30.tab
#	-kin2Dcont -samp -wrap -179 181 -179 181 -g2 -nosmooth -group -l 0.021924572 sea -l 0.087489896 cyan output.kincont >> output.kin
#
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).SmoothHist2D -name="Proline phi,psi plot w/ density-dependent smoothing" -center0 -fields=4,5,9 -blt=30 -mask=10,99.9,46,85,14 -contour=:99.8,98 tabs/top500-rama-Pro-blt30.tab
#	-kin2Dcont -samp -wrap -179 181 -179 181 -g2 -nosmooth -group -l 0.04732904 sea -l 0.15257935 cyan output.kincont >> output.kin
#
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).SmoothHist2D -name="Pre-proline phi,psi plot w/ density-dependent smoothing" -center0 -fields=8,9,15 -blt=30 -mask=10,99.9,46,85,14 -contour=:99.8,98 tabs/top500-rama-prepro-noGP.old
#	-kin2Dcont -samp -wrap -179 181 -179 181 -g2 -nosmooth -group -l 0.023311377 sea -l 0.12535621 cyan output.kincont >> output.kin
#
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).SmoothHist2D -name="General, non(pro,prepro,repet)" -center0 -fields=5,6,4 -blt=100000 -mask=10,99.95,46,97,17 -contour=:99.9,95 tabs/noGPSansSecPreProB30.tab
#
# For Jan Hermans (already B<30, B field set to res #):
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).SmoothHist2D -name="Ala, non(pro,prepro,repet)" -center0 -fields=5,6,4 -blt=100000 -mask=10,99.9,46,70,14 -contour=:99.8,99.5,98,95,90 tabs/AlaSansSecPreProB30.tab
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).SmoothHist2D -name="Gly (sym), non(pro,prepro,repet)" -center0 -fields=5,6,4 -sym -blt=100000 -mask=10,99.9,46,70,14 -contour=:99.8,99.5,98,95,90 tabs/GlySansSecPreProB30.tab
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).SmoothHist2D -name="Gly (not sym), non(pro,prepro,repet)" -center0 -fields=5,6,4 -blt=100000 -mask=10,99.9,46,70,14 -contour=:99.8,99.5,98,95,90 tabs/GlySansSecPreProB30.tab
#}}}

#{{{ BackBone Angles, like tau and omega
BBANGOPTS = -blt=40 -mask=10,16 -contour=:99.9,99.5,99,98,95,90
bbang : all
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota2D -output=scifi -name="Psi-Phi" -fields=4,5,9 -interval=2 -center0 $(BBANGOPTS) /home/ian/rlab/psiphi/dang-peptide.tab
	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota3D -output=scifi3D -name="Psi-Phi-Omega" -fields=4,5,6,9 -interval=8 $(BBANGOPTS) /home/ian/rlab/psiphi/dang-peptide.tab
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota1D -output=tau -name="Tau" -fields=7,8 $(BBANGOPTS) /home/ian/rlab/chiropraxis/tau-omega.tab
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota1D -output=omega -name="Omega" -fields=6,8 $(BBANGOPTS) /home/ian/rlab/chiropraxis/tau-omega.tab
#	$(JAVA) -cp $(JTARGET) $(PACKAGE).Rota1D -output=cis -name="Omega (cis only)" -fields=6,8 $(BBANGOPTS) /home/ian/rlab/chiropraxis/omega-cis.tab
#}}} BackBone Angles, like tau and omega

