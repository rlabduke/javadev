// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>Util</code> defines static utility functions for making atom and bond
* selections and other common tasks when using Molikin classes.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May 10 09:33:51 EDT 2005
*/
public class Util //extends ... implements ...
{
//{{{ Constructor(s)
//##############################################################################
    private Util()
    {
        super();
    }
//}}}

//{{{ extractOrderedStatesByName
//##############################################################################
    static public Collection extractOrderedStatesByName(Model model)
    { return extractOrderedStatesByName(model.getResidues(), model.getStates()); }
    
    /**
    * Extracts all the uniquely named AtomStates for the given model, in the
    * order of Residues and Atoms given.
    * This is often used to prepare input for AtomGraph.
    */
    static public Collection extractOrderedStatesByName(Collection residues, Collection modelStates)
    {
        ModelState[]    states      = (ModelState[]) modelStates.toArray(new ModelState[modelStates.size()]);
        Set             usedNames   = new HashSet(); // to avoid duplicates
        ArrayList       atomStates  = new ArrayList();
        
        for(Iterator ri = residues.iterator(); ri.hasNext(); )
        {
            Residue res = (Residue)ri.next();
            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom atom = (Atom)ai.next();
                for(int i = 0; i < states.length; i++)
                {
                    try
                    {
                        AtomState as = states[i].get(atom);
                        // We want to make sure every atom output has a unique PDB name.
                        // We're not worried so much about duplicating coordinates (old code).
                        // Name requirement is important for dealing with alt confs,
                        // where a single atom (' ') may move in A but not B --
                        // this led to two ATOM entries with different coords but the same name.
                        String aName = as.getAtom().toString()+as.getAltConf();
                        //if(!usedNames.contains(as)) -- for comparison by XYZ coords
                        if(!usedNames.contains(aName))
                        {
                            //usedNames.add(as); -- for comparison by XYZ coords
                            usedNames.add(aName);
                            atomStates.add(as);
                        }
                    }
                    catch(AtomException ex) {} // no state
                }
            }//for each atom
        }// for each residue
        return atomStates;
    }
//}}}

//{{{ altsAreCompatible
//##############################################################################
    /**
    * True iff both states have the same alternate conformation label
    * or if one of them has " " (a single space) as its ID.
    */
    static public boolean altsAreCompatible(AtomState as1, AtomState as2)
    {
        String alt1 = as1.getAltConf(), alt2 = as2.getAltConf();
        return (alt1.equals(alt2) || alt1.equals(" ") || alt2.equals(" "));
    }
//}}}

//{{{ isMainchain, isWater
//##############################################################################
    /** Based on Prekin PKINCSBS.c decidemainside() */
    static String mcPattern = ".N[ T].|.C[A ].|.O .|.OXT|[^2][HDQ][A ] |.[HDQ].['*]|.P  |.O[123]P|.[CO][1-5]['*]| CM2";
    //                                                   ^^^^
    //                              makes one Gly H sidechain, the other mainchain
    // added _CM2 and _O3P for tr0001 on 051114
    static Matcher mcMatcher = null;
    static public boolean isMainchain(AtomState as)
    {
        if(mcMatcher == null) mcMatcher = Pattern.compile(mcPattern).matcher("");
        mcMatcher.reset(as.getName());
        return mcMatcher.matches();
    }
    
    static String waterPattern = "HOH|DOD|H20|D20|WAT|SOL|TIP|TP3|MTO|HOD|DOH";
    static Matcher waterMatcher = null;
    static public boolean isWater(AtomState as)
    { return isWater(as.getResidue()); }
    static public boolean isWater(Residue res)
    {
        if(waterMatcher == null) waterMatcher = Pattern.compile(waterPattern).matcher("");
        waterMatcher.reset(res.getName());
        return waterMatcher.matches();
    }
//}}}

