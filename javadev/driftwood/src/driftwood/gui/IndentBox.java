// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
//import driftwood.*;
//}}}
/**
* <code>IndentBox</code> is an ordinary horizontal Box
* which contains zero to two Components:
*   an optional rigid area to create an indent from the left edge
*   and a core component that this box is effectively a wrapper for.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May  6 15:24:44 EDT 2003
*/
public class IndentBox extends SwapBox
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    int             indent = 0;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public IndentBox(Component comp)
    {
        super(comp);
    }
//}}}

//{{{ get/setIndent
//##################################################################################################
    public int getIndent()
    { return indent; }
    
    public void setIndent(int i)
    {
        if(indent > 0) this.remove(0);
        this.indent = i;
        if(indent > 0) this.add(Box.createRigidArea(new Dimension(indent, 0)), 0);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

