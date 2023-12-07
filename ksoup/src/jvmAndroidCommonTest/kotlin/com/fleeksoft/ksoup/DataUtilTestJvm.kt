package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.helper.DataUtil
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
import io.ktor.utils.io.charsets.*
import okio.source
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataUtilTestJvm {
    private fun inputStream(data: String): ByteArrayInputStream {
        return ByteArrayInputStream(data.toByteArray())
    }

    @Test
    fun parseSequenceBufferReader() {
        // https://github.com/jhy/jsoup/pull/1671
        val bufferReader: BufferReader = TestHelper.resourceFilePathToBufferReader("htmltests/medium.html")
        val fileContent = String(bufferReader.readByteArray())
        val halfLength = fileContent.length / 2
        val firstPart: String = fileContent.substring(0, halfLength)
        val secondPart = fileContent.substring(halfLength)
        val sequenceStream =
            SequenceInputStream(
                inputStream(firstPart),
                inputStream(secondPart),
            )
        val doc: Document =
            DataUtil.parseInputSource(
                BufferReader(sequenceStream.source()),
                null,
                "",
                Parser.htmlParser(),
            )
        assertEquals(fileContent, doc.outerHtml())
    }

    @Test
    fun testLowercaseUtf8CharsetWithInputStream() {
        val inputStream = FileInputStream(TestHelper.getResourceAbsolutePath("htmltests/lowercase-charset-test.html"))
        val doc: Document = Ksoup.parseInputStream(inputStream = inputStream, baseUri = "", charsetName = null)
        val form = doc.select("#form").first()
        assertEquals(2, form!!.children().size)
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }

    @Test
    fun handlesChunkedInputStream() {
        val file = File(TestHelper.getResourceAbsolutePath("htmltests/large.html"))
        val input = getFileAsString(file)
        val expected =
            Ksoup.parse(
                html = input,
                baseUri = "https://example.com",
            )
        val doc: Document =
            Ksoup.parse(
                bufferReader = BufferReader(FileInputStream(file).source()),
                charsetName = null,
                baseUri = "https://example.com",
            )

        val doc2: Document =
            Ksoup.parseInputStream(
                inputStream = FileInputStream(file),
                charsetName = null,
                baseUri = "https://example.com",
            )

        println("""docSize: ${doc.toString().length}, expectedSize: ${expected.toString().length}""")
        assertTrue(doc.hasSameValue(expected))
        assertTrue(doc2.hasSameValue(expected))
    }

    @Test
    fun handlesUnlimitedRead() {
        val file = File(TestHelper.getResourceAbsolutePath("htmltests/large.html"))
        val input: String = getFileAsString(file)

        //        VaryingReadInputStream stream = new VaryingReadInputStream(ParseTest.inputStreamFrom(input));
        val expected: Document = Ksoup.parse(html = input, baseUri = "https://example.com")
        val doc: Document =
            Ksoup.parseInputStream(
                inputStream = inputStreamFrom(input),
                charsetName = null,
                baseUri = "https://example.com",
            )
        val doc2: Document =
            Ksoup.parseInputStream(
                inputStream = FileInputStream(file),
                charsetName = null,
                baseUri = "https://example.com",
            )
        val docThree: Document =
            Ksoup.parse(
                bufferReader = BufferReader(FileInputStream(file).source()),
                charsetName = null,
                baseUri = "https://example.com",
            )

        assertTrue(doc.hasSameValue(expected))
        assertTrue(doc.hasSameValue(doc2))
        assertTrue(doc.hasSameValue(docThree))
    }

    companion object {
        fun getFileAsString(file: File): String {
            val bytes: ByteArray =
                if (file.getName().endsWith(".gz")) {
                    val stream: InputStream = GZIPInputStream(FileInputStream(file))
                    val byteBuffer: ByteArray = DataUtil.readToByteBuffer(BufferReader(stream.source()), 0)
                    byteBuffer
                } else {
                    Files.readAllBytes(file.toPath())
                }
            return String(bytes)
        }

        fun inputStreamFrom(s: String): InputStream {
            return ByteArrayInputStream(s.toByteArray(StandardCharsets.UTF_8))
        }
    }
}
