// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package silk;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
import driftwood.util.Strings;
//}}}
/**
* <code>NDimTable_Sparse</code> uses a special hashtable to handle very large
* NDimTables in which most entries are 0.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jan 11 10:08:56 EST 2006
*/
public class NDimTable_Sparse extends NDimTable
{
//{{{ Constants
    static final long HASH_EMPTY = -1; // should be a bin index that never occurs
//}}}

//{{{ Variable definitions
//##############################################################################
    long[] indexHash;
    int indexHashCount = 0; // how many slots have been filled in indexHash
    final double loadFactor = 0.75;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a new <i>n</i>-dimensional lookup table of floating point numbers.
    *
    * @param name    the name of this table, for later identification
    * @param ndim    <i>n</i>, the number of dimensions
    * @param min     the minimum ranges of each dimension
    * @param max     the maximum ranges of each dimension
    * @param bins    the number of bins in each dimension
    * @param wrap    whether or not to wrap in each dimension
    * @param maxused the maxiumum bins to be non-zero
    */
    public NDimTable_Sparse(String name, int ndim, double[] min, double[] max, int[] bins, boolean[] wrap, int maxused)
    {
        super(name, ndim, min, max, bins, wrap);

        // Allocate table and initialize it
        lookupTable = new double[maxused];
        indexHash = new long[maxused];
        zero();
    }
    
    /**
    * Creates a NDimTable_Sparse without initializing any of its fields.
    * For use by functions that reconstruct a table from some stored form.
    */
    protected NDimTable_Sparse()
    { super(); }
//}}}

//{{{ long2hash
//##################################################################################################
    // All these functions start by computing an index into an imaginary dense table
    // big enough to hold all the possible cells, just as is done in NDimTable_Dense,
    // only we have to use a LONG because that table would be so huge.
    // Then we map it back down to an INT via a custom hashtable implementation.
    // The hashtable cannot delete elements, but it can be resized.
    
    // Takes a 64-bit "index" and converts it to a 32-bit offset into lookupTable.
    // If you're going to store a value, pass in claimIt = true
    int long2hash(long which, boolean claimIt)
    {
        if(which < 0)
            throw new IndexOutOfBoundsException("negative index into full table: "+which);
        int hash = (int)(((which >> 32) ^ which) & 0xffffffffL);
        // For quadratic probing with triangular numbers: 1, 3, 6, 10, 15, 21, 28, ...
        int triNum = 0, triStep = 0;
        while(true)
        {
            triNum = triNum + triStep++;
            int idx = Math.abs(hash+triNum) % indexHash.length;
            if(indexHash[idx] == which) return idx;
            else if(indexHash[idx] == HASH_EMPTY)
            {
                if(claimIt)
                {
                    indexHash[idx] = which;
                    indexHashCount++;
                    if(indexHashCount > loadFactor * indexHash.length)
                    {
                        //throw new IllegalStateException("hash table is full");
                        rehash();
                        return long2hash(which, claimIt); // idx will have moved
                    }
                }
                return idx;
            }
        }
    }
    
    private void rehash()
    {
        NDimTable_Sparse that = new NDimTable_Sparse(this.ourName, this.nDim,
            this.minVal, this.maxVal, this.nBins, this.doWrap, 2*this.indexHash.length);
        for(int i = 0; i < indexHash.length; i++)
        {
            long which = indexHash[i];
            if(which == HASH_EMPTY) continue;
            int idx = that.long2hash(which, true);
            that.lookupTable[idx] = this.lookupTable[i];
        }
        this.lookupTable = that.lookupTable;
        this.indexHash = that.indexHash;
        if(this.indexHashCount != that.indexHashCount)
            throw new RuntimeException("hash table changed size: "+this.indexHashCount+" -> "+that.indexHashCount);
    }
//}}}

//{{{ bin2index, bin2index_limit, index2bin
//##################################################################################################
    // Takes a set of bin numbers and produces a linear offset into an imaginary, full-size table.
    // If no bin can be found after wrapping is applied, returns -1.
    long bin2index(int[] where)
    {
        long which = 0;
        int bin, i;
        for(i = 0; i < nDim-1; i++)
        {
            bin = wrapbin(where[i], i);
            if(bin < 0 || bin >= nBins[i]) return -1;
            which += bin;
            which *= nBins[i+1];
        }
        bin = wrapbin(where[i], i);
        if(bin < 0 || bin >= nBins[i]) return -1;
        which += bin;
        return which;
    }

