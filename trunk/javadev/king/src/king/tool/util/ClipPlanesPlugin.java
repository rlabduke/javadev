// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;
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
import javax.swing.event.*;
import driftwood.gui.*;
//}}}
/**
* <code>ClipPlanesPlugin</code> provides additional, configurable clipping for
* complicated kinemages.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Dec 14 11:20:34 EST 2004
*/
public class ClipPlanesPlugin extends Plugin implements ChangeListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JSlider clip1, clip2;
    JCheckBox enable1, enable2;
    JFrame dialog;
    final Object clipKey1 = new Object(), clipKey2 = new Object();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ClipPlanesPlugin(ToolBox tb)
    {
        super(tb);
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        clip1 = new JSlider(0, 800, 200);
        clip1.addChangeListener(this);
        clip2 = new JSlider(0, 800, 200);
        clip2.addChangeListener(this);
        
        enable1 = new JCheckBox(new ReflectiveAction("Set for vis. lists", null, this, "onSetVis1"));
        enable2 = new JCheckBox(new ReflectiveAction("Set for vis. lists", null, this, "onSetVis2"));
        
        TablePane2 cp = new TablePane2();
        cp.weights(0,1).addCell(enable1);
        cp.hfill(true).addCell(clip1).newRow();
        cp.weights(0,1).addCell(enable2);
        cp.hfill(true).addCell(clip2).newRow();
        
        dialog = new JFrame(this.toString());
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setContentPane(cp);
    }
//}}}

//{{{ toString, getToolsMenuItem, onShowDialog getHelpAnchor
//##################################################################################################
    public String toString()
    {
        return "Clipping planes";
    }
    
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
    public JMenuItem getToolsMenuItem()
    {
        JMenuItem item = new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onShowDialog"));
        return item;
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowDialog(ActionEvent ev)
    {
        dialog.pack();
        dialog.setVisible(true);
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

//{{{ stateChanged
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        Object src = ev.getSource();
        Engine engine = kCanvas.getEngine();
        
        if(src == clip1)
        {
            double val = clip1.getValue() / 200.0;
            engine.putClipMode(clipKey1, val, -val);
        }
        else if(src == clip2)
        {
            double val = clip2.getValue() / 200.0;
            engine.putClipMode(clipKey2, val, -val);
        }
        else System.err.println("Unknown event source: "+src);
        
        kCanvas.repaint();
    }
//}}}

//{{{ onSetVis1/2, setVis
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSetVis1(ActionEvent ev)
    { setVis(clipKey1, enable1.isSelected()); }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSetVis2(ActionEvent ev)
    { setVis(clipKey2, enable2.isSelected()); }
    
    void setVis(Object clipKey, boolean isChecked)
    {
        Kinemage k = kMain.getKinemage();
        if(k == null) return;
        
        for(Iterator gi = k.iterator(); gi.hasNext(); )
        {
            for(Iterator si = ((KGroup)gi.next()).iterator(); si.hasNext(); )
            {
                for(Iterator li = ((KSubgroup)si.next()).iterator(); li.hasNext(); )
                {
                    KList list = (KList) li.next();
                    if(isChecked && list.isTotallyOn())                     list.setClipMode(clipKey);
                    else if(!isChecked && list.getClipMode() == clipKey)    list.setClipMode(null);
                }
            }
        }
        
        kCanvas.repaint();
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

