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
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>Suppose</code> generates kinemages with lots of information
* that is helpful for superimposing two protein structures,
* including a standard difference-distance map,
* unsigned-sum and vector difference-distance plots,
* a Lesk plot, and LSQMAN commands to transform one PDB onto the other.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Sep  9 08:48:55 EDT 2003
*/
public class Suppose //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Model           model1  = null;
    Model           model2  = null;
    AtomState[]     ca1     = null;
    AtomState[]     ca2     = null;
    String[]        labels  = null;
    final int       nIter   = 7;
    Triple[][]      vdd12   = null;
    Triple[][]      vdd21   = null;
    double[][]      vdd_w   = null;
    double[][]      usdd    = null;
    
    int             nResToSuperpos  = -1;
    double          fracToSuperpos  = -1;

    File            file1 = null, file2 = null;
    InputStream     input1 = null, input2 = null;
    PrintStream     output = System.out;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Suppose()
    {
        super();
    }
//}}}

//{{{ computeUSDD
//##############################################################################
    /**
    * Calculates the (weighted) unsigned-sum difference distance 
    * for each point in m1 against all the points in m2.
    * This operation *is* symmetric/commutative: usdd(m1, m2) == usdd(m2, m1)
    * @throws IllegalArgumentException if m1, m2, and/or w are of different lengths.
    */
    static public double[] computeUSDD(Triple[] m1, Triple[] m2, double[] w)
    {
        // Check inputs
        if(m1.length != m2.length || m1.length != w.length)
            throw new IllegalArgumentException("Lengths of inputs must match");
        
        // Define variables
        int i, j;                   // for iteration
        int len = m1.length;        // number of points
        double diffdist;            // difference in distance
        double weight;              // weight for length, from w[]
        // offsets for output
        double[] off = new double[len];
        for(i = 0; i < len; i++) off[i] = 0.0;
        
        // Iterate over all points in m1
        for(i = 0; i < len; i++)
        {
            // Iterate over all points in m2
            for(j = 0; j < len; j++)
            {
                diffdist    = m1[i].distance(m1[j]) - m2[i].distance(m2[j]);
                weight      = w[i] / (w[i] + w[j]); // raw weight
                off[i]      += Math.abs(weight*diffdist);
            }
            // average movement, adjusted to be comparable to VDD mag.
            off[i] *= 2.5/len;
        }
        
        return off;
    }
//}}}

//{{{ computeVDD
//##############################################################################
    /**
    * For each point i in m1, the difference in i-to-j distance between m1 and m2
    * is computed and projected as a vector along the i-j bond (for all j from 1 to N);
    * these vectors are summed.
    * The sum is weighted such that the contribution of the i-to-j signal
    * is Wi/(Wi+Wj) for i and Wj/(Wi+Wj) for j.
    * All results are scaled by 3/N to give approximately correct lengths as well.
    *
    * <p>This algorithm requires O(N^2) time and O(N) memory.
    *
    * @param m1     the first set of points (Model 1)
    * @param m2     the second set of points
    * @param w      a set of per-point weights
    * @throws IllegalArgumentException if m1, m2, and/or w are of different lengths.
    */
    static public Triple[] computeVDD(Triple[] m1, Triple[] m2, double[] w)
    {
        // Check inputs
        if(m1.length != m2.length || m1.length != w.length)
            throw new IllegalArgumentException("Lengths of inputs must match");
        
        // Define variables
        int i, j;                   // for iteration
        int len = m1.length;        // number of points
        Triple t = new Triple();    // working register
        double diffdist;            // difference in distance
        double weight;              // weight for vector from w[]
        // offset vectors for output
        Triple[] off = new Triple[len];
        for(i = 0; i < len; i++) off[i] = new Triple(0,0,0);
        
        // Iterate over all points in m1
        for(i = 0; i < len; i++)
        {
            // Iterate over all points in m2
            for(j = 0; j < len; j++)
            {
                diffdist    = m1[i].distance(m1[j]) - m2[i].distance(m2[j]);
                weight      = w[i] / (w[i] + w[j]); // raw weight
                t.likeVector(m1[i], m1[j]);         // from i to j
                t.unit().mult(weight*diffdist);     // length = weight*diffdist
                off[i].add(t);                      // sum up for point i
            }
            off[i].mult(3.0/len); // approximates correct length
        }
        
        return off;
    }