    // Takes a set of bin numbers and produces a linear offset into an imaginary, full-size table.
    // If no bin can be found after wrapping is applied, the edge of the table is used.
    long bin2index_limit(int[] where)
    {
        long which = 0;
        int bin, i;
        for(i = 0; i < nDim-1; i++)
        {
            bin = Math.min(nBins[i]-1, Math.max(wrapbin(where[i], i), 0));
            which += bin;
            which *= nBins[i+1];
        }
        bin = Math.min(nBins[i]-1, Math.max(wrapbin(where[i], i), 0));
        which += bin;
        return which;
    }

    // Takes an index into lookuptable and regenerates a set of bin numbers
    // where[] is overwritten
    void index2bin(int idx, int[] where)
    {
        long which = indexHash[idx];
        if(which == HASH_EMPTY)
            throw new IllegalArgumentException(idx+" is not a claimed index in the indexHash[]");
        for(int i = nDim-1; i >= 0; i--)
        {
            where[i] = (int)(which % nBins[i]);
            which -= where[i];
            which /= nBins[i];
        }
    }
//}}}

//{{{ set/addValueAt, valueAt, valueAtLimit
//##################################################################################################
    public void setValueAt(int[] where, double value)
    {
        long which = bin2index(where);
        int idx = long2hash(which, true);
        lookupTable[idx] = value;
    }

    public void addValueAt(int[] where, double value)
    {
        long which = bin2index(where);
        int idx = long2hash(which, true);
        lookupTable[idx] += value;
    }

    public double valueAt(int[] where)
    {
        long which = bin2index(where);
        int idx = long2hash(which, false);
        return lookupTable[idx];
    }

    public double valueAtLimit(int[] where)
    {
        long which = bin2index_limit(where);
        int idx = long2hash(which, false);
        return lookupTable[idx];
    }
//}}}

//{{{ zero, transformTrueNaturalLog
//##################################################################################################
    public void zero()
    {
        super.zero();
        for(int i = 0; i < indexHash.length; i++) indexHash[i] = HASH_EMPTY;
    }
    
    /**
    * Transforms every value <i>v</i> in the table on a log scale.
    * The mapping <i>v</i> --&gt; <i>v'</i> is as follows:
    * <p>v' = ln(v)
    * <p>Note that if <i>v</i> is zero, <i>v'</i> will be negative infinity!
    */
    public void transformTrueNaturalLog()
    {
        // The other transforms map 0 -> 0, but this one requires special checks for sparse data:
        for(int i = 0; i < lookupTable.length; i++)
            if(indexHash[i] != HASH_EMPTY)
                lookupTable[i] = Math.log(lookupTable[i]);
    }
//}}}

//{{{ classifyByHills
//##############################################################################
    int[] hillTable = null; // holds the labels, since we can't climb in place!
    
    /**
    * Traverses the table, transforming each positive (non-zero) value into
    * a negative integer label, such that each point is labeled according to
    * which hill it belongs to.
    * <p>In an N-dimensional space, each of the 3^N - 1 neighbor points
    * (ie, diagonal neighbors are included) is queried, and the largest
    * positive value is followed recursively, until the top is reached.
    */
    public void classifyByHills()
    {
        tgf_b = 1.0; // used for next unused label
        hillTable = new int[lookupTable.length];
        
        for(int i = 0; i < indexHash.length; i++)
        {
            long which = indexHash[i];
            if(which == HASH_EMPTY) continue;
            recurseHills(which);
        }
        
        for(int i = 0; i < lookupTable.length; i++)
            lookupTable[i] = hillTable[i];
        hillTable = null;
    }
    
