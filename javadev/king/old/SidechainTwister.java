// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool;
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
import javax.swing.event.*;
import chiropraxis.sc.*;
import chiropraxis.rotarama.*;
import driftwood.gui.*;
import driftwood.moldb.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>SidechainTwister</code> is a GUI for repositioning a sidechain
* based on its dihedral angles (chi1, chi2, etc).
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu May  8 15:36:11 EDT 2003
*/
public class SidechainTwister implements ChangeListener, ListSelectionListener
{
//{{{ Constants
    static final DecimalFormat df1 = new DecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##################################################################################################
    String              resCode;        // lowercase
    SidechainAngles     scAngles;
    Rotamer             rotamer;
    JPanel              twistPane;
    JList               rotamerList;
    AngleDial[]         dials;
    JLabel              rotaQuality;
    
    /** List of listeners for ChangeEvents */
    Collection  changeListeners = new ArrayList();
    /** Marker for logical multi-dial update */
    boolean     isUpdating      = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @throws IllegalArgumentException if the residue code isn't recognized
    * @throws IOException if the needed resource(s) can't be loaded from the JAR file
    * @throws NoSuchElementException if the resource is missing a required entry
    */
    public SidechainTwister(String rescode) throws IOException
    {
        this.resCode    = rescode.toLowerCase();
        this.scAngles   = new SidechainAngles();
        this.rotamer    = Rotamer.getInstance();
        buildGUI();
    }
//}}}

//{{{ buildGUI, getDialPanel
//##################################################################################################
    private void buildGUI()
    {
        // Dials
        TablePane dialPane = new TablePane();
        String[] angleNames = scAngles.nameAllAngles(resCode);
        if(angleNames == null)
            throw new IllegalArgumentException("Bad residue code '"+resCode+"' isn't recognized");
        
        dials = new AngleDial[angleNames.length];
        for(int i = 0; i < angleNames.length; i++)
        {
            dialPane.add(new JLabel(angleNames[i]));
            dials[i] = new AngleDial();
            dials[i].addChangeListener(this);
            dialPane.add(dials[i]);
            dialPane.newRow();
        }
        
        // Top-level pane
        twistPane = new JPanel(new BorderLayout());
        twistPane.add(dialPane, BorderLayout.WEST);
        
        // Rotamer list
        RotamerDef[] rotamers = scAngles.getAllRotamers(resCode);
        if(rotamers == null)
            throw new IllegalArgumentException("Bad residue code '"+resCode+"' isn't recognized");
        rotamerList = new JList(rotamers);
        rotamerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rotamerList.addListSelectionListener(this);
        
        // Rotamer quality readout
        rotaQuality = new JLabel(" "); // placeholder
        rotaQuality.setToolTipText("Quality assessment for the current side-chain conformation");
        
        TablePane rotamerPane = new TablePane();
        rotamerPane.hfill(true).vfill(true).weights(1,1).addCell(new JScrollPane(rotamerList));
        rotamerPane.newRow().weights(1,0).add(rotaQuality);
        
        twistPane.add(rotamerPane, BorderLayout.CENTER);
    }
    
    /** Returns the (unique) GUI representation of this object */
    public Component getDialPanel()
    {
        return twistPane;
    }
//}}}

//{{{ add/removeChangeListener, fireStateChanged
//##################################################################################################
    public void addChangeListener(ChangeListener l)
    {
        changeListeners.add(l);
    }
    
    public void removeChangeListener(ChangeListener l)
    {
        changeListeners.remove(l);
    }
    
    /** Notifies all listeners and repaints this component */
    protected void fireStateChanged(ChangeEvent ev)
    {
        for(Iterator iter = changeListeners.iterator(); iter.hasNext(); )
        {
            ((ChangeListener)iter.next()).stateChanged(ev);
        }
    }
//}}}

//{{{ isUpdating
//##################################################################################################
    /**
    * Implements a means of updating several dials at once
    * while maintaining getValueIsAdjusting() == true.
    * This is useful to us in e.g. setAllAngles().
    */
    public boolean isUpdating()
    { return isUpdating; }
    
    public void isUpdating(boolean b)
    { isUpdating = b; }
//}}}

//{{{ stateChanged, valueChanged, getValueIsAdjusting
//##################################################################################################
    /**
    * Acts as a broadcast point -- all messages from the dials
    * are propagated unaltered to this components listeners
    */
    public void stateChanged(ChangeEvent ev)
    {
        fireStateChanged(ev);
    }
    
    /* Gets called when a new rotamer is picked from the list */
    public void valueChanged(ListSelectionEvent ev)
    {
        RotamerDef def = (RotamerDef)rotamerList.getSelectedValue();
        if(def == null)
            SoftLog.err.println("Couldn't retrieve angles for '"+resCode+"."+rotamerList.getSelectedValue()+"'");
        else
            initSomeAngles(def.chiAngles);
    }

    /** Returns true if any of the dials is currently being updated */
    public boolean getValueIsAdjusting()
    {
        if(isUpdating()) return true;
        
        for(int i = 0; i < dials.length; i++)
        {
            if(dials[i].getValueIsAdjusting()) return true;
        }
        return false;
    }
//}}}

//{{{ get/set/initAllAngles, initSomeAngles
//##################################################################################################
    /** (measured in degrees) */
    public double[] getAllAngles()
    {
        double[] angles = new double[dials.length];
        for(int i = 0; i < dials.length; i++)
            angles[i] = dials[i].getDegrees();
        return angles;
    }
    
    /** (measured in degrees) */
    public void setAllAngles(double[] angles)
    {
        if(angles.length < dials.length)
            throw new IllegalArgumentException("Not enough angles provided!");
        
        isUpdating(true);
        for(int i = 0; i < dials.length; i++)
            dials[i].setDegrees(angles[i]);
        isUpdating(false);
        fireStateChanged(new ChangeEvent(this));
    }
    
    /** (measured in degrees) */
    public void initAllAngles(double[] angles)
    {
        if(angles.length < dials.length)
            throw new IllegalArgumentException("Not enough angles provided!");

        initSomeAngles(angles);
    }
    
    /** Doesn't check to make sure there are enough angles to set all dials*/
    public void initSomeAngles(double[] angles)
    {
        int len = Math.min(angles.length, dials.length);
        isUpdating(true);
        for(int i = 0; i < len; i++)
        {
            dials[i].setOrigDegrees(angles[i]);
            dials[i].setDegrees(angles[i]);
        }
        isUpdating(false);
        fireStateChanged(new ChangeEvent(this));
    }
//}}}

//{{{ setFeedback
//##################################################################################################
    /**
    * Sets the string that will be displayed as feedback
    * on the quality of the currently selected rotamer.
    */
    public void setFeedback(AminoAcid aa)
    {
        if(aa == null)
        {
            rotaQuality.setText("-");
            return;
        }
        
        try
        {
            double score = rotamer.evaluate(aa) * 100.0;
            String eval;
            if(score > 20)          eval = "Excellent";
            else if(score > 10)     eval = "Good";
            else if(score >  2)     eval = "Fair";
            else if(score >  1)     eval = "Poor";
            else                    eval = "OUTLIER";
            rotaQuality.setText(eval+" ("+df1.format(score)+"%)");
        }
        catch(IllegalArgumentException ex)
        {
            rotaQuality.setText("-");
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

