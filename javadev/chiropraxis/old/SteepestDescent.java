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
* <code>SteepestDescent</code> performs steepest-descent
* minimization of a PotentialFunction with arbitrary step length.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 28 07:59:57 EST 2003
*/
public class SteepestDescent //extends ... implements ...
{
//{{{ Constants
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
    
    /** The current step length. */
    double              stepLen     = 1.0;
    /** A multiplier for successful steps (future ones will be longer). */
    double              goodStep    = 1.2;
    /** A multiplier for failed steps (future ones will be shorter). */
    double              badStep     = 0.5;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SteepestDescent(PotentialFunction energyFunc)
    {
        super();
        func        = energyFunc;
    }
//}}}

//{{{ step
//##############################################################################
    /**
    * Executes one step of steepest-descent minimization on
    * the system using the potential function.
    * @return the magnitude of the gradient at the start of this step.
    */
    public double step()
    {
        // Get the starting state
        lastState = func.getState(lastState); // created iff it didn't exist
        if(testState == null) testState = new double[ lastState.length ];
        
        // Get the energy and gradient at the starting state
        double startE = func.evaluate();
        grad = func.gradient(grad); // created iff it didn't exist
        
        // Calculate magnitude of the gradient
        double gMag = 0.0;
        for(int i = 0; i < grad.length; i++)
            gMag += grad[i] * grad[i];
        gMag = Math.sqrt(gMag);

        // Try arbitrary steps against the gradient
        // until we reduce the energy.
        while(true)
        {
            // Step against the gradient
            double mult = stepLen / gMag;
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
            else
            {
                stepLen = 1e-10;    // an arbitrary small value
                gMag    = 0;
                break;
            }
        }
        
        return gMag;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

