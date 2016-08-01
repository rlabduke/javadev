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
* <code>CompositeCrayon</code> aggregates the results of many different Crayons.
* While you could theoretically mix Atom, Bond, and Ribbon crayons within
* one composite, don't -- it will lead to unexpected results (and exceptions!)
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct 19 13:11:00 EDT 2006
*/
public class CompositeCrayon extends AbstractCrayon implements AtomCrayon, BondCrayon, RibbonCrayon
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Collection      crayons     = new ArrayList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CompositeCrayon()
    {
        super();
    }
//}}}

//{{{ add
//##############################################################################
    /**
    * Adds a crayon at the end of the list.
    * @return this (for chaining)
    */
    public CompositeCrayon add(Crayon c)
    {
        this.crayons.add(c);
        return this;
    }
//}}}

//{{{ shouldPrint, getColor/Pointmasters/Aspects
//##############################################################################
    /** Returns false unless all crayons report true. */
    public boolean shouldPrint()
    {
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
            if(!((Crayon) iter.next()).shouldPrint()) return false;
        return true;
    }

    /** Returns string from first non-null crayon, or null for none */
    public String getColor()
    {
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
        {
            Crayon c = (Crayon) iter.next();
            String color = c.getColor();
            if(color != null) return color;
        }
        return null;
    }
    
    /** Returns string from first non-null crayon, or null for none */
    public String getPointmasters()
    {
        StringBuffer buf = null;
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
        {
            Crayon c = (Crayon) iter.next();
            String pm = c.getPointmasters();
            if(pm != null)
            {
                if(buf == null) buf = new StringBuffer();
                buf.append(pm);
            }
        }
        if(buf == null) return null;
        else return buf.toString();
    }
    
    /** Returns string from first non-null crayon, or null for none */
    public String getAspects()
    {
        StringBuffer buf = null;
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
        {
            Crayon c = (Crayon) iter.next();
            String aspects = c.getAspects();
            if(aspects != null)
            {
                if(buf == null) buf = new StringBuffer();
                buf.append(aspects);
            }
        }
        if(buf == null) return null;
        else return buf.toString();
    }
//}}}
    
//{{{ getWidth/Radius/Unpickable
//##############################################################################
    /** Returns value from first non-zero crayon, or 0 for default */
    public int getWidth()
    {
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
        {
            Crayon c = (Crayon) iter.next();
            int width = c.getWidth();
            if(width != 0) return width;
        }
        return 0;
    }

    /** Returns value from first non-zero crayon, or 0 for default */
    public double getRadius()
    {
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
        {
            Crayon c = (Crayon) iter.next();
            double radius = c.getRadius();
            if(radius != 0) return radius;
        }
        return 0;
    }
    
    /** Returns false unless one or more crayons report true. */
    public boolean getUnpickable()
    {
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
            if(((Crayon) iter.next()).getUnpickable()) return true;
        return false;
    }
//}}}

//{{{ forAtom/Bond/Ribbon
//##############################################################################
    public void forAtom(AtomState as)
    {
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
        {
            // If this cast fails, you added the wrong type of crayon.
            // Better to fail fast than produce weird results later.
            AtomCrayon c = (AtomCrayon) iter.next();
            c.forAtom(as);
        }
    }
    
    public void forBond(AtomState from, AtomState toward)
    {
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
        {
            // If this cast fails, you added the wrong type of crayon.
            // Better to fail fast than produce weird results later.
            BondCrayon c = (BondCrayon) iter.next();
            c.forBond(from, toward);
        }
    }
    
    public void forRibbon(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals)
    {
        for(Iterator iter = crayons.iterator(); iter.hasNext(); )
        {
            // If this cast fails, you added the wrong type of crayon.
            // Better to fail fast than produce weird results later.
            RibbonCrayon c = (RibbonCrayon) iter.next();
            c.forRibbon(point, start, end, interval, nIntervals);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

