package driftwood.r3;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.*;

public class BuilderTest
{
    static final double EPS = 1e-8;
    Builder builder;

    @Before public void setUp()
    { builder = new Builder(); }

    // --- construct4 ---

    @Test public void construct4Straight()
    {
        // A-B-C along X axis, extend straight: D should be at (4,0,0)
        Triple a = new Triple(0, 0, 0);
        Triple b = new Triple(1, 0, 0);
        Triple c = new Triple(2, 0, 0);
        // But A-B-C are colinear, making dihedral undefined.
        // Use a slightly off-axis A instead.
        a = new Triple(0, 0.001, 0);
        Triple d = builder.construct4(a, b, c, 1.0, 180, 0);
        // Should be approximately at (3,0,0)
        assertEquals(3.0, d.getX(), 0.01);
        assertEquals(0.0, d.getY(), 0.01);
        assertEquals(0.0, d.getZ(), 0.01);
    }

    @Test public void construct4RightAngle()
    {
        // Build D at 90 degrees from BC
        Triple a = new Triple(0, 1, 0);
        Triple b = new Triple(0, 0, 0);
        Triple c = new Triple(1, 0, 0);
        Triple d = builder.construct4(a, b, c, 1.0, 90, 0);
        // D should be at distance 1 from C, angle BCD = 90, dihedral ABCD = 0
        double dist = c.distance(d);
        assertEquals(1.0, dist, EPS);
        double angle = Triple.angle(b, c, d);
        assertEquals(90.0, angle, 0.1);
    }

    @Test public void construct4Length()
    {
        // Verify the length of C-D
        Triple a = new Triple(0, 1, 0);
        Triple b = new Triple(0, 0, 0);
        Triple c = new Triple(1, 0, 0);
        double targetLen = 2.5;
        Triple d = builder.construct4(a, b, c, targetLen, 109.5, 45);
        assertEquals(targetLen, c.distance(d), EPS);
    }

    @Test public void construct4DihedralSymmetry()
    {
        // Two constructions with opposite dihedrals should produce symmetric results
        Triple a = new Triple(0, 1, 0);
        Triple b = new Triple(0, 0, 0);
        Triple c = new Triple(1, 0, 0);
        Triple d1 = builder.construct4(a, b, c, 1.5, 109.5, 120);
        Triple d2 = builder.construct4(a, b, c, 1.5, 109.5, -120);
        // Same distance from C
        assertEquals(c.distance(d1), c.distance(d2), EPS);
        // Same X coordinate (dihedral rotates around BC axis = X axis)
        assertEquals(d1.getX(), d2.getX(), EPS);
    }

    @Test public void construct4TetrahedralAngle()
    {
        // Construct tetrahedral geometry (109.47 degrees)
        Triple a = new Triple(0, 1, 0);
        Triple b = new Triple(0, 0, 0);
        Triple c = new Triple(1, 0, 0);
        Triple d = builder.construct4(a, b, c, 1.0, 109.47, 60);
        double angle = Triple.angle(b, c, d);
        assertEquals(109.47, angle, 0.1);
    }

    // --- dock3on3 ---

    @Test public void dock3on3Identity()
    {
        // Docking identical points should give identity-like transform
        Triple p1 = new Triple(1, 0, 0);
        Triple p2 = new Triple(0, 1, 0);
        Triple p3 = new Triple(0, 0, 1);
        Transform t = builder.dock3on3(p1, p2, p3, p1, p2, p3);
        Triple out = new Triple();
        t.transform(p1, out);
        assertEquals(p1.getX(), out.getX(), EPS);
        assertEquals(p1.getY(), out.getY(), EPS);
        assertEquals(p1.getZ(), out.getZ(), EPS);
    }

    @Test public void dock3on3PureTranslation()
    {
        // Mobile points shifted by (10,0,0) relative to reference
        Triple r1 = new Triple(0, 0, 0), r2 = new Triple(1, 0, 0), r3 = new Triple(0, 1, 0);
        Triple m1 = new Triple(10, 0, 0), m2 = new Triple(11, 0, 0), m3 = new Triple(10, 1, 0);
        Transform t = builder.dock3on3(r1, r2, r3, m1, m2, m3);

        Triple out = new Triple();
        t.transform(m1, out);
        assertEquals(r1.getX(), out.getX(), EPS);
        assertEquals(r1.getY(), out.getY(), EPS);
        assertEquals(r1.getZ(), out.getZ(), EPS);

        t.transform(m2, out);
        assertEquals(r2.getX(), out.getX(), EPS);
        assertEquals(r2.getY(), out.getY(), EPS);
        assertEquals(r2.getZ(), out.getZ(), EPS);
    }

    @Test public void dock3on3Rotation()
    {
        // Reference in XY plane, mobile rotated 90 deg around Z
        Triple r1 = new Triple(0, 0, 0), r2 = new Triple(1, 0, 0), r3 = new Triple(0, 1, 0);
        Triple m1 = new Triple(0, 0, 0), m2 = new Triple(0, 1, 0), m3 = new Triple(-1, 0, 0);
        Transform t = builder.dock3on3(r1, r2, r3, m1, m2, m3);

        Triple out = new Triple();
        t.transform(m1, out);
        assertEquals(r1.getX(), out.getX(), EPS);
        assertEquals(r1.getY(), out.getY(), EPS);
        assertEquals(r1.getZ(), out.getZ(), EPS);

        t.transform(m2, out);
        assertEquals(r2.getX(), out.getX(), EPS);
        assertEquals(r2.getY(), out.getY(), EPS);
        assertEquals(r2.getZ(), out.getZ(), EPS);

        t.transform(m3, out);
        assertEquals(r3.getX(), out.getX(), EPS);
        assertEquals(r3.getY(), out.getY(), EPS);
        assertEquals(r3.getZ(), out.getZ(), EPS);
    }

