// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>Interdigitator</code> reads in a PDB file and outputs information about
* the degree of sidechain "interdigitation."  Hopefully studying such numbers
* will be useful in determining whether such "interlockedness" is important for 
* determing protein structure and/or stability -- i.e. whether <b>directionality</b>
* and not just <b>amount</b> of vdW contact matters.
* 
* <p>Copyright (C) 2009 by Daniel Keedy. All rights reserved.
*/
public class Interdigitator //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("#.###");
    String bbAtoms = " N  , H  , CA , HA , C  , O  ";
    String protResnames =  "ALA,ARG,ASN,ASP,CYS,GLU,GLN,GLY,HIS,ILE,LEU,LYS,MET,PHE,PRO,SER,THR,TRP,TYR,VAL";
    String protResnamesNoGly = "ALA,ARG,ASN,ASP,CYS,GLU,GLN,HIS,ILE,LEU,LYS,MET,PHE,PRO,SER,THR,TRP,TYR,VAL";
//}}}

//{{{ Variable definitions
//##############################################################################
    // Basic I/O variables
    String    filename      =  null;
    boolean   verbose       =  false;
    String    outputMode    =  "atom"; // alternative: "res"
    boolean   outKin        =  false;  // print @arrowlist kin, not csv
    boolean   elimSurfRsds  =  true;   // based on multiple criteria
    boolean   bbOkOthAtom   =  false;  // OK if neighbor atom is bb (but primary atom must still be sc)
    
    /** Atoms in residues separated by this much or more can be considered for 
    * nearest neighbor */
    int  okSeqDiff  =  1;
    
    // 4.0 seems reasonable: Probe wouldn't pick up dots for any pair types (I think...),
    // but then again we don't necessarily need true contact to get interesting information
    /** Atoms separated by this many Angstroms or more can be considered for 
    * nearest neighbor */
    double  maxDist  =  4.0;
    
    /** For each non-backbone atom, closest-in-space atom (& vector to it) that is 
    * "distant in sequence" (i.e. separated by at least <code>okSeqDiff</code> residues) 
    * and "close in space" (i.e. no more than <code>maxDist</code> A away) */
    HashMap<Atom,Atom>    nearestNeighborAtms  =  null;
    HashMap<Atom,Triple>  nearestNeighborLocs  =  null;
    
    /** For each residue, a vector pointing from Calpha to sidechain-end center
    * of mass. Used to calculate packing moments (i.e. interdigitation) */
    HashMap<Residue,Triple>  scAxes  =  null;
    
    /** For each residue, a measure of interdigitation or "packing moment," 
    * defined as average over sidechain atoms of dot product of (atom to nearest
    * distant-in-sequence atom) with (Calpha to sidechain-end center of mass) */
    HashMap<Residue,Double>  packingMoments  =  null;
    
    /** Self-explanatory.  Uses all protein atoms, including H's & bb atoms */
    Triple  protCentroid  =  null;
    
    /** For each atom, vector to protein centroid */
    HashMap<Atom,Triple>  toProtCentroid  =  null;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Interdigitator()
    {
        super();
    }
//}}}

//{{{ processModel
//##############################################################################
    public void processModel(String modelName, Model model, ModelState state)
    {
        if(outputMode.equals("res"))
        {
            System.err.println("\n** Warning: Code doesn't yet correct for aa type in -res mode! **\n");
            //System.err.println("\n** Code doesn't yet correct for aa type in -res mode ... quitting! **\n");
            //System.exit(0);
        }
        
        // For each non-backbone atom, find vector to closest-in-space atom 
        // that is "distant-in-sequence"
        findNearestNeighbors(model, state);
        
        // Prep for packing moments by calculating sidechain "axes" 
        // (Calpha to sidechain-end center of mass vectors)
        calcScAxes(model, state);
        
        // For surface residue ID'ing and/or for kin
        calcProteinCentroid(model, state);
        
        if(elimSurfRsds)
        {
            // Eliminate "surface residues"
            eliminateSurfaceResidues(model, state);
        }
        
        if(outKin)
        {
            // Print kin of (mostly) arrows
            System.out.println("@group {interdigitator!} collapsable");
            printScAxes(model, state);
            printNearestNeighbors(model, state);
            printCasToProtCentroid(state);
        }
        else
        {
            // Actually calculate per-residue packing moments & print csv
            printPackingMoments(model);
        }
        
        // done!
    }
