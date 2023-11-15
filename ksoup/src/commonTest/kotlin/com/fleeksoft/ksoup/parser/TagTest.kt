package com.fleeksoft.ksoup.parser

import kotlin.test.*

/**
 * Tag tests.
 * @author Sabeeh, fleeksoft@gmail.com
 */
class TagTest {
    @Test
    fun isCaseSensitive() {
        val p1 = Tag.valueOf("P")
        val p2 = Tag.valueOf("p")
        assertNotEquals(p1, p2)
    }

    @Test
    fun canBeInsensitive() {
        // TODO: multilocale test may move to jvm
//        Locale.setDefault(locale)
        val script1 = Tag.valueOf("script", ParseSettings.htmlDefault)
        val script2 = Tag.valueOf("SCRIPT", ParseSettings.htmlDefault)
        assertSame(script1, script2)
    }

    @Test
    fun trims() {
        val p1 = Tag.valueOf("p")
        val p2 = Tag.valueOf(" p ")
        assertEquals(p1, p2)
    }

    @Test
    fun equality() {
        val p1 = Tag.valueOf("p")
        val p2 = Tag.valueOf("p")
        assertEquals(p1, p2)
        assertSame(p1, p2)
    }

    @Test
    fun divSemantics() {
        val div = Tag.valueOf("div")
        assertTrue(div.isBlock)
        assertTrue(div.formatAsBlock())
    }

    @Test
    fun pSemantics() {
        val p = Tag.valueOf("p")
        assertTrue(p.isBlock)
        assertFalse(p.formatAsBlock())
    }

    @Test
    fun imgSemantics() {
        val img = Tag.valueOf("img")
        assertTrue(img.isInline())
        assertTrue(img.isSelfClosing())
        assertFalse(img.isBlock)
    }

    @Test
    fun defaultSemantics() {
        val foo = Tag.valueOf("FOO") // not defined
        val foo2 = Tag.valueOf("FOO")
        assertEquals(foo, foo2)
        assertTrue(foo.isInline())
        assertTrue(foo.formatAsBlock())
    }

    @Test
    fun valueOfChecksNotEmpty() {
        assertFailsWith<IllegalArgumentException> { Tag.valueOf(" ") }
    }

    @Test
    fun knownTags() {
        assertTrue(Tag.isKnownTag("div"))
        assertFalse(Tag.isKnownTag("explain"))
    }

    @Test
    fun knownSvgNamespace() {
        val svgHtml = Tag.valueOf("svg") // no namespace specified, defaults to html, so not the known tag
        val svg = Tag.valueOf("svg", Parser.NamespaceSvg, ParseSettings.htmlDefault)
        assertEquals(Parser.NamespaceHtml, svgHtml.namespace())
        assertEquals(Parser.NamespaceSvg, svg.namespace())
        assertFalse(svgHtml.isBlock) // generated
        assertTrue(svg.isBlock) // known
    }

    @Test
    fun unknownTagNamespace() {
        val fooHtml = Tag.valueOf("foo") // no namespace specified, defaults to html
        val foo = Tag.valueOf("foo", Parser.NamespaceSvg, ParseSettings.htmlDefault)
        assertEquals(Parser.NamespaceHtml, fooHtml.namespace())
        assertEquals(Parser.NamespaceSvg, foo.namespace())
        assertFalse(fooHtml.isBlock) // generated
        assertFalse(foo.isBlock) // generated
    }
}
