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
//}}}
/**
* <code>Neighborhood</code> is a local region of a macromolecular structure.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Mar 16 2010
*/
public class Neighborhood //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Residue  cen;
    Model    model;
    
    double   dist = Double.NaN;
    int      nPrev = -1, nNext = -1;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a structural neighborhood containing the specified central residue
    * plus all residues within a specified number of residues along the sequence.
    * @param res the central residue of the structural neighborhood
    * @param model a model containing the central residue
    * @param nPrev number of residues to include backwards (N-ward)
    * @param nNext number of residues to include forwards (C-ward)
    */
    public Neighborhood(Residue cen, Model model, int nPrev, int nNext) throws NullPointerException
    {
        if(cen == null) throw new NullPointerException("Must provide a non-null residue");
        if(model == null) throw new NullPointerException("Must provide a non-null model");
        this.cen = cen;
        this.model = model;
        this.nPrev = nPrev;
        this.nNext = nNext;
    }

    /**
    * Creates a structural neighborhood containing the specified central residue
    * plus all residues within a specified distance (including sidechain atoms).
    * @param res the central residue of the structural neighborhood
    * @param model a model containing the central residue
    * @param dist maximum allowed distance from the central residue
    */
    public Neighborhood(Residue cen, Model model, double dist) throws NullPointerException
    {
        if(cen == null) throw new NullPointerException("Must provide a non-null residue");
        if(model == null) throw new NullPointerException("Must provide a non-null model");
        this.cen = cen;
        this.model = model;
        this.dist = dist;
    }
//}}}

//{{{ getMembers
//##################################################################################################
    /**
    * Returns all residues in this structural neighborhood.
    * How that is defined (distance-based vs. sequence-based) 
    * is dictated by the constructor originally used.
    */
    public ArrayList<Residue> getMembers()
    {
        ArrayList<Residue> neighbors = new ArrayList<Residue>();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if(!Double.isNaN(dist)) // distance-based
            {
                try
                {
                    if(areWithin(res, cen, model, dist))  neighbors.add(res);
                }
               
                catch(AtomException ex)
                {
                    System.err.println("error determining if "
                        +res+" is within "+dist+"A of "+cen);
                }
            }
            else if(nPrev != -1 && nNext != -1) // sequence-based
            {
                int d = Math.abs(res.getSequenceInteger() - cen.getSequenceInteger());
                if(d <= nPrev && d <= nNext)  neighbors.add(res);
            }
        }
        return neighbors;
    }
//}}}

// OLD...
////{{{ getStretch
////##################################################################################################
//    /**
//    * Returns all residues within a specified number of residues along the chain
//    * in each direction from the central residue, <b>including</b> the central 
//    * residue.
//    * @param res the central residue of the structural neighborhood
//    * @param model a model containing the central residue
//    * @param dist maximum allowed distance from the central residue
//    */
//    public static ArrayList<Residue> getStretch(Residue res, Model model, double dist)
//    {
//        //if(res == null) throw new NullPointerException("Must provide a non-null residue");
//        //if(model == null) throw new NullPointerException("Must provide a non-null model");
//        
//        ArrayList<Residue> stretch = new ArrayList<Residue>();
//        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
//        {
//            ???
//        }
//        return stretch;
//    }
////}}}
//
////{{{ getAllWithin
////##################################################################################################
//    /**
//    * Returns all residues within a specified distance of the central residue 
//    * in sequence order, <b>including</b> the central residue.
//    * @param res the central residue of the structural neighborhood
//    * @param model a model containing the central residue
//    * @param dist maximum allowed distance from the central residue
//    */
//    public static ArrayList<Residue> getAllWithin(Residue res, Model model, double dist)
//    {
//        //if(res == null) throw new NullPointerException("Must provide a non-null residue");
//        //if(model == null) throw new NullPointerException("Must provide a non-null model");
//        
//        ArrayList<Residue> neighbors = new ArrayList<Residue>();
//        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
//        {
//            Residue oth = (Residue) iter.next();
//            try
//            {
//                if(areWithin(res, oth, dist) && !res.equals(oth))
//                    neighbors.add(oth);
//            }
//            catch(AtomException ex)
//            {
//                System.err.println("error determining if "+res+" and "
//                    +oth+" are within "+dist+"A of each other");
//            }
//        }
//        Collections.sort(neighbors);
//        return neighbors;
//    }
////}}}
//
////{{{ getOthersWithin
////##################################################################################################
//    /**
//    * Returns all residues within a specified distance of the central residue 
//    * in sequence order, <b>excluding</b> the central residue.
//    * @param res the central residue of the structural neighborhood
//    * @param model a model containing the central residue
//    * @param dist maximum allowed distance from the central residue
//    */
//    public static ArrayList<Residue> getOthersWithin(Residue res, Model model, double dist)
//    {
//        //if(res == null) throw new NullPointerException("Must provide a non-null residue");
//        //if(model == null) throw new NullPointerException("Must provide a non-null model");
//        
//        ArrayList<Residue> neighbors = new ArrayList<Residue>();
//        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
//        {
//            Residue oth = (Residue) iter.next();
//            try
//            {
//                if(areWithin(res, oth, dist))
//                    neighbors.add(oth);
//            }
//            catch(AtomException ex)
//            {
//                System.err.println("error determining if "+res+" and "
//                    +oth+" are within "+dist+"A of each other");
//            }
//        }
//        Collections.sort(neighbors);
//        return neighbors;
//    }
////}}}
// ...OLD

////{{{ areWithin
//##############################################################################
    /**
    * Returns true if any atom in the first residue is sufficiently close 
    * to any atom in the second residue, including sidechain atoms.
    * @param d threshold distance for any atom-atom pair between the two residues
    */
    public static boolean areWithin(Residue r1, Residue r2, Model m, double d) throws AtomException
    {
        ModelState s = m.getState();
        for(Iterator iter1 = r1.getAtoms().iterator(); iter1.hasNext(); )
        {
            AtomState a1 = s.get( (Atom) iter1.next() );
            for(Iterator iter2 = r2.getAtoms().iterator(); iter2.hasNext(); )
            {
                AtomState a2 = s.get( (Atom) iter2.next() );
                if(a1.distance(a2) < d) return true;
            }
        }
        return false;
    }
//}}}

//{{{ assignWeights
//##############################################################################
//    /**
//    * Assigns weights to the residues in local structural neighborhood 
//    * based on __?__ (inversely proportional to distance?), 
//    * which could be used for various averaging procedures.
//    */
//    public HashMap<Residue,Double> assignWeights()
//    {
//        HashMap<Residue,Double> weights = new HashMap<Residue,Double>();
//        
//        __?__
//        
//    }
//}}}
}//class
