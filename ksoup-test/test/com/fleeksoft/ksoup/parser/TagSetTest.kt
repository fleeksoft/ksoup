package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.TagSet
import kotlin.test.*


class TagSetTest {

    @Test
    fun canRetrieveNewTagsSensitive() {
        val doc: Document =
            Ksoup.parse(html = "<div><p>One</p></div>", baseUri = "", parser = Parser.htmlParser().settings(ParseSettings.preserveCase))
        val tags = doc.parser()!!.tagSet()
        // should be the full html set
        val meta = tags.get("meta", Parser.NamespaceHtml)
        assertNotNull(meta)
        assertTrue(meta.isKnownTag())

        val p = doc.expectFirst("p")
        assertTrue(p.tag().isKnownTag())

        assertNull(tags.get("FOO", Parser.NamespaceHtml))
        p.tagName("FOO")
        val foo = p.tag()
        assertEquals("FOO", foo.name())
        assertEquals("foo", foo.normalName())
        assertEquals(Parser.NamespaceHtml, foo.namespace())
        assertFalse(foo.isKnownTag())

        assertSame(foo, tags.get("FOO", Parser.NamespaceHtml))
        assertSame(foo, tags.valueOf("FOO", Parser.NamespaceHtml))
        assertNull(tags.get("FOO", "SomeOtherNamespace"))
    }

    @Test
    fun canRetrieveNewTagsInsensitive() {
        val doc = Ksoup.parse("<div><p>One</p></div>")
        val tags = doc.parser()!!.tagSet()
        // should be the full html set
        val meta = tags.get("meta", Parser.NamespaceHtml)
        assertNotNull(meta)
        assertTrue(meta.isKnownTag())

        val p = doc.expectFirst("p")
        assertTrue(p.tag().isKnownTag())

        assertNull(tags.get("FOO", Parser.NamespaceHtml))
        p.tagName("FOO")
        val foo = p.tag()
        assertEquals("foo", foo.name())
        assertEquals("foo", foo.normalName())
        assertEquals(Parser.NamespaceHtml, foo.namespace())
        assertFalse(foo.isKnownTag())

        assertSame(foo, tags.get("foo", Parser.NamespaceHtml))
        assertSame(foo, tags.valueOf("FOO", Parser.NamespaceHtml, doc.parser()!!.settings()!!))
        assertNull(tags.get("foo", "SomeOtherNamespace"))
    }

    @Test
    fun supplyCustomTagSet() {
        val tags = TagSet.Html()
        tags.valueOf("custom", Parser.NamespaceHtml).set(Tag.PreserveWhitespace).set(Tag.Block)
        val parser = Parser.htmlParser().tagSet(tags)

        val doc = Ksoup.parse("<body><custom>\n\nFoo\n Bar</custom></body>", parser)
        val custom = doc.expectFirst("custom")
        assertTrue(custom.tag().preserveWhitespace())
        assertTrue(custom.tag().isBlock())
        assertEquals(
            "<custom>\n" +
                    "\n" +
                    "Foo\n" +
                    " Bar" +
                    "</custom>", custom.outerHtml()
        )
    }

    @Test
    fun knownTags() {
        // tests that tags explicitly inserted via .add are 'known'; those that come implicitly via valueOf are not
        val tags = TagSet.Html()
        val custom = Tag("custom")
        assertEquals("custom", custom.name())
        assertEquals(Parser.NamespaceHtml, custom.namespace())
        assertFalse(custom.isKnownTag()) // not yet

        val br = tags.get("br", Parser.NamespaceHtml)
        assertNotNull(br)
        assertTrue(br.isKnownTag())
        assertSame(br, tags.valueOf("br", Parser.NamespaceHtml))

        val foo = tags.valueOf("foo", Parser.NamespaceHtml)
        assertFalse(foo.isKnownTag())

        tags.add(custom)
        assertTrue(custom.isKnownTag())
        assertSame(custom, tags.get("custom", Parser.NamespaceHtml))
        assertSame(custom, tags.valueOf("custom", Parser.NamespaceHtml))
        val capCustom = tags.valueOf("Custom", Parser.NamespaceHtml)
        assertTrue(capCustom.isKnownTag()) // cloned from a known tag, so is still known

        // known if set or clear called
        val c1 = Tag("bar")
        assertFalse(c1.isKnownTag())
        c1.set(Tag.Block)
        assertTrue(c1.isKnownTag())
        c1.clear(Tag.Block)
        assertTrue(c1.isKnownTag())
        c1.clear(Tag.Known)
        assertFalse(c1.isKnownTag())
    }
}