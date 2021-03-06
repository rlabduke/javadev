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
//import driftwood.util.NullNaturalComparator;
//}}}
/**
* <code>Model</code> is the head of a logical (naming) description
* of a macromolecular chemical system. For a physical (positional)
* description, see ModelState. Models are Sets of Residues.
*
* <p>It is recommended that every residues in the model have a unique
* CNIT code, or {@link #getResidue(String)} won't work properly.
*
* <p>An interesting note about Models is that they can contain multiple
* ModelStates.  Alt confs are typically stored in separate ModelStates!
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jun 10 10:36:48 EDT 2003
*/
public class Model implements Cloneable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** The name of this model (never null) */
    String              name;

    /** The set of Residues belonging to this Model */
    UberSet             residues;
    /** Residues by CNIT code for fast lookup: Map&lt;String, Residue&gt; */
    Map                 resCNIT;
    
    // Chains and Segments shouldn't be implemented as real classes
    // because of the difficulting synchronizing residue membership
    // to Model, Chain, *and* Segment during add() and remove() ops. 
    
    /** The chains: a lookup table based on names: Map&lt;String, Set&lt;Residue&gt;&gt;*/
    Map                 chainMap;
    
    /** The segments: a lookup table based on names: Map&lt;String, Set&lt;Residue&gt;&gt;*/
    Map                 segmentMap;
    
    /** The conformations: a lookup table based on names: Map&lt;String, ModelState&gt;*/
    Map                 stateMap;
    
    /** Number of times this model has been modified */
    int                 modCount        = 0;
    
    /** Contains Disulfide objects defined at the file level.  Optional! */
    Disulfides          disulfides;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @param name an identifier for this model. May not be null.
    */
    public Model(String name)
    {
        if(name == null)
            throw new NullPointerException("Must provide a non-null model name");
        this.name = name;
        
        // Use Uber because Residues need to
        // retrieve residue before and after themselves!
        residues    = new UberSet();
        resCNIT     = new HashMap();
        
        // Use TreeMap because it keeps the identifiers in alphabetical order
        // Use UberMap because is preserves the order from the input file
        // (This matters esp. for chains when _ is the LAST chain in the file)
        //chainMap    = new TreeMap(); // all keys should be non-empty Strings
        chainMap    = new UberMap(); // all keys should be non-empty Strings
        //segmentMap  = new TreeMap(); // all keys are non-null
        segmentMap  = new UberMap(); // all keys are non-null
        stateMap    = new TreeMap(); // all keys should be non-empty Strings
    }
//}}}

//{{{ clone
//##################################################################################################
    /**
    * Makes a deep-enough copy of this Model that can
    * be changed without impacting the original.
    * Residues themselves and all associated ModelStates
    * are NOT copied, so they shouldn't be changed.
    * However, they can easily be added/removed from the cloned Model.
    */
    public Object clone()
    {
        try
        {
            Model m         = (Model) super.clone();
            m.residues      = new UberSet(m.residues);
            m.resCNIT       = new HashMap(m.resCNIT);
            m.chainMap      = new UberMap(m.chainMap);
            m.segmentMap    = new UberMap(m.segmentMap);
            m.stateMap      = new TreeMap(m.stateMap);

            // Deep copy of chain and segment maps
            for(Iterator iter = m.chainMap.entrySet().iterator(); iter.hasNext(); )
            {
                Map.Entry e = (Map.Entry) iter.next();
                e.setValue(new UberSet((Set)e.getValue()));
            }
            for(Iterator iter = m.segmentMap.entrySet().iterator(); iter.hasNext(); )
            {
                Map.Entry e = (Map.Entry) iter.next();
                e.setValue(new UberSet((Set)e.getValue()));
            }

            return m;
        }
        catch(CloneNotSupportedException ex)
        { throw new Error("Clone failed unexpectedly"); }
    }
//}}}

