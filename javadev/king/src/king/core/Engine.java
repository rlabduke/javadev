// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.util.*;;
//}}}
/**
* <code>Engine</code> is responsible for transforming coordinates, Z-buffering,
* and requesting that points render themselves. 
*
* <p>Begun on Mon Apr 22 17:21:31 EDT 2002
* <br>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
*/
public class Engine //extends ... implements ...
{
//{{{ Constants
    public static final int QUALITY_FAIR        = -1;
    public static final int QUALITY_GOOD        = 0;
    public static final int QUALITY_BETTER      = 1;
    public static final int QUALITY_BEST        = 2;
//}}}

//{{{ Variables
//##################################################################################################
    /** READ ONLY: The highest layer in the rendering Z-buffer */
    public final int    TOP_LAYER;
    
    // READ ONLY: Parameters for transforming geometry
    public Transform    xform3D         = null;
    public Transform    xform2D         = null;
    public double       zoom3D          = 1;
    public double       zoom2D          = 1;
    public double       clipBack        = 0;
    public double       clipFront       = 1;
    public double       clipDepth       = 1;
    public boolean      usePerspective  = false;
    public double       perspDist       = 2000;
    
    // READ ONLY: Parameters for painting points
    public boolean      useStereo       = false;
    public float        stereoRotation  = 0;
    public boolean      bigMarkers      = false;
    public boolean      bigLabels       = false;
    public boolean      cueThickness    = false;
    public boolean      thinLines       = false;
    public boolean      cueIntensity    = true;
    public boolean      whiteBackground = false;
    public boolean      monochrome      = false;
    public boolean      colorByList     = false;
    public int          widthCue        = 0;        // cue passed to point, between 0 and 4
    public int          colorCue        = 0;        // cue passed to point, between 0 and 4
    public int          activeAspect    = 0;        // 0 -> don't use aspects, x -> use aspect x if present
    public int          markerSize      = 1;
    public Font         labelFont       = null;
    public FontMetrics  labelFontMetrics = null;
    public int          backgroundMode  = -1;
    public Triple       lightingVector  = new Triple(-1, 1, 3).unit();
    
    // READ/WRITE: Shared "scratch" objects that points can use
    public Triple           work1           = new Triple();
    public Triple           work2           = new Triple();
    public Line2D.Double    line1           = new Line2D.Double();
    public Ellipse2D.Double ellipse1        = new Ellipse2D.Double();
    public GeneralPath      path1           = new GeneralPath();
    
    // FOR USE BY ENGINE ONLY
    ArrayList[]         zbuffer;
    HashMap             ballmap;        // Map<KPoint, Double> for line shortening
    //OdHash              ballmap;        // Map<KPoint, double> for line shortening
    
