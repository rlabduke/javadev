// (jEdit options) :folding=explicit:collapseFolds=1:
package king;
//import java.awt.*;
import java.io.*;
//import java.util.*;
//import javax.swing.*;

/**
* <code>KinParser</code> is based on the concepts of <code>java.io.StreamTokenizer</code>, but is specialized for reading kinemages.
* To facilitate a multi-level or recursive decoder, the next() and longText() methods keep returning the same value
* until someone acknowledges the use of the token by calling consume().
*
* <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Apr 27 23:12:12 EDT 2002
*/

public class KinParser //extends ... implements ...
{
//{{{ type constants
    /** The end of the file has been reached */
    public static final int EOF     = 0x0001;
    /** An uninterpretable sequence */
    public static final int BADVAL  = 0x0002;
    /** A keyword, like @group or @kinemage (with the '@' sign) */
    public static final int KEYWORD = 0x0004;
    /** An identifying name, like {Data} or {Ligands} (without braces) */
    public static final int NAME    = 0x0008;
    /** A parameter, like color= or master= (without the trailing '=' sign) */
    public static final int PARAM   = 0x0010;
    /** An integer */
    public static final int INT     = 0x0020;
    /** A real (float or double) */
    public static final int REAL    = 0x0040;
    /** Any kind of number, == (INT | REAL) */
    public static final int NUMBER  = 0x0060;
    /** An unremarkable string constant, like animate or hotpink */
    public static final int CONST   = 0x0080;
    /** A string of aspect specifiers, like (ABC) (without parens) */
    public static final int ASPECT  = 0x0100;
    /** A comment, &lt;like this&gt; (without brackets) */
    public static final int COMMENT = 0x0200;
    /** A long block of text, as follows @text or @caption */
    public static final int LONGTXT = 0x0400;
    /** A single-quoted string, as for pointmasters */
    public static final int SQUOTE  = 0x0800;
    /** A double-quoted string */
    public static final int DQUOTE  = 0x1000;
    
