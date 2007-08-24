// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>Ramalyze</code> is the executable Ramachandran-analysis program
* in chiropraxis.rotarama.
*
* Ramalyze loads and analyzes the models and calculates overall statistics,
* but delegates the task of generating output files to other classes.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar  8 08:56:34 EST 2004
*/
public class Ramalyze //extends ... implements ...
{
//{{{ Constants
    public static final Object MODE_PDF = "PDF document";
    //public static final Object MODE_KIN = "Kinemage";
    public static final Object MODE_RAW = "Raw csv output"; // added by DAK 07/08/24
    public static final DecimalFormat df = new DecimalFormat("#.##"); // added by DAK 07/08/24
//}}}

//{{{ CLASS: RamaEval
//##############################################################################
    /** Provides an evaluation for a single residue */
    public static class RamaEval
    {
        public static final String FAVORED = "Favored";
        public static final String ALLOWED = "Allowed";
        public static final String OUTLIER = "OUTLIER";
        public static final String NOSCORE = "Not evaluated";
        
        public static final String GENERAL = "General case";
        public static final String GLYCINE = "Glycine";
        public static final String PROLINE = "Proline";
        public static final String PREPRO  = "Pre-proline";
        public static final String NOTYPE  = "Unknown type";
        
        Residue res;
        String name; // starts as res.toString(), may be improved later
        String modelName;
        public float phi = 0, psi = 0;
        public float numscore = 0;
        public String score = NOSCORE;
        public String type  = NOTYPE;
        
        public RamaEval(Residue res, String modelName)
        {
            this.res = res;
            this.name = res.toString();
            this.modelName = modelName;
        }
        
        /** Equal iff Residues have the same name and same phi, psi. */
        public boolean equals(Object o)
        {
            if(!(o instanceof RamaEval)) return false;
            RamaEval that = (RamaEval) o;
            //System.err.println(this.res+" ?= "+that.res);
            //System.err.println(this.phi+" ?= "+that.phi);
            //System.err.println(this.psi+" ?= "+that.psi);
            return (this.res.toString().equals(that.res.toString()))
                && (this.phi == that.phi) && (this.psi == that.psi);
        }
        
