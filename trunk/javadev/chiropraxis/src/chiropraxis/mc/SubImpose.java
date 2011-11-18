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
import driftwood.util.Strings;
import chiropraxis.sc.SidechainAngles2;
//}}}
/**
* <code>SubImpose</code> is a tool for superimposing proteins based on a
* carefully specified subset of atoms,
* and calculating RMSD on a (possibly different) set of atoms.
* It's also a play on words for "superimpose" and "subset".
*
* <ol>
* <li>If -super or -super1 is specified, those atoms from structure 1 are used to superimpose it onto the corresponding atoms from structure 2.</li>
* <li>If -super2 is specified, those atoms from structure 2 will correspond to the atoms from structure 1 specified by -super1.</li>
* <li>If -sieve is specified, those atoms are cut down to the specified fraction (0,1] by Lesk's sieve.</li>
* <li>If -kin is specified, a kinemage is written showing the the atom correspondences.</li>
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
    DecimalFormat df  = new DecimalFormat("0.0###");
    DecimalFormat df2 = new DecimalFormat("#0.0##");
    DecimalFormat df3 = new DecimalFormat("0.0###");
//}}}

//{{{ CLASS: SimpleResAligner
//##############################################################################
    public static class SimpleResAligner implements Alignment.Scorer
    {
        // High is good, low is bad.
        public double score(Object a, Object b)
        {
            Residue r = (Residue) a;
            Residue s = (Residue) b;
            if(r.getName().equals(s.getName()))
                return 4;   // match
            else
                return -1;   // mismatch
        }
        
        public double open_gap(Object a) { return -8; }
        public double extend_gap(Object a) { return -2; }
    }
//}}}

//{{{ CLASS: SimpleNonWaterResAligner
//##############################################################################
    /**
    * Extends SimpleResAligner by penalizing water-anything residue pairings, 
    * which in my experience often screwed up alignments and thereby prevented 
    * superpositions that were anywhere close to reasonable. - DAK 090824
    */
    public static class SimpleNonWaterResAligner extends SimpleResAligner
    {
        // High is good, low is bad.
        public double score(Object a, Object b)
        {
            Residue r = (Residue) a;
            Residue s = (Residue) b;
            if(r.getName().equals("HOH") || s.getName().equals("HOH"))
                return -1; // penalize water-anything pairing
            if(r.getName().equals(s.getName()))
                return 4;  // match
            else
                return -1; // mismatch
        }
        
        public double open_gap(Object a) { return -8; }
        public double extend_gap(Object a) { return -2; }
    }
//}}}

