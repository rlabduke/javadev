// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>Alignment</code> represents an alignment of two sequences (which may
* then form the basis for aligning two structures).
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Mar  2 15:41:13 EST 2007
*/
public class Alignment //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ INTERFACE: Scorer
//##############################################################################
    /**
    * A scoring function to be used with the alignment algorithms in this class.
    * Since it might be convenient to align Residue, Atoms, or AtomStates,
    * this interface just deals with generic Objects.
    */
    public interface Scorer
    {
        /**
        * Scores the alignment of things A and B with each other.
        * High scores are favorable; low scores are bad.
        * A or B should never be null.
        */
        public double score(Object a, Object b);
        
        /**
        * Cost of starting a gap, across from thing A.
        * Either open_gap() or extend_gap() is applied at a position, but not both.
        * Gap penalities are usually constant, and I don't know
        * what a residue-specific gap penalty would do to the algorithm's
        * correctness guarantee!
        */
        public double open_gap(Object a);
        
        /**
        * Cost of extending a gap, across from thing A.
        * Either open_gap() or extend_gap() is applied at a position, but not both.
        * Gap penalities are usually constant, and I don't know
        * what a residue-specific gap penalty would do to the algorithm's
        * correctness guarantee!
        */
        public double extend_gap(Object a);
    }
//}}}

//{{{ INTERFACE: Aligner
//##############################################################################
    /**
    * An alignment algorithm.
    * Provided so that the static alignment methods in this class
    * can easily be passed as "function objects" to other routines.
    */
    public interface Aligner
    {
        /**
        * Performs some kind of alignment, with gaps represented by nulls.
        * In the case of non-global alignments, no all input objects are
        * guaranteed to be represented in the output alignment.
        * @param objsA      the "source" set of objects, suitable for scoring by scorer
        * @param objsB      the "target" set of objects, suitable for scoring by scorer
        * @param scorer     a Scorer suited for whatever type of objects are in A and B
        */
        public Alignment align(Object[] objsA, Object[] objsB, Scorer scorer);
    }
//}}}

