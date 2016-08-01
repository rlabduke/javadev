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
    /** 2^10 (1024) characters, for specifying buffer sizes */
    public static final int KILOCHAR = 1<<10;
    
    /** 2^20 (1024 * 1024) characters, for specifying buffer sizes */
    public static final int MEGACHAR = 1<<20;
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The source of characters */
    Reader  reader;
    /** How many characters have been read before the current charAt(0) */
    long    prevChars   = 0;
    /** Number of line breaks preceding preceding charAt(0) */
    int     lineAtZero  = 0;
    /** Number of characters following the last line break but before charAt(0) */
    int     colAtZero   = 0;
    /** Number of usable characters in the buffer */
    int     dataLen     = 0;
    /** Minimum accessible index in the buffer */
    int     dataMin     = 0;
    /** Power-of-two-minus-one bitmask used to wrap absolute character index (prevChars + charAtIndex) to a buffer index. */
    long    bufMask;
    /** Actual buffer of characters; reading and writing wrap around in a cycle. */
    char[]  buffer;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Default buffer size is 16 kchars,
    * enough for ~200 (80 char) lines of comments and/or whitespace
    * preceding a single real token.
    */
    public CharWindow(Reader reader) throws IOException
    { this(reader, 16*KILOCHAR); }
    
    /**
    * Creates a new window around the given reader,
    * with a buffer of at least the specified size (possibly larger).
    * Actually, the buffer is at least twice the minimum size,
    * so you get equal amounts of look-ahead and look-behind.
    * (Look-behind turns out to be very important for recovering the context
    * of syntax errors!)
    */
    public CharWindow(Reader reader, int minBufferSize) throws IOException
    {
        super();
        int bufLen;
        for(bufLen = 16; bufLen < minBufferSize; bufLen <<= 1);
        bufLen <<= 1; // double it: half for lookahead, half for lookbehind
        
        this.buffer     = new char[bufLen]; // a power of two
        this.bufMask    = bufLen - 1;       // lowest N bits set
        this.reader     = reader;
        
        // fill the buffer initially half full
        for(this.dataLen = 0; dataLen < bufLen/2; dataLen++)
        {
            int c = reader.read();
            if(c == -1) break;
            buffer[ (int)(dataLen & bufMask) ] = (char) c;
        }
    }
    
    public CharWindow(InputStream in) throws IOException
    { this(new InputStreamReader(in)); }
    
    public CharWindow(InputStream in, int minBufferSize) throws IOException
    { this(new InputStreamReader(in), minBufferSize); }
    
    /** Buffer size == s.length() */
    public CharWindow(String s) throws IOException
    { this(new StringReader(s), s.length()); }

    public CharWindow(File f) throws IOException
    { this(new FileReader(f)); }
    
    public CharWindow(File f, int minBufferSize) throws IOException
    { this(new FileReader(f), minBufferSize); }

    public CharWindow(URL url) throws IOException
    { this(url.openStream()); }
    
    public CharWindow(URL url, int minBufferSize) throws IOException
    { this(url.openStream(), minBufferSize); }
//}}}

//{{{ charAt, length, read, advance
//##############################################################################
    public char charAt(int index)
    {
        if(index < dataMin || index >= dataLen)
            throw new IndexOutOfBoundsException("Current range is ["+dataMin+", "+dataLen+"); can't get "+index);
        else
            return buffer[ (int)((prevChars + index) & bufMask) ];
    }
    
    /** Returns the actual remaining length, or the buffer size if not yet at EOF. */
    public int length()
    { return dataLen; }
    
    /** Returns character at index 0 and advances zero point by one. To detect EOF, check length() == 0. */
    public char read() throws IOException
    {
        // Get charAt(0); if newline, update counters.
        char atZero = buffer[ (int)(prevChars & bufMask) ];
        if(atZero == '\n')
        {
            this.lineAtZero++;
            this.colAtZero = 0;
        }
        else
            this.colAtZero++;
            
        // Read new char; if EOF, decrease available forward chars.
        int c = reader.read();
        if(c == -1)
            dataLen--;
        else
            buffer[ (int)((prevChars + dataLen) & bufMask) ] = (char) c;
        
        // Move current position forward by one.
        prevChars++;
        dataMin = (int) Math.max(-prevChars, dataLen - buffer.length);
        
        return atZero;
    }
    
    /** Call read() repeatedly to advance the zero point of this simulated CharSequence by the specified amount. */
    public void advance(int howMuch) throws IOException
    {
        if(howMuch < 0)
            throw new IllegalArgumentException("Can't advance backwards!");
        
        for(int i = 0; i < howMuch; i++) read();
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

//{{{ contextAt, startsWith
//##############################################################################
    /**
    * Returns a string showing the context of the specified position,
    * spanning the entire line that position occurs on (up to the limits of the buffer).
    */
    public String contextAt(int index)
    {
        int start = index;
        while(start > dataMin && charAt(start-1) != '\n') start--;
        int end = index;
        while(end < dataLen && charAt(end) != '\n') end++;
        
        return toString(start, end);
    }
    
    /**
    * Tests whether the string <code>other</code> appears in this buffer,
    * starting at position <code>index</code> in the window.
    * @param index defaults to 0
    */
    public boolean startsWith(CharSequence other, int index)
    {
        int thisLen = this.length() - index, otherLen = other.length();
        if(otherLen > thisLen)
            return false;
        for(int i = 0; i < otherLen; )
            if(this.charAt(index++) != other.charAt(i++))
                return false;
        return true;
    }
    
    public boolean startsWith(CharSequence other)
    { return startsWith(other, 0); }
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

