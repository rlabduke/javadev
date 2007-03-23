// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package silk;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.*;
//}}}
/**
* <code>SilkCmdLine</code> is responsible for reading command
* line parameters to set up SilkOptions for a run.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Apr 17 13:27:22 EDT 2003
*/
public class SilkCmdLine //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    SilkOptions     opt;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public SilkCmdLine()
    {
        opt = new SilkOptions();
    }
//}}}

//{{{ Main, main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        // Set up options and let user know what we're doing
        opt.fillInMissing();
        if(opt.verbosity >= SilkOptions.V_STANDARD) describeSettings();
        
        // Load data from stdin
        TabDataLoader       loader  = new TabDataLoader(opt);
        LineNumberReader    in      = new LineNumberReader(new InputStreamReader(System.in));
        Collection          data    = loader.parseReader(in);
        System.err.println("Loaded "+data.size()+" data samples for analysis");
        
        // Process it
        System.err.println("Processing data samples...");
        SilkEngine  engine          = new SilkEngine();
        NDimTable   densityTrace    = engine.processData(data, opt);
        System.err.println("...done.");
        
        // Write it out
        System.err.println("Writing output in "+opt.outputMode+" format...");
        doOutput(data, densityTrace, engine);
        System.err.println("...done.");
    }

    public static void main(String[] args)
    {
        SilkCmdLine mainprog = new SilkCmdLine();
        
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
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error reading data: "+ex.getMessage());
            System.exit(1);
        }
    }
//}}}

//{{{ describeSettings
//##################################################################################################
    void describeSettings()
    {
        // Data dimensions and per-dimension settings
        System.err.println("Processing "+opt.nDim+"-dimensional data:");
        if(opt.label != 0)
            System.err.println("  Label  in col "+opt.label);
        for(int i = 0; i < opt.nDim; i++)
        {
            System.err.print("  x"+(i+1)+"     in col "+opt.coords[i]
                +" bounded by ["+opt.bounds[2*i]+", "+opt.bounds[2*i + 1]+"]"
                +" in "+opt.gridsamples[i]+" bins with wrap="+opt.wrap[i]);
            if(opt.aniso[i] != 1.0)
                System.err.print(" with "+opt.aniso[i]+"x smoothing");
            System.err.println();
        }
        if(opt.weight != 0)
            System.err.println("  Weight in col "+opt.weight);
        
        if(opt.sparse)  System.err.println("Using sparse data storage");
        else            System.err.println("Using dense data storage");
        
        // Operations
        System.err.println("Calculating "+opt.operation+" followed by "+opt.postop+" and scaling by "+opt.scale);
        if(opt.operation != SilkOptions.OP_HISTOGRAM)
        {
            System.err.print("  Using half-width of "+opt.halfwidth);
            if(opt.twopass) System.err.println(" ("+opt.ddhalfwidth+" on second pass with lambda="+opt.lambda+")");
            else            System.err.println();
        }
        if(opt.hillClimb) System.err.println("Squashing values below "+opt.hillSquash+", climbing hills, and labeling peaks");
    }
//}}}

