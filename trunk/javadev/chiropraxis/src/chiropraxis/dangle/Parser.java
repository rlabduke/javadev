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
//}}}
/**
* <code>Parser</code> decodes a simple grammar for specifying measurements in molecules:
* <ul>
* <li>expression &rarr; measurement*</li>
* <li>measurement &rarr; builtin | distance | angle | dihedral</li>
* <li>builtin &rarr; "phi" | "psi" | "omega" | "chi1" | "chi2" | "chi3" | "chi4" | "tau" | "alpha" | "beta" | "gamma" | "delta" | "epsilon" | "zeta" | "eta" | "theta"</li>
* <li>distance &rarr; ("distance" | "dist") label atomspec atomspec</li>
* <li>angle &rarr; "angle" label atomspec atomspec atomspec</li>
* <li>dihedral &rarr; ("dihedral" | "torsion") label atomspec atomspec atomspec atomspec</li>
* <li>label &rarr; [A-Za-z0-9_.-]+</li>
* <li>atomspec &rarr; resno? atomname</li>
* <li>resno &rarr; "i" | "i+" [1-9] | "i-" [1-9]</li>
* <li>atomname &rarr; [_A-Z0-9*']{4} | "/" regex "/"</li>
* <li>regex &rarr; <i>a java.util.regex regular expression; use _ instead of spaces.</i></li>
* </ul>
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 15 11:18:34 EST 2007
*/
public class Parser //extends ... implements ...
{
//{{{ Constants
    // If you add built-ins here, you should also modify
    // Measurement.newBuiltin(), the javadoc above, and the man page.
    final Matcher BUILTIN   = Pattern.compile("phi|psi|omega|chi1|chi2|chi3|chi4|tau|alpha|beta|gamma|delta|epsilon|zeta|eta|theta").matcher("");
    final Matcher DISTANCE  = Pattern.compile("dist(ance)?").matcher("");
    final Matcher ANGLE     = Pattern.compile("angle").matcher("");
    final Matcher DIHEDRAL  = Pattern.compile("dihedral|torsion").matcher("");
    final Matcher LABEL     = Pattern.compile("[A-Za-z0-9_.-]+").matcher("");
    final Matcher RESNO     = Pattern.compile("i|i(-[1-9])|i\\+([1-9])").matcher("");
    final Matcher ATOMNAME  = Pattern.compile("[_A-Z0-9*']{4}|/[^/ ]*/").matcher("");
//}}}

//{{{ Variable definitions
//##############################################################################
    StringTokenizer tokens;
    String token;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Parser()
    {
        super();
    }
//}}}

//{{{ parse, nextToken, accept
//##############################################################################
    /**
    * Parses a text expression into a set of Measurements.
    * @return a Collection of Measurement objects defined by the expression.
    */
    public Collection parse(String expr) throws ParseException
    {
        Collection meas = new ArrayList();
        tokens = new StringTokenizer(expr, ",; \t\n\r\f");
        nextToken();
        while(token != null) meas.add(measurement());
        return meas;
    }
    
    /** Returns the current (soon-to-be previous) token and advances to the next one */
    String nextToken()
    {
        String oldToken = token;
        if(!tokens.hasMoreTokens())
            token = null;
        else
            token = tokens.nextToken();
        return oldToken;
    }
    
    boolean accept(Matcher symbol)
    {
        if(token == null) return false;
        else if(symbol.reset(token).matches())
        {
            nextToken();
            return true;
        }
        else return false;
    }
    
    //boolean matches(Matcher symbol)
    //{
    //    if(token == null) return false;
    //    else return symbol.reset(token).matches();
    //}
//}}}

//{{{ measurement
//##############################################################################
    Measurement measurement() throws ParseException
    {
        if(accept(BUILTIN))
        {
            return Measurement.newBuiltin(BUILTIN.group());
        }
        else if(accept(DISTANCE))
        {
            return Measurement.newDistance(
                label(),
                atomspec(),
                atomspec()
            );
        }
        else if(accept(ANGLE))
        {
            return Measurement.newAngle(
                label(),
                atomspec(),
                atomspec(),
                atomspec()
            );
        }
        else if(accept(DIHEDRAL))
        {
            return Measurement.newDihedral(
                label(),
                atomspec(),
                atomspec(),
                atomspec(),
                atomspec()
            );
        }
        else throw new ParseException("Expected 'distance', 'angle', or 'dihedral'", 0);
    }
//}}}

//{{{ label
//##############################################################################
    String label() throws ParseException
    {
        if(accept(LABEL)) return LABEL.group();
        else throw new ParseException("Expected descriptive label", 0);
    }
//}}}

//{{{ atomspec
//##############################################################################
    AtomSpec atomspec() throws ParseException
    {
        int resOffset = 0;
        if(accept(RESNO))
        {
            String grp = RESNO.group(1);
            if(grp == null) grp = RESNO.group(2);
            if(grp != null)
            {
                try { resOffset = Integer.parseInt(grp); }
                catch(NumberFormatException ex) { throw new ParseException("Unexpected difficulty parsing residue number!", 0); }
            }
        }
        if(accept(ATOMNAME))
        {
            AtomSpec a = new AtomSpec(
                resOffset,
                ATOMNAME.group()
            );
            return a;
        }
        else throw new ParseException("Expected atom name", 0);
    }
//}}}

//{{{ main -- for testing
//##############################################################################
    static public void main(String[] args)
    {
        Parser p = new Parser();
        for(int i = 0; i < args.length; i++)
        {
            try
            {
                Collection meas = p.parse(args[i]);
                for(Iterator iter = meas.iterator(); iter.hasNext(); )
                    System.out.print(" "+iter.next()+" ;");
                System.out.println();
            }
            catch(ParseException ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

