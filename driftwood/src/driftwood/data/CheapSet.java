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
* <code>CheapSet</code> is an open-addressing hashtable Set implementation
* with quadratic probing.
* It DOES NOT allow null, although it does allow a custom hash function.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May 10 14:12:33 EDT 2005
*/
public class CheapSet extends AbstractSet
{
//{{{ Constants
    // Overflow, multiplying negative numbers seems to give same result
    // as doing the calculation with longs and then casting to int.
    //static final long HASH_MULT = 2654435769L; // 0x9e3779b9 -- negative as an int!
    static final int HASH_MULT = 0x9e3779b9;
    
    static final Object DELETED_PROXY = new Object();
//}}}

//{{{ CLASS: CheapIterator
//##############################################################################
class CheapIterator implements Iterator
{
    int     lastEntry = -1;
    int     nextEntry = -1;
    int     expectedNumChanges;
    
    public CheapIterator()
    {
        super();
        this.expectedNumChanges = CheapSet.this.numChanges;
    }
    
    public boolean hasNext()
    {
        checkForMods();
        if(nextEntry == -1) findNext();
        return (nextEntry != -1);
    }
    
    private void findNext()
    {
        Object[] entries = CheapSet.this.setEntries;
        for(nextEntry = lastEntry+1; nextEntry < entries.length; nextEntry++)
        {
            Object entry = entries[nextEntry];
            if(entry != null && entry != DELETED_PROXY) return;
        }
        nextEntry = -1; // failed to find a next entry
    }
    
    public Object next()
    {
        checkForMods();
        if(nextEntry == -1) findNext();
        if(nextEntry == -1)
            throw new NoSuchElementException("No more elements in iteration");
        
        lastEntry = nextEntry;
        nextEntry = -1;
        return CheapSet.this.setEntries[lastEntry];
    }
    
    public void remove()
    {
        checkForMods();
        if(lastEntry != -1)
        {
            CheapSet.this.setEntries[lastEntry] = CheapSet.DELETED_PROXY;
            CheapSet.this.setSize--;
            this.expectedNumChanges = ++CheapSet.this.numChanges;
        }
        else
            throw new NoSuchElementException("No elements have yet been returned by this iterator");
    }
    
    private void checkForMods()
    {
        if(this.expectedNumChanges != CheapSet.this.numChanges)
            throw new ConcurrentModificationException("Backing set was changed during iteration");
    }
}
//}}}

//{{{ Variable definitions
//##############################################################################
    final HashFunction  hashFunc;

    Object[]            setEntries;         // length is always a power of 2
    int                 setSize     = 0;

    final double        loadFactor;
    int                 log2Capacity;       // aka 'p'
    int                 hashMask;           // == (1 << p) - 1;

    int                 numChanges  = 0;    // used for fail-fast iteration
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Default capacity is 16. */
    public CheapSet()
    { this(16); }
    /** Default load factor is 0.75. */
    public CheapSet(int initCapacity)
    { this(initCapacity, 0.75); }
    /** Default hash function is NullNaturalComparator. */
    public CheapSet(int initCapacity, double loadFactor)
    { this(initCapacity, loadFactor, null); }
    public CheapSet(HashFunction hashFunc)
    { this(16, 0.75, null); }
    public CheapSet(int initCapacity, HashFunction hashFunc)
    { this(initCapacity, 0.75, null); }
    
    /**
    * See java.util.HashMap for a discussion of capacity and load factor.
    * Note that in keeping with the semantics of HashMap, the effective
    * maximum size() before rehashing is the capacity divided by the load factor.
    * A null hashFunc will result in NullNaturalComparator being used.
    */
    public CheapSet(int initCapacity, double loadFactor, HashFunction hashFunc)
    {
        super();
        if(hashFunc == null)    this.hashFunc = new NullNaturalComparator();
        else                    this.hashFunc = hashFunc;
        
        if(loadFactor < 0.1)    loadFactor = 0.1;
        if(loadFactor > 1.0)    loadFactor = 1.0;
        this.loadFactor = loadFactor;
        
        for(log2Capacity = 4; (1<<log2Capacity) < initCapacity && log2Capacity < 30; log2Capacity++) {}
        // These invariant relationships must be retained when resizing the table.
        int capacity    = 1 << log2Capacity; // == 2**log2Capacity
        setEntries      = new Object[capacity];
        hashMask        = setEntries.length - 1;
    }
    
    public CheapSet(Collection c)
    {
        this(c.size());
        this.addAll(c);
    }
//}}}

//{{{ index
//##############################################################################    
    /**
    * Converts any integer hash code into an index
    * between zero and mapEntries.length.
    * A secondary hash function is applied at this stage
    * to promote good distribution of hash values,
    * as described in CLR Intro to Algorithms.
    */
    private final int index(int hcode)
    {
        // It's ok if we lose the upper 32 bits.
        hcode = (HASH_MULT * hcode);
        hcode = (hcode >>> (32-log2Capacity)) & hashMask;
        return hcode;
    }
//}}}

//{{{ rehash, ensureCapacity, getLoadFactor
//##############################################################################
    private final void rehash(int newCapacity)
    {
        if(log2Capacity == 30)
            throw new UnsupportedOperationException("Set has reached maximum size");
        
        // Get putAll() to do our dirty work for us
        CheapSet newSet = new CheapSet(newCapacity, loadFactor, hashFunc);
        newSet.addAll(this);
        
        // Canabalize the new set
        this.setEntries     = newSet.setEntries;
        this.setSize        = newSet.setSize;
        this.log2Capacity   = newSet.log2Capacity;
        this.hashMask       = newSet.hashMask;
        
        numChanges++;
    }
    
