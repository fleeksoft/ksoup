package com.fleeksoft.ksoup.parser

import com.fleeksoft.io.byteInputStream
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val parsed: Document =
            Ksoup.parse(
                input = "<p>H\u00E9llo, w\u00F6rld!".byteInputStream(),
                baseUri = "",
                charsetName = null,
            )
        val text = parsed.selectFirst("p")?.wholeText()
        assertEquals("H\u00E9llo, w\u00F6rld!", text)
    }
}
