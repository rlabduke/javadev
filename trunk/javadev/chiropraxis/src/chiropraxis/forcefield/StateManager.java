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
* Before beginning the minimization call: accept();
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jul 12 13:17:42 EDT 2004
*/
public class StateManager //extends ... implements ...
{
//{{{ Constants
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
    
    // The various energy terms being used right now
    EnergyTerm[] energyFunc = {};
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
        
        readState();
    }
//}}}

//{{{ readState, writeState
//##############################################################################
    /**
    * Initializes the current state and test state to be the contents of
    * the current coordinates of the MutableTuple3's this object was created with.
    */
    public void readState()
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
    public void writeState()
    {
        for(int i = 0, ii = 0; i < points.length; i++)
        {
            points[i].setX( state[ii++] );
            points[i].setY( state[ii++] );
            points[i].setZ( state[ii++] );
        }
    }
//}}}

//{{{ setEnergyTerms
//##############################################################################
    /** Sets the energy terms that will be evaluated for this state. */
    public void setEnergyTerms(EnergyTerm[] terms)
    {
        this.energyFunc = (EnergyTerm[]) terms.clone();
    }
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

//{{{ eval
//##############################################################################
    public double eval(double[] s, double[] g)
    {
        Arrays.fill(g, 0);
        double energy = 0;
        for(int i = 0, end_i = energyFunc.length; i < end_i; i++)
        {
            energy += energyFunc[i].eval(s, g);
        }
        
        return energy;
    }

    public double eval(double[] s)
    {
        double energy = 0;
        for(int i = 0, end_i = energyFunc.length; i < end_i; i++)
        {
            energy += energyFunc[i].eval(s);
        }
        
        return energy;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

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
        for(int i = 0; i < points.length-2; i++)
            terms.add(new BondTerm(i, i+1, 1, 1));
        terms.add(new BondTerm(0, points.length-1, 1, 1));
        for(int i = 0; i < points.length-4; i++)
            terms.add(new BondTerm(i, i+2, 1.5, 1));
        //for(int i = 0; i < points.length-4; i++)
        //    terms.add(new AngleTerm(i, i+1, i+2, 120, 1));
        
        StateManager stateman = new StateManager(points, points.length-2);
        stateman.setEnergyTerms((EnergyTerm[])terms.toArray(new EnergyTerm[terms.size()]));
        GradientMinimizer min = new GradientMinimizer(stateman);
        
        PrintStream out = System.out;
        DecimalFormat df = new DecimalFormat("0.0####");
        out.println("@kinemage 1");
        int j, netEvals = 0;
        for(j = 0; j < 100; j++)
        {
            out.println("@group {step "+j+"} animate dominant");
            out.println("@vectorlist {step "+j+"}");
            for(int i = 0; i < points.length-2; i++)
                out.println("{} "+points[i].format(df));
            
            if(! min.step()) break;
            System.err.println("step "+j+": "+min.getFuncEvals()+" evals; dEnergy = "+df.format(min.getDeltaEnergy())+"; |Gradient| = "+df.format(min.getGradMag()));
            netEvals += min.getFuncEvals();
            stateman.writeState();
        }
        System.err.println("Average "+(netEvals / (j+1))+" evals per step");
    }
//}}}
}//class

