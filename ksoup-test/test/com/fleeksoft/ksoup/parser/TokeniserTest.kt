package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.nodes.CDataNode
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.TextNode
import kotlin.test.*

class TokeniserTest {
    @BeforeTest
    fun initKsoup() {
        TestHelper.initKsoup()
    }

    @Test
    fun bufferUpInAttributeVal() {

        // check each double, singlem, unquoted impls
        val quotes = arrayOf("\"", "'", "")
        for (quote in quotes) {
            val preamble = "<img src=$quote"
            val tail = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
            val sb = StringBuilder(preamble)
            val charsToFillBuffer = CharacterReader.maxBufferLen - preamble.length
            for (i in 0 until charsToFillBuffer) {
                sb.append('a')
            }
            sb.append('X') // First character to cross character buffer boundary
            sb.append(tail).append(quote).append(">\n")
            val html = sb.toString()
            val doc = Ksoup.parse(html)
            val src = doc.select("img").attr("src")
            assertTrue(src.contains("X"), "Handles for quote $quote")
            assertTrue(src.contains(tail))
        }
    }

    @Test
    fun handleSuperLargeTagNames() {
        // unlikely, but valid. so who knows.
        val sb = StringBuilder(CharacterReader.maxBufferLen)
        do {
            sb.append("LargeTagName")
        } while (sb.length < CharacterReader.maxBufferLen)
        val tag = sb.toString()
        val html = "<$tag>One</$tag>"
        val doc = Parser.htmlParser().settings(ParseSettings.preserveCase).parseInput(html, "")
        val els = doc.select(tag)
        assertEquals(1, els.size)
        val el = els.first()
        assertNotNull(el)
        assertEquals("One", el.text())
        assertEquals(tag, el.tagName())
    }

    @Test
    fun handleSuperLargeAttributeName() {
        val sb = StringBuilder(CharacterReader.maxBufferLen)
        do {
            sb.append("LargAttributeName")
        } while (sb.length < CharacterReader.maxBufferLen)
        val attrName = sb.toString()
        val html = "<p $attrName=foo>One</p>"
        val doc = Ksoup.parse(html)
        val els = doc.getElementsByAttribute(attrName)
        assertEquals(1, els.size)
        val el = els.first()
        assertNotNull(el)
        assertEquals("One", el.text())
        val (key, value) = el.attributes().asList()[0]
        assertEquals(attrName.lowercase(), key)
        assertEquals("foo", value)
    }

    @Test
    fun handleLargeText() {
        val sb = StringBuilder(CharacterReader.maxBufferLen)
        do {
            sb.append("A Large Amount of Text")
        } while (sb.length < CharacterReader.maxBufferLen)
        val text = sb.toString()
        val html = "<p>$text</p>"
        val doc = Ksoup.parse(html)
        val els = doc.select("p")
        assertEquals(1, els.size)
        val el = els.first()
        assertNotNull(el)
        assertEquals(text, el.text())
    }

    @Test
    fun handleLargeComment() {
        val sb = StringBuilder(CharacterReader.maxBufferLen)
        do {
            sb.append("Quite a comment ")
        } while (sb.length < CharacterReader.maxBufferLen)
        val comment = sb.toString()
        val html = "<p><!-- $comment --></p>"
        val doc = Ksoup.parse(html)
        val els = doc.select("p")
        assertEquals(1, els.size)
        val el = els.first()
        assertNotNull(el)
        val child = el.childNode(0) as Comment
        assertEquals(" $comment ", child.getData())
    }

    @Test
    fun handleLargeCdata() {
        val sb = StringBuilder(CharacterReader.maxBufferLen)
        do {
            sb.append("Quite a lot of CDATA <><><><>")
        } while (sb.length < CharacterReader.maxBufferLen)
        val cdata = sb.toString()
        val html = "<p><![CDATA[$cdata]]></p>"
        val doc = Ksoup.parse(html)
        val els = doc.select("p")
        assertEquals(1, els.size)
        val el = els.first()
        assertNotNull(el)
        val child = el.childNode(0) as TextNode
        assertEquals(cdata, el.text())
        assertEquals(cdata, child.getWholeText())
    }

    @Test
    fun handleLargeTitle() {
        val sb = StringBuilder(CharacterReader.maxBufferLen)
        do {
            sb.append("Quite a long title")
        } while (sb.length < CharacterReader.maxBufferLen)
        val title = sb.toString()
        val html = "<title>$title</title>"
        val doc = Ksoup.parse(html)
        val els = doc.select("title")
        assertEquals(1, els.size)
        val el = els.first()
        assertNotNull(el)
        val child = el.childNode(0) as TextNode
        assertEquals(title, el.text())
        assertEquals(title, child.getWholeText())
        assertEquals(title, doc.title())
    }

    @Test
    fun cp1252Entities() {
        assertEquals("\u20ac", Ksoup.parse("&#0128;").text())
        assertEquals("\u201a", Ksoup.parse("&#0130;").text())
        assertEquals("\u20ac", Ksoup.parse("&#x80;").text())
    }

    @Test
    fun cp1252EntitiesProduceError() {
        val parser = Parser(HtmlTreeBuilder())
        parser.setTrackErrors(10)
        assertEquals("\u20ac", parser.parseInput("<html><body>&#0128;</body></html>", "").text())
        assertEquals(1, parser.getErrors().size)
    }

    @Test
    fun cp1252SubstitutionTable() {
        for (i in Tokeniser.win1252Extensions.indices) {
            val s = byteArrayOf((i + Tokeniser.win1252ExtensionsStart).toByte()).decodeToString()
            // TODO: check it
//                String(byteArrayOf((i + Tokeniser.win1252ExtensionsStart).toByte()), Charsets.forName("Windows-1252"))
            assertEquals(1, s.length)

            // some of these characters are illegal
            if (s[0] == '\ufffd') {
                continue
            }
            assertEquals(s[0].code, Tokeniser.win1252Extensions[i], "At: $i")
        }
    }

    @Test
    fun canParseVeryLongBogusComment() {
        val commentData = StringBuilder(CharacterReader.maxBufferLen)
        do {
            commentData.append("blah blah blah blah ")
        } while (commentData.length < CharacterReader.maxBufferLen)
        val expectedCommentData = commentData.toString()
        val testMarkup = "<html><body><!$expectedCommentData></body></html>"
        val parser = Parser(HtmlTreeBuilder())
        val doc = parser.parseInput(testMarkup, "")
        val commentNode = doc.body().childNode(0)
        assertTrue(commentNode is Comment, "Expected comment node")
        assertEquals(expectedCommentData, commentNode.getData())
    }

    @Test
    fun canParseCdataEndingAtEdgeOfBuffer() {
        val cdataStart = "<![CDATA["
        val cdataEnd = "]]>"
        val bufLen =
            CharacterReader.maxBufferLen - cdataStart.length - 1 // also breaks with -2, but not with -3 or 0
        val cdataContentsArray = CharArray(bufLen)
        cdataContentsArray.fill('x')
        val cdataContents = cdataContentsArray.concatToString()
        val testMarkup = cdataStart + cdataContents + cdataEnd
        val parser = Parser(HtmlTreeBuilder())
        val doc = parser.parseInput(testMarkup, "")
        val cdataNode = doc.body().childNode(0)
        assertTrue(cdataNode is CDataNode, "Expected CDATA node")
        assertEquals(cdataContents, cdataNode.text())
    }
}
