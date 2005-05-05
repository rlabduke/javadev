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
* <code>UberMap</code> is a replacement for java.util.LinkedHashMap
* with additional features to make it a truly multi-purpose data structure.
* Functions that are part of the Map specification adhere to the
* contracts documented in the official Sun javadocs; see them for details.
*
* <p>Hash table length is always a power of two, and
* hash collisions are resolved by chaining.
* The secondary hash function is the multiplicative one proposed by Knuth,
* as cited in "Introduction to Algorithms" by Cormen, Leiserson, Rivest, and Stein.
* Thus w = 32 bits (the size of a Java int)
* and s = 2654435769 ~= 2**w * (sqrt(5)-1)/2.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb  3 08:41:42 EST 2004
*/
public class UberMap extends AbstractMap
{
//{{{ Constants
    // Overflow, multiplying negative numbers seems to give same result
    // as doing the calculation with longs and then casting to int.
    //static final long HASH_MULT = 2654435769L; // 0x9e3779b9 -- negative as an int!
    static final int HASH_MULT = 0x9e3779b9;
//}}}

//{{{ CLASS: UberEntry
//##############################################################################
static class UberEntry implements Map.Entry
{
    final Object    key;
    final int       hashCode;
    Object          value;
    UberEntry       before  = null;
    UberEntry       after   = null;
    UberEntry       chain   = null;
    
    public UberEntry(Object k, Object v, int h)
    {
        this.key        = k;
        this.value      = v;
        this.hashCode   = h;
    }
    
    public boolean equals(Object o)
    {
        if(o == null) return false;
        if(!(o instanceof Map.Entry)) return false;
        
        Map.Entry e = (Map.Entry) o;
        return (this.key==null ? e.getKey()==null : this.key.equals(e.getKey()))
            && (this.value==null ? e.getValue()==null : this.value.equals(e.getValue()));
    }
    
    /**
    * Falls back to the native hashCode() for key to ensure we obey the equals() contract.
    * FIXME: This is pathological (always 0) if key == value, as is the case for Sets.
    */
    public int hashCode()
    {
        return (this.key==null ? 0 : this.key.hashCode())
            ^ (this.value==null ? 0 : this.value.hashCode());
    }

    public Object getKey() { return key; }
    public Object getValue() { return value; }
    public Object setValue(Object newVal)
    {
        Object old = value;
        value = newVal;
        return old;
    }
    
    public String toString()
    { return key+" -> "+value+" @ "+hashCode; }
}
//}}}

//{{{ CLASS: UberEntrySet
//##############################################################################
class UberEntrySet extends AbstractSet
{
    public UberEntrySet()
    { super(); }
    
    public void clear()
    { UberMap.this.clear(); }
    
    public boolean contains(Object o)
    {
        if(!(o instanceof Map.Entry)) return false;
        Map.Entry e = (Map.Entry) o;
        return e.equals(UberMap.this.fetchEntry(e.getKey()));
    }
    
    public Iterator iterator()
    { return new UberEntryIterator(); }
    
    public boolean remove(Object o)
    {
        if(!(o instanceof Map.Entry)) return false;
        Map.Entry e = (Map.Entry) o;
        UberEntry u = UberMap.this.fetchEntry(e.getKey());
        if(e.equals(u))
        {
            UberMap.this.remove(e.getKey());
            return true;
        }
        else return false;
    }
    
    public int size()
    { return UberMap.this.size(); }
}
//}}}

//{{{ CLASS: UberEntryIterator
//##############################################################################
class UberEntryIterator implements Iterator
{
    UberEntry   prevEntry = null;
    UberEntry   nextEntry;
    int         expectedNumChanges;
    
    public UberEntryIterator()
    {
        super();
        this.expectedNumChanges = UberMap.this.numChanges;
        this.nextEntry = UberMap.this.mapHead;
    }
    
    public boolean hasNext()
    {
        checkForMods();
        return (nextEntry != null);
    }
    
    public Object next()
    {
        checkForMods();
        if(nextEntry == null)
            throw new NoSuchElementException("No more elements in iteration");
        
        prevEntry   = nextEntry;
        nextEntry   = nextEntry.after;
        return prevEntry;
    }
    
    public void remove()
    {
        checkForMods();
        if(prevEntry != null)
        {
            UberMap.this.remove(prevEntry.getKey());
            this.expectedNumChanges = UberMap.this.numChanges;
        }
        else
            throw new NoSuchElementException("No elements have yet been returned by this iterator");
    }
    
