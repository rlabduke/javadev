// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.lang.reflect.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;

import driftwood.gui.TablePane;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>ToolBox</code> instantiates and coordinates all the tools and plugins,
* using the Reflection API and a service provider (SPI) model,
* like the one in javax.imageio.ImageIO.scanForPlugins().
* We scan all jar files on the classpath for lists of Plugins that could be loaded by KiNG.
* Plugins are listed in a plain text file <code>META-INF/services/king.Plugin</code>
* that's part of one or more JARs on the classpath.
* One fully-qualified class name is given per line, and nothing else.
*
* <p>Likewise, the submenu of Tools that a plugin belongs to, if any, is determined
* in the preferences file by the <i>classname<i><code>.menuName</code> property.
* The special values <code>&lt;main menu&gt;</code> and <code>&lt;not shown&gt;</code>
* put the plugin in the main Tools menu or don't put in anywhere at all, respectively.
* KingPrefs scans all jars for files named <code>king/king_prefs</code>,
* so plugins bundled in separate jars can include such a file to describe
* which menus they belong in.
*
* <p>Begun on Fri Jun 21 09:30:40 EDT 2002
* <br>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
*/
public class ToolBox implements MouseListener, MouseMotionListener, TransformSignalSubscriber
{
//{{{ Static fields
    /** The menu name that will put a Plugin in the main menu rather than a submenu. */
    static final String MENU_MAIN = "<main menu>";
    /** The menu name that will keep a Plugin out of the Tools menu entirely. */
    static final String MENU_NONE = "<not shown>";
//}}}

//{{{ CLASS: PluginComparator, MenuComparator
//##################################################################################################
    /** Sorts tools before plugins, then alphabetically by name. */
    static class PluginComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            boolean tool1 = (o1 instanceof BasicTool);
            boolean tool2 = (o2 instanceof BasicTool);
            if(tool1 && !tool2)         return -1;
            else if(tool2 && !tool1)    return 1;
            else return o1.toString().compareTo(o2.toString());
        }
    }
    /** Sorts JMenus alphabetically by name. */
    static class MenuComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            JMenu m1 = (JMenu) o1;
            JMenu m2 = (JMenu) o2;
            return m1.getText().compareTo(m2.getText());
        }
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    // These are public so tools in any package can access them.
    public KingMain         kMain;
    public KinCanvas        kCanvas;
    public ToolServices     services;
    public TransformSignal  sigTransform;
    
    ArrayList               plugins;
    BasicTool               activeTool;
    final BasicTool         defaultTool;
    JMenuItem               activeToolMI = null, defaultToolMI = null;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public ToolBox(KingMain kmain, KinCanvas kcanv)
    {
        // These are referenced by the tools
        kMain       = kmain;
        kCanvas     = kcanv;
        services    = new ToolServices(this);
        sigTransform = new TransformSignal();
        
        plugins = new ArrayList();
        defaultTool = activeTool = new BasicTool(this);
        plugins.add(activeTool);
        loadPlugins();
        activeTool.start();
    }
//}}}

//{{{ loadPlugins
//##################################################################################################
    /**
    * Automatically loads all the tools and plugins that are currently available
    * to the system, while respecting their dependencies and applet safety.
    */
    void loadPlugins()
    {
        // returned list might not be mutable, so make a copy
        Collection toLoad = new ArrayList(scanForPlugins());
        int oldSize;
        do // cycle through all remaining names until no more can be loaded
        {
            oldSize = toLoad.size();
            for(Iterator iter = toLoad.iterator(); iter.hasNext(); )
            {
                String name = (String) iter.next();
                if(canLoadPlugin(name))
                {
                    // Only try once, because we should succeed.
                    addPluginByName(name);
                    iter.remove();
                }
            }
        }
        while(oldSize > toLoad.size());
        
        Collections.sort(plugins, new PluginComparator());
    }
//}}}

