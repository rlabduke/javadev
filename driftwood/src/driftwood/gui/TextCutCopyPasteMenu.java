// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

//import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
//import driftwood.*;
//}}}
/**
* <code>TextCutCopyPasteMenu</code> provides a cut/copy/paste context
* menu for JTextComponents. It can be used as a contextual
* (popup) menu, or incorporated into another menu.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jul  8 12:30:59 EDT 2003
*/
public class TextCutCopyPasteMenu implements MouseListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JTextComponent      target;
    JPopupMenu          popupMenu;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public TextCutCopyPasteMenu(JTextComponent target)
    {
        super();
        this.target = target;
        
        createPopupMenu();
        
        target.addMouseListener(this);
    }
//}}}

//{{{ createPopupMenu
//##############################################################################
    private void createPopupMenu()
    {
        ReflectiveAction cutAction, copyAction, pasteAction, selAction;
        cutAction   = new ReflectiveAction("Cut",   null, this, "onCut");
        copyAction  = new ReflectiveAction("Copy",  null, this, "onCopy");
        pasteAction = new ReflectiveAction("Paste", null, this, "onPaste");
        selAction   = new ReflectiveAction("Select all", null, this, "onSelectAll");
        
        popupMenu = new JPopupMenu();
        popupMenu.add(new JMenuItem(cutAction));
        popupMenu.add(new JMenuItem(copyAction));
        popupMenu.add(new JMenuItem(pasteAction));
        popupMenu.addSeparator();
        popupMenu.add(new JMenuItem(selAction));
    }
//}}}

//{{{ mouseClicked, mousePressed, mouseReleased
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    { checkForPopupEvent(ev); }
    public void mouseEntered(MouseEvent ev)
    {}
    public void mouseExited(MouseEvent ev)
    {}
    public void mousePressed(MouseEvent ev)
    { checkForPopupEvent(ev); }
    public void mouseReleased(MouseEvent ev)
    { checkForPopupEvent(ev); }
    
    private void checkForPopupEvent(MouseEvent ev)
    {
        if(ev.isPopupTrigger())
        {
            popupMenu.show(target, ev.getX(), ev.getY());
        }
    }
//}}}

//{{{ on{Cut, Copy, Paste}
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCut(ActionEvent ev)
    {
        target.cut();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCopy(ActionEvent ev)
    {
        target.copy();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPaste(ActionEvent ev)
    {
        target.paste();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSelectAll(ActionEvent ev)
    {
        target.selectAll();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

