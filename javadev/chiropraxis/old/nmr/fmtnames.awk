#!/usr/bin/awk
$1 != currRes {
    currRes = $1;
    print "};"
    print "lookup" currRes " = {";
}
{
    diana = substr($0, 81, 4)
    if(substr($0, 33, 1) ~ /[1-9]/)
        pdb = substr($0, 33, 4);
    else
        pdb = substr($0, 32, 4);
    print "    \""diana"\", \""pdb"\",";
}
