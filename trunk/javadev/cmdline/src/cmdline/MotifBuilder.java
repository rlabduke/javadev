// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.util.Strings;
import driftwood.moldb2.*;
import driftwood.r3.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
//}}}
/**
* <code>MotifBuilder</code> constructs substructures/motifs
* based on residue ranges from Top5200 PDBs, which may be local or 
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
    
    /** Use special atom selection for printing <code>SubImpose</code> commands
    * so strand pair superposition goes smoothly. */
    String supBetaArom;
    
    /** Text file each line of which has format "pCNIT" followed optionally by 
    * as many additional "CNIT" residue specifiers as you want from that PDB */
    File descriptorsFile;
    
    /** Directory containing input PDBs in format "ppppFHC.pdb" */
    File inDir;
    
    /** Directory to which motif PDBs will be written */
    File outDir;
    
    /** How many additional residues we'll add in the N and C directions */
    int getN = 0, getC = 0;
    
    /** How many additional residues we'll use for superposition commands */
    int supN = 0, supC = 0;
    
    /** Simple options for which atom types to use for superposition commands */
    String supSel = "ca";
    
    /** Global variable used for superposing onto reference substructure */
    AtomState[] refAtoms;
    
    /** Global variable used for printing <code>SubImpose</code> commands */
    String refSel;
    File refFile;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MotifBuilder()
    {
        super();
    }
//}}}

//{{{ CLASS: Descriptor
//##############################################################################
    class Descriptor
    {
        String orig;
        String[] parts;
        int r = 0; // index of current residue in this descriptor (0 to n)
        
        public Descriptor(String s)
        {
            orig = s;
            parts = Strings.explode(s, ',');
            // 1a8o,A 195 ASN,A 203 LYS,A 222 LYS,A 234 GLY
            // 0    1         2         3         4
            // or
            // 1ab1,A,12,,ASN,A,58,,ASN
            // 0    1 2  34   5 6  78
        }
        
        //{{{ get_ function
        String getPdbid()
        { return parts[0].trim(); }
        String getChain()
        {
            if(parts[1].length() == 9)  return parts[r+1].substring(0,1).replace(" ","_");
            if(parts[1].length() == 10) return parts[r+1].substring(0,1).replace(" ","_");
            return parts[(4*r)+1].substring(0,1).replace(" ","_");
        }
        String getResnum()
        {
            if(parts[1].length() == 9)  return parts[r+1].substring(1,5).trim();
            if(parts[1].length() == 10) return parts[r+1].substring(2,6).trim();
            return parts[(4*r)+2].trim();
        }
        String getInscode()
        {
            if(parts[1].length() == 9)  return parts[r+1].substring(5,6).trim();
            if(parts[1].length() == 10) return parts[r+1].substring(6,7).trim();
            return parts[(4*r)+3].trim();
        }
        String getRestype()
        {
            if(parts[1].length() == 9)  return parts[r+1].substring(6,9).trim();
            if(parts[1].length() == 10) return parts[r+1].substring(7,10).trim();
            return parts[(4*r)+4].trim();
        }
        //}}}
        
        void nextRes()
        { if(r < parts.length-1) r++; }
        
        void resetRes()
        { r = 0; }
        
        int numRes()
        {
            if(parts[1].length() == 9)  return parts.length-1;
            if(parts[1].length() == 10) return parts.length-1;
            return (parts.length-1)/4;
        }
        
        public String toString()
        { return orig; }
    }
//}}}

//{{{ readDescriptors
//##############################################################################
    /** Makes a nice array of motif Descriptors. */
    public Descriptor[] readDescriptors()
    {
        // Get descriptors from file
        ArrayList<Descriptor> descriptors = new ArrayList<Descriptor>();
        BufferedReader br = null;
        try
        {
            FileInputStream is = new FileInputStream(descriptorsFile);
            br = new BufferedReader(new InputStreamReader(is));
            
            String s;
            boolean eof = false;
            
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
                            Descriptor d = new Descriptor(s);
                            descriptors.add(d);
                        }
                    }
                }
                catch(EOFException ex)
                { eof = true; }
                catch(IOException ex)
                { System.err.println("IO Error : "+ex.getMessage()); }
            }
        }
        catch(IOException ex)
        { System.err.println("IO Error : "+ex.getMessage()); }
        
        // Arrange into nice array
        int n = descriptors.size();
        Descriptor[] ret = new Descriptor[n];
        for(int i = 0; i < n; i++)
        {
            Descriptor descriptor = descriptors.get(i);
            ret[i] = descriptor;
        }
        return ret;
    }
