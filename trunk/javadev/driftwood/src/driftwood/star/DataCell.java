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
* Care is taken to ensure all Lists stored in a data cell are immutable,
* which is required to guarantee the correctness of the database table primary keys.
*
* <p><code>DataCell</code> objects are part of the Document Object Model (DOM)
* defined by a <code>StarFile</code> object.
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

//{{{ toString, getName, clear, removeItem
//##############################################################################
    /** Returns the name of this data cell. */
    public String toString()
    { return getName(); }
    
    /** Returns the name of this data cell. */
    public String getName()
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
    * The list will be immutable.
    */
    public List getItem(String itemName)
    {
        Object o = items.get(itemName);
        if(o == null)                   return Collections.EMPTY_LIST;
        else if(o instanceof String)    return Collections.singletonList(o);
        else                            return (List) o;
    }
    
    /**
    * Returns the single string value associated with the given key,
    * or null if missing from this data cell.
    * @throws ClassCastException if the key maps to a list of values.
    */
    public String getSingleItem(String itemName)
    {
        return (String) items.get(itemName);
    }
//}}}

//{{{ getKey
//##############################################################################
    /**
    * Data in STAR files is often structured into the logical equivalent
    * of tables in a relational database.
    * Thus, some columns (data name -&gt; list of data values) act as
    * primary keys.
    * This function returns a Map&lt;String value, Integer index&gt; to use for
    * looking up table row indices based on the value of that row's key.
    * The generated keys are cached unless the key column changes.
    * Returns an empty map if the item name is unknown.
    * @throws IllegalArgumentException if the named column contains
    *   duplicate values.
    */
    public Map getKey(String itemName)
    {
        Map key = (Map) keys.get(itemName);
        if(key != null) return key;
        
        int i = 0;
        key = new HashMap();
        List data = getItem(itemName);
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            Object datum = iter.next();
            if(key.put(datum, new Integer(i++)) != null)
                throw new IllegalArgumentException("Column "+itemName+" contains duplicate value: "+datum);
        }
        
        Map unmodKey = Collections.unmodifiableMap(key);
        keys.put(itemName, unmodKey);
        return unmodKey;
    }
//}}}

//{{{ putItem, putItemAfter
//##############################################################################
    /** Replaces the previous item of the same name or adds to the end of the list. */
    public void putItem(String itemName, String itemValue)
    {
        keys.remove(itemName);
        items.put(itemName, itemValue);
    }

    /** Replaces the previous item of the same name or adds to the end of the list. */
    public void putItem(String itemName, List itemValues)
    {
        keys.remove(itemName);
        String[] data = (String[]) itemValues.toArray(new String[itemValues.size()]);
        items.put(itemName, new FinalArrayList(data));
    }
    
    /** Inserts the new item after the reference item or at the end of the list. */
    public void putItemAfter(String refItem, String itemName, String itemValue)
    {
        keys.remove(itemName);
        items.putAfter(refItem, itemName, itemValue);
    }

    /** Inserts the new item after the reference item or at the end of the list. */
    public void putItemAfter(String refItem, String itemName, List itemValues)
    {
        keys.remove(itemName);
        String[] data = (String[]) itemValues.toArray(new String[itemValues.size()]);
        items.putAfter(refItem, itemName, new FinalArrayList(data));
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

