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
//import driftwood.*;
//}}}
/**
* <code>GradientMinimizer</code> performs steepest-descent minimization or
* conjugate directions (aka conjugate gradient) minimization
* on a StateManager.
*
* <b>WARNING! This code is by no means exemplary.
* It does usually work, but it's probably inefficient
* and definitely doesn't use the "best practices" algorithms.
* See one of the Numerical Recipes books or perhaps the
* GNU Scientific Library for production-quality code.</b>
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 28 07:59:57 EST 2003
*/
public class GradientMinimizer //extends ... implements ...
{
//{{{ Constants
    static DecimalFormat df = new DecimalFormat("0.###E0");
    static final double GOLD = (3.0-Math.sqrt(5.0))/2.0; // 0.381...
    static final double GOLDEN = 2.0 - GOLD; // 1.681...
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The energy function to be minimized. */
    StateManager        system;
    /** The energy at the end of the last step. */
    double              prevEnergy;
    /** The gradient or search vector from the previous step. */
    double[]            prevGrad;
    double              prevGMag;
    /** The energy at the end of the current step. */
    double              currEnergy;
    /** The gradient or search vector from the current step. */
    double[]            currGrad;
    double              currGMag;
    
    /** Number of steps executed. */
    int                 nSteps      = 0;
    /** Whether we've hit the minimum */
    boolean             hitBottom   = false;
    
    /** The current step length. */
    double              stepLen     = 1e-2;
    /** Number of times the function has been evaluated for the last step. */
    int                 eval        = 0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public GradientMinimizer(StateManager stateman)
    {
        super();
        system = stateman;
        
        // Initialize the gradient and energy
        currEnergy = prevEnergy = system.accept();
        prevGrad = (double[]) system.gradient.clone();
        currGrad = (double[]) system.gradient.clone();
        currGMag = prevGMag = getGradientMagnitude(system.gradient);
    }
//}}}

//{{{ step
//##############################################################################
    /**
    * Executes one step of minimization on the system.
    * Once the minimization is so close to complete as to be numerically
    * unstable, this function will return false and further calls to it will
    * have no effect.
    */
    public boolean step()
    {
        if(hitBottom) return false;

        // Save results from previous run for this step()
        prevEnergy = currEnergy;
        prevGMag = currGMag;
        double[] swap = prevGrad;
        prevGrad = currGrad;
        currGrad = swap;
        
        // Calculate the search vector for conjugate directions.
        makeSearchVector(); // sets currGrad and currGMag

        // Take a step against the search vector to reduce the energy.
        if(currGMag > 0)
            doLineMinimization(currGMag, currGrad); 
        else // currGMag is 0 or NaN
            hitBottom   = true;
        
        if(hitBottom) currGMag = 0;
        
        nSteps++;
        return true;
    }
//}}}

//{{{ getGradientMagnitude, makeSearchVector
//##############################################################################
    /** Returns the magnitude of grad */
    double getGradientMagnitude(double[] grad)
    {
        double gMag = 0.0;
        for(int i = 0, end_i = grad.length; i < end_i; i++)
            gMag += grad[i] * grad[i];
        gMag = Math.sqrt(gMag);
        return gMag;
    }
    
