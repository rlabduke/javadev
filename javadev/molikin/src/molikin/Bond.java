// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>Bond</code> describes a connection of some type between two AtomStates,
* which must be indentified by unique integers.
* Given atoms 1, 2, and 3 with bonds between them,
* bonds 1-2 and 2-1 compare as equal,
* and they sort as (1-3, 1-2, 2-3).
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May  6 14:03:37 EDT 2005
*/
public class Bond implements Comparable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    public final AtomState lower, higher;
    final int iLow, iHigh;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * @param one    an atom state associated with index number iOne. This field
    *   doesn't matter at all to Bond and can be null; it's only for the
    *   convenience of other users.
    * @param iOne   a UNIQUE serial number for this AtomState that will be used
    *   to identify it and for sorting purposes.
    * @param two    (see one)
    * @param iTwo   (see iOne)
    */
    public Bond(AtomState one, int iOne, AtomState two, int iTwo)
    {
        super();
        if(iOne > iTwo)
        {
            lower   = two;
            iLow    = iTwo;
            higher  = one;
            iHigh   = iOne;
        }
        else
        {
            lower   = one;
            iLow    = iOne;
            higher  = two;
            iHigh   = iTwo;
        }
    }
//}}}

//{{{ equals, hashCode, compareTo
//##############################################################################
    /**
    * Two bonds compare as equal if compareTo() == 0, that is, if their
    * end points have the same serial numbers.
    */
    public boolean equals(Object o)
    { return this.compareTo(o) == 0; }
    
    public int hashCode()
    { return this.iLow << 16 | this.iHigh; }
    
    /**
    * First compares on the lower of the two serial numbers,
    * then on the higher of them.
    */
    public int compareTo(Object o)
    {
        Bond that = (Bond) o;
        int diff = this.iLow - that.iLow;
        if(diff == 0)
            diff = that.iHigh - this.iHigh; // reverse order!
        return diff;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

