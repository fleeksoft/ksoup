package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.kotlinx.KotlinxKsoupEngine
import com.fleeksoft.ksoup.KsoupEngineInstance
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferReaderTest {
    @BeforeTest
    fun initKsoup() {
        KsoupEngineInstance.init(KotlinxKsoupEngine())
    }

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
        assertEquals(6, reader.read(buffer, 0, 6))
        assertEquals("Hello,", buffer.decodeToString())
    }
}
