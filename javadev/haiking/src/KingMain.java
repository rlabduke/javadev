// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package kinglite;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
//}}}
/**
* <code>KingMain</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jan 28 15:32:36 EST 2005
*/
public class KingMain extends MIDlet implements CommandListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KinCanvas       kCanvas;
    KinLoader       kLoader;
    Command         cmdExit, cmdMemInfo;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KingMain()
    {
        super();
        kCanvas = new KinCanvas(this);
        kLoader = new KinLoader(this);
        
        // Common commands
        cmdExit = new Command("Exit", Command.EXIT, 10);
        kCanvas.addCommand(cmdExit);
        kLoader.addCommand(cmdExit);

        cmdMemInfo = new Command("Mem. info", Command.HELP, 9);
        kCanvas.addCommand(cmdMemInfo);
        kLoader.addCommand(cmdMemInfo);
    }
//}}}

//{{{ start/pause/destroyApp
//##############################################################################
    public void startApp()
    {
        Display.getDisplay(this).setCurrent(kLoader);
    }
    
    public void pauseApp() {}
    
    public void destroyApp(boolean unconditional) {}
//}}}

//{{{ commandAction, info
//##############################################################################
    public void commandAction(Command c, Displayable s)
    {
        if(c == cmdExit) notifyDestroyed();
        else if(c == cmdMemInfo)
        {
            Runtime r = Runtime.getRuntime();
            info(r.freeMemory()+" bytes free, "+r.totalMemory()+" total.");
        }
    }

    public void info(String msg)
    {
        System.err.println(msg);
        Alert alert = new Alert("Info", msg, null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        Display.getDisplay(this).setCurrent(alert);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

