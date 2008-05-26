// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.Strings;
//}}}
/**
* <code>AvgStrucGenerator</code> generates an average PDB from a set of PDBs.
* This is done over a local structural region. 
* It takes as input:
* (1) a list containing central residue numbers
* (2) any number of PDB files in "1234CH.pdb" (where C is chain)
* (3) indices relative to the central residue via -range=#,# (opt'l)
* 
* Note that sequence alignment (i.e. correspondence) is handled internally, but 
* the input PDB files *must* already be aligned in 3-space for this to work!
*
* TODO: re-scale average stdevs/atom so max is 40 (to make more B-factor-ish)?
*
* <p>Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon May 13, 2008.
*/
public class AvgStrucGenerator //extends ... implements ...
{
//{{{ Constants
    String        bbAtoms   = " N  , CA , C  , O  ";//, CB , HA "; // Pro doesn't have ' H  '
    DecimalFormat df        = new DecimalFormat("###.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Output file containing res #'s, e.g. from Helix/SheetBuilder */
    File                   resnumFile   = null;
    
    /** Simple list of supplied PDB filenames, all of which should be aligned 
    * onto the same reference structure and each of which may potentially contain
    * multiple N-caps, beta aromatics, or whatever the motif of interest may be */
    ArrayList<String>      pdbFilenames = null;
    
    /** Links 4-character PDB ID codes to above PDB filenames */
    TreeMap<String,String> pdbidsToFilenames = null;
    
    /** Identity and coords for reference model(s), which will ultimately be modified
    * according to the coords in localCoords and output as the average structure */
    Model                  refModel     = null;
    ModelState             refState     = null;
    Model                  refModel2    = null;
    ModelState             refState2    = null;
    double[]               refCoords    = null; // if applicable, has beta arom THEN opp, regardless of seq order
    
    /** Ensemble of (xyz)n vectors representing coords of local model+states.
    * Ultimately used to calculate average coords */
    ArrayList<double[]>    localCoords  = null;
    
    /** Single (xyz)n vector representing coords of average model+state and
    * corresponding std dev for each coordinate */
    double[]               avgCoords    = null;
    double[]               stdevs       = null;
    
    /** Number of structures contributing to avgCoords */
    int                    mdlCount     = 0;
    
    /** Column in anglesFile containing res #. 0-indexed */
    int                    resnumIdx    = 2;
    int                    resnum2idx   = Integer.MAX_VALUE; // NOTE: code associated with this variable 
                                                             // doesn't really work as advertised right now!
    
    /** Residue indices relative to the N-cap (helices) or the aromatic and its 
    * opposite residue (sheet) for inclusion in the output coordinates */
    int                    initIdx      = -2;
    int                    finalIdx     = 2;
    
    /** Maximum displacement for any one atom in the alignment for which we will
    * still consider the superposition good enough to use that structure in the
    * coordinate averaging */
    double                 distCutoff   = 2;
    
    boolean                verbose      = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AvgStrucGenerator()
    {
        super();
        pdbFilenames = new ArrayList<String>();
        localCoords = new ArrayList<double[]>();
    }
//}}}

//{{{ populatePdbidMap
//##############################################################################
    /**
    * Links 4-character PDB ID's to actual filenames pointing to PDB files
    */
    public void populatePdbidMap()
    {
        pdbidsToFilenames = new TreeMap<String,String>();
        
        try
        {
            Scanner s = new Scanner(resnumFile);
            while (s.hasNextLine())
            {
                // "/localspace/neo500/Hoptimize/1a8dAH.pdb:helix from A 234 ASN to A 243 THR:Ncap A 234 ASN:117.362:119.372:94.484:96.115:112.275:108.998:112.512:-61.576:146.823:-89.646:165.213:-56.85:-30.271:2.148:1.737:5.318:3.803::i+3:GLU:2:3:"
                String line = s.nextLine();
                
                // Get PDB ID
                int dotPdbIdx = line.indexOf(".pdb");
                String pdbid = line.substring(dotPdbIdx-6, dotPdbIdx-2);
                
                // Find corresponding file and make the connection
                for (String pdbFilename : pdbFilenames)
                {
                    // "/localspace/neo500/Hoptimize/1a8dAH.pdb"
                    String[] levels = Strings.explode(pdbFilename, '/');
                    String filename = levels[levels.length-1];        // "1a8dAH.pdb"
                    String pdbidInFilename = filename.substring(0,4); // "1a8d"
                    
                    if (pdbid.equals(pdbidInFilename))
                        pdbidsToFilenames.put(pdbid,pdbFilename);
                }
            }
            
            if (verbose)
            {
                for (Iterator iter = pdbidsToFilenames.keySet().iterator(); iter.hasNext(); )
                {
                    String id = (String) iter.next();
                    String filename = (String) pdbidsToFilenames.get(id);
                    System.err.println(id+" --> "+filename);
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.out.flush();
            System.err.println("Trouble with a file in populatePdbidMap()");
        }
    }
//}}}

//{{{ getLocalCoords
//##############################################################################
    /**
    * Looks through lines of a file containing res #'s of interest and ends up
    * populating localCoords, an ArrayList of double[]'s. Each of those double[]'s
    * is an array of n(xyz) vectors representing multiple atoms and residues 
    * comprising one local structure
    */
    public void getLocalCoords()
    {
        try
        {
            Scanner s = new Scanner(resnumFile);
            while (s.hasNextLine())
            {
                try
                {
                    // "/localspace/neo500/Hoptimize/1a8dAH.pdb:helix from A 234 ASN to A 243 THR:Ncap A 234 ASN:117.362:119.372:94.484:96.115:112.275:108.998:112.512:-61.576:146.823:-89.646:165.213:-56.85:-30.271:2.148:1.737:5.318:3.803::i+3:GLU:2:3:"
                    // "/localspace/neo500/Hoptimize/1a8dAH.pdb:A 91 PHE:PHE:A 171 ILE:ILE:4:3:2:4:111.19179555930297:4.323971397633469:-33.19818538964804:129.3644904319606:-158.23870442397526:143.89442845284785:124.97723272643277:18.604961060866437:109.33519492709395:24.209048379449346:23.51539906628312:-6.456628588536039:156.768185070337:"
                    String line = s.nextLine();
                    
                    // Get PDB ID and chain
                    int dotPdbIdx = line.indexOf(".pdb");
                    String pdbid = line.substring(dotPdbIdx-6, dotPdbIdx-2);
                    String chain = line.substring(dotPdbIdx-2, dotPdbIdx-1);
                    
                    // Get res #(s)
                    int resnum  = getResnum(line, resnumIdx);
                    int resnum2 = Integer.MAX_VALUE;
                    if (resnum2idx != Integer.MAX_VALUE)
                        resnum2 = getResnum(line, resnum2idx); // assume same chain...
                    
                    String fn = pdbidsToFilenames.get(pdbid);
                    if (fn != null)
                    {
                        // PDB file was provided for this entry in the input list, so let's go for it
                        if (verbose)  System.err.println("Finding coords for "+resnum+" in "+pdbid);
                        
                        // Get existing coords of entire structure
                        PdbReader reader = new PdbReader();
                        File f = new File(fn);
                        CoordinateFile cf = reader.read(f);
                        Iterator models = cf.getModels().iterator();
                        Model m = (Model) models.next();
                        ModelState ms = m.getState();
                        
                        // Make model+coords of local region
                        Model      localModel = new Model("local model "+pdbid+" "+resnum);
                        ModelState localState = new ModelState();
                        // For a 2nd segment of seq, e.g. opp beta strand:
                        Model      localModel2 = 
                            (resnum2 == Integer.MAX_VALUE ? null : new Model("local model2 "+pdbid+" "+resnum2)); 
                        ModelState localState2 = 
                            (resnum2 == Integer.MAX_VALUE ? null : new ModelState());
                        
                        for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
                        {
                            Residue res = (Residue) ri.next();
                            int currResnum = res.getSequenceInteger();
                            
                            if (res != null && chain.equals(res.getChain()) && 
                                (currResnum >= resnum+initIdx && currResnum <= resnum+finalIdx))
                            {
                                // Add this residue to the local model+state
                                if (verbose)  System.err.println("... found "+res);
                                for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
                                {
                                    Atom a = (Atom) ai.next();
                                    if (bbAtoms.indexOf(a.getName()) != -1)
                                    {
                                        if (!localModel.contains(res))
                                            localModel.add(res); // may remove res from m, but that should be OK
                                        AtomState as = ms.get(a);
                                        localState.add(as);
                                    }
                                }
                            }
                            else if (res != null && chain.equals(res.getChain()) && resnum2 != Integer.MAX_VALUE && 
                                (currResnum >= resnum2+initIdx && currResnum <= resnum2+finalIdx))
                            {
                                // Add this residue to the local model+state
                                if (verbose)  System.err.println("... found "+res);
                                for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
                                {
                                    Atom a = (Atom) ai.next();
                                    if (bbAtoms.indexOf(a.getName()) != -1)
                                    {
                                        if (!localModel2.contains(res))
                                            localModel2.add(res); // may remove res from m, but that should be OK
                                        AtomState as = ms.get(a);
                                        localState2.add(as);
                                    }
                                }
                            }
                        }
                        
                        // Do something with new local state
                        if (refModel == null && refState == null)
                        {
                            // This is the new reference local state
                            refModel = localModel;
                            refState = localState;
                            if (resnum2idx != Integer.MAX_VALUE)
                            {
                                refModel2 = localModel2;
                                refState2 = localState2;
                            }
                            setRefCoords();
                            if (verbose) System.err.println("Setting "+localModel+
                                (resnum2idx != Integer.MAX_VALUE ? " and "+localModel2 : "")
                                +" as reference");
                        }
                        else
                        {
                            // Add this local state's coords to the growing list of n(xyz) vectors
                            addToLocalCoords(localModel, localState, localModel2, localState2);
                        }
                    }
                }
                catch (ResidueException re)
                {
                    System.err.println("Trouble with a residue in getLocalCoords()...");
                }
                catch (AtomException ae)
                {
                    System.err.println("Trouble with an atom in getLocalCoords()...");
                }
            }// done w/ this local motif
        }
        catch (IOException ioe)
        {
            System.err.println("Trouble with I/O in getLocalCoords()...");
        }
    }
//}}}

//{{{ getResnum
//##############################################################################
    /**
    * Extracts residue # integer from a line of Helix-/SheetBuilder text output.
    */
    public int getResnum(String line, int thisResnumIdx)
    {
        try
        {
            // "Ncap A 234 ASN", "Ncap A   19A ASN", "Ncap A 1069 ASP", etc.
            // "A 91 PHE"      , "A   60 PHE"      , "A  578 TYR"     , etc.
            String[] tokens = Strings.explode(line, ':');
            String token = tokens[thisResnumIdx];
            int resnum = Integer.MAX_VALUE;
            String resnumString = null;
            if (token.startsWith("Ncap"))
            {
                // helix: "Ncap A 234 ASN"
                //resnumString = token.substring(token.length()-8, token.length()-4).trim();
                resnumString = token.substring(6, token.length()-3).trim();
            }
            else
            {
                // sheet: "A 91 PHE"
                resnumString = token.substring(1, token.length()-3).trim();
            }
            try
            { resnum = Integer.parseInt(resnumString); }
            catch (NumberFormatException nfex)
            { resnum = Integer.parseInt(resnumString.substring(0,resnumString.length()-1)); }
            
            return resnum;
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("Trouble with a number in getLocalCoords()...");
        }
        return Integer.MAX_VALUE;
    }
//}}}

//{{{ addToLocalCoords
//##############################################################################
    /**
    * For given local model+state, finds atoms corresponding to those in the 
    * previously determined reference local model+state, then adds the new atoms'
    * coordinates to a growing list of n(xyz) vectors. Hopefully, the xyz triplets
    * will be added  in the same atom-by-atom order each time this method is run
    * (i.e. for different local model+states vs. the same reference), which will 
    * be necessary for making an average local structure!
    */
    public void addToLocalCoords(Model localModel, ModelState localState, Model localModel2, ModelState localState2)
    {
        try
        {
            //{{{ Align residues by sequence
            // For now we just take all residues as they appear in the file,
            // without regard to chain IDs, etc.
            Alignment align = Alignment.needlemanWunsch(refModel.getResidues().toArray(), localModel.getResidues().toArray(), new SimpleResAligner());
            if (verbose)
            {
                System.err.println("Residue alignments:");
                for (int i = 0; i < align.a.length; i++)
                    System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            }
            Alignment align2 = null;
            if (localModel2 != null && localState2 != null)
            {
                align2 = Alignment.needlemanWunsch(refModel2.getResidues().toArray(), localModel2.getResidues().toArray(), new SimpleResAligner());
                if (verbose)
                {
                    System.err.println("Residue2 alignments:");
                    for (int i = 0; i < align2.a.length; i++)
                        System.err.println("  "+align2.a[i]+" <==> "+align2.b[i]);
                }
            }
            
            // Make sure the seq alignment(s) was/were "successful"...
            boolean alnmtsOK = true;
            for(int i = 0, len = align.a.length; i < len; i++)
                if (align.a[i] == null || align.b[i] == null)  alnmtsOK = false;
            if (align2 != null)  for(int i = 0, len = align2.a.length; i < len; i++)
                if (align2.a[i] == null || align2.b[i] == null)  alnmtsOK = false;
            //}}}
            
            if (alnmtsOK)
            {
                ArrayList<Double> coords = new ArrayList<Double>();
                
                //{{{ Add coords for stretch #1
                for(int i = 0, len = align.a.length; i < len; i++)
                {
                    Residue localRes = (Residue) align.b[i];
                    for(Iterator ai = localRes.getAtoms().iterator(); ai.hasNext(); )
                    {
                        Atom a = (Atom) ai.next();
                        if (bbAtoms.indexOf(a.getName()) != -1)
                        {
                            AtomState as = localState.get(a);
                            coords.add(as.getX());
                            coords.add(as.getY());
                            coords.add(as.getZ());
                        }
                    }
                }
                //}}}
                
                //{{{ Add coords for stretch #2 (opt'l)
                // Note that these coords will come after stretch #1's coords, 
                // regardless of sequence!
                if (align2 != null)
                {
                    for(int i = 0, len = align2.a.length; i < len; i++)
                    {
                        Residue localRes = (Residue) align2.a[i];
                        // Make ref xyz vector and store it
                        for(Iterator ai = localRes.getAtoms().iterator(); ai.hasNext(); )
                        {
                            Atom a = (Atom) ai.next();
                            if (bbAtoms.indexOf(a.getName()) != -1)
                            {
                                AtomState as = localState2.get(a);
                                coords.add(as.getX());
                                coords.add(as.getY());
                                coords.add(as.getZ());
                            }
                        }
                    }
                }
                //}}}
                
                double[] coordsArray = new double[coords.size()];
                for (int j = 0; j < coords.size(); j++)  coordsArray[j] = coords.get(j);
                localCoords.add(coordsArray);
            }
            else System.err.println("... bad alnmt => not using these coords for avg struc");
            
            System.err.println();
        }
        catch (AtomException ae)
        {
            System.err.println("Trouble with an atom in addToLocalCoords()...");
        }
    }
//}}}

//{{{ setRefCoords
//##############################################################################
    /**
    * Very similar to addToLocalCoords, but simply makes and stores an n(xyz)
    * vector for the reference model+state for later use
    */
    public void setRefCoords()
    {
        try
        {
            //{{{ Align residues by sequence
            // For now we just take all residues as they appear in the file,
            // without regard to chain IDs, etc.
            Alignment align = Alignment.needlemanWunsch(refModel.getResidues().toArray(), refModel.getResidues().toArray(), new SimpleResAligner());
            if (verbose)
            {
                System.err.println("setRefCoords residue alignments:");
                for (int i = 0; i < align.a.length; i++)
                    System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            }
            Alignment align2 = null;
            if (refModel2 != null)
            {
                align2 = Alignment.needlemanWunsch(refModel2.getResidues().toArray(), refModel2.getResidues().toArray(), new SimpleResAligner());
                if (verbose)
                {
                    System.err.println("setRefCoords residue2 alignments:");
                    for (int i = 0; i < align2.a.length; i++)
                        System.err.println("  "+align2.a[i]+" <==> "+align2.b[i]);
                }
            }
            
            // Make sure the seq alignment(s) was/were "successful"...
            boolean alnmtsOK = true;
            for(int i = 0, len = align.a.length; i < len; i++)
                if (align.a[i] == null || align.b[i] == null)    alnmtsOK = false;
            if (align2 != null)  for(int i = 0, len = align2.a.length; i < len; i++)
                if (align2.a[i] == null || align2.b[i] == null)  alnmtsOK = false;
            //}}}
            
            if (alnmtsOK)
            {
                ArrayList<Double> coords = new ArrayList<Double>();
                
                //{{{ Add coords for stretch #1
                for(int i = 0, len = align.a.length; i < len; i++)
                {
                    Residue refRes   = (Residue) align.a[i];
                    // Make ref xyz vector and store it
                    for(Iterator ai = refRes.getAtoms().iterator(); ai.hasNext(); )
                    {
                        Atom a = (Atom) ai.next();
                        if (bbAtoms.indexOf(a.getName()) != -1)
                        {
                            AtomState as = refState.get(a);
                            coords.add(as.getX());
                            coords.add(as.getY());
                            coords.add(as.getZ());
                        }
                    }
                }
                //}}}
                
                //{{{ Add coords for stretch #2 (opt'l)
                // Note that these coords will come after stretch #1's coords, 
                // regardless of sequence!
                if (align2 != null)
                {
                    for(int i = 0, len = align2.a.length; i < len; i++)
                    {
                        Residue refRes   = (Residue) align2.a[i];
                        // Make ref xyz vector and store it
                        for(Iterator ai = refRes.getAtoms().iterator(); ai.hasNext(); )
                        {
                            Atom a = (Atom) ai.next();
                            if (bbAtoms.indexOf(a.getName()) != -1)
                            {
                                AtomState as = refState2.get(a);
                                coords.add(as.getX());
                                coords.add(as.getY());
                                coords.add(as.getZ());
                            }
                        }
                    }
                }
                //}}}
                
                // Add coords for local residues' atoms to growing ensemble of n(xyz) vectors
                refCoords = new double[coords.size()];
                for (int i = 0; i < coords.size(); i++)  refCoords[i] = coords.get(i);
            }
            else System.err.println("... bad alnmt during setRefCoords");
            
            System.err.println();
        }
        catch (AtomException ae)
        {
            System.err.println("Trouble with an atom in setRefCoords...");
        }
    }
//}}}

//{{{ averageCoords
//##############################################################################
    /**
    * The first time this method is called (recognized b/c avgCoords is null), 
    * it averages the coordinates of structures that are not incomplete from 
    * localCoords. The second time it's run, it re-averages after eliminating
    * structures that have at least one atoms that's too far from its previously
    * determined average position
    */
    public void averageCoords()
    {
        // Get sums and counts for calc'ing averages
        int numXYZs = 3 * (Strings.explode(bbAtoms,',')).length * (finalIdx-initIdx+1);
        if (resnum2idx != Integer.MAX_VALUE)  numXYZs *= 2;
        double[] sums = new double[numXYZs];
        for (int i = 0; i < numXYZs; i++)   sums[i] = 0.0;
        mdlCount = 0;
        TreeSet<Integer> idxsToKeep = new TreeSet<Integer>();
        for (int i = 0; i < localCoords.size(); i++)
        {
            // x1 y1 z1 x2 y2 z2 ...
            double[] nXYZvector = localCoords.get(i);
            if (nXYZvector.length == numXYZs && !tooFarAway(nXYZvector))
            {
                // Acceptable contributor to avg struc
                idxsToKeep.add(i);
                for (int j = 0; j < nXYZvector.length; j++)
                    sums[j] = sums[j] + nXYZvector[j];
                mdlCount++;
                if (verbose)
                {
                    System.err.print("contrib struc "+mdlCount);
                    if (avgCoords == null)  System.err.print(": ");
                    else                    System.err.print(" (close enough): ");
                    for (int j = 0; j < nXYZvector.length; j++)
                        System.err.print(df.format(nXYZvector[j])+",");
                    System.err.println();
                }
            }
            else System.err.println(nXYZvector+" too far away or not right length ("+
                nXYZvector.length+" instead of "+numXYZs+")");
        }
        ArrayList<double[]> tempLocalCoords = new ArrayList<double[]>();
        for (int i = 0; i < localCoords.size(); i++)
        {
            if (idxsToKeep.contains(i))  tempLocalCoords.add(localCoords.get(i));
        }
        localCoords = tempLocalCoords;
        System.err.println("# contributors to average structure: "+mdlCount);
        
        // Average the coordinates
        avgCoords = new double[numXYZs];
        for (int i = 0; i < numXYZs; i++)  avgCoords[i] = sums[i] / (1.0*mdlCount);
        if (verbose)
        {
            System.err.println("# entries per n(xyz) vector: "+avgCoords.length);
            System.err.print("avg coords: ");
            for (int i = 0; i < avgCoords.length; i++)
            {
                if (i%3 == 0)  System.err.print(df.format(avgCoords[i])+"  ");
                else           System.err.print(df.format(avgCoords[i])+", ");
            }
            System.err.println();
        }
        
        // Also get standard deviations of coordinates
        stdevs = new double[numXYZs];
        for (int i = 0; i < numXYZs; i++)   stdevs[i] = 0.0;
        mdlCount = 0;
        for (double[] nXYZvector : localCoords)
        {
            // x1 y1 z1 x2 y2 z2 ...
            if (nXYZvector.length == numXYZs)
            {
                for (int i = 0; i < nXYZvector.length; i++)
                    stdevs[i] = stdevs[i] + Math.pow(nXYZvector[i]-avgCoords[i], 2);
                mdlCount++;
            }
        }
        for (int i = 0; i < numXYZs; i++)  stdevs[i] = Math.sqrt(stdevs[i] / (1.0*mdlCount));
    }
//}}}

//{{{ tooFarAway
//##############################################################################
    /**
    * Tells if a potentially contributing structure in the form of an n(xyz) 
    * vector is too far away from the main ensemble (defined as 2 Angstroms 
    * [formerly 2sigmas] in Cartesian space for any one of the bb heavy atoms
    * in the current *average* coords).
    * If avgCoords not yet defined, eliminates this n(xyz) vector if any of its
    * atoms are more than 2 Angstroms from the *reference* n(xyz) vector; this 
    * eliminates input PDBs that are very poorly aligned and off in space some-
    * where.
    */
    public boolean tooFarAway(double[] nXYZvector)
    {
        //{{{ [unused]
        //if (avgCoords == null || stdevs == null)
        //{
        //    if (verbose) System.err.println("1st averageCoords run => "
        //        +"can't say whether "+nXYZvector+" is too far from avg");
        //    return false;
        //}
        //}}}
        
        if (avgCoords == null)
        {
            for (int i = 0; i < nXYZvector.length; i++)
            {
                if (Math.abs(nXYZvector[i]-refCoords[i]) > distCutoff)
                {
                    if (verbose) System.err.println(df.format(nXYZvector[i])
                        +" too far away from ref: "+df.format(refCoords[i]));
                    return true;
                }
            }
            if (verbose) System.err.println(nXYZvector+" not too far from ref");
            return false;
        }
        else
        {
            for (int i = 0; i < nXYZvector.length; i++)
            {
                if (Math.abs(nXYZvector[i]-avgCoords[i]) > 2)//Math.abs(2*stdevs[i]))
                {
                    if (verbose) System.err.println(df.format(nXYZvector[i])
                        +" too far away from avg: "+df.format(avgCoords[i]));
                    return true;
                }
            }
            if (verbose) System.err.println(nXYZvector+" not too far from avg");
            return false;
        }
    }
//}}}

//{{{ printAvgStruc
//##############################################################################
    /**
    * The method name says it all!
    */
    public void printAvgStruc()
    {
        try
        {            
            // Make new average model+state (containing only certaina atom types)
            Model      avgModel = new Model("avg model");
            ModelState avgState = new ModelState();
            int count = 0;
            for(Iterator ri = refModel.getResidues().iterator(); ri.hasNext(); )
            {
                Residue res = (Residue) ri.next();
                for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
                {
                    Atom a = (Atom) ai.next();
                    if (bbAtoms.indexOf(a.getName()) != -1)
                    {
                        AtomState as = refState.get(a);
                        double pseudoB = 0;
                        as.setX(avgCoords[count]);  pseudoB += stdevs[count];  count++;
                        as.setY(avgCoords[count]);  pseudoB += stdevs[count];  count++;
                        as.setZ(avgCoords[count]);  pseudoB += stdevs[count];  count++;
                        // average (of x,y,&z) stdev for this atom -- rough estimate of confidence in its position
                        pseudoB /= 3.0;
                        as.setTempFactor(pseudoB);
                        as.setOccupancy(1.0);
                        
                        if (!avgModel.contains(res))
                            avgModel.add(res); // may remove res from refModel, but that should be OK
                        avgState.add(as);
                    }
                }
            }
            
            // Print out new average model+state as a PDB
            if (verbose) System.err.println("Printing average structure...");
            System.out.println("USER  MOD "+mdlCount+" contributing structures");
            PdbWriter pdbWriter = new PdbWriter(System.out);
            pdbWriter.writeResidues(avgModel.getResidues(), avgState);
            pdbWriter.close();
        }
        catch (ResidueException re)
        {
            System.err.println("Trouble with a residue in printAvgStruc()...");
        }
        catch (AtomException ae)
        {
            System.err.println("Trouble with an atom in printAvgStruc()...");
        }
    }
//}}}

//{{{ CLASS: SimpleResAligner
//##############################################################################
    static class SimpleResAligner implements Alignment.Scorer
    {
        // High is good, low is bad.
        public int score(Object a, Object b)
        {
            Residue r = (Residue) a;
            Residue s = (Residue) b;
            if(r == null || s == null)
                return -1;  // gap
            else if(r.getName().equals(s.getName()))
                return 2;   // match
            else
                return 0;   // mismatch
        }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if (resnumFile == null)
        {
            System.err.println("Need a file supplying res #'s!");
            System.exit(0);
        }
        
        populatePdbidMap();
        getLocalCoords();
        
        // 1) let all structures with correct # atoms contribute (if <= 2 Ang from ref)
        averageCoords(); 
        
        //// 2) eliminate structures too far from ^avg and re-calculate
        //averageCoords(); 
        
        printAvgStruc();
    }

    public static void main(String[] args)
    {
        AvgStrucGenerator mainprog = new AvgStrucGenerator();
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
            InputStream is = getClass().getResourceAsStream("AvgStrucGenerator.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'AvgStrucGenerator.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.AvgStrucGenerator");
        System.err.println("Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.");
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
        if (resnumFile == null)        resnumFile = new File(arg);
        else                           pdbFilenames.add(arg);
        //else throw new IllegalArgumentException("Error handling file "+arg+"!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-range"))
        {
            Scanner s = new Scanner(param).useDelimiter(",");
            int idx1 = 999, idx2 = 999;
            while (s.hasNext())
            {
                String token = s.next();
                int tokenInt = Integer.parseInt(token);
                if (idx1 == 999)        idx1 = tokenInt;
                else if (idx2 == 999)   idx2 = tokenInt;
                else System.err.println("Wrong format: should be -range=#,#");
            }
            if (idx1 != 999 && idx2 != 999)
            {
                initIdx  = idx1;
                finalIdx = idx2;
            }
            else System.err.println("Wrong format: should be -range=#,#");
        }
        else if(flag.equals("-residx"))
        {
            try
            {
                resnumIdx = Integer.parseInt(param);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Couldn't parse "+param+" as an integer for resnumIdx");
            }
        }
        else if(flag.equals("-res2idx"))
        {
            try
            {
                resnum2idx = Integer.parseInt(param);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Couldn't parse "+param+" as an integer for resnum2idx");
            }
            System.err.println("Warning: -res2idx=# doesn't work quite right!");
        }
        else if(flag.equals("-distcutoff"))
        {
            try
            {
                distCutoff = Double.parseDouble(param);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Couldn't parse "+param+" as a double for distCutoff");
            }
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