//}}}

//{{{ kin3dVDD
//##############################################################################
    /**
    * Write out the kinemage-format points of a @vectorlist to display
    * the results of the VDD calculation.
    * This function does not write the actual @vectorlist line, but
    * only the point definitions following it.
    * @throws IllegalArgumentException if m1 and off are of different lengths.
    */
    static public void kin3dVDD(OutputStream out, Triple[] m1, Triple[] off)
    { kin3dVDD(new OutputStreamWriter(out), m1, off); }
    
    static public void kin3dVDD(Writer w, Triple[] m1, Triple[] off)
    {
        PrintWriter     out = new PrintWriter(w);
        DecimalFormat   df  = new DecimalFormat("0.0###");
        Triple          t   = new Triple();
        if(m1.length != off.length)
            throw new IllegalArgumentException("Lengths of inputs must match");
        
        for(int i = 0; i < m1.length; i++)
        {
            t.like(m1[i]);
            out.print("{}P "+df.format(t.getX())+" "+df.format(t.getY())+" "+df.format(t.getZ()));
            t.add(off[i]);
            out.println("{} "+df.format(t.getX())+" "+df.format(t.getY())+" "+df.format(t.getZ()));
        }
        
        out.flush();
    }
//}}}

//{{{ kin2dStdDD
//##############################################################################
    /**
    * Write out the kinemage-format points of a @vectorlist to display
    * the results of the standard difference-distance calculation.
    * The result is a 3-D bar chart of vector magnitudes, with the pointIDs
    * taken from the toString() method of objects in m1[] and m2[].
    * This function does not write the actual @vectorlist line, but
    * only the point definitions following it.
    * @throws IllegalArgumentException if m1 and off are of different lengths.
    */
    static public void kin2dStdDD(OutputStream out, Triple[] m1, Triple[] m2, String[] labels)
    { kin2dStdDD(new OutputStreamWriter(out), m1, m2, labels); }
    
    static public void kin2dStdDD(Writer w, Triple[] m1, Triple[] m2, String[] labels)
    {
        PrintWriter     out = new PrintWriter(w);
        DecimalFormat   df  = new DecimalFormat("0.0###");
        if(m1.length != m2.length || m1.length != labels.length)
            throw new IllegalArgumentException("Lengths of inputs must match");
        
        double[] levels = {-1, 0.025, 0.050, 0.075, 0.100, 0.200, 0.300, 0.600, 1.0,
            1.5, 2.0, 3.5, 5.0, Double.POSITIVE_INFINITY};
        String[] colors = {"gray", "purple", "blue", "sky", "cyan", "sea", "green", "lime",
            "yellow", "gold", "orange", "red", "hotpink"};
        String color = "deadwhite";        
        
        double diffdist;
        for(int i = 0; i < m1.length; i++)
        {
            for(int j = 0; j < m2.length; j++)
            {
                diffdist = Math.abs(m1[i].distance(m1[j]) - m2[i].distance(m2[j]));
                for(int k = 0; diffdist > levels[k]; k++) color = colors[k];
                out.println("{"+df.format(diffdist)+": "+labels[i]+","+labels[j]+"}P "
                    +color+" "+df.format(i)+" "+df.format(j)+" 0.0");
            }
        }
        
        out.flush();
    }
//}}}

