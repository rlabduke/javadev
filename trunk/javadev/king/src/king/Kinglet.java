// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
//import java.text.*;
//import java.util.*;
import javax.swing.*;
//import driftwood.util.SoftLog;
//}}}
/**
* <code>Kinglet</code> is the KiNG loader applet, to allow KiNG to be used in browsers.
*
* <p>Begun on Sun Jun  9 14:53:42 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class Kinglet extends JApplet implements MouseListener
{
    public static final String NORMAL   = "normal";
    public static final String FLAT     = "flat";
    public static final String LAUNCHER = "launcher";
    
    KingMain    kMain           = null;
    String      mode            = null;

    public void init()
    {
        String m = getParameter("mode");
        if(m == null) mode = NORMAL;
        else
        {
            m = m.toLowerCase();
            if(m.equals("flat")) mode = FLAT;
            else if(m.equals("launcher")) mode = LAUNCHER;
            else mode = NORMAL;
        }
        
        if(mode.equals(NORMAL))
        {
            Icon kingletIcon = new ImageIcon(getClass().getResource("images/kinglet-logo.jpg"));
            getContentPane().add(new JLabel(kingletIcon), BorderLayout.CENTER);
            validate();
        }
        else if(mode.equals(LAUNCHER))
        {
            Icon kingletIcon = new ImageIcon(getClass().getResource("images/king-btn.png"));
            try
            {
                String launcherButton = this.getParameter("launcherImage");
                if(launcherButton != null)
                    kingletIcon = new ImageIcon(new URL(this.getDocumentBase(), launcherButton));
            }
            catch(Exception ex) { System.err.println(ex.getMessage()); }
            getContentPane().add(new JLabel(kingletIcon), BorderLayout.CENTER);
            addMouseListener(this);
            validate();
        }
    }

    public void start()
    { if(!mode.equals(LAUNCHER)) launch(); }
    
    public void mouseClicked(MouseEvent ev)
    { launch(); }
    
    void launch()
    {
        kMain = new KingMain(this, (mode.equals(FLAT)));
        kMain.Main();
    }

    /** Initiates shutdown by calling dispose() on the windows. */
    public void stop()
    {
        if(kMain != null) kMain.shutdown();
        kMain = null; // so it can be GC'd
    }

    public void mouseEntered(MouseEvent ev)     {}
    public void mouseExited(MouseEvent ev)      {}
    public void mousePressed(MouseEvent ev)     {}
    public void mouseReleased(MouseEvent ev)    {}
}//class
