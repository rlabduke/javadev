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
* <code>PCAPlotter</code> is a utility class for examining principal components
* of certain atoms' motion as analyzed by singular value decomposition (SVD). 
* It takes in a PDB containing the original coordinates of the atoms of interest
* and a comma-delimited text file showing principal components for displacements
* and outputs a kinemage of "arrows" pointing from the original positions to 
* where the PC's would move those atoms.
* The user specifies which principal component he or she wants (#1, #2, ...)
*
* Copyright (C) Daniel Keedy, November 18, 2007.
*/
public class PCAPlotter //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    boolean verbose = false;
    File orig = null;
    File u    = null;
    int pc = 1;
    ArrayList<Triple> origCoords = null;
    ArrayList<String> atomNames = null;
    ArrayList<Triple> pcs = null;
    String color = "red";
    double scale = 1;
    String group = null;
    
//}}}

//{{{ Constructor(s)
//##################################################################################################
    public PCAPlotter()
    {
        super();
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
        if (verbose)
        {
            System.out.println("Finished parsing args...");
            System.out.println("U matrix file is '"+u+"'");
            System.out.println("Orig coords PDB file is '"+origCoords+"'");
        }
        
        getPCs();
        getOrigCoords();
        if (pcs.size() != origCoords.size())
        {
            System.err.println("Need same # coords in U matrix and orig coords,"
                +" not "+pcs.size()+" for U vs. "+origCoords.size()+" for orig coords!");
            System.exit(0);
        }
        
        makeKin();
        
    }

    public static void main(String[] args)
    {
        PCAPlotter pcap = new PCAPlotter();
        pcap.Main(args);
    }
//}}}

//{{{ parseArgs
//##################################################################################################
    public void parseArgs(String[] args)
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
        
        // Checks
        if (u == null)
        {
            System.err.println("Can't open U matrix text file!");
            System.exit(0);
        }
        if (orig == null)
        {
            System.err.println("Can't open PDB file with specific atoms' original coordinates!");
            System.exit(0);
        }
    }
//}}}

//{{{ interpretArg, interpretFlag
//##################################################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
        if (u == null)          u = new File(arg);
        else if (orig == null)  orig = new File(arg);
        else
            System.err.println("Couldn't understand flag "+arg+"... "
                +" Already have U matrix and orig PDB coords!");
    }

    public void interpretFlag(String flag, String param)
    {
        // Look thru flags
        if (flag.equals("-help") || flag.equals("-h"))
        {
            System.err.println();
            System.err.println("  Usage: java PCAPlotter [U_matrix_text_file]"
                +" [atoms'_orig_coords.pdb]");
            System.err.println();
            System.exit(0);
        }
        else if (flag.equals("-pc"))
            pc = Integer.parseInt(param);
        else if (flag.equals("-color"))
            color = param;
        else if (flag.equals("-scale"))
            scale = Integer.parseInt(param);
        else if (flag.equals("-group"))
            group = param;
        else if (flag.equals("-verbose") || flag.equals("-v"))
            verbose = true;
        else
            System.out.println("Couldn't understand flag "+flag);
    }
//}}}

