// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>KIterator</code> is a way of iterating over kinemage contents
* without writing a recursive function to handle the nesting of KGroups.
* It is structured as an iterator rather than some kind of Visitor pattern
* because the inversion of control means the tree can be traversed piecewise,
* as is needed for Find / Find Next type functions.
*
* <p>If you're not sure that this instance will only return AGEs (for instance),
* you should declare it as <code>KIterator&lt;AHE&gt;</code> and use <code>instanceof</code>.
*
* <p>This function may fail with a java.util.ConcurrentModificationException
* if the structure of the kinemage is modified during the search
* (i.e., add/remove groups, lists, or points).
* Obviously, clients should not rely on this behavior for correctness.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Dec 19 10:57:57 EST 2002
*/
public class KIterator<T extends AHE> implements Iterable<T>, Iterator<T>
{
//{{{ Constants
    public enum Opts { KINEMAGE, GROUP, LIST, POINT, VISIBLE_ONLY, PICKABLE_ONLY }
//}}}

//{{{ Variable definitions
//##################################################################################################
    Set<Opts>               opts;
    T                       next;
    Iterator<T>             iter;
    LinkedList<Iterator<T>> iterStack;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    public KIterator(AGE top, Opts opt)
    { this(top, EnumSet.of(opt)); }
    public KIterator(AGE top, Opts opt, Opts... opts)
    { this(top, EnumSet.of(opt, opts)); }
    
    public KIterator(AGE top, Set<Opts> opts)
    {
        this.opts       = opts;
        this.next       = null;
        this.iter       = (Iterator<T>) Collections.singleton(top).iterator();
        this.iterStack  = new LinkedList<Iterator<T>>();
    }
//}}}

//{{{ nextImpl
//##################################################################################################
    /**
    * Function that searches the tree for the next KPoint
    * and places it into the variable next.
    * If there are no more points, next == null.
    */
    private void nextImpl()
    {
        // True tail-recursion has been replaced by a while-loop pseudo-recursion
        // because otherwise we tend to get StackOverflowErrors.
        // If Java was smarter it would perform this optimization for us... ;)
        while(true)
        {
            if(iter.hasNext())
            {
                T ahe = iter.next();
                if(ahe instanceof KPoint)
                {
                    if(shouldVisit((KPoint) ahe))
                    {
                        next = ahe;
                        return; // recursion bottoms out here (success)
                    }
                }
                else if(ahe instanceof AGE)
                {
                    AGE age = (AGE) ahe;
                    if(shouldVisitChildren(age))
                    {
                        iterStack.addLast(iter);
                        iter = age.getChildren().iterator();
                    }
                    if(shouldVisit(age))
                    {
                        next = ahe;
                        return; // recursion bottoms out here (success)
                    }
                    // else recurses
                }
                // else recurses: we can ignore it and move on
            }
            else if(!iterStack.isEmpty())
            {
                iter = iterStack.removeLast();
                // recurses
            }
            else
            {
                next = null;
                return; // recursion bottoms out here (failure)
            }
        }
    }
//}}}

//{{{ shouldVisit(Children)
//##################################################################################################
    protected boolean shouldVisit(KPoint point)
    {
        return (point.isOn() || !opts.contains(Opts.VISIBLE_ONLY))
            && !(point.isUnpickable() && opts.contains(Opts.PICKABLE_ONLY));
            //&& opts.contains(Opts.POINT); // this has already been checked at the list level
    }
    
    protected boolean shouldVisit(AGE age)
    {
        boolean ans = (age.isOn() || !opts.contains(Opts.VISIBLE_ONLY));
        if(age instanceof KList)            ans &= opts.contains(Opts.LIST);
        else if(age instanceof KGroup)      ans &= opts.contains(Opts.GROUP);
        else if(age instanceof Kinemage)    ans &= opts.contains(Opts.KINEMAGE);
        return ans;
    }
    
    protected boolean shouldVisitChildren(AGE age)
    {
        boolean ans = (age.isOn() || !opts.contains(Opts.VISIBLE_ONLY));
        if(age instanceof KList)        ans &= opts.contains(Opts.POINT);
        else if(age instanceof KGroup)  ans &= opts.contains(Opts.GROUP) || opts.contains(Opts.LIST) || opts.contains(Opts.POINT);
        return ans;
    }
//}}}

//{{{ next, hasNext, iterator, remove
//##################################################################################################
    /**
    * Returns the next KPoint, if there is one, or null if there isn't.
    */
    public T next()
    {
        // hasNext() may have already stocked next for us
        if(next == null) nextImpl();
        T retval = next;
        // mark this value as consumed
        next = null;
        return retval;
    }
    
    /**
    * Returns true if there is another point available (and it's not null,
    * but lists should never contain null points).
    */
    public boolean hasNext()
    {
        if(next == null) nextImpl();
        return (next != null);
    }
    
    /** Not supported by this implementation */
    public void remove()
    { throw new UnsupportedOperationException(); }
    
    public Iterator<T> iterator()
    { return this; }
//}}}

//{{{ allLists/Points/NonPoints, visibleLists
//##################################################################################################
    public static KIterator<KList> allLists(AGE top)
    { return new KIterator<KList>(top, KIterator.Opts.LIST); }

    public static KIterator<KPoint> allPoints(AGE top)
    { return new KIterator<KPoint>(top, KIterator.Opts.POINT); }

    public static KIterator<AGE> allNonPoints(AGE top)
    { return new KIterator<AGE>(top, KIterator.Opts.LIST, KIterator.Opts.GROUP, KIterator.Opts.KINEMAGE); }

    public static KIterator<KList> visibleLists(AGE top)
    { return new KIterator<KList>(top, KIterator.Opts.LIST, KIterator.Opts.VISIBLE_ONLY); }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

