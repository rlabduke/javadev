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
import driftwood.r3.*;
//}}}
/**
* <code>KPoint</code> is a generic, non-instantiable representation of a 'point' in a kinemage.
* This class and its subclasses contain the code for drawing all the different graphics primitives in Mage.
*
* <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:57:57 EDT 2002
*/
public interface KPoint extends AHE, Cloneable, MutableTuple3
{
//{{{ Constants
    // Bit allocation for 'multi':
    //   kngpt  points  future  color (no longer used)
    // skkkkkkkppppppppffffffffcccccccc
    
    /** A mask for isolating the color bits using AND */
    //public static final int COLOR_MASK      = 0x000000ff;
    /** If this bit is set, the point is 'live' and should be painted */
    public static final int ON_BIT          = 0x40000000;
    /** If this bit is set, the point will not be picked by a mouse click */
    public static final int UNPICKABLE      = 0x20000000;
    /** Used by e.g. TrianglePoints to tell whose normal to in lighting effects */
    public static final int SEQ_EVEN_BIT    = 0x10000000;
    /** A flag used by Mage only; point is visible but not written to PDB output. */
    public static final int GHOST_BIT       = 0x08000000;
//}}}

//{{{ clone
//##################################################################################################
    public Object clone();
//}}}

//{{{ get/setOrigX/Y/Z
//##################################################################################################
    /** Returns the untransformed coordinate for this point.
    * @deprecated In favor of getX(). */
    public float getOrigX();
    /** Returns the untransformed coordinate for this point.
    * @deprecated In favor of getY(). */
    public float getOrigY();
    /** Returns the untransformed coordinate for this point.
    * @deprecated In favor of getZ). */
    public float getOrigZ();

    /** Assigns a value to the untransformed coordinate for this point.
    * @deprecated In favor of setX(). */
    public void setOrigX(double xx);
    /** Assigns a value to the untransformed coordinate for this point.
    * @deprecated In favor of setY(). */
    public void setOrigY(double yy);
    /** Assigns a value to the untransformed coordinate for this point.
    * @deprecated In favor of setZ(). */
    public void setOrigZ(double zz);
    /** Assigns a value to the untransformed coordinates for this point.
    * @deprecated In favor of setXYZ(). */
    public void setOrigXYZ(Tuple3 t);
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

    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    public void setDrawX(double xx);
    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    public void setDrawY(double yy);
    /** Assigns a value to the fully transformed (drawing) coordinate for this point */
    public void setDrawZ(double zz);
    /** Assigns a value to the fully transformed (drawing) coordinates for this point */
    public void setDrawXYZ(Tuple3 t);
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
    
//{{{ signalTransform
//##################################################################################################
    public void signalTransform(Engine engine, Transform xform);

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
    public void signalTransform(Engine engine, Transform xform, double zoom);
//}}}

//{{{ paintStandard, isPickedBy
//##################################################################################################
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paintStandard(Engine engine);

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

