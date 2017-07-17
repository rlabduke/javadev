// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.io.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import java.util.List;
//}}}
/**
 * <code>ViewEditor</code> is a UI for renaming, reordering, deleting, etc views.
 *
 * <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
 * <br>Begun on Thu Dec  5 09:46:29 EST 2002
*/
public class ViewEditor //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain    kMain;
    
    JDialog     dialog;
    JList       list;
    
    JButton     close;  // DAK 090726
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
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        list = new FatJList(0, 20);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(12);
        list.setBorder( BorderFactory.createEtchedBorder() );
        JScrollPane listScroll = new JScrollPane(list);
        
        JButton go         = new JButton(new ReflectiveAction("Go to", null, this, "onGoTo"));
        JButton gonext     = new JButton(new ReflectiveAction("Go Next", kMain.prefs.stepForwardIcon, this, "onGoNext"));
        JButton goprev     = new JButton(new ReflectiveAction("Go Prev", kMain.prefs.stepBackIcon, this, "onGoPrev"));
        JButton rename     = new JButton(new ReflectiveAction("Rename", null, this, "onRename"));
        JButton delete     = new JButton(new ReflectiveAction("Delete", kMain.prefs.deleteIcon, this, "onDelete"));
        JButton up         = new JButton(new ReflectiveAction("Move up", kMain.prefs.moveUpIcon, this, "onMoveUp"));
        JButton down       = new JButton(new ReflectiveAction("Move down", kMain.prefs.moveDownIcon, this, "onMoveDown"));
        /*JButton*/ close  = new JButton(new ReflectiveAction("Close", null, this, "onClose"));
        JButton export     = new JButton(new ReflectiveAction("Export", null, this, "onExport"));
        //JButton importView = new JButton(new ReflectiveAction("Import", null, this, "onImport"));
        
        // Mnemonics: require alt+key to execute
        go.setMnemonic(KeyEvent.VK_G);
        rename.setMnemonic(KeyEvent.VK_R);
        delete.setMnemonic(KeyEvent.VK_BACK_SPACE);
        up.setMnemonic(KeyEvent.VK_U);
        down.setMnemonic(KeyEvent.VK_D);
        close.setMnemonic(KeyEvent.VK_C);
        
        // Key bindings: just type the key to execute -- DAK 090929
        InputMap im = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_G,          0                       ), "goto"    );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,      KingMain.MENU_ACCEL_MASK), "gonext"  );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,       KingMain.MENU_ACCEL_MASK), "goprev"  );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,         KingMain.MENU_ACCEL_MASK), "moveup"  );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,       KingMain.MENU_ACCEL_MASK), "movedown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R,          0                       ), "rename"  );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0                       ), "delete"  );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D,          0                       ), "delete"  );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,     0                       ), "close"   );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,          KingMain.MENU_ACCEL_MASK), "close"   );
        ActionMap am = dialog.getRootPane().getActionMap();
        am.put("goto"    , new ReflectiveAction(null, null, this, "onGoTo"    ));
        am.put("gonext"  , new ReflectiveAction(null, null, this, "onGoNext"  ));
        am.put("goprev"  , new ReflectiveAction(null, null, this, "onGoPrev"  ));
        am.put("rename"  , new ReflectiveAction(null, null, this, "onRename"  ));
        am.put("delete"  , new ReflectiveAction(null, null, this, "onDelete"  ));
        am.put("moveup"  , new ReflectiveAction(null, null, this, "onMoveUp"  ));
        am.put("movedown", new ReflectiveAction(null, null, this, "onMoveDown"));
        am.put("close"   , new ReflectiveAction(null, null, this, "onClose"   ));
        
        TablePane cp = new TablePane();
        cp.insets(2).hfill(true).weights(0,0);
        cp.save().weights(1,1).vfill(true).hfill(true).addCell(listScroll, 1, 11).restore();
        cp.addCell(go).newRow();
        cp.addCell(gonext).newRow();
        cp.addCell(goprev).newRow();
        cp.save().weights(0,1).insets(0).addCell(Box.createVerticalStrut(10)).restore().newRow();
        cp.addCell(rename).newRow();
        cp.addCell(delete).newRow();
        cp.save().weights(0,1).insets(0).addCell(Box.createVerticalStrut(10)).restore().newRow();
        cp.addCell(up).newRow();
        cp.addCell(down).newRow();
        cp.save().weights(0,1).insets(0).addCell(Box.createVerticalStrut(10)).restore().newRow();
        cp.addCell(export).newRow();
        //cp.addCell(importView).newRow();
        cp.center().hfill(false).addCell(close, 2, 1);
        dialog.setContentPane(cp);
        
        dialog.getRootPane().setDefaultButton(close);
        
        list.requestFocus();
    }
