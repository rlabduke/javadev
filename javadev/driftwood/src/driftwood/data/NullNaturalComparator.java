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
* <code>NullNaturalComparator</code> allows the use of
* objects' natural order when some of the objects may be null.
* This was developed for use with java.util.TreeMap, though
* it may be useful other places, too.
* Nulls can either be sorted to the beginning or end.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jun 13 14:36:00 EDT 2003
*/
public class NullNaturalComparator implements Comparator
{
    private boolean nullComesFirst;

    /**
    * Creates a comparator that sorts nulls to the front of the list.
    */
    public NullNaturalComparator()
    {
        nullComesFirst = true;
    }

    /**
    * Creates a comparator that sorts nulls to either the front or the back of the list.
    */
    public NullNaturalComparator(boolean nullComesFirst)
    {
        this.nullComesFirst = nullComesFirst;
    }

    /**
    * Returns less than, greater than, or equal to zero
    * as o1 is less than, greater than, or equal to o2.
    * Null may either be less than or greater than all other objects.
    * If both o1 and o2 are non-null, this is equal to o1.compareTo(o2).
    */
    public int compare(Object o1, Object o2)
    {
        if(o1 == null)
        {
            if(o2 == null)  return 0;
            else            return (nullComesFirst ? -1 : 1);
        }
        else // o1 != null
        {
            if(o2 == null)  return (nullComesFirst ? 1 : -1);
            else            return ((Comparable)o1).compareTo(o2);
        }
    }
}//class

