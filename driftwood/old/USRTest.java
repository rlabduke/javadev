// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.r3;

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
* <code>USRTest</code> has not yet been documented.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 20 15:38:48 EDT 2007
*/
public class USRTest //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public USRTest()
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
        PdbReader pdbReader = new PdbReader();
        CoordinateFile coordFile = pdbReader.read(System.in);
        Collection usrObjs = new ArrayList();
        for(Iterator mi = coordFile.getModels().iterator(); mi.hasNext(); )
        {
            Model model = (Model) mi.next();
            ModelState state = model.getState().createForModel(model);
            Collection atomStates = state.getLocalStateMap().values();
            UltrafastShapeRecognition usr = new UltrafastShapeRecognition(atomStates);
            usrObjs.add(usr);
        }
        UltrafastShapeRecognition[] usrs = (UltrafastShapeRecognition[]) usrObjs.toArray(new UltrafastShapeRecognition[usrObjs.size()]);
        Model[] models = (Model[]) coordFile.getModels().toArray(new Model[usrObjs.size()]);
        System.out.print(",");
        for(int i = 0; i < usrs.length; i++)
            System.out.print(models[i].getName()+",");
        System.out.println();
        for(int i = 0; i < usrs.length; i++)
        {
            System.out.print(models[i].getName()+",");
            for(int j = 0; j < usrs.length; j++)
            {
                System.out.print(usrs[i].scoreUSR(usrs[j]));
                System.out.print(",");
            }
            System.out.println();
        }
    }

    public static void main(String[] args)
    {
        USRTest mainprog = new USRTest();
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
            InputStream is = getClass().getResourceAsStream("USRTest.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'USRTest.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("driftwood.r3.USRTest");
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

