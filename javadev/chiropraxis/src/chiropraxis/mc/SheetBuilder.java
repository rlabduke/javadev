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
import driftwood.util.Strings;
//}}}
/**
* <code>SheetBuilder</code> has not yet been documented.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class SheetBuilder //extends ... implements ...
{
//{{{ Constants
    String aaNames = "ALA,ARG,ASN,ASP,CYS,GLU,GLN,GLY,HIS,ILE,LEU,LYS,MET,PHE,PRO,SER,THR,TRP,TYR,VAL";
    String aromNames = "PHE,TYR";
    DecimalFormat df = new DecimalFormat("0.0##");
//}}}

//{{{ Variable definitions
//##############################################################################
    String filename               = null;
    boolean verbose               = false;
    boolean doBetaAroms           = false;
    boolean doKin                 = true;
    TreeSet<String> oppNames      = null;
    ArrayList<BetaArom> betaAroms = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SheetBuilder()
    {
        super();
    }
//}}}

//{{{ processModel
//##############################################################################
    void processModel(String modelName, Model model, ModelState state)
    {
        if(verbose) System.err.println("Processing "+modelName+"["+model+"]");
        
        // Create a set of Peptides and connect them up
        Collection peptides = createPeptides(model, state);
        connectPeptides(peptides);
        findHBonds(peptides, state);
        
        // Try to identify sheet based on H-bonding pattern
        assignSecStruct(peptides);
        // all Peptide data has now been filled in!
        
        // Map each residue to a beta-sheet plane
        // and a normal to that plane, if possible.
        // Returns a Map<Residue, Triple>
        Map normals1 = calcSheetNormals1(peptides, model, state);
        // Flesh the normals out into a local coordinate system
        // and measure the Ca-Cb's angle to the normal.
        Map angles1 = measureSheetAngles1(peptides, normals1, state);
        
        // Similar to Ian's normals & angles above, but 
        // instead of assigning one plane to a strand plus both its neighbor strands,
        // now we assign a pair of planes to each strand: one for each neighbor strand.
        // Returns a Map<Residue, Triple[]>
        Map normals2 = calcSheetNormals2(peptides, model, state);
        // Measure concavity vs. convexity in the "across-strand" direction.
        Map angles2 = measureSheetAngles2(peptides, normals2, state);
        
        // Similar to the above two methods, but 
        // now the pair of planes is spaced *along* the central strand 
        // rather than *across* the three strands.
        // Returns a Map<Residue, Triple[]>
        Map normals3 = calcSheetNormals3(peptides, model, state);
        // Measure concavity vs. convexity in the "along-strand" direction.
        Map angles3 = measureSheetAngles3(peptides, normals3, state);
        
        if(doBetaAroms)
        {
            // This is the stuff Daniel Keedy and Ed Triplett came up with 
            // for a local system for an aromatic residue in a plus (p) rotamer
            // across from a given residue type in a beta sheet.
            // If the -betaarom flag is used, we'll hijack Ian's earlier code
            // to decide which peptides in this model deserve beta status, 
            // then launch into our own stuff.
            addBetaAroms(peptides, modelName, model, state);
        }
        
        if(doKin)
        {
            System.out.println("@kinemage {"+modelName+" sheets}");
            sketchHbonds(System.out, peptides, state);
            System.out.println("@group {normals}");
            sketchNormals1(System.out, normals1, state);
            sketchNormals2(System.out, normals2, state);
            sketchNormals3(System.out, normals3, state);
            //sketchPlanes(System.out, angles1, state);
            //sketchLocalAxes(System.out, angles1, state);
            if(doBetaAroms) sketchBetaAroms(System.out, state, angles2, angles3);
        }
        else
        {
            // Print csv-formatted stats
            if(doBetaAroms)
                printBetaAromStats(System.out, angles1, angles2, angles3);
            else
            {
                // Default if not -kin and not -betaarom:
                // complete the standard sheet stats output.
                printNeighborAngles2(System.out, modelName, peptides, angles2);
                printNeighborAngles3(System.out, modelName, peptides, angles3);
            }
        }
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

//{{{ assignSecStruct
//##############################################################################
    /**
    * Given a collection of Peptides, we attempt to flag some of them as being
    * part of a beta sheet.
    * <p>If they're antiparallel and beta, and the nitrogen of peptide n is
    * H-bonded to the oxygen of peptide m, then one of the following is true: <ul>
    * <li>n+1's O is H-bonded to m-1's N</li>
    * <li>n-1's O is H-bonded to m+1's N</li>
    * </ul>
    * <p>If they're parallel and beta, and the nitrogen of peptide n is
    * H-bonded to the oxygen of peptide m, then one of the following is true: <ul>
    * <li>n+1's O is H-bonded to m+1's N</li>
    * <li>n-1's O is H-bonded to m-1's N</li>
    * </ul>
    * However, it must ALSO be true that |n-m| is greater than 5 OR that
    * n and m are in different chains to avoid picking up helices here.
    * These fields will be filled: isBeta, isParallelN, isParallelO
    */
    void assignSecStruct(Collection peptides)
    {
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pepN = (Peptide) iter.next();
            Peptide pepM = pepN.hbondN;
            if(pepM != null)
            {
                // Antiparallel?
                if(pepN.next != null && pepM.prev != null && pepN.next.hbondO == pepM.prev)
                {
                    pepN.isBeta = pepM.isBeta = true;
                    pepN.isParallelN = pepM.isParallelO = false;
                }
                else if(pepN.prev != null && pepM.next != null && pepN.prev.hbondO == pepM.next)
                {
                    pepN.isBeta = pepM.isBeta = true;
                    pepN.isParallelN = pepM.isParallelO = false;
                }
                // Parallel?
                else if(pepN.chain != pepM.chain || Math.abs(pepN.index - pepM.index) > 5)
                {
                    if(pepN.next != null && pepM.next != null && pepN.next.hbondO == pepM.next)
                    {
                        pepN.isBeta = pepM.isBeta = true;
                        pepN.isParallelN = pepM.isParallelO = true;
                    }
                    else if(pepN.prev != null && pepM.prev != null && pepN.prev.hbondO == pepM.prev)
                    {
                        pepN.isBeta = pepM.isBeta = true;
                        pepN.isParallelN = pepM.isParallelO = true;
                    }
                }
            }
        }
    }
