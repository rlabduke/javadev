// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>MeasureProOmegas</code> is a simple utility class to measure the omega 
* angle for every proline in the Top5200 and use it to determine whether it's in
* a cis or trans peptide.
* 
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
*/
public class MeasureProOmegas //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String filename                = null;
    boolean verbose                = false;
    ArrayList<String> prolines     = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MeasureProOmegas()
    {
        super();
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        File file = new File(filename);
        LineNumberReader in = new LineNumberReader(new FileReader(file));
        PdbReader pdbReader = new PdbReader();
        CoordinateFile cf = pdbReader.read(in);
        
        Model m = cf.getFirstModel();
        ModelState state = m.getState();
        processModel(cf.getIdCode(), m, state);
    }
    
    public static void main(String[] args)
    {
        MeasureProOmegas mainprog = new MeasureProOmegas();
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

//{{{ processModel
//##############################################################################
    public void processModel(String modelName, Model model, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("#.###");
        
        // Could be, e.g., "../Hoptimize/16pkAH.pdb:"
        Scanner s = new Scanner(filename).useDelimiter("/");
        String possibleFilename = ""+filename;
        while (s.hasNext())   possibleFilename = s.next(); // => "16pkAH.pdb"
        int idxDotPdb = possibleFilename.indexOf(".pdb");
        String pdbidPlus = possibleFilename.substring(0, idxDotPdb); // => "16pkAH"
        String pdbid = "";
        if (pdbidPlus.length() > 4) pdbid = pdbidPlus.substring(0,4); // => "16pk"
        else                        pdbid = pdbidPlus;                // => "16pk"
        
        prolines = new ArrayList<String>();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if (res.getName().equals("PRO"))
            {
                String chain = res.getChain();
                if (chain.equals(" "))   chain = "A";
                String proline = filename+":"+pdbid+":"+chain+":"+res.getSequenceInteger()+":"+"PRO:";
                
                if (res.getPrev(model) != null)
                {
                    if (res.getPrev(model).getSequenceInteger() != res.getSequenceInteger()-1)
                    {
                        // The "previous" residue is probably before a gap and this
                        // proline is at the end of the gap (e.g. 197 in 1amu).
                        // Therefore, we can't *really* measure omega.
                        proline += ":??";
                    }
                    else
                    {
                        try
                        {
                            AtomState ca_prev = state.get(res.getPrev(model).getAtom(" CA "));
                            AtomState c_prev  = state.get(res.getPrev(model).getAtom(" C  "));
                            AtomState n       = state.get(res.getAtom(" N  "));
                            AtomState ca      = state.get(res.getAtom(" CA "));
                            
                            double omega = Double.NaN;
                            omega = Triple.dihedral(ca_prev, c_prev, n, ca); // -180 to 180
                            if (omega > -30 && omega < 30)
                                proline += df.format(omega)+":"+"cis";
                            else if ((omega >= -180 && omega < -150) || (omega > 150 && omega <= 180))
                                proline += df.format(omega)+":"+"trans";
                            else // omega weird or somehow undefined
                                proline += df.format(omega)+":??";
                        }
                        catch (AtomException ae) // one of the four atoms missing
                        { proline += ":??"; }
                    }
                }
                else
                {
                    // There is no previous residue; therefore, we can't *really* measure omega.
                    proline += ":??";
                }
                
                prolines.add(proline);
            }
        }
        
        for (String proline : prolines) System.out.println(proline);
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
            InputStream is = getClass().getResourceAsStream("MeasureProOmegas.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'MeasureProOmegas.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.MeasureProOmegas");
        System.err.println("Copyright (C) 2007 by Daniel Keedy. All rights reserved.");
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
        if (filename == null)
            filename = arg;
        else
            System.out.println("Didn't need "+arg+"; already have file "+filename);
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-v") || flag.equals("-verbose"))
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

