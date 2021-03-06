// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
import java.awt.geom.*;
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
* <code>RingPoint</code> represents a screen-oriented annulus around a particular point.
* Ring size scales up and down like balls; i.e. it's a real size rather than a display size.
* It implements Mage ringlists.
*
* <p>Begun on Sat Apr 27 11:02:02 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class RingPoint extends AbstractPoint // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    public float r0 = 0f, r = 0f; // radius ( r0 >= 0 => use list radius )
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new data point representing one end of a line.
    *
    * @param list the list that contains this point
    * @param label the pointID of this point
    */
    public RingPoint(KList list, String label)
    {
        super(list, label);
    }
//}}}
    
//{{{ get/setRadius
//##################################################################################################
    /** Sets the radius of this point, if applicable */
    public void setRadius(float radius)
    {
        if(radius >= 0) r0 = radius;
    }
    
    public float getRadius()
    { return r0; }
//}}}

//{{{ signalTransform
//##################################################################################################
    /** Rings require a zoom factor, so this throws UnsupportedOperationException */
    public void signalTransform(Engine engine, Transform xform)
    { throw new UnsupportedOperationException(this.getClass()+".signalTransform() requires a zoom factor"); }
    
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
        // Don't call super.signalTransform() b/c we do it all here
        
        if(r0 <= 0 && parent != null) r = (float)(parent.radius * zoom);
        else                          r = (float)(r0 * zoom);
        xform.transform(this, engine.work1);
        setDrawXYZ(engine.work1);

        if(engine.usePerspective)
        {
            // multiply radius by perspDist/(perspDist - originalZ)
            // This is a very subtle effect -- barely notable.
            r *= (engine.perspDist) / (engine.perspDist - z);
            
            // This is the old code -- seems to be wrong. (031017)
            //r *= (engine.perspDist + z) / engine.perspDist;
        }
        
        // Can't handle (artificial) thickness cues here
        // b/c engine.widthCue isn't set yet.
        
        engine.addPaintable(this, z);
        
        // Rings don't do line shortening around them -- the point is to see the center.
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
    {
        float dx, dy;
        dx = (x - xx);
        dy = (y - yy);

        //if( Math.abs( Math.sqrt(dx*dx + dy*dy) - r ) <= radius ) return this;
        float sqDist = (dx*dx + dy*dy);
        float minDist = r - radius; if(minDist > 0) minDist *= minDist;
        float maxDist = r + radius; maxDist *= maxDist;
        if(minDist <= sqDist && sqDist <= maxDist)  return this;
        else                                        return null;
    }
//}}}

//{{{ paintStandard
//##################################################################################################
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paintStandard(Engine engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
        int alpha = (parent == null ? 255 : parent.alpha);
        Paint paint = maincolor.getPaint(engine.backgroundMode, 1, engine.colorCue, alpha);
        
        // For now we ignore the linewidth issue
        double d = 2*r;
        engine.painter.drawOval(paint, calcLineWidth(engine), engine.widthCue, x, y, z, d, d);
    }
//}}}

//{{{ calcLineWidth
//##################################################################################################
    // Default way of finding the right line width to use, given the settings in the engine
    int calcLineWidth(Engine engine)
    {
        if(engine.thinLines)    return 1;
        else if(parent != null) return parent.width;
        else                    return 2;
    }
//}}}
}//class
