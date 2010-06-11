// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.draw;
import king.*;
import king.core.*;
import king.points.*;
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
* <code>MultiListEditorPlugin</code> allows one to edit multiple lists at once.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Mar  8 2010
*/
public class MultiListEditorPlugin extends Plugin
{
//{{{ Constants
    float RADIUS_MIN = 0.01f;
    float RADIUS_MAX = Float.POSITIVE_INFINITY;
    int   WIDTH_MIN = 1;
    int   WIDTH_MAX = 7;
    int   ALPHA_MIN = 0;
    int   ALPHA_MAX = 255;
//}}}

//{{{ Variable definitions
//##############################################################################
    JDialog dialog;
    JCheckBox cbVec, cbBall, cbDot, cbRib, cbProbe, cbOther, cbVis;
    JComboBox colors;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MultiListEditorPlugin(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        JLabel what = new JLabel("Change all:");
        
        cbVec   = new JCheckBox("vectorlists", true);
        cbBall  = new JCheckBox("balllists", false);
        cbDot   = new JCheckBox("dotlists", false);
        cbRib   = new JCheckBox("ribbonlists", false);
        
        cbProbe = new JCheckBox("Probe contacts", false);
        cbOther = new JCheckBox(new ReflectiveAction("human history", null, this, "onChangeHistory"));
        
        JButton bigger        = new JButton(new ReflectiveAction("BIGGER", null, this, "onBigger"));
        JButton smaller       = new JButton(new ReflectiveAction("smaller", null, this, "onSmaller"));
        JButton opaquer       = new JButton(new ReflectiveAction("opaquer", null, this, "onOpaquer"));
        JButton transparenter = new JButton(new ReflectiveAction("transparenter", null, this, "onTransparenter"));
        JButton master        = new JButton(new ReflectiveAction("add master", null, this, "onAddMaster"));
        JButton color         = new JButton(new ReflectiveAction("set color", null, this, "onSetColor"));
        colors = new JComboBox(KPalette.getStandardMap().values().toArray());
        
        cbVis   = new JCheckBox("only visible", true);
        
        TablePane2 cp = new TablePane2();
        cp.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        cp.addCell(what).newRow();
        cp.addCell(cbVec).addCell(cbProbe).newRow();
        cp.addCell(cbBall).addCell(cbOther).newRow();
        cp.addCell(cbDot).newRow();
        cp.addCell(cbRib).newRow();
        cp.addCell(bigger).addCell(smaller).newRow();
        cp.addCell(opaquer).addCell(transparenter).newRow();
        cp.addCell(color).addCell(colors).newRow();
        cp.addCell(master).newRow();
        cp.addCell(cbVis);
        
        dialog = new JDialog(kMain.getTopWindow(), this.toString(), false);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setContentPane(cp);
        
        // key bindings: just type the key to execute
        InputMap im = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0                       ), "close");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,      KingMain.MENU_ACCEL_MASK), "close");
        ActionMap am = dialog.getRootPane().getActionMap();
        am.put("close", new ReflectiveAction(null, null, this, "onClose"));
    }
//}}}

