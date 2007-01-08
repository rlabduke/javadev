// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;
import king.*;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.JMenuItem;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>DiveTool</code> has not yet been documented.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jan  5 15:50:46 EST 2007
*/
public class DiveTool extends Plugin implements KMessage.Subscriber
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    ObjectLink<Command,Command> link = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DiveTool(ToolBox tb)
    {
        super(tb);
        try
        {
            link = new ObjectLink<Command,Command>("localhost", 1681);
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        kMain.subscribe(this);
    }
//}}}

//{{{ deliverMessage
//##############################################################################
    public void deliverMessage(KMessage msg)
    {
        if(link == null) return;
        try
        {
            if(msg.testKin(AHE.CHANGE_VIEW_TRANSFORM) || msg.testProg(KMessage.VIEW_SELECTED))
            {
                KView view = kMain.getView();
                if(view != null)
                {
                    Command cmd = new CmdSetView(view);
                    link.put(cmd);
                }
            }
            if(msg.testProg(KMessage.KIN_SWITCHED))
            {
                Kinemage kin = kMain.getKinemage();
                if(kin != null)
                {
                    Command cmd = new CmdLoadKinemage(kin);
                    link.put(cmd);
                }
            }
            if(msg.testKin(AHE.CHANGE_TREE_ON_OFF))
            {
                Kinemage kin = kMain.getKinemage();
                if(kin != null)
                {
                    Command cmd = new CmdSetOnOffState(kin);
                    link.put(cmd);
                }
            }
            // Flush Commands from other nodes -- we're not interested!
            Command cmd = link.get();
            while(cmd != null) cmd = link.get();
        }
        catch(Exception ex)
        {
            SoftLog.err.println("Error sending message: "+ex.getMessage());
            ex.printStackTrace();
            link = null;
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
    public JMenuItem getToolsMenuItem()
    { return null; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

