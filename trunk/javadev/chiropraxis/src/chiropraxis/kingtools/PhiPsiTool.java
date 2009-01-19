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
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
import chiropraxis.mc.*;
import chiropraxis.rotarama.*;
//}}}
/**
* <code>PhiPsiTool</code> aims to provide a simple mechanism for changing 
* backbone phi,psi of a PDB and getting back out a modified PDB.
*
* It is modeled after Ian's BackrubTool class.
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Sun Jan 18 2009
*/
public class PhiPsiTool extends ModelingTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PhiPsiTool(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return null; }
    
    public String getHelpAnchor()
    { return "#phipsi-tool"; }
    
    public String toString() { return "Tweak phi/psi tool"; }
//}}}

//{{{ start/stop/reset
//##################################################################################################
    public void start()
    {
        super.start();
        
        // force loading of data tables that will be used later
        try { Ramachandran.getInstance(); }
        catch(IOException ex) {}
        
        // Bring up model manager
        modelman.onShowDialog(null);
        
        // Helpful hint for users:
        this.services.setID("Ctrl-click, option-click, or middle-click the residue to change phi,psi");
        kCanvas.repaint(); // to make it show up!
    }
//}}}

//{{{ c_click
//##################################################################################################
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null)
        {
        }
        else
        {
            Model model = modelman.getModel();
            ModelState state = modelman.getMoltenState();
            Residue ctrRes = this.getResidueNearest(model, state,
                p.getX(), p.getY(), p.getZ());
            try { new PhiPsiWindow(kMain, ctrRes, modelman); }
            catch(IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    ctrRes+"doesn't have neighbors in the same chain.",
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

