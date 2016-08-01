// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;
import molikin.*;
import molikin.logic.RibbonLogic;

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
* <code>RibbonPane</code> is the UI "page" for ribbon drawings
* of macromolecular structures.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  5 10:58:41 EDT 2005
*/
public class RibbonPane extends TablePane2 implements DrawingPane
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    CoordinateFile  coordFile;
    String          title;
    
    SelectorPane    selector;
    JCheckBox       cbProtein, cbNucleic;
    JCheckBox       cbUntwistRibbons, cbDnaStyle;
    JComboBox       cmColorBy;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RibbonPane(CoordinateFile cfile, String title)
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
        
        cbUntwistRibbons    = new JCheckBox("untwist ribbons", true);
        cbDnaStyle          = new JCheckBox("DNA-style, not RNA", false);
        
        cmColorBy   = new JComboBox(new Object[] {
            RibbonLogic.COLOR_BY_SEC_STRUCT,
            RibbonLogic.COLOR_BY_RAINBOW
        });
        
        this.hfill(true).vfill(true).addCell(selector, 2, 1).newRow();
        this.weights(1,0).memorize();
        this.addCell(this.strut(0,6)).newRow();
        this.startSubtable(2,1);
            this.weights(0,0).memorize();
            this.addCell(new JLabel("Color by")).addCell(cmColorBy);
        this.endSubtable();
        this.addCell(this.strut(0,6)).newRow();
        this.addCell(cbProtein).skip().newRow();
        this.addCell(cbNucleic).skip().newRow();
        this.addCell(this.strut(0,6)).newRow();
        //this.addCell(cbDnaStyle).addCell(cbUntwistRibbons).newRow();
        this.addCell(cbDnaStyle).newRow();
        this.addCell(this.strut(0,6)).newRow();
        
        this.setBorder( BorderFactory.createTitledBorder(null, "Ribbons") );
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
        
        RibbonLogic logic = new RibbonLogic();
        logic.secondaryStructure    = coordFile.getSecondaryStructure();
        logic.doProtein             = cbProtein.isSelected();
        logic.doNucleic             = cbNucleic.isSelected();
        logic.doUntwistRibbons      = cbUntwistRibbons.isSelected();
        logic.doDnaStyle            = cbDnaStyle.isSelected();
        logic.colorBy               = this.cmColorBy.getSelectedItem();
        logic.printKinemage(out, m, residues, bbColor);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

