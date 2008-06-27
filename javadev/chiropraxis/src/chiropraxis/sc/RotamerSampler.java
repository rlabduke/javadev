// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.sc;

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
* <code>RotamerSampler</code> accepts a list of chi angles and
* figures of merit, and translates them into PDB-format coordinates
* for one amino acid type.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan  6 15:33:28 EST 2004
*/
public class RotamerSampler //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df  = new DecimalFormat("###.###");
    DecimalFormat df2 = new DecimalFormat("###");
//}}}

//{{{ Variable definitions
//##############################################################################
    String                          aaType           = null;
    File                            inFile           = null;
    /** Our "template" residue */
    Residue                         res              = null;
    boolean                         allAngles        = true;
    String                          group            = null;
    String                          color            = null;
    boolean                         verbose          = false;
                                    
    boolean                         plotChis         = false;
    /** For plotChis */
    boolean                         doChi234         = false;
    /** Chis; prob vals from inFile; atomic x,y,z (srcdata format) */
    boolean                         printData        = false;
                                    
    /** Prints chis remaining after pruning in same format as silk.RotamerSampler 
    * to new file (separate from stdout output) */
    File                            notPrunedOutFile = null;
    /** 4-char sc atom names from " CB " on out, incl'ing H's */
    ArrayList<String>               atomNames        = null;
    /** Store coordinates & residues of rotamers and enable their lookup using 
    * double[]'s of chi (and pct and stat) values */
    HashMap<double[], ModelState>   valsToStates     = null;
    HashMap<double[], Residue>      valsToResidues   = null;
    
    // MY ORIGINAL AVERAGE-AVERAGE-DISTANCE-MAXIMIZATION METHOD
    /** Eliminate (1-fracNotPruned) of samples based on R3 spread */
    double                          fracNotPruned    = Double.NaN;
    /** Eliminate samples based on R3 spread until numNotPruned samples are left */
    int                             numNotPruned     = Integer.MIN_VALUE;
    
    // JEFF'S DISTANCE-TO-CLUSTER-CENTROID METHOD
    /** Add samples based on distance from rotamer cluster centers in R3 
    * until have numSelected samples */
    int                             numSelected      = Integer.MIN_VALUE;
    /** Stores rotamer cluster "identities" for all sample points and enables their
    * lookup using double[]'s of chi (and pct and stat) values. Two sample points
    * are in the same rotamer cluster if all their chis are within 60 degrees */
    HashMap<double[], String>       valsToClusters   = null;
    /** Number of unique rotamer clusters in the above */
    ArrayList<String>               clusters         = null;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotamerSampler()
    {
        super();
    }
//}}}

