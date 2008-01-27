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
* <code>SheetBuilder</code> has not yet been documented.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class SheetBuilder //extends ... implements ...
{
//{{{ Constants
    String aaNames="ALA,ARG,ASN,ASP,CYS,GLU,GLN,GLY,HIS,ILE,LEU,LYS,MET,PHE,PRO,SER,THR,TRP,TYR,VAL";
//}}}

//{{{ Variable definitions
//##############################################################################
    String filename               = null;
    boolean doBetaAroms           = false;
    ArrayList<BetaArom> betaAroms = new ArrayList<BetaArom>();
    TreeSet<String> oppResTypes   = null;
    boolean doPrint               = true;
    boolean doKin                 = false;
    boolean verbose               = false;
    Map resToSheetAxes            = null;
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
        if (verbose)
            System.out.println("Processing model "+model+"...");
        
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
        
        Map normals = calcSheetNormals(peptides, model, state);
        // Flesh the normals out into a local coordinate system
        // and measure the Ca-Cb's angle to the normal.
        //Map angles = measureSheetAngles(peptides, normals, state);
        resToSheetAxes = measureSheetAngles(peptides, normals, state);
        
        if (doBetaAroms)
        {
            // This is the stuff Daniel Keedy and Ed Triplett came up with 
            // for a local system for an aromatic residue in a plus (p) rotamer
            // across from a given residue type in a beta sheet.
            // If the -betaarom flag is used, we'll hijack Ian's earlier code
            // to decide which peptides in this pdb deserve beta status then
            // launch into our own stuff.
            addBetaAroms(peptides, model, state);
            if (doKin)
            {
                System.out.println("@text");
                System.out.println("@kinemage 1");
                sketchHbonds(System.out, peptides, state);
                sketchBetaAroms(System.out, state);
            }
        }
        else
        {
            // This is the stuff Ian had already written for SheetBuilder.
            // We're leaving it here as the default.
            
            //System.out.println("@text");
            //printAlongStrandNeighborAngles(System.out, peptides, angles);
            //printCrossStrandNeighborAngles(System.out, modelName, peptides, angles);
            printCrossStrandNeighborAngles(System.out, modelName, peptides, resToSheetAxes);
            //System.out.println("@kinemage 1");
            //sketchHbonds(System.out, peptides, state);
            //sketchNormals(System.out, normals, state);
            //sketchLocalAxes(System.out, angles, state);
            //sketchPlanes(System.out, angles, state);
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
        if (verbose)
            System.out.println("Starting assignSecStruct...");
        
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
                    if (doBetaAroms)
                        continue; // b/c we just want anti-parallel ones for aromatic betas
                    else
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
    }
//}}}

//{{{ addBetaAroms
//##############################################################################
    void addBetaAroms(Collection peptides, Model model, ModelState state)
    {
        if (verbose)
            System.out.println("Starting addBetaArom...");
        
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            // M = this strand, N = opposite strand
            Peptide pepM = (Peptide) iter.next();
            
            // Make sure nothing we need is null
            if (pepM.cRes != null && pepM.nRes != null && 
                pepM.hbondO != null && pepM.prev != null)
            {
                Peptide pepN       = pepM.hbondO;
                Peptide pepMminus1 = pepM.prev;
                if (pepMminus1.hbondN != null && pepN.next != null)
                {
                    Peptide pepNplus1 = pepN.next;
                    // See if we've got an aromatic in a p rotamer on a beta
                    // strand flanked by an HB'ing NH and CO to an Xaa residue
                    // on the opposite strand flanked by a CO and NH which are
                    // HB'ing to the aforementioned NH and CO.
                    // We're arbitrarily going for peptides with such a residue
                    // on the peptide's CO-containing residue (i.e. earlier in 
                    // sequence), not its N-containing residue (i.e. later in 
                    // sequence), so we frame our requirements based on that.
                    if (isPlusArom(pepM.cRes, state) && 
                        rightOppResType(pepN.nRes) &&
                        pepM.isBeta && pepMminus1.isBeta && 
                        pepN.isBeta && pepNplus1.isBeta &&
                        pepNplus1.equals(pepMminus1.hbondN) && 
                        pepN.equals(pepM.hbondO))
                    {
                        try
                        {
                            if (verbose)
                                System.out.println("Let's make a BetaArom for "+pepM.cRes+
                                    " hanging over "+pepN.nRes);
                            
                            //{{{ Basic info about the BetaArom
                                // Make a BetaArom object representing local region and
                                // add it to the list
                                BetaArom ba = new BetaArom();
                                ba.pdb     = filename;
                                ba.aromRes = pepM.cRes;
                                ba.oppRes  = pepN.nRes;
                                
                                // Number of fully beta residues (i.e. both peptides of which the
                                // residue is a part must be beta) on each end of arom & opp residues.
                                // Min = 0.
                                int[] aromNumBetaResNC = getNumBetaRes(pepM.cRes, model, peptides);
                                ba.aromNumBetaResN = aromNumBetaResNC[0];
                                ba.aromNumBetaResC = aromNumBetaResNC[1];
                                int[] oppNumBetaResNC  = getNumBetaRes(pepN.nRes, model, peptides);
                                ba.oppNumBetaResN  = oppNumBetaResNC[0];
                                ba.oppNumBetaResC  = oppNumBetaResNC[1];
                                
                                // Aromatic's Ca(i-1), Ca(i), Ca(i+1), and Cb(i)
                                ArrayList<AtomState> aromCoordsAL = new ArrayList<AtomState>();
                                aromCoordsAL.add(state.get(pepM.cRes.getPrev(model).getAtom(" CA ")));
                                aromCoordsAL.add(state.get(pepM.cRes.getAtom(" CA ")));
                                aromCoordsAL.add(state.get(pepM.cRes.getNext(model).getAtom(" CA ")));
                                aromCoordsAL.add(state.get(pepM.cRes.getAtom(" CB ")));
                                ba.aromCoords = aromCoordsAL;
                                
                                // Opposite residue's Ca(i-1), Ca(i), Ca(i+1), and Cb(i)
                                ArrayList<AtomState> oppCoordsAL = new ArrayList<AtomState>();
                                oppCoordsAL.add(state.get(pepN.nRes.getPrev(model).getAtom(" CA ")));
                                oppCoordsAL.add(state.get(pepN.nRes.getAtom(" CA ")));
                                oppCoordsAL.add(state.get(pepN.nRes.getNext(model).getAtom(" CA ")));
                                if (pepN.nRes.getName().equals("GLY"))
                                    oppCoordsAL.add(null);
                                else
                                    oppCoordsAL.add(state.get(pepN.nRes.getAtom(" CB ")));
                                ba.oppCoords = oppCoordsAL;
                            //}}}
                            
                            //{{{ Angles, dihedrals, & other msrmts
                                // Ed's angle btw Cb(arom)-Ca(arom)-Ca(opp)
                                Triple cbArom = state.get(pepM.cRes.getAtom(" CB "));
                                Triple caArom = state.get(pepM.cRes.getAtom(" CA "));
                                Triple caOpp  = state.get(pepN.nRes.getAtom(" CA "));
                                ba.cbcacaAngle = Triple.angle(cbArom, caArom, caOpp);
                                
                                // Dihedral from Ca(i-1,arom)-Ca(i,arom)-Ca(i,opp)-Ca(i+1,opp)
                                Triple ca_iminus1Arom = state.get(pepM.cRes.getPrev(model).getAtom(" CA "));
                                Triple ca_iplus1Opp   = state.get(pepN.nRes.getNext(model).getAtom(" CA "));
                                ba.nwardDihedral = Triple.dihedral(
                                    ca_iminus1Arom, caArom, caOpp, ca_iplus1Opp);
                                
                                // Dihedral from Ca(i+1,arom)-Ca(i,arom)-Ca(i,opp)-Ca(i-1,opp)
                                Triple ca_iplus1Arom = state.get(pepM.cRes.getNext(model).getAtom(" CA "));
                                Triple ca_iminus1Opp = state.get(pepN.nRes.getPrev(model).getAtom(" CA "));
                                ba.cwardDihedral = Triple.dihedral(
                                    ca_iplus1Arom, caArom, caOpp, ca_iminus1Opp);
                                 
                                // Dihedral from Ca(i-1,arom)-Ca(i,arom)-Ca(i,opp)-Ca(i-1,opp)
                                ba.minusDihedral = Triple.dihedral(
                                    ca_iminus1Arom, caArom, caOpp, ca_iminus1Opp);
                                
                                // Dihedral from Ca(i+1,arom)-Ca(i,arom)-Ca(i,opp)-Ca(i+1,opp)
                                ba.plusDihedral = Triple.dihedral(
                                    ca_iplus1Arom, caArom, caOpp, ca_iplus1Opp);
                                                          
                                // Angle btw Ca(i+1,arom)-Ca(i,arom)-Ca(i+1,arom)
                                ba.aromAngle = Triple.angle(
                                    ca_iplus1Arom, caArom, ca_iminus1Arom);
                                
                                // Angle btw Ca(i+1,opp)-Ca(i,opp)-Ca(i+1,opp)
                                ba.oppAngle = Triple.angle(
                                    ca_iplus1Opp, caOpp, ca_iminus1Opp);
                                
                                // "Fray": roughly speaking, how much the two strands are
                                // pulling apart as viewed from a profile view
                                ba.fray = Math.abs(ba.cwardDihedral-ba.nwardDihedral) - 
                                    Math.abs(ba.aromAngle-ba.oppAngle);
                                
                                // "Tilt": angle btw Ca(arom)(i-1,i,i+1) & Ca(opp)(i-1,i,i+1) planes,
                                // i.e.   /...\   or   \.../   
                                Triple normArom = new Triple().likeNormal(ca_iminus1Arom, caArom, ca_iplus1Arom);
                                Triple normOpp  = new Triple().likeNormal(ca_iminus1Opp, caOpp, ca_iplus1Opp);
                                ba.tilt = normArom.angle(normOpp);
                                
                                // Angle indicating whether CaCb points toward concave or convex side
                                // of sheet (< 90 concave, > 90 convex)
                                ba.cacb_Concavity = calcConcavity(pepM.cRes, state, peptides);
                                
                                // Some other angles/measurements...
                                //???
                                
                            //}}}
                            
                            //{{{ Unused code
                                // OLD VERSION #2 BELOW...
                                // Dot product btw sidechain Ca-Cb vector and local "curvature vector":
                                //    closer to -1 => on convex side (outside) of very curved sheet
                                //    closer to +1 => on concave side (inside) of very curved sheet
                                //if (pepM.hbondN != null)
                                //{
                                //    if (pepM.hbondN.cRes != null && pepM.hbondN.nRes != null)
                                //    {
                                //        // Through pepM's C=O (the "opposite" strand for beta arom purposes)
                                //        Triple caThruO      = state.get(pepM.hbondO.cRes.getAtom(" CA "));
                                //        Triple caThruO_opt2 = state.get(pepM.hbondO.nRes.getAtom(" CA "));
                                //        double distThruO = caArom.distance(caThruO);
                                //        if (caArom.distance(caThruO_opt2) < distThruO)   caThruO = caThruO_opt2;
                                //        // Through pepM's N-H
                                //        Triple caThruN      = state.get(pepM.hbondN.cRes.getAtom(" CA "));
                                //        Triple caThruN_opt2 = state.get(pepM.hbondN.nRes.getAtom(" CA "));
                                //        double distThruN = caArom.distance(caThruN);
                                //        if (caArom.distance(caThruN_opt2) < distThruN)   caThruN = caThruN_opt2;
                                //        // Pulling it together
                                //        Triple cacb = new Triple(cbArom).sub(caArom).unit();
                                //        Triple midpt = new Triple().likeMidpoint(caThruO, caThruN);
                                //        Triple curveVector = midpt.sub(caArom).unit();
                                //        ba.curvatureDot = cacb.dot(curveVector);
                                //    }
                                //}
                                //else ba.curvatureDot = Double.NaN;
                                // OLD VERSION #1 BELOW...
                                //// Note that Ca(i+2) residue-wise goes with pep(i+1) but Ca(i-2) goes
                                //// with pep(i-2) since we originally used the C-containing res of the 
                                //// peptide to find these beta aromatics
                                //Peptide pepMminus2 = pepMminus1.prev;
                                //Peptide pepMplus1  = pepM.next;
                                //if (pepMminus2 != null && pepMplus1 != null)
                                //{
                                //    if (pepMminus1.isBeta && pepMplus1.isBeta)
                                //    {
                                //        Triple cacb = new Triple(cbArom).sub(caArom).unit();
                                //        Triple ca_iminus2Arom = state.get(pepMminus2.cRes.getAtom(" CA "));
                                //        Triple ca_iplus2Arom  = state.get(pepMplus1.nRes.getAtom(" CA "));
                                //        Triple midpt = new Triple().likeMidpoint(ca_iminus2Arom, ca_iplus2Arom);
                                //        Triple curveVector = midpt.sub(caArom).unit();
                                //        ba.curvatureDot = cacb.dot(curveVector);
                                //    }
                                //}
                                //else ba.curvatureDot = Double.NaN;
                            //}}}
                            
                            betaAroms.add(ba);
                        }
                        catch (AtomException ae)
                        {
                            System.err.println("Couldn't make a BetaArom object "+
                                "for this aromatic: "+pepM.nRes);
                        }
                    }
                }
            }
        }
    }
