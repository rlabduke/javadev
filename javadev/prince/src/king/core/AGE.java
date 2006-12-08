// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import driftwood.r3.*;
//}}}
/**
* <code>AGE</code> (Abstract Grouping Element) is the basis for
*  all groups, subgroups, lists, kinemages -- but does not include points.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 10:50:36 EDT 2002
*/
abstract public class AGE<P extends AGE, C extends AHE> extends AHEImpl<P> implements Iterable<C>
{
//{{{ Constants
    protected static final int FLAG_ON              = (1<<0);
    protected static final int FLAG_NOBUTTON        = (1<<1);
    protected static final int FLAG_DOMINANT        = (1<<2);
    protected static final int FLAG_COLLAPSIBLE     = (1<<3);
    protected static final int FLAG_LENS            = (1<<4);
    
    protected static final int FLAG_ANIMATE         = (1<<8);
    protected static final int FLAG_2ANIMATE        = (1<<9);
    protected static final int FLAG_NOHILITE        = (1<<10);
//}}}

//{{{ Variable definitions
//##################################################################################################
    protected P                     parent      = null;
    protected ArrayList<C>          children    = new ArrayList<C>();
    protected Collection<String>    masters     = null;
    protected Transform             transform   = null;
    protected int                   flags       = FLAG_ON;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Constructor */
    public AGE()
    {
    }
//}}}

//{{{ clone, get/setParent
//##################################################################################################
    /**
    * Creates a deep copy of this AGE and all its children,
    * including make full copies of all the points.
    * Not all AGEs implement Cloneable, so this operation could fail.
    */
    public AGE<P,C> clone() throws CloneNotSupportedException
    { return clone(true); }
    
    /**
    * Creates a copy of this AGE and all its children.
    * Not all AGEs implement Cloneable, so this operation could fail.
    * @param clonePoints whether to clone even the individual points,
    *   or whether we should use instance= at the list level instead.
    */
    public AGE<P,C> clone(boolean clonePoints) throws CloneNotSupportedException
    { return (AGE<P,C>) super.clone(); }
    
    public P getParent()
    { return parent; }
    
    public void setParent(P owner)
    {
        parent = owner;
    }
//}}}

//{{{ add, replace, clear, getChildren, iterator
//##################################################################################################
    /** Adds a child to this element */
    public void add(C child)
    {
        if(child.getParent() == null)
            child.setParent(this);
        children.add(child);
        if(child instanceof AGE)    fireKinChanged(CHANGE_TREE_CONTENTS);
        else                        fireKinChanged(CHANGE_POINT_CONTENTS);
    }
    
    /**
    * Replaces oldChild with newChild, or performs an add()
    * of newChild if oldChild couldn't be found as part of the kinemage.
    * This is very useful for things like Mage's Remote Update.
    * @return the group actually replaced, or null for none
    */
    public C replace(C oldChild, C newChild)
    {
        int idx = children.indexOf(oldChild);
        if(idx == -1)
        {
            add(newChild);
            return null;
        }
        else
        {
            if(newChild.getParent() == null)
                newChild.setParent(this);
            return children.set(idx, newChild);
        }
    }
    
    /** Removes all children from this element */
    public void clear()
    { children.clear(); }
    
    /**
    * Returns the actual object that holds the children.
    * If you modify it, you must call fireKinChanged() yourself.
    */
    public ArrayList<C> getChildren()
    { return children; }
    
    public Iterator<C> iterator()
    { return getChildren().iterator(); }
//}}}

//{{{ setName, is/setOn, (set)hasButton
//##################################################################################################
    public void setName(String nm)
    {
        super.setName(nm);
        fireKinChanged(CHANGE_TREE_PROPERTIES);
    }
    
    /** Indicates whether this element will paint itself, given the chance */
    public boolean isOn()
    { return (flags & FLAG_ON) == FLAG_ON; }
    /** Sets the painting status of this element */
    public void setOn(boolean b)
    {
        int oldFlags = flags;
        if(b)   flags |= FLAG_ON;
        else    flags &= ~FLAG_ON;
        if(flags != oldFlags) fireKinChanged(CHANGE_TREE_ON_OFF);
    }
    
