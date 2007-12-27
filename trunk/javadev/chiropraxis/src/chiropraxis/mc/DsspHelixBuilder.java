// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

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
* <code>DsspHelixBuilder</code> was modified from HelixBuilder to use an 
* implementation of the Kabsch & Sander DSSP algorithm instead of my heuristic
* method for figuring out which residues are in a helix.
* It's not a perfect replication of the DSSP algorithm for some reason; e.g.,
* it splits up a stretch of helical residues that are actually two kinked 
* helices according to DSSP into two helices.
* Like HelixBuilder, this class creates Helix objects, which hold information 
* about alpha helices, for an input PDB file.
* Specifically, it measures parameters related to the N cap of each helix in 
* the file.
* Note that this now contains some bug fixes (e.g. handles Pro at N2 or N3) that
* the original HelixBuilder does not.
* 
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class DsspHelixBuilder //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String filename                = null;
    ArrayList<NTurn> nTurns        = null;
    ArrayList<MinHelix> minHelices = null;
    ArrayList<NTurn[]> stretches   = null;
    ArrayList<Helix> helices       = null;
    boolean doNcaps                = false;
    boolean onlyHbNcaps            = false;
    boolean doKin                  = false;
    boolean doPrint                = true;
    boolean smoothAxes             = false;
    int smoothAxesTimes            = 0;
    boolean vectorSumAxis          = false;
    boolean verbose                = false;
    boolean noKinHeading           = false;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DsspHelixBuilder()
    {
        super();
    }
//}}}



//{{{ processModel
//##############################################################################
    void processModel(String modelName, Model model, ModelState state)
    {
        // Create a set of Peptides and connect them up
        Collection peptides = createPeptides(model, state);
        connectPeptides(peptides);
        findHBonds(peptides, state);
        getPsiPhis(peptides, state);
        
        // Try to identify *helix* based on H-bonding pattern
        buildMinHelices(peptides, model);
        buildHelices(model, state);
        
        // Axis & Ncap stuff
        findAxes(model, state);
        if (smoothAxes)
            for (int i = 0; i < smoothAxesTimes; i ++)  smoothAxes();
        if (doNcaps)    findNcaps(model, state);
        
        if (doKin)
        {
            if (!noKinHeading)
                System.out.println("@kinemage {"+filename+" helices}");
            sketchHbonds(System.out, peptides, state);
            sketchNcaps(System.out, state);
            sketchAxes(System.out);
        }
        if (verbose)
            printHelicalPeptides(System.out, peptides);
    }
//}}}

//{{{ createPeptides
//##############################################################################
    /**
    * Given a model and a state, create Peptide objects for all the "complete"
    * peptides in the model.
    * These fields will be filled: nRes, cRes, midpoint
    */
    Collection createPeptides(Model model, ModelState state)
    {
        ArrayList peptides = new ArrayList();
        Residue prev = null;
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if(! AminoAcid.isAminoAcid(res)) continue;
            
            try
            {
                Peptide pep = new Peptide(prev, res, state); // prev could be null
                // If prev is null, no distance check.
                if(prev == null) peptides.add(pep);
                // If we have two residues, make sure they're connected,
                // or else do two separate half-peptides.
                else
                {
                    AtomState pepC = state.get(prev.getAtom(" C  "));
                    AtomState pepN = state.get(res.getAtom(" N  "));
                    if(pepC.sqDistance(pepN) < 4.0) // within 2 A of each other
                        peptides.add(pep);
                    else
                    {
                        peptides.add(new Peptide(prev, null, state));
                        peptides.add(new Peptide(null, res,  state));
                    }
                }
            }
            catch(AtomException ex) // missing atoms? try halves.
            {
                try { peptides.add(new Peptide(prev, null, state)); }
                catch(AtomException ex2) {}
                try { peptides.add(new Peptide(null, res,  state)); }
                catch(AtomException ex2) {}
            }
            prev = res;
        }//for all residues
        
        // Add last residue as a half-peptide
        try { peptides.add(new Peptide(prev, null, state)); }
        catch(AtomException ex) {}
        
        return peptides;
    }
//}}}

//{{{ connectPeptides
//##############################################################################
    /**
    * Given an ordered collection of peptides, connect them on the criteria that
    * successive peptides must share a common residue between them.
    * These fields will be filled: prev, next, chain, index.
    * Chain and index will be indexed starting from 1, not 0.
    */
    void connectPeptides(Collection peptides)
    {
        Peptide prev = null;
        int chain = 0;
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pept = (Peptide) iter.next();
            if(prev != null && prev.nRes != null && pept.cRes != null && prev.nRes == pept.cRes)
            {
                // Chain is continuous
                prev.next = pept;
                pept.prev = prev;
                pept.chain = prev.chain;
                pept.index = prev.index+1;
            }
            else
            {
                // Chain is broken
                pept.chain = ++chain;
                pept.index = 1;
            }
            prev = pept;
        }
    }
//}}}

//{{{ findHBonds
//##############################################################################
    /**
    * Maps out all the inter-peptide H-bonds based on the criteria defined in
    * W. Kabsch and C. Sander (1983) Biopolymers, 22:2577.
    * The basic idea is that the H-bond is accepted if
    * E = 0.42*0.20*332*(1/rON + 1/rCH - 1/rOH - 1/rCN) is less than -0.5 kcal/mol.
    * Atom-atom distances are in Angstroms and E is in kcal/mol.
    * Ideal alignment allows distances up to 5.2 A (O to N);
    * ideal distance allows angles up to 63 degrees.
    * Be careful -- it will try to pick up i to {i, i+1, i+2} "H-bonds".
    * Only the strongest H-bond for each N to an unbonded O is kept.
    * These fields will be filled: hbondN, hbondO.
    */
    void findHBonds(Collection peptides, ModelState state)
    {
        Peptide[] pep = (Peptide[]) peptides.toArray(new Peptide[peptides.size()]);
        // Do carbon/oxygen lookup just once
        AtomState[] carbon = new AtomState[pep.length];
        AtomState[] oxygen = new AtomState[pep.length];
        for(int i = 0; i < pep.length; i++)
        {
            if(pep[i].cRes != null) try {
                carbon[i] = state.get(pep[i].cRes.getAtom(" C  "));
                oxygen[i] = state.get(pep[i].cRes.getAtom(" O  "));
            } catch(AtomException ex) {} // left as null
        }
        // For each N/H, look for bonded C/O
        final double maxNOdist2 = 5.2*5.2; //5.3*5.3;
        for(int i = 0; i < pep.length; i++)
        {
            if(pep[i].nRes != null) try
            {
                AtomState nitrogen = state.get(pep[i].nRes.getAtom(" N  "));
                AtomState hydrogen = state.get(pep[i].nRes.getAtom(" H  "));
                Peptide bestBond = null;
                double bestBondE = -0.5;
                for(int j = 0; j < pep.length; j++)
                {
                    if(i == j) continue; // no intra-peptide H-bonds
                    if(pep[i].chain == pep[j].chain && Math.abs(pep[i].index - pep[j].index) <= 2)
                        continue; // no i to {i, i+1, i+2} H-bonds!
                    if(carbon[j] == null || oxygen[j] == null) continue;
                    if(nitrogen.sqDistance(oxygen[j]) > maxNOdist2) continue;
                    
                    double rON = oxygen[j].distance(nitrogen);
                    double rCH = carbon[j].distance(hydrogen);
                    double rOH = oxygen[j].distance(hydrogen);
                    double rCN = carbon[j].distance(nitrogen);
                    double energy = 27.9*(1/rON + 1/rCH - 1/rOH - 1/rCN);
                    if(energy < bestBondE && pep[j].hbondO == null)
                    {
                        bestBond = pep[j];
                        bestBondE = energy;
                    }
                }//for all possible partners
                if(bestBond != null)
                {
                    pep[i].hbondN = bestBond;
                    bestBond.hbondO = pep[i];
                }
            }
            catch(AtomException ex) {} // no connections then
        }//for each peptide N
    }
//}}}

