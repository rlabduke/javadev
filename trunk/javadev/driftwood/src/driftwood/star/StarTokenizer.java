// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.star;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>StarTokenizer</code> is a tokenizer that can handle all valid tokens
* allowed by the STAR (Self-defining Text Archive and Retrival) file format
* as of the 1994 paper by Hall and Spadaccini (J Chem Inf Comput Sci, 34:505).
*
* <p>One part of the syntax <i>not</i> documented is that within quoted strings,
* a doubling of the quote character should be interpretted as one of that
* character (literally).
* The final quote character must be followed by white space or EOF to really
* terminate the quoted string.
*
* <p>This class takes in a stream of characters and outputs discrete tokens,
* stripped of whitespace and quoting characters. Comments are discarded.
* Data and save block names are returned stripped of their leading "data_" or "save_".
* Data names are NOT stripped of their leading underscore.
*
* <p>Copyright (C) 2003-2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Apr  7 13:34:38 EDT 2003
*/
public class StarTokenizer //extends ... implements ...
{
//{{{ Constants
    static final String     TYPE_NONE           = "nil-type";
    static final String     TYPE_DATA_NAME      = "data-name";      // token starting with _
    static final String     TYPE_DATA_VALUE     = "data-value";     // any normal token
    static final String     TYPE_GLOBAL_START   = "global-start";   // start of a global_ block
    static final String     TYPE_DATA_START     = "data-start";     // start of a data_ block
    static final String     TYPE_SAVE_START     = "save-start";     // start of a save_ block
    static final String     TYPE_SAVE_END       = "save-end";       // end of a save_ block (save_)
    static final String     TYPE_LOOP_START     = "loop-start";     // start of a loop_ block (loop_)
    static final String     TYPE_STOP           = "stop";           // explicit end of a nested loop_ (stop_)

    static final int        NIL_CHARACTER       = -1;
//}}}

//{{{ Variable definitions
//##################################################################################################
    LineNumberReader        in;
    char[]                  buffer          = new char[256];
    int                     bufferIndex     = 0;
    int                     firstChar;      // first char of next token, or NIL if none
    long                    charsRead;      // a count of how many characters we've read
    
