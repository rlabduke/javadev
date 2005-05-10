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
import driftwood.moldb2.*;
//}}}
/**
* <code>Util</code> defines static utility functions for making atom and bond
* selections and other common tasks when using Molikin classes.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May 10 09:33:51 EDT 2005
*/
public class Util //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    private Util()
    {
        super();
    }
//}}}

//{{{ extractOrderedStatesByName
//##############################################################################
    static public Collection extractOrderedStatesByName(Model model)
    { return extractOrderedStatesByName(model.getResidues(), model.getStates()); }
    
    /**
    * Extracts all the uniquely named AtomStates for the given model, in the
    * order of Residues and Atoms given.
    * This is often used to prepare input for AtomGraph.
    */
    static public Collection extractOrderedStatesByName(Collection residues, Collection modelStates)
    {
        ModelState[]    states      = (ModelState[]) modelStates.toArray(new ModelState[modelStates.size()]);
        Set             usedNames   = new HashSet(); // to avoid duplicates
        ArrayList       atomStates  = new ArrayList();
        
        for(Iterator ri = residues.iterator(); ri.hasNext(); )
        {
            Residue res = (Residue)ri.next();
            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom atom = (Atom)ai.next();
                for(int i = 0; i < states.length; i++)
                {
                    try
                    {
                        AtomState as = states[i].get(atom);
                        // We want to make sure every atom output has a unique PDB name.
                        // We're not worried so much about duplicating coordinates (old code).
                        // Name requirement is important for dealing with alt confs,
                        // where a single atom (' ') may move in A but not B --
                        // this led to two ATOM entries with different coords but the same name.
                        String aName = as.getAtom().toString()+as.getAltConf();
                        //if(!usedNames.contains(as)) -- for comparison by XYZ coords
                        if(!usedNames.contains(aName))
                        {
                            //usedNames.add(as); -- for comparison by XYZ coords
                            usedNames.add(aName);
                            atomStates.add(as);
                        }
                    }
                    catch(AtomException ex) {} // no state
                }
            }//for each atom
        }// for each residue
        return atomStates;
    }
//}}}

//{{{ isH, isQ, isCNO
//##############################################################################
    static public boolean isH(AtomState as)
    {
        String name = as.getName().toUpperCase();
        if(name.length() < 2)
            return as.equals("H");
        char c1 = name.charAt(0), c2 = name.charAt(1);
        return ((c2 == 'H' || c2 == 'D' || c2 == 'T')
            &&  (c1 == ' ' || ('0' <= c1 && c1 <= '9')));
    }
    
    /** True iff this is an NMR pseudo-hydrogen "Q" */
    static public boolean isQ(AtomState as)
    {
        String name = as.getName().toUpperCase();
        if(name.length() < 2)
            return as.equals("H");
        char c1 = name.charAt(0), c2 = name.charAt(1);
        return ((c2 == 'Q')
            &&  (c1 == ' ' || ('0' <= c1 && c1 <= '9')));
    }
    
    /**
    * Returns true iff this atom appears to be carbon, nitrogen, or oxygen
    * based on its atom name.
    */
    static public boolean isCNO(AtomState as)
    {
        String name = as.getName().toUpperCase();
        if(name.length() < 2)
            return as.equals("C") || as.equals("N") || as.equals("O");
        char c1 = name.charAt(0), c2 = name.charAt(1);
        return ((c2 == 'C' || c2 == 'N' || c2 == 'O')
            &&  (c1 == ' ' || ('0' <= c1 && c1 <= '9')));
    }
//}}}

//{{{ altsAreCompatible
//##############################################################################
    /**
    * True iff both states have the same alternate conformation label
    * or if one of them has " " (a single space) as its ID.
    */
    static public boolean altsAreCompatible(AtomState as1, AtomState as2)
    {
        String alt1 = as1.getAltConf(), alt2 = as2.getAltConf();
        return (alt1.equals(alt2) || alt1.equals(" ") || alt2.equals(" "));
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

