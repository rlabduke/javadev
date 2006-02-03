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
    BondCrayon  crayon  = ConstCrayon.NONE;
    AtomIDer    ider    = new PrekinIDer();
    
    boolean halfbonds = false; // draw half bonds instead of whole ones
    Triple midpoint = new Triple(); // for half bond calculation
    Collection selectedBonds = new ArrayList();
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
        //long time = System.currentTimeMillis();

        // Doing the selection inline saves a bit of time on allocating selectedBonds,
        // but much greater savings are achieved through bond order optimization.
        selectedBonds.clear();
        Util.selectBondsBetween(bonds, srcA, dstA, srcR, dstR, selectedBonds);
        // The optimization reduces total bond drawing time by ~20%
        // because it reduces kinemage size by ~15%. Less output, faster code!
        Bond[] b = (Bond[]) selectedBonds.toArray(new Bond[selectedBonds.size()]);
        Bond.optimizeBondSequence(b);
        
        Bond last = new Bond(null, -1, null, -1);
        for(int i = 0; i < b.length; i++)
        {
            Bond curr = b[i];
            String higherColor = crayon.colorBond(curr.higher, curr.lower);
            
            if(curr.lower != last.higher)
                out.print("{"+ider.identifyAtom(curr.lower)+"}P "+curr.lower.format(df)+" ");
            if(halfbonds) // insignificant speed penalty to check in-line
            {
                // Draw the midpoint only if we change color / pointmasters / etc.
                // Is this really wise? It might make editing harder later on...
                String lowerColor = crayon.colorBond(curr.lower, curr.higher);
                if(!lowerColor.equals(higherColor))
                {
                    midpoint.likeMidpoint(curr.lower, curr.higher);
                    out.print("{mid}U "+lowerColor+" "+midpoint.format(df)+" ");
                }
            }
            out.println("{"+ider.identifyAtom(curr.higher)+"}L "+higherColor+" "+curr.higher.format(df));
            last = curr;
        }
        out.flush();
        
        //time = System.currentTimeMillis() - time;
        //System.err.println("Drawing bonds:          "+time+" ms");
    }
    
    public void printSticks(Collection bonds, Set srcA, Set dstA)
    { printSticks(bonds, srcA, dstA, null, null); }
    
    public void printSticks(Collection bonds)
    { printSticks(bonds, null, null, null, null); }
//}}}

//{{{ get/setHalfBonds, get/setCrayon, get/setAtomIDer
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
    
    /** The AtomIDer used to make point IDs. */
    public AtomIDer getAtomIDer()
    { return this.ider; }
    /** The AtomIDer used to make point IDs. */
    public void setAtomIDer(AtomIDer ai)
    { this.ider = ai; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

