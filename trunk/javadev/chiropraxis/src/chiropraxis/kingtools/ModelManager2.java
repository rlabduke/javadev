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
* <code>ModelManager2</code> is a plugin that manages a macromolecular
* model for use by other tools that would like to update it.
* It takes care of drawing the new model to the graphics, controlling
* updates, and writing out the modified portions.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 16:12:43 EST 2003
*/
public class ModelManager2 extends Plugin
{
//{{{ Constants
//}}}

//{{{ CLASS: ModelStatePair
//##################################################################################################
    /** Used for undoing model and (possibly) state changes. */
    static class ModelStatePair
    {
        Model       model;      // Model we're working on
        String      stateLabel; // Corresponds to one of the ModelStates stored in model
        File        asPDB;      // A cache for a PDB representation of said ModelState
        
        public ModelStatePair(Model m, String sLabel)
        {
            model       = m;
            stateLabel  = sLabel;
            asPDB       = null;
        }
        
        public Model getModel()
        { return model; }
        
        public String getAltConf()
        { return stateLabel; }
        
        public ModelState getState()
        { return model.getState(stateLabel); }
        
        public File getPDB()
        {
            if(asPDB == null)
            {
                try {
                    asPDB = File.createTempFile("king", ".pdb");
                    PdbWriter writer = new PdbWriter(asPDB);
                    writer.writeResidues(model.getResidues(), this.getState());
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
    // These are all for last file opened/saved, for comparison when modified version is saved
    File                    srcfile         = null; // only used to display name to user
    CoordinateFile          srccoordfile    = null; // so we can write ALL models, though we just edited one
    Model                   srcmodel        = null; // replace this w/ our new one when we write
    //ModelState              srcstate        = null; // used to see what changed; deprecated
    
    LinkedList              stateList   = null; // Stack<ModelStatePair>, used for undos
    ModelState              moltenState = null;
    File                    moltenPDB   = null;
    
    Collection              registeredTools;
    Map                     moltenRes;          // Map<Tool, Collection<Residue>>
    Set                     allMoltenRes;
    
    BgKinRunner             probePlotter    = null;
    File                    noeFile         = null;
    String                  noeFormat       = "xplor";
    NoePanel                noePanel;
    ExpectedNoePanel        expNoePanel;
    
    SuffixFileFilter        pdbFilter, rotFilter, noeFilter;
    JFileChooser            openChooser, saveChooser, noeChooser;
    FastModelOpen           fastModelOpen;
    JCheckBox               cbUseSegID;
    boolean                 changedSinceSave = false;
    JDialog                 dialog;
    JLabel                  lblFileName;
    JLabel                  lblNumMolten;
    JMenuItem               miUndo;
    JCheckBox               cbShowProbe, cbShowNOEs, cbShowExpNOEs;
    AttentiveTextField      tfProbeCmd;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ModelManager2(ToolBox tb)
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
        rotFilter = new SuffixFileFilter("Rotated-coordinate files");
        rotFilter.addSuffix(".rot");
        noeFilter = new SuffixFileFilter("NOE files");
        noeFilter.addSuffix(".noe");
        noeFilter.addSuffix(".tbl");    // xplor
        noeFilter.addSuffix(".list");   // aria
        noeFilter.addSuffix(".upl");    // dyana
        
        String currdir = System.getProperty("user.dir");

        openChooser = new JFileChooser();
        openChooser.addChoosableFileFilter(pdbFilter);
        openChooser.setFileFilter(pdbFilter);
        if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
        cbUseSegID = new JCheckBox("Use SegID to define chains", false);
        openChooser.setAccessory(cbUseSegID);
        // can't set PDB file yet b/c kinemage not loaded

        saveChooser = new JFileChooser();
        //XXX-??? saveChooser.addChoosableFileFilter(rotFilter);
        saveChooser.setFileFilter(rotFilter);
        if(currdir != null) saveChooser.setCurrentDirectory(new File(currdir));
        
        noeChooser = new JFileChooser();
        noeChooser.addChoosableFileFilter(noeFilter);
        noeChooser.setFileFilter(noeFilter);
        if(currdir != null) noeChooser.setCurrentDirectory(new File(currdir));
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
        // Button is the same as hitting Return on text field; added for JSR
        JButton bnRunProbe = new JButton(">");
        bnRunProbe.addActionListener(new ReflectiveAction("edit-probe-cmd", null, this, "onEditProbeCmd"));
        TablePane2 probePane = new TablePane2();
        probePane.hfill(true).addCell(tfProbeCmd).weights(0,0).addCell(bnRunProbe);
        FoldingBox probeBox = new FoldingBox(cbShowProbe, probePane);
        probeBox.setAutoPack(true);
        probeBox.setIndent(10);

        cbShowNOEs = new JCheckBox("NOEs", false);
        cbShowNOEs.addActionListener(visUpdate);
        noePanel = new NoePanel(kMain, this);
        FoldingBox noeBox = new FoldingBox(cbShowNOEs, noePanel);
        noeBox.setAutoPack(true);
        noeBox.setIndent(10);

        cbShowExpNOEs = new JCheckBox("Expected NOEs", false);
        cbShowExpNOEs.addActionListener(visUpdate);
        expNoePanel = new ExpectedNoePanel(kMain, this);
        FoldingBox expNoeBox = new FoldingBox(cbShowExpNOEs, expNoePanel);
        expNoeBox.setAutoPack(true);
        expNoeBox.setIndent(10);
        
        JCheckBox cbFastOpen = new JCheckBox("Fast model open", false);
        fastModelOpen = new FastModelOpen(this);
        //fastModelOpen.list.setVisibleRowCount(-1);
        //fastModelOpen.list.setLayoutOrientation(JList.VERTICAL_WRAP);
        FoldingBox fbFastOpen = new FoldingBox(cbFastOpen, new JScrollPane(fastModelOpen.list));
        fbFastOpen.setAutoPack(true);
        fbFastOpen.setIndent(10);
        
        TablePane cp = new TablePane();
        cp.insets(2).weights(1,0.1);
        cp.addCell(lblFileName);
        cp.newRow();
        cp.addCell(lblNumMolten);
        cp.newRow();
        cp.addCell(cbFastOpen);
        cp.newRow();
        cp.save().hfill(true).vfill(true).weights(1,1).addCell(fbFastOpen).restore();
        cp.newRow().addCell(cp.strut(0,2)).newRow(); //spacer
        cp.addCell(cbShowProbe);
        cp.newRow();
        cp.save().hfill(true).vfill(true).addCell(probeBox).restore();
        cp.newRow();
        cp.addCell(cbShowNOEs);
        cp.newRow();
        cp.save().hfill(true).vfill(true).addCell(noeBox).restore();
        cp.newRow();
        cp.addCell(cbShowExpNOEs);
        cp.newRow();
        cp.save().hfill(true).vfill(true).addCell(expNoeBox).restore();
        
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
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KingMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Open NOE file...", null, this, "onOpenNOE"));
        item.setMnemonic(KeyEvent.VK_N);
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, UIMenus.MENU_ACCEL_MASK));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Save PDB file...", null, this, "onSaveFullPDB"));
        item.setMnemonic(KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KingMain.MENU_ACCEL_MASK));
        menu.add(item);
        
        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menubar.add(menu);
        item = new JMenuItem(new ReflectiveAction("Choose alt conf...", null, this, "onChooseAltConf"));
        //item.setMnemonic(KeyEvent.VK_O);
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, UIMenus.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Create new alt conf...", null, this, "onCreateAltConf"));
        //item.setMnemonic(KeyEvent.VK_O);
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, UIMenus.MENU_ACCEL_MASK));
        menu.add(item);
        menu.addSeparator();
        miUndo = item = new JMenuItem(new ReflectiveAction("Undo last change", null, this, "onUndo"));
        item.setMnemonic(KeyEvent.VK_U);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KingMain.MENU_ACCEL_MASK));
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
        if(srcfile != null)
        {
            String fileLbl = "File: "+srcfile.getName();
            String alt = getAltConf();
            if(!alt.equals(" ")) fileLbl = fileLbl+" ["+alt+"]";
            lblFileName.setText(fileLbl);
        }
        else lblFileName.setText("File not loaded");
        
        lblNumMolten.setText("Residues checked out: "+allMoltenRes.size());
        miUndo.setEnabled( stateList.size() > 1 );
        
        cbShowNOEs.setEnabled( noeFile != null );
        //cbShowExpNOEs.setEnabled( noeFile != null );
        expNoePanel.refreshGUI();
        
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
            // If NOEs were just turned on/off, turn them on/off in the display, too.
            if(ev.getSource() == cbShowNOEs)
            {
                noePanel.getNoePlotter().setLastGroupOn(cbShowNOEs.isSelected());
                kCanvas.repaint();
            }
            // If expected NOEs were just turned on/off, turn them on/off in the display, too.
            if(ev.getSource() == cbShowExpNOEs)
            {
                expNoePanel.getNoePlotter().setLastGroupOn(cbShowExpNOEs.isSelected());
                kCanvas.repaint();
            }
        }
        
        refreshGUI();
        requestStateRefresh();
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
            File f = new File(kin.atPdbfile);
            if(f.exists())
            {
                // setSelectedFile() doesn't do this prior to 1.4.1
                openChooser.setCurrentDirectory(f);
                openChooser.setSelectedFile(f);
            }
        }
        
        // Open the new file
        String currdir = System.getProperty("user.dir");
        if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
        if(JFileChooser.APPROVE_OPTION == openChooser.showOpenDialog(kMain.getTopWindow()))
        {
            try
            {
                File f = openChooser.getSelectedFile();
                if(f != null && f.exists())
                    openPDB(f);
                System.setProperty("user.dir", f.getAbsolutePath());
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
        srccoordfile            = pdbr.read(srcfile);
        changedSinceSave        = false;
        
        // Let user select model
        Model m;
        Collection models = srccoordfile.getModels();
        if(models.size() == 1)
            m = srccoordfile.getFirstModel();
        else
        {
            Object[] choices = models.toArray();
            m = (Model)JOptionPane.showInputDialog(kMain.getTopWindow(),
                "This file has multiple models. Please choose one:",
                "Choose model", JOptionPane.PLAIN_MESSAGE,
                null, choices, choices[0]);
            if(m == null)
                m = srccoordfile.getFirstModel();
        }
        
        // Let user select alt conf (iff there's more than one)
        String altLabel = askAltConf(m, "This file has alternate conformations. Please choose one:");
        
        // so we know when we go to save the file
        srcmodel = m;
        
        stateList.clear(); // can't undo loading a new file, b/c we overwrite srcfile and friends
        setModelAndState(m, altLabel);
        
        fastModelOpen.updateList(srcfile.getParentFile(), pdbFilter);
        
        refreshGUI();
    }
//}}}

//{{{ askAltConf
//##################################################################################################
    /** Helper method to ask user which alt conf to use **/
    String askAltConf(Model m, String question)
    {
        ArrayList states = new ArrayList(m.getStates().keySet());
        if(states.size() == 1) return (String) states.get(0);
        else
        {
            states.remove(" "); // all letters treat this as parent
            Object[] choices = states.toArray();
            String c = (String)JOptionPane.showInputDialog(kMain.getTopWindow(),
                question,
                "Choose alt. conf.", JOptionPane.PLAIN_MESSAGE,
                null, choices, choices[0]);
            if(c == null)   return (String) states.get(0);
            else            return c;
        }
    }
//}}}

//{{{ onChooseAltConf, onCreateAltConf
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onChooseAltConf(ActionEvent ev)
    {
        Model m = this.getModel();
        if(m == null) return;
        // Let user select alt conf (iff there's more than one)
        String altLabel = askAltConf(m, "This file has alternate conformations. Please choose one:");
        setModelAndState(m, altLabel);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCreateAltConf(ActionEvent ev)
    {
        Model m = this.getModel();
        if(m == null) return;
        // Let user select alt conf (iff there's more than one)
        String oldAlt = askAltConf(m, "Choose a state to base your new alternate conformation on:");
        // Let the user specify a new alt conf ID
        String newAlt = (String) JOptionPane.showInputDialog(kMain.getTopWindow(),
            "What label should be used for the new alternate? (e.g. 'X')",
            "Name alt. conf.", JOptionPane.PLAIN_MESSAGE);
        if(newAlt == null) return;
        
        Model newModel = (Model) m.clone();
        HashMap newStates = new HashMap(newModel.getStates());
        newStates.put(newAlt, new ModelState(m.getState(oldAlt)));
        newModel.setStates(newStates);
        setModelAndState(newModel, newAlt);
        addUserMod("Created alternate conformation '"+newAlt+"'");
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
        
        String currdir = System.getProperty("user.dir");
        if(currdir != null) saveChooser.setCurrentDirectory(new File(currdir));
        
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
        // Older code keeps changing directory out from under us!!
        //saveChooser.setSelectedFile(new File(openChooser.getCurrentDirectory(), name));
        saveChooser.setSelectedFile(new File(name));
        
        savePDB(true);
        
        System.setProperty("user.dir", f.getAbsolutePath());
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
                        Model m = this.getModel();
                        
                        // It's too hard to scan the model and decide what's changed,
                        // so now we just rely on tools calling addUserMod()
                        //for(Iterator iter = moves.iterator(); iter.hasNext(); )
                        //    srccoordfile.addHeader(CoordinateFile.SECTION_USER_MOD, iter.next().toString());
                        
                        // Sync up the atom-by-atom labels with the global scheme
                        //adjustAltConfLabels(m);
                        
                        // Replace the model we started from with the current model
                        srccoordfile.replace(srcmodel, m);
                        pdbWriter.writeCoordinateFile(srccoordfile);
                        
                        srcfile             = f;
                        srcmodel            = m;
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

//{{{ repairAltConfs
//##################################################################################################
    /**
    * Given a Model, we traverse its ModelStates, looking for AtomStates 
    * that need to be relabeled or removed due to a conformational split 
    * in one of the ModelStates. -DAK
    */
    void repairAltConfs(Model m)
    {
        // Prep atoms
        ArrayList<Atom> atoms = new ArrayList<Atom>();
        for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
        {
            Residue r = (Residue) ri.next();
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom a = (Atom) ai.next();
                atoms.add(a);
            }
        }
        
        repairAltConfs1(m, atoms);  //debugAltConfs("AFTER 1");
        repairAltConfs2(m, atoms);  //debugAltConfs("AFTER 2");
        repairAltConfs3(m, atoms);  //debugAltConfs("AFTER 3");
        repairAltConfs4(m, atoms);  //debugAltConfs("AFTER 4");
    }
//}}}

//{{{ repairAltConfs1
//##################################################################################################
    /**
    * Relabels new ' ' alts to non-' ' (1st half of new split)
    * and old ' ' alts to *different* non-' ' (2nd half of new split).
    */
    void repairAltConfs1(Model m, ArrayList<Atom> atoms)
    {
        CheapSet allAS = new CheapSet(new IdentityHashFunction());
        for(Iterator msi = m.getStates().keySet().iterator(); msi.hasNext(); )
        {
            String altLabel = (String) msi.next();
            ModelState state = (ModelState) m.getStates().get(altLabel);
            for(Atom a : atoms)
            {
                try
                {
                    AtomState as = state.get(a);
                    if(allAS.add(as))
                    {
                        // If this is alt ' ' but we haven't seen it before, it's been
                        // split off from ' ' and needs a more appropriate alt ID
                        // (Wait until here to check whether altLabel is ' '
                        // so we make sure to add ' ' AtomStates to the set)
                        if(!altLabel.equals(" ") && as.getAltConf().equals(" "))
                        {
                            as.setAltConf(altLabel);
                            
                            // Also relabel the other alt conf states for this atom
                            for(Iterator msi2 = m.getStates().keySet().iterator(); msi2.hasNext(); )
                            {
                                String altLabel2 = (String) msi2.next();
                                if(altLabel2.equals(altLabel) || altLabel2.equals(" ")) continue;
                                ModelState state2 = (ModelState) m.getStates().get(altLabel2);
                                AtomState as2 = state2.get(a);
                                if(as2.getAltConf().equals(" ")) as2.setAltConf(altLabel2);
                            }
                        }
                    }
                }
                catch(AtomException ex) {}
            }
        }
    }
//}}}

//{{{ repairAltConfs2
//##################################################################################################
    /**
    * Removes old ' ' non-alts in places where new alts exist due to a split.
    */
    void repairAltConfs2(Model m, ArrayList<Atom> atoms)
    {
        ArrayList atomsToDrop = new ArrayList();
        for(Iterator msi = m.getStates().keySet().iterator(); msi.hasNext(); )
        {
            String altLabel = (String) msi.next();
            if(altLabel.equals(" ")) continue; // looking for alt confs in this loop
            ModelState state = (ModelState) m.getStates().get(altLabel);
            for(Atom a : atoms)
            {
                try
                {
                    AtomState as = state.get(a);
                    // If this atom has alt confs (at least after the updates 
                    // we just did), it shouldn't be in state ' ' anymore
                    if(!as.getAltConf().equals(" ")) atomsToDrop.add(a);
                }
                catch(AtomException ex) {}
            }
        }
        
        // Make new state ' '
        ModelState state_    = (ModelState) m.getStates().get(" ");
        ModelState state_new = new ModelState();
        for(Atom a : atoms)
        {
            if(!atomsToDrop.contains(a))
            {
                try
                {
                    AtomState as = state_.get(a);
                    state_new.add(as);
                }
                catch(AtomException ex) {}
            }
        }
        makeChangedModel(" ", state_new);
    }
//}}}

//{{{ repairAltConfs3
//##################################################################################################
    /**
    * Replaces old, stranded versions of non-' ' alts with new, moved versions
    * and relabels new, divergent alts.
    */
    void repairAltConfs3(Model m, ArrayList<Atom> atoms)
    {
        ModelState stateRemod = (ModelState) m.getStates().get(getAltConf());
        ModelState stateNew = new ModelState(); // alt conf state to be recreated
        String  altLabelNew = null;
        for(Iterator msi = m.getStates().keySet().iterator(); msi.hasNext(); )
        {
            String altLabel = (String) msi.next();
            if(altLabel.equals(" ") || altLabel.equals(getAltConf())) continue;
            ModelState stateOther = (ModelState) m.getStates().get(altLabel);
            for(Atom a : atoms)
            {
                try
                {
                    AtomState asRemod = stateRemod.get(a);
                    AtomState asOther = stateOther.get(a);
                    // Do these have the same alt ID but different coordinates?
                    // If so, something has to change!
                    if(asOther.getAltConf().equals(asRemod.getAltConf()) && asOther != asRemod)
                    {
                        if(asRemod.getAltConf().equals(getAltConf()))
                        {
                            // Remod state has proper alt ID;
                            // other state should be "replaced" with it
                            // (This is the case when e.g. 'A' is moved and 'C' has no 
                            // state defined at this point so it refers to the old 'A'
                            // even after 'A' has diverged to a new conformation)
                            altLabelNew = altLabel; // state that'll get updated
                            stateNew.add(asRemod);
                        }
                        else
                        {
                            // Remod state has improper alt ID because it has branched off;
                            // needs to get relabeled with proper alt ID
                            // (This is the case when e.g. 'C' is moved and has no state 
                            // defined at this point, so a child/copy of 'A' actually gets moved,
                            // which should then become a genuine 'C' conformation)
                            asRemod.setAltConf(getAltConf());
                            // ARE PROBLEMS HERE BECAUSE OF MY CONFUSION ON OBJECT REFERENCES?
                        }
                    }
                }
                catch(AtomException ex) {}
            }
        }
        
        if(altLabelNew != null)
        {
            // Fill in the rest of the new non-' ' state,
            // but don't overwrite the corrections we've just made
            ModelState state = (ModelState) m.getStates().get(altLabelNew);
            for(Atom a : atoms)
            {
                try
                {
                    AtomState as = state.get(a);
                    stateNew.add(as);
                }
                catch(AtomException ex) {} // maybe an atom state we reassigned above!
            }
            makeChangedModel(altLabelNew, stateNew);
        }
    }
//}}}

//{{{ repairAltConfs4
//##################################################################################################
    /**
    * Copies atom states and fixes alt labels for non-' ' alts 
    * at 2-way splits with a 3rd, more extended, just-remodeled alt.
    */
    void repairAltConfs4(Model m, ArrayList<Atom> atoms)
    {
        // See if this method is even appropriate for this model
        // (need >= 3 non-' ' alts for it to be helpful)
        Set altLabels = new TreeSet();
        for(Iterator msi = m.getStates().keySet().iterator(); msi.hasNext(); )
        {
            String altLabel = (String) msi.next();
            if(!altLabel.equals(" ")) altLabels.add(altLabel);
        }
        if(altLabels.size() < 3) return;
        
        HashSet<Atom> atomsToClone = new HashSet<Atom>();
        for(Atom a : atoms)
        {
            // Does this atom have >= 3 alts, 
            // >= 2 of which have the same AtomState and 
            // >= 1 of which has a different AtomState from those >= 2?
            // Could happen if e.g. C was remodeled where C originally did not exist 
            // but where A and B originally recoalesced.  A and C may be properly 
            // extended, but B could get "left behind" and still refer to A, 
            // in which case B needs to get relabeled so it's represented as a 
            // unique atom state (even though it has the same coordinates as A
            // for this location).
            CheapSet allAS = new CheapSet(new IdentityHashFunction());
            for(Iterator msi = m.getStates().keySet().iterator(); msi.hasNext(); )
            {
                String altLabel = (String) msi.next();
                if(altLabel.equals(" ")) continue;
                ModelState state = (ModelState) m.getStates().get(altLabel);
                try
                {
                    AtomState as = state.get(a);
                    allAS.add(as);
                }
                catch(AtomException ex) {}
            }
            if(allAS.size() >= 2) atomsToClone.add(a);
        }
        
        // Clone atoms we just decided should now be alts, and label them appropriately
        for(Iterator msi = m.getStates().keySet().iterator(); msi.hasNext(); )
        {
            String altLabel = (String) msi.next();
            if(altLabel.equals(" ") || altLabel.equals(getAltConf())) continue;
            ModelState state = (ModelState) m.getStates().get(altLabel);
            ModelState stateNew = new ModelState();
            for(Atom a : atoms)
            {
                try
                {
                    AtomState as = state.get(a);
                    if(atomsToClone.contains(a))
                    {
                        AtomState asNew = (AtomState) as.clone();
                        asNew.setAltConf(altLabel);
                        stateNew.add(asNew);
                    }
                    else stateNew.add(as);
                }
                catch(AtomException ex) {}
            }
            makeChangedModel(altLabel, stateNew);
        }
    }
//}}}

//{{{ debugAltConfs
//##################################################################################################
    void debugAltConfs(String stage)
    {
        System.err.println(stage+":");
        Atom ca = null;
        for(Iterator rIter = getModel().getResidues().iterator(); rIter.hasNext(); )
        {
            Residue r = (Residue) rIter.next();
            // Specific to 1ejg, for debugging:
            //if(r.getSequenceInteger() == 6) ca = r.getAtom(" CA ");
            //if(r.getSequenceInteger() == 6) ca = r.getAtom(" C  ");
            //if(r.getSequenceInteger() == 7) ca = r.getAtom(" CA ");
            //if(r.getSequenceInteger() == 8) ca = r.getAtom(" CA ");
            if(r.getSequenceInteger() == 8) ca = r.getAtom(" C  ");
        }
        if(ca != null)
        {
            try { System.err.println("_ -> "+getModel().getState(" ").get(ca)); }
            catch(AtomException ex) { System.err.println("_ -> ??????"); }
            try { System.err.println("A -> "+getModel().getState("A").get(ca)); }
            catch(AtomException ex) { System.err.println("A -> ??????"); }
            try { System.err.println("B -> "+getModel().getState("B").get(ca)); }
            catch(AtomException ex) { System.err.println("B -> ??????"); }
            try { System.err.println("C -> "+getModel().getState("C").get(ca)); }
            catch(AtomException ex) { System.err.println("C -> ??????"); }
        }
        System.err.println();
    }
//}}}

//{{{ adjustAltConfLabels [DEPRECATED]
//##################################################################################################
    /**
    * Given a Model, we traverse its ModelStates, looking for AtomStates whose alts don't match.
    * E.g., an AtomState with alt conf 'A' hiding in ModelState 'B'.
    * This is done in a smart way, so that if that AtomState were also in ModelState 'A', it is unaffected.
    */
    void adjustAltConfLabels(Model m)
    {
        CheapSet allAS = new CheapSet(new IdentityHashFunction());
        for(Iterator msi = m.getStates().keySet().iterator(); msi.hasNext(); )
        {
            String altLabel = (String) msi.next();
            ModelState state = (ModelState) m.getStates().get(altLabel);
            for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
            {
                Residue r = (Residue) ri.next();
                for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
                {
                    Atom a = (Atom) ai.next();
                    try
                    {
                        AtomState as = state.get(a);
                        // If we haven't seen this before, set its alt ID
                        if(allAS.add(as)) as.setAltConf(altLabel);
                    }
                    catch(AtomException ex) {}
                }
            }
        }
    }
//}}}

//{{{ addUserMod
//##################################################################################################
    /**
    * It's just too damn hard to figure out all the ways a model might have changed.
    * So now tools are responsible for calling this function AFTER they change something.
    * Right now, it doesn't get undone when we undo, but it might in the future.
    * We could add that capability by storing these in the ModelStatePair class instead,
    * and then insert them just before saving the file.
    */
    public void addUserMod(String msg)
    {
        if(!msg.startsWith("USER  "))
            msg = "USER  MOD "+msg;
        srccoordfile.addHeader(CoordinateFile.SECTION_USER_MOD, msg);
    }
//}}}

//{{{ getModel, getAltConf, getFrozen{State, PDB}
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
    * Returns the identifying label for the alternate conformation currently in use.
    * If no Model is loaded, the user will be prompted to select one.
    * Please DO NOT cache this value as it may change.
    * @throws IllegalStateException if the user refuses to load a model
    */
    public String getAltConf()
    { return getMSP().getAltConf(); }

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

//{{{ requestStateRefresh
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
        if(cbShowNOEs.isSelected())     visualizeNOEs();
        if(cbShowExpNOEs.isSelected())  visualizeExpectedNOEs();
        kCanvas.repaint();
    }
//}}}

//{{{ requestStateChange
//##################################################################################################
    /**
    * Causes the state changes from the named tool to be
    * incorporated into the static ("frozen") state
    * of the model.
    * As a side effect, the tool is unregistered, and the model updated.
    */
    public void requestStateChange(Remodeler tool)
    {
        debugAltConfs("ORIGINAL");
        
        ModelState s = tool.updateModelState( getFrozenState() );
        s = s.createCollapsed(); // reduce lookup time in the future
        
        // Every permanent change generates a new Model, so that it can
        // hold the new set of ModelStates (only one of them has changed).
        // It costs more memory, but not a LOT more, and seems cleaner than other approaches.
        makeChangedModel(this.getAltConf(), s);
        
        //debugAltConfs("AFTER MOVE");
        //adjustAltConfLabels(this.getModel());
        repairAltConfs(this.getModel());
        
        visualizeFrozenModel(); // not done for every refresh
        unregisterTool(tool);
        // -> requestStateRefresh();
        // -> refreshGUI(); // make sure Undo item is up-to-date
    }
//}}}

//{{{ makeChangedModel
//##################################################################################################
    /**
    * Makes a new model, incorporating changes to a single state, and stores it.
    * Somewhat wasteful memory-wise, but I can't figure out a way to update a 
    * model's states without making a totally new model! -DAK
    */
    void makeChangedModel(String altLabel, ModelState s)
    {
        Model newModel = (Model) this.getModel().clone();
        HashMap newStates = new HashMap(newModel.getStates());
        newStates.put(altLabel, s);
        newModel.setStates(newStates);
        ModelStatePair msp = new ModelStatePair(newModel, this.getAltConf());
        stateList.addLast(msp);
        changedSinceSave = true;
    }
//}}}

//{{{ set/replaceModelAndState, isMolten
//##################################################################################################
    /**
    * Used in place of requestStateChange for a tool to (eg) make a mutation,
    * changing both the Model and the ModelState.
    * @throws IllegalStateException if isMolten() == true
    */
    public void replaceModelAndState(Model m, ModelState s)
    {
        if(isMolten())
            throw new IllegalStateException("Cannot install new model while old model is molten");
        
        // Every permanent change generates a new Model, so that it can
        // hold the new set of ModelStates (only one of them has changed).
        // It costs more memory, but not a LOT more, and seems cleaner than other approaches.
        Model newModel = (Model) m.clone();
        HashMap newStates = new HashMap(newModel.getStates());
        newStates.put(this.getAltConf(), s);
        newModel.setStates(newStates);
        
        setModelAndState(newModel, this.getAltConf());
        changedSinceSave = true;
    }
    
    /**
    * Installs a new Model and/or changes which alt conf we're working on.
    * This is the atomic way of changing the frozen state, and can be undone.
    * The visualizations are updated automatically.
    */
    public void setModelAndState(Model m, String altLabel)
    {
        ModelStatePair msp = new ModelStatePair(m, altLabel);
        stateList.addLast(msp);
        
        visualizeFrozenModel(); // not done for every refresh
        requestStateRefresh();
        refreshGUI(); // make sure Undo item is up-to-date
    }
    
    public boolean isMolten()
    { return allMoltenRes.size() > 0; }
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
        registeredTools.remove(tool);
        moltenRes.remove(tool);
        
        allMoltenRes.clear();
        for(Iterator iter = moltenRes.values().iterator(); iter.hasNext(); )
        {
            allMoltenRes.addAll( (Collection)iter.next() );
        }
        refreshGUI();
        
        requestStateRefresh();
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

//{{{ onUndo, undoChange
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
        
        String key = "king.ModelManager2."+name;
        
        ModelPlotter plotter = (ModelPlotter)kin.metadata.get(key);
        if(plotter == null)
        {
            plotter                 = new ModelPlotter();
            plotter.mainColor       = mcColor;
            plotter.sideColor       = scColor;
            plotter.hyColor         = KPalette.gray;
            plotter.modelWidth      = width;
            
            KGroup group = plotter.createGroup(name);
            kin.add(group);
            kin.metadata.put(key, plotter);
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
                +"within {bbradius} of {bbcenter} not water not({molten})),file2' 'file2' '{pdbfile}' -";
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

//{{{ onOpenNOE, visualizeNOEs, visualizeExpectedNOEs
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenNOE(ActionEvent ev)
    {
        // Open the new file
        String currdir = System.getProperty("user.dir");
        if(currdir != null) noeChooser.setCurrentDirectory(new File(currdir));
        if(JFileChooser.APPROVE_OPTION == noeChooser.showOpenDialog(kMain.getTopWindow()))
        {
            File f = noeChooser.getSelectedFile();
            if(f != null && f.exists())
            {
                Object[] choices = {"xplor", "dyana", "aria"};
                String defaultChoice = "xplor";
                String filename = f.getName().toLowerCase();
                if(filename.endsWith(".upl")) defaultChoice = "dyana";
                else if(filename.endsWith(".list")) defaultChoice = "aria";
                
                String choice = (String)JOptionPane.showInputDialog(kMain.getTopWindow(),
                    "What format are these NOEs in?",
                    "Choose format", JOptionPane.PLAIN_MESSAGE,
                    null, choices, defaultChoice);
                if(choice != null)
                    noeFormat = choice;
                noeFile = f;
                refreshGUI();
                System.setProperty("user.dir", f.getAbsolutePath());
            }
        }
    }
    
    void visualizeNOEs()
    {
        noePanel.visualizeNOEs(allMoltenRes, noeFile, noeFormat);
    }
    
    void visualizeExpectedNOEs()
    {
        expNoePanel.visualizeNOEs(allMoltenRes, noeFile, noeFormat);
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
    }
//}}}

//{{{ getHelpAnchor, toString, isAppletSafe
//##################################################################################################
    public String getHelpAnchor()
    { return "#modelman-plugin"; }
    
    public String toString() { return "Model manager"; }

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

