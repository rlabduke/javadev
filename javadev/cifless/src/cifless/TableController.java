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

//{{{ Variable definitions
//##############################################################################
    CifTableModel       tModel;
    FilteredTableModel  filter;
    JFrame              jFrame;
    JTable              jTable;
    JTextArea           textArea;
    JFileChooser        chooser;
    SuffixFileFilter    csvFilter;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public TableController(CifTableModel tModel)
    { this(tModel, null, null); }
    
    public TableController(CifTableModel tModel, String filterColumn, String filterValue)
    {
        super();
        //this.cifFile    = cifFile;
        //this.cifDict    = cifDict;
        this.tModel     = tModel;
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
        buildChooser();
        
        this.jTable = new FullTextAsTipsJTable(filter);
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // or else our columns get squished
        jTable.addMouseListener(this);
        jTable.setShowGrid(true);
        
        this.textArea = new JTextArea(10, 0);
        textArea.setEditable(false);
        
        JMenuBar menubar = new JMenuBar();
        menubar.add(new JMenuItem(new ReflectiveAction("Export CSV", null, this, "onExportCSV")));
        menubar.add(new JMenuItem(new ReflectiveAction("Un-filter", null, this, "onShowAll")));
        
        TablePane2 cp = new TablePane2();
        cp.hfill(true).vfill(true).memorize();
        cp.add(new JScrollPane(jTable));
        cp.newRow();
        cp.add(new JScrollPane(textArea));
        
        this.jFrame = new JFrame(tModel.getTableName());
        jFrame.setDefaultCloseOperation(jFrame.DISPOSE_ON_CLOSE);
        jFrame.setContentPane(cp);
        jFrame.setJMenuBar(menubar);
        jFrame.pack();
        jFrame.show();
    }
//}}}

//{{{ buildChooser
//##############################################################################
    private void buildChooser()
    {
        csvFilter = new SuffixFileFilter("Comma-separated value (CSV)");
        csvFilter.addSuffix(".csv");
        
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(csvFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(csvFilter);
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
    }
//}}}

//{{{ mouseClicked, mousePressed, mouseReleased
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {
        int row = jTable.rowAtPoint(ev.getPoint());
        int col = jTable.columnAtPoint(ev.getPoint());
        if(row != -1 && col != -1)
        {
            col = jTable.convertColumnIndexToModel(col);
            String cifValue = (String) tModel.getValueAt(row, col);
            //System.err.println("Clicked row "+row+", column "+col+" ("+cifName+" = "+cifValue+")");
            textArea.setText(cifValue);
        }
        
        checkForPopupEvent(ev, ev.getClickCount() == 2);
    }
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
            menu.add(new FilterMenuAction(cifName, cifValue));
            //System.err.println(cifName +" -> "+ cifValue);
            
            menu.show(jTable, ev.getX(), ev.getY());
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

//{{{ onExportCSV
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExportCSV(ActionEvent ev)
    {
        // Show the Save dialog
        if(JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(jFrame))
        {
            File f = chooser.getSelectedFile();
            if(!csvFilter.accept(f) &&
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(jFrame,
            "This file has the wrong extension. Append '.csv' to the name?",
            "Fix extension?", JOptionPane.YES_NO_OPTION))
            {
                f = new File(f+".csv");
            }
            
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(jFrame,
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    PrintStream p = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
                    exportTableAsCSV(filter, p);
                    p.close();
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(jFrame,
                        "An I/O error occurred while saving the file:\n"+ex.getMessage(),
                        "Sorry!", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
    }
//}}}

//{{{ exportTableAsCSV
//##############################################################################
    public static void exportTableAsCSV(TableModel t, PrintStream p)
    {
        int cols = t.getColumnCount(), rows = t.getRowCount();
        for(int i = 0; i < cols; i++)
        {
            if(i > 0) p.print(",");
            p.print(encodeForCSV(t.getColumnName(i)));
        }
        p.println();
        
        for(int j = 0; j < rows; j++)
        {
            for(int i = 0; i < cols; i++)
            {
                if(i > 0) p.print(",");
                p.print(encodeForCSV(String.valueOf(t.getValueAt(j, i))));
            }
            p.println();
        }
    }
    
    public static String encodeForCSV(String s)
    {
        boolean needsQuotes = false;
        if(s.indexOf('\n') >= 0)
            needsQuotes = true;
        else if(s.indexOf(',') >= 0)
            needsQuotes = true;
        
        if(s.indexOf('\\') >= 0)
        {
            needsQuotes = true;
            s = s.replace("\\", "\\\\");
        }
        if(s.indexOf('"') >= 0)
        {
            needsQuotes = true;
            s = s.replace("\"", "\\\"");
        }
        
        if(needsQuotes) return "\""+s+"\"";
        else return s;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

