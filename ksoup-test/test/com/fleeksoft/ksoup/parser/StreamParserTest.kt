package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.helper.DataUtil
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.openStream
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.toReader
import com.fleeksoft.ksoup.readGzipFile
import com.fleeksoft.ksoup.select.Elements
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for the StreamParser.
 */
class StreamParserTest {
    @BeforeTest
    fun initKsoup() {
        TestHelper.initKsoup()
    }

    @Test
    fun canStream() {
        val html =
            "<title>Test</title></head><div id=1>D1</div><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>"
        StreamParser(Parser.htmlParser()).parse(html, "").use { parser ->
            val seen = StringBuilder()
            parser.stream().forEach { el -> trackSeen(el, seen) }
            assertEquals(
                "title[Test];head+;div#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];body;html;#root;",
                seen.toString()
            )
        }
    }

    @Test
    fun canStreamXml() {
        val html =
            "<outmost><DIV id=1>D1</DIV><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>"
        StreamParser(Parser.xmlParser()).parse(html, "").use { parser ->
            val seen = StringBuilder()
            parser.stream().forEach { el -> trackSeen(el, seen) }
            assertEquals(
                "DIV#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];outmost;#root;",
                seen.toString()
            )
        }
    }

    @Test
    fun canIterate() {
        // same as stream, just a different interface
        val html =
            "<title>Test</title></head><div id=1>D1</div><div id=2>D2<p id=3><span>P One</p><p id=4>P Two</p></div><div id=5>D3<p id=6>P three</p>"
        val parser: StreamParser = StreamParser(Parser.htmlParser()).parse(html, "")
        val seen = StringBuilder()

        val it: Iterator<Element?> = parser.iterator()
        while (it.hasNext()) {
            trackSeen(it.next()!!, seen)
        }

        assertEquals(
            "title[Test];head+;div#1[D1]+;span[P One];p#3+;p#4[P Two];div#2[D2]+;p#6[P three];div#5[D3];body;html;#root;",
            seen.toString()
        )
        // checks expected order, and the + indicates that element had a next sibling at time of emission
    }

    @Test
    fun canReuse() {
        val parser: StreamParser = StreamParser(Parser.htmlParser())
        val html1 = "<p>One<p>Two"
        parser.parse(html1, "")

        val seen = StringBuilder()
        parser.stream().forEach({ el -> trackSeen(el, seen) })
        assertEquals("head+;p[One]+;p[Two];body;html;#root;", seen.toString())

        val html2 = "<div>Three<div>Four</div></div>"
        val seen2 = StringBuilder()
        parser.parse(html2, "")
        parser.stream().forEach({ el -> trackSeen(el, seen2) })
        assertEquals("head+;div[Four];div[Three];body;html;#root;", seen2.toString())

        // re-run without a new parse should be empty
        val seen3 = StringBuilder()
        parser.stream().forEach({ el -> trackSeen(el, seen3) })
        assertEquals("", seen3.toString())
    }

    @Test
    fun canStopAndCompleteAndReuse() {
        val parser: StreamParser = StreamParser(Parser.htmlParser())
        val html1 = "<p>One<p>Two"
        parser.parse(html1, "")

        val p: Element = parser.expectFirst("p")
        assertEquals("One", p.text())
        parser.stop()

        val it: Iterator<Element?> = parser.iterator()
        assertFalse(it.hasNext())
        assertFailsWith<NoSuchElementException> { it.next() }

        val p2: Element? = parser.selectNext("p")
        assertNull(p2)

        val completed: Document = parser.complete()
        val ps: Elements = completed.select("p")
        assertEquals(2, ps.size)
        assertEquals("One", ps[0].text())
        assertEquals("Two", ps[1].text())

        // can reuse
        parser.parse("<div>DIV", "")
        val div: Element = parser.expectFirst("div")
        assertEquals("DIV", div.text())
    }

    @Test
    fun select() {
        val html = "<title>One</title><p id=1>P One</p><p id=2>P Two</p>"
        val parser: StreamParser = StreamParser(Parser.htmlParser()).parse(html, "")

        val title: Element = parser.expectFirst("title")
        assertEquals("One", title.text())

        val partialDoc: Document? = title.ownerDocument()
        assertNotNull(partialDoc)
        // at this point, we should have one P with no text - as title was emitted on P head
        val ps: Elements = partialDoc.select("p")
        assertEquals(1, ps.size)
        assertEquals("", ps[0].text())
        assertSame(partialDoc, parser.document())

        val title2: Element? = parser.selectFirst("title")
        assertSame(title2, title)

        val p1: Element = parser.expectNext("p")
        assertEquals("P One", p1.text())

        val p2: Element = parser.expectNext("p")
        assertEquals("P Two", p2.text())

        val pNone: Element? = parser.selectNext("p")
        assertNull(pNone)
    }

