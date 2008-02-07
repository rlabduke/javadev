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
import chiropraxis.sc.SidechainAngles2;
//}}}
/**
* <code>SubImpose</code> is a tool for superimposing proteins based on a
* carefully specified subset of atoms,
* and calculating RMSD on a (possibly different) set of atoms.
* It's also a play on words for "superimpose" and "subset".
*
* <ol>
* <li>If -super is specified, those atoms from structure 1 are used to superimpose it onto the corresponding atoms from structure 2.</li>
* <li>If -sieve is specified, those atoms are cut down to the specified fraction (0,1] by Lesk's sieve.</li>
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
    boolean showTransform = false;
    boolean fix180flips = true;
    String structIn1 = null, structIn2 = null;
    String kinOut = null, pdbOut = null;
    String superimpose = null; // describes atoms in structure 1 for superimposing onto structure 2
    String superimpose2 = null; // describes atoms in structure 2 which will match up with those
                                // in structure 1 described in superimpose
                                // added by dak
    Collection rmsd = new ArrayList(); // selection strings to do rmsd over
    double leskSieve = 0;
    double rmsdCutoff = Double.NaN; // above which PDB is not written out
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SubImpose()
    {
        super();
    }
//}}}

//{{{ fix180rotations
//##############################################################################
    /**
    * For symmetric residues in res1 where the symmetric chi angle differs by
    * more than 90 degrees from the corresponding residue in res2,
    * rotate by 180 degrees to remove an rmsd that comes only from atom naming.
    * @return a new version of s1 with flips incorporated
    */
    ModelState fix180rotations(ModelState s1, ModelState s2, Alignment align) throws IOException
    {
        DecimalFormat df = new DecimalFormat("0.0");
        SidechainAngles2 sc = new SidechainAngles2();
        for(int i = 0, len = align.a.length; i < len; i++)
        {
            if(align.a[i] == null || align.b[i] == null) continue;
            Residue r1 = (Residue) align.a[i];
            Residue r2 = (Residue) align.b[i];
            if(!r1.getName().equals(r2.getName())) continue; // sequence mismatch
            
            String whichAngle = null;
            if("PHE".equals(r1.getName()))      whichAngle = "chi2";
            else if("TYR".equals(r1.getName())) whichAngle = "chi2";
            else if("ASP".equals(r1.getName())) whichAngle = "chi2";
            else if("GLU".equals(r1.getName())) whichAngle = "chi3";
            else continue;
            
            // This could potentially also be a problem for Arg "chi5" if you
            // had a nonstandard rotamer library somewhere that messed up the
            // NH1/NH2 naming convention, but for now we won't worry about it.
            // FYI, NH1 is cis to CD, and NH2 is trans to CD:
            // -- CD          NH1
            //      \        /
            //       NE -- CZ
            //               \
            //                NH2
            
            try
            {
                // Calculate both chi angles on [0,360)
                double a1 = sc.measureAngle(whichAngle, r1, s1) % 360;
                if(a1 < 0) a1 += 360;
                double a2 = sc.measureAngle(whichAngle, r2, s2) % 360;
                if(a2 < 0) a2 += 360;
                // Calculate difference on [0,180]
                double diff = Math.abs(a1 - a2);
                if(diff > 180) diff = 360 - diff;
                // If greater than 90, flip r1
                if(diff > 90)
                {
                    s1 = sc.setAngle(whichAngle, r1, s1, (a1 - 180));
                    if(verbose)
                        System.err.println("Flipped "+whichAngle+" for "+r1+"; "+df.format(a1)+" - "+df.format(a2)+" = "+df.format(diff)+"; "+df.format(a1)+" --> "+df.format(sc.measureAngle(whichAngle, r1, s1)));
                }
            }
            catch(AtomException ex)
            { System.err.println("Unable to flip "+whichAngle+" for "+r1+": "+ex.getMessage()); }
        }
        
        return s1.createCollapsed();
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
        int numAs1s = 0; // added by dak
        Selection sel = Selection.fromString(selection);
        Collection allStates1 = Model.extractOrderedStatesByName(res1, Collections.singleton(s1));
        sel.init(allStates1);
        Collection selStates1 = new ArrayList();
        for(Iterator iter = allStates1.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if(sel.select(as))  selStates1.add(as);
        }
        
        // added by dak
        int matched = 0;
        Collection selStates2 = new ArrayList();
        String selection2 = superimpose2; // comes from -super2=[text] flag
        if (selection2 != null)
        {
            // Residue correspondences (sic below) given by flag
            // Get selected atom states from model 2
            Selection sel2 = Selection.fromString(selection2);
            Collection allStates2 = Model.extractOrderedStatesByName(res2, Collections.singleton(s2));
            sel2.init(allStates2);
            for(Iterator iter2 = allStates2.iterator(); iter2.hasNext(); )
            {
                AtomState as2 = (AtomState) iter2.next();
                if(sel2.select(as2))
                {
                    selStates2.add(as2);
                    matched ++; // placeholder so code below (arranging into nice arrays) doesn't break
                }
            }
        }
        else
        {
            // Need to set up residue correspondences (sic below) algorithmically as Ian originally intended
            // Set up residue correspondances
            Map map1to2 = new HashMap();
            for(int i = 0; i < align.a.length; i++)
            {
                if(align.a[i] != null)
                    map1to2.put(align.a[i], align.b[i]); // b[i] may be null
            }
            
            // Get corresponding states from model 2
            //Collection selStates2 = new ArrayList(); // moved outside of for statement by dak
            //int matched = 0; // moved outside of for statement by dak
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
        }
        
        if(selStates1.size() != selStates2.size() || matched > selStates1.size())
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

//{{{ sortByLeskSieve
//##############################################################################
    /**
    * Applies Lesk's "sieve" method for selecting an optimal set
    * of C-alphas to superimpose.
    * A least-squares fit is applied over all points, and the rmsd (=score)
    * and worst-fitting pair are entered as a SortItem at the front of the array.
    * The worst pair is then removed from the set being fit, the fit is repeated,
    * and the new rmsd and worst pair are entered in the second array position.
    * This is repeated until all points have been processed.
    *
    * It appears this was described in
    *   AM Lesk (1991) "Protein Architecture: A Practical Guide", IRL Press, Oxford.
    * It may have also been described in
    *   Lesk and Chothia (1984) J Mol Biol 174, 175-191.
    *
    * THESE ARRAYS ARE SORTED IN PLACE (probabably in O(n**2) time).
    */
    static void sortByLeskSieve(Tuple3[] sm1, Tuple3[] sm2)
    {
        int i, len = sm1.length;
        
        // We're going to screw up the order of these arrays
        // as we "sieve" out the worst-fitting pairs.

        // More variables we'll need
        SuperPoser  sp      = new SuperPoser(sm1, sm2);
        Triple      t       = new Triple();
        Transform   R;
        double      rmsd, gap2, worstGap2;
        int         worstIndex;
        Tuple3      mSwap;
        
        for( ; len > 0; len--)
        {
            sp.reset(sm1, 0, sm2, 0, len);
            R       = sp.superpos();
            rmsd    = sp.calcRMSD(R);
            //rmsd    = sp.calcRMSD(R) / Math.sqrt(len); // from Gerstein & Altman 1995 JMB: rmsd grows as square of # of atoms
            
            // Find worst-fitting pair
            worstIndex  = -1;
            worstGap2   = -1;
            for(i = 0; i < len; i++)
            {
                R.transform(sm2[i], t);
                gap2 = t.sqDistance(sm1[i]);
                if(gap2 > worstGap2)
                {
                    worstGap2 = gap2;
                    worstIndex = i;
                }
            }
            
            // Swap worst pair to back of list
            mSwap = sm1[len-1];
            sm1[len-1] = sm1[worstIndex];
            sm1[worstIndex] = mSwap;
            mSwap = sm2[len-1];
            sm2[len-1] = sm2[worstIndex];
            sm2[worstIndex] = mSwap;
        }
    }
//}}}

//{{{ writeKin
//##############################################################################
    void writeKin(AtomState[][] atoms, int maxlen) throws IOException
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(this.kinOut))));
        out.println("@kinemage");
        out.println("@group {correspondances} dominant");
        out.println("@vectorlist {pairs} color= green");
        for(int i = 0; i < maxlen; i++)
        {
            AtomState a1 = atoms[0][i], a2 = atoms[1][i];
            out.println("{"+a1+"}P "+df.format(a1.getX())+" "+df.format(a1.getY())+" "+df.format(a1.getZ()));
            out.println("{"+a2+"}L "+df.format(a2.getX())+" "+df.format(a2.getY())+" "+df.format(a2.getZ()));
        }
        out.close();
    }
