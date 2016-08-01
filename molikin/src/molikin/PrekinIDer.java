// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.util.*;
//}}}
/**
* <code>PrekinIDer</code> creates point IDs for atoms that match the
* output of DCR's Prekin.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Nov  8 15:18:46 EST 2005
*/
public class PrekinIDer implements AtomIDer
{
//{{{ Constants
    static final DecimalFormat df2 = driftwood.util.Strings.usDecimalFormat("0.00");
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PrekinIDer()
    {
        super();
    }
//}}}

//{{{ identifyAtom
//##############################################################################
    public String identifyAtom(AtomState as)
    {
        Residue res = as.getResidue();
        StringBuffer buf = new StringBuffer(16); // should be long enough for almost all
        
        buf.append(as.getName());                       // 4 chars
        buf.append(as.getAltConf());                    // 1
        buf.append(res.getName());                      // 3
        buf.append(" ");                                // 1
        buf.append(res.getChain());                     // 1
        buf.append(" ");                                // 1
        buf.append(res.getSequenceNumber().trim());     // 1 - 4 (or more)
        buf.append(res.getInsertionCode());             // 1
        
        if((as.getTempFactor() > 0.0)||(as.getOccupancy() < 1.0))
        {
            StringBuffer buf2 = new StringBuffer(buf.length() + 8);
            // lowercasing is somewhat expensive, computationally.
            buf2.append(buf.toString().toLowerCase());
            if (as.getOccupancy() < 1.0) buf2.append(" "+df2.format(as.getOccupancy())+"B");
            else buf2.append(" B");                              // 2
            buf2.append(df2.format(as.getTempFactor()));    // 4 - 5 (or more)
            return buf2.toString();
        }
        else return buf.toString().toLowerCase();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

