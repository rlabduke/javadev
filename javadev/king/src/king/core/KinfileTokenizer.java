// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

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
* <code>KinfileTokenizer</code> is a second-generation implementation
* of a tokenizer that complies with the Kinemage File Format, v1.0.
*
* It takes in a stream of characters and outputs discrete tokens,
* stripped of whitespace and quoting characters.
* [Equals signs (=) and at signs (@) are preserved.]
*
* It currently does not check for the presence of illegal characters.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Apr  7 13:34:38 EDT 2003
*/
public class KinfileTokenizer //extends ... implements ...
{
//{{{ Constants
    static final String     TYPE_NONE           = "nil-type";
    static final String     TYPE_IDENTIFIER     = "identifier";
    static final String     TYPE_COMMENT        = "comment";
    static final String     TYPE_ASPECT         = "aspect";
    static final String     TYPE_SINGLE_QUOTE   = "s-quote";
    static final String     TYPE_DOUBLE_QUOTE   = "d-quote";

    static final String     TYPE_KEYWORD        = "keyword";
    static final String     TYPE_PROPERTY       = "property";
    static final String     TYPE_INTEGER        = "integer";
    static final String     TYPE_NUMBER         = "number";
    static final String     TYPE_LITERAL        = "literal";

    static final int        NIL_CHARACTER       = -1;
//}}}

//{{{ Variable definitions
//##################################################################################################
    LineNumberReader        in;
    char[]                  buffer          = new char[256];
    int                     bufferIndex     = 0;
    int                     firstChar;      // first char of next token
    long                    charsRead;      // a count of how many characters we've read
    
    String                  stringValue;
    double                  doubleValue;
    String                  type;
    boolean                 isBOL;
    boolean                 isEOF;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new KinfileTokenizer on the given input stream,
    * and advances it to the first token.
    * @throws IOException if there's a problem reading from the input stream
    */
    public KinfileTokenizer(LineNumberReader input) throws IOException
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
            error("Token too long; increased buffer size to "+buffer.length);
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
        doubleValue     = Double.NaN;
        type            = TYPE_NONE;
        isBOL           = false;
        bufferClear();
        
        // Get first character (from stream or previously read)
        if(firstChar > 0)   c = firstChar;
        else                c = in_read();
        firstChar = NIL_CHARACTER;
        
        // Skip leading whitespace
        while(c == ' ' || c == '\n' || c == ',' || c == '\t' || c == '\f')
        {
            // LNR translates all linefeeds into '\n'
            isBOL   = (c == '\n');
            c       = in_read();
        }
        
        // Guess token type based on first character
        if(c == -1)
        {
            isEOF = true;
            return;
        }
        else if(c == '{')
        {
            readQuoted('{', '}');
            type = TYPE_IDENTIFIER;
        }
        else if(c == '<')
        {
            readQuoted('<', '>');
            type = TYPE_COMMENT;
        }
        else if(c == '(')
        {
            readQuoted('(', ')');
            type = TYPE_ASPECT;
        }
        else if(c == '\'')
        {
            readQuoted('\'');
            type = TYPE_SINGLE_QUOTE;
        }
        else if(c == '"')
        {
            readQuoted('"');
            type = TYPE_DOUBLE_QUOTE;
        }
        else
        {
            bufferAppend((char)c);
            readUnquoted();
            // type is set in readUnquoted()
        }
        
        bufferClear();
    }
//}}}

//{{{ advanceToKeyword
//##################################################################################################
    /**
    * Reads ahead until encountering a keyword token,
    * one that begins with an at sign (@) at the beginning of a line.
    * All the characters from the current position to the
    * start of the keyword are returned by this function.
    * @throws IOException if there's a problem reading from the input stream
    */
    public String advanceToKeyword() throws IOException
    {
        int c;
        // Init variables
        stringValue     = null;
        doubleValue     = Double.NaN;
        type            = TYPE_NONE;
        isBOL           = false;
        bufferClear();
        
        // Get first character (from stream or previously read)
        if(firstChar > 0)   c = firstChar;
        else                c = in_read();
        firstChar = NIL_CHARACTER;
        
        // Read until we find a keyword
        StringBuffer longBuf = new StringBuffer(1024);
        while(!(isBOL && c == '@'))
        {
            longBuf.append((char)c);
            // LNR translates all linefeeds into '\n'
            isBOL   = (c == '\n');
            c       = in_read();
            if(c == -1)
            {
                isEOF = true;
                return longBuf.toString();
            }
        }
        
        String retVal = longBuf.toString();
        bufferAppend((char)c);
        readUnquoted();
        bufferClear();
        return retVal;
    }