//{{{ prepForR3
//##############################################################################
    /**
    * Fills in atomNames and valsToStates so we don't have to calculate them repeatedly
    * during the recursive method pruneR3 (which itself calls calcSpread repeatedly)
    */
    public void prepForR3(ArrayList<double[]> data, Residue res, ModelState modelState)
    {
        // Store atom names for this sc type in advance
        String exclude = " N  , H  , CA , HA , C  , O  ";
        atomNames = new ArrayList<String>();
        for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
        {
            Atom a = (Atom)ai.next();
            if (exclude.indexOf(a.getName()) == -1)   atomNames.add(a.getName());
        }
        
        // Store dihedral values <=> coordinates correspondences in advance
        valsToStates   = new HashMap<double[], ModelState>();
        valsToResidues = new HashMap<double[], Residue>();
        int k = 1;
        for(Iterator iter = data.iterator(); iter.hasNext(); k++)
        {
            try
            {
                // Make conformation
                double[] vals = (double[])iter.next();
                Residue tempRes = new Residue(res, " ", "", Integer.toString(k), " ", res.getName());
                ModelState tempState = tempRes.cloneStates(res, modelState, new ModelState(modelState));
                SidechainAngles2 angles = new SidechainAngles2();
                tempState = angles.setChiAngles(tempRes, tempState, vals);
                
                // Store it
                valsToStates.put(vals, tempState);
                valsToResidues.put(vals, tempRes);
            }
            catch(AtomException ex) { ex.printStackTrace(); }
            catch(IOException   ex) { ex.printStackTrace(); }
        }
        
        // If using distance-to-cluster-centroid method, assign sample points to rotamer clusters
        if (numSelected != Integer.MIN_VALUE)
        {
            valsToClusters = new HashMap<double[], String>();
            
            int idx = 1;
            for (int i = 0; i < data.size(); i++)
            {
                double[] vals = (double[])data.get(i);
                boolean assigned = false;
                
                // If this sample is in the same cluster as a previously assigned sample, 
                // put this one in that cluster
                for (int j = 0; j < data.size() && !assigned; j++)
                {
                    if (i == j) continue;
                    double[] vals2 = (double[])data.get(j);
                    String cluster = valsToClusters.get(vals2);
                    if (cluster == null) continue;
                    if (sameCluster(vals, vals2))
                    {
                        //if (verbose) System.err.println("  found match");
                        valsToClusters.put(vals, cluster);
                        assigned = true;
                    }
                }
                
                // Otherwise, start a new cluster with this one
                if (!assigned)
                {
                    //if (verbose) System.err.println("  new cluster");
                    valsToClusters.put(vals, "'cluster "+idx+"'");
                    idx++;
                }
            }
            
            if (verbose)
            {
                for (Iterator iter = valsToClusters.keySet().iterator(); iter.hasNext(); )
                {
                    double[] vals = (double[])iter.next();
                    String cluster = valsToClusters.get(vals);
                    System.err.println(chisString(vals)+" belongs to "+cluster);
                }
            }
            
            // Get list of unique clusters just assigned
            TreeSet<String> clustersSet = new TreeSet<String>();
            for (Iterator iter = valsToClusters.values().iterator(); iter.hasNext(); )
                clustersSet.add((String)iter.next());
            clusters = new ArrayList<String>();
            for (Iterator iter = clustersSet.iterator(); iter.hasNext(); )
                clusters.add( (String)iter.next());
            if (verbose)
            {
                System.err.println("Unique clusters: ");
                for (String cluster : clusters)  System.err.println("  "+cluster);
            }
        }
    }
//}}}

