// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
//}}}
/**
* <code>UIText</code> is the kinemage text manager.
* It takes care of displaying and editing the text.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Jun  9 19:06:25 EDT 2002
*/
public class UIText implements MouseListener, KMessage.Subscriber
{
//{{{ Static fields
//}}}

//{{{ INTERFACE: HypertextListener
//##############################################################################
    /**
    * <code>HypertextListener</code> is able to get events when the user
    * selects a Mage-style *{hyperlink}* from the text window.
    *
    * @see UIText#addHypertextListener(HypertextListener)
    * @see UIText#removeHypertextListener(HypertextListener)
    *
    * <p>Copyright (C) 2004-2007 by Ian W. Davis. All rights reserved.
    * <br>Begun on Fri Jul 16 11:47:37 EDT 2004
    */
    public interface HypertextListener //extends ... implements ...
    {
        /**
        * Called by UIText whenever the user selects any Mage-style
        * hyperlink, which is bracked by *{ and *}.
        * @param link   the text of the link, minus the flanking brackets
        */
        public void mageHypertextHit(String link);
    }//class
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    JFrame frame;
    JTextArea textarea;
    JCheckBox allowTextEdits;
    
    JButton popupButton;
    
    Collection mageHypertextListeners = new ArrayList();
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
        
        allowTextEdits = new JCheckBox(new ReflectiveAction("Allow text to be edited", null, this, "onAllowTextEdits"));
        allowTextEdits.setSelected( kMain.getPrefs().getBoolean("textDefaultAllowEdits") );
        
        textarea = new JTextArea();
        textarea.setEditable( allowTextEdits.isSelected() );
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
        textarea.addMouseListener(this);
        textarea.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        //textarea.setFont(new Font("Monospaced", Font.PLAIN, (int)Math.round(12 * kMain.getPrefs().getFloat("fontMagnification"))));
        JScrollPane textScroll = new JScrollPane(textarea);
        textScroll.setPreferredSize(new Dimension(500,400));
        new TextCutCopyPasteMenu(textarea);
        this.addHypertextListener(new MageHypertext(kMain));
        
        frame.getContentPane().add(allowTextEdits, BorderLayout.NORTH);
        frame.getContentPane().add(textScroll, BorderLayout.CENTER);
        kMain.subscribe(this);
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

//{{{ shutdown, deliverMessage
//##################################################################################################
    /** Initiates shutdown by calling dispose() on the frame. */
    public void shutdown()
    {
        frame.dispose();
    }
    
    public void deliverMessage(KMessage msg)
    {
        if(msg.testProg(KMessage.ALL_CLOSED))
            this.setText("");
    }
//}}}

//{{{ cascadeBehind, onPopupButton, onAllowTextEdits, getButton
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
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAllowTextEdits(ActionEvent ev)
    {
        textarea.setEditable( allowTextEdits.isSelected() );
    }
    
    public JButton getButton() { return popupButton; }
//}}}

//{{{ Mouse listeners (for hypertext)
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {
        int where = textarea.viewToModel(ev.getPoint());
        //System.err.println("Click occurred at position "+where);
        
        String text = this.getText();
        int prevOpen, prevClose, nextOpen, nextClose;
        // "where-#" terms below ensure that link is active out through the space between } and *
        // Original code used "where" in all four places, cutting the link short.
        // Passing a negative start index to (last)IndexOf is the same as passing zero.
        prevOpen = text.lastIndexOf("*{", where);
        prevClose = text.lastIndexOf("}*", where-2);
        nextOpen = text.indexOf("*{", where);
        nextClose = text.indexOf("}*", where-1);
        //System.err.println("prevs:" + prevOpen + "," + prevClose + "; nexts:" + nextOpen + "," + nextClose);
        
        //                   Still works if prevClose == -1             Might not be a next hyperlink...
        if(prevOpen != -1 && prevOpen > prevClose && nextClose != -1 && (nextClose < nextOpen || nextOpen == -1))
        {
            String link = text.substring(prevOpen+2, nextClose);
            textarea.select(prevOpen, nextClose+2);
            //System.err.println("Hit hypertext: '"+link+"'");
            for(Iterator iter = mageHypertextListeners.iterator(); iter.hasNext(); )
            {
                HypertextListener listener = (HypertextListener) iter.next();
                listener.mageHypertextHit(link);
            }
        }
    }

    public void mouseEntered(MouseEvent ev)     {}
    public void mouseExited(MouseEvent ev)      {}
    public void mousePressed(MouseEvent ev)     {}
    public void mouseReleased(MouseEvent ev)    {}
//}}}

//{{{ add/removeHypertextListener
//##################################################################################################
    /** Registers a listener for hypertext events. */
    public void addHypertextListener(HypertextListener listener)
    {
        mageHypertextListeners.add(listener);
    }

    /** Registers a listener for hypertext events. */
    public void removeHypertextListener(HypertextListener listener)
    {
        mageHypertextListeners.remove(listener);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class
