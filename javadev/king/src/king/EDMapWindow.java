// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.isosurface.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>EDMapWindow</code> has controls for one
* electron density map, contoured at two levels.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Mar  5 09:00:11 EST 2003
*/
public class EDMapWindow implements ChangeListener, ActionListener, TransformSignalSubscriber
{
//{{{ Constants
    DecimalFormat df1 = new DecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain    kMain;
    KinCanvas   kCanvas;
    ToolBox     parent;
    
    CrystalVertexSource     map;
    MarchingCubes           mc1, mc2;
    EDMapPlotter            plotter1, plotter2;
    String                  title;
    
    JDialog     dialog;
    JSlider     extent, slider1, slider2;
    JCheckBox   label1, label2;
    JComboBox   color1, color2;
    JCheckBox   useTriangles;
    JButton     discard, export;
    
    float       ctrX, ctrY, ctrZ;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public EDMapWindow(ToolBox parent, CrystalVertexSource map, String title)
    {
        this.parent     = parent;
        kMain           = parent.kMain;
        kCanvas         = parent.kCanvas;
        
        parent.sigTransform.subscribe(this);
        
        this.map        = map;
        this.title      = title;
        ctrX = ctrY = ctrZ = Float.NaN;
        
        // Plotters need to be non-null for signalTransform()
        // These are never used though; overwritten on first updateMesh()
        Object mode = MarchingCubes.MODE_TRIANGLE;
        plotter1 = new EDMapPlotter(false, mode);
        plotter2 = new EDMapPlotter(false, mode);
        //mc1 = new MarchingCubes(map, map, plotter1, mode);
        //mc2 = new MarchingCubes(map, map, plotter2, mode);
        
        buildGUI();
        
        dialog.pack();
        dialog.setLocationRelativeTo(kMain.getTopWindow());
        dialog.setVisible(true);
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    void buildGUI()
    {
        dialog = new JDialog(kMain.getTopWindow(), title+" - EDMap", false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        label1 = new JCheckBox("1.2 sigma", true);
        label2 = new JCheckBox("3.0 sigma", false);
        
        color1 = new JComboBox(kMain.getKinemage().getAllPaintMap().values().toArray());
        color1.setSelectedItem(KPalette.gray);
        color2 = new JComboBox(kMain.getKinemage().getAllPaintMap().values().toArray());
        color2.setSelectedItem(KPalette.purple);
        
        extent = new JSlider(0, 30, 10);
        extent.setMajorTickSpacing(10);
        extent.setMinorTickSpacing(2);
        extent.setPaintTicks(true);
        //extent.setSnapToTicks(true); -- this seems to be buggy/weird
        extent.setPaintLabels(true);
        
        slider1 = new JSlider(-80, 80, 12);
        slider1.setMajorTickSpacing(10);
        slider1.setPaintTicks(true);
        //slider1.setSnapToTicks(true); -- this seems to be buggy/weird
        slider1.setPaintLabels(false);

        slider2 = new JSlider(-80, 80, 30);
        slider2.setMajorTickSpacing(10);
        slider2.setPaintTicks(true);
        //slider2.setSnapToTicks(true); -- this seems to be buggy/weird
        slider2.setPaintLabels(false);
        
        useTriangles = new JCheckBox(new ReflectiveAction("Translucent surface", null, this, "onTriangles"));
        useTriangles.setToolTipText("Enables a translucent triangle-mesh surface; use with Best rendering quality.");
        discard = new JButton(new ReflectiveAction("Discard this map", null, this, "onMapDiscard"));
        export  = new JButton(new ReflectiveAction("Export to kinemage", null, this, "onMapExport"));
        
        label1.addActionListener(this);
        label2.addActionListener(this);
        color1.addActionListener(this);
        color2.addActionListener(this);
        extent.addChangeListener(this);
        slider1.addChangeListener(this);
        slider2.addChangeListener(this);
        
        TablePane pane = new TablePane();
        pane.save().hfill(true).addCell(extent, 2, 1).restore();
        pane.newRow();
        pane.add(pane.strut(0,8));
        pane.newRow();
        pane.add(label1);
        pane.add(color1);
        pane.newRow();
        pane.save().hfill(true).addCell(slider1, 2, 1).restore();
        pane.newRow();
        pane.add(pane.strut(0,4));
        pane.newRow();
        pane.add(label2);
        pane.add(color2);
        pane.newRow();
        pane.save().hfill(true).addCell(slider2, 2, 1).restore();
        pane.newRow();
        pane.add(pane.strut(0,4));
        pane.newRow();
        pane.add(useTriangles, 2, 1);
        pane.newRow();
        pane.center().hfill(true);
        pane.add(export, 2, 1);
        pane.newRow();
        pane.add(discard, 2, 1);
        
        dialog.setContentPane(pane);
    }
//}}}

//{{{ stateChanged, actionPerformed, onTriangles, calcSliderValue
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        double val;
        val = calcSliderValue(slider1);
        label1.setText(df1.format(val)+" sigma");
        val = calcSliderValue(slider2);
        label2.setText(df1.format(val)+" sigma");
        
        updateMesh();
        kCanvas.repaint();
    }
    
    public void actionPerformed(ActionEvent ev)
    {
        kCanvas.repaint();
    }
    
    // target of reflection
    public void onTriangles(ActionEvent ev)
    {
        updateMesh();
        kCanvas.repaint();
    }
    
    double calcSliderValue(JSlider slider)
    {
        int i = slider.getValue();
        if(-60 <= i && i <= 60)
            return i/10.0;
        else if(i > 60)
            return (6.0 + (i-60)*2.0);
        else if(i < -60)
            return -(6.0 + (-i-60)*2.0);
        else
            throw new Error("assertion failure");
    }
//}}}

//{{{ centerChanged
//##################################################################################################
    /**
    * Reports on whether the viewing center has been changed.
    * Has the side effect of updating the internal center to match the current view.
    */
    boolean centerChanged()
    {
        KingView v = kMain.getView();
        if(v == null) return false;
        
        float[] ctr = v.getCenter();
        boolean ret = (ctrX != ctr[0] || ctrY != ctr[1] || ctrZ != ctr[2]);
        
        ctrX = ctr[0];
        ctrY = ctr[1];
        ctrZ = ctr[2];
        
        return ret;
    }
//}}}

//{{{ updateMesh
//##################################################################################################
    void updateMesh()
    {
        if(Float.isNaN(ctrX) || Float.isNaN(ctrY) || Float.isNaN(ctrZ)) return;
        
        // Regenerate our plotting apparatus here in case the user's
        // preference for std. mesh vs. cobwebs has changed.
        Object mode = (useTriangles.isSelected() ? MarchingCubes.MODE_TRIANGLE : MarchingCubes.MODE_MESH);
        plotter1 = new EDMapPlotter(false, mode);
        plotter2 = new EDMapPlotter(false, mode);
        mc1 = new MarchingCubes(map, map, plotter1, mode);
        mc2 = new MarchingCubes(map, map, plotter2, mode);
        
        double val, size = extent.getValue() / 2.0;
        int[] corner1 = new int[3], corner2 = new int[3];
        
        map.findVertexForPoint(ctrX-size, ctrY-size, ctrZ-size, corner1);
        map.findVertexForPoint(ctrX+size, ctrY+size, ctrZ+size, corner2);
        
        /*double[] xyz = new double[3];
        map.locateVertex(corner1[0], corner1[1], corner1[2], xyz);
        SoftLog.err.println("findVertex("+(ctrX-size)+" "+(ctrY-size)+" " +(ctrZ-size)+") -> "+xyz[0]+" "+xyz[1]+" "+xyz[2]);
        map.locateVertex(corner2[0], corner2[1], corner2[2], xyz);
        SoftLog.err.println("findVertex("+(ctrX+size)+" "+(ctrY+size)+" " +(ctrZ+size)+") -> "+xyz[0]+" "+xyz[1]+" "+xyz[2]);*/
        
        val = calcSliderValue(slider1);
        mc1.march(corner1[0], corner1[1], corner1[2], corner2[0], corner2[1], corner2[2], val*map.sigma);
        
        val = calcSliderValue(slider2);
        mc2.march(corner1[0], corner1[1], corner1[2], corner2[0], corner2[1], corner2[2], val*map.sigma);
        
        //SoftLog.err.println("Updated mesh: "+corner1[0]+" "+corner1[1]+" "+corner1[2]+" / "+corner2[0]+" "+corner2[1]+" "+corner2[2]);
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
    */
    public void signalTransform(Engine engine, Transform xform)
    {
        KList list;
        if(centerChanged()) updateMesh();
        
        list = plotter1.getList();
        if(list != null && label1.isSelected())
        {
            list.setColor((KPaint)color1.getSelectedItem());
            list.signalTransform(engine, xform);
        }
        
        list = plotter2.getList();
        if(list != null && label2.isSelected())
        {
            list.setColor((KPaint)color2.getSelectedItem());
            list.signalTransform(engine, xform);
        }
        
        //SoftLog.err.println("Painted maps.");
    }
//}}}
    
//{{{ onMapDiscard, onMapExport
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMapDiscard(ActionEvent ev)
    {
        dialog.dispose();
        parent.sigTransform.unsubscribe(this);
        kCanvas.repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMapExport(ActionEvent ev)
    {
        // insert lists into kinemage
        Kinemage kin = kMain.getKinemage();
        
        KGroup group = new KGroup(kin, "ED map");
        kin.add(group);
        
        KSubgroup subgroup = new KSubgroup(group, "ED map");
        subgroup.setHasButton(false);
        group.add(subgroup);
        
        KList list1, list2;
        list1 = plotter1.getList(); plotter1.freeList();
        list2 = plotter2.getList(); plotter2.freeList();
        if(list1 != null && label1.isSelected())
        {
            list1.setOwner(subgroup);
            subgroup.add(list1);
        }
        if(list2 != null && label2.isSelected())
        {
            list2.setOwner(subgroup);
            subgroup.add(list2);
        }
        updateMesh(); // regenerate the meshes we just exported
        
        kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

