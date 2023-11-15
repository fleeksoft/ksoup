package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.parser.Tag
import com.fleeksoft.ksoup.select.NodeVisitor
import kotlin.test.Test
import kotlin.test.*

/**
 * Tests Nodes
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class NodeTest {
    @Test
    fun handlesBaseUri() {
        val tag = Tag.valueOf("a")
        val attribs = Attributes()
        attribs.put("relHref", "/foo")
        attribs.put("absHref", "http://bar/qux")
        val noBase = Element(tag, "", attribs)
        assertEquals(
            "",
            noBase.absUrl("relHref")
        ) // with no base, should NOT fallback to href attrib, whatever it is
        assertEquals("http://bar/qux", noBase.absUrl("absHref")) // no base but valid attrib, return attrib
        val withBase = Element(tag, "http://foo/", attribs)
        assertEquals("http://foo/foo", withBase.absUrl("relHref")) // construct abs from base + rel
        assertEquals("http://bar/qux", withBase.absUrl("absHref")) // href is abs, so returns that
        assertEquals("", withBase.absUrl("noval"))
        val dodgyBase = Element(tag, "wtf://no-such-protocol/", attribs)
        assertEquals("http://bar/qux", dodgyBase.absUrl("absHref")) // base fails, but href good, so get that
        assertEquals("", dodgyBase.absUrl("relHref")) // base fails, only rel href, so return nothing
    }

    @Test
    fun setBaseUriIsRecursive() {
        val doc = Ksoup.parse("<div><p></p></div>")
        val baseUri = "https://jsoup.org"
        doc.setBaseUri(baseUri)
        assertEquals(baseUri, doc.baseUri())
        assertEquals(baseUri, doc.select("div").first()!!.baseUri())
        assertEquals(baseUri, doc.select("p").first()!!.baseUri())
    }

    @Test
    fun handlesAbsPrefix() {
        val doc = Ksoup.parse("<a href=/foo>Hello</a>", "https://jsoup.org/")
        val a = doc.select("a").first()
        assertEquals("/foo", a!!.attr("href"))
        assertEquals("https://jsoup.org/foo", a.attr("abs:href"))
        assertTrue(a.hasAttr("abs:href"))
    }

    @Test
    fun handlesAbsOnImage() {
        val doc = Ksoup.parse("<p><img src=\"/rez/osi_logo.png\" /></p>", "https://jsoup.org/")
        val img = doc.select("img").first()
        assertEquals("https://jsoup.org/rez/osi_logo.png", img!!.attr("abs:src"))
        assertEquals(img.absUrl("src"), img.attr("abs:src"))
    }

    @Test
    fun handlesAbsPrefixOnHasAttr() {
        // 1: no abs url; 2: has abs url
        val doc = Ksoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://jsoup.org/'>Two</a>")
        val one = doc.select("#1").first()
        val two = doc.select("#2").first()
        assertFalse(one!!.hasAttr("abs:href"))
        assertTrue(one.hasAttr("href"))
        assertEquals("", one.absUrl("href"))
        assertTrue(two!!.hasAttr("abs:href"))
        assertTrue(two.hasAttr("href"))
        assertEquals("https://jsoup.org/", two.absUrl("href"))
    }

    @Test
    fun literalAbsPrefix() {
        // if there is a literal attribute "abs:xxx", don't try and make absolute.
        val doc = Ksoup.parse("<a abs:href='odd'>One</a>")
        val el = doc.select("a").first()
        assertTrue(el!!.hasAttr("abs:href"))
        assertEquals("odd", el.attr("abs:href"))
    }

    @Test
    fun handleAbsOnFileUris() {
        val doc = Ksoup.parse("<a href='password'>One/a><a href='/var/log/messages'>Two</a>", "file://etc/")
        val one = doc.select("a").first()
        assertEquals("file://etc/password", one!!.absUrl("href"))
        val two = doc.select("a")[1]
        assertEquals("file://var/log/messages", two.absUrl("href"))
    }

    @Test
    fun handleAbsOnLocalhostFileUris() {
        val doc = Ksoup.parse("<a href='password'>One/a><a href='/var/log/messages'>Two</a>", "file://localhost/etc/")
        val one = doc.select("a").first()!!
        assertEquals("file://localhost/etc/password", one.absUrl("href"))
    }

    @Test
    fun handlesAbsOnProtocolessAbsoluteUris() {
        val doc1 = Ksoup.parse("<a href='//example.net/foo'>One</a>", "http://example.com/")
        val doc2 = Ksoup.parse("<a href='//example.net/foo'>One</a>", "https://example.com/")
        val one = doc1.select("a").first()
        val two = doc2.select("a").first()
        assertEquals("http://example.net/foo", one!!.absUrl("href"))
        assertEquals("https://example.net/foo", two!!.absUrl("href"))
        val doc3 = Ksoup.parse("<img src=//www.google.com/images/errors/logo_sm.gif alt=Google>", "https://google.com")
        assertEquals("https://www.google.com/images/errors/logo_sm.gif", doc3.select("img").attr("abs:src"))
    }

    /*
    Test for an issue with Java's abs URL handler.
     */
    @Test
    fun absHandlesRelativeQuery() {
        val doc =
            Ksoup.parse("<a href='?foo'>One</a> <a href='bar.html?foo'>Two</a>", "https://jsoup.org/path/file?bar")
        val a1 = doc.select("a").first()!!
        assertEquals("https://jsoup.org/path/file?foo", a1.absUrl("href"))
        val a2 = doc.select("a")[1]
        assertEquals("https://jsoup.org/path/bar.html?foo", a2.absUrl("href"))
    }

    @Test
    fun absHandlesDotFromIndex() {
        val doc = Ksoup.parse("<a href='./one/two.html'>One</a>", "http://example.com")
        val a1 = doc.select("a").first()
        assertEquals("http://example.com/one/two.html", a1!!.absUrl("href"))
    }

    @Test
    fun handlesAbsOnUnknownProtocols() {
        // https://github.com/jhy/jsoup/issues/1610
        // URL would throw on unknown protocol tel: as no stream handler is registered
        val urls = arrayOf("mailto:example@example.com", "tel:867-5309") // mail has a handler, tel doesn't
        for (url in urls) {
            val attr = Attributes().put("href", url)
            val noBase = Element(Tag.valueOf("a"), null, attr)
            assertEquals(url, noBase.absUrl("href"))
            val withBase = Element(Tag.valueOf("a"), "http://example.com/", attr)
            assertEquals(url, withBase.absUrl("href"))
        }
    }

    @Test
    fun testRemove() {
        val doc = Ksoup.parse("<p>One <span>two</span> three</p>")
        val p = doc.select("p").first()
        p!!.childNode(0).remove()
        assertEquals("two three", p.text())
        assertEquals("<span>two</span> three", com.fleeksoft.ksoup.TextUtil.stripNewlines(p.html()))
    }

    @Test
    fun removeOnOrphanIsNoop() {
        // https://github.com/jhy/jsoup/issues/1898
        val node = Element("div")
        assertNull(node.parentNode())
        node.remove()
        assertNull(node.parentNode())
    }

    @Test
    fun testReplace() {
        val doc = Ksoup.parse("<p>One <span>two</span> three</p>")
        val p = doc.select("p").first()
        val insert = doc.createElement("em").text("foo")
        p!!.childNode(1).replaceWith(insert)
        assertEquals("One <em>foo</em> three", p.html())
    }

    @Test
    fun ownerDocument() {
        val doc = Ksoup.parse("<p>Hello")
        val p = doc.select("p").first()
        assertSame(p!!.ownerDocument(), doc)
        assertSame(doc.ownerDocument(), doc)
        assertNull(doc.parent())
    }

    @Test
    fun root() {
        val doc = Ksoup.parse("<div><p>Hello")
        val p = doc.select("p").first()
        val root: Node = p!!.root()
        assertSame(doc, root)
        assertNull(root.parent())
        assertSame(doc.root(), doc)
        assertSame(doc.root(), assertNotNull(doc.ownerDocument()))
        val standAlone = Element(Tag.valueOf("p"), "")
        assertNull(standAlone.parent())
        assertSame(standAlone.root(), standAlone)
        assertNull(standAlone.ownerDocument())
    }

    @Test
    fun before() {
        val doc = Ksoup.parse("<p>One <b>two</b> three</p>")
        val newNode = Element(Tag.valueOf("em"), "")
        newNode.appendText("four")
        doc.select("b").first()!!.before(newNode)
        assertEquals("<p>One <em>four</em><b>two</b> three</p>", doc.body().html())
        doc.select("b").first()!!.before("<i>five</i>")
        assertEquals("<p>One <em>four</em><i>five</i><b>two</b> three</p>", doc.body().html())
    }

    @Test
    fun beforeShuffle() {
        // https://github.com/jhy/jsoup/issues/1898
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div>")
        val div = doc.select("div")[0]
        val ps = doc.select("p")
        val p1 = ps[0]
        val p2 = ps[1]
        val p3 = ps[2]
        p2.before(p1)
        p3.before(p2)
        // ^ should be no-ops, they are already before
        assertEquals("One Two Three", div.text())
        p2.before(p1)
        p1.before(p3)
        assertEquals("Three One Two", div.text())
    }

    @Test
    fun after() {
        val doc = Ksoup.parse("<p>One <b>two</b> three</p>")
        val newNode = Element(Tag.valueOf("em"), "")
        newNode.appendText("four")
        doc.select("b").first()!!.after(newNode)
        assertEquals("<p>One <b>two</b><em>four</em> three</p>", doc.body().html())
        doc.select("b").first()!!.after("<i>five</i>")
        assertEquals("<p>One <b>two</b><i>five</i><em>four</em> three</p>", doc.body().html())
    }

    @Test
    fun afterShuffle() {
        // https://github.com/jhy/jsoup/issues/1898
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div>")
        val div = doc.select("div")[0]
        val ps = doc.select("p")
        val p1 = ps[0]
        val p2 = ps[1]
        val p3 = ps[2]
        p1.after(p2)
        p2.after(p3)
        // ^ should be no-ops, they are already before
        assertEquals("One Two Three", div.text())
        p3.after(p1)
        p1.after(p2)
        assertEquals("Three One Two", div.text())
    }

    @Test
    fun unwrap() {
        val doc = Ksoup.parse("<div>One <span>Two <b>Three</b></span> Four</div>")
        val span = doc.select("span").first()
        val twoText = span!!.childNode(0)
        val node = span.unwrap()
        assertEquals(
            "<div>One Two <b>Three</b> Four</div>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.body().html())
        )
        assertTrue(node is TextNode)
        assertEquals("Two ", (node as TextNode?)!!.text())
        assertEquals(node, twoText)
        assertEquals(node.parent(), doc.select("div").first())
    }

    @Test
    fun unwrapNoChildren() {
        val doc = Ksoup.parse("<div>One <span></span> Two</div>")
        val span = doc.select("span").first()
        val node = span!!.unwrap()
        assertEquals("<div>One  Two</div>", com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.body().html()))
        assertNull(node)
    }

    @Test
    fun traverse() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        doc.select("div").first()!!.traverse(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                accum.append("<").append(node.nodeName()).append(">")
            }

            override fun tail(node: Node, depth: Int) {
                accum.append("</").append(node.nodeName()).append(">")
            }
        })
        assertEquals("<div><p><#text></#text></p></div>", accum.toString())
    }

    @Test
    fun forEachNode() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There</div><div id=1>Gone<p></div>")
        doc.forEachNode { node: Node ->
            if (node is TextNode) {
                val textNode = node
                if (textNode.text() == "There") {
                    textNode.text("There Now")
                    textNode.after("<p>Another")
                }
            } else if (node.attr("id") == "1") node.remove()
        }
        assertEquals(
            "<div><p>Hello</p></div><div>There Now<p>Another</p></div>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.body().html())
        )
    }

    @Test
    fun orphanNodeReturnsNullForSiblingElements() {
        val node: Node = Element(Tag.valueOf("p"), "")
        val el = Element(Tag.valueOf("p"), "")
        assertEquals(0, node.siblingIndex())
        assertEquals(0, node.siblingNodes().size)
        assertNull(node.previousSibling())
        assertNull(node.nextSibling())
        assertEquals(0, el.siblingElements().size)
        assertNull(el.previousElementSibling())
        assertNull(el.nextElementSibling())
    }

    @Test
    fun nodeIsNotASiblingOfItself() {
        val doc = Ksoup.parse("<div><p>One<p>Two<p>Three</div>")
        val p2 = doc.select("p")[1]
        assertEquals("Two", p2.text())
        val nodes = p2.siblingNodes()
        assertEquals(2, nodes.size)
        assertEquals("<p>One</p>", nodes[0].outerHtml())
        assertEquals("<p>Three</p>", nodes[1].outerHtml())
    }

    @Test
    fun childNodesCopy() {
        val doc = Ksoup.parse("<div id=1>Text 1 <p>One</p> Text 2 <p>Two<p>Three</div><div id=2>")
        val div1 = doc.select("#1").first()
        val div2 = doc.select("#2").first()
        val divChildren = div1!!.childNodesCopy()
        assertEquals(5, divChildren.size)
        val tn1 = div1.childNode(0) as TextNode
        val tn2 = divChildren[0] as TextNode
        tn2.text("Text 1 updated")
        assertEquals("Text 1 ", tn1.text())
        div2!!.insertChildren(-1, divChildren)
        assertEquals(
            "<div id=\"1\">Text 1 <p>One</p> Text 2 <p>Two</p><p>Three</p></div><div id=\"2\">Text 1 updated"
                    + "<p>One</p> Text 2 <p>Two</p><p>Three</p></div>",
            com.fleeksoft.ksoup.TextUtil.stripNewlines(doc.body().html())
        )
    }

    @Test
    fun supportsClone() {
        val doc: Document = Ksoup.parse("<div class=foo>Text</div>")
        val el: Element = doc.select("div").first()!!
        assertTrue(el.hasClass("foo"))
        val elClone: Element = doc.clone().select("div").first()!!
        assertTrue(elClone.hasClass("foo"))
        assertEquals("Text", elClone.text())
        el.removeClass("foo")
        el.text("None")
        assertFalse(el.hasClass("foo"))
        assertTrue(elClone.hasClass("foo"))
        assertEquals("None", el.text())
        assertEquals("Text", elClone.text())
    }

    @Test
    fun changingAttributeValueShouldReplaceExistingAttributeCaseInsensitive() {
        val document = Ksoup.parse("<INPUT id=\"foo\" NAME=\"foo\" VALUE=\"\">")
        val inputElement = document.select("#foo").first()
        inputElement!!.attr("value", "bar")
        assertEquals(singletonAttributes(), getAttributesCaseInsensitive(inputElement))
    }

    private fun getAttributesCaseInsensitive(element: Element?): Attributes {
        val matches = Attributes()
        for (attribute in element!!.attributes()) {
            if (attribute.key.equals("value", ignoreCase = true)) {
                matches.put(attribute)
            }
        }
        return matches
    }

    private fun singletonAttributes(): Attributes {
        val attributes = Attributes()
        attributes.put("value", "bar")
        return attributes
    }

    @Test
    fun clonedNodesHaveOwnerDocsAndIndependentSettings() {
        // https://github.com/jhy/jsoup/issues/763
        val doc = Ksoup.parse("<div>Text</div><div>Two</div>")
        doc.outputSettings().prettyPrint(false)
        val div = doc.selectFirst("div")
        assertNotNull(div)
        val text = div.childNode(0) as TextNode
        assertNotNull(text)
        val textClone = text.clone()
        val docClone = textClone.ownerDocument()
        assertNotNull(docClone)
        assertFalse(docClone.outputSettings().prettyPrint())
        assertNotSame(doc, docClone)
        doc.outputSettings().prettyPrint(true)
        assertTrue(doc.outputSettings().prettyPrint())
        assertFalse(docClone.outputSettings().prettyPrint())
        assertEquals(
            1,
            docClone.childNodes().size
        ) // check did not get the second div as the owner's children
        assertEquals(textClone, docClone.childNode(0)) // note not the head or the body -- not normalized
    }

    @Test
    fun firstAndLastChild() {
        val html = "<div>One <span>Two</span> <a href></a> Three</div>"
        val doc = Ksoup.parse(html)
        val div = doc.selectFirst("div")
        val a = doc.selectFirst("a")
        assertNotNull(div)
        assertNotNull(a)

        // nodes
        val first = div.firstChild() as TextNode?
        assertEquals("One ", first!!.text())
        val last = div.lastChild() as TextNode?
        assertEquals(" Three", last!!.text())
        assertNull(a.firstChild())
        assertNull(a.lastChild())

        // elements
        val firstEl = div.firstElementChild()
        assertEquals("span", firstEl!!.tagName())
        val lastEl = div.lastElementChild()
        assertEquals("a", lastEl!!.tagName())
        assertNull(a.firstElementChild())
        assertNull(a.lastElementChild())
        assertNull(firstEl.firstElementChild())
        assertNull(firstEl.lastElementChild())
    }

    @Test
    fun nodeName() {
        val div = Element("DIV")
        assertEquals("DIV", div.tagName())
        assertEquals("DIV", div.nodeName())
        assertEquals("div", div.normalName())
        assertTrue(div.isNode("div"))
        assertTrue(Node.isNode(div, "div"))
        val text = TextNode("Some Text")
        assertEquals("#text", text.nodeName())
        assertEquals("#text", text.normalName())
    }
}