//}}}

//{{{ calcSheetNormals1
//##############################################################################
    /**
    * Returns a Map&lt;Residue, Triple&gt; that maps each Residue in model
    * that falls in a "reasonable" part of the beta sheet to a Triple
    * representing the normal vector of the beta sheet at that Residue's C-alpha.
    * The normal is the normal of a plane least-squares fit through
    * six nearby peptide centers: the ones before and after this residue in
    * the strand, and their two (each) H-bonding partners, all of which
    * must be present and classified as being in beta sheet.
    */
    Map calcSheetNormals1(Collection peptides, Model model, ModelState state)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }
        
        Map normals = new HashMap();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Peptide cPep = (Peptide) cPeptides.get(res);
            Peptide nPep = (Peptide) nPeptides.get(res);
            if(cPep != null && cPep.hbondN != null && cPep.hbondO != null
            && nPep != null && nPep.hbondN != null && nPep.hbondO != null
            && cPep.isBeta && cPep.hbondN.isBeta && cPep.hbondO.isBeta
            && nPep.isBeta && nPep.hbondN.isBeta && nPep.hbondO.isBeta)
            {
                Collection guidePts = new ArrayList();
                guidePts.add(cPep.hbondN.midpoint);
                guidePts.add(cPep.midpoint);
                guidePts.add(cPep.hbondO.midpoint);
                guidePts.add(nPep.hbondN.midpoint);
                guidePts.add(nPep.midpoint);
                guidePts.add(nPep.hbondO.midpoint);
                LsqPlane lsqPlane = new LsqPlane(guidePts);
                Triple normal = new Triple(lsqPlane.getNormal());
                normals.put(res, normal);
                // Try to make it point the same way as Ca-Cb
                try
                {
                    AtomState ca = state.get(res.getAtom(" CA "));
                    AtomState cb = state.get(res.getAtom(" CB "));
                    Triple cacb = new Triple(cb).sub(ca);
                    if(cacb.dot(normal) < 0) normal.neg();
                }
                catch(AtomException ex) {} // oh well (e.g. Gly)
            }
        }
        return normals;
    }
//}}}

