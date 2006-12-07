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
* <code>KList</code> implements the concept of a list in a kinemage.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:37:31 EDT 2002
*/
public class KList extends AGE<KGroup,KPoint> implements Cloneable
{
//{{{ Constants
    public enum Type { vector, dot, mark, label, triangle, ribbon, ring, ball, sphere, arrow }
//}}}

//{{{ Variable definitions
//##################################################################################################
    protected KList     instance    = null;     // the list that this one is an instance= {xxx} of (usually null)
    protected Type      type;                   // type of object represented by this list
    
    protected KPaint    color       = KPalette.defaultColor;
    protected int       alpha       = 255;      // 255 = opaque, 0 = fully transparent
    protected float     radius      = 0.2f;     // seems to be default in Mage; also used for arrow tine length (radius=)
    protected int       width       = 2;
    protected Object    clipMode    = null;     // null for default, else some object key
    protected int       dimension   = 3;        // for high-dimensional kinemages
    
    // Parameters used for arrowlists; see ArrowPoint for explanation
    // of tine PERPendicular and PARallel components.
    protected float     angle       = 20f;
    protected float     tinePerp    = (float)(radius * Math.sin(Math.toRadians(angle)));
    protected float     tinePar     = (float)(radius * Math.cos(Math.toRadians(angle)));
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Creates a new KList of the specified type with an empty name. */
    public KList(Type type)
    {
        this(type, "");
    }

    /** Creates a new KList with the specified name. */
    public KList(Type type, String nm)
    {
        super();
        this.type = type;
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
    public KList clone(boolean clonePoints)
    {
        try
        {
            KList x = (KList) super.clone(clonePoints);
            x.children = new ArrayList<KPoint>();
            
            if(clonePoints)
            {
                // Deep copy of children from original source
                KList orig = this.getUltimateInstance();
                if(orig == null) orig = this;
                
                KPoint prev = null;
                for(KPoint child : orig.getChildren())
                {
                    KPoint clone = (KPoint) child.clone();
                    x.add(clone);
                    clone.setParent(x);
                    
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
            if(this.masters != null) x.masters = new ArrayList<String>(this.masters);
            
            return x;
        }
        catch(CloneNotSupportedException ex)
        { throw new Error("Clone failed in cloneable object"); }
    }
//}}}

//{{{ get/set(Ultimate)Instance
//##################################################################################################
    /** Sets which list this one is an instance of, or null for none. */
    public void setInstance(KList inst)
    {
        this.instance = inst;
        fireKinChanged(CHANGE_POINT_CONTENTS);
    }
    
    /** Gets which list this one is an instance of, or null for none. */
    public KList getInstance()
    { return this.instance; }
    
    /** In case of a chain of instance-of relationships, finds the list at the end of the chain. */
    public KList getUltimateInstance()
    {
        KList inst = this.getInstance();
        if(inst == null) return null;
        else
        {
            while(inst.getInstance() != null) inst = inst.getInstance();
            return inst;
        }
    }
//}}}

//{{{ get/set{Type, Color, Width, Radius}
//##################################################################################################
    /** Determines the type of points held by this list */
    public Type getType()
    { return type; }
    
    /** Determines the default color of points held by this list */
    public KPaint getColor()
    { return color; }
    /** Establishes the default color of points held by this list */
    public void setColor(KPaint c)
    {
        color = c;
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    
    /** Determines the default width of points held by this list */
    public int getWidth()
    { return width; }
    /** Establishes the default width of points held by this list */
    public void setWidth(int w)
    {
        if(w > 7)       width = 7;
        else if(w < 1)  width = 1;
        else            width = w;
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    
    public float getRadius()
    { return radius; }
    public void setRadius(float r)
    {
        radius      = r;
        tinePerp    = (float)(radius * Math.sin(Math.toRadians(angle)));
        tinePar     = (float)(radius * Math.cos(Math.toRadians(angle)));
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
//}}}

//{{{ get/set{Angle, Style, ClipMode, Dimension}
//##################################################################################################
    /** For use with ArrowPoint */
    public float getAngle()
    { return angle; }
    public void setAngle(float a)
    {
        angle       = a;
        tinePerp    = (float)(radius * Math.sin(Math.toRadians(angle)));
        tinePar     = (float)(radius * Math.cos(Math.toRadians(angle)));
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    
    /** For use with MarkerPoint */
    public int getStyle()
    { return (flags & MASK_MARKER_STYLES); }
    public void setStyle(int s)
    {
        flags = (s & MASK_MARKER_STYLES) | (flags & ~MASK_MARKER_STYLES);
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    
    /** Gets the clipping mode key for this list. Usually null. See Engine.chooseClipMode(). */
    public Object getClipMode()
    { return clipMode; }
    /** Sets the clipping mode for this list. */
    public void setClipMode(Object key)
    {
        this.clipMode = key;
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    
    /** Returns the nominal number of coordinates per point in this list. */
    public int getDimension() { return this.dimension; }
    /** Sets the nominal number of coordinates per point in this list. */
    public void setDimension(int d)
    {
        this.dimension = d;
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
//}}}

//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform)
    {
        // If the button is off, this will never be rendered
        if(!isOn()) return;
        if(this.clipMode != null) engine.chooseClipMode(this.clipMode); // set alt clipping

        // If we're an instance of someone else, transform those points too
        KList inst = getUltimateInstance();
        if(inst != null)
        {
            engine.setActingParent(this);
            Transform myXform = xform;
            if(inst.getTransform() != null)
                myXform = new Transform().like(inst.getTransform()).append(xform);
            for(KPoint child : inst.getChildren()) child.doTransform(engine, myXform, engine.zoom3D);
        }
        
        engine.setActingParent(null);
        Transform myXform = xform;
        if(this.getTransform() != null)
            myXform = new Transform().like(this.getTransform()).append(xform);
        for(KPoint child : this.getChildren()) child.doTransform(engine, myXform, engine.zoom3D);

        if(this.clipMode != null) engine.chooseClipMode(null); // reset to default
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

