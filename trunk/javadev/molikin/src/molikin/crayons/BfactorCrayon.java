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
* <code>BfactorCrayon</code> colors things by B-factor.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct 19 13:12:08 EDT 2006
*/
public class BfactorCrayon extends AbstractCrayon implements AtomCrayon, BondCrayon, RibbonCrayon
{
//{{{ Constants
    // Always need COLORS to be one longer than MAXVALS
    private static final String[] B_COLORS    = {"blue", "purple", "magenta", "hotpink", "red", "orange", "gold", "yellow", "yellowtint", "white"};
    private static final double[] B_MAXVALS   = {       5,        10,        15,        20,    30,       40,      50,      60,           80      };
//}}}

//{{{ Variable definitions
//##############################################################################
    int colorIndex;
    double[] maxvals;
    String[] colors;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BfactorCrayon()
    {
        this(B_MAXVALS, B_COLORS);
    }

    protected BfactorCrayon(double[] maxvals, String[] colors)
    {
        super();
        if(maxvals.length+1 > colors.length)
            throw new IllegalArgumentException("Must have at least one more color than value-stop");
        this.maxvals    = (double[]) maxvals.clone();
        this.colors     = (String[]) colors.clone();
    }
//}}}

//{{{ forAtom/Bond/Ribbon
//##############################################################################
    public void forAtom(AtomState as)
    { setValue(as.getTempFactor()); }
    
    public void forBond(AtomState from, AtomState toward)
    {
        // Could do average of from + toward, could do max --
        // but this way it works with half-bond coloring.
        setValue(from.getTempFactor());
    }
    
    public void forRibbon(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals)
    { setValue(0); } // XXX-TODO: fix me!
//}}}

//{{{ setValue, getColor, getKinString
//##############################################################################
    protected void setValue(double val)
    {
        for(colorIndex = 0; colorIndex < maxvals.length; colorIndex++)
        {
            if(maxvals[colorIndex] > val) break;
        }
    }

    public String getColor()
    { return colors[colorIndex]; }
    
    public String getKinString()
    { return getColor(); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

