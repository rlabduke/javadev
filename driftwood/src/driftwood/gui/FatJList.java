// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>FatJList</code> is a Swing JList that can augment its width a bit,
* so it doesn't look so cramped.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb  2 14:52:37 EST 2004
*/
public class FatJList extends JList
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    int minWidth;
    int padding;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * The list will prefer to be in a scrollport that's as wide
    * as its preferred width plus padding, but will never be
    * less than minWidth wide.
    * minWidth may be set to zero if no minimum should be enforced.
    */
    public FatJList(int minWidth, int padding)
    {
        super();
        this.minWidth   = minWidth;
        this.padding    = padding;
    }
//}}}

//{{{ getPreferredScrollableViewportSize
//##############################################################################
    public Dimension getPreferredScrollableViewportSize()
    {
        Dimension d = super.getPreferredScrollableViewportSize();
        d.width += padding;
        if(d.width < minWidth)
            d.width = minWidth;
        return d;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

