// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.dangle;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>Measurement</code> is a set of AtomSpecs and a type of measurement
* to make among them -- distance, angle, dihedral, etc.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 15 11:18:34 EST 2007
*/
abstract public class Measurement //extends ... implements ...
{
//{{{ Constants
    public static final Object TYPE_UNKNOWN     = "unknown";
    public static final Object TYPE_DISTANCE    = "distance";
    public static final Object TYPE_ANGLE       = "angle";
    public static final Object TYPE_DIHEDRAL    = "dihedral";
    public static final Object TYPE_MAXB        = "maxb";
    public static final Object TYPE_MINQ        = "minq";
//}}}

//{{{ Variable definitions
//##############################################################################
    String label;
    double mean = Double.NaN;
    double sigma = Double.NaN;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Measurement(String label)
    {
        super();
        this.label = label;
    }
//}}}

//{{{ measure, getLabel/Type
//##############################################################################
    /**
    * Returns the specified measure in the given state,
    * or NaN if the measure could not be computed
    * (usually because 1+ atoms/residues don't exist).
    * @return the measure, or NaN if undefined
    */
    abstract public double measure(Model model, ModelState state, Residue res);
    
    public String getLabel()
    { return label; }
    
    /** Returns one of the TYPE_* constants. */
    public Object getType()
    { return TYPE_UNKNOWN; }
//}}}

//{{{ setMeanAndSigma, getDeviation, toStringIdeal
//##############################################################################
    /**
    * Sets the mean value and (expected) standard deviation for this measure,
    * if applicable.
    * @return this, for chaining
    */
    public Measurement setMeanAndSigma(double mean, double sigma)
    {
        this.mean = mean;
        this.sigma = sigma;
        return this;
    }
    
    /**
    * Returns the deviation of measure from the mean in standard-deviation units (sigmas).
    * If any of the values involved are NaN, returns NaN.
    */
    public double getDeviation(double measure)
    {
        // If any values are NaN, result will be NaN too.
        return (measure - mean) / sigma;
    }
    
