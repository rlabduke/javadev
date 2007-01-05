// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.points.*;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;

import java.util.List;
//}}}
/**
 * <code>ToolServices</code> implements the most common manipulation functions,
 * including picking, pickcentering, rotation, translation, measures, and markers.
 * There is one ToolServices object in each ToolBox, and all the Tools in that ToolBox
 * should use it for their interactions.
 *
 * <p>There are still some calls to KinCanvas.repaint() in this class,
 * but for the moment I think they're legit.
 * One instance of this class is tightly coupled to a particular canvas,
 * and only the canvas needs to know that some tool wants a point picked.
 *
 * <p>Begun on Fri Jun 21 09:30:40 EDT 2002
 * <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class ToolServices implements Transformable
{
//{{{ Static fields
    static final DecimalFormat df_3 = new DecimalFormat("###,###,##0.###");
    static final DecimalFormat df_1 = new DecimalFormat("##0.#");
    /*static final int[] marker_styles = {
        MarkerPoint.CROSS_2 | MarkerPoint.X_2,
        MarkerPoint.CROSS_L | MarkerPoint.X_L,
        MarkerPoint.CROSS_M | MarkerPoint.X_M,
        MarkerPoint.CROSS_M,
        MarkerPoint.RING_L
    };*/
    static final int[] marker_styles = {
        MarkerPoint.SQUARE_L | MarkerPoint.SQUARE_M | MarkerPoint.CROSS_2,
        MarkerPoint.RING_L   | MarkerPoint.RING_M   | MarkerPoint.CROSS_L,
        MarkerPoint.SQUARE_M | MarkerPoint.CROSS_M,
        MarkerPoint.RING_M   | MarkerPoint.CROSS_M,
        MarkerPoint.DISC_L   | MarkerPoint.DISC_M   | MarkerPoint.DISC_S // shouldn't ever see this one!
    };
//}}}

//{{{ Variable definitions
//##################################################################################################
    ToolBox     parent;
    KingMain    kMain;
    KinCanvas   kCanvas;
    
    // Delta zoom and clip that are not "used" by the quantized
    // models are saved and applied to the next mouse move
    float delta_zoom    = 0f;
    float delta_clip    = 0f;

    // Strings to paint
    String pointID      = null;
    String distance     = null;
    String aspect       = null;
    String coords       = null;
    
    // Control service options
    public JCheckBoxMenuItem    doXYZ;
    public JCheckBox            doMarkers;
    public JCheckBoxMenuItem    doFlatland;
    public JCheckBoxMenuItem    doSuperpick;
    public JCheckBoxMenuItem    doObjectPick;
    public JCheckBoxMenuItem    doMeasureAll;
    public JCheckBox            doPickcenter;
    
    // Markers and point backlog
    LinkedList  trackedPoints;
    final int   maxTrackedPoints = 5;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public ToolServices(ToolBox tb)
    {
        parent  = tb;
        kMain   = tb.kMain;
        kCanvas = tb.kCanvas;

        trackedPoints   = new LinkedList();

        doXYZ           = new JCheckBoxMenuItem(new ReflectiveAction("Show XYZ coordinates", null, this, "onShowXYZ"));
            doXYZ.setSelected(false);
        doMarkers       = new JCheckBox(new ReflectiveAction("Markers", null, this, "onShowMarkers"));
            doMarkers.setSelected(false);
            doMarkers.setToolTipText("Markers painted on points you've selected with the mouse");
        doFlatland      = new JCheckBoxMenuItem("Flatland", false);
            doFlatland.setToolTipText("Translate in X-Y instead of rotating");
        doSuperpick     = new JCheckBoxMenuItem("Superpick", false);
            doSuperpick.setToolTipText("Pick points that are otherwise unpickable");
        doObjectPick    = new JCheckBoxMenuItem(new ReflectiveAction("Pick objects", null, this, "onObjectPick"));
            doObjectPick.setSelected(kCanvas.getEngine().useObjPicking);
            doObjectPick.setToolTipText("Pick lines and faces in addition to points");
        doMeasureAll    = new JCheckBoxMenuItem(new ReflectiveAction("Measure angle & dihedral", null, this, "onShowMeasures"));
            doMeasureAll.setSelected(false);
        doPickcenter    = new JCheckBox("Pick center", false);
            doPickcenter.setToolTipText("Click this, then choose a new center of rotation");
    }
