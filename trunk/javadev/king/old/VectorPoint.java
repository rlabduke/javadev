// (jEdit options) :folding=explicit:collapseFolds=1:
package king;
import java.awt.*;
import java.awt.geom.*;
//import java.io.*;
//import java.util.*;
//import javax.swing.*;
//import duke.kinemage.util.*;

/**
 * <code>VectorPoint</code> represents the endpoint of a line.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Fri Apr 26 16:46:09 EDT 2002
*/
public class VectorPoint extends KPoint // implements ...
{
//##################################################################################################
    VectorPoint from = null;
    int width = 0; // width of this line (0 => use parent.width)
    float shorten = 0f; // amount to shorten this line (in 3-D pixels), as set by one or more BallPoints
    
//{{{ constructor()
//##################################################################################################
    /**
    * Creates a new data point representing one end of a line.
    *
    * @param list the list that contains this point
    * @param label the pointID of this point
    * @param start where this line is drawn from, or null if it's the starting point
    */
    public VectorPoint(KList list, String label, VectorPoint start)
    {
        super(list, label);
        setPrev(start);
    }
//}}}
    
//{{{ render()
//##################################################################################################
    /**
    * Draws this primitive represented by this point to the specified graphics context.
    *
    * @param g the Graphics2D to draw to
    * @param xoff the offset in x (half the display width)
    * @param yoff the offset in y (half the display height)
    * @param engine the Engine calling this function, which contains many interesting public fields
    */
    public void render(Graphics2D g, float xoff, float yoff, Engine engine)
    {
        /*
        if(from == null || equals(from)) return;
        
        int maincolor = getPaintIndex(engine);
        if(maincolor < 0) return;// -1 ==> invisible
        Color[] colors = engine.palette.getEntry(maincolor);
        g.setPaint( colors[engine.colorCue] );
        
        // Determine who's in back and who's in front.
        float xb, yb, zb, shortenb, xf, yf, zf, shortenf; // Back and Front
        if(from.z < z)
        {
            xb = from.x;
            yb = from.y;
            zb = from.z;
            shortenb = from.shorten;
            xf = x;
            yf = y;
            zf = z;
            shortenf = shorten;
        }
        else // from.z >= z
        {
            xf = from.x;
            yf = from.y;
            zf = from.z;
            shortenf = from.shorten;
            xb = x;
            yb = y;
            zb = z;
            shortenb = shorten;
        }

        // If line ends extend outside clipping area, calc. its intersection w/ clipping plane
        // If a ball resides at one end point, shorten the line by the radius of the ball
        float dz, dxdz, dydz, dzb, dzf, sx, sy, sz, s;
        dz = zf - zb;
        dxdz = (xf - xb)/dz; // == (dx/dz)
        dydz = (yf - yb)/dz; // == (dy/dz)
        dzb = engine.clipBack - zb;
        dzf = engine.clipFront - zf;
        
        // Clipping or shortening for back point.
        if(shortenb > 0f)
        {
            s = (float)Math.sqrt(shortenb*shortenb / ((dxdz*dxdz + dydz*dydz + 1)*dz*dz));
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
        if(shortenf > 0f)
        {
            s = (float)Math.sqrt(shortenf*shortenf / ((dxdz*dxdz + dydz*dydz + 1)*dz*dz));
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
    
        // see KPoint for this function def.
        // Remember, graphics y and 3-space y go opposite ways!
        prettyLine(g, (int)(xb+xoff), (int)(yoff-yb), (int)(xf+xoff), (int)(yoff-yf), getWidth(engine));
        */
    }
//}}}
    
//{{{ renderForPrinter()
//##################################################################################################
    /**
    * Draws the primitive represented by this point to the specified graphics context, with high quality graphics suitable for publication.
    * This code was copied directly from render(), with minor modifications!
    *
    * @param g the Graphics2D to draw to
    * @param xoff the offset in x (half the display width)
    * @param yoff the offset in y (half the display height)
    * @param engine the Engine calling this function, which contains many interesting public fields
    */
    public void renderForPrinter(Graphics2D g, float xoff, float yoff, Engine engine)
    {
        /*
        if(from == null || equals(from)) return;
        
        int maincolor = getPaintIndex(engine);
        if(maincolor < 0) return;// -1 ==> invisible
        Color[] colors = engine.palette.getEntry(maincolor);
        g.setPaint( colors[engine.colorCue] );
        
        // Determine who's in back and who's in front.
        float xb, yb, zb, shortenb, xf, yf, zf, shortenf; // Back and Front
        if(from.z < z)
        {
            xb = from.x;
            yb = from.y;
            zb = from.z;
            shortenb = from.shorten;
            xf = x;
            yf = y;
            zf = z;
            shortenf = shorten;
        }
        else // from.z >= z
        {
            xf = from.x;
            yf = from.y;
            zf = from.z;
            shortenf = from.shorten;
            xb = x;
            yb = y;
            zb = z;
            shortenb = shorten;
        }

        // If line ends extend outside clipping area, calc. its intersection w/ clipping plane
        // If a ball resides at one end point, shorten the line by the radius of the ball
        float dz, dxdz, dydz, dzb, dzf, sx, sy, sz, s;
        dz = zf - zb;
        dxdz = (xf - xb)/dz; // == (dx/dz)
        dydz = (yf - yb)/dz; // == (dy/dz)
        dzb = engine.clipBack - zb;
        dzf = engine.clipFront - zf;
        
        // Clipping or shortening for back point.
        if(shortenb > 0f)
        {
            s = (float)Math.sqrt(shortenb*shortenb / ((dxdz*dxdz + dydz*dydz + 1)*dz*dz));
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
        if(shortenf > 0f)
        {
            s = (float)Math.sqrt(shortenf*shortenf / ((dxdz*dxdz + dydz*dydz + 1)*dz*dz));
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
    
        // Remember, graphics y and 3-space y go opposite ways!
        int wid = 2;
        if(engine.thinLines) wid = 1;
        else if(width > 0) wid = width;
        else if(parent != null) wid = parent.width;
        if(wid < 1) wid = 1;
        if(wid > 7) wid = 7;
        g.setStroke(ColorManager.pens[wid-1][engine.widthCue]);
        g.draw(new Line2D.Float(xb+xoff, yoff-yb, xf+xoff, yoff-yf));
        */
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
        from = (VectorPoint)pt;
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

//{{{ getWidth()
//##################################################################################################
    // Vector way of finding the right line width to use, given the settings in the engine
    int getWidth(Engine engine)
    {
        int wid = 2;
        if(width > 0) wid = width;
        else if(parent != null) wid = parent.width;
        
        if(engine.thinLines) return (int)(1*widthScale[engine.widthCue] + 0.5f);
        else
        {
            int w = (int)(wid*widthScale[engine.widthCue] + 0.5f);
            return (w < 1 ? 1 : w);
        }
    }
//}}}

//{{{ getZBufferCoord()
//##################################################################################################
    /**
    * Called by the rendering engine to determine the effective z of this element for buffering purposes.
    * For most points, this is just their current z coordinate,
    * but e.g. vectors want to be buffered at the z of the midpoint of the line, rather than one end.
    *
    * @param engine the Engine calling this function, which contains many interesting public fields
    * @return the desired z coordinate for buffering, in the transformed coordinate system.
    */
    public float getZBufferCoord(Engine engine)
    {
        // This only works because starting points are listed before ending points
        // in a kinemage, thus, from has already been transformed when we get here!
        /*if(from != null)
        {
            if(from.z < z && from.z <= engine.clipFront && z >= engine.clipBack)
            {
                return Math.max(from.z, engine.clipBack);
            }
            else if(from.z >= engine.clipBack && z <= engine.clipFront) // && from.z >= z
            {
                return Math.max(z, engine.clipBack);
            }
            else return z;
        }
        else return z;*/
        return 0;
    }
//}}}

//{{{ Transform functions
//##################################################################################################
    /**
    * Wipes out any previous transforms, preparing for a fresh start.
    * Propagated down to individual points.
    */
    public void preTransform()
    {
        x = x0;
        y = y0;
        z = z0;
        shorten = 0f;
    }
//}}}    
}//class
