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
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>ViewpointPlugin</code> provides +/- 90 degree rotations
* around the three axes.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Nov  4 10:53:16 EST 2003
*/
public class ViewpointPlugin extends Plugin implements MouseMotionListener
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    JDialog dialog;
    JLabel calipX, calipY, calipZ;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ViewpointPlugin(ToolBox tb)
    {
        super(tb);
        buildGUI();
        kCanvas.addMouseMotionListener(this);
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        KingPrefs prefs = kMain.getPrefs();
        ReflectiveAction axp = new ReflectiveAction(null, prefs.rotXpIcon, this, "onRotXP");
        axp.setTooltip("+90 degrees around X axis");
        ReflectiveAction axm = new ReflectiveAction(null, prefs.rotXmIcon, this, "onRotXM");
        axm.setTooltip("-90 degrees around X axis");
        ReflectiveAction ayp = new ReflectiveAction(null, prefs.rotYpIcon, this, "onRotYP");
        ayp.setTooltip("+90 degrees around Y axis");
        ReflectiveAction aym = new ReflectiveAction(null, prefs.rotYmIcon, this, "onRotYM");
        aym.setTooltip("-90 degrees around Y axis");
        ReflectiveAction azp = new ReflectiveAction(null, prefs.rotZpIcon, this, "onRotZP");
        azp.setTooltip("+90 degrees around Z axis");
        ReflectiveAction azm = new ReflectiveAction(null, prefs.rotZmIcon, this, "onRotZM");
        azm.setTooltip("-90 degrees around Z axis");
        
        JButton rxp = new JButton(axp);
        JButton rxm = new JButton(axm);
        JButton ryp = new JButton(ayp);
        JButton rym = new JButton(aym);
        JButton rzp = new JButton(azp);
        JButton rzm = new JButton(azm);
        
        calipX = new JLabel("X:");
        calipY = new JLabel("Y:");
        calipZ = new JLabel("Z:");
        
        ReflectiveAction actionI = new ReflectiveAction("Go to standard viewpoint", null, this, "onCanonicalView");
        actionI.setTooltip("Looking toward center from +Z, with +Y up and +X to the right");
        JButton viewI = new JButton(actionI);
        
        TablePane cp = new TablePane();
        cp.center();
        cp.addCell(ryp).addCell(rym).newRow();
        cp.addCell(rzp).addCell(rzm).newRow();
        cp.addCell(rxp).addCell(rxm).newRow();
        cp.save().hfill(true);
            cp.addCell(calipX,2,1).newRow();
            cp.addCell(calipY,2,1).newRow();
            cp.addCell(calipZ,2,1).newRow();
        cp.restore();
        cp.addCell(viewI, 2, 1);
        
        dialog = new JDialog(kMain.getTopWindow(), this.toString(), false);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setContentPane(cp);
    }
//}}}

//{{{ toString, getToolsMenuItem
//##################################################################################################
    public String toString()
    {
        return "90 deg. rotations";
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
//}}}

//{{{ getHelpAnchor
//##################################################################################################
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this plugin. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#edmap-plugin" (or null)
    */
    public String getHelpAnchor()
    { return "#rot90-plugin"; }
//}}}

//{{{ onRot{X,Y,Z}{P,M}
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRotXP(ActionEvent ev)
    {
        KingView view = kMain.getView();
        if(view == null) return;
        view.rotateX((float)(Math.PI / 2.0));
        kCanvas.repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRotXM(ActionEvent ev)
    {
        KingView view = kMain.getView();
        if(view == null) return;
        view.rotateX((float)(-Math.PI / 2.0));
        kCanvas.repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRotYP(ActionEvent ev)
    {
        KingView view = kMain.getView();
        if(view == null) return;
        view.rotateY((float)(Math.PI / 2.0));
        kCanvas.repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRotYM(ActionEvent ev)
    {
        KingView view = kMain.getView();
        if(view == null) return;
        view.rotateY((float)(-Math.PI / 2.0));
        kCanvas.repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRotZP(ActionEvent ev)
    {
        KingView view = kMain.getView();
        if(view == null) return;
        view.rotateZ((float)(Math.PI / 2.0));
        kCanvas.repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRotZM(ActionEvent ev)
    {
        KingView view = kMain.getView();
        if(view == null) return;
        view.rotateZ((float)(-Math.PI / 2.0));
        kCanvas.repaint();
    }
//}}}

//{{{ onCanonicalView
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCanonicalView(ActionEvent ev)
    {
        KingView view = kMain.getView();
        if(view == null) return;
        float[][] xform  = { {1f, 0f, 0f}, {0f, 1f, 0f}, {0f, 0f, 1f} };
        view.setMatrix(xform);
        kCanvas.repaint();
    }
//}}}

//{{{ Mouse motion listeners
//##################################################################################################
    public void mouseDragged(MouseEvent ev)
    { mouseMoved(ev); }
    public void mouseMoved(MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(dialog.isVisible() && v != null)
        {
            Dimension dim = kCanvas.getSize();
            float[] center = v.getCenter();
            float[] offset = v.translateRotated(ev.getX() - dim.width/2, dim.height/2 - ev.getY(), 0, Math.min(dim.width, dim.height));
            calipX.setText("X: "+df.format(center[0]+offset[0]));
            calipY.setText("Y: "+df.format(center[1]+offset[1]));
            calipZ.setText("Z: "+df.format(center[2]+offset[2]));
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

