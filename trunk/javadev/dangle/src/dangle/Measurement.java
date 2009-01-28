// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package dangle;

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
    public static final Object TYPE_V_ANGLE     = "vector_angle";
    public static final Object TYPE_MAXB        = "maxb";
    public static final Object TYPE_MINQ        = "minq";
    public static final Object TYPE_PLANARITY   = "planarity";
    public static final Object TYPE_PUCKER   	= "pucker";
    public static final Object TYPE_BASEPPERP  	= "basePperp";
    public static final Object TYPE_ISPREPRO  	= "isprepro";
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

//{{{ measure
//##############################################################################
    /**
    * Returns the specified measure in the given state,
    * or NaN if the measure could not be computed
    * (usually because 1+ atoms/residues don't exist).
    * @return the measure, or NaN if undefined
    */
    public double measure(Model model, ModelState state, Residue res, boolean doHetsInGeneral)
    {
        // Wouldn't want to give deviations for molecules not described by the 
        // distribution this code is using, so check for hets and/or DNA.
        if (!doHetsInGeneral && !isProtOrNucAcid(res))
        {
            Collection<Atom> thisResiduesAtoms = res.getAtoms();
            for(Iterator iter = thisResiduesAtoms.iterator(); iter.hasNext(); )
                if( ((Atom)iter.next()).isHet() )
                {
                    this.deviation = Double.NaN;
                    return Double.NaN;
                }
            //{{{ old
            //Iterator iter = thisResiduesAtoms.iterator();
            //while (iter.hasNext())
            //{
            //    Atom atom = (Atom) iter.next();
            //    if (atom.isHet())
            //    {
            //        this.deviation = Double.NaN;
            //        return Double.NaN;
            //    }
            //}
            //}}}
        }
        
        // Proceed with measurement for this residue
        double measure;
        if(resSpec == null || resSpec.isMatch(model, state, res))
            measure = measureImpl(model, state, res);
        else
            measure = Double.NaN;
        this.deviation = (measure - mean) / sigma;
        return measure;
    }
    
    /**
    * Utility method -- redirects to the above 'measure' method
    */
    public double measure(Model model, ModelState state, Residue res)
    {
        return measure(model, state, res, false);
    }
//}}}

