// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.data.TinyMap;
import driftwood.r3.*;
//}}}
/**
* <code>AbstractPoint</code> is a generic, non-instantiable representation of a 'point' in a kinemage.
* This class and its subclasses contain the code for drawing all the different graphics primitives in Mage.
*
* <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:57:57 EDT 2002
*/
abstract public class AbstractPoint extends AHEImpl implements KPoint
{
//{{{ Constants
    // Bit allocation for 'multi':
    //   kngpt  points  future  tinymap
    // skkkkkkkppppppppffffffffmmmmmmmm
    
    /** Isolates all the bits used by tinymap */
    public static final int TINYMAP_AND     = 0xff;
    /** tinymap keys, 0 - 7 */
    public static final int ASPECTS_KEY     = (1<<0);
    public static final int COMMENT_KEY     = (1<<1);
    public static final int COORDS_KEY      = (1<<2); // for kins with >3 dimensions
    
    /** If this bit is set, the point is 'live' and should be painted */
    public static final int ON_BIT          = 0x40000000;
    /** If this bit is set, the point will not be picked by a mouse click */
    public static final int UNPICKABLE      = 0x20000000;
    /** Used by e.g. TrianglePoints to tell whose normal to in lighting effects */
    public static final int SEQ_EVEN_BIT    = 0x10000000;
    /** A flag used by Mage only; point is visible but not written to PDB output. */
    public static final int GHOST_BIT       = 0x08000000;
//}}}

//{{{ Variable definitions
//##################################################################################################
    float x0 = 0f, y0 = 0f, z0 = 0f; // permanent coords
    float x  = 0f, y  = 0f, z  = 0f; // transformed coords

    KList       parent  = null;         // list that contains this point
    Object[]    tmValues = null;        // holds (ASPECTS), <point comments>, etc.
    int         pm_mask = 0;            // bit flags for pointmasters 
    
    /** Color this point is drawn in; null means take from list */
    KPaint      color   = null;
    
    /** higher bits are used as flags */
    public int multi = 0 | ON_BIT;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public AbstractPoint()
    {
    }

    /**
    * Constructor
    */
    public AbstractPoint(KList owner, String nm)
    {
        setOwner(owner);
        setName(nm);
    }
//}}}

//{{{ clone
//##################################################################################################
    public Object clone()
    {
        try { return super.clone(); }
        catch(CloneNotSupportedException ex)
        { throw new Error("Clone failed in cloneable object"); }
    }
//}}}

//{{{ get/setOrigX/Y/Z
//##################################################################################################
    /** Returns the untransformed coordinate for this point.
    * @deprecated In favor of getX(). */
    public float getOrigX()
    { return x0; }
    /** Returns the untransformed coordinate for this point.
    * @deprecated In favor of getY(). */
    public float getOrigY()
    { return y0; }
    /** Returns the untransformed coordinate for this point.
    * @deprecated In favor of getZ). */
    public float getOrigZ()
    { return z0; }

    /** Assigns a value to the untransformed coordinate for this point.
    * @deprecated In favor of setX(). */
    public void setOrigX(double xx) { x0 = (float)xx; }
    /** Assigns a value to the untransformed coordinate for this point.
    * @deprecated In favor of setY(). */
    public void setOrigY(double yy) { y0 = (float)yy; }
    /** Assigns a value to the untransformed coordinate for this point.
    * @deprecated In favor of setZ(). */
    public void setOrigZ(double zz) { z0 = (float)zz; }
    /** Assigns a value to the untransformed coordinates for this point.
    * @deprecated In favor of setXYZ(). */
    public void setOrigXYZ(Tuple3 t)
    {
        this.setOrigX(t.getX());
        this.setOrigY(t.getY());
        this.setOrigZ(t.getZ());
    }
//}}}

//{{{ get/setX/Y/Z
//##################################################################################################
    /** Returns the untransformed coordinate for this point */
    public double getX()
    { return x0; }
    /** Returns the untransformed coordinate for this point */
    public double getY()
    { return y0; }
    /** Returns the untransformed coordinate for this point */
    public double getZ()
    { return z0; }