//}}}

//{{{ readQuoted
//##################################################################################################
    /** Sets stringValue and doubleValue; appends characters to buffer */
    void readQuoted(char open, char close) throws IOException
    {
        int c, depth = 1;
        while(depth > 0)
        {
            c = in_read();
            if(c == -1)         { depth = 0; error("Quoted token terminated by EOF; type = "+open+""+close); }
            else if(c == open)  { ++depth;          bufferAppend((char)c); }
            else if(c == close) { if(--depth > 0)   bufferAppend((char)c); }
            else bufferAppend((char)c);
        }
        
        stringValue = bufferToString();
        doubleValue = Double.NaN;
    }

    /** Sets stringValue and doubleValue; appends characters to buffer */
    void readQuoted(char close) throws IOException
    {
        int c, depth = 1;
        while(depth > 0)
        {
            c = in_read();
            if(c == -1)         { depth = 0; error("Quoted token terminated by EOF; type = "+close+""+close); }
            else if(c == close) depth--;
            else bufferAppend((char)c);
        }
        
        stringValue = bufferToString();
        doubleValue = Double.NaN;
    }
//}}}

//{{{ readUnquoted
//##################################################################################################
    /** Sets stringValue, doubleValue, and type; appends characters to buffer */
    void readUnquoted() throws IOException
    {
        int c;
        while(true)
        {
            c = in_read();
            if(c == -1 || c == ' ' || c == '\n' || c == ',' || c == '\t' || c == '\f'
            || c == '{' || c == '<' || c == '(' || c == '\'' || c == '"')
            { firstChar = c; break; }
            else if(c == '=')
            { bufferAppend((char)c); break; }
            else bufferAppend((char)c);
        }
        
        /* This block allows spaces (but not newlines) to come between the word and the equals sign */
        while(c == ' ' || c == ',' || c == '\t')
        {
            c = in_read();
            if(c == '=')
            {
                bufferAppend('=');
                firstChar = NIL_CHARACTER;
            }
            else firstChar = c;
        }
        /* This block allows spaces (but not newlines) to come between the word and the equals sign */
        
        stringValue = bufferToString();
        doubleValue = Double.NaN;
        c = stringValue.charAt(0);
        
        if(stringValue.startsWith("@") && isBOL())  type = TYPE_KEYWORD;
        else if(stringValue.endsWith("="))          type = TYPE_PROPERTY;
        else if(('0' <= c && c <= '9') || c == '.' || c == '-' || c == '+')
        {
            try {
                doubleValue = Double.parseDouble(stringValue);
                if(doubleValue == Math.rint(doubleValue))   type = TYPE_INTEGER;
                else                                        type = TYPE_NUMBER;
            } catch(NumberFormatException ex) {             type = TYPE_LITERAL; }
        }
        else                                        type = TYPE_LITERAL;
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
    /** Returns true if the current token is an identifier */
    public boolean isIdentifier()
    { return (type == TYPE_IDENTIFIER); }
    /** Returns true if the current token is a comment */
    public boolean isComment()
    { return (type == TYPE_COMMENT); }
    /** Returns true if the current token is an aspect */
    public boolean isAspect()
    { return (type == TYPE_ASPECT); }
    /** Returns true if the current token is single quoted */
    public boolean isSingleQuote()
    { return (type == TYPE_SINGLE_QUOTE); }
    /** Returns true if the current token is double quoted */
    public boolean isDoubleQuote()
    { return (type == TYPE_DOUBLE_QUOTE); }
    /** Returns true if the current token is a keyword */
    public boolean isKeyword()
    { return (type == TYPE_KEYWORD); }
    /** Returns true if the current token is a property */
    public boolean isProperty()
    { return (type == TYPE_PROPERTY); }
    /** Returns true if the current token is an integer */
    public boolean isInteger()
    { return (type == TYPE_INTEGER); }
    /** Returns true if the current token is some kind of number */
    public boolean isNumber()
    { return (type == TYPE_NUMBER || type == TYPE_INTEGER); }
    /** Returns true if the current token is a string literal */
    public boolean isLiteral()
    { return (type == TYPE_LITERAL); }
    
    /** Returns the type of the current token as a string */
    public String getType()
    { return type; }
//}}}

//{{{ get{String, Int, Float, Double}
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
    
    /**
    * Returns the value of the current token as an integer.
    * @throws IllegalStateException if the token is not an integer
    */
    public int getInt()
    {
        if(!isInteger() || Double.isNaN(doubleValue) || doubleValue != Math.rint(doubleValue))
            throw new IllegalStateException("[line "+(in.getLineNumber()+1)+"] Token is not an integer");
        return (int)doubleValue;
    }
    
    /**
    * Returns the value of the current token as a float.
    * @throws IllegalStateException if the token is not a number
    */
    public float getFloat()
    {
        if(!isNumber() || Double.isNaN(doubleValue))
            throw new IllegalStateException("[line "+(in.getLineNumber()+1)+"] Token is not a number");
        return (float)doubleValue;
    }

    /**
    * Returns the value of the current token as a double.
    * @throws IllegalStateException if the token is not a number
    */
    public double getDouble()
    {
        if(!isNumber() || Double.isNaN(doubleValue))
            throw new IllegalStateException("[line "+(in.getLineNumber()+1)+"] Token is not a number");
        return doubleValue;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ Main() and main()
//##################################################################################################
    /**
    * Main() function for running as an application.
    * Takes a kinemage on stdin and writes tokens to stdout
    */
    public void Main(boolean doCSS) throws IOException
    {
        if(doCSS)
        {
            System.out.println("<div class='kin2html'>");
            while(!isEOF())
            {
                String start, end, s = getString();
                if(isBOL()) System.out.print("\n<br />");
                if(isIdentifier()) { start = "{"; end = "}"; }
                else if(isComment()) { start = "&lt;"; end = "&gt;"; }
                else if(isAspect()) { start = "("; end = ")"; }
                else if(isSingleQuote()) { start = "'"; end = "'"; }
                else if(isDoubleQuote()) { start = "\""; end = "\""; }
                else { start = ""; end = ""; }
                
                System.out.print("<span class='"+getType()+"'>"+start+s+end+"</span> ");

                if(isKeyword() && (s.equals("@text") || s.equals("@caption")))
                {
                    System.out.print("\n<pre>");
                    System.out.print(advanceToKeyword());
                    System.out.print("</pre>\n");
                }
                else advance();
            }
            System.out.println("\n</div>");
        }
        else
        {
            String s;
            long time = System.currentTimeMillis();
            while(!isEOF())
            {
                s = getString();
                if(isKeyword() && (s.equals("@text") || s.equals("@caption")))
                {
                    System.out.println("[>>> "+s+"]");
                    System.out.println(advanceToKeyword());
                    System.out.println("[<<< "+s+"]");
                }
                else
                {
                    if(isBOL()) System.out.println("[BOL:"+justifyLeft(getType(), 10)+"] "+s);
                    else        System.out.println("[    "+justifyLeft(getType(), 10)+"] "+s);
                    advance();
                }
            }
            time = System.currentTimeMillis() - time;
            System.out.println("END OF FILE ("+time+" ms)");
            System.out.println();
            System.out.println();
        }
    }

    public static void main(String[] args)
    {
        boolean doCSS = false;
        for(int i = 0; i < args.length; i++)
        {
            if("-css".equals(args[i])) doCSS = true;
            else System.err.println("*** Takes a kinemage on stdin and writes tokens to stdout.");
        }
        
        try
        { new KinfileTokenizer(new LineNumberReader(new InputStreamReader(System.in))).Main(doCSS); }
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