//}}}


//{{{ findNearestNeighbor(s)
//##############################################################################
    /**
    * For each non-backbone atom, finds vector to closest-in-space atom that is
    * "distant in sequence" and "close in space"
    */
    public void findNearestNeighbors(Model model, ModelState state)
    {
        nearestNeighborLocs = new HashMap<Atom,Triple>();
        nearestNeighborAtms = new HashMap<Atom,Atom>();
        
        for(Iterator r = model.getResidues().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            if(protResnamesNoGly.indexOf(res.getName()) != -1) // no Gly; sc axis would be weird
            {
                for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
                {
                    Atom atom = (Atom) a.next();
                    if(bbAtoms.indexOf(atom.getName()) == -1)
                    {
                        try
                        {
                            findNearestNeighbor(model, state, res, atom);
                        }
                        catch(AtomException ae)
                        {
                            System.err.println("Can't process "+atom+" to find nearest neighbor");
                        }
                    }
                }//per atom
            }
        }//per residue
    }

    /**
    * For one given non-backbone atom, finds vector to closest-in-space atom that is 
    * "distant in sequence" (i.e. separated by at least <code>okSeqDiff</code> residues) 
    * and "close in space" (i.e. no more than <code>maxDist</code> A away)
    */
    public void findNearestNeighbor(Model model, ModelState state, Residue res, Atom atom) throws AtomException
    {
        AtomState atomPos = state.get(res.getAtom(atom.getName()));
        double minDist = Double.POSITIVE_INFINITY;
        AtomState minDistAtomPos = null;
        Atom      minDistAtom    = null;
        
        for(Iterator r = model.getResidues().iterator(); r.hasNext(); )
        {
            Residue othRes = (Residue) r.next();
            if( bbOkOthAtom && protResnames.indexOf(res.getName()) == -1)      continue;
            if(!bbOkOthAtom && protResnamesNoGly.indexOf(res.getName()) == -1) continue; 
            
            if(res.getSequenceInteger() >= othRes.getSequenceInteger() + okSeqDiff
            || res.getSequenceInteger() <= othRes.getSequenceInteger() - okSeqDiff)
            {
                for(Iterator a = othRes.getAtoms().iterator(); a.hasNext(); )
                {
                    Atom othAtom = (Atom) a.next();
                    if(!bbOkOthAtom && bbAtoms.indexOf(othAtom.getName()) != -1) continue;
                    // ^ other atom is bb: not OK (opt'l)
                    // else either bb or sc OK here b/c in surroundings
                    
                    try
                    {
                        AtomState othAtomPos = state.get(othRes.getAtom(othAtom.getName()));
                        if(Triple.distance(atomPos, othAtomPos) < minDist)
                        {
                            minDist = Triple.distance(atomPos, othAtomPos);
                            minDistAtomPos = othAtomPos;
                            minDistAtom = othAtom;
                        }
                    }
                    catch(AtomException ae)
                    {
                        System.err.println("Can't process "+othAtom+" to consider as nearest neighbor to "+atom);
                    }
                }//per other atom
            }
        }//per other residue
        
        if(minDist > maxDist)
        {
            if(verbose) System.err.println(atom+" nearest neighbor is "+df.format(minDist)+" away"
                +" - too far (>"+df.format(maxDist)+")!");
        }
        else
        {
            if(verbose) System.err.println(atom+" nearest neighbor is "+df.format(minDist)+" away");
            nearestNeighborLocs.put(atom, new Triple().likeVector(atomPos, minDistAtomPos));
            nearestNeighborAtms.put(atom, minDistAtom);
        }
    }
//}}}

