// (jEdit options) :folding=explicit:collapseFolds=1:
package king.tool.moteking;

import king.*;
import king.core.*;

import motej.*;
import motej.event.CoreButtonEvent;
import motej.event.CoreButtonListener;

public class MoteTool extends BasicTool {
  
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
		mote.addCoreButtonListener(new CoreButtonListener() {
		
			public void buttonPressed(CoreButtonEvent evt) {
				if (evt.isButtonAPressed()) {
					System.out.println("Button A pressed!");
				}
				if (evt.isButtonBPressed()) {
					System.out.println("Button B pressed!");
				}
				if (evt.isNoButtonPressed()) {
					System.out.println("No button pressed.");
				}
			}
		
		});
  }
  //}}}
  
  //{{{ stop
  public void stop() {
    mote.disconnect();
  }
  //}}}
  
  public String toString() { return "Mote Navigate"; }
}

