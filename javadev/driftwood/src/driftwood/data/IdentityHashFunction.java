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
* <code>IdentityHashFunction</code> implements identity (==) as the equality test.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May 10 15:17:02 EDT 2005
*/
public class IdentityHashFunction implements HashFunction
{
   public IdentityHashFunction()
    {
        super();
    }

    /** Returns (o1 == o2) */
    public boolean areEqual(Object o1, Object o2)
    {
        return (o1 == o2);
    }
    
    /** Returns System.identityHashCode(o1). */
    public int hashCodeFor(Object o1)
    {
        return System.identityHashCode(o1);
    }
}//class

