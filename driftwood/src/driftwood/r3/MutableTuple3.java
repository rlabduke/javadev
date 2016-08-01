// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.r3;

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
* <code>MutableTuple3</code> is a Tuple3 whose coordinates can be updated.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Mar 28 10:00:01 EST 2003
*/
public interface MutableTuple3 extends Tuple3
{
    /** Assigns a value to the first element of this tuple */
    public void setX(double x);
    /** Assigns a value to the second element of this tuple */
    public void setY(double y);
    /** Assigns a value to the third element of this tuple */
    public void setZ(double z);
    /** Assigns a value to all the elements of this tuple */
    public void setXYZ(double x, double y, double z);
}//class

