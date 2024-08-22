package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TextUtil
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import com.fleeksoft.ksoup.ported.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TraversorTest {

    // Note: NodeTraversor.traverse(new NodeVisitor) is tested in
    // ElementsTest#traverse()
    @Test
    fun filterVisit() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        NodeTraversor.filter(
            object : NodeFilter {
                override fun head(
                    node: Node,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    accum.append("<").append(node.nodeName()).append(">")
                    return NodeFilter.FilterResult.CONTINUE
                }

                override fun tail(
                    node: Node?,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    accum.append("</").append(node!!.nodeName()).append(">")
                    return NodeFilter.FilterResult.CONTINUE
                }
            },
            doc.select("div"),
        )
        assertEquals("<div><p><#text></#text></p></div><div><#text></#text></div>", accum.toString())
    }

    @Test
    fun filterSkipChildren() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        NodeTraversor.filter(
            object : NodeFilter {
                override fun head(
                    node: Node,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    accum.append("<").append(node.nodeName()).append(">")
                    // OMIT contents of p:
                    return if ("p" == node.nodeName()) NodeFilter.FilterResult.SKIP_CHILDREN else NodeFilter.FilterResult.CONTINUE
                }

                override fun tail(
                    node: Node?,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    accum.append("</").append(node!!.nodeName()).append(">")
                    return NodeFilter.FilterResult.CONTINUE
                }
            },
            doc.select("div"),
        )
        assertEquals("<div><p></p></div><div><#text></#text></div>", accum.toString())
    }

    @Test
    fun filterSkipEntirely() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        NodeTraversor.filter(
            object : NodeFilter {
                override fun head(
                    node: Node,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    // OMIT p:
                    if ("p" == node.nodeName()) return NodeFilter.FilterResult.SKIP_ENTIRELY
                    accum.append("<").append(node.nodeName()).append(">")
                    return NodeFilter.FilterResult.CONTINUE
                }

                override fun tail(
                    node: Node?,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    accum.append("</").append(node!!.nodeName()).append(">")
                    return NodeFilter.FilterResult.CONTINUE
                }
            },
            doc.select("div"),
        )
        assertEquals("<div></div><div><#text></#text></div>", accum.toString())
    }

    @Test
    fun filterRemove() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There be <b>bold</b></div>")
        NodeTraversor.filter(
            object : NodeFilter {
                override fun head(
                    node: Node,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    // Delete "p" in head:
                    return if ("p" == node.nodeName()) NodeFilter.FilterResult.REMOVE else NodeFilter.FilterResult.CONTINUE
                }

                override fun tail(
                    node: Node?,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    // Delete "b" in tail:
                    return if ("b" == node!!.nodeName()) NodeFilter.FilterResult.REMOVE else NodeFilter.FilterResult.CONTINUE
                }
            },
            doc.select("div"),
        )
        assertEquals("<div></div>\n<div>\n There be\n</div>", doc.select("body").html())
    }

    @Test
    fun filterStop() {
        val doc = Ksoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        NodeTraversor.filter(
            object : NodeFilter {
                override fun head(
                    node: Node,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    accum.append("<").append(node.nodeName()).append(">")
                    return NodeFilter.FilterResult.CONTINUE
                }

                override fun tail(
                    node: Node?,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    accum.append("</").append(node!!.nodeName()).append(">")
                    // Stop after p.
                    return if ("p" == node.nodeName()) NodeFilter.FilterResult.STOP else NodeFilter.FilterResult.CONTINUE
                }
            },
            doc.select("div"),
        )
        assertEquals("<div><p><#text></#text></p>", accum.toString())
    }

    @Test
    fun replaceElement() {
        // test we can replace an element during traversal
        val html = "<div><p>One <i>two</i> <i>three</i> four.</p></div>"
        val doc = Ksoup.parse(html)
        NodeTraversor.traverse({ node, depth ->
            if (node is Element) {
                if (node.nameIs("i")) {
                    val u = Element("u").insertChildren(0, node.childNodes())
                    node.replaceWith(u)
                }
            }
        }, doc)
        val p = doc.selectFirst("p")
        assertNotNull(p)
        assertEquals("<p>One <u>two</u> <u>three</u> four.</p>", p.outerHtml())
    }

    @Test
    fun canAddChildren() {
        val doc = Ksoup.parse("<div><p></p><p></p></div>")
        NodeTraversor.traverse(
            object : NodeVisitor {
                var i = 0

                override fun head(
                    node: Node,
                    depth: Int,
                ) {
                    if (node.nodeName() == "p") {
                        val p = node as Element
                        p.append("<span>" + i++ + "</span>")
                    }
                }

                override fun tail(
                    node: Node,
                    depth: Int,
                ) {
                    if (node.nodeName() == "p") {
                        val p = node as Element
                        p.append("<span>" + i++ + "</span>")
                    }
                }
            },
            doc,
        )
        assertEquals(
            """<div>
 <p><span>0</span><span>1</span></p>
 <p><span>2</span><span>3</span></p>
</div>""",
            doc.body().html(),
        )
    }

    @Test
    fun canSpecifyOnlyHead() {
        // really, a compilation test - works as a lambda if just head
        val doc = Ksoup.parse("<div><p>One</p></div>")
        val count = intArrayOf(0)
        NodeTraversor.traverse({ node, depth -> count[0]++ }, doc)
        assertEquals(7, count[0])
    }

    @Test
    fun canRemoveDuringHead() {
        val doc = Ksoup.parse("<div><p id=1>Zero<p id=1>One<p id=2>Two<p>Three</div>")
        NodeTraversor.traverse(
            { node, depth ->
                if (node.attr("id") == "1") {
                    node.remove()
                } else if (node is TextNode && node.text() == "Three") {
                    node.remove()
                }
            },
            doc,
        )
        assertEquals("<div><p id=\"2\">Two</p><p></p></div>", TextUtil.stripNewlines(doc.body().html()))
    }

    @Test
    fun elementFunctionalTraverse() {
        val doc: Document = Ksoup.parse("<div><p>1<p>2<p>3")
        val body = doc.body()

        var seenCount: Int = 0
        var deepest: Int = 0
        body.traverse { node, depth ->
            ++seenCount
            if (depth > deepest) deepest = depth
        }

        assertEquals(8, seenCount) // body and contents
        assertEquals(3, deepest)
    }

    @Test
    fun seesDocRoot() {
        val doc: Document = Ksoup.parse("<p>One")
        val seen: AtomicBoolean = AtomicBoolean(false)
        doc.traverse { node, depth ->
            if (node == doc) seen.set(true)
        }
        assertTrue(seen.get())
    }
}
