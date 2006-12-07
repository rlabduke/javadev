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
* Engine itself contains all of the transformation and picking logic.
* Engine2D also does "traditional" drawing using java.awt.Graphics objects,
* based on the transformed coordinates.
* Other Engine subclasses may do drawing e.g. via OpenGL calls, and only
* do coordinate transforms in software for the purpose of picking.
*
* <p>Because KPoints can only store ONE set of transformed coordinates at a time,
* and because there may be multiple Engines rendering different views of the same kinemage,
* you MUST re-transform the kinemage immediately prior to doing any picking operations.
*
* <p>Begun on Mon Apr 22 17:21:31 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
abstract public class Engine //extends ... implements ...
{
//{{{ Constants
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
    public double       perspDist       = 2000;
    
    // READ ONLY: Parameters for painting points
    public boolean      useObjPicking   = false;    // should we pick triangles, lines, balls as solid objects?
    public boolean      bigMarkers      = false;
    public boolean      bigLabels       = false;
    public boolean      cueIntensity    = true;
    public boolean      monochrome      = false;
    public int          widthCue        = 0;        // cue passed to point, between 0 and 4
    public int          colorCue        = 0;        // cue passed to point, between 0 and 4
    public int          activeAspect    = 0;        // 0 -> don't use aspects, x -> use aspect x if present
    public int          markerSize      = 1;
    public Font         labelFont       = null;
    public int          backgroundMode  = -1;
    public Triple       lightingVector  = new Triple(-1, 1, 3).unit();
    public Rectangle    pickingRect     = new Rectangle(); // bounds of one side of stero area or whole canvas
    
    // READ ONLY: These are set from Kinemage obj by KinCanvas on every drawing pass.
    // Changing them here will have NO EFFECT because they'll be overwritten.
    public boolean      usePerspective  = false;
    public boolean      cueThickness    = false;
    public boolean      thinLines       = false;
    public boolean      whiteBackground = false;
    public boolean      colorByList     = false;
    
    // FOR USE BY ENGINE ONLY
    ArrayList<KPoint>[]         zbuffer;
    HashMap<KPoint, Double>     ballmap;                // for line shortening
    ArrayList<KList>[]          parents;                // KList that is acting parent for each pt in zbuffer; default is null
    KList                       actingParent = null;    // KList that is parent for each pt as added; null = no change
    // See setActingParent() for a description of the stupid hijinks we're pulling here.
    
    // Things needed to support multiple clipping planes:
    double              viewClipBack    = -1;
    double              viewClipFront   = 1;
    double              viewClipScaling = 1;
    // Objects used as keys are just arbitrary identifiers (can be "new Object()", for instance)
    Map<Object, Double> frontClipMap    = new HashMap<Object, Double>();
    Map<Object, Double> backClipMap     = new HashMap<Object, Double>();
    
    float               pickingRadius   = 5f;
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
    }
//}}}
    
//{{{ transform
//##################################################################################################
    /**
    * Transforms the given Transformable, without rendering it to graphics.
    * If you want transformation AND rendering, you should call render() or equivalent in a subclass.
    * @param xformable      the Transformable that will be transformed.
    * @param view           a KView representing the current rotation/zoom/clip
    * @param bounds         the bounds of the area to render to.
    *   Note that this function does not clip g to ensure that it only paints within these bounds!
    */
    void transform(Transformable xformable, KView view, Rectangle bounds)
    {
        ArrayList<KPoint>   zb;     // == zbuffer[i], saves array lookups
        ArrayList<KList>    pnt;    // == parents[i], saves array lookups
        
        // Clear the cache of old paintables
        for(int i = 0; i <= TOP_LAYER; i++)
        {
            zbuffer[i].clear();
            parents[i].clear();
        }
        ballmap.clear();
        
        // Transform the paintables
        xform3D = create3DTransform(view, bounds);
        xform2D = create2DTransform(bounds);
        this.chooseClipMode(null); // default to std clipping planes
        xformable.doTransform(this, xform3D);
        pickingRect.setBounds(bounds); // save these bounds as the picking region
    }    
//}}}

