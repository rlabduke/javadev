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
* <code>HelixBuilder</code> has not yet been documented.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class HelixBuilder //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String filename;
    String list;                 // list of filenames if want more than one
    ArrayList<String> filenames; // from list
    ArrayList<Helix> helices;
    boolean doNcaps;
    boolean onlyHbNcaps;
    boolean doKin;
    boolean doPrint;
    boolean smoothAxes;
    int smoothAxesTimes;
    boolean vectorSumAxis;
    boolean verbose;
    boolean noKinHeading;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public HelixBuilder()
    {
        super();
        filename          = null;
        list              = null;
        filenames         = null;
        helices           = new ArrayList<Helix>();
        doNcaps           = false;
        onlyHbNcaps       = false;
        doKin             = false;
        doPrint           = true;
        smoothAxes        = false;
        smoothAxesTimes   = 0;
        vectorSumAxis     = false;
        verbose           = false;
        noKinHeading      = false;
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
        assignSecStruct(peptides);
        assignLeftoverSecStruct(peptides);
        //assignLeftoverSecStruct2(peptides); // ~ same thing but ignores phiC and psiN
        addHelices(peptides, model, state);
        //addHelices2(peptides, model, state); // addresses cRes vs. nRes confusion
        //updatePepDefs(peptides, model); // to comply with what was done to Residues above
        // all Peptide data has now been filled in!
        findAxes(model, state);
        if (smoothAxes)
            for (int i = 0; i < smoothAxesTimes; i ++)
                smoothAxes();
        if (vectorSumAxis)  addVectorSumAxis();
        if (doNcaps)        findNcaps(model, state);
        
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
        final double maxNOdist2 = 5.3*5.3;
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
                
                if (verbose)
                {
                    System.out.println("**"+pep[i]);
                    System.out.println("psiN: "+pep[i].psiN+", phiC: "+pep[i].phiC);
                    System.out.println();
                }
            } 
            catch (AtomException ex) {} // left as null
        }
    }
//}}}

//{{{ assignSecStruct
//##############################################################################
    /**
    * We'll use Ian's infrastructure for SheetBuilder, but since we *are* 
    * looking for helices, we'll take only take peptides for which either
    * (1) pep's cRes N is H-bonded to resi-4's O and pep's cRes phi is in beta range
    *     or
    * (2) pep's nRes O is H-bonded to resi+4's N and pep's nRes psi is in beta range
    * 
    * Note that the phi & psi cutoffs used above are very approximate and were taken
    * from a simple visual inspection of the general case Rama plot in the 2003 Ca
    * geom paper.
    */
    void assignSecStruct(Collection peptides)
    {
        // First run-through
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep != null)
            {
                if (verbose)
                    System.out.println(pep+" "+pep.phiC+" "+pep.psiN);
                
                if(pep.next != null && pep.prev != null && 
                   pep.nRes != null && pep.cRes != null)
                {
                    boolean hbForward = false, hbBackward = false;
                    
                    // Peptide's C=O makes an HB forward (beg or mid)
                    if (pep.hbondO != null && pep.hbondO.nRes != null)
                    {
                        int seqNumDiff = 
                            ((pep.hbondO).nRes).getSequenceInteger() -
                            (pep.nRes).getSequenceInteger();
                        if (seqNumDiff == 3)
                            hbForward = true;
                    }
                    
                    // Peptide's N-H makes an HB backwards (mid or end)
                    if (pep.hbondN != null && pep.hbondN.nRes != null)
                    {
                        int seqNumDiff = 
                            (pep.nRes).getSequenceInteger() - 
                            ((pep.hbondN).nRes).getSequenceInteger();
                        if (seqNumDiff == 3)
                            hbBackward = true;   //alter to allow for C caps?
                    }
                    
                    if ( (pep.phiC > -180 && pep.phiC < 0 && hbForward) ||
                         (pep.psiN > -90 && pep.psiN < 45 && hbBackward) )
                        pep.isHelix = true;
                }
            }
        }
    }
//}}}

