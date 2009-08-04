// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.points.VectorPoint;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
//}}}
/**
* <code>ParaPoint</code> is proxy point used for parallel coordinates.
*
* <p>Copyright (C) 2006-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Nov 17 11:34:49 EST 2006
*/
public class ParaPoint extends VectorPoint
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KPoint      proxyFor;
    int         dimIdx;
    ParaParams  params;
    double      width;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a new data point representing one end of a line.
    *
    * @param proxyFor   the (Ball)Point that this line represents
    * @param dimIdx     the dimension / axis of proxyFor to represent, from 0 ... D
    *                   (Could probably be figured by tracing back along start.)
    * @param start      where this line is drawn from, or null if it's the starting point
    */
    public ParaPoint(KPoint proxyFor, int dimIdx, ParaPoint start, ParaParams params, double width)
    {
        super(proxyFor.getName(), start);
        this.setParent(proxyFor.getParent());
        this.proxyFor   = proxyFor;
        this.dimIdx     = dimIdx;
        this.params     = params;
        this.width      = width;
        if (width < 900) this.width = 900;
        syncCoords();
    }
//}}}

//{{{ syncCoords
//##############################################################################
    protected void syncCoords()
    {
        float[] allCoords = proxyFor.getAllCoords();
        if(allCoords == null || allCoords.length <= dimIdx)
            throw new IllegalArgumentException("Not enough coordinates in proxy to support parallel coords");
        
        this.x0 = (float) (1.0 * dimIdx / (params.getNumDim() - 1)+((float)dimIdx-(float)params.getNumDim()/2)/ (float)(params.getNumDim()-1)*(width-900)/900); // [0, 1]
        this.y0 = (float) ((allCoords[dimIdx] - params.getMin(dimIdx)) / params.getRange(dimIdx)); // [0, 1]
        this.z0 = 0; // may use this for something later, not now
    }
//}}}

//{{{ get/setX/Y/Z
//##################################################################################################
    /** Returns the untransformed coordinate for this point */
    //public double getX() { return proxyFor.getX(); }
    /** Returns the untransformed coordinate for this point */
    //public double getY() { return proxyFor.getY(); }
    /** Returns the untransformed coordinate for this point */
    //public double getZ() { return proxyFor.getZ(); }

    /** Assigns a value to the untransformed coordinate for this point */
    public void setX(double xx) {}
    /** Assigns a value to the untransformed coordinate for this point */
    public void setY(double yy) {}
    /** Assigns a value to the untransformed coordinate for this point */
    public void setZ(double zz) {}
    /** Assigns a value to the untransformed coordinates for this point */
    public void setXYZ(double xx, double yy, double zz) {}
//}}}

//{{{ get/setDrawX/Y/Z
//##################################################################################################
    /** Returns the fully transformed (drawing) coordinate for this point */
    //public float getDrawX() { return proxyFor.getDrawX(); }
    /** Returns the fully transformed (drawing) coordinate for this point */
    //public float getDrawY() { return proxyFor.getDrawY(); }
    /** Returns the fully transformed (drawing) coordinate for this point */
    //public float getDrawZ() { return proxyFor.getDrawZ(); }

    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    //public void setDrawX(double xx) { proxyFor.setDrawX(xx); }
    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    //public void setDrawY(double yy) { proxyFor.setDrawY(yy); }
    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    //public void setDrawZ(double zz) { proxyFor.setDrawZ(zz); }
    /** Assigns a value to the fully transformed (drawing) coordinates for this point */
    //public void setDrawXYZ(Tuple3 t) { proxyFor.setDrawXYZ(t); }
//}}}

//{{{ get/setAllCoords, useCoordsXYZ
//##################################################################################################
    /**
    * Stores an array of coordinates for "high-dimensional" points.
    * The float[] is stored without cloning and so is subject to overwrite.
    */
    //public void setAllCoords(float[] coords) { proxyFor.setAllCoords(coords); }
    
    /**
    * Retrieves the "high-dimensional" coordinates of this point, or null if not set.
    * The float[] is returned without cloning and so is subject to overwrite.
    */
    //public float[] getAllCoords() { return proxyFor.getAllCoords(); }
    
    /**
    * Copies the high-dimensional coordinates at the specified indices
    * into this point's (untransformed) X, Y, and Z fields.
    * If a index is out of range (0-based), it is ignored and the value is not changed.
    */
    public void useCoordsXYZ(int xIndex, int yIndex, int zIndex) { proxyFor.useCoordsXYZ(xIndex, yIndex, zIndex); }
//}}}

//{{{ get/setPrev, isBreak
//##################################################################################################
    /**
    * Sets the point that precedes this one.
    * This matters to "chainable" points, like vectors and triangles.
    * For other points, it does nothing.
    * @param pt the point preceding this one in seqence
    */
    //public void setPrev(KPoint pt) { proxyFor.setPrev(pt); }
    
    /**
    * Gets the point preceding this one in the chain.
    * @return the preceding point, or null if (a) this is a break in the chain or (b) this is not a chainable point type.
    */
    //public KPoint getPrev() { return proxyFor.getPrev(); }
    
    /**
    * True iff this is a chainable point type (e.g. vector, triangle) AND there is a chain break.
    */
    //public boolean isBreak() { return proxyFor.isBreak(); }
//}}}

