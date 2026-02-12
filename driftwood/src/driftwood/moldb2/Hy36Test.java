package driftwood.moldb2;

import org.junit.Test;
import static org.junit.Assert.*;

public class Hy36Test
{
    // --- width 4: standard numeric range ---

    @Test public void encode4zero()
    { assertEquals("   0", Hy36.encode(4, 0)); }

    @Test public void encode4negative()
    { assertEquals("-999", Hy36.encode(4, -999)); }

    @Test public void encode4negativeSmall()
    { assertEquals(" -78", Hy36.encode(4, -78)); }

    @Test public void encode4max()
    { assertEquals("9999", Hy36.encode(4, 9999)); }

    // --- width 4: hybrid-36 uppercase range ---

    @Test public void encode4firstHybrid()
    { assertEquals("A000", Hy36.encode(4, 10000)); }

    @Test public void encode4hybridLetterDigits()
    {
        assertEquals("A001", Hy36.encode(4, 10001));
        assertEquals("A009", Hy36.encode(4, 10009));
        assertEquals("A00A", Hy36.encode(4, 10010));
        assertEquals("A00Z", Hy36.encode(4, 10035));
        assertEquals("A010", Hy36.encode(4, 10036));
    }

    @Test public void encode4upperBoundary()
    {
        assertEquals("AZZZ", Hy36.encode(4, 10000 + 36*36*36 - 1));
        assertEquals("B000", Hy36.encode(4, 10000 + 36*36*36));
        assertEquals("ZZZZ", Hy36.encode(4, 10000 + 26*36*36*36 - 1));
    }

    // --- width 4: hybrid-36 lowercase range ---

    @Test public void encode4firstLowercase()
    { assertEquals("a000", Hy36.encode(4, 10000 + 26*36*36*36)); }

    @Test public void encode4lastLowercase()
    { assertEquals("zzzz", Hy36.encode(4, 10000 + 2*26*36*36*36 - 1)); }

    // --- width 5: standard numeric range ---

    @Test public void encode5zero()
    { assertEquals("    0", Hy36.encode(5, 0)); }

    @Test public void encode5negative()
    { assertEquals("-9999", Hy36.encode(5, -9999)); }

    @Test public void encode5max()
    { assertEquals("99999", Hy36.encode(5, 99999)); }

    // --- width 5: hybrid-36 ---

    @Test public void encode5firstHybrid()
    { assertEquals("A0000", Hy36.encode(5, 100000)); }

    @Test public void encode5lastUpper()
    { assertEquals("ZZZZZ", Hy36.encode(5, 100000 + 26*36*36*36*36 - 1)); }

    @Test public void encode5firstLower()
    { assertEquals("a0000", Hy36.encode(5, 100000 + 26*36*36*36*36)); }

    @Test public void encode5lastLower()
    { assertEquals("zzzzz", Hy36.encode(5, 100000 + 2*26*36*36*36*36 - 1)); }

    // --- decode roundtrips ---

    @Test public void roundtrip4()
    {
        int[] vals = {-999, -78, -6, 0, 1, 9999, 10000, 10035, 10036,
            10000 + 26*36*36*36 - 1, 10000 + 26*36*36*36,
            10000 + 2*26*36*36*36 - 1};
        for(int v : vals)
            assertEquals("roundtrip4 " + v, v, Hy36.decode(4, Hy36.encode(4, v)));
    }

    @Test public void roundtrip5()
    {
        int[] vals = {-9999, -123, 0, 12, 99999, 100000,
            100000 + 26*36*36*36*36 - 1, 100000 + 26*36*36*36*36,
            100000 + 2*26*36*36*36*36 - 1};
        for(int v : vals)
            assertEquals("roundtrip5 " + v, v, Hy36.decode(5, Hy36.encode(5, v)));
    }

    // --- decode specific strings ---

    @Test public void decode4spaces()
    { assertEquals(0, Hy36.decode(4, "    ")); }

    @Test public void decode4negativeZero()
    { assertEquals(0, Hy36.decode(4, "  -0")); }

    @Test public void decode5spaces()
    { assertEquals(0, Hy36.decode(5, "     ")); }

    // --- encode errors ---

    @Test(expected = Error.class)
    public void encode4tooSmall()
    { Hy36.encode(4, -1000); }

    @Test(expected = Error.class)
    public void encode4tooLarge()
    { Hy36.encode(4, 2436112); }

    @Test(expected = Error.class)
    public void encode5tooSmall()
    { Hy36.encode(5, -10000); }

    @Test(expected = Error.class)
    public void encode5tooLarge()
    { Hy36.encode(5, 87440032); }

    @Test(expected = Error.class)
    public void encodeUnsupportedWidth()
    { Hy36.encode(3, 0); }

    // --- decode errors ---

    @Test(expected = Error.class)
    public void decode4empty()
    { Hy36.decode(4, ""); }

    @Test(expected = Error.class)
    public void decode4wrongLength()
    { Hy36.decode(4, "    0"); }

    @Test(expected = Error.class)
    public void decode4invalidChars()
    { Hy36.decode(4, "A=BC"); }

    @Test(expected = Error.class)
    public void decode4mixedCase()
    { Hy36.decode(4, "40a0"); }

    @Test(expected = Error.class)
    public void decodeUnsupportedWidth()
    { Hy36.decode(3, "AAA"); }
}
