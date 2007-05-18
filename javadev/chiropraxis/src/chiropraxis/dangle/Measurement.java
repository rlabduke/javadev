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
    public static final Object TYPE_PLANARITY   = "planarity";
//}}}

//{{{ Variable definitions
//##############################################################################
    ResSpec resSpec = null;
    String label;
    double mean = Double.NaN;
    double sigma = Double.NaN;
    double deviation = Double.NaN;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Measurement(String label)
    {
        super();
        this.label = label;
    }
//}}}

//{{{ measure, getDeviation, measureImpl, getLabel/Type, setResSpec
//##############################################################################
    /**
    * Returns the specified measure in the given state,
    * or NaN if the measure could not be computed
    * (usually because 1+ atoms/residues don't exist).
    * @return the measure, or NaN if undefined
    */
    public double measure(Model model, ModelState state, Residue res)
    {
        double measure;
        if(resSpec == null || resSpec.isMatch(model, state, res))
            measure = measureImpl(model, state, res);
        else
            measure = Double.NaN;
        this.deviation = (measure - mean) / sigma;
        return measure;
    }
    
    /**
    * Returns the deviation from the mean in standard-deviation units (sigmas)
    * for the last call to measure().
    * If any of the values involved are NaN, returns NaN.
    */
    public double getDeviation()
    { return deviation; }

    abstract protected double measureImpl(Model model, ModelState state, Residue res);
    
    public String getLabel()
    { return label; }
    
    /** Returns one of the TYPE_* constants. */
    public Object getType()
    { return TYPE_UNKNOWN; }
    
    public void setResSpec(ResSpec resSpec)
    { this.resSpec = resSpec; }
//}}}

//{{{ setMeanAndSigma, toString, toStringImpl
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
    
    public String toString()
    {
        return (resSpec == null ? "" : resSpec+" ")
            + toStringImpl()
            + (!Double.isNaN(mean) && !Double.isNaN(sigma) ? " ideal "+mean+" "+sigma : "");
    }
    
    abstract protected String toStringImpl();
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
        else if("cbdev".equals(label))
            return new Distance(label,
                new AtomSpec( 0, "_CB_"),
                new XyzSpec.IdealTetrahedral(
                    new AtomSpec(0, "_N__"),
                    new AtomSpec(0, "_C__"),
                    new AtomSpec(0, "_CA_"),
                    1.536, 110.4, 110.6, 123.1, -123.0
            ));
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
    static public Measurement newDistance(String label, XyzSpec a, XyzSpec b)
    { return new Distance(label, a, b); }
    
    static class Distance extends Measurement
    {
        XyzSpec a, b;
        
        public Distance(String label, XyzSpec a, XyzSpec b)
        { super(label); this.a = a; this.b = b; }
        
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
            Tuple3 aa = a.get(model, state, res);
            Tuple3 bb = b.get(model, state, res);
            if(aa == null || bb == null)
                return Double.NaN;
            else return new Triple(aa).distance(bb);
        }
        
        protected String toStringImpl()
        { return "distance "+getLabel()+" "+a+", "+b; }
        
        public Object getType()
        { return TYPE_DISTANCE; }
    }
//}}}

//{{{ newAngle
//##############################################################################
    static public Measurement newAngle(String label, XyzSpec a, XyzSpec b, XyzSpec c)
    { return new Angle(label, a, b, c); }
    
    static class Angle extends Measurement
    {
        XyzSpec a, b, c;
        
        public Angle(String label, XyzSpec a, XyzSpec b, XyzSpec c)
        { super(label); this.a = a; this.b = b; this.c = c; }
        
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
            Tuple3 aa = a.get(model, state, res);
            Tuple3 bb = b.get(model, state, res);
            Tuple3 cc = c.get(model, state, res);
            if(aa == null || bb == null || cc == null)
                return Double.NaN;
            else return Triple.angle(aa, bb, cc);
        }
        
        protected String toStringImpl()
        { return "angle "+getLabel()+" "+a+", "+b+", "+c; }
        
        public Object getType()
        { return TYPE_ANGLE; }
    }
//}}}

//{{{ newDihedral
//##############################################################################
    static public Measurement newDihedral(String label, XyzSpec a, XyzSpec b, XyzSpec c, XyzSpec d)
    { return new Dihedral(label, a, b, c, d); }
    
    static class Dihedral extends Measurement
    {
        XyzSpec a, b, c, d;

        public Dihedral(String label, XyzSpec a, XyzSpec b, XyzSpec c, XyzSpec d)
        { super(label); this.a = a; this.b = b; this.c = c; this.d = d; }
        
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
            Tuple3 aa = a.get(model, state, res);
            Tuple3 bb = b.get(model, state, res);
            Tuple3 cc = c.get(model, state, res);
            Tuple3 dd = d.get(model, state, res);
            if(aa == null || bb == null || cc == null || dd == null)
                return Double.NaN;
            else return Triple.dihedral(aa, bb, cc, dd);
        }

        protected String toStringImpl()
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
        
        protected double measureImpl(Model model, ModelState state, Residue res)
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
        
        protected String toStringImpl()
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
        
        protected double measureImpl(Model model, ModelState state, Residue res)
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
        
        protected String toStringImpl()
        { return "minq "+getLabel()+" "+a; }
        
        public Object getType()
        { return TYPE_MINQ; }
    }
