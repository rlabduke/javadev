package driftwood.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringsTest
{
    // --- justifyLeft ---

    @Test public void justifyLeftPads()
    { assertEquals("abc   ", Strings.justifyLeft("abc", 6)); }

    @Test public void justifyLeftExactLength()
    { assertEquals("abc", Strings.justifyLeft("abc", 3)); }

    @Test public void justifyLeftLongerString()
    { assertEquals("abcdef", Strings.justifyLeft("abcdef", 3)); }

    // --- justifyRight ---

    @Test public void justifyRightPads()
    { assertEquals("   abc", Strings.justifyRight("abc", 6)); }

    @Test public void justifyRightExactLength()
    { assertEquals("abc", Strings.justifyRight("abc", 3)); }

    // --- justifyCenter ---

    @Test public void justifyCenterEvenPad()
    { assertEquals(" ab ", Strings.justifyCenter("ab", 4)); }

    @Test public void justifyCenterOddPad()
    {
        String result = Strings.justifyCenter("ab", 5);
        assertEquals(5, result.length());
        assertEquals("ab", result.trim());
    }

    // --- forceLeft ---

    @Test public void forceLeftTruncates()
    { assertEquals("abc", Strings.forceLeft("abcdef", 3)); }

    @Test public void forceLeftPads()
    { assertEquals("ab   ", Strings.forceLeft("ab", 5)); }

    @Test public void forceLeftExact()
    { assertEquals("abc", Strings.forceLeft("abc", 3)); }

    // --- forceRight ---

    @Test public void forceRightTruncates()
    { assertEquals("def", Strings.forceRight("abcdef", 3)); }

    @Test public void forceRightPads()
    { assertEquals("   ab", Strings.forceRight("ab", 5)); }

    @Test public void forceRightExact()
    { assertEquals("abc", Strings.forceRight("abc", 3)); }

    // --- count ---

    @Test public void countSubstring()
    { assertEquals(3, Strings.count("abcabcabc", "abc")); }

    @Test public void countNoMatch()
    { assertEquals(0, Strings.count("abcabc", "xyz")); }

    @Test public void countOverlapping()
    { assertEquals(2, Strings.count("aaa", "aa")); }

    @Test public void countSingleChar()
    { assertEquals(3, Strings.count("a.b.c.d", ".")); }

    // --- explode ---

    @Test public void explodeComma()
    {
        String[] result = Strings.explode("a,b,c", ',');
        assertArrayEquals(new String[]{"a", "b", "c"}, result);
    }

    @Test public void explodeCommaKeepsEmpty()
    {
        String[] result = Strings.explode("a,,b", ',', true, false);
        assertArrayEquals(new String[]{"a", "", "b"}, result);
    }

    @Test public void explodeCommaDropsEmpty()
    {
        String[] result = Strings.explode("a,,b", ',', false, false);
        assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test public void explodeWhitespaceTrims()
    {
        String[] result = Strings.explode("a b  c", ' ');
        assertArrayEquals(new String[]{"a", "b", "c"}, result);
    }

    @Test public void explodeSingleElement()
    {
        String[] result = Strings.explode("abc", ',');
        assertArrayEquals(new String[]{"abc"}, result);
    }

    @Test public void explodeEmpty()
    {
        String[] result = Strings.explode("", ',', false, false);
        assertEquals(0, result.length);
    }

    // --- explodeInts ---

    @Test public void explodeIntsBasic()
    {
        int[] result = Strings.explodeInts("1,2,3", ',');
        assertArrayEquals(new int[]{1, 2, 3}, result);
    }

    @Test public void explodeIntsWithSpaces()
    {
        int[] result = Strings.explodeInts("10 20 30", ' ');
        assertArrayEquals(new int[]{10, 20, 30}, result);
    }

    @Test(expected = NumberFormatException.class)
    public void explodeIntsInvalid()
    { Strings.explodeInts("1,abc,3", ','); }

    // --- explodeDoubles ---

    @Test public void explodeDoublesBasic()
    {
        double[] result = Strings.explodeDoubles("1.5,2.5,3.5", ',');
        assertEquals(3, result.length);
        assertEquals(1.5, result[0], 1e-10);
        assertEquals(2.5, result[1], 1e-10);
        assertEquals(3.5, result[2], 1e-10);
    }

    // --- expandVariables ---

    @Test public void expandVariablesBasic()
    {
        String result = Strings.expandVariables("Hello {0}!", new String[]{"world"});
        assertEquals("Hello world!", result);
    }

    @Test public void expandVariablesMultiple()
    {
        String result = Strings.expandVariables("{0} and {1}", new String[]{"A", "B"});
        assertEquals("A and B", result);
    }

    @Test public void expandVariablesRepeated()
    {
        String result = Strings.expandVariables("{0} {0} {0}", new String[]{"ha"});
        assertEquals("ha ha ha", result);
    }

    @Test public void expandVariablesNoMatch()
    {
        String result = Strings.expandVariables("no placeholders", new String[]{"X"});
        assertEquals("no placeholders", result);
    }

    @Test public void expandVariablesWithKeys()
    {
        String result = Strings.expandVariables("{name} is {age}",
            new String[]{"name", "age"}, new String[]{"Alice", "30"});
        assertEquals("Alice is 30", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void expandVariablesMismatchedArrays()
    {
        Strings.expandVariables("{x}", new String[]{"x", "y"}, new String[]{"1"});
    }

    // --- compareVersions ---

    @Test public void compareVersionsEqual()
    { assertEquals(0, Strings.compareVersions("1.2.3", "1.2.3")); }

    @Test public void compareVersionsLess()
    { assertTrue(Strings.compareVersions("1.2.3", "1.2.4") < 0); }

    @Test public void compareVersionsGreater()
    { assertTrue(Strings.compareVersions("2.0", "1.9") > 0); }

    @Test public void compareVersionsDifferentLength()
    { assertTrue(Strings.compareVersions("1.2", "1.2.1") < 0); }

    @Test public void compareVersionsDifferentLengthReverse()
    { assertTrue(Strings.compareVersions("1.2.1", "1.2") > 0); }

    @Test public void compareVersionsNumericVsLexical()
    {
        // "9" < "10" numerically but "9" > "10" lexically
        assertTrue(Strings.compareVersions("1.9", "1.10") < 0);
    }

    @Test public void compareVersionsWithText()
    {
        // "rc1" vs "rc2" should compare lexically
        assertTrue(Strings.compareVersions("1.0.rc1", "1.0.rc2") < 0);
    }

    // --- tokenizeCommandLine ---

    @Test public void tokenizeSimple()
    {
        String[] tokens = Strings.tokenizeCommandLine("ls -la /tmp");
        assertArrayEquals(new String[]{"ls", "-la", "/tmp"}, tokens);
    }

    @Test public void tokenizeDoubleQuotes()
    {
        String[] tokens = Strings.tokenizeCommandLine("echo \"hello world\"");
        assertArrayEquals(new String[]{"echo", "hello world"}, tokens);
    }

    @Test public void tokenizeSingleQuotes()
    {
        String[] tokens = Strings.tokenizeCommandLine("echo 'hello world'");
        assertArrayEquals(new String[]{"echo", "hello world"}, tokens);
    }

    @Test public void tokenizeMultipleSpaces()
    {
        String[] tokens = Strings.tokenizeCommandLine("a   b   c");
        assertArrayEquals(new String[]{"a", "b", "c"}, tokens);
    }

    @Test public void tokenizeEmptyQuotes()
    {
        String[] tokens = Strings.tokenizeCommandLine("a \"\" b");
        assertArrayEquals(new String[]{"a", "", "b"}, tokens);
    }

    @Test public void tokenizeEmpty()
    {
        String[] tokens = Strings.tokenizeCommandLine("");
        assertEquals(0, tokens.length);
    }

    // --- formatMemory ---

    @Test public void formatMemoryBytes()
    { assertEquals("500 b", Strings.formatMemory(500)); }

    @Test public void formatMemoryKb()
    { assertEquals("5.0 kb", Strings.formatMemory(5000)); }

    @Test public void formatMemoryMb()
    { assertEquals("5.0 Mb", Strings.formatMemory(5000000)); }

    @Test public void formatMemoryGb()
    { assertEquals("5.0 Gb", Strings.formatMemory(5000000000L)); }

    // --- arrayToFloat ---

    @Test public void arrayToFloat()
    {
        float[] result = Strings.arrayToFloat(new String[]{"1.5", "2.5", ""});
        assertEquals(1.5f, result[0], 1e-6);
        assertEquals(2.5f, result[1], 1e-6);
        assertTrue(Float.isNaN(result[2]));
    }

    @Test public void arrayToFloatNull()
    {
        float[] result = Strings.arrayToFloat(new String[]{null});
        assertTrue(Float.isNaN(result[0]));
    }

    // --- arrayToDouble ---

    @Test public void arrayToDouble()
    {
        double[] result = Strings.arrayToDouble(new String[]{"1.5", "", null});
        assertEquals(1.5, result[0], 1e-10);
        assertTrue(Double.isNaN(result[1]));
        assertTrue(Double.isNaN(result[2]));
    }

    // --- arrayInParens ---

    @Test public void arrayInParensInts()
    { assertEquals("(1, 2, 3)", Strings.arrayInParens(new int[]{1, 2, 3})); }
}
