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
* <code>NDimTable_Dense</code> is an implementation of NDimTable that allocates
* storage space for EVERY possible cell in the table, even if most of them
* are zero. NDimTable_Sparse may be a better choice for very large tables when
* less than one quarter of the cells are non-zero.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jan 11 10:08:49 EST 2006
*/
public class NDimTable_Dense extends NDimTable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
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
    */
    public NDimTable_Dense(String name, int ndim, double[] min, double[] max, int[] bins, boolean[] wrap)
    {
        super(name, ndim, min, max, bins, wrap);

        // Calculate number of entries in lookupTable
        int n_entries = 1;
        for(int i = 0; i < nDim; i++) n_entries *= nBins[i];

        // Allocate table and initialize it
        lookupTable = new double[n_entries];
        zero();
    }
    
    /**
    * Creates a NDimTable_Dense without initializing any of its fields.
    * For use by functions that reconstruct a table from some stored form.
    */
    protected NDimTable_Dense()
    { super(); }
//}}}

//{{{ bin2index, bin2index_limit, index2bin
//##################################################################################################
    // Takes a set of bin numbers and produces a linear offset into lookuptable.
    // If no bin can be found after wrapping is applied, returns -1.
    int bin2index(int[] where)
    { return (int) bin2long(where); }

    // Takes a set of bin numbers and produces a linear offset into lookuptable.
    // If no bin can be found after wrapping is applied, the edge of the table is used.
    int bin2index_limit(int[] where)
    { return (int) bin2long_limit(where); }

    // Takes an index into lookuptable and regenerates a set of bin numbers
    // where[] is overwritten
    void index2bin(int which, int[] where)
    {
        for(int i = nDim-1; i >= 0; i--)
        {
            where[i] = which % nBins[i];
            which -= where[i];
            which /= nBins[i];
        }
    }
//}}}

//{{{ set/addValueAt, valueAt, valueAtLimit
//##################################################################################################
    /**
    * Sets the number of data points in a given bin.
    * You (probably) shouldn't ever be using this.
    *
    * @param where the bin to affect
    * @param value the value to place in the bin
    */
    public void setValueAt(int[] where, double value)
    { lookupTable[bin2index(where)] = value; }

    /**
    * Adds to the number of data points in a given bin.
    * You (probably) shouldn't ever be using this.
    *
    * @param where the bin to affect
    * @param value the value to add to the bin
    */
    public void addValueAt(int[] where, double value)
    //{ lookupTable[bin2index(where)] += value; }
    {
        try { lookupTable[bin2index(where)] += value; }
        catch(IndexOutOfBoundsException ex)
        {
            System.err.print("IOOBEx:");
            for(int i = 0; i < where.length; i++) System.err.print(" "+where[i]+"/"+wrapbin(where[i], i));
            System.err.print(" -> "+bin2index(where));
            System.err.println("\t\tcontains(where) -> "+contains(where));
            //ex.printStackTrace();
        }
    }

    /**
    * Retrieves the number of data points in a given bin.
    * If no bin can be found after wrapping is applied, an exception is thrown.
    *
    * @param where the bin to look in
    * @return the number of data points in the bin
    * @throws IndexOutOfBoundsException if no such bin exists in the table
    */
    public double valueAt(int[] where)
    { return lookupTable[bin2index(where)]; }

    /**
    * Retrieves the number of data points in a given bin.
    * If no bin can be found after wrapping is applied, the edge of the table is used.
    *
    * @param where the bin to look in
    * @return the number of data points in the bin
    */
    public double valueAtLimit(int[] where)
    { return lookupTable[bin2index_limit(where)]; }
//}}}

