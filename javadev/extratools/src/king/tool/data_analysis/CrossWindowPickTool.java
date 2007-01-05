// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.data_analysis;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.ref.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>CrossWindowPickTool</code> transmits the picked point ID
* to every instance of KiNG in the current VM, asking them to pick
* a point they can find with the same name.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Dec  3 08:42:34 EST 2003
*/
public class CrossWindowPickTool extends BasicTool
{
//{{{ Constants
    /** A List&lt;SoftReference&lt;CrossWindowPickTool&gt;&gt; */
    private static final ArrayList instanceList = new ArrayList();
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CrossWindowPickTool(ToolBox tb)
    {
        super(tb);
        
        synchronized(instanceList)
        {
            // Every instance of this class knows about the others
            instanceList.add( new SoftReference(this) );
        }
    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
        
        if(p != null)
        {
            signalCrossPick(p.getName());
        }
    }
//}}}

//{{{ signalCrossPick, tryCrossPick
//##############################################################################
    /**
    * Notifies all other instances of this class
    * to try picking a particular point ID.
    */
    void signalCrossPick(String pointID)
    {
        synchronized(instanceList)
        {
            for(Iterator iter = instanceList.iterator(); iter.hasNext(); )
            {
                SoftReference       ref     = (SoftReference)iter.next();
                CrossWindowPickTool other   = (CrossWindowPickTool)ref.get();
                
                // that instance has been GC'd
                if(other == null) iter.remove();
                // don't signal ourselves please
                else if(other == this) {}
                // else signal it to pick
                else other.tryCrossPick(pointID);
            }
        }
    }
    
    /**
    * Called when another instance picks a point.
    */
    void tryCrossPick(String pointID)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        for(KPoint p : KIterator.visiblePoints(kin))
        {
            if(p.getName().equals(pointID))
            {
                services.pick(p);
                break;
            }
        }
    }
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

//{{{ getToolPanel, getHelpURL/Anchor, toString, isAppletSafe
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
    
    public String toString() { return "Cross-window picking"; }
    
    /** This plugin is not applet-safe because it requires multiple KiNG windows open. */
    static public boolean isAppletSafe()
    {
        return false;
    }
//}}}
}//class