//{{{ getPsiPhis
//##############################################################################
    /**
    * For each peptide (Ca i-1, C i-1, N i, Ca i), set the psi for the N-most
    * residue and the phi for the C-most residue.
    * This will be used to assign secondary structure below (phi,psi in helical
    * range).
    */
    void getPsiPhis(Collection peptides, ModelState state)
    {
        Peptide[] pep = (Peptide[]) peptides.toArray(new Peptide[peptides.size()]);
        for (int i = 0; i < pep.length; i++)
        {
            if (pep[i].nRes != null && pep[i].cRes != null) try
            {
                // Get psiN
                AtomState N_iminus1  = state.get(pep[i].nRes.getAtom(" N  "));
                AtomState Ca_iminus1 = state.get(pep[i].nRes.getAtom(" CA "));
                AtomState C_iminus1  = state.get(pep[i].nRes.getAtom(" C  "));
                AtomState N_i        = state.get(pep[i].cRes.getAtom(" N  "));
                pep[i].psiN = new Triple().dihedral(N_iminus1, Ca_iminus1,
                    C_iminus1, N_i);
                
                // Get phiC
                //AtomState C_iminus1  = state.get(pep[i].nRes.getAtom(" C  "));
                //AtomState N_i        = state.get(pep[i].cRes.getAtom(" N  "));
                AtomState Ca_i       = state.get(pep[i].cRes.getAtom(" CA "));
                AtomState C_i        = state.get(pep[i].cRes.getAtom(" C  "));
                pep[i].phiC = new Triple().dihedral(C_iminus1, N_i, Ca_i, C_i);
                
                //if (verbose)
                //{
                //    System.out.println("**"+pep[i]);
                //    System.out.println("psiN: "+pep[i].psiN+", phiC: "+pep[i].phiC);
                //    System.out.println();
                //}
            } 
            catch (AtomException ex) {} // left as null
        }
    }
//}}}

//{{{ CLASS: NTurn
//##############################################################################
    public static class NTurn
    {
        int n = 0;
        Residue firstRes = null, lastRes = null;
        
        public NTurn(Residue first, Residue last, int length)
        {
            firstRes = first;
            lastRes  = last;
            n        = length;
        }
        
        // UNUSED
        public int overlap(NTurn other)
        {
            TreeSet<Integer> thisResNums = new TreeSet<Integer>();
            thisResNums.add(firstRes.getSequenceInteger());
            thisResNums.add(lastRes.getSequenceInteger());
            for (int i = 0; i < n; i ++)
            {
                int resNumInBtw = firstRes.getSequenceInteger() + i;
                thisResNums.add(resNumInBtw);
            }
            
            TreeSet<Integer> otherResNums = new TreeSet<Integer>();
            otherResNums.add(other.firstRes.getSequenceInteger());
            otherResNums.add(other.lastRes.getSequenceInteger());
            for (int i = 0; i < n; i ++)
            {
                int otherResNumInBtw = other.firstRes.getSequenceInteger() + i;
                otherResNums.add(otherResNumInBtw);
            }
            
            TreeSet<Integer> intersection = new TreeSet<Integer>();
            Iterator iter = thisResNums.iterator();
            while (iter.hasNext())
            {
                int num = (Integer) iter.next();
                if (otherResNums.contains(num)) // meaning it must be in both
                    intersection.add(num);
            }
            
            return intersection.size();
        }
        
        public String toString()
        {
            return n+"-turn from '"+firstRes+"' to '"+lastRes+"'";
        }
    }
//}}}

//{{{ CLASS: MinHelix
//##############################################################################
    public static class MinHelix
    {
        int n = 0;
        Residue firstRes = null, lastRes = null;
        
        public MinHelix(Residue first, Residue last, int length)
        {
            firstRes = first;
            lastRes  = last;
            n        = length;
        }
        
        public int overlap(MinHelix other)
        {
            TreeSet<Integer> thisResNums = new TreeSet<Integer>();
            thisResNums.add(firstRes.getSequenceInteger());
            thisResNums.add(lastRes.getSequenceInteger());
            for (int i = 0; i < n; i ++)
            {
                int resNumInBtw = firstRes.getSequenceInteger() + i;
                thisResNums.add(resNumInBtw);
            }
            
            TreeSet<Integer> otherResNums = new TreeSet<Integer>();
            otherResNums.add(other.firstRes.getSequenceInteger());
            otherResNums.add(other.lastRes.getSequenceInteger());
            for (int i = 0; i < n; i ++)
            {
                int otherResNumInBtw = other.firstRes.getSequenceInteger() + i;
                otherResNums.add(otherResNumInBtw);
            }
            
            TreeSet<Integer> intersection = new TreeSet<Integer>();
            Iterator iter = thisResNums.iterator();
            while (iter.hasNext())
            {
                int num = (Integer) iter.next();
                if (otherResNums.contains(num)) // meaning it must be in both
                    intersection.add(num);
            }
            
            return intersection.size();
        }
        
        public String toString()
        {
            return "MinHelix from '"+firstRes+"' to '"+lastRes+"'";
        }
    }
//}}}

//{{{ buildMinHelices
//##############################################################################
    /**
    * Make Kabsch & Sander's "minimal helices," i.e. two consecutive n-turns.
    * These MinHelix objects can overlap, e.g. NTurns 1-4 and 2-5 form MinHelix
    * 1-5 and NTurns 2-5 and 3-6 form MinHelix 2-6, so MinHelix 1-5 overlaps 
    * with MinHelix 2-6.
    */
    void buildMinHelices(Collection peptides, Model model)
    {
        if (verbose) System.out.println("Starting buildMinHelices...");
        
        // Start by storing n-turns (defined as Hbonded from CO of residue i to 
        // NH of residue i+n) in an ArrayList of NTurn objects
        nTurns = new ArrayList<NTurn>();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep != null)
            {
                if (verbose) System.out.println("Making n-turn(s) with '"+pep+"'...");
                if (pep.hbondO != null)
                {
                    Peptide pep2 = pep.hbondO;
                    Residue first = pep.cRes;  // cRes = "residue containing CO"
                    Residue last  = pep2.nRes; // nRes = "residue containing NH"
                    if (first != null && last != null)
                    {
                        int length = last.getSequenceInteger() - first.getSequenceInteger();
                        
                        if (length == 3 || length == 4 || length == 5)
                        {
                            NTurn nTurn = new NTurn(first, last, length);
                            if (verbose) System.out.println("Made  '"+nTurn+"' !");
                            nTurns.add(nTurn);
                            if (verbose) System.out.println("Added '"+nTurn+"' !");
                        }
                        else if (verbose)
                            System.out.println("'"+pep.cRes+"' HB's through its CO"
                                +" to a residue "+length+" away => not making an NTurn!");
                    }
                }
                else if (verbose)
                    System.out.println("No HB partner for '"+pep.cRes+"'"
                        +" through its CO => not making an NTurn!");
            }
        }
        
        // Next, make minimal helices (simply 2 consecutive n-turns)
        minHelices = new ArrayList<MinHelix>();
        ArrayList<NTurn> nTurnsForMinHelix = new ArrayList<NTurn>();
        for (int i = 0; i < nTurns.size(); i ++)
        {
            NTurn nTurn = nTurns.get(i);
            if (nTurnsForMinHelix.size() == 0)
            {
                nTurnsForMinHelix.add(nTurn);
            }
            else //if (nTurnsForMinHelix.size() >= 1)
            {
                NTurn prevNTurn = nTurnsForMinHelix.get(0);
                if (prevNTurn.firstRes.getNext(model).equals(nTurn.firstRes))
                {
                    // These n-turns are consecutive => make a MinHelix
                    // If n-turn1:1-4 and n-turn2:2-5, minhelix:2-4
                    Residue mhfr = nTurn.firstRes;
                    Residue mhlr = prevNTurn.lastRes;
                    int diff = mhlr.getSequenceInteger() - mhfr.getSequenceInteger();
                    MinHelix mh = new MinHelix(mhfr, mhlr, diff);
                    minHelices.add(mh);
                }
                
                // Whether we made a MinHelix or not, start a new AL<NTurn> for
                // the next putative MinHelix
                nTurnsForMinHelix = new ArrayList<NTurn>();
                nTurnsForMinHelix.add(nTurn); // b/c haven't used this nTurn yet
            }
        }
        
        if (verbose)
        {
            System.out.println("Minimal helices: ");
            for (MinHelix mh : minHelices)
                System.out.println("   "+mh);
            System.out.println();
        }
    }
