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
import driftwood.util.*;
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
    ArrayList           targetResidues;
    String              resInfo1;
    String              resInfo2;
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
    JLabel              confQuality;
    ModelState          changePuckerState = null;
    boolean             doChangePucker = false;
    //boolean             doSuperpose = false;
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
    public RnaBackboneRotator(KingMain kMain, Residue targ1, Residue targ2, ModelManager2 mm) throws IOException
    {
        this.kMain      = kMain;
        this.targetRes1  = targ1;
        this.targetRes2  = targ2;
        this.resInfo1 = "molt:"+mm.getModel().getName()+":"+targ1.getChain()+":"+targ1.getSequenceNumber()+":"+targ1.getInsertionCode()+":"+targ1.getName()+":";
        this.resInfo2 = "molt:"+mm.getModel().getName()+":"+targ2.getChain()+":"+targ2.getSequenceNumber()+":"+targ2.getInsertionCode()+":"+targ2.getName()+":";
        this.modelman   = mm;
        this.confAngles   = new ConformerAngles();
        this.conformer    = Conformer.getInstance();
        //this.scFlipper  = new SidechainsLtoD();
        try {
            rnaIdealizer = new RnaIdealizer();
        } catch(IOException ex) { ex.printStackTrace(SoftLog.err); }

        buildGUI(kMain.getTopWindow());
                
        targetResidues = new ArrayList();
        targetResidues.add(targetRes1);
        targetResidues.add(targetRes2);
        modelman.registerTool(this, targetResidues);
        
        // not sure if this is best place to set adjacency map....if 
        // model is fit poorly first then this will fail.
        //confAngles.setAdjacency(targetRes1, targetRes2, modelman.getMoltenState());
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
        TablePane2 centDials0 = new TablePane2();
        TablePane2 centDials1 = new TablePane2();
        TablePane2 rightDials = new TablePane2();
        dials = new AngleDial[angleNames.length];
        for(int i = 0; i < angleNames.length; i++)
        {
          TablePane2 dialPane = centDials1;
          if (angleNames[i].indexOf("-1") > -1) dialPane = centDials0;
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
          double[] bounds;
          if (angleNames[i].indexOf("-1") > -1) bounds = confAngles.getBounds(angleNames[i].substring(0, angleNames[i].indexOf("-1")));
          else bounds = confAngles.getBounds(angleNames[i]);
          for (int j = 0; j < bounds.length; j+=2) {
            dials[i].setBoundsDegrees(bounds[j], bounds[j+1]);
          }
          dials[i].addChangeListener(this);
          dialPane.add(dials[i]);
          dialPane.newRow();
          dialPane.add(new JLabel(angleNames[i]));
          dialPane.newRow();
        }
        topDialPane.add(leftDials);
        topDialPane.add(centDials0);
        topDialPane.add(centDials1);
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
        String[] conftemp = conformer.getDefinedConformerNames();
        String[] conformers = new String[conftemp.length+1];
        System.arraycopy(conftemp, 0, conformers, 1, conftemp.length);
        conformers[0] = "original";
        conformerList = new JList(conformers);
        conformerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conformerList.setSelectionModel(new ReclickListSelectionModel(conformerList));
        conformerList.addListSelectionListener(this);
        
        // Rotamer quality readout
        confQuality = new JLabel();
        confQuality.setToolTipText("Quality assessment for the current suite conformation");
        setFeedback();
        
        TablePane2 conformerPane = new TablePane2();
        conformerPane.vfill(false).add(new JLabel("Conformers"));
        conformerPane.add(new JLabel("Pick superpose atoms"));
        conformerPane.newRow();
        //conformerPane.hfill(true).vfill(true).addCell(new JScrollPane(conformerList), 1, 4);
        conformerPane.vfill(true).hfill(true).addCell(new JScrollPane(conformerList), 1, 4);
        
        Object[] res1Atoms = targetRes1.getAtoms().toArray();
        Object[] res2Atoms = targetRes2.getAtoms().toArray();
        atomsList1 = new JList(res1Atoms);
        atomsList1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //atomsList1.setSelectionModel(new ReclickListSelectionModel(atomsList1));
        atomsList1.addListSelectionListener(this);
        atomsList2 = new JList(res2Atoms);
        atomsList2.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //atomsList2.setSelectionModel(new ReclickListSelectionModel(atomsList2));
        atomsList2.addListSelectionListener(this);
        
        //conformerPane.newRow();
        conformerPane.hfill(true).vfill(true).add(new JScrollPane(atomsList1));
        conformerPane.newRow();
        conformerPane.vfill(false).add(new JLabel("Residue 1"));
        conformerPane.newRow();
        conformerPane.hfill(true).vfill(true).add(new JScrollPane(atomsList2));
        conformerPane.newRow();
        conformerPane.vfill(false).add(new JLabel("Residue 2"));
        conformerPane.newRow().weights(2,0).addCell(confQuality, 2, 1);
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
          //System.out.println("Changing pucker");
            //String choice = makeOptionPane();
            //if((choice == null)||(choice.equals(JOptionPane.UNINITIALIZED_VALUE))) return; // user canceled operation
            
            // Create the mutated sidechain
            //ModelState newState = new ModelState(modelman.getMoltenState());
            //rnaIdealizer.debugModelState(targetResidues, modelman.getFrozenState(), "frozenres-pre.pdb");
            changePuckerState = rnaIdealizer.makeIdealResidue(state, targetResidues, puckers, false);
            //rnaIdealizer.debugModelState(targetResidues, modelman.getFrozenState(), "frozenres-post.pdb");

            //modelman.requestStateRefresh();

         //   double[] angleVals = confAngles.measureAllAngles(targetRes1, targetRes2, changePuckerState, true);
            //rnaIdealizer.debugModelState(targetResidues, changePuckerState, "changepucker.pdb");
            //System.out.print("measured angles of changed pucker: ");
            //for (double d : angleVals) {
            //  System.out.print(df1.format(d) + ":");
            //}
            //System.out.println();
         //   setAllDials(angleVals);
            return changePuckerState;
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
        if(!isUpdating()) {
          //System.out.println("state changed");
            modelman.requestStateRefresh();
        }
        setFeedback(); // otherwise, we evaluate the old conformation
    }
    
    /* Gets called when a new conformer is picked from the list */
    // At first I attempted to be more efficient about changing puckers.
    // The code would only change puckers when the conf list was hit.
    // However this only worked when RnaIdealizer modified the atomstates directly,
    // instead of returning clones.  It turns out that this was changing the frozen
    // state directly in the model manager.  I didn't realize that on requesting a
    // state refresh, the model manager always goes back to the frozen model and 
    // makes its changes from that model.  
    // Changing the frozen model would be alright, except for the fact that cancelling
    // the change wouldn't actually cancel, and then trying to superimpose on the
    // frozen model didn't quite work out either.
    // The fix was to change the puckers once, set the dials, and init the angles
    // for the selected conformer here, and then always change the puckers to the 
    // selected conformer's during requestStateRefresh, so the dial adjustments work
    // of the changed pucker state.
    // Lessons: Never mess with the modelstate in requestStateRefresh, and remember
    // that the modelstate always works from the frozen model!! VBC 100205
    public void valueChanged(ListSelectionEvent ev)
    {
      JList hitList = (JList) ev.getSource();
      if (hitList.equals(conformerList)) {
        String confName = (String)conformerList.getSelectedValue();
        if(confName != null) {
          if (confName.equals("original")) {
            changePuckerState = modelman.getFrozenState();
            double[] angleVals = confAngles.measureAllAngles(targetRes1, targetRes2, changePuckerState, true);
            initSomeAngles(angleVals);
          } else {
            String bin = conformer.getConformerBin(confName);
            try {
              changePuckerState = rnaIdealizer.makeIdealResidue(modelman.getFrozenState(), targetResidues, bin, false);
              double[] angleVals = confAngles.measureAllAngles(targetRes1, targetRes2, changePuckerState, true);
              setAllDials(angleVals);
              double[] meanVals = conformer.getMeanValues(confName);
              for (int i = 0; i < meanVals.length; i++) {
                //System.out.println(Double.isNaN(meanVals[i]));
                if (Double.isNaN(meanVals[i])) {
                  dials[i].setPaintOrigAngle(false);
                  meanVals[i] = angleVals[i];
                } else {
                  dials[i].setPaintOrigAngle(true);
                }
              }
              initSomeAngles(meanVals);
          } catch (AtomException ae) {ae.printStackTrace(SoftLog.err);}
          // else there is no current selection
          }
        }
      } else if (hitList.equals(atomsList1)||hitList.equals(atomsList2)) {
        getSuperposeAtoms();
        //doSuperpose = true;
        stateChanged(new ChangeEvent(this));
        //doSuperpose = false;
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

//{{{ getSuperposeAtoms
public void getSuperposeAtoms() {
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
  } else {
    ArrayList atomNames = new ArrayList();
    atomNames.add(" O3'");
    atomsToSuper.add(atomNames);
    atomNames = new ArrayList();
    atomNames.add(" P  ");
    atomNames.add(" O5'");
    atomsToSuper.add(atomNames);
  }
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
          String[] d = new String[dials.length];
          for(int i = 0; i < dials.length; i++) {
            String val = dials[i].getDegreesWrapped();
            if (val.equals("NaN")) {
              d[i] = "__?__";
            } else {
              d[i] = val;
            }
          }
          String resInf = resInfo1+"__?__:"+d[1]+":"+d[2]+":"+d[3]+":"+d[4]+":"+d[5]+"\n";
          resInf = resInf+resInfo2+d[6]+":"+d[7]+":"+d[8]+":"+d[9]+":__?__:__?__";
          //System.out.println(resInf);
          //System.out.println(findProgram("suitename"));
          Process proc = Runtime.getRuntime().exec(findProgram("suitename"));
          DataOutputStream stream = new DataOutputStream(proc.getOutputStream());
          stream.writeBytes(resInf);
          //System.out.println(resInf.length());
          //System.out.println(stream.size());
          stream.flush();
          stream.close();
          BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String line;
          line = reader.readLine();
          line = reader.readLine();
          String[] split = Strings.explode(line, ':');
          String eval = "Need suitename for score";
          if (split.length == 6) {
            eval = split[5];
          }
          //System.out.println(line);
          //while ((line = reader.readLine()) != null) {
          //  System.out.println(line);
          //}
          
            //double score = conformer.evaluate(targetRes1, targetRes2, modelman.getMoltenState()) * 100.0;
            //double score = 100;
            //String eval;
            //if(score > 20)          eval = "Excellent";
            //else if(score > 10)     eval = "Good";
            //else if(score >  2)     eval = "Fair";
            //else if(score >  1)     eval = "Poor";
            //else                    eval = "OUTLIER";
            //confQuality.setText(eval+" ("+df1.format(score)+"%)");
            confQuality.setText(eval);
        }
        catch(IllegalArgumentException ex)
        {
            confQuality.setText("Download suitename for scoring");
        }
        catch (IOException ie) {
          ie.printStackTrace(SoftLog.err);
        }
    }
