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
* <code>Tuple3HashFunction</code> allows for comparing any Tuple3s by the rules
* used for Triple, even if the Tuple3s have a different natural equality test.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu May  5 13:22:15 EDT 2005
*/
public class Tuple3HashFunction //extends ... implements ...
{
//{{{ Constructor(s)
//##############################################################################
    public Tuple3HashFunction()
    {
        super();
    }
//}}}

//{{{ areEqual, hashCodeFor
//##############################################################################
    /**
    * Returns true iff o1 and o2 are Tuple3's
    * with EXACTLY the same X, Y, and Z coordinates.
    * @throws ClassCastException if o1 or o2 is not a Tuple3.
    */
    public boolean areEqual(Object o1, Object o2)
    {
        Tuple3 t1 = (Tuple3) o1, t2 = (Tuple3) o2;
        return (t1.getX() == t2.getX()
            &&  t1.getY() == t2.getY()
            &&  t1.getZ() == t2.getZ());
    }
    
    /**
    * Returns a hash code composed of a the int representations of the
    * X, Y, and Z coordinates as floats, permuted to avoid collisions of
    * e.g. (1,0,0) with (0,1,0) and (0,0,1), and XORed together.
    */
    public int hashCodeFor(Object o1)
    {
        Tuple3 t1 = (Tuple3) o1;
        // I stole this from Colt:
        //   this avoids excessive hashCollisions
        //   in the case values are of the form (1.0, 2.0, 3.0, ...)
        int b1 = Float.floatToIntBits(((float)t1.getX()) * 663608941.737f);
        int b2 = Float.floatToIntBits(((float)t1.getY()) * 663608941.737f);
        int b3 = Float.floatToIntBits(((float)t1.getZ()) * 663608941.737f);
        // The rotation of bits is my own idea, to avoid getting
        // the same result when X, Y, and Z are simply permuted.
        return (b1 ^ (b2<<11 | b2>>>21) ^ (b3<<22 | b3>>>10));
    }
//}}}
}//class