//}}}

//{{{ buildHelices
//##############################################################################
    /**
    * Make Helix objects from MinHelix objects.
    * First, combine minimal helices into helices if they share at least one residue.
    * Finally, try to add additional residue(s) at the helix's N-terminus if we 
    * think they include the Ncap.
    */
    void buildHelices(Model model, ModelState state)
    {
        if (verbose) System.out.println("Staring buildHelices...");
        
        // Make Helix objects for sets of overlapping MinHelix objects
        helices = new ArrayList<Helix>();
        ArrayList<MinHelix> consecMinHelices = new ArrayList<MinHelix>();
        boolean doneConnecting = false;
        for (int i = 0; i < minHelices.size(); i ++)
        {
            MinHelix minHelix = minHelices.get(i);
            if (consecMinHelices.size() == 0)
            {
                if (verbose) System.out.println("Starting new consecMinHelices with '"+minHelix+"'");
                consecMinHelices.add(minHelix);
            }
            else
            {
                MinHelix lastMinHelix = consecMinHelices.get(consecMinHelices.size()-1);
                if (minHelix.overlap(lastMinHelix) >= 1)
                {
                    if (verbose) System.out.println("Adding '"+minHelix+"' to consecMinHelices");
                    consecMinHelices.add(minHelix);
                }
                else
                {
                    doneConnecting = true;
                }
            }
            if (i == minHelices.size()-1)    doneConnecting = true;
            
            if (doneConnecting)
            {
                // Completed set of overlapping min helices => make helix
                if (verbose) System.out.println("Completed AL consecMinHelices;"
                    +" size = "+consecMinHelices.size());
                TreeSet<Residue> resSet = new TreeSet<Residue>();
                for (MinHelix consecMinHelix : consecMinHelices)
                {
                    resSet.add(consecMinHelix.firstRes);
                    Residue resInBtw = consecMinHelix.firstRes;
                    for (int r = 0; r < consecMinHelix.n; r ++)
                    {
                        resInBtw = resInBtw.getNext(model); // keeps updating = good
                        resSet.add(resInBtw);
                    }
                    resSet.add(consecMinHelix.lastRes);
                }
                if (verbose)
                {
                    System.out.println("About to make new Helix from resSet:");
                    Iterator iter = resSet.iterator();
                    while (iter.hasNext())
                        System.out.println("   "+iter.next());
                }
                Helix helix = new Helix(resSet);
                helices.add(helix);
                if (verbose) System.out.println("Added '"+helix+"' for MinHelix set:"
                        +"\n   ("+consecMinHelices.get(0)+") to "
                        +"("+consecMinHelices.get(consecMinHelices.size()-1)+")");
                
                // Reset => start new set of overlapping MinHelix objects
                consecMinHelices = new ArrayList<MinHelix>();
                consecMinHelices.add(minHelix); // b/c haven't used this minHelix yet
                doneConnecting = false;
            }
        }
        
        if (verbose) System.out.println("After connecting MinHelices but before "
            +"adding putative Ncap residue, # helices = "+helices.size());
        
        // Also add putative Ncap residues based on i,i+3 distance < 5.9 A as in my
        // original HelixBuilder.
        // A convenient approach: add the preceding residue to each helix iff
        // Ca(i)-Ca(i+3) distance < 5.9 A (N-term)
        try
        {
            for (Helix helix : helices)
            {
                Residue first  = helix.getRes("first");
                Residue prev   = first.getPrev(model);
                Residue prev3  = first.getNext(model).getNext(model);
                if (prev != null && prev3 != null)
                {
                    AtomState caPrev  = state.get(prev.getAtom(" CA "));
                    AtomState caPrev3 = state.get(prev3.getAtom(" CA "));
                    if (caPrev != null && caPrev3 != null)
                    {
                        if (caPrev.distance(caPrev3) < 5.9)
                        {
                            // Add this residue b/c it's possibly the Ncap!
                            ArrayList<Residue> temp = new ArrayList<Residue>();
                            temp.add(prev); // new first residue
                            for (Residue res : helix.residues)
                                temp.add(res);
                            //Collections.sort(temp); // doesn't work for some reason...
                            helix.residues = temp; // refer to this new object
                            if (verbose) System.out.println("Added '"+prev+"' to '"
                                +helix+"' since Ca(i)-Ca(i+3) dist = "+caPrev.distance(caPrev3));
                        }
                    }
                }
            }
        }
        catch (AtomException ae)
        {
            System.err.println("Couldn't find one/both of res i-1 or i+2 for the "
                +"first residue of some helix...");
        }
    }
//}}}

//{{{ findAxes
//##############################################################################
    public void findAxes(Model model, ModelState state)
    {
        /** 
        * Calculates local helical axis for each set of 4 Ca's in each Helix. 
        * This is only one way of doing it... Can also Google some other
        * possibilities later.
        * This will be used to tabulate the angle between the local helix axis
        * and the normal vectors of certain peptides in the helix (e.g. Ncap, 
        * Asn's w/in helix) to look for evidence of backrubs in a local
        * coordinate system.
        * The point is to examine under what local circumstances the backrub
        * occurs.
        */
        for (Helix helix : helices)
        {
            ArrayList<Triple> thisHelixAxisHeads = new ArrayList<Triple>();
            ArrayList<Triple> thisHelixAxisTails = new ArrayList<Triple>();
            
            for (int r = 0; r < helix.residues.size()-3; r ++) // should be a sorted AL
            {
                // Take line from Ca_i   to Ca_i+2
                //  and line from Ca_i+1 to Ca_i+3
                // Line between the midpoints of those two lines points in 
                // the direction of a local axis vector.
                
                // Find the requisite 4 Ca's
                Residue[] res = new Residue[4];
                res[0] = helix.residues.get(r);
                for (int i = 0; i < 3; i ++)
                    if (res[i].getNext(model) != null)
                        res[i+1] = res[i].getNext(model);
                AtomState[] cas = new AtomState[4];
                for (int i = 0; i < 4; i ++)
                    if (res[i] != null)
                    {
                        try
                        {
                            cas[i] = state.get(res[i].getAtom(" CA "));
                        }
                        catch ( driftwood.moldb2.AtomException ae)
                        {
                            System.err.println("Can't find CA in res "+res[i]);
                        }
                    }
                int numValidCas = 0;
                for (int i = 0; i < cas.length; i ++)
                    if (cas[i] != null)
                        numValidCas ++;
                
                if (numValidCas == 4)
                {
                    // Calculate the axis
                    Triple midpoint0to2 = new Triple().likeMidpoint(
                        cas[0], cas[2]);
                    Triple midpoint1to3 = new Triple().likeMidpoint(
                        cas[1], cas[3]);
                    Triple axisDir = new Triple().likeVector(
                        midpoint0to2, midpoint1to3);
                    Triple axisHead = axisDir.unit().mult(2).add(midpoint0to2);
                    Triple axisTail = midpoint0to2; // can change to midpoint
                                                    // of midpoints later...
                    // Add the axis to this helix
                    thisHelixAxisHeads.add(axisHead);
                    thisHelixAxisTails.add(axisTail);
                }
                else
                {
                    System.err.println("Wrong number Cas for this helical axis!");
                    System.err.println("Expected 4 but got "+numValidCas+"...");
                }
            }
            
            // Finish by giving a list of axes to this helix
            helix.axisTails = thisHelixAxisTails;
            helix.axisHeads = thisHelixAxisHeads;
            
            if (verbose)
            {
                System.out.println(helix.axisTails.size()+" axis tails and "+
                    helix.axisHeads.size()+" axis heads in "+helix);
                System.out.println("Residues:");
                for (int r = 0; r < helix.residues.size(); r ++)
                    System.out.println("  "+helix.residues.get(r));
                System.out.println("First residue: "+helix.getRes("first"));
                System.out.println("Last  residue: "+helix.getRes("last"));
                System.out.println();
            }
            
        }//for(each Helix in helices)
    }
//}}}

