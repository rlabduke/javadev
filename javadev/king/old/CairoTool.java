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
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
import chiropraxis.cairo.*;
import chiropraxis.rotarama.*;
//}}}
/**
* <code>CairoTool</code> is an implementation of "C-alpha hinges,"
* a rotation around an axis connecting any two C-alphas.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  2 15:59:18 EDT 2003
*/
public class CairoTool extends BasicTool implements ChangeListener
{
//{{{ Constants
    static final DecimalFormat df1 = new DecimalFormat("+0.0;-0.0");
    static final DecimalFormat df0 = new DecimalFormat("0");
    
    static final Color normalColor  = new Color(0f, 0f, 0f);
    static final Color alertColor   = new Color(0.6f, 0f, 0f);
//}}}

//{{{ Variable definitions
//##################################################################################################
    ModelManager        modelman        = null;
    Residue             anchor1, anchor2;
    ModelState          modelState      = null;
    PeptideTwister      twister         = null;
    KList               anchorList;
    ModelPlotter        plotter;
    ProbePlotter        probePlotter    = null;
    Ramachandran        rama            = null;
    
    //GUI
    JPanel              tabwrapper;
    JTabbedPane         tabpane;
    TablePane           toolpane;
    AngleDial           cairoDial;
    JLabel[]            headerLabels, res1Labels, res2Labels;
    JCheckBoxMenuItem   cbmUpdateDots, cbmDotsToWater;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public CairoTool(ToolBox tb)
    {
        super(tb);
        anchor1 = anchor2 = null;
        
        anchorList = new KList();
        plotter = new ModelPlotter();
        
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI()
    {
        cairoDial = new AngleDial();
        cairoDial.addChangeListener(this);
        
        JButton btnRelease = new JButton(new ReflectiveAction("Release", null, this, "onReleaseResidues"));
        
        headerLabels = new JLabel[] { new JLabel("Residue"), new JLabel("Tau dev"), new JLabel("Ramachdrn"), new JLabel("phi,psi") };
        res1Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        res2Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        
        toolpane = new TablePane();
        toolpane.startSubtable(); // this way, contents aren't stretched
        toolpane.save().hfill(true).vfill(true).addCell(cairoDial).restore();
        toolpane.save().center().middle();
        toolpane.addCell(btnRelease);
        toolpane.restore();
        toolpane.newRow();
        toolpane.save().hfill(true).startSubtable(2,1);
        toolpane.hfill(true).insets(1,3,1,3);
        for(int i = 0; i < headerLabels.length; i++)
        {
            toolpane.add(headerLabels[i]);
            toolpane.add(res1Labels[i]);
            toolpane.add(res2Labels[i]);
            toolpane.newRow();
        }
        toolpane.endSubtable().restore();
        toolpane.endSubtable(); // this way, contents aren't stretched
        
        tabpane = new JTabbedPane();
        tabpane.addTab("Hinges", toolpane);
        
        // Prevents the background from showing through
        tabwrapper = new JPanel(new BorderLayout());
        tabwrapper.add(tabpane);
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
        
        menu = new JMenu("Options");
        menu.setMnemonic(KeyEvent.VK_O);
        menubar.add(menu);
        cbmUpdateDots = new JCheckBoxMenuItem(new ReflectiveAction("\"Live\" Probe dots", null, this, "onProbeOption"));
        cbmUpdateDots.setMnemonic(KeyEvent.VK_P);
        menu.add(cbmUpdateDots);
        cbmDotsToWater = new JCheckBoxMenuItem(new ReflectiveAction("Show dots to water", null, this, "onProbeOption"));
        cbmDotsToWater.setMnemonic(KeyEvent.VK_W);
        menu.add(cbmDotsToWater);
        
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menubar.add(menu);
        menu.add(this.getHelpMenuItem());
        
        dialog.setJMenuBar(menubar);
    }
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return tabwrapper; }
    
    public String getHelpAnchor()
    { return "#hinges-tool"; }
    
    public String toString() { return "C-alpha hinges"; }
//}}}

//{{{ start/stop/reset, findModelman
//##################################################################################################
    public void start()
    {
        super.start();
        
        if(!findModelman())
        {
            JOptionPane.showMessageDialog(kMain.getMainWindow(),
                "This tool requires the Model Manager\nplugin in order to function.",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
        }

        // force loading of data tables that will be used later
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) {}
        
        plotter.mainColor   = KPalette.peachtint;
        plotter.sideColor   = KPalette.orange;
        plotter.hyColor     = KPalette.gray;
        anchorList.setColor(  KPalette.orange);
    }
    
