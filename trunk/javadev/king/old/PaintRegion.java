// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
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
* <code>PaintRegion</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 27 10:19:24 EST 2003
*/
public class PaintRegion //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** READ ONLY: The highest layer in the rendering Z-buffer */
    public final int    TOP_LAYER;
    
    /** READ ONLY: Z-coordinate of the rear slab (clipping plane) */
    public double       slabRear = 0;
    
    /** READ ONLY: Z-coordinate of the front slab (clipping plane) */
    public double       slabFront = 0;
    
    /** READ ONLY: Depth of the slab (clipping plane): slabFront - slabRear */
    public double       slabDepth = 0;
    
    ArrayList[]         zbuffer;
    Map                 cache;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PaintRegion()
    {
        TOP_LAYER = 1000;
        clearCache();
    }
//}}}

//{{{ addPaintable
//##################################################################################################
    /**
    * Registers a paintable to be drawn to the screen.
    * @param cacheKey   a unique non-null key to identify this Paintable for later retrieval.
    * @param p          the Paintable to be rendered.
    * @param zcoord     the Z coordinate of the transformed object.
    *   A layer number is calculated automatically from this. 
    */
    public void addPaintable(Object cacheKey, Paintable p, double zcoord)
    {
        addPaintableToLayer(cacheKey, p,
            (int)(TOP_LAYER*(zcoord-slabRear)/slabDepth));
    }

    /**
    * Registers a paintable to be drawn to the screen.
    * @param cacheKey   a unique non-null key to identify this Paintable for later retrieval.
    * @param p          the Paintable to be rendered.
    * @param layer      a number between 0 and TOP_LAYER, inclusive, that
    *   determines the order of rendering. Objects at 0 are furthest from
    *   the observer and are rendered first; objects in the TOP_LAYER are
    *   closest to the observer and are rendered last.
    */
    public void addPaintableToLayer(Object cacheKey, Paintable p, int layer)
    {
        if(layer < 0 || layer > TOP_LAYER || p == null) return;
        if(cacheKey != null) cache.put(cacheKey, p);
        zbuffer[layer].add(p);
    }
//}}}

//{{{ getCachedPaintable, clearCache
//##################################################################################################
    /**
    * Retrieves a paintable that was previously added using addPaintable().
    * This way, a data object can re-use the same Paintable for drawing to
    * a particular PaintRegion.
    * @return the previously registered Paintable, or null if none was found.
    *   The cache is periodically cleared, so objects shouldn't count on always
    *   being able to recover their old Paintables.
    */
    public Paintable getCachedPaintable(Object key)
    {
        return (Paintable)cache.get(key);
    }
    
    /**
    * Clears out the cache of Paintables and frees
    * the memory used by the cache and Z-buffer.
    */
    public void clearCache()
    {
        zbuffer = null;
        cache   = null;
        
        zbuffer = new ArrayList[TOP_LAYER+1];
        for(int i = 0; i <= TOP_LAYER; i++)
        {
            zbuffer[i] = new ArrayList(10);
        }
        
        cache = new HashMap(10*TOP_LAYER);
    }
//}}}

//{{{ create3DTransform
//##################################################################################################
    /**
    * Builds a Transform suitable for use with TransformSignal.
    * @param view       a KingView describing the current rotation, zoom, and clipping
    * @param bounds     the region of the Component where the Paintables will be rendered
    */
    public Transform create3DTransform(KingView view, Rectangle bounds)
    {
        double width, height, size, xOff, yOff;
        width   = bounds.getWidth();
        height  = bounds.getHeight();
        size    = Math.min(width, height);
        xOff    = bounds.getX() + width/2.0;
        yOff    = bounds.getY() + height/2.0;
        
        // Get information from the current view
        double zoom, cx, cy, cz, R11, R12, R13, R21, R22, R23, R31, R32, R33;
        synchronized(view)
        {
            view.compile();
            zoom        = size / view.getSpan();
            cx          = view.cx;
            cy          = view.cy;
            cz          = view.cz;
            R11         = view.R11;
            R12         = view.R12;
            R13         = view.R13;
            R21         = view.R21;
            R22         = view.R22;
            R23         = view.R23;
            R31         = view.R31;
            R32         = view.R32;
            R33         = view.R33;
            slabFront   = view.getClip() * size/2.0;
            slabRear    = -slabFront;
            slabDepth   = slabFront - slabRear;
        }
        
        // Build our transform
        Transform ret = new Transform(), work = new Transform();
        work.likeTranslation(-cx, -cy, -cz);                                // center on rotation center
            ret.append(work);
        work.likeMatrix(R11, R12, R13, R21, R22, R23, R31, R32, R33);       // rotate
            ret.append(work);
        work.likeScale(zoom);                                               // zoom
            ret.append(work);
        // Can't apply perspective using a matrix (?),
        // so we have to do it point by point later on
        work.likeScale(1, -1, 1);                                           // invert Y axis
            ret.append(work);
        work.likeTranslation(xOff, yOff, 0);                                // center on screen
            ret.append(work);
        
        return ret;
    }
//}}}

//{{{ create2DTransform
//##################################################################################################
    /**
    * Builds a Transform suitable for use with TransformSignal.
    * @param bounds     the region of the Component where the Paintables will be rendered
    */
    public Transform create2DTransform(Rectangle bounds)
    {
        double width, height, size, xOff, yOff;
        width   = bounds.getWidth();
        height  = bounds.getHeight();
        size    = Math.min(width, height);
        xOff    = bounds.getX() + width/2.0;
        yOff    = bounds.getY() + height/2.0;
        
        // Build our transform
        Transform ret = new Transform(), work = new Transform();
        work.likeScale(size/400.0);                                         // resize to fill screen
            ret.append(work);
        work.likeScale(1, -1, 1);                                           // invert Y axis
            ret.append(work);
        work.likeTranslation(xOff, yOff, 0);                                // center on screen
            ret.append(work);
        
        return ret;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

