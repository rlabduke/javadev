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
import driftwood.data.*;
import driftwood.util.Strings;
//}}}
/**
* <code>Residue</code> represents a group of atoms,
* usually one that forms some connected chemical unit.
* Residues are usually small groups, 10-20 atoms.
* Each atom in a residue must have a unique name.
*
* <p>Residues implement strict equality and cannot be duplicated;
* each Residue is unique.
* We cannot allow shallow copies of Residues because Atoms
* need to know which Residue they belong to (e.g. for full name).
* Thus, the copy constructor makes deep copies;
* it is an Atom-cloning convenience ONLY.
*
* <p>A residue may belong to zero or more Models.
* A residue does not know, therefore, which model(s) it belongs to,
* but this allows us to cheaply clone and then tweak a given Model.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 17 10:01:58 EST 2003
*/
public class Residue implements Comparable
{
//{{{ Constants
    /** Flag value used for sequence number strings that aren't actually integer numbers. */
    public static final int NAN_SEQ = Integer.MAX_VALUE;
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** The chain and segment that this Residue belongs to (may not be zero/null) */
    String          chain           = " ";
    String          segment         = "";
    
    /** The set of atoms belonging to this Residue: Map&lt:String, Atom&gt; */
    Map             atoms;
    Collection      unmodAtoms      = null;
    
    /** The index of this residue in its chain; may be zero or negative */
    String          seqNum;
    
    /** The integer version of seqNum, or NAN_SEQ if seqNum is alphanumeric. Used for sorting. */
    int             seqInt;
    
    /** The insertion code for this residue */
    String          insCode;
    
    /** The name for this residue (recommended: 3 letters, uppercase) */
    String          resName;
    
    /** The cached, full name of this residue */
    String          qnameCache      = null;
    
    /** Number of times this residue has been modified */
    int             modCount        = 0;
    
    /**
    * A numeric counter for classifying Residues into arbitrary "sections".
    * Use it any way you like in your own code,
    * but unless you assign it a meaningful value, don't expect it to have one.
    * Default value is zero (0).
    */
    public int sectionID = 0;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new residue without any atoms in it.
    * @param chain      the chain ID. Not null. Space (" ") is a good default.
    * @param segment    the seg ID. Not null. Empty string ("") is a good default.
    * @param seqNum     the number in sequence. May have any value.
    * @param insCode    the insertion code. Not null. Space (" ") is a good default.
    * @param resName    the residue name. Not null. Empty string ("") is a good default.
    */
    public Residue(String chain, String segment, String seqNum, String insCode, String resName)
    {
        if(chain == null)
            throw new IllegalArgumentException("Must provide a non-null chain ID");
        if(segment == null)
            throw new IllegalArgumentException("Must provide a non-null segment ID");
        if(seqNum == null)
            throw new IllegalArgumentException("Must provide a non-null sequence number/ID");
        if(insCode == null)
            throw new IllegalArgumentException("Must provide a non-null insertion code");
        if(resName == null)
            throw new IllegalArgumentException("Must provide a non-null residue name");
        
        this.chain      = chain;
        this.segment    = segment;
        this.seqNum     = seqNum;
        this.insCode    = insCode;
        this.resName    = resName;
        
        try { this.seqInt = Integer.parseInt(this.seqNum.trim()); }
        catch(NumberFormatException ex) { this.seqInt = NAN_SEQ; }

        atoms = new UberMap();
    }

    /**
    * Creates a new residue just like template, with (copies of) all the same Atoms.
    * Thus, this is a deep copy. Providing a new residue name is optional.
    * Note that this is really just a convenience method for using the Atom
    * copy constructor directly to populate a new Residue.
    * This copy WILL NOT be equal to template, as each Residue object is considered unique.
    */
    public Residue(Residue template, String chain, String segment, String seqNum, String insCode, String resName)
    {
        this(chain, segment, seqNum, insCode, resName);
        
        try
        {
            for(Iterator iter = template.getAtoms().iterator(); iter.hasNext(); )
                add( new Atom((Atom)iter.next()) );
        }
        catch(AtomException ex)
        {
            System.err.println("Unable to duplicate residue?");
            ex.printStackTrace();
        }
    }