//{{{ assignLeftoverSecStruct
//##############################################################################
    void assignLeftoverSecStruct(Collection peptides)
    {
        // Second run-through to catch residues on ends that either make an HB
        // *or* are in the proper phiC or psiN range.
        // Not as stringent here: make it either/or so we pick up those iffy 
        // ones on the ends or in between two stretches labeled as helical in 
        // assignSecStruct (i.e. "w/in" a helix)
        ArrayList<Peptide> pepsToMakeHelical = new ArrayList<Peptide>();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep != null)
            {
                if(pep.next != null && pep.prev != null && 
                   pep.nRes != null && pep.cRes != null)
                {
                    boolean hbForward = false, hbBackward = false;
                    
                    // Peptide's C=O makes an HB forward (beg or mid)
                    if (pep.hbondO != null && pep.hbondO.nRes != null)
                    {
                        int seqNumDiff = 
                            ((pep.hbondO).cRes).getSequenceInteger() -
                            (pep.cRes).getSequenceInteger(); 
                            // Was nRes to nRes, but this works equally well and
                            // makes more sense looking at the helix b/c pep.cRes
                            // is the one whose CO H-bonds to the N-H of the 
                            // capping box.
                        if (seqNumDiff == 3)
                            hbForward = true;
                    }
                    
                    // Peptide's N-H makes an HB backwards (mid or end)
                    if (pep.hbondN != null && pep.hbondN.nRes != null)
                    {
                        int seqNumDiff = 
                            (pep.nRes).getSequenceInteger() - 
                            ((pep.hbondN).nRes).getSequenceInteger();
                            // Note that pep.nRes's NH HB's to the CO of 
                            // pep.hbondN and that residue difference is 4, but 
                            // we look at the N-containing res of that target 
                            // peptide, not the C-containing res, so 3 is the 
                            // correct target
                        if (seqNumDiff == 3)
                            hbBackward = true;   //alter to allow for C caps?
                    }
                    
                    // Requirements:
                    // (1) res on one or the other side is helical
                    // (2) phi,psi in right range
                    //     or
                    //     helical (i+3) HB to a helical peptide
                    boolean nResIsHelical = false,  cResIsHelical = false,
                            rightPsiN     = false,  rightPhiC     = false, 
                            makesNwardHBtoHelicalPeptide = false,
                            makesCwardHBtoHelicalPeptide = false;
                    for(Iterator iter2 = peptides.iterator(); iter2.hasNext(); )
                    {
                        Peptide pep2 = (Peptide) iter2.next();
                        if(pep2 != null)
                        {
                            if (pep2.isHelix && pep2.cRes.equals(pep.nRes))
                                nResIsHelical = true;
                            if (pep2.isHelix && pep2.nRes.equals(pep.cRes))
                                cResIsHelical = true;
                        }
                    }
                    
                    if (pep.psiN > -90 && pep.psiN < 45)    rightPsiN = true;
                    if (pep.phiC > -180 && pep.phiC < 0)    rightPhiC = true;
                    if (hbBackward && pep.hbondN.isHelix)
                        makesNwardHBtoHelicalPeptide = true;
                    if (hbForward && pep.hbondO.isHelix)
                        makesCwardHBtoHelicalPeptide = true;
                    
                    // Was commented out version here until I realized I had
                    // confused nRes and cRes.
                    // All that changes is cResIsHelical becomes nResIsHelical
                    // and vice versa.
                    //if ((nResIsHelical && (rightPsiN || makesNwardHBtoHelicalPeptide)) ||
                    //    (cResIsHelical && (rightPhiC || makesCwardHBtoHelicalPeptide)))
                    if ((cResIsHelical && (rightPsiN || makesNwardHBtoHelicalPeptide)) ||
                        (nResIsHelical && (rightPhiC || makesCwardHBtoHelicalPeptide)))
                        pepsToMakeHelical.add(pep);//pep.isHelix = true;
                        
                }
            }
        }
        
        for (Peptide pep : pepsToMakeHelical)
            pep.isHelix = true;
    }
//}}}