    private double recurseHills(long which)
    {
        int hash = long2hash(which, false);
        double val = lookupTable[hash], label = hillTable[hash];
        if(val == 0)    return 0;       // should only happen directly from classifyByHills()
        if(label != 0)  return label;   // already converted to a label
        
        // Does a neighbor have a higher value than this cell?
        // yes: call recurseHills() on that cell, and use that label
        // no: give this cell a new label, and return it
        //
        // This is recursive with shared (instance) variables, BUT
        // we get all the goody out of them before this function itself recurses,
        // so it *should* all be OK...
        index2bin(hash, va_current);    // best bin so far: this one
        tgf_a = val;                    // best value so far: this one
        System.arraycopy(va_current, 0, tgf_curr, 0, nDim); // find current bin, then go one left and one right:
        for(int i = 0; i < nDim; i++)
        {
            tgf_start[i] = tgf_curr[i]-1;   // min bound for loop
            tgf_end[i]   = tgf_curr[i]+1;   // max bound for loop
            tgf_curr[i]  = tgf_start[i];    // curr loop indices: start at start
        }
        hillsRecurseMaxNeighbor(0);
        
        long nextWhich = bin2index(va_current);
        if(nextWhich == which) // we're the highest value: we've reached the hill top
        {
            label = tgf_b++; // next label is more positive than this one: +1, +2, +3, ...
            System.err.print("    peak "+((int)label)+":");
            centerOf(va_current, tgf_pt);
            for(int i = 0; i < nDim; i++) System.err.print(" "+tgf_pt[i]);
            System.err.println(" -> "+tgf_a);
        }
        else // further up and further in: try the next highest point
            label = recurseHills(nextWhich);
        hillTable[hash] = (int) label;
        return label;
    }
    