    /**
    * Ensures that the table has at least newCapacity buckets in it.
    * Note that in keeping with the semantics of HashMap, the effective
    * maximum size() before rehashing is the capacity divided by the load factor.
    */
    public void ensureCapacity(int newCapacity)
    {
        if(newCapacity > setEntries.length)
            rehash(newCapacity);
    }
    
    public double getLoadFactor()
    { return loadFactor; }
//}}}

//{{{ add
//##############################################################################
    public boolean add(Object o)
    {
        if(o == null) throw new NullPointerException("CheapSet cannot contain nulls");
        // Assume we're going to increase the size of the map;
        // i.e., that this key isn't already in the table.
        if(setSize+1 > loadFactor*setEntries.length)
            rehash(setEntries.length * 2);
        
        int hash = index(hashFunc.hashCodeFor(o));
        // For quadratic probing with triangular numbers: 1, 3, 6, 10, 15, 21, 28, ...
        int triNum = 0, triStep = 0;
        while(true)
        {
            triNum = triNum + triStep++;
            int idx = (hash+triNum) & hashMask; // equiv. to % table length
            Object entry = setEntries[idx];
            if(entry == null || entry == DELETED_PROXY)
            {
                setEntries[idx] = o;
                setSize++;
                numChanges++;
                return true;
            }
            else if(hashFunc.areEqual(entry, o)) return false;
        }
    }
//}}}

//{{{ clear
//##############################################################################
    public void clear()
    {
        for(int i = 0; i < setEntries.length; i++)
            setEntries[i] = null;
        setSize = 0;
        numChanges++;
    }
//}}}

//{{{ contains, get
//##############################################################################
    public boolean contains(Object o)
    { return (get(o) != null); }
    
    /**
    * Given an object o that <i>compares as equal</i> to some object present in
    * the set, this function returns the actual object from the set, else it
    * returns null (which isn't a valid set member anyway).
    * This is useful for doing uniquification operations like String.intern().
    */
    public Object get(Object o)
    {
        if(o == null) throw new NullPointerException("CheapSet cannot contain nulls");
        int hash = index(hashFunc.hashCodeFor(o));
        // For quadratic probing with triangular numbers: 1, 3, 6, 10, 15, 21, 28, ...
        int triNum = 0, triStep = 0;
        while(true)
        {
            triNum = triNum + triStep++;
            int idx = (hash+triNum) & hashMask; // equiv. to % table length
            Object entry = setEntries[idx];
            if(entry == null)
                return null; // == false in contains()
            else if(entry == DELETED_PROXY)
                continue;
            else if(hashFunc.areEqual(entry, o))
                return entry; // == true in contains()
        }
    }
//}}}

//{{{ iterator
//##############################################################################
    public Iterator iterator()
    { return new CheapIterator(); }
//}}}

//{{{ remove
//##############################################################################
    public boolean remove(Object o)
    {
        if(o == null) throw new NullPointerException("CheapSet cannot contain nulls");
        int hash = index(hashFunc.hashCodeFor(o));
        // For quadratic probing with triangular numbers: 1, 3, 6, 10, 15, 21, 28, ...
        int triNum = 0, triStep = 0;
        while(true)
        {
            triNum = triNum + triStep++;
            int idx = (hash+triNum) & hashMask; // equiv. to % table length
            Object entry = setEntries[idx];
            if(entry == null)
                return false;
            else if(entry == DELETED_PROXY)
                continue;
            else if(hashFunc.areEqual(entry, o))
            {
                setEntries[idx] = DELETED_PROXY;
                setSize--;
                return true;
            }
        }
    }
//}}}

//{{{ size
//##############################################################################
    public int size()
    { return setSize; }
//}}}

/*{{{ FOR TESTING ONLY: main        * /
//##############################################################################
    public static void main(String[] args)
    {
        CheapSet s = new CheapSet();
        
        // Insertion test
        s.add("apple");
        s.add("applet");
        s.add("orange");
        s.add("lime");
        s.add("eggplant");
        s.add("lemon");
        s.add("black-hole");
        s.add("lettuce");
        s.add("proto-apple");
        
        // Removal / equality test
        CheapSet copy = new CheapSet(s);
        if(!s.equals(copy)) throw new Error("Duplicate sets are not equal");
        int i = 0;
        for(Iterator iter = s.iterator(); iter.hasNext(); )
        {
            iter.next();
            if(++i % 4 == 3) iter.remove();
        }
        System.out.println("Orig = "+copy);
        System.out.println("Mod. = "+s);
        if(s.equals(copy)) throw new Error("Sets with different contents are still equal");
        
        // Rehashing test
        s.ensureCapacity(32);
        
        // Null test
        try {
            s.add(null);
            throw new Error("Set allowed null to be added");
        } catch(NullPointerException ex) {}
        try {
            s.contains(null);
            throw new Error("Set allowed null to be added");
        } catch(NullPointerException ex) {}
        try {
            s.remove(null);
            throw new Error("Set allowed null to be added");
        } catch(NullPointerException ex) {}
        
        // Show set and its entries
        // Iteration test
        System.out.println();
        System.out.println("Set (size="+s.size()+"):");
        System.out.println(s.toString());
    }
/*}}}*/

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

