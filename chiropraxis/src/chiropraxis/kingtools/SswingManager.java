// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
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

import java.util.List; // means all refs to "List" are this

import driftwood.data.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>SswingManager</code> is a plugin that manages a macromolecular
* model for use by other tools that would like to update it.
* It takes care of drawing the new model to the graphics, controlling
* updates, and writing out the modified portions.
*
* <p>Copyright (C) 2004 by Shuren Wang. All rights reserved.
* <br>Begun on Thu July 20 16:12:43 EST 2004
*/
public class SswingManager extends Plugin
{
//{{{ Constants
//}}}

//{{{ CLASS: ModelStatePair
//##################################################################################################
    /** Used for undoing model and (possibly) state changes. */
    static class ModelStatePair
    {
        Model       model;
        ModelState  state;
        /** A cache for a PDB representation of state */
        File        asPDB;

        public ModelStatePair(Model m, ModelState s)
        {
            model = m;
            state = s;
            asPDB = null;
        }

        public Model getModel()
        { return model; }

        public ModelState getState()
        { return state; }

        public File getPDB()
        {
            if(asPDB == null)
            {
                try {
                    asPDB = File.createTempFile("king", ".pdb");
                    PdbWriter writer = new PdbWriter(asPDB);
                    writer.writeResidues(model.getResidues(), state);
                    writer.close();
                    asPDB.deleteOnExit();
                } catch(IOException ex)
                { ex.printStackTrace(SoftLog.err); }
            }
            return asPDB;
        }
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    File                    srcfile     = null;
    ModelGroup              srcmodgrp   = null;
    Model                   srcmodel    = null;
    ModelState              srcstate    = null;
    FastModelOpen           fastModelOpen;

    LinkedList              stateList   = null; // Stack<ModelStatePair>
    ModelState              moltenState = null;
    File                    moltenPDB   = null;

    Collection              registeredTools;
    Map                     moltenRes;          // Map<Tool, Collection<Residue>>
    Set                     allMoltenRes;

    BgKinRunner             probePlotter    = null;
    SswingRunner            sswingPlotter   = null;

    SuffixFileFilter        pdbFilter, mapFilter, rotFilter;
    JFileChooser            openChooser, mapChooser, saveChooser;
    JCheckBox               cbUseSegID;
    boolean                 changedSinceSave = false;
    JDialog                 dialog;
    JLabel                  lblFileName;
    JLabel                  lblNumMolten;
    JMenuItem               miUndo;
    JCheckBox               cbShowProbe, cbShowSswing;
    AttentiveTextField      tfProbeCmd, tfSswingCmd;

