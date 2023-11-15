package com.fleeksoft.ksoup.ported

import io.ktor.utils.io.core.String
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufferReaderTest {
    @Test
    fun testReadSingleCharacter() {
        val reader = BufferReader("Hello, Reader!")
        val character = reader.read()
        assertEquals('H'.code, character)
    }

    @Test
    fun testReadIntoArray() {
        val reader = BufferReader("Hello, Reader!")
        val buffer = ByteArray(5)
        val numCharsRead = reader[buffer]
        assertEquals(5, numCharsRead)
        assertEquals("Hello", buffer.decodeToString())
    }

    @Test
    fun testSkipCharacters() {
        val reader = BufferReader("Hello, Reader!")
        reader.skip(7)
        val buffer = ByteArray(6)
        reader[buffer]
        assertEquals("Reader", buffer.decodeToString())
    }

    @Test
    fun testRewind() {
        val inputData = "Hello, Reader!"
        val reader = BufferReader(inputData)
        val buffer = ByteArray(6)
        reader[buffer]
        assertEquals("Hello,", buffer.decodeToString())
        assertEquals(8L, reader.size())
        reader.rewind()
        assertEquals(inputData.length, reader.size().toInt())
        assertEquals(inputData, reader.readString())
        assertEquals(0, reader.size().toInt())
    }

    @Test
    fun testClose() {
        val reader = BufferReader("Hello, Reader!")
        reader.close()
        assertFailsWith<IOException> {
            reader.read()
        }
    }

    @Test
    fun testMarkAndReset() {
        val reader: BufferReader = BufferReader("Hello, Reader!")
        reader.mark(100)  // Marking a position
        val buffer = ByteArray(5)
        reader[buffer]
        assertEquals("Hello", buffer.decodeToString())

        reader.reset()  // Resetting to the marked position
        val bufferReset = ByteArray(5)
        reader[bufferReset]
        assertEquals("Hello", String(bufferReset))  // Reading the same string again
    }
}