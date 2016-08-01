// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.phenix;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
import driftwood.util.Strings;
//}}}
/**
* <code>PepLibSuper</code> quick and dirty tool for superimposing peptide libraries for T. Terwilliger.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Sep 17 15:30:34 PDT 2006
*/
public class PepLibSuper //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PepLibSuper()
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
    public void Main() throws IOException, NumberFormatException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
        DecimalFormat df = new DecimalFormat("0.000");
        ArrayList pdbLines = new ArrayList(), pdbTriples = new ArrayList();
        String s;
        Transform R = new Transform();
        Builder builder = new Builder();
        Triple a1 = null, a2 = null, a3 = null;
        while((s = in.readLine()) != null)
        {
            if(s.startsWith("ATOM      1") && a1 != null && a2 != null && a3 != null)
            {
                //R = builder.dock3on3(new Triple(0,0,0), new Triple(1.5,0,0), new Triple(0,-1.5,0), a1, a2, a3);
                R = builder.dock3on3(new Triple(0,0,0), new Triple(1.5,0,0), new Triple(1.5,1.5,0), a1, a2, a3);
                for(int i = 0; i < pdbLines.size(); i++)
                {
                    Triple x = (Triple) pdbTriples.get(i);
                    R.transform(x);
                }
                //Triple n = (Triple) pdbTriples.get(0);
                //Triple c = (Triple) pdbTriples.get(2);
                //if((Math.abs(n.getZ()) > 0.001
                //||  Math.abs(c.getY()) > 0.001
                //||  Math.abs(c.getZ()) > 0.001
                //))
                if(pdbLines.size() <= 20) // more than this implies too many alt confs!
                {
                    out.println("MODEL");
                    for(int i = 0; i < pdbLines.size(); i++)
                    {
                        Triple x = (Triple) pdbTriples.get(i);
                        out.print(pdbLines.get(i).toString().substring(0,30));
                        out.print(Strings.justifyRight(df.format(x.getX()), 8));
                        out.print(Strings.justifyRight(df.format(x.getY()), 8));
                        out.print(Strings.justifyRight(df.format(x.getZ()), 8));
                        out.print(pdbLines.get(i).toString().substring(54));
                        out.println();
                    }
                    //out.println("TER");
                    out.println("ENDMDL");
                }
                pdbLines.clear();
                pdbTriples.clear();
                a1 = a2 = a3 = null;
            }
            
            pdbLines.add(s);
            Triple t = new Triple(Double.parseDouble(s.substring(30,38).trim()),
                Double.parseDouble(s.substring(38,46).trim()),
                Double.parseDouble(s.substring(46,54).trim()));
            pdbTriples.add(t);
            
            if(     s.startsWith("ATOM      2"))
                a1 = t;
            else if(s.startsWith("ATOM      3"))
                a2 = t;
            //else if(s.startsWith("ATOM      1"))
            else if(s.startsWith("ATOM      4"))
                a3 = t;
        }
    }

    public static void main(String[] args)
    {
        PepLibSuper mainprog = new PepLibSuper();
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
            InputStream is = getClass().getResourceAsStream("PepLibSuper.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'PepLibSuper.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.phenix.PepLibSuper");
        System.err.println("Copyright (C) 2006 by Ian W. Davis. All rights reserved.");
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

