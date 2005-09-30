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
* (for proteins and nucleic acids) backbone vs. sidechain.
* Waters and ions are grouped separately from ohets and unknowns,
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
    public Set bbHeavy      = new CheapSet(new IdentityHashFunction());
    public Set bbHydro      = new CheapSet(new IdentityHashFunction());
    public Set scHeavy      = new CheapSet(new IdentityHashFunction());
    public Set scHydro      = new CheapSet(new IdentityHashFunction());
    public Set watHeavy     = new CheapSet(new IdentityHashFunction());
    public Set watHydro     = new CheapSet(new IdentityHashFunction());
    public Set ion          = new CheapSet(new IdentityHashFunction());
    public Set hetHeavy     = new CheapSet(new IdentityHashFunction());
    public Set hetHydro     = new CheapSet(new IdentityHashFunction());

    // Now, the unions of the above:
    public Set bioHeavy     = new CheapSet(new IdentityHashFunction()); // bbHeavy + scHeavy
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
            if(clas == ResClassifier.PROTEIN || clas == ResClassifier.NUCACID)
            {
                if(Util.isMainchain(as))
                {
                    if(Util.isH(as))    bbHydro.add(as);
                    else                bbHeavy.add(as);
                }
                else // sidechain
                {
                    if(Util.isH(as))    scHydro.add(as);
                    else                scHeavy.add(as);
                }
            }
            else if(clas == ResClassifier.WATER)
            {
                if(Util.isH(as))    watHydro.add(as);
                else                watHeavy.add(as);
            }
            else if(clas == ResClassifier.ION)
            {
                                    ion.add(as);
            }
            else // OHET and UNKNOWN
            {
                if(Util.isH(as))    hetHydro.add(as);
                else                hetHeavy.add(as);
            }
        }// for each atom state
        
        // Now, create the unions of those sets for convenience:
        bioHeavy.addAll(bbHeavy);
        bioHeavy.addAll(scHeavy);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

