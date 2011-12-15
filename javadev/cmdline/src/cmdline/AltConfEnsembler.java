// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.util.Strings;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.data.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
//}}}
/**
* <code>AltConfEnsembler</code> makes a multi-MODEL ensemble based on 
* all possible combinations of independent alternate conformation regions.
*
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Nov 29 2011
*/
/*
XX-TODO:
   implement Probe-based concept of interaction between regions
   use A-A vs. A-B vs. B-B dists to propose alt label swaps in orig structure
   kin output w/ diff color per region
   graph or tree data structure?...
*/
public class AltConfEnsembler //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    DecimalFormat df = new DecimalFormat("#.###");
    double pdbPrecision = 1000;
//}}}

//{{{ CLASS: AltConfRegion
//##############################################################################
    /**
    * Simple representation of an internally correlated alternate conformation region
    * that is independent of other such regions in sequence and space.
    */
    public static class AltConfRegion
    {
        private  Set  residues;
        private  Set  alts;     // e.g. 'A' or 'B', but not ' '
        
        public AltConfRegion()
        {
            super();
            residues  =  new TreeSet<Residue>();
            alts      =  new TreeSet<String>();
        }
        
        public void addResidue(Residue res)
        { residues.add(res); }
        public void addAlt(String alt)
        { alts.add(alt); }
        
        public Collection getResidues()
        { return Collections.unmodifiableCollection(residues); }
        public Collection getAlts()
        { return Collections.unmodifiableCollection(alts); }
        
        /** Updates this by merging into it the residues and alts of other. */
        public void subsume(AltConfRegion other)
        {
            for(Iterator iter = other.getResidues().iterator(); iter.hasNext(); )
            {
                Residue res = (Residue) iter.next();
                this.residues.add(res);
            }
            for(Iterator iter = other.getAlts().iterator(); iter.hasNext(); )
            {
                String alt = (String) iter.next();
                this.alts.add(alt);
            }
        }
        
        public String toString()
        {
            return residues.toString()+" "+alts.toString();
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    // INPUT
    boolean   verbose      = false;
    File      inFile       = null;
    String    pdbOut       = null;
    String    chain        = null;
    boolean   useHets      = false;
    double    minMinDist   = 3.0; // rough guess at vdW radius + vdW radius... :\
    int       maxSize      = 300;
    int       actualSize   = 0; // only incremented if expected size < max allowable size
    
    // OUTPUT
    HashSet   allRes       = null; // including hets, e.g. waters
    HashSet   altRes       = null; // ditto
    TreeSet   altLabels    = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AltConfEnsembler()
    {
        super();
    }
//}}}

//{{{ defineRegions
//##############################################################################
    /**
    * Finds linear-in-sequence alternate regions.
    */
    public ArrayList defineRegions(Model model)
    {
        // For stats later:
        allRes = new HashSet<Residue>();
        altRes = new HashSet<Residue>();
        altLabels = new TreeSet<String>();
        
        ArrayList<AltConfRegion> linRegs = new ArrayList<AltConfRegion>();
        AltConfRegion linReg = new AltConfRegion();
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
                if(resHasHets) continue; // to next residue
            }
            allRes.add(res);
            if(chain != null && !res.getChain().equals(chain)) continue; // to next residue
            
            Set resAlts = altConfsOf(res, model);
            if(resAlts.size() >= 2)
            {
                // Extend current linear region
                linReg.addResidue(res);
                altRes.add(res);
                for(Iterator sIter = resAlts.iterator(); sIter.hasNext(); )
                {
                    String alt = (String) sIter.next();
                    linReg.addAlt(alt);
                    altLabels.add(alt);
                }
            }
            else
            {
                // End current linear region (if we're in one)
                if(!linReg.getResidues().isEmpty())
                {
                    linRegs.add(linReg);
                    if(verbose) System.err.println("Made linear region: "+linReg);
                }
                linReg = new AltConfRegion();
            }
        }
        
        return linRegs;
    }
//}}}
        
