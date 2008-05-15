// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.Strings;
//}}}
/**
* <code>AvgStrucGenerator</code> generates an average PDB from a set of PDBs.
* This is done over a local structural region. 
* It takes as input:
* (1) a list containing central residue numbers
* (2) any number of PDB files in "1234CH.pdb" (where C is chain)
* (3) indices relative to the central residue via -range=#,# (opt'l)
* 
* Note that sequence alignment (i.e. correspondence) is handled internally, but 
* the input PDB files *must* already be aligned in 3-space for this to work!
*
* TODO: re-scale average stdevs/atom so max is 40 (to make more B-factor-ish)?
*
* <p>Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon May 13, 2008.
*/
public class AvgStrucGenerator //extends ... implements ...
{
//{{{ Constants
    String        bbAtoms   = " N  , CA , C  , O  ";//, CB , HA "; // Pro doesn't have ' H  '
    DecimalFormat df        = new DecimalFormat("###.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Output file containing res #'s, e.g. from Helix/SheetBuilder */
    File                   resnumFile   = null;
    
    /** Simple list of supplied PDB filenames, all of which should be aligned 
    * onto the same reference structure and each of which may potentially contain
    * multiple N-caps, beta aromatics, or whatever the motif of interest may be */
    ArrayList<String>      pdbFilenames = null;
    
    /** Links 4-character PDB ID codes to above PDB filenames */
    TreeMap<String,String> pdbidsToFilenames = null;
    
    /** Identity and coords for reference model, which will ultimately be modified
    * according to the coords in localCoords and output as the average structure */
    Model                  refModel     = null;
    ModelState             refState     = null;
    
    /** Ensemble of (xyz)n vectors representing coords of local model+states.
    * Ultimately used to calculate average coords */
    ArrayList<double[]>    localCoords  = null;
    
    /** Column in anglesFile containing res #. 0-indexed */
    int                    resnumIdx    = 2;
    
    /** Residue indices relative to the N-cap (helices) or the aromatic and its 
    * opposite residue (sheet) for inclusion in the output coordinates */
    int                    initIdx      = -2;
    int                    finalIdx     = 2;
    
    boolean                verbose      = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AvgStrucGenerator()
    {
        super();
        pdbFilenames = new ArrayList<String>();
        localCoords = new ArrayList<double[]>();
    }
//}}}

//{{{ populatePdbidMap
//##############################################################################
    /**
    * Links 4-character PDB ID's to actual filenames pointing to PDB files
    */
    public void populatePdbidMap()
    {
        pdbidsToFilenames = new TreeMap<String,String>();
        
        try
        {
            Scanner s = new Scanner(resnumFile);
            while (s.hasNextLine())
            {
                // "/localspace/neo500/Hoptimize/1a8dAH.pdb:helix from A 234 ASN to A 243 THR:Ncap A 234 ASN:117.362:119.372:94.484:96.115:112.275:108.998:112.512:-61.576:146.823:-89.646:165.213:-56.85:-30.271:2.148:1.737:5.318:3.803::i+3:GLU:2:3:"
                String line = s.nextLine();
                
                // Get PDB ID
                int dotPdbIdx = line.indexOf(".pdb");
                String pdbid = line.substring(dotPdbIdx-6, dotPdbIdx-2);
                
                // Find corresponding file and make the connection
                for (String pdbFilename : pdbFilenames)
                {
                    // "/localspace/neo500/Hoptimize/1a8dAH.pdb"
                    String[] levels = Strings.explode(pdbFilename, '/');
                    String filename = levels[levels.length-1];        // "1a8dAH.pdb"
                    String pdbidInFilename = filename.substring(0,4); // "1a8d"
                    
                    if (pdbid.equals(pdbidInFilename))
                        pdbidsToFilenames.put(pdbid,pdbFilename);
                }
            }
            
            if (verbose)
            {
                for (Iterator iter = pdbidsToFilenames.keySet().iterator(); iter.hasNext(); )
                {
                    String id = (String) iter.next();
                    String filename = (String) pdbidsToFilenames.get(id);
                    System.err.println(id+" --> "+filename);
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.out.flush();
            System.err.println("Trouble with a file in populatePdbidMap()");
        }
    }
//}}}

//{{{ getLocalCoords
//##############################################################################
    /**
    * Looks through lines of a file containing res #'s of interest and ends up
    * populating localCoords, an ArrayList of double[]'s. Each of those double[]'s
    * is an array of n(xyz) vectors representing multiple atoms and residues 
    * comprising one local structure
    */
    public void getLocalCoords()
    {
        try
        {
            Scanner s = new Scanner(resnumFile);
            while (s.hasNextLine())
            {
                try
                {
                    // "/localspace/neo500/Hoptimize/1a8dAH.pdb:helix from A 234 ASN to A 243 THR:Ncap A 234 ASN:117.362:119.372:94.484:96.115:112.275:108.998:112.512:-61.576:146.823:-89.646:165.213:-56.85:-30.271:2.148:1.737:5.318:3.803::i+3:GLU:2:3:"
                    String line = s.nextLine();
                    
                    // Get PDB ID and chain
                    int dotPdbIdx = line.indexOf(".pdb");
                    String pdbid = line.substring(dotPdbIdx-6, dotPdbIdx-2);
                    String chain = line.substring(dotPdbIdx-2, dotPdbIdx-1);
                    
                    // Get res #
                    // "Ncap A 234 ASN", "Ncap A   19A ASN", "Ncap A 1069 ASP", etc.
                    String[] tokens = Strings.explode(line, ':');
                    String token = tokens[resnumIdx];
                    String resnumString = token.substring(token.length()-8,token.length()-4).trim();
                    int resnum = Integer.MAX_VALUE;
                    try
                    { resnum = Integer.parseInt(resnumString); }
                    catch (NumberFormatException nfex)
                    { resnum = Integer.parseInt(resnumString.substring(0,resnumString.length()-1)); }
                    
                    String fn = pdbidsToFilenames.get(pdbid);
                    if (fn != null)
                    {
                        // PDB file was provided for this entry in the input list, so let's go for it
                        if (verbose)  System.err.println("Finding coords for "+resnum+" in "+pdbid);
                        
                        // Get existing coords of entire structure
                        PdbReader reader = new PdbReader();
                        File f = new File(fn);
                        CoordinateFile cf = reader.read(f);
                        Iterator models = cf.getModels().iterator();
                        Model m = (Model) models.next();
                        ModelState ms = m.getState();
                        
                        // Make model+coords of local region
                        Model      localModel = new Model("local model "+pdbid+" "+resnum);
                        ModelState localState = new ModelState();
                        for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
                        {
                            Residue res = (Residue) ri.next();
                            int currResnum = res.getSequenceInteger();
                            if (res != null && chain.equals(res.getChain()) && 
                                currResnum >= resnum+initIdx && currResnum <= resnum+finalIdx)
                            {
                                // Add this residue to the local model+state
                                if (verbose)  System.err.println("... found "+res);
                                for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
                                {
                                    Atom a = (Atom) ai.next();
                                    if (bbAtoms.indexOf(a.getName()) != -1)
                                    {
                                        if (!localModel.contains(res))
                                            localModel.add(res); // may remove res from m, but that should be OK
                                        AtomState as = ms.get(a);
                                        localState.add(as);
                                    }
                                }
                            }
                        }
                        
                        // Do something with new local state
                        if (refModel == null && refState == null)
                        {
                            // This is the new reference local state
                            refModel = localModel;
                            refState = localState;
                            if (verbose)  System.err.println("Setting "+localModel+" as reference");
                        }
                        else
                        {
                            // Add this local state's coords to the growing list of n(xyz) vectors
                            addToLocalCoords(localModel, localState);
                        }
                    }
                }
                catch (ResidueException re)
                {
                    System.err.println("Trouble with a residue in getLocalCoords()...");
                }
                catch (AtomException ae)
                {
                    System.err.println("Trouble with an atom in getLocalCoords()...");
                }
            }// done w/ this local motif
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("Trouble with a number in getLocalCoords()...");
        }
        catch (IOException ioe)
        {
            System.err.println("Trouble with I/O in getLocalCoords()...");
        }
    }
//}}}

//{{{ addToLocalCoords
//##############################################################################
    /**
    * For given local model+state, finds atoms corresponding to those in the 
    * previously determined reference local model+state, then adds the new atoms'
    * coordinates to a growing list of n(xyz) vectors. Hopefully, the xyz triplets
    * will be added  in the same atom-by-atom order each time this method is run
    * (i.e. for different local model+states vs. the same reference), which will 
    * be necessary for making an average local structure!
    */
    public void addToLocalCoords(Model localModel, ModelState localState)
    {
        try
        {
            // Align residues by sequence
            // For now we just take all residues as they appear in the file,
            // without regard to chain IDs, etc.
            Alignment align = Alignment.needlemanWunsch(refModel.getResidues().toArray(), localModel.getResidues().toArray(), new SimpleResAligner());
            if (verbose)
            {
                System.err.println("Residue alignments:");
                for (int i = 0; i < align.a.length; i++)
                    System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
                System.err.println();
            }
            
            // Add coords for local residues' atoms to growing ensemble of n(xyz) vectors
            ArrayList<Double> coords = new ArrayList<Double>();
            for(int i = 0, len = align.a.length; i < len; i++)
            {
                if (align.a[i] == null || align.b[i] == null) continue;
                Residue localRes = (Residue) align.b[i];
                
                // Make n(xyz) vector and add it
                for(Iterator ai = localRes.getAtoms().iterator(); ai.hasNext(); )
                {
                    Atom a = (Atom) ai.next();
                    if (bbAtoms.indexOf(a.getName()) != -1)
                    {
                        AtomState as = localState.get(a);
                        coords.add(as.getX());
                        coords.add(as.getY());
                        coords.add(as.getZ());
                    }
                }
            }
            double[] coordsArray = new double[coords.size()];
            for (int j = 0; j < coords.size(); j++)  coordsArray[j] = coords.get(j);
            localCoords.add(coordsArray);
        }
        catch (AtomException ae)
        {
            System.err.println("Trouble with an atom in addToLocalCoords()...");
        }
    }
//}}}

//{{{ printAvgStruc
//##############################################################################
    /**
    * The method name says it all!
    */
    public void printAvgStruc()
    {
        try
        {
            int numXYZs = 3 * (Strings.explode(bbAtoms,',')).length * (finalIdx-initIdx+1);
            
            //{{{ Some pre-output statistics
            
            
            
            // Average the coordinates
            double[] sums = new double[numXYZs];
            for (int i = 0; i < numXYZs; i++)   sums[i] = 0.0;
            int count = 0;
            for (double[] nXYZvector : localCoords)
            {
                // as String: "98.693 55.476 23.792 99.011 56.78 24.366 ..."
                // as String: "100.91 58.299 24.262 102.349 58.601 24.308..."
                if (nXYZvector.length == numXYZs)
                {
                    for (int i = 0; i < nXYZvector.length; i++)
                        sums[i] = sums[i] + nXYZvector[i];
                    count++;
                }
            }
            System.err.println("# contributors to average structure: "+count);
            double[] avgCoords = new double[numXYZs];
            for (int i = 0; i < numXYZs; i++)  avgCoords[i] = sums[i] / (1.0*count);
            if (verbose)
            {
                System.err.println("# entries per n(xyz) vector: "+avgCoords.length);
                System.err.print("avg coords: ");
                for (int i = 0; i < avgCoords.length; i++)
                {
                    if (i%3 == 0)  System.err.print(df.format(avgCoords[i])+"  ");
                    else           System.err.print(df.format(avgCoords[i])+", ");
                }
                System.err.println();
            }
            
            // Also get standard deviations of coordinates
            double[] stdevs = new double[numXYZs];
            for (int i = 0; i < numXYZs; i++)   stdevs[i] = 0.0;
            count = 0;
            for (double[] nXYZvector : localCoords)
            {
                // as String: "98.693 55.476 23.792 99.011 56.78 24.366 ..."
                // as String: "100.91 58.299 24.262 102.349 58.601 24.308..."
                if (nXYZvector.length == numXYZs)
                {
                    for (int i = 0; i < nXYZvector.length; i++)
                        stdevs[i] = stdevs[i] + Math.pow(nXYZvector[i]-avgCoords[i], 2);
                    count++;
                }
            }
            for (int i = 0; i < numXYZs; i++)  stdevs[i] = Math.sqrt(stdevs[i] / (1.0*count));
            
            
            
            //}}}
            
            // Make new average model+state (containing only certaina atom types)
            Model      avgModel = new Model("avg model");
            ModelState avgState = new ModelState();
            count = 0;
            for(Iterator ri = refModel.getResidues().iterator(); ri.hasNext(); )
            {
                Residue res = (Residue) ri.next();
                for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
                {
                    Atom a = (Atom) ai.next();
                    if (bbAtoms.indexOf(a.getName()) != -1)
                    {
                        AtomState as = refState.get(a);
                        double pseudoB = 0;
                        as.setX(avgCoords[count]);  pseudoB += stdevs[count];  count++;
                        as.setY(avgCoords[count]);  pseudoB += stdevs[count];  count++;
                        as.setZ(avgCoords[count]);  pseudoB += stdevs[count];  count++;
                        // average (of x,y,&z) stdev for this atom -- rough estimate of confidence in its position
                        pseudoB /= 3.0;
                        as.setTempFactor(pseudoB);
                        as.setOccupancy(1.0);
                        
                        if (!avgModel.contains(res))
                            avgModel.add(res); // may remove res from refModel, but that should be OK
                        avgState.add(as);
                    }
                }
            }
            
            // Print out new average model+state as a PDB
            if (verbose) System.err.println("Printing average structure...");
            PdbWriter pdbWriter = new PdbWriter(System.out);
            pdbWriter.writeResidues(avgModel.getResidues(), avgState);
            pdbWriter.close();
        }
        catch (ResidueException re)
        {
            System.err.println("Trouble with a residue in printAvgStruc()...");
        }
        catch (AtomException ae)
        {
            System.err.println("Trouble with an atom in printAvgStruc()...");
        }
    }
//}}}

//{{{ CLASS: SimpleResAligner
//##############################################################################
    static class SimpleResAligner implements Alignment.Scorer
    {
        // High is good, low is bad.
        public int score(Object a, Object b)
        {
            Residue r = (Residue) a;
            Residue s = (Residue) b;
            if(r == null || s == null)
                return -1;  // gap
            else if(r.getName().equals(s.getName()))
                return 2;   // match
            else
                return 0;   // mismatch
        }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if (resnumFile == null)
        {
            System.err.println("Need a file supplying res #'s!");
            System.exit(0);
        }
        populatePdbidMap();
        getLocalCoords();
        printAvgStruc();
    }

    public static void main(String[] args)
    {
        AvgStrucGenerator mainprog = new AvgStrucGenerator();
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
            InputStream is = getClass().getResourceAsStream("AvgStrucGenerator.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'AvgStrucGenerator.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.AvgStrucGenerator");
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
        if (resnumFile == null)        resnumFile = new File(arg);
        else                           pdbFilenames.add(arg);
        //else throw new IllegalArgumentException("Error handling file "+arg+"!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-range"))
        {
            Scanner s = new Scanner(param).useDelimiter(",");
            int idx1 = 999, idx2 = 999;
            while (s.hasNext())
            {
                String token = s.next();
                int tokenInt = Integer.parseInt(token);
                if (idx1 == 999)        idx1 = tokenInt;
                else if (idx2 == 999)   idx2 = tokenInt;
                else System.err.println("Wrong format: should be -range=#,#");
            }
            if (idx1 != 999 && idx2 != 999)
            {
                initIdx  = idx1;
                finalIdx = idx2;
            }
            else System.err.println("Wrong format: should be -range=#,#");
        }
        else if(flag.equals("-residx"))
        {
            try
            {
                resnumIdx = Integer.parseInt(param);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Couldn't parse "+param+" as an integer for resnumIdx");
            }
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

