// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.cairo;

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
import driftwood.util.SoftLog;
import chiropraxis.sc.*;
//}}}
/**
* <code>CaRotation</code> ("CAiRO") is the first and simplest
* implementation of something that allows one to refit a structure
* by rotating atoms around any axis conecting two C-alphas.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 30 11:28:15 EDT 2003
*/
public class CaRotation //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public CaRotation()
    {
        super();
    }
//}}}

//{{{ makeMobileGroup
//##################################################################################################
    /**
    * Returns a Collection of the residues between r1 and r2, inclusive.
    * The order of r1 and r2 doesn't matter.
    * @throws IllegalArgumentException if no path can be found
    *   that connects r1 and r2.
    */
    public static Collection makeMobileGroup(Residue r1, Residue r2)
    {
        // who's on first?
        Residue first, last;
        if(r1.compareTo(r2) < 0)
        { first = r1; last = r2; }
        else
        { first = r2; last = r1; }
        
        Collection residues = new ArrayList();
        residues.add(first);
        Residue next;
        for(next = first.getNext(); next != null && !next.equals(last); next = next.getNext())
        {
            residues.add(next);
        }
        if(next == null)
            throw new IllegalArgumentException("Cannot connect residues "+first+" and "+last);
        residues.add(last);
        
        return residues;
    }
//}}}

//{{{ makeConformation
//##################################################################################################
    /**
    * Rotates a collection of residues (as from
    * {@link #makeMobileGroup(Residue, Residue)})
    * by theta degrees around the axis through the
    * C-alphas of the terminal residues.
    * @throws ResidueException if either one of the terminal residues
    *   is missing a C-alpha.
    * @throws AtomException if the state is missing a state definition
    *   for any of the mobile atoms.
    */
    public static ModelState makeConformation(Collection residues, ModelState state, double theta)
    {
        // Find first and last residue in the collection
        Residue first, last;
        Iterator iter = residues.iterator();
        first = last = (Residue)iter.next();
        while(iter.hasNext()) last = (Residue)iter.next();
        
        // Find atoms to rotate around
        Atom firstCA, lastCA;
        firstCA = first.getAtom(" CA ");
        lastCA  =  last.getAtom(" CA ");
        if(firstCA == null || lastCA == null)
            throw new ResidueException("C-alpha is missing from "+first+" or "+last);
        
        // do the rotation
        Atom[]      atoms   = getMobileAtoms(residues);
        Transform   rot     = new Transform().likeRotation(state.get(firstCA), state.get(lastCA), theta);
        ModelState  rv      = transformAtoms(rot, atoms, state);
        
        // fix the sidechains
        rv = SidechainIdealizer.idealizeCB(first, rv);
        rv = SidechainIdealizer.idealizeCB(last, rv);
        
        return rv.createCollapsed(state);
    }
//}}}

//{{{ twistPeptides
//##################################################################################################
    /**
    * A more efficient way of doing lots of i to i+1 rotations along a chain.
    * @param residues an array of length L
    * @param thetas an array of length &gt;= (L-1)
    * @throws ResidueException if any of the residues
    *   is missing a C-alpha.
    * @throws AtomException if the state is missing a state definition
    *   for any of the mobile atoms.
    */
    public static ModelState twistPeptides(Residue[] residues, ModelState state, double[] thetas)
    {
        ArrayList   atomList    = new ArrayList();
        Transform   rot         = new Transform();
        ModelState  rv          = new ModelState(state);
        
        for(int i = 0; i < residues.length - 1; i++)
        {
            if(thetas[i] != 0)
            {
                atomList.clear();
                Atom a;
                a = residues[i  ].getAtom(" C  "); if(a != null) atomList.add(a);
                a = residues[i  ].getAtom(" O  "); if(a != null) atomList.add(a);
                a = residues[i+1].getAtom(" N  "); if(a != null) atomList.add(a);
                a = residues[i+1].getAtom(" H  "); if(a != null) atomList.add(a);
                
                // Find atoms to rotate around
                Atom firstCA    = residues[i  ].getAtom(" CA ");
                Atom lastCA     = residues[i+1].getAtom(" CA ");
                if(firstCA == null || lastCA == null)
                    throw new ResidueException("C-alpha is missing from "+residues[i]+" or "+residues[i+1]);
                
                // do the rotation
                Atom[] atoms    = (Atom[])atomList.toArray(new Atom[atomList.size()]);
                rot.likeRotation(rv.get(firstCA), rv.get(lastCA), thetas[i]);
                rv = transformAtoms(rot, atoms, rv); // keep stacking them up
            }
        }
        
        // fix the sidechains
        for(int i = 0; i < residues.length; i++)
        {
            rv = SidechainIdealizer.idealizeCB(residues[i], rv);
        }
        
        return rv.createCollapsed(state);
    }
//}}}

//{{{ getMobileAtoms
//##################################################################################################
    static Atom[] getMobileAtoms(Collection residues)
    {
        ArrayList   atoms   = new ArrayList();
        Iterator    iter    = residues.iterator();
        Residue     r       = (Residue)iter.next();
        
        // Add the first residue except N and H
        for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
        {
            Atom    a   = (Atom)ai.next();
            String  an  = a.getName();
            if(!(an.equals(" N  ") || an.equals(" H  ")))
                atoms.add(a);
        }
        
        // Add all atoms for the rest of residues except the last
        r = (Residue)iter.next();
        while(iter.hasNext())
        {
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom    a   = (Atom)ai.next();
                atoms.add(a);
            }
            r = (Residue)iter.next();
        }
        
        // Add last residue except C and O
        for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
        {
            Atom    a   = (Atom)ai.next();
            String  an  = a.getName();
            if(!(an.equals(" C  ") || an.equals(" O  ")))
                atoms.add(a);
        }
        
        return (Atom[])atoms.toArray(new Atom[atoms.size()]);
    }
//}}}

//{{{ transformAtoms
//##################################################################################################
    /** Applies a Transform to all the Atoms in the given array */
    static ModelState transformAtoms(Transform t, Atom[] atoms, ModelState state)
    {
        ModelState rv = new ModelState(state);
        
        for(int i = 0; i < atoms.length; i++)
        {
            // Named atom may be missing a state in the
            // case of alternate conformations where
            // some sets are incomplete (eg more A's than B's).
            try
            {
                AtomState origState = state.get(atoms[i]);
                AtomState newState  = (AtomState)origState.clone();
                rv.add(newState);
                t.transform(newState);
            }
            catch(AtomException ex)
            { SoftLog.err.println(ex.getMessage()); }
        }
        
        return rv;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

