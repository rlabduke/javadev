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
* <code>NonbondedTerm</code> uses a hash table / 3-D bins approach to
* make computing non-bonded terms more efficient. This seems to be a win over
* a simple all-against-all strategy for any more than a few hundred points.
* <p>Following in the footsteps of Sculpt, we use a Lennard-Jones-like 4-8
* potential with a standard Coulombic one in a vacuum, so
* <code>E = A/r^8 - B/r^4 + Qij/r</code>.
* The derivatives are straight-forward and based on my own calculations.
*
* <p>The standard form for the Lennard-Jones potential is
* <br>A/r^12 - B/r^6
* <br>where the first term is repulsive and the second is attractive.
*
* <p>A nicer formulation uses the optimal separation r0 and the well depth e:
* <br>e*[(r0/r)^12 - 2*(r0/r)^6]
*
* <p>Given e and r0, you can find A and B as follows:
* <br>A = e * r0^12
* <br>B = 2e * r0^6
*
* <p>The same argument holds easily for the 4-8 potential used here.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul 14 10:30:12 EDT 2004
*/
public class NonbondedTerm implements EnergyTerm
{
//{{{ Constants
    /** Number of different atom types. Max index must be one less than this. */
    public static final int NUM_TYPES = 2;
//}}}

//{{{ CLASS: Key
//##############################################################################
    static class Key
    {
        static final int HALF = (1 << 20);
        
        long index = -1;
        int  hashCode = -1;
        
        public Key()
        {}
        
        public Key(int i, int j, int k)
        { set(i, j, k); }
        
        /** i, j, k must be between -HALF and HALF-1 */
        public void set(int i, int j, int k)
        {
            index = ((i+HALF))
                | ((j+HALF) << 21)
                | ((k+HALF) << 42);
            hashCode = (int)(index ^ (index >> 32));
        }
        
        public int hashCode()
        { return hashCode; }
        
        public boolean equals(Object o)
        { return (o == null ? false : ((Key)o).index == this.index); }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    int[]           atomType;
    double[]        Aij; // vdW repulsive term
    double[]        Bij; // vdw attractive term
    double[]        Qij; // electrostatics: qi * qj
    final double    cutoff, cutoff_2;
    int             initCap;
    HashMap         lookup;
    Key             tmpkey;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public NonbondedTerm(int[] atomType, double cutoff, int initCap)
    {
        super();
        this.atomType   = (int[]) atomType.clone();
        this.Aij        = new double[ NUM_TYPES*NUM_TYPES ];
        this.Bij        = new double[ NUM_TYPES*NUM_TYPES ];
        this.Qij        = new double[ NUM_TYPES*NUM_TYPES ];
        
        this.Aij        = new double[] {0, 0, 0, 1};
        this.Bij        = new double[] {0, 0, 0, 2};
        this.Qij        = new double[] {0, 0, 0, 0};
        
        this.cutoff     = cutoff;
        this.cutoff_2   = cutoff*cutoff;
        this.initCap    = initCap;
        this.lookup     = new HashMap();
        this.tmpkey     = new Key();
    }
//}}}

//{{{ buildLookupTable
//##############################################################################
    void buildLookupTable(double[] state)
    {
        lookup.clear();
        int i, j, len = 3 * (state.length / 3); // rounds down non-multiple of 3
        for(i = len - 3 ; i >= 0; i -= 3)
        {
            int a, b, c;
            a = (int)(state[i  ] / cutoff);
            b = (int)(state[i+1] / cutoff);
            c = (int)(state[i+2] / cutoff);
            tmpkey.set(a, b, c);
            
            int[] cell = (int[]) lookup.get(tmpkey);
            if(cell == null)
            {
                cell = new int[initCap];
                cell[0] = i;
                for(j = 1; j < cell.length; j++) cell[j] = -1; 
                lookup.put(tmpkey, cell);
                tmpkey = new Key();
            }
            else if(cell[cell.length-1] != -1)
            {
                int[] newCell = new int[2*cell.length];
                for(j = 0; j < cell.length; j++) newCell[j] = cell[j];
                newCell[j++] = i;
                for( ; j < newCell.length; j++) newCell[j] = -1;
                lookup.put(tmpkey, newCell);
                tmpkey = new Key();
            }
            else
            {
                for(j = 0; j < cell.length; j++)
                {
                    if(cell[j] == -1)
                    {
                        cell[j] = i;
                        break;
                    }
                }
            }
        }//for each point
    }
//}}}

//{{{ eval(state)
//##############################################################################
    public double eval(double[] state)
    {
        buildLookupTable(state);
        
        int i, p = 0, type, len = state.length;
        double energy = 0;
        for(i = 0; i < len; i += 3)
        {
            double x, y, z;
            x = state[i  ];
            y = state[i+1];
            z = state[i+2];
            int a, b, c;
            a = (int)(x / cutoff);
            b = (int)(y / cutoff);
            c = (int)(z / cutoff);
            type = atomType[p];
            
            energy += evalCell(state, i, type, x, y, z, a+1, b+1, c+1);
            energy += evalCell(state, i, type, x, y, z, a+1, b+1, c  );
            energy += evalCell(state, i, type, x, y, z, a+1, b+1, c-1);
            energy += evalCell(state, i, type, x, y, z, a+1, b  , c+1);
            energy += evalCell(state, i, type, x, y, z, a+1, b  , c  );
            energy += evalCell(state, i, type, x, y, z, a+1, b  , c-1);
            energy += evalCell(state, i, type, x, y, z, a+1, b-1, c+1);
            energy += evalCell(state, i, type, x, y, z, a+1, b-1, c  );
            energy += evalCell(state, i, type, x, y, z, a+1, b-1, c-1);
            energy += evalCell(state, i, type, x, y, z, a  , b+1, c+1);
            energy += evalCell(state, i, type, x, y, z, a  , b+1, c  );
            energy += evalCell(state, i, type, x, y, z, a  , b+1, c-1);
            energy += evalCell(state, i, type, x, y, z, a  , b  , c+1);
            energy += evalCell(state, i, type, x, y, z, a  , b  , c  );
            energy += evalCell(state, i, type, x, y, z, a  , b  , c-1);
            energy += evalCell(state, i, type, x, y, z, a  , b-1, c+1);
            energy += evalCell(state, i, type, x, y, z, a  , b-1, c  );
            energy += evalCell(state, i, type, x, y, z, a  , b-1, c-1);
            energy += evalCell(state, i, type, x, y, z, a-1, b+1, c+1);
            energy += evalCell(state, i, type, x, y, z, a-1, b+1, c  );
            energy += evalCell(state, i, type, x, y, z, a-1, b+1, c-1);
            energy += evalCell(state, i, type, x, y, z, a-1, b  , c+1);
            energy += evalCell(state, i, type, x, y, z, a-1, b  , c  );
            energy += evalCell(state, i, type, x, y, z, a-1, b  , c-1);
            energy += evalCell(state, i, type, x, y, z, a-1, b-1, c+1);
            energy += evalCell(state, i, type, x, y, z, a-1, b-1, c  );
            energy += evalCell(state, i, type, x, y, z, a-1, b-1, c-1);
            
            p++;
        }
        return energy;
    }
//}}}

//{{{ eval(state, gradient)
//##############################################################################
    public double eval(double[] state, double[] gradient)
    {
        buildLookupTable(state);
        
        int i, p = 0, type, len = state.length;
        double energy = 0;
        for(i = 0; i < len; i += 3)
        {
            double x, y, z;
            x = state[i  ];
            y = state[i+1];
            z = state[i+2];
            int a, b, c;
            a = (int)(x / cutoff);
            b = (int)(y / cutoff);
            c = (int)(z / cutoff);
            type = atomType[p];
            
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b+1, c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b+1, c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b+1, c-1);
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b  , c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b  , c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b  , c-1);
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b-1, c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b-1, c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a+1, b-1, c-1);
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b+1, c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b+1, c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b+1, c-1);
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b  , c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b  , c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b  , c-1);
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b-1, c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b-1, c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a  , b-1, c-1);
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b+1, c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b+1, c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b+1, c-1);
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b  , c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b  , c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b  , c-1);
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b-1, c+1);
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b-1, c  );
            energy += evalCell(state, gradient, i, type, x, y, z, a-1, b-1, c-1);
            
            p++;
        }
        return energy;
    }
