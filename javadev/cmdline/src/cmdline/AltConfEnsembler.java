// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.util.Strings;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.data.*;
import molikin.logic.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
//}}}
/**
* <code>AltConfEnsembler</code> makes a multi-MODEL ensemble based on 
* all possible combinations of independent alternate conformation networks.
*
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Nov 29 2011
*/
/*
TODO:
   Probe-based concept of interaction between networks
   propose alt label swaps in orig structure based on A-A vs. A-B vs. B-B dists 
   graph or tree data structure?
*/
public class AltConfEnsembler //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    DecimalFormat df = new DecimalFormat("#.###");
    double pdbPrecision = 1000;
//}}}

//{{{ Variable definitions
//##############################################################################
    // INPUT
    boolean            verbose         = false;
    File               inFile          = null;
    String             pdbOut          = null;
    String             kinOut          = null;
    String             chain           = null;
    boolean            useHets         = false;
    /*double             minMinDist      = 3.0; // rough guess at vdW radius + vdW radius... :\*/
    int                maxSize         = 1000;
    int                actualSize      = 0; // only incremented if expected size < max allowable size
    
    // OUTPUT
    HashSet            allRes          = null; // including hets, e.g. waters
    HashSet            altRes          = null; // ditto
    TreeSet            altLabels       = null;
    Map                networkIndices  = null;
    BallAndStickLogic  logic           = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AltConfEnsembler()
    {
        super();
    }
//}}}

//{{{ defineNetworks
//##############################################################################
    /**
    * Finds linear-in-sequence alternate networks.
    */
    public ArrayList defineNetworks(Model model)
    {
        // For stats later:
        allRes = new HashSet<Residue>();
        altRes = new HashSet<Residue>();
        altLabels = new TreeSet<String>();
        
        ArrayList<AltConfNetwork> linNets = new ArrayList<AltConfNetwork>();
        AltConfNetwork linNet = new AltConfNetwork();
        for(Iterator rIter = model.getResidues().iterator(); rIter.hasNext(); )
        {
            Residue res = (Residue) rIter.next();
            if(!useHets)
            {
                boolean resHasHets = false;
                for(Iterator aIter = res.getAtoms().iterator(); aIter.hasNext(); )
                {
                    Atom a = (Atom) aIter.next();
                    if(a.isHet())
                    {
                        resHasHets = true;
                        break; // out of atom loop
                    }
                }
                //if(resHasHets) continue; // to next residue
            }
            allRes.add(res);
            if(chain != null && !res.getChain().equals(chain)) continue; // to next residue
            
            Set resAlts = altConfsOf(res, model);
            if(resAlts.size() >= 2)
            {
                // Extend current linear network
                linNet.addResidue(res);
                altRes.add(res);
                for(Iterator sIter = resAlts.iterator(); sIter.hasNext(); )
                {
                    String alt = (String) sIter.next();
                    linNet.addAlt(alt);
                    altLabels.add(alt);
                }
            }
            else
            {
                // End current linear network (if we're in one)
                if(!linNet.getResidues().isEmpty())
                {
                    linNets.add(linNet);
                    if(verbose) System.err.println("Made linear network: "+linNet);
                }
                linNet = new AltConfNetwork();
            }
        }
        
        return linNets;
    }
//}}}

//{{{ mergeNetworks
//##############################################################################        
    /**
    * Recursively merges alternate networks that "interact".
    * Modifies <code>networks</code>, so provide a copy or clone
    * of your input list if you don't want it messed with!
    */
    public ArrayList mergeNetworks(ArrayList networks, Model model)
    {
        // Find two networks to merge
        boolean foundMerger = false;
        AltConfNetwork netGettingSubsumed = null;
        for(int i = 0; i < networks.size(); i++)
        {
            for(int j = 0; j < networks.size(); j++)
            {
                AltConfNetwork net1 = (AltConfNetwork) networks.get(i);
                AltConfNetwork net2 = (AltConfNetwork) networks.get(j);
                if(net1 == net2) continue;
                /*double minDist = closestApproach(net1, net2, model);
                if(minDist < minMinDist)*/
                if(net1.interactsWith(net2, model))
                {
                    foundMerger = true;
                    // It's arbitrary which network subsumes which, 
                    // but we'll let the first one subsume the second one
                    // so the final list is (more likely to be?) in sequence order
                    netGettingSubsumed = net2;
                    if(verbose)
                        System.err.println("\nNetwork "+net1+"\neats    "+net2);
                    net1.subsume(net2);
                    if(verbose)
                        System.err.println("making  "+net1);
                        /*+" (dist: "+df.format(minDist)+" < "+minMinDist+")");*/
                    break;
                }
            }
            if(foundMerger) break;
        }
        
        // If we found a merger, get rid of the subsumed network then recurse
        if(foundMerger && netGettingSubsumed != null)
        {
            networks.remove(netGettingSubsumed);
            mergeNetworks(networks, model);
        }
            // Otherwise, we're done!
        if(verbose)
        {
            System.err.println("\nFinal list of networks:");
            for(int i = 0; i < networks.size(); i++) System.err.println(networks.get(i));
            System.err.println();
        }
        return networks;
    }
