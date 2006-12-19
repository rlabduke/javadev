// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

//import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>Plugin</code> is a way for arbitrary, specialized
* functionalities to be incorporated into KiNG in a
* highly modular manner.
*
* To be a Plugin, a class should at a minimum implement getToolsMenuItem(),
* getHelpMenuItem(), and toString().
* It's often easier to implement getHelpAnchor() than getHelpMenuItem().
* More complicated plugins should implement getDependencies() and isAppletSafe().
*
* Plugins are very similar to Tools, except that Plugins
* do not receive mouse events from the graphics window; and
* Plugins are not exclusive, whereas activating one Tool
* necessarily de-activates the previous one.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Apr  1 12:23:40 EST 2003
*/
abstract public class Plugin //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    // These are protected so that the Plugin can be subclassed
    // outside of the "king" package.
    protected ToolBox       parent;

    protected KingMain      kMain;
    protected KinCanvas     kCanvas;
    protected ToolServices  services;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Plugin(ToolBox tb)
    {
        parent = tb;
        
        kMain       = tb.kMain;
        kCanvas     = tb.kCanvas;
        services    = tb.services;
    }
//}}}

//{{{ getToolsMenuItem, getHelpMenuItem
//##################################################################################################
    /**
    * Creates a new JMenuItem to be displayed in the Tools menu,
    * which will allow the user to access function(s) associated
    * with this Plugin.
    *
    * Only one JMenuItem may be returned, but it could be a JMenu
    * that contained several functionalities under it.
    *
    * The Plugin may return null to indicate that it has no
    * associated menu item.
    */
    abstract public JMenuItem getToolsMenuItem();

    /**
    * Creates a new JMenuItem to be displayed in the Help menu,
    * which will allow the user to access help information associated
    * with this Plugin.
    *
    * Only one JMenuItem may be returned, but it could be a JMenu
    * that contained several items under it. However,
    * Plugins are encouraged to consolidate all documentation
    * into one location. The king.HTMLHelp class may be of use here.
    *
    * By default, a menu item will be created that signals the onHelp()
    * function, which in turn displays the HTML page named by getHelpURL().
    *
    * The Plugin may return null to indicate that it has no
    * associated menu item.
    */
    public JMenuItem getHelpMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onHelp"));
    }
//}}}

//{{{ onHelp, getHelpURL, getHelpAnchor
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHelp(ActionEvent ev)
    {
        URL start = this.getHelpURL();
        if(start != null) new HTMLHelp(kMain, start).show();
        else JOptionPane.showMessageDialog(kMain.getTopWindow(), "Unable to find documentation for this plugin.", "Sorry!", JOptionPane.ERROR_MESSAGE);
    }

    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        URL     url     = getClass().getResource("/king/html/king-manual.html");
        String  anchor  = getHelpAnchor();
        if(url != null && anchor != null)
        {
            try { url = new URL(url, anchor); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
            return url;
        }
        else return null;
    }
    
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this plugin. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#edmap-plugin" (or null)
    */
    public String getHelpAnchor()
    { return null; }
//}}}

//{{{ getDependencies, isAppletSafe
//##################################################################################################
    /**
    * Returns a Collection&lt;String&gt; of all Plugins that must be instantiated
    * before this one is. If one or more dependencies cannot be resolved,
    * the plugin will generally not be loaded;
    * thus, be careful to avoid circular dependencies.
    * @return a Collection of the fully-qualified names of all plugins that
    * this Plugin depends on, as Strings. The default is no dependencies.
    */
    static public Collection getDependencies()
    {
        return Collections.EMPTY_LIST;
    }
    
    /**
    * Returns true if and only if this plugin is safe to instantiate when
    * KiNG is running as an applet in a web browser.
    * Plugins that access the file system or arbitrary URLs (among other things)
    * should override this method to return true.
    * @return the default value of false
    */
    static public boolean isAppletSafe()
    {
        return true;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

