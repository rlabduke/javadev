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
* <p>Begun on Thu Oct 22 2009
* <p>Copyright (C) 2009 by Daniel Keedy. All rights reserved.
*/
public class RotamerCorrectness //extends ... implements ...
{
//{{{ Constants
    static final String NO_CONSENSUS_ROTNAME = "NO_CONSENSUS";
    DecimalFormat df = new DecimalFormat("0.000");
    public static final Object MODE_RESIDUE = "rotamer correctness per-residue";
    public static final Object MODE_SUMMARY = "rotamer correctness summary";
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose = false;
    
    Object mode = MODE_SUMMARY;
    
    String  targFilename;
    String  mdlsFilename; // could be file or directory
    String  homsFilename; // could be file or directory
    
    Model               targ;
    ArrayList<Residue>  targResList; // gets sorted so output will be in sequence order
    
    Rotalyze rotalyze;
    HashMap<Residue,String[]>  targsRotNames; // only used if multiple target MODELs found
    HashMap<Residue,String>    targRotNames;  // reflects consensus if multiple target MODELs found
    HashMap<Residue,Double>    targRotFracs;  // fraction of target rotnames that match consensus target rotname (if applicable)
    HashMap<Residue,Integer>   targRotCounts; // number of target rotnames (1 if single-MODEL target file; >1 otherwise)
    HashMap<Residue,String[]>  mdlsRotNames;
    HashMap<Residue,String>    cnsnsMdlRotNames; // consensus rotnames (modal rotname if popular enough)
    HashMap<Residue,Double>    modalMdlRotFracs; // fraction of model rotnames that match modal model rotname (which may or may not reach consensus)
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
            targ = targCoords.getFirstModel(); // other methods use these residues for indexing
            if(targCoords.getModels().size() == 1)
            {
                // Single target MODEL -- makes things simple
                targRotNames = rotalyze.getRotNames(targ);
            }
            else
            {
                // Multiple target MODELs -- store rotnames across target MODELs for each residue
                targsRotNames = new HashMap<Residue,String[]>();
                for(Iterator mItr = targCoords.getModels().iterator(); mItr.hasNext(); )
                {
                    Model trg = (Model) mItr.next();
                    HashMap<Residue,String> trgRotNames = rotalyze.getRotNames(trg);
                    storeTargetRotamers(trg, trgRotNames);
                }
            }
        }
        catch(IOException ex)
        { System.err.println("Error rotalyzing target: "+targFilename+"!"); }
    }
//}}}

//{{{ storeTargetRotamers
//##############################################################################
    /** Stores each target rotamer from one MODEL of a multi-MODEL target PDB. */
    public void storeTargetRotamers(Model trg, HashMap<Residue,String> trgRotNames)
    {
        // Align target MODEL onto target by sequence
        // We have to do this instead of iterating through the Residues of 'trg'
        // because Residues from different Models are treated as different by 
        // driftwood, even if they're from different instances/models of the same
        // protein (as in this case).
        Alignment align = Alignment.alignChains(
            SubImpose.getChains(trg), SubImpose.getChains(targ), 
            new Alignment.NeedlemanWunsch(), new SubImpose.SimpleResAligner());
        if(verbose)
        {
            System.err.println("Target MODEL <==> target residue alignments:");
            for(int i = 0; i < align.a.length; i++)
                System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            System.err.println();
        }
        for(Iterator rItr = trgRotNames.keySet().iterator(); rItr.hasNext(); )
        {
            // Target MODEL rotamer we wanna store
            Residue trgRes     = (Residue) rItr.next();
            String  trgRotName = trgRotNames.get(trgRes);
            
            // Find its corresponding target residue
            Residue targRes = null;
            for(int i = 0; i < align.a.length; i++)
            {
                Residue mRes = (Residue) align.a[i];
                Residue tRes = (Residue) align.b[i];
                if(mRes != null && mRes.getCNIT().equals(trgRes.getCNIT()))  targRes = tRes;
            }
            if(targRes == null) continue; // target MODEL has extra non-target tail or something
            
            // Add it to the arrays for this target residue
            String[] trgRotNamesOld = targsRotNames.get(targRes);
            String[] trgRotNamesNew;
            if(trgRotNamesOld != null)
            {
                trgRotNamesNew = new String[trgRotNamesOld.length+1];
                for(int i = 0; i < trgRotNamesOld.length; i++) trgRotNamesNew[i] = trgRotNamesOld[i];
            }
            else trgRotNamesNew = new String[1]; // "new" target residue
            trgRotNamesNew[trgRotNamesNew.length-1] = trgRotName; // add
            targsRotNames.put(targRes, trgRotNamesNew); // re-store
        }
    }
