// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;

import driftwood.gui.*;
import king.*;
import king.core.*;
//}}}
/**
* <code>SimpleTool</code> hijacks the machinery of KiNG's dynamically loaded Tools
* to get easy-to-use mouse listener functions.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 21 12:11:18 EDT 2006
*/
public class SimpleTool extends king.BasicTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SimpleTool(SimpleApp app, ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ click, drag, wheel
//##############################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        // optional:
        //super.click(x, y, p, ev);
        
        if(p != null)
        {
            if(p.getColor() == KPalette.hotpink) p.setColor(null);
            else p.setColor(KPalette.hotpink);
        }
        kMain.getKinemage().signal.signalKinemage(kMain.getKinemage(), KinemageSignal.APPEARANCE);
    }
    
    /** Override this function for right-button/shift clicks */
    //public void s_click(int x, int y, KPoint p, MouseEvent ev) {}
    /** Override this function for middle-button/control clicks */
    //public void c_click(int x, int y, KPoint p, MouseEvent ev) {}
    /** Override this function for shift-control clicks */
    //public void sc_click(int x, int y, KPoint p, MouseEvent ev) {}

    /** Override this function for (left-button) drags */
    //public void drag(int dx, int dy, MouseEvent ev) {}
    /** Override this function for right-button/shift drags */
    //public void s_drag(int dx, int dy, MouseEvent ev) {}
    /** Override this function for middle-button/control drags */
    //public void c_drag(int dx, int dy, MouseEvent ev) {}
    /** Override this function for shift-control drags */
    //public void sc_drag(int dx, int dy, MouseEvent ev) {}

    /** Override this function for mouse wheel motion */
    //public void wheel(int rotation, MouseEvent ev) {}
    /** Override this function for mouse wheel motion with shift down */
    //public void s_wheel(int rotation, MouseEvent ev) {}
    /** Override this function for mouse wheel motion with control down */
    //public void c_wheel(int rotation, MouseEvent ev) {}
    /** Override this function for mouse wheel motion with shift AND control down */
    //public void sc_wheel(int rotation, MouseEvent ev) {}
//}}}

//{{{ toString
//##############################################################################
    public String toString() { return "This *would* show up in KiNG's Tools menu"; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

