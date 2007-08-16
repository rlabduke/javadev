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
* <code>HalfBondElementCrayon</code> colors bonds by the atom elements,
* to produce half-bond coloring.
* Carbons are NOT assigned a color, to allow recoloring them by list color.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct 19 13:12:08 EDT 2006
*/
public class HalfBondElementCrayon extends AbstractCrayon implements BondCrayon
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String color = null;
    boolean colorCarbons = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public HalfBondElementCrayon()
    {
        super();
    }
//}}}

//{{{ forBond
//##############################################################################
    public void forBond(AtomState from, AtomState toward)
    {
        String elem = from.getElement();
        if(colorCarbons || !elem.equals("C"))
            this.color = Util.getElementColor(elem);
        else
            this.color = null;
    }
//}}}

//{{{ getColor, getKinString
//##############################################################################
    public String getColor()
    { return color; }
    
    public String getKinString()
    { return (color == null ? "" : color); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

