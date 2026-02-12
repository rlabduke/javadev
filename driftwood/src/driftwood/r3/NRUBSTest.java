package driftwood.r3;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class NRUBSTest
{
    static final double EPS = 1e-8;
    NRUBS nrubs;

    @Before public void setUp()
    { nrubs = new NRUBS(); }

    // --- spline basic properties ---

    @Test public void splineOutputLength()
    {
        // G guide points, N intervals => N*(G-3) + 1 output points
        Tuple3[] guides = makeLinear(6);
        int N = 4;
        Tuple3[] result = nrubs.spline(guides, N);
        assertEquals(N * (guides.length - 3) + 1, result.length);
    }

    @Test public void splineMinimumGuidePoints()
    {
        // 4 guide points = 1 segment
        Tuple3[] guides = makeLinear(4);
        Tuple3[] result = nrubs.spline(guides, 10);
        assertEquals(11, result.length); // 10*(4-3)+1
    }

    @Test public void splineFiveGuidePoints()
    {
        Tuple3[] guides = makeLinear(5);
        Tuple3[] result = nrubs.spline(guides, 8);
        assertEquals(17, result.length); // 8*(5-3)+1
    }

    // --- spline with colinear guide points ---

    @Test public void splineColinearStaysOnLine()
    {
        // Points along the X axis: spline should stay near the line
        Tuple3[] guides = new Tuple3[] {
            new Triple(0, 0, 0),
            new Triple(1, 0, 0),
            new Triple(2, 0, 0),
            new Triple(3, 0, 0),
            new Triple(4, 0, 0),
            new Triple(5, 0, 0)
        };
        Tuple3[] result = nrubs.spline(guides, 10);
        for (Tuple3 pt : result)
        {
            assertEquals(0.0, pt.getY(), EPS);
            assertEquals(0.0, pt.getZ(), EPS);
        }
    }

    @Test public void splineColinearMonotonic()
    {
        // X should be monotonically increasing for linear guide points
        Tuple3[] guides = makeLinear(6);
        Tuple3[] result = nrubs.spline(guides, 10);
        for (int i = 1; i < result.length; i++)
            assertTrue("X should increase: pt " + (i-1) + " to " + i,
                result[i].getX() >= result[i-1].getX() - EPS);
    }

    // --- spline stays in convex hull ---

    @Test public void splineWithinConvexHull()
    {
        // B-spline segments lie within convex hull of their 4 control points
        Tuple3[] guides = new Tuple3[] {
            new Triple(0, 0, 0),
            new Triple(1, 2, 0),
            new Triple(3, -1, 0),
            new Triple(4, 1, 0)
        };
        double minX = 0, maxX = 4, minY = -1, maxY = 2;
        Tuple3[] result = nrubs.spline(guides, 20);
        for (Tuple3 pt : result)
        {
            assertTrue(pt.getX() >= minX - EPS);
            assertTrue(pt.getX() <= maxX + EPS);
            assertTrue(pt.getY() >= minY - EPS);
            assertTrue(pt.getY() <= maxY + EPS);
        }
    }

    // --- B coefficients ---

    @Test public void bCoeffsSumToOne()
    {
        // B-spline basis functions should sum to 1 at any parameter value
        // (partition of unity property)
        Tuple3[] guides = makeLinear(4);
        // We can verify indirectly: for all-ones guide points, output should be all-ones
        Tuple3[] allOnes = new Tuple3[] {
            new Triple(1, 1, 1),
            new Triple(1, 1, 1),
            new Triple(1, 1, 1),
            new Triple(1, 1, 1)
        };
        Tuple3[] result = nrubs.spline(allOnes, 10);
        for (Tuple3 pt : result)
        {
            assertEquals(1.0, pt.getX(), EPS);
            assertEquals(1.0, pt.getY(), EPS);
            assertEquals(1.0, pt.getZ(), EPS);
        }
    }

    // --- spline endpoints ---

    @Test public void splineDoesNotPassThroughGuides()
    {
        // B-splines generally don't interpolate their control points
        Tuple3[] guides = new Tuple3[] {
            new Triple(0, 0, 0),
            new Triple(1, 3, 0),
            new Triple(3, -2, 0),
            new Triple(4, 0, 0)
        };
        Tuple3[] result = nrubs.spline(guides, 10);
        // First output point should NOT be exactly at guide[0]
        // (it's influenced by all 4 control points)
        assertTrue(result[0].getX() > 0.1); // should be pulled toward interior
    }

    // --- caching ---

    @Test public void splineCachingProducesSameResults()
    {
        Tuple3[] guides = makeLinear(6);
        Tuple3[] result1 = nrubs.spline(guides, 5);
        Tuple3[] result2 = nrubs.spline(guides, 5); // should use cached B
        assertEquals(result1.length, result2.length);
        for (int i = 0; i < result1.length; i++)
        {
            assertEquals(result1[i].getX(), result2[i].getX(), EPS);
            assertEquals(result1[i].getY(), result2[i].getY(), EPS);
            assertEquals(result1[i].getZ(), result2[i].getZ(), EPS);
        }
    }

    // --- 3D spline ---

    @Test public void spline3D()
    {
        // Helix-like guide points
        Tuple3[] guides = new Tuple3[] {
            new Triple(1, 0, 0),
            new Triple(0, 1, 1),
            new Triple(-1, 0, 2),
            new Triple(0, -1, 3),
            new Triple(1, 0, 4)
        };
        Tuple3[] result = nrubs.spline(guides, 10);
        // Z should be monotonically increasing
        for (int i = 1; i < result.length; i++)
            assertTrue(result[i].getZ() >= result[i-1].getZ() - EPS);
    }

    // --- spline segment override ---

    @Test public void splineSingleSegment()
    {
        Tuple3[] guides = makeLinear(6);
        Triple[] out = new Triple[11];
        for (int i = 0; i < out.length; i++) out[i] = new Triple();

        // Compute segment starting at guide index 1 (uses guides 1,2,3,4)
        nrubs.spline(guides, 1, out, 0, 10);
        // Output should be populated (not all zeros)
        boolean allZero = true;
        for (Triple t : out)
            if (t.getX() != 0) { allZero = false; break; }
        assertFalse(allZero);
    }

    // --- Helper ---

    private Tuple3[] makeLinear(int n)
    {
        Tuple3[] pts = new Tuple3[n];
        for (int i = 0; i < n; i++)
            pts[i] = new Triple(i, 0, 0);
        return pts;
    }
}
