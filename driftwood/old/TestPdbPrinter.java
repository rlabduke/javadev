// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>TestPdbPrinter</code> takes a PDB file on stdin
* and prints a tree-view of it to stdout.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jun 13 13:19:47 EDT 2003
*/
public class TestPdbPrinter //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public TestPdbPrinter()
    {
        super();
    }
//}}}

//{{{ treePrint
//##################################################################################################
    static void treePrint(Model model, Collection modelStates)
    {
        ModelState[] states = (ModelState[])modelStates.toArray(new ModelState[modelStates.size()]);
        
        System.out.println("Model "+model);
        for(Iterator riter = model.getResidues().iterator(); riter.hasNext(); )
        {
            Residue res = (Residue)riter.next();
            System.out.println("+-"+res);
            for(Iterator aiter = res.getAtoms().iterator(); aiter.hasNext(); )
            {
                Atom atom = (Atom)aiter.next();
                for(int si = 0; si < states.length; si++)
                {
                    AtomState astate = states[si].getLocal(atom);
                    if(astate != null) System.out.println("| +-"+astate);
                }
            }
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ Main, main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        PdbReader reader = new PdbReader();
        try
        {
            long readTime = System.currentTimeMillis();
            ModelGroup group = reader.read(System.in);
            readTime = System.currentTimeMillis() - readTime;
            
            long printTime = System.currentTimeMillis();
            for(Iterator iter = group.getModels().iterator(); iter.hasNext(); )
            {
                Model model = (Model)iter.next();
                treePrint(model, model.getStates());
            }
            printTime = System.currentTimeMillis() - printTime;
            
            System.out.println();
            System.out.println();
            System.out.println("Required "+readTime+" ms to load PDB file into memory.");
            System.out.println("Required "+printTime+" ms to traverse PDB file and print atoms.");
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.err.println("*** Reading PDB file failed on I/O ***");
        }
    }

    public static void main(String[] args)
    {
        TestPdbPrinter mainprog = new TestPdbPrinter();
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
//##################################################################################################
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
                { throw new IllegalArgumentException("'"+arg+"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("TestPdbPrinter.help");
            if(is == null) System.err.println("\n*** Unable to locate help information in 'TestPdbPrinter.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("driftwood.moldb2.TestPdbPrinter");
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
//##################################################################################################
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