//{{{ calcScAxis/es
//##############################################################################
    /**
    * For each residue, calculate & store vector from Calpha to sidechain-end 
    * center of mass.
    */
    public void calcScAxes(Model model, ModelState state)
    {
        scAxes = new HashMap<Residue,Triple>();
        
        for(Iterator r = model.getResidues().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            if(protResnamesNoGly.indexOf(res.getName()) != -1) // no Gly; sc axis would be weird
            {
                try
                {
                    Triple scAxis = calcScAxis(model, state, res);
                    if(verbose)
                    {
                        if(scAxis != null) System.err.println("found sc axis for "+res);
                        else               System.err.println(res+" sc axis null");
                    }
                    scAxes.put(res, scAxis);
                }
                catch(AtomException ae)
                {
                    System.err.println("Can't process "+res+" to calculate sc axis");
                }
            }
        }//per residue
    }

    /**
    * For one given residue, calculate vector from Calpha to sidechain- 
    * end center of mass (actually just centroid of all sidechain atoms)
    */
    public Triple calcScAxis(Model model, ModelState state, Residue res) throws AtomException
    {
        Atom   ca    = res.getAtom(" CA ");
        Triple caLoc = new Triple(state.get(ca) );
        
        ArrayList<Triple> scAtLocs = new ArrayList<Triple>();
        for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
        {
            Atom atom = (Atom) a.next();
            if(bbAtoms.indexOf(atom.getName()) == -1)
            {
                Triple scAtLoc = new Triple( state.get(atom) );
                scAtLocs.add(scAtLoc);
            }
        }//per atom
        
        // Calculate center of mass (actually centroid b/c ignoring differences in atomic mass)
        Triple scCentroid = calcCentroid(scAtLocs);
        Triple scAxis = new Triple().likeVector(caLoc, scCentroid);
        return scAxis;
    }
//}}}

//{{{ calcProteinCentroid
//##############################################################################
    /**
    * Calculates (non-mass-weighted) centroid of whole protein based on all atoms
    * and vectors to it from all atoms.
    */
    public void calcProteinCentroid(Model model, ModelState state)
    {
        // Calculate protein centroid
        ArrayList<Triple> allAtomLocs = new ArrayList<Triple>();
        for (Iterator r = scAxes.keySet().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
            {
                Atom atom = (Atom) a.next();
                try
                {
                    allAtomLocs.add( new Triple(state.get(atom)) );
                }
                catch (AtomException ae)
                {
                    System.err.println("Can't process "+atom+" to calculate protein centroid");
                }
            }//per atom
        }//per residue
        protCentroid = calcCentroid(allAtomLocs);  // set global variable
        
        // Get all vectors to protein centroid for all protein atoms
        toProtCentroid = new HashMap<Atom,Triple>();  // also a global variable
        for(Iterator r = model.getResidues().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            if (protResnames.indexOf(res.getName()) != -1)
            {
                for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
                {
                    Atom atom = (Atom) a.next();
                    try
                    {
                        Triple atLoc = new Triple(state.get(atom));
                        toProtCentroid.put(atom, new Triple().likeVector(atLoc, protCentroid));
                    }
                    catch (AtomException ae)
                    {
                        System.err.println("Can't process "+atom+" to calculate vector -> centroid");
                    }
                }//per atom
            }
        }//per residue
    }
//}}}

//{{{ calcCentroid
//##############################################################################
    /**
    * Calculates centroid of generic set of triples
    */
    public Triple calcCentroid(ArrayList<Triple> pts)
    {
        int count = 0;
        double x = 0, y = 0, z = 0;
        for (Triple pt : pts)
        {
            x += pt.getX();  y += pt.getY();  z += pt.getZ();  count++;
        }
        Triple centroid = new Triple( x/(1.0*count), y/(1.0*count), z/(1.0*count) );
        return centroid;
    }
//}}}


//{{{ eliminateSurfaceResidues
//##############################################################################
    /**
    * Eliminate "surface residues," i.e. those either:<ul>
    *  <li>with no near neighbors (completely exposed), or</li>
    *  <li>with at least one atom whose nearest neighbor is a water, or</li>
    *  <li>whose Calpha is in 90th percentile of distance from protein centroid, or</li>
    *  <li>whose Calpha is in 80th percentile of distance from protein centroid AND 
    *      whose sidechain is pointed outward</li>
    * </ul> by deleting them and all their constituent atoms from the global hashes.
    *
    * The water thing alone is a really dumb way to do it 'cause if the 
    * crystallographer left out waters or two sidechains are on the surface but 
    * are just closer to each other than they are to any waters, this method 
    * won't get rid of 'em.  But combined with the distal + pointed outward 
    * and very distal criteria, it should be OK...
    */
    public void eliminateSurfaceResidues(Model model, ModelState state)
    {
        // Figure out which other residues to eliminate
        TreeSet<Residue> noNeighbors      = getResiduesWithNoNeighbors(model, state);
        TreeSet<Residue> nearWater        = getResiduesNearWater(state);
        TreeSet<Residue> distalAndOutward = getDistalResiduesPointedOutward(model, state);
        TreeSet<Residue> veryDistal       = getVeryDistalResidues(model, state);
        
        TreeSet<Residue> rsdsToRemove = new TreeSet<Residue>();
        for(Residue res : noNeighbors)      rsdsToRemove.add(res);
        for(Residue res : nearWater)        rsdsToRemove.add(res);
        for(Residue res : distalAndOutward) rsdsToRemove.add(res);
        for(Residue res : veryDistal)       rsdsToRemove.add(res);
        
        // Remove those residues & their constituent atoms from hashes
        for(Iterator r = rsdsToRemove.iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            scAxes.remove(res);
            for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
            {
                Atom atom = (Atom) a.next();
                if(bbAtoms.indexOf(atom.getName()) == -1)
                {
                    nearestNeighborAtms.remove(atom);
                    nearestNeighborLocs.remove(atom);
                }
            }//per atom
        }//per residue
    }