//}}}

//{{{ evalCell(state)
//##############################################################################
    double evalCell(double[] state, int i, int type, double x, double y, double z, int a, int b, int c)
    {
        tmpkey.set(a, b, c);
        int[] cell = (int[]) lookup.get(tmpkey);
        if(cell == null) return 0;
        
        double energy = 0;
        for(int k = 0; k < cell.length; k++)
        {
            int j = cell[k];
            if(j <= i) break;
            
            double dx, dy, dz, rij_2;
            dx = x - state[j  ];
            dy = y - state[j+1];
            dz = z - state[j+2];
            rij_2 = dx*dx + dy*dy + dz*dz;
            if(rij_2 > cutoff_2) continue;
            
            int typeIndex = type*NUM_TYPES + atomType[j/3];
            double rij_4 = rij_2 * rij_2;
            double rij_8 = rij_4 * rij_4;
            energy += Aij[typeIndex]/rij_8 - Bij[typeIndex]/rij_4;
            
            // Doing electrostatics separately may save us some sqrt() evals
            double Q = Qij[typeIndex];
            if(Q != 0) energy += Q / Math.sqrt(rij_2);
        }
        return energy;
    }
//}}}

//{{{ evalCell(state, gradient)
//##############################################################################
    double evalCell(double[] state, double[] gradient, int i, int type, double x, double y, double z, int a, int b, int c)
    {
        tmpkey.set(a, b, c);
        int[] cell = (int[]) lookup.get(tmpkey);
        if(cell == null) return 0;
        
        double energy = 0;
        for(int k = 0; k < cell.length; k++)
        {
            int j = cell[k];
            if(j <= i) break;
            
            double dx, dy, dz, rij_2;
            dx = x - state[j  ];
            dy = y - state[j+1];
            dz = z - state[j+2];
            rij_2 = dx*dx + dy*dy + dz*dz;
            if(rij_2 > cutoff_2) continue;
            
            int typeIndex = type*NUM_TYPES + atomType[j/3];
            double rij = Math.sqrt(rij_2);
            double rij_4 = rij_2 * rij_2;
            double rij_8 = rij_4 * rij_4;
            
            double eA, eB, eQ, eABQ, dex, dey, dez;
            eA = Aij[typeIndex]/rij_8;
            eB = -Bij[typeIndex]/rij_4;
            eQ = Qij[typeIndex]/rij;
            energy += eA + eB + eQ;
            
            eABQ = (-8*eA + -4*eB - eQ) / rij_2;
            dex = eABQ * dx;
            dey = eABQ * dy;
            dez = eABQ * dz;
            gradient[i  ] += dex;
            gradient[i+1] += dey;
            gradient[i+2] += dez;
            gradient[j  ] -= dex;
            gradient[j+1] -= dey;
            gradient[j+2] -= dez;
        }
        return energy;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

