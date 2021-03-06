// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
import king.*;
import king.core.*;
import king.points.BallPoint;

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
* <code>BackrubWindow</code> is an implementation of the BACKRUB algorithm
* for small changes to the protein mainchain.
* As such, it is a specialization of the HingeTool.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  2 15:59:18 EDT 2003
*/
public class BackrubWindow implements Remodeler, ChangeListener, WindowListener
{
//{{{ Constants
    static final DecimalFormat df1 = new DecimalFormat("+0.0;-0.0");
    static final DecimalFormat df0 = new DecimalFormat("0");
    
    static final Color normalColor  = new Color(0f, 0f, 0f);
    static final Color alertColor   = new Color(0.6f, 0f, 0f);
//}}}

//{{{ Variable definitions
//##################################################################################################
    // Things that used to be supplied by BasicTool as a superclass:
    KingMain            kMain;
    KinCanvas           kCanvas;
    ModelManager2       modelman;
    Window             dialog;
    
    Residue             anchor1, ctrRes, anchor2;
    KList               anchorList;
    Ramachandran        rama            = null;
    TauByPhiPsi         tauscorer       = null;
    
    //GUI
    TablePane2          toolpane;
    AngleDial           hingeDial, pept1Dial, pept2Dial;
    JLabel[]            headerLabels, res1Labels, res2Labels, res3Labels;
    JCheckBox           cbIdealizeSC;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @throws IllegalArgumentException if the residue code isn't recognized
    */
    public BackrubWindow(KingMain kMain, Residue target, ModelManager2 mm)
    {
        this.kMain = kMain;
        this.kCanvas = kMain.getCanvas();
        this.modelman = mm;
        this.ctrRes = target;
        this.anchorList = new KList(KList.BALL);
        anchorList.setColor( KPalette.peach );
        
        buildGUI(kMain.getTopWindow());
        
        // force loading of data tables that will be used later
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) {}
        try { tauscorer = TauByPhiPsi.getInstance(); }
        catch(IOException ex) {}
        
        Model model = modelman.getModel();
        ModelState state = modelman.getMoltenState();
        this.anchor1 = ctrRes.getPrev(model);
        this.anchor2 = ctrRes.getNext(model);
        if(anchor1 != null && anchor2 != null)
        {
            markAnchor(anchor1, state);
            markAnchor(anchor2, state);
            //updateLabels(); -- done by stateChanged()
            
            // May also throw IAEx:
            Collection residues = CaRotation.makeMobileGroup(modelman.getModel(), anchor1, anchor2);
            modelman.registerTool(this, residues);
            stateChanged(null);
        }
        else
        {
            dialog.dispose();
            throw new IllegalArgumentException("Can't find neighbor residues for "+target);
        }
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI(Frame frame)
    {
        hingeDial = new AngleDial();
        hingeDial.addChangeListener(this);
        pept1Dial = new AngleDial();
        pept1Dial.addChangeListener(this);
        pept2Dial = new AngleDial();
        pept2Dial.addChangeListener(this);
        
        JButton btnRelease = new JButton(new ReflectiveAction("Finished", null, this, "onReleaseResidues"));
        JButton btnRotamer = new JButton(new ReflectiveAction("Rotate sidechain", null, this, "onRotateSidechain"));
        
        headerLabels = new JLabel[] { new JLabel("Residue"), new JLabel("Tau dev"), new JLabel("Karplus"), new JLabel("Ramachdrn"), new JLabel("phi,psi") };
        res1Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        res2Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        res3Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        
        cbIdealizeSC = new JCheckBox(new ReflectiveAction("Idealize sidechains", null, this, "onToggleIdealSC"));
        cbIdealizeSC.setSelected(false); // 25 Oct 06: changed by mandate from JSR
        
        toolpane = new TablePane2();
        toolpane.skip();
        toolpane.addCell(pept1Dial);
        toolpane.addCell(hingeDial);
        toolpane.addCell(pept2Dial);
        toolpane.newRow();
        for(int i = 0; i < headerLabels.length; i++)
        {
            if(i == 2) continue; // skip Karplus' phi/psi-dependent tau deviation
            toolpane.add(headerLabels[i]);
            toolpane.add(res1Labels[i]);
            toolpane.add(res2Labels[i]);
            toolpane.add(res3Labels[i]);
            toolpane.newRow();
        }
        toolpane.newRow();
        toolpane.addCell(TablePane2.strut(0,10));
        toolpane.newRow();
        toolpane.startSubtable(4,1);
            toolpane.addCell(btnRelease);
            toolpane.addCell(cbIdealizeSC);
            toolpane.addCell(btnRotamer);
        toolpane.endSubtable();
        
        /*JMenuBar menubar = new JMenuBar();
        JMenu menu, submenu;
        JMenuItem item;
        
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menubar.add(menu);
        menu.add(this.getHelpMenuItem());*/
        
        // Assemble the dialog
        if (kMain.getPrefs().getBoolean("minimizableTools")) {
          JFrame fm = new JFrame("BACKRUB: "+ctrRes.toString());
          fm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
          fm.setContentPane(toolpane);
          dialog = fm;
        } else {
          JDialog dial = new JDialog(frame, "BACKRUB: "+ctrRes.toString(), false);
          dial.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
          dial.setContentPane(toolpane);
          dialog = dial;
        }
        dialog.addWindowListener(this);
        //dialog.setJMenuBar(menubar);
        dialog.pack();
        dialog.setVisible(true);
    }
//}}}

//{{{ markAnchor
//##################################################################################################
    /** Creates a new ball to mark the position of an anchor residue */
    void markAnchor(Residue res, ModelState state)
    {
        Atom ca = res.getAtom(" CA ");
        if(ca == null) return;
        
        try
        {
            AtomState cas = state.get(ca);
            BallPoint mark = new BallPoint("C-alpha axis endpoint");
            mark.r0 = 0.3f;
            mark.setX(cas.getX());
            mark.setY(cas.getY());
            mark.setZ(cas.getZ());
            anchorList.add(mark);
            kCanvas.repaint();
        }
        catch(AtomException ex) { ex.printStackTrace(); }
    }
//}}}

//{{{ stateChanged, updateModelState, onToggleIdealSC
//##################################################################################################
    // ev may be null!
    public void stateChanged(ChangeEvent ev)
    {
        modelman.requestStateRefresh(); // will call this.updateModelState()
        updateLabels();
        kCanvas.repaint();
    }

