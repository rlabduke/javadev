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
import javax.swing.tree.*;
//}}}
/**
 * <code>KSubgroup</code> is the KiNG implementation of a Mage subgroup.
 *
 * <p>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
 * <br>Begun on Wed Oct  2 12:10:01 EDT 2002
*/
public class KSubgroup extends AGE implements Cloneable
{
//{{{ Variable definitions
//##################################################################################################
    KGroup parent = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Constructor */
    public KSubgroup()
    {
        children = new ArrayList(10);
        setName(null);
    }

    /** Constructor */
    public KSubgroup(KGroup owner, String nm)
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
            KSubgroup x = (KSubgroup) super.clone(clonePoints);
            x.setName( x.getName() ); // tricks it into creating a new JCheckBox object
            
            // Deep copy of children
            x.children = new ArrayList();
            for(Iterator iter = this.children.iterator(); iter.hasNext(); )
            {
                KList child = (KList) iter.next();
                KList clone = (KList) child.clone(clonePoints);
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

//{{{ get/setOwner()
//##################################################################################################
    /** Determines the owner (parent) of this element */
    public AGE getOwner()
    { return parent; }
    /** Establishes the owner (parent) of this element */
    public void setOwner(AGE owner)
    {
        parent = (KGroup)owner;
    }
//}}}

//{{{ add()
//##################################################################################################
    /** Adds a child to this element */
    public void add(KList child)
    { children.add(child); }
//}}}

//{{{ MutableTreeNode functions -- insert()
//##################################################################################################
    public void insert(MutableTreeNode child, int index)
    {
        if(! (child instanceof KList)) throw new IllegalArgumentException("Subgroups can only contain lists!");
        
        if(index < 0 || index > children.size())    children.add(child);
        else                                        children.add(index, child);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

