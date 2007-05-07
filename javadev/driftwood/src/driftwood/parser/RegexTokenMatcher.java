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
//{{{ Whitespace, or not
    /**
    * A string of whitespace characters, including newlines and carriage returns.
    */
    public static final Pattern WHITESPACE = Pattern.compile("(?:\\s+)");
    
    /**
    * A string of non-whitespace characters.
    */
    public static final Pattern NON_WHITESPACE = Pattern.compile("(?:\\S+)");
//}}} Whitespace, or not
    
//{{{ Numbers
    /**
    * The subset of INTEGER that represents the positive numbers (excludes 0).
    * A digit 1-9 possibly followed by other digits.
    */
    public static final Pattern POSITIVE_INT;
    
    /**
    * The subset of INTEGER that represents 0 and the positive numbers.
    * Either "0", or a digit 1-9 possibly followed by other digits.
    */
    public static final Pattern NATURAL_INT;
    
    /**
    * Recognizes a subset of the integer number formats acceptable to Integer.parseInt().
    * Allowed: explicit "+" signs
    * Disallowed: spaces, commas, computerized scientific notation, useless leading zeros.
    */
    public static final Pattern INTEGER;
    
    /**
    * Recognizes a subset of the real number formats acceptable to Double.parseDouble().
    * Allowed: explicit "+" signs, computerized scientific notation ("1e6" == 1 000 000).
    * Disallowed: spaces, commas, useless leading zeros.
    */
    public static final Pattern REAL_NUM;
    
    static
    {
        String sign     = "(?:[+-]?)";
        String digits   = "(?:[0-9]+)";
        String positive = "(?:[1-9][0-9]*)";
        String natural  = "(?:0|"+positive+")";
        String integer  = "(?:"+sign+natural+")";
        String real     = "(?:"+integer+"(?:\\.(?:"+digits+")?)?)";
        String real_exp = "(?:"+real+"(?:[eE]"+integer+")?)";
        
        POSITIVE_INT    = Pattern.compile(positive);
        NATURAL_INT     = Pattern.compile(natural);
        INTEGER         = Pattern.compile(integer);
        REAL_NUM        = Pattern.compile(real_exp);
    }
//}}} Numbers
    
//{{{ Words
    /**
    * Something that would be a valid variable/function/class name in Java and many other C-like languages.
    * Starts with a letter or underscore, and continues with letters, underscores, or digits.
    */
    public static final Pattern JAVA_WORD = Pattern.compile("(?:[a-zA-Z_][a-zA-Z_0-9]*)");
    
    /**
    * All the symbols / operators / punctuation from Java, and maybe a few other languages.
    */
    public static final Pattern JAVA_PUNC = Pattern.compile("(?:[~!%^&|*/<>.=+-]=?|[,;:?(){}\\[\\]]|&&|\\|\\||<<=?|>>=?|>>>=?|\\+\\+|--)");
//}}} Words

//{{{ Strings
    /**
    * The same as DOUBLE_QUOTE_STRING, but with single quotes (').
    */
    public static final Pattern SINGLE_QUOTE_STRING = Pattern.compile("(?s:'(?:[^'\\\\]|\\\\.)*')"); // (?s: ... ) lets . match linefeeds
    
    /**
    * The standard string format in Java, C, etc: delimited by quotes ("), escaped by backslashes (\).
    * Escaped control sequences like \n are not interpretted or removed by this pattern.
    * Internal newlines *are* allowed, unlike in Java etc.
    */
    public static final Pattern DOUBLE_QUOTE_STRING = Pattern.compile( SINGLE_QUOTE_STRING.pattern().replace('\'', '"') );
    
    /**
    * The same as DOUBLE_QUOTE_STRING, but with slashes (/).
    * If used for regular expressions, slashes must be escaped no matter where they appear
    * (eg even inside character classes).
    */
    public static final Pattern SLASH_QUOTE_STRING = Pattern.compile( SINGLE_QUOTE_STRING.pattern().replace('\'', '/') );
//}}} Strings
    
//{{{ Comments
    /**
    * A shell-style comment, starting with # and extending to end of line.
    */
    public static final Pattern HASH_COMMENT = Pattern.compile("(?:#.*)");
    
    /**
    * A C++ style comment, starting with // and extending to end of line.
    */
    public static final Pattern DOUBLE_SLASH_COMMENT = Pattern.compile("(?://.*)");
    
    /**
    * A C style comment, starting with slash-star and extending to star-slash.
    */
    public static final Pattern SLASH_STAR_COMMENT = Pattern.compile("(?s:/\\*.*?\\*/)"); // (?s: ... ) lets . match linefeeds
