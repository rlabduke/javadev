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
//import driftwood.*;
//}}}
/**
* <code>GradientMinimizer</code> performs steepest-descent minimization or
* conjugate directions (a.k.a. conjugate gradient) minimization
* on a PotentialFunction, using arbitrary step lengths.
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
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The energy function to be minimized. */
    PotentialFunction   func;
    /** The state of the system at the start of this iteration. */
    double[]            lastState   = null;
    /** A possible state of the system, which is being evaluated now. */
    double[]            testState   = null;
    /** The gradient (of lastState) for the current step. */
    double[]            grad        = null;
    /** The gradient or search vector from the previous step. */
    double[]            prevGrad    = null;
    double              prevGMag    = Double.NaN;
    /** Number of steps executed. */
    int                 nSteps      = 0;
    /** Whether we've hit the minimum */
    boolean             hitBottom   = false;
    
    /** Whether to use conjugate directions or steepest descent. */
    boolean             useConjDir  = true;
    /** Whether to use a line search or arbitrary steps. */
    boolean             useLineSrch = true;
    
    /** The current step length. */
    double              stepLen     = 1.0;
    /** A multiplier for successful steps (future ones will be longer). */
    double              goodStep    = 1.2;
    /** A multiplier for failed steps (future ones will be shorter). */
    double              badStep     = 0.5;
    /** A multiplier to guess the next step length in a line search */
    double              lineStep    = 1.0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public GradientMinimizer(PotentialFunction energyFunc)
    {
        super();
        func        = energyFunc;
    }
//}}}

//{{{ step
//##############################################################################
    /**
    * Executes one step of minimization on
    * the system using the potential function.
    * @return the magnitude of the gradient at the start of this step.
    *   Once the minimization is so close to complete as to be numerically
    *   unstable, this function will return 0 and further calls to it will
    *   have no effect.
    */
    public double step()
    {
        if(hitBottom) return 0.0;
        
        // Get the starting state
        lastState = func.getState(lastState);   // created iff it didn't exist
        if(testState == null) testState = new double[ lastState.length ];
        
        // Calculate the gradient, or search vector for conjugate directions.
        grad = func.gradient(grad);             // created iff it didn't exist
        double gMag = getGradientMagnitude();   // magnitude of the gradient
        gMag = makeSearchVector(gMag);          // overwrites grad, gMag if appropriate

        // Take a step against the search vector to reduce the energy.
        if(gMag > 0)
        {
            if(useLineSrch) gMag = doLineSearch(gMag); 
            else            gMag = doArbitraryStep(gMag);
        }
        else // gMag is 0 or NaN
        {
            hitBottom   = true;
            gMag        = 0;
        }
        
        // Save results from this run for next step()
        prevGMag = gMag;
        double[] swap = prevGrad;
        prevGrad = grad;
        grad = swap;
        
        nSteps++;
        return gMag;
    }
//}}}

//{{{   getGradientMagnitude, makeSearchVector
//##############################################################################
    /** Returns the magnitude of grad */
    double getGradientMagnitude()
    {
        double gMag = 0.0;
        for(int i = 0; i < grad.length; i++)
            gMag += grad[i] * grad[i];
        gMag = Math.sqrt(gMag);
        return gMag;
    }
    
    /**
    * Transforms grad into a conjugate direction if appropriate,
    * and returns its new magnitude.
    */
    double makeSearchVector(double gMag)
    {
        if(useConjDir && nSteps % grad.length != 0)
        {
            double gamma = (gMag/prevGMag)*(gMag/prevGMag);
            gMag = 0.0;
            for(int i = 0; i < grad.length; i++)
            {
                grad[i] = grad[i] + gamma*prevGrad[i];
                gMag += grad[i] * grad[i];
            }
            gMag = Math.sqrt(gMag);
        }
        return gMag;
    }
//}}}

//{{{   doArbitraryStep
//##############################################################################
    /**
    * Takes an arbitrary step based on the gradient/search vector.
    * In the future, this may incorporate a line search too.
    */
    double doArbitraryStep(double gMag)
    {
        double startE = func.evaluate();
        while(true)
        {
            // Step against the gradient/search vector
            double mult = stepLen / gMag;
            if(Double.isNaN(mult))
            {
                hitBottom   = true;
                return 0;
            }
            for(int i = 0; i < testState.length; i++)
                testState[i] = lastState[i] - mult*grad[i];
            func.setState(testState);
            
            // Evaluate our progress
            double testE = func.evaluate();
            if(testE < startE) // success!
            {
                stepLen *= goodStep;
                break;
            }
            else if(stepLen > 0) // failure - try a smaller step
            {
                stepLen *= badStep;
            }
            else // can't take a small enough step
            {
                hitBottom   = true;
                return 0;
            }
        }
        return gMag;
    }
//}}}

