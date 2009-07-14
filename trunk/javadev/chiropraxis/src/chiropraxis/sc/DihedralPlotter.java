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
import driftwood.util.Strings;
//}}}
/**
* <code>DihedralPlotter</code> plots dihedrals and their associated probability
* values in the same multi-dimensional space.
* 
* Input:
*  - Silk pct            '.data' file
*  - Silk RotamerSampler '.list' file - adds sampled rotamers (opt'l)
*
* The template for the structure of this class was roughly silk.util.RotamerSampler.
*
* <p>Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Apr 14 2008
*/
public class DihedralPlotter //extends ... implements ...
{
//{{{ Constants
    PrintStream o = System.out;
    PrintStream e = System.err;
    static DecimalFormat df  = new DecimalFormat("###.#");
    static DecimalFormat df2 = new DecimalFormat("#.#########");
//}}}

//{{{ Variable definitions
//##############################################################################
    
    boolean verbose    = false;
    
    /** If false, @kinemage and axis are drawn */
    boolean append     = false;
    /** If provided, used for group name */
    String  label      = null;
    
    File    pctFile    = null;
    File    sampFile   = null;
    
    /** Replicate points above this value in a separate list. Derived from cmdline: -cutoff=# */
    float   sampCutoff = Float.NaN;
    /** Scale probability values to min out at this floor and max out at this ceiling */
    float   probFlor   = 0;
    float   probCeil   = 100;
    
    /** Includes prob at end, so #dihedrals + 1 */
    int     nDim       = Integer.MAX_VALUE;
    /** Max value derived from data; used internally for normalization */
    float   maxProb    = Float.NaN;
    