//{{{ getDeviation, measureImpl, getLabel/Type, setResSpec, isProtOrNucAcid
//##############################################################################
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
    
    public boolean isProtOrNucAcid(Residue res)
    {
        //String lowerCa = ":gly:ala:val:phe:pro:met:ile:leu:asp:glu:lys:arg:ser:thr:tyr:his:cys:asn:gln:trp:asx:glx:ace:for:nh2:nme:mse:aib:abu:pca:mly:cyo:m3l:dgn:csd:";
        String aaNames = ":GLY:ALA:VAL:PHE:PRO:MET:ILE:LEU:ASP:GLU:LYS:ARG:SER:THR:TYR:HIS:CYS:ASN:GLN:TRP:ASX:GLX:ACE:FOR:NH2:NME:MSE:AIB:ABU:PCA:MLY:CYO:M3L:DGN:CSD:";
        String naNames = ":  C:  G:  A:  T:  U:CYT:GUA:ADE:THY:URA:URI:CTP:CDP:CMP:GTP:GDP:GMP:ATP:ADP:AMP:TTP:TDP:TMP:UTP:UDP:UMP:GSP:H2U:PSU:4SU:1MG:2MG:M2G:5MC:5MU:T6A:1MA:RIA:OMC:OMG: YG:  I:7MG:YYG:C  :G  :A  :T  :U  :YG :I  : rC: rG: rA: rT: rU: dC: dG: dA: dT: dU: DC: DG: DA: DT: DU:";
        
        String resname = res.getName();
        if (aaNames.indexOf(resname) != -1 || naNames.indexOf(resname) != -1) 
            return true; // it's a valid protein or nucleic acid residue name
        return false;
    }
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
            + (!Double.isNaN(mean)  && !Double.isNaN(sigma)  ? " ideal "+mean+" "+sigma : "");
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
            
            return new Measurement[] 
            {
                newBuiltin("alpha"),
                newBuiltin("beta"),
                newBuiltin("gamma"),
                newBuiltin("delta"),
                newBuiltin("epsilon"),
                newBuiltin("zeta"),
                //newBuiltin("c2o2")      // added 7/31/07 -- DK
            };
        if ("suitefit".equals(label))  	// added 6/20/07 -- DK
	        return new Measurement[] 
            {
                newBuiltin("O5'-C5'"),
                newBuiltin("C5'-C4'"),
                newBuiltin("C4'-C3'"),
                newBuiltin("C3'-C2'"),
                newBuiltin("C2'-C1'"),
                newBuiltin("O4'-C1'"),
                newBuiltin("O4'-C4'"),
                newBuiltin("O3'-C3'"),
                newBuiltin("C2'-O2'"),
                newBuiltin("C3'-C4'-O4'"),
                newBuiltin("C4'-O4'-C1'"),
                newBuiltin("O4'-C1'-C2'"),
                newBuiltin("C1'-C2'-C3'"),
                newBuiltin("C4'-C3'-C2'"),
                newBuiltin("C3'-C2'-C1'"),
                newBuiltin("C2'-C1'-O4'"),
                newBuiltin("C1'-O4'-C4'"),
                newBuiltin("O3'-C3'-C4'"),
                newBuiltin("C3'-C4'-C5'"),
                newBuiltin("delta"),
                newBuiltin("C3'-C4'-O4'-C1'"),
                newBuiltin("C4'-O4'-C1'-C2'"),
                newBuiltin("O4'-C1'-C2'-C3'"),
                newBuiltin("C4'-C3'-C2'-C1'"),
                newBuiltin("C3'-C2'-C1'-O4'"),
                newBuiltin("C2'-C1'-O4'-C4'"),
                newBuiltin("O3'-C4'-C3'-C2'"),
                newBuiltin("C5'-C3'-C4'-O4'")
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
        // NEED TO EDIT REGEX FOR 'SE  ', 2ND-TO-LAST ATOM OF SELENOMETHIONINE (MSE)
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
        else if("isprepro".equals(label))
            return new IsPrePro(label)
            ;
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
        else if("c2o2".equals(label)) // added 7/31/07 -- DK
            return new Distance(label,
                new AtomSpec( 0, "_C2*"),
                new AtomSpec( 0, "_O2*")
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
	 
	// Start of "suitefit" Builtins, added 6/20/07. -- DK
	// The 9 distances for suitefit:
	else if("O5'-C5'".equals(label) || "O5'--C5'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_O5*"),
                new AtomSpec( 0, "_C5*")
	    );
	else if("C5'-C4'".equals(label) || "C5'--C4'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_C5*"),
                new AtomSpec( 0, "_C4*")
	    );
	else if("C4'-C3'".equals(label) || "C4'--C3'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 0, "_C3*")
	    );
	else if("C3'-C2'".equals(label) || "C3'--C2'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_C2*")
	    );
	else if("C2'-C1'".equals(label) || "C2'--C1'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_C2*"),
                new AtomSpec( 0, "_C1*")
	    );
	else if("O4'-C1'".equals(label) || "O4'--C1'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_O4*"),
                new AtomSpec( 0, "_C1*")
	    );
	else if("O4'-C4'".equals(label) || "O4'--C4'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_O4*"),
                new AtomSpec( 0, "_C4*")
	    );
	else if("O3'-C3'".equals(label) || "O3'--C3'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_O3*"),
                new AtomSpec( 0, "_C3*")
	    );
	else if("C2'-O2'".equals(label) || "C2'--O2'".equals(label)) 
            return new Distance(label,
                new AtomSpec( 0, "_C2*"),
                new AtomSpec( 0, "_O2*")
	    );
	// The 10 angles for suitefit:
	else if("C3'-C4'-O4'".equals(label))
            return new Angle(label,
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_C4*"),
		new AtomSpec( 0, "_O4*")
	    );
	else if("C4'-O4'-C1'".equals(label))
            return new Angle(label,
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 0, "_O4*"),
		new AtomSpec( 0, "_C1*")
	    );
	else if("O4'-C1'-C2'".equals(label)) 
            return new Angle(label,
                new AtomSpec( 0, "_O4*"),
                new AtomSpec( 0, "_C1*"),
		new AtomSpec( 0, "_C2*")
	    );
	else if("C1'-C2'-C3'".equals(label)) 
            return new Angle(label,
                new AtomSpec( 0, "_C1*"),
                new AtomSpec( 0, "_C2*"),
		new AtomSpec( 0, "_C3*")
	    );
	else if("C4'-C3'-C2'".equals(label)) 
            return new Angle(label,
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 0, "_C3*"),
		new AtomSpec( 0, "_C2*")
	    );
	else if("C3'-C2'-C1'".equals(label)) 
            return new Angle(label,
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_C2*"),
		new AtomSpec( 0, "_C1*")
	    );
	else if("C2'-C1'-O4'".equals(label)) 
            return new Angle(label,
                new AtomSpec( 0, "_C2*"),
                new AtomSpec( 0, "_C1*"),
		new AtomSpec( 0, "_O4*")
	    );
	else if("C1'-O4'-C4'".equals(label)) 
            return new Angle(label,
                new AtomSpec( 0, "_C1*"),
                new AtomSpec( 0, "_O4*"),
		new AtomSpec( 0, "_C4*")
	    );
	else if("O3'-C3'-C4'".equals(label)) 
            return new Angle(label,
                new AtomSpec( 0, "_O3*"),
                new AtomSpec( 0, "_C3*"),
		new AtomSpec( 0, "_C4*")
	    );
	else if("C3'-C4'-C5'".equals(label)) 
            return new Angle(label,
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_C4*"),
		new AtomSpec( 0, "_C5*")
	    );
	
	// The 8 dihedrals for suitefit (other than delta, which is already defined above):
	else if("C3'-C4'-O4'-C1'".equals(label)) 
            return new Dihedral(label,
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_C4*"),
		new AtomSpec( 0, "_O4*"),
		new AtomSpec( 0, "_C1*")
	    );
	else if("C4'-O4'-C1'-C2'".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 0, "_O4*"),
                new AtomSpec( 0, "_C1*"),
                new AtomSpec( 0, "_C2*")
            );
	else if("O4'-C1'-C2'-C3'".equals(label)) 
            return new Dihedral(label,
                new AtomSpec( 0, "_O4*"),
                new AtomSpec( 0, "_C1*"),
	 	new AtomSpec( 0, "_C2*"),
		new AtomSpec( 0, "_C3*")
	    );
	else if("C4'-C3'-C2'-C1'".equals(label)) 
            return new Dihedral(label,
                new AtomSpec( 0, "_C4*"),
                new AtomSpec( 0, "_C3*"),
	 	new AtomSpec( 0, "_C2*"),
		new AtomSpec( 0, "_C1*")
	    );
	else if("C3'-C2'-C1'-O4'".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_C3*"),
                new AtomSpec( 0, "_C2*"),
                new AtomSpec( 0, "_C1*"),
                new AtomSpec( 0, "_O4*")
            );
	else if("C2'-C1'-O4'-C4'".equals(label)) 
            return new Dihedral(label,
                new AtomSpec( 0, "_C2*"),
                new AtomSpec( 0, "_C1*"),
	 	new AtomSpec( 0, "_O4*"),
		new AtomSpec( 0, "_C4*")
	    );
	else if("O3'-C4'-C3'-C2'".equals(label)) 
            return new Dihedral(label,
                new AtomSpec( 0, "_O3*"),
                new AtomSpec( 0, "_C4*"),
	 	new AtomSpec( 0, "_C3*"),
		new AtomSpec( 0, "_C2*")
	    );
	else if("C5'-C3'-C4'-O4'".equals(label)) 
            return new Dihedral(label,
                new AtomSpec( 0, "_C5*"),
                new AtomSpec( 0, "_C3*"),
	 	new AtomSpec( 0, "_C4*"),
		new AtomSpec( 0, "_O4*")
	    );
	
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
            else if (a instanceof AtomSpec && b instanceof AtomSpec)
            {
                // aa, bb must be AtomStates from AtomSpec.get(...)
                TreeSet<String> alts = new TreeSet<String>();
                alts.add(((AtomState) aa).getAltConf());
                alts.add(((AtomState) bb).getAltConf());
                if (alts.size() > 1)    return Double.NaN;
            }
            return new Triple(aa).distance(bb);
        }
        
        protected String toStringImpl()
        { return "distance "+getLabel()+" "+a+", "+b; }
        
        public Object getType()
        { return TYPE_DISTANCE; }
        
        public XyzSpec getA()
        { return a; }
        
        public XyzSpec getB()
        { return b; }
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
            else if (a instanceof AtomSpec && b instanceof AtomSpec && 
                     c instanceof AtomSpec)
            {
                // aa, bb, cc must be AtomStates from AtomSpec.get(...)
                TreeSet<String> alts = new TreeSet<String>();
                alts.add(((AtomState) aa).getAltConf());
                alts.add(((AtomState) bb).getAltConf());
                alts.add(((AtomState) cc).getAltConf());
                if (alts.size() > 1)
                    return Double.NaN;
            }
            return Triple.angle(aa, bb, cc);
        }
        
        protected String toStringImpl()
        { return "angle "+getLabel()+" "+a+", "+b+", "+c; }
        
        public Object getType()
        { return TYPE_ANGLE; }
        
        public AtomSpec getA()
        { return (AtomSpec) a; }
        
        public AtomSpec getB()
        { return (AtomSpec) b; }
        
        public AtomSpec getC()
        { return (AtomSpec) c; }
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
            else if (a instanceof AtomSpec && b instanceof AtomSpec && 
                     c instanceof AtomSpec && d instanceof AtomSpec)
            {
                // aa, bb, cc, dd must be AtomStates from AtomSpec.get(...)
                TreeSet<String> alts = new TreeSet<String>();
                alts.add(((AtomState) aa).getAltConf());
                alts.add(((AtomState) bb).getAltConf());
                alts.add(((AtomState) cc).getAltConf());
                alts.add(((AtomState) dd).getAltConf());
                if (alts.size() > 1)    return Double.NaN;
            }
            return Triple.dihedral(aa, bb, cc, dd);
        }

        protected String toStringImpl()
        { return "dihedral "+getLabel()+" "+a+", "+b+", "+c+", "+d; }
        
        public Object getType()
        { return TYPE_DIHEDRAL; }
    }