//}}}

//{{{ getResiduesWithNoNeighbors
//##############################################################################
    /**
    * Returns list of residues with no atoms with a "near neighbor"
    * (i.e. another "distant in sequence" atom < <code>maxDist</code> A away)
    */
    public TreeSet<Residue> getResiduesWithNoNeighbors(Model model, ModelState state)
    {
        TreeSet<Residue> rsds = new TreeSet<Residue>();
        
        for(Iterator r = scAxes.keySet().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            int neighborCount = 0;
            for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
            {
                Atom atom = (Atom) a.next();
                if(bbAtoms.indexOf(atom.getName()) == -1)
                {
                    Atom othAtom = nearestNeighborAtms.get(atom);
                    if(othAtom != null) neighborCount++;
                }
                
            }//per atom
            //if(verbose) System.err.println("("+res+" has "+neighborCount+" near neighbors)");
            if(neighborCount == 0)
            {
                if(verbose) System.err.println("removing "+res+" b/c no near neighbors");
                rsds.add(res);
            }
        }//per residue
        
        return rsds;
    }
//}}}

//{{{ getResiduesNearWater
//##############################################################################
    /**
    * Returns list of residues with at least one atom whose nearest neighbor is 
    * a water.
    */
    public TreeSet<Residue> getResiduesNearWater(ModelState state)
    {
        TreeSet<Residue> rsds = new TreeSet<Residue>();
        
        for(Iterator r = scAxes.keySet().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
            {
                Atom atom = (Atom) a.next();
                if(bbAtoms.indexOf(atom.getName()) == -1)
                {
                    Atom othAtom = nearestNeighborAtms.get(atom);
                    if(othAtom == null) continue; // this atom has no near neighbor
                    String othResName = othAtom.getResidue().getName();
                    if(othResName.equals("HOH") || othResName.equals("H2O") || othResName.equals(" WAT"))
                    {
                        if(verbose) System.err.println("removing "+res+" b/c a sc atom touches water");
                        rsds.add(res);
                    }
                }
            }//per atom
        }//per residue
        
        return rsds;
    }
//}}}

//{{{ getDistalResiduesPointedOutward
//##############################################################################
    /**
    * Returns list of residues considered "surface" because both:
    *
    * (1) their Calpha is in the top 80% of distances to protein centroid, and
    * (2) their sc axes are pointed "away" (i.e. >90deg) from the Calpha -> protein 
    *     centroid vector
    *
    * Either of these criteria alone could find some "false positive" "surface" 
    * residues, but taken together I think they'll mostly find what we want.
    */
    public TreeSet<Residue> getDistalResiduesPointedOutward(Model model, ModelState state)
    {
        // Get residues sufficiently distal from centroid to be suspected as 
        // surface (i.e. their Ca in top 80%)
        double maxDistToCentroid = calcMaxDistToCentroid(0.8);
        TreeSet<Residue> distal = new TreeSet<Residue>();
        for(Iterator tc = toProtCentroid.keySet().iterator(); tc.hasNext(); )
        {
            Atom atom = (Atom) tc.next();
            Triple atomToCentroid = toProtCentroid.get(atom);
            if(atomToCentroid.mag() > maxDistToCentroid)  distal.add(atom.getResidue());
        }
        
        // Also get residues pointed outward (sc axes >90deg from Calpha -> centroid)
        TreeSet<Residue> outward = new TreeSet<Residue>();
        for(Iterator r = scAxes.keySet().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            Triple scAxis = scAxes.get(res);
            Triple caToProtCentroid = toProtCentroid.get( res.getAtom(" CA ") );
            if(scAxis.angle(caToProtCentroid) > 90)  outward.add(res);
        }
        
        // Return intersection of residues
        TreeSet<Residue> intersect = new TreeSet<Residue>();
        for(Iterator d = distal.iterator();  d.hasNext(); )
        {
            Residue res = (Residue) d.next();
            if(outward.contains(res))  intersect.add(res);
        }
        if(verbose) for(Iterator i = intersect.iterator(); i.hasNext(); )
        {
            Residue res = (Residue) i.next();
            System.err.println("removing "+res+" b/c a sc atom is ptd away & Ca is pretty far from centroid");
        }
        return intersect;
    }
