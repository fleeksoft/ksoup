package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup
import kotlin.jvm.JvmStatic
import kotlin.test.*

/**
 * Token queue tests.
 */
class TokenQueueTest {

    @Test
    fun chompBalanced() {
        val tq = TokenQueue(":contains(one (two) three) four")
        val pre = tq.consumeTo("(")
        val guts = tq.chompBalanced('(', ')')
        val remainder = tq.remainder()
        assertEquals(":contains", pre)
        assertEquals("one (two) three", guts)
        assertEquals(" four", remainder)
    }

    @Test
    fun chompEscapedBalanced() {
        val tq = TokenQueue(":contains(one (two) \\( \\) \\) three) four")
        val pre = tq.consumeTo("(")
        val guts = tq.chompBalanced('(', ')')
        val remainder = tq.remainder()
        assertEquals(":contains", pre)
        assertEquals("one (two) \\( \\) \\) three", guts)
        assertEquals("one (two) ( ) ) three", TokenQueue.unescape(guts))
        assertEquals(" four", remainder)
    }

    @Test
    fun chompBalancedMatchesAsMuchAsPossible() {
        val tq = TokenQueue("unbalanced(something(or another)) else")
        tq.consumeTo("(")
        val match = tq.chompBalanced('(', ')')
        assertEquals("something(or another)", match)
    }

    @Test
    fun unescape() {
        assertEquals("one ( ) \\", TokenQueue.unescape("one \\( \\) \\\\"))
    }

    @Test
    fun unescape_2() {
        assertEquals("\\&", TokenQueue.unescape("\\\\\\&"))
    }

    @Test
    fun escapeCssIdentifier() {
        // combine both sequences
        val allCases = escapeCssIdentifier_WebPlatformTestParameters() + escapeCssIdentifier_additionalParameters()

        allCases.forEach { (expected, input) -> {
//            println("expected: $expected, input: $input")
                assertEquals(
                    expected,
                    TokenQueue.escapeCssIdentifier(input)
                )
            }
        }
    }

    @Test
    fun chompToIgnoreCase() {
        val t = "<textarea>one < two </TEXTarea>"
        var tq = TokenQueue(t)
        var data = tq.chompToIgnoreCase("</textarea")
        assertEquals("<textarea>one < two ", data)
        tq = TokenQueue("<textarea> one two < three </oops>")
        data = tq.chompToIgnoreCase("</textarea")
        assertEquals("<textarea> one two < three </oops>", data)
    }

    @Test
    fun consumeToIgnoreSecondCallTest() {
        val t = "<textarea>one < two </TEXTarea> third </TEXTarea>"
        val tq = TokenQueue(t)
        var data = tq.chompToIgnoreCase("</textarea>")
        assertEquals("<textarea>one < two ", data)
        data = tq.chompToIgnoreCase("</textarea>")
        assertEquals(" third ", data)
    }

    @Test
    fun testNestedQuotes() {
        validateNestedQuotes(
            "<html><body><a id=\"identifier\" onclick=\"func('arg')\" /></body></html>",
            "a[onclick*=\"('arg\"]",
        )
        validateNestedQuotes(
            "<html><body><a id=\"identifier\" onclick=func('arg') /></body></html>",
            "a[onclick*=\"('arg\"]",
        )
        validateNestedQuotes(
            "<html><body><a id=\"identifier\" onclick='func(\"arg\")' /></body></html>",
            "a[onclick*='(\"arg']",
        )
        validateNestedQuotes(
            "<html><body><a id=\"identifier\" onclick=func(\"arg\") /></body></html>",
            "a[onclick*='(\"arg']",
        )
    }

    @Test
    fun chompBalancedThrowIllegalArgumentException() {
        try {
            val tq = TokenQueue("unbalanced(something(or another)) else")
            tq.consumeTo("(")
            tq.chompBalanced('(', '+')
            fail("should have thrown IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Did not find balanced marker at 'something(or another)) else'", expected.message)
        }
    }

    @Test
    fun testQuotedPattern() {
        val doc = Ksoup.parse("<div>\\) foo1</div><div>( foo2</div><div>1) foo3</div>");
        assertEquals("\\) foo1", doc.select("div:matches(" + Regex.escape("\\)") + ")")[0].childNode(0).toString());
        assertEquals("( foo2", doc.select("div:matches(" + Regex.escape("(") + ")")[0].childNode(0).toString());
        assertEquals("1) foo3", doc.select("div:matches(" + Regex.escape("1)") + ")")[0].childNode(0).toString());
    }

    @Test
    fun consumeEscapedTag() {
        val q = TokenQueue("p\\\\p p\\.p p\\:p p\\!p")
        assertEquals("p\\p", q.consumeElementSelector())
        assertTrue(q.consumeWhitespace())
        assertEquals("p.p", q.consumeElementSelector())
        assertTrue(q.consumeWhitespace())
        assertEquals("p:p", q.consumeElementSelector())
        assertTrue(q.consumeWhitespace())
        assertEquals("p!p", q.consumeElementSelector())
        assertTrue(q.isEmpty())
    }