    String                  pdbFileName="";
    String                  densityFileName="";
    boolean                 sswingRunning= true;


//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public SswingManager(ToolBox tb)
    {
        super(tb);

        stateList       = new LinkedList();
        registeredTools = new ArrayList();
        moltenRes       = new HashMap();
        allMoltenRes    = new HashSet();

        buildFileChoosers();
        buildDialog();
    }
//}}}

//{{{ buildFileChoosers
//##################################################################################################
    /** Constructs the Open and Save file choosers */
    private void buildFileChoosers()
    {
        pdbFilter = new SuffixFileFilter("Protein Data Bank (PDB) files");
        pdbFilter.addSuffix(".pdb");
        mapFilter = new SuffixFileFilter("CCP4 Map files");
        mapFilter.addSuffix(".ccp4");
        rotFilter = new SuffixFileFilter("Rotated-coordinate files");
        rotFilter.addSuffix(".rot");

        String currdir = System.getProperty("user.dir");

        openChooser = new JFileChooser();
        openChooser.addChoosableFileFilter(pdbFilter);
        openChooser.setFileFilter(pdbFilter);
        if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
        cbUseSegID = new JCheckBox("Use SegID to define chains", false);
        openChooser.setAccessory(cbUseSegID);
        // can't set PDB file yet b/c kinemage not loaded

        mapChooser = new JFileChooser();
        mapChooser.addChoosableFileFilter(mapFilter);
        mapChooser.setFileFilter(mapFilter);
        if(currdir != null) mapChooser.setCurrentDirectory(new File(currdir));

        saveChooser = new JFileChooser();
        //XXX-??? saveChooser.addChoosableFileFilter(rotFilter);
        saveChooser.setFileFilter(rotFilter);
    }
//}}}

//{{{ buildDialog
//##################################################################################################
    private void buildDialog()
    {
        lblFileName         = new JLabel();
        lblNumMolten        = new JLabel();
        Action visUpdate = new ReflectiveAction("Update visualizations", null, this, "onUpdateVis");

        cbShowProbe = new JCheckBox("Probe dots", false);
        cbShowProbe.addActionListener(visUpdate);
        tfProbeCmd = new AttentiveTextField("", 25);
        tfProbeCmd.addActionListener(new ReflectiveAction("edit-probe-cmd", null, this, "onEditProbeCmd"));
        FoldingBox probeBox = new FoldingBox(cbShowProbe, tfProbeCmd);
        probeBox.setAutoPack(true);
        probeBox.setIndent(10);

        cbShowSswing = new JCheckBox("Sswing", false);
        cbShowSswing.addActionListener(visUpdate);
        tfSswingCmd = new AttentiveTextField("", 25);
        tfSswingCmd.addActionListener(new ReflectiveAction("edit-sswing-cmd", null, this, "onEditSswingCmd"));
        FoldingBox sswingBox = new FoldingBox(cbShowSswing, tfSswingCmd);
        sswingBox.setAutoPack(true);
        sswingBox.setIndent(10);

        TablePane cp = new TablePane();
        cp.insets(2).weights(1,0.1);
        cp.addCell(lblFileName);
        cp.newRow();
        cp.addCell(lblNumMolten);
        cp.newRow();
        cp.newRow().addCell(cp.strut(0,2)).newRow(); //spacer
        cp.addCell(cbShowProbe);
        cp.newRow();
        cp.save().hfill(true).vfill(true).addCell(probeBox).restore();
        cp.newRow();

        cp.addCell(cbShowSswing);
        cp.newRow();
        cp.addCell(sswingBox);
        cp.newRow();

        dialog = new JDialog(kMain.getTopWindow(), this.toString(), false); // not modal
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setContentPane(cp);
        dialog.setJMenuBar(buildMenus());

        refreshGUI();
    }
//}}}

//{{{ buildMenus
//##################################################################################################
    private JMenuBar buildMenus()
    {
        JMenuBar menubar = new JMenuBar();
        JMenu menu, submenu;
        JMenuItem item;

        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menubar.add(menu);
        item = new JMenuItem(new ReflectiveAction("Open PDB file...", null, this, "onOpenPDB"));
        item.setMnemonic(KeyEvent.VK_O);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, UIMenus.MENU_ACCEL_MASK));
        menubar.add(menu);
        item = new JMenuItem(new ReflectiveAction("Open Map file...", null, this, "onOpenMap"));
        item.setMnemonic(KeyEvent.VK_M);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, UIMenus.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Save PDB file...", null, this, "onSaveFullPDB"));
        item.setMnemonic(KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, UIMenus.MENU_ACCEL_MASK));
        menu.add(item);
        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menubar.add(menu);
        miUndo = item = new JMenuItem(new ReflectiveAction("Undo last change", null, this, "onUndo"));
        item.setMnemonic(KeyEvent.VK_U);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, UIMenus.MENU_ACCEL_MASK));
        menu.add(item);
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menubar.add(menu);
        menu.add(this.getHelpMenuItem());

        return menubar;
    }
//}}}

