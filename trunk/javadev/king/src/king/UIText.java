// (jEdit options) :folding=explicit:collapseFolds=1:
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
//import java.text.*;
//import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
/**
* <code>UIText</code> is the kinemage text manager.
* It takes care of displaying and editing the text.
*
* <p>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Jun  9 19:06:25 EDT 2002
*/
public class UIText implements ChangeListener, MouseListener
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    JFrame frame;
    //JEditorPane editpane;
    JTextArea textarea;
    
    JButton popupButton;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public UIText(KingMain kmain)
    {
        kMain = kmain;
        popupButton = new JButton(new ReflectiveAction("Show text", null, this, "onPopupButton"));

        frame = new JFrame("Text window");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setIconImage(kMain.getPrefs().windowIcon);
        
        //editpane = new JEditorPane("text/html", "");
        //editpane.setEditable(false);
        //JScrollPane editScroll = new JScrollPane(editpane);
        //editScroll.setPreferredSize(new Dimension(500,400));
        
        textarea = new JTextArea();
        textarea.setEditable(true);
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
        textarea.addMouseListener(this);
        JScrollPane textScroll = new JScrollPane(textarea);
        textScroll.setPreferredSize(new Dimension(500,400));
        new TextCutCopyPasteMenu(textarea);
        
        //JTabbedPane tabPane = new JTabbedPane();
        //tabPane.addTab("Read", null, editScroll, "Read the text that accompanies this kinemage");
        //tabPane.addTab("Revise", null, textScroll, "Edit the text that accompanies this kinemage");
        //tabPane.setSelectedIndex(0);
        //tabPane.addChangeListener(this);
        
        //frame.getContentPane().add(tabPane, BorderLayout.CENTER);
        //frame.getContentPane().setBackground(new Color(0.5f, 0.5f, 0.5f));
        frame.getContentPane().add(textScroll, BorderLayout.CENTER);
    }
//}}}

//{{{ get/set/appendText()
//##################################################################################################
    public String getText()
    { return textarea.getText(); }
    
    public void setText(String txt)
    {
        textarea.setText(txt);
        textarea.setCaretPosition(0); // at the top
        //editpane.setText(text2html(txt));
    }

    public void appendText(String txt)
    {
        txt = getText().concat(txt);
        
        // Keep the text window from moving around too much
        int caret = textarea.getCaretPosition();
        caret = Math.min(caret, txt.length());
        textarea.setText(txt);
        textarea.setCaretPosition(caret);
    }
//}}}

//{{{ shutdown
//##################################################################################################
    /** Initiates shutdown by calling dispose() on the frame. */
    public void shutdown()
    {
        frame.dispose();
    }
//}}}

//{{{ cascadeBehind, onPopupButton, getButton
//##################################################################################################
    /**
    * Positions this window above, left, and behind the specified window.
    */
    public void cascadeBehind(Window w)
    {
        if(w == null) return;
        
        frame.pack();
        Point p = w.getLocation();
        frame.setLocation(p);
        frame.setVisible(true);
        p.x += 24;
        p.y += 24;
        w.setLocation(p);
        w.toFront();
        w.requestFocus();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPopupButton(ActionEvent ev)
    {
        if(!frame.isVisible())
        {
            frame.pack();
            //frame.setLocationRelativeTo(kMain.getTopWindow()); // centers frame
            frame.setVisible(true);
        }
        else
        {
            frame.toFront();
            //frame.requestFocus();
        }
    }
    
    public JButton getButton() { return popupButton; }
//}}}

//{{{ stateChanged -- called when tabs are switched
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        //editpane.setText(text2html(textarea.getText()));
    }
//}}}

//{{{ Mouse listeners (for hypertext)
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {
        int where = textarea.viewToModel(ev.getPoint());
        //System.err.println("Click occurred at position "+where);
    }

    public void mouseEntered(MouseEvent ev)     {}
    public void mouseExited(MouseEvent ev)      {}
    public void mousePressed(MouseEvent ev)     {}
    public void mouseReleased(MouseEvent ev)    {}
//}}}
}//class
