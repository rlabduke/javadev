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
* Also works on each beta aromatic in a SheetBuilder output list in a similar 
* way.
*/
public class SubImposeScripter //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    String javaCmd = "java -Xmx512m -cp ~/javadev/chiropraxis/chiropraxis.jar chiropraxis.mc.SubImpose ";
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose = false;
    String finalKin = null;
    String helixOrSheet = "helix";
    
    /** The master name(s) applied to this group. */
    ArrayList<String> masters = null;
    
    /** The name of the file of Helix-/SheetBuilder text output. */
    String file = null;
    
    /** The first helix/aromatic in the set, onto which all others will be 
    * superimposed. */
    String ref = null;
    
    /** PDB ID (e.g. 1B8AH) and full file path for the first-in-set, reference  
    * helix/aromatic described below. */
    String pdb2, pdb2Path;
    
    /** Atom type used to align residues. */
    String atom = "_CA_";
    
    /** Where *onto* superposed PDBs will be stored. */
    String pdbDir = null;
    
    /** Residue indices relative to the Ncap (e.g. "-1,1,2" is N', N1, N2) or 
    * beta aromatic which will be used for the superimposition. */
    String idxs = "allhelix";
    
    /** Residue indices relative to the Ncap (helices) or the aromatic and its 
    * opposite residue (sheet) for inclusion in the output coordinates. */
    int initIdx  = -5, finalIdx = 5;
    
    /** Number residues in the above ref helix. */
    int refHelixNumRes;
    
    /** Threshold above which a superimposed PDB is not written out (passed on
    * to SubImpose). */
    String rmsdCutoff = null;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SubImposeScripter()
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
        int ncapResnum = Integer.parseInt(ncapSubstring.substring(7,11).trim());
        if (verbose) System.out.println("ncapResnum = "+ncapResnum);
        
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
            
            int resnum1 = ncapResnum + initIdx;   if (resnum1 <= 0) resnum1 = 1;
            int resnum2 = ncapResnum + finalIdx;
            prekinForHelix(true, pdb2Path, resnum1, resnum2);
                
            if (verbose) System.out.println("Setting ref="+ref+" and pdb2="+pdb2);
        }
        else
        {
            String pdb1 = pdb;
            String mobile = cmd;
            
            String pdbLoc = "";
            if (pdbDir != null)   pdbLoc += pdbDir+"/";
            pdbLoc += pdb1+chain+ncapResnum+"_onto_ref.pdb";
            
            System.out.println(javaCmd+mobile+ref+"-pdb=temp");
            System.out.println("grep \" "+chain+" \" temp > "+pdbLoc);
            int resnum1 = ncapResnum + initIdx;   if (resnum1 <= 0) resnum1 = 1;
            int resnum2 = ncapResnum + finalIdx;
            prekinForHelix(false, pdbLoc, resnum1, resnum2);
            
            if (verbose)
            {
                System.out.println("Final command: "+javaCmd+ref+mobile+"-pdb=temp");
                System.out.println("grep \" "+chain+" \" temp > "+pdbLoc);
            }
        }
    }
//}}}

