// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>NeighborGreeter</code> finds each residue's neighbor in each direction.
* It uses driftwood.moldb2 plus N(i)--C(i-1) and C(i)--N(i+1) distance checks.
* 
* <p>Begun on Wed Feb 10 2010
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
*/
public class NeighborGreeter //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String   filename  = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public NeighborGreeter()
    {
        super();
    }
//}}}

//{{{ peptideBond
//##############################################################################
    /**
    * Assuming resN and resC exist (i.e. are not null), returns true if they 
    * are connected by a normal covalent peptide bond (i.e. C-N < 1.4A).
    */
    public boolean peptideBond(Residue resN, Residue resC, Model model)
    {
        if(resN == null || resC == null) return false;
        
        try
        {
            ModelState state = model.getState();
            
            AtomState nwardC = state.get(resN.getAtom(" C  ")); // 1st in seq
            AtomState cwardN = state.get(resC.getAtom(" N  ")); // 2nd in seq
            double dist = nwardC.distance(cwardN);
            
            if(dist < 1.4) return true;
        }
        catch(AtomException ex)
        { System.err.println("Can't complete distance check(s) for "+resN+" to "+resC+"!"); }
        
        return false; // if can't find the atoms, best to assume chain break or something
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        CoordinateFile coords = new PdbReader().read(new File(filename));
        Model model = coords.getFirstModel();
        
        System.out.println("file,chain,resnum,inscode,restype"
            +",prev_chain,prev_resnum,prev_inscode,prev_restype"
            +",next_chain,next_resnum,next_inscode,next_restype");
        
        ArrayList<Residue> residues = new ArrayList<Residue>();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            if(r != null) residues.add(r);
        }
        Collections.sort(residues);
        for(int i = 0; i < residues.size(); i++)
        {
            Residue resCurr = residues.get(i);
            Residue resPrev = resCurr.getPrev(model);
            Residue resNext = resCurr.getNext(model);
            
            if(!AminoAcid.isAminoAcid(resCurr.getName())) continue;
            
            // Mark residues as null if they aren't amino acids with normal covalent connections
            if(resPrev != null)
                if(!AminoAcid.isAminoAcid(resPrev.getName()) || !peptideBond(resPrev, resCurr, model))
                    resPrev = null;
            if(resNext != null)
                if(!AminoAcid.isAminoAcid(resNext.getName()) || !peptideBond(resCurr, resNext, model))
                    resNext = null;
            
            String rc = (resCurr == null ? "NULL,NULL,NULL,NULL" : 
                resCurr.getChain().trim()+","+resCurr.getSequenceNumber().trim()+","+
                resCurr.getInsertionCode().trim()+","+resCurr.getName()); 
            String rp = (resPrev == null ? "NULL,NULL,NULL,NULL" : 
                resPrev.getChain().trim()+","+resPrev.getSequenceNumber().trim()+","+
                resPrev.getInsertionCode().trim()+","+resPrev.getName());
            String rn = (resNext == null ? "NULL,NULL,NULL,NULL" : 
                resNext.getChain().trim()+","+resNext.getSequenceNumber().trim()+","+
                resNext.getInsertionCode().trim()+","+resNext.getName());
            
            System.out.println(filename+","+rc+","+rp+","+rn);
        }
    }
    
    public static void main(String[] args)
    {
        NeighborGreeter mainprog = new NeighborGreeter();
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
            InputStream is = getClass().getResourceAsStream("NeighborGreeter.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'NeighborGreeter.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.NeighborGreeter");
        System.err.println("Copyright (C) 2010 by Daniel Keedy. All rights reserved.");
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
        if(filename == null) filename = arg;
        else System.err.println("Didn't need "+arg+"; already have file "+filename);
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
