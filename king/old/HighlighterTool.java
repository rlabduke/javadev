// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
//}}}
/**
 * <code>HighlighterTool</code> has not yet been documented.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Mon Oct 14 11:01:37 EDT 2002
*/
public class HighlighterTool extends MMTool implements ActionListener, ChangeListener
{
//{{{ Constants
    static final String[] HIGHLIGHT_COLORS =
    { "hotpink", "yellow", "green", "cyan", "invisible" };
//}}}

//{{{ Variable definitions
//##################################################################################################
    JLabel          label0, label1, label2;
    JComboBox       box1, box2;
    JSlider         slider1, slider2;
    GridBagPanel    toolpanel;
    
    KSubgroup       subgroup;
    KList           list1, list2;
    int             list1_oldWidth = 0, list2_oldWidth = 0;
    float           list1_oldRadius = 0, list2_oldRadius = 0;
    int             list1_oldColor = 0, list2_oldColor = 0;
    boolean         list1_oldOn = true, list2_oldOn = true;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public HighlighterTool(ToolBox tb)
    {
        super(tb);
        setToolName("Highlighter");
        
        label0  = new JLabel("---");
        label1  = new JLabel("---");
        label2  = new JLabel("---");
        box1    = new JComboBox(HIGHLIGHT_COLORS);
        box1.setSelectedItem("yellow");
        box2    = new JComboBox(HIGHLIGHT_COLORS);
        box2.setSelectedItem("invisible");
        slider1 = new JSlider(JSlider.VERTICAL);
        slider1.addChangeListener(this);
        slider2 = new JSlider(JSlider.VERTICAL);
        slider2.addChangeListener(this);
        
        toolpanel = new GridBagPanel();
        toolpanel.gbc.weightx = 1.0;
        toolpanel.gbc.weighty = 0.0;
        toolpanel.add(label0, 0, 0, 2, 1);
        toolpanel.add(label1, 0, 1);
        toolpanel.add(label2, 1, 1);
        toolpanel.add(box1, 0, 2);
        toolpanel.add(box2, 1, 2);
        toolpanel.gbc.fill = GridBagConstraints.VERTICAL;
        toolpanel.gbc.weighty = 1.0;
        toolpanel.add(slider1, 0, 3);
        toolpanel.add(slider2, 1, 3);
        toolpanel.gbc.fill = GridBagConstraints.HORIZONTAL;
        toolpanel.gbc.weighty = 0.0;
        toolpanel.add(doMarkers, 0, 10, 2, 1);
        toolpanel.add(doXYZ, 0, 11, 2, 1);
        toolpanel.add(doMeasureAll, 0, 12, 2, 1);

        setSubgroup(null);
        setList1(null);
        setList2(null);
    }
//}}}

//{{{ click() functions    
//##################################################################################################
    /** Selects a new subgroup whose lists should be highlighted. */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.c_click(x, y, p, ev);
        
        if(p == null) return;
        setSubgroup((KSubgroup)p.getOwner().getOwner());
    }
//}}}

//{{{ Transform functions
//##################################################################################################
    /**
    * Wipes out any previous transforms, preparing for a fresh start.
    * Propagated down to individual points.
    */
    public void preTransform()
    {
        super.preTransform();
        
        ColorManager colorman = parent.kMain.getKinemage().getColorManager();
        if(list1 != null) list1.setColor( colorman.getIndex((String)box1.getSelectedItem()) );
        if(list2 != null) list2.setColor( colorman.getIndex((String)box2.getSelectedItem()) );
    }
//}}}

//{{{ slider listeners
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        if(ev.getSource() == slider1)
        {
            int nList1 = slider1.getValue();
            if(nList1 >= 0) setList1((KList)subgroup.getChildAt(nList1));
            else setList1(null);
        }
        else if(ev.getSource() == slider2)
        {
            int nList2 = slider2.getValue();
            if(nList2 >= 0) setList2((KList)subgroup.getChildAt(nList2));
            else setList2(null);
        }
        parent.kCanvas.repaint();
    }
//}}}

//{{{ combo box listeners
//##################################################################################################
    public void actionPerformed(ActionEvent ev)
    {
        //ColorManager colorman = parent.kMain.getKinemage().getColorManager();
        //if(ev.getSource() == box1 && list1 != null) list1.setColor( colorman.getIndex((String)box1.getSelectedItem()) );
        //else if(ev.getSource() == box2 && list2 != null) list2.setColor( colorman.getIndex((String)box2.getSelectedItem()) );
        parent.kCanvas.repaint();
    }
//}}}

//{{{ getToolPanel()
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    public Component getToolPanel()
    { return toolpanel; }
//}}}

//{{{ setList1/2()
//##################################################################################################
    void setList1(KList l)
    {
        //ColorManager colorman = parent.kMain.getKinemage().getColorManager();

        // Restore old settings!
        if(list1 != null)
        {
            list1.width = list1_oldWidth;
            list1.radius = list1_oldRadius;
            list1.setColor(list1_oldColor);
            list1.setOn(list1_oldOn);
            list1 = null;
        }
        
        list1 = l;
        if(list1 != null)
        {
            // Save old settings
            list1_oldWidth = list1.width;
            list1_oldRadius = list1.radius;
            list1_oldColor = list1.getColor();
            list1_oldOn = list1.isOn();
            
            // Set temporary values
            list1.width = Math.min(7, list1.width+4);
            list1.radius *= 1.1f;
            //list1.setColor( colorman.getIndex((String)box1.getSelectedItem()) );
            list1.setOn(true);
            
            label1.setText(list1.getName());
        }
        else label1.setText("---");
    }

    void setList2(KList l)
    {
        //ColorManager colorman = parent.kMain.getKinemage().getColorManager();

        // Restore old settings!
        if(list2 != null)
        {
            list2.width = list2_oldWidth;
            list2.radius = list2_oldRadius;
            list2.setColor(list2_oldColor);
            list2.setOn(list2_oldOn);
            list2 = null;
        }
        
        list2 = l;
        if(list2 != null)
        {
            // Save old settings
            list2_oldWidth = list2.width;
            list2_oldRadius = list2.radius;
            list2_oldColor = list2.getColor();
            list2_oldOn = list2.isOn();
            
            // Set temporary values
            list2.width = Math.min(7, list2.width+4);
            list2.radius *= 1.1f;
            //list2.setColor( colorman.getIndex((String)box2.getSelectedItem()) );
            list2.setOn(true);
            
            label2.setText(list2.getName());
        }
        else label2.setText("---");
    }
//}}}

//{{{ setSubgroup()
//##################################################################################################
    void setSubgroup(KSubgroup sg)
    {
        subgroup = sg;
        
        if(subgroup == null)
        {
            label0.setText("---");
            setList1(null);
            setList2(null);
            return;
        }
        
        int nLists = subgroup.getChildCount();
        slider1.setMaximum(nLists-1);
        slider1.setValue(0);
        slider2.setMaximum(nLists-1);
        slider2.setValue(0);
        
        label0.setText(subgroup.getName());
        
        // set up lists to be highlighted
        int nList1 = slider1.getValue();
        if(nList1 >= 0) setList1((KList)subgroup.getChildAt(nList1));
        else setList1(null);
        
        int nList2 = slider2.getValue();
        if(nList2 >= 0) setList2((KList)subgroup.getChildAt(nList2));
        else setList2(null);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

