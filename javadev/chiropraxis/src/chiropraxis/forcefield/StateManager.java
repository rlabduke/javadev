// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.forcefield;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
//}}}
/**
* <code>StateManager</code> keeps track of the points and energy terms
* used for a molecular mechanics force field minimization.
*
* Before beginning the minimization call <code>accept()</code>.
* GradientMinimizer does this for you, so you don't have to worry about it.
*
* This class uses IdentityHashMap, which first became available with Java 1.4.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jul 12 13:17:42 EDT 2004
*/
public class StateManager //extends ... implements ...
{
//{{{ Constants
    static final EnergyTerm[] emptyTerms = {};
//}}}

//{{{ Variable definitions
//##############################################################################
    // The 3-D points that define this system
    MutableTuple3[] points;
    
    // Only the first N coordinates actually move during minimization
    int nMobile;

    // The current state, and the current "test" state for line minimization
    double[] state, testState;
    
    /** The gradient of the function as calculated by the last call to accept(). Read only. */
    public double[] gradient;
    
    // Gradients of individual components
    double[] gBond, gAngle, gNonbond, gExtra;
    
    // The various energy terms being used right now
    EnergyTerm[] bondTerms = {};
    EnergyTerm[] angleTerms = {};
    EnergyTerm[] nbTerms = {};
    EnergyTerm[] extraTerms = {};
    
    // Weights on all the various energy terms
    double wBond = 1, wAngle = 1, wNonbond = 1, wExtra = 1;
    
    // A table for looking up the index of a particular point
    Map indexTable;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public StateManager(MutableTuple3[] points, int nMobile)
    {
        super();
        this.points = (MutableTuple3[]) points.clone();
        this.nMobile = nMobile * 3;
        
        this.state      = new double[ points.length * 3 ];
        this.testState  = new double[ points.length * 3 ];
        this.gradient   = new double[ points.length * 3 ];
        this.gBond      = new double[ points.length * 3 ];
        this.gAngle     = new double[ points.length * 3 ];
        this.gNonbond   = new double[ points.length * 3 ];
        this.gExtra     = new double[ points.length * 3 ];
        
        this.indexTable = new IdentityHashMap();
        for(int i = 0; i < points.length; i++)
            indexTable.put(points[i], new Integer(i));
        
        setState();
    }
//}}}

//{{{ get/setState
//##############################################################################
    /**
    * Initializes the current state and test state to be the contents of
    * the current coordinates of the MutableTuple3's this object was created with.
    */
    public void setState()
    {
        for(int i = 0, ii = 0; i < points.length; i++)
        {
            state[ii] = testState[ii] = points[i].getX(); ii++;
            state[ii] = testState[ii] = points[i].getY(); ii++;
            state[ii] = testState[ii] = points[i].getZ(); ii++;
        }
    }
    
    /**
    * Writes the current state into the coordinates of the
    * MutableTuple3's this object was created with.
    */
    public void getState()
    {
        for(int i = 0, ii = 0; i < points.length; i++)
        {
            points[i].setX( state[ii++] );
            points[i].setY( state[ii++] );
            points[i].setZ( state[ii++] );
        }
    }
//}}}

//{{{ get/setPoint, getIndex
//##############################################################################
    /** Changes the coordinates of the ith point. */
    public void setPoint(int i, double x, double y, double z)
    {
        int ii = i*3;
        state[ii  ] = x;
        state[ii+1] = y;
        state[ii+2] = z;
    }
    
    /** Updates the coordinates of the ith point from its corresponding MutableTuple3 object. */
    public void setPoint(int i)
    {
        int ii = i*3;
        state[ii  ] = points[i].getX();
        state[ii+1] = points[i].getY();
        state[ii+2] = points[i].getZ();
    }
    
    /** Fills in the coordinates of the ith point */
    public void getPoint(int i, MutableTuple3 t)
    {
        int ii = i*3;
        t.setXYZ( state[ii], state[ii+1], state[ii+2] );
    }
    
