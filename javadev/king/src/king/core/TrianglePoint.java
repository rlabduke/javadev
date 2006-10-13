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
public class TrianglePoint extends AbstractPoint // implements ...
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
        
        xform.transform(this, engine.work1);
        setDrawXYZ(engine.work1);

        double triangleZ;
        if(from == null || from.from == null)   triangleZ = z;
        //else                                    triangleZ = (z + from.z + from.from.z)/3.0;
        // Sort by average of two backmost vertices (midpoint of back edge).
        // This helps for triangles "outlined" by vectors, because if the vectors will always
        // sort in front of or equal to the triangle, so if they come after the triangles
        // in the kinemage, they'll always be visible. Helps with e.g. protein ribbons.
        else
        {
            if(z < from.z)
            {
                if(from.z < from.from.z)    triangleZ = (z + from.z)/2.0;
                else                        triangleZ = (z + from.from.z)/2.0;
            }
            else
            {
                if(z < from.from.z)         triangleZ = (z + from.z)/2.0;
                else                        triangleZ = (from.z + from.from.z)/2.0;
            }
        }
        engine.addPaintable(this, triangleZ);
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
        if(objPick && from != null && from.from != null)
        {
            // deliberately using transformed coordinates, b/c they're projected flat
            TrianglePoint A = this, B = from, C = from.from;
            // first, make sure this is really a triangle, i.e. A != B != C
            // otherwise, the signed area is always zero and it looks like we hit the edge
            if(!((A.x == B.x && A.y == B.y) || (B.x == C.x && B.y == C.y) || (C.x == A.x && C.y == A.y)))
            {
                // then, do Andrew Ban's nifty intersection test
                if(Builder.checkTriangle(xx, yy, A.x, A.y, B.x, B.y, C.x, C.y))
                    return this; // always this, so changing colors works as expected
                /*{
                    float dx, dy, dA, dB, dC;
                    dx = xx - A.x; dy = yy - A.y; dA = dx*dx + dy*dy;
                    dx = xx - B.x; dy = yy - B.y; dB = dx*dx + dy*dy;
                    dx = xx - B.x; dy = yy - C.y; dC = dx*dx + dy*dy;
                    if(dA <= dB && dA <= dC)    return A;
                    else if(dB <= dC)           return B;
                    else                        return C;
                }*/
            }
        }
        
        return super.isPickedBy(xx, yy, radius, objPick);
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
        if(from == null || from.from == null || maincolor.isInvisible()) return;
        
        TrianglePoint A, B, C = from.from.from;
        int colorCue = engine.colorCue;
        // If this is a ribbon list, color the triangles in pairs (code for dependent triangles)
        if((multi & SEQ_EVEN_BIT) != 0 && parent != null && parent.getType() == KList.RIBBON && C != null)
        {
            A = from;
            B = from.from;
            //C = from.from.from; -- already set
            // We must match depth cueing AND lighting angle if we want ribbons to look uniform
            // This is a huge pain in the butt -- code derived from signalTransform().
            double triangleZ;
            if(A.z < B.z)
            {
                if(B.z < C.z)   triangleZ = (A.z + B.z)/2.0;
                else            triangleZ = (A.z + C.z)/2.0;
            }
            else
            {
                if(A.z < C.z)   triangleZ = (A.z + B.z)/2.0;
                else            triangleZ = (B.z + C.z)/2.0;
            }
            // wrong, too simple:
            //colorCue = (int)Math.floor(KPaint.COLOR_LEVELS * (triangleZ - engine.clipBack) / engine.clipDepth);
            // right, multiple round off:
            int i = (int)(engine.TOP_LAYER*(triangleZ-engine.clipBack)/engine.clipDepth);
            colorCue = (KPaint.COLOR_LEVELS*i)/(engine.TOP_LAYER+1); // int division (floor)
            if(colorCue < 0) colorCue = 0;
            else if(colorCue >= KPaint.COLOR_LEVELS) colorCue = KPaint.COLOR_LEVELS-1;
        }
        // Otherwise, color each triangle individually (also independent triangles in ribbons)
        else
        {
            A = this;
            B = from;
            C = from.from;
            //colorCue = engine.colorCue; -- already set
        }

        // Do dot product of surface normal with lighting vector
        // to determine diffuse lighting.
        //engine.work1.likeVector(B, A);
        engine.work1.setXYZ( A.getDrawX()-B.getDrawX(), A.getDrawY()-B.getDrawY(), A.getDrawZ()-B.getDrawZ() );
        //engine.work2.likeVector(B, C);
        engine.work2.setXYZ( C.getDrawX()-B.getDrawX(), C.getDrawY()-B.getDrawY(), C.getDrawZ()-B.getDrawZ() );
        engine.work1.cross(engine.work2).unit();
        double dotprod = engine.work1.dot(engine.lightingVector);
        int alpha = (parent == null ? 255 : parent.alpha);
        Paint paint = maincolor.getPaint(engine.backgroundMode, dotprod, colorCue, alpha);
        
        engine.painter.paintTriangle(paint,
            x, y, z,
            from.x, from.y, from.z,
            from.from.x, from.from.y, from.from.z
        );
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
