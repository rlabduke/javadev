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
import driftwood.r3.Triple;
//}}}
/**
* <code>AtomState</code> holds the "stateful" properties
* of an Atom: its position, charge, temperature factor,
* and occupancy.
*
* <p>AtomStates inherit equals() and hashCode() from Triple,
* which means only their coordinates are considered in these tests.
* This is useful for certain spatial/geometrical applications,
* but could cause problems if you're not aware of it!
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  9 15:02:45 EDT 2003
*/
public class AtomState extends Triple implements Cloneable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Atom        atom;
    
    // x, y, and z are inherited from Triple
    
    String      serial;
    String      altconf         = " ";
    float       charge          = 0;
    float       bfactor         = 0;
    float       occupancy       = 0;
    String      past80          = "";
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new AtomState for the given Atom.
    * @param    atom    the Atom this state applies to (not null)
    * @param    serial  a unique serial number for this state.
    *   This should be unique at least among states for this atom,
    *   and ideally for all states for all atoms in this model.
    */
    public AtomState(Atom atom, String serial)
    {
        super();
        
        if(atom == null)
            throw new NullPointerException("Cannot create a state for a null Atom");
        
        this.atom   = atom;
        this.serial = serial;
    }
//}}}

//{{{ toString
//##################################################################################################
    /**
    * Gives the qualified atom name, as given by {@link Atom#toString()},
    * plus information on alternate conformation, B-factor, occupancy, etc.
    */
    public String toString()
    {
        return atom+"["+altconf+"/"+serial+"]: "+super.toString()+" B="+bfactor+" Q="+occupancy+" e="+charge;
    }
//}}}

//{{{ get{Atom, Serial, AltConf, Charge, TempFactor, Occupancy, Past80}
//##################################################################################################
    public Atom getAtom()
    { return atom; }
    
    public String getSerial()
    { return serial; }
    
    public String getAltConf()
    { return altconf; }
    
    public double getCharge()
    { return charge; }
    
    public double getTempFactor()
    { return bfactor; }
    
    public double getOccupancy()
    { return occupancy; }
    
    /** Extra, unstructure information stored past column 80 in the original PDB file.  Not null. */
    public String getPast80()
    { return past80; }
//}}}

//{{{ getName, getElement, getResidue, isHet
//##################################################################################################
    /** Convenience function that passes through to the underlying Atom */
    public String getName()
    { return atom.getName(); }
    
    /** Convenience function that passes through to the underlying Atom */
    public String getElement()
    { return atom.getElement(); }
    
    /** Convenience function that passes through to the underlying Atom */
    public Residue getResidue()
    { return atom.getResidue(); }
    
    /** Convenience function that passes through to the underlying Atom */
    public boolean isHet()
    { return atom.isHet(); }
//}}}

//{{{ set{AltConf, Charge, TempFactor, Occupancy, Past80}
//##################################################################################################
    public void setAltConf(String alt)
    { altconf = alt; }
    
    public void setCharge(double e)
    { charge = (float)e; }
    
    public void setTempFactor(double b)
    { bfactor = (float)b; }
    
    public void setOccupancy(double q)
    { occupancy = (float)q; }
    
    /** Extra, unstructure information stored past column 80 in the original PDB file.  Not null. */
    public void setPast80(String s)
    { past80 = s; }
//}}}

//{{{ clone, cloneFor
//##################################################################################################
    /**
    * Makes a deep-enough copy of this AtomState that can
    * be changed without impacting the original.
    */
    public Object clone()
    {
        try
        { return super.clone(); }
        catch(CloneNotSupportedException ex)
        { throw new Error("Clone failed unexpectedly"); }
    }
    
    /**
    * Makes a copy of this AtomState but attaches it to a new Atom.
    */
    public AtomState cloneFor(Atom a, String newSerial)
    {
        AtomState as = (AtomState)this.clone();
        as.atom     = a;
        as.serial   = newSerial;
        return as;
    }
    
    public AtomState cloneFor(Atom a)
    { return cloneFor(a, this.getSerial()); }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

