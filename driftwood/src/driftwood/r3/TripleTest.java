package driftwood.r3;

import org.junit.Test;
import static org.junit.Assert.*;

public class TripleTest
{
    static final double EPS = 1e-10;

    // --- Constructors ---

    @Test public void defaultConstructorIsOrigin()
    {
        Triple t = new Triple();
        assertEquals(0, t.x, 0);
        assertEquals(0, t.y, 0);
        assertEquals(0, t.z, 0);
    }

    @Test public void xyzConstructor()
    {
        Triple t = new Triple(1, 2, 3);
        assertEquals(1, t.x, 0);
        assertEquals(2, t.y, 0);
        assertEquals(3, t.z, 0);
    }

    @Test public void copyConstructor()
    {
        Triple orig = new Triple(4, 5, 6);
        Triple copy = new Triple(orig);
        assertEquals(4, copy.x, 0);
        assertEquals(5, copy.y, 0);
        assertEquals(6, copy.z, 0);
    }

    // --- like ---

    @Test public void likeAssignsCoords()
    {
        Triple a = new Triple(1, 2, 3);
        Triple b = new Triple();
        b.like(a);
        assertEquals(1, b.x, 0);
        assertEquals(2, b.y, 0);
        assertEquals(3, b.z, 0);
    }

    @Test public void likeNullLeavesUnchanged()
    {
        Triple t = new Triple(1, 2, 3);
        t.like(null);
        assertEquals(1, t.x, 0);
        assertEquals(2, t.y, 0);
        assertEquals(3, t.z, 0);
    }

    // --- dot product ---

    @Test public void dotProductOrthogonal()
    {
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(0, 1, 0);
        assertEquals(0, a.dot(b), EPS);
    }

    @Test public void dotProductParallel()
    {
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(3, 0, 0);
        assertEquals(3, a.dot(b), EPS);
    }

    @Test public void dotProductGeneral()
    {
        Triple a = new Triple(1, 2, 3);
        Triple b = new Triple(4, 5, 6);
        assertEquals(32, a.dot(b), EPS); // 4+10+18
    }

    // --- cross product ---

    @Test public void crossProductBasis()
    {
        // i x j = k
        Triple i = new Triple(1, 0, 0);
        Triple j = new Triple(0, 1, 0);
        i.cross(j);
        assertEquals(0, i.x, EPS);
        assertEquals(0, i.y, EPS);
        assertEquals(1, i.z, EPS);
    }

    @Test public void crossProductAnticommutative()
    {
        Triple a = new Triple(1, 2, 3);
        Triple b = new Triple(4, 5, 6);
        Triple ab = new Triple(a).cross(b);
        Triple ba = new Triple(b).cross(a);
        assertEquals(-ab.x, ba.x, EPS);
        assertEquals(-ab.y, ba.y, EPS);
        assertEquals(-ab.z, ba.z, EPS);
    }

    @Test public void likeCrossMatchesCross()
    {
        Triple a = new Triple(2, 3, 4);
        Triple b = new Triple(5, 6, 7);
        Triple direct = new Triple(a).cross(b);
        Triple via = new Triple().likeCross(a, b);
        assertEquals(direct.x, via.x, EPS);
        assertEquals(direct.y, via.y, EPS);
        assertEquals(direct.z, via.z, EPS);
    }

    // --- magnitude ---

    @Test public void magOfUnitVector()
    {
        assertEquals(1, new Triple(1, 0, 0).mag(), EPS);
        assertEquals(1, new Triple(0, 1, 0).mag(), EPS);
        assertEquals(1, new Triple(0, 0, 1).mag(), EPS);
    }

    @Test public void magOf345()
    {
        // 3-4-5 right triangle in xz plane, mag = 5
        assertEquals(5, new Triple(3, 0, 4).mag(), EPS);
    }

    @Test public void mag2IsSquaredMag()
    {
        Triple t = new Triple(1, 2, 3);
        assertEquals(14, t.mag2(), EPS); // 1+4+9
    }

    @Test public void staticMag()
    {
        Triple t = new Triple(3, 4, 0);
        assertEquals(5, Triple.mag(t), EPS);
    }

    // --- unit ---

    @Test public void unitNormalizesVector()
    {
        Triple t = new Triple(3, 4, 0).unit();
        assertEquals(1, t.mag(), EPS);
        assertEquals(3.0/5, t.x, EPS);
        assertEquals(4.0/5, t.y, EPS);
    }

    @Test public void unitOfZeroReturnsZero()
    {
        Triple t = new Triple(0, 0, 0).unit();
        assertEquals(0, t.x, 0);
        assertEquals(0, t.y, 0);
        assertEquals(0, t.z, 0);
    }

    // --- arithmetic ---

    @Test public void mult()
    {
        Triple t = new Triple(1, 2, 3).mult(2);
        assertEquals(2, t.x, EPS);
        assertEquals(4, t.y, EPS);
        assertEquals(6, t.z, EPS);
    }

