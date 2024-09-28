package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.io.KByteBuffer
import com.fleeksoft.ksoup.ported.toByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KByteBufferTest {

    @Test
    fun testInitialization() {
        val buffer = KByteBuffer(10)
        assertEquals(10, buffer.size)
        assertEquals(0, buffer.position())
        assertEquals(0, buffer.available())
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testWriteBytes() {
        val buffer = KByteBuffer(10)
        val data = "Hello".toByteArray()
        buffer.writeBytes(data, data.size)
        assertEquals(5, buffer.available())
        assertEquals(0, buffer.position())
    }

    @Test
    fun testReadText() {
        val buffer = KByteBuffer(10)
        val data = "Hello".toByteArray()
        buffer.writeBytes(data, data.size)

        val text = buffer.readText(Charsets.UTF8, 5)
        assertEquals("Hello", text)
        assertEquals(0, buffer.available())
        assertEquals(5, buffer.position())
    }

    @Test
    fun testCompact() {
        val buffer = KByteBuffer(10)
        val data = "12345".toByteArray()
        buffer.writeBytes(data, data.size)

        buffer.readText(Charsets.UTF8, 5)
        assertEquals(5, buffer.position())

        buffer.compact()
        assertEquals(0, buffer.position())
    }

    @Test
    fun testExhausted() {
        val buffer = KByteBuffer(5)
        val data = "Hello".toByteArray()
        buffer.writeBytes(data, data.size)

        buffer.readText(Charsets.UTF8, 5)
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testWriteBytesOverflow() {
        val buffer = KByteBuffer(5)
        val data = "Overflow".toByteArray()

        assertFailsWith<IllegalArgumentException> {
            buffer.writeBytes(data, data.size)
        }
    }

    @Test
    fun testReadTextWithOverflow() {
        val buffer = KByteBuffer(5)
        val data = "Hello".toByteArray()
        buffer.writeBytes(data, data.size)

        val text = buffer.readText(Charsets.UTF8, 6) // max > data length
        assertEquals("Hello", text)
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testCompactAndWriteAgain() {
        val buffer = KByteBuffer(10)
        val data1 = "12345".toByteArray()
        buffer.writeBytes(data1, data1.size)

        buffer.readText(Charsets.UTF8, 5)
        buffer.compact()

        val data2 = "67890".toByteArray()
        buffer.writeBytes(data2, data2.size)

        val text = buffer.readText(Charsets.UTF8, 5)
        assertEquals("67890", text)
    }

    @Test
    fun testPartialDecodingWithRemainingBytes() {
        val buffer = KByteBuffer(10)
        val data = "Hello".toByteArray(Charsets.forName("UTF-8"))

        // Write some data into the buffer
        buffer.writeBytes(data, data.size)

        // Decode part of the data (e.g., max = 3)
        val result = buffer.readText(Charsets.forName("UTF-8"), 3)

        // Verify that only 3 bytes were consumed and 2 are left in the buffer
        assertEquals("Hel", result)
        assertEquals(2, buffer.available())
        assertEquals(3, buffer.position())
    }

    @Test
    fun testDecodingRemainingBytesLater() {
        val buffer = KByteBuffer(10)
        val data = "HelloWorld".toByteArray(Charsets.forName("UTF-8"))

        // Write some data into the buffer
        buffer.writeBytes(data, data.size)

        // Decode part of the data (e.g., max = 5)
        val firstResult = buffer.readText(Charsets.forName("UTF-8"), 5)

        // Verify that 5 bytes were consumed and 5 are left in the buffer
        assertEquals("Hello", firstResult)
        assertEquals(5, buffer.available())
        assertEquals(5, buffer.position())

        // Decode the remaining bytes
        val secondResult = buffer.readText(Charsets.forName("UTF-8"), 5)

        // Verify that the remaining bytes are consumed
        assertEquals("World", secondResult)
        assertEquals(0, buffer.available())
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testIncompleteMultiByteCharacter() {
        val buffer = KByteBuffer(10)
        val data = "äöü".toByteArray(Charsets.forName("UTF-8")) // Multi-byte characters

        // Write partial data to simulate incomplete multi-byte characters
        buffer.writeBytes(data.copyOfRange(0, 4), 4) // First character (ä) is 2 bytes

        // Attempt to decode part of the data (e.g., max = 3, not enough to decode full multi-byte character)
        val result = buffer.readText(Charsets.forName("UTF-8"), 3)

        // Verify that no partial characters are decoded
        assertEquals("ä", result)
        assertEquals(2, buffer.available()) // Still 4 bytes available
        assertEquals(2, buffer.position()) // Position remains the same
    }

    @Test
    fun testCompleteMultiByteCharacterAfterMoreData() {
        val buffer = KByteBuffer(10)
        val data = "äöü".toByteArray(Charsets.forName("UTF-8")) // Multi-byte characters

        // Write partial data (2 bytes, just enough to complete the first character ä)
        buffer.writeBytes(data.copyOfRange(0, 2), 2)

        // Attempt to decode the data
        val result = buffer.readText(Charsets.forName("UTF-8"), 2)

        // Verify that the first multi-byte character (ä) is decoded
        assertEquals("ä", result)
        assertEquals(0, buffer.available())
        assertEquals(2, buffer.position())

        // Write more data to complete the next character
        buffer.writeBytes(data.copyOfRange(2, data.size), data.size - 2)
        assertEquals(data.size - 2, buffer.available())
        assertEquals(2, buffer.position())

        // Decode the next characters
        val secondResult = buffer.readText(Charsets.forName("UTF-8"), 4)

        // Verify that the remaining characters are decoded
        assertEquals("öü", secondResult)
        assertEquals(6, buffer.position())
        assertEquals(0, buffer.available())
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testDecodingWithNonUTF8Charset() {
        val buffer = KByteBuffer(10)
        val data = "Hello".toByteArray(Charsets.forName("ISO-8859-1")) // Non-UTF8 charset

        // Write some data into the buffer
        buffer.writeBytes(data, data.size)

        // Decode the data using ISO-8859-1 charset
        val result = buffer.readText(Charsets.forName("ISO-8859-1"), 5)

        // Verify that the string is decoded correctly
        assertEquals("Hello", result)
        assertEquals(0, buffer.available())
        assertTrue(buffer.exhausted())
    }
}