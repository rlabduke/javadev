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
import javax.swing.table.*;
import driftwood.gui.*;
//}}}
/**
* <code>TableController</code> handles events on a JTable and
* the pop-up menus that it creates.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Jul 22 09:38:33 EDT 2006
*/
public class TableController implements MouseListener
{
//{{{ Constants
//}}}

//{{{ CLASS: CifMenuAction
//##############################################################################
    class CifMenuAction extends AbstractAction
    {
        String cifItem;
        String cifValue;
        
        public CifMenuAction(String cifItem, String cifValue)
        {
            super(cifItem);
            this.cifItem = cifItem;
            this.cifValue = cifValue;
        }
        
        public void actionPerformed(ActionEvent ev)
        {
            String cifTable = cifItem.substring(1, cifItem.indexOf("."));
            String cifColumn = cifItem.substring(cifItem.indexOf(".")+1);

            // Until multi-column keys can be handled, filtering isn't wise:
            //new TableController(cifTable, cifFile, cifDict, cifColumn, cifValue);
            new TableController(cifTable, cifFile, cifDict);
        }
    }
//}}}

//{{{ CLASS: FilterMenuAction
//##############################################################################
    class FilterMenuAction extends AbstractAction
    {
        String cifItem;
        String cifValue;
        
        public FilterMenuAction(String cifItem, String cifValue)
        {
            super("filter");
            this.cifItem = cifItem;
            this.cifValue = cifValue;
        }
        
        public void actionPerformed(ActionEvent ev)
        {
            String cifTable = cifItem.substring(1, cifItem.indexOf("."));
            String cifColumn = cifItem.substring(cifItem.indexOf(".")+1);
            
            int filterIdx = tModel.getColumnIndex(cifColumn);
            if(filterIdx >= 0)
                filter.filterColumnEquals(filterIdx, cifValue);
        }
    }
//}}}

//{{{ CLASS: TableMenuAction
//##############################################################################
    class TableMenuAction extends AbstractAction
    {
        String cifTable;
        
        public TableMenuAction(String cifTable)
        {
            super(cifTable);
            this.cifTable = cifTable;
        }
        
