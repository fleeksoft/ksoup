package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.*
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.openSourceReader
import com.fleeksoft.ksoup.ported.toByteArray
import com.fleeksoft.ksoup.ported.toSourceFile
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.test.runTest
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

    private fun dataToStream(
        data: String,
        charset: String,
    ): SourceReader {
        return data.toByteArray(Charsets.forName(charset)).openSourceReader()
    }

    @Test
    fun discardsSpuriousByteOrderMark() {
        val html = "\uFEFF<html><head><title>One</title></head><body>Two</body></html>"
        val doc: Document = DataUtil.parseInputSource(
            sourceReader = html.openSourceReader(),
            baseUri = "http://foo.com/",
            charsetName = "UTF-8",
            parser = Parser.htmlParser(),
        )
        println("doc: $doc")
        assertEquals("One", doc.head().text())
    }

    @Test
    fun discardsSpuriousByteOrderMarkWhenNoCharsetSet() {
        val html = "\uFEFF<html><head><title>One</title></head><body>Two</body></html>"
        val doc: Document = DataUtil.parseInputSource(
            sourceReader = html.openSourceReader(),
            baseUri = "http://foo.com/",
            charsetName = null,
            parser = Parser.htmlParser(),
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
                sourceReader = html.openSourceReader(),
                baseUri = "http://example.com",
                charsetName = null,
                parser = Parser.htmlParser(),
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
    fun secondMetaElementWithContentTypeContainsCharsetParameter() {
        if (Platform.isJsOrWasm() || Platform.isApple() || Platform.isWindows()) {
            // FIXME: euc-kr charset not supported
            return
        }
        val html =
            "<html><head>" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html\">" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=euc-kr\">" +
                    "</head><body>한국어</body></html>"
        val doc: Document = DataUtil.parseInputSource(
            sourceReader = dataToStream(data = html, charset = "euc-kr"),
            baseUri = "http://example.com",
            charsetName = null,
            parser = Parser.htmlParser(),
        )
        assertEquals("한국어", doc.body().text())
    }

    @Test
    fun firstMetaElementWithCharsetShouldBeUsedForDecoding() {
        val html =
            "<html><head>" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=koi8-u\">" +
                    "</head><body>Übergrößenträger</body></html>"
        val docByteArrayCharset: Document =
            DataUtil.parseInputSource(
                sourceReader = dataToStream(data = html, charset = "iso-8859-1"),
                baseUri = "http://example.com",
                charsetName = null,
                parser = Parser.htmlParser(),
            )

        assertEquals("Übergrößenträger", docByteArrayCharset.body().text())
    }

    @Test
    fun supportsBOMinFiles() = runTest {
        if (BuildConfig.isKotlinx && Platform.isJsOrWasm()) {
            // FIXME: UTF-16 charset not supported
            return@runTest
        }
        var input = TestHelper.getResourceAbsolutePath("bomtests/bom_utf16be.html")
        var doc: Document =
            Ksoup.parseFile(filePath = input, baseUri = "http://example.com", charsetName = null)
        assertContains(doc.title(), "UTF-16BE")
        assertContains(doc.text(), "가각갂갃간갅")
        input = TestHelper.getResourceAbsolutePath("bomtests/bom_utf16le.html")
        doc = Ksoup.parseFile(filePath = input, baseUri = "http://example.com", charsetName = null)
        assertContains(doc.title(), "UTF-16LE")
        assertContains(doc.text(), "가각갂갃간갅")

        if (Platform.isJsOrWasm() || Platform.isWindows() || Platform.isLinux()) {
            // FIXME: UTF-32 charset not supported
            return@runTest
        }

        input = TestHelper.getResourceAbsolutePath("bomtests/bom_utf32be.html")
        doc = Ksoup.parseFile(filePath = input, baseUri = "http://example.com", charsetName = null)
        assertContains(doc.title(), "UTF-32BE")
        assertContains(doc.text(), "가각갂갃간갅")
        input = TestHelper.getResourceAbsolutePath("bomtests/bom_utf32le.html")
        doc = Ksoup.parseFile(filePath = input, baseUri = "http://example.com", charsetName = null)
        assertContains(doc.title(), "UTF-32LE")
        assertContains(doc.text(), "가각갂갃간갅")
    }

    @Test
    fun streamerSupportsBOMinFiles() = runTest {
        if (BuildConfig.isKotlinx && Platform.isJsOrWasm()) {
            // FIXME: UTF-16 charset not supported
            return@runTest
        }
        // test files from http://www.i18nl10n.com/korean/utftest/
        var source = TestHelper.readResource("bomtests/bom_utf16be.html")
        val parser = Parser.htmlParser()

        var doc: Document = DataUtil.streamParser(sourceReader = source, baseUri = "http://example.com", charset = null, parser = parser)
            .complete()
        assertContains(doc.title(), "UTF-16BE")
        assertContains(doc.text(), "가각갂갃간갅")

        source = TestHelper.readResource("bomtests/bom_utf16le.html")
        doc = DataUtil.streamParser(sourceReader = source, baseUri = "http://example.com", charset = null, parser = parser)
            .complete()
        assertContains(doc.title(), "UTF-16LE")
        assertContains(doc.text(), "가각갂갃간갅")

        if (Platform.isJsOrWasm() || Platform.isWindows() || Platform.isLinux()) {
            // FIXME: UTF-32 charset not supported
            return@runTest
        }

        source = TestHelper.readResource("bomtests/bom_utf32be.html")
        doc = DataUtil.streamParser(sourceReader = source, baseUri = "http://example.com", charset = null, parser = parser)
            .complete()
        assertContains(doc.title(), "UTF-32BE")
        assertContains(doc.text(), "가각갂갃간갅")

        source = TestHelper.readResource("bomtests/bom_utf32le.html")
        doc = DataUtil.streamParser(sourceReader = source, baseUri = "http://example.com", charset = null, parser = parser)
            .complete()
        assertContains(doc.title(), "UTF-32LE")
        assertContains(doc.text(), "가각갂갃간갅")
    }

    @Test
    fun supportsUTF8BOM() = runTest {
        val source = TestHelper.readResource("bomtests/bom_utf8.html")
        val doc: Document = Ksoup.parse(sourceReader = source, baseUri = "http://example.com", charsetName = null)
        assertEquals("OK", doc.head().select("title").text())
    }

    @Test
    fun noExtraNULLBytes() {
        val b = "<html><head><meta charset=\"UTF-8\"></head><body><div><u>ü</u>ü</div></body></html>"
            .toByteArray(Charsets.UTF8)
        val doc = Ksoup.parse(b.openSourceReader(), baseUri = "", charsetName = null)
        assertFalse(doc.outerHtml().contains("\u0000"))
    }

    @Test
    fun supportsZippedUTF8BOM() = runTest {
        val resourceName = "bomtests/bom_utf8.html.gz"
        val doc: Document = if (BuildConfig.isKotlinx) {
            val source = TestHelper.readResource(resourceName)
            Ksoup.parse(sourceReader = source, baseUri = "http://example.com")
        } else {
            Ksoup.parseFile(filePath = TestHelper.getResourceAbsolutePath(resourceName), baseUri = "http://example.com")
        }

        assertEquals("OK", doc.head().select("title").text())
        assertEquals(
            "There is a UTF8 BOM at the top (before the XML decl). If not read correctly, will look like a non-joining space.",
            doc.body().text(),
        )
    }

    @Test
    fun streamerSupportsZippedUTF8BOM() = runTest {
        val source = TestHelper.readGzipResource("bomtests/bom_utf8.html.gz")
        val doc = DataUtil.streamParser(
            sourceReader = source,
            baseUri = "http://example.com",
            charset = null,
            parser = Parser.htmlParser()
        ).complete()
        assertEquals("OK", doc.head().select("title").text())
        assertEquals(
            "There is a UTF8 BOM at the top (before the XML decl). If not read correctly, will look like a non-joining space.",
            doc.body().text()
        )
    }

    @Test
    fun supportsXmlCharsetDeclaration() {
        val encoding = "iso-8859-1"
        val soup = (
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>" +
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">Hellö Wörld!</html>"
                ).toByteArray(Charsets.forName(encoding)).openSourceReader()
        val doc: Document = Ksoup.parse(soup, baseUri = "", charsetName = null)
        assertEquals("Hellö Wörld!", doc.body().text())
    }

    @Test
    fun loadsGzipFile() = runTest {
        if (BuildConfig.isKotlinx) {
//            kotlinx module not support gzip
            return@runTest
        }
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/gzip.html.gz")
        val doc: Document = Ksoup.parseFile(filePath = input, charsetName = null)
        doc.toString()
        assertEquals("Gzip test", doc.title())
        assertEquals("This is a gzipped HTML file.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun loadsZGzipFile() = runTest {
        if (BuildConfig.isKotlinx) {
//            kotlinx module not support gzip
            return@runTest
        }
        // compressed on win, with z suffix
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/gzip.html.z")
        val doc: Document = Ksoup.parseFile(filePath = input, charsetName = null)
        assertEquals("Gzip test", doc.title())
        assertEquals("This is a gzipped HTML file.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun handlesFakeGzipFile() = runTest {
        if (BuildConfig.isKotlinx) {
//            kotlinx module not support gzip
            return@runTest
        }
        val input: String = TestHelper.getResourceAbsolutePath("htmltests/fake-gzip.html.gz")
        val doc: Document = Ksoup.parseFile(filePath = input, charsetName = null)
        assertEquals("This is not gzipped", doc.title())
        assertEquals("And should still be readable.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun handlesChunkedInputStream() = runTest {
        if (BuildConfig.isKotlinx) {
//            kotlinx module not support gzip
            return@runTest
        }
        val resourceFile = TestHelper.getResourceAbsolutePath("htmltests/large.html.gz")
        val inputFile = resourceFile.toSourceFile()
        val input: String = TestHelper.getFileAsString(resourceFile.uniVfs)

        val expected = Ksoup.parse(input, "https://example.com")
        val doc: Document = Ksoup.parseFile(inputFile, baseUri = "https://example.com", charsetName = null)

        println("""docSize: ${doc.toString().length}, expectedSize: ${expected.toString().length}""")
        assertTrue(doc.hasSameValue(expected))
    }

    @Test
    fun testStringVsSourceReaderParse() = runTest {
        val resourceFile = TestHelper.getResourceAbsolutePath("htmltests/large.html.gz")
        val input: String = TestHelper.getFileAsString(resourceFile.uniVfs)

        val expected = Ksoup.parse(input, "https://example.com")
        val doc: Document = Ksoup.parse(sourceReader = input.openSourceReader(), baseUri = "https://example.com", charsetName = null)

        assertTrue(doc.hasSameValue(expected))
    }

    @Test
    fun handlesUnlimitedRead() = runTest {
        val inputFile: String = TestHelper.getResourceAbsolutePath("htmltests/large.html.gz")
        val input: String = TestHelper.getFileAsString(inputFile.uniVfs)
        val byteBuffer: ByteArray = DataUtil.readToByteBuffer(input.openSourceReader(), 0)
        val read = byteBuffer.decodeToString()
        assertEquals(input, read)
    }
}
