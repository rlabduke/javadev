package driftwood.data;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class CheapSetTest
{
    // --- Basic operations ---

    @Test public void emptySetSize()
    { assertEquals(0, new CheapSet().size()); }

    @Test public void addAndContains()
    {
        CheapSet s = new CheapSet();
        assertTrue(s.add("hello"));
        assertTrue(s.contains("hello"));
        assertEquals(1, s.size());
    }

    @Test public void addDuplicateReturnsFalse()
    {
        CheapSet s = new CheapSet();
        assertTrue(s.add("x"));
        assertFalse(s.add("x"));
        assertEquals(1, s.size());
    }

    @Test public void containsMissing()
    {
        CheapSet s = new CheapSet();
        s.add("a");
        assertFalse(s.contains("b"));
    }

    @Test public void removeExisting()
    {
        CheapSet s = new CheapSet();
        s.add("x");
        assertTrue(s.remove("x"));
        assertFalse(s.contains("x"));
        assertEquals(0, s.size());
    }

    @Test public void removeMissing()
    {
        CheapSet s = new CheapSet();
        assertFalse(s.remove("x"));
    }

    @Test public void removeAndReAdd()
    {
        CheapSet s = new CheapSet();
        s.add("x");
        s.remove("x");
        assertTrue(s.add("x"));
        assertTrue(s.contains("x"));
    }

    // --- Null handling ---

    @Test(expected = NullPointerException.class)
    public void addNullThrows()
    { new CheapSet().add(null); }

    @Test(expected = NullPointerException.class)
    public void containsNullThrows()
    { new CheapSet().contains(null); }

    @Test(expected = NullPointerException.class)
    public void removeNullThrows()
    { new CheapSet().remove(null); }

    // --- Multiple elements ---

    @Test public void addMultiple()
    {
        CheapSet s = new CheapSet();
        s.add("apple");
        s.add("banana");
        s.add("cherry");
        assertEquals(3, s.size());
        assertTrue(s.contains("apple"));
        assertTrue(s.contains("banana"));
        assertTrue(s.contains("cherry"));
    }

    @Test public void removeFromMiddle()
    {
        CheapSet s = new CheapSet();
        s.add("a"); s.add("b"); s.add("c");
        s.remove("b");
        assertTrue(s.contains("a"));
        assertFalse(s.contains("b"));
        assertTrue(s.contains("c"));
        assertEquals(2, s.size());
    }

    // --- get (unique to CheapSet) ---

    @Test public void getReturnsActualObject()
    {
        CheapSet s = new CheapSet();
        String original = new String("test"); // force new object
        s.add(original);
        String lookup = new String("test"); // different object, same value
        assertSame(original, s.get(lookup));
    }

    @Test public void getMissingReturnsNull()
    {
        CheapSet s = new CheapSet();
        assertNull(s.get("missing"));
    }

    // --- Clear ---

    @Test public void clear()
    {
        CheapSet s = new CheapSet();
        s.add("a"); s.add("b"); s.add("c");
        s.clear();
        assertEquals(0, s.size());
        assertFalse(s.contains("a"));
    }

    // --- Rehashing ---

    @Test public void manyElementsTriggerRehash()
    {
        CheapSet s = new CheapSet(4); // small initial capacity
        for (int i = 0; i < 100; i++)
            s.add("item" + i);
        assertEquals(100, s.size());
        for (int i = 0; i < 100; i++)
            assertTrue(s.contains("item" + i));
    }

    @Test public void ensureCapacity()
    {
        CheapSet s = new CheapSet();
        s.add("a"); s.add("b");
        s.ensureCapacity(1024);
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertEquals(2, s.size());
    }

    // --- Iterator ---

    @Test public void iteratorVisitsAll()
    {
        CheapSet s = new CheapSet();
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c", "d"));
        for (String e : expected) s.add(e);
        Set<String> actual = new HashSet<>();
        for (Iterator it = s.iterator(); it.hasNext(); )
            actual.add((String) it.next());
        assertEquals(expected, actual);
    }

    @Test public void iteratorRemove()
    {
        CheapSet s = new CheapSet();
        s.add("a"); s.add("b"); s.add("c");
        Iterator it = s.iterator();
        while (it.hasNext())
        {
            String val = (String) it.next();
            if (val.equals("b")) it.remove();
        }
        assertEquals(2, s.size());
        assertFalse(s.contains("b"));
    }

    @Test(expected = ConcurrentModificationException.class)
    public void iteratorDetectsConcurrentMod()
    {
        CheapSet s = new CheapSet();
        s.add("a"); s.add("b");
        Iterator it = s.iterator();
        it.next();
        s.add("c"); // modify during iteration
        it.next();
    }

    // --- Collection constructor ---

    @Test public void constructFromCollection()
    {
        List<String> list = Arrays.asList("x", "y", "z");
        CheapSet s = new CheapSet(list);
        assertEquals(3, s.size());
        assertTrue(s.contains("x"));
        assertTrue(s.contains("y"));
        assertTrue(s.contains("z"));
    }

    // --- equals ---

    @Test public void equalSets()
    {
        CheapSet s1 = new CheapSet();
        s1.add("a"); s1.add("b");
        CheapSet s2 = new CheapSet();
        s2.add("b"); s2.add("a");
        assertEquals(s1, s2);
    }

    @Test public void unequalSets()
    {
        CheapSet s1 = new CheapSet();
        s1.add("a"); s1.add("b");
        CheapSet s2 = new CheapSet();
        s2.add("a"); s2.add("c");
        assertNotEquals(s1, s2);
    }

    // --- Load factor ---

    @Test public void getLoadFactor()
    { assertEquals(0.75, new CheapSet().getLoadFactor(), 1e-10); }

    @Test public void customLoadFactor()
    { assertEquals(0.5, new CheapSet(16, 0.5).getLoadFactor(), 1e-10); }

    // --- Integer keys (hash collisions) ---

    @Test public void integerElements()
    {
        CheapSet s = new CheapSet();
        for (int i = 0; i < 50; i++)
            s.add(Integer.valueOf(i));
        assertEquals(50, s.size());
        for (int i = 0; i < 50; i++)
            assertTrue(s.contains(Integer.valueOf(i)));
    }
}
