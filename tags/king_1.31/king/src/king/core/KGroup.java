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
import javax.swing.*;
import javax.swing.tree.*;
import driftwood.gui.*;
//}}}
/**
 * <code>KGroup</code> is the KiNG implementation of a Mage group.
 *
 * <p>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
 * <br>Begun on Wed Oct  2 12:10:01 EDT 2002
*/
public class KGroup extends AGE implements Cloneable
{
//{{{ Variable definitions
//##################################################################################################
    Kinemage parent     = null;
    boolean animate     = false;
    boolean animate2    = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Constructor */
    public KGroup()
    {
        children = new ArrayList(10);
        setName(null);
    }

    /** Constructor */
    public KGroup(Kinemage owner, String nm)
    {
        children = new ArrayList(10);
        setOwner(owner);
        setName(nm);
    }
//}}}

//{{{ clone
//##################################################################################################
    /**
    * Creates a copy of this group and all its children.
    * @param clonePoints whether to clone even the individual points,
    *   or whether we should use instance= at the list level instead.
    */
    public Object clone(boolean clonePoints)
    {
        try
        {
            KGroup x = (KGroup) super.clone(clonePoints);
            x.setName( x.getName() ); // tricks it into creating a new JCheckBox object
            
            // Deep copy of children
            x.children = new ArrayList();
            for(Iterator iter = this.children.iterator(); iter.hasNext(); )
            {
                KSubgroup child = (KSubgroup) iter.next();
                KSubgroup clone = (KSubgroup) child.clone(clonePoints);
                clone.setOwner(x);
                x.add(clone);
            }
            
            // Semi-deep copy of masters, which just contains Strings
            if(this.masters != null) x.masters = new ArrayList(this.masters);
            
            return x;
        }
        catch(CloneNotSupportedException ex)
        { throw new Error("Clone failed in cloneable object"); }
    }
//}}}

//{{{ setName()
//##################################################################################################
    /** Sets the name of this element */
    public void setName(String nm)
    {
        super.setName(nm);
        
        StringBuffer buttonName = new StringBuffer();
        if(this.isAnimate())                        buttonName.append("*");
        if(this.is2Animate())                       buttonName.append("%");
        if(this.isAnimate() || this.is2Animate())   buttonName.append(" ");
        buttonName.append(nm);
        
        cbox.setText(buttonName.toString());
    }
//}}}

//{{{ get/setOwner()
//##################################################################################################
    /** Determines the owner (parent) of this element */
    public AGE getOwner()
    { return parent; }
    /** Establishes the owner (parent) of this element */
    public void setOwner(AGE owner)
    {
        parent = (Kinemage)owner;
    }
//}}}

//{{{ is/set(2)Animate()
//##################################################################################################
    /** Checks to see if this group should be animated. */
    public boolean isAnimate() { return animate; }
    /** Sets the animate property. Animations still must be regenerated manually! */ 
    public void setAnimate(boolean b) { animate = b; setName(getName()); }
    /** Checks to see if this group should be animated. */
    public boolean is2Animate() { return animate2; }
    /** Sets the animate property. Animations still must be regenerated manually! */ 
    public void set2Animate(boolean b) { animate2 = b; setName(getName()); }
//}}}

//{{{ add()
//##################################################################################################
    /** Adds a child to this element */
    public void add(KSubgroup child)
    { children.add(child); }
//}}}

//{{{ MutableTreeNode functions -- insert()
//##################################################################################################
    public void insert(MutableTreeNode child, int index)
    {
        if(! (child instanceof KSubgroup)) throw new IllegalArgumentException("Groups can only contain subgroups!");
        
        if(index < 0 || index > children.size())    children.add(child);
        else                                        children.add(index, child);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

