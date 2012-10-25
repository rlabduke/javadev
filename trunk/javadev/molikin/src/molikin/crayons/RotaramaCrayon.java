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
* <code>RotaramaCrayon</code> colors things by rotamer/Ramachandran score.
*
* <p>Copyright (C) 20?? by Daniel A. Keedy. All rights reserved.
* <br>Begun on ??? ?? 20??
*/
public class RotaramaCrayon extends AbstractCrayon implements AtomCrayon, BondCrayon, RibbonCrayon
{
//{{{ Constants
    // Always need COLORS to be one longer than MAXVALS
    private static final String[] R_COLORS    = {"white", "yellowtint", "gold", "orange", "red", "hotpink", "magenta", "purple", "blue"};
    private static final double[] R_MAXVALS   = {      0.01,         0.02,   0.03,     0.04,  0.05,      0.06,      0.1,      0.25     };
    //private static final String[] R_COLORS    = {"white", "yellowtint", "yellow", "gold", "orange", "red", "hotpink", "magenta", "purple", "blue"};
    //private static final double[] R_MAXVALS   = {      0.01,         0.02,     0.05,   0.1,      0.2,   0.3,       0.4,       0.55,     0.7      };
    //private static final String[] R_COLORS    = {"hotpink", "red", "orange", "yellow", "green", "blue"};
    //private static final double[] R_MAXVALS   = {        0.1,   0.2,      0.3,      0.4,     0.5      };
    //private static final String[] R_COLORS    = {"white", "yellowtint", "yellow", "gold", "orange", "red", "hotpink", "magenta", "purple", "blue"};
    //private static final double[] R_MAXVALS   = {        0.1,          0.2,      0.3,    0.4,      0.55,  0.7,       0.8,       0.9,      0.99   };
//}}}

//{{{ Variable definitions
//##############################################################################
    int colorIndex;
    double[] maxvals;
    String[] colors;
    HashMap<Residue,Double> rota, rama;
//}}}

//{{{ Constructor(s)
//##############################################################################
    // Useless!  Has to be here to extend AbstractCrayon
    public RotaramaCrayon()
    {
        this(R_MAXVALS, R_COLORS, null, null);
    }

    // Useless!
    protected RotaramaCrayon(double[] maxvals, String[] colors)
    {
        this(maxvals, colors, null, null);
    }

    // Should actually get used
    public RotaramaCrayon(Map rota, Map rama)
    {
        this(R_MAXVALS, R_COLORS, rota, rama);
    }

    protected RotaramaCrayon(double[] maxvals, String[] colors, Map rota, Map rama)
    {
        super();
        if(maxvals.length+1 > colors.length)
            throw new IllegalArgumentException("Must have at least one more color than value-stop");
        this.maxvals    = (double[]) maxvals.clone();
        this.colors     = (String[]) colors.clone();
        this.rota = (HashMap<Residue,Double>) rota;
        this.rama = (HashMap<Residue,Double>) rama;
    }
//}}}

//{{{ forAtom/Bond/Ribbon
//##############################################################################
    public void forAtom(AtomState as)
    {
        setValue(scoreForAtom(as));
    }
    
    public void forBond(AtomState from, AtomState toward)
    {
        // Could do average of from + toward, could do max --
        // but this way it works with half-bond coloring.
        setValue(scoreForAtom(from));
    }
    
    public void forRibbon(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals)
    { setValue(0); } // XXX-TODO: fix me!
//}}}

//{{{ scoreForAtom
//##############################################################################
    private double scoreForAtom(AtomState as)
    {
        if(rama == null || rota == null) throw new UnsupportedOperationException();
        Atom a = as.getAtom();
        if(a != null)
        {
            Residue r = a.getResidue();
            if(r != null)
            {
                if(AminoAcid.isBackbone(a))
                {
                    if(rama.get(r) != null) return rama.get(r);
                    else return Double.POSITIVE_INFINITY; // will use max color
                }
                else // assume sidechain
                {
                    if(rota.get(r) != null) return rota.get(r);
                    else return Double.POSITIVE_INFINITY; // will use max color
                }
            }
            else throw new UnsupportedOperationException();
        }
        else throw new UnsupportedOperationException();
    }
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

