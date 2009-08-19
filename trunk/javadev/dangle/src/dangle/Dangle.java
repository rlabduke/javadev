// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package dangle;

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
import driftwood.parser.*;
import driftwood.util.Strings;
//}}}
/**
* <code>Dangle</code> is a flexible replacement for many of Dang's jobs.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 15 15:48:20 EST 2007
*/
public class Dangle //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions (+ version number!)
//##############################################################################
    
    //String versionNumber = "1.02.090518";
    
    boolean forcePDB = false, forceCIF = false;
    boolean doWrap = false; // if true wrap dihedrals to 0 to 360 instead of -180 to 180
    boolean showDeviation = false;
    boolean outliersOnly = false;
    boolean doParCoor = false;
    boolean doGeomKin;  // if true make kinemage for each file showing visual 
    			        // representations of angle & dist deviations
    boolean doDistDevsKin = false;
    boolean doAngleDevsKin = false;
    boolean doKinHeadings = false;
    boolean subgroupNotGroup = false;
    double sigmaCutoff = 4;
    Collection files = new ArrayList();
    Collection measurements = new ArrayList();
    boolean ignoreDNA = false;
    boolean ignoreRNA = false;
    boolean doHets = false;
    ArrayList<Integer> resnums = null;
    int[] resrange = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Dangle()
    {
        super();
    }
//}}}

//{{{ fullOutput
//##############################################################################
    void fullOutput(String label, CoordinateFile coords)
    {
        final PrintStream out = System.out;
        final DecimalFormat df = new DecimalFormat("0.###");
        
        Measurement[] meas = (Measurement[]) measurements.toArray(new Measurement[measurements.size()]);
        
        // Print headings
        out.print("# label:model:chain:number:ins:type");
        int c2o2idx = Integer.MIN_VALUE;
        for(int i = 0; i < meas.length; i++)
        {
            out.print(":"+meas[i].getLabel());
            if(showDeviation)
                out.print(":sigma "+meas[i].getLabel());
        }
        out.println();
        for(int i = 0; i < meas.length; i++)  out.println("# "+meas[i]);
        
        double[] vals = new double[meas.length];
        double[] devs = new double[meas.length];
        
        for(Iterator models = coords.getModels().iterator(); models.hasNext(); )
        {
            Model model = (Model) models.next();
            ModelState state = model.getState();
            String prefix = label+":"+model.getName()+":";
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                int resnum = res.getSequenceInteger();
                if((resnums  == null && resrange == null)
                || (resnums  != null && resnums.contains(resnum))
                || (resrange != null && resrange[0] <= resnum && resrange[1] >= resnum) )
                {
                    boolean print = false;
                    for(int i = 0; i < meas.length; i++)
                    {
                        vals[i] = meas[i].measure(model, state, res, doHets);
                        devs[i] = meas[i].getDeviation();
                        if(!Double.isNaN(vals[i]))
                        {
                            print = true;
                            if(meas[i].getType() == Measurement.TYPE_DIHEDRAL)
                                vals[i] = wrap360(vals[i]);
                        }
                    }
                    /*if(ignoreDNA && print) // not worth checking if print already false
                    {
                        Measurement c2o2 = Measurement.newBuiltin("c2o2");
                        double c2o2dist = c2o2.measure(model, state, res);
                        if(Double.isNaN(c2o2dist)) print = false;
                    }*/
                    if(isNucAcid(res))
                    {
                        if     (isRNA(model, state, res) && ignoreRNA)  print = false;
                        else if(isDNA(model, state, res) && ignoreDNA)  print = false;
                    }
                    if(print)
                    {
                        out.print(prefix);
                        out.print(res.getChain()+":"+res.getSequenceNumber()+":"+res.getInsertionCode()+":"+res.getName());
                        for(int i = 0; i < vals.length; i++)
                        {
                            out.print(":");
                            if(!Double.isNaN(vals[i]))  out.print(df.format(vals[i]));
                            else                        out.print("__?__");
                            if(showDeviation)
                            {
                                out.print(":");
                                if(!Double.isNaN(devs[i]))  out.print(df.format(devs[i]));
                                else                        out.print("__?__");
                            }
                        }
                    out.println();
                    }
                }
            }
        }
    }