    protected String toStringIdeal()
    {
        if(!Double.isNaN(mean) && !Double.isNaN(sigma))
            return " ideal "+mean+" "+sigma;
        else
            return "";
    }
//}}}

//{{{ newSuperBuiltin
//##############################################################################
    static public Measurement[] newSuperBuiltin(String label)
    {
        // If you add super-builtins here, you should also modify
        // Parser.SUPERBLTN, the Parser javadoc, and the man page.
        if("rnabb".equals(label))
            return new Measurement[] {
                newBuiltin("alpha"),
                newBuiltin("beta"),
                newBuiltin("gamma"),
                newBuiltin("delta"),
                newBuiltin("epsilon"),
                newBuiltin("zeta")
            };
        else return null;
    }
//}}}

//{{{ newBuiltin
//##############################################################################
    static public Measurement newBuiltin(String label)
    {
        // If you add built-ins here, you should also modify
        // Parser.BUILTIN, the Parser javadoc, and the man page.
        //{{{ proteins
        if("phi".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_C__"),
                new AtomSpec( 0, "_N__"),
                new AtomSpec( 0, "_CA_"),
                new AtomSpec( 0, "_C__")
            );
        else if("psi".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_N__"),
                new AtomSpec( 0, "_CA_"),
                new AtomSpec( 0, "_C__"),
                new AtomSpec( 1, "_N__")
            );
        // Same definition as Dang: named for the first residue in the peptide
        else if("omega".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_CA_"),
                new AtomSpec( 0, "_C__"),
                new AtomSpec( 1, "_N__"),
                new AtomSpec( 1, "_CA_")
            );
        else if("chi1".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_N__"),
                new AtomSpec( 0, "_CA_"),
                new AtomSpec( 0, "_CB_"),
                new AtomSpec( 0, "/_[ACNOS]G[_1]/")
            );
        else if("chi2".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_CA_"),
                new AtomSpec( 0, "_CB_"),
                new AtomSpec( 0, "/_[ACNOS]G[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]D[_1]/")
            );
        else if("chi3".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_CB_"),
                new AtomSpec( 0, "/_[ACNOS]G[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]D[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]E[_1]/")
            );
        else if("chi4".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "/_[ACNOS]G[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]D[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]E[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]Z[_1]/")
            );
        else if("tau".equals(label))
            return new Angle(label,
                new AtomSpec( 0, "_N__"),
                new AtomSpec( 0, "_CA_"),
                new AtomSpec( 0, "_C__")
            );
        //}}} proteins
        //{{{ nucleic acids
        else if("alpha".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_O3*"),
                new AtomSpec( 0, "_P__"),
                new AtomSpec( 0, "_O5*"),
                new AtomSpec( 0, "_C5*")
            );
        else if("beta".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_P__"),
                new AtomSpec( 0, "_O5*"),
                new AtomSpec( 0, "_C5*"),
                new AtomSpec( 0, "_C4*")
            );
        else if("gamma".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_O5*"),
                new AtomSpec( 0, "_C5*"),
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 0, "_C3*")
            );
        else if("delta".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_C5*"),
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_O3*")
            );
        else if("epsilon".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_O3*"),
                new AtomSpec( 1, "_P__")
            );
        else if("zeta".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_O3*"),
                new AtomSpec( 1, "_P__"),
                new AtomSpec( 1, "_O5*")
            );
        else if("eta".equals(label)) // virtual!
            return new Dihedral(label,
                new AtomSpec(-1, "_C4*"),
                new AtomSpec( 0, "_P__"),
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 1, "_P__")
            );
        else if("theta".equals(label)) // virtual!
            return new Dihedral(label,
                new AtomSpec( 0, "_P__"),
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 1, "_P__"),
                new AtomSpec( 1, "_C4*")
            );
        else if("chi".equals(label))
            return new Group(
                new Dihedral(label, // A, G
                    new AtomSpec( 0, "_O4*"),
                    new AtomSpec( 0, "_C1*"),
                    new AtomSpec( 0, "_N9_"),
                    new AtomSpec( 0, "_C4_")
            )).add(
                new Dihedral(label, // C, T, U
                    new AtomSpec( 0, "_O4*"),
                    new AtomSpec( 0, "_C1*"),
                    new AtomSpec( 0, "_N1_"),
                    new AtomSpec( 0, "_C2_")
            ));
        //}}} nucleic acids
        //{{{ nucleic acids, i-1
        else if("alpha-1".equals(label))
            return new Dihedral(label,
                new AtomSpec(-2, "_O3*"),
                new AtomSpec(-1, "_P__"),
                new AtomSpec(-1, "_O5*"),
                new AtomSpec(-1, "_C5*")
            );
        else if("beta-1".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_P__"),
                new AtomSpec(-1, "_O5*"),
                new AtomSpec(-1, "_C5*"),
                new AtomSpec(-1, "_C4*")
            );
        else if("gamma-1".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_O5*"),
                new AtomSpec(-1, "_C5*"),
                new AtomSpec(-1, "_C4*"),
                new AtomSpec(-1, "_C3*")
            );
        else if("delta-1".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_C5*"),
                new AtomSpec(-1, "_C4*"),
                new AtomSpec(-1, "_C3*"),
                new AtomSpec(-1, "_O3*")
            );
        else if("epsilon-1".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_C4*"),
                new AtomSpec(-1, "_C3*"),
                new AtomSpec(-1, "_O3*"),
                new AtomSpec( 0, "_P__")
            );
        else if("zeta-1".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_C3*"),
                new AtomSpec(-1, "_O3*"),
                new AtomSpec( 0, "_P__"),
                new AtomSpec( 0, "_O5*")
            );
        else if("chi-1".equals(label))
            return new Group(
                new Dihedral(label, // A, G
                    new AtomSpec(-1, "_O4*"),
                    new AtomSpec(-1, "_C1*"),
                    new AtomSpec(-1, "_N9_"),
                    new AtomSpec(-1, "_C4_")
            )).add(
                new Dihedral(label, // C, T, U
                    new AtomSpec(-1, "_O4*"),
                    new AtomSpec(-1, "_C1*"),
                    new AtomSpec(-1, "_N1_"),
                    new AtomSpec(-1, "_C2_")
            ));
        //}}} nucleic acids, i-1
        else return null;
    }
//}}}

//{{{ newDistance
//##############################################################################
    static public Measurement newDistance(String label, AtomSpec a, AtomSpec b)
    { return new Distance(label, a, b); }
    
    static class Distance extends Measurement
    {
        AtomSpec a, b;
        
        public Distance(String label, AtomSpec a, AtomSpec b)
        { super(label); this.a = a; this.b = b; }
        
        public double measure(Model model, ModelState state, Residue res)
        {
            AtomState aa = a.get(model, state, res);
            AtomState bb = b.get(model, state, res);
            if(aa == null || bb == null)
                return Double.NaN;
            else return new Triple(aa).distance(bb);
        }
        
        public String toString()
        { return "distance "+getLabel()+" "+a+", "+b+toStringIdeal(); }
        
        public Object getType()
        { return TYPE_DISTANCE; }
    }
//}}}

