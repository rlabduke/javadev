// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.isosurface;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>Ccp4VertexSource</code> manages vertex information built
* from electron density maps in CCP4's native format.
* The class only supports type 2 [image, real (floating-point) numbers] maps.
* It also ignores skew operations, symmetry operators, and all comments.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jul 17 10:41:33 EDT 2003
*/
public class Ccp4VertexSource extends CrystalVertexSource
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    DataInputStream     in;                     // the source of data
    float[]             data            = null; // holds the data we read in from the file
    boolean             littleEndian    = false;
    
    /**
    * A mapping from CCP4's idea of columns (fast), rows, and sections (slow)
    * to the x, y, and z axes. (Actually, the a, b, and c axes.)
    * Array keys are 0, 1, or 2 for X, Y, or Z (A, B, or C).
    * Array values are 0, 1, or 2 for columns, rows, or sections.
    */
    int[]               idx             = new int[3];
    
    /** The inverse of idx */
    int[]               crs             = new int[3];
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a new VertexSource based on a crystallographic map file.
    * All data will be read in.
    * @param in the source of map data
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    public Ccp4VertexSource(InputStream in) throws IOException
    {
        this(in, true);
    }
    
    /**
    * Creates a new VertexSource based on a crystallographic map file.
    * @param in the source of map data
    * @param readData if false, only header info will be read
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    public Ccp4VertexSource(InputStream in, boolean readData) throws IOException
    {
        // Auto-detect a gzipped input stream
        in = new BufferedInputStream(in);
        in.mark(10);
        if(in.read() == 31 && in.read() == 139)
        {
            // We've found the gzip magic numbers...
            in.reset();
            in = new java.util.zip.GZIPInputStream(in);
        }
        else in.reset();
        
        // Now cast it as a DataInputStream
        this.in = new DataInputStream(in);
        super.init(readData);
    }
//}}}

//{{{ readHeader
//##################################################################################################
    /**
    * Decodes information from the map header.
    * In particular, this method should fill in all the unit cell parameters.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    void readHeader() throws IOException
    {
        byte[] header = new byte[1024];
        in.readFully(header);
        
        // This must be set before getInt() is called!!
        littleEndian = (header[0] != 0);
        
        if(getInt(header, 4) != 2)
            throw new IllegalArgumentException("Bad map data type: only mode 2 is supported");
        if(header[208] != 'M' || header[209] != 'A'
        || header[210] != 'P' || header[211] != ' ')
            throw new IllegalArgumentException("Bad map format: 'MAP ' missing at word #53");
        
        // See variable definitions for an explanation
        crs[0] = getInt(header, 17)-1;
        crs[1] = getInt(header, 18)-1;
        crs[2] = getInt(header, 19)-1;
        idx[ crs[0] ] = 0;
        idx[ crs[1] ] = 1;
        idx[ crs[2] ] = 2;
        
        // Dimensions in terms of indexes
        aCount      = getInt(header, 1+idx[0]);
        aMin        = getInt(header, 5+idx[0]);
        aMax        = aMin + aCount - 1;
        aSteps      = getInt(header, 8);
        bCount      = getInt(header, 1+idx[1]);
        bMin        = getInt(header, 5+idx[1]);
        bMax        = bMin + bCount - 1;
        bSteps      = getInt(header, 9);
        cCount      = getInt(header, 1+idx[2]);
        cMin        = getInt(header, 5+idx[2]);
        cMax        = cMin + cCount - 1;
        cSteps      = getInt(header, 10);
        
        // Unit cell dimensions
        aLength     = Float.intBitsToFloat( getInt(header, 11) );
        bLength     = Float.intBitsToFloat( getInt(header, 12) );
        cLength     = Float.intBitsToFloat( getInt(header, 13) );
        alpha       = Float.intBitsToFloat( getInt(header, 14) );
        beta        = Float.intBitsToFloat( getInt(header, 15) );
        gamma       = Float.intBitsToFloat( getInt(header, 16) );
        
        // Map statistics
        mean        = Float.intBitsToFloat( getInt(header, 22) );
        sigma       = Float.intBitsToFloat( getInt(header, 55) );
        
        // Skip all symmetry records
        int symBytes = getInt(header, 24);
        while(symBytes > 0) symBytes -= in.skip(symBytes);
            
    }
//}}}

//{{{ getInt
//##############################################################################
    /**
    * Returns an int constructed from 4 bytes.
    * @param bytes  the source of data
    * @param index  which integer to use; numbering starts at 1
    */
    protected int getInt(byte[] bytes, int index)
    {
        index = 4*(index-1);
        if(littleEndian)
        {
            return (((bytes[index+3] & 0xFF) << 24) |
                    ((bytes[index+2] & 0xFF) << 16) |
                    ((bytes[index+1] & 0xFF) <<  8) |
                    ((bytes[ index ] & 0xFF)));
        }
        else
        {
            return (((bytes[ index ] & 0xFF) << 24) |
                    ((bytes[index+1] & 0xFF) << 16) |
                    ((bytes[index+2] & 0xFF) <<  8) |
                    ((bytes[index+3] & 0xFF)));
        }
    }
    
    /** Flips the byte order if needed */
    protected int getInt(int i)
    {
        if(littleEndian)
        {
            return (((i & 0x000000FF) <<  24) |
                    ((i & 0x0000FF00) <<   8) |
                    ((i & 0x00FF0000) >>>  8) |
                    ((i & 0xFF000000) >>> 24));
        }
        else return i;
    }
//}}}

//{{{ readData
//##################################################################################################
    /**
    * Decodes the body of a map file (i.e. density values for all grid points) and stores it in
    * memory. Assumes readHeader has already been called.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    void readData() throws IOException
    {
        // Allocate memory for the table...
        data = new float[ aCount*bCount*cCount ];
        
        int[] cnt   = new int[3]; // col, row, seg
        int[] limit = new int[3];
        limit[ idx[0] ] = aCount;
        limit[ idx[1] ] = bCount;
        limit[ idx[2] ] = cCount;
        
        for(cnt[2] = 0; cnt[2] < limit[2]; cnt[2]++)
        {
            for(cnt[1] = 0; cnt[1] < limit[1]; cnt[1]++)
            {
                for(cnt[0] = 0; cnt[0] < limit[0]; cnt[0]++)
                {
                    float value = Float.intBitsToFloat(getInt(in.readInt()));
                    setValue( cnt[idx[0]], cnt[idx[1]], cnt[idx[2]], value);
                }
            }
        }
        
        // For consistency with OMap and Xplor
        SoftLog.err.println("map header  : mean = "+mean+"; sigma = "+sigma);
    }
//}}}
    
//{{{ hasData
//##################################################################################################
    /**
    * Returns true iff density data was read in at the time of creation.
    */
    public boolean hasData()
    {
        return (data != null);
    }
//}}}

//{{{ get/setValue
//##################################################################################################
    /**
    * Returns the value at the specified grid point,
    * where the indexes i, j, and k have been adjusted
    * to start from 0 (i.e. i==0 means aMin, j==1 means bMin+1, etc.)
    */
    public double getValue(int i, int j, int k)
    {
        return (double)data[ i + j*aCount + k*aCount*bCount ];
    }
    
    /**
    * Sets the value at the specified grid point,
    * where the indexes i, j, and k have been adjusted
    * to start from 0 (i.e. i==0 means aMin, j==1 means bMin+1, etc.)
    */
    protected void setValue(int i, int j, int k, double d)
    {
        data[ i + j*aCount + k*aCount*bCount ] = (float)d;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

