package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.PlatformType
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufferReaderTest {
    @Test
    fun testSpecialCharsBufferReader() {
        if (Platform.current == PlatformType.JS || Platform.current == PlatformType.IOS) {
            // FIXME: euc-kr charset not supported
            return
        }

        val specialText1 = "Hello &amp;&lt;&gt; Å å π 新 there ¾ © »"
        val specialText2 = "Übergrößenträger"
        val specialText3 = "한국어"

        assertEquals(specialText1, BufferReader(specialText1).readString())

        assertEquals(
            specialText3,
            BufferReader(
                byteArray = specialText3.toByteArray(Charset.forName("euc-kr")),
                charset = "euc-kr",
            ).readString(specialText3.length.toLong()),
        )

        assertEquals(specialText2, BufferReader(specialText2).readString())
        assertEquals(
            specialText2,
            BufferReader(
                byteArray = specialText2.toByteArray(Charset.forName("iso-8859-1")),
                charset = "iso-8859-1",
            ).readString(specialText2.length.toLong()),
        )
    }

    @Test
    fun testDetermineCharSize() {
        val char1 = 'H'
        val char2 = 'Ü'
        val char3 = '한'
        val char4 = '국'
        val char5 = '어'

        assertEquals(1, BufferReader.determineCharSize(char1.toString().toByteArray()[0].toInt()))
        assertEquals(2, BufferReader.determineCharSize(char2.toString().toByteArray()[0].toInt()))
        assertEquals(3, BufferReader.determineCharSize(char3.toString().toByteArray()[0].toInt()))
        assertEquals(3, BufferReader.determineCharSize(char4.toString().toByteArray()[0].toInt()))
        assertEquals(3, BufferReader.determineCharSize(char5.toString().toByteArray()[0].toInt()))
    }

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
