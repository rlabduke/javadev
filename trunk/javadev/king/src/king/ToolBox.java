// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
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
 * <code>ToolBox</code> instantiates and coordinates all the tools and plugins.
 *
 * <p>Begun on Fri Jun 21 09:30:40 EDT 2002
 * <br>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
*/
public class ToolBox implements MouseListener, MouseMotionListener, TransformSignalSubscriber
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    // These are public so tools in any package can access them.
    public KingMain     kMain;
    public KinCanvas    kCanvas;
    public ToolServices services;
    
    public TransformSignal      sigTransform;
    
    ArrayList   tools;
    BasicTool   activeTool;
    final BasicTool                 defaultTool;
          JRadioButtonMenuItem      defaultToolMI   = null;

    ArrayList   plugins;
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
        
        // Create plugins first because some tools rely on them
        plugins = new ArrayList();
        if(kMain.getApplet() == null)
        {
            addPluginByName("king.tool.model.ModelManager2");
        }
        addPluginByName("king.EDMapPlugin");
        addPluginByName("king.tool.util.ViewpointPlugin");
        addPluginByName("king.tool.draw.SolidObjPlugin");
        
        // Create tools
        tools = new ArrayList();
        defaultTool = activeTool = new BasicTool(this);
        tools.add(activeTool);
        addToolByName("king.tool.util.MovePointTool");
        addToolByName("king.tool.util.EditPropertiesTool");
        addToolByName("king.tool.draw.DrawingTool");
        addToolByName("king.tool.util.Dock3On3Tool");
        addToolByName("king.tool.util.DockLsqTool");
        if(kMain.getApplet() == null)
        {
            addToolByName("king.tool.model.HingeTool");
            addToolByName("king.tool.model.ScRotTool");
            addToolByName("king.tool.model.ScMutTool");
            addToolByName("king.tool.xtal.RNAMapTool");
            addToolByName("king.tool.util.CrossWindowPickTool");
        }
        
        activeTool.start();
    }
//}}}

//{{{ addToolByName
//##################################################################################################
    /**
    * Tries to instantiate a tool of the named class and insert it into the Toolbox,
    * by using the Reflection API.
    * @param name the fully qualified Java name of the tool class, e.g. "king.BasicTool"
    * @return true on success, false on failure
    */
    private boolean addToolByName(String name)
    {
        BasicTool thetool;
        
        // First, check to see if we already have one.
        for(Iterator iter = tools.iterator(); iter.hasNext(); )
        {
            thetool = (BasicTool)iter.next();
            if(thetool.getClass().getName().equals(name)) return true;
        }
        
        // If not, try to load one dynamically.
        try
        {
            Class[] constargs = { ToolBox.class };
            Object[] initargs = { this };
            
            Class toolclass = Class.forName(name);
            Constructor toolconst = toolclass.getConstructor(constargs);
            thetool = (BasicTool)toolconst.newInstance(initargs);
            tools.add(thetool);
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

//{{{ addToolsTo{Tools, Help}Menu
//##################################################################################################
    /** Appends menu items for using the loaded tools */
    public void addToolsToToolsMenu(JMenu menu)
    {
        BasicTool t;
        JRadioButtonMenuItem item, first = null;
        ButtonGroup group = new ButtonGroup();
        for(Iterator iter = tools.iterator(); iter.hasNext(); )
        {
            t = (BasicTool)iter.next();
            item = t.getToolsRBMI();
            if(item != null)
            {
                menu.add(item);
                group.add(item);
                if(first == null) first = item;
            }
        }
        first.setSelected(true);
        defaultToolMI = first;
    }
    
    /** Appends menu items for understanding the loaded tools */
    public void addToolsToHelpMenu(JMenu menu)
    {
        BasicTool t;
        JMenuItem item;
        for(Iterator iter = tools.iterator(); iter.hasNext(); )
        {
            t = (BasicTool)iter.next();
            item = t.getHelpMenuItem();
            if(item != null) menu.add(item);
        }
    }
//}}}

//{{{ addPluginByName, getPluginList
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
    
    /** Returns an unmodifiable List of all installed plugins */
    public java.util.List getPluginList()
    { return Collections.unmodifiableList(plugins); }
//}}}

//{{{ addPluginsTo{Tools, Help}Menu
//##################################################################################################
    /** Appends menu items for using the loaded plugins */
    public void addPluginsToToolsMenu(JMenu menu)
    {
        Plugin p;
        JMenuItem item;
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            p = (Plugin)iter.next();
            item = p.getToolsMenuItem();
            if(item != null) menu.add(item);
        }
    }
    
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
