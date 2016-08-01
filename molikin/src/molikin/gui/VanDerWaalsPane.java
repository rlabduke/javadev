// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;
import molikin.*;
import molikin.crayons.*;
import molikin.logic.VanDerWaalsLogic;

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
* <code>VanDerWaalsPane</code> is the UI "page" for VDW drawings
* of macromolecular structures.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  5 10:58:41 EDT 2005
*/
public class VanDerWaalsPane extends TablePane2 implements DrawingPane
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    CoordinateFile  coordFile;
    String          title;
    
    SelectorPane    selector;
    JCheckBox       cbProtein, cbNucleic, cbHets, cbMetals, cbWater;
    JCheckBox       cbMainchain, cbSidechains, cbHydrogens, cbUseSpheres;
    JComboBox       cmColorBy;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public VanDerWaalsPane(CoordinateFile cfile, String title)
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
        cbMetals    = new JCheckBox("metals/ions", true);
        cbWater     = new JCheckBox("water", false);
        
        cbMainchain     = new JCheckBox("mainchain", true);
        cbSidechains    = new JCheckBox("sidechains", true);
        cbHydrogens     = new JCheckBox("hydrogens", true);
        
        cbUseSpheres    = new JCheckBox("use spheres?", false);
        
        cmColorBy   = new JComboBox(new Object[] {
            VanDerWaalsLogic.COLOR_BY_ELEMENT,
            VanDerWaalsLogic.COLOR_BY_RES_TYPE,
            VanDerWaalsLogic.COLOR_BY_B_FACTOR,
            VanDerWaalsLogic.COLOR_BY_OCCUPANCY
        });
        
        this.hfill(true).vfill(true).addCell(selector, 2, 1).newRow();
        this.weights(1,0).memorize();
        this.addCell(this.strut(0,6)).newRow();
        this.startSubtable(2,1);
            this.weights(0,0).memorize();
            this.addCell(new JLabel("Color by")).addCell(cmColorBy);
        this.endSubtable();
        this.addCell(this.strut(0,6)).newRow();
        this.addCell(cbProtein).addCell(cbMainchain).newRow();
        this.addCell(cbNucleic).addCell(cbSidechains).newRow();
        this.addCell(cbHets).addCell(cbHydrogens).newRow();
        this.addCell(cbMetals).skip().newRow();
        this.addCell(cbWater).addCell(cbUseSpheres).newRow();
        
        this.setBorder( BorderFactory.createTitledBorder(null, "van der Waals") );
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
    public void printKinemage(PrintWriter out, Model m, String chainID, String pdbId, String bbColor)
    {
        Collection models = selector.getSelectedModels();
        if(!models.contains(m)) return;
        
        Collection chains = selector.getSelectedChains();
        if(!chains.contains(chainID)) return;
        
        Collection chainRes = m.getChain(chainID);
        if(chainRes == null) return;
        
        Set residues = selector.getSelectedResidues(chainRes);
        if(residues.size() == 0) return;
        
        VanDerWaalsLogic logic = new VanDerWaalsLogic();
        logic.doProtein     = this.cbProtein.isSelected();
        logic.doNucleic     = this.cbNucleic.isSelected();
        logic.doHets        = this.cbHets.isSelected();
        logic.doMetals      = this.cbMetals.isSelected();
        logic.doWater       = this.cbWater.isSelected();
        logic.doMainchain   = this.cbMainchain.isSelected();
        logic.doSidechains  = this.cbSidechains.isSelected();
        logic.doHydrogens   = this.cbHydrogens.isSelected();
        logic.doUseSpheres  = this.cbUseSpheres.isSelected();
        logic.colorBy       = this.cmColorBy.getSelectedItem();
        logic.printKinemage(out, m, residues, pdbId, bbColor);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

