// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
 * <code>ToolBoxMW</code> extends ToolBox with mouse-wheel functionality.
 * This class needs Java 1.4 or later, it is NOT compatible with Java 1.3.
 * It should never be referenced directly from anywhere in the KiNG program;
 * only references generated through the Reflection API are safe.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Mon Dec  9 10:35:21 EST 2002
*/
public class ToolBoxMW extends ToolBox implements MouseWheelListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ToolBoxMW(KingMain kmain, KinCanvas kcanv)
    {
        super(kmain, kcanv);
    }
//}}}

//{{{ listenTo
//##################################################################################################
    /** Does all the work to make the ToolBox listen to the specified component. */
    public void listenTo(Component c)
    {
        super.listenTo(c);
        c.addMouseWheelListener(this);
    }
//}}}

//{{{ mouseWheelMoved
//##################################################################################################
    public void mouseWheelMoved(MouseWheelEvent ev)
    {
        this.mouseWheelMoved(ev, ev.getWheelRotation());
    }
//}}}
}//class

