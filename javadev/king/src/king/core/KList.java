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
import javax.swing.tree.*;
import driftwood.r3.*;
//}}}
/**
* <code>KList</code> implements the concept of a list in a kinemage.
*
* <p>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:37:31 EDT 2002
*/
public class KList extends AGE implements Cloneable
{
//{{{ Constants
    public static final String  UNKNOWN  = "";
    public static final String  VECTOR   = "vector";
    public static final String  DOT      = "dot";
    public static final String  MARK     = "mark";
    public static final String  LABEL    = "label";
    public static final String  TRIANGLE = "triangle";
    public static final String  RIBBON   = "ribbon";
    public static final String  BALL     = "ball";
    public static final String  SPHERE   = "sphere";
    public static final String  ARROW    = "arrow";

    public static final int     NOHILITE = 0x00000001;  // no highlight on balls
//}}}

//{{{ Variable definitions
//##################################################################################################
    KSubgroup   parent      = null;
    KList       instance    = null;     // the list that this one is an instance= {xxx} of, or null

    public String type = UNKNOWN;       // type of object represented by this list
    
    public KPaint   color   = KPalette.defaultColor;
    public int      alpha   = 255;      // 255 = opaque, 0 = fully transparent
    public float    radius  = 0.2f;     // seems to be default in Mage; also used for arrow tine length (radius=)
    public int      width   = 2;
    public int      flags   = 0;        // nohighlight for balls, style for markers, etc
    Object          clipMode = null;    // null for default, else some object key
    int             dimension = 3;      // for high-dimensional kinemages
    
    // Parameters used for arrowlists; see ArrowPoint for explanation
    // of tine PERPendicular and PARallel components.
    float   angle       = 20f;
    float   tinePerp    = (float)(radius * Math.sin(Math.toRadians(angle)));
    float   tinePar     = (float)(radius * Math.cos(Math.toRadians(angle)));
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Constructor */
    public KList()
    {
        children = new ArrayList(20);
        setName(null);
    }

    /** Constructor */
    public KList(KSubgroup owner, String nm)
    {
        children = new ArrayList(20);
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
            KList x = (KList) super.clone(clonePoints);
            x.setName( x.getName() ); // tricks it into creating a new JCheckBox object
            x.children = new ArrayList();
            
            if(clonePoints)
            {
                // Deep copy of children from original source
                KList orig = this;
                while(orig.getInstance() != null) orig = orig.getInstance();
                
                KPoint prev = null;
                for(Iterator iter = orig.children.iterator(); iter.hasNext(); )
                {
                    KPoint child = (KPoint) iter.next();
                    KPoint clone = (KPoint) child.clone();
                    clone.setOwner(x);
                    x.add(clone);
                    
                    // Everything has been deep copied; we just need
                    // to correct the linked-list pointers for
                    // VectorPoints and TrianglePoints.
                    if(clone.getPrev() != null)
                        clone.setPrev(prev);
                    prev = clone;
                }
            }
            else // we'll use instance= to fake it!
            {
                if(this.getInstance() == null)
                    x.setInstance(this);
                else
                    x.setInstance(this.getInstance());
            }
            
            // Semi-deep copy of masters, which just contains Strings
            if(this.masters != null) x.masters = new ArrayList(this.masters);
            
            return x;
        }
        catch(CloneNotSupportedException ex)
        { throw new Error("Clone failed in cloneable object"); }
    }
//}}}

//{{{ get/setOwner, get/setInstance
//##################################################################################################
    /** Determines the owner (parent) of this element */
    public AGE getOwner()
    { return parent; }
    /** Establishes the owner (parent) of this element */
    public void setOwner(AGE owner)
    {
        parent = (KSubgroup)owner;
    }
    
    /** Sets which list this one is an instance of, or null for none. */
    public void setInstance(KList inst)
    { this.instance = inst; }
    /** Gets which list this one is an instance of, or null for none. */
    public KList getInstance()
    { return this.instance; }
