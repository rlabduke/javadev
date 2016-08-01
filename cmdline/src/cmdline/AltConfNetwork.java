// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.util.Strings;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.data.*;
import molikin.logic.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
//}}}
/**
* <code>AltConfNetwork</code> is a simple representation of 
* an internally correlated alternate conformation network 
* that is independent of other such networks in sequence and space.
*
* <p>Copyright (C) 2012 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu Oct 18 2012
*/
public class AltConfNetwork //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    // Approximate numbers...
    // But at least better than just a flat 3.0 Angstroms cutoff.  (Probably.)
    final private double vdWradiusH     = 1.22; // non-polar H (longer than polar H), from Lindsay's new values
    final private double vdWradiusHeavy = 1.8;  // Jane's (very insistent!) suggestion
    final private double probeRadius    = 0.25;
    final private double touchDistHH         = vdWradiusH     + 2*probeRadius + vdWradiusH;
    final private double touchDistHeavyH     = vdWradiusHeavy + 2*probeRadius + vdWradiusH;
    final private double touchDistHeavyHeavy = vdWradiusHeavy + 2*probeRadius + vdWradiusHeavy;
//}}}

//{{{ Variable definitions
//##############################################################################
    private  Set  residues;
    private  Set  alts;     // e.g. 'A' or 'B', but not ' '
//}}}
        
//{{{ Constructor(s)
//##############################################################################
    public AltConfNetwork()
    {
        super();
        residues  =  new TreeSet<Residue>();
        alts      =  new TreeSet<String>();
    }   
//}}}

//{{{ add/getResidue(s), add/getAlt(s)
//##############################################################################
    public void addResidue(Residue res)
    { residues.add(res); }
    public void addAlt(String alt)
    { alts.add(alt); }
    
    public Collection getResidues()
    { return Collections.unmodifiableCollection(residues); }
    public Collection getAlts()
    { return Collections.unmodifiableCollection(alts); }
//}}}
    
//{{{ subsume
//##############################################################################
    /** Updates this by merging into it the residues and alts of other. */
    public void subsume(AltConfNetwork other)
    {
        for(Iterator iter = other.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            this.residues.add(res);
        }
        for(Iterator iter = other.getAlts().iterator(); iter.hasNext(); )
        {
            String alt = (String) iter.next();
            this.alts.add(alt);
        }
    }
//}}}
        
//{{{ interactsWith
//##############################################################################
    /**
    * Tells whether or not this network interacts with that one.
    * Uses pretty large vdW radii for H and non-H atom types, 
    * so is kind of liberal in saying that two atoms "interact".
    */
    public boolean interactsWith(AltConfNetwork that, Model model)
    {
        for(Iterator thisResIter = this.getResidues().iterator(); thisResIter.hasNext(); )
        {
            Residue thisRes = (Residue) thisResIter.next();
            for(Iterator thatResIter = that.getResidues().iterator(); thatResIter.hasNext(); )
            {
                Residue thatRes = (Residue) thatResIter.next();
                if(thisRes.getNext(model).equals(thatRes) || thatRes.getNext(model).equals(thisRes))
                    return true; // interaction could propagate covalently through mainchain
                for(Iterator thisAtomIter = thisRes.getAtoms().iterator(); thisAtomIter.hasNext(); )
                {
                    Atom thisAtom = (Atom) thisAtomIter.next();
                    for(Iterator thatAtomIter = thatRes.getAtoms().iterator(); thatAtomIter.hasNext(); )
                    {
                        Atom thatAtom = (Atom) thatAtomIter.next();
                        for(Iterator thisStateIter = model.getStates().keySet().iterator(); thisStateIter.hasNext(); )
                        {
                            String thisStateLabel = (String) thisStateIter.next();
                            if(!this.getAlts().contains(thisStateLabel)) continue;
                            ModelState thisState = model.getState(thisStateLabel);
                            for(Iterator thatStateIter = model.getStates().keySet().iterator(); thatStateIter.hasNext(); )
                            {
                                String thatStateLabel = (String) thatStateIter.next();
                                if(!that.getAlts().contains(thatStateLabel)) continue;
                                ModelState thatState = model.getState(thatStateLabel);
                                try
                                {
                                    AtomState thisAtomState = thisState.get(thisAtom);
                                    AtomState thatAtomState = thatState.get(thatAtom);
                                    
                                    // Make sure both atoms have alt confs, because
                                    // the fact that static sidechain bits don't move 
                                    // means they're not differently influenced by different nearby alt confs
                                    if(thisAtomState.getAltConf().equals(" ")
                                    || thatAtomState.getAltConf().equals(" ")) continue;
                                    
                                    double dist = thisAtomState.distance(thatAtomState);
                                    //if(dist < minDist) minDist = dist; // <-- too simplistic
                                    boolean thisH = thisAtom.getElement().equals("H");
                                    boolean thatH = thatAtom.getElement().equals("H");
                                    if(dist < touchDistHH && thisH && thatH)
                                        return true;
                                    else if(dist < touchDistHeavyH && ((thisH && !thatH) || (!thisH && thatH)))
                                        return true;
                                    else if(dist < touchDistHeavyHeavy && !thisH && !thatH)
                                        return true;
                                }
                                catch(AtomException ex) {}
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
//}}}
        
//{{{ toString
//##############################################################################
    public String toString()
    {
        return residues.toString()+" "+alts.toString();
    }
//}}}
}//class

