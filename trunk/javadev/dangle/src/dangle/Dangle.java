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

//{{{ Variable definitions
//##############################################################################
    boolean forcePDB = false, forceCIF = false;
    boolean doWrap = false; // if true wrap dihedrals to 0 to 360 instead of -180 to 180
    boolean showDeviation = false;
    boolean outliersOnly = false;
    boolean doHets = false;
    boolean doParCoor = false;
    boolean doGeomKin;  // if true make kinemage for each file showing 
    			        // visual representations of geometry outliers
    boolean subgroup = false;
    double sigmaCutoff = 4;
    Collection files = new ArrayList();
    Collection measurements = new ArrayList();
    TreeSet<Integer> resnums;
    String altConf;
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
        
        // All Measurements have been defined at this point, so we know 
        // if it's necessary to deploy disulfide info to models or not.
        boolean anyDisulfMeasures = false;
        for(int i = 0; i < meas.length; i++)
            if(meas[i].resSpec != null && meas[i].resSpec.requireDisulf)
                anyDisulfMeasures = true;
        if(anyDisulfMeasures) coords.deployDisulfidesToModels();
        
        // Print headings
        out.print("# label:model:chain:number:ins:type");
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
            ModelState state = (altConf == null ? model.getState() : model.getState(altConf));
            if(state == null) System.err.println("Input structure "+coords.getIdCode()+
                " [model "+model+"] does not contain a state named '"+altConf+"'!");
            String prefix = label+":"+model.getName()+":";
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                if(resnums == null || resnums.contains(res.getSequenceInteger()))
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
        
        coords.deployDisulfidesToModels();
        
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
            ModelState state = (altConf == null ? model.getState() : model.getState(altConf));
            if(state == null) System.err.println("Input structure "+coords.getIdCode()+
                " [model "+model+"] does not contain a state named '"+altConf+"'!");
            String prefix = label+":"+model.getName()+":";
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                if(resnums == null || resnums.contains(res.getSequenceInteger()))
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
        
        coords.deployDisulfidesToModels();
        
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
                if(Measurement.isProtOrNucAcid(res) 
                && (doHets || (!doHets && !Measurement.isHet(res))))
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
            ModelState state = (altConf == null ? model.getState() : model.getState(altConf));
            if(state == null) System.err.println("Input structure "+coords.getIdCode()+
                " [model "+model+"] does not contain a state named '"+altConf+"'!");
            out.print(model.getName());
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                if(resnums == null || resnums.contains(res.getSequenceInteger()))
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

//{{{ geomKinOutput
//##############################################################################
    public void geomKinOutput(String label, CoordinateFile coords)
    {
        coords.deployDisulfidesToModels();
        GeomKinSmith gks = new GeomKinSmith( 
            label,
            coords,
            (ArrayList<Measurement>) measurements,
            sigmaCutoff,
            subgroup,
            doHets,
            resnums,
            altConf
        );
        try { gks.makeKin(); }
        catch(IllegalArgumentException ex) { ex.printStackTrace(); }
    }
//}}}

//{{{ chooseMeasures
//##############################################################################
    /**
    * Chooses the set of measurements to be used.  Useful because we sometimes 
    * want to load up a different set depending on what flags were provided.
    * If no measurements specified for geom kin output, use only relevant measurements.
    * If no measurements specified for text output, load up all builtins.
    */
    public void chooseMeasures() throws IOException, ParseException
    {
        if(measurements.isEmpty()) 
        {
            if(doGeomKin)
            {
                System.err.println("No geom kin measurements specified -- loading defaults! (including cbdev)");
                measurements = new ArrayList();
                showDeviation = true;
                try { loadMeasures("EnghHuber_IntlTblsF_1999.txt"); }
                catch(Exception ex) { ex.printStackTrace(); }
                try { loadMeasures("ParkinsonBerman_ActaCrystD_1996_RNA.txt"); }
                catch(Exception ex) { ex.printStackTrace(); }
                try { loadMeasures("GelbinBerman_JACS_1996_DNA.txt"); }
                catch(Exception ex) { ex.printStackTrace(); }
                try { measurements.add(Measurement.newBuiltin("cbdev")); }
                catch(Exception ex) { ex.printStackTrace(); }
            }
            else
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
        }
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

//{{{ parseResnums
//##############################################################################
    void parseResnums(String r) throws NumberFormatException
    {
        // Something like "1-99,4,58-71"
        resnums = new TreeSet<Integer>();
        String[] ranges = Strings.explode(r, ',', false, true);
        for(String range : ranges)
        {
            if(range.indexOf("-") != -1)
            {
                // Range of residue numbers ("1-99")
                String[] rangeBegEnd = Strings.explode(range, '-', false, true);
                int beg = Integer.parseInt(rangeBegEnd[0]); // "1"
                int end = Integer.parseInt(rangeBegEnd[1]); // "99"
                if(end >= beg)
                    for(int curr = beg; curr <= end; curr++) resnums.add(curr);
                else //if(end < beg)
                    for(int curr = end; curr <= beg; curr++) resnums.add(curr);
            }
            else
            {
                // Single residue number ("4")
                resnums.add(Integer.parseInt(range));
            }
        }
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
        
        chooseMeasures();
        
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
                    geomKinOutput(f.getName(), coords);
                else if(doParCoor)
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
        System.err.println("dangle.Dangle version "+getVersion()+" build "+getBuild());
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
        System.err.println("dangle.Dangle version "+getVersion()+" build "+getBuild());
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

    // Get build number
    String getBuild()
    {
        InputStream is = getClass().getResourceAsStream("buildnum.props");
        if(is == null)
            System.err.println("\n*** Unable to locate version number in 'buildnum.props' ***\n");
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
        {
            doWrap = true;
        }
        else if(flag.equals("-validate") || flag.equals("-val"))
        {
            showDeviation = true;
        }
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
        }
        else if(flag.equals("-sub") || flag.equals("-subgroup"))
        {
            subgroup = true;
        }
        else if(flag.equals("-dohets") || flag.equals("-hets"))
        {
            doHets = true;
        }
        else if(flag.equals("-res") || flag.equals("-resnum") || flag.equals("-resnums") || flag.equals("-residues"))
        {
            try { parseResnums(param); }
            catch(NumberFormatException ex)
            { System.err.println("*** Error: Can't format '"+param+"' as list of resnum ranges!"); }
            catch(Exception ex) { ex.printStackTrace(); }
        }
        else if(flag.equals("-alt"))
        {
            if(param.length() == 1) altConf = param;
            else System.err.println("*** Error: Preferred alternate (-alt="+param+") must be one character!");
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

