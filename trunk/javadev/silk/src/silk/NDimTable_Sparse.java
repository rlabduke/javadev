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

//{{{ long2index, rehash
//##################################################################################################
    // All these functions start by computing an index into an imaginary dense table
    // big enough to hold all the possible cells, just as is done in NDimTable_Dense,
    // only we have to use a LONG because that table would be so huge.
    // Then we map it back down to an INT via a custom hashtable implementation.
    // The hashtable cannot delete elements, but it can be resized.
    
    // Takes a 64-bit "index" and converts it to a 32-bit offset into lookupTable.
    // If you're going to store a value, pass in claimIt = true
    int long2index(long which, boolean claimIt)
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
                        return long2index(which, claimIt); // idx will have moved
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
            int idx = that.long2index(which, true);
            that.lookupTable[idx] = this.lookupTable[i];
        }
        this.lookupTable = that.lookupTable;
        this.indexHash = that.indexHash;
        if(this.indexHashCount != that.indexHashCount)
            throw new RuntimeException("hash table changed size: "+this.indexHashCount+" -> "+that.indexHashCount);
    }
//}}}

//{{{ index2bin, bin2index, set/addValueAt, valueAt, valueAtLimit
//##################################################################################################
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
    
    int bin2index(int[] where)
    { return long2index(bin2long(where), false); }
    
    public void setValueAt(int[] where, double value)
    {
        long which = bin2long(where);
        int idx = long2index(which, true);
        lookupTable[idx] = value;
    }

    public void addValueAt(int[] where, double value)
    {
        long which = bin2long(where);
        int idx = long2index(which, true);
        lookupTable[idx] += value;
    }

    public double valueAt(int[] where)
    {
        long which = bin2long(where);
        int idx = long2index(which, false);
        return lookupTable[idx];
    }

    public double valueAtLimit(int[] where)
    {
        long which = bin2long_limit(where);
        int idx = long2index(which, false);
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

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

