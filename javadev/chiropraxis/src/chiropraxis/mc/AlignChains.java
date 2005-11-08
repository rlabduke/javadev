// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>AlignChains</code> takes several chains in a PDB file and aligns them
* each on top of the first.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 25 09:32:27 EDT 2005
*/
public class AlignChains //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String      fileName    = null;
    String      refChainID  = null;
    Collection  mobChainIDs = new ArrayList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AlignChains()
    {
        super();
    }
//}}}

//{{{ optimizeAlignment, extractCAs
//##############################################################################
    /**
    * Finds the optimal ungapped C-alpha superposition
    * between two sets of contiguous residues, which may or may not
    * be of the same length.
    */
    public Transform optimizeAlignment(Collection refChain, Collection mobChain, ModelState state)
    {
        AtomState[] ref = extractCAs(refChain, state);
        AtomState[] mob = extractCAs(mobChain, state);
        int         len         = Math.min(ref.length, mob.length);
        int         maxoff      = Math.max(ref.length, mob.length) - len;
        Transform   bestR       = null;
        double      bestRMSD    = Double.POSITIVE_INFINITY;
        DecimalFormat df = new DecimalFormat("0.000");
        
        double[] w = new double[len];
        for(int i = 0; i < len; i++) w[i] = 1;
        
        for(int i = 0; i <= maxoff; i++)
        {
            int ref_i = (ref.length == len ? 0 : i);
            int mob_i = (mob.length == len ? 0 : i);
            SuperPoser sp = new SuperPoser(ref, ref_i, mob, mob_i, len);
            Transform R = sp.superpos(w);
            double rmsd = sp.calcRMSD(R, w);
            if(rmsd < bestRMSD)
            {
                bestR       = R;
                bestRMSD    = rmsd;
            }
        }
        System.err.println("Best Ca RMSD: "+df.format(bestRMSD));
        return bestR;
    }
    
    AtomState[] extractCAs(Collection residues, ModelState state)
    {
        Collection cas = new ArrayList();
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            try { cas.add(state.get(r.getAtom(" CA "))); }
            catch(AtomException ex) {}
        }
        return (AtomState[]) cas.toArray(new AtomState[cas.size()]);
    }
//}}}

//{{{ extractOrderedStatesByName
//##############################################################################
    /**
    * Extracts all the uniquely named AtomStates for the given model, in the
    * order of Residues and Atoms given.
    */
    static public Collection extractOrderedStatesByName(Collection residues, Collection modelStates)
    {
        ModelState[]    states      = (ModelState[]) modelStates.toArray(new ModelState[modelStates.size()]);
        Set             usedNames   = new HashSet(); // to avoid duplicates
        ArrayList       atomStates  = new ArrayList();
        
        for(Iterator ri = residues.iterator(); ri.hasNext(); )
        {
            Residue res = (Residue)ri.next();
            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom atom = (Atom)ai.next();
                for(int i = 0; i < states.length; i++)
                {
                    try
                    {
                        AtomState as = states[i].get(atom);
                        // We want to make sure every atom output has a unique PDB name.
                        // We're not worried so much about duplicating coordinates (old code).
                        // Name requirement is important for dealing with alt confs,
                        // where a single atom (' ') may move in A but not B --
                        // this led to two ATOM entries with different coords but the same name.
                        String aName = as.getAtom().toString()+as.getAltConf();
                        //if(!usedNames.contains(as)) -- for comparison by XYZ coords
                        if(!usedNames.contains(aName))
                        {
                            //usedNames.add(as); -- for comparison by XYZ coords
                            usedNames.add(aName);
                            atomStates.add(as);
                        }
                    }
                    catch(AtomException ex) {} // no state
                }
            }//for each atom
        }// for each residue
        return atomStates;
    }
//}}}

//{{{ alignChains
//##############################################################################
    /** Alters coordinates of mobChains to superimpose them on refChain. */
    public void alignChains(CoordinateFile coordFile, String refChain, Collection mobChains)
    {
        for(Iterator mi = coordFile.getModels().iterator(); mi.hasNext(); )
        {
            Model m = (Model) mi.next();
            Collection allAtomStates = extractOrderedStatesByName(m.getResidues(), m.getStates());
            AtomState[] atomStates = (AtomState[]) allAtomStates.toArray(new AtomState[allAtomStates.size()]);
            for(Iterator ci = mobChains.iterator(); ci.hasNext(); )
            {
                String mobChain = (String) ci.next();
                Collection ref = m.getChain(refChain);
                Collection mob = m.getChain(mobChain);
                if(ref == null || mob == null) continue;
                Transform R = optimizeAlignment(ref, mob, m.getState());
                Set mobSet = new CheapSet(mob);
                for(int i = 0; i< atomStates.length; i++)
                    if(mobSet.contains(atomStates[i].getResidue()))
                        R.transform(atomStates[i]);
            }
        }
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
        if(fileName == null)        throw new IllegalArgumentException("No PDB specified");
        if(refChainID == null)      throw new IllegalArgumentException("No reference chain");
        if(mobChainIDs.size() < 1)  throw new IllegalArgumentException("No mobile chain(s)");
        
        CoordinateFile cfile = new PdbReader().read(new File(fileName));
        alignChains(cfile, refChainID, mobChainIDs);
        new PdbWriter(System.out).writeCoordinateFile(cfile, new HashMap());
    }

    public static void main(String[] args)
    {
        AlignChains mainprog = new AlignChains();
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
            InputStream is = getClass().getResourceAsStream("AlignChains.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'AlignChains.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.AlignChains");
        System.err.println("Copyright (C) 2005 by Ian W. Davis. All rights reserved.");
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
        if(fileName == null)        fileName = arg;
        else if(refChainID == null) refChainID = arg;
        else                        mobChainIDs.add(arg);
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

