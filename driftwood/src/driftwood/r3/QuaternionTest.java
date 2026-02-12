package driftwood.r3;

import org.junit.Test;
import static org.junit.Assert.*;

public class QuaternionTest
{
    static final double EPS = 1e-10;

    // --- constructors ---

    @Test public void defaultConstructorIsIdentity()
    {
        Quaternion q = new Quaternion();
        assertEquals(0, q.getX(), 0);
        assertEquals(0, q.getY(), 0);
        assertEquals(0, q.getZ(), 0);
        assertEquals(1, q.getW(), 0);
    }

    @Test public void parameterizedConstructor()
    {
        Quaternion q = new Quaternion(1, 2, 3, 4);
        assertEquals(1, q.getX(), 0);
        assertEquals(2, q.getY(), 0);
        assertEquals(3, q.getZ(), 0);
        assertEquals(4, q.getW(), 0);
    }

    // --- identity rotation ---

    @Test public void identityQuaternionGivesIdentityTransform()
    {
        Quaternion q = new Quaternion(); // identity
        Transform t = new Transform().likeQuaternion(q);
        Triple p = new Triple(3, 4, 5);
        t.transform(p);
        assertEquals(3, p.x, EPS);
        assertEquals(4, p.y, EPS);
        assertEquals(5, p.z, EPS);
    }

    // --- roundtrip: rotation matrix -> quaternion -> rotation matrix ---

    @Test public void roundtripRotation90Z()
    {
        Transform orig = new Transform().likeRotation(new Triple(0, 0, 1), 90);
        Quaternion q = new Quaternion().likeRotation(orig);
        Transform recovered = new Transform().likeQuaternion(q);

        Triple p1 = new Triple(1, 0, 0);
        Triple p2 = new Triple(1, 0, 0);
        orig.transform(p1);
        recovered.transform(p2);
        assertEquals("x", p1.x, p2.x, EPS);
        assertEquals("y", p1.y, p2.y, EPS);
        assertEquals("z", p1.z, p2.z, EPS);
    }

    @Test public void roundtripRotation45XY()
    {
        Transform orig = new Transform().likeRotation(new Triple(1, 1, 0), 45);
        Quaternion q = new Quaternion().likeRotation(orig);
        Transform recovered = new Transform().likeQuaternion(q);

        // Test multiple points
        Triple[] testPoints = {
            new Triple(1, 0, 0),
            new Triple(0, 1, 0),
            new Triple(0, 0, 1),
            new Triple(1, 2, 3)
        };
        for(Triple orig_p : testPoints)
        {
            Triple p1 = new Triple(orig_p);
            Triple p2 = new Triple(orig_p);
            orig.transform(p1);
            recovered.transform(p2);
            assertEquals("x for "+orig_p, p1.x, p2.x, 1e-8);
            assertEquals("y for "+orig_p, p1.y, p2.y, 1e-8);
            assertEquals("z for "+orig_p, p1.z, p2.z, 1e-8);
        }
    }

    @Test public void roundtripRotation180X()
    {
        // 180 degrees is a special case (trace = -1)
        Transform orig = new Transform().likeRotation(new Triple(1, 0, 0), 180);
        Quaternion q = new Quaternion().likeRotation(orig);
        Transform recovered = new Transform().likeQuaternion(q);

        Triple p1 = new Triple(0, 1, 0);
        Triple p2 = new Triple(0, 1, 0);
        orig.transform(p1);
        recovered.transform(p2);
        assertEquals("x", p1.x, p2.x, EPS);
        assertEquals("y", p1.y, p2.y, EPS);
        assertEquals("z", p1.z, p2.z, EPS);
    }

    @Test public void roundtripArbitraryRotation()
    {
        Transform orig = new Transform().likeRotation(new Triple(1, 2, 3), 137);
        Quaternion q = new Quaternion().likeRotation(orig);
        Transform recovered = new Transform().likeQuaternion(q);

        Triple p1 = new Triple(7, 11, 13);
        Triple p2 = new Triple(7, 11, 13);
        orig.transform(p1);
        recovered.transform(p2);
        assertEquals("x", p1.x, p2.x, 1e-8);
        assertEquals("y", p1.y, p2.y, 1e-8);
        assertEquals("z", p1.z, p2.z, 1e-8);
    }