//{{{ isProtein, isNucleicAcid, isIon
//##############################################################################
    /** Based on Prekin's AAList */
    static String protPattern = "GLY|ALA|VAL|PHE|PRO|MET|ILE|LEU|ASP|GLU|LYS|ARG|SER|THR|TYR|HIS|CYS|ASN|GLN|TRP|ASX|GLX|ACE|FOR|NH2|NME|MSE|AIB|ABU|PCA|MLY|CYO|M31";
    //mly added 001114 for myosin 2MYS methylated lysine
    //cyo added 010708 for S-LECTIN 1SLT oxidized cys
    //m3l added 041011 for methylated lysine== Methyl 3 Lysine
    static Matcher protMatcher = null;
    static public boolean isProtein(Residue res)
    {
        if(protMatcher == null) protMatcher = Pattern.compile(protPattern).matcher("");
        protMatcher.reset(res.getName());
        return protMatcher.matches();
    }

    /** Based on Prekin's NAList */
    static String nucacidPattern = "  C|  G|  A|  T|  U|CYT|GUA|ADE|THY|URA|URI|CTP|CDP|CMP|GTP|GDP|GMP|ATP|ADP|AMP|TTP|TDP|TMP|UTP|UDP|UMP|GSP|H2U|PSU|1MG|2MG|M2G|5MC|5MU|T6A|1MA|RIA|OMC|OMG| YG|  I|7MG|C  |G  |A  |T  |U  |YG |I  ";
    //7mg added 001114 for tRNA 1EHZ
    static Matcher nucacidMatcher = null;
    static public boolean isNucleicAcid(Residue res)
    {
        if(nucacidMatcher == null) nucacidMatcher = Pattern.compile(nucacidPattern).matcher("");
        nucacidMatcher.reset(res.getName());
        return nucacidMatcher.matches();
    }

    /** Based on name only -- if you want only 1 atom, you must check that yourself */
    static String ionPattern = " *(?:HE|LI|BE|F|NE|NA|MG|P|S|CL|AR|K|CA|CR|MN|FE|CO|NI|CU|ZN|GA|AS|SE|BR|KR|RB|SR|MO|RU|RH|PD|AG|CD|SN|I|XE|CS|BA|W|RE|OS|IR|PT|AU|HG|TL|PB|BI|RN|FR|RA|U|PU) *";
    static Matcher ionMatcher = null;
    static public boolean isIon(Residue res)
    {
        if(ionMatcher == null) ionMatcher = Pattern.compile(ionPattern).matcher("");
        ionMatcher.reset(res.getName());
        return ionMatcher.matches();
    }
//}}}

//{{{ getElementColor
//##############################################################################
    static Map elementColors = null;
    /**
    * Given the element symbol (1 or 2 chars, uppercase) this returns
    * the standard kinemage color for it.
    */
    static public String getElementColor(String element)
    {
        if(elementColors == null) //{{{
        {
            elementColors = new HashMap();
            elementColors.put("H", "gray");
            elementColors.put("C", "white");
            elementColors.put("N", "sky");
            elementColors.put("O", "red");
            elementColors.put("S", "yellow");
            elementColors.put("P", "gold");
            // These ~ borrowed from RasMol
            // We could equally well turn to Probe's atomprops.h
            elementColors.put("HE", "pinktint");
            elementColors.put("LI", "brown");
            elementColors.put("B", "green");
            elementColors.put("F", "orange");
            elementColors.put("NA", "blue");
            elementColors.put("MG", "greentint");
            elementColors.put("AL", "gray");
            elementColors.put("SI", "gold");
            elementColors.put("CL", "green");
            elementColors.put("CA", "gray");
            elementColors.put("TI", "gray");
            elementColors.put("CR", "gray");
            elementColors.put("MN", "gray");
            elementColors.put("FE", "orange");
            elementColors.put("NI", "brown");
            elementColors.put("CU", "brown");
            elementColors.put("ZN", "brown");
            elementColors.put("BR", "brown");
            elementColors.put("AG", "gray");
            elementColors.put("I", "bluetint");
            elementColors.put("BA", "orange");
            elementColors.put("AU", "gold");
        }//}}}
        String color = (String) elementColors.get(element);
        if(color == null)   return "hotpink";
        else                return color;
    }