//{{{ scanForPlugins
//##################################################################################################
    /**
    * Using a service-provider (SPI) model like the one in ImageIO.scanForPlugins(),
    * we scan all jar files on the classpath for lists of Plugins that
    * could be loaded by KiNG.
    * Plugins are listed in a plain text file <code>META-INF/services/king.Plugin</code>
    * that's part of one or more JARs on the classpath.
    * @return a Collection&lt;String&gt; of fully-qualified plugin names.
    */
    Collection scanForPlugins()
    {
        Collection pluginNames = new ArrayList();
        try
        {
            ClassLoader loader = this.getClass().getClassLoader();
            // No leading slashes when using this method
            Enumeration urls = loader.getResources("META-INF/services/king.Plugin");
            while(urls.hasMoreElements())
            {
                URL url = (URL) urls.nextElement();
                try
                {
                    LineNumberReader in = new LineNumberReader(new InputStreamReader(url.openStream()));
                    String s;
                    while((s = in.readLine()) != null)
                    {
                        s = s.trim();
                        if(!s.equals("") && !s.startsWith("#"))
                            pluginNames.add(s);
                    }
                }
                catch(IOException ex)
                { SoftLog.err.println("Plugin SPI error: "+ex.getMessage()); }
            }
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        
        return pluginNames;
    }
//}}}

//{{{ canLoadPlugin
//##################################################################################################
    /**
    * Returns true iff the following conditions are met:
    * (1) the named class can be located and loaded
    * (2) the plugin is applet-safe, or we're running as an application
    * (3) all the plugins this one depends on are already loaded.
    * @param className the fully qualified name of the Plugin class to check
    */
    boolean canLoadPlugin(String className)
    {
        // Make a list of the full names of loaded plugins
        Set loadedPlugins = new HashSet();
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
            loadedPlugins.add(iter.next().getClass().getName());
        
        try
        {
            Class pluginClass = Class.forName(className);
            Method appletSafe = pluginClass.getMethod("isAppletSafe", new Class[] {});
            Boolean safe = (Boolean) appletSafe.invoke(null, new Object[] {});
            if(kMain.getApplet() != null && safe.booleanValue() == false)
                return false; // can't load because we're not applet safe
            Method getDepend = pluginClass.getMethod("getDependencies", new Class[] {});
            Collection deps = (Collection) getDepend.invoke(null, new Object[] {});
            for(Iterator iter = deps.iterator(); iter.hasNext(); )
            {
                if(!loadedPlugins.contains(iter.next()))
                    return false; // can't load because of a dependency
            }
            return true; // can load; we've passed all the tests
        }
        catch(Throwable t)
        {
            t.printStackTrace(SoftLog.err);
            return false; // can't load because of a reflection error
        }
    }
//}}}

//{{{ getPluginList, getPluginMenuName
//##################################################################################################
    /** Returns an unmodifiable List of all installed plugins */
    public java.util.List getPluginList()
    { return Collections.unmodifiableList(plugins); }
    
    /** Returns the name of the menu the given Plugin belongs in right now */
    public String getPluginMenuName(Plugin p)
    {
        KingPrefs prefs = kMain.getPrefs();
        String menuName = prefs.getString(p.getClass().getName()+".menuName", MENU_MAIN).trim();
        if(menuName.equals("")) menuName = MENU_MAIN;
        return menuName;
    }
//}}}

//{{{ addPluginByName
//##################################################################################################
    /**
    * Tries to instantiate a plugin of the named class and insert it into the Toolbox,
    * by using the Reflection API.
    * @param name the fully qualified Java name of the plugin class, e.g. "king.BasicPlugin"
    * @return true on success, false on failure
    */
    private boolean addPluginByName(String name)
    {
        Plugin theplugin;
        
        // First, check to see if we already have one.
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            theplugin = (Plugin)iter.next();
            if(theplugin.getClass().getName().equals(name)) return true;
        }
        
        // If not, try to load one dynamically.
        try
        {
            Class[] constargs = { ToolBox.class };
            Object[] initargs = { this };
            
            Class pluginclass = Class.forName(name);
            Constructor pluginconst = pluginclass.getConstructor(constargs);
            theplugin = (Plugin)pluginconst.newInstance(initargs);
            plugins.add(theplugin);
        }
        catch(Throwable t)
        {
            t.printStackTrace(SoftLog.err);
            SoftLog.err.println("While trying to load '"+name+"': "+t.getMessage());
            return false;
        }
        return true;
    }
//}}}

//{{{ addPluginsToToolsMenu
//##################################################################################################
    /** Appends menu items for using the loaded plugins */
    public void addPluginsToToolsMenu(JMenu menu)
    {
        Plugin p;
        JMenuItem item;
        defaultToolMI = activeToolMI = null;
        Map submenus = new HashMap(); // Map<String, JMenu>
        ButtonGroup group = new ButtonGroup();
        // Add things to primary menu and create submenus
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            p = (Plugin)iter.next();
            item = p.getToolsMenuItem();
            if(item != null)
            {
                String menuName = getPluginMenuName(p);
                if(MENU_MAIN.equals(menuName))      menu.add(item);
                else if(MENU_NONE.equals(menuName)) {}  // don't add to any menu
                else // add to the named submenu
                {
                    JMenu submenu = (JMenu) submenus.get(menuName);
                    if(submenu == null)
                    {
                        submenu = new JMenu(menuName);
                        submenus.put(menuName, submenu);
                    }
                    submenu.add(item);
                }
            }
            if(p instanceof BasicTool && item != null)
            {
                group.add(item);
                if(p == defaultTool)    defaultToolMI = item;
                if(p == activeTool)     activeToolMI = item;
            }
        }
        // Sort the submenus alphabetically and add them at the end
        ArrayList submenuList = new ArrayList(submenus.values());
        Collections.sort(submenuList, new MenuComparator());
        for(Iterator iter = submenuList.iterator(); iter.hasNext(); )
        {
            JMenu submenu = (JMenu) iter.next();
            menu.add(submenu);
        }
        // Mark the active tool as such
        if(activeToolMI != null) activeToolMI.setSelected(true);
    }
