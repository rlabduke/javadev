// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;

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
import driftwood.moldb2.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>SidechainSswing</code> is a GUI for repositioning a sidechain
* based on its dihedral angles (chi1, chi2, etc).
*
* <p>Copyright (C) 2004 by Shuren Wang. All rights reserved.
* <br>Begun on Thu May  8 15:36:11 EDT 2004
*/
public class SidechainSswing implements Remodeler, ChangeListener, ListSelectionListener, WindowListener
{
//{{{ Constants
    static final DecimalFormat df1 = new DecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##################################################################################################
    Residue             targetRes;
    ModelManager2       modelman;
    SidechainAngles2    scAngles;
    Rotamer             rotamer;
    SidechainIdealizer  scIdealizer     = null;
    
    JDialog             dialog;
    JCheckBox           cbIdealize;
    JList               rotamerList;
    AngleDial[]         dials;
    JLabel              rotaQuality;
    
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
    public SidechainSswing(Frame frame, Residue target, ModelManager2 mm) throws IOException
    {
        this.targetRes  = target;
        this.modelman   = mm;
        this.scAngles   = new SidechainAngles2();
        this.rotamer    = Rotamer.getInstance();
        try {
            scIdealizer = new SidechainIdealizer();
        } catch(IOException ex) { ex.printStackTrace(SoftLog.err); }

        buildGUI(frame);

        modelman.registerTool(this, Collections.singleton(targetRes));
    }
//}}}

//{{{ buildGUI, getDialPanel
//##################################################################################################
    private void buildGUI(Frame frame)
    {
        // Dials
        TablePane dialPane = new TablePane();
        String[] angleNames = scAngles.nameAllAngles(targetRes);
        if(angleNames == null)
            throw new IllegalArgumentException("Bad residue code '"+targetRes.getName()+"' isn't recognized");
        double[] angleVals = scAngles.measureAllAngles(targetRes, modelman.getMoltenState());
        
        dials = new AngleDial[angleNames.length];
        for(int i = 0; i < angleNames.length; i++)
        {
            dialPane.add(new JLabel(angleNames[i]));
            dials[i] = new AngleDial();
            dials[i].setOrigDegrees(angleVals[i]);
            dials[i].setDegrees(angleVals[i]);
            dials[i].addChangeListener(this);
            dialPane.add(dials[i]);
            dialPane.newRow();
        }
        
        // Top-level pane
        JPanel twistPane = new JPanel(new BorderLayout());
        twistPane.add(dialPane, BorderLayout.WEST);//
        
        // Rotamer list
        RotamerDef[] rotamers = scAngles.getAllRotamers(targetRes);
        if(rotamers == null)
            throw new IllegalArgumentException("Bad residue code '"+targetRes.getName()+"' isn't recognized");
        RotamerDef origRotamer = new RotamerDef();
        origRotamer.rotamerName = "original";
        origRotamer.chiAngles = scAngles.measureChiAngles(targetRes, modelman.getMoltenState());
        RotamerDef[] rotamers2 = new RotamerDef[ rotamers.length+1 ];
        System.arraycopy(rotamers, 0, rotamers2, 1, rotamers.length);
        rotamers2[0] = origRotamer;
        rotamerList = new JList(rotamers2);
        rotamerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rotamerList.addListSelectionListener(this);
        
        // Rotamer quality readout
        rotaQuality = new JLabel();
        rotaQuality.setToolTipText("Quality assessment for the current side-chain conformation");
        setFeedback();
        TablePane rotamerPane = new TablePane();
        rotamerPane.hfill(true).vfill(true).weights(1,1).addCell(new JScrollPane(rotamerList));
        rotamerPane.newRow().weights(1,0).add(rotaQuality);
        twistPane.add(rotamerPane, BorderLayout.CENTER);//

        JLabel label=new JLabel("Run sswing remotely...");
        label.setFont(new Font("Times-Roman", Font.ITALIC, 14));
        twistPane.add(label, BorderLayout.CENTER);
                
        // Other controls
        cbIdealize = new JCheckBox(new ReflectiveAction("Idealize sidechain", null, this, "onIdealizeOnOff"));
        if(scIdealizer != null) cbIdealize.setSelected(true);
        else                    cbIdealize.setEnabled(false);
        twistPane.add(cbIdealize, BorderLayout.NORTH);//
        JButton release = new JButton(new ReflectiveAction("Release", null, this, "onReleaseResidue"));
        twistPane.add(release, BorderLayout.SOUTH);
        
        // Assemble the dialog
        dialog = new JDialog(frame, targetRes.toString(), false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(this);
        dialog.setContentPane(twistPane);
        dialog.pack();
        dialog.setVisible(true);
    }
//}}}

//{{{ onReleaseResidue, onIdealizeOnOff
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /**
    * Checks residues back into the ModelManager.
    * @param ev is ignored, may be null.
    */
    public void onReleaseResidue(ActionEvent ev)
    {
        int reply = JOptionPane.showConfirmDialog(dialog,//
            "Do you want to keep the changes\nyou've made to this residue?",//
            "Keep changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);//
        if(reply == JOptionPane.CANCEL_OPTION) return;//
        
        if(reply == JOptionPane.YES_OPTION)//
            modelman.requestStateChange(this); // // will call this.updateModelState()
        else // // == JOptionPane.NO_OPTION
            modelman.unregisterTool(this);
        
        dialog.dispose();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onIdealizeOnOff(ActionEvent ev)
    {
        stateChanged(null);
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
    /** Gets called when the dials move. */
    public void stateChanged(ChangeEvent ev)
    {
        setFeedback();
        modelman.requestStateRefresh();
    }
    
    /* Gets called when a new rotamer is picked from the list */
    public void valueChanged(ListSelectionEvent ev)
    {
        RotamerDef def = (RotamerDef)rotamerList.getSelectedValue();
        if(def == null)
            SoftLog.err.println("Couldn't retrieve angles for '"+targetRes.getName()+"."+rotamerList.getSelectedValue()+"'");
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
        stateChanged(new ChangeEvent(this));
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
        stateChanged(new ChangeEvent(this));
    }
//}}}

//{{{ setFeedback
//##################################################################################################
    /**
    * Sets the string that will be displayed as feedback
    * on the quality of the currently selected rotamer.
    */
    public void setFeedback()
    {
        //rotaQuality.setText("-???-");
        try
        {
            double score = rotamer.evaluate(targetRes, modelman.getMoltenState()) * 100.0;
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

//{{{ updateModelState
//##################################################################################################
    /**
    * Allows this tool to modify the geometry of the current model.
    * This function is called by the model manager at two times:
    * <ol>
    * <li>When this tool is registered and someone requests
    *   that the molten model be updated</li>
    * <li>When this tool requests that the model be permanently changed.</li>
    * </ol>
    * Tools are absolutely not permitted to modify s: all changes
    * should be done in a new ModelState which should be returned
    * from this function.
    */
    public ModelState updateModelState(ModelState s)
    {
        ModelState ret;
        if(scIdealizer != null && cbIdealize.isSelected())
            ret = scIdealizer.idealizeSidechain(targetRes, s);
        else
            ret = s;
        
        ret = scAngles.setAllAngles(targetRes, ret, this.getAllAngles());
        return ret;            
    }
//}}}

//{{{ Dialog window listeners
//##################################################################################################
    public void windowActivated(WindowEvent ev)   {}
    public void windowClosed(WindowEvent ev)      {}
    public void windowClosing(WindowEvent ev)     { onReleaseResidue(null); }
    public void windowDeactivated(WindowEvent ev) {}
    public void windowDeiconified(WindowEvent ev) {}
    public void windowIconified(WindowEvent ev)   {}
    public void windowOpened(WindowEvent ev)      {}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

