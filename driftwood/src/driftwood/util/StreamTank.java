// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.util;

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
* <code>StreamTank</code> is a holding tank for stream data,
* collecting bytes in an array an later feeding them back out.
* This class exists for handling large quantities of data,
* for which duplicating the byte buffer (as is required to use
* ByteArrayOutputStream and ByteArrayInputStream out of the box)
* is unacceptable in terms of size and/or speed.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 16 18:51:18 EDT 2003
*/
public class StreamTank extends ByteArrayOutputStream
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    boolean isClosed = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public StreamTank()
    {
        super();
    }

    /**
    * Constructor
    */
    public StreamTank(int size)
    {
        super(size);
    }
//}}}

//{{{ close, reset
//##################################################################################################
    /**
    * Unlike its superclass, this stream will throw an exception
    * if write operations are attempted after it has been closed.
    * Due to the rules of inheritance, however, it can't throw
    * a checked exception, so we opt for IllegalStateException instead.
    */
    public void close()
    {
        // The superclass doesn't DO anything here,
        // so it can't throw an exception...
        try { super.close(); }
        catch(IOException ex) {}
        isClosed = true;
    }
    
    /** Throws IllegalStateException if the stream has already been closed. */
    public void reset()
    {
        if(isClosed)
            throw new IllegalStateException("Can't reset a closed stream");
        super.reset();
    }
//}}}

//{{{ write
//##################################################################################################
    /** Throws IllegalStateException if the stream has already been closed. */
    public void write(byte[] b, int off, int len)
    {
        if(isClosed)
            throw new IllegalStateException("Can't write to a closed stream");
        super.write(b, off, len);
    }

    /** Throws IllegalStateException if the stream has already been closed. */
    public void write(int b)
    {
        if(isClosed)
            throw new IllegalStateException("Can't write to a closed stream");
        super.write(b);
    }
//}}}

//{{{ getInputStream
//##################################################################################################
    /**
    * Returns an input stream that can be used to retreive bytes
    * that were fed into this output stream.
    * This function may be called multiple times and will not
    * result in a duplication of the internal buffer.
    * However, this tank MUST be closed before calling this
    * function, or an IllegalStateException will result.
    */
    public ByteArrayInputStream getInputStream()
    {
        if(!isClosed)
            throw new IllegalStateException("Must close output stream before attempting to read input");
        return new ByteArrayInputStream(buf, 0, this.size());
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

