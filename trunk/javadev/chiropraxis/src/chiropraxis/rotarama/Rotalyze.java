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
import driftwood.moldb2.*;

import chiropraxis.sc.SidechainAngles2;
//}}}
/**
* <code>Rotalyze</code> is the command-line executable for rotamer analysis.
* It produces the same raw text output as the old hless.Rotamer, but with
* an additional column that provides the rotamer name.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb 20 16:37:54 EST 2007
*/
public class Rotalyze //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    File infile = null, outfile = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Rotalyze()
    {
        super();
    }
//}}}

//{{{ doCsvText
//##############################################################################
    public void doCsvText(CoordinateFile coordFile, OutputStream outputStream) throws IOException
    {
        // XXX-TODO:  deal with mmCIF files, multiple models, and alt. confs.
        // As is, this class is hideously incomplete, but it does just enough
        // to satisfy MolProbity's needs right now.
        
        DecimalFormat   df1 = new DecimalFormat("0.0");
        PrintWriter     out = new PrintWriter(outputStream);
        out.println("#residue:score%:chi1:chi2:chi3:chi4:rotamer");
        
        Rotamer             rotamer     = Rotamer.getInstance();
        SidechainAngles2    scAngles    = new SidechainAngles2();
        for(Iterator mi = coordFile.getModels().iterator(); mi.hasNext(); )
        {
            Model       model = (Model) mi.next();
            ModelState  state = model.getState();
            for(Iterator ri = model.getResidues().iterator(); ri.hasNext(); )
            {
                try
                {
                    Residue res = (Residue) ri.next();
                    double[] chis = scAngles.measureChiAngles(res, state);
                    double eval = rotamer.evaluate(res.getName(), chis);
                    String rotname = "OUTLIER";
                    if(eval >= 0.01) rotname = rotamer.identify(res.getName(), chis);
                    
                    // (RMI and DAK 07/08/24) Added to fix conversion on OUTLIERs
                    // Was -180 -> 180; now is 0 -> 360
                    for(int i = 0; i < chis.length; i++)
                    {
                       chis[i] = chis[i] % 360;
                       if(chis[i] < 0) chis[i] += 360;
                    }
                    
                    out.print(res.getCNIT());
                    out.print(":");
                    out.print(df1.format(eval*100));
                    for(int i = 0; i < 4; i++)
                    {
                        out.print(":");
                        if(i < chis.length) out.print(df1.format(chis[i]));
                    }
                    out.print(":");
                    out.print(rotname);
                    out.println();
                }
                catch(Exception ex)
                {}//{ System.err.println(ex.getClass()+": "+ex.getMessage()); }
            }
        }
        out.flush();
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
    public void Main() throws IOException
    {
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile;
        if(infile == null)  coordFile = pdbReader.read(System.in);
        else                coordFile = pdbReader.read(infile);
        
        OutputStream out;
        if(outfile == null) out = System.out;
        else out = new BufferedOutputStream(new FileOutputStream(outfile));
        
        doCsvText(coordFile, out);

        try { out.flush(); out.close(); }
        catch(IOException ex) {} // PdfWriter might have already closed it!
    }

    public static void main(String[] args)
    {
        Rotalyze mainprog = new Rotalyze();
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
            InputStream is = getClass().getResourceAsStream("Rotalyze.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Rotalyze.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.rotarama.Rotalyze");
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
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

