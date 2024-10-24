package com.fleeksoft.ksoup.parser

import com.fleeksoft.io.StringReader
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.exception.UncheckedIOException
import com.fleeksoft.ksoup.internal.StringUtil
import kotlinx.coroutines.test.runTest
import kotlin.test.*


/**
 * Test suite for character reader.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class CharacterReaderTest {

    @Test
    fun testReadMixSpecialChar() {
        val input = "ä<a>ä</a>"
        val charReader = CharacterReader(StringReader(input))
        input.forEachIndexed { index, char ->
            assertEquals(index, charReader.pos())
            assertEquals(char, charReader.consume())
        }
    }

    @Test
    fun testSpecialCharacterReader() {
        val specialText1 = "Hello &amp;&lt;&gt; Å å π 新 there ¾ © »"
        val specialText2 = "Übergrößenträger"

        assertEquals(specialText1, CharacterReader(specialText1).toString())
        assertEquals(specialText2, CharacterReader(specialText2).toString())
    }

    @Test
    fun testUtf8Reader() {
        val test71540chars = "\ud869\udeb2\u304b\u309a  1"
        val extractedText = CharacterReader(test71540chars).toString()
        assertEquals(test71540chars, extractedText)
    }

    @Test
    fun testStrReader() {
        val test71540chars = "Abccdddd  1"
        val extractedText = CharacterReader(test71540chars).toString()
        assertEquals(test71540chars, extractedText)
    }

    @Test
    fun consume() {
        val r = CharacterReader("one")
        assertEquals(0, r.pos())
        assertEquals('o', r.current())
        assertEquals('o', r.consume())
        assertEquals(1, r.pos())
        assertEquals('n', r.current())
        assertEquals(1, r.pos())
        assertEquals('n', r.consume())
        assertEquals('e', r.consume())
        assertTrue(r.isEmpty())
        assertEquals(CharacterReader.EOF, r.consume())
        assertTrue(r.isEmpty())
        assertEquals(CharacterReader.EOF, r.consume())
    }

    @Test
    fun unconsume() {
        val r = CharacterReader("one")
        assertEquals('o', r.consume())
        assertEquals('n', r.current())
        r.unconsume()
        assertEquals('o', r.current())
        assertEquals('o', r.consume())
        assertEquals('n', r.consume())
        assertEquals('e', r.consume())
        assertTrue(r.isEmpty())
        r.unconsume()
        assertFalse(r.isEmpty())
        assertEquals('e', r.current())
        assertEquals('e', r.consume())
        assertTrue(r.isEmpty())
        assertEquals(CharacterReader.EOF, r.consume())
        r.unconsume() // read past, so have to eat again
        assertTrue(r.isEmpty())
        r.unconsume()
        assertFalse(r.isEmpty())
        assertEquals('e', r.consume())
        assertTrue(r.isEmpty())
        assertEquals(CharacterReader.EOF, r.consume())
        assertTrue(r.isEmpty())

        // unconsume all remaining characters
        for (i in 0..3) {
            r.unconsume()
        }
        assertFailsWith<UncheckedIOException> { r.unconsume() }
    }

    @Test
    fun mark() {
        val r = CharacterReader("one")
        r.consume()
        r.mark()
        assertEquals(1, r.pos())
        assertEquals('n', r.consume())
        assertEquals('e', r.consume())
        assertTrue(r.isEmpty())
        r.rewindToMark()
        assertEquals(1, r.pos())
        assertEquals('n', r.consume())
        assertFalse(r.isEmpty())
        assertEquals(2, r.pos())
    }

    @Test
    fun rewindToMark() {
        val r = CharacterReader("nothing")
        // marking should be invalid
        assertFailsWith<UncheckedIOException> { r.rewindToMark() }
    }

    @Test
    fun consumeToEnd() {
        val input = "one two three"
        val r = CharacterReader(input)
        val toEnd = r.consumeToEnd()
        assertEquals(input, toEnd)
        assertTrue(r.isEmpty())
    }

    @Test
    fun nextIndexOfChar() {
        val input = "blah blah"
        val r = CharacterReader(input)
        assertEquals(-1, r.nextIndexOf('x'))
        assertEquals(3, r.nextIndexOf('h'))
        val pull = r.consumeTo('h')
        assertEquals("bla", pull)
        r.consume()
        assertEquals(2, r.nextIndexOf('l'))
        assertEquals(" blah", r.consumeToEnd())
        assertEquals(-1, r.nextIndexOf('x'))
    }

    @Test
    fun nextIndexOfString() {
        val `in` = "One Two something Two Three Four"
        val r = CharacterReader(`in`)
        assertEquals(-1, r.nextIndexOf("Foo"))
        assertEquals(4, r.nextIndexOf("Two"))
        assertEquals("One Two ", r.consumeTo("something"))
        assertEquals(10, r.nextIndexOf("Two"))
        assertEquals("something Two Three Four", r.consumeToEnd())
        assertEquals(-1, r.nextIndexOf("Two"))
    }

    @Test
    fun nextIndexOfUnmatched() {
        val r = CharacterReader("<[[one]]")
        assertEquals(-1, r.nextIndexOf("]]>"))
    }

    @Test
    fun consumeToChar() {
        val r = CharacterReader("One Two Three")
        assertEquals("One ", r.consumeTo('T'))
        assertEquals("", r.consumeTo('T')) // on Two
        assertEquals('T', r.consume())
        assertEquals("wo ", r.consumeTo('T'))
        assertEquals('T', r.consume())
        assertEquals("hree", r.consumeTo('T')) // consume to end
    }

    @Test
    fun consumeToString() {
        val r = CharacterReader("One Two Two Four")
        assertEquals("One ", r.consumeTo("Two"))
        assertEquals('T', r.consume())
        assertEquals("wo ", r.consumeTo("Two"))
        assertEquals('T', r.consume())
        // To handle strings straddling across buffers, consumeTo() may return the
        // data in multiple pieces near EOF.
        val builder = StringBuilder()
        var part: String
        do {
            part = r.consumeTo("Qux")
            builder.append(part)
        } while (part.isNotEmpty())
        assertEquals("wo Four", builder.toString())
    }

    @Test
    fun advance() {
        val r = CharacterReader("One Two Three")
        assertEquals('O', r.consume())
        r.advance()
        assertEquals('e', r.consume())
    }

    @Test
    fun consumeToAny() {
        val r = CharacterReader("One &bar; qux")
        assertEquals("One ", r.consumeToAny('&', ';'))
        assertTrue(r.matches('&'))
        assertTrue(r.matches("&bar;"))
        assertEquals('&', r.consume())
        assertEquals("bar", r.consumeToAny('&', ';'))
        assertEquals(';', r.consume())
        assertEquals(" qux", r.consumeToAny('&', ';'))
    }

    @Test
    fun consumeLetterSequence() {
        val r = CharacterReader("One &bar; qux")
        assertEquals("One", r.consumeLetterSequence())
        assertEquals(" &", r.consumeTo("bar;"))
        assertEquals("bar", r.consumeLetterSequence())
        assertEquals("; qux", r.consumeToEnd())
    }

    @Test
    fun consumeLetterThenDigitSequence() {
        val r = CharacterReader("One12 Two &bar; qux")
        assertEquals("One12", r.consumeLetterThenDigitSequence())
        assertEquals(' ', r.consume())
        assertEquals("Two", r.consumeLetterThenDigitSequence())
        assertEquals(" &bar; qux", r.consumeToEnd())
    }

    @Test
    fun matches() {
        val r = CharacterReader("One Two Three")
        assertTrue(r.matches('O'))
        assertTrue(r.matches("One Two Three"))
        assertTrue(r.matches("One"))
        assertFalse(r.matches("one"))
        assertEquals('O', r.consume())
        assertFalse(r.matches("One"))
        assertTrue(r.matches("ne Two Three"))
        assertFalse(r.matches("ne Two Three Four"))
        assertEquals("ne Two Three", r.consumeToEnd())
        assertFalse(r.matches("ne"))
        assertTrue(r.isEmpty())
    }

    @Test
    fun matchesIgnoreCase() {
        val r = CharacterReader("One Two Three")
        assertTrue(r.matchesIgnoreCase("O"))
        assertTrue(r.matchesIgnoreCase("o"))
        assertTrue(r.matches('O'))
        assertFalse(r.matches('o'))
        assertTrue(r.matchesIgnoreCase("One Two Three"))
        assertTrue(r.matchesIgnoreCase("ONE two THREE"))
        assertTrue(r.matchesIgnoreCase("One"))
        assertTrue(r.matchesIgnoreCase("one"))
        assertEquals('O', r.consume())
        assertFalse(r.matchesIgnoreCase("One"))
        assertTrue(r.matchesIgnoreCase("NE Two Three"))
        assertFalse(r.matchesIgnoreCase("ne Two Three Four"))
        assertEquals("ne Two Three", r.consumeToEnd())
        assertFalse(r.matchesIgnoreCase("ne"))
    }

    @Test
    fun containsIgnoreCase() {
        val r = CharacterReader("One TWO three")
        assertTrue(r.containsIgnoreCase("two"))
        assertTrue(r.containsIgnoreCase("three"))
        // weird one: does not find one, because it scans for consistent case only
        assertFalse(r.containsIgnoreCase("one"))
    }

    @Test
    fun containsIgnoreCaseBuffer() {
        val html =
            "<p><p><p></title><p></TITLE><p>" + bufferBuster("Foo Bar Qux ") + "<foo><bar></title>"
        val r = CharacterReader(html)
        assertTrue(r.containsIgnoreCase("</title>"))
        assertFalse(r.containsIgnoreCase("</not>"))
        assertFalse(r.containsIgnoreCase("</not>")) // cached, but we only test functionally here
        assertTrue(r.containsIgnoreCase("</title>"))
        r.consumeTo("</title>")
        assertTrue(r.containsIgnoreCase("</title>"))
        r.consumeTo("<p>")
        assertTrue(r.matches("<p>"))
        assertTrue(r.containsIgnoreCase("</title>"))
        assertTrue(r.containsIgnoreCase("</title>"))
        assertFalse(r.containsIgnoreCase("</not>"))
        assertFalse(r.containsIgnoreCase("</not>"))
        r.consumeTo("</TITLE>")
        r.consumeTo("<p>")
        assertTrue(r.matches("<p>"))
        assertFalse(r.containsIgnoreCase("</title>")) // because we haven't buffered up yet, we don't know
        r.consumeTo("<foo>")
        assertFalse(r.matches("<foo>")) // buffer underrun
        r.consumeTo("<foo>")
        assertTrue(r.matches("<foo>")) // cross the buffer
        assertTrue(r.containsIgnoreCase("</TITLE>"))
        assertTrue(r.containsIgnoreCase("</title>"))
    }

    @Test
    fun matchesAny() {
        val scan = charArrayOf(' ', '\n', '\t')
        val r = CharacterReader("One\nTwo\tThree")
        assertFalse(r.matchesAny(*scan))
        assertEquals("One", r.consumeToAny(*scan))
        assertTrue(r.matchesAny(*scan))
        assertEquals('\n', r.consume())
        assertFalse(r.matchesAny(*scan))
        // nothing to match
        r.consumeToEnd()
        assertTrue(r.isEmpty())
        assertFalse(r.matchesAny(*scan))
    }

    @Test
    fun matchesDigit() {
        val r = CharacterReader("42")
        r.consumeToEnd()
        assertTrue(r.isEmpty())
        // nothing to match
        assertFalse(r.matchesDigit())
        r.unconsume()
        assertTrue(r.matchesDigit())
    }

    @Test
    fun cachesStrings() {
        val r = CharacterReader("Check\tCheck\tCheck\tCHOKE\tA string that is longer than 16 chars")
        val one = r.consumeTo('\t')
        r.consume()
        val two = r.consumeTo('\t')
        r.consume()
        val three = r.consumeTo('\t')
        r.consume()
        val four = r.consumeTo('\t')
        r.consume()
        val five = r.consumeTo('\t')
        assertEquals("Check", one)
        assertEquals("Check", two)
        assertEquals("Check", three)
        assertEquals("CHOKE", four)
        assertSame(one, two)
        assertSame(two, three)
        assertNotSame(three, four)
        assertNotSame(four, five)
        assertEquals(five, "A string that is longer than 16 chars")
    }

    @Test
    fun rangeEquals() {
        val r = CharacterReader("Check\tCheck\tCheck\tCHOKE")
        assertTrue(r.rangeEquals(0, 5, "Check"))
        assertFalse(r.rangeEquals(0, 5, "CHOKE"))
        assertFalse(r.rangeEquals(0, 5, "Chec"))
        assertTrue(r.rangeEquals(6, 5, "Check"))
        assertFalse(r.rangeEquals(6, 5, "Chuck"))
        assertTrue(r.rangeEquals(12, 5, "Check"))
        assertFalse(r.rangeEquals(12, 5, "Cheeky"))
        assertTrue(r.rangeEquals(18, 5, "CHOKE"))
        assertFalse(r.rangeEquals(18, 5, "CHIKE"))
    }

    @Test
    fun empty() {
        var r = CharacterReader("One")
        assertTrue(r.matchConsume("One"))
        assertTrue(r.isEmpty())
        r = CharacterReader("Two")
        val two = r.consumeToEnd()
        assertEquals("Two", two)
    }

    @Test
    fun consumeToNonexistentEndWhenAtAnd() {
        val r = CharacterReader("<!")
        assertTrue(r.matchConsume("<!"))
        assertTrue(r.isEmpty())
        val after = r.consumeTo('>')
        assertEquals("", after)
        assertTrue(r.isEmpty())
    }

    @Test
    fun notEmptyAtBufferSplitPoint() {
        val len = CharacterReader.BufferSize * 12
        val builder: StringBuilder = StringUtil.borrowBuilder()
        while (builder.length <= len) builder.append('!')
        val r = CharacterReader(builder.toString())
        StringUtil.releaseBuilder(builder)


        // consume through
        for (pos in 0 until len) {
            assertEquals(pos, r.pos())
            assertFalse(r.isEmpty())
            assertEquals('!', r.consume())
            assertEquals(pos + 1, r.pos())
            assertFalse(r.isEmpty())
        }
        assertEquals('!', r.consume())
        assertTrue(r.isEmpty())
        assertEquals(CharacterReader.EOF, r.consume())
    }

    @Test
    fun bufferUp() {
        val note = "HelloThere" // + ! = 11 chars
        val loopCount = 64
        val sb = StringBuilder()
        for (i in 0 until loopCount) {
            sb.append(note)
            sb.append("!")
        }
        val s = sb.toString()
        val r = CharacterReader(s)
        for (i in 0 until loopCount) {
            val pull = r.consumeTo('!')
            assertEquals(note, pull)
            assertEquals('!', r.current())
            r.advance()
        }
        assertTrue(r.isEmpty())
    }

    @Test
    fun canEnableAndDisableLineNumberTracking() {
        val reader = CharacterReader("Hello!")
        assertFalse(reader.isTrackNewlines())
        reader.trackNewlines(true)
        assertTrue(reader.isTrackNewlines())
        reader.trackNewlines(false)
        assertFalse(reader.isTrackNewlines())
    }

    @Test
    fun canTrackNewlines() {
        val builder = StringBuilder()
        builder.append("<foo>\n<bar>\n<qux>\n")
        while (builder.length < CharacterReader.BufferSize) {
            builder.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
        }
        builder.append("[foo]\n[bar]")
        val content = builder.toString() // 32789
        val noTrack = CharacterReader(content)
        assertFalse(noTrack.isTrackNewlines())

        // check that no tracking works as expected (pos is 0 indexed, line number stays at 1, col is pos+1)
        assertEquals(0, noTrack.pos())
        assertEquals(1, noTrack.lineNumber())
        assertEquals(1, noTrack.columnNumber())
        noTrack.consumeTo("<qux>")
        assertEquals(12, noTrack.pos())
        assertEquals(1, noTrack.lineNumber())
        assertEquals(13, noTrack.columnNumber())
        assertEquals("1:13", noTrack.posLineCol())
        // get over the buffer
        while (!noTrack.matches("[foo]")) noTrack.consumeTo("[foo]")
        assertEquals(2090, noTrack.pos())
        assertEquals(1, noTrack.lineNumber())
        assertEquals(noTrack.pos() + 1, noTrack.columnNumber())
        assertEquals("1:2091", noTrack.posLineCol())

        val track = CharacterReader(content)
        track.trackNewlines(true)
        assertTrue(track.isTrackNewlines())

        // and the line numbers: "<foo>\n<bar>\n<qux>\n"
        assertEquals(0, track.pos())
        assertEquals(1, track.lineNumber())
        assertEquals(1, track.columnNumber())
        track.consumeTo('\n')
        assertEquals(1, track.lineNumber())
        assertEquals(6, track.columnNumber())
        track.consume()
        assertEquals(2, track.lineNumber())
        assertEquals(1, track.columnNumber())
        assertEquals("<bar>", track.consumeTo('\n'))
        assertEquals(2, track.lineNumber())
        assertEquals(6, track.columnNumber())
        assertEquals("\n", track.consumeTo("<qux>"))
        assertEquals(12, track.pos())
        assertEquals(3, track.lineNumber())
        assertEquals(1, track.columnNumber())
        assertEquals("3:1", track.posLineCol())
        assertEquals("<qux>", track.consumeTo('\n'))
        assertEquals("3:6", track.posLineCol())
        // get over the buffer
        while (!track.matches("[foo]")) track.consumeTo("[foo]")
        assertEquals(2090, track.pos())
        assertEquals(4, track.lineNumber())
        assertEquals(2073, track.columnNumber())
        assertEquals("4:2073", track.posLineCol())
        track.consumeTo('\n')
        assertEquals("4:2078", track.posLineCol())
        track.consumeTo("[bar]")
        assertEquals(5, track.lineNumber())
        assertEquals("5:1", track.posLineCol())
        track.consumeToEnd()
        assertEquals("5:6", track.posLineCol())
    }

    @Test
    fun countsColumnsOverBufferWhenNoNewlines() {
        val builder = StringBuilder()
        while (builder.length < CharacterReader.BufferSize * 4) builder.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
        val content = builder.toString()
        val reader = CharacterReader(content)
        reader.trackNewlines(true)
        assertEquals("1:1", reader.posLineCol())
        val seen = StringBuilder()
        while (!reader.isEmpty()) seen.append(reader.consume())
        assertEquals(content, seen.toString())
        assertEquals(content.length, reader.pos())
        assertEquals(reader.pos() + 1, reader.columnNumber())
        assertEquals(1, reader.lineNumber())
    }

    @Test
    fun lineNumbersAgreeWithEditor() = runTest {
        val content: String = TestHelper.readResourceAsString("htmltests/large.html.gz")
        val reader = CharacterReader(content)
        reader.trackNewlines(true)
        val scan = "<p>VESTIBULUM" // near the end of the file
        while (!reader.matches(scan)) reader.consumeTo(scan)
        assertEquals(280218, reader.pos())
        assertEquals(1002, reader.lineNumber())
        assertEquals(1, reader.columnNumber())
        reader.consumeTo(' ')
        assertEquals(1002, reader.lineNumber())
        assertEquals(14, reader.columnNumber())
    }

    @Test
    fun consumeDoubleQuotedAttributeConsumesThruSingleQuote() {
        val html = "He'llo\" >"
        val r = CharacterReader(html)
        assertEquals("He'llo", r.consumeAttributeQuoted(false))
        assertEquals('"', r.consume())
    }

    @Test
    fun consumeSingleQuotedAttributeConsumesThruDoubleQuote() {
        val html = "He\"llo' >"
        val r = CharacterReader(html)
        assertEquals("He\"llo", r.consumeAttributeQuoted(true))
        assertEquals('\'', r.consume())
    }

    @Test
    fun consumeDoubleQuotedAttributeConsumesThruSingleQuoteToAmp() {
        val html = "He'llo &copy;\" >"
        val r = CharacterReader(html)
        assertEquals("He'llo ", r.consumeAttributeQuoted(false))
        assertEquals('&', r.consume())
    }

    @Test
    fun consumeSingleQuotedAttributeConsumesThruDoubleQuoteToAmp() {
        val html = "He\"llo &copy;' >"
        val r = CharacterReader(html)
        assertEquals("He\"llo ", r.consumeAttributeQuoted(true))
        assertEquals('&', r.consume())
    }

    companion object {
        fun bufferBuster(content: String): String {
            val builder = StringBuilder()
            while (builder.length < CharacterReader.BufferSize) builder.append(content)
            return builder.toString()
        }
    }
}