    /** Creates a new residue just like template, with (copies of) all the same Atoms. */
    public Residue(Residue template)
    {
        this(template.getChain(),
            template.getSegment(),
            template.getSequenceNumber(),
            template.getInsertionCode(),
            template.getName());
    }
//}}}

//{{{ cloneStates
//##################################################################################################
    /**
    * A utility function for use with the copy constructor.
    * Given a source residue and state, make copies of the states
    * for all the atoms in this residue that exist in the source.
    * @throws AtomException if from doesn't contain a state for one of the
    * relevant atoms in fromRes; or if to already contains a mapping for
    * one of the relevant atoms in this.
    * @return to
    */
    public ModelState cloneStates(Residue fromRes, ModelState from, ModelState to) throws AtomException
    {
        for(Iterator iter = this.getAtoms().iterator(); iter.hasNext(); )
        {
            Atom dst = (Atom)iter.next();
            Atom src = fromRes.getAtom( dst.getName() );
            if(src != null)
                to.add( from.get(src).cloneFor(dst) );
        }
        return to;
    }
//}}}

//{{{ get{Chain, Segment, Atoms, Atom}
//##################################################################################################
    /** Never null, defaults to space (" "). */
    public String getChain()
    { return chain; }
    
    /** Never null, defaults to empty (""), any number of characters. */
    public String getSegment()
    { return segment; }
    
    /**
    * Returns an unmodifiable view of the Atoms in this residue
    */
    public Collection getAtoms()
    {
        if(unmodAtoms == null)
            unmodAtoms = Collections.unmodifiableCollection(atoms.values());
        return unmodAtoms;
    }
    
    /**
    * Returns the atom of the specified name,
    * or null if no such atom is known.
    */
    public Atom getAtom(String name)
    {
        return (Atom)atoms.get(name);
    }
//}}}

//{{{ get{SequenceNumber/Integer, InsertionCode, Name, CNIT}
//##################################################################################################
    /**
    * Returns the sequence number of this residue as a non-null String.
    * Thanks to mmCIF, this doesn't have to be an integer, or even a real number;
    * it's allowed to be some arbitrary string.
    * Note that insertion codes function to give a sort order
    * within residues that share the same sequence number.
    */
    public String getSequenceNumber()
    { return seqNum; }
    
    /** Returns the sequence number as an integer, or NAN_SEQ if it's some non-integer string. */
    public int getSequenceInteger()
    { return seqInt; }
    
    /** The default insertion code if none was specified is space (" "). */
    public String getInsertionCode()
    { return insCode; }
    
    /**
    * Returns the abreviated name that identifies what kind of residue this is.
    * These are usually three letters and all caps, but that is NOT
    * enforced by this class.
    */
    public String getName()
    { return resName; }
    
    /**
    * Returns the 9-character "Chain, Number, Insertion code, Type" name of this
    * residue, formatted as "CNNNNITTT". Blank chain IDs and insertion codes
    * are left as spaces; short numbers and types ({@link #getName()}) are
    * padded with spaces and justified to the right and left, respectively.
    */
    public String getCNIT()
    {
        StringBuffer sb = new StringBuffer(9);
        sb.append(getChain().length() > 0 ? getChain().substring(0, 1) : " ");
        sb.append(Strings.justifyRight(getSequenceNumber(), 4));
        sb.append(getInsertionCode().length() > 0 ? getInsertionCode().substring(0, 1) : " ");
        sb.append(Strings.justifyLeft(getName(), 3));
        return sb.toString();
    }
//}}}

//{{{ get{Next, Prev}
//##################################################################################################
    /**
    * Returns the residue after this one in the chain, or
    * null if there is no such residue.
    * This function relies on the ordering maintained by
    * Model and will not work properly if the residues are
    * out of order.
    * The chain ID is checked; but segment ID and distance
    * are NOT taken into consideration.
    */
    public Residue getNext(Model parent)
    {
        if(parent == null)
            return null;
        
        try
        {
            Residue next = (Residue)parent.residues.itemAfter(this);
            
            if(!next.getChain().equals(this.getChain()))
                return null;
            
            return next;
        }
        catch(NoSuchElementException ex)
        { return null; }
    }
    
