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
    
    Model            model;
    ResClassifier    resC        = null;
    Collection       atomStates  = null;
    AtomClassifier   atomC       = null;
    AtomGraph        atomGraph   = null;
    VirtualBackbone  virtualBB   = null;
    Collection       modStates   = null; // collection of modelstates
//}}}

//{{{ Constructor(s), getDataFor
//##############################################################################
    protected DataCache(Model m)
    {
        super();
        this.model = m;
    }
    
    protected DataCache(Model m, Collection states)
    {
        super();
        this.model = m;
        this.modStates = states;
    }
    
    /** Call this to obtain a DataCache for a given model, using cached data if available. */
    // my assumption (vbc) is if someone calls this with a separate set of
    // modelstates, then they DON'T want to use cached data
    static public DataCache getDataFor(Model m, Collection states)
    {
        DataCache data = null;
        if (states == null)
        {
            SoftReference ref = (SoftReference) dataEntries.get(m);
            if(ref != null) data = (DataCache) ref.get();
            
            if(data == null)
            {
                data = new DataCache(m);
                dataEntries.put(m, new SoftReference(data));
            }
        }
        else
        {
            data = new DataCache(m, states);
        }
        return data;
    }
    
    static public DataCache getDataFor(Model m)
    {
        return getDataFor(m, null);
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
      if(atomStates == null) {
        if (modStates == null) {
          atomStates = Util.extractOrderedStatesByName(model);
        } else {
          atomStates = Util.extractOrderedStatesByName(model, modStates);
        }
      }
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

//{{{ getVirtualBackbone
//##############################################################################
    public VirtualBackbone getVirtualBackbone()
    {
        if(virtualBB == null)
            virtualBB = new VirtualBackbone(model, model.getStates().values(), getResClassifier());
        return virtualBB;
    }
//}}}

//{{{ getModelId
//##############################################################################
    public String getModelId()
    {
        return model.toString();
    }
  //}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

