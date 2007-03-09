// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.r3;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>UltrafastShapeRecognition</code> implements the "USR" algorithm from
* Ballester and Richards, Proc Royal Soc A, 2007.
* It does shape matching of point sets (originally small molecules) by
* choosing several reference points and then calculating the average distance,
* variance, and skew of the distances from the references to all other atoms.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Mar  9 14:36:01 EST 2007
*/
public class UltrafastShapeRecognition //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    double[] queryMoments;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a USR object for repeated testing of many things
    * against a single "query" structure.
    * This just saves the calculations for this structure, making repeated
    * tests faster.
    * @param ref    reference points for the query structure
    * @param pts    normal points for the reference structure
    */
    public UltrafastShapeRecognition(Collection ref, Collection pts)
    {
        super();
        this.queryMoments = new double[ref.size() * 3];
        int i = 0;
        for(Iterator iter = ref.iterator(); iter.hasNext(); i++)
            calcThreeMoments((Tuple3) iter.next(), pts, queryMoments, i*3);
    }

    /**
    * Creates a USR object for repeated testing of many things
    * against a single "query" structure.
    * This just saves the calculations for this structure, making repeated
    * tests faster.
    * @param pts    normal points for the reference structure
    */
    public UltrafastShapeRecognition(Collection pts)
    { this(getReferencePoints(pts), pts); }
//}}}

//{{{ calcThreeMoments
//##############################################################################
    /**
    * Calculates the distances from ref to all points in pts, then finds
    * the first, second, and third moments of the distribution of distances.
    *
    * This is one of the atomic steps in the USR algorithm.
    *
    * @param ref    the reference point from which distances are calculated
    * @param pts    a set of Tuple3s to which distances are calculated
    * @param out    the first, second, and third moments are written here
    * @param offset where to start writing the moments
    */
    private static void calcThreeMoments(Tuple3 ref, Collection pts, double[] out, int offset)
    {
        // General formula for moments k >= 2:
        // u_k = < (x - <x>)^k >
        double[] d = new double[pts.size()];
        double d_avg = 0;
        int i = 0;
        for(Iterator iter = pts.iterator(); iter.hasNext(); i++)
        {
            Tuple3 pt = (Tuple3) iter.next();
            d[i] = Triple.distance(ref, pt);
            d_avg += d[i];
        }
        
        d_avg /= d.length;
        double u2 = 0, u3 = 0;
        for(i = 0; i < d.length; i++)
        {
            double dd = d[i] - d_avg;
            double dd2 = dd*dd;
            u2 += dd2;
            u3 += dd2 * dd;
        }
        
        out[offset  ] = d_avg;
        out[offset+1] = u2 / d.length;
        out[offset+2] = u3 / d.length;
    }
//}}}

//{{{ scoreUSR
//##############################################################################
    /**
    * Given two lists of moments, score them according to the USR scheme.
    * This is one of the atomic steps in the USR algorithm.
    * @param m1     moments for one point cloud
    * @param m2     moments for another point cloud
    * @return a number between 0 (dissimilar) and 1 (very similar)
    */
    private static double scoreUSR(double[] m1, double[] m2)
    {
        int len = m1.length;
        if(len != m2.length)
            throw new IllegalArgumentException("must have same number of moments (reference points) to compare");
        // inverse of (average Manhattan distance + 1)
        double s = 0;
        for(int i = 0; i < len; i++)
            s += Math.abs(m1[i] - m2[i]);
        return 1.0 / ( 1.0 + s/len );
    }
    
    /** Scores two pre-computed shapes against each other */
    public double scoreUSR(UltrafastShapeRecognition that)
    { return scoreUSR(this.queryMoments, that.queryMoments); }
//}}}

//{{{ getReferencePoints, getByDistance
//##############################################################################
    private static Collection getReferencePoints(Collection pts)
    {
        ArrayList ref = new ArrayList();
        Triple ctd = new Triple();
        for(Iterator iter = pts.iterator(); iter.hasNext(); )
            ctd.add((Tuple3) iter.next());
        ctd.div(pts.size());
        
        Tuple3 cst = getByDistance(ctd, pts, true);     // closest to ctd
        Tuple3 fct = getByDistance(cst, pts, false);    // farthest from cst
        Tuple3 ftf = getByDistance(fct, pts, false);    // farthest from fct
        
        ref.add(ctd);
        ref.add(cst);
        ref.add(fct);
        ref.add(ftf);
        
        return ref;
    }
    
    private static Tuple3 getByDistance(Tuple3 target, Collection pts, boolean nearest)
    {
        // find nearest to or farthest from target, depending on flag
        final double sign = (nearest ? 1 : -1);

        Tuple3 bestPt = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for(Iterator iter = pts.iterator(); iter.hasNext(); )
        {
            Tuple3 pt = (Tuple3) iter.next();
            double dist = sign * Triple.sqDistance(target, pt);
            if(dist < bestDist)
            {
                bestPt = pt;
                bestDist = dist;
            }
        }
        
        return bestPt;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

