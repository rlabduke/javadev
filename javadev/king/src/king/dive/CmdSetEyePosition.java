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
* <code>CmdSetEyePosition</code> requests that the Slaves redraw their
* graphics to reflect a new observer position.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec 15 11:39:22 EST 2006
*/
public class CmdSetEyePosition implements Command
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Triple  leftEye;
    Triple  rightEye;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CmdSetEyePosition(Triple leftEye, Triple rightEye)
    {
        super();
        this.leftEye = leftEye;
        this.rightEye = rightEye;
    }
//}}}

//{{{ doCommand
//##############################################################################
    public void doCommand(Slave slave)
    {
        slave.leftEyePos = leftEye;
        slave.rightEyePos = rightEye;
        slave.canvas.repaint();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

