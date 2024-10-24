package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.ported.openSourceReader
import com.fleeksoft.charset.Charsets
import com.fleeksoft.charset.toByteArray
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
            parse(
                sourceReader = "<p>H\u00E9llo, w\u00F6rld!".toByteArray(Charsets.UTF8).openSourceReader(),
                baseUri = "",
                charsetName = null,
            )
        val text = parsed.selectFirst("p")?.wholeText()
        assertEquals("H\u00E9llo, w\u00F6rld!", text)
    }
}