//}}}

//{{{ altConfsOf
//##############################################################################
    /**
    * Returns the single-character labels of true alternate conformations
    * in the given residue across all non-' ' states of the given model.
    * Requirements: different alt conf labels & different XYZ positions.
    */
    public Set altConfsOf(Residue res, Model model)
    {
        Set altLabels = new HashSet<String>();
        
        for(Iterator aIter = res.getAtoms().iterator(); aIter.hasNext(); )
        {
            Atom a = (Atom) aIter.next();
            CheapSet aXYZ       = new CheapSet(new IdentityHashFunction());
            CheapSet aAltLabels = new CheapSet(new IdentityHashFunction());
            for(Iterator sIter = model.getStates().keySet().iterator(); sIter.hasNext(); )
            {
                String stateLabel = (String) sIter.next();
                ModelState state = model.getState(stateLabel);
                try
                {
                    AtomState as = state.get(a);
                    if(!as.getAltConf().equals(" "))// || as.getOccupancy() < 1.0)
                    {
                        Triple xyz = new Triple(
                            Math.round(as.getX()*pdbPrecision)/pdbPrecision, 
                            Math.round(as.getY()*pdbPrecision)/pdbPrecision, 
                            Math.round(as.getZ()*pdbPrecision)/pdbPrecision);
                        aXYZ.add(xyz);
                        aAltLabels.add(as.getAltConf());
                    }
                }
                catch(AtomException ex) {}
            }
            
            if(aXYZ.size() > 1 && aAltLabels.size() > 1)
            {
                for(Iterator sIter = aAltLabels.iterator(); sIter.hasNext(); )
                {
                    String altLabel = (String) sIter.next();
                    altLabels.add(altLabel);
                }
            }
        }
        
        return altLabels;
    }
//}}}

//{{{ closestApproach
//##############################################################################
    /**
    * Determines the closest alt-conf-atom to alt-conf-atom distance 
    * between the two supplied alt conf networks,
    * regardless of whether those atoms' alt conf labels actually match.
    * If it's small enough, the two networks are probably coupled.
    * It is NOT required that the input PDB alt labels are correct,
    * because even if they're not, we can tell that the two networks interact;
    * however, the resulting alt conf ensemble will reflect 
    * any incorrect alt labels in the input PDB.
    */
    public double closestApproach(AltConfNetwork network1, AltConfNetwork network2, Model model)
    {
        double minDist = Double.POSITIVE_INFINITY;
        
        // For atom in network 1
        for(Iterator rIter1 = network1.getResidues().iterator(); rIter1.hasNext(); )
        {
            Residue res1 = (Residue) rIter1.next();
            for(Iterator aIter1 = res1.getAtoms().iterator(); aIter1.hasNext(); )
            {
                Atom a1 = (Atom) aIter1.next();
                
                // For atom in network 2
                for(Iterator rIter2 = network2.getResidues().iterator(); rIter2.hasNext(); )
                {
                    Residue res2 = (Residue) rIter2.next();
                    for(Iterator aIter2 = res2.getAtoms().iterator(); aIter2.hasNext(); )
                    {
                        Atom a2 = (Atom) aIter2.next();
                        
                        // Update minimum atom-atom distance based on all alt conf state combos
                        for(Iterator sIter1 = model.getStates().keySet().iterator(); sIter1.hasNext(); )
                        {
                            String stateLabel1 = (String) sIter1.next();
                            if(!network1.getAlts().contains(stateLabel1)) continue;
                            ModelState state1 = model.getState(stateLabel1);
                            for(Iterator sIter2 = model.getStates().keySet().iterator(); sIter2.hasNext(); )
                            {
                                String stateLabel2 = (String) sIter2.next();
                                if(!network2.getAlts().contains(stateLabel2)) continue;
                                if(stateLabel2.equals(stateLabel1)) continue;
                                ModelState state2 = model.getState(stateLabel2);
                                try
                                {
                                    AtomState as1 = state1.get(a1);
                                    AtomState as2 = state1.get(a2);
                                    double dist = as1.distance(as2);
                                    if(dist < minDist) minDist = dist;
                                }
                                catch(AtomException ex) {}
                            }
                        }
                    }
                }
            }
        } // ... Phew!
        
        return minDist;
    }
