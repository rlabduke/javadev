// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
 * <code>Aspect</code> has not yet been documented.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Wed Dec  4 10:26:15 EST 2002
*/
public class Aspect //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Kinemage parent;
    Integer index;
    String  name;
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
        this.name = name;
        this.index = index;
    }
//}}}

//{{{ getName, getIndex
//##################################################################################################
    public String getName()
    { return name; }
    public Integer getIndex()
    { return index; }
//}}}

//{{{ selectedFromMenu
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void selectedFromMenu(ActionEvent ev)
    {
        if(parent != null) parent.notifyAspectSelected(this);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