//{{{ smoothAxes
//##############################################################################
    public void smoothAxes()
    {
        /** 
        * Smooths the local helical axes for each set of 4 Ca's in each Helix 
        * made in findAxes by averaging the direction of each with that of its
        * previous and next neighbors (if they exist, i.e. we aren't on the end
        * of the helix).
        * Goal: Generate a direction that is robust to weirdnesses in the local
        * geometry (e.g. helix turning into beta or something) but does reflect
        * subtle local changes in helical direction.
        */
        for (Helix helix : helices)
        {
            for (int i = 0; i < helix.axisHeads.size(); i ++)
            {
                // Starting axis tail & head
                Triple tail = helix.axisTails.get(i);
                Triple head = helix.axisHeads.get(i);
                
                // Get adjacent residues' 4 Ca local axis heads and tails
                // if applicable
                ArrayList<Triple> adjacTails = new ArrayList<Triple>();
                ArrayList<Triple> adjacHeads = new ArrayList<Triple>();
                adjacTails.add(new Triple(tail)); // put in same AL b/c we'll
                adjacHeads.add(new Triple(head)); // avg all its contents later
                if (i > 0)
                {
                    adjacTails.add(new Triple(helix.axisTails.get(i-1)));
                    adjacHeads.add(new Triple(helix.axisHeads.get(i-1)));
                }
                if (i < helix.axisHeads.size()-1)
                {
                    adjacTails.add(new Triple(helix.axisTails.get(i+1)));
                    adjacHeads.add(new Triple(helix.axisHeads.get(i+1)));
                }
                
                // Translate to origin and average orientation with 0, 1, or 2
                // adjacent axes, depending on if they exist
                Triple newHead = new Triple();
                double x = 0, y = 0, z = 0;
                if (adjacTails.size() > 1 && adjacHeads.size() > 1)
                {
                    for (int a = 0; a < adjacHeads.size(); a ++)
                    {
                        Triple adjacTail = adjacTails.get(a);
                        Triple adjacHead = adjacHeads.get(a);
                        Triple adjacHeadAtOrig = new Triple(
                            adjacHead.getX()-adjacTail.getX(),
                            adjacHead.getY()-adjacTail.getY(),
                            adjacHead.getZ()-adjacTail.getZ());
                        x += adjacHeadAtOrig.getX();
                        y += adjacHeadAtOrig.getY();
                        z += adjacHeadAtOrig.getZ();
                    }
                    // Average orientation
                    newHead = new Triple(x/adjacHeads.size(), 
                        y/adjacHeads.size(), z/adjacHeads.size());
                    
                    // Translate back to the helix
                    newHead.add(tail);
                    
                }
                // else if no adjacent residues in helix with axes, do nothing 
                // to this local axis
                
                // Leave axisTails.get(i) alone, but update axisHeads.get(i)
                helix.axisHeads.set(i, newHead);
            }
            
        }//for(each Helix in helices)
    }
//}}}



//{{{ findNcaps
//##############################################################################
    public void findNcaps(Model model, ModelState state)
    /** 
    * We just assume the first residue in a helix is the Ncap.
    * This is a *very simple* Ncap-finding algorithm!
    *
    * Can alter later to incorporate Ca position relative to cylinder of
    * helix as in original RLab helix cap paper
    */
    {
        for (Helix helix : helices)
        {
            // Find the Ncaps
            if (helix.ncap != null) continue;
            else
            {
                if (verbose)
                    System.out.println("Calling typeOfNcapHbond for '"+helix+"' in '"+filename+"'");
                String hb = typeOfNcapHbond(helix.getRes("first"), model, state);
                
                if (onlyHbNcaps && !hb.equals("i+2") && !hb.equals("i+3"))
                {
                    helix.ncap = null;
                }
                else
                {
                    helix.ncap = new Ncap(helix.getRes("first"));
                    if (hb.equals("i+2"))       helix.ncap.hb_i2 = true;
                    if (hb.equals("i+3"))       helix.ncap.hb_i3 = true;
                }
            }
        }
        
        for (Helix helix : helices)
        {
            if (helix.ncap != null)
            {
                // Calculate "backrub-like" distances & angles for each Ncap
                if (onlyHbNcaps)    
                    setNcapDistances(helix, model, state);
                setNcapAngles(helix, model, state);
                
                // Set phi, psi for Ncap i, i+1, and i-1 residues
                setNcapPhiPsis(helix, model, state);
                
                // Set "sc length" for Ncap & N3
                setNcapScLengths(helix, model);
                
                // See if there's a capping box
                setCappingBox(helix, model, state);
            }
        }
    }
//}}}

//{{{ typeOfNcapHbond
//##############################################################################
    public String typeOfNcapHbond(Residue res, Model model, ModelState state)
    /** 
    * This is a simple geometric routine to determine whether a putative Ncap
    * residue is a Ser/Thr/Asn/Asp and makes an i+2 or i+3 Hbond or not.
    * Uses cutoff of 2.9A to determine whether an Hbond is there or not, which
    * is something we could obviously alter later, but it seems reasonable.
    */
    {
        try
        {
            Residue res2 = res.getNext(model).getNext(model);
            Residue res3 = res2.getNext(model);
            Atom h2 = res2.getAtom(" H  "); // doesn't exist for prolines! (" HA ")
            Atom h3 = res3.getAtom(" H  ");
            Atom n2 = res2.getAtom(" N  ");
            Atom n3 = res3.getAtom(" N  ");
            Triple likeH2 = null;
            Triple likeH3 = null;
            Triple likeN2 = null;
            Triple likeN3 = null;
            if (h2 != null)     likeH2 = new Triple(state.get(h2));
            if (h3 != null)     likeH3 = new Triple(state.get(h3));
            if (n2 != null)     likeN2 = new Triple(state.get(n2));
            if (n3 != null)     likeN3 = new Triple(state.get(n3));
            
            Triple likeO0   = null;
            Triple likeO0_2 = null;
            Triple likeC0   = null;
            if (res.getName().equals("ASN"))
            {
                Atom o0 = res.getAtom(" OD1");
                Atom c0 = res.getAtom(" CG ");
                if (o0 != null)     likeO0 = new Triple(state.get(o0));
                if (c0 != null)     likeC0 = new Triple(state.get(c0));
            }
            if (res.getName().equals("ASP"))
            {
                Atom o0   = res.getAtom(" OD1");
                Atom o0_2 = res.getAtom(" OD2");
                Atom c0   = res.getAtom(" CG ");
                if (o0 != null)     likeO0   = new Triple(state.get(o0));
                if (o0_2 != null)   likeO0_2 = new Triple(state.get(o0_2));
                if (c0 != null)     likeC0   = new Triple(state.get(c0));
            }
            if (res.getName().equals("SER"))
            {
                Atom o0 = res.getAtom(" OG ");
                Atom c0 = res.getAtom(" CB ");
                if (o0 != null)     likeO0 = new Triple(state.get(o0));
                if (c0 != null)     likeC0 = new Triple(state.get(c0));
            }
            if (res.getName().equals("THR"))
            {
                Atom o0 = res.getAtom(" OG1");
                Atom c0 = res.getAtom(" CB ");
                if (o0 != null)     likeO0 = new Triple(state.get(o0));
                if (c0 != null)     likeC0 = new Triple(state.get(c0));
            }
            
            // Accept Hbond according to Kabsch & Sander criterion:
            // E = 0.42*0.20*332*(1/rON + 1/rCH - 1/rOH - 1/rCN) 
            // is less than -0.5 kcal/mol.
            // The difference is we're now dealing with a sc-mc 
            // Hbond, but it should work about as well (?).
            if (likeH2 != null && likeH3 != null && 
                likeN2 != null && likeN3 != null && 
                likeO0 != null && likeC0 != null)
            {
                double rON = likeO0.distance(likeN3);
                double rCH = likeC0.distance(likeH3);
                double rOH = likeO0.distance(likeH3);
                double rCN = likeC0.distance(likeN3);
                double energy3 = 27.9*(1/rON + 1/rCH - 1/rOH - 1/rCN);
                
                rON = likeO0.distance(likeN2);
                rCH = likeC0.distance(likeH2);
                rOH = likeO0.distance(likeH2);
                rCN = likeC0.distance(likeN2);
                double energy2 = 27.9*(1/rON + 1/rCH - 1/rOH - 1/rCN);
                if (verbose) System.out.println("Ncap energy3 = "+energy3);
                if (verbose) System.out.println("Ncap energy2 = "+energy2);
                if (energy2 < energy3 && energy2 < -0.5)
                {
                    if (verbose) System.out.println("Ncap "+res+" makes an i+2"
                        +" Hb with energy "+energy2);
                    return "i+2";
                }
                if (energy3 < energy2 && energy3 < -0.5)
                {
                    if (verbose) System.out.println("Ncap "+res+" makes an i+3"
                        +" Hb with energy "+energy3);
                    return "i+3";
                }
            }
            if (verbose) System.out.println("Res '"+res+"' isn't an "+
                "NDST that makes an Hb...");
            return "";
        }
        catch (AtomException ae)
        {
            // If e.g. there are disordered sidechain atoms
            System.err.println("Problem figuring out if "+res+" in '"+filename
                +"' is an Asn/Asp/Ser/Thr whose sc Hbonds to i+2 or i+3 mc and"
                +" is therefore an Ncap...");
            return "";
        }
    }
    