//}}}

//{{{ isPlus, rightOppResType
//##############################################################################
    boolean isPlusArom(Residue res, ModelState state)
    {
        String resName = res.getName();
        if (resName.equals("PHE") || resName.equals("TYR") || 
            resName.equals("HIS") || resName.equals("TRP"))
        {
            try
            {
                AtomState n  = state.get(res.getAtom(" N  "));
                AtomState ca = state.get(res.getAtom(" CA "));
                AtomState cb = state.get(res.getAtom(" CB "));
                AtomState cg = state.get(res.getAtom(" CG "));
                
                double chi1 = Triple.dihedral(n, ca, cb, cg);
                
                if (chi1 > 30 && chi1 < 90)
                    return true;
            }
            catch (AtomException ae)
            {
                System.err.println("Couldn't figure out if "+res+" is p rota or not!");
            }
        }
        return false;
    }

    boolean rightOppResType(Residue res)
    {
        if (verbose)
        {
            System.out.print("Trying to figure out if "+res+" is a ");
            Scanner s = new Scanner(aaNames).useDelimiter(",");
            while (s.hasNext())
                System.out.print(s.next()+" or ");
            System.out.println();
        }
        
        if (oppResTypes.contains(res.getName()))
        {
            if (verbose)
            {
                System.out.print("  Decided that "+res+" is a ");
                Scanner s = new Scanner(aaNames).useDelimiter(",");
                while (s.hasNext())
                    System.out.print(s.next()+" or ");
                System.out.println();
                System.out.println();
            }
            return true;
        }
        else
        {
            if (verbose)
            {
                System.out.print("  Decided that "+res+" is *NOT* a ");
                Scanner s = new Scanner(aaNames).useDelimiter(",");
                while (s.hasNext())
                    System.out.print(s.next()+" or ");
                System.out.println();
                System.out.println();
            }
            return false;
        }
    }