    /**
    * Retrieves the index associated with given point.
    * If the point is not part of this state, returns -1.
    * Lookup is based on strict equality, not equals().
    */
    public int getIndex(MutableTuple3 t)
    {
        Integer i = (Integer) indexTable.get(t);
        if(i == null) return -1;
        else return i.intValue();
    }
//}}}

//{{{ get/setXXXTerms
//##############################################################################
    /** Sets the bond energy terms that will be evaluated for this state. */
    public void setBondTerms(Collection terms, double weight)
    {
        this.bondTerms = (EnergyTerm[]) terms.toArray(new EnergyTerm[terms.size()]);
        this.wBond = weight;
    }
    public void setBondTerms(Collection terms)
    { setBondTerms(terms, 1); }
    /** Returns a copy of the array of bond terms that are being evaluated for this state. */
    public EnergyTerm[] getBondTerms()
    { return (EnergyTerm[]) bondTerms.clone(); }

    /** Sets the angle energy terms that will be evaluated for this state. */
    public void setAngleTerms(Collection terms, double weight)
    {
        this.angleTerms = (EnergyTerm[]) terms.toArray(new EnergyTerm[terms.size()]);
        this.wAngle = weight;
    }
    public void setAngleTerms(Collection terms)
    { setAngleTerms(terms, 1); }
    /** Returns a copy of the array of angle terms that are being evaluated for this state. */
    public EnergyTerm[] getAngleTerms()
    { return (EnergyTerm[]) angleTerms.clone(); }

    /** Sets the nonbonded energy terms that will be evaluated for this state. */
    public void setNbTerms(Collection terms, double weight)
    {
        this.nbTerms = (EnergyTerm[]) terms.toArray(new EnergyTerm[terms.size()]);
        this.wNonbond = weight;
    }
    public void setNbTerms(Collection terms)
    { setNbTerms(terms, 1); }
    /** Returns a copy of the array of nonbonded terms that are being evaluated for this state. */
    public EnergyTerm[] getNbTerms()
    { return (EnergyTerm[]) nbTerms.clone(); }

    /** Sets the extra energy terms that will be evaluated for this state. */
    public void setExtraTerms(Collection terms, double weight)
    {
        this.extraTerms = (EnergyTerm[]) terms.toArray(new EnergyTerm[terms.size()]);
        this.wExtra = weight;
    }
    public void setExtraTerms(Collection terms)
    { setExtraTerms(terms, 1); }
    /** Returns a copy of the array of extra terms that are being evaluated for this state. */
    public EnergyTerm[] getExtraTerms()
    { return (EnergyTerm[]) extraTerms.clone(); }
//}}}

//{{{ test, accept
//##############################################################################
    /**
    * Evaluates this function at its current state PLUS lambda*path and returns
    * the energy.
    */
    public double test(double lambda, double[] path)
    {
        for(int i = 0; i < nMobile; i++)
        {
            testState[i] = state[i] + lambda*path[i];
        }
        // remaining terms of testState and state are the same
        return eval(testState);
    }
    
    /**
    * Accepts the last state submitted to test() and re-evaluates the gradient.
    */
    public double accept()
    {
        double[] swap = state;
        state = testState;
        testState = swap;
        
        return eval(state, gradient);
    }
    
