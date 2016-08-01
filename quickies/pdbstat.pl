#!/usr/bin/env perl
#
# pdbstat.pl - a script to document the contents of a PDB file
#
# Usage: pdbstat.pl pdbfilename
#
# Output: a series of key=value statements describing the model
#   Keys include:
#   compnd          a description of the compound, from the PDB file
#   models          number of models in the file
#   chains          number of chains in the file
#   unique_chains   number of unique (not duplicated) chains
#   residues        total number of residues (in 1st model)
#   sidechains      are sidechains present? (0/>0)
#   hydrogens       are hydrogens present? (0/>0)
#   hets            number of non-water heterogens
#
# IWD 8/26/02
# builds on IWD's pdbstat and MGP's pdbstat.awk
#
use strict;
use warnings;

# Variables for tracking data
my $models = 0;         # number of ENDMDL records encountered
my %chains;             # each chain associated w/ a string of residue types
my $residues = 0;       # number of distinct residues (changes of res. ID)
my $rescode = "";       # current res. ID
my $cbetas = 0;         # number of C-betas (for sidechains)
my $hydrogens = 0;      # number of hydrogens
my $hets = 0;           # number of non-water hets
my $hetcode = "";       # current het ID

# Variables for processing data
my $compnd;             # Description of the compound
my $chain;              # Chain ID (one letter)
my $resno;              # Residue number (four chars)
my $icode;              # Insertion code (one char)
my $restype;            # Residue type (three chars)
my $atom;               # Atom (four) + alt (one)
my $id;                 # 9-character residue ID

while(<>)
{
    chomp;
    
    # These will be meaningless for some lines, but that's OK
    $chain   = substr($_,21,1);
    $resno   = substr($_,22,4);
    $icode   = substr($_,26,1);
    $restype = substr($_,17,3);
    $atom    = substr($_,12,5);
    $id = $chain.$resno.$icode.$restype;

    # Switch on record type
    if(/^ENDMDL/) { $models++; }
    elsif(/^COMPND/) { $compnd  .= " " . substr($_,10,60); }
    elsif($models == 0)
    {
        if(/^ATOM  /)
        {
            # Start of a new residue?
            if($id ne $rescode)
            {
                $residues++;
                $rescode = $id;
                $chains{$chain} .= $restype;
            }
            
            # Atom name == CB?
            if($atom =~ / CB [ A1]/) { $cbetas++; }
            # Atom is a hydrogen?
            elsif($atom =~ /[ 1-9][HDTZ][ A-Z][ 1-9][ A1]/) { $hydrogens++; }
        }
        elsif(/^HETATM/)
        {
            # Start of a new residue?
            if($id ne $hetcode and $restype !~ /HOH|DOD|H20|D20|WAT/)
            {
                $hets++;
                $hetcode = $id;
            }
        }
    }
}

# Output
print "compnd=${compnd}\n";
print "models=${models}\n";
print "chains=".keys(%chains)."\n";

# Determine number of entries in %chains that are unique
my @ch = values(%chains);
my $unique_chains = 0;
my ($i, $j);

CHAIN: for($i = 0; $i <= $#ch; $i++)
{
    for($j = $i+1; $j <= $#ch; $j++)
    {
        if($ch[$i] eq $ch[$j]) { next CHAIN; }
    }
    $unique_chains++;
}
print "unique_chains=${unique_chains}\n";

# More output
print "residues=${residues}\n";
print "sidechains=${cbetas}\n";
print "hydrogens=${hydrogens}\n";
print "hets=${hets}\n";

