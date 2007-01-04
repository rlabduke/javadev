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
* <code>BallPoint</code> represents a 3-D sphere.
* It implements Mage balllists.
*
* <p>Begun on Sat Apr 27 11:02:02 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class BallPoint extends RingPoint // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new data point representing one end of a line.
    *
    * @param list the list that contains this point
    * @param label the pointID of this point
    */
    public BallPoint(KList list, String label)
    {
        super(list, label);
    }
//}}}
    
//{{{ signalTransform
//##################################################################################################
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
        // RingPoint signal transform handles most stuff
        super.signalTransform(engine, xform, zoom);

        // We still need to ask for line shortening, though
        // Otherwise we create gaps around lines when our pointmaster is off.
        // If ball is translucent, we should see line going to its center (?)
        if(!this.getDrawingColor(engine).isInvisible() && (parent == null || parent.alpha > 192)) engine.addShortener(this, r);
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
        // We have this goofy call to the AbstractPoint implementation
        // because Java doesn't allow super.super.foo()
        return _isPickedBy(xx, yy, Math.max(radius, r), objPick);

        // Balls should always act in "object pick" mode
        //if(objPick) return super.isPickedBy(xx, yy, Math.max(radius, r), objPick);
        //else        return super.isPickedBy(xx, yy, radius, objPick);
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

        // We *can* do extra depth cueing hints for balls, but I prefer not to.
        // It works for small, isolated balls (e.g. waters), but it is confusing
        // and downright misleading when many balls are close together, because
        // the sizes of the balls are consistently and substantially inflated.
        //
        // We have to do this here b/c now widthCue is set
        //if(engine.cueThickness) r *= KPalette.widthScale[ engine.widthCue ];
        
        engine.painter.paintBall(paint, x, y, z, r, ((parent.flags & KList.NOHILITE) == 0));
    }
//}}}
}//class
