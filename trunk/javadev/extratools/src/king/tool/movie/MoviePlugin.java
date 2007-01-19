// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.movie;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
//}}}
/**
* <code>MoviePlugin</code> has not yet been documented.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 15 10:01:22 EST 2007
*/
public class MoviePlugin extends Plugin
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MoviePlugin(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ getToolsMenuItem, isAppletSafe
//##############################################################################
    public JMenuItem getToolsMenuItem()
    {
        JMenuItem item = new JMenuItem(new ReflectiveAction("Make movie...", null, this, "onMakeMovie"));
        return item;
    }
    
    static public boolean isAppletSafe()
    {
        return false;
    }
//}}}

//{{{ onMakeMovie
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMakeMovie(ActionEvent ev)
    {
        new MovieMaker(kMain);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

