package driftwood.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class TinyMapTest
{
    // --- countSetBits ---

    @Test public void countSetBitsZero()
    { assertEquals(0, TinyMap.countSetBits(0)); }

    @Test public void countSetBitsOne()
    { assertEquals(1, TinyMap.countSetBits(1)); }

    @Test public void countSetBitsAllOnes()
    { assertEquals(32, TinyMap.countSetBits(0xFFFFFFFF)); }

    @Test public void countSetBitsAlternating()
    { assertEquals(16, TinyMap.countSetBits(0x55555555)); }

    @Test public void countSetBitsOtherAlternating()
    { assertEquals(16, TinyMap.countSetBits(0xAAAAAAAA)); }

    @Test public void countSetBitsPowerOfTwo()
    {
        for (int i = 0; i < 32; i++)
            assertEquals(1, TinyMap.countSetBits(1 << i));
    }

    @Test public void countSetBitsSpecific()
    { assertEquals(5, TinyMap.countSetBits(0b10101010100)); }

    // --- indexOf ---

    @Test public void indexOfEmptyMap()
    { assertEquals(0, TinyMap.indexOf(5, 0)); }

    @Test public void indexOfSingleEntry()
    {
        // Key 3 is present (bit 3 set). Asking for key 3: 0 entries below bit 3.
        int map = 1 << 3; // 0b1000
        assertEquals(0, TinyMap.indexOf(3, map));
    }

    @Test public void indexOfWithLowerEntries()
    {
        // Keys 1 and 3 present. Key 3 should be at index 1 (one entry below it).
        int map = (1 << 1) | (1 << 3); // 0b1010
        assertEquals(1, TinyMap.indexOf(3, map));
    }

    @Test public void indexOfWithGap()
    {
        // Keys 0, 5, 10 present. Key 10 should be at index 2.
        int map = (1 << 0) | (1 << 5) | (1 << 10);
        assertEquals(2, TinyMap.indexOf(10, map));
    }

    // --- contains ---

    @Test public void containsEmpty()
    { assertFalse(TinyMap.contains(0, 0)); }

    @Test public void containsPresent()
    { assertTrue(TinyMap.contains(5, 1 << 5)); }

    @Test public void containsAbsent()
    { assertFalse(TinyMap.contains(5, 1 << 6)); }

    // --- size ---

    @Test public void sizeEmpty()
    {
        TinyMap m = new TinyMap();
        assertEquals(0, m.size());
    }

    @Test public void sizeAfterPuts()
    {
        TinyMap m = new TinyMap();
        m.put(0, "a");
        m.put(5, "b");
        m.put(10, "c");
        assertEquals(3, m.size());
    }

    // --- put / get ---

    @Test public void putAndGet()
    {
        TinyMap m = new TinyMap();
        m.put(0, "zero");
        m.put(1, "one");
        m.put(31, "max");
        assertEquals("zero", m.get(0));
        assertEquals("one", m.get(1));
        assertEquals("max", m.get(31));
    }

    @Test public void getMissing()
    {
        TinyMap m = new TinyMap();
        assertNull(m.get(5));
    }

    @Test public void putReturnsOld()
    {
        TinyMap m = new TinyMap();
        assertNull(m.put(3, "first"));
        assertEquals("first", m.put(3, "second"));
        assertEquals("second", m.get(3));
    }

    @Test public void putNullValue()
    {
        TinyMap m = new TinyMap();
        m.put(5, "val");
        m.put(5, null);
        assertNull(m.get(5));
        assertTrue(m.contains(5));
    }

    @Test public void putMultipleKeysOrdered()
    {
        TinyMap m = new TinyMap();
        m.put(0, "a");
        m.put(1, "b");
        m.put(2, "c");
        assertEquals("a", m.get(0));
        assertEquals("b", m.get(1));
        assertEquals("c", m.get(2));
    }

    @Test public void putMultipleKeysReverse()
    {
        TinyMap m = new TinyMap();
        m.put(31, "z");
        m.put(15, "m");
        m.put(0, "a");
        assertEquals("z", m.get(31));
        assertEquals("m", m.get(15));
        assertEquals("a", m.get(0));
    }

    @Test public void putSparseKeys()
    {
        TinyMap m = new TinyMap();
        m.put(3, "three");
        m.put(17, "seventeen");
        m.put(29, "twentynine");
        assertEquals(3, m.size());
        assertEquals("three", m.get(3));
        assertEquals("seventeen", m.get(17));
        assertEquals("twentynine", m.get(29));
    }

    // --- remove ---

    @Test public void removeExisting()
    {
        TinyMap m = new TinyMap();
        m.put(5, "val");
        assertEquals("val", m.remove(5));
        assertNull(m.get(5));
        assertFalse(m.contains(5));
        assertEquals(0, m.size());
    }

    @Test public void removeMissing()
    {
        TinyMap m = new TinyMap();
        assertNull(m.remove(5));
    }

    @Test public void removeMiddle()
    {
        TinyMap m = new TinyMap();
        m.put(0, "a");
        m.put(5, "b");
        m.put(10, "c");
        m.remove(5);
        assertEquals("a", m.get(0));
        assertNull(m.get(5));
        assertEquals("c", m.get(10));
        assertEquals(2, m.size());
    }

    @Test public void removeAll()
    {
        TinyMap m = new TinyMap();
        m.put(1, "a");
        m.put(2, "b");
        m.remove(1);
        m.remove(2);
        assertEquals(0, m.size());
        // Should be able to re-add
        m.put(1, "new");
        assertEquals("new", m.get(1));
    }

    // --- toString ---

    @Test public void toStringEmpty()
    { assertEquals("[0 items]", new TinyMap().toString()); }

    @Test public void toStringWithEntries()
    {
        TinyMap m = new TinyMap();
        m.put(0, "zero");
        m.put(5, "five");
        String s = m.toString();
        assertTrue(s.contains("2 items"));
        assertTrue(s.contains("0:zero"));
        assertTrue(s.contains("5:five"));
    }
}
