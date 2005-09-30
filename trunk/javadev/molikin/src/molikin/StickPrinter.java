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
//}}}

//{{{ Constructor(s)
//##############################################################################
    public StickPrinter(PrintWriter out)
    {
        super();
        this.out = out;
    }
//}}}

//{{{ printSticks(bonds)
//##############################################################################
    /**
    * Draws the supplied Bond objects in order.
    * Only points are generated; the client is responsible for writing "@vectorlist ...".
    */
    public void printSticks(Collection bonds)
    {
        if(bonds.size() == 0) return;
        
        Bond last = new Bond(null, -1, null, -1);
        for(Iterator iter = bonds.iterator(); iter.hasNext(); )
        {
            Bond curr = (Bond) iter.next();
            if(curr.iLow != last.iHigh)
                out.print("{"+curr.lower.getAtom()+"}P "+curr.lower.format(df)+" ");
            out.println("{"+curr.higher.getAtom()+"}L "+crayon.colorBond(curr.lower, curr.higher)+" "+curr.higher.format(df));
            last = curr;
        }
        
        out.flush();
    }
//}}}

//{{{ printSticks(bonds, srcA, dstA)
//##############################################################################
    /**
    * Draws the supplied Bond objects in order.
    * Only bonds that go from AtomStates in srcA to AtomStates in dstA (or vice versa) are drawn.
    * Only points are generated; the client is responsible for writing "@vectorlist ...".
    * @param srcA   a Set of AtomStates
    * @param dstA   a Set of AtomStates
    */
    public void printSticks(Collection bonds, Set srcA, Set dstA)
    {
        if(bonds.size() == 0) return;
        
        Bond last = new Bond(null, -1, null, -1);
        for(Iterator iter = bonds.iterator(); iter.hasNext(); )
        {
            Bond curr = (Bond) iter.next();
            if((srcA.contains(curr.lower) && dstA.contains(curr.higher))
            || (dstA.contains(curr.lower) && srcA.contains(curr.higher)))
            {
                if(curr.iLow != last.iHigh)
                    out.print("{"+curr.lower.getAtom()+"}P "+curr.lower.format(df)+" ");
                out.println("{"+curr.higher.getAtom()+"}L "+crayon.colorBond(curr.lower, curr.higher)+" "+curr.higher.format(df));
                last = curr;
            }
        }
        
        out.flush();
    }
//}}}

//{{{ printSticks(bonds, srcA, dstA, srcR, dstR)
//##############################################################################
    /**
    * Draws the supplied Bond objects in order.
    * Only bonds that go from AtomStates in srcA which belong to Residues in srcR,
    * to AtomStates in dstR that belong to Residues in dstR (or vice versa), are drawn.
    * Only points are generated; the client is responsible for writing "@vectorlist ...".
    * @param srcA   a Set of AtomStates
    * @param dstA   a Set of AtomStates
    * @param srcR   a Set of Residues
    * @param dstR   a Set of Residues
    */
    public void printSticks(Collection bonds, Set srcA, Set dstA, Set srcR, Set dstR)
    {
        if(bonds.size() == 0) return;
        
        Bond last = new Bond(null, -1, null, -1);
        for(Iterator iter = bonds.iterator(); iter.hasNext(); )
        {
            Bond curr = (Bond) iter.next();
            if((srcA.contains(curr.lower) && dstA.contains(curr.higher) && srcR.contains(curr.lower.getResidue()) && dstR.contains(curr.higher.getResidue()))
            || (dstA.contains(curr.lower) && srcA.contains(curr.higher) && dstR.contains(curr.lower.getResidue()) && srcR.contains(curr.higher.getResidue())))
            {
                if(curr.iLow != last.iHigh)
                    out.print("{"+curr.lower.getAtom()+"}P "+curr.lower.format(df)+" ");
                out.println("{"+curr.higher.getAtom()+"}L "+crayon.colorBond(curr.lower, curr.higher)+" "+curr.higher.format(df));
                last = curr;
            }
        }
        
        out.flush();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

