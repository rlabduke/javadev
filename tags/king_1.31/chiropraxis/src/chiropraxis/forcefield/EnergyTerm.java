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
* <code>EnergyTerm</code> is a generic interface for the terms of a
* molecular-mechanics-style "force field" that can have first derivatives
* computed (preferably analytically).
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jul 12 08:49:45 EDT 2004
*/
public interface EnergyTerm //extends ... implements ...
{
    /**
    * Calculates the energy contribution of this term
    * based on the given system state.
    * @param state the state of the system, as represented by a (read-only)
    *   vector of real numbers.
    * @return the energy of this term, in unspecified units.
    */
    public double eval(double[] state);

    /**
    * Calculates the energy contribution of this term
    * based on the given system state, as well as the gradient.
    * @param state the state of the system, as represented by a (read-only)
    *   vector of real numbers.
    * @param gradient equal in length to <code>state</code>, the components
    *   of the gradient should be added to this array. If it is [0] coming in,
    *   the output will be exactly the gradient of this term alone.
    *   (The "gradient" is the vector of partial first derivatives
    *   of this term with respect to each variable of state in turn.
    *   The negative of the gradient is the force vector.)
    * @return the energy of this term, in unspecified units.
    */
    public double eval(double[] state, double[] gradient);
}//class

