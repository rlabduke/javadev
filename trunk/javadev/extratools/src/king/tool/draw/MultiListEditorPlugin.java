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
    JCheckBox cbVec, cbBall, cbDot, cbProbe, cbVis;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MultiListEditorPlugin(ToolBox tb)
    {
        super(tb);
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        cbVec   = new JCheckBox("vectorlists", true);
        cbBall  = new JCheckBox("balllists", false);
        cbDot   = new JCheckBox("dotlists", false);
        cbProbe = new JCheckBox("Probe dots", false);
        
        JButton bigger  = new JButton(new ReflectiveAction("BIGGER", null, this, "onBigger"));
        JButton smaller = new JButton(new ReflectiveAction("smaller", null, this, "onSmaller"));
        JButton opaquer = new JButton(new ReflectiveAction("Opaquer", null, this, "onOpaquer"));
        JButton clearer = new JButton(new ReflectiveAction("Clearer", null, this, "onClearer"));
        JButton master  = new JButton(new ReflectiveAction("Add master", null, this, "onAddMaster"));
        
        cbVis   = new JCheckBox("only visible", true);
        
        TablePane2 cp = new TablePane2();
        cp.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        cp.addCell(cbVec).newRow();
        cp.addCell(cbBall).newRow();
        cp.addCell(cbDot).newRow();
        cp.addCell(cbProbe).newRow();
        cp.addCell(bigger).addCell(opaquer).newRow();
        cp.addCell(smaller).addCell(clearer).newRow();
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

//{{{ onClearer, onOpaquer
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onClearer(ActionEvent ev)
    {
        for(KList l : getLists())
        {
            int a = l.getAlpha() - 15;
            l.setAlpha(Math.max(a, ALPHA_MIN));
            l.fireKinChanged(KList.CHANGE_LIST_PROPERTIES);
        }
    }

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
            if(cbProbe.isSelected() && isProbeDotList(l))     lists.add(l);
        }
        return lists;
    }
//}}}

//{{{ isProbeDotList
//##############################################################################
    public boolean isProbeDotList(KList l)
    {
        if(l.getName().equals("x")) return true;
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
    { return "#multi-list-edit-plugin"; }

    public String toString()
    { return "Multi-list editor"; }
    
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