//{{{ writeScriptForSheet
//##############################################################################
    public void writeScriptForSheet(String line)
    {
        // "../../Hoptimize/1a12AH.pdb:A   91 PHE:PHE:A   79 THR:THR:3:1:1:4:99.89886097229135:-3.906033881059705:-14.553165468362735:127.58280006790784:-146.04199941733032:135.01802134029677:130.8702282467995:6.499338493805773:129.1597844305044:::::"
        if (verbose)
        {
            System.out.println("Starting writeScriptForSheet for line: ");
            System.out.println(line);
        }
        
        // Prep
        Scanner s = new Scanner(line).useDelimiter(":");
        // "../../Hoptimize/1a12AH.pdb"
        String pdbPath = s.next();
        // "A   91 PHE"
        String aromRes = s.next();
        String aromChain = aromRes.substring(0,1);
        s.next();
        // "A   79 THR"
        String oppRes  = s.next();
        String oppChain = oppRes.substring(0,1);
        
        // "1a12AH"
        String pdb = "";
        for (Scanner ps = new Scanner(pdbPath).useDelimiter("/"); ps.hasNext(); )
        {
            String token = ps.next();
            int dotPdbIdx = token.indexOf(".pdb");
            if (dotPdbIdx >= 0)
            {
                // "1a12AH.pdb"
                pdb = token.substring(0,dotPdbIdx);
            }
        }
        String aromResnumString = aromRes.substring(1,aromRes.length()-3).trim();
        String oppResnumString  = oppRes.substring(1,oppRes.length()-3).trim();
        int aromResnum = 0; int oppResnum = 0;
        try { aromResnum = Integer.parseInt(aromResnumString); }
        catch (java.lang.NumberFormatException nfe) { // e.g. "84P"
            aromResnum = Integer.parseInt(aromResnumString.substring(0,aromResnumString.length()-1)); }
        try { oppResnum = Integer.parseInt(oppResnumString); }
        catch (java.lang.NumberFormatException nfe) { // e.g. "84P"
            oppResnum = Integer.parseInt(oppResnumString.substring(0,oppResnumString.length()-1)); }
        if (verbose)
        {
            System.out.println("aromResnum = "+aromResnum);
            System.out.println("oppResnum = " +oppResnum);
        }
        
        String[] stretches = getStretches(line, aromResnum+":"+oppResnum);
        
        // Make command
        // "\(chainA\&atom_CA_\&" -- will add res indices to this each time
        String spec = "";
        if (!aromChain.equals("")) spec = "\\(chain"+aromChain+"\\&atom"+atom+"\\&";
        else                       spec =                      "\\(atom"+atom+"\\&";
        // "../../Hoptimize/1a12AH_chA.pdb -super=\(chainA\&atom_CA_\&90-90\),
        //    \(chainA\&atom_CA_\&92-92\),\(chainA\&atom_CA_\&78-80\)"
        String cmd = " -super=";
        for (int i = 0; i < stretches.length; i ++)
        {
            String stretch = stretches[i];
            if (i != 0)  cmd += ",";
            cmd += spec+stretch+"\\)";
        }
        
        cmd += " "+pdbPath+" ";
        
        if (rmsdCutoff != null)   cmd += " -rmsdcutoff="+rmsdCutoff+" ";
        
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
            
            int[] resnums = new int[4]; // arom init,final res; opp init,final res
            resnums[0] = aromResnum + initIdx;   if (resnums[0] <= 0) resnums[0] = 1;
            resnums[1] = aromResnum + finalIdx;
            resnums[2] = oppResnum  + initIdx;   if (resnums[2] <= 0) resnums[2] = 1;
            resnums[3] = oppResnum  + finalIdx;
            
            prekinForSheet(true, pdb2Path, resnums);
                
            if (verbose) System.out.println("Setting ref="+ref+" and pdb2="+pdb2);
        }
        else
        {
            String pdb1 = pdb;
            String mobile = cmd;
            
            String pdbLoc = "";
            if (pdbDir != null)   pdbLoc += pdbDir+"/";
            pdbLoc += pdb1+aromChain+aromResnum+"_onto_ref.pdb";
            
            System.out.println(javaCmd+mobile+ref+"-pdb=temp");
            System.out.println("grep \" "+aromChain+" \" temp > "+pdbLoc);
            
            int[] resnums = new int[4]; // arom init,final res; opp init,final res
            resnums[0] = aromResnum + initIdx;   if (resnums[0] <= 0) resnums[0] = 1;
            resnums[1] = aromResnum + finalIdx;
            resnums[2] = oppResnum  + initIdx;   if (resnums[2] <= 0) resnums[2] = 1;
            resnums[3] = oppResnum  + finalIdx;
            prekinForSheet(false, pdbLoc, resnums);
            
            if (verbose)
            {
                System.out.println("Final command: "+javaCmd+ref+mobile+"-pdb=temp");
                System.out.println("grep \" "+aromChain+" \" temp > "+pdbLoc);
            }
        }
    }
//}}}

