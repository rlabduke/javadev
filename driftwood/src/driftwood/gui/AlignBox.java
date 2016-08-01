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
* <code>AlignBox</code> implements get and set for alignment properties,
* which Boxes don't have in Java 1.3 because they're not JComponents.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May  6 15:24:44 EDT 2003
*/
public class AlignBox extends Box
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    private float       alignX          = 0;
    private float       alignY          = 0;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a horizontal box.
    */
    public AlignBox()
    {
        this(BoxLayout.X_AXIS);
    }
    
    public AlignBox(int axis)
    {
        super(axis);
    }
//}}}

//{{{ get/set{AlignmentX, AlignmentY}
//##################################################################################################
    /** Default value is 0.0 */
    public float getAlignmentX()
    { return alignX; }
    public void setAlignmentX(float a)
    { alignX = a; }

    /** Default value is 0.0 */
    public float getAlignmentY()
    { return alignY; }
    public void setAlignmentY(float a)
    { alignY = a; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

