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
* <p>This code tries to be a production-quality conjugate gradient minimizer,
* but I have limited experience with scientific-numeric computing.
* If you want something really bulletproof, the GNU Scientific Library may be better.
*
* <p>This code is based on ideas from "Molecular Modelling" by Andrew R. Leach
* and "Numerical Recipes in C", Ch 10 (http://www.library.cornell.edu/nr/bookcpdf.html).
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
    double[]            prevPath;
    double              prevPMag;
    /** The energy at the end of the current step. */
    double              currEnergy;
    /** The gradient or search vector from the current step. */
    double[]            currGrad;
    double              currGMag;
    double[]            currPath;
    double              currPMag;
    
    /** Number of steps executed. */
    int                 nSteps      = 0;
    /** Number of times the function has been evaluated for the last step. */
    int                 eval        = 0;
    /** Whether we've hit the minimum */
    boolean             hitBottom   = false;
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
        prevPath = (double[]) system.gradient.clone();
        currGrad = (double[]) system.gradient.clone();
        currPath = (double[]) system.gradient.clone();
        currGMag = currPMag = prevGMag = prevPMag = getMagnitude(system.gradient);
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
        double[] swap;
        prevEnergy = currEnergy;
        prevGMag = currGMag; prevPMag = currPMag;
        swap = prevGrad; prevGrad = currGrad; currGrad = swap;
        swap = prevPath; prevPath = currPath; currPath = swap;
        
        // Calculate the search vector for conjugate directions.
        makeSearchVector(); // sets currGrad, currGMag, currPath, currPMag

        // Take a step against the search vector to reduce the energy.
        if(currGMag > 0 && currPMag > 0)
            doLineMinimization(); 
        else // currG/PMag is 0 or NaN
        {
            hitBottom = true;
            //System.err.println("|G| = "+df.format(currGMag)+"    |P| = "+df.format(currPMag));
        }
        
        if(hitBottom) currGMag = 0;
        
        nSteps++;
        return true;
    }
//}}}

//{{{ makeSearchVector, getMagnitude
//##############################################################################
    /**
    * Transforms grad into a conjugate direction if appropriate,
    * and returns its new magnitude.
    */
    void makeSearchVector()
    {
        int len = system.gradient.length;
        System.arraycopy(system.gradient, 0, currGrad, 0, len);
        currGMag = getMagnitude(currGrad);
        
        if(nSteps % len != 0)
        {
            /* The Fletcher-Reeves version * /
            double gamma = (currGMag/prevGMag);
            gamma = gamma*gamma;
            /* The Fletcher-Reeves version */
            
            /* The Polak-Ribiere version */
            double gamma = 0;
            for(int i = 0; i < len; i++)
                gamma += (currGrad[i] - prevGrad[i]) * currGrad[i];
            gamma = gamma / (prevGMag * prevGMag);
            /* The Polak-Ribiere version */
            
            for(int i = 0; i < len; i++)
                currPath[i] = currGrad[i] + gamma*prevPath[i];
            currPMag = getMagnitude(currPath);
        }
        else // steepest descent
        {
            System.arraycopy(currGrad, 0, currPath, 0, len);
            currPMag = currGMag;
        }
    }
    
    /** Returns the magnitude of grad */
    double getMagnitude(double[] grad)
    {
        double gMag = 0.0;
        for(int i = 0, end_i = grad.length; i < end_i; i++)
            gMag += grad[i] * grad[i];
        gMag = Math.sqrt(gMag);
        return gMag;
    }
//}}}

//{{{ doLineMinimization
//##############################################################################
    void doLineMinimization()
    {
        final double initStep = 1e-2; // seems to be a consistently good guess
        final double tol = 1e-4; // no less than 3e-8 for doubles, 1e-4 for floats
        eval = 0;
        
        // Bracket the function on (a, b, c)
        double[] path = currPath;
        double a = 0, b, c, x, fa, fb, fc, fx;
        fa = this.prevEnergy; //system.test(a, path); eval++;
        b = initStep;
        
        // Find a smaller energy at b:
        while(true)
        {
            fb = system.test(-b, path); eval++;
            if(fb < fa) break;      // good -- we found a smaller point
            else if(b < 1e-20)      // if min is at 0, Brent won't get closer than 1e-10 anyway
            {
                // Testing |G| / (Ãn) when n = number of variables is due to T. Schlick
                if(currGMag / Math.sqrt(path.length) < 1e-6*(1+Math.abs(fa)) || currGMag == prevGMag)
                {
                    hitBottom = true;
                    //System.err.println("b is too small after "+eval+" evals;    |G| = "+df.format(currGMag));
                    return;
                }
                else // just a snag, take a small "random" step along the path
                {
                    currEnergy = system.accept(); eval++; // larger steps may cause explosions!
                    //System.err.println("Got stuck, took a small step;    |G| = "+df.format(currGMag));
                    return;
                }
            }
            else b /= 8.0;          // failure - try a smaller step
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
        //if(eval > 20) System.err.print("Bracketed on "+a+" "+b+" "+c+" in "+eval+" evals.");

        // Brent's method.
        double d = 0, e = 0, u, v, w, xm, fu, fv, fw, p, q, r, tol1, tol2;
        x = w = v       = b;
        fx = fw = fv    = fb;
        b = c; // change in nomenclature: min now bracketed in (a,b)
        for(int i = 0; i < 100; i++)
        {
            xm = (a + b) / 2.0;
            // a and b started off > 0, so x will always be > 0.
            // This saves us some calls to Math.abs(tol1) below.
            tol1 = tol*x + 1e-10;
            tol2 = 2.0 * tol1;
            // If bracketing interval is smaller than twice the fractional tolerance, end.
            if(Math.abs(x-xm) <= (tol2 - (b-a)/2.0))
            {
                currEnergy = system.accept(-x, path); eval++;
                //if(eval > 40) System.err.println("Solved to "+tol+" as "+b+" in "+eval+" evals.");
                return;
            }
            // Try a parabolic fit if ... ?
            if(Math.abs(e) > tol1)
            {
                r = (x-w) * (fx-fv);
                q = (x-v) * (fx-fw);
                p = (x-v)*q - (x-w)*r;
                q = 2.0 * (q-r);
                if(q > 0.0) p = -p;
                else        q = -q;
                // Parabolic fit is not OK; do golden sections instead.
                if(Math.abs(p) >= Math.abs(0.5*q*e) || p <= q*(a-x) || p >= q*(b-x))
                {
                    e = (x >= xm ? a-x : b-x);
                    d = GOLD * e;
                }
                // Parabolic fit *is* OK, use that instead of golden sections.
                else
                {
                    e = d;
                    d = p/q;
                    u = x+d;
                    if(u-a < tol2 || b-u < tol2)
                        d = (xm-x >= 0 ? tol1 : -tol1);//(xm-x >= 0 ? Math.abs(tol1) : -Math.abs(tol1));
                }
            }
            // Skip parabolic fitting; search by golden sections.
            else
            {
                e = (x >= xm ? a-x : b-x);
                d = GOLD * e;
            }
            //u = (Math.abs(d) >= tol1 ? x+d : x+(d >= 0 ? Math.abs(tol1) : -Math.abs(tol1)));
            // Make sure we don't take any steps smaller than our tolerance.
            u = (Math.abs(d) >= tol1 ? x+d : x+(d >= 0 ? tol1 : -tol1));
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
    
    /** Returns the fractional change in energy after the last step. */
    public double getFracDeltaEnergy()
    { return (currEnergy - prevEnergy) / prevEnergy; }
    
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
}//class

