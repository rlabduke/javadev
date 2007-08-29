// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2.selection;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.parser.*;
//}}}
/**
* <code>SelectionParser</code> parses Probe-like selection strings and
* generates a Selection object from them.
* See Selection for a specification of the grammar!
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:33:28 PDT 2007
*/
public class SelectionParser //extends ... implements ...
{
//{{{ Constants
    final Matcher LOOSE_OR      = Pattern.compile("\\||or").matcher("");
    final Matcher LOOSE_AND     = Pattern.compile("&|and").matcher("");
    final Matcher TIGHT_OR      = Pattern.compile(",").matcher("");
    final Matcher NOT_SYMBOL    = Pattern.compile("!|not").matcher("");
    final Matcher START_SUBSEL  = Pattern.compile("\\(").matcher("");
    final Matcher END_SUBSEL    = Pattern.compile("\\)").matcher("");
    final Matcher KEYWORD       = Pattern.compile("\\*|all|none|protein|mc|sc|base|alpha|beta|nitrogen|carbon|oxygen|sulfur|phosphorus|hydrogen|metal|polar|nonpolar|charged|donor|acceptor|aromatic|methyl|het|water|dna|rna").matcher("");
    final Matcher WITHIN        = Pattern.compile("within").matcher("");
    final Matcher OF            = Pattern.compile("of").matcher("");
    final Matcher COMMA         = Pattern.compile(",").matcher("");
    final Matcher MINUS_SIGN    = Pattern.compile("-").matcher("");
    final Matcher REAL          = RegexTokenMatcher.UNSIGNED_REAL.matcher("");
    
    final Pattern[] toIgnore = {
        RegexTokenMatcher.HASH_COMMENT,
        RegexTokenMatcher.WHITESPACE
    };
    
    final Matcher WORD          = Pattern.compile("[A-Za-z_]+").matcher("");
    final Matcher OPERATOR      = Pattern.compile("[!&*()|,-]").matcher("");

    final Pattern[] toAccept = {
        // No possible input subsequence should match more than one of these!
        WORD.pattern(),
        OPERATOR.pattern(),
        RegexTokenMatcher.UNSIGNED_REAL
    };
//}}}

//{{{ CLASS: DummySelection
//##############################################################################
    /** To be used in places where we haven't implemented things yet. */
    static class DummySelection extends Selection
    {
        String name;
        
        public DummySelection(String name)
        {
            super();
            this.name = name;
        }
        
        protected boolean selectImpl(AtomState as)
        {
            System.err.println("*** Warning: using dummy object ["+name+"] in selection attempt!");
            return false;
        }
        
        public String toString()
        { return "["+name+"]"; }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    TokenWindow t;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SelectionParser()
    {
        super();
    }
//}}}

//{{{ parse
//##############################################################################
    public Selection parse(String expr) throws ParseException, IOException
    { return parse(new CharWindow(expr)); }
    
    /**
    * Parses a text expression into a Selection object.
    */
    public Selection parse(CharWindow expr) throws ParseException, IOException
    {
        TokenMatcher tokenMatcher = new RegexTokenMatcher(
            RegexTokenMatcher.joinPatterns(toAccept),
            RegexTokenMatcher.joinPatterns(toIgnore)
        );
        t = new TokenWindow(expr, tokenMatcher);
        
        Selection s = loose_or();
        if(s == null)
            throw t.syntaxError("Empty expression!");
        if(t.token() != null)
            throw t.syntaxError("Unexpected token at end of expression ["+t.token()+"]");
        return s;
    }
//}}}

//{{{ loose_or, loose_and
//##############################################################################
    Selection loose_or() throws ParseException, IOException
    {
        Selection s = loose_and();
        if(s == null) return null;
        if(t.accept(LOOSE_OR))
        {
            OrTerm or = new OrTerm(s);
            do {
                s = loose_and();
                if(s == null)
                    throw t.syntaxError("Incomplete OR expression");
                else or.add(s);
            } while(t.accept(LOOSE_OR));
            return or;
        }
        else return s;
    }

