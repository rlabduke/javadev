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
* <code>Chain</code> is a simple container that groups all
* the Residues that are in one continuous chain.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  9 16:52:29 EDT 2003
*/
public class Chain implements Comparable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    GnuLinkedHashSet    residues;
    char                chainID;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Chain(char chainID)
    {
        residues        = new GnuLinkedHashSet();
        this.chainID    = chainID;
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
    
    /** Returns true iff no Residues are part of this Chain */
    public boolean isEmpty()
    { return residues.isEmpty(); }
//}}}

//{{{ getID, toString, compareTo, compareLax
//##################################################################################################
    /** Same as toString() */
    public char getID()
    { return chainID; }
    
    public String toString()
    { return Character.toString(chainID); }
    
    /**
    * This comparison is consistent with equals but
    * arbitrary in that for two chains with the same "name",
    * the ordering will depend on their natural hash codes
    * (i.e., their locations in memory).
    */
    public int compareTo(Object o)
    {
        if(o == null) return 1;
        Chain c1 = this;
        Chain c2 = (Chain)o;
        
        int r = c1.chainID - c2.chainID;
        if(r != 0) return r;
        else return System.identityHashCode(c1) - System.identityHashCode(c2);
    }
    
    /**
    * This comparison is suitable for sorting applications,
    * where Chains need to be placed in an appropriate order,
    * but it is inconsistent with equals as it will return 0
    * for any two chains with the same "name".
    */
    public int compareLax(Object o)
    {
        if(o == null) return -1;
        Chain c1 = this;
        Chain c2 = (Chain)o;
        
        return c1.chainID - c2.chainID;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