//{{{ [assignLeftoverSecStruct2] not used right now
//##############################################################################
    void assignLeftoverSecStruct2(Collection peptides)
    {
        // Second run-through to catch residues on ends that make an HB in the
        // right direction.
        // Do *not* consider them helical if they're just in the right phi 
        // (C-term) or psi (N-term) region w/out an HB, b/c that might lead to 
        // 'chaining' effects in which residues are counted then the next ones 
        // are counted b/c those previous ones were counted, etc.
        ArrayList<Peptide> pepsToMakeHelical = new ArrayList<Peptide>();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep != null)
            {
                if(pep.next != null && pep.prev != null && 
                   pep.nRes != null && pep.cRes != null)
                {
                    boolean hbForward = false, hbBackward = false;
                    
                    // Peptide's C=O makes an HB forward (beg or mid)
                    if (pep.hbondO != null && pep.hbondO.nRes != null)
                    {
                        int seqNumDiff = 
                            ((pep.hbondO).nRes).getSequenceInteger() -
                            (pep.nRes).getSequenceInteger();
                        if (seqNumDiff == 3)
                            hbForward = true;
                    }
                    
                    // Peptide's N-H makes an HB backwards (mid or end)
                    if (pep.hbondN != null && pep.hbondN.nRes != null)
                    {
                        int seqNumDiff = 
                            (pep.nRes).getSequenceInteger() - 
                            ((pep.hbondN).nRes).getSequenceInteger();
                        if (seqNumDiff == 3)
                            hbBackward = true;   //alter to allow for C caps?
                    }
                    
                    // Requirements:
                    // (1) res on one or the other side is helical
                    // (2) helical (i+3) HB to a helical peptide
                    boolean nResIsHelical = false,  cResIsHelical = false,
                            makesNwardHB = false, makesCwardHB = false;
                    for(Iterator iter2 = peptides.iterator(); iter2.hasNext(); )
                    {
                        Peptide pep2 = (Peptide) iter2.next();
                        if(pep2 != null)
                        {
                            if (pep2.isHelix && pep2.cRes.equals(pep.nRes))
                                nResIsHelical = true;
                            if (pep2.isHelix && pep2.nRes.equals(pep.cRes))
                                cResIsHelical = true;
                        }
                    }
                    if (hbBackward && pep.hbondN.isHelix)   makesNwardHB = true;
                    if (hbForward && pep.hbondO.isHelix)    makesCwardHB = true;
                    
                    if ((nResIsHelical && makesNwardHB) ||
                        (cResIsHelical && makesCwardHB))
                        pepsToMakeHelical.add(pep);
                }
            }
        }
        
        for (Peptide pep : pepsToMakeHelical)
            pep.isHelix = true;
    }
//}}}

//{{{ addHelices
//##############################################################################
    void addHelices(Collection peptides, Model model, ModelState state)
    {
        // *Note* There's still some cRes vs. nRes confusion here!
        // It's been addressed in addHelices2, but that new method may work 
        // better (3_10 helices?) or worse in certain cases, so we'll leave 
        // this one just in case.
        
        // The problem: should we really be looking at the nRes Ca(i,i-3) distance 
        // at helix C-termini and the cRes Ca(i,i+3) distance at N-termini?
        // What's here seems to work well for now, so we won't mess with it 
        // just yet...
        
        // Start by finding overall first and last residues in the sequence to
        // avoid null pointer errors near the ends
        Residue firstRes = null, lastRes = null;
        for (Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue currRes = (Residue) iter.next();
            if (firstRes == null)   firstRes = currRes;
            if (lastRes == null)    lastRes = currRes;
            if (currRes.getSequenceInteger() < firstRes.getSequenceInteger())
                firstRes = currRes;
            if (currRes.getSequenceInteger() > lastRes.getSequenceInteger())
                lastRes = currRes;
        }
        
        TreeSet thisHelixsResidues = new TreeSet<Residue>();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if (pep.isHelix)
            {
                // Decide which Residues from the Peptides are helical.
                // Simultaneously catch residues on ends that meet the  
                // requirements in the assignSecStruc methods but upon 
                // visual inspection are way too far from the helix.
                
                // A convenient filter: eliminate residues from helix if
                // Ca i to i+3 > 6.0A (N-term) or Ca i to i-3 > 6.0A (C-term).
                
                // Look at cRes of peptide if worried about end of sequence
                if (pep.cRes != null && pep.cRes.getSequenceInteger() <= 
                    lastRes.getSequenceInteger() - 3)
                {
                    try
                    {
                        Residue iplus3 = pep.cRes.getNext(model).getNext(model).
                            getNext(model);
                        AtomState ca        = state.get(pep.cRes.getAtom(" CA "));
                        AtomState ca_iplus3 = state.get(iplus3.getAtom(" CA "));
                        if (ca != null && ca_iplus3 != null && ca.distance(ca_iplus3) < 6)
                        {
                            if (verbose)
                            {
                                System.out.println("Looking at "+pep);
                                System.out.println("  Did add residue "+pep.cRes+
                                    " to a helix\tCa-Ca(i+3) dist "+
                                    ca.distance(ca_iplus3));
                                System.out.println();
                            }
                            thisHelixsResidues.add(pep.cRes);
                        }
                        else
                        {
                            if (verbose)
                            {
                                System.out.println("Looking at peptide "+pep);
                                System.out.println("  *Didn't* add residue "+
                                    pep.cRes+" to a helix\tCa-Ca(i+3) dist "+
                                    ca.distance(ca_iplus3));
                                System.out.println();
                            }
                        }
                    }
                    catch (AtomException ae)
                    {
                        System.err.println("Couldn't find res i+3 for res "+pep.cRes);
                        Residue iplus3 = null;
                    }
                }
                // Look at nRes of peptide if worried about beginning of sequence
                if (pep.nRes != null && pep.nRes.getSequenceInteger() >= 
                    firstRes.getSequenceInteger() + 3)
                {
                    try
                    {
                        Residue iminus3 = pep.nRes.getPrev(model).getPrev(model).
                            getPrev(model);
                        AtomState ca         = state.get(pep.nRes.getAtom(" CA "));
                        AtomState ca_iminus3 = state.get(iminus3.getAtom(" CA "));
                        if (ca != null && ca_iminus3 != null && ca.distance(ca_iminus3) < 6)
                        {
                            if (verbose)
                            {
                                System.out.println("Looking at peptide "+pep);
                                System.out.println("  Did add residue "+
                                    pep.nRes+" to a helix\tCa-Ca(i-3) dist "+
                                    ca.distance(ca_iminus3));
                                System.out.println();
                            }
                            thisHelixsResidues.add(pep.nRes);
                        }
                        else
                        {
                            if (verbose)
                            {
                                System.out.println("Looking at peptide "+pep);
                                System.out.println("  *Didn't* add residue "+pep.nRes+
                                    " to a helix\tCa-Ca(i-3) dist "+
                                    ca.distance(ca_iminus3));
                                System.out.println();
                            }
                        }
                    }
                    catch (AtomException ae)
                    {
                        System.err.println("Couldn't find res i-3 for res "+pep.nRes);
                        Residue iminus3 = null;
                    }
                }
                
                // If done with a stretch of helical peptides
                if (!pep.next.isHelix || pep.next == null || pep.cRes == null)
                {
                    // If at least 5 residues, we're good to go.
                    // Make this helix, add it to the list, and reset this 
                    // helix-making process
                    Iterator it = thisHelixsResidues.iterator();
                    ArrayList<Residue> r = new ArrayList<Residue>();
                    while (it.hasNext())
                        r.add( (Residue)it.next() );
                    Collections.sort(r);
                    if (thisHelixsResidues.size() >= 5)
                    {
                        Helix thisHelix = new Helix(thisHelixsResidues);
                        helices.add(thisHelix);
                        thisHelixsResidues = new TreeSet<Residue>();
                        if (verbose)
                            System.out.println("Making helix starting at '"+r.get(0)+"'"+
                            " b/c size is "+r.size());
                    }
                    else
                    {
                        // Reset this helix-making process w/out making a helix
                        thisHelixsResidues = new TreeSet<Residue>();
                        if (verbose)
                            System.out.println("Only "+r.size()+" residues in this helix"+
                                " starting at '"+r.get(0)+"', so not making it...");
                    }
                }
            }
        }
    }
    //}}}

