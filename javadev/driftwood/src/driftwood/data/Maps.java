// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.data;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>Maps</code> contains utility functions for working with java.util.Map objects.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct  4 16:10:51 EDT 2005
*/
public class Maps //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ sort, sortByKey, sortByValue
//##############################################################################
    /** Returns the map entries, sorted (c is used directly on the Map.Entry objects). */
    static public Map.Entry[] sort(Map m, Comparator c)
    {
        Map.Entry[] e = (Map.Entry[]) m.entrySet().toArray( new Map.Entry[m.size()] );
        Arrays.sort(e, c);
        return e;
    }

    /** Returns the map entries, sorted by key (c is wrapped in a KeyComparator). */
    static public Map.Entry[] sortByKey(Map m, Comparator c)
    { return sort(m, new KeyComparator(c)); }

    /** Returns the map entries, sorted by value (c is wrapped in a ValueComparator). */
    static public Map.Entry[] sortByValue(Map m, Comparator c)
    { return sort(m, new ValueComparator(c)); }
//}}}

//{{{ increment
//##############################################################################
    /**
    * Assumes that all keys map to Integers, and that keys not present map to 0.
    * Increments the value for the key and returns the new value (ie, pre-increment).
    */
    static public int increment(Map m, Object key)
    {
        Integer value = (Integer) m.get(key);
        int v = 1;
        if(value != null) v = value.intValue() + 1;
        m.put(key, new Integer(v));
        return v;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

