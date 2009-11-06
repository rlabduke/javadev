// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import chiropraxis.mc.*;
//}}}
/**
* <code>RotamerCorrectness</code> compares rotamer names derived from 
* multidimensional distributions for multiple models versus a single target.
* It's basically a more aesthetically pleasing implementation of the 'corRot' 
* score from our assessment of template-based models in CASP8.
* 
* TO-DO:
*  - corroborate consensus model choice of non-target rotamer, 
*    IFF homolog is similar enough otherwise (>##%?) to target
*  - find consensus target rotamer at each position if target is NMR
*  - option to output just ^ ?
*
* <p>Begun on Thu Oct 22 2009
* <p>Copyright (C) 2009 by Daniel Keedy. All rights reserved.
*/
public class RotamerCorrectness //extends ... implements ...
{
//{{{ Constants
    static final String NO_CONSENSUS_ROTNAME = "NO_CONSENSUS";
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose = false;
    
    String                     targFilename;
    String                     mdlsDirname;
    String                     homsDirname;
    
    Model               targ;
    ArrayList<Residue>  targResList; // gets sorted so output will be in sequence order
    
    Rotalyze rotalyze;
    HashMap<Residue,String>    targRotNames;
    HashMap<Residue,String[]>  mdlsRotNames;
    HashMap<Residue,String>    cnsnsMdlRotNames; // consensus rotnames (modal rotname if popular enough)
    HashMap<Residue,Double>    cnsnsMdlRotFracs; // fraction of model rotnames that match consensus model rotname
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotamerCorrectness()
    {
        super();
    }
//}}}

//{{{ rotalyzeTarget
//##############################################################################
    public void rotalyzeTarget()
    {
        File targFile = new File(targFilename);
        System.err.println("Rotalyzing "+targFile);
        try
        {
            CoordinateFile targCoords = new PdbReader().read(targFile);
            targ = targCoords.getFirstModel();
            targRotNames = rotalyze.getRotNames(targ);
        }
        catch(IOException ex)
        { System.err.println("Error rotalyzing target: "+targFilename+"!"); }
    }
//}}}

//{{{ rotalyzeModels
//##############################################################################
    public void rotalyzeModels()
    {
        File mdlsDir = new File(mdlsDirname);
        String[] mdlFilenames = mdlsDir.list();
        if(mdlFilenames == null)
        {
            System.err.println(mdlsDirname+" either is not a directory or does not exist!");
            System.exit(1);
        }
        mdlsRotNames = new HashMap<Residue,String[]>();
        for(String mdlFilename : mdlFilenames)
        {
            if(mdlFilename.indexOf(".pdb") == -1) continue; // only consider PDBs
            rotalyzeModel(mdlFilename);
        }
    }
//}}}

//{{{ rotalyzeModel
//##############################################################################
    public void rotalyzeModel(String mdlFilename)
    {
        File mdlFile = new File(mdlsDirname+"/"+mdlFilename);
        if(mdlFile.isDirectory()) return; // skip nested directories
        System.err.println("Rotalyzing "+mdlFile);
        try
        {
            CoordinateFile mdlCoords = new PdbReader().read(mdlFile);
            for(Iterator mItr = mdlCoords.getModels().iterator(); mItr.hasNext(); )
            {
                Model mdl = (Model) mItr.next();
                HashMap<Residue,String> mdlRotNames = rotalyze.getRotNames(mdl);
                storeModelRotamers(mdl, targ, mdlRotNames);
            }
        }
        catch(IOException ex)
        { System.err.println("Error rotalyzing model: "+mdlFilename+"!"); }
    }
//}}}

//{{{ storeModelRotamers
//##############################################################################
    /** Syncs each model rotamer to a target residue and stores. */
    public void storeModelRotamers(Model mdl, Model targ, HashMap<Residue,String> mdlRotNames)
    {
        // Align model onto target by sequence
        Alignment align = Alignment.alignChains(
            SubImpose.getChains(mdl), SubImpose.getChains(targ), 
            new Alignment.NeedlemanWunsch(), new SubImpose.SimpleResAligner());
        if(verbose)
        {
            System.err.println("Model <==> target residue alignments:");
            for(int i = 0; i < align.a.length; i++)
                System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            System.err.println();
        }
        for(Iterator rItr = mdlRotNames.keySet().iterator(); rItr.hasNext(); )
        {
            // Model rotamer we wanna store
            Residue mdlRes = (Residue) rItr.next();
            String mdlRotName = mdlRotNames.get(mdlRes);
            
            // Find its corresponding target residue
            Residue targRes = null;
            for(int i = 0; i < align.a.length; i++)
            {
                Residue mRes = (Residue) align.a[i];
                Residue tRes = (Residue) align.b[i];
                if(mRes != null && mRes.getCNIT().equals(mdlRes.getCNIT()))  targRes = tRes;
            }
            if(targRes == null) continue; // model has extra non-target tail or something
            
            // Add it to the arrays for this target residue
            String[] mdlRotNamesOld = mdlsRotNames.get(targRes);
            String[] mdlRotNamesNew;
            if(mdlRotNamesOld != null)
            {
                mdlRotNamesNew = new String[mdlRotNamesOld.length+1];
                for(int i = 0; i < mdlRotNamesOld.length; i++) mdlRotNamesNew[i] = mdlRotNamesOld[i];
            }
            else mdlRotNamesNew = new String[1]; // "new" target residue
            mdlRotNamesNew[mdlRotNamesNew.length-1] = mdlRotName; // add
            
            // Re-store
            mdlsRotNames.put(targRes, mdlRotNamesNew);
        }
    }
//}}}

//{{{ conductConsensus
//##############################################################################
    public void conductConsensus()
    {
        targResList = new ArrayList<Residue>(); // positions at which to look for consensus
        for(Iterator rItr = targRotNames.keySet().iterator(); rItr.hasNext(); )
        {
            Residue targRes = (Residue) rItr.next();
            if(mdlsRotNames.get(targRes) != null)
                targResList.add(targRes); // eliminates other chains, unpredicted tails, etc.
        }
        Collections.sort(targResList);
        
        cnsnsMdlRotNames = new HashMap<Residue,String>();
        cnsnsMdlRotFracs = new HashMap<Residue,Double>();
        for(Residue targRes : targResList)
        {
            String    targRotName = targRotNames.get(targRes);
            String[]  mdlRotNames = mdlsRotNames.get(targRes);
            
            String modalRotName = calcModalRotName(mdlRotNames); // modal rotname: most common across models
            double modalRotFrac = calcModalRotFrac(mdlRotNames); // how common this modal rotname is
            
            if(isConsensus(targRes.getName(), modalRotFrac))
                cnsnsMdlRotNames.put(targRes, modalRotName);
            else
                cnsnsMdlRotNames.put(targRes, this.NO_CONSENSUS_ROTNAME);
            cnsnsMdlRotFracs.put(targRes, modalRotFrac); // regardless of whether we reached consensus or not
        }
    }
//}}}

//{{{ isConsensus
//##############################################################################
    public boolean isConsensus(String aaName, double frac)
    {
        final String aa1chi  = "CYS SER THR VAL";
        final String aa2chis = "ASN ASP HIS ILE LEU PHE TRP TYR";
        final String aa3chis = "GLN GLU MET";
        final String aa4chis = "ARG LYS";
        
        if(aa1chi.indexOf(aaName)  != -1 && frac >= 0.85) return true;
        if(aa2chis.indexOf(aaName) != -1 && frac >= 0.7 ) return true;
        if(aa3chis.indexOf(aaName) != -1 && frac >= 0.55) return true;
        if(aa4chis.indexOf(aaName) != -1 && frac >= 0.4 ) return true;
        return false;
    }
//}}}

//{{{ calcModalRotName
//##############################################################################
    public String calcModalRotName(String[] names)
    {
        HashMap<String,Integer> name_to_freq = new HashMap<String,Integer>();
        
        int tally = 0;
        for(String name : names)
        {
            if(name_to_freq.get(name) != null)
            {
                int freq = name_to_freq.get(name);
                name_to_freq.put(name, freq+1);
            }
            else
            {
                name_to_freq.put(name, 1);
            }
            tally++;
        }
        
        int    maxFreq = -1;
        String maxName = null;
        for(Iterator nItr = name_to_freq.keySet().iterator(); nItr.hasNext(); )
        {
            String name = (String) nItr.next();
            int freq = name_to_freq.get(name);
            if(freq > maxFreq)
            {
                maxFreq = freq;
                maxName = name;
            }
        }
        
        return maxName;
    }
//}}}

//{{{ calcModalRotFrac
//##############################################################################
    public double calcModalRotFrac(String[] names)
    {
        HashMap<String,Integer> name_to_freq = new HashMap<String,Integer>();
        
        int tally = 0;
        for(String name : names)
        {
            if(name_to_freq.get(name) != null)
            {
                int freq = name_to_freq.get(name);
                name_to_freq.put(name, freq+1);
            }
            else
            {
                name_to_freq.put(name, 1);
            }
            tally++;
        }
        
        int    maxFreq = -1;
        String maxName = null;
        for(Iterator nItr = name_to_freq.keySet().iterator(); nItr.hasNext(); )
        {
            String name = (String) nItr.next();
            int freq = name_to_freq.get(name);
            if(freq > maxFreq)
            {
                maxFreq = freq;
                maxName = name;
            }
        }
        
        return (1.0*maxFreq)/(1.0*tally);
    }
//}}}

//{{{ calcModelsRotFrac
//##############################################################################
    public double calcModelsRotFrac(Residue targRes)
    {
        String   targRotName = targRotNames.get(targRes);
        String[] mdlRotNames = mdlsRotNames.get(targRes);
        int matches = 0;
        int tally   = 0;
        for(String mdlRotName : mdlRotNames)
        {
            if(mdlRotName.equals(targRotName) && !targRotName.equals("OUTLIER")) matches++;
            tally++;
        }
        return (1.0*matches) / (1.0*tally);
    }
//}}}

//{{{ doOutput
//##############################################################################
    public void doOutput()
    {
        System.out.println("targ:res:targ_rotname:cnsns_mdl_rotname:num_mdls:frac_cnsns:cnsns_match?:frac_match");
        // 1. target residue
        // 2. target rotname
        // 3. consensus model rotname
        // 4. fraction of model rotnames that contribute to consensus model rotname
        // 5. whether or not consensus model rotname matches target rotname (1 or 0)
        // 6. fraction of model rotnames that match target rotname
        for(Residue targRes : targResList)
        {
            String     targPdbId   =     targ.getState().getName();
            
            String     targChain   =     targRes.getChain().trim();
            int        targResNum  =     targRes.getSequenceInteger();
            String     targInsCode =     targRes.getInsertionCode().trim();
            String     targResType =     targRes.getName().trim();
            
            String     targRotName =     targRotNames.get(targRes);
            String cnsnsMdlRotName = cnsnsMdlRotNames.get(targRes);
            int    cnsnsMdlCount   =     mdlsRotNames.get(targRes).length;
            double cnsnsMdlRotFrac = cnsnsMdlRotFracs.get(targRes);
            int    cnsnsMatch      = 
                (cnsnsMdlRotName.equals(targRotName) && !targRotName.equals("OUTLIER") ? 1 : 0);
            double mdlsRotFrac     = calcModelsRotFrac(targRes);
            
            DecimalFormat df = new DecimalFormat("0.00");
            System.out.println(
                targPdbId+":"+targChain+":"+targResNum+":"+targInsCode+":"+targResType+":"+
                targRotName+":"+
                cnsnsMdlRotName+":"+cnsnsMdlCount+":"+df.format(cnsnsMdlRotFrac)+":"+cnsnsMatch+":"+
                df.format(mdlsRotFrac));
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
        if(targFilename == null || mdlsDirname == null)
        {
            System.err.println("Must provide at least a target filename and a models dirname!");
            System.exit(1);
        }
        
        rotalyze = new Rotalyze();
        rotalyzeTarget();
        rotalyzeModels();
        // if(???) rotalyzeHomologs();
        
        conductConsensus();
        
        doOutput();
    }
    
    public static void main(String[] args)
    {
        RotamerCorrectness mainprog = new RotamerCorrectness();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(Exception ex)
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
            InputStream is = getClass().getResourceAsStream("RotamerCorrectness.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'RotamerCorrectness.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.rotarama.RotamerCorrectness");
        System.err.println("Copyright (C) 2009 by Daniel Keedy. All rights reserved.");
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
        if     (targFilename == null) targFilename = arg;
        else if(mdlsDirname  == null) mdlsDirname  = arg;
        else if(homsDirname  == null) homsDirname  = arg;
        else System.err.println("Too many filenames: "+arg+"!");
    }
    
    void interpretFlag(String flag, String param)
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
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class