//{{{ calcSheetNormals2
//##############################################################################
    /**
    * Returns a Map&lt;Residue, Triple[]&gt; that maps each Residue in the model
    * that falls in a "reasonable" part of the beta sheet to a Triple[] 
    * containing the following:<ol>
    *   <li>the unit vector sum of the normals of two planes,
    *       each defined by a dipeptide on the central strand 
    *       and a dipeptide on one of the adjacent strands</li>
    *   <li>a vector pointing from the centroid of one of those planes
    *       to the centroid of the other plane</li></ol>
    * This only works if there's a strand on both sides!
    * Note that before their summation and unit-vector-ification, the two normals 
    * have been inverted if necessary so that they both point toward the central 
    * strand.  However, this *hasn't* been done to the C&alpha-C&beta vector!  
    * This way, the normals' unit sum forms a local "concavity vector", that 
    * can then be compared with the C&alpha-C&beta vector once the requisite 
    * projections are performed (happens within SheetAxes2).
    */
    Map calcSheetNormals2(Collection peptides, Model model, ModelState state)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }
        
        Map normals = new HashMap();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Peptide cPep = (Peptide) cPeptides.get(res);
            Peptide nPep = (Peptide) nPeptides.get(res);
            if(cPep != null && cPep.hbondN != null && cPep.hbondO != null
            && nPep != null && nPep.hbondN != null && nPep.hbondO != null
            && cPep.isBeta && cPep.hbondN.isBeta && cPep.hbondO.isBeta
            && nPep.isBeta && nPep.hbondN.isBeta && nPep.hbondO.isBeta)
            {
                // Normal 1: thru central residue's CO
                // ("Opposite" strand for plus aromatics)
                Collection guidePts1 = new ArrayList();
                guidePts1.add(cPep.midpoint);
                guidePts1.add(nPep.midpoint);
                guidePts1.add(cPep.hbondO.midpoint);
                guidePts1.add(nPep.hbondN.midpoint);
                LsqPlane lsqPlane1 = new LsqPlane(guidePts1);
                Triple normal1 = new Triple(lsqPlane1.getNormal());
                Triple anchor1 = new Triple(lsqPlane1.getAnchor());
                
                // Normal 2: thru central residue's NH
                Collection guidePts2 = new ArrayList();
                guidePts2.add(cPep.midpoint);
                guidePts2.add(nPep.midpoint);
                guidePts2.add(cPep.hbondN.midpoint);
                guidePts2.add(nPep.hbondO.midpoint);
                LsqPlane lsqPlane2 = new LsqPlane(guidePts2);
                Triple normal2 = new Triple(lsqPlane2.getNormal());
                Triple anchor2 = new Triple(lsqPlane2.getAnchor());
                
                // Try to make both normals point toward the middle strand
                Triple anchor1to2 = new Triple().likeVector(anchor1, anchor2);
                Triple anchor2to1 = new Triple().likeVector(anchor2, anchor1);
                if(normal1.dot(anchor1to2) < 0)   normal1.neg();
                if(normal2.dot(anchor2to1) < 0)   normal2.neg();
                
                if(normal1.angle(normal2) < 90)
                {
                    //   ...res...                   \ ...res... /    <= bad normals
                    //  /         \    instead of     /         \     <= planes
                    //   \       /                                    <= good normals
                    
                    // Now that the two side-planes' normals are pointing toward the
                    // central strand (as above), we know that their vector sum 
                    // indicates the *concave* direction of the local beta sheet!
                    Triple[] vectors = new Triple[2];
                    vectors[0] = new Triple().likeSum(normal1, normal2).unit();
                    vectors[1] = new Triple(anchor2).sub(anchor1);
                    normals.put(res, vectors);
                }
            }
        }
        return normals;
    }
//}}}

//{{{ calcSheetNormals3
//##############################################################################
    /**
    * Returns a Map&lt;Residue, Triple[]&gt; that maps each Residue in the model
    * that falls in a "reasonable" part of the beta sheet to a Triple[] 
    * containing the following:<ol>
    *   <li>the unit vector sum of the normals of two planes,
    *       one defined by the most and second-most N-ward peptides
    *       (relative to the central strand) of the two adjacent strands,
    *       and the other defined by the most and second-most C-ward peptides 
    *       (relative to the central strand) of the two adjacent strands</li>
    *   <li>a vector pointing from the former plane's centroid 
    *       to the latter plane's centroid</li>
    *   <li>a normal to the central four peptides' plane (not counting the 
    *       central strand), to serve at an approximation to the local 
    *       along-strand coordinate system</li></ol>
    * Note that before their summation and unit-vector-ification, the two normals 
    * have been inverted if necessary so that they both point toward the central 
    * strand.  However, this *hasn't* been done to the C&alpha-C&beta vector!  
    * This way, the normals' unit sum forms a local "concavity vector", that 
    * can then be compared with the C&alpha-C&beta vector once the requisite 
    * projections are performed (happens within SheetAxes3).
    */
    Map calcSheetNormals3(Collection peptides, Model model, ModelState state)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }
        
        Map normals = new HashMap();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Peptide cPep = (Peptide) cPeptides.get(res);
            Peptide nPep = (Peptide) nPeptides.get(res);
            if(cPep != null && cPep.hbondN != null && cPep.hbondO != null
            && nPep != null && nPep.hbondN != null && nPep.hbondO != null
            && cPep.isBeta && cPep.hbondN.isBeta && cPep.hbondO.isBeta
            && nPep.isBeta && nPep.hbondN.isBeta && nPep.hbondO.isBeta)
            {
                // Figure out which peptide relative to each of the (c,n)Pep.hbond(N,O)
                // is closest to the central Calpha and therefore should be used.
                // This just depends on whether we're in || or anti-|| sheet.
                Peptide nPepHbNNeighbor = null, nPepHbONeighbor = null, 
                        cPepHbNNeighbor = null, cPepHbONeighbor = null;
                if(nPep.isParallelN)  nPepHbNNeighbor = nPep.hbondN.prev;
                else                  nPepHbNNeighbor = nPep.hbondN.next; // anti-||
                if(nPep.isParallelO)  nPepHbONeighbor = nPep.hbondO.prev;
                else                  nPepHbONeighbor = nPep.hbondO.next; // anti-||
                if(cPep.isParallelN)  cPepHbNNeighbor = cPep.hbondN.next;
                else                  cPepHbNNeighbor = cPep.hbondN.prev; // anti-||
                if(cPep.isParallelO)  cPepHbONeighbor = cPep.hbondO.next;
                else                  cPepHbONeighbor = cPep.hbondO.prev; // anti-||
                if(nPepHbNNeighbor != null && nPepHbONeighbor != null
                && cPepHbNNeighbor != null && cPepHbONeighbor != null
                && nPepHbNNeighbor.isBeta  && nPepHbONeighbor.isBeta
                && cPepHbNNeighbor.isBeta  && cPepHbONeighbor.isBeta)
                {
                    // Normal 1: N-ward on central strand
                    Collection guidePts1 = new ArrayList();
                    guidePts1.add(nPep.hbondN.midpoint);
                    guidePts1.add(nPep.hbondO.midpoint);
                    guidePts1.add(nPepHbNNeighbor.midpoint);
                    guidePts1.add(nPepHbONeighbor.midpoint);
                    LsqPlane lsqPlane1 = new LsqPlane(guidePts1);
                    Triple normal1 = new Triple(lsqPlane1.getNormal());
                    Triple anchor1 = new Triple(lsqPlane1.getAnchor());
                    
                    // Normal 2: C-ward on central strand
                    Collection guidePts2 = new ArrayList();
                    guidePts2.add(cPep.hbondN.midpoint);
                    guidePts2.add(cPep.hbondO.midpoint);
                    guidePts2.add(cPepHbNNeighbor.midpoint);
                    guidePts2.add(cPepHbONeighbor.midpoint);
                    LsqPlane lsqPlane2 = new LsqPlane(guidePts2);
                    Triple normal2 = new Triple(lsqPlane2.getNormal());
                    Triple anchor2 = new Triple(lsqPlane2.getAnchor());
                    
                    // Try to make both normals point in the general direction of
                    // the central residue
                    Triple anchor1to2 = new Triple().likeVector(anchor1, anchor2);
                    Triple anchor2to1 = new Triple().likeVector(anchor2, anchor1);
                    if(normal1.dot(anchor1to2) < 0)   normal1.neg();
                    if(normal2.dot(anchor2to1) < 0)   normal2.neg();
                    
                    // Make an approximation to the Z axis in order to define the
                    // coordinate plane. Its up-or-down direction is irrelevant
                    // since CaCb and the normals' sum will be defined in the same 
                    // coord system, i.e. relative to zAxis, so comparisons btw 
                    // them are valid.
                    Collection guidePts3 = new ArrayList();
                    guidePts3.add(nPep.hbondN.midpoint);
                    guidePts3.add(nPep.hbondO.midpoint);
                    guidePts3.add(cPep.hbondN.midpoint);
                    guidePts3.add(cPep.hbondO.midpoint);
                    LsqPlane lsqPlane3 = new LsqPlane(guidePts3);
                    
                    if(normal1.angle(normal2) < 90)
                    {
                        // Now that the two side-planes' normals are pointing toward the
                        // central residue, we know that their vector sum indicates the
                        // *concave* direction of the local beta sheet!
                        Triple[] vectors = new Triple[3];
                        vectors[0] = new Triple().likeSum(normal1, normal2).unit();
                        vectors[1] = new Triple(anchor2).sub(anchor1);
                        vectors[2] = new Triple(lsqPlane3.getNormal()); // the approx Z axis
                        normals.put(res, vectors);
                    }
                }
            }
        }
        return normals;
    }
