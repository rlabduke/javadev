// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;

import driftwood.gui.*;
import driftwood.isosurface.*;
import driftwood.r3.*;
//}}}
/**
* <code>EDMapTool</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar  4 13:14:16 EST 2003
*/
public class EDMapTool extends BasicTool implements PropertyChangeListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    TablePane           toolpane;
    JButton             btnOpenMap;
    
    JFileChooser        filechooser;
    JRadioButton        btnXplorType, btnOType;
    SuffixFileFilter    omapFilter, xmapFilter, mapFilter;
    
    ArrayList           mapWindows;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public EDMapTool(ToolBox tb)
    {
        super(tb);
        
        mapWindows = new ArrayList();
        makeFileFilters();
        makeFileChooser();
        buildGUI();
    }
//}}}

//{{{ makeFileFilters, makeFileChooser
//##################################################################################################
    void makeFileFilters()
    {
        omapFilter = new SuffixFileFilter("O maps (DSN6/Brix)");
        omapFilter.addSuffix(".brix");
        omapFilter.addSuffix(".brix.gz");
        omapFilter.addSuffix(".dsn6");
        omapFilter.addSuffix(".dsn6.gz");
        omapFilter.addSuffix(".dn6");
        omapFilter.addSuffix(".dn6.gz");
        omapFilter.addSuffix(".omap");
        omapFilter.addSuffix(".omap.gz");
        
        xmapFilter = new SuffixFileFilter("XPLOR maps (ASCII format)");
        xmapFilter.addSuffix(".xmap");
        xmapFilter.addSuffix(".xmap.gz");
        xmapFilter.addSuffix(".xplor");
        xmapFilter.addSuffix(".xplor.gz");
        
        mapFilter = new SuffixFileFilter("All electron density maps");
        mapFilter.addSuffix(".xmap");
        mapFilter.addSuffix(".xmap.gz");
        mapFilter.addSuffix(".xplor");
        mapFilter.addSuffix(".xplor.gz");
        mapFilter.addSuffix(".brix");
        mapFilter.addSuffix(".brix.gz");
        mapFilter.addSuffix(".dsn6");
        mapFilter.addSuffix(".dsn6.gz");
        mapFilter.addSuffix(".dn6");
        mapFilter.addSuffix(".dn6.gz");
        mapFilter.addSuffix(".omap");
        mapFilter.addSuffix(".omap.gz");
        mapFilter.addSuffix(".map");
        mapFilter.addSuffix(".map.gz");
    }
    
    void makeFileChooser()
    {
        // Make accessory for file chooser
        TablePane acc = new TablePane();
        acc.weights(0,0);
        acc.add(new JLabel("Map type?"));
        acc.newRow();
        btnOType = new JRadioButton("O");
        acc.add(btnOType);
        acc.newRow();
        btnXplorType = new JRadioButton("XPLOR");
        acc.add(btnXplorType);
        
        // Make buttons mutually exclusive
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(btnOType);
        btnGroup.add(btnXplorType);
        
        // Make actual file chooser -- will throw an exception if we're running as an Applet
        filechooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
        
        filechooser.setAccessory(acc);
        filechooser.addPropertyChangeListener(this);
        filechooser.addChoosableFileFilter(mapFilter);
        filechooser.setFileFilter(mapFilter);
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    void buildGUI()
    {
        btnOpenMap  = new JButton(new ReflectiveAction("Open map...", null, this, "onOpenMap"));

        toolpane = new TablePane();
        toolpane.center().hfill(true);
        toolpane.add(toolpane.strut(0,8));
        toolpane.newRow();
        toolpane.add(btnOpenMap);
    }
//}}}

//{{{ start/stop/reset functions
//##################################################################################################
    public void start()
    {
        super.start();
        onOpenMap(null);
    }

    public void stop()
    {
        super.stop();
        EDMapWindow win;
        Object[] mapwins = mapWindows.toArray();
        // can't use an iterator b/c of concurrent modification exception
        for(int i = 0; i < mapwins.length; i++)
        {
            win = (EDMapWindow)mapwins[i];
            win.onMapDiscard(null);
        }
        
        mapWindows.clear();
    }
    
//}}}

//{{{ propertyChange
//##################################################################################################
    public void propertyChange(PropertyChangeEvent ev)
    {
        if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(ev.getPropertyName()))
        {
            File f = (File)ev.getNewValue();
            if(f == null) {}
            else if(omapFilter.accept(f)) btnOType.setSelected(true);
            else if(xmapFilter.accept(f)) btnXplorType.setSelected(true);
        }
    }
//}}}

//{{{ onOpenMap
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenMap(ActionEvent ev)
    {
        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getMainWindow())
        && kMain.getKinemage() != null)
        {
            try
            {
                File f = filechooser.getSelectedFile();
                if(f != null && f.exists())
                {
                    CrystalVertexSource map;
                    
                    if(btnOType.isSelected())
                    {
                        map = new OMapVertexSource(new FileInputStream(f));
                    }
                    else if(btnXplorType.isSelected())
                    {
                        map = new XplorVertexSource(new FileInputStream(f));
                    }
                    else throw new IllegalArgumentException("Map type not specified");
                    
                    EDMapWindow win = new EDMapWindow(this, map, f.getName());
                    addMapWindow(win);
                }
            }
            catch(IOException ex)
            {
                JOptionPane.showMessageDialog(kMain.getMainWindow(),
                    "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
            catch(IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(kMain.getMainWindow(),
                    "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
//}}}

//{{{ addMapWindow, removeMapWindow
//##################################################################################################
    public void addMapWindow(EDMapWindow win)
    {
        mapWindows.add(win);
        kCanvas.repaint();
    }
    
    public void removeMapWindow(EDMapWindow win)
    {
        mapWindows.remove(win);
        kCanvas.repaint();
    }
//}}}

//{{{ signalTransform
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
        EDMapWindow win;
        for(Iterator iter = mapWindows.iterator(); iter.hasNext(); )
        {
            win = (EDMapWindow)iter.next();
            win.signalTransform(engine, xform);
        }
    }
//}}}
    
//{{{ getToolPanel, getHelpURL, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    public Component getToolPanel()
    { return toolpane; }
    
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        return getClass().getResource("html/edmap-tool.html");
    }
    
    public String toString() { return "ED Map"; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

