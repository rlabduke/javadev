// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.lang.ref.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>DataCache</code> lazily calculates all the geometry, connectivity, etc.
* for a Model object, and caches it in a memory-sensitive way.
* Use the getDataFor() method to create a DataCache for a particular model.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct  6 08:40:05 EDT 2005
*/
public class DataCache //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    // maps Models to SoftReference(DataCache)
    // entries are removed when no more references to the model exist
    // values may go to null in response to memory demand
    static WeakHashMap dataEntries = new WeakHashMap();
    
    Model           model;
    ResClassifier   resC        = null;
    Collection      atomStates  = null;
    AtomClassifier  atomC       = null;
    AtomGraph       atomGraph   = null;
    PseudoBackbone  pseudoBB    = null;
//}}}

//{{{ Constructor(s), getDataFor
//##############################################################################
    protected DataCache(Model m)
    {
        super();
        this.model = m;
    }
    
    /** Call this to obtain a DataCache for a given model, using cached data if available. */
    static public DataCache getDataFor(Model m)
    {
        SoftReference ref = (SoftReference) dataEntries.get(m);
        DataCache data = null;
        if(ref != null) data = (DataCache) ref.get();
        
        if(data == null)
        {
            data = new DataCache(m);
            dataEntries.put(m, new SoftReference(data));
        }
        
        return data;
    }
//}}}

//{{{ getResClassifier, getUniqueAtomStates, getAtomClassifier, getCovalentGraph
//##############################################################################
    public ResClassifier getResClassifier()
    {
        if(resC == null)
            resC = new ResClassifier(model.getResidues());
        return resC;
    }
    
    public Collection getUniqueAtomStates()
    {
        if(atomStates == null)
            atomStates = Util.extractOrderedStatesByName(model);
        return atomStates;
    }
    
    public AtomClassifier getAtomClassifier()
    {
        if(atomC == null)
            atomC = new AtomClassifier(getUniqueAtomStates(), getResClassifier());
        return atomC;
    }
    
    public AtomGraph getCovalentGraph()
    {
        if(atomGraph == null)
            atomGraph = new AtomGraph(getUniqueAtomStates());
        return atomGraph;
    }
//}}}

//{{{ getPseudoBackbone
//##############################################################################
    public PseudoBackbone getPseudoBackbone()
    {
        if(pseudoBB == null)
            pseudoBB = new PseudoBackbone(model, model.getStates().values(), getResClassifier());
        return pseudoBB;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

