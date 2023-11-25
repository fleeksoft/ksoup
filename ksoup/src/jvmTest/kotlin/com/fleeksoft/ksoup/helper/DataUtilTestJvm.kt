package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.integration.ParseTest
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parse
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
import io.ktor.utils.io.charsets.*
import java.io.*
import kotlin.test.*

class DataUtilTestJvm {

    private fun inputStream(data: String): ByteArrayInputStream {
        return ByteArrayInputStream(data.toByteArray())
    }


    @Test
    @Throws(IOException::class)
    fun parseSequenceBufferReader() {
        // https://github.com/jhy/jsoup/pull/1671
        val bufferReader: BufferReader = ParseTest.resourceFilePathToBufferReader("htmltests/medium.html")
        val fileContent = String(bufferReader.readByteArray())
        val halfLength = fileContent.length / 2
        val firstPart: String = fileContent.substring(0, halfLength)
        val secondPart = fileContent.substring(halfLength)
        val sequenceStream = SequenceInputStream(
            inputStream(firstPart),
            inputStream(secondPart)
        )
        val doc: Document =
            DataUtil.parseInputSource(
                BufferReader(sequenceStream.readAllBytes()),
                null,
                "",
                Parser.htmlParser()
            )
        assertEquals(fileContent, doc.outerHtml())
    }


    @Test
    fun testLowercaseUtf8CharsetWithInputStream() {
        val inputStream = FileInputStream(ParseTest.getResourceAbsolutePath("htmltests/lowercase-charset-test.html"))
        val doc: Document = Ksoup.parse(inputStream = inputStream, baseUri = "", charsetName = null)
        val form = doc.select("#form").first()
        assertEquals(2, form!!.children().size)
        assertEquals("UTF-8", doc.outputSettings().charset().name.uppercase())
    }
}