//{{{ mergeRegions
//##############################################################################        
    /**
    * Merges linear-in-sequence alternate regions that are close in space.
    */
    public ArrayList mergeRegions(ArrayList linRegs, Model model)
    {
        ArrayList<AltConfRegion> conRegs = new ArrayList<AltConfRegion>();
        for(int i = 0; i < linRegs.size(); i++)
        {
            AltConfRegion linReg = (AltConfRegion) linRegs.get(i);
            boolean subsumed = false;
            for(int j = 0; j < conRegs.size(); j++)
            {
                AltConfRegion conReg = conRegs.get(j);
                double minDist = closestApproach(linReg, conReg, model);
                if(minDist < minMinDist)
                {
                    if(verbose) System.err.println("\nRegion "+conReg+"\neats   "+linReg);
                    conReg.subsume(linReg); // add to close-in-space region
                    if(verbose) System.err.println("making "+conReg+" (dist: "+df.format(minDist)+" < "+minMinDist+")");
                    subsumed = true;
                    continue; // to next subsumable linear region
                }
            }
            if(!subsumed) conRegs.add(linReg); // start new close-in-space region
        }
        
        if(verbose)
        {
            System.err.println("\nFinal list of regions:");
            for(AltConfRegion conReg : conRegs) System.err.println(conReg);
            System.err.println();
        }
        return conRegs;
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
    * between the two supplied alt conf regions,
    * regardless of whether those atoms' alt conf labels actually match.
    * If it's small enough, the two regions are probably coupled.
    * It is NOT required that the input PDB alt labels are correct,
    * because even if they're not, we can tell that the two regions interact;
    * however, the resulting alt conf ensemble will reflect 
    * any incorrect alt labels in the input PDB.
    */
    public double closestApproach(AltConfRegion region1, AltConfRegion region2, Model model)
    {
        double minDist = Double.POSITIVE_INFINITY;
        
        // For atom in region 1
        for(Iterator rIter1 = region1.getResidues().iterator(); rIter1.hasNext(); )
        {
            Residue res1 = (Residue) rIter1.next();
            for(Iterator aIter1 = res1.getAtoms().iterator(); aIter1.hasNext(); )
            {
                Atom a1 = (Atom) aIter1.next();
                
                // For atom in region 2
                for(Iterator rIter2 = region2.getResidues().iterator(); rIter2.hasNext(); )
                {
                    Residue res2 = (Residue) rIter2.next();
                    for(Iterator aIter2 = res2.getAtoms().iterator(); aIter2.hasNext(); )
                    {
                        Atom a2 = (Atom) aIter2.next();
                        
                        // Update minimum atom-atom distance based on all alt conf state combos
                        for(Iterator sIter1 = model.getStates().keySet().iterator(); sIter1.hasNext(); )
                        {
                            String stateLabel1 = (String) sIter1.next();
                            if(!region1.getAlts().contains(stateLabel1)) continue;
                            ModelState state1 = model.getState(stateLabel1);
                            for(Iterator sIter2 = model.getStates().keySet().iterator(); sIter2.hasNext(); )
                            {
                                String stateLabel2 = (String) sIter2.next();
                                if(!region2.getAlts().contains(stateLabel2)) continue;
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
    public long theoreticalSize(ArrayList regions)
    {
        long count = 0;
        for(int i = 0; i < regions.size(); i++)
        {
            AltConfRegion region = (AltConfRegion) regions.get(i);
            int numAlts = region.getAlts().size();
            if(count == 0) count = (long) numAlts;
            else if(count * numAlts >= Long.MAX_VALUE) return -1;
            else count *= numAlts;
        }
        return count;
    }
//}}}

//{{{ addToEnsemble
//##############################################################################
    /**
    * Adds each alternate conformation for each region onto a growing list
    * in a recursive & branching fashion.
    */
    public void addToEnsemble(ArrayList altsAssigned, ArrayList regions, ArrayList altCombos)
    {
        if(altsAssigned.size() == regions.size())
        {
            // Exit condition: we've made a complete combination of alt regions
            String[] altCombo = (String[]) altsAssigned.toArray(new String[altsAssigned.size()]);
            altCombos.add(altCombo);
        }
        else
        {
            // Continue recursion...
            AltConfRegion region = (AltConfRegion) regions.get(altsAssigned.size());
            for(Iterator iter = region.getAlts().iterator(); iter.hasNext(); )
            {
                String alt = (String) iter.next();
                ArrayList<String> newAltsAssigned = (ArrayList<String>) altsAssigned.clone();
                newAltsAssigned.add(alt); // alt chosen for this region of this ensemble member
                addToEnsemble(newAltsAssigned, regions, altCombos); // now for next region
            }
        }
    }
//}}}

//{{{ getAltConfGrayCodes [DEPRECATED]
//##############################################################################
    ///**
    //* Converts the original list of alternate conformation label combinations 
    //* to a Gray-code version of such a list, in which each successive entry 
    //* is related to the one before it by a single change.
    //* Unfortunately, it doesn't fully succeed when different regions have
    //* different numbers of alternates, since I wasn't able to find an 
    //* algorithm to compute <i>n</i>-ary Gray codes when <i>n</i> is variable.
    //*/
    //public ArrayList<String[]> getAltConfGrayCodes(ArrayList regions, ArrayList altCombos)
    //{
    //    HashMap<Integer, String[]> debugMap = new HashMap<Integer, String[]>();
    //    
    //    ArrayList<Integer> intCodes = new ArrayList<Integer>();
    //    for(int i = 0; i < altCombos.size(); i++)
    //    {
    //        // Convert e.g. ["A","B","B"] array to 011 integer
    //        String[] altCombo = (String[]) altCombos.get(i);
    //        String intString = "";
    //        for(int j = 0; j < altCombo.length; j++)
    //            intString += ALPHABET.indexOf(altCombo[j]);
    //        try
    //        {
    //            int intCode = Integer.parseInt(intString);
    //            intCodes.add(intCode);
    //            if(verbose) debugMap.put(intCode, altCombo);
    //        }
    //        catch(NumberFormatException ex)
    //        { System.err.println("Can't format "+intString+" as an integer!  What to do?!"); }
    //    }
    //    
    //    // Make sure we're in ascending integer order
    //    Collections.sort(intCodes);
    //    
    //    ArrayList<String[]> grayCodes = new ArrayList<String[]>();
    //    for(int intCode : intCodes)
    //    {
    //        // Convert e.g. 101 -> 212, i.e. its Gray code
    //        int[] intGrayCode = grayCode(intCode, regions.size());
    //        
    //        // Now convert back to letters, e.g. 212 -> ["C","A","C"]
    //        String[] grayCode = new String[intGrayCode.length];
    //        for(int i = 0; i < grayCode.length; i++)
    //            grayCode[i] = ALPHABET.substring(intGrayCode[i], intGrayCode[i]+1);
    //        grayCodes.add(grayCode);
    //        
    //        if(verbose)
    //        {
    //            String[] altCombo = debugMap.get(intCode);
    //            for(int i = 0; i < altCombo.length; i++) System.err.print(altCombo[i]);
    //            System.err.print(" -> "+intCode+" -> ");
    //            for(int i = 0; i < intGrayCode.length; i++) System.err.print(intGrayCode[i]);
    //            System.err.print(" -> ");
    //            for(int i = 0; i < grayCode.length; i++) System.err.print(grayCode[i]);
    //            System.err.println();
    //        }
    //    }
    //    return grayCodes;
    //}
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
    public ArrayList<String[]> getAltConfGrayCodes(ArrayList regions, ArrayList original, long expectedSize)
    {
        ArrayList<String[]> grayCodes = new ArrayList<String[]>();
        int value = 0; // input value for conversion to the "standard" n-ary Gray code sequence
        while(grayCodes.size() < expectedSize)
        {
            int[] intGrayCode = grayCode(value, regions.size());
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
            
            if(isValidAltCombo(grayCode, regions))
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
    * is consistent with the alt labels observed at all alt regions.
    */
    public boolean isValidAltCombo(String[] altCombo, ArrayList regions)
    {
        for(int i = 0; i < altCombo.length; i++)
        {
            AltConfRegion region = (AltConfRegion) regions.get(i);
            if(!region.getAlts().contains(altCombo[i])) return false;
        }
        return true;
    }
//}}}

//{{{ printEnsemble
//##############################################################################
    public void printEnsemble(ArrayList altCombos, ArrayList regions, Model model) throws IOException
    {
        for(int i = 0; i < altCombos.size(); i++)
        {
            String[] altCombo = (String[]) altCombos.get(i);
            ModelState newState = new ModelState(model.getState(" ")); // template
            for(int j = 0; j < altCombo.length; j++)
            {
                String alt = altCombo[j];
                AltConfRegion region = (AltConfRegion) regions.get(j);
                ModelState altState = model.getState(alt);
                // We don't want to add the full complement of alt conf atoms 
                // from this ^ alt state, just the ones in this particular region
                for(Iterator rIter = region.getResidues().iterator(); rIter.hasNext(); )
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
            if(verbose)
            {
                System.err.print("Printing ");
                for(int j = 0; j < altCombo.length; j++) System.err.print(altCombo[j]);
                System.err.println();
            }
            printModel(model, newState); // since alt combo loop is done
        }
    }
//}}}

//{{{ printModel
//##############################################################################
    public void printModel(Model model, ModelState state) throws IOException
    {
        // Prepare MODEL coordinates
        Model newModel = (Model) model.clone(); // use original model for template
        HashMap newStates = new HashMap();
        newStates.put(" ", state); // treat new state as default for new model
        newModel.setStates(newStates);
        CoordinateFile ensembleMember = new CoordinateFile();
        ensembleMember.add(newModel);
        actualSize++;
        
        /*System.out.println("MODEL     "+Strings.forceRight(""+actualSize, 4));
        PdbWriter pdbWriter = new PdbWriter(System.out);
        pdbWriter.writeCoordinateFile(ensembleMember);*/
        
        /*// Prepare file
        if(pdbOut == null) throw new IOException("*** Error writing PDB!");
        File pdbFile = new File(pdbOut);
        
        // MODEL header
        PrintWriter printWriter = new PrintWriter(pdbFile);
        if(printWriter == null) throw new IOException("*** Error writing PDB!");
        printWriter.print("MODEL     "+Strings.forceRight(""+actualSize, 4));
        
        // Coordinates
        PdbWriter pdbWriter = new PdbWriter(pdbFile);
        if(pdbWriter == null) throw new IOException("*** Error writing PDB!");
        pdbWriter.writeCoordinateFile(ensembleMember);*/
        
        boolean append = actualSize > 1;
        OutputStream output = new FileOutputStream(pdbOut, append);
        
        PrintWriter printWriter = new PrintWriter(output);
        if(printWriter == null) throw new IOException("*** Error writing PDB!");
        printWriter.println("MODEL     "+Strings.forceRight(""+actualSize, 4));
        printWriter.flush();
        //printWriter.close();
        
        PdbWriter pdbWriter = new PdbWriter(output);
        if(pdbWriter == null) throw new IOException("*** Error writing PDB!");
        pdbWriter.writeCoordinateFile(ensembleMember);
        //pdbWriter.close();
        
        output.close();
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
        
        // Define alt conf regions
        ArrayList linRegs = defineRegions(model);
        ArrayList regions = mergeRegions(linRegs, model);
        
        long linExpectedSize = theoreticalSize(linRegs);
        long expectedSize = theoreticalSize(regions);
        if(expectedSize <= maxSize)
        {
            // Create alt conf ensemble by enumerating combinations
            ArrayList<String[]> original = new ArrayList<String[]>();
            addToEnsemble(new ArrayList<String>(), regions, original);
            
            /*{{{ Convert to Gray code [DEPRECATED]
            ArrayList<String[]> grayCode = getAltConfGrayCodes(regions, original);
            I can't get this ^ working quite right.
            The problem is that the input "integers" (single-character alt label "strings"
            converted to integer "strings") aren't consecutive, even after sorting,
            when alt C is missing in most regions.
            }}}*/
            
            // Convert to Gray code
            ArrayList<String[]> grayCode = getAltConfGrayCodes(regions, original, expectedSize);
            
            // Print PDB models
            if(pdbOut != null) printEnsemble(grayCode, regions, model);
            else System.err.println("Not printing PDB because -pdb=out.pdb not provided");
        }
        else System.err.println("Not printing PDB because too many models ("
            +expectedSize+" > "+maxSize+")");
        
        // Report alt conf stats
        /*System.out.println("file,num_alts,
            num_res,num_alt_res,
            num_lin_alt_reg,num_alt_reg,
            num_lin_alt_mdl,num_alt_mdl");*/
        System.out.println(inFile.getName()+","+altLabels.size()+","
            +allRes.size()+","+altRes.size()+","
            +linRegs.size()+","+regions.size()+","
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
                System.err.println("\n*** Usage: java AltConfEnsembler in.pdb > alt_ensem.pdb ***\n");
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
            else if(flag.equals("-dist"))
            {
                try
                { minMinDist = Double.parseDouble(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format "+param+" as a double!"); }
            }
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

