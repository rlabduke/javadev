// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>Aspect</code> is a lightweight class used for kinemage "aspects",
* a kind of alternative point color.
* Aspects specify color on a point-by-point basis, but there may be many different
* aspects in one kinemage.
* They are used for "color-by" schemes, where the same objects may be colored
* differently based on different sets of criteria.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Dec  4 10:26:15 EST 2002
*/
public class Aspect //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    protected Kinemage  parent;
    protected String    name;
    protected Integer   index;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @param parent the kinemage this aspect belongs to.
    * @param name   a descriptive name for this aspect.
    * @param index  the index of this aspect in the aspect string,
    *               where the first position is index 1.
    */
    public Aspect(Kinemage parent, String name, Integer index)
    {
        this.parent = parent;
        this.name   = name;
        this.index  = index;
    }
//}}}

//{{{ getName, getIndex
//##################################################################################################
    public String getName()
    { return name; }
    public Integer getIndex()
    { return index; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