//}}}

//{{{ measureSheetAngles1
//##############################################################################
    /** Returns a map of Residues to SheetAxes */
    Map measureSheetAngles1(Collection peptides, Map normals, ModelState state)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }
        
        Map angles = new HashMap();
        for(Iterator iter = normals.keySet().iterator(); iter.hasNext(); )
        {
            Residue res     = (Residue) iter.next();
            Triple  normal  = (Triple) normals.get(res);
            Peptide cPep    = (Peptide) cPeptides.get(res);
            Peptide nPep    = (Peptide) nPeptides.get(res);
            if(cPep == null || nPep == null) continue;
            Triple  n2c     = new Triple(cPep.midpoint).sub(nPep.midpoint);
            try
            {
                AtomState   ca      = state.get(res.getAtom(" CA "));
                AtomState   cb      = state.get(res.getAtom(" CB "));
                Triple      cacb    = new Triple(cb).sub(ca);
                SheetAxes   axes    = new SheetAxes(normal, n2c, cacb);
                angles.put(res, axes);
            }
            catch(AtomException ex) {}
        }
        return angles;
    }
//}}}

//{{{ measureSheetAngles2
//##############################################################################
    /** Returns a map of Residues to SheetAxes2 */
    Map measureSheetAngles2(Collection peptides, Map normals2, ModelState state)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }
        
        Map angles2 = new HashMap();
        for(Iterator iter = normals2.keySet().iterator(); iter.hasNext(); )
        {
            Residue res      = (Residue) iter.next();
            Triple[] vectors = (Triple[]) normals2.get(res);
            Peptide cPep     = (Peptide) cPeptides.get(res);
            Peptide nPep     = (Peptide) nPeptides.get(res);
            if(cPep == null || nPep == null) continue;
            Triple  n2c      = new Triple(cPep.midpoint).sub(nPep.midpoint);
            try
            {
                AtomState   ca      = state.get(res.getAtom(" CA "));
                AtomState   cb      = state.get(res.getAtom(" CB "));
                Triple      cacb    = new Triple(cb).sub(ca);
                SheetAxes2  axes2   = new SheetAxes2(vectors[0], vectors[1], n2c, cacb);
                angles2.put(res, axes2);
            }
            catch(AtomException ex) {}
        }
        return angles2;
    }
