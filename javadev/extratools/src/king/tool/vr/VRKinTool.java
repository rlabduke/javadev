// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.vr;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>VRKinTool</code> allows some actions in KiNG to be transmitted to
* the "vrkin" program that runs in the Duke DIVE.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jan  5 15:50:46 EST 2007
*/
public class VRKinTool extends Plugin implements KMessage.Subscriber
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Socket              socket;
    DataOutputStream    outStream;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public VRKinTool(ToolBox tb)
    {
        super(tb);
        kMain.subscribe(this);
    }
//}}}

//{{{ deliverMessage
//##############################################################################
    public void deliverMessage(KMessage msg)
    {
        if(socket == null) return;
        try
        {
            if(msg.testKin(AHE.CHANGE_VIEW_TRANSFORM) || msg.testProg(KMessage.VIEW_SELECTED))
            {
                KView view = kMain.getView();
                if(view != null)
                {
                }
            }
            if(msg.testProg(KMessage.KIN_SWITCHED))
            {
                Kinemage kin = kMain.getKinemage();
                if(kin != null)
                {
                }
            }
            if(msg.testKin(AHE.CHANGE_TREE_ON_OFF))
            {
                Kinemage kin = kMain.getKinemage();
                if(kin != null)
                    sendString(cmdSetOnOffState(kin));
            }
        }
        catch(Exception ex)
        {
            SoftLog.err.println("Error sending message: "+ex.getMessage());
            ex.printStackTrace();
            disconnect();
        }
    }
//}}}

//{{{ getToolsMenuItem
//##############################################################################
    public JMenuItem getToolsMenuItem()
    {
        JMenu menu = new JMenu(this.toString());
        JMenuItem item = new JMenuItem(new ReflectiveAction("Connect ...", null, this, "onConnectToMaster"));
        menu.add(item);
        return menu;
    }
    
    public String toString() {
      return "VRKin plugin";
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
                disconnect();
                connect(
                    hostname.getText(),
                    Integer.parseInt(port.getText())
                );
            }
            catch(Exception ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ connect, disconnect, sendString
//##############################################################################
    void connect(String host, int port) throws IOException
    {
        InetAddress addr    = InetAddress.getByName(host); //IOEx
        this.socket         = new Socket(addr, port); //IOEx
        socket.setKeepAlive(true); // may not be needed, really
        
        this.outStream      = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }
    
    void disconnect()
    {
        if(outStream != null)   try { outStream.close(); } catch(IOException ex) {}
        if(socket != null)      try { socket.close(); } catch(IOException ex) {}
        socket      = null;
        outStream   = null;
    }
    
    void sendString(String msg)
    {
        //System.err.println(msg);
        if(this.outStream == null) return;
        try
        {
            outStream.writeInt(msg.length()); // high byte first
            outStream.writeBytes(msg); // low bytes only
            outStream.flush();
        }
        catch(IOException ex)
        { ex.printStackTrace(); }
    }
//}}}

//{{{ cmdSetOnOffState
//##############################################################################
    /**
    * Produces a string that records the on/off state of every element
    * (except individual points) in the entire kinemage, like this:
    *   "show 111100010110..."
    * Traversal is depth first, with parents visited before children,
    * and the first character is always "1" (for the kinemage itself).
    */
    String cmdSetOnOffState(Kinemage kin)
    {
        StringBuffer sb = new StringBuffer("show ");
        for(AGE age : KIterator.allNonPoints(kin))
            sb.append(age.isOn() ? "1" : "0");
        return sb.toString();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

