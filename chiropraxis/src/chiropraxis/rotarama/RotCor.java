// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.*;
import chiropraxis.mc.*;
//}}}
/**
* <code>RotCor</code> compares rotamer names for multiple models vs. a single 
* target.  It reports the <i>rotcor</i> score. 
* 
* <p>Begun on Fri Jan 15 2010
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
*/
public class RotCor //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("0.000");
    public static final String NO_CONSENSUS_ROTNAME = "NO_CONSENSUS";
    public static final String MODEL = "model-level output";
    public static final String RESIDUE = "residue-level output";
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean  verbose = false;
    
    Object   mode = MODEL;
    
    String   trgFilename; // PDB file
    String   mdlsDirname; // directory
    String   homsDirname; // directory
    
    Model    trg;
    String   trgName;
    int      trgCount;
    
    HashMap<Residue,String[]>  trgsRotNames; // only used if multiple target MODELs found
    HashMap<Residue,String>    trgRotNames;  // (reflects consensus if multiple target MODELs found)
    
    HashMap<Residue,String[]>  mdlsRotNames; // only used in RESIDUE mode
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotCor()
    {
        super();
    }
//}}}

//{{{ prepTarget
//##############################################################################
    public void prepTarget()
    {
        if(trgFilename.indexOf(".pdb") == -1)
        {
            System.err.println(trgFilename+" must be a .pdb file!");
            System.exit(1);
        }
        File trgFile = new File(trgFilename);
        Rotalyze rotalyze = new Rotalyze();
        
        String[] trgNamePieces = Strings.explode(trgFilename, '/');
        trgName = trgNamePieces[trgNamePieces.length-1];
        if(verbose) System.err.println("Rotalyzing "+trgName);
        trgName = trgName.substring(0, trgName.indexOf(".pdb"));
        
        try
        {
            CoordinateFile trgCoords = new PdbReader().read(trgFile);
            trg = trgCoords.getFirstModel(); // other methods use these residues for indexing
            trgCount = trgCoords.getModels().size();
            if(trgCount == 1)
            {
                // Single target MODEL -- makes things simple
                trgRotNames = rotalyze.getNames(trg);
                
                // But still check that we're not using just one MODEL 
                // of what should really be a multi-MODEL target
                for(Iterator hItr = trgCoords.getHeaders().iterator(); hItr.hasNext(); )
                {
                    String header = (String) hItr.next();
                    if(header.indexOf("REMARK") != -1 && header.indexOf(" - MODEL ") != -1)
                    {
                        // "REMARK   T0474 - MODEL 20"  (from CASP8)
                        System.err.println("Warning! Header suggests target may"
                            +" actually be multi-model: \""+header+"\"");
                    }
                }
            }
            else
            {
                // Multiple target MODELs -- store rotnames across target MODELs for each residue
                System.err.println("Found >1 PDB-style MODELs in target ("+trgFilename+
                    ") -- calculating consensus rotamer at each position");
                trgsRotNames = new HashMap<Residue,String[]>();
                for(Iterator mItr = trgCoords.getModels().iterator(); mItr.hasNext(); )
                {
                    Model currTrg = (Model) mItr.next();
                    trgRotNames = rotalyze.getNames(currTrg); // temporary storage
                    storeTarget(currTrg, trgRotNames);
                }
                conductTargetConsensus();
            }
        }
        catch(IOException ex)
        { System.err.println("Error rotalyzing target: "+trgFilename+"!"); }
    }
//}}}

//{{{ storeTarget
//##############################################################################
    /**
    * Stores each target rotamer from one MODEL of a multi-MODEL target PDB.
    */
    public void storeTarget(Model currTrg, HashMap<Residue,String> trgRotNames)
    {
        for(Iterator iter1 = trgRotNames.keySet().iterator(); iter1.hasNext(); )
        {
            // Rotamer we wanna store from current target MODEL
            Residue res = (Residue) iter1.next();
            String  rot = trgRotNames.get(res);
            
            // Find corresponding residue from target MODEL 1
            Residue trgRes = null;
            for(Iterator iter2 = trg.getResidues().iterator(); iter2.hasNext(); )
            {
                Residue tRes = (Residue) iter2.next();
                if(tRes != null && tRes.getCNIT().equals(res.getCNIT()))
                    trgRes = tRes;
            }
            if(trgRes == null) continue; // current target MODEL has extra tail or something
            
            // Add it to the arrays for this target residue
            String[] trgRotNamesOld = trgsRotNames.get(trgRes);
            String[] trgRotNamesNew;
            if(trgRotNamesOld != null)
            {
                trgRotNamesNew = new String[trgRotNamesOld.length+1];
                for(int i = 0; i < trgRotNamesOld.length; i++) trgRotNamesNew[i] = trgRotNamesOld[i];
            }
            else trgRotNamesNew = new String[1]; // "new" target residue
            trgRotNamesNew[trgRotNamesNew.length-1] = rot; // add
            trgsRotNames.put(trgRes, trgRotNamesNew); // re-store
        }
    }
