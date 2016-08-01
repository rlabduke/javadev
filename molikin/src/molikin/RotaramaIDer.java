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
* <code>RotaramaIDer</code> creates point IDs for atoms that match the
* output of DCR's Prekin but add rota or Rama scores if applicable.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Fri Feb  5 2010
*/
public class RotaramaIDer implements AtomIDer
{
//{{{ Constants
    private static final String[] mcAtomNames = {" N  ", " H  ", " CA ", " C  ", " O  ", "_N__", "_H__", "_CA_", "_C__", "_O__"};
    static final DecimalFormat df2 = driftwood.util.Strings.usDecimalFormat("0.00");
    static final DecimalFormat df3 = driftwood.util.Strings.usDecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##############################################################################
    HashMap<Residue,Double> rota, rama;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotaramaIDer(Map rota, Map rama)
    {
        super();
        this.rota = (HashMap<Residue,Double>) rota;
        this.rama = (HashMap<Residue,Double>) rama;
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
            return buf2.toString()+recallRotarama(as, res);
        }
        else return buf.toString().toLowerCase()+recallRotarama(as, res);
    }
//}}}

//{{{ recallRotarama
//##############################################################################
    private String recallRotarama(AtomState as, Residue res)
    {
        Atom a = as.getAtom();
        if(AminoAcid.isBackbone(a))
        {
            for(Iterator iter = rama.keySet().iterator(); iter.hasNext(); )
            {
                Residue r = (Residue) iter.next();
                if(r.getCNIT().equals(res.getCNIT()))
                {
                    return " rama"+df3.format(100*rama.get(res))+"%";
                }
            }
        }
        else // assume sidechain
        {
            for(Iterator iter = rota.keySet().iterator(); iter.hasNext(); )
            {
                Residue r = (Residue) iter.next();
                if(r.getCNIT().equals(res.getCNIT()))
                {
                    return " rota"+df3.format(100*rota.get(res))+"%";
                }
            }
        }
        return "";
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