//}}}

//{{{ getNumBetaRes, resIsBeta
//##############################################################################
    int[] getNumBetaRes(Residue res, Model model, Collection peptides)
    {
        int numN = 0;
        int numC = 0;
        
        // N-ward
        boolean endOfStrand = false;
        Residue currRes = res;
        while (!endOfStrand)
        {
            if (currRes.getPrev(model) != null)
            {
                currRes = currRes.getPrev(model);
                if (!resIsBeta(currRes, model, peptides))   endOfStrand = true;
                else                                        numN += 1;
            }
        }
        // C-ward
        endOfStrand = false;
        currRes = res;
        while (!endOfStrand)
        {
            if (currRes.getNext(model) != null)
            {
                currRes = currRes.getNext(model);
                if (!resIsBeta(currRes, model, peptides))   endOfStrand = true;
                else                                        numC += 1;
            }
        }
        if (verbose)
        {
            System.out.println("# res N-ward of '"+res+"': "+numN);
            System.out.println("# res C-ward of '"+res+"': "+numC);
        }
        
        int[] numBetaResNC = new int[2];
        numBetaResNC[0] = numN;
        numBetaResNC[1] = numC;
        return numBetaResNC;
    }

    boolean resIsBeta(Residue res, Model model, Collection peptides)
    {
        Peptide nPep = null; // peptide N-ward to this residue; also contains this residue's N-H
        Peptide cPep = null; // peptide C-ward to this residue; also contains this residue's C=O
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if (res.equals(pep.nRes))   nPep = pep;
            if (res.equals(pep.cRes))   cPep = pep;
        }
        if (nPep.isBeta && cPep.isBeta)   return true;
        return false;
    }