    Font                bigFont, smallFont;
    Rectangle           pickingRect = new Rectangle();
    float               pickingRadius2 = 25f;
    boolean             warnedPickingRegion = false; // have we warned user about out-of-bounds picks?
//}}}
    
//{{{ Constructor
//##################################################################################################
    /**
    * Creates a new rendering engine.
    */
    public Engine()
    {
        TOP_LAYER = 1000;
        
        flushZBuffer();
        
        stereoRotation  = (float)Math.toRadians(6.0);
        bigFont         = new Font("SansSerif", Font.PLAIN, 24);
        smallFont       = new Font("SansSerif", Font.PLAIN, 12);
    }
//}}}
    
//{{{ render
//##################################################################################################
    /**
    * Transforms the given TransformSignalSubscriber and renders it to a graphics context.
    * @param subscriber     the subscriber that will be transformed and rendered
    * @param view           a KingView representing the current rotation/zoom/clip
    * @param bounds         the bounds of the area to render to
    * @param g              the graphics context of a Swing component or an offscreen buffer
    * @param quality        one of the QUALITY_* constants
    */
    public void render(TransformSignalSubscriber subscriber, KingView view, Rectangle bounds, Graphics2D g, int quality)
    {
        // The game plan:
        // 1. Paint background
        // 2. Load drawing variables
        // 3. For each region (there are 2 for stereo):
        //  a. Transform the coordinates from model-space to screen-space,
        //     add transformed objects to Z-buffer.
        //  b. Paint all objects in Z-buffer, from back to front.
        
        // Get colors and prepare the graphics device
        if(whiteBackground)
        {
            if(monochrome)  backgroundMode = KPaint.WHITE_MONO;
            else            backgroundMode = KPaint.WHITE_COLOR;
            g.setColor(Color.white);
        }
        else
        {
            if(monochrome)  backgroundMode = KPaint.BLACK_MONO;
            else            backgroundMode = KPaint.BLACK_COLOR;
            g.setColor(Color.black);
        }
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        if(quality >= QUALITY_BETTER)
        //if(quality >= QUALITY_BEST)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Set some last-minute drawing variables
        markerSize          = (bigMarkers   ? 2         : 1);
        labelFont           = (bigLabels    ? bigFont   : smallFont);
        labelFontMetrics    = g.getFontMetrics(labelFont);
        
        if(useStereo)
        {
            int         halfwidth   = Math.max(0, bounds.width/2 - 10);
            KingView    altview     = (KingView)view.clone();
            altview.rotateY(stereoRotation);

            Rectangle oldClipRgn, clipRgn;
            oldClipRgn  = g.getClipBounds();
            clipRgn     = new Rectangle();
            
            clipRgn.setBounds(bounds.x, bounds.y, halfwidth, bounds.height);
            g.setClip(clipRgn);
            renderLoop(subscriber, altview, clipRgn, g, quality);
            
            clipRgn.setBounds((bounds.x + bounds.width - halfwidth), bounds.y, halfwidth, bounds.height);
            g.setClip(clipRgn);
            renderLoop(subscriber,    view, clipRgn, g, quality);
            
            g.setClip(oldClipRgn);
        }
        else//!useStereo
        {
            renderLoop(subscriber, view, bounds, g, quality);
        }
    }
//}}}

//{{{ renderLoop
//##################################################################################################
    /**
    * Transforms the given TransformSignalSubscriber and renders it to a graphics context.
    * @param subscriber     the subscriber that will be transformed and rendered
    * @param view           a KingView representing the current rotation/zoom/clip
    * @param bounds         the bounds of the area to render to.
    *   Note that this function does not clip g to ensure that it only paints within these bounds!
    * @param g              the graphics context of a Swing component or an offscreen buffer
    * @param quality        one of the QUALITY_* constants
    */
    void renderLoop(TransformSignalSubscriber subscriber, KingView view, Rectangle bounds, Graphics2D g, int quality)
    {
        int i, j, end_j;    // loop over z-buffer, thru z-buffer
        ArrayList zb;       // == zbuffer[i], saves array lookups
        
        // Clear the cache of old paintables
        for(i = 0; i <= TOP_LAYER; i++)
        {
            zbuffer[i].clear();
        }
        ballmap.clear();
        
        // Transform the paintables
        xform3D = create3DTransform(view, bounds);
        xform2D = create2DTransform(bounds);
        subscriber.signalTransform(this, xform3D);
        pickingRect.setBounds(bounds); // save these bounds as the picking region
        
        if(quality == QUALITY_FAIR)
        {
            // Now paint them to the graphics
            for(i = 0; i <= TOP_LAYER; i++)
            {
                // Calculate depth-cueing constants for this level
                if(cueIntensity)    colorCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
                else                colorCue = KPaint.COLOR_LEVELS - 1;
                if(cueThickness)    widthCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
                else                widthCue = (KPaint.COLOR_LEVELS-1) / 2;
                
                // Render all points at this level (faster to not use iterators)
                zb      = zbuffer[i];
                end_j   = zb.size();
                for(j = 0; j < end_j; j++) ((KPoint)zb.get(j)).paintFast(g, this);
            }
        }
        else if(quality == QUALITY_BEST)// || quality == QUALITY_BETTER)
        {
            // Now paint them to the graphics
            for(i = 0; i <= TOP_LAYER; i++)
            {
                // Calculate depth-cueing constants for this level
                if(cueIntensity)    colorCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
                else                colorCue = KPaint.COLOR_LEVELS - 1;
                if(cueThickness)    widthCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
                else                widthCue = (KPaint.COLOR_LEVELS-1) / 2;
                
                // Render all points at this level (faster to not use iterators)
                zb      = zbuffer[i];
                end_j   = zb.size();
                for(j = 0; j < end_j; j++) ((KPoint)zb.get(j)).paintHighQuality(g, this);
            }
        }
        else//quality == QUALITY_GOOD or QUALITY_BETTER
        {
            // Now paint them to the graphics
            for(i = 0; i <= TOP_LAYER; i++)
            {
                // Calculate depth-cueing constants for this level
                if(cueIntensity)    colorCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
                else                colorCue = KPaint.COLOR_LEVELS - 1;
                if(cueThickness)    widthCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
                else                widthCue = (KPaint.COLOR_LEVELS-1) / 2;
                
                //if(colorCue >= KPaint.COLOR_LEVELS)
                //    SoftLog.err.println("colorCue = "+colorCue+"; i = "+i+"; TOP_LAYER = "+TOP_LAYER);
                
                // Render all points at this level (faster to not use iterators)
                zb      = zbuffer[i];
                end_j   = zb.size();
                for(j = 0; j < end_j; j++) ((KPoint)zb.get(j)).paintStandard(g, this);
            }
        }
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
        double cx, cy, cz, R11, R12, R13, R21, R22, R23, R31, R32, R33;
        synchronized(view)
        {
            view.compile();
            zoom3D      = size / view.getSpan();
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
            clipFront   = view.getClip() * size/2.0;
            clipBack    = -clipFront;
            clipDepth   = clipFront - clipBack;
        }
        
        // Build our transform
        Transform ret = new Transform(), work = new Transform();
        work.likeTranslation(-cx, -cy, -cz);                                // center on rotation center
            ret.append(work);
        work.likeMatrix(R11, R12, R13, R21, R22, R23, R31, R32, R33);       // rotate
            ret.append(work);
        work.likeScale(zoom3D);                                             // zoom
            ret.append(work);
        if(usePerspective)
        {
            perspDist = 5.0 * size;
            work.likePerspective(perspDist);
                ret.append(work);
            // We also have to move clipping planes
            // because this alters z coords too.
            // See driftwood.r3.Transform.likePerspective()
            // for more detailed explanation.
            clipFront = perspDist*clipFront / (perspDist - clipFront);
            clipBack  = perspDist*clipBack  / (perspDist - clipBack);
        }
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
        zoom2D  = size / 400.0;
        
        // Build our transform
        Transform ret = new Transform(), work = new Transform();
        work.likeScale(zoom2D);                                             // resize to fill screen
            ret.append(work);
        work.likeScale(1, -1, 1);                                           // invert Y axis
            ret.append(work);
        work.likeTranslation(xOff, yOff, 0);                                // center on screen
            ret.append(work);
        
        return ret;
    }
//}}}

//{{{ addPaintable, flushZBuffer
//##################################################################################################
    /**
    * Registers a paintable to be drawn to the screen.
    * @param p          the KPoint to be rendered.
    * @param zcoord     the Z coordinate of the transformed object.
    *   A layer number is calculated automatically from this. 
    */
    public void addPaintable(KPoint p, double zcoord)
    {
        addPaintableToLayer(p, (int)(TOP_LAYER*(zcoord-clipBack)/clipDepth));
    }

