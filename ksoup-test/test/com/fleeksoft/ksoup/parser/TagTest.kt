package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.TagSet
import kotlin.test.*


/**
 * Tag tests.
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
        val script1: Tag = Tag.valueOf("script", Parser.NamespaceHtml, ParseSettings.htmlDefault)
        val script2: Tag = Tag.valueOf("SCRIPT", Parser.NamespaceHtml, ParseSettings.htmlDefault)
        assertEquals(script1, script2)

        val htmlTags = TagSet.Html()
        val script3 = htmlTags.valueOf("script", Parser.NamespaceHtml, ParseSettings.htmlDefault)
        val script4 = htmlTags.valueOf("SCRIPT", Parser.NamespaceHtml, ParseSettings.htmlDefault)
        assertSame(script3, script4)
    }

    @Test
    fun trims() {
        val p1 = Tag.valueOf("p")
        val p2 = Tag.valueOf(" p ")
        assertEquals(p1, p2)
    }

    @Test
    fun equality() {
        val p1: Tag = Tag.valueOf("p")
        val p2: Tag = Tag.valueOf("p")
        assertEquals(p1, p2)
        assertNotSame(p1, p2) // not same because Tag.valueOf creates new clone of the TagSet.Html, so changes don't clobber all

        val html1 = TagSet.Html()
        val html2 = TagSet.Html()
        assertEquals(html1, html2)
        assertNotSame(html1, html2)

        val p3 = html1.valueOf("p", Parser.NamespaceHtml)
        val p4 = html1.valueOf("p", Parser.NamespaceHtml)
        val p5 = html2.valueOf("p", Parser.NamespaceHtml)
        val p6 = html2.valueOf("p", Parser.NamespaceHtml)
        assertEquals(p1, p3)
        assertEquals(p3, p4)
        assertEquals(p4, p5)
        assertSame(p3, p4)
        assertSame(p5, p6)
        assertNotSame(p3, p5)
    }

    @Test
    fun divSemantics() {
        val div = Tag.valueOf("div")

        assertTrue(div.isBlock())
        assertFalse(div.isInline())
        assertTrue(div.isKnownTag())
    }

    @Test
    fun pSemantics() {
        val p = Tag.valueOf("p")
        assertTrue(p.isKnownTag())
        assertTrue(p.isBlock())
        assertFalse(p.isInline())
    }

    @Test
    fun imgSemantics() {
        val img = Tag.valueOf("img")
        assertTrue(img.isInline())
        assertTrue(img.isSelfClosing())
        assertFalse(img.isBlock())
    }

    @Test
    fun defaultSemantics() {
        val foo = Tag.valueOf("FOO") // not defined
        val foo2 = Tag.valueOf("FOO")

        assertEquals(foo, foo2)
        assertFalse(foo.isKnownTag())
        assertTrue(foo.isInline())
        assertFalse(foo.isBlock())
        assertFalse(foo.`is`(Tag.InlineContainer))
        assertFalse(foo.preserveWhitespace())
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

        assertFalse(svgHtml.isKnownTag()) // generated
        assertTrue(svg.isKnownTag()) // known
    }

    @Test
    fun unknownTagNamespace() {
        val fooHtml = Tag.valueOf("foo") // no namespace specified, defaults to html
        val foo = Tag.valueOf("foo", Parser.NamespaceSvg, ParseSettings.htmlDefault)

        assertEquals(Parser.NamespaceHtml, fooHtml.namespace())
        assertEquals(Parser.NamespaceSvg, foo.namespace())

        assertFalse(fooHtml.isKnownTag()) // generated
        assertFalse(foo.isKnownTag()) // generated
    }

    @Test
    fun canSetOptions() {
        val tag = Tag("foo", Parser.NamespaceHtml)
        assertFalse(tag.isKnownTag())
        assertFalse(tag.isEmpty())
        tag.set(Tag.Void)
        assertTrue(tag.isEmpty())
        assertTrue(tag.isKnownTag())
    }

    @Test
    fun updateNameAndNamespace() {
        val tag = Tag("foo", Parser.NamespaceHtml)
        tag.name("bar").namespace(Parser.NamespaceSvg)
        tag.set(Tag.Block)
        assertEquals("bar", tag.name())
        assertEquals(Parser.NamespaceSvg, tag.namespace())
        assertTrue(tag.isBlock()) // properties are unchanged

        // test in a doc
        val doc = Ksoup.parse("<foo>One</foo><foo>Two</foo>")
        val foo = doc.expectFirst("foo").tag()
        foo.name("BAR")
        assertEquals("<BAR>One</BAR><BAR>Two</BAR>", doc.body().html()) // is case-sensitive
    }

    @Test
    fun formSubmittable() {
        // https://github.com/jhy/jsoup/issues/2323
        val img = Tag.valueOf("img")
        val input = Tag.valueOf("input")
        val imgOpts = img.options
        val inputOpts = input.options
        assertFalse(img.isFormSubmittable())
        assertTrue(input.isFormSubmittable())
        assertEquals(imgOpts, img.options)
        assertEquals(inputOpts, input.options)
    }

    @Test
    fun stableHashcode() {
        // tests that the hashcode is stable and suitable as a key
        val tags: HashSet<Tag?> = HashSet()
        val img = Tag.valueOf("img")
        val IMG = Tag.valueOf("IMG")
        val imgS = Tag.valueOf("img", Parser.NamespaceSvg, ParseSettings.htmlDefault)

        assertEquals(-2074969810, img.hashCode())
        assertEquals(-2075954866, IMG.hashCode())
        assertEquals(-292873947, imgS.hashCode())

        tags.add(img)
        tags.add(IMG)
        tags.add(imgS)

        imgS.set(Tag.Block)
        assertEquals(-292873947, imgS.hashCode())

        assertTrue(tags.contains(img))
        assertTrue(tags.contains(IMG))
        assertTrue(tags.contains(imgS))
    }

    @Test
    fun prefix() {
        val img = Tag.valueOf("img")
        val book = Tag.valueOf("bk:book")

        assertEquals("", img.prefix())
        assertEquals("bk", book.prefix())
    }

    @Test
    fun localname() {
        val img = Tag.valueOf("img")
        val book = Tag.valueOf("bk:book")

        assertEquals("img", img.localName())
        assertEquals("book", book.localName())
    }

    @Test
    fun valueOfWithSettings() {
        val img1 = Tag.valueOf("img", ParseSettings.htmlDefault)
        val img2: Tag? = Tag.valueOf("IMG", ParseSettings.htmlDefault)
        val img3 = Tag.valueOf("IMG", ParseSettings.preserveCase)

        assertNotSame(img1, img2) // because we are creating new TagSets with html()
        assertNotSame(img1, img3)
        assertEquals("IMG", img3.toString())
        assertEquals("img", img1.toString())

        val tagSet = TagSet.Html()
        assertSame(
            tagSet.valueOf("img", Parser.NamespaceHtml, ParseSettings.htmlDefault),
            tagSet.valueOf("IMG", Parser.NamespaceHtml, ParseSettings.htmlDefault)
        )

        assertNotSame(
            tagSet.valueOf("img", Parser.NamespaceHtml),
            tagSet.valueOf("IMG", Parser.NamespaceHtml)
        )
    }

    @Test
    fun equals() {
        val tags: TagSet = TagSet.Html()
        val p1: Tag = tags.get("p", Parser.NamespaceHtml)!!
        val p2: Tag = p1.clone()
        assertEquals(p1, p2)

        p2.namespace = "Other"
        assertNotEquals(p1, p2)
        p2.namespace = p1.namespace

        p2.tagName = "P"
        assertNotEquals(p1, p2)
        p2.tagName = p1.tagName

        p2.normalName = "pp"
        assertNotEquals(p1, p2)
        p2.normalName = p1.normalName

        p2.options = 0
        assertNotEquals(p1, p2)
        p2.options = p1.options
    }
}
