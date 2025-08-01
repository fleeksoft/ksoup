package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TestHelper
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PrinterTest {

    @Test
    fun pretty() = runTest {
        // parse /printertests/input-1.html, check formatted same as pretty-1.html
        val doc = TestHelper.parseResource(resourceName = "printertests/input-1.html")
        val expected = TestHelper.readResourceAsString("printertests/pretty-1.html")
        val html = doc.html()
        assertEquals(expected, html)
        assertEquals(html, doc.outerHtml())
    }

    @Test
    fun passthru() = runTest {
        // disable pretty, should be almost 1:1 of input (other than a couple parse normalizations; doctype, pre)
        val doc = TestHelper.parseResource("/printertests/input-1.html")
        doc.outputSettings().prettyPrint(false)

        val expected: String = TestHelper.readResourceAsString("/printertests/passthru-1.html")
        val html = doc.html()
        assertEquals(expected, html)
        assertEquals(html, doc.outerHtml())
    }

    @Test
    fun outline() = runTest {
        // outline mode, most everything gets indented
        val doc = TestHelper.parseResource(resourceName = "printertests/input-1.html")
        doc.outputSettings().outline(true)

        val expected: String = TestHelper.readResourceAsString("/printertests/outline-1.html")
        val html = doc.html()
        assertEquals(expected, html)
        assertEquals(html, doc.outerHtml())
    }

    @Test
    fun sequentialTextNodesDontCollapse() {
        // tests that the pretty printer does not collapse (trim leading | trailing whitespace) when there are
        // sequential textnodes. That doesn't happen in a parse, but can when manipulated.
        val doc: Document = Ksoup.parse("<div><div></div>Hello</div>") // needs to be text that would indent
        val div = doc.expectFirst("div")
        val hello = div.childNode(1) as TextNode
        hello.after(" there.")
        assertEquals("Hello", hello.getWholeText())
        assertEquals(" there.", (hello.nextSibling() as TextNode).getWholeText())

        assertEquals("Hello there.", div.text())
        assertEquals("<div></div>\nHello there.", div.html())
        assertEquals("<div>\n <div></div>\n Hello there.\n</div>", div.outerHtml())
    }

    @Test
    fun sequentialTextNodesCollapseAdjacentWhitespace() {
        // https://github.com/jhy/jsoup/pull/2349
        // Tests that the pretty printer collapses whitespace between sequential text nodes into a single space.
        // This must also work with intermediate empty and blank text nodes.
        val doc: Document = Ksoup.parseBodyFragment("Before <span> </span> After")
        doc.expectFirst("span")
            .after(TextNode("")).after(TextNode("")).after(TextNode(" ")).after(TextNode(""))
            .remove()
        assertEquals(6, doc.body().textNodes().size) // no collapse before printing
        assertEquals("Before After", doc.body().html())
    }

    @Test
    fun dontCollapseTextAfterNonElements() {
        val doc: Document = Ksoup.parse("<div><div></div>Hello <!-- -_- --> there</div>")
        val body = doc.body()
        assertEquals("Hello there", body.text())
        assertEquals("<div>\n <div></div>\n Hello <!-- -_- -->\n  there\n</div>", body.html())
    }

    @Test
    fun spaceAfterSpanInBlock() {
        val doc: Document =
            Ksoup.parse("<div> <span>Span</span> \n Text  <span>Follow</span></div> <p> <span>Span</span>  Text <span>Follow</span> </p>")
        val body = doc.body()
        assertEquals("Span Text Follow Span Text Follow", body.text())
        assertEquals("<div>\n <span>Span</span> Text <span>Follow</span>\n</div>\n<p><span>Span</span> Text <span>Follow</span></p>", body.html())
    }
}
