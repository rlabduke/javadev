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
* <code>Segment</code> is a physical or logical grouping
* of Residues within a Model.
* This class is used to represent the chain IDs and/or
* segment IDs that appear in PDB files.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 17 10:01:58 EST 2003
*/
public class Segment //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Model       parent      = null;
    
    String      segID;
    Map         residues;                   // Map< Residue.getID(), Residue >
    Residue     lastRes = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new Segment with the segment/chain ID
    */
    public Segment(String id)
    {
        if(id == null) throw new IllegalArgumentException("Must provide a segment ID");
        
        if(id.trim().equals(""))    segID = " ";
        else                        segID = id.trim();
        
        residues = new GnuLinkedHashMap();
    }
//}}}

//{{{ getID, get/setModel
//##################################################################################################
    public String getID()
    { return segID; }
    
    public Model getModel()
    { return parent; }
    /** For use by Model.addSegment(). */    
    protected void setModel(Model m)
    { parent = m; }
//}}}

//{{{ addResidue
//##################################################################################################
    /** Adds the non-null Residue r to this segment and messages r.setSegment() */
    public void addResidue(Residue r)
    {
        if(r == null) throw new IllegalArgumentException("Cannot add a null Residue");
        String id = r.getID();
        if(residues.containsKey(id)) throw new IllegalArgumentException(
            "Residue ID collision: Segment already contains Residue '"+id+"'");
        
        residues.put(id, r);
        r.setSegment(this);
        r.setPrev(lastRes);
        if(lastRes != null) lastRes.setNext(r);
        lastRes = r;
    }
//}}}

//{{{ getResidue, getResidues
//##################################################################################################
    /**
    * Returns the named Residue or throws NoSuchElementException if not found.
    * Names are the IDs returned by Residue.getID(): three-letter code for residue
    * type, followed by a sequence number and an optional insertion code.
    */
    public Residue getResidue(String id)
    {
        if(residues.containsKey(id)) return (Residue)residues.get(id);
        else throw new NoSuchElementException("Cannot find Residue '"+id+"'");
    }
    
    /** Returns an unmodifiable Collection of Residues */
    public Collection getResidues()
    {
        return Collections.unmodifiableCollection(residues.values());
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

