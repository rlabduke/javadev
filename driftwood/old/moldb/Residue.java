// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.gnutil.*;
//}}}
/**
* <code>Residue</code> represents a group of atoms,
* usually one that forms some connected chemical unit.
* Residues are usually small groups, 10-20 atoms.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 17 10:01:58 EST 2003
*/
public class Residue //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Segment         parent          = null;
    Residue         next            = null;
    Residue         prev            = null;
    
    String          seqNum;                     // may include insertion code
    String          resType;                    // eg ALA, LYS; must be 3 chars
    List            atoms;
    Map             atomMap;                    // Map<Atom.getID(), Atom>; holds non-degenerate structure
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Residue(String type, String number)
    {
        if(number == null)  throw new IllegalArgumentException("Must provide a residue sequence number");
        if(type == null)    throw new IllegalArgumentException("Must provide a residue type");
        
        seqNum  = number.trim();
        resType = type.trim();
        
        atoms = new ArrayList();
        atomMap = new GnuLinkedHashMap();
    }
//}}}

//{{{ get/setSegment, get{Number, Type, ID}
//##################################################################################################
    public Segment getSegment()
    { return parent; }
    /** For use by Segment.addResidue() */
    protected void setSegment(Segment seg)
    { parent = seg; }
    
    public String getNumber()
    { return seqNum; }
    public String getType()
    { return resType; }
    
    /** Returns getType()+getNumber() */
    public String getID()
    { return getType()+getNumber(); }
//}}}

//{{{ addAtom
//##################################################################################################
    /** Adds a non-null atom to the internal list and messages Atom.setResidue() */
    public void addAtom(Atom a)
    {
        if(a == null) throw new IllegalArgumentException("Cannot add a null Atom");
        atoms.add(a);
        
        Atom old = (Atom)atomMap.get(a.getID());
        if((old == null)
        || (a.getOccupancy() > old.getOccupancy())
        || (a.getOccupancy() == old.getOccupancy() && a.getAltConf() < old.getAltConf()))
        { atomMap.put(a.getID(), a); }
        
        a.setResidue(this);
    }
//}}}

//{{{ getAtom
//##################################################################################################
    /**
    * Finds and returns the Atom with the specified ID and the "lowest" alt conf
    * (i.e., preferring blank over A over B over C, etc.),
    * or throws a NoSuchElementException if no Atom by that name is found.
    */
    public Atom getAtom(String id)
    {
        /*Atom a, ret = null;
        int i, endi = atoms.size();
        for(i = 0; i < endi; i++)
        {
            a = (Atom)atoms.get(i);
            if( a.getID().equals(id)
            && (ret == null || ret.getAltConf() > a.getAltConf()) )
            {
                ret = a;
            }
        }
        */
        Atom ret = (Atom)atomMap.get(id);
        if(ret == null) throw new NoSuchElementException("Cannot find Atom '"+id+"'");
        return ret;
    }
//}}}

//{{{ getAtoms, getAtomSet, getAtomMap
//##################################################################################################
    /** Returns an unmodifiable Collection of all Atoms in this Residue */
    public Collection getAtoms()
    { return Collections.unmodifiableCollection(atoms); }
    
    /**
    * Retuns a modifiable Set of uniquely named Atoms in this Residue.
    * For names that correspond to more than one Atom, the Atom with
    * the highest occupancy or lowest alternate conformation ID is selected.
    */
    public Set getAtomSet()
    { return new GnuLinkedHashSet(atomMap.values()); }
    
    /**
    * Retuns an unmodifiable Map relating unique Atom names (Strings)
    * to actual Atom objects in this Residue.
    * For names that correspond to more than one Atom, the Atom with
    * the highest occupancy or lowest alternate conformation ID is selected.
    */
    public Map getAtomMap()
    { return Collections.unmodifiableMap(atomMap); }
//}}}

//{{{ get/set{Next, Prev}
//##################################################################################################
    public Residue getNext()
    { return next; }
    public Residue getPrev()
    { return prev; }
    
    /** Called by Segment.addResidue() */
    protected void setNext(Residue n)
    { next = n; }
    /** Called by Segment.addResidue() */
    protected void setPrev(Residue p)
    { prev = p; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

