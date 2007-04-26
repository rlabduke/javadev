// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cifless;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.event.*;
import javax.swing.table.*;
//import driftwood.*;
//}}}
/**
* <code>FilteredTableModel</code> is a lightweight wrapper on top of another
* TableModel that allows only certain rows to be displayed.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jul 27 15:07:06 EDT 2006
*/
public class FilteredTableModel extends AbstractTableModel implements TableModelListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    protected TableModel model;
    int[] rowFilter = null; // if set, a list of row indices that are actually used
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FilteredTableModel(TableModel m)
    {
        super();
        this.setModel(m);
    }
//}}}

//{{{ get/setModel, tableChanged
//##############################################################################
    public TableModel getModel()
    { return model; }
    
    public void setModel(TableModel model)
    {
        this.model = model;
        model.addTableModelListener(this);
    }
    
    /** By default forward all events to all the listeners. */
    public void tableChanged(TableModelEvent e)
    { fireTableChanged(e); }
//}}}

//{{{ get/setValueAt, getRow/ColumnCount, getColumnName/Class, isCellEditable
//##############################################################################
    // By default, forward all messages to the underlying model.
    
    public Object getValueAt(int aRow, int aColumn)
    {
        return model.getValueAt(translateFilteredRowIndex(aRow), aColumn);
    }
    
    public void setValueAt(Object aValue, int aRow, int aColumn)
    {
        model.setValueAt(aValue, translateFilteredRowIndex(aRow), aColumn);
    }
    
    public int getRowCount()
    {
        if(model == null) return 0;
        else if(rowFilter == null) return model.getRowCount();
        else return rowFilter.length;
    }
    
    public boolean isCellEditable(int row, int column)
    {
        return model.isCellEditable(translateFilteredRowIndex(row), column);
    }
    
    public int getColumnCount()
    { return (model == null) ? 0 : model.getColumnCount(); }
    
    public String getColumnName(int aColumn)
    { return model.getColumnName(aColumn); }
    
    public Class getColumnClass(int aColumn)
    { return model.getColumnClass(aColumn); }
//}}}

//{{{ translateFilteredRowIndex, unfilter, filterColumnEquals
//##############################################################################
    /**
    * If the document is filtered to show only certain rows,
    * translate the filtered row index to actual row index.
    */
    private int translateFilteredRowIndex(int rowIdx)
    { return (rowFilter == null ? rowIdx : rowFilter[rowIdx]); }
    
    /** Turns off filtering -- shows all rows */
    public void unfilter()
    { this.rowFilter = null; fireTableDataChanged(); }
    
    public void filterColumnEquals(int colIdx, String colValue)
    {
        this.unfilter();
        ArrayList filter = new ArrayList();
        
        int nr = this.getRowCount();
        for(int i = 0; i < nr; i++)
        {
            String val = (String) this.getValueAt(i, colIdx);
            if(val.equals(colValue))
                filter.add(new Integer(i));
        }
        
        this.rowFilter = new int[filter.size()];
        for(int i = 0; i < rowFilter.length; i++)
            rowFilter[i] = ((Integer) filter.get(i)).intValue();
        
        fireTableDataChanged();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

