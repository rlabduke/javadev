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
	public static final Object TYPE_PCCPROJEC   = "PCCProjection"; // S.J. 12/10/14 or all P Projection on C1'-C1' line related measurements. Accessible only as part of "virtualsuite" SUPERBLTN
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
    public double measure(Model model, ModelState state, Residue res, boolean doHets)
    {
        // Wouldn't want to give deviations for molecules not described by 
        // the ideal values this code is using, so check for hets
        if(!isProtOrNucAcid(res)    // not valid residue by default
        && !(isHet(res) && doHets)) // not valid residue by user flag
        {
            this.deviation = Double.NaN;
            return Double.NaN;
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

//{{{ getDeviation, measureImpl, getLabel/Type, setResSpec
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

    /**
    * If this Measurement lacks a ResSpec, assigns the provided ResSpec to it.
    * If this Measurement already has a ResSpec, uses the new res name and offset 
    * but reconciles the two by including all true require__ variables from both. 
    * Could produce logical inconsistencies (e.g. "for oxy deoxy") that should 
    * be handled elsewhere.
    */
    public void setResSpec(ResSpec resSpec)
    /*{ this.resSpec = resSpec; }*/ // original version
    {
        if(this.resSpec == null)
            this.resSpec = resSpec; // use new one
        else
        {
            ResSpec newResSpec = new ResSpec(
                resSpec.resOffset,
                (this.resSpec.requireCis    || resSpec.requireCis),
                (this.resSpec.requireDeoxy  || resSpec.requireDeoxy),
                (this.resSpec.requireOxy    || resSpec.requireOxy),
                (this.resSpec.require2p     || resSpec.require2p),
                (this.resSpec.requireDisulf || resSpec.requireDisulf),
                resSpec.origResName
            );
            if(newResSpec.requireDeoxy && newResSpec.requireOxy)
                throw new IllegalArgumentException("Cannot combine oxy and deoxy residue specifiers!");
            this.resSpec = newResSpec;
        }
    }
//}}}

//{{{ isProtOrNucAcid, isHet
//##############################################################################
    public static boolean isProtOrNucAcid(Residue res)
    {
        String aaNames = ":GLY:ALA:VAL:PHE:PRO:MET:ILE:LEU:ASP:GLU:LYS:ARG:SER:THR:TYR:HIS:CYS:ASN:GLN:TRP:ASX:GLX:ACE:FOR:NH2:NME:MSE:AIB:ABU:PCA:MLY:CYO:M3L:DGN:CSD:";
        String naNames = ":  C:  G:  A:  T:  U:CYT:GUA:ADE:THY:URA:URI:GSP:H2U:PSU:4SU:1MG:2MG:M2G:5MC:5MU:T6A:1MA:RIA:OMC:OMG: YG:  I:7MG:YYG:YG :A2M:5FU:G7M:OMU:PR5:FHU:XUG:A23:UMS:FMU:UR3:CFL:UD5:CSL:UFT:5IC:5BU:BGM:CBR:U34:CCC:AVC:TM2:AET: IU:C  :G  :A  :T  :U  :I  : rC: rG: rA: rT: rU: dC: dG: dA: dT: dU: DC: DG: DA: DT: DU:";
        
        // Removed these from normal naNames because they are essentially always monomers
        // and thus should not be treated like normal bases with minor changes. - DAK 091201 
        String notNaNames = "CTP:CDP:CMP:GTP:GDP:GMP:ATP:ADP:AMP:TTP:TDP:TMP:UTP:UDP:UMP";
        
        String resname = res.getName();
        if(aaNames.indexOf(resname) != -1 || naNames.indexOf(resname) != -1) 
            return true; // it's a valid protein or nucleic acid residue name
        return false;
    }

    public static boolean isHet(Residue res)
    {
        for(Iterator aItr = res.getAtoms().iterator(); aItr.hasNext(); )
        {
            Atom a = (Atom) aItr.next();
            if(a.isHet())
            {
                // There's at least one "het" atom in this residue
                return true;
            }
        }
        return false;
    }
//}}}

//{{{ reqCis/Deoxy/Oxy/2p/Disulf
//##############################################################################
    /** @return this, for chaining */
    public Measurement reqCis()
    {
        if(this.resSpec == null) this.resSpec = new ResSpec();
        this.resSpec.requireCis();
        return this;
    }

    /** @return this, for chaining */
    public Measurement reqDeoxy()
    {
        if(this.resSpec == null) this.resSpec = new ResSpec();
        this.resSpec.requireDeoxy();
        return this;
    }

    /** @return this, for chaining */
    public Measurement reqOxy()
    {
        if(this.resSpec == null) this.resSpec = new ResSpec();
        this.resSpec.requireOxy();
        return this;
    }

    /** @return this, for chaining */
    public Measurement req2p()
    {
        if(this.resSpec == null) this.resSpec = new ResSpec();
        this.resSpec.require2p();
        return this;
    }

    /** @return this, for chaining */
    public Measurement reqDisulf()
    {
        if(this.resSpec == null) this.resSpec = new ResSpec();
        this.resSpec.requireDisulf();
        return this;
    }

    /**
    * @param resName a residue name regex or simple string
    * @return this, for chaining
    */
    public Measurement reqResName(String resName)
    {
        if(this.resSpec == null) this.resSpec = new ResSpec(resName);
        else this.resSpec.setResNames(resName);
        return this;
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
                //newBuiltin("alpha"),
                //newBuiltin("beta"),
                //newBuiltin("gamma"),
                //newBuiltin("delta"),
                //newBuiltin("epsilon"),
                //newBuiltin("zeta")
                newBuiltin("alpha"  ).reqOxy(),
                newBuiltin("beta"   ).reqOxy(),
                newBuiltin("gamma"  ).reqOxy(),
                newBuiltin("delta"  ).reqOxy(),
                newBuiltin("epsilon").reqOxy(),
                newBuiltin("zeta"   ).reqOxy()
            };
        else if("dnabb".equals(label)) // added 8/19/09 -- DK
            return new Measurement[] 
            {
                //newBuiltin("alpha"),
                //newBuiltin("beta"),
                //newBuiltin("gamma"),
                //newBuiltin("delta"),
                //newBuiltin("epsilon"),
                //newBuiltin("zeta")
                newBuiltin("alpha"  ).reqDeoxy(),
                newBuiltin("beta"   ).reqDeoxy(),
                newBuiltin("gamma"  ).reqDeoxy(),
                newBuiltin("delta"  ).reqDeoxy(),
                newBuiltin("epsilon").reqDeoxy(),
                newBuiltin("zeta"   ).reqDeoxy()
            };
        else if("suitefit".equals(label)) // added 6/20/07 -- DK
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
		else if("virtualsuite".equals(label)) // S.J. added 12/09/14
		{//this SUPERBLTN will contain some BUILTINs, and also some individual measurements
			
			// base pperp related parameters
			String bppLabel = "base-P perp";
            Measurement.BasePhosPerp bpp = new Measurement.BasePhosPerp(bppLabel);
			bppLabel = "base-P perp -1";
            Measurement.BasePhosPerp bpp_prev = new Measurement.BasePhosPerp(bppLabel,-1);
			bppLabel = "Dist C1'-Pperp Point";
			Measurement.BasePhosPerp ext_dist = new Measurement.BasePhosPerp(bppLabel,false);
			bppLabel = "Dist C1'-Pperp Point -1";
			Measurement.BasePhosPerp ext_dist_prev = new Measurement.BasePhosPerp(bppLabel,-1,false);
			
			// p projection related parameters
			String pprojLabel = "Dist P-Pproj";
			Measurement.PhosC1C1Proj pproj_dist = new Measurement.PhosC1C1Proj(pprojLabel,1);
			pprojLabel = "Dist C1'-Pproj";
			Measurement.PhosC1C1Proj c1proj_dist = new Measurement.PhosC1C1Proj(pprojLabel,2);
			pprojLabel = "P ratio";
			Measurement.PhosC1C1Proj pproj_ratio = new Measurement.PhosC1C1Proj(pprojLabel,3);
			pprojLabel = "C1-Pproj/C1-C1 ratio";
			Measurement.PhosC1C1Proj c1proj_ratio = new Measurement.PhosC1C1Proj(pprojLabel,4);
			
			return new Measurement[]
			{
				newBuiltin("N1/9-N1/9"),
				newBuiltin("N1/9-C1'-C1'-N1/9"),
				newBuiltin("N1/9-C1'-C1'"),
				newBuiltin("C1'-C1'-N1/9"),
				newBuiltin("N1/9-C1'-C1'-P"),
				pproj_dist,
				c1proj_dist,
				newBuiltin("C1'-C1'"),
				bpp_prev,
				ext_dist_prev,
				bpp,
				ext_dist,
				newBuiltin("P-P"),
				pproj_ratio,
				c1proj_ratio
			};
		}
		else if("disulfides".equals(label) || "disulf".equals(label) || "ss".equals(label)) // added 9/21/09 -- DK
	        return new Measurement[]
            {
                newBuiltin("phi"  ).reqDisulf(),
                newBuiltin("psi"  ).reqDisulf(),
                newBuiltin("chi1" ).reqDisulf(),
                newBuiltin("chi2" ).reqDisulf(),
                newBuiltin("chi3" ).reqDisulf(),
                newBuiltin("CB-S-S").reqDisulf(),
                newBuiltin("S--S").reqDisulf(),
                newBuiltin("S-S-CB'"),
                newBuiltin("chi2'"), //  explicitly
                newBuiltin("chi1'"), //   require
                newBuiltin("phi'" ), //   disulf
                newBuiltin("psi'" )  //   already
                // VBC params here too?
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
        // Used to be same definition as Dang (named for the first residue in the peptide)
        // but now named for second residue in the peptide. -DK 100217
        else if("omega".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_CA_"),
                new AtomSpec(-1, "_C__"),
                new AtomSpec( 0, "_N__"),
                new AtomSpec( 0, "_CA_")
            );
        else if("chi1".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_N__"),
                new AtomSpec( 0, "_CA_"),
                new AtomSpec( 0, "_CB_"),
                new AtomSpec( 0, "/_[ACNOS]G[_1]/")
            );
        //else if("chi2".equals(label))
        //    return new Dihedral(label,
        //        new AtomSpec( 0, "_CA_"),
        //        new AtomSpec( 0, "_CB_"),
        //        new AtomSpec( 0, "/_[ACNOS]G[_1]/"),
        //        new AtomSpec( 0, "/_[ACNOS]D[_1]/")
        //    );
        else if("chi2".equals(label))
            return new Group(
                new Dihedral(label, // disulfides
                    new AtomSpec( 0, "_CA_"),
                    new AtomSpec( 0, "_CB_"),
                    new AtomSpec( 0, "_SG_"),
                    new AtomSpec( 0, "_SG_").otherEndDisulf()
                )
            ).add(
                new Dihedral(label, // regular sidechains (default case)
                    new AtomSpec( 0, "_CA_"),
                    new AtomSpec( 0, "_CB_"),
                    new AtomSpec( 0, "/_[ACNOS]G[_1]/"),
                    new AtomSpec( 0, "/_[ACNOS]D[_1]/")
            ));
        //else if("chi3".equals(label))
        //    return new Dihedral(label,
        //        new AtomSpec( 0, "_CB_"),
        //        new AtomSpec( 0, "/_[ACNOS]G[_1]/"),
        //        new AtomSpec( 0, "/_[ACNOS]D[_1]/"),
        //        new AtomSpec( 0, "/_[ACNOS]E[_1]/")
        //    );
        else if("chi3".equals(label))
            return new Group(
                new Dihedral(label, // disulfides
                    new AtomSpec( 0, "_CB_"),
                    new AtomSpec( 0, "_SG_"),
                    new AtomSpec( 0, "_SG_").otherEndDisulf(),
                    new AtomSpec( 0, "_CB_").otherEndDisulf()
                )
            ).add(
                new Dihedral(label, // Pro
                    new AtomSpec( 0, "_CB_"),
                    new AtomSpec( 0, "_CG_"),
                    new AtomSpec( 0, "_CD_"), // don't wanna allow N here for other residue types b/c could
                    new AtomSpec( 0, "_N__")  // cause weird sc-mc jumps if end of sc missing -- DAK 100226
                ).reqResName("PRO")
            ).add(
                new Dihedral(label, // regular sidechains (default case)
                    new AtomSpec( 0, "_CB_"),
                    new AtomSpec( 0, "/_[ACNOS]G[_1]/"),
                    new AtomSpec( 0, "/(_[ACNOS]D[_1])|(SE__)/"), // now handles selenoMet -- DAK 090923
                    new AtomSpec( 0, "/_[ACNOS]E[_1]/")
            ));
        else if("chi4".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "/_[ACNOS]G[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]D[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]E[_1]/"),
                new AtomSpec( 0, "/_[ACNOS]Z[_1]/")
            );
        // Added the following disulfide-specific builtins -- DAK 9/22/09
        else if("phi'".equals(label) || "phip".equals(label))
            return new Dihedral(label,
                new AtomSpec(-1, "_C__").otherEndDisulf(),
                new AtomSpec( 0, "_N__").otherEndDisulf(),
                new AtomSpec( 0, "_CA_").otherEndDisulf(),
                new AtomSpec( 0, "_C__").otherEndDisulf()
            ).reqDisulf();
        else if("psi'".equals(label) || "psip".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_N__").otherEndDisulf(),
                new AtomSpec( 0, "_CA_").otherEndDisulf(),
                new AtomSpec( 0, "_C__").otherEndDisulf(),
                new AtomSpec( 1, "_N__").otherEndDisulf()
            ).reqDisulf();
        else if("chi1'".equals(label) || "chi1p".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_N__").otherEndDisulf(),
                new AtomSpec( 0, "_CA_").otherEndDisulf(),
                new AtomSpec( 0, "_CB_").otherEndDisulf(),
                new AtomSpec( 0, "_SG_").otherEndDisulf()
            ).reqDisulf();
        else if("chi2'".equals(label) || "chi2p".equals(label))
            return new Dihedral(label,
                new AtomSpec( 0, "_CA_").otherEndDisulf(),
                new AtomSpec( 0, "_CB_").otherEndDisulf(),
                new AtomSpec( 0, "_SG_").otherEndDisulf(),
                new AtomSpec( 0, "_SG_")
            ).reqDisulf();
        else if("tau".equals(label))
            return new Angle(label,
                new AtomSpec( 0, "_N__"),
                new AtomSpec( 0, "_CA_"),
                new AtomSpec( 0, "_C__")
            );
        else if("S-S-CB'".equals(label))
            return new Angle(label,
                new AtomSpec( 0, "_CB_").otherEndDisulf(),
                new AtomSpec( 0, "_SG_").otherEndDisulf(),
                new AtomSpec( 0, "_SG_")
            ).reqDisulf();
        else if("CB-S-S".equals(label))
            return new Angle(label,
                new AtomSpec( 0, "_SG_").otherEndDisulf(),
                new AtomSpec( 0, "_SG_"),
                new AtomSpec( 0, "_CB_")
            ).reqDisulf();
        else if("S--S".equals(label))
            return new Distance(label,
                new AtomSpec( 0, "_SG_").otherEndDisulf(),
                new AtomSpec( 0, "_SG_")
            ).reqDisulf();
        /* OLD VERSION OF CBDEV FOR BACKUP
        else if("cbdev".equals(label))
            return new Distance(label,
                new AtomSpec( 0, "_CB_"),
                new XyzSpec.IdealTetrahedral(
                    new AtomSpec( 0, "_N__"),
                    new AtomSpec( 0, "_C__"),
                    new AtomSpec( 0, "_CA_"),
                    1.536, 110.4, 110.6, 123.1, -123.0
            ));*/
        // Extensively edited this builtin based on PKINANGL.c from Prekin.
        // Now, parameters to build an ideal Cbeta are amino-acid-specific and 
        // the output is identical to that from Prekin (as far as I can tell).
        // Here are Ian's old amino-acid-independent defaults, for posterity:
        // 1.536, 110.4, 110.6, 123.1, -123.0 -- DAK 090924
        else if("cbdev".equals(label))
            return new Group(
                new Distance(label,
                    new AtomSpec( 0, "_CB_"),
                    new XyzSpec.IdealTetrahedral(
                        new AtomSpec( 0, "_N__"),
                        new AtomSpec( 0, "_C__"),
                        new AtomSpec( 0, "_CA_"),
                        1.536, 110.1, 110.6, 122.9, -122.6 // Ala
                )).reqResName("ALA")
            ).add(new Distance(label,
                    new AtomSpec( 0, "_CB_"),
                    new XyzSpec.IdealTetrahedral(
                        new AtomSpec( 0, "_N__"),
                        new AtomSpec( 0, "_C__"),
                        new AtomSpec( 0, "_CA_"),
                        1.530, 112.2, 103.0, 115.1, -120.7 // Pro
                )).reqResName("PRO")
            ).add(
                new Distance(label,
                    new AtomSpec( 0, "_CB_"),
                    new XyzSpec.IdealTetrahedral(
                        new AtomSpec( 0, "_N__"),
                        new AtomSpec( 0, "_C__"),
                        new AtomSpec( 0, "_CA_"),
                        1.540, 109.1, 111.5, 123.4, -122.0 // Val/Thr/Ile (branched-beta)
                )).reqResName("/(VAL)|(THR)|(ILE)/")
            ).add(
                // I guess this serves to keep Gly out of the "everything else" category (?)
                new Distance(label,
                    new AtomSpec( 0, "_CB_"),
                    new XyzSpec.IdealTetrahedral(
                        new AtomSpec( 0, "_N__"),
                        new AtomSpec( 0, "_C__"),
                        new AtomSpec( 0, "_CA_"),
                        1.100, 109.3, 109.3, 121.6, -121.6 // Gly
                )).reqResName("GLY")
            ).add(
                new Distance(label,
                    new AtomSpec( 0, "_CB_"),
                    new XyzSpec.IdealTetrahedral(
                        new AtomSpec( 0, "_N__"),
                        new AtomSpec( 0, "_C__"),
                        new AtomSpec( 0, "_CA_"),
                        1.530, 110.1, 110.5, 122.8, -122.6 // everything else
                ))
            );
        else if("hadev".equals(label))
            return new Distance(label,
                new AtomSpec( 0, "_HA_"),
                new XyzSpec.IdealTetrahedral(
                    new AtomSpec( 0, "_N__"),
                    new AtomSpec( 0, "_C__"),
                    new AtomSpec( 0, "_CA_"),
                    1.100, 107.9, 108.1, -118.3, 118.2 // from IWD's SidechainIdealizer.idealizeCB()
            ));
        else if("nhdev".equals(label))
            return new Distance(label,
                new AtomSpec( 0, "_H__"),
                new XyzSpec.IdealTetrahedral(
                    new AtomSpec(-1, "_C__"),
                    new AtomSpec( 0, "_CA_"),
                    new AtomSpec( 0, "_N__"),
                    // N--H, CA-N-H, C-N-H, C-CA-N-H, CA-C-N-H
                    //1.000, 114.4, 123.8, 180.0, 180.0 // raw idealpolyala12-[alpha/beta].pdb with H's from ??
                    1.000, 114.3, 123.9, 180.0, 180.0 // what Reduce does to idealpolyala12-[alpha/beta].pdb
            ));
        else if("codev".equals(label))
            return new Distance(label,
                new AtomSpec( 0, "_O__"),
                new XyzSpec.IdealTetrahedral(
                    new AtomSpec( 0, "_CA_"),
                    new AtomSpec( 1, "_N__"),
                    new AtomSpec( 0, "_C__"),
                    // C--O, N-C-O, CA-C-O, CA-N-C-O, N-CA-C-O
                    1.229, 122.7, 120.1, 180.0, 180.0 // from Engh & Huber resource file in this package
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
        // Deprecated as method for differentiating RNA vs. DNA.
        // Use ResSpec.requireOxy() or ResSpec.requireDeoxy() instead.
        //else if("c2o2".equals(label)) // added 7/31/07 -- DK
        //    return new Distance(label,
        //        new AtomSpec( 0, "_C2*"),
        //        new AtomSpec( 0, "(_O2*)|(SE2*)")
        //    );
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
                new Dihedral(label, // modified bases FHU and PSU, added 10/27/09 -- DK & SJ
                    new AtomSpec( 0, "_O4*"),
                    new AtomSpec( 0, "_C1*"),
                    new AtomSpec( 0, "_C5_"),
                    new AtomSpec( 0, "_C4_")
                ).reqResName("/(FHU)|(PSU)|/")
            ).add(
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
		//{{{ BUILTINs for "virtualsuite" SUPERBLTN - S.J. 12/09/14
		else if("N1/9-N1/9".equals(label))
			return new Group(
				new Distance(label,
			 	    new AtomSpec(-1, "_N9_"),
				    new AtomSpec( 0, "_N9_")
		    )).add(
				new Distance(label,
				    new AtomSpec(-1, "_N9_"),
				    new AtomSpec( 0, "_N1_")
			)).add(
				new Distance(label,
				    new AtomSpec(-1, "_N1_"),
				    new AtomSpec( 0, "_N9_")
			)).add(
			    new Distance(label,
					new AtomSpec(-1, "_N1_"),
					new AtomSpec( 0, "_N1_")
			));
		else if("N1/9-C1'-C1'-N1/9".equals(label))
			return new Group(
				new Dihedral(label,
				    new AtomSpec(-1, "_N9_"),
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*"),		 
				    new AtomSpec( 0, "_N9_")
			)).add(
			    new Dihedral(label,
					new AtomSpec(-1, "_N9_"),
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*"),	
					new AtomSpec( 0, "_N1_")
			)).add(
				new Dihedral(label,
					new AtomSpec(-1, "_N1_"),
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*"),	
					new AtomSpec( 0, "_N9_")
		    )).add(
				new Dihedral(label,
					new AtomSpec(-1, "_N1_"),
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*"),	
					new AtomSpec( 0, "_N1_")
		    ));
		else if("N1/9-C1'-C1'".equals(label))
			return new Group(
				new Angle(label,
					new AtomSpec(-1, "_N9_"),
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*")
			)).add(
				new Angle(label,
				    new AtomSpec(-1, "_N1_"),
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*") 
		    ));			  
		else if("C1'-C1'-N1/9".equals(label))
			return new Group(
				new Angle(label,
				    new AtomSpec(-1, "_C1*"),
				    new AtomSpec( 0, "_C1*"),
					new AtomSpec( 0, "_N9_")
			)).add(
				new Angle(label,
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*"),
					new AtomSpec( 0, "_N1_") 
			));	
		else if("N1/9-C1'-C1'-P".equals(label))
			return new Group(
				new Dihedral(label,
					new AtomSpec(-1, "_N9_"),
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*"),		 
					new AtomSpec( 0, "_P__")
			)).add(
				new Dihedral(label,
					new AtomSpec(-1, "_N1_"),
					new AtomSpec(-1, "_C1*"),
					new AtomSpec( 0, "_C1*"),	
					new AtomSpec( 0, "_P__")
		    ));
		else if("C1'-C1'".equals(label))
			return new Distance(label,
				new AtomSpec(-1, "_C1*"),
				new AtomSpec( 0, "_C1*")
		    );
		else if("P-P".equals(label))
			return new Distance(label,
				new AtomSpec( 0, "_P__"),
				new AtomSpec( 1, "_P__")
			);
		//}}} BUILTINs for "virtualsuite" SUPERBLTN
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
            return Triple.angle(aa, bb, cc);
        }
        
        protected String toStringImpl()
        { return "angle "+getLabel()+" "+a+", "+b+", "+c; }
        
        public Object getType()
        { return TYPE_ANGLE; }
        
        // Made the below getX() methods return XyzSpec instead of AtomSpec;
        // now GeomKinSmith casts the return value as AtomSpec instead.
        // Should be more generally applicable now (?).
        
        public XyzSpec getA()
        { return a; }
        
        public XyzSpec getB()
        { return b; }
        
        public XyzSpec getC()
        { return c; }
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
            return Triple.dihedral(aa, bb, cc, dd);
        }
        
        protected String toStringImpl()
        { return "dihedral "+getLabel()+" "+a+", "+b+", "+c+", "+d; }
        
        public Object getType()
        { return TYPE_DIHEDRAL; }
        
        // Made the below getX() methods return XyzSpec instead of AtomSpec;
        // now GeomKinSmith casts the return value as AtomSpec instead.
        // Should be more generally applicable now (?).
        
        public XyzSpec getA()
        { return a; }
        
        public XyzSpec getB()
        { return b; }
        
        public XyzSpec getC()
        { return c; }
        
        public XyzSpec getD()
        { return d; }
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
        
        //Collection<XyzSpec> specs = new ArrayList();
        
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
// S.J. 12/09/14 - changed to calculate pperp for current or prev residue, based on value of flag passed, 0 for current, -1 for prev	
// also changed to calculate either the pperp distance, or the distance of the pperp point from the corresponding C1', based on the another flag passed. true for pperp, false for pperp point C1' distance
    public static class BasePhosPerp extends Measurement
    {
		int prev_or_current=0; // to keep track if we are calculating pperp for prev residue or current residue
		boolean calc_pperp = true; // will calc pperp if true, distance of pperp point to C1' if false
		
        public BasePhosPerp(String label)
        { 
			super(label);
		}
		
		public BasePhosPerp(String label,int flag)
        { 
			super(label);
			prev_or_current=flag;// 0 for current residue, -1 for prev residue
		}
		
		public BasePhosPerp(String label,boolean flag2)
        { 
			super(label);
			calc_pperp=flag2;// true for pperp, false for distance of pperp point to C1'
		}
		
		public BasePhosPerp(String label,int flag, boolean flag2)
        { 
			super(label);
			prev_or_current=flag;// 0 for current residue, -1 for prev residue
			calc_pperp=flag2;
		}
        
        protected double measureImpl(Model model, ModelState state, Residue res) 
        {
            double pperpDist = Double.NaN;
            try
            {
                Residue next = res.getNext(model); // want 3' P (later in seq!)
				Residue prev = res.getPrev(model); // to get C1' and N1/9 for prev residue
				
				Atom phos= null,carb = null,nitr = null;
				if(next != null)
                {
                    // Get relevant atom coords
					if(prev_or_current == 0) //calculate the pperp for current residue
					{	
						phos = next.getAtom(" P  ");
						carb =  res.getAtom(" C1'");
						nitr =  res.getAtom(" N9 ");
						if(carb == null) carb = res.getAtom(" C1*");
						if(nitr == null) nitr = res.getAtom(" N1 ");
					}
					else if(prev_or_current == -1 && prev != null)//calculate the pperp for the prev residue
					{
						phos = res.getAtom(" P  ");
						carb =  prev.getAtom(" C1'");
						nitr =  prev.getAtom(" N9 ");
						if(carb == null) carb = prev.getAtom(" C1*");
						if(nitr == null) nitr = prev.getAtom(" N1 ");
					}
                    AtomState p   = state.get(phos);
                    AtomState c1  = state.get(carb);
                    AtomState n19 = state.get(nitr);
                    // Draw appropriate vectors
                    Triple n19_p  = new Triple().likeVector(n19, p);
                    Triple n19_c1 = new Triple().likeVector(n19, c1);
                    // Get distance from N19 to the intersection point 
                    // of the N19->C1 line and the perpendicular line
                    // Note: Added denominator to fix this math! -DAK 110223
                    double dist_n19_corner = n19_p.dot(n19_c1) / n19_c1.mag();
                    // Move along the N19->C1 vector by that amount to the "corner"
                    Triple n19_corner = new Triple(n19_c1).unit().mult(dist_n19_corner);
                    Triple corner = new Triple().likeSum(n19, n19_corner);
                    // Measure the final result
					if(calc_pperp) //calculate pperp distance
						pperpDist = Triple.distance(corner, p);
					else // calculate distance from C1'
						pperpDist = Triple.distance(corner,c1);
                }
            }
            catch(AtomException ae) {}
            return pperpDist;
        }
        
        protected String toStringImpl()
        { return getLabel(); }
        
        public Object getType()
        { return TYPE_BASEPPERP; }
    }
//}}}

//{{{ CLASS: PhosC1C1Proj
//##############################################################################
// S.J. 12/10/14 - added to calcuate measures related to the projection of P on the C1'-C1' line. As part of parameters for SUPERBLTN "virtualsuite"
// Will calculate different things based on the value of the variable what_to_calc:
// 1: P-Pproj distance
// 2: Pproj-C1'(i-1) distance
// 3: Ratio: Pproj-C1'(i-1) distance/P-Pproj distance
// 4: Ratio: Pproj-C1'(i-1) distance/C1'-C1' distance
	public static class PhosC1C1Proj extends Measurement
    {
		int what_to_calc = 0;
		
		public PhosC1C1Proj(String label, int flag) // make sures that you have to pass a number to it
        { 
			super(label);
			what_to_calc=flag;
		}
		
		protected double measureImpl(Model model, ModelState state, Residue res) 
        {
            double final_measure = Double.NaN;
            try
            {
				Residue prev = res.getPrev(model); // to get the C1' from the prev residue
				if(prev != null)
				{
					Atom phos = res.getAtom(" P  ");
					Atom carb_prev = prev.getAtom(" C1'");
					if(carb_prev == null) carb_prev = prev.getAtom(" C1*");
					Atom carb = res.getAtom(" C1'");
					if(carb == null) carb = res.getAtom(" C1*");
					
					AtomState p   = state.get(phos); // Code copied from BasePhosPperp class, changed the variable names
                    AtomState c1  = state.get(carb);
                    AtomState c1prev = state.get(carb_prev);
                    // Draw appropriate vectors
                    Triple c1prev_p  = new Triple().likeVector(c1prev, p);
                    Triple c1prev_c1 = new Triple().likeVector(c1prev, c1);
                    // Get distance from C1 prev to the intersection point 
                    // of the C1->C1 line and the perpendicular line from P
                    double dist_c1prev_proj = c1prev_p.dot(c1prev_c1) / c1prev_c1.mag();
                    // Move along the C1->C1 vector by that amount to the "projection"
                    Triple c1prev_proj = new Triple(c1prev_c1).unit().mult(dist_c1prev_proj);
                    Triple proj = new Triple().likeSum(c1prev, c1prev_proj);
					
					if(what_to_calc == 1)
						final_measure = Triple.distance(proj,p);
					else if (what_to_calc == 2)
						final_measure = Triple.distance(proj,c1prev);
					else if(what_to_calc == 3)
						final_measure = Triple.distance(proj,c1prev)/Triple.distance(proj,p);	
					else if(what_to_calc == 4)
						final_measure = Triple.distance(proj,c1prev)/Triple.distance(c1prev,c1);
				}
			}
			catch(AtomException ae) {}
            return final_measure;
		}
		
		protected String toStringImpl()
        { return getLabel(); }
        
        public Object getType()
        { return TYPE_PCCPROJEC; }
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
            Residue next = res.getNext(model);
            if(next != null && next.getName().equals("PRO")) return (double) 1.0;
            return (double) 0;
        }
        
        protected String toStringImpl()
        { return getLabel(); }
        
        public Object getType()
        { return TYPE_ISPREPRO; }
    }
//}}}
        
//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