//}}}

//{{{ calcConcavity
//##############################################################################
    /**
    * Returns an angle btw the CaCb vector and the local "concavity vector."
    * Goal: to indicate whether CaCb points toward the concave or the convex 
    * side of the sheet (< 90 concave, > 90 convex).
    */
    double calcConcavity(Residue res, ModelState state, Collection peptides)
    {
        Peptide nPep = null, cPep = null;
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.nRes != null && p.nRes.equals(res))   nPep = p;
            if(p.cRes != null && p.cRes.equals(res))   cPep = p;
        }
        
        if(cPep != null && cPep.hbondN != null && cPep.hbondO != null
        && nPep != null && nPep.hbondN != null && nPep.hbondO != null
        && cPep.isBeta && cPep.hbondN.isBeta && cPep.hbondO.isBeta
        && nPep.isBeta && nPep.hbondN.isBeta && nPep.hbondO.isBeta)
        {
            // Normal1: thru cPep's C=O ("opposite" strand for plus aromatics)
            Collection guidePts1 = new ArrayList();
            guidePts1.add(cPep.midpoint);
            guidePts1.add(nPep.midpoint);
            guidePts1.add(cPep.hbondO.midpoint);
            guidePts1.add(nPep.hbondN.midpoint);
            LsqPlane lsqPlane1 = new LsqPlane(guidePts1);
            Triple normal1 = new Triple(lsqPlane1.getNormal());
            Triple anchor1 = new Triple(lsqPlane1.getAnchor());
            try // to make this normal point toward the middle strand
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple toCa = new Triple(ca).sub(anchor1);
                if(normal1.dot(toCa) < 0)   normal1.neg();
            }
            catch(AtomException ex) {}
            
            // Normal2: thru cPep's N-H
            Collection guidePts2 = new ArrayList();
            guidePts2.add(cPep.midpoint);
            guidePts2.add(nPep.midpoint);
            guidePts2.add(cPep.hbondN.midpoint);
            guidePts2.add(nPep.hbondO.midpoint);
            LsqPlane lsqPlane2 = new LsqPlane(guidePts2);
            Triple normal2 = new Triple(lsqPlane2.getNormal());
            Triple anchor2 = new Triple(lsqPlane2.getAnchor());
            try // to make this normal point toward the middle strand
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple toCa = new Triple(ca).sub(anchor2);
                if(normal2.dot(toCa) < 0)   normal2.neg();
            }
            catch(AtomException ex) {}
            
            // Now that the two side-planes' normals are pointing toward the
            // central strand, we know that their vector sum indicates the
            // *concave* direction of the local beta sheet!
            // Therefore, the angle btw CaCb and the normals' vector sum tells
            // us whether the CaCb vector points toward the concave or convex
            // side of the sheet: < 90 concave, > 90 convex.
            try
            {
                Triple normalsSum = new Triple().likeSum(normal1, normal2).unit();
                AtomState ca = state.get(res.getAtom(" CA "));
                AtomState cb = state.get(res.getAtom(" CB "));
                Triple cacb = new Triple(cb).sub(ca);
                //if (verbose)
                //{
                //    System.out.println("@group {"+res+" concav norms} dominant");
                //    System.out.println("@vectorlist");
                //    System.out.println("{norm1 anchor}P "+anchor1.toString().replace(',', ' '));
                //    System.out.println("{norm1 head} "+anchor1.add(normal1).toString().replace(',', ' '));
                //    System.out.println("{norm2 anchor}P "+anchor2.toString().replace(',', ' '));
                //    System.out.println("{norm2 head} "+anchor2.add(normal2).toString().replace(',', ' '));
                //    System.out.println("{cacb anchor}P "+new Triple(ca).toString().replace(',', ' '));
                //    System.out.println("{cacb head} "+new Triple(cb).toString().replace(',', ' '));
                //}
                return normalsSum.angle(cacb);
            }
            catch(AtomException ex) {} // oh well (e.g. Gly)
        }
        return Double.NaN; 
    }
