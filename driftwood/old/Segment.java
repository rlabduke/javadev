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
import driftwood.gnutil.*;
//}}}
/**
* <code>Segment</code> is a simple container that groups all
* the Residues that share the same segment identifier.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  9 16:52:29 EDT 2003
*/
public class Segment implements Comparable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    GnuLinkedHashSet    residues;
    String              segID;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Segment(String segID)
    {
        residues        = new GnuLinkedHashSet();
        this.segID      = segID;
    }
//}}}

//{{{ add, remove, isEmpty
//##################################################################################################
    /** For use by Model/Residue */
    protected void add(Residue r)
    { residues.add(r); }
    
    /** For use by Model/Residue */
    protected void remove(Residue r)
    { residues.remove(r); }
    
    /** Returns true iff no Residues are part of this Segment */
    public boolean isEmpty()
    { return residues.isEmpty(); }
//}}}

//{{{ getID, toString, compareTo, compareLax
//##################################################################################################
    /** Same as toString() */
    public String getID()
    { return segID; }
    
    public String toString()
    { return segID; }
    
    /**
    * This comparison is consistent with equals but
    * arbitrary in that for two segments with the same "name",
    * the ordering will depend on their natural hash codes
    * (i.e., their locations in memory).
    */
    public int compareTo(Object o)
    {
        if(o == null) return 1;
        Segment s1 = this;
        Segment s2 = (Segment)o;
        
        int r = s1.segID.compareTo(s2.segID);
        if(r != 0) return r;
        else return System.identityHashCode(s1) - System.identityHashCode(s2);
    }
    
    /**
    * This comparison is suitable for sorting applications,
    * where Segments need to be placed in an appropriate order,
    * but it is inconsistent with equals as it will return 0
    * for any two segments with the same "name".
    */
    public int compareLax(Object o)
    {
        if(o == null) return -1;
        Segment s1 = this;
        Segment s2 = (Segment)o;
        
        return s1.segID.compareTo(s2.segID);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

