package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import kotlin.test.*
import kotlin.test.Test

class CommentTest {
    private val comment = Comment(" This is one heck of a comment! ")
    private val decl = Comment("?xml encoding='ISO-8859-1'?")

    @Test
    fun nodeName() {
        assertEquals("#comment", comment.nodeName())
    }

    @Test
    fun getData() {
        assertEquals(" This is one heck of a comment! ", comment.getData())
    }

    @Test
    fun testToString() {
        assertEquals("<!-- This is one heck of a comment! -->", comment.toString())
        var doc = Ksoup.parse("<div><!-- comment--></div>")
        assertEquals("<div>\n <!-- comment-->\n</div>", doc.body().html())
        doc = Ksoup.parse("<p>One<!-- comment -->Two</p>")
        assertEquals("<p>One<!-- comment -->Two</p>", doc.body().html())
        assertEquals("OneTwo", doc.text())
    }

    @Test
    fun testHtmlNoPretty() {
        val doc = Ksoup.parse("<!-- a simple comment -->")
        doc.outputSettings().prettyPrint(false)
        assertEquals("<!-- a simple comment --><html><head></head><body></body></html>", doc.html())
        val node = doc.childNode(0)
        val c1 = node as Comment
        assertEquals("<!-- a simple comment -->", c1.outerHtml())
    }

    @Test
    fun stableIndentInBlock() {
        val html = "<div><!-- comment --> Text</div><p><!-- comment --> Text</p>"
        val doc = Ksoup.parse(html)
        val out = doc.body().html()
        assertEquals(
            """<div>
 <!-- comment --> Text
</div>
<p><!-- comment --> Text</p>""",
            out,
        )
        val doc2 = Ksoup.parse(out)
        val out2 = doc2.body().html()
        assertEquals(out, out2)
    }

    @Test
    fun testClone() {
        val c1 = comment.clone()
        assertNotSame(comment, c1)
        assertEquals(comment.getData(), comment.getData())
        c1.setData("New")
        assertEquals("New", c1.getData())
        assertNotEquals(c1.getData(), comment.getData())
    }

    @Test
    fun isXmlDeclaration() {
        assertFalse(comment.isXmlDeclaration())
        assertTrue(decl.isXmlDeclaration())
    }

    @Test
    fun asXmlDeclaration() {
        val xmlDeclaration = decl.asXmlDeclaration()
        assertNotNull(xmlDeclaration)
    }
}
