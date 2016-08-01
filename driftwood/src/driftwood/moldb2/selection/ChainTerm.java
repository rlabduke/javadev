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
* <code>ChainTerm</code> handles "chainX" selections.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Aug 30 09:11:42 PDT 2007
*/
public class ChainTerm extends Selection
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String name;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ChainTerm(String name)
    {
        super();
        this.name = name.replace('_', ' ');
    }
//}}}

//{{{ selectImpl, toString
//##############################################################################
    protected boolean selectImpl(AtomState as)
    {
        return name.equals(as.getResidue().getChain());
    }
    
    public String toString()
    {
        return "chain"+name.replace(' ', '_');
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

