#!/bin/bash
######################################
#Gary Kapral 04/20/2011
#update the het dictionary in reduce on the cluster; downloads the het dictionary from PDB, date stamps it 
#makes it readable by reduce, and links the reduce_wwPDB_het_dict.txt used by reduce to the new het dictionary
#use: sign in as srcer, run from /home/srcer/src/reduce/reduce_trunk/
#other programs used: adjust_het_dict.pl, comment_out_OH_on_P_v3.0.pl
######################################

cd ./chiropraxis
ant clean
ant build 
cd ../cifless
ant clean
ant build
cd ../cmdline
ant clean
ant build
cd ../dangle
ant clean
ant build
cd ../driftwood
ant clean
ant build
cd ../extratools
ant clean
ant build
cd ../fftoys
ant clean
ant build
cd ../geometer
ant clean
ant build
cd ../jiffiloop
ant clean
ant build
cd ../molikin
and clean
ant build
cd ../rdcvis
ant clean
ant build
cd ../silk
ant clean
ant build
cd ../
cp ./chiropraxis/chiropraxis.jar ./king/plugins/.
cp ./cifless/cifless.jar ./king/plugins/.
cp ./cmdline/cmdline.jar ./king/plugins/.
cp ./dangle/dangle.jar./king/plugins/.
cp ./driftwood/driftwood.jar ./king/plugins/.
cp ./extratools/extratools.jar ./king/plugins/.
cp ./fftoys/fftoys.jar ./king/plugins/.
cp ./geometer/geometer.jar ./king/plugins/.
cp ./jiffiloop/jiffiloop.jar ./king/plugins/.
cp ./molikin/molikin.jar ./king/plugins/.
cp ./rdcvis/rdcvis.jar ./king/plugins/.
cp ./silk/silk.jar ./king/plugins/.
cd ./king
ant clean
ant build

