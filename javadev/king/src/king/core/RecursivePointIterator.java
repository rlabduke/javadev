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
* <code>RecursivePointIterator</code> is an iterator over
* all the points beneath a given AGE. It is used by
* searching functions, etc.
*
* <p>This function may fail with a java.util.ConcurrentModificationException
* if the structure of the kinemage is modified during the search
* (i.e., add/remove groups, subgroups, or lists).
*
* <p>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Dec 19 10:57:57 EST 2002
*/
public class RecursivePointIterator //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    LinkedList  iterStack;
    Iterator    iter;
    KPoint      next;
    boolean     findTurnedOff; // ignore points/AGEs that are not ON
    boolean     findUnpickable; // ignore points that are unpickable
//}}}

//{{{ Constructor(s)
//##################################################################################################
    public RecursivePointIterator(AGE top, boolean findTurnedOff, boolean findUnpickable)
    {
        iterStack   = new LinkedList();
        iter        = top.iterator();
        next        = null;
        this.findTurnedOff = findTurnedOff;
        this.findUnpickable = findUnpickable;
    }

    public RecursivePointIterator(AGE top)
    { this(top, false, false); }
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
                Object o = iter.next();
                if(o instanceof KPoint
                && (findTurnedOff || ((KPoint)o).isOn())
                && (findUnpickable || !((KPoint)o).isUnpickable()) )
                {
                    next = (KPoint)o;
                    return; // recursion bottoms out here (success)
                }
                else if(o instanceof AGE
                && (findTurnedOff || ((AGE)o).isOn()) )
                {
                    iterStack.addLast(iter);
                    iter = ((AGE)o).iterator();
                    // recurses
                }
                // else recurses: we can ignore it and move on 
            }
            else if(!iterStack.isEmpty())
            {
                iter = (Iterator)iterStack.removeLast();
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

//{{{ next, hasNext
//##################################################################################################
    /**
    * Returns the next KPoint, if there is one, or null if there isn't.
    */
    public KPoint next()
    {
        // hasNext() may have already stocked next for us
        if(next == null) nextImpl();
        KPoint retval = next;
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
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