    @Test public void div()
    {
        Triple t = new Triple(4, 6, 8).div(2);
        assertEquals(2, t.x, EPS);
        assertEquals(3, t.y, EPS);
        assertEquals(4, t.z, EPS);
    }

    @Test public void add()
    {
        Triple t = new Triple(1, 2, 3).add(new Triple(10, 20, 30));
        assertEquals(11, t.x, EPS);
        assertEquals(22, t.y, EPS);
        assertEquals(33, t.z, EPS);
    }

    @Test public void sub()
    {
        Triple t = new Triple(10, 20, 30).sub(new Triple(1, 2, 3));
        assertEquals(9, t.x, EPS);
        assertEquals(18, t.y, EPS);
        assertEquals(27, t.z, EPS);
    }

    @Test public void neg()
    {
        Triple t = new Triple(1, -2, 3).neg();
        assertEquals(-1, t.x, EPS);
        assertEquals(2, t.y, EPS);
        assertEquals(-3, t.z, EPS);
    }

    @Test public void addMult()
    {
        Triple t = new Triple(1, 2, 3).addMult(2, new Triple(10, 20, 30));
        assertEquals(21, t.x, EPS);
        assertEquals(42, t.y, EPS);
        assertEquals(63, t.z, EPS);
    }

    @Test public void likeSum()
    {
        Triple a = new Triple(1, 2, 3);
        Triple b = new Triple(4, 5, 6);
        Triple r = new Triple().likeSum(a, b);
        assertEquals(5, r.x, EPS);
        assertEquals(7, r.y, EPS);
        assertEquals(9, r.z, EPS);
    }

    @Test public void likeDiff()
    {
        Triple a = new Triple(10, 20, 30);
        Triple b = new Triple(1, 2, 3);
        Triple r = new Triple().likeDiff(a, b);
        assertEquals(9, r.x, EPS);
        assertEquals(18, r.y, EPS);
        assertEquals(27, r.z, EPS);
    }

    @Test public void likeProd()
    {
        Triple v = new Triple(1, 2, 3);
        Triple r = new Triple().likeProd(5, v);
        assertEquals(5, r.x, EPS);
        assertEquals(10, r.y, EPS);
        assertEquals(15, r.z, EPS);
    }

    // --- distance ---

    @Test public void distanceSamePoint()
    {
        Triple a = new Triple(1, 2, 3);
        assertEquals(0, a.distance(a), EPS);
    }

    @Test public void distanceKnown()
    {
        Triple a = new Triple(0, 0, 0);
        Triple b = new Triple(3, 4, 0);
        assertEquals(5, a.distance(b), EPS);
    }

    @Test public void staticDistance()
    {
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(4, 4, 0);
        assertEquals(5, Triple.distance(a, b), EPS);
    }

    @Test public void sqDistanceKnown()
    {
        Triple a = new Triple(0, 0, 0);
        Triple b = new Triple(3, 4, 0);
        assertEquals(25, a.sqDistance(b), EPS);
    }

    // --- midpoint ---

    @Test public void likeMidpoint()
    {
        Triple a = new Triple(0, 0, 0);
        Triple b = new Triple(10, 20, 30);
        Triple m = new Triple().likeMidpoint(a, b);
        assertEquals(5, m.x, EPS);
        assertEquals(10, m.y, EPS);
        assertEquals(15, m.z, EPS);
    }

    // --- angle ---

    @Test public void angleOrthogonal()
    {
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(0, 1, 0);
        assertEquals(90, a.angle(b), EPS);
    }

    @Test public void angleParallel()
    {
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(5, 0, 0);
        assertEquals(0, a.angle(b), EPS);
    }

    @Test public void angleAntiparallel()
    {
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(-1, 0, 0);
        assertEquals(180, a.angle(b), EPS);
    }

    @Test public void angle45()
    {
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(1, 1, 0);
        assertEquals(45, a.angle(b), 1e-6);
    }

    @Test public void staticAngleABC()
    {
        // Right angle at origin
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(0, 0, 0);
        Triple c = new Triple(0, 1, 0);
        assertEquals(90, Triple.angle(a, b, c), EPS);
    }

    @Test public void staticAngle60()
    {
        // Equilateral triangle: side=1 at (0,0), (1,0), (0.5, sqrt(3)/2)
        Triple a = new Triple(0, 0, 0);
        Triple b = new Triple(0.5, Math.sqrt(3)/2, 0);
        Triple c = new Triple(1, 0, 0);
        assertEquals(60, Triple.angle(a, b, c), 1e-6);
    }

    // --- dihedral ---

    @Test public void dihedralPlanar()
    {
        // All coplanar => dihedral = 0 or 180
        Triple a = new Triple(0, 0, 0);
        Triple b = new Triple(1, 0, 0);
        Triple c = new Triple(2, 0, 0);
        Triple d = new Triple(3, 1, 0);
        // A-B-C are colinear, so normal is degenerate. Use a non-degenerate case:
        Triple a2 = new Triple(0, 1, 0);
        Triple b2 = new Triple(1, 0, 0);
        Triple c2 = new Triple(2, 0, 0);
        Triple d2 = new Triple(3, 1, 0);
        assertEquals(0, Triple.dihedral(a2, b2, c2, d2), 1e-6);
    }