//}}}

//{{{ theoreticalSize
//##############################################################################
    public long theoreticalSize(ArrayList networks)
    {
        long count = 0;
        for(int i = 0; i < networks.size(); i++)
        {
            AltConfNetwork network = (AltConfNetwork) networks.get(i);
            int numAlts = network.getAlts().size();
            if(count == 0) count = (long) numAlts;
            else if(count * numAlts >= Long.MAX_VALUE) return -1;
            else count *= numAlts;
        }
        return count;
    }
//}}}

//{{{ prepForKinOutput
//##############################################################################
    public void prepForKinOutput(ArrayList networks, Model model)
    {
        // Assign each residue to an alt conf network 
        // labeled by a (basically arbitary) integer index
        // which could later get mapped to e.g. color
        this.networkIndices = new HashMap<Residue,Integer>(); 
        int maxNetworkIndexSoFar = 0;
        for(Iterator rIter = model.getResidues().iterator(); rIter.hasNext(); )
        {
            Residue res = (Residue) rIter.next();
            boolean inNetwork = false;
            for(Iterator nIter = networks.iterator(); nIter.hasNext(); )
            {
                AltConfNetwork network = (AltConfNetwork) nIter.next();
                if(network.getResidues().contains(res))
                {
                    int networkIndex = networks.indexOf(network);
                    this.networkIndices.put(res, networkIndex);
                    inNetwork = true;
                }
            }
            if(!inNetwork)
            {
                this.networkIndices.put(res, -1);
            }
        }
        
        // Set up Molikin Logic
        this.logic = new BallAndStickLogic();
        this.logic.doProtein       = true;
        this.logic.doNucleic       = true;
        this.logic.doHets          = true;
        this.logic.doMetals        = true;
        this.logic.doWater         = true;
        this.logic.doVirtualBB     = true;
        this.logic.doMainchain     = true;
        this.logic.doSidechains    = true;
        this.logic.doHydrogens     = true;
        this.logic.doDisulfides    = true;
        this.logic.doBallsOnCarbon = false;
        this.logic.doBallsOnAtoms  = false;
        //this.logic.colorBy       = BallAndStickLogic.COLOR_BY_MC_SC;
        this.logic.colorBy         = BallAndStickLogic.COLOR_BY_ALT_NETWORK;
        this.logic.altConfNetworks = networkIndices;
        this.logic.doLigate        = true; // use terminal residues only for inter-residue bonds
    }
//}}}

//{{{ addToEnsemble
//##############################################################################
    /**
    * Adds each alternate conformation for each network onto a growing list
    * in a recursive & branching fashion.
    */
    public void addToEnsemble(ArrayList altsAssigned, ArrayList networks, ArrayList altCombos)
    {
        if(altsAssigned.size() == networks.size())
        {
            // Exit condition: we've made a complete combination of alt networks
            String[] altCombo = (String[]) altsAssigned.toArray(new String[altsAssigned.size()]);
            altCombos.add(altCombo);
        }
        else
        {
            // Continue recursion...
            AltConfNetwork network = (AltConfNetwork) networks.get(altsAssigned.size());
            for(Iterator iter = network.getAlts().iterator(); iter.hasNext(); )
            {
                String alt = (String) iter.next();
                ArrayList<String> newAltsAssigned = (ArrayList<String>) altsAssigned.clone();
                newAltsAssigned.add(alt); // alt chosen for this network of this ensemble member
                addToEnsemble(newAltsAssigned, networks, altCombos); // now for next network
            }
        }
    }