    @Test
    fun canRemoveFromDom() {
        val html = "<div>One</div><div>DESTROY</div><div>Two</div>"
        val parser: StreamParser = StreamParser(Parser.htmlParser()).parse(html, "")
        parser.parse(html, "")

        parser.stream().forEach(
            { el ->
                if (el.ownText() == "DESTROY") el.remove()
            })

        val doc: Document = parser.document()
        val divs: Elements = doc.select("div")
        assertEquals(2, divs.size)
        assertEquals("One Two", divs.text())
    }

    @Test
    fun canRemoveWithIterator() {
        val html = "<div>One</div><div>DESTROY</div><div>Two</div>"
        val parser: StreamParser = StreamParser(Parser.htmlParser()).parse(html, "")
        parser.parse(html, "")

        val it = parser.iterator()
        while (it.hasNext()) {
            val el: Element = it.next()!!
            if (el.ownText() == "DESTROY") it.remove() // we know el.remove() works, from above test
        }

        val doc: Document = parser.document()
        val divs: Elements = doc.select("div")
        assertEquals(2, divs.size)
        assertEquals("One Two", divs.text())
    }

    @Test
    fun canSelectWithHas() {
        val parser: StreamParser = basic()

        val el: Element = parser.expectNext("div:has(p)")
        assertEquals("Two", el.text())
    }

    @Test
    fun canSelectWithSibling() {
        val parser: StreamParser = basic()

        val el: Element = parser.expectNext("div:first-of-type")
        assertEquals("One", el.text())

        val el2: Element? = parser.selectNext("div:first-of-type")
        assertNull(el2)
    }

    @Test
    fun canLoopOnSelectNext() {
        val streamer: StreamParser = StreamParser(Parser.htmlParser()).parse("<div><p>One<p>Two<p>Thr</div>", "")

        var count = 0
        var e: Element?
        while ((streamer.selectNext("p").also { e = it }) != null) {
            assertEquals(3, e?.text()?.length) // has a body
            e?.remove()
            count++
        }

        assertEquals(3, count)
        assertEquals(0, streamer.document().select("p").size) // removed all during iter

        assertTrue(isClosed(streamer)) // read to the end
    }

    @Test
    fun worksWithXmlParser() {
        val streamer: StreamParser =
            StreamParser(Parser.xmlParser()).parse("<div><p>One</p><p>Two</p><p>Thr</p></div>", "")

        var count = 0
        var e: Element?
        while ((streamer.selectNext("p").also { e = it }) != null) {
            assertEquals(3, e?.text()?.length) // has a body
            e?.remove()
            count++
        }

        assertEquals(3, count)
        assertEquals(0, streamer.document().select("p").size) // removed all during iter

        assertTrue(isClosed(streamer)) // read to the end
    }

    @Test
    fun closedOnStreamDrained() {
        val streamer: StreamParser = basic()
        assertFalse(isClosed(streamer))
        val count = streamer.stream().count()
        assertEquals(7, count)

        assertTrue(isClosed(streamer))
    }

    @Test
    fun closedOnIteratorDrained() {
        val streamer: StreamParser = basic()

        var count = 0
        val it: Iterator<Element?> = streamer.iterator()
        while (it.hasNext()) {
            it.next()
            count++
        }
        assertEquals(7, count)
        assertTrue(isClosed(streamer))
    }

    @Test
    fun closedOnComplete() {
        val streamer: StreamParser = basic()
        val doc: Document? = streamer.complete()
        assertTrue(isClosed(streamer))
    }

    @Test
    fun closedOnTryWithResources() {
        var copy: StreamParser? = null
        basic().use { streamer ->
            copy = streamer
            assertFalse(isClosed(copy!!))
        }
        assertTrue(isClosed(copy!!))
    }

    @Test
    fun doesNotReadPastParse() {
        val streamer: StreamParser = basic()
        val div: Element = streamer.expectFirst("div")

        // we should have read the sibling div, but not yet its children p
        val sib: Element? = div.nextElementSibling()
        assertNotNull(sib)
        assertEquals("div", sib.tagName())
        assertEquals(0, sib.childNodeSize())

        // the Reader should be at "<p>" because we haven't consumed it
        assertTrue(getReader(streamer).matches("<p>Two"))
    }

    @Test
    fun canParseFileReader() = runTest() {
        val file = TestHelper.getResourceAbsolutePath("htmltests/large.html.gz").uniVfs


        val reader = readGzipFile(file).toReader()
        val streamer: StreamParser = StreamParser(Parser.htmlParser()).parse(reader, file.absolutePath)

        var last: Element? = null
        var e: Element?
        while ((streamer.selectNext("p").also { e = it }) != null) {
            last = e
        }
        assertTrue(last!!.text().startsWith("VESTIBULUM"))

        // the reader should be closed as streamer is closed on completion of read
        assertTrue(isClosed(streamer))
    }

