// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import driftwood.r3.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>AtomicDisplMatrixMaker</code> is a utility class that takes in a PDB
* file containing an ensemble of models and outputs a PDB containing the 
* information necessary for the A matrix in the A=UsigmaV(T) equation for 
* principal component analysis.
* The models in the input PDB must already be aligned to one of the constituent
* models, usually the first one, and possibly done in VMD or by other means.
* 
* Copyright (C) Daniel Keedy, November 18, 2007.
*/
public class AtomicDisplMatrixMaker //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    File file = null;
    boolean verbose;
    TreeMap<String,Triple> namesToAvgs = null;
    
//}}}

//{{{ Constructor(s)
//##################################################################################################
    public AtomicDisplMatrixMaker()
    {
        super();
        verbose = false;
        namesToAvgs = new TreeMap<String,Triple>();
    }
//}}}

//{{{ Main, main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main(String[] args)
    {
        parseArgs(args);
        if (file == null)
        {
            System.err.println("Can't open file '"+args[0]+"'!");
            System.err.println("Need a multi-model PDB file as input!");
            System.exit(0);
        }
        if (verbose)
        {
            System.out.println("Finished parsing args...");
            System.out.println("File is '"+file+"'");
        }
        
        getAvgs();
        makeMatrix();
    }

    public static void main(String[] args)
    {
        AtomicDisplMatrixMaker admm = new AtomicDisplMatrixMaker();
        admm.Main(args);
    }
//}}}

//{{{ parseArgs
//##################################################################################################
    public void parseArgs(String[] args)
    {
        for (String arg : args)
        {
            if (arg.equals("-help") || arg.equals("-h"))
            {
                System.err.println();
                System.err.println("  Usage: java AtomicDisplMatrixMaker [multi-model.pdb]");
                System.err.println();
                System.exit(0);
            }
            else if (!arg.substring(0,1).equals("-"))
                file = new File(args[0]);
            else if (arg.equals("-verbose") || arg.equals("-v"))
                verbose = true;
            else
                System.out.println("Couldn't understand flag "+arg);
        }
    }
//}}}

//{{{ getAvgs, getAvg
//##################################################################################################
    public void getAvgs()
    {
        if (verbose) System.out.println("Starting getAvgs...");
        try
        {
            // Get initial String descriptors of atoms.
            // This is PDB columns 1 to 30, so 0 to 29 in Java
            // "ATOM      1  N   ASP A 235     "
            ArrayList<String> names = new ArrayList<String>();
            boolean gotNames = false;
            Scanner s = new Scanner(file);
            while (s.hasNextLine() && !gotNames)
            {
                String line = s.nextLine();
                if (verbose) System.out.println("Looking at line '"+line+"'...");
                if (line.indexOf("ATOM") >= 0 && line.indexOf("END") < 0)
                {
                    names.add(line.substring(0,30));
                    if (verbose) System.out.println("Adding name '"+
                        line.substring(0,29)+"' to the list...");
                }
                else if (line.indexOf("END") >= 0)
                    gotNames = true;
            }
            if (verbose) System.out.println("Got all atoms' names...");
            
            // For each atom descriptor, go through the entire file and get an 
            // average position, then map the descriptor to that average position.
            for (String name : names)
            {
                Triple avg = getAvg(name);
                namesToAvgs.put(name, avg);
            }
            if (verbose) System.out.println("Made map of 'names' --> avg positions...");
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't read input file!");
        }
    }

    public Triple getAvg(String name)
    {
        if (verbose) System.out.println("Getting avg positon for '"+name+"'");
        
        // Initialize data holders
        Triple sums = new Triple(0,0,0);
        int count = 0;
        
        // Look through multi-model PDB file
        try
        {
            Scanner s = new Scanner(file);
            while (s.hasNextLine())
            {
                String line = s.nextLine();
                if (line.indexOf(name) >= 0)
                {
                    sums.setX(sums.getX() + Double.parseDouble(line.substring(30,38).trim()));
                    sums.setY(sums.getY() + Double.parseDouble(line.substring(38,46).trim()));
                    sums.setZ(sums.getZ() + Double.parseDouble(line.substring(46,54).trim()));
                    count ++;
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't read input file!");
        }
        
        Triple avg = new Triple(
            sums.getX()/count, sums.getY()/count, sums.getZ()/count);
        if (verbose)
        {
            System.out.println("Average data for name '"+name+"'...");
            System.out.println("   sums.getX() = "+sums.getX());
            System.out.println("   count       = "+count);
            System.out.println("   avg         = "+avg);
            System.out.println();
        }
        return avg;
    }
//}}}

//{{{ makeMatrix
//##################################################################################################
    public void makeMatrix()
    {
        // This method will read back through the multi-model PDB, spitting out
        // exactly what it sees except for replacing all coordinates with what
        // was originally there minus the average position for that coordinate
        // (e.g. each original x for Asp235 CA minus the avg x for Asp235 CA).
        DecimalFormat df = new DecimalFormat("###.###");
        if (verbose) System.out.println("Starting output...");
        try
        {
            Scanner s = new Scanner(file);
            while (s.hasNextLine())
            {
                String line = s.nextLine();
                if (verbose) System.out.println("\nDoing output for line '"+line+"'...");
                
                if (line.indexOf("ATOM") < 0)
                    System.out.println(line);
                else
                {
                    // Read name and orig coords from this line
                    Triple orig = new Triple();
                    orig.setX(Double.parseDouble(line.substring(30,38).trim()));
                    orig.setY(Double.parseDouble(line.substring(38,46).trim()));
                    orig.setZ(Double.parseDouble(line.substring(46,54).trim()));
                    
                    // Recall avg coords for this name and subtract from orig
                    String name = line.substring(0,30);
                    Triple avg = namesToAvgs.get(name);
                    if (verbose) System.out.println("Avg for '"+name+"': "+avg);
                    Triple diff = new Triple(orig.getX()-avg.getX(), 
                        orig.getY()-avg.getY(), orig.getZ()-avg.getZ());
                    
                    // Format strings so exactly 8 characters (pad w/ spaces if nec.)
                    String x = df.format(diff.getX()).toString();
                    String y = df.format(diff.getY()).toString();
                    String z = df.format(diff.getZ()).toString();
                    String[] xyz = new String[3];
                    xyz[0] = x;   xyz[1] = y;   xyz[2] = z;
                    for (int i = 0; i < 3; i ++)
                    {
                        // "35.667" -> length=6 so add 2 spaces -> "  35.667" 
                        String w = xyz[i];
                        int spacesNeeded = 8 - w.length();
                        String wNew = "";
                        for (int j = 0; j < spacesNeeded; j ++)
                            wNew += " ";
                        wNew += w;     // e.g. "  " + "35.667" = "  35.667"
                        xyz[i] = wNew; // simply replace old String w/ new, formatted one
                    }
                    
                    // Reiterate whatever ends the line starting at PDB columsn 55
                    // (Java idx 54)
                    String theRest = line.substring(54);
                    
                    // Print out in PDB format
                    System.out.println(name+xyz[0]+xyz[1]+xyz[2]+theRest);
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't read input file!");
            System.exit(0);
        }
    }
//}}}

}//class

