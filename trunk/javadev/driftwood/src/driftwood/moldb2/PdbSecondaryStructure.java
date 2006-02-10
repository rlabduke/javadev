// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>PdbSecondaryStructure</code> returns secondary structure assignments
* based on the PDB file HELIX, SHEET, and TURN records.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb  2 09:45:24 EST 2006
*/
class PdbSecondaryStructure extends SecondaryStructure
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PdbSecondaryStructure(Collection headers)
    {
        super();
        for(Iterator iter = headers.iterator(); iter.hasNext(); )
        {
            String s = (String) iter.next();
            try
            {
                     if(s.startsWith("HELIX ")) ranges.add(forHelix(s));
                else if(s.startsWith("SHEET ")) ranges.add(forSheet(s));
                else if(s.startsWith("TURN  ")) ranges.add(forTurn(s));
            }
            catch(NumberFormatException ex)
            { System.err.println("Non-numeric sequence numbers: "+s); }
            catch(IndexOutOfBoundsException ex)
            { System.err.println("PDB record too short: "+s); }
        }
    }
//}}}

//{{{ forHelix, forSheet, forTurn
//##############################################################################
    // All of these can throw NumberFormatException (for non-numeric data)
    // and IndexOutOfBoundsException, for lines that are too short.

    /** @param s a PDB "HELIX" record */
    Range forHelix(String s) throws NumberFormatException
    {
        Range r = new Range();
        r.type = HELIX;
        r.chainId = s.substring(19,20);
        if(!r.chainId.equals(s.substring(31,32)))
            System.err.println("Mismatched chain IDs: "+s);
        r.initSeqNum = Integer.parseInt(s.substring(21,25).trim());
        r.endSeqNum  = Integer.parseInt(s.substring(33,37).trim());
        r.initICode = s.substring(25,26);
        r.endICode  = s.substring(37,38);
        return r;
    }

    /** @param s a PDB "SHEET" record */
    Range forSheet(String s) throws NumberFormatException
    {
        Range r = new Range();
        r.type = STRAND;
        r.chainId = s.substring(21,22);
        if(!r.chainId.equals(s.substring(32,33)))
            System.err.println("Mismatched chain IDs: "+s);
        r.initSeqNum = Integer.parseInt(s.substring(22,26).trim());
        r.endSeqNum  = Integer.parseInt(s.substring(33,37).trim());
        r.initICode = s.substring(26,27);
        r.endICode  = s.substring(37,38);
        return r;
    }

    /** @param s a PDB "TURN" record */
    Range forTurn(String s) throws NumberFormatException
    {
        Range r = new Range();
        r.type = TURN;
        r.chainId = s.substring(19,20);
        if(!r.chainId.equals(s.substring(30,31)))
            System.err.println("Mismatched chain IDs: "+s);
        r.initSeqNum = Integer.parseInt(s.substring(20,24).trim());
        r.endSeqNum  = Integer.parseInt(s.substring(31,35).trim());
        r.initICode = s.substring(24,25);
        r.endICode  = s.substring(35,36);
        return r;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

