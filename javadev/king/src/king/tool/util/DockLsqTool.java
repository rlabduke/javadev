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
* <code>DockLsqTool</code> is a simple tool for doing
* least-squares docking.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jul 11 13:32:28 EDT 2003
*/
public class DockLsqTool extends BasicTool
{
//{{{ Constants
//}}}

//{{{ CLASS: PointKeeper
//##############################################################################
    class PointKeeper implements ActionListener
    {
        public JList        pointList;
        public ArrayList    tupleList;
        public JButton      btnClear, btnRemove;
        public KList        markList;
        
        DefaultListModel    listModel;
        
        public PointKeeper(KPaint paint)
        {
            tupleList = new ArrayList();
            listModel = new DefaultListModel();
            pointList = new JList(listModel);
            pointList.setVisibleRowCount(3);
            btnClear = new JButton("Clear");
            btnClear.addActionListener(this);
            btnRemove = new JButton("Remove last");
            btnRemove.addActionListener(this);
            markList = new KList();
            markList.setColor(paint);
        }
        
        public int count()
        { return tupleList.size(); }
        
        public void add(String tag, Tuple3 t)
        {
            tupleList.add(t);
            listModel.addElement(tag);
            LabelPoint label = new LabelPoint(markList, Integer.toString(count()));
            label.setOrigX(t.getX());
            label.setOrigY(t.getY());
            label.setOrigZ(t.getZ());
            label.setUnpickable(true);
            markList.add(label);
            syncDockButton();
        }
        
        public void removeLast()
        {
            if(tupleList.size() > 0) tupleList.remove(tupleList.size()-1);
            if(listModel.size() > 0) listModel.remove(listModel.size()-1);
            if(markList.children.size() > 0) markList.children.remove(markList.children.size()-1);
            syncDockButton();
        }
        
        public void actionPerformed(ActionEvent ev)
        {
            if(ev.getSource() == btnClear) clear();
            else if(ev.getSource() == btnRemove) removeLast();
            
            kCanvas.repaint();
        }
        
        public void clear()
        {
            tupleList.clear();
            listModel.clear();
            markList.clear();
            syncDockButton();
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    TablePane       toolpane;
    PointKeeper     pkReference;
    PointKeeper     pkMobile;
    JRadioButton    btnReference, btnMobile;
    JButton         btnDock;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DockLsqTool(ToolBox tb)
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
        
        btnReference    = new JRadioButton("Reference", true);
        btnMobile       = new JRadioButton("Mobile", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(btnReference);
        bg.add(btnMobile);
        
        toolpane = new TablePane();
        toolpane.center();
        toolpane.add(btnReference);
        toolpane.add(pkReference.btnClear);
        toolpane.add(pkReference.btnRemove);
        toolpane.newRow().save().hfill(true).vfill(true);
        toolpane.add(new JScrollPane(pkReference.pointList),3,1);
        toolpane.newRow().restore();
        toolpane.add(btnMobile);
        toolpane.add(pkMobile.btnClear);
        toolpane.add(pkMobile.btnRemove);
        toolpane.newRow().save().hfill(true).vfill(true);
        toolpane.add(new JScrollPane(pkMobile.pointList),3,1);
        toolpane.newRow().restore();
        toolpane.add(btnDock,3,1);
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
            if(btnReference.isSelected())
            {
                pkReference.add(p.getName(), t);
            }
            else if(btnMobile.isSelected()) 
            {
                pkMobile.add(p.getName(), t);
            }
            else
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Either 'Reference' or 'Mobile' should be selected.",
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
        Tuple3[] ref = (Tuple3[])pkReference.tupleList.toArray(new Tuple3[pkReference.tupleList.size()]);
        Tuple3[] mob = (Tuple3[])pkMobile.tupleList.toArray(new Tuple3[pkMobile.tupleList.size()]);
        double[] w = new double[ref.length];
        Arrays.fill(w, 1.0);
        
        SuperPoser poser = new SuperPoser(ref, mob);
        Transform t = poser.superpos(w);
        
        Kinemage kin = kMain.getKinemage();
        if(!t.isNaN() && kin != null)
        {
            transformAllVisible(kin, t);
            kin.setModified(true);
        }
        
        // Swap which button is selected
        if(btnReference.isSelected())   btnMobile.setSelected(true);
        else                            btnReference.setSelected(true);
        
        pkReference.clear();
        pkMobile.clear();
        kCanvas.repaint();
    }
    
    void syncDockButton()
    {
        btnDock.setEnabled(pkReference.count() >= 2 && pkReference.count() == pkMobile.count());
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
    { return "#lsqdock-tool"; }
    
    public String toString() { return "Least-squares docking"; }
//}}}
}//class