//}}}

//{{{ newVectorAngle
//##############################################################################
    static public Measurement newVectorAngle(String label, XyzSpec a, XyzSpec b)
    { return new VectorAngle(label, a, b); }
    
    static class VectorAngle extends Measurement
    {
        XyzSpec a, b;
        
        public VectorAngle(String label, XyzSpec a, XyzSpec b)
        { super(label); this.a = a; this.b = b; }
        
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
            Tuple3 aa = a.get(model, state, res);
            Tuple3 bb = b.get(model, state, res);
            if(aa == null || bb == null)
                return Double.NaN;
            double angle = new Triple(aa).angle(bb);
            // Sign of plane normals is random, limit angle to [0, 90]
            if(angle > 90 && (a instanceof XyzSpec.Normal || b instanceof XyzSpec.Normal))
                angle = 180 - angle;
            return angle;
        }
        
        protected String toStringImpl()
        { return "vector_angle "+getLabel()+" "+a+", "+b; }
        
        public Object getType()
        { return TYPE_V_ANGLE; }
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
            
            // Actually, specifying both is unnecessary:
            // if the upper limit is 130, the lower limit is (180-130)/2 = 25;
            // if the lower limit is 25, the upper limit is (180 - 2*25) = 130.
            // I've left the code here because this function seems fast enough
            // and explicitly constraining both ends makes it easier to think about.
            
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