//{{{ doOutput
//##################################################################################################
    void doOutput(Collection data, NDimTable densityTrace, SilkEngine engine)
    {
        DecimalFormat df = null;
        
        if(opt.outputMode == SilkOptions.OUTPUT_VALUE_LAST)
            densityTrace.writeText(opt.outputSink, df, false);
        else if(opt.outputMode == SilkOptions.OUTPUT_VALUE_FIRST)
            densityTrace.writeText(opt.outputSink, df, true);
        else if(opt.outputMode == SilkOptions.OUTPUT_NDFT)
        {
            try
            {
                DataOutputStream dos = new DataOutputStream(opt.outputSink);
                ((NDimTable_Dense) densityTrace).writeNDFT(dos);
                dos.close();
            }
            catch(IOException ex)
            {
                ex.printStackTrace();
                System.err.println("Unable to write output do to I/O error: "+ex.getMessage());
            }
        }
        else if(opt.outputMode == SilkOptions.OUTPUT_KINEMAGE)
        {
            // wrapping the data means it will be plotted correctly
            engine.wrapData(data, opt);
            PrintStream ps = new PrintStream(opt.outputSink);
            ps.println("@kinemage 1");
            ps.println("@caption");
            ps.println(opt.title);
            ps.println();
            KinfilePlotter plotter = new KinfilePlotter();
            if(opt.nDim == 1)
            {
                ps.println("@flat");
                plotter.plot1D(opt.outputSink, data);
                if(opt.contours != null)
                    plotter.contour1D(opt.outputSink, opt.contours, densityTrace);
            }
            else if(opt.nDim == 2)
            {
                ps.println("@flat");
                // Experimental attempt at even sampling via modified k-means
                //KMeans kMeans = new KMeans(opt);
                //kMeans.findClusters(data, 100, plotter);
                plotter.plot2D(opt.outputSink, data);
            }
            else if(opt.nDim == 3)
            {
                plotter.plot3D(opt.outputSink, data);
            }
            else throw new IllegalArgumentException("Too many dimensions to create kinemage output!");
            ps.flush();
        }
        else throw new IllegalArgumentException("Unknown output mode: "+opt.outputMode);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ parseArguments, showHelp
//##################################################################################################
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
                { throw new IllegalArgumentException("'"+arg+"' expects to be followed by a parameter"); }
                catch(NumberFormatException ex)
                { throw new IllegalArgumentException("'"+arg+"' expects to be followed by a number"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("SilkCmdLine.help");
            if(is == null) System.err.println("\n*** Unable to locate help information in 'SilkCmdLine.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("silk.SilkCmdLine");
        System.err.println("Copyright (C) 2003 by Ian W. Davis. All rights reserved.");
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
//##################################################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
    }
    
    void interpretFlag(String flag, String param) throws NumberFormatException
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-ndim"))           opt.nDim = Integer.parseInt(param);
        else if(flag.equals("-label"))          opt.label = Integer.parseInt(param);
        else if(flag.equals("-coords"))         opt.coords = explodeInts(param);
        else if(flag.equals("-bounds"))         opt.bounds = explodeDoubles(param);
        else if(flag.equals("-wrap"))           opt.wrap = new boolean[] {true};
        else if(flag.equals("-crop"))           opt.crop = explodeDoubles(param);
        else if(flag.equals("-insep"))          opt.inSep = param.charAt(0);
        else if(flag.equals("-sparse"))         opt.sparse = true;
        else if(flag.equals("-dense"))          opt.sparse = false;
        else if(flag.equals("-weight"))         opt.weight = Integer.parseInt(param);
        else if(flag.equals("-aniso"))          opt.aniso = explodeDoubles(param);
        else if(flag.equals("-gridsize"))       opt.gridsize = explodeDoubles(param);
        else if(flag.equals("-gridsamples"))    opt.gridsamples = explodeInts(param);
        else if(flag.equals("-histogram"))
        {
            opt.operation   = SilkOptions.OP_HISTOGRAM;
        }
        else if(flag.equals("-gaussian"))
        {
            opt.operation   = SilkOptions.OP_GAUSSIAN;
            opt.halfwidth   = Double.parseDouble(param);
        }
        else if(flag.equals("-cosine"))
        {
            opt.operation   = SilkOptions.OP_COSINE;
            opt.halfwidth   = Double.parseDouble(param);
        }
        else if(flag.equals("-twopass"))
        {
            opt.twopass     = true;
            opt.ddhalfwidth = Double.parseDouble(param);
        }
        else if(flag.equals("-lambda"))         opt.lambda = Double.parseDouble(param);
        else if(flag.equals("-no-op"))          opt.postop = SilkOptions.POSTOP_NONE;
        else if(flag.equals("-counts"))         opt.postop = SilkOptions.POSTOP_COUNTS;
        else if(flag.equals("-ln"))             opt.postop = SilkOptions.POSTOP_LN;
        else if(flag.equals("-0to1"))           opt.postop = SilkOptions.POSTOP_0TO1;
        else if(flag.equals("-fraction"))       opt.postop = SilkOptions.POSTOP_FRACTION;
        else if(flag.equals("-energy"))         opt.postop = SilkOptions.POSTOP_ENERGY;
        else if(flag.equals("-scale"))          opt.scale = Double.parseDouble(param);
        else if(flag.equals("-hillclimb"))
        {
            opt.hillClimb = true;
            if(param != null) opt.hillSquash = Double.parseDouble(param);
        }
        else if(flag.equals("-title"))          opt.title = param;
        else if(flag.equals("-first"))          opt.outputMode = SilkOptions.OUTPUT_VALUE_FIRST;
        else if(flag.equals("-ndft"))           opt.outputMode = SilkOptions.OUTPUT_NDFT;
        else if(flag.equals("-kin"))            opt.outputMode = SilkOptions.OUTPUT_KINEMAGE;
        else if(flag.equals("-sigdig"))         opt.sigdig = Integer.parseInt(param);
        else if(flag.equals("-outsep"))         opt.outSep = param.charAt(0);
        else if(flag.equals("-contour"))        opt.contours = explodeDoubles(param);
        else if(flag.equals("-v"))              opt.verbosity = SilkOptions.V_VERBOSE;
        else if(flag.equals("-quiet"))          opt.verbosity = SilkOptions.V_QUIET;
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}

//{{{ explodeInts, explodeDoubles
//##################################################################################################
    int[] explodeInts(String s) throws NumberFormatException
    {
        String[]    strings = Strings.explode(s, ',', true, true);
        int[]       ints    = new int[strings.length];
        for(int i = 0; i < strings.length; i++)
            ints[i] = Integer.parseInt(strings[i]);
        return ints;
    }
    
    double[] explodeDoubles(String s) throws NumberFormatException
    {
        String[]    strings = Strings.explode(s, ',', true, true);
        double[]    doubles = new double[strings.length];
        for(int i = 0; i < strings.length; i++)
            doubles[i] = Double.parseDouble(strings[i]);
        return doubles;
    }
//}}}
}//class

