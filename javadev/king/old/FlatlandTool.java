// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
//import gnu.regexp.*;
//}}}
/**
 * <code>FlatlandTool</code> is a simple tool for flatland scrolling, ala Mage
 *
 * <p>Begun on Wed Jul 17 10:26:39 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class FlatlandTool extends BasicTool // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    // Variables
    // go
    // here
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public FlatlandTool(ToolBox tb)
    {
        super(tb);
        setToolName("Flatland");
    }
//}}}

//{{{ click() functions    
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        identify(p);
    }
    
    /** Override this function for right-button/shift clicks */
    public void s_click(int x, int y, KPoint p, MouseEvent ev)
    {
        KingView v = parent.kMain.getView();
        
        click(x, y, p, ev);
        pickcenter(p, v);
    }
    
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {}
    /** Override this function for shift-control clicks */
    public void sc_click(int x, int y, KPoint p, MouseEvent ev)
    {}
//}}}

//{{{ drag() functions    
//##################################################################################################
    /** Override this function for (left-button) drags */
    public void drag(int dx, int dy, MouseEvent ev)
    {
        translate(dx, dy, parent.kMain.getView());
    }
    
    /** Override this function for right-button/shift drags */
    public void s_drag(int dx, int dy, MouseEvent ev)
    {
        KingView v = parent.kMain.getView();
        zoom(dx+dy,v);
    }
    
    /** Override this function for middle-button/control drags */
    public void c_drag(int dx, int dy, MouseEvent ev)
    {}

    /** Override this function for shift-control drags */
    public void sc_drag(int dx, int dy, MouseEvent ev)
    {}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