    Selection loose_and() throws ParseException, IOException
    {
        Selection s = tight_and();
        if(s == null) return null;
        if(t.accept(LOOSE_AND))
        {
            AndTerm and = new AndTerm(s);
            do {
                s = tight_and();
                if(s == null)
                    throw t.syntaxError("Incomplete AND expression");
                else and.add(s);
            } while(t.accept(LOOSE_AND));
            return and;
        }
        else return s;
    }
//}}}

//{{{ tight_and, tight_or
//##############################################################################
    Selection tight_and() throws ParseException, IOException
    {
        Selection s = tight_or();
        if(s == null) return null;
        Selection r = tight_or();
        if(r == null) return s;
        else
        {
            AndTerm and = new AndTerm(s);
            while(r != null)
            {
                and.add(r);
                r = tight_or();
            }
            return and;
        }
    }
    
    Selection tight_or() throws ParseException, IOException
    {
        Selection s = not_expr();
        if(s == null) return null;
        if(t.accept(TIGHT_OR))
        {
            OrTerm or = new OrTerm(s);
            do {
                s = not_expr();
                if(s == null)
                    throw t.syntaxError("Incomplete OR expression");
                else or.add(s);
            } while(t.accept(TIGHT_OR));
            return or;
        }
        else return s;
    }
//}}}

//{{{ not_expr, subexpr
//##############################################################################
    Selection not_expr() throws ParseException, IOException
    {
        boolean doNot = t.accept(NOT_SYMBOL);
        Selection s = subexpr();
        if(s == null) s = simple_expr();
        
        if(s == null && doNot)  throw t.syntaxError("Incomplete NOT expression");
        else if(s == null)      return null;
        else if(doNot)          return new NotTerm(s);
        else                    return s;
    }

    Selection subexpr() throws ParseException, IOException
    {
        if(t.accept(START_SUBSEL))
        {
            Selection s = loose_or();
            if(s == null)
                throw t.syntaxError("Empty subexpression!");
            t.require(END_SUBSEL);
            return s;
        }
        else return null;
    }
//}}}

//{{{ simple_expr
//##############################################################################
    Selection simple_expr() throws ParseException, IOException
    {
        if(t.accept(KEYWORD)) return new DummySelection(KEYWORD.group());
        else if(t.accept(WITHIN)) return within();
        else return null;
    }
//}}}

//{{{ within
//##############################################################################
    Selection within() throws ParseException, IOException
    {
        double dist = real();
        t.require(OF);
        Selection s = subexpr();
        if(s != null)
        {
            return new DummySelection("within-selection");
        }
        else
        {
            double x = real();
            t.accept(COMMA); // optional
            double y = real();
            t.accept(COMMA); // optional
            double z = real();
            return new DummySelection("within-xyz");
        }
    }
//}}}

//{{{ real
//##############################################################################
    double real() throws ParseException, IOException
    {
        int sign = 1;
        if(t.accept(MINUS_SIGN)) sign = -1;
        t.require(REAL);
        try { return sign * Double.parseDouble(REAL.group()); }
        catch(NumberFormatException ex) { throw new ParseException("Unexpected difficulty parsing real number ["+t.token()+"]", 0); }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ main -- for testing
//##############################################################################
    static public void main(String[] args)
    {
        SelectionParser p = new SelectionParser();
        PrintStream out = System.out;
        try
        {
            test_ok("mc");
            test_ok("mc sc");
            test_ok(" mc  sc ");
            test_ok("mc & sc & het");
            test_ok("mc,sc");
            test_ok("mc or sc and not het");
            test_ok("mc,sc !het");
            test_ok("(mc,sc) within 8 of 1, 2.3, 4.5 not within 10 of (het | metal)");
            test_fail("");
            test_fail("()");
            test_fail("(");
            test_fail("(&)");
            test_fail("!&");
            test_fail("within 42 of (!)");
            test_fail("mc or ");
            
            out.println();
            out.println("=== All tests passed! ===");
            out.println();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    static private void test_ok(String expr) throws IOException, ParseException
    {
        SelectionParser p = new SelectionParser();
        PrintStream out = System.out;
        out.println("'"+expr+"' = "+p.parse(expr));
    }
    
    static private void test_fail(String expr) throws IOException
    {
        try
        {
            test_ok(expr);
            throw new RuntimeException("Bad expression parsed OK: '"+expr+"'");
        }
        catch(ParseException ex)
        {
            PrintStream out = System.out;
            out.println("unparsable (as expected): '"+expr+"'");
            out.println(ex.getMessage());
        }
    }
}//class

