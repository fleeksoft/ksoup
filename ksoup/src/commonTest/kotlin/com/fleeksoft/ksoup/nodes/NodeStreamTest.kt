package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class NodeStreamTest {
    var html: String = "<div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div>"

    @Test
    fun canStream() {
        val doc: Document = Ksoup.parse(html)
        val seen = StringBuilder()
        val stream: Sequence<Node> = doc.nodeStream()
        stream.forEach { node: Node -> NodeIteratorTest.trackSeen(node, seen) }
        assertEquals("#root;html;head;body;div#1;p;One;p;Two;div#2;p;Three;p;Four;", seen.toString())
    }

    @Test
    fun canStreamParallel() {
        val doc: Document = Ksoup.parse(html)
//        parallel not supported in sequence
//        val count: Long = doc.nodeStream().parallel().count()
        val count: Int = doc.nodeStream().count()
        assertEquals(14, count)
    }

    @Test
    fun canFindFirst() {
        val doc: Document = Ksoup.parse(html)
        val first = doc.nodeStream().firstOrNull()
        assertNotNull(first)
        assertSame(doc, first)
    }

    @Test
    fun canFilter() {
        val doc: Document = Ksoup.parse(html)
        val seen = StringBuilder()

        doc.nodeStream()
            .filter { node -> node is TextNode }
            .forEach { node -> NodeIteratorTest.trackSeen(node, seen) }

        assertEquals("One;Two;Three;Four;", seen.toString())
    }

    @Test
    fun canRemove() {
        val html = "<div id=1><p>One<p>Two</div><div id=2><p>Three<p>Four</div><div id=3><p>Five"
        val doc: Document = Ksoup.parse(html)

        doc.nodeStream()
            .filter { node -> node is Element }
            .filter { node -> node.attr("id") == "1" || node.attr("id") == "2" }
            .forEach(Node::remove)

        NodeIteratorTest.assertContents(doc, "#root;html;head;body;div#3;p;Five;")
    }

    @Test
    fun elementStream() {
        val doc: Document = Ksoup.parse(html)
        val seen = StringBuilder()
        val stream: Sequence<Element> = doc.stream()
        stream.forEach { node: Element -> NodeIteratorTest.trackSeen(node, seen) }
        assertEquals("#root;html;head;body;div#1;p;p;div#2;p;p;", seen.toString())
    }
}