//}}}

//{{{ pick
//##################################################################################################
    /**
    * Displays the ID of the point in the appropriate text box,
    * and registers this point with the marker-tracking system.
    */
    public void pick(KPoint p)
    {
        if(doPickcenter.isSelected())
        {
            doPickcenter.setSelected(false);
            centerOnPoint(p);
        }
        else
        {
            identify(p);
            
            if(doMeasureAll.isSelected())   measureAll(p);
            else                            measure(p);
            
            mark(p);
        }
    }
//}}}

//{{{ identify, measure, mark
//##################################################################################################
    public void identify(KPoint p)
    {
        if(kMain.getView() == null || p == null)
        {
            setID(null);
            setDist(null);
            setCoords(null);
        }
        else
        {
            setID(p.getName());
            setCoords(df_3.format(p.getX())+"  "+df_3.format(p.getY())+"  "+df_3.format(p.getZ()));
        }
        kCanvas.repaint();
    }
    
    public void measure(KPoint p)
    {
        KPoint q = getLastPicked(0);
        if(kMain.getView() == null || p == null)
        {
            setDist(null);
        }
        else if(q != null)
        {
            double dx = p.getX() - q.getX();
            double dy = p.getY() - q.getY();
            double dz = p.getZ() - q.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            setDist(df_3.format(dist));
        }
        kCanvas.repaint();
    }

    /**
    * "Officialy" picks a point -- puts a marker there and
    * considers it a point in the sequence of measures.
    * Be sure to call this AFTER measure() / measureAll()
    */
    public void mark(KPoint p)
    {
        if(p != null)
            trackedPoints.addFirst(p);

        while(trackedPoints.size() > maxTrackedPoints)
            trackedPoints.removeLast();
        
        if(doMarkers.isSelected()) kCanvas.repaint();
    }
//}}}