//{{{ createFromNDFT
//##################################################################################################
    /**
    * Recreates an <i>n</i>-dimensional lookup table of floating point numbers from a file.
    * See <code>writeNDFT()</code> for details of the format.
    *
    * @param in The stream to read from
    *
    * @throws IOException if there is a problem reading from the stream
    */
    public static NDimTable createFromNDFT(DataInputStream in) throws IOException
    {
        NDimTable ndt = new NDimTable_Dense();
        
        // Extract our name
        StringBuffer sb = new StringBuffer();
        char c;
        while( (c = (char)in.readUnsignedByte()) != 0 ) sb.append(c);
        ndt.ourName = sb.toString();

        // How many dimensions?
        ndt.nDim = in.readInt();

        // Allocate storage for minVal, maxVal, nBins, doWrap
        ndt.minVal  = new   double[ndt.nDim];
        ndt.maxVal  = new   double[ndt.nDim];
        ndt.nBins   = new     int[ndt.nDim];
        ndt.doWrap  = new boolean[ndt.nDim];

        // Read them in
        int i;
        for(i = 0; i < ndt.nDim; i++) ndt.minVal[i] = in.readFloat();
        for(i = 0; i < ndt.nDim; i++) ndt.maxVal[i] = in.readFloat();
        for(i = 0; i < ndt.nDim; i++) ndt.nBins[i]  = in.readInt();
        for(i = 0; i < ndt.nDim; i++) ndt.doWrap[i] = in.readBoolean();

        // Get the number of real points
        ndt.realcount = in.readInt();

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

        // Calculate number of entries in lookupTable
        int n_entries = 1;
        for(i = 0; i < ndt.nDim; i++) n_entries *= ndt.nBins[i];

        // Allocate table and initialize it
        ndt.lookupTable = new double[n_entries];
        for(i = 0; i < ndt.lookupTable.length; i++) ndt.lookupTable[i] = in.readFloat();
        
        return ndt;
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
        NDimTable ndt = new NDimTable_Dense();
        
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
        int n_entries = 1;
        for(i = 0; i < ndt.nDim; i++) n_entries *= ndt.nBins[i];
        ndt.lookupTable = new double[n_entries];
        
        //{{{ Old code that requires all entries to be present, including zeros
        //// Determine whether values come first or last.
        //s = getLine(in);
        //if(s.indexOf("last") != -1) // value comes last
        //{
        //    for(i = 0; i < ndt.lookupTable.length; i++)
        //    {
        //        s = in.readLine();
        //        if(s == null) throw new EOFException("No lines remaining in "+in);
        //        int x = s.lastIndexOf(' ');
        //        if(x != -1) s = s.substring(x+1);
        //        ndt.lookupTable[i] = Double.parseDouble(s);
        //    }
        //}
        //else // value comes first (default)
        //{
        //    for(i = 0; i < ndt.lookupTable.length; i++)
        //    {
        //        s = in.readLine();
        //        if(s == null) throw new EOFException("No lines remaining in "+in);
        //        int x = s.indexOf(' ');
        //        if(x != -1) s = s.substring(0, x);
        //        ndt.lookupTable[i] = Double.parseDouble(s);
        //    }
        //}
        //}}} Old code that requires all entries to be present, including zeros
        
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
    * Saves the contents of the table to a file for re-use later.
    *
    * First, the name is written out as a C-style string (one byte per character, null terminated).
    * Then nDim is written, then all of minVal, all of maxVal, all of nBins, and all of doWrap. Next is realcount.
    * Finally, all of lookupTable is written out. In theory then, any other
    * application can also read and use this data. Note that high bytes are
    * written first. Some machines and platforms may need to take this into
    * account.
    *
    * This format is the original binary format used by NDFloatTable and as a result causes
    * a loss of precision as this class is downcast to the capabilities of NDFloatTable.
    *
    * @param out the stream to write to.
    *
    * @throws IOException if an IO error occurs.
    */
    public void writeNDFT(DataOutputStream out) throws IOException
    {
        out.writeBytes(ourName);                                                // length bytes
        out.writeByte(0);                                                       // 1 byte

        out.writeInt(nDim);                                                     // 4 bytes

        int i;
        for(i = 0; i < nDim; i++) out.writeFloat((float)minVal[i]);             // ndim*4 bytes, loss of precision
        for(i = 0; i < nDim; i++) out.writeFloat((float)maxVal[i]);             // ndim*4 bytes, loss of precision
        for(i = 0; i < nDim; i++) out.writeInt(nBins[i]);                       // ndim*4 bytes
        for(i = 0; i < nDim; i++) out.writeBoolean(doWrap[i]);                  // ndim*1 bytes

        out.writeInt((int)realcount);                                           // 4 bytes, loss of precision

        for(i = 0; i < lookupTable.length; i++) out.writeFloat((float)lookupTable[i]); // nvals*4 bytes, loss of precision
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

