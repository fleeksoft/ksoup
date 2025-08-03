package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.TextNode
import kotlin.test.Test
import kotlin.test.assertEquals


class NodesTest {
    @Test
    fun before() {
        val doc = Ksoup.parse("<span>One</span> <span>Two</span> <span>Three</span>")
        val nodes: Nodes<TextNode> = doc.selectNodes("::text:contains(o)", TextNode::class)
        nodes.before("<wbr>")
        assertEquals("<span><wbr>One</span> <span><wbr>Two</span> <span>Three</span>", doc.body().html())
    }

    @Test
    fun after() {
        val doc: Document = Ksoup.parse("<span>One</span> <span>Two</span> <span>Three</span>")
        val nodes: Nodes<TextNode> = doc.selectNodes("::text:contains(o)", TextNode::class)
        nodes.after("<wbr>")
        assertEquals("<span>One<wbr></span> <span>Two<wbr></span> <span>Three</span>", doc.body().html())
    }

    @Test
    fun wrap() {
        val doc: Document = Ksoup.parse("<span>One</span> <span>Two</span> <span>Three</span>")
        val nodes: Nodes<TextNode> = doc.selectNodes("::text:contains(o)", TextNode::class)
        nodes.wrap("<b></b>")
        assertEquals("<span><b>One</b></span> <span><b>Two</b></span> <span>Three</span>", doc.body().html())
    }
}
