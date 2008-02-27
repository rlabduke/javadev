// (jEdit options) :folding=explicit:collapseFolds=1:
package king.tool.moteking;

import king.*;
import king.core.*;

import motej.*;
import motej.event.*;
import motej.request.*;

public class MoteTool extends BasicTool implements CoreButtonListener, AccelerometerListener {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  Mote mote;
  //}}}
  
  //{{{ Constructor
  public MoteTool(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ start
  public void start()
  {
    mote = MoteFinder.getMoteFinder().findMote();
    System.out.println("mote found!");
		mote.addCoreButtonListener(this);
    System.out.println("adding accel listener");
    mote.addAccelerometerListener(this);
    mote.setReportMode(ReportModeRequest.DATA_REPORT_0x31, true);
    System.out.println("mote ready!");
  }
  //}}}
  
  //{{{ buttonPressed
  public void buttonPressed(CoreButtonEvent evt) {
    if (evt.isButtonAPressed()) {
      System.out.println("Button A pressed!");
    }
    if (evt.isButtonBPressed()) {
      System.out.println("Button B pressed!");
    }
    //if (evt.isNoButtonPressed()) {
    //  System.out.println("No button pressed.");
    //}
  }
  //}}}
  
  //{{{ accelerometerChanged
  public void accelerometerChanged(AccelerometerEvent evt) {
    System.out.println(evt.getX() + " : " + evt.getY() + " : " + evt.getZ());
  }
  //}}}
  
  //{{{ stop
  public void stop() {
    mote.setReportMode(ReportModeRequest.DATA_REPORT_0x30);
    mote.disconnect();
  }
  //}}}
  
  public String toString() { return "Mote Navigate"; }
}

