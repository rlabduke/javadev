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
* <code>VectorPoint</code> represents the endpoint of a line.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Apr 26 16:46:09 EDT 2002
*/
public class VectorPoint extends AbstractPoint // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    protected VectorPoint   from    = null;
    protected int           width   = 0;        // width of this line (0 => use parent.width)
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new data point representing one end of a line.
    *
    * @param label the pointID of this point
    * @param start where this line is drawn from, or null if it's the starting point
    */
    public VectorPoint(String label, VectorPoint start)
    {
        super(label);
        setPrev(start);
    }
//}}}

//{{{ get/setPrev, isBreak, get/setWidth
//##################################################################################################
    public void setPrev(KPoint pt)
    {
        super.setPrev(pt);
        from = (VectorPoint)pt;
    }
    
    public VectorPoint getPrev()
    { return from; }
    
    public boolean isBreak()
    { return (from == null); }

    public int getWidth()
    { return width; }
    
    public void setWidth(int w)
    {
        if(w > 7)       width = 7;
        else if(w < 0)  width = 0;
        else            width = w;
        fireKinChanged(CHANGE_POINT_PROPERTIES);
    }
//}}}

//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform, double zoom)
    {
        // Don't call super.doTransform() b/c we do it all here
        
        if(parent.getScreen())
        {
            double width  = engine.pickingRect.getWidth();
            double height = engine.pickingRect.getHeight();
            double x = width /2 + getX()/200.0 * Math.min(width, height)/2;
            double y = height/2 - getY()/200.0 * Math.min(width, height)/2;
            setDrawXYZ(new Triple(x, y, getZ()));
        }
        else
        {
            xform.transform(this, engine.work1);
            setDrawXYZ(engine.work1);
        }
        
        // This only works because starting points are listed before ending points
        // in a kinemage, thus, from has already been transformed when we get here!
        // The idea is to add points based on the midpoint of the *visible* range.
        if(from != null)
        {
          KList par = (KList) this.getParent();
          if (par != null && par.getRear() == true) {
            if (from.z < z && from.z <= engine.clipFront && z >= engine.clipBack) {
              engine.addPaintable(this, Math.max(from.z, engine.clipBack)-1);
            } else if (from.z >= engine.clipBack && z <= engine.clipFront) {
              engine.addPaintable(this, Math.max(z, engine.clipBack)-1);
            }
            
          } else if (par != null && par.getFore() == true) {
            if (from.z < z && from.z <= engine.clipFront && z >= engine.clipBack) {
              engine.addPaintable(this, Math.min(z, engine.clipFront)+1);
            } else if (from.z >= engine.clipBack && z <= engine.clipFront) {
              engine.addPaintable(this, Math.min(from.z, engine.clipFront)+1);
            }
            
          } else {
            if(from.z < z && from.z <= engine.clipFront && z >= engine.clipBack)
            {
                engine.addPaintable(this,
                    (Math.max(from.z, engine.clipBack)+Math.min(z, engine.clipFront)) / 2.0);
            }
            else if(from.z >= engine.clipBack && z <= engine.clipFront) // && from.z >= z
            {
                engine.addPaintable(this,
                    (Math.max(z, engine.clipBack)+Math.min(from.z, engine.clipFront)) / 2.0);
            }
            // else don't paint
          }
        }
        else engine.addPaintable(this, z); // won't be painted, but will be pickable
    }
//}}}

//{{{ isPickedBy
//##################################################################################################
    public KPoint isPickedBy(float xx, float yy, float radius, boolean objPick)
    {
        if(objPick && from != null)
        {
            VectorPoint A = this, B = from;
            // first check: bounding box
            if(xx > (Math.min(A.x,B.x) - radius) && xx < (Math.max(A.x,B.x)+radius)
            && yy > (Math.min(A.y,B.y) - radius) && yy < (Math.max(A.y,B.y)+radius))
            {
                // line as ax + by + d = 0, like a plane
                float a = B.y - A.y, b = A.x - B.x;
                double num = a*xx + b*yy - (a*A.x + b*A.y); // parenthesized term is -d
                double d2 = (num*num) / (a*a + b*b); // square of distance to the line
                //System.err.println("x = "+xx+" : "+Math.min(A.x,B.x)+" - "+Math.max(A.x,B.x));
                //System.err.println("y = "+yy+" : "+Math.min(A.y,B.y)+" - "+Math.max(A.y,B.y));
                //System.err.println("a = "+a+"; b = "+b+"; d = "+(-(a*A.x + b*A.y)));
                //System.err.println("D^2 = "+d2);
                // Always return the "line to" point, so that color changes work as expected
                if(d2 < radius*radius) return this;
                /*{
                    float dx, dy, dA, dB;
                    dx = xx - A.x; dy = yy - A.y; dA = dx*dx + dy*dy;
                    dx = xx - B.x; dy = yy - B.y; dB = dx*dx + dy*dy;
                    if(dA <= dB)    return A;
                    else            return B;
                }*/
            }
        }
        
        // Not else {...} b/c this can be true for line ends even outside the bounding box:        
        return super.isPickedBy(xx, yy, radius, objPick);
    }
//}}}