//}}}

//{{{ measureSheetAngles3
//##############################################################################
    /** Returns a map of Residues to SheetAxes3 */
    Map measureSheetAngles3(Collection peptides, Map normals3, ModelState state)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }
        
        Map angles3 = new HashMap();
        for(Iterator iter = normals3.keySet().iterator(); iter.hasNext(); )
        {
            Residue res      = (Residue) iter.next();
            Triple[] vectors = (Triple[]) normals3.get(res);
            Peptide cPep     = (Peptide) cPeptides.get(res);
            Peptide nPep     = (Peptide) nPeptides.get(res);
            if(cPep == null || nPep == null) continue;
            Triple  n2c      = new Triple(cPep.midpoint).sub(nPep.midpoint);
            try
            {
                AtomState   ca      = state.get(res.getAtom(" CA "));
                AtomState   cb      = state.get(res.getAtom(" CB "));
                Triple      cacb    = new Triple(cb).sub(ca);
                SheetAxes3  axes3   = new SheetAxes3(vectors[0], vectors[1], vectors[2], cacb);
                angles3.put(res, axes3);
            }
            catch(AtomException ex) {}
        }
        return angles3;
    }
//}}}

//{{{ addBetaAroms
//##############################################################################
    /**
    * Finds "beta aromatics": Phe/Tyr in a beta strand with an Xaa residue 
    * on the adjacent beta strand (in the aromatic's CO H-bonding direction).
    * These strands can be either anti-parallel or (less common?) parallel.
    * In the case of anti-parallel strands where the aromatic has a plus-chi1 rotamer, 
    * we're dealing with Daniel Keedy & Ed Triplett's backrub-related examples.
    */
    void addBetaAroms(Collection peptides, String modelName, Model model, ModelState state)
    {
        if(verbose) System.err.println("Adding beta aromatics");
        betaAroms = new ArrayList<BetaArom>();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            // We're arbitrarily going for peptides with the aromatic on 
            // its CO-containing residue (i.e. earlier in sequence), 
            // not its N-containing residue (i.e. later in sequence).
            
            // Aromatic
            Peptide pepM = (Peptide) iter.next();
            if(pepM == null) continue;
            if(!pepM.isBeta || pepM.prev == null || !pepM.prev.isBeta) continue; // prev pep has arom NH
            if(pepM.cRes == null) continue;
            Residue aromRes = pepM.cRes;
            if(aromNames.indexOf(aromRes.getName()) == -1) continue;
            
            // Opposite
            Peptide pepN = pepM.hbondO;
            if(pepN == null) continue;
            Residue oppRes = null;
            if(!pepM.isParallelO) // anti-parallel
            {
                if(!pepN.isBeta || pepN.next == null || !pepN.next.isBeta) continue;
                oppRes = pepN.nRes;
            }
            else // parallel
            {
                if(!pepN.isBeta || pepN.prev == null || !pepN.prev.isBeta) continue;
                oppRes = pepN.cRes;
            }
            if(oppRes == null) continue;
            if(!oppNames.contains(oppRes.getName())) continue;
            
            BetaArom ba = new BetaArom(aromRes, oppRes, pepM.isParallelO, peptides, model, modelName);
            try
            {
                ba.calcGeometry();
                betaAroms.add(ba);
                if(verbose) System.err.println(".. Added "+ba);
            }
            catch(AtomException ex) // really a beta arom, but necessary atom(s) missing
            { System.err.println("Error creating "+ba); }
            catch(NullPointerException ex) // really a beta arom, but necessary atom(s) missing
            { System.err.println("Error creating "+ba); }
        }
        if(verbose) System.err.println("Beta aromatics found: "+betaAroms.size());
    }
//}}}

//{{{ sketchHbonds
//##############################################################################
    void sketchHbonds(PrintStream out, Collection peptides, ModelState state)
    {
        out.println("@group {peptides & hbonds}");
        out.println("@balllist {peptides} radius= 0.1 color= green");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            
            if(pep.isBeta)
                out.println("{"+pep+"} r=0.3 "+pep.midpoint.format(df));
            else
            {
                // If using -betaaroms flag, we're only interested in aromatics
                // in beta regions and don't even wanna print non-beta peptides
                if(!doBetaAroms)
                    out.println("{"+pep+"} "+pep.midpoint.format(df));
            }
        }
        
        out.println("@vectorlist {N hbonds} color= sky");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.hbondN != null)
            {
                if(pep.isBeta || !doBetaAroms) // Again, if -betaaroms, we're picky
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
        }
        
        out.println("@vectorlist {O hbonds} color= red");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.hbondO != null)
            {
                if(pep.isBeta || !doBetaAroms) // Again, if -betaaroms, we're picky
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
    }
//}}}

