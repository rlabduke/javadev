// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gnutil.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>ModelManager</code> is a plugin that manages a macromolecular
* model for use by other tools that would like to update it.
* It takes care of drawing the new model to the graphics, controlling
* updates, and writing out the modified portions.
*
* <p>The manager works on a check-in, check-out basis that allows
* only one entity to modify a given set of residues at a time.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 16:12:43 EST 2003
*/
public class ModelManager extends Plugin
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    File                    srcfile     = null;
    ModelGroup              modelGroup  = null;
    Model                   model       = null;
    /** A wrapper object that follows the current conformation */
    ModelState              stateHead   = null;
    /** A cache for a PDB representation of stateHead */
    File                    stateAsPdb  = null;
    
    HashSet                 checkedoutRes;
    HashSet                 modifiedRes;
    
    SuffixFileFilter        pdbFilter, rotFilter;
    JFileChooser            openChooser, saveChooser;
    JDialog                 dialog;
    JLabel                  lblFileName;
    JLabel                  lblNumCheckedOut;
    JLabel                  lblNumModified;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ModelManager(ToolBox tb)
    {
        super(tb);
        
        checkedoutRes       = new HashSet();
        modifiedRes         = new HashSet();
        
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
        
        openChooser = new JFileChooser();
        openChooser.addChoosableFileFilter(pdbFilter);
        openChooser.setFileFilter(pdbFilter);
        // can't set PDB file yet b/c kinemage not loaded

        saveChooser = new JFileChooser();
        //XXX-??? saveChooser.addChoosableFileFilter(rotFilter);
        saveChooser.setFileFilter(rotFilter);
    }
//}}}