    /** Assigns a value to the untransformed coordinate for this point */
    public void setX(double xx) { x0 = (float)xx; }
    /** Assigns a value to the untransformed coordinate for this point */
    public void setY(double yy) { y0 = (float)yy; }
    /** Assigns a value to the untransformed coordinate for this point */
    public void setZ(double zz) { z0 = (float)zz; }
    /** Assigns a value to the untransformed coordinates for this point */
    public void setXYZ(double xx, double yy, double zz)
    {
        x0 = (float)xx;
        y0 = (float)yy;
        z0 = (float)zz;
    }
//}}}

//{{{ get/setDrawX/Y/Z
//##################################################################################################
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawX()
    { return x; }
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawY()
    { return y; }
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawZ()
    { return z; }

    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    public void setDrawX(double xx) { x = (float)xx; }
    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    public void setDrawY(double yy) { y = (float)yy; }
    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    public void setDrawZ(double zz) { z = (float)zz; }
    /** Assigns a value to the fully transformed (drawing) coordinates for this point */
    public void setDrawXYZ(Tuple3 t)
    {
        x = (float)t.getX();
        y = (float)t.getY();
        z = (float)t.getZ();
    }
//}}}

//{{{ get/setAllCoords, useCoordsXYZ
//##################################################################################################
    /**
    * Stores an array of coordinates for "high-dimensional" points.
    * The float[] is stored without cloning and so is subject to overwrite.
    */
    public void setAllCoords(float[] coords)
    { tmPut(COORDS_KEY, coords); }
    
    /**
    * Retrieves the "high-dimensional" coordinates of this point, or null if not set.
    * The float[] is returned without cloning and so is subject to overwrite.
    */
    public float[] getAllCoords()
    { return (float[]) tmGet(COORDS_KEY); }
    
    /**
    * Copies the high-dimensional coordinates at the specified indices
    * into this point's (untransformed) X, Y, and Z fields.
    * If a index is out of range (0-based), it is ignored and the value is not changed.
    */
    public void useCoordsXYZ(int xIndex, int yIndex, int zIndex)
    {
        float[] coords = this.getAllCoords();
        if(coords == null) return;
        if((xIndex >= 0) && (xIndex < coords.length)) this.setX( coords[xIndex] );
        if((yIndex >= 0) && (yIndex < coords.length)) this.setY( coords[yIndex] );
        if((zIndex >= 0) && (zIndex < coords.length)) this.setZ( coords[zIndex] );
    }
//}}}

//{{{ get/setOwner, get/setPrev, isBreak
//##################################################################################################
    /** Determines the owner (parent) of this element */
    public AGE getOwner()
    { return parent; }
    /** Establishes the owner (parent) of this element */
    public void setOwner(AGE owner)
    {
        parent = (KList)owner;
    }

    /**
    * Sets the point that precedes this one.
    * This matters to "chainable" points, like vectors and triangles.
    * For other points, it does nothing.
    * @param pt the point preceding this one in seqence
    */
    public void setPrev(KPoint pt)
    {
        if(pt == null)                          multi &= ~SEQ_EVEN_BIT; // turn off
        else if(pt instanceof AbstractPoint)
        {
            AbstractPoint apt = (AbstractPoint) pt;
            if((apt.multi & SEQ_EVEN_BIT) != 0) multi &= ~SEQ_EVEN_BIT; // turn off
            else                                multi |= SEQ_EVEN_BIT;  // turn on
        }
        else                                    multi &= ~SEQ_EVEN_BIT; // turn off
    }
    
    /**
    * Gets the point preceding this one in the chain.
    * @return the preceding point, or null if (a) this is a break in the chain or (b) this is not a chainable point type.
    */
    public KPoint getPrev()
    { return null; }
    
    /**
    * True iff this is a chainable point type (e.g. vector, triangle) AND there is a chain break.
    */
    public boolean isBreak()
    { return false; }
//}}}

//{{{ is/get/set{On, Unpickable, Ghost, Color, Aspects, Width, Radius, Comment}
//##################################################################################################
    /** Indicates whether this element will paint itself, given the chance */
    public boolean isOn()
    { return ((multi & ON_BIT) != 0); }
    /** Sets the painting status of this element */
    public void setOn(boolean paint)
    {
        if(paint)   multi |=    ON_BIT;
        else        multi &=   ~ON_BIT;
    }
    
    /** Indicates whether this point can be picked with the mouse */
    public boolean isUnpickable()
    { return ((multi & UNPICKABLE) != 0); }
    /** Sets the picking status of this point */
    public void setUnpickable(boolean b)
    {
        if(b)   multi |=    UNPICKABLE;
        else    multi &=   ~UNPICKABLE;
    }

    /** Indicates whether this point is a "ghost" for Mage */
    public boolean isGhost()
    { return ((multi & GHOST_BIT) != 0); }
    /** Sets the ghost status of this point */
    public void setGhost(boolean b)
    {
        if(b)   multi |=    GHOST_BIT;
        else    multi &=   ~GHOST_BIT;
    }

    /** Returns the color of this point, or null if it inherits from its list */
    public KPaint getColor()
    { return color; }
    /** Sets the color of this point. */
    public void setColor(KPaint c) { color = c; }
    /** Gets the aspect string of this point */
    public String getAspects() { return (String) tmGet(ASPECTS_KEY); }
    /** Sets the aspect string of this point */
    public void setAspects(String a) { tmPut(ASPECTS_KEY, a); }
    
