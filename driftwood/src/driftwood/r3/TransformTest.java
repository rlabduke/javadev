package driftwood.r3;

import org.junit.Test;
import static org.junit.Assert.*;

public class TransformTest
{
    static final double EPS = 1e-10;

    // --- helpers ---

    static void assertTripleEquals(String msg, double ex, double ey, double ez, Triple t)
    {
        assertEquals(msg+" x", ex, t.x, EPS);
        assertEquals(msg+" y", ey, t.y, EPS);
        assertEquals(msg+" z", ez, t.z, EPS);
    }

    // --- identity ---

    @Test public void identityLeavesPointUnchanged()
    {
        Transform I = new Transform();
        Triple p = new Triple(5, 10, 15);
        I.transform(p);
        assertTripleEquals("identity", 5, 10, 15, p);
    }

    @Test public void likeIdentityResetsToIdentity()
    {
        Transform t = new Transform().likeTranslation(1, 2, 3);
        t.likeIdentity();
        Triple p = new Triple(5, 10, 15);
        t.transform(p);
        assertTripleEquals("likeIdentity", 5, 10, 15, p);
    }

    // --- translation ---

    @Test public void translationMovesPoint()
    {
        Transform t = new Transform().likeTranslation(10, 20, 30);
        Triple p = new Triple(1, 2, 3);
        t.transform(p);
        assertTripleEquals("translate point", 11, 22, 33, p);
    }

    @Test public void translationDoesNotMoveVector()
    {
        Transform t = new Transform().likeTranslation(10, 20, 30);
        Triple v = new Triple(1, 2, 3);
        t.transformVector(v);
        assertTripleEquals("translate vector", 1, 2, 3, v);
    }

    @Test public void translationFromTuple()
    {
        Triple offset = new Triple(5, 6, 7);
        Transform t = new Transform().likeTranslation(offset);
        Triple p = new Triple(1, 1, 1);
        t.transform(p);
        assertTripleEquals("translate tuple", 6, 7, 8, p);
    }

    // --- rotation ---

    @Test public void rotation90AroundZ()
    {
        // Rotate (1,0,0) by 90 degrees around Z => (0,1,0)
        Transform r = new Transform().likeRotation(new Triple(0, 0, 1), 90);
        Triple p = new Triple(1, 0, 0);
        r.transform(p);
        assertTripleEquals("rot90 Z", 0, 1, 0, p);
    }

    @Test public void rotation90AroundX()
    {
        // Rotate (0,1,0) by 90 degrees around X => (0,0,1)
        Transform r = new Transform().likeRotation(new Triple(1, 0, 0), 90);
        Triple p = new Triple(0, 1, 0);
        r.transform(p);
        assertTripleEquals("rot90 X", 0, 0, 1, p);
    }

    @Test public void rotation90AroundY()
    {
        // Rotate (0,0,1) by 90 degrees around Y => (1,0,0)
        Transform r = new Transform().likeRotation(new Triple(0, 1, 0), 90);
        Triple p = new Triple(0, 0, 1);
        r.transform(p);
        assertTripleEquals("rot90 Y", 1, 0, 0, p);
    }

    @Test public void rotation180AroundZ()
    {
        Transform r = new Transform().likeRotation(new Triple(0, 0, 1), 180);
        Triple p = new Triple(1, 0, 0);
        r.transform(p);
        assertTripleEquals("rot180 Z", -1, 0, 0, p);
    }

    @Test public void rotationPreservesLength()
    {
        Transform r = new Transform().likeRotation(new Triple(1, 1, 1), 37);
        Triple p = new Triple(3, 4, 5);
        double origMag = p.mag();
        r.transform(p);
        assertEquals("rotation preserves length", origMag, p.mag(), EPS);
    }

