// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
//}}}
/**
 * <code>ViewEditor</code> has not yet been documented.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Thu Dec  5 09:46:29 EST 2002
*/
public class ViewEditor //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    
    JDialog     dialog;
    JList       list;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ViewEditor(KingMain kmain)
    {
        kMain = kmain;
        
        dialog = new JDialog(kMain.getTopWindow(), "Edit views", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        list = new FatJList(0, 20);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(12);
        list.setBorder( BorderFactory.createEtchedBorder() );
        JScrollPane listScroll = new JScrollPane(list);
        
        JButton close   = new JButton(new ReflectiveAction("Close", null, this, "onClose"));
        JButton up      = new JButton(new ReflectiveAction(null, kMain.prefs.moveUpIcon, this, "onMoveUp"));
        JButton down    = new JButton(new ReflectiveAction(null, kMain.prefs.moveDownIcon, this, "onMoveDown"));
        JButton go      = new JButton(new ReflectiveAction("Go to", null, this, "onGoTo"));
        JButton rename  = new JButton(new ReflectiveAction("Rename", null, this, "onRename"));
        JButton delete  = new JButton(new ReflectiveAction("Delete", kMain.prefs.deleteIcon, this, "onDelete"));

        up.setMnemonic(KeyEvent.VK_U);
        down.setMnemonic(KeyEvent.VK_D);
        rename.setMnemonic(KeyEvent.VK_R);
        go.setMnemonic(KeyEvent.VK_G);
        close.setMnemonic(KeyEvent.VK_C);
        
        TablePane cp = new TablePane();
        cp.insets(4).hfill(true).weights(0,0);
        cp.save().weights(1,1).vfill(true).hfill(true).addCell(listScroll, 1, 7).restore();
        cp.addCell(up).newRow();
        cp.save().weights(0,1).insets(0).addCell(Box.createVerticalGlue()).restore().newRow();
        cp.addCell(go).newRow();
        cp.addCell(rename).newRow();
        cp.addCell(delete).newRow();
        cp.save().weights(0,1).insets(0).addCell(Box.createVerticalGlue()).restore().newRow();
        cp.addCell(down).newRow();
        cp.center().hfill(false).addCell(close, 2, 1);
        dialog.setContentPane(cp);
        
        dialog.getRootPane().setDefaultButton(close);
    }
//}}}

//{{{ onClose, onMoveUp, onMoveDown, onGoTo, onRename, onDelete
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onClose(ActionEvent ev)
    {
        dialog.dispose();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMoveUp(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        KingView view = (KingView)list.getSelectedValue();
        if(view == null) return;
        
        java.util.List viewList = kin.getViewList();
        ListIterator iter = viewList.listIterator();
        Object swap = null, next = null;
        do
        {
            swap = next;
            next = iter.next();
        } while(!view.equals(next));
        if(swap != null)
        {
            iter.set(swap);
            iter.previous(); //back to next...
            iter.previous(); //back to swap...
            iter.set(view);
        }

        // Re-fill the list so names are updated
        list.setListData( viewList.toArray() );
        list.setSelectedValue(view, true);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMoveDown(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        KingView view = (KingView)list.getSelectedValue();
        if(view == null) return;
        
        java.util.List viewList = kin.getViewList();
        ListIterator iter = viewList.listIterator();
        while(!view.equals(iter.next())) {}
        if(iter.hasNext())
        {
            Object swap = iter.next();
            iter.set(view);
            iter.previous(); //back to swap...
            iter.previous(); //back to view...
            iter.set(swap);
        }

        // Re-fill the list so names are updated
        list.setListData( viewList.toArray() );
        list.setSelectedValue(view, true);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onGoTo(ActionEvent ev)
    {
        KingView view = (KingView)list.getSelectedValue();
        if(view == null) return;
        
        view.selectedFromMenu(null);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRename(ActionEvent ev)
    {
        KingView view = (KingView)list.getSelectedValue();
        if(view == null) return;
        
        String viewname = (String)JOptionPane.showInputDialog(kMain.getTopWindow(),
            "Name for this view:",
            "Rename view",
            JOptionPane.PLAIN_MESSAGE,
            null,//icon
            null,//selections
            view.getName());
        if(viewname == null) return;
        view.setName(viewname);

        // Re-fill the list so names are updated
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        list.setListData( kin.getViewList().toArray() );
        list.setSelectedValue(view, true);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDelete(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        KingView view = (KingView)list.getSelectedValue();
        if(view == null) return;
        
        java.util.List viewList = kin.getViewList();
        viewList.remove(view);

        // Re-fill the list so names are updated
        list.setListData( viewList.toArray() );
    }
    
//}}}

//{{{ editViews
//##################################################################################################
    /** Display the view-editing dialog box */
    public void editViews()
    {
        // Fill the list
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        list.setListData( kin.getViewList().toArray() );
        
        // Display dialog box
        dialog.pack();
        dialog.setLocationRelativeTo(kMain.getTopWindow());
        dialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
        
        UIMenus menus = kMain.getMenus();
        if(menus != null) menus.rebuildViewsMenu(kin.getViewIterator());
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

