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
import driftwood.data.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>NoePanel</code> provides a GUI interface to
* Brian Coggin's <code>noe-display</code> program.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Sep 23 11:03:07 EDT 2003
*/
public class NoePanel extends TablePane implements ActionListener, ChangeListener
{
//{{{ Constants
    static DecimalFormat df = new DecimalFormat("0.0###");
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain            kMain;
    ModelManager2       modelman;
    BgKinRunner         noePlotter  = null;
    
    AttentiveComboBox   cmDisplaySel;
    JTextField          tfRescaleSel;
    ExpSlider           slRescale;
    JComboBox           coColorBy;
    JComboBox           coDistCalc;
    JComboBox           coDistCorr;
    JCheckBox           cbEnumAmbig;
    JTextField          tfMoreFlags;
    
    Map                 mapColorBy;
    Map                 mapDistCalc;
    Map                 mapDistCorr;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public NoePanel(KingMain kmain, ModelManager2 modelman)
    {
        super();
        this.kMain      = kmain;
        this.modelman   = modelman;
        buildMaps();
        buildGUI();
    }
//}}}

//{{{ buildMaps
//##############################################################################
    private void buildMaps()
    {
        mapColorBy = new UberMap();
        mapColorBy.put("ratio (model dist/target dist)", "-cr");
        mapColorBy.put("NOE target distance", "-cd");
        mapColorBy.put("violations (model - target >= 0.5)", "-cv");
        
        mapDistCalc = new UberMap();
        mapDistCalc.put("geometric center", "-dc");
        mapDistCalc.put("r^-6 summation", "-ds");
        
        mapDistCorr = new UberMap();
        mapDistCorr.put("none", "");
        mapDistCorr.put("add", "+");
        mapDistCorr.put("remove", "-");
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        TablePane cp = this;
        
        slRescale = new ExpSlider(0.5, 2.0, 50);
        slRescale.setDouble(1.0);
        slRescale.setLabels(5, new DecimalFormat("0.#"));
        slRescale.addChangeListener(this);
        
        tfRescaleSel = new AttentiveTextField("*");
        tfRescaleSel.addActionListener(this);
        
        cmDisplaySel = new AttentiveComboBox(
            new Object[] { "{molten}", "within 5 of {center}", "all", "viol" });
        //cmDisplaySel.setEditable(true); -- redundant
        cmDisplaySel.addActionListener(this);
        
        coColorBy   = new JComboBox(mapColorBy.keySet().toArray());
        coColorBy.addActionListener(this);
        
        coDistCalc  = new JComboBox(mapDistCalc.keySet().toArray());
        coDistCalc.addActionListener(this);
        
        coDistCorr  = new JComboBox(mapDistCorr.keySet().toArray());
        coDistCorr.addActionListener(this);
        
        cbEnumAmbig = new JCheckBox("Enumerate ambiguous NOEs");
        cbEnumAmbig.addActionListener(this);
        
        tfMoreFlags = new AttentiveTextField("");
        tfMoreFlags.addActionListener(this);
        
        cp.save().insets(0).hfill(true).vfill(true).startSubtable(2,1);
            cp.save().weights(0,1).addCell(new JLabel("Color by:")).restore();
            cp.save().right().addCell(coColorBy).restore();
        cp.endSubtable().restore();
        cp.newRow();
        cp.save().weights(0,1).addCell(new JLabel("display:")).restore();
        cp.save().hfill(true).addCell(cmDisplaySel).restore();
        cp.newRow();
        cp.addCell(cbEnumAmbig,2,1);
        cp.newRow();
        cp.save().weights(0,1).addCell(new JLabel("rescale:")).restore();
        cp.save().hfill(true).addCell(tfRescaleSel).restore();
        cp.newRow();
        cp.save().hfill(true).addCell(slRescale,2,1).restore();
        cp.newRow();
        cp.addCell(cp.strut(0,4));
        cp.newRow();
        cp.save().insets(0).hfill(true).vfill(true).startSubtable(2,1);
            cp.addCell(new JLabel("Pseudoatom distance calc:"));
            cp.addCell(new JLabel("Corrections:"));
            cp.newRow();
            cp.save().hfill(true);
            cp.addCell(coDistCalc);
            cp.addCell(coDistCorr);
            cp.restore();
        cp.endSubtable().restore();
        cp.newRow();
        cp.save().insets(0).hfill(true).vfill(true).startSubtable(2,1);
            cp.save().weights(0,1).addCell(new JLabel("More flags:")).restore();
            cp.save().hfill(true).addCell(tfMoreFlags).restore();
        cp.endSubtable().restore();
    }
//}}}

//{{{ getNoePlotter, visualizeNOEs
//##############################################################################
    BgKinRunner getNoePlotter()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin != null && (noePlotter == null || !noePlotter.getKinemage().equals(kin)))
        {
            if(noePlotter != null) noePlotter.terminate(); // clean up the old one
            
            // Incomplete command line, will be completed in a moment
            noePlotter = new BgKinRunner(kMain, kin, "");
        }
        return noePlotter;
    }
    
    public void visualizeNOEs(Collection residues, File noeFile, String noeFormat)
    {
        try
        {
            BgKinRunner np = getNoePlotter();
            String optColor = mapColorBy.get(coColorBy.getSelectedItem()).toString();
            String optCalc  = mapDistCalc.get(coDistCalc.getSelectedItem()).toString();
            String optCorr  = mapDistCorr.get(coDistCorr.getSelectedItem()).toString();
            String optAmbig = cbEnumAmbig.isSelected() ? "-a" : "";
            String optExtra = tfMoreFlags.getText();
            String noeCmd = " -t -g 'Dynamic NOEs' -f  "+optColor+" "+optCalc+optCorr
                +" -n "+noeFormat+" -s '"+getDisplaySelection()+"'"
                +" -r "+df.format( slRescale.getDouble() )+" '"+getRescaleSelection()+"'"
                +" "+optAmbig+" "+optExtra+" '{pdbfile}' '"+noeFile.getCanonicalPath()+"'";
            
            String noeExe = noePlotter.findProgram("noe-display");
            noePlotter.setCommand(noeExe+noeCmd); // now complete cmd line
            np.requestRun(residues, modelman.getMoltenState(), modelman.getFrozenPDB());
        }
        catch(IOException ex)
        { ex.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ get{Display, Rescale}Selection
//##############################################################################
    String getDisplaySelection()
    {
        return cmDisplaySel.getText();
    }
    
    String getRescaleSelection()
    {
        return tfRescaleSel.getText();
    }
    
//}}}

//{{{ actionPerformed, stateChanged
//##############################################################################
    public void actionPerformed(ActionEvent ev)
    {
        modelman.requestStateRefresh();
    }
    
    public void stateChanged(ChangeEvent ev)
    {
        modelman.requestStateRefresh();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