//{{{ is/get/set{On, Unpickable, Ghost, Color, Aspects, Width, Radius, Comment}
//##################################################################################################
    /** Indicates whether this element will paint itself, given the chance */
    public boolean isOn() { return proxyFor.isOn(); }
    /** Sets the painting status of this element */
    public void setOn(boolean paint) { proxyFor.setOn(paint); }
    
    /** Indicates whether this point can be picked with the mouse */
    public boolean isUnpickable() { return proxyFor.isUnpickable(); }
    /** Sets the picking status of this point */
    public void setUnpickable(boolean b) { proxyFor.setUnpickable(b); }

    /** Indicates whether this point is a "ghost" for Mage */
    public boolean isGhost() { return proxyFor.isGhost(); }
    /** Sets the ghost status of this point */
    public void setGhost(boolean b) { proxyFor.setGhost(b); }

    /** Returns the color of this point, or null if it inherits from its list */
    public KPaint getColor() { return proxyFor.getColor(); }
    /** Sets the color of this point. */
    public void setColor(KPaint c) { proxyFor.setColor(c); }
    /** Gets the aspect string of this point */
    public String getAspects() { return proxyFor.getAspects(); }
    /** Sets the aspect string of this point */
    public void setAspects(String a) { proxyFor.setAspects(a); }
    
    /** Gets the line width of this point, if applicable */
    //public int getWidth() { return proxyFor.getWidth(); }
    /** Sets the line width of this point, if applicable */
    //public void setWidth(int w) { proxyFor.setWidth(w); }
    
    /** Gets the radius of this point, if applicable */
    public float getRadius() { return proxyFor.getRadius(); }
    /** Sets the radius of this point, if applicable */
    public void setRadius(float radius) { proxyFor.setRadius(radius); }
    
    /** Sets the point comment for this point. */
    public void setComment(String cmt) { proxyFor.setComment(cmt); }
    /** Gets the comment for this point, which defaults to null. */
    public String getComment() { return proxyFor.getComment(); }
//}}}

//{{{ getDrawingColor
//##################################################################################################
    /** Returns the color that will be used to draw this point (ignoring aspects). Never null. */
    public KPaint getDrawingColor() { return proxyFor.getDrawingColor(); }

    /** Returns the color that will be used to draw this point, taking aspects into account. Never null. */
    public KPaint getDrawingColor(Engine engine) { return proxyFor.getDrawingColor(engine); }
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
    public void pmHit(int mask, int offmask, boolean turnon) { proxyFor.pmHit(mask, offmask, turnon); }
    
    /** Indicates whether or not the given pointmaster set would affect this point. */
    public boolean pmWouldHit(int mask) { return proxyFor.pmWouldHit(mask); }
    
    public int getPmMask() { return proxyFor.getPmMask(); }
    
    public void setPmMask(int mask) { proxyFor.setPmMask(mask); }
//}}}
    
//{{{ calcBoundingBox, calcRadiusSq
//##################################################################################################
    /**
    * Gets a bounding box for the current model.
    * @param bound the first 6 elements get set to { minX, minY, minZ, maxX, maxY, maxZ }.
    * Should be called with { +inf, +inf, +inf, -inf, -inf, -inf }
    */
    public void calcBoundingBox(float[] bound)
    {
        syncCoords(); // just to be safe
        super.calcBoundingBox(bound);
    }

    /**
    * Gets the square of the radius of this model from the specified center.
    * @param center an array with the x, y,  and z coordinates of the center
    * @return the square of the radius of this element, centered at center
    */
    public float calcRadiusSq(float[] center)
    {
        syncCoords(); // just to be safe
        return super.calcRadiusSq(center);
    }
//}}}
    
//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform)
    {
        syncCoords(); // just to be safe
        super.doTransform(engine, xform);
    }

    public void doTransform(Engine engine, Transform xform, double zoom)
    {
        syncCoords(); // just to be safe
        super.doTransform(engine, xform, zoom);
    }
//}}}

//{{{ paint2D, isPickedBy
//##################################################################################################
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paint2D(Engine2D engine)
    {
        super.paint2D(engine);
    }

    /**
    * Returns true if the specified pick hits this point, else returns false
    * Pays no attention to whether this point is marked as unpickable.
    * @param radius the desired picking radius
    * @param objPick whether we should try to pick solid objects in addition to points
    * @return the KPoint that should be counted as being picked, or null for none.
    *   Usually <code>this</code>, but maybe not for object picking.
    */
    public KPoint isPickedBy(float xx, float yy, float radius, boolean objPick)
    {
        //syncCoords(); // just to be safe
        return super.isPickedBy(xx, yy, radius, objPick);
    }
//}}}

//{{{ get/setName, get/setParent, toString
//##################################################################################################
    /** Gets the name of this element */
    public String getName() { return proxyFor.getName(); }
    /** Sets the name of this element */
    public void setName(String nm)
    {
        if(proxyFor == null) super.setName(nm); // called by constructor
        else proxyFor.setName(nm);
    }

    /** Determines the owner (parent) of this element */
    public KList getParent()
    {
        if(proxyFor == null) return super.getParent(); // called by constructor
        else return proxyFor.getParent();
    }
    /** Establishes the owner (parent) of this element */
    public void setParent(KList owner)
    {
        if(proxyFor == null) super.setParent(owner); // called by constructor
        else proxyFor.setParent(owner);
    }
    
    /** Gets the name of this element (same as <code>getName()</code>*/
    public String toString() { return proxyFor.toString(); }
//}}}

//{{{ getKinemage
//##################################################################################################
    /** Retrieves the Kinemage object holding this element, or null if none. */
    public Kinemage getKinemage() { return proxyFor.getKinemage(); }
//}}}

//{{{ isVisible, isTotallyOn
//##################################################################################################
    /** Indicates whether this element will actually be painted (i.e., is it and all its parents on?) */
    public boolean isVisible() { return proxyFor.isVisible(); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