//{{{ refreshGUI, onUpdateVis
//##################################################################################################
    /** One stop shopping to ensure the GUI reflects the current conditions. */
    protected void refreshGUI()
    {
        if(srcfile != null) lblFileName.setText("File: "+srcfile.getName());
        else                lblFileName.setText("File not loaded");
        lblNumMolten.setText("Residues checked out: "+allMoltenRes.size());
        miUndo.setEnabled( stateList.size() > 1 );

        dialog.pack();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onUpdateVis(ActionEvent ev)
    {

        if(ev != null)
        {
            // If dots were just turned on/off, turn them on/off in the display, too.
            if(ev.getSource() == cbShowProbe)
            {
                this.getProbePlotter().setLastGroupOn(cbShowProbe.isSelected());
                kCanvas.repaint();
            }
            // If sswing
            if(ev.getSource() == cbShowSswing)
            {
//               this.getSswingPlotter().setLastGroupOn(cbShowSswing.isSelected());
//               kCanvas.repaint();
            }
         }

        refreshGUI();
        requestStateRefresh();
    }
//}}}

//{{{ onOpenMap
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenMap(ActionEvent ev)
    {

        // Open the new file
        if(JFileChooser.APPROVE_OPTION == mapChooser.showOpenDialog(kMain.getTopWindow()))
        {
//            try
//            {
                File f = mapChooser.getSelectedFile();
                if(f != null && f.exists()){
                    densityFileName=f.getName();
                }
//            }
//            catch(IOException ex)
//            {
//                JOptionPane.showMessageDialog(kMain.getTopWindow(),
//                    "An I/O error occurred while loading the file:\n"+ex.getMessage(),
//                    "Sorry!", JOptionPane.ERROR_MESSAGE);
//                ex.printStackTrace(SoftLog.err);
//            }
        }
    }
//}}}


//{{{ onOpenPDB
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenPDB(ActionEvent ev)
    {
        // Make sure we don't have any residues checked out right now
        if(isMolten())
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "Can't open a new model while some residues are checked out for modification.",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Ask about saving the old one (if it's been modified)
        askSavePDB();

        // If a @pdbfile was specified, try to pre-select that
        // TODO-XXX: this assumes kin was opened from current dir!
        Kinemage kin = kMain.getKinemage();
        if(kin != null && kin.atPdbfile != null)
        {
            // Done once, at create time
            //String currdir = System.getProperty("user.dir");
            //if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));

            File f = new File(kin.atPdbfile);
            if(f.exists())
            {
                // setSelectedFile() doesn't do this prior to 1.4.1
                openChooser.setCurrentDirectory(f);
                openChooser.setSelectedFile(f);
                // get pdbfile name for sswing command line
                pdbFileName=f.getName();
            }
        }

        // Open the new file
        if(JFileChooser.APPROVE_OPTION == openChooser.showOpenDialog(kMain.getTopWindow()))
        {
            try
            {
                File f = openChooser.getSelectedFile();
                if(f != null && f.exists()){
                    openPDB(f);
                    pdbFileName=f.getName();
                }
            }
            catch(IOException ex)
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace(SoftLog.err);
            }
        }
    }
//}}}

//{{{ openPDB
//##################################################################################################
    protected void openPDB(File f) throws IOException
    {
        srcfile                 = f;
        PdbReader pdbr          = new PdbReader();
        pdbr.setUseSegID(cbUseSegID.isSelected());
        srcmodgrp               = pdbr.read(srcfile);
        changedSinceSave        = false;

        // Let user select model
        Model m;
        ModelState s;
        Collection models = srcmodgrp.getModels();
        if(models.size() == 1)
            m = srcmodgrp.getFirstModel();
        else
        {
            Object[] choices = models.toArray();
            m = (Model)JOptionPane.showInputDialog(kMain.getTopWindow(),
                "This file has multiple models. Please choose one:",
                "Choose model", JOptionPane.PLAIN_MESSAGE,
                null, choices, choices[0]);
            if(m == null)
                m = srcmodgrp.getFirstModel();
        }

        // Let user select alt conf
        Collection states = new ArrayList(m.getStateIDs());
        if(states.size() == 1)
            s = m.getState();
        else
        {
            states.remove(new Character(' ')); // all letters treat this as parent
            Object[] choices = states.toArray();
            Character c = (Character)JOptionPane.showInputDialog(kMain.getTopWindow(),
                "This file has alternate conformations. Please choose one:",
                "Choose alt. conf.", JOptionPane.PLAIN_MESSAGE,
                null, choices, choices[0]);
            if(c == null)
                s = m.getState();
            else
                s = m.getState( c.charValue() );
        }

        // so we know when we go to save the file
        srcmodel = m;
        srcstate = s;

        //stateList.clear(); // can't undo loading a new file (?)
        replaceModelAndState(m, s);

////        fastModelOpen.updateList(srcfile.getParentFile(), pdbFilter);

        refreshGUI();
    }
//}}}

//{{{ askSavePDB, onSaveFullPDB
//##################################################################################################
    /** Asks the user if s/he wants to keep the changes made to all residues. */
    void askSavePDB()
    {
        // We can save even if there are some residues checked out,
        // but we should warn that those changes won't be saved.
        if( isMolten()
        && JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
        "Some changes have not been committed and won't be saved. Save anyway?",
        "Save model?", JOptionPane.YES_NO_OPTION)) return;

        if( changedSinceSave
        && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
        "Save any changes made to the current model?",
        "Save model?", JOptionPane.YES_NO_OPTION)) onSaveFullPDB(null);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSaveFullPDB(ActionEvent ev)
    {
        saveChooser.resetChoosableFileFilters();
        saveChooser.setFileFilter(pdbFilter);

        // Ganked auto-versioning code from KinfileIO
        File f = srcfile;
        String name = f.getName();
        if(name.endsWith(".pdb"))
        {
            if(name.length() > 6 && name.charAt(name.length()-6) == '.')
            {
                String  prefix  = name.substring(0, name.length()-6);
                char    version = name.charAt(name.length()-5);
                if('0' <= version && version < '9')         name = prefix+"."+(++version)+".pdb";
                else if(version == '9')                     name = prefix+".a.pdb";
                else if('a' <= version && version < 'z')    name = prefix+"."+(++version)+".pdb";
                else                                        name = prefix+"."+version+".1.pdb";
            }
            else
            {
                String prefix = name.substring(0, name.length()-4);
                name = prefix+".1.pdb";
            }
        }
        saveChooser.setSelectedFile(new File(openChooser.getCurrentDirectory(), name));

        savePDB(true);
    }
