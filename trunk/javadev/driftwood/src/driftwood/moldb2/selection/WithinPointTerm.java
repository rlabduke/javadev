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
import driftwood.r3.*;
//}}}
/**
* <code>WithinPointTerm</code> handles "within DIST of X, Y, Z" statements.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:33:28 PDT 2007
*/
public class WithinPointTerm extends Selection
{
//{{{ Constants
    static final private DecimalFormat df = new DecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##############################################################################
    double      distance;
    double      sqDistance;
    Triple      center;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public WithinPointTerm(double distance, double x, double y, double z)
    {
        super();
        this.distance = distance;
        this.sqDistance = distance * distance;
        this.center = new Triple(x,y,z);
    }
//}}}

//{{{ selectImpl, toString
//##############################################################################
    /**
    * Returns true iff the given AtomState should belong to this selection.
    */
    protected boolean selectImpl(AtomState as)
    {
        return center.sqDistance(as) <= sqDistance;
    }
    
    public String toString()
    { return "within "+df.format(distance)+" of "+df.format(center.getX())+", "+df.format(center.getY())+", "+df.format(center.getZ()); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

