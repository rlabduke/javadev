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
* <code>AltConfNetworkCrayon</code> colors residues based on which alt conf
* network (defined elsewhere, e.g. in cmdline.AltConfEnsembler) they're in.
*
* <p>Copyright (C) 2012 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Oct 8 2012
*/
public class AltConfNetworkCrayon extends AbstractCrayon implements AtomCrayon, BondCrayon, RibbonCrayon
{
//{{{ Constants
//##############################################################################
    private final String[] DEFAULT_COLORS = {
        "red","green",
        "blue","peachtint",
        "purple","yellow",
        "magenta", "lime",
        "peach", "cyan",
        "sky", "peach",
        "cyan","orange",
        "greentint", "pink", "brown", "lilactint", "pintkint", "sea", "hotpink"
    };
//}}}

//{{{ Variable definitions
//##############################################################################
    String[] colors;
    int colorIndex;
    // each residue maps to a network which has a quasi-arbitrary integer index
    HashMap<Residue,Integer> networks;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AltConfNetworkCrayon(Map n)
    {
        super();
        this.colors   = DEFAULT_COLORS;
        this.networks = (HashMap<Residue,Integer>) n;
    }

    public AltConfNetworkCrayon(String[] c, Map n)
    {
        super();
        this.colors   = (String[]) c.clone();
        this.networks = (HashMap<Residue,Integer>) n;
    }
//}}}

//{{{ forAtom/Bond/Ribbon
//##############################################################################
    public void forAtom(AtomState as)
    { setValue(as); }
    
    public void forBond(AtomState from, AtomState toward)
    { setValue(from); }
    
    public void forRibbon(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals)
    { setValue(null); } // XXX-TODO: fix me!
//}}}

//{{{ setValue, getColor, getKinString
//##############################################################################
    protected void setValue(AtomState as)
    {
        int networkIndex = -1;  // default: not in an alt conf network
        
        if(networks == null) throw new UnsupportedOperationException();
        Atom a = as.getAtom();
        if(a != null)
        {
            Residue r = a.getResidue();
            if(r != null)
            {
                if(networks.get(r) != null)
                {
                    networkIndex = networks.get(r);
                }
            }
            else throw new UnsupportedOperationException();
        }
        else throw new UnsupportedOperationException();
        
        colorIndex = networkIndex % colors.length;
    }
    
    public String getColor()
    {
        // Returning null will signal to a CompositeCrayon to move onto the next Crayon 
        // to determine what color to use.  I expect that to happen when the last AtomState
        // evaluated here doesn't belong to an alt conf network.
        return (colorIndex < 0 ? null : colors[colorIndex]);
    }
    
    public String getKinString()
    { return getColor(); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