    /**
    * Combo of test() and accept()
    */
    public double accept(double lambda, double[] path)
    {
        for(int i = 0; i < nMobile; i++)
        {
            testState[i] = state[i] + lambda*path[i];
        }
        
        double[] swap = state;
        state = testState;
        testState = swap;
        
        return eval(state, gradient);
    }
//}}}

//{{{ eval(state, gradient)
//##############################################################################
    public double eval(double[] s, double[] g)
    {
        //Arrays.fill(g, 0);
        double bondE = 0, angleE = 0, nonbondE = 0, extraE = 0;
        
        if(bondTerms != null && wBond != 0)
        {
            Arrays.fill(gBond, 0);
            for(int i = 0, end_i = bondTerms.length; i < end_i; i++)
                bondE += bondTerms[i].eval(s, gBond);
        }
        if(angleTerms != null && wAngle != 0)
        {
            Arrays.fill(gAngle, 0);
            for(int i = 0, end_i = angleTerms.length; i < end_i; i++)
                angleE += angleTerms[i].eval(s, gAngle);
        }
        if(nbTerms != null && wNonbond != 0)
        {
            Arrays.fill(gNonbond, 0);
            for(int i = 0, end_i = nbTerms.length; i < end_i; i++)
                nonbondE += nbTerms[i].eval(s, gNonbond);
        }
        if(extraTerms != null && wExtra != 0)
        {
            Arrays.fill(gExtra, 0);
            for(int i = 0, end_i = extraTerms.length; i < end_i; i++)
                extraE += extraTerms[i].eval(s, gExtra);
        }
        
        for(int i = 0, end_i = g.length; i < end_i; i++)
            g[i] = wBond*gBond[i] + wAngle*gAngle[i] + wNonbond*gNonbond[i] + wExtra*gExtra[i];
        
        return wBond*bondE + wAngle*angleE + wNonbond*nonbondE + wExtra*extraE;
    }
//}}}

//{{{ eval(state)
//##############################################################################
    public double eval(double[] s)
    {
        double bondE = 0, angleE = 0, nonbondE = 0, extraE = 0;
        
        if(bondTerms != null && wBond != 0)
        {
            for(int i = 0, end_i = bondTerms.length; i < end_i; i++)
                bondE += bondTerms[i].eval(s);
        }
        if(angleTerms != null && wAngle != 0)
        {
            for(int i = 0, end_i = angleTerms.length; i < end_i; i++)
                angleE += angleTerms[i].eval(s);
        }
        if(nbTerms != null && wNonbond != 0)
        {
            for(int i = 0, end_i = nbTerms.length; i < end_i; i++)
                nonbondE += nbTerms[i].eval(s);
        }
        if(extraTerms != null && wExtra != 0)
        {
            for(int i = 0, end_i = extraTerms.length; i < end_i; i++)
                extraE += extraTerms[i].eval(s);
        }
        
        return wBond*bondE + wAngle*angleE + wNonbond*nonbondE + wExtra*extraE;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

/*
//{{{ main (for testing)
//##############################################################################
    public static void main(String[] args)
    {
        Triple[] points = new Triple[42];
        for(int i = 0; i < points.length-2; i++)
            points[i] = new Triple(10*Math.random(), 10*Math.random(), 10*Math.random());
        points[points.length-2] = new Triple(0,0,0);
        points[points.length-1] = new Triple(10,10,10);
        
        ArrayList terms = new ArrayList();
        for(int i = 0; i < points.length-3; i++)
            terms.add(new BondTerm(i, i+1, 1, 1));
        terms.add(new BondTerm(points.length-3, points.length-1, 0, 1));
        terms.add(new BondTerm(0,               points.length-2, 0, 1));
        // For faking angle restraints with distances:
        //for(int i = 0; i < points.length-4; i++)
        //    terms.add(new BondTerm(i, i+2, 1.5, 1));
        for(int i = 0; i < points.length-4; i++)
            terms.add(new AngleTerm(i, i+1, i+2, 120, 1));
        
        StateManager stateman = new StateManager(points, points.length-2);
        stateman.setEnergyTerms((EnergyTerm[])terms.toArray(new EnergyTerm[terms.size()]));
        GradientMinimizer min = new GradientMinimizer(stateman);
        
        PrintStream out = System.out;
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@kinemage 1");
        int j, netEvals = 0;
        for(j = 0; j < 100; j++)
        {
            out.println("@group {step "+j+"} animate dominant");
            out.println("@vectorlist {step "+j+"}");
            for(int i = 0; i < points.length-2; i++)
                out.println("{} "+points[i].format(df));
            
            if(! min.step()) break;
            System.err.println(j+": "+min.getFuncEvals()+" evals; dE = "+df.format(100*min.getFracDeltaEnergy())
                +"% ("+df.format(min.getDeltaEnergy())+"); |G| = "+df.format(min.getGradMag()));
            netEvals += min.getFuncEvals();
            stateman.getState();
        }
        System.err.println("Average "+(netEvals / (j+1))+" evals per step");
        
        // long time = System.currentTimeMillis();
        // for(int j = 0; j < 100000; j++)
        // {
            // if(! min.step()) break;
        // }
        // time = System.currentTimeMillis() - time;
        // System.err.println(time+" millis to do 100,000 CG minimization steps.");
    }
//}}}
*/
}//class

