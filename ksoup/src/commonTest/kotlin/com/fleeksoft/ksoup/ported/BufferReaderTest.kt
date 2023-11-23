package com.fleeksoft.ksoup.ported

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
    }

    @Test
    fun testClose() {
        val reader = BufferReader("Hello, Reader!")
        reader.close()
        assertFailsWith<IOException> {
            reader.read()
        }
    }
}