//{{{ create3DTransform
//##################################################################################################
    /**
    * Builds a Transform suitable for use with Transformables.
    * @param view       a KView describing the current rotation, zoom, and clipping
    * @param bounds     the region of the Component where the paintable points will be rendered
    */
    Transform create3DTransform(KView view, Rectangle bounds)
    {
        double width, height, size, xOff, yOff;
        width   = bounds.getWidth();
        height  = bounds.getHeight();
        size    = Math.min(width, height);
        xOff    = bounds.getX() + width/2.0;
        yOff    = bounds.getY() + height/2.0;
        this.viewClipScaling = size/2.0;
        
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
            viewClipFront = view.getClip();
            viewClipBack = -viewClipFront;
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
    * Builds a Transform suitable for use with Transformables.
    * @param bounds     the region of the Component where the paintable points will be rendered
    */
    Transform create2DTransform(Rectangle bounds)
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

//{{{ setActingParent
//##################################################################################################
    /**
    * This is a *really* dumb hack that allows us to implement Mage's instance=
    * feature fairly cheaply in terms of both time and space.
    * The idea is that each point in the zbuffer will be associated with a KList
    * object that *should* own it for drawing purposes.
    * For most KPoints, this should be the one KList they belong to,
    * but we choose to record <code>null</code> instead to save a few operations.
    * If something else is stored, we set the point's owner to the specified list
    * just for the duration of the drawing operation, to ensure it appears in the
    * correct color, width, radius, etc.
    * <p>Most normal lists should call this function with the parameter <code>null</code>
    * before beginning to transform their points.
    * Lists that are "instances" of other lists will instead pass in <code>this</code>
    * (but should remember to reset to null after transforming all their points).
    */
    public void setActingParent(KList pnt)
    {
        this.actingParent = pnt;
    }
//}}}

//{{{ chooseClipMode, putClipMode
//##################################################################################################
    /**
    * Enables an arbitrary number of clipping planes (all normal to the line of sight)
    * to be used for clipping various parts of the kinemage.
    * @param key    a unique identifier for this set of planes (e.g. new Object())
    * @param front  the front clipping plane, in view units (positive)
    * @param back   the back clipping plane, in view units (negative)
    */
    public void putClipMode(Object key, double front, double back)
    {
        if(key == null) return;
        frontClipMap.put(key, new Double(front));
        backClipMap.put(key, new Double(back));
    }
    
    /**
    * Selects a clipping mode based on the key used for putClipMode().
    * A null key selects the default (KView) clipping planes.
    */
    public void chooseClipMode(Object key)
    {
        this.clipFront  = this.viewClipFront;
        this.clipBack   = this.viewClipBack;
        
        if(key != null)
        {
            Double d = frontClipMap.get(key);
            if(d != null) this.clipFront = d.doubleValue();
            d = backClipMap.get(key);
            if(d != null) this.clipBack = d.doubleValue();
        }
        
        // Convert from KView units to pixel units
        this.clipFront  *= viewClipScaling;
        this.clipBack   *= viewClipScaling;
        
        if(usePerspective)
        {
            // We also have to move clipping planes
            // because this alters z coords too.
            // See driftwood.r3.Transform.likePerspective()
            // for more detailed explanation.
            clipFront = perspDist*clipFront / (perspDist - clipFront);
            clipBack  = perspDist*clipBack  / (perspDist - clipBack);
        }
        
        this.clipDepth  = clipFront - clipBack;
    }
//}}}

//{{{ addPaintable, addPaintableToLayer, flushZBuffer
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
        parents[layer].add(actingParent);
    }
    
    /**
    * This may need to be called to reclaim memory when kinemages are closed.
    */
    public void flushZBuffer()
    {
        zbuffer = null;
        // Goofy syntax required by Java for no good reason I can see.
        // Can't do ... = new ArrayList<KPoint>[TOP_LAYER+1]
        zbuffer = (ArrayList<KPoint>[]) new ArrayList[TOP_LAYER+1];
        for(int i = 0; i <= TOP_LAYER; i++)
        {
            zbuffer[i] = new ArrayList<KPoint>(10);
        }
        
        parents = null;
        // Goofy syntax required by Java for no good reason I can see.
        // Can't do ... = new ArrayList<KList>[TOP_LAYER+1]
        parents = (ArrayList<KList>[]) new ArrayList[TOP_LAYER+1];
        for(int i = 0; i <= TOP_LAYER; i++)
        {
            parents[i] = new ArrayList<KList>(10);
        }
        
        ballmap = null;
        ballmap = new HashMap<KPoint, Double>(1000);
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
        Double old = ballmap.get(p);
        if(old == null || old.doubleValue() < radius) ballmap.put(p, new Double(radius));
    }
    
    /**
    * Returns the amount of shortening, between 0 and +inf,
    * that should be applied at the given point.
    */
    public double getShortening(KPoint p)
    {
        Double radius = ballmap.get(p);
        if(radius == null) return 0;
        else return radius.doubleValue();
    }
//}}}

//{{{ updatePrefs, syncToKin
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    public void updatePrefs(Props prefs)
    {
        this.setPickingRadius( prefs.getDouble("pickingRadius") );
        useObjPicking = prefs.getBoolean("pickObjects");
    }
    
    /** Takes needed display settings from the kinemage */
    public void syncToKin(Kinemage kin)
    {
        //if(kin.currAspect == null) this.activeAspect = 0;
        //else this.activeAspect = kin.currAspect.getIndex().intValue();
        
        this.usePerspective     = kin.atPerspective;
        this.cueThickness       = ! kin.atOnewidth;
        this.thinLines          = kin.atThinline;
        this.whiteBackground    = kin.atWhitebackground;
        this.colorByList        = kin.atListcolordominant;
    }
