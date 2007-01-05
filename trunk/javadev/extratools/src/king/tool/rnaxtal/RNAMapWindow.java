// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;
import king.*;
import king.core.*;
import king.points.*;

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
* Attempting to use EDMap programs as a starting point for RNA ED
* analysis. VBC 27 Oct 2003.
*
* Controls were added to allow polypicking to be turned on and off,
* and also to change the color. VBC 6 Jan 2004.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Mar  5 09:00:11 EST 2003
*/
public class RNAMapWindow extends EDMapWindow //implements ChangeListener, ActionListener, TransformSignalSubscriber
{
//{{{ Constants
    //DecimalFormat df1 = new DecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##################################################################################################
    /*
    KingMain    kMain;
    KinCanvas   kCanvas;
    ToolBox     parent;
    
    CrystalVertexSource     map;
    MarchingCubes           mc1, mc2;
    //RNAMapPlotter           plotter1, plotter2;
    EDMapPlotter    plotter1, plotter2;
    String                  title;
    
    JDialog     dialog;
    JSlider     extent, slider1, slider2;
    JCheckBox   label1, label2;
    JComboBox   color1, color2;
    JButton     draw, discard, export;

    float       ctrX, ctrY, ctrZ;

    */
    JCheckBox   planePicker, polyPicker;
    //RNAPolygonTracker   polyTracker;
    RNAPolyPlotter  polyPlotter;
    RNAPlanePlotter planePlotter;
    JComboBox  polyColor;
    JButton draw;

//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public RNAMapWindow(ToolBox parent, CrystalVertexSource map, String title)
    {
	super(parent, map, title);

        //this.parent     = parent;
        //kMain           = parent.kMain;
        //kCanvas         = parent.kCanvas;
        
        //parent.sigTransform.subscribe(this);
        
        //this.map        = map;
        //this.title      = title;
        //ctrX = ctrY = ctrZ = Float.NaN;
        
	//Object mode = MarchingCubes.MODE_TRIANGLE;
        //plotter1 = new EDMapPlotter(false, mode);
        //plotter2 = new EDMapPlotter(false, mode);
	//polyTracker = new RNAPolygonTracker();
	polyPlotter = new RNAPolyPlotter(map);
	planePlotter = new RNAPlanePlotter();
	addToGUI();
        //mc1 = new MarchingCubes(map, map, plotter1, MarchingCubes.MODE_MESH);
        //mc2 = new MarchingCubes(map, map, plotter2, MarchingCubes.MODE_MESH);
        
        //buildGUI();
        
        //dialog.pack();
        //dialog.setLocationRelativeTo(kMain.getTopWindow());
        //dialog.setVisible(true);

	//System.out.println("value at 63, 56, 134 converted to ind: " + map.getValue(59, 17, 99));
	
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    void addToGUI()
    {
        //dialog = new JDialog(kMain.getTopWindow(), title+" - RNAMap", false);
        //dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        //label1 = new JCheckBox("1.2 sigma", false);
        //label2 = new JCheckBox("3.0 sigma", true);

	polyPicker = new JCheckBox("Poly Picker", true);
	planePicker = new JCheckBox("Plane Picker", false);
        
        //color1 = new JComboBox(kMain.getKinemage().getAllPaintMap().values().toArray());
        //color1.setSelectedItem(KPalette.gray);
        //color2 = new JComboBox(kMain.getKinemage().getAllPaintMap().values().toArray());
        //color2.setSelectedItem(KPalette.purple);

	polyColor = new JComboBox(kMain.getKinemage().getAllPaintMap().values().toArray());
	polyColor.setSelectedItem(KPalette.gold);
        
        //extent = new JSlider(0, 40, 10);
        //extent.setMajorTickSpacing(10);
        //extent.setMinorTickSpacing(2);
        //extent.setPaintTicks(true);
        //extent.setPaintLabels(true);
        
        //slider1 = new JSlider(-80, 80, 12);
        //slider1.setMajorTickSpacing(10);
        //slider1.setPaintTicks(true);
        //slider1.setPaintLabels(false);

        //slider2 = new JSlider(-80, 80, 30);
        //slider2.setMajorTickSpacing(10);
        //slider2.setPaintTicks(true);
        //slider2.setPaintLabels(false);
        
        //discard = new JButton(new ReflectiveAction("Discard this map", null, this, "onMapDiscard"));
        //export  = new JButton(new ReflectiveAction("Export to kinemage", null, this, "onMapExport"));
	draw = new JButton(new ReflectiveAction("Draw perpendicular", null, this, "onDraw"));
        
        //label1.addActionListener(this);
        //label2.addActionListener(this);
	polyPicker.addActionListener(this);
	planePicker.addActionListener(this);
        //color1.addActionListener(this);
        //color2.addActionListener(this);
	polyColor.addActionListener(this);
        //extent.addChangeListener(this);
        //slider1.addChangeListener(this);
        //slider2.addChangeListener(this);
        
        TablePane pane = (TablePane) dialog.getContentPane();
	/*
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
	pane.add(polyPicker);
	pane.add(polyColor);
        pane.newRow();
	pane.add(planePicker);
	pane.newRow();
        pane.center().hfill(true);
	pane.add(draw, 2, 1);
	pane.newRow();
        pane.add(export, 2, 1);
        pane.newRow();
        pane.add(discard, 2, 1);
        */
	pane.add(pane.strut(0,4));
	pane.newRow();
	pane.add(polyPicker);
	pane.add(polyColor);
        pane.newRow();
	pane.add(planePicker);
	pane.newRow();
        pane.center().hfill(true);
	pane.add(draw, 2, 1);
        dialog.setContentPane(pane);
    }
//}}}

//{{{ stateChanged, actionPerformed, calcSliderValue
//##################################################################################################
    /*
    public void stateChanged(ChangeEvent ev)
    {
        double val;
        val = calcSliderValue(slider1);
        label1.setText(df1.format(val)+" sigma");
        val = calcSliderValue(slider2);
        label2.setText(df1.format(val)+" sigma");
        
	if(!(extent.getValueIsAdjusting())) {
	    updateMesh();
	}
        kCanvas.repaint();
    }
    
    public void actionPerformed(ActionEvent ev)
    {
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
    */
//}}}

//{{{ centerChanged
//##################################################################################################
    /**
    * Reports on whether the viewing center has been changed.
    * Has the side effect of updating the internal center to match the current view.
    */
    /*
    boolean centerChanged()
    {
        KView v = kMain.getView();
        if(v == null) return false;
        
        float[] ctr = v.getCenter();
        boolean ret = (ctrX != ctr[0] || ctrY != ctr[1] || ctrZ != ctr[2]);
        
        ctrX = ctr[0];
        ctrY = ctr[1];
        ctrZ = ctr[2];
        
        return ret;
    }
    */
//}}}

//{{{ updateMesh
//##################################################################################################
    /*
    void updateMesh()
    {
        if(Float.isNaN(ctrX) || Float.isNaN(ctrY) || Float.isNaN(ctrZ)) return;
        
        double val, size = extent.getValue() / 2.0;
        int[] corner1 = new int[3], corner2 = new int[3];
        
        map.findVertexForPoint(ctrX-size, ctrY-size, ctrZ-size, corner1);
        map.findVertexForPoint(ctrX+size, ctrY+size, ctrZ+size, corner2);
        
        val = calcSliderValue(slider1);
        mc1.march(corner1[0], corner1[1], corner1[2], corner2[0], corner2[1], corner2[2], val*map.sigma);
        
        val = calcSliderValue(slider2);
        mc2.march(corner1[0], corner1[1], corner1[2], corner2[0], corner2[1], corner2[2], val*map.sigma);
        
        //SoftLog.err.println("Updated mesh: "+corner1[0]+" "+corner1[1]+" "+corner1[2]+" / "+corner2[0]+" "+corner2[1]+" "+corner2[2]);
    }
    */
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
	super.signalTransform(engine, xform);
        KList list;
        if(centerChanged()) updateMesh();
        /*
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
	*/
	list = polyPlotter.getList();
	if (list != null && polyPicker.isSelected()) {
	    list.setColor((KPaint)polyColor.getSelectedItem());
	    list.signalTransform(engine, xform);
	}

	list = planePlotter.getList();
	if (list != null && planePicker.isSelected()) {
	    //list.setColor((KPaint)polyColor.getSelectedItem());
	    list.signalTransform(engine, xform);
	}
        
        //SoftLog.err.println("Painted maps.");
    }
//}}}
    
//{{{ onMapDiscard, onMapExport
//##################################################################################################