    @Test public void dihedralRightAngle()
    {
        // 90-degree dihedral: D is in +z from C relative to the ABC plane
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(0, 0, 0);
        Triple c = new Triple(0, 1, 0);
        Triple d = new Triple(0, 1, 1);
        assertEquals(90, Math.abs(Triple.dihedral(a, b, c, d)), 1e-6);
    }

    @Test public void dihedralSignFlip()
    {
        // D on opposite side of plane should flip sign
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(0, 0, 0);
        Triple c = new Triple(0, 1, 0);
        double d1 = Triple.dihedral(a, b, c, new Triple(0, 1, 1));
        double d2 = Triple.dihedral(a, b, c, new Triple(0, 1, -1));
        assertEquals("opposite signs", -d1, d2, 1e-6);
    }

    @Test public void dihedral180()
    {
        // 180 degrees: A and D on opposite sides in the same plane
        Triple a = new Triple(1, 0, 0);
        Triple b = new Triple(0, 0, 0);
        Triple c = new Triple(0, 1, 0);
        Triple d = new Triple(0, 1, 0).add(new Triple(-1, 0, 0)); // (-1, 1, 0)
        assertEquals(180, Math.abs(Triple.dihedral(a, b, c, d)), 1e-6);
    }

    // --- likeNormal ---

    @Test public void likeNormalOrthogonalToPlane()
    {
        // Points in the xy-plane => normal along z
        Triple a = new Triple(0, 0, 0);
        Triple b = new Triple(1, 0, 0);
        Triple c = new Triple(0, 1, 0);
        Triple n = new Triple().likeNormal(a, b, c);
        assertEquals(1, n.mag(), EPS);
        // Should be (0,0,1) or (0,0,-1)
        assertEquals(0, Math.abs(n.x), EPS);
        assertEquals(0, Math.abs(n.y), EPS);
        assertEquals(1, Math.abs(n.z), EPS);
    }

    // --- likeProjection ---

    @Test public void likeProjectionOnAxis()
    {
        // Project (3,4,0) onto x-axis: should give (3,0,0)
        Triple pt = new Triple(3, 4, 0);
        Triple from = new Triple(0, 0, 0);
        Triple to = new Triple(1, 0, 0);
        Triple proj = new Triple().likeProjection(pt, from, to);
        assertEquals(3, proj.x, EPS);
        assertEquals(0, proj.y, EPS);
        assertEquals(0, proj.z, EPS);
    }

    @Test public void likeProjectionPointOnLine()
    {
        // Point already on line
        Triple pt = new Triple(5, 0, 0);
        Triple from = new Triple(0, 0, 0);
        Triple to = new Triple(10, 0, 0);
        Triple proj = new Triple().likeProjection(pt, from, to);
        assertEquals(5, proj.x, EPS);
        assertEquals(0, proj.y, EPS);
        assertEquals(0, proj.z, EPS);
    }

    // --- likeOrthogonal ---

    @Test public void likeOrthogonalIsOrthogonal()
    {
        Triple v = new Triple(1, 2, 3);
        Triple orth = new Triple().likeOrthogonal(v);
        assertEquals(90, v.angle(orth), 1e-6);
        assertEquals(1, orth.mag(), EPS);
    }

    @Test public void likeOrthogonalAxisVectors()
    {
        // Test all three axis-aligned vectors
        for(Triple v : new Triple[]{ new Triple(1,0,0), new Triple(0,1,0), new Triple(0,0,1) })
        {
            Triple orth = new Triple().likeOrthogonal(v);
            assertEquals(0, v.dot(orth), EPS);
            assertEquals(1, orth.mag(), EPS);
        }
    }

    // --- likeVector ---

    @Test public void likeVector()
    {
        Triple from = new Triple(1, 2, 3);
        Triple to = new Triple(4, 6, 8);
        Triple v = new Triple().likeVector(from, to);
        assertEquals(3, v.x, EPS);
        assertEquals(4, v.y, EPS);
        assertEquals(5, v.z, EPS);
    }

    // --- equals, hashCode ---

    @Test public void equalsAndHashCode()
    {
        Triple a = new Triple(1.5, 2.5, 3.5);
        Triple b = new Triple(1.5, 2.5, 3.5);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void notEqualDifferent()
    {
        Triple a = new Triple(1, 2, 3);
        Triple b = new Triple(1, 2, 4);
        assertFalse(a.equals(b));
    }

    // --- isNaN ---

    @Test public void isNaN()
    {
        assertFalse(new Triple(1, 2, 3).isNaN());
        assertTrue(new Triple(Double.NaN, 0, 0).isNaN());
    }

    // --- chaining ---

    @Test public void methodChaining()
    {
        // Verify that mutating methods return this
        Triple t = new Triple(1, 0, 0);
        Triple result = t.mult(3).add(new Triple(0, 1, 0)).unit();
        assertSame(t, result);
    }
}
