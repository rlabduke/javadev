// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package util.isosurface;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>DSN6VertexSource</code> manages vertex information built
* from a binary format "O" / DSN6 map file.
*
* DSN6 files have a 512 byte header of 256 signed, 2-byte integers
* (i.e. Java shorts), most of which are unassigned.
* The data is structured such that there are two nested levels of
* grid, with Z slow, Y medium, and X fast. The large grid blocks
* are 512 byte bricks of 8x8x8 unsigned byte values.
*
* (??? IS THIS TRUE ???)
* Actually, large grid blocks "at the ends" may be less than 8x8x8;
* they could be, e.g., 4x7x5. These are not padded with zeros to
* fill out an 8x8x8 cube but instead are written out sequentially,
* taking up less than 512 bytes.
* (??? IS THIS TRUE ???)
*
* <p>The translation from byte values to density values is by this equation:
* <br>byte_val = prod*density_val + plus
* <p>Equivalently,
* <br>density_val = (byte_val - plus) / prod
* <p>"Plus" and "prod" are scaling factors derived from the header block.
*
* No source on the web is completely correct, but PARTIAL details available at
* <ul>
* <li>http://www.uoxray.uoregon.edu/tnt/manual/node104.html</li>
* <li>http://zombie.imsb.au.dk/~mok/brix/brix-1.html</li>
* <li>http://www.imsb.au.dk/~mok/o/o_man/node305.html#SECTION001020000000000000000</li>
* </ul>
*
* =========================================================
*     THIS CLASS STILL DOES NOT FUNCTION CORRECTLY!!!
* =========================================================
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb 10 14:34:41 EST 2003
*/
public class DSN6VertexSource extends CrystalVertexSource
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    DataInputStream     in;                     // the source of data
    byte[]              data            = null; // holds the data we read in from the file
    double              prod, plus;             // scale factors used by DSN6 format
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new VertexSource based on a crystallographic map file.
    * All data will be read in.
    * @param in the source of map data
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    public DSN6VertexSource(InputStream in) throws IOException
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
    public DSN6VertexSource(InputStream in, boolean readData) throws IOException
    {
        this.in = new DataInputStream(new BufferedInputStream(in));
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
        int i;
        int[] header = new int[256];
        for(i = 0; i < header.length; i++) header[i] = in.readShort();

        aMin = header[0];
        bMin = header[1];
        cMin = header[2];
        
        aCount = header[3];
        bCount = header[4];
        cCount = header[5];
        
        aMax = aMin + aCount - 1;
        bMax = bMin + bCount - 1;
        cMax = cMin + cCount - 1;
        
        aSteps = header[6];
        bSteps = header[7];
        cSteps = header[8];
        
        // scale factors used by DSN6 format
        double s16, s17, s18, s19;
        s16 = header[15];
        s17 = header[16];
        s18 = header[17];
        s19 = header[18];
        prod = s16 / s19;
        plus = s17;
        
        aLength = header[9]  / s18;
        bLength = header[10] / s18;
        cLength = header[11] / s18;
        alpha   = header[12] / s18;
        beta    = header[13] / s18;
        gamma   = header[14] / s18;
    }
//}}}

//{{{ readData (old)
//##################################################################################################
    /**
    * Decodes the body of a map file (i.e. density values for all grid points) and stores it in
    * memory. Assumes readHeader has already been called.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    * /
    void readData() throws IOException
    {
        int ii, jj, kk, i, j, k;
        
        // Allocate memory for the table...
        data = new byte[ aCount*bCount*cCount ];
        
        // X fast, Y medium, Z slow
        for(kk = 0; kk < cCount; kk += 8)
        {
            for(jj = 0; jj < bCount; jj += 8)
            {
                for(ii = 0; ii < aCount; ii += 8)
                {
                    for(k = 0; k < 8; k++)
                    {
                        for(j = 0; j < 8; j++)
                        {
                            for(i = 0; i < 8; i++)
                            {
                                setValue(ii+i, jj+j, kk+k, in.read());
                            }
                        }
                    }
                }
            }
        }
    }*/
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
        int ii, jj, kk;
        
        // Allocate memory for the table...
        data = new byte[ aCount*bCount*cCount ];
        
        // X fast, Y medium, Z slow
        for(kk = 0; kk < cCount; kk += 8)
        {
            for(jj = 0; jj < bCount; jj += 8)
            {
                for(ii = 0; ii < aCount; ii += 8)
                {
                    readBrick(ii, jj, kk);
                }
            }
        }
    }
//}}}
    
//{{{ readBrick
//##################################################################################################
    /**
    * Decodes one brick from a DSN6 file. Bricks must be decoded in sequence; no random access is done.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    void readBrick(int ii, int jj, int kk) throws IOException
    {
        int i, j, k, end_i, end_j, end_k;
        end_i = Math.min(8, aCount - ii);
        end_j = Math.min(8, bCount - jj);
        end_k = Math.min(8, cCount - kk);
        int bytesRead = 0;
        
        // X fast, Y medium, Z slow
        for(k = 0; k < end_k; k++)
        {
            for(j = 0; j < end_j; j++)
            {
                for(i = 0; i < end_i; i++)
                {
                    setValue(ii+i, jj+j, kk+k, in.read());
                    bytesRead++;
                }
            }
        }
        
        // Discard any remaining bytes in this brick
        for(i = bytesRead; i < 512; i++) in.read();
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

//{{{ calcMeanAndSigma
//##################################################################################################
    /**
    * Determines the mean and standard deviation for the data set.
    */
    void calcMeanAndSigma()
    {
        int i;
        double d;
        
        long m = 0;
        for(i = 0; i < data.length; i++) m += (data[i] & 0xff);
        mean =((double)m / (double)data.length - plus) / prod;
        
        sigma = 0.0;
        for(i = 0; i < data.length; i++)
        {
            d = mean - (((data[i] & 0xff) - plus) / prod);
            sigma += d*d;
        }
        sigma = Math.sqrt(sigma/data.length);
    }
//}}}
    
//{{{ getValue
//##################################################################################################
    /**
    * Returns the value at the specified grid point,
    * where the indexes i, j, and k have been adjusted
    * to start from 0 (i.e. i==0 means aMin, j==1 means bMin+1, etc.)
    */
    public double getValue(int i, int j, int k)
    {
        // & 0xff is the magic spell to treat a byte as unsigned
        int r = (data[ i + j*aCount + k*aCount*bCount ] & 0xff);
        return (r - plus) / prod;
    }
//}}}

//{{{ setValue
//##################################################################################################
    /**
    * Assigns the value at the specified grid point,
    * where the indexes i, j, and k have been adjusted
    * to start from 0 (i.e. i==0 means aMin, j==1 means bMin+1, etc.)
    *
    * If indices are out of range, this function has no effect.
    */
    void setValue(int i, int j, int k, int val) throws EOFException
    {
        if(val == -1) throw new EOFException("Premature end of file");
        else if(val < 0 || val > 255) System.err.println("Byte value out of range ("+val+")");
        else if(i<aCount && j<bCount && k<cCount) data[ i + j*aCount + k*aCount*bCount ] = (byte)val;
        else throw new IndexOutOfBoundsException("setValue out of bounds: i = "+i+"; j = "+j+"; k = "+k);
    }
//}}}

//{{{ toString
//##################################################################################################
    public String toString()
    {
        return super.toString()+"\nprod = "+prod+"; plus = "+plus;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

