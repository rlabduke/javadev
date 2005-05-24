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
        
        // Use TreeMap because it keeps the identifiers in order
        // (Is this always what we want?)
        chainMap    = new TreeMap(); // all keys should be non-empty Strings
        //segmentMap  = new TreeMap(new NullNaturalComparator());
        segmentMap  = new TreeMap(); // all keys are non-null
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
            m.chainMap      = new TreeMap(m.chainMap);
            m.segmentMap    = new TreeMap(m.segmentMap);
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
    
    /** Returns a Collection&lt;Collection&lt;Residue&gt;&gt; for all chains in the model */
    public Collection getChains()
    {
        return Collections.unmodifiableCollection(chainMap.values());
    }
    
    /**
    * Returns a chain identified by its one letter code,
    * in the form of an unmodifiable Set&lt;Residue&gt;;
    * or null if there is no such chain in this model.
    */
    public Set getChain(String chainID)
    {
        Set chain = (Set)chainMap.get(chainID);
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

//{{{ getState(IDs), makeState
//##################################################################################################
    /** Returns an unmodifiable Set&lt;String&gt; of all the populated conformations in this model */
    public Set getStateIDs()
    { return Collections.unmodifiableSet(stateMap.keySet()); }
    
    /** Returns an unmodifiable Collection&lt;ModelState&gt; for all the populated conformations in this model */
    public Collection getStates()
    { return Collections.unmodifiableCollection(stateMap.values()); }
    
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
    * Returns a conformation identified by its one letter code,
    * in the form of a ModelState;
    * or <b>creates it if it didn't previously exist</b>.
    * <p>If the ID is something other than space (' '), the
    * new conformation will have the default conformation set
    * as its parent. If a default conformation does not exist
    * yet, it will also be created.
    */
    public ModelState makeState(String stateID)
    {
        ModelState state = (ModelState)stateMap.get(stateID);
        if(state == null)
        {
            state = new ModelState();
            stateMap.put(stateID, state);
            // This counts as a logical change because the
            // results of getStates() is altered.
            this.modified();
            
            if(! " ".equals(stateID))
                state.setParent(this.makeState(" "));
        }
        
        return state;
    }
//}}}

//{{{ fillInStates
//##################################################################################################
    /**
    * Once this model's states are fully populated, you can call this function to make sure that
    * every state (except possibly " ") has an AtomState for every Atom in the model.
    * @throws AtomException if no state exists for some Atom in the model.
    */
    public void fillInStates() throws AtomException
    {
        System.err.println("Filling in states for this model...");
        Collection allStates = this.stateMap.values();
        for(Iterator iter = this.stateMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry e = (Map.Entry) iter.next();
            String altConf = (String) e.getKey();
            if(" ".equals(altConf)) continue; // base conf. doesn't need all atoms defined
            
            ModelState state = (ModelState) e.getValue();
            e.setValue( state.fillInForModel(this, altConf, allStates) );
        }
        this.modified(); // changes the ModelState objects returned
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

