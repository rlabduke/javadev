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
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
import chiropraxis.mc.*;
import chiropraxis.rotarama.*;
//}}}
/**
* <code>HingeTool</code> is an implementation of "C-alpha hinges,"
* a rotation around an axis connecting any two C-alphas.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  2 15:59:18 EDT 2003
*/
public class HingeTool extends ModelingTool implements Remodeler, ChangeListener
{
//{{{ Constants
    static final DecimalFormat df1 = new DecimalFormat("+0.0;-0.0");
    static final DecimalFormat df0 = new DecimalFormat("0");
    
    static final Color normalColor  = new Color(0f, 0f, 0f);
    static final Color alertColor   = new Color(0.6f, 0f, 0f);
//}}}

//{{{ Variable definitions
//##################################################################################################
    Residue             anchor1, anchor2;
    PeptideTwister2     twister         = null;
    KList               anchorList;
    Ramachandran        rama            = null;
    TauByPhiPsi         tauscorer       = null;
    
    //GUI
    JPanel              tabwrapper;
    JTabbedPane         tabpane;
    TablePane           toolpane;
    AngleDial           hingeDial;
    JLabel[]            headerLabels, res1Labels, res2Labels;
    JCheckBox           cbIdealizeSC;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public HingeTool(ToolBox tb)
    {
        super(tb);
        anchor1 = anchor2 = null;
        
        anchorList = new KList();
        
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI()
    {
        hingeDial = new AngleDial();
        hingeDial.addChangeListener(this);
        
        JButton btnRelease = new JButton(new ReflectiveAction("Release", null, this, "onReleaseResidues"));
        
        headerLabels = new JLabel[] { new JLabel("Residue"), new JLabel("Tau dev"), new JLabel("Karplus"), new JLabel("Ramachdrn"), new JLabel("phi,psi") };
        res1Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        res2Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        
        cbIdealizeSC = new JCheckBox(new ReflectiveAction("Idealize end sidechains", null, this, "onToggleIdealSC"));
        cbIdealizeSC.setSelected(true);
        
        toolpane = new TablePane();
        toolpane.startSubtable(); // this way, contents aren't stretched
        toolpane.save().hfill(true).vfill(true).addCell(hingeDial).restore();
        toolpane.save().center().middle();
        toolpane.addCell(btnRelease);
        toolpane.restore();
        toolpane.newRow();
        toolpane.addCell(cbIdealizeSC, 2, 1);
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

//{{{ start/stop/reset
//##################################################################################################
    public void start()
    {
        super.start();
        
        // force loading of data tables that will be used later
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) {}
        try { tauscorer = TauByPhiPsi.getInstance(); }
        catch(IOException ex) {}
        
        anchorList.setColor( KPalette.peach );
        
        // Bring up model manager
        modelman.onShowDialog(null);
    }
//}}}

//{{{ c_click, markAnchor, wheel, c_wheel
//##################################################################################################
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null || (anchor1 != null && anchor2 != null))
        { onReleaseResidues(null); }
        else if(p != null)
        {
            ModelState state = modelman.getMoltenState();
            Residue newRes = this.getResidueNearest(modelman.getModel(), state,
                p.getOrigX(), p.getOrigY(), p.getOrigZ());
            if(anchor1 == null)
            {
                anchor1 = newRes;
                markAnchor(newRes, state);
                updateLabels();
            }
            else if(anchor2 == null)
            {
                anchor2 = newRes;
                markAnchor(newRes, state);
                //updateLabels(); -- done by grabResidues()
                grabResidues();
            }
        }
    }
    
    /** Creates a new ball to mark the position of an anchor residue */
    void markAnchor(Residue res, ModelState state)
    {
        Atom ca = res.getAtom(" CA ");
        if(ca == null) return;
        
        try
        {
            AtomState cas = state.get(ca);
            BallPoint mark = new BallPoint(anchorList, "C-alpha axis endpoint");
            mark.r0 = 0.3f;
            mark.setOrigX(cas.getX());
            mark.setOrigY(cas.getY());
            mark.setOrigZ(cas.getZ());
            anchorList.add(mark);
            kCanvas.repaint();
        }
        catch(AtomException ex) { ex.printStackTrace(); }
    }
    
    public void wheel(int rotation, MouseEvent ev)
    { hingeDial.setDegrees(hingeDial.getDegrees()-rotation); }
    
    public void c_wheel(int rotation, MouseEvent ev)
    { super.wheel(rotation, ev); }
