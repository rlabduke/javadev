// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package silk.util;
import silk.*;

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
* <code>KarplusTableToNDFT</code> loads the ASCII format tables
* provided by P. A. Karplus in his "Hidden Strain" paper into NDFT files.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jan  9 15:24:38 EST 2004
*/
public class KarplusTableToNDFT //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /**
    * Symbols map to angle values.
    * Where there are few or no observations, we take the null hypothesis:
    * the average value from Engh & Huber.
    */
    String      symbols     = ".x-=0123456789ABCDE";
    double[]    genAngles   = { 111.2, 111.2, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119 };
    double[]    glyAngles   = { 112.5, 112.5, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119 };
    double[]    proAngles   = { 111.8, 111.8, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119 };
    double[]    angles      = genAngles;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KarplusTableToNDFT()
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
        NDimTable ndt = new NDimTable(
            "Tau as a function of phi, psi; after P.A. Karplus (1996)",
            2,
            new double[]    {-180,  -180},
            new double[]    {180,   180},
            new int[]       {36,    36},
            new boolean[]   {true,  true}
        );
        
        int[] i = {0, 0};
        LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
        for(i[1] = 35; i[1] >= 0; i[1]--)
        {
            String s = in.readLine();
            for(i[0] = 0; i[0] < 36; i[0]++)
            {
                char symbol = s.charAt(i[0]);
                double angle = angles[symbols.indexOf(symbol)];
                ndt.setValueAt(i, angle);
            }
        }
        DataOutputStream out = new DataOutputStream(System.out);
        ndt.writeNDFT(out);
        out.flush();
    }

    public static void main(String[] args)
    {
        KarplusTableToNDFT mainprog = new KarplusTableToNDFT();
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
            InputStream is = getClass().getResourceAsStream("KarplusTableToNDFT.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'KarplusTableToNDFT.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("silk.util.KarplusTableToNDFT");
        System.err.println("Copyright (C) 2004 by Ian W. Davis. All rights reserved.");
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
        else if(flag.equals("-gly"))
            angles = glyAngles;
        else if(flag.equals("-pro"))
            angles = proAngles;
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

