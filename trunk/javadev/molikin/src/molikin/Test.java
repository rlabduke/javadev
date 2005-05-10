// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>Test</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon May  9 08:54:43 EDT 2005
*/
public class Test //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Test()
    {
        super();
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
        File infile = null; // FIXME later
        
        long time = System.currentTimeMillis();
        PdbReader   pdbReader   = new PdbReader();
        ModelGroup  modelGroup;
        if(infile == null)  modelGroup = pdbReader.read(System.in);
        else                modelGroup = pdbReader.read(infile);
        Model model = modelGroup.getFirstModel();
        time = System.currentTimeMillis() - time;
        System.err.println("Loading PDB:         "+time+" ms");

        time = System.currentTimeMillis();
        AtomGraph graph = new AtomGraph(Util.extractOrderedStatesByName(model));
        time = System.currentTimeMillis() - time;
        System.err.println("Initializing graph:  "+time+" ms");

        time = System.currentTimeMillis();
        SortedSet bonds = graph.getCovalentBonds();
        time = System.currentTimeMillis() - time;
        System.err.println("Determining network: "+time+" ms");

        time = System.currentTimeMillis();
        printBonds(System.out, bonds);
        time = System.currentTimeMillis() - time;
        System.err.println("Drawing bonds:       "+time+" ms");
    }

    public static void main(String[] args)
    {
        Test mainprog = new Test();
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

//{{{ printBonds
//##############################################################################
    static void printBonds(PrintStream out, Collection bonds)
    {
        DecimalFormat df = new DecimalFormat("0.000");
        out.println("@kinemage");
        out.println("@vectorlist {bonds?} color= yellow");
        Bond last = new Bond(null, -1, null, -1);
        for(Iterator iter = bonds.iterator(); iter.hasNext(); )
        {
            Bond curr = (Bond) iter.next();
            if(curr.iLow != last.iHigh)
                out.print("{"+curr.lower.getAtom()+"}P "+curr.lower.format(df)+" ");
            out.println("{"+curr.higher.getAtom()+"}L "+curr.higher.format(df));
            last = curr;
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
            InputStream is = getClass().getResourceAsStream("Test.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Test.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("molikin.Test");
        System.err.println("Copyright (C) 2005 by Ian W. Davis. All rights reserved.");
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