//{{{ sketchBetaAroms
//##############################################################################
    void sketchBetaAroms(PrintStream out, ModelState state, Map angles2, Map angles3)
    {
        out.println("@group {beta aroms}");
        
        out.println("@balllist {Calphas} radius= 0.3 color= hotpink");
        for(BetaArom ba : betaAroms)
        {
            try
            {
                AtomState ca = state.get(ba.aromRes.getAtom(" CA "));
                out.println("{"+ba+"} "+ca.format(df));
            }
            catch(AtomException ex)
            { System.err.println("Error sketching CA ball for "+ba); }
        }
        
        out.println("@vectorlist {cross-sheet} color= yellow off");
        for(BetaArom ba : betaAroms)
        {
            try
            {
                for(Iterator iter = angles2.keySet().iterator(); iter.hasNext(); )
                {
                    Residue res = (Residue) iter.next();
                    if(res.equals(ba.aromRes))
                    {
                        AtomState ca = state.get(ba.aromRes.getAtom(" CA "));
                        SheetAxes2 axes2 = (SheetAxes2) angles2.get(res);
                        
                        Triple caPlusNormal12 = new Triple().likeSum(ca, axes2.normal.mult(2));
                        out.println("{normal1+2 unit vector}P "+ca.format(df));
                        out.println("{normal1+2 unit vector} "+caPlusNormal12.format(df));
                        
                        Triple caPlusStrand = new Triple().likeSum(ca, axes2.strand.mult(2));
                        out.println("{strand}P "+ca.format(df));
                        out.println("{strand} "+caPlusStrand.format(df));
                        
                        Triple caPlusZAxis = new Triple().likeSum(ca, axes2.zAxis.mult(2));
                        out.println("{zAxis}P "+ca.format(df));
                        out.println("{zAxis} "+caPlusZAxis.format(df));
                        
                        Triple caPlusCross = new Triple().likeSum(ca, axes2.cross.mult(2));
                        out.println("{cross}P "+ca.format(df));
                        out.println("{cross} "+caPlusCross.format(df));
                    }
                }
            }
            catch(AtomException ex)
            { System.err.println("Error sketching SheetAxes2 vectors for "+ba); }
        }
        
        out.println("@vectorlist {along-sheet} color= sky off");
        for(BetaArom ba : betaAroms)
        {
            try
            {
                for(Iterator iter = angles3.keySet().iterator(); iter.hasNext(); )
                {
                    Residue res = (Residue) iter.next();
                    if(res.equals(ba.aromRes))
                    {
                        AtomState ca = state.get(ba.aromRes.getAtom(" CA "));
                        SheetAxes3 axes3 = (SheetAxes3) angles3.get(res);
                        
                        Triple caPlusNormal12 = new Triple().likeSum(ca, axes3.normal.mult(2));
                        out.println("{normal1+2 unit vector}P "+ca.format(df));
                        out.println("{normal1+2 unit vector} "+caPlusNormal12.format(df));
                        
                        Triple caPlusStrand = new Triple().likeSum(ca, axes3.strand.mult(2));
                        out.println("{strand}P "+ca.format(df));
                        out.println("{strand} "+caPlusStrand.format(df));
                        
                        Triple caPlusZAxis = new Triple().likeSum(ca, axes3.zAxis.mult(2));
                        out.println("{zAxis}P "+ca.format(df));
                        out.println("{zAxis} "+caPlusZAxis.format(df));
                        
                        Triple caPlusCross = new Triple().likeSum(ca, axes3.cross.mult(2));
                        out.println("{cross}P "+ca.format(df));
                        out.println("{cross} "+caPlusCross.format(df));
                    }
                }
            }
            catch(AtomException ex)
            { System.err.println("Error sketching SheetAxes3 vectors for "+ba); }
        }
        
    }
//}}}

//{{{ sketchNormals1
//##############################################################################
    /** Sketches normals to 6-Calpha planes. */
    void sketchNormals1(PrintStream out, Map normals, ModelState state)
    {
        out.println("@arrowlist {6-Calpha} color= magenta radius= 0.25 off");
        for(Iterator iter = normals.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Triple normal = (Triple) normals.get(res);
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple(normal).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{normal: "+tip.format(df)+"} "+tip.format(df));
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ sketchNormals2
//##############################################################################
    /**
    * Sketches normals to 4-Calpha planes on either side of central strand.
    * Shows cross-sheet concavity.
    */
    void sketchNormals2(PrintStream out, Map normals, ModelState state)
    {
        out.println("@arrowlist {cross-sheet} color= lilac radius= 0.25");
        for(Iterator iter = normals.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Triple[] vectors = (Triple[]) normals.get(res);
            Triple normal = vectors[0];
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple(normal).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{cross-sheet normal: "+tip.format(df)+"} "+tip.format(df));
            }
            catch(AtomException ex) { ex.printStackTrace(); }
            /*Triple anchor1PlusNormal1 = new Triple().likeSum(anchor1, new Triple(normal1).mult(2));
            Triple anchor2PlusNormal2 = new Triple().likeSum(anchor2, new Triple(normal2).mult(2));
            out.println("{"+res+" cross-sheet normal}P "+anchor1.format(df));
            out.println("{"+res+" cross-sheet normal} " +anchor1PlusNormal1.format(df));
            out.println("{"+res+" cross-sheet normal}P "+anchor2.format(df));
            out.println("{"+res+" cross-sheet normal} " +anchor2PlusNormal2.format(df));*/
        }
    }
//}}}
        
//{{{ sketchNormals3
//##############################################################################
    /**
    * Sketches normals to 4-Calpha planes in either direction along central strand.
    * Shows along-sheet concavity.
    */
    void sketchNormals3(PrintStream out, Map normals, ModelState state)
    {
        out.println("@arrowlist {along-sheet} color= pink radius= 0.25");
        for(Iterator iter = normals.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Triple[] vectors = (Triple[]) normals.get(res);
            Triple normal = vectors[0];
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple(normal).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{along-sheet normal: "+tip.format(df)+"} "+tip.format(df));
            }
            catch(AtomException ex) { ex.printStackTrace(); }
            /*Triple anchor1PlusNormal1 = new Triple().likeSum(anchor1, new Triple(normal1).mult(2));
            Triple anchor2PlusNormal2 = new Triple().likeSum(anchor2, new Triple(normal2).mult(2));
            out.println("{"+res+" along-sheet normal}P "+anchor1.format(df));
            out.println("{"+res+" along-sheet normal} " +anchor1PlusNormal1.format(df));
            out.println("{"+res+" along-sheet normal}P "+anchor2.format(df));
            out.println("{"+res+" along-sheet normal} " +anchor2PlusNormal2.format(df));*/
        }
    }
