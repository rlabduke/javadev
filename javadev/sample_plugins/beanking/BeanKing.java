// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
import king.core.*;
import king.*;
import bsh.*;
import bsh.util.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>BeanKing</code> makes the Beanshell scripting environment accessible from within KiNG.
*
* TODO:
*   Fancier GUI with Help info and builtin scripts
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jul  1 15:23:55 EDT 2004
*/
public class BeanKing extends Plugin
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Interpreter        interp;
    JConsole            console;
    JFrame              frame;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BeanKing(ToolBox tb)
    {
        super(tb);
        
        console = new JConsole();
        console.setPreferredSize(new Dimension(400, 300));

        frame = new JFrame(this.toString());
        frame.getContentPane().add(console);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();

        interp = new Interpreter(console);
        new Thread(interp).start();
        
        try
        {
            interp.set("parent", parent);
            interp.set("kMain", kMain);
            interp.set("kCanvas", kCanvas);
            interp.set("services", services);
        }
        catch(EvalError e)
        { e.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ toString, getToolsMenuItem, onShow
//##############################################################################
    public String toString()
    { return "Beanshell console"; }
    
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onShow"));
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShow(ActionEvent ev)
    {
        frame.setVisible(true);
    }
//}}}

//{{{ getHelpURL
//##############################################################################
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        URL url = null;
        try { url = new URL("http://www.beanshell.org/"); }
        catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
        return url;
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

