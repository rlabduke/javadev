// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.nmr;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>NoeCheck</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jul 10 10:31:26 EDT 2003
*/
public class NoeCheck //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    File    pdbFile = null;
    File    noeFile = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public NoeCheck()
    {
        super();
    }
//}}}

//{{{ evalNoes
//##############################################################################
    void evalNoes(PrintWriter out, Collection noes, ModelState ms)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@kinemage 1");
        out.println("@vectorlist {NOE violations} color= gray");
        for(Iterator iter = noes.iterator(); iter.hasNext(); )
        {
            NoeConstraint noe   = (NoeConstraint)iter.next();
            double ideal        = noe.getDistance();
            double actual       = noe.findBest(ms);
            double pct          = (ideal-actual) / ideal;
            
            if(Math.abs(pct) > 0.20)
            {
                String color;
                     if(pct < -0.60)    color = "orange";
                else if(pct < -0.40)    color = "peach";
                else if(pct < -0.20)    color = "peachtint";
                else if(pct > 0.60)     color = "green";
                else if(pct > 0.40)     color = "sea";
                else if(pct > 0.20)     color = "greentint";
                else                    color = "deadwhite";
                
                Triple p1 = noe.getCentroid1(ms);
                Triple p2 = noe.getCentroid2(ms);
                out.println("{1}P "+color+" "+df.format(p1.getX())+" "+df.format(p1.getY())+" "+df.format(p1.getZ()));
                out.println("{2}L "+color+" "+df.format(p2.getX())+" "+df.format(p2.getY())+" "+df.format(p2.getZ()));
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
    public void Main()
    {
        if(pdbFile == null || noeFile == null)
            throw new IllegalArgumentException("Missing file names: NoeCheck <pdbfile> <noefile>");
        
        try
        {
            PdbReader reader    = new PdbReader();
            ModelGroup mg       = reader.read(pdbFile);
            Model model         = mg.getFirstModel();
            ModelState state    = model.getState();
            DyanaReader dyana   = new DyanaReader(model);
            Collection noes     = dyana.readNOEs(noeFile);
            
            evalNoes(new PrintWriter(new OutputStreamWriter(System.out)), noes, state);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        NoeCheck mainprog = new NoeCheck();
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
            InputStream is = getClass().getResourceAsStream("NoeCheck.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'NoeCheck.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.nmr.NoeCheck");
        System.err.println("Copyright (C) 2003 by Ian W. Davis. All rights reserved.");
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
        if(pdbFile == null)
            pdbFile = new File(arg);
        else if(noeFile == null)
            noeFile = new File(arg);
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

