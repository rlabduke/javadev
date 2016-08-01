// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gnutil;

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
* <code>Test</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  9 09:43:33 EDT 2003
*/
public class Test //extends ... implements ...
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
    public Test()
    {
        super();
    }
//}}}

//{{{ prettyPrint
//##################################################################################################
    public void prettyPrint(Collection c)
    {
        Iterator iter = c.iterator();
        while(iter.hasNext())
        {
            System.out.println("    "+iter.next());
        }
    }
//}}}

//{{{ Main, main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        String[] setData = {null, "First", "Second", "Third", "Fourth", "Fifth",
            "Sixth", "Seventh", "Eighth", "Ninth", "Tenth and final"};
        
        GnuLinkedHashSet set = new GnuLinkedHashSet(Arrays.asList(setData));
        
        System.out.println("Starting:");
        prettyPrint(set);
        
        System.out.println("Eighth before Third:");
        set.moveBefore("Eighth", "Third");
        prettyPrint(set);
        
        System.out.println("Sixth after First:");
        set.moveAfter("Sixth", "First");
        prettyPrint(set);
        
        System.out.println("Fifth to the front:");
        set.moveBefore("Fifth", null);
        prettyPrint(set);
        
        System.out.println("Second to the back:");
        set.moveAfter("Second", "Tenth and final");
        prettyPrint(set);
        
        System.out.println("Before Fifth (null): "+set.getBefore("Fifth"));
        System.out.println("After Fifth (null): "+set.getAfter("Fifth"));
        System.out.println("Before First (null): "+set.getBefore("First"));
        System.out.println("After First (Sixth): "+set.getAfter("First"));
        System.out.println("Before Third (Eighth): "+set.getBefore("Third"));
        System.out.println("After Third (Fourth): "+set.getAfter("Third"));
        System.out.println("Before Fourth (Third): "+set.getBefore("Fourth"));
        System.out.println("After Fourth (Seventh): "+set.getAfter("Fourth"));
        System.out.println("Before Ninth (Seventh): "+set.getBefore("Ninth"));
        System.out.println("After Ninth (Tenth and final): "+set.getAfter("Ninth"));
        System.out.println("Before Second (Tenth and final): "+set.getBefore("Second"));
        System.out.println("After Second (null): "+set.getAfter("Second"));
        
        // All of these should fail!
        try { set.moveBefore(null, "Eleventeenth"); } catch(NoSuchElementException ex) { System.out.println("*** "+ex.getMessage()); }
        try { set.moveBefore("Eleventeenth", null); } catch(NoSuchElementException ex) { System.out.println("*** "+ex.getMessage()); }
        try { set.moveAfter(null, "Eleventeenth"); } catch(NoSuchElementException ex) { System.out.println("*** "+ex.getMessage()); }
        try { set.moveAfter("Eleventeenth", null); } catch(NoSuchElementException ex) { System.out.println("*** "+ex.getMessage()); }
        try { set.getBefore("Eleventeenth"); } catch(NoSuchElementException ex) { System.out.println("*** "+ex.getMessage()); }
        try { set.getAfter("Eleventeenth"); } catch(NoSuchElementException ex) { System.out.println("*** "+ex.getMessage()); }
    }

    public static void main(String[] args)
    {
        Test mainprog = new Test();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
            SoftLog.err.println();
            mainprog.showHelp(true);
            SoftLog.err.println();
            SoftLog.err.println("*** Error parsing arguments: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("Test.help");
            if(is == null) SoftLog.err.println("\n*** Unable to locate help information in 'Test.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        SoftLog.err.println("driftwood.gnutil.Test");
        SoftLog.err.println("Copyright (C) 2003 by Ian W. Davis. All rights reserved.");
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

