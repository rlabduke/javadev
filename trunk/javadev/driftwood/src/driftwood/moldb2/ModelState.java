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
//import driftwood.*;
//}}}
/**
* <code>ModelState</code> is the physical-positional counterpart
* to Model: it holds all the coordinates and stateful information
* for atoms that are stored in (usually) a single Model.
*
* <p>ModelStates can be 'chained' to parent states, so that a
* modified state need only represent how it differs
* from some reference state.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 11 10:24:27 EDT 2003
*/
public class ModelState //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** The parent state. We defer to our parent if we're missing a particular mapping. */
    ModelState      parent      = null;
    
    /** The Map&lt;Atom, AtomState&gt; that defines this state. */
    Map             stateMap;
    Map             unmodMap    = null;
    String          pdbName     = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Provides a suggested capacity to possibly improve performance. */
    public ModelState(ModelState parent, int sizeHint)
    {
        this.parent = parent;
        if (parent != null)
          this.pdbName = parent.getName();
        stateMap = new HashMap(sizeHint);
    }
    
    /** Creates a state that defers to parent for missing mappings. */
    public ModelState(ModelState parent)
    {
        this.parent = parent;
        if (parent != null) 
          this.pdbName = parent.getName();
        stateMap = new HashMap();
    }
    
    /** Creates a state without a reference (parent) state to defer to. */
    public ModelState()
    { this(null); }
//}}}

//{{{ getLocal, getImpl, get, getLocalStateMap
//##################################################################################################
    /**
    * Retrieves a mapping for the specified Atom
    * iff *this* state knows about its state.
    * The parent state is NOT queried by this operation.
    * @return the AtomState, or null if no state is available.
    */
    public AtomState getLocal(Atom key)
    {
        return (AtomState)stateMap.get(key);
    }
    
    /** Does a recursive query for state; returns null on failure. */
    protected AtomState getImpl(Atom key)
    {
        AtomState s = getLocal(key);
        if(s == null && parent != null) return parent.getImpl(key);
        else                            return s;
    }
    
    /**
    * Retrieves a mapping for the specified atom.
    * This is the ordinary query, where every atom should
    * have a state defined. Therefore, an exception is thrown
    * if neither this state nor its ancestors can provide
    * a state for the query atom.
    * @throws AtomException if no state is known for the Atom,
    *   including the case where key is null.
    */
    public AtomState get(Atom key) throws AtomException
    {
        AtomState s = getImpl(key);
        if(s == null)
            throw new AtomException(key+" has no state specified");
        
        return s;
    }
    
    /**
    * Returns the unmodifiable Map&lt;Atom, AtomState&gt; that represents
    * this state, but not its ancestors.
    * This is a low-level sort of function that most clients shouldn't be using.
    */
    public Map getLocalStateMap()
    {
        if(unmodMap == null)
            unmodMap = Collections.unmodifiableMap(stateMap);
        return unmodMap;
    }
//}}}

//{{{ hasState
//##################################################################################################
    /** Returns true iff get(atom) will return a valid state. */
    public boolean hasState(Atom atom)
    {
        return (this.getImpl(atom) != null);
    }
//}}}

//{{{ add, addOverwrite
//##################################################################################################
    /**
    * Inserts a mapping for the specified Atom.
    * @throws AtomException if a mapping already exists
    *   for the target atom. Overwrites must be performed
    *   explicitly using addOverwrite().
    * @throws NullPointerException if state is null
    */
    public void add(AtomState state) throws AtomException
    {
        Atom key = state.getAtom();
        if(stateMap.containsKey(key))
            throw new AtomException(this.toString()+" already contains a state for "+key);
        
        stateMap.put(key, state);
    }

    /**
    * Inserts a mapping for the specified Atom.
    * @return the previous mapping, or null if none
    * @throws NullPointerException if state is null
    */
    public AtomState addOverwrite(AtomState state)
    { return (AtomState)stateMap.put(state.getAtom(), state); }
//}}}

//{{{ get/setParent
//##################################################################################################
    /**
    * Returns the reference state that
    * this state is defined on top of,
    * or null if none.
    */
    public ModelState getParent()
    { return parent; }
    
    /**
    * Sets the reference state for this state to fall back on.
    * @throws IllegalArgumentException if a model is made its
    *   own parent.
    */
    public void setParent(ModelState parent)
    {
        ModelState ancestor = parent;
        while(ancestor != null)
        {
            if(ancestor == this)
                throw new IllegalArgumentException("Circular inheritance detected");
            
            ancestor = ancestor.getParent();
        }
        
        this.parent = parent;
    }
//}}}

//{{{ get/setName
public String getName() {
  return pdbName;
}

public void setName(String nm) {
  pdbName = nm;
}
//}}}