//{{{ getName, toString
//##################################################################################################
    /** Same as toString() */
    public String getName()
    { return name; }

    /** Returns the identifier the model was created with (never null). */
    public String toString()
    { return name; }
//}}}

//{{{ modified, getModCount
//##################################################################################################
    /** Call this after changing anything about this Model */
    protected void modified()
    {
        modCount++;
    }
    
    /**
    * Gets a 'count' of the modifications to this model.
    * The integer returned is guaranteed to change from
    * one call to the next if and only if the logical
    * structure of this model has been changed.
    *
    * This call must iterate through all member residues,
    * thus incurring a slight performance cost.
    */
    public int getModCount()
    {
        int c = modCount;
        for(Iterator iter = this.getResidues().iterator(); iter.hasNext(); )
            c += ((Residue)iter.next()).getModCount();
        return c;
    }
//}}}

//{{{ add, remove, replace
//##################################################################################################
    /**
    * Adds the given Residue to this Model.
    * If it previously belonged to another Model,
    * it will be removed from that Model first.
    * The Residue will be added to the end of the residue list;
    * call restoreOrder() if iteration order is important.
    * @throws ResidueException if a residue with the same fully-
    *   qualified name is already part of this model.
    */
    public void add(Residue r) throws ResidueException
    {
        if(residues.contains(r))
            throw new ResidueException("A residue named "+r+" is already part of model "+this);
        
        residues.add(r);
        resCNIT.put(r.getCNIT(), r);
        addToChain(r, r.getChain());
        addToSegment(r, r.getSegment());
        
        this.modified();
    }
    
    /**
    * Removes the given Residue from this Model.
    * @throws ResidueException if the Residue
    *   wasn't part of this Model already.
    */
    public void remove(Residue r) throws ResidueException
    {
        if(!residues.contains(r))
            throw new ResidueException(r+" is not part of model "+this);
        
        residues.remove(r);
        resCNIT.remove(r.getCNIT());
        removeFromChain(r, r.getChain());
        removeFromSegment(r, r.getSegment());
        
        this.modified();
    }

    /**
    * Replaces one residue with another, maintaining the order of residues.
    * At the moment, however, the new residue goes at the end of the chain
    * and segment lists, so they will probably no longer be in order.
    * @throws ResidueException if the Residue
    *   wasn't part of this Model already.
    */
    public void replace(Residue oldRes, Residue newRes) throws ResidueException
    {
        if(residues.contains(newRes))
            throw new ResidueException("A residue named "+newRes+" is already part of model "+this);
        if(!residues.contains(oldRes))
            throw new ResidueException(oldRes+" is not part of model "+this);
        
        resCNIT.remove(oldRes.getCNIT());
        removeFromChain(oldRes, oldRes.getChain());
        removeFromSegment(oldRes, oldRes.getSegment());

        resCNIT.put(newRes.getCNIT(), newRes);
        addToChain(newRes, newRes.getChain());
        addToSegment(newRes, newRes.getSegment());

        residues.addBefore(oldRes, newRes);
        residues.remove(oldRes);
        this.modified();
    }
//}}}

//{{{ getResidues, getResidue, contains
//##################################################################################################
    /**
    * Returns an unmodifiable view of the Residues in this model.
    */
    public Collection getResidues()
    {
        return Collections.unmodifiableCollection(residues);
    }
    
    /**
    * Returns the residue with the specified CNIT code,
    * or null if no such residue is known.
    * This will only work dependably if every residue in the model
    * has a unique CNIT code.
    * @see Residue#getCNIT()
    */
    public Residue getResidue(String cnit)
    {
        return (Residue)resCNIT.get(cnit);
    }
    
    /**
    * Returns true if the residue is part of this model.
    */
    public boolean contains(Residue r)
    {
        return residues.contains(r);
    }
//}}}

