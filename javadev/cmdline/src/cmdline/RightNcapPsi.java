// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.util.*;
import java.io.*;
import java.text.ParseException;
//}}}
/**
* This is a simple utility class to take a (Dssp)HelixBuilder output list and 
* output only those lines that match the given Ncap amino acid and Hbond types.
*/
public class RightNcapPsi //extends ... implements ...
{
//{{{ Constants
//##############################################################################
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose = false;
    String filename = null;
    String res      = null;
    String hbond    = null;
    int psiMin      = -999;
    int psiMax      = 999;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RightNcapPsi()
    {
        super();
    }
//}}}

//{{{ meetsCriteria
//##############################################################################
    public boolean meetsCriteria(String line)
    {
        if (verbose) System.out.println("Starting meetsCriteria for '"+line+"'");
        boolean violatesCriterion = false;
        
        // "Hop_1a-f/1a2pAH.pdb:helix from A    6 THR to A   17 TYR:Ncap A    6 THR:133.251:104.671:108.465:102.025:113.628:111.539:112.262:-141.896:23.545:-100.605:163.878:-56.031:-49.15:3.557:2.122:5.048:3.821::i+3::1:0:"

        if (res != null)
        {
            String ncapOnwards = line.substring(line.indexOf("Ncap"));
            String thisRestype = ncapOnwards.substring(12,15);
            if (verbose) System.out.println("thisRestype: "+thisRestype);
            if (!thisRestype.equals(res))        violatesCriterion = true;
        }
        
        if (hbond != null)
        {
            // e.g. i3 or i2
            if (hbond.equals("i3"))
                if (line.indexOf("i+3") < 0)    violatesCriterion = true;
            if (hbond.equals("i2"))
                if (line.indexOf("i+2") < 0)    violatesCriterion = true;
            
            if (verbose && line.indexOf("i+3") >= 0)    System.out.println(
                line.substring(line.indexOf("i+3"), line.indexOf("i+3")+3));
            if (verbose && line.indexOf("i+2") >= 0)    System.out.println(
                line.substring(line.indexOf("i+2"), line.indexOf("i+2")+3));
        }
        
        if (psiMin != -999 && psiMax != 999)
        {
            Scanner s = new Scanner(line).useDelimiter(":");
            for (int i = 0; i < 13; i ++)   s.next();
            double thisPsi = Double.parseDouble(s.next());
            if (thisPsi < 0)    thisPsi += 360;
            if (verbose) System.out.println("thisPsi wrapped360: "+thisPsi);
            if (thisPsi < psiMin || thisPsi > psiMax)  violatesCriterion = true;
        }
        
        if (verbose) System.out.println("violatesCriterion: "+violatesCriterion+"\n");
        if (violatesCriterion)   return false;
        return true;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, ParseException
    {
        if (verbose) System.out.println("Starting Main...");
        if (filename == null)
        {
            System.err.println("Need input file!");
            System.exit(0);
        }
        else
        {
            Scanner s = new Scanner(new File(filename));
            while (s.hasNextLine())
            {
                String line = s.nextLine();
                if (meetsCriteria(line))   System.out.println(line);
            }
        }
    }

    public static void main(String[] args)
    {
        RightNcapPsi mainprog = new RightNcapPsi();
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
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(2);
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
            String help = "RightNcapPsi Help\n\n"
                +"   -res=AAA\n   -hb=i3|i2\n   -psi=##-## (0->360, not -180->180\n";
            System.out.println(help);
        }
        System.err.println("chiropraxis.mc.RightNcapPsi");
        System.err.println("Copyright (C) 2007 by Daniel Keedy. All rights reserved.");
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
        if (filename == null)      filename = arg;
        else throw new IllegalArgumentException("too many arguments!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-v"))
        {
            System.out.println("Doing verbose...");
            verbose = true;
        }
        else if(flag.equals("-res"))
        {
            res = param;
        }
        else if(flag.equals("-hb"))
        {
            hbond = param;
        }
        else if(flag.equals("-psi"))
        {
            Scanner s = new Scanner(param).useDelimiter("-");
            psiMin = Integer.parseInt(s.next());
            psiMax = Integer.parseInt(s.next());
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class