    private void checkForMods()
    {
        if(this.expectedNumChanges != UberMap.this.numChanges)
            throw new ConcurrentModificationException("Backing map was changed during iteration");
    }
}
//}}}

//{{{ Variable definitions
//##############################################################################
    final HashFunction  hashFunc;

    UberEntry[]         mapEntries;         // length is always a power of 2
    UberEntry           mapHead     = null;
    UberEntry           mapTail     = null;
    int                 mapSize     = 0;

    final double        loadFactor;
    int                 log2Capacity;       // aka 'p'
    int                 hashMask;           // == (1 << p) - 1;

    UberEntrySet        mapEntrySet = null;
    int                 numChanges  = 0;    // used for fail-fast iteration
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Default capacity is 16. */
    public UberMap()
    { this(16); }
    /** Default load factor is 0.75. */
    public UberMap(int initCapacity)
    { this(initCapacity, 0.75); }
    /** Default hash function is NullNaturalComparator. */
    public UberMap(int initCapacity, double loadFactor)
    { this(initCapacity, loadFactor, null); }
    public UberMap(HashFunction hashFunc)
    { this(16, 0.75, null); }
    public UberMap(int initCapacity, HashFunction hashFunc)
    { this(initCapacity, 0.75, null); }
    
    /**
    * See java.util.HashMap for a discussion of capacity and load factor.
    * Note that in keeping with the semantics of HashMap, the effective
    * maximum size() before rehashing is the capacity divided by the load factor.
    * A null hashFunc will result in NullNaturalComparator being used.
    */
    public UberMap(int initCapacity, double loadFactor, HashFunction hashFunc)
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
        mapEntries      = new UberEntry[capacity];
        hashMask        = mapEntries.length - 1;
    }
    
    public UberMap(Map map)
    {
        this(map.size());
        this.putAll(map);
    }
//}}}

