// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

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
* <code>ProxyPoint</code> implements all the functions of KPoint by delegating
* them to another KPoint.
*
* <p>Copyright (C) 2004-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Oct 18 09:33:32 EDT 2004
*/
class ProxyPoint implements KPoint
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KPoint      proxyFor;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ProxyPoint(KPoint proxyFor)
    {
        super();
        this.proxyFor   = proxyFor;
    }
//}}}

//{{{ clone
//##################################################################################################
    public KPoint clone() { return proxyFor.clone(); }
//}}}

//{{{ get/setX/Y/Z
//##################################################################################################
    /** Returns the untransformed coordinate for this point */
    public double getX() { return proxyFor.getX(); }
    /** Returns the untransformed coordinate for this point */
    public double getY() { return proxyFor.getY(); }
    /** Returns the untransformed coordinate for this point */
    public double getZ() { return proxyFor.getZ(); }

    /** Assigns a value to the untransformed coordinate for this point */
    public void setX(double xx) { proxyFor.setX(xx); }
    /** Assigns a value to the untransformed coordinate for this point */
    public void setY(double yy) { proxyFor.setY(yy); }
    /** Assigns a value to the untransformed coordinate for this point */
    public void setZ(double zz) { proxyFor.setZ(zz); }
    /** Assigns a value to the untransformed coordinates for this point */
    public void setXYZ(double xx, double yy, double zz) { proxyFor.setXYZ(xx, yy, zz); }
//}}}

//{{{ get/setDrawX/Y/Z
//##################################################################################################
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawX() { return proxyFor.getDrawX(); }
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawY() { return proxyFor.getDrawY(); }
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawZ() { return proxyFor.getDrawZ(); }
//}}}

//{{{ get/setAllCoords, useCoordsXYZ
//##################################################################################################
    /**
    * Stores an array of coordinates for "high-dimensional" points.
    * The float[] is stored without cloning and so is subject to overwrite.
    */
    public void setAllCoords(float[] coords) { proxyFor.setAllCoords(coords); }
    
    /**
    * Retrieves the "high-dimensional" coordinates of this point, or null if not set.
    * The float[] is returned without cloning and so is subject to overwrite.
    */
    public float[] getAllCoords() { return proxyFor.getAllCoords(); }
    
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
    public void setPrev(KPoint pt) { proxyFor.setPrev(pt); }
    
    /**
    * Gets the point preceding this one in the chain.
    * @return the preceding point, or null if (a) this is a break in the chain or (b) this is not a chainable point type.
    */
    public KPoint getPrev() { return proxyFor.getPrev(); }
    
    /**
    * True iff this is a chainable point type (e.g. vector, triangle) AND there is a chain break.
    */
    public boolean isBreak() { return proxyFor.isBreak(); }
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
    public int getWidth() { return proxyFor.getWidth(); }
    /** Sets the line width of this point, if applicable */
    public void setWidth(int w) { proxyFor.setWidth(w); }
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
    public void calcBoundingBox(float[] bound) { proxyFor.calcBoundingBox(bound); }

    /**
    * Gets the square of the radius of this model from the specified center.
    * @param center an array with the x, y,  and z coordinates of the center
    * @return the square of the radius of this element, centered at center
    */
    public float calcRadiusSq(float[] center) { return proxyFor.calcRadiusSq(center); }
//}}}
    
//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform) { proxyFor.doTransform(engine, xform); }

    public void doTransform(Engine engine, Transform xform, double zoom) { proxyFor.doTransform(engine, xform, zoom); }
//}}}

//{{{ paint2D, isPickedBy
//##################################################################################################
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paint2D(Engine2D engine) { proxyFor.paint2D(engine); }

    /**
    * Returns true if the specified pick hits this point, else returns false
    * Pays no attention to whether this point is marked as unpickable.
    * @param radius the desired picking radius
    * @param objPick whether we should try to pick solid objects in addition to points
    * @return the KPoint that should be counted as being picked, or null for none.
    *   Usually <code>this</code>, but maybe not for object picking.
    */
    public KPoint isPickedBy(float xx, float yy, float radius, boolean objPick) { return proxyFor.isPickedBy(xx, yy, radius, objPick); }
//}}}

//{{{ get/setName, get/setOwner, toString
//##################################################################################################
    /** Gets the name of this element */
    public String getName() { return proxyFor.getName(); }
    /** Sets the name of this element */
    public void setName(String nm) { proxyFor.setName(nm); }

    /** Determines the owner (parent) of this element */
    public KList getParent() { return proxyFor.getParent(); }
    /** Establishes the owner (parent) of this element */
    public void setParent(KList owner) { proxyFor.setParent(owner); }
    
    /** Gets the name of this element (same as <code>getName()</code>*/
    public String toString() { return proxyFor.toString(); }
//}}}

//{{{ getKinemage, getDepth, fireKinChanged
//##################################################################################################
    /** Retrieves the Kinemage object holding this element, or null if none. */
    public Kinemage getKinemage() { return proxyFor.getKinemage(); }
    
    public int getDepth() { return proxyFor.getDepth(); }
    
    public void fireKinChanged(int event) { proxyFor.fireKinChanged(event); }
//}}}

//{{{ isVisible
//##################################################################################################
    /** Indicates whether this element will actually be painted (i.e., is it and all its parents on?) */
    public boolean isVisible() { return proxyFor.isVisible(); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

