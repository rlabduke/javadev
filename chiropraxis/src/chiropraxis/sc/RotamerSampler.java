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
* It can also prune samples such that the remaining samples are 
* as "spread out" as possible in terms of the position & orientation 
* of the sidechain's tip with respect to the residue's own backbone.
* 
* IDEAS / TO-DO:
*  - include pct &/o stat as "terms" in quats' "electrostatic repulsion" "energy"?
*     * that's one way to fuse the R3 & bioinformatic worlds in one method
*     * another would be to make the quat repulsion stuff call-able from silk RotSamp
*  - repulse only w/in peaks?
*     * would be faster, but...
*     * would require knowledge of peaks (maybe if called by silk?)
*     * how to deal with edges of adjoining peaks?
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan  6 15:33:28 EST 2004
*/
public class RotamerSampler //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    
    // INPUT
    
    boolean  verbose    = false;
    
    String   aaType;
    File     inFile;
    
    /** Our "template" residue. */
    Residue  res;
    
    boolean  allAngles  = true;
    
    String   group;
    String   color;
    
    // OUTPUT
    
    /** Do "inverse rotamers": superpose sc end instead of bb. */
    boolean  inverse    = false;
    
    /** Output input-formatted strings of remaining samples instead of PDB-format coordinates. */
    boolean  printSamples  = false;
    
    /** Output chi values in kinemage format instead of PDB-format coordinates. */
    boolean  plotChis  = false;
    /** If plotting chi values, plot chis 2,3,4 instead of chis 1,2,3. */
    boolean  chi234  = false;
    
    /** Output quaternion x,y,z,w in kinemage format instead of PDB-format coordinates. */
    boolean  plotQuats  = false;
    
    /** Number of samples to keep; eliminate the rest to maximize spread in quaternion space. */
    int     keepCnt  = Integer.MAX_VALUE;
    /** Fraction of samples to keep; eliminate the rest to maximize spread in quaternion space. */
    double  keepFrc  = Double.NaN;
    
    // FOR INTERNAL USE
    
    /** Numbered residues for sampled sidechain conformers, keyed by chis. */
    HashMap<double[], Residue>     residues;
    /** States for sampled sidechain conformers, keyed by chis. */
    HashMap<double[], ModelState>  states;
    
    /** Quaternions describing orientation of sidechain tip relative to own backbone, keyed by chis. */
    HashMap<double[], Quaternion>  quats;
    /** Quaternions describing orientation of sidechain tip relative to own backbone, keyed by chis. 
    * Nothing pruned from this guy in subsequent steps. */
    HashMap<double[], Quaternion>  quatsOrig;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotamerSampler()
    {
        super();
    }
//}}}