//}}}

//{{{ outliersOutput
//##############################################################################
    void outliersOutput(String label, CoordinateFile coords)
    {
        final PrintStream out = System.out;
        final DecimalFormat df = new DecimalFormat("0.###");
        
        Measurement[] meas = (Measurement[]) measurements.toArray(new Measurement[measurements.size()]);
        
        out.println("# label:model:chain:number:ins:type:measure:value:sigmas");
        // This is a LOT of output and hard to interpret...
        //for(int i = 0; i < meas.length; i++)
        //    out.println("# "+meas[i]);
        
        double[] vals = new double[meas.length];
        double[] devs = new double[meas.length];
        
        for(Iterator models = coords.getModels().iterator(); models.hasNext(); )
        {
            Model model = (Model) models.next();
            ModelState state = model.getState();
            String prefix = label+":"+model.getName()+":";
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                int resnum = res.getSequenceInteger();
                if((resnums  == null && resrange == null)
                || (resnums  != null && resnums.contains(resnum))
                || (resrange != null && resrange[0] <= resnum && resrange[1] >= resnum) )
                {
                    boolean print = false;
                    for(int i = 0; i < meas.length; i++)
                    {
                        vals[i] = meas[i].measure(model, state, res, doHets);
                        devs[i] = meas[i].getDeviation();
                        if(!Double.isNaN(vals[i]))
                        {
                            print = true;
                            if(meas[i].getType() == Measurement.TYPE_DIHEDRAL)
                                vals[i] = wrap360(vals[i]);
                        }
                    }
                    /*if(ignoreDNA && print) // not worth checking if print already false
                    {
                        Measurement c2o2 = Measurement.newBuiltin("c2o2");
                        double c2o2dist = c2o2.measure(model, state, res);
                        if(Double.isNaN(c2o2dist)) print = false;
                    }*/
                    if(isNucAcid(res))
                    {
                        if     (isRNA(model, state, res) && ignoreRNA)  print = false;
                        else if(isDNA(model, state, res) && ignoreDNA)  print = false;
                    }
                    if(print)
                    {
                        for(int i = 0; i < vals.length; i++)
                        {
                            if(!Double.isNaN(devs[i]) && Math.abs(devs[i]) >= sigmaCutoff)
                            {
                                out.print(prefix);
                                out.print(res.getChain()+":"+res.getSequenceNumber()+":"+res.getInsertionCode()+":"+res.getName());
                                out.print(":"+meas[i].getLabel()+":"+df.format(vals[i])+":"+df.format(devs[i]));
                                out.println();
                            }
                        }
                    }
                }
            }
        }
    }
//}}}

//{{{ parCoorOutput
//##############################################################################
    void parCoorOutput(String label, CoordinateFile coords)
    {
        final PrintStream out = System.out;
        final DecimalFormat df = new DecimalFormat("0.###");
        
        Measurement[] meas = (Measurement[]) measurements.toArray(new Measurement[measurements.size()]);
        
        // Residue names/numbers should be same in all models, so use model #1 as template
        Iterator m = coords.getModels().iterator();
        Model model1 = (Model) m.next();
        
        // Print headings
        out.print("# model");
        for(Iterator residues = model1.getResidues().iterator(); residues.hasNext(); )
        {
            Residue res = (Residue) residues.next();
            if(resnums == null || resnums.contains(res.getSequenceInteger()))
            {
                if(isProtOrNucAcid(res))
                {
                    for(int i = 0; i < meas.length; i++)
                        out.print(":"+res+" "+meas[i].getLabel());
                }
            }
        }
        out.println();
        
        double[] vals = new double[meas.length];
        double[] devs = new double[meas.length];
        
        for(Iterator models = coords.getModels().iterator(); models.hasNext(); )
        {
            Model model = (Model) models.next();
            ModelState state = model.getState();
            out.print(model.getName());
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                int resnum = res.getSequenceInteger();
                if((resnums  == null && resrange == null)
                || (resnums  != null && resnums.contains(resnum))
                || (resrange != null && resrange[0] <= resnum && resrange[1] >= resnum) )
                {
                    boolean print = false;
                    for(int i = 0; i < meas.length; i++)
                    {
                        vals[i] = meas[i].measure(model, state, res, doHets);
                        devs[i] = meas[i].getDeviation();
                        if(!Double.isNaN(vals[i]))
                        {
                            print = true;
                            if(meas[i].getType() == Measurement.TYPE_DIHEDRAL)
                                vals[i] = wrap360(vals[i]);
                        }
                    }
                    /*if(ignoreDNA && print) // not worth checking if print already false
                    {
                        Measurement c2o2 = Measurement.newBuiltin("c2o2");
                        double c2o2dist = c2o2.measure(model, state, res);
                        if(Double.isNaN(c2o2dist)) print = false;
                    }*/
                    if(isNucAcid(res))
                    {
                        if     (isRNA(model, state, res) && ignoreRNA)  print = false;
                        else if(isDNA(model, state, res) && ignoreDNA)  print = false;
                    }
                    if(print)
                    {
                        // Output all measurements for this residue.
                        // The same thing for the next residue in this model  
                        // will also be on this same line of output
                        for(int i = 0; i < vals.length; i++)
                        {
                            out.print(":");
                            if(!Double.isNaN(vals[i]))  out.print(df.format(vals[i]));
                            else                        out.print("__?__");
                        }
                    }
                }
            } // on to next residue...
            out.println();
        } // on to next model...
    }
