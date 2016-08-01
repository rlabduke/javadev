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
* <code>SimpleNonbondedTerm</code> uses simple but inefficient algorithms to
* do non-bonded calculations. May be faster for small systems.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul 14 10:30:12 EDT 2004
*/
public class SimpleNonbondedTerm //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    final double cutoff, cutoff_2;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SimpleNonbondedTerm(double cutoff)
    {
        super();
        this.cutoff     = cutoff;
        this.cutoff_2   = cutoff*cutoff;
    }
//}}}

//{{{ countInteractions
//##############################################################################
    /** Counts the number of pairs within interaction distance. */
    public int countInteractions(double[] state)
    {
        int i, j, count = 0, len = state.length;
        for(i = 0; i < len; i += 3)
        {
            double ix, iy, iz;
            ix = state[i  ];
            iy = state[i+1];
            iz = state[i+2];
            for(j = i+3; j < len; j += 3)
            {
                double dx, dy, dz, rij_2;
                dx = ix - state[j  ];
                dy = iy - state[j+1];
                dz = iz - state[j+2];
                rij_2 = dx*dx + dy*dy + dz*dz;
                if(rij_2 <= cutoff_2) count++;
            }
        }
        
        return count;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

