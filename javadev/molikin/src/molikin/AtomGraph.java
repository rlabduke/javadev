// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>AtomGraph</code> takes a universe of AtomStates and determines their
* bonded connectivity based purely on inter-atom distances.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May  6 14:03:36 EDT 2005
*/
public class AtomGraph //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /**
    * All the AtomStates in this graph, in ascending order by index number.
    */
    AtomState[]         atomStates;
    
    /**
    * Map&lt;AtomState, Integer&gt; for finding specific states in atomStates[].
    * AtomStates are compared by identity, not equality, to avoid XYZ overlaps.
    */
    Map                 stateIndices;
    
    /**
    * Built at create time, for looking up bondedness relationships.
    */
    SpatialBin          stateBin;
    
    /**
    * A bunch of Collection&lt;AtomState&gt; for recording which atoms are
    * one bond away from a particular atom. Starts empty and is filled lazily.
    * Indices match with those of atomStates and stateIndices.
    */
    Collection[]        covNeighbors;
    
    /**
    * All of the Bonds for all of the states.
    * Fully populating this is expensive, so it's created lazily when requested.
    * Data will be plundered from covNeighbors when it's available.
    * Unmodifiable once created.
    */
    Collection          allBonds = null;
    
    /**
    * Contains all AtomStates known not to have any bonds to them.
    * Stays null until allBonds is populated.
    * Unmodifiable once created.
    */
    Collection          unbondedAtoms = null;
    
    /**
    * Controls whether chain ID should be considered when figuring out bonds.
    * If set to false, then cross-chain bonds are allowed iff either
    * (1) at least one atom is not C, N, O, P, or
    * (2) at least one is marked as a HETATM.
    * Hydrogens can only bond to something in their own chain in this case.
    * Cross-chain bonds are a big problem for EM structures (e.g. 1DYL).
    * This is implemented here rather than as an external filter because I want
    * this class to return all (true) bonds, but not spurious connections.
    * This is somewhat inconsistent, as I don't try at this level to filter out
    * spurious bonds caused by terrible (intra-chain) geometry in old files.
    */
    boolean             ignoreChains = false;
    
    // Used by bond search functions:
    private ArrayList _hits1 = new ArrayList(), _hits2 = new ArrayList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a connectivity graph for all the AtomStates in states.
    * The order of iteration over states IS important, as that order is used
    * by many of the later drawing algorithms.
    */
    public AtomGraph(Collection states)
    {
        super();
        //long time = System.currentTimeMillis();
        
        // AtomStates are compared by identity, not equality, to avoid XYZ overlaps.
        this.atomStates = (AtomState[]) states.toArray( new AtomState[states.size()] );
        this.stateIndices = new IdentityHashMap( atomStates.length );
        for(int i = 0; i < atomStates.length; i++) stateIndices.put(atomStates[i], new Integer(i));
        
        // Do a spatial hashing so neighbors can be found quickly.
        // TODO: try different values here to tune performance.
        this.stateBin = new SpatialBin(3.0);
        this.stateBin.addAll(Arrays.asList(this.atomStates));
        
        // Start an empty map for AtomState -> Collection<AtomState> mapping.
        this.covNeighbors = new Collection[this.atomStates.length];
        for(int i = 0; i < covNeighbors.length; i++) covNeighbors[i] = null;
        
        //time = System.currentTimeMillis() - time;
        //System.err.println("Spatial binning:        "+time+" ms");
    }
//}}}

