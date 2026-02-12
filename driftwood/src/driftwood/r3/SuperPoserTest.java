package driftwood.r3;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuperPoserTest
{
    static final double EPS = 1e-8;

    // --- identical structures ---

    @Test public void identicalStructuresRmsdZero()
    {
        Triple[] ref = {
            new Triple(0, 0, 0),
            new Triple(1, 0, 0),
            new Triple(1, 1, 0),
            new Triple(0, 1, 0)
        };
        Triple[] mob = {
            new Triple(0, 0, 0),
            new Triple(1, 0, 0),
            new Triple(1, 1, 0),
            new Triple(0, 1, 0)
        };
        SuperPoser sp = new SuperPoser(ref, mob);
        Transform R = sp.superpos();
        assertEquals(0, sp.calcRMSD(R), EPS);
    }

    // --- pure translation ---

    @Test public void pureTranslation()
    {
        Triple[] ref = {
            new Triple(0, 0, 0),
            new Triple(1, 0, 0),
            new Triple(0, 1, 0),
            new Triple(0, 0, 1)
        };
        Triple[] mob = new Triple[ref.length];
        for(int i = 0; i < ref.length; i++)
            mob[i] = new Triple(ref[i]).add(new Triple(10, 20, 30));

        SuperPoser sp = new SuperPoser(ref, mob);
        Transform R = sp.superpos();
        assertEquals("rmsd after superpos", 0, sp.calcRMSD(R), EPS);

        // Verify the transform moves the mobile centroid to the reference centroid
        Triple mobPoint = new Triple(mob[0]);
        R.transform(mobPoint);
        assertEquals("transformed x", ref[0].x, mobPoint.x, 1e-4);
        assertEquals("transformed y", ref[0].y, mobPoint.y, 1e-4);
        assertEquals("transformed z", ref[0].z, mobPoint.z, 1e-4);
    }

    // --- pure rotation ---

    @Test public void pureRotation90Z()
    {
        Triple[] ref = {
            new Triple(1, 0, 0),
            new Triple(2, 0, 0),
            new Triple(1, 1, 0),
            new Triple(1, 0, 1)
        };
        Transform rot = new Transform().likeRotation(new Triple(0, 0, 1), 90);
        Triple[] mob = new Triple[ref.length];
        for(int i = 0; i < ref.length; i++)
        {
            mob[i] = new Triple(ref[i]);
            rot.transform(mob[i]);
        }

        SuperPoser sp = new SuperPoser(ref, mob);
        Transform R = sp.superpos();
        assertEquals("rmsd", 0, sp.calcRMSD(R), EPS);
    }

    // --- rotation + translation ---

    @Test public void rotationPlusTranslation()
    {
        Triple[] ref = {
            new Triple(0, 0, 0),
            new Triple(3, 0, 0),
            new Triple(0, 4, 0),
            new Triple(0, 0, 5),
            new Triple(1, 1, 1)
        };
        Transform rot = new Transform().likeRotation(new Triple(1, 1, 1), 60);
        Transform trans = new Transform().likeTranslation(5, -3, 7);
        Transform combined = new Transform().like(rot);
        combined.append(trans);

        Triple[] mob = new Triple[ref.length];
        for(int i = 0; i < ref.length; i++)
        {
            mob[i] = new Triple(ref[i]);
            combined.transform(mob[i]);
        }

        SuperPoser sp = new SuperPoser(ref, mob);
        Transform R = sp.superpos();
        assertEquals("rmsd", 0, sp.calcRMSD(R), 1e-6);
    }

    // --- known RMSD ---

    @Test public void knownNonZeroRmsd()
    {
        // Reference: simple square
        Triple[] ref = {
            new Triple(0, 0, 0),
            new Triple(1, 0, 0),
            new Triple(1, 1, 0),
            new Triple(0, 1, 0)
        };
        // Mobile: same square but perturbed slightly
        Triple[] mob = {
            new Triple(0.1, 0, 0),
            new Triple(1.1, 0, 0),
            new Triple(1.1, 1, 0),
            new Triple(0.1, 1, 0)
        };
        // These differ only by translation of 0.1 in x, so after
        // centroid alignment, RMSD should be 0
        SuperPoser sp = new SuperPoser(ref, mob);
        Transform R = sp.superpos();
        assertEquals("pure shift rmsd", 0, sp.calcRMSD(R), EPS);
    }

    // --- reset ---

    @Test public void resetAllowsReuse()
    {
        Triple[] ref1 = { new Triple(0,0,0), new Triple(1,0,0), new Triple(0,1,0), new Triple(0,0,1) };
        Triple[] mob1 = { new Triple(1,1,1), new Triple(2,1,1), new Triple(1,2,1), new Triple(1,1,2) };
        SuperPoser sp = new SuperPoser(ref1, mob1);
        Transform R1 = sp.superpos();
        assertEquals("first rmsd", 0, sp.calcRMSD(R1), EPS);

        // Reset with different data
        Triple[] ref2 = { new Triple(0,0,0), new Triple(5,0,0), new Triple(0,5,0), new Triple(0,0,5) };
        Triple[] mob2 = new Triple[ref2.length];
        Transform rot = new Transform().likeRotation(new Triple(0,1,0), 45);
        for(int i = 0; i < ref2.length; i++)
        {
            mob2[i] = new Triple(ref2[i]);
            rot.transform(mob2[i]);
        }
        sp.reset(ref2, mob2);
        Transform R2 = sp.superpos();
        assertEquals("second rmsd after reset", 0, sp.calcRMSD(R2), 1e-6);
    }

    // --- offset constructors ---

    @Test public void offsetConstructor()
    {
        Triple[] ref = {
            new Triple(99, 99, 99), // skipped
            new Triple(0, 0, 0),
            new Triple(1, 0, 0),
            new Triple(0, 1, 0),
            new Triple(0, 0, 1)
        };
        Triple[] mob = {
            new Triple(10, 10, 10),
            new Triple(11, 10, 10),
            new Triple(10, 11, 10),
            new Triple(10, 10, 11),
            new Triple(99, 99, 99) // skipped
        };
        SuperPoser sp = new SuperPoser(ref, 1, mob, 0, 4);
        Transform R = sp.superpos();
        assertEquals("offset rmsd", 0, sp.calcRMSD(R), EPS);
    }

    // --- arbitrary rotation axes ---

    @Test public void arbitraryRotationRecovery()
    {
        // Create a non-trivial point cloud
        Triple[] ref = {
            new Triple(1, 0, 0),
            new Triple(0, 2, 0),
            new Triple(0, 0, 3),
            new Triple(1, 1, 1),
            new Triple(-1, 2, -1),
            new Triple(3, -1, 2)
        };
        // Apply arbitrary rotation + translation
        Transform rot = new Transform().likeRotation(new Triple(2, -1, 3), 123);
        Transform trans = new Transform().likeTranslation(-7, 13, -4);
        Transform combined = new Transform().like(rot);
        combined.append(trans);

        Triple[] mob = new Triple[ref.length];
        for(int i = 0; i < ref.length; i++)
        {
            mob[i] = new Triple(ref[i]);
            combined.transform(mob[i]);
        }

        SuperPoser sp = new SuperPoser(ref, mob);
        Transform R = sp.superpos();
        assertEquals("arbitrary rotation rmsd", 0, sp.calcRMSD(R), 1e-6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notEnoughPointsThrows()
    {
        Triple[] ref = { new Triple(0,0,0), new Triple(1,0,0) };
        Triple[] mob = { new Triple(0,0,0) };
        new SuperPoser(ref, 0, mob, 0, 3); // 3 > mob.length
    }
}
