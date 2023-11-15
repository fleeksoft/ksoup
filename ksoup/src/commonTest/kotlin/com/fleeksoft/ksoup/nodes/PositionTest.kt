package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.select.NodeTraversor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Functional tests for the Position tracking behavior (across nodes, treebuilder, etc.)
 */
internal class PositionTest {
    @Test
    fun parserTrackDefaults() {
        val htmlParser = Parser.htmlParser()
        assertFalse(htmlParser.isTrackPosition)
        htmlParser.setTrackPosition(true)
        assertTrue(htmlParser.isTrackPosition)
        val xmlParser = Parser.htmlParser()
        assertFalse(xmlParser.isTrackPosition)
        xmlParser.setTrackPosition(true)
        assertTrue(xmlParser.isTrackPosition)
    }

    @Test
    fun tracksPosition() {
        val html = "<p id=1\n class=foo>\n<span>Hello\n &reg;\n there &copy.</span> now.\n <!-- comment --> "
        val doc = Ksoup.parse(html, TrackingParser)
        val body = doc.expectFirst("body")
        val p = doc.expectFirst("p")
        val span = doc.expectFirst("span")
        val text = span.firstChild() as TextNode?
        assertNotNull(text)
        val now = span.nextSibling() as TextNode?
        assertNotNull(now)
        val comment = now.nextSibling() as Comment?
        assertNotNull(comment)
        assertFalse(body.sourceRange().isTracked())
        val pRange = p.sourceRange()
        assertEquals("1,1:0-2,12:19", pRange.toString())

        // no explicit P closer
        val pEndRange = p.endSourceRange()
        assertFalse(pEndRange.isTracked())
        val pStart = pRange.start()
        assertTrue(pStart.isTracked)
        assertEquals(0, pStart.pos())
        assertEquals(1, pStart.columnNumber())
        assertEquals(1, pStart.lineNumber())
        assertEquals("1,1:0", pStart.toString())
        val pEnd = pRange.end()
        assertTrue(pStart.isTracked)
        assertEquals(19, pEnd.pos())
        assertEquals(12, pEnd.columnNumber())
        assertEquals(2, pEnd.lineNumber())
        assertEquals("2,12:19", pEnd.toString())
        assertEquals("3,1:20", span.sourceRange().start().toString())
        assertEquals("3,7:26", span.sourceRange().end().toString())

        // span end tag
        val spanEnd = span.endSourceRange()
        assertTrue(spanEnd.isTracked())
        assertEquals("5,14:52-5,21:59", spanEnd.toString())
        val wholeText = text.getWholeText()
        assertEquals("Hello\n ®\n there ©.", wholeText)
        val textOrig = "Hello\n &reg;\n there &copy."
        val textRange = text.sourceRange()
        assertEquals(textRange.end().pos() - textRange.start().pos(), textOrig.length)
        assertEquals("3,7:26", textRange.start().toString())
        assertEquals("5,14:52", textRange.end().toString())
        assertEquals("6,2:66", comment.sourceRange().start().toString())
        assertEquals("6,18:82", comment.sourceRange().end().toString())
    }

    @Test
    fun tracksMarkup() {
        val html = "<!doctype\nhtml>\n<title>jsoup &copy;\n2022</title><body>\n<![CDATA[\n<jsoup>\n]]>"
        val doc = Ksoup.parse(html, TrackingParser)
        val doctype = doc.documentType()
        assertNotNull(doctype)
        assertEquals("html", doctype.name())
        assertEquals("1,1:0-2,6:15", doctype.sourceRange().toString())
        val title = doc.expectFirst("title")
        val titleText = title.firstChild() as TextNode?
        assertNotNull(titleText)
        assertEquals("jsoup ©\n2022", title.text())
        assertEquals(titleText.getWholeText(), title.text())
        assertEquals("3,1:16-3,8:23", title.sourceRange().toString())
        assertEquals("3,8:23-4,5:40", titleText.sourceRange().toString())
        val cdata = doc.body().childNode(1) as CDataNode
        assertEquals("\n<jsoup>\n", cdata.text())
        assertEquals("5,1:55-7,4:76", cdata.sourceRange().toString())
    }