//}}} Comments
//}}}

//{{{ Variable definitions
//##############################################################################
    Matcher accept, ignore;
    String theToken = null;
    int tokenEnd = 0;
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
        this.accept = accept.matcher("");
        this.ignore = ignore.matcher("");
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
        this.ignore.reset(s);
        int end = start, len = s.length();
        while(end < len)
        {
            ignore.region(end, len);
            if(!ignore.lookingAt()) break;
            end = ignore.end(); // absolute position, not relative to region()
        }
        
        this.accept.reset(s);
        accept.region(end, len);
        // match must start at begining, but doesn't have to extend to end
        if(accept.lookingAt())
        {
            this.theToken = normalize(accept.group());
            this.tokenEnd = accept.end();
            return true;
        }
        // If whitespace consumed to end of s, return null token.
        // This allows us to deal with trailing whitespace but not
        // consume it prematurely, in case we change tokenization rules
        // in mid-file and it suddenly becomes significant.
        else if(end > start && end == len)
        {
            this.theToken = null;
            this.tokenEnd = ignore.end();
            return true;
        }
        else return false;
    }
    
    public int end()
    { return tokenEnd; }
    
    public String token()
    { return theToken; }
    
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

    public static Pattern joinPatterns(Pattern[] p)
    {
        String[] s = new String[p.length];
        for(int i = 0; i < p.length; i++)
            s[i] = p[i].pattern();
        return joinPatterns(s);
    }

    public static Pattern joinPatterns(Matcher[] p)
    {
        String[] s = new String[p.length];
        for(int i = 0; i < p.length; i++)
            s[i] = p[i].pattern().pattern();
        return joinPatterns(s);
    }
//}}}

//{{{ recursivePattern
//##############################################################################
    /**
    * Some languages have quoting systems that escape quote chacters with
    * a "balancing" requirement:  the number of "open" symbols must match
    * the number of "close" symbols, and the opens must precede the closes.
    * For example, PostScript quotes strings this way with parentheses,
    * kinemage quotes strings this way with curly braces,
    * and AppleScript does comments this way with <code>(* ... *)</code>.
    *
    * <p>Formally, <code>STRING = OPEN ( GUTS | STRING )+ CLOSE</code>.
    * By allowing GUTS to match 0 characters, one can allow empty strings.
    *
    * <p>Such a pattern cannot actually be specified with regular expressions;
    * however, by nesting things 10 or 20 levels deep, one can match any
    * reasonable real-life examples.
    */
    public static Pattern recursivePattern(String start, String guts, String close, int maxDepth)
    {
        StringBuffer b = new StringBuffer();
        for(int i = 0; i < maxDepth; i++)
            b.append("(?:").append(start).append(")(?:(?:").append(guts).append(")|(?:");
        b.append(guts);
        for(int i = 0; i < maxDepth; i++)
            b.append("))+(?:").append(close).append(")");
        return Pattern.compile(b.toString());
    }

    public static Pattern recursivePattern(Pattern start, Pattern guts, Pattern close, int maxDepth)
    { return recursivePattern(start.pattern(), guts.pattern(), close.pattern(), maxDepth); }

    public static Pattern recursivePattern(Matcher start, Matcher guts, Matcher close, int maxDepth)
    { return recursivePattern(start.pattern().pattern(), guts.pattern().pattern(), close.pattern().pattern(), maxDepth); }
//}}}

//{{{ main (simple unit test)
//##############################################################################
    public static void main(String[] args)
    {
        // Matches foo's separated by whitespace
        //RegexTokenMatcher m = new RegexTokenMatcher("foo", "\\s");
        //String test = "foo   foo  foofoo  f";
        //int[] pos = {0, 1, 3, 7, 8};
        //for(int i = 0; i < pos.length; i++)
        //    System.out.println("matches at "+pos[i]+"? "+(m.match(test, pos[i]) ? "true ["+pos[i]+", "+m.end()+") -> '"+m.token()+"'" : "false"));

        RegexTokenMatcher m = new RegexTokenMatcher(recursivePattern("<", "[a-zA-Z .]*", ">", 3), WHITESPACE);
        String test = "<foo>   <Fred approached.  <<Hi there>> said Bob.>";
        System.out.println(test);
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