    static final int END_OF_KEYWORD  = EOF | KEYWORD;
    static final int END_OF_POINT = EOF | KEYWORD | NAME;
//}}}

//{{{ Powers of ten    
//##################################################################################################
    double[] powersOfTen = {
        1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9,
        1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19,
        1e20, 1e21, 1e22, 1e23, 1e24, 1e25, 1e26, 1e27, 1e28, 1e29,
        1e30, 1e31, 1e32, 1e33, 1e34, 1e35, 1e36, 1e37, 1e38, 1e39,
        1e40, 1e41, 1e42, 1e43, 1e44, 1e45, 1e46, 1e47, 1e48, 1e49,
        1e50, 1e51, 1e52, 1e53, 1e54, 1e55, 1e56, 1e57, 1e58, 1e59,
        1e60, 1e61, 1e62, 1e63, 1e64, 1e65, 1e66, 1e67, 1e68, 1e69,
        1e70, 1e71, 1e72, 1e73, 1e74, 1e75, 1e76, 1e77, 1e78, 1e79,
        1e80, 1e81, 1e82, 1e83, 1e84, 1e85, 1e86, 1e87, 1e88, 1e89,
        1e90, 1e91, 1e92, 1e93, 1e94, 1e95, 1e96, 1e97, 1e98, 1e99,
        1e100, 1e101, 1e102, 1e103, 1e104, 1e105, 1e106, 1e107, 1e108, 1e109,
        1e110, 1e111, 1e112, 1e113, 1e114, 1e115, 1e116, 1e117, 1e118, 1e119,
        1e120, 1e121, 1e122, 1e123, 1e124, 1e125, 1e126, 1e127, 1e128, 1e129,
        1e130, 1e131, 1e132, 1e133, 1e134, 1e135, 1e136, 1e137, 1e138, 1e139,
        1e140, 1e141, 1e142, 1e143, 1e144, 1e145, 1e146, 1e147, 1e148, 1e149,
        1e150, 1e151, 1e152, 1e153, 1e154, 1e155, 1e156, 1e157, 1e158, 1e159,
        1e160, 1e161, 1e162, 1e163, 1e164, 1e165, 1e166, 1e167, 1e168, 1e169,
        1e170, 1e171, 1e172, 1e173, 1e174, 1e175, 1e176, 1e177, 1e178, 1e179,
        1e180, 1e181, 1e182, 1e183, 1e184, 1e185, 1e186, 1e187, 1e188, 1e189,
        1e190, 1e191, 1e192, 1e193, 1e194, 1e195, 1e196, 1e197, 1e198, 1e199,
        1e200, 1e201, 1e202, 1e203, 1e204, 1e205, 1e206, 1e207, 1e208, 1e209,
        1e210, 1e211, 1e212, 1e213, 1e214, 1e215, 1e216, 1e217, 1e218, 1e219,
        1e220, 1e221, 1e222, 1e223, 1e224, 1e225, 1e226, 1e227, 1e228, 1e229,
        1e230, 1e231, 1e232, 1e233, 1e234, 1e235, 1e236, 1e237, 1e238, 1e239,
        1e240, 1e241, 1e242, 1e243, 1e244, 1e245, 1e246, 1e247, 1e248, 1e249,
        1e250, 1e251, 1e252, 1e253, 1e254, 1e255, 1e256, 1e257, 1e258, 1e259,
        1e260, 1e261, 1e262, 1e263, 1e264, 1e265, 1e266, 1e267, 1e268, 1e269,
        1e270, 1e271, 1e272, 1e273, 1e274, 1e275, 1e276, 1e277, 1e278, 1e279,
        1e280, 1e281, 1e282, 1e283, 1e284, 1e285, 1e286, 1e287, 1e288, 1e289,
        1e290, 1e291, 1e292, 1e293, 1e294, 1e295, 1e296, 1e297, 1e298, 1e299,
        1e300, 1e301, 1e302, 1e303, 1e304, 1e305, 1e306, 1e307, 1e308
    };
//}}}

//{{{ character constants    
//##################################################################################################
    static final String newlines   = "\r\n";
    static final String whitespace = ", \t\r\n";
    static final String terminator = ", \t\r\n={(<";
    static final String number     = "1234567890.+-";
    static final char   atsign     = '@';
    static final char   eqsign     = '=';
    static final char   name       = '{';
    static final String endname    = "}\r\n";
    static final char   aspect     = '(';
    static final String endaspect  = ")\r\n";
    static final char   comment    = '<';
    static final String endcomment = ">\r\n";
    static final char   squote     = '\'';
    static final String endsquote  = "'\r\n";
    static final char   dquote     = '"';
    static final String enddquote  = "\"\r\n";
//}}}

//{{{ variables    
//##################################################################################################
    /** The type of entity just read */
    public int    type = 0;
    /** Integer value of last entity, if any */
    public int    ival = 0;
    /** Float value of last entity, if any */
    public float  fval = 0f;
    /** Double value of last entity, if any */
    public double dval = 0.0;
    /** String value of last entity */
    public String sval = null;
    
    PushbackReader r = null;
    //StringBuffer buf = null;
    //char           c = 0;
    
    boolean isConsumed = true;
    
    // Buffer variables
    char[] cbuf = new char[4096];
    int ibuf = 0; // current index for insertion
//}}}

//{{{ Constructor    
//##################################################################################################
    /**
    * Create a new kinemage parser from some input stream.
    */
    public KinParser(Reader in)
    {
        r = new PushbackReader(in);
    }
//}}}

//{{{ consume(), isXXX()
//##################################################################################################
    /**
    * Acknowledges that the current token has been processed and that the stream should read the next one.
    */
    public void consume() { isConsumed = true; }
    
