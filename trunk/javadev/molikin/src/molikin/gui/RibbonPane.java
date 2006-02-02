// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;
import molikin.*;

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
    
    PrintWriter     out = null;
    RibbonPrinter   rp  = null;
    
    SelectorPane    selector;
    JCheckBox       cbProtein, cbNucleic;
    JCheckBox       cbUntwistRibbons;
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
            cbNucleic.setEnabled(false);
        
        cbUntwistRibbons = new JCheckBox("untwist ribbons", true);
        
        cmColorBy   = new JComboBox(new String[] {"secondary structure", "B factor", "occupancy"});
            cmColorBy.setEnabled(false);
        
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
        this.addCell(cbUntwistRibbons).skip().newRow();
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
        
        this.out = out;
        this.rp = new RibbonPrinter(out);
        rp.setCrayon(new ProteinSecStructCrayon(coordFile.getSecondaryStructure()));
        
        if(cbProtein.isSelected())  printProtein(m, residues, bbColor);
        if(cbNucleic.isSelected())  printNucAcid(m, residues, bbColor);
        
        this.out.flush();
        this.out = null;
        this.rp = null;
    }
//}}}

//{{{ printProtein
//##############################################################################
    void printProtein(Model model, Set selectedRes, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        ModelState      state   = model.getState();
        
        Ribbons ribbons = new Ribbons();
        Collection contigs = ribbons.getProteinContigs(selectedRes, state, resC);
        for(Iterator iter = contigs.iterator(); iter.hasNext(); )
        {
            Collection contig = (Collection) iter.next();
            if(contig.size() < 2) continue; // too small to use!
            GuidePoint[] guides = ribbons.makeProteinGuidepoints(contig, state);
            if(cbUntwistRibbons.isSelected()) ribbons.untwistRibbon(guides);
            rp.printGuidepoints(guides);
            out.println("@vectorlist {protein ribbon} color= "+bbColor+" master= {protein} master= {ribbon}");
            rp.printUniformFiveLine(guides, 4);
        }
    }
//}}}

//{{{ printNucAcid
//##############################################################################
    void printNucAcid(Model model, Set selectedRes, String bbColor)
    {
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

