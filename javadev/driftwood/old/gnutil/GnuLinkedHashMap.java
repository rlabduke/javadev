/* GnuLinkedHashMap.java -- a class providing hashtable data structure,
   mapping Object --> Object, with linked list traversal
   Copyright (C) 2001, 2002 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package driftwood.gnutil;

import java.util.*;

/**
 * This class provides a hashtable-backed implementation of the
 * Map interface, with predictable traversal order.
 * <p>
 *
 * It uses a hash-bucket approach; that is, hash collisions are handled
 * by linking the new node off of the pre-existing node (or list of
 * nodes).  In this manner, techniques such as linear probing (which
 * can cause primary clustering) and rehashing (which does not fit very
 * well with Java's method of precomputing hash codes) are avoided.  In
 * addition, this maintains a doubly-linked list which tracks either
 * insertion or access order.
 * <p>
 *
 * In insertion order, calling <code>put</code> adds the key to the end of
 * traversal, unless the key was already in the map; changing traversal order
 * requires removing and reinserting a key.  On the other hand, in access
 * order, all calls to <code>put</code> and <code>get</code> cause the
 * accessed key to move to the end of the traversal list.  Note that any
 * accesses to the map's contents via its collection views and iterators do
 * not affect the map's traversal order, since the collection views do not
 * call <code>put</code> or <code>get</code>.
 * <p>
 *
 * One of the nice features of tracking insertion order is that you can
 * copy a hashtable, and regardless of the implementation of the original,
 * produce the same results when iterating over the copy.  This is possible
 * without needing the overhead of <code>TreeMap</code>.
 * <p>
 *
 * When using this {@link #GnuLinkedHashMap(int, float, boolean) constructor},
 * you can build an access-order mapping.  This can be used to implement LRU
 * caches, for example.  By overriding {@link #removeEldestEntry(Map.Entry)},
 * you can also control the removal of the oldest entry, and thereby do
 * things like keep the map at a fixed size.
 * <p>
 *
 * Under ideal circumstances (no collisions), GnuLinkedHashMap offers O(1) 
 * performance on most operations (<code>containsValue()</code> is,
 * of course, O(n)).  In the worst case (all keys map to the same 
 * hash code -- very unlikely), most operations are O(n).  Traversal is
 * faster than in GnuHashMap (proportional to the map size, and not the space
 * allocated for the map), but other operations may be slower because of the
 * overhead of the maintaining the traversal order list.
 * <p>
 *
 * GnuLinkedHashMap accepts the null key and null values.  It is not
 * synchronized, so if you need multi-threaded access, consider using:<br>
 * <code>Map m = Collections.synchronizedMap(new GnuLinkedHashMap(...));</code>
 * <p>
 *
 * The iterators are <i>fail-fast</i>, meaning that any structural
 * modification, except for <code>remove()</code> called on the iterator
 * itself, cause the iterator to throw a
 * {@link ConcurrentModificationException} rather than exhibit
 * non-deterministic behavior.
 *
 * @author Eric Blake (ebb9@email.byu.edu)
 * @see Object#hashCode()
 * @see Collection
 * @see Map
 * @see GnuHashMap
 * @see TreeMap
 * @see Hashtable
 * @since 1.4
 * @status updated to 1.4
 */
public class GnuLinkedHashMap extends GnuHashMap
{
  /**
   * Compatible with JDK 1.4.
   */
  private static final long serialVersionUID = 3801124242820219131L;

  /**
   * The oldest Entry to begin iteration at.
   */
  transient LinkedHashEntry root;

  /**
   * The iteration order of this linked hash map: <code>true</code> for
   * access-order, <code>false</code> for insertion-order.
   *
   * @serial true for access order traversal
   */
  final boolean accessOrder;

  /**
   * Class to represent an entry in the hash table. Holds a single key-value
   * pair and the doubly-linked insertion order list.
   */
  class LinkedHashEntry extends HashEntry
  {
    /**
     * The predecessor in the iteration list. If this entry is the root
     * (eldest), pred points to the newest entry.
     */
    LinkedHashEntry pred;

    /** The successor in the iteration list, null if this is the newest. */
    LinkedHashEntry succ;

    /**
     * Simple constructor.
     *
     * @param key the key
     * @param value the value
     */
    LinkedHashEntry(Object key, Object value)
    {
      super(key, value);
      if (root == null)
        {
          root = this;
          pred = this;
        }
      else
        {
          pred = root.pred;
          pred.succ = this;
          root.pred = this;
        }
    }

    /**
     * Called when this entry is accessed via put or get. This version does
     * the necessary bookkeeping to keep the doubly-linked list in order,
     * after moving this element to the newest position in access order.
     */
    void access()
    {
      if (accessOrder && succ != null)
        {
          modCount++;
          if (this == root)
            {
              root = succ;
              pred.succ = this;
              succ = null;
            }
          else
            {
              pred.succ = succ;
              succ.pred = pred;
              succ = null;
              pred = root.pred;
              pred.succ = this;
            }
        }
    }

