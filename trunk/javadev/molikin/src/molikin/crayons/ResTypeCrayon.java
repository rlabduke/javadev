// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.crayons;
import molikin.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>ResTypeCrayon</code> colors Cys-S---S-Cys bonds yellow.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct 19 13:11:10 EDT 2006
*/
public class ResTypeCrayon extends AbstractCrayon implements AtomCrayon, BondCrayon
{
//{{{ Constants
    private static Map resColors;
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean excludeMainchain = true;
    String color = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ResTypeCrayon()
    {
        super();
    }
//}}}

//{{{ forAtom/Bond, setColor
//##############################################################################
    public void forAtom(AtomState as)
    { setColor(as); }
    
    public void forBond(AtomState from, AtomState toward)
    { setColor(from); }
    
    private void setColor(AtomState as)
    {
        if(excludeMainchain && Util.isMainchain(as))
        {
            color = null;
            return;
        }
        
        if(resColors == null) //{{{
        {
            resColors = new HashMap();
            // From PKINCRTL.h
            // A type bases
            resColors.put("  A", "pink");
            resColors.put("A  ", "pink");
            resColors.put("ADE", "pink");
            resColors.put("ATP", "pink");
            resColors.put("ADP", "pink");
            resColors.put("AMP", "pink");
            resColors.put("1MA", "pink");
            resColors.put("RIA", "pink");
            resColors.put("T6A", "pink");
            // T,U type bases
            resColors.put("  T", "sky");
            resColors.put("T  ", "sky");
            resColors.put("THY", "sky");
            resColors.put("TTP", "sky");
            resColors.put("TDP", "sky");
            resColors.put("TMP", "sky");
            resColors.put("  U", "sky");
            resColors.put("U  ", "sky");
            resColors.put("URA", "sky");
            resColors.put("URI", "sky");
            resColors.put("UTP", "sky");
            resColors.put("UDP", "sky");
            resColors.put("UMP", "sky");
            resColors.put("PSU", "sky");
            resColors.put("4SU", "sky");
            resColors.put("5MU", "sky");
            resColors.put("H2U", "sky");
            // G type bases
            resColors.put("  G", "sea");
            resColors.put("G  ", "sea");
            resColors.put("GUA", "sea");
            resColors.put("GTP", "sea");
            resColors.put("GDP", "sea");
            resColors.put("GMP", "sea");
            resColors.put("GSP", "sea");
            resColors.put("1MG", "sea");
            resColors.put("2MG", "sea");
            resColors.put("M2G", "sea");
            resColors.put("7MG", "sea");
            resColors.put("OMG", "sea");
            // C type bases
            resColors.put("  C", "yellow");
            resColors.put("C  ", "yellow");
            resColors.put("CYT", "yellow");
            resColors.put("CTP", "yellow");
            resColors.put("CDP", "yellow");
            resColors.put("CMP", "yellow");
            resColors.put("5MC", "yellow");
            resColors.put("OMC", "yellow");
            // other bases
            resColors.put("  I", "white");
            resColors.put("I  ", "white");
            resColors.put(" YG", "white");
            resColors.put("YG ", "white");
            // From PKINCSUB.c
            // Standard amino acids
            resColors.put("CYS", "yellow");
            resColors.put("PRO", "hotpink");
            resColors.put("GLY", "hotpink");
            resColors.put("TYR", "sea");
            resColors.put("PHE", "sea");
            resColors.put("TRP", "sea");
            resColors.put("LEU", "gold");
            resColors.put("ILE", "gold");
            resColors.put("VAL", "gold");
            resColors.put("MET", "gold");
            resColors.put("ALA", "gold");
            resColors.put("SER", "cyan");
            resColors.put("THR", "cyan");
            resColors.put("GLN", "cyan");
            resColors.put("ASN", "cyan");
            resColors.put("LYS", "sky");
            resColors.put("ARG", "sky");
            resColors.put("HIS", "sky");
            resColors.put("ASP", "pink");
            resColors.put("GLU", "pink");
        } //}}}
        this.color = (String) resColors.get(as.getResidue().getName());
        // it's OK for color to be null
    }
//}}}

//{{{ getColor, getKinString
//##############################################################################
    public String getColor()
    { return color; }
    
    public String getKinString()
    { return color == null ? "" : color; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

