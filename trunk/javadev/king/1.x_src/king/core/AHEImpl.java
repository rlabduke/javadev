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
* <p>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 10:50:32 EDT 2002
*/
abstract public class AHEImpl implements AHE
{
//{{{ Variable definitions
//##################################################################################################
    String name = "";
//}}}

//{{{ get/setName, toString
//##################################################################################################
    /** Gets the name of this element */
    public String getName()
    { return name; }
    /** Sets the name of this element */
    public void setName(String nm)
    { name = nm; }

    /** Gets the name of this element (same as <code>getName()</code>*/
    public String toString()
    { return getName(); }
//}}}

//{{{ getKinemage
//##################################################################################################
    /** Retrieves the Kinemage object holding this element, or null if none. */
    public Kinemage getKinemage()
    {
        AGE owner = getOwner();
        if(owner == null)   return null;
        else                return owner.getKinemage();
    }
//}}}

//{{{ isVisible, isTotallyOn
//##################################################################################################
    /** Indicates whether this element will actually be painted (i.e., is it and all its parents on?) */
    public boolean isVisible()
    { return (getOwner().isVisible() && isOn()); }
    /** Returns true iff this element is On, it's owner is On, it's owner's owner is On, and so on */
    public boolean isTotallyOn()
    {
        if(this.getOwner() == null) return this.isOn();
        else return (this.isOn() && this.getOwner().isTotallyOn());
    }
//}}}
    
//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

