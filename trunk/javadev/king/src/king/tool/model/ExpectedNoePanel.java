// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.model;
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
import driftwood.util.SoftLog;
//}}}
/**
* <code>ExpectedNoePanel</code> provides a GUI interface to
* Brian Coggin's <code>noe-display</code> program.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Sep 23 11:03:07 EDT 2003
*/
public class ExpectedNoePanel extends TablePane implements ActionListener, ChangeListener
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.0###");
    static final String DEFAULT_MAX_LENGTH = "4.5";
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain            kMain;
    ModelManager2       modelman;
    BgKinRunner         noePlotter  = null;
    
    //JTextField          tfDisplaySel;
    AttentiveComboBox   cmDisplaySel;
    JTextField          tfMaxLength;
    JCheckBox           cbShowAll;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ExpectedNoePanel(KingMain kmain, ModelManager2 modelman)
    {
        super();
        this.kMain      = kmain;
        this.modelman   = modelman;
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        TablePane cp = this;
        
        //tfDisplaySel = new AttentiveTextField("{molten}");
        //tfDisplaySel.addActionListener(this);
        
        cmDisplaySel = new AttentiveComboBox(
            new Object[] { "{molten}", "within 5 of {center}", "all", "viol" });
        //cmDisplaySel.setEditable(true); -- redundant
        cmDisplaySel.addActionListener(this);
        
        tfMaxLength = new AttentiveTextField(DEFAULT_MAX_LENGTH);
        tfMaxLength.addActionListener(this);
        
        cbShowAll = new JCheckBox("Show all predicted NOEs", true);
        cbShowAll.addActionListener(this);
        
        JButton bnSimulate = new JButton(new ReflectiveAction("Simulate experiment(s)...", null, this, "onSimulateExperiment"));
        
        cp.save().weights(0,1).addCell(new JLabel("Display:")).restore();
        //cp.save().hfill(true).addCell(tfDisplaySel).restore();
        cp.save().hfill(true).addCell(cmDisplaySel).restore();
        cp.newRow();
        cp.save().weights(0,1).addCell(new JLabel("Max length:")).restore();
        cp.save().hfill(true).addCell(tfMaxLength).restore();
        cp.newRow();
        cp.save().hfill(true).addCell(bnSimulate,2,1).restore();
        cp.newRow();
        cp.addCell(cbShowAll,2,1);
        
        refreshGUI();
    }
//}}}

//{{{ refreshGUI, onUpdateVis
//##################################################################################################
    /** One stop shopping to ensure the GUI reflects the current conditions. */
    public void refreshGUI()
    {
        if(modelman.noeFile == null)
        {
            cbShowAll.setEnabled(false);
            cbShowAll.setSelected(true);
        }
        else
            cbShowAll.setEnabled(true);
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
    
    /** noeFile may be null, if the user hasn't loaded one yet. */
    public void visualizeNOEs(Collection residues, File noeFile, String noeFormat)
    {
        try
        {
            String all = (cbShowAll.isSelected() ? "a" : "");
            String len = getMaxLength();
            tfMaxLength.setText(len);

            String filepath;
            if(noeFile == null) { filepath = ""; all = "a"; }
            else filepath = "'"+noeFile.getCanonicalPath()+"'";
            
            BgKinRunner np = getNoePlotter();
            String noeCmd = " -t -g 'Expected NOEs' -f  "
                +" -n "+noeFormat
                +" -e"+all+"n "+len+" '"+getDisplaySelection()+"'"
                +" '{pdbfile}' "+filepath;
            
            String noeExe = noePlotter.findProgram("noe-display");
            noePlotter.setCommand(noeExe+noeCmd); // now complete cmd line
            np.requestRun(residues, modelman.getMoltenState(), modelman.getFrozenPDB());
        }
        catch(IOException ex)
        { ex.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ getDisplaySelection, getMaxLength
//##############################################################################
    String getDisplaySelection()
    {
        //return tfDisplaySel.getText();
        return cmDisplaySel.getText();
    }
    
    String getMaxLength()
    {
        try
        {
            double len = Double.parseDouble(tfMaxLength.getText());
            if(len < 1.0)   len = 1.0;
            if(len > 20.0)  len = 20.0;
            return df.format(len);
        }
        catch(NumberFormatException ex)
        {
            return DEFAULT_MAX_LENGTH;
        }
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

//{{{ onSimulateExperiment
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSimulateExperiment(ActionEvent ev)
    {
        JOptionPane.showMessageDialog(kMain.getTopWindow(),
            "This feature has not been implemented yet.",
            "Sorry!", JOptionPane.ERROR_MESSAGE);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

