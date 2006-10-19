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
* <code>OccupancyCrayon</code> colors things by occupancy.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct 19 13:12:13 EDT 2006
*/
public class OccupancyCrayon extends BfactorCrayon
{
//{{{ Constants
    // Always need COLORS to be one longer than MAXVALS
    private static final String[] Q_COLORS    = {"white", "lilactint", "lilac", "purple", "gray", "green"};
    private static final double[] Q_MAXVALS   = {       0.02,        0.33,    0.66,     0.99,   1.01     };
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public OccupancyCrayon()
    {
        super(Q_MAXVALS, Q_COLORS);
    }
//}}}

//{{{ forAtom/Bond/Ribbon
//##############################################################################
    public void forAtom(AtomState as)
    { setValue(as.getOccupancy()); }
    
    public void forBond(AtomState from, AtomState toward)
    {
        // Could do average of from + toward, could do max --
        // but this way it works with half-bond coloring.
        setValue(from.getOccupancy());
    }
    
    public void forRibbon(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals)
    { setValue(0); } // XXX-TODO: fix me!
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