//{{{ CLASS: NeedlemanWunsch
//##############################################################################
    /** Uses the algorithm of Needleman and Wunsch to do a gapped global alignment. */
    public static class NeedlemanWunsch implements Aligner
    {
        public Alignment align(Object[] objsA, Object[] objsB, Scorer scorer)
        { return needlemanWunsch(objsA, objsB, scorer); }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The aligned input objects, with gaps represented by nulls */
    public Object[] a, b;
//}}}

//{{{ Constructor(s)
//##############################################################################
    private Alignment()
    {
        super();
    }
//}}}

//{{{ score
//##############################################################################
    /**
    * Score the generated alignment using the provided Scorer.
    */
    public double score(Scorer scorer)
    {
        double score = 0;
        // A and B should be the same length;  this will generate an exception if not
        for(int i = 0; i < Math.max(a.length, b.length); i++)
        {
            // A or B may be null, but not both
            if(a[i] == null)
            {
                if(i == 0 || a[i-1] != null) score += scorer.open_gap(b[i]);
                else score += scorer.extend_gap(b[i]);
            }
            else if(b[i] == null)
            {
                if(i == 0 || b[i-1] != null) score += scorer.open_gap(a[i]);
                else score += scorer.extend_gap(a[i]);
            }
            else score += scorer.score(a[i], b[i]);
        }
        return score;
    }
//}}}

//{{{ needlemanWunsch
//##############################################################################
    /**
    * Uses the algorithm of Needleman and Wunsch to do a gapped global alignment.
    * @param objsA      the "source" set of objects, suitable for scoring by scorer
    * @param objsB      the "target" set of objects, suitable for scoring by scorer
    * @param scorer     a Scorer suited for whatever type of objects are in A and B
    */
    static public Alignment needlemanWunsch(Object[] objsA, Object[] objsB, Scorer scorer)
    {
        Object[] a = objsA, b = objsB; // for ease of typing
        
        // As the array is filled in, each box i,j will contain the maximum
        // possible score for any alignment of the first i symbols in A with
        // the first j symbols in B.
        double[][] nw = new double[ a.length+1 ][ b.length+1 ];
        
        // For backtracking and generating the alignment
        int[][] hist = new int[ a.length+1 ][ b.length+1 ];
        final int GAP_IN_A = 1, GAP_IN_B = 2, NO_GAP = 4;
        
        // First cell -- two zero-length strings
        nw[0][0] = 0;
        
        // First row
        // Fill in with cumulative gap penalty
        for(int i = 0; i < a.length; i++)
        {
            nw[i+1][0] = nw[i][0] + (i == 0 ? scorer.open_gap(a[i]) : scorer.extend_gap(a[i]));
            hist[i+1][0] = GAP_IN_B;
        }
        
        // First column
        for(int j = 0; j < b.length; j++)
        {
            nw[0][j+1] = nw[0][j] + (j == 0 ? scorer.open_gap(b[j]) : scorer.extend_gap(b[j]));
            hist[0][j+1] = GAP_IN_A;
        }
        
        // Rest of table
        for(int i = 0; i < a.length; i++)
        {
            for(int j = 0; j < b.length; j++)
            {
                double s1 = nw[i  ][j+1] + (hist[i  ][j+1] == GAP_IN_B ? scorer.extend_gap(a[i]) : scorer.open_gap(a[i]));
                double s2 = nw[i+1][j  ] + (hist[i+1][j  ] == GAP_IN_A ? scorer.extend_gap(b[j]) : scorer.open_gap(b[j]));
                double s3 = nw[i  ][j  ] + scorer.score(a[i], b[j]);
                // If everything is equal, prefer no gap
                if(s3 >= s1 && s3 >= s2)
                {
                    nw[i+1][j+1] = s3;
                    hist[i+1][j+1] = NO_GAP;
                }
                else if(s1 >= s2)
                {
                    nw[i+1][j+1] = s1;
                    hist[i+1][j+1] = GAP_IN_B;
                }
                else // s2 is greatest
                {
                    nw[i+1][j+1] = s2;
                    hist[i+1][j+1] = GAP_IN_A;
                }
            }
        }
        
        // Backtrack to generate the optimal alignment
        LinkedList aa = new LinkedList(), bb = new LinkedList();
        int i = a.length-1, j = b.length-1;
        while(i >= 0 && j >= 0)
        {
            if(hist[i+1][j+1] == NO_GAP)
            {
                aa.addFirst(a[i]);
                bb.addFirst(b[j]);
                i--; j--;
            }
            else if(hist[i+1][j+1] == GAP_IN_B)
            {
                aa.addFirst(a[i]);
                bb.addFirst(null);
                i--;
            }
            else // hist[i+1][j+1] == GAP_IN_A
            {
                aa.addFirst(null);
                bb.addFirst(b[j]);
                j--;
            }
        }
        
        // Either aa or bb now has nothing but gaps to be added;
        // i.e., only one of the two loops below will execute (at most).
        while(i >= 0)
        {
            aa.addFirst(a[i]);
            bb.addFirst(null);
            i--;
        }
        while(j >= 0)
        {
            aa.addFirst(null);
            bb.addFirst(b[j]);
            j--;
        }
        
        Alignment align = new Alignment();
        align.a = aa.toArray();
        align.b = bb.toArray();
        return align;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ alignChains, next_permutation, reverse
//##############################################################################
    /**
    * Creates an alignment that does not cross chain boundaries.
    * Run time is factorial in the number of chains (!).
    * Alignments that have a net score less than zero are excluded.
    * @param chainsA    a Collection of Collections of Residues, giving the chains of A
    * @param chainsB    a Collection of Collections of Residues, giving the chains of B
    * @param aligner    an algorithm for sequence alignment, such as Needleman-Wunsch.
    * @param scorer     a Scorer suited for whatever type of objects are in A and B
    */
    public static Alignment alignChains(Collection chainsA, Collection chainsB, Aligner aligner, Scorer scorer)
    {
        // Score each chain against each other chain
        Alignment[][] alignments = new Alignment[ chainsA.size() ][ chainsB.size() ];
        double[][] scores = new double[ chainsA.size() ][ chainsB.size() ];
        int idxA = 0;
        for(Iterator itrA = chainsA.iterator(); itrA.hasNext(); idxA++)
        {
            Object[] objsA = ((Collection) itrA.next()).toArray();
            int idxB = 0;
            for(Iterator itrB = chainsB.iterator(); itrB.hasNext(); idxB++)
            {
                Object[] objsB = ((Collection) itrB.next()).toArray();
                alignments[idxA][idxB]  = aligner.align(objsA, objsB, scorer);
                scores[idxA][idxB]      = alignments[idxA][idxB].score(scorer);
                //System.err.printf("%8.1f ", new Object[] { new Double(scores[idxA][idxB]) });
            }
            //System.err.println();
        }
        // Try all permutations of pairing up the chains,
        // pick the one with the maximum sum-of-scores.
        int a = chainsA.size(), b = chainsB.size();
        int min = Math.min(a, b), max = Math.max(a, b);
        double best_score = Double.NEGATIVE_INFINITY;
        int[] x = new int[max], best_x = null;
        for(int i = 0; i < max; i++) x[i] = Math.min(i, min); // [0 1 2 ... min-1 min min min]
        while(true)
        {
            double total_score = 0;
            for(int i = 0; i < max; i++)
            {
                if(x[i] >= min) // this is an X-vs-null pairing, no score contribution
                    continue;
                else if(a >= b) // A has more chains
                    total_score += scores[ i ][ x[i] ];
                else // B has more chains
                    total_score += scores[ x[i] ][ i ];
            }
            if(best_x == null || total_score > best_score)
            {
                best_score = total_score;
                best_x = (int[]) x.clone();
            }
            if(!next_permutation(x)) break;
        }
        // Construct the final alignment
        ArrayList aa = new ArrayList(), bb = new ArrayList();
        x = best_x;
        for(int i = 0; i < max; i++)
        {
            if(x[i] >= min) // this is an X-vs-null pairing, no score contribution
                continue;
            else if(a >= b) // A has more chains
            {
                if( scores[ i ][ x[i] ] <= 0 ) continue; // bad alignment, don't include
                aa.addAll(Arrays.asList( alignments[ i ][ x[i] ].a ));
                bb.addAll(Arrays.asList( alignments[ i ][ x[i] ].b ));
            }
            else // B has more chains
            {
                if( scores[ x[i] ][ i ] <= 0 ) continue; // bad alignment, don't include
                aa.addAll(Arrays.asList( alignments[ x[i] ][ i ].a ));
                bb.addAll(Arrays.asList( alignments[ x[i] ][ i ].b ));
            }
        }
        
        Alignment align = new Alignment();
        align.a = aa.toArray();
        align.b = bb.toArray();
        return align;
    }
    
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

//{{{ main (for testing)
//##############################################################################
    public static void main(String[] args)
    {
        //int[] x = {1, 2, 3, 3};
        //do
        //{
        //    for(int i = 0; i < x.length; i++)
        //        System.out.print(" "+x[i]);
        //    System.out.println();
        //} while( next_permutation(x) );
        //if(true) return;
        
        Alignment align = needlemanWunsch(
            makeCharSet(args[0]),
            makeCharSet(args[1]),
            new Scorer()
            {
                public double score(Object a, Object b)
                {
                    if(a.equals(b)) return 1;
                    else return 0;
                }
                public double open_gap(Object a)
                { return extend_gap(a); }
                public double extend_gap(Object a)
                { return -1; }
            }
        );
        printAlignedSet(align.a);
        printAlignedSet(align.b);
    }
    
    static Object[] makeCharSet(String chars)
    {
        ArrayList list = new ArrayList();
        for(int i = 0; i < chars.length(); i++)
            list.add(chars.substring(i, i+1));
        return list.toArray();
    }
    
    static void printAlignedSet(Object[] aligned)
    {
        for(int i = 0; i < aligned.length; i++)
        {
            if(aligned[i] == null) System.out.print("-");
            else System.out.print(aligned[i]);
        }
        System.out.println();
    }
//}}}
}//class

