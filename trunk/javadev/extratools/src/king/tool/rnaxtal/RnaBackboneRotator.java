// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;
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
import chiropraxis.kingtools.*;
import chiropraxis.sc.*;
import chiropraxis.rotarama.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>RnaBackboneRotator</code> is a GUI for repositioning a sidechain
* based on its dihedral angles (chi1, chi2, etc).
*
* <p>Copyright (C) 2010 by Vincent B Chen. All rights reserved.
* <br>Begun on Mon Jan 04 15:59:57 EST 2010 
*/
public class RnaBackboneRotator implements Remodeler, ChangeListener, ListSelectionListener, WindowListener
{
//{{{ Constants
    static final DecimalFormat df1 = new DecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain            kMain;
    Residue             targetRes1;
    Residue             targetRes2;
    ModelManager2       modelman;
    //SidechainAngles2    scAngles;
    Conformer           conformer;
    RnaIdealizer        rnaIdealizer     = null;
    
    Window             dialog;
    JCheckBox           cbIdealize;
    JCheckBox           useDaa;
    JList               conformerList;
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
    public RnaBackboneRotator(KingMain kMain, Residue target, Residue targ2, ModelManager2 mm) throws IOException
    {
        this.kMain      = kMain;
        this.targetRes1  = target;
        this.targetRes2  = targ2;
        this.modelman   = mm;
        //this.scAngles   = new SidechainAngles2();
        this.conformer    = Conformer.getInstance();
        //this.scFlipper  = new SidechainsLtoD();
        try {
            rnaIdealizer = new RnaIdealizer();
        } catch(IOException ex) { ex.printStackTrace(SoftLog.err); }

        buildGUI(kMain.getTopWindow());
        
        ArrayList residues = new ArrayList();
        residues.add(targetRes1);
        residues.add(targetRes2);
        modelman.registerTool(this, residues);
    }
//}}}

//{{{ buildGUI, getDialPanel
//##################################################################################################
    private void buildGUI(Frame frame)
    {
        // Dials
        TablePane dialPane = new TablePane();
        String[] angleNames = conformer.getAngleNames();
        double[] angleVals = conformer.measureAllAngles(targetRes1, targetRes2, modelman.getMoltenState());
        
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
        twistPane.add(dialPane, BorderLayout.WEST);
        
        // Rotamer list
        //RotamerDef[] conformers = scAngles.getAllRotamers(targetRes);
        //if(conformers == null)
        //    throw new IllegalArgumentException("Bad residue code '"+targetRes.getName()+"' isn't recognized");
        //RotamerDef origRotamer = new RotamerDef();
        //origRotamer.conformerName = "original";
        //origRotamer.chiAngles = scAngles.measureChiAngles(targetRes, modelman.getMoltenState());
        //RotamerDef[] conformers2 = new RotamerDef[ conformers.length+1 ];
        //System.arraycopy(conformers, 0, conformers2, 1, conformers.length);
        //conformers2[0] = origRotamer;
        //conformerList = new JList(conformers2);
        //conformerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //conformerList.setSelectionModel(new ReclickListSelectionModel(conformerList));
        //conformerList.addListSelectionListener(this);
        
        // Rotamer quality readout
        //rotaQuality = new JLabel();
        //rotaQuality.setToolTipText("Quality assessment for the current side-chain conformation");
        //setFeedback();
        //TablePane conformerPane = new TablePane();
        //conformerPane.hfill(true).vfill(true).weights(1,1).addCell(new JScrollPane(conformerList));
        //conformerPane.newRow().weights(1,0).add(rotaQuality);
        //twistPane.add(conformerPane, BorderLayout.CENTER);
        
        // Other controls
        TablePane optionPane = new TablePane();
        //cbIdealize = new JCheckBox(new ReflectiveAction("Idealize sidechain", null, this, "onIdealizeOnOff"));
        //if(scIdealizer != null) cbIdealize.setSelected(true);
        //else                    cbIdealize.setEnabled(false);
        //optionPane.addCell(cbIdealize);
        
        //useDaa = new JCheckBox(new ReflectiveAction("Use D-amino acid", null, this, "onDaminoAcid"));
        //useDaa.setSelected(false);
        //optionPane.addCell(useDaa);
        //twistPane.add(optionPane, BorderLayout.NORTH);
        
        JButton btnRelease = new JButton(new ReflectiveAction("Finished", null, this, "onReleaseResidue"));
        //JButton btnBackrub = new JButton(new ReflectiveAction("BACKRUB mainchain", null, this, "onBackrub"));
        TablePane2 btnPane = new TablePane2();
        btnPane.addCell(btnRelease);
        //btnPane.addCell(btnBackrub);
        twistPane.add(btnPane, BorderLayout.SOUTH);
        
        // Assemble the dialog
        if (kMain.getPrefs().getBoolean("minimizableTools")) {
          JFrame fm = new JFrame(targetRes1.toString()+" rotator");
          fm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
          fm.setContentPane(twistPane);
          dialog = fm;
        } else {
          JDialog dial = new JDialog(frame, targetRes1.toString(), false);
          dial.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
          dial.setContentPane(twistPane);
          dialog = dial;
        }
        dialog.addWindowListener(this);
        dialog.pack();
        dialog.setVisible(true);
    }
//}}}

//{{{ onReleaseResidue, onIdealizeOnOff, onDaminoAcid
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
            modelman.addUserMod("Refit backbone of "+targetRes1+" & "+targetRes2);
        }
        else //  == JOptionPane.NO_OPTION
            modelman.unregisterTool(this);
        
        dialog.dispose();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    //public void onBackrub(ActionEvent ev)
    //{
    //    try { new BackrubWindow(kMain, targetRes, modelman); }
    //    catch(IllegalArgumentException ex)
    //    {
    //        JOptionPane.showMessageDialog(kMain.getTopWindow(),
    //            targetRes+"doesn't have neighbors in the same chain.\n",
    //            "Sorry!", JOptionPane.ERROR_MESSAGE);
    //    }
    //}

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onIdealizeOnOff(ActionEvent ev)
    {
        stateChanged(null);
    }
    
    public void onDaminoAcid(ActionEvent ev) {
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
        // This keeps us from running Probe N times when a conformer with
        // N angles is selected from the list! (Maybe?)
        if(!isUpdating())
            modelman.requestStateRefresh();
        setFeedback(); // otherwise, we evaluate the old conformation
    }
    
    /* Gets called when a new conformer is picked from the list */
    public void valueChanged(ListSelectionEvent ev)
    {
        RotamerDef def = (RotamerDef)conformerList.getSelectedValue();
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
    * on the quality of the currently selected conformer.
    */
    public void setFeedback()
    {
        //rotaQuality.setText("-???-");
        try
        {
            double score = conformer.evaluate(targetRes1, targetRes2, modelman.getMoltenState()) * 100.0;
            String eval;
            if(score > 20)          eval = "Excellent";
            else if(score > 10)     eval = "Good";
            else if(score >  2)     eval = "Fair";
            else if(score >  1)     eval = "Poor";
            else                    eval = "OUTLIER";
            //confQuality.setText(eval+" ("+df1.format(score)+"%)");
        }
        catch(IllegalArgumentException ex)
        {
            //confQuality.setText("-");
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
        //if(scIdealizer != null && cbIdealize.isSelected())
        //    ret = scIdealizer.idealizeSidechain(targetRes, s);
        //if (useDaa.isSelected()) {
        //  try {
        //    ret = scFlipper.changeSidechainLtoD(targetRes, s);
        //  } catch (AtomException ae) {
        //    ae.printStackTrace(SoftLog.err);
        //    ret = s;
        //  }
        //}
        
        ret = conformer.setAllAngles(targetRes1, targetRes2, ret, this.getAllAngles());
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