//}}}

//{{{ addPluginsToHelpMenu
//##################################################################################################
    /** Appends menu items for understanding the loaded plugins */
    public void addPluginsToHelpMenu(JMenu menu)
    {
        Plugin p;
        JMenuItem item;
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            p = (Plugin)iter.next();
            item = p.getHelpMenuItem();
            if(item != null) menu.add(item);
        }
    }
//}}}

//{{{ toolActivated, notifyChange
//##################################################################################################
    /** Called by Tools when their radio button gets hit. */
    public void toolActivated(BasicTool t)
    {
        activeTool.stop();
        //services.clearEverything();
        activeTool = t;
        activeTool.start();
    }
    
    /** Programmatically selects the Navigate tool. */
    public void activateDefaultTool()
    {
        if(defaultToolMI != null)
            defaultToolMI.setSelected(true);
        toolActivated(defaultTool);
    }

    /**
    * Called by KinCanvas when something happens.
    * Shouldn't be called directly under normal circumstances.
    */
    public void notifyChange(int event_mask)
    {
        int reset_tool = KingMain.EM_SWITCH | KingMain.EM_CLOSE | KingMain.EM_CLOSEALL;
        if((event_mask & reset_tool) != 0)
        {
            services.clearEverything();
            activeTool.reset();
        }
    }
//}}}

//{{{ Mouse listeners
//##################################################################################################
    // All of these just 'bounce' the event to the current active tool
    
    public void mouseDragged(MouseEvent ev)
    {
        activeTool.mouseDragged(ev);
    }
    
    public void mouseMoved(MouseEvent ev)
    {
        activeTool.mouseMoved(ev);
    }

    public void mouseClicked(MouseEvent ev)
    {
        activeTool.mouseClicked(ev);
    }

    public void mouseEntered(MouseEvent ev)
    {
        activeTool.mouseEntered(ev);
    }

    public void mouseExited(MouseEvent ev)
    {
        activeTool.mouseExited(ev);
    }
    
    public void mousePressed(MouseEvent ev)
    {
        // required for the keyboard arrows, etc
        // to pick up events!
        kCanvas.requestFocus();
        activeTool.mousePressed(ev);
    }
    
    public void mouseReleased(MouseEvent ev)
    {
        activeTool.mouseReleased(ev);
    }

    /**
    * Not a real listener. Exists for other systems to "fake"
    * mouse wheel events, e.g. through arrow keys.
    * If we're in Java 1.4, real wheel events will be
    * redirected here by ToolBoxMW.
    */
    public void mouseWheelMoved(MouseEvent ev, int rotation)
    {
        activeTool.mouseWheelMoved(ev, rotation);
    }
//}}}

//{{{ onArrowUp/Down/Right/Left
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowUp(ActionEvent ev)
    {
        activeTool.onArrowUp(ev);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowDown(ActionEvent ev)
    {
        activeTool.onArrowDown(ev);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowRight(ActionEvent ev)
    {
        activeTool.onArrowRight(ev);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowLeft(ActionEvent ev)
    {
        activeTool.onArrowLeft(ev);
    }
//}}}
    
//{{{ signalTransform, overpaintCanvas
//##################################################################################################
    /**
    * A call to this method indicates the subscriber
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
    *
    * <p>This method will be called in response to TransformSignal.signalTransform().
    *
    * @param engine     the Engine object describing the
    *   dimensions and properties of the space to be painted.
    * @param xform      the Transform to apply.
    *   The subscriber must not modify the original Transform it
    *   receives! Subscibers may, however, copy and modify the
    *   Transform(s) they pass to internal substructures.
    */
    public void signalTransform(Engine engine, Transform xform)
    {
        // Plugins:
        sigTransform.signalTransform(engine, xform);
        // Markers:
        services.signalTransform(engine, xform);
        // Active tool:
        activeTool.signalTransform(engine, xform);
    }

    /**
    * Called by KinCanvas after all kinemage painting is complete,
    * this gives the tools a chance to write additional info
    * (e.g., point IDs) to the graphics area.
    * @param g2 the Graphics2D of the KinCanvas being painted
    */
    public void overpaintCanvas(Graphics2D g2)
    {
        services.overpaintCanvas(g2);
        activeTool.overpaintCanvas(g2);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class