    /** Gets the line width of this point, if applicable */
    public int getWidth()
    { return 0; }
    /** Sets the line width of this point, if applicable */
    public void setWidth(int w)
    {}
    /** Gets the radius of this point, if applicable */
    public float getRadius()
    { return 0; }
    /** Sets the radius of this point, if applicable */
    public void setRadius(float radius)
    {}
    
    /** Sets the point comment for this point. */
    public void setComment(String cmt)
    { tmPut(COMMENT_KEY, cmt); }
    /** Gets the comment for this point, which defaults to null. */
    public String getComment()
    { return (String) tmGet(COMMENT_KEY); }
//}}}

//{{{ getDrawingColor
//##################################################################################################
    /** Returns the color that will be used to draw this point (ignoring aspects). Never null. */
    public KPaint getDrawingColor()
    {
        KPaint paint = null;
        //boolean byList = (engine.colorByList && parent != null);
        
        // If live bit has been unset by a pointmaster, we're invisible!
        if(!isOn())             paint = KPalette.invisible;
        //else if(byList)         paint = parent.color;
        else if(color != null)  paint = color;
        else if(parent != null) paint = parent.color;
        
        if(paint == null)       paint = KPalette.defaultColor;
        return paint;
    }

    /** Returns the color that will be used to draw this point, taking aspects into account. Never null. */
    public KPaint getDrawingColor(Engine engine)
    {
        KPaint paint = null, tmppaint = null;
        boolean byList = (engine.colorByList && parent != null);
        String aspects = this.getAspects();
        boolean doAspects = (engine.activeAspect > 0
            && aspects != null
            && aspects.length() >= engine.activeAspect);
            
        // If live bit has been unset by a pointmaster, we're invisible!
        if(!isOn())             paint = KPalette.invisible;
        else if(byList)         paint = parent.color;
        // This way, we only use the aspect if we recognize the character
        else if(doAspects && (tmppaint = KPalette.forAspect(aspects.charAt(engine.activeAspect-1))) != null)
                                paint = tmppaint;
        else if(color != null)  paint = color;
        else if(parent != null) paint = parent.color;
        
        if(paint == null)       paint = KPalette.defaultColor;
        return paint;
    }
//}}}

//{{{ pmHit, pmWouldHit, get/setPmMask
//##################################################################################################
    /**
    * Processes a pointmaster on/off request.
    * @param mask       the bitmask indicating which masters are being turned on/off
    * @param offmask    the bitmask indicating which masters are already off
    * @param turnon <code>true</code> if affected points are to be turned on,
    *               <code>false</code> if affected points are to be turned off.
    */
    public void pmHit(int mask, int offmask, boolean turnon)
    {
        //Turn OFF if we're affected by this pointmaster, period.
        //Turn ON if and only if all our other pointmasters are also ON already.
        if(turnon)
        {
            if((   mask & pm_mask) != 0
            && (offmask & pm_mask) == 0)    setOn(true);
        }
        else
        {
            if((mask & pm_mask) != 0)       setOn(false);
        }
    }
    
    /** Indicates whether or not the given pointmaster set would affect this point. */
    public boolean pmWouldHit(int mask)
    { return (mask & pm_mask) != 0; }
    
    public int getPmMask()
    { return pm_mask; }
    
    public void setPmMask(int mask)
    { this.pm_mask = mask; }
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
        if(x0 < bound[0]) bound[0] = x0;
        if(y0 < bound[1]) bound[1] = y0;
        if(z0 < bound[2]) bound[2] = z0;
        if(x0 > bound[3]) bound[3] = x0;
        if(y0 > bound[4]) bound[4] = y0;
        if(z0 > bound[5]) bound[5] = z0;
    }

    /**
    * Gets the square of the radius of this model from the specified center.
    * @param center an array with the x, y,  and z coordinates of the center
    * @return the square of the radius of this element, centered at center
    */
    public float calcRadiusSq(float[] center)
    {
        float dx, dy, dz;
        dx = x0 - center[0];
        dy = y0 - center[1];
        dz = z0 - center[2];
        return (dx*dx + dy*dy + dz*dz);
    }
//}}}
    
//{{{ signalTransform
//##################################################################################################
    public void signalTransform(Engine engine, Transform xform)
    { signalTransform(engine, xform, 1.0); }

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
    * @param zoom       the zoom factor encoded by the Transform,
    *   as a convenience for resizing things.
    */
    public void signalTransform(Engine engine, Transform xform, double zoom)
    {
        // We have to transform whether we're on or not, because dependent points
        // (vectors, triangles) may be on and expect our coords to be valid.
        // Point-on is checked during drawing and picking by getDrawingColor.
        
        xform.transform(this, engine.work1);
        setDrawXYZ(engine.work1);
        
        engine.addPaintable(this, z);
    }