//{{{   doLineSearch
//##############################################################################
    /**
    * Performs a line search against the current gradient / search vector
    * to find the minimum of the function in that direction.
    *
    * Search has two stages, after AR Leach (Molec Modeling and Appl):
    *   1. Bracket the minimum:
    *       Find three points (x0,y0), (x1,y1), (x2,y2) such that
    *       x0 < x1 < x2 and y1 < y0 and y1 < y2 (high-low-high).
    *   2. Refine the minimum:
    *       Fit a parabola to the three points.
    *       Make its minimum the new (x1,y1). Repeat as needed.
    *
    * Here's my plan for #1. The initial state is x0. Take an arbitrary step:
    *
    * - If the new y is greater than y0, make that (x2,y2) and use the
    *   "failed step" regime to find (x1,y1). If it takes multiple tries,
    *   (x2,y2) is always overwritten with the previous try.
    * - If the new y is less than y0, we've found (x1,y1). Use the
    *   "succeeded step" regime to find (x2,y2). If it takes multiple tries
    *   (x1, y1) is always overwritten with the previous try.
    */
    double doLineSearch(double gMag)
    {
        // x's are distances against the gradient
        // y's are energies
        double x0, x1, x2, y0, y1, y2;
        x0 = 0.0;
        y0 = func.evaluate();
        
        // Take a first step
        double mult = stepLen / gMag;
        if(Double.isNaN(mult))
        {
            hitBottom   = true;
            return 0;
        }
        for(int i = 0; i < testState.length; i++)
            testState[i] = lastState[i] - mult*grad[i];
        func.setState(testState);
        
        // Bracket the minimum
        double testE = func.evaluate();
        if(testE < y0) // exploring forward
        {
            x1 = stepLen;
            y1 = testE;
            
            // Continue steping forward until we find a larger value
            while(true)
            {
                stepLen *= goodStep;
                x2 = x1 + stepLen;
                mult = x2 / gMag;
                // won't be NaN b/c stepLen is getting bigger, not smaller
                for(int i = 0; i < testState.length; i++)
                    testState[i] = lastState[i] - mult*grad[i];
                func.setState(testState);
                y2 = func.evaluate();
                if(y2 > y1) // we've bracketed it!
                {
                    break;
                }
                else // point 2 is even lower than point 1
                {
                    y0 = y1;
                    x0 = x1;
                    y1 = y2;
                    x1 = x2;
                }
            }//while(not bracketed)
        }
        else // backtracking
        {
            x2 = stepLen;
            y2 = testE;
            
            // Continue backward until we find a smaller value
            while(true)
            {
                stepLen *= badStep;
                if(stepLen == 0.0) // we must already be at the minimum!
                {
                    hitBottom   = true;
                    func.setState(lastState);
                    return 0;
                }
                x1 = x0 + stepLen;
                mult = x1 / gMag;
                if(Double.isNaN(mult))
                {
                    hitBottom   = true;
                    func.setState(lastState);
                    return 0;
                }
                for(int i = 0; i < testState.length; i++)
                    testState[i] = lastState[i] - mult*grad[i];
                func.setState(testState);
                y1 = func.evaluate();
                if(y1 < y0) // we've bracketed it!
                {
                    break;
                }
                else // point 1 is still higher than point 0
                {
                    y2 = y1;
                    x2 = x1;
                }
            }
        }
        
        //System.err.println("  (x0,y0) = ("+df.format(x0)+", "+df.format(y0)+")");
        //System.err.println("  (x1,y1) = ("+df.format(x1)+", "+df.format(y1)+")");
        //System.err.println("  (x2,y2) = ("+df.format(x2)+", "+df.format(y2)+")");
        
        // Alright, we've got the bastard bracketed.
        // Let's fit some curves!
        // This is a fit to a quadratic function using Lagrange interpolation polynomials
        double x_ = 0.0, y_ = 0.0;
        for(int k = 0; k < 5; k++)
        {
            x_ = ((x1*x1 - x2*x2)*y0 + (x2*x2 - x0*x0)*y1 + (x0*x0 - x1*x1)*y2)
                / 2.0 / ((x1-x2)*y0 + (x2-x0)*y1 + (x0-x1)*y2);
            mult = x_ / gMag;
            if(Double.isNaN(mult))
            {
                x_ = x1;
                break;
            }
            else if(Math.abs(x1-x_)/x1 < 1e-4)
            {
                break; // close enough!
            }
            for(int i = 0; i < testState.length; i++)
                testState[i] = lastState[i] - mult*grad[i];
            func.setState(testState);
            y_ = func.evaluate();
            if(x_ > x1)
            {
                y0 = y1;
                x0 = x1;
                y1 = y_;
                x1 = x_;
            }
            else
            {
                y2 = y1;
                x2 = x1;
                y1 = y_;
                x1 = x_;
            }
            //System.err.println("  (x*,y*) = ("+df.format(x_)+", "+df.format(y_)+")");
        }
        
        stepLen = x_ * lineStep;
        return gMag;
    }
//}}}

//{{{ resetSearch, useConjugates, useLineSearch
//##############################################################################
    /** Resets the search path so the next step will be steepest descent. */
    public void resetSearch()
    { nSteps = 0; }
    
    /**
    * If false, all search paths will be steepest descent.
    * If true, each path will be conjugate to previous paths:
    * s' = g' + (g'.g'/g.g)*g
    * Defaults to true, because it usually converges faster.
    * For cases far from the minimum, steepest descents may be more robust.
    */
    public void useConjugates(boolean b)
    { useConjDir = b; }
    
    /**
    * If false, all search paths will be arbitrary steps.
    * If true, a line search will be performed along each path to find the minimum.
    * Defaults to false, because it's more expensive computationally.
    */
    public void useLineSearch(boolean b)
    { useLineSrch = b; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