    /**
    * Registers a paintable to be drawn to the screen.
    * @param p          the KPoint to be rendered.
    * @param layer      a number between 0 and TOP_LAYER, inclusive, that
    *   determines the order of rendering. Objects at 0 are furthest from
    *   the observer and are rendered first; objects in the TOP_LAYER are
    *   closest to the observer and are rendered last.
    */
    public void addPaintableToLayer(KPoint p, int layer)
    {
        if(layer < 0 || layer > TOP_LAYER || p == null) return;
        zbuffer[layer].add(p);
    }
    
    /**
    * This may need to be called to reclaim memory when kinemages are closed.
    */
    public void flushZBuffer()
    {
        zbuffer = null;
        zbuffer = new ArrayList[TOP_LAYER+1];
        for(int i = 0; i <= TOP_LAYER; i++)
        {
            zbuffer[i] = new ArrayList(10);
        }
        
        ballmap = null;
        ballmap = new HashMap(1000);
        //ballmap = new OdHash(1000);
    }
//}}}

//{{{ addShortener, getShortening
//##################################################################################################
    /**
    * Registers the given point as having some radius that other
    * objects should respect. In particular, this radius will be
    * used to shorten lines that originate/terminate at this location.
    */
    public void addShortener(KPoint p, double radius)
    {
        Double old = (Double)ballmap.get(p);
        if(old == null || old.doubleValue() < radius) ballmap.put(p, new Double(radius));
        //double old = ballmap.get(p, 0);
        //if(old < radius) ballmap.put(p, radius);
    }
    
