// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.isosurface;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>OMapVertexSource</code> manages vertex information built
* from a binary format "O" map files.
* Older O maps are in the DSN6 format, which has a binary header
* and some byte swapping issues. Newer maps are in the Brix format,
* which is very similar, but has an ASCII header and the map data
* isn't byte-swapped. This class auto-detects which of the two
* formats is in use and reads either one. It can also detect and
* unzip map files that have been compressed with gzip.
*
* <!-- {{{ file format description -->
* <h3>DSN6 files</h3>
*
* DSN6 files have a 512 byte header of 256 signed, big-endian,
* 2-byte integers (i.e. Java shorts), most of which are unassigned.
* The data is structured such that there are two nested levels of
* grid, with Z slow, Y medium, and X fast. The large grid blocks
* are 512 byte bricks of 8x8x8 unsigned byte values.
*
* <p>Every brick is 8x8x8, even the ones "at the ends" that have less
* than 8x8x8 of data. For example a brick that has 4x7x5 in data
* is padded out to 8x8x8, with meaningless data in the other 4 samples
* along x, the other 1 along y, and the other 3 along z. It would
* make sense to set these to zero, but as far as I can tell, this is
* not done by most programs.
*
* <p>The catch: every PAIR of bytes in the density bricks is transposed,
* so the x indices are NOT 0, 1, 2, 3, 4, 5, 6, 7; but instead are
* 1, 0, 3, 2, 5, 4, 7, 6. This is due to the same endian-ism issue that
* affects the header. If I ever get a hold of the sick bastard
* who decided this was a good format... !
*
* <p>The translation from byte values to density values is by this equation:
* byte_val = prod*density_val + plus
*
* <p>Equivalently,
* density_val = (byte_val - plus) / prod
*
* <p>"Plus" and "prod" are scaling factors derived from the header block.
*
*
* <h3>BRIX files</h3>
*
* Brix files have a 512-byte ASCII header that begins with a smiley: ":-)"
* (without the quote marks). This is then followed by self-descriptive
* data for the unit cell parameters, prod and plus, etc. The end of useful
* data *should* be marked with a formfeed character, but this isn't reliable.
* The remainder of the 512 bytes may be padded with spaces (but I don't think it's required).
*
* The data is stored in 512 byte bricks, just as for DSN6 files. However,
* bytes are written out in the order that a normal human would have choosen,
* i.e., not transposed.
*
*
* <h3>References</h3>
* No source on the web is completely correct, but PARTIAL details are available at
* <ul>
* <li>http://www.uoxray.uoregon.edu/tnt/manual/node104.html</li>
* <li>http://zombie.imsb.au.dk/~mok/brix/brix-1.html</li>
* <li>http://www.imsb.au.dk/~mok/o/o_man/node305.html#SECTION001020000000000000000</li>
* <li>The getcube() method in sftools.f from the CCP4 project. This is how I figured out the
*   byte-swapping issue, finally.</li>
* </ul>
* <!-- }}} file format description -->
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb 10 14:34:41 EST 2003
*/
public class OMapVertexSource extends CrystalVertexSource
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    DataInputStream     in;                     // the source of data
    byte[]              data            = null; // holds the data we read in from the file
    double              prod, plus;             // scale factors used by the format
    boolean             bytesTransposed;
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
    public OMapVertexSource(InputStream in) throws IOException
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
    public OMapVertexSource(InputStream in, boolean readData) throws IOException
    {
        // Auto-detect a gzipped input stream
        in = new BufferedInputStream(in);
        in.mark(10);
        if(in.read() == 31 && in.read() == 139)
        {
            // We've found the gzip magic numbers...
            in.reset();
            // We have to double buffer this because of a bug
            // in GZIPInputStream.mark()
            in = new BufferedInputStream(new java.util.zip.GZIPInputStream(in));
        }
        else in.reset();
        
        // Check for Brix format
        in.mark(10);
        bytesTransposed = !(in.read() == ':' && in.read() == '-' && in.read() == ')' && in.read() == ' ');
        in.reset();
        
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
        int i;
        String s;
        
        if(bytesTransposed) // DSN6
        {
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
        else // BRIX
        {
            try
            {
                char[] header = new char[512];
                for(i = 0; i < header.length; i++) header[i] = (char)in.read();
                for(i = 0; i < header.length && header[i] != '\f'; i++) {}
                StringTokenizer tok = new StringTokenizer(new String(header, 0, i));
                
                if(! tok.nextToken().equals(":-)")) throw new IllegalArgumentException("Missing smiley");
                if(! tok.nextToken().equalsIgnoreCase("origin")) throw new IllegalArgumentException("Parameters out of order");
                    aMin = Integer.parseInt(tok.nextToken());
                    bMin = Integer.parseInt(tok.nextToken());
                    cMin = Integer.parseInt(tok.nextToken());
                if(! tok.nextToken().equalsIgnoreCase("extent")) throw new IllegalArgumentException("Parameters out of order");
                    aCount = Integer.parseInt(tok.nextToken());
                    bCount = Integer.parseInt(tok.nextToken());
                    cCount = Integer.parseInt(tok.nextToken());
                    aMax = aMin + aCount - 1;
                    bMax = bMin + bCount - 1;
                    cMax = cMin + cCount - 1;
                if(! tok.nextToken().equalsIgnoreCase("grid")) throw new IllegalArgumentException("Parameters out of order");
                    aSteps = Integer.parseInt(tok.nextToken());
                    bSteps = Integer.parseInt(tok.nextToken());
                    cSteps = Integer.parseInt(tok.nextToken());
                if(! tok.nextToken().equalsIgnoreCase("cell")) throw new IllegalArgumentException("Parameters out of order");
                    aLength = Double.parseDouble(tok.nextToken());
                    bLength = Double.parseDouble(tok.nextToken());
                    cLength = Double.parseDouble(tok.nextToken());
                    alpha   = Double.parseDouble(tok.nextToken());
                    beta    = Double.parseDouble(tok.nextToken());
                    gamma   = Double.parseDouble(tok.nextToken());
                if(! tok.nextToken().equalsIgnoreCase("prod")) throw new IllegalArgumentException("Parameters out of order");
                    prod    = Double.parseDouble(tok.nextToken());
                if(! tok.nextToken().equalsIgnoreCase("plus")) throw new IllegalArgumentException("Parameters out of order");
                    plus    = Double.parseDouble(tok.nextToken());
                if(! tok.nextToken().equalsIgnoreCase("sigma")) throw new IllegalArgumentException("Parameters out of order");
                    sigma   = Double.parseDouble(tok.nextToken());
                    mean    = 0; // always has mean of 0?
            }
            catch(NumberFormatException ex)
            { throw new IllegalArgumentException("Bad number: "+ex.getMessage()); }
        }
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
        int ii, jj, kk;
        
        try
        {
            // Allocate memory for the table...
            data = new byte[ aCount*bCount*cCount ];
            
            if(bytesTransposed) // DSN6
            {
                // X fast, Y medium, Z slow
                for(kk = 0; kk < cCount; kk += 8)
                {
                    for(jj = 0; jj < bCount; jj += 8)
                    {
                        for(ii = 0; ii < aCount; ii += 8)
                        {
                            readBrickSwapped(ii, jj, kk);
                        }
                    }
                }
            }
            else // BRIX
            {
                // X fast, Y medium, Z slow
                for(kk = 0; kk < cCount; kk += 8)
                {
                    for(jj = 0; jj < bCount; jj += 8)
                    {
                        for(ii = 0; ii < aCount; ii += 8)
                        {
                            readBrickNotSwapped(ii, jj, kk);
                        }
                    }
                }
            }
        }
        catch(OutOfMemoryError er)
        {
            throw new IllegalArgumentException("Not enough memory: probably not a DSN6 map");
        }
        
        // Take our best guess at the mean and sigma
        double oldMean  = mean;
        double oldSigma = sigma;
        calcMeanAndSigma();
        SoftLog.err.println("calculated  : mean = "+mean+"; sigma = "+sigma);
        // We don't divide by mean b/c oldMean == 0
        if(Math.abs(mean-oldMean) < 0.05 && Math.abs((sigma-oldSigma)/sigma) < 0.05)
        {
            mean    = 0;
            sigma   = oldSigma;
            SoftLog.err.println("close enough: mean = "+mean+"; sigma = "+sigma);
        }
    }
//}}}
    
//{{{ readBrickSwapped
//##################################################################################################
    /**
    * Decodes one brick from a DSN6 file. Bricks must be decoded in sequence; no random access is done.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    void readBrickSwapped(int ii, int jj, int kk) throws IOException
    {
        int i, j, k;
        
        // X fast, Y medium, Z slow
        for(k = 0; k < 8; k++)
        {
            for(j = 0; j < 8; j++)
            {
                for(i = 0; i < 8; i += 2)
                {
                    // Every pair of bytes is swapped!
                    setValue(ii+i+1, jj+j, kk+k, in.read());
                    setValue(ii+i,   jj+j, kk+k, in.read());
                }
            }
        }
    }
//}}}
    
//{{{ readBrickNotSwapped
//##################################################################################################
    /**
    * Decodes one brick from a DSN6 file. Bricks must be decoded in sequence; no random access is done.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    void readBrickNotSwapped(int ii, int jj, int kk) throws IOException
    {
        int i, j, k;
        
        // X fast, Y medium, Z slow
        for(k = 0; k < 8; k++)
        {
            for(j = 0; j < 8; j++)
            {
                for(i = 0; i < 8; i++) setValue(ii+i,   jj+j, kk+k, in.read());
            }
        }
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
        //else if(val < 0 || val > 255) SoftLog.err.println("Byte value out of range ("+val+")");
        else if(i<aCount && j<bCount && k<cCount) data[ i + j*aCount + k*aCount*bCount ] = (byte)val;
        //else nothing -- some bytes are meaningless in O files
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

