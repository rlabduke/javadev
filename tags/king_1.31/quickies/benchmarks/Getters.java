// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports

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
* <code>Getters</code> is designed to test the speed of
* getters vs. direct access to member variables.
*
* Getters appear to be only ~12% slower,
* and each access takes on the order of 2 ns anyway.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jul 11 10:01:13 EDT 2003
*/
public class Getters //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Getters()
    {
        super();
    }
//}}}

//{{{ CLASS: Dummy
//##############################################################################
    static class Dummy
    {
        double v1 = 0.0;
        double v2 = 1.0;
        double v3 = 2.0;
        double v4 = 3.14;
        double v5 = 2.000000001;
        double v6 = 0.6667;
        double v7 = Double.NaN;
        double v8 = 1e100;
        double v9 = -999.999;
        double v10 = 10.0;
        
        double getValue1() { return v1; }
        double getValue2() { return v2; }
        double getValue3() { return v3; }
        double getValue4() { return v4; }
        double getValue5() { return v5; }
        double getValue6() { return v6; }
        double getValue7() { return v7; }
        double getValue8() { return v8; }
        double getValue9() { return v9; }
        double getValue10() { return v10; }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        final int reps = 1 << 27;
        Dummy dummy = new Dummy();
        double dval;
        
        long time = System.currentTimeMillis();
        for(int i = 0; i < reps; i++)
        {
            dval = dummy.v1;
            dval = dummy.v2;
            dval = dummy.v3;
            dval = dummy.v4;
            dval = dummy.v5;
            dval = dummy.v6;
            dval = dummy.v7;
            dval = dummy.v8;
            dval = dummy.v9;
            dval = dummy.v10;
        }
        time = System.currentTimeMillis() - time;
        System.err.println(reps*10+" direct accesses in "+time+" ms");
        
        time = System.currentTimeMillis();
        for(int i = 0; i < reps; i++)
        {
            dval = dummy.getValue1();
            dval = dummy.getValue2();
            dval = dummy.getValue3();
            dval = dummy.getValue4();
            dval = dummy.getValue5();
            dval = dummy.getValue6();
            dval = dummy.getValue7();
            dval = dummy.getValue8();
            dval = dummy.getValue9();
            dval = dummy.getValue10();
        }
        time = System.currentTimeMillis() - time;
        System.err.println(reps*10+" getter accesses in "+time+" ms");
        
    }

    public static void main(String[] args)
    {
        Getters mainprog = new Getters();
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
            InputStream is = getClass().getResourceAsStream("Getters.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Getters.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println(".Getters");
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