//}}}

//{{{ sketchLocalAxes
//##############################################################################
    void sketchLocalAxes(PrintStream out, Map angles, ModelState state)
    {
        out.println("@group {local axes}");
        out.println("@vectorlist {axes} color= brown");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple(axes.strand).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{strand}red "+tip.format(df));
                tip.like(axes.cross).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{cross}green "+tip.format(df));
                tip.like(axes.normal).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{normal}blue "+tip.format(df));
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
        out.println("@labellist {angles} color= peach");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple(axes.strand).add(ca);
                out.println("{strand: "+df.format(axes.angleAlong)+"}red "+tip.format(df));
                tip.like(axes.cross).add(ca);
                out.println("{cross: "+df.format(axes.angleAcross)+"}green "+tip.format(df));
                tip.like(axes.normal).add(ca);
                out.println("{normal: "+df.format(axes.angleNormal)+"}blue "+tip.format(df));
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ sketchPlanes
//##############################################################################
    void sketchPlanes(PrintStream out, Map angles, ModelState state)
    {
        Transform xform = new Transform();
        // It's important that we visit the residues in order.
        ArrayList residues = new ArrayList(angles.keySet());
        Collections.sort(residues);
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            out.println("@group {"+res.getCNIT()+"} animate dominant");
            out.println("@vectorlist {axes} color= brown");
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple();
                for(int i = 0; i < 360; i+=5)
                {
                    tip.like(axes.strand).mult(5);
                    xform.likeRotation(axes.normal, i);
                    xform.transformVector(tip);
                    tip.add(ca);
                    out.println("{"+res+"}P "+ca.format(df));
                    out.println("{plane} "+tip.format(df));
                }
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ printNeighborAngles2
//##############################################################################
    /** Prints along-sheet-oriented stats. */
    void printNeighborAngles2(PrintStream out, String modelName, Collection peptides, Map angles)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }

        out.println("residue,normal,across,along,next-neighbor?,normal,across,along");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            out.print(res.getCNIT()+","+df.format(axes.angleNormal)+","+df.format(axes.angleAcross)+","+df.format(axes.angleAlong));
            Peptide pep = (Peptide) cPeptides.get(res);
            SheetAxes next = (SheetAxes) angles.get(pep.nRes);
            if(pep.nRes !=  null && next != null)
                out.print(","+pep.nRes.getCNIT()+","+df.format(next.angleNormal)+","+df.format(next.angleAcross)+","+df.format(next.angleAlong));
            out.println();
        }
    }
//}}}

//{{{ printNeighborAngles3
//##############################################################################
    /** Prints cross-sheet-oriented stats. */
    void printNeighborAngles3(PrintStream out, String modelName, Collection peptides, Map angles)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }

        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            out.print(modelName+","+res.getCNIT()+","+df.format(axes.angleNormal)+","+df.format(axes.angleAcross)+","+df.format(axes.angleAlong));
            Peptide pep = (Peptide) nPeptides.get(res);
            if(pep.hbondN != null) // has a cross-strand neighbor
            {
                Residue nextRes;
                if(pep.isParallelN) nextRes = pep.hbondN.nRes;
                else                nextRes = pep.hbondN.cRes;
                SheetAxes next = (SheetAxes) angles.get(nextRes);
                if(nextRes !=  null && next != null)
                    out.print(","+nextRes.getCNIT()+","+df.format(next.angleNormal)+","+df.format(next.angleAcross)+","+df.format(next.angleAlong));
            }
            out.println();
        }
    }
//}}}

