// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.nmr;

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
* <code>NoeConstraint</code> is a class for modeling NOE
* (Nuclear Overhauser Effect) distance constraints between
* two atoms, which may or may not be unambiguously identified.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul  9 16:08:05 EDT 2003
*/
public class NoeConstraint //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Atom[]              atoms1;
    Atom[]              atoms2;
    double              distance;
    
    /** One of the atom states in the best-fit pair after calling findBest() */
    AtomState           best1 = null, best2 = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * @throws IllegalArgumentException if either atoms1 or atoms2 has less than one entry
    */
    public NoeConstraint(Collection atoms1, Collection atoms2, double distance)
    {
        super();
        
        if(atoms1 == null || atoms1.size() < 1
        || atoms2 == null || atoms2.size() < 1)
            throw new IllegalArgumentException("Not enough atoms (null or length=0)");
        
        this.atoms1     = (Atom[])atoms1.toArray(new Atom[atoms1.size()]);
        this.atoms2     = (Atom[])atoms2.toArray(new Atom[atoms2.size()]);
        this.distance   = distance;
    }
//}}}

//{{{ findBest
//##############################################################################
    /**
    * Finds the pair of atoms, based on the current model,
    * that are closest to satisfying the distance constraint.
    * After calling this method, those atoms are retrieved via 
    * <code>getBest1</code> and <code>getBest2</code>.
    * @return the actual distance between the best-fit pair
    */
    public double findBest(ModelState ms)
    {
        int i, j;
        AtomState as1, as2;
        double dist, gap;
        double bestGap = Double.MAX_VALUE, bestDist = Double.MAX_VALUE;
        
        for(i = 0; i < atoms1.length; i++)
        {
            as1 = ms.get(atoms1[i]);
            for(j = 0; j < atoms2.length; j++)
            {
                as2     = ms.get(atoms2[j]);
                dist    = as1.distance(as2);
                gap     = Math.abs(this.distance - dist);
                if(gap < bestGap)
                {
                    bestGap     = gap;
                    bestDist    = dist;
                    best1       = as1;
                    best2       = as2;
                }
            }
        }
        
        return bestDist;                
    }
//}}}

//{{{ get{Distance, Best1/2, Centroid1/2}
//##############################################################################
    /** Returns the ideal distance for this constraint. */
    public double getDistance()
    { return distance; }
    
    /** Gives one of the two atoms that together best satisfy the constraint */
    public AtomState getBest1()
    { return best1; }
    
    /** Gives one of the two atoms that together best satisfy the constraint */
    public AtomState getBest2()
    { return best2; }
    
    /** Gives the centroid position of all atoms considered for end 1 */
    public Triple getCentroid1(ModelState ms)
    { return calcCentroid(atoms1, ms); }

    /** Gives the centroid position of all atoms considered for end 2 */
    public Triple getCentroid2(ModelState ms)
    { return calcCentroid(atoms2, ms); }
//}}}

//{{{ calcCentroid
//##############################################################################
    /** Calculates the centroid of a set of atoms */
    private Triple calcCentroid(Atom[] atoms, ModelState ms)
    {
        Triple t = new Triple();
        for(int i = 0; i < atoms.length; i++)
        {
            AtomState as = ms.get(atoms[i]);
            t.add(as);
        }
        t.mult( 1.0 / atoms.length );
        return t;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

