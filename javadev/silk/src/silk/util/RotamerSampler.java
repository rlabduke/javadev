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
* (Added by DAK 2/9/08) I've added some features so peaks themselves are guaranteed
* to be sampled if a modal peaks file from Silk is also supplied. Now, the variable
* samples is the # steps you want to move away from the mode in each direction,
* not the total # samples over the relevant range. In other words, samples is how
* many points you want in each direction of each dimension IN ADDITION TO the peaks.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan  6 11:25:18 EST 2004
*/
public class RotamerSampler //extends ... implements ...
{
//{{{ Constants
    static DecimalFormat df  = new DecimalFormat("0.####");
    static DecimalFormat df2 = new DecimalFormat("#.##");
//}}}

//{{{ Variable definitions
//##############################################################################
    // Note the following correspondences for the Top500 and Top5200 Makefiles:
    //    'stat'                           file1, ndtMain
    //    'pct'                            file2, ndtCheck
    //    'stat|pct', hills modal peaks    file3
    boolean               verbose = false;
    File                  file1 = null, file2 = null, file3 = null;
    NDimTable             ndtMain = null, ndtCheck = null;
    int[]                 samples = null;
    int[]                 degrees = null; // max degrees from the peaks in each dimension
    double[]              minBounds = null, maxBounds = null;
    double                mainGE = Double.NEGATIVE_INFINITY, checkGE = Double.NEGATIVE_INFINITY;
    ArrayList<double[]>   sampPts = null;
    ArrayList<double[]>   peaks = null;
    int[]                 max = null;
    int[]                 min = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotamerSampler()
    {
        super();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ takeSamplesAroundPeaks
//##############################################################################
    /** Takes samples from both tables around the modal peaks (recursive).*/
    void takeSamplesAroundPeaks() throws IOException
    {
        DecimalFormat df2 = new DecimalFormat("#.#");
        System.err.println("Sampling around peaks...");
        
        Scanner ls = new Scanner(file3);
        for (int i = 0; i < ndtMain.getDimensions()+4; i++) ls.nextLine(); // skip headers
        while (ls.hasNextLine())
        {
            String line = ls.nextLine(); // e.g. "65.0 85.0 0.05649024553933026"
            String[] coordinates = Strings.explode(line, ' ');
            double[] peak = new double[coordinates.length];
            for (int i = 0; i < ndtMain.getDimensions(); i++)
                peak[i] = Double.parseDouble(coordinates[i]);
            peaks.add(peak);
            
            for (int i = 0; i < ndtMain.getDimensions(); i++)
            {
                minBounds[i] = peak[i] - degrees[i];//samples[i]*degrees[i];
                maxBounds[i] = peak[i] + degrees[i];//samples[i]*degrees[i];
            }
            if (verbose)
            {
                System.err.print("Sampling around peak (");
                for (int i = 0; i < peak.length-2; i++)  System.err.print(df2.format(peak[i])+",");
                System.err.print(df2.format(peak[peak.length-2])+"): from (");
                for (int i = 0; i < minBounds.length-1; i++)   System.err.print(df2.format(minBounds[i])+",");
                System.err.print(df2.format(minBounds[minBounds.length-1])+") to (");
                for (int i = 0; i < maxBounds.length-1; i++)   System.err.print(df2.format(maxBounds[i])+",");
                System.err.println(df2.format(maxBounds[maxBounds.length-1])+")...");
            }
            takeSamplesAroundPeak(new double[ndtMain.getDimensions()], 0); // will use new min/maxBounds!
        }
        //{{{ Old "above vs. below" code
//        // To avoid repeating the peak sample points
//        int[] samplesCopy  = new int[ndtMain.getDimensions()];
//        int[] samplesBelow = new int[ndtMain.getDimensions()];
//        int[] samplesAbove = new int[ndtMain.getDimensions()];
//        for (int depth = 0; depth < samples.length; depth++)
//        {
//            samplesCopy[depth]  = samples[depth];
//            samplesBelow[depth] = (int) Math.floor( (1.0*samples[depth]/2) );
//            samplesAbove[depth] = (int) Math.ceil( (1.0*samples[depth]/2) );
//            System.err.println("chi"+(depth+1)+": "+samplesCopy[depth]+" samples total, "+
//                samplesBelow[depth]+" samples below, "+
//                samplesAbove[depth]+" samples at/above");
//        }
//        
//        Scanner ls = new Scanner(file3);
//        for (int i = 0; i < ndtMain.getDimensions()+4; i++) ls.nextLine(); // skip headers
//        while (ls.hasNextLine())
//        {
//            String line = ls.nextLine(); // e.g. "65.0 85.0 0.05649024553933026"
//            String[] peaks  = Strings.explode(line, ' ');
//            double[] peakCoords = new double[peaks.length];
//            for (int i = 0; i < ndtMain.getDimensions(); i++)
//                peakCoords[i] = Double.parseDouble(peaks[i]);
//            
//            for (int i = 0; i < ndtMain.getDimensions(); i++)
//            {
//                minBounds[i] = peakCoords[i] - degrees[i];
//                maxBounds[i] = peakCoords[i];
//            }
//            if (verbose)
//            {
//                System.err.print("Sampling below    peak (");
//                for (int i = 0; i < peakCoords.length-2; i++)  System.err.print(peakCoords[i]+",");
//                System.err.print(peakCoords[peakCoords.length-2]+"): from (");
//                for (int i = 0; i < minBounds.length-1; i++)   System.err.print(minBounds[i]+",");
//                System.err.print(minBounds[minBounds.length-1]+") to (");
//                for (int i = 0; i < maxBounds.length-1; i++)   System.err.print(maxBounds[i]+",");
//                System.err.println(maxBounds[maxBounds.length-1]+")...");
//            }
//            samples = samplesBelow;
//            takeSamples(new double[ndtMain.getDimensions()], 0); // will use new min/maxBounds!
//            
//            for (int i = 0; i < ndtMain.getDimensions(); i++)
//            {
//                minBounds[i] = peakCoords[i];
//                maxBounds[i] = peakCoords[i] + degrees[i];
//            }
//            if (verbose)
//            {
//                System.err.print("Sampling at/above peak (");
//                for (int i = 0; i < peakCoords.length-2; i++)  System.err.print(peakCoords[i]+",");
//                System.err.print(peakCoords[peakCoords.length-2]+"): from (");
//                for (int i = 0; i < minBounds.length-1; i++)   System.err.print(minBounds[i]+",");
//                System.err.print(minBounds[minBounds.length-1]+") to (");
//                for (int i = 0; i < maxBounds.length-1; i++)   System.err.print(maxBounds[i]+",");
//                System.err.println(maxBounds[maxBounds.length-1]+")...");
//            }
//            samples = samplesAbove;
//            takeSamples(new double[ndtMain.getDimensions()], 0); // will use new min/maxBounds!
//            samples = samplesCopy;
//        }
//        
//        System.err.println("...done.");
        //}}}
    }
//}}}

//{{{ takeSamplesAroundPeak
//##############################################################################
    /** Takes samples at the specified intervals from both tables (recursive).*/
    void takeSamplesAroundPeak(double[] coords, int depth)
    {
        if(depth >= coords.length)
        {
            double mainVal = ndtMain.valueAt(coords);
            double checkVal = ndtCheck.valueAt(coords);
            if(mainVal >= mainGE && checkVal >= checkGE || isPeak(coords)) // want to include peaks even if below our sampling threshold
            {
                coords = wrapCoords(coords);
                if (verbose)
                {
                    System.err.print("\npct="+checkVal+" >= cutoff="+checkGE+" @ ");
                    for (int i = 0; i < coords.length; i ++)
                        System.err.print(coords[i]+",");
                    System.err.println(" (or it's a peak)");
                }
                double[] sampPt = new double[coords.length+2];
                for(int i = 0; i < coords.length; i++) sampPt[i] = coords[i];
                sampPt[sampPt.length-2] = mainVal;
                sampPt[sampPt.length-1] = checkVal;
                sampPts.add(sampPt);
                if (isPeak(coords)) peaks.add(sampPt);
            }
            else if (verbose)
            {
                System.err.print("\n(pct="+checkVal+" < cutoff="+checkGE+" @ ");
                for (int i = 0; i < coords.length; i ++)
                    System.err.print(coords[i]+",");
                System.err.println(")");
            }
        }
        else
        {
            for(int i = 0; i < (samples[depth]*2)+1; i++)
            {
                double frac = 0;
                if ((samples[depth]*2) != 0)   frac = (1.0*i) / ( (samples[depth]*2) );
                if (verbose) System.err.println("frac="+frac);
                // Equation below: (left edge) + (frac)*(left-to-right width)
                coords[depth] = minBounds[depth] + frac*(maxBounds[depth]-minBounds[depth]);
                takeSamplesAroundPeak(coords, depth+1);
            }
        }
    }

    /** Utility method.*/
    double[] wrapCoords(double[] oldCoords)
    {
        double[] newCoords = new double[oldCoords.length];
        for (int i = 0; i < oldCoords.length; i ++)
        {
            double val = oldCoords[i];
            if (val > max[i])    val -= max[i]; // e.g. 370 => 370-360 = +10
            if (val < min[i])    val += max[i]; // e.g. -10 => -10+360 = 350
            newCoords[i] = val;
        }
        return newCoords;
    }

    /** Utility method.*/
    boolean isPeak(double[] coords)
    {
        for (double[] peak : peaks)
        {
            boolean allCoordsMatch = true;
            for (int i = 0; i < ndtMain.getDimensions(); i++)
                if (coords[i] != peak[i])   allCoordsMatch = false;
            if (allCoordsMatch)   return true;
        }
        return false;
    }
//}}}

//{{{ printSamplesAroundPeaks
//##############################################################################
    /** Checks peaks then prints 'em out. */
    void printSamplesAroundPeaks()
    {
        // Do checks
        sampPts = resolveConflictingPeaks(sampPts);
        
        // Print remaining peaks
        for (double[] sampPt : sampPts)
        {
            for(int i = 0; i < sampPt.length-2; i++)
                System.out.print(df.format(sampPt[i])+":");
            System.out.print(sampPt[sampPt.length-2]+":"); // mainVal
            System.out.println(sampPt[sampPt.length-1]);   // checkVal
        }
    }

    /** Makes sure samples around peaks aren't too close to each other. Recursive! */
    ArrayList<double[]> resolveConflictingPeaks(ArrayList<double[]> currSampPts)
    {
        //if (verbose) System.err.println("currSampPts.size() = "+currSampPts.size());
        
        double minDist = Double.POSITIVE_INFINITY;
        for (int i = 0; i < degrees.length; i++)
            if ((degrees[i]*1.0)/(samples[i]*1.0) < minDist)   minDist = (degrees[i]*1.0)/(samples[i]*1.0);
        
        for (double[] sampPt : currSampPts)
        {
            for (double[] otherPt : currSampPts)
            {
                if (!sampPt.equals(otherPt))
                {
                    //double squares = 0;
                    //for (int k = 0; k < ndtMain.getDimensions(); k ++) // per dimension
                    //{
                    //    // Takes care of wrapping from 0 => max (default: 360)
                    //    double x1 = sampPt[k];    double x1wrap = x1 - max[k];
                    //    double x2 = otherPt[k];   double x2wrap = x2 - max[k];
                    //    
                    //    double x1_x2         = Math.abs(x1     - x2);
                    //    double x1wrap_x2     = Math.abs(x1wrap - x2);
                    //    double x1_x2wrap     = Math.abs(x1     - x2wrap);
                    //    double x1wrap_x2wrap = Math.abs(x1wrap - x2wrap);
                    //    
                    //    double closest = Double.POSITIVE_INFINITY;
                    //    if (x1_x2 < closest)         { closest = x1_x2;         x1 = x1;     x2 = x2;     }
                    //    if (x1wrap_x2 < closest)     { closest = x1wrap_x2;     x1 = x1wrap; x2 = x2;     }
                    //    if (x1_x2wrap < closest)     { closest = x1_x2wrap;     x1 = x1;     x2 = x2wrap; }
                    //    if (x1wrap_x2wrap < closest) { closest = x1wrap_x2wrap; x1 = x1wrap; x2 = x2wrap; }
                    //    
                    //    squares += Math.pow( (x1-x2), 2);
                    //}
                    //double dist = Math.sqrt(squares);
                    //
                    //if (dist < minDist && !peaks.contains(otherPt))
                    if (tooClose(sampPt, otherPt) && !peaks.contains(otherPt))
                    {
                        // Make a new sampPts and recursively resolve peak-peak conflicts in it
                        ArrayList<double[]> newSampPts = new ArrayList<double[]>();
                        for (double[] temp : currSampPts)   newSampPts.add(temp);
                        
                        if (verbose) System.err.println("otherPt "+ptString(otherPt)+" too close to "+ptString(sampPt)+
                            " => removing "+ptString(otherPt));
                        newSampPts.remove(otherPt);
                        
                        return resolveConflictingPeaks(newSampPts);
                    }
                    // else don't remove anything because both are peaks (weird but could happen with large value for degrees)
                }
            }
        }
        if (verbose) System.err.println("Reached end of recursive loop...");
        return currSampPts; // final exit point from recursive loop
    }
    
    /** Decides that a given pair of peaks are too close to each other if they're
    * closer than the sampling spacing in all dimensions. */
    boolean tooClose(double[] pt1, double[] pt2)
    {
        // Get distances between the two points in all dimensions
        double[] closests = new double[ndtMain.getDimensions()];
        for (int k = 0; k < ndtMain.getDimensions(); k ++) // per dimension
        {
            // Take care of wrapping from 0 => max (default: 360)
            double x1 = pt1[k];   double x1wrap = x1 - max[k];
            double x2 = pt2[k];   double x2wrap = x2 - max[k];
            
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
        // dimension to the sampling spacing in that dimension. If the distance is
        // the smaller number for all dimensions, pt1 is within the "sampling box"
        // (an n-dimensional rectangular prism) of pt2
        for (int k = 0; k < ndtMain.getDimensions(); k ++) // per dimension
        {
            double sampSpacing = degrees[k] / (1.0*samples[k]);
            if (closests[k] >= sampSpacing)
            {
                if (verbose) System.err.println("Far enough away in chi"+(k+1)+
                    ": "+df.format(closests[k])+" > "+df.format(sampSpacing));
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
    
//{{{ old code for resolveConflictingPeaks
//    /** Makes sure samples around peaks aren't too close to each other. */
//    void resolveConflictingPeaks()
//    {
//        ArrayList<double[]> toRemove  = new ArrayList<double[]>();
//        ArrayList<double[]> toKeep    = new ArrayList<double[]>();
//        
//        double minDegTravel = Double.POSITIVE_INFINITY;
//        for (int degree : degrees) if (degree < minDegTravel) minDegTravel = 1.0*degree;
//        
//        for (double[] sampPt : sampPts) if (peaks.contains(sampPt)) toKeep.add(sampPt);
//        
//        for (double[] sampPt : sampPts)//int i = 0; i < sampPts.size(); i ++)
//        {
//            //double[] sampPt = sampPts.get(i);
//            for (double[] otherPt : sampPts)//int j = i+1; j < sampPts.size(); j ++)
//            {
//                //double[] otherPt = sampPts.get(j); // guaranteed to not reference the same point
//                if (!sampPt.equals(otherPt))
//                {
//                    double squares = 0;
//                    for (int k = 0; k < ndtMain.getDimensions(); k ++) // per dimension
//                    {
//                        // Takes care of wrapping from 0 => max (default: 360)
//                        double x1 = sampPt[k];    double x1wrap = x1 - max[k];
//                        double x2 = otherPt[k];   double x2wrap = x2 - max[k];
//                        
//                        double x1_x2         = Math.abs(x1     - x2);
//                        double x1wrap_x2     = Math.abs(x1wrap - x2);
//                        double x1_x2wrap     = Math.abs(x1     - x2wrap);
//                        double x1wrap_x2wrap = Math.abs(x1wrap - x2wrap);
//                        
//                        double closest = Double.POSITIVE_INFINITY;
//                        if (x1_x2 < closest)         { closest = x1_x2;         x1 = x1;     x2 = x2;     }
//                        if (x1wrap_x2 < closest)     { closest = x1wrap_x2;     x1 = x1wrap; x2 = x2;     }
//                        if (x1_x2wrap < closest)     { closest = x1_x2wrap;     x1 = x1;     x2 = x2wrap; }
//                        if (x1wrap_x2wrap < closest) { closest = x1wrap_x2wrap; x1 = x1wrap; x2 = x2wrap; }
//                        
//                        squares += Math.pow( (x1-x2), 2);
//                    }
//                    double dist = Math.sqrt(squares);
//                    // Only delete this point if:
//                    //    - it's too close to another point
//                    //      and
//                    //    - not planning to keep this point (e.g. because it's a peak) or remove
//                    //      the other point (in which case there would be no conflict to resolve!)
//                    //      or
//                    //    - somehow planning to keep both already, in which case if one is a peak
//                    //      delete the other, but if neither is remove sampPt
//                    if (dist < minDegTravel)
//                    {
//                        if ( (!toKeep.contains(sampPt) && !toRemove.contains(otherPt))
//                          || ( toKeep.contains(sampPt) && toKeep.contains(otherPt) &&  peaks.contains(otherPt) && !peaks.contains(sampPt))
//                          || ( toKeep.contains(sampPt) && toKeep.contains(otherPt) && !peaks.contains(otherPt) && !peaks.contains(sampPt)) )
//                        {
//                            if (verbose)
//                            {
//                                System.err.print("(");
//                                for (int k = 0; k < sampPt.length-3; k ++) System.err.print(df2.format(sampPt[k])+",");
//                                System.err.print(df2.format(sampPt[sampPt.length-3])+") vs. (");
//                                for (int k = 0; k < otherPt.length-3; k ++) System.err.print(df2.format(otherPt[k])+",");
//                                System.err.print(df2.format(otherPt[otherPt.length-3])+")\t dist="+df2.format(dist)+" ?< "+df2.format(minDegTravel));
//                                System.err.println("   \t => removing");
//                            }
//                            toRemove.add(sampPt);
//                            toKeep.add(otherPt);
//                        }
//                        else if (toKeep.contains(sampPt) && toKeep.contains(otherPt) && peaks.contains(sampPt) && !peaks.contains(otherPt))
//                        {
//                            if (verbose)
//                            {
//                                System.err.print("(");
//                                for (int k = 0; k < sampPt.length-3; k ++) System.err.print(df2.format(otherPt[k])+",");
//                                System.err.print(df2.format(otherPt[otherPt.length-3])+") vs. (");
//                                for (int k = 0; k < sampPt.length-3; k ++) System.err.print(df2.format(sampPt[k])+",");
//                                System.err.print(df2.format(sampPt[sampPt.length-3])+")\t dist="+df2.format(dist)+" ?< "+df2.format(minDegTravel));
//                                System.err.println("   \t => removing");
//                            }
//                            toRemove.add(otherPt);
//                            toKeep.add(sampPt);
//                        }
//                    }
//                }
//            }
//        }
//        for (double[] sampPt : toRemove)     sampPts.remove(sampPt);
//    }
//}}}

//}}}

//{{{ takeSamples
//##############################################################################
    /** Takes samples at the specified intervals from both tables (recursive).*/
    void takeSamples(double[] coords, int depth)
    {
        if(depth >= coords.length)
        {
            double mainVal = ndtMain.valueAt(coords);
            double checkVal = ndtCheck.valueAt(coords);
            if(mainVal >= mainGE && checkVal >= checkGE)
            {
                if (verbose)
                {
                    System.err.print("\npct="+checkVal+" >= cutoff="+checkGE+" @ ");
                    for (int i = 0; i < coords.length; i ++)
                        System.err.print(coords[i]+",");
                    System.err.println();
                }
                for(int i = 0; i < coords.length; i++)
                    System.out.print(df.format(coords[i])+":");
                System.out.println(mainVal+":"+checkVal);
            }
            else if (verbose)
            {
                System.err.print("\npct="+checkVal+" < cutoff="+checkGE+" @ ");
                for (int i = 0; i < coords.length; i ++)
                    System.err.print(coords[i]+",");
                System.err.println();
            }
        }
        else
        {
            for(int i = 0; i < samples[depth]; i++)
            {
                double frac = (i+0.5)/(samples[depth]);
                coords[depth] = (1.0-frac)*minBounds[depth] + frac*maxBounds[depth];
                takeSamples(coords, depth+1);
            }
        }
    }
//}}}

//{{{ getLine
//##############################################################################
    /** Returns a trimmed line, discarding everything up to and including the 
    * first colon. */
    static private String getLine(LineNumberReader in) throws IOException
    {
        String s = in.readLine();
        if(s == null) throw new EOFException("No lines remaining in "+in);
        if(s.indexOf(':') > 0) s = s.substring(s.indexOf(':')+1);
        return s.trim();
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
        //if (samples.length >= 5) try
        //{
        //    System.err.println("High-dimensional data -> using sparse hash table...");
        //    ndtMain     = NDimTable_Sparse.createFromText(in1);
        //    ndtCheck    = NDimTable_Sparse.createFromText(in2);
        //}
        //catch (IOException ioe) // Is this even the right exception to expect?...
        //{
        //    System.err.println("Sparse hash table failed...");
            ndtMain     = NDimTable_Dense.createFromText(in1);
            ndtCheck    = NDimTable_Dense.createFromText(in2);
        //}
        in1.close();
        in2.close();
        
        minBounds = ndtMain.getMinBounds();
        maxBounds = ndtMain.getMaxBounds();
        
        if (samples == null || samples.length != ndtMain.getDimensions() || samples.length != ndtCheck.getDimensions() || (degrees != null && degrees.length != ndtMain.getDimensions()))
            throw new IllegalArgumentException("Samples not specified, or mismatch in tables and/or number of samples");
        if (file3 == null && degrees != null)
        {
            System.err.println("Hills modal peaks file and -degrees=#[,#,...] flag must accompany each other!");
            System.exit(0);
        }
        if (max == null)
        {
            max = new int[ndtMain.getDimensions()];
            for (int i = 0; i < ndtMain.getDimensions(); i++) max[i] = 360;
        }
        if (min == null)
        {
            min = new int[ndtMain.getDimensions()];
            for (int i = 0; i < ndtMain.getDimensions(); i++) min[i] = 0;
        }
        else if (max.length != ndtMain.getDimensions())
            System.err.println("max=#,#,... should have "+ndtMain.getDimensions()+" dimensions!");
        
        boolean allZero = true;
        if (degrees != null) for (int degree : degrees) if (degree != 0) allZero = false;
        if (allZero || (file3 != null && degrees == null))
        {
            System.err.println("Current -degrees=0,0,... => changing to -sample=0,0,...");
            degrees = new int[samples.length];
            for (int i = 0; i < samples.length; i++) 
            {
                degrees[i] = 0;   samples[i] = 0;
            }
        }
            
        System.out.print("# ");
        for(int i = 1; i <= ndtMain.getDimensions(); i++)
            System.out.print("chi"+i+":");
        System.out.println("main:check");
        if (file3 != null && degrees != null)
        {
            // Take N samples in each dimension within a set distance from each modal peak
            // and print them out
            sampPts = new ArrayList<double[]>();
            peaks   = new ArrayList<double[]>();
            takeSamplesAroundPeaks();
            printSamplesAroundPeaks();
        }
        else
        {
            // Take N samples in each dimension and print them out
            takeSamples(new double[ndtMain.getDimensions()], 0);
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
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'RotamerSampler.help' ***\n");
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
            else if(flag.equals("-sample"))
            {
                String[] s = Strings.explode(param, ',');
                samples = new int[s.length];
                for(int i = 0; i < s.length; i++) samples[i] = Integer.parseInt(s[i]);
            }
            else if(flag.equals("-degrees"))
            {
                String[] s = Strings.explode(param, ',');
                degrees = new int[s.length];
                for(int i = 0; i < s.length; i++) degrees[i] = Integer.parseInt(s[i]);
            }
            else if(flag.equals("-1ge")) // 'stat' in Makefile
            {
                mainGE = Double.parseDouble(param);
            }
            else if(flag.equals("-2ge")) // 'pct' in Makefile
            {
                checkGE = Double.parseDouble(param);
            }
            else if(flag.equals("-max"))
            {
                String[] s = Strings.explode(param, ',');
                max = new int[s.length];
                for(int i = 0; i < s.length; i++) max[i] = Integer.parseInt(s[i]);
                
            }
            else if(flag.equals("-min"))
            {
                String[] s = Strings.explode(param, ',');
                min = new int[s.length];
                for(int i = 0; i < s.length; i++) min[i] = Integer.parseInt(s[i]);
                
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
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("Non-number argument to "+flag+": '"+param+"'"); }
    }
//}}}
}//class

