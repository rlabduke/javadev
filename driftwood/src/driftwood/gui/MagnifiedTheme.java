// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
//}}}
/**
* <code>MagnifiedTheme</code> is a Swing Metal theme that
* magnifies all font sizes by some given scale factor.
* This can be a useful accomodation for the visually challenged.
*
* <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Oct 14 14:10:15 EDT 2002
*/
public class MagnifiedTheme extends DefaultMetalTheme // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    float mag;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public MagnifiedTheme(float magnification)
    {
        mag = magnification;
    }
//}}}

//{{{ get___Font()
//##################################################################################################
    public String getName()
    { return "Magnified"; }
    
    public FontUIResource getControlTextFont()
    {
        FontUIResource orig = super.getControlTextFont();
        FontUIResource big = new FontUIResource(orig.deriveFont(mag*orig.getSize()));
        return big;
    }

    public FontUIResource getMenuTextFont()
    {
        FontUIResource orig = super.getMenuTextFont();
        FontUIResource big = new FontUIResource(orig.deriveFont(mag*orig.getSize()));
        return big;
    }

    public FontUIResource getSubTextFont()
    {
        FontUIResource orig = super.getSubTextFont();
        FontUIResource big = new FontUIResource(orig.deriveFont(mag*orig.getSize()));
        return big;
    }

    public FontUIResource getSystemTextFont()
    {
        FontUIResource orig = super.getSystemTextFont();
        FontUIResource big = new FontUIResource(orig.deriveFont(mag*orig.getSize()));
        return big;
    }

    public FontUIResource getUserTextFont()
    {
        FontUIResource orig = super.getUserTextFont();
        FontUIResource big = new FontUIResource(orig.deriveFont(mag*orig.getSize()));
        return big;
    }

    public FontUIResource getWindowTitleFont()
    {
        FontUIResource orig = super.getWindowTitleFont();
        FontUIResource big = new FontUIResource(orig.deriveFont(mag*orig.getSize()));
        return big;
    }
//}}}
}//class

