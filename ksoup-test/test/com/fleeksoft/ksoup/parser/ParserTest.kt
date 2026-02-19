package com.fleeksoft.ksoup.parser

import com.fleeksoft.io.byteInputStream
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parseInput
import kotlin.test.*

class ParserTest {

    @Test
    fun unescapeEntities() {
        val s = Parser.unescapeEntities("One &amp; Two", false)
        assertEquals("One & Two", s)
    }

    @Test
    fun unescapeEntitiesHandlesLargeInput() {
        val longBody = StringBuilder(500000)
        do {
            longBody.append("SomeNonEncodedInput")
        } while (longBody.length < 64 * 1024)
        val body = longBody.toString()
        assertEquals(body, Parser.unescapeEntities(body, false))
    }

    @Test
    fun unescapeTracksErrors() {
        val parser = Parser.htmlParser()
        parser.setTrackErrors(10)

        var s: String = parser.unescape("One &bogus; &amp; &gt Two", false)
        assertEquals("One &bogus; & > Two", s)
        val errors = parser.getErrors()
        assertEquals(2, errors.size)
        assertEquals("<1:6>: Invalid character reference: invalid named reference [bogus]", errors[0].toString())
        assertEquals("<1:22>: Invalid character reference: missing semicolon on [&gt]", errors[1].toString())

        // can reuse parser; errors will be reset
        s = parser.unescape("One &amp; &bogus; Two", false)
        assertEquals("One & &bogus; Two", s)
        assertEquals(1, parser.getErrors().size)
        assertEquals("<1:12>: Invalid character reference: invalid named reference [bogus]", parser.getErrors()[0].toString())
    }

    @Test
    fun testUtf8() {
        val parsed: Document = Ksoup.parseInput(
            input = "<p>H\u00E9llo, w\u00F6rld!".byteInputStream(),
            baseUri = "",
            charsetName = null,
        )
        val text = parsed.selectFirst("p")?.wholeText()
        assertEquals("H\u00E9llo, w\u00F6rld!", text)
    }

    @Test
    fun testClone() {
        // Test HTML parser cloning
        val htmlParser = Parser.htmlParser()
        val htmlClone = htmlParser.clone()
        assertNotSame(htmlParser, htmlClone)
        // Ensure the tree builder instances are different
        assertNotSame(htmlParser.getTreeBuilder(), htmlClone.getTreeBuilder())
        // Check that settings are cloned properly (for example, tag case settings)
        assertEquals(htmlParser.settings().preserveTagCase(), htmlClone.settings().preserveTagCase())
        assertEquals(htmlParser.settings().preserveAttributeCase(), htmlClone.settings().preserveAttributeCase())

        // Test XML parser cloning
        val xmlParser = Parser.xmlParser()
        val xmlClone = xmlParser.clone()
        assertNotSame(xmlParser, xmlClone)
        assertNotSame(xmlParser.getTreeBuilder(), xmlClone.getTreeBuilder())
        assertEquals(xmlParser.settings().preserveTagCase(), xmlClone.settings().preserveTagCase())
        assertEquals(xmlParser.settings().preserveAttributeCase(), xmlClone.settings().preserveAttributeCase())
    }

    @Test
    fun testCloneCopyTagSet() {
        val parser = Parser.htmlParser()
        parser.tagSet().add(Tag("foo"))
        parser.tagSet().onNewTag { tag: Tag -> tag.set(Tag.SelfClose) }
        val clone = parser.clone()

        // Ensure the tagsets are different instances
        assertNotSame(clone.tagSet(), parser.tagSet())
        // Check that cloned tagset contains same tag
        assertNotNull(clone.tagSet().get("foo", Parser.NamespaceHtml))
        // Ensure onNewTag customizers are retained
        val custom: Tag = clone.tagSet().valueOf("qux", Parser.NamespaceHtml)
        assertTrue(custom.isSelfClosing())
        // Check that cloned tagset does not observe modifications made to the original
        assertNull(clone.tagSet().get("bar", Parser.NamespaceHtml))
        parser.tagSet().add(Tag("bar"))
        assertNull(clone.tagSet().get("bar", Parser.NamespaceHtml))
    }
}