//}}}

//{{{ setCappingBox
//##############################################################################
    public void setCappingBox(Helix helix, Model model, ModelState state)
    /** 
    * This is a simple geometric routine akin to typeOfNcapHbond.
    * For a given valid N cap, if residue N3 exists, it documents what aa type
    * it is and whether or not it's a Gln/Glu that makes a "capping box" 
    * hydrogen bond.
    * These two values are stored as fields in the Ncap class.
    */
    {
        try
        {
            Residue res0 = helix.ncap.res;
            if (res0.getNext(model) != null)
            {
                if (verbose) System.out.println("res1 exists!");
                Residue res1 = res0.getNext(model);
                if (res1.getNext(model) != null)
                {
                    if (verbose) System.out.println("res2 exists!");
                    Residue res2 = res1.getNext(model);
                    if (res2.getNext(model) != null)
                    {
                        // Set res3
                        if (verbose) System.out.println("res3 exists!");
                        Residue res3 = res2.getNext(model);
                        helix.ncap.res3 = res3;
                        
                        // Get potentially Hbonded capping box sc3 & mc0 coordinates
                        Atom h0 = res0.getAtom(" H  ");
                        Atom n0 = res0.getAtom(" N  ");
                        Triple likeH0  = null;
                        Triple likeN0 = null;
                        if (h0 != null)     likeH0 = new Triple(state.get(h0));
                        if (n0 != null)     likeN0 = new Triple(state.get(n0));
                        
                        Triple likeO3   = null;
                        Triple likeO3_2 = null;
                        Triple likeC3   = null;
                        if (res3.getName().equals("GLN"))
                        {
                            Atom o3 = res3.getAtom(" OE1");
                            Atom c3 = res3.getAtom(" CD ");
                            if (o3 != null)     likeO3 = new Triple(state.get(o3));
                            if (c3 != null)     likeC3 = new Triple(state.get(c3));
                        }
                        if (res3.getName().equals("GLU"))
                        {
                            Atom o3   = res3.getAtom(" OE1");
                            Atom o3_2 = res3.getAtom(" OE2");
                            Atom c3   = res3.getAtom(" CD ");
                            if (o3 != null)     likeO3   = new Triple(state.get(o3));
                            if (o3_2 != null)   likeO3_2 = new Triple(state.get(o3_2));
                            if (c3 != null)     likeC3 = new Triple(state.get(c3));
                        }
                        if (res3.getName().equals("ASN"))
                        {
                            Atom o3 = res3.getAtom(" OD1");
                            Atom c3 = res3.getAtom(" CG ");
                            if (o3 != null)     likeO3 = new Triple(state.get(o3));
                            if (c3 != null)     likeC3 = new Triple(state.get(c3));
                        }
                        if (res3.getName().equals("ASP"))
                        {
                            Atom o3   = res3.getAtom(" OD1");
                            Atom o3_2 = res3.getAtom(" OD2");
                            Atom c3   = res3.getAtom(" CG ");
                            if (o3 != null)     likeO3   = new Triple(state.get(o3));
                            if (o3_2 != null)   likeO3_2 = new Triple(state.get(o3_2));
                            if (c3 != null)     likeC3 = new Triple(state.get(c3));
                        }
                        if (res3.getName().equals("SER"))
                        {
                            Atom o3 = res3.getAtom(" OG ");
                            Atom c3 = res3.getAtom(" CB ");
                            if (o3 != null)     likeO3 = new Triple(state.get(o3));
                            if (c3 != null)     likeC3 = new Triple(state.get(c3));
                        }
                        if (res3.getName().equals("THR"))
                        {
                            Atom o3 = res3.getAtom(" OG1");
                            Atom c3 = res3.getAtom(" CB ");
                            if (o3 != null)     likeO3 = new Triple(state.get(o3));
                            if (c3 != null)     likeC3 = new Triple(state.get(c3));
                        }
                        
                        // Accept Hbond according to Kabsch & Sander criterion:
                        // E = 0.42*0.20*332*(1/rON + 1/rCH - 1/rOH - 1/rCN) 
                        // is less than -0.5 kcal/mol.
                        // The difference is we're now dealing with a sc-mc 
                        // Hbond, but it should work about as well (?).
                        if (likeH0 != null && likeN0 != null && 
                            likeO3 != null && likeC3 != null)
                        {
                            double rON = likeO3.distance(likeN0);
                            double rCH = likeC3.distance(likeH0);
                            double rOH = likeO3.distance(likeH0);
                            double rCN = likeC3.distance(likeN0);
                            double energy = 27.9*(1/rON + 1/rCH - 1/rOH - 1/rCN);
                            if (likeO3_2 != null)
                            {
                                double rON_2 = likeO3_2.distance(likeN0);
                                double rOH_2 = likeO3_2.distance(likeH0);
                                double energy2 = 27.9*(1/rON_2 + 1/rCH - 1/rOH_2 - 1/rCN);
                                if (energy2 < energy)       energy = energy2;
                            }
                            if (energy < -0.5)
                            {
                                helix.ncap.cappingBoxResType = res3.getName();
                                if (verbose) System.out.println(res3+" makes capping "
                                    +"box Hb to "+res0+"\t energy="+energy);
                            }
                        }
                        else // no capping box for this helix
                            if (verbose) System.out.println("No Glu/Gln/Asn/Asp/"+
                                "Ser/Thr capping box for "+helix);
                    }
                }
            }
        }
        catch (AtomException ae)
        {
            System.err.println("Trouble looking for capping box in '"+helix+"'");
        }
    }
//}}}

//{{{ setNcapDistances
//##############################################################################
    public void setNcapDistances(Helix helix, Model model, ModelState state)
    {
        try
        {
            // First, get AtomStates to use for distance calculations
            Residue res = helix.ncap.res;
            Residue res2 = res.getNext(model).getNext(model);
            Residue res3 = res2.getNext(model);
            Residue resminus1 = res.getPrev(model);
            
            Triple likeH2 = null;
            Triple likeH3 = null;
            if (res2.getAtom(" H  ") != null)
                likeH2 = new Triple(state.get(res2.getAtom(" H  ")));
            if (res3.getAtom(" H  ") != null)
                likeH3 = new Triple(state.get(res3.getAtom(" H  ")));
            
            Triple scAtom = null;
            Triple scAtom2 = null; // for ASP b/c two poss HB'ing atoms
            if (res.getName().equals("SER"))
                scAtom = new Triple(state.get(res.getAtom(" OG ")));
            if (res.getName().equals("THR"))
                scAtom = new Triple(state.get(res.getAtom(" OG1")));
            if (res.getName().equals("ASN"))
                scAtom = new Triple(state.get(res.getAtom(" OD1")));
            if (res.getName().equals("ASP"))
            {
                scAtom  = new Triple(state.get(res.getAtom(" OD1")));
                scAtom2 = new Triple(state.get(res.getAtom(" OD2")));
            }
            
            Triple likeNcapCa   = null;
            Triple likeN3Ca     = null;
            Triple likeNprimeCa = null;
            if (res != null)
                if (res.getAtom(" CA ") != null)
                    likeNcapCa   = new Triple(state.get(res.getAtom(" CA ")));
            if (res3 != null)
                if (res3.getAtom(" CA ") != null)
                    likeN3Ca     = new Triple(state.get(res3.getAtom(" CA ")));
            if (resminus1 != null)
                if (resminus1.getAtom(" CA ") != null)
                    likeNprimeCa = new Triple(state.get(resminus1.getAtom(" CA ")));
            
            if (verbose)
            {
                System.out.println("likeNcapCa: '"+likeNcapCa+"'");
                System.out.println("likeN3Ca: '"+likeN3Ca+"'");
                System.out.println("likeNprimeCa: '"+likeNprimeCa+"'");
            }
            
            double dist = Double.NaN;
            // Set distNcapScToN2H
            if (likeH2 != null)
            {
                dist = Triple.distance(scAtom, likeH2);
                if (scAtom2 != null)
                {
                    double altDist = Triple.distance(scAtom2, likeH2);
                    if (altDist < dist)  dist = altDist;
                }
                helix.ncap.distNcapScToN2H = dist;
            }
            
            // Set distNcapScToN3H
            if (likeH3 != null)
            {
                dist = Triple.distance(scAtom, likeH3);
                if (scAtom2 != null)
                {
                    double altDist = Triple.distance(scAtom2, likeH3);
                    if (altDist < dist)  dist = altDist;
                }
                helix.ncap.distNcapScToN3H = dist;
            }
            
            // Set distNcapCaToN3Ca
            if (likeNcapCa != null && likeN3Ca != null)
            {
                dist = Triple.distance(likeNcapCa, likeN3Ca);
                helix.ncap.distNcapCaToN3Ca = dist;
            }
            
            // Set distNprimeCaToN3Ca
            if (likeNcapCa != null && likeNprimeCa != null)
            {
                dist = Triple.distance(likeNcapCa, likeNprimeCa);
                helix.ncap.distNprimeCaToN3Ca = dist;
            }
        }
        catch (driftwood.moldb2.AtomException ae)
        {
            System.err.println("Problem calculating Ncap distances...");
        }
    }
