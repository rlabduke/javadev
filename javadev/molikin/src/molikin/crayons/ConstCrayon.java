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
* <code>ConstCrayon</code> is an Atom/BondCrayon that always returns the same
* string, regardless of input.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 11:41:50 EDT 2005
*/
public class ConstCrayon extends AbstractCrayon implements AtomCrayon, BondCrayon, RibbonCrayon
{
//{{{ Constants
    /** A crayon that always returns the empty string ("") */
    public static final ConstCrayon NONE = new ConstCrayon(null);
//}}}

//{{{ Variable definitions
//##############################################################################
    String kinString = null;
    String color = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ConstCrayon(String color)
    {
        super();
        this.color = color;
    }
//}}}

//{{{ getKinString, forAtom/Bond/Ribbon
//##############################################################################
    public String getKinString()
    {
        if(kinString == null) kinString = super.getKinString();
        return kinString;
    }
    
    public void forAtom(AtomState as) {}
    public void forBond(AtomState from, AtomState toward) {}
    public void forRibbon(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals) {}
//}}}

//{{{ get/setColor
//##############################################################################
    public String getColor()
    { return color; }
    public void setColor(String s)
    { this.color = s; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

