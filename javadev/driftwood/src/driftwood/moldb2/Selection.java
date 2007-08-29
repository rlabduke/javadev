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
* <li>SimpleTerm &rarr; Keyword | Chain | Seg | ResRange | Within</li>
* <li>Keyword &rarr; "*" | "all" | "none" | "protein" | "mc" | "sc" | "base" | "alpha" | "beta"
*   | "nitrogen" | "carbon" | "oxygen" | "sulfur" | "phosphorus" | "hydrogen" | "metal"
*   | "polar" | "nonpolar" | "charged" | "donor" | "acceptor" | "aromatic" | "methyl"
*   | "het" | "water" | "dna" | "rna"</li>
* <li>Chain &rarr; "chain" CHAR</li>
* <li>Seg &rarr; "seg" CHAR{4}</li>
* <li>ResRange &rarr; INT ( ( "-" | "to" ) INT )</li>
* <li>Within &rarr; "within" REAL "of" ( ( "(" Selection ")" ) | ( REAL ","? REAL ","? REAL ) )</li>
* <li>REAL &rarr; "-"? UREAL</li>
* <li>INT &rarr; "-"? UINT
*   <br><i>(Otherwise, there's an ambiguity between "negative" and "range" in the grammar.)</i></li>
* </ul>
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
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Selection()
    {
        super();
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
        //TODO: check for initialization
        return selectImpl(as);
    }

    /**
    * Returns true iff the given AtomState should belong to this selection.
    * Subclasses should override this method.
    */
    abstract protected boolean selectImpl(AtomState as);
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

