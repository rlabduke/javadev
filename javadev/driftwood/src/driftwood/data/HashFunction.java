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
* <code>HashFunction</code> defines a pluggable hash function and equality test
* for use in place of hashCode() and equals() by hashtable-like data structures.
* This allows for multiple modes of using a particular object as a key in a map,
* without having to subclass or wrap each object in its own proxy.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu May  5 12:51:03 EDT 2005
*/
public interface HashFunction
{
    /**
    * Returns true if o1.equals(o2) in some alternate universe,
    * where o1.equals() is defined differently.
    * All the usual notes about writing a good equals() apply here.
    * NB: behavior when o1 and/or o2 is null is implementation dependent,
    * although implementations should allow nulls whenever feasible.
    */
    public boolean areEqual(Object o1, Object o2);
    
    /**
    * Returns an integer hash code for o1, just like calling o1.hashCode --
    * except that it may not be the SAME code you would get that way.
    * All the usual notes about writing a sane hashCode() apply here.
    * You must ensure that if o1 and o2 are equal by the areEqual() function,
    * then this function returns the same hash code for both o1 and o2.
    * The reverse is not true: having the same hash code does not necessarily
    * imply two objects will compare as equal,
    * although the closer to true that is, the better.
    * NB: behavior when o1 is null is implementation dependent,
    * although implementations should allow nulls whenever feasible.
    */
    public int hashCodeFor(Object o1);
}//class

