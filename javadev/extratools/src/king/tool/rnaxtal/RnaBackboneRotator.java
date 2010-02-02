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
    ConformerAngles     confAngles;
    Conformer           conformer;
    String              currentBin = "";   // current suite pucker states
    RnaIdealizer        rnaIdealizer     = null;
    
    Window              dialog;
    //JCheckBox           dockOther;
    //JCheckBox           useDaa;
    JList               conformerList;
    JList               atomsList1;
    JList               atomsList2;
    AngleDial[]         dials;
    JLabel              rotaQuality;
    ModelState          changePuckerState = null;
    boolean             doChangePucker = false;
    boolean             doSuperpose = false;
    ArrayList           atomsToSuper;
    
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
        this.confAngles   = new ConformerAngles();
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
        
        // not sure if this is best place to set adjacency map....if 
        // model is fit poorly first then this will fail.
        confAngles.setAdjacency(targetRes1, targetRes2, modelman.getMoltenState());
    }
//}}}

//{{{ buildGUI, getDialPanel
//##################################################################################################
    private void buildGUI(Frame frame)
    {
        // Dials
        TablePane topDialPane = new TablePane();
        String[] angleNames = confAngles.getAngleNames();
        double[] angleVals = confAngles.measureAllAngles(targetRes1, targetRes2, modelman.getMoltenState());
        
        TablePane2 leftDials = new TablePane2();
        TablePane2 centDials = new TablePane2();
        TablePane2 rightDials = new TablePane2();
        dials = new AngleDial[angleNames.length];
        for(int i = 0; i < angleNames.length; i++)
        {
          TablePane2 dialPane = centDials;
          if (angleNames[i].indexOf("chi") > -1) dialPane = leftDials;
          else if (angleNames[i].indexOf("delta") > -1) dialPane = rightDials;
          else {
            leftDials.add(new JLabel(""), 1, 2);
            leftDials.newRow();
            leftDials.newRow();
            rightDials.add(new JLabel(""), 1, 2);
            rightDials.newRow();
            rightDials.newRow();
          }
          dials[i] = new AngleDial();
          dials[i].setOrigDegrees(angleVals[i]);
          dials[i].setDegrees(angleVals[i]);
          dials[i].addChangeListener(this);
          dialPane.add(dials[i]);
          dialPane.newRow();
          dialPane.add(new JLabel(angleNames[i]));
          dialPane.newRow();
        }
        topDialPane.add(leftDials);
        topDialPane.add(centDials);
        topDialPane.add(rightDials);
        
        // Top-level pane
        JPanel twistPane = new JPanel(new BorderLayout());
        twistPane.add(topDialPane, BorderLayout.WEST);
        
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
        String[] conformers = conformer.getDefinedConformerNames();
        conformerList = new JList(conformers);
        conformerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conformerList.setSelectionModel(new ReclickListSelectionModel(conformerList));
        conformerList.addListSelectionListener(this);
        
        // Rotamer quality readout
        //rotaQuality = new JLabel();
        //rotaQuality.setToolTipText("Quality assessment for the current side-chain conformation");
        //setFeedback();
        TablePane2 conformerPane = new TablePane2();
        conformerPane.vfill(false).add(new JLabel("Conformers"));
        conformerPane.add(new JLabel("Pick superpose atoms"));
        conformerPane.newRow();
        //conformerPane.hfill(true).vfill(true).addCell(new JScrollPane(conformerList), 1, 4);
        conformerPane.vfill(true).hfill(true).addCell(new JScrollPane(conformerList), 1, 4);
        //conformerPane.newRow().weights(1,0).add(rotaQuality);
        Object[] res1Atoms = targetRes1.getAtoms().toArray();
        Object[] res2Atoms = targetRes2.getAtoms().toArray();
        atomsList1 = new JList(res1Atoms);
        atomsList1.addListSelectionListener(this);
        atomsList2 = new JList(res2Atoms);
        atomsList2.addListSelectionListener(this);
        
        //conformerPane.newRow();
        conformerPane.hfill(true).vfill(true).add(new JScrollPane(atomsList1));
        conformerPane.newRow();
        conformerPane.vfill(false).add(new JLabel("Residue 1"));
        conformerPane.newRow();
        conformerPane.hfill(true).vfill(true).add(new JScrollPane(atomsList2));
        conformerPane.newRow();
        conformerPane.vfill(false).add(new JLabel("Residue 2"));
        twistPane.add(conformerPane, BorderLayout.EAST);
        
        // Other controls
        TablePane optionPane = new TablePane();
        //dockOther = new JCheckBox(new ReflectiveAction("Dock on second residue", null, this, "onDockOther"));
        //optionPane.addCell(dockOther);
        //cbIdealize = new JCheckBox(new ReflectiveAction("Idealize sidechain", null, this, "onIdealizeOnOff"));
        //if(scIdealizer != null) cbIdealize.setSelected(true);
        //else                    cbIdealize.setEnabled(false);
        //optionPane.addCell(cbIdealize);
        
        //useDaa = new JCheckBox(new ReflectiveAction("Use D-amino acid", null, this, "onDaminoAcid"));
        //useDaa.setSelected(false);
        //optionPane.addCell(useDaa);
        //JButton changePucker = new JButton(new ReflectiveAction("Change puckers", null, this, "onChangePucker"));
        //optionPane.addCell(changePucker);
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
            "Do you want to keep the changes\nyou've made to this suite?",
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
   // public void onDockOther(ActionEvent ev)
   // {
   //   doChangePucker = true;
   //   stateChanged(null);
   //   doChangePucker = false;
   // }
   // 
   // public void onDaminoAcid(ActionEvent ev) {
   //   stateChanged(null);
   // }
//}}}

//{{{ makeOptionPane
private String makeOptionPane() {
  JOptionPane pane = new JOptionPane("Mutate this sidechain to what?", JOptionPane.QUESTION_MESSAGE,
               JOptionPane.OK_CANCEL_OPTION, null);
  pane.setSelectionValues(rnaIdealizer.getPuckerStates().toArray());
  //pane.setInitialSelectionValue(orig.getName());
  JDialog dialog = pane.createDialog(kMain.getTopWindow(), "Choose mutation");
  pane.selectInitialValue();
 // JCheckBox usePdbv2Box = new JCheckBox("Use PDB v2.3 (old) format");
 // usePdbv2Box.setPreferredSize(new Dimension(300, 30)); // I arrived at these values through
 // usePdbv2Box.setMaximumSize(new Dimension(300, 30));   //    a lot of trial and error just
 // usePdbv2Box.setAlignmentX((float)0.5);                //    to get the check box right.
 // usePdbv3 = true;
 // usePdbv2Box.addItemListener(this);
 // pane.add(usePdbv2Box);
  //System.out.println(usePdbv2Box.getPreferredSize());
  //System.out.println(usePdbv2Box.getMaximumSize());
  //System.out.println(usePdbv2Box.getAlignmentX());
  dialog.setVisible(true);
  return (String)pane.getInputValue();
}
//}}}

  //{{{ changePucker
  public ModelState changePuckers(String puckers, ModelState state) {
    try
        {
            //String choice = makeOptionPane();
            //if((choice == null)||(choice.equals(JOptionPane.UNINITIALIZED_VALUE))) return; // user canceled operation
            
            // Create the mutated sidechain
            //ModelState newState = new ModelState(modelman.getMoltenState());
            ArrayList orig = new ArrayList();
            orig.add(targetRes1);
            orig.add(targetRes2);
            changePuckerState = rnaIdealizer.makeIdealResidue(state, orig, puckers, false);
            
            //modelman.requestStateRefresh();

            double[] angleVals = confAngles.measureAllAngles(targetRes1, targetRes2, changePuckerState, true);
            //System.out.print("measured angles of changed pucker: ");
            //for (double d : angleVals) {
            //  System.out.print(df1.format(d) + ":");
            //}
            //System.out.println();
            setAllDials(angleVals);
            return changePuckerState;
            //modelman.requestStateRefresh();
            //changePuckerState = null;
            // Align it on the old backbone
            //newState = rnaIdealizer.dockResidue(newResidues, newState, orig, modelman.getMoltenState());
            
       //     modelman.requestStateRefresh();
       //     // Create the mutated model
       //     ArrayList newResList = new ArrayList(newResidues);
       //     Model newModel = (Model) modelman.getModel().clone();
       //     newModel.replace(targetRes1, (Residue)newResList.get(0));
       //     newModel.replace(targetRes2, (Residue)newResList.get(1));
       //     targetRes1 = (Residue)newResList.get(0);
       //     targetRes2 = (Residue)newResList.get(1);
       //     // Remove any unnecessary AtomStates from the model
       //     newState = newState.createForModel(newModel);
       //     
       //     // Insert the mutated model into the model manager
       //     modelman.replaceModelAndState(newModel, newState);
       //     
       //     modelman.registerTool(this, newResList);
            // Make a note in the headers
           // modelman.addUserMod("Mutated "+orig+" to "+newRes);
            
            // Set it up for rotation
            //try {
            //    new SidechainRotator(kMain, newRes, modelman);
            //} catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
            
        }
    //    catch(ResidueException ex)
    //    {
    //        ex.printStackTrace(SoftLog.err);
    //    }
        catch(AtomException ex)
        {
            ex.printStackTrace(SoftLog.err);
        }
        return null;
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
      JList hitList = (JList) ev.getSource();
      if (hitList.equals(conformerList)) {
        String confName = (String)conformerList.getSelectedValue();
        if(confName != null) {
          String bin = conformer.getConformerBin(confName);
          if (!bin.equals(currentBin)) {
            doChangePucker = true;
            //changePuckers(bin);
            currentBin = bin;
            stateChanged(new ChangeEvent(this));
            doChangePucker = false;
          }
          initSomeAngles(conformer.getMeanValues(confName));
        }
        // else there is no current selection
      } else if (hitList.equals(atomsList1)||hitList.equals(atomsList2)) {
        Object[] atoms1 = atomsList1.getSelectedValues();
        Object[] atoms2 = atomsList2.getSelectedValues();
        atomsToSuper = new ArrayList();
        if (atoms1.length + atoms2.length > 2) {
          // if more than 3 atoms picked, then do superposition
          ArrayList atomNames = new ArrayList();
          for (Object o : atoms1) {
            Atom at = (Atom) o;
            atomNames.add(at.getName());
          }
          atomsToSuper.add(atomNames);
          atomNames = new ArrayList();
          for (Object o : atoms2) {
            Atom at = (Atom) o;
            atomNames.add(at.getName());
          }
          atomsToSuper.add(atomNames);
          doSuperpose = true;
          stateChanged(new ChangeEvent(this));
          doSuperpose = false;
        }
      }
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

//{{{ setAllDials
    /** (measured in degrees) */
    public void setAllDials(double[] angles)
    {
        if(angles.length < dials.length)
            throw new IllegalArgumentException("Not enough angles provided!");
        
        isUpdating(true);
        for(int i = 0; i < dials.length; i++)
            dials[i].setDegrees(angles[i]);
        isUpdating(false);
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
            //double score = conformer.evaluate(targetRes1, targetRes2, modelman.getMoltenState()) * 100.0;
            double score = 100;
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
      //if (changePuckerState != null) {
      //  ret = changePuckerState;
      //}
      //if (dockOther.isSelected()) {
      //  rnaIdealizer.setDockResidue(1);
      //} else {
      //  rnaIdealizer.setDockResidue(0);
      //}
      if (doChangePucker) ret = changePuckers(currentBin, s);

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
        //System.out.println("Setting angles: ");
        //double[] angles = this.getAllAngles();
        //for (double d : angles) {
        //  System.out.print(df1.format(d) + ":");
        //}
        //System.out.println(" on the following state: ");
        //System.out.println(ret.debugStates(20));
        //ArrayList rezzes = new ArrayList();
        //rezzes.add(targetRes1);
        //rezzes.add(targetRes2);
        //rnaIdealizer.debugModelState(rezzes, ret, "pre-setAngles.pdb");
        ret = confAngles.setAllAngles(targetRes1, targetRes2, ret, this.getAllAngles());
        
        try {
          if (doSuperpose) {
            ArrayList rezzes = new ArrayList();
            rezzes.add(targetRes1);
            rezzes.add(targetRes2);
            //Model frozen = modelman.getModel();
            //Residue frozRes1 = frozen.getResidue(targetRes1.getCNIT());
            //Residue frozRes2 = frozen.getResidue(targetRes2.getCNIT());
            //ArrayList frozRezzes = new ArrayList();
            //frozRezzes.add(frozRes1);
            //frozRezzes.add(frozRes2);
            ret = rnaIdealizer.dockResidues(rezzes, ret, rezzes, modelman.getFrozenState(), atomsToSuper);
            
          }
        } catch (AtomException ae) {
          ae.printStackTrace(SoftLog.err);
        }
        //rnaIdealizer.debugModelState(rezzes, ret, "post-setAngles.pdb");
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