//{{{ measureAll
//##################################################################################################
    public void measureAll(KPoint p)
    {
        StringBuffer msg = new StringBuffer();
        KPoint q = getLastPicked(0);
        if(kMain.getView() == null || p == null) setDist(null);
        else if(q != null)
        {
            // Naming scheme:
            //  points are p, q, r, and s, from most recently picked to least recently picked
            //  vectors are d (from q to p), e (from r to q), and f (from s to r)
            //  normal to the s-r-q plane is u, normal to the r-q-p plane is v
            double dx = p.getX() - q.getX();
            double dy = p.getY() - q.getY();
            double dz = p.getZ() - q.getZ();
            double dmag = Math.sqrt(dx*dx + dy*dy + dz*dz);
            msg.append("dist: ").append(df_3.format(dmag));
            
            KPoint r = getLastPicked(1);
            if(r != null)
            {
                double ex = q.getX() - r.getX();
                double ey = q.getY() - r.getY();
                double ez = q.getZ() - r.getZ();
                double emag = Math.sqrt(ex*ex + ey*ey + ez*ez);
                // Dot product: d . e = d e cos(a) = dxex + dyey + dzez
                // But notice 'e' has the wrong sign here for this to work right...
                double angle = Math.toDegrees(Math.acos((dx*ex + dy*ey + dz*ez) / (dmag * -emag)));
                msg.append("   angl: ").append(df_1.format(angle));
                
                KPoint s = getLastPicked(2);
                if(s != null)
                {
                    double fx = r.getX() - s.getX();
                    double fy = r.getY() - s.getY();
                    double fz = r.getZ() - s.getZ();
                    double fmag = Math.sqrt(fx*fx + fy*fy + fz*fz);
                    
                    // Cross product: a x b = (aybz-azby, -axbz+azbx, axby-aybx)
                    // 'u' and 'v' are normals to planes
                    // u = f x e, v = e x d
                    double ux = fy*ez - fz*ey;
                    double uy = fz*ex - fx*ez;
                    double uz = fx*ey - fy*ex;
                    double umag = Math.sqrt(ux*ux + uy*uy + uz*uz);
                    double vx = ey*dz - ez*dy;
                    double vy = ez*dx - ex*dz;
                    double vz = ex*dy - ey*dx;
                    double vmag = Math.sqrt(vx*vx + vy*vy + vz*vz);
                    // Dot product again
                    angle = Math.toDegrees(Math.acos((ux*vx + uy*vy + uz*vz) / (umag * vmag)));
                    
                    // BUT, that doesn't solve the handedness (sign) problem for the dihedral!
                    // To do that, we look at the angle between 'f' and 'v'
                    // Dot product again
                    if( Math.toDegrees(Math.acos((fx*vx + fy*vy + fz*vz) / (fmag * vmag))) > 90.0 )
                    { angle = -angle; }
                    
                    msg.append("   dihd: ").append(df_1.format(angle));

                    if(kMain.getPrefs().getBoolean("measureVectorVectorAngle"))
                    {
                        // Angle between an axis (r-s) and a vector (q-p)
                        // Dot product: d . f = d f cos(a) = dxfx + dyfy + dzfz
                        // But notice 'f' has the wrong sign here for this to work right...
                        angle = Math.toDegrees(Math.acos((dx*fx + dy*fy + dz*fz) / (dmag * -fmag)));
                        msg.append("   vect: ").append(df_1.format(angle));
                    }
                }//dihedral
            }//angle
            
            setDist(msg.toString());
        }//distance
        kCanvas.repaint();
    }//measureAll()
    
//}}}

//{{{ centerOnPoint
//##################################################################################################
    /** Centers the view on the point */
    public void centerOnPoint(KPoint p)
    {
        KView v = kMain.getView();
        if(v == null || p == null) return;
        
        v.setCenter((float)p.getX(), (float)p.getY(), (float)p.getZ());
    }
//}}}

//{{{ rotate, pinwheel
//##################################################################################################
    /** Given offsets in pixels, does normal kinemage rotation */
    public void rotate(float dx, float dy)
    {
        KView v = kMain.getView();
        if(v == null) return;
        
        v.rotateX((float)(2.0*Math.PI) * dy / 600f);
        v.rotateY((float)(2.0*Math.PI) * dx / 600f);
    }

    /** Given a distance in pixels, does pinwheel rotation */
    public void pinwheel(float dist)
    {
        KView v = kMain.getView();
        if(v == null) return;

        v.rotateZ((float)(-2.0*Math.PI) * dist / 600f);
    }
//}}}

//{{{ translate, ztranslate
//##################################################################################################
    /** Given offsets in pixels, does a flatland translation */
    public void translate(int dx, int dy)
    {
        KView v = kMain.getView();
        if(v == null) return;
        
        Dimension dim = kCanvas.getCanvasSize();
        v.viewTranslateRotated(dx, -dy, 0, (dim.width < dim.height ? dim.width : dim.height));
    }

    /** Given an offset in pixels, does translation into/out of the screen */
    public void ztranslate(int d)
    {
        KView v = kMain.getView();
        if(v == null) return;
        
        Dimension dim = kCanvas.getCanvasSize();
        v.viewTranslateRotated(0, 0, d, (dim.width < dim.height ? dim.width : dim.height));
    }
//}}}

