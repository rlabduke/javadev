// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
//}}}
/**
* <code>GrepLines</code> is a utility class for taking the first n lines of a
* file.  It's useful for, say, reducing 2.2GB kinemages you accidentally made to 
* a manageable size (...).
*
* Copyright (C) Daniel Keedy, November 18, 2007.
*/
public class GrepLines //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    boolean verbose = false;
    File file       = null;
    int firstN      = Integer.MAX_VALUE;
    int lastN       = Integer.MAX_VALUE;
    int[] nRange    = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    public GrepLines()
    {
        super();
    }
//}}}

//{{{ doOutput
//##################################################################################################
    public void doOutput()
    {
        try
        {
            if (lastN != Integer.MAX_VALUE)
            {
                int lineCount = 0;
                Scanner s = new Scanner(file);
                while (s.hasNextLine())
                {
                    String line = s.nextLine();
                    lineCount ++;
                    if (lineCount <= lastN)
                        System.out.println(line);
                    else
                        System.exit(0); // to avoid reading through the rest of the file
                }
            }
            else if (firstN != Integer.MAX_VALUE)
            {
                int lineCount = 0;
                Scanner s = new Scanner(file);
                while (s.hasNextLine() && lineCount < firstN-1)
                {
                    String line = s.nextLine();
                    lineCount ++;
                }
                while (s.hasNextLine())   System.out.println(s.nextLine());
            }
            else if (nRange != null)
            {
                int lineCount = 0;
                Scanner s = new Scanner(file);
                while (s.hasNextLine() && lineCount < nRange[0]-1)
                {
                    String line = s.nextLine();
                    lineCount ++;
                }
                while (s.hasNextLine())
                {
                    String line = s.nextLine();
                    lineCount ++;
                    if (lineCount <= nRange[1])
                        System.out.println(line);
                    else
                        System.exit(0); // to avoid reading through the rest of the file
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Trouble reading file...");
        }
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
        
        if (file != null && ( firstN != Integer.MAX_VALUE || lastN != Integer.MAX_VALUE || nRange != null) )
            doOutput();
        else
        {
            System.err.println("Didn't supply a file or line number(s) to start/end at!");
            System.exit(0);
        }
    }

    public static void main(String[] args)
    {
        GrepLines folnl = new GrepLines();
        folnl.Main(args);
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
            System.err.println("  Usage: java FirstNLines [firstN=#|lastN=#|nRange=#,#] 'file'");
            System.err.println();
            System.err.println("  Line numbers start at 0");
            System.err.println();
            System.exit(0);
        }
        else if (flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if (flag.equals("-first"))
        {
            try
            {
                firstN = Integer.parseInt(param);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Can't parse "+param+" as an int!");
            }
        }
        else if (flag.equals("-last"))
        {
            try
            {
                lastN = Integer.parseInt(param);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Can't parse "+param+" as an int!");
            }
        }
        else if (flag.equals("-range"))
        {
            try
            {
                Scanner s = new Scanner(param).useDelimiter(",");
                ArrayList<String> params = new ArrayList<String>();
                while (s.hasNext()) params.add(s.next());
                nRange = new int[params.size()];
                for (int i = 0; i < params.size(); i ++)
                    nRange[i] = Integer.parseInt(params.get(i));
                if (nRange.length != 2)
                {
                    System.err.println("Must provide min & max only, e.g. -n=3,12");
                    nRange = null;
                }
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Can't parse "+param+" as integers!");
            }
        }
        else
            System.out.println("Couldn't understand flag "+flag);
    }
//}}}

}//class

