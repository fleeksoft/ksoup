package com.fleeksoft.ksoup.integration

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.Ksoup.parseFile
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
import com.fleeksoft.ksoup.readFile
import com.fleeksoft.ksoup.readGzipFile
import io.ktor.utils.io.charsets.name
import okio.Path
import okio.Path.Companion.toPath
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
    fun testHtml5Charset() {
        // test that <meta charset="gb2312"> works
        var `in` = getResourceAbsolutePath("htmltests/meta-charset-1.html")
        var doc: Document =
            parseFile(
                file = `in`,
                baseUri = "http://example.com/",
                charsetName = null
            ) //gb2312, has html5 <meta charset>
        assertEquals("新", doc.text())
        assertEquals("GB2312", doc.outputSettings().charset().name.uppercase())

        // double check, no charset, falls back to utf8 which is incorrect
        `in` = getResourceAbsolutePath("htmltests/meta-charset-2.html") //
        doc = parseFile(
            file = `in`,
            baseUri = "http://example.com",
            charsetName = null
        ) // gb2312, no charset
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
        assertNotEquals("新", doc.text())

        // confirm fallback to utf8
        `in` = getResourceAbsolutePath("htmltests/meta-charset-3.html")
        doc = parseFile(
            file = `in`,
            baseUri = "http://example.com/",
            charsetName = null
        ) // utf8, no charset
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
        assertEquals("新", doc.text())
    }

    @Test
    fun testBrokenHtml5CharsetWithASingleDoubleQuote() {
        val `in` = BufferReader(
            """
    <html>
    <head><meta charset=UTF-8"></head>
    <body></body>
    </html>
    """.trimIndent()
        )

        val doc: Document = parse(`in`, null, "http://example.com/")
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }

    @Test
    fun testLowercaseUtf8Charset() {
        val `in` = getResourceAbsolutePath("htmltests/lowercase-charset-test.html")
        val doc: Document = parseFile(`in`, null)
        val form = doc.select("#form").first()
        assertEquals(2, form!!.children().size)
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }

    @Test
    fun testXwiki() {
        // https://github.com/jhy/jsoup/issues/1324
        // this tests that when in CharacterReader we hit a buffer while marked, we preserve the mark when buffered up and can rewind
        val `in` = getResourceAbsolutePath("htmltests/xwiki-1324.html.gz")
        val doc: Document = parseFile(
            file = `in`,
            baseUri = "https://localhost/",
            charsetName = null
        )
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())

        // was getting busted at =userdirectory, because it hit the bufferup point but the mark was then lost. so
        // updated to preserve the mark.
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;section=userdirectory\" title=\"Customize the user directory live table.\">User Directory</a>"
        assertEquals(wantHtml, doc.select("[data-id=userdirectory]").outerHtml())
    }

    @Test
    fun testXwikiExpanded() {
        // https://github.com/jhy/jsoup/issues/1324
        // this tests that if there is a huge illegal character reference, we can get through a buffer and rewind, and still catch that it's an invalid refence,
        // and the parse tree is correct.

        val parser = Parser.htmlParser()
        val doc = parse(
            resourceFilePathToBufferReader("htmltests/xwiki-edit.html.gz"),
            "UTF-8",
            "https://localhost/",
            parser.setTrackErrors(100)
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
    fun testWikiExpandedFromString() {
        val `in` = getResourceAbsolutePath("htmltests/xwiki-edit.html.gz")
        val html = getFileAsString(`in`.toPath())
        val doc = parse(html)
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;RIGHTHERERIGHTHERERIGHTHERERIGHTHERE"
        assertTrue(doc.select("[data-id=userdirectory]").outerHtml().startsWith(wantHtml))
    }

    @Test
    fun testWikiFromString() {
        val `in` = getResourceAbsolutePath("htmltests/xwiki-1324.html.gz")
        val html = getFileAsString(`in`.toPath())
        val doc = parse(html)
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
        val wantHtml =
            "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;section=userdirectory\" title=\"Customize the user directory live table.\">User Directory</a>"
        assertEquals(wantHtml, doc.select("[data-id=userdirectory]").outerHtml())
    }

    @Test
    fun testFileParseNoCharsetMethod() {
        val file = getResourceAbsolutePath("htmltests/xwiki-1324.html.gz")
        val doc: Document = parseFile(file)
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text())
    }

    companion object {

        fun getResourceAbsolutePath(resourceName: String): String {
//            /Users/sabeeh/AndroidStudioProjects/ksoup/ksoup/src/commonTest/resources/
//            return "src/commonTest/resources/$resourceName".toPath()
            return "/Users/sabeeh/AndroidStudioProjects/ksoup/ksoup/src/commonTest/resources/$resourceName"
        }

        fun getFileAsString(file: Path): String {
            val bytes: ByteArray = if (file.name.endsWith(".gz")) {
                readGzipFile(file).readByteArray()
            } else {
                readFile(file).readByteArray()
            }
            return bytes.decodeToString()
        }

        fun resourceFilePathToBufferReader(path: String): BufferReader {
            val file = this.getResourceAbsolutePath(path)
            return pathToBufferReader(file.toPath())
        }

        fun pathToBufferReader(file: Path): BufferReader {
            return if (file.name.endsWith(".gz")) {
                BufferReader(readGzipFile(file).readByteArray())
            } else {
                BufferReader(readFile(file).readByteArray())
            }
        }
    }
}
