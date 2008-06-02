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
* <code>DihedralPlotter</code> is a utility program used to plot dihedrals and 
* probability values from a Silk -fraction run (typically files ending in '.data') 
* with e.g. the top5200-angles data into a multi-dimensional kinemage. The output
* is a .kin file of sets of dihedrals and their probabilites in the same space. 
* If a second, '.list', e.g. file from silk.util.RotamerSampler, is also provided,
* the user can see where those sampled rotamers fall in the dihedral/probability
* space. The probability value (e.g. a Silk 'pct' value) for each peak is the 
* last column of each line of the input '__.data' file and is plotted as an 
* additional dimension.
*
* The template for the structure of this class was (roughly) silk.util.RotamerSampler.
*
* <p>Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Apr 14 2008
*/
public class DihedralPlotter //extends ... implements ...
{
//{{{ Constants
    static DecimalFormat df  = new DecimalFormat("###.#");
    static DecimalFormat df2 = new DecimalFormat("#.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean     verbose      = false;
    File        pctFile      = null;
    File        sampFile     = null;
    double      sampCutoff   = Double.NaN;
    int         nDim         = Integer.MAX_VALUE; // includes prob at end (so #chis +1)
    double      maxProb      = Double.NaN;
    String      label        = null;
    int[]       min          = null;  // actual min/max of data; will be used to
    int[]       max          = null;  // shift up to a 0,360 range (does not rescale!)
    boolean     scale        = false; // actually rescales everything according to min/max
    boolean     append       = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DihedralPlotter()
    {
        super();
    }
//}}}

//{{{ doKin
//##############################################################################
    public void doKin() throws FileNotFoundException
    {
        if (!append) 
        {
            if (label == null)  label = pctFile.getName();
            System.out.println("@kinemage {"+label+"}");
        }
        
        //{{{ Axis
        if (!append)  System.out.println(
            "@group {axis} dominant                                  \n"+
            "@vectorlist {axis} color= white width= 1                \n"+      
            "{0, 0, 0} P 0.000 0.000 0.000                           \n"+
            "{180, 0, 0} L 180.000 0.000 0.000                       \n"+
            "{360, 0, 0} L 360.000 0.000 0.000                       \n"+
            "{360, 180, 0} L 360.000 180.000 0.000                   \n"+
            "{360, 360, 0} L 360.000 360.000 0.000                   \n"+
            "{180, 360, 0} L 180.000 360.000 0.000                   \n"+
            "{0, 360, 0} L 0.000 360.000 0.000                       \n"+
            "{0, 180, 0} L 0.000 180.000 0.000                       \n"+
            "{0, 0, 0} L 0.000 0.000 0.000                           \n"+
            "{0, 0, 180} L 0.000 0.000 180.000                       \n"+
            "{0, 0, 360} L 0.000 0.000 360.000                       \n"+
            "{180, 0, 360} L 180.000 0.000 360.000                   \n"+
            "{360, 0, 360} L 360.000 0.000 360.000                   \n"+
            "{360, 180, 360} L 360.000 180.000 360.000               \n"+
            "{360, 360, 360} L 360.000 360.000 360.000               \n"+
            "{180, 360, 360} L 180.000 360.000 360.000               \n"+
            "{0, 360, 360} L 0.000 360.000 360.000                   \n"+
            "{0, 180, 360} L 0.000 180.000 360.000                   \n"+
            "{0, 0, 360} L 0.000 0.000 360.000                       \n"+
            "{360, 0, 0} P 360.000 0.000 0.000                       \n"+
            "{360, 0, 180} L 360.000 0.000 180.000                   \n"+
            "{360, 0, 360} L 360.000 0.000 360.000                   \n"+
            "{360, 360, 0} P 360.000 360.000 0.000                   \n"+
            "{360, 360, 180} L 360.000 360.000 180.000               \n"+
            "{360, 360, 360} L 360.000 360.000 360.000               \n"+
            "{0, 360, 0} P 0.000 360.000 0.000                       \n"+
            "{0, 360, 180} L 0.000 360.000 180.000                   \n"+
            "{0, 360, 360} L 0.000 360.000 360.000                   \n"+
            "@balllist {origin} color= pink radius= 0.3           \n"+
            "{origin} 0 0 0                                          \n"
        );
        //}}}
        
        //{{{ Header
        if (nDim >= 4)  System.out.println("@1axischoice 1 2 "+nDim);
        System.out.print("@dimensions ");
        for (int i = 1; i < nDim; i ++)  System.out.print(" {chi"+i+"}");
        System.out.println(" {norm'd probability}");
        System.out.print("@dimminmax ");
        for (int i = 1; i <= nDim; i ++)  System.out.print(" 0 360");//" "+min[i]+" "+max[i]);
        System.out.println();
        System.out.println("@group {"+label+"} animate dominant");
        //}}}
        
        //{{{ List of pct points 
        System.out.println("@dotlist {pct} color= peachtint master= {pct} dimension= "+nDim);
        Scanner s = new Scanner(pctFile);
        while (s.hasNextLine())
        {
            String line = s.nextLine();
            if (!line.startsWith("#"))
            {
                // Point ID
                String[] parts = Strings.explode(line, ' ');
                if (parts.length == 1)  parts = Strings.explode(line, ':');
                
                double[] coords = new double[parts.length];
                for (int i = 0; i < parts.length; i ++)
                    coords[i] = Double.parseDouble(parts[i]);
                
                System.out.print( "{"+df.format(coords[0]) );
                for (int i = 1; i < coords.length-1; i ++)
                    System.out.print( ", "+df.format(coords[i]) );
                System.out.print( ", pct="+df2.format(coords[coords.length-1])+"} " );
                
                // Actual multi-D coordinates
                for (int i = 0; i < coords.length-1; i ++)
                {
                    if (scale)
                    {
                        double scaledCoord = ( (coords[i]-1.0*min[i]) / (1.0*max[i]-1.0*min[i]) ) * 360;
                        coords[i] = scaledCoord;
                    }
                    else
                    {
                        // e.g. -85 from -90->90 data => -85+(0--90) = -85+90 = 5
                        coords[i] = coords[i] + (0 - min[i]);
                        if (coords[i] > 360)  coords[i] = coords[i] - 360;
                    }
                }
                for (int i = 0; i < coords.length-1; i ++)
                    System.out.print( df.format(coords[i])+" " );
                double prob = coords[coords.length-1];
                double normProb = 360 * (prob / maxProb);
                System.out.println( df.format(normProb) );
            }
        }
        //}}}
        
        //{{{ (Opt'l) List of pct points above the user-imposed cutoff
        if (!Double.isNaN(sampCutoff))
        {
            System.out.println("@dotlist {pct >"+sampCutoff+"} color= bluetint "
                +"master= {pct >"+sampCutoff+"} dimension= "+nDim);
            s = new Scanner(pctFile);
            while (s.hasNextLine())
            {
                String line = s.nextLine();
                if (!line.startsWith("#"))
                {
                    String[] parts = Strings.explode(line, ' ');
                    if (parts.length == 1)  parts = Strings.explode(line, ':');
                    
                    double[] coords = new double[parts.length];
                    for (int i = 0; i < parts.length; i ++)
                        coords[i] = Double.parseDouble(parts[i]);
                    
                    double prob = coords[coords.length-1];
                    if (prob > sampCutoff)
                    {
                        // Point ID
                        System.out.print( "{"+df.format(coords[0]) );
                        for (int i = 1; i < coords.length-1; i ++)
                            System.out.print( ", "+df.format(coords[i]) );
                        System.out.print( ", pct="+df2.format(coords[coords.length-1])+"} " );
                        
                        // Actual multi-D coordinates
                        for (int i = 0; i < coords.length-1; i ++)
                        {
                            if (scale)
                            {
                                double scaledCoord = ( (coords[i]-1.0*min[i]) / (1.0*max[i]-1.0*min[i]) ) * 360;
                                coords[i] = scaledCoord;
                            }
                            else
                            {
                                // e.g. -85 from -90->90 data => -85+(0--90) = -85+90 = 5
                                coords[i] = coords[i] + (0 - min[i]); 
                                if (coords[i] > 360)  coords[i] = coords[i] - 360;
                            }
                        }
                        for (int i = 0; i < coords.length-1; i ++)
                            System.out.print( df.format(coords[i])+" " );
                        double normProb = 360 * (prob / maxProb);
                        System.out.println( df.format(normProb) );
                    }
                }
            }
        }
        //}}}
        
        //{{{ (Opt'l) List of samp points
        if (sampFile != null)
        {
            System.out.println("@balllist {samp} radius= 4 color= green master= {samp} dimension= "+nDim);
            s = new Scanner(sampFile);
            while (s.hasNextLine())
            {
                String line = s.nextLine();
                if (!line.startsWith("#"))
                {
                    // Point ID
                    String[] parts = Strings.explode(line, ' ');
                    if (parts.length == 1)  parts = Strings.explode(line, ':');
                    
                    double[] coords = new double[parts.length];
                    for (int i = 0; i < parts.length; i ++)
                        coords[i] = Double.parseDouble(parts[i]);
                    
                    System.out.print( "{"+df.format(coords[0]) );
                    for (int i = 1; i < coords.length-2; i ++) // skip -2 b/c it's stat, not pct
                        System.out.print( ", "+df.format(coords[i]) );
                    System.out.print( ", pct="+df2.format(coords[coords.length-1])+"} " );
                    
                    // Actual multi-D coordinates
                    for (int i = 0; i < coords.length-2; i ++)
                    {
                        if (scale)
                        {
                            double scaledCoord = ( (coords[i]-1.0*min[i]) / (1.0*max[i]-1.0*min[i]) ) * 360;
                            coords[i] = scaledCoord;
                        }
                        else
                        {
                            // e.g. -85 from -90->90 data => -85+(0--90) = -85+90 = 5
                            coords[i] = coords[i] + (0 - min[i]); 
                            if (coords[i] > 360)  coords[i] = coords[i] - 360;
                        }
                    }
                    for (int i = 0; i < coords.length-2; i ++)
                        System.out.print( df.format(coords[i])+" " );
                    double prob = coords[coords.length-1];
                    double normProb = 360 * (prob / maxProb);
                    System.out.println( df.format(normProb) );
                }
            }
        }
        //}}}
        
        System.out.flush();
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        // Checks
        if(pctFile == null)throw new IllegalArgumentException("Must specify at leat one file name");
        if(scale && (min == null || max == null))
        {
            System.err.println("Need -min=# and -max=# if using -scale");
            scale = false;
        }
        
        // Get facts about input data
        Scanner s1 = new Scanner(pctFile);
        while (s1.hasNextLine())
        {
            String line = s1.nextLine();
            if (!line.startsWith("#"))
            {
                // Number of dimensions
                String[] parts = Strings.explode(line, ' ');
                if (parts.length == 1)  parts = Strings.explode(line, ':');
                nDim = parts.length;
                
                // Max probability (for normalization)
                double prob = Double.parseDouble(parts[parts.length-1]);
                if (Double.isNaN(maxProb))  maxProb = prob;
                else if (prob > maxProb)    maxProb = prob;
            }
        }
        
        // Another check
        if (min == null || max == null)
        {
            System.err.println("min and/or max not provided => using 0,360 in all dimensions");
            if (min == null)
            {
                min = new int[nDim];
                for (int i = 0; i < nDim; i ++)  min[i] = 0;
            }
            if (max == null)
            {
                max = new int[nDim];
                for (int i = 0; i < nDim; i ++)  max[i] = 360;
            }
        }
        
        // Output
        if (nDim != Integer.MAX_VALUE)  doKin();
        else
        {
            System.err.println("Couldn't figure out # dims...");
            System.exit(0);
        }
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
                try { streamcopy(is, System.out); }
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
                    sampCutoff = Double.parseDouble(param);
                }
                catch (NumberFormatException nfe)
                {
                    System.err.println("Can't parse -cutoff="+param+" as a double");
                }
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
            else if(flag.equals("-scale"))
            {
                scale = true;
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

