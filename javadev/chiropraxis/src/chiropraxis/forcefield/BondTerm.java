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
* <code>BondTerm</code> is a harmonic bond-stretching term of a force field.
* The energy <code>E = k * (r - r0)^2</code>, and
* the gradient <code>G = 2k * (r - r0) * (xa - xb) / r</code>.
*
* Analytic derivatives based on my own calculations and on
* Britt H. Park (2003) "Some Useful Math for Molecular Mechanics"
* http://www.sciencething.org/MD.html
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jul 12 08:49:45 EDT 2004
*/
public class BondTerm implements EnergyTerm
{
//{{{ Constants
    /** Endpoints are always assumed to be distinct, so there has to be
    * some minimum distance between them to avoid divide-by-zero errors. */
    static final double MIN_R = 1e-6;
//}}}

//{{{ Variable definitions
//##############################################################################
    int a, b;       // indexes of the first coordinate of the two endpoints
    double k;       // the spring constant
    double r0;      // the target or ideal bond length
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BondTerm(int atomA, int atomB, double targetDist, double springConst)
    {
        super();
        a   = atomA * 3;
        b   = atomB * 3;
        r0  = targetDist;
        k   = springConst;
    }
//}}}

//{{{ eval
//##############################################################################
    public double eval(double[] state, double[] gradient)
    {
        // energy
        double dx, dy, dz, r, dr, E;
        dx = state[a  ] - state[b  ]; // sign is correct for a, opposite for b
        dy = state[a+1] - state[b+1];
        dz = state[a+2] - state[b+2];
        r = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if(r < MIN_R) r = MIN_R;
        dr = r - r0;
        E = k * (dr*dr);
        
        // gradient
        double g, gx, gy, gz;
        g = 2 * k * dr / r;
        gx = g * dx;
        gy = g * dy;
        gz = g * dz;
        gradient[a  ] += gx;
        gradient[a+1] += gy;
        gradient[a+2] += gz;
        gradient[b  ] -= gx;
        gradient[b+1] -= gy;
        gradient[b+2] -= gz;
        
        return E;
    }

    public double eval(double[] state)
    {
        double dx, dy, dz, r, dr;
        dx = state[a  ] - state[b  ];
        dy = state[a+1] - state[b+1];
        dz = state[a+2] - state[b+2];
        r = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if(r < MIN_R) r = MIN_R;
        dr = r - r0;
        return k * (dr*dr);
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
        PrintStream out = System.out;
        
        double E;
        double[] state, grad;

        state   = new double[] {1,0,0,-1,0,0};
        grad    = new double[] {0,0,0,0,0,0};
        BondTerm term1a = new BondTerm(0, 1, 1, 1);
        E = term1a.eval(state, grad);
        print(out, state, E, grad);

        grad    = new double[] {0,0,0,0,0,0};
        BondTerm term1b = new BondTerm(0, 1, 2, 1);
        E = term1b.eval(state, grad);
        print(out, state, E, grad);

        state   = new double[] {0,1,0,0,-1,0};
        grad    = new double[] {0,0,0,0,0,0};
        BondTerm term2 = new BondTerm(0, 1, 1, 1);
        E = term2.eval(state, grad);
        print(out, state, E, grad);

        state   = new double[] {0,0,1,0,0,-1};
        grad    = new double[] {0,0,0,0,0,0};
        BondTerm term3 = new BondTerm(0, 1, 1, 1);
        E = term3.eval(state, grad);
        print(out, state, E, grad);

        state   = new double[] {1, 1, 1, -1, -1, -1};
        grad    = new double[] {0,0,0,0,0,0};
        BondTerm term4 = new BondTerm(0, 1, 1, 1);
        E = term4.eval(state, grad);
        print(out, state, E, grad);
    }
    
    static void print(PrintStream out, double[] state, double energy, double[] grad)
    {
        DecimalFormat df = new DecimalFormat("0.000");
        out.print("State:");
        for(int i = 0; i < state.length; i++) out.print(" "+df.format(state[i]));
        out.println();
        out.println("Energy: "+df.format(energy));
        out.print("Gradient:");
        for(int i = 0; i < grad.length; i++) out.print(" "+df.format(grad[i]));
        out.println();
        out.println();
    }
//}}}
*/
}//class

