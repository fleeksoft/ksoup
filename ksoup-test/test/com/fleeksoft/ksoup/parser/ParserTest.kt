package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.openBufferReader
import com.fleeksoft.ksoup.ported.toByteArray
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @BeforeTest
    fun initKsoup() {
        TestHelper.initKsoup()
    }

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
                bufferReader = "<p>H\u00E9llo, w\u00F6rld!".toByteArray(Charsets.UTF8).openBufferReader(),
                baseUri = "",
                charsetName = null,
            )
        val text = parsed.selectFirst("p")?.wholeText()
        assertEquals("H\u00E9llo, w\u00F6rld!", text)
    }
}