//}}}

//{{{ stateChanged, updateModelState, onToggleIdealSC
//##################################################################################################
    // ev may be null!
    public void stateChanged(ChangeEvent ev)
    {
        modelman.requestStateRefresh(); // will call this.updateModelState()
        updateLabels();
        if(twister != null) twister.updateLabels();
        kCanvas.repaint();
    }

    public ModelState updateModelState(ModelState before)
    {
        try
        {
            Collection residues = CaRotation.makeMobileGroup(modelman.getModel(), anchor1, anchor2);
            ModelState after = CaRotation.makeConformation(
                residues, before, hingeDial.getDegrees(), cbIdealizeSC.isSelected());
            
            if(twister != null)
                after = twister.updateConformation(after);
            
            return after;
        }
        catch(AtomException ex)
        {
            ex.printStackTrace();
            return before;
        }
    }
    
    // target of reflection
    public void onToggleIdealSC(ActionEvent ev)
    { stateChanged(null); }
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
            Model model = modelman.getModel();
            ModelState modelState = modelman.getMoltenState();
            
            try {
                double taudev = AminoAcid.getTauDeviation(r, modelState);
                l[1].setText(df1.format(taudev));
                if(Math.abs(taudev) >= 3.0) l[1].setForeground(alertColor);
            } catch(AtomException ex) { l[1].setText("-?-"); }
            
            try {
                double phi = AminoAcid.getPhi(model, r, modelState);
                double psi = AminoAcid.getPsi(model, r, modelState);
                l[4].setText(df0.format(phi)+" , "+df0.format(psi));

                if(rama == null)
                    l[3].setText("[no data]");
                else if(rama.isOutlier(model, r, modelState))
                {
                    l[3].setText("OUTLIER");
                    l[3].setForeground(alertColor);
                }
                else if(rama.rawScore(model, r, modelState) > 0.02)
                    l[3].setText("favored");
                else
                    l[3].setText("allowed");
                
                if(tauscorer == null)
                    l[2].setText("[no data]");
                else
                {
                    double taudev = tauscorer.getTauDeviation(model, r, modelState);
                    l[2].setText(df1.format(taudev));
                    if(Math.abs(taudev) >= 3.0) l[2].setForeground(alertColor);
                }
            }
            catch(AtomException ex)     { l[2].setText("-?-"); l[3].setText("[no phi,psi]"); l[4].setText("? , ?"); }
            catch(ResidueException ex)  { l[2].setText("-?-"); l[3].setText("[no phi,psi]"); l[4].setText("? , ?"); }
        }
        
        dialog.pack();
    }
//}}}

//{{{ grabResidues
//##################################################################################################
    /** Responsible for checking residues out from the ModelManager */
    void grabResidues()
    {
        if(anchor1 == null || anchor2 == null) return;
        hingeDial.setDegrees(0); // no rotation to start with
        
        // put anchors in logical order
        if(anchor1.compareTo(anchor2) > 0)
        {
            Residue swap = anchor1;
            anchor1 = anchor2;
            anchor2 = swap;
        }
        
        try
        {
            Collection residues = CaRotation.makeMobileGroup(modelman.getModel(), anchor1, anchor2);
            modelman.registerTool(this, residues);
            
            // Insert new tab
            twister = new PeptideTwister2(this, residues);
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
        }
        catch(IllegalArgumentException ex)
        {
            // Couldn't connect, so start over
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
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
        if(anchor1 == null || anchor2 == null)
        {
            anchor1 = anchor2 = null;
            anchorList.clear();
            updateLabels();
            kCanvas.repaint();
            return;
        }
        
        int reply = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "Do you want to keep the changes\nyou've made to these residues?",
            "Keep changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(reply == JOptionPane.CANCEL_OPTION) return;
        
        if(reply == JOptionPane.YES_OPTION)
            modelman.requestStateChange(this); // will call this.updateModelState()
        else //  == JOptionPane.NO_OPTION
            modelman.unregisterTool(this);

        anchor1 = anchor2 = null;
        anchorList.clear();
        tabpane.removeTabAt(1);
        twister = null;
        updateLabels();
        kCanvas.repaint();
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
        anchorList.signalTransform(engine, xform);
    }
//}}}
    
//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

