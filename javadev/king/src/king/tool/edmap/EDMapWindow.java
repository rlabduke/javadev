// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.edmap;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;
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
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Mar  5 09:00:11 EST 2003
*/
public class EDMapWindow implements ChangeListener, ActionListener, Transformable
{
//{{{ Constants
    DecimalFormat df1 = new DecimalFormat("0.0");
    static final String MAP_2FOFC = "2Fo-Fc";
    static final String MAP_FOFC = "Fo-Fc";
    static final String MAP_ANOMALOUS = "anomalous";
//}}}

//{{{ Variable definitions
//##################################################################################################
    protected KingMain    kMain;
    protected KinCanvas   kCanvas;
    ToolBox     parent;
    
    CrystalVertexSource     map;
    MarchingCubes           mc1, mc2;
    EDMapPlotter            plotter1, plotter2;
    String                  title;
    
    protected Window     dialog;
    JLabel      typeLabel;
    String      mapType;
    JSlider     extent, slider1, slider2;
    JCheckBox   label1, label2;
    JComboBox   color1, color2;
    JCheckBox   useTriangles, useLowRes;
    JButton     discard, export;
    
    boolean     phenixColors = false;
    
    float       ctrX, ctrY, ctrZ;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public EDMapWindow(ToolBox parent, CrystalVertexSource map, String title, boolean phenixColors)
    {
        this.parent     = parent;
        kMain           = parent.kMain;
        kCanvas         = parent.kCanvas;
        this.phenixColors = phenixColors;
        
        parent.transformables.add(this);
        
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
        
        mapType = parseType(title);
        
        Window lastEdMapWindow = null;
        for(Window win : kMain.getTopWindow().getOwnedWindows()) {
          if (win instanceof Dialog) {
            Dialog dia = (Dialog) win;
            //System.out.println(fra.getTitle());
            if ((dia.getTitle()!=null)&&(dia.getTitle().endsWith("EDMap"))) {
              lastEdMapWindow = dia;
            }
          }
          if (win instanceof Frame) {
            Frame fra = (Frame) win;
            //System.out.println(fra.getTitle());
            if ((fra.getTitle()!=null)&&(fra.getTitle().endsWith("EDMap"))) {
              lastEdMapWindow = fra;
            }
          }
        }
        
        buildGUI();
        
        setType(mapType);

        //if (lastEdMapWindow != null) {
        //  System.out.println(((Dialog)lastEdMapWindow).getTitle());
        //}
        
        dialog.pack();
        Container w = kMain.getContentContainer();
        if((w != null)&&(lastEdMapWindow == null))
        {
          Point p = w.getLocation();
          Dimension dimDlg = dialog.getSize();
          Dimension dimWin = w.getSize();
          p.x += dimWin.width - (dimDlg.width / 2) ;
          p.y += (dimWin.height - dimDlg.height) / 2;
          dialog.setLocation(p);
        } else {
          //System.out.println("Setting "+title+" relative to "+((Dialog)lastEdMapWindow).getTitle());
          Point p = lastEdMapWindow.getLocation();
          p.x += 50;
          p.y += 50;
          dialog.setLocation(p);
        }
        dialog.setVisible(true);
    }
    
//}}}

//{{{ parseTitle
/***
* Attempt to parse out the type of map from the file name to set appropriate presets.
*/
public String parseType(String title) {
  Pattern twoFoFcRegex = Pattern.compile("2[pm]?[Ff]o(\\-)?[qD]?[Ff]c[^.]*(fill)?");
  Pattern oneFoFcRegex = Pattern.compile("[pm]?[Ff]o(\\-)?[qD]?[Ff]c[^.]*(fill)?");
  Matcher twoMatcher = twoFoFcRegex.matcher(title);
  Matcher oneMatcher = oneFoFcRegex.matcher(title);
  if (twoMatcher.find()) {
    this.title = twoMatcher.group();
    return MAP_2FOFC;
  } else if (oneMatcher.find()) {
    this.title = oneMatcher.group();
    return MAP_FOFC;
  } else if (title.matches(".*ANOM\\..*")) {
    this.title = "Anomalous";
    return MAP_ANOMALOUS;
  }
  return "";
}
//}}}

//{{{ buildGUI
//##################################################################################################
    void buildGUI()
    {
      typeLabel = new JLabel("Set to: "+mapType);
      
        label1 = new JCheckBox("1.2 sigma", true);
        label2 = new JCheckBox("3.0 sigma", false);
        
        color1 = new JComboBox(kMain.getKinemage().getAllPaintMap().values().toArray());
        color1.setSelectedItem(KPalette.gray);
        color2 = new JComboBox(kMain.getKinemage().getAllPaintMap().values().toArray());
        color2.setSelectedItem(KPalette.purple);
        
        extent = new JSlider(0, 30, 15);
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
        useLowRes = new JCheckBox(new ReflectiveAction("Coarser mesh", null, this, "onCoarseMesh"));
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
        if (!mapType.equals("")) {
          pane.add(typeLabel);
          pane.newRow();
          pane.save().hfill(true).addCell(new JSeparator(),2,1).restore();
        }
        pane.newRow();
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
        pane.add(useLowRes, 2, 1);
        pane.newRow();
        pane.center().hfill(true);
        pane.add(export, 2, 1);
        pane.newRow();
        pane.add(discard, 2, 1);
        
        JMenuBar menubar = new JMenuBar();
        JMenu menu;
        JMenuItem item;
        
        menu = new JMenu("Presets");
        menu.setIcon(kMain.getPrefs().basicDownIcon);
        menu.setHorizontalTextPosition(AbstractButton.LEADING);
        menu.setMnemonic(KeyEvent.VK_P);
        menubar.add(menu);
        item = new JMenuItem(new ReflectiveAction("2Fo - Fc", null, this, "on2FoFc"));
        item.setMnemonic(KeyEvent.VK_2);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, KingMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Fo - Fc", null, this, "onFoFc"));
        item.setMnemonic(KeyEvent.VK_1);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, KingMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Coot 2Fo - Fc", null, this, "onCoot2FoFc"));
        item.setMnemonic(KeyEvent.VK_4);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, KingMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Coot Fo - Fc", null, this, "onCootFoFc"));
        item.setMnemonic(KeyEvent.VK_3);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, KingMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Anomalous", null, this, "onAnomalous"));
        item.setMnemonic(KeyEvent.VK_5);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, KingMain.MENU_ACCEL_MASK));
        menu.add(item);
        
        if (kMain.getPrefs().getBoolean("minimizableTools")) {
          JFrame fm = new JFrame(title+"-EDMap");
          fm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
          fm.setContentPane(pane);
          fm.setJMenuBar(menubar);
          dialog = fm;
        } else {
          JDialog dial = new JDialog(kMain.getTopWindow(), title+"-EDMap", false);
          dial.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
          dial.setContentPane(pane);
          dial.setJMenuBar(menubar);
          dialog = dial;
        }
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
        
        if(!extent.getValueIsAdjusting()
        && !slider1.getValueIsAdjusting()
        && !slider2.getValueIsAdjusting())
        {
            updateMesh();
            kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
        }
    }
    
    public void actionPerformed(ActionEvent ev)
    {
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }
    
    // target of reflection
    public void onTriangles(ActionEvent ev)
    {
        updateMesh();
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }
    
    // target of reflection
    public void onCoarseMesh(ActionEvent ev)
    {
        updateMesh();
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
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
    protected boolean centerChanged()
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
//}}}

//{{{ updateMesh
//##################################################################################################
    protected void updateMesh()
    {
        if(Float.isNaN(ctrX) || Float.isNaN(ctrY) || Float.isNaN(ctrZ)) return;
        
        // Regenerate our plotting apparatus here in case the user's
        // preference for std. mesh vs. cobwebs has changed.
        Object mode = (useTriangles.isSelected() ? MarchingCubes.MODE_TRIANGLE : MarchingCubes.MODE_MESH);
        plotter1 = new EDMapPlotter(false, mode);
        plotter2 = new EDMapPlotter(false, mode);
        
        double val, size = extent.getValue() / 2.0;
        int[] corner1 = new int[3], corner2 = new int[3];
        
        if(useLowRes.isSelected())
        {
            LowResolutionVertexSource lores = new LowResolutionVertexSource(map, 2);
            mc1 = new MarchingCubes(lores, lores, plotter1, mode);
            mc2 = new MarchingCubes(lores, lores, plotter2, mode);
            lores.findVertexForPoint(ctrX-size, ctrY-size, ctrZ-size, corner1);
            lores.findVertexForPoint(ctrX+size, ctrY+size, ctrZ+size, corner2);
        }
        else
        {
            mc1 = new MarchingCubes(map, map, plotter1, mode);
            mc2 = new MarchingCubes(map, map, plotter2, mode);
            map.findVertexForPoint(ctrX-size, ctrY-size, ctrZ-size, corner1);
            map.findVertexForPoint(ctrX+size, ctrY+size, ctrZ+size, corner2);
        }
        
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

//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform)
    {
        KList list;
        if(centerChanged()) updateMesh();
        
        list = plotter1.getList();
        if(list != null && label1.isSelected())
        {
            list.setColor((KPaint)color1.getSelectedItem());
            list.doTransform(engine, xform);
        }
        
        list = plotter2.getList();
        if(list != null && label2.isSelected())
        {
            list.setColor((KPaint)color2.getSelectedItem());
            list.doTransform(engine, xform);
        }
        
        //SoftLog.err.println("Painted maps.");
    }
//}}}

//{{{ setType
public void setType(String type) {
  if (type.equals(MAP_2FOFC)) {
    if (phenixColors) onCoot2FoFc(null);
    else              on2FoFc(null);
  } else if (type.equals(MAP_FOFC)) {
    if (phenixColors) onCootFoFc(null);
    else              onFoFc(null); //new ActionEvent(this, AWTEvent.WINDOW_EVENT_MASK, "")
  } else if (type.equals(MAP_ANOMALOUS)) {
    onAnomalous(null);
  }
}
//}}}

//{{{ on2FoFc, onFoFc
//##################################################################################################
    // Preset values for 2Fo-Fc maps
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void on2FoFc(ActionEvent ev)
    {
        slider1.setValue(12); // +1.2
        slider2.setValue(30); // +3.0
        color1.setSelectedItem(KPalette.gray);
        color2.setSelectedItem(KPalette.purple);
        
        updateMesh();
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }
    
    // Preset values for Fo-Fc (difference) maps
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFoFc(ActionEvent ev)
    {
        slider1.setValue(-35); // -3.5
        slider2.setValue( 35); // +3.5
        color1.setSelectedItem(KPalette.orange);
        color2.setSelectedItem(KPalette.sky);
        label1.setSelected(true);
        label2.setSelected(true);
        
        updateMesh();
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }
    
    public void onCoot2FoFc(ActionEvent ev)
    {
        slider1.setValue(12); // +1.2
        slider2.setValue(30); // +3.0
        color1.setSelectedItem(KPalette.sky);
        color2.setSelectedItem(KPalette.purple);
        
        updateMesh();
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }
    
    public void onCootFoFc(ActionEvent ev)
    {
        slider1.setValue(-30); // -3.5
        slider2.setValue( 30); // +3.5
        color1.setSelectedItem(KPalette.red);
        color2.setSelectedItem(KPalette.green);
        label1.setSelected(true);
        label2.setSelected(true);
        
        updateMesh();
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }
    
    public void onAnomalous(ActionEvent ev){
      slider1.setValue(30);
      color1.setSelectedItem(KPalette.yellow);
      label1.setSelected(true);
      
      updateMesh();
      kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }
//}}}
    
//{{{ onMapDiscard, onMapExport
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMapDiscard(ActionEvent ev)
    {
        dialog.dispose();
        parent.transformables.remove(this);
        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMapExport(ActionEvent ev)
    {
        // insert lists into kinemage
        Kinemage kin = kMain.getKinemage();
        
        KGroup group = new KGroup("ED map");
        kin.add(group);
        kin.setModified(true);
        
        KGroup subgroup = new KGroup("ED map");
        subgroup.setHasButton(false);
        group.add(subgroup);
        
        KList list1, list2;
        list1 = plotter1.getList(); plotter1.freeList();
        list2 = plotter2.getList(); plotter2.freeList();
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
        updateMesh(); // regenerate the meshes we just exported
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

