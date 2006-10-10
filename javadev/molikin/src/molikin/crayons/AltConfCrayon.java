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
* <code>AltConfCrayon</code> puts in pointmasters for atoms with alt confs.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Nov 10 10:50:17 EST 2005
*/
public class AltConfCrayon extends AbstractCrayon implements AtomCrayon, BondCrayon
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String altconf = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AltConfCrayon()
    {
        super();
    }
//}}}

//{{{ forAtom, forBond, getPointmasters
//##############################################################################
    public void forAtom(AtomState as)
    {
        String alt = as.getAltConf();
        if(alt.equals(" ")) altconf = null;
        else altconf = String.valueOf(Character.toLowerCase(alt.charAt(0)));
    }
    
    public void forBond(AtomState from, AtomState toward)
    {
        String altf = from.getAltConf();
        String altt = toward.getAltConf();
        if(altf.equals(" "))
        {
            if(altt.equals(" ")) altconf = null;
            else altconf = String.valueOf(Character.toLowerCase(altt.charAt(0)));
        }
        else // altf != " "
        {
            if(altt.equals(" ")) altconf = String.valueOf(Character.toLowerCase(altf.charAt(0)));
            // From and Toward should never belong to two different, non-blank alts.
            // But just in case:
            else altconf = ""+Character.toLowerCase(altf.charAt(0))+Character.toLowerCase(altt.charAt(0));
        }
    }
    
    public String getPointmasters()
    { return altconf; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

