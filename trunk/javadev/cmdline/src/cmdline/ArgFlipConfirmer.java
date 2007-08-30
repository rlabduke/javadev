// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package/Imports
//##################################################################################################
package cmdline;

import java.io.*;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}

public class ArgFlipConfirmer
{ 
//{{{ Variable Definitions
//##################################################################################################
    ArrayList<String> flippedLines;
    File junk_args;
    CoordinateFile cf_orig;
    CoordinateFile cf_after;
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
            flippedLines = new ArrayList<String>();
            junk_args = new File(args[0]);
            File pdb_orig  = new File(args[1]);   // e.g. 1amuH.pdb
            File pdb_after = new File(args[2]);   // tmp2.pdb
            
            PdbReader pdbr_orig = new PdbReader();
            cf_orig = pdbr_orig.read(pdb_orig);
            PdbReader pdbr_after = new PdbReader();
            cf_after = pdbr_after.read(pdb_after);
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
		try
        {
            Scanner ls = new Scanner(junk_args);
            while (ls.hasNextLine())
            {
                String argLine = ls.nextLine();
                
                // Find each Arg's pre- and post-refit guanidinium normal vectors
                Triple preNormal  = getNormal(argLine, false);
                Triple postNormal = getNormal(argLine, true);
                
                //System.out.println("preNormal: "+preNormal.getX()+", "+
                //                    preNormal.getY()+", "+preNormal.getZ());
                //System.out.println("postNormal: "+postNormal.getX()+", "+
                //                    postNormal.getY()+", "+postNormal.getZ());
                
                // Currently, flip defined as 180 +/- 30 degrees ... can change later
                double angle = preNormal.angle(postNormal);// / (2*Math.PI) * 360;
                
                //System.out.println("angle: "+angle);
                
                if ( (angle > 150 && angle < 210) || (angle > -210 && angle < -150) )
                    flippedLines.add(argLine);
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.out.println("Couldn't scan through junk_args");
        }
	}
//}}}

//{{{ getNormal
//##################################################################################################
	private Triple getNormal(String argLine, boolean afterRefit)
	{
        CoordinateFile cf = cf_orig;
        if (afterRefit)
            cf = cf_after;
        
        Scanner s = new Scanner(argLine);
        String chain   = s.next();
        String resno   = s.next();
        String restype = "ARG";
        
        //System.out.println("chain: '"+chain+"'");
        //System.out.println("resno: '"+resno+"'");
        //System.out.println("restype: '"+restype+"'");
        
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
                
                String thisResno = (res.getSequenceNumber()).trim();
                String thisResType = (res.getName()).trim();
                if (resno.equals(thisResno) && restype.equals(thisResType))
                {
                    
                    //System.out.println("found an ARG...");
                    
                    // Look thru this residue's atoms for CZ, NH1, and NH2
                    for (Iterator iterAtoms = (res.getAtoms()).iterator(); iterAtoms.hasNext(); )
                    {
                        Atom atom = (Atom) iterAtoms.next();
                        try
                        {
                            String thisAtomName = atom.getName();
                            
                            //System.out.println("atom: '"+thisAtomName+"'");
                            
                            if (thisAtomName.equals(" CZ ") )
                            {
                                //System.out.println("Found CZ...");
                                cz = (Triple) state.get(atom);
                                //System.out.println("cz.getX() == "+cz.getX());
                            }
                            else if (thisAtomName.equals(" NH1"))
                            {
                                //System.out.println("Found NH1...");
                                nh1 = (Triple) state.get(atom);
                                //System.out.println("nh1.getX() == "+nh1.getX());
                            }
                            else if (thisAtomName.equals(" NH2"))
                            {
                                //System.out.println("Found NH2...");
                                nh2 = (Triple) state.get(atom);
                                //System.out.println("nh2.getX() == "+nh2.getX());
                            }
                            
                        }
                        catch (AtomException ae)
                        {
                            System.out.println("Couldn't extract CZ/NH1/NH2 coords");
                        }
                        
                        if (cz != null && nh1 != null && nh2 != null)
                        {
                            // Found all three Triples... Now to calculate normal
                            Triple t = new Triple();
                            
                            //System.out.println("return Triple = "+t.getX()+", "+t.getY()+", "+t.getZ());
                            
                            return t.likeNormal(cz, nh1, nh2);
                        }
                    }
                
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
		for (String line : flippedLines)
            System.out.println(line);
	}
//}}}

} //class