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
* <code>XplorVertexSource</code> manages vertex information built
* from an ASCII format X-PLOR/CNS map file. Details available
* in the X-PLOR manual, v 3.1 at
* <a href="http://www.ocms.ox.ac.uk/mirrored/xplor/manual/htmlman/htmlman.html">
* http://www.ocms.ox.ac.uk/mirrored/xplor/manual/htmlman/htmlman.html</a>.
* This class can auto-detect a gzipped X-PLOR file and unzip it
* internally on the fly.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb 10 14:34:41 EST 2003
*/
public class XplorVertexSource extends CrystalVertexSource
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    LineNumberReader    in;                         // the source of data

    StringBuffer        comments        = new StringBuffer();
    float[]             data            = null; // holds the data we read in from the file

    // Used by nextValue():
    float[]             nvQueue         = new float[6];
    int                 nvCount         = 0;
    int                 nvFront         = 0;
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
    public XplorVertexSource(InputStream in) throws IOException
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
    public XplorVertexSource(InputStream in, boolean readData) throws IOException
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
        
        // Now cast it as a Reader (double buffered now, but that's OK)
        this.in = new LineNumberReader(new InputStreamReader(in));
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
        try
        {
            String s;
            StringTokenizer tok;
            
            // Deal with leading comments
            while((s = in.readLine().trim()).equals("")); // skip blank lines
            tok = new StringTokenizer(s);
            int nComments = Integer.parseInt(tok.nextToken());
            for(int i = 0; i < nComments; i++) comments.append(in.readLine().trim());
                
            // Dimensions in terms of indexes
            tok = new StringTokenizer(in.readLine());
            aSteps      = Integer.parseInt(tok.nextToken());
            aMin        = Integer.parseInt(tok.nextToken());
            aMax        = Integer.parseInt(tok.nextToken());
            aCount      = aMax - aMin + 1;
            bSteps      = Integer.parseInt(tok.nextToken());
            bMin        = Integer.parseInt(tok.nextToken());
            bMax        = Integer.parseInt(tok.nextToken());
            bCount      = bMax - bMin + 1;
            cSteps      = Integer.parseInt(tok.nextToken());
            cMin        = Integer.parseInt(tok.nextToken());
            cMax        = Integer.parseInt(tok.nextToken());
            cCount      = cMax - cMin + 1;
            
            // Unit cell dimensions
            aLength     = nextValue();
            bLength     = nextValue();
            cLength     = nextValue();
            alpha       = nextValue();
            beta        = nextValue();
            gamma       = nextValue();
            
            // This should read 'ZYX'
            if(!in.readLine().trim().equalsIgnoreCase("ZYX"))
                throw new IllegalArgumentException("Bad format -- not a ZYX file");
        }
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("Bad number: "+ex.getMessage()); }
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
        try
        {
            int i;
            
            // Allocate memory for the table...
            data = new float[ aCount*bCount*cCount ];
            
            for(i = 0; i < data.length; i++)
            {
                data[i] = nextValue();
            }
        }
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("Bad number: "+ex.getMessage()); }
        
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
    
//{{{ nextValue
//##################################################################################################
    /** Reads the next ASCII formatted value from the regularly spaced section of the X-PLOR file. */
    float nextValue() throws IOException, NumberFormatException
    {
        // refill
        if(nvCount <= nvFront)
        {
            // Skip ksect lines
            String s;
            while(true)
            {
                s = in.readLine();
                if(s == null) throw new EOFException("Unexpected EOF in X-PLOR file at line "+in.getLineNumber());
                else if(s.charAt(2) != '.' || s.length() < 12) {}//SoftLog.err.println("Skipping ksect line: '"+s+"'");
                else break;
            }
            
            // read up to 6 values from this line
            for(nvCount = nvFront = 0; nvCount < 6 && 12*(nvCount+1) <= s.length(); nvCount++)
            {
                nvQueue[nvCount] = Float.parseFloat(s.substring(12*nvCount, 12*(nvCount+1)));
            }
            
            // if we still don't have any numbers, throw an exception
            if(nvCount <= nvFront) throw new IOException("Can't read any more numbers from X-PLOR file (line "+in.getLineNumber()+")");
        }
        
        return nvQueue[nvFront++];
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

        mean = 0.0;
        for(i = 0; i < data.length; i++)
        {
            mean += data[i];
        }
        mean /= data.length;
        
        sigma = 0.0;
        for(i = 0; i < data.length; i++)
        {
            d = mean - data[i];
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
        return (double)data[ i + j*aCount + k*aCount*bCount ];
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

