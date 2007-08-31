// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>SubImpose</code> is a tool for superimposing proteins based on a
* carefully specified subset of atoms,
* and calculating RMSD on a (possibly different) set of atoms.
* It's also a play on words for "superimpose" and "subset".
*
* <ol>
* <li>If -super is specified, those atoms from structure 1 are used to superimpose it onto the corresponding atoms from structure 2.</li>
* <li>If -kin is specified, a kinemage is written showing the the atom correspondances.</li>
* <li>If -pdb is specified, the new coordinates for structure 1 are written to file.</li>
* <li>If -rms is specified, those atoms from structure 1 and the corresponding ones from structure 2 are used to compute RMSD.</li>
* </ol>
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Aug 30 15:23:09 PDT 2007
*/
public class SubImpose //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ CLASS: SimpleResAligner
//##############################################################################
    static class SimpleResAligner implements Alignment.Scorer
    {
        // High is good, low is bad.
        public int score(Object a, Object b)
        {
            Residue r = (Residue) a;
            Residue s = (Residue) b;
            if(r == null || s == null)
                return -1;  // gap
            else if(r.getName().equals(s.getName()))
                return 2;   // match
            else
                return 0;   // mismatch
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose = false;
    String structIn1 = null, structIn2 = null;
    String kinOut = null, pdbOut = null;
    String superimpose = null;
    Collection rmsd = new ArrayList(); // selection strings to do rmsd over
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SubImpose()
    {
        super();
    }
//}}}

//{{{ getAtomsForSelection
//##############################################################################
    /**
    * Apply the given selection to res1/s1, then find the corresponding atoms in res2/s2.
    * return as a 2xN array of matched AtomStates, no nulls.
    */
    AtomState[][] getAtomsForSelection(Collection res1, ModelState s1, Collection res2, ModelState s2, String selection, Alignment align) throws ParseException
    {
        // Get selected atom states from model 1
        Selection sel = Selection.fromString(selection);
        Collection allStates1 = Model.extractOrderedStatesByName(res1, Collections.singleton(s1));
        sel.init(allStates1);
        Collection selStates1 = new ArrayList();
        for(Iterator iter = allStates1.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if(sel.select(as)) selStates1.add(as);
        }
        
        // Set up residue correspondances
        Map map1to2 = new HashMap();
        for(int i = 0; i < align.a.length; i++)
        {
            if(align.a[i] != null)
                map1to2.put(align.a[i], align.b[i]); // b[i] may be null
        }
        
        // Get corresponding states from model 2
        Collection selStates2 = new ArrayList();
        int matched = 0;
        for(Iterator iter = selStates1.iterator(); iter.hasNext(); )
        {
            AtomState as1 = (AtomState) iter.next();
            AtomState as2 = null;
            Residue r = (Residue) map1to2.get( as1.getResidue() );
            if(r != null)
            {
                Atom a = r.getAtom( as1.getName() );
                if(a != null)
                {
                    try
                    {
                        as2 = s2.get(a);
                        matched++;
                    }
                    catch(AtomException ex) { ex.printStackTrace(); }
                }
            }
            selStates2.add(as2); // as2 could still be null
        }
        
        if(selStates1.size() != selStates2.size()
        || matched > selStates1.size())
            throw new RuntimeException("logical error; sel1="+selStates1.size()+", sel2="+selStates2.size()+", matched="+matched);
            
        // Arrange data into nice arrays for convenience
        AtomState[][] ret = new AtomState[2][matched];
        Iterator iter1 = selStates1.iterator();
        Iterator iter2 = selStates2.iterator();
        int idx = 0;
        while(iter1.hasNext())
        {
            AtomState as1 = (AtomState) iter1.next();
            AtomState as2 = (AtomState) iter2.next();
            if(as2 == null) continue;
            ret[0][idx] = as1;
            ret[1][idx] = as2;
            idx++;
        }
        
        if(idx != matched)
            throw new RuntimeException("logical error; idx="+idx+", matched="+matched);
        
        return ret;
    }
//}}}

//{{{ writeKin
//##############################################################################
    void writeKin(AtomState[][] atoms) throws IOException
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(this.kinOut))));
        out.println("@kinemage");
        out.println("@group {correspondances} dominant");
        out.println("@vectorlist {pairs} color= green");
        for(int i = 0; i < atoms[0].length; i++)
        {
            AtomState a1 = atoms[0][i], a2 = atoms[1][i];
            out.println("{"+a1+"}P "+df.format(a1.getX())+" "+df.format(a1.getY())+" "+df.format(a1.getZ()));
            out.println("{"+a2+"}L "+df.format(a2.getX())+" "+df.format(a2.getY())+" "+df.format(a2.getZ()));
        }
        out.close();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, ParseException
    {
        if(structIn1 == null || structIn2 == null)
            throw new IllegalArgumentException("must provide two structures");
        
        // Read in structures, get arrays of residues.
        PdbReader pdbReader = new PdbReader();
        CoordinateFile s1 = pdbReader.read(new File(structIn1));
        CoordinateFile s2 = pdbReader.read(new File(structIn2));
        Model m1 = s1.getFirstModel();
        Model m2 = s2.getFirstModel();
        
        DecimalFormat df = new DecimalFormat("0.0###");
        System.err.println("rmsd\tn_atoms\tselection");
        
        // Align residues by sequence
        // For now we just take all residues as they appear in the file,
        // without regard to chain IDs, etc.
        Alignment align = Alignment.needlemanWunsch(m1.getResidues().toArray(), m2.getResidues().toArray(), new SimpleResAligner());
        if(verbose)
        {
            System.err.println("Residue alignments:");
            for(int i = 0; i < align.a.length; i++)
                System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            System.err.println();
        }
        
        // If -super, do superimposition of s1 on s2:
        Transform R = new Transform(); // identity, defaults to no superposition
        if(superimpose != null)
        {
            AtomState[][] atoms = getAtomsForSelection(m1.getResidues(), m1.getState(), m2.getResidues(), m2.getState(), superimpose, align);
            if(verbose)
            {
                System.err.println("Atom alignments:");
                for(int i = 0; i < atoms[0].length; i++)
                    System.err.println("  "+atoms[0][i]+" <==> "+atoms[1][i]);
                System.err.println();
            }
            if(atoms[0].length < 3)
                throw new IllegalArgumentException("Can't superimpose on less than 3 atoms!");
            // struct2 is the reference point, struct1 should move.
            SuperPoser superpos = new SuperPoser(atoms[1], atoms[0]);
            R = superpos.superpos();
            System.err.println(df.format(superpos.calcRMSD(R))+"\t"+atoms[0].length+"\t"+superimpose);
            
            // Transform model 1 so transformed coords will be used in the future
            for(Iterator iter = Model.extractOrderedStatesByName(m1).iterator(); iter.hasNext(); )
            {
                AtomState as = (AtomState) iter.next();
                R.transform(as);
            }
            
            // Write kinemage showing atoms for superposition:
            if(kinOut != null) writeKin(atoms);
            
            // Write superimposed PDB file:
            if(pdbOut != null)
            {
                PdbWriter pdbWriter = new PdbWriter(new File(pdbOut));
                pdbWriter.writeCoordinateFile(s1);
                pdbWriter.close();
            }
        }
        else
        {
            if(kinOut != null)
                System.err.println("WARNING: can't use -kin without -super");
            if(pdbOut != null)
                System.err.println("WARNING: can't use -pdb without -super");
        }
        
        // If -rms, do RMSD calculation
        for(Iterator iter = rmsd.iterator(); iter.hasNext(); )
        {
            String rmsd_sel = (String) iter.next();
            AtomState[][] atoms = getAtomsForSelection(m1.getResidues(), m1.getState(), m2.getResidues(), m2.getState(), rmsd_sel, align);
            if(verbose)
            {
                System.err.println("Atom alignments:");
                for(int i = 0; i < atoms[0].length; i++)
                    System.err.println("  "+atoms[0][i]+" <==> "+atoms[1][i]);
                System.err.println();
            }
            // struct2 is the reference point, struct1 should move.
            // (nothing's really moving here so it doesn't matter).
            SuperPoser superpos = new SuperPoser(atoms[1], atoms[0]);
            // Don't recalculate, use our old transform!
            //R = superpos.superpos();
            System.out.println(df.format(superpos.calcRMSD(R))+"\t"+atoms[0].length+"\t"+rmsd_sel);
        }
    }

    public static void main(String[] args)
    {
        SubImpose mainprog = new SubImpose();
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
            InputStream is = getClass().getResourceAsStream("SubImpose.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SubImpose.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.SubImpose");
        System.err.println("Copyright (C) 2007 by Ian W. Davis. All rights reserved.");
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
        if(structIn1 == null)       structIn1 = arg;
        else if(structIn2 == null)  structIn2 = arg;
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
            verbose = true;
        else if(flag.equals("-super"))
            superimpose = param;
        else if(flag.equals("-kin"))
            kinOut = param;
        else if(flag.equals("-pdb"))
            pdbOut = param;
        else if(flag.equals("-rms"))
            rmsd.add(param);
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

