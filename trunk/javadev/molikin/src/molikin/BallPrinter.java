// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

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
* <code>BallPrinter</code> generates kinemage output for things like
* atom markers, CPKs, and water/ion balls.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 11:03:30 EDT 2005
*/
public class BallPrinter //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.000");
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter out;
    AtomCrayon crayon = ConstCrayon.NONE;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BallPrinter(PrintWriter out)
    {
        super();
        this.out = out;
    }
//}}}

//{{{ printBalls
//##############################################################################
    /**
    * Draws the supplied AtomState objects in order,
    * but only if they belong to the given set of Residues (may be null).
    * Only points are generated; the client is responsible for writing "@balllist ...".
    */
    public void printBalls(Collection atoms, Set res)
    {
        for(Iterator iter = atoms.iterator(); iter.hasNext(); )
        {
            AtomState curr = (AtomState) iter.next();
            if(res == null || res.contains(curr.getResidue()))
            {
                out.println("{"+curr.getAtom()+"}"+crayon.colorAtom(curr)+" "+curr.format(df));
            }
        }
        
        out.flush();
    }
    
    /**
    * Draws the supplied AtomState objects in order.
    * Only points are generated; the client is responsible for writing "@balllist ...".
    */
    public void printBalls(Collection atoms)
    { printBalls(atoms, null); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

