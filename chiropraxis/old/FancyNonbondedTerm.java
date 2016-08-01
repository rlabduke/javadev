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
* <code>FancyNonbondedTerm</code> uses a hash table / 3-D bins approach to
* make computing non-bonded terms more efficient. This seems to be a win over
* a simple all-against-all strategy for any more than a few hundred points.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul 14 10:30:12 EDT 2004
*/
public class FancyNonbondedTerm //extends ... implements ...
{
//{{{ Constants
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
    final double cutoff, cutoff_2;
    int     initCap;
    HashMap lookup;
    Key     tmpkey;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FancyNonbondedTerm(double cutoff, int initCap)
    {
        super();
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

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ countInteractions
//##############################################################################
    /** Counts the number of pairs within interaction distance. */
    public int countInteractions(double[] state)
    {
        buildLookupTable(state);
        
        int i, count = 0, len = state.length;
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
            
            count += countNear(state, i, x, y, z, a+1, b+1, c+1);
            count += countNear(state, i, x, y, z, a+1, b+1, c  );
            count += countNear(state, i, x, y, z, a+1, b+1, c-1);
            count += countNear(state, i, x, y, z, a+1, b  , c+1);
            count += countNear(state, i, x, y, z, a+1, b  , c  );
            count += countNear(state, i, x, y, z, a+1, b  , c-1);
            count += countNear(state, i, x, y, z, a+1, b-1, c+1);
            count += countNear(state, i, x, y, z, a+1, b-1, c  );
            count += countNear(state, i, x, y, z, a+1, b-1, c-1);
            count += countNear(state, i, x, y, z, a  , b+1, c+1);
            count += countNear(state, i, x, y, z, a  , b+1, c  );
            count += countNear(state, i, x, y, z, a  , b+1, c-1);
            count += countNear(state, i, x, y, z, a  , b  , c+1);
            count += countNear(state, i, x, y, z, a  , b  , c  );
            count += countNear(state, i, x, y, z, a  , b  , c-1);
            count += countNear(state, i, x, y, z, a  , b-1, c+1);
            count += countNear(state, i, x, y, z, a  , b-1, c  );
            count += countNear(state, i, x, y, z, a  , b-1, c-1);
            count += countNear(state, i, x, y, z, a-1, b+1, c+1);
            count += countNear(state, i, x, y, z, a-1, b+1, c  );
            count += countNear(state, i, x, y, z, a-1, b+1, c-1);
            count += countNear(state, i, x, y, z, a-1, b  , c+1);
            count += countNear(state, i, x, y, z, a-1, b  , c  );
            count += countNear(state, i, x, y, z, a-1, b  , c-1);
            count += countNear(state, i, x, y, z, a-1, b-1, c+1);
            count += countNear(state, i, x, y, z, a-1, b-1, c  );
            count += countNear(state, i, x, y, z, a-1, b-1, c-1);
        }
        return count;
    }
//}}}

//{{{ countNear
//##############################################################################
    int countNear(double[] state, int which, double x, double y, double z, int a, int b, int c)
    {
        tmpkey.set(a, b, c);
        int[] cell = (int[]) lookup.get(tmpkey);
        if(cell == null) return 0;
        
        int i, j, count = 0;
        for(i = 0; i < cell.length; i++)
        {
            j = cell[i];
            if(j <= which) break;
            double dx, dy, dz, rij_2;
            dx = x - state[j  ];
            dy = y - state[j+1];
            dz = z - state[j+2];
            rij_2 = dx*dx + dy*dy + dz*dz;
            if(rij_2 <= cutoff_2) count++;
        }
        return count;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

