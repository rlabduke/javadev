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
    final Matcher CHAIN         = Pattern.compile("chain([A-Za-z0-9_])").matcher("");
    final Matcher RESNAME       = Pattern.compile("res([A-Za-z0-9_]{3})").matcher("");
    final Matcher ATOM          = Pattern.compile("atom([A-Za-z0-9_]{4})").matcher("");
    final Matcher RESNUM        = Pattern.compile("("+RegexTokenMatcher.SIGNED_INT+")([A-Z])?").matcher("");
    final Matcher RESRANGE      = Pattern.compile(RESNUM.pattern()+"-"+RESNUM.pattern()).matcher("");
    final Matcher RANGE_OP      = Pattern.compile("-|to").matcher("");
    final Matcher WITHIN        = Pattern.compile("within").matcher("");
    final Matcher OF            = Pattern.compile("of").matcher("");
    final Matcher COMMA         = Pattern.compile(",").matcher("");
    final Matcher REAL          = RegexTokenMatcher.SIGNED_REAL.matcher("");
    
    final Pattern[] toIgnore = {
        RegexTokenMatcher.HASH_COMMENT,
        RegexTokenMatcher.WHITESPACE
    };
    
    // Categories for tokenizer rather than parser:
    final Matcher WORD          = Pattern.compile("[A-Za-z_]+").matcher("");
    final Matcher OPERATOR      = Pattern.compile("[!&*()|,-]").matcher("");
    final Matcher REAL_NOT_INT;
    {
        // Modified from RegexTokenMatcher to ensure we don't match ints!
        String sign         = "(?:[+-]?)";
        String digits       = "(?:[0-9]+)";
        String positive     = "(?:[1-9][0-9]*)";
        String natural      = "(?:0|"+positive+")";
        String integer      = "(?:"+sign+natural+")";
        String u_real       = "(?:"+natural+"(?:\\.(?:"+digits+")?)?)";
        String u_real_dec   = "(?:"+natural+"\\.(?:"+digits+")?)";  // requires decimal point
        String u_real_e     = "(?:"+u_real+"[eE]"+integer+")";      // requires exponent
        String s_real_noint = "(?:"+sign+"(?:"+u_real_dec+"|"+u_real_e+"))";
        
        REAL_NOT_INT        = Pattern.compile(s_real_noint).matcher("");
    }

    final Pattern[] toAccept = {
        // No possible input prefix should match more than one of these!
        // However, we can't manage that because of the residue number range grammar.
        // So, order matters here:  first patterns will be matched preferentially.
        WORD.pattern(),
        RESRANGE.pattern(),     // can start with "-"
        // Both REAL and RESNUM can match a string that starts with an int, e.g. -1.234:
        // but RESNUM stops at the decimal point, leaving behind ".234" as a bad token.
        // But consider "1A to 2B":  opposite problem, left with "A" and "B" hanging.
        // So we first specifically match reals with a decimal point or exponent,
        // then ints with a possible trailing insertion code.
        REAL_NOT_INT.pattern(), // can start with "-", catches decimals/exponents only
        RESNUM.pattern(),       // can start with "-", catches insertion codes and ints
        REAL.pattern(),         // can start with "-", catches nothing?
        OPERATOR.pattern(),     // can start with "-" (equal to "-", actually)
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
        if(t.accept(KEYWORD))           return KeywordTerm.get(KEYWORD.group());
        else if(t.accept(CHAIN))        return new ChainTerm(CHAIN.group(1));
        else if(t.accept(RESNAME))      return new ResNameTerm(RESNAME.group(1));
        else if(t.accept(ATOM))         return new AtomTerm(ATOM.group(1));
        else if(t.accept(RESRANGE))     return res_range();
        else if(t.accept(RESNUM))       return res_num();
        else if(t.accept(WITHIN))       return within();
        else                            return null;
    }
//}}}