//}}}

//{{{ setNcapAngles
//##############################################################################
    public void setNcapAngles(Helix helix, Model model, ModelState state)
    {
        try
        {
            // One option is angle between the local helix axis for the Ncap residue
            // and the normal to the plane formed by Ca(i,i-1,i+1).
            Residue res = helix.ncap.res;
            AtomState ca = state.get(res.getAtom(" CA "));
            Triple tail = helix.axisTails.get(0);
            Triple head = helix.axisHeads.get(0);
            Triple axisAtOrigin = new Triple(head.getX()-tail.getX(),
                head.getY()-tail.getY(), head.getZ()-tail.getZ() );
            if (res.getPrev(model) != null && res.getNext(model) != null)
            {
                // this angle defined
                AtomState prevCa = state.get(res.getPrev(model).getAtom(" CA "));
                AtomState nextCa = state.get(res.getNext(model).getAtom(" CA "));
                
                Triple normal = new Triple().likeNormal(prevCa, ca, nextCa); 
                
                helix.ncap.normalTail = ca;
                helix.ncap.normalHead = new Triple(ca.getX()+normal.getX(), 
                    ca.getY()+normal.getY(), ca.getZ()+normal.getZ());
                
                // OK to mess with normal directly now
                helix.ncap.planeNormalAngle = normal.angle(axisAtOrigin);
            }
            
            // A second option is the angle between the Ncap Ca_Cb vector 
            // and the local helix axis
            Triple likeCa = new Triple(ca); // same coords as ca above but different object
            if (!res.getName().equals("GLY"))
            {
                Triple likeCb = new Triple(state.get(res.getAtom(" CB ")));
                Triple caCbAtOrigin = new Triple().likeVector(likeCa, likeCb);
                helix.ncap.caCbAngle = caCbAtOrigin.angle(axisAtOrigin);
            }
            // else (default in Ncap constructor: Double.NaN)
            
            // Another two measures involve Ca's so we'll do them together.
            // (1) The first is another option to describe the backrub state 
            //     that ignores my artificial local helix axes and defines
            //     the backrub as the angle between the Ca(i-1,i+1,i+2) and
            //     Ca(i-1,i,i+1) planes: first = reference, second = backrub.
            // (2) The second is the angle btw the virtual bonds Ca(i-1,i+1) 
            //     and Ca(i+1,i+2), which was visually observed to potentially be
            //     different for short Ser/Thr vs. long Asn/Asp Ncaps.
            if (res.getPrev(model) != null && res.getNext(model) != null)
            {
                Residue resNext = res.getNext(model);
                Residue resPrev = res.getPrev(model);
                if (resNext.getNext(model) != null)
                {
                    Residue resNext2 = resNext.getNext(model);
                    
                    Triple likeCaPrev  = new Triple(state.get(resPrev.getAtom(" CA ")));
                           likeCa      = new Triple(state.get(res.getAtom(" CA ")));
                    Triple likeCaNext  = new Triple(state.get(resNext.getAtom(" CA ")));
                    Triple likeCaNext2 = new Triple(state.get(resNext2.getAtom(" CA ")));
                    
                    // (1) caPlanesAngle
                    Triple norm1 = new Triple().likeNormal(likeCaPrev, likeCa, likeCaNext);
                    Triple norm2 = new Triple().likeNormal(likeCaPrev, likeCaNext, likeCaNext2);
                    helix.ncap.caPlanesAngle = norm1.angle(norm2);
                    
                    // (2) caEntryAngle
                    helix.ncap.caEntryAngle = new Triple().angle(
                        likeCaPrev, likeCaNext, likeCaNext2);
                }
            }
            
            // Another set of measures is the tau angles (N-Ca-C) for i, i-1, i+1.
            // They may be strained if a backrub occurs
            Triple likeN =  new Triple(state.get(res.getAtom(" N  ")));
                   likeCa = new Triple(state.get(res.getAtom(" CA ")));
            Triple likeC =  new Triple(state.get(res.getAtom(" C  ")));
            helix.ncap.tau = Triple.angle(likeN, likeCa, likeC);
            if (res.getPrev(model) != null)
            {
                likeN  = new Triple(state.get(res.getPrev(model).getAtom(" N  ")));
                likeCa = new Triple(state.get(res.getPrev(model).getAtom(" CA ")));
                likeC  = new Triple(state.get(res.getPrev(model).getAtom(" C  ")));
                helix.ncap.nprimeTau = Triple.angle(likeN, likeCa, likeC);
            }
            if (res.getNext(model) != null)
            {
                likeN  = new Triple(state.get(res.getNext(model).getAtom(" N  ")));
                likeCa = new Triple(state.get(res.getNext(model).getAtom(" CA ")));
                likeC  = new Triple(state.get(res.getNext(model).getAtom(" C  ")));
                helix.ncap.n1Tau = Triple.angle(likeN, likeCa, likeC);
            }
        }
        catch (driftwood.moldb2.AtomException ae)
        {
            System.err.println("Problem calculating Ncap angles...");
        }
    }
//}}}

