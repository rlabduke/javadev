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
//}}}
/**
* <code>SuperPoser</code> provides the algorithms to superimpose
* (part of) one protein structure on another.
* Actually, you can use it for any set of 3-D points you want
* to superposition, but protein C-alphas is most common.
*
* <p>Copyright (C) 2003-2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Sep 15 10:04:36 EDT 2003
*/
public class SuperPoser //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    int         len;
    Triple[]    ref;
    Triple[]    mob;
    /**
    * This is just the default uniform (1.0) weighting.
    * Using non-uniform weights, especially zero, raises questions about
    * how to calculate the centroid, the RMSD, and the superposition itself.
    * Either these weren't addressed in the paper, or I didn't pick up on it;
    * either way, the old behavior was probably not correct.
    */
    double[]    w;
    Triple      refCentroid;
    Triple      mobCentroid;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SuperPoser(Tuple3[] m1, Tuple3[] m2)
    { this(m1, 0, m2, 0, Math.max(m1.length, m2.length)); }
    
    /**
    * Creates a new superpositioner.
    * @param m1     the static or reference structure
    * @param off1   the index of the first point to use from m1
    * @param m2     the rotating or mobile structure
    * @param off2   the index of the first point to use from m1
    * @param l      the number of points to use from m1 and m2
    * @throws IllegalArgumentException if there aren't enough points in m1 or m2
    */
    public SuperPoser(Tuple3[] m1, int off1, Tuple3[] m2, int off2, int l)
    {
        super();
        reset(m1, off1, m2, off2, l);
    }
//}}}

//{{{ reset
//##############################################################################
    public void reset(Tuple3[] m1, Tuple3[] m2)
    { reset(m1, 0, m2, 0, Math.max(m1.length, m2.length)); }
    
    /**
    * Resets this superpositioner with new point sets.
    * Can save on object allocation if they're the same size
    * or smaller than before.
    * @param m1     the static or reference structure
    * @param off1   the index of the first point to use from m1
    * @param m2     the rotating or mobile structure
    * @param off2   the index of the first point to use from m1
    * @param l      the number of points to use from m1 and m2
    * @throws IllegalArgumentException if there aren't enough points in m1 or m2
    */
    public void reset(Tuple3[] m1, int off1, Tuple3[] m2, int off2, int l)
    {
        if(off1+l > m1.length || off2+l > m2.length)
            throw new IllegalArgumentException("Not enough points in m1 and/or m2");
        
        int i;
        len = l;
        if(ref == null || ref.length < len)
        {
            ref = new Triple[len];
            mob = new Triple[len];
            w = new double[len];
            for(i = 0; i < len; i++)
            {
                ref[i] = new Triple( m1[i+off1] );
                mob[i] = new Triple( m2[i+off2] );
                w[i] = 1.0;
            }
        }
        else
        {
            for(i = 0; i < len; i++)
            {
                ref[i].like( m1[i+off1] );
                mob[i].like( m2[i+off2] );
                w[i] = 1.0;
            }
        }
        
        refCentroid = calcCentroid(ref);
        mobCentroid = calcCentroid(mob);
        for(i = 0; i < len; i++)
        {
            ref[i].sub(refCentroid);
            mob[i].sub(mobCentroid);
        }
    }
//}}}

//{{{ get, set for Tuple3
//##############################################################################
    /** Retrieve the ith component of t: 1 = x, 2 = y, 3 = z. */
    static private double get(Tuple3 t, int i)
    {
        switch(i)
        {
            case 1: return t.getX();
            case 2: return t.getY();
            case 3: return t.getZ();
            default:
                throw new IllegalArgumentException("Index must be 1, 2, or 3");
        }
    }
    
    /** Assign the ith component of t: 1 = x, 2 = y, 3 = z. */
    static private void set(MutableTuple3 t, int i, double val)
    {
        switch(i)
        {
            case 1:
                t.setX(val);
                break;
            case 2:
                t.setY(val);
                break;
            case 3:
                t.setZ(val);
                break;
            default:
                throw new IllegalArgumentException("Index must be 1, 2, or 3");
        }
    }
//}}}

//{{{ calcCentroid, calcRMSD
//##############################################################################
    /**
    * Finds the centroid (average) of a set of points.
    * Only the first len points are considered.
    */
    Triple calcCentroid(Triple[] m)
    {
        Triple c = new Triple();
        for(int i = 0; i < len; i++)
            c.add(m[i]);
        c.mult(1.0/len);
        return c;
    }
    
    /**
    * Calculates the weighted root-mean-square deviation between the point sets.
    * The supplied transformation is applied to the the mobile set before calculating.
    */
    public double calcRMSD(Transform R)
    {
        double rmsd = 0.0;
        Triple t = new Triple();
        for(int i = 0; i < len; i++)
        {
            // We use transformVector() because mob and ref have both
            // had their centroids shifted to the origen already.
            R.transformVector(mob[i], t);
            t.sub(ref[i]);
            rmsd += w[i] * t.mag2();
        }
        return Math.sqrt(rmsd / len);
    }
