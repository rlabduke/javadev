// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb;

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
* <code>AABackbone</code> is a utility class for dealing
* with the backbone of an amino acid.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 18 09:53:55 EST 2003
*/
public class AABackbone implements Cloneable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Residue source;
    
    // Required atoms
    public Atom     CA;     // the C-alpha
    public Atom     N;      // the amide N
    public Atom     C;      // the carbonyl C
    public Atom     O;      // the carbonyl O

    // Optional atoms
    public Atom     H;      // the amide H
    public Atom     HA;     // the central H
    public Atom     HA1;    // the central H (Gly) -- 1HA_
    public Atom     HA2;    // the central H (Gly) -- 2HA_
    public Atom     CB;     // the C-beta
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor.
    * All atoms are simple references to the originals -- use clone() to make a copy.
    * Throws a NoSuchElementException if any of the backbone heavy atoms (CA, N, C, O) is missing.
    */
    public AABackbone(Residue r)
    {
        if(r == null) throw new IllegalArgumentException("Must specify a source residue");
        source = r;
        
        CA  = source.getAtom(" CA ");
        N   = source.getAtom(" N  ");
        C   = source.getAtom(" C  ");
        O   = source.getAtom(" O  ");
        
        H = HA = HA1 = HA2 = CB = null;
            try { H     = source.getAtom(" H  "); } catch(NoSuchElementException ex) {}
        
        if(source.getType().toUpperCase().equals("GLY"))
        {
            try { HA1   = source.getAtom("1HA "); } catch(NoSuchElementException ex) {}
            try { HA2   = source.getAtom("2HA "); } catch(NoSuchElementException ex) {}
        }
        else
        {
            try { CB    = source.getAtom(" CB "); } catch(NoSuchElementException ex) {}
            try { HA    = source.getAtom(" HA "); } catch(NoSuchElementException ex) {}
        }
    }
//}}}

//{{{ getResidue
//##################################################################################################
    public Residue getResidue()
    { return source; }
//}}}

//{{{ isPrepro
//##################################################################################################
    /** Indicates whether this residue precedes a proline. */
    public boolean isPrepro()
    {
        Residue next = source.getNext();
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
    * Returns phi for the source residue.
    * If this residue is at the end of a chain, NaN is returned.
    */
    public double getPhi()
    {
        try {
            Residue prev = source.getPrev();
            if(prev != null)
            {
                Atom prevC = prev.getAtom(" C  ");
                if(this.N.sqDistance(prevC) < 9.0) return Atom.dihedral(prevC, N, CA, C);
            }
        } catch(NoSuchElementException ex) {}
        
        return Double.NaN;
    }

    /**
    * Returns psi for the source residue.
    * If this residue is at the end of a chain, NaN is returned.
    */
    public double getPsi()
    {
        try {
            Residue next = source.getNext();
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

//{{{ clone
//##################################################################################################
    /**
    * Produces a semi-deep copy of this backbone segment.
    * A reference to the same Residue will be maintained,
    * but each Atom reference will be replaced with its clone()
    * @throws UnsupportedOperationException if cloning fails
    */
    public Object clone()
    {
        try
        {
            AABackbone c = (AABackbone)super.clone();
            c.CA  = (Atom)CA.clone();
            c.N   = (Atom)N.clone();
            c.C   = (Atom)C.clone();
            c.O   = (Atom)O.clone();
            if(H != null)   c.H   = (Atom)H.clone();
            if(HA != null)  c.HA  = (Atom)HA.clone();
            if(HA1 != null) c.HA1 = (Atom)HA1.clone();
            if(HA2 != null) c.HA2 = (Atom)HA2.clone();
            if(CB != null)  c.CB  = (Atom)CB.clone();
            return c;
        }
        catch(CloneNotSupportedException ex)
        {
            UnsupportedOperationException ex2 = new UnsupportedOperationException("Clone failed unexpectedly");
            ex2.initCause(ex);
            throw ex2;
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