    String                  stringValue;
    String                  type;
    boolean                 isBOL;
    boolean                 isEOF;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new StarTokenizer on the given input stream,
    * and advances it to the first token.
    * @throws IOException if there's a problem reading from the input stream
    */
    public StarTokenizer(LineNumberReader input) throws IOException
    {
        in          = input;
        charsRead   = 0;
        isEOF       = false;
        // A trick -- this makes the tokenizer think that
        // the first token is at the beginning of a line.
        firstChar   = '\n';
        advance();
    }
//}}}

//{{{ buffer{Append, ToString, Clear}
//##################################################################################################
    /**
    * We use our own custom buffer system because
    * StringBuffers allocate memory 16 chars (32 bytes)
    * at a time, which is then wasted when we convert
    * them to Strings.
    */
    void bufferAppend(char ch)
    {
        if(bufferIndex >= buffer.length)
        {
            char[] newBuffer = new char[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
            //error("Token too long; increased buffer size to "+buffer.length);
        }
        buffer[bufferIndex++] = ch;
    }
    
    /** Returns the current buffer contents as a string */
    String bufferToString()
    { return new String(buffer, 0, bufferIndex); }
    
    /** Empties the buffer contents */
    void bufferClear()
    { bufferIndex = 0; }
//}}}

//{{{ in_read, getCharsRead, error
//##################################################################################################
    /**
    * A replacement for in.read() that allows us to track the
    * total number of character read so far.
    * Causes a performance penalty of less than 1%.
    */
    int in_read() throws IOException
    {
        charsRead++;
        return in.read();
    }
    
    /** Returns the number of characters read in thus far */
    public long getCharsRead()
    {
        return charsRead;
    }
    
    void error(String msg)
    {
        SoftLog.err.println("[line "+(in.getLineNumber()+1)+"] "+msg);
    }
//}}}

//{{{ advance
//##################################################################################################
    /**
    * Advances this tokenizer to the next token in the stream.
    * The type of token can be queried with the isXXX() functions,
    * and the value (in the appropriate form) can be retrieved with the getXXX() functions.
    * @throws IOException if there's a problem reading from the input stream
    */
    public void advance() throws IOException
    {
        int c;
        // Init variables
        stringValue     = null;
        type            = TYPE_NONE;
        isBOL           = false;
        bufferClear();
        
        // Get first character (from stream or previously read)
        if(firstChar > 0)   c = firstChar;
        else                c = in_read();
        firstChar = NIL_CHARACTER;
        
        // Skip leading whitespace and comments
        while(c == ' ' || c == '\n' || c == '\t' || c == '\f')
        {
            // LNR translates all linefeeds into '\n'
            isBOL   = (c == '\n');
            c       = in_read();
            if(c == '#') // skip comments
            {
                while(c != '\n' && c != -1)
                    c = in_read();
            }
        }
        
        // Guess token type based on first character
        if(c == -1)
        {
            isEOF = true;
            return;
        }
        else if(c == '\'' || c == '"')
        {
            readQuoted((char)c);
            type = TYPE_DATA_VALUE;
        }
        else if(isBOL && c == ';')
        {
            readLongQuoted((char)c);
            type = TYPE_DATA_VALUE;
        }
        else
        {
            bufferAppend((char)c);
            readUnquoted();
            
            if(stringValue.startsWith("_"))
                type = TYPE_DATA_NAME;
            else if(stringValue.equals("loop_"))
                type = TYPE_LOOP_START;
            else if(stringValue.startsWith("save_"))
            {
                if(stringValue.length() == 5)
                    type = TYPE_SAVE_END;
                else
                {
                    stringValue = stringValue.substring(5);
                    type = TYPE_SAVE_START;
                }
            }
            else if(stringValue.startsWith("data_"))
            {
                stringValue = stringValue.substring(5);
                type = TYPE_DATA_START;
            }
            else if(stringValue.equals("global_"))
                type = TYPE_GLOBAL_START;
            else if(stringValue.equals("stop_"))
                type = TYPE_STOP;
            else
                type = TYPE_DATA_VALUE;
        }
        
        bufferClear();
    }
//}}}

//{{{ readQuoted
//##################################################################################################
    /** Sets stringValue and appends characters to buffer */
    void readQuoted(char close) throws IOException
    {
        int c, depth = 1;
        while(depth > 0)
        {
            c = in_read();
            if(c == -1)         { depth = 0; error("Quoted token terminated by EOF; type = "+close+""+close); }
            else if(c == '\n')  { depth = 0; error("Quoted token terminated by newline; type = "+close+""+close); }
            //else if(c == close) depth--;
            else if(c == close)
            {
                int c2 = in_read();
                if(c2 == close)
                    bufferAppend((char)c2); // doubling up means one literal quote
                else if(c2 == -1 || c2 == ' ' || c2 == '\n' || c2 == '\t' || c2 == '\f')
                {
                    depth--; // real end of quoted string
                    firstChar = c2; // save this char for the next token
                }
                else // don't know what to do here -- a syntax error
                {
                    depth = 0;
                    firstChar = c2;
                    error("Quoted token terminated without trailing whitespace/EOF; type = "+close+""+close); 
                }
                    
            }
            else bufferAppend((char)c);
        }
        
        stringValue = bufferToString();
    }
//}}}

//{{{ readLongQuoted
//##################################################################################################
    /** Sets stringValue and appends characters to buffer */
    void readLongQuoted(char close) throws IOException
    {
        int c, depth = 1;
        boolean lastWasNewline = false;
        while(depth > 0)
        {
            c = in_read();
            if(c == -1)         { depth = 0; error("Quoted token terminated by EOF; type = "+close+""+close); }
            else if(c == '\n')  lastWasNewline = true;
            else if(lastWasNewline)
            {
                if(c == close) depth--;
                else
                {
                    bufferAppend('\n');
                    bufferAppend((char)c);
                }
                lastWasNewline = false;
            }
            else bufferAppend((char)c);
        }
        
        stringValue = bufferToString();
    }
//}}}

//{{{ readUnquoted
//##################################################################################################
    /** Sets stringValue and appends characters to buffer */
    void readUnquoted() throws IOException
    {
        int c;
        while(true)
        {
            c = in_read();
            if(c == -1 || c == ' ' || c == '\n' || c == '\t' || c == '\f')
            { firstChar = c; break; }
            else bufferAppend((char)c);
        }
        
        stringValue = bufferToString();
    }
//}}}

//{{{ is{BOL, EOF, etc}, getType
//##################################################################################################
    /** Returns true if the current token occured at the Beginning Of a Line */
    public boolean isBOL()
    { return isBOL; }
    /** Returns true if we've reached the End Of File */
    public boolean isEOF()
    { return isEOF; }
    /** Returns true if the current token is the name of a data item. */
    public boolean isName()
    { return (type == TYPE_DATA_NAME); }
    /** Returns true if the current token is the value of a data item. */
    public boolean isValue()
    { return (type == TYPE_DATA_VALUE); }
    /** Returns true if the current token is the start of a global_ block */
    public boolean isGlobal()
    { return (type == TYPE_GLOBAL_START); }
    /** Returns true if the current token is the start of a data_ block */
    public boolean isData()
    { return (type == TYPE_DATA_START); }
    /** Returns true if the current token is the start of a save_ block */
    public boolean isSaveStart()
    { return (type == TYPE_SAVE_START); }
    /** Returns true if the current token is the end of a save_ block */
    public boolean isSaveEnd()
    { return (type == TYPE_SAVE_START); }
    /** Returns true if the current token is the start of a loop_ */
    public boolean isLoopStart()
    { return (type == TYPE_LOOP_START); }
    /** Returns true if the current token is the explicit end of a loop_ (ie, a stop_) */
    public boolean isLoopEnd()
    { return (type == TYPE_STOP); }
    
    /** Returns the type of the current token as a string */
    public String getType()
    { return type; }
//}}}

//{{{ getString
//##################################################################################################
    /**
    * Returns the value of the current token as a string,
    * or null if there is no token available.
    */
    public String getString()
    {
        //if(stringValue == null)
        //    throw new IllegalStateException("No token is available");
        return stringValue;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ Main() and main()
//##################################################################################################
    /**
    * Main() function for running as an application.
    * Takes a STAR file on stdin and writes tokens to stdout
    */
    public void Main() throws IOException
    {
        String s;
        long time = System.currentTimeMillis();
        while(!isEOF())
        {
            s = getString();
            if(isBOL()) System.out.println("[BOL:"+justifyLeft(getType(), 12)+"] "+s);
            else        System.out.println("[    "+justifyLeft(getType(), 12)+"] "+s);
            advance();
        }
        time = System.currentTimeMillis() - time;
        System.out.println("END OF FILE ("+time+" ms)");
        System.out.println();
        System.out.println();
    }

    public static void main(String[] args)
    {
        if(args.length > 0)
            System.err.println("*** Takes a STAR file on stdin and writes tokens to stdout.");
        try
        { new StarTokenizer(new LineNumberReader(new InputStreamReader(System.in))).Main(); }
        catch(IOException ex)
        { ex.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ justifyLeft
//##################################################################################################
    /**
    * Pads a string with spaces and left-justifies it.
    *
    * @param s the string to justify
    * @param len the desired length
    */
    public static String justifyLeft(String s, int len)
    {
        StringBuffer sb = new StringBuffer(s);
        sb.ensureCapacity(len);
        for(int i = s.length(); i < len; i++) sb.append(' ');
        return sb.toString();
    }
//}}}
}//class

