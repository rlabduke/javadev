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
import driftwood.util.SoftLog;
//}}}
/**
* <code>ScRotTool</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Sep 24 13:29:16 EDT 2003
*/
public class ScRotTool extends ModelingTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Residue                 targetRes       = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ScRotTool(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ start/stop/reset
//##################################################################################################
    public void start()
    {
        super.start();

        // force loading of data tables that will be used later
        try { Rotamer.getInstance(); }
        catch(IOException ex) {}
        
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
            try {
                new SidechainRotator(kMain.getTopWindow(), targetRes, modelman);
            } catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        }
    }
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
    { return "#scrot-tool"; }
    
    public String toString() { return "Sidechain rotator"; }
//}}}
}//class

