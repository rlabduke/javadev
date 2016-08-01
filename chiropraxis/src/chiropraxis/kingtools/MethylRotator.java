// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
import king.*;

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
* <code>MethylRotator</code> is a GUI for repositioning a sidechain
* based on rotations of its methyl groups.
*
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Fri Jul  1 2011
*/
public class MethylRotator implements Remodeler, ChangeListener, ListSelectionListener, WindowListener
{
//{{{ Constants
    static final DecimalFormat df1 = new DecimalFormat("+0.0;-0.0");
    static final DecimalFormat df0 = new DecimalFormat("0");
    
    static final Color normalColor  = new Color(0f, 0f, 0f);
    static final Color mediumColor  = new Color(0.3f, 0f, 0f);
    static final Color alertColor   = new Color(0.6f, 0f, 0f);
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain            kMain;
    Residue             targetRes;
    ModelManager2       modelman;
    SidechainAngles2    scAngles;
    Rotamer             rotamer;
    SidechainIdealizer  scIdealizer     = null;
    SidechainsLtoD      scFlipper       = null;
    
    Window              dialog;
    JCheckBox           cbIdealize;
    JCheckBox           useDaa;
    JList               rotamerList;
    AngleDial[]         dials;
    JLabel[]            labels;
    
    /** Marker for logical multi-dial update */
    boolean     isUpdating      = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @throws IOException if the needed resource(s) can't be loaded from the JAR file
    * @throws IllegalArgumentException if the residue code isn't recognized
    */
    public MethylRotator(KingMain kMain, Residue target, ModelManager2 mm) throws IOException
    {
        this.kMain       = kMain;
        this.targetRes   = target;
        this.modelman    = mm;
        this.scAngles    = new SidechainAngles2();
        //this.rotamer    = Rotamer.getInstance();
        //this.scFlipper  = new SidechainsLtoD();
        try { this.scIdealizer = new SidechainIdealizer(); }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }

        buildGUI(kMain.getTopWindow());

        modelman.registerTool(this, Collections.singleton(targetRes));
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI(Frame frame)
    {
        // Dials
        TablePane dialPane = new TablePane();
        String[] angleNames = scAngles.nameMethylAngles(targetRes);
        if(angleNames == null)
            throw new IllegalArgumentException("Bad residue code '"+targetRes.getName()+"' isn't recognized");
        if(angleNames.length == 0)
            throw new IllegalArgumentException("Residue type '"+targetRes.getName()+"' doesn't have any methyls");
        double[] angleVals = scAngles.measureMethylAngles(targetRes, modelman.getMoltenState());
        
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
        
        // Warnings for large methyl rotations (analogous to rotamer quality readout)
        TablePane labelPane = new TablePane();
        
        labels = new JLabel[angleNames.length];
        for(int i = 0; i < angleNames.length; i++)
        {
            labels[i] = new JLabel();
            labels[i].setToolTipText("Quality assessment for the current methyl rotation");
            labelPane.add(labels[i]);
            labelPane.newRow();
        }
        setFeedback();
        
        // Top-level pane
        JPanel twistPane = new JPanel(new BorderLayout());
        twistPane.add(dialPane, BorderLayout.WEST);
        twistPane.add(labelPane, BorderLayout.CENTER);
        
        // Other controls
        TablePane optionPane = new TablePane();
        cbIdealize = new JCheckBox(new ReflectiveAction("Idealize sidechain", null, this, "onIdealizeOnOff"));
        if(scIdealizer != null) cbIdealize.setSelected(true);
        else                    cbIdealize.setEnabled(false);
        optionPane.addCell(cbIdealize);
        twistPane.add(optionPane, BorderLayout.NORTH);
        
        JButton btnRelease  = new JButton(new ReflectiveAction("Finished", null, this, "onReleaseResidue"));
        JButton btnRotateSc = new JButton(new ReflectiveAction("Rotate sidechain", null, this, "onRotateSidechain"));
        TablePane2 btnPane = new TablePane2();
        btnPane.addCell(btnRelease);
        btnPane.addCell(btnRotateSc);
        twistPane.add(btnPane, BorderLayout.SOUTH);
        
        // Assemble the dialog
        if(kMain.getPrefs().getBoolean("minimizableTools"))
        {
            JFrame fm = new JFrame(targetRes.toString()+" methyl rotator");
            fm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            fm.setContentPane(twistPane);
            dialog = fm;
        }
        else
        {
            JDialog dial = new JDialog(frame, targetRes.toString()+" methyl rotator", false);
            dial.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dial.setContentPane(twistPane);
            dialog = dial;
        }
        dialog.addWindowListener(this);
        dialog.pack();
        dialog.setVisible(true);
    }
//}}}

//{{{ onReleaseResidue, onRotateSidechain, onIdealizeOnOff
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /**
    * Checks residues back into the ModelManager.
    * @param ev is ignored, may be null.
    */
    public void onReleaseResidue(ActionEvent ev)
    {
        int reply = JOptionPane.showConfirmDialog(dialog,
            "Do you want to keep the changes\nyou've made to this residue?",
            "Keep changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(reply == JOptionPane.CANCEL_OPTION) return;
        
        if(reply == JOptionPane.YES_OPTION)
        {
            modelman.requestStateChange(this); // will call this.updateModelState()
            modelman.addUserMod("Refit sidechain methyl(s) of "+targetRes);
        }
        else //  == JOptionPane.NO_OPTION
            modelman.unregisterTool(this);
        
        dialog.dispose();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /**
    * Launches a SidechainRotator window for this residue.
    * @param ev is ignored, may be null.
    */
    public void onRotateSidechain(ActionEvent ev)
    {
        try
        {
            new SidechainRotator(kMain, targetRes, modelman);
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
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
        // This keeps us from running Probe N times when a rotamer with
        // N angles is selected from the list! (Maybe?)
        if(!isUpdating())
            modelman.requestStateRefresh();
        setFeedback(); // otherwise, we evaluate the old conformation
    }
    
    /* Gets called when a new rotamer is picked from the list */
    public void valueChanged(ListSelectionEvent ev)
    {
        RotamerDef def = (RotamerDef)rotamerList.getSelectedValue();
        if(def != null) initSomeAngles(def.chiAngles);
        // else there is no current selection
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
    
    /** Doesn't check to make sure there are enough angles to set all dials */
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
        for(int i = 0; i < dials.length; i++)
        {
            double rotAngle = Math.abs(dials[i].getDegrees() - dials[i].getOrigDegrees());
            if(Double.isNaN(rotAngle))
            {
                labels[i].setText("-?-");
            }
            else if(rotAngle > 15)
            {
                labels[i].setText("LARGE CHANGE");
                labels[i].setForeground(alertColor);
            }
            else if(rotAngle > 10)
            {
                labels[i].setText("medium change");
                labels[i].setForeground(alertColor);
            }
            else if(rotAngle > 5)
            {
                labels[i].setText("medium change");
                labels[i].setForeground(mediumColor);
            }
            else if(rotAngle > 0)
            {
                labels[i].setText("small change");
                labels[i].setForeground(normalColor);
            }
            else // rotAngle == 0
            {
                labels[i].setText("no change");
                labels[i].setForeground(normalColor);
            }
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
        ModelState ret = s;
        if(scIdealizer != null && cbIdealize.isSelected())
            ret = scIdealizer.idealizeSidechain(targetRes, s);
        // Set methyl angles using "all" angles from this class (i.e. all *methyl* angles)
        //ret = scAngles.setAllAngles(targetRes, ret, this.getAllAngles());
        ret = scAngles.setMethylAngles(targetRes, ret, this.getAllAngles());
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