//{{{ getChain(IDs), addTo/removeFromChain
//##################################################################################################
    /** Returns an unmodifiable Set&lt;String&gt; of all the populated chains in this model */
    public Set getChainIDs()
    {
        return Collections.unmodifiableSet(chainMap.keySet());
    }
    
    /**
    * Returns a chain identified by its one letter code,
    * in the form of an unmodifiable Set&lt;Residue&gt;;
    * or null if there is no such chain in this model.
    */
    public Set getChain(String chainID)
    {
        Set chain = (Set)chainMap.get(chainID);
        if (chain==null) return null;
        return Collections.unmodifiableSet(chain);
    }
    
    /** Registers the given Residue with the named chain; for use by this and Residue */
    void addToChain(Residue r, String chainID)
    {
        Set chain = (Set)chainMap.get(chainID);
        if(chain == null)
        {
            chain = new UberSet();
            chainMap.put(chainID, chain);
        }
        
        chain.add(r);
        this.modified();
    }
    
    /**
    * Removes the given Residue from the chain if it was a part of it.
    * @throws ResidueException if the residue was not part of the chain.
    */
    void removeFromChain(Residue r, String chainID) throws ResidueException
    {
        Set chain = (Set)chainMap.get(chainID);
        if(chain == null)
            throw new ResidueException(r+" is not a part of non-existant chain "+chainID);
        
        if(!chain.remove(r))
            throw new ResidueException(r+" is not a part of chain "+chainID);
        
        // Eliminate empty chains
        if(chain.isEmpty()) chainMap.remove(chainID);
        
        this.modified();
    }
//}}}

//{{{ getSegment(IDs), addTo/removeFromSegment
//##################################################################################################
    /** Returns an unmodifiable Set&lt;String&gt; of all the populated segments in this model */
    public Set getSegmentIDs()
    {
        return Collections.unmodifiableSet(segmentMap.keySet());
    }
    
    /**
    * Returns a segment identified by its one letter code,
    * in the form of an unmodifiable Set&lt;Residue&gt;;
    * or null if there is no such segment in this model.
    */
    public Set getSegment(String segmentID)
    {
        Set segment = (Set)segmentMap.get(segmentID);
        return Collections.unmodifiableSet(segment);
    }
    
    /** Registers the given Residue with the named segment; for use by this and Residue */
    void addToSegment(Residue r, String segmentID)
    {
        Set segment = (Set)segmentMap.get(segmentID);
        if(segment == null)
        {
            segment = new UberSet();
            segmentMap.put(segmentID, segment);
        }
        
        segment.add(r);
        this.modified();
    }
    
    /**
    * Removes the given Residue from the segment if it was a part of it.
    * @throws ResidueException if the residue was not part of the segment.
    */
    void removeFromSegment(Residue r, String segmentID) throws ResidueException
    {
        Set segment = (Set)segmentMap.get(segmentID);
        if(segment == null)
            throw new ResidueException(r+" is not a part of non-existant segment "+segmentID);
        
        if(!segment.remove(r))
            throw new ResidueException(r+" is not a part of segment "+segmentID);
        
        // Eliminate empty segments
        if(segment.isEmpty()) segmentMap.remove(segmentID);
        
        this.modified();
    }
//}}}

//{{{ getState(s), setStates
//##################################################################################################
    /** Returns an unmodifiable Map&lt;String, ModelState&gt; for all the populated conformations in this model */
    public Map getStates()
    { return Collections.unmodifiableMap(stateMap); }
    
    /**
    * Returns a conformation identified by its one letter code,
    * in the form of a ModelState;
    * or null if there is no such conformation in this model.
    */
    public ModelState getState(String stateID)
    {
        ModelState state = (ModelState)stateMap.get(stateID);
        return state;
    }
    
    /**
    * Returns the default conformation.
    * The default conformation is 'A' if alternate conformations are present,
    * or space (' ') if only one conformation is present.
    */
    public ModelState getState()
    {
        ModelState          state = getState("A");
        if(state == null)   state = getState(" ");
        return state;
    }
    
    /**
    * Uses the given Map&lt;String, ModelState&gt; to associate
    * a new set of states with a new set of labels in this Model.
    */
    public void setStates(Map newStates)
    {
        this.stateMap = new TreeMap(newStates);
        // This counts as a logical change because the
        // results of getStates() is altered.
        this.modified();
    }
