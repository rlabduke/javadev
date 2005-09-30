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
            return name.equals("H");
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

//{{{ selectBondsBetween
//##############################################################################
    /**
    * Selects bonds that bridge the src and dst sets. Bonds will be returned
    * in the same order they were input.
    * @param bonds  a bunch of Bond objects, of which 0+ will be selected.
    * @param src    AtomStates the bonds are allowed to "originate" from.
    *   (Bonds are symmetrical, so there's no difference b/t originate and terminate.)
    *   This should probably be a collection that uses identity for contains()
    *   rather than equals(), such as CheapMap(IdentityHashFunction).
    * @param dst    AtomStates the bonds are allowed to "terminate" at.
    *   This and src can be exchanged and the results will be identical.
    */
    static public Collection selectBondsBetween(Collection bonds, Set src, Set dst)
    {
        //SortedSet out = new TreeSet();
        Collection out = new ArrayList();
        for(Iterator iter = bonds.iterator(); iter.hasNext(); )
        {
            Bond bond = (Bond) iter.next();
            if((src.contains(bond.lower) && dst.contains(bond.higher))
            || (dst.contains(bond.lower) && src.contains(bond.higher)))
            {
                out.add(bond);
            }
        }
        return out;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

