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
//}}}
/**
* <code>Atom</code> holds the "stateless" aspects of an
* atom as part of a macromolecular model.
*
* <p>Atoms implement strict equality.
* Uniqueness is critical, as Atoms are used as hashtable keys
* for looking up positions in space (AtomState) and connectivities (BondSet).
* Atoms related by use of the copy constructor are NOT equal,
* and thus relevant AtomStates need to be duplicated for the copied Atoms.
* AtomState provides the cloneFor() method to simplify this process.
*
* <p>An atom may belong to only one residue; maintaining the
* correct one-to-one reciprocal links is the resposibility of
* the parent Residue.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 17 10:01:58 EST 2003
*/
public class Atom
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** May be null, maintained by parent Residue */
    Residue         parent      = null;
    
    /** Typically PDB-style (exactly 4 characters), never null */
    String          name;
    
    /** True iff this atom is considered part of a het group */
    boolean         het         = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new Atom with the given name.
    * @param    name        the name of this atom (not null)
    * @param    isHet       true if this is a het atom
    */
    public Atom(String name, boolean isHet)
    {
        if(name == null)
            throw new NullPointerException("Must supply a non-null Atom name");
        
        this.name       = name;
        this.het        = isHet;
    }

    /** Creates a new non-het atom */
    public Atom(String name)
    { this(name, false); }

    /**
    * Creates an Atom with the same name and het status as template.
    * The duplicate Atom does not belong to any Residue initially,
    * and is not equal() to the original.
    * @param    template    the atom to copy / clone
    */
    public Atom(Atom template)
    { this(template.getName(), template.isHet()); }
//}}}

//{{{ getName, getResidue, isHet, toString
//##################################################################################################
    /** Returns the name of this atom, usually a four letter code */
    public String getName()
    { return name; }

    public Residue getResidue()
    { return parent; }
    
    public boolean isHet()
    { return het; }
    
    /**
    * Returns the qualified name of this atom,
    * i.e., the name appended to the name of its parent Residue.
    */
    public String toString()
    {
        if(parent != null)  return parent.toString()+":"+name;
        else                return name;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

