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

