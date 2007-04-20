// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import java.text.*;
import javax.swing.*;
import javax.swing.event.*;
//}}}

public class HighLowSliders {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.00");
  //}}}
  
  //{{{ Variables
  JSlider lowSlider;
  JLabel lowLabel;
  JSlider highSlider;
  JLabel highLabel;
  int multiplier;
  //}}}
  
  //{{{ Constructor
  public HighLowSliders(int lowVal, int highVal, int mult) {
    multiplier = mult;
    lowSlider = new JSlider(lowVal * mult, highVal * mult, lowVal * mult);
    lowLabel = new JLabel(Integer.toString(lowVal));
    highSlider = new JSlider(lowVal * mult, highVal * mult, highVal * mult);
    highLabel = new JLabel(Integer.toString(highVal));
  }
  //}}}
  
  //{{{ addChangeListener
  public void addChangeListener(ChangeListener l) {
    lowSlider.addChangeListener(l);
    highSlider.addChangeListener(l);
  }
  //}}}
  
  //{{{ contains
  public boolean contains(JSlider slider) {
    return (slider.equals(lowSlider)||slider.equals(highSlider));
  } 
  //}}}
  
  //{{{ stateChanged
  public void stateChanged(ChangeEvent ev) {
    JSlider source = (JSlider) ev.getSource();
    if (source.equals(lowSlider)) {
      int val = lowSlider.getValue();
      if (val > highSlider.getValue()) {
        highSlider.setValue(val);
      }
      updateLabels();
    }
    if (source.equals(highSlider)) {
      int val = highSlider.getValue();
      if (val < lowSlider.getValue()) {
        lowSlider.setValue(val);
      }
      updateLabels();
    }
  }
  //}}}
  
  //{{{ updateLabels
  public void updateLabels() {
    lowLabel.setText(df.format(((double)lowSlider.getValue()) / multiplier));
    highLabel.setText(df.format(((double)highSlider.getValue()) / multiplier));
  }
  //}}}
  
  //{{{ set Functions
  public void setMajorTickSpacing(int n) {
    lowSlider.setMajorTickSpacing(n * multiplier);
    highSlider.setMajorTickSpacing(n * multiplier);
  }
  
  public void setPaintTicks(boolean b) {
    lowSlider.setPaintTicks(b);
    highSlider.setPaintTicks(b);
  }
  //}}}
  
  //{{{ get functions
  public JSlider getLowSlider() {
    return lowSlider;
  }
  
  public JSlider getHighSlider() {
    return highSlider;
  }
  
  public JLabel getLowLabel() {
    return lowLabel;
  }

  public JLabel getHighLabel() {
    return highLabel;
  }
  
  public double getLowValue() {
    return ((double)lowSlider.getValue()) / multiplier;
  }
  
  public double getHighValue() {
    return ((double)highSlider.getValue()) / multiplier;
  }
  //}}}
  
  
  
}