//{{{ CLASS: PuckerAng
//##############################################################################
    public static class PuckerAng extends Measurement
    {
        Dihedral v0;
	Dihedral v1;
	Dihedral v2;
	Dihedral v3;
	Dihedral v4;
	double v0dbl;
	double v1dbl;
	double v2dbl;
	double v3dbl;
	double v4dbl;
		    
	Collection<XyzSpec> specs = new ArrayList();
        
        public PuckerAng(String label)
        { super(label); }
	
	// I altered Ian's "chaining" method here -- DK
	/** @return this, for chaining */
        public PuckerAng add(XyzSpec spec)
        {
            specs.add(spec);
            return this;
        }
        
        /** This is an implementation of Eqn 3 in Altona JACS 1972. -- DK */
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
		
		//for (XyzSpec spec : specs)	
		//{
		//	if(spec instanceof AtomSpec)
		//		System.out.print(spec.toString() + "|");
		//}
			
		v0 = new Dihedral(label,
			new AtomSpec( 0, "_C4*"), 	// assume order of atoms is OK...
			new AtomSpec( 0, "_O4*"),
			new AtomSpec( 0, "_C1*"),
			new AtomSpec( 0, "_C2*"));
		v1 = new Dihedral(label,
			new AtomSpec( 0, "_O4*"),
			new AtomSpec( 0, "_C1*"),
			new AtomSpec( 0, "_C2*"),
			new AtomSpec( 0, "_C3*"));
		v2 = new Dihedral(label,
			new AtomSpec( 0, "_C1*"),
			new AtomSpec( 0, "_C2*"),
			new AtomSpec( 0, "_C3*"),
			new AtomSpec( 0, "_C4*"));
		v3 = new Dihedral(label,
			new AtomSpec( 0, "_C2*"),
			new AtomSpec( 0, "_C3*"),
			new AtomSpec( 0, "_C4*"),
			new AtomSpec( 0, "_O4*"));
		v4 = new Dihedral(label,
			new AtomSpec( 0, "_C3*"),
			new AtomSpec( 0, "_C4*"),
			new AtomSpec( 0, "_O4*"),
			new AtomSpec( 0, "_C1*"));
		
		v0dbl = v0.measureImpl(model, state, res);
		v1dbl = v1.measureImpl(model, state, res);
		v2dbl = v2.measureImpl(model, state, res);
		v3dbl = v3.measureImpl(model, state, res);
		v4dbl = v4.measureImpl(model, state, res);
		
		double numer = (v4dbl + v1dbl) - (v3dbl + v0dbl);
		double denom = (2 * v2dbl * ( 
			Math.sin(36.0/360*2*Math.PI) + 
			Math.sin(72.0/360*2*Math.PI)
			));
		double dihdP = Math.toDegrees(Math.atan(numer / denom));
		
		if (( v2dbl < 0 ) && ( !Double.isNaN(dihdP) ))
			return dihdP + 180;
		else
			return dihdP;
	}
        
        // deleted private static class Normal extends Triple -- DK
        
        protected String toStringImpl() // altered a bit from Planarity -- DK
        {
            StringBuffer buf = new StringBuffer("pucker " + getLabel());
	    
	    // deleted for(XyzSpec spec : specs) ..... stuff -- DK
            
            return buf.toString();
        }
        
        public Object getType()
        { return TYPE_PUCKER; }
    }