//}}}

//{{{ buildMotif
//##############################################################################
    /**
    * Uses a list of descriptors to compile a "motif", writes each example to 
    * a separate PDB file, and prints instructions to superpose each example
    * onto the first example using <code>chiropraxis.mc.SubImpose</code>.
    */
    public void buildMotif()
    {
        if(inDir != null && inDir.isDirectory()) System.out.println("#!/bin/bash");
        else System.err.println("Must use local files to get out SubImpose commands!");
        
        Descriptor[] descriptors = readDescriptors();
        for(int i = 0; i < descriptors.length; i++)
        {
            Descriptor d = descriptors[i];
            String name = d.getPdbid()+d.getChain()+"_"+d.getResnum()
                +d.getInscode()+AminoAcid.translate(d.getRestype().toLowerCase())+".pdb";
            System.err.println("Building "+name);
            
            try
            {
                CoordinateFile substruc = buildSubstruc(d, name);
                if(substruc == null)
                {
                    System.err.println("Failed to build "+name);
                    continue;
                }
                
                File motFile = writeSubstrucPdb(substruc, name);
                
                if(inDir != null && inDir.isDirectory()) printSupCommand(d, motFile);
                else System.err.println("Must use local files to get SubImpose commands!");
                
                substruc = null; // necessary to prevent memory leak!
            }
            catch(NumberFormatException ex)
            { System.err.println("Bad descriptor format!"); }
            catch(MalformedURLException ex)
            { System.err.println("Bad URL"); }
            catch(IOException ex)
            { System.err.println("Couldn't find "+name+" .. is it in the Top5200?"); }
            catch(AtomException ex)
            { System.err.println("Missing atoms - can't superpose "+name+" onto ref!"); }
            catch(NullPointerException ex)
            { System.err.println("Missing residues (?) - can't superpose "+name+" onto ref!"); }
        }
    }
//}}}

