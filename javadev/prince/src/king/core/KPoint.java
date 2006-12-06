// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import driftwood.r3.*;
//}}}
/**
* <code>KPoint</code> is a generic, non-instantiable representation of a 'point' in a kinemage.
* This class and its subclasses contain the code for drawing all the different graphics primitives in Mage.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:57:57 EDT 2002
*/
public interface KPoint extends AHE, Cloneable, MutableTuple3
{
//{{{ Constants
//}}}

//{{{ clone
//##################################################################################################
    public Object clone();
//}}}

//{{{ get/setX/Y/Z
//##################################################################################################
    /** Returns the untransformed coordinate for this point */
    public double getX();
    /** Returns the untransformed coordinate for this point */
    public double getY();
    /** Returns the untransformed coordinate for this point */
    public double getZ();

    /** Assigns a value to the untransformed coordinate for this point */
    public void setX(double xx);
    /** Assigns a value to the untransformed coordinate for this point */
    public void setY(double yy);
    /** Assigns a value to the untransformed coordinate for this point */
    public void setZ(double zz);
    /** Assigns a value to the untransformed coordinates for this point */
    public void setXYZ(double xx, double yy, double zz);
//}}}

//{{{ get/setDrawX/Y/Z
//##################################################################################################
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawX();
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawY();
    /** Returns the fully transformed (drawing) coordinate for this point */
    public float getDrawZ();
//}}}

//{{{ get/setAllCoords, useCoordsXYZ
//##################################################################################################
    /**
    * Stores an array of coordinates for "high-dimensional" points.
    * The float[] is stored without cloning and so is subject to overwrite.
    */
    public void setAllCoords(float[] coords);
    
    /**
    * Retrieves the "high-dimensional" coordinates of this point, or null if not set.
    * The float[] is returned without cloning and so is subject to overwrite.
    */
    public float[] getAllCoords();
    
    /**
    * Copies the high-dimensional coordinates at the specified indices
    * into this point's (untransformed) X, Y, and Z fields.
    * If a index is out of range (0-based), it is ignored and the value is not changed.
    */
    public void useCoordsXYZ(int xIndex, int yIndex, int zIndex);
//}}}

//{{{ get/setPrev, isBreak
//##################################################################################################
    /**
    * Sets the point that precedes this one.
    * This matters to "chainable" points, like vectors and triangles.
    * For other points, it does nothing.
    * @param pt the point preceding this one in seqence
    */
    public void setPrev(KPoint pt);
    
    /**
    * Gets the point preceding this one in the chain.
    * @return the preceding point, or null if (a) this is a break in the chain or (b) this is not a chainable point type.
    */
    public KPoint getPrev();
    
    /**
    * True iff this is a chainable point type (e.g. vector, triangle) AND there is a chain break.
    */
    public boolean isBreak();
//}}}

//{{{ is/get/set{On, Unpickable, Ghost, Color, Aspects, Width, Radius, Comment}
//##################################################################################################
    /** Indicates whether this point can be picked with the mouse */
    public boolean isUnpickable();
    /** Sets the picking status of this point */
    public void setUnpickable(boolean b);

    /** Indicates whether this point is a "ghost" for Mage */
    public boolean isGhost();
    /** Sets the ghost status of this point */
    public void setGhost(boolean b);

    /** Returns the color of this point, or null if it inherits from its list */
    public KPaint getColor();
    /** Sets the color of this point. */
    public void setColor(KPaint c);
    /** Gets the aspect string of this point */
    public String getAspects();
    /** Sets the aspect string of this point */
    public void setAspects(String a);
    
    /** Gets the line width of this point, if applicable */
    public int getWidth();
    /** Sets the line width of this point, if applicable */
    public void setWidth(int w);
    /** Gets the radius of this point, if applicable */
    public float getRadius();
    /** Sets the radius of this point, if applicable */
    public void setRadius(float radius);
    
    /** Sets the point comment for this point. */
    public void setComment(String cmt);
    /** Gets the comment for this point, which defaults to null. */
    public String getComment();
//}}}

//{{{ getDrawingColor
//##################################################################################################
    /** Returns the color that will be used to draw this point (ignoring aspects). Never null. */
    public KPaint getDrawingColor();

    /** Returns the color that will be used to draw this point, taking aspects into account. Never null. */
    public KPaint getDrawingColor(Engine engine);
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
    public void pmHit(int mask, int offmask, boolean turnon);
    
    /** Indicates whether or not the given pointmaster set would affect this point. */
    public boolean pmWouldHit(int mask);
    
    public int getPmMask();
    
    public void setPmMask(int mask);
//}}}
    
//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform);

    /**
    /**
    * A call to this method indicates the object
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
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
    public void doTransform(Engine engine, Transform xform, double zoom);
//}}}

//{{{ paint2D, isPickedBy
//##################################################################################################
    /**
    * Renders this point to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paint2D(Engine2D engine);

    /**
    * Returns true if the specified pick hits this point, else returns false
    * Pays no attention to whether this point is marked as unpickable.
    * @param radius the desired picking radius
    * @param objPick whether we should try to pick solid objects in addition to points
    * @return the KPoint that should be counted as being picked, or null for none.
    *   Usually <code>this</code>, but maybe not for object picking.
    */
    public KPoint isPickedBy(float xx, float yy, float radius, boolean objPick);
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

