package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.helper.DataUtil
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.openSourceReader
import com.fleeksoft.ksoup.ported.toByteArray
import kotlinx.coroutines.test.runTest
import java.io.*
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class DataUtilTestJvm {

    private fun inputStream(data: String): ByteArrayInputStream {
        return ByteArrayInputStream(data.encodeToByteArray())
    }

    @Test
    fun loadsGzipPath() = runTest {
        if (!TestHelper.isGzipSupported()) {
//            gzip not supported for this
            return@runTest
        }
        val `in`: Path = ParserHelper.getPath("/htmltests/gzip.html.gz")
        val doc: Document = Ksoup.parsePath(`in`)
        assertEquals("Gzip test", doc.title())
        assertEquals("This is a gzipped HTML file.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun loadsZGzipPath() = runTest {
        if (!TestHelper.isGzipSupported()) {
//            gzip not supported for this
            return@runTest
        }
        // compressed on win, with z suffix
        val `in`: Path = ParserHelper.getPath("htmltests/gzip.html.z")
        val doc: Document = Ksoup.parsePath(`in`)
        assertEquals("Gzip test", doc.title())
        assertEquals("This is a gzipped HTML file.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun handlesFakeGzipPath() = runTest {
        if (!TestHelper.isGzipSupported()) {
//            gzip not supported for this
            return@runTest
        }
        val `in`: Path = ParserHelper.getPath("htmltests/fake-gzip.html.gz")
        val doc: Document = Ksoup.parsePath(`in`)
        assertEquals("This is not gzipped", doc.title())
        assertEquals("And should still be readable.", doc.selectFirst("p")!!.text())
    }

    @Test
    fun testParseSequenceBufferReader() = runTest {
        val bufferReader = TestHelper.resourceFilePathToStream("htmltests/medium.html")
        val fileContent = String(bufferReader.readAllBytes())
        val halfLength = fileContent.length / 2
        val firstPart: String = fileContent.substring(0, halfLength)
        val secondPart = fileContent.substring(halfLength)
        val sequenceStream = SequenceInputStream(
            inputStream(firstPart),
            inputStream(secondPart),
        )
        val doc: Document = DataUtil.parseInputSource(
            sourceReader = sequenceStream.readAllBytes().openSourceReader(),
            charsetName = null,
            baseUri = "",
            parser = Parser.htmlParser(),
        )
        assertEquals(fileContent, doc.outerHtml())
    }

    @Test
    fun testLowercaseUtf8CharsetWithInputStream() {
        val inputStream = FileInputStream(TestHelper.getResourceAbsolutePath("htmltests/lowercase-charset-test.html"))
        val doc: Document = Ksoup.parseInputStream(inputStream = inputStream, baseUri = "", charsetName = null)
        val form = doc.select("#form").first()
        assertNotNull(form)
        assertEquals(2, form.children().size)
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }

    @Test
    fun testHandlesChunkedInputStream() {
        val file = File(TestHelper.getResourceAbsolutePath("htmltests/large.html.gz"))
        val input = getFileAsString(file)
        val expected = Ksoup.parse(html = input, baseUri = "https://example.com")
        val doc: Document = Ksoup.parse(
            sourceReader = GZIPInputStream(FileInputStream(file)).readAllBytes().openSourceReader(),
            charsetName = null,
            baseUri = "https://example.com",
        )

        val doc2: Document = Ksoup.parseInputStream(
            inputStream = GZIPInputStream(FileInputStream(file)),
            charsetName = null,
            baseUri = "https://example.com"
        )

        println("""docSize: ${doc.toString().length}, expectedSize: ${expected.toString().length}""")
        assertTrue(doc.hasSameValue(expected))
        assertTrue(doc2.hasSameValue(expected))
    }

    @Test
    fun testHandlesUnlimitedRead() {
        val file = File(TestHelper.getResourceAbsolutePath("htmltests/large.html.gz"))
        val input: String = getFileAsString(file)

        //        VaryingReadInputStream stream = new VaryingReadInputStream(ParseTest.inputStreamFrom(input));
        val expected: Document = Ksoup.parse(html = input, baseUri = "https://example.com")
        val doc: Document = Ksoup.parseInputStream(
            inputStream = inputStreamFrom(input),
            charsetName = null,
            baseUri = "https://example.com",
        )
        val doc2: Document = Ksoup.parseInputStream(
            inputStream = GZIPInputStream(FileInputStream(file)),
            charsetName = null,
            baseUri = "https://example.com",
        )
        val docThree: Document = Ksoup.parse(
            sourceReader = GZIPInputStream(FileInputStream(file)).readAllBytes().openSourceReader(),
            charsetName = null,
            baseUri = "https://example.com",
        )

        assertTrue(doc.hasSameValue(expected))
        assertTrue(doc.hasSameValue(doc2))
        assertTrue(doc.hasSameValue(docThree))
    }

    @Test
    fun testStreamIssue() = runTest {
        val file = File(TestHelper.getResourceAbsolutePath("htmltests/large.html.gz"))


        val doc: Document = Ksoup.parseInputStream(
            inputStream = GZIPInputStream(FileInputStream(file)),
            charsetName = null,
            baseUri = "https://example.com",
        )
        assertEquals(280745, doc.toString().length)
    }

    companion object {
        fun getFileAsString(file: File): String {
            val bytes: ByteArray =
                if (file.getName().endsWith(".gz")) {
                    val stream: InputStream = GZIPInputStream(FileInputStream(file))
                    val byteBuffer: ByteArray = DataUtil.readToByteBuffer(stream.readAllBytes().openSourceReader(), 0)
                    byteBuffer
                } else {
                    file.readBytes()
                }
            return String(bytes)
        }

        fun inputStreamFrom(s: String): InputStream {
            return ByteArrayInputStream(s.toByteArray(Charsets.UTF8))
        }
    }
}