//}}}

//{{{ CLASS: Planarity
//##############################################################################
    public static class Planarity extends Measurement
    {
        Collection<XyzSpec> specs = new ArrayList();
        
        public Planarity(String label)
        { super(label); }
        
        /** @return this, for chaining */
        public Planarity add(XyzSpec spec)
        {
            specs.add(spec);
            return this;
        }
        
        /** This is an O(n^6) runtime, O(n^3) storage algorithm -- don't call with much more than ~12 atoms! */
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
            // Angle limits on which triangles we will accept --
            // long, skinny triangle are unsteady and "roll" side-to-side.
            final double cosWide = Math.cos(Math.toRadians(130));
            final double cosNarrow = Math.cos(Math.toRadians(25));
            
            long time = System.currentTimeMillis();
            // Convert AtomSpecs and XyzSpecs into coordinates.
            Collection<Tuple3> all = new ArrayList();
            for(XyzSpec spec : specs)
            {
                if(spec instanceof AtomSpec)
                {
                    Collection<Tuple3> t = ((AtomSpec) spec).getAll(model, state, res);
                    if(t.isEmpty()) return Double.NaN;
                    else all.addAll(t);
                }
                else
                {
                    Tuple3 t = spec.get(model, state, res);
                    if(t == null) return Double.NaN;
                    else all.add(t);
                }
            }
            
            // For every possible trio of coordinates, calculate a unit normal.
            final int len = all.size();
            Tuple3[] t = all.toArray(new Tuple3[len]);
            Collection<Triple> normals = new ArrayList();
            Triple w1 = new Triple(), w2 = new Triple(), w3 = new Triple();
            for(int i = 0; i < len; i++)
            for(int j = i+1; j < len; j++)
            for(int k = j+1; k < len; k++)
            {
                w1.likeVector(t[i], t[j]).unit();
                w2.likeVector(t[j], t[k]).unit();
                w3.likeVector(t[k], t[i]).unit();
                
                // Discard triangles with too-wide or too-narrow angles:
                double d1 = -w1.dot(w2), d2 = -w2.dot(w3), d3 = -w3.dot(w1);
                if(d1 < cosWide || d1 > cosNarrow) continue;
                if(d2 < cosWide || d2 > cosNarrow) continue;
                if(d3 < cosWide || d3 > cosNarrow) continue;
                
                w1.likeCross(w1, w2).unit();
                normals.add(new Normal(t[i], t[j], t[k], w1));
            }
            
            // For every possible pair of normals, calculate a dot product.
            // Find the dot product closest to zero.
            double minDot = 1; // == 0 degree angle
            Triple mostNormal1 = null, mostNormal2 = null;
            for(Triple ni : normals)
            {
                for(Triple nj : normals)
                {
                    double dot = Math.abs(ni.dot(nj));
                    if(dot <= minDot)
                    {
                        minDot = dot;
                        mostNormal1 = ni;
                        mostNormal2 = nj;
                    }
                }
            }
            
            // Return the angle, in degrees
            // acos returns NaN sometimes when we're
            // too close to an angle of 0 or 180 (if |minDot| > 1)
            double ret = Math.toDegrees(Math.acos( minDot ));
            if(Double.isNaN(ret)) ret = (minDot>=0.0 ? 0.0 : 180.0);
            //System.err.println("  runtime for "+len+" points, "+normals.size()+" normals: "+(System.currentTimeMillis() - time)+" ms");
            //System.err.println("  angle = "+ret+" for "+mostNormal1+" :: "+mostNormal2);
            return ret;
        }
        
        private static class Normal extends Triple
        {
            // Uncomment these lines for help debugging:
            //Object a, b, c;
            
            public Normal(Object a, Object b, Object c, Tuple3 t)
            {
                super(t);
                //this.a = a; this.b = b; this.c = c;
            }
            
            public String toString()
            {
                return super.toString()
                    //+" "+a+" "+b+" "+c
                ;
            }
        }
        
        protected String toStringImpl()
        {
            StringBuffer buf = new StringBuffer("planarity "+getLabel()+" (");
            boolean first = true;
            for(XyzSpec spec : specs)
            {
                if(first) first = false;
                else buf.append(", ");
                buf.append(spec);
            }
            buf.append(")");
            return buf.toString();
        }
        
        public Object getType()
        { return TYPE_PLANARITY; }
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
        
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
            for(Iterator iter = group.iterator(); iter.hasNext(); )
            {
                Measurement m = (Measurement) iter.next();
                double val = m.measure(model, state, res);
                if(!Double.isNaN(val))
                {
                    // So deviation will be calc'd correctly
                    this.setMeanAndSigma(m.mean, m.sigma);
                    return val;
                }
            }
            return Double.NaN;
        }
        
        protected String toStringImpl()
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