//}}}

//{{{ CLASS: PuckerAmp
//##############################################################################
    public static class PuckerAmp extends Measurement
    {
        Dihedral v0;
	Dihedral v1;
	Dihedral v2;
	Dihedral v3;
	Dihedral v4;
	double v0dbl;
	double v1dbl;
	double v2dbl;
	double v3dbl;
	double v4dbl;
	
	Collection<XyzSpec> specs = new ArrayList();
        
        public PuckerAmp(String label)
        { super(label); }
	
	// I deleted Ian's "chaining" method here -- DK
        
        /** This is an implementation of Eqn 12 in Rao ActaCryst 1981. -- DK */
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
		v0 = new Dihedral(label,
			new AtomSpec( 0, "_C4*"), 	// assume order of atoms is OK...
			new AtomSpec( 0, "_O4*"),
			new AtomSpec( 0, "_C1*"),
			new AtomSpec( 0, "_C2*"));
		v1 = new Dihedral(label,
			new AtomSpec( 0, "_O4*"),
			new AtomSpec( 0, "_C1*"),
			new AtomSpec( 0, "_C2*"),
			new AtomSpec( 0, "_C3*"));
		v2 = new Dihedral(label,
			new AtomSpec( 0, "_C1*"),
			new AtomSpec( 0, "_C2*"),
			new AtomSpec( 0, "_C3*"),
			new AtomSpec( 0, "_C4*"));
		v3 = new Dihedral(label,
			new AtomSpec( 0, "_C2*"),
			new AtomSpec( 0, "_C3*"),
			new AtomSpec( 0, "_C4*"),
			new AtomSpec( 0, "_O4*"));
		v4 = new Dihedral(label,
			new AtomSpec( 0, "_C3*"),
			new AtomSpec( 0, "_C4*"),
			new AtomSpec( 0, "_O4*"),
			new AtomSpec( 0, "_C1*"));
		
		v0dbl = v0.measureImpl(model, state, res);
		v1dbl = v1.measureImpl(model, state, res);
		v2dbl = v2.measureImpl(model, state, res);
		v3dbl = v3.measureImpl(model, state, res);
		v4dbl = v4.measureImpl(model, state, res);
		//For debugging:
		//System.out.println("v0dbl:"+v0dbl + " v1dbl:"+v1dbl + "v2dbl:"+v2dbl + "v3dbl:"+v3dbl + "v4dbl:"+v4dbl);
		
		double sum_i_squared = (v0dbl*v0dbl)+(v1dbl*v1dbl)+(v2dbl*v2dbl)+(v3dbl*v3dbl)+(v4dbl*v4dbl);
		double sum_i_iplus1 = (v0dbl*v1dbl)+(v1dbl*v2dbl)+(v2dbl*v3dbl)+(v3dbl*v4dbl)+(v4dbl*v0dbl);
		double sum_i_iplus2 = (v0dbl*v2dbl)+(v1dbl*v3dbl)+(v2dbl*v4dbl)+(v3dbl*v0dbl)+(v4dbl*v1dbl);
		//For debugging:
		//System.out.print("sum_i_squared: "+sum_i_squared+"\t");
		//System.out.print("sum_i_iplus1: "+sum_i_iplus1+"\t");
		//System.out.println("sum_i_iplus2: "+sum_i_iplus2+"\t");
		
		double tau_m_squared = ( Math.pow(2.0/5, 2) ) * 
		    ( sum_i_squared - 
		        ((1.0 + Math.sqrt(5)) / 2) * sum_i_iplus1 + 
		        ((-1.0 + Math.sqrt(5)) / 2) * sum_i_iplus2 
		    );
		double tau_m = Math.sqrt(tau_m_squared);
		return tau_m;
	}
        
        // deleted private static class Normal extends Triple -- DK
        
        protected String toStringImpl() // altered a bit from Planarity -- DK
        {
            StringBuffer buf = new StringBuffer("pucker " + getLabel());
	    
	    // deleted for(XyzSpec spec : specs) ..... stuff -- DK
            
            return buf.toString();
        }
        
        public Object getType()
        { return TYPE_PUCKER; }
    }
