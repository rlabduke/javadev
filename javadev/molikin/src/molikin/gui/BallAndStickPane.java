// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;
import molikin.*;
import molikin.crayons.*;
import molikin.logic.BallAndStickLogic;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.data.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>BallAndStickPane</code> is the UI "page" for (ball and) stick drawings
* of macromolecular structures.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  5 10:58:41 EDT 2005
*/
public class BallAndStickPane extends TablePane2 implements DrawingPane
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    CoordinateFile  coordFile;
    String          title;
    
    SelectorPane    selector;
    JCheckBox       cbProtein, cbNucleic, cbHets, cbIons, cbWater;
    JCheckBox       cbPseudoBB, cbBackbone, cbSidechains, cbHydrogens, cbDisulfides;
    JCheckBox       cbBallsOnCarbon, cbBallsOnAtoms;
    JComboBox       cmColorBy;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BallAndStickPane(CoordinateFile cfile, String title)
    {
        super();
        this.coordFile  = cfile;
        this.title      = title;
        
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        selector = new SelectorPane(coordFile);
        
        cbProtein   = new JCheckBox("protein", true);
        cbNucleic   = new JCheckBox("nucleic acids", true);
        cbHets      = new JCheckBox("hets (non-water)", true);
        cbIons      = new JCheckBox("metals/ions", true);
        cbWater     = new JCheckBox("water", false);
        
        cbPseudoBB      = new JCheckBox("C-alpha trace", true);
        cbBackbone      = new JCheckBox("backbone", false);
        cbSidechains    = new JCheckBox("sidechain", false);
        cbHydrogens     = new JCheckBox("hydrogens", false);
        cbDisulfides    = new JCheckBox("disulfides", false);
        
        cbBallsOnCarbon     = new JCheckBox("balls on C atoms too", false);
        cbBallsOnAtoms      = new JCheckBox("balls on N, O, P, etc.", false);
        
        cmColorBy   = new JComboBox(new Object[] {
            BallAndStickLogic.COLOR_BY_MC_SC,
            BallAndStickLogic.COLOR_BY_RES_TYPE,
            BallAndStickLogic.COLOR_BY_ELEMENT,
            BallAndStickLogic.COLOR_BY_B_FACTOR,
            BallAndStickLogic.COLOR_BY_OCCUPANCY
        });
        
        this.hfill(true).vfill(true).addCell(selector, 2, 1).newRow();
        this.weights(1,0).memorize();
        this.addCell(this.strut(0,6)).newRow();
        this.startSubtable(2,1);
            this.weights(0,0).memorize();
            this.addCell(new JLabel("Color by")).addCell(cmColorBy);
        this.endSubtable();
        this.addCell(this.strut(0,6)).newRow();
        this.addCell(cbProtein).addCell(cbPseudoBB).newRow();
        this.addCell(cbNucleic).addCell(cbBackbone).newRow();
        this.addCell(cbHets).addCell(cbSidechains).newRow();
        this.addCell(cbIons).addCell(cbDisulfides).newRow();
        this.addCell(cbWater).addCell(cbHydrogens).newRow();
        this.addCell(this.strut(0,6)).newRow();
        this.addCell(cbBallsOnAtoms).addCell(cbBallsOnCarbon).newRow();
        
        this.setBorder( BorderFactory.createTitledBorder(null, "Ball & stick") );
    }
//}}}

//{{{ toString, getSelectedModels, getSelectedChains
//##############################################################################
    public String toString()
    { return this.title; }
    
    /** As a Collection of Model objects. */
    public Collection getSelectedModels()
    { return selector.getSelectedModels(); }
    
    /** As a Collection of Strings representing chain IDs. */
    public Collection getSelectedChains()
    { return selector.getSelectedChains(); }
//}}}

//{{{ printKinemage
//##############################################################################
    /** Emits the kinemage (text) representation as selected by the user */
    public void printKinemage(PrintWriter out, Model m, String chainID, String bbColor)
    {
        Collection models = selector.getSelectedModels();
        if(!models.contains(m)) return;
        
        Collection chains = selector.getSelectedChains();
        if(!chains.contains(chainID)) return;
        
        Collection chainRes = m.getChain(chainID);
        if(chainRes == null) return;
        
        Set residues = selector.getSelectedResidues(chainRes);
        if(residues.size() == 0) return;
        
        BallAndStickLogic logic = new BallAndStickLogic();
        logic.doProtein         = this.cbProtein.isSelected();
        logic.doNucleic         = this.cbNucleic.isSelected();
        logic.doHets            = this.cbHets.isSelected();
        logic.doIons            = this.cbIons.isSelected();
        logic.doWater           = this.cbWater.isSelected();
        logic.doPseudoBB        = this.cbPseudoBB.isSelected();
        logic.doBackbone        = this.cbBackbone.isSelected();
        logic.doSidechains      = this.cbSidechains.isSelected();
        logic.doHydrogens       = this.cbHydrogens.isSelected();
        logic.doDisulfides      = this.cbDisulfides.isSelected();
        logic.doBallsOnCarbon   = this.cbBallsOnCarbon.isSelected();
        logic.doBallsOnAtoms    = this.cbBallsOnAtoms.isSelected();
        logic.colorBy           = this.cmColorBy.getSelectedItem();
        logic.printKinemage(out, m, residues, bbColor);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