//}}}

//{{{ pickPoint, setPickingRadius
//##################################################################################################
    /**
    * Finds the point clicked on with the mouse.
    * @param xcoord the x coord of the pick, relative to the drawing surface
    * @param ycoord the y coord of the pick, relative to the drawing surface
    * @param superpick if true, even pick points marked as unpickable
    * @return the KPoint that was selected, or null if none
    */
    public KPoint pickPoint(int xcoord, int ycoord, boolean superpick)
    {
        if(!pickingRect.contains(xcoord, ycoord))
        {
            if(!warnedPickingRegion)
            {
                try
                {
                    JCheckBox dontWarn = new JCheckBox("Don't warn me again", false);
                    JOptionPane.showMessageDialog(null,
                        new Object[] {
                            "When using stereo, only the right-hand half\n"+
                            "of the screen is active for picking.",
                            dontWarn
                        },
                        "Out-of-bounds pick",
                        JOptionPane.WARNING_MESSAGE);
                    warnedPickingRegion = dontWarn.isSelected();
                }
                catch(HeadlessException ex) {}
            }
            return null;
        }
        
        // Iterate over all levels and all points in each level, searching for "the one"
        KPoint      theone = null;
        // Note: looping front to back, rather than back to front as in render()
        for(int i = TOP_LAYER; i >= 0 && theone == null; i--)
        {
            ArrayList<KPoint> zb = zbuffer[i];
            for(int j = 0, end_j = zb.size(); j < end_j && theone == null; j++)
            {
                KPoint p = zb.get(j);
                // q will usually be p or null, but sometimes not for object picking
                KPoint q = p.isPickedBy(xcoord, ycoord, pickingRadius, useObjPicking);
                // Off points have to be transformed anyway in case they're used by
                // other ends of lines or triangles, so we have to check it here.
                // Using getDrawingColor() checks for invisible, aspect-invisible, *and* off points
                if(q != null && (!q.isUnpickable() || superpick) && !q.getDrawingColor(this).isInvisible())
                    theone = q;
            }
        }
        return theone;
    }
    
    public void setPickingRadius(double r)
    {
        if(r > 1)
            this.pickingRadius = (float)r;
    }
//}}}

// XXX: this should act with an identity KView so that picking units are real space coords (?)
//{{{ pickAll3D
//##################################################################################################
    /**
    * Finds all points within the specified radius of the given coordinates.
    * All coordinates are device coordinates -- i.e., coordinates in the transformed space.
    * The units, therefore, are pixels.
    * @return all the KPoints that were selected
    * /
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
                double dx = p.getDrawX() - xcoord;
                double dy = p.getDrawY() - ycoord;
                double dz = p.getDrawZ() - zcoord;
                // Using getDrawingColor() checks for invisible, aspect-invisible, *and* off points
                if((dx*dx + dy*dy + dz*dz) <= r2 && (!p.isUnpickable() || superpick) && !p.getDrawingColor(this).isInvisible())
                    found.add(p);
            }
        }
        return found;
    }
    */
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
    public Collection<KPoint> pickAll2D(double xcoord, double ycoord, boolean superpick, double radius)
    {
        // Iterate over all levels and all points in each level, searching for "the one"
        ArrayList<KPoint> found = new ArrayList<KPoint>();
        
        // Note: looping front to back, rather than back to front as in render()
        for(int i = TOP_LAYER; i >= 0; i--)
        {
            ArrayList<KPoint> zb = zbuffer[i];
            for(int j = 0, end_j = zb.size(); j < end_j; j++)
            {
                KPoint p = zb.get(j);
                // q will usually be p or null, but sometimes not for object picking
                KPoint q = p.isPickedBy((float)xcoord, (float)ycoord, (float)radius, useObjPicking);
                // Off points have to be transformed anyway in case they're used by
                // other ends of lines or triangles, so we have to check it here.
                // Using getDrawingColor() checks for invisible, aspect-invisible, *and* off points
                if(q != null && (!q.isUnpickable() || superpick) && !q.getDrawingColor(this).isInvisible())
                    found.add(q);
            }
        }
        return found;
    }
//}}}

//{{{ getNumberPainted
//##################################################################################################
    /** Calculates the number of KPoint objects that were "painted" in the last cycle. */
    public int getNumberPainted()
    {
        int num = 0;
        for(int i = 0; i < zbuffer.length; i++) num += zbuffer[i].size();
        return num;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class
