// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.star;

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
* <code>DataCell</code> represents a grouping of data items in a STAR file:
* a data block, a global block, or a save frame.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun May 16 14:31:25 EDT 2004
*/
public class DataCell //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    UberMap     items;  // contains Strings and List<String>
    Map         keys;   // cached primary key lookup tables
    String      name;   // the name of this data cell
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DataCell(String name)
    {
        super();
        this.name = name;
        this.items = new UberMap();
        this.keys = new HashMap();
    }
//}}}

//{{{ toString, clear, removeItem
//##############################################################################
    /** Returns the name of this data cell. */
    public String toString()
    { return name; }
    
    /** Removes all data items from this cell. */
    public void clear()
    {
        items.clear();
        keys.clear();
    }
    
    /** Removes the named data item if it is present. */
    public Object removeItem(String itemName)
    {
        keys.remove(itemName);
        return items.remove(itemName);
    }
//}}}

//{{{ getItemNames, getItem, getSingleItem
//##############################################################################
    /** Returns a Set&lt;String&gt; of all item names registered in this cell. */
    public Set getItemNames()
    { return Collections.unmodifiableSet(items.keySet()); }
    
    /**
    * Returns a List&lt;String&gt; of the zero or more item values
    * associated with the given item name.
    */
    public List getItem(String itemName)
    {
        return null;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

