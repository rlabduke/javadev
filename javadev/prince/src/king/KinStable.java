// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import java.util.List;
import javax.swing.Timer;
//}}}
/**
* <code>KinStable</code> holds all of the data about one or more kin files.
* It acts as the root for the hierarchy of groups.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Apr 24 11:22:51 EDT 2002
*/
public class KinStable implements ListSelectionListener, KMessage.Subscriber, ActionListener
{
//{{{ Variables
    KingMain            kMain           = null;
    List<Kinemage>      children;
    Kinemage            currentKinemage = null; // the current kinemage within the file
    volatile boolean    isLocked        = false;
    Timer               timer;
    
    JList               kinChooser      = null; // a list of all kinemages present in the stable
//}}}

//{{{ Constructor
//##################################################################################################
    /**
    * Constructor
    */
    public KinStable(KingMain kmain)
    {
        kMain = kmain;
        children = new ArrayList<Kinemage>();
        
        kinChooser = new JList(new DefaultListModel()); // must specify or get a ClassCastEx later!
        kinChooser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        kinChooser.setVisibleRowCount(4);
        kinChooser.setFixedCellWidth(100);
        kinChooser.addListSelectionListener(this);

        kMain.subscribe(this);
        
        // Check for changes to the kinemage N times per second
        timer = new Timer(1000/20, this);
        timer.start();
    }
//}}}

//{{{ getKins, actionPerformed
//##################################################################################################
    /** Returns an unmodifiable list of all open kinemages. */
    public List<Kinemage> getKins()
    { return Collections.unmodifiableList(children); }
    
    /** Called periodically by a Timer to check if anything about the current kinemage has changed. */
    public void actionPerformed(ActionEvent ev)
    {
        if(currentKinemage == null) return;
        
        // We stop the timer in case our duties take longer than its period,
        // in which case all processor time would be eaten by event notifications.
        timer.stop();
        int changes = currentKinemage.queryKinChanged();
        if(changes != 0)
            kMain.publish(new KMessage(currentKinemage, changes));
        timer.start();
    }
//}}}

//{{{ deliverMessage
//##############################################################################
    public void deliverMessage(KMessage msg)
    {
        // doesn't react to messages because all updates are made in appropriate functions
    }

    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    /*void notifyChange(int event_mask)
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
    }*/
//}}}

//{{{ closeAll, closeCurrent, append
//##################################################################################################
    public void closeAll()
    {
        children.clear();
        currentKinemage = null;
        //kMain.getTextWindow().setText("");
        
        //kinChooser.removeAllItems(); // leaks memory!
        // This leak is a bug in JComboBox.removeAllItems()
        // as of v1.4.1 and has been reported to Sun by IWD.
        // The following is a work-around:
        kinChooser.setModel(new DefaultListModel());

        kMain.publish(new KMessage(this, KMessage.ALL_CLOSED));
    }
    
    public void closeCurrent()
    {
        Kinemage oldKin = currentKinemage;
        children.remove(currentKinemage);
        currentKinemage = null;
        int selPos = kinChooser.getSelectedIndex();
        Object selection = kinChooser.getSelectedValue();
        DefaultListModel model = (DefaultListModel)kinChooser.getModel();
        model.removeElement(selection);
        
        selPos = Math.min(selPos, kinChooser.getModel().getSize() - 1);
        if(selPos >= 0) kinChooser.setSelectedIndex(selPos);

        kMain.publish(new KMessage(this, KMessage.KIN_CLOSED));
    }
    
    /**
    * Adds in the specified collection of kinemages.
    * If there is no current kinemage, then the first of these becomes the current kinemage.
    * @param kins a group of Kinemage objects to add (not null)
    */
    public void append(Collection<Kinemage> newKins)
    {
        children.addAll(newKins);
        
        DefaultListModel model = (DefaultListModel) kinChooser.getModel();
        boolean first = true;
        for(Kinemage k : newKins)
        {
            model.addElement(k);
            if(first)
            {
                currentKinemage = k;
                kinChooser.setSelectedValue(k, true);
                first = false;
            }
        }
        // This if statement corrects for a bug(?) in java 1.5
        // which was causing king to not select a kinemage if one
        // was opened using the menu and there were no prior open
        // kins.
        if(kinChooser.getSelectedIndex() == -1)
            kinChooser.setSelectedIndex(0);

        kMain.publish(new KMessage(this, KMessage.KIN_SWITCHED));
    }
///}}}

//{{{ valueChanged, setLocked
//##################################################################################################
    /* Gets called when a new kinemage is picked from the list (kinChooser) */
    public void valueChanged(ListSelectionEvent ev)
    {
        currentKinemage = (Kinemage) kinChooser.getSelectedValue();
        kMain.publish(new KMessage(this, KMessage.KIN_SWITCHED));
    }

    /** Used to control access to kinemage during file loading */
    public synchronized void setLocked(boolean l)
    {
        if(isLocked != l)
        {
            isLocked = l;
            kMain.publish(new KMessage(this, KMessage.KIN_SWITCHED));
        }
    }
//}}}
    
//{{{ getKinemage, getChooser, changeCurrentKinemage
//##################################################################################################
    /** Returns the Kingemage that contains all of the 3-D data being displayed. */
    public synchronized Kinemage getKinemage()
    {
        if(isLocked) return null;
        else return currentKinemage;
    }
    
    /** Returns a JList that lists all the loaded kinemages. */
    public Component getChooser() { return kinChooser; }

    /** Indexing starts from 1 */
    public void changeCurrentKinemage(int kinNum)
    {
        kinNum -= 1;
        if(0 <= kinNum && kinNum < children.size())
            kinChooser.setSelectedValue(children.get(kinNum), true);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class
