// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.util.Strings;
import driftwood.moldb2.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
//}}}
/**
* <code>MotifBuilder</code> constructs substructures/motifs
* based on residue ranges from Top5200 PDBs, which are 
* sitting somewhere "in the cloud" (i.e. on a computer in the RLab).
*
* The idea is to extract substructures with certain characteristics 
* (specific motifs, ends of helices, beginnings of sheets, ...) 
* from our big database into a usable form, namely PDB coordinates.
*
* The resulting array of CoordinateFiles can be output as PDBs, or could 
* feed into any number of things, like KiNG's EnsembleMotionTool for PCA.
*
* [The user can optionally supply a SQL select file with the expected SELECT 
* signature to generate the list of residues of interest.] - NEED TO IMPLEMENT!
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Sun May 31 2009
*/
public class MotifBuilder //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("#.##");
//}}}

//{{{ Variable definitions
//##############################################################################
    
    boolean verbose = false;
    
    /** Text file each line of which has format "PCNIT" followed optionally by 
    * as many additional "CNIT" residue specifiers as you want from that PDB */
    File descriptorsFile;
    
    /** Directory to which motif PDBs will be written */
    File outDir;
    
    /** How many additional residues we'll add in the N direction */
    int nWard = 0;
    /** How many additional residues we'll add in the C direction */
    int cWard = 0;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MotifBuilder()
    {
        super();
    }
//}}}

//{{{ setDescriptorsFile
//##############################################################################
    /** 
    * Call this to provide a line-by-line file of PCNITNIT substructure descriptors.
    */
    public void setDescriptors(File f)
    {
        descriptorsFile = f;
    }
//}}}

//{{{ readDescriptors
//##############################################################################
    /** 
    * Makes a nice 2D array of motif descriptors.  
    * Each array entry is 8 Strings: PCNITNIT.
    */
    public String[][] readDescriptors()
    {
        // Get descriptors from file
        ArrayList<String[]> descriptorsList = new ArrayList<String[]>();
        BufferedReader br = null;
        try
        {
            FileInputStream is = new FileInputStream(descriptorsFile);
            br = new BufferedReader(new InputStreamReader(is));
            
            String s;
            boolean eof = false;
            s = br.readLine(); // skip header
            
            while(!eof)
            {
                try
                {
                    s = br.readLine();
                    if(s == null)
                    {
                        eof = true;
                        br.close();
                    }
                    else
                    {
                        if(!s.startsWith("#"))
                        {
                            String[] descriptor = Strings.explode(s, ',');
                            if(verbose)
                            {
                                System.err.print("Parsing: ");
                                for(int i = 0; i < descriptor.length-1; i++) System.err.print(descriptor[i]+",");
                                System.err.println(descriptor[descriptor.length-1]);
                            }
                            descriptorsList.add(descriptor);
                        }
                    }
                }
                catch(EOFException ex)
                {
                    eof = true;
                }
                catch(IOException ex)
                {
                    System.err.println("IO Error : "+ex.getMessage());
                }
            }
        }
        catch(IOException ex)
        {
            System.err.println("IO Error : "+ex.getMessage());
        }
        
        // Arrange into nice array
        int n = descriptorsList.size();
        String[][] descriptors = new String[n][];
        for(int i = 0; i < n; i++)
        {
            String[] descriptor = descriptorsList.get(i);
            descriptors[i] = descriptor;
        }
        
        return descriptors;
    }
//}}}

//{{{ buildMotif
//##############################################################################
    /**  Uses a list of descriptors to compile a "motif" coordinate file */
    public CoordinateFile[] buildMotif()
    {
        String[][] descriptors = readDescriptors();
        // 1a8d,A,234,,ASN
        // 1a8o,A,195,,ASN,A,203,,LYS,A,222,,LYS,A,234,,GLY
        
        ArrayList<CoordinateFile> motifAL = new ArrayList<CoordinateFile>();
        
        for(int i = 0; i < descriptors.length; i++)
        {
            String[] descriptor = descriptors[i];
            if(descriptor.length % 4 != 1) throw new IllegalArgumentException(
                "Residue descriptors must be of form PCNIT(CNITCNIT...)!");
            try
            {
                CoordinateFile substruc = readRemotePdb(descriptor);
                motifAL.add(substruc);
                if(verbose) System.err.println("Added "+substruc+" to "+motifAL);
            }
            catch(NumberFormatException ex)
            { System.err.println("Error formating "+descriptor[2]+" or "+descriptor[5]); }
            catch(MalformedURLException ex)
            { System.err.println("Bad URL"); }
            catch(IOException ex)
            { System.err.println("Couldn't find "+ex.getMessage()+" .. is it in the Top5200?"); }
        }
        
        CoordinateFile[] motif = new CoordinateFile[motifAL.size()];
        for(int i = 0; i < motifAL.size(); i++)
            motif[i] = motifAL.get(i);
        return motif;
    }
//}}}