    /**
    * Returns the residue before this one in the chain, or
    * null if there is no such residue.
    * This function relies on the ordering maintained by
    * Model and will not work properly if the residues are
    * out of order.
    * The chain ID is checked; but segment ID and distance
    * are NOT taken into consideration.
    */
    public Residue getPrev(Model parent)
    {
        if(parent == null)
            return null;
        
        try
        {
            Residue prev = (Residue)parent.residues.itemBefore(this);
            
            if(!prev.getChain().equals(this.getChain()))
                return null;
            
            return prev;
        }
        catch(NoSuchElementException ex)
        { return null; }
    }
//}}}

//{{{ add, remove
//##################################################################################################
    /**
    * Adds the given Atom to this Residue.
    * If it previously belonged to another Residue,
    * it will be removed from that Residue first.
    * @throws AtomException if an atom with the same name
    *   is already part of this residue.
    */
    public void add(Atom a) throws AtomException
    {
        String name = a.getName();
        if(atoms.containsKey(name))
            throw new AtomException("An atom named "+name+" is already part of "+this);
        
        if(a.parent != null)
            a.parent.remove(a);
        
        atoms.put(name, a);
        a.parent = this;
        
        this.modified();
    }
    
    /**
    * Removes the given Atom from this Residue.
    * @throws AtomException if the Atom
    *   wasn't part of this Residue already.
    */
    public void remove(Atom a) throws AtomException
    {
        String name = a.getName();
        Atom old = (Atom)atoms.get(name);
        if(!a.equals(old))
            throw new AtomException(a+" is not part of "+this);
            
        atoms.remove(name);
        a.parent = null;
        
        this.modified();
    }
//}}}

//{{{ modified, getModCount
//##################################################################################################
    /** Call this after changing anything about this Residue */
    protected void modified()
    { modCount++; }
    
    /**
    * Gets a 'count' of the modifications to this residue.
    * The integer returned is guaranteed to change from
    * one call to the next if and only if the internal
    * state of this residue has been changed.
    */
    public int getModCount()
    { return modCount; }
//}}}

//{{{ compareTo, toString
//##################################################################################################
    /**
    * This comparison is consistent with equals but
    * arbitrary in that for two residues with the same "name",
    * the ordering will depend on their natural hash codes
    * (i.e., their locations in memory).
    */
    public int compareTo(Object o)
    {
        if(o == null) return 1; // null sorts to front
        Residue r1 = this;
        Residue r2 = (Residue)o;
        
        int comp = r1.chain.compareTo(r2.chain);
        if(comp != 0) return comp;
        comp = r1.segment.compareTo(r2.segment);
        if(comp != 0) return comp;
        
        comp = r1.seqInt - r2.seqInt;
        if(comp != 0) return comp;
        // seqNums could still differ by whitespace...
        comp = r1.seqNum.compareTo(r2.seqNum);
        if(comp != 0) return comp;
        comp = r1.insCode.compareTo(r2.insCode);
        if(comp != 0) return comp;
        comp = r1.resName.compareTo(r2.resName);
        if(comp != 0) return comp;
        
        return System.identityHashCode(r1) - System.identityHashCode(r2);
    }
    
    /**
    * Returns the full name of the Residue, including chain and segment (if any),
    * number, insertion code, and residue three-letter code
    */
    public String toString()
    {
        if(qnameCache == null)
        {
            StringBuffer s = new StringBuffer();
            String chtrim = chain.trim();
            String segtrim = segment.trim();
            if(chtrim.length() > 0)     s.append(chtrim).append(' ');
            if(segtrim.length() > 0)    s.append(segtrim).append(' ');
            
            s.append(seqNum);
            String instrim = insCode.trim();
            if(instrim.length() > 0)  s.append(instrim);
            s.append(' ');
            s.append(resName);
            qnameCache = s.toString();
        }
        
        return qnameCache;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