//}}}

//{{{ savePDB
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void savePDB(boolean wholeThing)
    {
        if(JFileChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getTopWindow()))
        {
            File f = saveChooser.getSelectedFile();
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    PdbWriter pdbWriter = new PdbWriter(f);
                    if(wholeThing)
                    {
                        /* Dumb, simple version * /
                        pdbWriter.writeResidues(
                            this.getModel().getResidues(), this.getFrozenState() );

                        srcfile = f;
                        changedSinceSave = false;
                        refreshGUI();
                        /* Dumb, simple version */

                        // Use modified state for modified model,
                        // default (original) for all other models.
                        Set states = new UberSet(this.getModel().getStates());
                        states.remove(srcstate); // remove default state
                        Set newStates = new UberSet();
                        newStates.add(this.getFrozenState());
                        newStates.addAll(states);
                        Map stateMap = new HashMap();
                        stateMap.put(this.getModel(), newStates);

                        // Create a record of what was changed
                        Collection moves = detectMovedResidues(this.getModel(), srcstate, this.getFrozenState());
                        for(Iterator iter = moves.iterator(); iter.hasNext(); )
                            srcmodgrp.addHeader(ModelGroup.SECTION_USER_MOD, iter.next().toString());

                        // this won't do anything unless e.g. we made a mutation
                        srcmodgrp.replace(srcmodel, this.getModel());
                        pdbWriter.writeModelGroup(srcmodgrp, stateMap);

                        srcfile             = f;
                        srcmodel            = this.getModel();
                        changedSinceSave    = false;
                        refreshGUI();
                    }
                    else
                    {
                        throw new IOException("Partial save not supported");
                    }
                    pdbWriter.close();
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(), "An error occurred while saving the file.", "Sorry!", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
            }
        }
    }