//{{{ adjustZoom, adjustClipping
//##################################################################################################
    /** Given a pixel offset, does zooming */
    public void adjustZoom(float dist)
    {
        KView v = kMain.getView();
        if(v == null) return;

        BoundedRangeModel model = kCanvas.getZoomModel(); 
        // Delta zoom that's not "used" by the quantized zoommodel is saved and applied to the next mouse move
        delta_zoom += dist / 6f;
        model.setValue(model.getValue() + (int)delta_zoom);
        delta_zoom -= (int)delta_zoom;
        // repaint is automatic when zoom is adjusted
    }

    /** Given a pixel distance, adjusts clipping */
    public void adjustClipping(float dist)
    {
        KView v = kMain.getView();
        if(v == null) return;

        BoundedRangeModel model = kCanvas.getClipModel(); 
        // Delta clip that's not "used" by the quantized clipmodel is saved and applied to the next mouse move
        delta_clip += dist / 1f;
        model.setValue(model.getValue() + (int)delta_clip);
        delta_clip -= (int)delta_clip;
        // repaint is automatic when clip is adjusted
    }
//}}}

//{{{ getLastPicked, clearLastPicked, clearEverything
//##################################################################################################
    /** Gets last point picked (0), or two points ago (1), or three (2), etc. */
    public KPoint getLastPicked(int which)
    {
        KPoint ret = null;
        try {
            ret = (KPoint)trackedPoints.get(which);
        } catch(IndexOutOfBoundsException ex) {}
        return ret;
    }
    
    /** Removes the backlog of picked points */
    public void clearLastPicked()
    { trackedPoints.clear(); }
    
    /** Clears out all markers and starts over */
    public void clearEverything()
    {
        clearLastPicked();
        setID(null);
        setDist(null);
        setAspect(null);
        setCoords(null);
    }
//}}}

//{{{ onShowMarkers, onShowXYZ, onShowMeasures, onObjectPick
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowMarkers(ActionEvent ev)
    { kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_ON_OFF)); }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowMeasures(ActionEvent ev)
    {
        // Measures turned on
        if(doMeasureAll.isSelected())
        {
            clearLastPicked();
            doMarkers.setSelected(true);
        }
        // Measures turned off
        else
        {
            setDist(null);
            doMarkers.setSelected(false);
        }
        
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_ON_OFF));
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowXYZ(ActionEvent ev)
    { kCanvas.repaint(); }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onObjectPick(ActionEvent ev)
    { kCanvas.getEngine().useObjPicking = doObjectPick.isSelected(); }
//}}}

//{{{ empty
//##################################################################################################
//}}}

//{{{ makeNormalMarkers
//##################################################################################################
    KList makeNormalMarkers(List points, int howmany)
    {
        KList marks = new KList(KList.MARK);
        marks.setColor(KPalette.white);
        
        for(int i = 0; i < howmany && i < points.size(); i++)
        {
            KPoint p = (KPoint) points.get(i);
            MarkerPoint mark = new MarkerPoint(p, KPalette.white, marker_styles[i]);
            mark.setParent(marks);
            marks.add(mark);
        }
        
        return marks;
    }
//}}}

//{{{ makeMageMeasures
//##################################################################################################
    /**
    * Constructs a subgroup that looks like Mage's measures from recently picked points.
    * Most recently picked is at the head of the list.
    */
    KGroup makeMageMeasures(List points)
    {
        KGroup subgroup = new KGroup();

        KList lines = new KList(KList.VECTOR, "lines");
        subgroup.add(lines);
        lines.setWidth(5);
        lines.setColor(KPalette.white);
        
        KPoint tracked;
        VectorPoint prev = null;
        for(int i = 3; i >= 0; i--)
        {
            if(points.size() > i)
            {
                tracked = (KPoint) points.get(i);
                VectorPoint curr = new VectorPoint("", prev);
                curr.setX(tracked.getX());
                curr.setY(tracked.getY());
                curr.setZ(tracked.getZ());
                curr.setUnpickable(true);
                prev = curr;
                lines.add(curr);
            }
        }
        
        KList dots = new KList(KList.DOT, "dots");
        subgroup.add(dots);
        dots.setWidth(2);
        dots.setColor(KPalette.hotpink);

        double x = 0, y = 0, z = 0;
        for(int i = 0; i < 4; i++)
        {
            if(points.size() > i)
            {
                tracked = (KPoint) points.get(i);
                x += tracked.getX();
                y += tracked.getY();
                z += tracked.getZ();
                if(i > 0)
                {
                    DotPoint dot = new DotPoint("");
                    dot.setX( x / (i+1) );
                    dot.setY( y / (i+1) );
                    dot.setZ( z / (i+1) );
                    dots.add(dot);
                }
            }
        }
        
        return subgroup;
    }
