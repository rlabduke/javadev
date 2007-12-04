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
* <code>Ncap</code> is a class evolved from Helix which came from Ian's Peptide.
* A Helix object can have an Ncap, which holds information about the residue
* type and angles related to possible backrubs.
* At the moment, all fields are public and written to directly.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* <br>Begun on Sun Nov 11, 2007
*/
public class Ncap //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Residue which is the Ncap for this helix. */
    public Residue res;
    
    /** Angle between plane of Ncap Ca(i,i-1,i+1) and local helix axis. */
    public double planeNormalAngle;
    
    /** Angle between Ncap Ca_Cb vector and local helix axis. */
    public double caCbAngle;
    
    /** Ncap i-1,i,i+1 residues' phi, psi dihedrals. i+/-1 angles may be better 
    * indicators of a backrub b/c they actually cause the change during KiNG's 
    * backrub tool.*/
    public double nprimePhi, nprimePsi, phi, psi, n1Phi, n1Psi;
    
    /** Distances that may vary between Ncap types (e.g. i+2 vs. i+3) or 
    * indicate where a residue is an Ncap vs. just continuation of a helix. 
    * For the first two, the atoms involved depend on the sc type. */
    double distNcapScToN2H, distNcapScToN3H, distNcapCaToN3Ca, distNprimeCaToN3Ca;
    
    /** Whether Ncap Hbonds to i+2 and i+3 or not */
    boolean hb_i2, hb_i3;
    
    /** Number of chi angles (a measure of sidechain length) for two potentially
    * important residues for this Ncap. */
    int ncapNumChis, n3NumChis;
    
    /** Tail position (i.e. beg. of vector) of the normal to the Ncap plane */
    public Triple normalTail;
    
    /** Head position (i.e. end of vector) of the normal to the Ncap plane */
    public Triple normalHead;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Ncap(Residue residue)
    {
        super();
        res = residue;
        planeNormalAngle   = Double.NaN;
        caCbAngle          = Double.NaN;
        distNcapScToN2H    = Double.NaN;
        distNcapScToN3H    = Double.NaN;
        distNcapCaToN3Ca   = Double.NaN;
        distNprimeCaToN3Ca = Double.NaN;
        nprimePhi          = Double.NaN;
        nprimePsi          = Double.NaN;
        phi                = Double.NaN;
        psi                = Double.NaN;
        n1Phi              = Double.NaN;
        n1Psi              = Double.NaN;
        hb_i2              = false;
        hb_i3              = false;
        ncapNumChis        = 999;
        n3NumChis          = 999;
        normalTail         = null;
        normalHead         = null;
        
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        return "Ncap "+res;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

