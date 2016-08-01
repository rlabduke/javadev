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
* <code>AndTerm</code> is a logical AND (intersection) of other selections.
* It will short-circuit on the first selection that returns false.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:33:28 PDT 2007
*/
public class AndTerm extends ComboTerm
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AndTerm(Selection first)
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
            if( !s.select(as) ) return false;
        }
        return true; // nobody returned false, so it must be true!
    }
    
    public String toString()
    { return super.toString(" and "); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

