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
* <code>NRUBS</code> implements non-rational, uniform B-splines, a much simpler
* thing than NURBS (non-uniform rational B-splines).
*
* <p>My reference was Computer Graphics: Principles and Practice, 2nd ed.
* in the Systems Programming Series by Foley, van Dam, Feiner, and Hughes (1993).
* With some work, I could probably use it to implement NURBS (which are a
* generalization of NRUBS, on two counts), but since Prekin uses the plain
* B-splines for ribbons, I don't see the point.
*
* <p>Here's my simplified layout of the math of splines. <!-- {{{ -->
* B-splines require 4 control points per segment, and adjacent segments
* share 3 of the 4 control points.
* For simplicity, I deal with only one segment at a time here,
* with control points P0, P1, P2, P3.
* Segments are C2 continuous at join points, meaning their second derivatives
* are equal. Doubling or tripling guide points may break this.
* The points along the actual spline are given as Q(t), where t is on [0,1].
* Although the Q(t) don't actually pass through any of the control points,
* they do lie within the convex hull of those points.
* You can make the spline go closer by doubling up a control point, but if
* three of them are the same, then that spline segment will be a straight line.
*
<p><pre>
t                               the parameter for spline coordinates

Q(t) = [x(t) y(t) z(t)]         the functions of t that give spline coordinates

    [P0]   [P0_x P0_y P0_z]
G = [P1] = [P1_x P1_y P1_z]     the control points (guide points)
    [P2]   [P2_x P2_y P2_z]
    [P3]   [P3_x P3_y P3_z]

            [-1  3 -3  1]
M = (1/6) * [ 3 -6  3  0]       the basis matrix, which determines blending functions
            [-3  0  3  0]
            [ 1  4  1  0]

T = [t^3  t^2  t  1]            powers of t -- makes this a CUBIC spline function

Q(t) = T * M * G                the spline function itself
     = T * C                        alternate representation: C = M * G
     = B * G                        alternate representation: B = T * M
</pre>
*
* <p>Notice that if you're going to do a bunch of segments with the same
* subdivisions of t (e.g. t = [0.00  0.25  0.50  0.75  1.00]), it makes sense
* to precalculate all the B(t) you need. This gives:
<pre>
Q(t) = (1/6) * {  [(1-t)^3]*P0 + [3t^3 - 6t^2 + 4]*P1 + [-3t^3 + 3t^2 + 3t + 1]*P2 + [t^3]*P3  }
</pre>
* <!-- }}} -->
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jan 19 16:56:14 EST 2006
*/
public class NRUBS //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Map Bcache = new HashMap(); // <Integer, double[][]>
    Triple work = new Triple();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public NRUBS()
    {
        super();
    }
//}}}

//{{{ spline [public]
//##############################################################################
    /**
    * Generates the points along a spline.  If you feed in G guide points and
    * request N intervals in each segment, you'll get back N*(G-3) + 1 points.
    */
    public Tuple3[] spline(Tuple3[] guidePts, int nIntervals)
    {
        double[][]  B   = getB(nIntervals);
        Triple[]    out = new Triple[ nIntervals*(guidePts.length-3) + 1 ];
        for(int i = 0; i < out.length; i++) out[i] = new Triple();
        
        for(int i = 0; i < guidePts.length - 3; i++)
            spline(guidePts, i, out, i*nIntervals, B);
        
        return out;
    }
//}}}

//{{{ spline [workhorse]
//##############################################################################
    /**
    * Calculates the N+1 points along the spline for a particular segment.
    * Using this repeatedly along a series of guidepoints will result in small
    * amount of duplicate calculation (each join calc'd twice).
    */
    private void spline(Tuple3[] guidePts, int guideStart, MutableTuple3[] splineOut, int splineStart, double[][] B)
    {
        Tuple3  P0 = guidePts[guideStart+0],
                P1 = guidePts[guideStart+1],
                P2 = guidePts[guideStart+2],
                P3 = guidePts[guideStart+3];
        for(int i = 0; i < B.length; i++)
        {
            work.setXYZ(0,0,0);
            work.addMult(B[i][0], P0);
            work.addMult(B[i][1], P1);
            work.addMult(B[i][2], P2);
            work.addMult(B[i][3], P3);
            splineOut[splineStart+i].setXYZ(work.getX(), work.getY(), work.getZ());
        }
    }
//}}}

//{{{ getB, calculateB
//##############################################################################
    /**
    * Returns the "B" coefficients for a particular number N of intervals
    * along a spline segment. Because N+1 points are generated (fencepost problem)
    * this function returns a double[N+1][4].
    *
    * If possible, the value will be retrieved from cache instead of recalculated.
    */
    private double[][] getB(int nIntervals)
    {
        Integer N = new Integer(nIntervals);
        double[][] B = (double[][]) Bcache.get(N);
        if(B == null)
        {
            B = calculateB(nIntervals);
            Bcache.put(N, B);
        }
        return B;
    }

    /**
    * Returns the "B" coefficients for a particular number N of intervals
    * along a spline segment. Because N+1 points are generated (fencepost problem)
    * this function returns a double[N+1][4].
    */
    static private double[][] calculateB(int nIntervals)
    {
        double[][] B = new double[nIntervals+1][4];
        for(int i = 0; i <= nIntervals; i++)
        {
            // This could be rearranged to be more efficient, but it doesn't
            // matter because we're going to cache the result.
            double t = ((double)i) / ((double)nIntervals);
            double t2 = t*t;
            double t3 = t2*t;
            double _1_t = 1-t;
            B[i][0] = (_1_t * _1_t * _1_t) / 6;
            B[i][1] = (3*t3 - 6*t2 + 4) / 6;
            B[i][2] = (-3*t3 + 3*t2 + 3*t + 1) / 6;
            B[i][3] = t3 / 6;
        }
        return B;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