//}}}

//{{{ findProgram
//##################################################################################################
    /**
    * Attempts to find the given program name in the same directory as the king.jar file.
    * In this case, the entire path will be quoted to protect any whitespace inside.
    * If not found, it assumes the program is in the PATH.
    * Automatically appends ".exe" if we appear to be running under Windows..
    * This was copied form BgKinRunner, but doesn't appear to work with single quotes on windows.
    * Double quotes around basename seems to work better, at least on vista.
    */
    public String findProgram(String basename)
    {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.indexOf("windows") != -1)
            basename = basename+".exe";
        
        // We search the directory holding the king.jar file
        // for 'probe' or 'probe.exe'; if not found, we just use 'probe'.
        File progFile = new File(kMain.getPrefs().jarFileDirectory, basename);
        if(progFile.exists())
        {
            // Full path might have spaces in it (Win, Mac)
            try { basename = "\""+progFile.getCanonicalPath()+"\""; }
            catch(Throwable t) { t.printStackTrace(SoftLog.err); }
        }
        
        return basename;
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
      String confName = (String)conformerList.getSelectedValue();
      if(confName != null) {
        //if (doChangePucker) { 
        if (!confName.equals("original")) { // if original is picked, just use frozen model
          String bin = conformer.getConformerBin(confName);
          ret = changePuckers(bin, s);
          //doChangePucker = false;
        }
      }

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
        //rnaIdealizer.debugModelState(targetResidues, ret, "pre-setAngles.pdb");
        ret = confAngles.setAllAngles(targetRes1, targetRes2, ret, this.getAllAngles());
        //rnaIdealizer.debugModelState(targetResidues, ret, "post-setAngles.pdb");

        try {
          //if (doSuperpose) {
            Model frozen = modelman.getModel();
            Residue frozRes1 = frozen.getResidue(targetRes1.getCNIT());
            Residue frozRes2 = frozen.getResidue(targetRes2.getCNIT());
            ArrayList frozRezzes = new ArrayList();
            frozRezzes.add(frozRes1);
            frozRezzes.add(frozRes2);
            getSuperposeAtoms();
            //rnaIdealizer.debugModelState(frozRezzes, modelman.getFrozenState(), "frozenres.pdb");
           // rnaIdealizer.debugModelState(targetResidues, ret, "pre-dock.pdb");
            ret = rnaIdealizer.dockResidues(targetResidues, ret, frozRezzes, modelman.getFrozenState(), atomsToSuper);
           // rnaIdealizer.debugModelState(targetResidues, ret, "post-dock.pdb");
          //}
        } catch (AtomException ae) {
          ae.printStackTrace(SoftLog.err);
        }
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

