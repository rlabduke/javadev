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
* <code>BallPoint</code> represents a 3-D sphere.
* It implements Mage balllists.
*
* <p>Begun on Sat Apr 27 11:02:02 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
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
    * Creates a new data point representing a ball.
    *
    * @param label the pointID of this point
    */
    public BallPoint(String label)
    {
        super(label);
    }
//}}}
    
//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform, double zoom)
    {
        // RingPoint signal transform handles most stuff
        super.doTransform(engine, xform, zoom);

        // We still need to ask for line shortening, though
        // Otherwise we create gaps around lines when our pointmaster is off.
        // If ball is translucent, we should see line going to its center (?)
        if(!this.getDrawingColor(engine).isInvisible() && (parent == null || parent.getAlpha() > 192)) engine.addShortener(this, r);
    }
//}}}

//{{{ isPickedBy
//##################################################################################################
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

//{{{ paint2D
//##################################################################################################
    public void paint2D(Engine2D engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
        int alpha = (parent == null ? 255 : parent.getAlpha());
        Paint paint = maincolor.getPaint(engine.backgroundMode, 1, engine.colorCue, alpha);

        // We *can* do extra depth cueing hints for balls, but I prefer not to.
        // It works for small, isolated balls (e.g. waters), but it is confusing
        // and downright misleading when many balls are close together, because
        // the sizes of the balls are consistently and substantially inflated.
        //
        // We have to do this here b/c now widthCue is set
        //if(engine.cueThickness) r *= KPalette.widthScale[ engine.widthCue ];
        
        engine.painter.paintBall(paint, x, y, z, r, !parent.getNoHighlight());
    }
//}}}
}//class