//}}}

//{{{ detectMovedResidues
//##################################################################################################
    /**
    * Given a model and two different states, we compute whether any atom in a given residue
    * has different coordinates in the two states or is present in one but not both.
    * We return a Collection of USER MOD header Strings, one per moved residue.
    */
    public static Collection detectMovedResidues(Model m, ModelState s1, ModelState s2)
    {
        ArrayList headers = new ArrayList();
        for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
        {
            Residue r = (Residue) ri.next();
            boolean mcMoved = false, scMoved = false;
            int created = 0;
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom a = (Atom) ai.next();
                if(s1.hasState(a) ^ s2.hasState(a)) created++;
                else
                {
                    try
                    {
                        AtomState a1 = s1.get(a);
                        AtomState a2 = s2.get(a);
                        if(a1.getX() != a2.getX() || a1.getY() != a2.getY() || a1.getZ() != a2.getZ())
                        {
                            if(AminoAcid.isBackbone(a)) mcMoved = true;
                            else                        scMoved = true;
                        }
                    }
                    catch(AtomException ex) {} // neither state defined this Atom
                }
            }
            if(mcMoved || scMoved)
            {
                headers.add("USER  MOD Residue moved: "+r
                    +(mcMoved ? " [backbone]" : "")
                    +(scMoved ? " [sidechain]" : ""));
            }
            if(created > 0) headers.add("USER  MOD "+created+" atom(s) created/destroyed in "+r);
        }
        return headers;
    }
//}}}

//{{{ getModel, getFrozen{State, PDB}
//##################################################################################################
    /**
    * Returns the Model currently in use.
    * If no Model is loaded, the user will be prompted to select one.
    * Please DO NOT cache this value as it may change.
    * @throws IllegalStateException if the user refuses to load a model
    */
    public Model getModel()
    { return getMSP().getModel(); }

    /**
    * Returns the ModelState currently in use -- the last committed change.
    * This state will not reflect "molten" updates that are outstanding.
    * If no Model is loaded, the user will be prompted to select one.
    * Please DO NOT cache this value as it may change.
    * @throws IllegalStateException if the user refuses to load a model
    */
    public ModelState getFrozenState()
    { return getMSP().getState(); }

    /**
    * Returns a PDB of the ModelState currently in use -- the last committed change.
    * This file will not reflect "molten" updates that are outstanding.
    * If no Model is loaded, the user will be prompted to select one.
    * Please DO NOT cache this value as it may change.
    * @throws IllegalStateException if the user refuses to load a model
    */
    public File getFrozenPDB()
    { return getMSP().getPDB(); }

    protected ModelStatePair getMSP()
    {
        if(stateList.size() < 1)
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
            "This tool requires an active model\nloaded in the Model Manager plugin.\nPlease load one now.",
            "Please load a model", JOptionPane.INFORMATION_MESSAGE);
            onOpenPDB(null);
        }
        if(stateList.size() < 1)
            throw new IllegalStateException("Cannot procede without a model loaded!");
        ModelStatePair msp = (ModelStatePair)stateList.getLast();
        return msp;
    }
//}}}

//{{{ getMolten{State, PDB}
//##################################################################################################
    /**
    * Retrieves the current state including molten changes.
    */
    public ModelState getMoltenState()
    {
        if(moltenState == null)
            requestStateRefresh();
        return moltenState;
    }

    /** Returns a FRAGMENT of a PDB file representing molten changes only. */
    public File getMoltenPDB()
    {
        if(moltenPDB == null)
        {
            try {
                moltenPDB = File.createTempFile("king", ".pdb");
                PdbWriter writer = new PdbWriter(moltenPDB);
                writer.writeResidues(allMoltenRes, getMoltenState());
                writer.close();
                moltenPDB.deleteOnExit();
            } catch(IOException ex)
            { ex.printStackTrace(SoftLog.err); }
        }
        return moltenPDB;
    }