    public boolean isEOF() { return (type & EOF) != 0; }
    public boolean isKeyword() { return (type & KEYWORD) != 0; }
    public boolean isName() { return (type & NAME) != 0; }
    public boolean isParam() { return (type & PARAM) != 0; }
    public boolean isNumber() { return (type & NUMBER) != 0; }
    public boolean isInteger() { return (type & INT) != 0; }
    public boolean isConstant() { return (type & CONST) != 0; }
    public boolean isAspect() { return (type & ASPECT) != 0; }
    public boolean isComment() { return (type & COMMENT) != 0; }
    public boolean isSingleQuote() { return (type & SQUOTE) != 0; }
    public boolean isDoubleQuote() { return (type & DQUOTE) != 0; }
    public boolean isEndOfKeyword() { return (type & END_OF_KEYWORD) != 0; }
    /** Warning: this will return true as soon as a NAME is encountered, so watch for e.g. master= {xxx} */
    public boolean isEndOfPoint() { return (type & END_OF_POINT) != 0; }
//}}}

//{{{ longText()
//##################################################################################################
    /**
    * Process until the next keyword at the begining of a line.
    * This will return the same value every time until consume() is called.
    * @return a constant signifying the type of token read, or -1 on EOF
    */
    public int longText()
    {
        // Unless someone's called consume(), this does nothing!
        if(!isConsumed) return type;
        
        // Zero everything out
        type = LONGTXT;
        char c;
        
        try
        {
            for(c = rread(); c != atsign; c = rread())
            {
                for( ; newlines.indexOf(c) == -1; c = rread()) bufAppend(c);
                bufAppend(c);
            }
            r.unread(c);
        }
        catch(IOException ex)
        {
            type |= EOF;
        }
        
        sval = bufToString();
        
        bufClear();
        isConsumed = false;
        return type;
    }

//}}}

//{{{ next()
//##################################################################################################
    /**
    * Process the next token.
    * This will return the same value every time until consume() is called.
    * @return a constant signifying the type of token read, or -1 on EOF
    */
    public int next()
    {
        // Unless someone's called consume(), this does nothing!
        if(!isConsumed) return type;
        
        // Zero everything out
        type = BADVAL;
        char c;
        
        try {
            // Discard any leading whitespace
            discardWhile(whitespace);
            
            // Read the first (non-whitespace) character, and decide what sort of thing this is.
            c = rread();
            if(c == atsign)
            {
                bufAppend(c);
                readUntil(terminator);
                type = KEYWORD;
            }
            else if(c == name)
            {
                readUntil(endname);
                rread(); // discard end-of-name character
                type = NAME;
            }
            else if(c == aspect)
            {
                readUntil(endaspect);
                rread(); // discard end-of-aspect character
                type = ASPECT;
            }
            else if(c == comment)
            {
                readUntil(endcomment);
                rread(); // discard end-of-comment character
                type = COMMENT;
            }
            else if(c == squote)
            {
                readUntil(endsquote);
                rread(); // discard end-of-quote character
                type = SQUOTE;
            }
            else if(c == dquote)
            {
                readUntil(enddquote);
                rread(); // discard end-of-quote character
                type = DQUOTE;
            }
            else if(number.indexOf(c) != -1)
            {
                bufAppend(c);
                readUntil(terminator);
                type = REAL;
            }
            else
            {
                bufAppend(c);
                readUntil(terminator);
                discardWhile(whitespace);
                c = rread();
                if(c == eqsign) type = PARAM;
                else { type = CONST; r.unread(c); }
            }
        }//try
        catch(IOException ex)
        {
            type |= EOF;
        }

        // If applicable, try parsing this number
        if(isNumber())
        {
            sval = "$*$";
            boolean hasDot = false;
            try
            {
                dval = bufToDouble();
                fval = (float)dval;
                type |= REAL;

                for(int i = 0; i < ibuf; i++) { if(cbuf[i] == '.') hasDot = true; }
                if(!hasDot)
                {
                    ival = bufToInt();
                    type |= INT;
                }
            } 
            catch(NumberFormatException ex)
            {
                type = CONST;
                sval = bufToString();
                //System.err.println("sval="+sval+"; dval="+dval+"; ibuf="+ibuf+"; "+ex.getMessage());
            }
        }
        else sval = bufToString();
        
        bufClear();
        isConsumed = false;
        return type;
    }
//}}}

//{{{ internal reading functions    
//##################################################################################################
    void readUntil(String term) throws IOException
    {
        
        char c;
        for(c = rread(); term.indexOf(c) == -1; c = rread() ) bufAppend(c);
        r.unread(c);
    }
        
