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
* <code>TinyMap</code> implements a very space-efficient map with up to 32 integer keys (0 - 31).
* Adding new keys and removing old ones are linear time operations and force object allocation.
* Retrieving values for existing keys is a constant time operation, on the other hand.
*
* <p>I expect this code will frequently be subclassed and/or copy-and-pasted into other classes,
* to avoid the storage overhead of allocating the TinyMap object itself.
*
* <p>I haven't bothered to implement equals(), hashCode(), or other Map methods.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jun  8 16:02:27 EDT 2006
*/
public class TinyMap //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    int         keyMap      = 0;    // bits set for keys that have values
    Object[]    values      = null; // first value corresponds to lowest set key bit, etc
//}}}

//{{{ Constructor(s)
//##############################################################################
    public TinyMap()
    {
        super();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ countSetBitsNaive, countSetBitsKWL
//##############################################################################
    /**
    * Counts the number of set bits (1's) in v.
    * Naive implementation for verifying correctness of others (32 cycles).
    * From "Bit Twiddling Hacks", http://graphics.stanford.edu/~seander/bithacks.html
    */
    //static public int countSetBitsNaive(int v)
    //{
    //    int c; // accumulates number of bits set in v
    //    for(c = 0; v != 0; v >>>= 1)
    //    {
    //        c += v & 1;
    //    }
    //    return c;
    //}

    /**
    * Counts the number of set bits (1's) in v.
    * Semi-optimized implementation (one cycle per set bit).
    * Attributed to Brian Kernighan / Peter Wegner / Derrick Lehmer.
    * From "Bit Twiddling Hacks", http://graphics.stanford.edu/~seander/bithacks.html
    */
    //static public int countSetBitsKWL(int v)
    //{
    //    int c; // accumulates number of bits set in v
    //    for(c = 0; v != 0; c++)
    //    {
    //        v &= v - 1; // clear the least significant bit set
    //    }
    //    return c;
    //}
//}}}

//{{{ countSetBits
//##############################################################################
    /**
    * Counts the number of set bits (1's) in v.
    * Optimal implementation with ~12 operations.
    * From "Bit Twiddling Hacks", http://graphics.stanford.edu/~seander/bithacks.html
    */
    static public int countSetBits(int v)
    {
        int w = v - ((v >>> 1) & 0x55555555);                       // temp
        int x = (w & 0x33333333) + ((w >>> 2) & 0x33333333);        // temp
        int c = ((x + (x >>> 4) & 0xF0F0F0F) * 0x1010101) >>> 24;   // count
        return c;
    }
//}}}

//{{{ indexOf, contains, size
//##############################################################################
    /**
    * Returns the array index (0 based) for the item with the given key (0 - 31).
    * If the item is not actually in the array, it returns where it *would* be,
    * if it were inserted.
    * @param key            the unique integer index for the item (0 - 31)
    * @param occupancyMap   has bits set for items that are actually present
    */
    public static int indexOf(int key, int occupancyMap)
    {
        int mask = (1 << key) - 1; // 1's for bit indices below this one
        return countSetBits(occupancyMap & mask);
    }
    public int indexOf(int key) { return indexOf(key, this.keyMap); }
    
    /** Whether the occupancy map indicates that the given item is present. */
    public static boolean contains(int key, int occupancyMap)
    {
        int bit = (1 << key); // 1 for the this index's bit
        return (bit & occupancyMap) != 0;
    }
    public boolean contains(int key) { return contains(key, this.keyMap); }
    
    /** Number of actual entries in this map. */
    public static int size(int occupancyMap)
    {
        return countSetBits(occupancyMap);
    }
    public int size() { return size(this.keyMap); } // not values.length, b/c values may be null
//}}}

//{{{ get, put, remove
//##############################################################################
    /**
    * Returns the value associated with the given key,
    * or null if this map does not contain that key.
    */
    public Object get(int key)
    {
        if(!contains(key, keyMap)) return null;
        else return values[indexOf(key, keyMap)];
    }
    
    /**
    * Associates a new value with key and returns the old value,
    * or null if none was set.
    */
    public Object put(int key, Object value)
    {
        int i = indexOf(key, keyMap);
        if(contains(key, keyMap))
        {
            Object old = values[i];
            values[i] = value;
            return old;
        }
        else
        {
            int values_length = size(keyMap); //values may be null!
            keyMap |= (1 << key);
            Object[] newvals = new Object[values_length+1];
            for(int j = 0; j < i; j++) newvals[j] = values[j];
            newvals[i] = value;
            for(int j = i; j < values_length; j++) newvals[j+1] = values[j];
            values = newvals;
            return null;
        }
    }
    
    /** Removes the value for the given key, if present. */
    public Object remove(int key)
    {
        if(!contains(key, keyMap)) return null;
        
        int i = indexOf(key, keyMap);
        Object old = values[i];
        
        keyMap &= ~(1 << key);
        Object[] newvals = new Object[values.length-1];
        for(int j = 0; j < i; j++) newvals[j] = values[j];
        for(int j = i+1; j < values.length; j++) newvals[j-1] = values[j];
        values = newvals;
        if(values.length == 0) values = null; // just to save space
        return old;
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("[").append(size()).append(" items");
        for(int k = 0; k < 32; k++)
        {
            if(contains(k))
                buf.append(", ").append(k).append(":").append(get(k));
        }
        buf.append("]");
        return buf.toString();
    }
//}}}

//{{{ main (for testing)
//##############################################################################
    /** /
    public static void main(String[] args)
    {
        //Random r = new Random();
        //for(int i = 0; i < 10000; i++)
        //{
        //    int v = r.nextInt();
        //    int c = countSetBitsNaive(v);
        //    int cKWL = countSetBitsKWL(v);
        //    int cOpt = countSetBits(v);
        //    //System.out.println(Integer.toBinaryString(v)+"    "+c+"    "+cKWL+"    "+cOpt);
        //    if(c < 0 || c > 32) System.out.println("*** Naive method failed! v = "+Integer.toBinaryString(v));
        //    if(c != cKWL)       System.out.println("*** KWL method failed! v = "+Integer.toBinaryString(v));
        //    if(c != cOpt)       System.out.println("*** Opt method failed! v = "+Integer.toBinaryString(v));
        //}
        
        TinyMap map = new TinyMap();    System.out.println(map);
        map.put(0, "zero");             System.out.println(map);
        map.put(1, "one");              System.out.println(map);
        map.put(5, "five");             System.out.println(map);
        map.put(15, "fifteen");         System.out.println(map);
        map.put(10, "ten");             System.out.println(map);
        map.remove(5);                  System.out.println(map);
        map.remove(6);                  System.out.println(map);
        map.put(15, "one-five");        System.out.println(map);
        map.put(15, null);              System.out.println(map);
        map.remove(0); map.remove(1); map.remove(15); map.remove(10); System.out.println(map);
        map.put(1, "one");              System.out.println(map);
        System.out.println("2:"+map.get(2));
    }
    /**/
//}}}
}//class

