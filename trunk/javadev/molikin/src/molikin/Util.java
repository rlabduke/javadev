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

//{{{ isH, isQ, isCNO
//##############################################################################
    static public boolean isH(AtomState as)
    {
        String name = as.getName();
        if(name.length() < 2)
            return name.equals("H");
        char c1 = name.charAt(0), c2 = name.charAt(1);
        return ((c2 == 'H' || c2 == 'D')// || c2 == 'T')
            &&  (c1 == ' ' || ('0' <= c1 && c1 <= '9')));
    }
    
    /** True iff this is an NMR pseudo-hydrogen "Q" */
    static public boolean isQ(AtomState as)
    {
        String name = as.getName();
        if(name.length() < 2)
            return name.equals("Q");
        char c1 = name.charAt(0), c2 = name.charAt(1);
        return ((c2 == 'Q')
            &&  (c1 == ' ' || ('0' <= c1 && c1 <= '9')));
    }
    
    /**
    * Returns true iff this atom appears to be carbon, nitrogen, or oxygen
    * based on its atom name.
    */
    static public boolean isCNO(AtomState as)
    {
        String name = as.getName();
        if(name.length() < 2)
            return name.equals("C") || name.equals("N") || name.equals("O");
        char c1 = name.charAt(0), c2 = name.charAt(1);
        return ((c2 == 'C' || c2 == 'N' || c2 == 'O')
            &&  (c1 == ' ' || ('0' <= c1 && c1 <= '9')));
    }
//}}}

//{{{ isMainchain, isWater
//##############################################################################
    /** Based on Prekin PKINCSBS.c decidemainside() */
    static String mcPattern = ".N[ T].|.C[A ].|.O .|.OXT|[^2][HDQ][A ] |.[HDQ].['*]|.P  |.O[12]P|.[CO][1-5]['*]";
    //                                                   ^^^^
    //                              makes one Gly H sidechain, the other mainchain
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
    static String nucacidPattern = "  C|  G|  A|  T|  U|CYT|GUA|ADE|THY|URA|URI|CTP|CDP|CMP|GTP|GDP|GMP|ATP|ADP|AMP|TTP|TDP|TMP|UTP|UDP|UMP|GSP|H2U|PSU|1MG|2MG|M2G|5MC|5MU|T6A|1MA|RIA|OMC|OMG| YG|  I|7MG";
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

//{{{ isS, isDisulfide
//##############################################################################
    static public boolean isS(AtomState as)
    {
        String name = as.getName();
        if(name.length() < 2)
            return name.equals("S");
        char c1 = name.charAt(0), c2 = name.charAt(1);
        return ((c2 == 'S')
            &&  (c1 == ' ' || ('0' <= c1 && c1 <= '9')));
    }
    
    /** AtomState fields of the Bond must be filled for this to work (ie non-null). */
    static public boolean isDisulfide(Bond bond)
    {
        return (isS(bond.lower) && isS(bond.higher));
    }
//}}}

//{{{ getElement, getElementColor
//##############################################################################
    /**
    * Returns our best guess at the element symbol (1 or 2 chars, uppercase)
    */
    static public String getElement(AtomState as)
    {
        String name = as.getName();
        if(name.length() < 2)
            return name;
        char c1 = name.charAt(0), c2 = name.charAt(1);
        if('A' <= c1 && c1 <= 'Z')  return name.substring(0,2);
        else if(c2 == 'D')          return "H"; // D is the isotope, not the element
        else                        return name.substring(1,2);
    }
    
    static Map elementColors = null;
    /**
    * Given the element symbol (1 or 2 chars, uppercase) this returns
    * the standard kinemage color for it.
    */
    static public String getElementColor(String element)
    {
        if(elementColors == null)
        {
            elementColors = new HashMap();
            elementColors.put("H", "gray");
            elementColors.put("C", "white");
            elementColors.put("N", "sky");
            elementColors.put("O", "red");
            elementColors.put("S", "yellow");
            elementColors.put("P", "gold");
            // These ~ borrowed from RasMol
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
        }
        String color = (String) elementColors.get(element);
        if(color == null)   return "hotpink";
        else                return color;
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

