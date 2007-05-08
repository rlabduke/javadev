// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.parser;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>TokenWindow</code> is the basis for tokenizing parsers.
* It supports token look-ahead and look-behind.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May  4 09:32:28 EDT 2007
*/
public class TokenWindow //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The source of tokens */
    CharWindow      window;
    /** The recognizer of tokens */
    TokenMatcher    matcher;
    /** How many tokens have been read before the current tokenAt(0); can't be accessed any more. */
    int             prevTokens  = 0;
    /** Highest index (exclusive) accessible by tokenAt(); this and higher indices cause more reading. */
    int             bufEnd      = 0;
    /** Power-of-two-minus-one bitmask used to wrap absolute token index (prevChars + tokenAtIndex) to a buffer index. */
    int             bufMask;
    /** Actual buffer of tokens; reading and writing wrap around in a cycle. */
    String[]        buffer;
    /** Results of window.end() for each token in the buffer. */
    int[]           winEnd;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Default buffer size is 16 */
    public TokenWindow(CharWindow window, TokenMatcher matcher)
    { this(window, matcher, 16); }
    
    public TokenWindow(CharWindow window, TokenMatcher matcher, int minBufferSize)
    {
        int bufLen;
        for(bufLen = 4; bufLen < minBufferSize; bufLen <<= 1);
        this.buffer     = new String[bufLen];   // a power of two
        this.winEnd     = new    int[bufLen];
        this.bufMask    = bufLen - 1;           // lowest N bits set
        this.window     = window;
        this.matcher    = matcher;
    }
//}}}

//{{{ token, tokenAt
//##############################################################################
    /** Same as tokenAt(0). */
    public String token() throws ParseException
    { return tokenAt(0); }
    
    /**
    * Returns the token at the specified index, or null if EOF.
    * @throws ParseException if more characters exist but they don't make a token.
    */
    public String tokenAt(int index) throws ParseException
    {
        if(index < bufEnd - buffer.length)
            throw new IndexOutOfBoundsException("Current range is ["+(bufEnd - buffer.length)+", "+bufEnd+"); can't get "+index);
        // If we wanted to catch references before the begining of the file:
        //else if(prevTokens + index < 0)
        //    throw new IndexOutOfBoundsException("Can't reference positions before the beginging of the stream");
        else while(index >= bufEnd)
        {
            int bufIns = (prevTokens + bufEnd++) & bufMask;
            int bufPrev = (bufIns - 1) & bufMask;
            int start = winEnd[bufPrev];
            if(start >= window.length()) // EOF
            {
                buffer[bufIns] = null;
                winEnd[bufIns] = start;
            }
            else if(matcher.match(window, start))
            {
                buffer[bufIns] = matcher.token();
                winEnd[bufIns] = matcher.end();
            }
            else
                throw syntaxError(matcher.end(), "bad token");
        }
        return buffer[ (prevTokens + index) & bufMask ];
    }
//}}}
    
//{{{ advance
//##############################################################################
    /** Same as advance(1). */
    public void advance() throws ParseException, IOException
    { advance(1); }
    
    /** Advances the zero point by the specified amount. */
    public void advance(int howMuch) throws ParseException, IOException
    {
        if(howMuch < 0)
            throw new IllegalArgumentException("Can't advance backwards!");
        tokenAt(howMuch); // forces advancement
        // if prevTokens == 0 and howMuch == 0, then winEnd[*] == 0, so it's OK
        int idx = (prevTokens + howMuch - 1) & bufMask;
        int adv = winEnd[idx];
        window.advance(adv);
        prevTokens += howMuch;
        bufEnd -= howMuch; // assert bufEnd >= 1;
        // Correct the end indices for buffered tokens
        // by the amount we advanced the window:
        for(int i = 0; i < buffer.length; i++)
            winEnd[ (prevTokens + i) & bufMask ] -= adv;
    }
//}}}

//{{{ accept
//##############################################################################
    /** If <code>s.equals(token())</code>, return true and advance by one .*/
    public boolean accept(String s) throws ParseException, IOException
    {
        String token = this.token();
        if(token == null) return false;
        
        if(s.equals(token))
        {
            advance();
            return true;
        }
        else return false;
    }

    /** If <code>m.reset(token()).matches()</code>, return true and advance by one .*/
    public boolean accept(Matcher m) throws ParseException, IOException
    {
        String token = this.token();
        if(token == null) return false;
        
        if(m.reset(token).matches())
        {
            advance();
            return true;
        }
        else return false;
    }
//}}}

//{{{ syntaxError
//##############################################################################
    /** Makes a ParseException with the specified message (by default at current token position [0]) */
    public ParseException syntaxError(int index, String detail)
    {
        int     bufCurr = (prevTokens + index) & bufMask;
        boolean isEOF   = (buffer[bufCurr] == null); // don't check if index is out of tokenAt() range -- who cares?
        int     bufPrev = (bufCurr - 1) & bufMask;
        int     pos     = winEnd[bufPrev]; // character position of start of bad token
        System.err.println("pos = "+pos);
        int     line    = window.lineAt(pos);
        int     col     = window.columnAt(pos);
        
        StringBuffer err = new StringBuffer();
        err.append("Syntax error at line "+line+", column "+col+": "+detail+"\n");
        String snipet = window.contextAt(pos);
        err.append("> "+snipet+"\n");
        if(col <= snipet.length() || isEOF)
        {
            err.append('>');
            for(int i = 0; i < col; i++) err.append(' ');
            err.append("^\n");
        }
        return new ParseException(err.toString(), line);
    }
    
    public ParseException syntaxError(String detail)
    { return syntaxError(0, detail); }
//}}}

//{{{ main (simple unit test)
//##############################################################################
    public static void main(String[] args) throws Exception
    {
        // Should be able to tokenize Java and maybe other languages
        // Some confusion between + and - as operators vs. signs on numbers
        Pattern[] accept = {
            RegexTokenMatcher.REAL_NUM,
            RegexTokenMatcher.JAVA_WORD,
            RegexTokenMatcher.JAVA_PUNC,
            RegexTokenMatcher.SINGLE_QUOTED_STRING,
            RegexTokenMatcher.DOUBLE_QUOTED_STRING
        };
        Pattern[] ignore = {
            RegexTokenMatcher.WHITESPACE,
            RegexTokenMatcher.HASH_COMMENT,
            RegexTokenMatcher.DOUBLE_SLASH_COMMENT,
            RegexTokenMatcher.SLASH_STAR_COMMENT
        };
        TokenMatcher m = new RegexTokenMatcher(
            //"\\S+", // accept maximal strings of non-whitespace characters
            //"\\s"   // ignore whitespace
            RegexTokenMatcher.joinPatterns(accept),
            RegexTokenMatcher.joinPatterns(ignore)
        );
        CharWindow w = new CharWindow(System.in);
        TokenWindow t = new TokenWindow(w, m);
        while(t.token() != null)
        {
            System.out.println(t.token());
            System.out.println("-----");
            t.advance();
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

