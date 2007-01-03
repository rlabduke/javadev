// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

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
* <code>SpherePoint</code> is like a BallPoint, except it renders a whole stack
* of disks like Mage does for @spherelists.
* This leads to a much more believable appearance in the case of spheres
* that intersect other solid objects, like each other!
*
* <p>Copyright (C) 2004-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Oct 18 10:26:11 EDT 2004
*/
public class SpherePoint extends BallPoint
{
//{{{ Constants
    static final int        N_DISKS     = 16;
    static final double[]   sin         = new double[N_DISKS];
    static final double[]   cos         = new double[N_DISKS];
    static
    {
        // Sin is fwd offset from largest disk, moving toward viewer.
        // Sin spacing is such that disk spacing is equal.
        // Cos is for scaling disk radius.
        for(int i = 0; i < N_DISKS; i++)
        {
            sin[i] = (double)i/(double)N_DISKS;
            cos[i] = Math.sqrt(1.0 - sin[i]*sin[i]);
        }
    }
//}}}

//{{{ CLASS: DiskProxyPoint
//##############################################################################
    /**
    * DiskProxyPoint is used to represent one disk in the stack for drawing and
    * picking purposes only.
    */
    public static class DiskProxyPoint extends ProxyPoint
    {
        int diskLevel;
        
        protected DiskProxyPoint(BallPoint proxyFor, int diskLevel)
        {
            super(proxyFor);
            this.diskLevel = diskLevel;
        }
        
        /**
        * Renders this Paintable to the specified graphics surface,
        * using the display settings from engine.
        */
        public void paint2D(Engine2D engine)
        {
            KPaint maincolor = getDrawingColor(engine);
            if(maincolor.isInvisible()) return;
            // Don't use alpha, b/c it won't look right for stacked disks
            Paint paint = maincolor.getPaint(engine.backgroundMode, SpherePoint.sin[diskLevel], engine.colorCue, 255);
            
            double r = SpherePoint.cos[diskLevel] * ((BallPoint)proxyFor).r;
            engine.painter.paintSphereDisk(paint, getDrawX(), getDrawY(), getDrawZ(), r);
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    DiskProxyPoint[] proxies;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SpherePoint(String label)
    {
        super(label);
        
        // Need one less b/c "this" acts as the base layer
        this.proxies = new DiskProxyPoint[N_DISKS-1];
        for(int i = 1; i < N_DISKS; i++)
        {
            this.proxies[i-1] = new DiskProxyPoint(this, i);
        }
    }
//}}}

//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform, double zoom)
    {
        super.doTransform(engine, xform, zoom);
        // "this" has already been added at i == 0
        for(int i = 1; i < N_DISKS; i++)
        {
            engine.addPaintable(proxies[i-1], z + sin[i]*r);
        }
    }
//}}}

//{{{ paint2D
//##############################################################################
    public void paint2D(Engine2D engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
            // Don't use alpha, b/c it won't look right for stacked disks
        Paint paint = maincolor.getPaint(engine.backgroundMode, 0, engine.colorCue, 255);

        // Can't do depth cueing by width -- causes really weird problems!!
        //
        // We have to do this here b/c now widthCue is set
        //if(engine.cueThickness) r *= KPalette.widthScale[ engine.widthCue ];
        
        engine.painter.paintSphereDisk(paint, x, y, z, r);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