//}}}

//{{{ getAltConfGrayCodes
//##############################################################################
    /**
    * Generates a "standard" n-ary Gray code sequence, skipping the implied 
    * alt combinations that can't actually be made, and continuing 'til we hit 
    * the expected ensemble size.
    * Results in some visual jumps where >1 alt change at once, 
    * but should at least always result in possible models.
    */
    public ArrayList<String[]> getAltConfGrayCodes(ArrayList networks, ArrayList original, long expectedSize)
    {
        ArrayList<String[]> grayCodes = new ArrayList<String[]>();
        int value = 0; // input value for conversion to the "standard" n-ary Gray code sequence
        while(grayCodes.size() < expectedSize)
        {
            int[] intGrayCode = grayCode(value, networks.size());
            String[] grayCode = new String[intGrayCode.length];
            
            // Workaround in case a structure has non-consecutive alts,
            // like A,B,N,O in 2ov0...  Seems to results in some visual jumps
            // where >1 alt change at once, but at least we complete in a 
            // reasonable runtime now.  It doesn't seem worth toiling much more
            // than this over such a fringe case.
            String altAlphabet = "";
            for(Iterator iter = altLabels.iterator(); iter.hasNext(); )
            {
                // altLabels is a TreeSet, so it should spit out its elements in order
                String alt = (String) iter.next();
                altAlphabet += alt;
            }
            for(int i = 0; i < grayCode.length; i++)
                grayCode[i] = altAlphabet.substring(intGrayCode[i], intGrayCode[i]+1);
            
            if(verbose)
            {
                System.err.print(value+" -> ");
                for(int i = 0; i < intGrayCode.length; i++) System.err.print(intGrayCode[i]);
                System.err.print(" -> ");
                for(int i = 0; i < grayCode.length; i++) System.err.print(grayCode[i]);
            }
            
            if(isValidAltCombo(grayCode, networks))
            {
                grayCodes.add(grayCode);
                if(verbose) System.err.println(" - GOOD!");
            }
            else if(verbose) System.err.println(" - bad");
            
            value++;
        }
        return grayCodes;
    }
//}}}

//{{{ grayCode
//##############################################################################
    /**
    * Uses the n-ary Gray code algorithm from the Gray code Wikipedia page
    * to convert a given integer (in our case representing a combination of 
    * alternate conformation labels) into a new Gray code version.
    * Quote: "Iterating through a sequence of combinations would result in 
    * a sequence of Gray codes in which only one digit changes at a time."
    */
    public int[] grayCode(int value, int digits)
    {
        int base = altLabels.size(); // # of unique alts: the n in n-ary Gray code
        
        int[] baseN = new int[digits];
        int[] gray = new int[digits];
        
        // 109 becomes [9,0,1]
        int tempvalue = value;
        for(int i = 0; i < digits; i++)
        {
            baseN[i] = tempvalue % base;
            tempvalue /= base;
        }
        
        // Do the "Gray magic"
        int shift = 0;
        for(int i = digits - 1; i >= 0; i--)
        {
            gray[i] = (baseN[i] - shift) % base;
            shift += gray[i] - base;
        }
        
        return gray;
    }
//}}}

//{{{ isValidAltCombo
//##############################################################################
    /**
    * Reports whether or not the given sequence of alt labels
    * is consistent with the alt labels observed at all alt networks.
    */
    public boolean isValidAltCombo(String[] altCombo, ArrayList networks)
    {
        for(int i = 0; i < altCombo.length; i++)
        {
            AltConfNetwork network = (AltConfNetwork) networks.get(i);
            if(!network.getAlts().contains(altCombo[i])) return false;
        }
        return true;
    }
//}}}

