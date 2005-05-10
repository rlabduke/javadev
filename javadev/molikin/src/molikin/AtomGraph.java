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
    * one covalent bond away from a particular atom. Starts empty and is filled lazily.
    * Indices match with those of atomStates and stateIndices.
    */
    Collection[]        covNeighbors;
    
    /**
    * All of the covalent Bonds for all of the states.
    * Fully populating this is expensive, so it's created lazily when requested.
    * Data will be plundered from covNeighbors when it's available.
    */
    SortedSet           allBonds = null;
    
    /**
    * Contains all AtomStates known not to have any covalent bonds to them.
    * Stays null until allBonds is populated.
    */
    Collection          unbondedAtoms = null;
    
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
    }
//}}}

//{{{ getCovalentBonds
//##############################################################################
    /**
    * Returns all the bonds between all the atoms in this graph.
    * For N atoms, this is an O(N) or O(N ln N) computation,
    * but it may still be reasonably expensive.
    * Results are cached for future calls to this function and to getCovalentNeighbors(),
    * and any cached results from previous getCovalentNeighbors() calls are used here.
    */
    public SortedSet getCovalentBonds()
    {
        if(this.allBonds == null)
        {
            // A Set is used so that duplicate bonds will be eliminated.
            this.allBonds = new TreeSet();
            this.unbondedAtoms = new ArrayList();
            for(int i = 0; i < atomStates.length; i++)
            {
                AtomState query = atomStates[i];
                Collection neighbors = this.getCovalentNeighbors(query); // forces neighbors to be found
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
        }
        return this.allBonds;
    }
//}}}

//{{{ getCovalentNeighbors
//##############################################################################
    /**
    * Gets all the AtomStates that the given AtomState is covalently bonded to.
    * AtomStates are compared by identity here, not equality, so use
    * one of the states passed to the constructor.
    */
    public Collection getCovalentNeighbors(AtomState query)
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
            if(Util.isH(query)) neighbors = connectHydrogens(query);
            else                neighbors = connectHeavyAtoms(query);
            this.covNeighbors[iQuery] = neighbors;
        }
        return neighbors;
    }
//}}}

//{{{ connectHydrogens
//##############################################################################
    /** Finds the closest covalent neighbor (JUST ONE) of a given H */
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
            if(Util.isH(hit) || Util.isQ(hit)) continue;
            
            double d2 = query.sqDistance(hit);
            if(d2 <= d2max && d2 < d2best && Util.altsAreCompatible(query, hit))
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
    /** Finds the closest covalent neighbor(s) of a given heavy atom */
    private Collection connectHeavyAtoms(AtomState query)
    {
        // See http://chemviz.ncsa.uiuc.edu/content/doc-resources-bond.html
        // and the Prekin source in PKINCSUB.c ::  connectheavyatom()
        _hits1.clear();
        final double toCNO, toOther;
        if(Util.isCNO(query))
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
            
            // We're bonded to an H iff the H is bonded to us --
            // this avoids H's with too many bonds to them.
            if(Util.isH(hit))
            {
                Collection neighborsH = getCovalentNeighbors(hit);
                //if(neighborsH.contains(query)) ... -- nice, but uses equals() instead of ==
                if(neighborsH.size() >= 1 && neighborsH.iterator().next() == query)
                    neighbors.add(hit);
            }
            else
            {
                double d2max;
                if(Util.isCNO(hit)) d2max = toCNO;
                else                d2max = toOther;
                if(query.sqDistance(hit) <= d2max && Util.altsAreCompatible(query, hit))
                    neighbors.add(hit);
            }
            
        }
        return neighbors;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

