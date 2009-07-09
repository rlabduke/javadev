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
* <p>In principle, we could generify this class to
* <code>KList&lt;C extends KPoint&gt; extends AGE&lt:KGroup,C&gt;</code>,
* but due to what I see as a bug in Sun's compiler, references to
* <code>KList</code> are then treated as <code>KList&lt;Object&gt;</code>
* (which isn't even legal!) rather than as <code>KList&lt;? extends KPoint&gt;</code>.
* All the extra typing probably isn't worth it;
* there are very few cases where you would actually get improved type safety,
* because almost all functions operate on lists of any kind of point.
*
* <p>Designating the type of a list has actually turned out to be very hard.
* Using an enum is not suitable, as the set of list types must be able to
* grow as third parties define new point types.
* Using the Class of the point type is possible, but translating it to a string
* for output is problematic, and some point types are used to implement
* more than one type of kinemage list (eg TrianglePoint for triangle- and ribbon-lists).
* So I've fallen back on plain old Strings, which aren't type safe, but are flexible.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:37:31 EDT 2002
*/
public class KList extends AGE<KGroup,KPoint> implements Cloneable
{
//{{{ Constants
    public static final String  VECTOR   = "vector";
    public static final String  DOT      = "dot";
    public static final String  MARK     = "mark";
    public static final String  LABEL    = "label";
    public static final String  TRIANGLE = "triangle";
    public static final String  RIBBON   = "ribbon";
    public static final String  RING     = "ring";
    public static final String  BALL     = "ball";
    public static final String  SPHERE   = "sphere";
    public static final String  ARROW    = "arrow";
//}}}

//{{{ Variable definitions
//##################################################################################################
    protected String    type;                   // type of object represented by this list
    protected KList     instance    = null;     // the list that this one is an instance= {xxx} of (usually null)
    
    protected KPaint    color       = KPalette.defaultColor;
    protected int       alpha       = 255;      // 255 = opaque, 0 = fully transparent
    protected float     radius      = 0.2f;     // seems to be default in Mage; also used for arrow tine length (radius=)
    protected float     angle       = 20f;      // for ArrowPoints
    protected int       width       = 2;
    protected int       style       = 0;        // style for MarkerPoints; could be alignment for labels
    protected Object    clipMode    = null;     // null for default, else some object key
    protected int       dimension   = 3;        // for high-dimensional kinemages
    protected boolean   screen      = false;    // scale w/ screen but don't rotate/translate (like ala_dipept in Mage) (DAK 090212)
    protected boolean   rear        = false;    // use the rearmost point to determine zbuffer bin.
    protected boolean   fore        = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Creates a new KList of the specified type with an empty name. */
    public KList(String type)
    {
        this(type, "");
    }

    /** Creates a new KList with the specified name. */
    public KList(String type, String nm)
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

//{{{ get/set{Type, Color, Width, Radius, Alpha, Screen}
//##################################################################################################
    /** Determines the type of points held by this list */
    public String getType()
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
        radius = r;
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    
    /** Gets the translucency of points in this list, from 0 (invisible) to 255 (opaque). */
    public int getAlpha()
    { return alpha; }
    /** Sets the translucency of points in this list, from 0 (invisible) to 255 (opaque). */
    public void setAlpha(int a)
    { alpha = a; }
    
    /** Sets screen variable for this list (DAK 090212). */
    public void setScreen(boolean scr)
    {
        this.screen = scr;
        // also set width & radius to defaults to avoid scaling-on-zoom confusion?
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    /** Gets screen variable for this list (DAK 090212). */
    public boolean getScreen()
    { return this.screen; }
    
    /** Sets rear variable for this list. */
    public void setRear(boolean rr) {
      this.rear = rr;
      fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    /** Gets rear variable for this list. */
    public boolean getRear() {
      return this.rear; 
    }
    
    /** Sets fore variable for this list. */
    public void setFore(boolean rr) {
      this.fore = rr;
      fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    /** Gets fore variable for this list. */
    public boolean getFore() {
      return this.fore; 
    }
//}}}

//{{{ get/set{Angle, Style, ClipMode, Dimension, NoHighlight}
//##################################################################################################
    /** For use with ArrowPoint */
    public float getAngle()
    { return angle; }
    public void setAngle(float a)
    {
        angle = a;
        fireKinChanged(CHANGE_LIST_PROPERTIES);
    }
    
    /** For use with MarkerPoint */
    public int getStyle()
    { return style; }
    public void setStyle(int s)
    {
        style = s;
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
    
    /** Indicates whether BallPoints should have their highlights supressed. */
    public boolean getNoHighlight()
    { return (flags & FLAG_NOHILITE) == FLAG_NOHILITE; }
    /** Sets whether BallPoints should have their highlights supressed. */
    public void setNoHighlight(boolean b)
    {
        int oldFlags = flags;
        if(b)   flags |= FLAG_NOHILITE;
        else    flags &= ~FLAG_NOHILITE;
        if(flags != oldFlags) fireKinChanged(CHANGE_TREE_PROPERTIES);
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
        
        // Messing with 'screen' keyword on lists for keeping children fixed on screen
        /*
        if(this.screen)
        {
            for(KPoint child : this.getChildren())
            {
                  System.err.println("canvas rect:"+engine.pickingRect);
                double width, height, size, xOff, yOff;
                width   = engine.pickingRect.getWidth();
                height  = engine.pickingRect.getHeight();
                size    = Math.min(width, height);
                xOff    = engine.pickingRect.getX() + width/2.0;
                yOff    = engine.pickingRect.getY() + height/2.0;
                
                Transform ret = new Transform(), work = new Transform();
                work.likeTranslation(xOff-child.getDrawX(), yOff-child.getDrawY(), 0);
                    ret.append(work);
                work.likeScale(200.0);  // <- ??
                    ret.append(work);
                
                  System.err.println(child+" orig : ("+child.getX()+","+child.getY()+","+child.getZ()+")");
                  //System.err.println(child+" b4 mv: ("+child.getDrawX()+","+child.getDrawY()+","+child.getDrawZ()+")");
                child.doTransform(engine, ret, engine.zoom3D);
                  System.err.println(child+" xfrmd: ("+child.getDrawX()+","+child.getDrawY()+","+child.getDrawZ()+")");
                  System.err.println(child+" xform:\n"+ret);
                  //System.err.println();
            }
        }
        else
            for(KPoint child : this.getChildren()) child.doTransform(engine, myXform, engine.zoom3D);
        */
        
        if(this.clipMode != null) engine.chooseClipMode(null); // reset to default
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

