// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2.selection;

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
* <code>ResNameTerm</code> handles "atomXXXX" selections.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Aug 30 09:11:42 PDT 2007
*/
public class ResNameTerm extends Selection
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String name;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ResNameTerm(String name)
    {
        super();
        this.name = name.replace('_', ' ');
    }
//}}}

//{{{ selectImpl, toString
//##############################################################################
    protected boolean selectImpl(AtomState as)
    {
        return name.equals(as.getResidue().getName());
    }
    
    public String toString()
    {
        return "res"+name.replace(' ', '_');
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

