package com.fleeksoft.ksoup.internal

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.internal.StringUtil.isAscii
import com.fleeksoft.ksoup.internal.StringUtil.isAsciiLetter
import com.fleeksoft.ksoup.internal.StringUtil.isBlank
import com.fleeksoft.ksoup.internal.StringUtil.isHexDigit
import com.fleeksoft.ksoup.internal.StringUtil.isNumeric
import com.fleeksoft.ksoup.internal.StringUtil.isWhitespace
import com.fleeksoft.ksoup.internal.StringUtil.join
import com.fleeksoft.ksoup.internal.StringUtil.normaliseWhitespace
import com.fleeksoft.ksoup.internal.StringUtil.padding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringUtilTest {

    @Test
    fun join() {
        assertEquals("", join(listOf(""), " "))
        assertEquals("one", join(listOf("one"), " "))
        assertEquals("one two three", join(mutableListOf<String?>("one", "two", "three"), " "))
    }

    @Test
    fun padding() {
        assertEquals("", padding(0))
        assertEquals(" ", padding(1))
        assertEquals("  ", padding(2))
        assertEquals("               ", padding(15))
        assertEquals("                              ", padding(45)) // we default to tap out at 30

        // memoization is up to 21 blocks (0 to 20 spaces) and exits early before min checks making maxPaddingWidth unused
        assertEquals("", padding(0, -1))
        assertEquals("                    ", padding(20, -1))

        // this test escapes memoization and continues through
        assertEquals("                     ", padding(21, -1))

        // this test escapes memoization and using unlimited length (-1) will allow requested spaces
        assertEquals("                              ", padding(30, -1))
        assertEquals("                                             ", padding(45, -1))

        // we tap out at 0 for this test
        assertEquals("", padding(0, 0))

        // as memoization is escaped, setting zero for max padding will not allow any requested width
        assertEquals("", padding(21, 0))

        // we tap out at 30 for these tests making > 30 use 30
        assertEquals("", padding(0, 30))
        assertEquals(" ", padding(1, 30))
        assertEquals("  ", padding(2, 30))
        assertEquals("               ", padding(15, 30))
        assertEquals("                              ", padding(45, 30))

        // max applies regardless of memoized
        assertEquals(5, padding(20, 5).length)
    }

    @Test
    fun paddingInACan() {
        val padding = padding
        assertEquals(21, padding.size)
        for (i in padding.indices) {
            assertEquals(i, padding[i].length)
        }
    }

    @Test
    fun isBlank() {
        assertTrue(isBlank(null))
        assertTrue(isBlank(""))
        assertTrue(isBlank("      "))
        assertTrue(isBlank("   \r\n  "))
        assertFalse(isBlank("hello"))
        assertFalse(isBlank("   hello   "))
    }

    @Test
    fun isNumeric() {
        assertFalse(isNumeric(null))
        assertFalse(isNumeric(" "))
        assertFalse(isNumeric("123 546"))
        assertFalse(isNumeric("hello"))
        assertFalse(isNumeric("123.334"))
        assertTrue(isNumeric("1"))
        assertTrue(isNumeric("1234"))
    }

    @Test
    fun isWhitespace() {
        assertTrue(isWhitespace('\t'.code))
        assertTrue(isWhitespace('\n'.code))
        assertTrue(isWhitespace('\r'.code))
        assertTrue(isWhitespace('\u000c'.code))
        assertTrue(isWhitespace(' '.code))
        assertFalse(isWhitespace('\u00a0'.code))
        assertFalse(isWhitespace('\u2000'.code))
        assertFalse(isWhitespace('\u3000'.code))
    }

    @Test
    fun normaliseWhiteSpace() {
        assertEquals(" ", normaliseWhitespace("    \r \n \r\n"))
        assertEquals(" hello there ", normaliseWhitespace("   hello   \r \n  there    \n"))
        assertEquals("hello", normaliseWhitespace("hello"))
        assertEquals("hello there", normaliseWhitespace("hello\nthere"))
    }

    @Test
    fun normaliseWhiteSpaceHandlesHighSurrogates() {
        val test71540chars = "\ud869\udeb2\u304b\u309a  1"
        val test71540charsExpectedSingleWhitespace = "\ud869\udeb2\u304b\u309a 1"
        assertEquals(test71540charsExpectedSingleWhitespace, normaliseWhitespace(test71540chars))
        val extractedText = Ksoup.parse(test71540chars).text()
        assertEquals(test71540charsExpectedSingleWhitespace, extractedText)
    }

    @Test
    fun resolvesRelativeUrls() {
        assertEquals("http://example.com/one/two?three", StringUtil.resolve("http://example.com", "./one/two?three"))
        assertEquals("http://example.com/one/two?three", StringUtil.resolve("http://example.com?one", "./one/two?three"))
        assertEquals("http://example.com/one/two?three#four", StringUtil.resolve("http://example.com", "./one/two?three#four"))
        assertEquals("https://example.com/one", StringUtil.resolve("http://example.com/", "https://example.com/one"))
        assertEquals("http://example.com/one/two.html", StringUtil.resolve("http://example.com/two/", "../one/two.html"))
        assertEquals("https://example2.com/one", StringUtil.resolve("https://example.com/", "//example2.com/one"))
        assertEquals("https://example.com:8080/one", StringUtil.resolve("https://example.com:8080", "./one"))
        assertEquals("https://example2.com/one", StringUtil.resolve("http://example.com/", "https://example2.com/one"))
        assertEquals("https://example.com/one", StringUtil.resolve("wrong", "https://example.com/one"))
        assertEquals("https://example.com/one", StringUtil.resolve("https://example.com/one", ""))
        assertEquals("https://example.com/one/two.c", StringUtil.resolve("https://example.com/one/two/", "../two.c"))
        assertEquals("https://example.com/two.c", StringUtil.resolve("https://example.com/one/two", "../two.c"))
        assertEquals("", StringUtil.resolve("wrong", "also wrong"))
        assertEquals("ftp://example.com/one", StringUtil.resolve("ftp://example.com/two/", "../one"))
        assertEquals("ftp://example.com/one/two.c", StringUtil.resolve("ftp://example.com/one/", "./two.c"))
        assertEquals("ftp://example.com/one/two.c", StringUtil.resolve("ftp://example.com/one/", "two.c"))
        // examples taken from rfc3986 section 5.4.2
        assertEquals("http://example.com/g", StringUtil.resolve("http://example.com/b/c/d;p?q", "../../../g"))
        assertEquals("http://example.com/g", StringUtil.resolve("http://example.com/b/c/d;p?q", "../../../../g"))
        assertEquals("http://example.com/g", StringUtil.resolve("http://example.com/b/c/d;p?q", "/./g"))
        assertEquals("http://example.com/g", StringUtil.resolve("http://example.com/b/c/d;p?q", "/../g"))
        assertEquals("http://example.com/b/c/g.", StringUtil.resolve("http://example.com/b/c/d;p?q", "g."))
        assertEquals("http://example.com/b/c/.g", StringUtil.resolve("http://example.com/b/c/d;p?q", ".g"))
        assertEquals("http://example.com/b/c/g..", StringUtil.resolve("http://example.com/b/c/d;p?q", "g.."))
        assertEquals("http://example.com/b/c/..g", StringUtil.resolve("http://example.com/b/c/d;p?q", "..g"))
        assertEquals("http://example.com/b/g", StringUtil.resolve("http://example.com/b/c/d;p?q", "./../g"))
        assertEquals("http://example.com/b/c/g/", StringUtil.resolve("http://example.com/b/c/d;p?q", "./g/."))
        assertEquals("http://example.com/b/c/g/h", StringUtil.resolve("http://example.com/b/c/d;p?q", "g/./h"))
        assertEquals("http://example.com/b/c/h", StringUtil.resolve("http://example.com/b/c/d;p?q", "g/../h"))
        assertEquals("http://example.com/b/c/g;x=1/y", StringUtil.resolve("http://example.com/b/c/d;p?q", "g;x=1/./y"))
        assertEquals("http://example.com/b/c/y", StringUtil.resolve("http://example.com/b/c/d;p?q", "g;x=1/../y"))
        assertEquals("http://example.com/b/c/g?y/./x", StringUtil.resolve("http://example.com/b/c/d;p?q", "g?y/./x"))
        assertEquals("http://example.com/b/c/g?y/../x", StringUtil.resolve("http://example.com/b/c/d;p?q", "g?y/../x"))
        assertEquals("http://example.com/b/c/g#s/./x", StringUtil.resolve("http://example.com/b/c/d;p?q", "g#s/./x"))
        assertEquals("http://example.com/b/c/g#s/../x", StringUtil.resolve("http://example.com/b/c/d;p?q", "g#s/../x"))
    }

    @Test
    fun stripsControlCharsFromUrls() {
        assertEquals("foo:bar", StringUtil.resolve("\nhttps://\texample.com/", "\r\nfo\to:ba\br"))
    }

    @Test
    fun allowsSpaceInUrl() {
        assertEquals("https://example.com/foo bar/", StringUtil.resolve("https://example.com/example/", "../foo bar/"))
    }

    @Test
    fun isAscii() {
        assertTrue(isAscii(""))
        assertTrue(isAscii("example.com"))
        assertTrue(isAscii("One Two"))
        assertFalse(isAscii("🧔"))
        assertFalse(isAscii("测试"))
        assertFalse(isAscii("测试.com"))
    }

    @Test
    fun isAsciiLetter() {
        assertTrue(isAsciiLetter('a'))
        assertTrue(isAsciiLetter('n'))
        assertTrue(isAsciiLetter('z'))
        assertTrue(isAsciiLetter('A'))
        assertTrue(isAsciiLetter('N'))
        assertTrue(isAsciiLetter('Z'))

        assertFalse(isAsciiLetter(' '))
        assertFalse(isAsciiLetter('-'))
        assertFalse(isAsciiLetter('0'))
        assertFalse(isAsciiLetter('ß'))
        assertFalse(isAsciiLetter('Ě'))
    }

    @Test
    fun isDigit() {
        assertTrue(StringUtil.isDigit('0'))
        assertTrue(StringUtil.isDigit('1'))
        assertTrue(StringUtil.isDigit('2'))
        assertTrue(StringUtil.isDigit('3'))
        assertTrue(StringUtil.isDigit('4'))
        assertTrue(StringUtil.isDigit('5'))
        assertTrue(StringUtil.isDigit('6'))
        assertTrue(StringUtil.isDigit('7'))
        assertTrue(StringUtil.isDigit('8'))
        assertTrue(StringUtil.isDigit('9'))
        assertFalse(StringUtil.isDigit('a'))
        assertFalse(StringUtil.isDigit('A'))
        assertFalse(StringUtil.isDigit('ä'))
        assertFalse(StringUtil.isDigit('Ä'))
        assertFalse(StringUtil.isDigit('١'))
        assertFalse(StringUtil.isDigit('୳'))
    }

    @Test
    fun isHexDigit() {
        assertTrue(isHexDigit('0'))
        assertTrue(isHexDigit('1'))
        assertTrue(isHexDigit('2'))
        assertTrue(isHexDigit('3'))
        assertTrue(isHexDigit('4'))
        assertTrue(isHexDigit('5'))
        assertTrue(isHexDigit('6'))
        assertTrue(isHexDigit('7'))
        assertTrue(isHexDigit('8'))
        assertTrue(isHexDigit('9'))
        assertTrue(isHexDigit('a'))
        assertTrue(isHexDigit('b'))
        assertTrue(isHexDigit('c'))
        assertTrue(isHexDigit('d'))
        assertTrue(isHexDigit('e'))
        assertTrue(isHexDigit('f'))
        assertTrue(isHexDigit('A'))
        assertTrue(isHexDigit('B'))
        assertTrue(isHexDigit('C'))
        assertTrue(isHexDigit('D'))
        assertTrue(isHexDigit('E'))
        assertTrue(isHexDigit('F'))

        assertFalse(isHexDigit('g'))
        assertFalse(isHexDigit('G'))
        assertFalse(isHexDigit('ä'))
        assertFalse(isHexDigit('Ä'))
        assertFalse(isHexDigit('١'))
        assertFalse(isHexDigit('୳'))
    }
}
