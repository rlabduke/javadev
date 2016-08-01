// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
//import gnu.regexp.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>KinTree</code> is the kinemage-browsing tree component used for group-level editing.
* One day this should be refactored to yield a reusable tree component for tools, etc.
*
* <p>Begun on Sun Jun 30 20:20:49 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class KinTree implements KMessage.Subscriber
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain    kMain;

    JFrame      frame;
    JTree       tree;
    JPopupMenu  menu;
    GroupEditor groupEditor;

    ReflectiveAction acNew, acDelete, acProperties, acTransform, acVisible,
        acCut, acCopy, acPaste, acUp, acDown;

    AGE         clipboard = null;       // destination for cut/paste events
    int         clipboardDepth = -1;    // previous getDepth() value for cut object
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public KinTree(KingMain kmain)
    {
        kMain = kmain;
        
        tree = new JTree(new KinTreeModel(new Kinemage())); // otherwise we get a "sample" TreeModel
        tree.setRootVisible(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setEditable(false);
        tree.setShowsRootHandles(true);
        // This will eliminate the annoying menu/tree interactions
        // where Alt-X also selects an item beginning with 'X'
        tree.setUI(new NullTreeUI());
        // Provides feedback on whether things are on or off
        tree.setCellRenderer(new OnOffRenderer());
        
        // JTree doesn't get built-in drop support until Java 1.6,
        // so there's no mechanism for dropping nodes in a particular place.
        //tree.setDragEnabled(true);
        
        initActions();
        buildMenu();
        buildGUI();
        
        groupEditor = new GroupEditor(kMain, frame);
        
        kMain.subscribe(this);
    }
//}}}

//{{{ initActions
//##################################################################################################
    private void initActions()
    {
        KingPrefs prefs = kMain.getPrefs();
        acNew           = new ReflectiveAction("New (sub)group", prefs.addIcon, this, "onNewGroup");
            acNew.setTooltip("Create a new group or subgroup below the highlighted one");
            acNew.setMnemonic(KeyEvent.VK_N);
            acNew.setCommandKey("new");
            acNew.setAccelerator(KeyEvent.VK_INSERT, 0);
            acNew.bind(tree);
        acDelete        = new ReflectiveAction("Delete", prefs.deleteIcon, this, "onDelete");
            acDelete.setTooltip("Discard the highlighted object from the hierarchy");
            acDelete.setMnemonic(KeyEvent.VK_D);
            acDelete.setCommandKey("delete");
            acDelete.setAccelerator(KeyEvent.VK_DELETE, 0);
            acDelete.bind(tree);
        acProperties    = new ReflectiveAction("Properties", null, this, "onProperties");
            acProperties.setTooltip("Edit properties associated with this object");
            acProperties.setMnemonic(KeyEvent.VK_R);
            acProperties.setCommandKey("properties");
            acProperties.setAccelerator(KeyEvent.VK_ENTER, 0);
            acProperties.bind(tree);
        acTransform     = new ReflectiveAction("Transform", null, this, "onTransform");
            acTransform.setTooltip("Rotate, translate, or scale the highlighted object");
            acTransform.setMnemonic(KeyEvent.VK_T);
        acVisible       = new ReflectiveAction("Show/Hide", null, this, "onToggleVisibility");
            acVisible.setTooltip("Turn on (off) the button that controls this object");
            acVisible.setMnemonic(KeyEvent.VK_S);
            acVisible.setCommandKey("toggle-visible");
            acVisible.setAccelerator(KeyEvent.VK_SLASH, 0);
            acVisible.bind(tree);
        acCut           = new ReflectiveAction("Cut", prefs.cutIcon, this, "onCut");
            acCut.setTooltip("Extract this object so it can be re-inserted elsewhere");
            acCut.setMnemonic(KeyEvent.VK_C);
            acCut.setCommandKey("cut");
            acCut.setAccelerator(KeyEvent.VK_X, KingMain.MENU_ACCEL_MASK);
            acCut.bind(tree);
        acCopy          = new ReflectiveAction("Copy", prefs.copyIcon, this, "onCopy");
            acCopy.setTooltip("Duplicate this object and place it on the clipboard");
            acCopy.setMnemonic(KeyEvent.VK_Y);
            acCopy.setCommandKey("copy");
            acCopy.setAccelerator(KeyEvent.VK_C, KingMain.MENU_ACCEL_MASK);
            acCopy.bind(tree);
        acPaste         = new ReflectiveAction("Paste", prefs.pasteIcon, this, "onPaste");
            acPaste.setTooltip("Re-insert the last object that was Cut or Copied from the hierarchy");
            acPaste.setMnemonic(KeyEvent.VK_P);
            acPaste.setCommandKey("paste");
            acPaste.setAccelerator(KeyEvent.VK_V, KingMain.MENU_ACCEL_MASK);
            acPaste.bind(tree);
        acUp            = new ReflectiveAction("Move up", prefs.moveUpIcon, this, "onUp");
            acUp.setTooltip("Move this object toward the top of the list of its peers");
            acUp.setMnemonic(KeyEvent.VK_U);
            acUp.setCommandKey("up");
            acUp.setAccelerator(KeyEvent.VK_UP, KingMain.MENU_ACCEL_MASK);
            acUp.bind(tree);
        acDown          = new ReflectiveAction("Move down", prefs.moveDownIcon, this, "onDown");
            acDown.setTooltip("Move this object toward the bottom of the list of its peers");
            acDown.setMnemonic(KeyEvent.VK_O);
            acDown.setCommandKey("down");
            acDown.setAccelerator(KeyEvent.VK_DOWN, KingMain.MENU_ACCEL_MASK);
            acDown.bind(tree);
    }
//}}}

//{{{ buildMenu
//##################################################################################################
    private void buildMenu()
    {
        menu = new JPopupMenu();
        menu.setInvoker(this.getTree());
        
        JMenuItem mNew = new JMenuItem(acNew);
        JMenuItem mDelete = new JMenuItem(acDelete);
        JMenuItem mProperties = new JMenuItem(acProperties);
        JMenuItem mTransform = new JMenuItem(acTransform);
        JMenuItem mVisible = new JMenuItem(acVisible);
        JMenuItem mCut = new JMenuItem(acCut);
        JMenuItem mCopy = new JMenuItem(acCopy);
        JMenuItem mPaste = new JMenuItem(acPaste);
        JMenuItem mUp = new JMenuItem(acUp);
        JMenuItem mDown = new JMenuItem(acDown);
        
        // assemble menu here!
        menu.add(mVisible);
        menu.add(mProperties);
        menu.addSeparator();
        menu.add(mCut);
        menu.add(mCopy);
        menu.add(mPaste);
        menu.add(mDelete);
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
        
        JScrollPane treeScroll = new JScrollPane(this.getTree());
        Dimension dim = treeScroll.getPreferredSize();
        dim.width = 300; treeScroll.setPreferredSize(dim);
        content.add(treeScroll, BorderLayout.CENTER);
        
        JButton btnNew = new JButton(acNew);
          btnNew.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnCut = new JButton(acCut);
          btnCut.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnCopy = new JButton(acCopy);
          btnCopy.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnPaste = new JButton(acPaste);
          btnPaste.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnUp = new JButton(acUp);
          btnUp.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnDown = new JButton(acDown);
          btnDown.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnDelete = new JButton(acDelete);
          btnDelete.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnVisible = new JButton(acVisible);
          btnVisible.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnProperties = new JButton(acProperties);
          btnProperties.setHorizontalAlignment(SwingConstants.LEFT);
        JButton btnTransform = new JButton(acTransform);
          btnTransform.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Key bindings: just type the key to execute -- DAK 090929
        InputMap im = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,     0                       ), "hide");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,          KingMain.MENU_ACCEL_MASK), "hide");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0                       ), "delete");
        ActionMap am = frame.getRootPane().getActionMap();
        am.put("hide", new ReflectiveAction(null, null, this, "onHide"));
        am.put("delete", new ReflectiveAction(null, null, this, "onDelete"));
        
        TablePane2 lBtns = new TablePane2();
        lBtns.weights(1,0).center().hfill(true).memorize();
        lBtns.addCell(btnVisible).newRow();
        lBtns.addCell(btnProperties).newRow();
        lBtns.addCell(btnTransform).newRow();
        lBtns.addCell(lBtns.strut(0,16)).newRow();
        lBtns.addCell(btnNew).newRow();
        lBtns.addCell(btnCut).newRow();
        lBtns.addCell(btnCopy).newRow();
        lBtns.addCell(btnPaste).newRow();
        lBtns.addCell(btnUp).newRow();
        lBtns.addCell(btnDown).newRow();
        lBtns.addCell(lBtns.strut(0,16)).newRow();
        lBtns.addCell(btnDelete).newRow();
        content.add(lBtns, BorderLayout.WEST);
        
        /*TablePane rBtns = new TablePane();
        rBtns.weights(1,0).center().hfill(true);
        rBtns.addCell(btnVisible).newRow();
        rBtns.addCell(btnProperties).newRow();
        rBtns.addCell(btnTransform).newRow();
        content.add(rBtns, BorderLayout.EAST);*/
        
        frame.pack();
    }
