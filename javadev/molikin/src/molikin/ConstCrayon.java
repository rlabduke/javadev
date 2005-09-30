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
* <code>ConstCrayon</code> is an Atom/BondCrayon that always returns the same
* string, regardless of input.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 11:41:50 EDT 2005
*/
public class ConstCrayon implements AtomCrayon, BondCrayon
{
//{{{ Constants
    /** A crayon that always returns the empty string ("") */
    public static final ConstCrayon NONE = new ConstCrayon("");
//}}}

//{{{ Variable definitions
//##############################################################################
    String color;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ConstCrayon(String color)
    {
        super();
        this.color = color;
    }
//}}}

//{{{ colorAtom, colorBond
//##############################################################################
    public String colorAtom(AtomState as)
    { return color; }
    
    public String colorBond(AtomState from, AtomState toward)
    { return color; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

