// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.draw;
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

import chiropraxis.kingtools.BgKinRunner;

import driftwood.data.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>ProbeNearbyTool</code> allows one to draw Probe dots for the entirety
* of a local area on the fly, without any structural refitting involved.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Aug  2 2010
*/
public class ProbeNearbyTool extends Plugin
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    File                    file         = null;
    CoordinateFile          coordFile    = null;
    Model                   model        = null;
    ModelState              state        = null;
    String                  altLabel     = null;
    BgKinRunner             probePlotter = null;
    
    SuffixFileFilter        pdbFilter;
    JFileChooser            openChooser;
    JCheckBox               cbUseSegID;
    JDialog                 dialog;
    JLabel                  lblFileName;
    AttentiveTextField      tfProbeRad;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ProbeNearbyTool(ToolBox tb)
    {
        super(tb);
        
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
        
        String currdir = System.getProperty("user.dir");

        openChooser = new JFileChooser();
        openChooser.addChoosableFileFilter(pdbFilter);
        openChooser.setFileFilter(pdbFilter);
        if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
        cbUseSegID = new JCheckBox("Use SegID to define chains", false);
        openChooser.setAccessory(cbUseSegID);
        // can't set PDB file yet b/c kinemage not loaded
    }
//}}}

//{{{ buildDialog
//##################################################################################################
    private void buildDialog()
    {
        lblFileName = new JLabel();
        JButton bnNewFile = new JButton("new file");
        bnNewFile.addActionListener(new ReflectiveAction("open-new-file", null, this, "onOpenPDB"));
        
        JLabel lblProbeRad = new JLabel("Radius from screen center:");
        tfProbeRad = new AttentiveTextField("5", 3);
        tfProbeRad.addActionListener(new ReflectiveAction("run-probe", null, this, "onRunProbe"));
        JButton bnRunProbe = new JButton("Probe!");
        bnRunProbe.addActionListener(new ReflectiveAction("run-probe", null, this, "onRunProbe"));
        
        TablePane cp = new TablePane();
        cp.insets(2).weights(1,0.1);
        cp.addCell(lblFileName).addCell(bnNewFile);
        cp.newRow().addCell(cp.strut(0,2)).newRow(); //spacer
        cp.addCell(lblProbeRad).addCell(tfProbeRad).newRow();
        cp.addCell(bnRunProbe);
        
        dialog = new JDialog(kMain.getTopWindow(), this.toString(), false); // not modal
        //dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setContentPane(cp);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                if(probePlotter != null)
                {
                    probePlotter.terminate();
                    probePlotter = null;
                }
                dialog.setVisible(false);
            }
        });
        
        refreshGUI();
    }
//}}}

//{{{ refreshGUI
//##################################################################################################
    /** One stop shopping to ensure the GUI reflects the current conditions. */
    protected void refreshGUI()
    {
        if(file != null)
        {
            String fileLbl = "File: "+file.getName();
            String alt = altLabel;
            if(!alt.equals(" ")) fileLbl = fileLbl+" ["+alt+"]";
            lblFileName.setText(fileLbl);
        }
        else lblFileName.setText("File not loaded");
        
        dialog.pack();
    }
//}}}

//{{{ onOpenPDB
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenPDB(ActionEvent ev)
    {
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
        file                    = f;
        PdbReader pdbr          = new PdbReader();
        pdbr.setUseSegID(cbUseSegID.isSelected());
        coordFile              = pdbr.read(file);
        
        // Let user select model
        Collection models = coordFile.getModels();
        if(models.size() == 1)
            model = coordFile.getFirstModel();
        else
        {
            Object[] choices = models.toArray();
            model = (Model)JOptionPane.showInputDialog(kMain.getTopWindow(),
                "This file has multiple models. Please choose one:",
                "Choose model", JOptionPane.PLAIN_MESSAGE,
                null, choices, choices[0]);
            if(model == null)
                model = coordFile.getFirstModel();
        }
        
        // Let user select alt conf (iff there's more than one)
        altLabel = askAltConf(model, "This file has alternate conformations. Please choose one:");
        
        if(altLabel != null)  state = model.getState(altLabel);
        if(state == null)     state = model.getState();
        
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

//{{{ onChooseAltConf
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onChooseAltConf(ActionEvent ev)
    {
        if(model == null) return;
        // Let user select alt conf (iff there's more than one)
        String altLabel = askAltConf(model, "This file has alternate conformations. Please choose one:");
    }
//}}}

//{{{ prepProbePlotter, onRunProbe
//##################################################################################################
    void prepProbePlotter() throws NumberFormatException
    {
        // Set up to plot Probe dots
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            if(probePlotter == null || !probePlotter.getKinemage().equals(kin))
                probePlotter = new BgKinRunner(kMain, kin, "");
            
            // Incomplete, will be completed in a moment
            double probeRad = Double.parseDouble(tfProbeRad.getText().trim());
            String probeCmd = " -quiet -kin -mc -both -stdbonds"
                +" 'within "+probeRad+" of {viewcenter} not water'"
                +" 'within "+probeRad+" of {viewcenter} not water' '{pdbfile}' -";
            String probeExe = probePlotter.findProgram("probe");
            probePlotter.setCommand(probeExe+probeCmd); // now complete cmd line
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRunProbe(ActionEvent ev)
    {
        try
        {
            prepProbePlotter();
            probePlotter.requestRun(new HashSet(), state, file);
        }
        catch(NumberFormatException ex)
        { SoftLog.err.println("Couldn't parse "+tfProbeRad.getText()+" as a # of Angstroms!"); }
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
        onOpenPDB(null);
    }
//}}}

//{{{ getHelpAnchor, toString, isAppletSafe
//##################################################################################################
    public String getHelpAnchor()
    { return "#probe-nearby-plugin"; }
    
    public String toString()
    { return "Probe nearby"; }

    /** This plugin is not applet-safe because it invokes other processes and loads files. */
    static public boolean isAppletSafe()
    { return false; }
//}}}
}//class