//{{{ [addHelices2]
//##############################################################################
    void addHelices2(Collection peptides, Model model, ModelState state)
    {
        // Only diff from addHelices: addresses cRes vs. nRes confusion
        
        // This one tends not to work as well, finding initial residues (which
        // we define as Ncaps) farther into the helix than they should be.
        // I don't know why...
        
        // Start by finding overall first and last residues in the sequence to
        // avoid null pointer errors near the ends
        Residue firstRes = null, lastRes = null;
        for (Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue currRes = (Residue) iter.next();
            if (firstRes == null)   firstRes = currRes;
            if (lastRes == null)    lastRes = currRes;
            if (currRes.getSequenceInteger() < firstRes.getSequenceInteger())
                firstRes = currRes;
            if (currRes.getSequenceInteger() > lastRes.getSequenceInteger())
                lastRes = currRes;
        }
        
        TreeSet thisHelixsResidues = new TreeSet<Residue>();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if (pep.isHelix)
            {
                // Decide which Residues from the Peptides are helical.
                // Simultaneously catch residues on ends that meet the  
                // requirements in the assignSecStruc methods but upon 
                // visual inspection are way too far from the helix.
                
                // A convenient filter: eliminate residues from helix if
                // Ca i to i+3 > 6.0A (N-term) or Ca i to i-3 > 6.0A (C-term).
                
                // Look at nRes of peptide if worried about end of sequence
                if (pep.nRes != null && pep.nRes.getSequenceInteger() <= 
                    lastRes.getSequenceInteger() - 3)
                {
                    try
                    {
                        Residue iplus3 = pep.nRes.getNext(model).getNext(model).
                            getNext(model);
                        AtomState ca        = state.get(pep.nRes.getAtom(" CA "));
                        AtomState ca_iplus3 = state.get(iplus3.getAtom(" CA "));
                        if (ca != null && ca_iplus3 != null && ca.distance(ca_iplus3) < 6)
                        {
                            if (verbose)
                            {
                                System.out.println("Looking at peptide "+pep);
                                System.out.println("  Did add residue "+pep.nRes+
                                    " to a helix\tCa-Ca(i+3) dist "+
                                    ca.distance(ca_iplus3));
                            }
                            thisHelixsResidues.add(pep.nRes);
                        }
                        else
                        {
                            if (verbose)
                            {
                                System.out.println("Looking at peptide "+pep);
                                System.out.println("  *Didn't* add residue "+
                                    pep.nRes+" to a helix\tCa-Ca(i+3) dist "+
                                    ca.distance(ca_iplus3));
                            }
                        }
                    }
                    catch (AtomException ae)
                    {
                        System.err.println("Couldn't find res i+3 for res "+pep.nRes);
                        Residue iplus3 = null;
                    }
                }
                // Look at cRes of peptide if worried about beginning of sequence
                if (pep.cRes != null && pep.cRes.getSequenceInteger() >= 
                    firstRes.getSequenceInteger() + 3)
                {
                    try
                    {
                        Residue iminus3 = pep.cRes.getPrev(model).getPrev(model).
                            getPrev(model);
                        AtomState ca         = state.get(pep.cRes.getAtom(" CA "));
                        AtomState ca_iminus3 = state.get(iminus3.getAtom(" CA "));
                        if (ca != null && ca_iminus3 != null && ca.distance(ca_iminus3) < 6)
                        {
                            if (verbose)
                            {
                                System.out.println("Looking at peptide "+pep);
                                System.out.println("  Did add residue "+
                                    pep.cRes+" to a helix\tCa-Ca(i-3) dist "+
                                    ca.distance(ca_iminus3));
                            }
                            thisHelixsResidues.add(pep.cRes);
                        }
                        else
                        {
                            if (verbose)
                            {
                                System.out.println("Looking at peptide "+pep);
                                System.out.println("  *Didn't* add residue "+pep.cRes+
                                    " to a helix\tCa-Ca(i-3) dist "+
                                    ca.distance(ca_iminus3));
                            }
                        }
                    }
                    catch (AtomException ae)
                    {
                        System.err.println("Couldn't find res i-3 for res "+pep.cRes);
                        Residue iminus3 = null;
                    }
                }
                
                // If done with a stretch of helical peptides
                if (!pep.next.isHelix || pep.next == null || pep.cRes == null)
                {
                    // If at least 4 residues, we're good to go.
                    // Make this helix, add it to the list, and reset this 
                    // helix-making process
                    if (thisHelixsResidues.size() >= 4)
                    {
                        Helix thisHelix = new Helix(thisHelixsResidues);
                        helices.add(thisHelix);
                        thisHelixsResidues = new TreeSet<Residue>();
                    }
                    else
                        if (verbose)
                            System.out.println("Only "+thisHelixsResidues.size()+
                                " residues in this helix, so not making it...");
                }
            }
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
                if (onlyHbNcaps && 
                    !ncapMakesHb(helix.getRes("first"), model, state).equals("i+2") &&
                    !ncapMakesHb(helix.getRes("first"), model, state).equals("i+3") )
                    helix.ncap = null;
                else
                {
                    helix.ncap = new Ncap(helix.getRes("first"));
                    if (ncapMakesHb(helix.getRes("first"), model, state).equals("i+2"))
                        helix.ncap.hb_i2 = true;
                    if (ncapMakesHb(helix.getRes("first"), model, state).equals("i+3"))
                        helix.ncap.hb_i3 = true;
                }
            }
            
            if (helix.ncap != null)
            {
                // Calculate "backrub-like" angles for each Ncap
                setNcapAngles(helix, model, state);
                
                // Set phi, psi for Ncap i, i+1, and i-1 residues
                setNcapPhiPsis(helix, model, state);
            }
        }
    }
