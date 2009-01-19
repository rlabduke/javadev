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
* <code>PhiPsiWindow</code> aims to provide a simple mechanism for changing 
* backbone phi,psi of a PDB and getting back out a modified PDB.
*
* It is modeled after Ian's BackrubWindow class.
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Sun Jan 18 2009
*/
public class PhiPsiWindow implements Remodeler, ChangeListener, WindowListener
{
//{{{ Constants
    static final DecimalFormat df2 = new DecimalFormat("0.0");
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
    JDialog             dialog;
    
    Residue             ctrRes;
    Ramachandran        rama            = null;
    
    double              origPhiVal = Double.NaN, origPsiVal = Double.NaN;
    
    //GUI
    TablePane2          toolpane;
    AngleDial           phiDial, psiDial;
    JLabel              phiLabel, psiLabel, ramaLabel;
    JCheckBox           cbIdealizeSC;
    JCheckBox           rotUpstreamMC;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @throws IllegalArgumentException if the residue code isn't recognized
    */
    public PhiPsiWindow(KingMain kMain, Residue target, ModelManager2 mm)
    {
        this.kMain = kMain;
        this.kCanvas = kMain.getCanvas();
        this.modelman = mm;
        this.ctrRes = target;
        
        buildGUI(kMain.getTopWindow());
        
        try { initDials(); }
        catch (ResidueException re) { re.printStackTrace(); }
        catch (AtomException    ae) { ae.printStackTrace(); }
        
        // force loading of data table that will be used later
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) {}
        
        // May also throw IAEx:
        Collection residues = PhiPsiRotation.makeMobileGroup(modelman.getModel(), ctrRes, rotUpstreamMC.isSelected());
        
        modelman.registerTool(this, residues);
        stateChanged(null);
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI(Frame frame)
    {
        phiDial = new AngleDial();
        phiDial.addChangeListener(this);
        psiDial = new AngleDial();
        psiDial.addChangeListener(this);
        
        JButton btnRelease = new JButton(new ReflectiveAction("Finished", null, this, "onReleaseResidues"));
        JButton btnRotamer = new JButton(new ReflectiveAction("Rotate sidechain", null, this, "onRotateSidechain"));
        
        //headerLabels = new JLabel[] { new JLabel("phi/psi"), new JLabel("Ramachdrn") };
        phiLabel  = new JLabel("phi");
        psiLabel  = new JLabel("psi");
        ramaLabel = new JLabel();
        
        cbIdealizeSC = new JCheckBox(new ReflectiveAction("Idealize sidechain", null, this, "onToggleIdealSC"));
        cbIdealizeSC.setSelected(true);
        //cbIdealizeSC.setSelected(false); // 25 Oct 06: changed by mandate from JSR
        
        rotUpstreamMC = new JCheckBox(new ReflectiveAction("Rotate mainchain upstream", null, this, "onToggleUpstreamMC"));
        rotUpstreamMC.setSelected(false);
        
        toolpane = new TablePane2();
        toolpane.center();
        toolpane.addCell(phiDial);
        toolpane.addCell(psiDial);
        toolpane.newRow();
        
        toolpane.add(phiLabel);
        toolpane.add(psiLabel);
        toolpane.newRow();
        
        toolpane.addCell(TablePane2.strut(0,10));
        toolpane.newRow();
        toolpane.add(ramaLabel);
        toolpane.newRow();
        
        toolpane.newRow();
        toolpane.addCell(TablePane2.strut(0,10));
        toolpane.newRow();
        toolpane.startSubtable(4,1);
            toolpane.addCell(rotUpstreamMC);
        toolpane.endSubtable();
        toolpane.newRow();
        toolpane.startSubtable(4,1);
            toolpane.addCell(cbIdealizeSC);
        toolpane.endSubtable();
        toolpane.newRow();
        toolpane.startSubtable(4,1);
            toolpane.addCell(btnRelease);
            toolpane.addCell(btnRotamer);
        toolpane.endSubtable();
        
        // Assemble the dialog
        dialog = new JDialog(frame, "Tweak phi/psi: "+ctrRes.toString(), false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(this);
        dialog.setContentPane(toolpane);
        //dialog.setJMenuBar(menubar);
        dialog.pack();
        dialog.setVisible(true);
    }
//}}}

//{{{ initDials
//##################################################################################################
    /**
    * Set dials to starting phi,psi
    */
    public void initDials() throws ResidueException, AtomException
    {
        if (Double.isNaN(origPhiVal))
            origPhiVal = AminoAcid.getPhi(modelman.getModel(), ctrRes, modelman.getMoltenState());
        if (Double.isNaN(origPsiVal))
            origPsiVal = AminoAcid.getPsi(modelman.getModel(), ctrRes, modelman.getMoltenState());
        
        phiDial.setOrigDegrees(origPhiVal);
        psiDial.setOrigDegrees(origPsiVal);
        
        phiDial.setDegrees(origPhiVal);
        psiDial.setDegrees(origPsiVal);
    }
//}}}

//{{{ stateChanged, updateModelState
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
            boolean upstream = rotUpstreamMC.isSelected();
            boolean idealize = cbIdealizeSC.isSelected();
            Collection residues = PhiPsiRotation.makeMobileGroup(
                modelman.getModel(), ctrRes, upstream);
            /*for(Iterator i = residues.iterator(); i.hasNext(); )  System.err.println(
                (upstream ? "upstream" : "downstream")+" from "+ctrRes+" includes "+(Residue)i.next());*/
            
            // Rotate phi
            ModelState after = PhiPsiRotation.makeConformation(
                residues, before, phiDial.getDegrees() - origPhiVal, true,  upstream, idealize);
            
            // Rotate psi
            after = PhiPsiRotation.makeConformation(
                residues, after,  psiDial.getDegrees() - origPsiVal, false, upstream, idealize);
            
            return after;
        }
        catch(AtomException ex)
        {
            ex.printStackTrace();
            return before;
        }
    }
//}}}

//{{{ onToggleIdealSC, onToggleUpstreamMC
//##################################################################################################
    // target of reflection
    public void onToggleIdealSC(ActionEvent ev)
    { stateChanged(null); }

    // target of reflection
    public void onToggleUpstreamMC(ActionEvent ev)
    {
        try { initDials(); }
        catch (ResidueException re) { re.printStackTrace(); }
        catch (AtomException    ae) { ae.printStackTrace(); }
        
        // new molten state
        modelman.unregisterTool(this);
        Collection residues = PhiPsiRotation.makeMobileGroup(modelman.getModel(), ctrRes, rotUpstreamMC.isSelected());
        modelman.registerTool(this, residues);
        
        stateChanged(null);
    }
//}}}

//{{{ updateLabels
//##################################################################################################
    void updateLabels()
    {
        // Make color normal again
        ramaLabel.setForeground(normalColor);
        
        if(ctrRes == null)   ramaLabel.setText("");
        else
        {
            Model model = modelman.getModel();
            ModelState modelState = modelman.getMoltenState();
            
            try
            {
                if(rama == null)
                    ramaLabel.setText("Ramachdrn: [no data]");
                else if(rama.isOutlier(model, ctrRes, modelState))
                {
                    ramaLabel.setText("Ramachdrn: OUTLIER");
                    ramaLabel.setForeground(alertColor);
                }
                else if(rama.rawScore(model, ctrRes, modelState) > 0.02)
                    ramaLabel.setText("Ramachdrn: favored");
                else
                    ramaLabel.setText("Ramachdrn: allowed");
            }
            catch(ResidueException ex) { ramaLabel.setText("Ramachdrn: [no phi,psi]"); }
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

