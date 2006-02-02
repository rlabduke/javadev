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
public class PdbSecondaryStructure extends SecondaryStructure
{
//{{{ Constants
//}}}

//{{{ CLASS: Range
//##############################################################################
    /** Describes a start-end range for a helix, sheet, or turn */
    static class Range
    {
        Object  type = COIL;
        String  chainId;
        int     initSeqNum, endSeqNum;
        String  initICode, endICode;
        
        public boolean contains(Residue r)
        {
            if(!chainId.equals(r.getChain())) return false;
            int seqNum = r.getSequenceInteger();
            if(seqNum < initSeqNum || seqNum > endSeqNum) return false;
            String iCode = r.getInsertionCode();
            if(seqNum == initSeqNum && iCode.compareTo(initICode) < 0) return false;
            if(seqNum == endSeqNum  && iCode.compareTo(endICode)  > 0) return false;
            return true;
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    Collection ranges = new ArrayList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PdbSecondaryStructure(Collection headers)
    {
        super();
        for(Iterator iter = headers.iterator(); iter.hasNext(); )
        {
            String s = (String) iter.next();
                 if(s.startsWith("HELIX ")) ranges.add(forHelix(s));
            else if(s.startsWith("SHEET ")) ranges.add(forSheet(s));
            else if(s.startsWith("TURN  ")) ranges.add(forTurn(s));
        }
    }
//}}}

//{{{ forHelix, forSheet, forTurn
//##############################################################################
    /** @param s a PDB "HELIX" record */
    Range forHelix(String s)
    {
        Range r = new Range();
        r.type = HELIX;
        r.chainId = s.substring(19,20);
        if(!r.chainId.equals(s.substring(31,32)))
            System.err.println("Mismatched chain IDs: "+s);
        try
        {
            r.initSeqNum = Integer.parseInt(s.substring(21,25).trim());
            r.endSeqNum  = Integer.parseInt(s.substring(33,37).trim());
        }
        catch(NumberFormatException ex)
        { System.err.println("Non-numeric sequence numbers: "+s); }
        r.initICode = s.substring(25,26);
        r.endICode  = s.substring(37,38);
        return r;
    }

    /** @param s a PDB "SHEET" record */
    Range forSheet(String s)
    {
        Range r = new Range();
        r.type = STRAND;
        r.chainId = s.substring(21,22);
        if(!r.chainId.equals(s.substring(32,33)))
            System.err.println("Mismatched chain IDs: "+s);
        try
        {
            r.initSeqNum = Integer.parseInt(s.substring(22,26).trim());
            r.endSeqNum  = Integer.parseInt(s.substring(33,37).trim());
        }
        catch(NumberFormatException ex)
        { System.err.println("Non-numeric sequence numbers: "+s); }
        r.initICode = s.substring(26,27);
        r.endICode  = s.substring(37,38);
        return r;
    }

    /** @param s a PDB "TURN" record */
    Range forTurn(String s)
    {
        Range r = new Range();
        r.type = TURN;
        r.chainId = s.substring(19,20);
        if(!r.chainId.equals(s.substring(30,31)))
            System.err.println("Mismatched chain IDs: "+s);
        try
        {
            r.initSeqNum = Integer.parseInt(s.substring(20,24).trim());
            r.endSeqNum  = Integer.parseInt(s.substring(31,35).trim());
        }
        catch(NumberFormatException ex)
        { System.err.println("Non-numeric sequence numbers: "+s); }
        r.initICode = s.substring(24,25);
        r.endICode  = s.substring(35,36);
        return r;
    }
//}}}

//{{{ classify
//##############################################################################
    public Object classify(Residue res)
    {
        for(Iterator iter = ranges.iterator(); iter.hasNext(); )
        {
            Range rng = (Range) iter.next();
            if(rng.contains(res)) return rng.type;
        }
        return COIL; // no entry for that residue
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

