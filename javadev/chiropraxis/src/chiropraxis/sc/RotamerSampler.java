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
import driftwood.moldb2.*;
import driftwood.util.Strings;
//}}}
/**
* <code>RotamerSampler</code> accepts a list of chi angles and
* figures of merit, and translates them into PDB-format coordinates
* for one amino acid type.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan  6 15:33:28 EST 2004
*/
public class RotamerSampler //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String      aaType       = null;
    File        inFile       = null;
    Residue     res          = null;  // our "template" residue
    boolean     allAngles    = true;
    boolean     plotChis     = false;
    boolean     printData    = false; // chis; prob vals from inFile; atomic x,y,z (srcdata format)
    String      group        = null;
    String      color        = null;
    boolean     doChi234     = false;
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

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, NumberFormatException
    {
        // Check arguments
        if(aaType == null || inFile == null)
            throw new IllegalArgumentException("Not enough command line arguments");
        
        // Obtain template residue
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile   = pdbReader.read(this.getClass().getResourceAsStream("singleres.pdb"));
        Model           model       = coordFile.getFirstModel();
        ModelState      modelState  = model.getState();
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue)iter.next();
            if(aaType.equals(r.getName()))
                res = r;
        }
        if(res == null)
            throw new IllegalArgumentException("Couldn't find a residue called '"+aaType+"'");
        
        // Read data from list file
        LineNumberReader in = new LineNumberReader(new FileReader(inFile));
        ArrayList data = new ArrayList();
        String s;
        int nFields = -1;
        while((s = in.readLine()) != null)
        {
            if(s.startsWith("#")) continue;
            
            String[] parts = Strings.explode(s, ':');
            if(nFields < 0) nFields = parts.length;
            else if(nFields != parts.length) throw new IllegalArgumentException("Data fields are of different lengths");
            double[] vals = new double[nFields];
            for(int i = 0; i < nFields; i++)
                vals[i] = Double.parseDouble(parts[i]);
            data.add(vals);
        }
        in.close();

        // Determine figure of merit
        SidechainAngles2 angles = new SidechainAngles2();
        int nAngles = (allAngles ? angles.countAllAngles(res) : angles.countChiAngles(res));
        //System.err.println("nAngles = "+nAngles);
        double maxWeight = 0;
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            double[] vals = (double[])iter.next();
            maxWeight = Math.max(maxWeight, vals[nAngles]);
        }
        
        if (plotChis)
        {
            // Plot chis in kin format instead of creating rotamers PDB
            DecimalFormat df = new DecimalFormat("###.#");
            if (group == null) group = aaType+" samp chis";
            if (color == null) color = "blue";
            System.out.println("@group {"+group+"} dominant");
            System.out.println("@balllist {"+group+"} radius= 3 color= "+color);
            for(Iterator iter = data.iterator(); iter.hasNext(); )
            {
                // Point ID
                double[] vals = (double[])iter.next();
                System.out.print("{");
                for (int i = 0; i < vals.length-2; i ++) System.out.print(df.format(vals[i])+", ");
                System.out.print("} ");
                
                // Actual x,y,z coordinates
                int numCoords = (vals.length-2 >= 4 ? 3 : vals.length-2); // max = 3, min = 1
                for (int i = (doChi234 ? 1 : 0); i < (doChi234 ? numCoords+1 : numCoords); i ++) 
                    System.out.print(df.format(vals[i])+" ");
                System.out.println();
            }
        }
        else
        {
            // Create conformers
            PdbWriter pdbWriter = new PdbWriter(System.out);
            pdbWriter.setRenumberAtoms(true);
            
            int nDim = 0;
            if (printData) // header for opt'l top5200-angles srcdata-esque output mode
            {
                int nChis = ((double[])data.get(0)).length-2; // last 2 in .list file are pct and stat
                System.out.print("atom_name ");
                for (int i = 1; i <= nChis; i++)  System.out.print("chi"+i+" ");
                System.out.println("pct? pct? x y z");
            }
            
            int i = 1;
            for(Iterator iter = data.iterator(); iter.hasNext(); i++)
            {
                try
                {
                    double[] vals = (double[])iter.next();
                    Residue tempRes = new Residue(res, " ", "", Integer.toString(i), " ", res.getName());
                    ModelState tempState = tempRes.cloneStates(res, modelState, new ModelState(modelState));
                    if(allAngles)
                        tempState = angles.setAllAngles(tempRes, tempState, vals);
                    else
                        tempState = angles.setChiAngles(tempRes, tempState, vals);
                    for(Iterator ai = tempRes.getAtoms().iterator(); ai.hasNext(); )
                    {
                        Atom a = (Atom)ai.next();
                        // Makes all weights make best use of the 6.2 formatted field available to them
                        double occ = 999.0 * vals[nAngles]/maxWeight;
                        if(occ >= 1000.0) throw new Error("Logical error in occupancy weighting scheme");
                        tempState.get(a).setOccupancy(occ);
                    }
                    if (printData)
                    {
                        // Spit out chi dihedrals; probability measures from the input .list file (could be
                        // pct and stat); and x,y,z for each atom in sampled sidechain conformation
                        for(Iterator ai = tempRes.getAtoms().iterator(); ai.hasNext(); )
                        {
                            Atom a = (Atom)ai.next();
                            AtomState as = tempState.get(a);
                            String bbAtomNames = " N  , CA , C  , O  , H  , HA ";
                            if (bbAtomNames.indexOf(as.getName()) == -1)
                            {
                                System.out.print(tempRes.getName()+" conf"+i+" "+as.getName()+":");
                                for (int j = 0; j < vals.length; j++)  System.out.print(vals[j]+":");
                                System.out.println(as.getX()+":"+as.getY()+":"+as.getZ());
                            }
                        }
                    }
                    else
                    {
                        // "Normal" PDB output
                        pdbWriter.writeResidues(Collections.singletonList(tempRes), tempState);
                    }
                }
                catch(AtomException ex) { ex.printStackTrace(); }
            }
            System.out.flush();
            pdbWriter.close();
        }
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
        catch(Throwable ex)
        {
            ex.printStackTrace();
            System.err.println();
            System.err.println("*** Error: "+ex.getMessage());
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
        System.err.println("chiropraxis.sc.RotamerSampler");
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
        if(aaType == null) aaType = arg.toUpperCase();
        else if(inFile == null) inFile = new File(arg);
        else throw new IllegalArgumentException("Too many command line arguments");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-chionly"))
        {
            allAngles = false;
        }
        else if(flag.equals("-plotchis"))
        {
            plotChis  = true;
            printData = false;
        }
        else if(flag.equals("-data") || flag.equals("-printdata"))
        {
            plotChis  = false;
            printData = true;
        }
        else if(flag.equals("-group"))
        {
            group = param;
        }
        else if(flag.equals("-color"))
        {
            color = param;
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
//}}}
}//class

