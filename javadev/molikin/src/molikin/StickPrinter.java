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
import driftwood.r3.Triple;
//}}}
/**
* <code>StickPrinter</code> generates kinemage output for stick renderings
* of molecules.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 09:47:09 EDT 2005
*/
public class StickPrinter //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.000");
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter out;
    BondCrayon crayon = ConstCrayon.NONE;
    
    boolean halfbonds = false; // draw half bonds instead of whole ones
    Triple midpoint = new Triple(); // for half bond calculation
//}}}

//{{{ Constructor(s)
//##############################################################################
    public StickPrinter(PrintWriter out)
    {
        super();
        this.out = out;
    }
//}}}

//{{{ printSticks
//##############################################################################
    /**
    * Draws the supplied Bond objects in order.
    * Only bonds that go from AtomStates in srcA which belong to Residues in srcR,
    * to AtomStates in dstR that belong to Residues in dstR (or vice versa), are drawn.
    * Only points are generated; the client is responsible for writing "@vectorlist ...".
    * @param srcA   a Set of AtomStates (may be null for "any")
    * @param dstA   a Set of AtomStates (may be null for "any")
    * @param srcR   a Set of Residues (may be null for "any")
    * @param dstR   a Set of Residues (may be null for "any")
    */
    public void printSticks(Collection bonds, Set srcA, Set dstA, Set srcR, Set dstR)
    {
        Bond last = new Bond(null, -1, null, -1);
        for(Iterator iter = bonds.iterator(); iter.hasNext(); )
        {
            Bond curr = (Bond) iter.next();
            
            // Testing for null vs. maintaining separate implementations that don't test at all
            // produces no measurable performance impact, even for the ribosome.
            
            boolean residuesAllowed = ((srcR == null || srcR.contains(curr.lower.getResidue())) && (dstR == null || dstR.contains(curr.higher.getResidue())))
                                    ||((dstR == null || dstR.contains(curr.lower.getResidue())) && (srcR == null || srcR.contains(curr.higher.getResidue())));
            if(!residuesAllowed) continue;
            
            boolean atomsAllowed    = ((srcA == null || srcA.contains(curr.lower)) && (dstA == null || dstA.contains(curr.higher)))
                                    ||((dstA == null || dstA.contains(curr.lower)) && (srcA == null || srcA.contains(curr.higher)));
            if(!atomsAllowed) continue;
            
            if(curr.iLow != last.iHigh)
                out.print("{"+curr.lower.getAtom()+"}P "+curr.lower.format(df)+" ");
            if(halfbonds) // insignificant speed penalty to check in-line
            {
                midpoint.likeMidpoint(curr.lower, curr.higher);
                out.print("{mid}U "+crayon.colorBond(curr.lower, curr.higher)+" "+midpoint.format(df)+" ");
            }
            out.println("{"+curr.higher.getAtom()+"}L "+crayon.colorBond(curr.higher, curr.lower)+" "+curr.higher.format(df));
            last = curr;
        }
        
        out.flush();
    }
    
    public void printSticks(Collection bonds, Set srcA, Set dstA)
    { printSticks(bonds, srcA, dstA, null, null); }
    
    public void printSticks(Collection bonds)
    { printSticks(bonds, null, null, null, null); }
//}}}

//{{{ get/setHalfBonds, get/setCrayon
//##############################################################################
    /** Whether this will print midpoints for bonds or directly from atom to atom. */
    public boolean getHalfBonds()
    { return this.halfbonds; }
    /** Whether this will print midpoints for bonds or directly from atom to atom. */
    public void setHalfBonds(boolean b)
    { this.halfbonds = b; }
    
    /** The BondCrayon used for coloring these sticks. */
    public BondCrayon getCrayon()
    { return this.crayon; }
    /** The BondCrayon used for coloring these sticks. */
    public void setCrayon(BondCrayon c)
    { this.crayon = c; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

