// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import com.apple.eawt.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>MacDropTarget</code> provides drag-and-drop document opening
* under Mac OS X. This file will not compile on non-Mac platforms
* and should be removed (no other code depends on it).
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Nov  7 09:41:14 EST 2003
*/
public class MacDropTarget implements ApplicationListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    static Application application = null;
    KingMain kMain;
//}}}

//{{{ Constructor(s)
//##############################################################################
    private MacDropTarget(KingMain kmain)
    {
        super();
        kMain = kmain;
    }
//}}}

//{{{ bindTo
//##############################################################################
    static public void bindTo(KingMain kMain)
    {
        if(application == null)
            application = new Application();
        
        MacDropTarget drop = new MacDropTarget(kMain);
        application.addApplicationListener(drop);
    }
//}}}

//{{{ Unhandled events
//##############################################################################
    public void handleAbout(ApplicationEvent e) {
    }
    public void handleOpenApplication(ApplicationEvent e) {
    }
    public void handleReOpenApplication(ApplicationEvent e) {
    }
    public void handlePreferences(ApplicationEvent e) {
    }
    public void handlePrintFile(ApplicationEvent e) {
    }
//}}}

//{{{ handleOpenFile, handleQuit
//##############################################################################
    public void handleOpenFile(ApplicationEvent ev)
    {
        //SoftLog.err.println("Received notification of file drop!");
        KinfileIO io = kMain.getKinIO();
        File f = new File(ev.getFilename());
        if(f.exists())
        {
            io.loadFile(f, null);
            ev.setHandled(true);
        }
        else SoftLog.err.println("Filename does not exist: '"+ev.getFilename()+"'");
    }
    
    public void handleQuit(ApplicationEvent ev)
    {
        // Necessary in order for Cmd-Q to work
        ev.setHandled(true);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