    public ModelState updateModelState(ModelState before)
    {
        try
        {
            Collection residues = CaRotation.makeMobileGroup(modelman.getModel(), anchor1, anchor2);
            boolean idealize = cbIdealizeSC.isSelected();
            
            // Major rotation
            ModelState after = CaRotation.makeConformation(
                residues, before, hingeDial.getDegrees(), idealize);
            
            // Peptide rotations
            Residue[]   res         = (Residue[]) residues.toArray(new Residue[residues.size()]);
            double[]    angles      = new double[]  { pept1Dial.getDegrees(), pept2Dial.getDegrees() };
            boolean[]   idealizeSC  = new boolean[] { idealize, idealize, idealize };
            //System.err.println(res.length+" "+angles.length+" "+idealizeSC.length);
            after = CaRotation.twistPeptides(res, after, angles, idealizeSC);
            
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
        updateLabels(ctrRes,  res2Labels);
        updateLabels(anchor2, res3Labels);
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
        int reply = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "Do you want to keep the changes\nyou've made to these residues?",
            "Keep changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(reply == JOptionPane.CANCEL_OPTION) return;
        
        if(reply == JOptionPane.YES_OPTION)
        {
            modelman.requestStateChange(this); // will call this.updateModelState()
            modelman.addUserMod("Refit backbone of "+ctrRes);
        }
        else //  == JOptionPane.NO_OPTION
            modelman.unregisterTool(this);
        
        dialog.dispose();
        kCanvas.repaint();
    }
//}}}

//{{{ onRotateSidechain
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /**
    * Launches a SidechainRotator window for this residue.
    * @param ev is ignored, may be null.
    */
    public void onRotateSidechain(ActionEvent ev)
    {
        try
        {
            new SidechainRotator(kMain, ctrRes, modelman);
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ Dialog window listeners
//##################################################################################################
    public void windowActivated(WindowEvent ev)   {}
    public void windowClosed(WindowEvent ev)      {}
    public void windowClosing(WindowEvent ev)     { onReleaseResidues(null); }
    public void windowDeactivated(WindowEvent ev) {}
    public void windowDeiconified(WindowEvent ev) {}
    public void windowIconified(WindowEvent ev)   {}
    public void windowOpened(WindowEvent ev)      {}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

