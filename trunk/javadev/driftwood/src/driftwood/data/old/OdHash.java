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
import driftwood.r3.Triple;
//}}}
/**
* <code>OdHash</code> is a fast hash table that maps Objects to doubles.
* All the data are stored in large arrays rather than one object per key,
* so you pay the allocation cost up front, but when the table's full or
* the values are updated frequently, you come out ahead.
* Note, however, that these tables are particularly wasteful of memory
* if the initial allocation is too large.
* <p>Arrays are reallocated every time a rehashing is necessary, and only then.
* Hash collisions are resolved by chaining.
* The hash function is the multiplicative one proposed by Knuth,
* as cited in "Introduction to Algorithms" by Cormen, Leiserson, Rivest, and Stein.
* Thus w = 32 bits (the size of a Java int)
* and s = 2654435769 ~= 2**w * (sqrt(5)-1)/2.
* <p>This class is not thread-safe.
*
* <p>Speed comparison produced by main():
<pre>
No operation: 53 ms
Array access & assignment: 119 ms
My hash table: 11782 ms
Java hash table: 29141 ms
</pre>
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Sep 18 12:01:20 EDT 2003
*/
public class OdHash //extends ... implements ...
{
//{{{ Constants
    static final long hashMultiplier = 2654435769L; // 0x9e3779b9 -- negative as an int!
    static final Object nullKeyProxy = new Object();
//}}}

//{{{ Variable definitions
//##############################################################################
    int         log2Capacity;   // aka 'p'
    int         hashmask;   // = (1 << p) - 1;
    int[]       hashtable;  //  hash table: size = 2**p
    Object[]    keys;       // linked list: size = loadFactor * 2**p
    double[]    values;
    int[]       chain;      // index of chained entry or -1 end of chain
    
    float       loadFactor;
    int         numEntries;
    int         firstFree;  // = -1 when linked list is full
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Analogous to the java.util.HashMap constructor. */
    public OdHash(int initCapacity)
    { this(initCapacity, 0.75f); }
    
    /** Analogous to the java.util.HashMap constructor. */
    public OdHash(int initCapacity, float loadFact)
    {
        super();
        // Minimum reasonable values
        if(initCapacity < 8)        initCapacity = 8;
        if(loadFact     < 0.25f)    loadFact = 0.25f;
        
        loadFactor = loadFact;
        
        // Determine starting capacity
        log2Capacity = 1;
        int capacity = 1 << log2Capacity;
        while(capacity < initCapacity) capacity = 1 << (++log2Capacity);
        hashmask = capacity - 1;
        
        // Allocate storage
        int maxUsed = (int)(capacity*loadFactor + 1);
        hashtable   = new int[capacity];
        keys        = new Object[maxUsed];
        values      = new double[maxUsed];
        chain       = new int[maxUsed];
        
        clear();
    }
//}}}

//{{{ clear, size, isEmpty
//##############################################################################
    /** Remove all mappings from the hashtable. No memory is freed. */
    public void clear()
    {
        int i, len;
        numEntries = 0;
        
        // Clear out hashtable
        len = hashtable.length;
        for(i = 0; i < len; i++) hashtable[i] = -1;
        
        // Set up linked list of free spaces
        firstFree = 0;
        len = keys.length;
        for(i = 0; i < len; i++)
        {
            keys[i]     = null; // so it can be garbage collected
            chain[i]    = i+1;
        }
        chain[len-1] = -1; // marks list as full
    }
    
    public int size()
    { return numEntries; }
    
    public int capacity()
    { return hashtable.length; }
    
    public boolean isEmpty()
    { return (numEntries == 0); }
//}}}

//{{{ put
//##############################################################################
    public void put(Object k, double v)
    {
        if(k == null) k = nullKeyProxy;
        
        // Hash code. It's ok if we lose the upper 32 bits.
        int kHash       = k.hashCode();
        int hashcode    = (int)(hashMultiplier * kHash);
        hashcode        = (hashcode >>> (32-log2Capacity)) & hashmask;
        
        // Find insertion point
        int idx = indexOf(k, hashcode);
        if(idx != -1) // Found our key with a different value
        {
            values[idx] = v;
        }
        else // Need to create a new entry
        {
            if(checkCapacity())
            {
                // Recompute hash code since table has grown
                hashcode    = (int)(hashMultiplier * kHash);
                hashcode    = (hashcode >>> (32-log2Capacity)) & hashmask;
            }
            numEntries++;
            idx = firstFree;            // we'll use the first free spot in the list
            firstFree = chain[idx];     // step to next free spot for next time
            keys[idx]           = k;
            values[idx]         = v;
            // this is the new front of the chain
            chain[idx]          = hashtable[hashcode];
            hashtable[hashcode] = idx;
        }
    }
//}}}

//{{{ checkCapacity
//##############################################################################
    /** Rehashes the table if we're full. Returns true iff a rehash was done. */
    protected boolean checkCapacity()
    {
        if(firstFree == -1)
        {
            // Create a new table, twice as big
            int newHashSize = (1 << (++log2Capacity));
            OdHash newHash = new OdHash(newHashSize, loadFactor);
            
            // Iterate through entries and add to new table
            for(int i = 0; i < hashtable.length; i++)
            {
                int j = hashtable[i];
                while(j != -1)
                {
                    newHash.put(keys[j], values[j]);
                    j = chain[j];
                }
            }
            
            // Harvest the data from the new table
            this.log2Capacity   = newHash.log2Capacity;
            this.hashmask       = newHash.hashmask;
            this.hashtable      = newHash.hashtable;
            this.keys           = newHash.keys;
            this.values         = newHash.values;
            this.chain          = newHash.chain;
            this.firstFree      = newHash.firstFree;
            return true;
        }
        else return false;
    }
//}}}

//{{{ containsKey, get
//##############################################################################
    public boolean containsKey(Object k)
    {
        return (indexOf(k) != -1);
    }
    