//{{{ readRemotePdb
//##############################################################################
    /** 
    * Uses a single descriptor to build a stripped-down coordinate file 
    * from a full PDB obtained via the network.
    */
    public CoordinateFile readRemotePdb(String[] descriptor) throws NumberFormatException, MalformedURLException, IOException
    {
        String pdbid = descriptor[0].trim();
        String chain = descriptor[1].trim();
        
        // Get & read file from network
        URL url = new URL("http://arachne.biochem.duke.edu/~keedy/Top5200/"+pdbid+"FH"+chain+".pdb");
        InputStream is = url.openStream();
        PdbReader reader = new PdbReader();
        CoordinateFile substruc = reader.read(is);
        is.close();
        is = null; // to save memory?
        
        // Remove all but model 1
        ArrayList<Model> notModel1 = new ArrayList<Model>();
        for(Iterator mItr = substruc.getModels().iterator(); mItr.hasNext(); )
        {
            Model m = (Model)mItr.next();
            if(m.getName() != "1")  notModel1.add(m);
        }
        for(Model model : notModel1)  substruc.remove(model);
        if(substruc.getModels().size() < 1) // file too big & read failed, or only partial
            return null;                    // model(s), or I removed too many models
        Model model = substruc.getFirstModel();
        
        // Find residues we wanna keep
        ArrayList<Residue> resToKeep  = new ArrayList<Residue>();
        for(int i = 1; i < descriptor.length; i += 4)
        {
            // One residue we want from this PDB
            //String chain   = descriptor[i].trim();
            int    resnum  = Integer.parseInt(descriptor[i+1].trim());
            String inscode = descriptor[i+2].trim();
            String restype = descriptor[i+3].trim();
            
            // Find it
            Residue res = findResidue(model, chain, resnum, inscode, restype);
            if(res == null)
            {
                System.err.println("Uh-oh: can't find "+chain+","+resnum+","+inscode+","+restype+"!");
                return null;
            }
            
            // Also find other nearby residues
            ArrayList<Residue> keep  = expandSelection(model, res);
            for(Residue r : keep)  resToKeep.add(r);
        }
        
        // Convert that to residues we want to *remove*
        ArrayList<Residue> resToRemove = new ArrayList<Residue>();
        for(Iterator rItr = model.getResidues().iterator(); rItr.hasNext(); )
        {
            Residue curr = (Residue) rItr.next();
            if(!resToKeep.contains(curr))  resToRemove.add(curr);
        }
        // Actually remove them
        for(Residue curr : resToRemove)
        {
            try
            { model.remove(curr); }
            catch(ResidueException ex)
            { System.err.println("Error removing "+curr+" from "+model); }
        }
        
        return substruc;
    }
//}}}

//{{{ findResidue
//##############################################################################
    /** Finds the residue in the given model matching the given CNIT */
    public Residue findResidue(Model model, String chain, int resnum, String inscode, String restype)
    {
        if(verbose) System.err.print("Looking for "+chain+","+resnum+","+inscode+","+restype);
        
        Residue res = null;
        for(Iterator rItr = model.getResidues().iterator(); rItr.hasNext(); )
        {
            Residue curr = (Residue) rItr.next();
            if(curr.getChain().equals(chain)
            && curr.getSequenceInteger() == resnum
            && curr.getInsertionCode().trim().equals(inscode)
            && curr.getName().equals(restype))
            {
                res = curr;
                break; // found it
            }
        }
        
        if(verbose)
        {
            if(res == null) System.err.println(" .. no dice!");
            else            System.err.println(" .. found it: "+res);
        }
        return res;
    }
//}}}

//{{{ expandSelection
//##############################################################################
    /** Collects list of residues within the desired range */
    public ArrayList<Residue> expandSelection(Model model, Residue res)
    {
        // Collect residues we want to *keep*
        ArrayList<Residue> resToKeep = new ArrayList<Residue>();
        resToKeep.add(res);
        
        // N-ward
        Residue curr = res;
        for(int i = 0; i < nWard; i++)
        {
            Residue next = (Residue) curr.getNext(model);
            if(next == null) break;
            else
            {
                resToKeep.add(next);
                curr = next;
            }
        }
        
        // C-ward
        curr = res;
        for(int i = 0; i < cWard; i++)
        {
            Residue prev = (Residue) curr.getPrev(model);
            if(prev == null) break;
            else
            {
                resToKeep.add(prev);
                curr = prev;
            }
        }
        
        return resToKeep;
    }
//}}}

//{{{ writeMotifPdbs
//##############################################################################
    /** 
    * Writes multiple single-MODEL motif PDBs to new files in specified directory.
    */
    public void writeMotifPdbs(CoordinateFile[] motif)
    {
        if(!outDir.exists())
        {
            boolean y = outDir.mkdir();
            System.err.println("Successfully made output directory '"+outDir+"'? .. "+(y?"yes":"NO!"));
        }
        
        int i = 1;
        for(CoordinateFile substruc : motif)
        {
            String filename = outDir+"/"+i+"_"+substruc.getIdCode()+".pdb"; // NEEDS RES RANGE!!
            try
            {
                PdbWriter writer = new PdbWriter( new File(filename) );
                writer.writeCoordinateFile(substruc);
                writer.close();
                i++;
            }
            catch(IOException ex)
            { System.err.println("*** Error writing to "+filename+"!"); }
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
        // *** COMMAND LINE MODE ***
        
        if(descriptorsFile == null) throw new IllegalArgumentException(
            "Must provide descriptors file!");
        if(outDir == null) throw new IllegalArgumentException(
            "Must provide output directory!");
        
        CoordinateFile[] motif = buildMotif();
        
        writeMotifPdbs(motif);
        
        // *** COMMAND LINE MODE ***
    }

    public static void main(String[] args)
    {
        MotifBuilder mainprog = new MotifBuilder();
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
            InputStream is = getClass().getResourceAsStream("MotifBuilder.help");
            if(is == null)
            {
                System.err.println("\n*** Usage: java MotifBuilder in.csv outdir ***\n");
            }
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.MotifBuilder");
        System.err.println("Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.");
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
        if(descriptorsFile == null) descriptorsFile = new File(arg);
        else if(outDir == null)     outDir          = new File(arg);
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
            else if(flag.equals("-n") || flag.equals("-nward"))
            {
                try
                { nWard = Integer.parseInt(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format -n(ward)="+param+" as integer!"); }
            }
            else if(flag.equals("-c") || flag.equals("-cward"))
            {
                try
                { cWard = Integer.parseInt(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format -c(ward)="+param+" as integer!"); }
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

