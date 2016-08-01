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
* <code>AtomClassifier</code> divides AtomStates into the groups needed in
* kinemage drawing: heavy atoms vs. hydrogens and
* (for proteins and nucleic acids) mainchain vs. sidechain.
* Waters and metals are grouped separately from ohets and unknowns,
* because the former shouldn't ever be forming bonds to anything...
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Sep 29 16:26:38 EDT 2005
*/
public class AtomClassifier //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Collection      atomStates;
    ResClassifier   resClassifier;
    
    // First, the disjoint sets:
    public Set mcHeavy      = new CheapSet(new IdentityHashFunction());
    public int mcNotCa      = 0;
    public Set mcHydro      = new CheapSet(new IdentityHashFunction());
    public Set scHeavy      = new CheapSet(new IdentityHashFunction());
    public Set scHydro      = new CheapSet(new IdentityHashFunction());
    public Set watHeavy     = new CheapSet(new IdentityHashFunction());
    public Set watHydro     = new CheapSet(new IdentityHashFunction());
    public Set metal        = new CheapSet(new IdentityHashFunction());
    public Set hetHeavy     = new CheapSet(new IdentityHashFunction());
    public Set hetHydro     = new CheapSet(new IdentityHashFunction());

    // Now, the unions of the above:
    public Set bioHeavy     = new CheapSet(new IdentityHashFunction()); // mcHeavy + scHeavy
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AtomClassifier(Collection atomStates, ResClassifier resClassifier)
    {
        super();
        this.atomStates     = atomStates;
        this.resClassifier  = resClassifier;
        
        // First, divide all AtomStates into one of eight disjoint sets
        for(Iterator iter = atomStates.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            Object clas = resClassifier.classify(as.getResidue());
            boolean isH = as.getElement().equals("H");
            if(clas == ResClassifier.PROTEIN || clas == ResClassifier.NUCACID)
            {
                if(Util.isMainchain(as))
                {
                  if(isH) { mcHydro.add(as); }
                  else {   
                    mcHeavy.add(as);
                    if (!as.getName().equals(" CA ")) mcNotCa++;
                  }
                }
                else // sidechain
                {
                    if(isH) scHydro.add(as);
                    else    scHeavy.add(as);
                }
            }
            else if(clas == ResClassifier.WATER)
            {
                if(isH)     watHydro.add(as);
                else        watHeavy.add(as);
            }
            else if(clas == ResClassifier.METAL)
            {
                            metal.add(as);
            }
            else // OHET and UNKNOWN
            {
                if(isH)     hetHydro.add(as);
                else        hetHeavy.add(as);
            }
        }// for each atom state
        
        // Now, create the unions of those sets for convenience:
        bioHeavy.addAll(mcHeavy);
        bioHeavy.addAll(scHeavy);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