    @Test public void rotationAboutArbitraryPoint()
    {
        // Rotate (2,0,0) by 90 around Z axis passing through (1,0,0)
        // => should map to (1,1,0)
        Triple from = new Triple(1, 0, 0);
        Triple to = new Triple(1, 0, 1);
        Transform r = new Transform().likeRotation(from, to, 90);
        Triple p = new Triple(2, 0, 0);
        r.transform(p);
        assertTripleEquals("rot about point", 1, 1, 0, p);
    }

    @Test public void rotation360IsIdentity()
    {
        Transform r = new Transform().likeRotation(new Triple(1, 1, 1), 360);
        Triple p = new Triple(2, 3, 5);
        r.transform(p);
        assertTripleEquals("rot360", 2, 3, 5, p);
    }

    // --- scaling ---

    @Test public void uniformScale()
    {
        Transform s = new Transform().likeScale(3);
        Triple p = new Triple(1, 2, 3);
        s.transform(p);
        assertTripleEquals("scale", 3, 6, 9, p);
    }

    @Test public void nonUniformScale()
    {
        Transform s = new Transform().likeScale(2, 3, 4);
        Triple p = new Triple(1, 1, 1);
        s.transform(p);
        assertTripleEquals("non-uniform scale", 2, 3, 4, p);
    }

    // --- composition ---

    @Test public void appendCombinesTransforms()
    {
        // Translate then rotate
        // Translate (1,0,0) by (1,0,0) => (2,0,0)
        // Then rotate 90 around Z => (0,2,0)
        Transform translate = new Transform().likeTranslation(1, 0, 0);
        Transform rotate = new Transform().likeRotation(new Triple(0, 0, 1), 90);
        // append means "t happens after" so: rotate.append(translate) means translate first, then rotate
        // Actually: premult(t) assigns this = t * this, and append == premult
        // So rotate.append(translate) => this = translate * rotate ... no wait.
        // Let me think again. append(t) sets this = t * this, which means t is applied AFTER this.
        // So to do translate first, then rotate: translate.append(rotate)
        Transform combined = new Transform().like(translate);
        combined.append(rotate);
        Triple p = new Triple(1, 0, 0);
        combined.transform(p);
        assertTripleEquals("append", 0, 2, 0, p);
    }

    @Test public void prependCombinesTransforms()
    {
        // prepend(t) sets this = this * t, meaning t happens BEFORE this.
        Transform rotate = new Transform().likeRotation(new Triple(0, 0, 1), 90);
        Transform translate = new Transform().likeTranslation(1, 0, 0);
        // We want: rotate after translate
        // rotate.prepend(translate) => this = rotate * translate, meaning translate first, then rotate
        Transform combined = new Transform().like(rotate);
        combined.prepend(translate);
        Triple p = new Triple(1, 0, 0);
        combined.transform(p);
        assertTripleEquals("prepend", 0, 2, 0, p);
    }

    @Test public void identityComposition()
    {
        Transform t = new Transform().likeTranslation(5, 10, 15);
        Transform I = new Transform();
        Transform combined = new Transform().like(t);
        combined.append(I);
        Triple p = new Triple(1, 1, 1);
        combined.transform(p);
        assertTripleEquals("identity composition", 6, 11, 16, p);
    }

    // --- like (copy) ---

    @Test public void likeCopiesTransform()
    {
        Transform orig = new Transform().likeTranslation(1, 2, 3);
        Transform copy = new Transform().like(orig);
        Triple p1 = new Triple(0, 0, 0);
        Triple p2 = new Triple(0, 0, 0);
        orig.transform(p1);
        copy.transform(p2);
        assertTripleEquals("like", p1.x, p1.y, p1.z, p2);
    }

    // --- likeMatrix ---

    @Test public void likeMatrixSetsRotationPart()
    {
        // 90-degree rotation around Z as explicit matrix
        Transform t = new Transform().likeMatrix(
            0, -1, 0,
            1,  0, 0,
            0,  0, 1);
        Triple p = new Triple(1, 0, 0);
        t.transform(p);
        assertTripleEquals("likeMatrix rot90Z", 0, 1, 0, p);
    }