    /**
     * Called when this entry is removed from the map. This version does
     * the necessary bookkeeping to keep the doubly-linked list in order.
     *
     * @return the value of this key as it is removed
     */
    Object cleanup()
    {
      if (this == root)
        {
          root = succ;
          if (succ != null)
            succ.pred = pred;
        }
      else if (succ == null)
        {
          pred.succ = null;
          root.pred = pred;
        }
      else
        {
          pred.succ = succ;
          succ.pred = pred;
        }
      return value;
    }
  } // class LinkedHashEntry

  /**
   * Construct a new insertion-ordered GnuLinkedHashMap with the default
   * capacity (11) and the default load factor (0.75).
   */
  public GnuLinkedHashMap()
  {
    super();
    accessOrder = false;
  }

  /**
   * Construct a new insertion-ordered GnuLinkedHashMap from the given Map,
   * with initial capacity the greater of the size of <code>m</code> or
   * the default of 11.
   * <p>
   *
   * Every element in Map m will be put into this new GnuHashMap, in the
   * order of m's iterator.
   *
   * @param m a Map whose key / value pairs will be put into
   *          the new GnuHashMap.  <b>NOTE: key / value pairs
   *          are not cloned in this constructor.</b>
   * @throws NullPointerException if m is null
   */
  public GnuLinkedHashMap(Map m)
  {
    super(m);
    accessOrder = false;
  }

  /**
   * Construct a new insertion-ordered GnuLinkedHashMap with a specific
   * inital capacity and default load factor of 0.75.
   *
   * @param initialCapacity the initial capacity of this GnuHashMap (&gt;= 0)
   * @throws IllegalArgumentException if (initialCapacity &lt; 0)
   */
  public GnuLinkedHashMap(int initialCapacity)
  {
    super(initialCapacity);
    accessOrder = false;
  }

  /**
   * Construct a new insertion-orderd GnuLinkedHashMap with a specific
   * inital capacity and load factor.
   *
   * @param initialCapacity the initial capacity (&gt;= 0)
   * @param loadFactor the load factor (&gt; 0, not NaN)
   * @throws IllegalArgumentException if (initialCapacity &lt; 0) ||
   *                                     ! (loadFactor &gt; 0.0)
   */
  public GnuLinkedHashMap(int initialCapacity, float loadFactor)
  {
    super(initialCapacity, loadFactor);
    accessOrder = false;
  }

  /**
   * Construct a new GnuLinkedHashMap with a specific inital capacity, load
   * factor, and ordering mode.
   *
   * @param initialCapacity the initial capacity (&gt;=0)
   * @param loadFactor the load factor (&gt;0, not NaN)
   * @param accessOrder true for access-order, false for insertion-order
   * @throws IllegalArgumentException if (initialCapacity &lt; 0) ||
   *                                     ! (loadFactor &gt; 0.0)
   */
  public GnuLinkedHashMap(int initialCapacity, float loadFactor,
                       boolean accessOrder)
  {
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
  }

  /**
   * Clears the Map so it has no keys. This is O(1).
   */
  public void clear()
  {
    super.clear();
    root = null;
  }

  /**
   * Returns <code>true</code> if this GnuHashMap contains a value
   * <code>o</code>, such that <code>o.equals(value)</code>.
   *
   * @param value the value to search for in this GnuHashMap
   * @return <code>true</code> if at least one key maps to the value
   */
  public boolean containsValue(Object value)
  {
    LinkedHashEntry e = root;
    while (e != null)
      {
        if (equals(value, e.value))
          return true;
        e = e.succ;
      }
    return false;
  }

  /**
   * Return the value in this Map associated with the supplied key,
   * or <code>null</code> if the key maps to nothing.  If this is an
   * access-ordered Map and the key is found, this performs structural
   * modification, moving the key to the newest end of the list. NOTE:
   * Since the value could also be null, you must use containsKey to
   * see if this key actually maps to something.
   *
   * @param key the key for which to fetch an associated value
   * @return what the key maps to, if present
   * @see #put(Object, Object)
   * @see #containsKey(Object)
   */
  public Object get(Object key)
  {
    int idx = hash(key);
    HashEntry e = buckets[idx];
    while (e != null)
      {
        if (equals(key, e.key))
          {
            e.access();
            return e.value;
          }
        e = e.next;
      }
    return null;
  }