//{{{ getStateForAltCombo
//##############################################################################
    public ModelState getStateForAltCombo(String[] altCombo, ArrayList networks, Model model)
    {
        ModelState newState = new ModelState(model.getState(" ")); // template
        for(int j = 0; j < altCombo.length; j++)
        {
            String alt = altCombo[j];
            AltConfNetwork network = (AltConfNetwork) networks.get(j);
            ModelState altState = model.getState(alt);
            // We don't want to add the full complement of alt conf atoms 
            // from this ^ alt state, just the ones in this particular network
            for(Iterator rIter = network.getResidues().iterator(); rIter.hasNext(); )
            {
                Residue res = (Residue) rIter.next();
                for(Iterator aIter = res.getAtoms().iterator(); aIter.hasNext(); )
                {
                    Atom a = (Atom) aIter.next();
                    try
                    {
                        AtomState as = altState.get(a);
                        //if(as.getAltConf().equals(alt))
                        // Allow for e.g. alt 'A' backbone atoms that form
                        // the branching point for alt 'C' sidechain atoms.
                        // Problem: if alt 'C' sidechain actually branches
                        // off alt 'B' backbone, not alt 'A', moldb2 will 
                        // typically select alt 'A' backbone anyway as the 
                        // "default" state, resulting in an unrealistic 'A' 
                        // backbone, 'C' sidechain model with bad geometry...
                        // But fixing that is beyond the responsibility of this class.
                        if(!as.getAltConf().equals(" "))
                        {
                            // Make a clone of the original atom state
                            // so we can make it LOOK like the default state
                            // without actually messing with the original,
                            // then add it to the hybrid state we're creating
                            AtomState as2 = (AtomState) as.clone();
                            as2.setAltConf(" ");
                            newState.addOverwrite(as2);
                        }
                    }
                    catch(AtomException ex) {}
                }
            }
        }
        return newState;
    }
//}}}

//{{{ printPdbEnsemble
//##############################################################################
    public void printPdbEnsemble(ArrayList altCombos, ArrayList networks, Model model) throws IOException
    {
        OutputStream output = new FileOutputStream(pdbOut, false); 
        
        for(int i = 0; i < altCombos.size(); i++)
        {
            // Prepare coordinates for just this alt combo
            String[] altCombo = (String[]) altCombos.get(i);
            ModelState newState = getStateForAltCombo(altCombo, networks, model);
            
            // Package them
            String altComboString = "";
            for(int j = 0; j < altCombo.length; j++) altComboString += altCombo[j];
            if(verbose) System.err.println("Printing " + altComboString);
            Model newModel = (Model) model.clone(); // use original model for template
            HashMap newStates = new HashMap();
            newStates.put(" ", newState); // treat new state as default for new model
            newModel.setStates(newStates);
            CoordinateFile ensembleMember = new CoordinateFile();
            ensembleMember.add(newModel);
            actualSize++;
            
            // Print them
            if(actualSize == 2) output = new FileOutputStream(pdbOut, true); // start appending
            PrintWriter printWriter = new PrintWriter(output);
            if(printWriter == null) throw new IOException("*** Error writing PDB!");
            printWriter.println("MODEL     "+Strings.forceRight(""+actualSize, 4));
            printWriter.flush();
            PdbWriter pdbWriter = new PdbWriter(output);
            if(pdbWriter == null) throw new IOException("*** Error writing PDB!");
            pdbWriter.writeCoordinateFile(ensembleMember);
            output.flush();
            pdbWriter.close();
        }
        
        output.close();
    }
//}}}
    
