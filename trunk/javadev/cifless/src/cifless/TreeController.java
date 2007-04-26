// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cifless;

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
* <code>TreeController</code> has not yet been documented.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Apr 26 10:33:52 EDT 2007
*/
public class TreeController implements TreeSelectionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    CifFile     cifFile;
    JTree       tree;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public TreeController(CifFile cifFile)
    {
        super();
        this.cifFile = cifFile;
        buildTree();
    }
//}}}

//{{{ buildTree, buildNode
//##############################################################################
    private void buildTree()
    {
        this.tree = new JTree( buildNode(cifFile.getTopBlock()), true );
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setToggleClickCount(1);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
        
        JScrollPane scroll = new JScrollPane(tree);
        JPanel cp = new JPanel(new BorderLayout());
        cp.add(scroll);
        
        JFrame frame = new JFrame("CifLess browser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(cp);
        
        frame.setSize(600, 700);
        //frame.pack();
        frame.setVisible(true);
    }
    
    private DefaultMutableTreeNode buildNode(CifFile.Block block)
    {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
            block.getName(), true);
        for(CifTableModel table : block.getTables().values())
            node.add(new DefaultMutableTreeNode(table, false));
        for(CifFile.Block subblock : block.getSubBlocks().values())
            node.add(buildNode(subblock));
        return node;
    }
//}}}

//{{{ valueChanged
//##############################################################################
    public void valueChanged(TreeSelectionEvent e)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            tree.getLastSelectedPathComponent();
        if (node == null) return;
        
        Object nodeInfo = node.getUserObject();
        if(node.isLeaf() && nodeInfo instanceof CifTableModel)
        {
            CifTableModel tModel = (CifTableModel) nodeInfo;
            new TableController(tModel);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