//}}}

//{{{ doTransform
//##################################################################################################
    /**
    * A call to this method indicates the subscriber
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
    *
    * <p>This method will be called in response to TransformSignal.doTransform().
    *
    * @param engine     the Engine object describing the
    *   dimensions and properties of the space to be painted.
    * @param xform      the Transform to apply.
    *   The subscriber must not modify the original Transform it
    *   receives! Subscibers may, however, copy and modify the
    *   Transform(s) they pass to internal substructures.
    */
    public void doTransform(Engine engine, Transform xform)
    {
        if(doMarkers.isSelected())
        {
            if(doMeasureAll.isSelected())
            {
                KGroup subgroup = makeMageMeasures(trackedPoints);
                subgroup.doTransform(engine, xform);
            }
            else
            {
                KList marks = makeNormalMarkers(trackedPoints, 2);
                marks.doTransform(engine, xform);
            }
        }
    }
//}}}
    
//{{{ overpaintCanvas
//##################################################################################################
    /**
    * Called by KinCanvas after all kinemage painting is complete,
    * this gives the tools a chance to write additional info
    * (e.g., point IDs) to the graphics area.
    * @param painter    the Painter that can paint on the current canvas
    */
    public void overpaintCanvas(Painter painter)
    {
        Engine engine = kCanvas.getEngine();
        if(engine == null) return;
        
        Dimension size = kCanvas.getCanvasSize();
        painter.setFont(engine.labelFont);
        Color fontColor = (engine.whiteBackground ? Color.black : Color.white);
        
        int ascent      = painter.getLabelAscent("X");
        int descent     = painter.getLabelDescent("X");
        int vOffset1    = ascent + 4;                           // just down from the top
        int vOffset2    = size.height - descent - 4;            // just up from the bottom
        int vOffset3    = vOffset2 - (ascent + descent + 4);    // just above that
        int hOffset1    = 4;                                    // left side
        int hOffset2    = size.width/2 + 4;                     // the middle
        
        // Display aspect name
        Aspect a = kCanvas.getCurrentAspect();
        if(a != null)   setAspect(a.getName());
        else            setAspect(null);
        
        if(aspect   != null)                        painter.paintLabel(fontColor, aspect, hOffset1, vOffset1, 0);
        if(coords   != null && doXYZ.isSelected())  painter.paintLabel(fontColor, coords, hOffset2, vOffset1, 0);
        if(pointID  != null)
        {
            int lblWidth = painter.getLabelWidth(pointID);
            // pointID is bumped up one line if it's really long
            if(hOffset1 + lblWidth >= hOffset2)
            {
                painter.paintLabel(fontColor, pointID, hOffset1, vOffset3, 0);
                if(hOffset1 + lblWidth >= size.width) // REALLY long: print to std out
                    SoftLog.out.println(pointID);
            }
            else
                painter.paintLabel(fontColor, pointID, hOffset1, vOffset2, 0);
        }
        if(distance != null)                        painter.paintLabel(fontColor, distance, hOffset2, vOffset2, 0);
    }
//}}}

//{{{ setID/Dist/Aspect/Coords
//##################################################################################################
    /** Bottom left label */
    public void setID(String s)
    { pointID = s; }
    /** Bottom center label */
    public void setDist(String s)
    { distance = s; }
    /** Top left label -- no one can change this */
    private void setAspect(String s)
    { aspect = s; }
    /** Top center label */
    public void setCoords(String s)
    {
        coords = s;
        if(coords != null && doXYZ.isSelected()) System.err.println(coords);
    }
//}}}
}//class