//{{{ res_range, res_num
//##############################################################################
    Selection res_range() throws ParseException, IOException
    {
        try
        {
            int num1 = Integer.parseInt(RESRANGE.group(1));
            int num2 = Integer.parseInt(RESRANGE.group(3));
            String ins1 = RESRANGE.group(2);
            String ins2 = RESRANGE.group(4);
            if(ins1 == null) ins1 = " ";
            if(ins2 == null) ins2 = " ";
            return new DummySelection("range "+num1+ins1+" to "+num2+ins2);
        }
        catch(NumberFormatException ex) { throw new ParseException("Unexpected difficulty parsing integer ["+t.token()+"]", 0); }
    }
    
    Selection res_num() throws ParseException, IOException
    {
        try
        {
            int num1 = Integer.parseInt(RESNUM.group(1));
            String ins1 = RESNUM.group(2);
            if(ins1 == null) ins1 = " ";
            if(t.accept(RANGE_OP))
            {
                t.require(RESNUM);
                int num2 = Integer.parseInt(RESNUM.group(1));
                String ins2 = RESNUM.group(2);
                if(ins2 == null) ins2 = " ";
                return new DummySelection("range "+num1+ins1+" to "+num2+ins2);
            }
            else return new DummySelection("single res "+num1+ins1);
        }
        catch(NumberFormatException ex) { throw new ParseException("Unexpected difficulty parsing integer ["+t.token()+"]", 0); }
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
            return new WithinSelectionTerm(dist, s);
        }
        else
        {
            double x = real();
            t.accept(COMMA); // optional
            double y = real();
            t.accept(COMMA); // optional
            double z = real();
            return new WithinPointTerm(dist, x, y, z);
        }
    }
//}}}

//{{{ real
//##############################################################################
    double real() throws ParseException, IOException
    {
        t.require(REAL);
        try { return Double.parseDouble(REAL.group()); }
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
            test_fail("");
            test_fail("()");
            test_fail("(");
            test_fail("(&)");
            test_fail("!&");
            test_fail("within 42 of (!)");
            test_fail("all or ");
            test_fail("1 - - 2"); // != "1--2", minus must immediately preceed number.
            
            test_ok("all");
            test_ok("all none");
            test_ok(" all  none ");
            test_ok("all & none & het");
            test_ok("all,none");
            test_ok("all or none and not het");
            test_ok("all,none !het");
            test_ok("(all,none) within 8 of 1, 2.3, 4.5 not within 10 of (het | none)");
            test_ok("atom_CA_ chainB,het");
            test_ok("resGLY,resPRO atom_N__,atom_CA_,atom_C__,atom_O__");
            test_ok("1-2 -1 -2 -1--2 -1-2 -11A--12B -5 6"); // residue numbers and ranges
            test_ok("1 - 2 -1 - -2 3 --4C 5 to 6 7D to 8E");
            
            out.println();
            out.println("=== All tests passed! ===");
            out.println();
            
            //final Pattern[] toIgnore = {
            //    RegexTokenMatcher.HASH_COMMENT,
            //    RegexTokenMatcher.WHITESPACE
            //};
            //final Matcher WORD          = Pattern.compile("[A-Za-z_]+").matcher("");
            //final Matcher OPERATOR      = Pattern.compile("[!&*()|,-]").matcher("");
            //final Pattern[] toAccept = {
            //    // No possible input subsequence should match more than one of these!
            //    WORD.pattern(),
            //    Pattern.compile("("+RegexTokenMatcher.SIGNED_INT+")([A-Z])?-("+RegexTokenMatcher.SIGNED_INT+")([A-Z])?"),
            //    RegexTokenMatcher.SIGNED_REAL,
            //    RegexTokenMatcher.SIGNED_INT,
            //    OPERATOR.pattern()
            //};
            //TokenMatcher tokenMatcher = new RegexTokenMatcher(
            //    RegexTokenMatcher.joinPatterns(toAccept),
            //    RegexTokenMatcher.joinPatterns(toIgnore)
            //    );
            //String expr = "1-2 -1 -2 -1--2 -1-2 -11A--12B -5 5 5.6";
            //TokenWindow t = new TokenWindow(new CharWindow(expr), tokenMatcher);
            //out.println(expr);
            //while(t.token() != null)
            //{
            //    out.println(t.token());
            //    t.advance();
            //}
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

