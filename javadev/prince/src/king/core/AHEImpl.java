// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>AHEImpl</code> (Abstract Hierarchy Element implementation)
* provides the basic services used by AGEs and most KPoints.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 10:50:32 EDT 2002
*/
abstract public class AHEImpl<P extends AGE> implements AHE<P>
{
//{{{ Variable definitions
//##################################################################################################
    protected String name = "";
//}}}

//{{{ get/setName, toString
//##################################################################################################
    public String getName()
    { return name; }
    public void setName(String nm)
    { name = nm; }

    /** Gets the name of this element (same as <code>getName()</code>*/
    public String toString()
    { return getName(); }
//}}}

//{{{ getKinemage, getDepth, fireKinChanged
//##################################################################################################
    public Kinemage getKinemage()
    {
        AGE parent = getParent();
        if(parent == null)  return null;
        else                return parent.getKinemage();
    }
    
    public int getDepth()
    {
        int depth = 0;
        AGE parent = getParent();
        while(parent != null)
        {
            depth++;
            parent = parent.getParent();
        }
        return depth;
    }
    
    public void fireKinChanged(int eventFlags)
    {
        AGE parent = this.getParent();
        if(parent != null) parent.fireKinChanged(eventFlags);
    }
//}}}

//{{{ isVisible
//##################################################################################################
    public boolean isVisible()
    {
        AGE parent = getParent();
        if(parent == null)  return this.isOn();
        else                return this.isOn() && parent.isVisible();
    }
//}}}
    
//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