//}}}

//{{{ getVeryDistalResidues
//##############################################################################
    /**
    * Returns list of residues considered "surface" because their Calpha is in 
    * the top 90% of distances to protein centroid.  This criterion seems strict
    * enough that no additional side-by-side criterion should be needed (?).
    *
    * In practice, this doesn't remove many additional residues beyond what 
    * getDistalResiduesPointedOutward() already does.
    */
    public TreeSet<Residue> getVeryDistalResidues(Model model, ModelState state)
    {
        // (Already have protein centroid & all vectors to protein centroid 
        //  for all protein atoms: proteinCentroid & toProtCentroid)
        
        double maxDistToCentroid = calcMaxDistToCentroid(0.9);  // stricter!
        TreeSet<Residue> veryDistal = new TreeSet<Residue>();
        for(Iterator tc = toProtCentroid.keySet().iterator(); tc.hasNext(); )
        {
            Atom atom = (Atom) tc.next();
            Triple atomToCentroid = toProtCentroid.get(atom);
            if(atomToCentroid.mag() > maxDistToCentroid)
            {
                Residue res = atom.getResidue();
                if(verbose) System.err.println("removing "+res+" b/c Ca is *very* far from centroid");
                veryDistal.add(res);
            }
        }
        
        return veryDistal;
    }
//}}}

//{{{ calcMaxDistToCentroid
//##############################################################################
    /**
    * Calculates cutoff on distance to protein centroid beyond which residues 
    * are likely to be "surface."
    * @param cutoffFrac e.g. 0.8 means 80% of atoms are close enough to not be "surface"
    */
    public double calcMaxDistToCentroid(double cutoffFrac)
    {
        ArrayList<Double> dists = new ArrayList<Double>();
        for(Iterator i = toProtCentroid.keySet().iterator(); i.hasNext(); )
        {
            Atom atom = (Atom) i.next();
            dists.add( toProtCentroid.get(atom).mag() );
        }
        
        Collections.sort(dists);
        int cutoffIdx  = (int) Math.floor(dists.size() * cutoffFrac);
        return dists.get(cutoffIdx);
    }
//}}}


//{{{ printScAxes
//##############################################################################
    /**
    * Print @arrowlist for each Ca -> sc centroid.
    */
    public void printScAxes(Model model, ModelState state)
    {
        System.out.println("@subgroup {sc axis} dominant");
        System.out.println("@arrowlist {sc axis} color= hotpink");
        
        for(Iterator r = scAxes.keySet().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            if(protResnamesNoGly.indexOf(res.getName()) != -1) // no Gly (sc axis would be weird)
            {
                try
                {
                    Triple axis = new Triple(scAxes.get(res));
                    Triple ext  = new Triple(axis).unit().mult(0.1);
                    Triple base = new Triple(state.get(res.getAtom(" CA "))).add(ext);
                    Triple tip  = new Triple(state.get(res.getAtom(" CA "))).add(axis).sub(ext);
                    
                    System.out.println( "{"+res+" sc axis base}P "+
                        df.format(base.getX())+" "+
                        df.format(base.getY())+" "+
                        df.format(base.getZ()) );
                    System.out.println( "{"+res+" sc axis tip} "+
                        df.format(tip.getX())+" "+
                        df.format(tip.getY())+" "+
                        df.format(tip.getZ()) );
                }
                catch(AtomException ae)
                {
                    System.err.println("Can't process "+res+" to draw sc axis");
                }
            }
        }//per residue
    }
