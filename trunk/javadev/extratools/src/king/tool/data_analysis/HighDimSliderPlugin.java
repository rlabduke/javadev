// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.data_analysis;
import king.*;
import king.core.*;
import king.tool.loops.*;
import driftwood.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

//}}}

public class HighDimSliderPlugin extends Plugin implements ChangeListener {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  HighLowSliders[] sliders;
  //}}}
  
  //{{{ Constructors
  public HighDimSliderPlugin(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ onStart
  public void onStart(ActionEvent ev) {
    buildGUI();
  }
  //}}}
  
  //{{{ buildGUI
  public void buildGUI() {
    Kinemage kin = kMain.getKinemage();
    TablePane2 pane = new TablePane2();
    int numDim = kin.dimensionNames.size();
    ArrayList<Number> minMax = new ArrayList<Number>(kin.dimensionMinMax);
    ArrayList<String> dimNames = new ArrayList<String>(kin.dimensionNames);
    sliders = new HighLowSliders[numDim];
    for (int i = 0; i < numDim; i++) {
      int lowVal = (int) Math.floor(minMax.get(i*2).doubleValue());
      int highVal = (int) Math.ceil(minMax.get(i*2+1).doubleValue());
      sliders[i] = new HighLowSliders(lowVal, highVal, 100);
      sliders[i].setMajorTickSpacing(20);
      sliders[i].setPaintTicks(true);
      sliders[i].addChangeListener(this);
      pane.top();
      pane.addCell(new JLabel(dimNames.get(i)), 1, 2);
      pane.middle();
      pane.add(sliders[i].getLowSlider());
      pane.add(sliders[i].getHighSlider());
      pane.newRow();
      pane.add(sliders[i].getLowLabel());
      pane.add(sliders[i].getHighLabel());
      pane.newRow();
    }
    JDialog dialog = new JDialog(kMain.getTopWindow(), "High Dim Sliders", false);
    //dialog.addWindowListener(this);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setContentPane(pane);
    dialog.pack();
    dialog.show();
    
  }
  //}}}
  
  //{{{ stateChanged
  public void stateChanged(ChangeEvent ev) {
    Kinemage kin = kMain.getKinemage();
    if(kin == null) return;
    for (HighLowSliders slider : sliders) {
      slider.stateChanged(ev);
    }
    String key = ParaParams.class.getName()+".instance";
    ParaParams params = (ParaParams) kin.metadata.get(key);
    if((params != null)&&(params.isInParallelMode())) {
      //params.parallelChildren.clear();
      setPoints();
      //params.redrawParallel();
    } else {
      setPoints();
    }
  }
  //}}}
  
  //{{{ setPoints
  public void setPoints() {
    Kinemage kin = kMain.getKinemage();
    KIterator<KList> lists = KIterator.allLists(kin);
    for (KList list : lists) {
      if (list.isOn()) {
        KIterator<KPoint> points = KIterator.allPoints(list);
        for (KPoint point : points) {
          point.setOn(true);
        }
        points = KIterator.allPoints(list);
        for (KPoint point : points) {
          float[] coords = point.getAllCoords();
          if (coords != null) {
            for (int i = 0; (i < coords.length && point.isOn()) ; i++) {
              float value = coords[i];
              HighLowSliders hls = sliders[i];
              if ((value < hls.getLowValue())||(value > hls.getHighValue())) {
                point.setOn(false);
              }
            }
          }
        }
      }
    }
  }
  //}}}
  
  //{{{ setParallelPoints
  //public void setParallelPoints() {
  //  Kinemage kin = kMain.getKinemage();
  //  KIterator<KList> lists = KIterator.allLists(kin);
  //  for (KList list : lists) {
  //    if (list.isOn()) {
  //      
  //}}}
  
  //{{{ turnOffPoints
  //public void turnOffPoints() {
  //  Kinemage kin = kMain.getKinemage();
  //  KIterator<KPoint> points = KIterator.visiblePoints(kin);
  //  for (KPoint point : points) {
  //    //System.out.println(point);
  //    float[] coords = point.getAllCoords();
  //    if (coords != null) {
  //      for (int i = 0; (i < coords.length && point.isOn()) ; i++) {
  //        float value = coords[i];
  //        HighLowSliders hls = sliders[i];
  //        if ((value < hls.getLowValue())||(value > hls.getHighValue())) {
  //          point.setOn(false);
  //        }
  //      }
  //    }
  //  }
  //}
  //}}}
  
  //{{{ getToolsMenuItem
  public JMenuItem getToolsMenuItem() {
    return new JMenuItem(new ReflectiveAction("High Dimensional Sliders", null, this, "onStart"));
  }
  //}}}
  
}
