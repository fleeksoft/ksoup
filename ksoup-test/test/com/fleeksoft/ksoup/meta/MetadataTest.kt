package com.fleeksoft.ksoup.meta

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.model.MetaData
import com.fleeksoft.ksoup.ported.openSourceReader
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
                    <link href="/pikacon-32x32.png" rel="shortcut icon" type="image/png">
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
        val sourceReader = html.openSourceReader()
        val metaData = Ksoup.parseMetaData(sourceReader, "https://example.com")

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
        assertEquals("https://example.com/pikacon-32x32.png", metaData.shortcutIcon)
    }


    fun testParseMetaDataFromReader() {
        val html = """
            <html>
            <head>
        <link rel="profile" href="http://gmpg.org/xfn/11">
    <link rel="alternate" href="https://animepahe.ru" hreflang="en-us">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="theme-color" content="#373a3c"><!-- Chrome, Firefox OS, Opera and Vivaldi -->
    <meta name="msapplication-navbutton-color" content="#373a3c"><!-- Windows Phone -->
    <meta name="apple-mobile-web-app-status-bar-style" content="#373a3c"><!-- iOS Safari -->
    <meta http-equiv="x-dns-prefetch-control" content="on">
    <link rel="preconnect" href="//i.animepahe.ru">
    <link rel="preload" href="/app/fonts/QldONTRRphEb_-V7LB6xTA.woff2" as="font" type="font/woff2" crossorigin>
    <link rel="preload" href="/app/css/bootstrap.min.css" as="style">
    <link rel="preload" href="/app/css/fork-awesome.min.css" as="style">
    <link rel="preload" href="/app/css/style.css" as="style">
    <link rel="preload" href="/app/js/vendor/bootstrap.bundle.min.js" as="script">
    <link rel="preload" href="/app/js/core.js" as="script">
    <meta name="msapplication-TileImage" content="https://animepahe.ru/animepahe-270x270.png">
    <title>animepahe :: okay-ish anime website</title>
    <meta name="description" content="Watch or download anime shows in HD 720p/1080p.">
    <meta name="keywords" content="Anime,Pahe,Mini,720p,HD,mp4,English,Subtitle,Hardsub">
    <meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1">
    <meta property="og:site_name" content="animepahe">
    <meta property="og:locale" content="en_US">
    <meta property="og:image" content="https://animepahe.ru/animepahe-270x270.png">
    <meta property="og:url" content="https://animepahe.ru">
    <meta property="og:type" content="website">
    <meta property="og:title" content="cloud anime encoding">
    <meta property="og:description" content="Watch or download anime shows in HD 720p/1080p.">
    <meta name="author" content="animepahe">
    <link href="/apple-touch-icon.png" rel="apple-touch-icon-precomposed" type="image/png">
    <link href="/apple-touch-icon.png" rel="shortcut icon">
    <link href="/pikacon-32x32.png" rel="shortcut icon" type="image/png">
    <link href="/pikacon.ico" rel="shortcut icon" type="image/x-icon">
    <link rel="alternate" type="application/rss+xml" title="RSS 2.0" href="https://animepahe.ru/feed">
    <link rel="stylesheet" href="/app/css/bootstrap.min.css">
    <link rel="stylesheet" href="/app/css/fork-awesome.min.css">
    <link rel="stylesheet" href="/app/css/style.css">
    </head>
    </html>
        """.trimIndent()

        val metaData = Ksoup.parseMetaData(html, "https://animepahe.ru/")
        assertEquals("animepahe :: okay-ish anime website", metaData.title)
        assertEquals("cloud anime encoding", metaData.ogTitle)
        assertEquals("https://animepahe.ru/animepahe-270x270.png", metaData.ogImage)
        assertEquals("https://animepahe.ru/apple-touch-icon.png", metaData.shortcutIcon)
    }

}