// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
//import driftwood.*;
//}}}
/**
* <code>KinTreeModel</code> allows one to display a kinemage hierarchy in a
* Swing JTree component.
* If the kinemage structure changes, call kinChanged().
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Dec 21 11:17:14 EST 2006
*/
public class KinTreeModel implements TreeModel
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    protected AGE root;
    protected Collection<TreeModelListener> tmls = new ArrayList<TreeModelListener>();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KinTreeModel(AGE root)
    {
        super();
        this.root = root;
    }
//}}}

//{{{ add/removeTreeModelListener, valueForPathChanged, kinChanged
//##############################################################################
    public void addTreeModelListener(TreeModelListener tml)
    { tmls.add(tml); }
    
    public void removeTreeModelListener(TreeModelListener tml)
    { tmls.remove(tml); }
    
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        TreeModelEvent ev = new TreeModelEvent(this, path);
        for(TreeModelListener tml : tmls)
            tml.treeNodesChanged(ev);
    }
    
    /**
    * Sends a TreeModelEvent that the entire tree structure has (potentially) changed.
    * @param structural if true, the parent-child relationships have changed in
    *   addition to the properties (eg names) of the nodes changing.
    */
    public void kinChanged(boolean structural)
    {
        TreeModelEvent ev = new TreeModelEvent(this, new Object[] { getRoot() });
        for(TreeModelListener tml : tmls)
        {
            if(structural)  tml.treeStructureChanged(ev);
            else            tml.treeNodesChanged(ev);
        }
    }
//}}}

//{{{ getRoot, isLeaf, getChildCount, getChild, getIndexOfChild
//##############################################################################
    public Object getRoot()
    { return this.root; }
    
    public boolean isLeaf(Object node)
    { return (node instanceof KList || ((AGE) node).getChildren().size() == 0); }
    
    public int getChildCount(Object parent)
    {
        if(parent instanceof KList) return 0;
        else return ((AGE) parent).getChildren().size();
    }
    
    public Object getChild(Object parent, int index)
    {
        if(isLeaf(parent)) return null;
        ArrayList children = ((AGE) parent).getChildren();
        if(0 <= index && index < children.size())
            return children.get(index);
        else return null;
    }
    
    public int getIndexOfChild(Object parent, Object child)
    {
        if(parent == null || child == null || isLeaf(parent))
            return -1;
        else return ((AGE) parent).getChildren().indexOf(child);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

