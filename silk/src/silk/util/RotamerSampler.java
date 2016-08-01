// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package silk.util;
import silk.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.Strings;
//}}}
/**
* <code>RotamerSampler</code> is a utility program to aid in creating sampled
* rotamer conformations for Homme's DEZYMER program.
* Given the 'stat' and 'pct' data files generated in the top500-angles directory,
* it will sample at different rates in each dimension to give a list of chi angles,
* their (unnormalized) statistical probabilities, and their rotamer score.
* It's also easy to eliminate those below a given level, e.g. 1% (maybe 5% or 10% for Homme's application).
* 
* If a 'hillmodes' peaks file from Silk is also specified, samples can be chosen
* locally near peaks; the exact peaks themselves will also be included (DAK 2/9/08).
* 
* IDEAS / TO-DO:
*  - incorporate MonteCarlo & chiropraxis.sc.RotamerSampler R3 functionality here?
*     * maybe just call chiropraxis.sc.RotamerSampler R3 functionality - don't port over whole class
*  - smarter "conflict resolution" btw ~overlapping samples
*  - why "WRONG # VALUES IN..." ??
*  - 
* 
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan  6 11:25:18 EST 2004
*/
public class RotamerSampler //extends ... implements ...
{
//{{{ Constants
    static DecimalFormat df  = new DecimalFormat("0.####");
    static DecimalFormat df2 = new DecimalFormat("#.#");
//}}}

//{{{ Variable definitions
//##############################################################################
    
    boolean verbose = false;
    
    /** Simply adds comment line with peak coordinates before the set of samples built near 
    * that peak. Could be used by other programs later for rotamer-specific analysis. */
    boolean  byPeak = false;
    
    /** Correspond to stat, pct, & hillmodes from Top500/Top5200 Makefile. */
    File  file1, file2, file3;
    
    /** Correspond to stat & pct from Top500/Top5200 Makefile. */
    NDimTable  ndtMain, ndtCheck;
    
    /** Threshold stat & pct for including a sample. */
    double  mainGE = Double.NEGATIVE_INFINITY, checkGE = Double.NEGATIVE_INFINITY;
    
    /** Bounds of sampling space: min1,max1,...,minN,maxN. Used for wrapping. 
    * This and <code>minBounds</code>/<code>maxBounds</code> both span the whole space
    * if sampling on even grid, but if sampling near peaks this still spans the whole
    * space whereas <code>minBounds</code>/<code>maxBounds</code> localize to peaks. */
    int[]  bounds;
    
    /** If sampling on even grid, actual bounds from ndtMain. 
    * If sampling near peaks, peaks +/- desired distance (degrees x samples). */
    double[]  minBounds, maxBounds;
    
    /** If sampling on even grid, number of samples total in each dimension. 
    * If sampling near peaks, number of samples in *each direction* of each dimension 
    * from a peak in addition to that peak itself. */
    int[]  samples;
    
    // Specific to sampling near peaks approach:
    
    /** Number of degrees per sample from peaks in each dimension (i.e. step size). */
    int[]  degrees;
    
    /** Integer peak ID => chis & pct (no stat). */
    HashMap<Integer,double[]>  peaks;
    