  /**
   * Returns <code>true</code> if this map should remove the eldest entry.
   * This method is invoked by all calls to <code>put</code> and
   * <code>putAll</code> which place a new entry in the map, providing
   * the implementer an opportunity to remove the eldest entry any time
   * a new one is added.  This can be used to save memory usage of the
   * hashtable, as well as emulating a cache, by deleting stale entries.
   * <p>
   *
   * For example, to keep the Map limited to 100 entries, override as follows:
   * <pre>
   * private static final int MAX_ENTRIES = 100;
   * protected boolean removeEldestEntry(Map.Entry eldest)
   * {
   *   return size() &gt; MAX_ENTRIES;
   * }
   * </pre><p>
   *
   * Typically, this method does not modify the map, but just uses the
   * return value as an indication to <code>put</code> whether to proceed.
   * However, if you override it to modify the map, you must return false
   * (indicating that <code>put</code> should leave the modified map alone),
   * or you face unspecified behavior.  Remember that in access-order mode,
   * even calling <code>get</code> is a structural modification, but using
   * the collections views (such as <code>keySet</code>) is not.
   * <p>
   *
   * This method is called after the eldest entry has been inserted, so
   * if <code>put</code> was called on a previously empty map, the eldest
   * entry is the one you just put in! The default implementation just
   * returns <code>false</code>, so that this map always behaves like
   * a normal one with unbounded growth.
   *
   * @param eldest the eldest element which would be removed if this
   *        returns true. For an access-order map, this is the least
   *        recently accessed; for an insertion-order map, this is the
   *        earliest element inserted.
   * @return true if <code>eldest</code> should be removed
   */
  protected boolean removeEldestEntry(Map.Entry eldest)
  {
    return false;
  }

  /**
   * Helper method called by <code>put</code>, which creates and adds a
   * new Entry, followed by performing bookkeeping (like removeEldestEntry).
   *
   * @param key the key of the new Entry
   * @param value the value
   * @param idx the index in buckets where the new Entry belongs
   * @param callRemove whether to call the removeEldestEntry method
   * @see #put(Object, Object)
   * @see #removeEldestEntry(Map.Entry)
   * @see LinkedHashEntry#LinkedHashEntry(Object, Object)
   */
  void addEntry(Object key, Object value, int idx, boolean callRemove)
  {
    LinkedHashEntry e = new LinkedHashEntry(key, value);
    e.next = buckets[idx];
    buckets[idx] = e;
    if (callRemove && removeEldestEntry(root))
      remove(root);
  }

  /**
   * Helper method, called by clone() to reset the doubly-linked list.
   *
   * @param m the map to add entries from
   * @see #clone()
   */
  void putAllInternal(Map m)
  {
    root = null;
    super.putAllInternal(m);
  }

  /**
   * Generates a parameterized iterator. This allows traversal to follow
   * the doubly-linked list instead of the random bin order of GnuHashMap.
   *
   * @param type {@link #KEYS}, {@link #VALUES}, or {@link #ENTRIES}
   * @return the appropriate iterator
   */
  Iterator iterator(final int type)
  {
    return new Iterator()
    {
      /** The current Entry. */
      LinkedHashEntry current = root;

      /** The previous Entry returned by next(). */
      LinkedHashEntry last;

      /** The number of known modifications to the backing Map. */
      int knownMod = modCount;

      /**
       * Returns true if the Iterator has more elements.
       *
       * @return true if there are more elements
       * @throws ConcurrentModificationException if the GnuHashMap was modified
       */
      public boolean hasNext()
      {
        if (knownMod != modCount)
          throw new ConcurrentModificationException();
        return current != null;
      }

      /**
       * Returns the next element in the Iterator's sequential view.
       *
       * @return the next element
       * @throws ConcurrentModificationException if the GnuHashMap was modified
       * @throws NoSuchElementException if there is none
       */
      public Object next()
      {
        if (knownMod != modCount)
          throw new ConcurrentModificationException();
        if (current == null)
          throw new NoSuchElementException();
        last = current;
        current = current.succ;
        return type == VALUES ? last.value : type == KEYS ? last.key : last;
      }
      
      /**
       * Removes from the backing GnuHashMap the last element which was fetched
       * with the <code>next()</code> method.
       *
       * @throws ConcurrentModificationException if the GnuHashMap was modified
       * @throws IllegalStateException if called when there is no last element
       */
      public void remove()
      {
        if (knownMod != modCount)
          throw new ConcurrentModificationException();
        if (last == null)
          throw new IllegalStateException();
        GnuLinkedHashMap.this.remove(last.key);
        last = null;
        knownMod++;
      }
    };
  }
  
  
//############################################################################
// Begin modifications to support re-ordering the map

