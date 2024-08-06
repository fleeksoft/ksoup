package com.fleeksoft.ksoup.integration

import com.fleeksoft.ksoup.*
import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.Ksoup.parseFile
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import korlibs.io.file.std.uniVfs
import korlibs.io.stream.openSync
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Integration test: parses from real-world example HTML.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class ParseTest {
    @Test
    fun testHtml5Charset() = runTest {
        if (Platform.isJS()) {
//            js resource access issue
            return@runTest
        }
        if (Platform.isApple()) {
//            apple don't support gb2312 or gbk
            return@runTest
        }
        // test that <meta charset="gb2312"> works
        var input = TestHelper.getResourceAbsolutePath("htmltests/meta-charset-1.html")
        var doc: Document =
            parseFile(
                filePath = input,
                baseUri = "http://example.com/",
                charsetName = null,
            ) // gb2312, has html5 <meta charset>
        if (Platform.isJS()) {
            // FIXME: on js it is returning GBK
            assertEquals("GBK", doc.outputSettings().charset().name.uppercase())
        } else {
            assertEquals("GB2312", doc.outputSettings().charset().name.uppercase())
        }

        assertEquals("新", doc.text())

        // double check, no charset, falls back to utf8 which is incorrect
        input = TestHelper.getResourceAbsolutePath("htmltests/meta-charset-2.html") //
        doc =
            parseFile(
                filePath = input,
                baseUri = "http://example.com",
                charsetName = null,
            ) // gb2312, no charset
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
        assertNotEquals("新", doc.text())

        // confirm fallback to utf8
        input = TestHelper.getResourceAbsolutePath("htmltests/meta-charset-3.html")
        doc =
            parseFile(
                filePath = input,
                baseUri = "http://example.com/",
                charsetName = null,
            ) // utf8, no charset
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
        assertEquals("新", doc.text())
    }

    @Test
    fun testBrokenHtml5CharsetWithASingleDoubleQuote() {
        val input =
            """
            <html>
            <head><meta charset=UTF-8"></head>
            <body></body>
            </html>
            """.trimIndent().openSync()

        val doc: Document = parse(syncStream = input, baseUri = "http://example.com/", charsetName = null)
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }

    @Test
    fun testLowercaseUtf8Charset() = runTest {
        if (Platform.isJS()) {
//            js resource access issue
            return@runTest
        }
        val input = TestHelper.getResourceAbsolutePath("htmltests/lowercase-charset-test.html")
        val doc: Document = parseFile(filePath = input, charsetName = null)
        val form = doc.select("#form").first()
        assertEquals(2, form!!.children().size)
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }

    @Test
    fun testXwiki() = runTest {
        if (Platform.isJS()) {
//            js resource access issue
            return@runTest
        }

        // this tests that when in CharacterReader we hit a buffer while marked, we preserve the mark when buffered up and can rewind
        val input = TestHelper.getResourceAbsolutePath("htmltests/xwiki-1324.html.gz")
        val doc: Document =
            parseFile(
                filePath = input,
                baseUri = "https://localhost/",
                charsetName = null,
            )
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())

        // was getting busted at =userdirectory, because it hit the bufferup point but the mark was then lost. so
        // updated to preserve the mark.
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;section=userdirectory\" title=\"Customize the user directory live table.\">User Directory</a>"
        assertEquals(wantHtml, doc.select("[data-id=userdirectory]").outerHtml())
    }

    @Test
    fun testXwikiExpanded() = runTest {
        if (Platform.isJS()) {
//            js resource access issue
            return@runTest
        }
        // this tests that if there is a huge illegal character reference, we can get through a buffer and rewind, and still catch that it's an invalid refence,
        // and the parse tree is correct.
        val parser = Parser.htmlParser()
        val doc =
            parse(
                syncStream = TestHelper.resourceFilePathToStream("htmltests/xwiki-edit.html.gz"),
                baseUri = "https://localhost/",
                charsetName = "UTF-8",
                parser = parser.setTrackErrors(100),
            )
        val errors = parser.getErrors()
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
        assertEquals(0, errors.size) // not an invalid reference because did not look legit

        // was getting busted at =userdirectory, because it hit the bufferup point but the mark was then lost. so
        // updated to preserve the mark.
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;RIGHTHERERIGHTHERERIGHTHERERIGHTHERE"
        assertTrue(doc.select("[data-id=userdirectory]").outerHtml().startsWith(wantHtml))
    }

    @Test
    fun testWikiExpandedFromString() = runTest {
        if (Platform.isJS()) {
//            js resource access issue
            return@runTest
        }
        val input = TestHelper.getResourceAbsolutePath("htmltests/xwiki-edit.html.gz")
        val html = TestHelper.getFileAsString(input.uniVfs)
        val doc = parse(html)
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;RIGHTHERERIGHTHERERIGHTHERERIGHTHERE"
        assertTrue(doc.select("[data-id=userdirectory]").outerHtml().startsWith(wantHtml))
    }

    @Test
    fun testWikiFromString() = runTest {
        if (Platform.isJS()) {
//            js resource access issue
            return@runTest
        }
        val input = TestHelper.getResourceAbsolutePath("htmltests/xwiki-1324.html.gz")
        val html = TestHelper.getFileAsString(input.uniVfs)
        val doc = parse(html)
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;section=userdirectory\" title=\"Customize the user directory live table.\">User Directory</a>"
        assertEquals(wantHtml, doc.select("[data-id=userdirectory]").outerHtml())
    }

    @Test
    fun testFileParseNoCharsetMethod() = runTest {
        if (Platform.isJS()) {
//            js resource access issue
            return@runTest
        }
        val file = TestHelper.getResourceAbsolutePath("htmltests/xwiki-1324.html.gz")
        val doc: Document = parseFile(file)
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
    }
}
