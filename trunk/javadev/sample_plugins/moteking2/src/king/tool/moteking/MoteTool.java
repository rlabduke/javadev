// (jEdit options) :folding=explicit:collapseFolds=1:
package king.tool.moteking;

import king.*;
import king.core.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import driftwood.gui.*;

import wiiremotej.*;
import wiiremotej.event.*;
//import motej.request.*;

public class MoteTool extends BasicTool implements WiiRemoteListener {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  WiiRemote remote;
  TablePane2 pane;
  
  JButton connectButton;
  JCheckBox accelBox, irBox;
  IRMouse mouse = null;
  //}}}
  
  //{{{ Constructor
  public MoteTool(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ buildGUI
  public void buildGUI() {
    //dialog = new JDialog(kMain.getTopWindow(),"Mote Navigate", false);
    connectButton = new JButton(new ReflectiveAction("Connect to Mote", null, this, "onConnect"));
    accelBox = new JCheckBox(new ReflectiveAction("Use Accelerometer", null, this, "onAccel"));
    accelBox.setSelected(false);
    accelBox.setEnabled(false);
    irBox = new JCheckBox(new ReflectiveAction("Use IR Sensor", null, this, "onIR"));
    irBox.setSelected(false);
    irBox.setEnabled(false);
    pane = new TablePane2();
    pane.add(connectButton);
    pane.newRow();
    pane.add(accelBox);
    pane.newRow();
    pane.add(irBox);
  }
  //}}}
  
  //{{{ start
  public void start()
  {
    buildGUI();
    show();
    //try {
    //  remote = WiiRemoteJ.findRemote();
    //  if (remote != null) {
    //    //System.out.println("mote found!");
    //    remote.addWiiRemoteListener(this);
    //    //System.out.println("mote currently using "+remote.getInputReport());
    //    System.out.println("enabling accel");
    //    //remote.enableContinuous();
    //    //remote.setAccelerometerEnabled(true);
    //    //System.out.println("mote currently using "+remote.getInputReport());
    //    remote.setIRSensorEnabled(true, WRIREvent.BASIC);
    //    //System.out.println("mote currently using "+remote.getInputReport());
    //    remote.setLEDIlluminated(0, true);
    //    System.out.println("mote ready!");
    //  }
    //} catch (Exception e) {
    //  e.printStackTrace();
    //}
  }
  //}}}
  
  //{{{ on Functions
  public void onAccel(ActionEvent ev) {
    if (remote != null) {
      if (remote.isConnected()) {
        try {
          if (accelBox.isSelected()) {
            remote.setAccelerometerEnabled(true);
          } else {
            remote.setAccelerometerEnabled(false);
          }
        } catch (IOException ie) {
          ioeHandler(ie);
        }
      } else {
        System.out.println("No mote connected!");
      }
    }
  }
  
  public void onIR(ActionEvent ev) {
    if (remote != null) {
      if (remote.isConnected()) {
        try {
          if (irBox.isSelected()) {
            remote.setIRSensorEnabled(true, WRIREvent.BASIC);
            mouse = IRMouse.getDefault();
          } else {
            remote.setIRSensorEnabled(false, WRIREvent.BASIC);
            mouse = null;
          }
        } catch (IOException ie) {
          ioeHandler(ie);
        } catch (Exception e) {
          System.out.println("Mouse exception!");
          e.printStackTrace();
        }
      } else {
        System.out.println("No mote connected!");
      }
    }
  }
  
  public void onConnect(ActionEvent ev) {
    try {
      remote = WiiRemoteJ.findRemote();
      if (remote != null) {
        remote.addWiiRemoteListener(this);
        accelBox.setEnabled(true);
        irBox.setEnabled(true);
        remote.setLEDIlluminated(0, true);
      }
    } catch (IOException ie) {
      ioeHandler(ie);
    } catch (IllegalStateException ise) {
      System.out.println("Error with bluetooth!");
      ise.printStackTrace();
    } catch (InterruptedException ie) {
      System.out.println("Interrupted!");
      ie.printStackTrace();
    }
  }
  //}}}
  
  //{{{ ioeHandler
  public void ioeHandler(IOException ie) {
    if (remote != null) {
      remote.disconnect();
    }
    accelBox.setSelected(false);
    accelBox.setEnabled(false);
    irBox.setSelected(false);
    irBox.setEnabled(false);
    System.out.println("Error writing to mote!");
    ie.printStackTrace();
  }
  //}}}
  
  //{{{ buttonInputReceived
  public void buttonInputReceived(WRButtonEvent evt) {
    if (evt.wasPressed(WRButtonEvent.A + WRButtonEvent.B)) {
      //System.out.println("Button A was pressed!");
      //try {
      //  mouse = IRMouse.getDefault();
      //} catch (Exception e) {
      //  e.printStackTrace();
      //}
    }
    if (evt.isOnlyPressed(WRButtonEvent.A + WRButtonEvent.B)) {
      //System.out.println("Button A pressed!");
    } else {
      //mouse = null;
    }
    if (evt.isOnlyPressed(WRButtonEvent.B)) {
      System.out.println("Button B pressed!");
    }
    if (evt.isOnlyPressed(WRButtonEvent.PLUS)) {
      services.adjustZoom(1);
    }
    if (evt.isOnlyPressed(WRButtonEvent.MINUS)) {
      services.adjustZoom(-1);
    }
    if (evt.isOnlyPressed(WRButtonEvent.LEFT)) {
      services.translate(-5, 0);
    }
    if (evt.isOnlyPressed(WRButtonEvent.RIGHT)) {
      services.translate(5, 0);
    }
    if (evt.isOnlyPressed(WRButtonEvent.UP)) {
      services.translate(0, -5);
    }
    if (evt.isOnlyPressed(WRButtonEvent.DOWN)) {
      services.translate(0, 5);
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
    //System.out.println(xrotate + " : " + yrotate + " : " + (int)(evt.getZAcceleration()/5*300)+300);
    services.rotate(xrotate, yrotate);
  }
  //}}}
  
  //{{{ irInputReceived
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
  
  public void statusReported(WRStatusEvent evt) {}
  //}}}
  
  //{{{ stop
  public void stop() {
    remote.disconnect();
  }
  //}}}
  
  //{{{ toString
  /** Returns a component with controls and options for this tool */
  protected Container getToolPanel()
  { return pane; }
  
  public String toString() { return "Mote Navigate"; }
  //}}}
}