    @Test
    fun canParseFile() = runTest {

        val file = TestHelper.getResourceAbsolutePath("htmltests/large.html.gz").uniVfs
        val streamer: StreamParser =
            DataUtil.streamParser(sourceReader = file.openStream(), baseUri = "", charset = Charsets.UTF8, parser = Parser.htmlParser())

        var last: Element? = null
        var e: Element?
        while ((streamer.selectNext("p").also { e = it }) != null) {
            last = e
        }
        assertTrue(last!!.text().startsWith("VESTIBULUM"))

        // the reader should be closed as streamer is closed on completion of read
        assertTrue(isClosed(streamer))
    }

    // Fragments
    @Test
    fun canStreamFragment() {
        val html = "<tr id=1><td>One</td><tr id=2><td>Two</td></tr><tr id=3><td>Three</td></tr>"
        val context = Element("table")

        StreamParser(Parser.htmlParser()).parseFragment(html, context, "").use { parser ->
            val seen = StringBuilder()
            parser.stream().forEach { el -> trackSeen(el, seen) }
            assertEquals("td[One];tr#1+;td[Two];tr#2+;td[Three];tr#3;tbody;table;#root;", seen.toString())

            // checks expected order, and the + indicates that element had a next sibling at time of emission
            // note that we don't get a full doc, just the fragment (and the context at the end of the stack)
            assertTrue(isClosed(parser)) // as read to completion
        }
    }

    @Test
    fun canIterateFragment() {
        // same as stream, just a different interface
        val html =
            "<tr id=1><td>One</td><tr id=2><td>Two</td></tr><tr id=3><td>Three</td></tr>" // missing </tr>, following <tr> infers it
        val context: Element = Element("table")

        StreamParser(Parser.htmlParser()).parseFragment(html, context, "").use { parser ->
            val seen = StringBuilder()
            val it: Iterator<Element?> = parser.iterator()
            while (it.hasNext()) {
                trackSeen(it.next()!!, seen)
            }

            assertEquals("td[One];tr#1+;td[Two];tr#2+;td[Three];tr#3;tbody;table;#root;", seen.toString())

            // checks expected order, and the + indicates that element had a next sibling at time of emission
            // note that we don't get a full doc, just the fragment (and the context at the end of the stack)
            assertTrue(isClosed(parser)) // as read to completion
        }
    }

    @Test
    fun canSelectAndCompleteFragment() {
        val html = "<tr id=1><td>One</td><tr id=2><td>Two</td></tr><tr id=3><td>Three</td></tr>"
        val context: Element = Element("table")

        StreamParser(Parser.htmlParser()).parseFragment(html, context, "").use { parser ->
            val first: Element = parser.expectNext("td")
            assertEquals("One", first.ownText())

            var el: Element? = parser.expectNext("td")
            assertEquals("Two", el?.ownText())

            el = parser.expectNext("td")
            assertEquals("Three", el.ownText())

            el = parser.selectNext("td")
            assertNull(el)

            val nodes: List<Node?> = parser.completeFragment()
            assertEquals(1, nodes.size) // should be the inferred tbody
            val tbody: Node? = nodes[0]
            assertEquals("tbody", tbody?.nodeName())
            val trs: List<Node>? = tbody?.childNodes()
            assertEquals(3, trs?.size) // should be the three TRs
            assertSame(trs!![0].childNode(0), first) // tr -> td

            assertSame(parser.document(), first.ownerDocument()) // the shell document for this fragment
        }
    }

    @Test
    fun canStreamFragmentXml() {
        val html = "<tr id=1><td>One</td></tr><tr id=2><td>Two</td></tr><tr id=3><td>Three</td></tr>"
        val context: Element = Element("Other")

        StreamParser(Parser.xmlParser()).parseFragment(html, context, "").use { parser ->
            val seen = StringBuilder()
            parser.stream().forEach { el -> trackSeen(el, seen) }
            assertEquals("td[One];tr#1+;td[Two];tr#2+;td[Three];tr#3;#root;", seen.toString())

            // checks expected order, and the + indicates that element had a next sibling at time of emission
            // note that we don't get a full doc, just the fragment
            assertTrue(isClosed(parser)) // as read to completion

            val nodes: List<Node?> = parser.completeFragment()
            assertEquals(3, nodes.size)
            assertEquals("tr", nodes[0]!!.nodeName())
        }
    }

    companion object {
        fun trackSeen(el: Element, actual: StringBuilder) {
            actual.append(el.tagName())
            if (el.hasAttr("id")) actual.append("#").append(el.id())
            if (!el.ownText().isEmpty()) actual.append("[").append(el.ownText()).append("]")
            if (el.nextElementSibling() != null) actual.append("+")

            actual.append(";")
        }

        fun basic(): StreamParser {
            val html = "<div>One</div><div><p>Two</div>"
            val parser: StreamParser = StreamParser(Parser.htmlParser()).parse(html, "")
            return parser
        }

        fun isClosed(streamer: StreamParser): Boolean {
            // a bit of a back door in!
            return getReader(streamer).isClosed()
        }

        private fun getReader(streamer: StreamParser): CharacterReader {
            return streamer.document().parser()!!.getTreeBuilder().reader
        }
    }
}
