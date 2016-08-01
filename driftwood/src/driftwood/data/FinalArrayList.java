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
* <code>FinalArrayList</code> is a very simple, lightweight wrapper
* to make an array into an <b>unmodifiable</b> list.
* The list is backed by the original array and is thus still potentially
* mutable if the array is modified.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed May 19 09:19:51 EDT 2004
*/
public class FinalArrayList extends AbstractList
{
    protected Object[] data;

    public FinalArrayList(Object[] a)
    {
        super();
        if(a == null) throw new NullPointerException("Must provide a non-null array");
        this.data = a;
    }
    
    public Object get(int i)
    { return data[i]; }
    
    public int size()
    { return data.length; }
}//class

