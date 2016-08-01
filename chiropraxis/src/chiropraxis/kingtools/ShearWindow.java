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
* <code>ShearWindow</code> is an implementation of the "shear" move
* described for helices in Davis/Richardson 2006 and more generally in 
* Smith/Kortemme 2008.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Fri Jul 30 2010
*/
public class ShearWindow implements Remodeler, ChangeListener, WindowListener
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
    Window              dialog;
    
    Residue             anchor1, ctrRes1, ctrRes2, anchor2;
    KList               anchorList;
    Ramachandran        rama            = null;
    TauByPhiPsi         tauscorer       = null;
    
    //GUI
    TablePane2          toolpane;
    AngleDial           shearDial, pept1Dial, pept2Dial, pept3Dial;
    JLabel[]            headerLabels, res1Labels, res2Labels, res3Labels, res4Labels;
    JCheckBox           cbIdealizeSC;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @throws IllegalArgumentException if the residue code isn't recognized
    */
    public ShearWindow(KingMain kMain, Residue target, ModelManager2 mm)
    {
        this.kMain = kMain;
        this.kCanvas = kMain.getCanvas();
        this.modelman = mm;
        this.ctrRes1 = target; // arbitrary choice btw ctrRes1 & ctrRes2
        this.anchorList = new KList(KList.BALL);
        anchorList.setColor( KPalette.peach );
        
        // force loading of data tables that will be used later
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) {}
        try { tauscorer = TauByPhiPsi.getInstance(); }
        catch(IOException ex) {}
        
        Model model = modelman.getModel();
        ModelState state = modelman.getMoltenState();
        this.anchor1 = ctrRes1.getPrev(model);
        this.ctrRes2 = ctrRes1.getNext(model);
        if(anchor1 != null && ctrRes2 != null)
        {
            this.anchor2 = ctrRes2.getNext(model);
            if(anchor2 != null)
            {
                //System.err.println("from "+anchor1+" to "+anchor2);
                
                buildGUI(kMain.getTopWindow());
                
                markAnchor(anchor1, state);
                markAnchor(anchor2, state);
                //updateLabels(); -- done by stateChanged()
                
                // May also throw IAEx:
                Collection residues = CaShear.makeMobileGroup(modelman.getModel(), anchor1, anchor2);
                //System.err.println(residues.size()+" residues before register tool:");
                //for(Iterator iter = residues.iterator(); iter.hasNext(); ) System.err.println((Residue)iter.next());
                modelman.registerTool(this, residues);
                
                stateChanged(null);
            }
            else
            {
                dialog.dispose();
                throw new IllegalArgumentException("Can't find enough neighbor residues for "+target);
            }
        }
        else
        {
            dialog.dispose();
            throw new IllegalArgumentException("Can't find enough neighbor residues for "+target);
        }
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI(Frame frame)
    {
        shearDial = new AngleDial();
        shearDial.addChangeListener(this);
        pept1Dial = new AngleDial();
        pept1Dial.addChangeListener(this);
        pept2Dial = new AngleDial();
        pept2Dial.addChangeListener(this);
        pept3Dial = new AngleDial();
        pept3Dial.addChangeListener(this);
        
        JButton btnRelease = new JButton(new ReflectiveAction("Finished", null, this, "onReleaseResidues"));
        JButton btnRotamer = new JButton(new ReflectiveAction("Rotate sidechain", null, this, "onRotateSidechain"));
        
        headerLabels = new JLabel[] { new JLabel("Residue"), new JLabel("Tau dev"), new JLabel("Karplus"), new JLabel("Ramachdrn"), new JLabel("phi,psi"), new JLabel("CA-C sig"), new JLabel("N-CA sig") };
        res1Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        res2Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        res3Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        res4Labels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };
        
        cbIdealizeSC = new JCheckBox(new ReflectiveAction("Idealize sidechains", null, this, "onToggleIdealSC"));
        cbIdealizeSC.setSelected(false); // 25 Oct 06: changed by mandate from JSR
        
        toolpane = new TablePane2();
        
        toolpane.skip();
        toolpane.skip();
        toolpane.addCell(shearDial);
        toolpane.skip();
        toolpane.skip();
        toolpane.newRow();
        
        toolpane.skip();
        toolpane.skip();
        toolpane.addCell(new JLabel("primary shear"));
        toolpane.skip();
        toolpane.skip();
        toolpane.newRow();
        
        toolpane.skip();
        toolpane.addCell(pept1Dial);
        toolpane.addCell(pept2Dial);
        toolpane.addCell(pept3Dial);
        toolpane.skip();
        toolpane.newRow();
        
        toolpane.skip();
        toolpane.addCell(new JLabel("1st peptide"));
        toolpane.addCell(new JLabel("2nd peptide"));
        toolpane.addCell(new JLabel("3rd peptide"));
        toolpane.skip();
        toolpane.newRow();
        
        toolpane.addCell(TablePane2.strut(0,10));
        toolpane.newRow();
        
        for(int i = 0; i < headerLabels.length; i++)
        {
            /*if(i == 2) continue; // skip Karplus' phi/psi-dependent tau deviation*/
            toolpane.add(headerLabels[i]);
            toolpane.add(res1Labels[i]);
            toolpane.add(res2Labels[i]);
            toolpane.add(res3Labels[i]);
            toolpane.add(res4Labels[i]);
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
        
        // Assemble the dialog
        if(kMain.getPrefs().getBoolean("minimizableTools"))
        {
            JFrame fm = new JFrame("Shear: "+anchor1.toString()+" to "+anchor2.toString());
            fm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            fm.setContentPane(toolpane);
            dialog = fm;
        }
        else
        {
            JDialog dial = new JDialog(frame, "Shear: "+anchor1.toString()+" to "+anchor2.toString(), false);
            dial.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dial.setContentPane(toolpane);
            dialog = dial;
        }
        dialog.addWindowListener(this);
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
            Collection residues = CaShear.makeMobileGroup(modelman.getModel(), anchor1, anchor2);
            boolean idealize = cbIdealizeSC.isSelected();
            
            // Major rotation
            ModelState after = CaShear.makeConformation(
                residues, before, shearDial.getDegrees(), idealize);
            
            // Peptide rotations
            Residue[] res        = (Residue[]) residues.toArray(new Residue[residues.size()]);
            double[]  angles     = new double[]  { pept1Dial.getDegrees(), pept2Dial.getDegrees(), pept3Dial.getDegrees() };
            boolean[] idealizeSC = new boolean[] { idealize, idealize, idealize, idealize };
            after = CaShear.twistPeptides(res, after, angles, idealizeSC);
            
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
        updateLabels(ctrRes1, res2Labels);
        updateLabels(ctrRes2, res3Labels);
        updateLabels(anchor2, res4Labels);
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
            
            // Tau dev (non-phi,psi-dependent)
            try
            {
                double taudev = AminoAcid.getTauDeviation(r, modelState);
                l[1].setText(df1.format(taudev));
                if(Math.abs(taudev) >= 3.0) l[1].setForeground(alertColor);
            }
            catch(AtomException ex) { l[1].setText("-?-"); }
            
            // Phi,psi, Ramachandran, & phi,psi-dependent tau dev
            try
            {
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
            catch(AtomException ex)    { l[2].setText("-?-"); l[3].setText("[no phi,psi]"); l[4].setText("? , ?"); }
            catch(ResidueException ex) { l[2].setText("-?-"); l[3].setText("[no phi,psi]"); l[4].setText("? , ?"); }
            
            // Bond length devs
            try
            {
                double sigmas = this.getLengthDeviation(model, r, modelState, "N--CA");
                l[6].setText(df1.format(sigmas));
                if(Math.abs(sigmas) >= 4.0) l[6].setForeground(alertColor);
            }
            catch(AtomException ex) { l[6].setText("-?-"); }
            try
            {
                double sigmas = this.getLengthDeviation(model, r, modelState, "CA--C");
                l[5].setText(df1.format(sigmas));
                if(Math.abs(sigmas) >= 4.0) l[5].setForeground(alertColor);
            }
            catch(AtomException ex) { l[5].setText("-?-"); }
        }
        
        dialog.pack();
    }
