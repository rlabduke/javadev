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
import driftwood.r3.Triple;
//}}}
/**
* <code>Atom</code> represents a single atom at some point in space.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 17 10:01:58 EST 2003
*/
public class Atom extends Triple implements Cloneable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Residue         parent      = null;
    
    String          id;                 // PDB-style: exactly 4 characters
    char            altconf     = ' ';
    double          occupancy   = 1;
    double          tempfactor  = 0;
    boolean         het         = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Atom(String id, double x, double y, double z)
    {
        super(x, y, z);
        
        if(id == null || id.length() != 4) throw new IllegalArgumentException("Atom ID must be 4 characters");
        this.id = id;
    }
//}}}

//{{{ getID, get/setResidue
//##################################################################################################
    public String getID()
    { return id; }
    
    /** May be null */
    public Residue getResidue()
    { return parent; }
    
    /** For use by Residue.addAtom(). */
    protected void setResidue(Residue r)
    { parent = r; }
//}}}

//{{{ get/set{AltConf, Occupancy, TempFactor}
//##################################################################################################
    public char getAltConf()
    { return altconf; }
    public void setAltConf(char altconf)
    { this.altconf = altconf; }
    
    public double getOccupancy()
    { return occupancy; }
    public void setOccupancy(double q)
    { occupancy = q; }
    
    public double getTempFactor()
    { return tempfactor; }
    public void setTempFactor(double b)
    { tempfactor = b; }
//}}}

//{{{ is/setHet
//##################################################################################################
    public boolean isHet()
    { return het; }
    public void setHet(boolean h)
    { het = h; }
//}}}

//{{{ equals, hashCode
//##################################################################################################
    /** Strict equality (identity) */
    public boolean equals(Object o)
    { return this == o; }
    
    public int hashCode()
    { return System.identityHashCode(this); }
//}}}

//{{{ clone
//##################################################################################################
    /**
    * Makes a (shallow) copy of this Atom.
    * Note that the new Atom will think it belongs to the same
    * Residue, but the reverse linkage will not be created --
    * that Residue will not be aware of the new Atom.
    */
    public Object clone() throws CloneNotSupportedException
    {
        /*Atom clone = new Atom(getID(), getX(), getY(), getZ());
        clone.setResidue(   this.getResidue());
        clone.setAltConf(   this.getAltConf());
        clone.setTempFactor(this.getTempFactor());
        clone.setOccupancy( this.getOccupancy());
        clone.setHet(       this.isHet());
        return clone;*/
        return super.clone();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