    /** Chis, stat, & pct => integer peak ID. */
    HashMap<double[],Integer>  samps;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotamerSampler()
    {
        super();
    }
//}}}

//{{{ sampleOnGrid
//##############################################################################
    /**
    * Takes samples at the specified intervals w/o regard to peaks. Recursive!
    */
    void sampleOnGrid(double[] coords, int depth)
    {
        if(depth >= coords.length)
        {
            double mainVal = ndtMain.valueAt(coords);
            double checkVal = ndtCheck.valueAt(coords);
            if(mainVal >= mainGE && checkVal >= checkGE)
            {
                if(verbose) System.err.println(mainVal+" >= "+mainGE+" (stat) and "
                    +checkVal+" >= "+checkGE+" (pct) @ "+Strings.arrayInParens(coords));
                for(int i = 0; i < coords.length; i++)
                    System.out.print(df.format(coords[i])+":");
                System.out.println(mainVal+":"+checkVal);
            }
            else if(verbose) System.err.println(mainVal+" < "+mainGE+" (stat) or "
                +checkVal+" < "+checkGE+" (pct) @ "+Strings.arrayInParens(coords));
        }
        else
        {
            for(int i = 0; i < samples[depth]; i++)
            {
                double frac = (i+0.5)/(samples[depth]);
                coords[depth] = (1.0-frac)*minBounds[depth] + frac*maxBounds[depth];
                sampleOnGrid(coords, depth+1);
            }
        }
    }
//}}}

//{{{ sampleNearPeaks
//##############################################################################
    void sampleNearPeaks() throws IOException
    {
        if(verbose) System.err.println("Sampling near peaks...");
        
        peaks = new HashMap<Integer,double[]>();
        Scanner ls = new Scanner(file3);
        for(int i = 0; i < ndtMain.getDimensions()+4; i++) ls.nextLine(); // skip headers
        while(ls.hasNextLine())
        {
            String line = ls.nextLine(); // e.g. "1 65.0 85.0 0.05649024553933026"
            String[] coordinates = Strings.explode(line, ' ');
            int peakId = -1;
            try
            { peakId = Integer.parseInt(coordinates[0]); }
            catch(NumberFormatException ex)
            { System.err.println("Error parsing "+coordinates[0]+" as int peak ID!"); }
            double[] peak = new double[coordinates.length-1];
            for(int i = 0; i < ndtMain.getDimensions()+1; i++)
                peak[i] = Double.parseDouble(coordinates[i+1]); // chis & pct (no stat)
            peaks.put(peakId, peak);
        }
        
        samps = new HashMap<double[],Integer>();
        for(Iterator pItr = peaks.keySet().iterator(); pItr.hasNext(); )
        {
            int peakId = (Integer) pItr.next();
            double[] peak = peaks.get(peakId);
            
            // Sample near peak using new min/maxBounds
            for(int i = 0; i < ndtMain.getDimensions(); i++)
            {
                minBounds[i] = peak[i] - samples[i]*degrees[i]; // steps x step size
                maxBounds[i] = peak[i] + samples[i]*degrees[i]; // steps x step size
            }
            if(verbose) System.err.println("Sampling around peak "+peakId+" @ "+Strings.arrayInParens(peak)+
                " from "+Strings.arrayInParens(minBounds)+" to "+Strings.arrayInParens(maxBounds));
            sampleNearPeak(peakId, new double[ndtMain.getDimensions()], 0);
        }
        
        printSamplesNearPeaks();
    }
//}}}

//{{{ sampleNearPeak
//##############################################################################
    /**
    * Takes samples at the specified intervals starting from peaks. Recursive!
    */
    void sampleNearPeak(int peakId, double[] coords, int depth)
    {
        if(depth >= coords.length)
        {
            double mainVal = ndtMain.valueAt(coords);
            double checkVal = ndtCheck.valueAt(coords);
            // we'll take samples above threshold or peaks even if below threshold
            if((mainVal >= mainGE && checkVal >= checkGE) || isPeak(coords))
            {
                coords = wrapCoords(coords);
                if(verbose) System.err.println(mainVal+" >= "+mainGE+" (stat) and "+checkVal+
                    " >= "+checkGE+" (pct) @ "+Strings.arrayInParens(coords)+" (or it's a peak)");
                
                double[] samp = new double[coords.length+2];
                for(int i = 0; i < coords.length; i++)
                    samp[i] = coords[i];        // chis
                samp[samp.length-2] = mainVal;  // stat = mainVal
                samp[samp.length-1] = checkVal; // pct = checkVal
                
                samps.put(samp, peakId);
            }
            else if(verbose) System.err.println(mainVal+" < "+mainGE+" (stat) or "+checkVal+
                " < "+checkGE+" (pct) @ "+Strings.arrayInParens(coords)+" (and not a peak)");
        }
        else
        {
            for(int i = 0; i < (samples[depth]*2)+1; i++)
            {
                double frac = 0;
                if((samples[depth]*2) != 0)
                    frac = (1.0*i) / (samples[depth]*2);
                // equation below: (left edge) + (frac)*(left-to-right width)
                coords[depth] = minBounds[depth] + frac*(maxBounds[depth]-minBounds[depth]);
                sampleNearPeak(peakId, coords, depth+1);
            }
        }
    }
//}}}

//{{{ printSamplesNearPeaks
//##############################################################################
    /** Checks peaks then prints 'em out. */
    void printSamplesNearPeaks()
    {
        /*samps = resolveConflicts(samps);*/
        // This ^ ends up basically eliminating points 
        // in concentrated regions - not what I wanted!
        
        // Print remaining peaks
        if(byPeak)
        {
            // Goal: allow subsequent analyses to find max/min of some value, 
            // e.g. steric clash, w/in each peak separately.
            for(Iterator pItr = peaks.keySet().iterator(); pItr.hasNext(); )
            {
                int peakId = (Integer) pItr.next();
                double[] peak = peaks.get(peakId);
                
                // Print comment indicating parent peak of following sample points.
                System.out.print("#"+peakId+":");
                for(int p = 0; p < peak.length-1; p++) 
                    System.out.print(df2.format(peak[p])+":"); // chis
                System.out.print("__?__:");                    // (no stat)
                System.out.println(peak[peak.length-1]);       // pct
                
                // Print samples derived from current peak
                for(Iterator sItr = samps.keySet().iterator(); sItr.hasNext(); )
                {
                    double[] samp = (double[]) sItr.next();
                    int sampPeakId = samps.get(samp);
                    if(sampPeakId == peakId)
                    {
                        // Sample derived from current peak
                        if(samp.length == ndtMain.getDimensions()+2)
                        {
                            for(int i = 0; i < samp.length-2; i++)
                                System.out.print(df.format(samp[i])+":"); // chis
                            System.out.print(  samp[samp.length-2]+":");  // stat = mainVal
                            System.out.println(samp[samp.length-1]);      // pct = checkVal
                        }
                        else System.err.println(
                            "WRONG # VALUES IN "+Strings.arrayInParens(samp)+" - SHOULD NEVER HAPPEN!!! - IGNORING FOR NOW");
                    }
                }
            }
        }
        else
        {
            for(Iterator sItr = samps.keySet().iterator(); sItr.hasNext(); )
            {
                double[] samp = (double[]) sItr.next();
                if(samp.length == ndtMain.getDimensions()+2)
                {
                    for(int i = 0; i < samp.length-2; i++)
                        System.out.print(df.format(samp[i])+":"); // chis
                    System.out.print(  samp[samp.length-2]+":");  // stat = mainVal
                    System.out.println(samp[samp.length-1]);      // pct = checkVal
                }
                else System.err.println(
                    "WRONG # VALUES IN "+Strings.arrayInParens(samp)+" - SHOULD NEVER HAPPEN!!! - IGNORING FOR NOW");
            }
        }
    }
//}}}

//{{{ resolveConflicts
//##############################################################################
    /**
    * If samples are too close to each other from sampling near peaks, 
    * this method drops the non-peak sample of the two if possible; 
    * otherwise it averages them. Recursive!
    */
    HashMap<double[],Integer> resolveConflicts(HashMap<double[],Integer> currSamps)
    {
        double minDist = Double.POSITIVE_INFINITY;
        for(int i = 0; i < degrees.length; i++)
            if(degrees[i] < minDist)
                minDist = degrees[i];
        
        for(Iterator s1Itr = currSamps.keySet().iterator(); s1Itr.hasNext(); )
        {
            double[] samp1 = (double[]) s1Itr.next();
            for(Iterator s2Itr = currSamps.keySet().iterator(); s2Itr.hasNext(); )
            {
                double[] samp2 = (double[]) s2Itr.next();
                if(!sameSample(samp1, samp2))
                {
                    double[] avg = tooClose(samp1, samp2);
                    if(avg != null) // means they're too close
                    {
                        // Make a new currSamps and recursively resolve peak-peak conflicts in it
                        HashMap<double[],Integer> newSamps = new HashMap<double[],Integer>();
                        for(Iterator sItr = currSamps.keySet().iterator(); sItr.hasNext(); )
                        {
                            double[] samp = (double[]) sItr.next();
                            newSamps.put(samp, currSamps.get(samp));
                        }
                        
                        if(isPeak(samp1) && !isPeak(samp2))
                        {
                            if(verbose) System.err.println(Strings.arrayInParens(samp1)+" is peak but "
                                +Strings.arrayInParens(samp2)+" is not => dropping latter");
                            newSamps.remove(samp2);
                        }
                        else if(!isPeak(samp1) && isPeak(samp2))
                        {
                            if(verbose) System.err.println(Strings.arrayInParens(samp2)+" is peak but "
                                +Strings.arrayInParens(samp1)+" is not => dropping latter");
                            newSamps.remove(samp1);
                        }
                        else if(isPeak(samp1) && isPeak(samp2))
                        {
                            throw new IllegalArgumentException(Strings.arrayInParens(samp2)+" and "
                                +Strings.arrayInParens(samp1)+" are conflicting PEAKS - should never happen!");
                        }
                        else // neither is a peak
                        {
                            if(verbose) System.err.println("samples "+Strings.arrayInParens(samp2)+
                                " and "+Strings.arrayInParens(samp1)+" too close => averaging them");
                            newSamps.remove(samp2);
                            newSamps.remove(samp1);
                            int peakId1 = currSamps.get(samp1);
                            int peakId2 = currSamps.get(samp2);
                            if(peakId1 <= peakId2) newSamps.put(avg, peakId1); // take the lower peak ID int 
                            else                   newSamps.put(avg, peakId2); //    if forced to choose
                        }
                        
                        return resolveConflicts(newSamps);
                    }//if too close
                }//if not same sample
            }//2
        }//1
        if(verbose) System.err.println("Reached end of recursive sample-conflict resolution");
        return currSamps; // final exit point from recursive loop
    }
//}}}

//{{{ tooClose
//##############################################################################
    /**
    * Decides that a given pair of samples are too close to each other if 
    * they're closer than the sampling spacing in all dimensions.
    * If that's true, returns the average of the two samples; if not, returns null.
    * Wrapping is considered.
    * @param pt1 = chis, stat, & pct
    * @param pt2 = chis, stat, & pct
    */
    double[] tooClose(double[] pt1, double[] pt2)
    {
        // Get distances between the two points in all dimensions.
        // Calculate average of the two in dihedral space (considering wrap)
        //   while we're at it.
        double[] closests = new double[ndtMain.getDimensions()];
        double[] avg      = new double[ndtMain.getDimensions()+1];
        for(int k = 0; k < ndtMain.getDimensions(); k++) // per dimension
        {
            // Consider wrapping (default: 0 = 360)
            //double x1 = pt1[k];   double x1wrap = x1 - max[k];
            //double x2 = pt2[k];   double x2wrap = x2 - max[k];
            double x1 = pt1[k];   double x1wrap = x1 - (bounds[k+1]-bounds[k]); //    62 - (360 -    0)
            double x2 = pt2[k];   double x2wrap = x2 - (bounds[k+1]-bounds[k]); // or 62 - (180 - -180)
            
            double x1_x2         = Math.abs(x1     - x2);
            double x1wrap_x2     = Math.abs(x1wrap - x2);
            double x1_x2wrap     = Math.abs(x1     - x2wrap);
            double x1wrap_x2wrap = Math.abs(x1wrap - x2wrap);
            
            closests[k] = Double.POSITIVE_INFINITY;
            if(x1_x2 < closests[k])         { closests[k] = x1_x2;         x1 = x1;     x2 = x2;     }
            if(x1wrap_x2 < closests[k])     { closests[k] = x1wrap_x2;     x1 = x1wrap; x2 = x2;     }
            if(x1_x2wrap < closests[k])     { closests[k] = x1_x2wrap;     x1 = x1;     x2 = x2wrap; }
            if(x1wrap_x2wrap < closests[k]) { closests[k] = x1wrap_x2wrap; x1 = x1wrap; x2 = x2wrap; }
            avg[k] = (x1+x2)/2;
        }
        avg[avg.length-2] = pt1[pt1.length-2]; // stat = mainVal
        avg[avg.length-1] = pt1[pt1.length-1]; // pct = checkVal
        
        // For each dimension, compare the distance between the two points in that
        // dimension to the sampling spacing in that dimension. If the distance is
        // the smaller number for all dimensions, pt1 is within the "sampling box"
        // (an n-dimensional rectangular prism) of pt2
        for(int k = 0; k < ndtMain.getDimensions(); k++) // per dimension
        {
            if(closests[k] >= degrees[k])
            {
                if(verbose) System.err.println("Far enough away in chi"+(k+1)+
                    ": "+df.format(closests[k])+" > "+df.format(degrees[k]));
                return null;
            }
        }
        if(verbose)
        {
            System.err.println(Strings.arrayInParens(pt1)+" too close to "+Strings.arrayInParens(pt2));
            System.err.println("avg of the two: "+Strings.arrayInParens(avg));
        }
        return avg;
    }
//}}}

//{{{ (helper functions)
//##############################################################################
    /** Copied directly from SilkCmdLine. */
    int[] explodeInts(String s) throws NumberFormatException
    {
        String[]    strings = Strings.explode(s, ',', true, true);
        int[]       ints    = new int[strings.length];
        for(int i = 0; i < strings.length; i++)
            ints[i] = Integer.parseInt(strings[i]);
        return ints;
    }