    /**
    * Retrieves a Map.Entry object without updating the access order.
    */
    LinkedHashEntry findEntry(Object key)
    {
        // Stolen directly from get
        int idx = hash(key);
        HashEntry e = buckets[idx];
        while (e != null)
        {
            if (equals(key, e.key))
            {
                //e.access(); -- this is where access order is updated
                //return e.value;
                return (LinkedHashEntry)e;
            }
            e = e.next;
        }
        return null;
    }
    
    
    /**
    * Gets the entry that immediately precedes the reference-keyed entry
    * in the linked list, or null if the reference is the eldest (first)
    * entry in this map.
    * If the entry is found and this is an access-ordered map,
    * it will be moved to the end, just as for get().
    * @throws NoSuchElementException if the reference key does not appear in this map
    */
    public Map.Entry getEntryBefore(Object refKey)
    {
        LinkedHashEntry ref = findEntry(refKey);
        if(ref == null)
            throw new NoSuchElementException(refKey+" is not a part of this map");
        
        // root.pred is co-opted as a pointer to the tail of the list
        LinkedHashEntry e = (ref == root ? null : ref.pred);
        if(e != null) e.access();
        return e;
    }
    
    
    /**
    * Gets the entry that immediately follows the reference-keyed entry
    * in the linked list, or null if the reference is the newest (last)
    * entry in this map.
    * If the entry is found and this is an access-ordered map,
    * it will be moved to the end, just as for get().
    * @throws NoSuchElementException if the reference key does not appear in this map
    */
    public Map.Entry getEntryAfter(Object refKey)
    {
        LinkedHashEntry ref = findEntry(refKey);
        if(ref == null)
            throw new NoSuchElementException(refKey+" is not a part of this map");
        
        LinkedHashEntry e = ref.succ;
        if(e != null) e.access();
        return e;
    }
    
    
    /** See getEntryBefore() */
    public Object getKeyBefore(Object refKey)
    { Map.Entry e = getEntryBefore(refKey); return (e == null ? null : e.getKey()); }
    /** See getEntryBefore() */
    public Object getValueBefore(Object refKey)
    { Map.Entry e = getEntryBefore(refKey); return (e == null ? null : e.getValue()); }
    /** See getEntryAfter() */
    public Object getKeyAfter(Object refKey)
    { Map.Entry e = getEntryAfter(refKey); return (e == null ? null : e.getKey()); }
    /** See getEntryAfter() */
    public Object getValueAfter(Object refKey)
    { Map.Entry e = getEntryAfter(refKey); return (e == null ? null : e.getValue()); }
    
    
    /**
    * If moveKey does not immediately precede refKey in the linked list,
    * the moveKey mapping will be removed from its current position
    * and re-inserted immediately before the reference mapping.
    * @throws NoSuchElementException if the reference key
    *   or the mobile key does not appear in this map
    */
    public void moveBefore(Object moveKey, Object refKey)
    {
        LinkedHashEntry ref = findEntry(refKey);
        if(ref == null)
            throw new NoSuchElementException(refKey+" is not a part of this map");
        LinkedHashEntry move = findEntry(moveKey);
        if(move == null)
            throw new NoSuchElementException(moveKey+" is not a part of this map");
        
        if(move.succ == ref) return; // we're already done
        
        modCount++;         // this will change the iteration order
        move.cleanup();     // extracts move from the linked list
        
        if(ref == root)
        {
            move.pred       = ref.pred; // points to the newest entry (tail)
            move.succ       = ref;
            ref.pred        = move;
            root            = move;
        }
        else
        {
            move.pred       = ref.pred;
            move.succ       = ref;
            ref.pred        = move;
            move.pred.succ  = move;     // pred is never null
        }
    }
    
    
    /**
    * If moveKey does not immediately follow refKey in the linked list,
    * the moveKey mapping will be removed from its current position
    * and re-inserted immediately after the reference mapping.
    * @throws NoSuchElementException if the reference key
    *   or the mobile key does not appear in this map
    */
    public void moveAfter(Object moveKey, Object refKey)
    {
        LinkedHashEntry ref = findEntry(refKey);
        if(ref == null)
            throw new NoSuchElementException(refKey+" is not a part of this map");
        LinkedHashEntry move = findEntry(moveKey);
        if(move == null)
            throw new NoSuchElementException(moveKey+" is not a part of this map");
        
        if(ref.succ == move) return; // we're already done
        
        modCount++;         // this will change the iteration order
        move.cleanup();     // extracts move from the linked list
        
        if(ref.succ == null) // ref is the tail of this list
        {
            move.pred       = ref;
            move.succ       = null;
            ref.succ        = move;
            root.pred       = move; // move is the new tail
        }
        else
        {
            move.pred       = ref;
            move.succ       = ref.succ;
            ref.succ        = move;
            move.succ.pred  = move; // we know ref.succ was not null, therefore move.succ is not null
        }
    }

// End modifications to support re-ordering the map
//############################################################################
} // class GnuLinkedHashMap
