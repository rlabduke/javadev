// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.parser;

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
* <code>CharWindow</code> represents a Reader as a CharSequence, such that some
* window of characters is currently accessible.
* If the EOF is within the buffer, the length will be correct;
* otherwise, length() will return the buffer length.
*
* <p>The purpose of this class is to allow matching regular expressions
* against an input stream, for the purpose of parsing a file.
* Obviously, the size of the buffer limits the length of pattern that can be matched.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu May  3 10:57:30 EDT 2007
*/
public class CharWindow implements CharSequence
{
//{{{ Constants
    /** Kilobyte, for specifying buffer sizes */
    public static final int KILOBYTE = 1<<10;
    
    /** Megabyte, for specifying buffer sizes */
    public static final int MEGABYTE = 1<<20;
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The source of characters */
    Reader  reader;
    /** How many characters have been read before the current charAt(0); can't be accessed any more. */
    long    prevChars   = 0;
    /** Number of line breaks preceding preceding charAt(0) */
    int     lineAtZero  = 0;
    /** Number of characters following the last line break but before charAt(0) */
    int     colAtZero   = 0;
    /** Number of usable characters in the buffer */
    int     dataLen     = 0;
    /** Power-of-two-minus-one bitmask used to wrap absolute character index (prevChars + charAtIndex) to a buffer index. */
    long    bufMask;
    /** Actual buffer of characters; reading and writing wrap around in a cycle. */
    char[]  buffer;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Default buffer size is 1k. */
    public CharWindow(Reader reader) throws IOException
    { this(reader, KILOBYTE); }
    
    public CharWindow(Reader reader, int minBufferSize) throws IOException
    {
        super();
        int bufLen;
        for(bufLen = 16; bufLen < minBufferSize; bufLen <<= 1);
        this.buffer     = new char[bufLen]; // a power of two
        this.bufMask    = bufLen - 1;       // lowest N bits set
        this.reader     = reader;
        
        advance(0); // fill the buffer initially
    }
    
    public CharWindow(InputStream in) throws IOException
    { this(new InputStreamReader(in)); }
    
    public CharWindow(InputStream in, int minBufferSize) throws IOException
    { this(new InputStreamReader(in), minBufferSize); }
    
    public CharWindow(String s) throws IOException
    { this(new StringReader(s)); }
    
    public CharWindow(String s, int minBufferSize) throws IOException
    { this(new StringReader(s), minBufferSize); }
//}}}

//{{{ charAt, length, advance
//##############################################################################
    public char charAt(int index)
    {
        if(index < 0 || index > dataLen)
            throw new IndexOutOfBoundsException("Current range is [0, "+dataLen+"); can't get "+index);
        else
            return buffer[ (int)((prevChars + index) & bufMask) ];
    }
    
    /** Returns the actual remaining length, or the buffer size if not yet at EOF. */
    public int length()
    { return dataLen; }
    
    /** Advances the zero point of this simulated CharSequence by the specified amount. */
    public void advance(int howMuch) throws IOException
    {
        if(howMuch < 0)
            throw new IllegalArgumentException("Can't advance backwards!");
        prevChars += howMuch;
        dataLen -= howMuch;
        for(int bufLen = buffer.length; dataLen < bufLen; dataLen++)
        {
            int c = reader.read();
            if(c == -1)
            {
                if(dataLen < 0) dataLen = 0;
                break;
            }
            buffer[ (int)((prevChars + dataLen) & bufMask) ] = (char) c;
        }
    }
//}}}

//{{{ toString, subSequence
//##############################################################################
    /** Returns a static snapshot of the specified region */
    public String toString(int start, int end)
    {
        StringBuffer sb = new StringBuffer(end - start);
        for(int i = start; i < end; i++)
            sb.append( this.charAt(i) );
        return sb.toString();
    }
    
    /** Returns a static snapshot of all characters in the buffer */
    public String toString()
    { return toString(0, dataLen); }
    
    /** Same as toString(start, end) */
    public CharSequence subSequence(int start, int end)
    { return toString(start, end); }
//}}}

//{{{ lineAt, columnAt
//##############################################################################
    /**
    * Returns the line number (starting from 1) of the given character index.
    */
    public int lineAt(int index)
    {
        int line = this.lineAtZero;
        for(int i = 0; i < index; i++)
        {
            if(charAt(i) == '\n') // line endings are normalized by Readers
                line++;
        }
        return line + 1;
    }

    /**
    * Returns the column number on the current line (starting from 1) of the given character index.
    */
    public int columnAt(int index)
    {
        int col = this.colAtZero;
        for(int i = 0; i < index; i++)
        {
            if(charAt(i) == '\n') // line endings are normalized by Readers
                col = 0;
            else
                col++;
        }
        return col + 1;
    }
//}}}

//{{{ main (simple unit test)
//##############################################################################
    /** Echos back complete lines typed at the console. */
    public static void main(String[] args) throws Exception
    {
        // Must pipe in file on stdin -- can't fill buffer properly from console.
        CharWindow w = new CharWindow(new InputStreamReader(System.in));
        int i = 0;
        while(w.length() > i)
        {
            char c = w.charAt(i++);
            System.out.println("% got 0x"+Integer.toHexString(c));
            if(c == '\n')
            {
                System.out.println("> "+w.subSequence(0,i-1));
                w.advance(i);
                System.out.println("> len = "+w.length());
                i = 0;
            }
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

