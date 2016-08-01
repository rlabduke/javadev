// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.util.*;
import java.io.*;
import java.text.ParseException;
//}}}
/**
* This is a simple utility class to take a SheetBuilder output list and 
* output only those lines that meet the given sheet geometry specifications.
*/
public class RightSheetParams //extends ... implements ...
{
//{{{ Constants
//##############################################################################
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose      = false;
    String filename      = null;
    String res           = null;
    String notRes        = null;
    
    double frayMin = -999, frayMin2 = -999, frayMax = 999, frayMax2 = 999;
    double tiltMin = -999, tiltMin2 = -999, tiltMax = 999, tiltMax2 = 999;
    
    int    minNumBetaRes  = 999;
    String ends           = null;
    String concaveVex     = null; // across-strand
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RightSheetParams()
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
        
        // "../../Hoptimize/1a12AH.pdb:A   91 PHE:PHE:A   79 THR:THR:3:1:1:4:99.89886097229135:-3.906033881059705:-14.553165468362735:127.58280006790784:-146.04199941733032:135.01802134029677:130.8702282467995:6.499338493805773:129.1597844305044:::::"
        
        Scanner s = new Scanner(line).useDelimiter(":");
        s.next(); // filename
        s.next(); // arom res
        s.next(); // arom res type
        s.next(); // opp res
        
        String thisRestype = s.next();
        if (res != null)
        {
            if (verbose) System.out.println("thisRestype: "+thisRestype);
            if (!thisRestype.equals(res))     violatesCriterion = true;
        }
        if (notRes != null)
        {
            if (verbose) System.out.println("thisRestype: "+thisRestype);
            if (thisRestype.equals(notRes))   violatesCriterion = true;
        }
        
        int[] nums = new int[4];
        nums[0] = Integer.parseInt(s.next()); // aromNumBetaResN
        nums[1] = Integer.parseInt(s.next()); // aromNumBetaResC
        nums[2] = Integer.parseInt(s.next()); // oppNumBetaResN
        nums[3] = Integer.parseInt(s.next()); // oppNumBetaResC
        if (minNumBetaRes != 999)
        {
            // A single min # beta residues on all sides of both residues
            if (nums[0] < minNumBetaRes || nums[1] < minNumBetaRes || 
                nums[2] < minNumBetaRes || nums[3] < minNumBetaRes)
                violatesCriterion = true;
        }
        if (ends != null)
        {
            if (ends.equals("aromn")) // aromatic strand longer N-ward
                if (nums[0] == 0 || nums[3] > 0)    violatesCriterion = true;
            if (ends.equals("oppn")) // opposite strand longer N-ward
                if (nums[2] == 0 || nums[1] > 0)    violatesCriterion = true;
            if (ends.equals("aromc")) // aromatic strand longer C-ward
                if (nums[1] == 0 || nums[2] > 0)    violatesCriterion = true;
            if (ends.equals("oppc")) // opposite strand longer C-ward
                if (nums[3] == 0 || nums[0] > 0)    violatesCriterion = true;
        }
        
        if (verbose) System.err.println("Fray limits: ("
            +frayMin+","+frayMax+")U("+frayMin2+","+frayMax2+")");
        for (int i = 0; i < 7; i++) s.next(); // skip to fray
        double thisFray = Double.parseDouble(s.next());
        if (frayMin != -999 && frayMax != 999)
        {
            if (frayMin2 != -999 && frayMax2 != 999) // two ranges given
            {
                if ( !(thisFray > frayMin && thisFray < frayMax) && 
                     !(thisFray > frayMin2 && thisFray < frayMax2) )
                    violatesCriterion = true;
            }
            else // only one range given
            {
                if (!(thisFray > frayMin && thisFray < frayMax))
                    violatesCriterion = true;
            }
        }
        
        double thisTilt = Double.parseDouble(s.next());
        if (tiltMin != -999 && tiltMax != 999)
        {
            if (tiltMin2 != -999 && tiltMax2 != 999) // two ranges given
            {
                if ( !(thisTilt > tiltMin && thisTilt < tiltMax) &&
                     !(thisTilt > tiltMin2 && thisTilt < tiltMax2) )
                    violatesCriterion = true;
            }
            else // only one range given
            {
                if (!(thisTilt > tiltMin && thisTilt < tiltMax))
                    violatesCriterion = true;
            }
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
        RightSheetParams mainprog = new RightSheetParams();
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
            String help = "RightSheetParams Help\n\n"
                +"   -[not]res=AAA\n   -fray=#,#[,#,#]\n   -tilt=#,#[,#,#]\n   [-concave,convex]\n";
            System.out.println(help);
        }
        System.err.println("chiropraxis.mc.RightSheetParams");
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
        else if(flag.equals("-notres"))
        {
            notRes = param;
        }
        else if(flag.equals("-fray"))
        {
            Scanner s = new Scanner(param).useDelimiter(",");
            frayMin = Integer.parseInt(s.next());
            frayMax = Integer.parseInt(s.next());
            if (s.hasNext())
            {
                frayMin2 = Integer.parseInt(s.next());
                frayMax2 = Integer.parseInt(s.next());
            }
        }
        else if(flag.equals("-tilt"))
        {
            Scanner s = new Scanner(param).useDelimiter(",");
            tiltMin = Integer.parseInt(s.next());
            tiltMax = Integer.parseInt(s.next());
            if (s.hasNext())
            {
                tiltMin2 = Integer.parseInt(s.next());
                tiltMax2 = Integer.parseInt(s.next());
            }
        }
        else if(flag.equals("-minnumbetares"))
        {
            minNumBetaRes = Integer.parseInt(param);
        }
        else if(flag.equals("-ends"))
        {
            ends = param.toLowerCase();
        }
        else if(flag.equals("-concave"))
        {
            concaveVex = "concave";
        }
        else if(flag.equals("-convex"))
        {
            concaveVex = "convex";
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class