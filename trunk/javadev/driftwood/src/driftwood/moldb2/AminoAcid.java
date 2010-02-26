// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
//}}}
/**
* <code>AminoAcid</code> is a class for working with Residues
* that model some sort of amino acid.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jun 20 14:56:39 EDT 2003
*/
public class AminoAcid //extends ... implements ...
{
//{{{ Constants
    static final String AA_NAMES = "GLY,ALA,VAL,LEU,ILE,PRO,PHE,TYR,TRP,SER,THR,CYS,MET,MSE,LYS,HIS,ARG,ASP,ASN,GLN,GLU";
    
    static final Set BACKBONE_ATOM_NAMES = new HashSet();
    static
    {
        BACKBONE_ATOM_NAMES.add(" N  ");
        BACKBONE_ATOM_NAMES.add(" H  ");
        BACKBONE_ATOM_NAMES.add("1H  ");
        BACKBONE_ATOM_NAMES.add("2H  ");
        BACKBONE_ATOM_NAMES.add("3H  ");
        BACKBONE_ATOM_NAMES.add(" CA ");
        BACKBONE_ATOM_NAMES.add(" HA ");
        BACKBONE_ATOM_NAMES.add("1HA ");
        BACKBONE_ATOM_NAMES.add("2HA ");
        BACKBONE_ATOM_NAMES.add(" C  ");
        BACKBONE_ATOM_NAMES.add(" O  ");
        BACKBONE_ATOM_NAMES.add(" OXT");
    }

    private static final Map threeToOneLetter;
    private static final Map oneToThreeLetter;

