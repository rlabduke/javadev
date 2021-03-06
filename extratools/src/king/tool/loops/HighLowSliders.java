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
  int separator;
  //}}}
  
  //{{{ Constructor
  public HighLowSliders(int lowVal, int highVal, int mult) {
    separator = 0;
    multiplier = mult;
    lowSlider = new JSlider(lowVal * mult, highVal * mult, lowVal * mult);
    lowLabel = new JLabel(df.format((double)lowVal));
    highSlider = new JSlider(lowVal * mult, highVal * mult, highVal * mult);
    highLabel = new JLabel(df.format((double)highVal));
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
      if (val > highSlider.getValue()-separator) {
        highSlider.setValue(val+separator);
      }
      updateLabels();
    }
    if (source.equals(highSlider)) {
      int val = highSlider.getValue();
      if (val < lowSlider.getValue()+separator) {
        lowSlider.setValue(val-separator);
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
  
  public void setSeparator(int n) {
    separator = n*multiplier;
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
