// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
//}}}
/**
* <code>CmdSetView</code> requests that the Slaves redraw their
* graphics to reflect a new kinemage view.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec 15 11:39:22 EST 2006
*/
public class CmdSetView implements Command
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KView   view;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CmdSetView(KView view)
    {
        super();
        this.view = view;
    }
//}}}

//{{{ doCommand
//##############################################################################
    public void doCommand(Slave slave)
    {
        slave.view = view;
        slave.canvas.repaint();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

