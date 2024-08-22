package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TestHelper
import kotlin.test.*

class NodeIteratorTest {

    var html: String = "<div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div>"

    @Test
    fun canIterateNodes() {
        val doc: Document = Ksoup.parse(html)
        val it = NodeIterator.from(doc)
        assertIterates(it, "#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;")
        // todo - need to review that the Document object #root holds the html element as child. Why not have document root == html element?
        assertFalse(it.hasNext())

        var threw = false
        try {
            it.next()
        } catch (e: NoSuchElementException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun hasNextIsPure() {
        val doc: Document = Ksoup.parse(html)
        val it = NodeIterator.from(doc)
        assertTrue(it.hasNext())
        assertTrue(it.hasNext())
        assertIterates(it, "#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;")
        assertFalse(it.hasNext())
    }

    @Test
    fun iterateSubTree() {
        val doc: Document = Ksoup.parse(html)

        val div1 = doc.expectFirst("div#1")
        val it = NodeIterator.from(div1)
        assertIterates(it, "div#1;p;One;p;Two;")
        assertFalse(it.hasNext())

        val div2 = doc.expectFirst("div#2")
        val it2 = NodeIterator.from(div2)
        assertIterates(it2, "div#2;p;Three;p;Four;")
        assertFalse(it2.hasNext())
    }

    @Test
    fun canRestart() {
        val doc: Document = Ksoup.parse(html)

        val it = NodeIterator.from(doc)
        assertIterates(it, "#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;")

        it.restart(doc.expectFirst("div#2"))
        assertIterates(it, "div#2;p;Three;p;Four;")
    }

    @Test
    fun canIterateJustOneSibling() {
        val doc: Document = Ksoup.parse(html)
        val p2 = doc.expectFirst("p:contains(Two)")
        assertEquals("Two", p2.text())

        val it = NodeIterator.from(p2)
        assertIterates(it, "p;Two;")

        val elIt: NodeIterator<Element> = NodeIterator(p2, Element::class)
        val found = elIt.next()
        assertSame(p2, found)
        assertFalse(elIt.hasNext())
    }

    @Test
    fun canIterateFirstEmptySibling() {
        val doc: Document = Ksoup.parse("<div><p id=1></p><p id=2>.</p><p id=3>..</p>")
        val p1 = doc.expectFirst("p#1")
        assertEquals("", p1.ownText())

        val it = NodeIterator.from(p1)
        assertTrue(it.hasNext())
        val node = it.next()
        assertSame(p1, node)
        assertFalse(it.hasNext())
    }

    @Test
    fun canRemoveViaIterator() {
        val html = "<div id=out1><div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div></div><div id=out2>Out2"
        val doc: Document = Ksoup.parse(html)

        var it = NodeIterator.from(doc)
        var seen: StringBuilder = StringBuilder()
        while (it.hasNext()) {
            val node = it.next()
            if (node.attr("id").equals("1")) it.remove()
            trackSeen(node, seen)
        }
        assertEquals("#root;html;head;body;div#out1;div#1;div#2;p;Three;p;Four;div#out2;Out2;", seen.toString())
        assertContents(doc, "#root;html;head;body;div#out1;div#2;p;Three;p;Four;div#out2;Out2;")

        it = NodeIterator.from(doc)
        seen = StringBuilder()
        while (it.hasNext()) {
            val node = it.next()
            if (node.attr("id").equals("2")) it.remove()
            trackSeen(node, seen)
        }
        assertEquals("#root;html;head;body;div#out1;div#2;div#out2;Out2;", seen.toString())
        assertContents(doc, "#root;html;head;body;div#out1;div#out2;Out2;")
    }

    @Test
    fun canRemoveViaNode() {
        val html = "<div id=out1><div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div></div><div id=out2>Out2"
        val doc: Document = Ksoup.parse(html)

        var it = NodeIterator.from(doc)
        var seen: StringBuilder = StringBuilder()
        while (it.hasNext()) {
            val node = it.next()
            if (node.attr("id").equals("1")) node.remove()
            trackSeen(node, seen)
        }
        assertEquals("#root;html;head;body;div#out1;div#1;div#2;p;Three;p;Four;div#out2;Out2;", seen.toString())
        assertContents(doc, "#root;html;head;body;div#out1;div#2;p;Three;p;Four;div#out2;Out2;")

        it = NodeIterator.from(doc)
        seen = StringBuilder()
        while (it.hasNext()) {
            val node = it.next()
            if (node.attr("id").equals("2")) node.remove()
            trackSeen(node, seen)
        }
        assertEquals("#root;html;head;body;div#out1;div#2;div#out2;Out2;", seen.toString())
        assertContents(doc, "#root;html;head;body;div#out1;div#out2;Out2;")
    }

    @Test
    fun canReplace() {
        val html = "<div id=out1><div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div></div><div id=out2>Out2"
        val doc: Document = Ksoup.parse(html)

        var it = NodeIterator.from(doc)
        var seen: StringBuilder = StringBuilder()
        while (it.hasNext()) {
            val node = it.next()
            trackSeen(node, seen)
            if (node.attr("id").equals("1")) {
                node.replaceWith(Element("span").text("Foo"))
            }
        }
        assertEquals(
            "#root;html;head;body;div#out1;div#1;span;Foo;div#2;p;Three;p;Four;div#out2;Out2;",
            seen.toString(),
        )
        // ^^ we don't see <p>One, do see the replaced in <span>, and the subsequent nodes
        assertContents(doc, "#root;html;head;body;div#out1;span;Foo;div#2;p;Three;p;Four;div#out2;Out2;")

        it = NodeIterator.from(doc)
        seen = StringBuilder()
        while (it.hasNext()) {
            val node = it.next()
            trackSeen(node, seen)
            if (node.attr("id").equals("2")) {
                node.replaceWith(Element("span").text("Bar"))
            }
        }
        assertEquals("#root;html;head;body;div#out1;span;Foo;div#2;span;Bar;div#out2;Out2;", seen.toString())
        assertContents(doc, "#root;html;head;body;div#out1;span;Foo;span;Bar;div#out2;Out2;")
    }

    @Test
    fun canWrap() {
        val doc: Document = Ksoup.parse(html)
        val it = NodeIterator.from(doc)
        var sawInner = false
        while (it.hasNext()) {
            val node = it.next()
            if (node.attr("id").equals("1")) {
                node.wrap("<div id=outer>")
            }
            if (node is TextNode && node.text().equals("One")) sawInner = true
        }
        assertContents(doc, "#root;html;head;body;div#outer;div#1;p;One;p;Two;div#2;p;Three;p;Four;")
        assertTrue(sawInner)
    }

    @Test
    fun canFilterForElements() {
        val doc: Document = Ksoup.parse(html)
        val it: NodeIterator<Element> = NodeIterator(doc, Element::class)

        val seen = StringBuilder()
        while (it.hasNext()) {
            val el = it.next()
            assertNotNull(el)
            trackSeen(el, seen)
        }

        assertEquals("#root;html;head;body;div#1;p;p;div#2;p;p;", seen.toString())
    }

    @Test
    fun canFilterForTextNodes() {
        val doc: Document = Ksoup.parse(html)
        val it: NodeIterator<TextNode> = NodeIterator(doc, TextNode::class)

        val seen: StringBuilder = StringBuilder()
        while (it.hasNext()) {
            val text = it.next()
            assertNotNull(text)
            trackSeen(text, seen)
        }

        assertEquals("One;Two;Three;Four;", seen.toString())
        assertContents(doc, "#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;")
    }

    @Test
    fun canModifyFilteredElements() {
        val doc: Document = Ksoup.parse(html)
        val it: NodeIterator<Element> = NodeIterator(doc, Element::class)

        val seen = StringBuilder()
        while (it.hasNext()) {
            val el = it.next()
            if (el.ownText().isNotEmpty()) el.text(el.ownText() + "++")
            trackSeen(el, seen)
        }

        assertEquals("#root;html;head;body;div#1;p;p;div#2;p;p;", seen.toString())
        assertContents(doc, "#root;html;head;body;div#1;p;One++;p;Two++;div#2;p;Three++;p;Four++;")
    }

    companion object {
        fun <T : Node> assertIterates(
            it: NodeIterator<T>,
            expected: String?,
        ) {
            var previous: Node? = null
            val actual = StringBuilder()
            while (it.hasNext()) {
                val node: Node = it.next()
                assertNotNull(node)
                assertNotSame(previous, node)

                trackSeen(node, actual)
                previous = node
            }
            assertEquals(expected, actual.toString())
        }

        fun assertContents(
            el: Element?,
            expected: String?,
        ) {
            val it = NodeIterator.from(el!!)
            assertIterates(it, expected)
        }

        fun trackSeen(
            node: Node,
            actual: StringBuilder,
        ) {
            if (node is Element) {
                val el = node
                actual.append(el.tagName())
                if (el.hasAttr("id")) actual.append("#").append(el.id())
            } else if (node is TextNode) {
                actual.append(node.text())
            } else {
                actual.append(node.nodeName())
            }
            actual.append(";")
        }
    }
}
