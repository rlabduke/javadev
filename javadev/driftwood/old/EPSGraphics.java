// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>EPSGraphics</code> is a small subclass of
* org.jibble.epsgraphics.EpsGraphics2D that implements
* a few more of the missing functions.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct  9 11:29:32 EDT 2003
*/
public class EPSGraphics extends org.jibble.epsgraphics.EpsGraphics2D
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The color that will be set if a non-Color Paint is requested. */
    Color       paintDelegate       = Color.red;
    /** A Graphics that can supply FontMetrics for us, or null to use Toolkit. */
    Graphics    metricsDelegate     = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public EPSGraphics()
    {
        super();
    }
//}}}

//{{{ get/setPaint
//##############################################################################
    /** <b>Kludge</b> - returns the currently set Color. Identical to getColor(). */
    public Paint getPaint()
    { return getColor(); }
    
    /**
    * <b>Kludge</b> - sets the Paint iff it's a solid Color, otherwise we
    * set the designated delegate paint color (red, by default).
    */
    public void setPaint(Paint p)
    {
        if(p instanceof Color)
            setColor((Color)p);
        else
            setColor(paintDelegate);
    }
//}}}

//{{{ getFontMetrics
//##############################################################################
    public FontMetrics getFontMetrics()
    { return getFontMetrics(getFont()); }
    
    /**
    * <b>Kludge</b> - returns font metrics from a delegated Graphics object,
    * or from the AWT Toolkit if no delegate has been named.
    */
    public FontMetrics getFontMetrics(Font f)
    {
        if(metricsDelegate != null)
            return metricsDelegate.getFontMetrics(f);
        else
            return Toolkit.getDefaultToolkit().getFontMetrics(f);
    }
//}}}

//{{{ setPaintDelegate, setFontMetricsDelegate
//##############################################################################
    /**
    * Sets a color to use instead of a non-Color Paint.
    * The default value is Color.red
    */
    public void setPaintDelegate(Color c)
    { if(c != null) paintDelegate = c; }
    
    /**
    * Sets a Graphics that can be consulted for FontMetrics.
    * By default, the AWT Toolkit is queried instead.
    * Pass <code>null</code> to restore the default behavior.
    */
    public void setFontMetricsDelegate(Graphics g)
    { metricsDelegate = g; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