//}}}

//{{{ requestStateRefresh, requestStateChange
//##################################################################################################
    /**
    * Causes all currently registered tools to be polled,
    * in the order they were registered, for outstanding ("molten")
    * changes to the model.
    * All active visualizations will subsequently be updated.
    */
    public void requestStateRefresh()
    {
        // Update model state
        moltenPDB = null;
        ModelState frozen = getFrozenState();
        moltenState = frozen;
        for(Iterator iter = registeredTools.iterator(); iter.hasNext(); )
        {
            moltenState = ((Remodeler)iter.next()).updateModelState(moltenState);
        }
        moltenState = moltenState.createCollapsed(frozen);

        // Update visualizations
        visualizeMoltenModel();
        if(cbShowProbe.isSelected())    visualizeProbeDots();
        if(cbShowSswing.isSelected())
        {
              if(densityFileName.length()==0) onOpenMap(null);
              if(sswingRunning){
                   visualizeSswing();
                   sswingRunning=false;
              }
        }

        kCanvas.repaint();

    }

    /**
    * Causes the state changes from the named tool to be
    * incorporated into the static ("frozen") state
    * of the model.
    * As a side effect, the tool is unregistered, and the model updated.
    */
    public void requestStateChange(Remodeler tool)
    {
        ModelState s = tool.updateModelState( getFrozenState() );
        s = s.createCollapsed(); // reduce lookup time in the future
        ModelStatePair msp = new ModelStatePair( getModel(), s );
        stateList.addLast(msp);
        changedSinceSave = true;

        visualizeFrozenModel(); // not done for every refresh
        unregisterTool(tool);
        // -> requestStateRefresh();
        // -> refreshGUI(); // make sure Undo item is up-to-date
    }
//}}}

//{{{ unregisterTool, registerTool
//##################################################################################################
    /**
    * Removes the tool from the list of active tools,
    * without committing any molten changes to the model.
    * The model and visualizations are updated automatically.
    */
    public void unregisterTool(Remodeler tool)
    {
        sswingRunning= false;
        registeredTools.remove(tool);
        moltenRes.remove(tool);

        allMoltenRes.clear();
        for(Iterator iter = moltenRes.values().iterator(); iter.hasNext(); )
        {
            allMoltenRes.addAll( (Collection)iter.next() );
        }
        refreshGUI();

        requestStateRefresh();
        sswingRunning= true;

    }

    /**
    * Adds the tool to the list of active tools,
    * marking it as working on the residues in targetRes.
    * The model and visualizations are updated automatically.
    */
    public void registerTool(Remodeler tool, Collection targetRes)
    {
        registeredTools.add(tool);
        moltenRes.put(tool, targetRes);
        allMoltenRes.addAll(targetRes);
        refreshGUI();
        requestStateRefresh();
    }
//}}}

//{{{ onUndo, undoChange, replaceModelAndState, isMolten
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onUndo(ActionEvent ev)
    {
        if(!undoChange())
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "No changes to undo.", "Sorry!", JOptionPane.ERROR_MESSAGE);
    }

    /**
    * Undoes the last change committed to the model, if possible.
    * The model and visualizations are updated automatically.
    * @return true iff the last change could be undone.
    */
    public boolean undoChange()
    {
        if(stateList.size() < 2) return false;

        stateList.removeLast();
        visualizeFrozenModel(); // not done for every refresh
        requestStateRefresh();
        refreshGUI(); // make sure Undo item is up-to-date
        return true;
    }

    /**
    * Installs a new Model and ModelState.
    * This change can be undone, provided that you had
    * the sense to copy the model object and change THAT
    * instead of the original.
    * If we detect the new model is NOT a copy, undo will be disabled.
    * The visualizations are updated automatically.
    * @throws IllegalStateException if isMolten() == true
    */
    public void replaceModelAndState(Model m, ModelState s)
    {
        if(isMolten())
            throw new IllegalStateException("Cannot install new model while old model is molten");

        // no undo possible if our current model object has been altered
        if(stateList.size() >= 1 && m == this.getModel())
            stateList.clear();

        ModelStatePair msp = new ModelStatePair(m, s);
        stateList.addLast(msp);

        visualizeFrozenModel(); // not done for every refresh
        requestStateRefresh();
        refreshGUI(); // make sure Undo item is up-to-date
    }

    public boolean isMolten()
    { return allMoltenRes.size() > 0; }
