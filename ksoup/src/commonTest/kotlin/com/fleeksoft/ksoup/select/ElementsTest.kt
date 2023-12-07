package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TextUtil
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Node
import kotlin.test.*
import kotlin.test.Test

/**
 * Tests for ElementList.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class ElementsTest {
    @Test
    fun filter() {
        val h =
            "<p>Excl</p><div class=headline><p>Hello</p><p>There</p></div><div class=headline><h1>Headline</h1></div>"
        val doc = Ksoup.parse(h)
        val els = doc.select(".headline").select("p")
        assertEquals(2, els.size)
        assertEquals("Hello", els[0].text())
        assertEquals("There", els[1].text())
    }

    @Test
    fun attributes() {
        val h = "<p title=foo><p title=bar><p class=foo><p class=bar>"
        val doc = Ksoup.parse(h)
        val withTitle = doc.select("p[title]")
        assertEquals(2, withTitle.size)
        assertTrue(withTitle.hasAttr("title"))
        assertFalse(withTitle.hasAttr("class"))
        assertEquals("foo", withTitle.attr("title"))
        withTitle.removeAttr("title")
        assertEquals(2, withTitle.size) // existing Elements are not reevaluated
        assertEquals(0, doc.select("p[title]").size)
        val ps = doc.select("p").attr("style", "classy")
        assertEquals(4, ps.size)
        assertEquals("classy", ps.last()!!.attr("style"))
        assertEquals("bar", ps.last()!!.attr("class"))
    }

    @Test
    fun hasAttr() {
        val doc = Ksoup.parse("<p title=foo><p title=bar><p class=foo><p class=bar>")
        val ps = doc.select("p")
        assertTrue(ps.hasAttr("class"))
        assertFalse(ps.hasAttr("style"))
    }

    @Test
    fun hasAbsAttr() {
        val doc = Ksoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://jsoup.org'>Two</a>")
        val one = doc.select("#1")
        val two = doc.select("#2")
        val both = doc.select("a")
        assertFalse(one.hasAttr("abs:href"))
        assertTrue(two.hasAttr("abs:href"))
        assertTrue(both.hasAttr("abs:href")) // hits on #2
    }

    @Test
    fun attr() {
        val doc = Ksoup.parse("<p title=foo><p title=bar><p class=foo><p class=bar>")
        val classVal = doc.select("p").attr("class")
        assertEquals("foo", classVal)
    }

    @Test
    fun absAttr() {
        val doc = Ksoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://jsoup.org'>Two</a>")
        val one = doc.select("#1")
        val two = doc.select("#2")
        val both = doc.select("a")
        assertEquals("", one.attr("abs:href"))
        assertEquals("https://jsoup.org", two.attr("abs:href"))
        assertEquals("https://jsoup.org", both.attr("abs:href"))
    }

    @Test
    fun classes() {
        val doc = Ksoup.parse("<div><p class='mellow yellow'></p><p class='red green'></p>")
        val els = doc.select("p")
        assertTrue(els.hasClass("red"))
        assertFalse(els.hasClass("blue"))
        els.addClass("blue")
        els.removeClass("yellow")
        els.toggleClass("mellow")
        assertEquals("blue", els[0].className())
        assertEquals("red green blue mellow", els[1].className())
    }

    @Test
    fun hasClassCaseInsensitive() {
        val els = Ksoup.parse("<p Class=One>One <p class=Two>Two <p CLASS=THREE>THREE").select("p")
        val one = els[0]
        val two = els[1]
        val thr = els[2]
        assertTrue(one.hasClass("One"))
        assertTrue(one.hasClass("ONE"))
        assertTrue(two.hasClass("TWO"))
        assertTrue(two.hasClass("Two"))
        assertTrue(thr.hasClass("ThreE"))
        assertTrue(thr.hasClass("three"))
    }

    @Test
    fun text() {
        val h = "<div><p>Hello<p>there<p>world</div>"
        val doc = Ksoup.parse(h)
        assertEquals("Hello there world", doc.select("div > *").text())
    }

    @Test
    fun hasText() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div><p></p></div>")
        val divs = doc.select("div")
        assertTrue(divs.hasText())
        assertFalse(doc.select("div + div").hasText())
    }

    @Test
    fun html() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div><p>There</p></div>")
        val divs = doc.select("div")
        assertEquals("<p>Hello</p>\n<p>There</p>", divs.html())
    }

    @Test
    fun outerHtml() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div><p>There</p></div>")
        val divs = doc.select("div")
        assertEquals("<div><p>Hello</p></div><div><p>There</p></div>", TextUtil.stripNewlines(divs.outerHtml()))
    }

    @Test
    fun setHtml() {
        val doc = Ksoup.parse("<p>One</p><p>Two</p><p>Three</p>")
        val ps = doc.select("p")
        ps.prepend("<b>Bold</b>").append("<i>Ital</i>")
        assertEquals("<p><b>Bold</b>Two<i>Ital</i></p>", TextUtil.stripNewlines(ps[1].outerHtml()))
        ps.html("<span>Gone</span>")
        assertEquals("<p><span>Gone</span></p>", TextUtil.stripNewlines(ps[1].outerHtml()))
    }

    @Test
    fun value() {
        val doc = Ksoup.parse("<input value='one' /><textarea>two</textarea>")
        val els = doc.select("input, textarea")
        assertEquals(2, els.size)
        assertEquals("one", els.value())
        assertEquals("two", els.last()?.value())
        els.value("three")
        assertEquals("three", els.first()?.value())
        assertEquals("three", els.last()?.value())
        assertEquals("<textarea>three</textarea>", els.last()!!.outerHtml())
    }

    @Test
    fun before() {
        val doc = Ksoup.parse("<p>This <a>is</a> <a>jsoup</a>.</p>")
        doc.select("a").before("<span>foo</span>")
        assertEquals(
            "<p>This <span>foo</span><a>is</a> <span>foo</span><a>jsoup</a>.</p>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun after() {
        val doc = Ksoup.parse("<p>This <a>is</a> <a>jsoup</a>.</p>")
        doc.select("a").after("<span>foo</span>")
        assertEquals(
            "<p>This <a>is</a><span>foo</span> <a>jsoup</a><span>foo</span>.</p>",
            TextUtil.stripNewlines(doc.body().html()),
        )
    }

    @Test
    fun wrap() {
        val h = "<p><b>This</b> is <b>jsoup</b></p>"
        val doc = Ksoup.parse(h)
        doc.select("b").wrap("<i></i>")
        assertEquals("<p><i><b>This</b></i> is <i><b>jsoup</b></i></p>", doc.body().html())
    }

    @Test
    fun wrapDiv() {
        val h = "<p><b>This</b> is <b>jsoup</b>.</p> <p>How do you like it?</p>"
        val doc = Ksoup.parse(h)
        doc.select("p").wrap("<div></div>")
        assertEquals(
            "<div>\n <p><b>This</b> is <b>jsoup</b>.</p>\n</div>\n<div>\n <p>How do you like it?</p>\n</div>",
            doc.body().html(),
        )
    }

    @Test
    fun unwrap() {
        val h = "<div><font>One</font> <font><a href=\"/\">Two</a></font></div"
        val doc = Ksoup.parse(h)
        doc.select("font").unwrap()
        assertEquals(
            """<div>
 One <a href="/">Two</a>
</div>""",
            doc.body().html(),
        )
    }

    @Test
    fun unwrapP() {
        val h = "<p><a>One</a> Two</p> Three <i>Four</i> <p>Fix <i>Six</i></p>"
        val doc = Ksoup.parse(h)
        doc.select("p").unwrap()
        assertEquals("<a>One</a> Two Three <i>Four</i> Fix <i>Six</i>", TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun unwrapKeepsSpace() {
        val h = "<p>One <span>two</span> <span>three</span> four</p>"
        val doc = Ksoup.parse(h)
        doc.select("span").unwrap()
        assertEquals("<p>One two three four</p>", doc.body().html())
    }

    @Test
    fun empty() {
        val doc = Ksoup.parse("<div><p>Hello <b>there</b></p> <p>now!</p></div>")
        doc.outputSettings().prettyPrint(false)
        doc.select("p").empty()
        assertEquals("<div><p></p> <p></p></div>", doc.body().html())
    }

    @Test
    fun remove() {
        val doc = Ksoup.parse("<div><p>Hello <b>there</b></p> jsoup <p>now!</p></div>")
        doc.outputSettings().prettyPrint(false)
        doc.select("p").remove()
        assertEquals("<div> jsoup </div>", doc.body().html())
    }

    @Test
    fun eq() {
        val h = "<p>Hello<p>there<p>world"
        val doc = Ksoup.parse(h)
        assertEquals("there", doc.select("p").eq(1).text())
        assertEquals("there", doc.select("p")[1].text())
    }

    @Test
    fun `is`() {
        val h = "<p>Hello<p title=foo>there<p>world"
        val doc = Ksoup.parse(h)
        val ps = doc.select("p")
        assertTrue(ps.`is`("[title=foo]"))
        assertFalse(ps.`is`("[title=bar]"))
    }

    @Test
    fun parents() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><p>There</p>")
        val parents = doc.select("p").parents()
        assertEquals(3, parents.size)
        assertEquals("div", parents[0].tagName())
        assertEquals("body", parents[1].tagName())
        assertEquals("html", parents[2].tagName())
    }

    @Test
    operator fun not() {
        val doc = Ksoup.parse("<div id=1><p>One</p></div> <div id=2><p><span>Two</span></p></div>")
        val div1 = doc.select("div").not(":has(p > span)")
        assertEquals(1, div1.size)
        assertEquals("1", div1.first()!!.id())
        val div2 = doc.select("div").not("#1")
        assertEquals(1, div2.size)
        assertEquals("2", div2.first()!!.id())
    }

    @Test
    fun tagNameSet() {
        val doc = Ksoup.parse("<p>Hello <i>there</i> <i>now</i></p>")
        doc.select("i").tagName("em")
        assertEquals("<p>Hello <em>there</em> <em>now</em></p>", doc.body().html())
    }

    @Test
    fun traverse() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        doc.select("div").traverse(
            object : NodeVisitor {
                override fun head(
                    node: Node,
                    depth: Int,
                ) {
                    accum.append("<").append(node.nodeName()).append(">")
                }

                override fun tail(
                    node: Node,
                    depth: Int,
                ) {
                    accum.append("</").append(node.nodeName()).append(">")
                }
            },
        )
        assertEquals("<div><p><#text></#text></p></div><div><#text></#text></div>", accum.toString())
    }

    @Test
    fun forms() {
        val doc = Ksoup.parse("<form id=1><input name=q></form><div /><form id=2><input name=f></form>")
        val els = doc.select("form, div")
        assertEquals(3, els.size)
        val forms = els.forms()
        assertEquals(2, forms.size)
        assertNotNull(forms[0])
        assertNotNull(forms[1])
        assertEquals("1", forms[0].id())
        assertEquals("2", forms[1].id())
    }

    @Test
    fun comments() {
        val doc = Ksoup.parse("<!-- comment1 --><p><!-- comment2 --><p class=two><!-- comment3 -->")
        val comments = doc.select("p").comments()
        assertEquals(2, comments.size)
        assertEquals(" comment2 ", comments[0].getData())
        assertEquals(" comment3 ", comments[1].getData())
        val comments1 = doc.select("p.two").comments()
        assertEquals(1, comments1.size)
        assertEquals(" comment3 ", comments1[0].getData())
    }

    @Test
    fun textNodes() {
        val doc = Ksoup.parse("One<p>Two<a>Three</a><p>Four</p>Five")
        val textNodes = doc.select("p").textNodes()
        assertEquals(2, textNodes.size)
        assertEquals("Two", textNodes[0].text())
        assertEquals("Four", textNodes[1].text())
    }

    @Test
    fun dataNodes() {
        var doc = Ksoup.parse("<p>One</p><script>Two</script><style>Three</style>")
        val dataNodes = doc.select("p, script, style").dataNodes()
        assertEquals(2, dataNodes.size)
        assertEquals("Two", dataNodes[0].getWholeData())
        assertEquals("Three", dataNodes[1].getWholeData())
        doc = Ksoup.parse("<head><script type=application/json><crux></script><script src=foo>Blah</script>")
        val script = doc.select("script[type=application/json]")
        val scriptNode = script.dataNodes()
        assertEquals(1, scriptNode.size)
        val dataNode = scriptNode[0]
        assertEquals("<crux>", dataNode.getWholeData())

        // check if they're live
        dataNode.setWholeData("<cromulent>")
        assertEquals("<script type=\"application/json\"><cromulent></script>", script.outerHtml())
    }

    @Test
    fun nodesEmpty() {
        val doc = Ksoup.parse("<p>")
        assertEquals(0, doc.select("form").textNodes().size)
    }

    @Test
    fun classWithHyphen() {
        val doc = Ksoup.parse("<p class='tab-nav'>Check</p>")
        val els = doc.getElementsByClass("tab-nav")
        assertEquals(1, els.size)
        assertEquals("Check", els.text())
    }

    @Test
    fun siblings() {
        val doc = Ksoup.parse("<div><p>1<p>2<p>3<p>4<p>5<p>6</div><div><p>7<p>8<p>9<p>10<p>11<p>12</div>")
        val els = doc.select("p:eq(3)") // gets p4 and p10
        assertEquals(2, els.size)
        val next = els.next()
        assertEquals(2, next.size)
        assertEquals("5", next.first()!!.text())
        assertEquals("11", next.last()!!.text())
        assertEquals(0, els.next("p:contains(6)").size)
        val nextF = els.next("p:contains(5)")
        assertEquals(1, nextF.size)
        assertEquals("5", nextF.first()!!.text())
        val nextA = els.nextAll()
        assertEquals(4, nextA.size)
        assertEquals("5", nextA.first()!!.text())
        assertEquals("12", nextA.last()!!.text())
        val nextAF = els.nextAll("p:contains(6)")
        assertEquals(1, nextAF.size)
        assertEquals("6", nextAF.first()!!.text())
        val prev = els.prev()
        assertEquals(2, prev.size)
        assertEquals("3", prev.first()!!.text())
        assertEquals("9", prev.last()!!.text())
        assertEquals(0, els.prev("p:contains(1)").size)
        val prevF = els.prev("p:contains(3)")
        assertEquals(1, prevF.size)
        assertEquals("3", prevF.first()!!.text())
        val prevA = els.prevAll()
        assertEquals(6, prevA.size)
        assertEquals("3", prevA.first()!!.text())
        assertEquals("7", prevA.last()!!.text())
        val prevAF = els.prevAll("p:contains(1)")
        assertEquals(1, prevAF.size)
        assertEquals("1", prevAF.first()!!.text())
    }

    @Test
    fun eachText() {
        val doc = Ksoup.parse("<div><p>1<p>2<p>3<p>4<p>5<p>6</div><div><p>7<p>8<p>9<p>10<p>11<p>12<p></p></div>")
        val divText = doc.select("div").eachText()
        assertEquals(2, divText.size)
        assertEquals("1 2 3 4 5 6", divText[0])
        assertEquals("7 8 9 10 11 12", divText[1])
        val pText = doc.select("p").eachText()
        val ps = doc.select("p")
        assertEquals(13, ps.size)
        assertEquals(12, pText.size) // not 13, as last doesn't have text
        assertEquals("1", pText[0])
        assertEquals("2", pText[1])
        assertEquals("5", pText[4])
        assertEquals("7", pText[6])
        assertEquals("12", pText[11])
    }

    @Test
    fun eachAttr() {
        val doc =
            Ksoup.parse(
                "<div><a href='/foo'>1</a><a href='http://example.com/bar'>2</a><a href=''>3</a><a>4</a>",
                "http://example.com",
            )
        val hrefAttrs = doc.select("a").eachAttr("href")
        assertEquals(3, hrefAttrs.size)
        assertEquals("/foo", hrefAttrs[0])
        assertEquals("http://example.com/bar", hrefAttrs[1])
        assertEquals("", hrefAttrs[2])
        assertEquals(4, doc.select("a").size)
        val absAttrs = doc.select("a").eachAttr("abs:href")
        assertEquals(3, absAttrs.size)
        assertEquals(3, absAttrs.size)
        assertEquals("http://example.com/foo", absAttrs[0])
        assertEquals("http://example.com/bar", absAttrs[1])
        assertEquals("http://example.com", absAttrs[2])
    }

    @Test
    fun setElementByIndex() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three")
        val newP = doc.createElement("p").text("New").attr("id", "new")
        val ps = doc.select("p")
        val two = ps[1]
        val old = ps.set(1, newP)
        assertSame(old, two)
        assertSame(newP, ps[1]) // replaced in list
        assertEquals("<p>One</p>\n<p id=\"new\">New</p>\n<p>Three</p>", doc.body().html()) // replaced in dom
    }

    @Test
    fun removeElementByIndex() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three")
        val ps = doc.select("p")
        val two = ps[1]
        assertTrue(ps.contains(two))
        val old = ps.removeAt(1)
        assertSame(old, two)
        assertEquals(2, ps.size) // removed from list
        assertFalse(ps.contains(old))
        assertEquals("<p>One</p>\n<p>Three</p>", doc.body().html()) // removed from dom
    }

    @Test
    fun removeElementByObject() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three")
        val ps = doc.select("p")
        val two = ps[1]
        assertTrue(ps.contains(two))
        val removed = ps.remove(two)
        assertTrue(removed)
        assertEquals(2, ps.size) // removed from list
        assertFalse(ps.contains(two))
        assertEquals("<p>One</p>\n<p>Three</p>", doc.body().html()) // removed from dom
    }

    @Test
    fun removeElementObjectNoops() {
        val doc: Document = Ksoup.parse("<p>One<p>Two<p>Three")
        val origHtml = doc.html()
        val newP = doc.createElement("p").text("New")
        val ps: Elements = doc.select("p")
        val size = ps.size
        assertFalse(ps.remove(newP))
//        assertFalse(ps.removeAll(newP.childNodes())) // already handled by ktlint
        assertEquals(origHtml, doc.html())
        assertEquals(size, ps.size)
    }

    @Test
    fun clear() {
        val doc = Ksoup.parse("<p>One</p><p>Two</p><div>Three</div>")
        val ps = doc.select("p")
        assertEquals(2, ps.size)
        ps.clear()
        assertEquals(0, ps.size)
        assertEquals(0, doc.select("p").size)
    }

    @Test
    fun removeAll() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three<p>Four</p><div>Div")
        val ps = doc.select("p")
        assertEquals(4, ps.size)
        val midPs = doc.select("p:gt(0):lt(3)") // Two and Three
        assertEquals(2, midPs.size)
        val removed = ps.removeAll(midPs)
        assertEquals(2, ps.size)
        assertTrue(removed)
        assertEquals(2, midPs.size)
        val divs = doc.select("div")
        assertEquals(1, divs.size)
        assertFalse(ps.removeAll(divs))
        assertEquals(2, ps.size)
        assertEquals("<p>One</p>\n<p>Four</p>\n<div>\n Div\n</div>", doc.body().html())
    }

    @Test
    fun retainAll() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three<p>Four</p><div>Div")
        val ps = doc.select("p")
//        val ps2 = Elements(ps)
//        ps2.removeAt(1)
        assertEquals(4, ps.size)
        val midPs = doc.select("p:gt(0):lt(3)") // Two and Three
        assertEquals(2, midPs.size)
        val removed = ps.retainAll(midPs)
        assertEquals(2, ps.size)
        assertTrue(removed)
        assertEquals(2, midPs.size)
        assertEquals("<p>Two</p>\n<p>Three</p>\n<div>\n Div\n</div>", doc.body().html())
        val psAgain = doc.select("p")
        assertFalse(midPs.retainAll(psAgain))
        assertEquals("<p>Two</p>\n<p>Three</p>\n<div>\n Div\n</div>", doc.body().html())
    }

    @Test
    fun iteratorRemovesFromDom() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three<p>Four")
        val ps: Elements = doc.select("p")
        assertEquals(4, ps.size)
        val it = ps.iterator()
        while (it.hasNext()) {
            val el = it.next()
            if (el.text().contains("Two")) it.remove()
        }
        assertEquals(3, ps.size)
        assertEquals("<p>One</p>\n<p>Three</p>\n<p>Four</p>", doc.body().html())
    }

    @Test
    fun removeIf() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three<p>Four")
        val ps = doc.select("p")
        assertEquals(4, ps.size)
        val removed = ps.removeIf { el -> el.text().contains("Two") }
        assertTrue(removed)
        assertEquals(3, ps.size)
        assertEquals("<p>One</p>\n<p>Three</p>\n<p>Four</p>", doc.body().html())
        assertFalse(ps.removeIf { el -> el.text().contains("Five") })
        assertEquals("<p>One</p>\n<p>Three</p>\n<p>Four</p>", doc.body().html())
    }

    @Test
    fun removeIfSupportsConcurrentRead() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three<p>Four")
        val ps = doc.select("p")
        assertEquals(4, ps.size)
        val removed = ps.removeIf { el -> ps.contains(el) }
        assertTrue(removed)
        assertEquals(0, ps.size)
        assertEquals("", doc.body().html())
    }

    @Test
    fun replaceAll() {
        val doc = Ksoup.parse("<p>One<p>Two<p>Three<p>Four")
        val ps = doc.select("p")
        assertEquals(4, ps.size)
        ps.replaceAll { el ->
            val div = doc.createElement("div")
            div.text(el.text())
            div
        }

        // Check Elements
        for (p in ps) {
            assertEquals("div", p.tagName())
        }

        // check dom
        assertEquals(
            "<div> One</div><div> Two</div><div> Three</div><div> Four</div>",
            TextUtil.normalizeSpaces(doc.body().html()),
        )
    }
}
