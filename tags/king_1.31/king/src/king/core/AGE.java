// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.*;
import driftwood.gui.*;
import driftwood.r3.*;
//}}}
/**
 * <code>AGE</code> (Abstract Grouping Element) is the basis for all groups, subgroups, lists, kinemages, etc.
 *
 * <p>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
 * <br>Begun on Wed Oct  2 10:50:36 EDT 2002
*/
abstract public class AGE extends AHE implements MutableTreeNode
{
//{{{ Variable definitions
//##################################################################################################
    static final Border cboxBorder = BorderFactory.createEmptyBorder(1,2,1,2);
    
    public java.util.List children = null;
    public ArrayList   masters     = null;
    
    boolean     on          = true;
    JCheckBox   cbox        = null;
    boolean     nobutton    = false;
    boolean     dominant    = false;
    boolean     recessiveon = false;
    boolean     lens        = false;
//}}}

//{{{ clone
//##################################################################################################
    /**
    * Creates a deep copy of this AGE and all its children,
    * including make full copies of all the points.
    * Not all AGEs implement Cloneable, so this operation could fail.
    */
    public Object clone() throws CloneNotSupportedException
    { return clone(true); }
    
    /**
    * Creates a copy of this AGE and all its children.
    * Not all AGEs implement Cloneable, so this operation could fail.
    * @param clonePoints whether to clone even the individual points,
    *   or whether we should use instance= at the list level instead.
    */
    public Object clone(boolean clonePoints) throws CloneNotSupportedException
    { return super.clone(); }
//}}}

//{{{ setName, iterator
//##################################################################################################
    /** Sets the name of this element */
    public void setName(String nm)
    {
        super.setName(nm);
        cbox = new JCheckBox(new ReflectiveAction(getName(), null, this, "cboxHit"));
        cbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbox.setBorder(cboxBorder);
        cbox.setSelected(on);
    }

    /** Returns an iterator over the children of this element. All children will be AHE's. */
    public ListIterator iterator()
    { return children.listIterator(); }
//}}}

//{{{ is/setOn, (set)hasButton, is/setDominant, is/setRecessiveOn, is/setLens
//##################################################################################################
    /** Indicates whether this element will paint itself, given the chance */
    public boolean isOn()
    { return on; }
    /** Sets the painting status of this element */
    public void setOn(boolean paint)
    {
        on = paint;
        cbox.setSelected(on);
    }
    
    /** Indicates whether this element would display a button, given the chance */
    public boolean hasButton()
    { return !nobutton; }
    /** Sets whether this element would display a button, given the chance */
    public void setHasButton(boolean b)
    { nobutton = !b; }
    
    /** Indicates whether this element supresses buttons of elements below it */
    public boolean isDominant()
    { return dominant; }
    /** Sets whether this element supresses buttons of elements below it */
    public void setDominant(boolean b)
    { dominant = b; }

    /** Indicates whether this element supresses buttons of elements below it WHEN OFF */
    public boolean isRecessiveOn()
    { return recessiveon; }
    /** Sets whether this element supresses buttons of elements below it WHEN OFF */
    public void setRecessiveOn(boolean b)
    { recessiveon = b; }
    
    /**
    * Indicates whether or not the points under this element
    * should be hidden if they are more than a certain distance
    * from the current center of viewing.
    * The name comes from the visualization community, where
    * this function is likened to a magnifying glass.
    */
    public boolean isLens()
    { return lens; }
    public void setLens(boolean b)
    { lens = b; }
//}}}

//{{{ cboxHit(), notifyCboxHit(), buildButtons()
//##################################################################################################
    /** Called when the associated checkbox is turned on/off */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void cboxHit(ActionEvent ev)
    {
        on = cbox.isSelected();
        notifyCboxHit();
    }
    
    /** Propagates notice upward that a checkbox was turned on/off */
    public void notifyCboxHit()
    {
        getOwner().notifyCboxHit();
    }

