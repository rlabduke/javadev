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
* <code>URLEncodedOutputStream</code> is an output stream where all bytes written
* are URL-encoded according to the description for java.net.URLEncoder.
* I would use that class directly, but Java's urlencode methods expect only Strings.
* Everything gets translated into UTF-8, which breaks binary files.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Sep 27 16:24:54 EDT 2004
*/
public class URLEncodedOutputStream extends OutputStream
{
//{{{ Constants
    static final String HEX_CHARS = "0123456789ABCDEF";
//}}}

//{{{ Variable definitions
//##############################################################################
    OutputStream out;
    boolean doEncoding = true;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public URLEncodedOutputStream(OutputStream out)
    {
        super();
        this.out = out;
    }
//}}}

//{{{ write, flush, close
//##############################################################################
    public void write(int b) throws IOException
    {
        if(!doEncoding)
            out.write(b);
        else if(('a' <= b && b <= 'z')
        || ('A' <= b && b <= 'Z')
        || ('0' <= b && b <= '9')
        || b == '.' || b == '-' || b == '*' || b == '_')
            out.write(b);
        else if(b == ' ')
            out.write('+');
        else
        {
            out.write('%');
            out.write(HEX_CHARS.charAt( (b>>4) & 0x0F ));
            out.write(HEX_CHARS.charAt( (b   ) & 0x0F ));
        }
    }
    
    public void flush() throws IOException
    { out.flush(); }
    
    public void close() throws IOException
    { out.close(); }
//}}}

//{{{ getEncoding, setEncoding
//##############################################################################
    /** Whether or not URL encoding is being applied. */
    public boolean getEncoding()
    { return doEncoding; }
    
    /** Sets whether data should be encoded or not. Encoding is enabled by default. */
    public void setEncoding(boolean b)
    { doEncoding = b; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

