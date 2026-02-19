package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.TagSet
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
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
        assertSame(foo, tags.valueOf("FOO", Parser.NamespaceHtml, doc.parser()!!.settings()))
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

    @Test
    fun canCustomizeAll() {
        val tags = TagSet.Html()
        tags.onNewTag { tag -> tag.set(Tag.SelfClose) }
        assertTrue(tags.get("script", Parser.NamespaceHtml)!!.`is`(Tag.SelfClose))
        assertTrue(tags.valueOf("SCRIPT", Parser.NamespaceHtml).`is`(Tag.SelfClose))
        assertTrue(tags.valueOf("custom", Parser.NamespaceHtml).`is`(Tag.SelfClose))

        val foo = Tag("foo", Parser.NamespaceHtml)
        assertFalse(foo.`is`(Tag.SelfClose))
        tags.add(foo)
        assertTrue(foo.`is`(Tag.SelfClose))
    }

    @Test
    fun canCustomizeSome() {
        val tags = TagSet.Html()
        tags.onNewTag { tag ->
            if (!tag.isKnownTag()) {
                tag.set(Tag.SelfClose)
            }
        }
        assertFalse(tags.valueOf("script", Parser.NamespaceHtml).`is`(Tag.SelfClose))
        assertFalse(tags.valueOf("SCRIPT", Parser.NamespaceHtml).`is`(Tag.SelfClose))
        assertTrue(tags.valueOf("custom-tag", Parser.NamespaceHtml).`is`(Tag.SelfClose))
    }

    @Test
    fun canParseWithCustomization() {
        // really would use tag.valueOf("script"); just a test example here
        val parser = Parser.htmlParser()
        parser.tagSet().onNewTag { tag ->
            if (tag.normalName() == "script") tag.set(Tag.SelfClose)
        }

        val doc: Document = Ksoup.parse("<script />Text", parser)
        assertEquals("<html>\n <head>\n  <script></script>\n </head>\n <body>Text</body>\n</html>", doc.html())
        // self closing bit still produces valid HTML
    }

    @Test
    fun canParseWithGeneralCustomization() {
        val parser = Parser.htmlParser()
        parser.tagSet().onNewTag { tag ->
            if (!tag.isKnownTag()) tag.set(Tag.SelfClose)
        }

        val doc: Document = Ksoup.parse("<custom-data />Bar <script />Text", parser)
        assertEquals("<custom-data></custom-data>Bar\n<script>Text</script>", doc.body().html())
    }

    @Test
    fun supportsMultipleCustomizers() {
        val tags = TagSet.Html()
        tags.onNewTag { tag ->
            if (tag.normalName() == "script") tag.set(Tag.SelfClose)
        }
        tags.onNewTag { tag ->
            if (!tag.isKnownTag()) tag.set(Tag.RcData)
        }

        assertTrue(tags.valueOf("script", Parser.NamespaceHtml).`is`(Tag.SelfClose))
        assertFalse(tags.valueOf("script", Parser.NamespaceHtml).`is`(Tag.RcData))
        assertTrue(tags.valueOf("custom-tag", Parser.NamespaceHtml).`is`(Tag.RcData))
    }

    @Test
    fun customizersArePreservedInSource() {
        val source = TagSet.Html()
        source.onNewTag { tag -> tag.set(Tag.RcData) }
        val copy = TagSet(source)
        assertTrue(copy.valueOf("script", Parser.NamespaceHtml).`is`(Tag.RcData))
        assertTrue(source.valueOf("script", Parser.NamespaceHtml).`is`(Tag.RcData))

        copy.onNewTag { tag -> tag.set(Tag.Void) }
        assertTrue(copy.valueOf("custom-tag", Parser.NamespaceHtml).`is`(Tag.Void))
        assertFalse(source.valueOf("custom-tag", Parser.NamespaceHtml).`is`(Tag.Void))
    }

    @Test
    fun copyPullThroughDoesNotMutateSource() {
        val source: TagSet = TagSet.Html()
        val copy = TagSet(source)

        val sourceNamespacesBefore = tagSetNamespaceCount(source)
        assertNotNull(copy.get("div", Parser.NamespaceHtml))
        val sourceNamespacesAfter = tagSetNamespaceCount(source)
        assertEquals(sourceNamespacesBefore, sourceNamespacesAfter)
    }

    @Test
    fun copyPullWithCustomizerThroughDoesNotMutateSource() {
        val source: TagSet = TagSet.Html()
        val copy = TagSet(source)

        val sourceAdds: AtomicInt = atomic(0)
        source.onNewTag { tag: Tag -> sourceAdds.incrementAndGet() }

        assertNotNull(copy.get("div", Parser.NamespaceHtml))
        assertEquals(0, sourceAdds.value)
    }

    companion object {
        private fun tagSetNamespaceCount(tagSet: TagSet): Int {
            return tagSet.tags.size
        }
    }
}