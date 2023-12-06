package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.*
import com.fleeksoft.ksoup.helper.ValidationException
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.parser.Tag
import com.fleeksoft.ksoup.select.Elements
import com.fleeksoft.ksoup.select.NodeFilter
import com.fleeksoft.ksoup.select.NodeVisitor
import com.fleeksoft.ksoup.select.QueryParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for Element (DOM stuff mostly).
 *
 * @author Sabeeh
 */
class ElementTest {
    private val reference =
        "<div id=div1><p>Hello</p><p>Another <b>element</b></p><div id=div2><img src=foo.png></div></div>"

    @Test
    fun testId() {
        val doc = Ksoup.parse("<div id=Foo>")
        val el = doc.selectFirst("div")
        assertEquals("Foo", el!!.id())
    }

    @Test
    fun testSetId() {
        val doc = Ksoup.parse("<div id=Boo>")
        val el = doc.selectFirst("div")
        el!!.id("Foo")
        assertEquals("Foo", el.id())
    }

    @Test
    fun elementsByTagName() {
        val doc = Ksoup.parse(reference)
        val divs: List<Element> = doc.getElementsByTag("div")
        assertEquals(2, divs.size)
        assertEquals("div1", divs[0].id())
        assertEquals("div2", divs[1].id())
        val ps: List<Element> = doc.getElementsByTag("p")
        assertEquals(2, ps.size)
        assertEquals("Hello", (ps[0].childNode(0) as TextNode).getWholeText())
        assertEquals("Another ", (ps[1].childNode(0) as TextNode).getWholeText())
        val ps2: List<Element> = doc.getElementsByTag("P")
        assertEquals(ps, ps2)
        val imgs: List<Element> = doc.getElementsByTag("img")
        assertEquals("foo.png", imgs[0].attr("src"))
        val empty: List<Element> = doc.getElementsByTag("wtf")
        assertEquals(0, empty.size)
    }

    @Test
    fun getNamespacedElementsByTag() {
        val doc = Ksoup.parse("<div><abc:def id=1>Hello</abc:def></div>")
        val els = doc.getElementsByTag("abc:def")
        assertEquals(1, els.size)
        assertEquals("1", els.first()!!.id())
        assertEquals("abc:def", els.first()!!.tagName())
    }

    @Test
    fun testGetElementById() {
        val doc = Ksoup.parse(reference)
        val div = doc.getElementById("div1")
        assertEquals("div1", div!!.id())
        assertNull(doc.getElementById("none"))
        val doc2 =
            Ksoup.parse("<div id=1><div id=2><p>Hello <span id=2>world!</span></p></div></div>")
        val div2 = doc2.getElementById("2")
        assertEquals("div", div2!!.tagName()) // not the span
        val span = div2.child(0).getElementById("2") // called from <p> context should be span
        assertEquals("span", span!!.tagName())
    }

    @Test
    fun testGetText() {
        val doc = Ksoup.parse(reference)
        assertEquals("Hello Another element", doc.text())
        assertEquals("Another element", doc.getElementsByTag("p")[1].text())
    }

    @Test
    fun testGetChildText() {
        val doc = Ksoup.parse("<p>Hello <b>there</b> now")
        val p = doc.select("p").first()
        assertEquals("Hello there now", p!!.text())
        assertEquals("Hello now", p.ownText())
    }

    @Test
    fun testNormalisesText() {
        val h = "<p>Hello<p>There.</p> \n <p>Here <b>is</b> \n s<b>om</b>e text."
        val doc = Ksoup.parse(h)
        val text = doc.text()
        assertEquals("Hello There. Here is some text.", text)
    }

    @Test
    fun testKeepsPreText() {
        val h = "<p>Hello \n \n there.</p> <div><pre>  What's \n\n  that?</pre>"
        val doc = Ksoup.parse(h)
        assertEquals("Hello there.   What's \n\n  that?", doc.text())
    }

    @Test
    fun testKeepsPreTextInCode() {
        val h = "<pre><code>code\n\ncode</code></pre>"
        val doc = Ksoup.parse(h)
        assertEquals("code\n\ncode", doc.text())
        assertEquals("<pre><code>code\n\ncode</code></pre>", doc.body().html())
    }

    @Test
    fun testKeepsPreTextAtDepth() {
        val h = "<pre><code><span><b>code\n\ncode</b></span></code></pre>"
        val doc = Ksoup.parse(h)
        assertEquals("code\n\ncode", doc.text())
        assertEquals("<pre><code><span><b>code\n\ncode</b></span></code></pre>", doc.body().html())
    }

    @Test
    fun doesNotWrapBlocksInPre() {
        // https://github.com/jhy/jsoup/issues/1891
        val h = "<pre><span><foo><div>TEST\n TEST</div></foo></span></pre>"
        val doc = Ksoup.parse(h)
        assertEquals("TEST\n TEST", doc.wholeText())
        assertEquals(h, doc.body().html())
    }

    @Test
    fun testBrHasSpace() {
        var doc = Ksoup.parse("<p>Hello<br>there</p>")
        assertEquals("Hello there", doc.text())
        assertEquals("Hello there", doc.select("p").first()!!.ownText())
        doc = Ksoup.parse("<p>Hello <br> there</p>")
        assertEquals("Hello there", doc.text())
    }

    @Test
    fun testBrHasSpaceCaseSensitive() {
        var doc = Ksoup.parse(
            "<p>Hello<br>there<BR>now</p>",
            Parser.htmlParser().settings(ParseSettings.preserveCase),
        )
        assertEquals("Hello there now", doc.text())
        assertEquals("Hello there now", doc.select("p").first()!!.ownText())
        doc = Ksoup.parse("<p>Hello <br> there <BR> now</p>")
        assertEquals("Hello there now", doc.text())
    }

    @Test
    fun textHasSpacesAfterBlock() {
        val doc = Ksoup.parse("<div>One</div><div>Two</div><span>Three</span><p>Fou<i>r</i></p>")
        val text = doc.text()
        val wholeText = doc.wholeText()
        assertEquals("One Two Three Four", text)
        assertEquals("OneTwoThreeFour", wholeText)
        assertEquals("OneTwo", Ksoup.parse("<span>One</span><span>Two</span>").text())
    }

    @Test
    fun testWholeText() {
        var doc = Ksoup.parse("<p> Hello\nthere &nbsp;  </p>")
        assertEquals(" Hello\nthere    ", doc.wholeText())
        doc = Ksoup.parse("<p>Hello  \n  there</p>")
        assertEquals("Hello  \n  there", doc.wholeText())
        doc = Ksoup.parse("<p>Hello  <div>\n  there</div></p>")
        assertEquals("Hello  \n  there", doc.wholeText())
    }

    @Test
    fun testGetSiblings() {
        val doc = Ksoup.parse("<div><p>Hello<p id=1>there<p>this<p>is<p>an<p id=last>element</div>")
        val p = doc.getElementById("1")
        assertEquals("there", p!!.text())
        assertEquals("Hello", p.previousElementSibling()!!.text())
        assertEquals("this", p.nextElementSibling()!!.text())
        assertEquals("Hello", p.firstElementSibling()!!.text())
        assertEquals("element", p.lastElementSibling()!!.text())
        assertNull(p.lastElementSibling()!!.nextElementSibling())
        assertNull(p.firstElementSibling()!!.previousElementSibling())
    }

    @Test
    fun nextElementSibling() {
        val doc = Ksoup.parse("<p>One</p>Two<p>Three</p>")
        val el = doc.expectFirst("p")
        assertNull(el.previousElementSibling())
        val next = el.nextElementSibling()
        assertNotNull(next)
        assertEquals("Three", next.text())
        assertNull(next.nextElementSibling())
    }

    @Test
    fun prevElementSibling() {
        val doc = Ksoup.parse("<p>One</p>Two<p>Three</p>")
        val el = doc.expectFirst("p:contains(Three)")
        assertNull(el.nextElementSibling())
        val prev = el.previousElementSibling()
        assertNotNull(prev)
        assertEquals("One", prev.text())
        assertNull(prev.previousElementSibling())
    }

    @Test
    fun testGetSiblingsWithDuplicateContent() {
        val doc =
            Ksoup.parse("<div><p>Hello<p id=1>there<p>this<p>this<p>is<p>an<p id=last>element</div>")
        val p = doc.getElementById("1")
        assertEquals("there", p!!.text())
        assertEquals("Hello", p.previousElementSibling()!!.text())
        assertEquals("this", p.nextElementSibling()!!.text())
        assertEquals("this", p.nextElementSibling()!!.nextElementSibling()!!.text())
        assertEquals(
            "is",
            p.nextElementSibling()!!.nextElementSibling()!!
                .nextElementSibling()!!.text(),
        )
        assertEquals("Hello", p.firstElementSibling()!!.text())
        assertEquals("element", p.lastElementSibling()!!.text())
    }

    @Test
    fun testFirstElementSiblingOnOrphan() {
        val p = Element("p")
        assertSame(p, p.firstElementSibling())
        assertSame(p, p.lastElementSibling())
    }

