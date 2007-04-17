// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.draw;
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
//}}}
/**
* <code>PointColorPlugin</code> allows one to turn on/off points and group
* them by color.  This is the equivalent of Mage's selection tools, when used
* in conjunction with the Draw Tool's "paint points" mode.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Apr 15 11:18:14 EDT 2007
*/
public class PointColorPlugin extends Plugin
{
//{{{ Constants
    static final String     DO_IF       = "if";
    static final String     DO_IF_NOT   = "if not";
    static final String[]   CONDITIONS  = {DO_IF, DO_IF_NOT};
//}}}

//{{{ Variable definitions
//##############################################################################
    JDialog dialog;
    JList   conditions, colors;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PointColorPlugin(ToolBox tb)
    {
        super(tb);
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        conditions = new FatJList(0, 10);
        conditions.setListData(CONDITIONS);
        conditions.setSelectedValue(DO_IF, true);
        
        colors = new FatJList(0, 10);
        colors.setListData(KPalette.getStandardMap().values().toArray());
        colors.setSelectedValue(KPalette.green, true);
        
        JButton turnOn = new JButton(new ReflectiveAction("Turn on", null, this, "onTurnOn"));
        JButton turnOff = new JButton(new ReflectiveAction("Turn off", null, this, "onTurnOff"));
        JButton toggle = new JButton(new ReflectiveAction("Toggle", null, this, "onToggle"));
        JButton extract = new JButton(new ReflectiveAction("Extract", null, this, "onExtract"));
        JButton bleach = new JButton(new ReflectiveAction("Bleach visible", null, this, "onBleachVisible"));
        
        TablePane2 cp = new TablePane2();
        cp.hfill(true).addCell(new JScrollPane(conditions));
        cp.hfill(true).addCell(new JScrollPane(colors));
        cp.newRow();
        cp.startSubtable(2, 1).hfill(true).memorize();
            cp.addCell(turnOn).addCell(turnOff);
            cp.newRow();
            cp.addCell(toggle).addCell(extract);
            cp.newRow();
            cp.addCell(cp.strut(0,8), 2, 1);
            cp.newRow();
            cp.addCell(bleach, 2, 1);
        cp.endSubtable();
        
        dialog = new JDialog(kMain.getTopWindow(), this.toString(), false);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setContentPane(cp);
    }
//}}}

//{{{ getPointColor, checkForSelectGroups
//##############################################################################
    /** Can't use p.getDrawingColor() b/c that return invisible when off */
    private KPaint getPointColor(KPoint p)
    {
        KPaint color = p.getColor();
        if(color != null) return color;
        KList list = p.getParent();
        if(list != null)
        {
            color = list.getColor();
            if(color != null) return color;
        }
        return KPalette.defaultColor;
    }

    private boolean checkForSelectGroups()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return false;
        boolean hasSelectGroups = false;
        for(KGroup group : kin) hasSelectGroups |= group.isSelect();
        if(!hasSelectGroups) JOptionPane.showMessageDialog(kMain.getTopWindow(),
            "Please enable the 'select' property on one or more groups.",
            "No selectable groups", JOptionPane.ERROR_MESSAGE);
        return hasSelectGroups;
    }
    
    private boolean isSelectable(AHE ahe)
    {
        if(ahe instanceof KGroup && ((KGroup)ahe).isSelect())
            return true;
        else if(ahe.getParent() != null)
            return isSelectable(ahe.getParent());
        else
            return false;
    }
//}}}

//{{{ onTurnOn/TurnOff/Toggle
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTurnOn(ActionEvent ev)
    { doOnOff(true); }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTurnOff(ActionEvent ev)
    { doOnOff(false); }
    
    private void doOnOff(boolean state)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        if(!checkForSelectGroups()) return;
        KPaint testColor = (KPaint) colors.getSelectedValue();
        if(testColor == null) return; // never happens?
        boolean ifFlag = (conditions.getSelectedValue() == DO_IF_NOT ? true : false);
        for(KPoint p : KIterator.allPoints(kin))
            if(isSelectable(p) && (ifFlag ^ testColor.equals(getPointColor(p))))
                p.setOn(state);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onToggle(ActionEvent ev)
    {
        // This doesn't work in parallel coordinates mode
        // if there's an even number of dimensions -- cancels out.
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        if(!checkForSelectGroups()) return;
        KPaint testColor = (KPaint) colors.getSelectedValue();
        if(testColor == null) return; // never happens?
        boolean ifFlag = (conditions.getSelectedValue() == DO_IF_NOT ? true : false);
        for(KPoint p : KIterator.allPoints(kin))
            if(isSelectable(p) && (ifFlag ^ testColor.equals(getPointColor(p))))
                p.setOn(!p.isOn());
    }
//}}}

//{{{ onExtract
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExtract(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        if(!checkForSelectGroups()) return;
        KPaint testColor = (KPaint) colors.getSelectedValue();
        if(testColor == null) return; // never happens?
        
        String name = JOptionPane.showInputDialog(kMain.getTopWindow(),
            "Name for new list?", "Name list", JOptionPane.QUESTION_MESSAGE);
        if(name == null) return; // user canceled dialog
        
        KList list = null;
        boolean ifFlag = (conditions.getSelectedValue() == DO_IF_NOT ? true : false);
        for(KIterator<KPoint> iter = KIterator.allPoints(kin); iter.hasNext(); )
        {
            KPoint p = iter.next();
            if(isSelectable(p) && (ifFlag ^ testColor.equals(getPointColor(p))))
            {
                if(list == null)
                {
                    list = p.getParent().clone(false);
                    list.setInstance(null); // shallow clone uses instance=
                    list.setName(name);
                }
                //p.getParent().remove(p); // causes ConcurrentModificationEx
                iter.remove();
                list.add(p);
            }
        }
        
        // Trying to do this at list create time also causes CoModEx
        if(list != null)
        {
            KGroup group = new KGroup(name);
            group.setDominant(true);
            KGroup subgroup = new KGroup(name);
            subgroup.setDominant(true);
            kin.add(group);
            group.add(subgroup);
            subgroup.add(list);
        }
    }
//}}}

//{{{ onBleachVisible
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onBleachVisible(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        for(KPoint p : KIterator.visiblePoints(kin))
            if(isSelectable(p))
                p.setColor(null);
    }
//}}}

//{{{ getToolsMenuItem, getHelpAnchor, toString, onShowDialog
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onShowDialog"));
    }
    
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        /*URL     url     = getClass().getResource("/extratools/tools-manual.html");
        String  anchor  = getHelpAnchor();
        if(url != null && anchor != null)
        {
            try { url = new URL(url, anchor); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
            return url;
        }
        else*/ return null;
    }
    
    public String getHelpAnchor()
    { return "#point-color-plugin"; }

    public String toString()
    { return "Select by color"; }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowDialog(ActionEvent ev)
    {
        dialog.pack();
        dialog.setVisible(true);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

