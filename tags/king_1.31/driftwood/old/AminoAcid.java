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
//import driftwood.*;
//}}}
/**
* <code>AminoAcid</code> is a utility class for dealing
* with the backbone and side chain of an amino acid.
*
* INCOMPLETE
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 18 09:53:55 EST 2003
*/
public class AminoAcid
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** The residue this amino acid is wrapping */
    public final Residue    parent;
    
    /** The C-alpha (required) */
    public final Atom       CA;
    /** The amide N (required) */
    public final Atom       N;
    /** The carbonyl C (required) */
    public final Atom       C;
    /** The carbonyl O (required) */
    public final Atom       O;

    /** The amide H (optional) */
    public final Atom       H;
    /** The central H (optional) */
    public final Atom       HA;
    /** The central H (Gly) -- 1HA_ (optional) */
    public final Atom       HA1;
    /** The central H (Gly) -- 2HA_ (optional) */
    public final Atom       HA2;
    /** The C-beta (optional) */
    public final Atom       CB;
    
    /** All the atoms of the mainchain, including hydrogens, excluding the C-beta */
    public final Map        mainchain;
    /** All the atoms of the sidechain, including hydrogens, starting from the C-beta */
    public final Map        sidechain;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * All atoms are simple references to the originals.
    */
    public AminoAcid(Residue r)
    {
        parent = r;
        
        // Get essential mainchain atoms
        CA  = parent.getAtom(" CA ");
        N   = parent.getAtom(" N  ");
        C   = parent.getAtom(" C  ");
        O   = parent.getAtom(" O  ");
        
        // Get hydrogens and C-betas
        H = HA = HA1 = HA2 = CB = null;
            try { H     = parent.getAtom(" H  "); } catch(NoSuchElementException ex) {}
        
        if(parent.getType().toUpperCase().equals("GLY"))
        {
            try { HA1   = parent.getAtom("1HA "); } catch(NoSuchElementException ex) {}
            try { HA2   = parent.getAtom("2HA "); } catch(NoSuchElementException ex) {}
        }
        else
        {
            try { CB    = parent.getAtom(" CB "); } catch(NoSuchElementException ex) {}
            try { HA    = parent.getAtom(" HA "); } catch(NoSuchElementException ex) {}
        }
        
        // Get other side chain atoms (and the CB again)
        this.atomMap    = new LinkedHashMap();
        this.sc         = new LinkedHashMap();
        Atom            atom;
        Collection      atomSet = parent.getAtomSet();
        for(Iterator iter = atomSet.iterator(); iter.hasNext(); )
        {
            atom = (Atom)iter.next();
            atomMap.put(atom.getID(), atom);
            if(atom != CA && atom != N && atom != C && atom != O
            && atom != H && atom != HA1 && atom != HA2 && atom != HA)
            {
                sc.put(atom.getID(), atom);
            }
        }
    }
//}}}

//{{{ getResidue
//##################################################################################################
    public Residue getResidue()
    { return parent; }
//}}}

//{{{ isPrepro
//##################################################################################################
    /** Indicates whether this residue precedes a proline. */
    public boolean isPrepro()
    {
        Residue next = parent.getNext();
        if(next == null) return false;
        if(next.getType().toUpperCase().equals("PRO"))
        {
            try
            {
                Atom nextN = next.getAtom(" N  ");
                //if(this.C.distance(nextN) < 2.0) return true;
                if(this.C.sqDistance(nextN) < 4.0) return true;
                else return false;
            }
            catch(NoSuchElementException ex) { return false; }
        }
        else return false;
    }
//}}}

//{{{ getPhi, getPsi
//##################################################################################################
    /**
    * Returns phi for the parent residue.
    * If this residue is at the end of a chain, NaN is returned.
    */
    public double getPhi()
    {
        try {
            Residue prev = parent.getPrev();
            if(prev != null)
            {
                Atom prevC = prev.getAtom(" C  ");
                if(this.N.sqDistance(prevC) < 9.0) return Atom.dihedral(prevC, N, CA, C);
            }
        } catch(NoSuchElementException ex) {}
        
        return Double.NaN;
    }

    /**
    * Returns psi for the parent residue.
    * If this residue is at the end of a chain, NaN is returned.
    */
    public double getPsi()
    {
        try {
            Residue next = parent.getNext();
            if(next != null)
            {
                Atom nextN = next.getAtom(" N  ");
                if(this.C.sqDistance(nextN) < 9.0) return Atom.dihedral(N, CA, C, nextN);
            }
        } catch(NoSuchElementException ex) {}
        
        return Double.NaN;
    }
//}}}

//{{{ getTau
//##################################################################################################
    /** Returns the tau angle (N-CA-C) for this residue */
    public double getTau()
    {
        return Atom.angle(N, CA, C);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

