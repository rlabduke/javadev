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
import driftwood.gnutil.*;
import driftwood.gui.*;
import driftwood.moldb.*;
import driftwood.r3.*;
import driftwood.util.*;
import chiropraxis.backrub.*;
import chiropraxis.rotarama.*;
import chiropraxis.sc.*;
//}}}
/**
* <code>BackrubTool</code> is a GUI for running Backrub interactively
* on kinemage+PDB models in KiNG.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 16:12:43 EST 2003
*/
public class BackrubTool extends BasicTool implements ChangeListener
{
//{{{ Constants
    static final DecimalFormat df1 = new DecimalFormat("0.0");
    static final DecimalFormat df0 = new DecimalFormat("0");
    
    static final Color normalColor  = new Color(0f, 0f, 0f);
    static final Color alertColor   = new Color(0.6f, 0f, 0f);
//}}}

//{{{ Variable definitions
//##################################################################################################
    File                    srcfile = null;
    PDBFile                 pdbfile = null;
    Residue                 residue = null;
    Backrub                 backrub = null;
    Backrub.Constraints     brc;
    BackrubPlotter          mobilePlotter   = null;
    BackrubPlotter          modPlotter      = null;
    Ramachandran            rama;
    SidechainAngles         scAngles;
    SidechainTwister        twister = null;
    GnuLinkedHashMap        modifiedResidues;   // Map<Residue, AminoAcid>
    Kinemage                probeDots;

    AngleDial               slMajor, slMinor1, slMinor2; // Dials for major and minor rotations
    JLabel                  lblResID, lblTauDev, lblRamaOK;
    JLabel                  lblTau1, lblTau2, lblTau3;
    JLabel                  lblRama1, lblRama2, lblRama3;
    JCheckBox               cbShowProbeDots, cbMoveSidechain;
    JCheckBoxMenuItem       cbmiShowDotsToWater;
    FoldingBox              boxSidechainDials;
    JFileChooser            openChooser, saveChooser;
    
    TablePane               toolpane;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public BackrubTool(ToolBox tb)
    {
        super(tb);
        
        brc                 = new Backrub.Constraints();
        modifiedResidues    = new GnuLinkedHashMap();
        probeDots           = null;
        
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) { rama = null; ex.printStackTrace(SoftLog.err); }
        
