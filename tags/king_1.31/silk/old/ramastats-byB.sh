#!/bin/bash
RAMASTAT="java -cp boundrotamers.jar boundrotamers.RamaStat"

### disfavored ###
TABLE="/home/ian/rlab/rotarama/rama-publish/final-draft/general.ndft"
TAB=tabs/top500-rama-noprepro.old
LEV=0.008962469
$RAMASTAT -fields=8,9,15,1 -B=0,12    -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=12,18   -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=18,24   -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=24,30   -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=30,36   -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=36,1000 -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB

### favored ###
LEV=0.2427307
$RAMASTAT -fields=8,9,15,1 -B=0,12    -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=12,18   -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=18,24   -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=24,30   -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=30,36   -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB
$RAMASTAT -fields=8,9,15,1 -B=36,1000 -resolution=0.0,2.0 -level=$LEV -table=$TABLE < $TAB

