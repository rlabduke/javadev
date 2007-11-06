// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>Helix</code> is a class evolved from Ian's Peptide.
* HelixBuilder makes an array of these then does alignments and whatnot to them..
* At the moment, all fields are public and written to directly.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class Helix //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Residues in this helix. Inherently contains chain and resnum info. */
    public ArrayList<Residue> residues;
    
    /** PDB code for the structure this helix is in */
    public String pdb;
    
    /** Residue which is the Ncap for this helix. */
    public Residue ncap;
    
    /** Angle between plane of Ncap Ca(i,i-1,i+1) and local helix axis. */
    public double ncapPlaneNormalAngle;
    
    /** Angle between Ncap Ca_Cb vector and local helix axis. */
    public double ncapCaCbAngle;
    
    /** Ncap residue's phi, psi dihedrals. */
    public double ncapPhi, ncapPsi;
    
    /** Tail position (i.e. beg. of vector) of the normal to the Ncap plane */
    public Triple ncapNormalTail;
    
    /** Head position (i.e. end of vector) of the normal to the Ncap plane */
    public Triple ncapNormalHead;
    
    /** Tail position (i.e. beginning of vector) of the 4-Ca helix axis 
    * starting at each residue. */
    public ArrayList<Triple> axisTails;
    
    /** Head position (i.e. end of vector) of the 4-Ca helix axis starting 
    * at each residue. */
    public ArrayList<Triple> axisHeads;
    
    /** Tail position (i.e. beginning of vector) of the vector sum version of
    * the 4-Ca helix axis starting at each residue. */
    public Triple vectorSumAxisTail;
    
    /** Head position (i.e. end of vector) of the vector sum version of 
    * the 4-Ca helix axis starting at each residue. */
    public Triple vectorSumAxisHead;
    
    // Don't need separate ArrayList<Triple> smoothAxisTails/Heads b/c
    // we'll just modify axisTails/Heads if we do that option
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Helix(TreeSet<Residue> residuesSet)
    {
        super();
        Iterator iter = residuesSet.iterator();
        residues = new ArrayList<Residue>();
        while (iter.hasNext())
            residues.add( (Residue)iter.next() );
        Collections.sort(residues);
        ncap = null;
        ncapPlaneNormalAngle = Double.NaN;
        ncapCaCbAngle = Double.NaN;
        ncapPhi = Double.NaN;
        ncapPsi = Double.NaN;
        ncapNormalTail = null;
        ncapNormalHead = null;
        axisTails = null;
        axisHeads = null;
        vectorSumAxisTail = null;
        vectorSumAxisHead = null;
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        return "helix from "+getRes("first")+" to "+getRes("last");
    }
//}}}

//{{{ getRes
//##############################################################################
    public Residue getRes(String firstOrLast)
    {
        Residue resToReturn = residues.get(0); 
        if (firstOrLast.equals("first"))
        {
            for (Residue res : residues)
            {
                if (res.getSequenceInteger() < resToReturn.getSequenceInteger())
                    resToReturn = res;
            }
        }
        else if (firstOrLast.equals("last"))
        {
            for (Residue res : residues)
            {
                if (res.getSequenceInteger() > resToReturn.getSequenceInteger())
                    resToReturn = res;
            }
        }
        return resToReturn;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

