package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.select.Elements
import kotlin.test.*

/**
 * Functional tests for the Position tracking behavior (across nodes, treebuilder, etc.)
 */
class PositionTest {
    @BeforeTest
    fun initKsoup() {
        TestHelper.initKsoup()
    }


    @Test
    fun parserTrackDefaults() {
        val htmlParser: Parser = Parser.htmlParser()
        assertFalse(htmlParser.isTrackPosition)
        htmlParser.setTrackPosition(true)
        assertTrue(htmlParser.isTrackPosition)

        val xmlParser: Parser = Parser.xmlParser()
        assertFalse(xmlParser.isTrackPosition)
        xmlParser.setTrackPosition(true)
        assertTrue(xmlParser.isTrackPosition)
    }

    @Test
    fun tracksPosition() {
        val content = "<p id=1\n class=foo>\n<span>Hello\n &reg;\n there &copy.</span> now.\n <!-- comment --> "
        val doc: Document = Ksoup.parse(content, TrackingHtmlParser)

        val html = doc.expectFirst("html")
        val body = doc.expectFirst("body")
        val p = doc.expectFirst("p")
        val span = doc.expectFirst("span")
        val text = span.firstChild() as TextNode?
        assertNotNull(text)
        val now = span.nextSibling() as TextNode?
        assertNotNull(now)
        val comment = now.nextSibling() as Comment?
        assertNotNull(comment)

        // implicit
        assertTrue(body.sourceRange().isTracked())
        assertTrue(body.endSourceRange().isTracked())
        assertTrue(body.sourceRange().isImplicit())
        assertTrue(body.endSourceRange().isImplicit())
        val htmlRange = html.sourceRange()
        assertEquals("1,1:0-1,1:0", htmlRange.toString())
        assertEquals(htmlRange, body.sourceRange())
        assertEquals(html.endSourceRange(), body.endSourceRange())

        val pRange = p.sourceRange()
        assertEquals("1,1:0-2,12:19", pRange.toString())
        assertFalse(pRange.isImplicit())
        assertTrue(p.endSourceRange().isImplicit())
        assertEquals("6,19:83-6,19:83", p.endSourceRange().toString())
        assertEquals(p.endSourceRange(), html.endSourceRange())

        // no explicit P closer
        val pEndRange = p.endSourceRange()
        assertTrue(pEndRange.isTracked())
        assertTrue(pEndRange.isImplicit())

        val pStart = pRange.start()
        assertTrue(pStart.isTracked())
        assertEquals(0, pStart.pos())
        assertEquals(1, pStart.columnNumber())
        assertEquals(1, pStart.lineNumber())
        assertEquals("1,1:0", pStart.toString())

        val pEnd = pRange.end()
        assertTrue(pStart.isTracked())
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
    fun tracksExpectedPoppedElements() {
        // When TreeBuilder hits a direct .pop(), vs popToClose(..)
        val html = "<html><head><meta></head><body><img><p>One</p><p>Two</p></body></html>"
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)

        val track = StringBuilder()
        doc.expectFirst("html").stream().forEach { el ->
            accumulatePositions(el, track)
            assertTrue(el.sourceRange().isTracked(), el.tagName())
            assertTrue(el.endSourceRange().isTracked(), el.tagName())
            assertFalse(el.sourceRange().isImplicit(), el.tagName())
            assertFalse(el.endSourceRange().isImplicit(), el.tagName())
        }
        assertEquals(
            "html:0-6~63-70; head:6-12~18-25; meta:12-18~12-18; body:25-31~56-63; img:31-36~31-36; p:36-39~42-46; p:46-49~52-56; ",
            track.toString(),
        )

        val textTrack: StringBuilder = StringBuilder()
        doc.nodeStream(TextNode::class).forEach { text -> accumulatePositions(text, textTrack) }
        assertEquals("#text:39-42; #text:49-52; ", textTrack.toString())
    }

