// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.draw;
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
//}}}
/**
* <code>EditPropertiesTool</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Feb 28 15:17:36 EST 2003
*/
public class EditPropertiesTool extends BasicTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    GroupEditor     grEditor;
    PointEditor     ptEditor;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public EditPropertiesTool(ToolBox tb)
    {
        super(tb);
        grEditor = new GroupEditor(kMain, kMain.getTopWindow());
        ptEditor = new PointEditor(kMain);
    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
        if(p != null) ptEditor.editPoint(p);
    }
    
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
        if(p != null)
        {
            KList list = (KList)p.getOwner();
            if(list != null && grEditor.editList(list))
                kMain.notifyChange(KingMain.EM_EDIT_GROSS);
        }
    }
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return "#editprop-tool"; }
    
    public String toString() { return "Edit properties"; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

