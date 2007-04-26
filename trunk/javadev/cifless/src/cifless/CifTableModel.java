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
//import javax.swing.*;
import driftwood.data.*;
//}}}
/**
* <code>CifTableModel</code> stores all the CIF items from one group, which is
* equivalent to all the columns for a table in a relational database.
* It implements the TableModel interface so it can be displayed in a JTable.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jul 21 12:24:03 EDT 2006
*/
public class CifTableModel extends javax.swing.table.AbstractTableModel
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String      tableName;
    Map         columns     = new UberMap();
    String[]    colNames    = {};
    List[]      colLists    = {};
    int         numCols     = 0;
    int         numRows     = 0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CifTableModel(String cifGroupName)
    {
        super();
        this.tableName = cifGroupName;
    }
//}}}

//{{{ addItem, getTableName
//##############################################################################
    /**
    * Adds a column (item) to the table.
    * NB: itemData is not copied, just stored as-is.
    */
    public void addItem(String cifItemName, List itemData)
    {
        this.columns.put(cifItemName, itemData);
        this.colNames = (String[]) columns.keySet().toArray(colNames);
        this.numCols = columns.size();
        
        this.colLists = (List[]) columns.values().toArray(colLists);
        numRows = colLists[0].size();
        for(int i = 1; i < numCols; i++) numRows = Math.min(numRows, colLists[i].size());
    }
    
    public String getTableName()
    { return this.tableName; }
//}}}

//{{{ getRow/ColumnCount, getValueAt, getColumnName/Index, getCifColumnName
//##############################################################################
    public int getRowCount()
    { return numRows; }
    
    public int getColumnCount()
    { return numCols; }
    
    public int getColumnIndex(String columnName)
    {
        for(int i = 0; i < this.numCols; i++)
            if(colNames[i].equals(columnName)) return i;
        return -1;
    }
    
    public Object getValueAt(int row, int col)
    { return colLists[col].get(row); }
    
    public String getColumnName(int col)
    { return colNames[col]; }
    
    public String getCifColumnName(int col)
    { return "_"+this.tableName+"."+this.getColumnName(col); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