//}}}

//{{{ isProtOrNucAcid, isNucAcid, isRNA, isDNA
//##############################################################################
    //static String lowerCa = ":gly:ala:val:phe:pro:met:ile:leu:asp:glu:lys:arg:ser:thr:tyr:his:cys:asn:gln:trp:asx:glx:ace:for:nh2:nme:mse:aib:abu:pca:mly:cyo:m3l:dgn:csd:";
    static String aaNames = ":GLY:ALA:VAL:PHE:PRO:MET:ILE:LEU:ASP:GLU:LYS:ARG:SER:THR:TYR:HIS:CYS:ASN:GLN:TRP:ASX:GLX:ACE:FOR:NH2:NME:MSE:AIB:ABU:PCA:MLY:CYO:M3L:DGN:CSD:";
    static String naNames = ":  C:  G:  A:  T:  U:CYT:GUA:ADE:THY:URA:URI:CTP:CDP:CMP:GTP:GDP:GMP:ATP:ADP:AMP:TTP:TDP:TMP:UTP:UDP:UMP:GSP:H2U:PSU:4SU:1MG:2MG:M2G:5MC:5MU:T6A:1MA:RIA:OMC:OMG: YG:  I:7MG:C  :G  :A  :T  :U  :YG :I  : rC: rG: rA: rT: rU: dC: dG: dA: dT: dU: DC: DG: DA: DT: DU:";
    
    static boolean isProtOrNucAcid(Residue res)
    {
        String resname = res.getName();
        if(aaNames.indexOf(resname) != -1 || naNames.indexOf(resname) != -1) 
            return true; // it's a valid protein or nucleic acid residue name
        return false;
    }

    /** Decides that residue <code>res</code> is nucleic acid simply based simply on residue name */
    static boolean isNucAcid(Residue res)
    {
        String resname = res.getName();
        if(naNames.indexOf(resname) != -1)
            return true; // it's a valid nucleic acid residue name
        return false;
    }

    /**
    * Decides that residue <code>res</code> is RNA if the C2'-O2' distance is measurable.
    * Should only be called on residues you've already decided are nucleic acid!
    */
    static boolean isRNA(Model model, ModelState state, Residue res)
    {
        Measurement c2o2 = Measurement.newBuiltin("c2o2");
        double c2o2dist = c2o2.measure(model, state, res);
        if(Double.isNaN(c2o2dist)) return false; // O2' undetectable - assuming not RNA
        return true;                             // O2' detectable   - assuming RNA
    }

    /**
    * Decides that residue <code>res</code> is DNA if the C2'-O2' distance is NOT measurable.
    * Should only be called on residues you've already decided are nucleic acid!
    */
    static boolean isDNA(Model model, ModelState state, Residue res)
    {
        Measurement c2o2 = Measurement.newBuiltin("c2o2");
        double c2o2dist = c2o2.measure(model, state, res);
        if(Double.isNaN(c2o2dist)) return true; // O2' undetectable - assuming DNA
        return false;                           // O2' detectable   - assuming not DNA
    }