    /** Builds a grouping of Mage-style on/off buttons in the specified container. */
    protected void buildButtons(Container cont)
    {
        if(hasButton()) cont.add(cbox);
        if(!isDominant())
        {
            AlignBox subbox = new AlignBox(BoxLayout.Y_AXIS);
            subbox.setAlignmentX(Component.LEFT_ALIGNMENT);
            for(Iterator iter = children.iterator(); iter.hasNext(); )
            { ((AGE)iter.next()).buildButtons(subbox); }
            
            IndentBox ibox;
            if(isRecessiveOn()) ibox = new FoldingBox(cbox, subbox);
            else                ibox = new IndentBox(subbox);
            ibox.setIndent(8);
            cont.add(ibox);
        }
    }
//}}}
    
//{{{ calcBoundingBox() and calcRadiusSq()
//##################################################################################################
    /**
    * Gets a bounding box for the current model.
    * @param bound the first 6 elements get set to { minX, minY, minZ, maxX, maxY, maxZ }.
    * Should be called with { +inf, +inf, +inf, -inf, -inf, -inf }
    */
    public void calcBoundingBox(float[] bound)
    {
        Iterator iter = children.iterator();
        while(iter.hasNext()) ((AHE)iter.next()).calcBoundingBox(bound);
    }
    
    /**
    * Gets the square of the radius of this model from the specified center.
    * @param center an array with the x, y,  and z coordinates of the center
    * @return the square of the radius of this element, centered at center
    */
    public float calcRadiusSq(float[] center)
    {
        float max = 0f;
        Iterator iter = children.iterator();
        while(iter.hasNext()) max = Math.max(max, ((AHE)iter.next()).calcRadiusSq(center));
        return max;
    }
//}}}
    
//{{{ signalTransform
//##################################################################################################
    /**
    * A call to this method indicates the subscriber
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
    *
    * <p>This method will be called in response to TransformSignal.signalTransform().
    *
    * @param engine     the Engine object describing the
    *   dimensions and properties of the space to be painted.
    * @param xform      the Transform to apply.
    *   The subscriber must not modify the original Transform it
    *   receives! Subscibers may, however, copy and modify the
    *   Transform(s) they pass to internal substructures.
    */
    public void signalTransform(Engine engine, Transform xform)
    {
        // If the button is off, this will never be rendered
        if(!isOn()) return;
        
        // Not using iterators speeds this up by a few tens of ms
        int i, end_i;
        end_i = children.size();
        for(i = 0; i < end_i; i++) ((AHE)children.get(i)).signalTransform(engine, xform);
    }
//}}}

//{{{ addMaster, removeMaster, hasMaster, masterIterator
//##################################################################################################
    /** Makes the named master control this AGE */
    public void addMaster(String masterName)
    {
        if(masters == null) masters = new ArrayList(5);
        if(!masters.contains(masterName))
        {
            masters.add(masterName);
        }
    }
    
    /** Stops the named master from controlling this AGE. No action if it wasn't ever added. */
    public void removeMaster(String masterName)
    {
        if(masters == null) return;
        int i;
        if((i = masters.lastIndexOf(masterName)) != -1)
        {
            masters.remove(i);
        }
    }
    
    /** Returns true iff the named master controls this AGE */
    public boolean hasMaster(String masterName)
    {
        if(masters == null) return false;
        else return masters.contains(masterName);
    }
    
    /** Returns an iterator over this AGE's masters (as Strings), or null if there are none. */
    public Iterator masterIterator()
    {
        if(masters == null) return null;
        else return masters.iterator();
    }
//}}}

//{{{ MutableTreeNode functions
//##################################################################################################
    // Required to be a TreeNode
    public Enumeration children()                 { return Collections.enumeration(children); }
    public boolean     getAllowsChildren()        { return true; }
    public TreeNode    getChildAt(int childIndex) { return (TreeNode)children.get(childIndex); }
    public int         getChildCount()            { return children.size(); }
    public int         getIndex(TreeNode node)    { return children.indexOf(node); }
    public TreeNode    getParent()                { return getOwner(); }
    public boolean     isLeaf()                   { return false; }
    
    // Required to be a MutableTreeNode
    // public void insert(MutableTreeNode child, int index)
    public void remove(int index)                       { children.remove(index); }
    public void remove(MutableTreeNode node)            { children.remove(node); }
    public void removeFromParent()                      { getOwner().remove(this); }
    public void setParent(MutableTreeNode newParent)    { setOwner((AGE)newParent); }
    public void setUserObject(Object obj)               {}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

