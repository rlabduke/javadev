// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.dangle;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.parser.*;
//}}}
/**
* <code>Parser</code> decodes a simple grammar for specifying measurements in molecules:
* <ul>
* <li>expression &rarr; measurement_for*</li>
* <li>measurement_for &rarr; ("for" resspec)? measurement</li>
* <li>resspec &rarr; resno? "cis"? ([_A-Z0-9]{3} | "/" regex "/")</li>
* <li>measurement &rarr; super_builtin | builtin | distance | angle | dihedral | maxb | minq | cbdev</li>
* <li>super_builtin &rarr; "rnabb"</li>
* <li>builtin &rarr; "phi" | "psi" | "omega" | "chi1" | "chi2" | "chi3" | "chi4" | "tau" | "alpha" | "beta" | "gamma" | "delta" | "epsilon" | "zeta" | "eta" | "theta" | "chi" | "alpha-1" | "beta-1" | "gamma-1" | "delta-1" | "epsilon-1" | "zeta-1" | "chi-1"</li>
* <li>distance &rarr; ("distance" | "dist") label atomspec atomspec ideal_clause?</li>
* <li>angle &rarr; "angle" label atomspec atomspec atomspec ideal_clause?</li>
* <li>dihedral &rarr; ("dihedral" | "torsion") label atomspec atomspec atomspec atomspec</li>
* <li>maxb &rarr; "maxb" label atomspec</li>
* <li>minq &rarr; ("minq" | "mino" | "minocc") label atomspec</li>
* <li>cbdev &rarr; "cbdev"</li>
* <li>label &rarr; [A-Za-z0-9*'_.-]+</li>
* <li>atomspec &rarr; resno? atomname</li>
* <li>resno &rarr; "i" | "i+" [1-9] | "i-" [1-9]</li>
* <li>atomname &rarr; [_A-Z0-9*']{4} | "/" regex "/"</li>
* <li>regex &rarr; <i>a java.util.regex regular expression; use _ instead of spaces.</i></li>
* <li>ideal_clause &rarr; "ideal" mean sigma</li>
* <li>mean &rarr; realnum</li>
* <li>sigma &rarr; realnum</li>
* <li>realnum &rarr; <i>a real number parseable by Double.parseDouble()</i></li>
* </ul>
*
* <p>To specify things like the nucleic acid sidechain dihedral "chi", which
* requires alternant *sets* of atom names, simply write two (or more) measurement
* definitions that have the same name (label).
* They will be tried in order until one works (i.e. all the atoms are found).
* The problem with using regexps in this case is that all nucleic acids
* have C2 and C4 atoms, so there's no guarantee that only N9--C4 and N1--C2
* bonds are considered, and not N9--C2 or N1--C4.
*
* <p>Comments start with the hash character (#) and extend to end-of-line.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 15 11:18:34 EST 2007
*/
public class Parser //extends ... implements ...
{
//{{{ Constants
    final Matcher FOR       = Pattern.compile("for").matcher("");
    final Matcher CIS       = Pattern.compile("cis").matcher("");
    final Matcher RESNAME   = Pattern.compile("[_A-Z0-9]{3}|/[^/ ]*/").matcher("");
    // If you add super-builtins here, you should also modify
    // Measurement.newSuperBuiltin(), the javadoc above, and the man page.
    final Matcher SUPERBLTN = Pattern.compile("rnabb").matcher("");
    // If you add built-ins here, you should also modify
    // Measurement.newBuiltin(), the javadoc above, and the man page.
    final Matcher BUILTIN   = Pattern.compile("phi|psi|omega|chi1|chi2|chi3|chi4|tau|alpha|beta|gamma|delta|epsilon|zeta|eta|theta|chi|alpha-1|beta-1|gamma-1|delta-1|epsilon-1|zeta-1|chi-1").matcher("");
    final Matcher DISTANCE  = Pattern.compile("dist(ance)?").matcher("");
    final Matcher ANGLE     = Pattern.compile("angle").matcher("");
    final Matcher DIHEDRAL  = Pattern.compile("dihedral|torsion").matcher("");
    final Matcher MAXB      = Pattern.compile("maxb", Pattern.CASE_INSENSITIVE).matcher("");
    final Matcher MINQ      = Pattern.compile("minq|mino|minocc", Pattern.CASE_INSENSITIVE).matcher("");
    final Matcher CBDEV     = Pattern.compile("cbdev").matcher("");
    final Matcher LABEL     = Pattern.compile("[A-Za-z0-9*'_.-]+").matcher("");
    final Matcher RESNO     = Pattern.compile("i|i(-[1-9])|i\\+([1-9])").matcher("");
    final Matcher ATOMNAME  = Pattern.compile("[_A-Z0-9*']{4}|/[^/ ]*/").matcher("");
    final Matcher IDEAL     = Pattern.compile("ideal").matcher("");
    //final Matcher REALNUM   = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?(0|[1-9][0-9]*)(\\.[0-9]+)?)?").matcher("");
    final Matcher REALNUM   = RegexTokenMatcher.SIGNED_REAL.matcher("");
    
