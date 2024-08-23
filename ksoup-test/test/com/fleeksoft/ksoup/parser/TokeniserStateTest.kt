package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.TextNode
import com.fleeksoft.ksoup.select.Elements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokeniserStateTest {

    private val whiteSpace = charArrayOf('\t', '\n', '\r', '\u000c', ' ')
    private val quote = charArrayOf('\'', '"')

    @Test
    fun ensureSearchArraysAreSorted() {
        val arrays =
            arrayOf(
                TokeniserState.attributeNameCharsSorted,
                TokeniserState.attributeValueUnquoted,
            )
        for (array in arrays) {
            val copy = array.copyOf(array.size)
            array.sort()
            assertTrue(array.contentEquals(copy))
        }
    }

    @Test
    fun testCharacterReferenceInRcdata() {
        val body = "<textarea>You&I</textarea>"
        val doc = Ksoup.parse(body)
        val els = doc.select("textarea")
        assertEquals("You&I", els.text())
    }

    @Test
    fun testBeforeTagName() {
        for (c in whiteSpace) {
            val body = "<div$c>test</div>"
            val doc = Ksoup.parse(body)
            val els = doc.select("div")
            assertEquals("test", els.text())
        }
    }

    @Test
    fun testEndTagOpen() {
        var body: String = "<div>hello world</"
        var doc: Document = Ksoup.parse(body)
        var els: Elements = doc.select("div")
        assertEquals("hello world</", els.text())
        body = "<div>hello world</div>"
        doc = Ksoup.parse(body)
        els = doc.select("div")
        assertEquals("hello world", els.text())
        body = "<div>fake</></div>"
        doc = Ksoup.parse(body)
        els = doc.select("div")
        assertEquals("fake", els.text())
        body = "<div>fake</?</div>"
        doc = Ksoup.parse(body)
        els = doc.select("div")
        assertEquals("fake", els.text())
    }

    @Test
    fun testRcdataLessthanSign() {
        var body: String = "<textarea><fake></textarea>"
        var doc: Document = Ksoup.parse(body)
        var els: Elements = doc.select("textarea")
        assertEquals("<fake>", els.text())
        body = "<textarea><open"
        doc = Ksoup.parse(body)
        els = doc.select("textarea")
        assertEquals("", els.text())
        body = "<textarea>hello world</?fake</textarea>"
        doc = Ksoup.parse(body)
        els = doc.select("textarea")
        assertEquals("hello world</?fake", els.text())
    }

    @Test
    fun testRCDATAEndTagName() {
        for (c in whiteSpace) {
            val body = "<textarea>data</textarea$c>"
            val doc = Ksoup.parse(body)
            val els = doc.select("textarea")
            assertEquals("data", els.text())
        }
    }

    @Test
    fun testCommentEndCoverage() {
        val html =
            "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --! --- --><p>Hello</p></body></html>"
        val doc = Ksoup.parse(html)
        val body = doc.body()
        val comment = body.childNode(1) as Comment
        assertEquals(" <table><tr><td></table> --! --- ", comment.getData())
        val p = body.child(1)
        val text = p.childNode(0) as TextNode
        assertEquals("Hello", text.getWholeText())
    }

    @Test
    fun testCommentEndBangCoverage() {
        val html =
            "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --!---!>--><p>Hello</p></body></html>"
        val doc = Ksoup.parse(html)
        val body = doc.body()
        val comment = body.childNode(1) as Comment
        assertEquals(" <table><tr><td></table> --!-", comment.getData())
        val p = body.child(1)
        val text = p.childNode(0) as TextNode
        assertEquals("Hello", text.getWholeText())
    }

    @Test
    fun testPublicIdentifiersWithWhitespace() {
        val expectedOutput = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0//EN\">"
        for (q in quote) {
            for (ws in whiteSpace) {
                val htmls =
                    arrayOf(
                        "<!DOCTYPE html${ws}PUBLIC $q-//W3C//DTD HTML 4.0//EN$q>",
                        "<!DOCTYPE html ${ws}PUBLIC $q-//W3C//DTD HTML 4.0//EN$q>",
                        "<!DOCTYPE html PUBLIC${ws}$q-//W3C//DTD HTML 4.0//EN$q>",
                        "<!DOCTYPE html PUBLIC ${ws}$q-//W3C//DTD HTML 4.0//EN$q>",
                        "<!DOCTYPE html PUBLIC $q-//W3C//DTD HTML 4.0//EN${q}$ws>",
                        "<!DOCTYPE html PUBLIC$q-//W3C//DTD HTML 4.0//EN${q}$ws>",
                    )
                for (html in htmls) {
                    val doc = Ksoup.parse(html)
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml())
                }
            }
        }
    }

    @Test
    fun testSystemIdentifiersWithWhitespace() {
        val expectedOutput = "<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/REC-html40/strict.dtd\">"
        for (q in quote) {
            for (ws in whiteSpace) {
                val htmls =
                    arrayOf(
                        "<!DOCTYPE html${ws}SYSTEM ${q}http://www.w3.org/TR/REC-html40/strict.dtd$q>",
                        "<!DOCTYPE html ${ws}SYSTEM ${q}http://www.w3.org/TR/REC-html40/strict.dtd$q>",
                        "<!DOCTYPE html SYSTEM${ws}${q}http://www.w3.org/TR/REC-html40/strict.dtd$q>",
                        "<!DOCTYPE html SYSTEM ${ws}${q}http://www.w3.org/TR/REC-html40/strict.dtd$q>",
                        "<!DOCTYPE html SYSTEM ${q}http://www.w3.org/TR/REC-html40/strict.dtd${q}$ws>",
                        "<!DOCTYPE html SYSTEM${q}http://www.w3.org/TR/REC-html40/strict.dtd${q}$ws>",
                    )
                for (html in htmls) {
                    val doc = Ksoup.parse(html)
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml())
                }
            }
        }
    }

    @Test
    fun testPublicAndSystemIdentifiersWithWhitespace() {
        val expectedOutput = (
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0//EN\"" +
                        " \"http://www.w3.org/TR/REC-html40/strict.dtd\">"
                )
        for (q in quote) {
            for (ws in whiteSpace) {
                val htmls =
                    arrayOf(
                        "<!DOCTYPE html PUBLIC $q-//W3C//DTD HTML 4.0//EN$q" +
                                "${ws}${q}http://www.w3.org/TR/REC-html40/strict.dtd$q>",
                        "<!DOCTYPE html PUBLIC $q-//W3C//DTD HTML 4.0//EN$q" +
                                "${q}http://www.w3.org/TR/REC-html40/strict.dtd$q>",
                    )
                for (html in htmls) {
                    val doc = Ksoup.parse(html)
                    assertEquals(expectedOutput, doc.childNode(0).outerHtml())
                }
            }
        }
    }

    @Test
    fun handlesLessInTagThanAsNewTag() {
        // out of spec, but clear author intent
        val html = "<p\n<p<div id=one <span>Two"
        val doc = Ksoup.parse(html)
        assertEquals(
            "<p></p><p></p><div id=\"one\"><span>Two</span></div>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testUnconsumeAtBufferBoundary() {
        val triggeringSnippet = "<a href=\"\"foo"
        val padding =
            CharArray(CharacterReader.RefillPoint - triggeringSnippet.length + 2) // The "foo" part must be just at the limit.
        padding.fill(' ')
        val paddedSnippet = padding.concatToString() + triggeringSnippet
        val errorList = ParseErrorList.tracking(1)
        Parser.parseFragment(paddedSnippet, null, "", errorList)
        assertEquals(CharacterReader.RefillPoint - 1, errorList[0].pos)
    }

    @Test
    fun testUnconsumeAfterBufferUp() {
        // test for after consume() a bufferUp occurs (look-forward) but then attempts to unconsume. Would throw a "No buffer left to unconsume"
        val triggeringSnippet = "<title>One <span>Two"
        val padding =
            CharArray(
                CharacterReader.RefillPoint - triggeringSnippet.length + 8,
            ) // The "<span" part must be just at the limit. The "containsIgnoreCase" scan does a bufferUp, losing the unconsume
        padding.fill(' ')
        val paddedSnippet = padding.concatToString() + triggeringSnippet
        val errorList = ParseErrorList.tracking(1)
        Parser.parseFragment(paddedSnippet, null, "", errorList)
        // just asserting we don't get a WTF on unconsume
    }

    @Test
    fun testOpeningAngleBracketInsteadOfAttribute() {
        val triggeringSnippet = "<html <"
        val errorList = ParseErrorList.tracking(1)
        Parser.parseFragment(triggeringSnippet, null, "", errorList)
        assertEquals(6, errorList[0].pos)
    }

    @Test
    fun testMalformedSelfClosingTag() {
        val triggeringSnippet = "<html /ouch"
        val errorList = ParseErrorList.tracking(1)
        Parser.parseFragment(triggeringSnippet, null, "", errorList)
        assertEquals(7, errorList[0].pos)
    }

    @Test
    fun testOpeningAngleBracketInTagName() {
        val triggeringSnippet = "<html<"
        val errorList = ParseErrorList.tracking(1)
        Parser.parseFragment(triggeringSnippet, null, "", errorList)
        assertEquals(5, errorList[0].pos)
    }

    @Test
    fun rcData() {
        val doc = Ksoup.parse("<title>One \u0000Two</title>")
        assertEquals("One �Two", doc.title())
    }

    @Test
    fun plaintext() {
        val doc = Ksoup.parse("<div>One<plaintext><div>Two</plaintext>\u0000no < Return")
        assertEquals(
            "<html><head></head><body><div>One<plaintext>&lt;div&gt;Two&lt;/plaintext&gt;�no &lt; Return</plaintext></div></body></html>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun nullInTag() {
        val doc = Ksoup.parse("<di\u0000v>One</di\u0000v>Two")
        assertEquals("<di�v>\n One\n</di�v>Two", doc.body().html())
    }

    @Test
    fun attributeValUnquoted() {
        var doc = Ksoup.parse("<p name=foo&lt;bar>")
        val p = doc.selectFirst("p")
        assertEquals("foo<bar", p!!.attr("name"))
        doc = Ksoup.parse("<p foo=")
        assertEquals("<p foo></p>", doc.body().html())
    }
}