    void readWhile(String term) throws IOException
    {
        char c;
        for(c = rread(); term.indexOf(c) != -1; c = rread() ) bufAppend(c);
        r.unread(c);
    }

//##################################################################################################
    void discardUntil(String term) throws IOException
    {
        char c;
        for(c = rread(); term.indexOf(c) == -1; c = rread() );
        r.unread(c);
    }
        
    void discardWhile(String term) throws IOException
    {
        char c;
        for(c = rread(); term.indexOf(c) != -1; c = rread() );
        r.unread(c);
    }

//##################################################################################################
    char rread() throws IOException
    {
        int ch = r.read();
        if(ch == -1) throw new EOFException();
        else return (char)ch;
    }
    
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}

//{{{ buffer manipulation
//##################################################################################################
    void bufAppend(int ch)
    {
        if(ibuf >= cbuf.length)
        {
            char[] newbuf = new char[ cbuf.length*2 ];
            System.arraycopy(cbuf, 0, newbuf, 0, ibuf);
            cbuf = newbuf;
        }
        cbuf[ibuf++] = (char)ch;
    }
    
    void bufClear()
    {
        ibuf = 0;
    }
    
    String bufToString()
    {
        return new String(cbuf, 0, ibuf);
    }
    
    int bufToInt() throws NumberFormatException
    {
        char    c;
        int     i   = 0;
        int     val = 0;
        boolean isNegative = (cbuf[0] == '-');
        if(isNegative) i++;
        else if(cbuf[0] == '+') i++;
        
        for( ; i < ibuf; i++)
        {
            c = cbuf[i];
            if('0' <= c && c <= '9') val = val*10 + (c - '0');
            else throw new NumberFormatException("Illegal character '"+c+"'");
        }
        if(isNegative) val = -val;
        
        return val;
    }

    // This is accurate enough for floats, but not for doubles
    // Actually, it gets the doubles generated by Math.random()
    // exactly right about 80% of the time, and has errors in the
    // last significant digit or two the rest of the time.
    // It's MUCH faster than Double.parseDouble(new String(char[]))!
    double bufToDouble() throws NumberFormatException
    {
        char    c;
        int     i   = 0;
        double  val = 0;
        int     exp = 0;
        int     decpt = -1;     // index of the decimal point
        int     endpt = ibuf;   // index of the first non-decimal digit
        boolean isNegative = (cbuf[0] == '-');
        if(isNegative) i++;
        else if(cbuf[0] == '+') i++;
        
        for( ; i < ibuf; i++)
        {
            c = cbuf[i];
            if(c == '.')
            {
                if(decpt != -1) throw new NumberFormatException("Two decimal points");
                decpt = i; // mark the decimal point here
            }
            else if('0' <= c && c <= '9') val = val*10.0 + (c - '0');
            else if(c == 'e' || c == 'E')
            {
                endpt = i;
                if(++i == ibuf) throw new NumberFormatException("Nothing after exponent");
                boolean isExpNegative = (cbuf[i] == '-');
                if(isExpNegative) i++;
                else if(cbuf[i] == '+') i++;
                
                for( ; i < ibuf; i++)
                {
                    c = cbuf[i];
                    if('0' <= c && c <= '9') exp = exp*10 + (c - '0');
                    else throw new NumberFormatException("Illegal character '"+c+"'");
                }
                if(isExpNegative) exp = -exp;
            }
            else throw new NumberFormatException("Illegal character '"+c+"'");
        }
        
        if(decpt != -1) exp += decpt-endpt+1;
        
        if(exp < -308) val = 0.0;
        else if(exp < 0) val /= powersOfTen[-exp];
        else if(exp < 309) val *= powersOfTen[exp];
        else val = Double.POSITIVE_INFINITY;
        
        if(isNegative) val = -val;
        return val;
    }
//}}}

//{{{ empty_segment    
//##################################################################################################
//}}}
}//class
