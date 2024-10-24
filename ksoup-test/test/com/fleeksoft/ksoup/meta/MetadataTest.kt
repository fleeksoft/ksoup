package com.fleeksoft.ksoup.meta

import com.fleeksoft.io.byteInputStream
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.model.MetaData
import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataTest {
    val html = """
            <html>
                <head>
                    <title>Test Page</title>
                    <meta property="og:title" content="Test OG Title">
                    <meta property="og:description" content="Test OG Description">
                    <meta property="og:image" content="https://example.com/image.png">
                    <meta property="og:url" content="https://example.com">
                    <meta name="twitter:title" content="Test Twitter Title">
                    <meta name="twitter:description" content="Test Twitter Description">
                    <meta name="twitter:image" content="https://example.com/twitter_image.png">
                    <meta name="description" content="Test Description">
                    <link rel="canonical" href="https://example.com">
                    <link rel="icon" href="/favicon.ico">
                </head>
            </html>
        """.trimIndent()

    @Test
    fun testParseMetaDataFromString() {
        val metaData = Ksoup.parseMetaData(html, "https://example.com")

        assertMetaData(metaData)
    }

    @Test
    fun testParseMetaDataFromSourceReader() {
        val metaData = Ksoup.parseMetaData(html.byteInputStream(), "https://example.com")

        assertMetaData(metaData)
    }


    @Test
    fun testParseMetaDataFromElement() {
        val doc = Ksoup.parse(html, "https://example.com")
        val metaData = Ksoup.parseMetaData(doc)

        assertMetaData(metaData)
    }

    private fun assertMetaData(metaData: MetaData) {
        assertEquals("Test Page", metaData.htmlTitle)
        assertEquals("Test OG Title", metaData.ogTitle)
        assertEquals("Test OG Description", metaData.ogDescription)
        assertEquals("https://example.com/image.png", metaData.ogImage)
        assertEquals("https://example.com", metaData.ogUrl)
        assertEquals("Test Twitter Title", metaData.twitterTitle)
        assertEquals("Test Twitter Description", metaData.twitterDescription)
        assertEquals("https://example.com/twitter_image.png", metaData.twitterImage)
        assertEquals("Test Description", metaData.description)
        assertEquals("https://example.com", metaData.canonical)
        assertEquals("https://example.com/favicon.ico", metaData.favicon)
    }

}