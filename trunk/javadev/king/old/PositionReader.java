// (jEdit options) :folding=explicit:collapseFolds=1:
package king;
//import java.awt.*;
import java.io.*;
//import java.text.*;
//import java.util.*;
//import javax.swing.*;

/**
* <code>PositionReader</code> is a BufferedReader that tracks number of lines and characters read.
* See LineNumberReader for appropriate documentation.
* May or may not work appropriately with readLine(), depending on internal implementation!
* In fact, may or may not work correctly in many cases, but works fine using only read() !!
* Test other functions before using in production work.
*
* <p>Begun on Sat Jun  8 15:37:12 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class PositionReader extends LineNumberReader // implements ...
{
//{{{ Variable definitions
//##################################################################################################
    long nChars = 0;
    long markChars = 0;
//}}}

//{{{ Constructors
//##################################################################################################
    public PositionReader(Reader in, int sz) { super(in, sz); }
    public PositionReader(Reader in) { super(in); }
//}}}

//{{{ Functions
//##################################################################################################
    public void mark(int readAheadLimit) throws IOException
    {
        super.mark(readAheadLimit);
        markChars = nChars;
    }
    
    public void reset() throws IOException
    {
        super.reset();
        nChars = markChars;
    }
    
    public int read() throws IOException
    {
        int r = super.read();
        if(r != -1) nChars++;
        return r;
    }
    
    public int read(char[] cbuf, int off, int len) throws IOException
    {
        int c = super.read(cbuf, off, len);
        if(c != -1) nChars += c;
        return c;
    }
    
    public long skip(long n) throws IOException
    {
        long c = super.skip(n);
        nChars += c;
        return c;
    }
    
    public long getPosition() { return nChars; }
    
    public void setPosition(long n) { nChars = n; }
//}}}

//{{{ Utility/debugging functions
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}

}//class
