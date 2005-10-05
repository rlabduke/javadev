// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
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
public class BallAndStickPane extends TablePane2
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    CoordinateFile  coordFile;
    
    SelectorPane    selector;
    JCheckBox       cbProtein, cbNucleic, cbHets, cbIons, cbWater;
    JCheckBox       cbPseudoBB, cbBackbone, cbSidechains, cbHydrogens;
    JCheckBox       cbBallsOnCarbon, cbBallsOnNoncarbon;
    JComboBox       cmColorBy;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BallAndStickPane(CoordinateFile cfile)
    {
        super();
        this.coordFile  = cfile;
        
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
        cbWater     = new JCheckBox("water", true);
        
        cbPseudoBB      = new JCheckBox("C-alpha trace", true);
        cbBackbone      = new JCheckBox("backbone", true);
        cbSidechains    = new JCheckBox("sidechain", true);
        cbHydrogens     = new JCheckBox("hydrogens", true);
        
        cbBallsOnCarbon     = new JCheckBox("balls on C atoms", false);
        cbBallsOnNoncarbon  = new JCheckBox("balls on N, O, P, etc.", false);
        
        cmColorBy   = new JComboBox(new String[] {"backbone / sidechain", "element (half bonds)", "B factor", "occupancy"});
        
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
        this.addCell(cbIons).addCell(cbHydrogens).newRow();
        this.addCell(cbWater).skip().newRow();
        this.addCell(this.strut(0,6)).newRow();
        this.addCell(cbBallsOnCarbon).addCell(cbBallsOnNoncarbon).newRow();
        
        this.setBorder( BorderFactory.createTitledBorder(null, "Ball & stick") );
    }
//}}}

//{{{ TEMPORARY - getSelectedResidues
//##############################################################################
    public Set getSelectedResidues(Model m)
    { return selector.getSelectedResidues(m); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

