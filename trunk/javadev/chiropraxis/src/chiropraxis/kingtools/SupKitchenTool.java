// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
import king.*;
import king.core.*;
import king.points.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.util.*;
import chiropraxis.mc.SupKitchen;
//}}}
/**
* <code>SupKitchenTool</code> is a graphical front-end for mc.SupKitchen.
*
* NEEDS GRAPHICAL VERSION OF RMSD STRAIGHT CUTOFF VS. LESK SIEVE!!!
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu May 14 2009
*/
public class SupKitchenTool extends BasicTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    
    TablePane2        pane;
    JCheckBox         cbVerbose; // for debugging
    JTextField        tfRefFile;
    JRadioButton      rbMdlDir;
    JTextField        tfMdlDir;
    JRadioButton      rbMdlFile; // e.g. NMR
    JTextField        tfMdlFile; // e.g. NMR
    JTextField        tfMaxEnsemSize;
    
    JCheckBox         cbSplitChains;
    JRadioButton      rbBbAtmsCa;
    JRadioButton      rbBbAtmsAllHvy;
    JRadioButton      rbBbAtmsAllHvyCb;
    JTextField        tfLesk;
    JCheckBox         cbKinEnsem;
    
    JTextField        tfPcChoice;
    JTextField        tfPcScale;
    JRadioButton      rbDistort;
    JRadioButton      rbRigidXform;
    JCheckBox         cbKinPca;
    
    /** Does all the under-the-hood work of superposition & PCA. */
    SupKitchen        kitchen;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SupKitchenTool(ToolBox tb)
    {
        super(tb);
        
        kitchen = new SupKitchen();
        
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        // INPUT
        JLabel labInputHeader = new JLabel("***  INPUT  ***");
        
        cbVerbose = new JCheckBox("Spit extra messages to stderr (for debugging)", false);
        
        rbMdlDir  = new JRadioButton("Models directory: ", true );
        rbMdlFile = new JRadioButton("Models NMR file: " , false);
        JLabel labRef = new JLabel("       Reference file (opt'l):  ");
        ButtonGroup btnGrpMdlSrc = new ButtonGroup();
        btnGrpMdlSrc.add(rbMdlDir);
        btnGrpMdlSrc.add(rbMdlFile);
        
        tfMdlDir  = new JTextField("none selected");
        tfMdlFile = new JTextField("none selected");
        tfRefFile = new JTextField("none selected");
        tfMdlDir.setEditable(false);
        tfMdlFile.setEditable(false);
        tfRefFile.setEditable(false);
        
        JButton btnSetMdlDir  = new JButton(new ReflectiveAction(
            "Choose...", null, this, "onSetMdlDir"));
        JButton btnSetMdlFile = new JButton(new ReflectiveAction(
            "Choose...", null, this, "onSetMdlFile"));
        JButton btnSetRefFile = new JButton(new ReflectiveAction(
            "Choose...", null, this, "onSetRefFile"));
        btnSetMdlDir.setEnabled(true);
        btnSetMdlFile.setEnabled(true);
        btnSetRefFile.setEnabled(true);
        
        JLabel labMaxEnsemSize1 = new JLabel("Max ensem size: ");
        tfMaxEnsemSize = new JTextField(""+kitchen.getMaxEnsemSize());
        JLabel labMaxEnsemSize2 = new JLabel("                ");
        
        JLabel labBreak1 = new JLabel("======================================");
        
        // ENSEMBLE
        JLabel labSupHeader = new JLabel("***  ENSEMBLE SUPERPOSITION  ***");
        
        cbSplitChains = new JCheckBox("Split input into chains", true);
        
        JLabel labBbAtms = new JLabel("Atoms for superposition:");
        rbBbAtmsCa       = new JRadioButton("Calphas only"               , false);
        rbBbAtmsAllHvy   = new JRadioButton("All bb heavy atoms"         , true);
        rbBbAtmsAllHvyCb = new JRadioButton("All bb heavy atoms + Cbetas", false);
        ButtonGroup btnGrpBbAtms = new ButtonGroup();
        btnGrpBbAtms.add(rbBbAtmsCa);
        btnGrpBbAtms.add(rbBbAtmsAllHvy);
        btnGrpBbAtms.add(rbBbAtmsAllHvyCb);
        
        JLabel labLesk1 = new JLabel("Trim w/ Lesk sieve to max RMSD:");
        tfLesk = new JTextField(""+kitchen.getRmsdLesk());
        tfLesk.setEditable(true);
        JLabel labLesk2 = new JLabel("(-1 = no trimming)");
        
        JButton btnSup = new JButton(new ReflectiveAction(
            "Superpose! Yay!!", null, this, "onSup"));
        cbKinEnsem  = new JCheckBox("kin", true);
        
        JButton btnSaveEnsemPdb = new JButton(new ReflectiveAction(
            "Save ensemble PDB", null, this, "onSaveEnsemPdb"));
        
        JLabel labBreak2 = new JLabel("======================================");
        
        // PRINCIPAL COMPONENT ANALYSIS
        JLabel labPcaHeader1 = new JLabel("***  PRINCIPAL COMPONENT ANALYSIS  ***");
        
        JLabel labPcaHeader2 = new JLabel("How does your ensemble \"like to move\"?");
        
        JLabel labPcChoice = new JLabel("Weighted avg of PCs: ");
        tfPcChoice = new JTextField("1-3");
        tfPcChoice.setEditable(true);
        
        JLabel labPcScale = new JLabel(" x");
        tfPcScale = new JTextField("1.0");
        tfPcScale.setEditable(true);
        
        rbDistort    = new JRadioButton("Allow PCs to distort bb geom"    , true );
        rbRigidXform = new JRadioButton("Rigid xform onto PC-distorted bb", false);
        ButtonGroup btnGrpDistortOrRigidXform = new ButtonGroup();
        btnGrpDistortOrRigidXform.add(rbDistort);
        btnGrpDistortOrRigidXform.add(rbRigidXform);
        
        JButton btnPca = new JButton(new ReflectiveAction(
            "Do PCA! Yay!!", null, this, "onPca"));
        cbKinPca  = new JCheckBox("kin"    , true);
        
        JButton btnSavePcaPdb = new JButton(new ReflectiveAction(
            "Save PCA-transformed PDB", null, this, "onSavePcaPdb"));
        
        // --------------------------------------------------------------------
        
        // INPUT
        pane = new TablePane2().top().weights(10, 1);
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // header
            pane.add(labInputHeader);
        pane.endSubtable();
        pane.newRow();
        pane.add(cbVerbose);
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // input: models
            pane.add(rbMdlDir);
            pane.add(tfMdlDir);
            pane.add(btnSetMdlDir);
            pane.newRow();
            pane.add(rbMdlFile);
            pane.add(tfMdlFile);
            pane.add(btnSetMdlFile);
        pane.endSubtable();
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // input: ref (opt'l)
            pane.add(labRef);
            pane.add(tfRefFile);
            pane.add(btnSetRefFile);
        pane.endSubtable();
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // max ensem size
            pane.add(labMaxEnsemSize1);
            pane.add(tfMaxEnsemSize);
            pane.add(labMaxEnsemSize2);
        pane.endSubtable();
        pane.newRow();
        
        // ENSEMBLE
        pane.add(labBreak1);
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // header
            pane.add(labSupHeader);
        pane.endSubtable();
        pane.newRow();
        pane.add(cbSplitChains);
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // which bb atoms
            pane.add(labBbAtms);
            pane.newRow();
            pane.add(rbBbAtmsCa);
            pane.newRow();
            pane.add(rbBbAtmsAllHvy);
            pane.newRow();
            pane.add(rbBbAtmsAllHvyCb);
        pane.endSubtable();
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // sup output
            pane.add(labLesk1);
            pane.add(tfLesk);
            pane.add(labLesk2);
        pane.endSubtable();
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // sup output
            pane.add(btnSup);
            pane.add(cbKinEnsem);
            pane.newRow();
            pane.add(btnSaveEnsemPdb);
        pane.endSubtable();
        pane.newRow();
        
        // PRINCIPAL COMPONENT ANALYSIS
        pane.add(labBreak2);
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // header
            pane.add(labPcaHeader1);
            pane.newRow();
            pane.add(labPcaHeader2);
        pane.endSubtable();
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // choice of PCs + scaling
            pane.add(labPcChoice);
            pane.add(tfPcChoice);
            pane.add(labPcScale);
            pane.add(tfPcScale);
        pane.endSubtable();
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // distort vs. rigid
            pane.add(rbDistort);
            pane.newRow();
            pane.add(rbRigidXform);
        pane.endSubtable();
        pane.newRow();
        pane.startSubtable(1, 1).hfill(true).memorize(); // PCA output
            pane.add(btnPca);
            pane.add(cbKinPca);
            pane.newRow();
            pane.add(btnSavePcaPdb);
        pane.endSubtable();
        
    }
