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
    File        file1 = null, file2 = null;
    NDimTable   ndtMain = null, ndtCheck = null;
    int[]       samples = null;
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
                double frac = (i+0.5)/(samples[depth]);
                coords[depth] = (1.0-frac)*minBounds[depth] + frac*maxBounds[depth];
                takeSamples(coords, depth+1);
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
            throw new IllegalArgumentException("Must specify two file names");
        
        // Load tables from disk
        InputStream in1 = new FileInputStream(file1);
        InputStream in2 = new FileInputStream(file2);
        ndtMain     = NDimTable_Dense.createFromText(in1);
        ndtCheck    = NDimTable_Dense.createFromText(in2);
        in1.close();
        in2.close();
        
        minBounds = ndtMain.getMinBounds();
        maxBounds = ndtMain.getMaxBounds();
        
        if(samples == null || samples.length != ndtMain.getDimensions() || samples.length != ndtCheck.getDimensions())
            throw new IllegalArgumentException("Samples not specified, or mismatch in tables and/or number of samples");
        
        // Take N samples in each dimension and print them out
        System.out.print("# ");
        for(int i = 1; i <= ndtMain.getDimensions(); i++)
            System.out.print("chi"+i+":");
        System.out.println("main:check");
        takeSamples(new double[ndtMain.getDimensions()], 0);
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
            else if(flag.equals("-1ge"))
            {
                mainGE = Double.parseDouble(param);
            }
            else if(flag.equals("-2ge"))
            {
                checkGE = Double.parseDouble(param);
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

