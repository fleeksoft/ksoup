package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.NodeFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeafNodeTest {

    @Test
    fun doesNotGetAttributesTooEasily() {
        // test to make sure we're not setting attributes on all nodes right away
        val body = "<p>One <!-- Two --> Three<![CDATA[Four]]></p>"
        val doc = Ksoup.parse(body)
        assertTrue(hasAnyAttributes(doc)) // should have one - the base uri on the doc
        val html = doc.child(0)
        assertFalse(hasAnyAttributes(html))
        val s = doc.outerHtml()
        assertFalse(hasAnyAttributes(html))
        var els = doc.select("p")
        val p = els.first()
        assertEquals(1, els.size)
        assertFalse(hasAnyAttributes(html))
        els = doc.select("p.none")
        assertFalse(hasAnyAttributes(html))
        val id = p!!.id()
        assertEquals("", id)
        assertFalse(p.hasClass("Foobs"))
        assertFalse(hasAnyAttributes(html))
        p.addClass("Foobs")
        assertTrue(p.hasClass("Foobs"))
        assertTrue(hasAnyAttributes(html))
        assertTrue(hasAnyAttributes(p))
        val attributes = p.attributes()
        assertTrue(attributes.hasKey("class"))
        p.clearAttributes()
        assertFalse(hasAnyAttributes(p))
        assertFalse(hasAnyAttributes(html))
        assertFalse(attributes.hasKey("class"))
    }

    private fun hasAnyAttributes(node: Node?): Boolean {
        val found = BooleanArray(1)
        node!!.filter(
            object : NodeFilter {
                override fun head(
                    node: Node,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    return if (node.hasAttributes()) {
                        found[0] = true
                        NodeFilter.FilterResult.STOP
                    } else {
                        NodeFilter.FilterResult.CONTINUE
                    }
                }

                override fun tail(
                    node: Node?,
                    depth: Int,
                ): NodeFilter.FilterResult {
                    return NodeFilter.FilterResult.CONTINUE
                }
            },
        )
        return found[0]
    }
}
