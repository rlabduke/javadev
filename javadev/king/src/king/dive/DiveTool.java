// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
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
        
        // For my convenience right now.  Should go away later.
        try { link = new ObjectLink<Command,Command>("localhost", 1681); }
        catch(IOException ex) {}
        
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

//{{{ getToolsMenuItem
//##############################################################################
    public JMenuItem getToolsMenuItem()
    {
        JMenu menu = new JMenu("DiVE plugin");
        JMenuItem item = new JMenuItem(new ReflectiveAction("Connect ...", null, this, "onConnectToMaster"));
        menu.add(item);
        return menu;
    }
//}}}

//{{{ onConnectToMaster
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onConnectToMaster(ActionEvent ev)
    {
        JTextField hostname = new JTextField("localhost");
        JTextField port = new JTextField("1681");
        
        TablePane2 cp = new TablePane2();
        cp.hfill(true).insets(4).memorize();
        cp.weights(0,1).addCell(new JLabel("Hostname")).addCell(hostname).newRow();
        cp.weights(0,1).addCell(new JLabel("Port")).addCell(port).newRow();
        
        int result = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            cp, "Connect to master",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
            
        if(result == JOptionPane.OK_OPTION)
        {
            try
            {
                if(link != null) link.disconnect();
                link = new ObjectLink<Command,Command>(
                    hostname.getText(),
                    Integer.parseInt(port.getText())
                );
            }
            catch(Exception ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