//}}}

//{{{ getPlotter
//##################################################################################################
    /**
    * Returns the plotter associated with the current kinemage.
    * If one does not exist, it is created and its data are inserted
    * into the kinemage.
    *
    * This way, a new group is inserted into every kinemage that's
    * used with this tool, and that group is tracked without
    * being referenced from this object, thereby avoiding memory leaks.
    */
    ModelPlotter getPlotter(String name, KPaint mcColor, KPaint scColor, int width)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return new ModelPlotter(); // dummy object

        String key = "king.SswingManager."+name;

        ModelPlotter plotter = (ModelPlotter)kin.metadata.get(key);
        if(plotter == null)
        {
            plotter                 = new ModelPlotter();
            plotter.mainColor       = mcColor;
            plotter.sideColor       = scColor;
            plotter.hyColor         = KPalette.gray;
            plotter.modelWidth      = width;

            KGroup group = plotter.createGroup(name);
            group.setOwner(kin);
            kin.add(group);
            kin.metadata.put(key, plotter);
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
        }

        return plotter;
    }
//}}}

//{{{ visualize{Molten, Frozen}Model
//##################################################################################################
    void visualizeMoltenModel()
    {
        ModelState      state   = getMoltenState();
        ModelPlotter    plotter = getPlotter("molten model", KPalette.peachtint, KPalette.orange, 5);

        plotter.plotAminoAcids(getModel(), allMoltenRes, state);
    }

    void visualizeFrozenModel()
    {
        Collection      allRes  = getModel().getResidues();
        ModelState      state   = getFrozenState();
        ModelPlotter    plotter = getPlotter("frozen model", KPalette.yellowtint, KPalette.sky, 2);

        plotter.plotAminoAcids(getModel(), allRes, state);
    }
//}}}

