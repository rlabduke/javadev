// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

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
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
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
    static class DiskProxyPoint extends ProxyPoint
    {
        int diskLevel;
        
        public DiskProxyPoint(BallPoint proxyFor, int diskLevel)
        {
            super(proxyFor);
            this.diskLevel = diskLevel;
        }
        
        /**
        * Renders this Paintable to the specified graphics surface,
        * using the display settings from engine.
        */
        public void paintStandard(Engine engine)
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
    public SpherePoint(KList list, String label)
    {
        super(list, label);
        
        // Need one less b/c "this" acts as the base layer
        this.proxies = new DiskProxyPoint[N_DISKS-1];
        for(int i = 1; i < N_DISKS; i++)
        {
            this.proxies[i-1] = new DiskProxyPoint(this, i);
        }
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
        super.signalTransform(engine, xform, zoom);
        // "this" has already been added at i == 0
        for(int i = 1; i < N_DISKS; i++)
        {
            engine.addPaintable(proxies[i-1], z + sin[i]*r);
        }
    }
//}}}

//{{{ paintStandard
//##############################################################################
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paintStandard(Engine engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
            // Don't use alpha, b/c it won't look right for stacked disks
        Paint paint = maincolor.getPaint(engine.backgroundMode, 0, engine.colorCue, 255);

        // We have to do this here b/c now widthCue is set
        if(engine.cueThickness) r *= KPalette.widthScale[ engine.widthCue ];
        
        engine.painter.paintSphereDisk(paint, x, y, z, r);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