//{{{ printBetaAromStats
//##############################################################################
    void printBetaAromStats(PrintStream out, Map angles1, Map angles2, Map angles3)
    {
        out.println(
            "model,arom_res,arom_restype,opp_res,opp_restype,"+
            "parallel,chi1,chi2,arom_maxB,opp_maxB,"+
            "arom_edge,opp_edge,"+
            "arom_betaN,arom_betaC,opp_betaN,opp_betaC,"+
            "Caa,Cgba,Cbaa,"+
            "Nward_Caaaa,Cward_Caaaa,Nward_cross_Caaaa,Cward_cross_Caaaa,"+
            "aromCaaa,oppCaaa,"+
            "fray,tilt,"+
            "phi_i-1,psi_i-1,phi_i,psi_i,phi_i+1,psi_i+1,"+
            "tau_i-1,tau,tau_i+1,"+
            "Cab_6CaNormal,Cab_acrossConcav,Cab_alongConcav");
        
        for(BetaArom ba : betaAroms)
        {
            // BetaArom angles
            out.print(ba.modelName+
                ","+ba.aromRes+
                ","+ba.aromRes.getName()+
                ","+ba.oppRes+
                ","+ba.oppRes.getName()+
                
                ","+ba.parallel+
                ","+df.format(ba.chi1)+
                ","+df.format(ba.chi2)+
                ","+df.format(ba.aromMaxB)+
                ","+df.format(ba.oppMaxB)+
                
                ","+ba.aromEdge+
                ","+ba.oppEdge+
                
                ","+ba.aromNumBetaResN+
                ","+ba.aromNumBetaResC+
                ","+ba.oppNumBetaResN+
                ","+ba.oppNumBetaResC+
                
                ","+df.format(ba.cacaDist)+
                ","+df.format(ba.cgcbcaAngle)+
                ","+df.format(ba.cbcacaAngle)+
                
                ","+df.format(ba.nwardTwist)+
                ","+df.format(ba.cwardTwist)+
                ","+df.format(ba.nwardCrossTwist)+
                ","+df.format(ba.cwardCrossTwist)+
                
                ","+df.format(ba.aromCaAngle)+
                ","+df.format(ba.oppCaAngle)+
                
                ","+df.format(ba.fray)+
                ","+df.format(ba.tilt)+
                
                ","+df.format(ba.aromPrevPhi)+
                ","+df.format(ba.aromPrevPsi)+
                ","+df.format(ba.aromPhi)+
                ","+df.format(ba.aromPsi)+
                ","+df.format(ba.aromNextPhi)+
                ","+df.format(ba.aromNextPsi)+
                
                ","+df.format(ba.aromPrevTau)+
                ","+df.format(ba.aromTau)+
                ","+df.format(ba.aromNextTau));
            
            // 6-Calpha normals: SheetAxes angles
            for(Iterator iter = angles1.keySet().iterator(); iter.hasNext(); )
            {
                Residue res = (Residue) iter.next();
                if(res.equals(ba.aromRes))
                {
                    SheetAxes axes = (SheetAxes) angles1.get(res);
                    out.print(","+df.format(axes.angleNormal));
                }
            }
            
            // Across-strand SheetAxes2 angle(s)
            for(Iterator iter = angles2.keySet().iterator(); iter.hasNext(); )
            {
                Residue res = (Residue) iter.next();
                if(res.equals(ba.aromRes))
                {
                    SheetAxes2 axes2 = (SheetAxes2) angles2.get(res);
                    out.print(","+df.format(axes2.cacb_acrossConcavity));
                }
            }
            
            // Along-strand SheetAxes3 angle(s)
            for(Iterator iter = angles3.keySet().iterator(); iter.hasNext(); )
            {
                Residue res = (Residue) iter.next();
                if(res.equals(ba.aromRes))
                {
                    SheetAxes3 axes3 = (SheetAxes3) angles3.get(res);
                    out.print(","+df.format(axes3.cacb_alongConcavity));
                }
            }
            
            out.println();
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
        CoordinateFile cf = new PdbReader().read(new File(filename));
        Model m = cf.getFirstModel();
        ModelState state = m.getState();
        processModel(cf.getIdCode().toLowerCase(), m, state);
    }

    public static void main(String[] args)
    {
        SheetBuilder mainprog = new SheetBuilder();
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
        
        // If no specified res types on opposite strand, allow all of 'em
        if(oppNames == null) 
        {
            oppNames = new TreeSet<String>();
            Scanner s = new Scanner(aaNames).useDelimiter(",");
            while(s.hasNext()) oppNames.add(s.next());
        }
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("SheetBuilder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SheetBuilder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.SheetBuilder");
        System.err.println("Copyright (C) 2004 by Ian W. Davis. All rights reserved.");
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
        if(filename == null) filename = arg;
        else System.out.println("Didn't need "+arg+"; already have file "+filename);
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-betaarom"))
        {
            doBetaAroms = true;
        }
        else if(flag.equals("-oppres") || flag.equals("-opp"))
        {
            if(oppNames == null) oppNames = new TreeSet<String>();
            String[] types = Strings.explode(param, ',');
            for(String type : types) oppNames.add(type);
        }
        else if(flag.equals("-kin"))
        {
            doKin = true;
        }
        else if(flag.equals("-csv"))
        {
            doKin = false;
        }
        else if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

