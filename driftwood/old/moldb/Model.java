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
* <code>Model</code> represents one static structure for
* a system of one or more molecules (except for variation
* modeled as alternate conformations)
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 17 10:01:58 EST 2003
*/
public class Model //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    PDBFile     parent      = null;
    
    String      modelID;
    Map         segments;                   // Map< Segment.getID(), Segment >
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Model(String id)
    {
        modelID = id;
        
        segments = new GnuLinkedHashMap();
    }
//}}}

//{{{ getID, get/setPDBFile
//##################################################################################################
    public String getID()
    { return modelID; }
    
    public PDBFile getPDBFile()
    { return parent; }
    /** For use by PDBFile.addModel() */
    protected void setPDBFile(PDBFile f)
    { parent = f; }
//}}}

//{{{ addSegment
//##################################################################################################
    /** Adds the non-null Segment s to this model and messages s.setModel() */
    public void addSegment(Segment s)
    {
        if(s == null) throw new IllegalArgumentException("Cannot add a null Segment");
        String id = s.getID();
        if(segments.containsKey(id)) throw new IllegalArgumentException(
            "Segment ID collision: Model already contains Segment '"+id+"'");
        
        segments.put(id, s);
        s.setModel(this);
    }
//}}}

//{{{ getSegment, getSegments
//##################################################################################################
    /**
    * Returns the named Segment or throws NoSuchElementException if not found.
    * Names are the IDs returned by Segment.getID().
    */
    public Segment getSegment(String id)
    {
        if(segments.containsKey(id)) return (Segment)segments.get(id);
        else throw new NoSuchElementException("Cannot find Segment '"+id+"'");
    }
    
    /** Returns an unmodifiable Collection of Segments */
    public Collection getSegments()
    {
        return Collections.unmodifiableCollection(segments.values());
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

