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
//import driftwood.*;
//}}}
/**
* <code>NdftToPgm</code> is a simple program that takes a NDFT file
* on stdin and writes a (plain) Portable Gray Map (pgm) file on stdout.
* The values in the table are placed on a log scale before being rendered.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Dec 16 08:32:14 EST 2003
*/
public class NdftToPgm //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public NdftToPgm()
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
        DataInputStream dis;
        dis = new DataInputStream(new BufferedInputStream(System.in));
        NDFloatTable ndft = new NDFloatTable(dis);
        
        if(ndft.getDimensions() != 2)
            throw new IllegalArgumentException("Must provide a 2-D NDFT");
        
        int[] size = ndft.getBins();
        int width = size[0], height = size[1];
        
        final int       MAXPIX  = (1<<16) - 1;
        final double    MAXBIN  = Math.log(ndft.maxValue() + 1.0);
        
        System.out.println("P5");
        System.out.println(width+" "+height);
        System.out.println(MAXPIX);
        
        DataOutputStream out = new DataOutputStream(System.out);
        int[] i = new int[2];
        for(i[1] = height-1; i[1] >= 0; i[1]--)
        {
            for(i[0] = 0; i[0] < width; i[0]++)
            {
                // val should be on [0, 1]
                double  val = Math.log(ndft.valueAt(i) + 1.0) / MAXBIN;
                int     pix = (int)Math.round(MAXPIX * val);
                // check for round up/down errors
                if(pix > MAXPIX)    pix = MAXPIX;
                else if(pix < 0)    pix = 0;
                //System.out.println(pix);
                out.writeChar(pix);
            }
        }
        out.flush();
    }

    public static void main(String[] args)
    {
        NdftToPgm mainprog = new NdftToPgm();
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
            InputStream is = getClass().getResourceAsStream("NdftToPgm.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'NdftToPgm.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.rotarama.NdftToPgm");
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

