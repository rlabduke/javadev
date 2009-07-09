// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import driftwood.data.TinyMap;
import driftwood.r3.*;
//}}}
/**
* <code>AbstractPoint</code> is a generic, non-instantiable representation of a 'point' in a kinemage.
* This class and its subclasses contain the code for drawing all the different graphics primitives in Mage.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:57:57 EDT 2002
*/
abstract public class AbstractPoint extends AHEImpl<KList> implements KPoint
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
    protected float x0 = 0f, y0 = 0f, z0 = 0f;  // permanent coords
    protected float x  = 0f, y  = 0f, z  = 0f;  // transformed coords

    protected KList     parent      = null;     // list that contains this point
    protected Object[]  tmValues    = null;     // holds (ASPECTS), <point comments>, etc.
    protected int       pm_mask     = 0;        // bit flags for pointmasters 
    
    /** Color this point is drawn in; null means take from list */
    protected KPaint    color       = null;
    
    /** higher bits are used as flags */
    protected int multi = 0 | ON_BIT;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    public AbstractPoint()
    { this(""); }

    public AbstractPoint(String nm)
    {
        super();
        setName(nm);
    }
//}}}

//{{{ clone
//##################################################################################################
    public KPoint clone()
    {
        try { return (KPoint) super.clone(); }
        catch(CloneNotSupportedException ex)
        { throw new Error("Clone failed in cloneable object"); }
    }
//}}}

//{{{ get/setX/Y/Z
//##################################################################################################
    public double getX()
    { return x0; }
    public double getY()
    { return y0; }
    public double getZ()
    { return z0; }

    public void setX(double xx) { x0 = (float)xx; fireKinChanged(CHANGE_POINT_COORDINATES); }
    public void setY(double yy) { y0 = (float)yy; fireKinChanged(CHANGE_POINT_COORDINATES); }
    public void setZ(double zz) { z0 = (float)zz; fireKinChanged(CHANGE_POINT_COORDINATES); }
    public void setXYZ(double xx, double yy, double zz)
    {
        x0 = (float)xx;
        y0 = (float)yy;
        z0 = (float)zz;
        fireKinChanged(CHANGE_POINT_COORDINATES);
    }
//}}}

//{{{ get/setDrawX/Y/Z
//##################################################################################################
    public float getDrawX()
    { return x; }
    public float getDrawY()
    { return y; }
    public float getDrawZ()
    { return z; }

    /** Assigns a value to the fully transformed (drawing) coordinates for this point (convenience for subclasses) */
    protected void setDrawXYZ(Tuple3 t)
    {
        x = (float)t.getX();
        y = (float)t.getY();
        z = (float)t.getZ();
    }
//}}}

//{{{ get/setAllCoords, useCoordsXYZ
//##################################################################################################
    public void setAllCoords(float[] coords)
    { tmPut(COORDS_KEY, coords); }
    
    public float[] getAllCoords()
    { return (float[]) tmGet(COORDS_KEY); }
    
    public void useCoordsXYZ(int xIndex, int yIndex, int zIndex)
    {
        float[] coords = this.getAllCoords();
        if(coords == null) return;
        if((xIndex >= 0) && (xIndex < coords.length)) this.setX( coords[xIndex] );
        if((yIndex >= 0) && (yIndex < coords.length)) this.setY( coords[yIndex] );
        if((zIndex >= 0) && (zIndex < coords.length)) this.setZ( coords[zIndex] );
    }
//}}}

//{{{ get/setParent, get/setPrev, isBreak
//##################################################################################################
    public KList getParent()
    { return parent; }
    public void setParent(KList owner)
    {
        parent = owner;
        fireKinChanged(CHANGE_POINT_PROPERTIES);
    }

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
        fireKinChanged(CHANGE_POINT_PROPERTIES);
    }
    
    public KPoint getPrev()
    { return null; }
    
    public boolean isBreak()
    { return false; }
//}}}

