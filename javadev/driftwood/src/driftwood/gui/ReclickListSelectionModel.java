// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
//import driftwood.*;
//}}}
/**
* <code>ReclickListSelectionModel</code> has not yet been documented.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 19 11:01:31 EDT 2004
*/
public class ReclickListSelectionModel implements ListSelectionModel
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JList   ourList;
    int     selection = -1; // no selection
    boolean isAdjusting = false;
    List    listeners = new ArrayList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ReclickListSelectionModel(JList list)
    {
        super();
        this.ourList = list;
    }
//}}}

//{{{ methods for ListSelectionModel
//##############################################################################
    public void addListSelectionListener(ListSelectionListener x)
    {
        this.listeners.add(x);
    }

    public void addSelectionInterval(int index0, int index1)
    {
        this.selection = index1;
        fireValueChanged();
    }

    public void clearSelection()
    {
        this.selection = -1;
        fireValueChanged();
    }

    public int getAnchorSelectionIndex()
    {
        return this.selection;
    }

    public int getLeadSelectionIndex()
    {
        return this.selection;
    }

    public int getMaxSelectionIndex()
    {
        return this.selection;
    }

    public int getMinSelectionIndex()
    {
        return this.selection;
    }

    public int getSelectionMode()
    {
        return SINGLE_SELECTION;
    }

    public boolean getValueIsAdjusting()
    {
        return this.isAdjusting;
    }

    public void insertIndexInterval(int index, int length, boolean before)
    {
        // NO_OP
        System.err.println("index="+index+"; length="+length+"; before="+before);
    }

    public boolean isSelectedIndex(int index)
    {
        return (index == this.selection);
    }

    public boolean isSelectionEmpty()
    {
        return (-1 == this.selection);
    }

    public void removeIndexInterval(int index0, int index1)
    {
        if(index0 <= this.selection && this.selection <= index1)
        {
            this.selection = -1;
            fireValueChanged();
        }
    }

    public void removeListSelectionListener(ListSelectionListener x)
    {
        this.listeners.remove(x);
    }

    public void removeSelectionInterval(int index0, int index1)
    {
        if(index0 <= this.selection && this.selection <= index1)
        {
            this.selection = -1;
            fireValueChanged();
        }
    }

    public void setAnchorSelectionIndex(int index)
    {
        this.selection = index;
        fireValueChanged();
    }

    public void setLeadSelectionIndex(int index)
    {
        this.selection = index;
        fireValueChanged();
    }

    public void setSelectionInterval(int index0, int index1)
    {
        this.selection = index1;
        fireValueChanged();
    }

    public void setSelectionMode(int selectionMode)
    {
        if(selectionMode != SINGLE_SELECTION)
            throw new IllegalArgumentException("Only SINGLE_SELECTION is allowed");
    }

    public void setValueIsAdjusting(boolean valueIsAdjusting)
    {
        this.isAdjusting = valueIsAdjusting;
        fireValueChanged();
    }
//}}}

//{{{ fireValueChanged
//##############################################################################
    protected void fireValueChanged()
    {
        ListSelectionEvent ev = new ListSelectionEvent(this.ourList,
            0, this.ourList.getModel().getSize()-1, this.isAdjusting);
        for(Iterator iter = this.listeners.iterator(); iter.hasNext(); )
        {
            ListSelectionListener l = (ListSelectionListener) iter.next();
            l.valueChanged(ev);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