    @Test
    fun tracksImplicitPoppedElements() {
        // When TreeBuilder hits a direct .pop(), vs popToClose(..)
        val html = "<meta><img><p>One<p>Two<p>Three"
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)

        val track: StringBuilder = StringBuilder()
        doc.expectFirst("html").stream().forEach { el ->
            assertTrue(el.sourceRange().isTracked())
            assertTrue(el.endSourceRange().isTracked())
            accumulatePositions(el, track)
        }

        assertTrue(doc.expectFirst("p").endSourceRange().isImplicit())
        assertFalse(doc.expectFirst("meta").endSourceRange().isImplicit())
        assertEquals(
            "html:0-0~31-31; head:0-0~6-6; meta:0-6~0-6; body:6-6~31-31; img:6-11~6-11; p:11-14~17-17; p:17-20~23-23; p:23-26~31-31; ",
            track.toString(),
        )
    }

    private fun printRange(node: Node) {
        if (node is Element) {
            val el = node
            println(
                (
                        (
                                (
                                        el.tagName() + "\t" +
                                                el.sourceRange().start().pos()
                                        ).toString() + "-" + el.sourceRange().end().pos()
                                ).toString() + "\t... " +
                                el.endSourceRange().start().pos()
                        ).toString() + "-" + el.endSourceRange().end().pos(),
            )
        } else {
            println(
                (
                        (
                                node.nodeName() + "\t" +
                                        node.sourceRange().start().pos()
                                ).toString() + "-" + node.sourceRange().end().pos()
                        ),
            )
        }
    }

    @Test
    fun tracksMarkup() {
        val html = "<!doctype\nhtml>\n<title>ksoup &copy;\n2022</title><body>\n<![CDATA[\n<ksoup>\n]]>"
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)

        val doctype = doc.documentType()
        assertNotNull(doctype)
        assertEquals("html", doctype.name())
        assertEquals("1,1:0-2,6:15", doctype.sourceRange().toString())

        val title = doc.expectFirst("title")
        val titleText = title.firstChild() as TextNode?
        assertNotNull(titleText)
        assertEquals("ksoup ©\n2022", title.text())
        assertEquals(titleText.getWholeText(), title.text())
        assertEquals("3,1:16-3,8:23", title.sourceRange().toString())
        assertEquals("3,8:23-4,5:40", titleText.sourceRange().toString())

        val cdata = doc.body().childNode(1) as CDataNode
        assertEquals("\n<ksoup>\n", cdata.text())
        assertEquals("5,1:55-7,4:76", cdata.sourceRange().toString())
    }

    @Test
    fun tracksDataNodes() {
        val html = "<head>\n<script>foo;\nbar()\n5 <= 4;</script>"
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)

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
        val doc: Document = Ksoup.parse(xml, TrackingXmlParser)

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
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)

        val track = StringBuilder()
        val textNodes: List<TextNode> =
            doc.nodeStream(TextNode::class)
                .onEach { node -> accumulatePositions(node, track) }.toList()

        assertEquals(5, textNodes.size)
        assertEquals("foo", textNodes[0].text())
        assertEquals("bar", textNodes[1].text())
        assertEquals("baz", textNodes[2].text())
        assertEquals("qux", textNodes[3].text())
        assertEquals("coo", textNodes[4].text())

        assertEquals("#text:7-10; #text:14-17; #text:21-24; #text:29-32; #text:37-40; ", track.toString())
    }

    @Test
    fun tracksClosingHtmlTagsInXml() {
        val xml = "<p>One</p><title>Two</title><data>Three</data>"
        val doc: Document = Ksoup.parse(xml, TrackingXmlParser)
        val els: Elements = doc.children()
        for (el: Element in els) {
            assertTrue(el.sourceRange().isTracked())
            assertTrue(el.endSourceRange().isTracked())
        }
    }

    @Test
    fun tracksClosingHeadingTags() {
        val html = "<h1>One</h1><h2>Two</h2><h10>Ten</h10>"
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)

        val els: Elements = doc.body().children()
        for (el: Element in els) {
            assertTrue(el.sourceRange().isTracked())
            assertTrue(el.endSourceRange().isTracked())
        }

        val h2 = doc.expectFirst("h2")
        assertEquals("1,13:12-1,17:16", h2.sourceRange().toString())
        assertEquals("1,20:19-1,25:24", h2.endSourceRange().toString())
    }

    @Test
    fun tracksAttributes() {
        val html =
            "<div one=\"Hello there\" id=1 class=foo attr1 = \"bar &amp; qux\" attr2='val &gt x' attr3=\"\" attr4 attr5>Text"
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)

        val div = doc.expectFirst("div")

        val track: StringBuilder = StringBuilder()
        for (attr: Attribute in div.attributes()) {
            val attrRange: Range.AttributeRange = attr.sourceRange()
            assertTrue(attrRange.nameRange().isTracked())
            assertTrue(attrRange.valueRange().isTracked())
            assertSame(attrRange, div.attributes().sourceRange(attr.key))

            assertFalse(attrRange.nameRange().isImplicit())
            if (attr.value.isEmpty()) {
                assertTrue(attrRange.valueRange().isImplicit())
            } else {
                assertFalse(attrRange.valueRange().isImplicit())
            }

            accumulatePositions(attr, track)
        }

        assertEquals(
            "one:5-8=10-21; id:23-25=26-27; class:28-33=34-37; attr1:38-43=47-60; attr2:62-67=69-78; attr3:80-85=85-85; attr4:89-94=94-94; attr5:95-100=100-100; ",
            track.toString(),
        )
    }

    @Test
    fun tracksAttributesAcrossLines() {
        val html = "<div one=\"Hello\nthere\" \nid=1 \nclass=\nfoo\nattr5>Text"
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)

        val div = doc.expectFirst("div")

        val track: StringBuilder = StringBuilder()
        for (attr: Attribute in div.attributes()) {
            val attrRange: Range.AttributeRange = attr.sourceRange()
            assertTrue(attrRange.nameRange().isTracked())
            assertTrue(attrRange.valueRange().isTracked())
            assertSame(attrRange, div.attributes().sourceRange(attr.key))
            assertFalse(attrRange.nameRange().isImplicit())
            if (attr.value.isEmpty()) {
                assertTrue(attrRange.valueRange().isImplicit())
            } else {
                assertFalse(attrRange.valueRange().isImplicit())
            }
            accumulatePositions(attr, track)
        }

        val value = div.attributes()["class"]
        assertEquals("foo", value)
        val foo: Range.AttributeRange = div.attributes().sourceRange("class")
        assertEquals("4,1:30-4,6:35=5,1:37-5,4:40", foo.toString())

        assertEquals("one:5-8=10-21; id:24-26=27-28; class:30-35=37-40; attr5:41-46=46-46; ", track.toString())
    }

    @Test
    fun trackAttributePositionInFirstElement() {
        val html = "<html lang=en class=dark><p hidden></p></html>"

        val htmlDoc: Document = Ksoup.parse(html, TrackingHtmlParser)
        val htmlPos: StringBuilder = StringBuilder()
        htmlDoc.expectFirst("html").nodeStream().forEach { node ->
            accumulatePositions(node, htmlPos)
            accumulateAttributePositions(node, htmlPos)
        }

        assertEquals(
            "html:0-25~39-46; lang:6-10=11-13; class:14-19=20-24; head:25-25~25-25; body:25-25~46-46; p:25-35~35-39; hidden:28-34=34-34; ",
            htmlPos.toString(),
        )

        val xmlDoc: Document = Ksoup.parse(html, TrackingXmlParser)
        val xmlPos: StringBuilder = StringBuilder()
        xmlDoc.expectFirst("html").nodeStream().forEach { node ->
            accumulatePositions(node, xmlPos)
            accumulateAttributePositions(node, xmlPos)
        }

        assertEquals(
            "html:0-25~39-46; lang:6-10=11-13; class:14-19=20-24; p:25-35~35-39; hidden:28-34=34-34; ",
            xmlPos.toString(),
        )
    }

    @Test
    fun trackAttributePositionWithCase() {
        val pomXml =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                    "    <modelVersion>4.0.0</modelVersion>"

        val htmlDoc: Document = Ksoup.parse(pomXml, TrackingHtmlParser)
        val htmlPos: StringBuilder = StringBuilder()
        htmlDoc.expectFirst("html").nodeStream().forEach { node ->
            accumulatePositions(node, htmlPos)
            accumulateAttributePositions(node, htmlPos)
        }

        assertEquals(
            "html:0-0~243-243; head:0-0~0-0; body:0-0~243-243; project:0-204~243-243; xmlns:9-14=16-49; xmlns:xsi:51-60=62-103; xsi:schemalocation:105-123=125-202; #text:204-209; modelversion:209-223~228-243; #text:223-228; ",
            htmlPos.toString(),
        )

        val xmlDoc: Document = Ksoup.parse(pomXml, TrackingXmlParser)
        val xmlPos: StringBuilder = StringBuilder()
        xmlDoc.expectFirst("project").nodeStream().forEach { node ->
            accumulatePositions(node, xmlPos)
            accumulateAttributePositions(node, xmlPos)
        }

        assertEquals(
            "project:0-204~243-243; xmlns:9-14=16-49; xmlns:xsi:51-60=62-103; xsi:schemaLocation:105-123=125-202; #text:204-209; modelVersion:209-223~228-243; #text:223-228; ",
            xmlPos.toString(),
        )

        val xmlDocLc: Document =
            Ksoup.parse(pomXml, Parser.xmlParser().setTrackPosition(true).settings(ParseSettings(false, false)))
        val xmlPosLc: StringBuilder = StringBuilder()
        xmlDocLc.expectFirst("project").nodeStream().forEach { node ->
            accumulatePositions(node, xmlPosLc)
            accumulateAttributePositions(node, xmlPosLc)
        }

        assertEquals(
            "project:0-204~243-243; xmlns:9-14=16-49; xmlns:xsi:51-60=62-103; xsi:schemalocation:105-123=125-202; #text:204-209; modelversion:209-223~228-243; #text:223-228; ",
            xmlPosLc.toString(),
        )
    }

    @Test
    fun trackAttributesPositionsDedupes() {
        val html = "<p id=1 id=2 Id=3 Id=4 id=5 Id=6>"
        val htmlDoc: Document = Ksoup.parse(html, TrackingHtmlParser)
        val htmlDocUc: Document =
            Ksoup.parse(html, Parser.htmlParser().setTrackPosition(true).settings(ParseSettings(true, true)))
        val xmlDoc: Document = Ksoup.parse(html, TrackingXmlParser)
        val xmlDocLc: Document =
            Ksoup.parse(html, Parser.xmlParser().setTrackPosition(true).settings(ParseSettings(false, false)))

        val htmlPos: StringBuilder = StringBuilder()
        val htmlUcPos: StringBuilder = StringBuilder()
        val xmlPos: StringBuilder = StringBuilder()
        val xmlLcPos: StringBuilder = StringBuilder()

        accumulateAttributePositions(htmlDoc.expectFirst("p"), htmlPos)
        accumulateAttributePositions(htmlDocUc.expectFirst("p"), htmlUcPos)
        accumulateAttributePositions(xmlDoc.expectFirst("p"), xmlPos)
        accumulateAttributePositions(xmlDocLc.expectFirst("p"), xmlLcPos)

        assertEquals("id:3-5=6-7; ", htmlPos.toString())
        assertEquals("id:3-5=6-7; Id:13-15=16-17; ", htmlUcPos.toString())
        assertEquals("id:3-5=6-7; Id:13-15=16-17; ", xmlPos.toString())
        assertEquals("id:3-5=6-7; ", xmlLcPos.toString())
    }

    @Test
    fun trackAttributesPositionsDirectionalDedupes() {
        val html = "<p Id=1 id=2 Id=3 Id=4 id=5 Id=6>"
        val htmlDoc: Document = Ksoup.parse(html, TrackingHtmlParser)
        val htmlDocUc: Document =
            Ksoup.parse(html, Parser.htmlParser().setTrackPosition(true).settings(ParseSettings(true, true)))
        val xmlDoc: Document = Ksoup.parse(html, TrackingXmlParser)
        val xmlDocLc: Document =
            Ksoup.parse(html, Parser.xmlParser().setTrackPosition(true).settings(ParseSettings(false, false)))

        val htmlPos: StringBuilder = StringBuilder()
        val htmlUcPos: StringBuilder = StringBuilder()
        val xmlPos: StringBuilder = StringBuilder()
        val xmlLcPos: StringBuilder = StringBuilder()

        accumulateAttributePositions(htmlDoc.expectFirst("p"), htmlPos)
        accumulateAttributePositions(htmlDocUc.expectFirst("p"), htmlUcPos)
        accumulateAttributePositions(xmlDoc.expectFirst("p"), xmlPos)
        accumulateAttributePositions(xmlDocLc.expectFirst("p"), xmlLcPos)

        assertEquals("id:3-5=6-7; ", htmlPos.toString())
        assertEquals("Id:3-5=6-7; id:8-10=11-12; ", htmlUcPos.toString())
        assertEquals("Id:3-5=6-7; id:8-10=11-12; ", xmlPos.toString())
        assertEquals("id:3-5=6-7; ", xmlLcPos.toString())
    }

    @Test
    fun tracksFrag() {
        val html = "<h1 id=1>One</h1>\n<h2 id=2>Two</h2><h10>Ten</h10>"
        val shellDoc = Document.createShell("")

        val nodes: List<Node> = TrackingHtmlParser.parseFragmentInput(html, shellDoc.body(), shellDoc.baseUri())
        val track: StringBuilder = StringBuilder()

        // nodes is the top level nodes - want to descend to check all tracked OK
        nodes.forEach { node ->
            node.nodeStream().forEach { descend ->
                accumulatePositions(descend, track)
                accumulateAttributePositions(descend, track)
            }
        }

        assertEquals(
            "h1:0-9~12-17; id:4-6=7-8; #text:9-12; #text:17-18; h2:18-27~30-35; id:22-24=25-26; #text:27-30; h10:35-40~43-49; #text:40-43; ",
            track.toString(),
        )
    }

    @Test
    fun tracksAfterPSelfClose() {
        val html = "foo<p/>bar &amp; 2"
        val doc = Ksoup.parse(html, TrackingHtmlParser)
        val track = StringBuilder()
        doc.body().forEachNode { node -> accumulatePositions(node, track) }
        assertEquals("body:0-0~18-18; #text:0-3; p:3-7~3-7; #text:7-18; ", track.toString())
    }

    @Test
    fun tracksFirstTextnode() {
        val html = "foo<p></p>bar<p></p><div><b>baz</b></div>"
        val doc = Ksoup.parse(html, TrackingHtmlParser)
        val track = StringBuilder()
        doc.body().forEachNode { node -> accumulatePositions(node, track) }
        assertEquals(
            "body:0-0~41-41; #text:0-3; p:3-6~6-10; #text:10-13; p:13-16~16-20; div:20-25~35-41; b:25-28~31-35; #text:28-31; ",
            track.toString()
        )
    }

    @Test
    fun updateKeyMaintainsRangeLc() {
        val html = "<p xsi:CLASS=On>One</p>"
        val doc: Document = Ksoup.parse(html, TrackingHtmlParser)
        val p = doc.expectFirst("p")
        val attr = p.attribute("xsi:class")
        assertNotNull(attr)

        val expectedRange = "1,4:3-1,13:12=1,14:13-1,16:15"
        assertEquals(expectedRange, attr.sourceRange().toString())
        attr.setKey("class")
        assertEquals(expectedRange, attr.sourceRange().toString())
        assertEquals("class=\"On\"", attr.html())
    }

    @Test
    fun tracksDocument() {
        val html = "<!doctype html><title>Foo</title><p>Bar."
        val doc = Ksoup.parse(html, TrackingHtmlParser)
        val track = StringBuilder()
        doc.forEachNode { node -> accumulatePositions(node, track) }
        assertEquals(
            "#document:0-0~40-40; #doctype:0-15; html:15-15~40-40; head:15-15~33-33; title:15-22~15-33; #text:22-25; body:33-33~40-40; p:33-36~40-40; #text:36-40; ",
            track.toString()
        )
    }

    @Test
    fun tracksDocumentXml() {
        val html = "<!doctype html><title>Foo</title><p>Bar."
        val doc = Ksoup.parse(html, TrackingXmlParser)
        val track = StringBuilder()
        doc.forEachNode { node -> accumulatePositions(node, track) }
        assertEquals("#document:0-0~40-40; #doctype:0-15; title:15-22~25-33; #text:22-25; p:33-36~40-40; #text:36-40; ", track.toString())
    }

    @Test
    fun updateKeyMaintainsRangeUc() {
        val html = "<p xsi:CLASS=On>One</p>"
        val doc: Document = Ksoup.parse(html, TrackingXmlParser)
        val p = doc.expectFirst("p")
        val attr = p.attribute("xsi:CLASS")
        assertNotNull(attr)

        val expectedRange = "1,4:3-1,13:12=1,14:13-1,16:15"
        assertEquals(expectedRange, attr.sourceRange().toString())
        attr.setKey("class")
        assertEquals(expectedRange, attr.sourceRange().toString())
        assertEquals("class=\"On\"", attr.html())

        attr.setKey("CLASSY")
        assertEquals(expectedRange, attr.sourceRange().toString())
        assertEquals("CLASSY=\"On\"", attr.html())

        attr.setValue("To")
        assertEquals(expectedRange, attr.sourceRange().toString())
        assertEquals("CLASSY=\"To\"", attr.html())

        assertEquals("<p CLASSY=\"To\">One</p>", p.outerHtml())

        p.attr("CLASSY", "Tree")
        assertEquals(expectedRange, attr.sourceRange().toString())
        assertEquals(
            "CLASSY=\"To\"",
            attr.html(),
        ) // changes in this direction do not get to the attribute as it's not connected that way

        val attr2 = p.attribute("CLASSY")
        assertEquals("CLASSY=\"Tree\"", attr2!!.html())
        assertEquals(expectedRange, attr2.sourceRange().toString())
    }

    companion object {
        var TrackingHtmlParser: Parser = Parser.htmlParser().setTrackPosition(true)
        var TrackingXmlParser: Parser = Parser.xmlParser().setTrackPosition(true)

        fun accumulatePositions(
            node: Node,
            sb: StringBuilder,
        ) {
            sb
                .append(node.nodeName())
                .append(':')
                .append(node.sourceRange().startPos())
                .append('-')
                .append(node.sourceRange().endPos())

            if (node is Element) {
                val el = node
                sb
                    .append("~")
                    .append(el.endSourceRange().startPos())
                    .append('-')
                    .append(el.endSourceRange().endPos())
            }
            sb.append("; ")
        }

        fun accumulateAttributePositions(
            node: Node,
            sb: StringBuilder,
        ) {
            if (node is LeafNode) return // leafnode pseudo attributes are not tracked

            for (attribute: Attribute in node.attributes()) {
                accumulatePositions(attribute, sb)
            }
        }

        fun accumulatePositions(
            attr: Attribute,
            sb: StringBuilder,
        ) {
            val range: Range.AttributeRange = attr.sourceRange()

            sb
                .append(attr.key)
                .append(':')
                .append(range.nameRange().startPos())
                .append('-')
                .append(range.nameRange().endPos())
                .append('=')
                .append(range.valueRange().startPos())
                .append('-')
                .append(range.valueRange().endPos())

            sb.append("; ")
        }
    }
}