    @Test
    fun consumeEscapedId() {
        val q = TokenQueue("i\\.d i\\\\d")
        assertEquals("i.d", q.consumeCssIdentifier())
        assertTrue(q.consumeWhitespace())
        assertEquals("i\\d", q.consumeCssIdentifier())
        assertTrue(q.isEmpty())
    }

    @Test
    fun escapeAtEof() {
        val q = TokenQueue("Foo\\")
        val s = q.consumeElementSelector()
        assertEquals("Foo", s) // no escape, no eof. Just straight up Foo.
    }

    @Test
    fun consumeCssIdentifier_WebPlatformTests() {
        val allCases = cssIdentifiers() + cssAdditionalIdentifiers()
        allCases.forEach { (expected, cssIdentifier) ->
            assertParsedCssIdentifierEquals(expected, cssIdentifier)
        }
    }


    @Test
    fun consumeCssIdentifierWithEmptyInput() {
        val emptyQueue = TokenQueue("")
        val exception = assertFailsWith<IllegalArgumentException> { emptyQueue.consumeCssIdentifier() }
        assertEquals("CSS identifier expected, but end of input found", exception.message)
    }

    // Some of jsoup's tests depend on this behavior
    @Test
    fun consumeCssIdentifier_invalidButSupportedForBackwardsCompatibility() {
        assertParsedCssIdentifierEquals("1", "1")
        assertParsedCssIdentifierEquals("-", "-")
        assertParsedCssIdentifierEquals("-1", "-1")
    }

