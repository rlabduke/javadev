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
* <code>Peptide</code> is a data structure for building up beta-sheet descriptions.
* At the moment, all fields are public and written to directly.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class Peptide //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Residues before and after this peptide. One may be null. */
    public Residue cRes, nRes;
    /** The midpoint of the two C-alphas at the ends of this peptide. Never null. */
    public Triple midpoint;
    /** The peptides before and after this one in a connected chain. May be null. */
    public Peptide prev = null, next = null;
    /** The peptides to which this one is H-bonded thru its N and O atoms. May be null. */
    public Peptide hbondN = null, hbondO = null;
    /** Whether or not this peptide can be considered to be in a beta sheet. */
    public boolean isBeta = false;
    /** Whether this strand is parallel to its two H-bonded neighbors. */
    public boolean isParallelN = false, isParallelO = false;
    /** The index of this chain, and the index of this peptide within it. -1 by default. */
    int chain = -1, index = -1;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * @throws AtomException if cRes or nRes is not null and missing a C-alpha
    */
    public Peptide(Residue cRes, Residue nRes, ModelState state)
    {
        super();
        this.nRes = nRes;
        this.cRes = cRes;
        if(cRes == null)
            midpoint = new Triple(state.get(nRes.getAtom(" N  ")));
        else if(nRes == null)
            midpoint = new Triple(state.get(cRes.getAtom(" C  ")));
        else
        {
            AtomState nca = state.get(nRes.getAtom(" CA "));
            AtomState cca = state.get(cRes.getAtom(" CA "));
            midpoint = new Triple().likeMidpoint(nca, cca);
        }
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        return "peptide from "+cRes+" to "+nRes;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