//}}}

//{{{ rotalyzeModels
//##############################################################################
    public void rotalyzeModels()
    {
        File mdlsFile = new File(mdlsFilename);
        mdlsRotNames = new HashMap<Residue,String[]>();
        if(mdlsFile.isDirectory())
        {
            // Directory of (potentially multi-MODEL) PDB files
            String[] mdlFilenames = mdlsFile.list();
            if(mdlFilenames == null)
            {
                System.err.println(mdlsFilename+" is an empty directory or does not exist!");
                System.exit(1);
            }
            for(String mdlFilename : mdlFilenames)
            {
                if(mdlFilename.indexOf(".pdb") == -1) continue; // only consider PDBs
                rotalyzeModel(mdlsFilename+"/"+mdlFilename);
            }
        }
        else
        {
            // Just one (potentially multi-MODEL) PDB file
            rotalyzeModel(mdlsFilename);
        }
    }
//}}}

//{{{ rotalyzeModel
//##############################################################################
    public void rotalyzeModel(String mdlFilename)
    {
        File mdlFile = new File(mdlFilename);
        if(mdlFile.isDirectory()) return; // skip nested directories
        System.err.println("Rotalyzing "+mdlFile);
        try
        {
            CoordinateFile mdlCoords = new PdbReader().read(mdlFile);
            for(Iterator mItr = mdlCoords.getModels().iterator(); mItr.hasNext(); )
            {
                Model mdl = (Model) mItr.next();
                HashMap<Residue,String> mdlRotNames = rotalyze.getRotNames(mdl);
                storeModelRotamers(mdl, mdlRotNames);
            }
        }
        catch(IOException ex)
        { System.err.println("Error rotalyzing model: "+mdlFilename+"!"); }
    }
//}}}

//{{{ storeModelRotamers
//##############################################################################
    /** Syncs each model rotamer to a target residue and stores. */
    public void storeModelRotamers(Model mdl, HashMap<Residue,String> mdlRotNames)
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
            Residue mdlRes    = (Residue) rItr.next();
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
            mdlsRotNames.put(targRes, mdlRotNamesNew); // re-store
        }
    }
//}}}

//{{{ rotalyzeHomologs
//##############################################################################
    public void rotalyzeHomologs()
    {
        System.err.println("\nVerification of rotamer differences from target"
            +" using homologs is not yet implemented!\n");
        showHelp(true);
        System.exit(1);
    }
//}}}

//{{{ defineTargResList
//##############################################################################
    public void defineTargResList()
    {
        targResList = new ArrayList<Residue>(); // positions at which to look for model consensus
        //                      multiple target MODELs vs. single target MODEL
        Map map = (targsRotNames != null? targsRotNames : targRotNames);
        for(Iterator rItr = map.keySet().iterator(); rItr.hasNext(); )
        {
            Residue targRes = (Residue) rItr.next();
            if(mdlsRotNames.get(targRes) != null)
                targResList.add(targRes); // eliminates other chains, unpredicted tails, etc.
        }
        Collections.sort(targResList);
    }
//}}}

