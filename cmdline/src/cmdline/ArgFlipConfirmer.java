// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package/Imports
//##################################################################################################
package cmdline;

import java.io.*;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import java.text.DecimalFormat;
//}}}

/**
* <code>ArgFlipConfirmer</code> takes in two PDB files.  The output is the angle
* between the original guanidinium group normal vector and that for the 
* refit Arg.  This will be filtered later on to take only ~180 degree flips,
* a special class of Arg refits.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
*/
public class ArgFlipConfirmer
{ 
//{{{ Variable Definitions
//##################################################################################################
    TreeSet<String> quasiCNITs;
    ArrayList<String> cnitsAndAngles;
    File pdb_orig = null;
    File pdb_after = null;;
    CoordinateFile cf_orig;
    CoordinateFile cf_after;
    boolean verbose = false;
//}}}

//{{{ main
//##################################################################################################
	public static void main(String[] args)
	{
		ArgFlipConfirmer afc = new ArgFlipConfirmer(args);
		afc.readInput();
        afc.printOutput();
	}
//}}}

//{{{ Constructor
//##################################################################################################
	public ArgFlipConfirmer(String[] args)
	{
		try
        {
            quasiCNITs = new TreeSet<String>();
            cnitsAndAngles = new ArrayList<String>();
            
            for (String arg : args)
            {
                if (arg.equals("-v"))
                    verbose = true;
                else if (pdb_orig == null)
                    pdb_orig  = new File(args[0]);   // e.g. 1amuH.pdb
                else if (pdb_after == null)
                    pdb_after = new File(args[1]);   // e.g. 1amuH_mod.pdb or tmp1.pdb
            }
            
            PdbReader pdbr_orig  = new PdbReader();
            cf_orig              = pdbr_orig.read(pdb_orig);
            
            PdbReader pdbr_after = new PdbReader();
            cf_after             = pdbr_after.read(pdb_after);
        }
        catch (IOException ioe)
        {
            System.out.println("Couldn't open one or more input files");
        }
	}
//}}}

//{{{ readInput
//##################################################################################################
	private void readInput()
	{
		// Get list of all Args, which we'll get angles for in next step
        getArgSet();
        
        // For testing....
        if (verbose)
        {
            Iterator iter2 = quasiCNITs.iterator();
            while (iter2.hasNext())
            {
                String quasiCNIT = (String) iter2.next();
                System.out.println(quasiCNIT);
            }
        }
        
        // Get orig-after guan angle for each Arg in list compiled above
        final DecimalFormat df = new DecimalFormat("###.#");
        Iterator iter = quasiCNITs.iterator();
        while (iter.hasNext())
        {
            String quasiCNIT = (String) iter.next();
            
            // Find each Arg's pre- and post-refit guanidinium normal vectors
            Triple preNormal  = getNormal(quasiCNIT, false);
            Triple postNormal = getNormal(quasiCNIT, true);
            
            // Made default change in guanidinium normal 0 degrees (July 8 2009 - DAK).
            // The problem is that disordered (or incomplete for whatever other reasons)
            // sidechains prevent calculation of a guanidinium normal.  To Jeff and I, 
            // the best strategy at this point is to simply report 0 for such cases so
            // that downstream applications ignore these Args as "not changing".  Not a
            // particularly elegant solution, but hey, neither is this class in general!
            double angle = 0;
            if(preNormal != null && postNormal != null)
                angle = preNormal.angle(postNormal);// / (2*Math.PI) * 360;
            
            // Add angle to output ArrayList 
            cnitsAndAngles.add(quasiCNIT+":"+df.format(angle));
        }
	}
//}}}

//{{{ getArgSet
//##################################################################################################
	private void getArgSet()
	{
        for (Iterator iterModels = (cf_orig.getModels()).iterator(); iterModels.hasNext(); )
        {
            Model mod = (Model) iterModels.next();
            ModelState state = mod.getState();
            for (Iterator iterResidues = (mod.getResidues()).iterator(); iterResidues.hasNext(); )
            {
                Residue res = (Residue) iterResidues.next();
                String thisResno = res.getSequenceNumber();
                String thisResType = res.getName();
                String thisChain = res.getChain();
                if (thisResType.equals("ARG"))
                {
                    String quasiCNIT = thisChain + thisResno + " ARG";
                    // format: "A    3 ARG"
                    //         "chain resno insertion-code restype
                    quasiCNITs.add(quasiCNIT);
                }
            }
        }
    }
//}}}

//{{{ getNormal
//##################################################################################################
	private Triple getNormal(String quasiCNIT, boolean afterRefit)
	{
        CoordinateFile cf = cf_orig;
        if (afterRefit)
            cf = cf_after;
        
        String chain = quasiCNIT.substring(0,1);
        String resno = (quasiCNIT.substring(1,5)).trim();
        String restype = "ARG";
        
        Triple cz  = null;
        Triple nh1 = null;
        Triple nh2 = null;
        for (Iterator iterModels = (cf.getModels()).iterator(); iterModels.hasNext(); )
        {
            Model mod = (Model) iterModels.next();
            
            //System.out.println("mod: "+mod.getName());
            
            ModelState state = mod.getState();
            for (Iterator iterResidues = (mod.getResidues()).iterator(); iterResidues.hasNext(); )
            {
                Residue res = (Residue) iterResidues.next();
                
                //System.out.println("res: '"+res.getName()+"' '"+res.getSequenceNumber()+"'");
                //System.out.println("res chain: '"+res.getChain()+"'");
                
                String thisResno = (res.getSequenceNumber()).trim();
                String thisResType = res.getName();
                String thisChain = res.getChain();
                if (resno.equals(thisResno) && restype.equals(thisResType)&& chain.equals(thisChain))
                {
                    
                    //System.out.println("found an ARG...");
                    
                    // Look thru this residue's atoms for CZ, NH1, and NH2
                    for (Iterator iterAtoms = (res.getAtoms()).iterator(); iterAtoms.hasNext(); )
                    {
                        Atom atom = (Atom) iterAtoms.next();
                        try
                        {
                            String thisAtomName = atom.getName();
                            
                            if(     thisAtomName.equals(" CZ "))  cz  = (Triple) state.get(atom);
                            else if(thisAtomName.equals(" NH1"))  nh1 = (Triple) state.get(atom);
                            else if(thisAtomName.equals(" NH2"))  nh2 = (Triple) state.get(atom);
                            
                        }
                        catch (AtomException ae)
                        { System.out.println("Couldn't extract CZ/NH1/NH2 coords"); }
                        
                        if (cz != null && nh1 != null && nh2 != null)
                        {
                            // Found all three Triples... Now to calculate normal
                            Triple t = new Triple();
                            return t.likeNormal(cz, nh1, nh2);
                        }
                    }
                    if(verbose && (cz == null || nh1 == null || nh2 == null))
                        System.err.println("*** Warning: At least 1/3 atoms null in "+quasiCNIT+"!");
                }
            }
        }
        
        return null;
    }
//}}}

//{{{ printOutput
//##################################################################################################
	private void printOutput()
	{
		for (String line : cnitsAndAngles)
            System.out.println(line);
	}
//}}}

} //class