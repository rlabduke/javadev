// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
import java.awt.geom.*;
//import java.io.*;
//import java.text.*;
//import java.util.*;
//import javax.swing.*;
import driftwood.r3.*;
//}}}
/**
* <code>TrianglePoint</code> provides filled, shaded triangles for triangle lists and ribbon lists.
* In a list of N points, there are N - 2 triangles: 1-2-3, 2-3-4, 3-4-5, etc.
*
* <p>Begun on Mon Jun 24 21:09:57 EDT 2002
* <br>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
*/
public class TrianglePoint extends KPoint // implements ...
{
//{{{ Static fields
    /** This bit is set if the point is followed by an 'L' or a 'D'; this triangle takes its normal from previous one */
    //static final int LINETO_BIT = 0x00800000;
//}}}

//{{{ Variable definitions
//##################################################################################################
    TrianglePoint from;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a new data point representing one point of a triangle.
    *
    * @param list the list that contains this point
    * @param label the pointID of this point
    * @param start where this line is drawn from, or null if it's the starting point
    */
    public TrianglePoint(KList list, String label, TrianglePoint start)
    {
        super(list, label);
        setPrev(start);
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
        // Don't call super.signalTransform() b/c we do it all here
        
        setXYZ(x0, y0, z0);
        xform.transform(this);

        double triangleZ;
        if(from == null || from.from == null)   triangleZ = z;
        else                                    triangleZ = (z + from.z + from.from.z)/3.0;
        engine.addPaintable(this, triangleZ);
    }
//}}}

//{{{ paintStandard
//##################################################################################################
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paintStandard(Graphics2D g, Engine engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(from == null || from.from == null || maincolor.isInvisible()) return;
        
        // If this is a ribbon list, color the triangles in pairs
        TrianglePoint A, B, C = from.from.from;
        if((multi & SEQ_EVEN_BIT) != 0 && parent != null && parent.getType() == KList.RIBBON && C != null)
        {
            A = from;
            B = from.from;
            //C = from.from.from; -- already set
        }
        else
        {
            A = this;
            B = from;
            C = from.from;
        }

        // Do dot product of surface normal with lighting vector
        // to determine diffuse lighting.
        engine.work1.likeVector(B, A);
        engine.work2.likeVector(B, C);
        engine.work1.cross(engine.work2).unit();
        double dotprod = engine.work1.dot(engine.lightingVector);
        Paint paint = maincolor.getPaint(engine.backgroundMode, dotprod, engine.colorCue, parent.alpha);
        g.setPaint(paint);
        
        xPoints[0] = (int)x;           yPoints[0] = (int)y;
        xPoints[1] = (int)from.x;      yPoints[1] = (int)from.y;
        xPoints[2] = (int)from.from.x; yPoints[2] = (int)from.from.y;
        g.fillPolygon(xPoints, yPoints, 3);
    }
//}}}

//{{{ paintHighQuality
//##################################################################################################
    /**
    * Produces a higher-quality, lower-speed rendering of
    * this paintable. If no such rendering is possible,
    * it should produce the same results as paintStandard()
    */
    public void paintHighQuality(Graphics2D g, Engine engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(from == null || from.from == null || maincolor.isInvisible()) return;
        
        TrianglePoint A, B, C = null;
        A = from;
        B = from.from;
        // If this is a ribbon list, color the triangles in pairs
        if((multi & SEQ_EVEN_BIT) != 0 && parent != null && parent.getType() == KList.RIBBON) C = from.from.from;
        if(C == null) C = this;
        
        // Do dot product of surface normal with lighting vector
        // to determine diffuse lighting.
        engine.work1.likeVector(B, A);
        engine.work2.likeVector(B, C);
        engine.work1.cross(engine.work2).unit();
        double dotprod = engine.work1.dot(engine.lightingVector);
        Paint paint = maincolor.getPaint(engine.backgroundMode, dotprod, engine.colorCue, parent.alpha);
        g.setPaint(paint);
        g.setStroke(KPalette.pen1);

        GeneralPath path = engine.path1;
        path.reset();
        path.moveTo(x,              y);
        path.lineTo(from.x,         from.y);
        path.lineTo(from.from.x,    from.from.y);
        path.closePath();
        g.fill(path);
        g.draw(path);   // closes up the hairline cracks between triangles (?)
    }
//}}}

//{{{ get/setPrev(), isBreak()
//##################################################################################################
    /**
    * Sets the point that precedes this one.
    * This matters to "chainable" points, like vectors and triangles.
    * For other points, it does nothing.
    * @param pt the point preceding this one in seqence
    */
    public void setPrev(KPoint pt)
    {
        super.setPrev(pt);
        from = (TrianglePoint)pt;
    }
    
    /**
    * Gets the point preceding this one in the chain.
    * @return the preceding point, or null if (a) this is a break in the chain or (b) this is not a chainable point type.
    */
    public KPoint getPrev()
    { return from; }
    
    /**
    * True iff this is a chainable point type (e.g. vector, triangle) AND there is a chain break.
    */
    public boolean isBreak()
    { return (from == null); }
//}}}
}//class