//{{{ buildConformers
//##############################################################################
    /**
    * Builds sidechains using chis. Stores in global hash map for possible
    * modification and output later.
    */
    public void buildConformers(ArrayList<double[]> data) throws IOException
    {
        // Obtain template residue
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile   = pdbReader.read(this.getClass().getResourceAsStream("singleres.pdb"));
        Model           model       = coordFile.getFirstModel();
        ModelState      modelState  = model.getState();
        Residue         res         = null;
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue)iter.next();
            if(aaType.equals(r.getName()))
                res = r;
        }
        if(res == null)
            throw new IllegalArgumentException("Couldn't find a residue called '"+aaType+"'");
        
        SidechainAngles2 angles = new SidechainAngles2();
        int nAngles = (allAngles ? angles.countAllAngles(res) : angles.countChiAngles(res));
        
        // Determine figure of merit
        double maxWeight = 0;
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            double[] vals = (double[]) iter.next();
            maxWeight = Math.max(maxWeight, vals[nAngles]);
        }
        
        // Build & store conformers
        residues = new HashMap<double[], Residue   >();
        states   = new HashMap<double[], ModelState>();
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
                residues.put(vals, tempRes);
                states.put(vals, tempState);
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ calcQuats
//##############################################################################
    /**
    * Maps each set of chis to a quaternion describing the relationship
    * between the sidechain's end and the backbone.
    */
    public void calcQuats(ArrayList<double[]> data)
    {
        String[] scEnd = null;
        if(aaType.equals("ASN")) scEnd = new String[] {" CG ", " OD1", " ND2"};
        if(aaType.equals("GLN")) scEnd = new String[] {" CD ", " OE1", " NE2"};
        if(aaType.equals("ARG")) scEnd = new String[] {" CZ ", " NH1", " NH2"};
        // ...
        if(scEnd == null) throw new IllegalArgumentException("Unrecognized aa type: "+aaType+"!");
        
        quats     = new HashMap<double[], Quaternion>();
        quatsOrig = new HashMap<double[], Quaternion>(); // for our records
        
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            try
            {
                double[] vals = (double[])iter.next();
                Residue    tempRes   = residues.get(vals);
                ModelState tempState = states.get(vals);
                
                Triple bb1 = null, bb2 = null, bb3 = null;
                Triple sc1 = null, sc2 = null, sc3 = null;
                for(Iterator ai = tempRes.getAtoms().iterator(); ai.hasNext(); )
                {
                    Atom      a  = (Atom) ai.next();
                    AtomState as = tempState.get(a);
                    if(a.getName().equals(" CA ")) bb1 = new Triple(as);
                    if(a.getName().equals(" N  ")) bb2 = new Triple(as);
                    if(a.getName().equals(" C  ")) bb3 = new Triple(as);
                    if(a.getName().equals(scEnd[0])) sc1 = new Triple(as);
                    if(a.getName().equals(scEnd[1])) sc2 = new Triple(as);
                    if(a.getName().equals(scEnd[2])) sc3 = new Triple(as);
                }
                if(bb1 == null || bb2 == null || bb3 == null)
                    throw new IllegalArgumentException("Can't find the 3 bb atoms in "+tempRes+"!");
                if(sc1 == null || sc2 == null || sc3 == null)
                    throw new IllegalArgumentException("Can't find the 3 sc atoms in "+tempRes+"!");
                Triple[] bb = new Triple[] {bb1, bb2, bb3};
                Triple[] sc = new Triple[] {sc1, sc2, sc3};
                
                // Quaternion
                SuperPoser poser = new SuperPoser(bb, sc);
                Transform xform = poser.superpos();
                Quaternion quat = new Quaternion().likeRotation(xform);
                quats.put(vals, quat);
                quatsOrig.put(vals, quat);
                
                if(inverse) // "inverse rotamers"
                {
                    for(Iterator ai = tempRes.getAtoms().iterator(); ai.hasNext(); )
                    {
                        Atom      a  = (Atom) ai.next();
                        AtomState as = tempState.get(a);
                        xform.transform(as);
                    }
                    states.put(vals, tempState); // overwrite
                }
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
        
        /*for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            double[] vals = (double[]) iter.next();
            Quaternion quat = quats.get(vals);
            System.out.println(Strings.arrayInParens(vals)+" "
                +quat.getX()+" "+quat.getY()+" "+quat.getZ()+" "+quat.getW());
        }
        System.exit(0);*/
    }
//}}}

//{{{ rmDuplicates, rmOneDuplicate
//##############################################################################
    /**
    * Makes sure no samples yield the same quaternion, probably (always?) b/c 
    * they have the same chi values.
    */
    public void rmDuplicates(ArrayList<double[]> data, ArrayList<double[]> dataOrig)
    {
        int keep = (keepCnt != Integer.MAX_VALUE ? keepCnt : (int)Math.floor(dataOrig.size()*keepFrc));
        System.err.println(data.size()+"/"+dataOrig.size()+" samples, goal: "+keep+"  (orig input)");
        
        boolean noMoreDuplicates = false;
        while(!noMoreDuplicates)
        {
            int duplicateIdx = rmOneDuplicate(data);
            if(duplicateIdx == -1)
                noMoreDuplicates = true;
            else
            {
                double[] vals = data.get(duplicateIdx);
                quats.remove(vals);
                data.remove(duplicateIdx);
            }
        }
    }

    public int rmOneDuplicate(ArrayList<double[]> data)
    {
        for(int i = 0; i < data.size(); i++)
        {
            double[] vals1 = data.get(i);
            Quaternion quat1 = quats.get(vals1);
            boolean duplicate = false;
            for(int j = 0; j < data.size() && i != j; j++)
            {
                double[] vals2 = data.get(j);
                Quaternion quat2 = quats.get(vals2);
                if(quat2.equals(quat1))
                {
                    duplicate = true;
                    if(verbose) System.err.println(
                        "Removing duplicate for "+Strings.arrayInParens(vals1));
                    return i;
                }
            }
        }
        return -1;
    }
//}}}

//{{{ repulse
//##############################################################################
    /**
    * Pretends quaternions are electrons on the surface of a sphere, and finds 
    *   sample point whose omission would most decrease "electrostatic energy".
    * Called recursively until we're left with the desired number of samples.
    * Warning: My implementation makes *NO* guarantee of avoiding local minima - 
    *   that's a significant problem!!!
    */
    public ArrayList<double[]> repulse(ArrayList<double[]> data, ArrayList<double[]> dataOrig)
    {
        int keep = (keepCnt != Integer.MAX_VALUE ? keepCnt : (int)Math.floor(dataOrig.size()*keepFrc));
        System.err.println(data.size()+"/"+dataOrig.size()+" samples, goal: "+keep);
        
        // See if we've reached final level of recursion.
        if(data.size() <= keep) return data;
        
        // Find sample whose omission would most decrease "electrostatic energy".
        double minE    = Double.POSITIVE_INFINITY;
        int    minEidx = -1;
        for(int i = 0; i < data.size(); i++)
        {
            ArrayList<double[]> dataOmit = new ArrayList<double[]>(); // w/o sample i
            for(int j = 0; j < data.size(); j++)
                if(j != i)
                    dataOmit.add(data.get(j));
            double E = repulsion(dataOmit);
            if (E < minE)
            {
                minE = E;
                minEidx = i;
            }
        }
        if (minEidx == -1 || minE == Double.NEGATIVE_INFINITY)
        {
            System.err.println("Removing any sample would only increase \"energy\"!");
            return data; // done
        }
        
        // Remove that sample
        double[] vals = data.get(minEidx);
        data.remove(minEidx);
        DecimalFormat df = new DecimalFormat("###.###");
        if(verbose) System.err.println(
            "Removed "+Strings.arrayInParens(vals)+",  E -> "+df.format(minE));
        return repulse(data, dataOrig);
    }
//}}}

//{{{ repulsion
//##############################################################################
    /**
    * Calculates "electrostatic energy" for a set of quaternions:
    *   E = Sum_i Sum_j 1/dist(x_i,x_j)^2
    */
    public double repulsion(ArrayList<double[]> dataOmit)
    {
        double E = 0;
        
        for(int i = 0; i < dataOmit.size(); i++)
        {
            for(int j = 0; j < dataOmit.size(); j++)
            {
                if(j != i)
                {
                    double[] vals_i = dataOmit.get(i);
                    double[] vals_j = dataOmit.get(j);
                    Quaternion quat_i = quats.get(vals_i);
                    Quaternion quat_j = quats.get(vals_j);
                    
                    if(quat_i.equals(quat_j) && verbose)
                        System.err.println("found a duplicate: "+quat_i);
                    
                    // distance in quaternion space
                    double dist = Math.sqrt( Math.pow(quat_i.getX()-quat_j.getX(), 2) +
                                             Math.pow(quat_i.getY()-quat_j.getY(), 2) +
                                             Math.pow(quat_i.getZ()-quat_j.getZ(), 2) +
                                             Math.pow(quat_i.getW()-quat_j.getW(), 2) );
                    double distSqrd = dist * dist;
                    E += 1.0 / distSqrd;
                }
            }
        }
        
        /*System.err.println("E = "+E);*/
        return E;
    }
//}}}

//{{{ printSamples
//##############################################################################
    /** Write out Silk-output-format strings for remaining samples. 
    * If -keep not used, should be same as input. */
    public void printSamples(ArrayList<double[]> data)
    {
        // Header(s?)
        try
        {
            LineNumberReader in = new LineNumberReader(new FileReader(inFile));
            String s;
            while((s = in.readLine()) != null)
            {
                // # chi1:chi2:chi3:chi4:main:check
                if(s.startsWith("#"))
                    System.out.println(s);
            }
            in.close();
        }
        catch(IOException ex)
        { System.err.println("Error reading headers in "+inFile); }
        
        // Sample strings
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            // 52.5:182.5:172.5:82.5:4.8326884352494234E-5:0.37011794417325267
            double[] vals = (double[])iter.next();
            for(int i = 0; i < vals.length-1; i++)
                System.out.print(vals[i]+":");
            System.out.println(vals[vals.length-1]);
        }
    }