//{{{ buildDialog
//##################################################################################################
    private void buildDialog()
    {
        lblFileName         = new JLabel("File not loaded");
        lblNumCheckedOut    = new JLabel();
        lblNumModified      = new JLabel();
        
        TablePane cp = new TablePane();
        cp.insets(6);
        cp.addCell(lblFileName);
        cp.newRow().addCell(cp.strut(0,8)); //spacer
        cp.newRow();
        cp.addCell(lblNumCheckedOut);
        cp.newRow();
        cp.addCell(lblNumModified);
        
        dialog = new JDialog(kMain.getMainWindow(), this.toString(), false); // not modal
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setContentPane(cp);
        
        dialog.setJMenuBar(buildMenus());
        
        updateCheckoutMonitor();
        dialog.pack();
        //dialog.setLocationRelativeTo(kMain.getMainWindow());
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
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Save modified fragment...", null, this, "onSavePDB"));
        item.setMnemonic(KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Save entire PDB file...", null, this, "onSaveFullPDB"));
        item.setMnemonic(KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
        menu.add(item);
        
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menubar.add(menu);
        menu.add(this.getHelpMenuItem());
        
        return menubar;
    }
//}}}

//{{{ onOpenPDB
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenPDB(ActionEvent ev)
    {
        // Make sure we don't have any residues checked out right now
        if(checkedoutRes.size() > 0)
        {
            JOptionPane.showMessageDialog(kMain.getMainWindow(),
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
            String currdir = System.getProperty("user.dir");
            if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
            
            File f = new File(kin.atPdbfile);
            if(f.exists())
            {
                // setSelectedFile() doesn't do this prior to 1.4.1
                openChooser.setCurrentDirectory(f);
                openChooser.setSelectedFile(f);
            }
        }
        
        // Open the new file
        if(JFileChooser.APPROVE_OPTION == openChooser.showOpenDialog(kMain.getMainWindow()))
        {
            try
            {
                File f = openChooser.getSelectedFile();
                if(f != null && f.exists())
                {
                    srcfile             = f;
                    PdbReader pdbReader = new PdbReader();
                    modelGroup          = pdbReader.read(srcfile);
                    model               = modelGroup.getFirstModel();
                    stateHead           = new ModelState(model.getState());
                    
                    modifiedRes.clear();
                    updateCheckoutMonitor();
                    
                    lblFileName.setText("File: "+srcfile.getName());
                    dialog.pack(); // resize to fit
                }
            }
            catch(IOException ex)
            {
                JOptionPane.showMessageDialog(kMain.getMainWindow(),
                    "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace(SoftLog.err);
            }
        }
    }
//}}}

//{{{ askSavePDB, onSave(Full)PDB
//##################################################################################################
    /** Asks the user if s/he wants to keep the changes made to all residues. */
    void askSavePDB()
    {
        if(modifiedRes.size() > 0
        && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getMainWindow(),
        "Save the changes made to this model?",
        "Save model?", JOptionPane.YES_NO_OPTION)) onSavePDB(null);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSavePDB(ActionEvent ev)
    {
        saveChooser.resetChoosableFileFilters();
        saveChooser.setFileFilter(rotFilter);
        
        File f = srcfile;
        String name = f.getName();
        if(name.toLowerCase().endsWith(".pdb")) name = name.substring(0, name.length()-4) + ".rot";
        else                                    name = name + ".rot";
        saveChooser.setSelectedFile(new File(openChooser.getCurrentDirectory(), name));
        
        savePDB(false);
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
        if(modelGroup == null || model == null) return;
        
        // We can save even if there are some residues checked out,
        // but we should warn that those changes won't be saved.
        if( checkedoutRes.size() > 0
            && JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(kMain.getMainWindow(),
            checkedoutRes.size()+" residues are checked out and won't be saved. Save anyway?",
            "Save model?", JOptionPane.YES_NO_OPTION)) return;
        
        if(JFileChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getMainWindow()))
        {
            File f = saveChooser.getSelectedFile();
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getMainWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    PdbWriter pdbWriter = new PdbWriter(f);
                    if(wholeThing)
                    {
                        // Use modified state for modified model,
                        // default (original) for all other models.
                        Set states = new GnuLinkedHashSet(model.getStates());
                        states.remove(model.getState()); // remove default state
                        Set newStates = new GnuLinkedHashSet();
                        newStates.add(stateHead);
                        newStates.addAll(states);
                        Map stateMap = new HashMap();
                        stateMap.put(model, newStates);
                        
                        pdbWriter.writeModelGroup(modelGroup, stateMap);
                        
                        srcfile = f;
                        lblFileName.setText("File: "+srcfile.getName());
                        dialog.pack(); // resize to fit
                    }
                    else
                    {
                        pdbWriter.writeResidues(modifiedRes, stateHead);
                    }
                    pdbWriter.close();
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(kMain.getMainWindow(), "An error occurred while saving the file.", "Sorry!", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
            }
        }
    }
//}}}

//{{{ get{File, Model, State}
//##################################################################################################
    public File getFile()
    { return srcfile; }

    /**
    * Please use ModelManager.getState() instead of Model.getState().
    * This manager maintains an empty state that tracks the
    * most current version of the model as its parent,
    * allowing all tools to be updated without caching problems, etc.
    */
    public Model getModel()
    {
        if(model == null)
        {
            JOptionPane.showMessageDialog(kMain.getMainWindow(),
            "This tool requires an active model\nloaded in the Model Manager plugin.\nPlease load one now.",
            "Please load a model", JOptionPane.INFORMATION_MESSAGE);
            
            onOpenPDB(null);
        }
        
        return model;
    }

    /**
    * Please use ModelManager.getState() instead of Model.getState().
    * This manager maintains an empty state that tracks the
    * most current version of the model as its parent,
    * allowing all tools to be updated without caching problems, etc.
    */
    public ModelState getState()
    { return stateHead; }
//}}}

//{{{ getStateAsPDB
//##################################################################################################
    /**
    * Returns the current state of the current model as a PDB file.
    * The file is transient and will be deleted when the program exits.
    * @throws IllegalStateException if no model is loaded
    * @throws IOException if there's a problem creating the temp file
    */
    public File getStateAsPDB() throws IOException
    {
        if(model == null || stateHead == null)
            throw new IllegalStateException("Missing Model and/or ModelState");
        
        if(stateAsPdb == null)
        {
            stateAsPdb = File.createTempFile("king", ".pdb");
            PdbWriter writer = new PdbWriter(stateAsPdb);
            writer.writeResidues(model.getResidues(), stateHead);
            writer.close();
            stateAsPdb.deleteOnExit();
        }
        return stateAsPdb;
    }
//}}}
    
//{{{ markModified
//##################################################################################################
    /**
    * Marks the given residue as modified.
    * No need to call this if you used updateState() to update the model.
    */
    public void markModified(Residue r)
    {
        // Clear our cached PDB representation
        stateAsPdb = null;
        
        if(!modifiedRes.contains(r))
        {
            modifiedRes.add(r);
            ModelPlotter plotter = getPlotter();
            plotter.plotAminoAcids(modifiedRes, stateHead);
            
            updateCheckoutMonitor();
            kCanvas.repaint();
        }
    }
//}}}

//{{{ updateState
//##################################################################################################
    /**
    * Registers an update to the current state.
    * The updated state s should either have no ancestors in common
    * with the existing model state (i.e., be modifications only)
    * or else be built from the state returned by this.getState().
    * <p>markModified() will be automatically called for all residues
    * that have updated mappings in the new state or its ancestors.
    */
    public void updateState(ModelState s)
    {
        // Clear our cached PDB representation
        stateAsPdb = null;
        
        // recursive update
        updateImpl(s);
        
        // Make stateHead now point to the newest state data!
        stateHead.setParent(s);
        
        // Do what markModified() does
        ModelPlotter plotter = getPlotter();
        plotter.plotAminoAcids(modifiedRes, stateHead);
        updateCheckoutMonitor();
        kCanvas.repaint();
    }
    
    /** The recursive portion of updateState() */
    void updateImpl(ModelState s)
    {
        // Make sure all the residues that have state entries
        // are marked as modified...
        for(Iterator iter = s.getLocalStateMap().keySet().iterator(); iter.hasNext(); )
        {
            modifiedRes.add(((Atom)iter.next()).getResidue());
        }
        
        // Recurse to parent states if present...
        ModelState p = s.getParent();
        if(p == null || p == stateHead) s.setParent(stateHead.getParent());
        else                            updateState(p);
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
    ModelPlotter getPlotter()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return null;
        
        ModelPlotter plotter = (ModelPlotter)kin.metadata.get("king.ModelManager.plotter");
        if(plotter == null)
        {
            plotter                 = new ModelPlotter();
            plotter.mainColor       = KPalette.bluetint;
            plotter.sideColor       = KPalette.sky;
            plotter.hyColor         = KPalette.gray;
            
            KGroup group = plotter.createGroup("New model");
            group.setOwner(kin);
            kin.add(group);
            kin.metadata.put("king.ModelManager.plotter", plotter);
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
        }
        
        return plotter;
    }
//}}}

//{{{ checkout, isCheckedOut, checkin, updateCheckoutMonitor
//##################################################################################################
    /**
    * Marks the specified Residue as 'owned' by the caller until
    * the checkin() method is called. Tools must checkout Residues
    * before modifying them, and it is recommended that they check
    * them out as soon as they begin working with them.
    * This will ensure that this portion of the model is not modified
    * by another process while the tool is working.
    * @throws IllegalStateException if the residue is already checked out
    */
    public void checkout(Residue r)
    {
        if(isCheckedOut(r)) throw new IllegalStateException("Residue "+r+" is already checked out");
        checkedoutRes.add(r);
        updateCheckoutMonitor();
    }
    
    /** Indicates whether the given Residue is currently checked out and thus unavailable */
    public boolean isCheckedOut(Residue r)
    { return checkedoutRes.contains(r); }
    
    /**
    * Releases the residue that was previously checked out.
    * @throws IllegalStateException if the residue wasn't already checked out
    */
    public void checkin(Residue r)
    {
        if(!isCheckedOut(r)) throw new IllegalStateException("Residue "+r+" was not checked out");
        checkedoutRes.remove(r);
        updateCheckoutMonitor();
    }
    
    /** Updates the label that shows how many residues are checked out */
    protected void updateCheckoutMonitor()
    {
        lblNumCheckedOut.setText("Residues checked out: "+checkedoutRes.size());
        lblNumModified.setText("Residues modified: "+modifiedRes.size());
        dialog.pack();
    }
//}}}

//{{{ findResidueByKinemageID
//##################################################################################################
    /**
    * Searches a model to find the residue named in a kinemage point ID.
    * The ID follows the format used by Prekin:
    * {AAAAaTTT C N+  B##.##}
    *  012345678901234567890
    * where A is the PDB atom ID, a is the alternate conformation code,
    * T is the residue type, C is the chain ID, N is the residue number
    * and insertion code (one or more digits) and the last field is the B factor.
    * @throws NoSuchElementException if the residue can't be found
    */
    public static Residue findResidueByKinemageID(Model model, String id)
    {
        try
        {
            char chainID = Character.toUpperCase(id.charAt(9));
            Collection residues = model.getChain(chainID);
            if(residues == null)
                throw new NoSuchElementException("Model "+model+" has no chain '"+chainID+"'");
            
            int resNum = 0;
            char iCode = ' ';
            int endOfNum = id.indexOf(' ',11);
            if(Character.isDigit(id.charAt(endOfNum-1)))
            {
                resNum  = Integer.parseInt(id.substring(11, endOfNum).trim());
                iCode   = ' ';
            }
            else
            {
                resNum  = Integer.parseInt(id.substring(11, endOfNum-1).trim());
                iCode   = id.charAt(endOfNum-1);
            }
            String resName = id.substring(5,8).toUpperCase();
            //System.err.println("chain="+chainID+"; num="+resNum+"; icode="+iCode+"; resName="+resName);
            
            for(Iterator iter = residues.iterator(); iter.hasNext(); )
            {
                Residue res = (Residue)iter.next();
                if(res.getSequenceNumber()  == resNum
                && res.getInsertionCode()   == iCode
                && res.getName().equals(resName)) return res;
            }// for(each residue in chain)
            
            throw new NoSuchElementException("Residue '"+id+"' does not exist");
        }
        catch(NumberFormatException ex)
        { throw new NoSuchElementException("Number misformatted: "+ex.getMessage()); }
        catch(IndexOutOfBoundsException ex)
        { throw new NoSuchElementException("Kinemage id too short: "+ex.getMessage()); }
    }
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
        return new JMenuItem(new ReflectiveAction(this.toString()+"...", null, this, "onShowDialog"));
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowDialog(ActionEvent ev)
    {
        MainWindow mw = kMain.getMainWindow();
        dialog.pack();
        Point loc = mw.getLocation();
        loc.x += mw.getWidth() - dialog.getWidth();
        loc.y += mw.getHeight() - dialog.getHeight();
        dialog.setLocation(loc);
        dialog.setVisible(true);
        if(model == null) onOpenPDB(null);
    }
//}}}

//{{{ getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return "#modelman-plugin"; }
    
    public String toString() { return "Model manager"; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

