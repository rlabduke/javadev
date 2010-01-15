// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>VirtualBackbone</code> is responsible for calculating Bond objects for
* a protein C-alpha trace or a nucleic acid virtual backbone.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Oct 15 17:19:42 EDT 2005
*/
public class VirtualBackbone //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Model           model;
    ModelState[]    states;
    ResClassifier   resC;
    Map             atomIndices;    // AtomState to Integer
    int             nextAtomIndex = 0;
    
    Collection      protBonds = null;
    Collection      nucBonds = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public VirtualBackbone(Model model, Collection modelStates, ResClassifier classifier)
    {
        super();
        this.model          = model;
        this.states         = (ModelState[]) modelStates.toArray( new ModelState[modelStates.size()] );
        this.resC           = classifier;
        this.atomIndices    = new IdentityHashMap();
    }
//}}}

//{{{ getProteinBonds
//##############################################################################
    /**
    * Returns a Collection of Bond objects that can be used to draw
    * protein C-alpha traces.
    */
    public Collection getProteinBonds()
    {
        if(this.protBonds == null)
        {
            // See AtomGraph for speed/mem comparison of TreeSet vs. CheapSet + sort()
            this.protBonds = new CheapSet();
            
            // Walk down the list of protein residues. For each pair that belong to
            // the same chain, check the distance between their C-alphas in every state.
            // Rely on the Set property of protBonds to eliminate duplicates.
            final double maxCaCa = 5.0; // from Prekin; ideal is 3.80
            Atom currCa, prevCa = null;
            for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
            {
                Residue r = (Residue) iter.next();
                if(resC.classify(r) != ResClassifier.PROTEIN) { prevCa = null; continue; }
                
                currCa = r.getAtom(" CA ");
                if(currCa != null && prevCa != null && currCa.getResidue().getChain().equals(prevCa.getResidue().getChain()))
                    bondAllStates(prevCa, currCa, maxCaCa, protBonds);
                
                prevCa = currCa;
            }
            
            Object[] bonds = this.protBonds.toArray();
            this.protBonds = null;
            Arrays.sort(bonds);
            this.protBonds = new FinalArrayList(bonds);
        }
        return this.protBonds;
    }
//}}}

//{{{ getNucAcidBonds
//##############################################################################
    /**
    * Returns a Collection of Bond objects that can be used to draw
    * nucleic acid virtual backbone traces.
    */
    public Collection getNucAcidBonds()
    {
        // C4'(prev) --- P --- C4' --- P(next)
        //                      |
        //                     C1'
        
        if(this.nucBonds == null)
        {
            // See AtomGraph for speed/mem comparison of TreeSet vs. CheapSet + sort()
            this.nucBonds = new CheapSet();
            
            // Walk down the list of nucleic acid residues. For each pair that belong to
            // the same chain, check the distance between their P and C4' in every state.
            // Rely on the Set property of nucBonds to eliminate duplicates.
            final double maxC4P = 5.0; // avg. P -- C4' dist is ~ 3.5-4.0, ~ like protein
            Atom P, C1, C4, prevC4 = null;
            for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
            {
                Residue r = (Residue) iter.next();
                if(resC.classify(r) != ResClassifier.NUCACID) { prevC4 = null; continue; }
                
                P  = r.getAtom(" P  ");
                C1 = r.getAtom(" C1*"); if(C1 == null) C1 = r.getAtom(" C1'");
                C4 = r.getAtom(" C4*"); if(C4 == null) C4 = r.getAtom(" C4'");
                
                // Bond from prev C4 to this P
                if(P != null && prevC4 != null && P.getResidue().getChain().equals(prevC4.getResidue().getChain()))
                    bondAllStates(prevC4, P, maxC4P, nucBonds);
                // Bond from this P to this C4
                if(P != null && C4 != null)
                    bondAllStates(P, C4, maxC4P, nucBonds);
                // Bond from this C4 to this C1
                if(C4 != null && C1 != null)
                    bondAllStates(C4, C1, maxC4P, nucBonds); // this dist is too lenient
                    
                prevC4 = C4;
            }
            
            Object[] bonds = this.nucBonds.toArray();
            this.nucBonds = null;
            Arrays.sort(bonds);
            this.nucBonds = new FinalArrayList(bonds);
        }
        return this.nucBonds;
    }
//}}}

//{{{ bondAllStates, getIndex
//##############################################################################
    private void bondAllStates(Atom atom1, Atom atom2, double maxDist, Collection outputBonds)
    {
        final double maxDist2 = maxDist * maxDist;
        for(int i = 0; i < states.length; i++)
        {
            try
            {
                AtomState as1 = states[i].get(atom1);
                AtomState as2 = states[i].get(atom2);
                if(as1.sqDistance(as2) <= maxDist2)
                    outputBonds.add( new Bond(as1, getIndex(as1), as2, getIndex(as2)) );
            }
            catch(AtomException ex) {}
        }
    }

    /**
    * Since residues are scanned in input order, C-alphas, phosphates, etc.
    * will be encountered in order and thus their indices will be assigned
    * so that the Bonds sort into the desired "normal" order, without us
    * explicitly having to assign an index to every AtomState in the model
    * (which DOES have to be done by AtomGraph; compare and contrast the code).
    */
    private int getIndex(AtomState as)
    {
        Integer i = (Integer) atomIndices.get(as);
        if(i == null)
        {
            i = new Integer(nextAtomIndex++);
            atomIndices.put(as, i);
        }
        return i.intValue();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

