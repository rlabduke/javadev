// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.dangle;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>XyzSpec</code> represents any Dangle expression that results in
* a position in 3-space:  either an atom, or some point calculated from atoms.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 15 11:18:34 EST 2007
*/
abstract public class XyzSpec //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ get, toString
//##############################################################################
    /**
    * @return the coordinates of this point, or null if it can't be calculated.
    */
    abstract public Tuple3 get(Model model, ModelState state, Residue curr);
    
    /**
    * @return a representation of this point that can be parsed again by Dangle
    */
    abstract public String toString();
//}}}

//{{{ class: Average
//##############################################################################
    /** Computes the (unweighted) average position of a group of atoms. */
    static public class Average extends XyzSpec
    {
        Collection<XyzSpec> specs = new ArrayList();
        
        /** @return this, for chaining */
        public Average add(XyzSpec spec)
        {
            specs.add(spec);
            return this;
        }
        
        public Tuple3 get(Model model, ModelState state, Residue curr)
        {
            Triple avg = new Triple();
            for(XyzSpec spec : specs)
            {
                Tuple3 t = spec.get(model, state, curr);
                if(t == null) return null;
                else avg.add(t);
            }
            avg.div( specs.size() );
            return avg;
        }
        
        public String toString()
        {
            StringBuffer buf = new StringBuffer("avg( ");
            for(XyzSpec spec : specs)
            {
                buf.append(spec);
                buf.append(" ");
            }
            buf.append(")");
            return buf.toString();
        }
    }
//}}}

//{{{ class: IdealTetrahedral
//##############################################################################
    /**
    * Projects the ideal position of an atom given tetrahedral geometry.
    * This is the calculation used for C-beta deviation.
    */
    static public class IdealTetrahedral extends XyzSpec
    {
        XyzSpec left, right, center;
        double length, angle1, angle2, dihedral1, dihedral2;
        
        /**
        * @param length     center - ideal
        * @param angle1     right - center - ideal
        * @param angle2     left - center - ideal
        * @param dihedral1  left - right - center - ideal
        * @param dihedral2  right - left - center - ideal
        */
        public IdealTetrahedral(XyzSpec left, XyzSpec right, XyzSpec center,
            double length, double angle1, double angle2, double dihedral1, double dihedral2)
        {
            super();
            this.left       = left;
            this.right      = right;
            this.center     = center;
            this.length     = length;
            this.angle1     = angle1;
            this.angle2     = angle2;
            this.dihedral1  = dihedral1;
            this.dihedral2  = dihedral2;
        }
        
        public Tuple3 get(Model model, ModelState state, Residue curr)
        {
            // See chiropraxis.sc.SidechainIdealizer
            Triple t1, t2, ideal = new Triple();
            Builder build = new Builder();
            
            Tuple3 l = left.get(model, state, curr);
            Tuple3 r = right.get(model, state, curr);
            Tuple3 c = center.get(model, state, curr);
            if(l == null || r == null || c == null)
                return null;
            
            // Construct from either side, take average position
            t1 = build.construct4(l, r, c, length, angle1, dihedral1);
            t2 = build.construct4(r, l, c, length, angle2, dihedral2);
            ideal.likeMidpoint(t1, t2);
            
            // Re-normalize bond length
            ideal.sub(c).unit().mult(length).add(c);
            
            return ideal;
        }
        
        public String toString()
        {
            return "idealtet("+left+", "+right+", "+center+", "+length
                +", "+angle1+", "+angle2+", "+dihedral1+", "+dihedral2+")";
        }
    }
//}}}

//{{{ class: Vector
//##############################################################################
    /** Returns the vector between two points */
    static public class Vector extends XyzSpec
    {
        XyzSpec from, to;
        
        public Vector(XyzSpec from, XyzSpec to)
        {
            super();
            this.from = from;
            this.to = to;
        }
        
        public Tuple3 get(Model model, ModelState state, Residue curr)
        {
            Tuple3 f = from.get(model, state, curr);
            Tuple3 t = to.get(model, state, curr);
            if(f == null || t == null)
                return null;
            return new Triple().likeVector(f, t);
        }
        
        public String toString()
        {
            return "vector("+from+", "+to+")";
        }
    }
//}}}

//{{{ class: Normal
//##############################################################################
    /** Computes the unit normal vector for the least-squares-fit plane through a group of atoms. */
    static public class Normal extends XyzSpec
    {
        Collection<XyzSpec> specs = new ArrayList();
        
        /** @return this, for chaining */
        public Normal add(XyzSpec spec)
        {
            specs.add(spec);
            return this;
        }
        
        public Tuple3 get(Model model, ModelState state, Residue curr)
        {
            // Convert AtomSpecs and XyzSpecs into coordinates.
            Collection<Tuple3> all = new ArrayList();
            for(XyzSpec spec : specs)
            {
                if(spec instanceof AtomSpec)
                {
                    Collection<Tuple3> t = ((AtomSpec) spec).getAll(model, state, curr);
                    if(t.isEmpty()) return null;
                    else all.addAll(t);
                }
                else
                {
                    Tuple3 t = spec.get(model, state, curr);
                    if(t == null) return null;
                    else all.add(t);
                }
            }
            if(all.size() < 3) return null;
            return new LsqPlane(all).getNormal();
        }
        
        public String toString()
        {
            StringBuffer buf = new StringBuffer("normal( ");
            for(XyzSpec spec : specs)
            {
                buf.append(spec);
                buf.append(" ");
            }
            buf.append(")");
            return buf.toString();
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

