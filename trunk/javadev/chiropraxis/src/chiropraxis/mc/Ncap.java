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
    /** Residues in the Ncap and capping box positions for this helix. */
    public Residue res, res3;
    
    /** Angle between plane of Ncap Ca(i,i-1,i+1) and local helix axis. */
    public double planeNormalAngle;
    
    /** Angle between Ncap Ca_Cb vector and local helix axis. */
    public double caCbAngle;
    
    /** Angle between Ca(i-1,i+1,i+2) and Ca(i-1,i,i+1) planes */
    public double caPlanesAngle;
    
    /** Angle btw Ca(i-1,i+1) & Ca(i+1,i+2) virtual bonds; diff for ST vs. ND? */
    public double caEntryAngle;
    
    /** Ncap i-1,i,i+1 residues' phi, psi dihedrals. i+/-1 angles may be better 
    * indicators of a backrub b/c they actually cause the change during KiNG's 
    * backrub tool.*/
    public double nprimePhi, nprimePsi, phi, psi, n1Phi, n1Psi;
    
    /** Ncap i-1,i,i+1 residues' tau angles. May be strained if a backrub
    * occurs. */
    public double nprimeTau, tau, n1Tau;
    
    /** Distances that may vary between Ncap types (e.g. i+2 vs. i+3) or 
    * indicate where a residue is an Ncap vs. just continuation of a helix. 
    * For the first two, the atoms involved depend on the sc type. */
    double distNcapScToN2H, distNcapScToN3H, distNcapCaToN3Ca, distNprimeCaToN3Ca;
    
    /** Whether or not Ncap Hbonds to i+2/3's NH */
    boolean hb_i2, hb_i3;
    
    /** Residue type of capping box, iff its sc makes an Hbond to i's NH */
    String cappingBoxResType;
    
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
        res3               = null;
        planeNormalAngle   = Double.NaN;
        caCbAngle          = Double.NaN;
        caPlanesAngle      = Double.NaN;
        caEntryAngle       = Double.NaN;
        distNcapScToN2H    = Double.NaN;
        distNcapScToN3H    = Double.NaN;
        distNcapCaToN3Ca   = Double.NaN;
        distNprimeCaToN3Ca = Double.NaN;
        nprimeTau          = Double.NaN;
        tau                = Double.NaN;
        n1Tau              = Double.NaN;
        nprimePhi          = Double.NaN;
        nprimePsi          = Double.NaN;
        phi                = Double.NaN;
        psi                = Double.NaN;
        n1Phi              = Double.NaN;
        n1Psi              = Double.NaN;
        hb_i2              = false;
        hb_i3              = false;
        cappingBoxResType  = "";
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