//}}}

//{{{ rms
//##############################################################################
    double rms(AtomState[] a, AtomState[] b)
    {
        double rmsd = 0.0;
        int len = a.length;
        Triple t = new Triple();
        for(int i = 0; i < len; i++)
        {
            t.likeDiff(a[i], b[i]);
            rmsd += t.mag2();
        }
        return Math.sqrt(rmsd / len);
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
    public void Main() throws IOException, ParseException
    {
        if(structIn1 == null || structIn2 == null)
            throw new IllegalArgumentException("must provide two structures");
        
        // Read in structures, get arrays of residues.
        PdbReader pdbReader = new PdbReader();
        CoordinateFile coord1 = pdbReader.read(new File(structIn1));
        CoordinateFile coord2 = pdbReader.read(new File(structIn2));
        Model m1 = coord1.getFirstModel();
        Model m2 = coord2.getFirstModel();
        ModelState s1 = m1.getState();
        ModelState s2 = m2.getState();
        
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
        
        // Fix 180 degree rotations of symmetric sidechains, which give spurious rmsds.
        if(fix180flips) s1 = fix180rotations(s1, s2, align);
        
        // If -super, do superimposition of s1 on s2:
        Transform R = new Transform(); // identity, defaults to no superposition
        if(superimpose != null)
        {
            AtomState[][] atoms = getAtomsForSelection(m1.getResidues(), s1, m2.getResidues(), s2, superimpose, align);
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
            
            int lenAtomsUsed = atoms[0].length;
            if(leskSieve > 0)
            {
                int len = (int) Math.round( leskSieve * atoms[0].length );
                if(len < 3)
                    System.err.println("WARNING: too few atoms for Lesk's sieve at "+df.format(leskSieve));
                else
                {
                    lenAtomsUsed = len;
                    sortByLeskSieve(atoms[0], atoms[1]);
                    superpos.reset(atoms[1], 0, atoms[0], 0, len); // only use the len best
                    R = superpos.superpos();
                    System.err.println(df.format(superpos.calcRMSD(R))+"\t"+len+"\t[Lesk's sieve = "+df.format(leskSieve)+"]");
                }
            }
            
            // Transform model 1 so transformed coords will be used in the future
            for(Iterator iter = Model.extractOrderedStatesByName(m1).iterator(); iter.hasNext(); )
            {
                AtomState as = (AtomState) iter.next();
                R.transform(as);
            }
            
            // Write kinemage showing atoms for superposition:
            if(kinOut != null) writeKin(atoms, lenAtomsUsed);
            
            // Write superimposed PDB file:
            // (... but only do so if we don't care about the RMSD to the target
            // or if that RMSD does not exceed our designated threshold):
            if(pdbOut != null)
            {
                if (Double.isNaN(rmsdCutoff) || superpos.calcRMSD(R) < rmsdCutoff)
                {
                    PdbWriter pdbWriter = new PdbWriter(new File(pdbOut));
                    pdbWriter.writeCoordinateFile(coord1);
                    pdbWriter.close();
                    if (verbose) System.err.println("Writing to "+pdbOut+" b/c RMSD="+
                        df.format(superpos.calcRMSD(R))+" < cutoff="+rmsdCutoff);
                }
                else if (verbose) System.err.println("Not writing to "+pdbOut+" b/c RMSD="+
                    df.format(superpos.calcRMSD(R))+" > cutoff="+rmsdCutoff);
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
            AtomState[][] atoms = getAtomsForSelection(m1.getResidues(), s1, m2.getResidues(), s2, rmsd_sel, align);
            if(verbose)
            {
                System.err.println("Atom alignments:");
                for(int i = 0; i < atoms[0].length; i++)
                    System.err.println("  "+atoms[0][i]+" <==> "+atoms[1][i]);
                System.err.println();
            }
            //!  Ack!  Can't use superpos.calcRMSD() b/c it translates everything to the origen!
            //!  // struct2 is the reference point, struct1 should move.
            //!  // (nothing's really moving here so it doesn't matter).
            //!  SuperPoser superpos = new SuperPoser(atoms[1], atoms[0]);
            //!  // Don't recalculate, use our old transform!
            //!  //R = superpos.superpos();
            //!  // Oops, no, use an identity transform -- coords already moved!
            //!  System.out.println(df.format(superpos.calcRMSD(new Transform()))+"\t"+atoms[0].length+"\t"+rmsd_sel);
            System.out.println(df.format(rms(atoms[1], atoms[0]))+"\t"+atoms[0].length+"\t"+rmsd_sel);
        }
        
        // Print the transform:
        if(showTransform)
        {
            System.out.println("Transformation matrix (premult, Rx -> x'):");
            System.out.println(R);
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
        else if(flag.equals("-t"))
            showTransform = true;
        else if(flag.equals("-noscflip"))
            fix180flips = false;
        else if(flag.equals("-super"))
            superimpose = param;
        else if(flag.equals("-super2"))
            superimpose2 = param; // added by dak
        else if(flag.equals("-sieve"))
        {
            try { leskSieve = Double.parseDouble(param); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException(param+" isn't a number!"); }
            if(leskSieve <= 0 || leskSieve > 1)
                throw new IllegalArgumentException("value for -sieve out of range (0,1]");
        }
        else if(flag.equals("-kin"))
            kinOut = param;
        else if(flag.equals("-pdb"))
            pdbOut = param;
        else if(flag.equals("-rms"))
            rmsd.add(param);
        else if(flag.equals("-rmsdcutoff"))
        {
            try { rmsdCutoff = Double.parseDouble(param); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException(param+" isn't a number!"); }
            if (Double.isNaN(rmsdCutoff) || rmsdCutoff < 0)
                System.err.println("Problem with "+param+" as param for -rmsdcutoff");
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