        try { scAngles = new SidechainAngles(); }
        catch(Exception ex) { scAngles = null; ex.printStackTrace(SoftLog.err); }
        
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    /** Constructs the primary interface for this tool */
    private void buildGUI()
    {
        SuffixFileFilter pdbFilter = new SuffixFileFilter("Protein Data Bank (PDB) files");
        pdbFilter.addSuffix(".pdb");
        SuffixFileFilter rotFilter = new SuffixFileFilter("Rotated-coordinate files");
        rotFilter.addSuffix(".rot");
        
        openChooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
        // TODO: get @pdbfile from kinemage and set it here if it exists
        openChooser.addChoosableFileFilter(pdbFilter);
        openChooser.setFileFilter(pdbFilter);
        
        saveChooser = new JFileChooser();
        saveChooser.addChoosableFileFilter(rotFilter);
        saveChooser.setFileFilter(rotFilter);

        lblResID        = new JLabel();
        lblResID.setHorizontalAlignment(JLabel.CENTER);
        lblTauDev       = new JLabel();
        lblTau1         = new JLabel();
        lblTau2         = new JLabel();
        lblTau3         = new JLabel();
        lblRamaOK       = new JLabel();
        lblRama1        = new JLabel();
        lblRama2        = new JLabel();
        lblRama3        = new JLabel();
        
        lblTauDev.setToolTipText("Worst deviation of tau from ideal");
        lblTau1.setToolTipText("Deviation of tau(i-1) from ideal");
        lblTau2.setToolTipText("Deviation of tau(i) from ideal");
        lblTau3.setToolTipText("Deviation of tau(i+1) from ideal");
        lblRama1.setToolTipText("(phi,psi) for i-1 residue");
        lblRama2.setToolTipText("(phi,psi) for i residue");
        lblRama3.setToolTipText("(phi,psi) for i+1 residue");
        
        cbShowProbeDots = new JCheckBox(new ReflectiveAction("Show probe dots", null, this, "onShowProbeDots"));
        cbMoveSidechain = new JCheckBox("Move central sidechain");
        boxSidechainDials = new FoldingBox(cbMoveSidechain, null);
        boxSidechainDials.setAutoPack(true);
        
        slMajor     = new AngleDial();
        slMinor1    = new AngleDial();
        slMinor2    = new AngleDial();
        
        slMajor.addChangeListener(this);
        slMinor1.addChangeListener(this);
        slMinor2.addChangeListener(this);
        
        setResidue(null); // updates labels, etc. before assembling

        toolpane = new TablePane();
        toolpane.center().insets(1,1,1,1).hfill(true);
        toolpane.add(toolpane.strut(0,4));
        toolpane.newRow();
        toolpane.add(lblResID, 3, 1);
        toolpane.newRow();
        toolpane.add(toolpane.strut(0,4));
        toolpane.newRow();
        toolpane.startSubtable(3, 1);
            toolpane.center();
            toolpane.add(lblTauDev);
            toolpane.add(lblRamaOK);
        toolpane.endSubtable();
        toolpane.hfill(false);
        toolpane.newRow();
        toolpane.add(lblTau1);
        toolpane.add(lblTau2);
        toolpane.add(lblTau3);
        toolpane.newRow();
        toolpane.add(lblRama1);
        toolpane.add(lblRama2);
        toolpane.add(lblRama3);
        toolpane.newRow();
        toolpane.add(toolpane.strut(0,8));
        toolpane.newRow();
        toolpane.add(new JLabel("N peptide"));
        toolpane.add(new JLabel("C-alpha"));
        toolpane.add(new JLabel("C peptide"));
        toolpane.newRow();
        toolpane.add(slMinor1);
        toolpane.add(slMajor);
        toolpane.add(slMinor2);
        toolpane.newRow();
        toolpane.add(cbShowProbeDots, 3, 1);
        toolpane.newRow();
        toolpane.add(cbMoveSidechain, 3, 1);
        toolpane.newRow();
        toolpane.save().hfill(true).vfill(true).addCell(boxSidechainDials, 3, 1).restore();
    }
//}}}

//{{{ initDialog, buildMenus
//##################################################################################################
    protected void initDialog()
    {
        super.initDialog();
        buildMenus();
        dialog.pack();
    }
    
    private void buildMenus()
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
        item = new JMenuItem(new ReflectiveAction("Save PDB file...", null, this, "onSavePDB"));
        item.setMnemonic(KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
        menu.add(item);
        
        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menubar.add(menu);
        cbmiShowDotsToWater = new JCheckBoxMenuItem(new ReflectiveAction("Show dots to water", null, this, "onShowProbeDots"));
        cbmiShowDotsToWater.setMnemonic(KeyEvent.VK_W);
        //cbmiShowDotsToWater.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK));
        menu.add(cbmiShowDotsToWater);
        item = new JMenuItem(new ReflectiveAction("Export dots to kinemage", null, this, "onExportDots"));
        item.setMnemonic(KeyEvent.VK_X);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Minimize distortions (local)", null, this, "onOptimize"));
        item.setMnemonic(KeyEvent.VK_L);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Minimize distortions (full)", null, this, "onOptimizeFull"));
        item.setMnemonic(KeyEvent.VK_F);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Keep changes to residue", null, this, "onKeepChanges"));
        item.setMnemonic(KeyEvent.VK_K);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Deselect current residue", null, this, "onDiscardChanges"));
        item.setMnemonic(KeyEvent.VK_D);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK));
        menu.add(item);
        
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menubar.add(menu);
        menu.add(this.getHelpMenuItem());
        
        dialog.setJMenuBar(menubar);
    }
//}}}

