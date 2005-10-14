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
* <code>BondOptimizer</code> is responsible for re-ordering Bond objects to
* minimize the number of polylines needed to draw them.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Oct 14 09:44:49 EDT 2005
*/
public class BondOptimizer //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    private BondOptimizer()
    {
        super();
    }
//}}}

//{{{ optimizeBondSequence
//##############################################################################
    /**
    * Rearranges bonds in order to fully minimize the number of
    * kinemage 'P' points needed to draw them,
    * although optimizations do not extend past residue boundaries.
    * Some bonds in the original list may be replaced by their mirror images.
    * This assumes the bonds are already sorted in their natural order.
    * This is pretty fast -- it takes maybe 1% of the time required to draw the bonds.
    */
    static public void optimizeBondSequence(Bond[] b)
    {
        //long time = System.currentTimeMillis();
        
        int first = 0, last; // for the current residue
        while(first < b.length)
        {
            // Define the range to optimize (one residue)
            Residue thisRes = b[first].lower.getResidue();
            for(last = first; last < b.length && (thisRes.equals(b[last].lower.getResidue()) || thisRes.equals(b[last].higher.getResidue())); last++) {}
            // We can now play with bonds first (inclusive) through last (exclusive)
            
            while(first < last)
            {
                // Find optimal starting point
                extendLeft(b, first, last);
                // Make this chain as long as we can
                first = extendRight(b, first, last);
            }
            // Now first == last
            // On to the next residue, or first == b.length if we're done
        }
        
        //time = System.currentTimeMillis() - time;
        //System.err.println(time+" ms optimizing bond order for "+b.length+" bonds");
        //for(int i = 0; i < b.length; i++) System.err.println(b[i].lower.getAtom()+" --- "+b[i].higher.getAtom());
    }
//}}}

//{{{ extendLeft
//##############################################################################
    /**
    * Optimizes the starting point of the polyline by insuring that it cannot
    * be further extended to the left.
    * @param curr   the site to optimize
    * @param last   the first bond that CANNOT be moved
    */
    static private void extendLeft(Bond[] b, int curr, int last)
    {
        OUTER_LOOP: while(true)
        {
            // Scan for bonds where curr.lower == x.higher
            for(int i = curr+1; i < last; i++)
            {
                if(b[curr].lower == b[i].higher)
                {
                    rotateRight(b, curr, i);
                    continue OUTER_LOOP;
                }
            }
            // If not, scan for bonds where curr.lower == x.lower and invert
            for(int i = curr+1; i < last; i++)
            {
                if(b[curr].lower == b[i].lower)
                {
                    b[i] = b[i].mirror;
                    rotateRight(b, curr, i);
                    continue OUTER_LOOP;
                }
            }
            // If we make it here, there's no way to further extend left.
            break;
        }
    }
//}}}

//{{{ extendRight
//##############################################################################
    /**
    * Optimizes the polyline by fully extending it to the right.
    * @param curr   the site to optimize
    * @param last   the first bond that CANNOT be moved
    * @return the first bond that could not be incorporated into the polyline;
    *   it will fall between curr+1 and last, inclusive.
    */
    static private int extendRight(Bond[] b, int curr, int last)
    {
        OUTER_LOOP: for( ; curr < last; curr++)
        {
            // If not, scan for bonds where curr.higher == x.lower
            for(int i = curr+1; i < last; i++)
            {
                if(b[curr].higher == b[i].lower)
                {
                    rotateRight(b, curr+1, i);
                    continue OUTER_LOOP;
                }
            }
            // If not, scan for bonds where curr.higher == x.higher and invert
            for(int i = curr+1; i < last; i++)
            {
                if(b[curr].higher == b[i].higher)
                {
                    b[i] = b[i].mirror;
                    rotateRight(b, curr+1, i);
                    continue OUTER_LOOP;
                }
            }
            // If we made it here, the polyline cannot be further extended.
            curr++;
            break;
        }
        return curr;
    }
//}}}

//{{{ rotateRight
//##############################################################################
    /**
    * Move items o[start]...o[end-1] to o[start+1]...o[end],
    * and take the old o[end] and put it in o[start].
    */
    static private void rotateRight(Object[] o, int start, int end)
    {
        if(start >= end) return;
        Object tmp = o[end];
        for(int i = end; i > start; i--)
            o[i] = o[i-1];
        o[start] = tmp;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

