// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

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
* <code>HierarchyWindow</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Apr  3 10:04:45 EST 2003
*/
public class HierarchyWindow //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain    kMain;
    KinTree     kTree;
    
    ReflectiveAction    acNew, acDelete, acProperties, acTransform, acVisible,
        acCut, acPaste, acUp, acDown;

    JFrame frame;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public HierarchyWindow(KingMain kmain, KinTree ktree)
    {
        kMain = kmain;
        kTree = ktree;

        initActions();
        buildGUI();
    }
//}}}

//{{{ initActions
//##################################################################################################
    private void initActions()
    {
        KingPrefs prefs = kMain.getPrefs();
        acNew           = new ReflectiveAction("New (sub)group", null, kTree, "onNewGroup");
        acDelete        = new ReflectiveAction("Delete", prefs.deleteIcon, kTree, "onDelete");
        acProperties    = new ReflectiveAction("Properties", null, kTree, "onProperties");
        acTransform     = new ReflectiveAction("Transform", null, kTree, "onTransform");
        acVisible       = new ReflectiveAction("Show/Hide", null, kTree, "onToggleVisibility");
        acCut           = new ReflectiveAction("Cut", null, kTree, "onCut");
        acPaste         = new ReflectiveAction("Paste", null, kTree, "onPaste");
        acUp            = new ReflectiveAction("Move up", prefs.moveUpIcon, kTree, "onUp");
        acDown          = new ReflectiveAction("Move down", prefs.moveDownIcon, kTree, "onDown");
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI()
    {
        frame = new JFrame();
        frame = new JFrame("Hierarchy window");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setIconImage(kMain.getPrefs().windowIcon);
        
        Container content = frame.getContentPane();
        content.setLayout(new BorderLayout());
        
        JScrollPane treeScroll = new JScrollPane(kTree.getTree());
        Dimension dim = treeScroll.getPreferredSize();
        dim.width = 300; treeScroll.setPreferredSize(dim);
        content.add(treeScroll, BorderLayout.CENTER);
        
        TablePane lBtns = new TablePane();
        lBtns.weights(1,0).center().hfill(true);
        lBtns.addCell(new JButton(acNew)).newRow();
        lBtns.addCell(new JButton(acCut)).newRow();
        lBtns.addCell(new JButton(acPaste)).newRow();
        lBtns.addCell(new JButton(acUp)).newRow();
        lBtns.addCell(new JButton(acDown)).newRow();
        lBtns.addCell(lBtns.strut(0,8)).newRow();
        lBtns.addCell(new JButton(acDelete)).newRow();
        content.add(lBtns, BorderLayout.WEST);
        
        TablePane rBtns = new TablePane();
        rBtns.weights(1,0).center().hfill(true);
        rBtns.addCell(new JButton(acVisible)).newRow();
        rBtns.addCell(new JButton(acProperties)).newRow();
        rBtns.addCell(new JButton(acTransform)).newRow();
        content.add(rBtns, BorderLayout.EAST);
        
        frame.pack();
    }
//}}}

//{{{ show, hide
//##################################################################################################
    public void show()
    {
        if(!frame.isVisible())
        {
            Window w = kMain.getMainWindow();
            if(w != null) frame.setLocationRelativeTo(w);
            frame.setVisible(true);
        }
        else
        {
            frame.toFront();
        }
    }
    
    public void hide()
    {
        frame.setVisible(false);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