    static
    {
        Map map = new HashMap();
        map.put("gly", "G");
        map.put("ala", "A");
        map.put("val", "V");
        map.put("leu", "L");
        map.put("ile", "I");
        map.put("met", "M");
        map.put("pro", "P");
        map.put("phe", "F");
        map.put("trp", "W");
        map.put("ser", "S");
        map.put("thr", "T");
        map.put("asn", "N");
        map.put("gln", "Q");
        map.put("tyr", "Y");
        map.put("cys", "C");
        map.put("lys", "K");
        map.put("arg", "R");
        map.put("his", "H");
        map.put("asp", "D");
        map.put("glu", "E");
        threeToOneLetter = Collections.unmodifiableMap(map);

        map = new HashMap();
        map.put("G", "gly");
        map.put("A", "ala");
        map.put("V", "val");
        map.put("L", "leu");
        map.put("I", "ile");
        map.put("M", "met");
        map.put("P", "pro");
        map.put("F", "phe");
        map.put("W", "trp");
        map.put("S", "ser");
        map.put("T", "thr");
        map.put("N", "asn");
        map.put("Q", "gln");
        map.put("Y", "tyr");
        map.put("C", "cys");
        map.put("K", "lys");
        map.put("R", "arg");
        map.put("H", "his");
        map.put("D", "asp");
        map.put("E", "glu");
        oneToThreeLetter = Collections.unmodifiableMap(map);
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor (not to be instantiated right now)
    */
    private AminoAcid()
    {
        super();
    }
//}}}

//{{{ translate
//##################################################################################################
    /**
    * Returns the translation of the amino acid name, either from one letter to 
    * three letters, or vice versa.  Returns "X" if three letter code is unknown, 
    * or "unk" if one letter code is unknown.
    */
    static public String translate(String code)
    {
        String trans = "";
        if(code.length() >= 3)
        {
            if(threeToOneLetter.containsKey(code))
                trans = (String) threeToOneLetter.get(code);
            else
                trans = "X";
        }
        else
        {
            if(oneToThreeLetter.containsKey(code))
                trans = (String) oneToThreeLetter.get(code);
            else
                trans = "unk";
        }
        return trans;
    }
//}}}

//{{{ getTau, getTauDeviation
//##################################################################################################
    /**
    * Return the tau angle (N-CA-C) of this residue, in degrees.
    * @throws AtomException if a required Atom or AtomState
    *   cannot be found in the supplied data.
    */
    static public double getTau(Residue res, ModelState state) throws AtomException
    {
        AtomState n, ca, c;
        // One of these will throw AtomException if
        // res.getAtom() returns null.
        n   = state.get( res.getAtom(" N  ") );
        ca  = state.get( res.getAtom(" CA ") );
        c   = state.get( res.getAtom(" C  ") );
        return Triple.angle(n, ca, c);
    }
    
    /**
    * Returns the tau angle deviation from ideality, in degrees.
    * Ideal values are taken from Engh &amp; Huber (1991).
    * @throws AtomException if a required Atom or AtomState
    *   cannot be found in the supplied data.
    */
    static public double getTauDeviation(Residue res, ModelState state) throws AtomException
    {
        double taudev, tau = getTau(res, state);
             if(res.getName().equals("GLY"))    taudev = tau - 112.5;
        else if(res.getName().equals("PRO"))    taudev = tau - 111.8;
        else                                    taudev = tau - 111.2;
        return taudev;
    }
//}}}

//{{{ getPhi, getPsi
//##################################################################################################
    /**
    * Return the phi angle (C-N-CA-C) of this residue, in degrees.
    * @throws AtomException if a required Atom or AtomState
    *   cannot be found in the supplied data.
    * @throws ResidueException if the preceding Residue
    *   cannot be found.
    */
    static public double getPhi(Model model, Residue res, ModelState state) throws ResidueException, AtomException
    {
        Residue prev = res.getPrev(model);
        if(prev == null)
            throw new ResidueException("Can't calculate PHI without a preceding residue");
        
        AtomState prevC, n, ca, c;
        // One of these will throw AtomException if
        // getAtom() returns null.
        prevC   = state.get(prev.getAtom(" C  ") );
        n       = state.get( res.getAtom(" N  ") );
        ca      = state.get( res.getAtom(" CA ") );
        c       = state.get( res.getAtom(" C  ") );
        
        if(prevC.sqDistance(n) > 4.0)
            throw new ResidueException("Preceding residue is too far away to be covalently bonded");
        
        return Triple.dihedral(prevC, n, ca, c);
    }

    /**
    * Return the psi angle (N-CA-C-N) of this residue, in degrees.
    * @throws AtomException if a required Atom or AtomState
    *   cannot be found in the supplied data.
    * @throws ResidueException if the following Residue
    *   cannot be found.
    */
    static public double getPsi(Model model, Residue res, ModelState state) throws ResidueException, AtomException
    {
        Residue next = res.getNext(model);
        if(next == null)
            throw new ResidueException("Can't calculate PSI without a following residue");
        
        AtomState n, ca, c, nextN;
        // One of these will throw AtomException if
        // getAtom() returns null.
        n       = state.get( res.getAtom(" N  ") );
        ca      = state.get( res.getAtom(" CA ") );
        c       = state.get( res.getAtom(" C  ") );
        nextN   = state.get(next.getAtom(" N  ") );
        
        if(c.sqDistance(nextN) > 4.0)
            throw new ResidueException("Following residue is too far away to be covalently bonded");

        return Triple.dihedral(n, ca, c, nextN);
    }
//}}}

//{{{ isPrepro
//##################################################################################################
    /**
    * Indicates whether this residue precedes a proline.
    * This is true iff there is a next residue with the name "PRO"
    * and this residue's C is less than 2.0 Angstroms from
    * the next residue's N.
    * @throws AtomException if a required Atom or AtomState
    *   cannot be found in the supplied data.
    */
    static public boolean isPrepro(Model model, Residue res, ModelState state)
    {
        Residue next = res.getNext(model);
        if(next == null) return false;
        
        if(next.getName().equals("PRO"))
        {
            try
            {
                AtomState thisC, nextN;
                nextN = state.get( next.getAtom(" N  ") );
                thisC = state.get(  res.getAtom(" C  ") );
                if(thisC.sqDistance(nextN) < 4.0) return true;
                else return false;
            }
            catch(AtomException ex) { return false; }
        }
        else return false;
    }
//}}}

//{{{ isCisPeptide
//##################################################################################################
    /**
    * Indicates whether this residue has a cis peptide bond 
    * to the preceding residue.
    * This is true iff there is a previous residue and omega, 
    * Ca(i-1)-C(i-1)-N(i)-Ca(i), is within 30 degrees of 0.
    * @throws AtomException if a required Atom or AtomState
    *   cannot be found in the supplied data.
    */
    static public boolean isCisPeptide(Model model, Residue res, ModelState state)
    {
        Residue prev = res.getPrev(model);
        if(prev == null) return false;
        
        try
        {
            AtomState prevCA, prevC, thisN, thisCA;
            prevCA = state.get( prev.getAtom(" CA ") );
            prevC  = state.get( prev.getAtom(" C  ") );
            thisN  = state.get(  res.getAtom(" N  ") );
            thisCA = state.get(  res.getAtom(" CA ") );
            double omega = Triple.dihedral(prevCA, prevC, thisN, thisCA);
            if(omega > -30 && omega < 30) return true;
            else return false;
        }
        catch(AtomException ex) { return false; }
    }
//}}}

//{{{ isBackbone, isAminoAcid
//##################################################################################################
    /** Returns true if this atom, based on its name, is part of the polypeptide backbone. */
    static public boolean isBackbone(Atom a)
    {
        return BACKBONE_ATOM_NAMES.contains(a.getName());
    }
    
    /** Returns true if this residue, based on its name, is a standard amino acid (20 + selenomet). */
    static public boolean isAminoAcid(Residue r)
    {
        return (AA_NAMES.indexOf(r.getName()) != -1);
    }

    static public boolean isAminoAcid(String s)
    {
        s = s.toUpperCase();
        return (AA_NAMES.indexOf(s) != -1);
    }
    
    // more flexible search for whether something is an AA (e.g. for alt conf agly bgly, etc)
    static public boolean isExtendedAminoAcid(String s) {
      boolean isEAA = false;
      s = s.toUpperCase();
      String[] coreAAs = AA_NAMES.split(",");
      for (int i = 0; i < coreAAs.length; i++) {
        String aa = coreAAs[i];
        if (s.indexOf(aa) != -1) {
          isEAA = true;
        }
      }
      return isEAA;
    }
    
    public static String getAAName(String s) {
      s = s.toUpperCase();
      String[] coreAAs = AA_NAMES.split(",");
      for (int i = 0; i < coreAAs.length; i++) {
        String aa = coreAAs[i];
        int aaInd = s.indexOf(aa);
        if (aaInd != -1) {
          return s.substring(aaInd - 1, aaInd + 3);
        }
      }
      return "UNK";
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