//{{{ selectR3
//##############################################################################
    /**
    * Finds all representative central rotamers (if not yet done), then adds the sample
    * from the provided data that is "farthest" from all previously selected samples.
    * If not yet reached desired # sample points, calls this function again (recursive).
    */
    public ArrayList<double[]> selectR3(ArrayList<double[]> data, ArrayList<double[]> selData, int targetNumRotas, Residue res)
    {
        if (verbose)
        {
            System.err.println("\nrotamers selected: "+selData.size()+" / "+targetNumRotas);
            for (Iterator iter = selData.iterator(); iter.hasNext(); )
                System.err.println("  "+chisString((double[])iter.next()));
        }
        
        // See if we've reached final level of recursion
        if (selData.size() >= targetNumRotas)   return selData;
        
        // If representatives from each cluster not yet added, finish that
        if (selData.size() < clusters.size())
        {
            ArrayList<double[]> reps = findClusterReps(data, res);
            for (Iterator iter = reps.iterator(); iter.hasNext(); )
                selData.add((double[])iter.next());
        }
        
        // Add the sample for which the sum of its atoms' distances to the 
        // corresponding atoms added to its cluster so far is highest
        else
        {
            double[] farthestVals    = null;
            double   farthestSumDist = Double.NEGATIVE_INFINITY;
            for (Iterator iter = data.iterator(); iter.hasNext(); )
            {
                double[] candVals = (double[])iter.next();
                if (selData.contains(candVals))  continue; // already assigned
                
                // Get the "sum distance" to the portion of its cluster that's already been added
                // for this candidate for next sample to be added
                ModelState candState = valsToStates.get(candVals);
                double sumDist = 0;
                Residue candRes = valsToResidues.get(candVals);
                //if (verbose) System.err.println("residue for "+chisString(candVals)+"is "+candRes+" with "+candRes.getAtoms().size()+" atoms");
                //for (Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
                for (Iterator ai = candRes.getAtoms().iterator(); ai.hasNext(); )
                {
                    Atom a = (Atom)ai.next();
                    for (String atomName : atomNames)
                    {
                        if (a.getName().equals(atomName))
                        {
                            //if (verbose) System.err.println("matched "+a.getName()+" to "+atomName);
                            try
                            {
                                // Look thru already added samples in this cluster
                                AtomState cand = candState.get(a);
                                for (Iterator exstIter = selData.iterator(); exstIter.hasNext(); )
                                {
                                    double[] exstVals = (double[])exstIter.next();
                                    //if (verbose) System.err.println("seeing if "+atomName+" in "+chisString(exstVals)+" is in same cluster as "+chisString(candVals));
                                    String candCluster = valsToClusters.get(candVals);
                                    String exstCluster = valsToClusters.get(exstVals);
                                    if (candCluster.equals(exstCluster))
                                    {
                                        //if (verbose) System.err.println(chisString(exstVals)+" and "+chisString(candVals)+" are in same cluster: "+valsToClusters.get(candVals));
                                        ModelState exstState = valsToStates.get(exstVals);
                                        
                                        Residue exstRes = valsToResidues.get(exstVals);
                                        Atom a2 = null;
                                        for (Iterator ai2 = exstRes.getAtoms().iterator(); ai2.hasNext(); )
                                        {
                                            Atom temp = (Atom)ai2.next();
                                            if (atomName.equals(temp.getName()))  a2 = temp;
                                        }
                                        AtomState exst = exstState.get(a2);
                                        
                                        //if (verbose) System.err.println("found "+a2.getName()+" in        "+chisString(exstVals)+":\t("+df.format(exst.getX())+", "+df.format(exst.getY())+", "+df.format(exst.getZ())+")");
                                        //if (verbose) System.err.println("comparing to "+a.getName()+" in "+chisString(candVals)+":\t("+df.format(cand.getX())+", "+df.format(cand.getY())+", "+df.format(cand.getZ())+")");
                                        //if (verbose) System.err.println("dist = "+Triple.distance(cand, exst));
                                        sumDist += Triple.distance(cand, exst);
                                    }
                                }
                            }
                            catch(AtomException ex) { ex.printStackTrace(); }
                        }
                    }
                }
                
                // See if this candidate is the best (i.e. most distant from its cluster) so far
                if (verbose) System.err.println("sum_dist for "+chisString(candVals)+" = "+df.format(sumDist)+" Angstroms");
                if (sumDist > farthestSumDist)
                {
                    farthestSumDist = sumDist;
                    farthestVals = candVals;
                }
            }
            selData.add(farthestVals);
        }
        
        return selectR3(data, selData, targetNumRotas, res);
    }
//}}}