    // --- slerp ---

    @Test public void slerpAtStartGivesStart()
    {
        Transform r0 = new Transform().likeRotation(new Triple(0, 0, 1), 0);
        Transform r1 = new Transform().likeRotation(new Triple(0, 0, 1), 90);
        Quaternion q0 = new Quaternion().likeRotation(r0);
        Quaternion q1 = new Quaternion().likeRotation(r1);

        Quaternion interp = new Quaternion().likeSlerp(q0, q1, 0.0);
        Transform t = new Transform().likeQuaternion(interp);
        Triple p = new Triple(1, 0, 0);
        t.transform(p);
        assertEquals("x at t=0", 1, p.x, 1e-8);
        assertEquals("y at t=0", 0, p.y, 1e-8);
    }

    @Test public void slerpAtEndGivesEnd()
    {
        Transform r0 = new Transform().likeRotation(new Triple(0, 0, 1), 0);
        Transform r1 = new Transform().likeRotation(new Triple(0, 0, 1), 90);
        Quaternion q0 = new Quaternion().likeRotation(r0);
        Quaternion q1 = new Quaternion().likeRotation(r1);

        Quaternion interp = new Quaternion().likeSlerp(q0, q1, 1.0);
        Transform t = new Transform().likeQuaternion(interp);
        Triple p = new Triple(1, 0, 0);
        t.transform(p);
        assertEquals("x at t=1", 0, p.x, 1e-8);
        assertEquals("y at t=1", 1, p.y, 1e-8);
    }

    @Test public void slerpMidpointIsHalfwayRotation()
    {
        Transform r0 = new Transform().likeRotation(new Triple(0, 0, 1), 0);
        Transform r1 = new Transform().likeRotation(new Triple(0, 0, 1), 90);
        Quaternion q0 = new Quaternion().likeRotation(r0);
        Quaternion q1 = new Quaternion().likeRotation(r1);

        Quaternion interp = new Quaternion().likeSlerp(q0, q1, 0.5);
        Transform t = new Transform().likeQuaternion(interp);
        Triple p = new Triple(1, 0, 0);
        t.transform(p);
        // 45-degree rotation: (cos45, sin45, 0)
        double c45 = Math.cos(Math.toRadians(45));
        double s45 = Math.sin(Math.toRadians(45));
        assertEquals("x at t=0.5", c45, p.x, 1e-8);
        assertEquals("y at t=0.5", s45, p.y, 1e-8);
    }

    // --- equals, hashCode ---

    @Test public void equalsAndHashCode()
    {
        Quaternion a = new Quaternion(1, 2, 3, 4);
        Quaternion b = new Quaternion(1, 2, 3, 4);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void notEqual()
    {
        Quaternion a = new Quaternion(1, 2, 3, 4);
        Quaternion b = new Quaternion(1, 2, 3, 5);
        assertFalse(a.equals(b));
    }

    // --- isNaN ---

    @Test public void isNaN()
    {
        assertFalse(new Quaternion(1, 2, 3, 4).isNaN());
        assertTrue(new Quaternion(Double.NaN, 0, 0, 1).isNaN());
    }

    // --- setters ---

    @Test public void setXYZW()
    {
        Quaternion q = new Quaternion();
        q.setXYZW(1, 2, 3, 4);
        assertEquals(1, q.getX(), 0);
        assertEquals(2, q.getY(), 0);
        assertEquals(3, q.getZ(), 0);
        assertEquals(4, q.getW(), 0);
    }

    @Test public void setXYZLeavesW()
    {
        Quaternion q = new Quaternion(0, 0, 0, 42);
        q.setXYZ(1, 2, 3);
        assertEquals(1, q.getX(), 0);
        assertEquals(2, q.getY(), 0);
        assertEquals(3, q.getZ(), 0);
        assertEquals(42, q.getW(), 0);
    }
}