//{{{ paint2D
//##################################################################################################
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paint2D(Engine2D engine)
    {
        //if(from == null || equals(from)) return;
        if(from == null || (x0 == from.x0 && y0 == from.y0 && z0 == from.z0)) return;
        
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
        
        // [IWD, 4 May 2005]
        // For Mage/Prekin, it's enough for EITHER end of the line to be off to
        // not draw it. The line below could be replaced with a custom pmHit()
        // that ORs this.pm_mask with from.pm_mask before deciding on a hit.
        // That would be faster (precomputed) but seems riskier at the moment...
        //
        // Oops, the real problem is the FROM side not being marked with a pointmaster,
        // which means... what? How does this end up being a problem again?
        //
        //    if(from.getDrawingColor(engine).isInvisible()) return;
        
        //Paint paint = maincolor.getPaint(engine.backgroundMode, engine.colorCue);
        int alpha = (parent == null ? 255 : parent.getAlpha());
        Paint paint = maincolor.getPaint(engine.backgroundMode, 1, engine.colorCue, alpha);
        
        // If we REALLY wanted to clip line segments to the visible volume, we
        // could use 6 planes defining a box / truncated pyramid.
        // See ArrowPoint for ideas on Cohen-Sutherland clipping.
        // To intersect a line with a plane, from Comp.Graphics.Algorithms FAQ 5:
        //  If the plane is defined as:
        //      a*x + b*y + c*z + d = 0
        //  and the line is defined as:
        //      x = x1 + (x2 - x1)*t = x1 + i*t
        //      y = y1 + (y2 - y1)*t = y1 + j*t
        //      z = z1 + (z2 - z1)*t = z1 + k*t
        //  Then just substitute these into the plane equation. You end up with:
        //      t = - (a*x1 + b*y1 + c*z1 + d)/(a*i + b*j + c*k)
        //  When the denominator is zero, the line is contained in the plane if
        //  the numerator is also zero (the point at t=0 satisfies the plane
        //  equation), otherwise the line is parallel to the plane.
        
        //{{{ Determine who's in back and who's in front.
        double xb, yb, zb, shortenb, xf, yf, zf, shortenf; // Back and Front
        boolean fromIsFront; // are the "f" (front) points this or this.from?
        if(from.z < z)
        {
            fromIsFront = false;
            xb = from.x;
            yb = from.y;
            zb = from.z;
            shortenb = engine.getShortening(from);
            xf = x;
            yf = y;
            zf = z;
            shortenf = engine.getShortening(this);
        }
        else // from.z >= z
        {
            fromIsFront = true;
            xf = from.x;
            yf = from.y;
            zf = from.z;
            shortenf = engine.getShortening(from);
            xb = x;
            yb = y;
            zb = z;
            shortenb = engine.getShortening(this);
        }
        //}}} Determine who's in back and who's in front.

        //{{{ Shorten to fit in clipping plane, outside of balls
        // If line ends extend outside clipping area, calc. its intersection w/ clipping plane
        // If a ball resides at one end point, shorten the line by the radius of the ball
        double dz, dxdz, dydz, dzb, dzf, sx, sy, sz, s;
        dz = zf - zb;
        dxdz = (xf - xb)/dz; // == (dx/dz)
        dydz = (yf - yb)/dz; // == (dy/dz)
        dzb = engine.clipBack - zb;
        dzf = engine.clipFront - zf;
        
        // Clipping or shortening for back point.
        if(shortenb > 0)
        {
            s = Math.sqrt(shortenb*shortenb / ((dxdz*dxdz + dydz*dydz + 1)*dz*dz));
            sz =  s*dz;
            sx = sz*dxdz;
            sy = sz*dydz;
            if(sz > dzb)
            {
                xb += sx;
                yb += sy;
                zb += sz;
            }
            else if(zb < engine.clipBack)
            {
                xb = xb + dxdz*dzb;
                yb = yb + dydz*dzb;
                zb = engine.clipBack;
            }
        }
        else if(zb < engine.clipBack)
        {
            xb = xb + dxdz*dzb;
            yb = yb + dydz*dzb;
            zb = engine.clipBack;
        }
        
        // Clipping or shortening for front point.
        if(shortenf > 0)
        {
            s = Math.sqrt(shortenf*shortenf / ((dxdz*dxdz + dydz*dydz + 1)*dz*dz));
            sz =  s*dz;
            sx = sz*dxdz;
            sy = sz*dydz;
            if(sz > -dzf)
            {
                xf -= sx;
                yf -= sy;
                zf -= sz;
            }
            else if(zf > engine.clipFront)
            {
                xf = xf + dxdz*dzf;
                yf = yf + dydz*dzf;
                zf = engine.clipFront;
            }
        }
        else if(zf > engine.clipFront)
        {
            xf = xf + dxdz*dzf;
            yf = yf + dydz*dzf;
            zf = engine.clipFront;
        }
        //}}} Shorten to fit in clipping plane, outside of balls
    
        //engine.painter.paintVector(paint, calcLineWidth(engine), engine.widthCue,
        //    xb, yb, zb, xf, yf, zf);
        
        if(fromIsFront) paintStandard2(engine, paint, xf, yf, zf, xb, yb, zb);
        else            paintStandard2(engine, paint, xb, yb, zb, xf, yf, zf);
    }
//}}}

//{{{ paintStandard2
//##################################################################################################
    /**
    * This function exists solely for the convenience of ArrowPoints;
    * a good JIT will optimize it away for VectorPoints.
    * Coordinates are already transformed, perspective corrected, and clipped by Z planes.
    * They have NOT been clipped to the drawing area yet.
    */
    protected void paintStandard2(Engine2D engine, Paint paint, double fromX, double fromY, double fromZ, double toX, double toY, double toZ)
    {
        int lineWidth = calcLineWidth(engine);
        engine.painter.paintVector(paint, lineWidth, engine.widthCue,
            fromX, fromY, fromZ, toX, toY, toZ);
    }
//}}}

//{{{ calcLineWidth
//##################################################################################################
    /** Vector way of finding the right line width to use, given the settings in the engine */
    protected int calcLineWidth(Engine engine)
    {
        if(engine.thinLines) return 1;
        
        int wid = 2;
        if(this.width > 0)      wid = this.width;
        else if(parent != null) wid = parent.getWidth();
        
        return wid;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