    /** Returns true if model manager is found */
    boolean findModelman()
    {
        modelman = null;
        Collection plugins = parent.getPluginList();
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            Plugin plugin = (Plugin)iter.next();
            if(ModelManager.class.equals(plugin.getClass()))
                modelman = (ModelManager)plugin;
        }
        
        return (modelman != null);
    }
//}}}

//{{{ c_click, markAnchor, wheel, c_wheel
//##################################################################################################
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        if(modelman == null) findModelman();
        
        if(p == null || (anchor1 != null && anchor2 != null))
        { onReleaseResidues(null); }
        else if(p != null && modelman != null && modelman.getModel() != null && p.getName().startsWith(" ca "))
        {
            try {
                Residue newRes = ModelManager.findResidueByKinemageID(modelman.getModel(), p.getName());
                if(anchor1 == null)
                {
                    anchor1 = newRes;
                    markAnchor(p);
                    updateLabels();
                }
                else if(anchor2 == null)
                {
                    anchor2 = newRes;
                    markAnchor(p);
                    //updateLabels(); -- done by grabResidues()
                    grabResidues();
                }
            } catch(NoSuchElementException ex) {} // thrown by findResByKinID()
        }
    }
    
    /** Creates a new ball to mark the position of an anchor residue */
    void markAnchor(KPoint p)
    {
        BallPoint mark = new BallPoint(anchorList, "C-alpha axis endpoint");
        mark.r0 = 0.3f;
        mark.x0 = p.x0;
        mark.y0 = p.y0;
        mark.z0 = p.z0;
        anchorList.add(mark);
        kCanvas.repaint();
    }
    
    public void wheel(int rotation, MouseEvent ev)
    { cairoDial.setDegrees(cairoDial.getDegrees()-rotation); }
    
    public void c_wheel(int rotation, MouseEvent ev)
    { super.wheel(rotation, ev); }
//}}}

//{{{ stateChanged
//##################################################################################################
    // ev may be null!
    public void stateChanged(ChangeEvent ev)
    {
        plotConformation();
        updateLabels();
        if(twister != null) twister.updateLabels();
        plotProbeDots();
        kCanvas.repaint();
    }
//}}}
    
//{{{ updateLabels
//##################################################################################################
    void updateLabels()
    {
        updateLabels(anchor1, res1Labels);
        updateLabels(anchor2, res2Labels);
    }
    
    void updateLabels(Residue r, JLabel[] l)
    {
        // Make color normal again
        for(int i = 0; i < l.length; i++) l[i].setForeground(normalColor);
            
        if(r == null)
        {
            for(int i = 0; i < l.length; i++) l[i].setText("");
        }
        else
        {
            l[0].setText(r.toString());
            if(modelState == null) modelState = modelman.getState();
            
            try {
                double taudev = AminoAcid.getTauDeviation(r, modelState);
                l[1].setText(df1.format(taudev));
                if(Math.abs(taudev) >= 3.0) l[1].setForeground(alertColor);
            } catch(AtomException ex) { l[1].setText("-?-"); }
            
            try {
                double phi = AminoAcid.getPhi(r, modelState);
                double psi = AminoAcid.getPsi(r, modelState);
                l[3].setText(df0.format(phi)+" , "+df0.format(psi));

                if(rama == null)
                    l[2].setText("[no data]");
                else if(rama.isOutlier(r, modelState))
                {
                    l[2].setText("OUTLIER");
                    l[2].setForeground(alertColor);
                }
                else if(rama.rawScore(r, modelState) > 0.02)
                    l[2].setText("favored");
                else
                    l[2].setText("allowed");
            }
            catch(AtomException ex)     { l[2].setText("[no phi,psi]"); l[3].setText("? , ?"); }
            catch(ResidueException ex)  { l[2].setText("[no phi,psi]"); l[3].setText("? , ?"); }
        }
        
        dialog.pack();
    }
//}}}

