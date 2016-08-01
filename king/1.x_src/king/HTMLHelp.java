// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
 * <code>HTMLHelp</code> is a simple HTML browser for displaying help information.
 *
 * <p>Begun on Wed Jun 26 22:15:35 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class HTMLHelp implements HyperlinkListener
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    
    JFrame frame;
    JEditorPane editpane;
    URL homepage;
    URL prevpage = null;
    LinkedList history;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a new help-system window.
    */
    public HTMLHelp(KingMain kmain, URL start)
    {
        kMain = kmain;
        homepage = start;
        history = new LinkedList();
        
        frame = new JFrame("KiNG Help");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setIconImage(kMain.prefs.windowIcon);
        
        editpane = new JEditorPane();
        editpane.addHyperlinkListener(this);
        editpane.setEditable(false);
        JScrollPane scroll = new JScrollPane(editpane);
        scroll.setPreferredSize(new Dimension(600,400));
        
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(new JButton(new ReflectiveAction("Back", kMain.prefs.htmlBackIcon, this, "onBack")));
        toolbar.addSeparator();
        toolbar.add(new JButton(new ReflectiveAction("Home", kMain.prefs.htmlHomeIcon, this, "onHome")));
        
        frame.getContentPane().add(toolbar, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
    }
//}}}

//{{{ show/hide
//##################################################################################################
    public void show()
    {
        try
        {
            editpane.setPage(homepage);
            history = new LinkedList(); // clear the history
            prevpage = homepage;
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        
        frame.pack();
        frame.setVisible(true);
    }
    
    public void hide() { frame.dispose(); }
//}}}

//{{{ hyperlinkUpdate, onBack, onHome
//##################################################################################################
    // This was ganked from the JEditorPane documentation.
    public void hyperlinkUpdate(HyperlinkEvent ev)
    {
        if(ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            if(ev instanceof HTMLFrameHyperlinkEvent)
            {
                HTMLDocument doc = (HTMLDocument)editpane.getDocument();
                doc.processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent)ev);
            }
            else
            {
                try
                {
                    if(prevpage != null) history.addLast(prevpage);
                    while(history.size() > 100) history.removeFirst();
                    URL url = ev.getURL();
                    editpane.setPage(url);
                    prevpage = url;
                }
                catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
            }
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onBack(ActionEvent ev)
    {
        if(history.size() < 1) return;
        try
        {
            URL url = (URL)history.removeLast();
            editpane.setPage(url);
            prevpage = url;
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHome(ActionEvent ev)
    {
        try
        {
            history = new LinkedList(); // Clear the history
            editpane.setPage(homepage);
            prevpage = homepage;
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
    }
//}}}
}//class
