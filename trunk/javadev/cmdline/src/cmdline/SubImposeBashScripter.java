// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.util.*;
import java.io.*;
import java.text.ParseException;
import driftwood.util.Strings;
//}}}
/**
* This is a simple utility class to make a Bash script that will run Ian's
* chiropraxis.mc.SubImpose on each helix in a HelixBuilder output list and 
* superimpose it onto the first helix in that list using a designated set of 
* residues.
*/
public class SubImposeBashScripter //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    String javaCmd = "java -Xmx512m -cp ~/javadev/chiropraxis/chiropraxis.jar chiropraxis.mc.SubImpose ";
    PrintStream out = System.out;
    
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The name of the file of HelixBuilder text output. */
    String file = null;
    
    /** Residue indices relative to the Ncap (e.g. "-1,1,2" is N', N1, N2) which 
    * will be used for the superimposition. */
    String ncapIdxs = "allhelix";
    
    /** The first helix in the set, onto which all others will be superimposed. */
    String ref = null;
    
    /** Number residues in the above ref helix. */
    int refHelixNumRes;
    
    /** PDB ID (e.g. 1B8AH) and full file path for the first-in-set, reference  
    * helix described above. */
    String pdb2, pdb2Path;
    
    /** Atom type used to align residues. */
    String atom = "_CA_";
    
    boolean verbose = false;
    String finalKin = null;
    String master = null;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SubImposeBashScripter()
    {
        super();
    }
//}}}

//{{{ writeScriptForHelix
//##############################################################################
    public void writeScriptForHelix(String line)
    {
        if (verbose)
        {
            System.out.println("Starting writeScriptForHelix for line: ");
            System.out.println(line);
        }
        
        // Prep
        // "pdbs/Hoptimize/1B8AH.pdb"
        String pdbPath = line.substring(0,line.indexOf(":"));
        // "Ncap A  306 ASP:..."
        String ncapOnwards = line.substring(line.indexOf("Ncap"));
        // "Ncap A  306 ASP"
        String ncapSubstring = ncapOnwards.substring(0,ncapOnwards.indexOf(":"));
        // "A"
        String chain = ncapSubstring.substring(5,6).trim();
        String[] stretches = getStretches(line, ncapSubstring);
        // "1B8AH"
        String pdb = "";
        for (Scanner s = new Scanner(pdbPath).useDelimiter("/"); s.hasNext(); )
        {
            String token = s.next();
            int dotPdbIdx = token.indexOf(".pdb");
            if (dotPdbIdx >= 0)
            {
                // "1B8AH.pdb"
                pdb = token.substring(0,dotPdbIdx);
            }
        }
        
        // Make command
        // "\(chainA\&atom_CA_\&" -- will add res indices to this each time
        String spec = "";
        if (!chain.equals("")) spec = "\\(chain"+chain+"\\&atom"+atom+"\\&";
        else                   spec =                  "\\(atom"+atom+"\\&";
        // "pdbs/Hoptimize/1B8AH_chA.pdb -super=\(chainA\&atom_CA_\&305-305\),
        //    \(chainA\&atom_CA_\&307-308\)"
        String cmd = " -super=";
        for (int i = 0; i < stretches.length; i ++)
        {
            String stretch = stretches[i];
            if (i != 0)  cmd += ",";
            cmd += spec+stretch+"\\)";
        }
        cmd += " "+pdbPath+" ";
        
        // Use command
        if (ref == null)
        {
            // Make this the reference PDB since it's first.
            // Use -super2 b/c in SubImpose, atoms in structure 1 are superimposed
            // on atoms in structure 2, so -super2 indicates the reference coordinates.
            String beforeSuper = cmd.substring(0,cmd.indexOf("=\\("));
            String afterSuper  = cmd.substring(cmd.indexOf("=\\("));
            ref = beforeSuper+"2"+afterSuper;
            pdb2 = pdb;
            pdb2Path = pdbPath;
            refHelixNumRes = getLastResnum(line) - getFirstResnum(line);
            if (verbose) System.out.println("Setting ref="+ref+" and pdb2="+pdb2);
        }
        else
        {
            String pdb1 = pdb;
            String mobile = cmd;
            out.println(javaCmd+mobile+ref+"-pdb=temp");
            out.println("grep \" "+chain+" \" temp > "+pdb1+"onto"+pdb2+".pdb");
            if (verbose)
            {
                System.out.println("Final command: "+javaCmd+ref+mobile+"-pdb=temp");
                System.out.println("               grep \" "+chain+" \" temp > "+
                    pdb1+"onto"+pdb2+".pdb");
            }
        }
    }
//}}}