//}}}

//{{{ getVdwRadius
//##############################################################################
    static Map exVdwRadii = null;
    /**
    * Given the element symbol (1 or 2 chars, uppercase) this returns
    * the explicit-H van der Waals radius (taken from Probe's atomprops.h).
    */
    static public double getVdwRadius(String element)
    {
        if(exVdwRadii == null) //{{{
        {
            exVdwRadii = new HashMap();
            // For non-metals, explicit VDW radii from 
            // Gavezzotti, J. Am. Chem. Soc. (1983) 105, 5220-5225.
            // or, if unavailable,
            // Bondi, J. Phys. Chem. (1964), V68, N3, 441-451.
            // Covalent and ionic radii from
            // Advanced Inorganic Chemistry, Cotton & Wilkinson, 1962, p93.
            exVdwRadii.put("H".toUpperCase(), new Double(1.17));
            //exVdwRadii.put("Harom".toUpperCase(), new Double(1.00));
            //exVdwRadii.put("Hpolar".toUpperCase(), new Double(1.00));
            //exVdwRadii.put("HOd".toUpperCase(), new Double(1.00));
            exVdwRadii.put("C".toUpperCase(), new Double(1.75));
            exVdwRadii.put("N".toUpperCase(), new Double(1.55));
            exVdwRadii.put("O".toUpperCase(), new Double(1.40));
            exVdwRadii.put("P".toUpperCase(), new Double(1.80));
            exVdwRadii.put("S".toUpperCase(), new Double(1.80));
            exVdwRadii.put("As".toUpperCase(), new Double(2.00));
            exVdwRadii.put("Se".toUpperCase(), new Double(1.90));
            exVdwRadii.put("F".toUpperCase(), new Double(1.30));
            exVdwRadii.put("Cl".toUpperCase(), new Double(1.77));
            exVdwRadii.put("Br".toUpperCase(), new Double(1.95));
            exVdwRadii.put("I".toUpperCase(), new Double(2.10));
            // for most common metals we use Pauling's ionic radii
            // "covalent radii" = ionic + 0.74 (i.e., oxygenVDW(1.4) - oxygenCov(0.66))
            // because the ionic radii are usually calculated from Oxygen-Metal distance
            exVdwRadii.put("Li".toUpperCase(), new Double(0.60));
            exVdwRadii.put("Na".toUpperCase(), new Double(0.95));
            exVdwRadii.put("Al".toUpperCase(), new Double(0.50));
            exVdwRadii.put("K".toUpperCase(), new Double(1.33));
            exVdwRadii.put("Mg".toUpperCase(), new Double(0.65));
            exVdwRadii.put("Ca".toUpperCase(), new Double(0.99));
            exVdwRadii.put("Mn".toUpperCase(), new Double(0.80));
            exVdwRadii.put("Fe".toUpperCase(), new Double(0.74));
            exVdwRadii.put("Co".toUpperCase(), new Double(0.70));
            exVdwRadii.put("Ni".toUpperCase(), new Double(0.66));
            exVdwRadii.put("Cu".toUpperCase(), new Double(0.72));
            exVdwRadii.put("Zn".toUpperCase(), new Double(0.71));
            exVdwRadii.put("Rb".toUpperCase(), new Double(1.48));
            exVdwRadii.put("Sr".toUpperCase(), new Double(1.10));
            exVdwRadii.put("Mo".toUpperCase(), new Double(0.93));
            exVdwRadii.put("Ag".toUpperCase(), new Double(1.26));
            exVdwRadii.put("Cd".toUpperCase(), new Double(0.91));
            exVdwRadii.put("In".toUpperCase(), new Double(0.81));
            exVdwRadii.put("Cs".toUpperCase(), new Double(1.69));
            exVdwRadii.put("Ba".toUpperCase(), new Double(1.29));
            exVdwRadii.put("Au".toUpperCase(), new Double(1.10));
            exVdwRadii.put("Hg".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Tl".toUpperCase(), new Double(1.44));
            exVdwRadii.put("Pb".toUpperCase(), new Double(0.84));
            // for other metals we use Shannon's ionic radii
            // Acta Crystallogr. (1975) A32, pg751.
            exVdwRadii.put("V".toUpperCase(), new Double(0.79));
            exVdwRadii.put("Cr".toUpperCase(), new Double(0.73));
            exVdwRadii.put("Te".toUpperCase(), new Double(0.97));
            exVdwRadii.put("Sm".toUpperCase(), new Double(1.08));
            exVdwRadii.put("Gd".toUpperCase(), new Double(1.05));
            exVdwRadii.put("Yb".toUpperCase(), new Double(1.14));
            exVdwRadii.put("W".toUpperCase(), new Double(0.66));
            exVdwRadii.put("Pt".toUpperCase(), new Double(0.63));
            exVdwRadii.put("U".toUpperCase(), new Double(1.03));
            // Cotton & Wilkinson and also-
            // L.E. Sutton (ed.) in Table of interatomic distances and configuration in molecules
            // and ions, Supplement 1956-1959, Special publication No. 18, Chemical Society,
            // London, UK, 1965 (as listed in web-elements by Mark Winter)
            //                   http://www.shef.ac.uk/chemistry/web-elements
            exVdwRadii.put("He".toUpperCase(), new Double(1.60));
            exVdwRadii.put("Be".toUpperCase(), new Double(0.31));
            exVdwRadii.put("B".toUpperCase(), new Double(0.20));
            exVdwRadii.put("Ne".toUpperCase(), new Double(1.60));
            exVdwRadii.put("Si".toUpperCase(), new Double(2.10));
            exVdwRadii.put("Ar".toUpperCase(), new Double(1.89));
            exVdwRadii.put("Sc".toUpperCase(), new Double(0.68));
            exVdwRadii.put("Ti".toUpperCase(), new Double(0.75));
            exVdwRadii.put("Ga".toUpperCase(), new Double(0.53));
            exVdwRadii.put("Ge".toUpperCase(), new Double(0.60));
            exVdwRadii.put("Kr".toUpperCase(), new Double(2.01));
            exVdwRadii.put("Y".toUpperCase(), new Double(0.90));
            exVdwRadii.put("Zr".toUpperCase(), new Double(0.77));
            exVdwRadii.put("Sn".toUpperCase(), new Double(0.71));
            exVdwRadii.put("Sb".toUpperCase(), new Double(2.20));
            exVdwRadii.put("Xe".toUpperCase(), new Double(2.18));
            exVdwRadii.put("La".toUpperCase(), new Double(1.03));
            exVdwRadii.put("Ce".toUpperCase(), new Double(0.87));
            exVdwRadii.put("Fr".toUpperCase(), new Double(1.94));
            exVdwRadii.put("Ra".toUpperCase(), new Double(1.62));
            exVdwRadii.put("Th".toUpperCase(), new Double(1.08));
            // finally, we have a set of elements where the radii are unknown
            // so we use estimates and extrapolations based on web-elements data
            exVdwRadii.put("Nb".toUpperCase(), new Double(0.86));
            exVdwRadii.put("Tc".toUpperCase(), new Double(0.71));
            exVdwRadii.put("Ru".toUpperCase(), new Double(0.82));
            exVdwRadii.put("Rh".toUpperCase(), new Double(0.76));
            exVdwRadii.put("Pd".toUpperCase(), new Double(1.05));
            exVdwRadii.put("Pr".toUpperCase(), new Double(1.11));
            exVdwRadii.put("Nd".toUpperCase(), new Double(1.10));
            exVdwRadii.put("Pm".toUpperCase(), new Double(1.15));
            exVdwRadii.put("Eu".toUpperCase(), new Double(1.31));
            exVdwRadii.put("Tb".toUpperCase(), new Double(1.05));
            exVdwRadii.put("Dy".toUpperCase(), new Double(1.05));
            exVdwRadii.put("Ho".toUpperCase(), new Double(1.04));
            exVdwRadii.put("Er".toUpperCase(), new Double(1.03));
            exVdwRadii.put("Tm".toUpperCase(), new Double(1.02));
            exVdwRadii.put("Lu".toUpperCase(), new Double(1.02));
            exVdwRadii.put("Hf".toUpperCase(), new Double(0.85));
            exVdwRadii.put("Ta".toUpperCase(), new Double(0.86));
            exVdwRadii.put("Re".toUpperCase(), new Double(0.77));
            exVdwRadii.put("Os".toUpperCase(), new Double(0.78));
            exVdwRadii.put("Ir".toUpperCase(), new Double(0.80));
            exVdwRadii.put("Bi".toUpperCase(), new Double(1.17));
            exVdwRadii.put("Po".toUpperCase(), new Double(0.99));
            exVdwRadii.put("At".toUpperCase(), new Double(0.91));
            exVdwRadii.put("Rn".toUpperCase(), new Double(2.50));
            exVdwRadii.put("Ac".toUpperCase(), new Double(1.30));
            exVdwRadii.put("Pa".toUpperCase(), new Double(1.10));
            exVdwRadii.put("Np".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Pu".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Am".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Cm".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Bk".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Cf".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Es".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Fm".toUpperCase(), new Double(1.00));
            exVdwRadii.put("Md".toUpperCase(), new Double(1.00));
            exVdwRadii.put("No".toUpperCase(), new Double(1.00));
        }//}}}
        Double radius = (Double) exVdwRadii.get(element);
        if(radius == null)  return 2.0;
        else                return radius.doubleValue();
    }
//}}}

//{{{ selectBondsBetween
//##############################################################################
    /**
    * Selects matching bonds, retaining their input order.
    * Only bonds that go from AtomStates in srcA which belong to Residues in srcR,
    * to AtomStates in dstR that belong to Residues in dstR (or vice versa), are drawn.
    * @param bonds  the Bonds to select from
    * @param srcA   a Set of AtomStates (may be null for "any")
    * @param dstA   a Set of AtomStates (may be null for "any")
    * @param srcR   a Set of Residues (may be null for "any")
    * @param dstR   a Set of Residues (may be null for "any")
    * @param out    a Collection to append selected bonds to. Will be created if null.
    * @return the Collection holding the selected bonds
    */
    static public Collection selectBondsBetween(Collection bonds, Set srcA, Set dstA, Set srcR, Set dstR, Collection out)
    {
        if(out == null) out = new ArrayList();
        for(Iterator iter = bonds.iterator(); iter.hasNext(); )
        {
            Bond curr = (Bond) iter.next();
            
            // Testing for null vs. maintaining separate implementations that don't test at all
            // produces no measurable performance impact, even for the ribosome.
            
            boolean residuesAllowed = ((srcR == null || srcR.contains(curr.lower.getResidue())) && (dstR == null || dstR.contains(curr.higher.getResidue())))
                                    ||((dstR == null || dstR.contains(curr.lower.getResidue())) && (srcR == null || srcR.contains(curr.higher.getResidue())));
            if(!residuesAllowed) continue;
            
            boolean atomsAllowed    = ((srcA == null || srcA.contains(curr.lower)) && (dstA == null || dstA.contains(curr.higher)))
                                    ||((dstA == null || dstA.contains(curr.lower)) && (srcA == null || srcA.contains(curr.higher)));
            if(!atomsAllowed) continue;
            
            out.add(curr);
        }
        return out;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

