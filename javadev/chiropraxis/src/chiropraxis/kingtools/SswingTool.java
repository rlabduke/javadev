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
import chiropraxis.rotarama.*;
import chiropraxis.sc.*;
import driftwood.moldb2.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>SswingTool</code> allows the user to run SSWING on a sidechain
* to find its optimal rotamer state, using contact dots and electron density.
*
* <p>Copyright (C) 2004 by XXX. All rights reserved.
* <br>Begun on July 20 13:29:16 EDT 2004
*/
public class SswingTool extends ModelingTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Residue             targetRes;
    File                ccp4MapFile = null;
    
    SuffixFileFilter    mapFilter;
    JFileChooser        mapChooser;
    JDialog             dialog;
    JTextField          sswingCmdField;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SswingTool(ToolBox tb)
    {
        super(tb);
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        // File chooser for CCP4 maps
        mapFilter = new SuffixFileFilter("CCP4 electron density maps");
        mapFilter.addSuffix(".ccp4");
        mapFilter.addSuffix(".mbk");
        mapFilter.addSuffix(".map");
        mapChooser = new JFileChooser();
        mapChooser.addChoosableFileFilter(mapFilter);
        mapChooser.setFileFilter(mapFilter);
        String currdir = System.getProperty("user.dir");
        if(currdir != null) mapChooser.setCurrentDirectory(new File(currdir));
        
        sswingCmdField = new JTextField(40);
        JButton mapBtn = new JButton(new ReflectiveAction("Choose new map...", null, this, "onChooseMap"));
        JButton runBtn = new JButton(new ReflectiveAction("Run Sswing", null, this, "onRunSswing"));
        JButton cancelBtn = new JButton(new ReflectiveAction("Don't run", null, this, "onCancel"));
        
        TablePane2 cp = new TablePane2();
        cp.hfill(true).addCell(sswingCmdField, 4, 1);
        cp.newRow();
        cp.weights(0,0).memorize();
        cp.addCell(mapBtn);
        cp.weights(1,0).addCell(Box.createGlue());
        cp.addCell(cancelBtn).addCell(runBtn);
        
        dialog = new JDialog(kMain.getTopWindow(), this.toString(), true); // modal
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setContentPane(cp);
        dialog.pack();
    }
//}}}

//{{{ start/stop/reset
//##################################################################################################
    public void start()
    {
        super.start();

        // force loading of data tables that will be used later
//        try { Rotamer.getInstance(); }
//        catch(IOException ex) {}
        
        // Bring up model manager
        modelman.onShowDialog(null);
    }
//}}}

//{{{ c_click
//##############################################################################
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p != null)
        {
            ModelState state = modelman.getMoltenState();
            targetRes = this.getResidueNearest(modelman.getModel(), state,
                p.getOrigX(), p.getOrigY(), p.getOrigZ());
            
            // Check for map file
            if(ccp4MapFile == null) onChooseMap(null);
            if(ccp4MapFile == null) return; // cancel if we have no map
            
            try
            {
                String cmd = makeSswingCmdLine();
                sswingCmdField.setText(cmd);
                dialog.setVisible(true); // hangs here until dialog closed
            }
            catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        }
    }
//}}}

//{{{ makeSswingCmdLine
//##############################################################################
    String makeSswingCmdLine() throws IOException
    {
        // XXX unneeded -- sswing needs auxillary scripts that have to be in the PATH too
        String sswingExe = SswingRunner.findProgram(kMain, "sswing");
        String sswingOption = "-f -s ";
        if(targetRes.getChain() != ' ')
            sswingOption = sswingOption+"-c "+targetRes.getChain();
        String cmd = sswingExe+" "+sswingOption+" "+
            "'"+modelman.getFrozenPDB().getCanonicalPath()+"' "+
            targetRes.getSequenceNumber()+" "+targetRes.getName()+" "+
            "'"+ccp4MapFile.getCanonicalPath()+"'";
        return cmd;
    }
//}}}

//{{{ onCancel, onRunSswing, onChooseMap
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCancel(ActionEvent ev)
    {
        dialog.setVisible(false);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRunSswing(ActionEvent ev)
    {
        dialog.setVisible(false);
        String cmd = sswingCmdField.getText();
        System.out.println(cmd+"...\n");
        SswingRunner sswingRunner = new SswingRunner(kMain, modelman, targetRes, cmd); // starts thread
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onChooseMap(ActionEvent ev)
    {
        // Open the new file
        if(JFileChooser.APPROVE_OPTION == mapChooser.showOpenDialog(kMain.getTopWindow()))
        {
            ccp4MapFile = mapChooser.getSelectedFile();
            // Rewrite cmd line to reflect new map file name
            try
            {
                String cmd = makeSswingCmdLine();
                sswingCmdField.setText(cmd);
            }
            catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return null; }
    
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this tool. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#navigate-tool" (or null)
    */
    public String getHelpAnchor()
    { return null; }
    
    public String toString() { return "Sswing"; }
//}}}
}//class