    // Use recursion to loop over an unknown number of dimensions.
    // Uses tgf_start/end/curr for looping; va_current and tgf_a for best found so far.
    private void hillsRecurseMaxNeighbor(int depth)
    {
        // Set up a loop and go down to the next level
        if(depth < nDim)
        {
            for(tgf_curr[depth] = tgf_start[depth]; tgf_curr[depth] <= tgf_end[depth]; tgf_curr[depth]++)
                hillsRecurseMaxNeighbor(depth+1);
        }
        // We're at the bottom -- is this cell better than the current best?
        else
        {
            // Make sure this is a real bin before we get going...
            if(contains(tgf_curr))
            {
                // It's a real bin, or at least it wraps to a real bin.
                double val = valueAt(tgf_curr);
                // Have we done better? Using >= means tied neighbors will drift toward higher indices...
                if(val >= tgf_a)
                {
                    tgf_a = val;
                    System.arraycopy(tgf_curr, 0, va_current, 0, nDim);
                }
            }
        }
    }
//}}}

//{{{ createFromText
//##################################################################################################
    /**
    * Recreates an <i>n</i>-dimensional lookup table of floating point numbers from a file.
    * See <code>writeText()</code> for details of the format.
    *
    * @param input The stream to read from
    *
    * @throws IOException if there is a problem reading from the stream
    * @throws NumberFormatException if there is a mis-formatted number
    */
    public static NDimTable createFromText(InputStream input) throws IOException, NumberFormatException
    {
        LineNumberReader in = new LineNumberReader(new InputStreamReader(input));
        NDimTable_Sparse ndt = new NDimTable_Sparse();
        
        // Extract our name
        String s = getLine(in);
        if(s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length()-1);
        ndt.ourName = s;

        // How many dimensions?
        ndt.nDim = Integer.parseInt(getLine(in));

        // Allocate storage for minVal, maxVal, nBins, doWrap
        ndt.minVal  = new   double[ndt.nDim];
        ndt.maxVal  = new   double[ndt.nDim];
        ndt.nBins   = new     int[ndt.nDim];
        ndt.doWrap  = new boolean[ndt.nDim];

        // Read them in
        int i;
        for(i = 0; i < ndt.nDim; )
        {
            s = getLine(in);
            if(s.equals("lower_bound  upper_bound  number_of_bins  wrapping")) continue;
            String[] parts  = Strings.explode(s, ' ');
            ndt.minVal[i]   = Double.parseDouble(parts[0]);
            ndt.maxVal[i]   = Double.parseDouble(parts[1]);
            ndt.nBins[i]    = Integer.parseInt(parts[2]);
            if(parts[3].equalsIgnoreCase("true")
            || parts[3].equalsIgnoreCase("yes")
            || parts[3].equalsIgnoreCase("on")
            || parts[3].equalsIgnoreCase("1"))
                ndt.doWrap[i] = true;
            else ndt.doWrap[i] = false;
            i++;
        }

        // We no longer know how many real points there were...
        ndt.realcount = 0;

        // Calculate the bin widths.
        ndt.wBin = new double[ndt.nDim];
        for(i = 0; i < ndt.nDim; i++) ndt.wBin[i] = (ndt.maxVal[i] - ndt.minVal[i]) / ndt.nBins[i];

        // Allocate storage for the tgfXXX variables
        ndt.tgf_start   = new int[ndt.nDim];
        ndt.tgf_end     = new int[ndt.nDim];
        ndt.tgf_curr    = new int[ndt.nDim];
        ndt.tgf_bin_ctr = new double[ndt.nDim];

        // Allocate storage for the vaXXX variables
        ndt.va_home     = new int[ndt.nDim];
        ndt.va_home_ctr = new double[ndt.nDim];
        ndt.va_neighbor = new int[ndt.nDim];
        ndt.va_current  = new int[ndt.nDim];
        ndt.va_contrib  = new double[ndt.nDim];

        // Calculate number of entries in lookupTable and allocate it.
        // Allocate table and initialize it
        final int maxused = 1000; // just a guess right now
        ndt.lookupTable = new double[maxused];
        ndt.indexHash = new long[maxused];
        ndt.zero();
        
        double[]    currPt      = new double[ndt.nDim];
        int[]       currBin     = new int[ndt.nDim];
        boolean     valuesFirst = (getLine(in).indexOf("first") != -1);
        
        while(true)
        {
            s = in.readLine(); if(s == null) break;
            double val = explodeDoubles(s, currPt, valuesFirst);
            if(val == 0) continue;
            ndt.whereIs(currPt, currBin);
            ndt.setValueAt(currBin, val);
        }
        
        return ndt;
    }
    
    /** Returns a trimmed line, discarding everything up to and including the first colon. */
    static private String getLine(LineNumberReader in) throws IOException
    {
        String s = in.readLine();
        if(s == null) throw new EOFException("No lines remaining in "+in);
        if(s.indexOf(':') > 0) s = s.substring(s.indexOf(':')+1);
        return s.trim();
    }
    
    static private double explodeDoubles(String s, double[] doubles, boolean valueFirst) throws NumberFormatException
    {
        String[] strings = Strings.explode(s, ' ');
        if(valueFirst)
        {
            for(int i = 0; i < doubles.length; i++)
                doubles[i] = Double.parseDouble(strings[i+1]);
            return Double.parseDouble(strings[0]);
        }
        else
        {
            for(int i = 0; i < doubles.length; i++)
                doubles[i] = Double.parseDouble(strings[i]);
            return Double.parseDouble(strings[doubles.length]);
        }
    }
//}}}

//{{{ writeNDFT
//##################################################################################################
    /**
    * Not implemented -- the resulting file would be too big.
    */
    public void writeNDFT(DataOutputStream out) throws IOException
    {
        throw new IOException("Function not implemented");
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