//}}}

//{{{ ncapMakesHb
//##############################################################################
public String ncapMakesHb(Residue res, Model model, ModelState state)
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
            Triple likeH2 = new Triple(state.get(res2.getAtom(" H  ")));
            Triple likeH3 = new Triple(state.get(res3.getAtom(" H  ")));
            
            // Ser
            if (res.getName().equals("SER"))
            {
                Triple likeOG = new Triple(state.get(res.getAtom(" OG ")));
                double dist2 = Triple.distance(likeOG, likeH2);
                double dist3 = Triple.distance(likeOG, likeH3);
                if (verbose)
                    System.out.println("Ncap "+res+" makes an Hb with distance "+
                        dist2+" or "+dist3);
                if (dist2 < 2.9 && dist2 < dist3)   return "i+2";
                if (dist3 < 2.9 && dist3 < dist2)   return "i+3";
            }
            
            // Thr
            if (res.getName().equals("THR"))
            {
                Triple likeOG1 = new Triple(state.get(res.getAtom(" OG1")));
                double dist2 = Triple.distance(likeOG1, likeH2);
                double dist3 = Triple.distance(likeOG1, likeH3);
                if (verbose)
                    System.out.println("Ncap "+res+" makes an Hb with distance "+
                        dist2+" or "+dist3);
                if (dist2 < 2.9 && dist2 < dist3)   return "i+2";
                if (dist3 < 2.9 && dist3 < dist2)   return "i+3";
            }
            
            // Asn
            if (res.getName().equals("ASN"))
            {
                Triple likeOD1 = new Triple(state.get(res.getAtom(" OD1")));
                double dist2 = Triple.distance(likeOD1, likeH2);
                double dist3 = Triple.distance(likeOD1, likeH3);
                if (verbose)
                    System.out.println("Ncap "+res+" makes an Hb with distance "+
                        dist2+" or "+dist3);
                if (dist2 < 2.9 && dist2 < dist3)   return "i+2";
                if (dist3 < 2.9 && dist3 < dist2)   return "i+3";
            }
            
            // Asp
            if (res.getName().equals("ASP"))
            {
                Triple likeOD1 = new Triple(state.get(res.getAtom(" OD1")));
                double od1Dist2 = Triple.distance(likeOD1, likeH2);
                double od1Dist3 = Triple.distance(likeOD1, likeH3);
                Triple likeOD2 = new Triple(state.get(res.getAtom(" OD2")));
                double od2Dist2 = Triple.distance(likeOD2, likeH2);
                double od2Dist3 = Triple.distance(likeOD2, likeH3);
                if (verbose)
                    System.out.println("Ncap "+res+" makes an Hb with distance "+
                        od1Dist2+", "+od1Dist3+", "+od2Dist2+", or "+od2Dist3);
                if ((od1Dist2 < 2.9 && od1Dist2 < od1Dist3 && od1Dist2 < od2Dist3) ||
                    (od2Dist2 < 2.9 && od2Dist2 < od1Dist3 && od2Dist2 < od2Dist3))
                    return "i+2";
                if ((od1Dist3 < 2.9 && od1Dist3 < od1Dist2 && od1Dist3 < od2Dist2) || 
                    (od2Dist3 < 2.9 && od2Dist3 < od1Dist2 && od2Dist3 < od2Dist2))
                    return "i+3";
            }
            return "";
        }
        catch (AtomException ae)
        {
            System.err.println("Problem figuring out if "+res+" is an Asn/Asp/Ser/Thr"+
                " whose sc Hbonds to i+2 or i+3 mc and is therefore an Ncap...");
            return "";
        }
    }
    
