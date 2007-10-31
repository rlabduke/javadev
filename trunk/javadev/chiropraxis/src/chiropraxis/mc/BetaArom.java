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
* <code>BetaArom</code> is a class evolved from Daniel Keedy's Helix, which
* was derived from Ian's Peptide.
* It defines a local region around an aromatic residue in a p rotamer in a beta 
* sheet.
* This region consists of the following:
*   Ca(i, i+1, i-1) and Cb for the aromatic
*   Ca(i, i+1, i-1) and Cb for the residue on the opposite strand over which 
*      the aromatic is "hovering" and which may be causing the aromatic to 
*      backrub forward or backward if it's a Gly or something else, respectively.
* SheetBuilder makes an array of these if given the right flag (-betaarom) 
* then does some calculations using the coordinates stored herein.
* At the moment, all fields are public and written to directly.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class BetaArom //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** PDB code for the structure this sheet is in */
    public String pdb;
    
    /** The aromatic residue which may be backrubbed */
    public Residue aromRes;
    
    /** The residue on the opposite strand which may be causing the aromatic 
    * to backrub */
    public Residue oppRes;
    
    /** AtomStates for the aromatic's Ca(i-1), Ca(i), Ca(i+1), and Cb(i)
    * atoms, in that order */
    public ArrayList<AtomState> aromCoords;
    
    /** AtomStates for the opposite residue's Ca(i-1), Ca(i), Ca(i+1), and Cb(i)
    * atoms, in that order */
    public ArrayList<AtomState> oppCoords;
    
    /** Angle from Cb(i,arom)-Ca(i,arom)-Ca(i,opp) */
    public double cbcacaAngle;
    
    /** Some other angle not yet defined... */
    //?????
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BetaArom()
    {
        super();
        pdb         = null;
        aromRes     = null;
        oppRes      = null;
        aromCoords  = null;
        oppCoords   = null;
        cbcacaAngle = Double.NaN;
        
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        return "aromatic "+aromRes+" hanging over "+oppRes;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

