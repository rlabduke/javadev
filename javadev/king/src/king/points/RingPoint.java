// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

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
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
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
    * Creates a new data point representing a ring.
    *
    * @param label the pointID of this point
    */
    public RingPoint(String label)
    {
        super(label);
    }
//}}}
    
//{{{ get/setRadius
//##################################################################################################
    /** Sets the radius of this point, if applicable */
    public void setRadius(float radius)
    {
        if(radius >= 0) r0 = radius;
        fireKinChanged(CHANGE_POINT_PROPERTIES);
    }
    
    public float getRadius()
    { return r0; }
    
    public float getDrawRadius()
    { return r; }
//}}}

//{{{ doTransform
//##################################################################################################
    /** Rings require a zoom factor, so this throws UnsupportedOperationException */
    public void doTransform(Engine engine, Transform xform)
    { throw new UnsupportedOperationException(this.getClass()+".doTransform() requires a zoom factor"); }
    
    public void doTransform(Engine engine, Transform xform, double zoom)
    {
        // Don't call super.doTransform() b/c we do it all here
        if (parent.getScreen() == true) {
          //Kinemage ancestor = getKinemage();
          //float span = ancestor.getSpan();
          r = 50;
          super.doTransform(engine, xform, zoom);
        } else {
        if(r0 <= 0 && parent != null) r = (float)(parent.getRadius() * zoom);
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
    }
//}}}

//{{{ isPickedBy
//##################################################################################################
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

//{{{ paint2D
//##################################################################################################
    public void paint2D(Engine2D engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
        int alpha = (parent == null ? 255 : parent.getAlpha());
        Paint paint = maincolor.getPaint(engine.backgroundMode, 1, engine.colorCue, alpha);
        
        // For now we ignore the linewidth issue
        double d = 2*r;
        engine.painter.drawOval(paint, calcLineWidth(engine), engine.widthCue, x, y, z, d, d);
    }
//}}}

//{{{ calcLineWidth
//##################################################################################################
    // Default way of finding the right line width to use, given the settings in the engine
    protected int calcLineWidth(Engine engine)
    {
        if(engine.thinLines)    return 1;
        else if(parent != null) return parent.getWidth();
        else                    return 2;
    }
//}}}
}//class