//}}}

//{{{ CLASS: BasePhosPerp
//##############################################################################
    public static class BasePhosPerp extends Measurement
    {
        public BasePhosPerp(String label)
        { super(label); }
        
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
            double pperpDist = Double.NaN;
            try
            {
                Residue next = res.getNext(model); // want 3' P (later in seq!)
                if (next != null)
                {
                    // Get relevant atom coords
                    Atom phos = next.getAtom(" P  ");
                    Atom carb =  res.getAtom(" C1'");
                    Atom nitr =  res.getAtom(" N9 ");
                    if (carb == null)   carb = res.getAtom(" C1*");
                    if (nitr == null)   nitr = res.getAtom(" N1 ");
                    AtomState p   = state.get(phos);
                    AtomState c1  = state.get(carb);
                    AtomState n19 = state.get(nitr);
                    // Draw appropriate vectors
                    Triple n19_p  = new Triple().likeVector(n19, p);
                    Triple n19_c1 = new Triple().likeVector(n19, c1);
                    // Get distance from N19 to the intersection point of the N19->C1
                    // line and the perpendicular line
                    double dist_n19_corner = n19_p.dot(n19_c1);
                    // Move along the N19->C1 vector by that amount to the "corner"
                    Triple n19_corner = new Triple(n19_c1).unit().mult(dist_n19_corner);
                    Triple corner = new Triple().likeSum(n19, n19_corner);
                    // Measure the final result
                    pperpDist = Triple.distance(corner, p);
                }
            }
            catch (AtomException ae) {}
            return pperpDist;
        }
        
        protected String toStringImpl()
        { return getLabel(); }
        
        public Object getType()
        { return TYPE_BASEPPERP; }
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

//{{{ CLASS: IsPrePro
//##############################################################################
    /** Simply tells whether (1) or not (0) the next residue in sequence, counting ins codes, is proline. */
    static public class IsPrePro extends Measurement
    {
        public IsPrePro(String label)
        { super(label); }
        
        protected double measureImpl(Model model, ModelState state, Residue res)
        {
            double isPrePro = 0;
            Residue next = res.getNext(model);
            if (next != null)
                if (next.getName().equals("PRO"))
                    isPrePro = 1;
            return isPrePro;
        }
        
        protected String toStringImpl()
        { return getLabel(); }
        
        public Object getType()
        { return TYPE_ISPREPRO; }  // ???
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