    // --- transform with separate in/out ---

    @Test public void transformSeparateInOut()
    {
        Transform t = new Transform().likeTranslation(1, 2, 3);
        Triple in = new Triple(10, 20, 30);
        Triple out = new Triple();
        t.transform(in, out);
        assertTripleEquals("in unchanged", 10, 20, 30, in);
        assertTripleEquals("out transformed", 11, 22, 33, out);
    }

    @Test public void transformVectorSeparateInOut()
    {
        Transform t = new Transform().likeScale(2);
        Triple in = new Triple(3, 4, 5);
        Triple out = new Triple();
        t.transformVector(in, out);
        assertTripleEquals("vector in", 3, 4, 5, in);
        assertTripleEquals("vector out", 6, 8, 10, out);
    }

    // --- perspective ---

    @Test public void perspectiveAtInfinity()
    {
        // Very large d => nearly identity (no perspective)
        Transform t = new Transform().likePerspective(1e10);
        Triple p = new Triple(1, 2, 3);
        t.transform(p);
        assertEquals(1, p.x, 1e-4);
        assertEquals(2, p.y, 1e-4);
        assertEquals(3, p.z, 1e-4);
    }

    @Test public void perspectiveShrinksFar()
    {
        // d = 100, point at z=50 => scale = d/(d-z) = 100/50 = 2
        Transform t = new Transform().likePerspective(100);
        Triple p = new Triple(1, 1, 50);
        t.transform(p);
        assertEquals(2, p.x, EPS);
        assertEquals(2, p.y, EPS);
    }

    // --- orthonormalize ---

    @Test public void orthonormalizeFixesDrift()
    {
        // Start with a rotation, add small perturbation, orthonormalize should fix it
        Transform t = new Transform().likeRotation(new Triple(1, 1, 1), 30);
        // Slightly perturb
        t.set(1, 1, t.get(1,1) + 0.001);
        t.set(2, 2, t.get(2,2) - 0.001);
        t.orthonormalize();

        // Verify orthogonality: rows should be orthogonal unit vectors
        Triple r1 = new Triple(t.get(1,1), t.get(1,2), t.get(1,3));
        Triple r2 = new Triple(t.get(2,1), t.get(2,2), t.get(2,3));
        Triple r3 = new Triple(t.get(3,1), t.get(3,2), t.get(3,3));
        assertEquals("row1 unit", 1, r1.mag(), 1e-6);
        assertEquals("row2 unit", 1, r2.mag(), 1e-6);
        assertEquals("row3 unit", 1, r3.mag(), 1e-6);
        assertEquals("r1.r2 orthogonal", 0, r1.dot(r2), 1e-6);
        assertEquals("r1.r3 orthogonal", 0, r1.dot(r3), 1e-6);
        assertEquals("r2.r3 orthogonal", 0, r2.dot(r3), 1e-6);
    }

    // --- get/set ---

    @Test public void getAndSet()
    {
        Transform t = new Transform();
        t.set(2, 3, 42.0);
        assertEquals(42.0, t.get(2, 3), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOutOfBoundsThrows()
    {
        new Transform().get(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setOutOfBoundsThrows()
    {
        new Transform().set(5, 1, 0);
    }

    // --- equals, hashCode ---

    @Test public void equalsAndHashCode()
    {
        Transform a = new Transform().likeTranslation(1, 2, 3);
        Transform b = new Transform().likeTranslation(1, 2, 3);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void notEqual()
    {
        Transform a = new Transform().likeTranslation(1, 2, 3);
        Transform b = new Transform().likeTranslation(1, 2, 4);
        assertFalse(a.equals(b));
    }

    // --- isNaN ---

    @Test public void isNaN()
    {
        assertFalse(new Transform().isNaN());
        Transform t = new Transform();
        t.set(1, 1, Double.NaN);
        assertTrue(t.isNaN());
    }
}
