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
* <li>measurement &rarr; distance | angle | dihedral</li>
* <li>distance &rarr; ("distance" | "dist") label atomspec atomspec</li>
* <li>angle &rarr; "angle" label atomspec atomspec atomspec</li>
* <li>dihedral &rarr; ("dihedral" | "torsion") label atomspec atomspec atomspec atomspec</li>
* <li>label &rarr; [A-Za-z0-9_.-]+</li>
* <li>atomspec &rarr; resno? atomname</li>
* <li>resno &rarr; "i" | "i+" [1-9] | "i-" [1-9]</li>
* <li>atomname &rarr; [_A-Z0-9*']{4}</li>
* </ul>
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 15 11:18:34 EST 2007
*/
public class Parser //extends ... implements ...
{
//{{{ Constants
    static final Pattern DISTANCE   = Pattern.compile("dist(ance)?");
    static final Pattern ANGLE      = Pattern.compile("angle");
    static final Pattern DIHEDRAL   = Pattern.compile("dihedral|torsion");
    static final Pattern LABEL      = Pattern.compile("[A-Za-z0-9_.-]+");
    static final Pattern RESNO      = Pattern.compile("i|i(-[1-9])|i\\+([1-9])");
    static final Pattern ATOMNAME   = Pattern.compile("[_A-Z0-9*']{4}");
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

//{{{ parse, nextToken, accept, matches
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
    
    boolean accept(Pattern symbol)
    {
        if(token == null) return false;
        else if(symbol.matcher(token).matches())
        {
            nextToken();
            return true;
        }
        else return false;
    }
    
    boolean matches(Pattern symbol)
    {
        if(token == null) return false;
        else return symbol.matcher(token).matches();
    }
//}}}

//{{{ measurement
//##############################################################################
    Measurement measurement() throws ParseException
    {
        if(accept(DISTANCE))
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
        if(matches(LABEL)) return nextToken();
        else throw new ParseException("Expected descriptive label", 0);
    }
//}}}

//{{{ atomspec
//##############################################################################
    AtomSpec atomspec() throws ParseException
    {
        int resOffset = 0;
        if(matches(RESNO))
        {
            Matcher m = RESNO.matcher(nextToken());
            m.matches(); // by definition of accept()
            String grp = m.group(1);
            if(grp == null) grp = m.group(2);
            if(grp != null)
            {
                try { resOffset = Integer.parseInt(grp); }
                catch(NumberFormatException ex) { throw new ParseException("Unexpected difficulty parsing residue number!", 0); }
            }
        }
        if(matches(ATOMNAME))
        {
            AtomSpec a = new AtomSpec(
                resOffset,
                nextToken()
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