    // --- signedArea2 ---

    @Test public void signedArea2Positive()
    {
        // CCW triangle: (0,0), (1,0), (0,1)
        double area = Builder.signedArea2(0, 0, 1, 0, 0, 1);
        assertTrue(area > 0);
        assertEquals(1.0, area, EPS); // parallelogram area = 1
    }

    @Test public void signedArea2Negative()
    {
        // CW triangle: (0,0), (0,1), (1,0)
        double area = Builder.signedArea2(0, 0, 0, 1, 1, 0);
        assertTrue(area < 0);
        assertEquals(-1.0, area, EPS);
    }

    @Test public void signedArea2Colinear()
    {
        // Colinear points
        double area = Builder.signedArea2(0, 0, 1, 1, 2, 2);
        assertEquals(0.0, area, EPS);
    }

    // --- checkTriangle ---

    @Test public void checkTriangleInside()
    {
        // Point (0.25, 0.25) inside triangle (0,0), (1,0), (0,1)
        assertTrue(Builder.checkTriangle(0.25, 0.25, 0, 0, 1, 0, 0, 1));
    }

    @Test public void checkTriangleOutside()
    {
        assertFalse(Builder.checkTriangle(2, 2, 0, 0, 1, 0, 0, 1));
    }

    @Test public void checkTriangleOnEdge()
    {
        // Point (0.5, 0) on edge AB
        assertTrue(Builder.checkTriangle(0.5, 0, 0, 0, 1, 0, 0, 1));
    }

    @Test public void checkTriangleOnVertex()
    {
        assertTrue(Builder.checkTriangle(0, 0, 0, 0, 1, 0, 0, 1));
    }

    @Test public void checkTriangleCentroid()
    {
        // Centroid of (0,0), (3,0), (0,3) is (1,1)
        assertTrue(Builder.checkTriangle(1, 1, 0, 0, 3, 0, 0, 3));
    }

    @Test public void checkTriangleJustOutside()
    {
        // Point just outside hypotenuse of (0,0), (1,0), (0,1)
        assertFalse(Builder.checkTriangle(0.6, 0.6, 0, 0, 1, 0, 0, 1));
    }

    // --- makeDotSphere ---

    @Test public void makeDotSphereRadius()
    {
        Collection dots = Builder.makeDotSphere(2.0, 1.0);
        assertTrue(dots.size() > 0);
        for (Object o : dots)
        {
            Triple t = (Triple) o;
            assertEquals(2.0, t.mag(), 0.1);
        }
    }

    @Test public void makeDotSphereHigherDensity()
    {
        Collection lo = Builder.makeDotSphere(1.0, 1.0);
        Collection hi = Builder.makeDotSphere(1.0, 4.0);
        assertTrue(hi.size() > lo.size());
    }

    @Test public void makeDotSphereSymmetry()
    {
        // Center of mass should be near origin
        Collection dots = Builder.makeDotSphere(1.0, 10.0);
        double cx = 0, cy = 0, cz = 0;
        for (Object o : dots)
        {
            Triple t = (Triple) o;
            cx += t.getX();
            cy += t.getY();
            cz += t.getZ();
        }
        int n = dots.size();
        cx /= n; cy /= n; cz /= n;
        assertEquals(0.0, cx, 0.1);
        assertEquals(0.0, cy, 0.1);
        assertEquals(0.0, cz, 0.1);
    }

    // --- makeBoundingBox ---

    @Test public void makeBoundingBoxSimple()
    {
        List<Triple> pts = Arrays.asList(
            new Triple(1, -2, 3),
            new Triple(-4, 5, -6),
            new Triple(7, 0, 0)
        );
        Triple[] bb = Builder.makeBoundingBox(pts);
        assertEquals(-4, bb[0].getX(), EPS);
        assertEquals(-2, bb[0].getY(), EPS);
        assertEquals(-6, bb[0].getZ(), EPS);
        assertEquals(7, bb[1].getX(), EPS);
        assertEquals(5, bb[1].getY(), EPS);
        assertEquals(3, bb[1].getZ(), EPS);
    }

    @Test public void makeBoundingBoxSinglePoint()
    {
        List<Triple> pts = Collections.singletonList(new Triple(3, 4, 5));
        Triple[] bb = Builder.makeBoundingBox(pts);
        assertEquals(3, bb[0].getX(), EPS);
        assertEquals(3, bb[1].getX(), EPS);
    }

    @Test public void makeBoundingBoxWithRadii()
    {
        List<Triple> pts = Arrays.asList(
            new Triple(0, 0, 0),
            new Triple(10, 10, 10)
        );
        double[] radii = {2.0, 3.0};
        Triple[] bb = Builder.makeBoundingBox(pts, radii);
        assertEquals(-2, bb[0].getX(), EPS);
        assertEquals(-2, bb[0].getY(), EPS);
        assertEquals(-2, bb[0].getZ(), EPS);
        assertEquals(13, bb[1].getX(), EPS);
        assertEquals(13, bb[1].getY(), EPS);
        assertEquals(13, bb[1].getZ(), EPS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void makeBoundingBoxMismatchedRadii()
    {
        List<Triple> pts = Arrays.asList(new Triple(0, 0, 0));
        Builder.makeBoundingBox(pts, new double[]{1.0, 2.0});
    }
}