    /*
    int[]   min        = null;  // actual min/max of data; will be used to
    int[]   max        = null;  // shift up to a 0,360 range (does not rescale!)
    boolean scale      = false; // actually rescales everything according to min/max
    */
    /** Determines how axes for dihedrals are drawn; value axis is always 0 to probCeil.
    * Derived from cmdline: -bounds=xmin,xmax,ymin,ymax,... */
    int[]   bounds     = null;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DihedralPlotter()
    {
        super();
    }
//}}}

//{{{ doChecks
//##############################################################################
    public void doChecks() throws FileNotFoundException, IllegalArgumentException
    {
        // Check #1
        if(pctFile == null) throw new IllegalArgumentException("*** Must specify at least one filename");
        
        // Get facts about input data
        Scanner s1 = new Scanner(pctFile);
        while (s1.hasNextLine())
        {
            String line = s1.nextLine();
            if(!line.startsWith("#"))
            {
                // Number of dimensions
                String[] parts = Strings.explode(line, ' ');
                if(parts.length == 1) parts = Strings.explode(line, ':');
                nDim = parts.length;
                
                // Max probability (for normalization)
                float prob = Float.parseFloat(parts[parts.length-1]);
                if(Float.isNaN(maxProb)) maxProb = prob;
                else if(prob > maxProb)  maxProb = prob;
            }
        }
        if(verbose) e.println("Found "+(nDim-1)+" dihedral(s) plus 1 value");
        
        // Another check
        if(bounds == null)
        {
            if(verbose) e.println("Didn't provide -bounds=xmin,xmax,... so using 0,360");
            bounds = new int[2*(nDim-1)];
            for(int i = 0; i < 2*(nDim-1); i++)
            {
                //bounds[i]   = 0;
                //bounds[i+1] = 360;
                if(i%2 == 0) bounds[i] = 0;
                else         bounds[i] = 360;
            }
        }
        
        // Last one
        if(nDim == Integer.MAX_VALUE)
        {
            System.err.println("*** Couldn't figure out number of dimensions!  Quitting...");
            System.exit(0);
        }
        
        // ... OK, one more
        if(label == null)
        {
            String f = pctFile.getName();
            if(f.indexOf(".") != -1) label = f.substring(0,f.indexOf("."));
            else                     label = f;
        }
    }
//}}}

//{{{ doKin
//##############################################################################
    public void doKin() throws FileNotFoundException
    {
        if(!append)
        {
            o.println("@kinemage {"+label+"}");
            doAxes();
        }
        doHeader();
        doPct();
        if(sampFile != null) doSamp();
        o.flush();
    }
//}}}

//{{{ doAxes
//##############################################################################
    public void doAxes()
    {
        o.print(
            "@group {axes} dominant\n"+
            "@vectorlist {axes} color= white width= 1\n");
        if(nDim == 2) o.print(
            "{  0 "+probFlor+"}P   0 "+probFlor+"\n"+
            "{  0 "+probCeil+"}    0 "+probCeil+"\n"+
            "{360 "+probCeil+"}  360 "+probCeil+"\n"+
            "{360 "+probFlor+"}  360 "+probFlor+"\n"+
            "{  0 "+probFlor+"}    0 "+probFlor+"\n");
        else o.print(   
            "{  0   0 "+probFlor+"}P   0   0 "+probFlor+"\n"+
            "{  0 360 "+probFlor+"}    0 360 "+probFlor+"\n"+
            "{360 360 "+probFlor+"}  360 360 "+probFlor+"\n"+
            "{360   0 "+probFlor+"}  360   0 "+probFlor+"\n"+
            "{  0   0 "+probFlor+"}    0   0 "+probFlor+"\n"+
            
            "{  0   0 "+probCeil+"}P   0   0 "+probCeil+"\n"+
            "{  0 360 "+probCeil+"}    0 360 "+probCeil+"\n"+
            "{360 360 "+probCeil+"}  360 360 "+probCeil+"\n"+
            "{360   0 "+probCeil+"}  360   0 "+probCeil+"\n"+
            "{  0   0 "+probCeil+"}    0   0 "+probCeil+"\n"+
            
            "{  0   0 "+probFlor+"}P   0   0 "+probFlor+"\n"+
            "{  0   0 "+probCeil+"}    0   0 "+probCeil+"\n"+
            
            "{  0 360 "+probFlor+"}P   0 360 "+probFlor+"\n"+
            "{  0 360 "+probCeil+"}    0 360 "+probCeil+"\n"+
            
            "{360 360 "+probFlor+"}P 360 360 "+probFlor+"\n"+
            "{360 360 "+probCeil+"}  360 360 "+probCeil+"\n"+
            
            "{360   0 "+probFlor+"}P 360   0 "+probFlor+"\n+"+
            "{360   0 "+probCeil+"}  360   0 "+probCeil+"\n");            
    }
//}}}

//{{{ doHeader
//##############################################################################
    public void doHeader()
    {
        if(nDim == 2) o.println("@flat");
        if(nDim >= 4) o.println("@1axischoice 1 2 "+nDim);
        o.print("@dimensions ");
        for(int i = 1; i < nDim; i++) o.print(" {chi"+i+"?}");
        o.println(" {norm'd probability}");
        o.print("@dimminmax ");
        for(int i = 0; i < 2*(nDim-1); i++) o.print(" "+bounds[i]);//0 360");//" "+min[i]+" "+max[i]);
        o.println();
        o.println("@group {"+label+"} animate dominant");
    }
//}}}

//{{{ doPct
//##############################################################################
    public void doPct() throws FileNotFoundException
    {
        o.println("@balllist {pct} master= {pct} dimension= "+nDim);
        Scanner s = new Scanner(pctFile);
        while (s.hasNextLine())
        {
            String line = s.nextLine();
            if(!line.startsWith("#"))
            {
                // Point ID
                String[] parts = Strings.explode(line, ' ');
                if(parts.length == 1)  parts = Strings.explode(line, ':');
                float[] coords = new float[parts.length];
                for(int i = 0; i < parts.length; i++)  coords[i] = Float.parseFloat(parts[i]);
                o.print( "{"+df.format(coords[0]) );
                for(int i = 1; i < coords.length-1; i++)  o.print( ", "+df.format(coords[i]) );
                o.print( ", pct="+df2.format(coords[coords.length-1])+"} " );
                
                // Radius
                float prob = coords[coords.length-1];
                float rad = prob*2; // radii scale linearly
                //double rad = 2 * Math.pow( (3*prob)/(4*Math.PI) , 0.333); // volumes scale linearly
                o.print("r="+rad+" ");
                
                // Coloring
                if(prob < 0.1)                     o.print("peachtint ");
                else if(prob >= 0.1 && prob < 0.2) o.print("peach ");
                else                               o.print("orange ");
                
                // Actual multi-D coordinates
                /*
                for(int i = 0; i < coords.length-1; i++)
                {
                    if(scale)
                    {
                        double scaledCoord = ( (coords[i]-1.0*min[i]) / (1.0*max[i]-1.0*min[i]) ) * 360;
                        coords[i] = scaledCoord;
                    }
                    else
                    {
                        // e.g. -85 from -90->90 data => -85+(0--90) = -85+90 = 5
                        coords[i] = coords[i] + (0 - min[i]);
                        if(coords[i] > 360)  coords[i] = coords[i] - 360;
                    }
                }
                */
                for(int i = 0; i < coords.length-1; i++)
                {
                    // e.g. -85 from -90->90 data => -85+(0--90) = -85+90 = 5
                    coords[i] = coords[i] + (0 - bounds[2*i]);        // min
                    if(coords[i] > 360)  coords[i] = coords[i] - 360; // max
                }
                
                for(int i = 0; i < coords.length-1; i++) o.print( df.format(coords[i])+" " );
                float normProb = probCeil * (prob / maxProb);
                o.println( df2.format(normProb) );
            }
        }
    }
//}}}

//{{{ doSamp
//##############################################################################
    public void doSamp() throws FileNotFoundException
    {
        o.println("@balllist {samp} radius= 2.0 color= green master= {samp} dimension= "+nDim);
        Scanner s = new Scanner(sampFile);
        while (s.hasNextLine())
        {
            String line = s.nextLine();
            if(!line.startsWith("#"))
            {
                // Point ID
                String[] parts = Strings.explode(line, ' ');
                if(parts.length == 1) parts = Strings.explode(line, ':');
                float[] coords = new float[parts.length];
                for(int i = 0; i < parts.length; i++) coords[i] = Float.parseFloat(parts[i]);
                o.print( "{"+df.format(coords[0]) );
                // skip last 2 instead of 1 columns b/c .list files report stat AND pct, not just pct
                for(int i = 1; i < coords.length-2; i++) o.print( ", "+df.format(coords[i]) );
                o.print( ", pct="+df2.format(coords[coords.length-1])+"} " );
                
                // Actual multi-D coordinates
                /*
                for(int i = 0; i < coords.length-2; i++)
                {
                    if(scale)
                    {
                        double scaledCoord = ( (coords[i]-1.0*min[i]) / (1.0*max[i]-1.0*min[i]) ) * 360;
                        coords[i] = scaledCoord;
                    }
                    else
                    {
                        // e.g. -85 from -90->90 data => -85+(0--90) = -85+90 = 5
                        coords[i] = coords[i] + (0 - min[i]); 
                        if(coords[i] > 360)  coords[i] = coords[i] - 360;
                    }
                }
                */
                for(int i = 0; i < coords.length-2; i++)
                {
                    // e.g. -85 from -90->90 data => -85+(0--90) = -85+90 = 5
                    coords[i] = coords[i] + (0 - bounds[2*i]);        // min
                    if(coords[i] > 360)  coords[i] = coords[i] - 360; // max
                }
                
                // skip last 2 instead of 1 columns b/c .list files report stat AND pct, not just pct
                for(int i = 0; i < coords.length-2; i++) o.print( df.format(coords[i])+" " );
                float prob = coords[coords.length-1];
                float normProb = probCeil * (prob / maxProb);
                o.println( df2.format(normProb) );
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
        doChecks();
        doKin();
    }

    public static void main(String[] args)
    {
        DihedralPlotter mainprog = new DihedralPlotter();
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
            InputStream is = getClass().getResourceAsStream("DihedralPlotter.help");
            if(is == null)
            {
                System.err.println(
                    "\nUsage: java DihedralPlotter fromsilk.data [fromsilk-rotasamp.list] [opts]");
                System.err.println(
                    "\n*** Unable to locate help information in 'DihedralPlotter.help' ***\n");
            }
            else
            {
                try { streamcopy(is, o); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.sc.DihedralPlotter");
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
        if(pctFile == null)         pctFile  = new File(arg);
        else if(sampFile == null)   sampFile = new File(arg);
        else throw new IllegalArgumentException("Too many file names: "+arg);
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
            else if(flag.equals("-label"))
            {
                label = param;
            }
            else if(flag.equals("-cutoff"))
            {
                try
                {
                    sampCutoff = Float.parseFloat(param);
                }
                catch (NumberFormatException nfe)
                {
                    System.err.println("Can't parse -cutoff="+param+" as a float");
                }
            }
            else if(flag.equals("-bounds"))
            {
                String[] s = Strings.explode(param, ',');
                bounds = new int[s.length];
                for(int i = 0; i < s.length; i++) bounds[i] = Integer.parseInt(s[i]);
            }
            else if(flag.equals("-append"))
            {
                append = true;
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

