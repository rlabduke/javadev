package molikin;

import org.junit.Test;
import static org.junit.Assert.*;
import driftwood.moldb2.*;

/**
 * Tests for Bond: canonical ordering, equality, hashing, mirror bonds,
 * and comparison.
 */
public class BondTest
{
    private AtomState makeAS(String name, int serial)
    {
        Atom a = new Atom(name, "C", false);
        return new AtomState(a, String.valueOf(serial));
    }

    // --- Canonical ordering ---

    @Test public void canonicalOrderLowerFirst()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        Bond b = new Bond(as1, 1, as2, 2);
        assertSame(as1, b.lower);
        assertSame(as2, b.higher);
    }

    @Test public void canonicalOrderSwapped()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        Bond b = new Bond(as2, 2, as1, 1);
        // Should still be ordered by index
        assertSame(as1, b.lower);
        assertSame(as2, b.higher);
    }

    // --- Mirror ---

    @Test public void mirrorReversesLowerHigher()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        Bond b = new Bond(as1, 1, as2, 2);
        Bond m = b.mirror;
        assertSame(as2, m.lower);
        assertSame(as1, m.higher);
    }

    @Test public void mirrorOfMirrorIsOriginal()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        Bond b = new Bond(as1, 1, as2, 2);
        assertSame(b, b.mirror.mirror);
    }

    // --- Equality ---

    @Test public void bondEqualsItself()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        Bond b = new Bond(as1, 1, as2, 2);
        assertTrue(b.equals(b));
    }

    @Test public void bondEqualsMirror()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        Bond b = new Bond(as1, 1, as2, 2);
        assertTrue(b.equals(b.mirror));
        assertTrue(b.mirror.equals(b));
    }

    @Test public void bondEqualsSameIndices()
    {
        AtomState as1a = makeAS(" CA ", 1);
        AtomState as1b = makeAS(" N  ", 1);
        AtomState as2a = makeAS(" CB ", 2);
        AtomState as2b = makeAS(" O  ", 2);
        Bond b1 = new Bond(as1a, 1, as2a, 2);
        Bond b2 = new Bond(as1b, 1, as2b, 2);
        assertTrue(b1.equals(b2)); // same indices => equal
    }

    @Test public void bondNotEqualDifferentIndices()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        AtomState as3 = makeAS(" CG ", 3);
        Bond b1 = new Bond(as1, 1, as2, 2);
        Bond b2 = new Bond(as1, 1, as3, 3);
        assertFalse(b1.equals(b2));
    }

    // --- HashCode ---

    @Test public void hashCodeConsistentWithEquals()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        Bond b = new Bond(as1, 1, as2, 2);
        assertEquals(b.hashCode(), b.mirror.hashCode());
    }

    // --- CompareTo ---

    @Test public void compareToSortsByLowThenHigh()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        AtomState as3 = makeAS(" CG ", 3);
        Bond b12 = new Bond(as1, 1, as2, 2);
        Bond b13 = new Bond(as1, 1, as3, 3);
        Bond b23 = new Bond(as2, 2, as3, 3);
        assertTrue(b12.compareTo(b13) < 0); // same low(1), high 2 < 3
        assertTrue(b12.compareTo(b23) < 0); // low 1 < 2
        assertTrue(b13.compareTo(b23) < 0); // low 1 < 2
    }

    @Test public void compareToZeroForEqual()
    {
        AtomState as1 = makeAS(" CA ", 1);
        AtomState as2 = makeAS(" CB ", 2);
        Bond b = new Bond(as1, 1, as2, 2);
        assertEquals(0, b.compareTo(b.mirror));
    }

    // --- Static optimize method (basic sanity) ---

    @Test public void optimizeBondSequenceDoesNotCrash() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        Atom aN = new Atom(" N  ", "N", false);
        Atom aCA = new Atom(" CA ", "C", false);
        Atom aC = new Atom(" C  ", "C", false);
        Atom aO = new Atom(" O  ", "O", false);
        res.add(aN);
        res.add(aCA);
        res.add(aC);
        res.add(aO);
        AtomState sN = new AtomState(aN, "1");
        AtomState sCA = new AtomState(aCA, "2");
        AtomState sC = new AtomState(aC, "3");
        AtomState sO = new AtomState(aO, "4");

        Bond[] bonds = {
            new Bond(sN, 0, sCA, 1),
            new Bond(sCA, 1, sC, 2),
            new Bond(sC, 2, sO, 3)
        };
        java.util.Arrays.sort(bonds);
        Bond.optimizeBondSequence(bonds);
        assertEquals(3, bonds.length);
    }
}