//}}}

//{{{ isPickedBy
//##################################################################################################
    /**
    * Returns true if the specified pick hits this point, else returns false
    * Pays no attention to whether this point is marked as unpickable.
    * @param radius the desired picking radius
    * @param objPick whether we should try to pick solid objects in addition to points
    * @return the KPoint that should be counted as being picked, or null for none.
    *   Usually <code>this</code>, but maybe not for object picking.
    */
    public KPoint isPickedBy(float xx, float yy, float radius, boolean objPick)
    { return _isPickedBy(xx, yy, radius, objPick); }
    
    // For BallPoint and RingPoint, because Java doesn't allow super.super:
    KPoint _isPickedBy(float xx, float yy, float radius, boolean objPick)
    {
        float deltax, deltay;
        deltax = (x - xx);
        deltay = (y - yy);
        if((deltax*deltax + deltay*deltay) <= radius*radius)    return this;
        else                                                    return null;        
    }
//}}}

//{{{ equals(), hashCode()
//##################################################################################################
    // These functions are for identifying points that occupy the same location in space.
    
    /** Tests two KPoints to see if they occupy the same space */
    public boolean equals(Object obj)
    {
        if(obj == null || !(obj instanceof KPoint)) return false;
        KPoint p = (KPoint)obj;
        if(getX() == p.getX() && getY() == p.getY() && getZ() == p.getZ()) return true;
        else return false;
    }
    
    /** Generates a hashcode based on the coordinates of this point */
    public int hashCode()
    {
        /* I stole this from Colt: */
        //   this avoids excessive hashCollisions
        //   in the case values are of the form (1.0, 2.0, 3.0, ...)
        int b1 = Float.floatToIntBits(x0*663608941.737f);
        int b2 = Float.floatToIntBits(y0*663608941.737f);
        int b3 = Float.floatToIntBits(z0*663608941.737f);
        // The rotation of bits is my own idea
        return (b1 ^ (b2<<11 | b2>>>21) ^ (b3<<22 | b3>>>10));
        /* I stole this from Colt: */
        
        /* Old version * /
        int b1 = Float.floatToIntBits(x0);
        int b2 = Float.floatToIntBits(y0);
        int b3 = Float.floatToIntBits(z0);
        // The rotation of bits is my own idea
        return (b1 ^ b2 ^ b3);
        /* Old version */
    }
//}}}

//{{{ tmGet, tmPut, tmRemove
//##############################################################################
    /**
    * Returns the value associated with the given key,
    * or null if this map does not contain that key.
    */
    Object tmGet(int key)
    {
        int keyMap = this.multi & TINYMAP_AND;
        if(!TinyMap.contains(key, keyMap)) return null;
        else return tmValues[TinyMap.indexOf(key, keyMap)];
    }
    
    /**
    * Associates a new value with key and returns the old value,
    * or null if none was set.
    */
    Object tmPut(int key, Object value)
    {
        int keyMap = this.multi & TINYMAP_AND;
        int i = TinyMap.indexOf(key, keyMap);
        if(TinyMap.contains(key, keyMap))
        {
            Object old = tmValues[i];
            tmValues[i] = value;
            return old;
        }
        else
        {
            int tmValues_length = TinyMap.size(keyMap); //tmValues may be null!
            this.multi |= (1 << key) & TINYMAP_AND;
            Object[] newvals = new Object[tmValues_length+1];
            for(int j = 0; j < i; j++) newvals[j] = tmValues[j];
            newvals[i] = value;
            for(int j = i; j < tmValues_length; j++) newvals[j+1] = tmValues[j];
            tmValues = newvals;
            return null;
        }
    }
    
    /** Removes the value for the given key, if present. */
    Object tmRemove(int key)
    {
        int keyMap = this.multi & TINYMAP_AND;
        if(!TinyMap.contains(key, keyMap)) return null;
        
        int i = TinyMap.indexOf(key, keyMap);
        Object old = tmValues[i];
        
        this.multi &= ~((1 << key) & TINYMAP_AND);
        Object[] newvals = new Object[tmValues.length-1];
        for(int j = 0; j < i; j++) newvals[j] = tmValues[j];
        for(int j = i+1; j < tmValues.length; j++) newvals[j-1] = tmValues[j];
        tmValues = newvals;
        if(tmValues.length == 0) tmValues = null; // just to save space
        return old;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