//{{{ grabResidues
//##################################################################################################
    /** Responsible for checking residues out from the ModelManager */
    void grabResidues()
    {
        if(modelman == null) return;
        if(anchor1 == null || anchor2 == null) return;
        cairoDial.setDegrees(0); // no rotation to start with
        
        // put anchors in logical order
        if(anchor1.compareTo(anchor2) > 0)
        {
            Residue swap = anchor1;
            anchor1 = anchor2;
            anchor2 = swap;
        }
        
        try
        {
            Collection residues = CaRotation.makeMobileGroup(anchor1, anchor2);
            for(Iterator iter = residues.iterator(); iter.hasNext(); )
            {
                modelman.checkout((Residue)iter.next());
            }
            
            // Insert new tab
            twister = new PeptideTwister(this, residues);
            Dimension d = twister.getPreferredSize();
            if(d.getWidth() <= 500)
                tabpane.insertTab("Peptides", null, twister, null, 1);
            else
            {
                JScrollPane twistScroll = new JScrollPane(twister,
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                d.setSize(500, d.getHeight()+50);
                twistScroll.setPreferredSize(d);
                tabpane.insertTab("Peptides", null, twistScroll, null, 1);
            }
    
            // Set up to plot Probe dots
            Kinemage    kin     = kMain.getKinemage();
            
            if(kin != null && (probePlotter == null || !probePlotter.getKinemage().equals(kin)))
            {
                if(probePlotter != null) probePlotter.terminate(); // clean up the old one
                probePlotter = new ProbePlotter(kMain, kin);
            }
        }
        catch(IllegalArgumentException ex)
        {
            // Couldn't connect, so start over
            JOptionPane.showMessageDialog(kMain.getMainWindow(),
                anchor1+" and "+anchor2+"\nare not part of the same chain.\nPlease try again.",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            anchor1 = anchor2 = null;
            anchorList.clear();
        }
        
        stateChanged(null);
    }
//}}}

//{{{ onReleaseResidues
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /**
    * Checks residues back into the ModelManager,
    * nulls the anchors and repaints the graphics.
    * @param ev is ignored, may be null.
    */
    public void onReleaseResidues(ActionEvent ev)
    {
        if(modelman == null) return;
        if(anchor1 == null || anchor2 == null)
        {
            anchor1 = anchor2 = null;
            anchorList.clear();
            updateLabels();
            kCanvas.repaint();
            return;
        }
        
        int reply = JOptionPane.showConfirmDialog(kMain.getMainWindow(),
            "Do you want to keep the changes\nyou've made to these residues?",
            "Keep changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(reply == JOptionPane.CANCEL_OPTION) return;
        
        Collection residues = CaRotation.makeMobileGroup(anchor1, anchor2);
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue res = (Residue)iter.next();
            modelman.checkin(res);
        }
        
        if(reply == JOptionPane.YES_OPTION && modelState != null)
        {
            modelman.updateState(modelState);
        }

        anchor1 = anchor2 = null;
        plotter.clearLists();
        anchorList.clear();
        tabpane.removeTabAt(1);
        twister = null;
        updateLabels();
        kCanvas.repaint();
    }
//}}}

//{{{ plotConformation, signalTransform
//##################################################################################################
    /** Uses plotter to plot the current conformation of the current selection */
    void plotConformation()
    {
        if(modelman == null) return;
        if(anchor1 == null || anchor2 == null) return;
        
        Collection residues = CaRotation.makeMobileGroup(anchor1, anchor2);
        modelState = CaRotation.makeConformation(residues, modelman.getState(), cairoDial.getDegrees());
        
        if(twister != null)
            modelState = twister.updateConformation(modelState);

        plotter.plotAminoAcids(residues, modelState);
        plotter.setHOn(false);
    }

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
        anchorList.signalTransform(engine, xform);
        KGroup group = plotter.createGroup("dummy");
        if(group != null) group.signalTransform(engine, xform);
    }
//}}}
    
//{{{ plotProbeDots, onProbeOption
//##################################################################################################
    void plotProbeDots()
    {
        if(probePlotter == null || modelman == null) return;
        if(anchor1 == null || anchor2 == null || modelState == null) return;
        
        if(cbmUpdateDots.isSelected())
        {
            Collection residues = CaRotation.makeMobileGroup(anchor1, anchor2);
            try { probePlotter.requestProbe(residues, modelState, modelman.getStateAsPDB()); }
            catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Called whenever the Probe options are changed. */
    public void onProbeOption(ActionEvent ev)
    {
        if(probePlotter == null) return;
        
        // cbmUpdateDots is checked in plotProbeDots()
        probePlotter.setDotsToWater( cbmDotsToWater.isSelected() );
        
        // We don't really need a full stateChanged() here:
        plotProbeDots();
        kCanvas.repaint();
    }
//}}}
    
//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

