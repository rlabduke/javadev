// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;
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
import chiropraxis.kingtools.*;
import chiropraxis.rotarama.*;
import chiropraxis.sc.*;
import driftwood.moldb2.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>RnaRotTool</code> has not yet been documented.
*
* <p>Copyright (C) 2010 by Vincent B Chen. All rights reserved.
* <br>Begun on Mon Jan 04 15:59:57 EST 2010 
*/
public class RnaRotTool extends ModelingTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Residue                 targetRes1       = null;
    Residue                 targetRes2       = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RnaRotTool(ToolBox tb)
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
        try { Conformer.getInstance(); }
        catch(IOException ex) {}
        
        // Bring up model manager
        modelman.onShowDialog(null);
        
        // Helpful hint for users:
        this.services.setID("Ctrl-click, option-click, or middle-click a residue to rotate it");
        kCanvas.repaint(); // to make it show up!
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
        targetRes1 = this.getResidueNearest(modelman.getModel(), state,
          p.getX(), p.getY(), p.getZ());
        targetRes2 = targetRes1.getNext(modelman.getModel());
        if (targetRes1 != null && targetRes2 != null) {
          try {
            new RnaBackboneRotator(kMain, targetRes1, targetRes2, modelman);
          } catch(IOException ex) { 
            ex.printStackTrace(SoftLog.err); 
          }
        }
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
    
    public String toString() { return "RNA rotator"; }
//}}}
}//class