    /**
    * Returns the double associated with key k.
    * @throws NoSuchElementException if containsKey(k) == false
    */
    public double get(Object k)
    {
        int idx = indexOf(k);
        if(idx == -1)
            throw new NoSuchElementException("'"+k+"' is not a key in this hash");
        return values[idx];
    }

    public double get(Object k, double defaultVal)
    {
        int idx = indexOf(k);
        if(idx == -1)
            return defaultVal;
        return values[idx];
    }
//}}}

//{{{ indexOf
//##############################################################################
    /** Returns index of key k or -1 if not found */
    protected int indexOf(Object k)
    {
        if(k == null) k = nullKeyProxy;
        
        // Hash code. It's ok if we lose the upper 32 bits.
        int hashcode = (int)(hashMultiplier * k.hashCode());
        hashcode = (hashcode >>> (32-log2Capacity)) & hashmask;
        
        return indexOf(k, hashcode);
    }

    /** Requires k != null and hashcode to be correct */
    protected int indexOf(Object k, int hashcode)
    {
        // Find insertion point
        int idx = hashtable[hashcode];
        while(true)
        {
            if(idx == -1) // Can't be true for first chain step
                return -1;
            else if(keys[idx].equals(k))
                return idx;
            idx = chain[idx];
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ maxChaining
//##############################################################################
    /** A nice diagnostic if things are slow. */
    public int maxChaining()
    {
        int i, j, step, maxStep = 0;
        // Iterate through entries
        for(i = 0; i < hashtable.length; i++)
        {
            j = hashtable[i];
            for(step = 0; j != -1; step++) j = chain[j];
            if(step > maxStep) maxStep = step;
        }
        return maxStep;
    }
//}}}

/** /
//{{{ main (for testing)
//##############################################################################
    static public void main(String[] args)
    {
        OdHash h = new OdHash(20);
        
        int i;
        final int num = 10000;
        Object[] keys = new Object[num];
        double[] vals = new double[num];
        
        // Test once as we build the array
        for(i = 0; i < num; i++)
        {
            if(i%2==0)
                keys[i] = new Object();
            else
                keys[i] = new Triple(Math.random(), Math.random(), Math.random());
            
            vals[i] = Math.random();
            
            h.put(keys[i], vals[i]);
            if(h.get(keys[i]) != vals[i])
                throw new Error("Test failed during setup!");
        }
        
        // Test again after rehashing
        for(i = 0; i < num; i++)
        {
            h.put(keys[i], vals[i]);
            if(h.get(keys[i]) != vals[i])
                throw new Error("Test failed after setup!");
        }
        
        // Test null
        h.put(null, Math.E);
        if(h.get(null) != Math.E)
            throw new Error("Test failed with null key!");
        
        // Test missing key
        try
        {
            h.get(new Object());
            throw new Error("Test failed with missing key!");
        }
        catch(NoSuchElementException ex) {} // this *should* happen
        
        echo("You should NOT see a list after this:");
        h.clear();
        h.dumpEntries();

        // Test removed key
        try
        {
            h.get(keys[0]);
            throw new Error("Test failed with removed key!");
        }
        catch(NoSuchElementException ex) {} // this *should* happen
        
        // Timing tests
        int loop, maxLoop = 1000;
        long noop, access, mine, java;
        Object k;
        double v;
        Double d;
        
        noop = System.currentTimeMillis();
        for(loop = 0; loop < maxLoop; loop++)
        {
            for(i = 0; i < num; i++)
            {
            }
        }
        noop = System.currentTimeMillis() - noop;
        echo("No operation: "+noop+" ms");

        access = System.currentTimeMillis();
        for(loop = 0; loop < maxLoop; loop++)
        {
            for(i = 0; i < num; i++)
            {
                k = keys[i];
                v = vals[i];
            }
        }
        access = System.currentTimeMillis() - access;
        echo("Array access & assignment: "+access+" ms");

        mine = System.currentTimeMillis();
        for(loop = 0; loop < maxLoop; loop++)
        {
            h.clear();
            for(i = 0; i < num; i++)
            {
                h.put(keys[i], vals[i]);
                v = h.get(keys[i]);
            }
        }
        mine = System.currentTimeMillis() - mine;
        echo("My hash table: "+mine+" ms");

        HashMap map = new HashMap();
        java = System.currentTimeMillis();
        for(loop = 0; loop < maxLoop; loop++)
        {
            map.clear();
            for(i = 0; i < num; i++)
            {
                map.put(keys[i], new Double(vals[i]));
                d = (Double)map.get(keys[i]);
            }
        }
        java = System.currentTimeMillis() - java;
        echo("Java hash table: "+java+" ms");

        echo("Test passed.");
    }
    
    static void echo(String s)
    { SoftLog.err.println(s); }

    private void dumpEntries()
    {
        // Iterate through entries
        for(int i = 0; i < hashtable.length; i++)
        {
            int step = 0;
            int j = hashtable[i];
            while(j != -1)
            {
                echo("  ("+keys[j]+", "+values[j]+") @ bucket "+i+", step "+step+", index "+j);
                j = chain[j];
                step++;
            }
        }
    }
//}}}
/**/

}//class

