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
* <code>AngleTerm</code> is a harmonic bond-angle-bending term of a force field.
* Following the example of T. Schlick, we evaluate an expression of cos(T) to
* save calls to trigonometric and inverse trig. functions.
* Thus, <code>E = k * (cos(T) - cos(T0))^2</code>, and the derivatives
* are too complex to write out here.
*
* Analytic derivatives based on my own calculations and on
* Britt H. Park (2003) "Some Useful Math for Molecular Mechanics"
* http://www.sciencething.org/MD.html
* and
* Tamar Schlick (1989)
* "A Recipe for Evaluating and Differentiating cos PHI Expressions"
* Journal of Computational Chemistry, 10(7): 951-956.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jul 12 10:02:42 EDT 2004
*/
public class AngleTerm implements EnergyTerm
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    int a, b, c;        // indexes of the first coordinate of the points (angle a-b-c)
    double k;           // the spring constant
    double cos_t0;      // the target or ideal cosine bond angle
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Target angle is in degrees. */
    public AngleTerm(int atomA, int atomB, int atomC, double targetAngle, double springConst)
    {
        super();
        a       = atomA * 3;
        b       = atomB * 3;
        c       = atomC * 3;
        k       = springConst;
        cos_t0  = Math.cos( Math.toRadians(targetAngle) );
    }
//}}}

//{{{ eval
//##############################################################################
    public double eval(double[] state, double[] gradient)
    {
        // Let u = a - b, v = c - b
        double ux, uy, uz, vx, vy, vz;
        ux = state[a  ] - state[b  ];
        uy = state[a+1] - state[b+1];
        uz = state[a+2] - state[b+2];
        vx = state[c  ] - state[b  ];
        vy = state[c+1] - state[b+1];
        vz = state[c+2] - state[b+2];
        
        // Numerator of cos(t): u.v -- aka N~
        double numer = ux*vx + uy*vy + uz*vz;
        
        // Denominator of cos(t): sqrt[ (u.u)(v.v) ] -- aka D~
        double denom2 = (ux*ux + uy*uy + uz*uz) * (vx*vx + vy*vy + vz*vz);
        double denom = Math.sqrt(denom2);
        
        // Value of cos(t), and the energy
        double cos_t, dcos_t, E;
        cos_t = numer / denom;
        dcos_t = cos_t - cos_t0;
        E = k * (dcos_t*dcos_t);
        
        /*
        // Derivatives of the u.v dot product, which go in the Numerator
        double dNax, dNay, dNaz, dNbx, dNby, dNbz, dNcx, dNcy, dNcz;
        dNax = 0*ux + 1*vx;
        dNcx = 1*ux + 0*vx;
        dNbx = -1*ux + -1*vx;
        
        // Derivatives of the lengths of u and v, which go in the Denomenator
        // These are the derivatives of D, not of D~
        double dDax, dDay, dDaz, dDbx, dDby, dDbz, dDcx, dDcy, dDcz;
        dDax = 2*ux;
        dDcx = 2*vx;
        dDbx = -2*(ux) + -2*(vx);
        */
        
        // Actual derivative of cos_t:
        // 1/D~ * [dN~/dx - dD~/dx * cos_t ]
        // [dN~/dx - 1/2D~ * dD/dx * cos_t ] / D~
        // [dN~/dx - 1/2D~ * 2(ux) * N~/D~] / D~
        // [dN~/dx - N~/(D~^2) * (ux) ] / D~
        // Gradient = 2k*(cos_t - cos_t0)*dcos_t/dx
        
        double scale = 2 * k * dcos_t / denom;
        double uvx = (ux+vx), uvy = (uy+vy), uvz = (uz+vz);
        gradient[a  ] = scale * (vx - numer*ux/denom2);
        gradient[a+1] = scale * (vy - numer*uy/denom2);
        gradient[a+2] = scale * (vz - numer*uz/denom2);
        gradient[c  ] = scale * (ux - numer*vx/denom2);
        gradient[c+1] = scale * (uy - numer*vy/denom2);
        gradient[c+2] = scale * (uz - numer*vz/denom2);
        gradient[b  ] = scale * (numer*uvx/denom2 - uvx);
        gradient[b+1] = scale * (numer*uvy/denom2 - uvy);
        gradient[b+2] = scale * (numer*uvz/denom2 - uvz);
        
        return E;
        
    }
    
    public double eval(double[] state)
    {
        // Let u = a - b, v = c - b
        double ux, uy, uz, vx, vy, vz;
        ux = state[a  ] - state[b  ];
        uy = state[a+1] - state[b+1];
        uz = state[a+2] - state[b+2];
        vx = state[c  ] - state[b  ];
        vy = state[c+1] - state[b+1];
        vz = state[c+2] - state[b+2];
        
        // Numerator of cos(t): u.v -- aka N~
        double numer = ux*vx + uy*vy + uz*vz;
        
        // Denominator of cos(t): sqrt[ (u.u)(v.v) ] -- aka D~
        double denom2 = (ux*ux + uy*uy + uz*uz) * (vx*vx + vy*vy + vz*vz);
        double denom = Math.sqrt(denom2);
        
        // Value of cos(t), and the energy
        double cos_t, dcos_t;
        cos_t = numer / denom;
        dcos_t = cos_t - cos_t0;
        return k * (dcos_t*dcos_t);
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

        state   = new double[] {1,0,0, 0,1,0, -1,0,0};
        grad    = new double[] {0,0,0,0,0,0,0,0,0};
        AngleTerm term1a = new AngleTerm(0,1,2,120,1);
        E = term1a.eval(state, grad);
        print(out, state, E, grad);

        grad    = new double[] {0,0,0,0,0,0,0,0,0};
        AngleTerm term1b = new AngleTerm(0,1,2,90,1);
        E = term1b.eval(state, grad);
        print(out, state, E, grad);

        state   = new double[] {0,1,0, 0,0,1, 0,-1,0};
        grad    = new double[] {0,0,0,0,0,0,0,0,0};
        AngleTerm term2 = new AngleTerm(0,1,2,120,1);
        E = term2.eval(state, grad);
        print(out, state, E, grad);

        state   = new double[] {0,0,1, 1,0,0, 0,0,-1};
        grad    = new double[] {0,0,0,0,0,0,0,0,0};
        AngleTerm term3 = new AngleTerm(0,1,2,120,1);
        E = term3.eval(state, grad);
        print(out, state, E, grad);

        state   = new double[] {1,1,1, 0,0,0, -1,-1,-1};
        grad    = new double[] {0,0,0,0,0,0,0,0,0};
        AngleTerm term4 = new AngleTerm(0,1,2,120,1);
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
