// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.util;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.lang.ref.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>SoftOutputStream</code> provides a memory-sensitive
* cache for data. The data is stored as weak references to
* (large) strings, so that the garbage collector can remove
* older data as necessary.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Sep 17 11:32:02 EDT 2003
*/
public class SoftOutputStream extends OutputStream
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Collection      strings;    // Collection< SoftReference(String) >
    byte[]          buffer;
    int             bufcount;
    final int       bufsize;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Creates a stream that stores data in 64 kB chunks. */
    public SoftOutputStream()
    { this(64); }
    
    /** Creates a stream that stores data in sizeKB*1024 byte chunks. */
    public SoftOutputStream(int sizeKB)
    {
        super();
        bufsize     = sizeKB*1024;
        bufcount    = 0;
        buffer      = new byte[bufsize];
        strings     = new ArrayList();
    }
//}}}

//{{{ close, flush
//##############################################################################
    /** Does nothing. */
    public void close()
    {}
    
    /** Flushes the current buffer contents to a String. */
    public synchronized void flush()
    {
        if(bufcount > 0)
        {
            String s = new String(buffer, 0, bufcount);
            strings.add( new SoftReference(s) );
            bufcount = 0;
        }
    }
//}}}

//{{{ write
//##############################################################################
    public synchronized void write(int b)
    {
        if(bufcount == bufsize) flush();
        buffer[bufcount++] = (byte)b;
    }
    
    public synchronized void write(byte[] b)
    { write(b, 0, b.length); }
    
    public synchronized void write(byte[] b, int off, int len)
    {
        while(len > 0)
        {
            int toWrite = Math.min(len, bufsize - bufcount);
            System.arraycopy(b, off, buffer, bufcount, toWrite);
            off         += toWrite;
            len         -= toWrite;
            bufcount    += toWrite;
            if(bufcount == bufsize) flush();
        }
    }
//}}}

//{{{ getString, clear
//##############################################################################
    /** Returns the contents of this stream as a string. */
    public synchronized String getString()
    {
        StringBuffer    sb = new StringBuffer();
        SoftReference   ref;
        String          s;
        for(Iterator iter = strings.iterator(); iter.hasNext(); )
        {
            ref = (SoftReference)iter.next();
            s   = (String)ref.get();
            if(s == null) s = "\n[*** Content lost due to low memory ***]\n";
            sb.append(s);
        }
        s = new String(buffer, 0, bufcount);
        sb.append(s);
        return sb.toString();
    }
    
    /** Remove all cached contents. */
    public synchronized void clear()
    {
        strings.clear();
        bufcount = 0;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