    /**
    * Modifies chis of provided sample to lie within user-dictated bounds
    *   if they don't already.
    * @param coords = (chis) OR (chis & pct)
    */
    double[] wrapCoords(double[] coords)
    {
        for(int i = 0; i < ndtMain.getDimensions(); i++)
        {
            if(coords[i] > bounds[i+1]) coords[i] -= bounds[i+1]; // e.g. 370 => 370-360 = +10
            if(coords[i] < bounds[i]  ) coords[i] += bounds[i+1]; // e.g. -10 => -10+360 = 350
        }
        return coords;
    }

    /**
    * Uses just chis (no stat or pct) to tell if given sample matches any peak.
    * @param coords = (chis) OR (chis & pct)
    */
    boolean isPeak(double[] coords)
    {
        for(Iterator pItr = peaks.keySet().iterator(); pItr.hasNext(); )
        {
            int peakId = (Integer) pItr.next();
            double[] peak = peaks.get(peakId);
            
            boolean allCoordsMatch = true;
            for(int i = 0; i < ndtMain.getDimensions(); i++)
                if(coords[i] != peak[i])
                    allCoordsMatch = false;
            if(allCoordsMatch)
                return true;
        }
        return false;
    }

    /**
    * Uses all values (chis, stat, & pct) to tell if given samples match each other.
    */
    boolean sameSample(double[] s1, double[] s2)
    {
        for(int i = 0; i < s1.length; i++)
            if(s1[i] != s2[i])
                return false;
        return true;
    }
//}}}

//{{{ doChecks
//##############################################################################
    /** Checks that a bunch on input parameters are OK. */
    public void doChecks()
    {
        if(samples == null || samples.length != ndtMain.getDimensions() 
        || samples.length != ndtCheck.getDimensions()
        || (degrees != null && degrees.length != ndtMain.getDimensions()))
            System.err.println("Samples not specified, or mismatch in tables and/or number of samples");
        
        if(bounds == null)
        {
            System.err.println("No -bounds=#,#,.. specified -> assuming 0-360 in all dimensions");
            bounds = new int[2*ndtMain.getDimensions()];
            for(int i = 0; i < 2*ndtMain.getDimensions(); i += 2)
            {
                bounds[i]   =   0;
                bounds[i+1] = 360;
            }
        }
        else if(bounds.length != 2*ndtMain.getDimensions())
            System.err.println("bounds=#,#,.. should have "+(2*ndtMain.getDimensions())+" entries!");
        
        if(file3 == null)
        {
            System.err.println("Third (hillmodes) file not provided -> sampling evenly on grid");
            if(byPeak)
                System.err.println("Ignoring -bypeak b/c third (hillmodes) file not provided");
        }
        else //if(file3 != null)
        {
            System.err.println("Third (hillmodes) file provided -> sampling near peaks");
            if(degrees == null)
            {
                System.err.println("Must specify -degrees=#,#,.. to accompany third (hillmodes) file");
                System.exit(0);
            }
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
        if(file1 == null || file2 == null)
            throw new IllegalArgumentException("Must specify at least two file names");
        
        // Load tables from disk
        InputStream in1 = new FileInputStream(file1);
        InputStream in2 = new FileInputStream(file2);
        ndtMain  = NDimTable_Dense.createFromText(in1);
        ndtCheck = NDimTable_Dense.createFromText(in2);
        in1.close();
        in2.close();
        
        doChecks();
        
        System.out.print("# "+(byPeak ? "peak" : ""));
        for(int i = 1; i <= ndtMain.getDimensions(); i++)
            System.out.print("chi"+i+":");
        System.out.println("main:check");
        
        minBounds = ndtMain.getMinBounds(); // values may change later if sampling near 
        maxBounds = ndtMain.getMaxBounds(); // peaks, but this sets the dimensionality
        
        if(file3 == null || degrees == null)
        {
            // Take samples in each dimension w/o regard to peaks
            // (i.e. what Ian's original RotamerSampler did).
            sampleOnGrid(new double[ndtMain.getDimensions()], 0);
        }
        else
        {
            // Take samples in each dimension outward from each peak.
            sampleNearPeaks();
        }
        System.out.flush();
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
            InputStream is = getClass().getResourceAsStream("RotamerSampler.help");
            if(is == null) System.err.println(
                "\n*** Usage: silk.util.RotamerSampler stat.data pct.data [hillmodes.data] "+
                    "-bounds=#,#,.. -samples=#,#,.. [-degrees=#,#,..] ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("silk.util.RotamerSampler");
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
        if(file1 == null)       file1 = new File(arg);
        else if(file2 == null)  file2 = new File(arg);
        else if(file3 == null)  file3 = new File(arg);
        else                    throw new IllegalArgumentException("Too many file names: "+arg);
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
            else if(flag.equals("-1ge")) // 'stat' in Makefile
            {
                mainGE = Double.parseDouble(param);
            }
            else if(flag.equals("-2ge")) // 'pct' in Makefile
            {
                checkGE = Double.parseDouble(param);
            }
            else if(flag.equals("-samples") || flag.equals("-samp"))
            {
                samples = explodeInts(param);
            }
            else if(flag.equals("-degrees") || flag.equals("-deg"))
            {
                degrees = explodeInts(param);
            }
            else if(flag.equals("-bounds"))
            {
                bounds = explodeInts(param);
            }
            else if(flag.equals("-bypeak"))
            {
                byPeak = true;
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

