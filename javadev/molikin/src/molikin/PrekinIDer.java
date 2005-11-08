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
    static final DecimalFormat df2 = new DecimalFormat("0.00");
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
        
        buf.append(Strings.justifyLeft(as.getName(), 4));
        buf.append(Strings.justifyLeft(as.getAltConf(), 1));
        buf.append(Strings.justifyLeft(res.getName(), 3));
        buf.append(" ");
        buf.append(Strings.justifyLeft(res.getChain(), 1));
        buf.append(" ");
        buf.append(res.getSequenceNumber().trim());
        buf.append(Strings.justifyLeft(res.getInsertionCode(), 1));
        
        StringBuffer buf2 = new StringBuffer(buf.length() + 8);
        buf2.append(buf.toString().toLowerCase());
        buf2.append(" B");
        buf2.append(df2.format(as.getTempFactor()));
        
        return buf2.toString();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

