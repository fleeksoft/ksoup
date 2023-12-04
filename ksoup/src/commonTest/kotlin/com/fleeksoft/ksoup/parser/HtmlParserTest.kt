package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.TextUtil
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parameterizedTest
import com.fleeksoft.ksoup.ported.BufferReader
import com.fleeksoft.ksoup.ported.System
import com.fleeksoft.ksoup.safety.Safelist
import io.ktor.utils.io.charsets.name
import io.ktor.utils.io.core.toByteArray
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the Parser
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class HtmlParserTest {
    @Test
    fun parsesSimpleDocument() {
        val html =
            "<html><head><title>First!</title></head><body><p>First post! <img src=\"foo.png\" /></p></body></html>"
        val doc = Ksoup.parse(html)
        // need a better way to verify these:
        val p = doc.body().child(0)
        assertEquals("p", p.tagName())
        val img = p.child(0)
        assertEquals("foo.png", img.attr("src"))
        assertEquals("img", img.tagName())
    }

    @Test
    fun parsesRoughAttributes() {
        val html =
            "<html><head><title>First!</title></head><body><p class=\"foo > bar\">First post! <img src=\"foo.png\" /></p></body></html>"
        val doc = Ksoup.parse(html)

        // need a better way to verify these:
        val p = doc.body().child(0)
        assertEquals("p", p.tagName())
        assertEquals("foo > bar", p.attr("class"))
    }

    @Test
    fun dropsDuplicateAttributes() {
        parameterizedTest(dupeAttributeData()) {
            val html: String = it.first
            val expected: String = it.second
            val parser = Parser.htmlParser().setTrackErrors(10)
            val doc = parser.parseInput(html, "")
            val el = doc.expectFirst("body > *")
            assertEquals(expected, el.outerHtml()) // normalized names due to lower casing
            val tag = el.normalName()
            assertEquals(1, parser.getErrors().size)
            assertEquals("Dropped duplicate attribute(s) in tag [$tag]", parser.getErrors()[0].errorMessage)
        }
    }

    @Test
    fun retainsAttributesOfDifferentCaseIfSensitive() {
        val html = "<p One=One One=Two one=Three two=Four two=Five Two=Six>Text</p>"
        val parser = Parser.htmlParser().settings(ParseSettings.preserveCase)
        val doc = parser.parseInput(html, "")
        assertEquals(
            "<p One=\"One\" one=\"Three\" two=\"Four\" Two=\"Six\">Text</p>",
            doc.selectFirst("p")!!
                .outerHtml(),
        )
    }

    @Test
    fun parsesQuiteRoughAttributes() {
        val html = "<p =a>One<a <p>Something</p>Else"
        // this (used to; now gets cleaner) gets a <p> with attr '=a' and an <a tag with an attribute named '<p'; and then auto-recreated
        var doc = Ksoup.parse(html)

        // NOTE: per spec this should be the test case. but impacts too many ppl
        // assertEquals("<p =a>One<a <p>Something</a></p>\n<a <p>Else</a>", doc.body().html());
        assertEquals("<p a>One<a></a></p><p><a>Something</a></p><a>Else</a>", TextUtil.stripNewlines(doc.body().html()))
        doc = Ksoup.parse("<p .....>")
        assertEquals("<p .....></p>", doc.body().html())
    }

    @Test
    fun parsesComments() {
        val html = "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --><p>Hello</p></body></html>"
        val doc = Ksoup.parse(html)
        val body = doc.body()
        val comment = body.childNode(1) as Comment // comment should not be sub of img, as it's an empty tag
        assertEquals(" <table><tr><td></table> ", comment.getData())
        val p = body.child(1)
        val text = p.childNode(0) as TextNode
        assertEquals("Hello", text.getWholeText())
    }

    @Test
    fun parsesUnterminatedComments() {
        val html = "<p>Hello<!-- <tr><td>"
        val doc = Ksoup.parse(html)
        val p = doc.getElementsByTag("p")[0]
        assertEquals("Hello", p.text())
        val text = p.childNode(0) as TextNode
        assertEquals("Hello", text.getWholeText())
        val comment = p.childNode(1) as Comment
        assertEquals(" <tr><td>", comment.getData())
    }

    @Test
    fun allDashCommentsAreNotParseErrors() {
        // https://github.com/jhy/jsoup/issues/1667
        // <!-----> is not a parse error
        val html = "<!------>"
        val parser = Parser.htmlParser().setTrackErrors(10)
        val doc = Ksoup.parse(html, parser)
        val comment = doc.childNode(0) as Comment
        assertEquals("--", comment.getData())
        assertEquals(0, parser.getErrors().size)
    }

    @Test
    fun dropsUnterminatedTag() {
        // ksoup used to parse this to <p>, but whatwg, webkit will drop.
        val h1 = "<p"
        var doc = Ksoup.parse(h1)
        assertEquals(0, doc.getElementsByTag("p").size)
        assertEquals("", doc.text())
        val h2 = "<div id=1<p id='2'"
        doc = Ksoup.parse(h2)
        assertEquals("", doc.text())
    }

    @Test
    fun dropsUnterminatedAttribute() {
        // jsoup used to parse this to <p id="foo">, but whatwg, webkit will drop.
        val h1 = "<p id=\"foo"
        val doc = Ksoup.parse(h1)
        assertEquals("", doc.text())
    }

    @Test
    fun parsesUnterminatedTextarea() {
        // don't parse right to end, but break on <p>
        val doc = Ksoup.parse("<body><p><textarea>one<p>two")
        val t = doc.select("textarea").first()
        assertEquals("one", t!!.text())
        assertEquals("two", doc.select("p")[1].text())
    }

    @Test
    fun parsesUnterminatedOption() {
        // bit weird this -- browsers and spec get stuck in select until there's a </select>
        val doc = Ksoup.parse("<body><p><select><option>One<option>Two</p><p>Three</p>")
        val options = doc.select("option")
        assertEquals(2, options.size)
        assertEquals("One", options.first()!!.text())
        assertEquals("TwoThree", options.last()!!.text())
    }

    @Test
    fun testSelectWithOption() {
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)
        val document = parser.parseInput("<select><option>Option 1</option></select>", "http://jsoup.org")
        assertEquals(0, parser.getErrors().size)
    }

    @Test
    fun testSpaceAfterTag() {
        val doc = Ksoup.parse("<div > <a name=\"top\"></a ><p id=1 >Hello</p></div>")
        assertEquals("<div><a name=\"top\"></a><p id=\"1\">Hello</p></div>", TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun createsDocumentStructure() {
        val html = "<meta name=keywords /><link rel=stylesheet /><title>jsoup</title><p>Hello world</p>"
        val doc = Ksoup.parse(html)
        val head = doc.head()
        val body = doc.body()
        assertEquals(1, doc.children().size) // root node: contains html node
        assertEquals(2, doc.child(0).children().size) // html node: head and body
        assertEquals(3, head.children().size)
        assertEquals(1, body.children().size)
        assertEquals("keywords", head.getElementsByTag("meta")[0].attr("name"))
        assertEquals(0, body.getElementsByTag("meta").size)
        assertEquals("jsoup", doc.title())
        assertEquals("Hello world", body.text())
        assertEquals("Hello world", body.children()[0].text())
    }

    @Test
    fun createsStructureFromBodySnippet() {
        // the bar baz stuff naturally goes into the body, but the 'foo' goes into root, and the normalisation routine
        // needs to move into the start of the body
        val html = "foo <b>bar</b> baz"
        val doc = Ksoup.parse(html)
        assertEquals("foo bar baz", doc.text())
    }

    @Test
    fun handlesEscapedData() {
        val html = "<div title='Surf &amp; Turf'>Reef &amp; Beef</div>"
        val doc = Ksoup.parse(html)
        val div = doc.getElementsByTag("div")[0]
        assertEquals("Surf & Turf", div.attr("title"))
        assertEquals("Reef & Beef", div.text())
    }

    @Test
    fun handlesDataOnlyTags() {
        val t = "<style>font-family: bold</style>"
        val tels: List<Element> = Ksoup.parse(t).getElementsByTag("style")
        assertEquals("font-family: bold", tels[0].data())
        assertEquals("", tels[0].text())
        val s = "<p>Hello</p><script>obj.insert('<a rel=\"none\" />');\ni++;</script><p>There</p>"
        val doc = Ksoup.parse(s)
        assertEquals("Hello There", doc.text())
        assertEquals("obj.insert('<a rel=\"none\" />');\ni++;", doc.data())
    }

    @Test
    fun handlesTextAfterData() {
        val h = "<html><body>pre <script>inner</script> aft</body></html>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<html><head></head><body>pre <script>inner</script> aft</body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun handlesTextArea() {
        val doc = Ksoup.parse("<textarea>Hello</textarea>")
        val els = doc.select("textarea")
        assertEquals("Hello", els.text())
        assertEquals("Hello", els.value())
    }

    @Test
    fun preservesSpaceInTextArea() {
        // preserve because the tag is marked as preserve white space
        val doc = Ksoup.parse("<textarea>\n\tOne\n\tTwo\n\tThree\n</textarea>")
        val expect = "One\n\tTwo\n\tThree" // the leading and trailing spaces are dropped as a convenience to authors
        val el = doc.select("textarea").first()
        assertEquals(expect, el!!.text())
        assertEquals(expect, el.value())
        assertEquals(expect, el.html())
        assertEquals(
            "<textarea>\n\t$expect\n</textarea>",
            el.outerHtml(),
        ) // but preserved in round-trip html
    }

    @Test
    fun preservesSpaceInScript() {
        // preserve because it's content is a data node
        val doc = Ksoup.parse("<script>\nOne\n\tTwo\n\tThree\n</script>")
        val expect = "\nOne\n\tTwo\n\tThree\n"
        val el = doc.select("script").first()
        assertEquals(expect, el!!.data())
        assertEquals("One\n\tTwo\n\tThree", el.html())
        assertEquals("<script>$expect</script>", el.outerHtml())
    }

    @Test
    fun doesNotCreateImplicitLists() {
        // old jsoup used to wrap this in <ul>, but that's not to spec
        val h = "<li>Point one<li>Point two"
        val doc = Ksoup.parse(h)
        val ol = doc.select("ul") // should NOT have created a default ul.
        assertEquals(0, ol.size)
        val lis = doc.select("li")
        assertEquals(2, lis.size)
        assertEquals("body", lis.first()!!.parent()!!.tagName())

        // no fiddling with non-implicit lists
        val h2 = "<ol><li><p>Point the first<li><p>Point the second"
        val doc2 = Ksoup.parse(h2)
        assertEquals(0, doc2.select("ul").size)
        assertEquals(1, doc2.select("ol").size)
        assertEquals(2, doc2.select("ol li").size)
        assertEquals(2, doc2.select("ol li p").size)
        assertEquals(1, doc2.select("ol li")[0].children().size) // one p in first li
    }

    @Test
    fun discardsNakedTds() {
        // jsoup used to make this into an implicit table; but browsers make it into a text run
        val h = "<td>Hello<td><p>There<p>now"
        val doc = Ksoup.parse(h)
        assertEquals("Hello<p>There</p><p>now</p>", TextUtil.stripNewlines(doc.body().html()))
        // <tbody> is introduced if no implicitly creating table, but allows tr to be directly under table
    }

    @Test
    fun handlesNestedImplicitTable() {
        val doc =
            Ksoup.parse("<table><td>1</td></tr> <td>2</td></tr> <td> <table><td>3</td> <td>4</td></table> <tr><td>5</table>")
        assertEquals(
            "<table><tbody><tr><td>1</td></tr><tr><td>2</td></tr><tr><td><table><tbody><tr><td>3</td><td>4</td></tr></tbody></table></td></tr><tr><td>5</td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesWhatWgExpensesTableExample() {
        // http://www.whatwg.org/specs/web-apps/current-work/multipage/tabular-data.html#examples-0
        val doc =
            Ksoup.parse("<table> <colgroup> <col> <colgroup> <col> <col> <col> <thead> <tr> <th> <th>2008 <th>2007 <th>2006 <tbody> <tr> <th scope=rowgroup> Research and development <td> $ 1,109 <td> $ 782 <td> $ 712 <tr> <th scope=row> Percentage of net sales <td> 3.4% <td> 3.3% <td> 3.7% <tbody> <tr> <th scope=rowgroup> Selling, general, and administrative <td> $ 3,761 <td> $ 2,963 <td> $ 2,433 <tr> <th scope=row> Percentage of net sales <td> 11.6% <td> 12.3% <td> 12.6% </table>")
        assertEquals(
            "<table><colgroup><col></colgroup><colgroup><col><col><col></colgroup><thead><tr><th></th><th>2008</th><th>2007</th><th>2006</th></tr></thead><tbody><tr><th scope=\"rowgroup\">Research and development</th><td>$ 1,109</td><td>$ 782</td><td>$ 712</td></tr><tr><th scope=\"row\">Percentage of net sales</th><td>3.4%</td><td>3.3%</td><td>3.7%</td></tr></tbody><tbody><tr><th scope=\"rowgroup\">Selling, general, and administrative</th><td>$ 3,761</td><td>$ 2,963</td><td>$ 2,433</td></tr><tr><th scope=\"row\">Percentage of net sales</th><td>11.6%</td><td>12.3%</td><td>12.6%</td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesTbodyTable() {
        val doc =
            Ksoup.parse("<html><head></head><body><table><tbody><tr><td>aaa</td><td>bbb</td></tr></tbody></table></body></html>")
        assertEquals(
            "<table><tbody><tr><td>aaa</td><td>bbb</td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesImplicitCaptionClose() {
        val doc = Ksoup.parse("<table><caption>A caption<td>One<td>Two")
        assertEquals(
            "<table><caption>A caption</caption><tbody><tr><td>One</td><td>Two</td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun noTableDirectInTable() {
        val doc = Ksoup.parse("<table> <td>One <td><table><td>Two</table> <table><td>Three")
        assertEquals(
            "<table><tbody><tr><td>One</td><td><table><tbody><tr><td>Two</td></tr></tbody></table><table><tbody><tr><td>Three</td></tr></tbody></table></td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun ignoresDupeEndTrTag() {
        val doc =
            Ksoup.parse("<table><tr><td>One</td><td><table><tr><td>Two</td></tr></tr></table></td><td>Three</td></tr></table>") // two </tr></tr>, must ignore or will close table
        assertEquals(
            "<table><tbody><tr><td>One</td><td><table><tbody><tr><td>Two</td></tr></tbody></table></td><td>Three</td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesBaseTags() {
        // only listen to the first base href
        val h = "<a href=1>#</a><base href='/2/'><a href='3'>#</a><base href='http://bar'><a href=/4>#</a>"
        val doc = Ksoup.parse(h, "http://foo/")
        assertEquals("http://foo/2/", doc.baseUri()) // gets set once, so doc and descendants have first only
        val anchors = doc.getElementsByTag("a")
        assertEquals(3, anchors.size)
        assertEquals("http://foo/2/", anchors[0].baseUri())
        assertEquals("http://foo/2/", anchors[1].baseUri())
        assertEquals("http://foo/2/", anchors[2].baseUri())
        assertEquals("http://foo/2/1", anchors[0].absUrl("href"))
        assertEquals("http://foo/2/3", anchors[1].absUrl("href"))
        assertEquals("http://foo/4", anchors[2].absUrl("href"))
    }

    @Test
    fun handlesProtocolRelativeUrl() {
        val base = "https://example.com/"
        val html = "<img src='//example.net/img.jpg'>"
        val doc = Ksoup.parse(html, base)
        val el = doc.select("img").first()
        assertEquals("https://example.net/img.jpg", el!!.absUrl("src"))
    }

    @Test
    fun handlesCdata() {
        // todo: as this is html namespace, should actually treat as bogus comment, not cdata. keep as cdata for now
        val h = "<div id=1><![CDATA[<html>\n <foo><&amp;]]></div>" // the &amp; in there should remain literal
        val doc = Ksoup.parse(h)
        val div = doc.getElementById("1")
        assertEquals("<html>\n <foo><&amp;", div!!.text())
        assertEquals(0, div.children().size)
        assertEquals(1, div.childNodeSize()) // no elements, one text node
    }

    @Test
    fun roundTripsCdata() {
        val h = "<div id=1><![CDATA[\n<html>\n <foo><&amp;]]></div>"
        val doc = Ksoup.parse(h)
        val div = doc.getElementById("1")
        assertEquals("<html>\n <foo><&amp;", div!!.text())
        assertEquals(0, div.children().size)
        assertEquals(1, div.childNodeSize()) // no elements, one text node
        assertEquals("<div id=\"1\"><![CDATA[\n<html>\n <foo><&amp;]]>\n</div>", div.outerHtml())
        val cdata = div.textNodes()[0] as CDataNode
        assertEquals("\n<html>\n <foo><&amp;", cdata.text())
    }

    @Test
    fun handlesCdataAcrossBuffer() {
        val sb = StringBuilder()
        while (sb.length <= CharacterReader.maxBufferLen) {
            sb.append("A suitable amount of CData.\n")
        }
        val cdata = sb.toString()
        val h = "<div><![CDATA[$cdata]]></div>"
        val doc = Ksoup.parse(h)
        val div = doc.selectFirst("div")
        val node = div!!.textNodes()[0] as CDataNode
        assertEquals(cdata, node.text())
    }

    @Test
    fun handlesCdataInScript() {
        val html = "<script type=\"text/javascript\">//<![CDATA[\n\n  foo();\n//]]></script>"
        val doc = Ksoup.parse(html)
        val data = "//<![CDATA[\n\n  foo();\n//]]>"
        val script = doc.selectFirst("script")
        assertEquals("", script!!.text()) // won't be parsed as cdata because in script data section
        assertEquals(data, script.data())
        assertEquals(html, script.outerHtml())
        val dataNode = script.childNode(0) as DataNode
        assertEquals(data, dataNode.getWholeData())
        // see - not a cdata node, because in script. contrast with XmlTreeBuilder - will be cdata.
    }

    @Test
    fun handlesUnclosedCdataAtEOF() {
        // https://github.com/jhy/jsoup/issues/349 would crash, as character reader would try to seek past EOF
        val h = "<![CDATA[]]"
        val doc = Ksoup.parse(h)
        assertEquals(1, doc.body().childNodeSize())
    }

    @Test
    fun handleCDataInText() {
        val h = "<p>One <![CDATA[Two <&]]> Three</p>"
        val doc = Ksoup.parse(h)
        val p = doc.selectFirst("p")
        val nodes = p!!.childNodes()
        assertEquals("One ", (nodes[0] as TextNode).getWholeText())
        assertEquals("Two <&", (nodes[1] as TextNode).getWholeText())
        assertEquals("Two <&", (nodes[1] as CDataNode).getWholeText())
        assertEquals(" Three", (nodes[2] as TextNode).getWholeText())
        assertEquals(h, p.outerHtml())
    }

    @Test
    fun cdataNodesAreTextNodes() {
        val h = "<p>One <![CDATA[ Two <& ]]> Three</p>"
        val doc = Ksoup.parse(h)
        val p = doc.selectFirst("p")
        val nodes = p!!.textNodes()
        assertEquals("One ", nodes[0].text())
        assertEquals(" Two <& ", nodes[1].text())
        assertEquals(" Three", nodes[2].text())
    }

    @Test
    fun handlesInvalidStartTags() {
        val h = "<div>Hello < There <&amp;></div>" // parse to <div {#text=Hello < There <&>}>
        val doc = Ksoup.parse(h)
        assertEquals("Hello < There <&>", doc.select("div").first()!!.text())
    }

    @Test
    fun handlesUnknownTags() {
        val h = "<div><foo title=bar>Hello<foo title=qux>there</foo></div>"
        val doc = Ksoup.parse(h)
        val foos = doc.select("foo")
        assertEquals(2, foos.size)
        assertEquals("bar", foos.first()!!.attr("title"))
        assertEquals("qux", foos.last()!!.attr("title"))
        assertEquals("there", foos.last()!!.text())
    }

    @Test
    fun handlesUnknownInlineTags() {
        val h = "<p><cust>Test</cust></p><p><cust><cust>Test</cust></cust></p>"
        val doc = Ksoup.parseBodyFragment(h)
        val out = doc.body().html()
        assertEquals(h, TextUtil.stripNewlines(out))
    }

    @Test
    fun parsesBodyFragment() {
        val h = "<!-- comment --><p><a href='foo'>One</a></p>"
        val doc = Ksoup.parseBodyFragment(h, "http://example.com")
        assertEquals(
            "<body><!-- comment --><p><a href=\"foo\">One</a></p></body>",
            TextUtil.stripNewlines(doc.body().outerHtml()),
        )
        assertEquals("http://example.com/foo", doc.select("a").first()!!.absUrl("href"))
    }

    @Test
    fun parseBodyIsIndexNoAttributes() {
        // https://github.com/jhy/jsoup/issues/1404
        val expectedHtml = """<form>
 <hr><label>This is a searchable index. Enter search keywords: <input name="isindex"></label>
 <hr>
</form>"""
        var doc = Ksoup.parse("<isindex>")
        assertEquals(expectedHtml, doc.body().html())
        doc = Ksoup.parseBodyFragment("<isindex>")
        assertEquals(expectedHtml, doc.body().html())
        doc = Ksoup.parseBodyFragment("<table><input></table>")
        assertEquals("<input>\n<table></table>", doc.body().html())
    }

    @Test
    fun handlesUnknownNamespaceTags() {
        // note that the first foo:bar should not really be allowed to be self closing, if parsed in html mode.
        val h = "<foo:bar id='1' /><abc:def id=2>Foo<p>Hello</p></abc:def><foo:bar>There</foo:bar>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<foo:bar id=\"1\" /><abc:def id=\"2\">Foo<p>Hello</p></abc:def><foo:bar>There</foo:bar>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesKnownEmptyBlocks() {
        // if a known tag, allow self closing outside of spec, but force an end tag. unknown tags can be self closing.
        val h =
            "<div id='1' /><script src='/foo' /><div id=2><img /><img></div><a id=3 /><i /><foo /><foo>One</foo> <hr /> hr text <hr> hr text two"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<div id=\"1\"></div><script src=\"/foo\"></script><div id=\"2\"><img><img></div><a id=\"3\"></a><i></i><foo /><foo>One</foo><hr> hr text <hr> hr text two",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesKnownEmptyNoFrames() {
        val h = "<html><head><noframes /><meta name=foo></head><body>One</body></html>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<html><head><noframes></noframes><meta name=\"foo\"></head><body>One</body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun handlesKnownEmptyStyle() {
        val h = "<html><head><style /><meta name=foo></head><body>One</body></html>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<html><head><style></style><meta name=\"foo\"></head><body>One</body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun handlesKnownEmptyTitle() {
        val h = "<html><head><title /><meta name=foo></head><body>One</body></html>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<html><head><title></title><meta name=\"foo\"></head><body>One</body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun handlesKnownEmptyIframe() {
        val h = "<p>One</p><iframe id=1 /><p>Two"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<html><head></head><body><p>One</p><iframe id=\"1\"></iframe><p>Two</p></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun handlesSolidusAtAttributeEnd() {
        // this test makes sure [<a href=/>link</a>] is parsed as [<a href="/">link</a>], not [<a href="" /><a>link</a>]
        val h = "<a href=/>link</a>"
        val doc = Ksoup.parse(h)
        assertEquals("<a href=\"/\">link</a>", doc.body().html())
    }

    @Test
    fun handlesMultiClosingBody() {
        val h = "<body><p>Hello</body><p>there</p></body></body></html><p>now"
        val doc = Ksoup.parse(h)
        assertEquals(3, doc.select("p").size)
        assertEquals(3, doc.body().children().size)
    }

    @Test
    fun handlesUnclosedDefinitionLists() {
        // jsoup used to create a <dl>, but that's not to spec
        val h = "<dt>Foo<dd>Bar<dt>Qux<dd>Zug"
        val doc = Ksoup.parse(h)
        assertEquals(0, doc.select("dl").size) // no auto dl
        assertEquals(4, doc.select("dt, dd").size)
        val dts = doc.select("dt")
        assertEquals(2, dts.size)
        assertEquals("Zug", dts[1].nextElementSibling()!!.text())
    }

    @Test
    fun handlesBlocksInDefinitions() {
        // per the spec, dt and dd are inline, but in practise are block
        val h = "<dl><dt><div id=1>Term</div></dt><dd><div id=2>Def</div></dd></dl>"
        val doc = Ksoup.parse(h)
        assertEquals("dt", doc.select("#1").first()!!.parent()!!.tagName())
        assertEquals("dd", doc.select("#2").first()!!.parent()!!.tagName())
        assertEquals(
            "<dl><dt><div id=\"1\">Term</div></dt><dd><div id=\"2\">Def</div></dd></dl>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesFrames() {
        val h =
            "<html><head><script></script><noscript></noscript></head><frameset><frame src=foo></frame><frame src=foo></frameset></html>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<html><head><script></script><noscript></noscript></head><frameset><frame src=\"foo\"><frame src=\"foo\"></frameset></html>",
            TextUtil.stripNewlines(doc.html()),
        )
        // no body auto vivification
    }

    @Test
    fun ignoresContentAfterFrameset() {
        val h = "<html><head><title>One</title></head><frameset><frame /><frame /></frameset><table></table></html>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<html><head><title>One</title></head><frameset><frame><frame></frameset></html>",
            TextUtil.stripNewlines(doc.html()),
        )
        // no body, no table. No crash!
    }

    @Test
    fun handlesJavadocFont() {
        val h =
            "<TD BGCOLOR=\"#EEEEFF\" CLASS=\"NavBarCell1\">    <A HREF=\"deprecated-list.html\"><FONT CLASS=\"NavBarFont1\"><B>Deprecated</B></FONT></A>&nbsp;</TD>"
        val doc = Ksoup.parse(h)
        val a = doc.select("a").first()
        assertEquals("Deprecated", a!!.text())
        assertEquals("font", a.child(0).tagName())
        assertEquals("b", a.child(0).child(0).tagName())
    }

    @Test
    fun handlesBaseWithoutHref() {
        val h = "<head><base target='_blank'></head><body><a href=/foo>Test</a></body>"
        val doc = Ksoup.parse(h, "http://example.com/")
        val a = doc.select("a").first()
        assertEquals("/foo", a!!.attr("href"))
        assertEquals("http://example.com/foo", a.attr("abs:href"))
    }

    @Test
    fun normalisesDocument() {
        val h = "<!doctype html>One<html>Two<head>Three<link></head>Four<body>Five </body>Six </html>Seven "
        val doc = Ksoup.parse(h)
        assertEquals(
            "<!doctype html><html><head></head><body>OneTwoThree<link>FourFive Six Seven</body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun normalisesEmptyDocument() {
        val doc = Ksoup.parse("")
        assertEquals("<html><head></head><body></body></html>", TextUtil.stripNewlines(doc.html()))
    }

    @Test
    fun normalisesHeadlessBody() {
        val doc = Ksoup.parse("<html><body><span class=\"foo\">bar</span>")
        assertEquals(
            "<html><head></head><body><span class=\"foo\">bar</span></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun normalisedBodyAfterContent() {
        val doc = Ksoup.parse("<font face=Arial><body class=name><div>One</div></body></font>")
        assertEquals(
            "<html><head></head><body class=\"name\"><font face=\"Arial\"><div>One</div></font></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun findsCharsetInMalformedMeta() {
        val h = "<meta http-equiv=Content-Type content=text/html; charset=gb2312>"
        // example cited for reason of html5's <meta charset> element
        val doc = Ksoup.parse(h)
        assertEquals("gb2312", doc.select("meta").attr("charset"))
    }

    @Test
    fun testHgroup() {
        // jsoup used to not allow hgroup in h{n}, but that's not in spec, and browsers are OK
        val doc =
            Ksoup.parse("<h1>Hello <h2>There <hgroup><h1>Another<h2>headline</hgroup> <hgroup><h1>More</h1><p>stuff</p></hgroup>")
        assertEquals(
            "<h1>Hello</h1><h2>There <hgroup><h1>Another</h1><h2>headline</h2></hgroup><hgroup><h1>More</h1><p>stuff</p></hgroup></h2>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testRelaxedTags() {
        val doc = Ksoup.parse("<abc_def id=1>Hello</abc_def> <abc-def>There</abc-def>")
        assertEquals(
            "<abc_def id=\"1\">Hello</abc_def> <abc-def>There</abc-def>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testHeaderContents() {
        // h* tags (h1 .. h9) in browsers can handle any internal content other than other h*. which is not per any
        // spec, which defines them as containing phrasing content only. so, reality over theory.
        val doc = Ksoup.parse("<h1>Hello <div>There</div> now</h1> <h2>More <h3>Content</h3></h2>")
        assertEquals(
            "<h1>Hello <div>There</div> now</h1><h2>More</h2><h3>Content</h3>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testSpanContents() {
        // like h1 tags, the spec says SPAN is phrasing only, but browsers and publisher treat span as a block tag
        val doc = Ksoup.parse("<span>Hello <div>there</div> <span>now</span></span>")
        assertEquals("<span>Hello <div>there</div><span>now</span></span>", TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun testNoImagesInNoScriptInHead() {
        // jsoup used to allow, but against spec if parsing with noscript
        val doc = Ksoup.parse("<html><head><noscript><img src='foo'></noscript></head><body><p>Hello</p></body></html>")
        assertEquals(
            "<html><head><noscript>&lt;img src=\"foo\"&gt;</noscript></head><body><p>Hello</p></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun testUnclosedNoscriptInHead() {
        // Was getting "EOF" in html output, because the #anythingElse handler was calling an undefined toString, so used object.toString.
        val strings = arrayOf("<noscript>", "<noscript>One")
        for (html in strings) {
            val doc = Ksoup.parse(html)
            assertEquals("$html</noscript>", TextUtil.stripNewlines(doc.head().html()))
        }
    }

    @Test
    fun testAFlowContents() {
        // html5 has <a> as either phrasing or block
        val doc = Ksoup.parse("<a>Hello <div>there</div> <span>now</span></a>")
        assertEquals("<a>Hello <div>there</div><span>now</span></a>", TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun testFontFlowContents() {
        // html5 has no definition of <font>; often used as flow
        val doc = Ksoup.parse("<font>Hello <div>there</div> <span>now</span></font>")
        assertEquals("<font>Hello <div>there</div><span>now</span></font>", TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun handlesMisnestedTagsBI() {
        // whatwg: <b><i></b></i>
        val h = "<p>1<b>2<i>3</b>4</i>5</p>"
        val doc = Ksoup.parse(h)
        assertEquals("<p>1<b>2<i>3</i></b><i>4</i>5</p>", doc.body().html())
        // adoption agency on </b>, reconstruction of formatters on 4.
    }

    @Test
    fun handlesMisnestedTagsBP() {
        //  whatwg: <b><p></b></p>
        val h = "<b>1<p>2</b>3</p>"
        val doc = Ksoup.parse(h)
        assertEquals("<b>1</b>\n<p><b>2</b>3</p>", doc.body().html())
    }

    @Test
    fun handlesMisnestedAInDivs() {
        val h = "<a href='#1'><div><div><a href='#2'>child</a></div</div></a>"
        val w =
            "<a href=\"#1\"></a> <div> <a href=\"#1\"></a> <div> <a href=\"#1\"></a><a href=\"#2\">child</a> </div> </div>"
        val doc = Ksoup.parse(h)
        assertEquals(
            StringUtil.normaliseWhitespace(w),
            StringUtil.normaliseWhitespace(doc.body().html()),
        )
    }

    @Test
    fun handlesUnexpectedMarkupInTables() {
        // whatwg - tests markers in active formatting (if they didn't work, would get in table)
        // also tests foster parenting
        val h = "<table><b><tr><td>aaa</td></tr>bbb</table>ccc"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<b></b><b>bbb</b><table><tbody><tr><td>aaa</td></tr></tbody></table><b>ccc</b>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesUnclosedFormattingElements() {
        // whatwg: formatting elements get collected and applied, but excess elements are thrown away
        val h = """
            <!DOCTYPE html>
            <p><b class=x><b class=x><b><b class=x><b class=x><b>X
            <p>X
            <p><b><b class=x><b>X
            <p></b></b></b></b></b></b>X
        """.trimIndent()
        val doc = Ksoup.parse(h)
        doc.outputSettings().indentAmount(0)
        val want = """
            <!doctype html>
            <html>
            <head></head>
            <body>
            <p><b class="x"><b class="x"><b><b class="x"><b class="x"><b>X </b></b></b></b></b></b></p>
            <p><b class="x"><b><b class="x"><b class="x"><b>X </b></b></b></b></b></p>
            <p><b class="x"><b><b class="x"><b class="x"><b><b><b class="x"><b>X </b></b></b></b></b></b></b></b></p>
            <p>X</p>
            </body>
            </html>
        """.trimIndent()
        assertEquals(want, doc.html())
    }

    @Test
    fun handlesUnclosedAnchors() {
        val h = "<a href='http://example.com/'>Link<p>Error link</a>"
        val doc = Ksoup.parse(h)
        val want = "<a href=\"http://example.com/\">Link</a>\n<p><a href=\"http://example.com/\">Error link</a></p>"
        assertEquals(want, doc.body().html())
    }

    @Test
    fun reconstructFormattingElements() {
        // tests attributes and multi b
        val h = "<p><b class=one>One <i>Two <b>Three</p><p>Hello</p>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<p><b class=\"one\">One <i>Two <b>Three</b></i></b></p>\n<p><b class=\"one\"><i><b>Hello</b></i></b></p>",
            doc.body().html(),
        )
    }

    @Test
    fun reconstructFormattingElementsInTable() {
        // tests that tables get formatting markers -- the <b> applies outside the table and does not leak in,
        // and the <i> inside the table and does not leak out.
        val h = "<p><b>One</p> <table><tr><td><p><i>Three<p>Four</i></td></tr></table> <p>Five</p>"
        val doc = Ksoup.parse(h)
        val want =
            "<p><b>One</b></p><b><table><tbody><tr><td><p><i>Three</i></p><p><i>Four</i></p></td></tr></tbody></table><p>Five</p></b>"
        assertEquals(want, TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun commentBeforeHtml() {
        val h = "<!-- comment --><!-- comment 2 --><p>One</p>"
        val doc = Ksoup.parse(h)
        assertEquals(
            "<!-- comment --><!-- comment 2 --><html><head></head><body><p>One</p></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun emptyTdTag() {
        val h = "<table><tr><td>One</td><td id='2' /></tr></table>"
        val doc = Ksoup.parse(h)
        assertEquals("<td>One</td>\n<td id=\"2\"></td>", doc.select("tr").first()!!.html())
    }

    @Test
    fun handlesSolidusInA() {
        // test for bug #66
        val h = "<a class=lp href=/lib/14160711/>link text</a>"
        val doc = Ksoup.parse(h)
        val a = doc.select("a").first()
        assertEquals("link text", a!!.text())
        assertEquals("/lib/14160711/", a.attr("href"))
    }

    @Test
    fun handlesSpanInTbody() {
        // test for bug 64
        val h = "<table><tbody><span class='1'><tr><td>One</td></tr><tr><td>Two</td></tr></span></tbody></table>"
        val doc = Ksoup.parse(h)
        assertEquals(doc.select("span").first()!!.children().size, 0) // the span gets closed
        assertEquals(doc.select("table").size, 1) // only one table
    }

    @Test
    fun handlesUnclosedTitleAtEof() {
        assertEquals("Data", Ksoup.parse("<title>Data").title())
        assertEquals("Data<", Ksoup.parse("<title>Data<").title())
        assertEquals("Data</", Ksoup.parse("<title>Data</").title())
        assertEquals("Data</t", Ksoup.parse("<title>Data</t").title())
        assertEquals("Data</ti", Ksoup.parse("<title>Data</ti").title())
        assertEquals("Data", Ksoup.parse("<title>Data</title>").title())
        assertEquals("Data", Ksoup.parse("<title>Data</title >").title())
    }

    @Test
    fun handlesUnclosedTitle() {
        val one = Ksoup.parse("<title>One <b>Two <b>Three</TITLE><p>Test</p>") // has title, so <b> is plain text
        assertEquals("One <b>Two <b>Three", one.title())
        assertEquals("Test", one.select("p").first()!!.text())
        val two = Ksoup.parse("<title>One<b>Two <p>Test</p>") // no title, so <b> causes </title> breakout
        assertEquals("One", two.title())
        assertEquals("<b>Two \n <p>Test</p></b>", two.body().html())
    }

    @Test
    fun handlesUnclosedScriptAtEof() {
        assertEquals(
            "Data",
            Ksoup.parse("<script>Data").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data<",
            Ksoup.parse("<script>Data<").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data</sc",
            Ksoup.parse("<script>Data</sc").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data</-sc",
            Ksoup.parse("<script>Data</-sc").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data</sc-",
            Ksoup.parse("<script>Data</sc-").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data</sc--",
            Ksoup.parse("<script>Data</sc--").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data",
            Ksoup.parse("<script>Data</script>").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data</script",
            Ksoup.parse("<script>Data</script").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data",
            Ksoup.parse("<script>Data</script ").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data",
            Ksoup.parse("<script>Data</script n").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data",
            Ksoup.parse("<script>Data</script n=").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data",
            Ksoup.parse("<script>Data</script n=\"").select("script").first()!!
                .data(),
        )
        assertEquals(
            "Data",
            Ksoup.parse("<script>Data</script n=\"p").select("script").first()!!
                .data(),
        )
    }

    @Test
    fun handlesUnclosedRawtextAtEof() {
        assertEquals(
            "Data",
            Ksoup.parse("<style>Data").select("style").first()!!
                .data(),
        )
        assertEquals(
            "Data</st",
            Ksoup.parse("<style>Data</st").select("style").first()!!
                .data(),
        )
        assertEquals(
            "Data",
            Ksoup.parse("<style>Data</style>").select("style").first()!!
                .data(),
        )
        assertEquals(
            "Data</style",
            Ksoup.parse("<style>Data</style").select("style").first()!!
                .data(),
        )
        assertEquals(
            "Data</-style",
            Ksoup.parse("<style>Data</-style").select("style").first()!!
                .data(),
        )
        assertEquals(
            "Data</style-",
            Ksoup.parse("<style>Data</style-").select("style").first()!!
                .data(),
        )
        assertEquals(
            "Data</style--",
            Ksoup.parse("<style>Data</style--").select("style").first()!!
                .data(),
        )
    }

    @Test
    fun noImplicitFormForTextAreas() {
        // old jsoup parser would create implicit forms for form children like <textarea>, but no more
        val doc = Ksoup.parse("<textarea>One</textarea>")
        assertEquals("<textarea>One</textarea>", doc.body().html())
    }

    @Test
    fun handlesEscapedScript() {
        val doc = Ksoup.parse("<script><!-- one <script>Blah</script> --></script>")
        assertEquals(
            "<!-- one <script>Blah</script> -->",
            doc.select("script").first()!!
                .data(),
        )
    }

    @Test
    fun handles0CharacterAsText() {
        val doc = Ksoup.parse("0<p>0</p>")
        assertEquals("0\n<p>0</p>", doc.body().html())
    }

    @Test
    fun handlesNullInData() {
        val doc = Ksoup.parse("<p id=\u0000>Blah \u0000</p>")
        assertEquals(
            "<p id=\"\uFFFD\">Blah &#x0;</p>",
            doc.body().html(),
        ) // replaced in attr, NOT replaced in data (but is escaped as control char <0x20)
    }

    @Test
    fun handlesNullInComments() {
        val doc = Ksoup.parse("<body><!-- \u0000 \u0000 -->")
        assertEquals("<!-- \uFFFD \uFFFD -->", doc.body().html())
    }

    @Test
    fun handlesNewlinesAndWhitespaceInTag() {
        val doc = Ksoup.parse("<a \n href=\"one\" \r\n id=\"two\" \u000c >")
        assertEquals("<a href=\"one\" id=\"two\"></a>", doc.body().html())
    }

    @Test
    fun handlesWhitespaceInoDocType() {
        val html = """<!DOCTYPE html
      PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
      "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">"""
        val doc = Ksoup.parse(html)
        assertEquals(
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
            doc.childNode(0).outerHtml(),
        )
    }

    @Test
    fun tracksErrorsWhenRequested() {
        val html = "<p>One</p href='no'>\n<!DOCTYPE html>\n&arrgh;<font />&#33 &amp &#xD800;<br /></div><foo"
        val parser = Parser.htmlParser().setTrackErrors(500)
        val doc = Ksoup.parse(html, "http://example.com", parser)
        val errors: List<ParseError> = parser.getErrors()
        assertEquals(9, errors.size)
        assertEquals("<1:21>: Attributes incorrectly present on end tag [/p]", errors[0].toString())
        assertEquals(
            "<2:16>: Unexpected Doctype token [<!doctype html>] when in state [InBody]",
            errors[1].toString(),
        )
        assertEquals(
            "<3:2>: Invalid character reference: invalid named reference [arrgh]",
            errors[2].toString(),
        )
        assertEquals("<3:16>: Tag [font] cannot be self closing; not a void tag", errors[3].toString())
        assertEquals(
            "<3:20>: Invalid character reference: missing semicolon on [&#33]",
            errors[4].toString(),
        )
        assertEquals(
            "<3:25>: Invalid character reference: missing semicolon on [&amp]",
            errors[5].toString(),
        )
        assertEquals(
            "<3:34>: Invalid character reference: character [55296] outside of valid range",
            errors[6].toString(),
        )
        assertEquals("<3:46>: Unexpected EndTag token [</div>] when in state [InBody]", errors[7].toString())
        assertEquals(
            "<3:51>: Unexpectedly reached end of file (EOF) in input state [TagName]",
            errors[8].toString(),
        )
    }

    @Test
    fun tracksLimitedErrorsWhenRequested() {
        val html = "<p>One</p href='no'>\n<!DOCTYPE html>\n&arrgh;<font /><br /><foo"
        val parser = Parser.htmlParser().setTrackErrors(3)
        val doc = parser.parseInput(html, "http://example.com")
        val errors: List<ParseError> = parser.getErrors()
        assertEquals(3, errors.size)
        assertEquals("<1:21>: Attributes incorrectly present on end tag [/p]", errors[0].toString())
        assertEquals(
            "<2:16>: Unexpected Doctype token [<!doctype html>] when in state [InBody]",
            errors[1].toString(),
        )
        assertEquals(
            "<3:2>: Invalid character reference: invalid named reference [arrgh]",
            errors[2].toString(),
        )
    }

    @Test
    fun noErrorsByDefault() {
        val html = "<p>One</p href='no'>&arrgh;<font /><br /><foo"
        val parser = Parser.htmlParser()
        val doc = Ksoup.parse(html, "http://example.com", parser)
        val errors: List<ParseError> = parser.getErrors()
        assertEquals(0, errors.size)
    }

    @Test
    fun optionalPClosersAreNotErrors() {
        val html = "<body><div><p>One<p>Two</div></body>"
        val parser = Parser.htmlParser().setTrackErrors(128)
        val doc = Ksoup.parse(html, "", parser)
        val errors = parser.getErrors()
        assertEquals(0, errors.size)
    }

    @Test
    fun handlesCommentsInTable() {
        val html = "<table><tr><td>text</td><!-- Comment --></tr></table>"
        val node = Ksoup.parseBodyFragment(html)
        assertEquals(
            "<html><head></head><body><table><tbody><tr><td>text</td><!-- Comment --></tr></tbody></table></body></html>",
            TextUtil.stripNewlines(node.outerHtml()),
        )
    }

    @Test
    fun handlesQuotesInCommentsInScripts() {
        val html = """<script>
  <!--
    document.write('</scr' + 'ipt>');
  // -->
</script>"""
        val node = Ksoup.parseBodyFragment(html)
        assertEquals(
            """<script>
  <!--
    document.write('</scr' + 'ipt>');
  // -->
</script>""",
            node.body().html(),
        )
    }

    @Test
    fun handleNullContextInParseFragment() {
        val html = "<ol><li>One</li></ol><p>Two</p>"
        val nodes = Parser.parseFragment(html, null, "http://example.com/")
        assertEquals(
            1,
            nodes.size,
        ) // returns <html> node (not document) -- no context means doc gets created
        assertEquals("html", nodes[0].nodeName())
        assertEquals(
            "<html> <head></head> <body> <ol> <li>One</li> </ol> <p>Two</p> </body> </html>",
            StringUtil.normaliseWhitespace(
                nodes[0].outerHtml(),
            ),
        )
    }

    @Test
    fun doesNotFindShortestMatchingEntity() {
        // previous behaviour was to identify a possible entity, then chomp down the string until a match was found.
        // (as defined in html5.) However in practise that lead to spurious matches against the author's intent.
        val html = "One &clubsuite; &clubsuit;"
        val doc = Ksoup.parse(html)
        assertEquals(StringUtil.normaliseWhitespace("One &amp;clubsuite; ♣"), doc.body().html())
    }

    @Test
    fun relaxedBaseEntityMatchAndStrictExtendedMatch() {
        // extended entities need a ; at the end to match, base does not
        val html = "&amp &quot &reg &icy &hopf &icy; &hopf;"
        val doc = Ksoup.parse(html)
        doc.outputSettings().escapeMode(Entities.EscapeMode.extended)
            .charset("ascii") // modifies output only to clarify test
        assertEquals("&amp; \" &reg; &amp;icy &amp;hopf &icy; &hopf;", doc.body().html())
    }

    @Test
    fun handlesXmlDeclarationAsBogusComment() {
        val html = "<?xml encoding='UTF-8' ?><body>One</body>"
        val doc = Ksoup.parse(html)
        assertEquals(
            "<!--?xml encoding='UTF-8' ?--> <html> <head></head> <body> One </body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
    }

    @Test
    fun handlesTagsInTextarea() {
        val html = "<textarea><p>Jsoup</p></textarea>"
        val doc = Ksoup.parse(html)
        assertEquals("<textarea>&lt;p&gt;Jsoup&lt;/p&gt;</textarea>", doc.body().html())
    }

    // form tests
    @Test
    fun createsFormElements() {
        val html = "<body><form><input id=1><input id=2></form></body>"
        val doc = Ksoup.parse(html)
        val el = doc.select("form").first()
        assertTrue(el is FormElement, "Is form element")
        val form = el as FormElement?
        val controls = form!!.elements()
        assertEquals(2, controls.size)
        assertEquals("1", controls[0].id())
        assertEquals("2", controls[1].id())
    }

    @Test
    fun associatedFormControlsWithDisjointForms() {
        // form gets closed, isn't parent of controls
        val html = "<table><tr><form><input type=hidden id=1><td><input type=text id=2></td><tr></table>"
        val doc = Ksoup.parse(html)
        val el = doc.select("form").first()
        assertTrue(el is FormElement, "Is form element")
        val form = el as FormElement?
        val controls = form!!.elements()
        assertEquals(2, controls.size)
        assertEquals("1", controls[0].id())
        assertEquals("2", controls[1].id())
        assertEquals(
            "<table><tbody><tr><form></form><input type=\"hidden\" id=\"1\"><td><input type=\"text\" id=\"2\"></td></tr><tr></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun handlesInputInTable() {
        val h = """<body>
<input type="hidden" name="a" value="">
<table>
<input type="hidden" name="b" value="" />
</table>
</body>"""
        val doc = Ksoup.parse(h)
        assertEquals(1, doc.select("table input").size)
        assertEquals(2, doc.select("input").size)
    }

    @Test
    fun convertsImageToImg() {
        // image to img, unless in a svg. old html cruft.
        val h = "<body><image><svg><image /></svg></body>"
        val doc = Ksoup.parse(h)
        assertEquals("<img>\n<svg>\n <image />\n</svg>", doc.body().html())
    }

    @Test
    fun handlesInvalidDoctypes() {
        // would previously throw invalid name exception on empty doctype
        var doc = Ksoup.parse("<!DOCTYPE>")
        assertEquals(
            "<!doctype> <html> <head></head> <body></body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
        doc = Ksoup.parse("<!DOCTYPE><html><p>Foo</p></html>")
        assertEquals(
            "<!doctype> <html> <head></head> <body> <p>Foo</p> </body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
        doc = Ksoup.parse("<!DOCTYPE \u0000>")
        assertEquals(
            "<!doctype �> <html> <head></head> <body></body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
    }

    @Test
    fun handlesManyChildren() {
        // Arrange
        val longBody = StringBuilder(500000)
        for (i in 0..24999) {
            longBody.append(i).append("<br>")
        }

        // Act
        val start = System.currentTimeMillis()
        val doc = Parser.parseBodyFragment(longBody.toString(), "")

        // Assert
        assertEquals(50000, doc.body().childNodeSize())
        assertTrue(System.currentTimeMillis() - start < 3000)
    }

    @Test
    @Throws(IOException::class)
    fun testInvalidTableContents() {
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/table-invalid-elements.html")
        val doc: Document = Ksoup.parseFile(input, "UTF-8")
        doc.outputSettings().prettyPrint(true)
        val rendered = doc.toString()
        val endOfEmail = rendered.indexOf("Comment")
        val guarantee = rendered.indexOf("Why am I here?")
        assertTrue(endOfEmail > -1, "Comment not found")
        assertTrue(guarantee > -1, "Search text not found")
        assertTrue(guarantee > endOfEmail, "Search text did not come after comment")
    }

    @Test
    fun testNormalisesIsIndex() {
        val doc = Ksoup.parse("<body><isindex action='/submit'></body>")
        val html = doc.outerHtml()
        assertEquals(
            "<form action=\"/submit\"> <hr><label>This is a searchable index. Enter search keywords: <input name=\"isindex\"></label> <hr> </form>",
            StringUtil.normaliseWhitespace(doc.body().html()),
        )
    }

    @Test
    fun testReinsertionModeForThCelss() {
        val body =
            "<body> <table> <tr> <th> <table><tr><td></td></tr></table> <div> <table><tr><td></td></tr></table> </div> <div></div> <div></div> <div></div> </th> </tr> </table> </body>"
        val doc = Ksoup.parse(body)
        assertEquals(1, doc.body().children().size)
    }

    @Test
    fun testUsingSingleQuotesInQueries() {
        val body = "<body> <div class='main'>hello</div></body>"
        val doc = Ksoup.parse(body)
        val main = doc.select("div[class='main']")
        assertEquals("hello", main.text())
    }

    @Test
    fun testSupportsNonAsciiTags() {
        val body = "<a進捗推移グラフ>Yes</a進捗推移グラフ><bрусский-тэг>Correct</<bрусский-тэг>"
        val doc = Ksoup.parse(body)
        var els = doc.select("a進捗推移グラフ")
        assertEquals("Yes", els.text())
        els = doc.select("bрусский-тэг")
        assertEquals("Correct", els.text())
    }

    @Test
    fun testSupportsPartiallyNonAsciiTags() {
        val body = "<div>Check</divá>"
        val doc = Ksoup.parse(body)
        val els = doc.select("div")
        assertEquals("Check", els.text())
    }

    @Test
    fun testFragment() {
        // make sure when parsing a body fragment, a script tag at start goes into the body
        val html = """
            <script type="text/javascript">console.log('foo');</script>
            <div id="somecontent">some content</div>
            <script type="text/javascript">console.log('bar');</script>
        """.trimIndent()
        val body = Ksoup.parseBodyFragment(html)
        assertEquals(
            """<script type="text/javascript">console.log('foo');</script>
<div id="somecontent">
 some content
</div>
<script type="text/javascript">console.log('bar');</script>""",
            body.body().html(),
        )
    }

    @Test
    fun testHtmlLowerCase() {
        val html = "<!doctype HTML><DIV ID=1>One</DIV>"
        val doc = Ksoup.parse(html)
        assertEquals(
            "<!doctype html> <html> <head></head> <body> <div id=\"1\"> One </div> </body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
        val div = doc.selectFirst("#1")
        div!!.after("<TaG>One</TaG>")
        assertEquals("<tag>One</tag>", TextUtil.stripNewlines(div.nextElementSibling()!!.outerHtml()))
    }

    @Test
    fun testHtmlLowerCaseAttributesOfVoidTags() {
        val html = "<!doctype HTML><IMG ALT=One></DIV>"
        val doc = Ksoup.parse(html)
        assertEquals(
            "<!doctype html> <html> <head></head> <body> <img alt=\"One\"> </body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
    }

    @Test
    fun testHtmlLowerCaseAttributesForm() {
        val html = "<form NAME=one>"
        val doc = Ksoup.parse(html)
        assertEquals("<form name=\"one\"></form>", StringUtil.normaliseWhitespace(doc.body().html()))
    }

    @Test
    fun canPreserveTagCase() {
        val parser = Parser.htmlParser()
        parser.settings(ParseSettings(true, false))
        val doc = parser.parseInput("<div id=1><SPAN ID=2>", "")
        assertEquals(
            "<html> <head></head> <body> <div id=\"1\"> <SPAN id=\"2\"></SPAN> </div> </body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
        val div = doc.selectFirst("#1")
        div!!.after("<TaG ID=one>One</TaG>")
        assertEquals("<TaG id=\"one\">One</TaG>", TextUtil.stripNewlines(div.nextElementSibling()!!.outerHtml()))
    }

    @Test
    fun canPreserveAttributeCase() {
        val parser = Parser.htmlParser()
        parser.settings(ParseSettings(false, true))
        val doc = parser.parseInput("<div id=1><SPAN ID=2>", "")
        assertEquals(
            "<html> <head></head> <body> <div id=\"1\"> <span ID=\"2\"></span> </div> </body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
        val div = doc.selectFirst("#1")
        div!!.after("<TaG ID=one>One</TaG>")
        assertEquals("<tag ID=\"one\">One</tag>", TextUtil.stripNewlines(div.nextElementSibling()!!.outerHtml()))
    }

    @Test
    fun canPreserveBothCase() {
        val parser = Parser.htmlParser()
        parser.settings(ParseSettings(true, true))
        val doc = parser.parseInput("<div id=1><SPAN ID=2>", "")
        assertEquals(
            "<html> <head></head> <body> <div id=\"1\"> <SPAN ID=\"2\"></SPAN> </div> </body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()),
        )
        val div = doc.selectFirst("#1")
        div!!.after("<TaG ID=one>One</TaG>")
        assertEquals("<TaG ID=\"one\">One</TaG>", TextUtil.stripNewlines(div.nextElementSibling()!!.outerHtml()))
    }

    @Test
    fun handlesControlCodeInAttributeName() {
        val doc = Ksoup.parse("<p><a \u0006=foo>One</a><a/\u0006=bar><a foo\u0006=bar>Two</a></p>")
        assertEquals("<p><a>One</a><a></a><a foo=\"bar\">Two</a></p>", doc.body().html())
    }

    @Test
    fun caseSensitiveParseTree() {
        val html = "<r><X>A</X><y>B</y></r>"
        val parser = Parser.htmlParser()
        parser.settings(ParseSettings.preserveCase)
        val doc = parser.parseInput(html, "")
        assertEquals("<r> <X> A </X> <y> B </y> </r>", StringUtil.normaliseWhitespace(doc.body().html()))
    }

    @Test
    fun caseInsensitiveParseTree() {
        val html = "<r><X>A</X><y>B</y></r>"
        val parser = Parser.htmlParser()
        val doc = parser.parseInput(html, "")
        assertEquals("<r> <x> A </x> <y> B </y> </r>", StringUtil.normaliseWhitespace(doc.body().html()))
    }

    @Test
    fun preservedCaseLinksCantNest() {
        val html = "<A>ONE <A>Two</A></A>"
        val doc = Parser.htmlParser()
            .settings(ParseSettings.preserveCase)
            .parseInput(html, "")
        // assertEquals("<A>ONE </A><A>Two</A>", StringUtil.normaliseWhitespace(doc.body().html()));
        assertEquals("<A>ONE </A><A>Two</A>", doc.body().html())
    }

    @Test
    fun normalizesDiscordantTags() {
        val document = Ksoup.parse("<div>test</DIV><p></p>")
        assertEquals("<div>\n test\n</div>\n<p></p>", document.body().html())
    }

    @Test
    fun selfClosingVoidIsNotAnError() {
        val html = "<p>test<br/>test<br/></p>"
        val parser = Parser.htmlParser().setTrackErrors(5)
        parser.parseInput(html, "")
        assertEquals(0, parser.getErrors().size)
        assertTrue(Ksoup.isValid(html, Safelist.basic()))
        val clean = Ksoup.clean(html, Safelist.basic())
        assertEquals("<p>test<br>\n test<br></p>", clean)
    }

    @Test
    fun selfClosingOnNonvoidIsError() {
        val html = "<p>test</p>\n\n<div /><div>Two</div>"
        val parser = Parser.htmlParser().setTrackErrors(5)
        parser.parseInput(html, "")
        assertEquals(1, parser.getErrors().size)
        assertEquals(
            "<3:8>: Tag [div] cannot be self closing; not a void tag",
            parser.getErrors()[0].toString(),
        )
        assertFalse(Ksoup.isValid(html, Safelist.relaxed()))
        val clean = Ksoup.clean(html, Safelist.relaxed())
        assertEquals("<p>test</p> <div></div> <div> Two </div>", StringUtil.normaliseWhitespace(clean))
    }

    @Test
    @Throws(IOException::class)
    fun testTemplateInsideTable() {
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/table-polymer-template.html")
        val doc: Document = Ksoup.parseFile(input, "UTF-8")
        doc.outputSettings().prettyPrint(true)
        val templates = doc.body().getElementsByTag("template")
        for (template in templates) {
            assertTrue(template.childNodes().size > 1)
        }
    }

    @Test
    fun testHandlesDeepSpans() {
        val sb = StringBuilder()
        for (i in 0..199) {
            sb.append("<span>")
        }
        sb.append("<p>One</p>")
        val doc = Ksoup.parse(sb.toString())
        assertEquals(200, doc.select("span").size)
        assertEquals(1, doc.select("p").size)
    }

    @Test
    fun commentAtEnd() {
        val doc = Ksoup.parse("<!")
        assertTrue(doc.childNode(0) is Comment)
    }

    @Test
    fun preSkipsFirstNewline() {
        val doc = Ksoup.parse("<pre>\n\nOne\nTwo\n</pre>")
        val pre = doc.selectFirst("pre")
        assertEquals("One\nTwo", pre!!.text())
        assertEquals("\nOne\nTwo\n", pre.wholeText())
    }

    @Test
    @Throws(IOException::class)
    fun handlesXmlDeclAndCommentsBeforeDoctype() {
        val `in`: String = TestHelper.getResourceAbsolutePath("htmltests/comments.html")
        val doc: Document = Ksoup.parseFile(`in`, "UTF-8")
        assertEquals(
            "<!--?xml version=\"1.0\" encoding=\"utf-8\"?--><!-- so --> <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><!-- what --> <html xml:lang=\"en\" lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"> <!-- now --> <head> <!-- then --> <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\"> <title>A Certain Kind of Test</title> </head> <body> <h1>Hello</h1>h1&gt; (There is a UTF8 hidden BOM at the top of this file.) </body> </html>",
            StringUtil.normaliseWhitespace(doc.html()),
        )
        assertEquals("A Certain Kind of Test", doc.head().select("title").text())
    }

    @Test
    @Throws(IOException::class)
    fun fallbackToUtfIfCantEncode() {
        // that charset can't be encoded, so make sure we flip to utf
        val input = "<html><meta charset=\"ISO-2022-CN\"/>One</html>"
        val doc = Ksoup.parse(bufferReader = BufferReader(input.toByteArray()), baseUri = "", charsetName = null)
        assertEquals("UTF-8", doc.charset().name.uppercase())
        assertEquals("One", doc.text())
        val html = doc.outerHtml()
        assertEquals("<html><head><meta charset=\"UTF-8\"></head><body>One</body></html>", TextUtil.stripNewlines(html))
    }

    @Test
    @Throws(IOException::class)
    fun characterReaderBuffer() {
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/character-reader-buffer.html.gz")
        val doc: Document = Ksoup.parseFile(input, "UTF-8")
        val expectedHref = "http://www.domain.com/path?param_one=value&param_two=value"
        val links = doc.select("a")
        assertEquals(2, links.size)
        assertEquals(expectedHref, links[0].attr("href")) // passes
        assertEquals(
            expectedHref,
            links[1].attr("href"),
        ) // fails, "but was:<...ath?param_one=value&[]_two-value>"
    }

    @Test
    fun selfClosingTextAreaDoesntLeaveDroppings() {
        // https://github.com/jhy/jsoup/issues/1220
        val doc = Ksoup.parse("<div><div><textarea/></div></div>")
        assertFalse(doc.body().html().contains("&lt;"))
        assertFalse(doc.body().html().contains("&gt;"))
        assertEquals("<div><div><textarea></textarea></div></div>", TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun testNoSpuriousSpace() {
        val doc = Ksoup.parse("Just<a>One</a><a>Two</a>")
        assertEquals("Just<a>One</a><a>Two</a>", doc.body().html())
        assertEquals("JustOneTwo", doc.body().text())
    }

    @Test
    fun pTagsGetIndented() {
        val html = "<div><p><a href=one>One</a><p><a href=two>Two</a></p></div>"
        val doc = Ksoup.parse(html)
        assertEquals(
            """<div>
 <p><a href="one">One</a></p>
 <p><a href="two">Two</a></p>
</div>""",
            doc.body().html(),
        )
    }

    @Test
    fun indentRegardlessOfCase() {
        val html = "<p>1</p><P>2</P>"
        val doc = Ksoup.parse(html)
        assertEquals(
            """<body>
 <p>1</p>
 <p>2</p>
</body>""",
            doc.body().outerHtml(),
        )
        val caseDoc = Ksoup.parse(html, "", Parser.htmlParser().settings(ParseSettings.preserveCase))
        assertEquals(
            """<body>
 <p>1</p>
 <P>2</P>
</body>""",
            caseDoc.body().outerHtml(),
        )
    }

    @Test
    fun testH20() {
        // https://github.com/jhy/jsoup/issues/731
        val html = "H<sub>2</sub>O"
        val clean = Ksoup.clean(html, Safelist.basic())
        assertEquals("H<sub>2</sub>O", clean)
        val doc = Ksoup.parse(html)
        assertEquals("H2O", doc.text())
    }

    @Test
    fun testUNewlines() {
        // https://github.com/jhy/jsoup/issues/851
        val html = "t<u>es</u>t <b>on</b> <i>f</i><u>ir</u>e"
        val clean = Ksoup.clean(html, Safelist.basic())
        assertEquals("t<u>es</u>t <b>on</b> <i>f</i><u>ir</u>e", clean)
        val doc = Ksoup.parse(html)
        assertEquals("test on fire", doc.text())
    }

    @Test
    fun testFarsi() {
        // https://github.com/jhy/jsoup/issues/1227
        val text = "نیمه\u200Cشب"
        val doc = Ksoup.parse("<p>$text")
        assertEquals(text, doc.text())
    }

    @Test
    fun testStartOptGroup() {
        // https://github.com/jhy/jsoup/issues/1313
        val html = """<select>
  <optgroup label="a">
  <option>one
  <option>two
  <option>three
  <optgroup label="b">
  <option>four
  <option>fix
  <option>six
</select>"""
        val doc = Ksoup.parse(html)
        val select = doc.selectFirst("select")
        assertEquals(2, select!!.childrenSize())
        assertEquals(
            "<optgroup label=\"a\"> <option>one </option><option>two </option><option>three </option></optgroup><optgroup label=\"b\"> <option>four </option><option>fix </option><option>six </option></optgroup>",
            select.html(),
        )
    }

    @Test
    fun readerClosedAfterParse() {
        val doc = Ksoup.parse("Hello")
        val treeBuilder = doc.parser()!!.getTreeBuilder()
        assertTrue(treeBuilder.reader.isClosed())
        assertNull(treeBuilder.tokeniser)
    }

    @Test
    fun scriptInDataNode() {
        var doc = Ksoup.parse("<script>Hello</script><style>There</style>")
        assertTrue(doc.selectFirst("script")!!.childNode(0) is DataNode)
        assertTrue(doc.selectFirst("style")!!.childNode(0) is DataNode)
        doc = Ksoup.parse(
            "<SCRIPT>Hello</SCRIPT><STYLE>There</STYLE>",
            "",
            Parser.htmlParser().settings(ParseSettings.preserveCase),
        )
        assertTrue(doc.selectFirst("script")!!.childNode(0) is DataNode)
        assertTrue(doc.selectFirst("style")!!.childNode(0) is DataNode)
    }

    @Test
    fun textareaValue() {
        val html = "<TEXTAREA>YES YES</TEXTAREA>"
        var doc = Ksoup.parse(html)
        assertEquals("YES YES", doc.selectFirst("textarea")?.value())
        doc = Ksoup.parse(html, "", Parser.htmlParser().settings(ParseSettings.preserveCase))
        assertEquals("YES YES", doc.selectFirst("textarea")?.value())
    }

    @Test
    fun preserveWhitespaceInHead() {
        val html =
            "\n<!doctype html>\n<html>\n<head>\n<title>Hello</title>\n</head>\n<body>\n<p>One</p>\n</body>\n</html>\n"
        val doc = Ksoup.parse(html)
        doc.outputSettings().prettyPrint(false)
        assertEquals(
            "<!doctype html>\n<html>\n<head>\n<title>Hello</title>\n</head>\n<body>\n<p>One</p>\n</body>\n</html>\n",
            doc.outerHtml(),
        )
    }

    @Test
    fun handleContentAfterBody() {
        val html = "<body>One</body>  <p>Hello!</p></html> <p>There</p>"
        val doc = Ksoup.parse(html)
        doc.outputSettings().prettyPrint(false)
        assertEquals(
            "<html><head></head><body>One<p>Hello!</p><p>There</p></body>  </html> ",
            doc.outerHtml(),
        )
    }

    @Test
    fun preservesTabs() {
        // testcase to demonstrate tab retention - https://github.com/jhy/jsoup/issues/1240
        val html = "<pre>One\tTwo</pre><span>\tThree\tFour</span>"
        val doc = Ksoup.parse(html)
        val pre = doc.selectFirst("pre")
        val span = doc.selectFirst("span")
        assertEquals("One\tTwo", pre!!.text())
        assertEquals("Three Four", span!!.text()) // normalized, including overall trim
        assertEquals(
            "\tThree\tFour",
            span.wholeText(),
        ) // text normalizes, wholeText retains original spaces incl tabs
        assertEquals("One\tTwo Three Four", doc.body().text())
        assertEquals(
            "<pre>One\tTwo</pre><span> Three Four</span>",
            doc.body().html(),
        ) // html output provides normalized space, incl tab in pre but not in span
        doc.outputSettings().prettyPrint(false)
        assertEquals(
            html,
            doc.body().html(),
        ) // disabling pretty-printing - round-trips the tab throughout, as no normalization occurs
    }

    @Test
    fun wholeTextTreatsBRasNewline() {
        val html = "<div>\nOne<br>Two <p>Three<br>Four</div>"
        val doc = Ksoup.parse(html)
        val div = doc.selectFirst("div")
        assertNotNull(div)
        assertEquals("\nOne\nTwo Three\nFour", div.wholeText())
        assertEquals("\nOne\nTwo ", div.wholeOwnText())
    }

    @Test
    fun canDetectAutomaticallyAddedElements() {
        val bare = "<script>One</script>"
        val full = "<html><head><title>Check</title></head><body><p>One</p></body></html>"
        assertTrue(didAddElements(bare))
        assertFalse(didAddElements(full))
    }

    private fun didAddElements(input: String): Boolean {
        // two passes, one as XML and one as HTML. XML does not vivify missing/optional tags
        val html = Ksoup.parse(input)
        val xml = Ksoup.parse(input, "", Parser.xmlParser())
        val htmlElementCount = html.getAllElements().size
        val xmlElementCount = xml.getAllElements().size
        return htmlElementCount > xmlElementCount
    }

    @Test
    fun canSetHtmlOnCreatedTableElements() {
        // https://github.com/jhy/jsoup/issues/1603
        val element = Element("tr")
        element.html("<tr><td>One</td></tr>")
        assertEquals("<tr>\n <tr>\n  <td>One</td>\n </tr>\n</tr>", element.outerHtml())
    }

    @Test
    fun parseFragmentOnCreatedDocument() {
        // https://github.com/jhy/jsoup/issues/1601
        val bareFragment = "<h2>text</h2>"
        val nodes = Document("").parser()!!
            .parseFragmentInput(bareFragment, Element("p"), "")
        assertEquals(1, nodes.size)
        val node = nodes[0]
        assertEquals("h2", node.nodeName())
        assertEquals("<p>\n <h2>text</h2></p>", node.parent()!!.outerHtml())
    }

    @Test
    fun nestedPFragments() {
        // https://github.com/jhy/jsoup/issues/1602
        val bareFragment = "<p></p><a></a>"
        val nodes = Document("").parser()!!
            .parseFragmentInput(bareFragment, Element("p"), "")
        assertEquals(2, nodes.size)
        val node = nodes[0]
        assertEquals(
            "<p>\n <p></p><a></a></p>",
            node.parent()!!.outerHtml(),
        ) // mis-nested because fragment forced into the element, OK
    }

    @Test
    fun nestedAnchorAdoption() {
        // https://github.com/jhy/jsoup/issues/1608
        val html = "<a>\n<b>\n<div>\n<a>test</a>\n</div>\n</b>\n</a>"
        val doc = Ksoup.parse(html)
        assertNotNull(doc)
        assertEquals("<a> <b> </b></a><b><div><a> </a><a>test</a></div></b>", TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun tagsMustStartWithAscii() {
        // https://github.com/jhy/jsoup/issues/1006
        val valid = arrayOf("a一", "a会员挂单金额5", "table(╯°□°)╯")
        val invalid = arrayOf("一", "会员挂单金额5", "(╯°□°)╯")
        for (tag in valid) {
            val doc = Ksoup.parse("<$tag>Text</$tag>")
            val els = doc.getElementsByTag(tag)
            assertEquals(1, els.size)
            assertEquals(tag, els[0].tagName())
            assertEquals("Text", els[0].text())
        }
        for (tag in invalid) {
            val doc = Ksoup.parse("<$tag>Text</$tag>")
            val els = doc.getElementsByTag(tag)
            assertEquals(0, els.size)
            assertEquals("&lt;$tag&gt;Text<!--/$tag-->", doc.body().html())
        }
    }

    @Test
    fun htmlOutputCorrectsInvalidAttributeNames() {
        val html = "<body style=\"color: red\" \" name\"><div =\"\"></div></body>"
        val doc = Ksoup.parse(html)
        assertEquals(Document.OutputSettings.Syntax.html, doc.outputSettings().syntax())
        val out = doc.body().outerHtml()
        assertEquals("<body style=\"color: red\" name>\n <div></div>\n</body>", out)
    }

    @Test
    fun templateInHead() {
        // https://try.jsoup.org/~EGp3UZxQe503TJDHQEQEzm8IeUc
        val html =
            "<head><template id=1><meta name=tmpl></template><title>Test</title><style>One</style></head><body><p>Two</p>"
        val doc = Ksoup.parse(html)
        var want =
            "<html><head><template id=\"1\"><meta name=\"tmpl\"></template><title>Test</title><style>One</style></head><body><p>Two</p></body></html>"
        assertEquals(want, TextUtil.stripNewlines(doc.html()))
        val template = doc.select("template#1")
        template.select("meta").attr("content", "Yes")
        template.unwrap()
        want =
            "<html><head><meta name=\"tmpl\" content=\"Yes\"><title>Test</title><style>One</style></head><body><p>Two</p></body></html>"
        assertEquals(want, TextUtil.stripNewlines(doc.html()))
    }

    @Test
    fun nestedTemplateInBody() {
        val html =
            "<body><template id=1><table><tr><template id=2><td>One</td><td>Two</td></template></tr></template></body>"
        val doc = Ksoup.parse(html)
        var want =
            "<html><head></head><body><template id=\"1\"><table><tbody><tr><template id=\"2\"><td>One</td><td>Two</td></template></tr></tbody></table></template></body></html>"
        assertEquals(want, TextUtil.stripNewlines(doc.html()))

        // todo - will be nice to add some simpler template element handling like clone children etc?
        val tmplTbl = doc.selectFirst("template#1")
        val tmplRow = doc.selectFirst("template#2")
        assertNotNull(tmplRow)
        assertNotNull(tmplTbl)
        tmplRow.appendChild(tmplRow.clone())
        doc.select("template").unwrap()
        want =
            "<html><head></head><body><table><tbody><tr><td>One</td><td>Two</td><td>One</td><td>Two</td></tr></tbody></table></body></html>"
        assertEquals(want, TextUtil.stripNewlines(doc.html()))
    }

    @Test
    fun canSelectIntoTemplate() {
        val html = "<body><div><template><p>Hello</p>"
        val doc = Ksoup.parse(html)
        val want = "<html><head></head><body><div><template><p>Hello</p></template></div></body></html>"
        assertEquals(want, TextUtil.stripNewlines(doc.html()))
        val p = doc.selectFirst("div p")
        val p1 = doc.selectFirst("template :containsOwn(Hello)")
        assertEquals("p", p!!.normalName())
        assertEquals(p, p1)
    }

    @Test
    fun tableRowFragment() {
        val doc = Ksoup.parse("<body><table></table></body")
        val html = "<tr><td><img></td></tr>"
        val table = doc.selectFirst("table")
        table!!.html(html) // invokes the fragment parser with table as context
        var want = "<tbody><tr><td><img></td></tr></tbody>"
        assertEquals(want, TextUtil.stripNewlines(table.html()))
        want = "<table><tbody><tr><td><img></td></tr></tbody></table>"
        assertEquals(want, TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun templateTableRowFragment() {
        // https://github.com/jhy/jsoup/issues/1409 (per the fragment <tr> use case)
        val doc = Ksoup.parse("<body><table><template></template></table></body")
        val html = "<tr><td><img></td></tr>"
        val tmpl = doc.selectFirst("template")
        tmpl!!.html(html) // invokes the fragment parser with template as context
        var want = "<tr><td><img></td></tr>"
        assertEquals(want, TextUtil.stripNewlines(tmpl.html()))
        tmpl.unwrap()
        want = "<html><head></head><body><table><tr><td><img></td></tr></table></body></html>"
        assertEquals(want, TextUtil.stripNewlines(doc.html()))
    }

    @Test
    fun templateNotInTableRowFragment() {
        // https://github.com/jhy/jsoup/issues/1409 (per the fragment <tr> use case)
        val doc = Ksoup.parse("<body><template></template></body")
        val html = "<tr><td><img></td></tr>"
        val tmpl = doc.selectFirst("template")
        tmpl!!.html(html) // invokes the fragment parser with template as context
        var want = "<tr><td><img></td></tr>"
        assertEquals(want, TextUtil.stripNewlines(tmpl.html()))
        tmpl.unwrap()
        want = "<html><head></head><body><tr><td><img></td></tr></body></html>"
        assertEquals(want, TextUtil.stripNewlines(doc.html()))
    }

    @Test
    fun templateFragment() {
        // https://github.com/jhy/jsoup/issues/1315
        val html = "<template id=\"lorem-ipsum\"><tr><td>Lorem</td><td>Ipsum</td></tr></template>"
        val frag = Ksoup.parseBodyFragment(html)
        val want = "<template id=\"lorem-ipsum\"><tr><td>Lorem</td><td>Ipsum</td></tr></template>"
        assertEquals(want, TextUtil.stripNewlines(frag.body().html()))
    }

    @Test
    fun templateInferredForm() {
        // https://github.com/jhy/jsoup/issues/1637 | https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=38987
        val doc = Ksoup.parse("<template><isindex action>")
        assertNotNull(doc)
        assertEquals(
            "<template><form><hr><label>This is a searchable index. Enter search keywords: <input name=\"isindex\"></label><hr></form></template>",
            TextUtil.stripNewlines(doc.head().html()),
        )
    }

    @Test
    fun trimNormalizeElementNamesInBuilder() {
        // https://github.com/jhy/jsoup/issues/1637 | https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=38983
        // This is interesting - in TB state, the element name was "template\u001E", so no name checks matched. Then,
        // when the Element is created, the name got normalized to "template" and so looked like there should be a
        // template on the stack during resetInsertionMode for the select.
        // The issue was that the normalization in Tag.valueOf did a trim which the Token.Tag did not
        val doc = Ksoup.parse("<template\u001E<select<input<")
        assertNotNull(doc)
        assertEquals(
            "<template><select></select><input>&lt;</template>",
            TextUtil.stripNewlines(doc.head().html()),
        )
    }

    @Test
    fun errorsBeforeHtml() {
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)
        val doc = Ksoup.parse("<!doctype html><!doctype something></div>", parser)
        val errors = parser.getErrors()
        assertEquals(2, errors.size)
        assertEquals(
            "<1:36>: Unexpected Doctype token [<!doctype something>] when in state [BeforeHtml]",
            errors[0].toString(),
        )
        assertEquals(
            "<1:42>: Unexpected EndTag token [</div>] when in state [BeforeHtml]",
            errors[1].toString(),
        )
        assertEquals("<!doctype html><html><head></head><body></body></html>", TextUtil.stripNewlines(doc.html()))
    }

    @Test
    fun afterHeadReAdds() {
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)
        val doc = Ksoup.parse("<head></head><meta charset=UTF8><p>Hello", parser)
        val errors = parser.getErrors()
        assertEquals(1, errors.size)
        assertEquals(
            "<1:33>: Unexpected StartTag token [<meta  charset=\"UTF8\">] when in state [AfterHead]",
            errors[0].toString(),
        )
        assertEquals(
            "<html><head><meta charset=\"UTF8\"></head><body><p>Hello</p></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
        // meta gets added back into head
    }

    @Test
    fun mergeHtmlAttributesFromBody() {
        val doc = Ksoup.parse("<html id=1 class=foo><body><html class=bar data=x><p>One")
        assertEquals(
            "<html id=\"1\" class=\"foo\" data=\"x\"><head></head><body><p>One</p></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun mergeHtmlNoAttributesFromBody() {
        val doc = Ksoup.parse("<html id=1 class=foo><body><html><p>One")
        assertEquals(
            "<html id=\"1\" class=\"foo\"><head></head><body><p>One</p></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )
    }

    @Test
    fun supportsRuby() {
        val html =
            "<ruby><rbc><rb>10</rb><rb>31</rb><rb>2002</rb></rbc><rtc><rt>Month</rt><rt>Day</rt><rt>Year</rt></rtc><rtc><rt>Expiration Date</rt><rp>(*)</rtc></ruby>"
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)
        val doc = Ksoup.parse(html, parser)
        val errors = parser.getErrors()
        assertEquals(3, errors.size)
        val ruby = doc.expectFirst("ruby")
        assertEquals(
            "<ruby><rbc><rb>10</rb><rb>31</rb><rb>2002</rb></rbc><rtc><rt>Month</rt><rt>Day</rt><rt>Year</rt></rtc><rtc><rt>Expiration Date</rt><rp>(*)</rp></rtc></ruby>",
            TextUtil.stripNewlines(ruby.outerHtml()),
        )
        assertEquals(
            "<1:38>: Unexpected StartTag token [<rb>] when in state [InBody]",
            errors[2].toString(),
        ) // 3 errors from rb in rtc as undefined
    }

    @Test
    fun rubyRpRtImplicitClose() {
        val html = "<ruby><rp>(<rt>Hello<rt>Hello<rp>)</ruby>\n"
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)
        val doc = Ksoup.parse(html, parser)
        assertEquals(0, parser.getErrors().size)
        val ruby = doc.expectFirst("ruby")
        assertEquals(
            "<ruby><rp>(</rp><rt>Hello</rt><rt>Hello</rt><rp>)</rp></ruby>",
            TextUtil.stripNewlines(ruby.outerHtml()),
        )
    }

    @Test
    fun rubyScopeError() {
        val html = "<ruby><div><rp>Hello"
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)
        val doc = Ksoup.parse(html, parser)
        val errors = parser.getErrors()
        assertEquals(2, errors.size)
        val ruby = doc.expectFirst("ruby")
        assertEquals(
            "<ruby><div><rp>Hello</rp></div></ruby>",
            TextUtil.stripNewlines(ruby.outerHtml()),
        )
        assertEquals("<1:16>: Unexpected StartTag token [<rp>] when in state [InBody]", errors[0].toString())
    }

    @Test
    fun errorOnEofIfOpen() {
        val html = "<div>"
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)
        val doc = Ksoup.parse(html, parser)
        val errors = parser.getErrors()
        assertEquals(1, errors.size)
        assertEquals("Unexpected EOF token [] when in state [InBody]", errors[0].errorMessage)
    }

    @Test
    fun NoErrorOnEofIfBodyOpen() {
        val html = "<body>"
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)
        val doc = Ksoup.parse(html, parser)
        val errors = parser.getErrors()
        assertEquals(0, errors.size)
    }

    @Test
    fun htmlClose() {
        // https://github.com/jhy/jsoup/issues/1851
        val html = "<body><div>One</html>Two</div></body>"
        val doc = Ksoup.parse(html)
        assertEquals("OneTwo", doc.expectFirst("body > div").text())
    }

    @Test
    fun largeTextareaContents() {
        // https://github.com/jhy/jsoup/issues/1929
        val sb = StringBuilder()
        val num = 2000
        for (i in 0..num) {
            sb.append("\n<text>foo</text>\n")
        }
        val textContent = sb.toString()
        val sourceHtml = "<textarea>$textContent</textarea>"
        val doc = Ksoup.parse(sourceHtml)
        val textArea = doc.expectFirst("textarea")
        assertEquals(textContent, textArea.wholeText())
    }

    @Test
    fun svgParseTest() {
        val html = "<div><svg viewBox=2><foreignObject><p>One</p></foreignObject></svg></div>"
        val doc = Ksoup.parse(html)
        assertHtmlNamespace(doc)
        val div = doc.expectFirst("div")
        assertHtmlNamespace(div)
        val svg = doc.expectFirst("svg")
        assertTrue(svg.attributes().hasKey("viewBox"))
        assertSvgNamespace(svg)
        assertSvgNamespace(doc.expectFirst("foreignObject"))
        assertHtmlNamespace(doc.expectFirst("p"))
        val serialized = div.html()
        assertEquals(
            """<svg viewBox="2">
 <foreignObject>
  <p>One</p>
 </foreignObject>
</svg>""",
            serialized,
        )
    }

    @Test
    fun mathParseText() {
        val html = "<div><math><mi><p>One</p><svg><text>Blah</text></svg></mi><ms></ms></div>"
        val doc = Ksoup.parse(html)
        assertHtmlNamespace(doc.expectFirst("div"))
        assertMathNamespace(doc.expectFirst("math"))
        assertMathNamespace(doc.expectFirst("mi"))
        assertHtmlNamespace(doc.expectFirst("p"))
        assertSvgNamespace(doc.expectFirst("svg"))
        assertSvgNamespace(doc.expectFirst("text"))
        assertMathNamespace(doc.expectFirst("ms"))
        val serialized = doc.expectFirst("div").html()
        assertEquals(
            """<math>
 <mi>
  <p>One</p>
  <svg>
   <text>Blah</text>
  </svg></mi><ms></ms>
</math>""",
            serialized,
        )
    }

    @Test
    fun mathSvgStyleTest() {
        val html = "<style><img></style><math><svg><style><img></img></style></svg></math>"
        val doc = Ksoup.parse(html)
        val htmlStyle = doc.expectFirst("style")
        assertHtmlNamespace(htmlStyle)
        assertEquals("<img>", htmlStyle.data()) // that's not an element, it's data (textish)
        val svgStyle = doc.expectFirst("svg style")
        assertMathNamespace(svgStyle) // in inherited math namespace as not an HTML integration point
        val styleImg = svgStyle.expectFirst("img")
        assertHtmlNamespace(styleImg) // this one is an img tag - in foreign to html elements
        assertMathNamespace(doc.expectFirst("svg"))
        assertMathNamespace(doc.expectFirst("math"))
    }

    @Test
    fun xmlnsAttributeError() {
        val html = "<p><svg></svg></body>"
        val parser = Parser.htmlParser().setTrackErrors(10)
        val doc = Ksoup.parse(html, parser)
        assertEquals(0, doc.parser()!!.getErrors().size)
        val html2 =
            "<html xmlns='http://www.w3.org/1999/xhtml'><p xmlns='http://www.w3.org/1999/xhtml'><i xmlns='xhtml'></i></body>"
        val doc2 = Ksoup.parse(html2, parser)
        assertEquals(1, doc2.parser()!!.getErrors().size)
        assertEquals("Invalid xmlns attribute [xhtml] on tag [i]", parser.getErrors()[0].errorMessage)
    }

    @Test
    fun mathAnnotationSvg() {
        val html = "<math><svg>" // not in annotation, svg will be in math ns
        val doc = Ksoup.parse(html)
        assertMathNamespace(doc.expectFirst("math"))
        assertMathNamespace(doc.expectFirst("svg"))
        val html2 = "<math><annotation-xml><svg>" // svg will be in svg ns
        val doc2 = Ksoup.parse(html2)
        assertMathNamespace(doc2.expectFirst("math"))
        assertMathNamespace(doc2.expectFirst("annotation-xml"))
        assertSvgNamespace(doc2.expectFirst("svg"))
    }

    @Test
    fun mathHtmlIntegrationPoint() {
        val html = "<math><div>Hello"
        val doc = Ksoup.parse(html)
        assertMathNamespace(doc.expectFirst("math"))
        assertHtmlNamespace(doc.expectFirst("div"))
        val html2 = "<math><divv>Hello"
        val doc2 = Ksoup.parse(html2)
        assertMathNamespace(doc2.expectFirst("math"))
        assertMathNamespace(doc2.expectFirst("divv"))
        val html3 = "<math><annotation-xml><divv>Hello"
        val doc3 = Ksoup.parse(html3)
        assertMathNamespace(doc3.expectFirst("math"))
        assertMathNamespace(doc3.expectFirst("annotation-xml"))
        assertMathNamespace(doc3.expectFirst("divv"))
        val html4 = "<math><annotation-xml encoding=text/html><divv>Hello"
        val doc4 = Ksoup.parse(html4)
        assertMathNamespace(doc4.expectFirst("math"))
        assertMathNamespace(doc4.expectFirst("annotation-xml"))
        assertHtmlNamespace(doc4.expectFirst("divv"))
    }

    companion object {
        private fun dupeAttributeData(): List<Pair<String, String>> {
            return listOf(
                Pair(
                    "<p One=One ONE=Two Two=two one=Three One=Four two=Five>Text</p>",
                    "<p one=\"One\" two=\"two\">Text</p>",
                ),
                Pair(
                    "<img One=One ONE=Two Two=two one=Three One=Four two=Five>",
                    "<img one=\"One\" two=\"two\">",
                ),
                Pair(
                    "<form One=One ONE=Two Two=two one=Three One=Four two=Five></form>",
                    "<form one=\"One\" two=\"two\"></form>",
                ),
            )
        }

        private fun assertHtmlNamespace(el: Element) {
            assertEquals(Parser.NamespaceHtml, el.tag().namespace())
        }

        private fun assertSvgNamespace(el: Element) {
            assertEquals(Parser.NamespaceSvg, el.tag().namespace())
        }

        private fun assertMathNamespace(el: Element) {
            assertEquals(Parser.NamespaceMathml, el.tag().namespace())
        }
    }
}
