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
* <code>UberSet</code> is a wrapper for UberMap that implements Set.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb  3 13:30:46 EST 2004
*/
public class UberSet extends AbstractSet
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    UberMap     map;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Default capacity is 16. */
    public UberSet()
    { this(16); }
    /** Default load factor is 0.75. */
    public UberSet(int initCapacity)
    { this(initCapacity, 0.75); }
    
    /**
    * See java.util.HashMap for a discussion of capacity and load factor.
    * Note that in keeping with the semantics of HashMap, the effective
    * maximum size() before rehashing is the capacity divided by the load factor.
    */
    public UberSet(int initCapacity, double loadFactor)
    {
        super();
        map = new UberMap(initCapacity, loadFactor);
    }
    
    public UberSet(Collection c)
    {
        this(c.size());
        this.addAll(c);
    }
//}}}

//{{{ add, addBefore, addAfter
//##############################################################################
    public boolean add(Object o)
    {
        if(this.contains(o)) return false;
        map.put(o, o);
        return true;
    }
    
    public boolean addBefore(Object ref, Object o)
    {
        if(this.contains(o)) return false;
        map.putBefore(ref, o, o);
        return true;
    }
    
    public boolean addAfter(Object ref, Object o)
    {
        if(this.contains(o)) return false;
        map.putAfter(ref, o, o);
        return true;
    }
//}}}

//{{{ clear, contains, ensureCapacity, iterator
//##############################################################################
    public void clear()
    { map.clear(); }
    
    public boolean contains(Object o)
    { return map.containsKey(o); }
    
    public void ensureCapacity(int cap)
    { map.ensureCapacity(cap); }
    
    public Iterator iterator()
    { return map.keySet().iterator(); }
//}}}

//{{{ remove, size, first/lastItem, itemBefore/After
//##############################################################################
    public boolean remove(Object o)
    {
        if(this.contains(o))
        {
            map.remove(o);
            return true;
        }
        else return false;
    }
    
    public int size()
    { return map.size(); }
    
    public Object firstItem()
    { return map.firstKey(); }
    
    public Object lastItem()
    { return map.lastKey(); }
    
    public Object itemBefore(Object o)
    { return map.keyBefore(o); }
    
    public Object itemAfter(Object o)
    { return map.keyAfter(o); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

