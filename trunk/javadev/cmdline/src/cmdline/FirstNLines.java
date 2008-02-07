// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
//}}}
/**
* <code>FirstNLines</code> is a utility class for taking the first n lines of a
* file.  It's useful for, say, reducing 2.2GB kinemages you accidentally made to 
* a manageable size (...).
*
* Copyright (C) Daniel Keedy, November 18, 2007.
*/
public class FirstNLines //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    boolean verbose = false;
    File file = null;
    int n = Integer.MAX_VALUE;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    public FirstNLines()
    {
        super();
    }
//}}}

//{{{ Main, main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main(String[] args)
    {
        parseArgs(args);
        if (verbose)
        {
            System.out.println("Finished parsing args...");
            System.out.println("Input file is '"+file+"'");
        }
        
        if (file != null && n != Integer.MAX_VALUE)
            doOutput();
        else
        {
            System.err.println("Didn't supply a file or integer n!");
            System.exit(0);
        }
    }

    public static void main(String[] args)
    {
        FirstNLines fnl = new FirstNLines();
        fnl.Main(args);
    }
//}}}

//{{{ parseArgs
//##################################################################################################
    public void parseArgs(String[] args)
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
//}}}

//{{{ interpretArg, interpretFlag
//##################################################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
        if (file == null)       file = new File(arg);
        else
            System.err.println("Couldn't understand flag "+arg+"... "
                +" Already have input file!");
    }

    public void interpretFlag(String flag, String param)
    {
        // Look thru flags
        if (flag.equals("-help") || flag.equals("-h"))
        {
            System.err.println();
            System.err.println("  Usage: java FirstNLines -n=# 'file'");
            System.err.println();
            System.exit(0);
        }
        else if (flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if (flag.equals("-n"))
        {
            try
            {
                n = Integer.parseInt(param);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Can't parse "+param+" as an int!");
            }
        }
        else
            System.out.println("Couldn't understand flag "+flag);
    }
//}}}

//{{{ doOutput
//##################################################################################################
    public void doOutput()
    {
        try
        {
            int lineCount = 0;
            Scanner s = new Scanner(file);
            while (s.hasNextLine())
            {
                String line = s.nextLine();
                lineCount ++;
                if (lineCount <= n)
                    System.out.println(line);
                else
                    System.exit(0); // to avoid reading through the rest of the file
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Trouble reading file...");
        }
    }
//}}}

}//class