//{{{ conductTargetConsensus
//##############################################################################
    /** Defines consensus target rotname for each residue.
    * Only used if input target PDB had multiple MODELs. */
    public void conductTargetConsensus()
    {
        targRotNames  = new HashMap<Residue,String>();
        targRotFracs  = new HashMap<Residue,Double>();
        targRotCounts = new HashMap<Residue,Integer>();
        for(Residue targRes : targResList)
        {
            String[] trgRotNames = targsRotNames.get(targRes);
            
            String modalRotName  = calcModalRotName(trgRotNames); // modal rotname: most common across target MODELs
            double modalRotFrac  = calcModalRotFrac(trgRotNames); // how common this modal rotname is
            int    trgRotCount   = trgRotNames.length;            // how many target MODELs contribute
            
            if(isConsensus(targRes.getName(), modalRotFrac))
                targRotNames.put(targRes, modalRotName);
            else
                targRotNames.put(targRes, this.NO_CONSENSUS_ROTNAME);
            targRotFracs.put(targRes, modalRotFrac); // regardless of whether we reached consensus or not
            targRotCounts.put(targRes, trgRotCount); // regardless of whether we reached consensus or not
        }
    }
//}}}

//{{{ conductModelConsensus
//##############################################################################
    public void conductModelConsensus()
    {
        cnsnsMdlRotNames = new HashMap<Residue,String>();
        modalMdlRotFracs = new HashMap<Residue,Double>();
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
            modalMdlRotFracs.put(targRes, modalRotFrac); // regardless of whether we reached consensus or not
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
            if(freq > maxFreq && !name.equals("OUTLIER"))
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

//{{{ residueOutput
//##############################################################################
    public void residueOutput()
    {
        System.out.println("targ:chain:resnum:inscode:restype:targ_rotname:num_targs:targs_frac_modal:"
            +"cnsns_mdl_rotname:num_mdls:mdls_frac_modal:mdl_cnsns_match:rotcor");
        // targ               target PDB ID
        // resnum             self-explanatory
        // chain              "
        // inscode            "
        // restype            "
        // targ_rotname       target rotname
        // num_targs          number of target MODELs that contribute to (potentially consensus) target rotname
        // targs_frac_modal   fraction of target MODELs that match modal target rotname (may or may not reach consensus)
        // cnsns_mdl_rotname  consensus model rotname
        // num_mdls           number of model rotnames that contribute to consensus model rotname
        // mdls_frac_modal    fraction of model rotnames that match modal model rotname (may or may not reach consensus)
        // mdl_cnsns_match    whether or not consensus model rotname matches target rotname (1 or 0)
        // frac_match         fraction of model rotnames that match target rotname
        for(Residue targRes : targResList)
        {
            String     targPdbId    =     targ.getState().getName().substring(0,4).toLowerCase();
            
            String     targChain    =     targRes.getChain().trim();
            int        targResNum   =     targRes.getSequenceInteger();
            String     targInsCode  =     targRes.getInsertionCode().trim();
            String     targResType  =     targRes.getName().trim();
            
            String     targRotName  =     targRotNames.get(targRes);
            double     targRotFrac  =     targRotFracs.get(targRes);
            int        targRotCount =     targRotCounts.get(targRes);
            String cnsnsMdlRotName  = cnsnsMdlRotNames.get(targRes);
            int    cnsnsMdlCount    =     mdlsRotNames.get(targRes).length;
            double modalMdlRotFrac  = modalMdlRotFracs.get(targRes);
            int    cnsnsMatch       =
                (cnsnsMdlRotName.equals(targRotName) && 
                !targRotName.equals("OUTLIER") && 
                !targRotName.equals(NO_CONSENSUS_ROTNAME) ? 1 : 0);
            double mdlsRotFrac      = calcModelsRotFrac(targRes);
            
            System.out.println(
                targPdbId+":"+targChain+":"+targResNum+":"+targInsCode+":"+targResType+":"+
                targRotName+":"+targRotCount+":"+df.format(targRotFrac)+":"+
                cnsnsMdlRotName+":"+cnsnsMdlCount+":"+df.format(modalMdlRotFrac)+":"+cnsnsMatch+":"+
                df.format(mdlsRotFrac));
        }
    }
//}}}

//{{{ summaryOutput
//##############################################################################
    public void summaryOutput()
    {
        System.out.println("targ:num_res:num_mdls:frac_cnsns_match:avg_rotcor");
        // targ              target PDB ID
        // num_res           target # of residues
        // num_targs         # of target MODELs (often just 1)
        // num_mdls          # of models
        // frac_cnsns_match  fraction of consensus model rotnames that match target rotname
        // avg_frac_match    fraction of model rotnames that match target rotname
        
        String targPdbId = targ.getState().getName().substring(0,4).toLowerCase();
        
        int trgCount = 0;
        int mdlCount = 0;
        double avgCnsnsMatch  = 0;
        double avgMdlsRotFrac = 0;
        for(Residue targRes : targResList)
        {
            if(targsRotNames != null) // multi-MODEL target
                trgCount += targsRotNames.get(targRes).length;
            else trgCount += 1;
            mdlCount += mdlsRotNames.get(targRes).length;
            String     targRotName =     targRotNames.get(targRes);
            String cnsnsMdlRotName = cnsnsMdlRotNames.get(targRes);
            int    cnsnsMatch  = 
                (cnsnsMdlRotName.equals(targRotName) && 
                !targRotName.equals("OUTLIER") && 
                !targRotName.equals(NO_CONSENSUS_ROTNAME) ? 1 : 0);
            double mdlsRotFrac = calcModelsRotFrac(targRes);
            avgMdlsRotFrac += 1.0*mdlsRotFrac;
            avgCnsnsMatch  += cnsnsMatch;
        }
        trgCount       /= targResList.size();
        mdlCount       /= targResList.size();
        avgCnsnsMatch  /= targResList.size();
        avgMdlsRotFrac /= targResList.size();
        
        // Note on the above: I'm calculating model count as an average number 
        // of models that went into consensus decisions across all residues. 
        // That's better than treating a single number of models that went into 
        // the consensus decision for a single, randomly selected residue because
        // it might be the case that some models didn't include that residue 
        // (e.g. at a tail or something).  (Though I reckon taking the mode 
        // instead of average would be even more robust...)
        // (Same applies to target count.)
        
        System.out.println(
            targPdbId+":"+targResList.size()+":"+trgCount+":"+
            mdlCount+":"+df.format(avgCnsnsMatch)+":"+df.format(avgMdlsRotFrac));
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if(targFilename == null || mdlsFilename == null)
        {
            showHelp(true);
            System.exit(1);
        }
        
        rotalyze = new Rotalyze();
        rotalyzeTarget();
        rotalyzeModels();
        if(homsFilename != null) rotalyzeHomologs();
        
        defineTargResList();
        if(targsRotNames != null) conductTargetConsensus();
        conductModelConsensus();
        
        if     (mode == MODE_RESIDUE) residueOutput();
        else if(mode == MODE_SUMMARY) summaryOutput();
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
        System.err.println("chiropraxis.rotarama.RotamerCorrectness version "+getVersion()+" build "+getBuild());
        System.err.println("Copyright (C) 2009 by Daniel Keedy. All rights reserved.");
    }

    // Get version number
    String getVersion()
    {
        InputStream is = getClass().getClassLoader().getResourceAsStream("chiropraxis/version.props");
        if(is == null)
            System.err.println("\n*** Unable to locate version number in 'version.props' ***\n");
        else
        {
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                line = reader.readLine();
                if(line != null && line.indexOf("version=") != -1)
                    return line.substring( line.indexOf("=")+1 );
            }
            catch(IOException ex) { ex.printStackTrace(); }
        }
        return "?.??";
    }

    // Get build number
    String getBuild()
    {
        InputStream is = getClass().getClassLoader().getResourceAsStream("chiropraxis/buildnum.props");
        if(is == null)
            System.err.println("\n*** Unable to locate build number in 'buildnum.props' ***\n");
        else
        {
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                line = reader.readLine();
                if(line != null && line.indexOf("buildnum=") != -1)
                    return line.substring( line.indexOf("=")+1 );
            }
            catch(IOException ex) { ex.printStackTrace(); }
        }
        return "yyyymmdd.????";
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
        else if(mdlsFilename == null) mdlsFilename = arg;
        else if(homsFilename == null) homsFilename = arg;
        else
        {
            showHelp(true);
            System.exit(1);
        }
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
        else if(flag.equals("-summ") || flag.equals("-summary"))
        {
            mode = MODE_SUMMARY;
        }
        else if(flag.equals("-res") || flag.equals("-residue"))
        {
            mode = MODE_RESIDUE;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class