//{{{ printKinEnsemble
//##############################################################################
    public void printKinEnsemble(ArrayList altCombos, ArrayList networks, Model model) throws IOException
    {
        OutputStream output = new FileOutputStream(kinOut, false); 
        PrintWriter printWriter = new PrintWriter(output);
        if(printWriter == null) throw new IOException("*** Error writing kin!");
        printWriter.println("@kinemage {alt ensem "+inFile.getName()+"}");
        output = new FileOutputStream(kinOut, true); // start appending
        printWriter.println("@master {all states}");
        printWriter.println("@group {parts list} dominant nobutton off");
        
        // Template (residues not in networks)
        ModelState state = model.getState(" ");
        Collection stateCollection = new ArrayList<ModelState>();
        stateCollection.add(state);
        
        TreeSet residueSet = new TreeSet();
        for(Iterator rIter = model.getResidues().iterator(); rIter.hasNext(); )
            residueSet.add( (Residue)rIter.next() );
        for(int i = 0; i < networks.size(); i++)
        {
            AltConfNetwork network = (AltConfNetwork) networks.get(i);
            for(Iterator rIter = network.getResidues().iterator(); rIter.hasNext(); )
                residueSet.remove( (Residue)rIter.next() );
        }
        
        // This is a real pain: if we use Molikin to print different residues 
        // at different times, we get gaps where the peptide (or phosphodiester)
        // bonds should be.  So, I've written a kludge that lets you use the 
        // first and last residues of a set given to Molikin ONLY for single-bond
        // ligatures between residues -- the rest of those residues are totally ignored.
        // Yes, I recognize how kludgy this is...  BUT it shouldn't affect anything 
        // else that uses Molikin without this ligation option.
        TreeSet ligatingResidues = new TreeSet();
        for(Iterator rIter = residueSet.iterator(); rIter.hasNext(); )
        {
            Residue res = (Residue) rIter.next();
            Residue prevRes = res.getPrev(model);
            Residue nextRes = res.getNext(model);
            if(prevRes != null && !residueSet.contains(prevRes))
                ligatingResidues.add(prevRes);
            if(nextRes != null && !residueSet.contains(nextRes))
                ligatingResidues.add(nextRes);
        }
        for(Iterator rIter = ligatingResidues.iterator(); rIter.hasNext(); )
            residueSet.add( (Residue)rIter.next() );
        
                
        printWriter.println("@subgroup {template}");
        // logic is already prepped with knowledge of networks,
        // so each residue should be colored "properly"
        this.logic.printKinemage(printWriter, model, 
            stateCollection, residueSet, inFile.getName(), "white");
        
        // Each state of each independent network
        for(int i = 0; i < networks.size(); i++)
        {
            AltConfNetwork network = (AltConfNetwork) networks.get(i);
            for(Iterator nIter = network.getAlts().iterator(); nIter.hasNext(); )
            {
                String alt = (String) nIter.next();
                state = model.getState(alt);
                stateCollection = new ArrayList<ModelState>();
                stateCollection.add(state);
                
                residueSet = new TreeSet();
                for(Iterator rIter = network.getResidues().iterator(); rIter.hasNext(); )
                    residueSet.add( (Residue)rIter.next() );
                
                ligatingResidues = new TreeSet();
                for(Iterator rIter = residueSet.iterator(); rIter.hasNext(); )
                {
                    Residue res = (Residue) rIter.next();
                    Residue prevRes = res.getPrev(model);
                    Residue nextRes = res.getNext(model);
                    if(prevRes != null && !residueSet.contains(prevRes))
                        ligatingResidues.add(prevRes);
                    if(nextRes != null && !residueSet.contains(nextRes))
                        ligatingResidues.add(nextRes);
                }
                for(Iterator rIter = ligatingResidues.iterator(); rIter.hasNext(); )
                    residueSet.add( (Residue)rIter.next() );
                
                String instance = (i+1) + alt;
                printWriter.println("@subgroup {"+instance+"}");
                this.logic.printKinemage(printWriter, model, 
                    stateCollection, residueSet, inFile.getName(), "white");
            }
        }
        
        // Instances of those parts in various combinations
        for(int i = 0; i < altCombos.size(); i++)
        {
            String[] altCombo = (String[]) altCombos.get(i);
            
            String altComboString = "";
            for(int j = 0; j < altCombo.length; j++) altComboString += altCombo[j];
            if(verbose) System.err.println("Printing " + altComboString);
            printWriter.println("@group {"+altComboString+"} dominant animate master= {all states}");
            
            printWriter.println("@subgroup {} instance= {template}");
            for(int j = 0; j < altCombo.length; j++)
            {
                String instance = (j+1) + altCombo[j];
                printWriter.println("@subgroup {} instance= {"+instance+"}");
            }
        }
        
        output.flush();
        printWriter.close();
    }
//}}}