//{{{ start/stop/reset functions
//##################################################################################################
    public void start()
    {
        super.start();
        // trigger chance for user to open PDB file
        onOpenPDB(null);
        // force loading of data tables that will be used later
        try
        {
            Ramachandran.getInstance();
            Rotamer.getInstance();
        }
        catch(IOException ex) {}
        
        // create our display groups and insert them into the kinemage
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            KGroup          group       = new KGroup(kin, "Backrub");
            kin.add(group);
            KSubgroup       subgroup;
            
            mobilePlotter               = new BackrubPlotter();
            mobilePlotter.mainColor     = KPalette.greentint;
            mobilePlotter.sideColor     = KPalette.green;
            mobilePlotter.hyColor       = KPalette.gray;
            mobilePlotter.modelWidth    = 5;
            subgroup = mobilePlotter.createSubgroup("mobile");
            subgroup.setOwner(group);
            group.add(subgroup);
            
            modPlotter                  = new BackrubPlotter();
            modPlotter.mainColor        = KPalette.bluetint;
            modPlotter.sideColor        = KPalette.sky;
            modPlotter.hyColor          = KPalette.gray;
            modPlotter.modelWidth       = 3;
            subgroup = modPlotter.createSubgroup("modified");
            subgroup.setOwner(group);
            group.add(subgroup);
            
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
        }
        else
        {
            JOptionPane.showMessageDialog(kMain.getMainWindow(),
                "You must have a kinemage loaded to use "+this.toString()+".",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            mobilePlotter   = null;
            modPlotter      = null;
        }
    }
    
    public void stop()
    {
        super.stop();
        if(residue != null) askKeepChanges();
        if(modifiedResidues.size() > 0) askSavePDB();
        pdbfile = null;
        setResidue(null);
    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p != null && pdbfile != null && p.getName().startsWith(" ca "))
        {
            try {
                Residue newRes = findResidueByKinemageID(pdbfile, p.getName());
                if(residue != null) askKeepChanges();
                setResidue(newRes);
            } catch(NoSuchElementException ex) {}
        }
    }
//}}}

//{{{ xx_wheel() functions
//##################################################################################################
    /** Override this function for mouse wheel motion */
    public void wheel(int rotation, MouseEvent ev)
    {
        cbShowProbeDots.setSelected(false); // otherwise Probe will choke this cmd
        probeDots = null;
        slMajor.setDegrees(slMajor.getDegrees()-rotation);
    }
    /** Override this function for mouse wheel motion with shift down */
    public void s_wheel(int rotation, MouseEvent ev)
    {
        cbShowProbeDots.setSelected(false); // otherwise Probe will choke this cmd
        probeDots = null;
        slMinor1.setDegrees(slMinor1.getDegrees()-rotation);
    }
    /** Override this function for mouse wheel motion with control down */
    public void c_wheel(int rotation, MouseEvent ev)
    {
        cbShowProbeDots.setSelected(false); // otherwise Probe will choke this cmd
        probeDots = null;
        slMinor2.setDegrees(slMinor2.getDegrees()-rotation);
    }
    /** Override this function for mouse wheel motion with shift AND control down */
    public void sc_wheel(int rotation, MouseEvent ev)
    { super.wheel(rotation, ev); }
//}}}