//{{{ getStretches
//##############################################################################
    public String[] getStretches(String line, String substring)
    {
        // Want stretches of contiguous residues used for alignment,
        // e.g. "305-305" or "307-322"
        if (verbose) System.out.println("Starting getStretches for line '"+line+
            "' and substring '"+substring+"'...");
        String[] stretches = null;
        
        if (helixOrSheet.equals("helix"))
        {
            String ncapSubstring = substring;
            if (idxs.equals("-1,1,2"))
            {
                // "Ncap A  306 ASP" > "306 " > "306 "
                String resnumString = ncapSubstring.substring(7,11).trim();
                int resnum = Integer.parseInt(resnumString);
                stretches = new String[2];
                stretches[0] = (resnum-1)+"-"+(resnum-1); // e.g. "305-305"
                stretches[1] = (resnum+1)+"-"+(resnum+2); // e.g. "307-308"
            }
            else if (idxs.equals("1,2,3"))
            {
                // "Ncap A  306 ASP" > "306 " > "306 "
                String resnumString = ncapSubstring.substring(7,11).trim();
                int resnum = Integer.parseInt(resnumString);
                stretches = new String[1];
                stretches[0] = (resnum+1)+"-"+(resnum+3); // e.g. "307-309"
            }
            else if (idxs.equals("-1,1,2,3"))
            {
                // "Ncap A  306 ASP" > "306 " > "306 "
                String resnumString = ncapSubstring.substring(7,11).trim();
                int resnum = Integer.parseInt(resnumString);
                stretches = new String[2];
                stretches[0] = (resnum-1)+"-"+(resnum-1); // e.g. "305-305"
                stretches[1] = (resnum+1)+"-"+(resnum+3); // e.g. "307-309"
            }
            else if (idxs.equals("allhelix"))
            {
                // We're limited to the number of residues in the ref helix, so
                // if the number of residues minus one (N cap) in this helix is 
                // greater than or equal to that in the ref helix, this measurement
                // is possible for this particular helix.
                // Otherwise, we can't do it.
                
                // Get number of residues in this helix
                int firstResnum = getFirstResnum(line);
                int lastResnum  = getLastResnum(line);
                int numRes = lastResnum-firstResnum+1; // e.g. 2-res helix: 1-2 has 2 res's, not 1
                if (firstResnum == -999 || lastResnum == 999)
                    System.err.println("Couldn't get first and/or last residue # in '"+line+"'...");
                if (verbose) 
                    System.out.println("Found 1st:"+firstResnum+" & last:"+lastResnum+
                        " res's in this helix");
                
                // Account for possible helix lengths relative to ref helix
                boolean tooLong = false;
                if (ref != null)
                {
                    // if (numRes == refHelixNumRes)  firstResnum, lastResnum already set
                    if (numRes > refHelixNumRes)
                        lastResnum = firstResnum + refHelixNumRes;
                    if (numRes < refHelixNumRes)
                    {
                        System.err.println("Can't use this helix b/c shorter than "
                            +"ref helix ("+numRes+" < "+refHelixNumRes+")!");
                        tooLong = true;
                    }
                }
                
                // Add a single stretch through those resnums, but not including 
                // the N cap, to the array
                if (!tooLong)
                {
                    stretches = new String[1];
                    stretches[0] = (firstResnum+1)+"-"+lastResnum;
                }
            }
        }
        
        else if (helixOrSheet.equals("sheet"))
        {
            // "91:79"
            Scanner s = new Scanner(substring).useDelimiter(":");
            int aromResnum = Integer.parseInt(s.next());
            int oppResnum  = Integer.parseInt(s.next());
            if (verbose) System.out.println("Resnums are "+aromResnum+" & "+oppResnum+
                " for "+substring);
            
            stretches = new String[3];
            if (idxs.equals("5closest"))
            {
                stretches[0] = (aromResnum-1)+"-"+(aromResnum-1); // e.g. "90-90"
                stretches[1] = (aromResnum+1)+"-"+(aromResnum+1); // e.g. "92-92"
                stretches[2] = (oppResnum-1) +"-"+(oppResnum+1);  // e.g. "78-80"
            }
            else if (idxs.equals("3closest"))
            {
                stretches[0] = (aromResnum-1)+"-"+(aromResnum-1); // e.g. "90-90"
                stretches[1] = (aromResnum+1)+"-"+(aromResnum+1); // e.g. "92-92"
                stretches[2] = (oppResnum) +"-"+(oppResnum);  // e.g. "78-80"
            }
        }
        
        if (verbose) System.out.println("stretches: "+stretches);
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

//{{{ prekinFor(Helix,Sheet)
//##############################################################################
    public void prekinForHelix(boolean ref, String pdbPath, int resnum1, int resnum2)
    {
        if (verbose) System.out.println("Starting prekinForHelix...");
        
        // MAY NEED EDITING!
        String cmd = "prekin ";
        if (!ref)   cmd += "-append ";
        cmd += "-scope -range "+resnum1+"-"+resnum2;
        if (ref)    cmd += " -show \"mc(white),sc(cyan),hy(gray)\"";
        else        cmd += " -show \"mc(brown),sc(green),hy(pinktint)\"";
        
        // Masters
        cmd += " -animate "+pdbPath+" - | sed -e 's/@g.*/& master= {all}/g'";
        if (masters != null) 
        {
            for (String master : masters)
                cmd += " | sed -e 's/@g.*/& master= {"+master+"}/g'";
        }
        
        cmd += " >> "+finalKin;
        System.out.println(cmd);
    }

    public void prekinForSheet(boolean ref, String pdbPath, int[] resnums)
    {
        if (verbose) System.out.println("Starting prekinForSheet...");
        
        //String cmd = "prekin ";
        //if (!ref)   cmd += "-append ";
        String cmd = "prekin -append -nogroup ";
        cmd += "-scope ";
        cmd += "-range "+resnums[0]+"-"+resnums[1]+" ";
        cmd += "-range "+resnums[2]+"-"+resnums[3]+" ";
        if (ref)    cmd += " -show \"mc(white),sc(cyan),hy(gray)\"";
        else        cmd += " -show \"mc(brown),sc(green),hy(pinktint)\"";
        
        // Masters
        cmd += " -animate "+pdbPath+" - | sed -e 's/@g.*/& master= {all}/g'";
        if (masters != null)
        {
            cmd += " | sed -e 's/@g.*/& ";
            for (String master : masters)   cmd += "master= {"+master+"} ";
            cmd += "/g'";
        }
        
        cmd += " >> "+finalKin;
        System.out.println(cmd);
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
        
        System.out.println("#!/bin/bash");
        
        // Set up final kin including the reference structure
        if (finalKin == null) finalKin = "final.kin";
        System.out.println("rm -f "+finalKin);
        System.out.println("touch "+finalKin);File f = new File(file);
        
        Scanner s = new Scanner(f);
        if (helixOrSheet.equals("helix"))
            while (s.hasNextLine())     writeScriptForHelix(s.nextLine());
        else
            while (s.hasNextLine())     writeScriptForSheet(s.nextLine());
    }

    public static void main(String[] args)
    {
        SubImposeScripter mainprog = new SubImposeScripter();
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
            InputStream is = getClass().getResourceAsStream("SubImposeScripter.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SubImposeScripter.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.SubImposeScripter");
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
        else if (flag.equals("-helixbuilder") || flag.equals("-ncaps"))
            helixOrSheet = "helix";
        else if (flag.equals("-sheetbuilder") || flag.equals("-betaarom"))
            helixOrSheet = "sheet";
        else if(flag.equals("-idxs") || flag.equals("-indices"))
        {
            idxs = param;
        }
        else if(flag.equals("-range"))
        {
            Scanner s = new Scanner(param).useDelimiter(",");
            int idx1 = 999, idx2 = 999;
            while (s.hasNext())
            {
                String token = s.next();
                int tokenInt = Integer.parseInt(token);
                if (idx1 == 999)        idx1 = tokenInt;
                else if (idx2 == 999)   idx2 = tokenInt;
                else System.err.println("Wrong format: should be -range=#,#");
            }
            if (idx1 != 999 && idx2 != 999)
            {
                initIdx  = idx1;
                finalIdx = idx2;
            }
            else System.err.println("Wrong format: should be -range=#,#");
        }
        else if(flag.equals("-kin"))
        {
            finalKin = param;
        }
        else if(flag.equals("-pdbdir"))
        {
            pdbDir = param;
        }
        else if(flag.equals("-master") || flag.equals("-masters"))
        {
            masters = new ArrayList<String>();
            Scanner s = new Scanner(param).useDelimiter(",");
            while (s.hasNext())     masters.add(s.next());
        }
        else if(flag.equals("-rmsdcutoff"))
        {
            rmsdCutoff = param;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class