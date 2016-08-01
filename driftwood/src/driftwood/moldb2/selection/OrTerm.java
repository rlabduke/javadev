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
* <code>OrTerm</code> is a logical OR (union) of other selections.
* It will short-circuit on the first selection that returns true.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:33:28 PDT 2007
*/
public class OrTerm extends ComboTerm
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public OrTerm(Selection first)
    {
        super(first);
    }
//}}}

//{{{ selectImpl, toString
//##############################################################################
    /**
    * Returns true iff the given AtomState should belong to this selection.
    */
    protected boolean selectImpl(AtomState as)
    {
        for(Iterator iter = this.childTerms.iterator(); iter.hasNext(); )
        {
            Selection s = (Selection) iter.next();
            if( s.select(as) ) return true;
        }
        return false; // nobody returned true, so it must be false!
    }
    
    public String toString()
    { return super.toString(" or "); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

