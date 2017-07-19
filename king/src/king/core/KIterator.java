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
    T                       last;
    Iterator<T>             iter;
    Iterator<T>             iterLast;
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
        this.last       = null;
        this.iter       = (Iterator<T>) Collections.singleton(top).iterator();
        this.iterLast   = null;
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
        last = null;
        while(true)
        {
            if(iter.hasNext())
            {
                T ahe = iter.next();
                iterLast = iter; // "iter" will change if we need to visit children
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
        last = next;
        // mark this value as consumed
        next = null;
        return last;
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
    
    /**
    * This function is supported only in a limited way:
    * it works only if you've called next() more recently than hasNext().
    * If you've called hasNext() more recently than next(),
    * then it will fail with an IllegalStateException.
    *
    * <p>Note that parents are visited before children.  Calling remove
    * on any parent will mean that none of its descendants are visited.
    *
    * <p>If this function does not meet your needs, consider performing two iterations:
    * In the first, build a Collection of all the items to be removed;
    * in the second, traverse that new Collection and remove its elements from the kinemage
    * by calling remove() on their parents.
    */
    public void remove()
    {
        if(last != null)
        {
            AGE parent = last.getParent();
            iterLast.remove();
            // Don't traverse children, if you were considering it:
            if(iter != iterLast)
            {
                iter = iterStack.removeLast();
                assert iter == iterLast;
            }
            int change = (last instanceof AGE ? AHE.CHANGE_TREE_CONTENTS : AHE.CHANGE_POINT_CONTENTS);
            last = null;
            parent.fireKinChanged(change);
        }
        else throw new IllegalStateException();
    }
    
    public Iterator<T> iterator()
    { return this; }
//}}}

//{{{ allLists/Points/NonPoints, visibleLists/Points
//##################################################################################################
    public static KIterator<KList> allLists(AGE top)
    { return new KIterator<KList>(top, KIterator.Opts.LIST); }

    public static KIterator<KPoint> allPoints(AGE top)
    { return new KIterator<KPoint>(top, KIterator.Opts.POINT); }

    public static KIterator<AGE> allNonPoints(AGE top)
    { return new KIterator<AGE>(top, KIterator.Opts.LIST, KIterator.Opts.GROUP, KIterator.Opts.KINEMAGE); }

    public static KIterator<KList> visibleLists(AGE top)
    { return new KIterator<KList>(top, KIterator.Opts.LIST, KIterator.Opts.VISIBLE_ONLY); }

    public static KIterator<KPoint> visiblePoints(AGE top)
    { return new KIterator<KPoint>(top, KIterator.Opts.POINT, KIterator.Opts.VISIBLE_ONLY); }
    
    public static KIterator<KGroup> visibleGroups(AGE top) 
    { return new KIterator<KGroup>(top, KIterator.Opts.GROUP, KIterator.Opts.VISIBLE_ONLY); }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

