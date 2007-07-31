// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.dangle;

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
    boolean doGeomKin;  // if true make kinemage for each file showing visual 
    			        // representations of angle & dist deviations
    boolean doDistDevsKin = false;
    boolean doAngleDevsKin = false;
    boolean doKinHeadings = false;
    boolean RNAnotDNA = false;
    double sigmaCutoff = 4;
    Collection files = new ArrayList();
    Collection measurements = new ArrayList();
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
        
        out.print("# label:model:chain:number:ins:type");
        int c2o2index = 999; // placeholder value
        for(int i = 0; i < meas.length; i++)
        {
            if (RNAnotDNA && (meas[i].getLabel()).equals("c2o2") && c2o2index == 999)
            {
                // Don't print label for meas c2o2 for the rnabb SuperBuiltin
                c2o2index = i;
            }
            else
            {
                out.print(":"+meas[i].getLabel());
                if(showDeviation)
                    out.print(":sigma "+meas[i].getLabel());
            }
        }
        out.println();
        
        for(int i = 0; i < meas.length; i++)
        {
            // Don't print name for meas c2o2 for the rnabb SuperBuiltin
            if ( !(RNAnotDNA && i == c2o2index) )
                out.println("# "+meas[i]);
        }
        
        double[] vals = new double[meas.length];
        double[] devs = new double[meas.length];
        if (RNAnotDNA && c2o2index != 999) // we're omitting the c2o2 meas
        {
            vals = new double[meas.length - 1];
            devs = new double[meas.length - 1];
        }
        
        for(Iterator models = coords.getModels().iterator(); models.hasNext(); )
        {
            Model model = (Model) models.next();
            ModelState state = model.getState();
            String prefix = label+":"+model.getName()+":";
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                boolean print = false;
                
                for(int i = 0; i < meas.length; i++)
                {
                    if ( !(RNAnotDNA && i == c2o2index) )
                    {
                        vals[i] = meas[i].measure(model, state, res);
                        devs[i] = meas[i].getDeviation();
                        if(!Double.isNaN(vals[i]))
                        {
                            print = true;
                            if(meas[i].getType() == Measurement.TYPE_DIHEDRAL)
                                vals[i] = wrap360(vals[i]);
                        }
                    }
                }
                
                // If we're considering the c2o2 meas for purpose of discrimination btw DNA & RNA
                if (RNAnotDNA && c2o2index != 999) 
                {
                    // If the c2o2 meas turns up NaN, it's DNA --> we wanna omit this residue
                    if ( Double.isNaN(meas[c2o2index].measure(model, state, res)) )
                        print = false;
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
        
        for(Iterator models = coords.getModels().iterator(); models.hasNext(); )
        {
            Model model = (Model) models.next();
            ModelState state = model.getState();
            String prefix = label+":"+model.getName()+":";
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                for(int i = 0; i < meas.length; i++)
                {
                    double val = meas[i].measure(model, state, res);
                    double dev = meas[i].getDeviation();
                    if(!Double.isNaN(dev) && Math.abs(dev) >= sigmaCutoff)
                    {
                        if(meas[i].getType() == Measurement.TYPE_DIHEDRAL)
                            val = wrap360(val);
                        out.print(prefix);
                        out.print(res.getChain()+":"+res.getSequenceNumber()+":"+res.getInsertionCode()+":"+res.getName());
                        out.print(":"+meas[i].getLabel()+":"+df.format(val)+":"+df.format(dev));
                        out.println();
                    }
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
                        sigmaCutoff);
                    gks.makeKin();
                }
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
        for (String arg : args)
        {
            if (arg.equals("rnabb"))        RNAnotDNA = true;
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
        System.err.println("chiropraxis.dangle.Dangle");
        System.err.println("Copyright (C) 2007 by Ian W. Davis. All rights reserved.");
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
            //try { loadMeasures("ParkinsonBerman_ActaCrystD_1996_DNA.txt"); }
            //catch(Exception ex) { ex.printStackTrace(); }
            throw new IllegalArgumentException("No DNA parameters defined yet!");
        }
        else if(flag.equals("-outliers"))
        {
            showDeviation = true;
            outliersOnly = true;
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
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

