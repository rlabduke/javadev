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
import driftwood.r3.*;
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
    protected int moview = 0;
    protected KGroup instance = null; // the (sub)group that this one is an instance= {xxx} of (usually null)
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

//{{{ get/set(Ultimate)Instance
//##################################################################################################
    /** Sets which (sub)group this one is an instance of, or null for none. */
    public void setInstance(KGroup inst)
    {
        this.instance = inst;
        fireKinChanged(CHANGE_POINT_CONTENTS);
        // Is this the right argument ^ here?...  -DAK 121023
    }
    
    /** Gets which (sub)group this one is an instance of, or null for none. */
    public KGroup getInstance()
    { return this.instance; }
    
    /** In case of a chain of instance-of relationships, finds the (sub)group at the end of the chain. */
    public KGroup getUltimateInstance()
    {
        KGroup inst = this.getInstance();
        if(inst == null) return null;
        else
        {
            while(inst.getInstance() != null) inst = inst.getInstance();
            return inst;
        }
    }
//}}}

//{{{ doTransform
//##################################################################################################
    // Added this method and the above ...Instance() methods 
    // so instance= works for groups and subgroups, not just lists.
    // DAK 121024
    public void doTransform(Engine engine, Transform xform)
    {
        // If the button is off, this will never be rendered
        if(!isOn()) return;
        
        // If we're an instance of someone else, transform those lists/(sub)groups too
        KGroup inst = getUltimateInstance();
        if(inst != null)
        {
            for(AGE child : inst.getChildren()) child.doTransform(engine, xform);
        }
        
        // Not using iterators speeds this up by a few tens of ms
        // Java 1.5 can do this automatically for Lists that implement RandomAccess
        for(AGE child : children) child.doTransform(engine, xform);
    }
//}}}

//{{{ is/set(2)Animate, is/setSelect
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
    
    /** Checks to see if this group can be selected-by-color. */
    public boolean isSelect()
    { return (flags & FLAG_SELECT) == FLAG_SELECT; }
    
    /** Sets the "select" property. */ 
    public void setSelect(boolean b)
    {
        int oldFlags = flags;
        if(b)   flags |= FLAG_SELECT;
        else    flags &= ~FLAG_SELECT;
        if(flags != oldFlags) fireKinChanged(CHANGE_TREE_PROPERTIES);
    }
//}}}

  //{{{ is/setMoview
  /** Checks to see if this group has a moview **/
  public boolean isMoview() {
    return (moview > 0);
  }
  /** Sets a moview for this group.  Should also have animate set, and be greater than 0 **/
  public void setMoview(int i) {
    moview = i;
  }
  
  public int getMoview() {
    return moview;
  }
  //}}}

  //{{{ isDeepestGroup
  public boolean isDeepestGroup() {
    ArrayList children = getChildren();
    Iterator iter = children.iterator();
    while (iter.hasNext()) {
      AGE child = (AGE) iter.next();
      if (child instanceof KGroup) return false;
    }
    return true;
  }
  //}}}

  
//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

