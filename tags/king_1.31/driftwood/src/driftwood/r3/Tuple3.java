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
* <code>Tuple3</code> is a point in <b>R<sup>3</sup></b>,
* a tuple with an X, a Y, and a Z component.
* This interface allows other things to act like Triples
* without having to actually be Triples.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 10 12:12:11 EST 2003
*/
public interface Tuple3 //extends ... implements ...
{
    /** Returns the first element of this tuple */
    public double getX();
    /** Returns the second element of this tuple */
    public double getY();
    /** Returns the third element of this tuple */
    public double getZ();
}//class

