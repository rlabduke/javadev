// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.parser;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>RegexTokenMatcher</code> matches input tokens based on regular expressions.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May  4 08:18:49 EDT 2007
*/
public class RegexTokenMatcher implements TokenMatcher
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Matcher matcher;
    int     whichGroup;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * At any point in a valid input sequence, one pattern or the other should match.
    * Together, they should consume the entire sequence and return a series
    * of tokens which should either be accepted (keywords, names, etc) or
    * silently ignored (whitespace, comments, etc).
    * <p>The entire text matched by <code>accept</code> will be returned
    * after normalization (implemented by a subclass).
    */
    public RegexTokenMatcher(Pattern accept, Pattern ignore)
    {
        super();
        
        // Need ignore at begining for leading ignorables in file, at end for trailing.
        this.matcher = Pattern.compile(
            "(?:"+ignore.pattern()+")*"
            +"("+accept.pattern()+")"
            +"(?:"+ignore.pattern()+")*"
        ).matcher("");
        
        // groupCount() works even before trying a match
        this.whichGroup = ignore.matcher("").groupCount() + 1;
    }
    
    public RegexTokenMatcher(String accept, String ignore)
    { this(Pattern.compile(accept), Pattern.compile(ignore)); }
    
    public RegexTokenMatcher(Matcher accept, Matcher ignore)
    { this(accept.pattern(), ignore.pattern()); }
//}}}

//{{{ match, end, token, normalize
//##############################################################################
    public boolean match(CharSequence s, int start)
    {
        this.matcher.reset(s);
        matcher.region(start, s.length());
        // match must start at begining, but doesn't have to extend to end
        return matcher.lookingAt();
    }
    
    public int end()
    { return matcher.end(); /* absolute position, not relative to region() */}
    
    public CharSequence token()
    { return normalize(matcher.group(whichGroup)); }
    
    /**
    * May be overriden by subclasses to normalize the raw tokens matched by the accept pattern.
    * In this implementation, it simply returns rawToken as-is.
    */
    public String normalize(String rawToken)
    { return rawToken; }
//}}}

//{{{ joinPatterns
//##############################################################################
    /** Strings together a bunch of Patterns with OR bars (|). */
    public static Pattern joinPatterns(Pattern[] p)
    {
        StringBuffer b = new StringBuffer();
        for(int i = 0; i < p.length; i++)
        {
            if(i > 0) b.append('|');
            b.append("(?:");
            b.append(p[i].pattern());
            b.append(')');
        }
        return Pattern.compile(b.toString());
    }

    /** Strings together a bunch of Patterns with OR bars (|). */
    public static Pattern joinPatterns(String[] p)
    {
        StringBuffer b = new StringBuffer();
        for(int i = 0; i < p.length; i++)
        {
            if(i > 0) b.append('|');
            b.append("(?:");
            b.append(p[i]);
            b.append(')');
        }
        return Pattern.compile(b.toString());
    }
//}}}

//{{{ main (simple unit test)
//##############################################################################
    public static void main(String[] args)
    {
        // Matches foo's separated by whitespace
        RegexTokenMatcher m = new RegexTokenMatcher("foo", "\\s");
        String test = "foo   foo  foofoo  f";
        System.out.println(test);
        //int[] pos = {0, 1, 3, 7, 8};
        //for(int i = 0; i < pos.length; i++)
        //    System.out.println("matches at "+pos[i]+"? "+(m.match(test, pos[i]) ? "true ["+pos[i]+", "+m.end()+") -> '"+m.token()+"'" : "false"));
        for(int i = 0; i < test.length(); i = m.end())
        {
            if(!m.match(test, i))
            {
                System.out.println("*** Syntax error at "+i);
                break;
            }
            System.out.println("> "+m.token());
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