//}}}

//{{{ get/setDisulfides
//##################################################################################################
    public Disulfides getDisulfides()
    { 
      return this.disulfides; 
    }

    public void setDisulfides(Disulfides disulfides)
    { 
      //System.out.println("Disulfides set to: "+disulfides);
      this.disulfides = disulfides; 
    }
//}}}

//{{{ fillInStates
//##################################################################################################
    /**
    * Once this model's states are fully populated, you can call this function to make sure that
    * every state (except possibly " ") has an AtomState for every Atom in the model.
    * @param cloneAtomStates    if true, AtomStates for fill-in will be cloned and
    *   given the "correct" alt conf label. If false, existing AtomStates will be
    *   used as-is. In most cases, the best choice is false -- this avoids e.g.
    *   creating new ATOM cards when the model is written to PDB.
    * @throws AtomException if no state exists for some Atom in the model.
    */
    public void fillInStates(boolean cloneAtomStates) throws AtomException
    {
        Collection allStates = this.stateMap.values();
        for(Iterator iter = this.stateMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry e = (Map.Entry) iter.next();
            String altConf = (String) e.getKey();
            if(" ".equals(altConf)) continue; // base conf. doesn't need all atoms defined
            
            ModelState state = (ModelState) e.getValue();
            if(cloneAtomStates) e.setValue( state.fillInForModel(this, altConf, allStates) );
            else                e.setValue( state.fillInForModel(this, allStates) );
        }
        this.modified(); // changes the ModelState objects returned
    }
//}}}

//{{{ extractOrderedStatesByName
//##############################################################################
    static public Collection extractOrderedStatesByName(Model model)
    { return extractOrderedStatesByName(model.getResidues(), model.getStates().values()); }
    
    /**
    * Extracts all the uniquely named AtomStates for the given model, in the
    * order of Residues and Atoms given.
    * This is often used to prepare input for AtomGraph.
    */
    static public Collection extractOrderedStatesByName(Collection residues, Collection modelStates)
    {
        ModelState[]    states      = (ModelState[]) modelStates.toArray(new ModelState[modelStates.size()]);
        Set             usedNames   = new HashSet(); // to avoid duplicates
        ArrayList       atomStates  = new ArrayList();
        
        for(Iterator ri = residues.iterator(); ri.hasNext(); )
        {
            Residue res = (Residue)ri.next();
            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom atom = (Atom)ai.next();
                for(int i = 0; i < states.length; i++)
                {
                    try
                    {
                        AtomState as = states[i].get(atom);
                        // We want to make sure every atom output has a unique PDB name.
                        // We're not worried so much about duplicating coordinates (old code).
                        // Name requirement is important for dealing with alt confs,
                        // where a single atom (' ') may move in A but not B --
                        // this led to two ATOM entries with different coords but the same name.
                        String aName = as.getAtom().toString()+as.getAltConf();
                        //if(!usedNames.contains(as)) -- for comparison by XYZ coords
                        if(!usedNames.contains(aName))
                        {
                            //usedNames.add(as); -- for comparison by XYZ coords
                            usedNames.add(aName);
                            atomStates.add(as);
                        }
                    }
                    catch(AtomException ex) {} // no state
                }
            }//for each atom
        }// for each residue
        return atomStates;
    }
//}}}

//{{{ setName
  /** Sets the name of this model; must be non-null **/
  public void setName(String name) {
    if(name == null)
      throw new NullPointerException("Must provide a non-null model name");
    this.name = name;
  }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