//}}}

//{{{ onSetMdlDir
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Divvies provided directory into an array of CoordinateFiles. */
    public void onSetMdlDir(ActionEvent ev)
    {
        // Make file chooser.  Will throw an exception if we're running as an applet (?)
        TablePane acc = new TablePane();
        JFileChooser fileChooser = new JFileChooser();
        String currDir = System.getProperty("user.dir");
        if(currDir != null) fileChooser.setCurrentDirectory(new File(currDir));
        fileChooser.setAccessory(acc);
        fileChooser.setDialogTitle("Choose directory of model PDB files");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(kMain.getTopWindow()))
        {
            File mdlDir = fileChooser.getSelectedFile();
            try
            {
                tfMdlDir.setText(mdlDir.getName());
                rbMdlDir.setSelected(true);
                tfMdlFile.setText("none selected");
                rbMdlFile.setSelected(false);
                System.err.println("Set models dir: " + mdlDir.getName());
                kitchen.setMdlFilename(mdlDir.getPath());
            }
            catch(IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Wrong file format was chosen, or file is corrupt:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                System.err.println("*** Error opening models dir:" + mdlDir.getName() + "!");
            }
        }
    }
//}}}

//{{{ onSetMdlFile
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Divvies provided file (usually multi-model, e.g. NMR) into an array
    * containing one CoordinateFile. Alternative to onSetMdlDir(). */
    public void onSetMdlFile(ActionEvent ev)
    {
        // Make file chooser.  Will throw an exception if we're running as an applet (?)
        TablePane acc = new TablePane();
        JFileChooser fileChooser = new JFileChooser();
        String currDir = System.getProperty("user.dir");
        if(currDir != null) fileChooser.setCurrentDirectory(new File(currDir));
        fileChooser.setAccessory(acc);
        fileChooser.setDialogTitle("Choose models PDB file");
        if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(kMain.getTopWindow()))
        {
            File mdlFile = fileChooser.getSelectedFile();
            try
            {
                tfMdlFile.setText(mdlFile.getName());
                rbMdlFile.setSelected(true);
                tfMdlDir.setText("none selected");
                rbMdlDir.setSelected(false);
                System.err.println("Set models file: " + mdlFile.getName());
                kitchen.setMdlFilename(mdlFile.getPath());
            }
            catch(IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Wrong file format was chosen, or file is corrupt:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                System.err.println("*** Error opening models file: " + mdlFile.getName() + "!");
            }
        }
    }
