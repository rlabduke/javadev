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
import driftwood.r3.*;
//}}}
/**
* <code>ResColorMapCrayon</code> colors residues based on a lookup map.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct 19 13:11:10 EDT 2006
*/
public class ResColorMapCrayon extends AbstractCrayon implements AtomCrayon, BondCrayon, RibbonCrayon
{
//{{{ Constants
    static final String[] RAINBOW = { "red", "orange", "gold", "yellow", "lime", "green", "sea", "cyan", "sky", "blue" };
//}}}

//{{{ Variable definitions
//##############################################################################
    Map     map;
    String  color = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    protected ResColorMapCrayon(Map map)
    {
        super();
        this.map = map;
    }
//}}}

//{{{ forAtom/Bond/Ribbon, setColor
//##############################################################################
    public void forAtom(AtomState as)
    { setColor(as.getResidue()); }
    
    public void forBond(AtomState from, AtomState toward)
    { setColor(from.getResidue()); }
    
    public void forRibbon(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals)
    { setColor(start.nextRes); } // good for proteins, off a bit for RNA/DNA
    
    private void setColor(Residue r)
    {
        this.color = (String) map.get(r);
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

//{{{ newRainbow
//##############################################################################
    /**
    * Creates a crayon that will rainbow color from N to C or from 5' to 3'.
    * Treats all the residues as one continuous chain for the purpose of
    * calculating coloring.
    * @param contigs    a Collection&lt;Collection&lt;Residue&gt;&gt;
    */
    public static ResColorMapCrayon newRainbow(Collection contigs)
    {
        int totalRes = 0;
        for(Iterator iter = contigs.iterator(); iter.hasNext(); )
            totalRes += ((Collection) iter.next()).size();
        
        int currRes = 0; // always < totalRes inside the inner loop
        Map map = new HashMap();
        for(Iterator i = contigs.iterator(); i.hasNext(); )
        {
            Collection contig = (Collection) i.next();
            for(Iterator j = contig.iterator(); j.hasNext(); )
            {
                Residue res = (Residue) j.next();
                int colorIdx = (RAINBOW.length * currRes) / totalRes;
                map.put(res, RAINBOW[colorIdx]);
                currRes++;
            }
        }
        
        return new ResColorMapCrayon(map);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