//{{{ getStretches
//##############################################################################
    public String[] getStretches(String line, String ncapSubstring)
    {
        // Want stretches of contiguous residues used for alignment,
        // e.g. "305-305" or "307-322"
        
        if (verbose) System.out.println("Starting getStretches for line '"+line+
            "' and ncapSubstring '"+ncapSubstring+"'...");
        
        String[] stretches = null;
        if (ncapIdxs.equals("-1,1,2"))
        {
            // "Ncap A  306 ASP" > "306 " > "306 "
            String resnumString = ncapSubstring.substring(7,11).trim();
            int resnum = Integer.parseInt(resnumString);
            stretches = new String[2];
            stretches[0] = (resnum-1)+"-"+(resnum-1); // e.g. "305-305"
            stretches[1] = (resnum+1)+"-"+(resnum+2); // e.g. "307-308"
        }
        else if (ncapIdxs.equals("allhelix"))
        {
            // We're limited to the number of residues in the ref helix, so
            // if the number of residues in this helix minus one (N cap) is 
            // greater than or equal to that in the ref helix, this measurement
            // is possible for this particular helix.
            // Otherwise, we can't do it.
            
            // Get number of residues in this helix
            int firstResnum = getFirstResnum(line);
            int lastResnum  = getLastResnum(line);
            int numRes = lastResnum-firstResnum;
            if (firstResnum == -999 || lastResnum == 999)
                System.err.println("Couldn't get first and/or last residue # in '"+line+"'...");
            if (verbose) 
                System.out.println("Found 1st:"+firstResnum+" & last:"+lastResnum+
                    " res's in this helix");
            
            // Make sure this helix is the right length
            if (ref != null)
            {
                // if (refHelixNumRes == numRes)    perfect!
                if (refHelixNumRes < numRes)
                    lastResnum = firstResnum + refHelixNumRes;
                if (refHelixNumRes > numRes)
                {
                    // Can't use this helix!
                    System.err.println("Can't use this helix b/c length:"+numRes
                        +" < "+refHelixNumRes+" in ref helix!");
                    
                    // HOW TO HANDLE UNWANTED HELICES?
                    
                }
            }
            
            // Add a single stretch through those resnums, but not including 
            // the N cap, to the array
            stretches = new String[1];
            stretches[0] = (firstResnum+1)+"-"+lastResnum;
        }
        else if (ncapIdxs.equals("dummy_option"))
        {
            // choose different stretches for alignment...
        }
        return stretches;
    }
//}}}

//{{{ getFirstResnum, getLastResnum
//##############################################################################
    public int getFirstResnum(String line)
    {
        int firstResnum = -999;
        Scanner s = new Scanner(line.substring(line.indexOf("helix")));
        try
        {
            // "helix from A  206 ASP to A  220 GLY:..."
            s.next();   // skip 'helix'
            s.next();   // skip 'from'
            s.next();   // skip chain ID, e.g. 'A'
            firstResnum = Integer.parseInt(s.next());
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("Couldn't get first residue # in '"+line+"'...");
        }
        return firstResnum;
    }

    public int getLastResnum(String line)
    {
        int lastResnum = 999;
        Scanner s = new Scanner(line.substring(line.indexOf("helix")));
        try
        {
            // "helix from A  206 ASP to A  220 GLY:..."
            s.next();   // skip 'helix'
            s.next();   // skip 'from'
            s.next();   // skip chain ID, e.g. 'A'
            s.next();   // skip firstResnum
            s.next();   // skip restype, e.g. 'ASP'
            s.next();   // skip 'to'
            s.next();   // skip second chain ID, e.g. 'A'
            lastResnum = Integer.parseInt(s.next());
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("Couldn't get last residue # in '"+line+"'...");
        }
        return lastResnum;
    }
//}}}

//{{{ writeVizScript
//##############################################################################
    public void writeVizScript()
    {
        if (verbose) System.out.println("Starting writeVizScript...");
        
        // Set up final kin including the reference structure
        if (finalKin == null) finalKin = "final.kin";
        out.println("rm -f "+finalKin);
        out.println("touch "+finalKin);
        out.println("prekin -scope -show \"mc(white),sc(cyan),hy(gray)\" "
            +pdb2Path.substring(0,pdb2Path.length()-4)+".pdb "+finalKin);
        
        // Command to make a kin of each superimposed PDB and append it to the final kin
        out.println("for pdb in *onto*.pdb");
        out.println("do");
        out.print("    prekin -append -scope -show \"mc(brown),sc(green),hy(pinktint)\""
            +" -animate $pdb - | sed -e 's/@g.*/& master= {all}/g'");
        if (master != null)
            out.print(" | sed -e 's/@g.*/& master= {"+master+"}/g'");
        out.println(" >> "+finalKin);
        out.println("done");
        out.println("rm -f *temp*");
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
        if (file == null)
        {
            System.err.println("Need input file!");
            System.exit(0);
        }
        
        out.println("#!/bin/bash");
        File f = new File(file);
        Scanner s = new Scanner(f);
        while (s.hasNextLine())     writeScriptForHelix(s.nextLine());
        
        writeVizScript();
    }

    public static void main(String[] args)
    {
        SubImposeBashScripter mainprog = new SubImposeBashScripter();
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
            InputStream is = getClass().getResourceAsStream("SubImposeBashScriptMaker.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SubImposeBashScriptMaker.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.SubImposeBashScriptMaker");
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
        if (file == null)      file = arg;
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
        else if(flag.equals("-ncapidxs") || flag.equals("-ncapindices"))
        {
            ncapIdxs = param;
        }
        else if(flag.equals("-kin"))
        {
            finalKin = param;
        }
        else if(flag.equals("-master"))
        {
            master = param;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class