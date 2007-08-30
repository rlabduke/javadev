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
* <code>ComboTerm</code> is the parent of AndTerm and OrTerm.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:58:57 PDT 2007
*/
abstract public class ComboTerm extends Selection
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    protected Collection childTerms;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ComboTerm(Selection first)
    {
        super();
        this.childTerms = new ArrayList();
        add(first);
    }
//}}}

//{{{ init, add, toString
//##############################################################################
    public void init(Collection atomStates)
    {
        super.init(atomStates);
        for(Iterator iter = childTerms.iterator(); iter.hasNext(); )
        {
            Selection s = (Selection) iter.next();
            s.init(atomStates);
        }
    }

    /**
    * Adds another term to the list managed by this selection.
    */
    public void add(Selection s)
    {
        childTerms.add(s);
    }
    
    public String toString(String sep)
    {
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for(Iterator iter = childTerms.iterator(); iter.hasNext(); )
        {
            if(first) first = false;
            else buf.append(sep);
            buf.append("(").append(iter.next()).append(")");
        }
        return buf.toString();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

