// (jEdit options) :folding=explicit:collapseFolds=1:
package king;
import king.core.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/**
 * <code>KinStable</code> holds all of the data about one or more kin files.
 * It acts as the root for the hierarchy of groups.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Wed Apr 24 11:22:51 EDT 2002
*/
public class KinStable implements ListSelectionListener
{
//{{{ Variables
    KingMain kMain = null;
    java.util.List children;
    Kinemage currentKinemage = null; // the current kinemage within the file
    volatile boolean isLocked = false;
    
    JList kinChooser = null; // a list of all kinemages present in the stable
//}}}

//{{{ Constructor
//##################################################################################################
    /**
    * Constructor
    */
    public KinStable(KingMain kmain)
    {
        kMain = kmain;
        children = new ArrayList(10);
        
        kinChooser = new JList(new DefaultListModel());
        kinChooser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        kinChooser.setVisibleRowCount(4);
        kinChooser.setFixedCellWidth(100);
        kinChooser.addListSelectionListener(this);
    }
//}}}

//{{{ iterator
//##################################################################################################
    /** Returns an iterator over the children of this element. All children will be Kinemages. */
    public ListIterator iterator()
    { return children.listIterator(); }
//}}}

//{{{ notifyChange
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    void notifyChange(int event_mask)
    {
        // Take care of yourself
        
        // Notify children
        Kinemage    kin     = this.getKinemage();
        KinCanvas   canvas  = kMain.getCanvas();
        Engine      engine  = (canvas==null? null : canvas.getEngine());
        
        if((event_mask & KingMain.EM_SWITCH) != 0
            && kin != null && engine != null)
        {
            canvas.getToolBox().services.doFlatland.setSelected(kin.atFlat);
            engine.whiteBackground  = kin.atWhitebackground;
            engine.usePerspective   = kin.atPerspective;
            engine.colorByList      = kin.atListcolordominant;
            
            if(kin.atOnewidth)
            {
                engine.cueThickness = false;
                engine.thinLines    = false;
            }
            else if(kin.atThinline)
            {
                engine.cueThickness = false;
                engine.thinLines    = true;
            }
            else
            {
                //engine.cueThickness = true;
                engine.thinLines    = false;
            }
            
            UIMenus menus = kMain.getMenus();
            if(menus != null) menus.displayMenu.syncCheckboxes();
            // canvas will redraw itself in a moment, anyway...
        }
        
        // Update animations to include new animate/2animate groups
        if((event_mask & KingMain.EM_EDIT_GROSS) != 0
            && kin != null)
        {
            kin.rebuildAnimations(false);
        }
    }
//}}}

//{{{ closeAll, closeCurrent, append
//##################################################################################################
    public void closeAll()
    {
        children.clear();
        currentKinemage = null;
        kMain.getTextWindow().setText("");
        //kinChooser.removeAllItems(); // leaks memory!
        // This leak is a bug in JComboBox.removeAllItems()
        // as of v1.4.1 and has been reported to Sun by IWD.
        // The following is a work-around:
        kinChooser.setModel(new DefaultListModel());

        kMain.notifyChange(kMain.EM_CLOSEALL);
    }
    
    public void closeCurrent()
    {
        children.remove(currentKinemage);
        currentKinemage = null;
        Object selection = kinChooser.getSelectedValue();
        DefaultListModel model = (DefaultListModel)kinChooser.getModel();
        model.removeElement(selection);

        kMain.notifyChange(kMain.EM_CLOSE);
    }
    
    /**
    * Adds in the specified collection of kinemages.
    * If there is no current kinemage, then the first of these becomes the current kinemage.
    * @param kins a group of Kinemage objects to add (not null)
    */
    public void append(Collection kins)
    {
        children.addAll(kins);
        
        DefaultListModel model = (DefaultListModel)kinChooser.getModel();
        Iterator iter = kins.iterator();
        if(iter.hasNext())
        {
            currentKinemage = (Kinemage)iter.next();
            currentKinemage.signal.subscribe(kMain);
            model.addElement(currentKinemage);
            kinChooser.setSelectedValue(currentKinemage, true);
        }
        while(iter.hasNext())
        {
            Kinemage k = (Kinemage)iter.next();
            k.signal.subscribe(kMain);
            model.addElement(k);
        }

        kMain.notifyChange(kMain.EM_SWITCH);
    }
///}}}

//{{{ valueChanged, setLocked
//##################################################################################################
    /* Gets called when a new kinemage is picked from the list (kinChooser) */
    public void valueChanged(ListSelectionEvent ev)
    {
        currentKinemage = (Kinemage)kinChooser.getSelectedValue();
        kMain.notifyChange(kMain.EM_SWITCH);
    }

    /** Used to control access to kinemage during file loading */
    public synchronized void setLocked(boolean l)
    {
        if(isLocked != l)
        {
            isLocked = l;
            kMain.notifyChange(kMain.EM_SWITCH);
        }
    }
//}}}
    
//{{{ getKinemage(), getChooser()
//##################################################################################################
    /** Returns the Kingemage that contains all of the 3-D data being displayed. */
    public synchronized Kinemage getKinemage()
    {
        if(isLocked) return null;
        else return currentKinemage;
    }
    
    /** Returns a JList that lists all the loaded kinemages. */
    public Component getChooser() { return kinChooser; }
//}}}
}//class