    /**
    * Transforms grad into a conjugate direction if appropriate,
    * and returns its new magnitude.
    */
    void makeSearchVector()
    {
        double[] grad = system.gradient;
        currGMag = getGradientMagnitude(grad);
        
        if(nSteps % grad.length != 0)
        {
            double gamma = (currGMag/prevGMag);
            gamma = gamma*gamma;
            for(int i = 0; i < grad.length; i++)
                currGrad[i] = grad[i] + gamma*prevGrad[i];
            currGMag = getGradientMagnitude(currGrad);
        }
        else
        {
            for(int i = 0; i < grad.length; i++)
                currGrad[i] = grad[i];
        }
    }
//}}}

//{{{ doLineMinimization
//##############################################################################
    void doLineMinimization(double gMag, double[] path)
    {
        final double tol = 1e-4; // no less than 3e-8 for doubles, 1e-4 for floats
        eval = 0;
        
        // Bracket the function on (a, b, c)
        double a = 0, b, c, x, fa, fb, fc, fx;
        fa = system.test(a, path); eval++;
        
        // Find a smaller energy at b:
        while(true)
        {
            b = a + stepLen / gMag;
            if(Double.isNaN(b))
            {
                hitBottom = true;
                return;
            }
            fb = system.test(-b, path); eval++;
            if(fb < fa)             break;          // good -- we found a smaller point
            else if(stepLen > 0)    stepLen *= 0.5; // failure - try a smaller step
            else                                    // can't take a small enough step
            {
                hitBottom = true;
                return;
            }
        }
        
        // Initial guess for c:
        c = b + (b-a)*GOLDEN;
        fc = system.test(-c, path); eval++;
        // Search for bracket by golden ratio
        while(fc < fb)
        {
            x = c + (c-b)*GOLDEN;
            fx = system.test(-x, path); eval++;
            a = b; b = c; c = x;
            fa = fb; fb = fc; fc = fx;
        }
        stepLen = b * gMag; // how far we stepped this time from a==0 to reach bracket center
        //System.err.print("Bracketed on "+a+" "+b+" "+c+" in "+eval+" evals.");

        // Brent's method. Adapted from Numerical Recipes; I don't know how it works.
        double d = 0, e = 0, eTemp, u, v, w, xm, fu, fv, fw, p, q, r, tol1, tol2;
        x = w = v       = b;
        fx = fw = fv    = fb;
        for(int i = 0; i < 100; i++)
        {
            xm = (a + b) / 2.0;
            tol1 = tol*x + 1e-10;
            tol2 = 2.0 * tol1;
            if(Math.abs(x-xm) <= (tol2 - (b-a)/2.0))
            {
                currEnergy = system.accept(-x, path); eval++;
                //System.err.println("Solved to "+tol+" as "+b+" in "+eval+" evals.");
                return;
            }
            if(Math.abs(e) > tol1)
            {
                r = (x-w) * (fx-fv);
                q = (x-v) * (fx-fw);
                p = (x-v)*q - (x-w)*r;
                q = 2.0 * (q-r);
                if(q > 0.0) p = -p;
                else        q = -q;
                eTemp = e;
                if(Math.abs(p) >= Math.abs(0.5*q*eTemp) || p <= q*(a-x) || p >= q*(b-x))
                {
                    e = (x >= xm ? a-x : b-x);
                    d = GOLD * e;
                }
                else
                {
                    e = d;
                    d = p/q;
                    u = x+d;
                    if(u-a < tol2 || b-u < tol2)
                        d = (xm-x >= 0 ? Math.abs(tol1) : -Math.abs(tol1));
                }
            }
            else
            {
                e = (x >= xm ? a-x : b-x);
                d = GOLD * e;
            }
            u = (Math.abs(d) >= tol1 ? x+d : x+(d >= 0 ? Math.abs(tol1) : -Math.abs(tol1)));
            fu = system.test(-u, path); eval++;
            if(fu <= fx)
            {
                if(u >= x)  a = x;
                else        b = x;
                v = w; w = x; x = u;
                fv = fw; fw = fx; fx = fu;
            }
            else
            {
                if(u < x)   a = u;
                else        b = u;
                if(fu <= fw || w == x)
                {
                    v = w; w = u;
                    fv = fw; fw = fu;
                }
                else if(fu <= fv || v == x || v == w)
                {
                    v = u;
                    fv = fu;
                }
            }
        }
        
        System.err.println("GradientMinimizer.doLineMinimization(): Failed to minimize after 100 loops through Brent!");
        currEnergy = system.accept(-x, path); eval++;
        return;
    }
//}}}

//{{{ get(Delta)Energy, get(Delta)GradMag, getFuncEvals
//##############################################################################
    /** Returns the energy of the state after the last step. */
    public double getEnergy()
    { return currEnergy; }
    
    /** Returns the change in energy after the last step. */
    public double getDeltaEnergy()
    { return currEnergy - prevEnergy; }
    
    /** Returns the gradient magnitude after the last step. */
    public double getGradMag()
    { return currGMag; }
    
    /** Returns the change in gradient magnitude after the last step. */
    public double getDeltaGradMag()
    { return currGMag - prevGMag; }
    
    /** Returns the number of times the function was evaluated in the last step. */
    public int getFuncEvals()
    { return eval; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