//}}}

//{{{ getLengthDeviation
//##################################################################################################
    /**
    * A dead-simple implementation of Dangle's Engh & Huber bond length values.
    * Separates out Gly vs. Pro vs. other properly, & returns deviation in sigmas.
    * Doesn't seem worth importing all of Dangle just to do this!
    */
    double getLengthDeviation(Model model, Residue r, ModelState modelState, String whichLength) throws AtomException
    {
        // From Dangle:
        //
        // for GLY     distance    N--CA       _N__, _CA_      ideal 1.456 0.015
        // for PRO     distance    N--CA       _N__, _CA_      ideal 1.468 0.017
        //             distance    N--CA       _N__, _CA_      ideal 1.459 0.020
        //                                                     
        // for GLY     distance    CA--C       _CA_, _C__      ideal 1.514 0.016
        // for PRO     distance    CA--C       _CA_, _C__      ideal 1.524 0.020
        //             distance    CA--C       _CA_, _C__      ideal 1.525 0.026
        
        double expected, sigma, actual;
        if(whichLength.equals("N--CA"))
        {
            if     (r.getName().equals("GLY")) { expected = 1.456; sigma = 0.015; }
            else if(r.getName().equals("PRO")) { expected = 1.468; sigma = 0.017; }
            else                               { expected = 1.459; sigma = 0.020; }
            AtomState n  = modelState.get(r.getAtom(" N  "));
            AtomState ca = modelState.get(r.getAtom(" CA "));
            actual = Triple.distance(n, ca);
        }
        else if(whichLength.equals("CA--C"))
        {
            if     (r.getName().equals("GLY")) { expected = 1.514; sigma = 0.016; }
            else if(r.getName().equals("PRO")) { expected = 1.524; sigma = 0.020; }
            else                               { expected = 1.525; sigma = 0.026; }
            AtomState ca = modelState.get(r.getAtom(" CA "));
            AtomState c  = modelState.get(r.getAtom(" C  "));
            actual = Triple.distance(ca, c);
        }
        else throw new IllegalArgumentException("Invalid bond length specifier: "+whichLength);
        return (actual - expected) / sigma;
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
            modelman.addUserMod("Refit backbone of "+anchor1.toString()+" to "+anchor2.toString());
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
            new SidechainRotator(kMain, ctrRes1, modelman);
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