//{{{ getBonds
//##############################################################################
    /**
    * Returns all the bonds between all the atoms in this graph.
    * For N atoms, this is an O(N) or O(N ln N) computation,
    * but it may still be reasonably expensive.
    * Results are cached for future calls to this function and to getNeighbors(),
    * and any cached results from previous getNeighbors() calls are used here.
    */
    public Collection getBonds()
    {
        if(this.allBonds == null)
        {
            //long time = System.currentTimeMillis();

            // A Set is used so that duplicate bonds will be eliminated.
            // CheapSet + sort() ~ TreeSet in speed but uses less memory
            this.allBonds = new CheapSet();
            this.unbondedAtoms = new ArrayList();
            for(int i = 0; i < atomStates.length; i++)
            {
                AtomState query = atomStates[i];
                Collection neighbors = this.getNeighbors(query); // forces neighbors to be found
                if(neighbors.size() == 0)
                {
                    this.unbondedAtoms.add(query);
                }
                else
                {
                    //this.bondedAtoms.add(query); -- no need for this yet
                    int iQuery = ((Integer) stateIndices.get(query)).intValue();
                    for(Iterator iter = neighbors.iterator(); iter.hasNext(); )
                    {
                        AtomState hit = (AtomState) iter.next();
                        int iHit = ((Integer) stateIndices.get(hit)).intValue();
                        this.allBonds.add(new Bond(query, iQuery, hit, iHit));
                    }
                }
            }
            // Iteration over the FinalArrayList is twice as fast as over the wrapped TreeSet!
            //this.allBonds = Collections.unmodifiableCollection(this.allBonds);
            Object[] bonds = this.allBonds.toArray();
            this.allBonds = null;
            Arrays.sort(bonds);
            // There's no point in optimizing their order now, because when
            // broken down into mc, sc, etc the bonds will be disconnected again.
            this.allBonds = new FinalArrayList(bonds);
            this.unbondedAtoms = Collections.unmodifiableCollection(this.unbondedAtoms);

            //time = System.currentTimeMillis() - time;
            //System.err.println("Building bond network:  "+time+" ms");
        }
        return this.allBonds;
    }
//}}}

//{{{ getUnbonded
//##############################################################################
    /**
    * Returns a Collection of AtomStates that have no bonds.
    * These will usually be metals, ions, waters (oxygen only), etc.
    */
    public Collection getUnbonded()
    {
        if(this.unbondedAtoms == null) getBonds();
        return this.unbondedAtoms;
    }
//}}}

//{{{ getNeighbors
//##############################################################################
    /**
    * Gets all the AtomStates that the given AtomState is bonded to.
    * AtomStates are compared by identity here, not equality, so use
    * one of the states passed to the constructor.
    */
    public Collection getNeighbors(AtomState query)
    {
        //////////////////////////////////////////////////////////////////
        // Impl. note: this function is generally NOT REENTRANT, but is //
        // designed so connectHeavyAtoms() can call connectHydrogens(). //
        //////////////////////////////////////////////////////////////////
        
        // This will throw NPEx if query isn't a state we manage:
        int iQuery = ((Integer) stateIndices.get(query)).intValue();
        Collection neighbors = this.covNeighbors[iQuery];
        if(neighbors == null)
        {
            String elem = query.getElement();
            if(elem.equals("H"))    neighbors = connectHydrogens(query);
            else                    neighbors = connectHeavyAtoms(query);
            this.covNeighbors[iQuery] = neighbors;
        }
        return neighbors;
    }
//}}}

//{{{ connectHydrogens
//##############################################################################
    /** Finds the closest neighbor (JUST ONE) of a given H */
    private Collection connectHydrogens(AtomState query)
    {
        // See http://chemviz.ncsa.uiuc.edu/content/doc-resources-bond.html
        // and the Prekin source in PKINCSUB.c ::  connecthydrogen()
        final double d2max = 2.0 * 2.0;
        _hits2.clear();
        this.stateBin.findSphere(query, 2.0, _hits2);
        
        AtomState bestHit = null;
        double d2best = Double.POSITIVE_INFINITY;
        
        Collection neighbors = new ArrayList(1);
        for(Iterator iter = _hits2.iterator(); iter.hasNext(); )
        {
            AtomState hit = (AtomState) iter.next();
            if(hit == query) continue;
            String elem = hit.getElement();
            if(elem.equals("H") || elem.equals("Q")) continue;
            
            boolean chainsCompat = ignoreChains
                || query.getResidue().getChain().equals(hit.getResidue().getChain());
            double d2 = query.sqDistance(hit);
            if(d2 <= d2max && d2 < d2best && chainsCompat && Util.altsAreCompatible(query, hit))
            {
                bestHit = hit;
                d2best  = d2;
            }
        }
        if(bestHit != null) neighbors.add(bestHit);
        
        return neighbors;
    }
