// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.crayons;
import molikin.*;

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
* <code>DisulfideCrayon</code> colors Cys-S---S-Cys bonds yellow.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct 19 13:11:10 EDT 2006
*/
public class DisulfideCrayon extends AbstractCrayon implements BondCrayon
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean isYellow;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DisulfideCrayon()
    {
        super();
    }
//}}}

//{{{ forBond
//##############################################################################
    public void forBond(AtomState from, AtomState toward)
    {
        isYellow = from.getElement().equals("S") && toward.getElement().equals("S")
            && from.getResidue().getName().equals("CYS") && toward.getResidue().getName().equals("CYS");
    }
//}}}

//{{{ getColor, getKinString
//##############################################################################
    public String getColor()
    { return isYellow ? "yellow" : null; }
    
    public String getKinString()
    { return isYellow ? "yellow" : ""; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