    public void onDraw(ActionEvent ev) {
	VectorPoint phos = polyPlotter.getPhosphate();
	RNATriple intersect = planePlotter.getPointPlaneIntersect(phos);
	polyPlotter.addPoint(intersect);
	//updateMesh();
	kCanvas.repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /*
    public void onMapDiscard(ActionEvent ev)
    {
        dialog.dispose();
        parent.sigTransform.unsubscribe(this);
	parent.activateDefaultTool();
        kCanvas.repaint();
    }
    */

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMapExport(ActionEvent ev)
    {
        // insert lists into kinemage
	super.onMapExport(ev);

        Kinemage kin = kMain.getKinemage();
        KGroup group = null;
	KSubgroup subgroup = null;
        KList polyList = polyPlotter.getList();
	KList planeList = planePlotter.getList();

	if ((polyList != null)&&(planeList != null)) {
	    group = new KGroup(kin, "RNA map");
	    kin.add(group);
	    
	    subgroup = new KSubgroup(group, "RNA map");
	    subgroup.setHasButton(false);
	    group.add(subgroup);
	}

	
        //list1 = plotter1.getList(); plotter1.freeList();
        //list2 = plotter2.getList(); plotter2.freeList();
	//polyList = polyPlotter.getList();
	//planeList = planePlotter.getList();
	/*
        if(list1 != null && label1.isSelected())
        {
            list1.setParent(subgroup);
            subgroup.add(list1);
        }
        if(list2 != null && label2.isSelected())
        {
            list2.setParent(subgroup);
            subgroup.add(list2);
        }
	*/
	if (polyList != null && polyPicker.isSelected()) {
	    polyList.setParent(subgroup);
	    subgroup.add(polyList);
	}
	if (planeList != null && planePicker.isSelected()) {
	    planeList.setParent(subgroup);
	    subgroup.add(planeList);
	}	
        updateMesh(); // regenerate the meshes we just exported
        
        kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }
//}}}

    public boolean polyIsSelected() {
	return polyPicker.isSelected();
    }

    public boolean planeIsSelected() {
	return planePicker.isSelected();
    }

    
    public void polyTrack(VectorPoint p) {
	polyPlotter.polyTrack(p);
    }

    public void planeTrack(KPoint p) {
	planePlotter.getPlane(p);
    }

    /*****
// Old method, where the selected polygons were added directly to kinemage.
// It had the strange side effect of creating highlighted lines, i.e. 
// the selected polyhedra had gold highlighted purple lines...
    public void onTracked(KList trackedList) {

	Kinemage kin = kMain.getKinemage();
	KSubgroup subGroup;
	KList oldList, newList;
	KGroup group = new KGroup(kin, "tracker Group");

	kin.add(group);
	subGroup = new KSubgroup(group, "tracker Subgroup");
	subGroup.setHasButton(false);
	group.add(subGroup);
	trackedList.setParent(subGroup);
	subGroup.add(trackedList);
	
	//settID("tracking is going?");
	//System.out.println("on tracking...");
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
	//kCanvas.repaint();
	
	
	
    }
    ************/

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