//{{{ getPCs
//##################################################################################################
    public void getPCs()
    {
        // This method will read through the U matrix text file 
        // and populate Triple[] pcs.
        if (verbose) System.out.println("Starting getPCs...");
        try
        {
            // U matrix
            pcs = new ArrayList<Triple>();
            Scanner s = new Scanner(u);
            ArrayList<String> xyz = new ArrayList<String>();
            
            while (s.hasNextLine())
            {
                String line = s.nextLine();
                Scanner ls = new Scanner(line).useDelimiter(",");
                
                // Find correct column holding desired PC.
                // If pc == 1, dont' skip any columns and use PC #1
                for (int i = 0; i < pc - 1; i ++)
                    ls.next(); // skip this PC
                xyz.add(ls.next());
                
                if (xyz.size() >= 3)
                {
                    Triple point = new Triple(
                        Double.parseDouble(xyz.get(0)), 
                        Double.parseDouble(xyz.get(1)), 
                        Double.parseDouble(xyz.get(2)));
                    pcs.add(point);
                    xyz = new ArrayList<String>(); // reset for next x,y,z
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't read input file: '"+u+"'!");
            System.exit(0);
        }
        
        // OLD METHOD -- got x,y,z from same line (wrong!)
//       try
//       {
//           // U matrix
//           pcs = new ArrayList<Triple>();
//           Scanner s = new Scanner(u);
//           
//           // If pc == 1, dont' skip any lines and use PC #1
//           for (int i = 0; i < pc - 1; i ++)
//               if (s.hasNextLine())
//                   s.nextLine();
//           
//           String line = s.nextLine();
//           Scanner ls = new Scanner(line).useDelimiter(",");
//           while (ls.hasNext())
//           {
//               Triple pos = new Triple();
//               pos.setX(Double.parseDouble(ls.next()));
//               pos.setY(Double.parseDouble(ls.next()));
//               pos.setZ(Double.parseDouble(ls.next()));
//               pcs.add(pos);
//           }
//       }
//       catch (FileNotFoundException fnfe)
//       {
//           System.err.println("Can't read input file(s)!");
//           System.exit(0);
//       }
    }
//}}}

//{{{ getOrigCoords
//##################################################################################################
    public void getOrigCoords()
    {
        // This method will read through the original PDB 
        // and populate Triple[]s origCoords.
        if (verbose) System.out.println("Starting getOrigCoords...");
        try
        {
            // Orig PDB
            atomNames = new ArrayList<String>();
            origCoords = new ArrayList<Triple>();
            Scanner s = new Scanner(orig);
            while (s.hasNextLine())
            {
                String line = s.nextLine();
                if (verbose) System.out.println("\nDoing output for line '"+line+"'...");
                
                if (line.indexOf("ATOM") >= 0)
                {
                    // Read name and orig coords from this line
                    Triple pos = new Triple();
                    pos.setX(Double.parseDouble(line.substring(30,38).trim()));
                    pos.setY(Double.parseDouble(line.substring(38,46).trim()));
                    pos.setZ(Double.parseDouble(line.substring(46,54).trim()));
                    origCoords.add(pos);
                    
                    String atomName = line.substring(11,29);
                    atomNames.add(atomName);
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't read input file(s)!");
            System.exit(0);
        }
    }
//}}}

//{{{ makeKin
//##################################################################################################
    public void makeKin()
    {
        // This method will write a group in kin format for the specified (or
        // default of 1) principal component, with a vectorlist for "arrows" 
        // and a balllist for "arrowheads", and with masters for that principal
        // component on those two lists so the equivalent K* or MD principal
        // components can be turned on together in the final kinemage. 
        PrintStream out = System.out;
        if (group == null)
            out.println("@group {pc "+pc+"} dominant animate");
        else
            out.println("@group {"+group+"} dominant animate");
        
        // Calculate arrows
        ArrayList<Triple> arrows = new ArrayList<Triple>();
        for (int i = 0; i < origCoords.size(); i ++)
        {
            Triple origCoord = origCoords.get(i);
            Triple princComp = pcs.get(i);
            princComp.mult(scale);  // default: 1
            if (verbose)
                System.out.println("Scaling princComp & (therefore arrow) by "+scale+"...");
            double x = origCoord.getX() + princComp.getX();
            double y = origCoord.getY() + princComp.getY();
            double z = origCoord.getZ() + princComp.getZ();
            Triple arrow = new Triple(x, y, z);
            arrows.add(arrow);
        }
        
        // Vectorlist for arrows
        out.println("@vectorlist {pc arrows} master= {pc "+pc+"} color= "+color);
        for (int i = 0; i < origCoords.size(); i ++)
        {
            Triple origCoord = origCoords.get(i);
            Triple arrow = arrows.get(i);
            out.println("{"+atomNames.get(i)+"}P "+
                origCoord.getX()+" "+origCoord.getY()+" "+origCoord.getZ());
            out.println("{"+atomNames.get(i)+"} "+
                arrow.getX()+" "+arrow.getY()+" "+arrow.getZ());
        }
        
        // Balllist for arrowheads
        out.println("@balllist {pc arrowheads} master= {pc "+pc+"} color= "+color+" radius= 0.07");
        for (int i = 0; i < origCoords.size(); i ++)
        {
            Triple arrow = arrows.get(i);
            out.println("{"+atomNames.get(i)+"} "+
                arrow.getX()+" "+arrow.getY()+" "+arrow.getZ());
        }
    }
//}}}

}//class