    final Pattern[] toIgnore = {
        RegexTokenMatcher.WHITESPACE,
        Pattern.compile("[,;]+"),
        RegexTokenMatcher.HASH_COMMENT
    };
    
    final Pattern[] toAccept = {
        // The superset of other patterns. *Don't* do RESNAME|ATOMNAME, as RESNAME will match atoms, but is too short!
        LABEL.pattern(),
        RegexTokenMatcher.SLASH_QUOTED_STRING,
        RegexTokenMatcher.SIGNED_REAL
    };
//}}}

//{{{ Variable definitions
//##############################################################################
    TokenWindow t;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Parser()
    {
        super();
    }
//}}}

//{{{ parse
//##############################################################################
    /**
    * Parses a text expression into a set of Measurements.
    * @return a Collection of Measurement objects defined by the expression.
    */
    public Collection parse(CharWindow expr) throws ParseException, IOException
    {
        TokenMatcher tokenMatcher = new RegexTokenMatcher(
            RegexTokenMatcher.joinPatterns(toAccept),
            RegexTokenMatcher.joinPatterns(toIgnore)
        );
        t = new TokenWindow(expr, tokenMatcher);
        
        UberMap meas = new UberMap();
        //tokens = new StringTokenizer(expr, ",; \t\n\r\f");
        //nextToken();
        while(t.token() != null)
        {
            Measurement[] newMs = measurement_for();
            for(int i = 0; i < newMs.length; i++)
            {
                // If two measurements are defined with the same name, set them up as alternates.
                Measurement newM = newMs[i];
                Measurement oldM = (Measurement) meas.get(newM.getLabel());
                if(oldM == null)
                    meas.put(newM.getLabel(), newM);
                else if(oldM instanceof Measurement.Group)
                    ((Measurement.Group) oldM).add(newM);
                else
                {
                    Measurement.Group group = new Measurement.Group(oldM).add(newM);
                    meas.put(group.getLabel(), group);
                }
            }
        }
        return meas.values();
    }
//}}}

//{{{ measurement_for
//##############################################################################
    Measurement[] measurement_for() throws ParseException, IOException
    {
        ResSpec resSpec = null;
        if(t.accept(FOR))
            resSpec = resspec();
        
        Measurement[] m = measurement();
        if(resSpec != null)
        {
            for(int i = 0; i < m.length; i++)
                m[i].setResSpec(resSpec);
        }
        return m;
    }
//}}}

//{{{ resspec
//##############################################################################
    ResSpec resspec() throws ParseException, IOException
    {
        int resOffset = 0;
        if(t.accept(RESNO))
        {
            String grp = RESNO.group(1);
            if(grp == null) grp = RESNO.group(2);
            if(grp != null)
            {
                try { resOffset = Integer.parseInt(grp); }
                catch(NumberFormatException ex) { throw new ParseException("Unexpected difficulty parsing residue number ["+t.token()+"]", 0); }
            }
        }
        boolean requireCis = t.accept(CIS);
        if(t.accept(RESNAME))
        {
            ResSpec r = new ResSpec(
                resOffset,
                requireCis,
                RESNAME.group()
            );
            return r;
        }
        else throw t.syntaxError("Expected residue name ["+t.token()+"]");
    }
//}}}

//{{{ measurement
//##############################################################################
    Measurement[] measurement() throws ParseException, IOException
    {
        if(t.accept(SUPERBLTN))
        {
            return Measurement.newSuperBuiltin(SUPERBLTN.group());
        }
        else if(t.accept(BUILTIN))
        {
            return new Measurement[] {Measurement.newBuiltin(BUILTIN.group())};
        }
        else if(t.accept(DISTANCE))
        {
            Measurement m = Measurement.newDistance(
                label(),
                atomspec(),
                atomspec()
            );
            if(t.accept(IDEAL))
                m.setMeanAndSigma(realnum(), realnum());
            return new Measurement[] {m};
        }
        else if(t.accept(ANGLE))
        {
            Measurement m = Measurement.newAngle(
                label(),
                atomspec(),
                atomspec(),
                atomspec()
            );
            if(t.accept(IDEAL))
                m.setMeanAndSigma(realnum(), realnum());
            return new Measurement[] {m};
        }
        else if(t.accept(DIHEDRAL))
        {
            return new Measurement[] {Measurement.newDihedral(
                label(),
                atomspec(),
                atomspec(),
                atomspec(),
                atomspec()
            )};
        }
        else if(t.accept(MAXB))
        {
            return new Measurement[] {Measurement.newMaxB(
                label(),
                atomspec()
            )};
        }
        else if(t.accept(MINQ))
        {
            return new Measurement[] {Measurement.newMinQ(
                label(),
                atomspec()
            )};
        }
        else if(t.accept(CBDEV))
        {
            return new Measurement[] {Measurement.newCbDev(
                CBDEV.group()
            )};
        }
        else throw t.syntaxError("Expected measurement type ('distance', 'angle', 'dihedral', etc) ["+t.token()+"]");
    }
//}}}

//{{{ label
//##############################################################################
    String label() throws ParseException, IOException
    {
        if(t.accept(LABEL)) return LABEL.group();
        else throw t.syntaxError("Expected descriptive label ["+t.token()+"]");
    }
//}}}

//{{{ atomspec
//##############################################################################
    AtomSpec atomspec() throws ParseException, IOException
    {
        int resOffset = 0;
        if(t.accept(RESNO))
        {
            String grp = RESNO.group(1);
            if(grp == null) grp = RESNO.group(2);
            if(grp != null)
            {
                try { resOffset = Integer.parseInt(grp); }
                catch(NumberFormatException ex) { throw new ParseException("Unexpected difficulty parsing residue number ["+t.token()+"]", 0); }
            }
        }
        if(t.accept(ATOMNAME))
        {
            AtomSpec a = new AtomSpec(
                resOffset,
                ATOMNAME.group()
            );
            return a;
        }
        else throw t.syntaxError("Expected atom name ["+t.token()+"]");
    }
//}}}

//{{{ realnum
//##############################################################################
    double realnum() throws ParseException, IOException
    {
        if(t.accept(REALNUM))
        {
            try { return Double.parseDouble(REALNUM.group()); }
            catch(NumberFormatException ex) { throw new ParseException("Unexpected difficulty parsing real number ["+t.token()+"]", 0); }
        }
        else throw t.syntaxError("Expected real number ["+t.token()+"]");
    }
//}}}

//{{{ main -- for testing
//##############################################################################
    static public void main(String[] args)
    {
        Parser p = new Parser();
        try
        {
            Collection meas = p.parse(new CharWindow(System.in));
            for(Iterator iter = meas.iterator(); iter.hasNext(); )
                System.out.print(" "+iter.next()+" ;");
            System.out.println();
        }
        catch(ParseException ex) { ex.printStackTrace(); }
        catch(IOException ex) { ex.printStackTrace(); }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

