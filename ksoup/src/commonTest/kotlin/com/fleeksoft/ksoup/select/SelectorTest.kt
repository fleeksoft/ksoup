package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.Parser
import de.cketti.codepoints.deluxe.toCodePoint
import kotlin.test.*
import kotlin.test.Test

/**
 * Tests that the selector selects correctly.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class SelectorTest {
    @Test
    fun testByTag() {
        // should be case-insensitive
        val els = Ksoup.parse("<div id=1><div id=2><p>Hello</p></div></div><DIV id=3>").select("DIV")
        assertSelectedIds(els, "1", "2", "3")
        val none = Ksoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>").select("span")
        assertTrue(none.isEmpty())
    }

    @Test
    fun byEscapedTag() {
        // tested same result as js document.querySelector
        val doc = Ksoup.parse("<p.p>One</p.p> <p\\p>Two</p\\p>")
        val one = doc.expectFirst("p\\.p")
        assertEquals("One", one.text())
        val two = doc.expectFirst("p\\\\p")
        assertEquals("Two", two.text())
    }

    @Test
    fun testById() {
        val els = Ksoup.parse("<div><p id=foo>Hello</p><p id=foo>Foo two!</p></div>").select("#foo")
        assertSelectedOwnText(els, "Hello", "Foo two!")
        val none = Ksoup.parse("<div id=1></div>").select("#foo")
        assertTrue(none.isEmpty())
    }

    @Test
    fun byEscapedId() {
        val doc = Ksoup.parse("<p id='i.d'>One</p> <p id='i\\d'>Two</p> <p id='one-two/three'>Three</p>")
        val one = doc.expectFirst("#i\\.d")
        assertEquals("One", one.text())
        val two = doc.expectFirst("#i\\\\d")
        assertEquals("Two", two.text())
        val thr = doc.expectFirst("p#one-two\\/three")
        assertEquals("Three", thr.text())
    }

    @Test
    fun testByClass() {
        val els = Ksoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select("P.One")
        assertSelectedIds(els, "0", "1")
        val none = Ksoup.parse("<div class='one'></div>").select(".foo")
        assertTrue(none.isEmpty())
        val els2 = Ksoup.parse("<div class='One-Two' id=1></div>").select(".one-two")
        assertSelectedIds(els2, "1")
    }

    @Test
    fun byEscapedClass() {
        val doc = Ksoup.parse("<p class='one.two#three'>One</p>")
        assertSelectedOwnText(doc.select("p.one\\.two\\#three"), "One")
    }

    @Test
    fun testByClassCaseInsensitive() {
        val html = "<p Class=foo>One <p Class=Foo>Two <p class=FOO>Three <p class=farp>Four"
        val elsFromClass = Ksoup.parse(html).select("P.Foo")
        val elsFromAttr = Ksoup.parse(html).select("p[class=foo]")
        assertEquals(elsFromAttr.size, elsFromClass.size)
        assertSelectedOwnText(elsFromClass, "One", "Two", "Three")
    }

    @Test
    fun testByAttribute() {
        // TODO: mutlilocale test may move to jvm
//        Locale.setDefault(locale)
        val h =
            "<div Title=Foo /><div Title=Bar /><div Style=Qux /><div title=Balim /><div title=SLIM />" +
                "<div data-name='with spaces'/>"
        val doc = Ksoup.parse(h)
        val withTitle = doc.select("[title]")
        assertEquals(4, withTitle.size)
        val foo = doc.select("[TITLE=foo]")
        assertEquals(1, foo.size)
        val foo2 = doc.select("[title=\"foo\"]")
        assertEquals(1, foo2.size)
        val foo3 = doc.select("[title=\"Foo\"]")
        assertEquals(1, foo3.size)
        val dataName = doc.select("[data-name=\"with spaces\"]")
        assertEquals(1, dataName.size)
        assertEquals("with spaces", dataName.first()!!.attr("data-name"))
        val not = doc.select("div[title!=bar]")
        assertEquals(5, not.size)
        assertEquals("Foo", not.first()!!.attr("title"))
        val starts = doc.select("[title^=ba]")
        assertEquals(2, starts.size)
        assertEquals("Bar", starts.first()!!.attr("title"))
        assertEquals("Balim", starts.last()!!.attr("title"))
        val ends = doc.select("[title$=im]")
        assertEquals(2, ends.size)
        assertEquals("Balim", ends.first()!!.attr("title"))
        assertEquals("SLIM", ends.last()!!.attr("title"))
        val contains = doc.select("[title*=i]")
        assertEquals(2, contains.size)
        assertEquals("Balim", contains.first()!!.attr("title"))
        assertEquals("SLIM", contains.last()!!.attr("title"))
    }

    @Test
    fun testNamespacedTag() {
        val doc = Ksoup.parse("<div><abc:def id=1>Hello</abc:def></div> <abc:def class=bold id=2>There</abc:def>")
        val byTag = doc.select("abc|def")
        assertSelectedIds(byTag, "1", "2")
        val byAttr = doc.select(".bold")
        assertSelectedIds(byAttr, "2")
        val byTagAttr = doc.select("abc|def.bold")
        assertSelectedIds(byTagAttr, "2")
        val byContains = doc.select("abc|def:contains(e)")
        assertSelectedIds(byContains, "1", "2")
    }

    @Test
    fun testWildcardNamespacedTag() {
        val doc = Ksoup.parse("<div><abc:def id=1>Hello</abc:def></div> <abc:def class=bold id=2>There</abc:def>")
        val byTag = doc.select("*|def")
        assertSelectedIds(byTag, "1", "2")
        val byAttr = doc.select(".bold")
        assertSelectedIds(byAttr, "2")
        val byTagAttr = doc.select("*|def.bold")
        assertSelectedIds(byTagAttr, "2")
        val byContains = doc.select("*|def:contains(e)")
        assertSelectedIds(byContains, "1", "2")
    }

    @Test
    fun testWildcardNamespacedXmlTag() {
        val doc =
            Ksoup.parse(
                "<div><Abc:Def id=1>Hello</Abc:Def></div> <Abc:Def class=bold id=2>There</abc:def>",
                "",
                Parser.xmlParser(),
            )
        val byTag = doc.select("*|Def")
        assertSelectedIds(byTag, "1", "2")
        val byAttr = doc.select(".bold")
        assertSelectedIds(byAttr, "2")
        val byTagAttr = doc.select("*|Def.bold")
        assertSelectedIds(byTagAttr, "2")
        val byContains = doc.select("*|Def:contains(e)")
        assertSelectedIds(byContains, "1", "2")
    }

    @Test
    fun testWildCardNamespacedCaseVariations() {
        val doc = Ksoup.parse("<One:Two>One</One:Two><three:four>Two</three:four>", "", Parser.xmlParser())
        val els1 = doc.select("One|Two")
        val els2 = doc.select("one|two")
        val els3 = doc.select("Three|Four")
        val els4 = doc.select("three|Four")
        assertEquals(els1, els2)
        assertEquals(els3, els4)
        assertEquals("One", els1.text())
        assertEquals(1, els1.size)
        assertEquals("Two", els3.text())
        assertEquals(1, els2.size)
    }

    @Test
    fun testByAttributeStarting() {
        // TODO: mutlilocale test may move to jvm
//        Locale.setDefault(locale)
        val doc =
            Ksoup.parse("<div id=1 ATTRIBUTE data-name=jsoup>Hello</div><p data-val=5 id=2>There</p><p id=3>No</p>")
        var withData = doc.select("[^data-]")
        assertEquals(2, withData.size)
        assertEquals("1", withData.first()!!.id())
        assertEquals("2", withData.last()!!.id())
        withData = doc.select("p[^data-]")
        assertEquals(1, withData.size)
        assertEquals("2", withData.first()!!.id())
        assertEquals(1, doc.select("[^attrib]").size)
    }

    @Test
    fun testByAttributeRegex() {
        val doc =
            Ksoup.parse("<p><img src=foo.png id=1><img src=bar.jpg id=2><img src=qux.JPEG id=3><img src=old.gif><img></p>")
        val imgs = doc.select("img[src~=(?i)\\.(png|jpe?g)]")
        assertSelectedIds(imgs, "1", "2", "3")
    }

    @Test
    fun testByAttributeRegexCharacterClass() {
        val doc =
            Ksoup.parse("<p><img src=foo.png id=1><img src=bar.jpg id=2><img src=qux.JPEG id=3><img src=old.gif id=4></p>")
        val imgs = doc.select("img[src~=[o]]")
        assertSelectedIds(imgs, "1", "4")
    }

    @Test
    fun testByAttributeRegexCombined() {
        val doc = Ksoup.parse("<div><table class=x><td>Hello</td></table></div>")
        val els = doc.select("div table[class~=x|y]")
        assertEquals(1, els.size)
        assertEquals("Hello", els.text())
    }

    @Test
    fun testCombinedWithContains() {
        val doc = Ksoup.parse("<p id=1>One</p><p>Two +</p><p>Three +</p>")
        val els = doc.select("p#1 + :contains(+)")
        assertEquals(1, els.size)
        assertEquals("Two +", els.text())
        assertEquals("p", els.first()!!.tagName())
    }

    @Test
    fun testAllElements() {
        val h = "<div><p>Hello</p><p><b>there</b></p></div>"
        val doc = Ksoup.parse(h)
        val allDoc = doc.select("*")
        val allUnderDiv = doc.select("div *")
        assertEquals(8, allDoc.size)
        assertEquals(3, allUnderDiv.size)
        assertEquals("p", allUnderDiv.first()!!.tagName())
    }

    @Test
    fun testAllWithClass() {
        val h = "<p class=first>One<p class=first>Two<p>Three"
        val doc = Ksoup.parse(h)
        val ps = doc.select("*.first")
        assertEquals(2, ps.size)
    }

    @Test
    fun testGroupOr() {
        val h = "<div title=foo /><div title=bar /><div /><p></p><img /><span title=qux>"
        val doc = Ksoup.parse(h)
        val els = doc.select("p,div,[title]")
        assertEquals(5, els.size)
        assertEquals("div", els[0].tagName())
        assertEquals("foo", els[0].attr("title"))
        assertEquals("div", els[1].tagName())
        assertEquals("bar", els[1].attr("title"))
        assertEquals("div", els[2].tagName())
        assertEquals(0, els[2].attr("title").length) // missing attributes come back as empty string
        assertFalse(els[2].hasAttr("title"))
        assertEquals("p", els[3].tagName())
        assertEquals("span", els[4].tagName())
    }

    @Test
    fun testGroupOrAttribute() {
        val h = "<div id=1 /><div id=2 /><div title=foo /><div title=bar />"
        val els = Ksoup.parse(h).select("[id],[title=foo]")
        assertEquals(3, els.size)
        assertEquals("1", els[0].id())
        assertEquals("2", els[1].id())
        assertEquals("foo", els[2].attr("title"))
    }

    @Test
    fun descendant() {
        val h = "<div class=head><p class=first>Hello</p><p>There</p></div><p>None</p>"
        val doc = Ksoup.parse(h)
        val root = doc.getElementsByClass("HEAD").first()
        val els = root!!.select(".head p")
        assertEquals(2, els.size)
        assertEquals("Hello", els[0].text())
        assertEquals("There", els[1].text())
        val p = root.select("p.first")
        assertEquals(1, p.size)
        assertEquals("Hello", p[0].text())
        val empty = root.select("p .first") // self, not descend, should not match
        assertEquals(0, empty.size)
        val aboveRoot = root.select("body div.head")
        assertEquals(0, aboveRoot.size)
    }

    @Test
    fun and() {
        val h = "<div id=1 class='foo bar' title=bar name=qux><p class=foo title=bar>Hello</p></div"
        val doc = Ksoup.parse(h)
        val div = doc.select("div.foo")
        assertEquals(1, div.size)
        assertEquals("div", div.first()!!.tagName())
        val p = doc.select("div .foo") // space indicates like "div *.foo"
        assertEquals(1, p.size)
        assertEquals("p", p.first()!!.tagName())
        val div2 = doc.select("div#1.foo.bar[title=bar][name=qux]") // very specific!
        assertEquals(1, div2.size)
        assertEquals("div", div2.first()!!.tagName())
        val p2 = doc.select("div *.foo") // space indicates like "div *.foo"
        assertEquals(1, p2.size)
        assertEquals("p", p2.first()!!.tagName())
    }

    @Test
    fun deeperDescendant() {
        val h =
            "<div class=head><p><span class=first>Hello</div><div class=head><p class=first><span>Another</span><p>Again</div>"
        val doc = Ksoup.parse(h)
        val root = doc.getElementsByClass("head").first()
        val els = root!!.select("div p .first")
        assertEquals(1, els.size)
        assertEquals("Hello", els.first()!!.text())
        assertEquals("span", els.first()!!.tagName())
        val aboveRoot = root.select("body p .first")
        assertEquals(0, aboveRoot.size)
    }

    @Test
    fun parentChildElement() {
        val h = "<div id=1><div id=2><div id = 3></div></div></div><div id=4></div>"
        val doc = Ksoup.parse(h)
        val divs = doc.select("div > div")
        assertEquals(2, divs.size)
        assertEquals("2", divs[0].id()) // 2 is child of 1
        assertEquals("3", divs[1].id()) // 3 is child of 2
        val div2 = doc.select("div#1 > div")
        assertEquals(1, div2.size)
        assertEquals("2", div2[0].id())
    }

    @Test
    fun parentWithClassChild() {
        val h = "<h1 class=foo><a href=1 /></h1><h1 class=foo><a href=2 class=bar /></h1><h1><a href=3 /></h1>"
        val doc = Ksoup.parse(h)
        val allAs = doc.select("h1 > a")
        assertEquals(3, allAs.size)
        assertEquals("a", allAs.first()!!.tagName())
        val fooAs = doc.select("h1.foo > a")
        assertEquals(2, fooAs.size)
        assertEquals("a", fooAs.first()!!.tagName())
        val barAs = doc.select("h1.foo > a.bar")
        assertEquals(1, barAs.size)
    }

    @Test
    fun parentChildStar() {
        val h = "<div id=1><p>Hello<p><b>there</b></p></div><div id=2><span>Hi</span></div>"
        val doc = Ksoup.parse(h)
        val divChilds = doc.select("div > *")
        assertEquals(3, divChilds.size)
        assertEquals("p", divChilds[0].tagName())
        assertEquals("p", divChilds[1].tagName())
        assertEquals("span", divChilds[2].tagName())
    }

    @Test
    fun multiChildDescent() {
        val h = "<div id=foo><h1 class=bar><a href=http://example.com/>One</a></h1></div>"
        val doc = Ksoup.parse(h)
        val els = doc.select("div#foo > h1.bar > a[href*=example]")
        assertEquals(1, els.size)
        assertEquals("a", els.first()!!.tagName())
    }

    @Test
    fun caseInsensitive() {
        val h = "<dIv tItle=bAr><div>" // mixed case so a simple toLowerCase() on value doesn't catch
        val doc = Ksoup.parse(h)
        assertEquals(2, doc.select("DiV").size)
        assertEquals(1, doc.select("DiV[TiTLE]").size)
        assertEquals(1, doc.select("DiV[TiTLE=BAR]").size)
        assertEquals(0, doc.select("DiV[TiTLE=BARBARELLA]").size)
    }

    @Test
    fun adjacentSiblings() {
        val h = "<ol><li>One<li>Two<li>Three</ol>"
        val doc = Ksoup.parse(h)
        val sibs = doc.select("li + li")
        assertEquals(2, sibs.size)
        assertEquals("Two", sibs[0].text())
        assertEquals("Three", sibs[1].text())
    }

    @Test
    fun adjacentSiblingsWithId() {
        val h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>"
        val doc = Ksoup.parse(h)
        val sibs = doc.select("li#1 + li#2")
        assertEquals(1, sibs.size)
        assertEquals("Two", sibs[0].text())
    }

    @Test
    fun notAdjacent() {
        val h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>"
        val doc = Ksoup.parse(h)
        val sibs = doc.select("li#1 + li#3")
        assertEquals(0, sibs.size)
    }

    @Test
    fun mixCombinator() {
        val h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>"
        val doc = Ksoup.parse(h)
        val sibs = doc.select("body > div.foo li + li")
        assertEquals(2, sibs.size)
        assertEquals("Two", sibs[0].text())
        assertEquals("Three", sibs[1].text())
    }

    @Test
    fun mixCombinatorGroup() {
        val h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>"
        val doc = Ksoup.parse(h)
        val els = doc.select(".foo > ol, ol > li + li")
        assertEquals(3, els.size)
        assertEquals("ol", els[0].tagName())
        assertEquals("Two", els[1].text())
        assertEquals("Three", els[2].text())
    }

    @Test
    fun generalSiblings() {
        val h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>"
        val doc = Ksoup.parse(h)
        val els = doc.select("#1 ~ #3")
        assertEquals(1, els.size)
        assertEquals("Three", els.first()!!.text())
    }

    // for http://github.com/jhy/jsoup/issues#issue/10
    @Test
    fun testCharactersInIdAndClass() {
        // using CSS spec for identifiers (id and class): a-z0-9, -, _. NOT . (which is OK in html spec, but not css)
        val h = "<div><p id='a1-foo_bar'>One</p><p class='b2-qux_bif'>Two</p></div>"
        val doc = Ksoup.parse(h)
        val el1 = doc.getElementById("a1-foo_bar")
        assertEquals("One", el1!!.text())
        val el2 = doc.getElementsByClass("b2-qux_bif").first()
        assertEquals("Two", el2!!.text())
        val el3 = doc.select("#a1-foo_bar").first()
        assertEquals("One", el3!!.text())
        val el4 = doc.select(".b2-qux_bif").first()
        assertEquals("Two", el4!!.text())
    }

    // for http://github.com/jhy/jsoup/issues#issue/13
    @Test
    fun testSupportsLeadingCombinator() {
        var h = "<div><p><span>One</span><span>Two</span></p></div>"
        var doc = Ksoup.parse(h)
        val p = doc.select("div > p").first()
        val spans = p!!.select("> span")
        assertEquals(2, spans.size)
        assertEquals("One", spans.first()!!.text())

        // make sure doesn't get nested
        h = "<div id=1><div id=2><div id=3></div></div></div>"
        doc = Ksoup.parse(h)
        val div = doc.select("div").select(" > div").first()
        assertEquals("2", div!!.id())
    }

    @Test
    fun testPseudoLessThan() {
        val doc = Ksoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>")
        val ps = doc.select("div p:lt(2)")
        assertEquals(3, ps.size)
        assertEquals("One", ps[0].text())
        assertEquals("Two", ps[1].text())
        assertEquals("Four", ps[2].text())
    }

    @Test
    fun testPseudoGreaterThan() {
        val doc = Ksoup.parse("<div><p>One</p><p>Two</p><p>Three</p></div><div><p>Four</p>")
        val ps = doc.select("div p:gt(0)")
        assertEquals(2, ps.size)
        assertEquals("Two", ps[0].text())
        assertEquals("Three", ps[1].text())
    }

    @Test
    fun testPseudoEquals() {
        val doc = Ksoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>")
        val ps = doc.select("div p:eq(0)")
        assertEquals(2, ps.size)
        assertEquals("One", ps[0].text())
        assertEquals("Four", ps[1].text())
        val ps2 = doc.select("div:eq(0) p:eq(0)")
        assertEquals(1, ps2.size)
        assertEquals("One", ps2[0].text())
        assertEquals("p", ps2[0].tagName())
    }

    @Test
    fun testPseudoBetween() {
        val doc = Ksoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>")
        val ps = doc.select("div p:gt(0):lt(2)")
        assertEquals(1, ps.size)
        assertEquals("Two", ps[0].text())
    }

    @Test
    fun testPseudoCombined() {
        val doc = Ksoup.parse("<div class='foo'><p>One</p><p>Two</p></div><div><p>Three</p><p>Four</p></div>")
        val ps = doc.select("div.foo p:gt(0)")
        assertEquals(1, ps.size)
        assertEquals("Two", ps[0].text())
    }

    @Test
    fun testPseudoHas() {
        val doc =
            Ksoup.parse("<div id=0><p><span>Hello</span></p></div> <div id=1><span class=foo>There</span></div> <div id=2><p>Not</p></div>")
        val divs1 = doc.select("div:has(span)")
        assertEquals(2, divs1.size)
        assertEquals("0", divs1[0].id())
        assertEquals("1", divs1[1].id())
        val divs2 = doc.select("div:has([class])")
        assertEquals(1, divs2.size)
        assertEquals("1", divs2[0].id())
        val divs3 = doc.select("div:has(span, p)")
        assertEquals(3, divs3.size)
        assertEquals("0", divs3[0].id())
        assertEquals("1", divs3[1].id())
        assertEquals("2", divs3[2].id())
        val els1 = doc.body().select(":has(p)")
        assertEquals(3, els1.size) // body, div, div
        assertEquals("body", els1.first()!!.tagName())
        assertEquals("0", els1[1].id())
        assertEquals("2", els1[2].id())
        val els2 = doc.body().select(":has(> span)")
        assertEquals(2, els2.size) // p, div
        assertEquals("p", els2.first()!!.tagName())
        assertEquals("1", els2[1].id())
    }

    @Test
    fun testNestedHas() {
        val doc = Ksoup.parse("<div><p><span>One</span></p></div> <div><p>Two</p></div>")
        var divs = doc.select("div:has(p:has(span))")
        assertEquals(1, divs.size)
        assertEquals("One", divs.first()!!.text())

        // test matches in has
        divs = doc.select("div:has(p:matches((?i)two))")
        assertEquals(1, divs.size)
        assertEquals("div", divs.first()!!.tagName())
        assertEquals("Two", divs.first()!!.text())

        // test contains in has
        divs = doc.select("div:has(p:contains(two))")
        assertEquals(1, divs.size)
        assertEquals("div", divs.first()!!.tagName())
        assertEquals("Two", divs.first()!!.text())
    }

    @Test
    fun testPseudoContains() {
        // TODO: mutlilocale test may move to jvm
//        Locale.setDefault(locale)
        val doc = Ksoup.parse("<div><p>The Rain.</p> <p class=light>The <i>RAIN</i>.</p> <p>Rain, the.</p></div>")
        val ps1 = doc.select("p:contains(Rain)")
        assertEquals(3, ps1.size)
        val ps2 = doc.select("p:contains(the rain)")
        assertEquals(2, ps2.size)
        assertEquals("The Rain.", ps2.first()!!.html())
        assertEquals("The <i>RAIN</i>.", ps2.last()!!.html())
        val ps3 = doc.select("p:contains(the Rain):has(i)")
        assertEquals(1, ps3.size)
        assertEquals("light", ps3.first()!!.className())
        val ps4 = doc.select(".light:contains(rain)")
        assertEquals(1, ps4.size)
        assertEquals("light", ps3.first()!!.className())
        val ps5 = doc.select(":contains(rain)")
        assertEquals(8, ps5.size) // html, body, div,...
        val ps6 = doc.select(":contains(RAIN)")
        assertEquals(8, ps6.size)
    }

    @Test
    fun testPsuedoContainsWithParentheses() {
        val doc = Ksoup.parse("<div><p id=1>This (is good)</p><p id=2>This is bad)</p>")
        val ps1 = doc.select("p:contains(this (is good))")
        assertEquals(1, ps1.size)
        assertEquals("1", ps1.first()!!.id())
        val ps2 = doc.select("p:contains(this is bad\\))")
        assertEquals(1, ps2.size)
        assertEquals("2", ps2.first()!!.id())
    }

    @Test
    fun containsWholeText() {
        var doc = Ksoup.parse("<div><p> jsoup\n The <i>HTML</i> Parser</p><p>jsoup The HTML Parser</div>")
        val ps = doc.select("p")
        val es1 = doc.select("p:containsWholeText( jsoup\n The HTML Parser)")
        val es2 = doc.select("p:containsWholeText(jsoup The HTML Parser)")
        assertEquals(1, es1.size)
        assertEquals(1, es2.size)
        assertEquals(ps[0], es1.first())
        assertEquals(ps[1], es2.first())
        assertEquals(0, doc.select("div:containsWholeText(jsoup the html parser)").size)
        assertEquals(0, doc.select("div:containsWholeText(jsoup\n the html parser)").size)
        doc = Ksoup.parse("<div><p></p><p> </p><p>.  </p>")
        val blanks = doc.select("p:containsWholeText(  )")
        assertEquals(1, blanks.size)
        assertEquals(".  ", blanks.first()!!.wholeText())
    }

    @Test
    fun containsWholeOwnText() {
        var doc = Ksoup.parse("<div><p> jsoup\n The <i>HTML</i> Parser</p><p>jsoup The HTML Parser<br></div>")
        val ps = doc.select("p")
        val es1 = doc.select("p:containsWholeOwnText( jsoup\n The  Parser)")
        val es2 = doc.select("p:containsWholeOwnText(jsoup The HTML Parser\n)")
        assertEquals(1, es1.size)
        assertEquals(1, es2.size)
        assertEquals(ps[0], es1.first())
        assertEquals(ps[1], es2.first())
        assertEquals(0, doc.select("div:containsWholeOwnText(jsoup the html parser)").size)
        assertEquals(0, doc.select("div:containsWholeOwnText(jsoup\n the  parser)").size)
        doc = Ksoup.parse("<div><p></p><p> </p><p>.  </p>")
        val blanks = doc.select("p:containsWholeOwnText(  )")
        assertEquals(1, blanks.size)
        assertEquals(".  ", blanks.first()!!.wholeText())
    }

    @Test
    fun containsOwn() {
        // TODO: mutlilocale test may move to jvm
//        Locale.setDefault(locale)
        val doc = Ksoup.parse("<p id=1>Hello <b>there</b> igor</p>")
        val ps = doc.select("p:containsOwn(Hello IGOR)")
        assertEquals(1, ps.size)
        assertEquals("1", ps.first()!!.id())
        assertEquals(0, doc.select("p:containsOwn(there)").size)
        val doc2 = Ksoup.parse("<p>Hello <b>there</b> IGOR</p>")
        assertEquals(1, doc2.select("p:containsOwn(igor)").size)
    }

    @Test
    fun testMatches() {
        val doc =
            Ksoup.parse("<p id=1>The <i>Rain</i></p> <p id=2>There are 99 bottles.</p> <p id=3>Harder (this)</p> <p id=4>Rain</p>")
        val p1 = doc.select("p:matches(The rain)") // no match, case sensitive
        assertEquals(0, p1.size)
        val p2 = doc.select("p:matches((?i)the rain)") // case insense. should include root, html, body
        assertEquals(1, p2.size)
        assertEquals("1", p2.first()!!.id())
        val p4 = doc.select("p:matches((?i)^rain$)") // bounding
        assertEquals(1, p4.size)
        assertEquals("4", p4.first()!!.id())
        val p5 = doc.select("p:matches(\\d+)")
        assertEquals(1, p5.size)
        assertEquals("2", p5.first()!!.id())
        val p6 = doc.select("p:matches(\\w+\\s+\\(\\w+\\))") // test bracket matching
        assertEquals(1, p6.size)
        assertEquals("3", p6.first()!!.id())
        val p7 = doc.select("p:matches((?i)the):has(i)") // multi
        assertEquals(1, p7.size)
        assertEquals("1", p7.first()!!.id())
    }

    @Test
    fun matchesOwn() {
        val doc = Ksoup.parse("<p id=1>Hello <b>there</b> now</p>")
        val p1 = doc.select("p:matchesOwn((?i)hello now)")
        assertEquals(1, p1.size)
        assertEquals("1", p1.first()!!.id())
        assertEquals(0, doc.select("p:matchesOwn(there)").size)
    }

    @Test
    fun matchesWholeText() {
        val doc = Ksoup.parse("<p id=1>Hello <b>there</b>\n now</p><p id=2> </p><p id=3></p>")
        val p1 = doc.select("p:matchesWholeText((?i)hello there\n now)")
        assertEquals(1, p1.size)
        assertEquals("1", p1.first()!!.id())
        assertEquals(1, doc.select("p:matchesWholeText(there\n now)").size)
        assertEquals(0, doc.select("p:matchesWholeText(There\n now)").size)
        val p2 = doc.select("p:matchesWholeText(^\\s+$)")
        assertEquals(1, p2.size)
        assertEquals("2", p2.first()!!.id())
        val p3 = doc.select("p:matchesWholeText(^$)")
        assertEquals(1, p3.size)
        assertEquals("3", p3.first()!!.id())
    }

    @Test
    fun matchesWholeOwnText() {
        val doc = Ksoup.parse("<p id=1>Hello <b>there</b>\n now</p><p id=2> </p><p id=3><i>Text</i></p>")
        val p1 = doc.select("p:matchesWholeOwnText((?i)hello \n now)")
        assertEquals(1, p1.size)
        assertEquals("1", p1.first()!!.id())
        assertEquals(0, doc.select("p:matchesWholeOwnText(there\n now)").size)
        val p2 = doc.select("p:matchesWholeOwnText(^\\s+$)")
        assertEquals(1, p2.size)
        assertEquals("2", p2.first()!!.id())
        val p3 = doc.select("p:matchesWholeOwnText(^$)")
        assertEquals(1, p3.size)
        assertEquals("3", p3.first()!!.id())
    }

    @Test
    fun testRelaxedTags() {
        val doc = Ksoup.parse("<abc_def id=1>Hello</abc_def> <abc-def id=2>There</abc-def>")
        val el1 = doc.select("abc_def")
        assertEquals(1, el1.size)
        assertEquals("1", el1.first()!!.id())
        val el2 = doc.select("abc-def")
        assertEquals(1, el2.size)
        assertEquals("2", el2.first()!!.id())
    }

    @Test
    fun notParas() {
        val doc = Ksoup.parse("<p id=1>One</p> <p>Two</p> <p><span>Three</span></p>")
        val el1 = doc.select("p:not([id=1])")
        assertEquals(2, el1.size)
        assertEquals("Two", el1.first()!!.text())
        assertEquals("Three", el1.last()!!.text())
        val el2 = doc.select("p:not(:has(span))")
        assertEquals(2, el2.size)
        assertEquals("One", el2.first()!!.text())
        assertEquals("Two", el2.last()!!.text())
    }

    @Test
    fun notAll() {
        val doc = Ksoup.parse("<p>Two</p> <p><span>Three</span></p>")
        val el1 = doc.body().select(":not(p)") // should just be the span
        assertEquals(2, el1.size)
        assertEquals("body", el1.first()!!.tagName())
        assertEquals("span", el1.last()!!.tagName())
    }

    @Test
    fun notClass() {
        val doc = Ksoup.parse("<div class=left>One</div><div class=right id=1><p>Two</p></div>")
        val el1 = doc.select("div:not(.left)")
        assertEquals(1, el1.size)
        assertEquals("1", el1.first()!!.id())
    }

    @Test
    fun handlesCommasInSelector() {
        val doc = Ksoup.parse("<p name='1,2'>One</p><div>Two</div><ol><li>123</li><li>Text</li></ol>")
        val ps = doc.select("[name=1,2]")
        assertEquals(1, ps.size)
        val containers = doc.select("div, li:matches([0-9,]+)")
        assertEquals(2, containers.size)
        assertEquals("div", containers[0].tagName())
        assertEquals("li", containers[1].tagName())
        assertEquals("123", containers[1].text())
    }

    @Test
    fun selectSupplementaryCharacter() {
        val s = 135361.toCodePoint().toChars().concatToString()
        val doc = Ksoup.parse("<div k$s='$s'>^$s$/div>")
        assertEquals("div", doc.select("div[k$s]").first()!!.tagName())
        assertEquals("div", doc.select("div:containsOwn($s)").first()!!.tagName())
    }

    @Test
    fun selectClassWithSpace() {
        val html =
            """
            <div class="value">class without space</div>
            <div class="value ">class with space</div>
            """.trimIndent()
        val doc = Ksoup.parse(html)
        var found = doc.select("div[class=value ]")
        assertEquals(2, found.size)
        assertEquals("class without space", found[0].text())
        assertEquals("class with space", found[1].text())
        found = doc.select("div[class=\"value \"]")
        assertEquals(2, found.size)
        assertEquals("class without space", found[0].text())
        assertEquals("class with space", found[1].text())
        found = doc.select("div[class=\"value\\ \"]")
        assertEquals(0, found.size)
    }

    @Test
    fun selectSameElements() {
        val html = "<div>one</div><div>one</div>"
        val doc = Ksoup.parse(html)
        val els = doc.select("div")
        assertEquals(2, els.size)
        val subSelect = els.select(":contains(one)")
        assertEquals(2, subSelect.size)
    }

    @Test
    fun attributeWithBrackets() {
        val html = "<div data='End]'>One</div> <div data='[Another)]]'>Two</div>"
        val doc = Ksoup.parse(html)
        assertEquals("One", doc.select("div[data='End]']").first()!!.text())
        assertEquals("Two", doc.select("div[data='[Another)]]']").first()!!.text())
        assertEquals("One", doc.select("div[data=\"End]\"]").first()!!.text())
        assertEquals("Two", doc.select("div[data=\"[Another)]]\"]").first()!!.text())
    }

    @Test
    fun containsData() {
        // TODO: multilocale test may move to jvm
//        Locale.setDefault(locale)

        val html = "<p>function</p><script>FUNCTION</script><style>item</style><span><!-- comments --></span>"
        val doc = Ksoup.parse(html)
        val body = doc.body()
        val dataEls1 = body.select(":containsData(function)")
        val dataEls2 = body.select("script:containsData(function)")
        val dataEls3 = body.select("span:containsData(comments)")
        val dataEls4 = body.select(":containsData(o)")
        val dataEls5 = body.select("style:containsData(ITEM)")
        assertEquals(2, dataEls1.size) // body and script
        assertEquals(1, dataEls2.size)
        assertEquals(dataEls1.last(), dataEls2.first())
        assertEquals("<script>FUNCTION</script>", dataEls2.outerHtml())
        assertEquals(1, dataEls3.size)
        assertEquals("span", dataEls3.first()!!.tagName())
        assertEquals(3, dataEls4.size)
        assertEquals("body", dataEls4.first()!!.tagName())
        assertEquals("script", dataEls4[1].tagName())
        assertEquals("span", dataEls4[2].tagName())
        assertEquals(1, dataEls5.size)
    }

    @Test
    fun containsWithQuote() {
        val html = "<p>One'One</p><p>One'Two</p>"
        val doc = Ksoup.parse(html)
        val els = doc.select("p:contains(One\\'One)")
        assertEquals(1, els.size)
        assertEquals("One'One", els.text())
    }

    @Test
    fun selectFirst() {
        val html = "<p>One<p>Two<p>Three"
        val doc = Ksoup.parse(html)
        assertEquals("One", doc.selectFirst("p")!!.text())
    }

    @Test
    fun selectFirstWithAnd() {
        val html = "<p>One<p class=foo>Two<p>Three"
        val doc = Ksoup.parse(html)
        assertEquals("Two", doc.selectFirst("p.foo")!!.text())
    }

    @Test
    fun selectFirstWithOr() {
        val html = "<p>One<p>Two<p>Three<div>Four"
        val doc = Ksoup.parse(html)
        assertEquals("One", doc.selectFirst("p, div")!!.text())
    }

    @Test
    fun matchText() {
        val html = "<p>One<br>Two</p>"
        val doc = Ksoup.parse(html)
        doc.outputSettings().prettyPrint(false)
        val origHtml = doc.html()
        val one = doc.select("p:matchText:first-child")
        assertEquals("One", one.first()!!.text())
        val two = doc.select("p:matchText:last-child")
        assertEquals("Two", two.first()!!.text())
        assertEquals(origHtml, doc.html())
        assertEquals("Two", doc.select("p:matchText + br + *").text())
    }

    @Test
    fun nthLastChildWithNoParent() {
        val el = Element("p").text("Orphan")
        val els = el.select("p:nth-last-child(1)")
        assertEquals(0, els.size)
    }

    @Test
    fun splitOnBr() {
        val html = "<div><p>One<br>Two<br>Three</p></div>"
        val doc = Ksoup.parse(html)
        val els = doc.select("p:matchText")
        assertEquals(3, els.size)
        assertEquals("One", els[0].text())
        assertEquals("Two", els[1].text())
        assertEquals("Three", els[2].toString())
    }

    @Test
    fun matchTextAttributes() {
        val doc = Ksoup.parse("<div><p class=one>One<br>Two<p class=two>Three<br>Four")
        val els = doc.select("p.two:matchText:last-child")
        assertEquals(1, els.size)
        assertEquals("Four", els.text())
    }

    @Test
    fun findBetweenSpan() {
        val doc = Ksoup.parse("<p><span>One</span> Two <span>Three</span>")
        val els = doc.select("span ~ p:matchText") // the Two becomes its own p, sibling of the span
        // todo - think this should really be 'p:matchText span ~ p'. The :matchText should behave as a modifier to expand the nodes.
        assertEquals(1, els.size)
        assertEquals("Two", els.text())
    }

    @Test
    fun startsWithBeginsWithSpace() {
        val doc = Ksoup.parse("<small><a href=\" mailto:abc@def.net\">(abc@def.net)</a></small>")
        val els = doc.select("a[href^=' mailto']")
        assertEquals(1, els.size)
    }

    @Test
    fun endsWithEndsWithSpaces() {
        val doc = Ksoup.parse("<small><a href=\" mailto:abc@def.net \">(abc@def.net)</a></small>")
        val els = doc.select("a[href$='.net ']")
        assertEquals(1, els.size)
    }

    // https://github.com/jhy/jsoup/issues/1257
    private val mixedCase = "<html xmlns:n=\"urn:ns\"><n:mixedCase>text</n:mixedCase></html>"
    private val lowercase = "<html xmlns:n=\"urn:ns\"><n:lowercase>text</n:lowercase></html>"

    @Test
    fun html_mixed_case_simple_name() {
        val doc = Ksoup.parse(mixedCase, "", Parser.htmlParser())
        assertEquals(0, doc.select("mixedCase").size)
    }

    @Test
    fun html_mixed_case_wildcard_name() {
        val doc = Ksoup.parse(mixedCase, "", Parser.htmlParser())
        assertEquals(1, doc.select("*|mixedCase").size)
    }

    @Test
    fun html_lowercase_simple_name() {
        val doc = Ksoup.parse(lowercase, "", Parser.htmlParser())
        assertEquals(0, doc.select("lowercase").size)
    }

    @Test
    fun html_lowercase_wildcard_name() {
        val doc = Ksoup.parse(lowercase, "", Parser.htmlParser())
        assertEquals(1, doc.select("*|lowercase").size)
    }

    @Test
    fun xml_mixed_case_simple_name() {
        val doc = Ksoup.parse(mixedCase, "", Parser.xmlParser())
        assertEquals(0, doc.select("mixedCase").size)
    }

    @Test
    fun xml_mixed_case_wildcard_name() {
        val doc = Ksoup.parse(mixedCase, "", Parser.xmlParser())
        assertEquals(1, doc.select("*|mixedCase").size)
    }

    @Test
    fun xml_lowercase_simple_name() {
        val doc = Ksoup.parse(lowercase, "", Parser.xmlParser())
        assertEquals(0, doc.select("lowercase").size)
    }

    @Test
    fun xml_lowercase_wildcard_name() {
        val doc = Ksoup.parse(lowercase, "", Parser.xmlParser())
        assertEquals(1, doc.select("*|lowercase").size)
    }

    @Test
    fun trimSelector() {
        // https://github.com/jhy/jsoup/issues/1274
        val doc = Ksoup.parse("<p><span>Hello")
        val els = doc.select(" p span ")
        assertEquals(1, els.size)
        assertEquals("Hello", els.first()!!.text())
    }

    @Test
    fun xmlWildcardNamespaceTest() {
        // https://github.com/jhy/jsoup/issues/1208
        val doc =
            Ksoup.parse("<ns1:MyXmlTag>1111</ns1:MyXmlTag><ns2:MyXmlTag>2222</ns2:MyXmlTag>", "", Parser.xmlParser())
        val select = doc.select("*|MyXmlTag")
        assertEquals(2, select.size)
        assertEquals("1111", select[0].text())
        assertEquals("2222", select[1].text())
    }

    @Test
    fun childElements() {
        // https://github.com/jhy/jsoup/issues/1292
        val html = "<body><span id=1>One <span id=2>Two</span></span></body>"
        val doc = Ksoup.parse(html)
        val outer = doc.selectFirst("span")
        val span = outer!!.selectFirst("span")
        val inner = outer.selectFirst("* span")
        assertEquals("1", outer.id())
        assertEquals("1", span!!.id())
        assertEquals("2", inner!!.id())
        assertEquals(outer, span)
        assertNotEquals(outer, inner)
    }

    @Test
    fun selectFirstLevelChildrenOnly() {
        // testcase for https://github.com/jhy/jsoup/issues/984
        val html = "<div><span>One <span>Two</span></span> <span>Three <span>Four</span></span>"
        val doc = Ksoup.parse(html)
        val div = doc.selectFirst("div")
        assertNotNull(div)

        // want to select One and Three only - the first level children
        val spans = div.select(":root > span")
        assertEquals(2, spans.size)
        assertEquals("One Two", spans[0].text())
        assertEquals("Three Four", spans[1].text())
    }

    @Test
    fun wildcardNamespaceMatchesNoNamespace() {
        // https://github.com/jhy/jsoup/issues/1565
        val xml = "<package><meta>One</meta><opf:meta>Two</opf:meta></package>"
        val doc = Ksoup.parse(xml, "", Parser.xmlParser())
        val metaEls = doc.select("meta")
        assertEquals(1, metaEls.size)
        assertEquals("One", metaEls[0].text())
        val nsEls = doc.select("*|meta")
        assertEquals(2, nsEls.size)
        assertEquals("One", nsEls[0].text())
        assertEquals("Two", nsEls[1].text())
    }

    @Test
    fun containsTextQueryIsNormalized() {
        val doc = Ksoup.parse("<p><p id=1>Hello  there now<em>!</em>")
        val a = doc.select("p:contains(Hello   there  now!)")
        val b = doc.select(":containsOwn(hello   there  now)")
        val c = doc.select("p:contains(Hello there now)")
        val d = doc.select(":containsOwn(hello There now)")
        val e = doc.select("p:contains(HelloThereNow)")
        assertEquals(1, a.size)
        assertEquals(a, b)
        assertEquals(a, c)
        assertEquals(a, d)
        assertEquals(0, e.size)
        assertNotEquals(a, e)
    }

    @Test
    fun selectorExceptionNotStringFormatException() {
        val ex = Selector.SelectorParseException("%&")
        assertEquals("%&", ex.message)
    }

    @Test
    fun evaluatorMemosAreReset() {
        val eval = QueryParser.parse("p ~ p")
        val andEval = eval as CombiningEvaluator.And
        val prevEval = andEval.evaluators[0] as StructuralEvaluator.PreviousSibling
        val map: com.fleeksoft.ksoup.ported.IdentityHashMap<Element, com.fleeksoft.ksoup.ported.IdentityHashMap<Element, Boolean>> =
            prevEval.threadMemo
        assertEquals(0, map.size) // no memo yet
        val doc1 = Ksoup.parse("<p>One<p>Two<p>Three")
        val doc2 = Ksoup.parse("<p>One2<p>Two2<p>Three2")
        val s1 = doc1.select(eval)
        assertEquals(2, s1.size)
        assertEquals("Two", s1.first()!!.text())
        val s2 = doc2.select(eval)
        assertEquals(2, s2.size)
        assertEquals("Two2", s2.first()!!.text())
        assertEquals(1, map.size) // root of doc 2
    }

    @Test
    fun blankTextNodesAreConsideredEmpty() {
        // https://github.com/jhy/jsoup/issues/1976
        val html = "<li id=1>\n </li><li id=2></li><li id=3> </li><li id=4>One</li><li id=5><span></li>"
        val doc = Ksoup.parse(html)
        val empty = doc.select("li:empty")
        val notEmpty = doc.select("li:not(:empty)")
        assertSelectedIds(empty, "1", "2", "3")
        assertSelectedIds(notEmpty, "4", "5")
    }

    @Test
    fun parentFromSpecifiedDescender() {
        // https://github.com/jhy/jsoup/issues/2018
        val html = "<ul id=outer><li>Foo</li><li>Bar <ul id=inner><li>Baz</li><li>Qux</li></ul> </li></ul>"
        val doc = Ksoup.parse(html)
        val ul = doc.expectFirst("#outer")
        assertEquals(2, ul.childrenSize())
        val li1 = ul.expectFirst("> li:nth-child(1)")
        assertEquals("Foo", li1.ownText())
        assertTrue(li1.select("ul").isEmpty())
        val li2 = ul.expectFirst("> li:nth-child(2)")
        assertEquals("Bar", li2.ownText())

        // And now for the bug - li2 select was not restricted to the li2 context
        val innerLis = li2.select("ul > li")
        assertSelectedOwnText(innerLis, "Baz", "Qux")

        // Confirm that parent selector (" ") works same as immediate parent (">");
        val innerLisFromParent = li2.select("ul li")
        assertEquals(innerLis, innerLisFromParent)
    }

    @Test
    fun rootImmediateParentSubquery() {
        // a combinator at the start of the query is applied to the Root selector. i.e. "> p" matches a P immediately parented
        // by the Root (which is <html> for a top level query, or the context element in :has)
        // in the sub query, the combinator was dropped incorrectly
        val html = "<p id=0><span>A</p> <p id=1><b><i><span>B</p> <p id=2><i>C</p>\n"
        val doc = Ksoup.parse(html)
        val els = doc.select("p:has(> span, > i)") // should match a p with an immediate span or i
        assertSelectedIds(els, "0", "2")
    }

    @Test
    fun `is`() {
        val html =
            "<h1 id=1><p></p></h1> <section><h1 id=2></h1></section> <article><h2 id=3></h2></article> <h2 id=4><p></p></h2>"
        val doc = Ksoup.parse(html)
        assertSelectedIds(
            doc.select(":is(section, article) :is(h1, h2, h3)"),
            "2",
            "3",
        )
        assertSelectedIds(
            doc.select(":is(section, article) ~ :is(h1, h2, h3):has(p)"),
            "4",
        )
        assertSelectedIds(
            doc.select(":is(h1:has(p), h2:has(section), h3)"),
            "1",
        )
        assertSelectedIds(
            doc.select(":is(h1, h2, h3):has(p)"),
            "1",
            "4",
        )
        val query = "div :is(h1, h2)"
        val parse = QueryParser.parse(query)
        assertEquals(query, parse.toString())
    }

    companion object {
        /** Test that the selected elements match exactly the specified IDs.  */
        fun assertSelectedIds(
            els: Elements,
            vararg ids: String?,
        ) {
            assertNotNull(els)
            assertEquals(ids.size, els.size, "Incorrect number of selected elements")
            for (i in ids.indices) {
                assertEquals(ids[i], els[i].id(), "Incorrect content at index")
            }
        }

        fun assertSelectedOwnText(
            els: Elements,
            vararg ownTexts: String?,
        ) {
            assertNotNull(els)
            assertEquals(ownTexts.size, els.size, "Incorrect number of selected elements")
            for (i in ownTexts.indices) {
                assertEquals(ownTexts[i], els[i].ownText(), "Incorrect content at index")
            }
        }
    }
}
