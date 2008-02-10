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
* to be sampled if a modal peaks file from Silk is also supplied. It remains true
* that samples is always the # samples over the relevant range, which is either
* the entire grid or (if a peak file is supplied) the box specified by degrees 
* surrounding each individual peak.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan  6 11:25:18 EST 2004
*/
public class RotamerSampler //extends ... implements ...
{
//{{{ Constants
    static DecimalFormat df = new DecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##############################################################################
    // Note the following correspondences for the Top500 and Top5200 Makefiles:
    //    'stat'                           file1, ndtMain
    //    'pct'                            file2, ndtCheck
    //    'stat|pct, hills modal peaks     file3
    boolean     verbose = false;
    File        file1 = null, file2 = null, file3 = null;
    NDimTable   ndtMain = null, ndtCheck = null;
    int[]       samples = null;
    int[]       degrees = null; // max degrees from the peaks in each dimension
    double[]    minBounds = null, maxBounds = null;
    double      mainGE = Double.NEGATIVE_INFINITY, checkGE = Double.NEGATIVE_INFINITY;
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
        System.err.println("Sampling around peaks...");
        
        Scanner ls = new Scanner(file3);
        for (int i = 0; i < ndtMain.getDimensions()+4; i++) ls.nextLine(); // skip headers
        while (ls.hasNextLine())
        {
            String line = ls.nextLine(); // e.g. "65.0 85.0 0.05649024553933026"
            String[] peaks  = Strings.explode(line, ' ');
            double[] peakCoords = new double[peaks.length];
            for (int i = 0; i < ndtMain.getDimensions(); i++)
                peakCoords[i] = Double.parseDouble(peaks[i]);
            
            for (int i = 0; i < ndtMain.getDimensions(); i++)
            {
                minBounds[i] = peakCoords[i] - degrees[i];
                maxBounds[i] = peakCoords[i] + degrees[i];
            }
            if (verbose)
            {
                System.err.print("Sampling around peak (");
                for (int i = 0; i < peakCoords.length-2; i++)  System.err.print(peakCoords[i]+",");
                System.err.print(peakCoords[peakCoords.length-2]+"): from (");
                for (int i = 0; i < minBounds.length-1; i++)   System.err.print(minBounds[i]+",");
                System.err.print(minBounds[minBounds.length-1]+") to (");
                for (int i = 0; i < maxBounds.length-1; i++)   System.err.print(maxBounds[i]+",");
                System.err.println(maxBounds[maxBounds.length-1]+")...");
            }
            takeSamples(new double[ndtMain.getDimensions()], 0); // will use new min/maxBounds!
        }
        
        //{{{ old "above vs. below" code
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

//{{{ takeSamples()
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
                for(int i = 0; i < coords.length; i++)
                    System.out.print(df.format(coords[i])+":");
                System.out.println(mainVal+":"+checkVal);
            }
        }
        else
        {
            for(int i = 0; i < samples[depth]; i++)
            {
                //double frac = (i+0.5)/(samples[depth]);
                double shift = 0.0;   if (1.0*samples[depth] % 2 == 1) shift = 0.5;
                double frac = (i+shift)/(samples[depth]);
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
        ndtMain     = NDimTable_Dense.createFromText(in1);
        ndtCheck    = NDimTable_Dense.createFromText(in2);
        in1.close();
        in2.close();
        
        minBounds = ndtMain.getMinBounds();
        maxBounds = ndtMain.getMaxBounds();
        
        if (samples == null || samples.length != ndtMain.getDimensions() || samples.length != ndtCheck.getDimensions() || (degrees != null && degrees.length != ndtMain.getDimensions()))
            throw new IllegalArgumentException("Samples not specified, or mismatch in tables and/or number of samples");
        if ((file3 == null && degrees != null) )
        {
            System.err.println("Hills modal peaks file and -degrees=#[,#,...] flag must accompany each other!");
            System.exit(0);
        }
        
        boolean allZero = true;
        if (degrees != null) for (int degree : degrees) if (degree != 0) allZero = false;
        if (allZero || (file3 != null && degrees == null))
        {
            System.err.println("Current -degrees=0,0,... => changing to -sample=1,1,...");
            degrees = new int[samples.length];
            for (int i = 0; i < samples.length; i++) 
            {
                degrees[i] = 0;   samples[i] = 1;
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
            takeSamplesAroundPeaks();
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