//{{{ newAngle
//##############################################################################
    static public Measurement newAngle(String label, AtomSpec a, AtomSpec b, AtomSpec c)
    { return new Angle(label, a, b, c); }
    
    static class Angle extends Measurement
    {
        AtomSpec a, b, c;
        
        public Angle(String label, AtomSpec a, AtomSpec b, AtomSpec c)
        { super(label); this.a = a; this.b = b; this.c = c; }
        
        public double measure(Model model, ModelState state, Residue res)
        {
            AtomState aa = a.get(model, state, res);
            AtomState bb = b.get(model, state, res);
            AtomState cc = c.get(model, state, res);
            if(aa == null || bb == null || cc == null)
                return Double.NaN;
            else return Triple.angle(aa, bb, cc);
        }
        
        public String toString()
        { return "angle "+getLabel()+" "+a+", "+b+", "+c+toStringIdeal(); }
        
        public Object getType()
        { return TYPE_ANGLE; }
    }
//}}}

//{{{ newDihedral
//##############################################################################
    static public Measurement newDihedral(String label, AtomSpec a, AtomSpec b, AtomSpec c, AtomSpec d)
    { return new Dihedral(label, a, b, c, d); }
    
    static class Dihedral extends Measurement
    {
        AtomSpec a, b, c, d;

        public Dihedral(String label, AtomSpec a, AtomSpec b, AtomSpec c, AtomSpec d)
        { super(label); this.a = a; this.b = b; this.c = c; this.d = d; }
        
        public double measure(Model model, ModelState state, Residue res)
        {
            AtomState aa = a.get(model, state, res);
            AtomState bb = b.get(model, state, res);
            AtomState cc = c.get(model, state, res);
            AtomState dd = d.get(model, state, res);
            if(aa == null || bb == null || cc == null || dd == null)
                return Double.NaN;
            else return Triple.dihedral(aa, bb, cc, dd);
        }

        public String toString()
        { return "dihedral "+getLabel()+" "+a+", "+b+", "+c+", "+d; }
        
        public Object getType()
        { return TYPE_DIHEDRAL; }
    }
//}}}

//{{{ newMaxB
//##############################################################################
    static public Measurement newMaxB(String label, AtomSpec a)
    { return new MaxB(label, a); }
    
    static class MaxB extends Measurement
    {
        AtomSpec a;
        
        public MaxB(String label, AtomSpec a)
        { super(label); this.a = a; }
        
        public double measure(Model model, ModelState state, Residue res)
        {
            Collection atoms = a.getAll(model, state, res);
            if(atoms.isEmpty()) return Double.NaN;
            double max = Double.NEGATIVE_INFINITY;
            for(Iterator iter = atoms.iterator(); iter.hasNext(); )
            {
                AtomState aa = (AtomState) iter.next();
                max = Math.max(max, aa.getTempFactor());
            }
            return max;
        }
        
        public String toString()
        { return "maxb "+getLabel()+" "+a; }
        
        public Object getType()
        { return TYPE_MAXB; }
    }
//}}}

//{{{ newMinQ
//##############################################################################
    static public Measurement newMinQ(String label, AtomSpec a)
    { return new MinQ(label, a); }
    
    static class MinQ extends Measurement
    {
        AtomSpec a;
        
        public MinQ(String label, AtomSpec a)
        { super(label); this.a = a; }
        
        public double measure(Model model, ModelState state, Residue res)
        {
            Collection atoms = a.getAll(model, state, res);
            if(atoms.isEmpty()) return Double.NaN;
            double min = Double.POSITIVE_INFINITY;
            for(Iterator iter = atoms.iterator(); iter.hasNext(); )
            {
                AtomState aa = (AtomState) iter.next();
                min = Math.min(min, aa.getOccupancy());
            }
            return min;
        }
        
        public String toString()
        { return "minq "+getLabel()+" "+a; }
        
        public Object getType()
        { return TYPE_MINQ; }
    }
//}}}

//{{{ CLASS: Group
//##############################################################################
    /** Allows for 1+ measurements to be evaluated in series, returning the first valid result. */
    static public class Group extends Measurement
    {
        Collection group = new ArrayList();
        Object type;
        
        public Group(Measurement first)
        {
            super(first.getLabel());
            group.add(first);
            this.type = first.getType();
        }
        
        /** Returns this for easy chaining. */
        public Group add(Measurement next)
        {
            group.add(next);
            if(this.type != next.getType())
                this.type = TYPE_UNKNOWN;
            return this;
        }
        
        public double measure(Model model, ModelState state, Residue res)
        {
            for(Iterator iter = group.iterator(); iter.hasNext(); )
            {
                Measurement m = (Measurement) iter.next();
                double val = m.measure(model, state, res);
                if(!Double.isNaN(val)) return val;
            }
            return Double.NaN;
        }
        
        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            for(Iterator iter = group.iterator(); iter.hasNext(); )
            {
                if(buf.length() > 0) buf.append(" ; ");
                buf.append(iter.next());
            }
            return buf.toString();
        }
        
        public Object getType()
        { return type; }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