//{{{ is/get/set{On, Unpickable, Ghost, Color, Aspects, Width, Radius, Comment}
//##################################################################################################
    public boolean isOn()
    { return ((multi & ON_BIT) != 0); }
    public void setOn(boolean paint)
    {
        if(paint)   multi |=    ON_BIT;
        else        multi &=   ~ON_BIT;
        fireKinChanged(CHANGE_POINT_ON_OFF);
    }
    
    public boolean isUnpickable()
    { return ((multi & UNPICKABLE) != 0); }
    public void setUnpickable(boolean b)
    {
        if(b)   multi |=    UNPICKABLE;
        else    multi &=   ~UNPICKABLE;
        fireKinChanged(CHANGE_POINT_PROPERTIES);
    }

    public boolean isGhost()
    { return ((multi & GHOST_BIT) != 0); }
    public void setGhost(boolean b)
    {
        if(b)   multi |=    GHOST_BIT;
        else    multi &=   ~GHOST_BIT;
        fireKinChanged(CHANGE_POINT_PROPERTIES);
    }

    public KPaint getColor()
    { return color; }
    public void setColor(KPaint c) { color = c; fireKinChanged(CHANGE_POINT_PROPERTIES); }
    
    public String getAspects() { return (String) tmGet(ASPECTS_KEY); }
    public void setAspects(String a) { tmPut(ASPECTS_KEY, a); }
    
    public int getWidth()
    { return 0; }
    public void setWidth(int w)
    {}
    
    public float getRadius()
    { return 0; }
    public void setRadius(float radius)
    {}
    
    public void setComment(String cmt)
    { tmPut(COMMENT_KEY, cmt); }
    public String getComment()
    { return (String) tmGet(COMMENT_KEY); }
//}}}

//{{{ getDrawingColor
//##################################################################################################
    public KPaint getDrawingColor()
    {
        KPaint paint = null;
        
        // If live bit has been unset by a pointmaster, we're invisible!
        if(!isOn())             paint = KPalette.invisible;
        else if(color != null)  paint = color;
        else if(parent != null) paint = parent.getColor();
        
        if(paint == null)       paint = KPalette.defaultColor;
        return paint;
    }

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
        else if(byList)         paint = parent.getColor();
        // This way, we only use the aspect if we recognize the character
        else if(doAspects && (tmppaint = KPalette.forAspect(aspects.charAt(engine.activeAspect-1))) != null)
                                paint = tmppaint;
        else if(color != null)  paint = color;
        else if(parent != null) paint = parent.getColor();
        
        if(paint == null)       paint = KPalette.defaultColor;
        return paint;
    }
//}}}

//{{{ pmHit, pmWouldHit, get/setPmMask
//##################################################################################################
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
    
    public boolean pmWouldHit(int mask)
    { return (mask & pm_mask) != 0; }
    
    public int getPmMask()
    { return pm_mask; }
    
    public void setPmMask(int mask)
    { this.pm_mask = mask; fireKinChanged(CHANGE_POINT_MASTERS); }
//}}}
    
//{{{ calcBoundingBox, calcRadiusSq
//##################################################################################################
    public void calcBoundingBox(float[] bound)
    {
        if(!parent.getScreen())
        {
            if(x0 < bound[0]) bound[0] = x0;
            if(y0 < bound[1]) bound[1] = y0;
            if(z0 < bound[2]) bound[2] = z0;
            if(x0 > bound[3]) bound[3] = x0;
            if(y0 > bound[4]) bound[4] = y0;
            if(z0 > bound[5]) bound[5] = z0;
        }
    }

    public float calcRadiusSq(float[] center)
    {
        float dx, dy, dz;
        dx = x0 - center[0];
        dy = y0 - center[1];
        dz = z0 - center[2];
        return (dx*dx + dy*dy + dz*dz);
    }
//}}}
    
