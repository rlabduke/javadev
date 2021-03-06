// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

import java.awt.*;
import java.awt.geom.*;
import java.lang.Math; // (ARK Spring2010)
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
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class TrianglePoint extends AbstractPoint // implements ...
{
//{{{ Static fields
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
    * @param label the pointID of this point
    * @param start where this line is drawn from, or null if it's the starting point
    */
    public TrianglePoint(String label, TrianglePoint start)
    {
        super(label);
        setPrev(start);
    }
//}}}

//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform, double zoom)
    {
        // Don't call super.doTransform() b/c we do it all here
        
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

//{{{ paint2D
//##################################################################################################
    public void paint2D(Engine2D engine)
    {
    	boolean onBackside = false; // (ARK Spring2010)
    	boolean doBackside = false;
    	if(parent.secondaryStructure!=null)
    		if(parent.secondaryStructure.equals("alpha") && engine.ribbonSidesAlpha
		   || parent.secondaryStructure.equals("beta") && engine.ribbonSidesBeta)
		{ doBackside = true; }
    	
        KPaint maincolor = getDrawingColor(engine);
        if(from == null || from.from == null || maincolor.isInvisible()) return;
        
        TrianglePoint A, B, C = from.from.from;  
        TrianglePoint A2, B2, C2 = from.from; // for the other triangle in the pair, (ARK Spring2010)  
        int colorCue = engine.colorCue;
        // If this is a ribbon list, color the triangles in pairs (code for dependent triangles)
        if((multi & SEQ_EVEN_BIT) != 0 && parent != null && parent.getType() == KList.RIBBON && C != null){ // added doBackside (ARK Spring2010)
	    A = from;
            B = from.from;
            // C = from.from.from; -- already set
            A2 = this;	// (ARK Spring2010)
            B2 = from;	// (ARK Spring2010)
            
            // We must match depth cueing AND lighting angle if we want ribbons to look uniform
            // This is a huge pain in the butt -- code derived from doTransform().
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
            A2 = null; // (ARK Spring2010)
            B2 = null; // (ARK Spring2010)
        }
        
        // Do dot product of surface normal with lighting vector
        // to determine diffuse lighting.
        //engine.work1.likeVector(B, A);
        engine.work1.setXYZ( A.getDrawX()-B.getDrawX(), A.getDrawY()-B.getDrawY(), A.getDrawZ()-B.getDrawZ() );
        //engine.work2.likeVector(B, C);
        engine.work2.setXYZ( C.getDrawX()-B.getDrawX(), C.getDrawY()-B.getDrawY(), C.getDrawZ()-B.getDrawZ() );
        engine.work1.cross(engine.work2).unit();
        
        double dotprod = engine.work1.dot(engine.lightingVector);
        
        int alpha = (parent == null ? 255 : parent.getAlpha());
        Paint paint = maincolor.getPaint(engine.backgroundMode, dotprod, colorCue, alpha); 
 
        // Ribbon backside considerations, (ARK Spring2010)
        // Look at pair of triangles separately
        if(doBackside && (multi & SEQ_EVEN_BIT) != 0 && parent != null && parent.getType() == KList.RIBBON && C2 != null){
        	engine.work1.setXYZ( A2.getDrawX()-B2.getDrawX(), A2.getDrawY()-B2.getDrawY(), A2.getDrawZ()-B2.getDrawZ() );
        	engine.work2.setXYZ( C2.getDrawX()-B2.getDrawX(), C2.getDrawY()-B2.getDrawY(), C2.getDrawZ()-B2.getDrawZ() );
        	engine.work1.cross(engine.work2).unit();  // surface normal of other triangle in pair
        	if(engine.work1.getZ()<=0.001) onBackside = true;
        }
        else if( doBackside ) 
		if(engine.work1.getZ()>=0.001) onBackside = true;  
        if(doBackside && onBackside) {
        	if(parent.getSecStruc().equals("alpha"))
        		paint = maincolor.getOffsetPaint(engine.curBackHSValpha, engine.backgroundMode, dotprod, colorCue, alpha); 
        	else if(parent.getSecStruc().equals("beta"))
        		paint = maincolor.getOffsetPaint(engine.curBackHSVbeta, engine.backgroundMode, dotprod, colorCue, alpha); 
        	else {} // revert to leaving the paint unchanged
        } // end backside
        
        engine.painter.paintTriangle(paint,
            x, y, z, 
            from.x, from.y, from.z,
            from.from.x, from.from.y, from.from.z
        );

    }
//}}}

//{{{ get/setPrev, isBreak
//##################################################################################################
    public void setPrev(KPoint pt)
    {
        super.setPrev(pt);
        from = (TrianglePoint)pt;
    }
    
    public TrianglePoint getPrev()
    { return from; }
    
    public boolean isBreak()
    { return (from == null); }
//}}}
}//class
