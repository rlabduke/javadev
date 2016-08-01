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
* <code>ReverseComparator</code> wraps another Comparator and reverses its ordering.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed May 25 09:49:29 EDT 2005
*/
public class ReverseComparator implements Comparator//extends ... implements ...
{
    private final Comparator cmp;

    public ReverseComparator(Comparator cmp)
    {
        super();
        this.cmp = cmp;
    }
    
    /** Returns the result of compare(o1,o2) for the wrapped comparator, negated. */
    public int compare(Object o1, Object o2)
    { return -cmp.compare(o1, o2); }
}//class