//{{{ printSimpleKin
//##############################################################################
    void printSimpleKin(Model model) throws IOException
    {
        OutputStream output = new FileOutputStream(kinOut, false);
        PrintWriter printWriter = new PrintWriter(output);
        if(printWriter == null) throw new IOException("*** Error writing kin!");
        printWriter.println("@kinemage {alt networks "+inFile.getName()+"}");
        
        printWriter.println("@group {alt networks "+inFile.getName()+"} dominant animate");
        
        Set residueSet = new TreeSet<Residue>();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
            residueSet.add( (Residue)iter.next() );
        
        // logic is already prepped with knowledge of networks,
        // so each residue should be colored "properly"
        this.logic.printKinemage(printWriter, model, residueSet, inFile.getName(), "white");
        
        output.flush();
        printWriter.close();
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        // Read input
        if(inFile == null) throw new IllegalArgumentException(
            "Must provide input PDB file!");
        PdbReader pdbReader = new PdbReader();
        CoordinateFile coordFile = pdbReader.read(inFile);
        Model model = coordFile.getFirstModel();
        
        // Define alt conf networks
        ArrayList linNets = defineNetworks(model);
        ArrayList networks = mergeNetworks((ArrayList)linNets.clone(), model);
        long linExpectedSize = theoreticalSize(linNets);
        long expectedSize = theoreticalSize(networks);
        
        // Prepare and output coordinates
        if(kinOut != null)
            prepForKinOutput(networks, model);
        if(expectedSize <= maxSize)
        {
            // Create alt conf ensemble by enumerating combinations
            // and converting to Gray code
            ArrayList<String[]> original = new ArrayList<String[]>();
            addToEnsemble(new ArrayList<String>(), networks, original);
            ArrayList<String[]> grayCode = getAltConfGrayCodes(networks, original, expectedSize);
            
            // Print PDB or kin of full, multi-model alt conf ensemble
            if(pdbOut != null)
                printPdbEnsemble(grayCode, networks, model);
            else if(kinOut != null)
                printKinEnsemble(grayCode, networks, model);
            else
                System.err.println("Not printing PDB or kin because "
                    +"neither -pdb=out.pdb nor -kin=out.kin provided");
        }
        else // too many alt combinations
        {
            if(pdbOut != null)
            {
                System.err.println("Not printing PDB because too many models ("
                    +expectedSize+" > "+maxSize+")");
            }
            else if(kinOut != null)
            {
                System.err.println("Printing single-group instead of full-ensemble kin");
                System.err.println("  because too many models ("+expectedSize+" > "+maxSize+")");
                printSimpleKin(model);
            }
        }
        
        // Report alt conf stats
        System.out.println("file,num_alts,"
            +"num_res,num_alt_res,"
            +"num_lin_alt_net,num_alt_net,"
            +"num_lin_alt_mdl,num_alt_mdl");
        System.out.println(inFile.getName()+","+altLabels.size()+","
            +allRes.size()+","+altRes.size()+","
            +linNets.size()+","+networks.size()+","
            +linExpectedSize+","+expectedSize);
    }

    public static void main(String[] args)
    {
        AltConfEnsembler mainprog = new AltConfEnsembler();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.err.println();
            System.err.println("*** Error in execution: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("AltConfEnsembler.help");
            if(is == null)
            {
                System.err.println("\n*** Usage: java AltConfEnsembler in.pdb -pdb=alt_ensem.pdb ***\n");
            }
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.AltConfEnsembler");
        System.err.println("Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.");
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
        if(inFile == null) inFile = new File(arg);
        else System.err.println("Can't use additional argument: "+arg);
    }
    
    void interpretFlag(String flag, String param)
    {
        try
        {
            if(flag.equals("-help") || flag.equals("-h"))
            {
                showHelp(true);
                System.exit(0);
            }
            else if(flag.equals("-verbose") || flag.equals("-v"))
            {
                verbose = true;
            }
            else if(flag.equals("-pdb"))
            {
                pdbOut = param;
            }
            else if(flag.equals("-kin"))
            {
                kinOut = param;
            }
            else if(flag.equals("-chain"))
            {
                chain = param;
            }
            else if(flag.equals("-hets"))
            {
                useHets = true;
            }
            else if(flag.equals("-nohets"))
            {
                useHets = false;
            }
            /*else if(flag.equals("-dist"))
            {
                try
                { minMinDist = Double.parseDouble(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format "+param+" as a double!"); }
            }*/
            else if(flag.equals("-maxensemblesize") || flag.equals("-maxensemsize"))
            {
                try
                { maxSize = Integer.parseInt(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format "+param+" as an integer!"); }
            }
            else if(flag.equals("-dummy_option"))
            {
                // handle option here
            }
            else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
        }
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("Non-number argument to "+flag+": '"+param+"'"); }
    }
//}}}
}//class