    companion object {
        private fun validateNestedQuotes(html: String, selector: String) {
            assertEquals(
                "#identifier",
                Ksoup.parse(html).select(selector).first()!!
                    .cssSelector(),
            )
        }

        // https://github.com/web-platform-tests/wpt/blob/328fa1c67bf5dfa6f24571d4c41dd10224b6d247/css/cssom/escape.html
        @JvmStatic
        fun escapeCssIdentifier_WebPlatformTestParameters(): Sequence<Pair<String, String>> = sequenceOf(
            // Empty
            "" to "",

            // Null bytes
            "\uFFFD" to "\u0000",
            "a\uFFFD" to "a\u0000",
            "\uFFFDb" to "\u0000b",
            "a\uFFFDb" to "a\u0000b",

            // Replacement character
            "\uFFFD" to "\uFFFD",
            "a\uFFFD" to "a\uFFFD",
            "\uFFFDb" to "\uFFFDb",
            "a\uFFFDb" to "a\uFFFDb",

            // Number prefix
            "\\30 a" to "0a",
            "\\31 a" to "1a",
            "\\32 a" to "2a",
            "\\33 a" to "3a",
            "\\34 a" to "4a",
            "\\35 a" to "5a",
            "\\36 a" to "6a",
            "\\37 a" to "7a",
            "\\38 a" to "8a",
            "\\39 a" to "9a",

            // Letter-number prefix
            "a0b" to "a0b",
            "a1b" to "a1b",
            "a2b" to "a2b",
            "a3b" to "a3b",
            "a4b" to "a4b",
            "a5b" to "a5b",
            "a6b" to "a6b",
            "a7b" to "a7b",
            "a8b" to "a8b",
            "a9b" to "a9b",

            // Dash-number prefix
            "-\\30 a" to "-0a",
            "-\\31 a" to "-1a",
            "-\\32 a" to "-2a",
            "-\\33 a" to "-3a",
            "-\\34 a" to "-4a",
            "-\\35 a" to "-5a",
            "-\\36 a" to "-6a",
            "-\\37 a" to "-7a",
            "-\\38 a" to "-8a",
            "-\\39 a" to "-9a",

            // Double-dash prefix
            "--a" to "--a",

            // Miscellaneous
            "\\1 \\2 \\1e \\1f " to "\u0001\u0002\u001E\u001F",
            "\u0080\u002D\u005F\u00A9" to "\u0080\u002D\u005F\u00A9",
            "\\7f \u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u008B\u008C\u008D\u008E\u008F\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A\u009B\u009C\u009D\u009E\u009F" to
                    "\u007F\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u008B\u008C\u008D\u008E\u008F\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A\u009B\u009C\u009D\u009E\u009F",
            "\u00A0\u00A1\u00A2" to "\u00A0\u00A1\u00A2",
            "a0123456789b" to "a0123456789b",
            "abcdefghijklmnopqrstuvwxyz" to "abcdefghijklmnopqrstuvwxyz",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",

            "hello\\\\world" to "hello\\world",       // Backslashes escape
            "hello\u1234world" to "hello\u1234world", // Preservation > U+0080
            "\\-" to "-",                             // Single dash escaped

            "\\ \\!xy" to "\u0020\u0021\u0078\u0079",

            // Astral symbol and lone surrogates
            "\uD834\uDF06" to "\uD834\uDF06",
            "\uDF06" to "\uDF06",
            "\uD834" to "\uD834"
        )

        @JvmStatic
        fun escapeCssIdentifier_additionalParameters(): Sequence<Pair<String, String>> = sequenceOf(
            "one\\#two\\.three\\/four\\\\five" to "one#two.three/four\\five",
            "-a" to "-a",
            "--" to "--"
        )

        @JvmStatic
        fun cssIdentifiers(): Sequence<Pair<String, String>> = sequenceOf(
            // https://github.com/web-platform-tests/wpt/blob/36036fb5212a3fc15fc5750cecb1923ba4071668/dom/nodes/ParentNode-querySelector-escapes.html
            // — escape hex digit
            "0nextIsWhiteSpace" to "\\30 nextIsWhiteSpace",
            "0nextIsNotHexLetters" to "\\30nextIsNotHexLetters",
            "0connectHexMoreThan6Hex" to "\\000030connectHexMoreThan6Hex",
            "0spaceMoreThan6Hex" to "\\000030 spaceMoreThan6Hex",

            // — hex digit special replacement
            "zero�" to "zero\\0",
            "zero�" to "zero\\000000",
            "�surrogateFirst" to "\\d83d surrogateFirst",
            "surrogateSecond�" to "surrogateSecond\\dd11",
            "surrogatePair��" to "surrogatePair\\d83d\\dd11",
            "outOfRange�" to "outOfRange\\110000",
            "outOfRange�" to "outOfRange\\110030",
            "outOfRange�" to "outOfRange\\555555",
            "outOfRange�" to "outOfRange\\ffffff",

            // — escape anything else
            ".comma" to "\\.comma",
            "-minus" to "\\-minus",
            "g" to "\\g",

            // non edge cases
            "aBMPRegular" to "\\61 BMPRegular",
            "🔑nonBMP" to "\\1f511 nonBMP",
            "00continueEscapes" to "\\30\\30 continueEscapes",
            "00continueEscapes" to "\\30 \\30 continueEscapes",
            "continueEscapes00" to "continueEscapes\\30 \\30 ",
            "continueEscapes00" to "continueEscapes\\30 \\30",
            "continueEscapes00" to "continueEscapes\\30\\30 ",
            "continueEscapes00" to "continueEscapes\\30\\30",

            // ident tests from Chromium CSS tests
            "hello" to "hel\\6Co",
            "&B" to "\\26 B",
            "hello" to "hel\\6C o",
            "spaces" to "spac\\65\r\ns",
            "spaces" to "sp\\61\tc\\65\u000cs",
            "test힙" to "test\\D799",
            "" to "\\E000",
            "test" to "te\\s\\t",
            "spaces in\tident" to "spaces\\ in\\\tident",
            ".,:!" to "\\.\\,\\:\\!",
            "null�" to "null\\0",
            "null�" to "null\\0000",
            "large�" to "large\\110000",
            "large�" to "large\\23456a",
            "surrogate�" to "surrogate\\D800",
            "surrogate�" to "surrogate\\0DBAC",
            "�surrogate" to "\\00DFFFsurrogate",
            "􏿿" to "\\10fFfF",
            "􏿿0" to "\\10fFfF0",
            "􀀀00" to "\\10000000",
            "eof�" to "eof\\",

            // simple identifiers
            "simple-ident" to "simple-ident",
            "testing123" to "testing123",
            "_underscore" to "_underscore",
            "-text" to "-text",
            "-m" to "-\\6d",
            "--abc" to "--abc",
            "--" to "--",
            "--11" to "--11",
            "---" to "---",
            " " to " ",
            " " to " ",
            "ሴ" to "ሴ",
            "𒍅" to "𒍅",
            "�" to " ",
            "ab�c" to "ab c"
        )

        @JvmStatic
        fun cssAdditionalIdentifiers(): Sequence<Pair<String, String>> = sequenceOf(
            "1st" to "\\31\r\nst",
            "1" to "\\31\r",
            "1a" to "\\31\ra",
            "1" to "\\031",
            "1" to "\\0031",
            "1" to "\\00031",
            "1" to "\\000031",
            "1" to "\\000031",
            "a" to "a\\\nb"
        )

        private fun parseCssIdentifier(text: String): String {
            val q = TokenQueue(text)
            return q.consumeCssIdentifier()
        }

        private fun assertParsedCssIdentifierEquals(expected: String?, cssIdentifier: String) {
            assertEquals(expected, parseCssIdentifier(cssIdentifier))
        }
    }
}
