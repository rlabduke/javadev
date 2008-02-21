// (jEdit options) :folding=explicit:collapseFolds=1:
package king.tool.moteking;

import king.*;
import king.core.*;

import wiiremotej.*;
import wiiremotej.event.*;
//import motej.request.*;

public class MoteTool extends BasicTool implements WiiRemoteListener {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  WiiRemote remote;
  IRMouse mouse = null;
  //}}}
  
  //{{{ Constructor
  public MoteTool(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ start
  public void start()
  {
    try {
      remote = WiiRemoteJ.findRemote();
      if (remote != null) {
        //System.out.println("mote found!");
        remote.addWiiRemoteListener(this);
        //System.out.println("mote currently using "+remote.getInputReport());
        System.out.println("enabling accel");
        remote.enableContinuous();
        //remote.setAccelerometerEnabled(true);
        //System.out.println("mote currently using "+remote.getInputReport());
        remote.setIRSensorEnabled(true, WRIREvent.BASIC);
        //System.out.println("mote currently using "+remote.getInputReport());
        remote.setLEDIlluminated(0, true);
        System.out.println("mote ready!");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  //}}}
  
  //{{{ buttonInputReceived
  public void buttonInputReceived(WRButtonEvent evt) {
    if (evt.wasPressed(WRButtonEvent.A)) {
      System.out.println("Button A was pressed!");
      try {
        mouse = IRMouse.getDefault();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (evt.isOnlyPressed(WRButtonEvent.A)) {
      //System.out.println("Button A pressed!");
    } else {
      mouse = null;
    }
    if (evt.isOnlyPressed(WRButtonEvent.B)) {
      System.out.println("Button B pressed!");
    }
    if (evt.isOnlyPressed(WRButtonEvent.A + WRButtonEvent.B)) {
      System.out.println("Buttons A and B pressed!");
    }
    if (evt.isOnlyPressed(WRButtonEvent.PLUS)) {
      
    }
    //if (evt.isNoButtonPressed()) {
    //  System.out.println("No button pressed.");
    //}
  }
  //}}}
  
  //{{{ accelerationInputReceived
  public void accelerationInputReceived(WRAccelerationEvent evt) {
    int xrotate = (int)(evt.getXAcceleration()/5*50);
    int yrotate = (int)(evt.getYAcceleration()/5*50);
    System.out.println(xrotate + " : " + yrotate + " : " + (int)(evt.getZAcceleration()/5*300)+300);
    services.rotate(xrotate, yrotate);
  }
  //}}}
  
  //{{{ events
  public void combinedInputReceived(WRCombinedEvent evt) {
    //System.out.println("Combined Input!");
    //WRAccelerationEvent accel = evt.getAccelerationEvent();
    //if (accel != null) {
    //  this.accelerationInputReceived(accel);
    //}
    //WRButtonEvent but = evt.getButtonEvent();
    //if (but != null) {
    //  this.buttonInputReceived(but);
    //}
    //WRIREvent ir = evt.getIREvent();
    //if (ir != null) {
    //  this.IRInputReceived(ir);
    //}
  }
  
  public void disconnected() {}
  
  public void extensionConnected(WiiRemoteExtension extension) {}
  
  public void extensionDisconnected(WiiRemoteExtension extension) {}
  
  public void extensionInputReceived(WRExtensionEvent evt) {}
  
  public void extensionPartiallyInserted() {}
  
  public void extensionUnknown() {}
  
  public void IRInputReceived(WRIREvent evt) {
    //System.out.println("Seeing some IR lights?");
    for (IRLight light : evt.getIRLights())
    {
      if (light != null)
      {
        if (mouse != null) {
          mouse.processMouseEvent(evt);
        }
      }
    }
  }
  
  public void statusReported(WRStatusEvent evt) {}
  //}}}
  
  //{{{ stop
  public void stop() {
    remote.disconnect();
  }
  //}}}
  
  public String toString() { return "Mote Navigate"; }
}