//}}}

//{{{ show, hide, onHide
//##################################################################################################
    public void show()
    {
        if(!frame.isVisible())
        {
            //Window w = kMain.getTopWindow();
            //if(w != null) frame.setLocationRelativeTo(w);
            frame.setLocationRelativeTo(kMain.getTopWindow()); // OK if it's null
            frame.setVisible(true);
        }
        else
        {
            frame.toFront();
        }
    }
    
    public void hide()
    {
        frame.dispose();
    }

    // DAK 090929
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHide(ActionEvent ev)
    {
        frame.dispose();
    }
//}}}

//{{{ reveal, getTreePath
//##################################################################################################
    /** Makes sure the given AGE is visible in the tree window */
    public void reveal(AGE target)
    {
        TreePath path = getTreePath(target);
        tree.scrollPathToVisible(path);
        tree.setSelectionPath(path);
    }
    
    private TreePath getTreePath(AGE leaf)
    {
        AGE parent = leaf.getParent();
        if(parent == null) return new TreePath(leaf);
        else return getTreePath(parent).pathByAddingChild(leaf);
    }
//}}}

//{{{ deliverMessage, getTree
//##################################################################################################
    static final long RELOAD_TREE_P = KMessage.KIN_SWITCHED | KMessage.KIN_CLOSED | KMessage.ALL_CLOSED;
    static final int RELOAD_TREE_K  = AHE.CHANGE_TREE_PROPERTIES | AHE.CHANGE_TREE_ON_OFF;
    
    public void deliverMessage(KMessage msg)
    {
        if(msg.testProg(RELOAD_TREE_P))
        {
            Kinemage k = kMain.getKinemage();
            if(k == null)
            {
                tree.setModel( new KinTreeModel(new Kinemage()) );
                tree.setRootVisible(false);
            }
            else
            {
                tree.setModel( new KinTreeModel(k) );
                tree.setRootVisible(true);
            }
        }
        else if(msg.testKin(AHE.CHANGE_TREE_CONTENTS))
        {
            TreeModel m = tree.getModel();
            if(m instanceof KinTreeModel)
            {
                // It's hard for us to tell which part(s) of the kinemage tree
                // changed, but JTree wipes out all selection and expandedness
                // information when we send treeStructureChanged().
                // So we go through all this to save and restore that info.
                
                TreePath selPath = tree.getSelectionPath();
                int[] selRow = tree.getSelectionRows();
                Collection<TreePath> vis = KinTreeModel.memorizeVisibility(tree);
                ((KinTreeModel)m).kinChanged(true);
                KinTreeModel.restoreVisibility(tree, vis);
                // Select the same node as before, if it's still there
                if(selPath != null && tree.getRowForPath(selPath) != -1)
                    tree.setSelectionPath(selPath);
                // Else select same row(s) as before, whether that's a parent, sibling, or other
                else if(selRow != null) tree.setSelectionRows(selRow);
                
                // Select same row(s) as before, whether that's a parent, sibling, or other
                //if(sel != null) tree.setSelectionRows(sel);
                //tree.setSelectionPath(sel);
                // For cases where we removed a node from the tree, select its parent
                //if(sel != null && tree.getRowForPath(sel) == -1)
                //    tree.setSelectionPath(sel.getParentPath());
            }
        }
        else if(msg.testKin(RELOAD_TREE_K))
        {
            TreeModel m = tree.getModel();
            if(m instanceof KinTreeModel)
                ((KinTreeModel)m).kinChanged(false);
        }
        
        // The combination of these two calls ensures that
        // the tree is displaying what the model currently holds.
        tree.treeDidChange(); // == revalidate(); repaint();
        //tree.revalidate();
    }

    public JTree getTree() { return tree; }
