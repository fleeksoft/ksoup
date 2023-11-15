package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.ported.BufferReader
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
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
        // testcase for https://github.com/jhy/jsoup/issues/1557. no repro.
        val parsed: Document =
            parse(BufferReader("<p>H\u00E9llo, w\u00F6rld!".toByteArray(Charsets.UTF_8)), null, "")
        val text = parsed.selectFirst("p")?.wholeText()
        assertEquals("H\u00E9llo, w\u00F6rld!", text)
    }
}