//}}}

//{{{ plotChis
//##############################################################################
    /** Plot chis in kin format instead of creating rotamers PDB. */
    public void plotChis(ArrayList<double[]> data)
    {
        DecimalFormat df = new DecimalFormat("###.#");
        if(group == null) group = aaType+" samp chis";
        if(color == null) color = "blue";
        System.out.println("@group {"+group+"} dominant");
        System.out.println("@balllist {"+group+"} radius= 3 color= "+color);
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            // Point ID
            double[] vals = (double[]) iter.next();
            System.out.print("{");
            for(int i = 0; i < vals.length-2; i++)
                System.out.print(df.format(vals[i])+", ");
            System.out.print("} ");
            
            // Chis as kin coords
            int numCoords = (vals.length-2 >= 4 ? 3 : vals.length-2); // max = 3, min = 1
            for(int i = (chi234 ? 1 : 0); i < (chi234 ? numCoords+1 : numCoords); i++) 
                System.out.print(df.format(vals[i])+" ");
            System.out.println();
        }
    }
//}}}

//{{{ plotQuats
//##############################################################################
    /**
    * Prints 4-D kinemage of quaternions describing sidechain tip position 
    * and orientation relative to the residue's own backbone.
    */
    public void plotQuats(ArrayList<double[]> data, ArrayList<double[]> dataOrig)
    {
        System.out.println("@dimensions {X} {Y} {Z} {W}");
        System.out.println("@dimminmax -1 1 -1 1 -1 1 -1 1");
        
        System.out.println("@group {axis} dominant");
        System.out.println("@vectorlist {axis}");
        System.out.println("{axis}P -1 -1 -1");
        System.out.println("{axis}   1 -1 -1");
        System.out.println("{axis}   1  1 -1");
        System.out.println("{axis}  -1  1 -1");
        System.out.println("{axis}  -1 -1 -1");
        System.out.println("{axis}P -1 -1  1");
        System.out.println("{axis}   1 -1  1");
        System.out.println("{axis}   1  1  1");
        System.out.println("{axis}  -1  1  1");
        System.out.println("{axis}  -1 -1  1");
        System.out.println("{axis}P -1 -1 -1");
        System.out.println("{axis}  -1 -1  1");
        System.out.println("{axis}P  1 -1 -1");
        System.out.println("{axis}   1 -1  1");
        System.out.println("{axis}P -1  1 -1");
        System.out.println("{axis}  -1  1  1");
        System.out.println("{axis}P  1  1 -1");
        System.out.println("{axis}   1  1  1");
        
        System.out.println("@group {"+aaType+" quats "+dataOrig.size()+"/"+dataOrig.size()+"} dominant animate");
        System.out.println("@balllist {"+aaType+" quats} radius= 0.05 dimension= 4");
        for(Iterator iter = dataOrig.iterator(); iter.hasNext(); )
        {
            double[] vals = (double[]) iter.next();
            Quaternion quat = quatsOrig.get(vals);
            System.out.println("{"+Strings.arrayInParens(vals)+"}" + (data.contains(vals) ? " " : "gray ")
                +quat.getX()+" "+quat.getY()+" "+quat.getZ()+" "+quat.getW());
        }
        
        int keep = (keepCnt != Integer.MAX_VALUE ? keepCnt : (int)Math.floor(dataOrig.size()*keepFrc));
        System.out.println("@group {"+aaType+" quats "+keep+"/"+dataOrig.size()+"} dominant animate");
        System.out.println("@balllist {"+aaType+" quats} radius= 0.05 dimension= 4");
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            double[] vals = (double[]) iter.next();
            Quaternion quat = quats.get(vals);
            System.out.println("{"+Strings.arrayInParens(vals)+"} "
                +quat.getX()+" "+quat.getY()+" "+quat.getZ()+" "+quat.getW());
        }
    }