//{{{ onBigger, onSmaller
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onBigger(ActionEvent ev)
    {
        for(KList l : getLists())
        {
            String t = l.getType();
            if(t.equals(KList.BALL) || t.equals(KList.SPHERE) || t.equals(KList.RING))
                changeRadius(l, true);
            else // VECTOR, DOT, RING, ARROW
                changeWidth(l, true);
            // TRIANGLE seems to be unaffected by radius and width
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSmaller(ActionEvent ev)
    {
        for(KList l : getLists())
        {
            String t = l.getType();
            if(t.equals(KList.BALL) || t.equals(KList.SPHERE) || t.equals(KList.RING))
                changeRadius(l, false);
            else // VECTOR, DOT, RING, ARROW
                changeWidth(l, false);
            // TRIANGLE seems to be unaffected by radius and width
        }
    }
//}}}

//{{{ onOpaquer, onTransparenter
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpaquer(ActionEvent ev)
    {
        for(KList l : getLists())
        {
            int a = l.getAlpha() + 15;
            l.setAlpha(Math.min(a, ALPHA_MAX));
            l.fireKinChanged(KList.CHANGE_LIST_PROPERTIES);
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTransparenter(ActionEvent ev)
    {
        for(KList l : getLists())
        {
            int a = l.getAlpha() - 15;
            l.setAlpha(Math.max(a, ALPHA_MIN));
            l.fireKinChanged(KList.CHANGE_LIST_PROPERTIES);
        }
    }
//}}}

//{{{ onSetColor
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSetColor(ActionEvent ev)
    {
        KPaint newColor = (KPaint) colors.getSelectedItem();
        for(KList l : getLists())
        {
            l.setColor(newColor);
            for(KPoint p : l.getChildren())  // bleach constituent points so 
                p.setColor(null);            // new color actually shows up
            l.fireKinChanged(KList.CHANGE_LIST_PROPERTIES);
        }
    }
//}}}

//{{{ onAddMaster
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAddMaster(ActionEvent ev)
    {
        String newMaster = (String) JOptionPane.showInputDialog(
            new JDialog(), "Name of new master:", "Add master to lists",
            JOptionPane.PLAIN_MESSAGE, null, null, null);
        if(newMaster == null) return;
        for(KList l : getLists())
            if(!l.getMasters().contains(newMaster))
                l.addMaster(newMaster);
    }
//}}}

//{{{ onClose
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onClose(ActionEvent ev)
    {
        dialog.dispose();
    }
//}}}

//{{{ onChangeHistory
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onChangeHistory(ActionEvent ev)
    {
        JOptionPane.showMessageDialog(dialog, "Cannot compute!", 
            "Change human history", JOptionPane.ERROR_MESSAGE);
        cbOther.setSelected(false);
    }
//}}}

//{{{ onShowDialog
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowDialog(ActionEvent ev)
    {
        buildGUI();
        dialog.pack();
        Container w = kMain.getContentContainer();
        if(w != null)
        {
            Point p = w.getLocation();
            Dimension dimDlg = dialog.getSize();
            Dimension dimWin = w.getSize();
            p.x += dimWin.width - (dimDlg.width / 2) ;
            p.y += (dimWin.height - dimDlg.height) / 2;
            dialog.setLocation(p);
        }
        dialog.setVisible(true);
    }
//}}}

//{{{ getLists
//##############################################################################
    public ArrayList<KList> getLists()
    {
        ArrayList<KList> lists = new ArrayList<KList>();
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return lists; // empty
        for(KList l : cbVis.isSelected() ? KIterator.visibleLists(kin) : KIterator.allLists(kin))
        {
            String t = l.getType();
            if(cbVec.isSelected() && t.equals(KList.VECTOR))  lists.add(l);
            if(cbBall.isSelected() && t.equals(KList.BALL))   lists.add(l);
            if(cbDot.isSelected() && t.equals(KList.DOT))     lists.add(l);
            if(cbRib.isSelected() && t.equals(KList.RIBBON))  lists.add(l);
            if(cbProbe.isSelected() && isProbeList(l))        lists.add(l);
        }
        return lists;
    }
//}}}

//{{{ isProbeList
//##############################################################################
    public boolean isProbeList(KList l)
    {
        //if(l.getName().equals("x")) return true; -- effective, but probably too kludgy
        for(Iterator iter = l.getMasters().iterator(); iter.hasNext(); )
        {
            String master = (String) iter.next();
            if(master.equals("vdw contact")
            || master.equals("small overlap")
            || master.equals("bad overlap")
            || master.equals("H-bonds")
            || master.equals("dots")) return true;
        }
        return false;
    }
//}}}

//{{{ changeRadius
//##############################################################################
    private void changeRadius(KList l, boolean increase)
    {
        // List
        if(increase)
        {
            float r = (float) (l.getRadius() * 1.5);
            l.setRadius(Math.min(r, RADIUS_MAX));
        }
        else // decrease
        {
            float r = (float) (l.getRadius() * 0.667);
            l.setRadius(Math.max(r, RADIUS_MIN));
        }
        
        // Points - override list
        for(KPoint p : l.getChildren())
        {
            if(p instanceof RingPoint) // or, by extension, BallPoint
            {
                RingPoint rp = (RingPoint) p;
                if(rp.r0 != 0f)
                {
                    if(increase)
                    {
                        float r = (float) (rp.r0 * 1.5);
                        rp.r0 = Math.min(r, RADIUS_MAX);
                    }
                    else // decrease
                    {
                        float r = (float) (rp.r0 * 0.667);
                        rp.r0 = Math.max(r, RADIUS_MIN);
                    }
                }
            }
        }
    }
//}}}

//{{{ changeWidth
//##############################################################################
    private void changeWidth(KList l, boolean increase)
    {
        // List
        if(increase)
        {
            int w = l.getWidth() + 1;
            l.setWidth(Math.min(w, WIDTH_MAX));
        }
        else // decrease
        {
            int w = l.getWidth() - 1;
            l.setWidth(Math.max(w, WIDTH_MIN));
        }
        
        // Points - override list
        for(KPoint p : l.getChildren())
        {
            if(p instanceof VectorPoint)
            {
                VectorPoint vp = (VectorPoint) p;
                if(vp.getWidth() != 0)
                {
                    if(increase)
                    {
                        int w = vp.getWidth() + 1;
                        vp.setWidth(Math.min(w, WIDTH_MAX));
                    }
                    else // decrease
                    {
                        int w = vp.getWidth() - 1;
                        vp.setWidth(Math.max(w, WIDTH_MIN)); 
                    }
                }
            }
        }
    }
//}}}

//{{{ getToolsMenuItem, getHelpAnchor, toString
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    { return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onShowDialog")); }
    
    public JMenuItem getHelpMenuItem()
    { return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onHelp")); }
    
    public String getHelpAnchor()
    { return "#multi-list-edit-plugin"; }

    public String toString()
    { return "Multi-list editor"; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