//}}}

//{{{ superpos
//##############################################################################
    /**
    * Creates a transformation that will superimpose the mobile point set
    * onto the reference point set by the iterative method of McLachlan.
    * <p>See A. D. McLachlan, Acta Cryst (1982) A38, 871-873.
    * Nomenclature remains the same except that r is called "mob" here,
    * and b is called "ref".
    */
    public Transform superpos()
    {
        // Declare all variables
        Transform   V   = new Transform();  // the special 3x3 matrix V (aka U)
        double      v;                      // V11+V22+V33
        Transform   R   = new Transform();  // the rotation matrix
        Transform   X   = new Transform();  // a working register for computation
        Triple      gp  = new Triple();     // the previous couple
        Triple      g   = new Triple();     // the couple
        double      g2;                     // == g.mag2()
        Triple      sp  = new Triple();     // the previous path vector
        Triple      s   = new Triple();     // the path vector
        Triple      l   = new Triple();     // the unit rotation axis
        double      theta;                  // the angle of rotation
        double      G;                      // a magic number
        double      H;                      // another magic number
        int         i, j;                   // indices on [1,3]: x/y/z or matrix cell
        int         n;                      // index on [0,len): point number
        int         p;                      // index on [0,inf): cycle number
        
        // Equations for updating the residual, E, appear to be
        // either wrong or numerically unstable.
        // Thus, I ignore it and monitor |g| and theta instead.
        
        // Calculate starting V: Vij = sum[wn rin bjn]
        for(i = 1; i <= 3; i++)   for(j = 1; j <= 3; j++)
        {
            double Vij = 0.0;
            for(n = 0; n < len; n++)
                Vij += w[n]*get(mob[n], i)*get(ref[n], j);
            V.set(i, j, Vij);
        }
        
        // Begin iterative approximation of correct rotation matrix
        for(p = 0; p < 18; p++) // limit to 18 cycles max (6 full iterations)
        {
            // Find the couple g
            g.setX( V.get(2,3) - V.get(3,2) );
            g.setY( V.get(3,1) - V.get(1,3) );
            g.setZ( V.get(1,2) - V.get(2,1) );
            g2 = g.mag2();
            
            // Calculate the path vector s and rotation axis l
            if(p % 3 == 0)  s.like(g);
            else            s.like(sp).mult(g2/gp.mag2()).add(g);
            gp.like(g);
            sp.like(s);
            l.like(s).unit();
            
            // Calculate v, G, H, and theta
            v = V.get(1,1) + V.get(2,2) + V.get(3,3);
            G = g.dot(l);
            H = 0.0;
            for(i = 1; i <= 3; i++)   for(j = 1; j <= 3; j++)
                H += get(l,i)*(  v*(i==j?1:0) - 0.5*(V.get(i,j)+V.get(j,i))  )*get(l,j);
            theta = Math.toDegrees(Math.atan2(G, H));
            
            // Update R and V if l is non-zero (it should be 1.0).
            // (If l were zero, we would get NaN transforms.)
            if(l.mag2() > 1e-12)
            {
                X.likeRotation(l, theta);
                R.premult(X);
                V.premult(X);
            }
            
            /* Print statistics * /
            double rmsd = calcRMSD(R, w);
            SoftLog.err.println("=== CYCLE "+(p+1)+" ===");
            SoftLog.err.println("RMSD    = "+rmsd);
            SoftLog.err.println("g^2     = "+g.mag2());
            SoftLog.err.println("theta   = "+theta);
            SoftLog.err.println("=== CYCLE "+(p+1)+" ===");
            SoftLog.err.println();
            /* Print statistics */
            
            // Conditions for early termination:
            // (1) It's the third, sixth, ninth, etc. cycle.
            // (2) The couple or the rotation angle is small.
            if(p%3 == 2 && (g2 < 1e-12 || theta < 1e-6))
                break;            
        }
        
        // Set up the proper translations to make this work for points
        l.like(mobCentroid).neg();  // recycle l since we're done with it
        X.likeTranslation(l);
        R.postmult(X);              // move centroid to origen first
        X.likeTranslation(refCentroid);
        R.premult(X);               // move mob. centroid to ref. last

        return R;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