//{{{ CLASS: StructureBasedAligner
//##############################################################################
    static class StructureBasedAligner implements Alignment.Scorer
    {
        public boolean atomsAreEquivalant(AtomState s, AtomState t)
        {
            if(s.getName().equals(t.getName()))
                return true;
            if(s.getName().equals(" H  ") && t.getName().equals(" CD ")  // Xaa, Pro
            || s.getName().equals(" HA ") && t.getName().equals(" HA2")  // Xaa, Gly
            || s.getName().equals(" HA ") && t.getName().equals("1HA ")  // Xaa, Gly
            || s.getName().equals(" CB ") && t.getName().equals(" HA3")  // Xaa, Gly
            || s.getName().equals(" CB ") && t.getName().equals("2HA ")) // Xaa, Gly
                return true;
            if(s.getName().equals(" CD ") && t.getName().equals(" H  ")  // Pro, Xaa
            || s.getName().equals(" HA2") && t.getName().equals(" HA ")  // Gly, Xaa
            || s.getName().equals("1HA ") && t.getName().equals(" HA ")  // Gly, Xaa
            || s.getName().equals(" HA3") && t.getName().equals(" CB ")  // Gly, Xaa
            || s.getName().equals("2HA ") && t.getName().equals(" CB ")) // Gly, Xaa
                return true;
            return false;
        }
        
        // High is good, low is bad.
        public double score(Object a, Object b)
        {
            AtomState s = (AtomState) a;
            AtomState t = (AtomState) b;
            if(s == null || t == null)
                return -1;  // gap
            if(!atomsAreEquivalant(s, t))
                return 0;   // mismatch, even accounting for quasi-structurally-equivalent atom names
            else if(s.distance(t) > 2)
                return 0;   // far away
            else if(s.distance(t) <= 2 && s.distance(t) > 1)
                return 1;   // in the neighborhood
            else if(s.distance(t) <= 1 && s.distance(t) > 0.5)
                return 2;   // close
            else if(s.distance(t) <= 0.5)
                return 3;   // very close
            else
            {
                System.err.println("Not sure how to score "+s+" vs. "+t+"!");
                return 0;
            }
        }
        
        public double open_gap(Object a) { return extend_gap(a); }
        public double extend_gap(Object a) { return score(a, null); }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose = false;
    boolean showDists = false;
    boolean showTransform = false;
    boolean fix180flips = true;
    String structIn1 = null, structIn2 = null;
    String kinOut = null, pdbOut = null;
    boolean kinStdOut = false, pdbStdOut = false;
    String superimpose1 = null; // describes atoms in structure 1 for superimposing onto structure 2
    String superimpose2 = null; // describes atoms in structure 2 which will match up with those
                                // in structure 1 described in superimpose1 - DAK
    String chainIDs1 = null; // single-character chain IDs from structure 1 to be used in sequence alignment
    String chainIDs2 = null; // single-character chain IDs from structure 2 to be used in sequence alignment
    String alignFilename = null; // manual sequence alignment input: chain1,resnum1,chain2,resnum2 per line
    Collection rmsd = new ArrayList(); // selection strings to do rmsd over
    double leskSieve = Double.NaN;
    double rmsdCutoff = Double.NaN; // above which PDB is not written out
    double rmsdGoal = Double.NaN; // iterative Lesk sieve until this rmsd is reached
    boolean shuffle = false;
    
    CoordinateFile coord1 = null, coord2 = null;
    Model m1 = null, m2 = null;
    ModelState s1 = null, s2 = null;
    Alignment align = null;
    AtomState[][] atoms = null;
    Transform R = null;
    int lenAtomsUsed = 0;
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

//{{{ readAlignmentFile
//##############################################################################
    private void readAlignmentFile() throws IOException
    {
        ArrayList<Residue> aList = new ArrayList<Residue>();
        ArrayList<Residue> bList = new ArrayList<Residue>();
        FileInputStream fstream = new FileInputStream(alignFilename);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        // Read file line by line
        String line = null;
        while((line = br.readLine()) != null)
        {
            String chain1 = null, chain2 = null;
            int resnum1 = Integer.MIN_VALUE, resnum2 = Integer.MIN_VALUE;
            try
            {
                // Read line
                if(line.equals("")) continue; // ignore blank lines
                String[] parts = Strings.explode(line, ',');
                if(parts.length != 4 && parts.length != 2)
                    throw new IllegalArgumentException(
                        "Format for user-provided alignment file: "+
                        "chain1,resnum1,chain2,resnum2 per line");
                resnum1 = Integer.parseInt(parts[1]);
                resnum2 = Integer.parseInt(parts[3]);
                if(parts.length == 4)
                {
                     chain1 = parts[0];
                     chain2 = parts[2];
                }
                if(chain1 == null || chain1.length() > 2 || chain2 == null || chain2.length() > 2)
                    throw new IllegalArgumentException(
                        "Format for user-provided alignment file: "+
                        "chain1,resnum1,chain2,resnum2 per line");
                
                // Find matching residues
                Residue r1 = null, r2 = null;
                for(Iterator iter = m1.getResidues().iterator(); iter.hasNext(); )
                {
                    Residue r = (Residue) iter.next();
                    if(parts.length == 4)
                    {
                        // Respect chain IDs
                        if(chain1 != null && r.getChain().equals(chain1)
                        && resnum1 != Integer.MIN_VALUE && r.getSequenceInteger() == resnum1)
                        {
                            r1 = r;
                            break;
                        }
                    }
                    else //if(parts.length == 2)
                    {
                        // Ignore chain IDs
                        if(resnum1 != Integer.MIN_VALUE && r.getSequenceInteger() == resnum1)
                        {
                            r1 = r;
                            break;
                        }
                    }
                }
                for(Iterator iter = m2.getResidues().iterator(); iter.hasNext(); )
                {
                    Residue r = (Residue) iter.next();
                    if(parts.length == 4)
                    {
                        // Respect chain IDs
                        if(chain2 != null && r.getChain().equals(chain2)
                        && resnum2 != Integer.MIN_VALUE && r.getSequenceInteger() == resnum2)
                        {
                            r2 = r;
                            break;
                        }
                    }
                    else //if(parts.length == 2)
                    {
                        // Ignore chain IDs
                        if(resnum2 != Integer.MIN_VALUE && r.getSequenceInteger() == resnum2)
                        {
                            r2 = r;
                            break;
                        }
                    }
                }
                
                // Add to alignment
                if(r1 != null && r2 != null)
                {
                    aList.add(r1);
                    bList.add(r2);
                    //System.err.println("Added "+r1+" <==> "+r2+" to manual alignment");
                }
                else throw new IllegalArgumentException(
                    "Can't find residues corresponding to "+line+"!");
            }
            catch(NumberFormatException ex)
            { System.err.println("Can't parse number in "+line+"!"); }
        }
        
        Object[] a = new Residue[aList.size()];
        Object[] b = new Residue[bList.size()];
        for(int i = 0; i < aList.size(); i++) a[i] = aList.get(i);
        for(int i = 0; i < bList.size(); i++) b[i] = bList.get(i);
        if(a.length != b.length) throw new IllegalArgumentException(
            "Different lengths for 2 sides of manual alignment!");
        align.a = a;
        align.b = b;
    }
//}}}

//{{{ getChains
//##############################################################################
    /**
    * If chainIDs is not null, the user has specified which chains will be used
    * for sequence alignment, e.g. when multiple copies are present in a crystal
    * lattice or something. Independent of and preceding actual choice of atoms 
    * atoms for superposition.
    */
    public static Collection getChains(Model m, String chainIDs)
    {
        Collection chains = new ArrayList();
        for(Iterator iter = m.getChainIDs().iterator(); iter.hasNext(); )
        {
            String chainID = (String) iter.next();
            if(chainIDs != null && chainIDs.indexOf(chainID) == -1)
                continue; // only use chains of interest
            chains.add( m.getChain(chainID) );
        }
        return chains;
    }
//}}}

//{{{ getAtomsForSelection
//##############################################################################
    /**
    * Apply the given selection to res1/s1, then find the corresponding atoms in res2/s2. 
    * If a second selection is given, instead apply that to res2/s2 
    * to find what the user thinks should be the corresponding atoms.
    * return as a 2xN array of matched AtomStates, no nulls.
    */
    public static AtomState[][] getAtomsForSelection(Collection res1, ModelState s1, Collection res2, ModelState s2, String selection1, String selection2, Alignment alignment, CoordinateFile cf1, CoordinateFile cf2) throws ParseException
    {
        // Get selected atom states from model 1
        Selection sel = Selection.fromString(selection1);
        Collection allStates1 = Model.extractOrderedStatesByName(res1, Collections.singleton(s1));
        sel.init(allStates1, cf1);
        Collection selStates1 = new ArrayList();
        for(Iterator iter = allStates1.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if(sel.select(as))  selStates1.add(as);
        }
        
        int matched = 0;
        Collection selStates2 = new ArrayList();
        if(selection2 != null)
        {
            // Residue correspondences given by flag
            // Get selected atom states from model 2
            Selection sel2 = Selection.fromString(selection2);
            Collection allStates2 = Model.extractOrderedStatesByName(res2, Collections.singleton(s2));
            sel2.init(allStates2, cf2);
            for(Iterator iter2 = allStates2.iterator(); iter2.hasNext(); )
            {
                AtomState as2 = (AtomState) iter2.next();
                if(sel2.select(as2))
                {
                    selStates2.add(as2);
                    matched++;
                }
            }
        }
        else
        {
            // Residue correspondences set up algorithmically, as Ian originally intended
            Map map1to2 = new HashMap();
            for(int i = 0; i < alignment.a.length; i++)
            {
                if(alignment.a[i] != null)
                    map1to2.put(alignment.a[i], alignment.b[i]); // b[i] may be null
            }
            
            // Get corresponding states from model 2
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

//{{{ permuteAtoms
//##############################################################################
    /**
    * Try all possible permutations of selected atoms from structure 1 
    * vs. all possible permutations of selected atoms from structure 2.
    * Preserve N->C sequence order to avoid unmeaningful superpositions, 
    * so in effect only permute atoms within the same residue.
    * If this method is not used, it is assumed that the atoms were read 
    * in the proper order in both structures and are thus paired correctly.  
    * This often works fine if the same number of atoms is selected per residue, 
    * but may fail if more or fewer atoms are selected from specific residues.
    **/
    AtomState[][] permuteAtoms()
    {
        double best_rmsd = Double.POSITIVE_INFINITY;
        int max = atoms[0].length; // == atoms[1].length
        int[] x = new int[max], best_x = null;
        int[] y = new int[max], best_y = null;
        for(int i = 0; i < max; i++)
        {
            x[i] = i; // mercurial indices to atoms from mobile structure
            y[i] = i; // mercurial indices to atoms from static structure
        }
        int tried = 0;
        while(true) // that x has more permutations for mobile atoms
        {
            AtomState[] atoms0 = new AtomState[max];
            for(int i = 0; i < max; i++) atoms0[i] = atoms[0][x[i]];
            if(validPermutation(atoms0))
            {
                while(true) // that y has more permutations for static atoms
                {
                    AtomState[] atoms1 = new AtomState[max];
                    for(int i = 0; i < max; i++) atoms1[i] = atoms[1][y[i]];
                    if(validPermutation(atoms1))
                    {
                        tried++;
                        SuperPoser superpos = new SuperPoser(atoms1, atoms0);
                        R = superpos.superpos();
                        double rmsd = superpos.calcRMSD(R);
                        if(rmsd < best_rmsd)
                        {
                            best_rmsd = rmsd;
                            best_x = (int[]) x.clone();
                            best_y = (int[]) y.clone();
                        }
                    }
                    if(!next_permutation(y)) break; // from inner
                }
            }//inner: static
            if(!next_permutation(x)) break; // from outer
        }//outer: mobile
        if(verbose) System.err.println("Tried "+tried+" permutation(s); "+
            "best rmsd: "+df.format(best_rmsd));
        
        // Construct the final array
        AtomState[][] bestAtoms = new AtomState[ 2 ][ max ];
        for(int i = 0; i < max; i++)
        {
            bestAtoms[0][i] = atoms[ 0 ] [ best_x[i] ]; // mobile (permuted)
            bestAtoms[1][i] = atoms[ 1 ] [ best_y[i] ]; // static (permuted)
        }
        return bestAtoms;
    }
    
    /**
    * Makes sure that the supplied array of atoms is "valid" 
    * by checking that all pairs of consecutive atoms are in sequence order.
    */
    private boolean validPermutation(AtomState[] a)
    {
        for(int j = 0; j < a.length-1; j++)
        {
            Residue r1 = a[j  ].getAtom().getResidue();
            Residue r2 = a[j+1].getAtom().getResidue();
            if(r1.getSequenceInteger() > r2.getSequenceInteger()) return false;
        }
        return true; // apparently in sequence order
    }
//}}}

//{{{ next_permutation, reverse
//##############################################################################    
    /**
    * Borrowed from the C++ STL via a nice blog entry:
    *   http://marknelson.us/2002/03/01/next-permutation
    * Called repeatedly with a sequence of integers initially in ascending order,
    * will generate all permutations in lexicographical order, returning false
    * when no more remain.
    */
    private static boolean next_permutation(int[] x)
    {
        if(x.length <= 1) return false;
        int i = x.length - 1;
        while(true)
        {
            int ii = i--;
            if(x[i] < x[ii])
            {
                int j = x.length;
                while(!(x[i] < x[--j]));
                int swap = x[i]; x[i] = x[j]; x[j] = swap;
                reverse(x, ii, x.length);
                return true;
            }
            if(i == 0)
            {
                reverse(x, 0, x.length);
                return false;
            }
        }
    }
    
    private static void reverse(int[] x, int start_inc, int end_exc)
    {
        int i = start_inc, j = end_exc-1;
        while(i < j)
        {
            int swap = x[i];
            x[i] = x[j];
            x[j] = swap;
            i++;
            j--;
        }
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
    public static void sortByLeskSieve(Tuple3[] sm1, Tuple3[] sm2)
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

//{{{ writeKin, writePdb
//##############################################################################
    void writeKin() throws IOException
    {
        PrintStream out = null;
        if(this.kinOut != null) out = new PrintStream(new BufferedOutputStream(new FileOutputStream(this.kinOut)));
        else                    out = new PrintStream(System.out);
        if(out == null)  throw new IOException("*** Error writing kinemage!");
        out.println("@kinemage");
        out.println("@group {correspondances} dominant");
        out.println("@vectorlist {pairs} color= green");
        for(int i = 0; i < lenAtomsUsed; i++)
        {
            AtomState a1 = atoms[0][i], a2 = atoms[1][i];
            out.println("{"+a1+"}P "+df3.format(a1.getX())+" "+df3.format(a1.getY())+" "+df3.format(a1.getZ()));
            out.println("{"+a2+"}L "+df3.format(a2.getX())+" "+df3.format(a2.getY())+" "+df3.format(a2.getZ()));
        }
        out.close();
    }

    public void writePdb() throws IOException
    {
        PdbWriter pdbWriter = null;
        if(pdbOut != null) pdbWriter = new PdbWriter(new File(pdbOut));
        else               pdbWriter = new PdbWriter(System.out);
        if(pdbWriter == null) throw new IOException("*** Error writing PDB!");
        pdbWriter.writeCoordinateFile(coord1);
        pdbWriter.close();
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

//{{{ doSuperposition
//##############################################################################
    public void doSuperposition() throws IOException, ParseException
    {
        // Decide which atom selection to use
        if(superimpose1 == null)
        {
            superimpose1 = "atom_CA_";
            if(verbose) System.err.print("No -super flag; using sequence-aligned CAs");
            if(!Double.isNaN(rmsdGoal)) // iterative sieve until rmsd goal is reached
            {
                if(verbose) System.err.println(" (sieve to "+rmsdGoal+"A rmsd)");
                if(!Double.isNaN(leskSieve)) System.err.println(
                    "Using -rmsdgoal="+rmsdGoal+" instead of -sieve="+leskSieve);
            }
            else // single sieve; default if not user-defined
            {
                if(Double.isNaN(leskSieve)) if(verbose) System.err.println(" (no sieve)");
                else if(verbose) System.err.println(" (best "+leskSieve*100+"%)");
            }
        }
        if(alignFilename != null && superimpose2 != null)
        {
            System.err.print("Ignoring -super2 flag because alignment file provided");
            superimpose2 = null;
        }
        
        // Align residues by sequence
        // With first approach, alignments can't cross chain boundaries.
        // As many chains as possible are aligned, without doubling up.
        if(chainIDs1 != null) System.err.println("Using subset of structure 1 chains: "+chainIDs1);
        if(chainIDs2 != null) System.err.println("Using subset of structure 2 chains: "+chainIDs2);
        align = Alignment.alignChains(getChains(m1, chainIDs1), getChains(m2, chainIDs2),
            new Alignment.NeedlemanWunsch(), new SimpleNonWaterResAligner());
        if(align.a.length == 0)
        {
            System.err.println("No good chain-to-chain alignment; using chain-break-crossing method");
            // The multiple chain alignment method above may have rejected ALL alignments 
            // because none of them is great.
            // This can even happen for one chain vs. one chain when they don't match well.
            // So, just align all residues without regard to chain IDs.
            // This was originally the default in the code before Ian added the multiple 
            // chain alignment method above; we'll use it now because it will actually 
            // return *something* despite its imperfections. -DAK 110129
            align = Alignment.needlemanWunsch(m1.getResidues().toArray(), 
                m2.getResidues().toArray(), new SimpleNonWaterResAligner());
        }
        if(alignFilename != null)
        {
            // Hijack Alignment object & replace with user-supplied alignment
            try
            {
                readAlignmentFile();
            }
            catch(IOException ex)
            {
                System.err.println("Error reading alignment file: "+alignFilename+"!"
                    +"  Using default automatic sequence alignment");
            }
        }
        if(verbose)
        {
            System.err.println("Residue alignments:");
            for(int i = 0; i < align.a.length; i++)
                System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            System.err.println();
        }
        
        // Fix 180 degree rotations of symmetric sidechains, which give spurious rmsds.
        if(fix180flips) s1 = fix180rotations(s1, s2, align);
        
        // If -super, do superimposition of s1 on s2.
        R = new Transform(); // identity, defaults to no superposition
        atoms = getAtomsForSelection(m1.getResidues(), s1, m2.getResidues(), 
            s2, superimpose1, superimpose2, align, coord1, coord2);
        if(shuffle)
        {
            System.err.println("Shuffling intra-residue atoms");
            permuteAtoms();
        }
        if(verbose)
        {
            System.err.println("Atom alignments:");
            for(int i = 0; i < atoms[0].length; i++)
                System.err.println("  "+atoms[0][i]+" <==> "+atoms[1][i]);
            System.err.println();
        }
        if(atoms[0].length < 3)
            throw new IllegalArgumentException("Can't superimpose on less than 3 atoms!");
        
        // struct2 is the reference point; struct1 should move.
        SuperPoser superpos = new SuperPoser(atoms[1], atoms[0]);
        R = superpos.superpos();
        System.err.println("rmsd\tn_atoms\tselection");
        System.err.println(df.format(superpos.calcRMSD(R))+"\t"+atoms[0].length+"\t"+superimpose1);
        
        lenAtomsUsed = atoms[0].length;
        if(!Double.isNaN(rmsdGoal))
        {
            // Eliminate selected atoms one-by-one until rmsd <= goal
            int sieveCount = 0;
            while(superpos.calcRMSD(R) > rmsdGoal)
            {
                sieveCount++;
                sortByLeskSieve(atoms[0], atoms[1]);
                int len = atoms[0].length - 1; // new length after we drop worst-fitting atom pair
                AtomState[][] newAtoms = new AtomState[2][len];
                for(int i = 0; i < 2; i++) for(int j = 0; j < len; j++) newAtoms[i][j] = atoms[i][j];
                atoms = newAtoms;
                if(atoms[0].length < 3) throw new IllegalArgumentException(
                    "Can't achieve rmsd goal of "+rmsdGoal+"A .. would have to trim to < 3 atoms!");
                superpos.reset(atoms[1], atoms[0]);
                R = superpos.superpos();
            }
            System.err.println(df.format(superpos.calcRMSD(R))+"\t"+atoms[0].length
                +"\t"+superimpose1+"  [sieve #"+sieveCount+"]");
        }
        else if(!Double.isNaN(leskSieve))
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
                System.err.println(df.format(superpos.calcRMSD(R))+"\t"+len
                    +"\t"+superimpose1+"  [sieve = "+df.format(leskSieve)+"]");
            }
        }
        
        // Transform model 1 so transformed coords will be used in the future.
        for(Iterator iter = Model.extractOrderedStatesByName(m1).iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            R.transform(as);
        }
        
        // Make sure RMSD does not exceed our threshold (if applicable)
        if(!Double.isNaN(rmsdCutoff))
        {
            if(superpos.calcRMSD(R) > rmsdCutoff)
            {
                System.err.println("No further output b/c RMSD="+
                    df.format(superpos.calcRMSD(R))+" > cutoff="+rmsdCutoff);
                atoms = null; // signal to Main()
                return;
            }
            else System.err.println("Proceeding with output b/c RMSD="+
                df.format(superpos.calcRMSD(R))+" <= cutoff="+rmsdCutoff);
        }
    }
//}}}

//{{{ doRmsd
//##############################################################################
    public void doRmsd() throws ParseException
    {
        for(Iterator iter = rmsd.iterator(); iter.hasNext(); )
        {
            String rmsd_sel = (String) iter.next();
            atoms = getAtomsForSelection(m1.getResidues(), s1, m2.getResidues(), s2, rmsd_sel, null, align, coord1, coord2);
            if(verbose)
            {
                System.err.println("Atom alignments:");
                for(int i = 0; i < atoms[0].length; i++)
                    System.err.println("  "+atoms[0][i]+" <==> "+atoms[1][i]);
                System.err.println();
            }
            //!  Ack!  Can't use superpos.calcRMSD() b/c it translates everything to the origin!
            //!  // struct2 is the reference point, struct1 should move.
            //!  // (nothing's really moving here so it doesn't matter).
            //!  SuperPoser superpos = new SuperPoser(atoms[1], atoms[0]);
            //!  // Don't recalculate, use our old transform!
            //!  //R = superpos.superpos();
            //!  // Oops, no, use an identity transform -- coords already moved!
            //!  System.out.println(df.format(superpos.calcRMSD(new Transform()))+"\t"+atoms[0].length+"\t"+rmsd_sel);
            System.out.println(df.format(rms(atoms[1], atoms[0]))+"\t"+atoms[0].length+"\t"+rmsd_sel);
        }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, ParseException
    {
        // Read in structures, get arrays of residues.
        if(structIn1 == null || structIn2 == null)
            throw new IllegalArgumentException("must provide two structures");
        PdbReader pdbReader = new PdbReader();
        coord1 = pdbReader.read(new File(structIn1));
        coord2 = pdbReader.read(new File(structIn2));
        m1 = coord1.getFirstModel();
        m2 = coord2.getFirstModel();
        s1 = m1.getState();
        s2 = m2.getState();
        
        // Superpose structure 2 onto structure 1 regardless of other options
        doSuperposition();
        
        if(atoms == null)
        {
            // If user wants PDB output but isn't using -pdb=outfile, 
            // warn them about what might happen
            if(!showTransform && !showDists && rmsd.isEmpty() && kinOut == null
            && !kinStdOut && pdbOut == null) System.err.println(
                "WARNING: You'll still get empty files if you use 'subimpose > outfile'!"
                +"\nUse 'subimpose -pdb=outfile' to be safe!");
            return;
        }
        
        // If -t, print the transform
        if(showTransform)
            System.out.println("Transformation matrix (premult, Rx -> x'):\n"+R);
        
        // If -d, print distances between atoms from selection
        else if(showDists)
            for(int i = 0; i < atoms[0].length; i++)
                System.out.println(atoms[0][i].getAtom()+","+atoms[1][i].getAtom()+","
                    +df2.format(Triple.distance(atoms[0][i], atoms[1][i])));
        
        // If -rms, do RMSD calculations over different sets of atoms
        else if(!rmsd.isEmpty())
            doRmsd();
        
        // If -kin, write kinemage showing atoms for superposition
        else if(kinOut != null || kinStdOut)
            writeKin();
        
        // If no other output specified, write superimposed PDB file
        //else if(atoms != null) // we already had an if(atoms == null) clause above
        else
            writePdb();
        // WARNING: You'll still get empty files if you use "subimpose > outfile"!
        // Use "subimpose -pdb=outfile" to be safe!
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
        else if(flag.equals("-d"))
            showDists = true;
        else if(flag.equals("-noscflip"))
            fix180flips = false;
        else if(flag.equals("-chains1") || flag.equals("-chain1") || flag.equals("-chains") || flag.equals("-chain"))
            chainIDs1 = param;
        else if(flag.equals("-chains2") || flag.equals("-chain2"))
            chainIDs2 = param;
        else if(flag.equals("-super1") || flag.equals("-super"))
            superimpose1 = param;
        else if(flag.equals("-super2"))
            superimpose2 = param;
        else if(flag.equals("-pdb"))
        {
            if(param == null) pdbStdOut = true;
            else pdbOut = param;
        }
        else if(flag.equals("-kin"))
        {
            if(param == null) kinStdOut = true;
            else kinOut = param;
        }
        else if(flag.equals("-rms"))
        {
            if(param == null) throw new IllegalArgumentException("-rms must be followed by a number!");
            rmsd.add(param);
        }
        else if(flag.equals("-sieve"))
        {
            try { leskSieve = Double.parseDouble(param); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException(param+" isn't a number!"); }
            if(leskSieve <= 0 || leskSieve > 1)
                throw new IllegalArgumentException("value for -sieve out of range (0,1]");
        }
        else if(flag.equals("-rmsdgoal") || flag.equals("-rmsdtarget"))
        {
            try { rmsdGoal = Double.parseDouble(param); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException(param+" isn't a number!"); }
            if(Double.isNaN(rmsdGoal) || rmsdGoal < 0)
                System.err.println("Problem with "+param+" as param for -rmsdgoal");
        }
        else if(flag.equals("-rmsdcutoff") || flag.equals("-rmsdmax"))
        {
            try { rmsdCutoff = Double.parseDouble(param); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException(param+" isn't a number!"); }
            if(Double.isNaN(rmsdCutoff) || rmsdCutoff < 0)
                System.err.println("Problem with "+param+" as param for -rmsdcutoff");
        }
        else if(flag.equals("-align"))
        {
            alignFilename = param;
        }
        else if(flag.equals("-shuffle"))
        {
            shuffle = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