//{{{ onShowProbeDots, runProbeDots
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowProbeDots(ActionEvent ev)
    {
        if(!cbShowProbeDots.isSelected()) probeDots = null;
        stateChanged(null);
    }
    
    /**
    * @param targetAA       the AminoAcids that will have dots produced
    * @param supportingAA   other AminoAcids that have been changed
    */
    void runProbeDots(Collection targetAA, Collection supportingAA)
    {
        if(srcfile == null || pdbfile == null || backrub == null) return;
        ArrayList atoms = new ArrayList();
        
        Set             targResidues    = new HashSet();
        StringBuffer    targNumbers     = new StringBuffer();
        for(Iterator iter = targetAA.iterator(); iter.hasNext(); )
        {
            AminoAcid aa = (AminoAcid)iter.next();
            if(aa.N     != null) atoms.add(aa.N);
            if(aa.CA    != null) atoms.add(aa.CA);
            if(aa.C     != null) atoms.add(aa.C);
            if(aa.O     != null) atoms.add(aa.O);
            if(aa.H     != null) atoms.add(aa.H);
            if(aa.HA    != null) atoms.add(aa.HA);
            if(aa.HA1   != null) atoms.add(aa.HA1);
            if(aa.HA2   != null) atoms.add(aa.HA2);
            atoms.addAll(aa.sc.values());
            
            targResidues.add(aa.getResidue());
            
            if(targNumbers.length() > 0) targNumbers.append(",");
            targNumbers.append(aa.getResidue().getNumber());
        }
        
        StringBuffer file2Numbers = new StringBuffer(targNumbers.toString());
        for(Iterator iter = supportingAA.iterator(); iter.hasNext(); )
        {
            AminoAcid aa = (AminoAcid)iter.next();
            if(! targResidues.contains(aa.getResidue()))
            {
                if(aa.N     != null) atoms.add(aa.N);
                if(aa.CA    != null) atoms.add(aa.CA);
                if(aa.C     != null) atoms.add(aa.C);
                if(aa.O     != null) atoms.add(aa.O);
                if(aa.H     != null) atoms.add(aa.H);
                if(aa.HA    != null) atoms.add(aa.HA);
                if(aa.HA1   != null) atoms.add(aa.HA1);
                if(aa.HA2   != null) atoms.add(aa.HA2);
                atoms.addAll(aa.sc.values());
                
                if(file2Numbers.length() > 0) file2Numbers.append(",");
                file2Numbers.append(aa.getResidue().getNumber());
            }
        }
        
        // We search the directory holding the king.jar file
        // for 'probe' or 'probe.exe'; if not found, we just use 'probe'.
        File probeExe = new File(kMain.getPrefs().jarFileDirectory, "probe");
        if(!probeExe.exists())
            probeExe = new File(kMain.getPrefs().jarFileDirectory, "probe.exe");
        String probeName = "probe";
        if(probeExe.exists())
        {
            try { probeName = probeExe.getCanonicalPath(); }
            catch(Throwable t) { t.printStackTrace(SoftLog.err); }
        }
        
        String probeCmd = probeName+" -quiet -drop -mc -both -stdbonds '(file1 "
            +(cbmiShowDotsToWater.isSelected() ? "" : "not water ")
            +"not("+file2Numbers+")),file2' 'file2 "+targNumbers+"' "+srcfile.getAbsolutePath()+" -";
        //SoftLog.err.println(probeCmd);
        String[] cmdTokens = Strings.tokenizeCommandLine(probeCmd);
        //for(int i = 0; i < cmdTokens.length; i++)
        //    SoftLog.err.println(cmdTokens[i]);
        
        try
        {
            // Build up the PDB fragment in a memory buffer
            // This decreases latency and may avoid deadlock...
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pdbfile.writeAtoms(atoms, baos);
            // Launch Probe and feed it the PDB file fragment
            Process probe = Runtime.getRuntime().exec(cmdTokens);
            baos.writeTo(probe.getOutputStream());
            // send EOF to let Probe know we're done
            probe.getOutputStream().close();
        
            // Buffer output to decrease latency and chance of deadlock?
            ProcessTank tank = new ProcessTank();
            if(!tank.fillTank(probe, 5000)) // 5 sec timeout
                SoftLog.err.println("*** Abnormal (forced) termination of PROBE process!");
            //SoftLog.err.println("Probe generated "+tank.stdoutSize()+" b on stdout and "+tank.stderrSize()+" b on stderr.");
            
            // Try to interpret what it sends back
            KinfileParser parser = new KinfileParser();
            parser.parse(new LineNumberReader(new InputStreamReader(tank.getStdout())));
            Collection kins = parser.getKinemages();
            if(kins.size() > 0) probeDots = (Kinemage)kins.iterator().next();
            else                probeDots = null;
            
            kCanvas.repaint();
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
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
    public Residue findResidueByKinemageID(PDBFile pdb, String id)
    {
        try
        {
            String resType = id.substring(5,8).trim().toUpperCase();
            int endOfNum = id.indexOf(' ',11);
            String resNum = id.substring(11, endOfNum).trim();
            String segID = id.substring(9,10).toUpperCase(); // chain ID

            Model model = pdb.getFirstModel();
            Segment seg = model.getSegment(segID);
            Residue res = seg.getResidue(resType+resNum);
            return res;
        }
        catch(IndexOutOfBoundsException ex)
        {
            NoSuchElementException ex2 = new NoSuchElementException("{"+id+"} not found");
            // This is Java 1.4+
            //ex2.initCause(ex);
            throw ex2;
        }
    }
//}}}

//{{{ onOpenPDB
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenPDB(ActionEvent ev)
    {
        // If a @pdbfile was specified, try to pre-select that
        Kinemage kin = kMain.getKinemage(); if(kin == null) return;
        File f;
        if(kin.atPdbfile != null)
        {
            f = new File(kin.atPdbfile);
            if(f.exists())
            {
                // setSelectedFile() doesn't do this prior to 1.4.1
                openChooser.setCurrentDirectory(f);
                openChooser.setSelectedFile(f);
            }
        }
        
        // Open the file
        if(JFileChooser.APPROVE_OPTION == openChooser.showOpenDialog(kMain.getMainWindow()))
        {
            try
            {
                f = openChooser.getSelectedFile();
                if(f != null && f.exists())
                {
                    srcfile = f;
                    pdbfile = new PDBFile();
                    pdbfile.read(srcfile);
                    setResidue(null);
                    
                    modifiedResidues.clear();
                    probeDots   = null;
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

//{{{ stateChanged
//##################################################################################################
    // ev may be null!
    public void stateChanged(ChangeEvent ev)
    {
        if(pdbfile == null || residue == null || backrub == null)
        {
            updateTauLabels(null);
            updateRamaLabels(null);
            return;
        }
        
        AminoAcid aa;
        Collection newBB = backrub.makeConformation(slMajor.getDegrees(),
            slMinor1.getDegrees(), 
            slMinor2.getDegrees());
        
        if(twister != null && scAngles != null)
        {
            Iterator iter = newBB.iterator();
            iter.next();
            aa = (AminoAcid)iter.next(); // central residue
            scAngles.setAllAngles(aa, twister.getAllAngles());
        }
        
        updateTauLabels(newBB);
        updateRamaLabels(newBB);
        
        Map newBBmap = new GnuLinkedHashMap();
        for(Iterator iter = newBB.iterator(); iter.hasNext(); )
        {
            aa = (AminoAcid)iter.next();
            newBBmap.put(aa.getResidue(), aa);
        }
        mobilePlotter.plotAminoAcids(newBBmap);
        
        if(cbShowProbeDots.isSelected()
        && !slMajor.getValueIsAdjusting()
        && !slMinor1.getValueIsAdjusting()
        && !slMinor2.getValueIsAdjusting()
        && (twister == null || !twister.getValueIsAdjusting()))
        {
            runProbeDots(newBB, modifiedResidues.values());
        }
        
        kCanvas.repaint();
    }
//}}}

//{{{ updateTauLabels
//##################################################################################################
    void updateTauLabels(Collection aaBackbones)
    {
        if(aaBackbones == null || aaBackbones.size() < 3)
        {
            lblTauDev.setText("Tau dev: "+df1.format(0)+" deg");
            lblTauDev.setForeground(normalColor);

            lblTau1.setText("-");
            lblTau2.setText("-");
            lblTau3.setText("-");
        }
        else
        {
            double worstTau = backrub.getWorstTauDev(aaBackbones);
            lblTauDev.setText("Tau dev: "+df1.format(worstTau)+" deg");
            if(worstTau < 3.0)  lblTauDev.setForeground(normalColor);
            else                lblTauDev.setForeground(alertColor);
            
            double dev;
            String type;
            Iterator iter = aaBackbones.iterator();
            AminoAcid m1   = (AminoAcid)iter.next();
            AminoAcid ctr  = (AminoAcid)iter.next();
            AminoAcid p1   = (AminoAcid)iter.next();
            
            type = m1.getResidue().getType();
            if(type.equals("GLY"))      dev = 112.5 - m1.getTau();
            else if(type.equals("PRO")) dev = 111.8 - m1.getTau();
            else                        dev = 111.2 - m1.getTau();
            lblTau1.setText("tau: "+df1.format(-dev));
            
            type = ctr.getResidue().getType();
            if(type.equals("GLY"))      dev = 112.5 - ctr.getTau();
            else if(type.equals("PRO")) dev = 111.8 - ctr.getTau();
            else                        dev = 111.2 - ctr.getTau();
            lblTau2.setText("tau: "+df1.format(-dev));
            
            type = p1.getResidue().getType();
            if(type.equals("GLY"))      dev = 112.5 - p1.getTau();
            else if(type.equals("PRO")) dev = 111.8 - p1.getTau();
            else                        dev = 111.2 - p1.getTau();
            lblTau3.setText("tau: "+df1.format(-dev));
        }
    }
//}}}

//{{{ updateRamaLabels
//##################################################################################################
    // also updates rotamer quality
    void updateRamaLabels(Collection aaBackbones)
    {
        if(aaBackbones == null || aaBackbones.size() < 3)
        {
            lblRamaOK.setText("Rama ok");
            lblRamaOK.setForeground(normalColor);
            
            lblRama1.setText("-");
            lblRama2.setText("-");
            lblRama3.setText("-");
            
            if(twister != null) twister.setFeedback(null);
        }
        else
        {
            if(backrub.isRamaOK(aaBackbones))
            {
                lblRamaOK.setText("Rama ok");
                lblRamaOK.setForeground(normalColor);
            }
            else
            {
                lblRamaOK.setText("Rama OUTLIER");
                lblRamaOK.setForeground(alertColor);
            }
            
            double phi, psi;
            Iterator iter = aaBackbones.iterator();
            AminoAcid m1   = (AminoAcid)iter.next();
            AminoAcid ctr  = (AminoAcid)iter.next();
            AminoAcid p1   = (AminoAcid)iter.next();
            
            phi = m1.getPhi(); psi = m1.getPsi();
            lblRama1.setText("("+df0.format(phi)+", "+df0.format(psi)+")");
            if(rama.isOutlier(m1))  lblRama1.setForeground(alertColor);
            else                    lblRama1.setForeground(normalColor);
            
            phi = ctr.getPhi(); psi = ctr.getPsi();
            lblRama2.setText("("+df0.format(phi)+", "+df0.format(psi)+")");
            if(rama.isOutlier(ctr)) lblRama2.setForeground(alertColor);
            else                    lblRama2.setForeground(normalColor);
            
            phi = p1.getPhi(); psi = p1.getPsi();
            lblRama3.setText("("+df0.format(phi)+", "+df0.format(psi)+")");
            if(rama.isOutlier(p1))  lblRama3.setForeground(alertColor);
            else                    lblRama3.setForeground(normalColor);
            
            if(twister != null) twister.setFeedback(ctr);
        }
    }
//}}}

//{{{ onOptimize, onOptimizeFull
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOptimize(ActionEvent ev)
    {
        if(pdbfile == null || residue == null || backrub == null) return;
        
        brc.majorAngle = slMajor.getDegrees();
        double minor1, minor2;
        minor1 = slMinor1.getDegrees();
        minor2 = slMinor2.getDegrees();
        brc.minor1Start = minor1 - 45;
        brc.minor1End   = minor1 + 45;
        brc.minor1Step  = 1.0;
        brc.minor2Start = minor2 - 45;
        brc.minor2End   = minor2 + 45;
        brc.minor2Step  = 1.0;
        
        backrub.optimizeConformation(brc);
        
        slMinor1.setDegrees((int)brc.minor1Best);
        slMinor2.setDegrees((int)brc.minor2Best);
        // triggers a stateChanged() if needed
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOptimizeFull(ActionEvent ev)
    {
        if(pdbfile == null || residue == null || backrub == null) return;
        
        Cursor oldCursor, waitCursor;
        oldCursor = toolpane.getCursor();
        waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        toolpane.setCursor(waitCursor);
        
        brc.majorAngle = slMajor.getDegrees();
        brc.minor1Start = -180;
        brc.minor1End   = 180;
        brc.minor1Step  = 1.0;
        brc.minor2Start = -180;
        brc.minor2End   = 180;
        brc.minor2Step  = 1.0;

        backrub.optimizeConformation(brc);
        slMinor1.setDegrees((int)brc.minor1Best);
        slMinor2.setDegrees((int)brc.minor2Best);
        // triggers a stateChanged() if needed

        toolpane.setCursor(oldCursor);
    }
//}}}

//{{{ signalTransform
//##################################################################################################
    /**
    * A call to this method indicates the subscriber
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
    *
    * <p>This method will be called in response to TransformSignal.signalTransform().
    *
    * @param engine     the Engine object describing the
    *   dimensions and properties of the space to be painted.
    * @param xform      the Transform to apply.
    *   The subscriber must not modify the original Transform it
    *   receives! Subscibers may, however, copy and modify the
    *   Transform(s) they pass to internal substructures.
    */
    public void signalTransform(Engine engine, Transform xform)
    {
        if(probeDots != null) probeDots.signalTransform(engine, xform);
    }
//}}}
    
//{{{ setResidue
//##################################################################################################
    /** Passing null will clear out existing changes */
    void setResidue(Residue r)
    {
        if(r == null)
        {
            residue     = null;
            backrub     = null;
            probeDots   = null;
            twister     = null;
            if(mobilePlotter != null) mobilePlotter.clearLists(); // erases the graphics
            boxSidechainDials.setTarget(null);
            if(pdbfile == null) lblResID.setText("(Load PDB file)");
            else                lblResID.setText("(Pick residue)");
            kCanvas.repaint();
        }
        else
        {
            residue = r;
            backrub = new Backrub(residue);
            try {
                twister = new SidechainTwister(residue.getType());
                twister.initAllAngles( scAngles.measureAllAngles(residue) );
                twister.addChangeListener(this);
                boxSidechainDials.setTarget(twister.getDialPanel());
            } catch(Exception ex) { SoftLog.err.println(ex.getMessage()); }
            lblResID.setText("Residue: "+residue.getID());
        }

        stateChanged(null);
        slMajor.setDegrees(0);
        slMinor1.setDegrees(0);
        slMinor2.setDegrees(0);
    }
//}}}

//{{{ askKeepChanges, onKeepChanges, onDiscardChanges
//##################################################################################################
    /**
    * Asks the user if s/he wants to keep the changes made to this residue.
    * The currently active residue will be set to null afterwords.
    */
    void askKeepChanges()
    {
        if(residue == null) return;
        
        if(//(slMajor.getDegrees() != 0 || slMinor1.getDegrees() != 0 || slMinor2.getDegrees() != 0) &&
        JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getMainWindow(),
        "Keep the changes made to this residue, "+residue.getID()+"?",
        "Change model?",
        JOptionPane.YES_NO_OPTION)) onKeepChanges(null);
        else                        onDiscardChanges(null);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onKeepChanges(ActionEvent ev)
    {
        if(residue == null) return;

        // Adjust backbone
        Collection newBB = backrub.makeConformation(slMajor.getDegrees(),
        slMinor1.getDegrees(), 
        slMinor2.getDegrees());
        
        // Adjust sidechain
        if(twister != null && scAngles != null)
        {
            Iterator iter = newBB.iterator();
            iter.next();
            AminoAcid aa = (AminoAcid)iter.next(); // central residue
            scAngles.setAllAngles(aa, twister.getAllAngles());
        }
        
        // Make it permanent
        backrub.updateModel(newBB);
        
        modifiedResidues.put(residue.getPrev(), new AminoAcid(residue.getPrev()));
        modifiedResidues.put(residue,           new AminoAcid(residue));
        modifiedResidues.put(residue.getNext(), new AminoAcid(residue.getNext()));
        
        modPlotter.plotAminoAcids(modifiedResidues);
        
        setResidue(null);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDiscardChanges(ActionEvent ev)
    {
        if(residue == null) return;
        setResidue(null);
    }
//}}}

//{{{ askSavePDB, onSavePDB
//##################################################################################################
    /** Asks the user if s/he wants to keep the changes made to all residues. */
    void askSavePDB()
    {
        if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getMainWindow(),
        "Save the changes made to this model?",
        "Save model?",
        JOptionPane.YES_NO_OPTION)) onSavePDB(null);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSavePDB(ActionEvent ev)
    {
        if(pdbfile == null) return;
        if(residue != null) askKeepChanges();
        
        File f = openChooser.getSelectedFile();
        String name = f.getName();
        if(name.toLowerCase().endsWith(".pdb")) name = name.substring(0, name.length()-4) + ".rot";
        else                                    name = name + ".rot";
        saveChooser.setSelectedFile(new File(openChooser.getCurrentDirectory(), name));

        if(JFileChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getMainWindow()))
        {
            f = saveChooser.getSelectedFile();
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getMainWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    ArrayList modifiedAtoms = new ArrayList();
                    for(Iterator iter = modifiedResidues.keySet().iterator(); iter.hasNext(); )
                        modifiedAtoms.addAll(((Residue)iter.next()).getAtoms());
                    
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
                    pdbfile.writeAtoms(modifiedAtoms , out);
                    out.close();
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

//{{{ onExportDots
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExportDots(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if( kin!= null && probeDots != null)
        {
            kin.appendKinemage(probeDots);
            probeDots = null;
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
            stateChanged(null); // regenerates Probe dots
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    {
        return toolpane;
    }
    
    public String getHelpAnchor()
    { return "#backrub-tool"; }
    
    public String toString() { return "Backrub"; }
//}}}
}//class

