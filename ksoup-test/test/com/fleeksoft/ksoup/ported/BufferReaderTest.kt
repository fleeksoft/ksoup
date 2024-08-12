package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.isApple
import com.fleeksoft.ksoup.isJS
import com.fleeksoft.ksoup.isWindows
import com.fleeksoft.ksoup.ported.io.Charsets
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferReaderTest {
    @Test
    fun testMixCharReader() {
        val inputData = "ä<a>ä</a>"
        val streamCharReader = inputData.toStreamCharReader()
        inputData.toCharArray().forEach { char ->
            val charArray = CharArray(1)
            assertEquals(1, streamCharReader.readCharArray(charArray, 0, 1))
            assertEquals(char, charArray[0])
        }
        val charArray = CharArray(1) { ' ' }
        assertEquals(-1, streamCharReader.readCharArray(charArray, 0, 1))
        assertEquals(' ', charArray[0])
    }

    @Test
    fun testMixCharArrayReader() {
        val inputData = "ä<a>ä</a>"
        val streamCharReader = inputData.toStreamCharReader()
        inputData.toCharArray().forEach {
            val charArray = CharArray(1)
            assertEquals(1, streamCharReader.readCharArray(charArray = charArray, offset = 0, count = 1))
            assertEquals(it, charArray[0])
        }
        val charArray = CharArray(1) { ' ' }
        assertEquals(-1, streamCharReader.readCharArray(charArray, 0, 1))
        assertEquals(' ', charArray[0])
    }

    @Test
    fun testRewind() {
        val inputData = "Hello, Reader!"
        val reader = inputData.openBufferReader()
        val buffer = ByteArray(6)
        reader.read(buffer, 0, 6)
        assertEquals("Hello,", buffer.decodeToString())
    }
}