//}}}

//{{{ setNcapAngles, setNcapPhiPsis
//##############################################################################
public void setNcapAngles(Helix helix, Model model, ModelState state)
    {
        try
        {
            // One option is angle between the local helix axis for the Ncap residue
            // and the normal to the plane formed by Ca(i,i-1,i+1).
            AtomState ca = state.get(helix.ncap.res.getAtom(" CA "));
            Triple tail = helix.axisTails.get(0);
            Triple head = helix.axisHeads.get(0);
            Triple axisAtOrigin = new Triple(head.getX()-tail.getX(),
                head.getY()-tail.getY(), head.getZ()-tail.getZ() );
            if (helix.ncap.res.getPrev(model) != null && helix.ncap.res.getNext(model) != null)
            {
                // this angle defined
                AtomState prevCa = state.get(helix.ncap.res.getPrev(model).getAtom(" CA "));
                AtomState nextCa = state.get(helix.ncap.res.getNext(model).getAtom(" CA "));
                
                Triple normal = new Triple().likeNormal(prevCa, ca, nextCa); 
                
                helix.ncap.normalTail = ca;
                helix.ncap.normalHead = new Triple(ca.getX()+normal.getX(), 
                    ca.getY()+normal.getY(), ca.getZ()+normal.getZ());
                
                // OK to mess with normal directly now
                helix.ncap.planeNormalAngle = normal.angle(axisAtOrigin);
            }
            // else (default in Ncap constructor: Double.NaN)
            
            // A second option is the angle between the Ncap Ca_Cb vector 
            // and the local helix axis
            Triple likeCa = new Triple(ca); // same coords as ca above but different object
            if (!helix.ncap.res.getName().equals("GLY"))
            {
                Triple likeCb = new Triple(state.get(helix.ncap.res.getAtom(" CB ")));
                Triple caCbAtOrigin = new Triple().likeVector(likeCa, likeCb);
                helix.ncap.caCbAngle = caCbAtOrigin.angle(axisAtOrigin);
            }
            // else (default in Ncap constructor: Double.NaN)
        }
        catch (driftwood.moldb2.AtomException ae)
        {
            System.err.println("Problem calculating Ncap angles...");
        }
    }

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

