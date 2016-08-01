// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.crayons;
import molikin.Crayon;
import molikin.AtomCrayon;
import molikin.BondCrayon;
import molikin.RibbonCrayon;

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
* <code>AbstractCrayon</code> provides default implementations of all Crayon methods,
* so children need only implement shouldPrint() and the appropriate attributes.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 10 07:46:31 EDT 2006
*/
public abstract class AbstractCrayon implements Crayon
{
//{{{ Constants
    static final DecimalFormat df = driftwood.util.Strings.usDecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AbstractCrayon()
    {
        super();
    }
//}}}

//{{{ shouldPrint, getColor/Pointmasters/Aspects/Width/Radius/Unpickable
//##############################################################################
    /** Returns true */
    public boolean shouldPrint()
    { return true; }

    /** Returns null for none */
    public String getColor()
    { return null; }
    
    /** Returns null for none */
    public String getPointmasters()
    { return null; }
    
    /** Returns null for none */
    public String getAspects()
    { return null; }
    
    /** Returns 0 for default */
    public int getWidth()
    { return 0; }

    /** Returns 0 for default */
    public double getRadius()
    { return 0; }
    
    /** Returns false */
    public boolean getUnpickable()
    { return false; }
//}}}

//{{{ getKinString
//##############################################################################
    /** Composites together non-default attributes into a kinemage-format string. */
    public String getKinString()
    {
        StringBuffer buf = null;
        
        boolean b = getUnpickable();
        if(b)
        {
            if(buf == null) buf = new StringBuffer();
            else buf.append(" ");
            buf.append("U");
        }
        
        String s = getColor();
        if(s != null)
        {
            if(buf == null) buf = new StringBuffer();
            else buf.append(" ");
            buf.append(s);
        }
        s = getPointmasters();
        if(s != null)
        {
            if(buf == null) buf = new StringBuffer();
            else buf.append(" ");
            buf.append("'").append(s).append("'");
        }
        s = getAspects();
        if(s != null)
        {
            if(buf == null) buf = new StringBuffer();
            else buf.append(" ");
            buf.append("(").append(s).append(")");
        }

        int i = getWidth();
        if(i > 0)
        {
            if(buf == null) buf = new StringBuffer();
            else buf.append(" ");
            buf.append("width").append(i);
        }

        double d = getRadius();
        if(d > 0)
        {
            if(buf == null) buf = new StringBuffer();
            else buf.append(" ");
            buf.append("r=").append(df.format(d));
        }
        
        if(buf == null) return "";
        else return buf.toString();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