//}}}

//{{{ onClose, onMoveUp, onMoveDown
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
        KView view = (KView)list.getSelectedValue();
        if(view == null) return;
        
        List<KView> viewList = kin.getViewList();
        ListIterator<KView> iter = viewList.listIterator();
        KView swap = null, next = null;
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
        kin.setModified(true);
        kin.fireKinChanged(AHE.CHANGE_VIEWS_LIST);

        // Re-fill the list so names are updated
        list.setListData( viewList.toArray() );
        list.setSelectedValue(view, true);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onMoveDown(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        KView view = (KView)list.getSelectedValue();
        if(view == null) return;
        
        List<KView> viewList = kin.getViewList();
        ListIterator<KView> iter = viewList.listIterator();
        while(!view.equals(iter.next())) {}
        if(iter.hasNext())
        {
            KView swap = iter.next();
            iter.set(view);
            iter.previous(); //back to swap...
            iter.previous(); //back to view...
            iter.set(swap);
        }
        kin.setModified(true);
        kin.fireKinChanged(AHE.CHANGE_VIEWS_LIST);

        // Re-fill the list so names are updated
        list.setListData( viewList.toArray() );
        list.setSelectedValue(view, true);
    }
//}}}

//{{{ onGoTo, onRename, onDelete
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onGoTo(ActionEvent ev)
    {
        KView view = (KView)list.getSelectedValue();
        if(view == null) return;
        kMain.setView(view);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRename(ActionEvent ev)
    {
        KView view = (KView)list.getSelectedValue();
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
        kin.setModified(true);
        kin.fireKinChanged(AHE.CHANGE_VIEWS_LIST);
        list.setListData( kin.getViewList().toArray() );
        list.setSelectedValue(view, true);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDelete(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        KView view = (KView)list.getSelectedValue();
        if(view == null) return;
        
        List<KView> viewList = kin.getViewList();
        viewList.remove(view);
        kin.setModified(true);
        kin.fireKinChanged(AHE.CHANGE_VIEWS_LIST);

        // Re-fill the list so names are updated
        list.setListData( viewList.toArray() );
    }
    
//}}}

//{{{ onGoNext, onGoPrev
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onGoNext(ActionEvent ev)
    {
        int index = list.getSelectedIndex()+1;
        if(index >= 0 && index < list.getModel().getSize())
        {
            list.setSelectedIndex(index);
            onGoTo(null);
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onGoPrev(ActionEvent ev)
    {
        int index = list.getSelectedIndex()-1;
        if(index >= 0 && index < list.getModel().getSize())
        {
            list.setSelectedIndex(index);
            onGoTo(null);
        }
    }
//}}}

//{{{ onExport
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExport(ActionEvent ev)
    {
      Kinemage kin = kMain.getKinemage();
      if(kin == null) return;
      KView view = (KView)list.getSelectedValue();
      if(view == null) return;
      int index = list.getSelectedIndex()+1;
      
      JFileChooser c = new JFileChooser();
      File workingDirectory = new File(System.getProperty("user.dir"));
      c.setCurrentDirectory(workingDirectory);
      int rVal = c.showSaveDialog(kMain.getTopWindow());
      if (rVal == JFileChooser.APPROVE_OPTION) {
        try {
          KinfileWriter kw = new KinfileWriter();
          PrintWriter out = new PrintWriter(c.getSelectedFile());
          kw.setOutWriter(out);
          kw.writeView(view, index);
          out.flush();
          JOptionPane.showMessageDialog(null, view.getName()+" was written!");
        } catch (FileNotFoundException fe) {
          System.out.println("Error writing to :"+(c.getSelectedFile()).getName());
        }
      }
      
     
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
        dialog.getRootPane().setDefaultButton(close);
        list.requestFocus();
        
        // remember, execution of this thread stops here until dialog is closed
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