//{{{ [empty_code_segment]
//##############################################################################
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        // Make helices
        if (list != null)   doManyFiles();   // more than one filename
        else                doFile();        // just one filename
        
        // Print output
        if (doPrint)
        {
            if (verbose)
                printHelices();
            printNcapAngles();
        }
    }
    
    public static void main(String[] args)
    {
        HelixBuilder mainprog = new HelixBuilder();
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

//{{{ doFile, [doManyFiles]
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

    public void doManyFiles() throws IOException // NOT WORKING RIGHT !!!
    {
        // Set up filenames ArrayList
        filenames = new ArrayList<String>();
        
        // From website...
        FileReader input = new FileReader(list);
        BufferedReader bufRead = new BufferedReader(input);
        String line;
        int count = 0;
        line = bufRead.readLine();
        count++;
        while (line != null)
        {
            filenames.add(line);
            line = bufRead.readLine();
        }
        
        // Load model group from PDB file(s)
        for (int i = 0; i < filenames.size(); i++)
        {
            System.out.println("Doing file "+filenames.get(i));
            filename = filenames.get(i); // seen by doFile()
            doFile();
        }
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
                    System.out.println("  ncap: "+helix.ncap);
                if (helix.ncap.planeNormalAngle != Double.NaN)
                    System.out.println("  ncap plane normal angle: "+helix.ncap.planeNormalAngle);
            }
            System.out.println();
        }
    }

    public void printNcapAngles()
    {
        // Only works right for one filename at a time!
        DecimalFormat df = new DecimalFormat("#.###");
        if (doNcaps)
        {
            System.out.print("file:helix:ncap:ca(i-1)_ca(i)_ca(i+1)-local_helix_axis_angle:"+
                "ca(i)_cb(i)-local_helix_axis_angle:i-1_phi:i-1_psi:i_phi:i_psi:i+1_phi:i+1_psi");
            if (onlyHbNcaps)
                System.out.println(":ncap_hb_i+2?:ncap_hb_i+3?");
            else
                System.out.println();
            for (Helix helix : helices)
            {
                if (helix.ncap != null)
                {
                    System.out.print(filename+":"+helix+":"+helix.ncap+":");
                    if (Double.isNaN(helix.ncap.planeNormalAngle))
                        System.out.print("__?__:");
                    else
                        System.out.print(df.format(helix.ncap.planeNormalAngle)+":");
                    if (Double.isNaN(helix.ncap.caCbAngle))
                        System.out.print("__?__:");
                    else
                        System.out.print(df.format(helix.ncap.caCbAngle)+":");
                    
                    if (Double.isNaN(helix.ncap.nprimePhi))
                        System.out.print("__?__:");
                    else
                        System.out.print(df.format(helix.ncap.nprimePhi)+":");
                    if (Double.isNaN(helix.ncap.nprimePsi))
                        System.out.print("__?__:");
                    else
                        System.out.print(df.format(helix.ncap.nprimePsi)+":");
                    
                    if (Double.isNaN(helix.ncap.phi))
                        System.out.print("__?__:");
                    else
                        System.out.print(df.format(helix.ncap.phi)+":");
                    if (Double.isNaN(helix.ncap.psi))
                        System.out.print("__?__:");
                    else
                        System.out.print(df.format(helix.ncap.psi)+":");
                    
                    if (Double.isNaN(helix.ncap.n1Phi))
                        System.out.print("__?__:");
                    else
                        System.out.print(df.format(helix.ncap.n1Phi)+":");
                    if (Double.isNaN(helix.ncap.n1Psi))
                        System.out.print("__?__:");
                    else
                        System.out.print(df.format(helix.ncap.n1Psi));
                    
                    if (onlyHbNcaps)
                    {
                        if (helix.ncap.hb_i2)   System.out.print(":1:");
                        else                    System.out.print(":0:");
                        if (helix.ncap.hb_i3)   System.out.print("1");
                        else                    System.out.print("0");
                    }
                    
                    System.out.println();
                }
                else // if (helix.ncap == null)
                    if (verbose && onlyHbNcaps)
                        System.out.println(filename+":"+helix+":no_hb_ncap::::");
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
            InputStream is = getClass().getResourceAsStream("HelixBuilder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'HelixBuilder.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.HelixBuilder");
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
        else if(flag.equals("-list"))
        {
            list = param;
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

//{{{ [updatePepDefs] not working right!
//##############################################################################
    void updatePepDefs(Collection peptides, Model model)
    {
        // Update helical definitions for Peptides after refining them for Residues
        // in addHelices
        // This will make our output kins look nice (green dots only for *helical*
        // peptides) and may also be useful for later calculations.
        ArrayList<Peptide> pepsToMakeHelical = new ArrayList<Peptide>();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            int helicesWithAResFromThisPeptide = 0;
            for (Helix helix : helices)
            {
                // See if this helix contains either of the residues in this peptide
                
                Residue firstRes = null, lastRes = null;
                for (Iterator iter2 = model.getResidues().iterator(); iter2.hasNext(); )
                {
                    Residue currRes = (Residue) iter2.next();
                    if (firstRes == null)   firstRes = currRes;
                    if (lastRes == null)    lastRes = currRes;
                    if (currRes.getSequenceInteger() < firstRes.getSequenceInteger())
                        firstRes = currRes;
                    if (currRes.getSequenceInteger() > lastRes.getSequenceInteger())
                        lastRes = currRes;
                }
                int nIntHelix = firstRes.getSequenceInteger();
                int cIntHelix = lastRes.getSequenceInteger();
                int nIntPep = -999999;
                if (pep.nRes != null)   nIntPep = pep.nRes.getSequenceInteger();
                int cIntPep = 999999;
                if (pep.cRes != null)   cIntPep = pep.cRes.getSequenceInteger();
                
                boolean nResInHelix = false;
                if (nIntPep > nIntHelix && nIntPep < cIntHelix) nResInHelix = true;
                boolean cResInHelix = false;
                if (cIntPep > nIntHelix && cIntPep < cIntHelix) cResInHelix = true;
                
                if (nResInHelix || cResInHelix) helicesWithAResFromThisPeptide ++;
                
                if (verbose)
                {
                    if (helix.residues.contains(pep.nRes) || 
                        helix.residues.contains(pep.cRes))
                    {
                        System.out.println(helix.toString()+" contains "+
                            pep.nRes+" or "+pep.cRes);
                        helicesWithAResFromThisPeptide ++;
                    }
                    else
                        System.out.println(helix.toString()+" does NOT contain "
                            +pep.nRes+" or "+pep.cRes);
                }
            }
            if (helicesWithAResFromThisPeptide > 0)
                pep.isHelix = true;
            else
                pep.isHelix = false;
        }
        
        for (Peptide pep : pepsToMakeHelical)
            pep.isHelix = true;
    }
//}}}

//{{{ [myDihedral] unused now!
//##############################################################################
    /**
    * This is Ian's comment in driftwood.r3.Triple which is "too expensive in 
    * terms of creating objects, as it creates 3 new Triples, but it *is* easy
    * to understand the code.
    * 
    * I'm using it since I got weird results when I just used Triple.dihedral
    * (4 Triples).  Don't know why it would work, but we'll see...
    */
    double myDihedral(Triple a, Triple b, Triple c, Triple d)
    {
        Triple e, f, g, u, v;
        e = new Triple().likeDiff(b, a);
        f = new Triple().likeDiff(c, b);
        g = new Triple().likeDiff(d, c);
        u = e.cross(f); // overwrite 'e' b/c we don't need it anymore
        v = f.cross(g); // overwrite 'f' b/c we don't need it anymore
        
        double dihedral = u.angle(v);
        
        // Solve handedness problem:
        if(u.angle(g) > 90.0) dihedral = -dihedral;
        
        return dihedral;
    }
//}}}

//{{{ [addVectorSumAxis] not working right!
//##############################################################################
public void addVectorSumAxis()
    {
        /** 
        * Makes a single, vector sum helical axis from the local helical 
        * axes for each set of 4 Ca's in each Helix made in findAxes.
        * Goal: Generate a direction that is robust to weirdnesses in the local
        * geometry (e.g. helix turning into beta or something) but does reflect
        * subtle local changes in helical direction.
        */
        
        
        // WHAT'S WRONG WITH THESE AXES' DIRECTIONS ???
        // PROBABLY AN ISSUE W/ NOT TRANSLATING TO ORIGIN...
        
        
        for (Helix helix : helices)
        {
            double x = 0, y = 0, z = 0;
            for (int i = 0; i < helix.axisHeads.size(); i ++)
            {
                Triple head = helix.axisHeads.get(i);
                Triple tail = helix.axisTails.get(i);
                Triple axisAtOrigin = new Triple(head.getX()-tail.getX(),
                    head.getY()-tail.getY(), head.getZ()-tail.getZ());
                x += axisAtOrigin.getX(); 
                y += axisAtOrigin.getY(); 
                z += axisAtOrigin.getZ();
            }
            
            helix.vectorSumAxisTail = helix.axisTails.get(0);
            helix.vectorSumAxisHead = new Triple(x, y, z).unit().mult(3).add(
                new Triple(helix.axisTails.get(0)));
            
        }//for(each Helix in helices)
    }
//}}}

}//class