        public int hashCode()
        { return this.res.toString().hashCode(); }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    File infile = null, outfile = null;
    Object mode = MODE_PDF;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Ramalyze()
    {
        super();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ improveResidueNames
//##############################################################################
    /**
    * Takes a Collection of RamaEval objects and improves the residue names
    * by removing redundant chain IDs and/or segment IDs.
    */
    static void improveResidueNames(Collection analysis, boolean useModelNames)
    {
        Set segIDs = new HashSet();
        Set chainIDs = new HashSet();
        for(Iterator iter = analysis.iterator(); iter.hasNext(); )
        {
            RamaEval eval = (RamaEval) iter.next();
            segIDs.add( eval.res.getSegment() );
            chainIDs.add(eval.res.getChain());
        }
        
        boolean useSegs = (segIDs.size() > 1);
        boolean useChains = (chainIDs.size() > 1);
        for(Iterator iter = analysis.iterator(); iter.hasNext(); )
        {
            RamaEval eval = (RamaEval) iter.next();
            Residue r = eval.res;
            StringBuffer name = new StringBuffer();
            if(useModelNames)           name.append("[").append(eval.modelName).append("] ");
            if(useChains)
            {
                if(" ".equals(r.getChain()))    name.append("_ ");
                else                            name.append(r.getChain()).append(' ');
            }
            if(useSegs)                 name.append(r.getSegment().trim()).append(' ');
            name.append(r.getSequenceNumber());
            if(!" ".equals(r.getInsertionCode())) name.append(r.getInsertionCode());
            name.append(' ');
            name.append(r.getName());
            eval.name = name.toString();
        }
    }
//}}}

//{{{ analyzeModel
//##############################################################################
    /**
    * Performs Ramachandran analysis on one Model and its associated ModelStates.
    * @return a Set of RamaEval objects. One object is produced for each protein
    * residue with a measurable phi, psi and a unique conformation.
    * @throws IOException if the Ramachandran evaluator cannot be loaded.
    */
    static public Set analyzeModel(Model model, Collection modelStates) throws IOException
    {
        String protein = "GLY,ALA,VAL,LEU,ILE,PRO,PHE,TYR,TRP,SER,THR,CYS,MET,MSE,LYS,HIS,ARG,ASP,ASN,GLN,GLU";
        UberSet analysis = new UberSet();
        Ramachandran rama = Ramachandran.getInstance();
        
        for(Iterator ri = model.getResidues().iterator(); ri.hasNext(); )
        {
            Residue res = (Residue) ri.next();
            if(protein.indexOf(res.getName()) == -1) continue;
            
            for(Iterator msi = modelStates.iterator(); msi.hasNext(); )
            {
                try
                {
                    ModelState ms = (ModelState) msi.next();
                    RamaEval eval = new RamaEval(res, model.getName());
                    eval.phi = (float) AminoAcid.getPhi(model, res, ms);
                    eval.psi = (float) AminoAcid.getPsi(model, res, ms);
                    eval.numscore = (float) rama.rawScore(model, res, ms);
                    
                    if(res.getName().equals("GLY"))             eval.type = RamaEval.GLYCINE;
                    else if(res.getName().equals("PRO"))        eval.type = RamaEval.PROLINE;
                    else if(AminoAcid.isPrepro(model, res, ms)) eval.type = RamaEval.PREPRO;
                    else                                        eval.type = RamaEval.GENERAL;
                    
                    if(eval.numscore >= Ramachandran.ALL_FAVORED)
                        eval.score = RamaEval.FAVORED;
                    else if(eval.type == RamaEval.GENERAL && eval.numscore >= Ramachandran.GENERAL_ALLOWED)
                        eval.score = RamaEval.ALLOWED;
                    else if(eval.type != RamaEval.GENERAL && eval.numscore >= Ramachandran.OTHER_ALLOWED)
                        eval.score = RamaEval.ALLOWED;
                    else
                        eval.score = RamaEval.OUTLIER;
                    
                    analysis.add(eval);
                }
                catch(AtomException ex) {}
                catch(ResidueException ex) {}
            }
        }
        
        return analysis;
    }
//}}}

//{{{ runAnalysis
//##############################################################################
    /**
    * @param inputPdbFile the structure to analyze in standard PDB format
    * @param out the destination for PDF, kinemage, whatever
    * @param mode one of the MODE_XXX constants from this class
    * @throws IOException if the Ramachandran evaluator can't be loaded,
    *   or if the PDB file can't be read in.
    * @throws IllegalArgumentException if an unknown mode is requested
    */
    static public void runAnalysis(InputStream inputPdbFile, OutputStream out, Object mode) throws IOException
    { runAnalysis( (new PdbReader()).read(inputPdbFile), out, mode ); }
    
    static public void runAnalysis(CoordinateFile coordFile, OutputStream out, Object mode) throws IOException
    {
        Map analyses = new UberMap();
        for(Iterator iter = coordFile.getModels().iterator(); iter.hasNext(); )
        {
            Model model = (Model) iter.next();
            Collection analysis = analyzeModel(model, model.getStates().values());
            boolean useModelNames = (coordFile.getModels().size() > 1);
            improveResidueNames(analysis, useModelNames);
            analyses.put(analysis, model.getName());
        }
        
        String label = null;
        if(coordFile.getFile() != null)
            label = coordFile.getFile().getName();
        else if(coordFile.getIdCode() != null)
            label = coordFile.getIdCode();
        
        if(mode == MODE_PDF)
        {
            RamaPdfWriter writer = new RamaPdfWriter();
            writer.createRamaPDF(analyses, label, out);
            try { out.flush(); }
            catch(IOException ex) {} // PdfWriter might have already closed it!
        }
        if(mode == MODE_RAW) // added by DAK 07/08/24
        {
            // Print RamaEval.numscores separated by colons
            int i = 0;
            for (Iterator iter = analyses.keySet().iterator(); iter.hasNext(); i++) // each model
            {
                Collection analysis = (Collection) iter.next();
                for (Iterator iter2 = analysis.iterator(); iter2.hasNext(); ) // each residue
                {
                    RamaEval eval = (RamaEval) iter2.next();
                    System.out.println(eval.name+":"+df.format(100*eval.numscore)+":"+
                        df.format(eval.phi)+":"+df.format(eval.psi)+":"+eval.score+":"+eval.type);
                }
            }
        }
        
        // TODO: else if(mode == MODE_KIN) ...
        else throw new IllegalArgumentException("Unknown output mode: "+mode);
    }
//}}}

///{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile;
        if(infile == null)  coordFile = pdbReader.read(System.in);
        else                coordFile = pdbReader.read(infile);
        
        OutputStream out;
        if(outfile == null) out = System.out;
        else out = new BufferedOutputStream(new FileOutputStream(outfile));
        
        runAnalysis(coordFile, out, this.mode);

        try { out.flush(); out.close(); }
        catch(IOException ex) {} // PdfWriter might have already closed it!
    }

    public static void main(String[] args)
    {
        // If we fail to do this, Java will crash if this program
        // is run from a non-graphical (e.g. script) environment.
        System.setProperty("java.awt.headless", "true");
        
        Ramalyze mainprog = new Ramalyze();
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
            InputStream is = getClass().getResourceAsStream("Ramalyze.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Ramalyze.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.rotarama.Ramalyze");
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
        if(infile == null)
            infile = new File(arg);
        else if(outfile == null)
            outfile = new File(arg);
        else throw new IllegalArgumentException("Too many file names on cmd line: '"+arg+"'");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-pdf"))
        {
            mode = MODE_PDF;
        }
        else if(flag.equals("-raw")) // added by DAK 07/08/24
        {
            mode = MODE_RAW;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