//}}}

//{{{ get/set{Type, Color, Width, Radius, Angle, Style, ClipMode}
//##################################################################################################
    /** Determines the type of points held by this list */
    public String getType()
    { return type; }
    /** Establishes the type of points held by this list */
    public void setType(String t)
    {
        type = t;
    }
    
    /** Determines the default color of points held by this list */
    public KPaint getColor()
    { return color; }
    /** Establishes the default color of points held by this list */
    public void setColor(KPaint c)
    { color = c; }
    
    /** Determines the default width of points held by this list */
    public int getWidth()
    { return width; }
    /** Establishes the default width of points held by this list */
    public void setWidth(int w)
    {
        if(w > 7)       width = 7;
        else if(w < 1)  width = 1;
        else            width = w;
    }
    
    public float getRadius()
    { return radius; }
    public void setRadius(float r)
    {
        radius      = r;
        tinePerp    = (float)(radius * Math.sin(Math.toRadians(angle)));
        tinePar     = (float)(radius * Math.cos(Math.toRadians(angle)));
    }
    
    /** For use with ArrowPoint */
    public float getAngle()
    { return angle; }
    public void setAngle(float a)
    {
        angle       = a;
        tinePerp    = (float)(radius * Math.sin(Math.toRadians(angle)));
        tinePar     = (float)(radius * Math.cos(Math.toRadians(angle)));
    }
    
    /** For use with MarkerPoint */
    public int getStyle()
    { return flags; }
    public void setStyle(int s)
    { flags = s; }
    
    /** Gets the clipping mode key for this list. Usually null. See Engine.chooseClipMode(). */
    public Object getClipMode()
    { return clipMode; }
    /** Sets the clipping mode for this list. */
    public void setClipMode(Object key)
    { this.clipMode = key; }
    
    /** Returns the nominal number of coordinates per point in this list. */
    public int getDimension() { return this.dimension; }
    /** Sets the nominal number of coordinates per point in this list. */
    public void setDimension(int d) { this.dimension = d; }
//}}}

//{{{ add, clear
//##################################################################################################
    /** Adds a child to this element */
    public void add(KPoint child)
    { children.add(child); }
    
    /** Removes all children from this element */
    public void clear()
    { children.clear(); }
//}}}

//{{{ buildButtons()
//##################################################################################################
    protected void buildButtons(Container cont)
    {
        if(hasButton()) cont.add(cbox);
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
        
        int i, end_i;
        
        if(this.clipMode != null) engine.chooseClipMode(this.clipMode); // set alt clipping

        // If we're an instance of someone else, transform those points too
        if(instance != null)
        {
            engine.setActingParent(this);
            end_i = instance.children.size();
            for(i = 0; i < end_i; i++) ((KPoint)instance.children.get(i)).signalTransform(engine, xform, engine.zoom3D);
        }
        
        // Not using iterators speeds this up by a few tens of ms
        engine.setActingParent(null);
        end_i = children.size();
        for(i = 0; i < end_i; i++) ((KPoint)children.get(i)).signalTransform(engine, xform, engine.zoom3D);

        if(this.clipMode != null) engine.chooseClipMode(null); // reset to default
    }
//}}}

//{{{ MutableTreeNode functions
//##################################################################################################
    // Required to be a TreeNode
    public Enumeration children()                 { return null; }
    public boolean     getAllowsChildren()        { return false; }
    public TreeNode    getChildAt(int childIndex) { return null; }
    /** Returns 0 for JTree so points won't be visible; not equal to children.size(). */
    public int         getChildCount()            { return 0; }
    public int         getIndex(TreeNode node)    { return 0; }
    public TreeNode    getParent()                { return getOwner(); }
    public boolean     isLeaf()                   { return true; }
    
    // Required to be a MutableTreeNode
    public void insert(MutableTreeNode child, int index)    {}
    public void remove(int index)                           {}
    public void remove(MutableTreeNode node)                {}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

