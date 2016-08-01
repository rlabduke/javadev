// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

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
* <code>PhiPsiRotation</code> is a simple implementation of something that 
* allows one to refit a structure by tweaking phi,psi.
*
* The user chooses whether to rotate the rest of the mainchain in the 
* upstream or downstream direction (either is clearly a very gross movement).
*
* This class is based off Ian's CaRotation ("CAiRO") class.
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Sun Jan 19 2009
*/
public class PhiPsiRotation //extends ... implements ...
{
//{{{ Constants
    static SidechainIdealizer sidechainIdealizer = null;
    static
    {
        try { sidechainIdealizer = new SidechainIdealizer(); }
        catch(IOException ex) { ex.printStackTrace(); }
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PhiPsiRotation()
    {
        super();
    }
//}}}

//{{{ makeMobileGroup
//##################################################################################################
    /**
    * Returns a Collection of the residues from <code>r</code> 
    * either to the beginning or end of the mainchain, inclusive
    * or plus or minus a certain number of residues, inclusive.
    */
    public static Collection makeMobileGroup(Model model, Residue r, boolean upstream, int numRes)
    {
        Collection residues = new ArrayList();
        residues.add(r);
        if(upstream)
        {
            Residue prev;
            if(numRes == Integer.MAX_VALUE)
                for(prev = r.getPrev(model); prev != null; prev = prev.getPrev(model))
                    residues.add(prev);
            else // specific # residues
                for(prev = r.getPrev(model); prev != null && residues.size() < numRes; prev = prev.getPrev(model))
                    residues.add(prev);
        }
        else // downstream (default)
        {
            Residue next;
            if(numRes == Integer.MAX_VALUE)
                for(next = r.getNext(model); next != null; next = next.getNext(model))
                    residues.add(next);
            else // specific # residues
                for(next = r.getNext(model); next != null && residues.size() < numRes; next = next.getNext(model))
                    residues.add(next);
        }
        return residues;
    }
//}}}

//{{{ makeConformation
//##################################################################################################
    /**
    * Rotates a collection of residues by theta degrees 
    * around either phi or psi.
    * @throws AtomException if the state is missing a state definition
    *   for any of the mobile atoms.
    */
    public static ModelState makeConformation(Collection residues, ModelState state, double theta, boolean phi, boolean upstream, int numRes, boolean idealizeSC) throws AtomException
    {
        // Find first and last residue in the collection
        Residue first, last;
        Iterator iter = residues.iterator();
        first = last = (Residue)iter.next();
        while(iter.hasNext()) last = (Residue)iter.next();
        
        // Find atoms to rotate around
        Atom atom1 = null, atom2 = null;
        
        if      ( upstream &&  phi) { atom1 = first.getAtom(" N  ");
                                      atom2 = first.getAtom(" CA "); }
        else if ( upstream && !phi) { atom1 = first.getAtom(" CA ");
                                      atom2 = first.getAtom(" C  "); }
        else if (!upstream &&  phi) { atom1 = first.getAtom(" N  ");
                                      atom2 = first.getAtom(" CA "); }
        else if (!upstream && !phi) { atom1 = first.getAtom(" CA ");
                                      atom2 = first.getAtom(" C  "); }
        if( upstream &&  phi && (atom1 == null || atom2 == null))
            throw new AtomException("N or CA missing from "+last);
        if( upstream && !phi && (atom1 == null || atom2 == null))
            throw new AtomException("CA or C missing from "+last);
        if(!upstream &&  phi && (atom1 == null || atom2 == null))
            throw new AtomException("N or CA missing from "+first);
        if(!upstream && !phi && (atom1 == null || atom2 == null))
            throw new AtomException("CA or C missing from "+first);
        
        // Do the rotation
        Atom[]      atoms   = getMobileAtoms(residues, phi, upstream, numRes);
        Transform   rot     = new Transform().likeRotation(state.get(atom1), state.get(atom2), theta);
        ModelState  rv      = transformAtoms(rot, atoms, state);
        
        // Idealize central sidechain (opt'l)
        if(idealizeSC)   rv = sidechainIdealizer.idealizeSidechain(first, rv);
        
        return rv.createCollapsed(state);
    }
//}}}

//{{{ getMobileAtoms
//##################################################################################################
    static Atom[] getMobileAtoms(Collection residues, boolean phi, boolean upstream, int numRes)
    {
        ArrayList   atoms   = new ArrayList();
        Iterator    iter    = residues.iterator();
        
        Residue     r       = (Residue)iter.next();
        
        if(!upstream && phi)
        {
            // Add the first-in-sequence (first-in-array) mobile residue, C and O only
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom    a   = (Atom)ai.next();
                String  an  = a.getName();
                if(an.equals(" C  ") || an.equals(" O  "))
                    atoms.add(a);
            }
        }
        else if(upstream && phi)
        {
            // Add the last-in-sequence (first-in-array) mobile residue, H only
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom    a   = (Atom)ai.next();
                String  an  = a.getName();
                if(an.equals(" H  "))
                    atoms.add(a);
            }
        }
        else if(upstream && !phi)
        {
            // Add the last-in-sequence (first-in-array) mobile residue, N and H only
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom    a   = (Atom)ai.next();
                String  an  = a.getName();
                if(an.equals(" N  ") || an.equals(" H  "))
                    atoms.add(a);
            }
        }
        else //if(!upstream && !phi)
        {
            // Add the first-in-sequence (first-in-array) mobile residue, O only
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom    a   = (Atom)ai.next();
                String  an  = a.getName();
                if(an.equals(" O  "))
                    atoms.add(a);
            }
        }
        
        // Add all atoms for the rest of residues
        int resCount = 1;
        while(iter.hasNext())
        {
            if(resCount >= numRes) break;
            r = (Residue)iter.next();
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom    a   = (Atom)ai.next();
                atoms.add(a);
            }
            resCount++;
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