    @Test
    fun tracksDataNodes() {
        val html = "<head>\n<script>foo;\nbar()\n5 <= 4;</script>"
        val doc = Ksoup.parse(html, TrackingParser)
        val script = doc.expectFirst("script")
        assertNotNull(script)
        assertEquals("2,1:7-2,9:15", script.sourceRange().toString())
        val data = script.firstChild() as DataNode?
        assertNotNull(data)
        assertEquals("2,9:15-4,8:33", data.sourceRange().toString())
    }

    @Test
    fun tracksXml() {
        val xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!doctype html>\n<rss url=foo>\nXML\n</rss>\n<!-- comment -->"
        val doc = Ksoup.parse(xml, Parser.xmlParser().setTrackPosition(true))
        val decl = doc.childNode(0) as XmlDeclaration
        assertEquals("1,1:0-1,39:38", decl.sourceRange().toString())
        val doctype = doc.childNode(2) as DocumentType
        assertEquals("2,1:39-2,16:54", doctype.sourceRange().toString())
        val rss = doc.firstElementChild()
        assertNotNull(rss)
        assertEquals("3,1:55-3,14:68", rss.sourceRange().toString())
        assertEquals("5,1:73-5,7:79", rss.endSourceRange().toString())
        val text = rss.firstChild() as TextNode?
        assertNotNull(text)
        assertEquals("3,14:68-5,1:73", text.sourceRange().toString())
        val comment = rss.nextSibling()!!.nextSibling() as Comment?
        assertEquals("6,1:80-6,17:96", comment!!.sourceRange().toString())
    }

    @Test
    fun tracksTableMovedText() {
        val html = "<table>foo<tr>bar<td>baz</td>qux</tr>coo</table>"
        val doc = Ksoup.parse(html, TrackingParser)
        val textNodes: MutableList<TextNode> = ArrayList()
        NodeTraversor.traverse({ node, depth ->
            if (node is TextNode) {
                textNodes.add(node)
            }
        }, doc)
        assertEquals(5, textNodes.size)
        assertEquals("1,8:7-1,11:10", textNodes[0].sourceRange().toString())
        assertEquals("1,15:14-1,18:17", textNodes[1].sourceRange().toString())
        assertEquals("1,22:21-1,25:24", textNodes[2].sourceRange().toString())
        assertEquals("1,30:29-1,33:32", textNodes[3].sourceRange().toString())
        assertEquals("1,38:37-1,41:40", textNodes[4].sourceRange().toString())
    }

    @Test
    fun tracksClosingHtmlTagsInXml() {
        // verifies https://github.com/jhy/jsoup/issues/1935
        val xml = "<p>One</p><title>Two</title><data>Three</data>"
        val doc = Ksoup.parse(xml, Parser.xmlParser().setTrackPosition(true))
        val els = doc.children()
        for (el in els) {
            assertTrue(el.sourceRange().isTracked())
            assertTrue(el.endSourceRange().isTracked())
        }
    }

    @Test
    fun tracksClosingHeadingTags() {
        // https://github.com/jhy/jsoup/issues/1987
        val html = "<h1>One</h1><h2>Two</h2><h10>Ten</h10>"
        val doc = Ksoup.parse(html, TrackingParser)
        val els = doc.body().children()
        for (el in els) {
            assertTrue(el.sourceRange().isTracked())
            assertTrue(el.endSourceRange().isTracked())
        }
        val h2 = doc.expectFirst("h2")
        assertEquals("1,13:12-1,17:16", h2.sourceRange().toString())
        assertEquals("1,20:19-1,25:24", h2.endSourceRange().toString())
    }

    companion object {
        var TrackingParser = Parser.htmlParser().setTrackPosition(true)
    }
}