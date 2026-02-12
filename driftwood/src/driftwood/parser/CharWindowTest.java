package driftwood.parser;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;

public class CharWindowTest
{
    // --- Construction ---

    @Test public void constructFromString() throws IOException
    {
        CharWindow w = new CharWindow("hello");
        assertEquals(5, w.length());
        assertEquals('h', w.charAt(0));
    }

    @Test public void constructFromReader() throws IOException
    {
        CharWindow w = new CharWindow(new StringReader("abc"), 16);
        assertEquals(3, w.length());
    }

    @Test public void constructFromInputStream() throws IOException
    {
        byte[] data = "test".getBytes("UTF-8");
        CharWindow w = new CharWindow(new ByteArrayInputStream(data));
        assertEquals(4, w.length());
        assertEquals('t', w.charAt(0));
    }

    // --- charAt ---

    @Test public void charAtSequential() throws IOException
    {
        CharWindow w = new CharWindow("abcde");
        assertEquals('a', w.charAt(0));
        assertEquals('b', w.charAt(1));
        assertEquals('c', w.charAt(2));
        assertEquals('d', w.charAt(3));
        assertEquals('e', w.charAt(4));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void charAtNegative() throws IOException
    {
        CharWindow w = new CharWindow("abc");
        w.charAt(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void charAtPastEnd() throws IOException
    {
        CharWindow w = new CharWindow("abc");
        w.charAt(3);
    }

    // --- length ---

    @Test public void lengthAtEOF() throws IOException
    {
        CharWindow w = new CharWindow("ab");
        assertEquals(2, w.length());
    }

    @Test public void lengthEmpty() throws IOException
    {
        CharWindow w = new CharWindow("");
        assertEquals(0, w.length());
    }

    // --- read ---

    @Test public void readReturnsCharAndAdvances() throws IOException
    {
        CharWindow w = new CharWindow("abc");
        assertEquals('a', w.read());
        assertEquals('b', w.charAt(0));
        assertEquals(2, w.length());
    }

    @Test public void readEntireString() throws IOException
    {
        CharWindow w = new CharWindow("hi");
        assertEquals('h', w.read());
        assertEquals('i', w.read());
        assertEquals(0, w.length());
    }

    @Test public void readToEOFDecreasesLength() throws IOException
    {
        CharWindow w = new CharWindow("x");
        assertEquals(1, w.length());
        w.read();
        assertEquals(0, w.length());
    }

    // --- advance ---

    @Test public void advanceSkipsChars() throws IOException
    {
        CharWindow w = new CharWindow("abcdef");
        w.advance(3);
        assertEquals('d', w.charAt(0));
        assertEquals(3, w.length());
    }

    @Test(expected = IllegalArgumentException.class)
    public void advanceNegativeThrows() throws IOException
    {
        CharWindow w = new CharWindow("abc");
        w.advance(-1);
    }

    @Test public void advanceZeroIsNoop() throws IOException
    {
        CharWindow w = new CharWindow("abc");
        w.advance(0);
        assertEquals('a', w.charAt(0));
        assertEquals(3, w.length());
    }

    // --- look-behind ---

    @Test public void lookBehindAfterAdvance() throws IOException
    {
        CharWindow w = new CharWindow("abcdef");
        w.advance(3);
        // After advancing 3, dataMin should be negative, allowing look-behind
        assertEquals('c', w.charAt(-1));
        assertEquals('b', w.charAt(-2));
        assertEquals('a', w.charAt(-3));
    }

    // --- toString ---

    @Test public void toStringFull() throws IOException
    {
        CharWindow w = new CharWindow("hello");
        assertEquals("hello", w.toString());
    }

    @Test public void toStringRange() throws IOException
    {
        CharWindow w = new CharWindow("abcdef");
        assertEquals("bcd", w.toString(1, 4));
    }

    @Test public void toStringAfterAdvance() throws IOException
    {
        CharWindow w = new CharWindow("abcdef");
        w.advance(2);
        assertEquals("cdef", w.toString());
    }

    // --- subSequence ---

    @Test public void subSequenceRange() throws IOException
    {
        CharWindow w = new CharWindow("abcdef");
        assertEquals("cde", w.subSequence(2, 5));
    }

    // --- lineAt ---

    @Test public void lineAtFirstLine() throws IOException
    {
        CharWindow w = new CharWindow("abc");
        assertEquals(1, w.lineAt(0));
        assertEquals(1, w.lineAt(2));
    }

    @Test public void lineAtMultipleLines() throws IOException
    {
        CharWindow w = new CharWindow("ab\ncd\nef");
        assertEquals(1, w.lineAt(0)); // 'a'
        assertEquals(1, w.lineAt(1)); // 'b'
        assertEquals(1, w.lineAt(2)); // '\n'
        assertEquals(2, w.lineAt(3)); // 'c'
        assertEquals(2, w.lineAt(4)); // 'd'
        assertEquals(2, w.lineAt(5)); // '\n'
        assertEquals(3, w.lineAt(6)); // 'e'
    }

    @Test public void lineAtAfterAdvance() throws IOException
    {
        CharWindow w = new CharWindow("ab\ncd\nef");
        w.advance(3); // past "ab\n", now at 'c'
        assertEquals(2, w.lineAt(0)); // 'c' is on line 2
    }

    // --- columnAt ---

    @Test public void columnAtFirstLine() throws IOException
    {
        CharWindow w = new CharWindow("abc");
        assertEquals(1, w.columnAt(0));
        assertEquals(2, w.columnAt(1));
        assertEquals(3, w.columnAt(2));
    }

    @Test public void columnAtAfterNewline() throws IOException
    {
        CharWindow w = new CharWindow("ab\ncd");
        assertEquals(1, w.columnAt(3)); // 'c' is column 1 on second line
        assertEquals(2, w.columnAt(4)); // 'd' is column 2
    }

    @Test public void columnAtAfterAdvance() throws IOException
    {
        CharWindow w = new CharWindow("ab\ncd");
        w.advance(3); // past "ab\n"
        assertEquals(1, w.columnAt(0)); // 'c' is column 1
        assertEquals(2, w.columnAt(1)); // 'd' is column 2
    }

    // --- contextAt ---

    @Test public void contextAtSingleLine() throws IOException
    {
        CharWindow w = new CharWindow("hello world");
        assertEquals("hello world", w.contextAt(5));
    }

    @Test public void contextAtMultiLine() throws IOException
    {
        CharWindow w = new CharWindow("line1\nline2\nline3");
        assertEquals("line2", w.contextAt(6)); // 'l' of "line2"
    }

    @Test public void contextAtMiddleOfLine() throws IOException
    {
        CharWindow w = new CharWindow("abc\ndef\nghi");
        assertEquals("def", w.contextAt(5)); // 'e' of "def"
    }

    // --- startsWith ---

    @Test public void startsWithTrue() throws IOException
    {
        CharWindow w = new CharWindow("hello world");
        assertTrue(w.startsWith("hello"));
    }

    @Test public void startsWithFalse() throws IOException
    {
        CharWindow w = new CharWindow("hello world");
        assertFalse(w.startsWith("world"));
    }

    @Test public void startsWithAtOffset() throws IOException
    {
        CharWindow w = new CharWindow("hello world");
        assertTrue(w.startsWith("world", 6));
    }

    @Test public void startsWithTooLong() throws IOException
    {
        CharWindow w = new CharWindow("hi");
        assertFalse(w.startsWith("hello"));
    }

    @Test public void startsWithEmpty() throws IOException
    {
        CharWindow w = new CharWindow("abc");
        assertTrue(w.startsWith(""));
    }

    @Test public void startsWithAfterAdvance() throws IOException
    {
        CharWindow w = new CharWindow("hello world");
        w.advance(6);
        assertTrue(w.startsWith("world"));
    }

    // --- constants ---

    @Test public void kilocharIs1024()
    { assertEquals(1024, CharWindow.KILOCHAR); }

    @Test public void megacharIs1M()
    { assertEquals(1024 * 1024, CharWindow.MEGACHAR); }
}
