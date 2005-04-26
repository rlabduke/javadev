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
* <code>PotentialFunction</code> is a generic interface for functions
* that can evaluate how favorable a given state is (energy),
* and provide the gradient of that energy function.
* States are described as vectors of doubles.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 28 07:59:57 EST 2003
*/
public interface PotentialFunction //extends ... implements ...
{
    /**
    * Reads out the current state of the system being evaluated.
    * Typically, this corresponds to updating the coordinates
    * of some points in R3.
    * @param outState an optional array to overwrite with the return values.
    *   If null or not the right length, a new array will be allocated.
    */
    public double[] getState(double[] outState);
    
    /**
    * Establishes a new state for the system being evaluated.
    * Typically, this corresponds to updating the coordinates
    * of some points in R3.
    * @param newState a vector of doubles specifying the new state.
    */
    public void setState(double[] newState);

    /**
    * Returns the value of the potential function
    * for the current state.
    * Caching this value is strongly recommended.
    */
    public double evaluate();

    /**
    * Calculates the gradient of the potential function
    * at the current state.
    * The gradient is a vector of all the partial first derivatives.
    * @param outGrad an optional array to overwrite with the return values.
    *   If null or not the right length, a new array will be allocated.
    */
    public double[] gradient(double[] outGrad);
}//class

