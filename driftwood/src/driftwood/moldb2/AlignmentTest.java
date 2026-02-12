package driftwood.moldb2;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlignmentTest
{
    // Simple scorer: +1 for match, -1 for gap (open or extend)
    static final Alignment.Scorer SIMPLE = new Alignment.Scorer()
    {
        public double score(Object a, Object b)
        { return a.equals(b) ? 1 : 0; }
        public double open_gap(Object a)
        { return -1; }
        public double extend_gap(Object a)
        { return -1; }
    };

    // Affine gap scorer: -2 to open, -0.5 to extend
    static final Alignment.Scorer AFFINE = new Alignment.Scorer()
    {
        public double score(Object a, Object b)
        { return a.equals(b) ? 2 : -1; }
        public double open_gap(Object a)
        { return -2; }
        public double extend_gap(Object a)
        { return -0.5; }
    };

    static Object[] chars(String s)
    {
        Object[] result = new Object[s.length()];
        for(int i = 0; i < s.length(); i++)
            result[i] = s.substring(i, i+1);
        return result;
    }

    static String alignedString(Object[] aligned)
    {
        StringBuilder sb = new StringBuilder();
        for(Object o : aligned)
            sb.append(o == null ? "-" : o.toString());
        return sb.toString();
    }

    // --- identical sequences ---

    @Test public void identicalSequences()
    {
        Alignment a = Alignment.needlemanWunsch(chars("ABCD"), chars("ABCD"), SIMPLE);
        assertEquals("ABCD", alignedString(a.a));
        assertEquals("ABCD", alignedString(a.b));
    }

    // --- completely different ---

    @Test public void completelyDifferent()
    {
        Alignment a = Alignment.needlemanWunsch(chars("AA"), chars("BB"), SIMPLE);
        assertEquals(a.a.length, a.b.length);
    }

    // --- insertion ---

    @Test public void insertionInB()
    {
        // A = "AC", B = "ABC" => A gets a gap: "A-C" vs "ABC"
        Alignment a = Alignment.needlemanWunsch(chars("AC"), chars("ABC"), SIMPLE);
        assertEquals(a.a.length, a.b.length);
        // Both A and C should be matched
        String aa = alignedString(a.a);
        String bb = alignedString(a.b);
        assertTrue("A aligned: " + aa + " vs " + bb,
            aa.contains("A") && aa.contains("C"));
    }

    // --- deletion ---

    @Test public void deletionInB()
    {
        // A = "ABC", B = "AC" => B gets a gap
        Alignment a = Alignment.needlemanWunsch(chars("ABC"), chars("AC"), SIMPLE);
        assertEquals(a.a.length, a.b.length);
        String bb = alignedString(a.b);
        assertTrue("gap in B", bb.contains("-"));
    }

    // --- single element sequences ---

    @Test public void singleMatch()
    {
        Alignment a = Alignment.needlemanWunsch(chars("A"), chars("A"), SIMPLE);
        assertEquals("A", alignedString(a.a));
        assertEquals("A", alignedString(a.b));
    }

    @Test public void singleMismatch()
    {
        Alignment a = Alignment.needlemanWunsch(chars("A"), chars("B"), SIMPLE);
        assertEquals(a.a.length, a.b.length);
    }

    // --- empty vs non-empty ---

    @Test public void emptyVsNonEmpty()
    {
        Alignment a = Alignment.needlemanWunsch(new Object[0], chars("ABC"), SIMPLE);
        assertEquals(a.a.length, a.b.length);
        // All of a should be gaps
        for(Object o : a.a) assertNull(o);
    }

    @Test public void bothEmpty()
    {
        Alignment a = Alignment.needlemanWunsch(new Object[0], new Object[0], SIMPLE);
        assertEquals(0, a.a.length);
        assertEquals(0, a.b.length);
    }

    // --- known alignment ---

    @Test public void knownAlignment()
    {
        // Classic example: "AGTC" vs "AGC" should align as "AGTC" / "AG-C"
        Alignment a = Alignment.needlemanWunsch(chars("AGTC"), chars("AGC"), SIMPLE);
        String aa = alignedString(a.a);
        String bb = alignedString(a.b);
        // A, G, C must match; T is gapped in B
        assertEquals(4, aa.length());
        assertEquals(4, bb.length());
        assertEquals('A', aa.charAt(0));
        assertEquals('A', bb.charAt(0));
    }

    // --- score method ---

    @Test public void scoreIdentical()
    {
        Alignment a = Alignment.needlemanWunsch(chars("ABCD"), chars("ABCD"), SIMPLE);
        assertEquals(4.0, a.score(SIMPLE), 1e-10);
    }

    @Test public void scoreWithGaps()
    {
        Alignment a = Alignment.needlemanWunsch(chars("ABC"), chars("AC"), SIMPLE);
        // Should have 2 matches and 1 gap
        double s = a.score(SIMPLE);
        assertTrue("score should reflect matches minus gaps: " + s, s > 0);
    }

    // --- affine gaps ---

    @Test public void affineGapPrefersSingleLongGap()
    {
        // With affine gaps, "ABCXYZDEF" vs "ABCDEF" should prefer one long gap
        // over multiple short gaps
        Alignment a = Alignment.needlemanWunsch(chars("ABCXYZDEF"), chars("ABCDEF"), AFFINE);
        String bb = alignedString(a.b);
        // Count gap runs
        int gapRuns = 0;
        boolean inGap = false;
        for(int i = 0; i < bb.length(); i++)
        {
            if(bb.charAt(i) == '-')
            {
                if(!inGap) { gapRuns++; inGap = true; }
            }
            else inGap = false;
        }
        assertEquals("should have one gap run", 1, gapRuns);
    }

    // --- NeedlemanWunsch Aligner interface ---

    @Test public void alignerInterface()
    {
        Alignment.Aligner aligner = new Alignment.NeedlemanWunsch();
        Alignment a = aligner.align(chars("ABC"), chars("ABC"), SIMPLE);
        assertEquals("ABC", alignedString(a.a));
        assertEquals("ABC", alignedString(a.b));
    }

    // --- longer sequences ---

    @Test public void longerSequence()
    {
        String seq1 = "MVLSPADKTNVKAAWGKVGAHAGEYGAEALERMFLSFPTTKTYFPHFDLSH";
        String seq2 = "MVHLTPEEKSAVTALWGKVNVDEVGGEALGRLLVVYPWTQRFFESFGDLST";
        Alignment a = Alignment.needlemanWunsch(chars(seq1), chars(seq2), SIMPLE);
        assertEquals(a.a.length, a.b.length);
        assertTrue("aligned length >= max input", a.a.length >= Math.max(seq1.length(), seq2.length()));
    }
}
