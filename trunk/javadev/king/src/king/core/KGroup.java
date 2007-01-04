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
//}}}
/**
* <code>KGroup</code> is the KiNG implementation of a kinemage group, subgroup, etc.
* Lists, on the other hand, are implemented as KList.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:10:01 EDT 2002
*/
public class KGroup extends AGE<AGE,AGE> implements Cloneable
{
//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Creates a new KGroup with an empty name. */
    public KGroup()
    { this(""); }

    /** Creates a new KGroup with the specified name. */
    public KGroup(String nm)
    {
        super();
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
    public KGroup clone(boolean clonePoints)
    {
        try
        {
            KGroup x = (KGroup) super.clone(clonePoints);
            
            // Deep copy of children
            x.children = new ArrayList();
            for(AGE child : children)
            {
                AGE clone = child.clone(clonePoints);
                x.add(clone);
                clone.setParent(x); // because it was already set to something else!
            }
            
            // Semi-deep copy of masters, which just contains Strings
            if(this.masters != null) x.masters = new ArrayList<String>(this.masters);
            
            return x;
        }
        catch(CloneNotSupportedException ex)
        { throw new Error("Clone failed in cloneable object"); }
    }
//}}}

//{{{ is/set(2)Animate
//##################################################################################################
    /** Checks to see if this group should be animated. */
    public boolean isAnimate()
    { return (flags & FLAG_ANIMATE) == FLAG_ANIMATE; }
    
    /** Sets the animate property. Animations still must be regenerated manually! */ 
    public void setAnimate(boolean b)
    {
        int oldFlags = flags;
        if(b)   flags |= FLAG_ANIMATE;
        else    flags &= ~FLAG_ANIMATE;
        if(flags != oldFlags) fireKinChanged(CHANGE_TREE_PROPERTIES);
    }
    
    /** Checks to see if this group should be animated. */
    public boolean is2Animate()
    { return (flags & FLAG_2ANIMATE) == FLAG_2ANIMATE; }
    
    /** Sets the animate property. Animations still must be regenerated manually! */ 
    public void set2Animate(boolean b)
    {
        int oldFlags = flags;
        if(b)   flags |= FLAG_2ANIMATE;
        else    flags &= ~FLAG_2ANIMATE;
        if(flags != oldFlags) fireKinChanged(CHANGE_TREE_PROPERTIES);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

