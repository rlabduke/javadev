// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.minimize;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>SimpleHarmonicPotential</code> is a PotentialFunction
* that describes Tuple3s connected by springs.
* This potential has been optimized by assuming a dense network
* of springs, i.e., springs between every pair of points.
* For sparse networks, this results in lots of unneccessary
* point-to-point distance calculations being done and cached.
*
* <p>Changes to the internal state do not affect the original data;
* you must call getState() or exportState() to read the results
* after minimization has been applied.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 28 07:59:57 EST 2003
*/
public class SimpleHarmonicPotential implements PotentialFunction
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The number of points in the system. There are 3N degrees of freedom. */
    final int   N1, N3;
    /** The current state of the system (coordinates). Length = 3N. */
    double[]    state;
    /** The ideal spring length between Tuples i and j, indexed at N*i + j. Length = N*N. */
    double[]    springs;
    /**
    * The relative spring weight between Tuples i and j, indexed at N*i + j.
    * Zero means no spring. Length = N*N.
    */
    double[]    weights;
    /** The point-point distances, precalculated to save time. Indexed at N*i + j, length = N*N. */
    double[]    distances;
    /** The value of this function in the current state, cached for efficiency. */
    double      stateValue = 0;
    boolean     valueDirty = true;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SimpleHarmonicPotential(Tuple3[] system)
    {
        super();
        N1 = system.length;
        N3 = 3 * N1;
        
        state = new double[N3];
        int i1 = 0, i3 = 0;
        while(i1 < N1)
        {
            state[i3+0] = system[i1].getX();
            state[i3+1] = system[i1].getY();
            state[i3+2] = system[i1].getZ();
            i1 += 1;
            i3 += 3;
        }
        
        int i, nn = N1*N1;
        springs = new double[nn];
        weights = new double[nn];
        for(i = 0; i < nn; i++)
        {
            springs[i] = 1.0;
            weights[i] = 0.0; // no spring present by default
        }
        
        distances = new double[nn];
        updateDistances();
    }
//}}}

//{{{ get/set/exportState, addSpring
//##############################################################################
    public double[] getState(double[] outState)
    {
        double[] out;
        if(outState != null && outState.length == N3)
            out = outState;
        else
            out = new double[N3];
        System.arraycopy(this.state, 0, out, 0, N3);
        return out;
    }
    
    public void exportState(MutableTuple3[] system)
    {
        int i1 = 0, i3 = 0;
        while(i1 < N1)
        {
            system[i1].setX( state[i3+0] );
            system[i1].setY( state[i3+1] );
            system[i1].setZ( state[i3+2] );
            i1 += 1;
            i3 += 3;
        }
    }

    public void setState(double[] newState)
    {
        System.arraycopy(newState, 0, this.state, 0, N3);
        updateDistances();
        valueDirty = true;
    }
    
    /**
    * Adds a spring-like restraint between two Tuple3s.
    * The order of i and j doesn't matter, but wt should be greater than zero.
    * @param i      the index of the first Tuple3
    * @param j      the index of the second Tuple3
    * @param len    the ideal length of the spring
    * @param wt     the relative weight (strength) of the spring
    */
    public void addSpring(int i, int j, double len, double wt)
    {
        if(i == j)
            throw new IllegalArgumentException("Cannot create a spring between point "+i+" and itself");
        springs[N1*i + j] = len;
        springs[N1*j + i] = len;
        weights[N1*i + j] = wt;
        weights[N1*j + i] = wt;
    }
//}}}

//{{{ get{Spring, Weight, Dist}, updateDistances
//##############################################################################
    double getSpring(int i, int j)
    {
        return springs[N1*i + j];
    }

    double getWeight(int i, int j)
    {
        return weights[N1*i + j];
    }
    
    /** Returns the distance between Tuple3s i and j */
    double getDist(int i, int j)
    {
        /*
        i *= 3;
        j *= 3;
        double dx = state[i+0] - state[j+0];
        double dy = state[i+1] - state[j+1];
        double dz = state[i+2] - state[j+2];
        return Math.sqrt( dx*dx + dy*dy + dz*dz );
        */
        return distances[N1*i + j];
    }
    
    void updateDistances()
    {
        int i1 = 0, i3 = 0;
        while(i1 < N1)
        {
            int j1 = i1+1, j3 = i3+3;
            while(j1 < N1)
            {
                double dx = state[i3+0] - state[j3+0];
                double dy = state[i3+1] - state[j3+1];
                double dz = state[i3+2] - state[j3+2];
                double dist = Math.sqrt( dx*dx + dy*dy + dz*dz );
                distances[N1*i1 + j1] = dist;
                distances[N1*j1 + i1] = dist;
                j1 += 1;
                j3 += 3;
            }
            i1 += 1;
            i3 += 3;
        }
    }
//}}}

//{{{ evaluate
//##############################################################################
    /**
    * Returns the value of the function for the current state.
    * Results are cached so that subsequent calls
    * (without setState() in between) are low-cost.
    */
    public double evaluate()
    {
        if(valueDirty)
        {
            double val = 0.0, k, R, r, d;
            for(int i = 0; i < N1; i++)
            {
                for(int j = 0; j < N1; j++)
                {
                    k = getWeight(i, j);        // spring constant
                    if(k > 0) // eval iff there's a spring here
                    {
                        R = getSpring(i, j);    // ideal distance
                        r = getDist(  i, j);    // actual distance
                        d = r - R;              // delta distance
                        val += k * d*d;
                    }
                }
            }
            stateValue = val;
            valueDirty = false;
        }
        
        return stateValue;
    }
//}}}

//{{{ gradient
//##############################################################################
    /**
    * Returns the gradient of the function for the current state.
    * Results are not cached; calculation is performed in full every time.
    */
    public double[] gradient(double[] outGrad)
    {
        double[] g;
        if(outGrad != null && outGrad.length == N3)
            g = outGrad;
        else
            g = new double[N3];
        
        int i1, j1, i3, j3;
        double k, R, r, mult;
        
        i1 = i3 = 0;
        while(i1 < N1)
        {
            g[i3+0] = 0.0;
            g[i3+1] = 0.0;
            g[i3+2] = 0.0;
            j1 = j3 = 0;
            while(j1 < N1)
            {
                k = getWeight(i1, j1);      // spring constant
                if(k > 0) // eval iff there's a spring here
                {
                    R = getSpring(i1, j1);  // ideal distance
                    r = getDist(  i1, j1);  // actual distance
                    mult = 4.0 * k * (r - R) / r;
                    g[i3+0] += mult * (state[i3+0] - state[j3+0]);
                    g[i3+1] += mult * (state[i3+1] - state[j3+1]);
                    g[i3+2] += mult * (state[i3+2] - state[j3+2]);
                }
                j1 += 1;
                j3 += 3;
            }
            i1 += 1;
            i3 += 3;
        }
        
        return g;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