        public void actionPerformed(ActionEvent ev)
        {
            new TableController(this.cifTable, cifFile, cifDict);
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    CifFile             cifFile;
    CifDictionary       cifDict;
    CifTableModel       tModel;
    FilteredTableModel  filter;
    JTable              jTable;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public TableController(String tableName, CifFile cifFile, CifDictionary cifDict)
    { this(tableName, cifFile, cifDict, null, null); }
    
    public TableController(String tableName, CifFile cifFile, CifDictionary cifDict, String filterColumn, String filterValue)
    {
        super();
        this.cifFile    = cifFile;
        this.cifDict    = cifDict;
        this.tModel     = cifFile.getTable(tableName);
        this.filter     = new FilteredTableModel(tModel);
        
        if(filterColumn != null && filterValue != null)
        {
            int filterIdx = tModel.getColumnIndex(filterColumn);
            if(filterIdx >= 0)
                filter.filterColumnEquals(filterIdx, filterValue);
        }
        
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        this.jTable = new JTable(filter);
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // or else our columns get squished
        jTable.addMouseListener(this);
        
        Color linkColor = new Color(0x00, 0x00, 0xcc);
        TableColumnModel colModel = jTable.getColumnModel();
        for(int i = 0; i < jTable.getColumnCount(); i++)
        {
            TableColumn col = colModel.getColumn(i);
            int colIdx = jTable.convertColumnIndexToModel(i);
            String cifName = tModel.getCifColumnName(colIdx);
            if(cifDict.getParent(cifName) != null || cifDict.getChildren(cifName) != null)
            {
                DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
                tcr.setForeground(linkColor);
                col.setCellRenderer(tcr);
            }
        }
        
        JMenuBar menubar = new JMenuBar();
        menubar.add(buildGroupMenus());
        menubar.add(new JMenuItem(new ReflectiveAction("Show all", null, this, "onShowAll")));
        
        JFrame jFrame = new JFrame(tModel.getTableName());
        jFrame.setDefaultCloseOperation(jFrame.DISPOSE_ON_CLOSE);
        jFrame.getContentPane().add(new JScrollPane(jTable));
        jFrame.setJMenuBar(menubar);
        jFrame.pack();
        jFrame.show();
    }
//}}}

//{{{ buildGroupMenus
//##############################################################################
    private JMenu buildGroupMenus()
    {
        JMenu menu = new JMenu("Tables");
        Map groupings = cifDict.getGroupings();
        for(Iterator gi = groupings.entrySet().iterator(); gi.hasNext(); )
        {
            Map.Entry e = (Map.Entry) gi.next();
            JMenu submenu = new JMenu((String) e.getKey());
            menu.add(submenu);
            int count = 0;
            for(Iterator ci = ((Collection) e.getValue()).iterator(); ci.hasNext(); count++)
            {
                if(count > 0 && count % 25 == 0)
                {
                    JMenu newMenu = new JMenu("more ...");
                    submenu.add(newMenu);
                    submenu = newMenu;
                }
                String tableName = (String) ci.next();
                submenu.add(new TableMenuAction(tableName)).setEnabled(this.cifFile.hasTable(tableName));
            }
        }
        return menu;
    }
//}}}

//{{{ mouseClicked, mousePressed, mouseReleased
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    { checkForPopupEvent(ev, ev.getClickCount() == 2); }
    public void mouseEntered(MouseEvent ev)
    {}
    public void mouseExited(MouseEvent ev)
    {}
    public void mousePressed(MouseEvent ev)
    { checkForPopupEvent(ev, false); }
    public void mouseReleased(MouseEvent ev)
    { checkForPopupEvent(ev, false); }
//}}}

//{{{ checkForPopupEvent
//##############################################################################
    private void checkForPopupEvent(MouseEvent ev, boolean doubleClick)
    {
        // Activated by right-click or double-click
        if(!ev.isPopupTrigger() && !doubleClick) return;
        
        int row = jTable.rowAtPoint(ev.getPoint());
        int col = jTable.columnAtPoint(ev.getPoint());
        if(row != -1 && col != -1)
        {
            col = jTable.convertColumnIndexToModel(col);
            String cifName = tModel.getCifColumnName(col);
            String cifValue = (String) tModel.getValueAt(row, col);
            //System.err.println("Clicked row "+row+", column "+col+" ("+cifName+" = "+cifValue+")");
            
            JPopupMenu menu = new JPopupMenu();
            String parent = cifDict.getParent(cifName);
            if(parent != null)
            {
                // Our CIF file should *always* have the parent ... I think ... well, just in case.
                menu.add(new CifMenuAction(parent, cifValue)).setEnabled(this.cifFile.hasItem(parent));
                Collection siblings = new ArrayList(cifDict.getChildren(parent));
                if(siblings != null)
                {
                    siblings.remove(cifName); // don't include us
                    menu.addSeparator();
                    menu.add("siblings ("+siblings.size()+"):").setEnabled(false);
                    addMenuItems(menu, siblings, cifValue);
                }
            }
            Collection children = cifDict.getChildren(cifName);
            if(children != null)
            {
                if(parent != null) menu.addSeparator();
                menu.add("children ("+children.size()+"):").setEnabled(false);
                addMenuItems(menu, children, cifValue);
            }
            // For filtering this table:
            if(parent != null || children != null)
                menu.addSeparator();
            menu.add(new FilterMenuAction(cifName, cifValue));
            //System.err.println(cifName +" -> "+ cifValue);
            
            menu.show(jTable, ev.getX(), ev.getY());
        }
    }
//}}}

//{{{ addMenuItems
//##############################################################################
    /** Creates cascading menus of child / sibling items, because some have 40+ items! */
    private void addMenuItems(JPopupMenu menu, Collection cItems, String cifValue)
    {
        int itemCount;
        ArrayList items = new ArrayList(cItems);
        Collections.sort(items);
        Iterator iter = items.iterator();
        for(itemCount = 0; iter.hasNext() && itemCount < 10; itemCount++)
        {
            String itemName = (String) iter.next();
            menu.add(new CifMenuAction(itemName, cifValue)).setEnabled(this.cifFile.hasItem(itemName));
        }
        
        if(iter.hasNext())
        {
            JMenu submenu = new JMenu("more ...");
            menu.add(submenu);
            for(itemCount = 0; iter.hasNext(); itemCount++)
            {
                if(itemCount > 0 && itemCount % 25 == 0)
                {
                    JMenu newsub = new JMenu("more ...");
                    submenu.add(newsub);
                    submenu = newsub;
                }
                String itemName = (String) iter.next();
                submenu.add(new CifMenuAction(itemName, cifValue)).setEnabled(this.cifFile.hasItem(itemName));
            }
        }
    }
//}}}

//{{{ onShowAll
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowAll(ActionEvent ev)
    {
        this.filter.unfilter();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

