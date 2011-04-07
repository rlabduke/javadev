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
* <code>KeywordTerm</code> contains the code for all the simple Probe keywords.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 16:42:16 PDT 2007
*/
public class KeywordTerm //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ CLASS: NamedTerm
//##############################################################################
    static abstract class NamedTerm extends Selection
    {
        String name;
        
        public NamedTerm(String name)
        {
            this.name = name;
        }
        
        public String toString()
        {
            return name;
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    private KeywordTerm()
    {
        super();
    }
//}}}

//{{{ get
//##############################################################################
    static Map map = null;
    static public Selection get(String keyword)
    {
        if(map == null)
        {
            map = new HashMap();
            map.put("all", new NamedTerm("all") { protected boolean selectImpl(AtomState as) {
                    return true;
            }});
            map.put("*", map.get("all"));
            map.put("none", new NamedTerm("none") { protected boolean selectImpl(AtomState as) {
                    return false;
            }});
            map.put("het", new NamedTerm("het") { protected boolean selectImpl(AtomState as) {
                    return as.isHet();
            }});
            map.put("carbon", new NamedTerm("carbon") { protected boolean selectImpl(AtomState as) {
                    return "C".equals(as.getElement());
            }});
            map.put("hydrogen", new NamedTerm("hydrogen") { protected boolean selectImpl(AtomState as) {
                    return "H".equals(as.getElement());
            }});
            map.put("nitrogen", new NamedTerm("nitrogen") { protected boolean selectImpl(AtomState as) {
                    return "N".equals(as.getElement());
            }});
            map.put("oxygen", new NamedTerm("oxygen") { protected boolean selectImpl(AtomState as) {
                    return "O".equals(as.getElement());
            }});
            map.put("phosphorus", new NamedTerm("phosphorus") { protected boolean selectImpl(AtomState as) {
                    return "P".equals(as.getElement());
            }});
            map.put("sulfur", new NamedTerm("sulfur") { protected boolean selectImpl(AtomState as) {
                    return "S".equals(as.getElement());
            }});
            map.put("alpha", new SecondaryStructureTerm("alpha"));
            map.put("beta", new SecondaryStructureTerm("beta"));
        }
        return (Selection) map.get(keyword); // null if not found
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

