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
* <code>DataBlock</code> represents a data_ block from a STAR file.
* This type of data cell is unique in that it can contain other
* data cells (save frames) and can fall back on a global_ block for
* items that it doesn't define internally.
*
* <p><code>DataBlock</code> objects are part of the Document Object Model (DOM)
* defined by a <code>StarFile</code> object.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed May 19 10:11:42 EDT 2004
*/
public class DataBlock extends DataCell
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    DataCell    globalBlock = null;             // may be null
    Map         saveFrames  = new UberMap();    // never null
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DataBlock(String name)
    {
        super(name);
    }
//}}}

//{{{ get/remove/clear/addSaveFrame
//##############################################################################
    /** Retrieves a save frame by name, or null if unknown. */
    public DataCell getSaveFrame(String cellName)
    { return (DataCell) saveFrames.get(cellName); }
    
    /** Retrieves all save frames as an unmodifiable collection. */
    public Collection getSaveFrames()
    { return Collections.unmodifiableCollection(saveFrames.values()); }
    
    /** Removes the named save frame if it exists. */
    public DataCell removeSaveFrame(String cellName)
    { return (DataCell) saveFrames.remove(cellName); }
    
    /** Removes all save frames. */
    public void clearSaveFrames()
    { saveFrames.clear(); }
    
    /** Adds the specified save frame. All save frames must have unique names. */
    public DataCell addSaveFrame(DataCell sf)
    { return (DataCell) saveFrames.put(sf.getName(), sf); }
//}}}

//{{{ getItemNames, getItem, getSingleItem
//##############################################################################
    /**
    * Returns a Set&lt;String&gt; of all item names registered in this cell
    * and in its global block, if any.
    */
    public Set getItemNames()
    {
        if(globalBlock == null) return super.getItemNames();
        
        UberSet names = new UberSet(super.getItemNames());
        names.addAll(globalBlock.getItemNames());
        return Collections.unmodifiableSet(names);
    }
    
    /**
    * Returns a List&lt;String&gt; of the zero or more item values
    * associated with the given item name in this block or the global block.
    * If an item is defined in both places, priority is given to this block.
    * The list will be immutable.
    */
    public List getItem(String itemName)
    {
        List l = super.getItem(itemName);
        if(l.isEmpty() && globalBlock != null)
            l = globalBlock.getItem(itemName);
        return l;
    }
    
    /**
    * Returns the single string value associated with the given key,
    * or null if missing from this data cell and from the global block.
    * @throws ClassCastException if the key maps to a list of values.
    */
    public String getSingleItem(String itemName)
    {
        String s = super.getSingleItem(itemName);
        if(s == null && globalBlock != null)
            s = globalBlock.getSingleItem(itemName);
        return s;
    }
//}}}

//{{{ get/setGlobalBlock
//##############################################################################
    /** Sets the data cell that this one will consult for additional item names. May be null. */
    public void setGlobalBlock(DataCell gb)
    {
        this.globalBlock = gb;
        this.keys.clear(); // b/c it may contains keys from the global block
    }
    
    /** Gets the data cell that this one will consult for additional item names. May be null. */
    public DataCell getGlobalBlock()
    { return globalBlock; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

