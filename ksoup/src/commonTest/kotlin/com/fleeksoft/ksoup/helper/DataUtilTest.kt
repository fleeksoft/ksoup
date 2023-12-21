package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.PlatformType
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
import com.fleeksoft.ksoup.ported.toBuffer
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import okio.Path.Companion.toPath
import kotlin.test.*

class DataUtilTest {
    @Test
    fun testCharset() {
        assertEquals("utf-8", DataUtil.getCharsetFromContentType("text/html;charset=utf-8 "))
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html; charset=UTF-8"))
        assertEquals(
            "ISO-8859-1",
            DataUtil.getCharsetFromContentType("text/html; charset=ISO-8859-1"),
        )
        assertNull(DataUtil.getCharsetFromContentType("text/html"))
        assertNull(DataUtil.getCharsetFromContentType(null))
        assertNull(DataUtil.getCharsetFromContentType("text/html;charset=Unknown"))
    }

    @Test
    fun testQuotedCharset() {
        assertEquals("utf-8", DataUtil.getCharsetFromContentType("text/html; charset=\"utf-8\""))
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html;charset=\"UTF-8\""))
        assertEquals(
            "ISO-8859-1",
            DataUtil.getCharsetFromContentType("text/html; charset=\"ISO-8859-1\""),
        )
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset=\"Unsupported\""))
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html; charset='UTF-8'"))
    }

    private fun bufferByteArrayCharset(data: String): BufferReader {
        return BufferReader(data.toByteArray())
    }

    private fun bufferByteArrayCharset(
        data: String,
        charset: String,
    ): BufferReader {
        return BufferReader(data.toByteArray(Charset.forName(charset)))
    }

    @Test
    fun discardsSpuriousByteOrderMark() {
        val html = "\uFEFF<html><head><title>One</title></head><body>Two</body></html>"
        val doc: Document =
            DataUtil.parseInputSource(
                this.bufferByteArrayCharset(html),
                "UTF-8",
                "http://foo.com/",
                Parser.htmlParser(),
            )
        assertEquals("One", doc.head().text())
    }

    @Test
    fun discardsSpuriousByteOrderMarkWhenNoCharsetSet() {
        val html = "\uFEFF<html><head><title>One</title></head><body>Two</body></html>"
        val doc: Document =
            DataUtil.parseInputSource(
                this.bufferByteArrayCharset(html),
                null,
                "http://foo.com/",
                Parser.htmlParser(),
            )
        assertEquals("One", doc.head().text())
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }

    @Test
    fun shouldNotThrowExceptionOnEmptyCharset() {
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset="))
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset=;"))
    }

    @Test
    fun shouldSelectFirstCharsetOnWeirdMultileCharsetsInMetaTags() {
        assertEquals(
            "ISO-8859-1",
            DataUtil.getCharsetFromContentType("text/html; charset=ISO-8859-1, charset=1251"),
        )
    }

    @Test
    fun shouldCorrectCharsetForDuplicateCharsetString() {
        assertEquals(
            "iso-8859-1",
            DataUtil.getCharsetFromContentType("text/html; charset=charset=iso-8859-1"),
        )
    }

    @Test
    fun shouldReturnNullForIllegalCharsetNames() {
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset=\$HJKDF§$/("))
    }

    @Test
    fun generatesMimeBoundaries() {
        val m1 = DataUtil.mimeBoundary()
        val m2 = DataUtil.mimeBoundary()
        assertEquals(DataUtil.boundaryLength, m1.length)
        assertEquals(DataUtil.boundaryLength, m2.length)
        assertNotSame(m1, m2)
    }

    @Test
    fun wrongMetaCharsetFallback() {
        val html = "<html><head><meta charset=iso-8></head><body></body></html>"
        val doc: Document =
            DataUtil.parseInputSource(
                this.bufferByteArrayCharset(html),
                null,
                "http://example.com",
                Parser.htmlParser(),
            )
        val expected = """<html>
 <head>
  <meta charset="iso-8">
 </head>
 <body></body>
</html>"""
        assertEquals(expected, doc.toString())
    }

    @Test
    @Throws(Exception::class)
    fun secondMetaElementWithContentTypeContainsCharsetParameter() {
        if (Platform.current == PlatformType.JS || Platform.current == PlatformType.IOS || Platform.current == PlatformType.WINDOWS) {
            // FIXME: euc-kr charset not supported
            return
        }
        val html =
            "<html><head>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html\">" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=euc-kr\">" +
                "</head><body>한국어</body></html>"
        val doc: Document =
            DataUtil.parseInputSource(
                bufferByteArrayCharset(html, "euc-kr"),
                null,
                "http://example.com",
                Parser.htmlParser(),
            )
        assertEquals("한국어", doc.body().text())
    }

    @Test
    @Throws(Exception::class)
    fun firstMetaElementWithCharsetShouldBeUsedForDecoding() {
        if (Platform.current != PlatformType.JVM && !TestHelper.forceAllTestsRun) {
            return
        }
        val html =
            "<html><head>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=koi8-u\">" +
                "</head><body>Übergrößenträger</body></html>"
        val docByteArrayCharset: Document =
            DataUtil.parseInputSource(
                bufferByteArrayCharset(html, "iso-8859-1"),
                null,
                "http://example.com",
                Parser.htmlParser(),
            )

        assertEquals("Übergrößenträger", docByteArrayCharset.body().text())
    }

    @Test
    fun supportsBOMinFiles() {
        if (Platform.current == PlatformType.JS || Platform.current == PlatformType.IOS || Platform.current == PlatformType.WINDOWS) {
            // FIXME: utf-16 charset not supported
            return
        }
        // test files from http://www.i18nl10n.com/korean/utftest/
        var input = TestHelper.getResourceAbsolutePath("bomtests/bom_utf16be.html")
        var doc: Document =
            Ksoup.parseFile(file = input, baseUri = "http://example.com", charsetName = null)
        assertTrue(doc.title().contains("UTF-16BE"))
        assertTrue(doc.text().contains("가각갂갃간갅"))
        input = TestHelper.getResourceAbsolutePath("bomtests/bom_utf16le.html")
        doc = Ksoup.parseFile(file = input, baseUri = "http://example.com", charsetName = null)
        assertTrue(doc.title().contains("UTF-16LE"))
        assertTrue(doc.text().contains("가각갂갃간갅"))
        input = TestHelper.getResourceAbsolutePath("bomtests/bom_utf32be.html")
        doc = Ksoup.parseFile(file = input, baseUri = "http://example.com", charsetName = null)
        assertTrue(doc.title().contains("UTF-32BE"))
        assertTrue(doc.text().contains("가각갂갃간갅"))
        input = TestHelper.getResourceAbsolutePath("bomtests/bom_utf32le.html")
        doc = Ksoup.parseFile(file = input, baseUri = "http://example.com", charsetName = null)
        assertTrue(doc.title().contains("UTF-32LE"))
        assertTrue(doc.text().contains("가각갂갃간갅"))
    }

    @Test
    fun supportsUTF8BOM() {
        val input: String = TestHelper.getResourceAbsolutePath("bomtests/bom_utf8.html")
        val doc: Document = Ksoup.parseFile(input, "http://example.com", null)
        assertEquals("OK", doc.head().select("title").text())
    }

    @Test
    fun noExtraNULLBytes() {
        val b =
            "<html><head><meta charset=\"UTF-8\"></head><body><div><u>ü</u>ü</div></body></html>".toByteArray(
                Charsets.UTF_8,
            )
        val doc = Ksoup.parse(BufferReader(b), baseUri = "", charsetName = null)
        assertFalse(doc.outerHtml().contains("\u0000"))
    }

    @Test
    fun supportsZippedUTF8BOM() {
        if (Platform.current == PlatformType.WINDOWS) {
//            gzip not supported yet
            return
        }

        val input: String = TestHelper.getResourceAbsolutePath("bomtests/bom_utf8.html.gz")
        val doc: Document =
            Ksoup.parseFile(
                file = input,
                baseUri = "http://example.com",
                charsetName = null,
            )
        assertEquals("OK", doc.head().select("title").text())
        assertEquals(
            "There is a UTF8 BOM at the top (before the XML decl). If not read correctly, will look like a non-joining space.",
            doc.body().text(),
        )
    }

    @Test
    fun supportsXmlCharsetDeclaration() {
        if (Platform.current != PlatformType.JVM && !TestHelper.forceAllTestsRun) {
            return
        }
        val encoding = "iso-8859-1"
        val soup =
            BufferReader(
                (
                    "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>" +
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">Hellö Wörld!</html>"
                ).toByteArray(
                    Charset.forName(encoding),
                ),
            )
        val doc: Document = Ksoup.parse(soup, baseUri = "", charsetName = null)
        assertEquals("Hellö Wörld!", doc.body().text())
    }

    @Test
    fun lLoadsGzipFile() {
        if (Platform.current == PlatformType.WINDOWS) {
//            gzip not supported yet
            return
        }
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/gzip.html.gz")
        val doc: Document = Ksoup.parseFile(file = input, charsetName = null)
        doc.toString()
        assertEquals("Gzip test", doc.title())
        assertEquals("This is a gzipped HTML file.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun loadsZGzipFile() {
        if (Platform.current == PlatformType.WINDOWS) {
//            gzip not supported yet
            return
        }
        // compressed on win, with z suffix
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/gzip.html.z")
        val doc: Document = Ksoup.parseFile(file = input, charsetName = null)
        assertEquals("Gzip test", doc.title())
        assertEquals("This is a gzipped HTML file.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun handlesFakeGzipFile() {
        if (Platform.current == PlatformType.WINDOWS) {
//            gzip not supported yet
            return
        }
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/fake-gzip.html.gz")
        val doc: Document = Ksoup.parseFile(file = input, charsetName = null)
        assertEquals("This is not gzipped", doc.title())
        assertEquals("And should still be readable.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun handlesChunkedInputStream() {
        if (Platform.current == PlatformType.WINDOWS) {
//            gzip not supported yet
            return
        }
        val inputFile: String = TestHelper.getResourceAbsolutePath("htmltests/large.html.gz")
        val input: String = TestHelper.getFileAsString(inputFile.toPath())
//        val stream = VaryingBufferReader(BufferReader(input))
        val expected = Ksoup.parse(input, "https://example.com")
        val doc: Document = Ksoup.parse(BufferReader(input), baseUri = "https://example.com", charsetName = null)

        println("""docSize: ${doc.toString().length}, expectedSize: ${expected.toString().length}""")
        assertTrue(doc.hasSameValue(expected))
    }

    @Test
    fun handlesUnlimitedRead() {
        if (Platform.current == PlatformType.WINDOWS) {
//            gzip not supported yet
            return
        }
        val inputFile: String = TestHelper.getResourceAbsolutePath("htmltests/large.html.gz")
        val input: String = TestHelper.getFileAsString(inputFile.toPath())
        val byteBuffer: ByteArray = DataUtil.readToByteBuffer(BufferReader(input.toBuffer()), 0)
        val read = byteBuffer.decodeToString()
        assertEquals(input, read)
    }
}