//}}}

//{{{ connectHeavyAtoms
//##############################################################################
    /** Finds the closest neighbor(s) of a given heavy atom */
    private Collection connectHeavyAtoms(AtomState query)
    {
        // See http://chemviz.ncsa.uiuc.edu/content/doc-resources-bond.html
        // and the Prekin source in PKINCSUB.c ::  connectheavyatom()
        _hits1.clear();
        final double toCNO, toOther;
        String qElem = query.getElement();
        boolean queryIsCNO = qElem.equals("C") || qElem.equals("N") || qElem.equals("O");
        boolean queryIsCNOP = queryIsCNO || qElem.equals("P");
        if(queryIsCNO)
        {
            toCNO   = 2.0 * 2.0;
            toOther = 2.2 * 2.2;
            this.stateBin.findSphere(query, 2.2, _hits1);
        }
        else // an "other" atom -- P, S, metal, etc
        {
            toCNO   = 2.2 * 2.2;
            toOther = 2.5 * 2.5;
            this.stateBin.findSphere(query, 2.5, _hits1);
        }

        Collection neighbors = new ArrayList( Math.min(4, _hits1.size()) );
        for(Iterator iter = _hits1.iterator(); iter.hasNext(); )
        {
            AtomState hit = (AtomState) iter.next();
            if(hit == query) continue;
            String hElem = hit.getElement();
            
            // We're bonded to an H iff the H is bonded to us --
            // this avoids H's with too many bonds to them.
            if(hElem.equals("H"))
            {
                Collection neighborsH = getNeighbors(hit);
                //if(neighborsH.contains(query)) ... -- nice, but uses equals() instead of ==
                if(neighborsH.size() >= 1 && neighborsH.iterator().next() == query)
                    neighbors.add(hit);
            }
            else
            {
                boolean hitIsCNO = hElem.equals("C") || hElem.equals("N") || hElem.equals("O");
                boolean chainsCompat = ignoreChains
                    || query.getResidue().getChain().equals(hit.getResidue().getChain())
                    || query.isHet()
                    || hit.isHet()
                    || !queryIsCNOP
                    || !(hitIsCNO || hElem.equals("P"));
                boolean sidechainsSameRes = true;
                if (!Util.isMainchain(query) & !Util.isMainchain(hit)) sidechainsSameRes = query.getResidue().equals(hit.getResidue());
                boolean possibleDisulfide = (query.getElement().equals("S") && hit.getElement().equals("S") && !sidechainsSameRes);
                //System.out.print(possibleDisulfide);
                double d2max;
                if(hitIsCNO)    d2max = toCNO;
                else            d2max = toOther;
                if(query.sqDistance(hit) <= d2max && chainsCompat && Util.altsAreCompatible(query, hit) && (sidechainsSameRes || possibleDisulfide))
                    neighbors.add(hit);
            }
            
        }
        return neighbors;
    }
//}}}

//{{{ setIgnoreChains
//##############################################################################
    /**
    * If true, AtomGraph will ignore chain IDs when figuring connectedness.
    * If false, chain IDs will be used in a pretty conservative way.
    * The default is false.
    * Changing the value of this property will cause any bonds that have
    * already been calculated to be flushed.
    */
    public void setIgnoreChains(boolean ignore)
    {
        if(ignore != ignoreChains)
        {
            this.ignoreChains = ignore;
            
            // flush out cached bonds: bonding decisions may change now!
            for(int i = 0; i < covNeighbors.length; i++) covNeighbors[i] = null;
            allBonds = null;
            unbondedAtoms = null;
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

