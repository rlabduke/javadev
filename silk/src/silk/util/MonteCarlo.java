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
* <code>MonteCarlo</code> is a utility program for non-deterministically sampling
* from Silk -energy values in a .data file. It uses the Metropolis Monte Carlo 
* approach. Empirically determined rules are as follows:
* 
* - More trials      => tighter clustering around modes
* - Higher init temp => same
* - Sim'd annealing 500 to 100 worked pretty well for Asn
* - ??? => ???
* 
* This could be used to e.g. produce ensembles of probable sidechain conformations
* at the protein surface from Silk-smoothed sidechain dihedral data.
*
* The template for the structure of this class was (roughly) silk.util.RotamerSampler.
*
* <p>Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Wed Apr 16, 2008
*/
public class MonteCarlo //extends ... implements ...
{
//{{{ Constants
    static Random        random  = new Random();
    static DecimalFormat df      = new DecimalFormat("###.###");
    static DecimalFormat df2     = new DecimalFormat("#.##");
    static DecimalFormat df3     = new DecimalFormat("###");
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean               verbose         = false;
    File                  file1           = null;
    File                  file2           = null;
    File                  file3           = null;
    NDimTable             ndtEnergy       = null;
    NDimTable             ndtPct          = null;
    double[]              minBounds       = null;
    double[]              maxBounds       = null;
    double[]              degrees         = null;       // max jump in multi-D space per move
    ArrayList<double[]>   samples         = null;
    ArrayList<double[]>   track           = null;
    double                temp            = 298;
    double[]              simAnnealTemps  = null;       // for simulated annealing
    double                cutoff          = Double.NaN;
    int                   trials          = 50;         // # trials to perform
    int                   moves           = 50;         // # moves per trial
    boolean               doChi234        = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MonteCarlo()
    {
        super();
    }
//}}}

//{{{ takeSamples
//##############################################################################
    /** Takes samples using the Monte Carlo Metropolis approach. */
    void takeSamples()
    {
        for (int t = 1; t <= trials; t ++)
        {
            // START TRIAL
            int movesTried    = 0;
            int movesAccepted = 0;
            
            // New random starting position
            double[] coords = new double[ndtEnergy.getDimensions()];
            for (int x = 0; x < coords.length; x ++)
            {
                double r = random.nextDouble();
                coords[x] = minBounds[x] + (r * maxBounds[x]);
            }
            
            // Start series of attempted moves
            for (int m = 1; m <= moves; m ++) 
            {
                if (simAnnealTemps != null)  updateTemp(m, moves);
                
                // Make a random move
                double[] newCoords = new double[ndtEnergy.getDimensions()];
                for (int x = 0; x < coords.length; x ++)
                {
                    double r = -1.0 + (2.0 * random.nextDouble()); // -1 to 1
                    newCoords[x] = coords[x] + ( degrees[x] * r );
                }
                newCoords = wrap(newCoords);
                
                // Test it with the Metropolis criterion
                double oldE = ndtEnergy.valueAt(coords);
                double newE = ndtEnergy.valueAt(newCoords);
                if (verbose)
                {
                    String dir = (newE < oldE ? "<" : ">");
                    System.err.println( "   newE = "+df.format(newE)+"  "+dir+
                        "  oldE = "+df.format(oldE) );
                }
                if (newE < oldE || boltzmannCoin(newE, oldE))
                {
                    // Accept the move
                    coords = newCoords;
                    movesTried ++;
                    movesAccepted ++;
                    if (track != null)  track.add(coords);
                }
                else
                {
                    // Reject the move
                    movesTried ++;
                }
            }
            
            if (acceptFinalPosition(coords))
            {
                samples.add(coords);
                if (track != null)   printToTrackKin(t);
            }
            if (track != null)   track = new ArrayList<double[]>();
            if (verbose)
            {
                double perc = 100.0 * (1.0*movesAccepted) / (1.0*movesTried);
                System.err.print("Acceptance stats for trial #"+t+":  "+
                    df.format(perc)+"% ("+movesAccepted+"/"+movesTried+")   "+
                    (acceptFinalPosition(coords) ? "accepted" : "rejected"));
            }
            // END TRIAL
        }
    }
//}}}

//{{{ addPeaks, wrap
//##############################################################################
    /** Add coordinates from peaks '.list' file as sample points, if provided */
    void addPeaks()
    {
        try
        {
            Scanner ls = new Scanner(file3);
            int idx = 0;
            while (ls.hasNextLine())
            {
                String line = ls.nextLine(); // e.g. "12.34:45.67:1.234:5.678"
                if (!line.startsWith("#"))
                {
                    idx ++;
                    String[] peakStr = Strings.explode(line, ':');
                    double[] peak = new double[peakStr.length-2];
                    for (int i = 0; i < peak.length; i ++)
                        peak[i] = Double.parseDouble(peakStr[i]);
                    samples.add(peak);
                    if (track != null)
                    {
                        // Print as part of the track kin but with a different point ID
                        String label = "hills peak "+idx;
                        System.out.println("@group {"+label+"} animate dominant master= {all}");
                        System.out.println("@balllist {"+label+"} radius= 5.0 color= green"
                            +" master= {all} master= {peaks}");
                        printCoords(peak, label, true);
                    }
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't find peaks '.list' file!");
        }
    }
    
    /** Returns a wrapped version of this point if it's moved off the grid. */
    double[] wrap(double[] coords)
    {
        for (int x = 0; x < coords.length; x ++)
        {
            if (coords[x] < minBounds[x])                            // e.g. -10 < 0
                coords[x] = coords[x] + (maxBounds[x]-minBounds[x]); //      -10 + (360-0) = 350
            if (coords[x] > maxBounds[x])                            // e.g. 370 > 360
                coords[x] = coords[x] - (maxBounds[x]-minBounds[x]); //      370 - (360-0) =  10
        }
        return coords;
    }
//}}}

//{{{ updateTemp, boltzmannCoin, acceptFinalPosition
//##############################################################################
    /** Updates temperature according to a simple, linear simulated annealing
    * schedule. Starts high and reaches low point by completion of trial. */
    void updateTemp(int currMove, int totalMoves)
    {
        if (verbose) System.err.print("Updating temp from "+df.format(temp));
        
        double pctDone = (1.0*currMove) / (1.0*totalMoves);
        temp = simAnnealTemps[0] - pctDone*(simAnnealTemps[0] - simAnnealTemps[1]);
        
        if (verbose) System.err.println(" to "+df.format(temp)+" on move "+currMove+"/"+totalMoves);
    }
    
    /** "Flips the Boltzmann coin," as Homme Hellinga likes to say, to see if
    * this move will be accepted despite the new energy being higher (worse). */
    boolean boltzmannCoin(double newE, double oldE)
    {
        // (k_Boltzmann in kcal/mol.K) * (temperature in K)
        double kT = 0.0019872 * temp; // = 0.59219 kcal/mol
        double r = random.nextDouble();
        
        double coinVal = Math.exp( (-1.0*(newE-oldE))/kT );
        
        if (verbose)
        {
            String dir = (coinVal < r ? "<" : ">");
            System.err.println("      B'man coin = "+df.format(coinVal)+"  "+dir+
                "  random val = "+df.format(r) );
        }
        
        if (coinVal > r)  return true;
        return false;
    }
    
    /** Decides to accept final position from a trial iff all of the following 
    * criteria are met:
    * (1) not too close to a previously reached final position (e.g. sample point)
    * (2) final position is above the 'pct' cutoff (if given)
    */
    boolean acceptFinalPosition(double[] coords)
    {
        boolean tooClose = false;
        for (double[] sample : samples)
            if (tooClose(sample, coords))
                tooClose = true;
        
        boolean cutoffOkay = false;
        if (!Double.isNaN(cutoff) && ndtPct.valueAt(coords) > cutoff)
            cutoffOkay = true;
        
        if (!tooClose && cutoffOkay)   return true;
        else return false;
    }
//}}}

//{{{ tooClose, ptString
//##############################################################################
    /** Decides that a given pair of samples are too close to each other if they're
    * closer than the maximum allowable move (in degrees) in all dimensions. */
    boolean tooClose(double[] pt1, double[] pt2)
    {
        // Get distances between the two points in all dimensions
        double[] closests = new double[ndtEnergy.getDimensions()];
        for (int k = 0; k < ndtEnergy.getDimensions(); k ++) // per dimension
        {
            // Take care of wrapping from 0 => max (default: 360)
            double x1 = pt1[k];   double x1wrap = x1 - maxBounds[k];
            double x2 = pt2[k];   double x2wrap = x2 - maxBounds[k];
            
            double x1_x2         = Math.abs(x1     - x2);
            double x1wrap_x2     = Math.abs(x1wrap - x2);
            double x1_x2wrap     = Math.abs(x1     - x2wrap);
            double x1wrap_x2wrap = Math.abs(x1wrap - x2wrap);
            
            closests[k] = Double.POSITIVE_INFINITY;
            if (x1_x2 < closests[k])         { closests[k] = x1_x2;         x1 = x1;     x2 = x2;     }
            if (x1wrap_x2 < closests[k])     { closests[k] = x1wrap_x2;     x1 = x1wrap; x2 = x2;     }
            if (x1_x2wrap < closests[k])     { closests[k] = x1_x2wrap;     x1 = x1;     x2 = x2wrap; }
            if (x1wrap_x2wrap < closests[k]) { closests[k] = x1wrap_x2wrap; x1 = x1wrap; x2 = x2wrap; }
        }
        
        // For each dimension, compare the distance between the two points in that 
        // dimension to the maximum allowable move (in degrees) in that dimension.
        // If the distance is the smaller number for all dimensions, pt1 is within 
        // the "sampling box" (an n-dimensional rectangular prism) of pt2.
        for (int k = 0; k < ndtEnergy.getDimensions(); k ++) // per dimension
        {
            if (closests[k] >= degrees[k])
            {
                if (verbose) System.err.println("Far enough away in chi"+(k+1)+
                    ": "+df.format(closests[k])+" > "+df.format(degrees[k]));//(sampSpacing));
                return false;
            }
        }
        if (verbose) System.err.println(ptString(pt1)+" too close to "+ptString(pt2));
        return true;
    }
    
    /** Returns String representing point: "(33, 44, 78, ..., 99)". */
    String ptString(double[] pt)
    {
        String ptStr = "(";
        for (int k = 0; k < pt.length-3; k ++)   ptStr += df2.format(pt[k])+",";
        ptStr += df2.format(pt[pt.length-3])+")";
        return ptStr;
    }
//}}}

//{{{ printToTrackKin, printCoords
//##############################################################################
    /** Prints a series of accepted points in dihedral space in kinemage format. */
    void printToTrackKin(int trialnum)
    {
        String c = "pink peachtint yellowtint greentint bluetint lilactint";
        String[] colors = Strings.explode(c, ' ');
        int idx = (int) ( (1.0*trialnum) % (1.0*colors.length) );
        String color = colors[idx];
        
        System.out.println("@group {MC trial "+trialnum+"} animate dominant master= {all}");
        
        // Points along trial trajectory
        System.out.println("@balllist {MC trial "+trialnum+"} radius= 1.2 color= "+color
            +" master= {all} master= {samp intermediate}");
        for (int i = 0; i < track.size()-1; i ++)
        {
            double[] coords = track.get(i);
            String label = "trial "+trialnum+" acc'd move "+i;
            boolean p = (i == 0 ? true : false);
            printCoords(coords, label, p);
        }
        
        // Final position at end of trajectory
        System.out.println("@balllist {MC trial "+trialnum+" final} radius= 5.0 color= "+color
            +" master= {all} master= {samp final}");
        double[] coords = track.get(track.size()-1);
        String label = "trial "+trialnum+" final position";
        printCoords(coords, label, true);
    }
    
    /** Prints a point's coordinates in kinemage form with the given point ID. 
    * Only handles input double[]s of length <= 4. */
    void printCoords(double[] coords, String label, boolean p)
    {
        System.out.print("{"+label+"}");
        if (p)  System.out.print("P ");
        else    System.out.print("  ");
        
        int init = (doChi234 ? 1 : 0);
        int fin  = (!doChi234 && coords.length == 4 ? coords.length-1 : coords.length);
        for (int i = init; i < fin; i ++)  System.out.print( df.format(coords[i])+" " );
        
        if (coords.length <= 2)
            for (int i = 0; i < 3-coords.length; i ++)   System.out.print(" 0");
        
        System.out.println();
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
            throw new IllegalArgumentException("Must specify 'energy' and 'pct' file names");
        
        // Load table from disk
        InputStream in1 = new FileInputStream(file1);
        InputStream in2 = new FileInputStream(file2);
        ndtEnergy = NDimTable_Dense.createFromText(in1);
        ndtPct    = NDimTable_Dense.createFromText(in2);
        in1.close();
        in2.close();
        
        minBounds = ndtEnergy.getMinBounds();
        maxBounds = ndtEnergy.getMaxBounds();
        
        if(degrees == null)
        {
            degrees = new double[ndtEnergy.getDimensions()];
            for (int x = 0; x < degrees.length; x ++)
                //degrees[x] = 10.0 + 10.0*x; // 10 for chi1, 20 for chi2, etc.
                degrees[x] = 10.0;
        }
        
        // Take samples
        samples = new ArrayList<double[]>();
        if (file3 != null)  addPeaks();
        takeSamples();
        
        // Print them out
        if (track == null)
        {
            System.out.print("# ");
            for(int i = 1; i <= ndtEnergy.getDimensions(); i++)
                System.out.print("chi"+i+":");
            System.out.println("main:check");
            
            for (double[] sample : samples)
            {
                for(int i = 0; i < sample.length; i++)
                    System.out.print(df3.format(Math.round(sample[i]))+":");
                //System.out.println("1:1");
                double val = ndtPct.valueAt(sample);
                System.out.println(val+":"+val);
            }
        }
        System.out.flush();
    }

    public static void main(String[] args)
    {
        MonteCarlo mainprog = new MonteCarlo();
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
            InputStream is = getClass().getResourceAsStream("MonteCarlo.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'MonteCarlo.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("silk.util.MonteCarlo");
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
        if(file1 == null)      file1 = new File(arg);
        else if(file2 == null) file2 = new File(arg);
        else if(file3 == null) file3 = new File(arg);
        else                   throw new IllegalArgumentException("Too many file names: "+arg);
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
            else if(flag.equals("-degrees"))
            {
                String[] s = Strings.explode(param, ',');
                degrees = new double[s.length];
                for(int i = 0; i < s.length; i++)  degrees[i] = Double.parseDouble(s[i]);
            }
            else if(flag.equals("-track"))
            {
                track = new ArrayList<double[]>();
            }
            else if(flag.equals("-temp"))
            {
                temp = Double.parseDouble(param);
            }
            else if(flag.equals("-simanneal"))
            {
                try
                {
                    String[] parts = Strings.explode(param, ',');
                    simAnnealTemps = new double[2];
                    simAnnealTemps[0] = Double.parseDouble(parts[0]); // initial temp
                    simAnnealTemps[1] = Double.parseDouble(parts[1]); // final temp
                }
                catch (IndexOutOfBoundsException ioobe)
                {
                    System.err.println("Must provide two temps sep'd by a comma,"
                        +"e.g. -simanneal=3000,298");
                }
            }
            else if(flag.equals("-cutoff"))
            {
                cutoff = Double.parseDouble(param);
            }
            else if(flag.equals("-trials"))
            {
                trials = Integer.parseInt(param);
            }
            else if(flag.equals("-moves"))
            {
                moves = Integer.parseInt(param);
            }
            else if(flag.equals("-chi234"))
            {
                doChi234 = true;
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

