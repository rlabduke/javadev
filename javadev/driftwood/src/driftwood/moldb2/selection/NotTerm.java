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
* <code>NotTerm</code> is the logical NOT (inverse) of another selection.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:33:28 PDT 2007
*/
public class NotTerm extends Selection
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Selection childTerm;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public NotTerm(Selection target)
    {
        super();
        this.childTerm = target;
    }
//}}}

//{{{ selectImpl, toString
//##############################################################################
    /**
    * Returns true iff the given AtomState should belong to this selection.
    */
    protected boolean selectImpl(AtomState as)
    {
        return !childTerm.select(as);
    }
    
    public String toString()
    { return "not ("+childTerm+")"; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

