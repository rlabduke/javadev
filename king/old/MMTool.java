// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
//}}}
/**
 * <code>MMTool</code> implements a move-and-measure mouse tool for basic manipulation of a kinemage.
 * In its final form, it should feature multiple styles of measuring and marking points,
 * with user-assignable functions for each click or drag type.
 *
 * <p>Begun on Fri Jun 21 09:30:40 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class MMTool extends BasicTool
{
//{{{ Static fields
    static final DecimalFormat df_3 = new DecimalFormat("###,###,##0.###");
    static final DecimalFormat df_1 = new DecimalFormat("##0.#");
    static final int[] marker_styles = { MarkerPoint.CROSS_2 | MarkerPoint.X_2,
        MarkerPoint.CROSS_L | MarkerPoint.X_L,
        MarkerPoint.CROSS_M | MarkerPoint.X_M,
        MarkerPoint.CROSS_M,
        MarkerPoint.RING_L
    };
    
    static final Object MODE_UNDECIDED  = new Object();
    static final Object MODE_ZOOM       = new Object();
    static final Object MODE_CLIP       = new Object();
//}}}

//{{{ Variable definitions
//##################################################################################################
    JPanel toolPanel;
    JCheckBox doMeasureAll, doMarkers, doXYZ;
    Object zoomClipMode = null;
    
    int lastPickedBacklogSize = 5;
    LinkedList lastPickedBacklog;
    
    int nMarkers = 2;
    LinkedList markers;
    
    PointEditor pointEditor;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public MMTool(ToolBox tb)
    {
        super(tb);
        setToolName("Move & Measure");
        lastPickedBacklog = new LinkedList();
        markers = new LinkedList();
        pointEditor = new PointEditor(parent.kMain);
        
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0,3,0,3);
        toolPanel = new JPanel(gbl);
        
        JLabel l;
        // Do click/drag labels:
        gbc.gridx = 0; gbc.fill = gbc.HORIZONTAL;
        l = new JLabel("Click: ", JLabel.RIGHT);
        gbc.gridy = 0; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("+Shift: ", JLabel.RIGHT);
        gbc.gridy = 1; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("+Ctrl: ", JLabel.RIGHT);
        gbc.gridy = 2; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("Drag: ", JLabel.RIGHT);
        gbc.gridy = 3; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("+Shift: ", JLabel.RIGHT);
        gbc.gridy = 4; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("+Ctrl: ", JLabel.RIGHT);
        gbc.gridy = 5; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        // Do click/drag ACTION labels:
        gbc.gridx = 1;  gbc.fill = gbc.HORIZONTAL;
        l = new JLabel("Identify & measure", JLabel.LEFT);
        gbc.gridy = 0; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("Center on point", JLabel.LEFT);
        gbc.gridy = 1; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("Edit point", JLabel.LEFT);
        gbc.gridy = 2; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("Rotate & pinwheel", JLabel.LEFT);
        gbc.gridy = 3; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("Zoom & clip", JLabel.LEFT);
        gbc.gridy = 4; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        l = new JLabel("Translate", JLabel.LEFT);
        gbc.gridy = 5; gbl.setConstraints(l, gbc);
        toolPanel.add(l);
        
        // Do other controls
        Component c = Box.createRigidArea(new Dimension(0,10));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.fill = gbc.HORIZONTAL;
        gbl.setConstraints(c, gbc);
        toolPanel.add(c);
        
        doMarkers = new JCheckBox(new ReflectiveAction("Show markers", null, this, "onShowMarkers"));
        doMarkers.setSelected(false);
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.fill = gbc.HORIZONTAL;
        gbl.setConstraints(doMarkers, gbc);
        toolPanel.add(doMarkers);

        doXYZ = new JCheckBox("Show XYZ coordinates", false);
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.fill = gbc.HORIZONTAL;
        gbl.setConstraints(doXYZ, gbc);
        toolPanel.add(doXYZ);

        doMeasureAll = new JCheckBox("Measure angle & dihedral too", false);
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2; gbc.fill = gbc.HORIZONTAL;
        gbl.setConstraints(doMeasureAll, gbc);
        toolPanel.add(doMeasureAll);
    }
//}}}

//{{{ start/stop/reset functions    
//##################################################################################################
    public void start()
    {
        clearLastPicked();
        markers.clear();
    }
//}}}

//{{{ click() functions    
//##################################################################################################
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        identify(p);
        if(doMeasureAll.isSelected()) measureAll(p);
        else measure(p);
        mark(p);
    }
    
    public void s_click(int x, int y, KPoint p, MouseEvent ev)
    {
        KingView v = parent.kMain.getView();
        
        click(x, y, p, ev);
        pickcenter(p, v);
    }
    
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        pointEditor.editPoint(p);
    }
    
    public void sc_click(int x, int y, KPoint p, MouseEvent ev) {}
//}}}

//{{{ click functionalities    
//##################################################################################################
    public void identify(KPoint p)
    {
        if(parent.kMain.getView() == null || p == null) parent.setID(null);
        else
        {
            parent.setID(p.getName());
            if(doXYZ.isSelected()) parent.setCoords(df_3.format(p.getOrigX())+"  "+df_3.format(p.getOrigY())+"  "+df_3.format(p.getOrigZ()));
        }
        parent.kCanvas.repaint();
    }
    
    public void measure(KPoint p)
    {
        KPoint q = getLastPicked(0);
        if(parent.kMain.getView() == null || p == null) parent.setDist(null);
        else if(q != null)
        {
            float dist, dx, dy, dz;
            dx = p.getOrigX() - q.getOrigX();
            dy = p.getOrigY() - q.getOrigY();
            dz = p.getOrigZ() - q.getOrigZ();
            dist = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
            parent.setDist(df_3.format(dist));
        }
        parent.kCanvas.repaint();
    }

    public void measureAll(KPoint p)
    {
        StringBuffer msg = new StringBuffer();
        KPoint q = getLastPicked(0);
        if(parent.kMain.getView() == null || p == null) parent.setDist(null);
        else if(q != null)
        {
            //{{{ Measure distance, angle, and dihedral angle...
            // Naming scheme:
            //  points are p, q, r, and s, from most recently picked to least recently picked
            //  vectors are d (from q to p), e (from r to q), and f (from s to r)
            //  normal to the s-r-q plane is u, normal to the r-q-p plane is v
            float dmag, dx, dy, dz;
            dx = p.getOrigX() - q.getOrigX();
            dy = p.getOrigY() - q.getOrigY();
            dz = p.getOrigZ() - q.getOrigZ();
            dmag = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
            msg.append("dist: ").append(df_3.format(dmag));
            
            KPoint r = getLastPicked(1);
            if(r != null)
            {
                float angle, emag, ex, ey, ez;
                ex = q.getOrigX() - r.getOrigX();
                ey = q.getOrigY() - r.getOrigY();
                ez = q.getOrigZ() - r.getOrigZ();
                emag = (float)Math.sqrt(ex*ex + ey*ey + ez*ez);
                // Dot product: d . e = d e cos(a) = dxex + dyey + dzez
                // But notice 'e' has the wrong sign here for this to work right...
                angle = (float)Math.toDegrees(Math.acos((dx*ex + dy*ey + dz*ez) / (dmag * -emag)));
                msg.append("   angl: ").append(df_1.format(angle));
                
                KPoint s = getLastPicked(2);
                if(s != null)
                {
                    float fmag, fx, fy, fz;
                    fx = r.getOrigX() - s.getOrigX();
                    fy = r.getOrigY() - s.getOrigY();
                    fz = r.getOrigZ() - s.getOrigZ();
                    fmag = (float)Math.sqrt(fx*fx + fy*fy + fz*fz);
                    
                    // Cross product: a x b = (aybz-azby, -axbz+azbx, axby-aybx)
                    // 'u' and 'v' are normals to planes
                    // u = f x e, v = e x d
                    float umag, ux, uy, uz, vmag, vx, vy, vz;
                    ux = fy*ez - fz*ey;
                    uy = fz*ex - fx*ez;
                    uz = fx*ey - fy*ex;
                    umag = (float)Math.sqrt(ux*ux + uy*uy + uz*uz);
                    vx = ey*dz - ez*dy;
                    vy = ez*dx - ex*dz;
                    vz = ex*dy - ey*dx;
                    vmag = (float)Math.sqrt(vx*vx + vy*vy + vz*vz);
                    // Dot product again
                    angle = (float)Math.toDegrees(Math.acos((ux*vx + uy*vy + uz*vz) / (umag * vmag)));
                    
                    // BUT, that doesn't solve the handedness (sign) problem for the dihedral!
                    // To do that, we look at the angle between 'f' and 'v'
                    // Dot product again
                    if( Math.toDegrees(Math.acos((fx*vx + fy*vy + fz*vz) / (fmag * vmag))) > 90.0 )
                    { angle = -angle; }
                    
                    msg.append("   dihd: ").append(df_1.format(angle));
                }//dihedral
            }//angle
            //}}}
            
            parent.setDist(msg.toString());
        }//distance
        parent.kCanvas.repaint();
    }//measureAll()
    
    public void mark(KPoint p)
    {
        if(doMeasureAll.isSelected()) nMarkers = 4;
        else nMarkers = 2;
        
        if(p != null) markers.addFirst(new MarkerPoint(p, 0, MarkerPoint.CROSS_L));
        while(markers.size() > nMarkers) markers.removeLast();
        if(doMarkers.isSelected()) parent.kCanvas.repaint();
    }
//}}}

//{{{ drag() functions    
//##################################################################################################
    public void drag(int dx, int dy, MouseEvent ev)
    {
        if(isNearTop) pinwheel(dx, parent.kMain.getView());
        else rotate(dx, dy, parent.kMain.getView());
    }
    
    public void s_drag(int dx, int dy, MouseEvent ev)
    {
        KingView v = parent.kMain.getView();
        if(zoomClipMode == MODE_UNDECIDED)
        {
            // Force a (strong?) committment to either horizonal
            // or vertical motion before we take action
                 if(Math.abs(dy) > 0+Math.abs(dx))  zoomClipMode = MODE_ZOOM;
            else if(Math.abs(dx) > 0+Math.abs(dy))  zoomClipMode = MODE_CLIP;
        }
        
        if(zoomClipMode == MODE_ZOOM)       zoom(dy, v);
        else if(zoomClipMode == MODE_CLIP)  clip(dx, v);
    }
    
    public void c_drag(int dx, int dy, MouseEvent ev)
    {
        if(isNearTop) ztranslate(dx, parent.kMain.getView());
        else translate(dx, dy, parent.kMain.getView());
    }

    public void sc_drag(int dx, int dy, MouseEvent ev) {}
//}}}

//{{{ drag functionalities    
//##################################################################################################
//}}}

//{{{ Mouse click listners    
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {
        super.mouseClicked(ev);
        
        // Handle picking, etc. here
        Dimension dim = parent.kCanvas.getSize();
        int x = ev.getX(), y = ev.getY();
        KPoint p = parent.kCanvas.getEngine().pickPoint(x, y, dim.width, dim.height);
        
        if(p != null)
        {
            lastPickedBacklog.addFirst(p);
            while(lastPickedBacklog.size() > lastPickedBacklogSize) lastPickedBacklog.removeLast();
        }
    }

    public void mousePressed(MouseEvent ev)
    {
        super.mousePressed(ev);
        zoomClipMode = MODE_UNDECIDED;
    }
//}}}

//{{{ GUI listeners
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowMarkers(ActionEvent ev)
    {
        parent.kMain.notifyChange(KingMain.EM_ON_OFF);
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
        if(doMarkers.isSelected())
        {
            MarkerPoint mp;
            int i = 0;
            for(Iterator iter = markers.iterator(); iter.hasNext(); i++)
            {
                mp = (MarkerPoint)iter.next();
                mp.setStyle(marker_styles[i]);
                mp.preTransform();
            }
        }
    }
    
    /**
    * Transforms this object into screen space (pixel) coordinates, including (x,y) offset and perspective.
    * Also does depth buffering.
    * Propagated down to individual points.
    * @param engine the engine holding the depth buffer and transform matrix
    * @param xform the transformation will only be applied if this is true
    */
    public void viewTransform(Engine engine)
    {
        if(doMarkers.isSelected())
        {
            for(Iterator iter = markers.iterator(); iter.hasNext(); ) ((KPoint)iter.next()).viewTransform(engine);
        }
    }
//}}}

//{{{ get/set functions    
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    public Component getToolPanel()
    {
        return toolPanel;
    }
    
    /** Gets last point picked (0), or two points ago (1), or three (2), etc. */
    public KPoint getLastPicked(int which)
    {
        KPoint ret = null;
        try {
            ret = (KPoint)lastPickedBacklog.get(which);
        } catch(IndexOutOfBoundsException ex) {}
        return ret;
    }
    
    /** Removes the backlog of picked points */
    public void clearLastPicked() { lastPickedBacklog.clear(); }
//}}}
}//class
