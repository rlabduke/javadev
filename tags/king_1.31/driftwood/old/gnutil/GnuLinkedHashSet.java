/* GnuLinkedHashSet.java -- a set backed by a GnuLinkedHashMap, for linked
   list traversal.
   Copyright (C) 2001 Free Software Foundation, Inc.

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

import java.io.Serializable;

/**
 * This class provides a hashtable-backed implementation of the
 * Set interface, with predictable traversal order.
 * <p>
 *
 * It uses a hash-bucket approach; that is, hash collisions are handled
 * by linking the new node off of the pre-existing node (or list of
 * nodes).  In this manner, techniques such as linear probing (which
 * can cause primary clustering) and rehashing (which does not fit very
 * well with Java's method of precomputing hash codes) are avoided.  In
 * addition, this maintains a doubly-linked list which tracks insertion
 * order.  Note that the insertion order is not modified if an
 * <code>add</code> simply reinserts an element in the set.
 * <p>
 *
 * One of the nice features of tracking insertion order is that you can
 * copy a set, and regardless of the implementation of the original,
 * produce the same results when iterating over the copy.  This is possible
 * without needing the overhead of <code>TreeSet</code>.
 * <p>
 *
 * Under ideal circumstances (no collisions), GnuLinkedHashSet offers O(1) 
 * performance on most operations.  In the worst case (all elements map
 * to the same hash code -- very unlikely), most operations are O(n).
 * <p>
 *
 * GnuLinkedHashSet accepts the null entry.  It is not synchronized, so if
 * you need multi-threaded access, consider using:<br>
 * <code>Set s = Collections.synchronizedSet(new GnuLinkedHashSet(...));</code>
 * <p>
 *
 * The iterators are <i>fail-fast</i>, meaning that any structural
 * modification, except for <code>remove()</code> called on the iterator
 * itself, cause the iterator to throw a
 * {@link ConcurrentModificationException} rather than exhibit
 * non-deterministic behavior.
 *
 * @author Eric Blake <ebb9@email.byu.edu>
 * @see Object#hashCode()
 * @see Collection
 * @see Set
 * @see GnuHashSet
 * @see TreeSet
 * @see Collections#synchronizedSet(Set)
 * @since 1.4
 * @status updated to 1.4
 */
public class GnuLinkedHashSet extends GnuHashSet
  implements Set, Cloneable, Serializable
{
  /**
   * Compatible with JDK 1.4.
   */
  private static final long serialVersionUID = -2851667679971038690L;

  /**
   * Construct a new, empty GnuHashSet whose backing GnuHashMap has the default
   * capacity (11) and loadFacor (0.75).
   */
  public GnuLinkedHashSet()
  {
    super();
  }

  /**
   * Construct a new, empty GnuHashSet whose backing GnuHashMap has the supplied
   * capacity and the default load factor (0.75).
   *
   * @param initialCapacity the initial capacity of the backing GnuHashMap
   * @throws IllegalArgumentException if the capacity is negative
   */
  public GnuLinkedHashSet(int initialCapacity)
  {
    super(initialCapacity);
  }

  /**
   * Construct a new, empty GnuHashSet whose backing GnuHashMap has the supplied
   * capacity and load factor.
   *
   * @param initialCapacity the initial capacity of the backing GnuHashMap
   * @param loadFactor the load factor of the backing GnuHashMap
   * @throws IllegalArgumentException if either argument is negative, or
   *         if loadFactor is POSITIVE_INFINITY or NaN
   */
  public GnuLinkedHashSet(int initialCapacity, float loadFactor)
  {
    super(initialCapacity, loadFactor);
  }

  /**
   * Construct a new GnuHashSet with the same elements as are in the supplied
   * collection (eliminating any duplicates, of course). The backing storage
   * has twice the size of the collection, or the default size of 11,
   * whichever is greater; and the default load factor (0.75).
   *
   * @param c a collection of initial set elements
   * @throws NullPointerException if c is null
   */
  public GnuLinkedHashSet(Collection c)
  {
    super(c);
  }

//############################################################################
// Begin modifications to support re-ordering the set

    /** Identical to map, but map is private... */
    GnuLinkedHashMap lmap;
    
    
    /**
    * Helper method which initializes the backing Map.
    *
    * @param capacity the initial capacity
    * @param load the initial load factor
    * @return the backing GnuHashMap
    */
    GnuHashMap init(int capacity, float load)
    {
        return (lmap = new GnuLinkedHashMap(capacity, load));
    }


    /**
    * Gets the entry that immediately precedes the reference entry
    * in the linked list, or null if the reference is the eldest (first)
    * entry in this set.
    * @throws NoSuchElementException if the reference does not appear in this set
    */
    public Object getBefore(Object ref)
    { return lmap.getKeyBefore(ref); }
    
    
    /**
    * Gets the entry that immediately follows the reference entry
    * in the linked list, or null if the reference is the newest (last)
    * entry in this set.
    * @throws NoSuchElementException if the reference does not appear in this set
    */
    public Object getAfter(Object ref)
    { return lmap.getKeyAfter(ref); }
    
    
    /**
    * If move does not immediately precede ref in the linked list,
    * then move will be removed from its current position
    * and re-inserted immediately before the reference.
    * @throws NoSuchElementException if the reference key
    *   or the mobile key does not appear in this set
    */
    public void moveBefore(Object move, Object ref)
    { lmap.moveBefore(move, ref); }
    
    
    /**
    * If move does not immediately follow ref in the linked list,
    * then move will be removed from its current position
    * and re-inserted immediately after the reference.
    * @throws NoSuchElementException if the reference key
    *   or the mobile key does not appear in this set
    */
    public void moveAfter(Object move, Object ref)
    { lmap.moveAfter(move, ref); }

// End modifications to support re-ordering the set
//############################################################################
}
