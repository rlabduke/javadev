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
 * Note that tree models are implemented by individual kinemages since Java 1.3 doesn't allow
 * an empty (i.e. root == null) tree model.
 *
 * <p>Begun on Sun Jun 30 20:20:49 EDT 2002
 * <br>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
*/
public class KinTree //extends ... implements ...
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

    MutableTreeNode clipboard   = null;     // destination for cut/paste events
    boolean         didEdit     = false;    // did this component initiate the edit?
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public KinTree(KingMain kmain)
    {
        kMain = kmain;
        
        DefaultTreeModel model = kMain.getTreeModel();
        if(model != null) model.reload();
        tree = new JTree(model);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setEditable(false);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        // This will eliminate the annoying menu/tree interactions
        // where Alt-X also selects an item beginning with 'X'
        tree.setUI(new NullTreeUI());
        // Provides feedback on whether things are on or off
        tree.setCellRenderer(new OnOffRenderer());
        
        initActions();
        buildMenu();
        buildGUI();
        
        groupEditor = new GroupEditor(kMain, frame);
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
            acCut.setAccelerator(KeyEvent.VK_X, UIMenus.MENU_ACCEL_MASK);
            acCut.bind(tree);
        acCopy          = new ReflectiveAction("Copy", prefs.copyIcon, this, "onCopy");
            acCopy.setTooltip("Duplicate this object and place it on the clipboard");
            acCopy.setMnemonic(KeyEvent.VK_Y);
            acCopy.setCommandKey("copy");
            acCopy.setAccelerator(KeyEvent.VK_C, UIMenus.MENU_ACCEL_MASK);
            acCopy.bind(tree);
        acPaste         = new ReflectiveAction("Paste", prefs.pasteIcon, this, "onPaste");
            acPaste.setTooltip("Re-insert the last object that was Cut or Copied from the hierarchy");
            acPaste.setMnemonic(KeyEvent.VK_P);
            acPaste.setCommandKey("paste");
            acPaste.setAccelerator(KeyEvent.VK_V, UIMenus.MENU_ACCEL_MASK);
            acPaste.bind(tree);
        acUp            = new ReflectiveAction("Move up", prefs.moveUpIcon, this, "onUp");
            acUp.setTooltip("Move this object toward the top of the list of its peers");
            acUp.setMnemonic(KeyEvent.VK_U);
            acUp.setCommandKey("up");
            acUp.setAccelerator(KeyEvent.VK_UP, UIMenus.MENU_ACCEL_MASK);
            acUp.bind(tree);
        acDown          = new ReflectiveAction("Move down", prefs.moveDownIcon, this, "onDown");
            acDown.setTooltip("Move this object toward the bottom of the list of its peers");
            acDown.setMnemonic(KeyEvent.VK_O);
            acDown.setCommandKey("down");
            acDown.setAccelerator(KeyEvent.VK_DOWN, UIMenus.MENU_ACCEL_MASK);
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
        
        TablePane lBtns = new TablePane();
        lBtns.weights(1,0).center().hfill(true);
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

//{{{ show, hide
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
//}}}

//{{{ notifyChange, getTree
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    void notifyChange(int event_mask)
    {
        int doReload = KingMain.EM_SWITCH | KingMain.EM_CLOSE | KingMain.EM_CLOSEALL | (didEdit ? 0 : KingMain.EM_EDIT_GROSS);
        didEdit = false;
        if((event_mask & doReload) != 0)
        {
            DefaultTreeModel model = kMain.getTreeModel();
            tree.setModel(model);
            if(model != null) model.reload();
        }
        
        // The combination of these two calls ensures that
        // the tree is displaying what the model currently holds.
        tree.treeDidChange();
        tree.revalidate();
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
        kMain.notifyChange(KingMain.EM_ON_OFF);
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
            changed = groupEditor.editGroup((KGroup)node);
        }
        else if(node instanceof KSubgroup)
        {
            changed = groupEditor.editSubgroup((KSubgroup)node);
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
        
        if(changed) 
        {
            didEdit = true;
            kMain.notifyChange(KingMain.EM_ON_OFF | KingMain.EM_EDIT_GROSS);
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
        
        // Unnecessary -- occurs when command is exec'd.
        //kMain.notifyChange(KingMain.EM_EDIT_FINE);
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
        
        MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
        if(node == null) return;
        
        if(node instanceof Kinemage)
        {
            if(JOptionPane.showConfirmDialog(frame, "Really close the current kinemage?",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            { kMain.getStable().closeCurrent(); }
            
            return;
        }
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        if(model != null)
        {
            if(!kMain.prefs.getBoolean("treeConfirmDelete") ||
               JOptionPane.showConfirmDialog(frame, "Really delete the selected item, '"+node.toString()+"'?",
               "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            {
                model.removeNodeFromParent(node);
                Kinemage k = kMain.getKinemage();
                if(k != null) k.setModified(true);
            }
        }
        
        didEdit = true;
        kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }
//}}}

//{{{ onCut, onPaste
//##################################################################################################
    /** Deletes the selected group/subgroup/list from the kinemage, saving it for the next paste command. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCut(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
        if(node == null || node instanceof Kinemage) return;
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        if(model != null)
        {
            if(clipboard == null ||
               JOptionPane.showConfirmDialog(frame, "An unpasted item is still in the clipboard -- discard it?",
               "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            {
                clipboard = node;
                model.removeNodeFromParent(node);
                Kinemage k = kMain.getKinemage();
                if(k != null) k.setModified(true);
            }
        }
        
        didEdit = true;
        kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }

    /** Places the previously cut group/whatever into the currently selected group. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPaste(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
        
        MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
        if(node == null) return;
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        if(model != null && clipboard != null)
        {
            if(node instanceof Kinemage)
            {
                Kinemage kin = (Kinemage)node;
                if(clipboard instanceof KGroup)
                {
                    model.insertNodeInto(clipboard, kin, kin.getChildCount());
                    clipboard.setParent(kin);
                    tree.setSelectionPath(path.pathByAddingChild(clipboard));
                    clipboard = null;
                }
                else if(clipboard instanceof KSubgroup)
                {
                    KGroup group = new KGroup(kin, "New group");
                    model.insertNodeInto(group, node, node.getChildCount());
                    path = path.pathByAddingChild(group);
                    
                    model.insertNodeInto(clipboard, group, group.getChildCount());
                    clipboard.setParent(group);
                    tree.setSelectionPath(path.pathByAddingChild(clipboard));
                    clipboard = null;
                }
                else if(clipboard instanceof KList)
                {
                    KGroup group = new KGroup(kin, "New group");
                    model.insertNodeInto(group, node, node.getChildCount());
                    path = path.pathByAddingChild(group);
                    
                    KSubgroup subgroup = new KSubgroup(group, "New subgroup");
                    model.insertNodeInto(subgroup, group, group.getChildCount());
                    path = path.pathByAddingChild(subgroup);
                    
                    model.insertNodeInto(clipboard, subgroup, subgroup.getChildCount());
                    clipboard.setParent(subgroup);
                    tree.setSelectionPath(path.pathByAddingChild(clipboard));
                    clipboard = null;
                }
                else failPaste();
            }//node is Kinemage
            else if(node instanceof KGroup)
            {
                KGroup group = (KGroup)node;
                
                if(clipboard instanceof KGroup &&
                   (!kMain.prefs.getBoolean("treeConfirmMerge") ||
                   JOptionPane.showConfirmDialog(frame, "Merge these two groups?",
                   "Confirm merge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION))
                {
                    KSubgroup toInsert;
                    for(Iterator iter = ((AGE)clipboard).iterator(); iter.hasNext(); )
                    {
                        toInsert = (KSubgroup)iter.next();
                        model.insertNodeInto(toInsert, group, group.getChildCount());
                        toInsert.setParent(group);
                    }
                    tree.setSelectionPath(path);
                    clipboard = null;
                }
                else if(clipboard instanceof KSubgroup)
                {
                    model.insertNodeInto(clipboard, group, group.getChildCount());
                    clipboard.setParent(group);
                    tree.setSelectionPath(path.pathByAddingChild(clipboard));
                    clipboard = null;
                }
                else if(clipboard instanceof KList)
                {
                    KSubgroup subgroup = new KSubgroup(group, "New subgroup");
                    model.insertNodeInto(subgroup, group, group.getChildCount());
                    path = path.pathByAddingChild(subgroup);
                    
                    model.insertNodeInto(clipboard, subgroup, subgroup.getChildCount());
                    clipboard.setParent(subgroup);
                    tree.setSelectionPath(path.pathByAddingChild(clipboard));
                    clipboard = null;
                }
                else failPaste();
            }//node is KGroup
            else if(node instanceof KSubgroup)
            {
                KSubgroup subgroup = (KSubgroup)node;
                
                if(clipboard instanceof KSubgroup &&
                   (!kMain.prefs.getBoolean("treeConfirmMerge") ||
                   JOptionPane.showConfirmDialog(frame, "Merge these two subgroups?",
                   "Confirm merge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION))
                {
                    KList toInsert;
                    for(Iterator iter = ((AGE)clipboard).iterator(); iter.hasNext(); )
                    {
                        toInsert = (KList)iter.next();
                        model.insertNodeInto(toInsert, subgroup, subgroup.getChildCount());
                        toInsert.setParent(subgroup);
                    }
                    tree.setSelectionPath(path);
                    clipboard = null;
                }
                else if(clipboard instanceof KList)
                {
                    model.insertNodeInto(clipboard, subgroup, subgroup.getChildCount());
                    clipboard.setParent(subgroup);
                    tree.setSelectionPath(path.pathByAddingChild(clipboard));
                    clipboard = null;
                }
                else failPaste();
            }//node is KSubgroup
            else if(node instanceof KList)
            {
                KList list = (KList)node;
                
                if(clipboard instanceof KList)
                {
                    KList toMerge = (KList)clipboard;
                    if(! toMerge.getType().equals(list.getType()) )
                    {
                        JOptionPane.showMessageDialog(frame,
                        "Cannot merge lists: type mismatch.",
                        "Sorry!", JOptionPane.ERROR_MESSAGE);
                    }
                    else if(!kMain.prefs.getBoolean("treeConfirmMerge")
                    || JOptionPane.showConfirmDialog(frame, "Merge these two lists? (Some properties\nof the pasted list may be lost or altered.)",
                       "Confirm merge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
                    {
                        list.children.addAll(toMerge.children);
                        for(Iterator iter = toMerge.iterator(); iter.hasNext(); )
                        {
                            ((KPoint)iter.next()).setOwner(list);
                        }
                        tree.setSelectionPath(path);
                        clipboard = null;
                    }
                }
                else failPaste();
            }//node is KList
            else failPaste();
            
            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
        }
        
        didEdit = true;
        kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }
    
    void failPaste()
    {
            JOptionPane.showMessageDialog(frame,
            "You can't paste that here.\nPut it higher up in the hierarchy.",
            "Sorry!", JOptionPane.ERROR_MESSAGE);
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
        
        MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
        if(node == null || node instanceof Kinemage) return;
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        if(model != null)
        {
            if(clipboard == null ||
               JOptionPane.showConfirmDialog(frame, "An unpasted item is still in the clipboard -- discard it?",
               "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            {
                try
                {
                    clipboard = (MutableTreeNode) ((AGE)node).clone();
                    Kinemage k = kMain.getKinemage();
                    if(k != null) k.setModified(true);
                }
                catch(CloneNotSupportedException ex)    { ex.printStackTrace(SoftLog.err); }
                catch(ClassCastException ex)            { ex.printStackTrace(SoftLog.err); }
            }
        }
        
        didEdit = true;
        kMain.notifyChange(KingMain.EM_EDIT_GROSS);
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
        TreePath parentpath = path.getParentPath();
        if(parentpath == null) return;
        
        MutableTreeNode childnode  = (MutableTreeNode)path.getLastPathComponent();
        MutableTreeNode parentnode = (MutableTreeNode)parentpath.getLastPathComponent();
        if(childnode == null || parentnode == null) return;
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        if(model != null)
        {
            int index = model.getIndexOfChild(parentnode, childnode);
            if(index > 0)
            {
                model.removeNodeFromParent(childnode);
                model.insertNodeInto(childnode, parentnode, index-1);
                tree.setSelectionPath(path);
                Kinemage k = kMain.getKinemage();
                if(k != null) k.setModified(true);
            }            
        }
        
        didEdit = true;
        kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }

    /** Moves the selected group/subgroup/list "down" the list of buttons. */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDown(ActionEvent ev)
    {
        TreePath path = tree.getSelectionPath();
        if(path == null) return;
         TreePath parentpath = path.getParentPath();
        if(parentpath == null) return;
       
        MutableTreeNode childnode  = (MutableTreeNode)path.getLastPathComponent();
        MutableTreeNode parentnode = (MutableTreeNode)parentpath.getLastPathComponent();
        if(childnode == null || parentnode == null) return;
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        if(model != null)
        {
            int index = model.getIndexOfChild(parentnode, childnode);
            if(index < model.getChildCount(parentnode) - 1)
            {
                model.removeNodeFromParent(childnode);
                model.insertNodeInto(childnode, parentnode, index+1);
                tree.setSelectionPath(path);
                Kinemage k = kMain.getKinemage();
                if(k != null) k.setModified(true);
            }            
        }
        
        didEdit = true;
        kMain.notifyChange(KingMain.EM_EDIT_GROSS);
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
        
        MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
        if(node == null || ! (node instanceof AGE)) return;
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        if(model == null) return;
        
        if(node instanceof Kinemage)
        {
            KGroup group = new KGroup((Kinemage)node, "New group");
            model.insertNodeInto(group, node, 0);
            tree.setSelectionPath(path.pathByAddingChild(group));
            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            didEdit = true;
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
        }
        else if(node instanceof KGroup)
        {
            KSubgroup subgroup = new KSubgroup((KGroup)node, "New subgroup");
            model.insertNodeInto(subgroup, node, 0);
            tree.setSelectionPath(path.pathByAddingChild(subgroup));
            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            didEdit = true;
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
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
                TreeNode node = (TreeNode) path.getLastPathComponent();
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
