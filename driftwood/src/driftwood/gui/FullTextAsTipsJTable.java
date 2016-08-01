// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.table.*;
//import driftwood.*;
//}}}
/**
* <code>FullTextAsTipsJTable</code> is necessary because JTable abbreviates long
* items with an ellipsis (...) but doesn't have the sense to show the full text
* when the mouse is hovered over it.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Apr 26 11:36:58 EDT 2007
*/
public class FullTextAsTipsJTable extends JTable
{
//{{{ Constants
//}}}

//{{{ CLASS: FullTextAsTipsJTableHeader
//##############################################################################
    public static class FullTextAsTipsJTableHeader extends JTableHeader
    {
        public FullTextAsTipsJTableHeader(TableColumnModel cm)
        { super(cm); }
        
        public String getToolTipText(MouseEvent e)
        {
            Point p = e.getPoint();
            int index = columnModel.getColumnIndexAtX(p.x);
            //int realIndex = columnModel.getColumn(index).getModelIndex();
            return columnModel.getColumn(index).getHeaderValue().toString();
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FullTextAsTipsJTable(TableModel dm)
    {
        super(dm);
        setTableHeader(new FullTextAsTipsJTableHeader(getColumnModel()));
    }
//}}}

//{{{ getToolTipText
//##############################################################################
    public String getToolTipText(MouseEvent e)
    {
        Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        //int realColumnIndex = convertColumnIndexToModel(colIndex);
        Object val = getValueAt(rowIndex, colIndex);
        if(val == null) return super.getToolTipText(e);
        // need line breaks to be respected:
        else return "<html><pre>"+val.toString();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