//{{{ findClusterReps
//##############################################################################
    /**
    * Finds a representative central rotamer for each cluster based on closeness 
    * to Cartesian centroid of the cluster
    */
    public ArrayList<double[]> findClusterReps(ArrayList<double[]> data, Residue res)
    {
        ArrayList<double[]> reps = new ArrayList<double[]>(clusters.size());
        //clustersToReps = new HashMap<String, double[]>();
        for (String cluster : clusters)
        {
            // Collect coordinates belonging to this cluster
            HashMap<double[], ModelState> valsToClusterStates = new HashMap<double[], ModelState>();
            for (Iterator iter = valsToClusters.keySet().iterator(); iter.hasNext(); )
            {
                double[] vals = (double[])iter.next();
                if (valsToClusters.get(vals).equals(cluster))
                {
                    // Found a(nother) member of this cluster
                    ModelState clusterState = valsToStates.get(vals);
                    valsToClusterStates.put(vals, clusterState);
                }
            }
            
            // Calculate centroid of this cluster
            Iterator iter = valsToClusterStates.keySet().iterator();
            ModelState template = valsToClusterStates.get( (double[])iter.next() );
            ModelState centroid = new ModelState(template);
            for (Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom a = (Atom)ai.next();
                for (String atomName : atomNames)
                {
                    if (a.getName().equals(atomName))
                    {
                        try
                        {
                            // Get coords for all atoms of this type
                            ArrayList<AtomState> clusterAtomStates = new ArrayList<AtomState>();
                            for (ModelState clusterState : valsToClusterStates.values())
                                clusterAtomStates.add(clusterState.get(a));
                            
                            // Average them
                            double x = 0, y = 0, z = 0;   int count = 0;
                            for (AtomState xyz : clusterAtomStates)
                            {
                                x += xyz.getX();  y += xyz.getY();  z += xyz.getZ();
                                count++;
                            }
                            
                            AtomState centroidAtomState = centroid.get(a);
                            centroidAtomState.setX( x/(1.0*count) );
                            centroidAtomState.setY( y/(1.0*count) );
                            centroidAtomState.setZ( z/(1.0*count) );
                        }
                        catch(AtomException ex) { ex.printStackTrace(); }
                    }
                }
            }
            
            // Figure out which sample state is closest to this cluster's centroid
            // and is therefore this cluster's representative (i.e. is closest to 
            // the bottom of this rotameric well)
            // "Closest" is defined on the basis of the sum of atom-atom distances
            // along the sidechain
            double[] closestVals = null;
            double closestSumDist = Double.POSITIVE_INFINITY;
            for (Iterator iter2 = valsToClusterStates.keySet().iterator(); iter2.hasNext(); )
            {
                double[] vals = (double[])iter2.next();
                ModelState clusterState = valsToClusterStates.get(vals);
                
                double sumDist = 0;
                for (Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
                {
                    Atom a = (Atom)ai.next();
                    for (String atomName : atomNames)
                    {
                        if (a.getName().equals(atomName))
                        {
                            try
                            {
                                AtomState cent = centroid.get(a);
                                AtomState curr = clusterState.get(a);
                                sumDist += Triple.distance(cent, curr);
                            }
                            catch(AtomException ex) { ex.printStackTrace(); }
                        }
                    }
                }
                if (sumDist < closestSumDist)
                {
                    closestSumDist = sumDist;
                    closestVals = vals;
                }
            }
            
            //clustersToReps.put(cluster, closestVals);
            if (verbose) System.err.println("representative for "+cluster+", n="+
                valsToClusterStates.keySet().size()+": "+chisString(closestVals));
            reps.add(closestVals);
        }//cluster
        
        return reps;
    }
//}}}

//{{{ sameCluster, chisString
//##############################################################################
    /**
    * Decides that two sample points are in the same rotamer cluster if all their 
    * chis are within 60 degrees; otherwise, they're not.
    * Handles wrapping from 0 to 360.
    */
    public boolean sameCluster(double[] vals1, double[] vals2)
    {
        boolean tooFar = false;
        for (int i = 0; i < vals1.length-2; i++) // last two entries are pct & stat
        {
            double y1 = vals1[i];
            double y2 = vals2[i];
            
            if((Math.abs(y1-y2) > 60)
            && (Math.abs(360-y1)+Math.abs(  0-y2) > 60)
            && (Math.abs(  0-y1)+Math.abs(360-y2) > 60) )   tooFar = true;
        }
        if (!tooFar)  return true;
        return false;
    }

    /**
    * Utility function for converting double[]'s of chi values, one pct value, 
    * and one stat value into Strings of chi values, e.g. "(185,62)"
    */
    public String chisString(double[] vals)
    {
        String s = "(";
        for (int i = 0; i < vals.length-3; i++)
        {
            String spaces = "";
            if (vals[i] < 10)                    spaces += "  ";
            if (vals[i] >= 10 && vals[i] < 100)  spaces += " ";
            s += spaces+df2.format(vals[i])+",";
        }
        String spaces = "";
        if (vals[vals.length-3] < 10)                                spaces += "  ";
        if (vals[vals.length-3] >= 10 && vals[vals.length-3] < 100)  spaces += " ";
        s += spaces+df2.format(vals[vals.length-3])+")";
        return s;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, NumberFormatException
    {
        //{{{ Prep
        // Check arguments
        if(aaType == null || inFile == null)
            throw new IllegalArgumentException("Not enough command line arguments");
        
        // Obtain template residue
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile   = pdbReader.read(this.getClass().getResourceAsStream("singleres.pdb"));
        Model           model       = coordFile.getFirstModel();
        ModelState      modelState  = model.getState();
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue)iter.next();
            if(aaType.equals(r.getName()))
                res = r;
        }
        if(res == null)
            throw new IllegalArgumentException("Couldn't find a residue called '"+aaType+"'");
        
        // Read data from list file
        LineNumberReader in = new LineNumberReader(new FileReader(inFile));
        ArrayList data = new ArrayList();
        String s;
        int nFields = -1;
        while((s = in.readLine()) != null)
        {
            if(s.startsWith("#")) continue;
            
            String[] parts = Strings.explode(s, ':');
            if(nFields < 0) nFields = parts.length;
            else if(nFields != parts.length) throw new IllegalArgumentException("Data fields are of different lengths");
            double[] vals = new double[nFields];
            for(int i = 0; i < nFields; i++)
                vals[i] = Double.parseDouble(parts[i]);
            data.add(vals);
        }
        in.close();

        // Determine figure of merit
        SidechainAngles2 angles = new SidechainAngles2();
        int nAngles = (allAngles ? angles.countAllAngles(res) : angles.countChiAngles(res));
        //System.err.println("nAngles = "+nAngles);
        double maxWeight = 0;
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            double[] vals = (double[])iter.next();
            maxWeight = Math.max(maxWeight, vals[nAngles]);
        }
        //}}}
        
        // Eliminate some sample points to maximize spread in Cartesian space (opt'l)
        if (!Double.isNaN(fracNotPruned) || numNotPruned != Integer.MIN_VALUE || numSelected != Integer.MIN_VALUE)
        {
            prepForR3(data, res, modelState);
            int targetNumRotas = 0;
            if (!Double.isNaN(fracNotPruned) || numNotPruned != Integer.MIN_VALUE)
            {
                // My original average-average-distance method (alters data by reference)
                if (!Double.isNaN(fracNotPruned))           targetNumRotas = (int)(data.size()*fracNotPruned);
                else if (numNotPruned != Integer.MIN_VALUE) targetNumRotas = numNotPruned;
                pruneR3(data, targetNumRotas, res);
            }
            else if (numSelected != Integer.MIN_VALUE)
            {
                // Jeff's distance-to-cluster-centroid method (returns new data)
                targetNumRotas = numSelected;
                ArrayList<double[]> selData = new ArrayList<double[]>(targetNumRotas);
                data = selectR3(data, selData, targetNumRotas, res);
            }
            
            // Print remaining samples in silk.RotamerSampler format (opt'l)
            if (notPrunedOutFile != null)
            {
                PrintWriter out = new PrintWriter(notPrunedOutFile);
                out.print("# ");
                for (int i = 1; i <= angles.countChiAngles(res); i++)  out.print("chi"+i+":");
                out.println("main:check");
                for(Iterator iter = data.iterator(); iter.hasNext(); )
                {
                    double[] vals = (double[])iter.next();
                    for (int i = 0; i < vals.length-1; i++)  out.print(vals[i]+":");
                    out.println(vals[vals.length-1]);
                }
                out.flush();
                out.close();
            }
        }
        
        //{{{ Output
        if (plotChis)
        {
            // Plot chis in kin format instead of creating rotamers PDB
            DecimalFormat df = new DecimalFormat("###.#");
            if (group == null) group = aaType+" samp chis";
            if (color == null) color = "blue";
            System.out.println("@group {"+group+"} dominant");
            System.out.println("@balllist {"+group+"} radius= 3 color= "+color);
            for(Iterator iter = data.iterator(); iter.hasNext(); )
            {
                // Point ID
                double[] vals = (double[])iter.next();
                System.out.print("{");
                for (int i = 0; i < vals.length-2; i ++) System.out.print(df.format(vals[i])+", ");
                System.out.print("} ");
                
                // Actual x,y,z coordinates
                int numCoords = (vals.length-2 >= 4 ? 3 : vals.length-2); // max = 3, min = 1
                for (int i = (doChi234 ? 1 : 0); i < (doChi234 ? numCoords+1 : numCoords); i ++) 
                    System.out.print(df.format(vals[i])+" ");
                System.out.println();
            }
        }
        else
        {
            // Create conformers
            PdbWriter pdbWriter = new PdbWriter(System.out);
            pdbWriter.setRenumberAtoms(true);
            
            int nDim = 0;
            if (printData) // header for opt'l top5200-angles srcdata-esque output mode
            {
                int nChis = ((double[])data.get(0)).length-2; // last 2 in .list file are pct and stat
                System.out.print("atom_name ");
                for (int i = 1; i <= nChis; i++)  System.out.print("chi"+i+" ");
                System.out.println("pct? pct? x y z");
            }
            
            int i = 1;
            for(Iterator iter = data.iterator(); iter.hasNext(); i++)
            {
                try
                {
                    double[] vals = (double[])iter.next();
                    Residue tempRes = new Residue(res, " ", "", Integer.toString(i), " ", res.getName());
                    ModelState tempState = tempRes.cloneStates(res, modelState, new ModelState(modelState));
                    if(allAngles)
                        tempState = angles.setAllAngles(tempRes, tempState, vals);
                    else
                        tempState = angles.setChiAngles(tempRes, tempState, vals);
                    for(Iterator ai = tempRes.getAtoms().iterator(); ai.hasNext(); )
                    {
                        Atom a = (Atom)ai.next();
                        // Makes all weights make best use of the 6.2 formatted field available to them
                        double occ = 999.0 * vals[nAngles]/maxWeight;
                        if(occ >= 1000.0) throw new Error("Logical error in occupancy weighting scheme");
                        tempState.get(a).setOccupancy(occ);
                    }
                    if (printData)
                    {
                        // Spit out chi dihedrals; probability measures from the input .list file (could be
                        // pct and stat); and x,y,z for each atom in sampled sidechain conformation
                        for(Iterator ai = tempRes.getAtoms().iterator(); ai.hasNext(); )
                        {
                            Atom a = (Atom)ai.next();
                            AtomState as = tempState.get(a);
                            String bbAtomNames = " N  , CA , C  , O  , H  , HA ";
                            if (bbAtomNames.indexOf(as.getName()) == -1)
                            {
                                System.out.print(tempRes.getName()+" conf"+i+" "+as.getName()+":");
                                for (int j = 0; j < vals.length; j++)  System.out.print(vals[j]+":");
                                System.out.println(as.getX()+":"+as.getY()+":"+as.getZ());
                            }
                        }
                    }
                    else
                    {
                        // "Normal" PDB output
                        pdbWriter.writeResidues(Collections.singletonList(tempRes), tempState);
                    }
                }
                catch(AtomException ex) { ex.printStackTrace(); }
            }
            System.out.flush();
            pdbWriter.close();
        }
        //}}}
    }

    public static void main(String[] args)
    {
        RotamerSampler mainprog = new RotamerSampler();
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
        catch(Throwable ex)
        {
            ex.printStackTrace();
            System.err.println();
            System.err.println("*** Error: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("RotamerSampler.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'RotamerSampler.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.sc.RotamerSampler");
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
        if(aaType == null) aaType = arg.toUpperCase();
        else if(inFile == null) inFile = new File(arg);
        else throw new IllegalArgumentException("Too many command line arguments");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-chionly"))
        {
            allAngles = false;
        }
        else if(flag.equals("-plotchis"))
        {
            plotChis  = true;
            printData = false;
        }
        else if(flag.equals("-data") || flag.equals("-printdata"))
        {
            plotChis  = false;
            printData = true;
        }
        else if(flag.equals("-group"))
        {
            group = param;
        }
        else if(flag.equals("-color"))
        {
            color = param;
        }
        else if(flag.equals("-chi234"))
        {
            doChi234 = true;
        }
        else if(flag.equals("-fracnotpruned"))
        {
            try
            {
                fracNotPruned = Double.parseDouble(param);
                numNotPruned  = Integer.MIN_VALUE;
                numSelected   = Integer.MIN_VALUE;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Couldn't parse "+param+" as double for fracNotPruned");
            }
        }
        else if(flag.equals("-numnotpruned"))
        {
            try
            {
                numNotPruned  = Integer.parseInt(param);
                fracNotPruned = Double.NaN;
                numSelected   = Integer.MIN_VALUE;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Couldn't parse "+param+" as int for numNotPruned");
            }
        }
        else if(flag.equals("-numselected"))
        {
            try
            {
                numSelected   = Integer.parseInt(param);
                numNotPruned  = Integer.MIN_VALUE;
                fracNotPruned = Double.NaN;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Couldn't parse "+param+" as int for numNotPruned");
            }
        }
        else if(flag.equals("-notprunedout") || flag.equals("-notprunedoutfile"))
        {
            notPrunedOutFile = new File(param);
        }
        else if(flag.equals("-v") || flag.equals("-verbose"))
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

//{{{ pruneR3
//##############################################################################
    /**
    * Finds sample point whose omission would most *increase* average spread (i.e.
    * average of spread_CB_, spread_CG_, etc.) and removes that sample point.
    * I define "spread" as:  average aross all atom types of 
    *                       (average across all sampled rotamers of
    *                       (average pairwise distances))
    * So the units of spread are Angstroms.
    * We want to increase spread, not decrease like you might intuitively expect, 
    * because we want to maximize dispersion in R3 for a given number of rotamers.
    * If that brings us down to the target number of sample points, defined as 
    * (fracNotPruned)*(init # sample points), we're done => return array of sample
    * points.
    * If still more sample points than desired, calls this function again (recursive).
    * This function DIRECTLY removes sample points from data (so it affects the data
    * in Main b/c the reference to data was passed here).
    */
    public ArrayList<double[]> pruneR3(ArrayList<double[]> data, int targetNumRotas, Residue res)
    {
        if (verbose) System.err.println("rotamers left = "+data.size()+" (target = "+targetNumRotas+")");
        
        // See if we've reached final level of recursion
        if (data.size() <= targetNumRotas)   return data;
        
        // Find sample whose omission would yield highest spread
        // (No need to explicity find sample whose omission would most increase 
        // spread relative to initial spread b/c initial spread is a constant.)
        double origSpread       = calcSpread(data, res);
        double highestSpread    = Double.NEGATIVE_INFINITY;
        int    idxHighestSpread = -1;
        for (int i = 0; i < data.size(); i++)
        {
            ArrayList<double[]> data2 = new ArrayList<double[]>(); // w/o sample i
            for (int j = 0; j < data.size(); j++)  if (j != i)  data2.add(data.get(j));
            double spread = calcSpread(data2, res);
            if (spread > highestSpread)
            {
                highestSpread = spread;
                idxHighestSpread = i;
            }
        }
        if (idxHighestSpread == -1 || highestSpread == Double.NEGATIVE_INFINITY)
        {
            System.err.println("removing any sample would only decrease spread");
            return null;
        }
        
        // Remove that sample
        double[] vals = data.get(idxHighestSpread);
        data.remove(idxHighestSpread);
        System.out.print("USER  MOD removed "+chisString(vals)+"\tspread: "+
            df.format(origSpread)+" -> "+df.format(highestSpread)+" Angstroms");
        if (verbose) System.err.println(
            "spread: "+df.format(origSpread)+" -> "+df.format(highestSpread)+" Angstroms");
        return pruneR3(data, targetNumRotas, res);
    }
//}}}

//{{{ calcSpread
//##############################################################################
    /**
    * Given a list of rotamers in dihedral space, generates sidechain conformations
    * for each using the appropriate aa type and ideal bond lengths+angles, then
    * calculates the spread, i.e. the average aross all atom types (_CB_, _CG_, etc.)
    * of (average across all sampled rotamers of (average pairwise distances)).
    * This includes all sc atoms, heavy and H, from _CB_ on out.
    */
    public double calcSpread(ArrayList<double[]> data, Residue res)
    {
        // Get list of xyz's (across current rota ensemble) for each sc atom type
        TreeMap<String, ArrayList<Triple>> atomNamesToXyzLists = new TreeMap<String, ArrayList<Triple>>();
        for (String atomName : atomNames)
            atomNamesToXyzLists.put(atomName, new ArrayList<Triple>());
        //int k = 1;
        for(Iterator iter = data.iterator(); iter.hasNext(); )//k++)
        {
            try
            {
                // Look up conformation
                double[] vals = (double[])iter.next();
                ModelState tempState = valsToStates.get(vals);
                Residue    tempRes   = valsToResidues.get(vals);
                //Residue tempRes = new Residue(res, " ", "", Integer.toString(k), " ", res.getName());
                //ModelState tempState = tempRes.cloneStates(res, modelState, new ModelState(modelState));
                //SidechainAngles2 angles = new SidechainAngles2();
                //tempState = angles.setChiAngles(tempRes, tempState, vals);
                
                // For each atom type of interest, find coordinates in this new conformation 
                // and add to appropriate growing list of coordinates
                AtomState xyz = null;
                for (Iterator ai = tempRes.getAtoms().iterator(); ai.hasNext(); )
                {
                    Atom a = (Atom)ai.next();
                    for (String atomName : atomNames)
                    {
                        if (a.getName().equals(atomName))
                        {
                            xyz = tempState.get(a);
                            ArrayList<Triple> xyzList = atomNamesToXyzLists.get(atomName);
                            xyzList.add(xyz);
                            atomNamesToXyzLists.put(atomName, xyzList);
                        }
                    }
                }
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
        
        // Calculate average (average distance to all neighbors) for all atom types
        ArrayList<Double> avgAvgDists = new ArrayList<Double>();
        for (String atomName : atomNames)
        {
            ArrayList<Double> avgDists = new ArrayList<Double>();
            
            // Compare each CB (or whatever atom type we're currently working on)
            // to all other CB's and get its average distance to them
            ArrayList<Triple> xyzList = atomNamesToXyzLists.get(atomName);
            for (int i = 0; i < xyzList.size(); i++)
            {
                double avgDist = 0;
                for (int j = 0; j < xyzList.size(); j++)
                {
                    if (i == j) continue;
                    avgDist += Triple.distance(xyzList.get(i), xyzList.get(j));
                }
                avgDist /= ( 1.0*(xyzList.size()-1) );
                avgDists.add(avgDist);
            }
            
            // Average this atom type's (average distance to all neighbors)s -> one final number
            double sum = 0;
            for (double avgDist : avgDists)  sum += avgDist;
            double avgAvgDist = sum / (1.0*avgDists.size());
            avgAvgDists.add(avgAvgDist);
        }
        
        // Average the average (average distance to all neighbors) across all atom types
        double spread = 0;
        for (double avgAvgDist : avgAvgDists)  spread += avgAvgDist;
        spread /= (1.0*avgAvgDists.size());
        return spread;
    }
//}}}

}//class
