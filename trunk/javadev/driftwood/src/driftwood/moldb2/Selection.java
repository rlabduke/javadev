// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.UberSet;
import driftwood.moldb2.selection.SelectionParser;
//}}}
/**
* <code>Selection</code> allows selecting specific residues and atoms using
* the selection language built into Probe (and NOEdisplay, etc).
*
* This is a partial specification of the grammar that I intend to implement here.
* Some options from Probe have not been included yet,
* and some of what's below isn't implemented yet.
*
* <ul>
* <li>Selection &rarr; LooseOrTerm ( ( "|" | "or" ) LooseOrTerm )*</li>
* <li>LooseOrTerm &rarr; LooseAndTerm ( ( "&" | "and" ) LooseAndTerm )*</li>
* <li>LooseAndTerm &rarr; ( TightAndTerm )+</li>
* <li>TightAndTerm &rarr; TightOrTerm ( "," TightOrTerm )*</li>
* <li>TightOrTerm &rarr; ( "!" | "not" )? NotTerm</li>
* <li>NotTerm &rarr; SimpleTerm | ( "(" Selection ")" )
*   <br><i>(That makes the grammar recursive!)</i></li>
* <li>SimpleTerm &rarr; Keyword | Chain | Seg | ResName | Atom | ResRange | Within | FromRes</li>
* <li>Keyword &rarr; "*" | "all" | "none" | "protein" | "mc" | "sc" | "base" | "alpha" | "beta"
*   | "nitrogen" | "carbon" | "oxygen" | "sulfur" | "phosphorus" | "hydrogen" | "metal"
*   | "polar" | "nonpolar" | "charged" | "donor" | "acceptor" | "aromatic" | "methyl"
*   | "het" | "water" | "dna" | "rna"</li>
* <li>Chain &rarr; "chain" CHAR</li>
* <li>Seg &rarr; "seg" CHAR{4}</li>
* <li>ResName &rarr; "res" CHAR{3}</li>
* <li>Atom &rarr; "atom" CHAR{4}</li>
* <li>ResRange &rarr; INT ( ( "-" | "to" ) INT )</li>
* <li>Within &rarr; "within" REAL "of" ( ( "(" Selection ")" ) | ( REAL ","? REAL ","? REAL ) )</li>
* <li>FromRes &rarr; "fromres" "(" Selection ")"</li>
* </ul>
*
* Note that there's an ambiguity around "-": it can denote negative numbers
* or residue ranges.  The implementation must take special care to get this right!
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:31:38 PDT 2007
*/
abstract public class Selection //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    private boolean initialized = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    protected Selection()
    {
        super();
    }
//}}}

//{{{ fromString
//##############################################################################
    /**
    * Use this method to obtain a Selection object from a specification string.
    */
    static public Selection fromString(String expr) throws ParseException
    {
        try
        {
            SelectionParser p = new SelectionParser();
            return p.parse(expr);
        }
        // this shouldn't ever happen b/c we're just working with strings
        catch(IOException ex) { ex.printStackTrace(); return null; }
    }
//}}}

//{{{ init
//##############################################################################
    /**
    * This must be called before using select().
    * It establishes the "universe" of atoms that will be
    * considered for "within ..." statements.
    * If you *know* you don't have any "within" statements,
    * you may pass an empty collection.
    */
    public void init(Collection atomStates)
    {
        this.initialized = true;
    }
//}}}

//{{{ select, selectImpl
//##############################################################################
    /**
    * Returns true iff the given AtomState should belong to this selection.
    * Public interface, not intended for override.
    */
    final public boolean select(AtomState as)
    {
        if(!this.initialized)
            throw new IllegalStateException("Selection must be initialized with init() before use!");
        return selectImpl(as);
    }

    /**
    * Returns true iff the given AtomState should belong to this selection.
    * Subclasses should override this method.
    */
    abstract protected boolean selectImpl(AtomState as);
//}}}

//{{{ selectAtomStates, selectResidues
//##############################################################################
    /**
    * Returns the list of AtomStates for which select() is true.
    */
    public Collection selectAtomStates(Collection atomStates)
    {
        ArrayList selected = new ArrayList();
        for(Iterator iter = atomStates.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if( this.select(as) ) selected.add(as);
        }
        return selected;
    }
    
    /**
    * Returns the Set of Residues for which at least one AtomState was selected.
    */
    public Set selectResidues(Collection atomStates)
    {
        // Must try them all unless already selected,
        // because only one has to match for the residue to be included.
        Set selected = new UberSet(); // nice to have them stay in order
        for(Iterator iter = atomStates.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            Residue r = as.getResidue();
            if(selected.contains(r)) continue;
            else if( this.select(as) ) selected.add(r);
        }
        return selected;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