//}}}

//{{{ onToggleVisibility
//##################################################################################################
    /** Turns the selected group/subgroup/list on or off, regardless of whether it has a button accessible. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onToggleVisibility(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        Object node = path.getLastPathComponent();
        if(node == null || ! (node instanceof AGE)) return;
        AGE ag = (AGE)node;
        ag.setOn(!ag.isOn());
    }
//}}}

//{{{ onProperties
//##################################################################################################
    public void onProperties(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        Object node = path.getLastPathComponent();
        if(node == null || ! (node instanceof AGE)) return;
        
        boolean changed = false;
        if(node instanceof Kinemage)
        {
            changed = groupEditor.editKinemage((Kinemage)node);
        }
        else if(node instanceof KGroup)
        {
            KGroup group = (KGroup) node;
            if(group.getDepth() == 1)
                changed = groupEditor.editGroup(group);
            else
                changed = groupEditor.editSubgroup(group);
        }
        else if(node instanceof KList)
        {
            changed = groupEditor.editList((KList)node);
        }
        else
        {
            JOptionPane.showMessageDialog(frame,
            "You can't edit this object's properties.",
            "Sorry!", JOptionPane.INFORMATION_MESSAGE);
        }
    }
//}}}

//{{{ onTransform
//##################################################################################################
    /** Allows one to translate, rotate, and scale all points beneath the selected group/list. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTransform(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        Object node = path.getLastPathComponent();
        if(node == null || ! (node instanceof AGE)) return;
        groupEditor.transform((AGE)node);
    }
//}}}

//{{{ onDelete
//##################################################################################################
    /** Deletes the selected group/subgroup/list completely, removing it from the kinemage */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDelete(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        AGE node = (AGE)path.getLastPathComponent();
        if(node == null) return;
        
        if(node instanceof Kinemage)
        {
            if(JOptionPane.showConfirmDialog(frame, "Really close the current kinemage?",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            { kMain.getStable().closeCurrent(); }
            
            return;
        }
        
        AGE parent = node.getParent();
        if(parent != null)
        {
            if(!kMain.prefs.getBoolean("treeConfirmDelete") ||
               JOptionPane.showConfirmDialog(frame, "Really delete the selected item, '"+node.toString()+"'?",
               "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            {
                parent.remove(node);
                Kinemage k = kMain.getKinemage();
                if(k != null) k.setModified(true);
            }
        }
    }
//}}}

//{{{ onCut
//##################################################################################################
    /** Deletes the selected group/subgroup/list from the kinemage, saving it for the next paste command. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCut(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        AGE node = (AGE) path.getLastPathComponent();
        if(node == null || node instanceof Kinemage) return;
        
        AGE parent = node.getParent();
        if(parent != null)
        {
            if(clipboard == null ||
               JOptionPane.showConfirmDialog(frame, "An unpasted item is still in the clipboard -- discard it?",
               "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            {
                clipboard = node;
                clipboardDepth = clipboard.getDepth();
                parent.remove(node);
                Kinemage k = kMain.getKinemage();
                if(k != null) k.setModified(true);
            }
        }
    }
//}}}

//{{{ onPaste
//##################################################################################################
    /** Places the previously cut group/whatever into the currently selected group. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPaste(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        AGE insert = (AGE) path.getLastPathComponent();
        if(insert == null) return;
        int insertDepth = insert.getDepth();
        
        if(clipboard == null) failPaste();
        else if(insert instanceof Kinemage && clipboard instanceof Kinemage)
        {
            ((Kinemage)insert).appendKinemage((Kinemage)clipboard);
            clipboard = null;
        }
        else if(insertDepth <= clipboardDepth - 1) // insert under
        {
            while(insertDepth < clipboardDepth - 1) // insert WAY under
            {
                KGroup newGroup = new KGroup("new");
                newGroup.setHasButton(false);
                insert.add(newGroup);
                insert = newGroup;
                insertDepth++;
                path = path.pathByAddingChild(newGroup);
            }
            // insert directly under
            insert.add(clipboard);
            tree.setSelectionPath(path.pathByAddingChild(clipboard));
            clipboard = null;
        }
        else if(insertDepth == clipboardDepth) // merge or insert after
        {
            if(!kMain.prefs.getBoolean("treeConfirmMerge") ||
                JOptionPane.showConfirmDialog(frame, "Merge these two?",
                "Confirm merge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            {
                for(Object child : clipboard) insert.add((AHE)child);
                clipboard = null;
            }
            else // insert after
            {
                AGE parent = insert.getParent();
                ArrayList children = parent.getChildren();
                int pos = children.indexOf(insert);
                children.add(pos+1, clipboard);
                clipboard.setParent(parent);
                parent.fireKinChanged(AHE.CHANGE_TREE_CONTENTS);
                tree.setSelectionPath(path.getParentPath().pathByAddingChild(clipboard));
                clipboard = null;
            }
        }
        else failPaste();
        
        Kinemage k = kMain.getKinemage();
        if(k != null) k.setModified(true);
    }
    
    void failPaste()
    {
        String msg = "You can't paste that here.\nPut it higher up in the hierarchy.";
        if(clipboard == null) msg = "Nothing on the clipboard to paste!";
        JOptionPane.showMessageDialog(frame, msg, "Sorry!", JOptionPane.ERROR_MESSAGE);
    }
//}}}

//{{{ onCopy
//##################################################################################################
    /** Duplicates the selected group/subgroup/list from the kinemage, saving it for the next paste command. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCopy(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        AGE node = (AGE)path.getLastPathComponent();
        if(node == null) return;
        
        if(clipboard == null ||
           JOptionPane.showConfirmDialog(frame, "An unpasted item is still in the clipboard -- discard it?",
           "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
        {
            try
            {
                clipboard = node.clone();
                clipboardDepth = clipboard.getDepth();
                Kinemage k = kMain.getKinemage();
                if(k != null) k.setModified(true);
            }
            catch(CloneNotSupportedException ex)    { ex.printStackTrace(SoftLog.err); }
            catch(ClassCastException ex)            { ex.printStackTrace(SoftLog.err); }
        }
    }
//}}}

//{{{ onUp, onDown
//##################################################################################################
    /** Moves the selected group/subgroup/list "up" the list of buttons. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onUp(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        AGE node = (AGE)path.getLastPathComponent();
        if(node == null) return;
        
        AGE parent = node.getParent();
        if(parent == null) return;
        
        ArrayList<AGE> children = parent.getChildren();
        int i = children.indexOf(node);
        if(i == -1 || i == 0) return;
        
        children.remove(i);
        children.add(i-1, node);
        parent.fireKinChanged(AHE.CHANGE_TREE_CONTENTS);

        Kinemage k = kMain.getKinemage();
        if(k != null) k.setModified(true);
    }

    /** Moves the selected group/subgroup/list "down" the list of buttons. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDown(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        AGE node = (AGE)path.getLastPathComponent();
        if(node == null) return;
        
        AGE parent = node.getParent();
        if(parent == null) return;
        
        ArrayList<AGE> children = parent.getChildren();
        int i = children.indexOf(node);
        if(i == -1 || i == children.size()-1) return;
        
        children.remove(i);
        children.add(i+1, node);
        parent.fireKinChanged(AHE.CHANGE_TREE_CONTENTS);

        Kinemage k = kMain.getKinemage();
        if(k != null) k.setModified(true);
    }
//}}}

//{{{ onNewGroup
//##################################################################################################
    /** Places a new, empty group into the currently selected group. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onNewGroup(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        AGE node = (AGE)path.getLastPathComponent();
        if(node == null) return;

        if(node instanceof Kinemage)
        {
            KGroup group = new KGroup("New group");
            ((Kinemage)node).add(group);
            tree.setSelectionPath(path.pathByAddingChild(group));
            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
        }
        else if(node instanceof KGroup && node.getDepth() == 1)
        {
            KGroup subgroup = new KGroup("New subgroup");
            ((KGroup)node).add(subgroup);
            tree.setSelectionPath(path.pathByAddingChild(subgroup));
            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
        }
        else
        {
            JOptionPane.showMessageDialog(frame,
            "You can't create a group/subgroup here.\nSelect the kinemage or a group and try again.",
            "Sorry!", JOptionPane.INFORMATION_MESSAGE);
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ INNER CLASS: OnOffRenderer
//##################################################################################################
    class OnOffRenderer extends DefaultTreeCellRenderer
    {
        public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            Component renderer = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            TreePath path = tree.getPathForRow(row);
            if(path != null)
            {
                Object node = path.getLastPathComponent();
                if(node != null && node instanceof AGE)
                {
                    AGE age = (AGE) node;
                    if(age.isOn())  this.setIcon(kMain.getPrefs().treeOnIcon);
                    else            this.setIcon(kMain.getPrefs().treeOffIcon);
                }
            }
            return renderer;
        }
    }
//}}}

//{{{ INNER CLASS: NullTreeUI
//##################################################################################################
//### NullTreeUI ##################################################################################
//##################################################################################################
    /**
    * Avoids selecting tree items by letter when using menu mnemonic keys.
    * Ignores all normal key presses, but navigation is handled through
    * InputMap/ActionMap, so we don't have any problems.
    *
    * See http://forum.java.sun.com/thread.jsp?forum=57&thread=242078
    */
    class NullTreeUI extends javax.swing.plaf.basic.BasicTreeUI
    {
        protected KeyListener createKeyListener()
        {
            return new NullKeyHandler();
        }
        
        protected MouseListener createMouseListener()
        {
            return new PopupMouseHandler();
        }
        
        /** A key handler that doesn't highlight things based on letters typed */
        class NullKeyHandler extends javax.swing.plaf.basic.BasicTreeUI.KeyHandler
        {
            public void keyTyped(KeyEvent ev)
            {
                // This would ignore only Ctrl/Alt + keypress
                //if((ev.getModifiers() & (KeyEvent.ALT_MASK|KeyEvent.CTRL_MASK)) == 0) super.keyTyped(ev);
            }
        }
        
        /** A mouse handler that can do menu popup on RMB and Edit Properties on double click */
        class PopupMouseHandler extends javax.swing.plaf.basic.BasicTreeUI.MouseHandler
        {
            public void mouseClicked(MouseEvent ev)
            {
                // I can't seem to prevent double click
                // from expanding / collapsing the tree.
                if(ev.getClickCount() == 2)
                    KinTree.this.onProperties(null);
                else
                    super.mouseClicked(ev);
            }
            
            public void mousePressed(MouseEvent ev)
            {
                if(ev.isPopupTrigger())
                {
                    MouseEvent fakeout = new MouseEvent(ev.getComponent(),
                        ev.getID(), ev.getWhen(),
                        ev.getModifiers() | MouseEvent.BUTTON1_MASK,
                        ev.getX(), ev.getY(),
                        ev.getClickCount(), true);
                        
                    // We have to fake that this is a LMB event in
                    // order for the tree to make a selection!
                    super.mousePressed(fakeout);
                    menu.show(ev.getComponent(), ev.getX(), ev.getY());
                }
                else super.mousePressed(ev);
            }
        }
    }//class(NullTreeUI)
//}}}
}//class