    /**
    * Returns the amount of shortening, between 0 and +inf,
    * that should be applied at the given point.
    */
    public double getShortening(KPoint p)
    {
        Double radius = (Double)ballmap.get(p);
        if(radius == null) return 0;
        else return radius.doubleValue();
        //return ballmap.get(p, 0);
    }
//}}}

//{{{ updatePrefs
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    public void updatePrefs(Props prefs)
    {
        stereoRotation  = (float)Math.toRadians(prefs.getDouble("stereoAngle"));
        bigFont         = new Font("SansSerif", Font.PLAIN, prefs.getInt("fontSizeBig"));
        smallFont       = new Font("SansSerif", Font.PLAIN, prefs.getInt("fontSizeSmall"));
        this.setPickingRadius( prefs.getDouble("pickingRadius") );
    }
//}}}

//{{{ pickPoint, setPickingRadius
//##################################################################################################
    /**
    * Finds the point clicked on with the mouse.
    * @param xcoord the x coord of the pick, relative to the drawing surface
    * @param ycoord the y coord of the pick, relative to the drawing surface
    * @param superpick if true, even pick points marked as unpickable
    * @return the KPoint that was selected
    */
    public KPoint pickPoint(int xcoord, int ycoord, boolean superpick)
    {
        if(!pickingRect.contains(xcoord, ycoord))
        {
            if(!warnedPickingRegion)
            {
                JOptionPane.showMessageDialog(null,
                    "When using stereo, only the right-hand half\n"+
                    "of the screen is active for picking.\n\n"+
                    "This message will not be displayed again.",
                    "Out-of-bounds pick",
                    JOptionPane.WARNING_MESSAGE);
                warnedPickingRegion = true;
            }
            return null;
        }
        
        // Iterate over all levels and all points in each level, searching for "the one"
        int         i, j, end_j;        // loop over z-buffer
        ArrayList   zb;                 // == zbuffer[i], saves array lookups
        KPoint      theone = null, p;
        
        // Note: looping front to back, rather than back to front as in render()
        for(i = TOP_LAYER; i >= 0 && theone == null; i--)
        {
            zb = zbuffer[i];
            end_j = zb.size();
            for(j = 0; j < end_j && theone == null; j++)
            {
                p = (KPoint)zb.get(j);
                if(p.isPickedBy(xcoord, ycoord, pickingRadius2) && (!p.isUnpickable() || superpick)) theone = p;
                
            }
        }
        return theone;
    }
    
    public void setPickingRadius(double r)
    {
        if(r > 1)
            this.pickingRadius2 = (float)(r*r);
    }
//}}}

//{{{ pickAll3D
//##################################################################################################
    /**
    * Finds all points within the specified radius of the given coordinates.
    * All coordinates are device coordinates -- i.e., coordinates in the transformed space.
    * The units, therefore, are pixels.
    * @return all the KPoints that were selected
    */
    public Collection pickAll3D(double xcoord, double ycoord, double zcoord, boolean superpick, double radius)
    {
        // Iterate over all levels and all points in each level, searching for "the one"
        int         i, j, end_j;        // loop over z-buffer
        ArrayList   zb;                 // == zbuffer[i], saves array lookups
        KPoint      p;
        ArrayList   found = new ArrayList();
        double      r2 = radius*radius;
        
        // Note: looping front to back, rather than back to front as in render()
        // start layer == (int)(TOP_LAYER*(zcoord-clipBack)/clipDepth)
        final int frontLayer = TOP_LAYER, backLayer = 0;
        for(i = frontLayer; i >= backLayer; i--)
        {
            zb = zbuffer[i];
            end_j = zb.size();
            for(j = 0; j < end_j; j++)
            {
                p = (KPoint)zb.get(j);
                double dx = p.getX() - xcoord;
                double dy = p.getY() - ycoord;
                double dz = p.getZ() - zcoord;
                if((dx*dx + dy*dy + dz*dz) <= r2 && (!p.isUnpickable() || superpick))
                    found.add(p);
            }
        }
        return found;
    }
//}}}

//{{{ pickAll2D
//##################################################################################################
    /**
    * Finds all points within the specified radius of the given x-y coordinates,
    * regardless of the z coordinate.
    * All coordinates are device coordinates -- i.e., coordinates in the transformed space.
    * The units, therefore, are pixels.
    * @return all the KPoints that were selected
    */
    public Collection pickAll2D(double xcoord, double ycoord, boolean superpick, double radius)
    {
        // Iterate over all levels and all points in each level, searching for "the one"
        int         i, j, end_j;        // loop over z-buffer
        ArrayList   zb;                 // == zbuffer[i], saves array lookups
        KPoint      p;
        ArrayList   found = new ArrayList();
        double      r2 = radius*radius;
        
        // Note: looping front to back, rather than back to front as in render()
        // start layer == (int)(TOP_LAYER*(zcoord-clipBack)/clipDepth)
        final int frontLayer = TOP_LAYER, backLayer = 0;
        for(i = frontLayer; i >= backLayer; i--)
        {
            zb = zbuffer[i];
            end_j = zb.size();
            for(j = 0; j < end_j; j++)
            {
                p = (KPoint)zb.get(j);
                double dx = p.getX() - xcoord;
                double dy = p.getY() - ycoord;
                if((dx*dx + dy*dy) <= r2 && (!p.isUnpickable() || superpick))
                    found.add(p);
            }
        }
        return found;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class