//{{{ createCollapsed
//##################################################################################################
    /**
    * Creates a new ModelState that has <code>exclude</code> as its parent
    * and contains all of the dominant mappings of this object and its ancestors.
    * Thus the new object will behave just like this one from
    * the standpoint of <code>get</code>, although
    * <code>getLocal</code> and <code>getLocalStateMap</code> will now
    * return more items.
    * @param exclude    only ancestors of this state which are not
    *   <code>exclude</code> or its ancestors will be collapsed.
    *   If <code>exclude</code> is null, the new state will be
    *   completely independent and have no parent.
    */
    public ModelState createCollapsed(ModelState exclude)
    {
        // Find all ancestors which should be excluded.
        ModelState  firstExclude    = exclude;
        HashSet     excluded        = new HashSet();
        while(exclude != null)
        {
            excluded.add(exclude);
            exclude = exclude.getParent();
        }
        
        // Guess how big we need to make our hash table.
        // This should save us some rehashings.
        int maxsize = 16;
        ModelState src = this;
        while(src != null && !excluded.contains(src))
        {
            maxsize = Math.max(maxsize, src.getLocalStateMap().size());
            src = src.getParent();
        }
        maxsize = (3*maxsize) / 2;
        
        // Walk up through the ancestors, adding all states
        // that aren't already represented.
        ModelState collapsed = new ModelState(firstExclude, maxsize);
        src = this;
        while(src != null && !excluded.contains(src))
        {
            for(Iterator iter = src.getLocalStateMap().keySet().iterator(); iter.hasNext(); )
            {
                Atom a = (Atom)iter.next();
                try
                {
                    if(collapsed.getLocal(a) == null)
                        collapsed.add(src.getLocal(a));
                }
                catch(AtomException ex)
                {
                    System.err.println("Logical error!");
                    ex.printStackTrace();
                }
            }
            src = src.getParent();
        }
        
        return collapsed;
    }
    
    /**
    * Convenience for createCollapsed(null).
    * Note that the new state will have no parent.
    */
    public ModelState createCollapsed()
    { return createCollapsed(null); }
//}}}

//{{{ createForModel
//##################################################################################################
    /**
    * Collapses all mappings into a new ModelState, but only includes
    * those that map to atoms currently in the supplied model.
    */
    public ModelState createForModel(Model model)
    {
        ModelState m = new ModelState();
        for(Iterator ri = model.getResidues().iterator(); ri.hasNext(); )
        {
            Residue res = (Residue) ri.next();
            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom a = (Atom) ai.next();
                AtomState s = this.getImpl(a);
                try
                {
                    if(s != null)
                        m.add(s);
                }
                catch(AtomException ex)
                {
                    System.err.println("Logical error!");
                    ex.printStackTrace();
                }
            }
        }
        return m;
    }
//}}}

//{{{ fillInForModel
//##################################################################################################
    /** Fills in without duplicating any AtomStates; alt conf labels are left as-is. */
    public ModelState fillInForModel(Model model, Collection otherModelStates) throws AtomException
    { return fillInForModel(model, null, otherModelStates); }
    
    /**
    * For any Atom for which this ModelState does not have an AtomState,
    * the otherModelStates are queried (in order) until a matching AtomState
    * is found. A new ModelState with this as its parent is created to contain
    * the "fill-in" states, which may or may not have "correct" alt IDs.
    *
    * <p>Two modes of fill-in are possible. If altConf is left as null, then existing
    * AtomState objects will be used for fill-in and they will not be altered.
    * If altConf is supplied, AtomStates will be cloned and the alt conf of the
    * clones will be set to the supplied value.
    * While this might *seem* like a good thing, in the case of many A and B states
    * and just a few C states, you end up creating a lot of new "C" ATOM cards that
    * are just duplicates of the information in A.
    *
    * @param model              the model to search for Atoms that need AtomStates.
    * @param altConf            the alternate conformation code for (cloned) fill-in AtomStates.
    *   If this is left null (the usual choice), AtomStates will be used as-is (not cloned).
    * @param otherModelStates   a Collection of ModelStates to be queried for fill-ins.
    *
    * @throws AtomException     if no state could be found in all of the otherModelStates for an Atom in the model.
    */
    public ModelState fillInForModel(Model model, String altConf, Collection otherModelStates) throws AtomException
    {
        ModelState m = new ModelState(this);
        ModelState[] others = (ModelState[]) otherModelStates.toArray(new ModelState[otherModelStates.size()]);
        for(Iterator ri = model.getResidues().iterator(); ri.hasNext(); )
        {
            Residue res = (Residue) ri.next();
            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom a = (Atom) ai.next();
                AtomState s = this.getImpl(a);
                if(s == null)
                {
                    for(int i = 0; i < others.length && s == null; i++)
                        s = others[i].getImpl(a);
                    if(s != null)
                    {
                        if(altConf != null)
                        {
                            s = (AtomState) s.clone();
                            s.setAltConf(altConf);
                        }
                        try { m.add(s); }
                        catch(AtomException ex) { System.err.println("Logical error!"); ex.printStackTrace(); }
                    }//if s != null
                    else throw new AtomException("No state found for "+a);
                }//if s == null
            }//for all atoms
        }// for all residues
        return m;
    }
//}}}

//{{{ toString
public String toString() {
  return (pdbName+" modelstate");
}
//}}}

//{{{ debugStates
///** a quick class to spit out all contents of this ModelState in a nicely formatted string 
//    @param i   number of mappings to return in string (enter 0 for all mappings). */
//public String debugStates(int i) {
//  String s = "";
//  Iterator keys = stateMap.keySet().iterator();
//  int j = 0;
//  ArrayList lines = new ArrayList();
//  while (keys.hasNext()) {
//    Object key = keys.next();
//    Object value = stateMap.get(key);
//    s = s + key.toString() + " -> " + value.toString() + "\n";
//    j++;
//    if (j == i) {
//      s = s + "truncated.....";
//      return s; // to limit number of entries.
//    }
//  }
//  return s;
//}
    /** Simple method to spit out all contents of this ModelState 
    * in a nicely formatted string.
    * @param i number of mappings to return in string (enter 0 for all mappings)
    */
    public String debugStates(int i)
    {
        Iterator keys = stateMap.keySet().iterator();
        int j = 0;
        ArrayList lines = new ArrayList();
        while(keys.hasNext())
        {
            Object key = keys.next();
            Object value = stateMap.get(key);
            lines.add(key.toString() + " -> " + value.toString());
            j++;
            if(j == i)
            {
                lines.add("truncated...");
                break;
            }
        }
        Collections.sort(lines);
        String s = "";
        for(int k = 0; k < lines.size(); k++)
        {
            String line = (String) lines.get(k);
            s += line + "\n";
        }
        return s;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