//}}}

//{{{ printNearestNeighbors
//##############################################################################
    /**
    * Print @arrowlist for each sc atom -> nearest inter-residue atom.
    */
    public void printNearestNeighbors(Model model, ModelState state)
    {
        System.out.println("@subgroup {sc -> neighbor} dominant");
        System.out.println("@arrowlist {sc -> neighbor} color= green alpha= 0.3");
        
        for(Iterator r = scAxes.keySet().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            if(protResnamesNoGly.indexOf(res.getName()) != -1) // no Gly; not sc
            {
                for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
                {
                    Atom atom = (Atom) a.next();
                    if(bbAtoms.indexOf(atom.getName()) == -1)
                    {
                        Atom othAtom = nearestNeighborAtms.get(atom);
                        if(othAtom == null) continue; // this atom has no near neighbor
                        try
                        {
                            Triple axis = nearestNeighborLocs.get(atom);
                            Triple ext  = new Triple(axis).unit().mult(0.2);
                            Triple base = new Triple(state.get(atom)).add(ext);
                            Triple tip  = new Triple(state.get(atom)).add(axis).sub(ext);
                            
                            System.out.println( "{"+atom+" -> "+othAtom+"}P "+
                                df.format(base.getX())+" "+
                                df.format(base.getY())+" "+
                                df.format(base.getZ()) );
                            System.out.println( "{"+atom+" -> "+othAtom+"} "+
                                df.format(tip.getX())+" "+
                                df.format(tip.getY())+" "+
                                df.format(tip.getZ()) );
                        }
                        catch(AtomException ae)
                        {
                            System.err.println("Can't process "+atom+" to draw arrow to neighbor");
                        }
                    }
                }//per atom
            }
        }//per residue
    }
//}}}

//{{{ printCasToProtCentroid
//##############################################################################
    /**
    * Print @arrowlist for Ca -> protein centroid for each NON-SURFACE residue,
    * as well as @ball for protein centroid.
    */
    public void printCasToProtCentroid(ModelState state)
    {
        System.out.println("@subgroup {prot centroid} dominant off");
        System.out.println("@balllist {prot centroid} radius= 0.5 color= lilac");
        System.out.println("{prot centroid} " +df.format(protCentroid.getX())+" "+
            df.format(protCentroid.getY())+" "+df.format(protCentroid.getZ()));
        System.out.println("@arrowlist {Ca -> prot centroid} width= 3 color= lilactint alpha= 0.5");
        
        for(Iterator a = toProtCentroid.keySet().iterator(); a.hasNext(); )
        {
            Atom atom = (Atom) a.next();
            if(atom.getName().equals(" CA "))
            {
                Residue res = atom.getResidue();
                if(scAxes.keySet().contains(res)) // scAxes has already had surface residues removed
                {
                    try
                    {
                        Triple axis = new Triple(protCentroid).sub(new Triple(state.get(atom)));
                        Triple ext  = new Triple(axis).unit().mult(0.1);
                        Triple base = new Triple(state.get(atom)).add(ext);
                        Triple tip  = protCentroid;
                        
                        System.out.println("{"+atom+" -> prot centroid}P "+
                            df.format(base.getX())+" "+
                            df.format(base.getY())+" "+
                            df.format(base.getZ()));
                        System.out.println("{"+atom+" -> prot centroid} "+
                            df.format(tip.getX())+" "+
                            df.format(tip.getY())+" "+
                            df.format(tip.getZ()));
                    }
                    catch(AtomException ae)
                    {
                        System.err.println("Can't process "+atom+" to draw CA -> prot centroid");
                    }
                }
            }//per non-surface Calpha
        }
    }
//}}}