//}}}

//{{{ calcSheetNormals
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
    Map calcSheetNormals(Collection peptides, Model model, ModelState state)
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

//{{{ measureSheetAngles
//##############################################################################
    /** Returns a map of Residues to SheetAxes */
    Map measureSheetAngles(Collection peptides, Map normals, ModelState state)
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

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
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
            
            //if (pep.nRes != null) try
            //{
            //    Triple nCa = state.get(pep.nRes.getAtom(" CA "));
            //    out.println("{nRes of pep "+pep+"} "+nCa.getX()+" "+nCa.getY()+" "+
            //        nCa.getZ());
            //}
            //catch (AtomException ae)
            //{
            //    out.println("oops...");   
            //}
            
            if(pep.isBeta)
                out.println("{"+pep+"} r=0.3 "+pep.midpoint.format(df));
            else
            {
                // If using -betaaroms flag, we're only interested in aromatics
                // in beta regions and don't even wanna print non-beta peptides
                if (!doBetaAroms)
                    out.println("{"+pep+"} "+pep.midpoint.format(df));
            }
        }
        
        out.println("@vectorlist {N hbonds} color= sky");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.hbondN != null)
            {
                if (pep.isBeta || !doBetaAroms) // again, if -betaaroms, we're picky
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
                if (pep.isBeta || !doBetaAroms) // again, if -betaaroms, we're picky
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

//{{{ sketchBetaAroms
//##############################################################################
    void sketchBetaAroms(PrintStream out, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {p aroms in beta}");
        out.println("@balllist {p aroms in beta} radius= 0.3 color= hotpink");
        try
        {
            for (BetaArom ba : betaAroms)
            {
                AtomState ca = state.get(ba.aromRes.getAtom(" CA "));
                out.println("{"+ba.toString()+" from "+ba.pdb+"} "+
                    df.format(ca.getX())+" "+
                    df.format(ca.getY())+" "+
                    df.format(ca.getZ()) );
            }
        }
        catch (AtomException ae)
        {
            System.err.println("Can't get coords to sketch ball for beta arom CA...");
        }
    }
//}}}

//{{{ sketchNormals
//##############################################################################
    void sketchNormals(PrintStream out, Map normals, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {normals & planes}");
        out.println("@vectorlist {peptides} color= magenta");
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

//{{{ sketchLocalAxes
//##############################################################################
    void sketchLocalAxes(PrintStream out, Map angles, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
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
        DecimalFormat df = new DecimalFormat("0.0###");
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

//{{{ printAlongStrandNeighborAngles
//##############################################################################
    void printAlongStrandNeighborAngles(PrintStream out, Collection peptides, Map angles)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }

        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("residue:normal:across:along:next-neighbor?:normal:across:along");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            out.print(res.getCNIT()+":"+df.format(axes.angleNormal)+":"+df.format(axes.angleAcross)+":"+df.format(axes.angleAlong));
            Peptide pep = (Peptide) cPeptides.get(res);
            SheetAxes next = (SheetAxes) angles.get(pep.nRes);
            if(pep.nRes !=  null && next != null)
                out.print(":"+pep.nRes.getCNIT()+":"+df.format(next.angleNormal)+":"+df.format(next.angleAcross)+":"+df.format(next.angleAlong));
            out.println();
        }
    }