    /** Indicates whether this element would display a button, given the chance */
    public boolean hasButton()
    { return !((flags & FLAG_NOBUTTON) == FLAG_NOBUTTON); }
    /** Sets whether this element would display a button, given the chance */
    public void setHasButton(boolean b)
    {
        int oldFlags = flags;
        if(!b)  flags |= FLAG_NOBUTTON;
        else    flags &= ~FLAG_NOBUTTON;
        if(flags != oldFlags) fireKinChanged(CHANGE_TREE_PROPERTIES);
    }
//}}}

//{{{ is/setDominant, is/setCollapsible, is/setLens
//##################################################################################################
    /** Indicates whether this element supresses buttons of elements below it */
    public boolean isDominant()
    { return (flags & FLAG_DOMINANT) == FLAG_DOMINANT; }
    /** Sets whether this element supresses buttons of elements below it */
    public void setDominant(boolean b)
    {
        int oldFlags = flags;
        if(b)   flags |= FLAG_DOMINANT;
        else    flags &= ~FLAG_DOMINANT;
        if(flags != oldFlags) fireKinChanged(CHANGE_TREE_PROPERTIES);
    }

    /** Indicates whether this element supresses buttons of elements below it WHEN OFF */
    public boolean isCollapsible()
    { return (flags & FLAG_COLLAPSIBLE) == FLAG_COLLAPSIBLE; }
    /** Sets whether this element supresses buttons of elements below it WHEN OFF */
    public void setCollapsible(boolean b)
    {
        int oldFlags = flags;
        if(b)   flags |= FLAG_COLLAPSIBLE;
        else    flags &= ~FLAG_COLLAPSIBLE;
        if(flags != oldFlags) fireKinChanged(CHANGE_TREE_PROPERTIES);
    }
    
    /**
    * Indicates whether or not the points under this element
    * should be hidden if they are more than a certain distance
    * from the current center of viewing.
    * The name comes from the visualization community, where
    * this function is likened to a magnifying glass.
    */
    public boolean isLens()
    { return (flags & FLAG_LENS) == FLAG_LENS; }
    public void setLens(boolean b)
    {
        int oldFlags = flags;
        if(b)   flags |= FLAG_LENS;
        else    flags &= ~FLAG_LENS;
        if(flags != oldFlags) fireKinChanged(FLAG_LENS);
    }
//}}}

//{{{ doTransform, get/setTransform, calcBoundingBox, calcRadiusSq
//##################################################################################################
    public void doTransform(Engine engine, Transform xform)
    {
        // If the button is off, this will never be rendered
        if(!isOn()) return;
        
        if(this.transform != null)
            xform = new Transform().like(this.transform).append(xform);
        
        // Not using iterators speeds this up by a few tens of ms
        // Java 1.5 can do this automatically for Lists that implement RandomAccess
        for(C child : children) child.doTransform(engine, xform);
    }
    
    /** Returns the extra Transform applied to points below this element (may be null) */
    public Transform getTransform()
    { return transform; }
    
    /** Sets the extra Transform applied to points below this element (may be null) */
    public void setTransform(Transform t)
    { this.transform = t; }

    public void calcBoundingBox(float[] bound)
    {
        for(C child : children) child.calcBoundingBox(bound);
    }
    
    public float calcRadiusSq(float[] center)
    {
        float max = 0f;
        for(C child : children) max = Math.max(max, child.calcRadiusSq(center));
        return max;
    }
//}}}
    
//{{{ addMaster, removeMaster, getMasters
//##################################################################################################
    /** Makes the named master control this AGE */
    public void addMaster(String masterName)
    {
        if(masters == null) masters = new ArrayList<String>(5);
        if(!masters.contains(masterName))
        {
            masters.add(masterName);
            fireKinChanged(CHANGE_TREE_MASTERS);
        }
    }
    
    /** Stops the named master from controlling this AGE. No action if it wasn't ever added. */
    public void removeMaster(String masterName)
    {
        if(masters == null) return;
        if(masters.contains(masterName))
        {
            masters.remove(masterName);
            fireKinChanged(CHANGE_TREE_MASTERS);
        }
    }
    
    /**
    * Returns the actual object that holds the masters.
    * If you modify it, you must call fireKinChanged() yourself.
    */
    public Collection<String> getMasters()
    {
        if(masters == null) return Collections.emptySet();
        else                return masters;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

