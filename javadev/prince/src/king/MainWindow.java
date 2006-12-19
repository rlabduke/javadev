// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
//import java.util.*;
import javax.swing.*;
//}}}
/**
* <code>MainWindow</code> is a top-level holder for a ContentPane and a menu bar.
* Other than that, it doesn't do much!
*
* <p>Begun on Wed Apr 24 11:22:51 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class MainWindow extends JFrame // implements ...
{
//{{{ Variables
//##################################################################################################
//}}}

//{{{ Constructor
//##################################################################################################
    /**
    * Does minimal initialization for a main window.
    * @param kmain the KingMain that owns this window
    */
    public MainWindow(KingMain kmain)
    {
        super("KiNG "+kmain.getPrefs().getString("version"));

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setIconImage(kmain.prefs.windowIcon);
    }
//}}}

//{{{ shutdown
//##################################################################################################
    /** Initiates shutdown by calling dispose() on the window. */
    public void shutdown()
    {
        this.dispose();
    }
//}}}
}//class