//{{{ setNcapPhiPsis
//##############################################################################
    public void setNcapPhiPsis(Helix helix, Model model, ModelState state)
    {
        try
        {
            // Phi, psi for Ncap residue
            Triple likeCa = new Triple(state.get(helix.ncap.res.getAtom(" CA ")));
            Triple likeN = new Triple(state.get(helix.ncap.res.getAtom(" N  ")));
            Triple likeC = new Triple(state.get(helix.ncap.res.getAtom(" C  ")));
            if (helix.ncap.res.getPrev(model) != null) // phi defined
            {
                Triple likePrevC = new Triple(state.get(helix.ncap.res.getPrev(model).getAtom(" C  ")));
                helix.ncap.phi = Triple.dihedral(likePrevC, likeN, likeCa, likeC);
            }
            if (helix.ncap.res.getNext(model) != null) // psi defined
            {
                Triple likeNextN = new Triple(state.get(helix.ncap.res.getNext(model).getAtom(" N  ")));
                helix.ncap.psi = Triple.dihedral(likeN, likeCa, likeC, likeNextN);
            }
            
            // Phi, psi for Ncap i+1 residue ("N1")
            if (helix.ncap.res.getNext(model) != null)
            {
                Residue n1 = helix.ncap.res.getNext(model);
                Triple likeN1Ca = new Triple(state.get(n1.getAtom(" CA ")));
                Triple likeN1N = new Triple(state.get(n1.getAtom(" N  ")));
                Triple likeN1C = new Triple(state.get(n1.getAtom(" C  ")));
                helix.ncap.n1Phi = Triple.dihedral(likeC, likeN1N, likeN1Ca, likeN1C);
                if (n1.getNext(model) != null) // psi defined
                {
                    Triple likeN2N = new Triple(state.get(n1.getNext(model).getAtom(" N  ")));
                    helix.ncap.n1Psi = Triple.dihedral(likeN1N, likeN1Ca, likeN1C, likeN2N);
                }
            }
            
            // Phi, psi for Ncap i-1 residue ("N'")
            if (helix.ncap.res.getPrev(model) != null)
            {
                Residue nprime = helix.ncap.res.getPrev(model);
                Triple likeNprimeCa = new Triple(state.get(nprime.getAtom(" CA ")));
                Triple likeNprimeN = new Triple(state.get(nprime.getAtom(" N  ")));
                Triple likeNprimeC = new Triple(state.get(nprime.getAtom(" C  ")));
                if (nprime.getPrev(model) != null) // phi defined
                {
                    Triple likendoubleprimeC = new Triple(state.get(
                        nprime.getPrev(model).getAtom(" C  ")));
                    helix.ncap.nprimePhi = Triple.dihedral(likendoubleprimeC, likeNprimeN, 
                        likeNprimeCa, likeNprimeC);
                }
                helix.ncap.nprimePsi = Triple.dihedral(likeNprimeN, likeNprimeCa, likeNprimeC, likeN);
            }
        }
        catch (driftwood.moldb2.AtomException ae)
        {
            System.err.println("Problem calculating ncap i, i-1, and i+1 phi & psi...");
        }
    }
//}}}

//{{{ setNcapScLengths
//##############################################################################
    public void setNcapScLengths(Helix helix, Model model)
    {
        TreeSet<String> zeroChis = new TreeSet<String>();
        zeroChis.add("GLY");   zeroChis.add("ALA");
        
        TreeSet<String> oneChi = new TreeSet<String>();
        oneChi.add("CYS");     oneChi.add("SER");     oneChi.add("VAL");
        oneChi.add("THR");     oneChi.add("PRO");     oneChi.add("PHE");
        oneChi.add("TYR");
        
        TreeSet<String> twoChis = new TreeSet<String>();
        twoChis.add("TRP");    twoChis.add("HIS");    twoChis.add("LEU");
        twoChis.add("ILE");    twoChis.add("ASP");    twoChis.add("ASN");
        
        TreeSet<String> threeChis = new TreeSet<String>();
        threeChis.add("GLU");  threeChis.add("GLN");  threeChis.add("MET");
        
        TreeSet<String> fourChis = new TreeSet<String>();
        fourChis.add("LYS");   fourChis.add("ARG");
        
        Residue n3 = helix.ncap.res.getNext(model).getNext(model).getNext(model);
        String n3ResType   = n3.getName();            
        String ncapResType = helix.ncap.res.getName();
        if (verbose) 
            System.out.println("ncapResType: '"+ncapResType+"'...\nn3ResType: '"+n3ResType+"'...");
        
        // Ncap
        if (helix.ncap != null)
        {
            if (zeroChis.contains(ncapResType))   helix.ncap.ncapNumChis = 0; // shouldn't happen
            if (oneChi.contains(ncapResType))     helix.ncap.ncapNumChis = 1;
            if (twoChis.contains(ncapResType))    helix.ncap.ncapNumChis = 2;
            if (threeChis.contains(ncapResType))  helix.ncap.ncapNumChis = 3; // shouldn't happen
            if (fourChis.contains(ncapResType))   helix.ncap.ncapNumChis = 4; // shouldn't happen
        }
        
        // N3
        if (n3 != null)
        {
            if (zeroChis.contains(n3ResType))     helix.ncap.n3NumChis = 0;
            if (oneChi.contains(n3ResType))       helix.ncap.n3NumChis = 1;
            if (twoChis.contains(n3ResType))      helix.ncap.n3NumChis = 2;
            if (threeChis.contains(n3ResType))    helix.ncap.n3NumChis = 3;
            if (fourChis.contains(n3ResType))     helix.ncap.n3NumChis = 4;
        }
    }
//}}}



//{{{ sketchHbonds
//##############################################################################
    void sketchHbonds(PrintStream out, Collection peptides, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {peptides & hbonds}");
        out.println("@balllist {peptides} radius= 0.1 color= green");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.isHelix) //Beta)
                out.println("{"+pep+"} r=0.3 "+pep.midpoint.format(df));
            else
                out.println("{"+pep+"} "+pep.midpoint.format(df));
        }
        
        out.println("@vectorlist {N hbonds} color= sky");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.hbondN != null)
            {
                try
                {
                    AtomState h = state.get(pep.nRes.getAtom(" H  "));
                    AtomState o = state.get(pep.hbondN.cRes.getAtom(" O  "));
                    out.println("{"+pep+"}P "+h.format(df));
                    out.println("{"+pep.hbondN+"} "+o.format(df));
                }
                catch(AtomException ex) {}
            }
        }
        
        out.println("@vectorlist {O hbonds} color= red");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.hbondO != null)
            {
                try
                {
                    AtomState o = state.get(pep.cRes.getAtom(" O  "));
                    AtomState h = state.get(pep.hbondO.nRes.getAtom(" H  "));
                    out.println("{"+pep+"}P "+o.format(df));
                    out.println("{"+pep.hbondO+"} "+h.format(df));
                }
                catch(AtomException ex) {}
            }
        }
    }
//}}}

//{{{ sketchNcaps
//##############################################################################
    void sketchNcaps(PrintStream out, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {ncaps}");
        out.println("@balllist {ncaps} radius= 0.3 color= hotpink");
        for (Helix helix : helices)
        {
            try
            {
                if (helix.ncap != null)
                {
                    AtomState ncapCa = state.get(helix.ncap.res.getAtom(" CA "));
                    out.println("{helix '"+helix.toString()+"' ncap} "+
                        df.format(ncapCa.getX())+" "+
                        df.format(ncapCa.getY())+" "+
                        df.format(ncapCa.getZ()) );
                }
            }
            catch (driftwood.moldb2.AtomException ae)
            {
                System.err.println("Can't find atom ' CA ' in helix "+helix);
            }
        }
        
        out.println("@vectorlist {ncap normals} color= hotpink");
        for (Helix helix : helices)
        {
            if (helix.ncap != null && 
                helix.ncap.normalTail != null && helix.ncap.normalHead != null)
            {
                out.println("{helix '"+helix.toString()+"' ncap normal tail}P "+
                    df.format(helix.ncap.normalTail.getX())+" "+
                    df.format(helix.ncap.normalTail.getY())+" "+
                    df.format(helix.ncap.normalTail.getZ()) );
                out.println("{helix '"+helix.toString()+"' ncap normal head}P "+
                    df.format(helix.ncap.normalHead.getX())+" "+
                    df.format(helix.ncap.normalHead.getY())+" "+
                    df.format(helix.ncap.normalHead.getZ()) );
                
            }
        }
    }
//}}}

//{{{ sketchAxes
//##############################################################################
    void sketchAxes(PrintStream out)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        String title = ""; // default
        if (smoothAxes)         title = "smoothed ";
        else if (vectorSumAxis) title = "vector sum ";
        out.println("@group {"+title+"helix axes}");
        out.println("@vectorlist {"+title+"helix axes} color= peach");
        if (vectorSumAxis)
        {
            for (Helix helix : helices)
            {
                if (helix.vectorSumAxisTail != null && 
                    helix.vectorSumAxisHead != null)
                {
                    out.println("{helix ("+helix.toString()+") vector sum axis "+
                        "tail}P "+helix.vectorSumAxisTail.getX()+" "+
                        helix.vectorSumAxisTail.getY()+" "+
                        helix.vectorSumAxisTail.getZ());
                    out.println("{helix ("+helix.toString()+") vector sum axis "+
                        "head} "+helix.vectorSumAxisHead.getX()+" "+
                        helix.vectorSumAxisHead.getY()+" "+
                        helix.vectorSumAxisHead.getZ());
                }
            }
        }
        else
        {
            for (Helix helix : helices)
            {
                if (helix.axisHeads != null && helix.axisTails != null)
                {
                    for (int i = 0; i < helix.axisHeads.size(); i++)
                    {
                        // There should be the same number of  entries in 
                        // axisHeads and axisTails, so this should be OK
                        // to do                        
                        out.println("{helix ("+helix.toString()+")"+
                            " res"+((int)i+1)+"-"+((int)i+2)+" "+title+
                            "axis tail}P "+
                        df.format(helix.axisTails.get(i).getX())+" "+
                        df.format(helix.axisTails.get(i).getY())+" "+
                        df.format(helix.axisTails.get(i).getZ()) );
                        
                        out.println("{helix ("+helix.toString()+")"+
                            " res"+((int)i+1)+"-"+((int)i+2)+" "+title+
                            "axis head} "+
                        df.format(helix.axisHeads.get(i).getX())+" "+
                        df.format(helix.axisHeads.get(i).getY())+" "+
                        df.format(helix.axisHeads.get(i).getZ()) );
                    }
                }
            }
        }
    }
