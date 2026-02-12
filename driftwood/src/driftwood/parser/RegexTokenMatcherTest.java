package driftwood.parser;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.regex.*;

public class RegexTokenMatcherTest
{
    // --- Pattern constants: SIGNED_INT ---

    @Test public void signedIntZero()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_INT, "0"); }

    @Test public void signedIntPositive()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_INT, "42"); }

    @Test public void signedIntNegative()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_INT, "-7"); }

    @Test public void signedIntExplicitPlus()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_INT, "+12"); }

    @Test public void signedIntNoLeadingZeros()
    { assertPatternDoesNotMatchFull(RegexTokenMatcher.SIGNED_INT, "007"); }

    // --- Pattern constants: UNSIGNED_INT ---

    @Test public void unsignedIntZero()
    { assertPatternMatches(RegexTokenMatcher.UNSIGNED_INT, "0"); }

    @Test public void unsignedIntPositive()
    { assertPatternMatches(RegexTokenMatcher.UNSIGNED_INT, "123"); }

    @Test public void unsignedIntRejectsSign()
    { assertPatternDoesNotMatchFull(RegexTokenMatcher.UNSIGNED_INT, "-5"); }

    // --- Pattern constants: SIGNED_REAL ---

    @Test public void signedRealSimple()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_REAL, "3.14"); }

    @Test public void signedRealNegative()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_REAL, "-2.5"); }

    @Test public void signedRealScientific()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_REAL, "1.5e10"); }

    @Test public void signedRealNegativeExponent()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_REAL, "6.022e-23"); }

    @Test public void signedRealTrailingDot()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_REAL, "42."); }

    @Test public void signedRealIntegerAlso()
    { assertPatternMatches(RegexTokenMatcher.SIGNED_REAL, "100"); }

    // --- Pattern constants: UNSIGNED_REAL ---

    @Test public void unsignedRealSimple()
    { assertPatternMatches(RegexTokenMatcher.UNSIGNED_REAL, "3.14"); }

    @Test public void unsignedRealRejectsSign()
    { assertPatternDoesNotMatchFull(RegexTokenMatcher.UNSIGNED_REAL, "-2.5"); }

    // --- Pattern constants: JAVA_WORD ---

    @Test public void javaWordSimple()
    { assertPatternMatches(RegexTokenMatcher.JAVA_WORD, "foo"); }

    @Test public void javaWordWithUnderscore()
    { assertPatternMatches(RegexTokenMatcher.JAVA_WORD, "_myVar"); }

    @Test public void javaWordWithDigits()
    { assertPatternMatches(RegexTokenMatcher.JAVA_WORD, "x42"); }

    @Test public void javaWordRejectsLeadingDigit()
    { assertPatternDoesNotMatchFull(RegexTokenMatcher.JAVA_WORD, "42x"); }

    // --- Pattern constants: JAVA_PUNC ---

    @Test public void javaPuncOperators()
    {
        String[] ops = {"+", "-", "*", "/", "=", "<", ">",
            "==", "!=", "<=", ">=", "+=", "-=", "*=", "/=",
            "&&", "||", "++", "--",
            ",", ";", ":", "?", "(", ")", "{", "}", "[", "]"};
        for (String op : ops)
            assertPatternMatches(RegexTokenMatcher.JAVA_PUNC, op);
    }

    // --- Pattern constants: Quoted strings ---

    @Test public void doubleQuotedString()
    { assertPatternMatches(RegexTokenMatcher.DOUBLE_QUOTED_STRING, "\"hello world\""); }

    @Test public void doubleQuotedStringWithEscape()
    { assertPatternMatches(RegexTokenMatcher.DOUBLE_QUOTED_STRING, "\"say \\\"hi\\\"\""); }

    @Test public void singleQuotedString()
    { assertPatternMatches(RegexTokenMatcher.SINGLE_QUOTED_STRING, "'hello'"); }

    @Test public void slashQuotedString()
    { assertPatternMatches(RegexTokenMatcher.SLASH_QUOTED_STRING, "/pattern/"); }

    // --- Pattern constants: Comments ---

    @Test public void hashComment()
    { assertPatternMatches(RegexTokenMatcher.HASH_COMMENT, "# this is a comment"); }

    @Test public void doubleSlashComment()
    { assertPatternMatches(RegexTokenMatcher.DOUBLE_SLASH_COMMENT, "// this is a comment"); }

    @Test public void slashStarComment()
    { assertPatternMatches(RegexTokenMatcher.SLASH_STAR_COMMENT, "/* block comment */"); }

    @Test public void slashStarCommentMultiline()
    { assertPatternMatches(RegexTokenMatcher.SLASH_STAR_COMMENT, "/* line1\nline2 */"); }

    // --- match / end / token ---

    @Test public void matchSimpleWord()
    {
        RegexTokenMatcher m = new RegexTokenMatcher(
            RegexTokenMatcher.JAVA_WORD, RegexTokenMatcher.WHITESPACE);
        assertTrue(m.match("hello", 0));
        assertEquals("hello", m.token());
        assertEquals(5, m.end());
    }

    @Test public void matchSkipsWhitespace()
    {
        RegexTokenMatcher m = new RegexTokenMatcher(
            RegexTokenMatcher.JAVA_WORD, RegexTokenMatcher.WHITESPACE);
        assertTrue(m.match("   hello", 0));
        assertEquals("hello", m.token());
        assertEquals(8, m.end());
    }

    @Test public void matchSequentialTokens()
    {
        RegexTokenMatcher m = new RegexTokenMatcher(
            RegexTokenMatcher.JAVA_WORD, RegexTokenMatcher.WHITESPACE);
        String input = "foo bar baz";

        assertTrue(m.match(input, 0));
        assertEquals("foo", m.token());
        int pos = m.end();

        assertTrue(m.match(input, pos));
        assertEquals("bar", m.token());
        pos = m.end();

        assertTrue(m.match(input, pos));
        assertEquals("baz", m.token());
    }

    @Test public void matchNumbers()
    {
        RegexTokenMatcher m = new RegexTokenMatcher(
            RegexTokenMatcher.SIGNED_REAL, RegexTokenMatcher.WHITESPACE);
        assertTrue(m.match("3.14", 0));
        assertEquals("3.14", m.token());
    }

    @Test public void matchFailsOnBadToken()
    {
        RegexTokenMatcher m = new RegexTokenMatcher(
            RegexTokenMatcher.UNSIGNED_INT, RegexTokenMatcher.WHITESPACE);
        assertFalse(m.match("abc", 0));
        assertNull(m.token());
    }

    @Test public void matchTrailingWhitespace()
    {
        RegexTokenMatcher m = new RegexTokenMatcher(
            RegexTokenMatcher.JAVA_WORD, RegexTokenMatcher.WHITESPACE);
        // After matching "foo", trailing whitespace returns true with null token
        assertTrue(m.match("foo   ", 0));
        assertEquals("foo", m.token());
        int pos = m.end();
        // Now only whitespace remains
        assertTrue(m.match("foo   ", pos));
        assertNull(m.token());
        assertEquals(6, m.end());
    }

    @Test public void matchFromMiddle()
    {
        RegexTokenMatcher m = new RegexTokenMatcher(
            RegexTokenMatcher.JAVA_WORD, RegexTokenMatcher.WHITESPACE);
        assertTrue(m.match("xxx hello", 3));
        assertEquals("hello", m.token());
    }

    // --- normalize ---

    @Test public void normalizeDefaultIsIdentity()
    {
        RegexTokenMatcher m = new RegexTokenMatcher(
            RegexTokenMatcher.JAVA_WORD, RegexTokenMatcher.WHITESPACE);
        assertEquals("test", m.normalize("test"));
    }

    // --- Constructor variants ---

    @Test public void constructFromStrings()
    {
        RegexTokenMatcher m = new RegexTokenMatcher("[a-z]+", "\\s+");
        assertTrue(m.match("hello", 0));
        assertEquals("hello", m.token());
    }

    // --- joinPatterns ---

    @Test public void joinPatternsFromPatternArray()
    {
        Pattern joined = RegexTokenMatcher.joinPatterns(new Pattern[]{
            RegexTokenMatcher.SIGNED_INT,
            RegexTokenMatcher.JAVA_WORD
        });
        Matcher m = joined.matcher("42");
        assertTrue(m.lookingAt());
        m = joined.matcher("foo");
        assertTrue(m.lookingAt());
    }

    @Test public void joinPatternsFromStringArray()
    {
        Pattern joined = RegexTokenMatcher.joinPatterns(new String[]{"abc", "def"});
        Matcher m = joined.matcher("abc");
        assertTrue(m.lookingAt());
        m = joined.matcher("def");
        assertTrue(m.lookingAt());
        m = joined.matcher("xyz");
        assertFalse(m.lookingAt());
    }

    // --- recursivePattern ---

    @Test public void recursivePatternSimple()
    {
        Pattern p = RegexTokenMatcher.recursivePattern("\\(", "[^()]*", "\\)", 3);
        Matcher m = p.matcher("(hello)");
        assertTrue(m.lookingAt());
    }

    @Test public void recursivePatternNested()
    {
        Pattern p = RegexTokenMatcher.recursivePattern("\\(", "[^()]*", "\\)", 3);
        Matcher m = p.matcher("(a(b)c)");
        assertTrue(m.lookingAt());
        assertEquals("(a(b)c)", m.group());
    }

    @Test public void recursivePatternAngleBrackets()
    {
        // Matches the same example from the class's main method
        Pattern p = RegexTokenMatcher.recursivePattern("<", "[a-zA-Z .]*", ">", 3);
        Matcher m = p.matcher("<foo>");
        assertTrue(m.lookingAt());
        assertEquals("<foo>", m.group());
    }

    @Test public void recursivePatternNestedAngle()
    {
        Pattern p = RegexTokenMatcher.recursivePattern("<", "[a-zA-Z .]*", ">", 3);
        Matcher m = p.matcher("<<Hi there>>");
        assertTrue(m.lookingAt());
        assertEquals("<<Hi there>>", m.group());
    }

    // --- Full tokenization loop ---

    @Test public void tokenizeFullExpression()
    {
        Pattern accept = RegexTokenMatcher.joinPatterns(new Pattern[]{
            RegexTokenMatcher.SIGNED_INT,
            RegexTokenMatcher.JAVA_WORD,
            RegexTokenMatcher.JAVA_PUNC
        });
        RegexTokenMatcher m = new RegexTokenMatcher(accept, RegexTokenMatcher.WHITESPACE);

        String input = "x = 42 + y";
        String[] expected = {"x", "=", "42", "+", "y"};

        int pos = 0;
        for (String exp : expected)
        {
            assertTrue("Should match at pos " + pos, m.match(input, pos));
            assertEquals(exp, m.token());
            pos = m.end();
        }
    }

    @Test public void tokenizeWithComments()
    {
        Pattern accept = RegexTokenMatcher.JAVA_WORD;
        Pattern ignore = RegexTokenMatcher.joinPatterns(new Pattern[]{
            RegexTokenMatcher.WHITESPACE,
            RegexTokenMatcher.HASH_COMMENT
        });
        RegexTokenMatcher m = new RegexTokenMatcher(accept, ignore);

        String input = "hello # comment\nworld";
        assertTrue(m.match(input, 0));
        assertEquals("hello", m.token());
        int pos = m.end();
        assertTrue(m.match(input, pos));
        assertEquals("world", m.token());
    }

    // --- Helpers ---

    private void assertPatternMatches(Pattern p, String s)
    {
        Matcher m = p.matcher(s);
        assertTrue("Pattern should match full string: " + s, m.matches());
    }

    private void assertPatternDoesNotMatchFull(Pattern p, String s)
    {
        Matcher m = p.matcher(s);
        assertFalse("Pattern should not match full string: " + s, m.matches());
    }
}