//{{{ entrySet
//##############################################################################
    public Set entrySet()
    {
        if(mapEntrySet == null)
            mapEntrySet = new UberEntrySet();
        return mapEntrySet;
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

//{{{ fetchEntry, containsKey, get
//##############################################################################
    /**
    * Retrieves the Entry for the specified key, or null
    * if no such key is found in the table. key may be null.
    */
    protected UberEntry fetchEntry(Object key)
    {
        int idx     = index(hashFunc.hashCodeFor(key));
        UberEntry e = mapEntries[idx];
        
        while(e != null)
        {
            if(hashFunc.areEqual(e.getKey(), key))
                return e;
            e = e.chain;
        }
        
        return null;
    }
    
    public boolean containsKey(Object key)
    { return (fetchEntry(key) != null); }
    
    public Object get(Object key)
    {
        UberEntry e = fetchEntry(key);
        if(e == null) return null;
        else return e.getValue();
    }
//}}}

//{{{ remove, clear, size
//##############################################################################
    public Object remove(Object key)
    {
        int idx         = index(hashFunc.hashCodeFor(key));
        UberEntry e     = mapEntries[idx];
        UberEntry prev  = null;
        
        while(e != null)
        {
            if(hashFunc.areEqual(e.getKey(), key))
            {
                // Preserve the chain of entries at this index
                if(prev == null)        mapEntries[idx] = e.chain;
                else                    prev.chain      = e.chain;
                // Excise this entry from the linked list
                if(e.before == null)    mapHead = e.after;
                else                    e.before.after = e.after;
                if(e.after == null)     mapTail = e.before;
                else                    e.after.before = e.before;
                // Up the counter and return the value
                numChanges++;
                mapSize--;
                return e.getValue();
            }
            prev    = e;
            e       = e.chain;
        }
        
        return null; // no mapping was in place for this key
    }
    
    public void clear()
    {
        for(int i = 0; i < mapEntries.length; i++)
            mapEntries[i] = null;
        mapHead = mapTail = null;
        mapSize = 0;
        numChanges++;
    }
    
    public int size()
    { return mapSize; }
//}}}

//{{{ putBeforeImpl
//##############################################################################
    /**
    * Does a put, and positions the new entry
    * immediately before refEntry in the linked list.
    * If the map is empty or refEntry is null,
    * the new entry goes at the end of the list.
    */
    protected Object putBeforeImpl(UberEntry refEntry, Object key, Object value)
    {
        // Check for the case that refEntry.key == key
        // i.e. that we're trying to put key before itself
        if(refEntry != null && hashFunc.areEqual(refEntry.getKey(), key))
        {
            Object oldValue = refEntry.getValue();
            refEntry.setValue(value);
            numChanges++;
            return oldValue;
        }
        
        // Else procede normally:
        // Assume we're going to increase the size of the map;
        // i.e., that this key isn't already in the table.
        if(mapSize+1 > loadFactor*mapEntries.length)
            rehash(mapEntries.length * 2);
        
        // Remove the old entry for this key, if any
        Object oldValue = remove(key);
        
        int hcode       = hashFunc.hashCodeFor(key);
        int idx         = index(hcode);
        UberEntry e     = new UberEntry(key, value, hcode);
        UberEntry prev  = mapEntries[idx];
        
        // Insert the new entry into the chain
        if(prev == null)
            mapEntries[idx] = e;
        else
        {
            while(prev.chain != null)
                prev = prev.chain;
            prev.chain = e;
        }
        
        // Insert the new entry into the linked list
        if(mapSize == 0)                // also means mapHead == mapTail == null
            mapHead = mapTail = e;
        else if(refEntry == null)       // put at end of list
        {
            e.before        = mapTail;
            e.before.after  = e;
            mapTail         = e;
        }
        else if(refEntry == mapHead)    // put at front of list
        {
            e.after         = mapHead;
            e.after.before  = e;
            mapHead         = e;
        }
        else                            // put in the middle of the list
        {
            e.before        = refEntry.before;
            e.after         = refEntry;
            e.before.after  = e;
            e.after.before  = e;
        }
        
        numChanges++;
        mapSize++;
        return oldValue;
    }
//}}}

//{{{ replace, put, putBefore, putAfter
//##############################################################################
    /**
    * Replaces the value currently associated with key with newValue,
    * without changing the position of the key in the linked list (unlike put()).
    * This function just calls putBefore(key, key, newValue), and so if key
    * is not in the map, it is equivalent to calling put(key, newValue).
    */
    public Object replace(Object key, Object newValue)
    { return putBefore(key, key, newValue); }
    
    /**
    * Adds a new mapping at the end of the map, even if key was
    * previously positioned elsewhere in the map.
    */
    public Object put(Object key, Object value)
    { return putBeforeImpl(null, key, value); }
    
    /**
    * Puts a new entry in the map, positioning it immediately
    * before the specified reference key in the order of iteration.
    * If refKey is not in the map, this is equivalent to put(key, value).
    */
    public Object putBefore(Object refKey, Object key, Object value)
    {
        UberEntry e = fetchEntry(refKey);
        return putBeforeImpl(e, key, value);
    }

    /**
    * Puts a new entry in the map, positioning it immediately
    * after the specified reference key in the order of iteration.
    * If refKey is not in the map, this is equivalent to put(key, value).
    */
    public Object putAfter(Object refKey, Object key, Object value)
    {
        UberEntry e = fetchEntry(refKey);
        if(e == null)
            return putBeforeImpl(e, key, value);
        else
            return putBeforeImpl(e.after, key, value);
    }
//}}}

//{{{ rehash, ensureCapacity, getLoadFactor
//##############################################################################
    private final void rehash(int newCapacity)
    {
        if(log2Capacity == 30) return; // table is as big as it can get
        
        // Get putAll() to do our dirty work for us
        UberMap newMap = new UberMap(newCapacity, loadFactor);
        newMap.putAll(this);
        
        // Canabalize the new map
        this.mapEntries     = newMap.mapEntries;
        this.mapHead        = newMap.mapHead;
        this.mapTail        = newMap.mapTail;
        this.mapSize        = newMap.mapSize;
        this.log2Capacity   = newMap.log2Capacity;
        this.hashMask       = newMap.hashMask;
        
        numChanges++;
    }
    
    /**
    * Ensures that the table has at least newCapacity buckets in it.
    * Note that in keeping with the semantics of HashMap, the effective
    * maximum size() before rehashing is the capacity divided by the load factor.
    */
    public void ensureCapacity(int newCapacity)
    {
        if(newCapacity > mapEntries.length)
            rehash(newCapacity);
    }
    
    public double getLoadFactor()
    { return loadFactor; }
//}}}

//{{{ firstKey, lastKey, keyBefore, keyAfter
//##############################################################################
    /**
    * Returns the key of the first entry in the linked list.
    * @throws NoSuchElementException if the list is empty
    */
    public Object firstKey()
    {
        if(mapHead == null)
            throw new NoSuchElementException("No first key; map is empty.");
        return mapHead.getKey();
    }

    /**
    * Returns the key of the last entry in the linked list.
    * @throws NoSuchElementException if the list is empty
    */
    public Object lastKey()
    {
        if(mapTail == null)
            throw new NoSuchElementException("No last key; map is empty.");
        return mapTail.getKey();
    }

    /**
    * Returns the key that precedes refKey in the iteration order.
    * @throws NoSuchElementException if refKey is not in the map,
    * or is the first key in the map.
    */
    public Object keyBefore(Object refKey)
    {
        UberEntry e = fetchEntry(refKey);
        if(e == null)
            throw new NoSuchElementException(refKey+" is not a key in this map");
        else if(e.before == null)
            throw new NoSuchElementException(refKey+" is the first key in this map");
        else return e.before.getKey();
    }

    /**
    * Returns the key that follows refKey in the iteration order.
    * @throws NoSuchElementException if refKey is not in the map,
    * or is the last key in the map.
    */
    public Object keyAfter(Object refKey)
    {
        UberEntry e = fetchEntry(refKey);
        if(e == null)
            throw new NoSuchElementException(refKey+" is not a key in this map");
        else if(e.after == null)
            throw new NoSuchElementException(refKey+" is the last key in this map");
        else return e.after.getKey();
    }
//}}}

/*{{{ FOR TESTING ONLY: main        * /
//##############################################################################
    public static void main(String[] args)
    {
        UberMap m = new UberMap();
        
        // Insertion test
        m.put("apple", "red");
        m.put("orange", "orange");
        m.put("lime", "green");
        m.put("eggplant", "purple");
        m.putBefore("lime", "lemon", "yellow");
        m.putBefore("apple", "proto-apple", "colorless");
        m.putAfter("lemon", "lettuce", "yellow-green");
        m.putAfter("eggplant", "black hole", "don't know");
        
        // Removal / equality test
        UberMap copy = new UberMap(m);
        if(!m.equals(copy)) throw new Error("Duplicate maps are not equal");
        int i = 0;
        for(Iterator iter = m.entrySet().iterator(); iter.hasNext(); )
        {
            iter.next();
            if(++i % 4 == 3) iter.remove();
        }
        if(m.equals(copy)) throw new Error("Maps with different contents are still equal");
        
        // Rehashing test
        m.ensureCapacity(32);
        
        // Nulls test
        m.putBefore("black hole", null, null);
        
        // Show map and its entries
        // Iteration test
        System.out.println();
        System.out.println("Map (size="+m.size()+"):");
        System.out.println(m.toString());
        System.out.println();
        System.out.println("Entries:");
        for(Iterator iter = m.entrySet().iterator(); iter.hasNext(); )
        {
            UberEntry e = (UberEntry) iter.next();
            System.out.println("  "+e+" (in bucket "+m.index(e.hashCode)+")");
        }
        
        // keySet / values test
        // firstKey / lastKey test
        // keyBefore / keyAfter
        System.out.println();
        System.out.println("Keys: ["+m.firstKey()+", ... , "+m.lastKey()+"]");
        System.out.println(m.keySet().toString());
        for(Iterator iter = m.keySet().iterator(); iter.hasNext(); )
        {
            Object key = iter.next();
            Object after = null, before = null;
            try { after  = m.keyAfter( key); } catch(NoSuchElementException ex) { after  = "[nothing]"; }
            try { before = m.keyBefore(key); } catch(NoSuchElementException ex) { before = "[nothing]"; }
            System.out.println("  "+key+" comes after "+before+" and before "+after);
        }
        System.out.println();
        System.out.println("Values:");
        System.out.println(m.values().toString());
        
        // Debugging: show table structure
        System.out.println();
        System.out.println("Hash buckets:");
        for(int j = 0; j < m.mapEntries.length; j++)
        {
            UberEntry e = m.mapEntries[j];
            if(e == null)
                System.out.println("  "+j+": ---");
            else
            {
                System.out.print("  "+j+": "+e.getKey());
                while(e.chain != null)
                {
                    e = e.chain;
                    System.out.print(" => "+e.getKey());
                }
                System.out.println();
            }
        }
    }
/*}}}*/

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

