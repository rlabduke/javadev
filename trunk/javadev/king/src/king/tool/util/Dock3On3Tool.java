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
import driftwood.r3.*;
//}}}
/**
* <code>Dock3On3Tool</code> is a simple tool for doing
* 3-point docking, like in Mage.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jul 11 13:32:28 EDT 2003
*/
public class Dock3On3Tool extends BasicTool
{
//{{{ Constants
//}}}

//{{{ CLASS: PointKeeper
//##############################################################################
    class PointKeeper implements ActionListener
    {
        public JList        pointList;
        public JButton      btnClear;
        public KList        markList;
        
        DefaultListModel    listModel;
        Tuple3              point1, point2, point3;
        
        public PointKeeper(KPaint paint)
        {
            listModel = new DefaultListModel();
            pointList = new JList(listModel);
            pointList.setVisibleRowCount(3);
            btnClear = new JButton("Clear");
            btnClear.addActionListener(this);
            markList = new KList();
            markList.setColor(paint);
            point1 = point2 = point3 = null;
        }
        
        public boolean canAdd()
        { return point3 == null; }
        
        public void add(String tag, Tuple3 t)
        {
            LabelPoint label;
            if(point1 == null)      { point1 = t; label = new LabelPoint(markList, "1"); }
            else if(point2 == null) { point2 = t; label = new LabelPoint(markList, "2"); }
            else if(point3 == null) { point3 = t; label = new LabelPoint(markList, "3"); }
            else throw new IllegalStateException("This keeper is already full!");
            
            listModel.addElement(tag);
            label.setOrigX(t.getX());
            label.setOrigY(t.getY());
            label.setOrigZ(t.getZ());
            label.setUnpickable(true);
            markList.add(label);
            syncDockButton();
        }
        
        public void actionPerformed(ActionEvent ev)
        { clear(); }
        
        public void clear()
        {
            listModel.clear();
            markList.clear();
            point1 = point2 = point3 = null;
            syncDockButton();
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    TablePane       toolpane;
    PointKeeper     pkReference;
    PointKeeper     pkMobile;
    JButton         btnDock;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Dock3On3Tool(ToolBox tb)
    {
        super(tb);
        pkReference = new PointKeeper(KPalette.sky);
        pkMobile    = new PointKeeper(KPalette.hotpink);
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        btnDock = new JButton(new ReflectiveAction(
            "Dock visible on invisible", null, this, "onDock"));
        btnDock.setEnabled(false);
        
        toolpane = new TablePane();
        toolpane.center();
        toolpane.add(new JLabel("Reference"));
        toolpane.add(pkReference.btnClear);
        toolpane.newRow().save().hfill(true).vfill(true);
        toolpane.add(new JScrollPane(pkReference.pointList),2,1);
        toolpane.newRow().restore();
        toolpane.add(new JLabel("Mobile"));
        toolpane.add(pkMobile.btnClear);
        toolpane.newRow().save().hfill(true).vfill(true);
        toolpane.add(new JScrollPane(pkMobile.pointList),2,1);
        toolpane.newRow().restore();
        toolpane.add(btnDock,2,1);
    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
        if(p != null)
        {
            Triple t = new Triple(p.getOrigX(), p.getOrigY(), p.getOrigZ());
            if(pkReference.canAdd())
            {
                pkReference.add(p.getName(), t);
            }
            else if(pkMobile.canAdd()) 
            {
                pkMobile.add(p.getName(), t);
            }
            else
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "You only need 6 points total for docking.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    { super.click(x, y, p, ev); }
//}}}

//{{{ onDock, syncDockButton
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDock(ActionEvent ev)
    {
        Builder builder = new Builder();
        Transform t = builder.dock3on3(
            pkReference.point1, pkReference.point2, pkReference.point3,
            pkMobile.point1, pkMobile.point2, pkMobile.point3);
        
        Kinemage kin = kMain.getKinemage();
        if(!t.isNaN() && kin != null)
        {
            transformAllVisible(kin, t);
            kin.setModified(true);
        }
        
        pkReference.clear();
        pkMobile.clear();
        kCanvas.repaint();
    }
    
    void syncDockButton()
    {
        btnDock.setEnabled(!(pkReference.canAdd() || pkMobile.canAdd()));
    }
//}}}

//{{{ transformAllVisible
//##############################################################################
    private void transformAllVisible(AGE target, Transform t)
    {
        if(!target.isOn()) return;
        
        if(target instanceof KList)
        {
            Triple proxy = new Triple();
            for(Iterator iter = target.iterator(); iter.hasNext(); )
            {
                KPoint pt = (KPoint)iter.next();
                if(pt.isOn())
                {
                    proxy.setXYZ(pt.getOrigX(), pt.getOrigY(), pt.getOrigZ());
                    t.transform(proxy);
                    pt.setOrigX(proxy.getX());
                    pt.setOrigY(proxy.getY());
                    pt.setOrigZ(proxy.getZ());
                }
            }
        }
        else
        {
            for(Iterator iter = target.iterator(); iter.hasNext(); )
                transformAllVisible((AGE)iter.next(), t);
        }
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
        pkReference.markList.signalTransform(engine, xform);
        pkMobile.markList.signalTransform(engine, xform);
    }
//}}}
    
//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ getToolPanel, getHelpURL/Anchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return toolpane; }
    
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this tool. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#navigate-tool" (or null)
    */
    public String getHelpAnchor()
    { return "#dock3-tool"; }
    
    public String toString() { return "Dock 3-on-3"; }
//}}}
}//class

