// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;

import driftwood.gui.*;
//}}}
/**
 * <code>EmphasisTool</code> is a means of emphasizing
 * one list out of a subgroup.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Mon Oct 14 11:01:37 EDT 2002
*/
public class EmphasisTool extends BasicTool implements ChangeListener, ActionListener
{
//{{{ Constants
    static final String[] HIGHLIGHT_COLORS =
    { "hotpink", "yellow", "green", "cyan", "invisible" };
//}}}

//{{{ Variable definitions
//##################################################################################################
    KSubgroup       subgroup = null;
    KList           list1 = null, list2 = null;
    boolean         wasOn1 = true, wasOn2 = true;
    int             oldWidth1 = 0, oldWidth2 = 0;

    JLabel          lblSubgroup;
    JCheckBox       lblList1, lblList2;
    JSlider         slider1, slider2;
    TablePane       toolpanel;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public EmphasisTool(ToolBox tb)
    {
        super(tb);
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI()
    {
        lblSubgroup = new JLabel();
        lblList1    = new JCheckBox();
        lblList2    = new JCheckBox();
        lblList1.setSelected(true);
        lblList2.setSelected(false);
        // Repaint on select / deselect
        lblList1.addActionListener(this);
        lblList2.addActionListener(this);

        slider1 = new JSlider(JSlider.VERTICAL);
        slider1.addChangeListener(this);
        slider1.setInverted(true);
        slider1.setSnapToTicks(true);
        slider2 = new JSlider(JSlider.VERTICAL);
        slider2.addChangeListener(this);
        slider2.setInverted(true);
        slider2.setSnapToTicks(true);
        
        toolpanel = new TablePane().center();
        toolpanel.add(lblSubgroup, 2, 1);
        toolpanel.newRow(); //--------
        toolpanel.save().hfill(true).startSubtable(2, 1);
        toolpanel.center();
        toolpanel.add(lblList1);
        toolpanel.add(lblList2);
        toolpanel.endSubtable();
        toolpanel.restore();
        toolpanel.newRow(); //--------
        toolpanel.add(slider1);
        toolpanel.add(slider2);
        
        freeSubgroup();
    }
//}}}

//{{{ start/stop/reset functions
//##################################################################################################
    public void stop()
    {
        super.stop();
        freeSubgroup(); // also frees the lists
        //TODO: Signal thru kMain?
        kCanvas.repaint();
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
            grabSubgroup((KSubgroup)p.getOwner().getOwner());
        }
    }
    
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    { super.click(x, y, p, ev); }
//}}}

//{{{ xx_wheel() functions
//##################################################################################################
    /** Override this function for mouse wheel motion */
    public void wheel(int rotation, MouseEvent ev)
    {
        slider1.setValue(slider1.getValue()+rotation);
    }
    /** Override this function for mouse wheel motion with shift down */
    public void s_wheel(int rotation, MouseEvent ev)
    {
        slider2.setValue(slider2.getValue()+rotation);
    }
    /** Override this function for mouse wheel motion with control down */
    public void c_wheel(int rotation, MouseEvent ev)
    { super.wheel(rotation, ev); }
    /** Override this function for mouse wheel motion with shift AND control down */
    public void sc_wheel(int rotation, MouseEvent ev)
    { super.s_wheel(rotation, ev); }
//}}}

//{{{ slider and checkbox listeners
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        if(subgroup == null) return;
        
        // update both sliders whenever one changes, because
        // of the exclusion rule: list1 != list2 even if slider1 == slider2
        int nList1 = slider1.getValue();
        if(nList1 >= 0) grabList1((KList)subgroup.getChildAt(nList1));
        int nList2 = slider2.getValue();
        if(nList2 >= 0) grabList2((KList)subgroup.getChildAt(nList2));

        //TODO: Signal thru kMain?
        kCanvas.repaint();
    }

    public void actionPerformed(ActionEvent ev)
    {
        stateChanged(null);
    }
//}}}

//{{{ grabSubgroup, freeSubgroup
//##################################################################################################
    /** Releases the currently held subgroup, if any */
    void freeSubgroup()
    {
        freeList1();
        freeList2();
        if(subgroup != null)
        {
            // restore saved properties
        }
        subgroup = null;
        
        // zero out the sliders
        slider1.setMaximum(0);
        slider1.setValue(0);
        slider2.setMaximum(0);
        slider2.setValue(0);

        lblSubgroup.setText("(click subgroup to emphasize)");
    }
    
    /** Grabs a subgroup for this tool */
    void grabSubgroup(KSubgroup s)
    {
        freeSubgroup();
        if(s == null) return;
        subgroup = s;
        
        int nLists = subgroup.getChildCount();
        slider1.setMaximum(nLists-1);
        slider1.setValue(0);
        slider2.setMaximum(nLists-1);
        slider2.setValue(0);
        // this should have generated a property change event
        // that will select list(s) to be highlighted

        lblSubgroup.setText(subgroup.getName());
    }
//}}}

//{{{ grabList, freeList
//##################################################################################################
    /** Releases the currently held list, if any */
    void freeList1()
    {
        if(list1 != null)
        {
            list1.setOn(wasOn1);
            list1.width = oldWidth1;
        }
        list1 = null;
        lblList1.setText("-");
    }
    
    /** Grabs a list and highlights it */
    void grabList1(KList l)
    {
        freeList1();
        if(l == null || l == list2 || !lblList1.isSelected()) return;
        list1 = l;
        
        // save properties
        wasOn1      = list1.isOn();
        oldWidth1   = list1.width;
        // set properties
        list1.setOn(true);
        list1.width = 7;
        lblList1.setText(list1.getName());
    }

    /** Releases the currently held list, if any */
    void freeList2()
    {
        if(list2 != null)
        {
            list2.setOn(wasOn2);
            list2.width = oldWidth2;
        }
        list2 = null;
        lblList2.setText("-");
    }
    
    /** Grabs a list and highlights it */
    void grabList2(KList l)
    {
        freeList2();
        if(l == null || l == list1 || !lblList2.isSelected()) return;
        list2 = l;
        
        // save properties
        wasOn2      = list2.isOn();
        oldWidth2   = list2.width;
        // set properties
        list2.setOn(true);
        list2.width = 7;
        lblList2.setText(list2.getName());
    }
//}}}

//{{{ getToolPanel, getHelpURL, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    {
        return toolpanel;
    }
    
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        return getClass().getResource("html/emphasis-tool.html");
    }
    
    public String toString() { return "Emphasize"; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

