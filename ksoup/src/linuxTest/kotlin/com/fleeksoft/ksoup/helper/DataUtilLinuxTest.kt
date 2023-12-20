package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.ported.BufferReader
import com.fleeksoft.ksoup.ported.toBuffer
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataUtilLinuxTest {
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
    fun handlesUnlimitedRead() {
        val inputFile: String = TestHelper.getResourceAbsolutePath("htmltests/large.html.gz")
        val input: String = TestHelper.getFileAsString(inputFile.toPath())
        val byteBuffer: ByteArray = DataUtil.readToByteBuffer(BufferReader(input.toBuffer()), 0)
        val read = byteBuffer.decodeToString()
        assertEquals(input, read)
    }
}