//{{{ printPackingMoments
//##############################################################################
    /**
    * For each residue, calculate average over sidechain atoms of dot product of
    * (atom to nearest distant-in-sequence atom) with (Calpha to sidechain-end 
    * center of mass).
    * Then print csv of resulting per-residue or per-atom packing "moments".
    */
    public void printPackingMoments(Model model)
    {
        if(outputMode.equals("res")) System.out.println("rsd,#cntribAtms,avAng_scAx_atm2nghbr");
        else                         System.out.println("atm,ang_scAx_atm2nghbr");
        
        for(Iterator r = model.getResidues().iterator(); r.hasNext(); )
        {
            Residue res = (Residue) r.next();
            Triple scAxis = scAxes.get(res);
            if(scAxis == null)
            {
                if(verbose) System.err.println(res+" sc axis null");
                continue; // next residue
            }
            if(protResnamesNoGly.indexOf(res.getName()) != -1) // no Gly; sc axis would be weird
            {
                // angles between (atom -> nearest neigbhor) and (sc axis)
                ArrayList<Double> angles = new ArrayList<Double>();
                for(Iterator a = res.getAtoms().iterator(); a.hasNext(); )
                {
                    Atom atom = (Atom) a.next();
                    if(bbAtoms.indexOf(atom.getName()) == -1)
                    {
                        Triple nearestNeighbor = nearestNeighborLocs.get(atom);
                        if(nearestNeighbor == null)
                        {
                            if(verbose) System.err.println(res+" nearest neighbor null");
                            continue; // next atom
                        }
                        else
                        {
                            double angle = scAxis.angle(nearestNeighbor);
                            angles.add(angle);
                            if(outputMode.equals("atom")) System.out.println(atom+","+df.format(angle));
                        }
                    }
                }//per atom
                
                if(angles.size() > 0)
                {
                    double sum = 0; int count = 0;
                    for(double angle : angles)
                    {
                        sum += angle;
                        count++;
                    }
                    double avg = sum / (1.0*count);
                    if(outputMode.equals("res")) System.out.println(res+","+count+","+df.format(avg));
                }
                else if(verbose)
                    System.err.println("No atoms available to calculate packing moment for "+res);
            }
        }//per residue
    }
//}}}


//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        File file = new File(filename);
        LineNumberReader in = new LineNumberReader(new FileReader(file));
        PdbReader pdbReader = new PdbReader();
        CoordinateFile cf = pdbReader.read(in);
        
        Model m = cf.getFirstModel();
        ModelState state = m.getState();
        processModel(cf.getIdCode(), m, state);
    }
    
    public static void main(String[] args)
    {
        Interdigitator mainprog = new Interdigitator();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
    }
//}}}

//{{{ parseArguments, showHelp
//##############################################################################
    /**
    * Parse the command-line options for this program.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    void parseArguments(String[] args)
    {
        String  arg, flag, param;
        boolean interpFlags = true;
        
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if(!arg.startsWith("-") || !interpFlags || arg.equals("-"))
            {
                // This is probably a filename or something
                interpretArg(arg);
            }
            else if(arg.equals("--"))
            {
                // Stop treating things as flags once we find --
                interpFlags = false;
            }
            else
            {
                // This is a flag. It may have a param after the = sign
                int eq = arg.indexOf('=');
                if(eq != -1)
                {
                    flag    = arg.substring(0, eq);
                    param   = arg.substring(eq+1);
                }
                else
                {
                    flag    = arg;
                    param   = null;
                }
                
                try { interpretFlag(flag, param); }
                catch(NullPointerException ex)
                { throw new IllegalArgumentException("'"+arg
                    +"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("Interdigitator.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Interdigitator.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.Interdigitator");
        System.err.println("Copyright (C) 2009 by Daniel Keedy. All rights reserved.");
    }

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ interpretArg, interpretFlag
//##############################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
        if (filename == null)
            filename = arg;
        else
            System.out.println("Didn't need "+arg+"; already have file "+filename);
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-v") || flag.equals("-verbose"))
        {
            verbose = true;
        }
        else if(flag.equals("-k") || flag.equals("-kin"))
        {
            outKin = true;
        }
        else if(flag.equals("-r") || flag.equals("-res"))
        {
            outputMode = "res";
        }
        else if(flag.equals("-a") || flag.equals("-atom"))
        {
            outputMode = "atom";
        }
        else if(flag.equals("-keepsurf"))
        {
            elimSurfRsds = false;
        }
        else if(flag.equals("-bbok"))
        {
            bbOkOthAtom = true;
        }
        else if(flag.equals("-okseqdiff"))
        {
            try
            { okSeqDiff = Integer.parseInt(param); }
            catch(NumberFormatException ex)
            { System.err.println("Can't format "+param+" as integer!  Using default okSeqDiff = "+okSeqDiff); }
        }
        else if(flag.equals("-maxdist"))
        {
            try
            { maxDist = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { System.err.println("Can't format "+param+" as double!  Using default maxDist = "+maxDist); }
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