//}}}

//{{{ printHelicalPeptides
//##############################################################################
    void printHelicalPeptides(PrintStream out, Collection peptides)
    {
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.isHelix) //Beta)
                out.println(filename.substring(0,4)+" "+pep); 
        }
    }
//}}}



//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        // Make helices
        doFile();
        
        // Print output
        if (doPrint)
        {
            if (verbose) printHelices();
            printNcapAngles();
        }
    }
    
    public static void main(String[] args)
    {
        DsspHelixBuilder mainprog = new DsspHelixBuilder();
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

//{{{ doFile
//##############################################################################
    public void doFile() throws IOException
    {
        File file = new File(filename);
        LineNumberReader in = new LineNumberReader(new FileReader(file));
        PdbReader pdbReader = new PdbReader();
        CoordinateFile cf = pdbReader.read(in);
        
        Model m = cf.getFirstModel();
        ModelState state = m.getState();
        processModel(cf.getIdCode(), m, state);
    }
//}}}

//{{{ printHelices, printNcapAngles
//##############################################################################
    public void printHelices()
    {
        // Only printing this if verbose...
        System.out.println("Total number helices in "+filename+": "+helices.size());
        for (Helix helix : helices)
        {
            System.out.println("** "+helix.toString());
            System.out.println("  "+helix.residues.size()+" residues");
            for (Residue residue : helix.residues)
                System.out.println("  "+residue);
            if (doNcaps)
            {
                if (helix.ncap != null)
                {
                    System.out.println("  ncap: "+helix.ncap);
                    if (helix.ncap.planeNormalAngle != Double.NaN)
                        System.out.println("  ncap plane normal angle: "+helix.ncap.planeNormalAngle);
                }
            }
            System.out.println();
        }
    }

    public void printNcapAngles()
    {
        DecimalFormat df = new DecimalFormat("#.###");
        if (doNcaps)
        {
            PrintStream out = System.out;
            out.print("file:helix:Ncap:"+
                "CaCaCa_axis:CaCb_axis:CaCa(i)Ca_CaCa(i+1)Ca:Ca(i-1)_Ca(i)_Ca(i+1):"+
                "tau(i-1):tau(i):tau(i+1):"+
                "phi(i-1):psi(i-1):phi(i):psi(i):phi(i+1):psi(i+1):");
            if (onlyHbNcaps)   out.print("NcapO_N2H:NcapO_N3H:NcapCa_N3Ca:"+
                "N'Ca_N3Ca:Ncap_HB_i+2?:Ncap_HB_i+3?:");
            out.println("cappingBox:NcapChis:N3Chis:");
            
            for (Helix helix : helices)
            {
                Ncap n = helix.ncap;
                if (n != null)
                {
                    out.print(filename+":"+helix+":"+n+":");
                    // "Backrub angles"
                    if (Double.isNaN(n.planeNormalAngle))   out.print("__?__:");
                    else    out.print(df.format(n.planeNormalAngle)+":");
                    if (Double.isNaN(n.caCbAngle))          out.print("__?__:");
                    else    out.print(df.format(n.caCbAngle)+":");
                    if (Double.isNaN(n.caPlanesAngle))      out.print("__?__:");
                    else    out.print(df.format(n.caPlanesAngle)+":");
                    if (Double.isNaN(n.caEntryAngle))       out.print("__?__:");
                    else    out.print(df.format(n.caEntryAngle)+":");
                    
                    if (Double.isNaN(n.nprimeTau))          out.print("__?__:");
                    else    out.print(df.format(n.nprimeTau)+":");
                    if (Double.isNaN(n.tau))                out.print("__?__:");
                    else    out.print(df.format(n.tau)+":");
                    if (Double.isNaN(n.n1Tau))              out.print("__?__:");
                    else    out.print(df.format(n.n1Tau)+":");
                    
                    // Phi, psi
                    if (Double.isNaN(n.nprimePhi))          out.print("__?__:");
                    else    out.print(df.format(n.nprimePhi)+":");
                    if (Double.isNaN(n.nprimePsi))          out.print("__?__:");
                    else    out.print(df.format(n.nprimePsi)+":");
                    if (Double.isNaN(n.phi))                out.print("__?__:");
                    else    out.print(df.format(n.phi)+":");
                    if (Double.isNaN(n.psi))                out.print("__?__:");
                    else    out.print(df.format(n.psi)+":");
                    if (Double.isNaN(n.n1Phi))              out.print("__?__:");
                    else    out.print(df.format(n.n1Phi)+":");
                    if (Double.isNaN(n.n1Psi))              out.print("__?__:");
                    else    out.print(df.format(n.n1Psi)+":");
                    
                    // Ncap Hbonding
                    if (onlyHbNcaps)
                    {
                        if (Double.isNaN(n.distNcapScToN2H))    out.print("__?__:");
                        else    out.print(df.format(n.distNcapScToN2H)+":");
                        if (Double.isNaN(n.distNcapScToN3H))    out.print("__?__:");
                        else    out.print(df.format(n.distNcapScToN3H)+":");
                        if (Double.isNaN(n.distNcapCaToN3Ca))   out.print("__?__:");
                        else    out.print(df.format(n.distNcapCaToN3Ca)+":");
                        if (Double.isNaN(n.distNprimeCaToN3Ca)) out.print("__?__:");
                        else    out.print(df.format(n.distNprimeCaToN3Ca)+":");
                        
                        if (n.hb_i2)   out.print("i+2:");
                        else           out.print(":");
                        if (n.hb_i3)   out.print("i+3:");
                        else           out.print(":");
                    }
                    
                    // Capping box
                    if (n.cappingBoxResType == null) out.print(":");
                    else                             out.print(n.cappingBoxResType+":");
                    
                    // Misc
                    if (n.ncapNumChis == 999)   out.print("__?__:");
                    else                        out.print(n.ncapNumChis+":");
                    if (n.n3NumChis == 999)     out.print("__?__:");
                    else                        out.print(n.n3NumChis+":");
                    
                    out.println();
                }
                else // if (n == null)
                    if (verbose && onlyHbNcaps)
                        out.println(filename+":"+helix+":no_HBed_NDST_Ncap!");
            }
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
            InputStream is = getClass().getResourceAsStream("DsspHelixBuilder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'DsspHelixBuilder.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.DsspHelixBuilder");
        System.err.println("Copyright (C) 2007 by Daniel Keedy. All rights reserved.");
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
        else if(flag.equals("-kin"))
        {
            doKin = true;
            doPrint = false;
        }
        else if(flag.equals("-print"))
        {
            doPrint = true;
        }
        else if(flag.equals("-ncaps"))
        {
            doNcaps = true;
        }
        else if(flag.equals("-onlyhbncaps"))
        {
            doNcaps = true;
            onlyHbNcaps = true;
        }
        else if(flag.equals("-smoothaxes"))
        {
            smoothAxes = true;
            if (param != null)  smoothAxesTimes = Integer.parseInt(param);
            else                smoothAxesTimes = 1;
        }
        else if(flag.equals("-vectorsumaxis"))
        {
            vectorSumAxis = true;
        }
        else if(flag.equals("-v") || flag.equals("-verbose"))
        {
            verbose = true;
        }
        else if(flag.equals("-nokinheading"))
        {
            noKinHeading = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}

}//class