//{{{ getProbePlotter, visualizeProbeDots, onEditProbeCmd
//##################################################################################################
    BgKinRunner getProbePlotter()
    {
        // Set up to plot Probe dots
        Kinemage kin = kMain.getKinemage();
        if(kin != null && (probePlotter == null || !probePlotter.getKinemage().equals(kin)))
        {
            if(probePlotter != null) probePlotter.terminate(); // clean up the old one

            // -drop is very important, or else the unselected atoms from file1
            // (waters, the residues we're excluding because they're in file2)
            // will interfere with (obstruct) the dots between file1 and file2 atoms.

            // Incomplete, will be completed in a moment
            String probeCmd = " -quiet -kin -drop -mc -both -stdbonds '(file1 "
                +"not water not({molten})),file2' 'file2' '{pdbfile}' -";
            probePlotter = new BgKinRunner(kMain, kin, probeCmd);
            String probeExe = probePlotter.findProgram("probe");
            probePlotter.setCommand(probeExe+probeCmd); // now complete cmd line
            tfProbeCmd.setText(probePlotter.getCommand());
        }
        return probePlotter;
    }

    void visualizeProbeDots()
    {
        BgKinRunner pp = getProbePlotter();
        pp.requestRun(allMoltenRes, getMoltenState(), getFrozenPDB());
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditProbeCmd(ActionEvent ev)
    {
        /*BgKinRunner pp = getProbePlotter();
        if(pp.editCommand(kMain.getTopWindow()))
            requestStateRefresh();*/
        this.getProbePlotter().setCommand(tfProbeCmd.getText());
        requestStateRefresh();
    }
//}}}


//{{{ getSswingPlotter, visualizeSswing, onEditSswingCmd
//##################################################################################################
    SswingRunner getSswingPlotter()
    {
        // Set up Sswing
        Kinemage kin = kMain.getKinemage();
        if(kin != null && (sswingPlotter == null || !sswingPlotter.getKinemage().equals(kin)))
        {
            if(sswingPlotter != null) sswingPlotter.terminate(); // clean up the old one

            //
            //
            //

            // Incomplete, will be completed in a moment
             for(Iterator iter = allMoltenRes.iterator(); iter.hasNext(); )
             {
               String sswingCmd =pdbFileName;
               sswingPlotter = new SswingRunner(kMain, kin, sswingCmd);
               String sswingExe = sswingPlotter.findProgram("sswing ");
               String sswingOption="-f -s ";
               Residue re = (Residue) iter.next();
               if (re.getChain()!=' ')
                  sswingOption=sswingOption+"-c "+re.getChain();
               sswingPlotter.setCommand(sswingExe+" "+
                                       sswingOption+" "+
                                       sswingCmd+" "+
                                       re.getSequenceNumber()+" "+
                                       re.getName()+" "+densityFileName); // now complete cmd line
               tfSswingCmd.setText(sswingPlotter.getCommand());
             }
       }
            return sswingPlotter;
    }

    void visualizeSswing()
    {

          SswingRunner sswingPlotter = getSswingPlotter();
          for(Iterator iter = allMoltenRes.iterator(); iter.hasNext(); )
          {

              String sswingCmd =pdbFileName;
//            sswingPlotter = new BgKinRunner(kMain, kin, sswingCmd);
              String sswingExe = sswingPlotter.findProgram("sswing ");
              String sswingOption="-f -s ";
              Residue re = (Residue) iter.next();
              if (re.getChain()!=' ')
                 sswingOption=sswingOption+"-c "+re.getChain();
              sswingPlotter.setCommand(sswingExe+" "+
                                       sswingOption+" "+
                                       sswingCmd+" "+
                                       re.getSequenceNumber()+" "+
                                       re.getName()+" "+densityFileName); // now complete cmd line
              tfSswingCmd.setText(sswingPlotter.getCommand());
        }
        System.out.println(sswingPlotter.getCommand()+" ....\n");
        sswingPlotter.setCommand(tfSswingCmd.getText());

        sswingPlotter.requestRun(allMoltenRes, getMoltenState(), getFrozenPDB());
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditSswingCmd(ActionEvent ev)
    {

        this.getSswingPlotter().setCommand(tfSswingCmd.getText());
        requestStateRefresh();
    }
//}}}


//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ getToolsMenuItem, onShowDialog
//##################################################################################################
    /**
    * Creates a new JMenuItem to be displayed in the Tools menu,
    * which will allow the user to access function(s) associated
    * with this Plugin.
    *
    * Only one JMenuItem may be returned, but it could be a JMenu
    * that contained several functionalities under it.
    *
    * The Plugin may return null to indicate that it has no
    * associated menu item.
    */
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onShowDialog"));
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowDialog(ActionEvent ev)
    {
        if(! dialog.isVisible() )
        {
            Container mw = kMain.getContentContainer();
            dialog.pack();
            Point loc = mw.getLocation();
            loc.x += mw.getWidth() - dialog.getWidth();
            loc.y += mw.getHeight() - dialog.getHeight();
            dialog.setLocation(loc);
            dialog.setVisible(true);
        }
        if(stateList == null || stateList.size() < 1) onOpenPDB(null);
//        if(cbShowSswing.isSelected())   onOpenMap(null);

    }
//}}}

//{{{ getHelpAnchor, toString, isAppletSafe
//##################################################################################################
    public String getHelpAnchor()
    { return "#modelman-plugin"; }
    
    public String toString() { return "Sidechain Fitting Manager"; }

    /** This plugin is not applet-safe because it invokes other processes and loads files. */
    static public boolean isAppletSafe()
    {
        return false;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

