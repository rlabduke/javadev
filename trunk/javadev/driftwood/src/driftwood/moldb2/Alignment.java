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
        * If A or B is null, then this function should return
        * the cost of leaving a gap.
        * Gap penalities are usually constant, and I don't know
        * what a residue-specific gap penalty would do to the algorithm's
        * correctness guarantee!
        */
        public int score(Object a, Object b);
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

/* doesn't get leading gaps right
//{{{ needlemanWunsch
//##############################################################################
    /**
    * Uses the algorithm of Needleman and Wunsch to do a gapped global alignment.
    * @param objsA      the "source" set of objects, suitable for scoring by scorer
    * @param objsB      the "target" set of objects, suitable for scoring by scorer
    * @param scorer     a Scorer suited for whatever type of objects are in A and B
    * /
    static public Alignment needlemanWunsch(Object[] objsA, Object[] objsB, Scorer scorer)
    {
        Object[] a = objsA, b = objsB; // for ease of typing
        
        // As the array is filled in, each box i,j will contain the maximum
        // possible score for any alignment of the first i+1 symbols in A with
        // the first j+1 symbols in B.
        int[][] nw = new int[ a.length ][ b.length ];
        
        // For backtracking and generating the alignment
        int[][] hist = new int[ a.length ][ b.length ];
        final int GAP_IN_A = 1, GAP_IN_B = 2, NO_GAP = 4;
        
        // First cell
        nw[0][0] = scorer.score(a[0], b[0]);
        hist[0][0] = NO_GAP;
        
        // First row
        // First fill in with cumulative gap penalty
        for(int i = 1; i < a.length; i++)
            nw[i][0] = nw[i-1][0] + scorer.score(a[i], null);
        // Then backtrack with match score + leading gap pen.
        for(int i = a.length-1; i > 0; i--)
        {
            nw[i][0] = nw[i-1][0] + scorer.score(a[i], b[0]);
            hist[i][0] = NO_GAP;
        }
        
        // First column
        for(int j = 1; j < b.length; j++)
            nw[0][j] = nw[0][j-1] + scorer.score(null, b[j]);
        for(int j = b.length-1; j > 0; j--)
        {
            nw[0][j] = nw[0][j-1] + scorer.score(a[0], b[j]);
            hist[0][j] = NO_GAP;
        }
        
        // Rest of table
        for(int i = 1; i < a.length; i++)
        {
            for(int j = 1; j < b.length; j++)
            {
                int s1 = nw[i-1][j] + scorer.score(a[i], null);
                int s2 = nw[i][j-1] + scorer.score(null, b[j]);
                int s3 = nw[i-1][j-1] + scorer.score(a[i], b[j]);
                if(s1 >= s2 && s1 >= s3)
                {
                    nw[i][j] = s1;
                    hist[i][j] = GAP_IN_B;
                }
                else if(s2 >= s3)
                {
                    nw[i][j] = s2;
                    hist[i][j] = GAP_IN_A;
                }
                else // s3 is greatest
                {
                    nw[i][j] = s3;
                    hist[i][j] = NO_GAP;
                }
            }
        }
        
        // Backtrack to generate the optimal alignment
        LinkedList aa = new LinkedList(), bb = new LinkedList();
        int i = a.length-1, j = b.length-1;
        while(i >= 0 && j >= 0)
        {
            if(hist[i][j] == NO_GAP)
            {
                aa.addFirst(a[i]);
                bb.addFirst(b[j]);
                i--; j--;
            }
            else if(hist[i][j] == GAP_IN_B)
            {
                aa.addFirst(a[i]);
                bb.addFirst(null);
                i--;
            }
            else // hist[i][j] == GAP_IN_A
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
doesn't get leading gaps right */

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
        int[][] nw = new int[ a.length+1 ][ b.length+1 ];
        
        // For backtracking and generating the alignment
        int[][] hist = new int[ a.length+1 ][ b.length+1 ];
        final int GAP_IN_A = 1, GAP_IN_B = 2, NO_GAP = 4;
        
        // First cell -- two zero-length strings
        nw[0][0] = 0;
        
        // First row
        // Fill in with cumulative gap penalty
        for(int i = 0; i < a.length; i++)
            nw[i+1][0] = nw[i][0] + scorer.score(a[i], null);
        
        // First column
        for(int j = 0; j < b.length; j++)
            nw[0][j+1] = nw[0][j] + scorer.score(null, b[j]);
        
        // Rest of table
        for(int i = 0; i < a.length; i++)
        {
            for(int j = 0; j < b.length; j++)
            {
                int s1 = nw[i  ][j+1] + scorer.score(a[i], null);
                int s2 = nw[i+1][j  ] + scorer.score(null, b[j]);
                int s3 = nw[i  ][j  ] + scorer.score(a[i], b[j]);
                if(s1 >= s2 && s1 >= s3)
                {
                    nw[i+1][j+1] = s1;
                    hist[i+1][j+1] = GAP_IN_B;
                }
                else if(s2 >= s3)
                {
                    nw[i+1][j+1] = s2;
                    hist[i+1][j+1] = GAP_IN_A;
                }
                else // s3 is greatest
                {
                    nw[i+1][j+1] = s3;
                    hist[i+1][j+1] = NO_GAP;
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

//{{{ main (for testing)
//##############################################################################
    public static void main(String[] args)
    {
        Alignment align = needlemanWunsch(
            makeCharSet(args[0]),
            makeCharSet(args[1]),
            new Scorer()
            {
                public int score(Object a, Object b)
                {
                    if(a == null || b == null) return -1;
                    else if(a.equals(b)) return 1;
                    else return 0;
                }
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