//}}}

//{{{ writePdb
//##############################################################################
    /** Write out PDB-format coordinates for conformers. */
    public void writePdb(ArrayList<double[]> data)
    {
        PdbWriter pdbWriter = new PdbWriter(System.out);
        pdbWriter.setRenumberAtoms(true);
        int i = 1;
        for(Iterator iter = data.iterator(); iter.hasNext(); )//i++)
        {
            double[] vals = (double[])iter.next();
            Residue    tempRes   = residues.get(vals);
            ModelState tempState = states.get(vals);
            System.out.println("MODEL     "+Strings.forceRight(""+i, 4));
            pdbWriter.writeResidues(Collections.singletonList(tempRes), tempState);
            System.out.println("ENDMDL");
            i++;
        }
        System.out.flush();
        pdbWriter.close();
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, NumberFormatException
    {
        // Check arguments
        if(aaType == null || inFile == null)
            throw new IllegalArgumentException("Not enough command line arguments - need aatype & listfile");
        
        // Read data from list file
        LineNumberReader in = new LineNumberReader(new FileReader(inFile));
        ArrayList data     = new ArrayList();
        ArrayList dataOrig = new ArrayList(); // for our records
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
            dataOrig.add(vals);
        }
        in.close();
        
        if (plotChis)
        {
            plotChis(data);
            return; // done
        }
        
        buildConformers(data);
        
        if(keepCnt != Integer.MAX_VALUE || !Double.isNaN(keepFrc))
        {
            calcQuats(data);
            rmDuplicates(data, dataOrig); // directly modifies data
            repulse(data, dataOrig); // directly modifies data
        }
        
        if(printSamples)
            printSamples(data);
        else if(plotQuats)
            plotQuats(data, dataOrig);
        else
            writePdb(data);
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
        }
        else if(flag.equals("-plotquats"))
        {
            plotQuats = true;
        }
        else if(flag.equals("-printsamples"))
        {
            printSamples = true;
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
            chi234 = true;
        }
        else if(flag.equals("-keep"))
        {
            try
            {
                double keep = Double.parseDouble(param);
                if     (keep <  0) System.err.println("D'oh!  -keep="+param+" must be >= 0...  Ignoring flag");
                else if(keep <= 1)
                {
                    System.err.println("Treating -keep="+param+" as a fraction");
                    keepFrc = keep;
                }
                else if(keep >  1)
                {
                    System.err.println("Treating -keep="+param+" as an integer count");
                    keepCnt = (int)Math.floor(keep);
                }
            }
            catch(NumberFormatException ex)
            { System.err.println("*** Error: couldn't parse -keep="+param+" as a double!"); }
        }
        else if(flag.equals("-inverse"))
        {
            inverse = true;
        }
        //{{{ OLD PAIRWISE ATOM-ATOM DISTANCE R3 SAMPLING STUFF
        //else if(flag.equals("-fracnotpruned"))
        //{
        //    try
        //    {
        //        fracNotPruned = Double.parseDouble(param);
        //        numNotPruned  = Integer.MIN_VALUE;
        //        numSelected   = Integer.MIN_VALUE;
        //    }
        //    catch (NumberFormatException nfe)
        //    {
        //        System.err.println("Couldn't parse "+param+" as double for fracNotPruned");
        //    }
        //}
        //else if(flag.equals("-numnotpruned"))
        //{
        //    try
        //    {
        //        numNotPruned  = Integer.parseInt(param);
        //        fracNotPruned = Double.NaN;
        //        numSelected   = Integer.MIN_VALUE;
        //    }
        //    catch (NumberFormatException nfe)
        //    {
        //        System.err.println("Couldn't parse "+param+" as int for numNotPruned");
        //    }
        //}
        //else if(flag.equals("-numselected"))
        //{
        //    try
        //    {
        //        numSelected   = Integer.parseInt(param);
        //        numNotPruned  = Integer.MIN_VALUE;
        //        fracNotPruned = Double.NaN;
        //    }
        //    catch (NumberFormatException nfe)
        //    {
        //        System.err.println("Couldn't parse "+param+" as int for numNotPruned");
        //    }
        //}
        //else if(flag.equals("-notprunedout") || flag.equals("-notprunedoutfile"))
        //{
        //    notPrunedOutFile = new File(param);
        //}
        //}}}
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
}//class
