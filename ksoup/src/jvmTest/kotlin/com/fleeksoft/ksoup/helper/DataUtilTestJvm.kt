package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.integration.ParseTest
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
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
}