//}}}

//{{{ printCrossStrandNeighborAngles
//##############################################################################
    void printCrossStrandNeighborAngles(PrintStream out, String prefix, Collection peptides, Map angles)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }

        DecimalFormat df = new DecimalFormat("0.0###");
        //out.println("residue:normal:across:along:acrossN-neighbor?:normal:across:along");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            out.print(prefix+":"+res.getCNIT()+":"+df.format(axes.angleNormal)+":"+df.format(axes.angleAcross)+":"+df.format(axes.angleAlong));
            Peptide pep = (Peptide) nPeptides.get(res);
            if(pep.hbondN != null) // has a cross-strand neighbor
            {
                Residue nextRes;
                if(pep.isParallelN) nextRes = pep.hbondN.nRes;
                else                nextRes = pep.hbondN.cRes;
                SheetAxes next = (SheetAxes) angles.get(nextRes);
                if(nextRes !=  null && next != null)
                    out.print(":"+nextRes.getCNIT()+":"+df.format(next.angleNormal)+":"+df.format(next.angleAcross)+":"+df.format(next.angleAlong));
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
        if (verbose)
            System.out.println("Starting Main method...");
        
        // Load model group from PDB files
        File file = new File(filename);
        LineNumberReader in = new LineNumberReader(new FileReader(file));
        PdbReader pdbReader = new PdbReader();
        CoordinateFile cf = pdbReader.read(in);
        
        Model m = cf.getFirstModel();
        ModelState state = m.getState();
        processModel(cf.getIdCode(), m, state);
        
        if (doPrint)
            printBetaAromStats(System.out);
        
    }

    public static void main(String[] args)
    {
        SheetBuilder mainprog = new SheetBuilder();
        try
        {
            mainprog.parseArguments(args);
            //System.out.println("Finished parsing args...");
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

//{{{ printBetaAromStats
//##############################################################################
    void printBetaAromStats(PrintStream out)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("pdb:arom_res:arom_res_type:opp_res:opp_res_type:"+
            "aromNumBetaResN:aromNumBetaResC:oppNumBetaResN:oppNumBetaResC:"+
            "CbCaCa:nwardDhdrl:cwardDhdrl:minusDhdrl:plusDhdrl:"+
            "aromSimpAng:oppSimpAng:"+
            "fray:tilt:"+
            "CaCb_Concavity:"+
            "CaCb_6CaNormal:CaCb_Across:CaCb_Along");
            //"cb(arom)_ca(arom)_ca(opp):"+
            //"ca(arom,i-1)_ca(arom,i)_ca(opp,i)_ca(opp,i+1):"+
            //"ca(arom,i+1)_ca(arom,i)_ca(opp,i)_ca(opp,i-1):"+
            //"ca(arom,i-1)_ca(arom,i)_ca(opp,i)_ca(opp,i-1):"+
            //"ca(arom,i+1)_ca(arom,i)_ca(opp,i)_ca(opp,i+1):"+
            //"arom_simple_angle:"+
            //"opp_simple_angle:"+
            
        for (BetaArom ba : betaAroms)
        {
            // DAK's beta aromatic angles
            out.print(ba.pdb+
                ":"+ba.aromRes+
                ":"+ba.aromRes.getName()+
                ":"+ba.oppRes+
                ":"+ba.oppRes.getName()+
                ":"+ba.aromNumBetaResN+
                ":"+ba.aromNumBetaResC+
                ":"+ba.oppNumBetaResN+
                ":"+ba.oppNumBetaResC+
                ":"+ba.cbcacaAngle+
                ":"+ba.nwardDihedral+
                ":"+ba.cwardDihedral+
                ":"+ba.minusDihedral+
                ":"+ba.plusDihedral+
                ":"+ba.aromAngle+
                ":"+ba.oppAngle+
                ":"+ba.fray+
                ":"+ba.tilt);
            if (!Double.isNaN(ba.cacb_Concavity)) out.print(":"+ba.cacb_Concavity);
            else                                  out.print(":");
            
            // IWD's sheet axis angles
            String sheetAxesAngles = ":::";
            for(Iterator iter = resToSheetAxes.keySet().iterator(); iter.hasNext(); )
            {
                Residue res = (Residue) iter.next();
                if (res.equals(ba.aromRes))
                {
                    SheetAxes axes = (SheetAxes) resToSheetAxes.get(res);
                    sheetAxesAngles = ":"+axes.angleNormal+
                                      ":"+axes.angleAcross+
                                      ":"+axes.angleAlong;
                }
            }
            out.println(sheetAxesAngles);
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
        if (oppResTypes == null) 
        {
            oppResTypes = new TreeSet<String>();
            Scanner s = new Scanner(aaNames).useDelimiter(",");
            while (s.hasNext())
                oppResTypes.add(s.next());
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
        else if(flag.equals("-betaarom"))
        {
            doBetaAroms = true;
        }
        else if(flag.equals("-oppres"))
        {
            // Consider only these res types on opposite strand
            if (oppResTypes != null)
            {
                System.err.println("Can't use -oppres when opposite residue types"+
                    " already specified!");
                // compiler will continue to end of method now...
            }
            else
            {
                oppResTypes = new TreeSet<String>();
                if (param != null)
                {
                    Scanner s = new Scanner(param).useDelimiter(",");
                    while (s.hasNext())
                    {
                        String elem = s.next();
                        oppResTypes.add(elem);
                    }
                }
            }
        }
        else if(flag.equals("-notoppres"))
        {
            // Remove these res types from consideration on opposite strand
            if (oppResTypes != null)
            {
                System.err.println("Can't use -notoppres when opposite residue types"+
                    " already specified!");
                // compiler will continue to end of method now...
            }
            else
            {
                // First add all res types to the set ...
                if (oppResTypes == null) 
                {
                    oppResTypes = new TreeSet<String>();
                    Scanner s = new Scanner(aaNames).useDelimiter(",");
                    while (s.hasNext())
                        oppResTypes.add(s.next());
                }
                // ... then remove the unwanted ones.
                if (param != null)
                {
                    Scanner s = new Scanner(param).useDelimiter(",");
                    while (s.hasNext())
                    {
                        String elem = s.next();
                        oppResTypes.remove(elem);
                    }
                }
            }
        }
        else if(flag.equals("-kin"))
        {
            doKin = true;
            doPrint = false;
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

