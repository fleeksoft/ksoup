package com.fleeksoft.ksoup.integration

import com.fleeksoft.io.byteInputStream
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration test: parses from real-world example HTML.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
class ParseTest {

    @Test
    fun testHtml5Charset() = runTest {
        if (!TestHelper.isGB2312Supported()) {
//            don't support gb2312 or gbk
            return@runTest
        }
        // test that <meta charset="gb2312"> works
        var doc = TestHelper.parseResource(
            "htmltests/meta-charset-1.html",
            baseUri = "http://example.com/"
        ) // gb2312, has html5 <meta charset>

        // FIXME: different name on different platforms
        assertContains(arrayOf("GBK", "GB2312"), doc.outputSettings().charset().name().uppercase())

        assertEquals("新", doc.text())

        // double check, no charset, falls back to utf8 which is incorrect
        doc = TestHelper.parseResource(
            "htmltests/meta-charset-2.html",
            baseUri = "http://example.com/"
        ) // gb2312, no charset

        assertEquals("UTF-8", doc.outputSettings().charset().name().uppercase())
        assertNotEquals("新", doc.text())

        // confirm fallback to utf8
        doc = TestHelper.parseResource(
            "htmltests/meta-charset-3.html",
            baseUri = "http://example.com/"
        ) // utf8, no charset

        assertEquals("UTF-8", doc.outputSettings().charset().name().uppercase())
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
            """.trimIndent().byteInputStream()

        val doc: Document = Ksoup.parse(input = input, baseUri = "http://example.com/", charsetName = null)
        assertEquals("UTF-8", doc.outputSettings().charset().name().uppercase())
    }

    @Test
    fun testLowercaseUtf8Charset() = runTest {
        val resourceName = "htmltests/lowercase-charset-test.html"
        val doc = TestHelper.parseResource(resourceName)
        val form = doc.select("#form").first()
        assertEquals(2, form!!.children().size)
        assertEquals("UTF-8", doc.outputSettings().charset().name().uppercase())
    }

    @Test
    fun testXwiki() = runTest {
        // this tests that when in CharacterReader we hit a buffer while marked, we preserve the mark when buffered up and can rewind
        val resourceName = "htmltests/xwiki-1324.html.gz"
        val doc: Document = if (!TestHelper.isGzipSupported()) {
            val input = TestHelper.readResource(resourceName)
            Ksoup.parse(input = input, baseUri = "https://localhost/")
        } else {
            Ksoup.parseFile(filePath = TestHelper.getResourceAbsolutePath(resourceName), baseUri = "https://localhost/")
        }
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())

        // was getting busted at =userdirectory, because it hit the bufferup point but the mark was then lost. so
        // updated to preserve the mark.
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;section=userdirectory\" title=\"Customize the user directory live table.\">User Directory</a>"
        assertEquals(wantHtml, doc.select("[data-id=userdirectory]").outerHtml())
    }

    @Test
    fun testXwikiExpanded() = runTest {
        // this tests that if there is a huge illegal character reference, we can get through a buffer and rewind, and still catch that it's an invalid refence,
        // and the parse tree is correct.
        val parser = Parser.htmlParser()
        val doc = Ksoup.parse(
            input = TestHelper.resourceFilePathToStream("htmltests/xwiki-edit.html.gz"),
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
        val html = TestHelper.readResourceAsString("htmltests/xwiki-edit.html.gz")
        val doc = Ksoup.parse(html)
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;RIGHTHERERIGHTHERERIGHTHERERIGHTHERE"
        assertTrue(doc.select("[data-id=userdirectory]").outerHtml().startsWith(wantHtml))
    }

    @Test
    fun testWikiFromString() = runTest {
        val html = TestHelper.readResourceAsString("htmltests/xwiki-1324.html.gz")
        val doc = Ksoup.parse(html)
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;section=userdirectory\" title=\"Customize the user directory live table.\">User Directory</a>"
        assertEquals(wantHtml, doc.select("[data-id=userdirectory]").outerHtml())
    }

    @Test
    fun testFileParseNoCharsetMethod() = runTest {
        val resourceName = "htmltests/xwiki-1324.html.gz"
        val doc: Document = if (!TestHelper.isGzipSupported()) {
            val source = TestHelper.readResource(resourceName)
            Ksoup.parse(input = source, baseUri = resourceName)
        } else {
            Ksoup.parseFile(filePath = TestHelper.getResourceAbsolutePath(resourceName))
        }
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
    }
}