//{{{ buildSubstruc
//##############################################################################
    /** 
    * Uses a single descriptor to build a stripped-down coordinate file
    * from a full PDB that's either local or obtained via the network..
    */
    public CoordinateFile buildSubstruc(Descriptor d, String name) throws IOException, MalformedURLException, NumberFormatException, AtomException, NullPointerException
    {
        // Make sure all residue ranges are from same chain (current limitation)
        d.resetRes();
        String pdbid = d.getPdbid();
        String chain = "";
        for(int i = 0; i < d.numRes(); i++)
        {
            String chainNew = d.getChain();
            if(chain.equals("")) chain = chainNew;
            else if(!chain.equals(chainNew))
            {
                System.err.println("All residues in "+d+" must be from same chain!");
                return null;
            }
            d.nextRes();
        }
        
        // Get & read local or remote file
        CoordinateFile substruc = null;
        if(inDir != null && inDir.isDirectory()) substruc = readLocalPdb(d);
        else                                     substruc = readRemotePdb(d);
        if(substruc == null)
        {
            System.err.println("Can't find substructure: "+name);
            return null;
        }
        
        // Check if read failed b/c file too big, or just partial model(s)
        if(substruc.getModels().size() < 1) return null;
        Model model = substruc.getFirstModel();
        
        // Find residues we wanna keep
        ArrayList<Residue> resToKeep  = new ArrayList<Residue>();
        d.resetRes();
        for(int i = 0; i < d.numRes(); i++)
        {
            // One residue we want from this PDB
            int    resnum  = Integer.parseInt(d.getResnum());
            String inscode = d.getInscode();
            String restype = d.getRestype();
            
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
            
            d.nextRes();
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
        
        /*
        // Superpose
        superposeOntoRef(substruc, model, descriptor);
        */
        
        return substruc;
    }
//}}}

//{{{ readLocalPdb
//##############################################################################
    /** 
    * Reads a full PDB obtained as a local file.
    */
    public CoordinateFile readLocalPdb(Descriptor d) throws IOException
    {
        d.resetRes();
        String pdbid = d.getPdbid();
        String chain = d.getChain();
        String desiredName = pdbid+"FH"+chain+".pdb";
        File inPdb = null;
        final File[] children = inDir.listFiles();
        for(File child : children)
        {
            if(child.getName().equals(desiredName))
            {
                inPdb = child;
                //motFile = inPdb;
                //if(refFile == null) refFile = motFile;
                break;
            }
        }
        if(inPdb == null) return null;
        InputStream is = new FileInputStream(inPdb);
        PdbReader reader = new PdbReader();
        CoordinateFile substruc = reader.read(is);
        is.close();
        return substruc;
    }
//}}}

//{{{ readRemotePdb
//##############################################################################
    /** 
    * Reads a full PDB obtained via the network.
    */
    public CoordinateFile readRemotePdb(Descriptor d) throws IOException, MalformedURLException
    {
        d.resetRes();
        String pdbid = d.getPdbid();
        String chain = d.getChain();
        URL url = new URL("http://arachne.biochem.duke.edu/~keedy/Top5200/"+pdbid+"FH"+chain+".pdb");
        InputStream is = url.openStream();
        PdbReader reader = new PdbReader();
        CoordinateFile substruc = reader.read(is);
        is.close();
        is = null; // to save memory?
        return substruc;
    }
//}}}

//{{{ findResidue
//##############################################################################
    /** Finds the residue in the given model matching the given CNIT */
    public Residue findResidue(Model model, String chain, int resnum, String inscode, String restype)
    {
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
        for(int i = 0; i < getN; i++)
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
        for(int i = 0; i < getC; i++)
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

//{{{ [superposeOntoRef]
//##############################################################################
//    public void superposeOntoRef(CoordinateFile substruc, Model model, String[] descriptor) throws AtomException, NullPointerException
//    {
//        // Get required atoms, possibly from multiple residue ranges
//        ArrayList<AtomState> atti = new ArrayList<AtomState>();
//        for(int i = 1; i < descriptor.length; i++)
//        {
//            // Find residue
//            String[] cnit = getCNIT(descriptor[i]);
//            Residue res = null;
//            for(Iterator rItr = model.getResidues().iterator(); rItr.hasNext(); )
//            {
//                Residue curr = (Residue) rItr.next();
//                if(cnit[0].equals(curr.getChain())
//                && cnit[1].equals(curr.getSequenceNumber().trim())
//                && cnit[3].equals(curr.getName()))  res = curr;
//            }
//            
//            // Add atoms N -> C
//            Residue curr = res;
//            for(int j = 0; j < getN; j++) curr = curr.getPrev(model);
//            int numRes = getN + 1 + getC;
//            for(int j = 0; j < numRes; j++)
//            {
//                atti.add(model.getState().get(curr.getAtom(" CA ")));
//                if(j < numRes-1) curr = curr.getNext(model); // work C-ward
//            }
//        }
//        
//        // Convert to array
//        AtomState[] atoms = new AtomState[atti.size()];
//        for(int i = 0; i < atti.size(); i++) atoms[i] = atti.get(i);
//        if(refAtoms == null) refAtoms = atoms;
//        
//        // Set up superposition
//        Transform R = new Transform(); // identity, defaults to no superposition
//        SuperPoser superpos = new SuperPoser(atoms, refAtoms);
//        R = superpos.superpos();
//        System.err.println("rmsd="+df.format(superpos.calcRMSD(R))+" (n="+atoms.length+")");
//        
//        // Do superposition
//        for(Iterator iter = Model.extractOrderedStatesByName(model).iterator(); iter.hasNext(); )
//        {
//            AtomState as = (AtomState) iter.next();
//            System.err.println("xforming "+as);
//            R.transform(as);
//            System.err.println("new!!!!! "+as);
//        }
//        
//        // Save memory
//        atti = null; atoms = null; R = null; superpos = null;
//    }
//}}}

//{{{ writeSubstrucPdb
//##############################################################################
    /** 
    * Writes one substructure PDB to a new file in the specified directory.
    * @return the output substructure PDB <code>File</code>
    */
    public File writeSubstrucPdb(CoordinateFile substruc, String name)
    {
        if(!outDir.exists())
        {
            boolean y = outDir.mkdir();
            System.err.println("Successfully made output directory '"+outDir+"'? .. "+(y?"YES":"NO"));
        }
        
        String filename = outDir+"/"+name;
        try
        {
            File file = new File(filename);
            if(refFile == null) refFile = file;
            PdbWriter writer = new PdbWriter(file);
            writer.writeCoordinateFile(substruc);
            writer.close();
            return file;
        }
        catch(IOException ex)
        { System.err.println("*** Error writing to "+filename+"!"); }
        
        return null;
    }
//}}}

//{{{ printSupCommand
//##############################################################################
    /** 
    * Prints instructions to superpose given substructure onto reference 
    * substructure using <code>chiropraxis.mc.SubImpose</code>.
    */
    public void printSupCommand(Descriptor d, File motFile) throws NumberFormatException
    {
        if((supN != 0 && supN > getN) || (supC != 0 && supC > getC))
            throw new IllegalArgumentException(
                "Can't superpose on fewer atoms than you're extracting!");
        
        String motSel = "";
        if(supBetaArom != null) // special case
        {
            if(d.numRes() < 3) return;
            d.resetRes(); int aroNum = Integer.parseInt(d.getResnum());
            d.nextRes();  int oppNum = Integer.parseInt(d.getResnum());
            if(supBetaArom.equals("heavy")) // N/CA/C/O, arom +/- 2, opp +/- 0
            {
                motSel += "(((atom_N__)or(atom_CA_)or(atom_C__)or(atom_O__))&";
                motSel += "("+(aroNum-2)+"-"+(aroNum+2)+"))or";
                motSel += "(((atom_N__)or(atom_CA_)or(atom_C__)or(atom_O__))&";
                motSel += "("+(oppNum-0)+"-"+(oppNum+0)+"))";
            }
            else if(supBetaArom.equals("ca")) // CAs, arom +/- 2, opp +/- 0
            {
                motSel += "(atom_CA_&";
                motSel += "("+(aroNum-2)+"-"+(aroNum+2)+"))or";
                motSel += "(atom_CA_&";
                motSel += "("+(oppNum-0)+"-"+(oppNum+0)+"))";
            }
            else throw new IllegalArgumentException("Invalid -betaarom=heavy/ca argument!");
        }
        else
        {
            // Desired atom type
            if(supSel.equals("ncaco")) motSel = "((atom_N__)or(atom_CA_)or(atom_C__)or(atom_O__))&(";
            else if(supSel.equals("cao")) motSel = "((atom_CA_)or(atom_O__))&(";
            else /*if(supSel.equals("ca"))*/ motSel = "atom_CA_&(";
            
            // Desired residue #s
            d.resetRes();
            for(int i = 0; i < d.numRes(); i++)
            {
                int resnum = Integer.parseInt(d.getResnum());
                if(supN != 0 || supC != 0) // sup on specific # residues in each direction
                    motSel += "("+(resnum-supN)+"-"+(resnum+supC)+")";
                else                       // sup on all residues we're extracting
                    motSel += "("+(resnum-getN)+"-"+(resnum+getC)+")";
                motSel += (i == d.numRes()-1 ? ")" : "or");
                d.nextRes();
            }
        }
        
        if(refSel == null)
        {
            // Set ref so all subsequent substrucs will get matched with it
            refSel = motSel;
        }
        
        System.out.println("echo 'SubImposing "+motFile.getName()+"'");
        System.out.println("subimpose -rmsdmax=1"+
            " -super1='"+motSel+"' "+motFile.getAbsolutePath()+
            " -super2='"+refSel+"' "+refFile.getAbsolutePath()+
            " -pdb="+motFile.getParent()+"-sup/"+motFile.getName());
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if(descriptorsFile == null) throw new IllegalArgumentException(
            "Must provide descriptors file!");
        if(outDir == null) throw new IllegalArgumentException(
            "Must provide output directory!");
        
        if(inDir == null) System.err.println(
            "No input directory provided -- will try to remotely access Top5200!");
        else System.err.println("Using "+inDir+" as source of PDB files");
        
        buildMotif();
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
                System.err.println("\n*** Usage: java MotifBuilder in.csv outdir (indir) ***\n");
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
        else if(inDir  == null)     inDir           = new File(arg);
        else System.err.println("Can't use additional argument: "+arg);
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
            if(flag.equals("-verbose") || flag.equals("-v"))
            {
                verbose = true;
            }
            else if(flag.equals("-n") || flag.equals("-getn"))
            {
                try { getN = Integer.parseInt(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format -(get)n="+param+" as integer!"); }
            }
            else if(flag.equals("-c") || flag.equals("-getc"))
            {
                try { getC = Integer.parseInt(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format -(get)c="+param+" as integer!"); }
            }
            else if(flag.equals("-supn"))
            {
                try { supN = Integer.parseInt(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format -supn="+param+" as integer!"); }
            }
            else if(flag.equals("-supc"))
            {
                try { supC = Integer.parseInt(param); }
                catch(NumberFormatException ex)
                { System.err.println("Can't format -supc="+param+" as integer!"); }
            }
            else if(flag.equals("-sup"))
            {
                if(param.equals("ca") || param.equals("cao") || param.equals("ncaco"))
                    supSel = param;
            }
            else if(flag.equals("-r") || flag.equals("-radius"))
            {
                System.err.println("-r(adius) not yet implemented!");
                System.exit(1);
            }
            else if(flag.equals("-betaarom") || flag.equals("-ba"))
            {
                supBetaArom = param;
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