//}}}

//{{{ onSetRefFile
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSetRefFile(ActionEvent ev)
    {
        // Make file chooser.  Will throw an exception if we're running as an applet (?)
        TablePane acc = new TablePane();
        JFileChooser fileChooser = new JFileChooser();
        String currDir = System.getProperty("user.dir");
        if(currDir != null) fileChooser.setCurrentDirectory(new File(currDir));
        fileChooser.setAccessory(acc);
        fileChooser.setDialogTitle("Choose reference PDB file");
        if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(kMain.getTopWindow()))
        {
            File refFile = fileChooser.getSelectedFile();
            try
            {
                tfRefFile.setText(refFile.getName());
                System.err.println("Set ref file: " + refFile.getName());
                kitchen.setRefFilename(refFile.getPath());
            }
            catch(IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Wrong file format was chosen, or file is corrupt:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                System.err.println("*** Error opening ref file: " + refFile.getName() + "!");
            }
        }
    }
//}}}

//{{{ onSup
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSup(ActionEvent ev)
    {
        if(tfMdlDir.getText().equals("none selected") && tfMdlFile.getText().equals("none selected"))
        {
            String error = "No models directory/file specified yet -- can't superpose!";
            JOptionPane.showMessageDialog(null, error, error, JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        kitchen.setVerbose(cbVerbose.isSelected());
        if(rbBbAtmsCa.isSelected())                kitchen.setSuperimpose(kitchen.SELECT_CA);
        else if(rbBbAtmsAllHvy.isSelected())       kitchen.setSuperimpose(kitchen.SELECT_BB_HEAVY);
        else /*if(rbBbAtmsAllHvyCb.isSelected())*/ kitchen.setSuperimpose(kitchen.SELECT_BB_HEAVY_CB);
        try
        {
            double leskSieve = Double.parseDouble(tfLesk.getText());
            kitchen.setRmsdLesk(leskSieve);
        }
        catch(NumberFormatException ex)
        {
            System.err.println("*** Error: Can't parse "+tfLesk.getText()+" as double for Lesk rmsd!");
            System.err.println("*** Simply using default: "+kitchen.getRmsdLesk()+"...");
        }
        try
        {
            int maxEnsemSize = Integer.parseInt(tfMaxEnsemSize.getText());
            kitchen.setMaxEnsemSize(maxEnsemSize);
        }
        catch(NumberFormatException ex)
        {
            System.err.println("*** Error: Can't parse "+tfMaxEnsemSize.getText()+" as integer for max ensem size!");
            System.err.println("*** Simply using default: "+kitchen.getMaxEnsemSize()+"...");
        }
        
        kitchen.makeSup();
        
        if(cbKinEnsem.isSelected())
            visEnsem(kitchen.getEnsemCoordFile(), kitchen.getTitle());
    }
//}}}

//{{{ onPca
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPca(ActionEvent ev)
    {
        if(kitchen.getEnsemCoordFile() == null)
        {
            JOptionPane.showMessageDialog(null, "No ensemble constructed yet -- can't do PCA!", 
                "No ensemble constructed yet -- can't do PCA!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        kitchen.setVerbose(cbVerbose.isSelected());
        if(rbBbAtmsCa.isSelected())                kitchen.setSuperimpose(kitchen.SELECT_CA);
        else if(rbBbAtmsAllHvy.isSelected())       kitchen.setSuperimpose(kitchen.SELECT_BB_HEAVY);
        else /*if(rbBbAtmsAllHvyCb.isSelected())*/ kitchen.setSuperimpose(kitchen.SELECT_BB_HEAVY_CB);
        try
        {
            double scale = Double.parseDouble(tfPcScale.getText());
            kitchen.setScale(scale);
        }
        catch(NumberFormatException ex)
        {
            System.err.println("*** Error: Can't format "+tfPcScale.getText()+" as double for scale!");
            System.err.println("*** Simply using default: "+kitchen.getScale()+"...");
        }
        kitchen.parsePcChoice(tfPcChoice.getText());
        kitchen.setDistort(rbDistort.isSelected());
        
        kitchen.doPca();
        
        if(cbKinPca.isSelected())
            visEnsem(kitchen.getPcaCoordFile(), kitchen.getTitle()+"_PCA");
    }
//}}}

//{{{ onSaveEnsemPdb
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSaveEnsemPdb(ActionEvent ev)
    {
        if(kitchen.getEnsemCoordFile() == null)
        {
            String error = "No ensemble constructed yet -- can't save!";
            JOptionPane.showMessageDialog(null, error, error, JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Make file chooser.  Will throw an exception if we're running as an applet (?)
        TablePane acc = new TablePane();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAccessory(acc);
        fileChooser.setDialogTitle("Save ensemble PDB file");
        String currDir = System.getProperty("user.dir");
        if(currDir != null) fileChooser.setCurrentDirectory(new File(currDir));
        
        SuffixFileFilter fileFilter = new SuffixFileFilter("PDB files (*.pdb)");
        fileFilter.addSuffix(".pdb");
        fileChooser.addChoosableFileFilter(fileFilter);
        fileChooser.setFileFilter(fileFilter);
        
        File suggestedFile = new File(kitchen.getTitle()+".pdb");
        fileChooser.setSelectedFile(suggestedFile);
        
        if(JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(kMain.getTopWindow()))
        {
            try
            {
                File f = fileChooser.getSelectedFile();
                if( !f.exists() ||
                    JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                        "This file exists -- do you want to overwrite it?",
                        "Overwrite file?", JOptionPane.YES_NO_OPTION)
                    == JOptionPane.YES_OPTION )
                {
                    System.err.println("Writing PDB: " + f.getName() + "...");
                    try
                    {
                        PrintStream ps = new PrintStream(f);
                        kitchen.writePdb(ps, kitchen.getEnsemCoordFile());
                        System.err.println("Done writing PDB to " + f.getName());
                    }
                    catch(FileNotFoundException ex)
                    {
                        String error = "Error writing to "+f.getName()+"!";
                        System.err.println(error);
                        JOptionPane.showMessageDialog(null, error, error, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            catch(IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Wrong file format was chosen, or file is corrupt:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                System.err.println("*** Error writing ensemble to PDB!");
            }
        }
    }
//}}}

//{{{ onSavePcaPdb
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSavePcaPdb(ActionEvent ev)
    {
        if(kitchen.getPcaCoordFile() == null)
        {
            String error = "PCA \"ensemble\" not constructed yet -- can't save!";
            JOptionPane.showMessageDialog(null, error, error, JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Make file chooser.  Will throw an exception if we're running as an applet (?)
        TablePane acc = new TablePane();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAccessory(acc);
        fileChooser.setDialogTitle("Save PCA-transformed reference PDB file");
        String currDir = System.getProperty("user.dir");
        if(currDir != null) fileChooser.setCurrentDirectory(new File(currDir));
        
        SuffixFileFilter fileFilter = new SuffixFileFilter("PDB files (*.pdb)");
        fileFilter.addSuffix(".pdb");
        fileChooser.addChoosableFileFilter(fileFilter);
        fileChooser.setFileFilter(fileFilter);
        
        File suggestedFile = new File(kitchen.getTitle()+"_PCA.pdb");
        fileChooser.setSelectedFile(suggestedFile);
        
        if(JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(kMain.getTopWindow()))
        {
            try
            {
                File f = fileChooser.getSelectedFile();
                if( !f.exists() ||
                    JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                        "This file exists -- do you want to overwrite it?",
                        "Overwrite file?", JOptionPane.YES_NO_OPTION)
                    == JOptionPane.YES_OPTION )
                {
                    System.err.println("Writing PDB: " + f.getName() + "...");
                    try
                    {
                        PrintStream ps = new PrintStream(f);
                        kitchen.writePdb(ps, kitchen.getPcaCoordFile());
                        System.err.println("Done writing PDB to " + f.getName());
                    }
                    catch(FileNotFoundException ex)
                    {
                        String error = "Error writing to "+f.getName()+"!";
                        System.err.println(error);
                        JOptionPane.showMessageDialog(null, error, error, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            catch(IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Wrong file format was chosen, or file is corrupt:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                System.err.println("*** Error writing ensemble to PDB!");
            }
        }
    }
//}}}

//{{{ visEnsem
//##############################################################################
    /**
    * Prints either superposed ensemble or PCA "ensemble" to graphics window.
    * We're basically emulating molikin.kingplugin.QuickinPlugin.buildKinemage().
    */
    public void visEnsem(CoordinateFile coordFile, String name)//, boolean animate)
    {
        // Get kin data in form of one of Ian's "StreamTank"s
        StreamTank kinData = new StreamTank();
        PrintWriter out = new PrintWriter(new OutputStreamWriter(kinData));
        kitchen.writeKin(out, coordFile, name);
        kinData.close();
        
        // Write out to a new kinemage in the graphics window.
        Kinemage newKin = new Kinemage(name);
        kMain.getKinIO().loadStream(kinData.getInputStream(), kinData.size(), newKin);
        kMain.getStable().append(Arrays.asList(new Kinemage[] {newKin}));
    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    { super.click(x, y, p, ev); }
    
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    { super.click(x, y, p, ev); }
//}}}

//{{{ getToolPanel, getHelpURL/Anchor, toString
//##############################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return pane; }
    
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        URL     url     = getClass().getResource("/chiropraxis/kingtools/tools-manual.html");
        String  anchor  = getHelpAnchor();
        if(url != null && anchor != null)
        {
            try { url = new URL(url, anchor); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
            return url;
        }
        else return null;
    }
    
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this tool. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#navigate-tool" (or null)
    */
    public String getHelpAnchor()
    { return "#supkitchen-tool"; }
    
    public String toString() { return "Sup Kitchen Tool"; }
//}}}
}//class

