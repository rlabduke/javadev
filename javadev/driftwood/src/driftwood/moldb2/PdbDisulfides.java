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
* <code>PdbDisulfides</code> returns disulfide bond assignments
* based on the PDB file SSBOND records.
* It was basically copied from IWD's PdbSecondaryStructure class.
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Sep 2 2009
*/
class PdbDisulfides extends Disulfides
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PdbDisulfides(Collection headers)
    {
        super();
        for(Iterator iter = headers.iterator(); iter.hasNext(); )
        {
            String s = (String) iter.next();
            try
            {
                if(s.startsWith("SSBOND ")) add(forDisulfide(s));
            }
            catch(NumberFormatException ex)
            { System.err.println("Non-numeric sequence numbers: "+s); }
            catch(IndexOutOfBoundsException ex)
            { System.err.println("PDB record too short: "+s); }
        }
    }
//}}}

//{{{ forDisulfide
//##############################################################################
    // This can throw NumberFormatException (for non-numeric data)
    // and IndexOutOfBoundsException, for lines that are too short.

    /** @param s a PDB "SSBOND" record */
    Disulfide forDisulfide(String s) throws NumberFormatException
    {
        // "SSBOND   1 CYS A   31    CYS A   73                                             "
        // "SSBOND *** CYS A  190    CYS C  190"
        Disulfide d = new Disulfide();
        d.initChainId = s.substring(15,16);
        d.endChainId  = s.substring(29,30);
        if(!d.initChainId.equals(d.endChainId))
            d.type = Disulfide.INTER_CHAIN;
        else //if(d.initChainId.equals(d.endChainId))
            d.type = Disulfide.INTRA_CHAIN;
        d.initSeqNum = Integer.parseInt(s.substring(17,21).trim());
        d.endSeqNum  = Integer.parseInt(s.substring(31,35).trim());
        d.initICode = s.substring(21,22);
        if(s.length() >= 36) // if space, may be truncated in non-std file
            d.endICode  = s.substring(35,36);
        else
            d.endICode = " "; // (default anyway)
        return d;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