//}}}

//{{{ loadMeasures, wrap360
//##############################################################################
    void loadMeasures(String resourceName) throws IOException, ParseException
    {
        measurements.addAll(
            new Parser().parse(
                new CharWindow(
                    this.getClass().getResource(resourceName)
                )
            )
        );
    }
    
    private double wrap360(double angle)
    {
        if(doWrap)
        {
            angle = angle % 360;
            if(angle < 0) return angle + 360;
            else return angle;
        }
        else return angle;
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
    public void Main() throws IOException, ParseException
    {
        PdbReader pr = new PdbReader();
        CifReader cr = new CifReader();
        if(measurements.isEmpty())
        {
            if(showDeviation) // all validatable measures
                throw new IllegalArgumentException("Must specify measures to validate (try -protein, -rna, -dna)");
            else // all builtins
            {
                Parser parser = new Parser();
                String defaults = parser.BUILTIN.pattern().pattern().replace('|', ' ');
                measurements.addAll(parser.parse(new CharWindow(defaults)));
            }
        }
        
        if(files.isEmpty())
        {
            if(forceCIF)    fullOutput("", cr.read(System.in));
            else            fullOutput("", pr.read(System.in));
        }
        else
        {
            for(Iterator iter = files.iterator(); iter.hasNext(); )
            {
                CoordinateFile coords;
                File f = (File) iter.next();
                if(forceCIF)
                    coords = cr.read(f);
                else if(forcePDB)
                    coords = pr.read(f);
                else if(f.getName().toLowerCase().endsWith(".cif"))
                    coords = cr.read(f);
                else
                    coords = pr.read(f);
                
                if(doGeomKin)
                {
                    GeomKinSmith gks = new GeomKinSmith( 
                        (ArrayList<Measurement>) measurements, f.getName(), 
                        coords, doDistDevsKin, doAngleDevsKin, doKinHeadings, 
                        sigmaCutoff, subgroupNotGroup, doHets, ignoreDNA, 
                        ignoreRNA, resnums, resrange);
                    gks.makeKin();
                }
                else if (doParCoor)
                    parCoorOutput(f.getName(), coords);
                else if(outliersOnly)
                    outliersOutput(f.getName(), coords);
                else
                    fullOutput(f.getName(), coords);
            }
        }
    }

    public static void main(String[] args)
    {
	Dangle mainprog = new Dangle();
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
        catch(IOException ex) { ex.printStackTrace(); }
        catch(ParseException ex) { ex.printStackTrace(); }
    }
//}}}

//{{{ parseArguments
//##############################################################################
    /**
    * Parse the command-line options for this program.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    void parseArguments(String[] args)
    {
        if(args.length == 0)
        {
            showHelp(true);
            System.exit(1);
        }
        
        /*for(String arg : args)
            if(arg.equals("rnabb") || arg.equals("-rna"))
                ignoreDNA = true;*/
        boolean doDNA = false;
        boolean doRNA = false;
        for(String arg : args)
        {
            if(arg.equals("-rna") || arg.equals("rnabb"))  doRNA = true;
            if(arg.equals("-dna") || arg.equals("dnabb"))  doDNA = true;
        }
        if     (!doDNA &&  doRNA) ignoreDNA = true;   // eval just RNA
        else if( doDNA && !doRNA) ignoreRNA = true;   // eval just DNA
        else if( doDNA &&  doRNA) System.err.println( // eval both DNA + RNA w/ diff ideal vals
            "WARNING: Simultaneous use of -dna & -rna may cause strange behavior!");
        
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
//}}}

//{{{ showHelp, showChanges, getVersion
//##############################################################################
    /**
    * Parse the command-line options for this program.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("Dangle.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Dangle.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        String versionNumber = getVersion();
        System.err.println("dangle.Dangle version " + versionNumber);
        System.err.println("Copyright (C) 2007 by Ian W. Davis. All rights reserved.");
    }

    // Display changes information
    void showChanges(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("Dangle.changes");
            if(is == null)
                System.err.println("\n*** Unable to locate changes information in 'Dangle.changes' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        String versionNumber = getVersion();
        System.err.println("dangle.Dangle version " + versionNumber);
        System.err.println("Copyright (C) 2007 by Ian W. Davis. All rights reserved.");
    }

    // Get version number
    String getVersion()
    {
        InputStream is = getClass().getResourceAsStream("version.props");
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
        File f = new File(arg);
        if(f.exists()) files.add(f);
        else
        {
            try { measurements.addAll(new Parser().parse(new CharWindow(arg))); }
            catch(Exception ex)
            {
                ex.printStackTrace();
                throw new IllegalArgumentException(ex.getMessage());
            }
        }
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        if(flag.equals("-changes"))
        {
            showChanges(true);
            System.exit(0);
        }
        else if(flag.equals("-cif"))
        {
            if(forcePDB) throw new IllegalArgumentException("Can't specify both -cif and -pdb");
            forceCIF = true;
        }
        else if(flag.equals("-pdb"))
        {
            if(forceCIF) throw new IllegalArgumentException("Can't specify both -cif and -pdb");
            forcePDB = true;
        }
        else if(flag.equals("-360"))
            doWrap = true;
        else if(flag.equals("-validate"))
            showDeviation = true;
        else if(flag.equals("-sigma"))
        {
            try { this.sigmaCutoff = Double.parseDouble(param); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException("Expected -sigma=#.#"); }
        }
        else if(flag.equals("-prot") || flag.equals("-protein") || flag.equals("-proteins"))
        {
            try { loadMeasures("EnghHuber_IntlTblsF_1999.txt"); }
            catch(Exception ex) { ex.printStackTrace(); }
        }
        else if(flag.equalsIgnoreCase("-rna"))
        {
            try { loadMeasures("ParkinsonBerman_ActaCrystD_1996_RNA.txt"); }
            catch(Exception ex) { ex.printStackTrace(); }
        }
        else if(flag.equalsIgnoreCase("-dna"))
        {
            try { loadMeasures("GelbinBerman_JACS_1996_DNA.txt"); }
            catch(Exception ex) { ex.printStackTrace(); }
            //throw new IllegalArgumentException("No DNA parameters defined yet!");
        }
        else if(flag.equals("-outliers"))
        {
            showDeviation = true;
            outliersOnly = true;
        }
        else if(flag.equals("-parcoor"))
        {
            doParCoor = true;
        }
        else if(flag.equals("-geometrykin") || flag.equals("-geomkin") || flag.equals("-kin"))
        {
            doGeomKin = true;
            doDistDevsKin = true;
            doAngleDevsKin = true;
        }
        else if(flag.equals("-distancedevskin") || flag.equals("-distdevskin"))
        {
            doGeomKin = true;
            doDistDevsKin = true;
        }
        else if(flag.equals("-angledevskin") || flag.equals("-angdevskin"))
        {
            doGeomKin = true;
            doAngleDevsKin = true;
        }
        else if(flag.equals("-kinheading"))
        {
            doKinHeadings = true;
        }
        else if(flag.equals("-sub") || flag.equals("-subgroup"))
        {
            subgroupNotGroup = true;
        }
        else if(flag.equals("-dohets") || flag.equals("-hets"))
        {
            doHets = true;
        }
        else if(flag.equals("-res") || flag.equals("-resnum"))
        {
            if(param.indexOf("-") != -1)
            {
                // Range of residue numbers ("1-99")
                try
                {
                    String[] resNumbers = Strings.explode(param, '-', false, true);
                    resrange = new int[2];
                    resnums = null;
                    for(int i = 0; i < resNumbers.length; i ++)
                        resrange[i] = Integer.parseInt(resNumbers[i]);
                }
                catch(NumberFormatException nfe) { resrange = null; }
            }
            else
            {
                // Set of residue numbers ("1,2,10,...")
                try
                {
                    String[] resNumbers = Strings.explode(param, ',', false, true);
                    resnums = new ArrayList<Integer>();
                    resrange = null;
                    for(String resNumber : resNumbers)
                        resnums.add(Integer.parseInt(resNumber));
                }
                catch(NumberFormatException nfe) { resnums = null; }
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