//{{{ kin1dVDD
//##############################################################################
    /**
    * Write out the kinemage-format points of a @vectorlist to display
    * the results of the VDD calculation.
    * The result is bar chart of vector magnitudes, with the pointIDs
    * taken from the toString() method of objects in m1[].
    * This function does not write the actual @vectorlist line, but
    * only the point definitions following it.
    * @throws IllegalArgumentException if m1 and off are of different lengths.
    */
    static public void kin1dVDD(OutputStream out, String[] labels, double[] off, double zDepth)
    { kin1dVDD(new OutputStreamWriter(out), labels, off, zDepth); }
    
    static public void kin1dVDD(Writer w, String[] labels, double[] off, double zDepth)
    {
        PrintWriter     out = new PrintWriter(w);
        DecimalFormat   df  = new DecimalFormat("0.0###");
        if(labels.length != off.length)
            throw new IllegalArgumentException("Lengths of inputs must match");
        
        for(int i = 0; i < labels.length; i++)
        {
            out.print("{"+labels[i]+"}P "+df.format(0.2*i)+" 0.0 "+df.format(zDepth));
            out.println("{\"} "+df.format(0.2*i)+" "+df.format(off[i])+" "+df.format(zDepth));
        }
        
        out.flush();
    }
//}}}

//{{{ sortByDD, CLASS: SortItem
//##############################################################################
    /** Container for sorting vector difference distances by magnitude */
    static public class SortItem implements Comparable
    {
        public Tuple3      ca1, ca2;
        public double      score;      // either diff-dist or rmsd
        public String      label;
        
        public SortItem(Tuple3 ca1, Tuple3 ca2, double s, String lbl)
        {
            this.ca1    = ca1;
            this.ca2    = ca2;
            score       = s;
            label       = lbl;
        }
        
        public int compareTo(Object o)
        {
            SortItem that = (SortItem)o;
            // Sort greatest to least
            if(this.score > that.score)         return -1;
            else if(this.score < that.score)    return 1;
            else                                return 0;
        }
    }
    
    /**
    * Returns an array of sorted items with the point with the greatest
    * vector difference distance at the front.
    */
    static public SortItem[] sortByDD(Tuple3[] m1, Tuple3[] m2, double[] diffdist, String[] labels)
    {
        if(m1.length != m2.length || m1.length != diffdist.length)
            throw new IllegalArgumentException("Lengths of inputs must match");
        
        SortItem[] toSort = new SortItem[m1.length];
        for(int i = 0; i < m1.length; i++)
        {
            toSort[i] = new SortItem(m1[i], m2[i], diffdist[i], labels[i]);
        }
        Arrays.sort(toSort);
        
        return toSort;
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
    */
    static public SortItem[] sortByLeskSieve(Tuple3[] m1, Tuple3[] m2, String[] labels)
    {
        int i, len = m1.length;
        
        // We're going to screw up the order of these arrays
        // as we "sieve" out the worst-fitting pairs.
        Tuple3[]    sm1     = new Tuple3[m1.length];
        Tuple3[]    sm2     = new Tuple3[m2.length];
        String[]    slabels = new String[labels.length];
        double[]    w       = new double[len];
        for(i = 0; i < len; i++)
        {
            sm1[i]      = m1[i];
            sm2[i]      = m2[i];
            slabels[i]  = labels[i];
            w[i]        = 1.0;
        }

        // More variables we'll need
        SortItem[]  sorted  = new SortItem[len];
        SuperPoser  sp      = new SuperPoser(sm1, sm2);
        Triple      t       = new Triple();
        Transform   R;
        double      rmsd, gap2, worstGap2;
        int         worstIndex;
        Tuple3      mSwap;
        String      lSwap;
        
        for( ; len > 0; len--)
        {
            sp.reset(sm1, 0, sm2, 0, len);
            R       = sp.superpos(w);
            rmsd    = sp.calcRMSD(R, w);
            
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
            
            // Enter worst pair at front of list
            sorted[ sorted.length - len ] = new SortItem(
                sm1[worstIndex], sm2[worstIndex], rmsd, slabels[worstIndex]);
            
            // Swap worst pair to back of list
            mSwap = sm1[len-1];
            sm1[len-1] = sm1[worstIndex];
            sm1[worstIndex] = mSwap;
            mSwap = sm2[len-1];
            sm2[len-1] = sm2[worstIndex];
            sm2[worstIndex] = mSwap;
            lSwap = slabels[len-1];
            slabels[len-1] = slabels[worstIndex];
            slabels[worstIndex] = lSwap;
        }
        
        return sorted;
    }
//}}}

//{{{ loadModel, loadAtomStates
//##############################################################################
    Model loadModel(InputStream in) throws IOException
    {
        if(in == null)
            throw new IllegalArgumentException("Must supply two input files");
        
        // Load model group from PDB files
        PdbReader pdbReader = new PdbReader();
        ModelGroup mg = pdbReader.read(in);
        return mg.getFirstModel();
    }
    
    AtomState[] loadAtomStates(Model m)
    {
        // Extract the C-alphas
        Collection  res     = m.getResidues();
        ModelState  state   = m.getState();
        ArrayList   atoms   = new ArrayList();
        for(Iterator iter = res.iterator(); iter.hasNext(); )
        {
            Residue r = (Residue)iter.next();
            Atom a = r.getAtom(" CA ");
            if(a != null) atoms.add(state.get(a));
        }
        
        // Make them into an array
        AtomState[] ca = (AtomState[])atoms.toArray(new AtomState[atoms.size()]);
        return ca;
    }
//}}}

//{{{ makeLsqman, printRes
//##############################################################################
    public String makeLsqman(SortItem[] sorted, int firstN)
    {
        int         i;
        int         numRes  = firstN;
        int         offset  = sorted.length - numRes;
        Residue[]   res1    = new Residue[numRes];
        Residue[]   res2    = new Residue[numRes];
        
        // Put the best fraction of residues into res,
        // then sort them into their natural order.
        for(i = 0; i < numRes; i++)
        {
            res1[i] = ((AtomState)sorted[i+offset].ca1).getResidue();
            res2[i] = ((AtomState)sorted[i+offset].ca2).getResidue();
        }
        Arrays.sort(res1);
        Arrays.sort(res2);
        
        StringBuffer    sb1         = new StringBuffer();
        StringBuffer    sb2         = new StringBuffer();
        Residue         firstRes    = res1[0];
        Residue         lastRes     = res1[0];
        sb1.append(printRes(res1[0], true));
        sb2.append(printRes(res2[0], true));
        for(i = 1; i < numRes; i++)
        {
            if(res1[i] != lastRes.getNext(model1))
            {
                if(lastRes != firstRes)
                    sb1.append('-').append(printRes(lastRes, false));
                sb1.append(' ');
                firstRes = res1[i];
                sb1.append(printRes(res1[i], true));
                sb2.append(' ');
                sb2.append(printRes(res2[i], true));
            }
            lastRes = res1[i];
        }
        if(lastRes != firstRes)
            sb1.append('-').append(printRes(lastRes, false));
        
        return "ex m1 \""+sb1+"\" m2 \""+sb2+"\"";
    }
    
    static public String printRes(Residue r, boolean useChainID)
    {
        StringBuffer sb = new StringBuffer();
        if(useChainID && r.getChain() != ' ') sb.append(r.getChain());
        sb.append(r.getSequenceNumber());
        if(r.getInsertionCode() != ' ') sb.append(r.getInsertionCode());
        return sb.toString();
    }
//}}}

//{{{ plotDiffDist
//##############################################################################
    void plotDiffDist()
    {
        int i;
        DecimalFormat df = new DecimalFormat("0.0###");
        
        // Kinemage 1 - the color-coded, standard difference distance plot
        AtomState[] sca1    = new AtomState[ca1.length];
        AtomState[] sca2    = new AtomState[ca2.length];
        String[]    slabels = new String[labels.length];
        output.println("@kinemage 1");
        output.println("@title {std diff dist}");
        output.println("@group {unsorted} dominant");
        output.println("@subgroup {diff dist}");
        output.println("@balllist {diff dist} nohighlight radius= 0.45");
        kin2dStdDD(output, ca1, ca2, labels);
        
        /*
        SortItem[]      sorted;
        StringBuffer    lsqmanZones = new StringBuffer();
        
        // Sorted by iterative USDD
        sorted = sortByDD(ca1, ca2, usdd[nIter], labels);
        for(i = 0; i < sorted.length; i++)
        {
            sca1[i] = (AtomState)sorted[i].ca1;
            sca2[i] = (AtomState)sorted[i].ca2;
            slabels[i] = sorted[i].label;
        }
        
        // Calculate RMSD for selected subset of atoms
        double[]    w       = new double[nResToSuperpos];
        for(i = 0; i < w.length; i++) w[i] = 1.0;
        
        int         off     = sorted.length - nResToSuperpos;
        SuperPoser  sp      = new SuperPoser(sca1, off, sca2, off, nResToSuperpos);
        Transform   R       = sp.superpos(w);
        double      rmsd    = sp.calcRMSD(R, w);        
        
        output.println("@group {sorted, iwusdd} dominant animate");
        output.println("@subgroup {diff dist}");
        output.println("@balllist {diff dist} nohighlight radius= 0.45");
        kin2dStdDD(output, sca1, sca2, slabels);
        lsqmanZones.append("Ca RMSD for IWUSDD = "+df.format(rmsd)+"\n");
        lsqmanZones.append("Selection covers "+nResToSuperpos+" residues ("
            +Math.round(100.0*nResToSuperpos/ca1.length)+"%)\n");
        lsqmanZones.append(makeLsqman(sorted, nResToSuperpos)+"\n\n\n");

        output.println("@text");
        output.println("LSQMAN script:");
        output.println("chain_mode original");
        output.println("hydrogens keep");
        output.println("hetatm keep");
        output.println("atom_types ca");
        output.println("read m1 "+file1);
        output.println("read m2 "+file2);
        output.println("### 'ex' line from below goes here ###");
        output.println("apply_operator m1 m2");
        output.println("write m2 superpos"+file2+"\n\n\n");
        output.println(lsqmanZones.toString());
        */
    }
//}}}

//{{{ plotMagnitudes
//##############################################################################
    void plotMagnitudes()
    {
        int i;
        // Kinemage 2 - the bar chart of vector magnitudes
        output.println("@kinemage 2");
        output.println("@title {per-res DD}");
        for(i = 0; i < nIter; i++)
        {
            output.println("@group {rd "+i+"} dominant animate");
            output.println("@subgroup {iterative-weighted}");
            output.println("@vectorlist {vdd-average (1->2, 2->1)} off master= {vector} color= red");
            kin1dVDD(output, labels, vdd_w[i+1], (i+1)/10.0);
            output.println("@vectorlist {2.5*av unsigned sum} master= {unsigned sum} color= yellow");
            kin1dVDD(output, labels, usdd[i+1], -(i+1)/10.0);
        }
    }
//}}}

//{{{ plotVectors3D
//##############################################################################
    void plotVectors3D()
    {
        int i;
        // Kinemage 3 - the 3-D plot of vectors on structures
        output.println("@kinemage 3");
        output.println("@title {vector DD}");
        for(i = 0; i < nIter; i++)
        {
            output.println("@group {rd "+i+"} dominant animate");
            output.println("@subgroup {iterative-weighted}");
            output.println("@vectorlist {1->2} color= magenta master= {1->2}");
            kin3dVDD(output, ca1, vdd12[i]);
            output.println("@vectorlist {2->1} color= magenta master= {2->1}");
            kin3dVDD(output, ca2, vdd21[i]);
        }
    }
//}}}

//{{{ plotLesk
//##############################################################################
    void plotLesk()
    {
        int i, len = ca1.length;
        DecimalFormat df = new DecimalFormat("0.0###");
        
        // Perform the Lesk sieve and IWUSDD sort
        SortItem[]  sortedL = sortByLeskSieve(ca1, ca2, labels);
        SortItem[]  sortedD = sortByDD(ca1, ca2, usdd[nIter], labels);
        AtomState[] sca1    = new AtomState[len];
        AtomState[] sca2    = new AtomState[len];
        String[]    slabels = new String[len];
        for(i = 0; i < len; i++)
        {
            sca1[i] = (AtomState)sortedD[i].ca1;
            sca2[i] = (AtomState)sortedD[i].ca2;
            slabels[i] = sortedD[i].label;
        }

        // Calculate RMSD for selected subset of atoms
        // We'll use these in a minute to get rmsd for all IWUSDD subsets
        double[]    w       = new double[len];
        for(i = 0; i < w.length; i++) w[i] = 1.0;
        int         off     = len - nResToSuperpos;
        SuperPoser  sp      = new SuperPoser(sca1, off, sca2, off, nResToSuperpos);
        Transform   R       = sp.superpos(w);
        double      rmsd    = sp.calcRMSD(R, w);        

        // Kinemage 4 - the Lesk plot of RMSD vs. # of residues included
        output.println("@kinemage 4");
        output.println("@title {Lesk plot}");
        output.println("@text");
        output.println("LSQMAN script:");
        output.println("chain_mode original");
        output.println("hydrogens keep");
        output.println("hetatm keep");
        output.println("atom_types ca");
        output.println("read m1 "+file1);
        output.println("read m2 "+file2);
        output.println("### 'ex' line from below goes here ###");
        output.println("apply_operator m1 m2");
        output.println("write m2 superpos"+file2);
        output.println();
        output.println();
        output.println("Ca RMSD for Lesk's sieve = "
            +df.format(sortedL[sortedL.length-nResToSuperpos].score));
        output.println("Selection covers "+nResToSuperpos+" residues ("
            +Math.round(100.0*nResToSuperpos/ca1.length)+"%)");
        output.println(makeLsqman(sortedL, nResToSuperpos));
        output.println();
        output.println();
        output.println("Ca RMSD for IWUSDD = "+df.format(rmsd));
        output.println("Selection covers "+nResToSuperpos+" residues ("
            +Math.round(100.0*nResToSuperpos/ca1.length)+"%)");
        output.println(makeLsqman(sortedD, nResToSuperpos));
        
        /* Output of all atoms and RMSDs for Lesk's sieve * /
        for(i = 0 ; i < len; i++)
        {
            SortItem si = sortedL[i];
            output.println("rmsd "+df.format(si.score)+" for "+(len-i)+" res; worst="+si.label);
        }
        /* Output of all atoms and RMSDs for Lesk's sieve */
        
        // Figure horizontal spacing to give square plot
        double hspace = sortedL[0].score / len;
        output.println("@vectorlist {IWUSDD vs. rmsd} color= sky");
        for(i = 0; i < len; i++)
        {
            sp.reset(sca1, i, sca2, i, len-i);
            R           = sp.superpos(w);
            rmsd        = sp.calcRMSD(R, w);        
            SortItem si = sortedD[i];
            output.println("{rmsd "+df.format(rmsd)+" for "+(len-i)+" res; worst="+si.label+"} "
                +df.format((len-i-1)*hspace)+" "+df.format(rmsd)+" 0.0");
        }
        
        output.println("@vectorlist {Lesk plot} color= sea");
        for(i = 0; i < len; i++)
        {
            SortItem si = sortedL[i];
            output.println("{rmsd "+df.format(si.score)+" for "+(len-i)+" res; worst="+si.label+"} "
                +df.format((len-i-1)*hspace)+" "+df.format(si.score)+" 0.0");
        }
        
        // Simple axes
        output.println("@vectorlist {axes} color= white");
        output.println("{} 0 0 0 {} "+df.format(i*hspace)+" 0 0 {} "
            +df.format(i*hspace)+" "+df.format(sortedL[0].score)+" 0"); 
    }
//}}}

//{{{ initData
//##############################################################################
    void initData() throws IOException
    {
        // Make arrays of C-alphas from input PDB files
        model1  = loadModel(input1);
        ca1     = loadAtomStates(model1);
        model2  = loadModel(input2);
        ca2     = loadAtomStates(model2);
        if(ca1.length != ca2.length)
            throw new IllegalArgumentException("Selections must have same number of C-alphas");
        
        // Get residue names out
        int i, j;
        labels = new String[ca1.length];
        for(i = 0; i < ca1.length; i++) labels[i] = ca1[i].getResidue().toString();
        
        // Calculate iterative rounds of both vector diff dists
        // starting from a flat initial weighting.
        vdd12   = new Triple[nIter][];
        vdd21   = new Triple[nIter][];
        vdd_w   = new double[nIter+1][ca1.length];              // weights
        for(i = 0; i < vdd_w[0].length; i++) vdd_w[0][i] = 1.0; // all start at 1
        for(i = 0; i < nIter; i++)
        {
            // Compute vdd
            vdd12[i] = computeVDD(ca1, ca2, vdd_w[i]);
            vdd21[i] = computeVDD(ca2, ca1, vdd_w[i]);
            // Compute a new set of weights from the average length of vdd12 and vdd21
            for(j = 0; j < vdd_w[i+1].length; j++)
                vdd_w[i+1][j] = (vdd12[i][j].mag() + vdd21[i][j].mag()) / 2.0;
        }
        
        // Calculate the same thing for the unsigned-sum diff dist
        usdd    = new double[nIter+1][ca1.length];              // weights
        for(i = 0; i < usdd[0].length; i++) usdd[0][i] = 1.0;   // all start at 1
        for(i = 0; i < nIter; i++)
        {
            usdd[i+1] = computeUSDD(ca1, ca2, usdd[i]);
        }
        
        // Establish number of residues to be used for superpositioning
        if(fracToSuperpos > 0)
        {
            if(fracToSuperpos > 1 && fracToSuperpos <= 100) // specified as a percentage
                nResToSuperpos = (int)Math.round(fracToSuperpos/100.0 * ca1.length);
            else if(fracToSuperpos <= 1)                    // specified as a fraction
                nResToSuperpos = (int)Math.round(fracToSuperpos * ca1.length);
        }
        if(nResToSuperpos < 3)
            nResToSuperpos = ca1.length; //(int)Math.round(0.666667*ca1.length);
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        initData();
        
        plotDiffDist();
        plotMagnitudes();
        plotVectors3D();
        plotLesk();

        try { input1.close(); } catch(IOException ex) {}
        try { input2.close(); } catch(IOException ex) {}
        output.close();
    }

    public static void main(String[] args)
    {
        Suppose mainprog = new Suppose();
        try
        {
            mainprog.showHelp(false);
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
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.err.println();
            System.err.println("*** I/O error: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("Suppose.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Suppose.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.Suppose");
        System.err.println("Copyright (C) 2003 by Ian W. Davis. All rights reserved.");
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
        File f = new File(arg);
        try
        {
            if(f.exists())
            {
                if(input1 == null)
                {
                    file1 = f;
                    input1 = new BufferedInputStream(new FileInputStream(f));
                }
                else if(input2 == null)
                {
                    file2 = f;
                    input2 = new BufferedInputStream(new FileInputStream(f));
                }
                else
                    output = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
            }
            else if(input1 != null && input2 != null)
                output = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
            else
                System.err.println("*** WARNING: file '"+f+"' was not found");
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.err.println("*** WARNING: file '"+f+"' was not found");
        }
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-n"))
        {
            try { nResToSuperpos = Integer.parseInt(param); }
            catch(NumberFormatException ex)
            { System.err.println("*** '"+param+"' is not a number"); }
        }
        else if(flag.equals("-f"))
        {
            try { fracToSuperpos = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { System.err.println("*** '"+param+"' is not a number"); }
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}

//{{{ empty
//##############################################################################
//}}}
}//class