//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform)
    { doTransform(engine, xform, 1.0); }

    public void doTransform(Engine engine, Transform xform, double zoom)
    {
        // We have to transform whether we're on or not, because dependent points
        // (vectors, triangles) may be on and expect our coords to be valid.
        // Point-on is checked during drawing and picking by getDrawingColor.
        if(parent.getScreen())
        {
            //{{{ [old code - ignore]
            //Kinemage ancestor = getKinemage();
            //float span = ancestor.getSpan();
            //float[] bounds = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
            //-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
            //parent.calcBoundingBox(bounds);
            //float[] center = new float[3];
            //center[0] = (bounds[3] + bounds[0])/2f;
            //center[1] = (bounds[4] + bounds[1])/2f;
            //center[2] = (bounds[5] + bounds[2])/2f;
            //System.out.println("bounds"+bounds[3]+" "+bounds[0]);
            //System.out.println("bounds"+bounds[4]+" "+bounds[1]);
            //double width  = engine.pickingRect.getWidth();
            //double height = engine.pickingRect.getHeight();
            //System.out.println("W: "+width+" H: "+height);
            //double xmult = (width/(bounds[3] - bounds[0]));
            //double ymult = (height/(bounds[4] - bounds[1]));
            //float width = center[0];
            //float height = center[1];
            //r = 50;
            //System.out.println((getX()+center[0]*xmult/2)*xmult+" "+(-getY()+center[1]*ymult/2)*ymult);
            //setDrawXYZ(new Triple((getX()-center[0])*xmult, (-getY()-center[1])*ymult, getZ()));
            //setDrawXYZ(new Triple(getX()+width/2, -getY()+height/2, getZ()));
            //}}}
            
            double width  = engine.pickingRect.getWidth();
            double height = engine.pickingRect.getHeight();
            double x = width /2 + getX()/200.0 * Math.min(width, height)/2;
            double y = height/2 - getY()/200.0 * Math.min(width, height)/2;
            setDrawXYZ(new Triple(x, y, getZ()));
            
            // Choice of second param shouldn't matter b/c engine handles 'screen' parent lists
            engine.addPaintable(this, z);
        }
        else
        {
            xform.transform(this, engine.work1);
            setDrawXYZ(engine.work1);
            
            engine.addPaintable(this, z);
        }
    }
//}}}

//{{{ isPickedBy
//##################################################################################################
    public KPoint isPickedBy(float xx, float yy, float radius, boolean objPick)
    { return _isPickedBy(xx, yy, radius, objPick); }
    
    // For BallPoint and RingPoint, because Java doesn't allow super.super:
    protected KPoint _isPickedBy(float xx, float yy, float radius, boolean objPick)
    {
        float deltax, deltay;
        deltax = (x - xx);
        deltay = (y - yy);
        if((deltax*deltax + deltay*deltay) <= radius*radius)    return this;
        else                                                    return null;        
    }
//}}}

//{{{ equals, hashCode
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
        // I stole this from Colt:
        //   this avoids excessive hashCollisions
        //   in the case values are of the form (1.0, 2.0, 3.0, ...)
        int b1 = Float.floatToIntBits(x0*663608941.737f);
        int b2 = Float.floatToIntBits(y0*663608941.737f);
        int b3 = Float.floatToIntBits(z0*663608941.737f);
        // The rotation of bits is my own idea
        return (b1 ^ (b2<<11 | b2>>>21) ^ (b3<<22 | b3>>>10));
    }
//}}}

//{{{ tmGet, tmPut, tmRemove
//##############################################################################
    /**
    * Returns the value associated with the given key,
    * or null if this map does not contain that key.
    */
    protected Object tmGet(int key)
    {
        int keyMap = this.multi & TINYMAP_AND;
        if(!TinyMap.contains(key, keyMap)) return null;
        else return tmValues[TinyMap.indexOf(key, keyMap)];
    }
    
    /**
    * Associates a new value with key and returns the old value,
    * or null if none was set.
    */
    protected Object tmPut(int key, Object value)
    {
        int keyMap = this.multi & TINYMAP_AND;
        int i = TinyMap.indexOf(key, keyMap);
        if(TinyMap.contains(key, keyMap))
        {
            Object old = tmValues[i];
            tmValues[i] = value;
            fireKinChanged(CHANGE_POINT_PROPERTIES);
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
            fireKinChanged(CHANGE_POINT_PROPERTIES);
            return null;
        }
    }
    
    /** Removes the value for the given key, if present. */
    protected Object tmRemove(int key)
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