    @Test
    fun testFirstAndLastSiblings() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three")
        val div = doc.expectFirst("div")
        val one = div.child(0)
        val two = div.child(1)
        val three = div.child(2)
        assertSame(one, one.firstElementSibling())
        assertSame(one, two.firstElementSibling())
        assertSame(three, three.lastElementSibling())
        assertSame(three, two.lastElementSibling())
        assertNull(one.previousElementSibling())
        assertNull(three.nextElementSibling())
    }

    @Test
    fun testGetParents() {
        val doc = Ksoup.parse("<div><p>Hello <span>there</span></div>")
        val span = doc.select("span").first()
        val parents = span!!.parents()
        assertEquals(4, parents.size)
        assertEquals("p", parents[0].tagName())
        assertEquals("div", parents[1].tagName())
        assertEquals("body", parents[2].tagName())
        assertEquals("html", parents[3].tagName())
        val orphan = Element("p")
        val none = orphan.parents()
        assertEquals(0, none.size)
    }

    @Test
    fun testElementSiblingIndex() {
        val doc = Ksoup.parse("<div><p>One</p>...<p>Two</p>...<p>Three</p>")
        val ps = doc.select("p")
        assertEquals(0, ps[0].elementSiblingIndex())
        assertEquals(1, ps[1].elementSiblingIndex())
        assertEquals(2, ps[2].elementSiblingIndex())
    }

    @Test
    fun testElementSiblingIndexSameContent() {
        val doc = Ksoup.parse("<div><p>One</p>...<p>One</p>...<p>One</p>")
        val ps = doc.select("p")
        assertEquals(0, ps[0].elementSiblingIndex())
        assertEquals(1, ps[1].elementSiblingIndex())
        assertEquals(2, ps[2].elementSiblingIndex())
    }

    @Test
    fun testGetElementsWithClass() {
        val doc =
            Ksoup.parse("<div class='mellow yellow'><span class=mellow>Hello <b class='yellow'>Yellow!</b></span><p>Empty</p></div>")
        val els: List<Element> = doc.getElementsByClass("mellow")
        assertEquals(2, els.size)
        assertEquals("div", els[0].tagName())
        assertEquals("span", els[1].tagName())
        val els2: List<Element> = doc.getElementsByClass("yellow")
        assertEquals(2, els2.size)
        assertEquals("div", els2[0].tagName())
        assertEquals("b", els2[1].tagName())
        val none: List<Element> = doc.getElementsByClass("solo")
        assertEquals(0, none.size)
    }

    @Test
    fun testGetElementsWithAttribute() {
        val doc = Ksoup.parse("<div style='bold'><p title=qux><p><b style></b></p></div>")
        val els: List<Element> = doc.getElementsByAttribute("style")
        assertEquals(2, els.size)
        assertEquals("div", els[0].tagName())
        assertEquals("b", els[1].tagName())
        val none: List<Element> = doc.getElementsByAttribute("class")
        assertEquals(0, none.size)
    }

    @Test
    fun testGetElementsWithAttributeDash() {
        val doc =
            Ksoup.parse("<meta http-equiv=content-type value=utf8 id=1> <meta name=foo content=bar id=2> <div http-equiv=content-type value=utf8 id=3>")
        val meta = doc.select("meta[http-equiv=content-type], meta[charset]")
        assertEquals(1, meta.size)
        assertEquals("1", meta.first()!!.id())
    }

    @Test
    fun testGetElementsWithAttributeValue() {
        val doc = Ksoup.parse("<div style='bold'><p><p><b style></b></p></div>")
        val els: List<Element> = doc.getElementsByAttributeValue("style", "bold")
        assertEquals(1, els.size)
        assertEquals("div", els[0].tagName())
        val none: List<Element> = doc.getElementsByAttributeValue("style", "none")
        assertEquals(0, none.size)
    }

    @Test
    fun testClassDomMethods() {
        val doc = Ksoup.parse("<div><span class=' mellow yellow '>Hello <b>Yellow</b></span></div>")
        val els: List<Element> = doc.getElementsByAttribute("class")
        val span = els[0]
        assertEquals("mellow yellow", span.className())
        assertTrue(span.hasClass("mellow"))
        assertTrue(span.hasClass("yellow"))
        var classes: Set<String?> = span.classNames()
        assertEquals(2, classes.size)
        assertTrue(classes.contains("mellow"))
        assertTrue(classes.contains("yellow"))
        assertEquals("", doc.className())
        classes = doc.classNames()
        assertEquals(0, classes.size)
        assertFalse(doc.hasClass("mellow"))
    }

    @Test
    fun testHasClassDomMethods() {
        val tag = Tag.valueOf("a")
        val attribs = Attributes()
        val el = Element(tag, "", attribs)
        attribs.put("class", "toto")
        var hasClass = el.hasClass("toto")
        assertTrue(hasClass)
        attribs.put("class", " toto")
        hasClass = el.hasClass("toto")
        assertTrue(hasClass)
        attribs.put("class", "toto ")
        hasClass = el.hasClass("toto")
        assertTrue(hasClass)
        attribs.put("class", "\ttoto ")
        hasClass = el.hasClass("toto")
        assertTrue(hasClass)
        attribs.put("class", "  toto ")
        hasClass = el.hasClass("toto")
        assertTrue(hasClass)
        attribs.put("class", "ab")
        hasClass = el.hasClass("toto")
        assertFalse(hasClass)
        attribs.put("class", "     ")
        hasClass = el.hasClass("toto")
        assertFalse(hasClass)
        attribs.put("class", "tototo")
        hasClass = el.hasClass("toto")
        assertFalse(hasClass)
        attribs.put("class", "raulpismuth  ")
        hasClass = el.hasClass("raulpismuth")
        assertTrue(hasClass)
        attribs.put("class", " abcd  raulpismuth efgh ")
        hasClass = el.hasClass("raulpismuth")
        assertTrue(hasClass)
        attribs.put("class", " abcd efgh raulpismuth")
        hasClass = el.hasClass("raulpismuth")
        assertTrue(hasClass)
        attribs.put("class", " abcd efgh raulpismuth ")
        hasClass = el.hasClass("raulpismuth")
        assertTrue(hasClass)
    }

    @Test
    fun testClassUpdates() {
        val doc = Ksoup.parse("<div class='mellow yellow'></div>")
        val div = doc.select("div").first()
        div!!.addClass("green")
        assertEquals("mellow yellow green", div.className())
        div.removeClass("red") // noop
        div.removeClass("yellow")
        assertEquals("mellow green", div.className())
        div.toggleClass("green").toggleClass("red")
        assertEquals("mellow red", div.className())
    }

    @Test
    fun testOuterHtml() {
        val doc =
            Ksoup.parse("<div title='Tags &amp;c.'><img src=foo.png><p><!-- comment -->Hello<p>there")
        assertEquals(
            "<html><head></head><body><div title=\"Tags &amp;c.\"><img src=\"foo.png\"><p><!-- comment -->Hello</p><p>there</p></div></body></html>",
            TextUtil.stripNewlines(doc.outerHtml()),
        )
    }

    @Test
    fun testInnerHtml() {
        val doc = Ksoup.parse("<div>\n <p>Hello</p> </div>")
        assertEquals("<p>Hello</p>", doc.getElementsByTag("div")[0].html())
    }

    @Test
    fun testFormatHtml() {
        val doc =
            Ksoup.parse("<title>Format test</title><div><p>Hello <span>ksoup <span>users</span></span></p><p>Good.</p></div>")
        assertEquals(
            "<html>\n <head>\n  <title>Format test</title>\n </head>\n <body>\n  <div>\n   <p>Hello <span>ksoup <span>users</span></span></p>\n   <p>Good.</p>\n  </div>\n </body>\n</html>",
            doc.html(),
        )
    }

    @Test
    fun testFormatOutline() {
        val doc =
            Ksoup.parse("<title>Format test</title><div><p>Hello <span>jsoup <span>users</span></span></p><p>Good.</p></div>")
        doc.outputSettings().outline(true)
        assertEquals(
            "<html>\n <head>\n  <title>Format test</title>\n </head>\n <body>\n  <div>\n   <p>\n    Hello \n    <span>\n     jsoup \n     <span>users</span>\n    </span>\n   </p>\n   <p>Good.</p>\n  </div>\n </body>\n</html>",
            doc.html(),
        )
    }

    @Test
    fun testSetIndent() {
        val doc = Ksoup.parse("<div><p>Hello\nthere</p></div>")
        doc.outputSettings().indentAmount(0)
        assertEquals(
            "<html>\n<head></head>\n<body>\n<div>\n<p>Hello there</p>\n</div>\n</body>\n</html>",
            doc.html(),
        )
    }

    @Test
    fun testIndentLevel() {
        // deep to test default and extended max
        val divs = StringBuilder()
        for (i in 0..39) {
            divs.append("<div>")
        }
        divs.append("Foo")
        val doc = Ksoup.parse(divs.toString())
        val settings = doc.outputSettings()
        val defaultMax = 30
        assertEquals(defaultMax, settings.maxPaddingWidth())
        var html = doc.html()
        assertTrue(
            html.contains(
                """                              <div>
                              Foo
                              </div>""",
            ),
        )
        settings.maxPaddingWidth(32)
        assertEquals(32, settings.maxPaddingWidth())
        html = doc.html()
        assertTrue(
            html.contains(
                """                                <div>
                                Foo
                                </div>""",
            ),
        )
        settings.maxPaddingWidth(-1)
        assertEquals(-1, settings.maxPaddingWidth())
        html = doc.html()
        assertTrue(
            html.contains(
                """                                         <div>
                                          Foo
                                         </div>""",
            ),
        )
    }

    @Test
    fun testNotPretty() {
        val doc = Ksoup.parse("<div>   \n<p>Hello\n there\n</p></div>")
        doc.outputSettings().prettyPrint(false)
        assertEquals(
            "<html><head></head><body><div>   \n<p>Hello\n there\n</p></div></body></html>",
            doc.html(),
        )
        val div = doc.select("div").first()
        assertEquals("   \n<p>Hello\n there\n</p>", div!!.html())
    }

    @Test
    fun testNotPrettyWithEnDashBody() {
        val html = "<div><span>1:15</span>&ndash;<span>2:15</span>&nbsp;p.m.</div>"
        val document = Ksoup.parse(html)
        document.outputSettings().prettyPrint(false)
        assertEquals(
            "<div><span>1:15</span>–<span>2:15</span>&nbsp;p.m.</div>",
            document.body().html(),
        )
    }

    @Test
    fun testPrettyWithEnDashBody() {
        val html = "<div><span>1:15</span>&ndash;<span>2:15</span>&nbsp;p.m.</div>"
        val document = Ksoup.parse(html)
        assertEquals(
            "<div>\n <span>1:15</span>–<span>2:15</span>&nbsp;p.m.\n</div>",
            document.body().html(),
        )
    }

    @Test
    fun testPrettyAndOutlineWithEnDashBody() {
        val html = "<div><span>1:15</span>&ndash;<span>2:15</span>&nbsp;p.m.</div>"
        val document = Ksoup.parse(html)
        document.outputSettings().outline(true)
        assertEquals(
            "<div>\n <span>1:15</span>\n –\n <span>2:15</span>\n &nbsp;p.m.\n</div>",
            document.body().html(),
        )
    }

    @Test
    fun testBasicFormats() {
        val html =
            "<span>0</span>.<div><span>1</span>-<span>2</span><p><span>3</span>-<span>4</span><div>5</div>"
        val doc = Ksoup.parse(html)
        assertEquals(
            """<span>0</span>.
<div>
 <span>1</span>-<span>2</span>
 <p><span>3</span>-<span>4</span></p>
 <div>
  5
 </div>
</div>""",
            doc.body().html(),
        )
    }

    @Test
    fun testEmptyElementFormatHtml() {
        // don't put newlines into empty blocks
        val doc = Ksoup.parse("<section><div></div></section>")
        assertEquals(
            "<section>\n <div></div>\n</section>",
            doc.select("section").first()!!
                .outerHtml(),
        )
    }

    @Test
    fun testNoIndentOnScriptAndStyle() {
        // don't newline+indent closing </script> and </style> tags
        val doc = Ksoup.parse("<script>one\ntwo</script>\n<style>three\nfour</style>")
        assertEquals("<script>one\ntwo</script>\n<style>three\nfour</style>", doc.head().html())
    }

    @Test
    fun testContainerOutput() {
        val doc =
            Ksoup.parse("<title>Hello there</title> <div><p>Hello</p><p>there</p></div> <div>Another</div>")
        assertEquals("<title>Hello there</title>", doc.select("title").first()!!.outerHtml())
        assertEquals(
            "<div>\n <p>Hello</p>\n <p>there</p>\n</div>",
            doc.select("div").first()!!
                .outerHtml(),
        )
        assertEquals(
            "<div>\n <p>Hello</p>\n <p>there</p>\n</div>\n<div>\n Another\n</div>",
            doc.select("body").first()!!
                .html(),
        )
    }

    @Test
    fun testSetText() {
        val h = "<div id=1>Hello <p>there <b>now</b></p></div>"
        val doc = Ksoup.parse(h)
        assertEquals("Hello there now", doc.text()) // need to sort out node whitespace
        assertEquals("there now", doc.select("p")[0].text())
        val div = doc.getElementById("1")!!.text("Gone")
        assertEquals("Gone", div.text())
        assertEquals(0, doc.select("p").size)
    }

    @Test
    fun testAddNewElement() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val div = doc.getElementById("1")
        div!!.appendElement("p").text("there")
        div.appendElement("P").attr("CLASS", "second").text("now")
        // manually specifying tag and attributes should maintain case based on parser settings
        assertEquals(
            "<html><head></head><body><div id=\"1\"><p>Hello</p><p>there</p><p class=\"second\">now</p></div></body></html>",
            TextUtil.stripNewlines(doc.html()),
        )

        // check sibling index (with short circuit on reindexChildren):
        val ps = doc.select("p")
        for (i in ps.indices) {
            assertEquals(i, ps[i].siblingIndex)
        }
    }

    @Test
    fun testAddBooleanAttribute() {
        val div = Element(Tag.valueOf("div"), "")
        div.attr("true", true)
        div.attr("false", "value")
        div.attr("false", false)
        assertTrue(div.hasAttr("true"))
        assertEquals("", div.attr("true"))
        val attributes = div.attributes().asList()
        assertEquals(1, attributes.size, "There should be one attribute")
        assertFalse(div.hasAttr("false"))
        assertEquals("<div true></div>", div.outerHtml())
    }

    @Test
    fun testAppendRowToTable() {
        val doc = Ksoup.parse("<table><tr><td>1</td></tr></table>")
        val table = doc.select("tbody").first()
        table!!.append("<tr><td>2</td></tr>")
        assertEquals(
            "<table><tbody><tr><td>1</td></tr><tr><td>2</td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testPrependRowToTable() {
        val doc = Ksoup.parse("<table><tr><td>1</td></tr></table>")
        val table = doc.select("tbody").first()
        table!!.prepend("<tr><td>2</td></tr>")
        assertEquals(
            "<table><tbody><tr><td>2</td></tr><tr><td>1</td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()),
        )

        // check sibling index (reindexChildren):
        val ps = doc.select("tr")
        for (i in ps.indices) {
            assertEquals(i, ps[i].siblingIndex)
        }
    }

    @Test
    fun testPrependElement() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val div = doc.getElementById("1")
        div!!.prependElement("p").text("Before")
        assertEquals("Before", div.child(0).text())
        assertEquals("Hello", div.child(1).text())
    }

    @Test
    fun testAddNewText() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val div = doc.getElementById("1")
        div!!.appendText(" there & now >")
        assertEquals("Hello there & now >", div.text())
        assertEquals(
            "<p>Hello</p> there &amp; now &gt;",
            TextUtil.stripNewlines(div.html()),
        )
    }

    @Test
    fun testPrependText() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val div = doc.getElementById("1")
        div!!.prependText("there & now > ")
        assertEquals("there & now > Hello", div.text())
        assertEquals(
            "there &amp; now &gt; <p>Hello</p>",
            TextUtil.stripNewlines(div.html()),
        )
    }

    @Test
    fun testAddNewHtml() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val div = doc.getElementById("1")
        div!!.append("<p>there</p><p>now</p>")
        assertEquals(
            "<p>Hello</p><p>there</p><p>now</p>",
            TextUtil.stripNewlines(div.html()),
        )

        // check sibling index (no reindexChildren):
        val ps = doc.select("p")
        for (i in ps.indices) {
            assertEquals(i, ps[i].siblingIndex)
        }
    }

    @Test
    fun testPrependNewHtml() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val div = doc.getElementById("1")
        div!!.prepend("<p>there</p><p>now</p>")
        assertEquals(
            "<p>there</p><p>now</p><p>Hello</p>",
            TextUtil.stripNewlines(div.html()),
        )

        // check sibling index (reindexChildren):
        val ps = doc.select("p")
        for (i in ps.indices) {
            assertEquals(i, ps[i].siblingIndex)
        }
    }

    @Test
    fun prependNodes() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val p = doc.expectFirst("p")
        p.prepend("Text <!-- comment --> ")
        assertEquals(
            "Text <!-- comment --> Hello",
            TextUtil.stripNewlines(p.html()),
        )
    }

    @Test
    fun appendNodes() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val p = doc.expectFirst("p")
        p.append(" Text <!-- comment -->")
        assertEquals(
            "Hello Text <!-- comment -->",
            TextUtil.stripNewlines(p.html()),
        )
    }

    @Test
    fun testSetHtml() {
        val doc = Ksoup.parse("<div id=1><p>Hello</p></div>")
        val div = doc.getElementById("1")
        div!!.html("<p>there</p><p>now</p>")
        assertEquals(
            "<p>there</p><p>now</p>",
            TextUtil.stripNewlines(div.html()),
        )
    }

    @Test
    fun testSetHtmlTitle() {
        val doc = Ksoup.parse("<html><head id=2><title id=1></title></head></html>")
        val title = doc.getElementById("1")
        title!!.html("good")
        assertEquals("good", title.html())
        title.html("<i>bad</i>")
        assertEquals("&lt;i&gt;bad&lt;/i&gt;", title.html())
        val head = doc.getElementById("2")
        head!!.html("<title><i>bad</i></title>")
        assertEquals("<title>&lt;i&gt;bad&lt;/i&gt;</title>", head.html())
    }

    @Test
    fun testWrap() {
        val doc = Ksoup.parse("<div><p>Hello</p><p>There</p></div>")
        val p = doc.select("p").first()
        p!!.wrap("<div class='head'></div>")
        assertEquals(
            "<div><div class=\"head\"><p>Hello</p></div><p>There</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
        val ret = p.wrap("<div><div class=foo></div><p>What?</p></div>")
        assertEquals(
            "<div><div class=\"head\"><div><div class=\"foo\"><p>Hello</p></div><p>What?</p></div></div><p>There</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
        assertEquals(ret, p)
    }

    @Test
    fun testWrapNoop() {
        val doc = Ksoup.parse("<div><p>Hello</p></div>")
        val p: Node = doc.select("p").first()!!
        val wrapped = p.wrap("Some junk")
        assertSame(p, wrapped)
        assertEquals(
            "<div><p>Hello</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
        // should be a NOOP
    }

    @Test
    fun testWrapOnOrphan() {
        val orphan: Element = Element("span").text("Hello!")
        assertFalse(orphan.hasParent())
        val wrapped: Element = orphan.wrap("<div></div> There!")
        assertSame(orphan, wrapped)
        assertTrue(orphan.hasParent()) // should now be in the DIV
        assertNotNull(orphan.parent())
        assertEquals("div", orphan.parent()!!.tagName())
        assertEquals("<div>\n <span>Hello!</span>\n</div>", orphan.parent()!!.outerHtml())
    }

    @Test
    fun testWrapArtificialStructure() {
        // div normally couldn't get into a p, but explicitly want to wrap
        val doc = Ksoup.parse("<p>Hello <i>there</i> now.")
        val i = doc.selectFirst("i")
        i!!.wrap("<div id=id1></div> quite")
        assertEquals("div", i.parent()!!.tagName())
        assertEquals(
            "<p>Hello <div id=\"id1\"><i>there</i></div> quite now.</p>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun before() {
        val doc = Ksoup.parse("<div><p>Hello</p><p>There</p></div>")
        val p1 = doc.select("p").first()
        p1!!.before("<div>one</div><div>two</div>")
        assertEquals(
            "<div><div>one</div><div>two</div><p>Hello</p><p>There</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
        doc.select("p").last()!!.before("<p>Three</p><!-- four -->")
        assertEquals(
            "<div><div>one</div><div>two</div><p>Hello</p><p>Three</p><!-- four --><p>There</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun after() {
        val doc = Ksoup.parse("<div><p>Hello</p><p>There</p></div>")
        val p1 = doc.select("p").first()
        p1!!.after("<div>one</div><div>two</div>")
        assertEquals(
            "<div><p>Hello</p><div>one</div><div>two</div><p>There</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
        doc.select("p").last()!!.after("<p>Three</p><!-- four -->")
        assertEquals(
            "<div><p>Hello</p><div>one</div><div>two</div><p>There</p><p>Three</p><!-- four --></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testWrapWithRemainder() {
        val doc = Ksoup.parse("<div><p>Hello</p></div>")
        val p = doc.select("p").first()
        p!!.wrap("<div class='head'></div><p>There!</p>")
        assertEquals(
            "<div><div class=\"head\"><p>Hello</p></div><p>There!</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testWrapWithSimpleRemainder() {
        val doc = Ksoup.parse("<p>Hello")
        val p = doc.selectFirst("p")
        val body = p!!.parent()
        assertNotNull(body)
        assertEquals("body", body.tagName())
        p.wrap("<div></div> There")
        val div = p.parent()
        assertNotNull(div)
        assertEquals("div", div.tagName())
        assertSame(div, p.parent())
        assertSame(body, div.parent())
        assertEquals(
            "<div><p>Hello</p></div> There",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testHasText() {
        val doc = Ksoup.parse("<div><p>Hello</p><p></p></div>")
        val div = doc.select("div").first()
        val ps = doc.select("p")
        assertTrue(div!!.hasText())
        assertTrue(ps.first()!!.hasText())
        assertFalse(ps.last()!!.hasText())
    }

    @Test
    fun dataset() {
        val doc =
            Ksoup.parse("<div id=1 data-name=jsoup class=new data-package=jar>Hello</div><p id=2>Hello</p>")
        val div = doc.select("div").first()!!
        val dataset: Attributes.Dataset = div.dataset()
        val attributes = div.attributes()

        // size, get, set, add, remove
        assertEquals(2, dataset.size)
        assertEquals("jsoup", dataset["name"])
        assertEquals("jar", dataset["package"])
        dataset["name"] = "jsoup updated"
        dataset["language"] = "java"
        dataset.remove("package")
        assertEquals(2, dataset.size)
        assertEquals(4, attributes.size())
        assertEquals("jsoup updated", attributes["data-name"])
        assertEquals("jsoup updated", dataset["name"])
        assertEquals("java", attributes["data-language"])
        assertEquals("java", dataset["language"])
        attributes.put("data-food", "bacon")
        assertEquals(3, dataset.size)
        assertEquals("bacon", dataset["food"])
        attributes.put("data-", "empty")
        assertNull(dataset[""]) // data- is not a data attribute
        val p = doc.select("p").first()
        assertEquals(0, p!!.dataset().size)
    }

    @Test
    fun parentlessToString() {
        val doc = Ksoup.parse("<img src='foo'>")
        val img = doc.select("img").first()
        assertEquals("<img src=\"foo\">", img.toString())
        img!!.remove() // lost its parent
        assertEquals("<img src=\"foo\">", img.toString())
    }

    @Test
    fun orphanDivToString() {
        val orphan = Element("div").id("foo").text("Hello")
        assertEquals("<div id=\"foo\">\n Hello\n</div>", orphan.toString())
    }

    @Test
    fun testClone() {
        val doc = Ksoup.parse("<div><p>One<p><span>Two</div>")
        val p = doc.select("p")[1]
        val clone = p.clone()
        assertNotNull(clone._parentNode) // should be a cloned document just containing this clone
        assertEquals(1, clone._parentNode!!.childNodeSize())
        assertSame(clone.ownerDocument(), clone._parentNode)
        assertEquals(0, clone.siblingIndex)
        assertEquals(1, p.siblingIndex)
        assertNotNull(p.parent())
        clone.append("<span>Three")
        assertEquals(
            "<p><span>Two</span><span>Three</span></p>",
            TextUtil.stripNewlines(clone.outerHtml()),
        )
        assertEquals(
            "<div><p>One</p><p><span>Two</span></p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        ) // not modified
        doc.body().appendChild(clone) // adopt
        assertNotNull(clone.parent())
        assertEquals(
            "<div><p>One</p><p><span>Two</span></p></div><p><span>Two</span><span>Three</span></p>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testClonesClassnames() {
        val doc = Ksoup.parse("<div class='one two'></div>")
        val div = doc.select("div").first()
        val classes: Set<String> = div!!.classNames()
        assertEquals(2, classes.size)
        assertTrue(classes.contains("one"))
        assertTrue(classes.contains("two"))
        val copy = div.clone()
        val copyClasses = copy.classNames()
        assertEquals(2, copyClasses.size)
        assertTrue(copyClasses.contains("one"))
        assertTrue(copyClasses.contains("two"))
        copyClasses.add("three")
        copyClasses.remove("one")
        assertTrue(classes.contains("one"))
        assertFalse(classes.contains("three"))
        assertFalse(copyClasses.contains("one"))
        assertTrue(copyClasses.contains("three"))
        assertEquals("", div.html())
        assertEquals("", copy.html())
    }

    @Test
    fun testShallowClone() {
        val base = "http://example.com/"
        val doc = Ksoup.parse("<div id=1 class=one><p id=2 class=two>One", base)
        val d = doc.selectFirst("div")
        val p = doc.selectFirst("p")
        val t = p!!.textNodes()[0]
        val d2 = d!!.shallowClone()
        val p2 = p.shallowClone()
        val t2 = t.shallowClone() as TextNode
        assertEquals(1, d.childNodeSize())
        assertEquals(0, d2.childNodeSize())
        assertEquals(1, p.childNodeSize())
        assertEquals(0, p2.childNodeSize())
        assertEquals("", p2.text())
        assertEquals("One", t2.text())
        assertEquals("two", p2.className())
        p2.removeClass("two")
        assertEquals("two", p.className())
        d2.append("<p id=3>Three")
        assertEquals(1, d2.childNodeSize())
        assertEquals("Three", d2.text())
        assertEquals("One", d.text())
        assertEquals(base, d2.baseUri())
    }

    @Test
    fun testTagNameSet() {
        val doc = Ksoup.parse("<div><i>Hello</i>")
        doc.select("i").first()!!.tagName("em")
        assertEquals(0, doc.select("i").size)
        assertEquals(1, doc.select("em").size)
        assertEquals("<em>Hello</em>", doc.select("div").first()!!.html())
    }

    @Test
    fun testHtmlContainsOuter() {
        val doc = Ksoup.parse("<title>Check</title> <div>Hello there</div>")
        doc.outputSettings().indentAmount(0)
        assertTrue(doc.html().contains(doc.select("title").outerHtml()))
        assertTrue(doc.html().contains(doc.select("div").outerHtml()))
    }

    @Test
    fun testGetTextNodes() {
        val doc = Ksoup.parse("<p>One <span>Two</span> Three <br> Four</p>")
        val textNodes = doc.select("p").first()!!.textNodes()
        assertEquals(3, textNodes.size)
        assertEquals("One ", textNodes[0].text())
        assertEquals(" Three ", textNodes[1].text())
        assertEquals(" Four", textNodes[2].text())
        assertEquals(0, doc.select("br").first()!!.textNodes().size)
    }

    @Test
    fun testManipulateTextNodes() {
        val doc = Ksoup.parse("<p>One <span>Two</span> Three <br> Four</p>")
        val p = doc.select("p").first()
        val textNodes = p!!.textNodes()
        textNodes[1].text(" three-more ")
        textNodes[2].splitText(3).text("-ur")
        assertEquals("One Two three-more Fo-ur", p.text())
        assertEquals("One three-more Fo-ur", p.ownText())
        assertEquals(4, p.textNodes().size) // grew because of split
    }

    @Test
    fun testGetDataNodes() {
        val doc = Ksoup.parse("<script>One Two</script> <style>Three Four</style> <p>Fix Six</p>")
        val script = doc.select("script").first()
        val style = doc.select("style").first()
        val p = doc.select("p").first()
        val scriptData = script!!.dataNodes()
        assertEquals(1, scriptData.size)
        assertEquals("One Two", scriptData[0].getWholeData())
        val styleData = style!!.dataNodes()
        assertEquals(1, styleData.size)
        assertEquals("Three Four", styleData[0].getWholeData())
        val pData = p!!.dataNodes()
        assertEquals(0, pData.size)
    }

    @Test
    fun elementIsNotASiblingOfItself() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div>")
        val p2 = doc.select("p")[1]
        assertEquals("Two", p2.text())
        val els = p2.siblingElements()
        assertEquals(2, els.size)
        assertEquals("<p>One</p>", els[0].outerHtml())
        assertEquals("<p>Three</p>", els[1].outerHtml())
    }

    @Test
    fun testChildThrowsIndexOutOfBoundsOnMissing() {
        val doc = Ksoup.parse("<div><p>One</p><p>Two</p></div>")
        val div = doc.select("div").first()
        assertEquals(2, div!!.children().size)
        assertEquals("One", div.child(0).text())
        try {
            div.child(3)
            fail("Should throw index out of bounds")
        } catch (e: IndexOutOfBoundsException) {
        }
    }

    @Test
    fun moveByAppend() {
        // test for https://github.com/jhy/jsoup/issues/239
        // can empty an element and append its children to another element
        val doc = Ksoup.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>")
        val div1 = doc.select("div")[0]
        val div2 = doc.select("div")[1]
        assertEquals(4, div1.childNodeSize())
        val children = div1.childNodes()
        assertEquals(4, children.size)
        div2.insertChildren(0, children)
        assertEquals(
            4,
            children.size,
        ) // children is NOT backed by div1.childNodes but a wrapper, so should still be 4 (but re-parented)
        assertEquals(0, div1.childNodeSize())
        assertEquals(4, div2.childNodeSize())
        assertEquals(
            "<div id=\"1\"></div>\n<div id=\"2\">\n Text \n <p>One</p> Text \n <p>Two</p>\n</div>",
            doc.body().html(),
        )
    }

    @Test
    fun insertChildrenArgumentValidation() {
        val doc = Ksoup.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>")
        val div1 = doc.select("div")[0]
        val div2 = doc.select("div")[1]
        val children = div1.childNodes()
        try {
            div2.insertChildren(6, children)
            fail()
        } catch (e: IllegalArgumentException) {
        }
        try {
            div2.insertChildren(-5, children)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun insertChildrenAtPosition() {
        val doc =
            Ksoup.parse("<div id=1>Text1 <p>One</p> Text2 <p>Two</p></div><div id=2>Text3 <p>Three</p></div>")
        val div1 = doc.select("div")[0]
        val p1s = div1.select("p")
        val div2 = doc.select("div")[1]
        assertEquals(2, div2.childNodeSize())
        div2.insertChildren(-1, p1s)
        assertEquals(2, div1.childNodeSize()) // moved two out
        assertEquals(4, div2.childNodeSize())
        assertEquals(3, p1s[1].siblingIndex()) // should be last
        val els: MutableList<Node> = ArrayList()
        val el1 = Element(Tag.valueOf("span"), "").text("Span1")
        val el2 = Element(Tag.valueOf("span"), "").text("Span2")
        val tn1 = TextNode("Text4")
        els.add(el1)
        els.add(el2)
        els.add(tn1)
        assertNull(el1.parent())
        div2.insertChildren(-2, els)
        assertEquals(div2, el1.parent())
        assertEquals(7, div2.childNodeSize())
        assertEquals(3, el1.siblingIndex())
        assertEquals(4, el2.siblingIndex())
        assertEquals(5, tn1.siblingIndex())
    }

    @Test
    fun insertChildrenAsCopy() {
        val doc = Ksoup.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>")
        val div1 = doc.select("div")[0]
        val div2 = doc.select("div")[1]
        val ps = doc.select("p").clone()
        ps.first()!!.text("One cloned")
        div2.insertChildren(-1, ps)
        assertEquals(4, div1.childNodeSize()) // not moved -- cloned
        assertEquals(2, div2.childNodeSize())
        assertEquals(
            "<div id=\"1\">Text <p>One</p> Text <p>Two</p></div><div id=\"2\"><p>One cloned</p><p>Two</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testCssPath() {
        val doc = Ksoup.parse("<div id=\"id1\">A</div><div>B</div><div class=\"c1 c2\">C</div>")
        val divA = doc.select("div")[0]
        val divB = doc.select("div")[1]
        val divC = doc.select("div")[2]
        assertEquals(divA.cssSelector(), "#id1")
        assertEquals(divB.cssSelector(), "html > body > div:nth-child(2)")
        assertEquals(divC.cssSelector(), "html > body > div.c1.c2")
        assertSame(divA, doc.select(divA.cssSelector()).first())
        assertSame(divB, doc.select(divB.cssSelector()).first())
        assertSame(divC, doc.select(divC.cssSelector()).first())
    }

    @Test
    fun testCssPathDuplicateIds() {
        // https://github.com/jhy/jsoup/issues/1147 - multiple elements with same ID, use the non-ID form
        val doc =
            Ksoup.parse("<article><div id=dupe>A</div><div id=dupe>B</div><div id=dupe class=c1>")
        val divA = doc.select("div")[0]
        val divB = doc.select("div")[1]
        val divC = doc.select("div")[2]
        assertEquals(divA.cssSelector(), "html > body > article > div:nth-child(1)")
        assertEquals(divB.cssSelector(), "html > body > article > div:nth-child(2)")
        assertEquals(divC.cssSelector(), "html > body > article > div.c1")
        assertSame(divA, doc.select(divA.cssSelector()).first())
        assertSame(divB, doc.select(divB.cssSelector()).first())
        assertSame(divC, doc.select(divC.cssSelector()).first())
    }

    @Test
    fun cssSelectorEscaped() {
        // https://github.com/jhy/jsoup/issues/1742
        val doc =
            Ksoup.parse("<p\\p>One</p\\p> <p id='one.two'>Two</p> <p class='one.two:three/four'>Three</p>")
        val one = doc.expectFirst("p\\\\p")
        val ps = doc.select("p")
        val two = ps[0]
        val three = ps[1]
        val oneSelect = one.cssSelector()
        assertEquals("html > body > p\\\\p", oneSelect)
        assertEquals(one, doc.expectFirst(oneSelect))
        val twoSelect = two.cssSelector()
        assertEquals("#one\\.two", twoSelect)
        assertEquals(two, doc.expectFirst(twoSelect))
        val threeSelect = three.cssSelector()
        assertEquals("html > body > p.one\\.two\\:three\\/four", threeSelect)
        assertEquals(three, doc.expectFirst(threeSelect))
    }

    @Test
    fun cssEscapedAmp() {
        val doc = Ksoup.parse("<p class='\\&'>One</p>")
        val one = doc.expectFirst(".\\\\\\&") // tested matches js querySelector
        assertEquals("One", one.text())
        val q = one.cssSelector()
        assertEquals("html > body > p.\\\\\\&", q)
        assertEquals(one, doc.expectFirst(q))
    }

    @Test
    fun cssSelectorEscapedClass() {
        // example in https://github.com/jhy/jsoup/issues/838
        val html = "<div class='B\\&W\\?'><div class=test>Text</div></div>"
        val parse = Ksoup.parse(html)
        val el = parse.expectFirst(".test")
        assertEquals("Text", el.text())
        val q = el.cssSelector()
        assertEquals("html > body > div.B\\\\\\&W\\\\\\? > div.test", q)
        val found = parse.expectFirst(q)
        assertEquals(found, el)
    }

    @Test
    fun testClassNames() {
        val doc = Ksoup.parse("<div class=\"c1 c2\">C</div>")
        val div = doc.select("div")[0]
        assertEquals("c1 c2", div.className())
        val set1 = div.classNames()
        val arr1: Array<Any> = set1.toTypedArray()
        assertEquals(2, arr1.size)
        assertEquals("c1", arr1[0])
        assertEquals("c2", arr1[1])

        // Changes to the set should not be reflected in the Elements getters
        set1.add("c3")
        assertEquals(2, div.classNames().size)
        assertEquals("c1 c2", div.className())

        // Update the class names to a fresh set
        val newSet: MutableSet<String> = LinkedHashSet(3)
        newSet.addAll(set1)
        newSet.add("c3")
        div.classNames(newSet)
        assertEquals("c1 c2 c3", div.className())
        val set2: Set<String> = div.classNames()
        val arr2: Array<Any> = set2.toTypedArray()
        assertEquals(3, arr2.size)
        assertEquals("c1", arr2[0])
        assertEquals("c2", arr2[1])
        assertEquals("c3", arr2[2])
    }

    @Test
    fun testHashAndEqualsAndValue() {
        // .equals and hashcode are identity. value is content.
        val doc1 =
            "<div id=1><p class=one>One</p><p class=one>One</p><p class=one>Two</p><p class=two>One</p></div>" +
                "<div id=2><p class=one>One</p><p class=one>One</p><p class=one>Two</p><p class=two>One</p></div>"
        val doc = Ksoup.parse(doc1)
        val els = doc.select("p")

        /*
        for (Element el : els) {
            System.out.println(el.hashCode() + " - " + el.outerHtml());
        }

        0 1534787905 - <p class="one">One</p>
        1 1534787905 - <p class="one">One</p>
        2 1539683239 - <p class="one">Two</p>
        3 1535455211 - <p class="two">One</p>
        4 1534787905 - <p class="one">One</p>
        5 1534787905 - <p class="one">One</p>
        6 1539683239 - <p class="one">Two</p>
        7 1535455211 - <p class="two">One</p>
        */assertEquals(8, els.size)
        val e0 = els[0]
        val e1 = els[1]
        val e2 = els[2]
        val e3 = els[3]
        val e4 = els[4]
        val e5 = els[5]
        val e6 = els[6]
        val e7 = els[7]
        assertEquals(e0, e0)
        assertTrue(e0.hasSameValue(e1))
        assertTrue(e0.hasSameValue(e4))
        assertTrue(e0.hasSameValue(e5))
        assertNotEquals(e0, e2)
        assertFalse(e0.hasSameValue(e2))
        assertFalse(e0.hasSameValue(e3))
        assertFalse(e0.hasSameValue(e6))
        assertFalse(e0.hasSameValue(e7))
        assertEquals(e0.hashCode(), e0.hashCode())
        assertNotEquals(e0.hashCode(), e2.hashCode())
        assertNotEquals(e0.hashCode(), e3.hashCode())
        assertNotEquals(e0.hashCode(), e6.hashCode())
        assertNotEquals(e0.hashCode(), e7.hashCode())
    }

    @Test
    fun testRelativeUrls() {
        val html =
            "<body><a href='./one.html'>One</a> <a href='two.html'>two</a> <a href='../three.html'>Three</a> <a href='//example2.com/four/'>Four</a> <a href='https://example2.com/five/'>Five</a> <a>Six</a> <a href=''>Seven</a>"
        val doc = Ksoup.parse(html, "http://example.com/bar/")
        val els = doc.select("a")
        assertEquals("http://example.com/bar/one.html", els[0].absUrl("href"))
        assertEquals("http://example.com/bar/two.html", els[1].absUrl("href"))
        assertEquals("http://example.com/three.html", els[2].absUrl("href"))
        assertEquals("http://example2.com/four/", els[3].absUrl("href"))
        assertEquals("https://example2.com/five/", els[4].absUrl("href"))
        assertEquals("", els[5].absUrl("href"))
        assertEquals("http://example.com/bar/", els[6].absUrl("href"))
    }

    @Test
    fun testRelativeIdnUrls() {
        val idn = "https://www.测试.测试/"
        val idnFoo = idn + "foo.html?bar"
        val doc = Ksoup.parse("<a href=''>One</a><a href='/bar.html?qux'>Two</a>", idnFoo)
        val els = doc.select("a")
        val one = els[0]
        val two = els[1]
        val hrefOne = one.absUrl("href")
        val hrefTwo = two.absUrl("href")
        assertEquals(idnFoo, hrefOne)
        assertEquals("https://www.测试.测试/bar.html?qux", hrefTwo)
    }

    @Test
    fun appendMustCorrectlyMoveChildrenInsideOneParentElement() {
        val doc = Document("")
        val body = doc.appendElement("body")
        body.appendElement("div1")
        body.appendElement("div2")
        val div3 = body.appendElement("div3")
        div3.text("Check")
        val div4 = body.appendElement("div4")
        val toMove = ArrayList<Element>()
        toMove.add(div3)
        toMove.add(div4)
        body.insertChildren(0, toMove)
        val result = doc.toString().replace("\\s+".toRegex(), "")
        assertEquals(
            "<body><div3>Check</div3><div4></div4><div1></div1><div2></div2></body>",
            result,
        )
    }

    @Test
    fun testHashcodeIsStableWithContentChanges() {
        val root = Element(Tag.valueOf("root"), "")
        val set = HashSet<Element>()
        // Add root node:
        set.add(root)
        root.appendChild(Element(Tag.valueOf("a"), ""))
        assertTrue(set.contains(root))
    }

    @Test
    fun testNamespacedElements() {
        // Namespaces with ns:tag in HTML must be translated to ns|tag in CSS.
        val html = "<html><body><fb:comments /></body></html>"
        val doc = Ksoup.parse(html, "http://example.com/bar/")
        val els = doc.select("fb|comments")
        assertEquals(1, els.size)
        assertEquals("html > body > fb|comments", els[0].cssSelector())
    }

    @Test
    fun testChainedRemoveAttributes() {
        val html = "<a one two three four>Text</a>"
        val doc = Ksoup.parse(html)
        val a: Element = doc.select("a").first()!!
        a
            .removeAttr("zero")
            .removeAttr("one")
            .removeAttr("two")
            .removeAttr("three")
            .removeAttr("four")
            .removeAttr("five")
        assertEquals("<a>Text</a>", a.outerHtml())
    }

    @Test
    fun testLoopedRemoveAttributes() {
        val html = "<a one two three four>Text</a><p foo>Two</p>"
        val doc = Ksoup.parse(html)
        for (el in doc.getAllElements()) {
            el.clearAttributes()
        }
        assertEquals("<a>Text</a>\n<p>Two</p>", doc.body().html())
    }

    @Test
    fun testIs() {
        val html = "<div><p>One <a class=big>Two</a> Three</p><p>Another</p>"
        val doc = Ksoup.parse(html)
        val p = doc.select("p").first()
        assertTrue(p!!.`is`("p"))
        assertFalse(p.`is`("div"))
        assertTrue(p.`is`("p:has(a)"))
        assertFalse(p.`is`("a")) // does not descend
        assertTrue(p.`is`("p:first-child"))
        assertFalse(p.`is`("p:last-child"))
        assertTrue(p.`is`("*"))
        assertTrue(p.`is`("div p"))
        val q = doc.select("p").last()
        assertTrue(q!!.`is`("p"))
        assertTrue(q.`is`("p ~ p"))
        assertTrue(q.`is`("p + p"))
        assertTrue(q.`is`("p:last-child"))
        assertFalse(q.`is`("p a"))
        assertFalse(q.`is`("a"))
    }

    @Test
    fun testEvalMethods() {
        val doc = Ksoup.parse("<div><p>One <a class=big>Two</a> Three</p><p>Another</p>")
        val p = doc.selectFirst(QueryParser.parse("p"))
        assertEquals("One Three", p!!.ownText())
        assertTrue(p.`is`(QueryParser.parse("p")))
        val aEval = QueryParser.parse("a")
        assertFalse(p.`is`(aEval))
        val a = p.selectFirst(aEval)
        assertEquals(
            "div",
            a!!.closest(QueryParser.parse("div:has( > p)"))!!
                .tagName(),
        )
        val body = p.closest(QueryParser.parse("body"))
        assertEquals("body", body!!.nodeName())
    }

    @Test
    fun testClosest() {
        val html = """<article>
  <div id=div-01>Here is div-01
    <div id=div-02>Here is div-02
      <div id=div-03>Here is div-03</div>
    </div>
  </div>
</article>"""
        val doc = Ksoup.parse(html)
        val el = doc.selectFirst("#div-03")
        assertEquals("Here is div-03", el!!.text())
        assertEquals("div-03", el.id())
        assertEquals("div-02", el.closest("#div-02")!!.id())
        assertEquals(el, el.closest("div div")) // closest div in a div is itself
        assertEquals("div-01", el.closest("article > div")!!.id())
        assertEquals("article", el.closest(":not(div)")!!.tagName())
        assertNull(el.closest("p"))
    }

    @Test
    fun elementByTagName() {
        val a = Element("P")
        assertEquals("P", a.tagName())
    }

    @Test
    fun testChildrenElements() {
        val html =
            "<div><p><a>One</a></p><p><a>Two</a></p>Three</div><span>Four</span><foo></foo><img>"
        val doc = Ksoup.parse(html)
        val div = doc.select("div").first()
        val p = doc.select("p").first()
        val span = doc.select("span").first()
        val foo = doc.select("foo").first()
        val img = doc.select("img").first()
        val docChildren = div!!.children()
        assertEquals(2, docChildren.size)
        assertEquals("<p><a>One</a></p>", docChildren[0].outerHtml())
        assertEquals("<p><a>Two</a></p>", docChildren[1].outerHtml())
        assertEquals(3, div.childNodes().size)
        assertEquals("Three", div.childNodes()[2].outerHtml())
        assertEquals(1, p!!.children().size)
        assertEquals("One", p.children().text())
        assertEquals(0, span!!.children().size)
        assertEquals(1, span.childNodes().size)
        assertEquals("Four", span.childNodes()[0].outerHtml())
        assertEquals(0, foo!!.children().size)
        assertEquals(0, foo.childNodes().size)
        assertEquals(0, img!!.children().size)
        assertEquals(0, img.childNodes().size)
    }

    @Test
    fun testShadowElementsAreUpdated() {
        val html =
            "<div><p><a>One</a></p><p><a>Two</a></p>Three</div><span>Four</span><foo></foo><img>"
        val doc: Document = Ksoup.parse(html)
        val div: Element = doc.select("div").first()!!
        val els = div.children()
        val nodes = div.childNodes()
        assertEquals(2, els.size) // the two Ps
        assertEquals(3, nodes.size) // the "Three" textnode
        val p3 = Element("p").text("P3")
        val p4 = Element("p").text("P4")
        div.insertChildren(1, p3)
        div.insertChildren(3, p4)
        val els2 = div.children()

        // first els should not have changed
        assertEquals(2, els.size)
        assertEquals(4, els2.size)
        assertEquals(
            """
    <p><a>One</a></p>
    <p>P3</p>
    <p><a>Two</a></p>
    <p>P4</p>Three
            """.trimIndent(),
            div.html(),
        )
        assertEquals("P3", els2[1].text())
        assertEquals("P4", els2[3].text())
        p3.after("<span>Another</span")
        val els3 = div.children()
        assertEquals(5, els3.size)
        assertEquals("span", els3[2].tagName())
        assertEquals("Another", els3[2].text())
        assertEquals(
            """
    <p><a>One</a></p>
    <p>P3</p><span>Another</span>
    <p><a>Two</a></p>
    <p>P4</p>Three
            """.trimIndent(),
            div.html(),
        )
    }

    @Test
    fun classNamesAndAttributeNameIsCaseInsensitive() {
        val html = "<p Class='SomeText AnotherText'>One</p>"
        val doc = Ksoup.parse(html)
        val p = doc.select("p").first()
        assertEquals("SomeText AnotherText", p!!.className())
        assertTrue(p.classNames().contains("SomeText"))
        assertTrue(p.classNames().contains("AnotherText"))
        assertTrue(p.hasClass("SomeText"))
        assertTrue(p.hasClass("sometext"))
        assertTrue(p.hasClass("AnotherText"))
        assertTrue(p.hasClass("anothertext"))
        val p1 = doc.select(".SomeText").first()
        val p2 = doc.select(".sometext").first()
        val p3 = doc.select("[class=SomeText AnotherText]").first()
        val p4 = doc.select("[Class=SomeText AnotherText]").first()
        val p5 = doc.select("[class=sometext anothertext]").first()
        val p6 = doc.select("[class=SomeText AnotherText]").first()
        val p7 = doc.select("[class^=sometext]").first()
        val p8 = doc.select("[class$=nothertext]").first()
        val p9 = doc.select("[class^=sometext]").first()
        val p10 = doc.select("[class$=AnotherText]").first()
        assertEquals("One", p1!!.text())
        assertEquals(p1, p2)
        assertEquals(p1, p3)
        assertEquals(p1, p4)
        assertEquals(p1, p5)
        assertEquals(p1, p6)
        assertEquals(p1, p7)
        assertEquals(p1, p8)
        assertEquals(p1, p9)
        assertEquals(p1, p10)
    }

    @Test
    fun testAppendTo() {
        val parentHtml = "<div class='a'></div>"
        val childHtml = "<div class='b'></div><p>Two</p>"
        val parentDoc = Ksoup.parse(parentHtml)
        val parent = parentDoc.body()
        val childDoc = Ksoup.parse(childHtml)
        val div = childDoc.select("div").first()
        val p = childDoc.select("p").first()
        val appendTo1 = div!!.appendTo(parent)
        assertEquals(div, appendTo1)
        val appendTo2 = p!!.appendTo(div)
        assertEquals(p, appendTo2)
        assertEquals(
            "<div class=\"a\"></div>\n<div class=\"b\">\n <p>Two</p>\n</div>",
            parentDoc.body().html(),
        )
        assertEquals("", childDoc.body().html()) // got moved out
    }

    @Test
    fun testNormalizesNbspInText() {
        val escaped = "You can't always get what you&nbsp;want."
        val withNbsp = "You can't always get what you want." // there is an nbsp char in there
        val doc = Ksoup.parse("<p>$escaped")
        val p = doc.select("p").first()
        assertEquals("You can't always get what you want.", p!!.text()) // text is normalized
        assertEquals("<p>$escaped</p>", p.outerHtml()) // html / whole text keeps &nbsp;
        assertEquals(withNbsp, p.textNodes()[0].getWholeText())
        assertEquals(160, withNbsp[29].code)
        val matched = doc.select("p:contains(get what you want)").first()
        assertEquals("p", matched!!.nodeName())
        assertTrue(matched.`is`(":containsOwn(get what you want)"))
    }

    @Test
    fun testNormalizesInvisiblesInText() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: ascii charset not supported
            return
        }
        val escaped = "This&shy;is&#x200b;one&shy;long&shy;word"
        val decoded =
            "This\u00ADis\u200Bone\u00ADlong\u00ADword" // browser would not display those soft hyphens / other chars, so we don't want them in the text
        val doc = Ksoup.parse("<p>$escaped")
        val p = doc.select("p").first()
        doc.outputSettings()
            .charset("ascii") // so that the outer html is easier to see with escaped invisibles
        assertEquals("Thisisonelongword", p!!.text()) // text is normalized
        assertEquals("<p>$escaped</p>", p.outerHtml()) // html / whole text keeps &shy etc;
        assertEquals(decoded, p.textNodes()[0].getWholeText())
        val matched = doc.select("p:contains(Thisisonelongword)")
            .first() // really just oneloneword, no invisibles
        assertEquals("p", matched!!.nodeName())
        assertTrue(matched.`is`(":containsOwn(Thisisonelongword)"))
    }

    @Test
    fun testRemoveBeforeIndex() {
        val doc = Ksoup.parse(
            "<html><body><div><p>before1</p><p>before2</p><p>XXX</p><p>after1</p><p>after2</p></div></body></html>",
            "",
        )
        val body = doc.select("body").first()
        val elems = body!!.select("p:matchesOwn(XXX)")
        val xElem = elems.first()
        val beforeX = xElem!!.parent()!!.getElementsByIndexLessThan(xElem.elementSiblingIndex())
        for (p in beforeX) {
            p.remove()
        }
        assertEquals(
            "<body><div><p>XXX</p><p>after1</p><p>after2</p></div></body>",
            TextUtil.stripNewlines(body.outerHtml()),
        )
    }

    @Test
    fun testRemoveAfterIndex() {
        val doc2 = Ksoup.parse(
            "<html><body><div><p>before1</p><p>before2</p><p>XXX</p><p>after1</p><p>after2</p></div></body></html>",
            "",
        )
        val body = doc2.select("body").first()
        val elems = body!!.select("p:matchesOwn(XXX)")
        val xElem = elems.first()
        val afterX = xElem!!.parent()!!.getElementsByIndexGreaterThan(xElem.elementSiblingIndex())
        for (p in afterX) {
            p.remove()
        }
        assertEquals(
            "<body><div><p>before1</p><p>before2</p><p>XXX</p></div></body>",
            TextUtil.stripNewlines(body.outerHtml()),
        )
    }

    @Test
    fun whiteSpaceClassElement() {
        val tag = Tag.valueOf("a")
        val attribs = Attributes()
        val el = Element(tag, "", attribs)
        attribs.put("class", "abc ")
        val hasClass = el.hasClass("ab")
        assertFalse(hasClass)
    }

    @Test
    fun testNextElementSiblingAfterClone() {
        // via https://github.com/jhy/jsoup/issues/951
        val html =
            "<!DOCTYPE html><html lang=\"en\"><head></head><body><div>Initial element</div></body></html>"
        val expectedText = "New element"
        val cloneExpect = "New element in clone"
        val original = Ksoup.parse(html)
        val clone = original.clone()
        val originalElement = original.body().child(0)
        originalElement.after("<div>$expectedText</div>")
        val originalNextElementSibling = originalElement.nextElementSibling()
        val originalNextSibling = originalElement.nextSibling() as Element?
        assertEquals(expectedText, originalNextElementSibling!!.text())
        assertEquals(expectedText, originalNextSibling!!.text())
        val cloneElement = clone.body().child(0)
        cloneElement.after("<div>$cloneExpect</div>")
        val cloneNextElementSibling = cloneElement.nextElementSibling()
        val cloneNextSibling = cloneElement.nextSibling() as Element?
        assertEquals(cloneExpect, cloneNextElementSibling!!.text())
        assertEquals(cloneExpect, cloneNextSibling!!.text())
    }

    @Test
    fun testRemovingEmptyClassAttributeWhenLastClassRemoved() {
        // https://github.com/jhy/jsoup/issues/947
        val doc = Ksoup.parse("<img class=\"one two\" />")
        val img = doc.select("img").first()
        img!!.removeClass("one")
        img.removeClass("two")
        assertFalse(doc.body().html().contains("class=\"\""))
    }

    @Test
    fun booleanAttributeOutput() {
        val doc = Ksoup.parse("<img src=foo noshade='' nohref async=async autofocus=false>")
        val img = doc.selectFirst("img")
        assertEquals(
            "<img src=\"foo\" noshade nohref async autofocus=\"false\">",
            img!!.outerHtml(),
        )
    }

    @Test
    fun textHasSpaceAfterBlockTags() {
        val doc = Ksoup.parse("<div>One</div>Two")
        assertEquals("One Two", doc.text())
    }

    @Test
    fun textHasSpaceBetweenDivAndCenterTags() {
        val doc =
            Ksoup.parse("<div>One</div><div>Two</div><center>Three</center><center>Four</center>")
        assertEquals("One Two Three Four", doc.text())
    }

    @Test
    fun testNextElementSiblings() {
        val doc = Ksoup.parse(
            "<ul id='ul'>" +
                "<li id='a'>a</li>" +
                "<li id='b'>b</li>" +
                "<li id='c'>c</li>" +
                "</ul> Not An Element but a node" +
                "<div id='div'>" +
                "<li id='d'>d</li>" +
                "</div>",
        )
        val element = doc.getElementById("a")
        val elementSiblings = element!!.nextElementSiblings()
        assertNotNull(elementSiblings)
        assertEquals(2, elementSiblings.size)
        assertEquals("b", elementSiblings[0].id())
        assertEquals("c", elementSiblings[1].id())
        val element1 = doc.getElementById("b")
        val elementSiblings1: List<Element> = element1!!.nextElementSiblings()
        assertNotNull(elementSiblings1)
        assertEquals(1, elementSiblings1.size)
        assertEquals("c", elementSiblings1[0].id())
        val element2 = doc.getElementById("c")
        val elementSiblings2: List<Element> = element2!!.nextElementSiblings()
        assertEquals(0, elementSiblings2.size)
        val ul = doc.getElementById("ul")
        val elementSiblings3: List<Element> = ul!!.nextElementSiblings()
        assertNotNull(elementSiblings3)
        assertEquals(1, elementSiblings3.size)
        assertEquals("div", elementSiblings3[0].id())
        val div = doc.getElementById("div")
        val elementSiblings4: List<Element> = div!!.nextElementSiblings()
        assertEquals(0, elementSiblings4.size)
    }

    @Test
    fun testPreviousElementSiblings() {
        val doc = Ksoup.parse(
            "<ul id='ul'>" +
                "<li id='a'>a</li>" +
                "<li id='b'>b</li>" +
                "<li id='c'>c</li>" +
                "</ul>" +
                "<div id='div'>" +
                "<li id='d'>d</li>" +
                "</div>",
        )
        val element = doc.getElementById("b")
        val elementSiblings = element!!.previousElementSiblings()
        assertNotNull(elementSiblings)
        assertEquals(1, elementSiblings.size)
        assertEquals("a", elementSiblings[0].id())
        val element1 = doc.getElementById("a")
        val elementSiblings1: List<Element> = element1!!.previousElementSiblings()
        assertEquals(0, elementSiblings1.size)
        val element2 = doc.getElementById("c")
        val elementSiblings2: List<Element> = element2!!.previousElementSiblings()
        assertNotNull(elementSiblings2)
        assertEquals(2, elementSiblings2.size)
        assertEquals("b", elementSiblings2[0].id())
        assertEquals("a", elementSiblings2[1].id())
        val ul = doc.getElementById("ul")
        val elementSiblings3: List<Element> = ul!!.previousElementSiblings()
        assertEquals(0, elementSiblings3.size)
    }

    @Test
    fun testClearAttributes() {
        val el = Element("a").attr("href", "http://example.com").text("Hello")
        assertEquals("<a href=\"http://example.com\">Hello</a>", el.outerHtml())
        val el2 = el.clearAttributes() // really just force testing the return type is Element
        assertSame(el, el2)
        assertEquals("<a>Hello</a>", el2.outerHtml())
    }

    @Test
    fun testRemoveAttr() {
        val el = Element("a")
            .attr("href", "http://example.com")
            .attr("id", "1")
            .text("Hello")
        assertEquals("<a href=\"http://example.com\" id=\"1\">Hello</a>", el.outerHtml())
        val el2 = el.removeAttr("href") // really just force testing the return type is Element
        assertSame(el, el2)
        assertEquals("<a id=\"1\">Hello</a>", el2.outerHtml())
    }

    @Test
    fun testRoot() {
        val el = Element("a")
        el.append("<span>Hello</span>")
        assertEquals("<a><span>Hello</span></a>", el.outerHtml())
        val span = el.selectFirst("span")
        assertNotNull(span)
        val el2 = span.root()
        assertSame(el, el2)
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three")
        val div = doc.selectFirst("div")
        assertSame(doc, div!!.root())
        assertSame(doc, div.ownerDocument())
    }

    @Test
    fun testTraverse() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three")
        val div = doc.selectFirst("div")
        assertNotNull(div)

        // TODO: use atomic integer
        var counter = 0
        val div2 = div.traverse(NodeVisitor { node, depth -> ++counter })
        assertEquals(7, counter)
        assertEquals(div2, div)
    }

    @Test
    fun testTraverseLambda() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three")
        val div = doc.selectFirst("div")
        assertNotNull(div)
        // TODO: use atomic integer
        var counter = 0
        val div2 = div.traverse(NodeVisitor { node, depth -> ++counter })
        assertEquals(7, counter)
        assertEquals(div2, div)
    }

    @Test
    fun testFilterCallReturnsElement() {
        // doesn't actually test the filter so much as the return type for Element. See node.nodeFilter for an actual test
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three")
        val div = doc.selectFirst("div")
        assertNotNull(div)
        val div2 = div.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                return NodeFilter.FilterResult.CONTINUE
            }

            override fun tail(node: Node?, depth: Int): NodeFilter.FilterResult {
                return NodeFilter.FilterResult.CONTINUE
            }
        })
        assertSame(div, div2)
    }

    @Test
    fun testFilterAsLambda() {
        val doc = Ksoup.parse("<div><p>One<p id=2>Two<p>Three")
        doc.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                return if (node.attr("id") == "2") NodeFilter.FilterResult.REMOVE else NodeFilter.FilterResult.CONTINUE
            }
        })
        assertEquals(
            "<div><p>One</p><p>Three</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testForEach() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There</div><div id=1>Gone<p></div>")
        doc.forEach { el: Element? ->
            if (el!!.id() == "1") {
                el.remove()
            } else if (el.text() == "There") {
                el.text("There Now")
                el.append("<p>Another</p>")
            }
        }
        assertEquals(
            "<div><p>Hello</p></div><div>There Now<p>Another</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun doesntDeleteZWJWhenNormalizingText() {
        val text = "\uD83D\uDC69\u200D\uD83D\uDCBB\uD83E\uDD26\uD83C\uDFFB\u200D\u2642\uFE0F"
        val doc = Ksoup.parse("<p>$text</p><div>One&zwj;Two</div>")
        val p = doc.selectFirst("p")
        val d = doc.selectFirst("div")
        assertEquals(12, p!!.text().length)
        assertEquals(text, p.text())
        assertEquals(7, d!!.text().length)
        assertEquals("One\u200DTwo", d.text())
        val found = doc.selectFirst("div:contains(One\u200DTwo)")
        assertTrue(found!!.hasSameValue(d))
    }

    @Test
    fun testReparentSeperateNodes() {
        val html = "<div><p>One<p>Two"
        val doc = Ksoup.parse(html)
        val new1 = Element("p").text("Three")
        val new2 = Element("p").text("Four")
        doc.body().insertChildren(-1, new1, new2)
        assertEquals(
            "<div><p>One</p><p>Two</p></div><p>Three</p><p>Four</p>",
            TextUtil.stripNewlines(doc.body().html()),
        )

        // note that these get moved from the above - as not copied
        doc.body().insertChildren(0, new1, new2)
        assertEquals(
            "<p>Three</p><p>Four</p><div><p>One</p><p>Two</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )
        doc.body().insertChildren(0, new2.clone(), new1.clone())
        assertEquals(
            "<p>Four</p><p>Three</p><p>Three</p><p>Four</p><div><p>One</p><p>Two</p></div>",
            TextUtil.stripNewlines(doc.body().html()),
        )

        // shifted to end
        doc.body().appendChild(new1)
        assertEquals(
            "<p>Four</p><p>Three</p><p>Four</p><div><p>One</p><p>Two</p></div><p>Three</p>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun testNotActuallyAReparent() {
        // prep
        val html = "<div>"
        val doc = Ksoup.parse(html)
        val div = doc.selectFirst("div")
        val new1 = Element("p").text("One")
        val new2 = Element("p").text("Two")
        div!!.addChildren(new1, new2)
        assertEquals(
            "<div><p>One</p><p>Two</p></div>",
            TextUtil.stripNewlines(div.outerHtml()),
        )

        // and the issue setup:
        val new3 = Element("p").text("Three")
        val wrap = Element("nav")
        wrap.addChildren(0, new1, new3)
        assertEquals(
            "<nav><p>One</p><p>Three</p></nav>",
            TextUtil.stripNewlines(wrap.outerHtml()),
        )
        div.addChildren(wrap)
        // now should be that One moved into wrap, leaving Two in div.
        assertEquals(
            "<div><p>Two</p><nav><p>One</p><p>Three</p></nav></div>",
            TextUtil.stripNewlines(div.outerHtml()),
        )
        assertEquals(
            "<div><p>Two</p><nav><p>One</p><p>Three</p></nav></div>",
            TextUtil.stripNewlines(div.outerHtml()),
        )
    }

    @Test
    fun testChildSizeWithMixedContent() {
        val doc =
            Ksoup.parse("<table><tbody>\n<tr>\n<td>15:00</td>\n<td>sport</td>\n</tr>\n</tbody></table>")
        val row = doc.selectFirst("table tbody tr")
        assertEquals(2, row!!.childrenSize())
        assertEquals(5, row.childNodeSize())
    }

    @Test
    fun isBlock() {
        val html = "<div><p><span>Hello</span>"
        val doc = Ksoup.parse(html)
        assertTrue(doc.selectFirst("div")!!.isBlock())
        assertTrue(doc.selectFirst("p")!!.isBlock())
        assertFalse(doc.selectFirst("span")!!.isBlock())
    }

    @Test
    fun testScriptTextHtmlSetAsData() {
        var src = "var foo = 5 < 2;\nvar bar = 1 && 2;"
        val html = "<script>$src</script>"
        val doc = Ksoup.parse(html)
        val el = doc.selectFirst("script")
        assertNotNull(el)
        validateScriptContents(src, el)
        src = "var foo = 4 < 2;\nvar bar > 1 && 2;"
        el.html(src)
        validateScriptContents(src, el)

        // special case for .text (in HTML; in XML will just be regular text)
        el.text(src)
        validateScriptContents(src, el)

        // XML, no special treatment, get escaped correctly
        val xml = Parser.xmlParser().parseInput(html, "")
        val xEl = xml.selectFirst("script")
        assertNotNull(xEl)
        src = "var foo = 5 < 2;\nvar bar = 1 && 2;"
        val escaped = "var foo = 5 &lt; 2;\nvar bar = 1 &amp;&amp; 2;"
        validateXmlScriptContents(xEl)
        xEl.text(src)
        validateXmlScriptContents(xEl)
        xEl.html(src)
        validateXmlScriptContents(xEl)
        assertEquals("<script>var foo = 4 < 2;\nvar bar > 1 && 2;</script>", el.outerHtml())
        assertEquals(
            "<script>$escaped</script>",
            xEl.outerHtml(),
        ) // escaped in xml as no special treatment
    }

    @Test
    fun testShallowCloneToString() {
        // https://github.com/jhy/jsoup/issues/1410
        val doc = Ksoup.parse("<p><i>Hello</i></p>")
        val p = doc.selectFirst("p")
        val i = doc.selectFirst("i")
        val pH = p!!.shallowClone().toString()
        val iH = i!!.shallowClone().toString()
        assertEquals("<p></p>", pH) // shallow, so no I
        assertEquals("<i></i>", iH)
        assertEquals(p.outerHtml(), p.toString())
        assertEquals(i.outerHtml(), i.toString())
    }

    @Test
    fun styleHtmlRoundTrips() {
        val styleContents = "foo < bar > qux {color:white;}"
        val html = "<head><style>$styleContents</style></head>"
        val doc = Ksoup.parse(html)
        val head = doc.head()
        val style = head.selectFirst("style")
        assertNotNull(style)
        assertEquals(styleContents, style.html())
        style.html(styleContents)
        assertEquals(styleContents, style.html())
        assertEquals("", style.text())
        style.text(styleContents) // pushes the HTML, not the Text
        assertEquals("", style.text())
        assertEquals(styleContents, style.html())
    }

    @Test
    fun moveChildren() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div><div></div>")
        val divs = doc.select("div")
        val a = divs[0]
        val b = divs[1]
        b.insertChildren(-1, a.childNodes())
        assertEquals(
            "<div></div>\n<div>\n <p>One</p>\n <p>Two</p>\n <p>Three</p>\n</div>",
            doc.body().html(),
        )
    }

    @Test
    fun moveChildrenToOuter() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div><div></div>")
        val divs = doc.select("div")
        val a = divs[0]
        val b = doc.body()
        b.insertChildren(-1, a.childNodes())
        assertEquals(
            "<div></div>\n<div></div>\n<p>One</p>\n<p>Two</p>\n<p>Three</p>",
            doc.body().html(),
        )
    }

    @Test
    fun appendChildren() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div><div><p>Four</div>")
        val divs = doc.select("div")
        val a = divs[0]
        val b = divs[1]
        b.appendChildren(a.childNodes())
        assertEquals(
            "<div></div>\n<div>\n <p>Four</p>\n <p>One</p>\n <p>Two</p>\n <p>Three</p>\n</div>",
            doc.body().html(),
        )
    }

    @Test
    fun prependChildren() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div><div><p>Four</div>")
        val divs = doc.select("div")
        val a = divs[0]
        val b = divs[1]
        b.prependChildren(a.childNodes())
        assertEquals(
            "<div></div>\n<div>\n <p>One</p>\n <p>Two</p>\n <p>Three</p>\n <p>Four</p>\n</div>",
            doc.body().html(),
        )
    }

    @Test
    fun loopMoveChildren() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div><div><p>Four</div>")
        val divs = doc.select("div")
        val a = divs[0]
        val b = divs[1]
        val outer = b.parent()
        assertNotNull(outer)
        for (node in a.childNodes()) {
            outer.appendChild(node)
        }
        assertEquals(
            "<div></div>\n<div>\n <p>Four</p>\n</div>\n<p>One</p>\n<p>Two</p>\n<p>Three</p>",
            doc.body().html(),
        )
    }

    @Test
    fun accessorsDoNotVivifyAttributes() {
        // internally, we don't want to create empty Attribute objects unless actually used for something
        val doc = Ksoup.parse("<div><p><a href=foo>One</a>")
        val div = doc.selectFirst("div")
        val p = doc.selectFirst("p")
        val a = doc.selectFirst("a")

        // should not create attributes
        assertEquals("", div!!.attr("href"))
        p!!.removeAttr("href")
        val hrefs = doc.select("[href]")
        assertEquals(1, hrefs.size)
        assertFalse(div.hasAttributes())
        assertFalse(p.hasAttributes())
        assertTrue(a!!.hasAttributes())
    }

    @Test
    fun childNodesAccessorDoesNotVivify() {
        val doc = Ksoup.parse("<p></p>")
        val p = doc.selectFirst("p")
        assertFalse(p!!.hasChildNodes())
        assertEquals(0, p.childNodeSize())
        assertEquals(0, p.childrenSize())
        val childNodes = p.childNodes()
        assertEquals(0, childNodes.size)
        val children = p.children()
        assertEquals(0, children.size)
        assertFalse(p.hasChildNodes())
    }

    @Test
    fun emptyChildrenElementsIsModifiable() {
        // using unmodifiable empty in childElementList as short circuit, but people may be modifying Elements.
        val p = Element("p")
        val els = p.children()
        assertEquals(0, els.size)
        els.add(Element("a"))
        assertEquals(1, els.size)
    }

    @Test
    fun attributeSizeDoesNotAutoVivify() {
        val doc = Ksoup.parse("<p></p>")
        val p = doc.selectFirst("p")
        assertNotNull(p)
        assertFalse(p.hasAttributes())
        assertEquals(0, p.attributesSize())
        assertFalse(p.hasAttributes())
        p.attr("foo", "bar")
        assertEquals(1, p.attributesSize())
        assertTrue(p.hasAttributes())
        p.removeAttr("foo")
        assertEquals(0, p.attributesSize())
    }

    @Test
    fun clonedElementsHaveOwnerDocsAndIndependentSettings() {
        // https://github.com/jhy/jsoup/issues/763
        val doc = Ksoup.parse("<div>Text</div><div>Two</div>")
        doc.outputSettings().prettyPrint(false)
        val div = doc.selectFirst("div")
        assertNotNull(div)
        val text = div.childNode(0)
        assertNotNull(text)
        val divClone = div.clone()
        val docClone = divClone.ownerDocument()
        assertNotNull(docClone)
        assertFalse(docClone.outputSettings().prettyPrint())
        assertNotSame(doc, docClone)
        assertSame(docClone, divClone.childNode(0).ownerDocument())
        // the cloned text has same owner doc as the cloned div
        doc.outputSettings().prettyPrint(true)
        assertTrue(doc.outputSettings().prettyPrint())
        assertFalse(docClone.outputSettings().prettyPrint())
        assertEquals(
            1,
            docClone.children().size,
        ) // check did not get the second div as the owner's children
        assertEquals(divClone, docClone.child(0)) // note not the head or the body -- not normalized
    }

    @Test
    fun prettySerializationRoundTrips() {
        parameterizedTest(testOutputSettings()) { settings ->
            // https://github.com/jhy/jsoup/issues/1688
            // tests that repeated html() and parse() does not accumulate errant spaces / newlines
            val doc =
                Ksoup.parse("<div>\nFoo\n<p>\nBar\nqux</p></div>\n<script>\n alert('Hello!');\n</script>")
            doc.outputSettings(settings)
            val html = doc.html()
            val doc2 = Ksoup.parse(html)
            doc2.outputSettings(settings)
            val html2 = doc2.html()
            assertEquals(html, html2)
        }
    }

    @Test
    fun prettyPrintScriptsDoesNotGrowOnRepeat() {
        val doc =
            Ksoup.parse("<div>\nFoo\n<p>\nBar\nqux</p></div>\n<script>\n alert('Hello!');\n</script>")
        val settings = doc.outputSettings()
        settings
            .prettyPrint(true)
            .outline(true)
            .indentAmount(4)
        val html = doc.html()
        val doc2 = Ksoup.parse(html)
        doc2.outputSettings(settings)
        val html2 = doc2.html()
        assertEquals(html, html2)
    }

    @Test
    fun elementBrText() {
        // testcase for https://github.com/jhy/jsoup/issues/1437
        val html = "<p>Hello<br>World</p>"
        val doc = Ksoup.parse(html)
        doc.outputSettings().prettyPrint(false) // otherwise html serializes as Hello<br>\n World.
        val p = doc.select("p").first()
        assertNotNull(p)
        assertEquals(html, p.outerHtml())
        assertEquals("Hello World", p.text())
        assertEquals("Hello\nWorld", p.wholeText())
    }

    @Test
    fun wrapTextAfterBr() {
        // https://github.com/jhy/jsoup/issues/1858
        val html = "<p>Hello<br>there<br>now.</p>"
        val doc = Ksoup.parse(html)
        assertEquals("<p>Hello<br>\n there<br>\n now.</p>", doc.body().html())
    }

    @Test
    fun prettyprintBrInBlock() {
        val html = "<div><br> </div>"
        val doc = Ksoup.parse(html)
        assertEquals("<div>\n <br>\n</div>", doc.body().html()) // not div\n br\n \n/div
    }

    @Test
    fun prettyprintBrWhenNotFirstChild() {
        // https://github.com/jhy/jsoup/issues/1911
        val h = "<div><p><br>Foo</p><br></div>"
        val doc = Ksoup.parse(h)
        assertEquals(
            """<div>
 <p><br>
  Foo</p>
 <br>
</div>""",
            doc.body().html(),
        )
        // br gets wrapped if in div, but not in p (block vs inline), but always wraps after
    }

    @Test
    fun preformatFlowsToChildTextNodes() {
        // https://github.com/jhy/jsoup/issues/1776
        val html =
            "<div><pre>One\n<span>\nTwo</span>\n <span>  \nThree</span>\n <span>Four <span>Five</span>\n  Six\n</pre>"
        val doc = Ksoup.parse(html)
        doc.outputSettings().indentAmount(2).prettyPrint(true)
        val div = doc.selectFirst("div")
        assertNotNull(div)
        val actual = div.outerHtml()
        val expect = """<div>
  <pre>One
<span>
Two</span>
 <span>  
Three</span>
 <span>Four <span>Five</span>
  Six
</span></pre>
</div>"""
        assertEquals(expect, actual)
        val expectText = """One

Two
   
Three
 Four Five
  Six
"""
        assertEquals(expectText, div.wholeText())
        val expectOwn = """One

 
 """
        assertEquals(expectOwn, div.child(0).wholeOwnText())
    }

    @Test
    fun inlineInBlockShouldIndent() {
        // was inconsistent between <div>\n<span> and <div><span> - former would print inline, latter would wrap(!)
        val html =
            "<div>One <span>Hello</span><span>!</span></div><div>\n<span>There</span></div><div> <span>Now</span></div>"
        val doc = Ksoup.parse(html)
        assertEquals(
            """<div>
 One <span>Hello</span><span>!</span>
</div>
<div>
 <span>There</span>
</div>
<div>
 <span>Now</span>
</div>""",
            doc.body().html(),
        )
    }

    @Test
    fun testExpectFirst() {
        val doc = Ksoup.parse("<p>One</p><p>Two <span>Three</span> <span>Four</span>")
        val span = doc.expectFirst("span")
        assertEquals("Three", span.text())
        assertNull(doc.selectFirst("div"))
        var threw = false
        try {
            val div = doc.expectFirst("div")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun testExpectFirstMessage() {
        val doc = Ksoup.parse("<p>One</p><p>Two <span>Three</span> <span>Four</span>")
        var threw = false
        val p = doc.expectFirst("P")
        try {
            val span = p.expectFirst("span.doesNotExist")
        } catch (e: ValidationException) {
            threw = true
            assertEquals(
                "No elements matched the query 'span.doesNotExist' on element 'p'.",
                e.message,
            )
        }
        assertTrue(threw)
    }

    @Test
    fun testExpectFirstMessageDoc() {
        val doc = Ksoup.parse("<p>One</p><p>Two <span>Three</span> <span>Four</span>")
        var threw = false
        val p = doc.expectFirst("P")
        try {
            val span = doc.expectFirst("span.doesNotExist")
        } catch (e: ValidationException) {
            threw = true
            assertEquals(
                "No elements matched the query 'span.doesNotExist' in the document.",
                e.message,
            )
        }
        assertTrue(threw)
    }

    @Test
    fun spanRunsMaintainSpace() {
        // https://github.com/jhy/jsoup/issues/1787
        val doc = Ksoup.parse("<p><span>One</span>\n<span>Two</span>\n<span>Three</span></p>")
        val text = "One Two Three"
        val body = doc.body()
        assertEquals(text, body.text())
        val p = doc.expectFirst("p")
        val html = p.html()
        p.html(html)
        assertEquals(text, body.text())
        assertEquals("<p><span>One</span> <span>Two</span> <span>Three</span></p>", body.html())
    }

    @Test
    fun doctypeIsPrettyPrinted() {
        // resolves underlying issue raised in https://github.com/jhy/jsoup/pull/1664
        val doc1 = Ksoup.parse("<!--\nlicense\n-->\n \n<!doctype html>\n<html>")
        val doc2 = Ksoup.parse("\n  <!doctype html><html>")
        val doc3 = Ksoup.parse("<!doctype html>\n<html>")
        val doc4 = Ksoup.parse("\n<!doctype html>\n<html>")
        val doc5 = Ksoup.parse("\n<!--\n comment \n -->  <!doctype html>\n<html>")
        val doc6 = Ksoup.parse("<!--\n comment \n -->  <!doctype html>\n<html>")
        assertEquals(
            "<!--\nlicense\n-->\n<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>",
            doc1.html(),
        )
        doc1.outputSettings().prettyPrint(false)
        assertEquals(
            "<!--\nlicense\n--><!doctype html>\n<html><head></head><body></body></html>",
            doc1.html(),
        )
        // note that the whitespace between the comment and the doctype is not retained, in Initial state
        assertEquals(
            "<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>",
            doc2.html(),
        )
        assertEquals(
            "<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>",
            doc3.html(),
        )
        assertEquals(
            "<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>",
            doc4.html(),
        )
        assertEquals(
            "<!--\n comment \n -->\n<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>",
            doc5.html(),
        )
        assertEquals(
            "<!--\n comment \n -->\n<!doctype html>\n<html>\n <head></head>\n <body></body>\n</html>",
            doc6.html(),
        )
    }

    @Test
    fun textnodeInBlockIndent() {
        val html = "<div>\n{{ msg }} \n </div>\n<div>\n{{ msg }} \n </div>"
        val doc = Ksoup.parse(html)
        assertEquals("<div>\n {{ msg }}\n</div>\n<div>\n {{ msg }}\n</div>", doc.body().html())
    }

    @Test
    fun stripTrailing() {
        val html = "<p> This <span>is </span>fine. </p>"
        val doc = Ksoup.parse(html)
        assertEquals("<p>This <span>is </span>fine.</p>", doc.body().html())
    }

    @Test
    fun elementIndentAndSpaceTrims() {
        val html = "<body><div> <p> One Two </p> <a>  Hello </a><p>\nSome text \n</p>\n </div>"
        val doc = Ksoup.parse(html)
        assertEquals(
            """<div>
 <p>One Two</p><a> Hello </a>
 <p>Some text</p>
</div>""",
            doc.body().html(),
        )
    }

    @Test
    fun divAInlineable() {
        val html = "<body><div> <a>Text</a>"
        val doc = Ksoup.parse(html)
        assertEquals(
            """<div>
 <a>Text</a>
</div>""",
            doc.body().html(),
        )
    }

    @Test
    fun noDanglingSpaceAfterCustomElement() {
        // https://github.com/jhy/jsoup/issues/1852
        var html = "<bar><p/>\n</bar>"
        var doc = Ksoup.parse(html)
        assertEquals("<bar>\n <p></p>\n</bar>", doc.body().html())
        html = "<foo>\n  <bar />\n</foo>"
        doc = Ksoup.parse(html)
        assertEquals("<foo>\n <bar />\n</foo>", doc.body().html())
    }

    @Test
    fun spanInBlockTrims() {
        val html = "<p>Lorem ipsum</p>\n<span>Thanks</span>"
        val doc = Ksoup.parse(html)
        val outHtml = doc.body().html()
        assertEquals("<p>Lorem ipsum</p><span>Thanks</span>", outHtml)
    }

    @Test
    fun replaceWithSelf() {
        // https://github.com/jhy/jsoup/issues/1843
        val doc = Ksoup.parse("<p>One<p>Two")
        val ps = doc.select("p")
        val first = ps.first()
        assertNotNull(first)
        first.replaceWith(first)
        assertEquals(ps[1], first.nextSibling())
        assertEquals("<p>One</p>\n<p>Two</p>", first.parent()!!.html())
    }

    @Test
    fun select() {
        val eval = QueryParser.parse("div")
        val doc = Ksoup.parse(reference)
        val els: Elements = doc.select("div")
        val els2: Elements = doc.select(eval)
        assertEquals(els, els2)
    }

    @Test
    fun insertChildrenValidation() {
        val doc = Ksoup.parse(reference)
        val div = doc.expectFirst("div")
        val ex: Throwable =
            assertFailsWith<ValidationException> { div.insertChildren(20, Element("div")) }
        assertEquals("Insert position out of bounds.", ex.message)
    }

    @Test
    fun cssSelectorNoDoc() {
        val el = Element("div")
        el.id("one")
        assertEquals("#one", el.cssSelector())
    }

    @Test
    fun cssSelectorNoParent() {
        val el = Element("div")
        assertEquals("div", el.cssSelector())
    }

    @Test
    fun cssSelectorDoesntStackOverflow() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: timeout error for js
            return
        }
        // https://github.com/jhy/jsoup/issues/2001
        var element = Element("element")
        val root = element

        // Create a long chain of elements
        for (i in 0..4999) {
            val elem2 = Element("element$i")
            element.appendChild(elem2)
            element = elem2
        }
        val selector = element.cssSelector() // would overflow in cssSelector parent() recurse
        val eval = QueryParser.parse(selector)
        assertEquals(eval.toString(), selector)
        assertTrue(selector.startsWith("element > element0 >"))
        assertTrue(selector.endsWith("8 > element4999"))
        val elements =
            root.select(selector) // would overflow in nested And ImmediateParent chain eval
        assertEquals(1, elements.size)
        assertEquals(element, elements.first())
    }

    @Test
    fun orphanSiblings() {
        val el = Element("div")
        assertEquals(0, el.siblingElements().size)
        assertEquals(0, el.nextElementSiblings().size)
        assertEquals(0, el.previousElementSiblings().size)
        assertNull(el.nextElementSibling())
        assertNull(el.previousElementSibling())
    }

    @Test
    fun getElementsByAttributeStarting() {
        val doc = Ksoup.parse("<div data-one=1 data-two=2 id=1><p data-one=3 id=2>Text</div><div>")
        val els = doc.getElementsByAttributeStarting(" data- ")
        assertEquals(2, els.size)
        assertEquals("1", els[0].id())
        assertEquals("2", els[1].id())
        assertEquals(0, doc.getElementsByAttributeStarting("not-data").size)
    }

    @Test
    fun getElementsByAttributeValueNot() {
        val doc =
            Ksoup.parse("<div data-one=1 data-two=2 id=1><p data-one=3 id=2>Text</div><div id=3>")
        val els = doc.body().getElementsByAttributeValueNot("data-one", "1")
        assertEquals(3, els.size) // the body, p, and last div
        assertEquals("body", els[0].normalName())
        assertEquals("2", els[1].id())
        assertEquals("3", els[2].id())
    }

    @Test
    fun getElementsByAttributeValueStarting() {
        val doc = Ksoup.parse("<a href=one1></a><a href=one2></a><a href=else</a>")
        val els = doc.getElementsByAttributeValueStarting("href", "one")
        assertEquals(2, els.size)
        assertEquals("one1", els[0].attr("href"))
        assertEquals("one2", els[1].attr("href"))
    }

    @Test
    fun getElementsByAttributeValueEnding() {
        val doc = Ksoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>")
        val els = doc.getElementsByAttributeValueEnding("href", "one")
        assertEquals(2, els.size)
        assertEquals("1one", els[0].attr("href"))
        assertEquals("2one", els[1].attr("href"))
    }

    @Test
    fun getElementsByAttributeValueContaining() {
        val doc = Ksoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>")
        val els = doc.getElementsByAttributeValueContaining("href", "on")
        assertEquals(2, els.size)
        assertEquals("1one", els[0].attr("href"))
        assertEquals("2one", els[1].attr("href"))
    }

    @Test
    fun getElementsByAttributeValueMatchingPattern() {
        val doc = Ksoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>")
        val els: Elements = doc.getElementsByAttributeValueMatching("href", "^\\d\\w+".toRegex())
        assertEquals(2, els.size)
        assertEquals("1one", els[0].attr("href"))
        assertEquals("2one", els[1].attr("href"))
    }

    @Test
    fun getElementsByAttributeValueMatching() {
        val doc = Ksoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>")
        val els = doc.getElementsByAttributeValueMatching("href", "^\\d\\w+")
        assertEquals(2, els.size)
        assertEquals("1one", els[0].attr("href"))
        assertEquals("2one", els[1].attr("href"))
    }

    @Test
    fun getElementsByAttributeValueMatchingValidation() {
        if (Platform.current == PlatformType.JS) {
//     always fail for js because js use double slash for escape character and it return different exception
            return
        }

        val doc = Ksoup.parse(reference)
        val ex: Throwable =
            assertFailsWith<IllegalArgumentException> {
                doc.getElementsByAttributeValueMatching(
                    "key",
                    "\\x",
                )
            }
        if (Platform.current == PlatformType.IOS) {
            assertEquals("Invalid hexadecimal escape sequence near index: 0\n\\x\n^", ex.message)
        } else {
            assertEquals("Illegal hexadecimal escape sequence near index 2\n\\x", ex.message)
        }
    }

    @Test
    fun getElementsByIndexEquals() {
        val doc = Ksoup.parse("<a href=1one></a><a href=2one></a><a href=else</a>")
        val els = doc.body().getElementsByIndexEquals(1)
        assertEquals(2, els.size)
        assertEquals("body", els[0].normalName())
        assertEquals("2one", els[1].attr("href"))
    }

    @Test
    fun getElementsContainingText() {
        val doc = Ksoup.parse("<div id=1>One</div><div>Two</div>")
        val els = doc.body().getElementsContainingText("one")
        assertEquals(2, els.size)
        assertEquals("body", els[0].normalName())
        assertEquals("1", els[1].id())
    }

    @Test
    fun getElementsContainingOwnText() {
        val doc = Ksoup.parse("<div id=1>One</div><div>Two</div>")
        val els = doc.body().getElementsContainingOwnText("one")
        assertEquals(1, els.size)
        assertEquals("1", els[0].id())
    }

    @Test
    fun getElementsMatchingTextValidation() {
        if (Platform.current == PlatformType.JS) {
//     always fail for js because js use double slash for escape character and it return different exception
            return
        }

        val doc = Ksoup.parse(reference)
        val ex: Throwable =
            assertFailsWith<IllegalArgumentException> { doc.getElementsMatchingText("\\x") }

        if (Platform.current == PlatformType.IOS) {
            assertEquals("Invalid hexadecimal escape sequence near index: 0\n\\x\n^", ex.message)
        } else {
            assertEquals("Illegal hexadecimal escape sequence near index 2\n\\x", ex.message)
        }
    }

    @Test
    fun getElementsMatchingText() {
        val doc = Ksoup.parse("<div id=1>One</div><div>Two</div>")
        val els = doc.body().getElementsMatchingText("O\\w+")
        assertEquals(2, els.size)
        assertEquals("body", els[0].normalName())
        assertEquals("1", els[1].id())
    }

    @Test
    fun getElementsMatchingOwnText() {
        val doc = Ksoup.parse("<div id=1>One</div><div>Two</div>")
        val els = doc.body().getElementsMatchingOwnText("O\\w+")
        assertEquals(1, els.size)
        assertEquals("1", els[0].id())
    }

    @Test
    fun getElementsMatchingOwnTextValidation() {
        if (Platform.current == PlatformType.JS) {
//     always fail for js because js use double slash for escape character and it return different exception
            return
        }

        val doc = Ksoup.parse(reference)
        val ex: Throwable =
            assertFailsWith<IllegalArgumentException> { doc.getElementsMatchingOwnText("\\x") }

        if (Platform.current == PlatformType.IOS) {
            assertEquals("Invalid hexadecimal escape sequence near index: 0\n\\x\n^", ex.message)
        } else {
            assertEquals("Illegal hexadecimal escape sequence near index 2\n\\x", ex.message)
        }
    }

    @Test
    fun hasText() {
        val doc =
            Ksoup.parse("<div id=1><p><i>One</i></p></div><div id=2>Two</div><div id=3><script>data</script> </div>")
        assertTrue(doc.getElementById("1")!!.hasText())
        assertTrue(doc.getElementById("2")!!.hasText())
        assertFalse(doc.getElementById("3")!!.hasText())
    }

    @Test
    fun dataInCdataNode() {
        val el = Element("div")
        val cdata = CDataNode("Some CData")
        el.appendChild(cdata)
        assertEquals("Some CData", el.data())
        val parse = Ksoup.parse("One <![CDATA[Hello]]>")
        assertEquals("Hello", parse.data())
    }

    @Test
    fun datanodesOutputCdataInXhtml() {
        val html = "<p><script>1 && 2</script><style>3 && 4</style> 5 &amp;&amp; 6</p>"
        val doc = Ksoup.parse(html) // parsed as HTML
        val out: String = TextUtil.normalizeSpaces(doc.body().html())
        assertEquals(html, out)
        val scriptEl = doc.expectFirst("script")
        val scriptDataNode = scriptEl.childNode(0) as DataNode
        assertEquals("1 && 2", scriptDataNode.getWholeData())
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
        val xml = doc.body().html()
        assertEquals(
            "<p><script><![CDATA[1 && 2]]></script><style><![CDATA[3 && 4]]></style> 5 &amp;&amp; 6</p>",
            TextUtil.normalizeSpaces(xml),
        )
        val xmlDoc = Ksoup.parse(xml, Parser.xmlParser())
        assertEquals(xml, xmlDoc.html())
        val scriptXmlEl = xmlDoc.expectFirst("script")
        val scriptCdata = scriptXmlEl.childNode(0) as CDataNode
        assertEquals(scriptCdata.text(), scriptDataNode.getWholeData())
    }

    //    StringBuffer adding \n in start but not when using StringBuilder
    @Test
    fun outerHtmlAppendable() {
        // tests not string builder flow
        val doc = Ksoup.parse("<div>One</div>")
        val buffer = StringBuilder()
        doc.body().outerHtml(buffer)
        assertEquals("<body>\n <div>\n  One\n </div>\n</body>", buffer.toString())
        val builder = StringBuilder()
        doc.body().outerHtml(builder)
        assertEquals("<body>\n <div>\n  One\n </div>\n</body>", builder.toString())
    }

    @Test
    fun rubyInline() {
        val html = "<ruby>T<rp>(</rp><rtc>!</rtc><rt>)</rt></ruby>"
        val doc = Ksoup.parse(html)
        assertEquals(html, doc.body().html())
    }

    @Test
    fun nestedFormatAsInlinePrintsAsBlock() {
        // https://github.com/jhy/jsoup/issues/1926
        val h = """        <table>
            <tr>
                <td>
                    <p style="display:inline;">A</p>
                    <p style="display:inline;">B</p>
                </td>
            </tr>
        </table>"""
        val doc = Ksoup.parse(h)
        val out = doc.body().html()
        assertEquals(
            """<table>
 <tbody>
  <tr>
   <td>
    <p style="display:inline;">A</p>
    <p style="display:inline;">B</p></td>
  </tr>
 </tbody>
</table>""",
            out,
        )
        // todo - I would prefer the </td> to wrap down there - but need to reimplement pretty printer to simplify and track indented state
    }

    @Test
    fun emptyDetachesChildren() {
        val html = "<div><p>One<p>Two</p>Three</div>"
        val doc = Ksoup.parse(html)
        val div = doc.expectFirst("div")
        assertEquals(3, div.childNodeSize())
        val childNodes = div.childNodes()
        div.empty()
        assertEquals(0, div.childNodeSize())
        assertEquals(3, childNodes.size) // copied before removing
        for (childNode in childNodes) {
            assertNull(childNode._parentNode)
        }
        val p = childNodes[0] as Element
        assertEquals(
            p,
            p.childNode(0).parentNode(),
        ) // TextNode "One" still has parent p, as detachment is only on div element
    }

    @Test
    fun emptyAndAddPreviousChild() {
        val html = "<div><p>One<p>Two<p>Three</div>"
        val doc = Ksoup.parse(html)
        val div = doc.expectFirst("div")
        val p = div.expectFirst("p")
        div
            .empty()
            .appendChild(p)
        assertEquals("<p>One</p>", div.html())
    }

    @Test
    fun emptyAndAddPreviousDescendant() {
        val html = "<header><div><p>One<p>Two<p>Three</div></header>"
        val doc = Ksoup.parse(html)
        val header = doc.expectFirst("header")
        val p = header.expectFirst("p")
        header
            .empty()
            .appendChild(p)
        assertEquals("<p>One</p>", header.html())
    }

    @Test
    fun xmlSyntaxSetsEscapeMode() {
        if (Platform.current == PlatformType.JS) {
            // FIXME: ascii charset not supported
            return
        }
        val html = "Foo&nbsp;&Succeeds;"
        val doc = Ksoup.parse(html)
        doc.outputSettings().charset("ascii") // so we can see the zws
        assertEquals("Foo&nbsp;&#x227b;", doc.body().html())
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
        val out = doc.body().html()
        assertEquals("Foo&#xa0;&#x227b;", out)

        // can set back if desired
        doc.outputSettings().escapeMode(Entities.EscapeMode.extended)
        assertEquals(
            "Foo&nbsp;&succ;",
            doc.body().html(),
        ) // succ is alias for Succeeds, and first hit in entities
    }

    companion object {
        private fun validateScriptContents(src: String, el: Element?) {
            assertEquals("", el!!.text()) // it's not text
            assertEquals("", el.ownText())
            assertEquals("", el.wholeText())
            assertEquals(src, el.html())
            assertEquals(src, el.data())
        }

        private fun validateXmlScriptContents(el: Element?) {
            assertEquals("var foo = 5 < 2; var bar = 1 && 2;", el!!.text())
            assertEquals("var foo = 5 < 2; var bar = 1 && 2;", el.ownText())
            assertEquals("var foo = 5 < 2;\nvar bar = 1 && 2;", el.wholeText())
            assertEquals("var foo = 5 &lt; 2;\nvar bar = 1 &amp;&amp; 2;", el.html())
            assertEquals("", el.data())
        }

        private fun testOutputSettings(): List<Document.OutputSettings> {
            return listOf(
                Document.OutputSettings().prettyPrint(true).indentAmount(4),
                Document.OutputSettings().prettyPrint(true).indentAmount(1),
                Document.OutputSettings().prettyPrint(true).indentAmount(4).outline(true),
                Document.OutputSettings().prettyPrint(false),
            )
        }
    }
}
