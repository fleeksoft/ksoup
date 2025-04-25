package com.fleeksoft.ksoup.parser

import com.fleeksoft.io.byteInputStream
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parseInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

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
        assertEquals(htmlParser.settings()!!.preserveTagCase(), htmlClone.settings()!!.preserveTagCase())
        assertEquals(htmlParser.settings()!!.preserveAttributeCase(), htmlClone.settings()!!.preserveAttributeCase())

        // Test XML parser cloning
        val xmlParser = Parser.xmlParser()
        val xmlClone = xmlParser.clone()
        assertNotSame(xmlParser, xmlClone)
        assertNotSame(xmlParser.getTreeBuilder(), xmlClone.getTreeBuilder())
        assertEquals(xmlParser.settings()!!.preserveTagCase(), xmlClone.settings()!!.preserveTagCase())
        assertEquals(xmlParser.settings()!!.preserveAttributeCase(), xmlClone.settings()!!.preserveAttributeCase())
    }
}
