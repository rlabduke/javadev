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



public class WrapMap extends UberMap {

  //{{{ Constructor(s)
  //##############################################################################
  /** Default capacity is 16. */
  public WrapMap()
  { super(); }
  /** Default load factor is 0.75. */
  public WrapMap(int initCapacity)
  { super(initCapacity); }
  /** Default hash function is NullNaturalComparator. */
  public WrapMap(int initCapacity, double loadFactor)
  { super(initCapacity, loadFactor); }
  public WrapMap(HashFunction hashFunc)
  { super(16, 0.75, null); }
  public WrapMap(int initCapacity, HashFunction hashFunc)
  { super(initCapacity, 0.75, null); }
  
  /**
  * See java.util.HashMap for a discussion of capacity and load factor.
  * Note that in keeping with the semantics of HashMap, the effective
  * maximum size() before rehashing is the capacity divided by the load factor.
  * A null hashFunc will result in NullNaturalComparator being used.
  */
  public WrapMap(int initCapacity, double loadFactor, HashFunction hashFunc)
  {
    super(initCapacity, loadFactor, hashFunc);
  }
  
  public WrapMap(Map map)
  {
    super(map);
  }
  //}}}
  
  /**
  * Returns the key that precedes refKey in the iteration order.
  * If key is the first key in the map, returns last key.
  * @throws NoSuchElementException if refKey is not in the map,
  * 
  */
  public Object keyBefore(Object refKey) {
    UberEntry e = fetchEntry(refKey);
    if(e == null)
      throw new NoSuchElementException(refKey+" is not a key in this map");
    else if(e.before == null)
      return lastKey();
    else return e.before.getKey();
  }
  
  /**
  * Returns the key that follows refKey in the iteration order.
  * If key is the last key in the map, returns first key.
  * @throws NoSuchElementException if refKey is not in the map,
  * 
  */
  public Object keyAfter(Object refKey) {
    UberEntry e = fetchEntry(refKey);
    if(e == null)
      throw new NoSuchElementException(refKey+" is not a key in this map");
    else if(e.after == null)
      return firstKey();
    else return e.after.getKey();
  }
}