//}}}

//{{{ conductTargetConsensus
//##############################################################################
    /**
    * Defines consensus target rotname for each residue.
    * Only used if input target PDB had multiple MODELs.
    */
    public void conductTargetConsensus()
    {
        trgRotNames = new HashMap<Residue,String>();
        for(Iterator rItr = trgsRotNames.keySet().iterator(); rItr.hasNext(); )
        {
            Residue trgRes = (Residue) rItr.next();
            String[] trgResRotNames = trgsRotNames.get(trgRes);
            
            String modalRotName  = calcModalRotName(trgResRotNames); // modal rotname: most common across target MODELs
            double modalRotFrac  = calcModalRotFrac(trgResRotNames); // how common this modal rotname is
            
            if(isConsensus(trgRes.getName(), modalRotFrac))
                trgRotNames.put(trgRes, modalRotName);
            else
                trgRotNames.put(trgRes, this.NO_CONSENSUS_ROTNAME);
        }
    }
//}}}

//{{{ isConsensus
//##############################################################################
    public boolean isConsensus(String aaName, double frac)
    {
        final String aa1chi  = "CYS SER THR VAL PRO";
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

//{{{ extractModel
//##############################################################################
    public Model extractModel(String filename) throws IOException
    {
        File file = new File(filename);
        if(file.isDirectory()) return null; // skip nested directories
        if(verbose) System.err.println("Rotalyzing "+file);
        CoordinateFile coords = new PdbReader().read(file);
        if(coords.getModels().size() == 0)
        {
            System.err.println("Found 0 models in "+filename+" ... skipping!");
            return null;
        }
        else
        {
            if(coords.getModels().size() > 1)
            {
                System.err.println("Found "+coords.getModels().size()+" models in "
                    +filename+" ... only considering first one");
            }
            Model mdl = coords.getFirstModel();
            return mdl;
        }
    }
//}}}

//{{{ alignModel
//##############################################################################
    /**
    * Uses sequence alignment to assign each model rotamer to a target residue.
    */
    public HashMap<Residue,String> alignModel(Model mdl, HashMap<Residue,String> mdl_to_mdl)
    {
        // Align model onto target by sequence
        Alignment align = Alignment.alignChains(
            SubImpose.getChains(mdl), SubImpose.getChains(trg), 
            new Alignment.NeedlemanWunsch(), new SubImpose.SimpleResAligner());
        //if(verbose)
        //{
        //    System.err.println("Model <==> Target:");
        //    for(int i = 0; i < align.a.length; i++)
        //        System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
        //    System.err.println();
        //}
        
        // Assign each model rotamer to a target residue
        HashMap<Residue,String> trg_to_mdl = new HashMap<Residue,String>();
        for(Iterator rItr = mdl_to_mdl.keySet().iterator(); rItr.hasNext(); )
        {
            // Model rotamer we wanna store
            Residue res    = (Residue) rItr.next();
            String mdlRotName = mdl_to_mdl.get(res);
                
            // Find its corresponding target residue
            Residue trgRes = null;
            for(int i = 0; i < align.a.length; i++)
            {
                Residue mRes = (Residue) align.a[i];
                Residue tRes = (Residue) align.b[i];
                if(mRes != null && mRes.getCNIT().equals(res.getCNIT()))  trgRes = tRes;
            }
            if(trgRes == null) continue; // model has extra non-target tail or something
            
            trg_to_mdl.put(trgRes, mdlRotName);
        }
        return trg_to_mdl;
    }
//}}}

//{{{ tallyRotamers
//##############################################################################
    public int[] tallyRotamers(HashMap<Residue,String> mdlRotNames)
    {
        int trgResTally = 0;
        int mdlResTally = 0;
        int matchTally = 0;
        
        ArrayList<Residue> trgResidues = new ArrayList<Residue>();
        for(Iterator rItr = trgRotNames.keySet().iterator(); rItr.hasNext(); )
        {
            Residue trgRes = (Residue) rItr.next();
            trgResidues.add(trgRes);
        }
        Collections.sort(trgResidues);
        for(int i = 0; i < trgResidues.size(); i++)
        {
            Residue trgRes = trgResidues.get(i);
            String trgRotName = trgRotNames.get(trgRes);
            String mdlRotName = mdlRotNames.get(trgRes);
            if(trgRotName != null && !trgRotName.equals("OUTLIER") && !trgRotName.equals(this.NO_CONSENSUS_ROTNAME))
            {
                trgResTally++;
                if(mdlRotName != null) mdlResTally++;
                boolean match = (mdlRotName != null && mdlRotName.equals(trgRotName));
                if(match) matchTally++;
                if(verbose) System.err.println(trgRes+": "
                    +(match?"":"(")+trgRotName+" "+(match?"=":"!=")+" "+mdlRotName+(match?"":")"));
            }
        }
        
        return new int[] {trgResTally, mdlResTally, matchTally};
    }
//}}}

//{{{ assessModels
//##############################################################################
    /**
    * Performs rotcor assessment for all models individually against the target.
    */
    public void assessModels()
    {
        File mdlsDir = new File(mdlsDirname);
        String[] listing = mdlsDir.list();
        if(listing == null)
        {
            System.err.println(mdlsDirname+" is an empty directory or does not exist!");
            System.exit(1);
        }
        ArrayList<String> mdlFilenames = new ArrayList<String>();
        for(int i = 0; i < listing.length; i++) mdlFilenames.add(listing[i]);
        Collections.sort(mdlFilenames);
        
        System.err.println("... "+mode);
        
        if(mode == MODEL)
        {
            System.out.println("Target:Model:TargetCount:TargetRotamers:ModelRotamers:Matches:RotCor");
            
            // Rotalyze models one at a time
            for(int i = 0; i < mdlFilenames.size(); i++)
            {
                String mdlFilename = mdlFilenames.get(i);
                if(mdlFilename.indexOf(".pdb") == -1) continue; // only consider PDBs
                assessModel(mdlFilename);
            }
        }
        else //if(mode == RESIDUE)
        {
            // Rotalyze all models in advance
            mdlsRotNames = new HashMap<Residue,String[]>();
            for(int i = 0; i < mdlFilenames.size(); i++)
            {
                String mdlFilename = mdlFilenames.get(i);
                if(mdlFilename.indexOf(".pdb") == -1) continue; // only consider PDBs
                rotalyzeModel(mdlFilename);
            }
            
            System.out.println("Target:Chain:ResNum:InsCode:ResType:TargetRotamer:TargetCount:ModelCount:FracMatch");
            
            ArrayList<Residue> trgResNames = new ArrayList<Residue>();
            for(Iterator rItr = trgRotNames.keySet().iterator(); rItr.hasNext(); )
            {
                trgResNames.add((Residue) rItr.next());
            }
            Collections.sort(trgResNames);
            for(int i = 0; i < trgResNames.size(); i++)
            {
                Residue trgRes = trgResNames.get(i);
                assessResidue(trgRes);
            }
        }
    }
//}}}

//{{{ assessModel
//##############################################################################
    /**
    * Performs rotcor assessment for one model against the target.
    */
    public void assessModel(String mdlFilename)
    {
        try
        {
            Model mdl = extractModel(mdlsDirname+"/"+mdlFilename);
            if(mdl == null) return;
            
            // Get rotamer names, then index by target residue
            Rotalyze rotalyze = new Rotalyze();
            HashMap<Residue,String> tmpRotNames = rotalyze.getNames(mdl);
            HashMap<Residue,String> mdlRotNames = alignModel(mdl, tmpRotNames);
            
            int[] tallies = tallyRotamers(mdlRotNames);
            double rotcor = (double) tallies[2] / (double) tallies[0];
            
            outputModel(mdlFilename, tallies, rotcor);
        }
        catch(IOException ex)
        { System.err.println("Error reading file: "+mdlFilename); }
    }
//}}}

//{{{ outputModel
//##############################################################################
    /**
    * Output for a single model.
    */
    public void outputModel(String mdlFilename, int[] tallies, double rotcor)
    {
        // Target Model TargetCount TargetRotamers ModelRotamers Matches RotCor
        
        System.out.println(
            trgName+":"+
            mdlFilename+":"+
            trgCount+":"+
            tallies[0]+":"+
            tallies[1]+":"+
            tallies[2]+":"+
            df.format(rotcor)
        );
    }
//}}}

//{{{ rotalyzeModel
//##############################################################################
    /**
    * Stores rotamers for the specified model file.
    * Only used for RESIDUE (i.e. not MODEL) output.
    */
    public void rotalyzeModel(String mdlFilename)
    {
        File mdlFile = new File(mdlsDirname+"/"+mdlFilename);
        if(mdlFile.isDirectory()) return; // skip nested directories
        if(verbose) System.err.println("Rotalyzing "+mdlFile);
        Rotalyze rotalyze = new Rotalyze();
        try
        {
            CoordinateFile mdlCoords = new PdbReader().read(mdlFile);
            for(Iterator mItr = mdlCoords.getModels().iterator(); mItr.hasNext(); )
            {
                Model mdl = (Model) mItr.next();
                HashMap<Residue,String> mdlRotNames = rotalyze.getNames(mdl);
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
            SubImpose.getChains(mdl), SubImpose.getChains(trg), 
            new Alignment.NeedlemanWunsch(), new SubImpose.SimpleResAligner());
        //if(verbose)
        //{
        //    System.err.println("Model <==> target residue alignments:");
        //    for(int i = 0; i < align.a.length; i++)
        //        System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
        //    System.err.println();
        //}
        for(Iterator rItr = mdlRotNames.keySet().iterator(); rItr.hasNext(); )
        {
            // Model rotamer we wanna store
            Residue mdlRes    = (Residue) rItr.next();
            String mdlRotName = mdlRotNames.get(mdlRes);
                
            // Find its corresponding target residue
            Residue trgRes = null;
            for(int i = 0; i < align.a.length; i++)
            {
                Residue mRes = (Residue) align.a[i];
                Residue tRes = (Residue) align.b[i];
                if(mRes != null && mRes.getCNIT().equals(mdlRes.getCNIT()))  trgRes = tRes;
            }
            if(trgRes == null) continue; // model has extra non-target tail or something
            
            // Add it to the arrays for this target residue
            String[] mdlRotNamesOld = mdlsRotNames.get(trgRes);
            String[] mdlRotNamesNew;
            if(mdlRotNamesOld != null)
            {
                mdlRotNamesNew = new String[mdlRotNamesOld.length+1];
                for(int i = 0; i < mdlRotNamesOld.length; i++)
                    mdlRotNamesNew[i] = mdlRotNamesOld[i];
            }
            else
            {
                mdlRotNamesNew = new String[1]; // "new" target residue
            }
            mdlRotNamesNew[mdlRotNamesNew.length-1] = mdlRotName; // add
            mdlsRotNames.put(trgRes, mdlRotNamesNew); // re-store
        }
    }
//}}}

//{{{ assessResidue
//##############################################################################
    /**
    * Determines the fraction of model rotamers that match the target rotamer
    * at a particular target residue.
    */
    public void assessResidue(Residue trgRes)
    {
        String   trgRotName  = trgRotNames.get(trgRes);
        String[] mdlRotNames = mdlsRotNames.get(trgRes);
        int      mdlNumRots;
        double   fracMatch;
        if(mdlRotNames != null)
        {
            mdlNumRots = mdlRotNames.length;
            int matches = 0;
            int tally   = 0;
            for(String mdlRotName : mdlRotNames)
            {
                if(mdlRotName.equals(trgRotName)
                && !trgRotName.equals("OUTLIER")
                && !trgRotName.equals(this.NO_CONSENSUS_ROTNAME))  matches++;
                tally++;
            }
            fracMatch = (1.0*matches)/(1.0*tally);
        }
        else // no model residues align to this target residue, so 0% match
        {
            mdlNumRots = 0;
            fracMatch = 0.0;
        }
        
        outputResidue(trgRes, trgRotName, mdlNumRots, fracMatch);
    }
//}}}

//{{{ outputResidue
//##############################################################################
    /**
    * Outputs the fraction of model rotamers that match the target rotamer
    * at a particular target residue.
    */
    public void outputResidue(Residue trgRes, String trgRotName, int mdlNumRots, double fracMatch)
    {
        // Target Chain ResNum InsCode ResType TargetRotamer TargetCount ModelCount FracMatch
        
        System.out.println(
            trgName+":"+
            trgRes.getChain()+":"+
            trgRes.getSequenceInteger()+":"+
            trgRes.getInsertionCode().trim()+":"+
            trgRes.getName()+":"+
            trgRotName+":"+
            trgCount+":"+
            mdlNumRots+":"+
            df.format(fracMatch)
        );
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if(trgFilename == null || mdlsDirname == null)
        {
            showHelp(true);
            System.exit(1);
        }
        while(mdlsDirname.endsWith("/"))
        {
            mdlsDirname = mdlsDirname.substring(0, mdlsDirname.length()-1);
        }
        
        prepTarget();
        assessModels();
    }
    
    public static void main(String[] args)
    {
        RotCor mainprog = new RotCor();
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
            InputStream is = getClass().getResourceAsStream("RotCor.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'RotCor.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.rotarama.RotCor version "+getVersion()+" build "+getBuild());
        System.err.println("Copyright (C) 2010 by Daniel Keedy. All rights reserved.");
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
        if     (mdlsDirname == null) mdlsDirname = arg;
        else if(trgFilename == null) trgFilename = arg;
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
        else if(flag.equals("-model") || flag.equals("-mdl") || flag.equals("-m"))
        {
            mode = MODEL;
        }
        else if(flag.equals("-residue") || flag.equals("-res") || flag.equals("-r"))
        {
            mode = RESIDUE;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class
