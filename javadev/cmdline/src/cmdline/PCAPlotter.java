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
    File s    = null;
    int[] pcs = new int[1];
    ArrayList<Triple> origCoords = null;
    ArrayList<String> atomNames = null;
    ArrayList<Triple[]> princCompArrays = null;
    ArrayList<Triple> avgPrincComps = null;
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
            if (s != null && pcs.length > 1)
                System.out.println("Will average vectors for PCs "+pcs[0]+"-"+pcs[pcs.length-1]);
        }
        
        getPCs();
        getOrigCoords();
        
        for (Triple[] princCompArray : princCompArrays)
        {
            if (princCompArray.length != origCoords.size())
            {
                System.err.println("Need same # coords (i.e. atoms) in U matrix and orig coords,"
                    +" not "+princCompArray.length+" for U vs. "+origCoords.size()+" for orig coords!");
                System.exit(0);
            }
        }
        
        if (s != null && pcs.length > 1)
            avgPCs();
        if (s != null && pcs.length <= 1)
            System.err.println("For weighted PC vector averaging, you need to supply both"
                +" an S matrix text file and the -pcs=#-# flag!");
        
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
        if (orig == null)       orig = new File(arg);
        else if (u == null)     u = new File(arg);
        else if (s == null)     s = new File(arg);
        else
            System.err.println("Couldn't understand flag "+arg+"... "
                +" Already have orig PDB coords, U matrix, and S matrix!");
    }

    public void interpretFlag(String flag, String param)
    {
        // Look thru flags
        if (flag.equals("-help") || flag.equals("-h"))
        {
            System.err.println();
            System.err.println("  Usage: java PCAPlotter [only_these_atoms'_orig_coords.pdb]"
                +" [U_matrix_text_file] [S_matrix_text_file (for opt'l avg'ing)]");
            System.err.println();
            System.exit(0);
        }
        else if (flag.equals("-pc") || flag.equals("-pcs"))
        {
            if (param.indexOf("-") < 0)
            {
                pcs = new int[1];
                pcs[0] = Integer.parseInt(param);
            }
            else // e.g. "-pc=1-20"
            {
                int first = Integer.parseInt(param.substring(0, param.indexOf("-")  ));
                int last  = Integer.parseInt(param.substring(   param.indexOf("-")+1));
                int numPcs = last - first + 1; // e.g. 1-20 => 19+1 = 20
                pcs = new int[numPcs];
                for (int i = 0; i < numPcs; i ++)
                    pcs[i] = first + i;
            }
        }
        else if (flag.equals("-color"))
        {
            color = param;
        }
        else if (flag.equals("-scale"))
        {
            scale = Integer.parseInt(param);
        }
        else if (flag.equals("-group"))
        {
            group = param;
        }
        else if (flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else
            System.out.println("Couldn't understand flag "+flag);
    }
//}}}

//{{{ getPCs
//##################################################################################################
    public void getPCs()
    {
        // This method will read through the U matrix text file 
        // and populate ArrayList<Triple[]> princCompArrays.
        if (verbose) System.out.println("Starting getPCs...");
        try
        {
            princCompArrays = new ArrayList<Triple[]>();
            
            for (int pc : pcs) // for each PC
            {
                if (verbose) System.out.println("Scanning U for PC #"+pc+" data...");
                // Put data for each atom into this PC's ArrayList<Triple> princCompAL
                ArrayList<Triple> princCompAL = new ArrayList<Triple>();
                ArrayList<String> xyz = new ArrayList<String>();
                Scanner scan = new Scanner(u);
                while (scan.hasNextLine())
                {
                    String line = scan.nextLine();
                    Scanner ls = new Scanner(line).useDelimiter(",");
                    
                    // Find correct column holding desired PC, e.g. if pc == 1, 
                    // don't skip any columns and use PC #1
                    // (I had previously done "correct row," which is wrong!)
                    for (int i = 0; i < pc - 1; i ++)
                        ls.next(); // skip this PC
                    xyz.add(ls.next());
                    
                    if (xyz.size() >= 3)
                    {
                        Triple point = new Triple(
                            Double.parseDouble(xyz.get(0)), 
                            Double.parseDouble(xyz.get(1)), 
                            Double.parseDouble(xyz.get(2)));
                        princCompAL.add(point);
                        if (verbose) System.out.println("Adding point '"+point+"' to princCompAL");
                        xyz = new ArrayList<String>(); // reset for next x,y,z
                    }
                }
                
                // Convert princCompAL into an array
                if (verbose) System.out.println("Converting princCompAL of size "
                    +princCompAL.size()+" to princCompArray...");
                Triple[] princCompArray = new Triple[princCompAL.size()];
                for (int i = 0; i < princCompAL.size(); i ++)
                    princCompArray[i] = princCompAL.get(i);
                
                // Add Triple[] princCompArray to ArrayList<Triple[]> princCompArrays
                princCompArrays.add(princCompArray);
                if (verbose) System.out.println("Added princCompArray '"+princCompArray
                    +"' of length "+princCompArray.length+" to princCompArrays...");
                
                // Done getting data for this principal component!
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
//           princComps = new ArrayList<Triple>();
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
//               princComps.add(pos);
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
            Scanner scan= new Scanner(orig);
            while (scan.hasNextLine())
            {
                String line = scan.nextLine();
                if (verbose) System.out.println("Doing getOrigCoords for line '"+line+"'...");
                
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

//{{{ avgPCs
//##################################################################################################
    public void avgPCs()
    {
        // Called iff an S matrix is provided and a range of PCs is given with -pcs=#-#.
        // This method will calculate a weighted-average vector for the orientation
        // of each atoms' principal components and place them in the previously null
        // ArrayList<Triple[]> weightedPrincCompArrays.
        try
        {
            // Get weights from [squares of] "singular values" in S matrix
            //  12 0  0   ...
            //  0  2  0   ...
            //  0  0  0.5 ...
            ArrayList<Double> weights = new ArrayList<Double>();
            int count = 0;
            Scanner scan = new Scanner(s);
            // We'll look through every row, not necessarily every column, but
            // that's fine b/c the excess columns are all zeroes anyway to
            // make the S matrix the right shape.
            while (scan.hasNextLine())
            {
                String line = scan.nextLine();
                Scanner ls = new Scanner(line).useDelimiter(",");
                for (int i = 0; i < count; i ++)    ls.next(); // skip token
                double singVal = Double.parseDouble(ls.next());
                //double singValSqrd = Math.pow(singVal,2);
                //weights.add(singValSqrd);
                weights.add(singVal);
                count ++;
            }
            if (verbose) System.out.println("weights: "+weights);
            
            // Normalize weights
            double max = -999;
            for (double weight : weights)
                if (weight > max)   max = weight;
            for (int i = 0; i < weights.size(); i ++)
                weights.set(i, weights.get(i)/max);
            if (verbose) System.out.println("normalized weights: "+weights);
            
            // Check that we have the correct # of weights
            if (weights.size()/3 != origCoords.size())
            {
                System.err.println("Need same # coords (i.e. atoms) in S matrix and orig coords,"
                    +" not "+weights.size()/3+" for S vs. "+origCoords.size()+" for orig coords!");
            }
            
            // Apply weights to principal components
            ArrayList<Triple[]> weightedPrincCompArrays = new ArrayList<Triple[]>();
            for (int i = 0; i < princCompArrays.size(); i ++)
            {
                double weight = weights.get(i); // weight for this PC
                Triple[] princCompArray = princCompArrays.get(i);
                // These Triples concatentate into a single column of U, which 
                // is a principal component of motion. 
                // We will multiply each element of this column vector by the 
                // appropriate weight for this principal component; i.e., we'll
                // multiply every x,y,z in every Triple in a given princCompArray
                // by its associated weight.
                
                // Triple => Triple correspondence
                Triple[] weightedPrincCompArray = new Triple[princCompArray.length];
                for (int j = 0; j < princCompArray.length; j ++)
                {
                    Triple pCForAtom = princCompArray[j];
                    Triple weightedPCForAtom = new Triple(
                        pCForAtom.getX() * weight,
                        pCForAtom.getY() * weight,
                        pCForAtom.getZ() * weight);
                    weightedPrincCompArray[j] = weightedPCForAtom;
                }
                
                weightedPrincCompArrays.add(weightedPrincCompArray);
            }
            
            // Average the first [given #] weighted principal component vectors
            // (i.e. just add their x's together, y's together, z's together)
            int numTriples = (weightedPrincCompArrays.get(0).length); // # atoms (rows/3)
            int numPCs = pcs.length;                                  // # PCs   (columns)
            avgPrincComps = new ArrayList<Triple>();
            for (int i = 0; i < numTriples; i ++)
            {
                double x = 0, y = 0, z = 0;
                for (int j = 0; j < numPCs; j ++)
                {
                    Triple[] weightedPrincCompArray = weightedPrincCompArrays.get(j);
                    Triple   wPCForAtom             = weightedPrincCompArray[i];
                    x += wPCForAtom.getX();
                    y += wPCForAtom.getY();
                    z += wPCForAtom.getZ();
                }
                Triple avgTriple = new Triple(x, y, z);
                avgPrincComps.add(avgTriple); //.set(i (*not* j), avgTriple);
            }
            if (verbose) System.out.println("avgPrincComps: "+avgPrincComps);
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't read input file: '"+s+"'!");
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
        
        if (avgPrincComps != null) // iff provided S matrix and pcs=#-# range
        {
            if (verbose) System.out.println("avgPrincComps != null...");
            Triple[] princCompArray = new Triple[avgPrincComps.size()];
            for (int i = 0; i < avgPrincComps.size(); i ++)
                princCompArray[i] = avgPrincComps.get(i); 
            if (group == null)
                out.println("@group {wt'd avg pcs"+pcs[0]+"-"+pcs[pcs.length-1]+" scale"+scale+"} dominant animate");
            else
                out.println("@group {"+group+"} dominant animate");
            
            // Calculate arrows
            ArrayList<Triple> arrows = new ArrayList<Triple>();
            for (int i = 0; i < origCoords.size(); i ++)
            {
                Triple origCoord = origCoords.get(i);
                Triple princComp = princCompArray[i];
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
            out.println("@vectorlist {pc arrows} master= {all} master= "
                +"{pcs"+pcs[0]+"-"+pcs[pcs.length-1]+"} color= "+color);
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
            out.println("@balllist {pc arrowheads} master= {all} master= "
                +"{pcs"+pcs[0]+"-"+pcs[pcs.length-1]+"} color= "+color+" radius= 0.07");
            for (int i = 0; i < origCoords.size(); i ++)
            {
                Triple arrow = arrows.get(i);
                out.println("{"+atomNames.get(i)+"} "+
                    arrow.getX()+" "+arrow.getY()+" "+arrow.getZ());
            }
        }
        else // not avg'ing vectors for pcs=#-#
        {
            if (verbose) System.out.println("avgPrincComps == null...");
            for (int p = 0; p < pcs.length; p ++)
            {
                int pc = pcs[p];
                Triple[] princCompArray = princCompArrays.get(p);
                
                if (group == null)
                    out.println("@group {pc"+pc+" scale"+scale+"} dominant animate");
                else
                    out.println("@group {"+group+"} dominant animate");
                
                // Calculate arrows
                ArrayList<Triple> arrows = new ArrayList<Triple>();
                for (int i = 0; i < origCoords.size(); i ++)
                {
                    Triple origCoord = origCoords.get(i);
                    Triple princComp = princCompArray[i];
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
                out.println("@vectorlist {pc arrows} master= {all} master= {pc"+pc+"} color= "+color);
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
                out.println("@balllist {pc arrowheads} master= {all} master= {pc"+pc+"} color= "+color+" radius= 0.07");
                for (int i = 0; i < origCoords.size(); i ++)
                {
                    Triple arrow = arrows.get(i);
                    out.println("{"+atomNames.get(i)+"} "+
                        arrow.getX()+" "+arrow.getY()+" "+arrow.getZ());
                }
            }
        }
    }
//}}}

}//class

