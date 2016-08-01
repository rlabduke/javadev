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
* <code>ValueComparator</code> wraps another Comparator and
* applies it to the <b>VALUEs</b> of Map.Entry objects.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed May 25 09:49:29 EDT 2005
*/
public class ValueComparator implements Comparator//extends ... implements ...
{
    private final Comparator cmp;

    public ValueComparator(Comparator cmp)
    {
        super();
        this.cmp = cmp;
    }
    
    /** Returns the result of compare() for the values of the Map.Entry objects. */
    public int compare(Object o1, Object o2)
    {
        Map.Entry e1 = (Map.Entry) o1;
        Map.Entry e2 = (Map.Entry) o2;
        return cmp.compare(e1.getValue(), e2.getValue());
    }
}//class

