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
* <code>OutputStreamTee</code> simply passes OutputStream commands
* on to two underlying streams, like the *nix utility 'tee'.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Sep 17 11:32:02 EDT 2003
*/
public class OutputStreamTee extends OutputStream
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    OutputStream stream1, stream2;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public OutputStreamTee(OutputStream s1, OutputStream s2)
    {
        super();
        if(s1 == null || s2 == null)
            throw new NullPointerException("Target streams may not be null");
        
        stream1 = s1;
        stream2 = s2;
    }
//}}}

//{{{ close, flush
//##############################################################################
    public void close() throws IOException
    {
        // Give both functions a chance, but throw ex from first
        // preferentially over ex from second.
        IOException ex = null;
        try { stream1.close(); } catch(IOException e) { ex = e; }
        try { stream2.close(); } catch(IOException e)
        {
            if(ex != null) throw ex;
            else throw e;
        }
        if(ex != null) throw ex;
    }

    public void flush() throws IOException
    {
        // Give both functions a chance, but throw ex from first
        // preferentially over ex from second.
        IOException ex = null;
        try { stream1.flush(); } catch(IOException e) { ex = e; }
        try { stream2.flush(); } catch(IOException e)
        {
            if(ex != null) throw ex;
            else throw e;
        }
        if(ex != null) throw ex;
    }
//}}}

//{{{ write, write, write
//##############################################################################
    public void write(int b) throws IOException
    {
        // Give both functions a chance, but throw ex from first
        // preferentially over ex from second.
        IOException ex = null;
        try { stream1.write(b); } catch(IOException e) { ex = e; }
        try { stream2.write(b); } catch(IOException e)
        {
            if(ex != null) throw ex;
            else throw e;
        }
        if(ex != null) throw ex;
    }

    public void write(byte[] b) throws IOException
    {
        // Give both functions a chance, but throw ex from first
        // preferentially over ex from second.
        IOException ex = null;
        try { stream1.write(b); } catch(IOException e) { ex = e; }
        try { stream2.write(b); } catch(IOException e)
        {
            if(ex != null) throw ex;
            else throw e;
        }
        if(ex != null) throw ex;
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        // Give both functions a chance, but throw ex from first
        // preferentially over ex from second.
        IOException ex = null;
        try { stream1.write(b, off, len); } catch(IOException e) { ex = e; }
        try { stream2.write(b, off, len); } catch(IOException e)
        {
            if(ex != null) throw ex;
            else throw e;
        }
        if(ex != null) throw ex;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

